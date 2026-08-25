CREATE TABLE inquiries (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO permissions (name, description, module) VALUES
    ('inquiries:view', 'View inquiries submitted from the public website', 'INQUIRIES'),
    ('inquiries:manage', 'Manage (clear/respond to) inquiries submitted from the public website', 'INQUIRIES')
ON CONFLICT (name) DO NOTHING;

-- Front-office tier by default — same roles the school would actually field enquiries.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER', 'DEPUTY_HEAD')
AND p.name IN ('inquiries:view', 'inquiries:manage')
ON CONFLICT DO NOTHING;
