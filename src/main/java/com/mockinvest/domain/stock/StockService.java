package com.mockinvest.domain.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final RestClient yahooSearchClient;
    private final ObjectMapper objectMapper;

    // 티커 → 한글 공식 종목명 매핑 (Yahoo Finance 영문명 대체용)
    private static final Map<String, String> KR_NAME_MAP = Map.ofEntries(
        // KOSPI
        Map.entry("005930.KS", "삼성전자"),
        Map.entry("000660.KS", "SK하이닉스"),
        Map.entry("035420.KS", "NAVER"),
        Map.entry("035720.KS", "카카오"),
        Map.entry("005380.KS", "현대차"),
        Map.entry("000270.KS", "기아"),
        Map.entry("051910.KS", "LG화학"),
        Map.entry("006400.KS", "삼성SDI"),
        Map.entry("028260.KS", "삼성물산"),
        Map.entry("012330.KS", "현대모비스"),
        Map.entry("068270.KS", "셀트리온"),
        Map.entry("207940.KS", "삼성바이오로직스"),
        Map.entry("373220.KS", "LG에너지솔루션"),
        Map.entry("034730.KS", "SK"),
        Map.entry("017670.KS", "SK텔레콤"),
        Map.entry("030200.KS", "KT"),
        Map.entry("032830.KS", "삼성생명"),
        Map.entry("105560.KS", "KB금융"),
        Map.entry("086790.KS", "하나금융지주"),
        Map.entry("055550.KS", "신한지주"),
        Map.entry("003550.KS", "LG"),
        Map.entry("066570.KS", "LG전자"),
        Map.entry("009150.KS", "삼성전기"),
        Map.entry("010130.KS", "고려아연"),
        Map.entry("011200.KS", "HMM"),
        Map.entry("004020.KS", "현대제철"),
        Map.entry("010950.KS", "S-Oil"),
        Map.entry("011170.KS", "롯데케미칼"),
        Map.entry("000720.KS", "현대건설"),
        Map.entry("003490.KS", "대한항공"),
        Map.entry("024110.KS", "기업은행"),
        Map.entry("139480.KS", "이마트"),
        Map.entry("004170.KS", "신세계"),
        Map.entry("018260.KS", "삼성SDS"),
        Map.entry("009540.KS", "HD한국조선해양"),
        Map.entry("010140.KS", "삼성중공업"),
        Map.entry("042660.KS", "한화오션"),
        Map.entry("090430.KS", "아모레퍼시픽"),
        Map.entry("000100.KS", "유한양행"),
        Map.entry("128940.KS", "한미약품"),
        Map.entry("185750.KS", "종근당"),
        Map.entry("326030.KS", "SK바이오팜"),
        Map.entry("097950.KS", "CJ제일제당"),
        Map.entry("001040.KS", "CJ"),
        Map.entry("069960.KS", "현대백화점"),
        Map.entry("021240.KS", "코웨이"),
        Map.entry("267250.KS", "HD현대"),
        Map.entry("329180.KS", "HD현대중공업"),
        Map.entry("005490.KS", "POSCO홀딩스"),
        Map.entry("051900.KS", "LG생활건강"),
        Map.entry("032640.KS", "LG유플러스"),
        Map.entry("071050.KS", "한국금융지주"),
        Map.entry("316140.KS", "우리금융지주"),
        Map.entry("078930.KS", "GS"),
        Map.entry("006800.KS", "미래에셋증권"),
        Map.entry("016360.KS", "삼성증권"),
        Map.entry("034020.KS", "두산에너빌리티"),
        Map.entry("003230.KS", "삼양식품"),
        Map.entry("047050.KS", "포스코인터내셔널"),
        Map.entry("161390.KS", "한국타이어앤테크놀로지"),
        // KOSDAQ
        Map.entry("247540.KQ", "에코프로비엠"),
        Map.entry("086520.KQ", "에코프로"),
        Map.entry("293490.KQ", "카카오게임즈"),
        Map.entry("263750.KQ", "펄어비스"),
        Map.entry("357780.KQ", "솔브레인"),
        Map.entry("112040.KQ", "위메이드"),
        Map.entry("091990.KQ", "셀트리온헬스케어"),
        Map.entry("068760.KQ", "셀트리온제약"),
        Map.entry("214150.KQ", "클래시스"),
        Map.entry("145020.KQ", "휴젤"),
        Map.entry("328130.KQ", "루닛"),
        Map.entry("277810.KQ", "레인보우로보틱스"),
        Map.entry("196170.KQ", "알테오젠"),
        Map.entry("039030.KQ", "이오테크닉스"),
        Map.entry("237690.KQ", "에스티팜"),
        Map.entry("041510.KQ", "에스엠"),
        Map.entry("035900.KQ", "JYP Ent."),
        Map.entry("122870.KQ", "와이지엔터테인먼트"),
        Map.entry("036830.KQ", "솔브레인홀딩스"),
        Map.entry("039200.KQ", "오스코텍")
    );

    public StockService(StockRepository stockRepository,
                        @Qualifier("yahooSearchClient") RestClient yahooSearchClient,
                        ObjectMapper objectMapper) {
        this.stockRepository = stockRepository;
        this.yahooSearchClient = yahooSearchClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Stock getByTicker(String ticker) {
        return stockRepository.findByTicker(ticker)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + ticker));
    }

    /**
     * 티커로 종목 조회 또는 생성. KR_NAME_MAP 기준으로 한글명 우선 적용.
     * 이미 저장된 종목이 영문명이면 한글명으로 업데이트.
     */
    @Transactional
    public Stock getOrCreate(String ticker, String yahooName, Stock.Market market) {
        String koreanName = KR_NAME_MAP.getOrDefault(ticker, yahooName);
        return stockRepository.findByTicker(ticker)
                .map(existing -> {
                    if (KR_NAME_MAP.containsKey(ticker) && !koreanName.equals(existing.getName())) {
                        existing.updateName(koreanName);
                    }
                    return existing;
                })
                .orElseGet(() -> stockRepository.save(Stock.of(ticker, koreanName, market)));
    }

    @Transactional(readOnly = true)
    public List<Stock> getRandom(int limit) {
        return stockRepository.findRandom(limit);
    }

    /**
     * 한글/영문/티커 통합 검색.
     * DB 우선(한글·영문 이름, 티커) → 결과 없을 때만 Yahoo Finance 호출
     */
    @Transactional
    public List<Stock> search(String query) {
        List<Stock> dbResults = stockRepository
                .findByNameContainingIgnoreCaseOrTickerContainingIgnoreCase(query, query);
        if (!dbResults.isEmpty()) {
            return dbResults;
        }
        return searchYahoo(query);
    }

    private List<Stock> searchYahoo(String query) {
        try {
            String json = yahooSearchClient.get()
                    .uri(u -> u.path("/v1/finance/search")
                            .queryParam("q", query)
                            .queryParam("lang", "ko-KR")
                            .queryParam("region", "KR")
                            .queryParam("quotesCount", 20)
                            .queryParam("newsCount", 0)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode quotes = objectMapper.readTree(json).path("quotes");
            List<Stock> results = new ArrayList<>();

            for (JsonNode q : quotes) {
                String ticker = q.path("symbol").asText();
                if (!ticker.endsWith(".KS") && !ticker.endsWith(".KQ")) continue;

                String name = q.path("longname").asText();
                if (name.isEmpty()) name = q.path("shortname").asText();

                Stock.Market market = ticker.endsWith(".KS") ? Stock.Market.KOSPI : Stock.Market.KOSDAQ;
                results.add(getOrCreate(ticker, name, market));
            }
            return results;
        } catch (Exception e) {
            log.warn("Yahoo 종목 검색 실패: {}", query, e);
            return List.of();
        }
    }
}
