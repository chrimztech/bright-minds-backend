-- Tracks a payment-gateway (Lenco) collection attempt separately from the confirmed `payments`
-- record — a mobile money collection is "pending customer approval" for a while before it's
-- known to have actually succeeded, and only a successful one ever becomes a real `Payment`.
CREATE TABLE gateway_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(20) NOT NULL DEFAULT 'LENCO',
    reference VARCHAR(100) NOT NULL UNIQUE,
    lenco_id VARCHAR(100),
    lenco_reference VARCHAR(100),
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    guardian_id UUID REFERENCES guardians(id) ON DELETE SET NULL,
    phone VARCHAR(20) NOT NULL,
    operator VARCHAR(20) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_id UUID REFERENCES payments(id) ON DELETE SET NULL,
    failure_reason TEXT,
    raw_response TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX gateway_transactions_invoice_idx ON gateway_transactions(invoice_id);
CREATE INDEX gateway_transactions_guardian_idx ON gateway_transactions(guardian_id);
