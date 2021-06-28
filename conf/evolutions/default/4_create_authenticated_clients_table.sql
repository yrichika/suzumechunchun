-- authenticated_clients table

-- !Ups

CREATE TABLE authenticated_clients(
    id BIGSERIAL NOT NULL,
    authenticated_client_id_hash VARCHAR(255) UNIQUE NOT NULL,
    authenticated_client_id_enc BYTEA NOT NULL,
    request_client_id_hash VARCHAR(255) UNIQUE NOT NULL,
    channel_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- !Downs

DROP TABLE authenticated_clients;