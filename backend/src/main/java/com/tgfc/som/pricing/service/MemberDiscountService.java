package com.tgfc.som.pricing.service;

import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.pricing.dto.MemberDiscVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 會員折扣服務
 *
 * 計算不同會員類型的折扣：
 * - Type 0 (Discounting 折價): 原價 × 折扣率
 * - Type 1 (Down Margin 下降): 直接設定折扣價
 * - Type 2 (Cost Markup 成本加成): 成本 × 加成比例
 * - SPECIAL (特殊會員): 直接設定特殊價格
 */
@Service
public class MemberDiscountService {

    private static final Logger log = LoggerFactory.getLogger(MemberDiscountService.class);

    /**
     * 計算訂單行項的會員折扣
     *
     * @param line 訂單行項
     * @param discountType 會員折扣類型
     * @param discRate 折扣率（Type 0 用）
     * @param markupRate 加成比例（Type 2 用）
     * @param cost 成本價（Type 2 用）
     * @return 會員折扣明細
     */
    public MemberDiscVO calculateDiscount(
            OrderLine line,
            MemberDiscountType discountType,
            BigDecimal discRate,
            BigDecimal markupRate,
            int cost) {

        if (discountType == null) {
            log.debug("無會員折扣類型，跳過計算: skuNo={}", line.getSkuNo());
            return null;
        }

        int originalPrice = line.getUnitPrice().amount() * line.getQuantity();

        return switch (discountType) {
            case DISCOUNTING -> calculateType0(line.getSkuNo(), originalPrice, discRate);
            case DOWN_MARGIN -> calculateType1(line.getSkuNo(), originalPrice, discRate);
            case COST_MARKUP -> calculateType2(line.getSkuNo(), originalPrice, cost, markupRate);
            case SPECIAL -> calculateSpecial(line.getSkuNo(), originalPrice, discRate);
        };
    }

    /**
     * Type 0: Discounting (折價)
     *
     * 折扣價 = 原價 × 折扣率
     * 折扣金額 = 折扣價 - 原價 (負數)
     */
    public MemberDiscVO calculateType0(String skuNo, int originalPrice, BigDecimal discRate) {
        if (discRate == null || discRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Type 0 折扣率無效: skuNo={}, discRate={}", skuNo, discRate);
            return null;
        }

        MemberDiscVO result = MemberDiscVO.ofType0(skuNo, originalPrice, discRate);

        log.info("Type 0 折扣計算: skuNo={}, originalPrice={}, discRate={}, discountPrice={}, discAmt={}",
                skuNo, originalPrice, discRate, result.discountPrice(), result.discAmt());

        return result;
    }

    /**
     * Type 1: Down Margin (下降)
     *
     * 折扣價由系統直接設定（通常從會員主檔取得）
     * 這裡使用 discRate 作為下降比例計算折扣價
     */
    public MemberDiscVO calculateType1(String skuNo, int originalPrice, BigDecimal discRate) {
        if (discRate == null) {
            log.warn("Type 1 下降比例無效: skuNo={}", skuNo);
            return null;
        }

        // Type 1 使用下降比例計算折扣價
        int discountPrice = BigDecimal.valueOf(originalPrice)
                .multiply(discRate)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();

        MemberDiscVO result = MemberDiscVO.ofType1(skuNo, originalPrice, discountPrice);

        log.info("Type 1 折扣計算: skuNo={}, originalPrice={}, discountPrice={}, discAmt={}",
                skuNo, originalPrice, result.discountPrice(), result.discAmt());

        return result;
    }

    /**
     * Type 2: Cost Markup (成本加成)
     *
     * 折扣價 = 成本 × 加成比例
     * 折扣金額 = 折扣價 - 原價
     *
     * 注意：折扣金額可能為正數（當成本加成後超過原價）
     * 此情況需要特殊處理（設為 0，發送警示）
     */
    public MemberDiscVO calculateType2(String skuNo, int originalPrice, int cost, BigDecimal markupRate) {
        if (markupRate == null || markupRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Type 2 加成比例無效: skuNo={}, markupRate={}", skuNo, markupRate);
            return null;
        }

        MemberDiscVO result = MemberDiscVO.ofType2(skuNo, originalPrice, cost, markupRate);

        log.info("Type 2 折扣計算: skuNo={}, originalPrice={}, cost={}, markupRate={}, discountPrice={}, discAmt={}",
                skuNo, originalPrice, cost, markupRate, result.discountPrice(), result.discAmt());

        // 檢查折扣金額是否為正數（異常情況）
        if (!result.isValidDiscount()) {
            log.warn("Type 2 計算結果異常 - 折扣金額為正數: skuNo={}, discAmt={}，將設為 0",
                    skuNo, result.discAmt());
        }

        return result;
    }

    /**
     * SPECIAL: 特殊會員
     *
     * 折扣價由系統直接設定
     */
    public MemberDiscVO calculateSpecial(String skuNo, int originalPrice, BigDecimal discRate) {
        if (discRate == null) {
            log.warn("SPECIAL 折扣率無效: skuNo={}", skuNo);
            return null;
        }

        // 特殊會員使用固定折扣率
        int discountPrice = BigDecimal.valueOf(originalPrice)
                .multiply(discRate)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();

        MemberDiscVO result = MemberDiscVO.ofSpecial(skuNo, originalPrice, discountPrice);

        log.info("SPECIAL 折扣計算: skuNo={}, originalPrice={}, discountPrice={}, discAmt={}",
                skuNo, originalPrice, result.discountPrice(), result.discAmt());

        return result;
    }

    /**
     * 計算訂單所有行項的會員折扣
     *
     * @param lines 訂單行項列表
     * @param discountType 會員折扣類型
     * @param discRate 折扣率
     * @param markupRate 加成比例（Type 2 用）
     * @return 折扣明細列表
     */
    public List<MemberDiscVO> calculateAllDiscounts(
            List<OrderLine> lines,
            MemberDiscountType discountType,
            BigDecimal discRate,
            BigDecimal markupRate) {

        List<MemberDiscVO> discounts = new ArrayList<>();
        List<MemberDiscVO> invalidDiscounts = new ArrayList<>();

        for (OrderLine line : lines) {
            // TODO: 從商品主檔取得成本價，這裡暫時使用假設值
            int cost = (int) (line.getUnitPrice().amount() * 0.7); // 假設成本為售價的 70%

            MemberDiscVO discount = calculateDiscount(line, discountType, discRate, markupRate, cost);

            if (discount != null) {
                if (discount.isValidDiscount()) {
                    discounts.add(discount);
                    // 更新行項的會員折扣
                    line.setMemberDisc(Money.of(Math.abs(discount.discAmt())));
                } else {
                    // Type 2 負結果處理
                    invalidDiscounts.add(discount);
                    log.warn("折扣計算異常，設為 0: skuNo={}, discAmt={}", discount.skuNo(), discount.discAmt());
                    // 設定折扣金額為 0
                    line.setMemberDisc(Money.ZERO);
                    // 記錄為無折扣
                    discounts.add(new MemberDiscVO(
                            discount.skuNo(),
                            discount.discType(),
                            discount.discTypeName(),
                            discount.originalPrice(),
                            discount.originalPrice(), // 折扣價 = 原價
                            0, // 折扣金額為 0
                            discount.discRate(),
                            discount.markupRate()
                    ));
                }
            }
        }

        // 如果有異常折扣，發送警示
        if (!invalidDiscounts.isEmpty()) {
            sendNegativeDiscountAlert(invalidDiscounts);
        }

        return discounts;
    }

    /**
     * 計算總會員折扣金額
     */
    public Money calculateTotalMemberDiscount(List<MemberDiscVO> discounts) {
        int total = discounts.stream()
                .mapToInt(MemberDiscVO::discAmt)
                .sum();
        return Money.of(total);
    }

    /**
     * 發送 Type 2 負結果警示郵件
     *
     * @param invalidDiscounts 異常折扣列表
     */
    private void sendNegativeDiscountAlert(List<MemberDiscVO> invalidDiscounts) {
        // TODO: 整合郵件服務發送警示
        log.error("Type 2 負結果警示 - 以下商品折扣計算異常（成本加成後超過原價）:");
        for (MemberDiscVO discount : invalidDiscounts) {
            log.error("  - skuNo: {}, originalPrice: {}, discountPrice: {}, discAmt: {}",
                    discount.skuNo(),
                    discount.originalPrice(),
                    discount.discountPrice(),
                    discount.discAmt());
        }
        // 實際實作時會呼叫 EmailService.sendAlert(...)
    }
}
