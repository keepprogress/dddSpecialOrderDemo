# 12. 訂單管理 API (Order Management API)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **Base URL**: `https://api.example.com/orders/v1`
- **相關文檔**:
  - [11-API-Design-Principles.md](./11-API-Design-Principles.md)
  - [02-Order-Creation-Flow.md](./02-Order-Creation-Flow.md)
  - [01-Order-Status-Lifecycle.md](./01-Order-Status-Lifecycle.md)

---

## 目錄
1. [API 總覽](#api-總覽)
2. [訂單 CRUD API](#訂單-crud-api)
3. [訂單狀態管理 API](#訂單狀態管理-api)
4. [訂單查詢 API](#訂單查詢-api)
5. [訂單項目管理 API](#訂單項目管理-api)
6. [資料模型](#資料模型)

---

## API 總覽

### 端點清單

| HTTP Method | 端點 | 說明 | 認證 |
|-------------|------|------|------|
| **訂單 CRUD** | | | |
| POST | `/api/v1/orders` | 建立訂單 | ✅ |
| GET | `/api/v1/orders/{orderId}` | 查詢單一訂單 | ✅ |
| GET | `/api/v1/orders` | 查詢訂單列表 | ✅ |
| PUT | `/api/v1/orders/{orderId}` | 更新訂單 | ✅ |
| DELETE | `/api/v1/orders/{orderId}` | 刪除訂單 (軟刪除) | ✅ |
| **訂單狀態** | | | |
| PATCH | `/api/v1/orders/{orderId}/status` | 更新訂單狀態 | ✅ |
| POST | `/api/v1/orders/{orderId}/confirm` | 確認訂單 | ✅ |
| POST | `/api/v1/orders/{orderId}/cancel` | 取消訂單 | ✅ |
| POST | `/api/v1/orders/{orderId}/close` | 結案訂單 | ✅ |
| **訂單項目** | | | |
| GET | `/api/v1/orders/{orderId}/items` | 查詢訂單項目 | ✅ |
| POST | `/api/v1/orders/{orderId}/items` | 新增項目 | ✅ |
| PUT | `/api/v1/orders/{orderId}/items/{itemId}` | 更新項目 | ✅ |
| DELETE | `/api/v1/orders/{orderId}/items/{itemId}` | 刪除項目 | ✅ |
| **查詢** | | | |
| GET | `/api/v1/orders/search` | 進階搜尋 | ✅ |
| GET | `/api/v1/orders/count` | 統計數量 | ✅ |
| GET | `/api/v1/orders/export` | 匯出訂單 | ✅ |

---

## 訂單 CRUD API

### 1. 建立訂單

```http
POST /api/v1/orders HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "channelId": "01",
  "deliveryType": "HOME_DELIVERY",
  "deliveryAddress": {
    "recipientName": "王小明",
    "phone": "0912345678",
    "city": "台北市",
    "district": "信義區",
    "address": "信義路五段7號",
    "zipCode": "110"
  },
  "items": [
    {
      "skuNo": "SKU000001",
      "quantity": 2,
      "remark": "顏色: 白色"
    },
    {
      "skuNo": "SKU000002",
      "quantity": 1
    }
  ],
  "installDate": "2025-11-15",
  "remark": "請在下午送達"
}
```

**回應 201 Created**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "orderNo": "A1234567890",
    "memberCardId": "A123456789",
    "memberName": "王小明",
    "channelId": "01",
    "channelName": "門市",
    "status": {
      "id": "1",
      "name": "草稿"
    },
    "pricing": {
      "subtotal": 10000.00,
      "discount": 500.00,
      "total": 9500.00,
      "tax": 450.00,
      "computes": [
        {
          "type": "1",
          "typeName": "商品小計",
          "amount": 8000.00
        },
        {
          "type": "2",
          "typeName": "安裝小計",
          "amount": 2000.00
        },
        {
          "type": "4",
          "typeName": "會員卡折扣",
          "amount": -500.00
        }
      ]
    },
    "deliveryType": "HOME_DELIVERY",
    "deliveryAddress": {
      "recipientName": "王小明",
      "phone": "0912345678",
      "fullAddress": "110 台北市信義區信義路五段7號"
    },
    "items": [
      {
        "itemId": "1",
        "skuNo": "SKU000001",
        "skuName": "三門冰箱",
        "quantity": 2,
        "unitPrice": 3500.00,
        "discountAmt": 200.00,
        "subtotal": 6800.00
      },
      {
        "itemId": "2",
        "skuNo": "SKU000002",
        "skuName": "安裝服務",
        "quantity": 1,
        "unitPrice": 2000.00,
        "discountAmt": 0.00,
        "subtotal": 2000.00
      }
    ],
    "installDate": "2025-11-15",
    "remark": "請在下午送達",
    "createTime": "2025-10-27T10:30:00Z",
    "createBy": "user123",
    "updateTime": "2025-10-27T10:30:00Z"
  },
  "message": "訂單建立成功",
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 400 Bad Request**:
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "請求參數驗證失敗",
    "validationErrors": [
      {
        "field": "memberCardId",
        "message": "會員卡號格式錯誤",
        "rejectedValue": "123"
      },
      {
        "field": "items[0].quantity",
        "message": "商品數量必須大於 0",
        "rejectedValue": 0
      }
    ]
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders"
}
```

---

### 2. 查詢單一訂單

```http
GET /api/v1/orders/SO20251027001 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "orderNo": "A1234567890",
    "memberCardId": "A123456789",
    "memberName": "王小明",
    "status": {
      "id": "4",
      "name": "有效"
    },
    "pricing": {
      "subtotal": 10000.00,
      "discount": 500.00,
      "total": 9500.00
    },
    "items": [...],
    "timeline": [
      {
        "status": "1",
        "statusName": "草稿",
        "timestamp": "2025-10-27T10:30:00Z",
        "operator": "user123"
      },
      {
        "status": "4",
        "statusName": "有效",
        "timestamp": "2025-10-27T10:35:00Z",
        "operator": "user123"
      }
    ],
    "createTime": "2025-10-27T10:30:00Z",
    "updateTime": "2025-10-27T10:35:00Z"
  },
  "timestamp": "2025-10-27T10:40:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 404 Not Found**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "找不到訂單",
    "details": "訂單編號 SO20251027999 不存在"
  },
  "timestamp": "2025-10-27T10:40:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027999"
}
```

---

### 3. 查詢訂單列表

```http
GET /api/v1/orders?status=VALID&page=0&size=20&sort=createTime,desc HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**Query Parameters**:
| 參數 | 類型 | 必填 | 說明 | 範例 |
|------|------|------|------|------|
| status | string | ❌ | 訂單狀態 | DRAFTS, VALID, PAID, CLOSED |
| memberCardId | string | ❌ | 會員卡號 | A123456789 |
| startDate | date | ❌ | 開始日期 | 2025-10-01 |
| endDate | date | ❌ | 結束日期 | 2025-10-31 |
| keyword | string | ❌ | 關鍵字搜尋 | 訂單編號、會員姓名 |
| page | int | ❌ | 頁碼 (從 0 開始) | 0 |
| size | int | ❌ | 每頁筆數 (預設 20, 最大 100) | 20 |
| sort | string | ❌ | 排序 (欄位,方向) | createTime,desc |

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": "SO20251027001",
        "orderNo": "A1234567890",
        "memberCardId": "A123456789",
        "memberName": "王小明",
        "status": {
          "id": "4",
          "name": "有效"
        },
        "totalAmt": 9500.00,
        "createTime": "2025-10-27T10:30:00Z"
      },
      {
        "orderId": "SO20251027002",
        "orderNo": "A1234567891",
        "memberCardId": "B987654321",
        "memberName": "李小華",
        "status": {
          "id": "3",
          "name": "已付款"
        },
        "totalAmt": 15800.00,
        "createTime": "2025-10-27T09:15:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "total": 156,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2025-10-27T10:45:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 4. 更新訂單

```http
PUT /api/v1/orders/SO20251027001 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789",
  "deliveryAddress": {
    "recipientName": "王小明",
    "phone": "0912345678",
    "city": "台北市",
    "district": "大安區",
    "address": "敦化南路二段105號",
    "zipCode": "106"
  },
  "items": [
    {
      "skuNo": "SKU000001",
      "quantity": 3
    }
  ],
  "installDate": "2025-11-20",
  "remark": "改為早上送達"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": {
      "id": "4",
      "name": "有效"
    },
    "pricing": {
      "total": 14250.00
    },
    "updateTime": "2025-10-27T11:00:00Z"
  },
  "message": "訂單更新成功",
  "timestamp": "2025-10-27T11:00:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 409 Conflict**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_ALREADY_PAID",
    "message": "訂單已付款，無法修改",
    "details": "訂單狀態: 已付款"
  },
  "timestamp": "2025-10-27T11:00:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001"
}
```

---

### 5. 刪除訂單 (軟刪除)

```http
DELETE /api/v1/orders/SO20251027001 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 204 No Content** (無 Body)

**錯誤回應 409 Conflict**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_CANNOT_DELETE",
    "message": "訂單無法刪除",
    "details": "已付款的訂單不可刪除"
  },
  "timestamp": "2025-10-27T11:10:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001"
}
```

---

## 訂單狀態管理 API

### 訂單狀態流轉

```
狀態流轉圖:
草稿(1) ──> 報價(2) ──> 有效(4) ──> 已付款(3) ──> 已結案(5)
  │            │           │
  └────────────┴───────────┴──────> 作廢(6)
```

### 1. 更新訂單狀態

```http
PATCH /api/v1/orders/SO20251027001/status HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "status": "VALID",
  "remark": "客戶已確認訂單"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": {
      "id": "4",
      "name": "有效"
    },
    "updateTime": "2025-10-27T11:20:00Z"
  },
  "message": "訂單狀態更新成功",
  "timestamp": "2025-10-27T11:20:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 422 Unprocessable Entity**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_INVALID_STATUS_TRANSITION",
    "message": "不允許的狀態變更",
    "details": "無法從「已付款」變更為「草稿」"
  },
  "timestamp": "2025-10-27T11:20:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001/status"
}
```

---

### 2. 確認訂單

```http
POST /api/v1/orders/SO20251027001/confirm HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "remark": "客戶已確認商品和價格"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": {
      "id": "4",
      "name": "有效"
    },
    "updateTime": "2025-10-27T11:25:00Z"
  },
  "message": "訂單確認成功",
  "timestamp": "2025-10-27T11:25:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 取消訂單

```http
POST /api/v1/orders/SO20251027001/cancel HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "reason": "客戶要求取消",
  "remark": "客戶改變心意"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": {
      "id": "6",
      "name": "作廢"
    },
    "cancelReason": "客戶要求取消",
    "updateTime": "2025-10-27T11:30:00Z"
  },
  "message": "訂單已取消",
  "timestamp": "2025-10-27T11:30:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 409 Conflict**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_ALREADY_PAID",
    "message": "訂單已付款，無法取消",
    "details": "已付款的訂單請聯絡客服處理退款"
  },
  "timestamp": "2025-10-27T11:30:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/orders/SO20251027001/cancel"
}
```

---

### 4. 結案訂單

```http
POST /api/v1/orders/SO20251027001/close HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "remark": "商品已送達並安裝完成"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "orderId": "SO20251027001",
    "status": {
      "id": "5",
      "name": "已結案"
    },
    "closeTime": "2025-11-20T16:00:00Z",
    "updateTime": "2025-11-20T16:00:00Z"
  },
  "message": "訂單已結案",
  "timestamp": "2025-11-20T16:00:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 訂單查詢 API

### 1. 進階搜尋

```http
POST /api/v1/orders/search HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "criteria": {
    "statusIds": ["4", "3"],
    "memberCardId": "A123456789",
    "dateRange": {
      "startDate": "2025-10-01",
      "endDate": "2025-10-31"
    },
    "amountRange": {
      "min": 5000,
      "max": 20000
    },
    "keyword": "冰箱"
  },
  "page": 0,
  "size": 20,
  "sort": [
    {
      "field": "createTime",
      "direction": "DESC"
    }
  ]
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "total": 45
  },
  "timestamp": "2025-10-27T12:00:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 統計數量

```http
GET /api/v1/orders/count?status=VALID HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "total": 1256,
    "byStatus": {
      "DRAFTS": 45,
      "QUOTE": 23,
      "VALID": 678,
      "PAID": 450,
      "CLOSED": 58,
      "CANCELLED": 2
    }
  },
  "timestamp": "2025-10-27T12:05:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 匯出訂單

```http
GET /api/v1/orders/export?status=VALID&format=xlsx HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**Query Parameters**:
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| status | string | ❌ | 訂單狀態 |
| startDate | date | ❌ | 開始日期 |
| endDate | date | ❌ | 結束日期 |
| format | string | ❌ | 匯出格式 (xlsx, csv, pdf) 預設 xlsx |

**回應 200 OK**:
```
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="orders_20251027.xlsx"

[Binary Excel File]
```

---

## 訂單項目管理 API

### 1. 查詢訂單項目

```http
GET /api/v1/orders/SO20251027001/items HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "itemId": "1",
      "skuNo": "SKU000001",
      "skuName": "三門冰箱",
      "category": "家電",
      "quantity": 2,
      "unitPrice": 3500.00,
      "discountAmt": 200.00,
      "subtotal": 6800.00,
      "remark": "顏色: 白色"
    },
    {
      "itemId": "2",
      "skuNo": "SKU000002",
      "skuName": "安裝服務",
      "category": "服務",
      "quantity": 1,
      "unitPrice": 2000.00,
      "discountAmt": 0.00,
      "subtotal": 2000.00
    }
  ],
  "timestamp": "2025-10-27T12:10:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 新增訂單項目

```http
POST /api/v1/orders/SO20251027001/items HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "skuNo": "SKU000003",
  "quantity": 1,
  "remark": "加購配件"
}
```

**回應 201 Created**:
```json
{
  "success": true,
  "data": {
    "itemId": "3",
    "skuNo": "SKU000003",
    "skuName": "延長保固",
    "quantity": 1,
    "unitPrice": 1500.00,
    "subtotal": 1500.00
  },
  "message": "項目新增成功",
  "timestamp": "2025-10-27T12:15:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 更新訂單項目

```http
PUT /api/v1/orders/SO20251027001/items/1 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "quantity": 3,
  "remark": "改為 3 台"
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "itemId": "1",
    "quantity": 3,
    "unitPrice": 3500.00,
    "subtotal": 10500.00,
    "updateTime": "2025-10-27T12:20:00Z"
  },
  "message": "項目更新成功",
  "timestamp": "2025-10-27T12:20:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 4. 刪除訂單項目

```http
DELETE /api/v1/orders/SO20251027001/items/3 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 204 No Content**

---

## 資料模型

### OrderRequest (建立/更新訂單)

```typescript
interface OrderRequest {
  memberCardId: string;        // 會員卡號 (必填)
  channelId: string;            // 通路代碼 (必填)
  deliveryType: DeliveryType;   // 配送方式
  deliveryAddress?: Address;    // 配送地址
  items: OrderItemRequest[];    // 訂單項目 (必填)
  installDate?: string;         // 安裝日期 (YYYY-MM-DD)
  remark?: string;              // 備註
}

interface OrderItemRequest {
  skuNo: string;                // 商品編號 (必填)
  quantity: number;             // 數量 (必填, > 0)
  remark?: string;              // 備註
}

interface Address {
  recipientName: string;        // 收件人姓名
  phone: string;                // 電話
  city: string;                 // 城市
  district: string;             // 行政區
  address: string;              // 詳細地址
  zipCode: string;              // 郵遞區號
}

enum DeliveryType {
  HOME_DELIVERY = 'HOME_DELIVERY',  // 宅配
  STORE_PICKUP = 'STORE_PICKUP',    // 門市自取
  DIRECT_DELIVERY = 'DIRECT_DELIVERY' // 直送
}
```

### OrderResponse (訂單回應)

```typescript
interface OrderResponse {
  orderId: string;              // 訂單 ID
  orderNo: string;              // 訂單編號
  memberCardId: string;         // 會員卡號
  memberName: string;           // 會員姓名
  channelId: string;            // 通路代碼
  channelName: string;          // 通路名稱
  status: OrderStatus;          // 訂單狀態
  pricing: PricingDetail;       // 計價明細
  deliveryType: DeliveryType;   // 配送方式
  deliveryAddress?: Address;    // 配送地址
  items: OrderItem[];           // 訂單項目
  installDate?: string;         // 安裝日期
  remark?: string;              // 備註
  timeline?: StatusTimeline[];  // 狀態時間軸
  createTime: string;           // 建立時間 (ISO 8601)
  createBy: string;             // 建立人
  updateTime: string;           // 更新時間 (ISO 8601)
  updateBy?: string;            // 更新人
}

interface OrderStatus {
  id: string;                   // 狀態 ID (1-6)
  name: string;                 // 狀態名稱
}

interface PricingDetail {
  subtotal: number;             // 小計
  discount: number;             // 折扣總額
  total: number;                // 總計
  tax: number;                  // 稅額
  computes: ComputeType[];      // 計價明細
}

interface ComputeType {
  type: string;                 // 類型 ID (1-6)
  typeName: string;             // 類型名稱
  amount: number;               // 金額
}

interface OrderItem {
  itemId: string;               // 項目 ID
  skuNo: string;                // 商品編號
  skuName: string;              // 商品名稱
  category: string;             // 商品分類
  quantity: number;             // 數量
  unitPrice: number;            // 單價
  discountAmt: number;          // 折扣金額
  subtotal: number;             // 小計
  remark?: string;              // 備註
}

interface StatusTimeline {
  status: string;               // 狀態 ID
  statusName: string;           // 狀態名稱
  timestamp: string;            // 時間戳記
  operator: string;             // 操作人
  remark?: string;              // 備註
}
```

---

## 錯誤碼

| 錯誤碼 | HTTP 狀態碼 | 說明 |
|--------|------------|------|
| ORDER_NOT_FOUND | 404 | 訂單不存在 |
| ORDER_ALREADY_PAID | 409 | 訂單已付款 |
| ORDER_ALREADY_CANCELLED | 409 | 訂單已取消 |
| ORDER_INVALID_STATUS | 422 | 無效的訂單狀態 |
| ORDER_INVALID_STATUS_TRANSITION | 422 | 不允許的狀態變更 |
| ORDER_CANNOT_DELETE | 409 | 訂單無法刪除 |
| ORDER_CANNOT_UPDATE | 409 | 訂單無法修改 |
| ORDER_CREATION_FAILED | 500 | 訂單建立失敗 |
| INVALID_SKU | 400 | 無效的商品編號 |
| INVALID_MEMBER_CARD | 400 | 無效的會員卡號 |
| INSUFFICIENT_STOCK | 422 | 庫存不足 |

---

## 相關文檔

- [11-API-Design-Principles.md](./11-API-Design-Principles.md) - API 設計原則
- [13-API-Pricing-Service.md](./13-API-Pricing-Service.md) - 計價服務 API
- [02-Order-Creation-Flow.md](./02-Order-Creation-Flow.md) - 訂單建立流程
- [01-Order-Status-Lifecycle.md](./01-Order-Status-Lifecycle.md) - 訂單狀態生命週期
