#!/usr/bin/env python3
"""증상 마스터 엑셀 → Flyway 시드 SQL 생성.

    python3 scripts/gen_symptom_seed.py \
        > be/src/main/resources/db/migration/V3__seed_symptom_master.sql

엑셀을 손으로 옮겨 적지 않기 위한 스크립트다. 엑셀이 갱신되면 다시 돌려 새 버전
마이그레이션(V5, V6 …)으로 추가한다 — 이미 적용된 마이그레이션은 수정하지 않는다.

의존성 없이 표준 라이브러리만 쓴다(xlsx 는 zip + XML).
"""

import re
import sys
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

EXCEL = Path(__file__).resolve().parent.parent / "씩씩이_증상마스터_희귀질환포함-2.xlsx"
SHEET = "xl/worksheets/sheet1.xml"
NS = {"m": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}

# 엑셀 컬럼 → 의미
COL = {
    "A": "name",           # 의료명칭
    "B": "name_en",        # 영어
    "C": "name_ko",        # 한국어(일상 표현)
    "D": "related",        # 유사/연관 검색어
    "E": "description",    # 증상 설명
    "F": "category",       # 위치(대분류)
    "G": "detail",         # 세부위치
    "H": "priority",       # 우선순위
    "I": "hpo",            # HPO 코드
}


def read_rows():
    """워크시트를 {의미: 값} 딕셔너리 목록으로 읽는다."""
    with zipfile.ZipFile(EXCEL) as z:
        root = ET.fromstring(z.read(SHEET))

    rows = []
    for r in root.findall(".//m:row", NS):
        cell = {}
        for c in r.findall("m:c", NS):
            col = "".join(ch for ch in c.get("r") if ch.isalpha())
            if c.get("t") == "inlineStr":
                value = "".join(t.text or "" for t in c.findall(".//m:t", NS))
            else:
                v = c.find("m:v", NS)
                value = v.text if v is not None else ""
            if col in COL:
                cell[COL[col]] = (value or "").strip()
        if cell:
            rows.append(cell)

    header, data = rows[0], rows[1:]
    if header.get("name") != "의료명칭":
        sys.exit(f"헤더가 예상과 다릅니다: {header}")
    return [r for r in data if r.get("name")]


def norm(value):
    """'-' 나 빈 문자열은 값 없음으로 본다."""
    if not value or value.strip() in {"-", "", "–"}:
        return None
    return value.strip()


def split_terms(value):
    """쉼표·중점으로 구분된 검색어를 쪼갠다."""
    if not norm(value):
        return []
    parts = re.split(r"[,·/]", value)
    return [p.strip() for p in parts if p.strip() and p.strip() != "-"]


def priority_of(value):
    """'1차(POC-…)' / '2차(일반빈도)' → enum."""
    return "PRIMARY" if value.startswith("1차") else "SECONDARY"


def q(value):
    """SQL 문자열 리터럴. NULL 과 작은따옴표를 처리한다."""
    if value is None:
        return "NULL"
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def main():
    rows = read_rows()

    # 카테고리는 엑셀에 등장한 순서를 표시 순서로 삼는다.
    categories = []
    for r in rows:
        c = norm(r.get("category"))
        if c and c not in categories:
            categories.append(c)

    out = []
    w = out.append

    w("-- 증상 마스터 시드.")
    w("--")
    w("-- 이 파일은 scripts/gen_symptom_seed.py 가 엑셀에서 생성한 것이다. 직접 수정하지 말고")
    w("-- 엑셀을 고친 뒤 스크립트를 다시 돌려 새 버전 마이그레이션으로 추가한다.")
    w(f"--   출처: {EXCEL.name}")
    w(f"--   증상 {len(rows)}개 / 카테고리 {len(categories)}개")
    w("")

    w("INSERT INTO symptom_categories (name, display_order) VALUES")
    w(",\n".join(f"    ({q(c)}, {i + 1})" for i, c in enumerate(categories)) + ";")
    w("")

    # 카테고리는 이름으로 찾아 넣는다. AUTO_INCREMENT 값을 가정하지 않기 위해서다.
    w("INSERT INTO symptoms")
    w("    (name, name_en, name_ko, description, category_id, detail_location, priority, hpo_code)")
    w("VALUES")
    values = []
    for r in rows:
        values.append(
            "    ({name}, {en}, {ko}, {desc},"
            " (SELECT id FROM symptom_categories WHERE name = {cat}),"
            " {detail}, {prio}, {hpo})".format(
                name=q(r["name"]),
                en=q(norm(r.get("name_en"))),
                ko=q(norm(r.get("name_ko"))),
                desc=q(norm(r.get("description"))),
                cat=q(norm(r.get("category"))),
                detail=q(norm(r.get("detail"))),
                prio=q(priority_of(r.get("priority", ""))),
                hpo=q(norm(r.get("hpo"))),
            )
        )
    w(",\n".join(values) + ";")
    w("")

    # 동의어: 의료명칭·영어·일상표현. 어느 것으로 검색해도 찾아지게 한다.
    synonyms = []
    for r in rows:
        terms = {r["name"]}
        if norm(r.get("name_en")):
            terms.add(r["name_en"].strip())
        terms.update(split_terms(r.get("name_ko", "")))
        for t in sorted(terms):
            synonyms.append((r["name"], t))

    w("-- 동의어 — 의료명칭/영어/일상표현 중 무엇으로 검색해도 같은 증상을 찾게 한다.")
    w("INSERT INTO symptom_synonyms (symptom_id, term) VALUES")
    w(",\n".join(
        f"    ((SELECT id FROM symptoms WHERE name = {q(n)}), {q(t)})" for n, t in synonyms
    ) + ";")
    w("")

    # 연관어: 하위유형·연관 증상. 동의어가 아니므로 따로 담는다.
    related = []
    for r in rows:
        for t in split_terms(r.get("related", "")):
            related.append((r["name"], t))

    w("-- 연관 검색어 — 하위유형·연관 증상. 정확히 같은 증상이 아니므로 동의어와 분리한다.")
    w("INSERT INTO symptom_related_terms (symptom_id, term) VALUES")
    w(",\n".join(
        f"    ((SELECT id FROM symptoms WHERE name = {q(n)}), {q(t)})" for n, t in related
    ) + ";")

    print("\n".join(out))

    print(
        f"\n-- 생성 요약: 카테고리 {len(categories)}, 증상 {len(rows)}, "
        f"동의어 {len(synonyms)}, 연관어 {len(related)}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
