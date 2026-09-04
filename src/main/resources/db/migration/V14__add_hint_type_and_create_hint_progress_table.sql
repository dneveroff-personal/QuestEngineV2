CREATE TABLE hint_progress (
                         id                  BIGSERIAL        PRIMARY KEY,
                         level_progress_id   BIGINT           NOT NULL,
                         hint_id             BIGINT           NOT NULL,
                         shown_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE hint_progress ADD CONSTRAINT fk_hint_progress_level_progress FOREIGN KEY (level_progress_id) REFERENCES level_progress(id);
ALTER TABLE hint_progress ADD CONSTRAINT fk_hint_progress_hint FOREIGN KEY (hint_id) REFERENCES hints(id);
ALTER TABLE hint_progress ADD CONSTRAINT uq_hint_progress_level_progress_hint UNIQUE (level_progress_id, hint_id);

CREATE INDEX idx_hint_progress_level_progress ON hint_progress(level_progress_id);
