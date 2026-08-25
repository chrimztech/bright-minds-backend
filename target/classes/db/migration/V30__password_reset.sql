ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_token_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_reset_token ON app_users (reset_token) WHERE reset_token IS NOT NULL;
