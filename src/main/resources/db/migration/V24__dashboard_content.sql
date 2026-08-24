ALTER TABLE school_settings
    ADD COLUMN IF NOT EXISTS dashboard_hero_heading VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dashboard_hero_subtext TEXT,
    ADD COLUMN IF NOT EXISTS dashboard_hero_image_url TEXT,
    ADD COLUMN IF NOT EXISTS dashboard_button_label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS dashboard_button_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dashboard_links TEXT DEFAULT '[]';

UPDATE school_settings SET dashboard_links = '[]' WHERE dashboard_links IS NULL;
