# Data Model: Keycloak 使用者登入與系統選擇

**Feature**: 001-keycloak-user-login
**Date**: 2025-12-17
**Status**: Complete

## Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│   TBL_USER      │       │  TBL_CHANNEL    │
├─────────────────┤       ├─────────────────┤
│ USER_ID (PK)    │       │ CHANNEL_ID (PK) │
│ USER_NAME       │       │ CHANNEL_NAME    │
│ SYSTEM_FLAG     │──────>│ CHANNEL_DESC    │
│ DISABLE_FLAG    │       └─────────────────┘
│ ENABLE_DATE     │
│ DISABLE_DATE    │       ┌─────────────────┐
└────────┬────────┘       │   TBL_STORE     │
         │                ├─────────────────┤
         │                │ STORE_ID (PK)   │
         │                │ STORE_NAME      │
         │                │ CHANNEL_ID (FK) │
         │                └────────┬────────┘
         │                         │
    ┌────┴────┐               ┌────┴────┐
    │         │               │         │
┌───┴─────────┴───┐   ┌───────┴─────────┴───┐
│TBL_USER_MAST_   │   │   TBL_USER_STORE    │
│    STORE        │   ├─────────────────────┤
├─────────────────┤   │ USER_ID (PK, FK)    │
│ USER_ID (PK,FK) │   │ STORE_ID (PK, FK)   │
│ STORE_ID (FK)   │   └─────────────────────┘
│ (NULL = 全區)   │
└─────────────────┘
```

---

## Entities

### 1. User (使用者)

**Table**: `TBL_USER`

| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| USER_ID | VARCHAR(20) | PK, NOT NULL | 使用者帳號（對應 Keycloak username） |
| USER_NAME | VARCHAR(100) | NOT NULL | 使用者姓名 |
| SYSTEM_FLAG | VARCHAR(50) | NULL | 系統別權限（comma-separated: "SO,TTS,APP"） |
| DISABLE_FLAG | CHAR(1) | NULL | 停用旗標（'Y' = 停用） |
| ENABLE_DATE | DATE | NULL | 啟用日期 |
| DISABLE_DATE | DATE | NULL | 停用日期 |

**Validation Rules**:
- USER_ID 必須與 Keycloak username 一致
- SYSTEM_FLAG 為 comma-separated 字串，有效值: SO, TTS, APP
- DISABLE_FLAG 只接受 'Y' 或 NULL
- ENABLE_DATE 必須 <= DISABLE_DATE

**State Transitions**:
```
Created → Active (DISABLE_FLAG != 'Y' AND within date range)
Active → Disabled (DISABLE_FLAG = 'Y')
Active → Expired (current date > DISABLE_DATE)
Disabled → Active (DISABLE_FLAG = NULL/空)
```

---

### 2. Channel (通路/系統別)

**Table**: `TBL_CHANNEL`

| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| CHANNEL_ID | VARCHAR(10) | PK, NOT NULL | 通路代碼 (SO, TTS, APP) |
| CHANNEL_NAME | VARCHAR(50) | NOT NULL | 通路名稱 |
| CHANNEL_DESC | VARCHAR(200) | NULL | 通路描述 |

**Static Data**:
| CHANNEL_ID | CHANNEL_NAME | CHANNEL_DESC |
|------------|--------------|--------------|
| SO | Special Order | 特殊訂單系統 |
| TTS | TTS | TTS 系統 |
| APP | APP | APP 系統 |

---

### 3. Store (店別)

**Table**: `TBL_STORE`

| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| STORE_ID | VARCHAR(10) | PK, NOT NULL | 店別代碼 |
| STORE_NAME | VARCHAR(100) | NOT NULL | 店別名稱 |
| CHANNEL_ID | VARCHAR(10) | FK, NOT NULL | 所屬通路 |

**Relationships**:
- Many-to-One: Store → Channel

---

### 4. UserMastStore (使用者主店別)

**Table**: `TBL_USER_MAST_STORE`

| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| USER_ID | VARCHAR(20) | PK, FK, NOT NULL | 使用者帳號 |
| STORE_ID | VARCHAR(10) | FK, NULL | 主店別代碼（NULL = 全區權限） |

**Validation Rules**:
- 一個使用者只能有一筆主店別記錄
- STORE_ID = NULL 代表「全區」權限，可存取所有店別資料
- STORE_ID 若非 NULL，必須存在於 TBL_STORE

**Relationships**:
- Many-to-One: UserMastStore → User (USER_ID)
- Many-to-One: UserMastStore → Store (STORE_ID, nullable)

---

### 5. UserStore (使用者支援店別)

**Table**: `TBL_USER_STORE`

| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| USER_ID | VARCHAR(20) | PK, FK, NOT NULL | 使用者帳號 |
| STORE_ID | VARCHAR(10) | PK, FK, NOT NULL | 支援店別代碼 |

**Validation Rules**:
- 一個使用者可以有多筆支援店別記錄
- USER_ID + STORE_ID 為複合主鍵
- STORE_ID 必須存在於 TBL_STORE

**Relationships**:
- Many-to-One: UserStore → User (USER_ID)
- Many-to-One: UserStore → Store (STORE_ID)

---

## Frontend Models (TypeScript)

### LoginContext (儲存於 Local Storage)

```typescript
interface LoginContext {
  // Keycloak Tokens
  accessToken: string;
  refreshToken: string;
  tokenExpiry: number; // Unix timestamp

  // User Info (from 6-checkpoint validation)
  userId: string;
  userName: string;
  systemFlags: string[]; // ['SO', 'TTS']

  // Selected Context
  selectedMastStore: MastStore;
  selectedSupportStores: Store[];
  selectedChannel: Channel;

  // Tab Management
  tabId: string;
  loginTimestamp: number;
}

interface MastStore {
  storeId: string | null; // null = 全區
  storeName: string;
}

interface Store {
  storeId: string;
  storeName: string;
  channelId: string;
}

interface Channel {
  channelId: string; // 'SO' | 'TTS' | 'APP'
  channelName: string;
}
```

---

## Backend Entities (Java - MyBatis)

> **注意**：本專案使用 MyBatis，Entity 由 **MyBatisGenerator 自動產生**（參考 Constitution 原則 VIII）。
> 以下範例為 MyBatisGenerator 產生的格式，不使用 JPA 註解。

### User.java (MyBatisGenerator 產生)

```java
/**
 * TBL_USER 資料表對應的 Entity
 * 由 MyBatisGenerator 自動產生，請勿手動修改
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String userName;
    private String systemFlag;
    private String disableFlag;
    private LocalDate enableDate;
    private LocalDate disableDate;

    // Getter/Setter 由 MyBatisGenerator 產生
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getSystemFlag() { return systemFlag; }
    public void setSystemFlag(String systemFlag) { this.systemFlag = systemFlag; }

    public String getDisableFlag() { return disableFlag; }
    public void setDisableFlag(String disableFlag) { this.disableFlag = disableFlag; }

    public LocalDate getEnableDate() { return enableDate; }
    public void setEnableDate(LocalDate enableDate) { this.enableDate = enableDate; }

    public LocalDate getDisableDate() { return disableDate; }
    public void setDisableDate(LocalDate disableDate) { this.disableDate = disableDate; }

    // toString, equals, hashCode 由 MyBatisGenerator 產生
}
```

### UserMastStore.java (MyBatisGenerator 產生)

```java
/**
 * TBL_USER_MAST_STORE 資料表對應的 Entity
 * 由 MyBatisGenerator 自動產生，請勿手動修改
 */
public class UserMastStore implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String storeId;  // nullable - null means 全區

    // Getter/Setter 由 MyBatisGenerator 產生
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
}
```

### 領域邏輯封裝 (Domain Service)

> **注意**：Entity 由 MyBatisGenerator 產生為純資料載體，領域邏輯應放在 Domain Service 或 Value Object 中。

```java
/**
 * 使用者領域服務 - 封裝業務邏輯
 */
public class UserDomainService {

    /**
     * 解析使用者的系統權限列表
     */
    public List<String> parseSystemFlags(User user) {
        String systemFlag = user.getSystemFlag();
        if (systemFlag == null || systemFlag.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(systemFlag.split(","));
    }

    /**
     * 判斷使用者是否為啟用狀態
     */
    public boolean isUserActive(User user) {
        LocalDate today = LocalDate.now();
        return !"Y".equals(user.getDisableFlag())
            && user.getEnableDate() != null
            && user.getDisableDate() != null
            && !today.isBefore(user.getEnableDate())
            && !today.isAfter(user.getDisableDate());
    }

    /**
     * 判斷使用者是否為全區權限
     */
    public boolean isAllRegion(UserMastStore userMastStore) {
        return userMastStore.getStoreId() == null;
    }
}
```

### API Response DTO (Java Records)

> **注意**：API 回應使用 Java Records（參考 Constitution 原則 X），與 MyBatis Entity 分離。

```java
/**
 * 使用者資訊回應 DTO
 */
public record UserInfoResponse(
    String userId,
    String userName,
    List<String> systemFlags,
    boolean active
) {}

/**
 * 主店別資訊回應 DTO
 */
public record MastStoreResponse(
    String storeId,    // null = 全區
    String storeName,
    boolean allRegion
) {}
```

---

## Data Volume Estimates

| Entity | Estimated Count | Growth Rate | Notes |
|--------|-----------------|-------------|-------|
| User | ~1,000 | Low | 員工數，緩慢成長 |
| Channel | 3 | Static | 固定值 |
| Store | ~500 | Low | 門市數，緩慢成長 |
| UserMastStore | ~1,000 | Low | 1:1 對應使用者 |
| UserStore | ~2,000 | Low | 平均每人 2 個支援店別 |

**3-Table Rule Compliance**:
- 總計 5 個資料表
- 符合例外條件：設定檔/元資料類型，非歷史資料
- 資料量 < 10,000 筆
