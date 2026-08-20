-- Module 1: security telemetry.
--
-- login_attempt records both successes and failures and drives the lockout
-- policy. audit_log is the append-only record of security-relevant actions.

CREATE TABLE login_attempt (
    id           UUID                     NOT NULL,
    user_id      UUID,
    email        VARCHAR(255)             NOT NULL,
    ip_address   VARCHAR(45)              NOT NULL,
    success      BOOLEAN                  NOT NULL,
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_login_attempt PRIMARY KEY (id),
    CONSTRAINT fk_login_attempt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_login_attempt_email_attempted_at ON login_attempt (email, attempted_at);

CREATE TABLE audit_log (
    id          UUID                     NOT NULL,
    user_id     UUID,
    action      VARCHAR(100)             NOT NULL,
    resource    VARCHAR(100)             NOT NULL,
    resource_id UUID,
    details     TEXT,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(255),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_resource_created_at ON audit_log (resource, created_at);
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
