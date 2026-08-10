-- Hibernate uses @Enumerated(EnumType.STRING) which binds VARCHAR.
-- PostgreSQL native enum types reject implicit VARCHAR casts, so convert to VARCHAR.
ALTER TABLE user_roles ALTER COLUMN role TYPE VARCHAR(50) USING role::text;
