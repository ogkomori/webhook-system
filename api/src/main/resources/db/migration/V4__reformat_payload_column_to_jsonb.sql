ALTER TABLE events
    DROP COLUMN payload;

ALTER TABLE events
    ADD payload JSONB;

ALTER TABLE events
    ALTER COLUMN payload SET NOT NULL;