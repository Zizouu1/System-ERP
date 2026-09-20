-- Remove legacy boolean `modified` flags and ensure timestamp-based modification tracking.

ALTER TABLE IF EXISTS incoming_materials
    DROP COLUMN IF EXISTS modified,
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;

ALTER TABLE IF EXISTS outgoing
    DROP COLUMN IF EXISTS modified,
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;

ALTER TABLE IF EXISTS psf_production
    DROP COLUMN IF EXISTS modified,
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;

ALTER TABLE IF EXISTS pf_production
    DROP COLUMN IF EXISTS modified,
    ADD COLUMN IF NOT EXISTS last_modified_at TIMESTAMP;
