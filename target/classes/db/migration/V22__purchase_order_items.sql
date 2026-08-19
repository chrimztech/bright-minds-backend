-- Purchase orders previously had no line items and no connection to inventory at all —
-- marking a PO "RECEIVED" only flipped a status label. This adds line items (each tied to
-- an inventory item) so receiving a PO can actually credit stock via the same
-- inventory_txns mechanism manual stock adjustments already use.
CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id UUID NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES inventory_items(id),
    quantity INTEGER NOT NULL,
    unit_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_purchase_order_items_po ON purchase_order_items(purchase_order_id);

-- Prevents double-receiving (crediting stock twice for the same PO) the same way
-- Admission.pupil_id prevents double-enrolling an application.
ALTER TABLE purchase_orders ADD COLUMN received_at TIMESTAMPTZ;
