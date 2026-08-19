-- Registration fee collected at the application/interview stage is a separate moment from
-- pupil enrollment (there was previously no way to record when/how it was paid, and no way
-- to convert an admitted application into an actual Pupil record at all). Adds the missing
-- fields plus a link back to the pupil once enrolled, and a read permission so Accounts
-- (finance-tier, not admissions-tier) can see registration income without full admissions
-- management rights.

ALTER TABLE admissions ADD COLUMN reg_fee_paid_on DATE;
ALTER TABLE admissions ADD COLUMN reg_fee_payment_method VARCHAR(30);
ALTER TABLE admissions ADD COLUMN pupil_id UUID REFERENCES pupils(id) ON DELETE SET NULL;

INSERT INTO permissions (name, description, module) VALUES
    ('admissions:view', 'View admission applications (read-only)', 'ADMISSIONS')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'HEAD_TEACHER', 'DEPUTY_HEAD', 'ACCOUNTANT')
AND p.name = 'admissions:view'
ON CONFLICT DO NOTHING;
