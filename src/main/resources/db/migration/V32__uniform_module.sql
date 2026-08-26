-- New Operations module: uniform/attire catalog + sales, reusing fee_items (category
-- 'UNIFORM') and invoices so it plugs straight into existing Fees & Payments / Accounts
-- reporting rather than a parallel accounting path.

INSERT INTO permissions (name, description, module) VALUES
    ('uniform:view',   'View uniform catalog and sales', 'UNIFORM'),
    ('uniform:manage', 'Manage uniform catalog and record sales', 'UNIFORM')
ON CONFLICT (name) DO NOTHING;

-- Same admin + finance tier as canteen:view/manage (Group C/F in V15) — a school-admin-tier
-- default since uniform sales create real invoices against a pupil's account.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER', 'DEPUTY_HEAD', 'ACCOUNTANT')
AND p.name IN ('uniform:view', 'uniform:manage')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
AND p.name IN ('uniform:view', 'uniform:manage')
ON CONFLICT DO NOTHING;

-- Default catalog — schools set real prices via the Uniform page; amount starts at 0 rather
-- than a guessed figure.
INSERT INTO fee_items (id, name, category, amount, is_recurring, created_at)
SELECT gen_random_uuid(), name, 'UNIFORM', 0, false, now()
FROM (VALUES ('Uniform'), ('P.E. Attire'), ('Jersey'), ('Socks'), ('Other')) AS defaults(name)
WHERE NOT EXISTS (
    SELECT 1 FROM fee_items f WHERE f.category = 'UNIFORM' AND f.name = defaults.name
);
