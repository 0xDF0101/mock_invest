CREATE TABLE holdings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    stock_id       BIGINT         NOT NULL,
    quantity       INT            NOT NULL DEFAULT 0,
    avg_buy_price  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    UNIQUE KEY uk_user_stock (user_id, stock_id),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
