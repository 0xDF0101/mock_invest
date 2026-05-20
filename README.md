# 📈 MockInvest

> 실제 시세로 배우는 한국 주식 모의 투자 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)](https://www.docker.com/)

---

## 소개

MockInvest는 실제 Yahoo Finance 시세를 기반으로 KOSPI · KOSDAQ 종목을 자유롭게 매수/매도할 수 있는 모의 투자 웹 서비스입니다.  
회원가입 시 **1,000만 원**의 초기 자금이 지급되며, 실제 시장 데이터로 투자 전략을 연습할 수 있습니다.

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 포트폴리오 대시보드 | 총 자산 · 수익률 · 보유 종목별 스파크라인 차트 실시간 확인 |
| 종목 검색 | 한글 · 영문 · 티커 통합 검색, KOSPI/KOSDAQ 추천 종목 표시 |
| 캔들 차트 | 1일 · 5일 · 1개월 · 3개월 기간별 인터랙티브 캔들 차트 |
| 매수 / 매도 | 현재가 기준 실시간 거래, 가중평균단가 자동 계산 |
| 거래 내역 | 전체 거래 내역 페이징 조회 |
| 지수 폴링 | KOSPI · KOSDAQ 지수 10초 주기 자동 갱신 |
| 모바일 최적화 | 반응형 다크 UI, 하단 네비게이션 바 |

---

## 기술 스택

**Backend**
- Java 21 (Virtual Threads)
- Spring Boot 3.5.0
- Spring Security (Form Login · BCrypt)
- Spring Data JPA + Flyway
- Caffeine 인메모리 캐시

**Frontend**
- Thymeleaf
- Bootstrap 5.3
- Lightweight Charts (캔들 차트)
- Canvas API (스파크라인)

**Infra**
- MySQL 8.0
- Docker + Docker Compose
- Nginx (리버스 프록시)

**외부 API**
- Yahoo Finance API (실시간 시세 · 캔들 · 종목 검색)

---

## 프로젝트 구조

```
src/main/java/com/mockinvest/
├── config/           # Security, RestClient 설정
├── domain/
│   ├── user/         # 회원 관리
│   ├── stock/        # 종목 검색 · 캐싱 (KOSPI/KOSDAQ 79종목 한글명)
│   ├── portfolio/    # 보유 종목 · 수익률 계산
│   └── trade/        # 매수 · 매도
├── infrastructure/
│   └── stockprice/   # Yahoo Finance 연동 (가격 · 캔들 캐싱)
└── web/
    ├── controller/   # MVC 컨트롤러 + REST API
    └── dto/          # 요청 · 응답 DTO
```

---

## 로컬 실행

### 사전 준비
- Java 21
- Maven

### 실행

```bash
# 저장소 클론
git clone https://github.com/0xDF0101/mock-invest.git
cd mock-invest

# 로컬 개발 프로파일로 실행 (H2 인메모리 DB)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`dev` 프로파일은 H2 인메모리 DB를 사용하며, 아래 계정이 자동 생성됩니다.

| 항목 | 값 |
|---|---|
| 아이디 | `admin` |
| 비밀번호 | `admin` |
| 초기 잔고 | 2,000,000원 + 보유 종목 |

---

## 배포 (Docker Compose)

```bash
# 환경변수 설정
cp .env.example .env
# .env 파일에서 DB_ROOT_PASSWORD, DB_USER, DB_PASSWORD 설정

# 데이터 디렉토리 생성
sudo mkdir -p /opt/mock-invest/mysql

# 빌드 & 실행
./mvnw package -DskipTests
docker compose up -d
```

서비스는 `http://서버IP:9081` 로 접속합니다.

### 구성

```
Nginx (9081) → Spring Boot App (8080) → MySQL (3306)
```

MySQL 데이터는 `/opt/mock-invest/mysql`에 저장됩니다.

---

## 주요 엔드포인트

| 경로 | 설명 |
|---|---|
| `GET /dashboard` | 포트폴리오 대시보드 |
| `GET /stocks/search?q=` | 종목 검색 |
| `GET /stocks/{ticker}` | 종목 상세 + 차트 |
| `POST /trade/buy` | 매수 |
| `POST /trade/sell` | 매도 |
| `GET /trade/history` | 거래 내역 |
| `GET /api/stocks/price/{ticker}` | 실시간 가격 (REST) |
| `GET /api/stocks/history/{ticker}` | 캔들 데이터 (REST) |
| `GET /api/stocks/featured` | 추천 종목 목록 (REST) |
