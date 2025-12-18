# Tasks: Keycloak 使用者登入與系統選擇

**Input**: Design documents from `/specs/001-keycloak-user-login/`

## Phase 1: Setup

- [x] T001 Verify project structure matches plan.md layout
- [x] T002 [P] Add Spring Security OAuth2 Resource Server dependencies in backend/pom.xml
- [x] T003 [P] Verify keycloak-angular 20.0.0 installed in frontend/package.json

## Phase 2: Foundational

- [x] T005 Configure SecurityConfig.java for OAuth2 Resource Server
- [x] T006 [P] Configure Keycloak JWT settings in application-sit.properties
- [x] T007 [P] Configure CORS settings in CorsConfig.java
- [x] T008 Run MyBatisGenerator for User, Store, Channel mappers
- [x] T009 [P] Create ApiResponse record
- [x] T010 [P] Create ErrorResponse record
- [x] T011 Create GlobalExceptionHandler
- [x] T012 Implement HealthController GET /api/health
- [x] T013 Configure Keycloak in app.config.ts
- [x] T014 [P] Create auth models in frontend/src/app/auth/models/index.ts
- [x] T016 Implement AuthInterceptor

## Phase 3: US1 - Keycloak Login (P1) MVP

- [x] T018 [P] [US1] Create UserValidationResponse record
- [x] T019 [P] [US1] Create ValidationErrorResponse record
- [x] T020 [US1] Implement UserValidationService (6-checkpoint)
- [x] T021 [US1] Implement AuthApplicationService
- [x] T022 [US1] Implement POST /api/auth/validate
- [x] T023 [US1] Implement AuthService with signals
- [x] T024 [US1] Implement authGuard
- [x] T025 [US1] Create LoginComponent
- [x] T026 [US1] Create ValidationErrorComponent
- [x] T027 [US1] Create ValidateComponent
- [x] T028 [US1] Implement TabManagerService
- [x] T029 [US1] Configure auth routes

## Phase 4: US2 - Store Selection (P2)

- [x] T030 [P] [US2] Create MastStoreResponse record
- [x] T031 [P] [US2] Create StoreResponse record
- [x] T032 [P] [US2] Create StoreSelectionRequest record
- [x] T033 [US2] Implement StoreQueryService
- [x] T034 [US2] Implement GET /api/stores/mast
- [x] T035 [US2] Implement GET /api/stores/support
- [x] T036 [US2] Implement POST /api/stores/select
- [x] T037 [US2] Create StoreService
- [x] T038 [US2] Create StoreSelectionComponent
- [x] T041 [US2] Add auto-skip logic
- [x] T042 [US2] Add route /store-selection

## Phase 5: US3 - Channel Selection (P3)

- [x] T043 [P] [US3] Create ChannelResponse record
- [x] T044 [P] [US3] Create ChannelSelectionRequest record
- [x] T045 [US3] Implement ChannelQueryService
- [x] T046 [US3] Implement GET /api/channels
- [x] T047 [US3] Implement POST /api/channels/select
- [x] T048 [US3] Create ChannelService
- [x] T049 [US3] Create ChannelSelectionComponent
- [x] T050 [US3] Add auto-skip logic
- [x] T051 [US3] Add route /channel-selection
- [x] T052 [US3] Update AuthService with selectedChannel

## Phase 6: US4 - Homepage (P4)

- [x] T053 [US4] Implement POST /api/auth/logout
- [x] T054 [US4] Create NavBarComponent
- [x] T055 [US4] Create HeaderComponent
- [x] T056 [US4] Add logout functionality
- [x] T057 [US4] Add switch system button
- [x] T058 [US4] Create MainLayoutComponent
- [x] T059 [US4] Create HomeComponent
- [x] T060 [US4] Create PlaceholderComponent
- [x] T061 [US4] Configure main app routes

## Phase 7: Polish

- [x] T062 Create AuditLogEntity via MyBatisGenerator
- [x] T063 [P] Implement AuditLogService
- [x] T064 Add audit logging
- [x] T065 [P] Create auth.spec.ts (Playwright)
- [x] T066 [P] Create store-selection.spec.ts
- [x] T067 [P] Create channel-selection.spec.ts
- [x] T068 [P] Create homepage.spec.ts
- [ ] T069 Run full Playwright test suite
- [ ] T070 Verify quickstart.md scenarios
- [x] T071 Code cleanup
- [x] T072 Update CLAUDE.md

## Summary

Total: 72 tasks
MVP (US1): 29 tasks (T001-T029)
US1: 12 tasks | US2: 13 tasks | US3: 10 tasks | US4: 9 tasks

**Completed**: 70/72 tasks
**Remaining**: T069 (Run Playwright tests), T070 (Verify quickstart.md scenarios)
