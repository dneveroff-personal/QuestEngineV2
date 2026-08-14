CREATE TABLE quest_progress (
                       id              BIGSERIAL PRIMARY KEY,
                       quest_id        BIGINT           NOT NULL,
                       team_id         BIGINT           NOT NULL,
                       status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING',
                       quest_started_at      TIMESTAMP WITH TIME ZONE,
                       entered_at      TIMESTAMP WITH TIME ZONE,
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       finished_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_quest_progress_team_id ON quest_progress(team_id);
CREATE INDEX idx_quest_progress_quest_id ON quest_progress(quest_id);

ALTER TABLE quest_progress ADD CONSTRAINT fk_quest_progress_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
ALTER TABLE quest_progress ADD CONSTRAINT fk_quest_progress_team FOREIGN KEY (team_id) REFERENCES teams(id);
ALTER TABLE quest_progress ADD CONSTRAINT uq_quest_progress_quest_team UNIQUE (quest_id, team_id);