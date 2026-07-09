CREATE TABLE quests (
                       id              BIGSERIAL PRIMARY KEY,
                       title           VARCHAR(255)     NOT NULL UNIQUE,
                       description     TEXT,
                       type            VARCHAR(16)     NOT NULL DEFAULT 'TEAM',
                       status          VARCHAR(16)     NOT NULL DEFAULT 'DRAFT',
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       started_at      TIMESTAMP WITH TIME ZONE,
                       end_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_quests_title            ON quests(title);
CREATE INDEX idx_quests_status           ON quests(status);
