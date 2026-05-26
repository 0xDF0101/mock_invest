CREATE TABLE limit_orders (
    id            BIGINT         AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT         NOT NULL,
    stock_id      BIGINT         NOT NULL,
    type          VARCHAR(4)     NOT NULL,
    limit_price   DECIMAL(15,2)  NOT NULL,
    quantity      INT            NOT NULL,
    status        VARCHAR(10)    NOT NULL DEFAULT 'PENDING',
    reserved_cash DECIMAL(15,2)  NOT NULL DEFAULT 0,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    filled_at     DATETIME,
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
