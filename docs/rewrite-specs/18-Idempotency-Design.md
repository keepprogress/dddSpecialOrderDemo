# 18. Idempotency Design - 冪等性設計

## 目錄

- [1. 什麼是冪等性](#1-什麼是冪等性)
- [2. 需要冪等性的場景](#2-需要冪等性的場景)
- [3. Idempotency-Key 設計](#3-idempotency-key-設計)
- [4. 實作方式](#4-實作方式)
- [5. 測試策略](#5-測試策略)
- [6. 最佳實踐](#6-最佳實踐)

---

## 1. 什麼是冪等性

### 1.1 定義

**冪等性 (Idempotency)**: 同一個操作執行多次與執行一次的效果相同。

```plaintext
數學定義:
f(f(x)) = f(x)

API 定義:
多次相同請求 → 相同結果 + 相同副作用

範例:
✅ GET /api/v1/orders/SO20251027001    # 冪等 (查詢多次結果相同)
✅ PUT /api/v1/orders/SO20251027001    # 冪等 (更新多次結果相同)
✅ DELETE /api/v1/orders/SO20251027001 # 冪等 (刪除多次結果相同)
❌ POST /api/v1/orders                 # 非冪等 (建立多次產生多筆訂單)
❌ POST /api/v1/payments/process       # 非冪等 (付款多次扣多次款)
```

### 1.2 為什麼需要冪等性

```plaintext
場景 1: 網路重試
┌──────────┐                    ┌──────────┐
│  客戶端   │─── Request 1 ────→ │  伺服器   │
│          │                    │          │
│          │← Response (逾時) ──│ (已處理) │
│          │                    │          │
│          │─── Retry ────────→ │          │
│          │                    │ ❌ 重複處理│
└──────────┘                    └──────────┘
結果: 訂單建立 2 次、扣款 2 次

場景 2: 用戶重複點擊
用戶連點 3 次「確認付款」按鈕
→ 發送 3 個請求
→ 扣款 3 次
→ 用戶損失

場景 3: 訊息重複消費
Message Queue 重複投遞訊息
→ 訂單狀態重複更新
→ 資料不一致
```

---

## 2. 需要冪等性的場景

### 2.1 HTTP Methods 冪等性矩陣

| HTTP Method | 冪等性 | 說明 | 實作方式 |
|------------|-------|------|---------|
| GET | ✅ 是 | 查詢不改變狀態 | 天然冪等 |
| PUT | ✅ 是 | 全量更新, 多次結果相同 | 天然冪等 |
| PATCH | ⚠️ 視情況 | 部分更新, 取決於操作 | 樂觀鎖 |
| DELETE | ✅ 是 | 刪除多次結果相同 | 天然冪等 |
| POST | ❌ 否 | 建立資源, 多次產生多筆 | **需實作** |

### 2.2 SOM 系統需要冪等性的 API

#### 2.2.1 訂單服務

```http
# ✅ 需要冪等性
POST /api/v1/orders                     # 建立訂單
POST /api/v1/orders/{orderId}/confirm   # 確認訂單
POST /api/v1/orders/{orderId}/cancel    # 取消訂單

# ✅ 天然冪等
GET /api/v1/orders/{orderId}
PUT /api/v1/orders/{orderId}
DELETE /api/v1/orders/{orderId}
```

#### 2.2.2 付款服務

```http
# ✅ 需要冪等性 (最重要!)
POST /api/v1/payments/prepare           # 準備付款
POST /api/v1/payments/process           # 處理付款
POST /api/v1/payments/cancel            # 取消付款
POST /api/v1/payments/refund            # 退款

# ✅ 天然冪等
GET /api/v1/payments/{paymentId}
```

#### 2.2.3 計價服務

```http
# ⚠️ 計價查詢可視為冪等 (無副作用)
POST /api/v1/pricing/calculate          # 計算價格 (只查詢, 不儲存)

# ✅ 需要冪等性
POST /api/v1/pricing/save               # 儲存計價結果
```

---

## 3. Idempotency-Key 設計

### 3.1 Idempotency-Key Header

```http
POST /api/v1/payments/process HTTP/1.1
Host: api.som.com
Authorization: Bearer eyJhbGc...
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000

{
  "paymentId": "PAY20251027001",
  "amount": 9500.00,
  "paymentMethod": "CREDIT_CARD"
}
```

**規範**:

1. **Header 名稱**: `Idempotency-Key` (RFC 建議)
2. **格式**: UUID v4 (36 字元)
3. **生成**: 客戶端生成 (前端或 API Gateway)
4. **唯一性**: 全域唯一, 不重複
5. **TTL**: 24 小時 (過期後可重複使用)

### 3.2 客戶端生成 Idempotency-Key

**Angular 8 (Frontend)**:

```typescript
// services/idempotency.service.ts
import { Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';

@Injectable({
  providedIn: 'root'
})
export class IdempotencyService {
  /**
   * 生成冪等鍵
   */
  generateKey(): string {
    return uuidv4();  // 550e8400-e29b-41d4-a716-446655440000
  }

  /**
   * 為操作生成並儲存冪等鍵 (防止重複點擊)
   */
  getOrCreateKey(operationId: string): string {
    const storageKey = `idempotency_key_${operationId}`;
    let key = sessionStorage.getItem(storageKey);

    if (!key) {
      key = this.generateKey();
      sessionStorage.setItem(storageKey, key);
    }

    return key;
  }

  /**
   * 清除冪等鍵 (操作完成後)
   */
  clearKey(operationId: string): void {
    const storageKey = `idempotency_key_${operationId}`;
    sessionStorage.removeItem(storageKey);
  }
}

// services/payment.service.ts
@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  constructor(
    private http: HttpClient,
    private idempotencyService: IdempotencyService
  ) {}

  processPayment(request: PaymentRequest): Observable<PaymentResponse> {
    // 生成或取得冪等鍵
    const idempotencyKey = this.idempotencyService.getOrCreateKey(
      `payment_${request.paymentId}`
    );

    // 加入 Idempotency-Key header
    const headers = new HttpHeaders({
      'Idempotency-Key': idempotencyKey
    });

    return this.http.post<PaymentResponse>(
      '/api/v1/payments/process',
      request,
      { headers }
    ).pipe(
      tap(() => {
        // 付款成功, 清除冪等鍵
        this.idempotencyService.clearKey(`payment_${request.paymentId}`);
      })
    );
  }
}
```

**防止重複點擊**:

```typescript
// components/payment-confirm.component.ts
export class PaymentConfirmComponent {
  isProcessing = false;

  constructor(private paymentService: PaymentService) {}

  onConfirmPayment(): void {
    // 防止重複點擊
    if (this.isProcessing) {
      return;
    }

    this.isProcessing = true;

    this.paymentService.processPayment(this.paymentRequest)
      .pipe(
        finalize(() => {
          this.isProcessing = false;  // 無論成功失敗都重置
        })
      )
      .subscribe(
        response => {
          this.showSuccess('付款成功');
        },
        error => {
          this.showError('付款失敗: ' + error.message);
        }
      );
  }
}
```

---

## 4. 實作方式

### 4.1 Redis 快取實作 (推薦)

#### 4.1.1 流程圖

```plaintext
┌───────────────────────────────────────────────────────────┐
│            Idempotency Processing Flow                    │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  1. 請求進來                                               │
│     ↓                                                      │
│  2. 檢查 Redis: payment:idempotency:{key}                │
│     ↓                                                      │
│     是否存在?                                              │
│     ├─ YES → 回傳快取結果 (200 OK)                        │
│     │                                                      │
│     └─ NO                                                  │
│         ↓                                                  │
│  3. 取得分散式鎖: lock:payment:{key}                      │
│     ↓                                                      │
│     是否取得?                                              │
│     ├─ NO → 等待 100ms, 重新檢查快取 (最多 3 次)          │
│     │                                                      │
│     └─ YES                                                 │
│         ↓                                                  │
│  4. 再次檢查快取 (Double-Check)                           │
│     ↓                                                      │
│     是否存在?                                              │
│     ├─ YES → 釋放鎖, 回傳快取結果                         │
│     │                                                      │
│     └─ NO                                                  │
│         ↓                                                  │
│  5. 執行業務邏輯 (付款)                                    │
│     ↓                                                      │
│  6. 儲存結果到 Redis (TTL 24 小時)                        │
│     ↓                                                      │
│  7. 釋放鎖                                                 │
│     ↓                                                      │
│  8. 回傳結果                                               │
│                                                            │
└───────────────────────────────────────────────────────────┘
```

#### 4.1.2 程式碼實作

```java
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PaymentProcessor paymentProcessor;

    private static final String IDEMPOTENCY_PREFIX = "payment:idempotency:";
    private static final String LOCK_PREFIX = "lock:payment:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final int MAX_RETRY = 3;

    /**
     * 處理付款 (冪等性保證)
     */
    public PaymentResponse processPayment(
        String idempotencyKey,
        PaymentRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);

        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;
        String lockKey = LOCK_PREFIX + idempotencyKey;

        // 1. 檢查快取
        PaymentResponse cached = getCachedResponse(cacheKey);
        if (cached != null) {
            log.info("Idempotency key hit: {}", idempotencyKey);
            return cached;
        }

        // 2. 取得分散式鎖
        boolean locked = acquireLock(lockKey);
        if (!locked) {
            // 未取得鎖, 重試檢查快取
            return retryWithCache(cacheKey, idempotencyKey, request);
        }

        try {
            // 3. Double-Check 快取 (避免競爭條件)
            cached = getCachedResponse(cacheKey);
            if (cached != null) {
                log.info("Idempotency key hit after lock: {}", idempotencyKey);
                return cached;
            }

            // 4. 執行付款邏輯
            log.info("Processing payment for idempotency key: {}", idempotencyKey);
            PaymentResponse response = paymentProcessor.process(request);

            // 5. 儲存結果到快取
            cacheResponse(cacheKey, response);

            return response;

        } finally {
            // 6. 釋放鎖
            releaseLock(lockKey);
        }
    }

    /**
     * 驗證 Idempotency-Key 格式
     */
    private void validateIdempotencyKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }

        // 驗證 UUID 格式
        try {
            UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Idempotency-Key must be a valid UUID", e
            );
        }
    }

    /**
     * 取得快取結果
     */
    private PaymentResponse getCachedResponse(String cacheKey) {
        return (PaymentResponse) redisTemplate.opsForValue().get(cacheKey);
    }

    /**
     * 快取結果
     */
    private void cacheResponse(String cacheKey, PaymentResponse response) {
        redisTemplate.opsForValue().set(cacheKey, response, IDEMPOTENCY_TTL);
        log.info("Cached payment response: {}", cacheKey);
    }

    /**
     * 取得分散式鎖
     */
    private boolean acquireLock(String lockKey) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey, "1", LOCK_TTL
        );
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 釋放鎖
     */
    private void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }

    /**
     * 重試檢查快取
     */
    private PaymentResponse retryWithCache(
        String cacheKey,
        String idempotencyKey,
        PaymentRequest request
    ) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                Thread.sleep(100);  // 等待 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting", e);
            }

            PaymentResponse cached = getCachedResponse(cacheKey);
            if (cached != null) {
                log.info("Idempotency key hit on retry {}: {}", i + 1, idempotencyKey);
                return cached;
            }
        }

        // 重試後仍無快取, 重新處理
        log.warn("Max retry reached for idempotency key: {}", idempotencyKey);
        return processPayment(idempotencyKey, request);
    }
}
```

### 4.2 資料庫實作 (備用方案)

```java
/**
 * 使用資料庫記錄冪等鍵
 * 優點: 資料持久化, 不受 Redis 重啟影響
 * 缺點: 效能較 Redis 慢
 */
@Service
public class PaymentServiceDbIdempotency {

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Transactional
    public PaymentResponse processPayment(
        String idempotencyKey,
        PaymentRequest request
    ) {
        // 1. 檢查資料庫
        Optional<IdempotencyRecord> existing =
            idempotencyRepository.findByKey(idempotencyKey);

        if (existing.isPresent()) {
            log.info("Idempotency key hit: {}", idempotencyKey);
            return parseResponse(existing.get().getResponseBody());
        }

        // 2. 建立記錄 (UNIQUE 約束防止並發)
        try {
            IdempotencyRecord record = new IdempotencyRecord();
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestMethod("POST");
            record.setRequestPath("/api/v1/payments/process");
            record.setRequestBody(serializeRequest(request));
            record.setStatus("PROCESSING");
            record.setExpiresAt(LocalDateTime.now().plusHours(24));

            idempotencyRepository.save(record);

        } catch (DataIntegrityViolationException e) {
            // 並發時 UNIQUE 約束失敗, 重試查詢
            log.warn("Concurrent request detected for key: {}", idempotencyKey);
            return processPayment(idempotencyKey, request);
        }

        // 3. 執行付款邏輯
        PaymentResponse response = paymentProcessor.process(request);

        // 4. 更新記錄
        IdempotencyRecord record = idempotencyRepository
            .findByKey(idempotencyKey)
            .orElseThrow();
        record.setStatus("COMPLETED");
        record.setResponseStatus(200);
        record.setResponseBody(serializeResponse(response));
        idempotencyRepository.save(record);

        return response;
    }
}
```

**資料表設計**:

```sql
CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR2(100) PRIMARY KEY,  -- UUID
    request_method VARCHAR2(10) NOT NULL,       -- POST
    request_path VARCHAR2(200) NOT NULL,        -- /api/v1/payments/process
    request_body CLOB,                          -- JSON
    response_status INT,                        -- 200
    response_body CLOB,                         -- JSON
    status VARCHAR2(20) DEFAULT 'PENDING',      -- PENDING, PROCESSING, COMPLETED, FAILED
    expires_at TIMESTAMP NOT NULL,              -- 24 小時後過期
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);
```

### 4.3 Controller 層實作

```java
@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * 處理付款 (冪等性保證)
     */
    @PostMapping("/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
        @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
        @RequestBody @Valid PaymentRequest request
    ) {
        log.info("Received payment request with idempotency key: {}", idempotencyKey);

        PaymentResponse response = paymentService.processPayment(
            idempotencyKey, request
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**API Gateway 層驗證**:

```java
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final List<String> IDEMPOTENT_METHODS =
        List.of("POST");

    private static final List<String> IDEMPOTENT_PATHS = List.of(
        "/api/v1/orders",
        "/api/v1/payments/process",
        "/api/v1/payments/refund"
    );

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // 檢查是否需要 Idempotency-Key
        if (requiresIdempotencyKey(method, path)) {
            String idempotencyKey = request.getHeader("Idempotency-Key");

            if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"success\": false, \"error\": {\"code\": \"IDEMPOTENCY_KEY_REQUIRED\", \"message\": \"Idempotency-Key header is required for this operation\"}}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresIdempotencyKey(String method, String path) {
        if (!IDEMPOTENT_METHODS.contains(method)) {
            return false;
        }

        return IDEMPOTENT_PATHS.stream()
            .anyMatch(path::startsWith);
    }
}
```

---

## 5. 測試策略

### 5.1 單元測試

```java
@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceIdempotencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        // 清空 Redis
        redisTemplate.getConnectionFactory()
            .getConnection()
            .flushAll();
    }

    @Test
    @DisplayName("相同 Idempotency-Key 只處理一次")
    void testIdempotency_SameKey_ProcessOnce() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = createPaymentRequest();

        PaymentResponse expectedResponse = new PaymentResponse();
        expectedResponse.setPaymentId("PAY001");
        expectedResponse.setStatus("COMPLETED");

        when(paymentProcessor.process(any())).thenReturn(expectedResponse);

        // Act - 第一次請求
        PaymentResponse response1 = paymentService.processPayment(
            idempotencyKey, request
        );

        // Act - 第二次請求 (相同 key)
        PaymentResponse response2 = paymentService.processPayment(
            idempotencyKey, request
        );

        // Assert
        assertEquals(response1.getPaymentId(), response2.getPaymentId());
        assertEquals(response1.getStatus(), response2.getStatus());

        // 驗證 paymentProcessor 只被呼叫一次
        verify(paymentProcessor, times(1)).process(any());
    }

    @Test
    @DisplayName("不同 Idempotency-Key 分別處理")
    void testIdempotency_DifferentKeys_ProcessSeparately() {
        // Arrange
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        PaymentRequest request = createPaymentRequest();

        when(paymentProcessor.process(any()))
            .thenReturn(createPaymentResponse("PAY001"))
            .thenReturn(createPaymentResponse("PAY002"));

        // Act
        PaymentResponse response1 = paymentService.processPayment(key1, request);
        PaymentResponse response2 = paymentService.processPayment(key2, request);

        // Assert
        assertNotEquals(response1.getPaymentId(), response2.getPaymentId());
        verify(paymentProcessor, times(2)).process(any());
    }

    @Test
    @DisplayName("並發請求使用相同 Idempotency-Key 只處理一次")
    void testIdempotency_ConcurrentRequests_ProcessOnce() throws Exception {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = createPaymentRequest();

        AtomicInteger processCount = new AtomicInteger(0);
        when(paymentProcessor.process(any())).thenAnswer(invocation -> {
            processCount.incrementAndGet();
            Thread.sleep(100);  // 模擬處理時間
            return createPaymentResponse("PAY001");
        });

        // Act - 並發發送 10 個請求
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Future<PaymentResponse>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.countDown();
                latch.await();
                return paymentService.processPayment(idempotencyKey, request);
            }));
        }

        // 等待所有請求完成
        List<PaymentResponse> responses = futures.stream()
            .map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        executor.shutdown();

        // Assert
        // 所有回應應該相同
        assertEquals(1, responses.stream()
            .map(PaymentResponse::getPaymentId)
            .distinct()
            .count());

        // 實際處理應該只有 1 次
        assertEquals(1, processCount.get());
    }
}
```

### 5.2 整合測試

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentApiIdempotencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("相同 Idempotency-Key 回傳相同結果")
    void testIdempotency_API_SameKey() {
        // Arrange
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = createPaymentRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        // Act - 發送兩次請求
        ResponseEntity<ApiResponse> response1 = restTemplate.postForEntity(
            "/api/v1/payments/process",
            entity,
            ApiResponse.class
        );

        ResponseEntity<ApiResponse> response2 = restTemplate.postForEntity(
            "/api/v1/payments/process",
            entity,
            ApiResponse.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(response1.getBody(), response2.getBody());
    }

    @Test
    @DisplayName("缺少 Idempotency-Key 回傳 400")
    void testIdempotency_API_MissingKey() {
        // Arrange
        PaymentRequest request = createPaymentRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 故意不加 Idempotency-Key

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/payments/process",
            entity,
            ApiResponse.class
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals(
            "IDEMPOTENCY_KEY_REQUIRED",
            response.getBody().getError().getCode()
        );
    }
}
```

---

## 6. 最佳實踐

### 6.1 Idempotency-Key 生成原則

```typescript
// ✅ 推薦: 客戶端生成 UUID
const idempotencyKey = uuidv4();

// ❌ 錯誤: 使用業務 ID
const idempotencyKey = `payment_${paymentId}`;
// 問題: paymentId 可能重複使用, 導致不同操作被誤判為相同

// ❌ 錯誤: 使用時間戳
const idempotencyKey = Date.now().toString();
// 問題: 多台機器時間不同步可能產生相同 key

// ❌ 錯誤: 使用請求內容 Hash
const idempotencyKey = md5(JSON.stringify(request));
// 問題: 請求內容相同但意圖不同的操作會被誤判
```

### 6.2 TTL 設定原則

```java
// ✅ 推薦: 24 小時 TTL
Duration TTL = Duration.ofHours(24);

// 原因:
// 1. 覆蓋大部分重試場景 (網路問題、用戶重複操作)
// 2. 避免記憶體/儲存空間無限增長
// 3. 24 小時後業務上不太可能再重試

// ⚠️ 特殊情況: 永久保存
// 如: 金融交易、法律文件簽署
// 需使用資料庫永久記錄, 不能只依賴 Redis
```

### 6.3 錯誤處理

```java
/**
 * 冪等性操作失敗後的處理
 */
@Service
public class PaymentService {

    public PaymentResponse processPayment(
        String idempotencyKey,
        PaymentRequest request
    ) {
        String cacheKey = IDEMPOTENCY_PREFIX + idempotencyKey;

        try {
            // 執行付款邏輯
            PaymentResponse response = paymentProcessor.process(request);

            // 只快取成功結果
            if ("COMPLETED".equals(response.getStatus())) {
                cacheResponse(cacheKey, response);
            } else {
                // ⚠️ 失敗結果不快取
                // 允許客戶端使用相同 key 重試
                log.warn("Payment failed, not caching: {}", idempotencyKey);
            }

            return response;

        } catch (Exception e) {
            // ⚠️ 異常不快取
            // 允許客戶端重試
            log.error("Payment error, not caching: {}", idempotencyKey, e);
            throw e;
        }
    }
}
```

### 6.4 監控指標

```java
@Component
@Aspect
public class IdempotencyMonitoringAspect {

    @Autowired
    private MeterRegistry meterRegistry;

    @Around("@annotation(IdempotentOperation)")
    public Object monitor(ProceedingJoinPoint pjp) throws Throwable {
        String operation = pjp.getSignature().getName();

        try {
            Object result = pjp.proceed();

            // 記錄成功 (首次處理或快取命中)
            meterRegistry.counter("idempotency.requests",
                "operation", operation,
                "result", "success"
            ).increment();

            return result;

        } catch (Exception e) {
            // 記錄失敗
            meterRegistry.counter("idempotency.requests",
                "operation", operation,
                "result", "failure"
            ).increment();

            throw e;
        }
    }
}

// Prometheus Metrics
idempotency_requests_total{operation="processPayment", result="success"} 95000
idempotency_requests_total{operation="processPayment", result="failure"} 5000

idempotency_cache_hit_rate{operation="processPayment"} 0.08  # 8% 重試率
```

---

## 總結

### 冪等性設計核心要點

1. **Idempotency-Key**: 客戶端生成 UUID, 24 小時 TTL
2. **實作方式**: Redis 快取 (推薦) 或資料庫記錄 (備用)
3. **分散式鎖**: 防止並發請求重複處理
4. **Double-Check**: 取得鎖後再次檢查快取
5. **只快取成功**: 失敗結果不快取, 允許重試
6. **監控**: 追蹤重試率 (正常 5-10%)

### 適用場景

| 場景 | 需要冪等性 | 實作方式 |
|-----|----------|---------|
| 訂單建立 | ✅ | Idempotency-Key |
| 付款處理 | ✅ | Idempotency-Key |
| 退款 | ✅ | Idempotency-Key |
| 訂單查詢 | ❌ | 天然冪等 (GET) |
| 訂單更新 | ⚠️ | 樂觀鎖 (Version) |
| 訂單刪除 | ❌ | 天然冪等 (DELETE) |

---

**參考文件**:
- `14-API-Payment-Service.md`: 付款服務 API
- `17-Cache-Strategy.md`: Redis 快取策略
- `16-Database-Design.md`: 資料庫設計

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
