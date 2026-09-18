ALTER TABLE quests ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_quests_archived ON quests(archived);
