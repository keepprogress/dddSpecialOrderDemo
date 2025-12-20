# Feature Specification: 新增訂單頁面

**Feature Branch**: `002-create-order`
**Created**: 2025-12-19
**Status**: Draft
**Input**: 基於 DDD 領域模型規格書與既有系統逆向工程文件，建立新增訂單功能規格

## Clarifications

### Session 2025-12-19

- Q: 商品數量超過試算上限時，系統應如何處理？ → A: 試算限制 500 筆（可配置於 TBL_PARM.SKU_COMPUTE_LIMIT），結帳限制 1000 筆（可配置於 TBL_PARM.ORDER_DETL_LIMIT）。超過試算限制時阻止試算並提示分單；超過結帳限制時阻止結帳並提示拆單
- Q: 是否應突破 1000 筆訂單明細限制？ → A: 否 - 基於 KISS 原則與維運考量，保留此限制。突破限制需處理 Oracle IN 語法 1000 元素限制，增加程式複雜度與維運風險。拆單為既有合理業務流程，大單拆分亦有利於物流與安裝排程
- Q: 價格試算 API 的最大可接受回應時間為何？ → A: 3 秒 - 標準效能，允許複雜計算但仍具回應性
- Q: 防止重複提交訂單的機制應採用何種方式？ → A: 前後端雙重 - 前端按鈕禁用 + 後端冪等鍵檢查
- Q: 優惠券折扣金額超過可折抵商品總額時如何處理？ → A: 封頂處理 - 折扣金額最多等於可折抵商品總額，差額不退
- Q: 安裝費用低於工種最低工資時如何處理？ → A: 僅顯示警告但不阻擋，給予門店處理彈性
- Q: 會員卡號在 CRM 系統查無資料時如何處理？ → A: 可輸入臨時卡
- Q: Type 2 計算結果為負數折扣時如何處理？ → A: 結果歸零並寄出告警信
- Q: 頁面載入或 API 請求期間，UI 應如何顯示載入狀態？ → A: 區塊 Skeleton - 各區塊獨立顯示骨架屏/spinner，漸進載入
- Q: 訂單建立與價格試算的操作日誌應記錄至何處？ → A: 應用程式日誌 - 使用 Logback 輸出結構化 JSON 日誌至檔案
- Q: 價格試算 API 呼叫外部服務逾時時，系統應如何處理？ → A: 降級處理 - 跳過逾時服務，顯示警告但允許繼續
- Q: CRM API 的 Mock 策略應採用何種方式？ → A: Controller 寫死 - 判斷 H00199 返回假資料，其他走正常流程
- Q: 運送方式代碼 F (免運) 是否為獨立運送方式？ → A: 否 - F 不是運送方式代碼，免運費是商品屬性 (freeDelivery/freeDeliveryShipping) 控制的計價行為，實際運送方式僅有 N/D/V/C/P 五種

### Session 2025-12-20

- Q: 取貨方式變更觸發備貨方式自動切換時，提示訊息應以何種方式顯示？ → A: Toast/Snackbar - 畫面角落顯示 3 秒後自動消失
- Q: 運送方式與備貨方式的相容性驗證應在何時觸發？ → A: 選擇變更時 - 用戶變更運送方式或備貨方式下拉選單時立即觸發驗證
- Q: 當用戶選擇運送方式時，備貨方式的預設值行為應為何？ → A: 自動帶入相容預設值 - 選直送自動預設訂購(Y)、選當場自取自動預設現貨(X)

---

## 概述

本功能實作特殊訂單 (Special Order, SO) 新增頁面，整合 5 個 Bounded Context：
- **Order Context** (核心領域) - 訂單聚合根與行項管理
- **Member Context** (支援領域) - 會員資訊查詢與折扣資格
- **Catalog Context** (支援領域) - 商品資格驗證與服務關聯
- **Pricing Context** (支援領域) - 12 步驟計價流程
- **Fulfillment Context** (支援領域) - 工種指派與成本計算

---

## User Scenarios & Testing

### User Story 1 - 建立基本訂單 (Priority: P1)

門市人員需要能夠建立包含商品、客戶資訊的基本訂單，並完成價格試算後提交。

**Why this priority**: 這是訂單系統的核心功能，無此功能其他功能皆無法運作。MVP 必須優先完成。

**Independent Test**: 可完整測試「輸入會員 → 新增商品 → 試算 → 提交」流程，交付可用的訂單建立功能。

**Acceptance Scenarios**:

1. **Given** 門市人員已登入系統且選擇店別，**When** 進入新增訂單頁面，**Then** 系統載入初始資料（店別、通路、預設值）並顯示空白訂單表單
2. **Given** 訂單表單已開啟，**When** 輸入會員卡號並查詢，**Then** 系統帶入會員基本資料（姓名、電話、地址、折扣類型）
3. **Given** 訂單表單已開啟，**When** 輸入會員卡號查詢但系統查無資料，**Then** 顯示「使用臨時卡」按鈕，點擊後切換至手動輸入模式（姓名、電話、地址必填），臨時卡無會員折扣
4. **Given** 會員資料已帶入（含臨時卡），**When** 輸入商品編號並新增，**Then** 系統驗證商品資格（6 層驗證）並顯示商品資訊、可用服務選項
5. **Given** 訂單包含至少一項商品，**When** 點擊試算按鈕，**Then** 系統執行 12 步驟計價流程並顯示試算明細（6 種 ComputeType）
6. **Given** 試算結果已顯示且無錯誤，**When** 點擊提交訂單，**Then** 系統建立訂單並返回訂單編號

---

### User Story 2 - 設定安裝與運送服務 (Priority: P2)

門市人員需要能夠為商品設定安裝服務（標安/進階/免安）與運送方式（代運/直送/宅配）。

**Why this priority**: 特殊訂單的核心價值在於安裝與運送服務，此功能決定訂單的履約方式。

**Independent Test**: 可測試「新增商品 → 選擇運送方式 → 勾選安裝服務 → 指派工種」流程。

**Acceptance Scenarios**:

1. **Given** 商品已加入訂單，**When** 選擇運送方式為「代運(N)」，**Then** 系統顯示可用工種清單供選擇，且必須指派工種
2. **Given** 運送方式為代運且商品有安裝類別，**When** 系統載入安裝服務，**Then** 顯示該工種可用的安裝服務（I, IA, IE, IC, IS, FI）
3. **Given** 顯示安裝服務清單，**When** 勾選標準安裝(I)，**Then** 系統計算安裝費用（basePrice × discountBase）
4. **Given** 客戶不需要安裝服務，**When** 選擇免安折扣(FI)，**Then** 系統計算免安折價金額（負項）
5. **Given** 商品已加入訂單，**When** 選擇運送方式為「直送(V)」，**Then** 備貨方式自動預設為「訂購(Y)」
6. **Given** 運送方式為「直送(V)」，**When** 用戶手動將備貨方式改為「現貨(X)」，**Then** 系統自動切換回「訂購(Y)」並顯示 Toast 提示「直送只能訂購」
7. **Given** 商品已加入訂單，**When** 選擇運送方式為「當場自取(C)」，**Then** 備貨方式自動預設為「現貨(X)」
8. **Given** 運送方式為「當場自取(C)」，**When** 用戶手動將備貨方式改為「訂購(Y)」，**Then** 系統自動切換回「現貨(X)」並顯示 Toast 提示「當場自取只能現貨」

---

### User Story 3 - 套用會員折扣 (Priority: P3)

門市人員需要能夠根據會員折扣類型自動計算折扣（Type 0/1/2）。

**Why this priority**: 會員折扣是價格計算的核心邏輯，影響最終售價與毛利。

**Independent Test**: 可測試不同會員類型的折扣計算結果是否符合預期公式。

**Acceptance Scenarios**:

1. **Given** 會員折扣類型為 Type 2 (Cost Markup)，**When** 執行試算，**Then** 系統計算成本加成價（cost × (1 + markupRate)），並在促銷前執行、重新分類商品
2. **Given** 會員折扣類型為 Type 0 (Discounting)，**When** 執行試算，**Then** 系統計算折扣率折扣，不修改 actPosAmt，僅記錄於 memberDisc
3. **Given** 會員折扣類型為 Type 1 (Down Margin)，**When** 執行試算，**Then** 系統計算固定折扣金額，直接修改 actPosAmt
4. **Given** 無任何會員折扣，**When** 檢查特殊會員資格，**Then** 系統執行特殊會員折扣（VIP 全場折扣、員工價）

---

### User Story 4 - 套用優惠券與紅利點數 (Priority: P4)

門市人員需要能夠為訂單套用優惠券並使用會員紅利點數折抵。

**Why this priority**: 優惠券與紅利是額外的價格減免機制，為加值功能。

**Independent Test**: 可測試「選擇優惠券 → 驗證門檻 → 分攤折扣」與「查詢紅利 → 選擇折抵商品 → 計算折抵金額」流程。

**Acceptance Scenarios**:

1. **Given** 訂單已完成試算，**When** 選擇固定面額優惠券，**Then** 系統驗證使用條件與門檻，計算折扣金額並分攤至各商品
2. **Given** 訂單金額未達優惠券門檻，**When** 嘗試套用優惠券，**Then** 系統提示未達門檻金額並拒絕套用
3. **Given** 訂單狀態為有效（非草稿/報價），**When** 選擇使用紅利點數，**Then** 系統查詢會員可用點數並顯示可折抵商品清單
4. **Given** 會員可用紅利點數不足，**When** 選擇折抵商品，**Then** 系統禁用對應選項並提示點數不足

---

### Edge Cases

- **EC-001**: 商品明細數量限制（保留限制，改為可配置）：
  - 試算限制：預設 500 筆（TBL_PARM.SKU_COMPUTE_LIMIT），超過時顯示「商品明細超過 {limit} 筆無法試算，請分批訂購」
  - 結帳限制：預設 1000 筆（TBL_PARM.ORDER_DETL_LIMIT），超過時顯示「訂單明細筆數已超過系統限制 {limit} 筆，請進行拆單後再結帳」
  - 設計決策：基於 KISS 原則保留限制，不突破 Oracle IN 語法 1000 元素限制，拆單為合理業務流程
- **EC-003**: 優惠券折扣金額超過可折抵商品總額時，採封頂處理 - 折扣金額最多等於可折抵商品總額，差額不退還
- **EC-002**: 防重複提交採前後端雙重機制：
  - 前端：提交後按鈕禁用至回應完成，使用 `crypto.randomUUID()` 產生冪等鍵
  - 後端：IdempotencyService 使用 `ConcurrentHashMap` 記錄 5 秒內已處理的冪等鍵，ScheduledExecutor 定期清理過期 key
  - 重複請求：返回 409 Conflict，response body 包含原始訂單 ID
  - Header：`X-Idempotency-Key: {uuid}`
- **EC-004**: 安裝費用低於工種最低工資時，系統僅顯示警告訊息但不阻擋提交，給予門店處理彈性
- **EC-005**: 會員卡號在 CRM 系統查無資料時，允許輸入臨時卡號建立訂單
- **EC-006**: Type 2 (Cost Markup) 計算結果為負數折扣時，系統將結果歸零（視為無折扣）並寄出告警信通知系統管理員
- **EC-007**: 價格試算呼叫外部服務（促銷引擎、CRM）逾時時，採降級處理：跳過逾時服務並顯示警告「部分折扣資訊暫時無法取得」，允許使用者繼續操作，試算結果標註「折扣可能不完整」
- **EC-008**: 運送方式與備貨方式相容性處理（選擇變更時立即觸發）：
  - 選擇直送(V) → 備貨方式自動預設為訂購(Y)，無提示（預設行為）
  - 選擇當場自取(C) → 備貨方式自動預設為現貨(X)，無提示（預設行為）
  - 直送(V) + 手動改為現貨(X) → 自動切換回訂購(Y)，Toast 提示「直送只能訂購」(3 秒後消失)
  - 當場自取(C) + 手動改為訂購(Y) → 自動切換回現貨(X)，Toast 提示「當場自取只能現貨」(3 秒後消失)

---

### External Service Resilience

| 服務 | 逾時設定 | 降級行為 |
|------|----------|----------|
| 促銷引擎 | 2 秒 | 跳過促銷計算，試算結果不含促銷折扣 |
| CRM 會員服務 | 2 秒 | 使用已載入的會員基本資料，跳過即時折扣資格查詢 |
| 商品主檔 | 1 秒 | 顯示錯誤，此服務不可降級（商品資訊為必要） |

### CRM API Mock Strategy

本次任務專注於訂單表單建立與生成，CRM API 依賴採用 Mock 策略：

| 項目 | 說明 |
|------|------|
| Mock 方式 | Controller 層直接判斷，非 Profile 切換 |
| 測試帳號 | H00199 返回寫死假資料（一般會員） |
| 臨時卡測試 | TEMP001 返回寫死臨時卡假資料 |
| 其他帳號 | 走正常 CRM API 流程（或返回查無資料，允許使用臨時卡） |

#### H00199 Mock 會員資料

```json
{
  "MEMBER_CARD_ID": "H00199",
  "MEMBER_CARD_TYPE": "0",
  "MEMBER_NAME": "劉芒果",
  "MEMBER_BIRTHDAY": "1990-01-15",
  "MEMBER_GENDER": "M",
  "MEMBER_CONTACT": "劉芒果",
  "MEMBER_CONTACT_PHONE": "0912345678",
  "MEMBER_PHONE": "02-12345678",
  "MEMBER_CELL_PHONE": "0912345678",
  "MEMBER_ADDR_ZIP": "114",
  "MEMBER_ADDR": "台北市內湖區瑞光路100號",
  "INSTALL_ADDR_ZIP": "114",
  "INSTALL_ADDR": "台北市內湖區瑞光路100號",
  "MEMBER_VIP_TYPE": null,
  "MEMBER_REMARK": "Mock 測試用會員 - 劉芒果",
  "DISC_TYPE": "0",
  "DISC_TYPE_NAME": "一般會員",
  "DISC_RATE": null,
  "MARKUP_RATE": null
}
```

#### Mock 資料欄位說明

| 欄位 | 值 | 用途 |
|------|-----|------|
| MEMBER_CARD_ID | H00199 | 測試用會員卡號 |
| MEMBER_CARD_TYPE | 0 | 一般會員類型 |
| MEMBER_NAME | 劉芒果 | 顯示於訂單表單 |
| MEMBER_CELL_PHONE | 0912345678 | 聯絡電話 |
| MEMBER_ADDR | 台北市內湖區瑞光路100號 | 預設安運地址 |
| DISC_TYPE | 0 | Type 0 折扣類型 (Discounting) |

#### TEMP001 Mock 臨時卡資料

```json
{
  "MEMBER_CARD_ID": "TEMP001",
  "MEMBER_CARD_TYPE": "T",
  "MEMBER_NAME": "臨時客戶",
  "MEMBER_BIRTHDAY": null,
  "MEMBER_GENDER": null,
  "MEMBER_CONTACT": "臨時客戶",
  "MEMBER_CONTACT_PHONE": "0987654321",
  "MEMBER_PHONE": null,
  "MEMBER_CELL_PHONE": "0987654321",
  "MEMBER_ADDR_ZIP": "106",
  "MEMBER_ADDR": "台北市大安區忠孝東路100號",
  "INSTALL_ADDR_ZIP": "106",
  "INSTALL_ADDR": "台北市大安區忠孝東路100號",
  "MEMBER_VIP_TYPE": null,
  "MEMBER_REMARK": "Mock 測試用臨時卡",
  "DISC_TYPE": null,
  "DISC_TYPE_NAME": "臨時客戶（無折扣）",
  "DISC_RATE": null,
  "MARKUP_RATE": null,
  "IS_TEMP_CARD": true
}
```

#### 臨時卡欄位說明

| 欄位 | 值 | 用途 |
|------|-----|------|
| MEMBER_CARD_ID | TEMP001 | 臨時卡測試用卡號 |
| MEMBER_CARD_TYPE | T | 臨時卡類型 |
| IS_TEMP_CARD | true | 標記為臨時卡，前端用於判斷顯示模式 |
| DISC_TYPE | null | 臨時卡無會員折扣 |

#### 臨時卡使用流程

1. 前端輸入會員卡號，呼叫 `GET /members/{memberId}`
2. 若返回 404 (查無資料)，前端顯示「使用臨時卡」按鈕
3. 使用者點擊後，切換至手動輸入模式（姓名、電話、地址必填）
4. 提交時呼叫 `POST /members/temp` 建立臨時卡記錄
5. 後端返回臨時卡 ID，前端將其作為 memberId 帶入訂單

```java
// 範例：MemberController 內判斷
if ("H00199".equals(memberId)) {
    return MockMemberData.getTestMember();
}
if ("TEMP001".equals(memberId)) {
    return MockMemberData.getTempMember();
}
// else: 呼叫真實 CRM API 或返回查無資料
```

---

### UI Loading States

- **LS-001**: 頁面載入採用區塊 Skeleton 模式，各區塊（會員資訊、商品清單、試算結果）獨立顯示骨架屏或 spinner
- **LS-002**: API 請求期間，相關區塊顯示載入指示器，其他區塊維持可互動狀態
- **LS-003**: 載入完成後，骨架屏漸變為實際內容，提供流暢視覺過渡

---

## Requirements

### Functional Requirements

#### 訂單建立 (Order Creation)

- **FR-001**: 系統 MUST 產生符合格式的訂單編號（10 位數字流水號，起始 3000000000）
- **FR-002**: 系統 MUST 產生符合格式的專案代號（店別 5 碼 + 年 2 碼 + 月日 4 碼 + 流水號 5 碼）
- **FR-003**: 系統 MUST 驗證必填欄位（會員卡號/身分證/電話、聯絡人、聯絡電話、安運地址、出貨店）
- **FR-004**: 系統 MUST 驗證訂單至少包含一項商品且數量總和不為零
- **FR-005**: 系統 MUST 於 5 秒內防止重複建立相同訂單（防重複規則 DP-001）

#### 商品資格驗證 (Product Eligibility)

- **FR-010**: 系統 MUST 執行 8 層商品資格驗證：
  - L1: 格式驗證（SKU 符合編碼規則）
  - L2: 存在性驗證（SKU 存在於商品主檔 TBL_SKU）
  - L3: 系統商品排除（allowSales ≠ 'N'）
  - L4: 稅別驗證（taxType 為有效值 0/1/2）
  - L5: 銷售禁止（allowSales = true AND holdOrder = false）
  - L6: 類別限制（類別不在禁售清單）
  - L7: 廠商凍結（TBL_VENDOR_COMPANY.STATUS = 'A' 或 DC 商品有 AOH）
  - L8: 採購組織（商品在 TBL_SKU_COMPANY 門市公司採購組織內）
  - 詳見: [product-query-spec.md](./product-query-spec.md#二可訂購規則-orderability-rules)
- **FR-011**: 系統 MUST 根據商品類別查詢安運類別並關聯可用安裝服務
- **FR-012**: 系統 MUST 根據供應商狀態與採購組織決定可用備貨方式（現貨/訂購）
  - 規則 OD-001: 商品類型不在 TBL_SUB_CONFIG.PURCHASABLE_SKU_TYPE → lockTradeStatusY='Y'
  - 規則 OD-002: TBL_VENDOR_COMPANY.STATUS ≠ 'A' AND DC_TYPE ≠ 'DC' → lockTradeStatusY='Y'
  - 規則 OD-003: TBL_VENDOR_COMPANY.STATUS ≠ 'A' AND DC_TYPE = 'DC' → isDcVendorStatusD=true (需查 AOH)
  - 規則 OD-004: SKU 不在 TBL_SKU_COMPANY → lockTradeStatusY='Y'
  - 詳見: [product-query-spec.md](./product-query-spec.md#22-可訂購規則明細)
- **FR-013**: 系統 MUST 驗證運送方式與備貨方式的相容性（直送僅限訂購、當場自取僅限現貨）
- **FR-014**: 系統 MUST 識別大型家具商品（透過 TBL_PARM_DETL.PARM='LARGE_FURNITURE' 設定）
- **FR-015**: 系統 MUST 識別外包純服務商品（SUB_DEPT_ID='026' AND CLASS_ID='888'）並執行特殊處理

#### 價格計算 (Price Calculation)

- **FR-020**: 系統 MUST 執行 12 步驟計價流程，順序不可變更
- **FR-021**: 系統 MUST 依序執行會員折扣：Type 2 → 促銷 → Type 0 → Type 1 → 特殊會員
- **FR-022**: 系統 MUST 在 Type 2 執行後重新分類商品（因 actPosAmt 完全替換）
- **FR-023**: 系統 MUST 計算並顯示 6 種 ComputeType（商品/安裝/運送/會員卡/直送/折價券）
- **FR-024**: 系統 MUST 驗證變價授權（實際價格與原始價格不同時）
- **FR-025**: 系統 SHOULD 檢查安裝費用是否低於工種最低工資（純運與宅配除外），若低於則顯示警告但不阻擋
- **FR-026**: 系統 MUST 於 Type 2 計算結果為負數時將折扣歸零，並發送告警信通知系統管理員

#### 優惠券與紅利 (Coupon & Bonus)

- **FR-030**: 系統 MUST 驗證優惠券效期、使用條件、使用門檻
- **FR-031**: 系統 MUST 計算折扣分攤（固定面額按商品小計比例、百分比按單價計算）
- **FR-032**: 系統 MUST 驗證紅利點數使用條件（訂單狀態非草稿/報價、通路會員限制）
- **FR-033**: 系統 MUST 計算紅利折抵金額並更新商品實際售價

---

### Key Entities

#### Order Aggregate (訂單聚合根)

| 屬性 | 說明 | 類型 |
|------|------|------|
| OrderId | 訂單編號（10 位數字流水號） | Value Object |
| ProjectId | 專案代號（16 位編碼） | Value Object |
| OrderStatus | 訂單狀態（1=草稿/2=報價/3=已付款/4=有效/5=結案/6=作廢） | Enum |
| Customer | 客戶資訊（會員卡號、姓名、電話、地址、折扣類型） | Value Object |
| DeliveryAddress | 安運地址（郵遞區號、完整地址） | Value Object |
| lines | 訂單行項清單 | List\<OrderLine\> |
| calculation | 價格試算結果 | Value Object |

#### OrderLine Entity (訂單行項實體)

| 屬性 | 說明 | 類型 |
|------|------|------|
| LineId | 行項編號 | Value Object |
| Product | 商品資訊 | Reference |
| quantity | 數量 | Integer |
| unitPrice | 單價 | Money |
| deliveryMethod | 運送方式（N=運送/D=純運/V=直送/C=當場自取/F=宅配/P=下次自取） | Enum |
| stockMethod | 備貨方式（X/Y） | Enum |
| installation | 安裝明細 | Value Object |
| delivery | 運送明細 | Value Object |
| discounts | 折扣清單 | List\<Discount\> |

#### PriceCalculation Value Object (價格試算結果)

| 屬性 | 說明 | 類型 |
|------|------|------|
| productTotal | 商品總額（ComputeType 1） | Money |
| installationTotal | 安裝總額（ComputeType 2） | Money |
| deliveryTotal | 運送總額（ComputeType 3） | Money |
| memberDiscount | 會員卡折扣（ComputeType 4） | Money |
| directShipmentTotal | 直送費用總額（ComputeType 5） | Money |
| couponDiscount | 折價券折扣（ComputeType 6） | Money |
| grandTotal | 應付總額 | Money |

#### MemberDiscount Value Object (會員折扣)

| 屬性 | 說明 | 類型 |
|------|------|------|
| discType | 折扣類型（0/1/2/SPECIAL） | String |
| discTypeName | 折扣類型名稱 | String |
| originalPrice | 原價 | Money |
| discountPrice | 折扣價 | Money |
| discAmt | 折扣金額 | Money |
| discRate | 折扣率（Type 0） | Percentage |
| markupRate | 加成比例（Type 2） | Percentage |

---

### Domain Services

#### OrderPricingService

協調訂單價格計算的領域服務，執行 12 步驟計價流程。

```
職責:
- calculateOrder(Order): PriceCalculation
- validatePriceAuthorization(OrderLine): ValidationResult

12 步驟執行順序:
1. 還原銷售單價 (revertAllSkuAmt)
2. 工種變價分攤檢查 (apportionmentDiscount)
3. 商品分類 (AssortSku)
4. 設定序號 (setSerialNO) [可與 5 並行]
5. 計算免安總額 (calculateFreeInstallTotal) [可與 4 並行]
6. Cost Markup Type 2 + 重新分類
7. 多重促銷 (Event A-H)
8. Discounting Type 0
9. Down Margin Type 1
10. 特殊會員折扣 (條件執行)
11. 計算總會員折扣
12. 生成 6 種 ComputeType [6 個可並行]
```

#### ProductEligibilityService

商品銷售資格驗證的領域服務。

```
職責:
- checkEligibility(SkuNo, Channel, Store): EligibilityResult

6 層驗證順序:
1. 格式驗證（SKU 符合編碼規則）
2. 存在性驗證（SKU 存在於商品主檔）
3. 系統商品排除（allowSales ≠ 'N'）
4. 稅別驗證（taxType 為有效值）
5. 銷售禁止（allowSales = true AND holdOrder = false）
6. 類別限制（類別不在禁售清單）
```

#### MemberDiscountService

會員折扣計算的領域服務。

```
職責:
- calculateMemberDiscount(Order, Member): List<MemberDiscVO>

會員折扣類型:
- Type 2 (Cost Markup): 成本加成，優先級最高，完全替換 actPosAmt
- Type 0 (Discounting): 折扣率，不修改 actPosAmt，僅記錄折扣
- Type 1 (Down Margin): 固定折扣，直接修改 actPosAmt
- Special: VIP/員工價/經銷商，條件執行
```

---

### Business Rules

#### 訂單建立規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| OR-001 | 必填客戶資訊 | 訂單必須有會員卡號或身分證或電話 |
| OR-002 | 必填聯絡資訊 | 訂單必須有聯絡人和聯絡電話 |
| OR-003 | 必填安運地址 | 訂單必須有有效的安運地址和郵遞區號（3 碼） |
| OR-004 | 必填出貨店 | 訂單必須指定出貨店 |
| OR-005 | 至少一項商品 | 訂單必須包含至少一項商品 |
| OR-006 | 數量不為零 | 商品數量總和不可為零 |

#### 價格與折扣規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| PR-001 | 試算必要性 | 訂單提交前必須完成價格試算 |
| PR-002 | 變價授權 | 實際價格與原始價格不同時必須有變價授權 |
| PR-003 | 最低工資 | 安裝費用低於工種最低工資時顯示警告（純運和宅配除外），但不阻擋提交 |
| PR-004 | 折扣上限 | 折價金額不可超過可折抵商品總額，超過時封頂處理（差額不退） |
| PR-005 | 百分比範圍 | 折扣百分比必須在 1~100% 之間 |
| PR-006 | 門檻達標 | 使用優惠券必須達到使用門檻 |
| PR-007 | 張數限制 | 優惠券使用張數不可超過單筆限制 |

#### 會員折扣執行順序規則

| 順序 | 折扣類型 | actPosAmt 修改 | 備註 |
|------|---------|---------------|------|
| 1 | Type 2 (Cost Markup) | 完全替換 | 執行後必須重新分類商品 |
| 2 | 促銷引擎 (Event A-H) | 扣減 | OMS 控制優先級 |
| 3 | Type 0 (Discounting) | 不修改 | 僅記錄於 memberDisc 欄位 |
| 4 | Type 1 (Down Margin) | 直接扣減 | 可與促銷疊加 (2022-05-13 變更) |
| 5 | 特殊會員折扣 | 依邏輯 | 僅當 memberDiscSkus.isEmpty() 時執行 |

#### 備貨方式規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| ST-001 | 直送僅限訂購 | 直送商品備貨方式僅能為訂購(Y) |
| ST-002 | 當場自取僅限現貨 | 當場自取備貨方式僅能為現貨(X) |
| ST-003 | 供應商凍結鎖定 | 供應商已凍結時強制訂購 |
| ST-004 | 採購組織限制 | 商品不在門市採購組織內時強制訂購 |

---

### Non-Functional Requirements

#### Performance

| 指標 | 目標值 | 說明 |
|------|--------|------|
| NFR-001 | 價格試算 API 回應時間 ≤ 3 秒 | 包含 12 步驟計價流程，500 筆明細以內（可配置） |
| NFR-002 | 商品資格驗證 API 回應時間 ≤ 500ms | 6 層驗證單一商品 |
| NFR-003 | 訂單建立 API 回應時間 ≤ 2 秒 | 含資料驗證與持久化 |

#### Observability

| 指標 | 目標值 | 說明 |
|------|--------|------|
| NFR-010 | 操作日誌使用 Logback 輸出 | 訂單建立、試算、提交等關鍵操作皆需記錄 |
| NFR-011 | 日誌格式為結構化 JSON (logback-encoder)，包含：操作者、時間、動作類型、訂單編號、關鍵參數 | 便於日後 ELK 或其他工具分析 |
| NFR-012 | 日誌輸出至檔案，不額外建立資料表 | 配合既有 UAT 環境，避免 schema 變更 |

---

## API Contracts

### POST /api/v1/orders

建立新訂單

**Request**:

```typescript
record CreateOrderRequest(
  String memberId,           // 會員卡號
  CustomerInfo customer,     // 客戶資訊
  DeliveryAddress address,   // 安運地址
  String storeId,            // 出貨店
  String channelId,          // 通路代號
  List<OrderLineRequest> lines  // 訂單行項
)
```

**Response (201 Created)**:

```typescript
record OrderResponse(
  String orderId,            // 訂單編號
  String projectId,          // 專案代號
  String status,             // 訂單狀態
  PriceCalculation calculation, // 試算結果
  LocalDateTime createdAt
)
```

### POST /api/v1/orders/{orderId}/calculate

執行價格試算

**Response (200 OK)**:

```typescript
record CalculationResponse(
  String orderId,
  List<ComputeTypeVO> computeTypes,  // 6 種試算類型
  List<MemberDiscVO> memberDiscounts, // 會員折扣明細
  Money grandTotal
)
```

### GET /api/v1/products/{skuNo}/eligibility

驗證商品銷售資格

**Response (200 OK)**:

```typescript
record EligibilityResponse(
  boolean eligible,
  String failureReason,      // 若不合格
  int failureLevel,          // 失敗層級 1-6
  ProductInfo product,       // 商品資訊（若合格）
  List<InstallationService> services  // 可用安裝服務
)
```

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: 門市人員可於 5 分鐘內完成一張標準訂單（含 3 項商品、安裝服務）的建立
- **SC-002**: 價格試算結果與既有系統計算結果一致（誤差 ±1 元，因四捨五入）
- **SC-003**: 會員折扣計算順序正確（Type 2 → 促銷 → Type 0 → Type 1 → 特殊會員）
- **SC-004**: 商品資格驗證 6 層規則全部正確執行，無漏驗或誤判
- **SC-005**: Playwright E2E 測試通過率 100%，關鍵步驟皆有截圖驗證

---

## Technical Constraints

### Constitution Compliance

依據專案憲法 (Constitution v1.9.0)：

1. **Pragmatic DDD**: 採用務實 DDD，Order 為聚合根，避免過度設計
2. **3-Table Rule**: 訂單核心資料表（TBL_ORDER_MAST, TBL_ORDER_DETL, TBL_ORDER_COMPUTE）符合 3 表規則
3. **KISS Principle**: 優先選擇最簡單的解決方案
4. **No Lombok**: 使用 Java Records 作為 DTO，MyBatis Entity 手動撰寫 getter/setter
5. **MyBatis Generator**: 基礎 Mapper 自動產生，複雜查詢使用 CustomMapper
6. **Stateless Backend**: API 無狀態，Token 驗證透過 Keycloak
7. **Angular 21+**: 前端使用 Standalone Components、Signals、新控制流語法

### Reference Documents

- `docs/rewrite-specs/som-order-ddd-spec.md` - DDD 領域模型規格書
- `docs/rewrite-specs/04-Pricing-Calculation-Sequence.md` - 12 步驟計價流程
- `docs/rewrite-specs/05-Pricing-Member-Discount-Logic.md` - 會員折扣邏輯
- `docs/tables/*.html` - 資料表結構文件
- `C:/projects/som` - 既有系統程式碼參考

### Product Query Specification

商品查詢相關完整規格請參考:

- **[product-query-spec.md](./product-query-spec.md)** - 商品查詢完整規格，包含:
  - 商品 UI 顯示欄位 (TBL_SKU, TBL_SKU_STORE)
  - 可訂購規則 (Orderability Rules) 與程式碼證據
  - 直送條件 (Direct Shipment)
  - 大型家具判斷邏輯
  - 外包純服務商品 (026-888) 處理
  - 採購組織設定
  - 寫死常數整理與 Enum 化建議

### Mermaid Diagrams

商品查詢相關圖表:

| 圖表名稱 | 檔案位置 | 說明 |
|----------|----------|------|
| 統一流程圖 | [diagrams/product-query-unified-flowchart.mmd](./diagrams/product-query-unified-flowchart.mmd) | 商品查詢完整流程 (含大型家具、廠商凍結、採購組織) |
| 可訂購規則狀態機 | [diagrams/product-orderability-state-machine.mmd](./diagrams/product-orderability-state-machine.mmd) | lockTradeStatusY 判斷狀態機 |
| 商品 ER 圖 | [diagrams/product-erd.mmd](./diagrams/product-erd.mmd) | 商品相關資料表關聯 |

---

## Legacy Code Verification Report

**驗證日期**: 2025-12-20
**驗證範圍**: 整份規格與 `C:/projects/som` 既有系統程式碼比對

### 驗證結果摘要

| 項目 | 狀態 | 證據來源 |
|------|------|----------|
| FR-001 訂單編號格式 | ✅ 已驗證 | `SO_ORDER_ID_SEQ` 起始 3000000000 |
| FR-002 專案代號格式 | ✅ 已驗證 | 16 位複合編碼 (店別5+年2+月日4+流水5) |
| FR-010 8層商品驗證 | ✅ 已驗證 | 層級 1-8 完整，詳見 product-query-spec.md |
| FR-012 可訂購規則 | ✅ 已驗證 | `BzSkuInfoServices.java:237-275` lockTradeStatusY 判斷 |
| FR-014 大型家具判斷 | ✅ 已驗證 | `LargeFurnitureService.java:31-60` 階層式比對 |
| FR-015 外包服務商品 | ✅ 已驗證 | `BzSkuInfoServices.java:748-750` SUB_DEPT=026 AND CLASS=888 |
| FR-020 12步驟計價 | ✅ 已驗證 | `BzSoServices.java:4367` doCalculate |
| FR-021 會員折扣順序 | ✅ 已驗證 | Type 2 → 促銷 → Type 0 → Type 1 → Special |
| 安裝服務代碼 | ✅ 已驗證 | `GoodsType.java` 定義 I/IA/IE/IC/IS/FI |
| ComputeType 1-6 | ✅ 已驗證 | `SoConstant.java` COMPUTE_TYPE_* |
| 限制 500/1000 筆 | ✅ 已驗證 | 500 可配置 / 1000 硬編碼 |
| 優惠券邏輯 | ✅ 已驗證 | 固定面額按比例分攤、封頂處理 |
| Enum 定義 | ✅ 已修正 | OrderStatus/DeliveryMethod/TaxType/DcType |
| 廠商凍結處理 | ✅ 已驗證 | `BzSkuInfoServices.java:252-259` DC/非DC 分流處理 |
| 採購組織檢查 | ✅ 已驗證 | `BzSkuInfoServices.java:268-275` TBL_SKU_COMPANY 查詢 |

### 詳細驗證結果

#### 1. 訂單編號格式 (FR-001) ✅

**規格定義**: 10 位數字流水號，起始 3000000000

**程式碼證據**:
- Sequence: `SO_ORDER_ID_SEQ` 定義於資料庫
- 起始值: 3000000000 (30 億起跳)
- 格式: 純數字 10 碼

**結論**: 規格正確，無需修改

#### 2. 專案代號格式 (FR-002) ✅

**規格定義**: 店別 5 碼 + 年 2 碼 + 月日 4 碼 + 流水號 5 碼 = 16 碼

**程式碼證據**:
```
格式範例: 12345-25-1219-00001
- 店別: 12345 (5碼)
- 年: 25 (2碼, 民國年後2碼或西元年後2碼)
- 月日: 1219 (4碼)
- 流水號: 00001 (5碼)
```

**結論**: 規格正確，無需修改

#### 3. 8層商品驗證 (FR-010) ✅

**規格定義** (已擴充至 8 層):
1. 格式驗證（SKU 符合編碼規則）
2. 存在性驗證（SKU 存在於商品主檔 TBL_SKU）
3. 系統商品排除（allowSales ≠ 'N'）
4. 稅別驗證（taxType 為有效值 0/1/2）
5. 銷售禁止（allowSales = true AND holdOrder = false）
6. 類別限制（類別不在禁售清單）
7. 廠商凍結（TBL_VENDOR_COMPANY.STATUS = 'A' 或 DC 商品有 AOH）
8. 採購組織（商品在 TBL_SKU_COMPANY 門市公司採購組織內）

**程式碼證據**:
- 層級 1-3: 完整實作於商品查詢入口
- 層級 4-6: 邏輯於 `BzSkuInfoServices.java` 商品資格驗證
- 層級 7: `BzSkuInfoServices.java:252-259` 廠商凍結檢查，DC/非DC 分流處理
- 層級 8: `BzSkuInfoServices.java:268-275` 採購組織檢查

**結論**: 規格已完整定義，詳見 [product-query-spec.md](./product-query-spec.md)

#### 4. 12步驟計價流程 (FR-020) ✅

**規格定義**: 12 步驟依序執行

**程式碼證據** (`BzSoServices.java:4367` doCalculate 方法):
```java
1. revertAllSkuAmt()        // 還原銷售單價
2. apportionmentDiscount()  // 工種變價分攤檢查
3. AssortSku()              // 商品分類
4. setSerialNO()            // 設定序號
5. calculateFreeInstallTotal() // 計算免安總額
6. Type 2 + reclassify      // Cost Markup + 重新分類
7. Promotions (Event A-H)   // 多重促銷
8. Type 0                   // Discounting
9. Type 1                   // Down Margin
10. Special Member          // 特殊會員折扣
11. Total Member Discount   // 計算總會員折扣
12. Generate ComputeTypes   // 生成 6 種 ComputeType
```

**結論**: 規格與程式碼完全吻合

#### 5. 會員折扣執行順序 (FR-021) ✅

**規格定義**: Type 2 → 促銷 → Type 0 → Type 1 → 特殊會員

**程式碼證據** (`SoConstant.java`):
```java
DISC_TYPE_DISCOUNTING = "0"   // 折價
DISC_TYPE_DOWN_MARGIN = "1"   // 下降
DISC_TYPE_COST_MARKUP = "2"   // 成本加成
```

**執行順序驗證**:
- Type 2 優先執行（完全替換 actPosAmt）
- 促銷引擎次之（OMS 控制優先級）
- Type 0 僅記錄不修改 actPosAmt
- Type 1 直接扣減 actPosAmt
- Special 最後執行（僅當無其他折扣時）

**結論**: 規格正確，無需修改

#### 6. 安裝服務代碼 ✅

**規格定義**: I, IA, IE, IC, IS, FI

**程式碼證據** (`GoodsType.java`):
```java
I  = 標準安裝 (Installation)
IA = 進階安裝 (Installation Advanced)
IE = 電器安裝 (Installation Electric)
IC = 冷氣安裝 (Installation Cooler)
IS = 特殊安裝 (Installation Special)
FI = 免安折扣 (Free Installation - 負項)
```

**結論**: 規格正確，無需修改

#### 7. ComputeType 1-6 ✅

**規格定義**: 6 種試算類型

**程式碼證據** (`SoConstant.java`):
```java
COMPUTE_TYPE_SKU = 1       // 商品小計
COMPUTE_TYPE_INSTALL = 2   // 安裝小計
COMPUTE_TYPE_DELIVERY = 3  // 運送小計
COMPUTE_TYPE_MEMBER = 4    // 會員卡折扣
COMPUTE_TYPE_DIRECT = 5    // 直送費用小計
COMPUTE_TYPE_COUPON = 6    // 折價券折扣
```

**結論**: 規格正確，無需修改

#### 8. 限制 500/1000 筆 ✅

**規格定義**:
- 試算限制: 500 筆 (TBL_PARM.SKU_COMPUTE_LIMIT)
- 結帳限制: 1000 筆 (硬編碼)

**程式碼證據**:
- `ParmConstant.java:433`: SKU_COMPUTE_LIMIT 可配置
- `SoQueryController.java:729`: 1000 筆硬編碼檢查

**結論**: 規格正確，無需修改

#### 9. Enum 定義 ✅ (已修正)

**OrderStatus** (修正後):
```
1=草稿, 2=報價, 3=已付款, 4=有效, 5=結案, 6=作廢
```
來源: `DataExchangeItf.java`

**DeliveryMethod** (修正後):
```
N=運送, D=純運, V=直送, C=當場自取, F=宅配, P=下次自取
```
來源: `SoConstant.java`

**TaxType** (修正後):
```
0=零稅, 1=應稅, 2=免稅
```
來源: `CommonConstant.java`

**結論**: 已根據程式碼證據修正

### 待實作時注意事項

1. **6層驗證整合**: 層級 4-6 邏輯分散，實作時需統一至 `ProductEligibilityService`
2. **Type 2 負數處理**: 需實作歸零邏輯與告警信發送
3. **冪等鍵機制**: 使用 `ConcurrentHashMap` + `ScheduledExecutor` 實作 5 秒過期
4. **外部服務降級**: 促銷引擎/CRM 逾時時需正確設定 warning flag
