package com.tgfc.som.pricing.service;

import com.tgfc.som.catalog.service.ProductEligibilityService;
import com.tgfc.som.fulfillment.service.WorkTypeService;
import com.tgfc.som.member.domain.MemberDiscountType;
import com.tgfc.som.order.domain.Order;
import com.tgfc.som.order.domain.OrderLine;
import com.tgfc.som.order.domain.valueobject.Customer;
import com.tgfc.som.order.domain.valueobject.Money;
import com.tgfc.som.order.domain.valueobject.PriceCalculation;
import com.tgfc.som.common.logging.StructuredLogging;
import com.tgfc.som.pricing.domain.ApportionmentResult;
import com.tgfc.som.pricing.domain.GoodsType;
import com.tgfc.som.pricing.domain.PromotionResult;
import com.tgfc.som.pricing.dto.MemberDiscVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 價格試算服務
 *
 * 實作 12-Step 計價流程 (pricing-calculation-spec.md Section 1.3):
 *
 * Step 1: revertAllSkuAmt - 復原原價 (初始化時不需要)
 * Step 2: apportionmentDiscount - 工種變價分攤
 * Step 3: assortSku - 商品分類 (GoodsType)
 * Step 4: memberDiscountType2 - 成本加成折扣 (優先於促銷)
 * Step 5: promotionCalculation - 促銷計算 (8 種 Event A-H)
 * Step 6: memberDiscountType0 - 折價折扣 (Discounting)
 * Step 7: memberDiscountType1 - 下降折扣 (Down Margin)
 * Step 8: specialMemberDiscount - 特殊會員折扣
 * Step 9-12: generateComputeTypes - 產生 ComputeType 1-6
 *
 * ComputeTypes:
 * 1. 商品小計
 * 2. 安裝小計
 * 3. 運送小計
 * 4. 會員折扣
 * 5. 直送費用
 * 6. 折價券折扣
 * 7. 紅利折抵 (擴充)
 *
 * @see <a href="../../../specs/002-create-order/pricing-calculation-spec.md">Pricing Calculation Spec</a>
 */
@Service
public class PriceCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PriceCalculationService.class);

    private final WorkTypeService workTypeService;
    private final MemberDiscountService memberDiscountService;
    private final PromotionService promotionService;
    private final ApportionmentService apportionmentService;
    private final ProductEligibilityService productEligibilityService;

    public PriceCalculationService(
            WorkTypeService workTypeService,
            MemberDiscountService memberDiscountService,
            PromotionService promotionService,
            ApportionmentService apportionmentService,
            ProductEligibilityService productEligibilityService) {
        this.workTypeService = workTypeService;
        this.memberDiscountService = memberDiscountService;
        this.promotionService = promotionService;
        this.apportionmentService = apportionmentService;
        this.productEligibilityService = productEligibilityService;
    }

    /**
     * 商品分類結果 (Step 3)
     */
    private record AssortedSkus(
        List<OrderLine> productLines,       // P: 一般商品
        List<OrderLine> installLines,       // I/IA/IE/IC/IS: 安裝商品
        List<OrderLine> freeInstallLines,   // FI: 免安商品
        List<OrderLine> deliveryLines,      // DD: 運送商品
        List<OrderLine> directShipmentLines,// VD: 直送商品
        List<OrderLine> workTypeLines       // D: 工種商品
    ) {
        static AssortedSkus empty() {
            return new AssortedSkus(
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
            );
        }
    }

    /**
     * 執行價格試算 (12-Step Flow)
     *
     * @param order 訂單
     * @return 試算結果
     */
    public PriceCalculation calculate(Order order) {
        try (var loggingContext = StructuredLogging.forOrder(order.getId().value(), null)
                .action("PRICE_CALCULATION")) {

            log.info("執行 12-Step 價格試算: orderId={}, lineCount={}",
                    order.getId().value(), order.getLineCount());

            List<String> warnings = new ArrayList<>();
            boolean promotionSkipped = false;
            List<MemberDiscVO> memberDiscounts = new ArrayList<>();

            // ============ Step 1: revertAllSkuAmt ============
            // 初次計算不需要復原，僅在重新計算時需要
            // (已在 OrderLine 初始化時設定)

            // ============ Step 2: apportionmentDiscount ============
            // 工種變價分攤 (安裝/運送費變價時分攤到各商品)
            applyApportionmentDiscount(order, warnings);

            // ============ Step 3: assortSku ============
            // 商品分類 (依 GoodsType 分群)
            AssortedSkus assortedSkus = assortSku(order);
            log.debug("商品分類完成: products={}, installs={}, freeInstalls={}, deliveries={}, directShipments={}, workTypes={}",
                    assortedSkus.productLines().size(),
                    assortedSkus.installLines().size(),
                    assortedSkus.freeInstallLines().size(),
                    assortedSkus.deliveryLines().size(),
                    assortedSkus.directShipmentLines().size(),
                    assortedSkus.workTypeLines().size());

            // ============ Step 4-8: Member Discount Calculation ============
            Customer customer = order.getCustomer();
            MemberDiscountType discountType = customer != null ? customer.discountType() : null;

            if (discountType != null) {
                // Step 4: memberDiscountType2 (Cost Markup) - 優先執行
                if (discountType == MemberDiscountType.COST_MARKUP) {
                    MemberDiscountResult result = calculateType2Discount(order, assortedSkus);
                    memberDiscounts.addAll(result.discounts());
                }

                // ============ Step 5: promotionCalculation ============
                // 促銷計算 (8 種 Event A-H)
                // Type 2 不參與促銷，其他類型可參與
                if (discountType != MemberDiscountType.COST_MARKUP) {
                    List<PromotionResult> promotionResults = calculatePromotions(order, assortedSkus);
                    if (promotionResults.isEmpty()) {
                        promotionSkipped = true;
                    }
                    applyPromotionResults(order, promotionResults, warnings);
                }

                // Step 6: memberDiscountType0 (Discounting)
                if (discountType == MemberDiscountType.DISCOUNTING) {
                    MemberDiscountResult result = calculateType0Discount(order, assortedSkus);
                    memberDiscounts.addAll(result.discounts());
                }

                // Step 7: memberDiscountType1 (Down Margin)
                if (discountType == MemberDiscountType.DOWN_MARGIN) {
                    MemberDiscountResult result = calculateType1Discount(order, assortedSkus);
                    memberDiscounts.addAll(result.discounts());
                }

                // Step 8: specialMemberDiscount (VIP/Employee)
                if (discountType == MemberDiscountType.SPECIAL) {
                    MemberDiscountResult result = calculateSpecialDiscount(order, assortedSkus);
                    memberDiscounts.addAll(result.discounts());
                }
            } else {
                log.debug("無會員折扣類型，跳過會員折扣計算: orderId={}", order.getId().value());
            }

            // ============ Step 9-12: generateComputeTypes ============
            // ComputeType 1: 商品小計
            Money productTotal = calculateProductTotal(order);

            // ComputeType 2: 安裝小計
            Money installationTotal = calculateInstallationTotal(order);

            // ComputeType 3: 運送小計
            Money deliveryTotal = calculateDeliveryTotal(order);

            // 驗證最低工資
            validateMinimumWage(order, warnings);

            // ComputeType 4: 會員折扣
            Money memberDiscount = calculateTotalMemberDiscount(memberDiscounts);

            // ComputeType 5: 直送費用
            Money directShipmentTotal = calculateDirectShipmentTotal(order);

            // ComputeType 6: 折價券折扣 - 從訂單行項加總（由 CouponService 設定）
            Money couponDiscount = calculateCouponDiscount(order);

            // ComputeType 7: 紅利點數折扣 - 從訂單行項加總（由 BonusService 設定）
            Money bonusDiscount = calculateBonusDiscount(order);

            // 計算稅額 (應稅/免稅分離)
            Money taxAmount = calculateTaxAmount(order);

            // 計算應付總額
            Money grandTotal = productTotal
                    .add(installationTotal)
                    .add(deliveryTotal)
                    .add(memberDiscount) // 負數
                    .add(directShipmentTotal)
                    .add(couponDiscount.negate()) // 轉為負數
                    .add(bonusDiscount.negate()); // 轉為負數

            log.info("12-Step 價格試算完成: productTotal={}, installationTotal={}, deliveryTotal={}, " +
                            "memberDiscount={}, couponDiscount={}, bonusDiscount={}, grandTotal={}",
                    productTotal.amount(), installationTotal.amount(), deliveryTotal.amount(),
                    memberDiscount.amount(), couponDiscount.amount(), bonusDiscount.amount(),
                    grandTotal.amount());

            return PriceCalculation.builder()
                    .productTotal(productTotal)
                    .installationTotal(installationTotal)
                    .deliveryTotal(deliveryTotal)
                    .memberDiscount(memberDiscount)
                    .directShipmentTotal(directShipmentTotal)
                    .couponDiscount(couponDiscount)
                    .bonusDiscount(bonusDiscount)
                    .taxAmount(taxAmount)
                    .grandTotal(grandTotal)
                    .memberDiscounts(memberDiscounts)
                    .warnings(warnings)
                    .promotionSkipped(promotionSkipped)
                    .build();
        }
    }

    // ============ Step 2: Apportionment Discount ============

    /**
     * Step 2: 工種變價分攤
     *
     * 當安裝/運送費有變價授權時，將變價差額分攤到該工種下的各商品
     */
    private void applyApportionmentDiscount(Order order, List<String> warnings) {
        // TODO: 取得工種變價資訊 (installAuthEmpId, deliveryAuthEmpId)
        // 目前為 stub 實作
        log.debug("Step 2: apportionmentDiscount - orderId={}", order.getId().value());
    }

    // ============ Step 3: Assort SKU ============

    /**
     * Step 3: 商品分類
     *
     * 依 GoodsType 將商品分群，供後續計算使用
     */
    private AssortedSkus assortSku(Order order) {
        List<OrderLine> productLines = new ArrayList<>();
        List<OrderLine> installLines = new ArrayList<>();
        List<OrderLine> freeInstallLines = new ArrayList<>();
        List<OrderLine> deliveryLines = new ArrayList<>();
        List<OrderLine> directShipmentLines = new ArrayList<>();
        List<OrderLine> workTypeLines = new ArrayList<>();

        for (OrderLine line : order.getLines()) {
            GoodsType goodsType = determineGoodsType(line);

            if (goodsType.isProduct()) {
                productLines.add(line);
            } else if (goodsType.isInstallation()) {
                installLines.add(line);
            } else if (goodsType.isFreeInstallation()) {
                freeInstallLines.add(line);
            } else if (goodsType.isDelivery()) {
                deliveryLines.add(line);
            } else if (goodsType.isDirectShipment()) {
                directShipmentLines.add(line);
            } else if (goodsType.isWorkType()) {
                workTypeLines.add(line);
            }
        }

        return new AssortedSkus(
            productLines, installLines, freeInstallLines,
            deliveryLines, directShipmentLines, workTypeLines
        );
    }

    /**
     * 判斷商品類型
     *
     * TODO: 從商品主檔取得 goodsTypeCode，這裡暫時根據商品特性判斷
     */
    private GoodsType determineGoodsType(OrderLine line) {
        // TODO: 實際應從商品主檔取得 goodsType
        // 暫時根據商品編號或特性判斷
        if (line.isDirectShipment()) {
            return GoodsType.VD;
        }
        // 預設為一般商品
        return GoodsType.P;
    }

    // ============ Step 4: Type 2 Discount ============

    /**
     * Step 4: 成本加成折扣 (Cost Markup)
     *
     * 執行時機: 所有促銷計算之前（因為會完全替換 actPosAmt）
     */
    private MemberDiscountResult calculateType2Discount(Order order, AssortedSkus assortedSkus) {
        log.debug("Step 4: memberDiscountType2 - orderId={}", order.getId().value());

        Customer customer = order.getCustomer();
        BigDecimal markupRate = getMarkupRate(customer);

        // 僅一般商品和安裝商品可參與 Type 2 折扣
        List<OrderLine> eligibleLines = new ArrayList<>();
        eligibleLines.addAll(assortedSkus.productLines());
        eligibleLines.addAll(assortedSkus.installLines());

        // 批次取得商品成本 (來自 ProductEligibilityService)
        List<String> skuNos = eligibleLines.stream()
            .map(OrderLine::getSkuNo)
            .collect(Collectors.toList());
        Map<String, Integer> costs = productEligibilityService.getProductCosts(skuNos);

        List<MemberDiscVO> discounts = new ArrayList<>();
        for (OrderLine line : eligibleLines) {
            // 從商品主檔取得成本價
            int cost = costs.getOrDefault(line.getSkuNo(), 0);
            if (cost == 0) {
                // Fallback: 若無成本資料，使用售價 70% 估算
                cost = (int) (line.getUnitPrice().amount() * 0.7);
                log.warn("商品成本未設定，使用估算值: skuNo={}, estimatedCost={}",
                    line.getSkuNo(), cost);
            }

            int originalPrice = line.getUnitPrice().amount() * line.getQuantity();

            MemberDiscVO discount = memberDiscountService.calculateType2(
                line.getSkuNo(), originalPrice, cost, markupRate
            );

            if (discount != null) {
                discounts.add(discount);
                line.setMemberDisc(Money.of(Math.abs(discount.discAmt())));
            }
        }

        Money totalDiscount = memberDiscountService.calculateTotalMemberDiscount(discounts);
        return new MemberDiscountResult(totalDiscount, discounts);
    }

    // ============ Step 5: Promotion Calculation ============

    /**
     * Step 5: 促銷計算
     *
     * 執行 8 種促銷類型 (A-H)
     * 注意: OMS 為唯一決策者，SOM 僅執行
     */
    private List<PromotionResult> calculatePromotions(Order order, AssortedSkus assortedSkus) {
        log.debug("Step 5: promotionCalculation - orderId={}", order.getId().value());

        // 收集可參與促銷的商品價格與數量
        Map<String, BigDecimal> skuPrices = new HashMap<>();
        Map<String, Integer> skuQuantities = new HashMap<>();

        for (OrderLine line : order.getLines()) {
            GoodsType goodsType = determineGoodsType(line);
            if (goodsType.canHaveMemberDiscount()) {
                skuPrices.put(line.getSkuNo(), BigDecimal.valueOf(line.getUnitPrice().amount()));
                skuQuantities.put(line.getSkuNo(), line.getQuantity());
            }
        }

        // 呼叫 PromotionService 計算促銷
        return promotionService.calculate(skuPrices, skuQuantities, order.getStoreId());
    }

    /**
     * 套用促銷結果到訂單行項
     */
    private void applyPromotionResults(Order order, List<PromotionResult> results, List<String> warnings) {
        for (PromotionResult result : results) {
            if (!result.isSuccessful()) {
                if ("EXPIRED".equals(result.getFailureReason())) {
                    warnings.add("促銷已過期: " + result.getEventNo());
                }
                continue;
            }

            // TODO: 套用促銷折扣到對應的訂單行項
            log.debug("套用促銷: eventNo={}, type={}, discount={}",
                result.getEventNo(), result.getPromotionType(), result.getTotalDiscount());
        }
    }

    // ============ Step 6: Type 0 Discount ============

    /**
     * Step 6: 折價折扣 (Discounting)
     */
    private MemberDiscountResult calculateType0Discount(Order order, AssortedSkus assortedSkus) {
        log.debug("Step 6: memberDiscountType0 - orderId={}", order.getId().value());

        Customer customer = order.getCustomer();
        BigDecimal discRate = getDiscountRate(customer);

        // 僅一般商品和安裝商品可參與 Type 0 折扣
        List<OrderLine> eligibleLines = new ArrayList<>();
        eligibleLines.addAll(assortedSkus.productLines());
        eligibleLines.addAll(assortedSkus.installLines());

        List<MemberDiscVO> discounts = new ArrayList<>();
        for (OrderLine line : eligibleLines) {
            // 檢查是否已參與促銷
            // TODO: 實作促銷排他檢查

            int originalPrice = line.getUnitPrice().amount() * line.getQuantity();
            MemberDiscVO discount = memberDiscountService.calculateType0(
                line.getSkuNo(), originalPrice, discRate
            );

            if (discount != null) {
                discounts.add(discount);
                line.setMemberDisc(Money.of(Math.abs(discount.discAmt())));
            }
        }

        Money totalDiscount = memberDiscountService.calculateTotalMemberDiscount(discounts);
        return new MemberDiscountResult(totalDiscount, discounts);
    }

    // ============ Step 7: Type 1 Discount ============

    /**
     * Step 7: 下降折扣 (Down Margin)
     */
    private MemberDiscountResult calculateType1Discount(Order order, AssortedSkus assortedSkus) {
        log.debug("Step 7: memberDiscountType1 - orderId={}", order.getId().value());

        Customer customer = order.getCustomer();
        BigDecimal discRate = getDiscountRate(customer);

        List<OrderLine> eligibleLines = new ArrayList<>();
        eligibleLines.addAll(assortedSkus.productLines());
        eligibleLines.addAll(assortedSkus.installLines());

        List<MemberDiscVO> discounts = new ArrayList<>();
        for (OrderLine line : eligibleLines) {
            int originalPrice = line.getUnitPrice().amount() * line.getQuantity();
            MemberDiscVO discount = memberDiscountService.calculateType1(
                line.getSkuNo(), originalPrice, discRate
            );

            if (discount != null) {
                discounts.add(discount);
                line.setMemberDisc(Money.of(Math.abs(discount.discAmt())));
            }
        }

        Money totalDiscount = memberDiscountService.calculateTotalMemberDiscount(discounts);
        return new MemberDiscountResult(totalDiscount, discounts);
    }

    // ============ Step 8: Special Discount ============

    /**
     * Step 8: 特殊會員折扣 (VIP/Employee)
     */
    private MemberDiscountResult calculateSpecialDiscount(Order order, AssortedSkus assortedSkus) {
        log.debug("Step 8: specialMemberDiscount - orderId={}", order.getId().value());

        Customer customer = order.getCustomer();
        BigDecimal discRate = getDiscountRate(customer);

        List<OrderLine> eligibleLines = new ArrayList<>();
        eligibleLines.addAll(assortedSkus.productLines());
        eligibleLines.addAll(assortedSkus.installLines());

        List<MemberDiscVO> discounts = new ArrayList<>();
        for (OrderLine line : eligibleLines) {
            int originalPrice = line.getUnitPrice().amount() * line.getQuantity();
            MemberDiscVO discount = memberDiscountService.calculateSpecial(
                line.getSkuNo(), originalPrice, discRate
            );

            if (discount != null) {
                discounts.add(discount);
                line.setMemberDisc(Money.of(Math.abs(discount.discAmt())));
            }
        }

        Money totalDiscount = memberDiscountService.calculateTotalMemberDiscount(discounts);
        return new MemberDiscountResult(totalDiscount, discounts);
    }

    /**
     * 計算總會員折扣
     */
    private Money calculateTotalMemberDiscount(List<MemberDiscVO> discounts) {
        if (discounts == null || discounts.isEmpty()) {
            return Money.ZERO;
        }
        return memberDiscountService.calculateTotalMemberDiscount(discounts);
    }

    /**
     * 會員折扣計算結果
     */
    private record MemberDiscountResult(Money totalDiscount, List<MemberDiscVO> discounts) {
        static MemberDiscountResult empty() {
            return new MemberDiscountResult(Money.ZERO, new ArrayList<>());
        }
    }

    /**
     * 取得會員折扣率
     */
    private BigDecimal getDiscountRate(Customer customer) {
        // TODO: 從會員主檔取得折扣率
        // 這裡使用預設值
        return switch (customer.discountType()) {
            case DISCOUNTING -> new BigDecimal("0.95"); // 預設 95 折
            case DOWN_MARGIN -> new BigDecimal("0.90"); // 預設 90 折
            case SPECIAL -> new BigDecimal("0.85"); // 特殊會員 85 折
            default -> BigDecimal.ONE;
        };
    }

    /**
     * 取得成本加成比例
     */
    private BigDecimal getMarkupRate(Customer customer) {
        // TODO: 從會員主檔取得加成比例
        // 這裡使用預設值
        if (customer.discountType() == MemberDiscountType.COST_MARKUP) {
            return new BigDecimal("1.05"); // 預設成本加成 5%
        }
        return BigDecimal.ONE;
    }

    /**
     * 計算優惠券折扣
     *
     * 從訂單行項加總 couponDisc（由 CouponService 設定）
     */
    private Money calculateCouponDiscount(Order order) {
        Money total = order.getLines().stream()
                .map(OrderLine::getCouponDisc)
                .reduce(Money.ZERO, Money::add);
        log.debug("優惠券折扣計算: orderId={}, couponDiscount={}", order.getId().value(), total.amount());
        return total;
    }

    /**
     * 計算紅利點數折扣
     *
     * 從訂單行項加總 bonusDisc（由 BonusService 設定）
     */
    private Money calculateBonusDiscount(Order order) {
        Money total = order.getLines().stream()
                .map(OrderLine::getBonusDisc)
                .reduce(Money.ZERO, Money::add);
        log.debug("紅利折扣計算: orderId={}, bonusDiscount={}", order.getId().value(), total.amount());
        return total;
    }

    /**
     * 計算商品小計
     */
    private Money calculateProductTotal(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getSubtotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算直送費用
     */
    private Money calculateDirectShipmentTotal(Order order) {
        // 直送商品需要額外費用（暫時為 0）
        long directShipmentCount = order.getLines().stream()
                .filter(OrderLine::isDirectShipment)
                .count();

        if (directShipmentCount > 0) {
            log.debug("訂單包含 {} 項直送商品", directShipmentCount);
        }

        return Money.ZERO;
    }

    /**
     * 計算稅額
     */
    private Money calculateTaxAmount(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getTaxAmount)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算安裝費用小計
     */
    private Money calculateInstallationTotal(Order order) {
        return order.getLines().stream()
                .filter(OrderLine::hasInstallation)
                .map(OrderLine::getInstallationTotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 計算運送費用小計
     */
    private Money calculateDeliveryTotal(Order order) {
        return order.getLines().stream()
                .map(OrderLine::getDeliveryTotal)
                .reduce(Money.ZERO, Money::add);
    }

    /**
     * 驗證最低工資
     *
     * 檢查每個訂單行項的安裝費用是否符合工種的最低工資要求
     * 如果不符合，加入警告訊息
     */
    private void validateMinimumWage(Order order, List<String> warnings) {
        for (OrderLine line : order.getLines()) {
            if (!line.hasInstallation()) {
                continue;
            }

            String workTypeId = line.getWorkTypeId();
            if (workTypeId == null) {
                continue;
            }

            workTypeService.getWorkType(workTypeId).ifPresent(workType -> {
                Money installationTotal = line.getInstallationTotal();
                if (!workType.meetsMinimumWage(installationTotal)) {
                    String warning = String.format(
                            "商品 %s 安裝費用 %s 低於工種 %s 最低工資 %s",
                            line.getSkuNo(),
                            installationTotal.amount(),
                            workType.workTypeName(),
                            workType.minimumWage().amount()
                    );
                    warnings.add(warning);
                    log.warn("最低工資驗證失敗: {}", warning);
                }
            });
        }
    }
}
