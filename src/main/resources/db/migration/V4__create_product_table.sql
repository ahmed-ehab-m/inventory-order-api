CREATE TABLE products
(

    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT         NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    description TEXT,
    image       TEXT,
    stock_count INT            NOT NULL,
    price       DECIMAL(10, 2) NOT NULL,
    is_deleted  BOOLEAN        NOT NULL DEFAULT FALSE,

    -- from BaseEntity
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id)

);