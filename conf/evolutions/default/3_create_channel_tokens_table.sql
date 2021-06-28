-- channel_tokens table

-- !Ups

CREATE TABLE channel_tokens (
    id BIGSERIAL NOT NULL,
    channel_token_id VARCHAR(255) UNIQUE NOT NULL,
    channel_id VARCHAR(255) UNIQUE NOT NULL,
    channel_name_enc BYTEA NOT NULL,
    host_channel_token_hash VARCHAR(255) UNIQUE NOT NULL,
    login_channel_token_hash VARCHAR(255) UNIQUE NOT NULL,
    login_channel_token_enc BYTEA NOT NULL,
    client_channel_token_hash VARCHAR(255) UNIQUE NOT NULL,
    client_channel_token_enc BYTEA NOT NULL,

    secret_key_enc BYTEA,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (channel_token_id)
);

-- !Downs

DROP TABLE channel_tokens;