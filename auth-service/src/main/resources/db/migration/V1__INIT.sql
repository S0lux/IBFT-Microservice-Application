CREATE TABLE users
(
    id            UUID                        NOT NULL,
    status        VARCHAR(255)                NOT NULL,
    phone_number  VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    email         VARCHAR(255),
    full_name     VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_phone_number UNIQUE (phone_number);