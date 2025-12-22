package com.tgfc.som.catalog.service;

import com.tgfc.som.catalog.dto.InstallationService;
import com.tgfc.som.order.domain.valueobject.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品服務關聯服務
 *
 * 查詢商品可用的安裝服務
 * 目前使用 Mock 資料，後續會整合商品主檔 API
 */
@Service
public class ProductServiceAssociationService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceAssociationService.class);

    // Mock 商品安裝服務對應表
    // key: 商品類別, value: 可用的安裝服務清單
    private static final Map<String, List<InstallationService>> CATEGORY_SERVICES = Map.of(
        // 家電類 - 需要安裝
        "APPLIANCE", List.of(
            new InstallationService(
                "I", "標準安裝", "SVC-I-001",
                new Money(500), false,
                new BigDecimal("0.85"), new BigDecimal("0.90")
            ),
            new InstallationService(
                "IA", "進階安裝", "SVC-IA-001",
                new Money(1000), false,
                new BigDecimal("0.80"), new BigDecimal("0.85")
            )
        ),
        // 冷氣類 - 必要安裝
        "AC", List.of(
            new InstallationService(
                "I", "標準安裝", "SVC-I-AC",
                new Money(3000), true,
                new BigDecimal("0.85"), new BigDecimal("0.90")
            ),
            new InstallationService(
                "IE", "銅管加長", "SVC-IE-AC",
                new Money(500), false,
                new BigDecimal("0.90"), new BigDecimal("0.95")
            )
        ),
        // 家具類 - 選配安裝
        "FURNITURE", List.of(
            new InstallationService(
                "I", "組裝服務", "SVC-I-FUR",
                new Money(300), false,
                new BigDecimal("0.85"), new BigDecimal("0.90")
            )
        ),
        // 3C 類 - 免費安裝或無需安裝
        "3C", List.of(
            new InstallationService(
                "FI", "免費設定", "SVC-FI-3C",
                Money.ZERO, false,
                BigDecimal.ONE, BigDecimal.ONE
            )
        )
    );

    // Mock 商品類別對應
    private static final Map<String, String> SKU_CATEGORY = Map.of(
        "014014014", "APPLIANCE",
        "015015015", "AC",
        "016016016", "FURNITURE",
        "017017017", "3C",
        "AC001", "AC",
        "TV001", "APPLIANCE",
        "SOFA001", "FURNITURE"
    );

    /**
     * 根據商品編號查詢可用的安裝服務
     *
     * @param skuNo 商品編號
     * @return 可用的安裝服務清單
     */
    public List<InstallationService> getAvailableServices(String skuNo) {
        log.info("查詢商品安裝服務: skuNo={}", skuNo);

        String category = SKU_CATEGORY.getOrDefault(skuNo, "DEFAULT");
        List<InstallationService> services = CATEGORY_SERVICES.getOrDefault(category, List.of());

        log.info("商品 {} 類別={}, 可用服務數={}", skuNo, category, services.size());

        return services;
    }

    /**
     * 根據商品類別查詢可用的安裝服務
     *
     * @param category 商品類別
     * @return 可用的安裝服務清單
     */
    public List<InstallationService> getServicesByCategory(String category) {
        log.info("查詢類別安裝服務: category={}", category);

        List<InstallationService> services = CATEGORY_SERVICES.getOrDefault(category, List.of());

        log.info("類別 {} 可用服務數={}", category, services.size());

        return services;
    }

    /**
     * 查詢商品是否需要必要安裝
     *
     * @param skuNo 商品編號
     * @return 是否需要必要安裝
     */
    public boolean requiresMandatoryInstallation(String skuNo) {
        List<InstallationService> services = getAvailableServices(skuNo);
        return services.stream().anyMatch(InstallationService::isMandatory);
    }

    /**
     * 查詢商品的必要安裝服務
     *
     * @param skuNo 商品編號
     * @return 必要安裝服務（如果有）
     */
    public List<InstallationService> getMandatoryServices(String skuNo) {
        return getAvailableServices(skuNo).stream()
                .filter(InstallationService::isMandatory)
                .toList();
    }

    /**
     * 查詢商品的選配安裝服務
     *
     * @param skuNo 商品編號
     * @return 選配安裝服務清單
     */
    public List<InstallationService> getOptionalServices(String skuNo) {
        return getAvailableServices(skuNo).stream()
                .filter(s -> !s.isMandatory())
                .toList();
    }

    /**
     * 根據服務類型代碼查詢服務詳情
     *
     * @param skuNo 商品編號
     * @param serviceType 服務類型代碼
     * @return 安裝服務（如果存在）
     */
    public InstallationService getServiceByType(String skuNo, String serviceType) {
        return getAvailableServices(skuNo).stream()
                .filter(s -> s.serviceType().equals(serviceType))
                .findFirst()
                .orElse(null);
    }
}
