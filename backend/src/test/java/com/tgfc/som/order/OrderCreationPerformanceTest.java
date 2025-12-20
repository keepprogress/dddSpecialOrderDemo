package com.tgfc.som.order;

import com.tgfc.som.catalog.service.ProductEligibilityService;
import com.tgfc.som.catalog.service.ProductServiceAssociationService;
import com.tgfc.som.common.service.IdempotencyService;
import com.tgfc.som.fulfillment.service.WorkTypeService;
import com.tgfc.som.order.dto.CreateOrderRequest;
import com.tgfc.som.order.dto.OrderResponse;
import com.tgfc.som.order.service.OrderService;
import com.tgfc.som.pricing.service.BonusService;
import com.tgfc.som.pricing.service.CouponService;
import com.tgfc.som.pricing.service.PriceCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T111c: 訂單建立 API 效能測試
 *
 * NFR-003: 訂單建立 API 回應時間 ≤ 2 秒
 */
@ExtendWith(MockitoExtension.class)
class OrderCreationPerformanceTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ProductEligibilityService productEligibilityService;

    @Mock
    private PriceCalculationService priceCalculationService;

    @Mock
    private ProductServiceAssociationService productServiceAssociationService;

    @Mock
    private WorkTypeService workTypeService;

    @Mock
    private CouponService couponService;

    @Mock
    private BonusService bonusService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                idempotencyService,
                productEligibilityService,
                priceCalculationService,
                productServiceAssociationService,
                workTypeService,
                couponService,
                bonusService
        );
    }

    @Test
    @DisplayName("NFR-003: 訂單建立應在 2 秒內完成")
    void createOrder_shouldCompleteWithin2Seconds() {
        // Given
        CreateOrderRequest request = createOrderRequest();
        String idempotencyKey = UUID.randomUUID().toString();
        String userId = "tester";

        // When
        long startTime = System.nanoTime();
        OrderResponse result = orderService.createOrder(request, idempotencyKey, userId);
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then
        System.out.printf("訂單建立耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 2000,
                String.format("訂單建立超過 2 秒限制，實際耗時: %d ms", durationMs));
        assertNotNull(result.orderId(), "訂單編號不應為 null");
    }

    @Test
    @DisplayName("NFR-003: 連續建立 50 筆訂單應維持效能")
    void createOrderRepeatedly_shouldMaintainPerformance() {
        // Given
        int iterations = 50;
        long totalDuration = 0;

        // When
        for (int i = 0; i < iterations; i++) {
            CreateOrderRequest request = createOrderRequest();
            String idempotencyKey = UUID.randomUUID().toString();

            long startTime = System.nanoTime();
            orderService.createOrder(request, idempotencyKey, "tester");
            long endTime = System.nanoTime();

            totalDuration += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        }

        long averageDuration = totalDuration / iterations;

        // Then
        System.out.printf("50 次建立總耗時: %d ms，平均: %d ms%n", totalDuration, averageDuration);
        assertTrue(averageDuration <= 500,
                String.format("平均建立時間超過 500ms，實際平均: %d ms", averageDuration));
    }

    @Test
    @DisplayName("包含臨時卡的訂單建立效能")
    void createOrderWithTempCard_shouldCompleteQuickly() {
        // Given
        CreateOrderRequest request = createTempCardOrderRequest();
        String idempotencyKey = UUID.randomUUID().toString();

        // When
        long startTime = System.nanoTime();
        OrderResponse result = orderService.createOrder(request, idempotencyKey, "tester");
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then
        System.out.printf("臨時卡訂單建立耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 2000,
                String.format("臨時卡訂單建立超過 2 秒，實際: %d ms", durationMs));
    }

    @Test
    @DisplayName("冪等性檢查應在 100ms 內完成")
    void idempotencyCheck_shouldCompleteQuickly() {
        // Given
        CreateOrderRequest request = createOrderRequest();
        String idempotencyKey = UUID.randomUUID().toString();

        // First creation
        orderService.createOrder(request, idempotencyKey, "tester");

        // When: 重複建立（應該快速返回）
        long startTime = System.nanoTime();
        try {
            orderService.createOrder(request, idempotencyKey, "tester");
        } catch (Exception e) {
            // 冪等性檢查可能拋出異常
        }
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then: 冪等性檢查應該非常快
        System.out.printf("冪等性檢查耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 100,
                String.format("冪等性檢查超過 100ms，實際: %d ms", durationMs));
    }

    private CreateOrderRequest createOrderRequest() {
        return new CreateOrderRequest(
                "H00199",
                new CreateOrderRequest.CustomerInfo(
                        "H00199",
                        "A",
                        "測試會員",
                        "M",
                        "02-12345678",
                        "0912345678",
                        null,
                        "測試會員",
                        "0912345678",
                        null,
                        "0",
                        false
                ),
                new CreateOrderRequest.AddressInfo(
                        "100",
                        "台北市中正區測試路1號"
                ),
                "S001",
                "SO",
                null // lines
        );
    }

    private CreateOrderRequest createTempCardOrderRequest() {
        return new CreateOrderRequest(
                "TEMP001",
                new CreateOrderRequest.CustomerInfo(
                        "TEMP001",
                        "T",
                        "臨時客戶",
                        "F",
                        null,
                        "0987654321",
                        null,
                        "臨時客戶",
                        "0987654321",
                        null,
                        null,
                        true
                ),
                new CreateOrderRequest.AddressInfo(
                        "110",
                        "台北市信義區測試路2號"
                ),
                "S001",
                "SO",
                null // lines
        );
    }
}
