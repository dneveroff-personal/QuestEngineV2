CREATE TABLE level_progress (
                                 id                      BIGSERIAL PRIMARY KEY,
                                 quest_progress_id       BIGINT           NOT NULL,
                                 level_id                BIGINT           NOT NULL,
                                 status                  VARCHAR(16),
                                 opened_at               TIMESTAMP WITH TIME ZONE,
                                 completed_at            TIMESTAMP WITH TIME ZONE,
                                 auto_transition_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_level_progress_quest_progress_id ON level_progress(quest_progress_id);
CREATE INDEX idx_level_progress_level_id ON level_progress(level_id);

ALTER TABLE level_progress ADD CONSTRAINT fk_level_progress_quest_progress FOREIGN KEY (quest_progress_id) REFERENCES quest_progress(id);
ALTER TABLE level_progress ADD CONSTRAINT fk_level_progress_level FOREIGN KEY (level_id) REFERENCES levels(id);
