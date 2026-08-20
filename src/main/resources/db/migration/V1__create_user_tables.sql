-- Module 1: core user accounts and account status tracking.
--
-- password is NULL until the invited user completes account activation, which
-- is why the column is nullable. is_active gates authentication.

CREATE TABLE users (
    id                UUID                     NOT NULL,
    name              VARCHAR(100)             NOT NULL,
    email             VARCHAR(255)             NOT NULL,
    password          VARCHAR(255),
    phone             VARCHAR(20),
    profile_image_url VARCHAR(500),
    is_active         BOOLEAN                  NOT NULL DEFAULT FALSE,
    is_locked         BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    -- The application lowercases every address before writing it and looks
    -- accounts up with lower(email). UNIQUE (email) alone would still allow a
    -- direct insert to create both Foo@x.com and foo@x.com; refusing anything
    -- but lowercase makes that impossible, and unlike a functional unique index
    -- it is portable to the H2 instance the migration test runs against.
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);

CREATE INDEX idx_users_is_active ON users (is_active);


-- Append-only record of every account status transition.
CREATE TABLE account_status_history (
    id         UUID                     NOT NULL,
    user_id    UUID                     NOT NULL,
    status     VARCHAR(30)              NOT NULL,
    changed_by UUID,
    reason     VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_account_status_history PRIMARY KEY (id),
    CONSTRAINT fk_account_status_history_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_status_history_changed_by
        FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_account_status_history_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED', 'DELETED'))
);

CREATE INDEX idx_account_status_history_user_id ON account_status_history (user_id);
CREATE INDEX idx_account_status_history_changed_at ON account_status_history (changed_at);
