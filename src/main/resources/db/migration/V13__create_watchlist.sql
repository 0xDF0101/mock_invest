CREATE TABLE watchlist (
    id         BIGINT   AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    stock_id   BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_watchlist (user_id, stock_id),
    FOREIGN KEY (user_id)  REFERENCES users(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
