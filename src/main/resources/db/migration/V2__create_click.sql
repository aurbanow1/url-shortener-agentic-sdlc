-- V2: click events for analytics (02-analytics), reduced before they are written:
-- no raw address, user agent, referrer path or request id is ever stored (NFR-P1).
-- The closed set of user-agent classes is a lookup table, not CHECK (... IN ...): on H2 2.4.240 a
-- CHECK built from a multi-value condition stops working once the connection that created it closes.
-- rollback: DROP TABLE click; DROP TABLE user_agent_class;  -- click history only; V1 untouched

CREATE TABLE user_agent_class (
    token VARCHAR(16) PRIMARY KEY
);

INSERT INTO user_agent_class (token) VALUES ('browser'), ('bot'), ('other'), ('unknown');

CREATE TABLE click (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    link_id          BIGINT        NOT NULL,
    clicked_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    clicked_on       DATE          NOT NULL,
    referrer         VARCHAR(2048),
    user_agent_class VARCHAR(16)   NOT NULL,
    client_hash      VARCHAR(64)   NOT NULL,
    CONSTRAINT fk_click_link FOREIGN KEY (link_id) REFERENCES link (id) ON DELETE CASCADE,
    CONSTRAINT fk_click_user_agent_class FOREIGN KEY (user_agent_class) REFERENCES user_agent_class (token),
    CONSTRAINT ck_click_client_hash_length CHECK (LENGTH(client_hash) = 64)
);

CREATE INDEX ix_click_link_day ON click (link_id, clicked_on);
