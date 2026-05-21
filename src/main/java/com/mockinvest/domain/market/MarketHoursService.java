package com.mockinvest.domain.market;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;

@Service
public class MarketHoursService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime OPEN_TIME  = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(15, 30);

    /**
     * KRX 공식 휴장일 목록. 고정 공휴일·변동 공휴일(설·추석·부처님오신날)·대체공휴일 포함.
     * 매년 KRX 휴장일 캘린더 기준으로 갱신 필요.
     */
    private static final Set<LocalDate> HOLIDAYS = Set.of(
        // ── 2026 ─────────────────────────────────────────────────────────────
        LocalDate.of(2026, 1, 1),   // 신정
        LocalDate.of(2026, 2, 16),  // 설날 연휴
        LocalDate.of(2026, 2, 17),  // 설날
        LocalDate.of(2026, 2, 18),  // 설날 연휴
        LocalDate.of(2026, 3, 2),   // 삼일절 대체 (3/1 일요일)
        LocalDate.of(2026, 5, 5),   // 어린이날
        LocalDate.of(2026, 5, 25),  // 부처님오신날 대체 (5/24 일요일)
        LocalDate.of(2026, 6, 6),   // 현충일
        LocalDate.of(2026, 8, 15),  // 광복절
        LocalDate.of(2026, 9, 24),  // 추석 연휴
        LocalDate.of(2026, 9, 25),  // 추석
        LocalDate.of(2026, 9, 26),  // 추석 연휴
        LocalDate.of(2026, 10, 3),  // 개천절
        LocalDate.of(2026, 10, 9),  // 한글날
        LocalDate.of(2026, 12, 25), // 성탄절
        LocalDate.of(2026, 12, 31), // 연말 휴장
        // ── 2027 ─────────────────────────────────────────────────────────────
        LocalDate.of(2027, 1, 1),   // 신정
        LocalDate.of(2027, 2, 6),   // 설날 연휴
        LocalDate.of(2027, 2, 7),   // 설날
        LocalDate.of(2027, 2, 8),   // 설날 연휴
        LocalDate.of(2027, 3, 1),   // 삼일절
        LocalDate.of(2027, 5, 5),   // 어린이날
        LocalDate.of(2027, 5, 13),  // 부처님오신날
        LocalDate.of(2027, 6, 6),   // 현충일
        LocalDate.of(2027, 8, 15),  // 광복절
        LocalDate.of(2027, 10, 3),  // 개천절
        LocalDate.of(2027, 10, 4),  // 추석 연휴
        LocalDate.of(2027, 10, 5),  // 추석
        LocalDate.of(2027, 10, 6),  // 추석 연휴
        LocalDate.of(2027, 10, 9),  // 한글날
        LocalDate.of(2027, 12, 25), // 성탄절
        LocalDate.of(2027, 12, 31)  // 연말 휴장
    );

    public boolean isOpen() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        return isTradingTime(now.toLocalDate(), now.toLocalTime());
    }

    public String getStatusLabel() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        if (isWeekend(date.getDayOfWeek())) return "휴장";
        if (HOLIDAYS.contains(date))        return "공휴일";
        if (time.isBefore(OPEN_TIME))       return "장전";
        if (time.isBefore(CLOSE_TIME))      return "장중";
        return "장마감";
    }

    /** 다음 시장 이벤트(개장 or 마감)까지의 epoch milliseconds */
    public long getNextEventMillis() {
        return nextEvent().toInstant().toEpochMilli();
    }

    /** true = 다음 이벤트가 개장 (현재 장 닫힘), false = 마감 (현재 장 열림) */
    public boolean isNextEventOpen() {
        return !isOpen();
    }

    private boolean isTradingTime(LocalDate date, LocalTime time) {
        if (isWeekend(date.getDayOfWeek())) return false;
        if (HOLIDAYS.contains(date)) return false;
        return !time.isBefore(OPEN_TIME) && time.isBefore(CLOSE_TIME);
    }

    private ZonedDateTime nextEvent() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        LocalDate today = now.toLocalDate();
        LocalTime time = now.toLocalTime();

        // 장 중 → 오늘 마감
        if (isTradingTime(today, time)) {
            return now.with(CLOSE_TIME);
        }
        // 오늘이 거래일이고 아직 장 전 → 오늘 개장
        if (!isWeekend(today.getDayOfWeek()) && !HOLIDAYS.contains(today) && time.isBefore(OPEN_TIME)) {
            return now.with(OPEN_TIME);
        }
        // 다음 거래일 개장
        LocalDate next = today.plusDays(1);
        while (isWeekend(next.getDayOfWeek()) || HOLIDAYS.contains(next)) {
            next = next.plusDays(1);
        }
        return ZonedDateTime.of(next, OPEN_TIME, KST);
    }

    private boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
