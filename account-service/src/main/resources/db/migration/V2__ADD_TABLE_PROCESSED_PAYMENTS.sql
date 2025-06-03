CREATE TABLE processed_payments
(
    payment_id     VARCHAR(255) NOT NULL,
    status         VARCHAR(255) NOT NULL,
    failure_reason VARCHAR(255),
    is_sent        BOOLEAN      NOT NULL,
    CONSTRAINT pk_processed_payments PRIMARY KEY (payment_id)
);