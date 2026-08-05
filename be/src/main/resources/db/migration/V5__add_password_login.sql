-- 이메일·비밀번호 자체 가입.
--
-- 소셜 계정과 같은 users 행을 쓴다. 별도 테이블로 쪼개지 않는 이유는 유저당 자격증명이
-- 0..1 개뿐이라 조인만 늘고 얻는 게 없기 때문이다. 소셜 전용 계정은 이 값이 NULL 이다.
--
-- 길이 255: BCrypt 해시는 60자지만 앞에 알고리즘 접두사가 붙는다({bcrypt}$2a$10$...).
-- 나중에 argon2 같은 더 긴 해시로 갈아탈 때 마이그레이션 없이 들어가도록 여유를 둔다.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NULL AFTER email;
