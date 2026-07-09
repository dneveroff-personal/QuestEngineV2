CREATE TABLE team_members (
                       id              BIGSERIAL PRIMARY KEY,
                       team_id         BIGINT           NOT NULL,
                       user_id         BIGINT           NOT NULL,
                       role            VARCHAR(16)     NOT NULL DEFAULT 'MEMBER',
                       joined_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_members_team_id ON team_members(team_id);
CREATE INDEX idx_team_members_user_id ON team_members(user_id);

ALTER TABLE team_members ADD CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE team_members ADD CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users(id);
