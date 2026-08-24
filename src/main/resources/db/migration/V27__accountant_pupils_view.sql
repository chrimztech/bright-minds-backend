-- ACCOUNTANT had no pupils:view permission at all, so GET /pupils/all (used by the Fees
-- payment/invoice filter dropdowns, and by the Pupils page itself) returned 403 for accountants
-- with no obvious indication why — pupil records are needed for billing/collection.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ACCOUNTANT' AND p.name = 'pupils:view'
ON CONFLICT DO NOTHING;
