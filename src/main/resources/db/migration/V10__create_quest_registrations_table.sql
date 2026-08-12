CREATE TABLE quest_registrations (
                       id              BIGSERIAL PRIMARY KEY,
                       quest_id        BIGINT           NOT NULL,
                       team_id         BIGINT           NOT NULL,
                       status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_quest_registrations_team_id ON quest_registrations(team_id);
CREATE INDEX idx_quest_registrations_quest_id ON quest_registrations(quest_id);

ALTER TABLE quest_registrations ADD CONSTRAINT fk_quest_registrations_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
ALTER TABLE quest_registrations ADD CONSTRAINT fk_quest_registrations_team FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE quest_registrations ADD CONSTRAINT uq_quest_registrations_quest_team UNIQUE (quest_id, team_id);