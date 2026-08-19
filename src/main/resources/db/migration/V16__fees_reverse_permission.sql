-- Splits "correct/delete a recorded payment or payslip" out from ordinary fee collection /
-- payroll management: today both share the fees:collect / payroll:manage permission, so any
-- Accountant can silently rewrite payment or pay history. These are granted only to the
-- admin tier by default (Accountants keep fees:collect / payroll:manage for normal work).

INSERT INTO permissions (name, description, module) VALUES
    ('fees:reverse', 'Edit, delete or reverse a recorded payment', 'FEES'),
    ('payroll:reverse', 'Edit or delete a recorded payslip', 'PAYROLL')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER')
AND p.name IN ('fees:reverse', 'payroll:reverse')
ON CONFLICT DO NOTHING;
