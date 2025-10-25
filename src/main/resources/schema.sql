-- DROP ALL (idempotent-ish): curăță tabelele dacă există (pentru rulări manuale)
DROP TABLE IF EXISTS auth_tokens;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;

-- USERS
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL
);

-- USER ROLES (ElementCollection)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role VARCHAR(64) NOT NULL,
                            PRIMARY KEY (user_id, role)
);

-- AUTH TOKENS (opaque token hash + expirare)
CREATE TABLE auth_tokens (
                             id BIGSERIAL PRIMARY KEY,
                             token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hex al tokenului
                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             created_at TIMESTAMPTZ NOT NULL,
                             expires_at TIMESTAMPTZ NOT NULL,
                             revoked BOOLEAN NOT NULL DEFAULT FALSE
);

-- INDEXURI UTILE
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_authtoken_expires ON auth_tokens(expires_at);

-- (OPȚIONAL) SEED ADMIN
-- ATENȚIE: pune aici un hash BCrypt real generat în aplicație (PasswordEncoder).
-- Exemplu de inserare când ai deja hash-ul (înlocuiește <BCRYPT_HASH>):
-- INSERT INTO users (email, password) VALUES ('admin@restaurant.local', '<BCRYPT_HASH>');
-- INSERT INTO user_roles (user_id, role) SELECT id, 'ROLE_ADMIN' FROM users WHERE email='admin@restaurant.local';

-- Pentru demo, recomand să creezi userii prin endpoint-ul /api/auth/register
