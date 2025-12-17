# Quickstart: Keycloak 使用者登入與系統選擇

**Feature**: 001-keycloak-user-login
**Date**: 2025-12-18
**Status**: Implementation Complete

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21+ | Backend runtime |
| Node.js | 20+ | Frontend build |
| Maven | 3.9+ | Backend build |
| Angular CLI | 21+ | Frontend development |

### Environment Setup

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd dddSpecialOrderDemo
   ```

2. **Backend Setup**
   ```bash
   cd backend
   mvn clean install -DskipTests
   ```

3. **Frontend Setup**
   ```bash
   cd frontend
   npm install
   ```

---

## Local Development

### Environment Profiles

| Profile | Database | Keycloak | Usage |
|---------|----------|----------|-------|
| `sit` | H2 (in-memory) | localhost:8180 | Local development |
| `uat` | Oracle UAT | authempsit02.testritegroup.com | Test environment |

### Backend (SIT - Local Development)

1. **Prerequisites**
   - Start local Keycloak on port 8180
   - Or use Docker: `docker run -p 8180:8080 quay.io/keycloak/keycloak start-dev`

2. **Start with SIT Profile**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=sit
   ```

3. **H2 Console Access**
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:somdb`
   - Username: `sa`
   - Password: (empty)

4. **API Endpoints**
   - Base URL: `http://localhost:8080/api`
   - Health: `http://localhost:8080/api/health`

### Backend (UAT - Test Environment)

1. **Start with UAT Profile**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=uat,secret
   ```
   Note: Requires `application-secret.properties` with Oracle credentials.

### Frontend (Development Server)

1. **Start Development Server (SIT)**
   ```bash
   cd frontend
   ng serve --configuration=sit
   ```

2. **Start Development Server (UAT)**
   ```bash
   cd frontend
   ng serve --configuration=uat
   ```

3. **Access Application**
   - URL: `http://localhost:4200`
   - Login redirects to Keycloak

---

## Keycloak Configuration

### SIT (Local Keycloak)

| Setting | Value |
|---------|-------|
| Auth Server URL | `http://localhost:8180` |
| Realm | `som` |
| Client ID (Frontend) | `som-frontend` |

### UAT (Test Environment)

| Setting | Value |
|---------|-------|
| Auth Server URL | `https://authempsit02.testritegroup.com/auth` |
| Realm | `testritegroup-employee` |
| Client ID (Frontend) | `epos-frontend` |
| Client ID (Backend) | `epos-backend` |

---

## Sample Data (H2/SIT)

Sample data is automatically loaded from `data-sit.sql` when using the SIT profile.

### Test Users

| EMP_ID | EMP_NAME | SYSTEM_FLAG | Notes |
|--------|----------|-------------|-------|
| `test.user` | Test User | SO,TTS | Normal user with single mast store |
| `admin.user` | Admin User | SO,TTS,APP | 全區 user (STORE_ID = NULL) |
| `disabled.user` | Disabled User | SO | User with DISABLED_FLAG = 'Y' |

### Schema Structure

Tables use UAT Oracle structure:
- `TBL_USER`: EMP_ID (PK), EMP_NAME, SYSTEM_FLAG, DISABLED_FLAG, START_DATE, END_DATE
- `TBL_CHANNEL`: CHANNEL_ID (PK), CHANNEL_NAME
- `TBL_STORE`: STORE_ID (PK), CHANNEL_ID, STORE_NAME
- `TBL_USER_MAST_STORE`: EMP_ID, STORE_ID (NULL = 全區)
- `TBL_USER_STORE`: EMP_ID, STORE_ID
- `TBL_AUDIT_LOG`: LOG_ID (PK), EMP_ID, ACTION_TYPE, etc.

---

## Testing

### Unit Tests

```bash
# Backend
cd backend
mvn test

# Frontend
cd frontend
ng test
```

### E2E Tests (Playwright)

```bash
cd frontend
npx playwright test
```

### Manual Testing Flow

1. **Login Flow**
   - Navigate to `http://localhost:4200`
   - Redirect to Keycloak login
   - Enter credentials
   - 6-checkpoint validation executes
   - Redirect to store selection

2. **Store Selection**
   - Select master store (or 全區)
   - Select support stores
   - Click confirm

3. **System Selection**
   - Select available system (SO/TTS/APP)
   - Click confirm
   - Redirect to homepage

4. **Homepage Verification**
   - Verify Nav Bar items (訂單管理, 退貨管理, 安運單管理, 主檔維護, 報表)
   - Verify user info display
   - Test logout button
   - Test "切換系統" button

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| CORS error | Backend not configured | Check `cors.allowed-origins` in application.properties |
| Token expired | Session timeout | Keycloak auto-refresh should handle (60 min timeout) |
| User not found | User ID mismatch | Ensure Keycloak username matches TBL_USER.EMP_ID |
| 403 Forbidden | Validation failed | Check 6-checkpoint validation errors in console |
| Keycloak connection refused | Local Keycloak not running | Start Keycloak on port 8180 |

### Debug Mode

Add to `application-sit.properties`:
```properties
logging.level.com.tgfc.som=DEBUG
logging.level.org.springframework.security=DEBUG
```

---

## API Testing (curl)

### Health Check (No Auth)

```bash
curl http://localhost:8080/api/health
```

### Validate User (With Token)

```bash
curl -X POST http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer <keycloak-token>" \
  -H "Content-Type: application/json"
```

### Get Master Stores

```bash
curl http://localhost:8080/api/stores/mast \
  -H "Authorization: Bearer <keycloak-token>"
```

### Get Support Stores

```bash
curl "http://localhost:8080/api/stores/support?mastStoreId=S001" \
  -H "Authorization: Bearer <keycloak-token>"
```

### Get Channels

```bash
curl http://localhost:8080/api/channels \
  -H "Authorization: Bearer <keycloak-token>"
```

### Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <keycloak-token>"
```
