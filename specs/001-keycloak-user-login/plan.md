# Implementation Plan: Keycloak 使用者登入與系統選擇

**Branch**: `001-keycloak-user-login` | **Date**: 2025-12-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-keycloak-user-login/spec.md`

## Summary

實作 Keycloak 整合的使用者登入功能，包含 6-checkpoint 驗證、主店別/支援店別選擇、系統別選擇（SO/TTS/APP），以及系統首頁導航列。

**技術方案**:
- **Frontend**: Angular 21+ (Standalone, Signals) + keycloak-angular
- **Backend**: Spring Boot 3.x + Spring Security OAuth2 Resource Server
- **State**: 前端 Local Storage（Stateless Backend）
- **單一分頁限制**: BroadcastChannel API + LocalStorage

## Technical Context

**Language/Version**: Java 21+ (Backend), TypeScript 5.9+ (Frontend)
**Primary Dependencies**: Spring Boot 3.x, Spring Security OAuth2, MyBatis, Angular 21, keycloak-angular 20.0.0
**Storage**: Oracle 21c (Production), H2 (Development/SIT)
**Testing**: JUnit 5 (Backend), Playwright (E2E)
**Target Platform**: Web Browser (Chrome, Edge, Firefox)
**Project Type**: Web application (frontend + backend)
**Performance Goals**: 登入流程 < 30 秒, 支援 500 並行使用者
**Constraints**: Stateless Backend, Single Tab Restriction, 60-minute Session Timeout
**Scale/Scope**: ~8 主要元件/服務, 3 資料表 (TBL_USER, TBL_USER_MAST_STORE, TBL_USER_STORE)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Pragmatic DDD | ✅ Pass | 使用輕量級 Domain Service 處理 6-checkpoint 驗證 |
| II. 3-Table Rule | ✅ Pass | 僅涉及 3 個 Table (TBL_USER, TBL_USER_MAST_STORE, TBL_USER_STORE) |
| III. KISS Principle | ✅ Pass | 簡單的驗證流程，無過度抽象 |
| IV. Database Schema Documentation | ✅ Pass | 已查閱 docs/tables/ 確認欄位定義 |
| V. Legacy Codebase Reference | ✅ Pass | 已參考 C:/projects/som 的登入流程 |
| VI. Stateless Backend Architecture | ✅ Pass | 前端 Local Storage 管理 Token，後端不維護 Session |
| VII. Playwright Verification | ⏳ Pending | 實作後需執行 E2E 測試 |
| VIII. MyBatis Generator Pattern | ✅ Pass | 使用 MyBatisGenerator 產生 Mapper/Entity |
| IX. No Lombok Policy | ✅ Pass | Entity 手動撰寫 getter/setter |
| X. Data Class Convention | ✅ Pass | Request/Response 使用 Java Records |
| XI. OpenAPI RESTful API Standard | ✅ Pass | API 遵循 RESTful 命名與 HTTP 狀態碼規範 |
| XII. Angular 21+ Frontend Standard | ✅ Pass | 使用 Standalone, Signals, inject(), OnPush |

## Project Structure

### Documentation (this feature)

```text
specs/001-keycloak-user-login/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification (Complete)
├── research.md          # Phase 0 output (Complete)
├── data-model.md        # Phase 1 output (Complete)
├── quickstart.md        # Phase 1 output (Complete)
├── contracts/           # Phase 1 output
│   └── auth-api.yaml    # API contracts (Complete)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/tgfc/som/
│   ├── auth/                        # Authentication module
│   │   ├── controller/              # REST Controllers
│   │   │   └── AuthController.java
│   │   ├── service/                 # Application Services
│   │   │   └── AuthApplicationService.java
│   │   └── domain/                  # Domain Services
│   │       └── UserValidationService.java
│   ├── store/                       # Store module
│   │   ├── controller/
│   │   │   └── StoreController.java
│   │   └── service/
│   │       └── StoreQueryService.java
│   ├── channel/                     # Channel module
│   │   ├── controller/
│   │   │   └── ChannelController.java
│   │   └── service/
│   │       └── ChannelQueryService.java
│   ├── mapper/                      # MyBatisGenerator Mappers
│   ├── entity/                      # MyBatisGenerator Entities
│   └── common/                      # Shared components
│       ├── config/                  # Security, MyBatis configs
│       │   └── SecurityConfig.java
│       └── dto/                     # Request/Response Records
└── src/main/resources/
    └── application.properties

frontend/
├── src/app/
│   ├── auth/                        # Authentication module
│   │   ├── components/
│   │   │   ├── login/               # Login component
│   │   │   ├── store-selection/     # Store selection
│   │   │   ├── channel-selection/   # Channel selection
│   │   │   └── validation-error/    # Error display
│   │   ├── services/
│   │   │   ├── auth.service.ts      # Auth state management
│   │   │   ├── auth.interceptor.ts  # HTTP interceptor
│   │   │   └── tab-manager.service.ts # Single tab control
│   │   ├── guards/
│   │   │   └── auth.guard.ts
│   │   └── models/
│   │       └── index.ts             # Interfaces
│   ├── core/                        # Core module
│   │   └── layout/
│   │       ├── nav-bar/             # Navigation bar
│   │       └── header/              # Header with user info
│   ├── shared/                      # Shared module
│   └── app.config.ts                # Keycloak configuration
└── e2e/                             # Playwright tests
    ├── tests/
    │   └── auth/
    └── screenshots/
```

**Structure Decision**: Web application 結構（frontend + backend），符合前後端分離架構需求。

## Complexity Tracking

> **No violations detected** - All design decisions comply with Constitution principles.

| Principle | Status | Justification |
|-----------|--------|---------------|
| 3-Table Rule | ✅ Compliant | TBL_USER, TBL_USER_MAST_STORE, TBL_USER_STORE (exactly 3 tables) |
| CustomMapper | ⚠️ May Need | 若需要複合查詢（JOIN 多表取得使用者完整權限資訊），需評估是否使用 CustomMapper |

## Phase Artifacts Status

| Phase | Artifact | Status | Path |
|-------|----------|--------|------|
| 0 | Research | ✅ Complete | [research.md](./research.md) |
| 1 | Data Model | ✅ Complete | [data-model.md](./data-model.md) |
| 1 | API Contracts | ✅ Complete | [contracts/auth-api.yaml](./contracts/auth-api.yaml) |
| 1 | Quickstart | ✅ Complete | [quickstart.md](./quickstart.md) |
| 2 | Tasks | ✅ Complete | [tasks.md](./tasks.md) |

## Next Steps

1. Execute tasks in dependency order (start with Phase 1: Setup)
2. Run `/speckit.implement` to begin automated task execution
3. Run Playwright verification after each user story completion
4. Update agent context with `update-agent-context.ps1 -AgentType claude`
