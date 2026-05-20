CREATE TABLE stocks (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticker  VARCHAR(20)     NOT NULL UNIQUE,
    name    VARCHAR(100)    NOT NULL,
    market  VARCHAR(10)     NOT NULL
);
