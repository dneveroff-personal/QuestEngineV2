CREATE TABLE quest_authors (
                        id              BIGSERIAL PRIMARY KEY,
                        quest_id        BIGINT           NOT NULL,
                        user_id         BIGINT           NOT NULL,
                        created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quest_authors_quest_id ON quest_authors(quest_id);
CREATE INDEX idx_quest_authors_user_id ON quest_authors(user_id);

ALTER TABLE quest_authors ADD CONSTRAINT fk_quest_authors_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
ALTER TABLE quest_authors ADD CONSTRAINT fk_quest_authors_user FOREIGN KEY (user_id) REFERENCES users(id);