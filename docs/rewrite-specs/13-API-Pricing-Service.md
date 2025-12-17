# 13. 計價服務 API (Pricing Service API)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **Base URL**: `https://api.example.com/pricing/v1`
- **相關文檔**:
  - [11-API-Design-Principles.md](./11-API-Design-Principles.md)
  - [04-Pricing-Calculation-Sequence.md](./04-Pricing-Calculation-Sequence.md)
  - [05-Pricing-Member-Discount-Logic.md](./05-Pricing-Member-Discount-Logic.md)

---

## 目錄
1. [API 總覽](#api-總覽)
2. [價格計算 API](#價格計算-api)
3. [折扣查詢 API](#折扣查詢-api)
4. [促銷活動 API](#促銷活動-api)
5. [資料模型](#資料模型)

---

## API 總覽

### 端點清單

| HTTP Method | 端點 | 說明 | 認證 |
|-------------|------|------|------|
| **價格計算** | | | |
| POST | `/api/v1/pricing/calculate` | 計算訂單價格 | ✅ |
| POST | `/api/v1/pricing/calculate-sku` | 計算單一商品價格 | ✅ |
| POST | `/api/v1/pricing/validate` | 驗證價格 | ✅ |
| **折扣查詢** | | | |
| GET | `/api/v1/pricing/discounts/{memberCardId}` | 查詢會員折扣 | ✅ |
| GET | `/api/v1/pricing/discounts/{memberCardId}/applicable` | 查詢可用折扣 | ✅ |
| **促銷活動** | | | |
| GET | `/api/v1/pricing/promotions` | 查詢促銷活動 | ✅ |
| GET | `/api/v1/pricing/promotions/{promotionId}` | 查詢單一促銷 | ✅ |
| GET | `/api/v1/pricing/promotions/active` | 查詢有效促銷 | ✅ |
| **計價歷史** | | | |
| GET | `/api/v1/pricing/history/{memberCardId}` | 查詢計價歷史 | ✅ |
| **快取管理** | | | |
| DELETE | `/api/v1/pricing/cache` | 清除計價快取 | ✅ (Admin) |
| DELETE | `/api/v1/pricing/cache/{memberCardId}` | 清除會員快取 | ✅ (Admin) |

---

## 價格計算 API

### 1. 計算訂單價格

計算完整訂單的價格，包含所有商品、折扣、促銷活動。

```http
POST /api/v1/pricing/calculate HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "channelId": "01",
  "skus": [
    {
      "skuNo": "SKU000001",
      "quantity": 2
    },
    {
      "skuNo": "SKU000002",
      "quantity": 1
    }
  ],
  "couponCode": "SUMMER2025"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "summary": {
      "originalTotal": 10000.00,
      "discountTotal": 500.00,
      "finalTotal": 9500.00,
      "taxAmount": 450.00,
      "grandTotal": 9950.00
    },
    "computes": [
      {
        "type": "1",
        "typeName": "商品小計",
        "amount": 8000.00,
        "description": "一般商品總計"
      },
      {
        "type": "2",
        "typeName": "安裝小計",
        "amount": 2000.00,
        "description": "安裝服務總計"
      },
      {
        "type": "4",
        "typeName": "會員卡折扣",
        "amount": -500.00,
        "description": "VIP 會員 95 折"
      }
    ],
    "items": [
      {
        "skuNo": "SKU000001",
        "skuName": "三門冰箱",
        "quantity": 2,
        "originalPrice": 3500.00,
        "discountPrice": 3400.00,
        "discountAmount": 100.00,
        "subtotal": 6800.00,
        "discountDetails": [
          {
            "type": "MEMBER_DISCOUNT",
            "typeName": "會員折扣",
            "discountType": "0",
            "discountTypeName": "折扣率",
            "rate": 0.05,
            "amount": 100.00,
            "description": "VIP 會員 95 折"
          }
        ]
      },
      {
        "skuNo": "SKU000002",
        "skuName": "安裝服務",
        "quantity": 1,
        "originalPrice": 2000.00,
        "discountPrice": 2000.00,
        "discountAmount": 0.00,
        "subtotal": 2000.00,
        "discountDetails": []
      }
    ],
    "appliedPromotions": [],
    "appliedCoupons": [],
    "calculationTime": 420,
    "cacheHit": false,
    "timestamp": "2025-10-27T10:30:00Z"
  },
  "traceId": "abc-123-def-456"
}
```

**計算步驟說明**:
```
12 步驟計價流程:
1️⃣ 還原 SKU 金額 (50ms)
2️⃣ 工種分攤 (100ms)
3️⃣ 商品分類 (30ms)
4️⃣ 設定序號 (20ms) ⚡ 平行執行
5️⃣ 計算免費安裝 (40ms) ⚡ 平行執行
6️⃣ 成本加成折扣 Type 2 (200ms) - 優先順序 1
7️⃣ 多重促銷 (500ms → 50ms with cache) - 優先順序 2
8️⃣ 折扣率 Type 0 (200ms) - 優先順序 3
9️⃣ 固定折扣 Type 1 (200ms) - 優先順序 4
🔟 特殊會員折扣 (150ms) - 條件式
1️⃣1️⃣ 計算總折扣 (10ms)
1️⃣2️⃣ 生成 6 個 ComputeType (60ms → 10ms with parallel)

總耗時: 1560ms → 420ms (快取命中時)
```

**錯誤回應 400 Bad Request**:
```json
{
  "success": false,
  "error": {
    "code": "PRICING_VALIDATION_ERROR",
    "message": "計價參數驗證失敗",
    "validationErrors": [
      {
        "field": "memberCardId",
        "message": "會員卡號不可為空"
      },
      {
        "field": "skus[0].quantity",
        "message": "商品數量必須大於 0"
      }
    ]
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/pricing/calculate"
}
```

**錯誤回應 404 Not Found**:
```json
{
  "success": false,
  "error": {
    "code": "PRICING_MEMBER_NOT_FOUND",
    "message": "找不到會員資料",
    "details": "會員卡號 A123456789 不存在或已過期"
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/pricing/calculate"
}
```

---

### 2. 計算單一商品價格

快速計算單一商品的價格，用於商品選擇時的即時計價。

```http
POST /api/v1/pricing/calculate-sku HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "skuNo": "SKU000001",
  "quantity": 2,
  "channelId": "01"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "skuNo": "SKU000001",
    "skuName": "三門冰箱",
    "quantity": 2,
    "pricing": {
      "originalPrice": 3500.00,
      "discountPrice": 3400.00,
      "discountAmount": 100.00,
      "subtotal": 6800.00
    },
    "discounts": [
      {
        "type": "MEMBER_DISCOUNT",
        "typeName": "會員折扣",
        "discountType": "0",
        "discountTypeName": "折扣率",
        "rate": 0.05,
        "amount": 100.00,
        "description": "VIP 會員 95 折"
      }
    ],
    "calculationTime": 50,
    "cacheHit": true,
    "timestamp": "2025-10-27T10:35:00Z"
  },
  "traceId": "abc-123-def-456"
}
```

---

### 3. 驗證價格

驗證前端計算的價格是否正確（防止前端篡改）。

```http
POST /api/v1/pricing/validate HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "channelId": "01",
  "skus": [
    {
      "skuNo": "SKU000001",
      "quantity": 2,
      "subtotal": 6800.00
    }
  ],
  "totalAmount": 9500.00
}
```

**回應 200 OK** (驗證通過):
```json
{
  "success": true,
  "data": {
    "valid": true,
    "expectedTotal": 9500.00,
    "actualTotal": 9500.00,
    "difference": 0.00,
    "message": "價格驗證通過"
  },
  "timestamp": "2025-10-27T10:40:00Z",
  "traceId": "abc-123-def-456"
}
```

**回應 200 OK** (驗證失敗):
```json
{
  "success": true,
  "data": {
    "valid": false,
    "expectedTotal": 9500.00,
    "actualTotal": 9000.00,
    "difference": 500.00,
    "message": "價格驗證失敗，請重新計算",
    "details": [
      {
        "skuNo": "SKU000001",
        "expected": 6800.00,
        "actual": 6500.00,
        "difference": 300.00
      }
    ]
  },
  "timestamp": "2025-10-27T10:40:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 折扣查詢 API

### 1. 查詢會員折扣

```http
GET /api/v1/pricing/discounts/A123456789 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "memberName": "王小明",
    "memberLevel": "VIP",
    "discounts": [
      {
        "discountId": "1",
        "discountType": "0",
        "discountTypeName": "折扣率 (Discounting)",
        "priority": 3,
        "rate": 0.05,
        "description": "全商品 95 折",
        "validFrom": "2025-01-01",
        "validUntil": "2025-12-31",
        "status": "ACTIVE"
      },
      {
        "discountId": "2",
        "discountType": "2",
        "discountTypeName": "成本加成 (Cost Markup)",
        "priority": 1,
        "markupRate": 1.2,
        "applicableCategories": ["家電"],
        "description": "家電類商品成本 ×1.2",
        "validFrom": "2025-01-01",
        "validUntil": "2025-12-31",
        "status": "ACTIVE"
      }
    ]
  },
  "timestamp": "2025-10-27T11:00:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 查詢可用折扣

查詢會員對特定商品清單可用的折扣。

```http
GET /api/v1/pricing/discounts/A123456789/applicable?skus=SKU000001,SKU000002 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "applicableDiscounts": [
      {
        "discountId": "2",
        "discountType": "2",
        "discountTypeName": "成本加成",
        "priority": 1,
        "applicableSkus": ["SKU000001"],
        "estimatedSavings": 200.00,
        "description": "家電類商品成本加成 1.2 倍"
      },
      {
        "discountId": "1",
        "discountType": "0",
        "discountTypeName": "折扣率",
        "priority": 3,
        "applicableSkus": ["SKU000001", "SKU000002"],
        "estimatedSavings": 500.00,
        "description": "全商品 95 折"
      }
    ],
    "bestDiscount": {
      "discountId": "2",
      "estimatedSavings": 200.00
    }
  },
  "timestamp": "2025-10-27T11:05:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 促銷活動 API

### 1. 查詢促銷活動列表

```http
GET /api/v1/pricing/promotions?active=true&page=0&size=20 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**Query Parameters**:
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| active | boolean | ❌ | 只查詢有效促銷 (預設 true) |
| category | string | ❌ | 商品分類 |
| page | int | ❌ | 頁碼 |
| size | int | ❌ | 每頁筆數 |

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "promotionId": "PROMO001",
        "promotionName": "夏季家電特賣",
        "promotionType": "PERCENTAGE_OFF",
        "discountRate": 0.20,
        "applicableCategories": ["家電"],
        "minPurchaseAmount": 5000.00,
        "startDate": "2025-06-01",
        "endDate": "2025-08-31",
        "status": "ACTIVE",
        "description": "家電類商品滿 5000 元打 8 折"
      },
      {
        "promotionId": "PROMO002",
        "promotionName": "買 2 送 1",
        "promotionType": "BUY_X_GET_Y",
        "buyQuantity": 2,
        "getQuantity": 1,
        "applicableSkus": ["SKU000001", "SKU000003"],
        "startDate": "2025-07-01",
        "endDate": "2025-07-31",
        "status": "ACTIVE",
        "description": "指定商品買 2 送 1"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 15
  },
  "timestamp": "2025-10-27T11:10:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 查詢單一促銷活動

```http
GET /api/v1/pricing/promotions/PROMO001 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "promotionId": "PROMO001",
    "promotionName": "夏季家電特賣",
    "promotionType": "PERCENTAGE_OFF",
    "discountRate": 0.20,
    "applicableCategories": ["家電"],
    "excludedSkus": ["SKU000099"],
    "minPurchaseAmount": 5000.00,
    "maxDiscountAmount": 2000.00,
    "startDate": "2025-06-01T00:00:00Z",
    "endDate": "2025-08-31T23:59:59Z",
    "status": "ACTIVE",
    "description": "家電類商品滿 5000 元打 8 折，最高折扣 2000 元",
    "terms": [
      "僅限家電類商品",
      "單筆訂單滿 5000 元可享折扣",
      "與其他促銷活動擇優使用",
      "最高折扣金額 2000 元"
    ],
    "createdTime": "2025-05-15T10:00:00Z",
    "updatedTime": "2025-06-01T00:00:00Z"
  },
  "timestamp": "2025-10-27T11:15:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 查詢有效促銷活動

查詢當前有效的促銷活動，用於前端展示。

```http
GET /api/v1/pricing/promotions/active HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "promotionId": "PROMO001",
      "promotionName": "夏季家電特賣",
      "discountRate": 0.20,
      "badge": "8折",
      "endDate": "2025-08-31",
      "daysRemaining": 35
    },
    {
      "promotionId": "PROMO002",
      "promotionName": "買 2 送 1",
      "badge": "買2送1",
      "endDate": "2025-07-31",
      "daysRemaining": 4
    }
  ],
  "timestamp": "2025-10-27T11:20:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 計價歷史 API

### 查詢計價歷史

```http
GET /api/v1/pricing/history/A123456789?page=0&size=20 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "historyId": "1",
        "memberCardId": "A123456789",
        "calculateTime": "2025-10-27T10:30:00Z",
        "skuCount": 2,
        "originalTotal": 10000.00,
        "discountTotal": 500.00,
        "finalTotal": 9500.00,
        "appliedDiscounts": ["會員折扣 95 折"],
        "calculationDuration": 420,
        "cacheHit": false
      },
      {
        "historyId": "2",
        "memberCardId": "A123456789",
        "calculateTime": "2025-10-26T14:20:00Z",
        "skuCount": 1,
        "originalTotal": 5000.00,
        "discountTotal": 250.00,
        "finalTotal": 4750.00,
        "appliedDiscounts": ["會員折扣 95 折"],
        "calculationDuration": 50,
        "cacheHit": true
      }
    ],
    "page": 0,
    "size": 20,
    "total": 45
  },
  "timestamp": "2025-10-27T11:25:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 快取管理 API

### 1. 清除計價快取 (需 Admin 權限)

```http
DELETE /api/v1/pricing/cache HTTP/1.1
Host: api.example.com
Authorization: Bearer {admin_jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "clearedKeys": 1256,
    "message": "已清除所有計價快取"
  },
  "timestamp": "2025-10-27T11:30:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 清除會員快取 (需 Admin 權限)

```http
DELETE /api/v1/pricing/cache/A123456789 HTTP/1.1
Host: api.example.com
Authorization: Bearer {admin_jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "clearedKeys": 15,
    "message": "已清除會員 A123456789 的計價快取"
  },
  "timestamp": "2025-10-27T11:35:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 資料模型

### PricingRequest (計價請求)

```typescript
interface PricingRequest {
  memberCardId: string;          // 會員卡號 (必填)
  channelId: string;              // 通路代碼 (必填)
  skus: SkuPricingRequest[];      // 商品清單 (必填)
  couponCode?: string;            // 優惠券代碼
  usePoints?: number;             // 使用點數
}

interface SkuPricingRequest {
  skuNo: string;                  // 商品編號 (必填)
  quantity: number;               // 數量 (必填, > 0)
}
```

### PricingResponse (計價回應)

```typescript
interface PricingResponse {
  summary: PricingSummary;        // 價格總覽
  computes: ComputeType[];        // 計價明細 (6 種類型)
  items: PricedItem[];            // 商品明細
  appliedPromotions: AppliedPromotion[];  // 已套用的促銷
  appliedCoupons: AppliedCoupon[];        // 已套用的優惠券
  calculationTime: number;        // 計算耗時 (毫秒)
  cacheHit: boolean;              // 是否快取命中
  timestamp: string;              // 時間戳記
}

interface PricingSummary {
  originalTotal: number;          // 原始總價
  discountTotal: number;          // 折扣總額
  finalTotal: number;             // 最終總價
  taxAmount: number;              // 稅額
  grandTotal: number;             // 含稅總價
}

interface PricedItem {
  skuNo: string;                  // 商品編號
  skuName: string;                // 商品名稱
  quantity: number;               // 數量
  originalPrice: number;          // 原價
  discountPrice: number;          // 折扣價
  discountAmount: number;         // 折扣金額
  subtotal: number;               // 小計
  discountDetails: DiscountDetail[];  // 折扣明細
}

interface DiscountDetail {
  type: DiscountSourceType;       // 折扣來源類型
  typeName: string;               // 類型名稱
  discountType?: string;          // 會員折扣類型 (0, 1, 2)
  discountTypeName?: string;      // 折扣類型名稱
  rate?: number;                  // 折扣率 (Type 0)
  amount: number;                 // 折扣金額
  description: string;            // 說明
}

enum DiscountSourceType {
  MEMBER_DISCOUNT = 'MEMBER_DISCOUNT',    // 會員折扣
  PROMOTION = 'PROMOTION',                // 促銷活動
  COUPON = 'COUPON',                      // 優惠券
  POINTS = 'POINTS'                       // 點數抵扣
}

interface ComputeType {
  type: string;                   // 類型 ID (1-6)
  typeName: string;               // 類型名稱
  amount: number;                 // 金額
  description: string;            // 說明
}
```

### 6 種 ComputeType 說明

| Type | 名稱 | 說明 |
|------|------|------|
| 1 | 商品小計 | 一般商品（非安裝、運送）的總價 |
| 2 | 安裝小計 | 安裝服務的總價 |
| 3 | 運送小計 | 運送服務的總價 |
| 4 | 會員卡折扣 | 會員折扣金額（負值） |
| 5 | 直送費用 | 直送額外費用 |
| 6 | 折價券折扣 | 折價券折扣金額（負值） |

---

## 錯誤碼

| 錯誤碼 | HTTP 狀態碼 | 說明 |
|--------|------------|------|
| PRICING_VALIDATION_ERROR | 400 | 計價參數驗證失敗 |
| PRICING_MEMBER_NOT_FOUND | 404 | 找不到會員資料 |
| PRICING_INVALID_SKU | 400 | 無效的商品編號 |
| PRICING_CALCULATION_FAILED | 500 | 價格計算失敗 |
| PRICING_DISCOUNT_NOT_FOUND | 404 | 找不到折扣資料 |
| PRICING_DISCOUNT_EXPIRED | 422 | 折扣已過期 |
| PRICING_PROMOTION_NOT_FOUND | 404 | 找不到促銷活動 |
| PRICING_COUPON_INVALID | 422 | 優惠券無效 |
| PRICING_COUPON_EXPIRED | 422 | 優惠券已過期 |
| PRICING_CACHE_ERROR | 500 | 快取操作失敗 |

---

## 效能指標

### 計價 API 效能目標

| 指標 | 目標值 | 當前值 |
|------|--------|--------|
| **平均回應時間** | < 500ms | 350ms ✅ |
| **P95 回應時間** | < 1000ms | 800ms ✅ |
| **P99 回應時間** | < 1500ms | 1200ms ✅ |
| **快取命中率** | > 90% | 95% ✅ |
| **吞吐量** | > 100 req/s | 180 req/s ✅ |

### 快取策略

```yaml
快取配置:
  pricing:
    key: "pricing:{memberCardId}:{skuHash}:{channelId}"
    ttl: 5 分鐘
    命中率: 95%
    節省時間: 1200ms → 50ms (-96%)

  member-discount:
    key: "member-discount:{memberCardId}:{discType}"
    ttl: 30 分鐘
    命中率: 98%
    節省時間: 200ms → 5ms (-97.5%)

  promotion:
    key: "promotion:active"
    ttl: 10 分鐘
    命中率: 99%
    節省時間: 500ms → 5ms (-99%)
```

---

## 相關文檔

- [04-Pricing-Calculation-Sequence.md](./04-Pricing-Calculation-Sequence.md) - 計價計算順序
- [05-Pricing-Member-Discount-Logic.md](./05-Pricing-Member-Discount-Logic.md) - 會員折扣邏輯
- [07-Pricing-Optimization-Strategy.md](./07-Pricing-Optimization-Strategy.md) - 計價優化策略
- [36-Backend-Pricing-Service.md](./36-Backend-Pricing-Service.md) - 計價服務實作
