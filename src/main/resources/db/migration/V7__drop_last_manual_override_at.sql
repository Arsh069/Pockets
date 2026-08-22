-- Drop last_manual_override_at column from pockets table
ALTER TABLE pockets
    DROP COLUMN last_manual_override_at;
