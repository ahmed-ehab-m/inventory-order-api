ALTER TABLE order_item
    ADD CONSTRAINT uk_order_product UNIQUE (order_id, product_id)
