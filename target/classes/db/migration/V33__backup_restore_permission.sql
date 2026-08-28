-- Restoring a backup drops and recreates the entire live schema and overwrites uploaded
-- files — genuinely destructive, unlike backup:create (read-only export). Kept on its own
-- permission, granted only to SUPER_ADMIN by default (not even ADMIN), so it can't be handed
-- out via Roles & Permissions without a conscious, separate decision.
INSERT INTO permissions (name, description, module) VALUES
    ('backup:restore', 'Restore the system from a backup archive (destructive)', 'SYSTEM')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.name = 'backup:restore'
ON CONFLICT DO NOTHING;
