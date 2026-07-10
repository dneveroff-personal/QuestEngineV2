CREATE TABLE teams (
                       id              BIGSERIAL PRIMARY KEY,
                       name            VARCHAR(255)     NOT NULL UNIQUE,
                       captain_id      BIGINT           NOT NULL,
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_teams_name             ON teams(name);

ALTER TABLE teams ADD CONSTRAINT fk_teams_user FOREIGN KEY (captain_id) REFERENCES users(id);
