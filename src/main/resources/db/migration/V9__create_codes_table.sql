CREATE TABLE codes (
                         id              BIGSERIAL PRIMARY KEY,
                         level_id        BIGINT           NOT NULL,
                         code_value      VARCHAR(255)     NOT NULL,
                         type            VARCHAR(255)     NOT NULL,
                         points          INTEGER         NOT NULL,
                         created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_codes_level_id ON codes(level_id);
CREATE INDEX idx_codes_value ON codes(code_value);

ALTER TABLE codes ADD CONSTRAINT fk_codes_level FOREIGN KEY (level_id) REFERENCES levels(id);
