# Implementation Plan: 新增訂單頁面

**Branch**: `002-create-order` | **Date**: 2025-12-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/002-create-order/spec.md`

## Summary

實作特殊訂單 (Special Order, SO) 新增頁面，整合 5 個 Bounded Context：Order、Member、Catalog、Pricing、Fulfillment。核心功能包含：
- 會員資訊查詢與臨時卡處理
- 6 層商品資格驗證
- 12 步驟計價流程（含會員折扣 Type 0/1/2）
- 運送/備貨方式相容性驗證
- 安裝服務與工種指派
- 優惠券與紅利點數折抵

技術方案採用 Pragmatic DDD，Order 為聚合根，使用 MyBatisGenerator 產生基礎 Mapper/Entity，複雜計價邏輯參考既有系統 `C:/projects/som` 實作。

## Technical Context

**Language/Version**: Java 21+ (Backend), TypeScript 5.9+ (Frontend)
**Primary Dependencies**:
- Backend: Spring Boot 3.x, Spring Security OAuth2, MyBatis 3.x, Oracle JDBC
- Frontend: Angular 21+, keycloak-angular, RxJS
**Storage**: Oracle 21c (Production), H2 (Development)
**Testing**: JUnit 5, Mockito (Backend), Jest/Karma, Playwright (Frontend/E2E)
**Target Platform**: Web Application (SPA + REST API)
**Project Type**: Web (frontend + backend)
**Performance Goals**:
- 價格試算 API ≤ 3 秒 (500 筆明細以內)
- 商品驗證 API ≤ 500ms
- 訂單建立 API ≤ 2 秒
**Constraints**:
- 訂單明細限制 1000 筆 (Oracle IN 語法限制)
- 試算限制 500 筆 (可配置)
- Stateless API (Keycloak Token 驗證)
**Scale/Scope**: 門市人員使用，單店同時約 5-10 人操作

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Pre-Phase 0 Check

| 原則 | 狀態 | 說明 |
|------|------|------|
| I. Pragmatic DDD | ✅ PASS | Order 為聚合根，避免過度設計 |
| II. 3-Table Rule | ✅ PASS | 核心資料表 TBL_ORDER_MAST, TBL_ORDER_DETL, TBL_ORDER_COMPUTE 符合 3 表規則 |
| III. KISS Principle | ✅ PASS | 優先參考既有系統實作，不重複造輪 |
| IV. Database Schema Documentation | ✅ PASS | 將查閱 docs/tables/*.html |
| V. Legacy Codebase Reference | ✅ PASS | 參考 C:/projects/som 既有實作 |
| VI. Stateless Backend | ✅ PASS | API 無狀態，Keycloak Token 驗證 |
| VII. Playwright Verification | ✅ PASS | E2E 測試與截圖驗證 |
| VIII. MyBatis Generator | ✅ PASS | 自動產生 Mapper/Entity，CustomMapper 僅用於複雜查詢 |
| IX. No Lombok | ✅ PASS | 使用 Java Records 作為 DTO |
| X. Data Class Convention | ✅ PASS | Records for DTO, Class for Entity |
| XI. OpenAPI RESTful API | ✅ PASS | springdoc-openapi 自動產生文件 |
| XII. Angular 21+ Standard | ✅ PASS | Standalone Components, Signals, 新控制流語法 |
| XIII. Code Coverage | ✅ PASS | 目標 ≥ 80% 覆蓋率 |

### Gate Result: **PASS** - 可進入 Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/002-create-order/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
│   └── order-api.yaml
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/tgfc/som/
│   ├── order/                    # Order Context (核心領域)
│   │   ├── controller/           # REST Controllers
│   │   │   └── OrderController.java
│   │   ├── service/              # Application Services
│   │   │   ├── OrderService.java
│   │   │   └── OrderPricingService.java
│   │   ├── domain/               # Domain Services & Value Objects
│   │   │   ├── OrderAggregate.java
│   │   │   ├── OrderLine.java
│   │   │   ├── PriceCalculation.java
│   │   │   └── MemberDiscount.java
│   │   └── dto/                  # Request/Response DTOs (Records)
│   │       ├── CreateOrderRequest.java
│   │       ├── OrderResponse.java
│   │       └── CalculationResponse.java
│   ├── member/                   # Member Context (支援領域)
│   │   ├── controller/
│   │   ├── service/
│   │   └── dto/
│   ├── catalog/                  # Catalog Context (支援領域)
│   │   ├── controller/
│   │   ├── service/
│   │   │   └── ProductEligibilityService.java
│   │   └── dto/
│   ├── pricing/                  # Pricing Context (支援領域)
│   │   └── service/
│   │       └── MemberDiscountService.java
│   ├── fulfillment/              # Fulfillment Context (支援領域)
│   │   └── service/
│   ├── mapper/                   # MyBatisGenerator 產生
│   ├── mapper/custom/            # 手動撰寫的 CustomMapper
│   ├── entity/                   # MyBatisGenerator 產生
│   └── common/
│       ├── config/
│       └── exception/
└── src/test/java/com/tgfc/som/
    ├── order/
    ├── member/
    ├── catalog/
    └── pricing/

frontend/
├── src/app/
│   ├── features/
│   │   └── order/
│   │       ├── pages/
│   │       │   └── create-order/
│   │       │       ├── create-order.component.ts
│   │       │       └── create-order.component.html
│   │       ├── components/
│   │       │   ├── member-info/
│   │       │   ├── product-list/
│   │       │   ├── calculation-summary/
│   │       │   └── delivery-options/
│   │       ├── services/
│   │       │   ├── order.service.ts
│   │       │   └── member.service.ts
│   │       └── order.routes.ts
│   ├── shared/
│   │   ├── components/
│   │   │   ├── toast/
│   │   │   └── skeleton/
│   │   └── services/
│   └── core/
└── e2e/
    ├── tests/
    │   └── order/
    │       └── create-order.spec.ts
    └── screenshots/
```

**Structure Decision**: 採用 Web Application 結構，前後端分離。Backend 依 DDD Bounded Context 組織，Frontend 依 Feature 組織。

## Complexity Tracking

> **無違規需要記錄** - 所有設計符合 Constitution 原則

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| (none) | - | - |

