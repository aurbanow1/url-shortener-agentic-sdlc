-- V1: the first stored resource (link) and the write side of the audit trail (audit_log).
-- rollback: DROP TABLE audit_log; DROP TABLE link;  -- pre-production baseline; destroys all rows

CREATE TABLE link (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(32)   NOT NULL,
    url             VARCHAR(2048) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    retired_at      TIMESTAMP WITH TIME ZONE,
    idempotency_key VARCHAR(255),
    CONSTRAINT uq_link_code            UNIQUE (code),
    CONSTRAINT uq_link_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_link_code_length     CHECK (LENGTH(code) >= 6),
    CONSTRAINT ck_link_url_not_empty   CHECK (url <> '')
);

CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    actor        VARCHAR(64)   NOT NULL,
    action       VARCHAR(64)   NOT NULL,
    entity       VARCHAR(32)   NOT NULL,
    entity_id    VARCHAR(64)   NOT NULL,
    request_id   VARCHAR(64)   NOT NULL,
    before_state VARCHAR(4096),
    after_state  VARCHAR(4096) NOT NULL
);
