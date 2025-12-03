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

-- RESTAURANT TABLES (mese)
CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    table_number INTEGER NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    xPosition DOUBLE PRECISION DEFAULT 0,
    yPosition DOUBLE PRECISION DEFAULT 0,
    width DOUBLE PRECISION NOT NULL DEFAULT 100,
    height DOUBLE PRECISION NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RESERVATIONS (rezervări)
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT NOT NULL REFERENCES restaurant_tables(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    customer_email VARCHAR(255),
    party_size INTEGER NOT NULL CHECK (party_size > 0),
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_time_range CHECK (end_time > start_time)
);

-- INDEXURI PENTRU REZERVĂRI
CREATE INDEX IF NOT EXISTS idx_reservations_table ON reservations(table_id);
CREATE INDEX IF NOT EXISTS idx_reservations_user ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_date ON reservations(reservation_date);
CREATE INDEX IF NOT EXISTS idx_reservations_datetime ON reservations(reservation_date, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_restaurant_tables_active ON restaurant_tables(is_active);
