-- 질환 마스터 시드.
--
-- 프로토타입(sicksick.html)의 DISEASES 3건을 옮긴 것이다. 검색 UI 동작을 확인할 수 있는
-- 최소 데이터이며, 목록에 없는 질환은 사용자가 '직접 입력하기'로 등록한다.
--
-- ⚠ 주요 증상 매핑에 대하여
-- 프로토타입의 증상은 서술형 표현('반복되는 두통', '팔다리 힘빠짐')이고 증상 마스터는
-- 임상 용어('두통', '근력 저하')다. 아래는 그 사이를 옮긴 것으로, 의학적 검수를 거친
-- 값이 아니다. 실제 서비스 반영 전에 임상 검토가 필요하다.
-- 마스터에 대응 항목이 없는 증상('편마비', '혈변', '아침 관절 경직')은 제외했다.
--
-- INSERT ... SELECT 를 쓰므로 증상명이 마스터에 없으면 해당 행만 조용히 빠진다.
-- 적재 후 아래 쿼리로 건수를 확인할 것:
--   SELECT c.name, COUNT(*) FROM conditions c
--     JOIN condition_symptoms cs ON cs.condition_id = c.id GROUP BY c.name;

INSERT INTO conditions (name, code, description) VALUES
    ('모야모야병', 'I67.5',
     '두개내 내경동맥 말단이 서서히 좁아지고, 이를 보상하려 가느다란 곁혈관이 생기는 만성 진행성 뇌혈관질환이에요.'),
    ('크론병', 'K50',
     '소화관에 비연속적으로 만성 염증이 반복되는 염증성 장질환(자가면역)이에요.'),
    ('류마티스 관절염', 'M05',
     '관절 활막에 만성 염증이 생겨 연골과 뼈를 파괴하는 전신성 자가면역질환이에요.');

-- 질환별 주요 증상 — 증상 선택 화면의 후보 목록으로 쓰인다.
INSERT INTO condition_symptoms (condition_id, symptom_id)
SELECT c.id, s.id
FROM (
    -- 모야모야병
    SELECT '모야모야병' AS condition_name, '두통' AS symptom_name
    UNION ALL SELECT '모야모야병', '어지러움'
    UNION ALL SELECT '모야모야병', '저림/이상감각'      -- 손발 저림
    UNION ALL SELECT '모야모야병', '시야 흐림'
    UNION ALL SELECT '모야모야병', '시력 저하'
    UNION ALL SELECT '모야모야병', '구음장애'            -- 언어 장애
    UNION ALL SELECT '모야모야병', '근력 저하'           -- 팔다리 힘빠짐
    UNION ALL SELECT '모야모야병', '경련'                -- 경련·발작

    -- 크론병
    UNION ALL SELECT '크론병', '설사'                    -- 만성 설사
    UNION ALL SELECT '크론병', '복통'
    UNION ALL SELECT '크론병', '발열'                    -- 미열
    UNION ALL SELECT '크론병', '피로'                    -- 피로감
    UNION ALL SELECT '크론병', '식욕부진'
    UNION ALL SELECT '크론병', '체중 감소'
    UNION ALL SELECT '크론병', '관절통'
    UNION ALL SELECT '크론병', '구역감'

    -- 류마티스 관절염
    UNION ALL SELECT '류마티스 관절염', '관절통'         -- 손가락·손목 통증
    UNION ALL SELECT '류마티스 관절염', '관절구축'       -- 관절 변형
    UNION ALL SELECT '류마티스 관절염', '부종'           -- 대칭성 관절 부종
    UNION ALL SELECT '류마티스 관절염', '근육통'
    UNION ALL SELECT '류마티스 관절염', '피로'           -- 전신 피로
    UNION ALL SELECT '류마티스 관절염', '발열'           -- 미열
    UNION ALL SELECT '류마티스 관절염', '저림/이상감각'  -- 손발 저림
    UNION ALL SELECT '류마티스 관절염', '어깨 결림'
) m
         JOIN conditions c ON c.name = m.condition_name
         JOIN symptoms s ON s.name = m.symptom_name;
