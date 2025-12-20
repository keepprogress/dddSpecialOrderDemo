package com.tgfc.som.pricing.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 促銷計算結果
 *
 * 用於 12-Step 計價流程的 Step 5: promotionCalculation()
 * 記錄單一商品或群組的促銷計算結果
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 21.7</a>
 */
public class PromotionResult {

    private final String eventNo;
    private final PromotionType promotionType;
    private final List<String> participatingSkus;
    private final BigDecimal totalDiscount;
    private final boolean successful;
    private final String failureReason;

    private PromotionResult(Builder builder) {
        this.eventNo = builder.eventNo;
        this.promotionType = builder.promotionType;
        this.participatingSkus = new ArrayList<>(builder.participatingSkus);
        this.totalDiscount = builder.totalDiscount;
        this.successful = builder.successful;
        this.failureReason = builder.failureReason;
    }

    /**
     * 建立空結果 (無促銷)
     */
    public static PromotionResult empty() {
        return new Builder()
            .successful(false)
            .failureReason("NO_PROMOTION")
            .totalDiscount(BigDecimal.ZERO)
            .build();
    }

    /**
     * 建立成功結果
     */
    public static PromotionResult success(String eventNo, PromotionType type,
                                          List<String> skus, BigDecimal discount) {
        return new Builder()
            .eventNo(eventNo)
            .promotionType(type)
            .participatingSkus(skus)
            .totalDiscount(discount)
            .successful(true)
            .build();
    }

    /**
     * 建立失敗結果 (促銷條件不符)
     */
    public static PromotionResult failed(String eventNo, PromotionType type, String reason) {
        return new Builder()
            .eventNo(eventNo)
            .promotionType(type)
            .successful(false)
            .failureReason(reason)
            .totalDiscount(BigDecimal.ZERO)
            .build();
    }

    public String getEventNo() {
        return eventNo;
    }

    public PromotionType getPromotionType() {
        return promotionType;
    }

    public List<String> getParticipatingSkus() {
        return new ArrayList<>(participatingSkus);
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    /**
     * 是否有促銷折扣
     */
    public boolean hasDiscount() {
        return successful && totalDiscount.compareTo(BigDecimal.ZERO) > 0;
    }

    public static class Builder {
        private String eventNo;
        private PromotionType promotionType;
        private List<String> participatingSkus = new ArrayList<>();
        private BigDecimal totalDiscount = BigDecimal.ZERO;
        private boolean successful;
        private String failureReason;

        public Builder eventNo(String eventNo) {
            this.eventNo = eventNo;
            return this;
        }

        public Builder promotionType(PromotionType promotionType) {
            this.promotionType = promotionType;
            return this;
        }

        public Builder participatingSkus(List<String> participatingSkus) {
            this.participatingSkus = participatingSkus != null
                ? new ArrayList<>(participatingSkus)
                : new ArrayList<>();
            return this;
        }

        public Builder totalDiscount(BigDecimal totalDiscount) {
            this.totalDiscount = totalDiscount;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder failureReason(String failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public PromotionResult build() {
            return new PromotionResult(this);
        }
    }
}
