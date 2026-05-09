CREATE TABLE order_item
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    price DECIMAL(10,2) NOT NULL,
    quantity INTEGER NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT fk_order_items FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_product_item FOREIGN KEY (product_id) REFERENCES products(id)
)