CREATE TABLE cart(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,-- ONE TO ONE

    -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),

    constraint fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id)

)