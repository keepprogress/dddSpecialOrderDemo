# 16. Database Design - 資料庫設計規範

## 目錄

- [1. 設計原則](#1-設計原則)
- [2. 現行資料庫分析](#2-現行資料庫分析)
- [3. 目標資料庫架構](#3-目標資料庫架構)
- [4. 核心資料表設計](#4-核心資料表設計)
- [5. 索引策略](#5-索引策略)
- [6. 資料遷移計畫](#6-資料遷移計畫)
- [7. Schema 版本控制](#7-schema-版本控制)

---

## 1. 設計原則

### 1.1 Database-Per-Service Pattern

```plaintext
┌─────────────────────────────────────────────────────────────┐
│                    Microservices Architecture               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Order Service│    │Pricing Service│   │Member Service│ │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘ │
│         │                   │                    │          │
│         ▼                   ▼                    ▼          │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │   Order DB   │    │  Pricing DB  │    │  Member DB   │ │
│  │  (Oracle)    │    │  (Oracle)    │    │   (Oracle)   │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐                      │
│  │Payment Service│   │Inventory Svc │                      │
│  └──────┬───────┘    └──────┬───────┘                      │
│         │                   │                               │
│         ▼                   ▼                               │
│  ┌──────────────┐    ┌──────────────┐                      │
│  │  Payment DB  │    │ Inventory DB │                      │
│  │  (Oracle)    │    │   (Oracle)   │                      │
│  └──────────────┘    └──────────────┘                      │
└─────────────────────────────────────────────────────────────┘
```

**核心原則**:

1. **資料庫隔離**: 每個微服務擁有獨立的資料庫 Schema
2. **鬆散耦合**: 服務間透過 API 通訊，不直接存取其他服務的資料庫
3. **自主性**: 每個服務可獨立選擇適合的資料庫技術
4. **一致性**: 使用 Saga Pattern 實現分散式交易

### 1.2 命名規範

```sql
-- 資料表命名: 小寫 + 底線分隔
CREATE TABLE orders (...)
CREATE TABLE order_items (...)
CREATE TABLE pricing_history (...)

-- 欄位命名: 小寫 + 底線分隔
order_id VARCHAR2(20)
member_card_id VARCHAR2(20)
created_at TIMESTAMP

-- 索引命名: idx_{table}_{columns}
CREATE INDEX idx_orders_member_id ON orders(member_card_id);

-- 外鍵命名: fk_{table}_{ref_table}
ALTER TABLE order_items
  ADD CONSTRAINT fk_order_items_orders
  FOREIGN KEY (order_id) REFERENCES orders(order_id);

-- 序列命名: seq_{table}_{column}
CREATE SEQUENCE seq_orders_id;
```

### 1.3 資料類型標準

| 用途 | Oracle 類型 | 說明 |
|-----|------------|------|
| 主鍵 ID | VARCHAR2(20) | 業務 ID (如 SO20251027001) |
| 數值主鍵 | NUMBER(19) | 自增序列 |
| 金額 | NUMBER(10,2) | 精確度 2 位小數 |
| 百分比 | NUMBER(5,2) | 如 12.50% |
| 日期時間 | TIMESTAMP | 精確到毫秒 |
| 布林值 | CHAR(1) | 'Y'/'N' 或 '1'/'0' |
| 狀態碼 | VARCHAR2(2) | 如 '1', '2', '4' |
| 文字 | VARCHAR2(n) | n ≤ 4000 |
| 大文字 | CLOB | 超過 4000 字元 |

---

## 2. 現行資料庫分析

### 2.1 核心資料表 (Current System)

```sql
-- 訂單主檔 (SO_MASTER)
CREATE TABLE SO_MASTER (
    SO_NO VARCHAR2(20) PRIMARY KEY,          -- 訂單編號
    MEMBER_CARD_ID VARCHAR2(20),             -- 會員卡號
    SO_STATUS_ID VARCHAR2(2),                -- 訂單狀態
    SO_DATE DATE,                            -- 訂單日期
    CHANNEL_ID VARCHAR2(10),                 -- 通路代碼
    TAX_NO VARCHAR2(20),                     -- 統編
    IS_TAX_ZERO CHAR(1),                     -- 免稅註記
    POS_AMT NUMBER(10,2),                    -- 商品總金額
    INSTALL_AMT NUMBER(10,2),                -- 安裝總金額
    DELIVERY_AMT NUMBER(10,2),               -- 運送總金額
    MEMBER_DIS_AMT NUMBER(10,2),             -- 會員卡折扣
    COUPON_DIS_AMT NUMBER(10,2),             -- 折價券折扣
    DIRECT_DELIVERY_AMT NUMBER(10,2),        -- 直送費用
    PAID_AMT NUMBER(10,2),                   -- 實付金額
    CREATOR VARCHAR2(20),
    CREATED_TIME TIMESTAMP,
    UPDATER VARCHAR2(20),
    UPDATED_TIME TIMESTAMP
);

-- 訂單明細 (SO_SKU)
CREATE TABLE SO_SKU (
    SO_SKU_ID NUMBER(19) PRIMARY KEY,
    SO_NO VARCHAR2(20),                      -- 外鍵 → SO_MASTER
    SKU_NO VARCHAR2(20),                     -- 商品編號
    SKU_QTY NUMBER(5),                       -- 數量
    SELLING_AMT NUMBER(10,2),                -- 售價
    MEMBER_DIS_AMT NUMBER(10,2),             -- 會員折扣
    POS_AMT NUMBER(10,2),                    -- 小計
    IS_SERIAL_NO CHAR(1),                    -- 是否需序號
    CREATOR VARCHAR2(20),
    CREATED_TIME TIMESTAMP
);

-- 安裝/測量明細 (SO_WORKTYPE)
CREATE TABLE SO_WORKTYPE (
    SO_WORKTYPE_ID NUMBER(19) PRIMARY KEY,
    SO_NO VARCHAR2(20),
    WORK_TYPE_ID VARCHAR2(10),               -- 工種代碼
    WORK_TYPE_AMT NUMBER(10,2),              -- 工資金額
    MEMBER_DIS_AMT NUMBER(10,2),             -- 會員折扣
    POS_AMT NUMBER(10,2),                    -- 小計
    CREATOR VARCHAR2(20),
    CREATED_TIME TIMESTAMP
);
```

**問題分析**:

1. **單一資料庫**: 所有功能共用同一個 Schema，難以水平擴展
2. **欄位冗餘**: `SO_MASTER` 包含計價結果，每次重新計價需更新 8 個欄位
3. **缺乏版本控制**: 無法追蹤訂單修改歷史
4. **缺乏軟刪除**: 使用硬刪除，無法復原
5. **命名不一致**: `SO_MASTER` vs `SO_SKU` vs `SO_WORKTYPE`

---

## 3. 目標資料庫架構

### 3.1 微服務資料庫分割

```plaintext
┌─────────────────────────────────────────────────────────────┐
│                        Database Schema                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ORDER_DB (訂單資料庫)                                       │
│  ├── orders (訂單主檔)                                       │
│  ├── order_items (訂單明細)                                  │
│  ├── order_status_history (狀態異動紀錄)                     │
│  └── order_events (事件日誌)                                 │
│                                                              │
│  PRICING_DB (計價資料庫)                                     │
│  ├── pricing_requests (計價請求)                             │
│  ├── pricing_results (計價結果)                              │
│  ├── pricing_computes (計價明細)                             │
│  ├── member_discounts (會員折扣)                             │
│  ├── promotions (促銷活動)                                   │
│  └── pricing_cache (計價快取)                                │
│                                                              │
│  PAYMENT_DB (付款資料庫)                                     │
│  ├── payments (付款記錄)                                     │
│  ├── payment_transactions (交易明細)                         │
│  ├── refunds (退款記錄)                                      │
│  └── idempotency_keys (冪等鍵)                               │
│                                                              │
│  MEMBER_DB (會員資料庫)                                      │
│  ├── members (會員資訊快取)                                  │
│  ├── member_points (點數記錄)                                │
│  ├── member_orders (訂單統計)                                │
│  └── member_sync_log (CRM 同步日誌)                          │
│                                                              │
│  INVENTORY_DB (庫存資料庫)                                   │
│  ├── inventory_reservations (庫存預留)                       │
│  ├── inventory_releases (庫存釋放)                           │
│  └── serial_numbers (序號管理)                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 核心資料表設計

### 4.1 ORDER_DB - 訂單資料庫

#### 4.1.1 orders (訂單主檔)

```sql
CREATE TABLE orders (
    -- 主鍵
    order_id VARCHAR2(20) PRIMARY KEY,       -- SO20251027001

    -- 基本資訊
    member_card_id VARCHAR2(20),             -- 會員卡號
    channel_id VARCHAR2(10) NOT NULL,        -- 通路代碼
    order_date TIMESTAMP NOT NULL,           -- 訂單日期

    -- 狀態
    status_id VARCHAR2(2) NOT NULL,          -- 1:草稿 2:報價 4:有效 3:已付款 5:已結案 6:作廢
    status_name VARCHAR2(20),                -- 狀態名稱
    status_updated_at TIMESTAMP,             -- 狀態更新時間
    status_updated_by VARCHAR2(20),          -- 狀態更新人員

    -- 發票資訊
    tax_no VARCHAR2(20),                     -- 統編
    is_tax_zero CHAR(1) DEFAULT 'N',         -- 免稅註記 Y/N

    -- 計價資訊 (快照, 來自 PRICING_DB)
    pricing_request_id VARCHAR2(36),         -- 計價請求 ID (UUID)
    original_total NUMBER(10,2),             -- 原始總金額
    discount_total NUMBER(10,2),             -- 折扣總金額
    final_total NUMBER(10,2),                -- 最終總金額
    pricing_version INT DEFAULT 1,           -- 計價版本號
    pricing_calculated_at TIMESTAMP,         -- 計價時間

    -- 付款資訊 (快照, 來自 PAYMENT_DB)
    payment_id VARCHAR2(20),                 -- 付款 ID
    paid_amount NUMBER(10,2),                -- 實付金額
    paid_at TIMESTAMP,                       -- 付款時間

    -- 配送資訊
    delivery_address VARCHAR2(200),          -- 配送地址
    delivery_contact VARCHAR2(50),           -- 聯絡人
    delivery_phone VARCHAR2(20),             -- 聯絡電話
    delivery_scheduled_date DATE,            -- 預計配送日期

    -- 備註
    remarks VARCHAR2(500),                   -- 訂單備註

    -- 軟刪除
    is_deleted CHAR(1) DEFAULT 'N',          -- 刪除註記 Y/N
    deleted_at TIMESTAMP,                    -- 刪除時間
    deleted_by VARCHAR2(20),                 -- 刪除人員

    -- 審計欄位
    created_by VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR2(20),
    updated_at TIMESTAMP,
    version INT DEFAULT 1 NOT NULL           -- 樂觀鎖版本號
);

-- 索引
CREATE INDEX idx_orders_member_id ON orders(member_card_id);
CREATE INDEX idx_orders_status ON orders(status_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_pricing_req ON orders(pricing_request_id);
CREATE INDEX idx_orders_payment_id ON orders(payment_id);

-- 複合索引
CREATE INDEX idx_orders_member_status ON orders(member_card_id, status_id);
CREATE INDEX idx_orders_date_status ON orders(order_date, status_id);

-- 備註
COMMENT ON TABLE orders IS '訂單主檔 - 儲存訂單基本資訊與計價/付款快照';
COMMENT ON COLUMN orders.pricing_request_id IS '計價請求 ID, 對應 PRICING_DB.pricing_requests';
COMMENT ON COLUMN orders.payment_id IS '付款 ID, 對應 PAYMENT_DB.payments';
COMMENT ON COLUMN orders.version IS '樂觀鎖版本號, 用於並發控制';
```

#### 4.1.2 order_items (訂單明細)

```sql
CREATE TABLE order_items (
    -- 主鍵
    item_id NUMBER(19) PRIMARY KEY,          -- 自增序列
    order_id VARCHAR2(20) NOT NULL,          -- 外鍵 → orders

    -- 商品資訊
    sku_no VARCHAR2(20) NOT NULL,            -- 商品編號
    sku_name VARCHAR2(100),                  -- 商品名稱 (冗餘, 避免跨庫查詢)
    quantity NUMBER(5) NOT NULL,             -- 數量

    -- 計價資訊 (快照)
    unit_price NUMBER(10,2),                 -- 單價
    original_amount NUMBER(10,2),            -- 原始金額
    discount_amount NUMBER(10,2),            -- 折扣金額
    final_amount NUMBER(10,2),               -- 最終金額

    -- 序號管理
    requires_serial_no CHAR(1) DEFAULT 'N',  -- 是否需要序號 Y/N
    serial_numbers VARCHAR2(500),            -- 序號清單 (JSON 格式)

    -- 安裝/測量工種
    work_type_id VARCHAR2(10),               -- 工種代碼
    work_type_name VARCHAR2(50),             -- 工種名稱
    work_type_amount NUMBER(10,2),           -- 工資金額

    -- 軟刪除
    is_deleted CHAR(1) DEFAULT 'N',
    deleted_at TIMESTAMP,
    deleted_by VARCHAR2(20),

    -- 審計欄位
    created_by VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR2(20),
    updated_at TIMESTAMP,

    -- 外鍵約束
    CONSTRAINT fk_order_items_orders
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 序列
CREATE SEQUENCE seq_order_items_id START WITH 1 INCREMENT BY 1;

-- 索引
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_sku_no ON order_items(sku_no);
CREATE INDEX idx_order_items_created_at ON order_items(created_at);

-- 備註
COMMENT ON TABLE order_items IS '訂單明細 - 儲存訂單商品與工種明細';
COMMENT ON COLUMN order_items.serial_numbers IS 'JSON 格式序號清單, 如: ["SN001", "SN002"]';
```

#### 4.1.3 order_status_history (狀態異動紀錄)

```sql
CREATE TABLE order_status_history (
    -- 主鍵
    history_id NUMBER(19) PRIMARY KEY,
    order_id VARCHAR2(20) NOT NULL,

    -- 狀態異動
    from_status_id VARCHAR2(2),              -- 原狀態 (NULL 表示新建)
    from_status_name VARCHAR2(20),
    to_status_id VARCHAR2(2) NOT NULL,       -- 新狀態
    to_status_name VARCHAR2(20),

    -- 異動原因
    reason VARCHAR2(200),                    -- 異動原因
    remarks VARCHAR2(500),                   -- 備註

    -- 審計欄位
    changed_by VARCHAR2(20) NOT NULL,        -- 異動人員
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 外鍵約束
    CONSTRAINT fk_status_history_orders
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 序列
CREATE SEQUENCE seq_order_status_history_id START WITH 1 INCREMENT BY 1;

-- 索引
CREATE INDEX idx_status_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_status_history_changed_at ON order_status_history(changed_at);

-- 備註
COMMENT ON TABLE order_status_history IS '訂單狀態異動歷史 - 追蹤訂單狀態變更';
```

#### 4.1.4 order_events (事件日誌)

```sql
CREATE TABLE order_events (
    -- 主鍵
    event_id VARCHAR2(36) PRIMARY KEY,       -- UUID
    order_id VARCHAR2(20) NOT NULL,

    -- 事件類型
    event_type VARCHAR2(50) NOT NULL,        -- ORDER_CREATED, ORDER_CONFIRMED, PRICING_CALCULATED, PAYMENT_COMPLETED, etc.
    event_payload CLOB,                      -- JSON 格式事件內容

    -- 狀態
    status VARCHAR2(20) DEFAULT 'PENDING',   -- PENDING, PUBLISHED, FAILED
    retry_count INT DEFAULT 0,
    error_message VARCHAR2(500),

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    published_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_type ON order_events(event_type);
CREATE INDEX idx_order_events_status ON order_events(status);
CREATE INDEX idx_order_events_created_at ON order_events(created_at);

-- 備註
COMMENT ON TABLE order_events IS '訂單事件日誌 - Saga Pattern 事件溯源';
COMMENT ON COLUMN order_events.event_payload IS 'JSON 格式, 包含事件相關資料';
```

### 4.2 PRICING_DB - 計價資料庫

#### 4.2.1 pricing_requests (計價請求)

```sql
CREATE TABLE pricing_requests (
    -- 主鍵
    request_id VARCHAR2(36) PRIMARY KEY,     -- UUID

    -- 請求資訊
    order_id VARCHAR2(20),                   -- 關聯訂單 (可為 NULL, 用於預估價)
    member_card_id VARCHAR2(20),             -- 會員卡號
    channel_id VARCHAR2(10) NOT NULL,        -- 通路代碼

    -- 請求內容
    request_payload CLOB NOT NULL,           -- JSON 格式, 包含 SKU 清單

    -- 計算結果
    original_total NUMBER(10,2),             -- 原始總金額
    discount_total NUMBER(10,2),             -- 折扣總金額
    final_total NUMBER(10,2),                -- 最終總金額

    -- 效能指標
    calculation_time_ms INT,                 -- 計算耗時 (毫秒)
    cache_hit CHAR(1) DEFAULT 'N',           -- 是否命中快取 Y/N

    -- 狀態
    status VARCHAR2(20) DEFAULT 'PENDING',   -- PENDING, COMPLETED, FAILED
    error_message VARCHAR2(500),

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_pricing_req_order_id ON pricing_requests(order_id);
CREATE INDEX idx_pricing_req_member_id ON pricing_requests(member_card_id);
CREATE INDEX idx_pricing_req_created_at ON pricing_requests(created_at);
CREATE INDEX idx_pricing_req_status ON pricing_requests(status);

-- 備註
COMMENT ON TABLE pricing_requests IS '計價請求紀錄 - 追蹤每次計價請求';
COMMENT ON COLUMN pricing_requests.request_payload IS 'JSON: {skus: [{skuNo, quantity}], ...}';
```

#### 4.2.2 pricing_results (計價結果)

```sql
CREATE TABLE pricing_results (
    -- 主鍵
    result_id NUMBER(19) PRIMARY KEY,
    request_id VARCHAR2(36) NOT NULL,        -- 外鍵 → pricing_requests

    -- 計算類型 (ComputeType)
    compute_type VARCHAR2(2) NOT NULL,       -- 1:商品小計 2:安裝小計 3:運送小計 4:會員折扣 5:直送費 6:折價券
    compute_name VARCHAR2(50),

    -- 金額
    amount NUMBER(10,2) NOT NULL,            -- 該類型金額

    -- 明細
    details CLOB,                            -- JSON 格式明細

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- 外鍵約束
    CONSTRAINT fk_pricing_results_requests
        FOREIGN KEY (request_id) REFERENCES pricing_requests(request_id)
);

-- 序列
CREATE SEQUENCE seq_pricing_results_id START WITH 1 INCREMENT BY 1;

-- 索引
CREATE INDEX idx_pricing_results_req_id ON pricing_results(request_id);
CREATE INDEX idx_pricing_results_type ON pricing_results(compute_type);

-- 備註
COMMENT ON TABLE pricing_results IS '計價結果明細 - 儲存 6 種 ComputeType 計價結果';
```

#### 4.2.3 pricing_cache (計價快取)

```sql
CREATE TABLE pricing_cache (
    -- 主鍵
    cache_key VARCHAR2(100) PRIMARY KEY,     -- MD5(memberCardId + skus + channelId)

    -- 快取內容
    pricing_result CLOB NOT NULL,            -- JSON 格式完整計價結果

    -- TTL
    expires_at TIMESTAMP NOT NULL,           -- 過期時間 (5分鐘)
    hit_count INT DEFAULT 0,                 -- 命中次數

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_hit_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_pricing_cache_expires ON pricing_cache(expires_at);

-- 備註
COMMENT ON TABLE pricing_cache IS '計價快取 - 減少重複計算 (TTL 5分鐘)';
COMMENT ON COLUMN pricing_cache.cache_key IS 'MD5 哈希值, 確保唯一性';
```

### 4.3 PAYMENT_DB - 付款資料庫

#### 4.3.1 payments (付款記錄)

```sql
CREATE TABLE payments (
    -- 主鍵
    payment_id VARCHAR2(20) PRIMARY KEY,     -- PAY20251027001
    order_id VARCHAR2(20) NOT NULL,          -- 關聯訂單

    -- 付款資訊
    amount NUMBER(10,2) NOT NULL,            -- 付款金額
    payment_method VARCHAR2(20) NOT NULL,    -- CASH, CREDIT_CARD, BANK_TRANSFER, etc.

    -- 狀態
    status VARCHAR2(20) DEFAULT 'PENDING',   -- PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    status_updated_at TIMESTAMP,

    -- POS 整合
    pos_order_no VARCHAR2(30),               -- POS 單號
    pos_receipt_no VARCHAR2(30),             -- POS 發票號碼
    pos_callback_at TIMESTAMP,               -- POS 回調時間
    pos_signature VARCHAR2(100),             -- POS 回調簽章

    -- 第三方支付
    gateway_provider VARCHAR2(50),           -- 支付商 (如: NewebPay, ECPay)
    gateway_transaction_id VARCHAR2(50),     -- 第三方交易 ID
    gateway_callback_url VARCHAR2(200),      -- 回調 URL

    -- 退款資訊
    refund_amount NUMBER(10,2) DEFAULT 0,    -- 已退款金額
    refund_status VARCHAR2(20),              -- NONE, PARTIAL, FULL

    -- 審計欄位
    created_by VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR2(20),
    updated_at TIMESTAMP,
    completed_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_pos_order_no ON payments(pos_order_no);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- 備註
COMMENT ON TABLE payments IS '付款記錄 - 儲存訂單付款資訊與第三方整合';
```

#### 4.3.2 idempotency_keys (冪等鍵)

```sql
CREATE TABLE idempotency_keys (
    -- 主鍵
    idempotency_key VARCHAR2(100) PRIMARY KEY,  -- 客戶端提供的唯一鍵

    -- 請求資訊
    request_method VARCHAR2(10) NOT NULL,       -- POST, PUT, PATCH
    request_path VARCHAR2(200) NOT NULL,        -- /api/v1/payments/process
    request_body CLOB,                          -- 請求內容

    -- 回應資訊
    response_status INT,                        -- HTTP Status Code
    response_body CLOB,                         -- 回應內容

    -- TTL
    expires_at TIMESTAMP NOT NULL,              -- 過期時間 (24 小時)

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引
CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- 備註
COMMENT ON TABLE idempotency_keys IS '冪等鍵記錄 - 防止重複處理 (TTL 24小時)';
```

### 4.4 MEMBER_DB - 會員資料庫

#### 4.4.1 members (會員資訊快取)

```sql
CREATE TABLE members (
    -- 主鍵
    member_card_id VARCHAR2(20) PRIMARY KEY, -- 會員卡號

    -- 基本資訊 (快取自 CRM)
    member_name VARCHAR2(50),
    member_level VARCHAR2(20),               -- VIP, GOLD, SILVER
    phone VARCHAR2(20),
    email VARCHAR2(100),

    -- 折扣資訊
    discount_type VARCHAR2(2),               -- 0:折扣率 1:固定折扣 2:成本加成
    discount_value NUMBER(10,4),             -- 折扣值
    discount_priority INT,                   -- 優先序

    -- 點數
    total_points INT DEFAULT 0,              -- 總點數
    available_points INT DEFAULT 0,          -- 可用點數

    -- 統計
    total_orders INT DEFAULT 0,              -- 訂單總數
    total_amount NUMBER(12,2) DEFAULT 0,     -- 消費總金額
    last_order_date DATE,                    -- 最後訂單日期

    -- CRM 同步
    crm_synced_at TIMESTAMP,                 -- CRM 同步時間
    cache_expires_at TIMESTAMP,              -- 快取過期時間 (30分鐘)

    -- 審計欄位
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- 索引
CREATE INDEX idx_members_level ON members(member_level);
CREATE INDEX idx_members_cache_expires ON members(cache_expires_at);
CREATE INDEX idx_members_last_order ON members(last_order_date);

-- 備註
COMMENT ON TABLE members IS '會員資訊快取 - 減少 CRM API 呼叫 (TTL 30分鐘)';
```

---

## 5. 索引策略

### 5.1 索引類型選擇

```sql
-- 1. B-Tree 索引 (預設, 適用於大部分查詢)
CREATE INDEX idx_orders_order_date ON orders(order_date);

-- 2. Bitmap 索引 (適用於低基數欄位)
CREATE BITMAP INDEX idx_orders_status_bitmap ON orders(status_id);
CREATE BITMAP INDEX idx_orders_is_deleted_bitmap ON orders(is_deleted);

-- 3. 函數索引 (適用於計算欄位查詢)
CREATE INDEX idx_orders_year_month
    ON orders(EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date));

-- 4. 部分索引 (只索引特定條件資料)
CREATE INDEX idx_orders_active
    ON orders(order_id) WHERE is_deleted = 'N';
```

### 5.2 索引維護策略

```sql
-- 定期重建索引 (每月執行)
ALTER INDEX idx_orders_member_id REBUILD ONLINE;

-- 監控索引使用率
SELECT
    index_name,
    table_name,
    blevel,              -- B-Tree 層級
    leaf_blocks,         -- 葉節點數量
    distinct_keys,       -- 唯一鍵數量
    clustering_factor    -- 聚集因子
FROM user_indexes
WHERE table_name = 'ORDERS';

-- 刪除未使用索引
DROP INDEX idx_unused_index;
```

---

## 6. 資料遷移計畫

### 6.1 遷移策略

```plaintext
階段 1: 雙寫 (Dual Write)
┌──────────┐
│ 應用程式 │
└────┬─────┘
     │
     ├─────────► 舊資料庫 (SO_MASTER)        [主寫入]
     │
     └─────────► 新資料庫 (orders)           [次寫入, 非同步]

階段 2: 資料驗證 (Validation)
- 比對新舊資料一致性
- 修正差異資料
- 執行 ETL 補齊歷史資料

階段 3: 讀寫切換 (Switchover)
┌──────────┐
│ 應用程式 │
└────┬─────┘
     │
     ├─────────► 新資料庫 (orders)           [主寫入]
     │
     └─────────► 舊資料庫 (SO_MASTER)        [只讀, 備份]

階段 4: 下線舊系統 (Decommission)
- 關閉舊資料庫寫入
- 封存歷史資料
```

### 6.2 ETL Script 範例

```sql
-- ETL: SO_MASTER → orders
INSERT INTO orders (
    order_id, member_card_id, channel_id, order_date,
    status_id, status_name, tax_no, is_tax_zero,
    original_total, discount_total, final_total,
    created_by, created_at, updated_by, updated_at
)
SELECT
    SO_NO,
    MEMBER_CARD_ID,
    CHANNEL_ID,
    SO_DATE,
    SO_STATUS_ID,
    CASE SO_STATUS_ID
        WHEN '1' THEN '草稿'
        WHEN '2' THEN '報價'
        WHEN '3' THEN '已付款'
        WHEN '4' THEN '有效'
        WHEN '5' THEN '已結案'
        WHEN '6' THEN '作廢'
    END,
    TAX_NO,
    IS_TAX_ZERO,
    (POS_AMT + INSTALL_AMT + DELIVERY_AMT),          -- 原始總金額
    (MEMBER_DIS_AMT + COUPON_DIS_AMT),               -- 折扣總金額
    PAID_AMT,                                         -- 最終總金額
    CREATOR,
    CREATED_TIME,
    UPDATER,
    UPDATED_TIME
FROM SO_MASTER
WHERE SO_NO NOT IN (SELECT order_id FROM orders);

-- ETL: SO_SKU + SO_WORKTYPE → order_items
INSERT INTO order_items (
    item_id, order_id, sku_no, quantity,
    unit_price, original_amount, discount_amount, final_amount,
    work_type_id, work_type_amount,
    created_by, created_at
)
SELECT
    seq_order_items_id.NEXTVAL,
    SO_NO,
    SKU_NO,
    SKU_QTY,
    SELLING_AMT,
    (SELLING_AMT * SKU_QTY),
    MEMBER_DIS_AMT,
    POS_AMT,
    NULL,
    NULL,
    CREATOR,
    CREATED_TIME
FROM SO_SKU
WHERE SO_NO IN (SELECT order_id FROM orders);
```

---

## 7. Schema 版本控制

### 7.1 Flyway 設定

**pom.xml**:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.3</version>
</dependency>
```

**application.yml**:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: ORDER_DB, PRICING_DB, PAYMENT_DB, MEMBER_DB
    table: flyway_schema_history
```

### 7.2 Migration Script 命名規範

```plaintext
db/migration/
├── order-db/
│   ├── V1.0.0__create_orders_table.sql
│   ├── V1.0.1__create_order_items_table.sql
│   ├── V1.0.2__create_order_status_history_table.sql
│   ├── V1.1.0__add_order_version_column.sql
│   └── V2.0.0__add_soft_delete_columns.sql
├── pricing-db/
│   ├── V1.0.0__create_pricing_requests_table.sql
│   ├── V1.0.1__create_pricing_results_table.sql
│   └── V1.1.0__add_pricing_cache_table.sql
└── payment-db/
    ├── V1.0.0__create_payments_table.sql
    └── V1.0.1__create_idempotency_keys_table.sql
```

**命名規則**: `V{major}.{minor}.{patch}__{description}.sql`

### 7.3 Migration Script 範例

**V1.0.0__create_orders_table.sql**:

```sql
-- ============================================================
-- Flyway Migration: V1.0.0__create_orders_table.sql
-- Description: 建立訂單主檔資料表
-- Author: Development Team
-- Date: 2025-10-27
-- ============================================================

CREATE TABLE orders (
    order_id VARCHAR2(20) PRIMARY KEY,
    member_card_id VARCHAR2(20),
    channel_id VARCHAR2(10) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status_id VARCHAR2(2) NOT NULL,
    status_name VARCHAR2(20),
    -- ... (其他欄位)
    created_by VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by VARCHAR2(20),
    updated_at TIMESTAMP,
    version INT DEFAULT 1 NOT NULL
);

-- 索引
CREATE INDEX idx_orders_member_id ON orders(member_card_id);
CREATE INDEX idx_orders_status ON orders(status_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);

-- 備註
COMMENT ON TABLE orders IS '訂單主檔 - 儲存訂單基本資訊';

-- ============================================================
-- End of Migration
-- ============================================================
```

**V1.1.0__add_order_version_column.sql**:

```sql
-- ============================================================
-- Flyway Migration: V1.1.0__add_order_version_column.sql
-- Description: 新增訂單樂觀鎖版本欄位
-- Author: Development Team
-- Date: 2025-11-01
-- ============================================================

-- 新增欄位
ALTER TABLE orders ADD (
    pricing_version INT DEFAULT 1
);

-- 更新現有資料
UPDATE orders SET pricing_version = 1 WHERE pricing_version IS NULL;

-- 備註
COMMENT ON COLUMN orders.pricing_version IS '計價版本號, 用於樂觀鎖';

-- ============================================================
-- End of Migration
-- ============================================================
```

---

## 總結

### 資料庫設計核心要點

1. **微服務資料庫隔離**: 每個服務擁有獨立的 Schema
2. **快照模式**: 訂單保存計價/付款快照, 避免跨庫 JOIN
3. **軟刪除**: 使用 `is_deleted` 標記, 不硬刪除資料
4. **樂觀鎖**: 使用 `version` 欄位防止並發衝突
5. **事件溯源**: 使用 `order_events` 實現 Saga Pattern
6. **索引策略**: B-Tree + Bitmap + 複合索引, 提升查詢效能
7. **資料遷移**: 雙寫 → 驗證 → 切換 → 下線, 確保平滑遷移
8. **Schema 版本控制**: Flyway 管理資料庫結構變更

---

**參考文件**:
- `11-API-Design-Principles.md`: API 設計規範
- `17-Cache-Strategy.md`: Redis 快取策略
- `18-Idempotency-Design.md`: 冪等性設計

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
