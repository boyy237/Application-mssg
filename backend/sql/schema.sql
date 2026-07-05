-- ════════════════════════════════════════════════════════════════
--  CipherTalk — Schéma PostgreSQL
--  Exécuter avec : psql -U postgres -d ciphertalk -f sql/schema.sql
-- ════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    username        VARCHAR(50)  UNIQUE NOT NULL,
    email           VARCHAR(120) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    avatar_color    VARCHAR(7)   NOT NULL DEFAULT '#4af0c4',
    is_online       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_seen       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversations (
    id              SERIAL PRIMARY KEY,
    is_group        BOOLEAN      NOT NULL DEFAULT FALSE,
    name            VARCHAR(100),
    cipher_a        INTEGER      NOT NULL DEFAULT 7,
    cipher_b        INTEGER      NOT NULL DEFAULT 3,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id        INTEGER REFERENCES conversations(id) ON DELETE CASCADE,
    user_id                 INTEGER REFERENCES users(id) ON DELETE CASCADE,
    joined_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_read_message_id    INTEGER,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id              SERIAL PRIMARY KEY,
    conversation_id INTEGER NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cipher_text     TEXT    NOT NULL,
    cipher_a        INTEGER NOT NULL,
    cipher_b        INTEGER NOT NULL,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index utiles pour les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_participants_user ON conversation_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
