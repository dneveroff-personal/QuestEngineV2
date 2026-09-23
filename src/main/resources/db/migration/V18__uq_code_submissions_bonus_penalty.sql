-- Сценарий 3 (concurrency-scenarios.md): один BONUS/PENALTY-код — ровно один раз
-- на LevelProgress. Частичный UNIQUE закрывает гонку read-then-write в
-- CodeSubmissionServiceImpl (exists + INSERT без блокировки).
-- MAIN-коды намеренно не включены: повторный ввод того же MAIN допустим (аудит + порог по DISTINCT).
CREATE UNIQUE INDEX uq_code_submissions_lp_matched_bonus_penalty
    ON code_submissions (level_progress_id, matched_code_id)
    WHERE result IN ('CORRECT_BONUS', 'CORRECT_PENALTY')
      AND matched_code_id IS NOT NULL;
