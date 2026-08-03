-- 온보딩 스키마 — 증상·질환 마스터와 사용자 등록 데이터.
--
-- 마스터(symptoms, conditions)와 사용자 데이터(user_conditions, user_condition_symptoms)를
-- 나눈다. 사용자 기록이 마스터를 이름이 아니라 id 로 참조해야, 나중에 명칭을 바꿔도 과거
-- 기록과 연결이 끊기지 않고 "어떤 증상이 많이 기록되나" 같은 집계가 가능하다.

-- ── 증상 마스터 ──────────────────────────────────────────────
-- 구조는 엑셀(씩씩이_증상마스터_희귀질환포함-2.xlsx) '안내' 시트의 권장안을 따른다.

-- 위치(대분류). 배/가슴/머리 같은 신체 큰 부위 단위.
CREATE TABLE symptom_categories (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    display_order INT         NOT NULL,
    UNIQUE KEY uk_symptom_categories_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE symptoms (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- 임상에서 쓰는 전문 용어. 화면 표시의 기준값이다.
    name            VARCHAR(100) NOT NULL,
    name_en         VARCHAR(100) NULL,
    -- 사용자가 검색할 법한 일상적 표현("열남", "몸이 찌뿌둥함").
    name_ko         VARCHAR(200) NULL,
    description     VARCHAR(500) NULL,
    category_id     BIGINT       NOT NULL,
    -- 통증·발진처럼 부위가 세분화되는 증상에만 있다. 전신성 증상은 NULL.
    detail_location VARCHAR(200) NULL,
    -- PRIMARY(1차 POC: 희귀질환·암·당뇨) / SECONDARY(2차 일반빈도)
    priority        VARCHAR(20)  NOT NULL,
    -- Human Phenotype Ontology 코드. 매핑을 확정하지 못한 항목은 NULL 이다.
    hpo_code        VARCHAR(20)  NULL,
    UNIQUE KEY uk_symptoms_name (name),
    KEY idx_symptoms_category (category_id),
    KEY idx_symptoms_priority (priority),
    CONSTRAINT fk_symptoms_category FOREIGN KEY (category_id) REFERENCES symptom_categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 순수 동의어. 의료명칭·영어·일상표현 중 무엇으로 검색해도 같은 증상을 찾게 한다.
CREATE TABLE symptom_synonyms (
    symptom_id BIGINT       NOT NULL,
    term       VARCHAR(200) NOT NULL,
    PRIMARY KEY (symptom_id, term),
    KEY idx_symptom_synonyms_term (term),
    CONSTRAINT fk_symptom_synonyms_symptom FOREIGN KEY (symptom_id) REFERENCES symptoms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 연관 검색어. 동의어와 성격이 다르다 — '두통' 으로 검색했을 때 '편두통·긴장성두통' 같은
-- 하위유형·연관 증상까지 노출시키기 위한 것이며, 정확히 같은 증상이 아니다.
-- 그래서 별도 테이블로 두고 검색 결과에서 구분할 수 있게 한다.
CREATE TABLE symptom_related_terms (
    symptom_id BIGINT       NOT NULL,
    term       VARCHAR(200) NOT NULL,
    PRIMARY KEY (symptom_id, term),
    KEY idx_symptom_related_terms_term (term),
    CONSTRAINT fk_symptom_related_symptom FOREIGN KEY (symptom_id) REFERENCES symptoms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ── 질환 마스터 ──────────────────────────────────────────────
CREATE TABLE conditions (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    -- 질병 분류 코드(KCD-8 등). 없을 수 있다.
    code        VARCHAR(20)  NULL,
    description VARCHAR(500) NULL,
    UNIQUE KEY uk_conditions_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 질환의 주요 증상. 증상 선택 화면에서 후보 목록으로 쓴다.
CREATE TABLE condition_symptoms (
    condition_id BIGINT NOT NULL,
    symptom_id   BIGINT NOT NULL,
    PRIMARY KEY (condition_id, symptom_id),
    KEY idx_condition_symptoms_symptom (symptom_id),
    CONSTRAINT fk_condition_symptoms_condition FOREIGN KEY (condition_id) REFERENCES conditions (id),
    CONSTRAINT fk_condition_symptoms_symptom FOREIGN KEY (symptom_id) REFERENCES symptoms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ── 사용자 프로필 ────────────────────────────────────────────
-- nickname 이 이미 users 에 있고 1:1 이라 테이블을 쪼개지 않는다.
-- 체중 '이력'이 필요해지면 그때 weight_logs 를 따로 만든다(여기는 현재값만).
ALTER TABLE users
    ADD COLUMN birth_date DATE     NULL AFTER nickname,
    ADD COLUMN height_cm  SMALLINT NULL AFTER birth_date,
    ADD COLUMN weight_kg  SMALLINT NULL AFTER height_cm;

-- ── 사용자가 등록한 질환 ─────────────────────────────────────
CREATE TABLE user_conditions (
    id                 BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id            BIGINT       NOT NULL,
    -- NULL 이면 마스터에 없는 질환을 사용자가 직접 입력한 것이다.
    -- 이 경우 custom_name 이 반드시 있어야 한다(애플리케이션에서 검증).
    condition_id       BIGINT       NULL,
    custom_name        VARCHAR(100) NULL,
    custom_code        VARCHAR(20)  NULL,
    custom_description VARCHAR(500) NULL,
    -- DIAGNOSED(진단 완료) / OBSERVING(진단 전)
    status             VARCHAR(20)  NOT NULL,
    -- 가장 최근 증상 시점. EXACT / D7 / D30 / M3 / OLD / UNKNOWN.
    -- "경험한 증상이 없어요" 를 고른 질환은 물어보지 않으므로 NULL 이다.
    recent_onset_type  VARCHAR(20)  NULL,
    -- EXACT 일 때만 채워진다.
    recent_onset_date  DATE         NULL,
    created_at         DATETIME(6)  NOT NULL,
    KEY idx_user_conditions_user (user_id),
    CONSTRAINT fk_user_conditions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_conditions_condition FOREIGN KEY (condition_id) REFERENCES conditions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 질환별로 사용자가 선택한 경험 증상.
CREATE TABLE user_condition_symptoms (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_condition_id BIGINT       NOT NULL,
    -- NULL 이면 마스터에 없는 증상을 직접 입력한 것이다(custom_name 필수).
    symptom_id        BIGINT       NULL,
    custom_name       VARCHAR(100) NULL,
    KEY idx_user_condition_symptoms_parent (user_condition_id),
    CONSTRAINT fk_user_condition_symptoms_parent
        FOREIGN KEY (user_condition_id) REFERENCES user_conditions (id),
    CONSTRAINT fk_user_condition_symptoms_symptom
        FOREIGN KEY (symptom_id) REFERENCES symptoms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
