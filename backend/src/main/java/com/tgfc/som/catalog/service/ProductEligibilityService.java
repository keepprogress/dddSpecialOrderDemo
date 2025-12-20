package com.tgfc.som.catalog.service;

import com.tgfc.som.catalog.domain.DcType;
import com.tgfc.som.catalog.domain.HoldOrderType;
import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.dto.EligibilityResponse.InstallationServiceInfo;
import com.tgfc.som.catalog.dto.OrderabilityResult;
import com.tgfc.som.catalog.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品銷售資格驗證服務
 *
 * 實作 8-Layer 驗證（來源: product-query-spec.md, spec.md FR-010）：
 * L1. 商品編號格式驗證
 * L2. 商品是否存在
 * L3. 是否為系統商品 (allowSales)
 * L4. 稅別設定是否正確
 * L5. 是否已禁止銷售 (holdOrder)
 * L6. 商品類別是否限制銷售
 * L7. 廠商凍結驗證 (TBL_VENDOR_COMPANY.STATUS)
 * L8. 採購組織驗證 (TBL_SKU_COMPANY)
 *
 * 可訂購規則 OD-001~OD-005 (後端):
 * OD-001: 商品類型限制
 * OD-002: 廠商凍結(非DC)
 * OD-003: 廠商凍結(DC) → isDcVendorStatusD
 * OD-004: 採購組織限制
 * OD-005: 無廠商ID
 */
@Service
public class ProductEligibilityService {

    private static final Logger log = LoggerFactory.getLogger(ProductEligibilityService.class);

    // 可訂購商品類型清單 (來源: TBL_SUB_CONFIG.MASTER_CONFIG_ID='C08')
    private static final Set<String> PURCHASABLE_SKU_TYPES = Set.of(
        "NORM", "DIEN", "COUP", "GIFT", "SAMP", "REPL"
    );

    // 大型家具類別設定 (來源: TBL_PARM_DETL.PARM='LARGE_FURNITURE')
    private static final List<LargeFurnitureConfig> LARGE_FURNITURE_CONFIGS = List.of(
        new LargeFurnitureConfig("040", null, null),      // 大類 040 全判定
        new LargeFurnitureConfig("041", "001", null),     // 大類 041 中類 001
        new LargeFurnitureConfig("041", "002", "001")     // 大類 041 中類 002 小類 001
    );

    // Mock 商品資料
    private static final Map<String, ProductInfo> MOCK_PRODUCTS = createMockProducts();

    private static Map<String, ProductInfo> createMockProducts() {
        return Map.ofEntries(
            Map.entry("014014014", ProductInfo.builder()
                .skuNo("014014014").skuName("測試冰箱 500L")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(35000).regularPrice(32000).posPrice(29900).cost(20000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(100)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            Map.entry("015015015", ProductInfo.builder()
                .skuNo("015015015").skuName("測試洗衣機 15KG")
                .skuType("NORM").subDeptId("D01").classId("C02").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(28000).regularPrice(25000).posPrice(23900).cost(16000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(50)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            Map.entry("016016016", ProductInfo.builder()
                .skuNo("016016016").skuName("測試冷氣 分離式 1對1")
                .skuType("NORM").subDeptId("D02").classId("C03").subClassId("S01")
                .taxType("1").vendorId("V002").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(45000).regularPrice(42000).posPrice(39900).cost(28000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(30)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            Map.entry("017017017", ProductInfo.builder()
                .skuNo("017017017").skuName("測試電視 55吋")
                .skuType("NORM").subDeptId("D03").classId("C04").subClassId("S01")
                .taxType("2").vendorId("V003").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(32000).regularPrice(30000).posPrice(28900).cost(18000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(20)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            Map.entry("018018018", ProductInfo.builder()
                .skuNo("018018018").skuName("測試淨水器")
                .skuType("NORM").subDeptId("D04").classId("C05").subClassId("S01")
                .taxType("0").vendorId("V004").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(12000).regularPrice(11000).posPrice(9900).cost(6000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(100)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L5 測試: 已停售商品
            Map.entry("019019019", ProductInfo.builder()
                .skuNo("019019019").skuName("已停售商品")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.C)
                .marketPrice(25000).regularPrice(23000).posPrice(21900).cost(15000)
                .allowSales(false).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(0)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L3 測試: 系統商品
            Map.entry("SYSSKU01", ProductInfo.builder()
                .skuNo("SYSSKU01").skuName("系統運費商品")
                .skuType("NORM").subDeptId("025").classId("SYS").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(0).regularPrice(0).posPrice(0).cost(0)
                .allowSales(true).isSystemSku(true).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(0)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // 大型家具商品
            Map.entry("020020020", ProductInfo.builder()
                .skuNo("020020020").skuName("可直送大型家具")
                .skuType("NORM").subDeptId("040").classId("C06").subClassId("S01")
                .taxType("1").vendorId("V005").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(55000).regularPrice(52000).posPrice(49900).cost(35000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .allowDirectShipment(true)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(10)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // 免運小家電
            Map.entry("021021021", ProductInfo.builder()
                .skuNo("021021021").skuName("免運小家電")
                .skuType("NORM").subDeptId("D06").classId("C07").subClassId("S01")
                .taxType("1").vendorId("V006").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(3500).regularPrice(3200).posPrice(2990).cost(2000)
                .allowSales(true).isSystemSku(false)
                .freeDelivery(true).freeDeliveryShipping(true)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(200)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L7 測試: 廠商凍結 (非DC)
            Map.entry("022022022", ProductInfo.builder()
                .skuNo("022022022").skuName("廠商凍結商品-XD")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V007").dcType(DcType.XD).holdOrderType(HoldOrderType.N)
                .marketPrice(15000).regularPrice(14000).posPrice(12900).cost(8000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("D").hasSkuCompany(true).stockAoh(0)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L7 測試: 廠商凍結 (DC 商品，需查 AOH)
            Map.entry("023023023", ProductInfo.builder()
                .skuNo("023023023").skuName("廠商凍結商品-DC有庫存")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V008").dcType(DcType.DC).holdOrderType(HoldOrderType.N)
                .marketPrice(18000).regularPrice(17000).posPrice(15900).cost(10000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("D").hasSkuCompany(true).stockAoh(50)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L7 測試: 廠商凍結 (DC 商品，無庫存)
            Map.entry("024024024", ProductInfo.builder()
                .skuNo("024024024").skuName("廠商凍結商品-DC無庫存")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V008").dcType(DcType.DC).holdOrderType(HoldOrderType.N)
                .marketPrice(18000).regularPrice(17000).posPrice(15900).cost(10000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("D").hasSkuCompany(true).stockAoh(0)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L8 測試: 不在採購組織
            Map.entry("025025025", ProductInfo.builder()
                .skuNo("025025025").skuName("非採購組織商品")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(10000).regularPrice(9000).posPrice(8900).cost(5000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(false).stockAoh(100)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // 外包純服務商品 (026-888)
            Map.entry("026888001", ProductInfo.builder()
                .skuNo("026888001").skuName("外包安裝服務")
                .skuType("NORM").subDeptId("026").classId("888").subClassId("001")
                .taxType("1").vendorId("V009").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(0).regularPrice(0).posPrice(0).cost(0)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(0)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // OD-005 測試: 無廠商ID
            Map.entry("027027027", ProductInfo.builder()
                .skuNo("027027027").skuName("無廠商ID商品")
                .skuType("NORM").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId(null).dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(5000).regularPrice(4500).posPrice(3990).cost(2000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(100)
                .skuStatus("A").skuStoreStatus("A")
                .build()),
            // L6 測試: 商品類型不可訂購
            Map.entry("028028028", ProductInfo.builder()
                .skuNo("028028028").skuName("不可訂購類型商品")
                .skuType("XXYY").subDeptId("D01").classId("C01").subClassId("S01")
                .taxType("1").vendorId("V001").dcType(null).holdOrderType(HoldOrderType.N)
                .marketPrice(5000).regularPrice(4500).posPrice(3990).cost(2000)
                .allowSales(true).isSystemSku(false).freeDelivery(false)
                .vendorStatus("A").hasSkuCompany(true).stockAoh(100)
                .skuStatus("A").skuStoreStatus("A")
                .build())
        );
    }

    /**
     * 檢查商品銷售資格 - 8 層驗證
     *
     * @param skuNo 商品編號
     * @param channelId 通路代號
     * @param storeId 店別代號
     * @return 資格驗證結果
     */
    public EligibilityResponse checkEligibility(String skuNo, String channelId, String storeId) {
        log.info("檢查商品銷售資格: skuNo={}, channelId={}, storeId={}", skuNo, channelId, storeId);

        // L1: 商品編號格式驗證
        if (!isValidSkuFormat(skuNo)) {
            log.warn("L1 失敗 - 商品編號格式錯誤: skuNo={}", skuNo);
            return EligibilityResponse.formatNotValid();
        }

        // L2: 商品是否存在
        ProductInfo product = MOCK_PRODUCTS.get(skuNo.toUpperCase());
        if (product == null) {
            log.warn("L2 失敗 - 商品不存在: skuNo={}", skuNo);
            return EligibilityResponse.productNotFound();
        }

        // L3: 是否為系統商品
        if (product.isSystemSku()) {
            log.warn("L3 失敗 - 系統商品無法銷售: skuNo={}", skuNo);
            return EligibilityResponse.systemProductNotAllowed();
        }

        // L4: 稅別設定是否正確
        if (!isValidTaxType(product.taxType())) {
            log.warn("L4 失敗 - 稅別設定錯誤: skuNo={}, taxType={}", skuNo, product.taxType());
            return EligibilityResponse.invalidTaxType();
        }

        // L5: 是否已禁止銷售
        if (!product.allowSales()) {
            log.warn("L5 失敗 - 商品已禁止銷售: skuNo={}", skuNo);
            return EligibilityResponse.salesProhibited();
        }

        // L6: 商品類別是否限制銷售 (OD-001)
        if (!PURCHASABLE_SKU_TYPES.contains(product.skuType())) {
            log.warn("L6 失敗 - 商品類型不可訂購: skuNo={}, skuType={}", skuNo, product.skuType());
            return EligibilityResponse.categoryRestricted();
        }

        // L7: 廠商凍結驗證 (OD-002, OD-003, OD-005)
        EligibilityResponse vendorCheckResult = checkVendorStatus(product);
        if (vendorCheckResult != null) {
            return vendorCheckResult;
        }

        // L8: 採購組織驗證 (OD-004)
        if (!product.hasSkuCompany()) {
            log.warn("L8 失敗 - 不在採購組織內: skuNo={}", skuNo);
            return EligibilityResponse.notInPurchaseOrg();
        }

        // 所有驗證通過，判斷大型家具和外包服務商品
        boolean isLargeFurniture = checkIsLargeFurniture(product);
        boolean isServiceSku = product.isServiceSku();

        // 組裝可訂購性結果
        OrderabilityResult orderability = buildOrderabilityResult(product, isLargeFurniture);

        // 組裝可用選項
        List<String> stockMethods = determineStockMethods(product, orderability);
        List<String> deliveryMethods = determineDeliveryMethods(product);
        List<InstallationServiceInfo> services = determineInstallationServices(product, isServiceSku);

        log.info("商品銷售資格驗證通過: skuNo={}, isLargeFurniture={}, isServiceSku={}, isDcVendorFrozen={}",
                skuNo, isLargeFurniture, isServiceSku, orderability.isDcVendorFrozen());

        return EligibilityResponse.success(
            product, orderability, services, stockMethods, deliveryMethods,
            isLargeFurniture, isServiceSku
        );
    }

    /**
     * L7: 廠商狀態驗證
     */
    private EligibilityResponse checkVendorStatus(ProductInfo product) {
        // OD-005: 無廠商ID
        if (product.vendorId() == null || product.vendorId().isBlank()) {
            log.warn("L7 失敗 - 無廠商ID: skuNo={}", product.skuNo());
            return EligibilityResponse.noVendorId();
        }

        // 廠商凍結檢查
        if (product.isVendorFrozen()) {
            DcType dcType = product.dcType();

            // OD-003: DC 商品廠商凍結 → 設置 isDcVendorStatusD，後續判斷
            if (dcType == DcType.DC) {
                log.info("L7 - DC商品廠商凍結，需查AOH: skuNo={}, stockAoh={}",
                        product.skuNo(), product.stockAoh());
                // 不回傳失敗，讓後續流程處理
                return null;
            }

            // OD-002: 非DC商品廠商凍結 → 直接不可訂購
            log.warn("L7 失敗 - 廠商凍結(非DC): skuNo={}, dcType={}", product.skuNo(), dcType);
            return EligibilityResponse.vendorFrozen();
        }

        return null; // 通過
    }

    /**
     * 判斷大型家具
     * 來源: LargeFurnitureService.java:31-60
     */
    private boolean checkIsLargeFurniture(ProductInfo product) {
        String subDeptId = product.subDeptId();
        String classId = product.classId();
        String subClassId = product.subClassId();

        for (LargeFurnitureConfig config : LARGE_FURNITURE_CONFIGS) {
            // 大類必須相等
            if (!config.subDeptId.equals(subDeptId)) {
                continue;
            }

            // 中類為 null → 大類符合即判定
            if (config.classId == null) {
                return true;
            }

            // 中類必須相等
            if (!config.classId.equals(classId)) {
                continue;
            }

            // 小類為 null → 大中類符合即判定
            if (config.subClassId == null) {
                return true;
            }

            // 大中小類皆符合
            if (config.subClassId.equals(subClassId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 組裝可訂購性結果
     */
    private OrderabilityResult buildOrderabilityResult(ProductInfo product, boolean isLargeFurniture) {
        if (product.isDcVendorFrozen()) {
            return OrderabilityResult.dcVendorFrozen(product.stockAoh(), isLargeFurniture);
        }
        return OrderabilityResult.canOrder(isLargeFurniture, product.isServiceSku());
    }

    /**
     * 判斷可用備貨方式
     */
    private List<String> determineStockMethods(ProductInfo product, OrderabilityResult orderability) {
        List<String> methods = new ArrayList<>();
        methods.add("X"); // 現貨總是可用

        // DC商品廠商凍結且非大型家具且無庫存 → 僅現貨
        if (orderability.isDcVendorFrozen() && !orderability.isLargeFurniture() && orderability.stockAoh() <= 0) {
            log.info("備貨方式限制為現貨: skuNo={}, isDcVendorFrozen=true, stockAoh=0",
                    product.skuNo());
            return methods;
        }

        methods.add("Y"); // 訂購
        return methods;
    }

    /**
     * 根據商品屬性決定可用的運送方式
     */
    private List<String> determineDeliveryMethods(ProductInfo product) {
        List<String> methods = new ArrayList<>();
        methods.add("N"); // 運送
        methods.add("C"); // 當場自取
        methods.add("P"); // 下次自取

        if (!product.isServiceSku()) {
            methods.add("D"); // 純運
            methods.add("F"); // 宅配
        }

        if (product.allowDirectShipment() || product.freeDelivery()) {
            methods.add("V"); // 直送
        }

        return methods;
    }

    /**
     * 決定可用安裝服務
     */
    private List<InstallationServiceInfo> determineInstallationServices(ProductInfo product, boolean isServiceSku) {
        List<InstallationServiceInfo> services = new ArrayList<>();

        if (isServiceSku) {
            // 外包服務商品固定有安裝
            services.add(new InstallationServiceInfo("I", "標準安裝", product.skuNo(), 0, true));
            return services;
        }

        // 一般商品根據類別判斷 (簡化版)
        if (product.subDeptId() != null && product.subDeptId().startsWith("D0")) {
            services.add(new InstallationServiceInfo("I", "標準安裝", "INST001", 500, false));
            services.add(new InstallationServiceInfo("IA", "進階安裝", "INST002", 1000, false));
            services.add(new InstallationServiceInfo("FI", "免安折扣", "FREE001", -300, false));
        }

        return services;
    }

    /**
     * L1: 驗證商品編號格式
     */
    public boolean isValidSkuFormat(String skuNo) {
        if (skuNo == null || skuNo.isBlank()) {
            return false;
        }
        // SKU 格式: 英數字，至少 5 碼
        return skuNo.matches("^[A-Za-z0-9]{5,}$");
    }

    /**
     * L4: 驗證稅別
     */
    private boolean isValidTaxType(String taxType) {
        return taxType != null && (
            taxType.equals("0") ||
            taxType.equals("1") ||
            taxType.equals("2")
        );
    }

    /**
     * 取得資格驗證失敗訊息
     */
    public String getEligibilityErrorMessage(int level) {
        return switch (level) {
            case 1 -> "商品編號格式錯誤";
            case 2 -> "商品不存在";
            case 3 -> "系統商品無法銷售";
            case 4 -> "稅別設定錯誤";
            case 5 -> "商品已禁止銷售";
            case 6 -> "商品類別限制銷售";
            case 7 -> "廠商已凍結";
            case 8 -> "不在採購組織內";
            default -> "商品不符合銷售資格";
        };
    }

    /**
     * 大型家具設定 (來源: TBL_PARM_DETL.PARM='LARGE_FURNITURE')
     */
    private record LargeFurnitureConfig(String subDeptId, String classId, String subClassId) {}
}
