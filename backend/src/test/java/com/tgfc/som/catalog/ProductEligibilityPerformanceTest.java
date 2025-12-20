package com.tgfc.som.catalog;

import com.tgfc.som.catalog.dto.EligibilityResponse;
import com.tgfc.som.catalog.service.ProductEligibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T111b: 商品驗證 API 效能測試
 *
 * NFR-002: 商品資格驗證 API 回應時間 ≤ 500ms
 */
@ExtendWith(MockitoExtension.class)
class ProductEligibilityPerformanceTest {

    private ProductEligibilityService productEligibilityService;

    @BeforeEach
    void setUp() {
        // 使用實際的 ProductEligibilityService
        // 在測試環境中，會使用 Mock 資料或 H2 資料庫
        productEligibilityService = new ProductEligibilityService();
    }

    @Test
    @DisplayName("NFR-002: 單次商品驗證應在 500ms 內完成")
    void checkEligibility_shouldCompleteWithin500Ms() {
        // Given
        String skuNo = "014014014";
        String channelId = "SO";
        String storeId = "S001";

        // When
        long startTime = System.nanoTime();
        EligibilityResponse result = productEligibilityService.checkEligibility(skuNo, channelId, storeId);
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then
        System.out.printf("商品驗證耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 500,
                String.format("商品驗證超過 500ms 限制，實際耗時: %d ms", durationMs));
        assertNotNull(result, "驗證結果不應為 null");
    }

    @Test
    @DisplayName("NFR-002: 連續驗證 100 個商品應維持效能")
    void checkEligibilityRepeatedly_shouldMaintainPerformance() {
        // Given
        String channelId = "SO";
        String storeId = "S001";
        int iterations = 100;
        long totalDuration = 0;

        // When
        for (int i = 0; i < iterations; i++) {
            String skuNo = String.format("%09d", i);
            long startTime = System.nanoTime();
            productEligibilityService.checkEligibility(skuNo, channelId, storeId);
            long endTime = System.nanoTime();
            totalDuration += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        }

        long averageDuration = totalDuration / iterations;

        // Then
        System.out.printf("100 次驗證總耗時: %d ms，平均: %d ms%n", totalDuration, averageDuration);
        assertTrue(averageDuration <= 100,
                String.format("平均驗證時間超過 100ms，實際平均: %d ms", averageDuration));
    }

    @Test
    @DisplayName("6-Layer 驗證流程效能")
    void checkEligibilitySixLayerValidation_shouldCompleteQuickly() {
        // Given: 測試各種驗證場景
        String[][] testCases = {
                {"014014014", "SO", "S001"},  // 正常商品
                {"000000000", "SO", "S001"},  // 不存在的商品
                {"999999999", "SO", "S001"},  // 格式錯誤
        };

        // When & Then
        for (String[] testCase : testCases) {
            long startTime = System.nanoTime();
            EligibilityResponse result = productEligibilityService.checkEligibility(
                    testCase[0], testCase[1], testCase[2]);
            long endTime = System.nanoTime();

            long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
            System.out.printf("商品 %s 驗證耗時: %d ms, eligible: %s%n",
                    testCase[0], durationMs, result.eligible());

            assertTrue(durationMs <= 500,
                    String.format("商品 %s 驗證超過 500ms，實際: %d ms", testCase[0], durationMs));
        }
    }

    @Test
    @DisplayName("Mock 資料驗證應在 50ms 內完成")
    void checkEligibilityWithMock_shouldCompleteWithin50Ms() {
        // Given: Mock 商品 (H00199 系列)
        String skuNo = "014014014";

        // When
        long startTime = System.nanoTime();
        EligibilityResponse result = productEligibilityService.checkEligibility(skuNo, "SO", "S001");
        long endTime = System.nanoTime();

        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

        // Then: Mock 資料應該非常快
        System.out.printf("Mock 商品驗證耗時: %d ms%n", durationMs);
        assertTrue(durationMs <= 50,
                String.format("Mock 驗證超過 50ms，實際: %d ms", durationMs));
    }
}
