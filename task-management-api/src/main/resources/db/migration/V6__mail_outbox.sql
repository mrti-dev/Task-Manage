CREATE TABLE mail_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kind VARCHAR(32) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    claimed_until DATETIME NULL,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_mail_outbox_due ON mail_outbox(status, next_attempt_at, claimed_until);
