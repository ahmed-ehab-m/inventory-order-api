CREATE TABLE orders
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_price DECIMAL(10,2) NOT NULL ,
    status VARCHAR(50) NOT NULL ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    user_id BIGINT NOT NULL,

        -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    CONSTRAINT fk_user_order FOREIGN KEY (user_id) REFERENCES users(id)
)