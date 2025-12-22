package com.tgfc.som.order.domain.valueobject;

import com.tgfc.som.pricing.dto.MemberDiscVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 價格試算結果值物件
 *
 * 對應 7 種 ComputeType：
 * 1. 商品小計 (productTotal)
 * 2. 安裝小計 (installationTotal)
 * 3. 運送小計 (deliveryTotal)
 * 4. 會員卡折扣 (memberDiscount)
 * 5. 直送費用小計 (directShipmentTotal)
 * 6. 折價券折扣 (couponDiscount)
 * 7. 紅利點數折扣 (bonusDiscount)
 */
public final class PriceCalculation {

    private final Money productTotal;
    private final Money installationTotal;
    private final Money deliveryTotal;
    private final Money memberDiscount;
    private final Money directShipmentTotal;
    private final Money couponDiscount;
    private final Money bonusDiscount;
    private final Money taxAmount;
    private final Money grandTotal;
    private final List<MemberDiscVO> memberDiscounts;
    private final List<String> warnings;
    private final boolean promotionSkipped;
    private final LocalDateTime calculatedAt;

    public PriceCalculation(
            Money productTotal,
            Money installationTotal,
            Money deliveryTotal,
            Money memberDiscount,
            Money directShipmentTotal,
            Money couponDiscount,
            Money bonusDiscount,
            Money taxAmount,
            Money grandTotal,
            List<MemberDiscVO> memberDiscounts,
            List<String> warnings,
            boolean promotionSkipped,
            LocalDateTime calculatedAt) {
        this.productTotal = Objects.requireNonNull(productTotal, "productTotal 不可為空");
        this.installationTotal = Objects.requireNonNull(installationTotal, "installationTotal 不可為空");
        this.deliveryTotal = Objects.requireNonNull(deliveryTotal, "deliveryTotal 不可為空");
        this.memberDiscount = Objects.requireNonNull(memberDiscount, "memberDiscount 不可為空");
        this.directShipmentTotal = Objects.requireNonNull(directShipmentTotal, "directShipmentTotal 不可為空");
        this.couponDiscount = Objects.requireNonNull(couponDiscount, "couponDiscount 不可為空");
        this.bonusDiscount = Objects.requireNonNull(bonusDiscount, "bonusDiscount 不可為空");
        this.taxAmount = Objects.requireNonNull(taxAmount, "taxAmount 不可為空");
        this.grandTotal = Objects.requireNonNull(grandTotal, "grandTotal 不可為空");
        this.memberDiscounts = memberDiscounts != null
            ? Collections.unmodifiableList(new ArrayList<>(memberDiscounts))
            : Collections.emptyList();
        this.warnings = warnings != null
            ? Collections.unmodifiableList(new ArrayList<>(warnings))
            : Collections.emptyList();
        this.promotionSkipped = promotionSkipped;
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt 不可為空");
    }

    /**
     * 建立空的價格試算結果
     */
    public static PriceCalculation empty() {
        return new PriceCalculation(
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            Money.ZERO,
            List.of(),
            List.of(),
            false,
            LocalDateTime.now()
        );
    }

    /**
     * 建立 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Money getProductTotal() {
        return productTotal;
    }

    public Money getInstallationTotal() {
        return installationTotal;
    }

    public Money getDeliveryTotal() {
        return deliveryTotal;
    }

    public Money getMemberDiscount() {
        return memberDiscount;
    }

    public Money getDirectShipmentTotal() {
        return directShipmentTotal;
    }

    public Money getCouponDiscount() {
        return couponDiscount;
    }

    public Money getBonusDiscount() {
        return bonusDiscount;
    }

    public Money getTaxAmount() {
        return taxAmount;
    }

    public Money getGrandTotal() {
        return grandTotal;
    }

    public List<MemberDiscVO> getMemberDiscounts() {
        return memberDiscounts;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isPromotionSkipped() {
        return promotionSkipped;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    /**
     * 是否有警告訊息
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * 計算總折扣金額
     */
    public Money getTotalDiscount() {
        return memberDiscount.add(couponDiscount).add(bonusDiscount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceCalculation that = (PriceCalculation) o;
        return promotionSkipped == that.promotionSkipped &&
               Objects.equals(productTotal, that.productTotal) &&
               Objects.equals(installationTotal, that.installationTotal) &&
               Objects.equals(deliveryTotal, that.deliveryTotal) &&
               Objects.equals(memberDiscount, that.memberDiscount) &&
               Objects.equals(directShipmentTotal, that.directShipmentTotal) &&
               Objects.equals(couponDiscount, that.couponDiscount) &&
               Objects.equals(bonusDiscount, that.bonusDiscount) &&
               Objects.equals(taxAmount, that.taxAmount) &&
               Objects.equals(grandTotal, that.grandTotal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productTotal, installationTotal, deliveryTotal,
                memberDiscount, directShipmentTotal, couponDiscount, bonusDiscount,
                taxAmount, grandTotal, promotionSkipped);
    }

    @Override
    public String toString() {
        return "PriceCalculation{" +
               "productTotal=" + productTotal +
               ", installationTotal=" + installationTotal +
               ", deliveryTotal=" + deliveryTotal +
               ", memberDiscount=" + memberDiscount +
               ", directShipmentTotal=" + directShipmentTotal +
               ", couponDiscount=" + couponDiscount +
               ", bonusDiscount=" + bonusDiscount +
               ", taxAmount=" + taxAmount +
               ", grandTotal=" + grandTotal +
               ", promotionSkipped=" + promotionSkipped +
               ", calculatedAt=" + calculatedAt +
               '}';
    }

    /**
     * Builder 類別
     */
    public static class Builder {
        private Money productTotal = Money.ZERO;
        private Money installationTotal = Money.ZERO;
        private Money deliveryTotal = Money.ZERO;
        private Money memberDiscount = Money.ZERO;
        private Money directShipmentTotal = Money.ZERO;
        private Money couponDiscount = Money.ZERO;
        private Money bonusDiscount = Money.ZERO;
        private Money taxAmount = Money.ZERO;
        private Money grandTotal = Money.ZERO;
        private List<MemberDiscVO> memberDiscounts = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private boolean promotionSkipped = false;
        private LocalDateTime calculatedAt = LocalDateTime.now();

        public Builder productTotal(Money productTotal) {
            this.productTotal = productTotal;
            return this;
        }

        public Builder installationTotal(Money installationTotal) {
            this.installationTotal = installationTotal;
            return this;
        }

        public Builder deliveryTotal(Money deliveryTotal) {
            this.deliveryTotal = deliveryTotal;
            return this;
        }

        public Builder memberDiscount(Money memberDiscount) {
            this.memberDiscount = memberDiscount;
            return this;
        }

        public Builder directShipmentTotal(Money directShipmentTotal) {
            this.directShipmentTotal = directShipmentTotal;
            return this;
        }

        public Builder couponDiscount(Money couponDiscount) {
            this.couponDiscount = couponDiscount;
            return this;
        }

        public Builder bonusDiscount(Money bonusDiscount) {
            this.bonusDiscount = bonusDiscount;
            return this;
        }

        public Builder taxAmount(Money taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        public Builder grandTotal(Money grandTotal) {
            this.grandTotal = grandTotal;
            return this;
        }

        public Builder memberDiscounts(List<MemberDiscVO> memberDiscounts) {
            this.memberDiscounts = memberDiscounts != null ? memberDiscounts : new ArrayList<>();
            return this;
        }

        public Builder addMemberDiscount(MemberDiscVO memberDiscount) {
            this.memberDiscounts.add(memberDiscount);
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder promotionSkipped(boolean promotionSkipped) {
            this.promotionSkipped = promotionSkipped;
            return this;
        }

        public Builder calculatedAt(LocalDateTime calculatedAt) {
            this.calculatedAt = calculatedAt;
            return this;
        }

        public PriceCalculation build() {
            return new PriceCalculation(
                productTotal,
                installationTotal,
                deliveryTotal,
                memberDiscount,
                directShipmentTotal,
                couponDiscount,
                bonusDiscount,
                taxAmount,
                grandTotal,
                memberDiscounts,
                warnings,
                promotionSkipped,
                calculatedAt
            );
        }
    }
}
