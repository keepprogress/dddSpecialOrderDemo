# Feature Specification: Keycloak 使用者登入與系統選擇

**Feature Branch**: `001-keycloak-user-login`
**Created**: 2025-12-17
**Status**: Draft
**Input**: User description: "使用者登入要通過 keycloak 驗證 特殊訂單系統需要先選擇系統別以及主店別 使用者可以有支援店別 接下來選擇系統別 可能是SO, TTS, APP"

## Clarifications

### Session 2025-12-17

- Q: 同一帳號是否允許開多個分頁？ → A: 限制單一分頁，避免狀態落差或單據衝突
- Q: 首頁佈局與導航列規格？ → A: Nav Bar 含訂單管理、退貨管理、安運單管理、主檔維護、報表；右上角顯示登入者資訊、登出、切換系統按鈕

### Session 2025-12-18

- Q: SIT 測試帳號設定 → A: EMP_ID=H00199, 密碼=Aa123456, 權限=SO/TTS/APP, 主店別=S001, 支援店別=S002/S003

## Technical Context (from Logical Schema)

本功能基於既有系統的 Authentication & Authorization Logical Schema 設計，以下為關鍵技術背景：

### 6-Checkpoint 驗證流程

使用者登入需通過以下 6 個驗證關卡：
1. **User Exists**: 使用者帳號必須存在於 TBL_USER
2. **SYSTEM_FLAG Not Null**: 使用者必須被授權至少一個系統別
3. **Not Disabled**: 使用者未被停用（DISABLE_FLAG != 'Y'）
4. **Dates Set**: 啟用日期與停用日期已設定
5. **Within Date Range**: 目前日期在有效期間內
6. **Has Function Permissions**: 使用者必須有至少一個功能權限

### 主店別 NULL 邏輯

- TBL_USER_MAST_STORE.STORE_ID = NULL 代表「全區」權限
- 擁有全區權限的使用者可存取所有店別資料
- 非全區使用者僅能存取指定的主店別資料

### 系統別（通路別）

- 系統別透過 TBL_CHANNEL 定義（CHANNEL_ID: SO, TTS, APP）
- 使用者的系統別權限儲存於 TBL_USER.SYSTEM_FLAG（comma-separated，例如 "SO,TTS"）

### Keycloak 環境設定 (測試區)

```properties
keycloak.auth-server-url=https://authempsit02.testritegroup.com/auth
keycloak.realm=testritegroup-employee
keycloak.ssl-required=external
keycloak.resource=epos-backend
keycloak.credentials.secret=2582c170-6623-4926-bd47-d329a48dff18
keycloak.confidential-port=0
keycloak.cors-max-age=999
keycloak.cors-allowed-methods=POST,PUT,DELETE,GET
keycloak.disable-trust-manager=true
```

### 前後端分離架構

- **Token 儲存位置**：前端 Local Storage（非 Server-Side Session）
- **理由**：採用前後端分離架構，後端為 Stateless API 服務
- **Token 類型**：Keycloak Access Token + Refresh Token
- **前端職責**：管理 Token 生命週期、自動刷新、過期處理

### Session 管理

- Session Timeout: 60 分鐘無活動後自動登出
- **實作方式**：透過 Keycloak Token 的 `exp` (expiration) claim 判斷，而非 Server-Side Session

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Keycloak 身份驗證登入 (Priority: P1)

使用者透過 Keycloak 進行身份驗證，完成登入流程。這是系統的基礎功能，所有使用者必須先通過身份驗證才能使用系統。

**Why this priority**: 身份驗證是系統安全的基礎，沒有驗證機制無法區分使用者身份與權限。這是所有後續功能的前置條件。

**Independent Test**: 可透過輸入有效的 Keycloak 憑證（帳號密碼）並成功登入來測試。成功後使用者應看到系統選擇畫面。

**Acceptance Scenarios**:

1. **Given** 使用者尚未登入系統，**When** 使用者在登入頁面輸入有效的帳號與密碼，**Then** 系統透過 Keycloak 驗證成功，並通過 6-checkpoint 驗證後導向店別選擇頁面
2. **Given** 使用者輸入無效的帳號或密碼，**When** 送出登入請求，**Then** 系統顯示「帳號或密碼錯誤」訊息，使用者停留在登入頁面
3. **Given** Keycloak 服務暫時無法連線，**When** 使用者嘗試登入，**Then** 系統顯示「驗證服務暫時無法使用，請稍後再試」訊息
4. **Given** 使用者已在其他分頁登入，**When** 使用者在新分頁開啟系統，**Then** 新分頁成為主要操作分頁，原有分頁顯示「已在其他分頁開啟」訊息並停止操作
5. **Given** 使用者通過 Keycloak 驗證但帳號已被停用（DISABLE_FLAG = 'Y'），**When** 系統進行 6-checkpoint 驗證，**Then** 系統顯示「帳號已被停用，請聯繫管理員」訊息
6. **Given** 使用者通過 Keycloak 驗證但目前日期不在有效期間內，**When** 系統進行 6-checkpoint 驗證，**Then** 系統顯示「帳號不在有效使用期間，請聯繫管理員」訊息

---

### User Story 2 - 選擇主店別與支援店別 (Priority: P2)

使用者登入後需要選擇主店別，系統顯示該使用者可操作的支援店別清單供選擇（可選）。

**Why this priority**: 店別選擇決定使用者可查看與操作的資料範圍，是資料權限控制的關鍵。必須在系統選擇前完成。

**Independent Test**: 登入後直接測試店別選擇功能。成功選擇主店別後，使用者應看到系統別選擇畫面。

**Acceptance Scenarios**:

1. **Given** 使用者完成 Keycloak 驗證，**When** 進入店別選擇頁面，**Then** 系統顯示該使用者被授權的主店別清單（來自 TBL_USER_MAST_STORE）
2. **Given** 使用者選擇了主店別，**When** 系統檢查該使用者是否有支援店別權限，**Then** 若有支援店別（來自 TBL_USER_STORE）則顯示支援店別清單供多選，若無則直接進入下一步
3. **Given** 使用者選擇主店別與支援店別（可選），**When** 點擊「確認」按鈕，**Then** 系統記錄使用者的店別選擇並導向系統別選擇頁面
4. **Given** 使用者只有一個主店別權限，**When** 進入店別選擇頁面，**Then** 系統自動選擇該主店別並跳過選擇步驟，直接進入系統別選擇
5. **Given** 使用者的主店別為「全區」（TBL_USER_MAST_STORE.STORE_ID = NULL），**When** 進入店別選擇頁面，**Then** 系統顯示「全區」選項，選擇後可存取所有店別資料

---

### User Story 3 - 選擇系統別 (Priority: P3)

使用者選擇要使用的系統別（SO、TTS 或 APP），完成登入流程並進入對應系統的主頁面。

**Why this priority**: 系統別決定使用者進入哪個業務模組。這是登入流程的最後一步，完成後使用者才能開始實際業務操作。

**Independent Test**: 完成店別選擇後測試系統別選擇。成功選擇後使用者應進入對應系統的首頁。

**Acceptance Scenarios**:

1. **Given** 使用者完成店別選擇，**When** 進入系統別選擇頁面，**Then** 系統根據 TBL_USER.SYSTEM_FLAG 解析並顯示使用者被授權的系統別選項（SO、TTS、APP）
2. **Given** 使用者選擇特定系統別（例如 SO），**When** 點擊確認，**Then** 系統記錄選擇並導向該系統別的主頁面
3. **Given** 使用者只有一個系統別權限（SYSTEM_FLAG 只有一個值），**When** 進入系統別選擇頁面，**Then** 系統自動選擇該系統別並跳過選擇步驟，直接進入對應系統首頁
4. **Given** 使用者已進入系統，**When** 使用者想切換到其他系統別，**Then** 系統提供切換系統別的功能（例如導航選單中的「切換系統」選項）
5. **Given** 使用者的 SYSTEM_FLAG 為 "SO,TTS"，**When** 進入系統別選擇頁面，**Then** 系統只顯示 SO 與 TTS 兩個選項（不顯示 APP）

---

### User Story 4 - 系統首頁與導航列 (Priority: P4)

使用者完成通路、主店別、系統別選擇後進入系統首頁，首頁提供導航列以存取各功能模組，並於右上角提供使用者操作選項。

**Why this priority**: 首頁是使用者完成登入流程後的主要操作介面，導航列決定使用者如何存取各功能模組。

**Independent Test**: 完成系統別選擇後，使用者應進入首頁並看到完整的導航列與右上角操作區域。

**Acceptance Scenarios**:

1. **Given** 使用者完成通路、主店別、系統別選擇，**When** 點擊確認進入系統，**Then** 系統導向首頁並顯示導航列（Nav Bar）
2. **Given** 使用者進入首頁，**When** 查看導航列，**Then** 導航列顯示以下功能選項：訂單管理、退貨管理、安運單管理、主檔維護、報表
3. **Given** 使用者進入首頁，**When** 查看右上角區域，**Then** 顯示登入者資訊（使用者名稱、所選店別、系統別）
4. **Given** 使用者進入首頁，**When** 查看右上角區域，**Then** 顯示「登出」按鈕，點擊後清除登入狀態並導向登入頁面
5. **Given** 使用者進入首頁，**When** 點擊右上角「切換系統」按鈕，**Then** 系統導向通路/主店別/系統別選擇頁面（保留登入狀態，不需重新驗證）
6. **Given** 使用者在首頁點擊導航列的功能選項（例如「訂單管理」），**When** 功能模組尚未實作，**Then** 顯示「功能開發中」佔位頁面

---

### Edge Cases

- **Keycloak Token 過期**: 使用者在系統中操作時，Keycloak Token 過期後如何處理？系統應自動導向登入頁面並保留使用者原先操作的頁面路徑，登入後自動返回。
- **Session Timeout**: 使用者閒置超過 60 分鐘後，系統應自動登出並導向登入頁面。
- **多分頁限制**: 同一帳號僅允許單一分頁/視窗操作，避免狀態落差或單據衝突。當使用者在新分頁開啟系統時，原有分頁應顯示「已在其他分頁開啟」訊息並停止操作。
- **權限變更**: 使用者登入後，管理員修改其店別或系統別權限，使用者應在下次重新選擇時看到更新後的權限。
- **網路中斷**: 使用者在選擇流程中網路中斷，重新連線後應保留先前的選擇狀態（例如已選擇的主店別）。
- **無任何權限**: 使用者通過 Keycloak 驗證但在系統中無任何店別或系統別權限時，顯示「您目前無任何系統使用權限，請聯繫管理員」訊息。
- **6-Checkpoint 驗證失敗**: 使用者通過 Keycloak 驗證但未通過任一 checkpoint（帳號不存在、已停用、不在有效期間、無功能權限等）時，顯示對應的錯誤訊息。
- **全區權限使用者**: 主店別為 NULL 的使用者在查詢資料時，應能存取所有店別的資料，但仍需遵循功能權限（USING_FLAG）限制。

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系統必須整合 Keycloak 作為身份驗證提供者，所有使用者登入必須通過 Keycloak 驗證
- **FR-002**: 系統必須在使用者通過 Keycloak 驗證後，執行 6-checkpoint 驗證（使用者存在、SYSTEM_FLAG 非空、未停用、日期已設定、在有效期內、有功能權限）
- **FR-003**: 系統必須在 6-checkpoint 驗證通過後，導向店別選擇頁面
- **FR-004**: 系統必須從 TBL_USER_MAST_STORE 取得使用者被授權的主店別清單供選擇
- **FR-005**: 系統必須正確處理主店別 NULL 邏輯（NULL 代表「全區」權限，可存取所有店別）
- **FR-006**: 系統必須在使用者選擇主店別後，從 TBL_USER_STORE 檢查是否有支援店別權限，若有則顯示支援店別清單供多選
- **FR-007**: 系統必須允許使用者選擇多個支援店別（若有權限）
- **FR-008**: 系統必須在使用者完成店別選擇後，導向系統別選擇頁面
- **FR-009**: 系統必須解析 TBL_USER.SYSTEM_FLAG（comma-separated）並顯示使用者被授權的系統別選項（SO、TTS、APP）
- **FR-010**: 系統必須在使用者選擇系統別後，導向對應系統別的主頁面
- **FR-011**: 系統必須記錄使用者的登入資訊（時間、選擇的店別、系統別）於稽核日誌
- **FR-012**: 系統必須在使用者只有一個選項時（例如只有一個主店別或一個系統別），自動選擇並跳過該選擇步驟
- **FR-013**: 系統必須提供「切換系統別」功能，允許使用者在不重新登入的情況下切換到其他系統別
- **FR-014**: 系統必須在 Keycloak Token 過期時，自動導向登入頁面並保留原操作頁面路徑
- **FR-015**: 系統必須在使用者無任何權限時，顯示明確的無權限訊息並提供聯繫管理員的指引
- **FR-016**: 系統必須實作 60 分鐘的 Session Timeout，閒置超時後自動登出
- **FR-017**: 前端必須將登入資訊（Access Token、Refresh Token、選擇的店別與系統別）儲存於 Local Storage
- **FR-018**: 後端必須為 Stateless API 服務，不依賴 Server-Side Session
- **FR-019**: 前端必須在 Access Token 過期前自動使用 Refresh Token 刷新
- **FR-020**: 系統必須限制同一帳號僅能在單一瀏覽器分頁/視窗中操作，避免狀態落差或單據衝突
- **FR-021**: 當偵測到同一帳號在新分頁開啟時，原有分頁必須顯示警告訊息並停止操作功能
- **FR-022**: 系統首頁必須提供導航列（Nav Bar），包含以下功能選項：訂單管理、退貨管理、安運單管理、主檔維護、報表
- **FR-023**: 首頁右上角必須顯示登入者資訊，包含使用者名稱、所選店別、系統別
- **FR-024**: 首頁右上角必須提供「登出」按鈕，點擊後清除 Local Storage 中的登入資訊並導向登入頁面
- **FR-025**: 首頁右上角必須提供「切換系統」按鈕，點擊後導向通路/主店別/系統別選擇頁面（保留 Keycloak 登入狀態）
- **FR-026**: 尚未實作的功能模組必須顯示「功能開發中」佔位頁面

### Key Entities (映射至既有資料表)

- **User（使用者）**: 對應 TBL_USER，包含 USER_ID、USER_NAME、SYSTEM_FLAG（comma-separated 系統別權限）、DISABLE_FLAG、ENABLE_DATE、DISABLE_DATE
- **Channel（通路/系統別）**: 對應 TBL_CHANNEL，包含 CHANNEL_ID（SO、TTS、APP）、CHANNEL_NAME、CHANNEL_DESC
- **Store（店別）**: 對應 TBL_STORE，包含 STORE_ID、STORE_NAME、CHANNEL_ID（所屬通路）
- **UserMastStore（使用者主店別）**: 對應 TBL_USER_MAST_STORE，記錄使用者與主店別的對應關係，STORE_ID = NULL 代表全區權限
- **UserStore（使用者支援店別）**: 對應 TBL_USER_STORE，記錄使用者與支援店別的對應關係
- **LoginContext（登入上下文）**: 儲存於前端 Local Storage，包含 Keycloak Access Token、Refresh Token、使用者 ID、選擇的主店別、支援店別清單、系統別（無 Server-Side Session）

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 使用者可在 30 秒內完成從登入到進入系統首頁的完整流程（包含 Keycloak 驗證、店別選擇、系統別選擇）
- **SC-002**: 系統支援至少 500 個並行登入使用者而不影響登入速度與穩定性
- **SC-003**: Keycloak 驗證失敗時，90% 的使用者能在首次嘗試時成功登入（排除密碼錯誤）
- **SC-004**: 使用者在選擇店別與系統別時，95% 的使用者在首次操作時能正確完成選擇流程
- **SC-005**: 系統在 Keycloak Token 過期後，能在 2 秒內偵測並自動導向登入頁面
- **SC-006**: 所有登入操作（成功與失敗）100% 記錄於稽核日誌，包含時間戳記、使用者 ID、IP 位址、操作結果

## Assumptions

- Keycloak 服務已由基礎架構團隊建置完成並正常運作
- 使用者的店別與系統別權限資料已由管理員預先設定於系統資料庫中
- 使用者已由 Keycloak 管理員建立帳號並設定密碼
- 系統別（SO、TTS、APP）的定義與業務邏輯已明確定義
- 使用者使用桌面瀏覽器（Chrome、Edge、Firefox）進行操作，不考慮行動裝置瀏覽器
- 使用者網路環境穩定，網路延遲時間在合理範圍內（< 200ms）

## Out of Scope

本功能規格不包含以下項目：

- Keycloak 服務的安裝、設定與維護
- 使用者帳號的建立、修改、刪除（由 Keycloak 管理員負責）
- 使用者權限的管理介面（店別、系統別權限的指派）
- 多因素驗證（MFA）整合（若需要可作為未來增強功能）
- 單一登入（SSO）與其他系統的整合
- 使用者個人資訊管理（例如修改密碼、Email、手機號碼）
