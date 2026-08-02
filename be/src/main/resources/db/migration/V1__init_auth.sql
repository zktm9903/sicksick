-- 인증·회원가입 기반 스키마.
--
-- 설계 원칙 두 가지:
--   1) 유저 식별키는 이메일이 아니라 (provider, provider_user_id) 다.
--      카카오는 이메일이 선택 동의라 아예 안 오는 경우가 흔하고, 유저가 소셜 계정
--      이메일을 나중에 바꿀 수도 있다.
--   2) 토큰·인증번호는 원문을 저장하지 않는다. DB 가 유출돼도 그것만으로는
--      로그인이 불가능해야 한다.

CREATE TABLE users (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    -- NULL 허용. 카카오 이메일 미동의 케이스가 실제로 발생한다.
    email             VARCHAR(255) NULL,
    nickname          VARCHAR(50)  NULL,
    phone             VARCHAR(20)  NULL,
    phone_verified_at DATETIME(6)  NULL,
    -- PENDING(가입 미완료) / ACTIVE / SUSPENDED
    status            VARCHAR(20)  NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    deleted_at        DATETIME(6)  NULL,
    -- MySQL 은 UNIQUE 컬럼의 NULL 중복을 허용하므로 이메일 없는 유저가 여럿이어도 된다.
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE social_accounts (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    -- KAKAO / NAVER (구글·애플은 추후)
    provider         VARCHAR(20)  NOT NULL,
    -- 카카오 id(Long), 네이버 response.id, 구글 sub — 전부 문자열로 보관한다.
    provider_user_id VARCHAR(255) NOT NULL,
    -- 연동 당시 스냅샷. 참고용이며 인증에 쓰지 않는다.
    email            VARCHAR(255) NULL,
    linked_at        DATETIME(6)  NOT NULL,
    -- 유저가 버튼을 두 번 눌러 콜백이 동시에 들어와도 계정이 두 개 생기지 않게 막는
    -- 최후 방어선. 애플리케이션은 여기서 나는 위반을 잡아 기존 유저를 재조회한다.
    UNIQUE KEY uk_social_provider_user (provider, provider_user_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE refresh_tokens (
    id               BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    -- SHA-256 hex. 원문은 쿠키에만 있고 서버에는 남기지 않는다.
    token_hash       CHAR(64)    NOT NULL,
    -- 회전해도 물려받는 값. 슬라이딩 만료가 무한히 이어지는 것을 막는 절대 만료 기준.
    chain_started_at DATETIME(6) NOT NULL,
    expires_at       DATETIME(6) NOT NULL,
    -- 폐기 시각. 폐기된 토큰이 다시 들어오면 탈취로 보고 해당 유저 전체를 무효화한다.
    revoked_at       DATETIME(6) NULL,
    created_at       DATETIME(6) NOT NULL,
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE terms (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    -- 개정 시 새 버전을 INSERT 한다. 기존 동의 이력은 옛 버전을 계속 가리킨다.
    version       VARCHAR(20)  NOT NULL,
    title         VARCHAR(200) NOT NULL,
    required      BOOLEAN      NOT NULL,
    display_order INT          NOT NULL,
    -- 현재 노출 중인 버전. 개정 시 옛 행을 false 로 내린다.
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_terms_code_version (code, version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 어떤 유저가 어느 약관의 '어느 버전'에 동의했는지 남긴다.
-- 분쟁 시 증명해야 하는 정보라 버전까지 걸어둔다(term_id 가 버전을 포함).
CREATE TABLE user_term_agreements (
    user_id   BIGINT      NOT NULL,
    term_id   BIGINT      NOT NULL,
    agreed    BOOLEAN     NOT NULL,
    agreed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, term_id),
    CONSTRAINT fk_user_term_agreements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_term_agreements_term FOREIGN KEY (term_id) REFERENCES terms (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 휴대폰 본인인증. 현재는 SMS 발송만 목업이고 나머지 흐름은 실제와 동일하다.
CREATE TABLE phone_verifications (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    phone       VARCHAR(20) NOT NULL,
    -- 목업이어도 평문으로 두지 않는다. 실제 SMS 연동으로 바꿀 때 구조가 그대로 간다.
    code_hash   CHAR(64)    NOT NULL,
    attempts    INT         NOT NULL DEFAULT 0,
    expires_at  DATETIME(6) NOT NULL,
    verified_at DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    KEY idx_phone_verifications_user (user_id),
    CONSTRAINT fk_phone_verifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 약관 시드. 항목·필수여부·순서는 프로토타입(sicksick.html)의 TermsStep 과 일치시킨다.
-- 증상·질환은 민감정보라 개인정보 동의와 별도로 다루게 될 수 있으나, 현재 화면 구성에는
-- 항목이 없으므로 추가하지 않는다(추가 시 V2 마이그레이션).
INSERT INTO terms (code, version, title, required, display_order, active) VALUES
    ('SERVICE',    'v1', '서비스 이용약관 동의',                    TRUE,  1, TRUE),
    ('PRIVACY',    'v1', '개인정보 수집 및 이용 동의',              TRUE,  2, TRUE),
    ('PHONE_AUTH', 'v1', '휴대폰 본인인증 서비스 이용약관 동의',    TRUE,  3, TRUE),
    ('MARKETING',  'v1', '기록 리마인드, 혜택 등 마케팅 정보 수신', FALSE, 4, TRUE),
    ('AGE_14',     'v1', '만 14세 이상입니다',                      TRUE,  5, TRUE);
