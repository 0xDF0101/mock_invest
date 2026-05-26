CREATE TABLE portfolio_snapshots (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    snapshot_date DATE         NOT NULL,
    total_asset   DECIMAL(15,2) NOT NULL,
    UNIQUE KEY uq_user_date (user_id, snapshot_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
