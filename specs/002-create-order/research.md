# Research: 新增訂單頁面

**Feature Branch**: `002-create-order`
**Created**: 2025-12-19
**Status**: Complete

---

## 1. 12 步驟計價流程實作策略

### 1.1 Decision

採用 **Domain Service 封裝計價邏輯**，以 `OrderPricingService` 作為協調者，執行 12 步驟計價流程。

### 1.2 Rationale

- 計價流程涉及多個領域概念（商品分類、會員折扣、促銷、安裝費用），適合使用 Domain Service 協調
- 既有系統 `BzSoServices.doCalculate()` 的職責過於龐大，新系統拆分為獨立的步驟服務
- 保持與既有系統計算結果一致（誤差 ±1 元，因四捨五入差異）

### 1.3 Implementation Strategy

```text
12 步驟執行順序（不可變更）:

Step 1: revertAllSkuAmt()        - 還原銷售單價（清除前次計算結果）
Step 2: apportionmentDiscount()  - 工種變價分攤檢查
Step 3: AssortSku()              - 商品分類（一般/安裝/運送/直送）
Step 4: setSerialNO()            - 設定序號 [可與 Step 5 並行]
Step 5: calculateFreeInstall()   - 計算免安總額 [可與 Step 4 並行]
Step 6: memberDiscountType2()    - Cost Markup (Type 2) + 重新分類
Step 7: promotionEngine()        - 多重促銷 (Event A-H)
Step 8: memberDiscountType0()    - Discounting (Type 0)
Step 9: memberDiscountType1()    - Down Margin (Type 1)
Step 10: specialMemberDiscount() - 特殊會員折扣（條件執行）
Step 11: calculateTotalDiscount()- 計算總會員折扣
Step 12: generateComputeTypes()  - 生成 6 種 ComputeType [6 個可並行]
```

### 1.4 Key Implementation Details

**Type 2 (Cost Markup) 特性**:
- 完全替換 `actPosAmt`（成本加成價）
- **必須重新分類商品**（因價格變更影響促銷資格）
- 應稅商品使用無條件捨去 `ROUND_FLOOR`

**Type 0 (Discounting) 特性**:
- **不修改 actPosAmt**，僅記錄於 `memberDisc` 欄位
- 折扣金額單獨顯示於 ComputeType 4（會員卡折扣）

**Type 1 (Down Margin) 特性**:
- **直接修改 actPosAmt**（扣減固定金額）
- 可與促銷疊加（2022-05-13 變更）

### 1.5 Alternatives Considered

| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| 既有邏輯直接複製 | 完全相容 | 難以維護、職責過大 | ❌ 拒絕 |
| 規則引擎（Drools） | 動態規則 | 過度設計、學習成本高 | ❌ 拒絕 |
| **Domain Service 拆分** | 清晰、可測試 | 需仔細對齊既有邏輯 | ✅ 採用 |

---

## 2. 商品資格 6 層驗證實作

### 2.1 Decision

採用 **Chain of Responsibility 模式**，以 `ProductEligibilityService` 執行 6 層驗證。

### 2.2 Rationale

- 驗證邏輯獨立且有順序依賴
- 可在任一層級快速失敗（fail-fast）
- 易於擴展新的驗證規則

### 2.3 Implementation Strategy

```java
public record EligibilityResult(
    boolean eligible,
    String failureReason,
    int failureLevel,  // 1-6, 0 表示通過
    ProductInfo product
) {}

// 6 層驗證順序
public EligibilityResult checkEligibility(String skuNo, String channelId, String storeId) {
    // Level 1: 格式驗證
    if (!isValidSkuFormat(skuNo)) {
        return new EligibilityResult(false, "商品編號格式錯誤", 1, null);
    }

    // Level 2: 存在性驗證
    ProductInfo product = productRepository.findBySku(skuNo);
    if (product == null) {
        return new EligibilityResult(false, "查無此商品", 2, null);
    }

    // Level 3: 系統商品排除
    if ("N".equals(product.getAllowSales())) {
        return new EligibilityResult(false, "系統保留商品不可銷售", 3, null);
    }

    // Level 4: 稅別驗證
    if (!isValidTaxType(product.getTaxType())) {
        return new EligibilityResult(false, "商品稅別設定異常", 4, null);
    }

    // Level 5: 銷售禁止檢查
    if (!product.isAllowSales() || product.isHoldOrder()) {
        return new EligibilityResult(false, "商品已停止銷售", 5, null);
    }

    // Level 6: 類別限制
    if (isRestrictedCategory(product.getCategory(), channelId)) {
        return new EligibilityResult(false, "此類別商品不可於 SO 銷售", 6, null);
    }

    return new EligibilityResult(true, null, 0, product);
}
```

### 2.4 Data Sources

| 驗證層級 | 資料表 | 查詢欄位 |
|---------|--------|---------|
| Level 2 | TBL_SKU_MAST | SKU_NO |
| Level 3 | TBL_SKU_MAST | ALLOW_SALES |
| Level 4 | TBL_SKU_MAST | TAX_TYPE |
| Level 5 | TBL_SKU_STORE | ALLOW_SALES, HOLD_ORDER |
| Level 6 | TBL_SKU_CATEGORY_RESTRICT | CHANNEL_ID, CATEGORY |

---

## 3. CRM API Mock 策略

### 3.1 Decision

採用 **Controller 層條件判斷**，H00199 返回寫死假資料，其他帳號走正常流程。

### 3.2 Rationale

- 簡單直接，符合 KISS 原則
- 不需要切換 Profile 或外部設定
- 測試帳號明確，易於追蹤

### 3.3 Implementation Strategy

```java
@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private static final String MOCK_MEMBER_ID = "H00199";

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable String memberId) {
        if (MOCK_MEMBER_ID.equals(memberId)) {
            return ResponseEntity.ok(MockMemberData.getTestMember());
        }
        // 走正常 CRM API 流程
        return memberService.findByCardNumber(memberId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

**Mock 會員資料**:

```java
public class MockMemberData {
    public static MemberResponse getTestMember() {
        return new MemberResponse(
            "H00199",           // memberId
            "0",                // cardType (一般會員)
            "劉芒果",            // name
            "1990-01-15",       // birthday
            "M",                // gender
            "0912345678",       // cellPhone
            "台北市內湖區瑞光路100號", // address
            "0",                // discType (Type 0: Discounting)
            "一般會員",          // discTypeName
            null,               // discRate
            null                // markupRate
        );
    }
}
```

---

## 4. 防重複提交機制

### 4.1 Decision

採用 **前後端雙重機制**：前端按鈕禁用 + 後端冪等鍵檢查。

### 4.2 Rationale

- 前端按鈕禁用提供即時回饋，改善使用者體驗
- 後端冪等鍵確保資料層安全，防止網路重試造成的重複
- 5 秒視窗足以涵蓋正常處理時間，避免長時間鎖定

### 4.3 Implementation Strategy

**前端實作（Angular）**:

```typescript
@Component({ ... })
export class CreateOrderComponent {
  isSubmitting = signal(false);

  async submitOrder() {
    if (this.isSubmitting()) return;

    this.isSubmitting.set(true);
    try {
      const idempotencyKey = crypto.randomUUID();
      await this.orderService.createOrder(this.orderData(), idempotencyKey);
      // 成功處理
    } catch (error) {
      // 錯誤處理
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
```

**後端實作（Spring）**:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {

        // 檢查冪等鍵（5 秒內重複請求返回 409）
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new OrderResponse(null, "重複提交，請稍後再試", null));
        }

        // 記錄冪等鍵（TTL 5 秒）
        idempotencyService.record(idempotencyKey, 5, TimeUnit.SECONDS);

        // 執行訂單建立
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**冪等鍵服務**:

```java
@Service
public class IdempotencyService {
    private final Map<String, Long> keyStore = new ConcurrentHashMap<>();

    public boolean isDuplicate(String key) {
        Long timestamp = keyStore.get(key);
        if (timestamp == null) return false;
        return System.currentTimeMillis() - timestamp < 5000;
    }

    public void record(String key, long ttl, TimeUnit unit) {
        keyStore.put(key, System.currentTimeMillis());
        // 可選：使用 ScheduledExecutorService 定時清理過期鍵
    }
}
```

---

## 5. 外部服務降級策略

### 5.1 Decision

採用 **Timeout + Fallback 模式**，依服務重要性決定降級行為。

### 5.2 Rationale

- 商品主檔為必要資訊，不可降級
- 促銷與會員折扣為加值服務，逾時可跳過並顯示警告
- 使用者可選擇繼續操作或等待服務恢復

### 5.3 Implementation Strategy

**服務逾時設定**:

| 服務 | 逾時設定 | 降級行為 |
|------|----------|----------|
| 促銷引擎 | 2 秒 | 跳過促銷計算，試算結果不含促銷折扣 |
| CRM 會員服務 | 2 秒 | 使用已載入的會員基本資料，跳過即時折扣資格查詢 |
| 商品主檔 | 1 秒 | 顯示錯誤，此服務不可降級（商品資訊為必要） |

**實作範例**:

```java
@Service
public class PricingServiceWithFallback {

    @Value("${pricing.promotion.timeout:2000}")
    private int promotionTimeout;

    public PriceCalculation calculate(Order order) {
        PriceCalculation result = new PriceCalculation();
        List<String> warnings = new ArrayList<>();

        // Step 1-5: 必要步驟（無降級）
        executeRequiredSteps(order, result);

        // Step 7: 促銷計算（可降級）
        try {
            CompletableFuture<PromotionResult> future = CompletableFuture
                .supplyAsync(() -> promotionService.calculate(order));

            PromotionResult promotion = future.get(promotionTimeout, TimeUnit.MILLISECONDS);
            applyPromotion(result, promotion);
        } catch (TimeoutException e) {
            warnings.add("部分折扣資訊暫時無法取得");
            result.setPromotionSkipped(true);
        }

        result.setWarnings(warnings);
        return result;
    }
}
```

**前端警告顯示**:

```html
@if (calculation().warnings.length > 0) {
  <div class="warning-banner">
    @for (warning of calculation().warnings; track warning) {
      <p>⚠️ {{ warning }}</p>
    }
    <p class="note">折扣可能不完整，您仍可繼續提交訂單</p>
  </div>
}
```

---

## 6. 會員折扣 actPosAmt 修改規則

### 6.1 Decision

嚴格遵循既有系統的 **actPosAmt 修改規則**，確保計算結果一致。

### 6.2 Summary Table

| 折扣類型 | 修改 actPosAmt | 修改方式 | 標記 posAmtChangePrice | 納入 totalMemberDisc |
|---------|---------------|---------|----------------------|---------------------|
| Type 2 (Cost Markup) | ✅ 是 | **完全替換** | true | ❌ 否 |
| Type 0 (Discounting) | ❌ **否** | 不修改 | false | ✅ **是** |
| Type 1 (Down Margin) | ✅ 是 | 扣減 | true | ❌ 否 |

### 6.3 Implementation Details

**Type 0 不納入 totalMemberDisc 的原因**:
- Type 0 不修改 `actPosAmt`，折扣金額記錄於 `memberDisc` 欄位
- 此折扣需單獨顯示於 ComputeType 4（會員卡折扣）
- 若同時納入 totalMemberDisc，會造成折扣重複計算

**Type 1/2 已反映在價格中**:
- Type 1/2 直接修改 `actPosAmt`，折扣已反映在商品小計
- 因此不需要額外計入 totalMemberDisc

---

## 7. 訂單編號與專案代號生成規則

### 7.1 Decision

採用 **資料庫序列 + 格式轉換** 生成訂單編號與專案代號。

### 7.2 Implementation Strategy

**訂單編號 (OrderId)**:
- 格式：10 位數字流水號
- 起始：3000000000
- 生成：資料庫序列 `SEQ_ORDER_ID`

```sql
CREATE SEQUENCE SEQ_ORDER_ID
  START WITH 3000000000
  INCREMENT BY 1
  NOCACHE;
```

**專案代號 (ProjectId)**:
- 格式：店別(5碼) + 年(2碼) + 月日(4碼) + 流水號(5碼)
- 範例：12345 + 24 + 1218 + 00001 = 1234524121800001

```java
public String generateProjectId(String storeId) {
    String year = String.valueOf(LocalDate.now().getYear()).substring(2);
    String monthDay = String.format("%02d%02d",
        LocalDate.now().getMonthValue(),
        LocalDate.now().getDayOfMonth());

    int sequence = projectIdSequenceService.getNextSequence(storeId, LocalDate.now());
    String sequenceStr = String.format("%05d", sequence);

    return storeId + year + monthDay + sequenceStr;
}
```

---

## 8. 商品數量上限處理

### 8.1 Decision

採用 **前後端雙重驗證**，達 50 項後禁止新增。

### 8.2 Implementation Strategy

**前端驗證（Angular）**:

```typescript
@Component({ ... })
export class ProductListComponent {
  private readonly MAX_ITEMS = 50;

  canAddProduct = computed(() =>
    this.orderLines().length < this.MAX_ITEMS
  );

  addProduct(product: ProductInfo) {
    if (!this.canAddProduct()) {
      this.showError('訂單已達商品上限（50項），請分單處理');
      return;
    }
    // 新增商品邏輯
  }
}
```

**後端驗證（Spring）**:

```java
@Service
public class OrderValidationService {
    private static final int MAX_ORDER_LINES = 50;

    public void validateForAddLine(Order order) {
        if (order.getLines().size() >= MAX_ORDER_LINES) {
            throw new BusinessException("ORDER_LIMIT_EXCEEDED",
                "訂單已達商品上限（50項），請分單處理");
        }
    }
}
```

---

## 9. UI Loading States

### 9.1 Decision

採用 **區塊 Skeleton 模式**，各區塊獨立顯示骨架屏或 spinner。

### 9.2 Implementation Strategy

**Angular 元件**:

```typescript
@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  template: `
    <div class="skeleton-container">
      @switch (type()) {
        @case ('text') {
          <div class="skeleton-line" [style.width]="width()"></div>
        }
        @case ('card') {
          <div class="skeleton-card"></div>
        }
        @case ('table') {
          @for (i of [1,2,3,4,5]; track i) {
            <div class="skeleton-row"></div>
          }
        }
      }
    </div>
  `
})
export class SkeletonLoaderComponent {
  type = input<'text' | 'card' | 'table'>('text');
  width = input('100%');
}
```

**使用方式**:

```html
@if (isLoadingMember()) {
  <app-skeleton-loader type="card" />
} @else {
  <app-member-info [member]="member()" />
}

@if (isLoadingProducts()) {
  <app-skeleton-loader type="table" />
} @else {
  <app-product-list [lines]="orderLines()" />
}
```

---

## 10. Type 2 負數折扣處理

### 10.1 Decision

Type 2 計算結果為負數時，**歸零並發送告警信**。

### 10.2 Implementation Strategy

```java
@Service
public class MemberDiscountService {

    @Value("${alert.email.recipients}")
    private String alertRecipients;

    public MemberDiscVO calculateType2Discount(OrderLine line, Member member) {
        BigDecimal cost = line.getProduct().getCost();
        BigDecimal markupRate = member.getMarkupRate();
        BigDecimal markupPrice = cost.multiply(BigDecimal.ONE.add(markupRate));

        BigDecimal discount = line.getUnitPrice().subtract(markupPrice);

        // 負數折扣歸零並發送告警
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            sendAlertEmail(line, member, discount);
            discount = BigDecimal.ZERO;
        }

        return new MemberDiscVO(
            line.getSkuNo(),
            "2",
            "Cost Markup",
            line.getUnitPrice(),
            markupPrice,
            discount,
            null,
            markupRate
        );
    }

    private void sendAlertEmail(OrderLine line, Member member, BigDecimal discount) {
        String subject = "[告警] Type 2 計算結果為負數";
        String body = String.format(
            "會員: %s, 商品: %s, 成本加成價高於售價, 折扣金額: %s",
            member.getMemberId(),
            line.getSkuNo(),
            discount
        );
        emailService.send(alertRecipients, subject, body);
    }
}
```

---

## Summary

本研究文件涵蓋了 002-create-order 功能的關鍵技術決策：

1. **12 步驟計價流程**：採用 Domain Service 封裝，嚴格遵循執行順序
2. **商品資格驗證**：Chain of Responsibility 模式，6 層快速失敗驗證
3. **CRM Mock 策略**：Controller 層條件判斷，H00199 返回假資料
4. **防重複提交**：前端按鈕禁用 + 後端冪等鍵（5 秒視窗）
5. **外部服務降級**：Timeout + Fallback，依服務重要性決定行為
6. **actPosAmt 修改規則**：Type 0 不修改、Type 1/2 修改
7. **訂單編號生成**：資料庫序列 + 格式轉換
8. **商品數量上限**：50 項，前後端雙重驗證
9. **UI Loading**：區塊 Skeleton 模式
10. **Type 2 負數處理**：歸零並發送告警信

所有研究項目已完成，可進入 Phase 1 設計階段。
