# Claude Code - DDD Special Order Demo

## Current Feature

**Feature ID**: 001-keycloak-user-login
**Branch**: `001-keycloak-user-login`
**Status**: Planning Complete

## Implementation Plan

**Location**: `specs/001-keycloak-user-login/plan.md`

### Phase 1 Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Specification | `specs/001-keycloak-user-login/spec.md` | Complete |
| Research | `specs/001-keycloak-user-login/research.md` | Complete |
| Data Model | `specs/001-keycloak-user-login/data-model.md` | Complete |
| API Contracts | `specs/001-keycloak-user-login/contracts/auth-api.yaml` | Complete |
| Quickstart | `specs/001-keycloak-user-login/quickstart.md` | Complete |

## Technical Context

### Backend

- **Language**: Java 21+
- **Framework**: Spring Boot 3.x+
- **Security**: Spring Security OAuth2 Resource Server
- **Database**: Oracle 21c (Production), H2 (Development)
- **Data Access**: MyBatis + MyBatisGenerator
- **Package**: `com.tgfc.som`

### Frontend

- **Language**: TypeScript 5.9+
- **Framework**: Angular 21+
- **Auth Library**: keycloak-angular
- **State Storage**: Local Storage

### Project Structure

```text
backend/src/main/java/com/tgfc/som/
├── auth/                    # Authentication module
│   ├── controller/          # REST Controllers
│   ├── service/             # Application Services
│   └── domain/              # Domain Services
├── mapper/                  # MyBatisGenerator 產生的 Mapper
├── mapper/custom/           # 手動撰寫的 CustomMapper (視需要)
├── entity/                  # MyBatisGenerator 產生的 Entity
├── common/                  # Shared components
│   ├── config/              # Keycloak, Security, MyBatis configs
│   └── exception/           # Exception handlers

frontend/src/app/
├── auth/                    # Authentication module
│   ├── components/          # Login, Store/System selection
│   ├── services/            # Auth service, Token interceptor
│   └── guards/              # Route guards
├── core/                    # Core module
│   ├── layout/              # Nav bar, Header
│   └── services/            # API service
├── shared/                  # Shared module
└── e2e/                     # Playwright tests
```

## Constitution Principles

See `.specify/memory/constitution.md` (v1.5.0) for project governance:

1. **Pragmatic DDD** - Lightweight DDD, avoid over-engineering
2. **3-Table Rule** - Max 3 tables per feature (exceptions documented)
3. **KISS Principle** - Simplest solution that works
4. **Database Schema Documentation** - Reference `docs/tables/` before implementation
5. **Legacy Codebase Reference** - Search `C:/projects/som` for existing patterns
6. **Stateless Backend Architecture** - No server-side session, JWT validation
7. **Playwright Verification** - E2E test with screenshots after implementation
8. **MyBatis Generator Pattern** - Use MyBatisGenerator for Mapper/Entity, CustomMapper only for complex SQL with performance concerns

## Key Domain Concepts

### 6-Checkpoint Validation

1. User exists in TBL_USER
2. SYSTEM_FLAG is not null
3. DISABLE_FLAG is not 'Y'
4. ENABLE_DATE and DISABLE_DATE are set
5. Current date is within ENABLE_DATE and DISABLE_DATE
6. User has function permissions

### 主店別 NULL 邏輯

- `STORE_ID = NULL` in TBL_USER_MAST_STORE means "全區" (all-region access)
- Frontend displays "全區" option
- Backend queries without store filter for all-region users

### Single Tab Restriction

- Use BroadcastChannel API + LocalStorage
- New tab takes over, old tab shows warning
- Prevents state conflicts and document collisions

## API Endpoints

Base URL: `/api`

| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/validate | 6-checkpoint user validation |
| POST | /auth/logout | Audit logout event |
| GET | /stores/mast | Get user's master stores |
| GET | /stores/support | Get user's support stores |
| POST | /stores/select | Record store selection |
| GET | /channels | Get user's authorized channels |
| POST | /channels/select | Record channel selection |
| GET | /health | Health check (no auth) |

## Next Steps

Run `/speckit.tasks` to generate implementation tasks from the plan.
