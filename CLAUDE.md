# Claude Code - DDD Special Order Demo

## Current Feature

**Feature ID**: 002-create-order
**Branch**: `002-create-order`
**Status**: Planning Complete

## Implementation Plan

**Location**: `specs/002-create-order/plan.md`

### Phase Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Specification | `specs/002-create-order/spec.md` | Complete |
| Research | `specs/002-create-order/research.md` | Complete |
| Data Model | `specs/002-create-order/data-model.md` | Complete |
| API Contracts | `specs/002-create-order/contracts/order-api.yaml` | Complete |
| Quickstart | `specs/002-create-order/quickstart.md` | Complete |
| Pricing Spec | `specs/002-create-order/pricing-calculation-spec.md` | Complete |
| Product Query Spec | `specs/002-create-order/product-query-spec.md` | Complete |
| Tasks | `specs/002-create-order/tasks.md` | Complete |

## Technical Context

### Backend

- **Language**: Java 21+
- **Framework**: Spring Boot 3.x+
- **Security**: Spring Security OAuth2 Resource Server + Keycloak
- **Database**: Oracle 21c (Production), H2 (Development)
- **Data Access**: MyBatis + MyBatisGenerator
- **Package**: `com.tgfc.som`
- **Logging**: Logback + logback-encoder (JSON)

### Frontend

- **Language**: TypeScript 5.9+
- **Framework**: Angular 21+ (Standalone, Signals, OnPush)
- **State Management**: Angular Signals + RxJS
- **Auth Library**: keycloak-angular
- **E2E Testing**: Playwright

### Project Structure

```text
backend/src/main/java/com/tgfc/som/
├── order/                       # Order Context (Core Domain)
│   ├── controller/
│   ├── service/
│   ├── domain/
│   └── dto/
├── product/                     # Catalog Context (Supporting)
├── member/                      # Member Context (Supporting)
├── pricing/                     # Pricing Context (Supporting)
├── fulfillment/                 # Fulfillment Context (Supporting)
├── mapper/                      # MyBatisGenerator Mappers
├── mapper/custom/               # Custom Mappers
├── entity/                      # MyBatisGenerator Entities
└── common/
    ├── config/
    ├── exception/
    └── constant/

frontend/src/app/
├── core/                        # Core module
├── shared/                      # Shared components
├── features/
│   └── order/                   # Order feature module
└── app.routes.ts
```

## Constitution Principles

See `.specify/memory/constitution.md` (v1.10.0) for project governance:

1. **Pragmatic DDD** - Lightweight DDD, avoid over-engineering
2. **3-Table Rule** - Max 3 tables per feature (exceptions documented)
3. **KISS Principle** - Simplest solution that works
4. **Database Schema Documentation** - Reference `docs/tables/` before implementation
5. **Legacy Codebase Reference** - Search `C:/projects/som` for existing patterns
6. **Stateless Backend Architecture** - No server-side session, JWT validation
7. **Playwright Verification** - E2E test with screenshots after implementation
8. **MyBatis Generator Pattern** - Use MyBatisGenerator for Mapper/Entity, CustomMapper only for complex SQL
9. **No Lombok Policy** - Use Java Records for DTO, manual getter/setter for Entity
10. **Data Class Convention** - Records for immutable DTO, Class for mutable Entity
11. **OpenAPI RESTful API Standard** - Code-First with springdoc-openapi
12. **Angular 21+ Frontend Standard** - Standalone, Signals, new control flow
13. **Code Coverage Requirement** - 80% line and branch coverage

## Key Domain Concepts

### 5 Bounded Contexts

- **Order Context** (Core) - Order aggregate, OrderLine entity
- **Member Context** (Supporting) - Member info and discount eligibility
- **Catalog Context** (Supporting) - Product eligibility (8-layer validation)
- **Pricing Context** (Supporting) - 12-step calculation flow
- **Fulfillment Context** (Supporting) - Worktype assignment

### 12-Step Pricing Calculation

1. revertAllSkuAmt - Restore original prices
2. apportionmentDiscount - Worktype price apportionment
3. AssortSku - Product classification
4. memberDiscountType2 - Cost Markup discount
5. promotionCalculation - 8 promotion types (A-H)
6. memberDiscountType0 - Discounting discount
7. memberDiscountType1 - Down Margin discount
8. specialMemberDiscount - VIP/Employee pricing
9-12. generateComputeType - Generate 6 ComputeTypes

### 8-Layer Product Eligibility

1. Format validation (SKU format)
2. Existence validation (TBL_SKU)
3. System product exclusion (allowSales)
4. Tax type validation (0/1/2)
5. Sales prohibition (holdOrder)
6. Category restriction (prohibited list)
7. Vendor freeze (TBL_VENDOR_COMPANY.STATUS)
8. Purchasing organization (TBL_SKU_COMPANY)

### Member Discount Types

- **Type 0 (Discounting)** - Discount rate, doesn't modify actPosAmt
- **Type 1 (Down Margin)** - Fixed discount, modifies actPosAmt
- **Type 2 (Cost Markup)** - Cost-based pricing, replaces actPosAmt

### Delivery/Stock Compatibility

| Delivery | Stock X | Stock Y |
|----------|---------|---------|
| N (Delivery) | OK | OK |
| D (Pure Delivery) | OK | OK |
| V (Direct) | Auto→Y | OK |
| C (Pickup Now) | OK | Auto→X |
| F (Home Delivery) | OK | OK |
| P (Pickup Later) | OK | OK |

## API Endpoints

Base URL: `/api/v1`

| Method | Path | Description |
|--------|------|-------------|
| POST | /orders | Create new order |
| POST | /orders/{id}/lines | Add order line |
| POST | /orders/{id}/calculate | Execute pricing calculation |
| POST | /orders/{id}/submit | Submit order |
| GET | /products/{skuNo}/eligibility | Check product eligibility |
| GET | /members/{memberId} | Get member info (Mock: H00199) |

## Next Steps

Run `/speckit.implement` to execute tasks from `tasks.md`.
