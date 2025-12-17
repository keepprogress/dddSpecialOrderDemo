# 36. Backend - Pricing Service Implementation

## Pricing Engine

```java
// PricingEngine.java
@Service
@Slf4j
public class PricingEngine {

    @Autowired
    private MemberDiscountCalculator memberDiscountCalculator;

    @Autowired
    private PricingCacheService cacheService;

    /**
     * 12 步驟計價主流程
     */
    public PricingResponse calculate(PricingRequest request) {
        long startTime = System.currentTimeMillis();

        // 檢查快取
        PricingResponse cached = cacheService.get(request);
        if (cached != null) {
            cached.setCacheHit(true);
            return cached;
        }

        // 1-3. 準備與還原
        List<PricingItem> items = preparePricingItems(request.getSkus());
        revertAndApportion(items);

        // 4-5. 並行計算運費與安裝費
        CompletableFuture<BigDecimal> deliveryFuture =
            CompletableFuture.supplyAsync(() -> calculateDeliveryFee(items));
        CompletableFuture<BigDecimal> installFuture =
            CompletableFuture.supplyAsync(() -> calculateInstallationFee(items));

        // 6-8. 會員折扣
        MemberDiscountResult memberDiscount =
            memberDiscountCalculator.calculate(
                request.getMemberCardId(), items, request.getChannelId()
            );

        // 等待並行任務
        BigDecimal deliveryFee = deliveryFuture.join();
        BigDecimal installationFee = installFuture.join();

        // 12. 計算 6 種 ComputeType
        List<ComputeTypeResult> computes =
            calculateComputeTypes(items, memberDiscount, deliveryFee, installationFee);

        // 組裝結果
        PricingResponse response = buildResponse(request, items, computes);
        response.setCalculationTime(System.currentTimeMillis() - startTime);
        response.setCacheHit(false);

        // 快取結果
        cacheService.put(request, response);

        return response;
    }

    private List<ComputeTypeResult> calculateComputeTypes(
        List<PricingItem> items,
        MemberDiscountResult memberDiscount,
        BigDecimal deliveryFee,
        BigDecimal installationFee
    ) {
        // 並行計算 6 種類型
        return CompletableFuture.allOf(
            CompletableFuture.supplyAsync(() -> computeType1(items)),
            CompletableFuture.supplyAsync(() -> computeType2(items)),
            CompletableFuture.supplyAsync(() -> computeType3(deliveryFee)),
            CompletableFuture.supplyAsync(() -> computeType4(memberDiscount)),
            CompletableFuture.supplyAsync(() -> computeType5(items)),
            CompletableFuture.supplyAsync(() -> computeType6(items))
        ).thenApply(v -> List.of(
            computeType1(items),
            computeType2(items),
            computeType3(deliveryFee),
            computeType4(memberDiscount),
            computeType5(items),
            computeType6(items)
        )).join();
    }
}
```

## Member Discount Calculator

```java
// MemberDiscountCalculator.java
@Service
public class MemberDiscountCalculator {

    @Autowired
    private MemberServiceClient memberServiceClient;

    public MemberDiscountResult calculate(
        String memberCardId,
        List<PricingItem> items,
        String channelId
    ) {
        MemberDiscount discount = getMemberDiscount(memberCardId);

        if (discount == null) {
            return MemberDiscountResult.none();
        }

        return switch (discount.getDiscountType()) {
            case "2" -> calculateCostMarkup(items, discount);
            case "0" -> calculateDiscountRate(items, discount);
            case "1" -> calculateFixedDiscount(items, discount);
            default -> MemberDiscountResult.none();
        };
    }

    /**
     * Type 2: 成本加成
     * 公式: 折扣價 = 成本 × 加成比例
     */
    private MemberDiscountResult calculateCostMarkup(
        List<PricingItem> items,
        MemberDiscount discount
    ) {
        BigDecimal discountValue = discount.getDiscountValue();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (PricingItem item : items) {
            BigDecimal discountPrice = item.getCostPrice().multiply(discountValue);
            BigDecimal discountAmount = item.getSellingPrice()
                .subtract(discountPrice)
                .multiply(BigDecimal.valueOf(item.getQuantity()));

            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                item.setDiscountAmount(discountAmount);
                totalDiscount = totalDiscount.add(discountAmount);
            }
        }

        return MemberDiscountResult.builder()
            .discountType("2")
            .totalDiscount(totalDiscount)
            .build();
    }
}
```

## Pricing Controller

```java
// PricingController.java
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    @Autowired
    private PricingEngine pricingEngine;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<PricingResponse>> calculatePrice(
        @RequestBody @Valid PricingRequest request
    ) {
        PricingResponse response = pricingEngine.calculate(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

**參考文件**:
- `04-Pricing-Calculation-Sequence.md`
- `13-API-Pricing-Service.md`

**文件版本**: v1.0
