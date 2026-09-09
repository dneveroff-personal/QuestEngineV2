CREATE TABLE manual_time_adjustments (
                         id                  BIGSERIAL PRIMARY KEY,
                         quest_progress_id   BIGINT           NOT NULL,
                         type                VARCHAR(20)      NOT NULL,
                         seconds             INTEGER          NOT NULL,
                         reason              VARCHAR(1000)    NOT NULL,
                         created_by          BIGINT           NOT NULL,
                         created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         revoked_at          TIMESTAMP WITH TIME ZONE,
                         revoked_by          BIGINT
);

ALTER TABLE manual_time_adjustments ADD CONSTRAINT fk_manual_time_adjustments_quest_progress FOREIGN KEY (quest_progress_id) REFERENCES quest_progress(id);
ALTER TABLE manual_time_adjustments ADD CONSTRAINT fk_manual_time_adjustments_created_by FOREIGN KEY (created_by) REFERENCES users(id);
ALTER TABLE manual_time_adjustments ADD CONSTRAINT fk_manual_time_adjustments_revoked_by FOREIGN KEY (revoked_by) REFERENCES users(id);
ALTER TABLE manual_time_adjustments ADD CONSTRAINT chk_manual_time_adjustments_seconds_positive CHECK (seconds > 0);
ALTER TABLE manual_time_adjustments ADD CONSTRAINT chk_manual_time_adjustments_type CHECK (type IN ('BONUS', 'PENALTY'));

-- Основной путь чтения: агрегация активных (не отозванных) корректировок по QuestProgress
-- (BonusPenaltyServiceImpl.getAdjustmentSeconds) — частичный индекс держит его быстрым
-- независимо от накопленной истории отозванных записей.
CREATE INDEX idx_manual_time_adjustments_active ON manual_time_adjustments(quest_progress_id, type) WHERE revoked_at IS NULL;

-- Список всех корректировок (включая отозванные) для конкретного QuestProgress — аудит/UI автора.
CREATE INDEX idx_manual_time_adjustments_quest_progress ON manual_time_adjustments(quest_progress_id);
