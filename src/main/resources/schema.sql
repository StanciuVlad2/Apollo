-- USERS
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE
    );

-- USER ROLES (ElementCollection)
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role)
    );

-- AUTH TOKENS (opaque token hash + expirare)
CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
    );

-- EMAIL VERIFICATION TOKENS
CREATE TABLE IF NOT EXISTS email_verification_tokens (
                                                         id BIGSERIAL PRIMARY KEY,
                                                         token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
    );

-- INDEXURI UTILE
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_authtoken_expires ON auth_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email_verification_user ON email_verification_tokens(user_id);
