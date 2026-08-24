ALTER TABLE school_settings
    ADD COLUMN IF NOT EXISTS login_button_label VARCHAR(255),
    ADD COLUMN IF NOT EXISTS login_button_url VARCHAR(500);
