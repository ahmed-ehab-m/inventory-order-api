ALTER TABLE order_item
CONSTRAINT uk_order_product UNIQUE (order_id, product_id)
