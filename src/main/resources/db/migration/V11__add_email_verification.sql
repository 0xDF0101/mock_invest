ALTER TABLE users
    ADD COLUMN email_verified     TINYINT(1)   NOT NULL DEFAULT 0,
    ADD COLUMN verification_token VARCHAR(100) NULL;

-- 기존 가입 사용자는 인증된 것으로 처리
UPDATE users SET email_verified = 1;
