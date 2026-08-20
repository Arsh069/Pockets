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
