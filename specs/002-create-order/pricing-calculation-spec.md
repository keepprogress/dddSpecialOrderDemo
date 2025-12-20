# Pricing Calculation Specification

> **Feature**: 002-create-order
> **Version**: 1.0.0
> **Last Updated**: 2025-12-20
> **Status**: Draft

## Overview

本文件定義訂單價格計算的完整規格，包含 12 步驟計價流程、會員折扣計算公式、促銷邏輯、分攤機制等詳細規則。

**來源驗證**: 所有計算公式均來自 `C:/projects/som` Legacy 程式碼驗證。

---

## 1. 12 Steps Calculation Flow

### 1.1 Flow Overview

```text
doCalculate(Order)
  |
  +-- Step 1: revertAllSkuAmt()
  |            還原所有商品的銷售單價至原始 POS 價
  |
  +-- Step 2: apportionmentDiscount()
  |            工種變價分攤（安裝/運送費變價時按比例分攤到各商品）
  |
  +-- Step 3: assortSku()
  |            商品分類（P/I/FI/DD/VD/D）
  |
  +-- Step 4: memberDiscountType2()
  |            Cost Markup 會員折扣（成本加價）
  |
  +-- Step 5: promotionCalculation()
  |            多重促銷計算（8 種促銷類型）
  |
  +-- Step 6: memberDiscountType0()
  |            Discounting 會員折扣（折價率折扣）
  |
  +-- Step 7: memberDiscountType1()
  |            Down Margin 會員折扣（固定降價）
  |
  +-- Step 8: specialMemberDiscount()
  |            特殊會員折扣（VIP/員工價）
  |
  +-- Step 9: generateComputeType1()
  |            生成商品小計試算記錄
  |
  +-- Step 10: generateComputeType2()
  |             生成安裝小計試算記錄
  |
  +-- Step 11: generateComputeType3()
  |             生成運送小計試算記錄
  |
  +-- Step 12: generateComputeType456()
               生成會員折扣/直送/折價券試算記錄
```

### 1.2 Step Dependency

```text
Step 1 ─────────────────────────────────────────────────────────┐
                                                                 |
Step 2 ────────────────────────────────────────────────────────>|
                                                                 |
Step 3 ────────────────────────────────────────────────────────>|
                                                                 |
Step 4 (Type 2) ──> Step 5 (Promotion) ──> Step 6 (Type 0) ──> |
                                                                 |
                                           Step 7 (Type 1) ──> |
                                                                 |
                                           Step 8 (Special) ──>|
                                                                 |
Step 9-12 ←─────────────────────────────────────────────────────┘
(可並行執行)
```

### 1.3 Step Details

#### Step 1: Revert All SKU Amount

**目的**: 還原商品價格至原始狀態，確保每次試算從相同基準開始

**規則**:
- `actPosAmt` = `posAmt`
- `actInstallPrice` = `installPrice` × `quantity`
- `actDeliveryPrice` = `deliveryPrice` × `quantity`
- 清除所有折扣記錄

**程式碼來源**: `BzSoServices.java` revertAllSkuAmt()

---

#### Step 2: Apportionment Discount (工種變價分攤)

**目的**: 當安裝/運送工種有變價授權時，將變價差額分攤到該工種下的各商品

**觸發條件**:
- `installAuthEmpId` IS NOT NULL AND `installPrice` != `actInstallPrice`
- `deliveryAuthEmpId` IS NOT NULL AND `deliveryPrice` != `actDeliveryPrice`

**分攤公式**:

```
changePriceForInstall = installPrice - actInstallPrice
changePriceForDelivery = deliveryPrice - actDeliveryPrice

For each SKU in workType:
  ratio = skuSubtotal / workTypeTotal
  apportionmentAmount = ROUND(changePriceAmount × ratio)

  newUnitPrice = originalUnitPrice - (apportionmentAmount / quantity)
  newTotalPrice = newUnitPrice × quantity - remainder

  Where:
    remainder = apportionmentAmount MOD quantity
```

**最後一筆修正**:
```
lastApportionment = changePriceAmount - sumOfPreviousApportionments
```

**程式碼來源**: `BzSoServices.java:5375-5572`

---

#### Step 3: Assort SKU (商品分類)

**目的**: 按商品類型分群，供後續計算使用

**分類規則**:

| GoodsType | 分類 | 說明 |
|-----------|------|------|
| P | lstGoodsSku | 一般商品 |
| I, IA, IE, IC, IS | lstInstallSku | 安裝商品 |
| FI | lstFreeInstallSku | 免安商品 |
| DD | lstDeliverSku | 運送商品 |
| VD | lstDirectShipmentSku | 直送商品 |
| D | lstWorkTypeSku | 工種商品 |

**程式碼來源**: `BzSoServices.java` AssortSku()

---

#### Step 4: Member Discount Type 2 (Cost Markup)

**目的**: 成本加價折扣 - 以成本為基準計算售價

**執行時機**: 所有促銷計算之前（因為會完全替換 actPosAmt）

**計算公式**:

```
discountPercent = discountPer / 100

For 商品類型 P:
  newPrice = CEIL(unitCost × (1 + discountPercent))

  IF !taxZero AND taxType == '1':
    newPrice = FLOOR(newPrice × 1.05)  // 加營業稅

  discountAmt = posAmt - newPrice
  actPosAmt = newPrice
  totalPrice = newPrice × quantity
```

**特殊處理**:
- Type 2 執行後必須重新分類商品（因 actPosAmt 完全替換）
- 計算結果為負數時 → 歸零並發送告警信

**程式碼來源**: `SoFunctionMemberDisServices.java:474-511`

---

#### Step 5: Promotion Calculation (多重促銷)

**目的**: 執行 8 種促銷類型計算

**促銷類型**:

| Type | Class | 說明 | 優先級 |
|------|-------|------|--------|
| A | SoEventA | 印花價 | 1 (最高) |
| B | SoEventB | 發票金額滿額加價購 | 2 |
| C | SoEventC | 商品滿額/滿件全面優惠 | 3 |
| D | SoEventD | 每買 M 個享 N 個優惠 | 4 |
| E | SoEventE | 買 A 群組享 B 群組優惠 | 5 |
| F | SoEventF | 合購價 | 6 |
| G | SoEventG | 共用商品合購價 | 7 |
| H | SoEventH | 單品拆價合購價 | 8 |

**執行順序**:
1. 依 EVENT_NO 分群商品
2. 依促銷類型優先級執行
3. 已參與促銷的商品不再參與其他促銷

**程式碼來源**: `SoComputeFunctionMain.java:76-105`

---

#### Step 6: Member Discount Type 0 (Discounting)

**目的**: 折價率折扣 - 不修改 actPosAmt，僅記錄折扣金額

**計算公式**:

```
discountPercent = discountPer / 100

For 商品類型 P:
  totalPrice = CEIL(
    posAmt
    + (bonusTotal / quantity)     // 紅利折抵
    + (promotionDisc / quantity)  // 組促折扣
  )

  discountAmt = CEIL(totalPrice × discountPercent)
  memberDisc = discountAmt × quantity

For 安裝商品 (I/IA/IE/IC/IS):
  discountAmt = CEIL(installPrice × discountPercent)
  memberDisc = discountAmt × quantity

For 運送商品 (DD/VD):
  discountAmt = CEIL(deliveryPrice × discountPercent)
  memberDisc = discountAmt × quantity
```

**特性**:
- 不修改 actPosAmt
- 折扣記錄於 memberDisc 欄位
- 最終在 ComputeType 4 中匯總

**程式碼來源**: `SoFunctionMemberDisServices.java:405-437`

---

#### Step 7: Member Discount Type 1 (Down Margin)

**目的**: 固定降價折扣 - 直接修改 actPosAmt

**計算公式**:

```
discountPercent = discountPer / 100

For 商品類型 P:
  discountAmt = CEIL((actPosAmt + FLOOR(promotionDisc / quantity)) × discountPercent)

  newPrice = actPosAmt - discountAmt
  actPosAmt = newPrice
  totalPrice = newPrice × quantity
  posAmtChangePrice = true

For 安裝商品:
  discountAmt = CEIL(installPrice × discountPercent)
  newPrice = installPrice - discountAmt
  actInstallPrice = newPrice × quantity
  installChangePrice = true

For 運送商品:
  discountAmt = CEIL(deliveryPrice × discountPercent)
  newPrice = deliveryPrice - discountAmt
  actDeliveryPrice = newPrice × quantity
  deliveryChangePrice = true
```

**特性**:
- 直接修改實際價格欄位
- 設定變價標記

**程式碼來源**: `SoFunctionMemberDisServices.java:440-471`

---

#### Step 8: Special Member Discount

**目的**: 執行特殊會員折扣（VIP、員工價等）

**觸發條件**:
- 會員卡別符合特殊會員類型
- 前面步驟無任何會員折扣

**程式碼來源**: `BzSoServices.java` soComputeMemberDisForSpecial()

---

#### Steps 9-12: Generate ComputeType Records

**目的**: 生成 6 種試算記錄

**ComputeType 定義**:

| Type | 名稱 | 計算來源 |
|------|------|----------|
| 1 | 商品小計 | SUM(lstGoodsSku.actPosAmt × quantity) |
| 2 | 安裝小計 | SUM(lstInstallSku.actInstallPrice) |
| 3 | 運送小計 | SUM(lstDeliverSku.actDeliveryPrice) |
| 4 | 會員卡折扣 | SUM(all.memberDisc) (負數) |
| 5 | 直送費用小計 | SUM(lstDirectShipmentSku.actDeliveryPrice) |
| 6 | 折價券折扣 | SUM(all.couponDisc) (負數) |

**每種 ComputeType 的欄位**:

| 欄位 | 說明 |
|------|------|
| totalPrice | 總額（折扣前） |
| discount | 折扣金額 |
| actTotalPrice | 實際總額（= totalPrice + discount） |
| actTotalPriceTx | 應稅金額 |
| actTotalPriceNtx | 免稅金額 |

**程式碼來源**: `BzSoServices.java:4961-5046` setOrderCompute()

---

## 2. Member Discount Calculation

### 2.1 Discount Type Overview

| Type | Name | Behavior | Price Field Modified |
|------|------|----------|---------------------|
| 0 | Discounting | 折價率折扣，不改價 | memberDisc only |
| 1 | Down Margin | 固定降價，改價 | actPosAmt/actInstallPrice/actDeliveryPrice |
| 2 | Cost Markup | 成本加價，完全替換價格 | actPosAmt (全部替換) |

### 2.2 Discount Percent Interpretation

```
discountPer = 0   → 無折扣 (100% 原價)
discountPer = 25  → 75 折 (付 75%)
discountPer = 60  → 4 折 (付 40%)
discountPer = 100 → 0 折 (免費)
```

### 2.3 Execution Priority

```
Type 2 (Cost Markup)
  ↓
Promotion Calculation
  ↓
Type 0 (Discounting)
  ↓
Type 1 (Down Margin)
  ↓
Special Member Discount
```

### 2.4 Tax-Aware Calculation

Type 2 營業稅處理：
```java
IF !taxZero AND taxType == '1':
  newPrice = FLOOR(newPrice × 1.05)  // 5% 營業稅
```

Type 0/1 應稅/免稅分離：
```java
IF taxType == '1':  // 應稅
  taxDisc += discountAmt
ELSE IF taxType == '2' OR taxZero:  // 免稅或零稅
  freeTaxDisc += discountAmt
```

---

## 3. Installation Fee Calculation

### 3.1 Installation Fee Components

```
安裝費 = 標準安裝費 + 進階安裝費 + 其他安裝費 - 免安折扣
```

**商品類型對應**:

| GoodsType | 說明 | 計算方式 |
|-----------|------|----------|
| I | 基本安裝 | installPrice × quantity |
| IA | 進階安裝 | installPrice × quantity |
| IE | 其他安裝 | installPrice × quantity |
| IC | 安裝調整 | installPrice × quantity |
| IS | 補安裝費 | installPrice × quantity |
| FI | 免安折扣 | 負數金額，抵扣安裝費 |

### 3.2 Worktype Cost Discount

**標安折數** (WORKTYPE_DISCOUNT_BASE): 有免安時適用
**非標折數** (WORKTYPE_DISCOUNT_EXTRA): 無免安時適用

```
IF hasFreeInstall:
  discountRate = WORKTYPE_DISCOUNT_BASE
ELSE:
  discountRate = WORKTYPE_DISCOUNT_EXTRA

adjustedCost = baseCost × discountRate
```

### 3.3 Minimum Wage Check

```
IF totalInstallFee < sumOfFreeInstallDiscount:
  WARNING: "工種 {workTypeId} 變價金額不可小於免安金額"
```

**例外**: 純運(D)和宅配除外

---

## 4. Delivery Fee Calculation

### 4.1 Delivery Fee Formula

**基礎公式**:
```
deliveryFee = volumeRate × volume
```

**材積計算**:
```
volume = (length × width × height) / 5000  // cm → 材積
```

### 4.2 Delivery Types

| DeliveryFlag | 說明 | 費用計算 |
|--------------|------|----------|
| N | 運送 | 標準運送費 |
| D | 純運 | 純運費（無安裝） |
| V | 直送 | 直送費（廠商送） |
| C | 當場自取 | 0 |
| P | 下次自取 | 0 |

### 4.3 Free Delivery Conditions

```
IF freeDelivery == 'Y' OR freeDeliveryShipping == 'Y':
  deliveryFee = 0

IF orderTotal >= freeDeliveryThreshold:
  deliveryFee = 0
```

---

## 5. Coupon Apportionment

### 5.1 Fixed Amount Coupon (Type 0)

**分攤公式**:
```
For each eligibleSku:
  ratio = skuPrice / totalEligiblePrice
  apportionedAmount = CEIL(couponAmount × ratio)

// 最後一筆修正
lastSkuApportionment = couponAmount - sumOfPreviousApportionments
```

### 5.2 Discount Rate Coupon (Type 1)

```
discountAmount = CEIL(skuPrice × couponDiscountRate)
```

### 5.3 Cap Processing (封頂處理)

```
IF totalCouponDiscount > totalEligibleAmount:
  actualDiscount = totalEligibleAmount
  excessAmount = totalCouponDiscount - totalEligibleAmount  // 不退
```

### 5.4 Tax Separation

```
FOR each sku in eligibleSkus:
  IF taxType == '1':  // 應稅
    taxCouponDisc += skuCouponDisc
  ELSE:               // 免稅
    freeTaxCouponDisc += skuCouponDisc
```

---

## 6. Tax Calculation

### 6.1 Tax Types

| TaxType | 說明 | 稅率 |
|---------|------|------|
| 1 | 應稅 | 5% |
| 2 | 免稅 | 0% |
| 0 | 零稅 | 0% (特殊) |

### 6.2 Tax Calculation Rules

**應稅商品**:
```
taxAmount = FLOOR(actTotalPrice / 1.05 × 0.05)
// 或
taxAmount = actTotalPrice - FLOOR(actTotalPrice / 1.05)
```

**免稅/零稅商品**:
```
taxAmount = 0
```

### 6.3 Tax Zero Order

當訂單標記為零稅 (`taxZero = true`) 時：
- 所有商品視為零稅計算
- Type 2 不加營業稅
- 試算結果全部歸入 actTotalPriceNtx

---

## 7. Rounding Rules

### 7.1 Rounding Methods

| 方法 | Java 實作 | 使用場景 |
|------|-----------|----------|
| 無條件進位 | `Math.ceil(value)` | 折扣金額計算 |
| 四捨五入 | `Math.round(value)` | 分攤金額計算 |
| 無條件捨去 | `BigDecimal.ROUND_FLOOR` | 營業稅計算 |

### 7.2 Application Rules

```
Type 0/1 折扣: CEIL
Type 2 新價格: CEIL
Type 2 營業稅: FLOOR
分攤金額: ROUND
最後一筆分攤: 差額修正（不使用舍入）
```

---

## 8. Data Structures

### 8.1 MemberDiscVO

```java
public record MemberDiscVO(
    String discountId,      // 折扣代號
    String discPer,         // 折扣率 (00~100)
    String discType,        // 折扣類型 (0/1/2)
    String startDate,       // 生效日
    String endDate,         // 失效日
    String discAmt,         // 折扣金額 (應稅)
    String ndiscAmt,        // 折扣金額 (免稅)
    String taxType,         // 稅別
    String skuNo,           // 商品編號
    String detlSeqId        // 明細流水號
) {}
```

### 8.2 OrderComputeVO

```java
public record OrderComputeVO(
    String orderId,
    String computeType,       // 1-6
    String storeId,
    BigDecimal totalPrice,    // 總額
    BigDecimal discount,      // 折扣 (負數)
    BigDecimal actTotalPrice, // 實際總額
    BigDecimal actTotalPriceTx,  // 應稅
    BigDecimal actTotalPriceNtx, // 免稅
    String authorizedEmpId,
    String authorizedEmpName,
    String authorizedReason,
    LocalDateTime authorizedDate,
    BigDecimal discountFrcms  // 加盟折扣
) {}
```

### 8.3 Price Fields in OrderDetlVO

| 欄位 | 說明 | 修改時機 |
|------|------|----------|
| posAmt | 原始 POS 價 | 不修改 |
| actPosAmt | 實際 POS 價 | Type 1/2 修改 |
| totalPrice | 商品總價 | Type 1/2 修改 |
| installPrice | 安裝單價 | 不修改 |
| actInstallPrice | 安裝實際總價 | Type 1 修改 |
| deliveryPrice | 運送單價 | 不修改 |
| actDeliveryPrice | 運送實際總價 | Type 1 修改 |
| memberDisc | 會員折扣 | Type 0 記錄 |
| discountAmt | 組促折扣 | 促銷計算記錄 |
| coupon0Disc | 固定面額折價券 | 折價券分攤 |
| coupon1Disc | 折扣率折價券 | 折價券計算 |
| preApportion | 分攤前金額 | 工種變價記錄 |
| workTypeChangPriceDisc | 工種變價分攤額 | 工種變價記錄 |

---

## 9. Flowcharts

### 9.1 Complete Calculation Flow

```mermaid
flowchart TD
    A[Start doCalculate] --> B[Step 1: Revert All SKU Amount]
    B --> C[Step 2: Worktype Change Price Apportionment]
    C --> D[Step 3: Assort SKU by GoodsType]
    D --> E[Step 4: Type 2 Cost Markup]
    E --> F{Has Type 2?}
    F -->|Yes| G[Re-assort SKU]
    F -->|No| H[Step 5: Promotion Calculation]
    G --> H
    H --> I[Step 6: Type 0 Discounting]
    I --> J[Step 7: Type 1 Down Margin]
    J --> K[Step 8: Special Member Discount]
    K --> L[Step 9-12: Generate ComputeTypes]
    L --> M[End]
```

### 9.2 Member Discount Type Selection

```mermaid
flowchart TD
    A[Member Discount] --> B{Check discType}
    B -->|Type 2| C[Cost Markup]
    B -->|Type 0| D[Discounting]
    B -->|Type 1| E[Down Margin]
    C --> F[Calculate: CEIL unitCost × 1+rate]
    C --> G[Replace actPosAmt completely]
    D --> H[Calculate: CEIL price × rate]
    D --> I[Store in memberDisc only]
    E --> J[Calculate: CEIL price × rate]
    E --> K[Subtract from actPosAmt]
```

---

## 10. Validation Rules

### 10.1 Pre-calculation Validation

| Rule | Description | Action |
|------|-------------|--------|
| V-001 | 商品明細數量 ≤ 500 | 超過則阻止試算 |
| V-002 | 至少一項商品 | 無商品則阻止試算 |
| V-003 | 會員資料已載入 | 無會員則跳過會員折扣 |

### 10.2 Post-calculation Validation

| Rule | Description | Action |
|------|-------------|--------|
| V-010 | Type 2 結果為負數 | 歸零並發送告警信 |
| V-011 | 安裝費低於免安金額 | 顯示警告（不阻擋） |
| V-012 | 試算結果合計不為負 | 異常則記錄日誌 |

---

## 11. Legacy Code References

| 功能 | 檔案 | 方法/行號 |
|------|------|----------|
| 主計價流程 | BzSoServices.java | doCalculate():4367 |
| 商品分類 | BzSoServices.java | AssortSku() |
| 工種變價分攤 | BzSoServices.java | apportionmentDiscount():5375-5572 |
| 會員折扣 Type 0 | SoFunctionMemberDisServices.java | updateMemberDiscForDiscountType0():405-437 |
| 會員折扣 Type 1 | SoFunctionMemberDisServices.java | updateMemberDiscForDiscountType1():440-471 |
| 會員折扣 Type 2 | SoFunctionMemberDisServices.java | updateMemberDiscForDiscountType2():474-511 |
| 促銷計算 | SoComputeFunctionMain.java | init():76-105 |
| 生成試算記錄 | BzSoServices.java | setOrderCompute():4961-5046 |
| 折價券分攤 | BzSoServices.java | :1613-1684 |
| 應稅免稅分離 | SoFunctionMemberDisServices.java | :595-628 |

---

## 12. Appendix

### A. GoodsType Constants

```java
public enum GoodsType {
    P("P", "一般商品"),
    I("I", "基本安裝"),
    IA("IA", "進階安裝"),
    IE("IE", "其他安裝"),
    IC("IC", "安裝調整"),
    IS("IS", "補安裝費"),
    FI("FI", "免安折扣"),
    DD("DD", "運送"),
    VD("VD", "直送"),
    D("D", "工種"),
    VT("VT", "會員卡折扣"),
    CP("CP", "折價券"),
    CK("CK", "折扣券"),
    CI("CI", "酷卡折扣"),
    BP("BP", "紅利折抵"),
    TT("TT", "總額折扣");
}
```

### B. ComputeType Constants

```java
public static final String COMPUTE_TYPE_1 = "1";  // 商品小計
public static final String COMPUTE_TYPE_2 = "2";  // 安裝小計
public static final String COMPUTE_TYPE_3 = "3";  // 運送小計
public static final String COMPUTE_TYPE_4 = "4";  // 會員卡折扣
public static final String COMPUTE_TYPE_5 = "5";  // 直送費用小計
public static final String COMPUTE_TYPE_6 = "6";  // 折價券折扣
```

### C. Member Discount Type Constants

```java
public static final String MEMBER_DISCOUNT_TYPE_0 = "0";  // Discounting
public static final String MEMBER_DISCOUNT_TYPE_1 = "1";  // Down Margin
public static final String MEMBER_DISCOUNT_TYPE_2 = "2";  // Cost Markup
```

### D. Tax Type Constants

```java
public static final String SKU_TAX_TYPE_0 = "0";  // 零稅
public static final String SKU_TAX_TYPE_1 = "1";  // 應稅
public static final String SKU_TAX_TYPE_2 = "2";  // 免稅
```
