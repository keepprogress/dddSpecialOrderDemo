# Tasks: 新增訂單頁面 (Create Order)

**Input**: Design documents from `/specs/002-create-order/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/order-api.yaml, research.md, quickstart.md

**Tests**: E2E tests with Playwright (per Constitution VII)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/tgfc/som/`
- **Frontend**: `frontend/src/app/`
- **Tests**: `backend/src/test/java/`, `frontend/e2e/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and package structure for 5 Bounded Contexts

- [x] T001 Create Order Context package structure in `backend/src/main/java/com/tgfc/som/order/{controller,service,domain,dto}/`
- [x] T002 [P] Create Member Context package structure in `backend/src/main/java/com/tgfc/som/member/{controller,service,dto}/`
- [x] T003 [P] Create Catalog Context package structure in `backend/src/main/java/com/tgfc/som/catalog/{service,dto}/`
- [x] T004 [P] Create Pricing Context package structure in `backend/src/main/java/com/tgfc/som/pricing/{service,dto}/`
- [x] T005 [P] Create Fulfillment Context package structure in `backend/src/main/java/com/tgfc/som/fulfillment/{service,dto}/`
- [x] T006 Create frontend order feature structure in `frontend/src/app/features/order/{create-order,components,services}/`
- [x] T007 [P] Create shared skeleton-loader component in `frontend/src/app/shared/components/skeleton-loader/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Value Objects (Backend)

- [x] T008 [P] Create Money value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/Money.java`
- [x] T009 [P] Create OrderId value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/OrderId.java`
- [x] T010 [P] Create ProjectId value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/ProjectId.java`
- [x] T011 [P] Create LineId value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/LineId.java`
- [x] T012 [P] Create DeliveryAddress value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/DeliveryAddress.java`
- [x] T013 [P] Create Customer value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/Customer.java`

### Enums (Backend)

- [x] T014 [P] Create OrderStatus enum in `backend/src/main/java/com/tgfc/som/order/domain/OrderStatus.java` (值：1=草稿/2=報價/3=已付款/4=有效/5=結案/6=作廢，參考 DataExchangeItf.java)
- [x] T015 [P] Create DeliveryMethod enum in `backend/src/main/java/com/tgfc/som/order/domain/DeliveryMethod.java` (值：N=運送/D=純運/V=直送/C=當場自取/F=宅配/P=下次自取，參考 SoConstant.java)
- [x] T016 [P] Create StockMethod enum in `backend/src/main/java/com/tgfc/som/order/domain/StockMethod.java` (值：X=現貨/Y=訂購，資料庫欄位 TRADE_STATUS)
- [x] T017 [P] Create TaxType enum in `backend/src/main/java/com/tgfc/som/order/domain/TaxType.java` (值：0=零稅/1=應稅/2=免稅，參考 CommonConstant.java)
- [x] T018 [P] Create MemberDiscountType enum in `backend/src/main/java/com/tgfc/som/member/domain/MemberDiscountType.java` (值：0=Discounting折價/1=DownMargin下降/2=CostMarkup成本加成，參考 SoConstant.java)

### Common Infrastructure (Backend)

- [x] T019 [P] Create GlobalExceptionHandler in `backend/src/main/java/com/tgfc/som/common/exception/GlobalExceptionHandler.java`
- [x] T020 [P] Create ErrorResponse record in `backend/src/main/java/com/tgfc/som/common/exception/ErrorResponse.java`
- [x] T021 [P] Create BusinessException in `backend/src/main/java/com/tgfc/som/common/exception/BusinessException.java`
- [x] T022 [P] Create DuplicateSubmissionException in `backend/src/main/java/com/tgfc/som/common/exception/DuplicateSubmissionException.java`
- [x] T023 Create IdempotencyService in `backend/src/main/java/com/tgfc/som/common/service/IdempotencyService.java` (5-sec memory cache, ConcurrentHashMap + ScheduledExecutor 清理過期 key)
- [x] T024 [P] Configure Logback JSON encoder in `backend/src/main/resources/logback-spring.xml` (JSON 格式需包含：operatorId, timestamp, actionType, orderId, duration, errorCode)

### External Service Resilience (Backend)

- [x] T024a [P] Create ExternalServiceConfig in `backend/src/main/java/com/tgfc/som/common/config/ExternalServiceConfig.java` (定義 timeout 設定：CRM 2秒、促銷引擎 2秒、商品主檔 1秒)
- [x] T024b Create timeout/fallback handling for CRM service in `backend/src/main/java/com/tgfc/som/member/service/MemberService.java` (2-sec timeout, use cached data on timeout)
- [x] T024c [P] Create timeout/fallback handling for promotion engine in `backend/src/main/java/com/tgfc/som/pricing/service/PriceCalculationService.java` (2-sec timeout, skip promotion on timeout, set warning flag)

### MyBatis Mappers (Backend)

- [x] T025 Run MyBatisGenerator for TBL_ORDER_MAST in `backend/src/main/java/com/tgfc/som/mapper/` (手動建立 Entity + Mapper)
- [x] T026 [P] Run MyBatisGenerator for TBL_ORDER_DETL in `backend/src/main/java/com/tgfc/som/mapper/` (手動建立 Entity + Mapper)
- [x] T027 [P] Run MyBatisGenerator for TBL_ORDER_COMPUTE in `backend/src/main/java/com/tgfc/som/mapper/` (手動建立 Entity + Mapper)
- [x] T028 [P] Run MyBatisGenerator for TBL_SKU_MAST in `backend/src/main/java/com/tgfc/som/mapper/` (手動建立 Entity + Mapper)
- [x] T029 [P] Create CustomOrderMapper in `backend/src/main/java/com/tgfc/som/mapper/custom/CustomOrderMapper.java`

### Frontend Core (Shared)

- [x] T030 Create order.service.ts in `frontend/src/app/features/order/services/order.service.ts`
- [x] T031 [P] Create member.service.ts in `frontend/src/app/features/order/services/member.service.ts`
- [x] T032 [P] Create product.service.ts in `frontend/src/app/features/order/services/product.service.ts`
- [x] T033 [P] Create order models/interfaces in `frontend/src/app/features/order/models/order.model.ts`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - 建立基本訂單 (Priority: P1) 🎯 MVP

**Goal**: 門市人員可建立包含商品、客戶資訊的基本訂單，完成價格試算後提交

**Independent Test**: 完整測試「輸入會員 → 新增商品 → 試算 → 提交」流程

### Domain Layer (Backend)

- [x] T034 [US1] Create Order aggregate root in `backend/src/main/java/com/tgfc/som/order/domain/Order.java`
- [x] T035 [US1] Create OrderLine entity in `backend/src/main/java/com/tgfc/som/order/domain/OrderLine.java`
- [x] T036 [P] [US1] Create PriceCalculation value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/PriceCalculation.java`

### DTOs (Backend)

- [x] T037 [P] [US1] Create CreateOrderRequest record in `backend/src/main/java/com/tgfc/som/order/dto/CreateOrderRequest.java`
- [x] T038 [P] [US1] Create OrderResponse record in `backend/src/main/java/com/tgfc/som/order/dto/OrderResponse.java`
- [x] T039 [P] [US1] Create AddOrderLineRequest record in `backend/src/main/java/com/tgfc/som/order/dto/AddOrderLineRequest.java`
- [x] T040 [P] [US1] Create OrderLineResponse record in `backend/src/main/java/com/tgfc/som/order/dto/OrderLineResponse.java`
- [x] T041 [P] [US1] Create CalculationResponse record in `backend/src/main/java/com/tgfc/som/order/dto/CalculationResponse.java`
- [x] T042 [P] [US1] Create MemberResponse record in `backend/src/main/java/com/tgfc/som/member/dto/MemberResponse.java`
- [x] T042a [P] [US1] Create TempMemberRequest record in `backend/src/main/java/com/tgfc/som/member/dto/TempMemberRequest.java` (臨時卡：姓名、電話、地址)
- [x] T043 [P] [US1] Create EligibilityResponse record in `backend/src/main/java/com/tgfc/som/catalog/dto/EligibilityResponse.java`
- [x] T044 [P] [US1] Create ProductInfo record in `backend/src/main/java/com/tgfc/som/catalog/dto/ProductInfo.java`

### Services (Backend)

- [x] T045 [US1] Create OrderService in `backend/src/main/java/com/tgfc/som/order/service/OrderService.java` (createOrder, addLine, calculate, submit)
- [x] T046 [US1] Create MemberService with H00199 mock + temp card support in `backend/src/main/java/com/tgfc/som/member/service/MemberService.java` (H00199 返回假資料、TEMP001 返回臨時卡假資料、其他查無資料時允許建立臨時卡)
- [x] T047 [US1] Create ProductEligibilityService (6-layer validation) in `backend/src/main/java/com/tgfc/som/catalog/service/ProductEligibilityService.java`
- [x] T048 [US1] Create basic PriceCalculationService in `backend/src/main/java/com/tgfc/som/pricing/service/PriceCalculationService.java` (ComputeType 1-3 only)

### Controllers (Backend)

- [x] T049 [US1] Create OrderController in `backend/src/main/java/com/tgfc/som/order/controller/OrderController.java` (POST /orders, POST /lines, POST /calculate, POST /submit)
- [x] T050 [P] [US1] Create MemberController in `backend/src/main/java/com/tgfc/som/member/controller/MemberController.java` (GET /members/{memberId}, POST /members/temp 建立臨時卡)
- [x] T051 [P] [US1] Create ProductController in `backend/src/main/java/com/tgfc/som/catalog/controller/ProductController.java` (GET /products/{skuNo}/eligibility)

### Frontend Components

- [x] T052 [US1] Create create-order.component.ts in `frontend/src/app/features/order/create-order/create-order.component.ts` (main page with Signals)
- [x] T053 [US1] Create create-order.component.html in `frontend/src/app/features/order/create-order/create-order.component.html` (template with @if/@for)
- [x] T054 [P] [US1] Create member-info.component.ts in `frontend/src/app/features/order/components/member-info/member-info.component.ts` (含臨時卡輸入模式：查無會員時顯示「使用臨時卡」按鈕，切換至手動輸入姓名/電話/地址)
- [x] T055 [P] [US1] Create product-list.component.ts in `frontend/src/app/features/order/components/product-list/product-list.component.ts`
- [x] T056 [US1] Create create-order.routes.ts in `frontend/src/app/features/order/create-order/create-order.routes.ts`
- [x] T057 [US1] Add order routes to app.routes.ts in `frontend/src/app/app.routes.ts`

### E2E Test

- [x] T058 [US1] Create basic order E2E test in `frontend/e2e/tests/order/create-order-basic.spec.ts` (member lookup, add product, calculate, submit)
- [x] T058a [US1] Create temp card E2E test in `frontend/e2e/tests/order/create-order-temp-card.spec.ts` (查無會員 → 使用臨時卡 → 輸入資料 → 提交訂單)

**Checkpoint**: User Story 1 完成 - 可獨立測試基本訂單建立流程 (MVP Ready)

---

## Phase 4: User Story 2 - 設定安裝與運送服務 (Priority: P2)

**Goal**: 門市人員可為商品設定安裝服務與運送方式

**Independent Test**: 測試「新增商品 → 選擇運送方式 → 勾選安裝服務 → 指派工種」流程

### Domain Layer (Backend)

- [x] T059 [P] [US2] Create InstallationDetail value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/InstallationDetail.java`
- [x] T060 [P] [US2] Create DeliveryDetail value object in `backend/src/main/java/com/tgfc/som/order/domain/valueobject/DeliveryDetail.java`
- [x] T061 [P] [US2] Create WorkType record in `backend/src/main/java/com/tgfc/som/fulfillment/dto/WorkType.java`
- [x] T062 [P] [US2] Create WorkCategory enum in `backend/src/main/java/com/tgfc/som/fulfillment/domain/WorkCategory.java`
- [x] T063 [P] [US2] Create InstallationService record in `backend/src/main/java/com/tgfc/som/catalog/dto/InstallationService.java`

### Services (Backend)

- [x] T064 [US2] Create ProductServiceAssociationService in `backend/src/main/java/com/tgfc/som/catalog/service/ProductServiceAssociationService.java` (get installation services by category)
- [x] T065 [US2] Create WorkTypeService in `backend/src/main/java/com/tgfc/som/fulfillment/service/WorkTypeService.java`
- [x] T066 [US2] Extend OrderService with attachInstallation, attachDelivery methods in `backend/src/main/java/com/tgfc/som/order/service/OrderService.java`
- [x] T067 [US2] Add minimum wage validation to PriceCalculationService in `backend/src/main/java/com/tgfc/som/pricing/service/PriceCalculationService.java`

### DTOs (Backend)

- [x] T068 [P] [US2] Create UpdateOrderLineRequest record in `backend/src/main/java/com/tgfc/som/order/dto/UpdateOrderLineRequest.java`
- [x] T069 [P] [US2] Create WorkTypeResponse record in `backend/src/main/java/com/tgfc/som/fulfillment/dto/WorkTypeResponse.java`

### Controllers (Backend)

- [x] T070 [US2] Add PUT/DELETE /orders/{orderId}/lines/{lineId} to OrderController in `backend/src/main/java/com/tgfc/som/order/controller/OrderController.java`
- [x] T071 [P] [US2] Create WorkTypeController in `backend/src/main/java/com/tgfc/som/fulfillment/controller/WorkTypeController.java` (GET /worktypes)

### Frontend Components

- [x] T072 [US2] Create service-config.component.ts in `frontend/src/app/features/order/components/service-config/service-config.component.ts` (delivery method, work type, installation services)
- [x] T073 [US2] Create service-config.component.html in `frontend/src/app/features/order/components/service-config/service-config.component.html`
- [x] T074 [US2] Integrate service-config into product-list.component in `frontend/src/app/features/order/components/product-list/product-list.component.ts`

### Delivery/Stock Compatibility (EC-008)

- [x] T074a [US2] Create ToastService in `frontend/src/app/shared/services/toast.service.ts` (Signals-based, 3s auto-dismiss)
- [x] T074b [P] [US2] Create ToastComponent (standalone) in `frontend/src/app/shared/components/toast/toast.component.ts`
- [x] T074c [US2] Add delivery/stock compatibility validation using Signal effect in `frontend/src/app/features/order/components/service-config/service-config.component.ts`:
  - 選擇直送(V) → 備貨方式自動預設為訂購(Y)，無提示
  - 選擇當場自取(C) → 備貨方式自動預設為現貨(X)，無提示
  - 直送(V) + 手動改為現貨(X) → 自動切換回訂購(Y)，Toast 提示「直送只能訂購」
  - 當場自取(C) + 手動改為訂購(Y) → 自動切換回現貨(X)，Toast 提示「當場自取只能現貨」
- [x] T074d [US2] Add backend double-check for delivery/stock compatibility in `backend/src/main/java/com/tgfc/som/order/service/OrderService.java`

### E2E Test

- [x] T075 [US2] Create installation/delivery E2E test in `frontend/e2e/tests/order/create-order-service.spec.ts` (select delivery method, add installation service)
- [x] T075a [US2] Add delivery/stock compatibility E2E test cases in `frontend/e2e/tests/order/create-order-service.spec.ts`:
  - Test 直送(V) auto-sets 訂購(Y)
  - Test 當場自取(C) auto-sets 現貨(X)
  - Test Toast appears for invalid manual override

**Checkpoint**: User Story 2 完成 - 可獨立測試安裝與運送服務配置

---

## Phase 5: User Story 3 - 套用會員折扣 (Priority: P3)

**Goal**: 系統根據會員折扣類型自動計算折扣（Type 0/1/2）

**Independent Test**: 測試不同會員類型的折扣計算結果是否符合預期公式

### Domain Layer (Backend)

- [x] T076 [P] [US3] Create MemberDiscVO record in `backend/src/main/java/com/tgfc/som/pricing/dto/MemberDiscVO.java`
- [x] T077 [P] [US3] Create ComputeTypeVO record in `backend/src/main/java/com/tgfc/som/pricing/dto/ComputeTypeVO.java`

### Services (Backend)

- [x] T078 [US3] Create MemberDiscountService in `backend/src/main/java/com/tgfc/som/pricing/service/MemberDiscountService.java` (calculateType0, calculateType1, calculateType2, calculateSpecial)
- [x] T079 [US3] Implement full 12-step pricing flow in PriceCalculationService in `backend/src/main/java/com/tgfc/som/pricing/service/PriceCalculationService.java`
- [x] T080 [US3] Add Type 2 negative result handling (set to zero, send alert email) in `backend/src/main/java/com/tgfc/som/pricing/service/MemberDiscountService.java`

### Controllers (Backend)

- [x] T081 [US3] Extend CalculationResponse with memberDiscounts in `backend/src/main/java/com/tgfc/som/order/dto/CalculationResponse.java`

### Frontend Components

- [x] T082 [US3] Create price-calculation.component.ts in `frontend/src/app/features/order/components/price-calculation/price-calculation.component.ts` (display 6 ComputeTypes)
- [x] T083 [US3] Create price-calculation.component.html in `frontend/src/app/features/order/components/price-calculation/price-calculation.component.html`
- [x] T084 [US3] Integrate price-calculation into create-order.component in `frontend/src/app/features/order/create-order/create-order.component.ts`

### Unit Tests (Backend)

- [x] T085 [P] [US3] Create MemberDiscountServiceTest in `backend/src/test/java/com/tgfc/som/pricing/service/MemberDiscountServiceTest.java` (Type 0/1/2 calculation)
- [x] T086 [P] [US3] Create PriceCalculationServiceTest in `backend/src/test/java/com/tgfc/som/pricing/service/PriceCalculationServiceTest.java` (12-step flow)

### E2E Test

- [x] T087 [US3] Create member discount E2E test in `frontend/e2e/tests/order/create-order-discount.spec.ts` (Type 0 member discount calculation)

**Checkpoint**: User Story 3 完成 - 可獨立測試會員折扣計算

---

## Phase 6: User Story 4 - 套用優惠券與紅利點數 (Priority: P4)

**Goal**: 門市人員可為訂單套用優惠券並使用會員紅利點數折抵

**Independent Test**: 測試「選擇優惠券 → 驗證門檻 → 分攤折扣」與「查詢紅利 → 選擇折抵商品 → 計算折抵金額」流程

### Domain Layer (Backend)

- [ ] T088 [P] [US4] Create Coupon value object in `backend/src/main/java/com/tgfc/som/pricing/domain/Coupon.java`
- [ ] T089 [P] [US4] Create CouponValidation record in `backend/src/main/java/com/tgfc/som/pricing/dto/CouponValidation.java`
- [ ] T090 [P] [US4] Create BonusRedemption record in `backend/src/main/java/com/tgfc/som/pricing/dto/BonusRedemption.java`

### DTOs (Backend)

- [ ] T091 [P] [US4] Create ApplyCouponRequest record in `backend/src/main/java/com/tgfc/som/order/dto/ApplyCouponRequest.java`
- [ ] T092 [P] [US4] Create RedeemBonusRequest record in `backend/src/main/java/com/tgfc/som/order/dto/RedeemBonusRequest.java`

### Services (Backend)

- [ ] T093 [US4] Create CouponService in `backend/src/main/java/com/tgfc/som/pricing/service/CouponService.java` (validate, apply, allocate discount)
- [ ] T094 [US4] Create BonusService in `backend/src/main/java/com/tgfc/som/pricing/service/BonusService.java` (check points, redeem)
- [ ] T095 [US4] Extend Order aggregate with applyCoupon, redeemBonusPoints methods in `backend/src/main/java/com/tgfc/som/order/domain/Order.java`
- [ ] T096 [US4] Add coupon threshold validation (cap at product total, no refund) in `backend/src/main/java/com/tgfc/som/pricing/service/CouponService.java`

### Controllers (Backend)

- [ ] T097 [US4] Add POST /orders/{orderId}/coupons to OrderController in `backend/src/main/java/com/tgfc/som/order/controller/OrderController.java`
- [ ] T098 [US4] Add POST /orders/{orderId}/bonus to OrderController in `backend/src/main/java/com/tgfc/som/order/controller/OrderController.java`

### Frontend Components

- [ ] T099 [US4] Create order-summary.component.ts in `frontend/src/app/features/order/components/order-summary/order-summary.component.ts` (coupon input, bonus redemption)
- [ ] T100 [US4] Create order-summary.component.html in `frontend/src/app/features/order/components/order-summary/order-summary.component.html`
- [ ] T101 [US4] Integrate order-summary into create-order.component in `frontend/src/app/features/order/create-order/create-order.component.ts`

### E2E Test

- [ ] T102 [US4] Create coupon/bonus E2E test in `frontend/e2e/tests/order/create-order-coupon.spec.ts` (apply coupon, verify discount)

**Checkpoint**: User Story 4 完成 - 可獨立測試優惠券與紅利折抵

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Logging & Observability

- [ ] T103 [P] Add structured JSON logging for order operations in `backend/src/main/java/com/tgfc/som/order/service/OrderService.java`
- [ ] T104 [P] Add structured logging for pricing calculations in `backend/src/main/java/com/tgfc/som/pricing/service/PriceCalculationService.java`

### Frontend Polish

- [ ] T107 [P] Implement skeleton loading states for all sections in `frontend/src/app/features/order/create-order/create-order.component.ts` (含 CSS transition 漸變過渡效果，骨架屏→實際內容平滑切換)
- [ ] T108 [P] Add submit button disable during API calls (prevent double submit) in `frontend/src/app/features/order/create-order/create-order.component.ts`

### OpenAPI Documentation

- [ ] T109 [P] Add springdoc annotations to all controllers in `backend/src/main/java/com/tgfc/som/*/controller/*.java`
- [ ] T110 [P] Verify generated OpenAPI matches contracts/order-api.yaml

### E2E Integration Test

- [ ] T111 Create full flow E2E test in `frontend/e2e/tests/order/create-order-full.spec.ts` (complete order creation with all features)

### Performance Validation

- [ ] T111a Execute performance baseline test for NFR-001 (價格試算 API ≤ 3秒, 500筆明細) in `backend/src/test/java/com/tgfc/som/pricing/PriceCalculationPerformanceTest.java`
- [ ] T111b [P] Execute performance baseline test for NFR-002 (商品驗證 API ≤ 500ms) in `backend/src/test/java/com/tgfc/som/catalog/ProductEligibilityPerformanceTest.java`
- [ ] T111c [P] Execute performance baseline test for NFR-003 (訂單建立 API ≤ 2秒) in `backend/src/test/java/com/tgfc/som/order/OrderCreationPerformanceTest.java`

### Validation

- [ ] T112 Run quickstart.md validation - verify development environment setup
- [ ] T113 Verify all API responses match contracts/order-api.yaml schema

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

- **User Story 1 (P1)**: Can start after Phase 2 - No dependencies on other stories (MVP)
- **User Story 2 (P2)**: Can start after Phase 2 - Extends US1's OrderLine but independently testable
- **User Story 3 (P3)**: Can start after Phase 2 - Extends US1's calculation but independently testable
- **User Story 4 (P4)**: Can start after Phase 2 - Uses calculation from US3 but independently testable

### Within Each User Story

- Domain/Value Objects → DTOs → Services → Controllers → Frontend → E2E
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

**Phase 1 (Setup)**:
- T002, T003, T004, T005 can run in parallel
- T006, T007 can run in parallel

**Phase 2 (Foundational)**:
- T008-T018 (Value Objects & Enums) can all run in parallel
- T019-T024 (Common Infrastructure) can run in parallel
- T025-T029 (MyBatis Mappers) can run in parallel after DB setup
- T030-T033 (Frontend Core) can run in parallel

**Phase 3 (User Story 1)**:
- T037-T044 (DTOs) can run in parallel
- T049-T051 (Controllers) can run in parallel after Services
- T054-T055 (Frontend Components) can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all DTOs for User Story 1 together:
Task: "Create CreateOrderRequest in backend/.../order/dto/CreateOrderRequest.java"
Task: "Create OrderResponse in backend/.../order/dto/OrderResponse.java"
Task: "Create AddOrderLineRequest in backend/.../order/dto/AddOrderLineRequest.java"
Task: "Create OrderLineResponse in backend/.../order/dto/OrderLineResponse.java"
Task: "Create MemberResponse in backend/.../member/dto/MemberResponse.java"
Task: "Create EligibilityResponse in backend/.../catalog/dto/EligibilityResponse.java"

# Launch all frontend components for User Story 1 together:
Task: "Create member-info.component in frontend/.../components/member-info/"
Task: "Create product-list.component in frontend/.../components/product-list/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready - basic order creation works!

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo (+ Installation/Delivery)
4. Add User Story 3 → Test independently → Deploy/Demo (+ Member Discounts)
5. Add User Story 4 → Test independently → Deploy/Demo (+ Coupon/Bonus)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Backend)
   - Developer B: User Story 1 (Frontend)
   - After US1 complete, split remaining stories
3. Stories complete and integrate independently

---

## Summary

| Phase | Task Count | Key Deliverables |
|-------|------------|------------------|
| Phase 1: Setup | 7 | Package structure for 5 Bounded Contexts |
| Phase 2: Foundational | 29 | Value Objects, Enums, Mappers, Services, External Service Resilience |
| Phase 3: US1 (P1) MVP | 27 | Basic order creation (member, product, calculate, submit, temp card) |
| Phase 4: US2 (P2) | 22 | Installation & Delivery services, Delivery/Stock compatibility (EC-008) |
| Phase 5: US3 (P3) | 12 | Member discount (Type 0/1/2), 12-step pricing |
| Phase 6: US4 (P4) | 15 | Coupon & Bonus points |
| Phase 7: Polish | 12 | Logging, Performance tests, E2E tests |
| **Total** | **124** | Complete order creation feature |

### Parallel Opportunities

- **Phase 2**: 20+ tasks can run in parallel
- **Phase 3**: 8+ tasks can run in parallel
- **User Stories**: All 4 can run in parallel after Phase 2 (with team capacity)

### Independent Test Criteria

- **US1**: Member H00199 → Add SKU → Calculate → Submit → Order ID returned
- **US1 (Temp Card)**: 查無會員 → 使用臨時卡 → 輸入姓名/電話/地址 → Submit → Order ID returned
- **US2**: Select delivery method → Add installation service → Verify cost
- **US3**: Type 0 member → Calculate → Verify memberDisc in response
- **US4**: Apply coupon → Verify discount capped at product total

### Suggested MVP Scope

**User Story 1 Only** (34 tasks total = Phase 1 + Phase 2 + Phase 3):
- Basic order creation flow
- Member lookup with H00199 mock
- Temp card support (查無會員時可手動輸入臨時卡資料)
- Product eligibility validation (6 layers)
- Simple price calculation (ComputeType 1-3)
- Order submission with idempotency check

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- **Constitution Compliance**: No Lombok, use Java Records for DTOs, MyBatisGenerator for mappers
