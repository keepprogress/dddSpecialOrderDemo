package com.tgfc.som.pricing.service;

import com.tgfc.som.fulfillment.service.WorkTypeService;
import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Customer;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.order.domain.valueobject.PriceCalculation;
import com.tgfc.som.pricing.dto.MemberDiscVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 價格試算服務
 *
 * 實作完整 pricing flow：
 * 1. 商品小計 (ComputeType 1)
 * 2. 安裝小計 (ComputeType 2)
 * 3. 運送小計 (ComputeType 3)
 * 4. 會員折扣 (ComputeType 4) - Type 0/1/2/SPECIAL
 * 5. 直送費用 (ComputeType 5)
 * 6. 折價券折扣 (ComputeType 6) - 由 CouponService 設定到訂單行項
 * 7. 紅利點數折扣 (ComputeType 7) - 由 BonusService 設定到訂單行項
 * 8. 稅額計算
 * 9. 最低工資驗證
 * 10. 應付總額計算
 */
@Service
public class PriceCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PriceCalculationService.class);

    private final WorkTypeService workTypeService;
    private final MemberDiscountService memberDiscountService;

    public PriceCalculationService(
            WorkTypeService workTypeService,
            MemberDiscountService memberDiscountService) {
        this.workTypeService = workTypeService;
        this.memberDiscountService = memberDiscountService;
    }

    /**
     * 執行價格試算
     *
     * @param order 訂單
     * @return 試算結果
     */
    public PriceCalculation calculate(Order order) {
        log.info("執行價格試算: orderId={}, lineCount={}",
                order.getId().value(), order.getLineCount());

        List<String> warnings = new ArrayList<>();
        boolean promotionSkipped = false;

        // ComputeType 1: 計算商品小計
        Money productTotal = calculateProductTotal(order);

        // ComputeType 2: 計算安裝小計
        Money installationTotal = calculateInstallationTotal(order);

        // ComputeType 3: 計算運送小計
        Money deliveryTotal = calculateDeliveryTotal(order);

        // 驗證最低工資
        validateMinimumWage(order, warnings);

        // ComputeType 4: 會員折扣
        MemberDiscountResult memberDiscountResult = calculateMemberDiscount(order);
        Money memberDiscount = memberDiscountResult.totalDiscount();
        List<MemberDiscVO> memberDiscounts = memberDiscountResult.discounts();

        // ComputeType 5: 直送費用 (暫時為 0)
        Money directShipmentTotal = calculateDirectShipmentTotal(order);

        // ComputeType 6: 折價券折扣 - 從訂單行項加總（由 CouponService 設定）
        Money couponDiscount = calculateCouponDiscount(order);

        // ComputeType 7: 紅利點數折扣 - 從訂單行項加總（由 BonusService 設定）
        Money bonusDiscount = calculateBonusDiscount(order);

        // 計算稅額
        Money taxAmount = calculateTaxAmount(order);

        // 計算應付總額
        Money grandTotal = productTotal
                .add(installationTotal)
                .add(deliveryTotal)
                .add(memberDiscount) // 負數
                .add(directShipmentTotal)
                .add(couponDiscount.negate()) // 轉為負數
                .add(bonusDiscount.negate()); // 轉為負數

        log.info("價格試算完成: productTotal={}, couponDiscount={}, bonusDiscount={}, grandTotal={}",
                productTotal.amount(), couponDiscount.amount(), bonusDiscount.amount(), grandTotal.amount());

        return PriceCalculation.builder()
                .productTotal(productTotal)
                .installationTotal(installationTotal)
                .deliveryTotal(deliveryTotal)
                .memberDiscount(memberDiscount)
                .directShipmentTotal(directShipmentTotal)
                .couponDiscount(couponDiscount)
                .bonusDiscount(bonusDiscount)
                .taxAmount(taxAmount)
                .grandTotal(grandTotal)
                .memberDiscounts(memberDiscounts)
                .warnings(warnings)
                .promotionSkipped(promotionSkipped)
                .build();
    }

    /**
     * 會員折扣計算結果
     */
    private record MemberDiscountResult(Money totalDiscount, List<MemberDiscVO> discounts) {
        static MemberDiscountResult empty() {
            return new MemberDiscountResult(Money.ZERO, new ArrayList<>());
        }
    }

    /**
     * 計算會員折扣
     *
     * 根據會員的折扣類型（Type 0/1/2/SPECIAL）計算折扣
     */
    private MemberDiscountResult calculateMemberDiscount(Order order) {
        Customer customer = order.getCustomer();
        if (customer == null || customer.discountType() == null) {
            log.debug("無會員折扣類型，跳過會員折扣計算: orderId={}", order.getId().value());
            return MemberDiscountResult.empty();
        }

        MemberDiscountType discountType = customer.discountType();
        BigDecimal discRate = getDiscountRate(customer);
        BigDecimal markupRate = getMarkupRate(customer);

        log.info("計算會員折扣: orderId={}, discountType={}, discRate={}, markupRate={}",
                order.getId().value(), discountType, discRate, markupRate);

        List<MemberDiscVO> discounts = memberDiscountService.calculateAllDiscounts(
                order.getLines(),
                discountType,
                discRate,
                markupRate
        );

        Money totalDiscount = memberDiscountService.calculateTotalMemberDiscount(discounts);

        log.info("會員折扣計算完成: orderId={}, totalDiscount={}, itemCount={}",
                order.getId().value(), totalDiscount.amount(), discounts.size());

        return new MemberDiscountResult(totalDiscount, discounts);
    }

    /**
     * 取得會員折扣率
     */
    private BigDecimal getDiscountRate(Customer customer) {
        // TODO: 從會員主檔取得折扣率
        // 這裡使用預設值
        return switch (customer.discountType()) {
            case DISCOUNTING -> new BigDecimal("0.95"); // 預設 95 折
            case DOWN_MARGIN -> new BigDecimal("0.90"); // 預設 90 折
            case SPECIAL -> new BigDecimal("0.85"); // 特殊會員 85 折
            default -> BigDecimal.ONE;
        };
    }

    /**
     * 取得成本加成比例
     */
    private BigDecimal getMarkupRate(Customer customer) {
        // TODO: 從會員主檔取得加成比例
        // 這裡使用預設值
        if (customer.discountType() == MemberDiscountType.COST_MARKUP) {
            return new BigDecimal("1.05"); // 預設成本加成 5%
        }
        return BigDecimal.ONE;
    }

    /**
     * 計算優惠券折扣
     *
     * 從訂單行項加總 couponDisc（由 CouponService 設定）
     */
    private Money calculateCouponDiscount(Order order) {
        Money total = order.getLines().stream()
                .map(OrderLine::getCouponDisc)
                .reduce(Money.ZERO, Money::add);
        log.debug("優惠券折扣計算: orderId={}, couponDiscount={}", order.getId().value(), total.amount());
        return total;
    }

    /**
     * 計算紅利點數折扣
     *
     * 從訂單行項加總 bonusDisc（由 BonusService 設定）
     */
    private Money calculateBonusDiscount(Order order) {
        Money total = order.getLines().stream()
                .map(OrderLine::getBonusDisc)
                .reduce(Money.ZERO, Money::add);
        log.debug("紅利折扣計算: orderId={}, bonusDiscount={}", order.getId().value(), total.amount());
        return total;
    }

    /**
     * 計算商品小計
     */
    private Money calculateProductTotal(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算直送費用
     */
    private Money calculateDirectShipmentTotal(Order order) {
        // 直送商品需要額外費用（暫時為 0）
        long directShipmentCount = order.getLines().stream()
                .filter(OrderLine::isDirectShipment)
                .count();

        if (directShipmentCount > 0) {
            log.debug("訂單包含 {} 項直送商品", directShipmentCount);
        }

        return Money.ZERO;
    }

    /**
     * 計算稅額
     */
    private Money calculateTaxAmount(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getTaxAmount)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算安裝費用小計
     */
    private Money calculateInstallationTotal(Order order) {
        return order.getLines().stream()
                .filter(OrderLine::hasInstallation)
                .map(OrderLine::getInstallationTotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算運送費用小計
     */
    private Money calculateDeliveryTotal(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getDeliveryTotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 驗證最低工資
     *
     * 檢查每個訂單行項的安裝費用是否符合工種的最低工資要求
     * 如果不符合，加入警告訊息
     */
    private void validateMinimumWage(Order order, List<String> warnings) {
        for (OrderLine line : order.getLines()) {
            if (!line.hasInstallation()) {
                continue;
            }

            String workTypeId = line.getWorkTypeId();
            if (workTypeId == null) {
                continue;
            }

            workTypeService.getWorkType(workTypeId).ifPresent(workType -> {
                Money installationTotal = line.getInstallationTotal();
                if (!workType.meetsMinimumWage(installationTotal)) {
                    String warning = String.format(
                            "商品 %s 安裝費用 %s 低於工種 %s 最低工資 %s",
                            line.getSkuNo(),
                            installationTotal.amount(),
                            workType.workTypeName(),
                            workType.minimumWage().amount()
                    );
                    warnings.add(warning);
                    log.warn("最低工資驗證失敗: {}", warning);
                }
            });
        }
    }
}
