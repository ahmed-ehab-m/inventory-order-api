CREATE TABLE categories
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    -- from BaseEntity
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);