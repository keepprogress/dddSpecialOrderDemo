package com.tgfc.som.catalog.service;

import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.dto.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 商品銷售資格驗證服務
 *
 * 實作 6-Layer 驗證：
 * 1. 商品編號格式驗證
 * 2. 商品是否存在
 * 3. 是否為系統商品
 * 4. 稅別設定是否正確
 * 5. 是否已禁止銷售
 * 6. 商品類別是否限制銷售
 */
@Service
public class ProductEligibilityService {

    private static final Logger log = LoggerFactory.getLogger(ProductEligibilityService.class);

    // Mock 商品資料
    private static final Map<String, ProductInfo> MOCK_PRODUCTS = Map.of(
        "014014014", new ProductInfo(
            "014014014", "測試冰箱 500L", "CAT01", "冰箱", "1",
            35000, 32000, 29900, 20000,
            true, false, false, false, false, false, false, true
        ),
        "015015015", new ProductInfo(
            "015015015", "測試洗衣機 15KG", "CAT02", "洗衣機", "1",
            28000, 25000, 23900, 16000,
            true, false, false, false, false, false, false, true
        ),
        "016016016", new ProductInfo(
            "016016016", "測試冷氣 分離式 1對1", "CAT03", "冷氣", "1",
            45000, 42000, 39900, 28000,
            true, false, false, false, false, false, false, true
        ),
        "017017017", new ProductInfo(
            "017017017", "測試電視 55吋", "CAT04", "電視", "2",
            32000, 30000, 28900, 18000,
            true, false, false, false, false, false, false, true
        ),
        "018018018", new ProductInfo(
            "018018018", "測試淨水器", "CAT05", "淨水器", "0",
            12000, 11000, 9900, 6000,
            true, false, false, false, false, false, false, true
        ),
        "019019019", new ProductInfo(
            "019019019", "已停售商品", "CAT01", "冰箱", "1",
            25000, 23000, 21900, 15000,
            false, false, false, false, false, false, false, true
        ),
        "SYSSKU01", new ProductInfo(
            "SYSSKU01", "系統運費商品", "SYS", "系統", "1",
            0, 0, 0, 0,
            true, false, true, false, false, false, false, false
        ),
        "020020020", new ProductInfo(
            "020020020", "可直送大型家具", "CAT06", "家具", "1",
            55000, 52000, 49900, 35000,
            true, false, false, false, false, false, true, true
        ),
        "021021021", new ProductInfo(
            "021021021", "免運小家電", "CAT07", "小家電", "1",
            3500, 3200, 2990, 2000,
            true, false, false, false, true, true, false, true
        )
    );

    /**
     * 檢查商品銷售資格
     *
     * @param skuNo 商品編號
     * @param channelId 通路代號
     * @param storeId 店別代號
     * @return 資格驗證結果
     */
    public EligibilityResponse checkEligibility(String skuNo, String channelId, String storeId) {
        log.info("檢查商品銷售資格: skuNo={}, channelId={}, storeId={}", skuNo, channelId, storeId);

        // Layer 1: 商品編號格式驗證
        if (!isValidSkuFormat(skuNo)) {
            log.warn("商品編號格式錯誤: skuNo={}", skuNo);
            return EligibilityResponse.formatNotValid();
        }

        // Layer 2: 商品是否存在
        ProductInfo product = MOCK_PRODUCTS.get(skuNo.toUpperCase());
        if (product == null) {
            log.warn("商品不存在: skuNo={}", skuNo);
            return EligibilityResponse.productNotFound();
        }

        // Layer 3: 是否為系統商品
        if (product.isSystemSku()) {
            log.warn("系統商品無法銷售: skuNo={}", skuNo);
            return EligibilityResponse.systemProductNotAllowed();
        }

        // Layer 4: 稅別設定是否正確
        if (!isValidTaxType(product.taxType())) {
            log.warn("稅別設定錯誤: skuNo={}, taxType={}", skuNo, product.taxType());
            return EligibilityResponse.invalidTaxType();
        }

        // Layer 5: 是否已禁止銷售
        if (!product.allowSales() || product.holdOrder()) {
            log.warn("商品已禁止銷售: skuNo={}", skuNo);
            return EligibilityResponse.salesProhibited();
        }

        // Layer 6: 商品類別是否限制銷售（目前未實作通路/店別限制）
        // 可根據 channelId 和 storeId 進行更細緻的限制

        // 驗證通過，組裝可用選項
        List<String> stockMethods = List.of("X", "Y");
        List<String> deliveryMethods = determineDeliveryMethods(product);

        log.info("商品銷售資格驗證通過: skuNo={}", skuNo);

        return EligibilityResponse.success(
            product,
            List.of(), // 安裝服務（暫時為空）
            stockMethods,
            deliveryMethods
        );
    }

    /**
     * 驗證商品編號格式
     */
    public boolean isValidSkuFormat(String skuNo) {
        if (skuNo == null || skuNo.isBlank()) {
            return false;
        }
        // SKU 格式: 英數字，至少 5 碼
        return skuNo.matches("^[A-Za-z0-9]{5,}$");
    }

    /**
     * 驗證稅別
     */
    private boolean isValidTaxType(String taxType) {
        return taxType != null && (
            taxType.equals("0") ||
            taxType.equals("1") ||
            taxType.equals("2")
        );
    }

    /**
     * 根據商品屬性決定可用的運送方式
     */
    private List<String> determineDeliveryMethods(ProductInfo product) {
        if (product.allowDirectShipment()) {
            return List.of("N", "D", "V", "C", "F", "P");
        } else if (product.freeDelivery()) {
            return List.of("N", "F", "C", "P");
        } else {
            return List.of("N", "D", "C", "F", "P");
        }
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
            default -> "商品不符合銷售資格";
        };
    }
}
