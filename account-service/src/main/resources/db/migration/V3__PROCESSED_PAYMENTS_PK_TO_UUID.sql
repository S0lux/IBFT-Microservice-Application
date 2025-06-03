ALTER TABLE processed_payments
DROP
COLUMN payment_id;

ALTER TABLE processed_payments
    ADD payment_id UUID NOT NULL PRIMARY KEY;