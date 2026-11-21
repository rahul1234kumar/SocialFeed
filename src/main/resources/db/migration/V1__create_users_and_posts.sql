-- V1: the schema Hibernate used to create with ddl-auto=update, now owned by Flyway.
-- Never edit this file after it has run anywhere: Flyway stores its checksum and
-- refuses to start if it changes. Make schema changes in a new V2__..., V3__... file.

CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    -- Readable names, so a "Duplicate entry ... for key" error says which field collided.
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE posts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    content     VARCHAR(500) NOT NULL,
    user_id     BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;
