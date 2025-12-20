package com.tgfc.som.pricing.service;

import com.tgfc.som.pricing.domain.PromotionResult;
import com.tgfc.som.pricing.domain.PromotionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 促銷計算服務 (Stub 版本)
 *
 * 用於 12-Step 計價流程的 Step 5: promotionCalculation()
 *
 * 重要: OMS 為唯一促銷決策者，SOM 僅執行 OMS 指定的促銷類型
 * - SOM 不實作「選擇最優促銷」邏輯
 * - 每個商品只會被 OMS 標記一個 Event 類型 (via TBL_SKU_STORE.PROM_EVENT_NO)
 * - 促銷過期時，按原價計算並記錄 Warning Log
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 21.4, 21.7</a>
 */
@Service
public class PromotionService {

    private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

    /**
     * 執行促銷計算 (主入口)
     *
     * @param skuPrices 商品編號對應價格 (skuNo -> actPosAmt)
     * @param skuQuantities 商品編號對應數量 (skuNo -> quantity)
     * @param storeId 店別代號
     * @return 促銷計算結果列表
     */
    public List<PromotionResult> calculate(
        Map<String, BigDecimal> skuPrices,
        Map<String, Integer> skuQuantities,
        String storeId
    ) {
        if (skuPrices == null || skuPrices.isEmpty()) {
            return Collections.emptyList();
        }

        List<PromotionResult> results = new ArrayList<>();

        // Step 1: 查詢各商品的促銷標記 (from TBL_SKU_STORE.PROM_EVENT_NO)
        Map<String, String> skuEventMap = queryPromotionEvents(skuPrices.keySet(), storeId);

        // Step 2: 依促銷類型分組執行
        for (Map.Entry<String, String> entry : skuEventMap.entrySet()) {
            String skuNo = entry.getKey();
            String eventNo = entry.getValue();

            if (eventNo == null || eventNo.isEmpty()) {
                continue; // 無促銷標記
            }

            PromotionResult result = executePromotion(
                eventNo,
                skuNo,
                skuPrices.get(skuNo),
                skuQuantities.getOrDefault(skuNo, 1),
                storeId
            );

            if (result != null) {
                results.add(result);
            }
        }

        return results;
    }

    /**
     * 查詢商品促銷標記
     *
     * TODO: 實作 MyBatis 查詢 TBL_SKU_STORE.PROM_EVENT_NO
     * 目前為 Stub，回傳空 Map
     */
    private Map<String, String> queryPromotionEvents(
        Iterable<String> skuNos,
        String storeId
    ) {
        // Stub: 回傳空 Map (無促銷)
        log.debug("queryPromotionEvents - storeId: {}, skuCount: {}",
            storeId, skuNos.spliterator().getExactSizeIfKnown());
        return Collections.emptyMap();
    }

    /**
     * 執行單一促銷計算
     */
    private PromotionResult executePromotion(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity,
        String storeId
    ) {
        // 解析促銷類型 (eventNo 的第一個字元)
        PromotionType type = parsePromotionType(eventNo);
        if (type == null) {
            log.warn("未知的促銷類型: eventNo={}", eventNo);
            return null;
        }

        // 檢查促銷是否過期 (風險窗口: 23:59 ~ 隔天 09:30)
        if (isPromotionExpired(eventNo)) {
            log.warn("促銷已過期或無效: eventNo={}", eventNo);
            return PromotionResult.failed(eventNo, type, "EXPIRED");
        }

        // 依促銷類型執行計算
        return switch (type) {
            case A -> calculateEventA(eventNo, skuNo, price, quantity);
            case B -> calculateEventB(eventNo, skuNo, price, quantity);
            case C -> calculateEventC(eventNo, skuNo, price, quantity);
            case D -> calculateEventD(eventNo, skuNo, price, quantity);
            case E -> calculateEventE(eventNo, skuNo, price, quantity);
            case F -> calculateEventF(eventNo, skuNo, price, quantity);
            case G -> calculateEventG(eventNo, skuNo, price, quantity);
            case H -> calculateEventH(eventNo, skuNo, price, quantity);
        };
    }

    /**
     * 解析促銷類型
     */
    private PromotionType parsePromotionType(String eventNo) {
        if (eventNo == null || eventNo.isEmpty()) {
            return null;
        }
        String typeCode = eventNo.substring(0, 1).toUpperCase();
        return PromotionType.fromCode(typeCode);
    }

    /**
     * 檢查促銷是否過期
     *
     * TODO: 實作 MyBatis 查詢促銷效期
     * 風險窗口: 23:59 ~ 隔天 09:30
     */
    private boolean isPromotionExpired(String eventNo) {
        // Stub: 永不過期
        return false;
    }

    // ============ Event Type A: 印花價 (單品促銷) ============

    /**
     * Event A: 印花價
     * 公式: newPrice = CEIL(posAmt × (1 - discRate)) 或 fixedPrice
     */
    private PromotionResult calculateEventA(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventA - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type B: 滿額加價購 ============

    /**
     * Event B: 滿額加價購
     * 公式: 優惠組數 = FLOOR(發票金額 / 條件金額)
     */
    private PromotionResult calculateEventB(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventB - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type C: 滿金額優惠 ============

    /**
     * Event C: 滿金額優惠
     * 公式: 全商品折扣 = 達標後統一折扣率
     */
    private PromotionResult calculateEventC(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventC - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type D: 買M享N ============

    /**
     * Event D: 買M享N (重複促銷)
     * 公式: 優惠數量 = (總數量 / M) × N
     */
    private PromotionResult calculateEventD(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventD - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type E: A群組享B優惠 ============

    /**
     * Event E: A群組享B優惠 (跨群組)
     * 公式: A群組達標 → B群組享折扣
     */
    private PromotionResult calculateEventE(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventE - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type F: 合購價 ============

    /**
     * Event F: 合購價 (多群組條件)
     * 公式: 所有群組達標 → 合購價
     */
    private PromotionResult calculateEventF(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventF - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type G: 共用商品合購 ============

    /**
     * Event G: 共用商品合購 (最複雜)
     * 公式: 多級距共用商品 → 取最大級距
     */
    private PromotionResult calculateEventG(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventG - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    // ============ Event Type H: 拆價合購 ============

    /**
     * Event H: 拆價合購 (多級距)
     * 公式: 單品拆價 → 多級距合購
     */
    private PromotionResult calculateEventH(
        String eventNo,
        String skuNo,
        BigDecimal price,
        int quantity
    ) {
        log.debug("calculateEventH - eventNo: {}, skuNo: {}", eventNo, skuNo);
        // Stub: 回傳空結果
        return PromotionResult.empty();
    }

    /**
     * 判斷商品是否已參與促銷
     * 用於後續會員折扣判斷 (已參與促銷的商品可能不再參與某些會員折扣)
     */
    public boolean hasParticipatedInPromotion(String skuNo, List<PromotionResult> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }
        return results.stream()
            .filter(PromotionResult::isSuccessful)
            .anyMatch(r -> r.getParticipatingSkus().contains(skuNo));
    }

    /**
     * 取得商品的促銷折扣金額
     */
    public BigDecimal getPromotionDiscount(String skuNo, List<PromotionResult> results) {
        if (results == null || results.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return results.stream()
            .filter(PromotionResult::isSuccessful)
            .filter(r -> r.getParticipatingSkus().contains(skuNo))
            .map(PromotionResult::getTotalDiscount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
