# Research: 新增訂單頁面

**Feature**: 002-create-order
**Date**: 2025-12-20
**Status**: Complete

## Research Tasks

### 1. 12 步驟計價流程實作策略

**Decision**: 參考既有系統 `BzSoServices.java:doCalculate` 方法，重新實作為 `OrderPricingService`

**Rationale**:
- 既有系統已驗證的計價邏輯，無需重新設計
- 12 步驟順序已明確定義，不可變更
- 部分步驟可並行執行（步驟 4&5、步驟 12 的 6 個 ComputeType）

**Alternatives Considered**:
| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| 直接移植既有程式碼 | 快速、行為一致 | 程式碼品質差、難維護 | ❌ 拒絕 |
| 完全重新設計 | 程式碼品質高 | 風險高、可能遺漏邊界情況 | ❌ 拒絕 |
| **參考設計重新實作** | 品質與一致性平衡 | 需對照驗證 | ✅ 採用 |

**Implementation Notes**:
- 建立 `OrderPricingService` 作為協調者
- 每個步驟封裝為獨立方法，便於測試
- 使用 Composition 而非繼承組織步驟邏輯

---

### 2. 會員折扣類型處理策略

**Decision**: 使用 Strategy Pattern 處理 Type 0/1/2/Special 四種折扣類型

**Rationale**:
- 四種折扣類型計算邏輯差異大
- Strategy Pattern 便於新增或修改折扣類型
- 執行順序由 `MemberDiscountService` 協調

**Type 2 特殊處理**:
- Type 2 (Cost Markup) 執行後必須重新分類商品
- 計算結果為負數時歸零，並發送告警信
- 使用 Spring Events 發送告警通知

**Code Reference**:
```java
// C:/projects/som 既有實作
DISC_TYPE_DISCOUNTING = "0"   // 折扣率，不修改 actPosAmt
DISC_TYPE_DOWN_MARGIN = "1"   // 固定折扣，直接修改 actPosAmt
DISC_TYPE_COST_MARKUP = "2"   // 成本加成，完全替換 actPosAmt
```

---

### 3. 商品資格 6 層驗證整合

**Decision**: 建立 `ProductEligibilityService` 統一 6 層驗證邏輯

**Rationale**:
- 既有系統驗證邏輯分散於多個 Service
- 統一入口便於維護與擴展
- 返回結構化結果包含失敗層級與原因

**Verification Layers**:
| 層級 | 驗證內容 | 來源 | 實作方式 |
|------|----------|------|----------|
| 1 | 格式驗證 | Regex | 前端 + 後端 |
| 2 | 存在性驗證 | TBL_SKU_MAST | MyBatis Query |
| 3 | 系統商品排除 | allowSales ≠ 'N' | Entity Field |
| 4 | 稅別驗證 | taxType 有效值 | Enum Validation |
| 5 | 銷售禁止 | allowSales & holdOrder | Business Rule |
| 6 | 類別限制 | 禁售清單 | Config Table |

---

### 4. 運送/備貨方式相容性驗證

**Decision**: 前端即時驗證 + 後端雙重檢查

**Rationale**:
- 前端即時回饋提升使用者體驗
- 後端驗證確保資料一致性
- Toast 通知不阻斷操作流程

**Compatibility Matrix**:
| 運送方式 | 現貨(X) | 訂購(Y) |
|----------|---------|---------|
| 運送(N) | ✅ | ✅ |
| 純運(D) | ✅ | ✅ |
| 直送(V) | ❌ → Y | ✅ |
| 當場自取(C) | ✅ | ❌ → X |
| 宅配(F) | ✅ | ✅ |
| 下次自取(P) | ✅ | ✅ |

**Frontend Implementation**:
```typescript
// Angular Signal-based reactive validation
deliveryMethod = signal<string>('N');
stockMethod = signal<string>('X');

// Effect for auto-correction
effect(() => {
  if (this.deliveryMethod() === 'V' && this.stockMethod() === 'X') {
    this.stockMethod.set('Y');
    this.toastService.show('直送只能訂購', 3000);
  }
  if (this.deliveryMethod() === 'C' && this.stockMethod() === 'Y') {
    this.stockMethod.set('X');
    this.toastService.show('當場自取只能現貨', 3000);
  }
});
```

---

### 5. 冪等性機制設計

**Decision**: 前端 UUID + 後端 ConcurrentHashMap 雙重機制

**Rationale**:
- 前端按鈕禁用防止快速重複點擊
- 後端冪等鍵檢查防止網路重試
- 5 秒過期時間平衡安全與效能

**Implementation**:
```java
// IdempotencyService
@Service
public class IdempotencyService {
    private final ConcurrentHashMap<String, Instant> processedKeys = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 每 10 秒清理過期 key
        scheduler.scheduleAtFixedRate(this::cleanupExpiredKeys, 10, 10, TimeUnit.SECONDS);
    }

    public boolean tryProcess(String idempotencyKey) {
        Instant existing = processedKeys.putIfAbsent(idempotencyKey, Instant.now());
        return existing == null; // true = 可處理, false = 重複
    }

    private void cleanupExpiredKeys() {
        Instant threshold = Instant.now().minusSeconds(5);
        processedKeys.entrySet().removeIf(e -> e.getValue().isBefore(threshold));
    }
}
```

**HTTP Response**:
- 首次請求: `201 Created` + Order Response
- 重複請求: `409 Conflict` + `{"originalOrderId": "xxx"}`

---

### 6. CRM API Mock 策略

**Decision**: Controller 層直接判斷，非 Profile 切換

**Rationale**:
- 簡單直接，符合 KISS 原則
- 測試帳號明確，便於驗證
- 未來可輕易移除 Mock 邏輯

**Mock Data**:
| 卡號 | 類型 | 用途 |
|------|------|------|
| K00123 | 一般會員 | 測試 Type 0 折扣 |
| TEMP001 | 臨時卡 | 測試臨時卡流程 |

---

### 7. 外部服務降級策略

**Decision**: Circuit Breaker Pattern + Graceful Degradation

**Rationale**:
- 促銷引擎/CRM 非核心服務，可降級
- 商品主檔為必要服務，不可降級
- 用戶可見的警告訊息告知折扣可能不完整

**Timeout Configuration**:
| 服務 | 逾時 | 降級行為 |
|------|------|----------|
| 促銷引擎 | 2s | 跳過促銷計算 |
| CRM 會員 | 2s | 使用已載入資料 |
| 商品主檔 | 1s | 顯示錯誤，阻止操作 |

---

### 8. 前端狀態管理策略

**Decision**: Angular Signals + RxJS (HTTP operations)

**Rationale**:
- Signals 提供簡潔的本地狀態管理
- RxJS 適合處理非同步 HTTP 操作
- `toSignal()` 橋接兩者

**State Structure**:
```typescript
// Order form state
interface OrderFormState {
  member: MemberInfo | null;
  lines: OrderLine[];
  calculation: PriceCalculation | null;
  isLoading: boolean;
  errors: ValidationError[];
}

// Using Signals
member = signal<MemberInfo | null>(null);
lines = signal<OrderLine[]>([]);
calculation = signal<PriceCalculation | null>(null);
isLoading = signal(false);

// Computed
totalItems = computed(() => this.lines().length);
grandTotal = computed(() => this.calculation()?.grandTotal ?? 0);
```

---

### 9. Toast 通知元件設計

**Decision**: 自建輕量 Toast Service + Component

**Rationale**:
- 避免引入大型 UI 框架
- 符合 Angular 21+ Standalone 架構
- 簡單需求不需要複雜的通知系統

**Implementation**:
```typescript
// toast.service.ts
@Injectable({ providedIn: 'root' })
export class ToastService {
  private toasts = signal<Toast[]>([]);

  readonly activeToasts = this.toasts.asReadonly();

  show(message: string, duration = 3000): void {
    const id = crypto.randomUUID();
    this.toasts.update(t => [...t, { id, message }]);
    setTimeout(() => this.dismiss(id), duration);
  }

  dismiss(id: string): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
```

---

### 10. Skeleton Loading 實作

**Decision**: CSS-based Skeleton + Angular @if/@for

**Rationale**:
- 純 CSS 動畫效能佳
- 各區塊獨立載入狀態
- 漸進式內容顯示

**Component Structure**:
```html
@if (memberLoading()) {
  <app-skeleton-member />
} @else {
  <app-member-info [member]="member()" />
}

@if (productsLoading()) {
  <app-skeleton-list [rows]="5" />
} @else {
  @for (line of lines(); track line.id) {
    <app-order-line [line]="line" />
  }
}
```

---

## Technology Decisions Summary

| 領域 | 決策 | 原因 |
|------|------|------|
| 計價流程 | 參考既有設計重新實作 | 平衡品質與一致性 |
| 會員折扣 | Strategy Pattern | 四種類型差異大 |
| 商品驗證 | 統一 Service | 整合分散邏輯 |
| 相容性驗證 | 前端即時 + 後端雙重 | UX + 資料一致 |
| 冪等性 | UUID + ConcurrentHashMap | 前後端雙重防護 |
| CRM Mock | Controller 直接判斷 | KISS 原則 |
| 降級策略 | Circuit Breaker | 區分核心/非核心 |
| 狀態管理 | Signals + RxJS | Angular 21+ 最佳實踐 |
| Toast | 自建輕量元件 | 避免大型依賴 |
| Skeleton | CSS-based | 效能優先 |

---

## References

- `C:/projects/som/src/main/java/.../BzSoServices.java:4367` - doCalculate 方法
- `C:/projects/som/src/main/java/.../SoConstant.java` - 常數定義
- `C:/projects/som/src/main/java/.../GoodsType.java` - 安裝服務代碼
- `docs/rewrite-specs/som-order-ddd-spec.md` - DDD 領域模型規格書
- `docs/rewrite-specs/04-Pricing-Calculation-Sequence.md` - 12 步驟計價流程
- `docs/rewrite-specs/05-Pricing-Member-Discount-Logic.md` - 會員折扣邏輯
