CREATE TABLE accounts
(
    type              VARCHAR(255)   NOT NULL,
    status            VARCHAR(255)   NOT NULL,
    available_balance DECIMAL(19, 2) NOT NULL,
    holding_balance   DECIMAL(19, 2) NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id           UUID           NOT NULL,
    account_number    VARCHAR(255)   NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (user_id, account_number)
);