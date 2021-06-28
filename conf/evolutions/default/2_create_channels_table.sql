-- channels table

-- !Ups

CREATE TABLE channels (
    id BIGSERIAL NOT NULL,
    channel_id VARCHAR(255) UNIQUE NOT NULL,
    host_id_hash VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (channel_id)
);

-- !Downs

DROP TABLE channels;