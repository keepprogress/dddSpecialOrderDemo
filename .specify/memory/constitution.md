<!--
  Sync Impact Report:

  Version Change: 1.9.0 → 1.10.0

  Modified Principles: N/A

  Added Sections:
  - Core Principles XIII: Code Coverage Requirement (程式碼覆蓋率要求)

  Removed Sections: N/A

  Changes in v1.10.0:
  - 新增「Code Coverage Requirement」原則
  - 強制所有功能達到 80% 以上的程式碼覆蓋率
  - 定義 JaCoCo (Backend) 與 Jest/Karma (Frontend) 工具配置
  - 建立覆蓋率排除規則與 CI/CD 整合要求
  - 更新 Testing Strategy 區段，整合覆蓋率要求

  Templates Requiring Updates:
  - .specify/templates/tasks-template.md: 建議在 Polish Phase 加入覆蓋率驗證任務 (⚠ 可選更新)
  - .specify/templates/plan-template.md: 無需更新

  Follow-up TODOs:
  - 確認 backend 已配置 JaCoCo Maven plugin
  - 確認 frontend 已配置 Jest 或 Karma coverage reporter
  - 建議在 CI/CD pipeline 加入覆蓋率 gate

  ---

  Version History:

  v1.10.0 (2025-12-19):
  - 新增 Code Coverage Requirement 原則（MINOR: 新原則）

  v1.9.0 (2025-12-17):
  - 新增 Angular 21+ Frontend Standard 原則（MINOR: 新原則）

  v1.8.0 (2025-12-17):
  - 新增 OpenAPI RESTful API Standard 原則（MINOR: 新原則）

  v1.7.0 (2025-12-17):
  - 新增 Data Class Convention 原則（MINOR: 新原則）

  v1.6.0 (2025-12-17):
  - 新增 No Lombok Policy 原則（MINOR: 新原則）

  v1.5.0 (2025-12-17):
  - 新增 MyBatis Generator Pattern 原則（MINOR: 新原則）

  v1.4.0 (2025-12-17):
  - 新增 Playwright Verification 原則（MINOR: 新原則）

  v1.3.0 (2025-12-17):
  - 新增 Stateless Backend Architecture 原則（MINOR: 新原則）

  v1.2.0 (2025-12-17):
  - 新增 Legacy Codebase Reference 原則（MINOR: 新原則）

  v1.1.0 (2025-12-17):
  - 新增 Database Schema Documentation 原則（MINOR: 新原則）

  v1.0.1 (2025-12-17):
  - 補充 3-Table Rule 的性能考量與例外情況（PATCH: 澄清）

  v1.0.0 (2025-12-17):
  - Initial creation
  - Core Principles (Pragmatic DDD, 3-Table Rule, KISS Principle)
  - Architecture Standards
  - Development Workflow
  - Governance
-->

# DDD Special Order Demo Constitution

## Core Principles

### I. Pragmatic DDD (Domain-Driven Design)

務實地套用 DDD 戰術模式與分層架構：
- **實體 (Entity)**、**值物件 (Value Object)**、**聚合 (Aggregate)** 僅在必要時使用
- **領域服務 (Domain Service)** 用於協調跨實體的業務邏輯
- **分層架構** 維持清晰的依賴方向：Presentation → Application → Domain → Infrastructure
- **反腐層 (Anti-Corruption Layer)** 用於隔離外部系統的複雜性
- **避免過度設計**：不為了 DDD 而 DDD，優先考慮可讀性與可維護性

**理由**：DDD 提供了清晰的業務邏輯組織方式，但過度使用會導致不必要的複雜性。務實的 DDD 在保持業務清晰的同時，避免過度抽象。

### II. 3-Table Rule (三表原則)

**關聯在 3 個 Table 以內的領域應套用 DDD，超過 3 個 Table 的關聯需要重新審視設計：**
- **1-3 Table**：套用 DDD 戰術模式（Entity, Value Object, Aggregate）
- **3+ Table**：考慮是否需要拆分 Bounded Context（限界上下文）或使用更輕量的模式
- **理由**：複雜的關聯通常意味著領域邊界不清晰，或混合了多個業務概念

**性能考量（Performance Considerations）：**
- **索引風險**：多表關聯若無法有效使用資料庫索引，會將過濾條件轉移到應用層處理
- **I/O 風險**：這會導致百萬筆資料的 input/output，造成嚴重的性能瓶頸
- **查詢計畫**：超過 3 個 Table 的 JOIN 查詢，資料庫優化器難以生成最佳執行計畫
- **網路傳輸**：大量資料在資料庫與應用層之間傳輸，消耗網路頻寬與記憶體

**例外情況（Exceptions）：**
- **非歷史資料**：若 Table 確定不是會持續增長的歷史資料類型（例如：設定檔、代碼表、元資料），則無百萬筆資料的風險
- **小資料集**：已確認資料量小且成長可控的情況下（例如：< 10,000 筆），可放寬此限制
- **必須記錄**：即使符合例外情況，仍需在 `plan.md` 明確記錄資料量評估與成長預測

**理由**：3-Table Rule 是基於性能與設計的雙重考量。除了領域邊界的清晰性外，更重要的是避免因多表關聯導致的性能災難，尤其在處理大量歷史資料時。

### III. KISS Principle (Keep It Simple, Stupid)

**簡單優於複雜，可讀性優於聰明：**
- 優先選擇最簡單的解決方案
- 不為未來可能不會發生的需求設計（YAGNI - You Aren't Gonna Need It）
- 程式碼應該一目了然，避免過度抽象
- 抽象層級應該有明確的業務價值，而非技術炫技
- **測試優先**：複雜的設計必須通過測試證明其必要性

**理由**：簡單的程式碼更容易理解、測試、維護與除錯。過早的抽象與過度設計通常會增加技術債，而非減少。

### IV. Database Schema Documentation (資料表文件規範)

**當對資料表欄位名稱、長度、意義有疑問時，必須查閱 `docs/tables` 目錄：**
- **強制查閱**：在設計或實作涉及資料庫欄位的功能前，必須先查閱相關資料表的文件
- **文件位置**：`docs/tables/[TABLE_NAME].html` 包含完整的欄位定義、資料型別、長度限制、註解
- **避免假設**：不可根據欄位名稱猜測其意義、長度或限制，必須以文件為準
- **發現不一致**：若發現資料庫實際結構與文件不一致，必須立即記錄並通知團隊更新文件

**查閱時機（When to Consult）：**
1. **設計階段**：規劃新功能涉及資料表查詢或寫入時
2. **實作階段**：撰寫 SQL 查詢、Mapper XML 或 Entity 類別時
3. **除錯階段**：發現資料異常或欄位截斷問題時
4. **Code Review**：審查涉及資料庫操作的程式碼時

**理由**：資料表文件是資料庫結構的唯一真實來源（Single Source of Truth）。依賴文件而非假設，可避免欄位長度溢位、資料型別錯誤、業務邏輯誤解等常見問題。

### V. Legacy Codebase Reference (既有程式碼參考)

**當需要了解現有業務邏輯、實作模式或系統行為時，必須查閱既有程式碼庫：**
- **程式碼位置**：`C:/projects/som` 包含既有特殊訂單系統的完整實作
- **強制查閱**：在設計新功能或重構現有功能前，必須先搜尋既有程式碼了解現行做法
- **避免重複造輪**：既有系統中已解決的問題，應參考其解決方案而非從頭設計
- **保持一致性**：新開發的功能應與既有系統的命名慣例、資料結構保持一致

**查閱時機（When to Consult）：**
1. **設計階段**：了解既有業務流程與資料流向
2. **實作階段**：參考既有的程式碼模式、工具類別、共用元件
3. **整合階段**：確認與既有系統的介面規格、資料格式
4. **除錯階段**：對照既有系統行為以理解預期結果

**搜尋建議（Search Tips）：**
- 使用關鍵字搜尋業務術語（例如：訂單、店別、系統別）
- 搜尋資料表名稱以找出相關的資料存取程式碼
- 搜尋 API 端點名稱以了解既有服務介面

**理由**：既有程式碼庫是業務邏輯的實際實作來源。參考既有程式碼可確保新系統與現行業務邏輯一致，避免因理解偏差導致的功能差異。

### VI. Stateless Backend Architecture (前後端分離與無狀態後端)

**本專案採用前後端分離架構，與既有程式碼（C:/projects/som）有關鍵差異：**

**架構差異（Key Differences from Legacy）：**
- **既有系統**：Server-Side Session，登入狀態存於後端
- **本專案**：Stateless API，後端不維護 Session 狀態

**後端設計原則（Backend Principles）：**
- **Stateless API**：所有 API 端點必須是無狀態的，不依賴 Server-Side Session
- **Token 驗證**：每個 API 請求必須攜帶有效的 Keycloak Access Token
- **驗證機制**：後端透過 Keycloak 公鑰驗證 Token 簽章與有效期
- **例外設定**：可設定特定端點免除 Token 驗證（例如：健康檢查、公開資源）

**前端設計原則（Frontend Principles）：**
- **Local Storage**：登入資訊（Access Token、Refresh Token、使用者選擇）儲存於瀏覽器 Local Storage
- **Token 管理**：前端負責 Token 生命週期管理，包含自動刷新與過期處理
- **請求攔截**：所有 API 請求自動附加 Authorization Header（Bearer Token）

**Keycloak 整合（Keycloak Integration）：**
- **驗證流程**：OAuth2 Authorization Code Flow with PKCE
- **Token 類型**：Access Token（短效期）+ Refresh Token（長效期）
- **後端角色**：驗證 Token、解析使用者資訊、不發行或管理 Token

**例外端點設定（Exception Endpoints）：**
```
# 免驗證端點範例
/api/health          # 健康檢查
/api/public/**       # 公開資源
/actuator/**         # Spring Actuator（視環境設定）
```

**參考既有程式碼時的注意事項（Caution When Referencing Legacy Code）：**
- **不可直接複製** Session 相關的程式碼
- **需要轉換** 將 Session 存取邏輯改為從 Token 解析使用者資訊
- **業務邏輯可參考**：資料驗證、計算邏輯、資料庫操作等非 Session 相關邏輯

**理由**：前後端分離架構提供更好的擴展性與部署彈性。Stateless 後端可水平擴展，不需要 Session 共享機制。前端管理 Token 可減少後端負擔，並支援 SPA（Single Page Application）架構。

### VII. Playwright Verification (Playwright 驗證規範)

**實作一段落可以運行後，必須透過 Playwright 進行自動化驗證：**

**驗證時機（When to Verify）：**
- **階段完成時**：每個 User Story 或功能模組實作完成且可運行時
- **整合完成時**：前後端整合完成後
- **修復完成時**：Bug 修復後確認問題已解決

**驗證流程（Verification Process）：**
1. **啟動應用程式**：確保前後端服務正常運行
2. **執行 Playwright 測試**：運行對應的 E2E 測試腳本
3. **截圖確認**：測試過程中自動截圖，記錄每個步驟的畫面狀態
4. **錯誤訊息檢查**：確認畫面上是否有非預期的錯誤訊息
5. **結果判定**：根據測試結果決定下一步行動

**截圖規範（Screenshot Requirements）：**
- **關鍵步驟截圖**：登入、選擇、提交等關鍵操作前後必須截圖
- **錯誤狀態截圖**：任何錯誤訊息或異常狀態必須截圖保存
- **截圖命名**：使用有意義的命名（例如：`login-success.png`、`store-selection-error.png`）
- **截圖位置**：儲存於 `e2e/screenshots/` 目錄

**驗證結果處理（Result Handling）：**
- **通過（Pass）**：所有測試通過且無非預期錯誤訊息，可進入下一階段
- **失敗（Fail）**：根據截圖與錯誤訊息分析問題，進行必要修改後重新驗證
- **部分通過（Partial）**：記錄已知問題，評估是否阻擋後續開發

**Playwright 測試範圍（Test Scope）：**
```
e2e/
├── tests/
│   ├── auth/           # 登入驗證相關測試
│   ├── navigation/     # 導航列功能測試
│   └── store-select/   # 店別選擇測試
├── screenshots/        # 測試截圖
└── playwright.config.ts
```

**理由**：Playwright 自動化驗證提供客觀、可重複的測試結果。截圖記錄可作為驗證證據，並協助快速定位問題。在實作階段即進行驗證，可及早發現問題並降低修復成本。

### VIII. MyBatis Generator Pattern (MyBatis 生成器模式)

**本專案使用 MyBatis 作為資料存取層，並採用 MyBatisGenerator 自動產生基礎程式碼：**

**自動產生原則（Auto-Generation Principles）：**
- **Mapper 介面**：由 MyBatisGenerator 自動產生，包含基本 CRUD 方法
- **Entity 類別**：由 MyBatisGenerator 自動產生，對應資料表結構，包含完整的 getter/setter
- **Mapper XML**：由 MyBatisGenerator 自動產生，包含基本 SQL 對應
- **不可手動修改**：自動產生的檔案不可手動修改，重新產生時會被覆蓋

**自動產生檔案位置（Generated File Locations）：**
```
backend/src/main/java/com/tgfc/som/
├── mapper/                    # MyBatisGenerator 產生的 Mapper 介面
│   ├── UserMapper.java
│   ├── StoreMapper.java
│   └── ...
├── entity/                    # MyBatisGenerator 產生的 Entity 類別
│   ├── User.java
│   ├── Store.java
│   └── ...
└── mapper/xml/                # MyBatisGenerator 產生的 Mapper XML
    ├── UserMapper.xml
    └── ...
```

**CustomMapper 使用時機（When to Use CustomMapper）：**
CustomMapper 是手動撰寫的 Mapper，僅在以下情況使用：
1. **SQL 複雜度高**：多表 JOIN、子查詢、複雜條件等無法用基本 CRUD 表達
2. **效能考量**：需要特殊的索引提示、分頁優化、批次處理
3. **特殊需求**：需要呼叫預存程序、使用資料庫特定函數

**CustomMapper 檔案位置（CustomMapper File Locations）：**
```
backend/src/main/java/com/tgfc/som/
├── mapper/custom/             # 手動撰寫的 CustomMapper 介面
│   ├── UserCustomMapper.java
│   └── ...
└── mapper/custom/xml/         # 手動撰寫的 CustomMapper XML
    ├── UserCustomMapper.xml
    └── ...
```

**命名規範（Naming Conventions）：**
- **自動產生**：`[EntityName]Mapper.java`、`[EntityName]Mapper.xml`
- **手動撰寫**：`[EntityName]CustomMapper.java`、`[EntityName]CustomMapper.xml`

**CustomMapper 審核要求（CustomMapper Review Requirements）：**
- **必須說明理由**：在程式碼註解或 PR 說明中記錄為何需要 CustomMapper
- **效能佐證**：若因效能考量，需提供查詢計畫或效能測試結果
- **Code Review**：所有 CustomMapper 必須經過 Code Review 確認必要性

**禁止事項（Prohibited Actions）：**
- **禁止修改自動產生的檔案**：任何客製需求應在 CustomMapper 中實作
- **禁止繞過 Generator**：不可手動建立與自動產生格式相同的 Mapper
- **禁止重複實作**：已在自動產生 Mapper 中存在的方法，不可在 CustomMapper 重複實作

**理由**：MyBatisGenerator 確保基礎 CRUD 程式碼的一致性與正確性，減少人為錯誤。將客製化需求集中於 CustomMapper，可明確區分自動產生與手動撰寫的程式碼，便於維護與重新產生。僅在確有必要時使用 CustomMapper，避免過度客製化導致的維護負擔。

### IX. No Lombok Policy (禁用 Lombok)

**本專案禁止使用 Lombok，所有 Java 類別必須手動撰寫標準程式碼：**

**禁止原因（Why Lombok is Prohibited）：**
- **Java Naming Convention 問題**：Lombok 在處理特定命名模式時會產生不符合預期的 getter/setter
  - 例如：`isActive` 欄位會產生 `isActive()` 而非 `getIsActive()`
  - 例如：`sName` 欄位會產生 `getSName()` 或 `getsName()`，行為不一致
- **IDE 支援問題**：部分 IDE 或工具可能無法正確解析 Lombok 產生的程式碼
- **除錯困難**：編譯時產生的程式碼在除錯時不可見，增加問題排查難度
- **隱性依賴**：Lombok 是編譯時依賴，升級 Java 版本時可能產生相容性問題

**禁用範圍（Scope of Prohibition）：**
- **Entity 類別**：禁止使用 `@Data`、`@Getter`、`@Setter`、`@Builder` 等
- **DTO 類別**：禁止使用任何 Lombok 註解
- **Service 類別**：禁止使用 `@RequiredArgsConstructor`、`@Slf4j` 等
- **所有 Java 類別**：全面禁止 Lombok

**替代方案（Alternatives）：**
- **getter/setter**：使用 IDE 自動產生功能（Alt+Insert in IntelliJ）
- **constructor**：手動撰寫或使用 IDE 產生
- **toString/equals/hashCode**：手動撰寫或使用 IDE 產生
- **Builder Pattern**：手動實作 Builder 類別
- **Logger**：直接宣告 `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`

**MyBatisGenerator 設定（Generator Configuration）：**
- **禁止使用 LombokPlugin**：generatorConfig.xml 不可包含 Lombok 相關 plugin
- **產生完整程式碼**：Generator 必須產生完整的 getter/setter/constructor

**Code Review 檢查項目（Review Checklist）：**
- [ ] 確認 pom.xml 不包含 Lombok 依賴
- [ ] 確認無任何 Lombok 註解（@Data, @Getter, @Setter, @Builder, @Slf4j 等）
- [ ] 確認 Entity/DTO 類別包含完整的 getter/setter
- [ ] 確認 generatorConfig.xml 無 LombokPlugin

**理由**：避免因 Lombok 與 Java naming convention 的交互作用導致不預期的錯誤。手動撰寫的程式碼雖然較為冗長，但行為明確可預測，便於除錯與維護。IDE 的自動產生功能可以有效減少手動撰寫的負擔。

### X. Data Class Convention (資料類別規範)

**本專案採用 Java Records 作為不可變資料類別的首選，並與原則 IX (No Lombok Policy) 整合：**

**使用 Java Records 的場景（When to Use Records）：**

| 場景 | 說明 | 範例 |
|------|------|------|
| **API Request DTO** | Controller 接收的請求參數 | `CreateOrderRequest`, `LoginRequest` |
| **API Response DTO** | Controller 回傳的回應資料 | `OrderResponse`, `UserInfoResponse` |
| **MyBatis 查詢結果** | 唯讀的查詢結果 DTO | `OrderSummaryDTO`, `StoreListDTO` |
| **Value Object** | 領域層的值物件 | `Money`, `Address`, `DateRange` |
| **Service 間傳遞** | Application Service 間的資料傳遞 | `OrderCreatedEvent`, `UserContext` |

**不使用 Java Records 的場景（When NOT to Use Records）：**

| 場景 | 原因 | 替代方案 |
|------|------|----------|
| **MyBatis Entity** | 需要 setter 進行 ORM 映射 | 傳統 Class + getter/setter |
| **Builder Pattern** | 欄位超過 20 個需要彈性建構 | 傳統 Class + Builder 內部類別 |
| **部分更新參數** | 欄位可能為 null 表示「不更新」 | 傳統 Class + Optional 或 null 標記 |
| **繼承需求** | Records 不支援繼承 | 傳統 Class |
| **可變狀態** | 需要在物件生命週期內修改狀態 | 傳統 Class |

**命名規範（Naming Conventions）：**

| 類型 | 後綴 | 範例 |
|------|------|------|
| API 請求 | `*Request` | `CreateOrderRequest` |
| API 回應 | `*Response` | `OrderDetailResponse` |
| 查詢結果 DTO | `*DTO` | `OrderSummaryDTO` |
| 命令物件 | `*Command` | `CreateOrderCommand` |
| 查詢條件 | `*Criteria` | `OrderSearchCriteria` |
| Value Object | 無後綴 | `Money`, `Address` |

**Records 語法範例（Syntax Examples）：**

```java
// API Request DTO
public record CreateOrderRequest(
    String customerId,
    List<OrderItemRequest> items,
    String deliveryAddress
) {}

// API Response DTO
public record OrderResponse(
    String orderId,
    String status,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {}

// MyBatis 查詢結果 DTO
public record OrderSummaryDTO(
    String orderId,
    String customerName,
    BigDecimal totalAmount,
    int itemCount
) {}

// Value Object
public record Money(
    BigDecimal amount,
    String currency
) {
    // Records 可以有額外方法
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

**MyBatis 與 Records 整合（MyBatis Integration）：**

```xml
<!-- Mapper XML 中使用 Records -->
<resultMap id="orderSummaryMap" type="com.tgfc.som.dto.OrderSummaryDTO">
    <constructor>
        <arg column="ORDER_ID" javaType="String"/>
        <arg column="CUSTOMER_NAME" javaType="String"/>
        <arg column="TOTAL_AMOUNT" javaType="java.math.BigDecimal"/>
        <arg column="ITEM_COUNT" javaType="int"/>
    </constructor>
</resultMap>

<!-- Records 作為查詢參數 -->
<select id="findOrders" resultMap="orderSummaryMap">
    SELECT ORDER_ID, CUSTOMER_NAME, TOTAL_AMOUNT, ITEM_COUNT
    FROM V_ORDER_SUMMARY
    WHERE STORE_ID = #{criteria.storeId}
    AND STATUS = #{criteria.status}
</select>
```

**與原則 IX 的關係（Relationship with No Lombok Policy）：**
- **Records 取代 Lombok**：Java Records 提供不可變資料類別的簡潔語法，無需 Lombok 的 `@Value` 或 `@Data`
- **Entity 仍需手動撰寫**：MyBatis Entity 需要 setter，不適用 Records，需手動撰寫 getter/setter
- **IDE 支援**：Records 是 Java 標準語法，所有 IDE 完整支援，無 Lombok 的工具相容問題

**Code Review 檢查項目（Review Checklist）：**
- [ ] 確認不可變 DTO 使用 Records 而非傳統 Class
- [ ] 確認 MyBatis Entity 使用傳統 Class（有 getter/setter）
- [ ] 確認命名規範正確（Request, Response, DTO, Command, Criteria）
- [ ] 確認 Records 的欄位順序與 MyBatis resultMap constructor 參數順序一致

**理由**：Java Records 是 Java 16+ 的標準語法，提供簡潔的不可變資料類別定義。使用 Records 可在遵守「No Lombok Policy」的同時，避免冗長的 getter/toString/equals/hashCode 手動撰寫。明確區分 Records（不可變 DTO）與傳統 Class（可變 Entity），可提升程式碼的清晰度與意圖表達。

### XI. OpenAPI RESTful API Standard (OpenAPI RESTful API 規範)

**本專案所有 API 必須遵循 OpenAPI 3.0+ 規範與 RESTful 設計原則：**

**OpenAPI 規範要求（OpenAPI Specification Requirements）：**
- **版本**：使用 OpenAPI 3.0 或更高版本
- **文件位置**：`docs/api/openapi.yaml` 或透過 springdoc-openapi 自動產生於 `/v3/api-docs`
- **文件同步**：API 實作必須與 OpenAPI 文件保持同步，任一方變更時需同步更新

**API 設計方式（API Design Approach）：**

| 方式 | 說明 | 適用場景 |
|------|------|----------|
| **Contract-First** | 先撰寫 OpenAPI YAML，再實作 Controller | 跨團隊協作、API 優先設計 |
| **Code-First** | 使用註解自動產生 OpenAPI 文件 | 快速迭代、單一團隊開發 |

**本專案採用 Code-First 方式**，使用 `springdoc-openapi` 自動產生文件。

**RESTful 資源命名規範（Resource Naming Conventions）：**

| 規則 | 正確範例 | 錯誤範例 |
|------|----------|----------|
| 使用名詞複數 | `/api/users` | `/api/user`, `/api/getUsers` |
| 使用小寫與連字號 | `/api/order-items` | `/api/orderItems`, `/api/OrderItems` |
| 避免動詞 | `/api/orders` (POST 建立) | `/api/createOrder` |
| 階層式資源 | `/api/users/{userId}/orders` | `/api/getUserOrders` |
| 版本前綴 | `/api/v1/users` | `/v1/api/users` |

**HTTP 方法使用規範（HTTP Method Usage）：**

| 方法 | 用途 | 冪等性 | 請求體 | 範例 |
|------|------|--------|--------|------|
| **GET** | 查詢資源 | 是 | 無 | `GET /api/users/{id}` |
| **POST** | 建立資源 | 否 | 有 | `POST /api/users` |
| **PUT** | 完整更新資源 | 是 | 有 | `PUT /api/users/{id}` |
| **PATCH** | 部分更新資源 | 是 | 有 | `PATCH /api/users/{id}` |
| **DELETE** | 刪除資源 | 是 | 無 | `DELETE /api/users/{id}` |

**HTTP 狀態碼使用規範（HTTP Status Code Usage）：**

| 狀態碼 | 含義 | 使用場景 |
|--------|------|----------|
| **200 OK** | 成功 | GET 查詢成功、PUT/PATCH 更新成功 |
| **201 Created** | 已建立 | POST 建立資源成功 |
| **204 No Content** | 無內容 | DELETE 刪除成功、無需回傳內容 |
| **400 Bad Request** | 請求錯誤 | 參數驗證失敗、格式錯誤 |
| **401 Unauthorized** | 未授權 | Token 無效或過期 |
| **403 Forbidden** | 禁止存取 | 無權限存取該資源 |
| **404 Not Found** | 找不到資源 | 資源不存在 |
| **409 Conflict** | 衝突 | 資源狀態衝突（如重複建立） |
| **422 Unprocessable Entity** | 無法處理 | 業務邏輯驗證失敗 |
| **500 Internal Server Error** | 伺服器錯誤 | 非預期的系統錯誤 |

**請求與回應格式（Request/Response Format）：**

```java
// 標準成功回應
public record ApiResponse<T>(
    boolean success,
    T data,
    String message
) {}

// 標準錯誤回應
public record ErrorResponse(
    String errorCode,
    String message,
    List<FieldError> fieldErrors,
    LocalDateTime timestamp
) {}

public record FieldError(
    String field,
    String message
) {}
```

**範例 API 定義（Example API Definition）：**

```yaml
openapi: 3.0.3
info:
  title: DDD Special Order API
  version: 1.0.0
paths:
  /api/v1/users/{userId}:
    get:
      summary: 取得使用者資訊
      operationId: getUserById
      tags:
        - Users
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
        '404':
          description: 使用者不存在
```

**Spring Boot Controller 範例（Controller Example）：**

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "使用者管理 API")
public class UserController {

    @Operation(summary = "取得使用者資訊")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "使用者不存在")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserById(
        @Parameter(description = "使用者 ID") @PathVariable String userId
    ) {
        // ...
    }

    @Operation(summary = "建立使用者")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "建立成功"),
        @ApiResponse(responseCode = "400", description = "參數驗證失敗")
    })
    @PostMapping
    public ResponseEntity<UserInfoResponse> createUser(
        @Valid @RequestBody CreateUserRequest request
    ) {
        // ...
    }
}
```

**springdoc-openapi 設定（Configuration）：**

```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true  # 開發環境啟用
  show-actuator: false
```

**Code Review 檢查項目（Review Checklist）：**
- [ ] API 路徑使用名詞複數、小寫、連字號
- [ ] HTTP 方法符合語意（GET 查詢、POST 建立、PUT/PATCH 更新、DELETE 刪除）
- [ ] 狀態碼使用正確（201 建立、204 刪除、4xx 客戶端錯誤、5xx 伺服器錯誤）
- [ ] Controller 方法有 `@Operation` 與 `@ApiResponses` 註解
- [ ] Request/Response DTO 有 `@Schema` 註解說明欄位
- [ ] API 路徑有版本前綴 `/api/v1/`

**理由**：OpenAPI 規範提供標準化的 API 文件格式，便於前後端協作、自動產生客戶端程式碼、API 測試與文件維護。RESTful 設計原則確保 API 語意清晰、可預測，降低學習成本與溝通成本。

### XII. Angular 21+ Frontend Standard (Angular 21+ 前端規範)

**本專案前端採用 Angular 21+ 最新架構模式，必須遵循以下規範：**

**核心技術棧（Core Technologies）：**
- **Framework**: Angular 21+ (Standalone, Signals, Zoneless-ready)
- **Language**: TypeScript 5.9+
- **Build Tool**: Angular CLI (Esbuild/Vite)
- **Change Detection**: OnPush by default

**強制規範（MUST Rules）：**

| 規範 | 說明 | 範例 |
|------|------|------|
| **Standalone Components** | 所有元件必須使用 `standalone: true` | `@Component({ standalone: true })` |
| **Angular Signals** | 使用 Signals 管理本地狀態 | `signal()`, `computed()`, `input()`, `output()` |
| **新控制流語法** | 使用 `@if`, `@for`, `@switch` | 取代 `*ngIf`, `*ngFor`, `[ngSwitch]` |
| **inject() 函數** | 使用 `inject()` 進行依賴注入 | 取代 constructor injection |
| **OnPush 策略** | 預設使用 `ChangeDetectionStrategy.OnPush` | 提升效能 |

**禁止使用（AVOID）：**
- `NgModule`（除非整合舊版套件必須使用）
- `@Input()` / `@Output()` 裝飾器（改用 `input()` / `output()` 函數）
- `*ngIf`, `*ngFor`, `[ngSwitch]` 舊語法
- Constructor injection（改用 `inject()` 函數）

**Signal-based Inputs/Outputs（信號式輸入輸出）：**

```typescript
// 舊寫法（禁止）
@Input() userId: string;
@Output() userSelected = new EventEmitter<User>();

// 新寫法（必須）
userId = input.required<string>();
userSelected = output<User>();

// 雙向綁定
selectedValue = model<string>();
```

**狀態管理策略（State Management）：**

| 場景 | 建議方案 | 說明 |
|------|----------|------|
| 同步狀態 / UI 綁定 | **Signals** | `signal()`, `computed()` |
| 非同步事件 (HTTP) | **RxJS** | `HttpClient`, `toSignal()` 轉換 |
| 複雜流程編排 | **RxJS** | `switchMap`, `combineLatest` 等 |
| Observable → Signal | **toSignal()** | 供 Template 消費 |

**新控制流語法範例（Control Flow Examples）：**

```html
<!-- 舊寫法（禁止） -->
<div *ngIf="users$ | async as users">
  <div *ngFor="let user of users">{{ user.name }}</div>
</div>

<!-- 新寫法（必須） -->
@if (users(); as usersList) {
  @for (user of usersList; track user.id) {
    <div>{{ user.name }}</div>
  } @empty {
    <p>No users found.</p>
  }
}

<!-- Switch 語法 -->
@switch (status()) {
  @case ('loading') {
    <app-spinner />
  }
  @case ('error') {
    <app-error [message]="errorMessage()" />
  }
  @default {
    <app-content [data]="data()" />
  }
}
```

**元件範例（Component Example）：**

```typescript
import { Component, signal, computed, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>{{ userName() }}</h1>
    <p>Orders: {{ orderCount() }}</p>

    @if (isLoading()) {
      <app-spinner />
    } @else {
      @for (order of orders(); track order.id) {
        <app-order-card [order]="order" />
      }
    }
  `
})
export class UserProfileComponent {
  // 依賴注入
  private userService = inject(UserService);

  // Signal inputs
  userId = input.required<string>();

  // Local signals
  isLoading = signal(false);

  // Computed signals
  userName = computed(() => this.user()?.name ?? 'Unknown');
  orderCount = computed(() => this.orders().length);

  // Observable to Signal
  user = toSignal(this.userService.getUser(this.userId()));
  orders = toSignal(this.userService.getOrders(this.userId()), { initialValue: [] });
}
```

**檔案命名規範（File Naming Conventions）：**

| 類型 | 格式 | 範例 |
|------|------|------|
| Component | `kebab-case.component.ts` | `user-profile.component.ts` |
| Service | `kebab-case.service.ts` | `user-profile.service.ts` |
| Interface | `kebab-case.interface.ts` | `user-response.interface.ts` |
| Pipe | `kebab-case.pipe.ts` | `date-format.pipe.ts` |
| Directive | `kebab-case.directive.ts` | `highlight.directive.ts` |
| Guard | `kebab-case.guard.ts` | `auth.guard.ts` |

**類別命名規範（Class Naming Conventions）：**

| 類型 | 格式 | 範例 |
|------|------|------|
| Component | `PascalCaseComponent` | `UserProfileComponent` |
| Service | `PascalCaseService` | `UserProfileService` |
| Interface | `PascalCase` | `UserResponse` |
| Pipe | `PascalCasePipe` | `DateFormatPipe` |

**專案結構（Project Structure）：**

```
frontend/src/app/
├── core/                    # 核心模組（單例服務、攔截器）
│   ├── services/
│   │   ├── auth.service.ts
│   │   └── http-error.interceptor.ts
│   └── guards/
├── shared/                  # 共用元件、Pipes、Directives
│   ├── components/
│   ├── pipes/
│   └── directives/
├── features/                # 功能模組（依功能分類）
│   ├── login/
│   │   ├── login.component.ts
│   │   └── login.routes.ts
│   └── dashboard/
├── app.component.ts
├── app.config.ts            # Application configuration
└── app.routes.ts            # Root routing
```

**效能優化（Performance Optimization）：**
- 使用 `@defer` 延遲載入元件（Deferrable Views）
- 使用 `NgOptimizedImage` 優化圖片載入
- 使用 `trackBy` (在 `@for` 中使用 `track`)

```html
<!-- 延遲載入 -->
@defer (on viewport) {
  <app-heavy-component />
} @placeholder {
  <div>Loading...</div>
}
```

**Code Review 檢查項目（Review Checklist）：**
- [ ] 元件使用 `standalone: true`
- [ ] 使用 `inject()` 函數進行依賴注入
- [ ] 使用新控制流語法 (`@if`, `@for`, `@switch`)
- [ ] 使用 Signal-based inputs/outputs (`input()`, `output()`)
- [ ] 設定 `ChangeDetectionStrategy.OnPush`
- [ ] 檔案命名符合 `kebab-case` 規範
- [ ] 無 `NgModule` 使用（除非必要並記錄原因）

**理由**：Angular 21+ 引入的 Standalone Components、Signals 與新控制流語法大幅簡化了開發模式，減少樣板程式碼。Signals 提供更直觀的狀態管理，搭配 OnPush 策略可顯著提升效能。統一的程式碼風格與架構模式有助於團隊協作與程式碼維護。

### XIII. Code Coverage Requirement (程式碼覆蓋率要求)

**本專案所有功能必須達到 80% 以上的程式碼覆蓋率：**

**覆蓋率要求（Coverage Requirements）：**

| 指標 | 最低要求 | 說明 |
|------|----------|------|
| **Line Coverage** | ≥ 80% | 程式碼行覆蓋率 |
| **Branch Coverage** | ≥ 80% | 分支覆蓋率（if/else, switch, 三元運算子） |
| **Class Coverage** | ≥ 80% | 類別覆蓋率（建議，非強制） |
| **Method Coverage** | ≥ 80% | 方法覆蓋率（建議，非強制） |

**工具配置（Tool Configuration）：**

**Backend (Java - JaCoCo)：**

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Frontend (Angular - Jest/Karma)：**

```javascript
// jest.config.js
module.exports = {
  coverageThreshold: {
    global: {
      branches: 80,
      functions: 80,
      lines: 80,
      statements: 80
    }
  }
};

// karma.conf.js
coverageReporter: {
  check: {
    global: {
      statements: 80,
      branches: 80,
      functions: 80,
      lines: 80
    }
  }
}
```

**覆蓋率排除規則（Exclusion Rules）：**

可排除計算的程式碼類型：

| 類型 | 說明 | 排除原因 |
|------|------|----------|
| **自動產生的程式碼** | MyBatisGenerator 產生的 Entity/Mapper | 由工具產生，無需測試 |
| **配置類別** | `*Config.java`, `*Configuration.java` | 主要為 Bean 定義，無業務邏輯 |
| **DTO/Record** | 純資料類別，無邏輯 | getter/setter/constructor 無測試價值 |
| **Exception 類別** | 自定義例外類別 | 通常只有建構子 |
| **Main Application** | `*Application.java` | Spring Boot 啟動類別 |

**JaCoCo 排除設定：**

```xml
<configuration>
    <excludes>
        <exclude>**/entity/**</exclude>
        <exclude>**/mapper/**</exclude>
        <exclude>**/*Config.class</exclude>
        <exclude>**/*Configuration.class</exclude>
        <exclude>**/*Application.class</exclude>
        <exclude>**/*Request.class</exclude>
        <exclude>**/*Response.class</exclude>
        <exclude>**/*DTO.class</exclude>
        <exclude>**/*Exception.class</exclude>
    </excludes>
</configuration>
```

**驗證時機（When to Verify）：**

| 時機 | 動作 | 阻擋規則 |
|------|------|----------|
| **本地開發** | `mvn test` / `npm test` | 可選執行 |
| **Pull Request** | CI/CD pipeline 自動執行 | 未達 80% 則 PR 無法合併 |
| **主分支推送** | CI/CD pipeline 自動執行 | 未達 80% 則建置失敗 |

**CI/CD 整合範例（GitHub Actions）：**

```yaml
# .github/workflows/ci.yml
- name: Run Backend Tests with Coverage
  run: mvn test jacoco:report jacoco:check

- name: Run Frontend Tests with Coverage
  run: npm run test:coverage -- --coverage-threshold='{"global":{"lines":80,"branches":80}}'

- name: Upload Coverage Report
  uses: codecov/codecov-action@v3
  with:
    files: ./backend/target/site/jacoco/jacoco.xml,./frontend/coverage/lcov.info
```

**例外申請流程（Exception Request）：**

若特定程式碼無法達到 80% 覆蓋率，必須：

1. **說明原因**：在 PR 描述中說明為何無法達到覆蓋率
2. **技術評估**：說明該程式碼的測試困難點（例如：第三方整合、硬體相依）
3. **替代方案**：說明如何透過其他方式確保品質（例如：整合測試、手動測試）
4. **審核批准**：需技術負責人書面批准
5. **記錄追蹤**：在 `plan.md` 的 Complexity Tracking 表格中記錄

**Code Review 檢查項目（Review Checklist）：**

- [ ] PR 包含對應的單元測試
- [ ] 覆蓋率報告顯示 Line Coverage ≥ 80%
- [ ] 覆蓋率報告顯示 Branch Coverage ≥ 80%
- [ ] 新增的商業邏輯有對應的測試案例
- [ ] 邊界條件與錯誤處理有測試覆蓋
- [ ] 若低於 80%，已提供正當理由並獲批准

**報告位置（Report Locations）：**

```
backend/target/site/jacoco/index.html    # JaCoCo HTML 報告
frontend/coverage/lcov-report/index.html # Jest/Karma HTML 報告
```

**理由**：80% 程式碼覆蓋率是業界公認的品質基準線。足夠的測試覆蓋率可以：
- **降低迴歸風險**：重構或修改時，測試可及早發現問題
- **提升信心**：開發者對程式碼變更更有信心
- **文件化行為**：測試案例本身就是程式碼行為的文件
- **促進良好設計**：難以測試的程式碼通常意味著設計問題

過高的覆蓋率要求（如 95%+）可能導致測試價值降低（為覆蓋率而測試），80% 是效益與成本的最佳平衡點。

## Architecture Standards

### Technology Stack Requirements

**Backend (後端)**：
- Framework: Spring Boot 3.x+
- Language: Java 21+
- Build Tool: Maven
- Database: Oracle 21c (Production), H2 (Development)
- Security: Jasypt encryption for sensitive configuration
- Data Access: MyBatis + MyBatisGenerator (without Lombok)
- API Documentation: springdoc-openapi (OpenAPI 3.0+)
- Test Coverage: JaCoCo (≥ 80% line & branch coverage)

**Frontend (前端)**：
- Framework: Angular 21+ (Standalone, Signals, Zoneless-ready)
- Language: TypeScript 5.9+
- Build Tool: Angular CLI (Esbuild/Vite)
- State Management: Angular Signals (preferred) + RxJS (async operations)
- Change Detection: OnPush by default
- Test Coverage: Jest/Karma (≥ 80% line & branch coverage)

### DDD Layer Architecture

```
Presentation Layer (展示層)
└── API Controllers / Angular Components

Application Layer (應用層)
└── Application Services / Use Cases

Domain Layer (領域層)
└── Entities, Value Objects, Domain Services, Aggregates

Infrastructure Layer (基礎設施層)
└── Repositories (MyBatis Mappers), External API Clients, Database Access
```

**Dependencies Flow**: Presentation → Application → Domain ← Infrastructure
**Rule**: 領域層 (Domain Layer) 不可依賴基礎設施層 (Infrastructure Layer)

### Bounded Context Guidelines

- **識別標準**：當發現超過 3-Table 關聯時，優先考慮拆分 Bounded Context
- **Context Map**：不同 Bounded Context 之間使用明確的 Context Map 定義關係
- **整合方式**：Bounded Context 間透過 Anti-Corruption Layer 或 Published Language 整合

## Development Workflow

### Constitution Compliance Gates

**Phase 0 (Research)**：
1. 確認功能範圍是否超過 3-Table 關聯
2. 若超過，明確記錄拆分 Bounded Context 的理由或設計決策

**Phase 1 (Design)**：
1. 驗證 DDD 戰術模式的使用是否合理（避免過度設計）
2. 確認分層架構的依賴方向正確
3. 檢查是否遵循 KISS 原則（是否有不必要的抽象）
4. 評估是否需要 CustomMapper（記錄理由）
5. 確認設計中未使用 Lombok

**Phase 2 (Implementation)**：
1. Code Review 必須檢查 3-Table Rule 合規性
2. 複雜設計必須有測試證明其必要性
3. 拒絕未通過 Constitution Check 的 Pull Request
4. 確認自動產生的 Mapper/Entity 未被手動修改
5. 確認無 Lombok 依賴與註解
6. **確認程式碼覆蓋率達到 80% 以上**

### Testing Strategy

- **單元測試 (Unit Tests)**：測試領域層的業務邏輯，確保 80% 以上覆蓋率
- **整合測試 (Integration Tests)**：測試跨層的互動與資料庫操作
- **契約測試 (Contract Tests)**：測試 API 契約的一致性
- **E2E 測試 (Playwright)**：實作階段完成後，透過 Playwright 進行端對端驗證，截圖確認畫面狀態與錯誤訊息
- **覆蓋率驗證**：PR 合併前必須通過 JaCoCo (Backend) / Jest (Frontend) 80% 覆蓋率檢查

### Complexity Justification

任何違反 Constitution 的設計（例如：超過 3-Table 關聯、過度抽象、CustomMapper 使用、覆蓋率低於 80%）必須在 `plan.md` 的 **Complexity Tracking** 表格中記錄：
- 違反的原則
- 為何需要此複雜度
- 為何較簡單的替代方案不可行

## Governance

### Amendment Process

1. **提案**：任何團隊成員可提出 Constitution 修正案
2. **討論**：團隊討論修正案的影響範圍與必要性
3. **批准**：需要技術負責人批准
4. **遷移**：批准後，更新相關文件與模板，並提供遷移計畫

### Versioning Policy

- **MAJOR 版本**：原則的刪除或重大重新定義（例如：移除 3-Table Rule）
- **MINOR 版本**：新增原則或擴充現有指導方針
- **PATCH 版本**：澄清、措辭修改、錯字修正

### Compliance Review

- **Pull Request Review**：所有 PR 必須驗證 Constitution 合規性
- **定期審查**：每季度檢視 Constitution 的有效性與適用性
- **違規處理**：違反 Constitution 的程式碼必須重構或提供明確的違規理由

**Version**: 1.10.0 | **Ratified**: 2025-12-17 | **Last Amended**: 2025-12-19
