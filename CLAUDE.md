# mock-invest

한국 주식 모의 투자 웹 서비스. 회원 가입 후 초기 자금 1,000만 원으로 KOSPI/KOSDAQ 종목을 실시간 시세로 매수/매도한다.

## 기술 스택

- **Java 21** / Spring Boot 3.5.0 / Gradle
- **Spring Security** (Form Login, BCrypt)
- **Spring Data JPA** + **Flyway** (MySQL)
- **Thymeleaf** (서버사이드 렌더링)
- **Yahoo Finance API** (주가·차트·종목 검색)
- Docker + Nginx

## 프로젝트 구조

```
src/main/java/com/mockinvest/
├── config/          # AppConfig (RestClient, BCrypt), SecurityConfig
├── domain/
│   ├── user/        # User 엔티티, UserService (UserDetailsService 구현)
│   ├── stock/       # Stock 엔티티, StockService (Yahoo 검색·캐싱)
│   ├── portfolio/   # Holding 엔티티, PortfolioService (수익률 계산)
│   └── trade/       # Trade 엔티티, TradeService (매수·매도)
├── event/           # TradeCompletedEvent
├── infrastructure/
│   └── stockprice/  # StockPriceProvider 인터페이스 + YahooFinanceProvider
└── web/
    ├── controller/  # AuthController, DashboardController, StockController, TradeController
    └── dto/         # RegisterRequest, TradeRequest, PortfolioDto, HoldingDto

src/main/resources/
├── db/migration/    # V1(users) V2(stocks) V3(holdings) V4(trades)
└── templates/       # auth/, dashboard/, stock/, trade/, fragments/
```

## 도메인 규칙

- 초기 자금: **10,000,000원** (`User.create()` 하드코딩, `PortfolioService.INITIAL_CAPITAL`)
- 매수 시 현재가 × 수량만큼 `cashBalance` 차감 → 잔액 부족이면 예외
- 매수 시 `Holding` 없으면 생성, 있으면 가중평균단가(`avgBuyPrice`) 재계산
- 매도 시 보유 수량 부족이면 예외; 보유 수량이 0이 되어도 Holding 레코드는 남음
- 매수·매도 완료 후 `TradeCompletedEvent` 발행 (현재 리스너 없음 — 확장 포인트)

## Yahoo Finance 연동

| Bean | Base URL | 용도 |
|---|---|---|
| `yahooRestClient` | `query1.finance.yahoo.com` | 현재가, 캔들 (`/v8/finance/chart`) |
| `yahooSearchClient` | `query2.finance.yahoo.com` | 종목 검색 (`/v1/finance/search`) |

- 한국 주식만 허용: ticker가 `.KS`(KOSPI) 또는 `.KQ`(KOSDAQ)로 끝나는 것만 필터
- 검색 결과는 `stocks` 테이블에 캐싱 (`StockService.getOrCreate`)
- API 실패 시 빈 목록 또는 0원 반환 (예외 비전파)

## 주요 엔드포인트

| 경로 | 설명 |
|---|---|
| `GET /auth/login` | 로그인 페이지 |
| `GET/POST /auth/register` | 회원가입 |
| `GET /dashboard` | 포트폴리오 + KOSPI/KOSDAQ 지수 |
| `GET /stocks/search?q=` | 종목 검색 |
| `GET /stocks/{ticker}?period=` | 종목 상세 + 캔들 차트 |
| `POST /trade/buy` | 매수 |
| `POST /trade/sell` | 매도 |
| `GET /trade/history` | 거래 내역 (페이징 20건) |
| `GET /api/stocks/price/{ticker}` | 실시간 폴링용 REST |
| `GET /api/stocks/history/{ticker}` | 차트 데이터 REST |

Security: `/auth/**`, `/css/**`, `/js/**`, `/api/stocks/**` 는 인증 없이 허용.

## 로컬 실행

```bash
# 환경변수 설정 (.env.example 참고)
cp .env.example .env

# Docker로 MySQL + 앱 실행
docker-compose up

# 또는 Gradle로 직접 실행 (MySQL이 별도 실행 중이어야 함)
./gradlew bootRun
```

DB는 Flyway가 자동 마이그레이션하며, `ddl-auto: validate`로 엔티티와 스키마 일치 여부를 검증한다.

## 미구현 / 확장 포인트

- `TradeCompletedEvent` 리스너 (알림, 로그 등)
- 사용자 간 수익률 랭킹
- 관심 종목 (즐겨찾기)
- WebSocket 실시간 시세 푸시 (현재는 REST 폴링)
- 회원정보 수정 / 비밀번호 변경
- 테스트 코드 (현재 없음)
