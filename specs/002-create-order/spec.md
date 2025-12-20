# Feature Specification: 新增訂單頁面

**Feature Branch**: `002-create-order`
**Created**: 2025-12-19
**Last Updated**: 2025-12-20
**Status**: Complete
**Version**: 2.0.0
**Input**: 基於 DDD 領域模型規格書與既有系統逆向工程文件，建立新增訂單功能規格

---

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
- Q: CRM API 的 Mock 策略應採用何種方式？ → A: Controller 寫死 - 判斷 K00123 返回假資料，其他走正常流程

### Session 2025-12-20

- Q: 取貨方式變更觸發備貨方式自動切換時，提示訊息應以何種方式顯示？ → A: Toast/Snackbar - 畫面角落顯示 3 秒後自動消失
- Q: 運送方式與備貨方式的相容性驗證應在何時觸發？ → A: 選擇變更時 - 用戶變更運送方式或備貨方式下拉選單時立即觸發驗證
- Q: 當用戶選擇運送方式時，備貨方式的預設值行為應為何？ → A: 自動帶入相容預設值 - 選直送自動預設訂購(Y)、選當場自取自動預設現貨(X)
- Q: OMS 促銷過期風險窗口處理？ → A: 靜默忽略，按原價計算，記錄 Warning Log
- Q: 多張折價券計算順序？ → A: 依加入順序 (FIFO)
- Q: 工種變價餘數分攤算法？ → A: 按 detlSeq 逐一分攤 $1
- Q: 零元商品處理？ → A: 打印 INFO Log 便於追蹤
- Q: CRM 長時間不可用？ → A: 三層降級：過期快取 → 臨時卡
- Q: Mock 會員卡號為何？ → A: K00123（變更自 H00199），用於測試 Type 0 折扣流程

### 澄清：運送方式代碼

**DeliveryMethod 共 6 種有效值**（來源：SoConstant.java:93-113）：

| 代碼 | 常數名稱 | 中文 | 費用 | 安裝 | 備貨限制 |
|:----:|----------|------|:----:|:----:|----------|
| N | DELIVERY_FLAG_N | 運送 | 有 | 有 | 無 |
| D | DELIVERY_FLAG_D | 純運 | 有 | 無 | 無 |
| V | DELIVERY_FLAG_V | 直送 | 有 | 無 | 僅限訂購(Y) |
| C | DELIVERY_FLAG_C | 當場自取 | 無 | 無 | 僅限現貨(X) |
| F | DELIVERY_FLAG_F | 宅配 | 有 | 無 | 無 |
| P | DELIVERY_FLAG_P | 下次自取 | 無 | 無 | 無 |

> **Note**: F 為「宅配」，使用工種代碼 0167，非「免運費」。免運費是商品屬性 (freeDelivery/freeDeliveryShipping) 控制的計價行為。

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
4. **Given** 會員資料已帶入（含臨時卡），**When** 輸入商品編號並新增，**Then** 系統驗證商品資格（8 層驗證）並顯示商品資訊、可用服務選項
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
3. **Given** 顯示安裝服務清單，**When** 勾選標準安裝(I)，**Then** 系統計算安裝費用（basePrice × DISCOUNT_BASE）
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
- **EC-002**: 防重複提交採前後端雙重機制：
  - 前端：提交後按鈕禁用至回應完成，使用 `crypto.randomUUID()` 產生冪等鍵
  - 後端：IdempotencyService 使用 `ConcurrentHashMap` 記錄 5 秒內已處理的冪等鍵，ScheduledExecutor 定期清理過期 key
  - 重複請求：返回 409 Conflict，response body 包含原始訂單 ID
  - Header：`X-Idempotency-Key: {uuid}`
- **EC-003**: 優惠券折扣金額超過可折抵商品總額時，採封頂處理 - 折扣金額最多等於可折抵商品總額，差額不退還
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
| 促銷引擎 | 2 秒 | 靜默忽略，按原價計算，記錄 Warning Log |
| CRM 會員服務 | 2 秒 | 三層降級：過期快取 → 顯示臨時卡選項 |
| 商品主檔 | 1 秒 | 顯示錯誤，此服務不可降級（商品資訊為必要） |

### CRM API Mock Strategy

本次任務專注於訂單表單建立與生成，CRM API 依賴採用 Mock 策略：

| 項目 | 說明 |
|------|------|
| Mock 方式 | Controller 層直接判斷，非 Profile 切換 |
| 測試帳號 | K00123 返回寫死假資料（一般會員） |
| 臨時卡測試 | TEMP001 返回寫死臨時卡假資料 |
| 其他帳號 | 走正常 CRM API 流程（或返回查無資料，允許使用臨時卡） |

---

## Requirements

### Functional Requirements

#### 訂單建立 (Order Creation)

- **FR-001**: 系統 MUST 產生符合格式的訂單編號（10 位數字流水號，起始 3000000000）
- **FR-002**: 系統 MUST 產生符合格式的專案代號（店別 5 碼 + 年 2 碼 + 月日 4 碼 + 流水號 5 碼）
- **FR-003**: 系統 MUST 驗證必填欄位（會員卡號/身分證/電話、聯絡人、聯絡電話、安運地址、出貨店）
- **FR-004**: 系統 MUST 驗證訂單至少包含一項商品且數量總和不為零
- **FR-005**: 系統 MUST 於 5 秒內防止重複建立相同訂單（防重複規則 DP-001）

#### 商品資格驗證 (Product Eligibility) - 8 層驗證

- **FR-010**: 系統 MUST 執行 8 層商品資格驗證：

| 層級 | 名稱 | 驗證條件 | 資料表 | 程式碼參考 |
|:----:|------|----------|--------|-----------|
| L1 | 格式驗證 | SKU 符合編碼規則 | - | - |
| L2 | 存在性驗證 | SKU 存在於商品主檔 | TBL_SKU | BzSkuInfoServices:202 |
| L3 | 系統商品排除 | allowSales ≠ 'N' | TBL_SKU_STORE | BzSkuInfoServices:237 |
| L4 | 稅別驗證 | taxType 為有效值 (0/1/2) | TBL_SKU | - |
| L5 | 銷售禁止 | allowSales = true AND holdOrder = false | TBL_SKU_STORE | - |
| L6 | 類別限制 | 類別不在禁售清單 | TBL_SUB_CONFIG | BzSkuInfoServices:238-244 |
| L7 | 廠商凍結 | STATUS = 'A' 或 (DC 商品有 AOH) | TBL_VENDOR_COMPANY | BzSkuInfoServices:252-259 |
| L8 | 採購組織 | 商品在門市公司採購組織內 | TBL_SKU_COMPANY | BzSkuInfoServices:268-275 |

#### 可訂購規則 (Orderability Rules)

- **FR-011**: 系統 MUST 根據商品類別查詢安運類別並關聯可用安裝服務
- **FR-012**: 系統 MUST 執行可訂購規則 (OD-001~020)：

**後端規則** (BzSkuInfoServices.java):

| 規則編號 | 規則名稱 | 判斷條件 | 結果 | 行號 |
|---------|---------|---------|------|------|
| OD-001 | 商品類型限制 | SKU_TYPE 不在 TBL_SUB_CONFIG.PURCHASABLE_SKU_TYPE | lockTradeStatusY='Y' | 238-244 |
| OD-002 | 廠商凍結(非DC) | TBL_VENDOR_COMPANY.STATUS ≠ 'A' AND DC_TYPE ≠ 'DC' | lockTradeStatusY='Y' | 252-259 |
| OD-003 | 廠商凍結(DC) | TBL_VENDOR_COMPANY.STATUS ≠ 'A' AND DC_TYPE = 'DC' | isDcVendorStatusD=true | 254-255 |
| OD-004 | 採購組織限制 | SKU 不在門市公司的 TBL_SKU_COMPANY | lockTradeStatusY='Y' | 268-275 |
| OD-005 | 無廠商ID | VENDOR_ID 為空 | 顯示錯誤訊息 | 261-265 |

**前端規則** (soSKUSubPage.jsp):

| 規則編號 | 規則名稱 | 判斷條件 | 結果 |
|---------|---------|---------|------|
| OD-006 | 票券商品限制 | 訂單來源='0002' 且商品非 068-011 | 拒絕新增 |
| OD-007 | 非票券訂單排除 | 訂單來源≠'0002' 且商品為 068-011 | 拒絕新增 |
| OD-008 | MINI通路限制 | channelId = 'MINI' | 禁用訂購選項 |
| OD-009 | CASA通路限制 | channelId = 'CASA' | 禁用訂購選項 |
| OD-010 | DIEN商品限制 | SKU_TYPE = 'DIEN' | 強制現貨(X)+下次自取(P) |
| OD-011 | 變價條碼唯一性 | QRCode 已存在於當前訂單 | 拒絕新增 |
| OD-012 | DC廠商凍結-大型家具 | isDcVendorStatusD=true AND largeFurniture=true | 允許訂購 |
| OD-013 | DC廠商凍結-庫存足 | isDcVendorStatusD=true AND AOH >= quantity | 允許訂購 |
| OD-014 | DC廠商凍結-庫存不足 | isDcVendorStatusD=true AND AOH < quantity | 強制現貨 |
| OD-015 | 主配檔檢查 | masterConfigId ≠ 'Y' | 禁用訂購選項 |
| OD-016 | 宅配重量限制 | weight = 0 | 禁用宅配選項 |
| OD-017 | 宅配店別限制 | 出貨店不支援宅配 | 禁用宅配選項 |
| OD-018 | 免安效期檢查 | 免安商品已過期 | 從可選清單移除 |
| OD-019 | 免安售價驗證 | eventAmt > installPosAmt | 移除該免安商品 |
| OD-020 | 工種查詢失敗 | 找不到對應工種 | 設為無安裝 |

- **FR-013**: 系統 MUST 驗證運送方式與備貨方式的相容性（直送僅限訂購、當場自取僅限現貨）
- **FR-014**: 系統 MUST 識別大型家具商品（透過 TBL_PARM_DETL.PARM='LARGE_FURNITURE' 設定）
- **FR-015**: 系統 MUST 識別外包純服務商品（SUB_DEPT_ID='026' AND CLASS_ID='888'）並執行特殊處理

#### 價格計算 (Price Calculation)

- **FR-020**: 系統 MUST 執行 12 步驟計價流程（順序不可變更）：

```text
doCalculate(Order)
  +-- Step 1:  revertAllSkuAmt()          還原銷售單價
  +-- Step 2:  apportionmentDiscount()    工種變價分攤
  +-- Step 3:  assortSku()                商品分類 (P/I/FI/DD/VD/D)
  +-- Step 4:  memberDiscountType2()      Type 2 Cost Markup
  +-- Step 5:  promotionCalculation()     多重促銷 (Event A-H)
  +-- Step 6:  memberDiscountType0()      Type 0 Discounting
  +-- Step 7:  memberDiscountType1()      Type 1 Down Margin
  +-- Step 8:  specialMemberDiscount()    特殊會員折扣
  +-- Step 9:  generateComputeType1()     商品小計
  +-- Step 10: generateComputeType2()     安裝小計
  +-- Step 11: generateComputeType3()     運送小計
  +-- Step 12: generateComputeType456()   會員折扣/直送/折價券
```

- **FR-021**: 系統 MUST 依序執行會員折扣：Type 2 → 促銷 → Type 0 → Type 1 → 特殊會員
- **FR-022**: 系統 MUST 在 Type 2 執行後重新分類商品（因 actPosAmt 完全替換）
- **FR-023**: 系統 MUST 計算並顯示 6 種 ComputeType
- **FR-024**: 系統 MUST 驗證變價授權（實際價格與原始價格不同時）
- **FR-025**: 系統 SHOULD 檢查安裝費用是否低於工種最低工資（純運與宅配除外），若低於則顯示警告但不阻擋
- **FR-026**: 系統 MUST 於 Type 2 計算結果為負數時將折扣歸零，並發送告警信通知系統管理員

#### 優惠券與紅利 (Coupon & Bonus)

- **FR-030**: 系統 MUST 驗證優惠券效期、使用條件、使用門檻
- **FR-031**: 系統 MUST 計算折扣分攤（固定面額按商品小計比例、百分比按單價計算），多張優惠券依加入順序 (FIFO) 計算
- **FR-032**: 系統 MUST 驗證紅利點數使用條件（訂單狀態非草稿/報價、通路會員限制）
- **FR-033**: 系統 MUST 計算紅利折抵金額並更新商品實際售價

---

## Key Constants & Enums

### GoodsType (商品類型)

| 代碼 | 說明 | 是否商品 | 分類 |
|:----:|------|:--------:|------|
| P | 主要商品 (Primary) | 是 | lstGoodsSku |
| I | 標準安裝 (Installation) | 是 | lstInstallSku |
| IA | 進階安裝 (Installation Advanced) | 是 | lstInstallSku |
| IE | 其他安裝 (Installation Extra) | 是 | lstInstallSku |
| IC | 安裝調整 (Installation Adjust) | 是 | lstInstallSku |
| IS | 補安裝費 (Installation Supplement) | 是 | lstInstallSku |
| FI | 免安折扣 (Free Installation) | 負項 | lstFreeInstallSku |
| D | 工種 (Work Type) | 否 | lstWorkTypeSku |
| DD | 運費商品 (Delivery) | 是 | lstDeliverSku |
| VD | 直送運送商品 (Vendor Delivery) | 是 | lstDirectShipmentSku |
| VT | 會員卡折扣 | 負項 | - |
| CP | 折價券 | 負項 | - |
| CK | 折扣券 | 負項 | - |
| CI | 酷卡折扣 | 負項 | - |
| BP | 紅利折抵 | 負項 | - |
| TT | 總額折扣 | 負項 | - |

### ComputeType (試算類型)

| Type | 名稱 | 計算來源 |
|:----:|------|----------|
| 1 | 商品小計 | SUM(lstGoodsSku.actPosAmt × quantity) |
| 2 | 安裝小計 | SUM(lstInstallSku.actInstallPrice) |
| 3 | 運送小計 | SUM(lstDeliverSku.actDeliveryPrice) |
| 4 | 會員卡折扣 | SUM(all.memberDisc) (負數) |
| 5 | 直送費用小計 | SUM(lstDirectShipmentSku.actDeliveryPrice) |
| 6 | 折價券折扣 | SUM(all.couponDisc) (負數) |

### MemberDiscountType (會員折扣類型)

| Type | 名稱 | actPosAmt 修改 | 執行順序 |
|:----:|------|:-------------:|:--------:|
| 2 | Cost Markup | 完全替換 | 1 (最先) |
| 0 | Discounting | 不修改 | 3 |
| 1 | Down Margin | 直接扣減 | 4 |
| Special | VIP/員工價 | 依邏輯 | 5 (最後) |

### TaxType (稅別)

| Type | 說明 | 稅率 |
|:----:|------|:----:|
| 0 | 零稅 | 0% |
| 1 | 應稅 | 5% |
| 2 | 免稅 | 0% |

### DC_TYPE (採購屬性)

| 代碼 | 英文 | 中文 | 廠商凍結處理 |
|:----:|------|------|-------------|
| XD | Cross Docking | 交叉轉運 | 直接鎖定不可訂購 |
| DC | Stock Holding | 庫存持有 | 設定 isDcVendorStatusD，需查 AOH |
| VD | Vendor Direct | 供應商直送 | 直接鎖定不可訂購 |

### holdOrder (採購權限)

| 值 | 說明 | PO商品可訂 | DC商品可訂 |
|:--:|------|:----------:|:----------:|
| N | 無 HOLD ORDER | ✅ | ✅ |
| A | 暫停採購及調撥 | ❌ | ✅ |
| B | 暫停店對店調撥 | ✅ | ❌ |
| C | 暫停所有採購調撥 | ❌ | ❌ |
| D | 暫停但允許MD下單及調撥 | ✅ | ✅ |
| E | 暫停但允許MD調撥 | ✅ | ✅ |

---

## Pricing Calculation Details

### 會員折扣計算公式

#### Type 2 (Cost Markup)

```
discountPercent = discountPer / 100

For 商品類型 P:
  newPrice = CEIL(unitCost × (1 + discountPercent))

  IF !taxZero AND taxType == '1':
    newPrice = FLOOR(newPrice × 1.05)  // 加營業稅

  discountAmt = posAmt - newPrice
  actPosAmt = newPrice  // 完全替換
  totalPrice = newPrice × quantity
```

**特殊處理**: Type 2 執行後必須重新分類商品

#### Type 0 (Discounting)

```
discountPercent = discountPer / 100

For 商品類型 P:
  totalPrice = CEIL(posAmt + (bonusTotal / quantity) + (promotionDisc / quantity))
  discountAmt = CEIL(totalPrice × discountPercent)
  memberDisc = discountAmt × quantity  // 僅記錄，不修改 actPosAmt
```

#### Type 1 (Down Margin)

```
discountPercent = discountPer / 100

For 商品類型 P:
  discountAmt = CEIL((actPosAmt + FLOOR(promotionDisc / quantity)) × discountPercent)
  newPrice = actPosAmt - discountAmt
  actPosAmt = newPrice  // 直接扣減
  totalPrice = newPrice × quantity
  posAmtChangePrice = true
```

### 工種變價分攤 (Apportionment)

**觸發條件**: 安裝/運送工種有變價授權時

```
changePriceForInstall = installPrice - actInstallPrice
changePriceForDelivery = deliveryPrice - actDeliveryPrice

For each SKU in workType:
  ratio = skuSubtotal / workTypeTotal
  apportionmentAmount = ROUND(changePriceAmount × ratio)
  remainder = apportionmentAmount MOD quantity  // 按 detlSeq 逐一分攤 $1
```

### 工種折數選擇

| GoodsType | 中文說明 | 使用折數 | 成本計算公式 |
|-----------|----------|---------|-------------|
| I | 標準安裝 | DISCOUNT_BASE | `amt × DISCOUNT_BASE ÷ 營業稅率` |
| IA | 提前安裝 | DISCOUNT_BASE | `amt × DISCOUNT_BASE ÷ 營業稅率` |
| IS | 補充安裝 | DISCOUNT_BASE | `amt × DISCOUNT_BASE ÷ 營業稅率` |
| IE | 其他安裝 | DISCOUNT_EXTRA | `amt × DISCOUNT_EXTRA ÷ 營業稅率` |
| IC | 安裝調整 | 無 | 成本固定為 0 |

### 優惠券分攤

**固定面額 (Type 0)**:
```
For each eligibleSku:
  ratio = skuPrice / totalEligiblePrice
  apportionedAmount = CEIL(couponAmount × ratio)

// 最後一筆修正
lastSkuApportionment = couponAmount - sumOfPreviousApportionments
```

**封頂處理**:
```
IF totalCouponDiscount > totalEligibleAmount:
  actualDiscount = totalEligibleAmount
  excessAmount = totalCouponDiscount - totalEligibleAmount  // 差額不退
```

### 四捨五入規則

| 方法 | Java 實作 | 使用場景 |
|------|-----------|----------|
| 無條件進位 | `Math.ceil(value)` | 折扣金額計算 (Type 0/1/2) |
| 四捨五入 | `Math.round(value)` | 分攤金額計算 |
| 無條件捨去 | `BigDecimal.ROUND_FLOOR` | 營業稅計算 |

---

## Key Entities

### Order Aggregate (訂單聚合根)

| 屬性 | 說明 | 類型 |
|------|------|------|
| OrderId | 訂單編號（10 位數字流水號，起始 3000000000） | Value Object |
| ProjectId | 專案代號（16 位編碼） | Value Object |
| OrderStatus | 訂單狀態（1=草稿/2=報價/3=已付款/4=有效/5=結案/6=作廢） | Enum |
| Customer | 客戶資訊（會員卡號、姓名、電話、地址、折扣類型） | Value Object |
| DeliveryAddress | 安運地址（郵遞區號、完整地址） | Value Object |
| lines | 訂單行項清單 | List\<OrderLine\> |
| calculation | 價格試算結果 | Value Object |

### OrderLine Entity (訂單行項實體)

| 屬性 | 說明 | 類型 |
|------|------|------|
| LineId | 行項編號 | Value Object |
| Product | 商品資訊 | Reference |
| quantity | 數量 | Integer |
| unitPrice | 單價 | Money |
| deliveryMethod | 運送方式（N/D/V/C/F/P） | Enum |
| stockMethod | 備貨方式（X=現貨/Y=訂購） | Enum |
| installation | 安裝明細 | Value Object |
| delivery | 運送明細 | Value Object |
| discounts | 折扣清單 | List\<Discount\> |

### PriceCalculation Value Object (價格試算結果)

| 屬性 | 說明 | 類型 |
|------|------|------|
| productTotal | 商品總額（ComputeType 1） | Money |
| installationTotal | 安裝總額（ComputeType 2） | Money |
| deliveryTotal | 運送總額（ComputeType 3） | Money |
| memberDiscount | 會員卡折扣（ComputeType 4） | Money |
| directShipmentTotal | 直送費用總額（ComputeType 5） | Money |
| couponDiscount | 折價券折扣（ComputeType 6） | Money |
| grandTotal | 應付總額 | Money |

---

## Domain Services

### OrderPricingService

協調訂單價格計算的領域服務，執行 12 步驟計價流程。

```
職責:
- calculateOrder(Order): PriceCalculation
- validatePriceAuthorization(OrderLine): ValidationResult

12 步驟執行順序:
1. 還原銷售單價 (revertAllSkuAmt)
2. 工種變價分攤檢查 (apportionmentDiscount)
3. 商品分類 (AssortSku)
4. Cost Markup Type 2 + 重新分類
5. 多重促銷 (Event A-H)
6. Discounting Type 0
7. Down Margin Type 1
8. 特殊會員折扣 (條件執行)
9-12. 生成 6 種 ComputeType
```

### ProductEligibilityService

商品銷售資格驗證的領域服務。

```
職責:
- checkEligibility(SkuNo, Channel, Store): EligibilityResult

8 層驗證順序:
1. 格式驗證（SKU 符合編碼規則）
2. 存在性驗證（SKU 存在於商品主檔）
3. 系統商品排除（allowSales ≠ 'N'）
4. 稅別驗證（taxType 為有效值 0/1/2）
5. 銷售禁止（allowSales = true AND holdOrder = false）
6. 類別限制（類別不在禁售清單）
7. 廠商凍結（廠商狀態為 'A' 或 DC 商品有 AOH）
8. 採購組織（商品在門市公司採購組織內）
```

### MemberDiscountService

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

### WorkTypeMappingService

工種對照與折數選擇的領域服務。

```
職責:
- findWorkType(subDeptId, classId, subClassId): WorkTypeMapping
- getDiscountRate(GoodsType): DiscountRate

查詢邏輯:
- 三個欄位 (大類+中類+子類) 完全匹配 TBL_WORKTYPE_SKUNO_MAPPING
- 若有多筆結果，取第一筆
- 若無匹配，商品視為不需安裝服務
```

---

## Business Rules

### 訂單建立規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| OR-001 | 必填客戶資訊 | 訂單必須有會員卡號或身分證或電話 |
| OR-002 | 必填聯絡資訊 | 訂單必須有聯絡人和聯絡電話 |
| OR-003 | 必填安運地址 | 訂單必須有有效的安運地址和郵遞區號（3 碼） |
| OR-004 | 必填出貨店 | 訂單必須指定出貨店 |
| OR-005 | 至少一項商品 | 訂單必須包含至少一項商品 |
| OR-006 | 數量不為零 | 商品數量總和不可為零 |

### 價格與折扣規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| PR-001 | 試算必要性 | 訂單提交前必須完成價格試算 |
| PR-002 | 變價授權 | 實際價格與原始價格不同時必須有變價授權 |
| PR-003 | 最低工資 | 安裝費用低於工種最低工資時顯示警告（純運和宅配除外），但不阻擋提交 |
| PR-004 | 折扣上限 | 折價金額不可超過可折抵商品總額，超過時封頂處理（差額不退） |
| PR-005 | 百分比範圍 | 折扣百分比必須在 1~100% 之間 |
| PR-006 | 門檻達標 | 使用優惠券必須達到使用門檻 |
| PR-007 | 張數限制 | 優惠券使用張數不可超過單筆限制 |

### 會員折扣執行順序規則

| 順序 | 折扣類型 | actPosAmt 修改 | 備註 |
|------|---------|---------------|------|
| 1 | Type 2 (Cost Markup) | 完全替換 | 執行後必須重新分類商品 |
| 2 | 促銷引擎 (Event A-H) | 扣減 | OMS 控制優先級 |
| 3 | Type 0 (Discounting) | 不修改 | 僅記錄於 memberDisc 欄位 |
| 4 | Type 1 (Down Margin) | 直接扣減 | 可與促銷疊加 (2022-05-13 變更) |
| 5 | 特殊會員折扣 | 依邏輯 | 僅當 memberDiscSkus.isEmpty() 時執行 |

### 備貨方式規則

| 規則編號 | 規則名稱 | 描述 |
|---------|---------|------|
| ST-001 | 直送僅限訂購 | 直送(V)商品備貨方式僅能為訂購(Y) |
| ST-002 | 當場自取僅限現貨 | 當場自取(C)備貨方式僅能為現貨(X) |
| ST-003 | 供應商凍結鎖定 | 供應商已凍結時強制訂購 |
| ST-004 | 採購組織限制 | 商品不在門市採購組織內時強制訂購 |

---

## Non-Functional Requirements

### Performance

| 指標 | 目標值 | 說明 |
|------|--------|------|
| NFR-001 | 價格試算 API 回應時間 ≤ 3 秒 | 包含 12 步驟計價流程，500 筆明細以內（可配置） |
| NFR-002 | 商品資格驗證 API 回應時間 ≤ 500ms | 8 層驗證單一商品 |
| NFR-003 | 訂單建立 API 回應時間 ≤ 2 秒 | 含資料驗證與持久化 |

### Observability

| 指標 | 目標值 | 說明 |
|------|--------|------|
| NFR-010 | 操作日誌使用 Logback 輸出 | 訂單建立、試算、提交等關鍵操作皆需記錄 |
| NFR-011 | 日誌格式為結構化 JSON | 包含操作者、時間、動作類型、訂單編號、關鍵參數 |
| NFR-012 | 日誌輸出至檔案 | 配合既有 UAT 環境，避免 schema 變更 |

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
  int failureLevel,          // 失敗層級 1-8
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
- **SC-004**: 商品資格驗證 8 層規則全部正確執行，無漏驗或誤判
- **SC-005**: Playwright E2E 測試通過率 100%，關鍵步驟皆有截圖驗證

---

## Technical Constraints

### Constitution Compliance

依據專案憲法 (Constitution v1.10.0)：

1. **I. Pragmatic DDD**: 採用務實 DDD，Order 為聚合根，避免過度設計
2. **II. 3-Table Rule**: 訂單核心資料表（TBL_ORDER_MAST, TBL_ORDER_DETL, TBL_ORDER_COMPUTE）符合 3 表規則
3. **III. KISS Principle**: 優先選擇最簡單的解決方案
4. **IV. Database Schema Documentation**: 參考 `docs/tables/*.html`
5. **V. Legacy Codebase Reference**: 參考 `C:/projects/som` 既有系統
6. **VI. Stateless Backend**: API 無狀態，Token 驗證透過 Keycloak
7. **VII. Playwright Verification**: E2E 測試需截圖驗證
8. **VIII. MyBatis Generator Pattern**: 基礎 Mapper 自動產生，複雜查詢使用 CustomMapper
9. **IX. No Lombok Policy**: 使用 Java Records 作為 DTO，Entity 手動撰寫 getter/setter
10. **X. Data Class Convention**: DTO 使用 Records，Entity 使用傳統 Class
11. **XI. OpenAPI RESTful API Standard**: 詳見 `contracts/order-api.yaml`
12. **XII. Angular 21+ Frontend Standard**: Standalone Components、Signals、新控制流語法
13. **XIII. Code Coverage Requirement**: 目標 ≥80% line & branch coverage

---

## Supplementary Specifications

| 規格文件 | 說明 | 狀態 |
|---------|------|------|
| [product-query-spec.md](./product-query-spec.md) | 商品查詢與可訂購規則 (OD-001~020) | Complete |
| [pricing-calculation-spec.md](./pricing-calculation-spec.md) | 12 步驟計價流程與公式 | Complete (v1.1.0) |
| [worktype-mapping-spec.md](./worktype-mapping-spec.md) | 工種對照與安裝費計算 | Complete |
| [delivery-fee-spec.md](./delivery-fee-spec.md) | 運費配送與材積計算 | Complete |
| [product-query-verification-report.md](./product-query-verification-report.md) | 商品查詢規格驗證報告 | Complete |

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
| FR-012 可訂購規則 | ✅ 已驗證 | BzSkuInfoServices.java:237-275 |
| FR-014 大型家具判斷 | ✅ 已驗證 | LargeFurnitureService.java:31-60 |
| FR-015 外包服務商品 | ✅ 已驗證 | BzSkuInfoServices.java:748-750 |
| FR-020 12步驟計價 | ✅ 已驗證 | BzSoServices.java:4367 doCalculate |
| FR-021 會員折扣順序 | ✅ 已驗證 | Type 2 → 促銷 → Type 0 → Type 1 → Special |
| 安裝服務代碼 | ✅ 已驗證 | GoodsType.java 定義 I/IA/IE/IC/IS/FI |
| ComputeType 1-6 | ✅ 已驗證 | SoConstant.java COMPUTE_TYPE_* |
| 工種折數選擇 | ✅ 已驗證 | BzSoServices.java:934-948 |
| 配送方式 6 種 | ✅ 已驗證 | SoConstant.java:93-113 |

---

## Change Log

| 版本 | 日期 | 變更內容 |
|------|------|---------|
| 1.0.0 | 2025-12-19 | 初版建立 |
| 1.1.0 | 2025-12-20 | 新增 EC-008 運送/備貨相容性規則 |
| 2.0.0 | 2025-12-20 | 整合 5 份補充規格，修正歧義：<br>- 6層驗證修正為8層驗證<br>- DeliveryMethod F 確認為「宅配」非免運費<br>- Constitution 版本更新為 v1.10.0<br>- 新增 OD-001~020 可訂購規則<br>- 新增 GoodsType、ComputeType 完整定義<br>- 新增工種折數選擇規則<br>- 新增優惠券分攤與四捨五入規則 |
