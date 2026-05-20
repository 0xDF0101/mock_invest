CREATE TABLE trades (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    stock_id   BIGINT         NOT NULL,
    type       VARCHAR(4)     NOT NULL,
    price      DECIMAL(15, 2) NOT NULL,
    quantity   INT            NOT NULL,
    amount     DECIMAL(15, 2) NOT NULL,
    created_at DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
