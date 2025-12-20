package com.tgfc.som.pricing.service;

import com.tgfc.som.fulfillment.dto.WorkType;
import com.tgfc.som.fulfillment.service.WorkTypeService;
import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.DeliveryMethod;
import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.StockMethod;
import com.tgfc.som.order.domain.TaxType;
import com.tgfc.som.order.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PriceCalculationService 單元測試
 *
 * 測試 pricing flow
 */
@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    @Mock
    private WorkTypeService workTypeService;

    private MemberDiscountService memberDiscountService;
    private PromotionService promotionService;
    private ApportionmentService apportionmentService;
    private com.tgfc.som.catalog.service.ProductEligibilityService productEligibilityService;
    private PriceCalculationService priceCalculationService;

    @BeforeEach
    void setUp() {
        memberDiscountService = new MemberDiscountService();
        promotionService = new PromotionService();
        apportionmentService = new ApportionmentService();
        productEligibilityService = new com.tgfc.som.catalog.service.ProductEligibilityService();

        priceCalculationService = new PriceCalculationService(
                workTypeService,
                memberDiscountService,
                promotionService,
                apportionmentService,
                productEligibilityService
        );
    }

    @Nested
    @DisplayName("商品小計計算 (ComputeType 1)")
    class ProductTotalTests {

        @Test
        @DisplayName("單一商品: 100 × 2 = 200")
        void shouldCalculateProductTotalForSingleLine() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 2, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertEquals(200, result.getProductTotal().amount());
        }

        @Test
        @DisplayName("多個商品: (100 × 2) + (200 × 3) = 800")
        void shouldCalculateProductTotalForMultipleLines() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品1", 2, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);
            order.addLine("015015015", "測試商品2", 3, Money.of(200),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertEquals(800, result.getProductTotal().amount());
        }
    }

    @Nested
    @DisplayName("會員折扣計算 (ComputeType 4)")
    class MemberDiscountTests {

        @Test
        @DisplayName("Type 0 折扣: 原價 1000 × 0.95 = 折扣金額 -50")
        void shouldCalculateMemberDiscountForType0() {
            // Arrange
            Order order = createOrderWithDiscount(MemberDiscountType.DISCOUNTING);
            order.addLine("014014014", "測試商品", 2, Money.of(500),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            // 原價 1000，95 折 → 折扣金額 -50
            assertEquals(-50, result.getMemberDiscount().amount());
            assertFalse(result.getMemberDiscounts().isEmpty());
        }

        @Test
        @DisplayName("無會員折扣類型時，折扣為零")
        void shouldReturnZeroDiscountWhenNoDiscountType() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 2, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertEquals(0, result.getMemberDiscount().amount());
            assertTrue(result.getMemberDiscounts().isEmpty());
        }
    }

    @Nested
    @DisplayName("最低工資驗證")
    class MinimumWageValidationTests {

        @Test
        @DisplayName("安裝費用低於最低工資時應產生警告")
        void shouldGenerateWarningWhenBelowMinimumWage() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 1, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // 設定安裝明細（費用 50，低於最低工資 100）
            var line = order.getLines().get(0);
            line.setInstallationDetail(InstallationDetail.standard(
                    "WT001", "電器安裝",
                    java.util.List.of("I"),
                    Money.of(50)
            ));

            when(workTypeService.getWorkType("WT001")).thenReturn(
                    Optional.of(WorkType.testWorkType("WT001", "電器安裝", 100))
            );

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertFalse(result.getWarnings().isEmpty());
            assertTrue(result.getWarnings().get(0).contains("低於工種"));
        }

        @Test
        @DisplayName("安裝費用符合最低工資時不應產生警告")
        void shouldNotGenerateWarningWhenMeetsMinimumWage() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 1, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // 設定安裝明細（費用 150，符合最低工資 100）
            var line = order.getLines().get(0);
            line.setInstallationDetail(InstallationDetail.standard(
                    "WT001", "電器安裝",
                    java.util.List.of("I"),
                    Money.of(150)
            ));

            when(workTypeService.getWorkType("WT001")).thenReturn(
                    Optional.of(WorkType.testWorkType("WT001", "電器安裝", 100))
            );

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertTrue(result.getWarnings().isEmpty());
        }
    }

    @Nested
    @DisplayName("應付總額計算")
    class GrandTotalTests {

        @Test
        @DisplayName("總額 = 商品 + 安裝 + 運送 - 會員折扣 + 直送 - 優惠券")
        void shouldCalculateGrandTotalCorrectly() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 1, Money.of(1000),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            // 目前只有商品小計，其他為 0
            assertEquals(1000, result.getGrandTotal().amount());
        }
    }

    @Nested
    @DisplayName("促銷引擎 Fallback")
    class PromotionFallbackTests {

        @Test
        @DisplayName("促銷引擎正常回應時，promotionSkipped 為 false")
        void shouldNotSkipPromotionWhenNormal() {
            // Arrange
            Order order = createOrderWithoutDiscount();
            order.addLine("014014014", "測試商品", 1, Money.of(100),
                    TaxType.TAXABLE, DeliveryMethod.MANAGED, StockMethod.IN_STOCK);

            // Act
            PriceCalculation result = priceCalculationService.calculate(order);

            // Assert
            assertFalse(result.isPromotionSkipped());
            assertEquals(0, result.getCouponDiscount().amount()); // 目前無促銷
        }
    }

    // ===== Helper Methods =====

    private Order createOrderWithoutDiscount() {
        return new Order(
                OrderId.of(3000000001L),
                ProjectId.of("1234524121800001"),
                Customer.ofTempCard("測試會員", "0912345678", "台北市測試路1號"),
                new DeliveryAddress("100", "台北市測試路1號"),
                "S001",
                "SO",
                "testuser"
        );
    }

    private Order createOrderWithDiscount(MemberDiscountType discountType) {
        return new Order(
                OrderId.of(3000000002L),
                ProjectId.of("1234524121800002"),
                Customer.ofMember("M001", "VIP", "測試會員", "0912345678", discountType),
                new DeliveryAddress("100", "台北市測試路1號"),
                "S001",
                "SO",
                "testuser"
        );
    }
}
