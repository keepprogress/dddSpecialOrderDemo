# 11. API 設計原則 (API Design Principles)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **相關文檔**:
  - [08-Architecture-Overview.md](./08-Architecture-Overview.md)
  - [12-API-Order-Management.md](./12-API-Order-Management.md)
  - [13-API-Pricing-Service.md](./13-API-Pricing-Service.md)

---

## 目錄
1. [RESTful 設計原則](#restful-設計原則)
2. [URL 命名規範](#url-命名規範)
3. [HTTP 方法使用](#http-方法使用)
4. [請求與回應格式](#請求與回應格式)
5. [錯誤處理](#錯誤處理)
6. [版本控制](#版本控制)
7. [分頁與排序](#分頁與排序)
8. [安全性設計](#安全性設計)

---

## RESTful 設計原則

### 核心原則

```
RESTful API 六大原則:
├─ 1. Client-Server 架構 (前後端分離)
├─ 2. Stateless (無狀態，每個請求獨立)
├─ 3. Cacheable (可快取，提升效能)
├─ 4. Uniform Interface (統一介面)
├─ 5. Layered System (分層系統)
└─ 6. Code on Demand (可選，按需載入程式碼)
```

### 資源導向設計

**❌ 不良設計 (RPC 風格)**:
```
POST /api/v1/createOrder
POST /api/v1/getOrderById?id=123
POST /api/v1/updateOrderStatus
POST /api/v1/deleteOrder
```

**✅ 良好設計 (RESTful)**:
```
POST   /api/v1/orders              # 建立訂單
GET    /api/v1/orders/{orderId}    # 查詢訂單
PUT    /api/v1/orders/{orderId}    # 更新訂單
DELETE /api/v1/orders/{orderId}    # 刪除訂單
PATCH  /api/v1/orders/{orderId}/status  # 部分更新：狀態
```

---

## URL 命名規範

### 基本規則

| 規則 | 說明 | 範例 |
|------|------|------|
| **複數名詞** | 使用複數表示資源集合 | `/orders` (✅) vs `/order` (❌) |
| **小寫字母** | 全部小寫，避免大小寫混淆 | `/orders` (✅) vs `/Orders` (❌) |
| **連字號分隔** | 使用 `-` 而非 `_` | `/order-items` (✅) vs `/order_items` (❌) |
| **階層關係** | 反映資源階層 | `/orders/{orderId}/items/{itemId}` |
| **避免動詞** | URL 是名詞，動作由 HTTP 方法表達 | `/orders` (✅) vs `/getOrders` (❌) |

### URL 結構

```
https://{domain}/{service}/{version}/{resource}/{id}/{sub-resource}
│       │         │         │         │         │    │
│       │         │         │         │         │    └─ 子資源
│       │         │         │         │         └────── 資源 ID
│       │         │         │         └──────────────── 資源名稱
│       │         │         └────────────────────────── API 版本
│       │         └──────────────────────────────────── 服務名稱
│       └────────────────────────────────────────────── 域名
└────────────────────────────────────────────────────── 協定

範例:
https://api.example.com/orders/v1/orders/SO20251027001/items
```

### 完整 URL 範例

```
# 訂單資源
GET    /api/v1/orders                          # 查詢訂單列表
POST   /api/v1/orders                          # 建立訂單
GET    /api/v1/orders/{orderId}                # 查詢單一訂單
PUT    /api/v1/orders/{orderId}                # 完整更新訂單
PATCH  /api/v1/orders/{orderId}                # 部分更新訂單
DELETE /api/v1/orders/{orderId}                # 刪除訂單

# 訂單狀態操作 (子資源)
PATCH  /api/v1/orders/{orderId}/status         # 更新訂單狀態
POST   /api/v1/orders/{orderId}/cancel         # 取消訂單
POST   /api/v1/orders/{orderId}/confirm        # 確認訂單

# 訂單項目 (子資源)
GET    /api/v1/orders/{orderId}/items          # 查詢訂單項目
POST   /api/v1/orders/{orderId}/items          # 新增項目
PUT    /api/v1/orders/{orderId}/items/{itemId} # 更新項目
DELETE /api/v1/orders/{orderId}/items/{itemId} # 刪除項目

# 計價資源
POST   /api/v1/pricing/calculate               # 計算價格
GET    /api/v1/pricing/history/{memberCardId}  # 查詢計價歷史

# 會員資源
GET    /api/v1/members/{memberCardId}          # 查詢會員資訊
GET    /api/v1/members/{memberCardId}/discounts # 查詢會員折扣
GET    /api/v1/members/{memberCardId}/orders   # 查詢會員訂單

# 查詢參數範例
GET    /api/v1/orders?status=VALID&page=0&size=20&sort=createTime,desc
```

---

## HTTP 方法使用

### 標準方法

| HTTP 方法 | 用途 | 冪等性 | 安全性 | 請求 Body | 回應 Body |
|----------|------|--------|--------|-----------|-----------|
| **GET** | 查詢資源 | ✅ 是 | ✅ 是 | ❌ 無 | ✅ 有 |
| **POST** | 建立資源 | ❌ 否 | ❌ 否 | ✅ 有 | ✅ 有 |
| **PUT** | 完整更新資源 | ✅ 是 | ❌ 否 | ✅ 有 | ✅ 有 |
| **PATCH** | 部分更新資源 | ❌ 否* | ❌ 否 | ✅ 有 | ✅ 有 |
| **DELETE** | 刪除資源 | ✅ 是 | ❌ 否 | ❌ 無 | ✅ 有** |

*PATCH 設計上應該是冪等的，但實作上不一定
**DELETE 通常回應 204 No Content (無 Body)

### 方法選擇指南

#### GET - 查詢資源
```http
# 查詢訂單列表
GET /api/v1/orders?status=VALID&page=0&size=20 HTTP/1.1
Authorization: Bearer {token}

# 回應
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": "SO20251027001",
        "totalAmt": 9500.00,
        "status": "VALID"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 156
  }
}
```

**要點**:
- 無副作用（不修改資料）
- 可快取
- 參數放在 Query String

---

#### POST - 建立資源
```http
# 建立訂單
POST /api/v1/orders HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "skus": [
    {
      "skuNo": "SKU001",
      "quantity": 2
    }
  ],
  "remark": "請準時送達"
}

# 回應
HTTP/1.1 201 Created
Location: /api/v1/orders/SO20251027001
Content-Type: application/json

{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "memberCardId": "A123456789",
    "totalAmt": 9500.00,
    "status": "DRAFTS",
    "createTime": "2025-10-27T10:30:00Z"
  }
}
```

**要點**:
- 回應 **201 Created**
- 回應 Header 包含 `Location`（新資源 URL）
- 回應 Body 包含新建立的資源
- 非冪等（多次呼叫會建立多個資源）

---

#### PUT - 完整更新資源
```http
# 完整更新訂單（需提供所有欄位）
PUT /api/v1/orders/SO20251027001 HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "orderId": "SO20251027001",
  "memberCardId": "A123456789",
  "skus": [
    {
      "skuNo": "SKU001",
      "quantity": 3  // 修改數量
    }
  ],
  "remark": "請在下午送達",  // 修改備註
  "status": "VALID"  // 必須提供所有欄位
}

# 回應
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "memberCardId": "A123456789",
    "totalAmt": 14250.00,
    "status": "VALID",
    "updateTime": "2025-10-27T11:00:00Z"
  }
}
```

**要點**:
- 完整替換資源（需提供所有欄位）
- 冪等（多次呼叫結果相同）
- 若資源不存在，可選擇回應 404 或建立新資源（較少見）

---

#### PATCH - 部分更新資源
```http
# 部分更新訂單（只更新狀態）
PATCH /api/v1/orders/SO20251027001/status HTTP/1.1
Authorization: Bearer {token}
Content-Type: application/json

{
  "status": "PAID"
}

# 回應
HTTP/1.1 200 OK
Content-Type: application/json

{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": "PAID",
    "updateTime": "2025-10-27T12:00:00Z"
  }
}
```

**要點**:
- 只更新提供的欄位
- 適合狀態變更、部分欄位更新
- 實作上應該是冪等的（但不強制）

---

#### DELETE - 刪除資源
```http
# 刪除訂單（軟刪除）
DELETE /api/v1/orders/SO20251027001 HTTP/1.1
Authorization: Bearer {token}

# 回應
HTTP/1.1 204 No Content
```

**要點**:
- 回應 **204 No Content**（無 Body）
- 或回應 **200 OK** + Body（包含刪除的資源資訊）
- 冪等（多次刪除結果相同）
- 建議使用軟刪除（標記為已刪除，不實際刪除）

---

## 請求與回應格式

### 統一回應格式

```typescript
// 成功回應
interface ApiResponse<T> {
  success: true;
  data: T;
  message?: string;
  timestamp: string;
  traceId: string;
}

// 錯誤回應
interface ErrorResponse {
  success: false;
  error: {
    code: string;
    message: string;
    details?: string;
    validationErrors?: ValidationError[];
  };
  timestamp: string;
  traceId: string;
  path: string;
}

interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
}
```

### 成功回應範例

```json
// 200 OK - 查詢成功
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "memberCardId": "A123456789",
    "totalAmt": 9500.00,
    "status": "VALID"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456"
}

// 201 Created - 建立成功
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "memberCardId": "A123456789",
    "totalAmt": 9500.00,
    "status": "DRAFTS",
    "createTime": "2025-10-27T10:30:00Z"
  },
  "message": "訂單建立成功",
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456"
}

// 204 No Content - 刪除成功（無 Body）
```

### 錯誤回應範例

```json
// 400 Bad Request - 驗證錯誤
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "請求參數驗證失敗",
    "validationErrors": [
      {
        "field": "memberCardId",
        "message": "會員卡號不可為空",
        "rejectedValue": null
      },
      {
        "field": "skus[0].quantity",
        "message": "商品數量必須大於 0",
        "rejectedValue": -1
      }
    ]
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders"
}

// 401 Unauthorized - 未授權
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "未授權，請先登入",
    "details": "JWT Token 無效或已過期"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders"
}

// 403 Forbidden - 無權限
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "無權限執行此操作",
    "details": "需要 ADMIN 角色"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/admin/orders"
}

// 404 Not Found - 資源不存在
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "找不到訂單",
    "details": "訂單編號 SO20251027001 不存在"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001"
}

// 409 Conflict - 資源衝突
{
  "success": false,
  "error": {
    "code": "ORDER_ALREADY_PAID",
    "message": "訂單已付款，無法取消",
    "details": "訂單狀態: PAID"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001/cancel"
}

// 500 Internal Server Error - 伺服器錯誤
{
  "success": false,
  "error": {
    "code": "INTERNAL_SERVER_ERROR",
    "message": "系統發生錯誤，請稍後再試",
    "details": "資料庫連線失敗"  // 生產環境不應暴露詳細錯誤
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders"
}
```

---

## 錯誤處理

### HTTP 狀態碼使用

| 狀態碼 | 說明 | 使用時機 |
|--------|------|---------|
| **2xx 成功** | | |
| 200 OK | 成功 | GET, PUT, PATCH 成功 |
| 201 Created | 已建立 | POST 建立成功 |
| 204 No Content | 無內容 | DELETE 成功 |
| **3xx 重新導向** | | |
| 301 Moved Permanently | 永久移動 | API 端點永久變更 |
| 302 Found | 暫時移動 | API 端點暫時變更 |
| 304 Not Modified | 未修改 | 快取有效 |
| **4xx 客戶端錯誤** | | |
| 400 Bad Request | 錯誤請求 | 參數驗證失敗 |
| 401 Unauthorized | 未授權 | JWT Token 無效/過期 |
| 403 Forbidden | 禁止存取 | 無權限 |
| 404 Not Found | 找不到 | 資源不存在 |
| 405 Method Not Allowed | 方法不允許 | HTTP 方法錯誤 |
| 409 Conflict | 衝突 | 資源狀態衝突 |
| 422 Unprocessable Entity | 無法處理 | 業務邏輯驗證失敗 |
| 429 Too Many Requests | 請求過多 | 超過限流 |
| **5xx 伺服器錯誤** | | |
| 500 Internal Server Error | 伺服器錯誤 | 未預期錯誤 |
| 502 Bad Gateway | 閘道錯誤 | 上游服務錯誤 |
| 503 Service Unavailable | 服務不可用 | 服務維護中 |
| 504 Gateway Timeout | 閘道逾時 | 上游服務逾時 |

### 錯誤碼設計

```typescript
// 錯誤碼格式: {模組}_{錯誤類型}
enum ErrorCode {
  // 通用錯誤 (COMMON_*)
  VALIDATION_ERROR = 'COMMON_VALIDATION_ERROR',
  UNAUTHORIZED = 'COMMON_UNAUTHORIZED',
  FORBIDDEN = 'COMMON_FORBIDDEN',
  RESOURCE_NOT_FOUND = 'COMMON_RESOURCE_NOT_FOUND',
  INTERNAL_SERVER_ERROR = 'COMMON_INTERNAL_SERVER_ERROR',

  // 訂單錯誤 (ORDER_*)
  ORDER_NOT_FOUND = 'ORDER_NOT_FOUND',
  ORDER_ALREADY_PAID = 'ORDER_ALREADY_PAID',
  ORDER_ALREADY_CANCELLED = 'ORDER_ALREADY_CANCELLED',
  ORDER_INVALID_STATUS = 'ORDER_INVALID_STATUS',
  ORDER_CREATION_FAILED = 'ORDER_CREATION_FAILED',

  // 計價錯誤 (PRICING_*)
  PRICING_CALCULATION_FAILED = 'PRICING_CALCULATION_FAILED',
  PRICING_INVALID_SKU = 'PRICING_INVALID_SKU',
  PRICING_MEMBER_NOT_FOUND = 'PRICING_MEMBER_NOT_FOUND',
  PRICING_DISCOUNT_INVALID = 'PRICING_DISCOUNT_INVALID',

  // 會員錯誤 (MEMBER_*)
  MEMBER_NOT_FOUND = 'MEMBER_NOT_FOUND',
  MEMBER_CARD_EXPIRED = 'MEMBER_CARD_EXPIRED',
  MEMBER_DISCOUNT_NOT_APPLICABLE = 'MEMBER_DISCOUNT_NOT_APPLICABLE',

  // 付款錯誤 (PAYMENT_*)
  PAYMENT_FAILED = 'PAYMENT_FAILED',
  PAYMENT_AMOUNT_MISMATCH = 'PAYMENT_AMOUNT_MISMATCH',
  PAYMENT_ALREADY_PROCESSED = 'PAYMENT_ALREADY_PROCESSED',

  // 外部整合錯誤 (EXTERNAL_*)
  CRM_CONNECTION_ERROR = 'EXTERNAL_CRM_CONNECTION_ERROR',
  POS_SYNC_FAILED = 'EXTERNAL_POS_SYNC_FAILED',
  HISU_API_ERROR = 'EXTERNAL_HISU_API_ERROR'
}
```

### 錯誤處理實作

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        ValidationException ex,
        HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .error(ErrorDetail.builder()
                .code(ErrorCode.VALIDATION_ERROR)
                .message("請求參數驗證失敗")
                .validationErrors(ex.getValidationErrors())
                .build())
            .timestamp(LocalDateTime.now())
            .traceId(MDC.get("traceId"))
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
        OrderNotFoundException ex,
        HttpServletRequest request
    ) {
        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .error(ErrorDetail.builder()
                .code(ErrorCode.ORDER_NOT_FOUND)
                .message("找不到訂單")
                .details(ex.getMessage())
                .build())
            .timestamp(LocalDateTime.now())
            .traceId(MDC.get("traceId"))
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error("未預期錯誤", ex);

        ErrorResponse error = ErrorResponse.builder()
            .success(false)
            .error(ErrorDetail.builder()
                .code(ErrorCode.INTERNAL_SERVER_ERROR)
                .message("系統發生錯誤，請稍後再試")
                // 生產環境不暴露詳細錯誤
                .details(environment.getActiveProfiles().contains("prod")
                    ? null
                    : ex.getMessage())
                .build())
            .timestamp(LocalDateTime.now())
            .traceId(MDC.get("traceId"))
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 版本控制

### 版本策略

```
API 版本控制方式比較:

1. URL Path Versioning (推薦) ✅
   /api/v1/orders
   /api/v2/orders

   優點: 清晰、易於理解、易於測試
   缺點: URL 變更

2. Query Parameter Versioning
   /api/orders?version=1

   優點: URL 不變
   缺點: 容易被忽略、快取問題

3. Header Versioning
   GET /api/orders
   X-API-Version: 1

   優點: URL 不變、RESTful
   缺點: 測試不便、文檔複雜

4. Content Negotiation (Accept Header)
   GET /api/orders
   Accept: application/vnd.som.v1+json

   優點: RESTful、標準
   缺點: 複雜、測試不便
```

### 推薦：URL Path Versioning

```
/api/v1/orders      # Version 1 (目前)
/api/v2/orders      # Version 2 (新版)
/api/v3/orders      # Version 3 (未來)

優點:
✅ 清晰易懂
✅ 易於文檔化
✅ 易於測試（Postman, Swagger UI）
✅ 易於部署（可獨立部署不同版本）
✅ 瀏覽器可直接訪問
```

### 版本演進範例

```java
// V1: 原始 API
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerV1 {

    @GetMapping("/{orderId}")
    public OrderResponseV1 getOrder(@PathVariable String orderId) {
        // V1 回應格式
        return OrderResponseV1.builder()
            .orderId(orderId)
            .totalAmt(order.getTotalAmt())
            .status(order.getStatus())  // 字串: "VALID"
            .build();
    }
}

// V2: 改善後的 API (Breaking Change)
@RestController
@RequestMapping("/api/v2/orders")
public class OrderControllerV2 {

    @GetMapping("/{orderId}")
    public OrderResponseV2 getOrder(@PathVariable String orderId) {
        // V2 回應格式 (結構變更)
        return OrderResponseV2.builder()
            .orderId(orderId)
            .pricing(PricingDetail.builder()
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .total(order.getTotalAmt())
                .build())
            .status(OrderStatus.builder()  // 物件: { "id": "4", "name": "有效" }
                .id(order.getStatus())
                .name(order.getStatusName())
                .build())
            .build();
    }
}
```

### 版本廢棄策略

```yaml
# 版本生命週期
Version Lifecycle:
  v1:
    released: 2023-01-01
    deprecated: 2025-01-01  # 宣告廢棄
    sunset: 2026-01-01      # 停止服務
    status: Active → Deprecated → Sunset

  v2:
    released: 2025-01-01
    status: Active

Deprecation Header (回應 Header):
  Deprecation: true
  Sunset: Sat, 01 Jan 2026 00:00:00 GMT
  Link: </api/v2/orders>; rel="successor-version"
```

---

## 分頁與排序

### 分頁設計

```
GET /api/v1/orders?page=0&size=20&sort=createTime,desc

Query Parameters:
├─ page: 頁碼 (從 0 開始)
├─ size: 每頁筆數 (預設 20, 最大 100)
└─ sort: 排序欄位,方向 (可多個，逗號分隔)

回應格式 (Page-based):
{
  "success": true,
  "data": {
    "content": [...],        # 資料陣列
    "page": 0,               # 當前頁碼
    "size": 20,              # 每頁筆數
    "total": 156,            # 總筆數
    "totalPages": 8,         # 總頁數
    "first": true,           # 是否第一頁
    "last": false            # 是否最後一頁
  }
}

回應格式 (Cursor-based):
{
  "success": true,
  "data": {
    "items": [...],
    "nextCursor": "eyJpZCI6MTIzfQ==",  # Base64 編碼的游標
    "hasMore": true
  }
}
```

### 分頁實作

```java
// OrderController.java
@GetMapping
public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String sort,
    @RequestParam(required = false) String status
) {
    // 驗證參數
    if (size > 100) {
        throw new ValidationException("每頁筆數不可超過 100");
    }

    // 解析排序參數
    Sort sortObj = parseSort(sort);  // "createTime,desc" → Sort.by(DESC, "createTime")

    // 建立分頁請求
    Pageable pageable = PageRequest.of(page, size, sortObj);

    // 查詢
    Page<Order> orders = orderService.getOrders(status, pageable);

    // 轉換為 DTO
    Page<OrderResponse> response = orders.map(OrderResponse::from);

    return ResponseEntity.ok(ApiResponse.success(response));
}

// 排序解析
private Sort parseSort(String sort) {
    if (StringUtils.isBlank(sort)) {
        return Sort.by(Sort.Direction.DESC, "createTime");  // 預設排序
    }

    String[] parts = sort.split(",");
    if (parts.length != 2) {
        throw new ValidationException("排序格式錯誤，應為: field,direction");
    }

    String field = parts[0];
    String direction = parts[1].toUpperCase();

    // 白名單驗證（防止 SQL Injection）
    List<String> allowedFields = List.of("createTime", "updateTime", "totalAmt", "orderId");
    if (!allowedFields.contains(field)) {
        throw new ValidationException("不支援的排序欄位: " + field);
    }

    return Sort.by(Sort.Direction.fromString(direction), field);
}
```

### 排序規範

```
單一欄位排序:
GET /api/v1/orders?sort=createTime,desc

多欄位排序:
GET /api/v1/orders?sort=status,asc&sort=createTime,desc

支援的排序方向:
├─ asc  (升序)
└─ desc (降序)

常見排序欄位:
├─ createTime (建立時間)
├─ updateTime (更新時間)
├─ totalAmt (金額)
└─ orderId (訂單編號)
```

---

## 安全性設計

### 認證與授權

```
認證流程:
1. 使用者登入 → POST /api/v1/auth/login
2. 伺服器驗證 → 產生 JWT Token
3. 回傳 Token → { "token": "eyJhbGc..." }
4. 後續請求帶 Token → Authorization: Bearer {token}
5. 伺服器驗證 Token → 允許存取

JWT Token 結構:
{
  "sub": "user123",          # 使用者 ID
  "roles": ["USER", "ADMIN"], # 角色
  "exp": 1698537600,         # 過期時間
  "iat": 1698451200          # 發行時間
}
```

### CORS 配置

```java
// SecurityConfig.java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // 允許的來源 (生產環境應設定實際域名)
    configuration.setAllowedOrigins(List.of(
        "http://localhost:4200",           // Angular 開發環境
        "https://som.example.com"          // 生產環境
    ));

    // 允許的 HTTP 方法
    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));

    // 允許的 Headers
    configuration.setAllowedHeaders(List.of("*"));

    // 暴露的 Headers (前端可讀取)
    configuration.setExposedHeaders(List.of(
        "Authorization",
        "X-Trace-Id",
        "X-RateLimit-Remaining"
    ));

    // 是否允許 Credentials (Cookies, Authorization Headers)
    configuration.setAllowCredentials(true);

    // Preflight 請求快取時間 (秒)
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 限流設計

```java
// RateLimitConfig.java
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiter apiRateLimiter() {
        // 每秒 10 個請求
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(500))
            .build();

        return RateLimiter.of("api", config);
    }
}

// RateLimitInterceptor.java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String clientId = getClientId(request);  // 從 JWT 取得使用者 ID

        try {
            // 嘗試取得許可
            rateLimiter.acquirePermission();

            // 在 Response Header 加入剩餘配額
            response.setHeader("X-RateLimit-Remaining",
                String.valueOf(rateLimiter.getMetrics().getAvailablePermissions()));

            return true;

        } catch (RequestNotPermitted e) {
            // 超過限流
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");  // 60 秒後重試

            ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                    .code("RATE_LIMIT_EXCEEDED")
                    .message("請求過於頻繁，請稍後再試")
                    .build())
                .build();

            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
            return false;
        }
    }
}
```

### 輸入驗證

```java
// OrderCreateRequest.java
public class OrderCreateRequest {

    @NotBlank(message = "會員卡號不可為空")
    @Pattern(regexp = "^[A-Z0-9]{10}$", message = "會員卡號格式錯誤")
    private String memberCardId;

    @NotEmpty(message = "至少需要一個商品")
    @Size(min = 1, max = 50, message = "商品數量需介於 1-50 之間")
    @Valid  // 驗證集合中的每個元素
    private List<SkuRequest> skus;

    @Size(max = 500, message = "備註最多 500 字")
    private String remark;
}

// SkuRequest.java
public class SkuRequest {

    @NotBlank(message = "商品編號不可為空")
    @Pattern(regexp = "^SKU[0-9]{6}$", message = "商品編號格式錯誤")
    private String skuNo;

    @Min(value = 1, message = "數量必須大於 0")
    @Max(value = 999, message = "數量不可超過 999")
    private Integer quantity;
}

// Controller 使用 @Valid
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
    @RequestBody @Valid OrderCreateRequest request  // ← @Valid 觸發驗證
) {
    Order order = orderService.createOrder(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(OrderResponse.from(order)));
}
```

---

## API 設計檢查清單

### 設計階段

- [ ] **資源命名**: 使用複數名詞、小寫、連字號
- [ ] **URL 結構**: 清晰的階層關係
- [ ] **HTTP 方法**: 正確使用 GET, POST, PUT, PATCH, DELETE
- [ ] **冪等性**: PUT, DELETE 需冪等
- [ ] **狀態碼**: 使用標準 HTTP 狀態碼
- [ ] **回應格式**: 統一的 JSON 格式
- [ ] **錯誤處理**: 明確的錯誤碼和訊息
- [ ] **版本控制**: URL 包含版本號 `/api/v1/`
- [ ] **分頁**: 支援分頁和排序
- [ ] **過濾**: 支援查詢參數過濾

### 安全性

- [ ] **認證**: JWT Token 認證
- [ ] **授權**: Role-based 權限控制
- [ ] **CORS**: 正確配置跨域
- [ ] **限流**: 防止濫用
- [ ] **輸入驗證**: 所有輸入都驗證
- [ ] **SQL Injection**: 使用參數化查詢
- [ ] **XSS**: 輸出編碼
- [ ] **CSRF**: API 通常不需要（使用 JWT）

### 文檔

- [ ] **OpenAPI Spec**: Swagger 文檔
- [ ] **範例**: 請求/回應範例
- [ ] **錯誤碼**: 錯誤碼說明
- [ ] **認證**: 認證方式說明
- [ ] **限制**: Rate Limit 說明

### 測試

- [ ] **單元測試**: Controller, Service 測試
- [ ] **整合測試**: API 端對端測試
- [ ] **效能測試**: 負載測試
- [ ] **安全測試**: 滲透測試

---

## 相關文檔

- [12-API-Order-Management.md](./12-API-Order-Management.md) - 訂單管理 API 詳細規格
- [13-API-Pricing-Service.md](./13-API-Pricing-Service.md) - 計價服務 API 詳細規格
- [14-API-Payment-Service.md](./14-API-Payment-Service.md) - 付款服務 API 詳細規格
- [15-API-Member-Service.md](./15-API-Member-Service.md) - 會員服務 API 詳細規格
- [34-Backend-Security-JWT.md](./34-Backend-Security-JWT.md) - JWT 安全實作
