package com.tgfc.som.catalog.service;

import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.dto.OrderabilityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 商品銷售資格驗證服務測試
 *
 * 測試範圍:
 * - 8-Layer 驗證 (L1-L8)
 * - OD-001~OD-005 後端可訂購規則
 * - 大型家具判斷邏輯
 * - 外包純服務商品判斷
 * - 備貨方式與運送方式決定邏輯
 *
 * 來源: product-query-spec.md, spec.md FR-010
 */
class ProductEligibilityServiceTest {

    private ProductEligibilityService service;

    private static final String CHANNEL_ID = "SO";
    private static final String STORE_ID = "S001";

    @BeforeEach
    void setUp() {
        service = new ProductEligibilityService();
    }

    // ========== L1: 商品編號格式驗證 ==========

    @Nested
    @DisplayName("L1: 商品編號格式驗證")
    class L1FormatValidationTest {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "   ", "A", "AB", "ABC", "ABCD"})
        @DisplayName("L1 失敗: null、空白、少於 5 碼")
        void checkEligibility_invalidFormat_returnsL1Failure(String skuNo) {
            EligibilityResponse result = service.checkEligibility(skuNo, CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(1, result.failureLevel());
            assertEquals("商品編號格式錯誤", result.failureReason());
        }

        @ParameterizedTest
        @ValueSource(strings = {"ABC-123", "ABC_123", "ABC.123", "ABC 123", "ABC!@#"})
        @DisplayName("L1 失敗: 包含特殊字元")
        void checkEligibility_specialCharacters_returnsL1Failure(String skuNo) {
            EligibilityResponse result = service.checkEligibility(skuNo, CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(1, result.failureLevel());
        }

        @Test
        @DisplayName("L1 通過: 有效的英數字格式")
        void isValidSkuFormat_validFormat_returnsTrue() {
            assertTrue(service.isValidSkuFormat("014014014"));
            assertTrue(service.isValidSkuFormat("ABCDE"));
            assertTrue(service.isValidSkuFormat("ABC123456"));
        }
    }

    // ========== L2: 商品是否存在 ==========

    @Nested
    @DisplayName("L2: 商品是否存在")
    class L2ExistenceValidationTest {

        @Test
        @DisplayName("L2 失敗: 商品不存在")
        void checkEligibility_productNotFound_returnsL2Failure() {
            EligibilityResponse result = service.checkEligibility("999999999", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(2, result.failureLevel());
            assertEquals("商品不存在", result.failureReason());
        }

        @Test
        @DisplayName("L2 通過: 商品存在")
        void checkEligibility_productExists_passesL2() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            // 如果 L2 通過，failureLevel 應該不是 2
            assertTrue(result.eligible() || result.failureLevel() != 2);
        }
    }

    // ========== L3: 系統商品驗證 ==========

    @Nested
    @DisplayName("L3: 系統商品驗證")
    class L3SystemProductTest {

        @Test
        @DisplayName("L3 失敗: 系統商品無法銷售")
        void checkEligibility_systemProduct_returnsL3Failure() {
            EligibilityResponse result = service.checkEligibility("SYSSKU01", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(3, result.failureLevel());
            assertEquals("系統商品無法銷售", result.failureReason());
        }

        @Test
        @DisplayName("L3 通過: 非系統商品")
        void checkEligibility_normalProduct_passesL3() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 3);
        }
    }

    // ========== L4: 稅別驗證 ==========

    @Nested
    @DisplayName("L4: 稅別驗證")
    class L4TaxTypeValidationTest {

        @Test
        @DisplayName("L4 通過: 稅別 0 (免稅)")
        void checkEligibility_taxType0_passesL4() {
            // 018018018 稅別為 "0"
            EligibilityResponse result = service.checkEligibility("018018018", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 4);
        }

        @Test
        @DisplayName("L4 通過: 稅別 1 (應稅)")
        void checkEligibility_taxType1_passesL4() {
            // 014014014 稅別為 "1"
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 4);
        }

        @Test
        @DisplayName("L4 通過: 稅別 2 (零稅率)")
        void checkEligibility_taxType2_passesL4() {
            // 017017017 稅別為 "2"
            EligibilityResponse result = service.checkEligibility("017017017", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 4);
        }
    }

    // ========== L5: 禁止銷售驗證 ==========

    @Nested
    @DisplayName("L5: 禁止銷售驗證")
    class L5SalesProhibitedTest {

        @Test
        @DisplayName("L5 失敗: 商品已停售")
        void checkEligibility_salesProhibited_returnsL5Failure() {
            // 019019019 allowSales = false
            EligibilityResponse result = service.checkEligibility("019019019", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(5, result.failureLevel());
            assertEquals("商品已禁止銷售", result.failureReason());
        }

        @Test
        @DisplayName("L5 通過: 商品允許銷售")
        void checkEligibility_salesAllowed_passesL5() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 5);
        }
    }

    // ========== L6: 商品類型驗證 (OD-001) ==========

    @Nested
    @DisplayName("L6: 商品類型驗證 (OD-001)")
    class L6CategoryRestrictionTest {

        @Test
        @DisplayName("L6/OD-001 失敗: 不可訂購的商品類型")
        void checkEligibility_invalidSkuType_returnsL6Failure() {
            // 028028028 skuType = "XXYY"
            EligibilityResponse result = service.checkEligibility("028028028", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(6, result.failureLevel());
            assertEquals("商品類別限制銷售", result.failureReason());
        }

        @ParameterizedTest
        @ValueSource(strings = {"014014014", "015015015", "016016016"})
        @DisplayName("L6/OD-001 通過: 可訂購的商品類型 (NORM)")
        void checkEligibility_validSkuType_passesL6(String skuNo) {
            EligibilityResponse result = service.checkEligibility(skuNo, CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible() || result.failureLevel() != 6);
        }
    }

    // ========== L7: 廠商凍結驗證 (OD-002, OD-003, OD-005) ==========

    @Nested
    @DisplayName("L7: 廠商凍結驗證")
    class L7VendorFreezeTest {

        @Test
        @DisplayName("L7/OD-005 失敗: 無廠商ID")
        void checkEligibility_noVendorId_returnsL7Failure() {
            // 027027027 vendorId = null
            EligibilityResponse result = service.checkEligibility("027027027", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(7, result.failureLevel());
            assertEquals("該SKU未設定廠商ID，無法新增", result.failureReason());
        }

        @Test
        @DisplayName("L7/OD-002 失敗: 非DC商品廠商凍結")
        void checkEligibility_vendorFrozenNonDc_returnsL7Failure() {
            // 022022022 dcType = XD, vendorStatus = "D"
            EligibilityResponse result = service.checkEligibility("022022022", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(7, result.failureLevel());
            assertEquals("廠商已凍結，商品無法訂購", result.failureReason());
        }

        @Test
        @DisplayName("L7/OD-003 通過: DC商品廠商凍結有庫存 - 設置 isDcVendorFrozen")
        void checkEligibility_vendorFrozenDcWithStock_passesWithFlag() {
            // 023023023 dcType = DC, vendorStatus = "D", stockAoh = 50
            EligibilityResponse result = service.checkEligibility("023023023", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertNotNull(result.orderability());
            assertTrue(result.orderability().isDcVendorFrozen());
            assertEquals(50, result.orderability().stockAoh());
        }

        @Test
        @DisplayName("L7/OD-003 通過: DC商品廠商凍結無庫存 - 設置 isDcVendorFrozen")
        void checkEligibility_vendorFrozenDcNoStock_passesWithFlag() {
            // 024024024 dcType = DC, vendorStatus = "D", stockAoh = 0
            EligibilityResponse result = service.checkEligibility("024024024", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertNotNull(result.orderability());
            assertTrue(result.orderability().isDcVendorFrozen());
            assertEquals(0, result.orderability().stockAoh());
        }

        @Test
        @DisplayName("L7 通過: 廠商正常")
        void checkEligibility_vendorActive_passesL7() {
            // 014014014 vendorStatus = "A"
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertFalse(result.orderability().isDcVendorFrozen());
        }
    }

    // ========== L8: 採購組織驗證 (OD-004) ==========

    @Nested
    @DisplayName("L8: 採購組織驗證 (OD-004)")
    class L8PurchaseOrgTest {

        @Test
        @DisplayName("L8/OD-004 失敗: 不在採購組織內")
        void checkEligibility_notInPurchaseOrg_returnsL8Failure() {
            // 025025025 hasSkuCompany = false
            EligibilityResponse result = service.checkEligibility("025025025", CHANNEL_ID, STORE_ID);

            assertFalse(result.eligible());
            assertEquals(8, result.failureLevel());
            assertEquals("商品不在門市採購組織內", result.failureReason());
        }

        @Test
        @DisplayName("L8/OD-004 通過: 在採購組織內")
        void checkEligibility_inPurchaseOrg_passesL8() {
            // 014014014 hasSkuCompany = true
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
        }
    }

    // ========== 大型家具判斷 ==========

    @Nested
    @DisplayName("大型家具判斷邏輯")
    class LargeFurnitureTest {

        @Test
        @DisplayName("大型家具: 大類 040 全判定")
        void checkEligibility_subDept040_isLargeFurniture() {
            // 020020020 subDeptId = "040"
            EligibilityResponse result = service.checkEligibility("020020020", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.isLargeFurniture());
            assertTrue(result.orderability().isLargeFurniture());
        }

        @Test
        @DisplayName("非大型家具: 其他大類")
        void checkEligibility_otherSubDept_notLargeFurniture() {
            // 014014014 subDeptId = "D01"
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertFalse(result.isLargeFurniture());
            assertFalse(result.orderability().isLargeFurniture());
        }
    }

    // ========== 外包純服務商品判斷 ==========

    @Nested
    @DisplayName("外包純服務商品判斷 (026-888)")
    class ServiceSkuTest {

        @Test
        @DisplayName("服務商品: 大類 026 中類 888")
        void checkEligibility_serviceSku_isServiceSku() {
            // 026888001 subDeptId = "026", classId = "888"
            EligibilityResponse result = service.checkEligibility("026888001", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.isServiceSku());
            assertTrue(result.orderability().isServiceSku());
        }

        @Test
        @DisplayName("非服務商品: 其他類別")
        void checkEligibility_normalProduct_notServiceSku() {
            // 014014014 一般商品
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertFalse(result.isServiceSku());
            assertFalse(result.orderability().isServiceSku());
        }
    }

    // ========== 備貨方式判斷 ==========

    @Nested
    @DisplayName("備貨方式判斷")
    class StockMethodsTest {

        @Test
        @DisplayName("一般商品: 可選 X (現貨) 和 Y (訂購)")
        void checkEligibility_normalProduct_hasXAndY() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableStockMethods().contains("X"));
            assertTrue(result.availableStockMethods().contains("Y"));
        }

        @Test
        @DisplayName("DC商品廠商凍結無庫存: 僅可選 X (現貨)")
        void checkEligibility_dcVendorFrozenNoStock_onlyX() {
            // 024024024 dcType = DC, vendorStatus = "D", stockAoh = 0
            EligibilityResponse result = service.checkEligibility("024024024", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableStockMethods().contains("X"));
            assertFalse(result.availableStockMethods().contains("Y"));
        }

        @Test
        @DisplayName("DC商品廠商凍結有庫存: 可選 X 和 Y")
        void checkEligibility_dcVendorFrozenWithStock_hasXAndY() {
            // 023023023 dcType = DC, vendorStatus = "D", stockAoh = 50
            EligibilityResponse result = service.checkEligibility("023023023", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableStockMethods().contains("X"));
            assertTrue(result.availableStockMethods().contains("Y"));
        }
    }

    // ========== 運送方式判斷 ==========

    @Nested
    @DisplayName("運送方式判斷")
    class DeliveryMethodsTest {

        @Test
        @DisplayName("一般商品: 包含 N, C, P, D, F")
        void checkEligibility_normalProduct_hasStandardMethods() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableDeliveryMethods().contains("N")); // 運送
            assertTrue(result.availableDeliveryMethods().contains("C")); // 當場自取
            assertTrue(result.availableDeliveryMethods().contains("P")); // 下次自取
            assertTrue(result.availableDeliveryMethods().contains("D")); // 純運
            assertTrue(result.availableDeliveryMethods().contains("F")); // 宅配
        }

        @Test
        @DisplayName("服務商品: 不包含 D 和 F")
        void checkEligibility_serviceSku_noDAndF() {
            // 026888001 服務商品
            EligibilityResponse result = service.checkEligibility("026888001", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableDeliveryMethods().contains("N"));
            assertFalse(result.availableDeliveryMethods().contains("D"));
            assertFalse(result.availableDeliveryMethods().contains("F"));
        }

        @Test
        @DisplayName("可直送商品: 包含 V")
        void checkEligibility_allowDirectShipment_hasV() {
            // 020020020 allowDirectShipment = true
            EligibilityResponse result = service.checkEligibility("020020020", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableDeliveryMethods().contains("V"));
        }

        @Test
        @DisplayName("免運商品: 包含 V")
        void checkEligibility_freeDelivery_hasV() {
            // 021021021 freeDelivery = true
            EligibilityResponse result = service.checkEligibility("021021021", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertTrue(result.availableDeliveryMethods().contains("V"));
        }
    }

    // ========== 安裝服務判斷 ==========

    @Nested
    @DisplayName("安裝服務判斷")
    class InstallationServicesTest {

        @Test
        @DisplayName("服務商品: 有強制安裝服務")
        void checkEligibility_serviceSku_hasMandatoryInstall() {
            // 026888001 服務商品
            EligibilityResponse result = service.checkEligibility("026888001", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertFalse(result.availableServices().isEmpty());
            assertTrue(result.availableServices().get(0).isMandatory());
            assertEquals("I", result.availableServices().get(0).serviceType());
        }

        @Test
        @DisplayName("一般商品: 有可選安裝服務")
        void checkEligibility_normalProduct_hasOptionalInstall() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertFalse(result.availableServices().isEmpty());
            // 包含 I, IA, FI 三種服務
            assertTrue(result.availableServices().stream()
                    .anyMatch(s -> "I".equals(s.serviceType())));
            assertTrue(result.availableServices().stream()
                    .anyMatch(s -> "IA".equals(s.serviceType())));
            assertTrue(result.availableServices().stream()
                    .anyMatch(s -> "FI".equals(s.serviceType())));
        }
    }

    // ========== 完整流程驗證 ==========

    @Nested
    @DisplayName("完整流程驗證")
    class FullFlowTest {

        @Test
        @DisplayName("正常商品: 8 層驗證全通過")
        void checkEligibility_normalProduct_passesAllLayers() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertEquals(0, result.failureLevel());
            assertNull(result.failureReason());
            assertNotNull(result.product());
            assertNotNull(result.orderability());
            assertFalse(result.availableStockMethods().isEmpty());
            assertFalse(result.availableDeliveryMethods().isEmpty());
        }

        @Test
        @DisplayName("商品資訊完整性")
        void checkEligibility_normalProduct_hasCompleteProductInfo() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            assertNotNull(result.product());
            assertEquals("014014014", result.product().skuNo());
            assertEquals("測試冰箱 500L", result.product().skuName());
            assertEquals("NORM", result.product().skuType());
            assertEquals(29900, result.product().posPrice());
        }

        @Test
        @DisplayName("可訂購性結果完整性")
        void checkEligibility_normalProduct_hasCompleteOrderability() {
            EligibilityResponse result = service.checkEligibility("014014014", CHANNEL_ID, STORE_ID);

            assertTrue(result.eligible());
            OrderabilityResult orderability = result.orderability();
            assertNotNull(orderability);
            assertTrue(orderability.orderable());
            assertNull(orderability.lockReason());
            assertFalse(orderability.isDcVendorFrozen());
        }
    }

    // ========== 錯誤訊息測試 ==========

    @Nested
    @DisplayName("錯誤訊息測試")
    class ErrorMessageTest {

        @Test
        @DisplayName("取得各層級錯誤訊息")
        void getEligibilityErrorMessage_returnsCorrectMessage() {
            assertEquals("商品編號格式錯誤", service.getEligibilityErrorMessage(1));
            assertEquals("商品不存在", service.getEligibilityErrorMessage(2));
            assertEquals("系統商品無法銷售", service.getEligibilityErrorMessage(3));
            assertEquals("稅別設定錯誤", service.getEligibilityErrorMessage(4));
            assertEquals("商品已禁止銷售", service.getEligibilityErrorMessage(5));
            assertEquals("商品類別限制銷售", service.getEligibilityErrorMessage(6));
            assertEquals("廠商已凍結", service.getEligibilityErrorMessage(7));
            assertEquals("不在採購組織內", service.getEligibilityErrorMessage(8));
            assertEquals("商品不符合銷售資格", service.getEligibilityErrorMessage(99));
        }
    }

    // ========== OrderabilityResult 測試 ==========

    @Nested
    @DisplayName("OrderabilityResult 測試")
    class OrderabilityResultTest {

        @Test
        @DisplayName("requiresSpotStock: DC廠商凍結 + 非大型家具 + 庫存不足")
        void requiresSpotStock_dcVendorFrozenNoStock_returnsTrue() {
            OrderabilityResult result = OrderabilityResult.dcVendorFrozen(0, false);

            assertTrue(result.requiresSpotStock(1)); // 需要 1 個但庫存 0
            assertTrue(result.requiresSpotStock(10));
        }

        @Test
        @DisplayName("requiresSpotStock: DC廠商凍結 + 非大型家具 + 庫存足夠")
        void requiresSpotStock_dcVendorFrozenWithStock_returnsFalse() {
            OrderabilityResult result = OrderabilityResult.dcVendorFrozen(50, false);

            assertFalse(result.requiresSpotStock(1));
            assertFalse(result.requiresSpotStock(50));
            assertTrue(result.requiresSpotStock(51)); // 庫存不足
        }

        @Test
        @DisplayName("requiresSpotStock: DC廠商凍結 + 大型家具 → 不強制現貨")
        void requiresSpotStock_dcVendorFrozenLargeFurniture_returnsFalse() {
            OrderabilityResult result = OrderabilityResult.dcVendorFrozen(0, true);

            assertFalse(result.requiresSpotStock(1)); // 大型家具不強制現貨
            assertFalse(result.requiresSpotStock(100));
        }

        @Test
        @DisplayName("一般可訂購結果")
        void orderable_normalProduct_hasCorrectFlags() {
            OrderabilityResult result = OrderabilityResult.canOrder(false, false);

            assertTrue(result.orderable());
            assertFalse(result.isDcVendorFrozen());
            assertFalse(result.isLargeFurniture());
            assertFalse(result.isServiceSku());
        }
    }
}
