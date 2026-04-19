-- Default password for user "admin" is "admin" (change after first login in production).
UPDATE users
SET password_hash = '$2a$10$k/5TFC9kQu4R4QAtp0GXkeFQQ7G1Xjk2ZrXKHFGzsOy7GLsiZQGVm'
WHERE username = 'admin'
  AND password_hash IS NULL;
