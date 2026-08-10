-- Hibernate maps String fields as VARCHAR/TEXT; PostgreSQL jsonb rejects implicit VARCHAR casts.
-- Convert jsonb columns to TEXT so Hibernate can write them without casting.
ALTER TABLE audit_logs    ALTER COLUMN details       TYPE TEXT USING details::text;
ALTER TABLE school_settings ALTER COLUMN grading_scale TYPE TEXT USING grading_scale::text;
