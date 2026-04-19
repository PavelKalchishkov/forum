-- Default administrator (API has no password field; add auth separately if needed.)
INSERT INTO users (username, email, role, created_at)
SELECT 'admin', 'admin@forum.local', 'ADMIN', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
