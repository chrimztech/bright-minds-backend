-- Same split as V16 for fees/payroll: correcting or deleting a recorded expense is admin-tier
-- only by default, separate from the accounts:manage permission used for normal recording.
INSERT INTO permissions (name, description, module) VALUES
    ('accounts:reverse', 'Edit or delete a recorded expense', 'ACCOUNTS')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER')
AND p.name = 'accounts:reverse'
ON CONFLICT DO NOTHING;
