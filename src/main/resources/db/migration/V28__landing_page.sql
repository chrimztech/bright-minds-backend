ALTER TABLE school_settings
    ADD COLUMN IF NOT EXISTS landing_hero_heading VARCHAR(255),
    ADD COLUMN IF NOT EXISTS landing_hero_subtext TEXT,
    ADD COLUMN IF NOT EXISTS landing_hero_images TEXT DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS landing_about_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS landing_about_body TEXT;

UPDATE school_settings SET landing_hero_images = '[]' WHERE landing_hero_images IS NULL;
