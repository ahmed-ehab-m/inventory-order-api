CREATE TABLE users (
	
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	name VARCHAR(255) NOT NULL,
	email VARCHAR(255) NOT NULL UNIQUE,
	password VARCHAR(255) NOT NULL,
	location TEXT,
	phone VARCHAR(50),
	role VARCHAR(50) NOT NULL , -- not ENUM because if any update to add a new role will be complex solution
	is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
	
		    -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
	
)