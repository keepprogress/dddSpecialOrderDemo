# Quickstart: Keycloak 使用者登入與系統選擇

**Feature**: 001-keycloak-user-login
**Date**: 2025-12-17
**Status**: Ready for Implementation

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| Java | 21+ | Backend runtime |
| Node.js | 20+ | Frontend build |
| Maven | 3.9+ | Backend build |
| Angular CLI | 19+ | Frontend development |

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

### Backend (H2 Database)

1. **Start with H2 Profile**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=h2
   ```

2. **H2 Console Access**
   - URL: `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:somdb`
   - Username: `sa`
   - Password: (empty)

3. **API Endpoints**
   - Base URL: `http://localhost:8080/api`
   - Health: `http://localhost:8080/api/health`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

### Frontend (Development Server)

1. **Start Development Server**
   ```bash
   cd frontend
   ng serve
   ```

2. **Access Application**
   - URL: `http://localhost:4200`
   - Login redirects to Keycloak

---

## Keycloak Configuration

### Test Environment

| Setting | Value |
|---------|-------|
| Auth Server URL | `https://authempsit02.testritegroup.com/auth` |
| Realm | `testritegroup-employee` |
| Client ID | `epos-frontend` |
| Audience | `epos-backend` |

### Required Keycloak Settings

1. **Client Configuration (epos-frontend)**
   - Access Type: `public`
   - Standard Flow Enabled: `ON`
   - Direct Access Grants: `OFF`
   - Valid Redirect URIs: `http://localhost:4200/*`
   - Web Origins: `http://localhost:4200`

2. **Client Configuration (epos-backend)**
   - Access Type: `bearer-only`
   - Used for: Backend API token validation

---

## Sample Data (H2)

### Users

```sql
-- Insert test users
INSERT INTO TBL_USER (USER_ID, USER_NAME, SYSTEM_FLAG, DISABLE_FLAG, ENABLE_DATE, DISABLE_DATE)
VALUES ('test.user', 'Test User', 'SO,TTS', NULL, '2024-01-01', '2025-12-31');

INSERT INTO TBL_USER (USER_ID, USER_NAME, SYSTEM_FLAG, DISABLE_FLAG, ENABLE_DATE, DISABLE_DATE)
VALUES ('admin.user', 'Admin User', 'SO,TTS,APP', NULL, '2024-01-01', '2025-12-31');

INSERT INTO TBL_USER (USER_ID, USER_NAME, SYSTEM_FLAG, DISABLE_FLAG, ENABLE_DATE, DISABLE_DATE)
VALUES ('disabled.user', 'Disabled User', 'SO', 'Y', '2024-01-01', '2025-12-31');
```

### Channels

```sql
-- Insert channels (static data)
INSERT INTO TBL_CHANNEL (CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC)
VALUES ('SO', 'Special Order', '特殊訂單系統');

INSERT INTO TBL_CHANNEL (CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC)
VALUES ('TTS', 'TTS', 'TTS 系統');

INSERT INTO TBL_CHANNEL (CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC)
VALUES ('APP', 'APP', 'APP 系統');
```

### Stores

```sql
-- Insert sample stores
INSERT INTO TBL_STORE (STORE_ID, STORE_NAME, CHANNEL_ID)
VALUES ('S001', '台北門市', 'SO');

INSERT INTO TBL_STORE (STORE_ID, STORE_NAME, CHANNEL_ID)
VALUES ('S002', '新竹門市', 'SO');

INSERT INTO TBL_STORE (STORE_ID, STORE_NAME, CHANNEL_ID)
VALUES ('S003', '台中門市', 'SO');
```

### User Store Assignments

```sql
-- User mast store (NULL = 全區)
INSERT INTO TBL_USER_MAST_STORE (USER_ID, STORE_ID)
VALUES ('test.user', 'S001');

INSERT INTO TBL_USER_MAST_STORE (USER_ID, STORE_ID)
VALUES ('admin.user', NULL);  -- 全區權限

-- User support stores
INSERT INTO TBL_USER_STORE (USER_ID, STORE_ID)
VALUES ('test.user', 'S002');

INSERT INTO TBL_USER_STORE (USER_ID, STORE_ID)
VALUES ('test.user', 'S003');
```

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
   - Verify Nav Bar items
   - Verify user info display
   - Test logout button
   - Test system switch

---

## Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| CORS error | Backend not configured | Check `cors.allowed-origins` in application.yml |
| Token expired | Session timeout | Keycloak token auto-refresh should handle this |
| User not found | User ID mismatch | Ensure Keycloak username matches TBL_USER.USER_ID |
| 403 Forbidden | Validation failed | Check 6-checkpoint validation errors |

### Debug Mode

```yaml
# application-h2.yml
logging:
  level:
    com.tgfc.som: DEBUG
    org.springframework.security: DEBUG
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
