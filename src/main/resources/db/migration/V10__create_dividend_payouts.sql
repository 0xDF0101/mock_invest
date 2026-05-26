CREATE TABLE dividend_payouts (
    id           BIGINT        AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    stock_id     BIGINT        NOT NULL,
    payout_date  DATE          NOT NULL,
    amount       DECIMAL(15,2) NOT NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_stock_date (user_id, stock_id, payout_date),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
