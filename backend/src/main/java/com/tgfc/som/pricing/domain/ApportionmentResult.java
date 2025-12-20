package com.tgfc.som.pricing.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 工種變價分攤結果
 *
 * 用於 12-Step 計價流程的 Step 2: apportionmentDiscount()
 * 記錄安裝/運送變價分攤到各商品的金額
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Section 21.5</a>
 */
public class ApportionmentResult {

    /**
     * 變價類型
     */
    public enum ChangeType {
        INSTALL("安裝變價"),
        DELIVERY("運送變價");

        private final String name;

        ChangeType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final ChangeType changeType;
    private final BigDecimal totalChangeAmount;
    private final Map<String, BigDecimal> lineApportionments;
    private final String authEmployeeId;

    private ApportionmentResult(Builder builder) {
        this.changeType = builder.changeType;
        this.totalChangeAmount = builder.totalChangeAmount;
        this.lineApportionments = new HashMap<>(builder.lineApportionments);
        this.authEmployeeId = builder.authEmployeeId;
    }

    /**
     * 建立空結果 (無變價)
     */
    public static ApportionmentResult empty(ChangeType type) {
        return new Builder()
            .changeType(type)
            .totalChangeAmount(BigDecimal.ZERO)
            .build();
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public BigDecimal getTotalChangeAmount() {
        return totalChangeAmount;
    }

    public Map<String, BigDecimal> getLineApportionments() {
        return Collections.unmodifiableMap(lineApportionments);
    }

    public String getAuthEmployeeId() {
        return authEmployeeId;
    }

    /**
     * 取得特定明細行的分攤金額
     */
    public BigDecimal getApportionmentFor(String lineId) {
        return lineApportionments.getOrDefault(lineId, BigDecimal.ZERO);
    }

    /**
     * 是否有變價
     */
    public boolean hasChange() {
        return totalChangeAmount.compareTo(BigDecimal.ZERO) != 0;
    }

    public static class Builder {
        private ChangeType changeType;
        private BigDecimal totalChangeAmount = BigDecimal.ZERO;
        private Map<String, BigDecimal> lineApportionments = new HashMap<>();
        private String authEmployeeId;

        public Builder changeType(ChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder totalChangeAmount(BigDecimal totalChangeAmount) {
            this.totalChangeAmount = totalChangeAmount;
            return this;
        }

        public Builder lineApportionments(Map<String, BigDecimal> lineApportionments) {
            this.lineApportionments = lineApportionments != null
                ? new HashMap<>(lineApportionments)
                : new HashMap<>();
            return this;
        }

        public Builder addLineApportionment(String lineId, BigDecimal amount) {
            this.lineApportionments.put(lineId, amount);
            return this;
        }

        public Builder authEmployeeId(String authEmployeeId) {
            this.authEmployeeId = authEmployeeId;
            return this;
        }

        public ApportionmentResult build() {
            return new ApportionmentResult(this);
        }
    }
}
