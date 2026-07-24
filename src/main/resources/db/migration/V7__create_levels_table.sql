CREATE TABLE levels (
                        id              BIGSERIAL PRIMARY KEY,
                        quest_id        BIGINT           NOT NULL,
                        title           VARCHAR(255)     NOT NULL,
                        order_idx       INTEGER         NOT NULL,
                        content         TEXT,
                        timeout         INTEGER,
                        created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_levels_quest_id ON levels(quest_id);

ALTER TABLE levels ADD CONSTRAINT fk_levels_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
ALTER TABLE levels ADD CONSTRAINT uq_levels_quest_order UNIQUE (quest_id, order_idx);