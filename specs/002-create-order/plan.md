# Implementation Plan: 新增訂單頁面

**Branch**: `002-create-order` | **Date**: 2025-12-19 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-create-order/spec.md`

## Summary

建立特殊訂單 (Special Order, SO) 新增頁面，整合 5 個 Bounded Context（Order、Member、Catalog、Pricing、Fulfillment），實作完整的訂單建立流程：會員查詢 → 商品新增（6 層驗證）→ 安裝/運送服務配置 → 12 步驟價格試算 → 會員折扣（Type 0/1/2）→ 優惠券/紅利折抵 → 訂單提交。

## Technical Context

**Language/Version**: Java 21 (Backend), TypeScript 5.9+ (Frontend)
**Primary Dependencies**:
- Backend: Spring Boot 3.4.0, Spring Security OAuth2 Resource Server, MyBatis 3.0.4, springdoc-openapi 2.8.14, Jasypt 3.0.5
- Frontend: Angular 21+, keycloak-angular 20.0.0, RxJS 7.8, Playwright (E2E)

**Storage**: Oracle 21c (Production), H2 (Development)
**Testing**: Spring Boot Test, Playwright E2E
**Target Platform**: Web Application (SPA + REST API)
**Project Type**: Web (frontend + backend)
**Performance Goals**:
- 價格試算 API ≤ 3 秒（500 筆明細以內，可配置）
- 商品資格驗證 ≤ 500ms
- 訂單建立 ≤ 2 秒

**Constraints**:
- 無 Server-Side Session（Stateless API）
- 禁用 Lombok（Constitution IX）
- 試算限制：500 筆（TBL_PARM.SKU_COMPUTE_LIMIT）
- 結帳限制：1000 筆（TBL_PARM.ORDER_DETL_LIMIT）
- 5 秒內防重複提交

**Scale/Scope**: 5 個 Bounded Context、12 步驟計價流程、6 層商品驗證、4 種會員折扣類型

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Design Evaluation

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Pragmatic DDD** | ✅ PASS | Order 為聚合根，避免過度設計 |
| **II. 3-Table Rule** | ⚠️ REVIEW | 訂單核心 3 表（TBL_ORDER_MAST, TBL_ORDER_DETL, TBL_ORDER_COMPUTE），但可能需關聯商品/會員/工種等支援表 |
| **III. KISS Principle** | ✅ PASS | 依循既有計價邏輯，不重新發明 |
| **IV. Database Schema Documentation** | ✅ PASS | 參考 `docs/tables/*.html` |
| **V. Legacy Codebase Reference** | ✅ PASS | 參考 `C:/projects/som` 既有實作 |
| **VI. Stateless Backend Architecture** | ✅ PASS | 無 Session，JWT Token 驗證 |
| **VII. Playwright Verification** | ✅ PASS | E2E 測試驗證關鍵流程 |
| **VIII. MyBatis Generator Pattern** | ✅ PASS | 基礎 CRUD 自動產生，複雜查詢用 CustomMapper |
| **IX. No Lombok Policy** | ✅ PASS | 使用 Java Records 或手動 getter/setter |
| **X. Data Class Convention** | ✅ PASS | API DTO 使用 Records，Entity 使用傳統 Class |
| **XI. OpenAPI RESTful API Standard** | ✅ PASS | springdoc-openapi 自動產生文件 |
| **XII. Angular 21+ Frontend Standard** | ✅ PASS | Standalone Components、Signals、新控制流 |

### 3-Table Rule 評估

**核心訂單資料表（符合 3 表規則）**:
1. `TBL_ORDER_MAST` - 訂單主檔（客戶資訊、狀態、出貨店）
2. `TBL_ORDER_DETL` - 訂單明細（商品行項、安裝/運送服務）
3. `TBL_ORDER_COMPUTE` - 試算記錄（6 種 ComputeType）

**支援查詢（唯讀，不納入 3 表計算）**:
- `TBL_SKU_MAST` / `TBL_SKU_STORE` - 商品資格驗證
- `TBL_WORKTYPE_MAST` - 工種資訊
- `TBL_MEMBER_DISCOUNT` - 會員折扣設定
- `TBL_COUPON_MAST` - 優惠券主檔

**結論**: 寫入操作集中在 3 個核心表，支援表為唯讀查詢，符合憲法規定。

### Post-Design Evaluation

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Pragmatic DDD** | ✅ PASS | Order Aggregate 設計合理，Domain Service 封裝計價邏輯 |
| **II. 3-Table Rule** | ✅ PASS | 核心寫入 3 表，支援表唯讀查詢 |
| **III. KISS Principle** | ✅ PASS | 依循既有邏輯，Chain of Responsibility 驗證模式簡潔 |
| **IV. Database Schema Documentation** | ✅ PASS | data-model.md 包含完整資料表對應 |
| **V. Legacy Codebase Reference** | ✅ PASS | research.md 引用既有 BzSoServices 實作 |
| **VI. Stateless Backend Architecture** | ✅ PASS | 冪等鍵使用記憶體存儲，無 Session |
| **VII. Playwright Verification** | ✅ PASS | quickstart.md 包含 E2E 測試範例 |
| **VIII. MyBatis Generator Pattern** | ✅ PASS | 結構規劃 mapper/ 與 mapper/custom/ 分離 |
| **IX. No Lombok Policy** | ✅ PASS | data-model.md 使用 Java Records |
| **X. Data Class Convention** | ✅ PASS | DTO 使用 Records，Entity 使用傳統 Class |
| **XI. OpenAPI RESTful API Standard** | ✅ PASS | contracts/order-api.yaml 符合 OpenAPI 3.0 |
| **XII. Angular 21+ Frontend Standard** | ✅ PASS | quickstart.md 範例使用 Signals、新控制流 |

**結論**: 設計階段通過所有 Constitution 原則檢查，無需記錄違規項目。

## Project Structure

### Documentation (this feature)

```text
specs/002-create-order/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── order-api.yaml   # OpenAPI specification
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/tgfc/som/
│   ├── order/                      # Order Context (核心領域)
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── service/
│   │   │   ├── OrderService.java
│   │   │   └── OrderPricingService.java
│   │   ├── domain/
│   │   │   ├── Order.java          # Aggregate Root
│   │   │   ├── OrderLine.java      # Entity
│   │   │   └── valueobject/        # Value Objects
│   │   └── dto/                    # Request/Response Records
│   ├── member/                     # Member Context (支援領域)
│   │   ├── controller/
│   │   ├── service/
│   │   └── dto/
│   ├── catalog/                    # Catalog Context (支援領域)
│   │   ├── service/
│   │   │   ├── ProductEligibilityService.java
│   │   │   └── ProductServiceAssociationService.java
│   │   └── dto/
│   ├── pricing/                    # Pricing Context (支援領域)
│   │   ├── service/
│   │   │   ├── PriceCalculationService.java
│   │   │   └── MemberDiscountService.java
│   │   └── dto/
│   ├── fulfillment/                # Fulfillment Context (支援領域)
│   │   ├── service/
│   │   └── dto/
│   ├── mapper/                     # MyBatisGenerator 產生
│   ├── mapper/custom/              # 手動撰寫的 CustomMapper
│   ├── entity/                     # MyBatisGenerator 產生
│   └── common/
│       ├── config/
│       └── exception/
└── src/test/java/com/tgfc/som/

frontend/
├── src/app/
│   ├── features/
│   │   └── order/
│   │       ├── create-order/
│   │       │   ├── create-order.component.ts
│   │       │   ├── create-order.component.html
│   │       │   └── create-order.routes.ts
│   │       ├── components/
│   │       │   ├── member-info/
│   │       │   ├── product-list/
│   │       │   ├── service-config/
│   │       │   ├── price-calculation/
│   │       │   └── order-summary/
│   │       └── services/
│   │           └── order.service.ts
│   ├── shared/
│   │   └── components/
│   │       └── skeleton-loader/
│   └── core/
└── e2e/
    └── tests/
        └── order/
            └── create-order.spec.ts
```

**Structure Decision**: 採用 Web Application 結構，後端依 Bounded Context 分層，前端依功能模組組織。

## Complexity Tracking

> 本功能符合 Constitution 規範，無需記錄違規項目。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | - | - |

---

## Phase 0: Research Tasks

### Research Items

基於 Technical Context 分析，以下項目需要深入研究：

1. **12 步驟計價流程實作策略**
   - 既有系統計價邏輯位於 `BzSoServices.java:4367`
   - 需確認各步驟的依賴關係與可並行性
   - 確認會員折扣執行順序：Type 2 → 促銷 → Type 0 → Type 1 → 特殊會員

2. **商品資格 6 層驗證實作**
   - 驗證層級：格式 → 存在性 → 系統商品 → 稅別 → 銷售禁止 → 類別限制
   - 確認各層級的資料來源與查詢方式

3. **CRM API Mock 策略**
   - H00199 測試帳號返回寫死假資料
   - 其他帳號走正常流程或返回查無資料

4. **防重複提交機制**
   - 前端：按鈕禁用至回應完成
   - 後端：冪等鍵（idempotency key）5 秒內檢查

5. **外部服務降級策略**
   - 促銷引擎逾時 2 秒：跳過促銷計算
   - CRM 會員服務逾時 2 秒：使用已載入資料
   - 商品主檔逾時 1 秒：顯示錯誤（不可降級）

### Research Output Location

研究結果將輸出至 `specs/002-create-order/research.md`

---

## Phase 1: Design Artifacts

### Artifacts to Generate

1. **data-model.md** - 實體與值物件定義
   - Order Aggregate（訂單聚合根）
   - OrderLine Entity（訂單行項實體）
   - PriceCalculation Value Object（價格試算結果）
   - MemberDiscount Value Object（會員折扣）

2. **contracts/order-api.yaml** - OpenAPI 規格
   - POST /api/v1/orders - 建立訂單
   - POST /api/v1/orders/{orderId}/calculate - 價格試算
   - GET /api/v1/products/{skuNo}/eligibility - 商品資格驗證
   - GET /api/v1/members/{memberId} - 會員查詢
   - POST /api/v1/orders/{orderId}/coupons - 套用優惠券

3. **quickstart.md** - 開發環境設定與快速入門

---

## Reference Documents

- `docs/rewrite-specs/som-order-ddd-spec.md` - DDD 領域模型規格書
- `docs/rewrite-specs/04-Pricing-Calculation-Sequence.md` - 12 步驟計價流程
- `docs/rewrite-specs/05-Pricing-Member-Discount-Logic.md` - 會員折扣邏輯
- `docs/tables/*.html` - 資料表結構文件
- `C:/projects/som` - 既有系統程式碼參考
