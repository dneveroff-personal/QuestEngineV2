CREATE TABLE team_join_requests (
    id              BIGSERIAL PRIMARY KEY,
    team_id         BIGINT           NOT NULL,
    user_id         BIGINT           NOT NULL,
    status          VARCHAR(16)      NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_join_requests_team_id ON team_join_requests(team_id);
CREATE INDEX idx_team_join_requests_user_id ON team_join_requests(user_id);
CREATE INDEX idx_team_join_requests_status ON team_join_requests(status);

ALTER TABLE team_join_requests ADD CONSTRAINT fk_team_join_requests_team FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE team_join_requests ADD CONSTRAINT fk_team_join_requests_user FOREIGN KEY (user_id) REFERENCES users(id);