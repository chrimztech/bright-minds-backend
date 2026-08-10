-- Fee items: add category column
ALTER TABLE fee_items ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'SCHOOL_FEE';

-- Users: add must_change_password flag (set true when admin creates account)
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- Dynamic roles (separate from the AppRole enum — for admin-created custom roles)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    module VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Seed system roles (mirror AppRole enum)
INSERT INTO roles (name, description, is_system) VALUES
    ('SUPER_ADMIN', 'Super Administrator with full system access', TRUE),
    ('HEAD_TEACHER', 'Head Teacher', TRUE),
    ('DEPUTY_HEAD', 'Deputy Head Teacher', TRUE),
    ('ADMIN', 'School Administrator', TRUE),
    ('ACCOUNTANT', 'Accountant - manages fees and finances', TRUE),
    ('TEACHER', 'Teacher', TRUE),
    ('CLASS_TEACHER', 'Class Teacher', TRUE),
    ('PARENT', 'Parent / Guardian', TRUE),
    ('LIBRARIAN', 'Librarian', TRUE),
    ('STORE_OFFICER', 'Store Officer', TRUE),
    ('TRANSPORT_OFFICER', 'Transport Officer', TRUE),
    ('NURSE', 'School Nurse', TRUE),
    ('SECURITY', 'Security Officer', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Seed permissions by module
INSERT INTO permissions (name, description, module) VALUES
    ('pupils:view',        'View pupil records',          'PUPILS'),
    ('pupils:create',      'Enrol new pupils',            'PUPILS'),
    ('pupils:edit',        'Edit pupil records',          'PUPILS'),
    ('pupils:delete',      'Delete pupil records',        'PUPILS'),
    ('staff:view',         'View staff records',          'STAFF'),
    ('staff:manage',       'Add / edit / delete staff',   'STAFF'),
    ('fees:view',          'View fee items and invoices', 'FEES'),
    ('fees:create',        'Create invoices',             'FEES'),
    ('fees:collect',       'Record payments',             'FEES'),
    ('fees:configure',     'Configure fee items',         'FEES'),
    ('attendance:view',    'View attendance records',     'ATTENDANCE'),
    ('attendance:mark',    'Mark attendance',             'ATTENDANCE'),
    ('exams:view',         'View exams and marks',        'EXAMS'),
    ('exams:manage',       'Create exams and enter marks','EXAMS'),
    ('library:view',       'View library',                'LIBRARY'),
    ('library:manage',     'Manage books and loans',      'LIBRARY'),
    ('transport:view',     'View transport',              'TRANSPORT'),
    ('transport:manage',   'Manage vehicles and routes',  'TRANSPORT'),
    ('canteen:view',       'View canteen',                'CANTEEN'),
    ('canteen:manage',     'Manage canteen',              'CANTEEN'),
    ('announcements:view', 'View announcements',          'COMMS'),
    ('announcements:post', 'Post announcements',          'COMMS'),
    ('payroll:view',       'View payroll',                'PAYROLL'),
    ('payroll:manage',     'Manage payroll',              'PAYROLL'),
    ('reports:view',       'View reports',                'REPORTS'),
    ('backup:create',      'Create system backup',        'ADMIN'),
    ('settings:edit',      'Edit school settings',        'ADMIN'),
    ('users:manage',       'Manage user accounts',        'ADMIN'),
    ('roles:manage',       'Manage roles and permissions','ADMIN'),
    ('audit:view',         'View audit logs',             'ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to SUPER_ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT DO NOTHING;
