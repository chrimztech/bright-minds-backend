-- Pickup points get their own fee instead of one flat fee per route — a route can span
-- pickup points at different distances that should be priced differently. The existing
-- transport_routes.pickup_points free-text column is left as-is for backward compatibility
-- with any existing display code; this table is the new source of truth for pricing.
CREATE TABLE transport_pickup_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES transport_routes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    fee NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_transport_pickup_points_route ON transport_pickup_points(route_id);

-- Assignments can now reference a specific priced pickup point instead of (or alongside)
-- the free-text pickup_point column already on this table.
ALTER TABLE transport_assignments ADD COLUMN pickup_point_id UUID REFERENCES transport_pickup_points(id) ON DELETE SET NULL;
