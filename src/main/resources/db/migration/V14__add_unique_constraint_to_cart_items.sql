ALTER TABLE cart_items
Add constraint uk_cart_product UNIQUE (cart_id,product_id)