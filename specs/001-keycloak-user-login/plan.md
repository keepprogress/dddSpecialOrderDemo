# Implementation Plan: Keycloak 使用者登入與系統選擇

**Branch**: `001-keycloak-user-login` | **Date**: 2025-12-17 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-keycloak-user-login/spec.md`

## Summary

實作 Keycloak 身份驗證整合，包含使用者登入、6-checkpoint 驗證、店別選擇（主店別/支援店別）、系統別選擇（SO/TTS/APP），以及系統首頁導航列。採用前後端分離架構，後端為 Stateless API，前端將登入資訊儲存於 Local Storage。

## Technical Context

**Language/Version**:
- Backend: Java 21+
- Frontend: TypeScript 5.9+

**Primary Dependencies**:
- Backend: Spring Boot 3.x+, Spring Security, MyBatis, MyBatisGenerator
- Frontend: Angular 21+, keycloak-angular

**Storage**:
- Production: Oracle 21c
- Development: H2
- Frontend: Local Storage (Token, 選擇狀態)

**Testing**:
- Backend: JUnit 5, Spring Boot Test
- Frontend: Jasmine, Karma
- E2E: Playwright

**Target Platform**: Web Application (桌面瀏覽器: Chrome, Edge, Firefox)

**Project Type**: Web Application (frontend + backend)

**Performance Goals**:
- 登入至首頁完整流程 < 30 秒
- 支援 500 並行登入使用者
- Token 過期偵測 < 2 秒

**Constraints**:
- Stateless 後端（無 Server-Side Session）
- 單一分頁限制（避免狀態衝突）
- 60 分鐘 Session Timeout

**Scale/Scope**:
- 4 個 User Stories
- 26 個 Functional Requirements
- 6 個資料表（TBL_USER, TBL_CHANNEL, TBL_STORE, TBL_USER_MAST_STORE, TBL_USER_STORE, 稽核日誌）

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Pragmatic DDD
- **Status**: PASS
- **評估**: 本功能涉及使用者驗證與權限檢查，適合使用輕量級 DDD。不需要複雜的 Aggregate 或 Domain Event。
- **設計決策**: 使用 Application Service 協調登入流程，Domain Service 處理 6-checkpoint 驗證邏輯。

### II. 3-Table Rule
- **Status**: PASS
- **評估**: 本功能涉及 5 個資料表（TBL_USER, TBL_CHANNEL, TBL_STORE, TBL_USER_MAST_STORE, TBL_USER_STORE）
- **例外說明**: 這些是設定檔/元資料類型的表，非歷史資料，資料量預估 < 10,000 筆，符合例外條件。
- **資料量評估**:
  - TBL_USER: ~1,000 筆（員工數）
  - TBL_CHANNEL: 3 筆（SO, TTS, APP）
  - TBL_STORE: ~500 筆（門市數）
  - TBL_USER_MAST_STORE: ~1,000 筆（1:1 對應使用者）
  - TBL_USER_STORE: ~2,000 筆（支援店別對應）

### III. KISS Principle
- **Status**: PASS
- **評估**: 設計保持簡單，無過度抽象。
- **設計決策**:
  - 使用 MyBatisGenerator 產生基本 Mapper/Entity
  - 不引入 CQRS，讀寫使用相同模型
  - Token 管理委託 keycloak-angular 處理

### IV. Database Schema Documentation
- **Status**: ACKNOWLEDGED
- **行動**: 實作前需查閱 `docs/tables/` 確認欄位定義

### V. Legacy Codebase Reference
- **Status**: ACKNOWLEDGED
- **行動**: 實作前需搜尋 `C:/projects/som` 了解既有驗證邏輯
- **注意**: Session 相關程式碼需轉換為 Token 解析

### VI. Stateless Backend Architecture
- **Status**: COMPLIANT
- **設計決策**:
  - 後端不維護 Session
  - 所有 API 需驗證 Keycloak Token
  - 例外端點: `/api/health`, `/actuator/**`

### VII. Playwright Verification
- **Status**: PLANNED
- **驗證點**:
  - User Story 1 完成後: 登入流程驗證
  - User Story 2 完成後: 店別選擇驗證
  - User Story 3 完成後: 系統別選擇驗證
  - User Story 4 完成後: 首頁導航列驗證

### VIII. MyBatis Generator Pattern
- **Status**: COMPLIANT
- **設計決策**:
  - 使用 MyBatisGenerator 產生 User, Store, Channel, UserMastStore, UserStore 的 Mapper/Entity
  - 基本 CRUD 使用自動產生的 Mapper
  - 若有複雜查詢需求（例如多表 JOIN），將建立對應的 CustomMapper
- **CustomMapper 評估**:
  - 目前設計不需要 CustomMapper，所有查詢可透過基本 Mapper 組合完成
  - 若效能測試發現瓶頸，再評估是否需要 CustomMapper

## Project Structure

### Documentation (this feature)

```text
specs/001-keycloak-user-login/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI specs)
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/tgfc/som/
│   ├── auth/                    # Authentication module
│   │   ├── controller/          # REST Controllers
│   │   ├── service/             # Application Services
│   │   └── domain/              # Domain Services
│   ├── mapper/                  # MyBatisGenerator 產生的 Mapper
│   │   ├── UserMapper.java
│   │   ├── StoreMapper.java
│   │   ├── ChannelMapper.java
│   │   ├── UserMastStoreMapper.java
│   │   └── UserStoreMapper.java
│   ├── mapper/custom/           # 手動撰寫的 CustomMapper (視需要)
│   ├── entity/                  # MyBatisGenerator 產生的 Entity
│   │   ├── User.java
│   │   ├── Store.java
│   │   ├── Channel.java
│   │   ├── UserMastStore.java
│   │   └── UserStore.java
│   ├── common/                  # Shared components
│   │   ├── config/              # Keycloak, Security, MyBatis configs
│   │   └── exception/           # Exception handlers
│   └── SomApplication.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-h2.yml
│   └── mapper/                  # MyBatisGenerator 產生的 Mapper XML
│       ├── UserMapper.xml
│       ├── StoreMapper.xml
│       └── ...
└── src/test/java/

frontend/
├── src/app/
│   ├── auth/                    # Authentication module
│   │   ├── components/          # Login, Store/System selection
│   │   ├── services/            # Auth service, Token interceptor
│   │   └── guards/              # Route guards
│   ├── core/                    # Core module
│   │   ├── layout/              # Nav bar, Header
│   │   └── services/            # API service
│   ├── shared/                  # Shared module
│   └── app.routes.ts
├── src/environments/
└── e2e/                         # Playwright tests
    ├── tests/
    │   ├── auth/
    │   ├── navigation/
    │   └── store-select/
    └── screenshots/
```

**Structure Decision**: Web Application 結構，前後端分離。後端使用 DDD 分層（但保持輕量），前端使用 Angular 模組化結構。

## Complexity Tracking

> **3-Table Rule 例外說明**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 5 資料表關聯 | 使用者權限需關聯使用者、主店別、支援店別、系統別資料 | 這些是設定檔類型資料，非歷史資料，資料量 < 10,000 筆，符合例外條件 |

## Phase 0 Output

See [research.md](./research.md) for:
- Keycloak Angular 整合最佳實踐
- 6-checkpoint 驗證實作方式
- 單一分頁限制實作方式
- Token 自動刷新機制

## Phase 1 Output

See:
- [data-model.md](./data-model.md) - Entity definitions
- [contracts/](./contracts/) - OpenAPI specifications
- [quickstart.md](./quickstart.md) - Local development setup
