-- Drop existing foreign keys with RESTRICT delete behavior
ALTER TABLE pockets
    DROP CONSTRAINT fk_pockets_user_id;

ALTER TABLE transactions
    DROP CONSTRAINT fk_transactions_user_id;

ALTER TABLE transactions
    DROP CONSTRAINT fk_transactions_pocket_id;

-- Re-create foreign keys with ON DELETE CASCADE
ALTER TABLE pockets
    ADD CONSTRAINT fk_pockets_user_id
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_user_id
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_pocket_id
    FOREIGN KEY (pocket_id) REFERENCES pockets (id)
    ON DELETE CASCADE;
