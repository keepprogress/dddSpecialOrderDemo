package com.tgfc.som.pricing.service;

import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.pricing.domain.Coupon;
import com.tgfc.som.pricing.dto.CouponValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 優惠券服務
 *
 * 處理優惠券的驗證、套用與折扣分攤
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    // 模擬的優惠券資料（實際應從資料庫或外部服務取得）
    private final Map<String, Coupon> couponRepository = new HashMap<>();

    public CouponService() {
        initializeSampleCoupons();
    }

    /**
     * 初始化範例優惠券（開發測試用）
     */
    private void initializeSampleCoupons() {
        // 固定金額優惠券：滿 1000 折 100
        couponRepository.put("FIXED100", Coupon.fixedAmount(
                "FIXED100", "滿千折百",
                Money.of(100),
                Money.of(1000),
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(30)
        ));

        // 百分比優惠券：9 折，最高折 500
        couponRepository.put("PERCENT10", Coupon.percentage(
                "PERCENT10", "全館 9 折",
                new BigDecimal("0.10"),
                Money.of(500),
                null,
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(30)
        ));

        // 免安裝費優惠券
        couponRepository.put("FREEINSTALL", Coupon.freeInstallation(
                "FREEINSTALL", "免安裝費",
                LocalDate.now().minusDays(30),
                LocalDate.now().plusDays(30)
        ));

        log.info("已初始化 {} 張範例優惠券", couponRepository.size());
    }

    /**
     * 查詢優惠券
     *
     * @param couponId 優惠券編號
     * @return 優惠券（如果存在）
     */
    public Optional<Coupon> findCoupon(String couponId) {
        // TODO: 實際應查詢資料庫或呼叫外部服務
        return Optional.ofNullable(couponRepository.get(couponId));
    }

    /**
     * 驗證優惠券
     *
     * @param couponId 優惠券編號
     * @param order 訂單
     * @return 驗證結果
     */
    public CouponValidation validateCoupon(String couponId, Order order) {
        log.info("驗證優惠券: couponId={}, orderId={}", couponId, order.getId().value());

        // 1. 檢查優惠券是否存在
        Optional<Coupon> couponOpt = findCoupon(couponId);
        if (couponOpt.isEmpty()) {
            log.warn("優惠券不存在: couponId={}", couponId);
            return CouponValidation.notFound();
        }

        Coupon coupon = couponOpt.get();

        // 2. 檢查有效期間
        if (!coupon.isValidPeriod()) {
            LocalDate today = LocalDate.now();
            if (coupon.validFrom() != null && today.isBefore(coupon.validFrom())) {
                log.warn("優惠券尚未生效: couponId={}, validFrom={}", couponId, coupon.validFrom());
                return CouponValidation.notYetValid();
            } else {
                log.warn("優惠券已過期: couponId={}, validTo={}", couponId, coupon.validTo());
                return CouponValidation.expired();
            }
        }

        // 3. 檢查剩餘數量
        if (!coupon.hasRemainingQuantity()) {
            log.warn("優惠券已用完: couponId={}", couponId);
            return CouponValidation.exhausted();
        }

        // 4. 計算適用商品總額
        List<String> applicableSkus = new ArrayList<>();
        int applicableTotal = 0;

        for (OrderLine line : order.getLines()) {
            if (coupon.isApplicableToSku(line.getSkuNo())) {
                applicableSkus.add(line.getSkuNo());
                applicableTotal += line.getSubtotal().amount();
            }
        }

        // 5. 檢查是否有適用商品
        if (applicableSkus.isEmpty()) {
            log.warn("無適用商品: couponId={}, orderId={}", couponId, order.getId().value());
            return CouponValidation.noApplicableProducts();
        }

        // 6. 檢查最低訂單金額門檻
        Money applicableTotalMoney = Money.of(applicableTotal);
        if (!coupon.meetsMinimumOrder(applicableTotalMoney)) {
            int minimumAmount = coupon.minimumOrderAmount().amount();
            log.warn("訂單金額未達門檻: couponId={}, minimum={}, current={}",
                    couponId, minimumAmount, applicableTotal);
            return CouponValidation.belowMinimum(minimumAmount, applicableTotal);
        }

        // 7. 計算折扣金額
        Money discountAmount = coupon.calculateDiscount(applicableTotalMoney);

        // 8. 折扣金額不能超過商品總額（不可產生負數/退款）
        discountAmount = capDiscountAtProductTotal(discountAmount, applicableTotalMoney);

        log.info("優惠券驗證成功: couponId={}, discountAmount={}, applicableSkus={}",
                couponId, discountAmount.amount(), applicableSkus.size());

        return CouponValidation.success(discountAmount, applicableSkus, coupon.freeInstallation());
    }

    /**
     * 套用優惠券到訂單
     *
     * @param couponId 優惠券編號
     * @param order 訂單
     * @return 折扣金額（負數）
     */
    public Money applyCoupon(String couponId, Order order) {
        CouponValidation validation = validateCoupon(couponId, order);

        if (!validation.valid()) {
            log.warn("優惠券套用失敗: couponId={}, reason={}", couponId, validation.failureReason());
            return Money.ZERO;
        }

        // 分攤折扣到各商品
        allocateDiscountToLines(order, validation);

        log.info("優惠券套用成功: couponId={}, discountAmount={}",
                couponId, validation.discountAmount().amount());

        // 返回負數的折扣金額
        return validation.discountAmount().negate();
    }

    /**
     * 將折扣金額分攤到各訂單行項
     *
     * 按照商品金額比例分攤
     */
    private void allocateDiscountToLines(Order order, CouponValidation validation) {
        if (validation.discountAmount().isZero()) {
            return;
        }

        List<String> applicableSkus = validation.applicableSkus();
        int totalDiscount = validation.discountAmount().amount();

        // 計算適用商品總額
        int applicableTotal = 0;
        for (OrderLine line : order.getLines()) {
            if (applicableSkus.contains(line.getSkuNo())) {
                applicableTotal += line.getSubtotal().amount();
            }
        }

        if (applicableTotal == 0) {
            return;
        }

        // 按比例分攤
        int allocatedDiscount = 0;
        List<OrderLine> applicableLines = order.getLines().stream()
                .filter(line -> applicableSkus.contains(line.getSkuNo()))
                .toList();

        for (int i = 0; i < applicableLines.size(); i++) {
            OrderLine line = applicableLines.get(i);
            int lineTotal = line.getSubtotal().amount();

            int lineDiscount;
            if (i == applicableLines.size() - 1) {
                // 最後一項：分配剩餘金額（避免捨入誤差）
                lineDiscount = totalDiscount - allocatedDiscount;
            } else {
                // 按比例分配
                lineDiscount = (int) ((long) totalDiscount * lineTotal / applicableTotal);
            }

            line.setCouponDisc(Money.of(lineDiscount));
            allocatedDiscount += lineDiscount;

            log.debug("折扣分攤: skuNo={}, lineTotal={}, lineDiscount={}",
                    line.getSkuNo(), lineTotal, lineDiscount);
        }

        // 處理免安裝費
        if (validation.freeInstallation()) {
            for (OrderLine line : order.getLines()) {
                if (line.hasInstallation()) {
                    // 將安裝費設為折扣
                    Money installationCost = line.getInstallationTotal();
                    line.setCouponDisc(line.getCouponDisc().add(installationCost));
                    log.debug("免安裝費: skuNo={}, installationCost={}",
                            line.getSkuNo(), installationCost.amount());
                }
            }
        }
    }

    /**
     * 確保折扣不超過商品總額（不可產生負數/退款）
     */
    private Money capDiscountAtProductTotal(Money discount, Money productTotal) {
        if (discount.amount() > productTotal.amount()) {
            log.debug("折扣金額超過商品總額，調整為商品總額: discount={} -> {}",
                    discount.amount(), productTotal.amount());
            return productTotal;
        }
        return discount;
    }

    /**
     * 移除訂單上的優惠券
     */
    public void removeCoupon(Order order) {
        for (OrderLine line : order.getLines()) {
            line.setCouponDisc(Money.ZERO);
        }
        log.info("已移除訂單優惠券: orderId={}", order.getId().value());
    }
}
