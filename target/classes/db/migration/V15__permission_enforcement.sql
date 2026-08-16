-- Wires the roles/permissions system (seeded in V2 but never consulted by any access
-- check) into real enforcement. Adds permissions for every module that previously had
-- no entry here, then grants every role exactly the access it already has today via
-- hardcoded @PreAuthorize lists, so switching enforcement over is behavior-neutral by
-- default — an admin editing a role's permissions afterward is what actually changes
-- anything.

INSERT INTO permissions (name, description, module) VALUES
    ('academic_years:manage', 'Manage academic years and terms',      'ACADEMIC'),
    ('classes:view',          'View classes',                          'CLASSES'),
    ('classes:manage',        'Create / edit / delete classes',        'CLASSES'),
    ('subjects:manage',       'Manage subjects',                       'ACADEMIC'),
    ('admissions:manage',     'Manage admissions',                     'ADMISSIONS'),
    ('discipline:view',       'View discipline records',               'DISCIPLINE'),
    ('discipline:manage',     'Record discipline incidents',           'DISCIPLINE'),
    ('homework:manage',       'Post / edit / delete homework',         'HOMEWORK'),
    ('timetable:manage',      'Edit timetable slots',                  'TIMETABLE'),
    ('report_cards:view',     'Enter report card remarks',             'EXAMS'),
    ('promotions:manage',     'Promote / demote pupils between grades','PROMOTIONS'),
    ('guardians:manage',      'Add / edit / delete parent records',    'GUARDIANS'),
    ('health:manage',         'Manage clinic / health records',        'HEALTH'),
    ('leave:manage',          'Approve / manage staff leave',          'LEAVE'),
    ('procurement:manage',    'Manage purchase orders and suppliers',  'PROCUREMENT'),
    ('inventory:manage',      'Manage inventory / stock',              'INVENTORY'),
    ('ptc:manage',            'Manage parent-teacher conference sessions', 'PTC'),
    ('events:manage',         'Create / edit / delete calendar events','EVENTS'),
    ('documents:manage',      'Upload / delete official documents',    'DOCUMENTS'),
    ('communication:manage',  'Send broadcast SMS / email',            'COMMS'),
    ('accounts:manage',       'Record / edit school expenses',         'ACCOUNTS')
ON CONFLICT (name) DO NOTHING;

-- Group A: teaching staff — day-to-day classroom access.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD','TEACHER','CLASS_TEACHER')
AND p.name IN (
    'pupils:view','pupils:create','pupils:edit','pupils:delete','staff:view',
    'attendance:view','attendance:mark','exams:view','exams:manage',
    'classes:view','discipline:view','discipline:manage','homework:manage',
    'report_cards:view','timetable:manage'
)
ON CONFLICT DO NOTHING;

-- Group B: school administration.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')
AND p.name IN (
    'staff:manage','announcements:post','reports:view','settings:edit',
    'academic_years:manage','admissions:manage','classes:manage','documents:manage',
    'events:manage','guardians:manage','health:manage','inventory:manage',
    'leave:manage','procurement:manage','promotions:manage','ptc:manage',
    'subjects:manage','communication:manage'
)
ON CONFLICT DO NOTHING;

-- Group C: finance.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','HEAD_TEACHER','ADMIN','ACCOUNTANT')
AND p.name IN (
    'fees:view','fees:create','fees:collect','fees:configure',
    'canteen:view','canteen:manage','payroll:view','payroll:manage','accounts:manage'
)
ON CONFLICT DO NOTHING;

-- Group D: super-admin-tier-only (role/permission management, system backups).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN')
AND p.name IN ('backup:create','roles:manage')
ON CONFLICT DO NOTHING;

-- Group E: user account administration and audit trail.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN','HEAD_TEACHER')
AND p.name IN ('users:manage','audit:view')
ON CONFLICT DO NOTHING;

-- Group F: library / transport — previously had no access control at all; this is a
-- conservative default (school admin tier) rather than reproducing the previous
-- "anyone logged in" gap.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')
AND p.name IN ('library:view','library:manage','transport:view','transport:manage')
ON CONFLICT DO NOTHING;

-- Keep SUPER_ADMIN's grant complete for every permission added above (the app also
-- hardcodes an unconditional SUPER_ADMIN bypass as a safety net regardless of this).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
