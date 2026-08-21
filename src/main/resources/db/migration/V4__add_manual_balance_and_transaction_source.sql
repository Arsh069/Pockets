-- Add manual_current_balance column to pockets with existing current_balance backfill
ALTER TABLE pockets
    ADD COLUMN manual_current_balance NUMERIC(19, 4);

UPDATE pockets
    SET manual_current_balance = current_balance
    WHERE manual_current_balance IS NULL;

ALTER TABLE pockets
    ALTER COLUMN manual_current_balance SET NOT NULL;

-- Add source column to transactions and backfill existing rows to 'IN_APP'
ALTER TABLE transactions
    ADD COLUMN source VARCHAR(255);

UPDATE transactions
    SET source = 'IN_APP'
    WHERE source IS NULL;

ALTER TABLE transactions
    ALTER COLUMN source SET NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transaction_source
    CHECK (source IN ('IN_APP', 'MANUAL_LOG'));

-- Allow manual transactions without idempotency keys
ALTER TABLE transactions
    ALTER COLUMN idempotency_key DROP NOT NULL;
