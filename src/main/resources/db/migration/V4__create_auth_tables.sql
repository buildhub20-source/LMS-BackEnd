-- Module 1: authentication artefacts.
--
-- Only hashes of the refresh, invitation and reset tokens are persisted. The
-- raw token is returned to the client or emailed once and never stored.

CREATE TABLE user_session (
    id                 UUID                     NOT NULL,
    user_id            UUID                     NOT NULL,
    refresh_token_hash VARCHAR(255)             NOT NULL,
    ip_address         VARCHAR(45),
    user_agent         VARCHAR(255),
    is_revoked         BOOLEAN                  NOT NULL DEFAULT FALSE,
    expires_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_used_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_session PRIMARY KEY (id),
    CONSTRAINT uk_user_session_refresh_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_session_user_id ON user_session (user_id);
CREATE INDEX idx_user_session_expires_at ON user_session (expires_at);

CREATE TABLE user_invitation (
    id          UUID                     NOT NULL,
    user_id     UUID                     NOT NULL,
    token_hash  VARCHAR(255)             NOT NULL,
    invited_by  UUID,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_invitation PRIMARY KEY (id),
    CONSTRAINT uk_user_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_invitation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_invitation_invited_by FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_user_invitation_user_id ON user_invitation (user_id);
CREATE INDEX idx_user_invitation_expires_at ON user_invitation (expires_at);

CREATE TABLE password_reset_token (
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    token_hash VARCHAR(255)             NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used    BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_password_reset_token PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token (user_id);
