-- ── admin 계정 ───────────────────────────────────────────────────────────────
-- 비밀번호: admin
MERGE INTO users (username, email, password, cash_balance, created_at)
    KEY (username)
    VALUES ('admin', 'admin@mockinvest.dev',
            '$2a$10$gpHpN4ES4p55wv20AtlPqeDPOMseltRZXV7bl.pfUjo43PSufLYQG',
            2000000.00, NOW());

-- ── 인기 종목 시드 (추천 종목 섹션용) ─────────────────────────────────────────
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('005930.KS', '삼성전자',       'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('000660.KS', 'SK하이닉스',     'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('035420.KS', 'NAVER',          'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('035720.KS', '카카오',         'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('005380.KS', '현대차',         'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('000270.KS', '기아',           'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('051910.KS', 'LG화학',         'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('068270.KS', '셀트리온',       'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('207940.KS', '삼성바이오로직스','KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('373220.KS', 'LG에너지솔루션', 'KOSPI');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('247540.KQ', '에코프로비엠',   'KOSDAQ');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('086520.KQ', '에코프로',       'KOSDAQ');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('293490.KQ', '카카오게임즈',   'KOSDAQ');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('263750.KQ', '펄어비스',       'KOSDAQ');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('357780.KQ', '솔브레인',       'KOSDAQ');
MERGE INTO stocks (ticker, name, market) KEY (ticker) VALUES ('112040.KQ', '위메이드',       'KOSDAQ');

-- ── admin 모의 보유 주식 (총 매수금액 ≈ 8,000,000원) ────────────────────────
-- 삼성전자 50주 × 70,000원 = 3,500,000원
MERGE INTO holdings (user_id, stock_id, quantity, avg_buy_price)
    KEY (user_id, stock_id)
    VALUES ((SELECT id FROM users  WHERE username = 'admin'),
            (SELECT id FROM stocks WHERE ticker   = '005930.KS'),
            50, 70000.00);

-- SK하이닉스 10주 × 160,000원 = 1,600,000원
MERGE INTO holdings (user_id, stock_id, quantity, avg_buy_price)
    KEY (user_id, stock_id)
    VALUES ((SELECT id FROM users  WHERE username = 'admin'),
            (SELECT id FROM stocks WHERE ticker   = '000660.KS'),
            10, 160000.00);

-- NAVER 10주 × 200,000원 = 2,000,000원
MERGE INTO holdings (user_id, stock_id, quantity, avg_buy_price)
    KEY (user_id, stock_id)
    VALUES ((SELECT id FROM users  WHERE username = 'admin'),
            (SELECT id FROM stocks WHERE ticker   = '035420.KS'),
            10, 200000.00);

-- 카카오 20주 × 45,000원 = 900,000원
MERGE INTO holdings (user_id, stock_id, quantity, avg_buy_price)
    KEY (user_id, stock_id)
    VALUES ((SELECT id FROM users  WHERE username = 'admin'),
            (SELECT id FROM stocks WHERE ticker   = '035720.KS'),
            20, 45000.00);

-- ── admin 거래 내역 ────────────────────────────────────────────────────────────
INSERT INTO trades (user_id, stock_id, type, price, quantity, amount, created_at)
SELECT u.id, s.id, 'BUY', 70000.00, 50, 3500000.00,
       DATEADD('DAY', -45, CURRENT_TIMESTAMP)
FROM users u, stocks s
WHERE u.username = 'admin' AND s.ticker = '005930.KS';

INSERT INTO trades (user_id, stock_id, type, price, quantity, amount, created_at)
SELECT u.id, s.id, 'BUY', 160000.00, 10, 1600000.00,
       DATEADD('DAY', -30, CURRENT_TIMESTAMP)
FROM users u, stocks s
WHERE u.username = 'admin' AND s.ticker = '000660.KS';

INSERT INTO trades (user_id, stock_id, type, price, quantity, amount, created_at)
SELECT u.id, s.id, 'BUY', 200000.00, 10, 2000000.00,
       DATEADD('DAY', -20, CURRENT_TIMESTAMP)
FROM users u, stocks s
WHERE u.username = 'admin' AND s.ticker = '035420.KS';

INSERT INTO trades (user_id, stock_id, type, price, quantity, amount, created_at)
SELECT u.id, s.id, 'BUY', 45000.00, 20, 900000.00,
       DATEADD('DAY', -10, CURRENT_TIMESTAMP)
FROM users u, stocks s
WHERE u.username = 'admin' AND s.ticker = '035720.KS';
