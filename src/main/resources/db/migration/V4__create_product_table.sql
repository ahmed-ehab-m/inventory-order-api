CREATE TABLE products(
	
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	category_id BIGINT NOT NULL,
	name VARCHAR(255) NOT NULL,
	description TEXT,
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	
		 -- from BaseEntity
	created_at  DATETIME, 
    updated_at  DATETIME,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    
	CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)

);