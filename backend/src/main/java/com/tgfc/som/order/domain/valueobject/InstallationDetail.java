package com.tgfc.som.order.domain.valueobject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 安裝明細值物件
 *
 * 記錄訂單行項的安裝服務配置
 */
public record InstallationDetail(
    String workTypeId,           // 工種代碼
    String workTypeName,         // 工種名稱
    List<String> serviceTypes,   // 安裝服務類型清單 (I, IA, IE, IC, IS, FI)
    Money installationCost,      // 安裝費用
    Money laborCost,             // 工資費用
    boolean isMandatory,         // 是否為必要安裝
    BigDecimal discountRate      // 安裝折扣率
) {
    public InstallationDetail {
        Objects.requireNonNull(workTypeId, "工種代碼不可為空");
        Objects.requireNonNull(workTypeName, "工種名稱不可為空");
        serviceTypes = serviceTypes != null ? List.copyOf(serviceTypes) : List.of();
        installationCost = installationCost != null ? installationCost : Money.ZERO;
        laborCost = laborCost != null ? laborCost : Money.ZERO;
        discountRate = discountRate != null ? discountRate : BigDecimal.ONE;
    }

    /**
     * 建立空的安裝明細（無安裝）
     */
    public static InstallationDetail none() {
        return new InstallationDetail(
            "0000", "無安裝",
            List.of(),
            Money.ZERO, Money.ZERO,
            false, BigDecimal.ONE
        );
    }

    /**
     * 建立標準安裝明細
     */
    public static InstallationDetail standard(
            String workTypeId,
            String workTypeName,
            List<String> serviceTypes,
            Money installationCost) {
        return new InstallationDetail(
            workTypeId, workTypeName,
            serviceTypes,
            installationCost, Money.ZERO,
            false, BigDecimal.ONE
        );
    }

    /**
     * 計算安裝總費用
     */
    public Money getTotalCost() {
        return installationCost.add(laborCost);
    }

    /**
     * 計算折扣後安裝費用
     */
    public Money getDiscountedCost() {
        return getTotalCost().multiply(discountRate, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 是否有安裝服務
     */
    public boolean hasInstallation() {
        return !serviceTypes.isEmpty() && !"0000".equals(workTypeId);
    }

    /**
     * 是否包含特定服務類型
     */
    public boolean hasServiceType(String serviceType) {
        return serviceTypes.contains(serviceType);
    }

    /**
     * 是否為純運（無安裝）
     */
    public boolean isPureDelivery() {
        return "0000".equals(workTypeId);
    }

    /**
     * 取得不可變的服務類型清單
     */
    public List<String> getServiceTypes() {
        return Collections.unmodifiableList(serviceTypes);
    }
}
