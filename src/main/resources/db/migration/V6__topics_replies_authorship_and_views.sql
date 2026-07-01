ALTER TABLE posts
    ADD COLUMN author_id BIGINT REFERENCES users (id),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

UPDATE posts SET updated_at = created_at WHERE updated_at IS NULL;

CREATE UNIQUE INDEX idx_posts_title_unique ON posts (LOWER(title));

ALTER TABLE replies
    ADD COLUMN author_id BIGINT REFERENCES users (id),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE replies SET updated_at = created_at WHERE updated_at IS NULL;
