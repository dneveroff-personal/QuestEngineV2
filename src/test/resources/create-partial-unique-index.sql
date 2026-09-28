-- Частичный UNIQUE-индекс для BONUS/PENALTY кодов (V18)
-- Гарантирует, что BONUS/PENALTY-код можно применить только один раз на LevelProgress
CREATE UNIQUE INDEX IF NOT EXISTS uq_code_submissions_lp_matched_bonus_penalty
    ON code_submissions (level_progress_id, matched_code_id)
    WHERE result IN ('CORRECT_BONUS', 'CORRECT_PENALTY')
      AND matched_code_id IS NOT NULL;