-- Lets a fee item (e.g. an Administrative Fee) carry a due date that flows onto every
-- invoice auto-billed from it, so the existing late-fee sweep (which keys off
-- Invoice.due_date) can apply to recurring/class-billed fees, not just one-off invoices.
ALTER TABLE fee_items ADD COLUMN IF NOT EXISTS due_date DATE;
