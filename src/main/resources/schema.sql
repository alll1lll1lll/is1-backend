CREATE TABLE IF NOT EXISTS app_user (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT app_user_username_unique    UNIQUE (username),
    CONSTRAINT app_user_username_not_blank CHECK (length(btrim(username)) > 0)
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id         SERIAL PRIMARY KEY,
    user_id    INTEGER     NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT now(),
    expires_at TIMESTAMP   NOT NULL,
    revoked_at TIMESTAMP,
    CONSTRAINT refresh_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX IF NOT EXISTS refresh_token_user_idx ON refresh_token (user_id);

CREATE TABLE IF NOT EXISTS coordinates (
    id SERIAL PRIMARY KEY,
    x  BIGINT  NOT NULL,
    y  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS location (
    id   SERIAL PRIMARY KEY,
    x    BIGINT           NOT NULL,
    y    DOUBLE PRECISION NOT NULL,
    name VARCHAR(255),
    CONSTRAINT location_name_not_blank CHECK (name IS NULL OR length(btrim(name)) > 0)
);

CREATE TABLE IF NOT EXISTS route (
    id               SERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    coordinates_id   INTEGER      NOT NULL REFERENCES coordinates (id) ON DELETE RESTRICT,
    creation_date    TIMESTAMP    NOT NULL DEFAULT now(),
    from_location_id INTEGER      REFERENCES location (id) ON DELETE RESTRICT,
    to_location_id   INTEGER      REFERENCES location (id) ON DELETE RESTRICT,
    distance         INTEGER      NOT NULL,
    rating           REAL,
    CONSTRAINT route_id_positive     CHECK (id > 0),
    CONSTRAINT route_name_not_blank  CHECK (length(btrim(name)) > 0),
    CONSTRAINT route_distance_gt_one CHECK (distance > 1),
    CONSTRAINT route_rating_positive CHECK (rating IS NULL OR rating > 0)
);

CREATE INDEX IF NOT EXISTS route_from_idx ON route (from_location_id);
CREATE INDEX IF NOT EXISTS route_to_idx ON route (to_location_id);
CREATE INDEX IF NOT EXISTS route_coordinates_idx ON route (coordinates_id);
