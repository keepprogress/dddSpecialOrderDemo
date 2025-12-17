# OpenSpec vs Rewrite-Spec 衝突分析報告

**生成日期**: 2025-10-27
**分析範圍**: OpenSpec (既有系統規格) vs docs/rewrite-spec/ (重構計劃規格)

---

## 執行摘要

本報告比較 SOM 系統的兩套規格文檔：
- **OpenSpec** (`openspec/`): 描述現有系統的實際狀況（21 個規格文檔）
- **Rewrite-Spec** (`docs/rewrite-spec/`): 描述未來重構計劃（38 個規格文檔）

**關鍵發現**: 兩套規格存在**重大架構衝突**，rewrite-spec 提出的是完全重寫方案，而非漸進式演進。

---

## 1. 架構衝突 (Critical)

### 1.1 整體架構模式

| 面向 | OpenSpec (現狀) | Rewrite-Spec (計劃) | 衝突等級 |
|-----|----------------|-------------------|---------|
| **架構模式** | 單體 + 微服務混合 (Hybrid) | 完全微服務 (Pure Microservices) | 🔴 **高** |
| **數據庫策略** | 共享數據庫 (Shared Database) | Database-Per-Service | 🔴 **高** |
| **前端架構** | JSP/jQuery (Server-side) | Angular 8 SPA (Client-side) | 🔴 **高** |
| **服務間通訊** | 直接數據庫訪問 + API | 僅 API (無直接 DB 訪問) | 🔴 **高** |

#### OpenSpec 架構 (現狀):
```
┌─────────────────────────────────────────────────┐
│  Monolith (so-webapp)                           │
│  - Java 8, Spring MVC 4.2.9, Tomcat 7          │
│  - JSP/JSTL + jQuery                            │
│  ├── Controllers (SoController, etc.)           │
│  ├── Services (BzSoServices, etc.)              │
│  └── MyBatis DAOs                               │
└───────────────┬─────────────────────────────────┘
                │
                ├─► Microservices (som-docker)
                │   - Java 11, Spring Boot 2.3.0
                │   - som-emp-api (port 8087)
                │   - som-customer-api (port 8086)
                │   - som-batch-api (port 8187)
                │   - som-b2b-api (port 8087)
                │   - dde-platform-api (port 8086)
                │
                └─► Oracle DB (SOMDBA Schema)
                    - 100+ tables (共享)
                    - 直接訪問 (所有服務)
```

#### Rewrite-Spec 架構 (計劃):
```
┌─────────────────────────────────────────────────┐
│  Angular 8 Frontend                             │
│  - TypeScript + NgRx + Angular Material         │
│  - Pure client-side rendering                   │
└───────────────┬─────────────────────────────────┘
                │ REST API (JWT)
                ↓
┌─────────────────────────────────────────────────┐
│  API Gateway                                    │
│  - Spring Cloud Gateway / Kong                  │
└───────────────┬─────────────────────────────────┘
                │
    ┌───────────┼───────────┬───────────┐
    ↓           ↓           ↓           ↓
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ Order  │ │Pricing │ │Payment │ │Member  │
│Service │ │Service │ │Service │ │Service │
└───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘
    │          │          │          │
    ↓          ↓          ↓          ↓
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│Order DB│ │Price DB│ │Pay DB  │ │Mem DB  │
│(獨立)   │ │(獨立)   │ │(獨立)   │ │(獨立)   │
└────────┘ └────────┘ └────────┘ └────────┘
```

### 1.2 微服務劃分差異

| OpenSpec (現有 7 個微服務) | Rewrite-Spec (計劃 5 個微服務) | 對應關係 |
|--------------------------|------------------------------|---------|
| som-emp-api (員工 API) | → Order Service | ⚠️ **重新劃分** |
| som-customer-api (客戶 API) | → Member Service | ⚠️ **重新劃分** |
| som-batch-api (批次 API) | → *分散到各服務* | 🔴 **消失** |
| som-b2b-api (B2B API) | → *整合到其他服務* | 🔴 **消失** |
| som-report-api (報表) | → *未規劃* | 🔴 **消失** |
| som-auth (認證) | → API Gateway | ⚠️ **遷移** |
| dde-platform-api (物流) | → Inventory Service | ⚠️ **合併** |
| *(無)* | ← **Pricing Service** | ✅ **新增** |
| *(無)* | ← **Payment Service** | ✅ **新增** |

**衝突分析**:
- OpenSpec 按**使用者角色**劃分 (emp/customer)
- Rewrite-Spec 按**業務領域**劃分 (order/pricing/payment)
- 兩者**服務邊界完全不同**

---

## 2. 技術棧衝突 (Critical)

### 2.1 核心技術版本

| 技術層 | OpenSpec (現狀) | Rewrite-Spec (計劃) | 升級難度 | 破壞性 |
|-------|----------------|-------------------|---------|-------|
| **Java** | 8 (單體) / 11 (微服務) | **17 LTS** | 🟡 中 | ⚠️ Breaking |
| **Spring** | 4.2.9 (單體) / Boot 2.3.0 (微服務) | **Boot 3.1.5** | 🔴 高 | 🔴 Breaking |
| **Jakarta EE** | javax.* | **jakarta.*** | 🔴 高 | 🔴 Breaking |
| **MyBatis** | 3.2.2 (單體) / 3.5.x (微服務) | **3.5.13** | 🟢 低 | ✅ Compatible |
| **Spring Security** | 無 (單體) / 5.x (微服務) | **6.1.5** | 🟡 中 | ⚠️ Breaking |

#### Java 8 → 17 破壞性變更:
```java
// ❌ OpenSpec (Java 8)
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

// ✅ Rewrite-Spec (Java 17 + Spring Boot 3)
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
```

**升級影響**:
- 所有 `javax.*` 套件需改為 `jakarta.*`
- 需重新編譯所有模組
- 依賴套件需全面更新

### 2.2 前端技術棧

| 面向 | OpenSpec | Rewrite-Spec | 遷移複雜度 |
|-----|----------|--------------|-----------|
| **視圖層** | JSP/JSTL | Angular 8 + TypeScript | 🔴 **完全重寫** |
| **狀態管理** | Session (server-side) | NgRx (client-side) | 🔴 **完全重寫** |
| **UI 框架** | Bootstrap 3 + jQuery | Angular Material | 🔴 **完全重寫** |
| **渲染模式** | Server-side rendering | Client-side SPA | 🔴 **完全重寫** |
| **打包工具** | Maven (JSP 打包到 WAR) | Angular CLI (npm build) | 🔴 **完全不同** |

#### 現有 JSP 架構 (OpenSpec):
```jsp
<!-- soCreate.jsp (252 行驗證邏輯) -->
<form action="/so/saveSo" method="POST">
  <%@ include file="subContactInfo.jsp" %>
  <%@ include file="soSKUSubPage.jsp" %>
  <script>
    // 252 行 JavaScript 驗證邏輯混在 JSP
    function validateOrder() { ... }
  </script>
</form>
```

#### 計劃 Angular 架構 (Rewrite-Spec):
```typescript
// order-create.component.ts
@Component({
  selector: 'app-order-create',
  templateUrl: './order-create.component.html'
})
export class OrderCreateComponent implements OnInit {
  constructor(
    private store: Store<AppState>,
    private orderService: OrderService
  ) {}

  onSubmit(): void {
    this.store.dispatch(OrderActions.createOrder({ order: this.orderForm.value }));
  }
}
```

**衝突**: 無法漸進式遷移，必須**整頁重寫**。

---

## 3. 數據庫架構衝突 (Critical)

### 3.1 數據庫隔離策略

| 策略 | OpenSpec | Rewrite-Spec | 影響 |
|-----|----------|--------------|------|
| **Schema 數量** | 2 個 (SOMDBA, DDEDBA) | 5+ 個 (每服務獨立) | 🔴 **重大變更** |
| **訪問模式** | 所有服務直接訪問 SOMDBA | 僅訪問自己的 Schema | 🔴 **重大變更** |
| **跨服務查詢** | 允許 (直接 JOIN) | 禁止 (僅透過 API) | 🔴 **重大變更** |
| **資料一致性** | ACID (同一 DB) | Eventual Consistency (Saga) | 🔴 **重大變更** |

#### OpenSpec 數據庫模式:
```sql
-- 所有服務共享 SOMDBA Schema
CREATE SCHEMA somdba;
USE somdba;

-- 單體 + 微服務都可直接訪問
TBL_ORDER           -- 訂單主檔 (所有服務可讀寫)
TBL_ORDER_DETL      -- 訂單明細 (所有服務可讀寫)
TBL_INSTALLATION    -- 安裝單 (所有服務可讀寫)
TBL_CRM_MEMBER      -- 會員快取 (所有服務可讀寫)
...100+ tables
```

#### Rewrite-Spec 數據庫模式:
```sql
-- Order Service Database
CREATE SCHEMA order_db;
USE order_db;
CREATE TABLE orders (...);           -- 僅 Order Service 訪問
CREATE TABLE order_items (...);      -- 僅 Order Service 訪問

-- Pricing Service Database
CREATE SCHEMA pricing_db;
USE pricing_db;
CREATE TABLE pricing_requests (...); -- 僅 Pricing Service 訪問
CREATE TABLE pricing_results (...);  -- 僅 Pricing Service 訪問

-- Member Service Database
CREATE SCHEMA member_db;
USE member_db;
CREATE TABLE members (...);          -- 僅 Member Service 訪問
```

### 3.2 表結構差異

| 表名 | OpenSpec | Rewrite-Spec | 衝突點 |
|-----|----------|--------------|-------|
| **訂單主檔** | TBL_ORDER (50+ 欄位) | orders (分拆到多表) | 🔴 欄位重新分配 |
| **訂單明細** | TBL_ORDER_DETL | order_items | ✅ 類似 |
| **計價明細** | TBL_ORDER_COMPUTE | pricing_computes | ⚠️ 遷移到 pricing_db |
| **安裝單** | TBL_INSTALLATION | installations | ⚠️ 遷移到 order_db |

#### 欄位分配衝突範例:

**OpenSpec TBL_ORDER**:
```sql
CREATE TABLE TBL_ORDER (
  ORDER_ID VARCHAR2(20),
  -- 訂單基本資訊
  STORE_ID VARCHAR2(10),
  CHANNEL_ID VARCHAR2(10),
  -- 會員資訊 (應屬於 Member Service)
  CARD_NO VARCHAR2(20),
  -- 金額計算 (應屬於 Pricing Service)
  POS_AMT NUMBER(10,2),
  MEMBER_DIS_AMT NUMBER(10,2),
  COUPON_DIS_AMT NUMBER(10,2),
  -- 付款資訊 (應屬於 Payment Service)
  PAID_AMT NUMBER(10,2),
  PAID_DATE DATE
  -- ... 50+ 欄位混在一起
);
```

**Rewrite-Spec 分拆**:
```sql
-- Order Service: orders
CREATE TABLE orders (
  order_id VARCHAR2(20),
  member_card_id VARCHAR2(20),  -- 外鍵參考 (不儲存會員明細)
  status VARCHAR2(2)
  -- 僅訂單核心欄位
);

-- Pricing Service: pricing_results
CREATE TABLE pricing_results (
  order_id VARCHAR2(20),        -- 外鍵
  subtotal NUMBER(10,2),
  discount NUMBER(10,2),
  total NUMBER(10,2)
  -- 僅計價結果
);

-- Payment Service: payments
CREATE TABLE payments (
  payment_id VARCHAR2(20),
  order_id VARCHAR2(20),        -- 外鍵
  paid_amt NUMBER(10,2),
  paid_date TIMESTAMP
  -- 僅付款資訊
);
```

**資料遷移挑戰**:
1. 如何拆分 TBL_ORDER 的 50+ 欄位？
2. 如何維護跨 Schema 的參照完整性？
3. 現有查詢都使用 JOIN，如何改為 API 呼叫？

---

## 4. API 設計衝突 (High)

### 4.1 API 風格差異

| 面向 | OpenSpec (現狀) | Rewrite-Spec (計劃) | 相容性 |
|-----|----------------|-------------------|-------|
| **API 風格** | RPC-like (混合) | RESTful (嚴格) | 🔴 **不相容** |
| **URL 結構** | 動詞導向 | 資源導向 | 🔴 **不相容** |
| **認證方式** | Session (單體) / JWT (微服務) | 統一 JWT | ⚠️ **需調整** |
| **回應格式** | 不統一 | 統一格式 | ⚠️ **需調整** |

#### OpenSpec API 範例 (som-emp-api):
```http
# RPC 風格 (現狀)
POST /api/emp/so/querySoList
POST /api/emp/so/getSoById
POST /api/emp/so/updateSoStatus
POST /api/emp/so/cancelSo
```

#### Rewrite-Spec API 範例:
```http
# RESTful 風格 (計劃)
GET    /api/v1/orders?status=VALID
GET    /api/v1/orders/{orderId}
PATCH  /api/v1/orders/{orderId}/status
POST   /api/v1/orders/{orderId}/cancel
```

**衝突**: 現有客戶端 (如果有) 需**完全改寫** API 呼叫。

### 4.2 回應格式差異

**OpenSpec (不統一)**:
```json
// 成功回應 (無固定格式)
{
  "orderId": "SO001",
  "status": "Y"
}

// 錯誤回應 (各 endpoint 不同)
{
  "error": "訂單不存在"
}
```

**Rewrite-Spec (統一格式)**:
```json
// 成功回應
{
  "success": true,
  "data": { "orderId": "SO001" },
  "timestamp": "2025-10-27T10:00:00Z"
}

// 錯誤回應
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "訂單不存在",
    "details": []
  },
  "timestamp": "2025-10-27T10:00:00Z"
}
```

---

## 5. 業務邏輯衝突 (Medium)

### 5.1 計價邏輯位置

| 面向 | OpenSpec | Rewrite-Spec | 衝突等級 |
|-----|----------|--------------|---------|
| **計價服務化** | 內嵌在 BzSoServices | 獨立 Pricing Service | ⚠️ |
| **計價快取** | 無 (每次重算) | Redis (TTL 5 分鐘) | ⚠️ |
| **會員折扣** | CRM 同步查詢 | 快取 + 異步更新 | ⚠️ |

**OpenSpec 計價流程**:
```java
// BzSoServices.doCalculate() - 650 行巨大方法
public void doCalculate(SoBean soBean) {
  // 步驟 1-12 全部在一個方法內
  // 1. 查詢 SKU 價格 (DB)
  // 2. 查詢會員卡折扣 (CRM API)
  // 3. 計算促銷折扣 (DB)
  // 4. 計算會員折扣
  // 5-12. ...

  // 無快取，每次重新計算
}
```

**Rewrite-Spec 計價流程**:
```java
// PricingService.calculatePrice()
@Cacheable(value = "pricing", key = "#request.orderId")
public PricingResult calculatePrice(PricingRequest request) {
  // 1. 檢查 Redis 快取
  // 2. 非同步查詢會員折扣 (不阻塞)
  // 3. 並行計算各促銷 (CompletableFuture)
  // 4. 合併結果
}
```

### 5.2 訂單狀態機

兩者**狀態定義相同**，但**轉換邏輯位置不同**:

| 狀態轉換 | OpenSpec | Rewrite-Spec |
|---------|----------|--------------|
| Draft → Quotation | BzSoServices | Order Service |
| Quotation → Effective | BzSoServices | Order Service |
| Effective → Paid | BzSoServices + BzPaymentServices | **Payment Service** (分離) |
| Paid → Closed | BzSoServices | **Saga Orchestrator** (新增) |

---

## 6. 部署架構衝突 (High)

### 6.1 部署模式

| 面向 | OpenSpec | Rewrite-Spec | 遷移難度 |
|-----|----------|--------------|---------|
| **部署單位** | WAR (單體) + JAR (微服務) | Docker Container | 🟡 中 |
| **容器化** | 部分微服務 (som-docker) | 全部容器化 | 🟡 中 |
| **編排工具** | 無 (手動部署) | **Kubernetes** | 🔴 高 |
| **服務發現** | 靜態 IP/Port | **K8s Service** | 🔴 高 |
| **負載均衡** | Nginx (外部) | **K8s Ingress** | 🟡 中 |

**OpenSpec 部署架構**:
```
Tomcat 7                      Docker Compose
├── so-webapp.war             ├── som-emp-api:8087
└── (單體應用)                 ├── som-customer-api:8086
                              └── som-batch-api:8187

手動部署:
1. mvn clean package
2. scp *.war server:/tomcat/webapps/
3. systemctl restart tomcat
```

**Rewrite-Spec 部署架構**:
```
Kubernetes Cluster
├── Namespace: som-prod
│   ├── Deployment: order-service (replicas: 3)
│   ├── Deployment: pricing-service (replicas: 5)  # 可獨立擴展
│   ├── Deployment: payment-service (replicas: 3)
│   └── StatefulSet: redis-cluster (6 pods)
└── Ingress: api-gateway
    └── Route: /api/v1/* → services

自動部署 (CI/CD):
1. git push → GitLab
2. GitLab CI → docker build
3. helm upgrade som-chart
4. Kubernetes rolling update
```

---

## 7. 測試策略衝突 (Medium)

### 7.1 測試覆蓋率

| 測試層級 | OpenSpec | Rewrite-Spec | Gap |
|---------|----------|--------------|-----|
| **Unit Tests** | 有限 (<30%) | 目標 ≥80% | 🔴 大幅提升 |
| **Integration Tests** | 手動 | 自動化 (TestContainers) | 🔴 新增 |
| **E2E Tests** | 無 | Cypress / Protractor | 🔴 新增 |
| **Contract Tests** | 無 | Pact (服務間契約) | 🔴 新增 |

**OpenSpec 測試現狀**:
```java
// 少量 JUnit 測試
@Test
public void testCreateOrder() {
  // 簡單測試
}
```

**Rewrite-Spec 測試要求**:
```java
// 1. Unit Test (Mockito)
@Test
void shouldCalculatePrice() {
  // Mock external dependencies
}

// 2. Integration Test (TestContainers)
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = ...;
}

// 3. Contract Test (Pact)
@Pact(consumer = "OrderService", provider = "PricingService")
void pricingContract() { ... }
```

---

## 8. 遷移風險分析

### 8.1 無法漸進式遷移的模組

| 模組 | 原因 | 風險等級 |
|-----|------|---------|
| **前端 (JSP → Angular)** | 技術棧完全不同 | 🔴 **極高** |
| **數據庫 (Shared → Per-Service)** | Schema 需重新設計 | 🔴 **極高** |
| **計價邏輯** | 邏輯分散在多處 | 🔴 **高** |
| **Session 管理** | Server-side → JWT | 🔴 **高** |

### 8.2 Big Bang vs Strangler Fig

**Rewrite-Spec 提出的是 Big Bang 重寫**:
- 5.5 個月 (22 週) 完全重寫
- 需要 15-20 人團隊
- 預算 600 萬/年

**OpenSpec 暗示的是 Strangler Fig 漸進演進**:
- 保留單體 (so-webapp)
- 逐步新增微服務 (som-emp-api, som-customer-api)
- 共存期間共享數據庫

**衝突**: 兩種策略**互斥**，需擇一執行。

---

## 9. 衝突優先級總結

### 🔴 Critical (立即解決)

1. **架構決策**: 選擇 Big Bang 或 Strangler Fig？
2. **數據庫策略**: Shared DB 或 Database-Per-Service？
3. **前端技術**: 保留 JSP 或切換到 Angular？
4. **微服務邊界**: 按角色 (OpenSpec) 或按領域 (Rewrite-Spec)？

### 🟡 High (3 個月內解決)

5. **API 風格**: RPC 或 RESTful？
6. **認證機制**: Session 或 JWT？
7. **部署方式**: Tomcat 或 Kubernetes？
8. **計價快取**: 無快取或 Redis？

### 🟢 Medium (6 個月內解決)

9. **測試策略**: 手動或自動化？
10. **監控系統**: 基礎日誌或 Prometheus + Grafana？
11. **CI/CD**: 手動部署或 Jenkins Pipeline？

---

## 10. 建議行動方案

### 選項 A: 採用 Rewrite-Spec (完全重寫)

**優點**:
- 技術債歸零
- 現代化架構 (微服務 + Angular)
- 易於維護和擴展

**缺點**:
- 高風險 (Big Bang)
- 需要大量資源 (15-20 人, 5.5 個月)
- 業務中斷風險
- 預算需求高 (600 萬/年)

**適用情境**:
- 有充足預算和人力
- 可接受業務暫停或並行開發
- 現有系統問題嚴重到必須重寫

---

### 選項 B: 調整 Rewrite-Spec (漸進式演進)

**調整建議**:

1. **保留單體核心**:
   - 不立即廢除 so-webapp
   - 新功能用微服務，舊功能保留單體

2. **階段式前端遷移**:
   - Phase 1: 新頁面用 Angular
   - Phase 2: 高頻頁面遷移
   - Phase 3: 剩餘頁面遷移

3. **延續共享數據庫**:
   - 短期: 繼續 Shared Database
   - 中期: 引入 API 邏輯層 (禁止直接跨服務 JOIN)
   - 長期: 逐步拆分 Schema

4. **統一微服務邊界**:
   - 融合 OpenSpec (角色) + Rewrite-Spec (領域)
   - 範例: `som-order-api` (領域) with `/emp/*` 和 `/customer/*` 端點 (角色)

**優點**:
- 降低風險
- 減少資源需求
- 業務持續運作

**缺點**:
- 技術債延續
- 架構混亂期 (2-3 年)
- 維護成本高

---

### 選項 C: 混合策略 (推薦)

**策略**:
1. **前端**: 全面改用 Angular (不可避免的 Big Bang)
2. **後端**: 漸進式微服務化 (Strangler Fig)
3. **數據庫**: 短期共享，長期分離

**實施計劃**:

**Phase 1 (3 個月)**: 基礎建設
- 建立 Angular 專案框架
- 部署 API Gateway
- 建立 CI/CD Pipeline
- 不改動現有業務邏輯

**Phase 2 (6 個月)**: 前端遷移
- 重寫 Angular 頁面 (對應現有 JSP)
- 現有後端 API 保持不變
- 並行運作 (JSP + Angular 路由)

**Phase 3 (9 個月)**: 後端重構
- 提取 Pricing Service (最獨立)
- 提取 Member Service (CRM 封裝)
- 保留訂單核心在單體

**Phase 4 (12 個月)**: 數據庫分離
- 逐步拆分 Schema
- 使用 Change Data Capture (CDC) 同步

---

## 11. 關鍵決策點

### 決策 1: 重寫 vs 重構？

**需考慮**:
- 現有系統壽命 (還能用幾年？)
- 技術債嚴重程度 (是否無法修復？)
- 團隊技術能力 (是否掌握新技術？)
- 業務變化速度 (是否需要快速迭代？)

**建議**: 進行 **PoC (Proof of Concept)** 驗證 Rewrite-Spec 可行性

### 決策 2: 微服務邊界如何劃分？

**需對齊**:
- OpenSpec (現有 7 個微服務)
- Rewrite-Spec (計劃 5 個微服務)
- 實際業務需求

**建議**: 召開架構評審會議，統一服務邊界定義

### 決策 3: 數據庫何時分離？

**需評估**:
- 跨服務 JOIN 查詢數量
- 交易一致性需求
- 遷移成本和風險

**建議**: 先引入 API 層（禁止直接跨服務 DB 訪問），再考慮物理分離

---

## 12. 結論

**核心發現**:
1. OpenSpec 描述的是**既有系統**（混合架構）
2. Rewrite-Spec 描述的是**理想未來**（純微服務）
3. 兩者之間存在**巨大鴻溝**，無法簡單橋接

**關鍵衝突**:
- 🔴 架構模式 (Hybrid vs Pure Microservices)
- 🔴 數據庫策略 (Shared vs Per-Service)
- 🔴 前端技術 (JSP vs Angular)
- 🔴 微服務邊界 (角色 vs 領域)

**建議**:
1. **立即行動**: 確定採用哪種策略 (A/B/C)
2. **對齊規格**: 統一 OpenSpec 和 Rewrite-Spec 的目標狀態
3. **制定路線圖**: 明確遷移路徑和時程
4. **建立 PoC**: 驗證關鍵技術可行性
5. **風險管理**: 準備回滾計劃

**下一步**:
- 召開架構決策會議（ADR - Architecture Decision Record）
- 選擇遷移策略並更新規格文檔
- 建立詳細的實施計劃（含風險緩解措施）

---

**報告結束**
