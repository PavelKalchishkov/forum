CREATE TABLE replies (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    content VARCHAR(10000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_replies_post_id ON replies (post_id);
