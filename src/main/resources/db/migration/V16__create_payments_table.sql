CREATE TABLE payments(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,

    amount DECIMAL(10,2) NOT NULL , -- because gateway fees | partial payment | Coupon
    currency VARCHAR(10) DEFAULT 'EGP',
    payment_method VARCHAR(50),   -- CARD, WALLET, FAWRY
    payment_status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED

    -- FROM PayMob
    transaction_id VARCHAR(255) UNIQUE,
    provider_response_code VARCHAR(100),
    provider_message VARCHAR(255),

    -- from BaseEntity
    -- CURRENT_TIMESTAMP => db store the date by herself not await to spring to send it
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- ON UPDATE => any one update this record , update the time automatically
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    --- comment here paymob server who is control update not actual user
    --- actual user in orders table
--     created_by  VARCHAR(255),
--     updated_by  VARCHAR(255),

    CONSTRAINT fk_order_payment FOREIGN KEY (order_id) REFERENCES orders(id)
)