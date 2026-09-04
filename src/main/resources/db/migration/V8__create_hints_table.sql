CREATE TABLE hints (
                         id              BIGSERIAL       PRIMARY KEY,
                         level_id        BIGINT          NOT NULL,
                         order_idx       INTEGER         NOT NULL,
                         delay_seconds   INTEGER         NOT NULL,
                         content         TEXT            NOT NULL,
                         type            VARCHAR(20)     NOT NULL,
                         bonus_penalty_seconds INTEGER,
                         created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hints_level_id ON hints(level_id);

ALTER TABLE hints ADD CONSTRAINT fk_hints_level FOREIGN KEY (level_id) REFERENCES levels(id);
ALTER TABLE hints ADD CONSTRAINT uq_hints_level_order UNIQUE (level_id, order_idx);
