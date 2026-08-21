-- Add version column to pockets for JPA optimistic locking
ALTER TABLE pockets
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
