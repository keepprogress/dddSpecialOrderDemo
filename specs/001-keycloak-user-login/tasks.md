# Tasks: Keycloak 使用者登入與系統選擇

**Input**: Design documents from `/specs/001-keycloak-user-login/`
**Prerequisites**: plan.md, spec.md, data-model.md, research.md, contracts/auth-api.yaml

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/tgfc/som/`
- **Frontend**: `frontend/src/app/`
- **Tests**: `backend/src/test/java/`, `frontend/e2e/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create backend package structure per plan.md in `backend/src/main/java/com/tgfc/som/`
- [ ] T002 [P] Create frontend module structure per plan.md in `frontend/src/app/`
- [ ] T003 [P] Add Spring Security OAuth2 Resource Server dependency to `backend/pom.xml`
- [ ] T004 [P] Initialize Angular 21 project with standalone components in `frontend/`
- [ ] T005 [P] Add keycloak-angular dependency to `frontend/package.json`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

### MyBatis Generator & Entity

- [ ] T006 Create H2 schema script for 5 tables in `backend/src/main/resources/schema-h2.sql`
- [ ] T007 Create sample data script in `backend/src/main/resources/data-h2.sql`
- [ ] T008 Run MyBatisGenerator to produce Entity/Mapper for User, Channel, Store, UserMastStore, UserStore

### Backend Security Configuration

- [ ] T009 Configure Spring Security OAuth2 Resource Server in `backend/src/main/java/com/tgfc/som/common/config/SecurityConfig.java`
- [ ] T010 [P] Configure CORS settings in `backend/src/main/resources/application.yml`
- [ ] T011 [P] Add Keycloak JWT issuer-uri and jwk-set-uri to `backend/src/main/resources/application.yml`
- [ ] T012 Create GlobalExceptionHandler in `backend/src/main/java/com/tgfc/som/common/exception/GlobalExceptionHandler.java`

### Frontend Keycloak Configuration

> **Angular 21+ (Constitution XII)**: 使用 `inject()` 依賴注入、functional guards

- [ ] T013 Configure keycloak-angular provider in `frontend/src/app/app.config.ts`
- [ ] T014 [P] Create silent-check-sso.html in `frontend/src/`
- [ ] T015 Create AuthInterceptor (use `inject()` for dependencies) in `frontend/src/app/auth/services/auth.interceptor.ts`
- [ ] T016 Create AuthGuard (functional guard pattern) in `frontend/src/app/auth/guards/auth.guard.ts`

### Shared Infrastructure

- [ ] T017 Create UserDomainService with 6-checkpoint validation logic in `backend/src/main/java/com/tgfc/som/auth/domain/UserDomainService.java`
- [ ] T018 [P] Create ValidationResult record in `backend/src/main/java/com/tgfc/som/auth/domain/ValidationResult.java`
- [ ] T019 Create API Response DTOs using **Java Records** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/`
- [ ] T020 [P] Create frontend models (TypeScript interfaces matching API contracts) in `frontend/src/app/auth/models/`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Keycloak 身份驗證登入 (Priority: P1) MVP

**Goal**: Users can authenticate via Keycloak and pass 6-checkpoint validation

**Independent Test**: Enter valid Keycloak credentials, successfully login, see store selection page

### Backend Implementation for US1

- [ ] T021 [US1] Create AuthController with POST /auth/validate endpoint in `backend/src/main/java/com/tgfc/som/auth/controller/AuthController.java`
- [ ] T022 [US1] Create AuthService to orchestrate validation in `backend/src/main/java/com/tgfc/som/auth/service/AuthService.java`
- [ ] T023 [US1] Implement JWT username extraction utility in `backend/src/main/java/com/tgfc/som/common/util/JwtUtils.java`
- [ ] T024 [US1] Create UserValidationResponse **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/UserValidationResponse.java`
- [ ] T025 [US1] Create ValidationErrorResponse **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/ValidationErrorResponse.java`
- [ ] T026 [US1] Add health check endpoint GET /health in `backend/src/main/java/com/tgfc/som/common/controller/HealthController.java`

### Frontend Implementation for US1

> **Angular 21+ (Constitution XII)**: 所有元件必須使用 `standalone: true`、`inject()` 依賴注入、`@if/@for` 新控制流、`input()/output()` 信號式輸入輸出、`ChangeDetectionStrategy.OnPush`

- [ ] T027 [US1] Create LoginComponent (standalone, redirect to Keycloak) in `frontend/src/app/auth/components/login/login.component.ts`
- [ ] T028 [US1] Create AuthService for token management in `frontend/src/app/auth/services/auth.service.ts`
- [ ] T029 [US1] Create TabManagerService for single-tab restriction in `frontend/src/app/auth/services/tab-manager.service.ts`
- [ ] T030 [US1] Create TabBlockedComponent (standalone, OnPush) for "已在其他分頁開啟" message in `frontend/src/app/auth/components/tab-blocked/tab-blocked.component.ts`
- [ ] T031 [US1] Create ValidationErrorComponent (standalone, OnPush) for 6-checkpoint failure messages in `frontend/src/app/auth/components/validation-error/validation-error.component.ts`
- [ ] T032 [US1] Configure routes for login flow in `frontend/src/app/app.routes.ts`

### Integration for US1

- [ ] T033 [US1] Wire AuthInterceptor to call POST /auth/validate after Keycloak login
- [ ] T034 [US1] Handle validation errors and display appropriate messages
- [ ] T035 [US1] Store LoginContext in LocalStorage after successful validation

**Checkpoint**: User Story 1 should be fully functional - users can login via Keycloak and pass 6-checkpoint validation

---

## Phase 4: User Story 2 - 選擇主店別與支援店別 (Priority: P2)

**Goal**: Users can select master store and optional support stores

**Independent Test**: After login, see store selection page, select stores, proceed to system selection

### Backend Implementation for US2

- [ ] T036 [P] [US2] Create StoreController in `backend/src/main/java/com/tgfc/som/auth/controller/StoreController.java`
- [ ] T037 [P] [US2] Create StoreService in `backend/src/main/java/com/tgfc/som/auth/service/StoreService.java`
- [ ] T038 [US2] Implement GET /stores/mast endpoint to return user's master stores
- [ ] T039 [US2] Implement GET /stores/support endpoint to return user's support stores
- [ ] T040 [US2] Implement POST /stores/select endpoint to record store selection
- [ ] T041 [US2] Create MastStoreResponse **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/MastStoreResponse.java`
- [ ] T042 [P] [US2] Create StoreResponse **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/StoreResponse.java`
- [ ] T043 [P] [US2] Create StoreSelectionRequest **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/StoreSelectionRequest.java`

### Frontend Implementation for US2

> **Angular 21+ (Constitution XII)**: 使用 Signals 管理選擇狀態、`@if/@for` 渲染清單、`input()/output()` 傳遞資料

- [ ] T044 [US2] Create StoreSelectionComponent (standalone, OnPush, Signals) in `frontend/src/app/auth/components/store-selection/store-selection.component.ts`
- [ ] T045 [US2] Create StoreService for store API calls in `frontend/src/app/auth/services/store.service.ts`
- [ ] T046 [US2] Implement master store dropdown with "全區" option display (use `@for` with track)
- [ ] T047 [US2] Implement support store multi-select (conditional display with `@if`)
- [ ] T048 [US2] Implement auto-skip logic when only one master store exists (use `computed()`)
- [ ] T049 [US2] Update LocalStorage with selected stores

**Checkpoint**: User Story 2 should be fully functional - users can select stores after login

---

## Phase 5: User Story 3 - 選擇系統別 (Priority: P3)

**Goal**: Users can select system/channel (SO, TTS, APP) and complete login flow

**Independent Test**: After store selection, see system selection page, select system, enter home page

### Backend Implementation for US3

- [ ] T050 [P] [US3] Create ChannelController in `backend/src/main/java/com/tgfc/som/auth/controller/ChannelController.java`
- [ ] T051 [P] [US3] Create ChannelService in `backend/src/main/java/com/tgfc/som/auth/service/ChannelService.java`
- [ ] T052 [US3] Implement GET /channels endpoint to return user's authorized channels
- [ ] T053 [US3] Implement POST /channels/select endpoint to record channel selection
- [ ] T054 [US3] Create ChannelResponse **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/ChannelResponse.java`
- [ ] T055 [P] [US3] Create ChannelSelectionRequest **record** (Constitution X) in `backend/src/main/java/com/tgfc/som/auth/dto/ChannelSelectionRequest.java`

### Frontend Implementation for US3

> **Angular 21+ (Constitution XII)**: 使用 Signals 管理選擇狀態、`@for` 渲染按鈕清單、`computed()` 處理自動跳過邏輯

- [ ] T056 [US3] Create ChannelSelectionComponent (standalone, OnPush, Signals) in `frontend/src/app/auth/components/channel-selection/channel-selection.component.ts`
- [ ] T057 [US3] Create ChannelService for channel API calls in `frontend/src/app/auth/services/channel.service.ts`
- [ ] T058 [US3] Implement channel buttons based on SYSTEM_FLAG (use `@for` with track)
- [ ] T059 [US3] Implement auto-skip logic when only one channel authorized (use `computed()`)
- [ ] T060 [US3] Update LocalStorage with selected channel
- [ ] T061 [US3] Navigate to home page after channel selection

**Checkpoint**: User Story 3 should be fully functional - users can complete full login flow

---

## Phase 6: User Story 4 - 系統首頁與導航列 (Priority: P4)

**Goal**: Users see home page with navigation bar and user info after completing login flow

**Independent Test**: After system selection, see home page with nav bar, user info, logout/switch buttons

### Backend Implementation for US4

- [ ] T062 [US4] Add POST /auth/logout endpoint for audit logging in AuthController
- [ ] T063 [US4] Create AuditLogService for login/logout/selection events in `backend/src/main/java/com/tgfc/som/auth/service/AuditLogService.java`

### Frontend Implementation for US4

> **Angular 21+ (Constitution XII)**: 所有元件使用 standalone、OnPush、Signals；導航列使用 `@for` 渲染選單項目

- [ ] T064 [P] [US4] Create NavBarComponent (standalone, OnPush) in `frontend/src/app/core/layout/nav-bar/nav-bar.component.ts`
- [ ] T065 [P] [US4] Create HeaderComponent (standalone, OnPush, Signals for user info) in `frontend/src/app/core/layout/header/header.component.ts`
- [ ] T066 [US4] Create HomeComponent (standalone, OnPush) in `frontend/src/app/core/home/home.component.ts`
- [ ] T067 [US4] Implement nav bar with menu items using `@for`: 訂單管理, 退貨管理, 安運單管理, 主檔維護, 報表
- [ ] T068 [US4] Display user info in header using Signals: userName, selected store, selected channel
- [ ] T069 [US4] Implement logout button - clear LocalStorage and redirect to login
- [ ] T070 [US4] Implement "切換系統" button - navigate to store selection (keep Keycloak login)
- [ ] T071 [US4] Create PlaceholderComponent (standalone, OnPush) for "功能開發中" pages in `frontend/src/app/shared/components/placeholder/placeholder.component.ts`
- [ ] T072 [US4] Configure routes for all nav menu items

**Checkpoint**: User Story 4 should be fully functional - complete user experience from login to home page

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Session & Token Management

- [ ] T073 Implement 60-minute session timeout using keycloak-angular withAutoRefreshToken
- [ ] T074 [P] Handle token expiration - redirect to login with return URL preservation
- [ ] T075 Add token auto-refresh before expiration

### Error Handling & UX

- [ ] T076 [P] Create unified error handling for API calls in frontend
- [ ] T077 [P] Handle Keycloak service unavailable scenario
- [ ] T078 Add loading spinners for async operations

### Audit & Logging

- [ ] T079 Create audit log table schema and entity (TBL_AUDIT_LOG)
- [ ] T080 Log all login/logout events with timestamp, userId, IP address, result

### Quickstart Validation

- [ ] T081 Run quickstart.md validation for local development setup
- [ ] T082 [P] Update quickstart.md with any additional setup steps discovered

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Frontend flow depends on US1 login completion
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Frontend flow depends on US2 store selection completion
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Frontend flow depends on US3 channel selection completion

### Within Each User Story

- Backend DTOs before Controllers
- Backend Services before Controllers
- Frontend Services before Components
- Core implementation before integration

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Backend and Frontend tasks within same story can run in parallel
- DTOs marked [P] within same story can run in parallel
- Different user stories can be worked on in parallel by different team members (backend-only or with mock data)

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test Keycloak login + 6-checkpoint validation
5. Deploy/demo if ready - users can authenticate

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP - Authentication!)
3. Add User Story 2 → Test independently → Deploy/Demo (Store Selection)
4. Add User Story 3 → Test independently → Deploy/Demo (System Selection)
5. Add User Story 4 → Test independently → Deploy/Demo (Full UI)
6. Add Phase 7 → Final polish → Production ready

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A (Backend): US1 backend → US2 backend → US3 backend → US4 backend
   - Developer B (Frontend): US1 frontend → US2 frontend → US3 frontend → US4 frontend
3. Integration after each story phase

---

## Task Summary

| Phase | Task Count | Parallel Tasks |
|-------|------------|----------------|
| Phase 1: Setup | 5 | 4 |
| Phase 2: Foundational | 15 | 8 |
| Phase 3: US1 (P1) MVP | 15 | 0 |
| Phase 4: US2 (P2) | 14 | 6 |
| Phase 5: US3 (P3) | 12 | 4 |
| Phase 6: US4 (P4) | 11 | 2 |
| Phase 7: Polish | 10 | 4 |
| **Total** | **82** | **28** |

### Tasks per User Story

| User Story | Backend Tasks | Frontend Tasks | Total |
|------------|---------------|----------------|-------|
| US1 (P1) | 6 | 9 | 15 |
| US2 (P2) | 8 | 6 | 14 |
| US3 (P3) | 6 | 6 | 12 |
| US4 (P4) | 2 | 9 | 11 |

### MVP Scope

**Minimum tasks for working authentication**: Phase 1 (5) + Phase 2 (15) + Phase 3 US1 (15) = **35 tasks**

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
