-- Optional wide school photo shown on the dashboard hero, alongside the existing small logo.
ALTER TABLE school_settings ADD COLUMN IF NOT EXISTS banner_url TEXT;
