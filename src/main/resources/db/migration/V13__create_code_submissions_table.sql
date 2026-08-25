CREATE TABLE code_submissions (
                         id                  BIGSERIAL PRIMARY KEY,
                         level_progress_id   BIGINT           NOT NULL,
                         submitted_by        BIGINT           NOT NULL,
                         raw_value           VARCHAR(255)     NOT NULL,
                         matched_code_id     BIGINT,
                         result              VARCHAR(50)      NOT NULL,
                         submitted_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE code_submissions ADD CONSTRAINT fk_code_submissions_level_progress FOREIGN KEY (level_progress_id) REFERENCES level_progress(id);
ALTER TABLE code_submissions ADD CONSTRAINT fk_code_submissions_submitted_by FOREIGN KEY (submitted_by) REFERENCES users(id);
ALTER TABLE code_submissions ADD CONSTRAINT fk_code_submissions_matched_code FOREIGN KEY (matched_code_id) REFERENCES codes(id);

-- Общий индекс для истории попыток команды на конкретном уровне (аудит, антибрутфорс-анализ постфактум).
CREATE INDEX idx_code_submissions_level_progress ON code_submissions(level_progress_id);

-- Частичный индекс: держит атомарную проверку порога (Сценарий 6) быстрой независимо от количества
-- неверных попыток на LevelProgress (rate limiting намеренно отсутствует, см. ADR-0016) —
-- индекс покрывает только строки с верным MAIN-результатом, их всегда мало (не больше числа кодов уровня).
CREATE INDEX idx_code_submissions_lp_correct_main ON code_submissions(level_progress_id) WHERE result = 'CORRECT_MAIN';
