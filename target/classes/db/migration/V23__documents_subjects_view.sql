-- Documents and Subjects were left open to any authenticated user with no real permission
-- backing that at all (unlike Library, which had a seeded but unenforced library:view).
-- Documents in particular can hold sensitive contracts/HR records, so "open to everyone,
-- unconditionally, forever" isn't something an admin could ever tighten via Roles &
-- Permissions. This makes it a real, grantable/revocable permission instead — granted
-- broadly by default (every staff-facing role) to preserve today's actual behavior, but now
-- an admin can restrict it for a specific role if they choose to.
INSERT INTO permissions (name, description, module) VALUES
    ('documents:view', 'View documents', 'DOCUMENTS'),
    ('subjects:view', 'View subjects', 'SUBJECTS')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN (
    'SUPER_ADMIN','HEAD_TEACHER','DEPUTY_HEAD','ADMIN','ACCOUNTANT',
    'TEACHER','CLASS_TEACHER','LIBRARIAN','STORE_OFFICER','TRANSPORT_OFFICER','NURSE','SECURITY'
)
AND p.name IN ('documents:view', 'subjects:view')
ON CONFLICT DO NOTHING;
