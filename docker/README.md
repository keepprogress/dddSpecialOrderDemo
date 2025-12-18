# SIT 環境 Docker 設置

## Quick Start

啟動 Keycloak：
```bash
cd docker
docker-compose up -d
```

## Keycloak Admin Console

- URL: http://localhost:8180/admin
- Username: `admin`
- Password: `admin`

## 測試帳號 (Keycloak)

| Username | Password | 對應 TBL_USER |
|----------|----------|---------------|
| `test.user` | `test123` | Normal user with single mast store |
| `admin.user` | `admin123` | Admin user with 全區 access |
| `disabled.user` | `disabled123` | User for testing disabled state |

## 設定說明

| Setting | Value |
|---------|-------|
| Auth Server URL | http://localhost:8180 |
| Realm | som |
| Client ID (Frontend) | som-frontend |
| Client ID (Backend) | som-backend |

## E2E 測試

啟動 Keycloak 後，可以執行 E2E 測試：
```bash
cd frontend
npm run e2e
```

## 停止服務

```bash
docker-compose down
```

## 重置 Keycloak 資料

```bash
docker-compose down -v
docker-compose up -d
```
