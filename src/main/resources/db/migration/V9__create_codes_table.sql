CREATE TABLE codes (
                         id              BIGSERIAL PRIMARY KEY,
                         level_id        BIGINT           NOT NULL,
                         code_value      VARCHAR(255)     NOT NULL,
                         code_index     INTEGER,
                         type            VARCHAR(255)     NOT NULL,
                         points          INTEGER,
                         created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_codes_level_id ON codes(level_id);
CREATE INDEX idx_codes_value ON codes(code_value);

ALTER TABLE codes ADD CONSTRAINT fk_codes_level FOREIGN KEY (level_id) REFERENCES levels(id);
ALTER TABLE codes ADD CONSTRAINT uq_codes_level_id_code_value UNIQUE (level_id, code_value);
