CREATE TABLE cart_items
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,

    -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    constraint fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart (id),
    constraint fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
)