ALTER TABLE bikes
    ADD COLUMN publication_requested_at TIMESTAMPTZ;

-- Bikes sent to moderation before this column existed keep their creation time
-- as the closest known approximation of when publication was requested.
UPDATE bikes
SET publication_requested_at = created_at
WHERE status <> 'PRIVATE';
