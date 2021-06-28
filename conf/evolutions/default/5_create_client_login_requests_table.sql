-- client_login_requests table

-- !Ups

CREATE TABLE client_login_requests (
    id BIGSERIAL NOT NULL,
    client_login_request_id VARCHAR(255) UNIQUE NOT NULL,
    request_client_id_hash VARCHAR(255) UNIQUE NOT NULL,
    request_client_id_enc BYTEA NOT NULL,
    channel_id VARCHAR(255) NOT NULL,
    codename_enc BYTEA,
    passphrase_enc BYTEA,
    is_authenticated BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (client_login_request_id)
);

-- !Downs

DROP TABLE client_login_requests;