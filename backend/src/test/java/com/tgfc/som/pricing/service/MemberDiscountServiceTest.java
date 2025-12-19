package com.tgfc.som.pricing.service;

import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.pricing.dto.MemberDiscVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemberDiscountService 單元測試
 *
 * 測試會員折扣計算：Type 0/1/2/SPECIAL
 */
class MemberDiscountServiceTest {

    private MemberDiscountService memberDiscountService;

    @BeforeEach
    void setUp() {
        memberDiscountService = new MemberDiscountService();
    }

    @Nested
    @DisplayName("Type 0: Discounting (折價)")
    class Type0DiscountingTests {

        @Test
        @DisplayName("95 折計算: 原價 1000 → 折扣價 950，折扣金額 -50")
        void shouldCalculateType0With95Percent() {
            // Arrange
            String skuNo = "014014014";
            int originalPrice = 1000;
            BigDecimal discRate = new BigDecimal("0.95");

            // Act
            MemberDiscVO result = memberDiscountService.calculateType0(skuNo, originalPrice, discRate);

            // Assert
            assertNotNull(result);
            assertEquals(skuNo, result.skuNo());
            assertEquals("0", result.discType());
            assertEquals("折價 (Discounting)", result.discTypeName());
            assertEquals(1000, result.originalPrice());
            assertEquals(950, result.discountPrice());
            assertEquals(-50, result.discAmt());
            assertTrue(result.isValidDiscount());
        }

        @Test
        @DisplayName("85 折計算: 原價 2000 → 折扣價 1700，折扣金額 -300")
        void shouldCalculateType0With85Percent() {
            // Arrange
            String skuNo = "015015015";
            int originalPrice = 2000;
            BigDecimal discRate = new BigDecimal("0.85");

            // Act
            MemberDiscVO result = memberDiscountService.calculateType0(skuNo, originalPrice, discRate);

            // Assert
            assertNotNull(result);
            assertEquals(1700, result.discountPrice());
            assertEquals(-300, result.discAmt());
            assertTrue(result.isValidDiscount());
        }

        @Test
        @DisplayName("無效折扣率應回傳 null")
        void shouldReturnNullForInvalidDiscRate() {
            // Act
            MemberDiscVO result1 = memberDiscountService.calculateType0("014014014", 1000, null);
            MemberDiscVO result2 = memberDiscountService.calculateType0("014014014", 1000, BigDecimal.ZERO);
            MemberDiscVO result3 = memberDiscountService.calculateType0("014014014", 1000, new BigDecimal("-0.5"));

            // Assert
            assertNull(result1);
            assertNull(result2);
            assertNull(result3);
        }

        @Test
        @DisplayName("四捨五入計算: 原價 333 × 0.95 = 316.35 → 316")
        void shouldRoundHalfUpForDecimalResult() {
            // Arrange
            String skuNo = "016016016";
            int originalPrice = 333;
            BigDecimal discRate = new BigDecimal("0.95");

            // Act
            MemberDiscVO result = memberDiscountService.calculateType0(skuNo, originalPrice, discRate);

            // Assert
            assertNotNull(result);
            assertEquals(316, result.discountPrice()); // 316.35 四捨五入
            assertEquals(-17, result.discAmt());
        }
    }

    @Nested
    @DisplayName("Type 1: Down Margin (下降)")
    class Type1DownMarginTests {

        @Test
        @DisplayName("直接設定折扣價: 原價 1000 → 折扣價 900，折扣金額 -100")
        void shouldCalculateType1WithDownMargin() {
            // Arrange
            String skuNo = "014014014";
            int originalPrice = 1000;
            BigDecimal discRate = new BigDecimal("0.90"); // 相當於設定折扣價為原價的 90%

            // Act
            MemberDiscVO result = memberDiscountService.calculateType1(skuNo, originalPrice, discRate);

            // Assert
            assertNotNull(result);
            assertEquals(skuNo, result.skuNo());
            assertEquals("1", result.discType());
            assertEquals("下降 (Down Margin)", result.discTypeName());
            assertEquals(1000, result.originalPrice());
            assertEquals(900, result.discountPrice());
            assertEquals(-100, result.discAmt());
            assertTrue(result.isValidDiscount());
        }

        @Test
        @DisplayName("無效下降比例應回傳 null")
        void shouldReturnNullForInvalidDiscRate() {
            // Act
            MemberDiscVO result = memberDiscountService.calculateType1("014014014", 1000, null);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Type 2: Cost Markup (成本加成)")
    class Type2CostMarkupTests {

        @Test
        @DisplayName("正常成本加成: 成本 700 × 1.05 = 735，原價 1000，折扣金額 -265")
        void shouldCalculateType2WithNormalMarkup() {
            // Arrange
            String skuNo = "014014014";
            int originalPrice = 1000;
            int cost = 700;
            BigDecimal markupRate = new BigDecimal("1.05");

            // Act
            MemberDiscVO result = memberDiscountService.calculateType2(skuNo, originalPrice, cost, markupRate);

            // Assert
            assertNotNull(result);
            assertEquals(skuNo, result.skuNo());
            assertEquals("2", result.discType());
            assertEquals("成本加成 (Cost Markup)", result.discTypeName());
            assertEquals(1000, result.originalPrice());
            assertEquals(735, result.discountPrice()); // 700 × 1.05
            assertEquals(-265, result.discAmt());
            assertTrue(result.isValidDiscount());
        }

        @Test
        @DisplayName("異常情況 - 成本加成後超過原價: 成本 950 × 1.10 = 1045 > 原價 1000")
        void shouldHandleNegativeResultWhenMarkupExceedsOriginal() {
            // Arrange
            String skuNo = "015015015";
            int originalPrice = 1000;
            int cost = 950;
            BigDecimal markupRate = new BigDecimal("1.10");

            // Act
            MemberDiscVO result = memberDiscountService.calculateType2(skuNo, originalPrice, cost, markupRate);

            // Assert
            assertNotNull(result);
            assertEquals(1000, result.originalPrice());
            assertEquals(1045, result.discountPrice()); // 950 × 1.10
            assertEquals(45, result.discAmt()); // 正數（異常情況）
            assertFalse(result.isValidDiscount()); // 應回傳 false
        }

        @Test
        @DisplayName("無效加成比例應回傳 null")
        void shouldReturnNullForInvalidMarkupRate() {
            // Act
            MemberDiscVO result1 = memberDiscountService.calculateType2("014014014", 1000, 700, null);
            MemberDiscVO result2 = memberDiscountService.calculateType2("014014014", 1000, 700, BigDecimal.ZERO);
            MemberDiscVO result3 = memberDiscountService.calculateType2("014014014", 1000, 700, new BigDecimal("-0.5"));

            // Assert
            assertNull(result1);
            assertNull(result2);
            assertNull(result3);
        }
    }

    @Nested
    @DisplayName("SPECIAL: 特殊會員")
    class SpecialMemberTests {

        @Test
        @DisplayName("特殊會員折扣: 原價 1000 × 0.85 → 折扣價 850，折扣金額 -150")
        void shouldCalculateSpecialMemberDiscount() {
            // Arrange
            String skuNo = "014014014";
            int originalPrice = 1000;
            BigDecimal discRate = new BigDecimal("0.85");

            // Act
            MemberDiscVO result = memberDiscountService.calculateSpecial(skuNo, originalPrice, discRate);

            // Assert
            assertNotNull(result);
            assertEquals(skuNo, result.skuNo());
            assertEquals("SPECIAL", result.discType());
            assertEquals("特殊會員", result.discTypeName());
            assertEquals(1000, result.originalPrice());
            assertEquals(850, result.discountPrice());
            assertEquals(-150, result.discAmt());
            assertTrue(result.isValidDiscount());
        }

        @Test
        @DisplayName("無效折扣率應回傳 null")
        void shouldReturnNullForInvalidDiscRate() {
            // Act
            MemberDiscVO result = memberDiscountService.calculateSpecial("014014014", 1000, null);

            // Assert
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("計算總會員折扣")
    class TotalDiscountTests {

        @Test
        @DisplayName("計算多個折扣的總和")
        void shouldCalculateTotalMemberDiscount() {
            // Arrange
            MemberDiscVO disc1 = MemberDiscVO.ofType0("014014014", 1000, new BigDecimal("0.95"));
            MemberDiscVO disc2 = MemberDiscVO.ofType0("015015015", 2000, new BigDecimal("0.90"));
            List<MemberDiscVO> discounts = List.of(disc1, disc2);

            // Act
            Money total = memberDiscountService.calculateTotalMemberDiscount(discounts);

            // Assert
            // disc1: -50, disc2: -200, total: -250
            assertEquals(-250, total.amount());
        }

        @Test
        @DisplayName("空列表回傳零")
        void shouldReturnZeroForEmptyList() {
            // Act
            Money total = memberDiscountService.calculateTotalMemberDiscount(List.of());

            // Assert
            assertEquals(0, total.amount());
        }
    }
}
