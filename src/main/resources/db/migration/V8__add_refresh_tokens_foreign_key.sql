-- 1. Clean up any currently-orphaned refresh_tokens rows where user_id is no longer present in users
DELETE FROM refresh_tokens
WHERE user_id NOT IN (SELECT id FROM users);

-- 2. Add foreign key constraint with ON DELETE CASCADE
ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_user_id
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE;
