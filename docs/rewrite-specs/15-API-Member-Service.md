# 15. 會員服務 API (Member Service API)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **Base URL**: `https://api.example.com/member/v1`
- **相關文檔**:
  - [11-API-Design-Principles.md](./11-API-Design-Principles.md)
  - [05-Pricing-Member-Discount-Logic.md](./05-Pricing-Member-Discount-Logic.md)
  - [37-Backend-External-Integration.md](./37-Backend-External-Integration.md)

---

## 目錄
1. [API 總覽](#api-總覽)
2. [會員資料 API](#會員資料-api)
3. [會員折扣 API](#會員折扣-api)
4. [會員訂單 API](#會員訂單-api)
5. [資料模型](#資料模型)

---

## API 總覽

### 端點清單

| HTTP Method | 端點 | 說明 | 認證 |
|-------------|------|------|------|
| **會員資料** | | | |
| GET | `/api/v1/members/{memberCardId}` | 查詢會員資訊 | ✅ |
| POST | `/api/v1/members/validate` | 驗證會員卡 | ✅ |
| GET | `/api/v1/members/{memberCardId}/profile` | 查詢會員檔案 | ✅ |
| PATCH | `/api/v1/members/{memberCardId}/profile` | 更新會員資料 | ✅ |
| **會員折扣** | | | |
| GET | `/api/v1/members/{memberCardId}/discounts` | 查詢會員折扣 | ✅ |
| GET | `/api/v1/members/{memberCardId}/discounts/{discountId}` | 查詢單一折扣 | ✅ |
| GET | `/api/v1/members/{memberCardId}/points` | 查詢會員點數 | ✅ |
| **會員訂單** | | | |
| GET | `/api/v1/members/{memberCardId}/orders` | 查詢會員訂單 | ✅ |
| GET | `/api/v1/members/{memberCardId}/statistics` | 查詢消費統計 | ✅ |
| **會員等級** | | | |
| GET | `/api/v1/members/levels` | 查詢會員等級列表 | ✅ |
| GET | `/api/v1/members/{memberCardId}/level` | 查詢會員等級 | ✅ |

---

## 會員資料 API

### 1. 查詢會員資訊

查詢會員基本資訊（包含等級、折扣、點數）。

```http
GET /api/v1/members/A123456789 HTTP/1.1
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
    "memberLevel": {
      "levelId": "VIP",
      "levelName": "VIP 會員",
      "priority": 1,
      "benefits": [
        "全商品 95 折",
        "生日當月 9 折",
        "免費安裝服務"
      ]
    },
    "email": "wang@example.com",
    "phone": "0912345678",
    "birthday": "1990-05-15",
    "gender": "M",
    "registeredDate": "2020-01-15",
    "validUntil": "2025-12-31",
    "status": "ACTIVE",
    "points": {
      "available": 2500,
      "pending": 150,
      "expired": 0,
      "lifetimeEarned": 5000
    },
    "discounts": [
      {
        "discountId": "1",
        "discountType": "0",
        "discountTypeName": "折扣率",
        "rate": 0.05,
        "description": "全商品 95 折"
      }
    ],
    "statistics": {
      "totalOrders": 45,
      "totalSpent": 450000.00,
      "averageOrderValue": 10000.00,
      "lastOrderDate": "2025-10-15"
    }
  },
  "timestamp": "2025-10-27T10:00:00Z",
  "traceId": "abc-123-def-456"
}
```

**錯誤回應 404 Not Found**:
```json
{
  "success": false,
  "error": {
    "code": "MEMBER_NOT_FOUND",
    "message": "找不到會員資料",
    "details": "會員卡號 A123456789 不存在"
  },
  "timestamp": "2025-10-27T10:00:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/members/A123456789"
}
```

**錯誤回應 422 Unprocessable Entity** (會員卡過期):
```json
{
  "success": false,
  "error": {
    "code": "MEMBER_CARD_EXPIRED",
    "message": "會員卡已過期",
    "details": "會員卡效期至 2024-12-31，請續約"
  },
  "timestamp": "2025-10-27T10:00:00Z",
  "traceId": "abc-123-def-456",
  "path": "/api/v1/members/A123456789"
}
```

---

### 2. 驗證會員卡

快速驗證會員卡是否有效（輕量級 API，用於訂單建立前驗證）。

```http
POST /api/v1/members/validate HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "memberCardId": "A123456789"
}
```

**回應 200 OK** (有效):
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "valid": true,
    "status": "ACTIVE",
    "validUntil": "2025-12-31",
    "memberName": "王小明",
    "memberLevel": "VIP"
  },
  "timestamp": "2025-10-27T10:05:00Z",
  "traceId": "abc-123-def-456"
}
```

**回應 200 OK** (無效):
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "valid": false,
    "reason": "EXPIRED",
    "message": "會員卡已過期"
  },
  "timestamp": "2025-10-27T10:05:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 查詢會員檔案

查詢會員詳細檔案資料（包含聯絡資訊、地址等）。

```http
GET /api/v1/members/A123456789/profile HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "personalInfo": {
      "name": "王小明",
      "englishName": "Ming Wang",
      "idNumber": "A123456789",
      "birthday": "1990-05-15",
      "gender": "M",
      "nationality": "TW"
    },
    "contactInfo": {
      "email": "wang@example.com",
      "phone": "0912345678",
      "homePhone": "02-12345678",
      "preferredContact": "MOBILE"
    },
    "addresses": [
      {
        "addressId": "1",
        "addressType": "HOME",
        "recipientName": "王小明",
        "phone": "0912345678",
        "city": "台北市",
        "district": "信義區",
        "address": "信義路五段7號",
        "zipCode": "110",
        "isDefault": true
      },
      {
        "addressId": "2",
        "addressType": "OFFICE",
        "recipientName": "王小明",
        "phone": "02-12345678",
        "city": "台北市",
        "district": "大安區",
        "address": "敦化南路二段105號",
        "zipCode": "106",
        "isDefault": false
      }
    ],
    "preferences": {
      "language": "zh-TW",
      "currency": "TWD",
      "receivePromotions": true,
      "receiveNewsletter": true
    },
    "registeredDate": "2020-01-15T00:00:00Z",
    "lastLoginTime": "2025-10-27T09:30:00Z"
  },
  "timestamp": "2025-10-27T10:10:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 4. 更新會員資料

更新會員檔案資料（僅能更新聯絡資訊、地址、偏好設定）。

```http
PATCH /api/v1/members/A123456789/profile HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "contactInfo": {
    "email": "newmail@example.com",
    "phone": "0923456789"
  },
  "preferences": {
    "receivePromotions": false
  }
}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "contactInfo": {
      "email": "newmail@example.com",
      "phone": "0923456789"
    },
    "preferences": {
      "receivePromotions": false
    },
    "updatedTime": "2025-10-27T10:15:00Z"
  },
  "message": "會員資料更新成功",
  "timestamp": "2025-10-27T10:15:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 會員折扣 API

### 1. 查詢會員折扣

查詢會員所有可用的折扣。

```http
GET /api/v1/members/A123456789/discounts HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "memberLevel": "VIP",
    "discounts": [
      {
        "discountId": "1",
        "discountType": "0",
        "discountTypeName": "折扣率 (Discounting)",
        "priority": 3,
        "config": {
          "rate": 0.05,
          "description": "全商品 95 折"
        },
        "applicableCategories": [],
        "excludedCategories": [],
        "validFrom": "2025-01-01",
        "validUntil": "2025-12-31",
        "status": "ACTIVE"
      },
      {
        "discountId": "2",
        "discountType": "2",
        "discountTypeName": "成本加成 (Cost Markup)",
        "priority": 1,
        "config": {
          "markupRate": 1.2,
          "description": "家電類商品成本 ×1.2"
        },
        "applicableCategories": ["家電"],
        "excludedCategories": [],
        "validFrom": "2025-01-01",
        "validUntil": "2025-12-31",
        "status": "ACTIVE"
      }
    ],
    "specialDiscounts": [
      {
        "discountId": "BIRTHDAY",
        "discountType": "0",
        "discountTypeName": "生日優惠",
        "config": {
          "rate": 0.10,
          "description": "生日當月 9 折"
        },
        "validFrom": "2025-05-01",
        "validUntil": "2025-05-31",
        "status": "ACTIVE"
      }
    ]
  },
  "timestamp": "2025-10-27T10:20:00Z",
  "traceId": "abc-123-def-456"
}
```

**折扣類型說明**:

| 類型 ID | 類型名稱 | 優先順序 | 計算公式 |
|---------|---------|---------|---------|
| **2** | 成本加成 (Cost Markup) | 1 (最高) | 折扣價 = 成本 × 加成比例 |
| **0** | 折扣率 (Discounting) | 3 | 折扣價 = 原價 × (1 - 折扣率) |
| **1** | 固定折扣 (Down Margin) | 4 (最低) | 折扣價 = 原價 - 固定金額 |

---

### 2. 查詢單一折扣

查詢特定折扣的詳細資訊。

```http
GET /api/v1/members/A123456789/discounts/1 HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "discountId": "1",
    "memberCardId": "A123456789",
    "discountType": "0",
    "discountTypeName": "折扣率",
    "priority": 3,
    "config": {
      "rate": 0.05,
      "description": "全商品 95 折"
    },
    "applicableCategories": [],
    "excludedCategories": [],
    "applicableSkus": [],
    "excludedSkus": [],
    "minPurchaseAmount": 0.00,
    "maxDiscountAmount": null,
    "validFrom": "2025-01-01T00:00:00Z",
    "validUntil": "2025-12-31T23:59:59Z",
    "status": "ACTIVE",
    "terms": [
      "適用於所有商品",
      "與其他折扣擇優使用",
      "不可與優惠券併用"
    ],
    "createdTime": "2025-01-01T00:00:00Z",
    "updatedTime": "2025-01-01T00:00:00Z"
  },
  "timestamp": "2025-10-27T10:25:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 3. 查詢會員點數

查詢會員點數餘額和歷史記錄。

```http
GET /api/v1/members/A123456789/points HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "points": {
      "available": 2500,
      "pending": 150,
      "expiringSoon": 100,
      "expired": 0,
      "lifetimeEarned": 5000,
      "lifetimeRedeemed": 2500
    },
    "expiringPoints": [
      {
        "points": 100,
        "expiryDate": "2025-11-30"
      }
    ],
    "recentTransactions": [
      {
        "transactionId": "PT001",
        "type": "EARN",
        "points": 150,
        "orderId": "SO20251027001",
        "description": "購物獲得點數",
        "status": "PENDING",
        "transactionDate": "2025-10-27T10:30:00Z",
        "effectiveDate": "2025-11-27T00:00:00Z"
      },
      {
        "transactionId": "PT002",
        "type": "REDEEM",
        "points": -500,
        "orderId": "SO20251015001",
        "description": "點數折抵",
        "status": "COMPLETED",
        "transactionDate": "2025-10-15T14:20:00Z"
      }
    ]
  },
  "timestamp": "2025-10-27T10:30:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 會員訂單 API

### 1. 查詢會員訂單

查詢會員的訂單列表。

```http
GET /api/v1/members/A123456789/orders?page=0&size=20&sort=createTime,desc HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**Query Parameters**:
| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| status | string | ❌ | 訂單狀態 |
| startDate | date | ❌ | 開始日期 |
| endDate | date | ❌ | 結束日期 |
| page | int | ❌ | 頁碼 |
| size | int | ❌ | 每頁筆數 |
| sort | string | ❌ | 排序 |

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "orders": [
      {
        "orderId": "SO20251027001",
        "orderNo": "A1234567890",
        "status": {
          "id": "4",
          "name": "有效"
        },
        "totalAmt": 9500.00,
        "itemCount": 2,
        "createTime": "2025-10-27T10:30:00Z",
        "deliveryType": "HOME_DELIVERY",
        "canCancel": true,
        "canReview": false
      },
      {
        "orderId": "SO20251015001",
        "orderNo": "A1234567880",
        "status": {
          "id": "5",
          "name": "已結案"
        },
        "totalAmt": 15800.00,
        "itemCount": 3,
        "createTime": "2025-10-15T14:20:00Z",
        "deliveryType": "HOME_DELIVERY",
        "canCancel": false,
        "canReview": true
      }
    ],
    "page": 0,
    "size": 20,
    "total": 45
  },
  "timestamp": "2025-10-27T10:35:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 查詢消費統計

查詢會員的消費統計資料。

```http
GET /api/v1/members/A123456789/statistics HTTP/1.1
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
    "memberSince": "2020-01-15",
    "summary": {
      "totalOrders": 45,
      "totalSpent": 450000.00,
      "totalSaved": 22500.00,
      "averageOrderValue": 10000.00,
      "lastOrderDate": "2025-10-27"
    },
    "byStatus": {
      "VALID": 2,
      "PAID": 1,
      "CLOSED": 40,
      "CANCELLED": 2
    },
    "byYear": [
      {
        "year": 2025,
        "orders": 12,
        "spent": 120000.00
      },
      {
        "year": 2024,
        "orders": 18,
        "spent": 180000.00
      },
      {
        "year": 2023,
        "orders": 15,
        "spent": 150000.00
      }
    ],
    "topCategories": [
      {
        "category": "家電",
        "orders": 20,
        "spent": 250000.00
      },
      {
        "category": "傢俱",
        "orders": 15,
        "spent": 150000.00
      },
      {
        "category": "寢具",
        "orders": 10,
        "spent": 50000.00
      }
    ],
    "favoriteProducts": [
      {
        "skuNo": "SKU000001",
        "skuName": "三門冰箱",
        "purchaseCount": 2
      }
    ]
  },
  "timestamp": "2025-10-27T10:40:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 會員等級 API

### 1. 查詢會員等級列表

查詢系統所有會員等級。

```http
GET /api/v1/members/levels HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "levelId": "VIP",
      "levelName": "VIP 會員",
      "priority": 1,
      "minSpending": 100000.00,
      "benefits": [
        "全商品 95 折",
        "生日當月 9 折",
        "免費安裝服務",
        "專屬客服"
      ],
      "discounts": [
        {
          "discountType": "0",
          "rate": 0.05,
          "description": "全商品 95 折"
        }
      ],
      "pointsRate": 1.2,
      "freeShipping": true,
      "icon": "https://cdn.example.com/icons/vip.png"
    },
    {
      "levelId": "GOLD",
      "levelName": "金卡會員",
      "priority": 2,
      "minSpending": 50000.00,
      "benefits": [
        "全商品 97 折",
        "生日當月 95 折"
      ],
      "discounts": [
        {
          "discountType": "0",
          "rate": 0.03,
          "description": "全商品 97 折"
        }
      ],
      "pointsRate": 1.1,
      "freeShipping": true,
      "icon": "https://cdn.example.com/icons/gold.png"
    },
    {
      "levelId": "SILVER",
      "levelName": "銀卡會員",
      "priority": 3,
      "minSpending": 0.00,
      "benefits": [
        "累積點數"
      ],
      "discounts": [],
      "pointsRate": 1.0,
      "freeShipping": false,
      "icon": "https://cdn.example.com/icons/silver.png"
    }
  ],
  "timestamp": "2025-10-27T10:45:00Z",
  "traceId": "abc-123-def-456"
}
```

---

### 2. 查詢會員等級

查詢特定會員的等級資訊和升級進度。

```http
GET /api/v1/members/A123456789/level HTTP/1.1
Host: api.example.com
Authorization: Bearer {jwt_token}
```

**回應 200 OK**:
```json
{
  "success": true,
  "data": {
    "memberCardId": "A123456789",
    "currentLevel": {
      "levelId": "VIP",
      "levelName": "VIP 會員",
      "priority": 1
    },
    "levelProgress": {
      "currentSpending": 450000.00,
      "requiredForCurrent": 100000.00,
      "nextLevel": null,
      "requiredForNext": null,
      "progress": 100.0
    },
    "benefits": [
      "全商品 95 折",
      "生日當月 9 折",
      "免費安裝服務",
      "專屬客服"
    ],
    "levelHistory": [
      {
        "levelId": "VIP",
        "levelName": "VIP 會員",
        "achievedDate": "2023-06-15"
      },
      {
        "levelId": "GOLD",
        "levelName": "金卡會員",
        "achievedDate": "2021-03-20"
      },
      {
        "levelId": "SILVER",
        "levelName": "銀卡會員",
        "achievedDate": "2020-01-15"
      }
    ]
  },
  "timestamp": "2025-10-27T10:50:00Z",
  "traceId": "abc-123-def-456"
}
```

---

## 資料模型

### MemberResponse (會員回應)

```typescript
interface MemberResponse {
  memberCardId: string;           // 會員卡號
  memberName: string;             // 會員姓名
  memberLevel: MemberLevel;       // 會員等級
  email: string;                  // Email
  phone: string;                  // 手機
  birthday: string;               // 生日 (YYYY-MM-DD)
  gender: Gender;                 // 性別
  registeredDate: string;         // 註冊日期
  validUntil: string;             // 有效期限
  status: MemberStatus;           // 狀態
  points: PointsSummary;          // 點數摘要
  discounts: MemberDiscount[];    // 會員折扣
  statistics: MemberStatistics;   // 消費統計
}

interface MemberLevel {
  levelId: string;                // 等級 ID
  levelName: string;              // 等級名稱
  priority: number;               // 優先順序
  benefits: string[];             // 權益
}

enum Gender {
  M = 'M',                        // 男性
  F = 'F',                        // 女性
  O = 'O'                         // 其他
}

enum MemberStatus {
  ACTIVE = 'ACTIVE',              // 有效
  EXPIRED = 'EXPIRED',            // 已過期
  SUSPENDED = 'SUSPENDED',        // 停權
  CANCELLED = 'CANCELLED'         // 已取消
}

interface PointsSummary {
  available: number;              // 可用點數
  pending: number;                // 待生效點數
  expired: number;                // 已過期點數
  lifetimeEarned: number;         // 累積獲得點數
}
```

### MemberDiscount (會員折扣)

```typescript
interface MemberDiscount {
  discountId: string;             // 折扣 ID
  discountType: DiscountType;     // 折扣類型 (0, 1, 2)
  discountTypeName: string;       // 折扣類型名稱
  priority: number;               // 優先順序 (1-4)
  config: DiscountConfig;         // 折扣配置
  applicableCategories: string[]; // 適用分類
  excludedCategories: string[];   // 排除分類
  validFrom: string;              // 生效日期
  validUntil: string;             // 失效日期
  status: DiscountStatus;         // 狀態
}

type DiscountType = '0' | '1' | '2';
// 0: Discounting (折扣率)
// 1: Down Margin (固定折扣)
// 2: Cost Markup (成本加成)

interface DiscountConfig {
  rate?: number;                  // 折扣率 (Type 0)
  amount?: number;                // 固定折扣金額 (Type 1)
  markupRate?: number;            // 成本加成比例 (Type 2)
  description: string;            // 說明
}

enum DiscountStatus {
  ACTIVE = 'ACTIVE',              // 有效
  INACTIVE = 'INACTIVE',          // 無效
  EXPIRED = 'EXPIRED'             // 已過期
}
```

### MemberStatistics (消費統計)

```typescript
interface MemberStatistics {
  totalOrders: number;            // 總訂單數
  totalSpent: number;             // 總消費金額
  averageOrderValue: number;      // 平均訂單金額
  lastOrderDate: string;          // 最後訂單日期
}
```

---

## CRM 系統整合

### 資料來源

會員服務 API 的資料來自 **CRM 系統**（外部 SOAP Web Service）。

**CRM WSDL**:
```
http://crmjbtst.testritegroup.com/RFEP/service/MemberWebService?wsdl
```

### 快取策略

由於 CRM 系統回應較慢（~200ms），會員服務使用 Redis 快取：

```yaml
快取配置:
  member-info:
    key: "member:{memberCardId}"
    ttl: 30 分鐘
    命中率: 95%
    節省時間: 200ms → 5ms (-97.5%)

  member-discounts:
    key: "member-discount:{memberCardId}:{discType}"
    ttl: 30 分鐘
    命中率: 98%

  member-levels:
    key: "member-levels:all"
    ttl: 24 小時
    命中率: 99%
```

### 容錯機制

使用 Resilience4j 實作容錯：

```java
@CircuitBreaker(name = "crm", fallbackMethod = "fallbackGetMember")
@Retry(name = "crm", maxAttempts = 3)
@TimeLimiter(name = "crm", timeout = Duration.ofSeconds(3))
public MemberInfo getMemberInfo(String memberCardId) {
    return crmClient.getMemberInfo(memberCardId);
}

// Fallback: 從快取取得
private MemberInfo fallbackGetMember(String memberCardId, Exception e) {
    return memberCache.get(memberCardId)
        .orElseThrow(() -> new MemberNotFoundException(memberCardId));
}
```

---

## 錯誤碼

| 錯誤碼 | HTTP 狀態碼 | 說明 |
|--------|------------|------|
| MEMBER_NOT_FOUND | 404 | 找不到會員資料 |
| MEMBER_CARD_EXPIRED | 422 | 會員卡已過期 |
| MEMBER_CARD_SUSPENDED | 422 | 會員卡已停權 |
| MEMBER_DISCOUNT_NOT_FOUND | 404 | 找不到折扣資料 |
| MEMBER_POINTS_INSUFFICIENT | 422 | 點數不足 |
| MEMBER_UPDATE_FAILED | 500 | 會員資料更新失敗 |
| CRM_CONNECTION_ERROR | 502 | CRM 系統連線失敗 |
| CRM_TIMEOUT | 504 | CRM 系統逾時 |

---

## 效能指標

| 指標 | 目標值 | 當前值 |
|------|--------|--------|
| **平均回應時間 (快取命中)** | < 50ms | 10ms ✅ |
| **平均回應時間 (快取未命中)** | < 300ms | 220ms ✅ |
| **快取命中率** | > 90% | 95% ✅ |
| **CRM 可用性** | > 99% | 99.5% ✅ |

---

## 相關文檔

- [05-Pricing-Member-Discount-Logic.md](./05-Pricing-Member-Discount-Logic.md) - 會員折扣邏輯
- [13-API-Pricing-Service.md](./13-API-Pricing-Service.md) - 計價服務 API
- [37-Backend-External-Integration.md](./37-Backend-External-Integration.md) - 外部整合實作
