-- Add QR payload fields and expiry timestamp to transactions
ALTER TABLE transactions
    ADD COLUMN raw_qr_payload TEXT,
    ADD COLUMN amount_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN payee_name VARCHAR(255),
    ADD COLUMN expires_at TIMESTAMP(6) WITH TIME ZONE;

-- Update status check constraint to include EXPIRED
ALTER TABLE transactions
    DROP CONSTRAINT transactions_status_check;

ALTER TABLE transactions
    ADD CONSTRAINT transactions_status_check
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'FAILED', 'EXPIRED'));
