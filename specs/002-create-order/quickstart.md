# Quickstart: 新增訂單頁面開發指南

**Feature Branch**: `002-create-order`
**Created**: 2025-12-19

---

## 1. 環境需求

### 1.1 系統需求

| 項目 | 版本 | 說明 |
|------|------|------|
| Java | 21+ | 使用 Records、Pattern Matching |
| Node.js | 20+ | Angular CLI 需求 |
| npm | 10+ | 套件管理 |
| Maven | 3.9+ | 後端建置 |
| Docker | 24+ | Keycloak 容器 |

### 1.2 開發工具

| 工具 | 建議版本 | 用途 |
|------|----------|------|
| IntelliJ IDEA | 2024.1+ | Java 開發 |
| VS Code | 1.90+ | 前端開發 |
| Postman/Insomnia | 最新版 | API 測試 |
| DBeaver | 最新版 | 資料庫管理 |

---

## 2. 快速開始

### 2.1 取得程式碼

```bash
# 切換到功能分支
git checkout 002-create-order

# 安裝相依套件
cd backend && mvn install -DskipTests
cd ../frontend && npm install
```

### 2.2 啟動 Keycloak

```bash
# 使用 Docker Compose 啟動
docker-compose up -d keycloak

# 等待啟動完成（約 30 秒）
# 訪問 http://localhost:8180/admin
# 帳號: admin / 密碼: admin
```

### 2.3 啟動後端

```bash
cd backend

# 開發環境（使用 H2 資料庫）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 驗證啟動成功
curl http://localhost:8080/api/health
```

### 2.4 啟動前端

```bash
cd frontend

# 開發模式
npm start

# 訪問 http://localhost:4200
```

---

## 3. 開發指南

### 3.1 後端開發

#### 3.1.1 新增 Controller

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "訂單管理 API")
public class OrderController {

    private final OrderService orderService = inject(OrderService.class);

    @Operation(summary = "建立新訂單")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "建立成功"),
        @ApiResponse(responseCode = "400", description = "請求驗證失敗"),
        @ApiResponse(responseCode = "409", description = "重複提交")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey) {

        OrderResponse response = orderService.createOrder(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### 3.1.2 新增 Service

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPricingService pricingService;
    private final IdempotencyService idempotencyService;

    // 使用手動建構子注入（禁用 Lombok）
    public OrderService(
            OrderRepository orderRepository,
            OrderPricingService pricingService,
            IdempotencyService idempotencyService) {
        this.orderRepository = orderRepository;
        this.pricingService = pricingService;
        this.idempotencyService = idempotencyService;
    }

    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        // 1. 冪等鍵檢查
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            throw new DuplicateSubmissionException("重複提交，請稍後再試");
        }
        idempotencyService.record(idempotencyKey);

        // 2. 建立訂單
        Order order = Order.create(
            request.storeId(),
            request.channelId(),
            request.customer(),
            request.deliveryAddress()
        );

        // 3. 儲存並回傳
        orderRepository.save(order);
        return OrderResponse.from(order);
    }
}
```

#### 3.1.3 使用 Java Records 作為 DTO

```java
// Request DTO
public record CreateOrderRequest(
    @NotBlank String storeId,
    @NotBlank String channelId,
    @Valid CustomerInfo customer,
    @Valid DeliveryAddress deliveryAddress,
    @Size(max = 50) List<OrderLineInput> lines
) {}

// Response DTO
public record OrderResponse(
    String orderId,
    String projectId,
    String status,
    Integer grandTotal,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId().value(),
            order.getProjectId().value(),
            order.getStatus().getCode(),
            order.getCalculation().grandTotal().amount(),
            order.getCreatedAt()
        );
    }
}
```

### 3.2 前端開發

#### 3.2.1 新增 Component（Standalone + Signals）

```typescript
import { Component, signal, computed, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>新增訂單</h1>

    <!-- 會員資訊區塊 -->
    @if (isLoadingMember()) {
      <app-skeleton-loader type="card" />
    } @else {
      <app-member-info [member]="member()" />
    }

    <!-- 商品清單區塊 -->
    @if (orderLines().length > 0) {
      @for (line of orderLines(); track line.lineId) {
        <app-order-line [line]="line" (remove)="removeLine(line.lineId)" />
      }
    } @empty {
      <p>請新增商品</p>
    }

    <!-- 新增商品 -->
    @if (canAddProduct()) {
      <app-add-product (add)="addProduct($event)" />
    } @else {
      <p class="warning">訂單已達商品上限</p>
    }

    <!-- 試算結果 -->
    @if (calculation()) {
      <app-price-calculation [calculation]="calculation()" />
    }

    <!-- 提交按鈕 -->
    <button
      [disabled]="isSubmitting() || !canSubmit()"
      (click)="submitOrder()">
      @if (isSubmitting()) {
        提交中...
      } @else {
        提交訂單
      }
    </button>
  `
})
export class CreateOrderComponent {
  // 依賴注入
  private orderService = inject(OrderService);
  private memberService = inject(MemberService);

  // 狀態管理（Signals）
  isLoadingMember = signal(false);
  isSubmitting = signal(false);
  member = signal<MemberResponse | null>(null);
  orderLines = signal<OrderLineResponse[]>([]);
  calculation = signal<CalculationResponse | null>(null);

  // 計算屬性（限制值應從後端 API 取得，此處示意用 500）
  canAddProduct = computed(() => this.orderLines().length < 500);
  canSubmit = computed(() =>
    this.orderLines().length > 0 &&
    this.calculation() !== null &&
    this.member() !== null
  );

  // 載入會員
  async loadMember(memberId: string) {
    this.isLoadingMember.set(true);
    try {
      const member = await this.memberService.getMember(memberId);
      this.member.set(member);
    } finally {
      this.isLoadingMember.set(false);
    }
  }

  // 新增商品
  async addProduct(product: AddProductEvent) {
    if (!this.canAddProduct()) return;

    const line = await this.orderService.addLine(product);
    this.orderLines.update(lines => [...lines, line]);
  }

  // 移除商品
  removeLine(lineId: string) {
    this.orderLines.update(lines =>
      lines.filter(l => l.lineId !== lineId)
    );
  }

  // 提交訂單
  async submitOrder() {
    if (this.isSubmitting()) return;

    this.isSubmitting.set(true);
    try {
      const idempotencyKey = crypto.randomUUID();
      await this.orderService.submit(idempotencyKey);
      // 導航至訂單詳情頁
    } catch (error) {
      // 錯誤處理
    } finally {
      this.isSubmitting.set(false);
    }
  }
}
```

#### 3.2.2 新增 Service

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/orders';

  async createOrder(request: CreateOrderRequest, idempotencyKey: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(this.baseUrl, request, {
        headers: { 'X-Idempotency-Key': idempotencyKey }
      })
    );
  }

  async addLine(orderId: string, request: AddOrderLineRequest): Promise<OrderLineResponse> {
    return firstValueFrom(
      this.http.post<OrderLineResponse>(`${this.baseUrl}/${orderId}/lines`, request)
    );
  }

  async calculate(orderId: string): Promise<CalculationResponse> {
    return firstValueFrom(
      this.http.post<CalculationResponse>(`${this.baseUrl}/${orderId}/calculate`, {})
    );
  }

  async submit(orderId: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(`${this.baseUrl}/${orderId}/submit`, {})
    );
  }
}
```

---

## 4. 測試

### 4.1 後端單元測試

```java
@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void createOrder_shouldGenerateOrderId() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(
            "12345",
            "0001",
            new CustomerInfo("K00123", "劉芒果", "0912345678", null, null),
            new DeliveryAddress("114", "台北市內湖區瑞光路100號"),
            List.of()
        );

        // When
        OrderResponse response = orderService.createOrder(request, UUID.randomUUID().toString());

        // Then
        assertThat(response.orderId()).isNotNull();
        assertThat(response.orderId()).matches("\\d{10}");
    }

    @Test
    void createOrder_shouldRejectDuplicateIdempotencyKey() {
        // Given
        String idempotencyKey = UUID.randomUUID().toString();
        CreateOrderRequest request = createSampleRequest();

        // When
        orderService.createOrder(request, idempotencyKey);

        // Then
        assertThrows(DuplicateSubmissionException.class, () ->
            orderService.createOrder(request, idempotencyKey)
        );
    }
}
```

### 4.2 Playwright E2E 測試

```typescript
// e2e/tests/order/create-order.spec.ts
import { test, expect } from '@playwright/test';

test.describe('新增訂單', () => {
  test.beforeEach(async ({ page }) => {
    // 登入
    await page.goto('/login');
    await page.fill('[data-testid="username"]', 'testuser');
    await page.fill('[data-testid="password"]', 'password');
    await page.click('[data-testid="login-button"]');
    await page.waitForURL('/dashboard');
  });

  test('應該能建立基本訂單', async ({ page }) => {
    // 進入新增訂單頁面
    await page.goto('/orders/new');
    await page.screenshot({ path: 'e2e/screenshots/create-order-1-initial.png' });

    // 輸入會員卡號
    await page.fill('[data-testid="member-id"]', 'K00123');
    await page.click('[data-testid="search-member"]');
    await page.waitForSelector('[data-testid="member-name"]');
    await expect(page.locator('[data-testid="member-name"]')).toHaveText('劉芒果');
    await page.screenshot({ path: 'e2e/screenshots/create-order-2-member.png' });

    // 新增商品
    await page.fill('[data-testid="sku-input"]', 'SKU00001');
    await page.click('[data-testid="add-product"]');
    await page.waitForSelector('[data-testid="product-line"]');
    await page.screenshot({ path: 'e2e/screenshots/create-order-3-product.png' });

    // 試算
    await page.click('[data-testid="calculate-button"]');
    await page.waitForSelector('[data-testid="calculation-result"]');
    await page.screenshot({ path: 'e2e/screenshots/create-order-4-calculation.png' });

    // 提交
    await page.click('[data-testid="submit-button"]');
    await page.waitForURL(/\/orders\/\d+/);
    await page.screenshot({ path: 'e2e/screenshots/create-order-5-success.png' });

    // 驗證訂單編號
    const orderId = await page.locator('[data-testid="order-id"]').textContent();
    expect(orderId).toMatch(/^\d{10}$/);
  });

  test('應該阻止超過系統限制的商品數量', async ({ page }) => {
    // 注意：實際限制為 500 筆（TBL_PARM.SKU_COMPUTE_LIMIT），此測試使用較小數量驗證機制
    // 測試環境可調整 SKU_COMPUTE_LIMIT 為較小值以加速測試
    await page.goto('/orders/new');

    // 假設測試環境限制設為 10 筆
    const testLimit = 10;
    for (let i = 0; i < testLimit; i++) {
      await page.fill('[data-testid="sku-input"]', `SKU${String(i).padStart(5, '0')}`);
      await page.click('[data-testid="add-product"]');
      await page.waitForSelector(`[data-testid="product-line-${i}"]`);
    }

    // 驗證無法繼續新增
    await expect(page.locator('[data-testid="add-product"]')).toBeDisabled();
    await expect(page.locator('[data-testid="limit-warning"]')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/create-order-limit.png' });
  });
});
```

### 4.3 執行測試

```bash
# 後端測試
cd backend
mvn test

# E2E 測試
cd frontend
npm run e2e

# E2E 測試（UI 模式）
npm run e2e:ui

# 查看測試報告
npm run e2e:report
```

---

## 5. API 測試（Swagger UI）

啟動後端後，訪問 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

### 5.1 測試流程

1. **取得 Token**：透過 Keycloak 登入取得 Access Token
2. **建立訂單**：POST /api/v1/orders
3. **新增商品**：POST /api/v1/orders/{orderId}/lines
4. **執行試算**：POST /api/v1/orders/{orderId}/calculate
5. **提交訂單**：POST /api/v1/orders/{orderId}/submit

### 5.2 測試資料

**Mock 會員**:
- 會員卡號：K00123
- 姓名：劉芒果
- 折扣類型：Type 0 (Discounting)

---

## 6. 常見問題

### Q1: Keycloak 無法啟動

```bash
# 檢查 Docker 狀態
docker ps

# 查看 Keycloak 日誌
docker logs keycloak

# 重新啟動
docker-compose restart keycloak
```

### Q2: 資料庫連線失敗

```bash
# 開發環境使用 H2，確認 Profile 正確
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Q3: 前端編譯錯誤

```bash
# 清除快取
npm cache clean --force
rm -rf node_modules
npm install
```

### Q4: Playwright 測試失敗

```bash
# 確認應用程式已啟動
curl http://localhost:8080/api/health
curl http://localhost:4200

# 安裝瀏覽器
npx playwright install
```

---

## 7. 參考資源

- [spec.md](./spec.md) - 功能規格
- [plan.md](./plan.md) - 實作計畫
- [research.md](./research.md) - 研究文件
- [data-model.md](./data-model.md) - 資料模型
- [contracts/order-api.yaml](./contracts/order-api.yaml) - API 規格
- [Constitution](.specify/memory/constitution.md) - 專案憲法
