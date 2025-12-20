package com.tgfc.som.pricing;

import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.DeliveryMethod;
import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.StockMethod;
import com.tgfc.som.order.domain.TaxType;
import com.tgfc.som.order.domain.valueobject.Customer;
import com.tgfc.som.order.domain.valueobject.DeliveryAddress;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.order.domain.valueobject.OrderId;
import com.tgfc.som.order.domain.valueobject.PriceCalculation;
import com.tgfc.som.order.domain.valueobject.ProjectId;
import com.tgfc.som.pricing.service.MemberDiscountService;
import com.tgfc.som.pricing.service.PriceCalculationService;
import com.tgfc.som.fulfillment.service.WorkTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T111a: 價格試算 API 效能測試
 *
 * NFR-001: 價格試算 API 回應時間 ≤ 3 秒（500 筆明細以內）
 */
@ExtendWith(MockitoExtension.class)
class PriceCalculationPerformanceTest {

    @Mock
    private WorkTypeService workTypeService;

    private PriceCalculationService priceCalculationService;
    private MemberDiscountService memberDiscountService;

    @BeforeEach
    void setUp() {
        memberDiscountService = new MemberDiscountService();
        priceCalculationService = new PriceCalculationService(workTypeService, memberDiscountService);
    }

    @Test
    @DisplayName("NFR-001: 500 筆明細價格試算應在 3 秒內完成")
    void calculateWith500Lines_shouldCompleteWithin3Seconds() {
        // Given: 建立包含 500 筆明細的訂單
        Order order = createOrderWithLines(500);

        // When: 執行價格試算並計時
        long startTime = System.nanoTime();
        PriceCalculation result = priceCalculationService.calculate(order);
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then: 執行時間應 ≤ 3000ms
        System.out.printf("500 筆明細試算耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 3000,
                String.format("價格試算超過 3 秒限制，實際耗時: %d ms", durationMs));

        // 驗證計算結果
        assertTrue(result.getGrandTotal().amount() > 0, "應付總額應大於 0");
    }

    @Test
    @DisplayName("NFR-001: 100 筆明細價格試算應在 1 秒內完成")
    void calculateWith100Lines_shouldCompleteWithin1Second() {
        // Given
        Order order = createOrderWithLines(100);

        // When
        long startTime = System.nanoTime();
        PriceCalculation result = priceCalculationService.calculate(order);
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then
        System.out.printf("100 筆明細試算耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 1000,
                String.format("價格試算超過 1 秒限制，實際耗時: %d ms", durationMs));
    }

    @Test
    @DisplayName("NFR-001: 單筆明細價格試算應在 100ms 內完成")
    void calculateWithSingleLine_shouldCompleteWithin100Ms() {
        // Given
        Order order = createOrderWithLines(1);

        // When
        long startTime = System.nanoTime();
        PriceCalculation result = priceCalculationService.calculate(order);
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then
        System.out.printf("單筆明細試算耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 100,
                String.format("價格試算超過 100ms 限制，實際耗時: %d ms", durationMs));
    }

    @Test
    @DisplayName("壓力測試: 連續執行 100 次試算")
    void calculateRepeatedly_shouldMaintainPerformance() {
        // Given
        Order order = createOrderWithLines(50);
        int iterations = 100;
        long totalDuration = 0;

        // When: 連續執行 100 次
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            priceCalculationService.calculate(order);
            long endTime = System.nanoTime();
            totalDuration += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        }

        long averageDuration = totalDuration / iterations;

        // Then: 平均執行時間應 ≤ 500ms
        System.out.printf("100 次試算總耗時: %d ms，平均: %d ms%n", totalDuration, averageDuration);
        assertTrue(averageDuration <= 500,
                String.format("平均試算時間超過 500ms 限制，實際平均: %d ms", averageDuration));
    }

    private Order createOrderWithLines(int lineCount) {
        Customer customer = new Customer(
                "K00123",
                "A",
                "測試會員",
                "M",
                "02-12345678",
                "0912345678",
                null,
                "測試會員",
                "0912345678",
                null,
                MemberDiscountType.DISCOUNTING,
                false
        );

        DeliveryAddress address = new DeliveryAddress("100", "台北市中正區測試路1號");

        // 使用有效的 ID 格式 (OrderId >= 3000000000, ProjectId = 16 碼數字)
        long seq = System.currentTimeMillis() % 100000L;
        Order order = new Order(
                OrderId.of(3000000000L + seq),
                ProjectId.of(String.format("00001241218%05d", seq)),
                customer,
                address,
                "S001",
                "SO",
                "tester"
        );

        // 新增指定數量的行項
        for (int i = 0; i < lineCount; i++) {
            order.addLine(
                    String.format("SKU%09d", i),
                    "測試商品 " + i,
                    1,
                    Money.of(1000 + (i % 100) * 10), // 價格變化
                    TaxType.TAXABLE,
                    DeliveryMethod.MANAGED,
                    StockMethod.IN_STOCK
            );
        }

        return order;
    }
}
