# Data Model: 新增訂單頁面

**Feature Branch**: `002-create-order`
**Created**: 2025-12-19
**Status**: Complete

---

## 1. Order Context (核心領域)

### 1.1 Order Aggregate Root (訂單聚合根)

```java
/**
 * 訂單聚合根
 *
 * 職責：
 * - 管理訂單生命週期（Draft → Quotation → Active → Paid → Closed）
 * - 協調訂單行項的新增、修改、刪除
 * - 執行訂單層級的業務規則驗證
 */
public class Order {
    // === Identity ===
    private OrderId id;                    // 訂單編號（10 位數字流水號）
    private ProjectId projectId;           // 專案代號（16 位編碼）

    // === Customer Information ===
    private Customer customer;             // 客戶資訊
    private DeliveryAddress deliveryAddress; // 安運地址

    // === Order Context ===
    private String storeId;                // 出貨店
    private String channelId;              // 通路代號
    private OrderStatus status;            // 訂單狀態
    private OrderSource source;            // 訂單來源

    // === Personnel ===
    private String handlerId;              // 接單人員 ID
    private String specialistId;           // 專員 ID

    // === Order Lines ===
    private List<OrderLine> lines;         // 訂單行項清單（最多 50 項）

    // === Calculation Result ===
    private PriceCalculation calculation;  // 價格試算結果

    // === Audit ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // === Business Methods ===
    public OrderLine addLine(Product product, int quantity);
    public void removeLine(LineId lineId);
    public void updateQuantity(LineId lineId, int quantity);
    public void attachInstallation(LineId lineId, InstallationSpec spec);
    public void attachDelivery(LineId lineId, DeliverySpec spec);
    public PriceCalculation calculate();
    public void applyCoupon(Coupon coupon);
    public void redeemBonusPoints(BonusRedemption redemption);
    public void submit();
    public void cancel(String reason);

    // === Invariants ===
    // - 訂單至少包含一項商品
    // - 商品數量總和不為零
    // - 商品數量不超過 50 項
    // - 提交前必須完成價格試算
}
```

### 1.2 OrderLine Entity (訂單行項實體)

```java
/**
 * 訂單行項實體
 *
 * 職責：
 * - 管理單一商品的數量、價格、服務配置
 * - 關聯安裝服務與運送服務
 * - 記錄折扣明細
 */
public class OrderLine {
    // === Identity ===
    private LineId id;                     // 行項編號
    private int serialNo;                  // 序號（顯示用）

    // === Product Reference ===
    private String skuNo;                  // 商品編號
    private String skuName;                // 商品名稱

    // === Quantity & Pricing ===
    private int quantity;                  // 數量
    private Money unitPrice;               // 單價（原始 POS 價格）
    private Money actualUnitPrice;         // 實際單價（折扣後）
    private TaxType taxType;               // 稅別

    // === Delivery Configuration ===
    private DeliveryMethod deliveryMethod; // 運送方式（N/D/V/C/F/P）
    private StockMethod stockMethod;       // 備貨方式（X/Y）
    private LocalDate deliveryDate;        // 預計出貨日

    // === Service Details ===
    private InstallationDetail installation; // 安裝明細
    private DeliveryDetail delivery;       // 運送明細
    private List<InstallationService> installationServices; // 安裝服務清單

    // === Discounts ===
    private List<Discount> discounts;      // 折扣清單
    private Money memberDisc;              // 會員折扣金額（Type 0 用）
    private Money bonusDisc;               // 紅利折抵金額

    // === Flags ===
    private boolean posAmtChangePrice;     // 是否已變價
    private boolean hasFreeInstall;        // 是否有免安折扣

    // === Business Methods ===
    public Money getSubtotal();            // 小計 = actualUnitPrice × quantity
    public Money getInstallationTotal();   // 安裝費用小計
    public Money getDeliveryTotal();       // 運送費用小計
}
```

### 1.3 Value Objects

#### OrderId (訂單編號)

```java
/**
 * 訂單編號值物件
 * 格式: 10 位數字流水號
 * 起始: 3000000000
 */
public record OrderId(
    String value
) {
    public OrderId {
        Objects.requireNonNull(value, "訂單編號不可為空");
        if (!value.matches("\\d{10}")) {
            throw new IllegalArgumentException("訂單編號必須為 10 位數字");
        }
    }
}
```

#### ProjectId (專案代號)

```java
/**
 * 專案代號值物件
 * 格式: 店別(5碼) + 年(2碼) + 月日(4碼) + 流水號(5碼)
 * 範例: 1234524121800001
 */
public record ProjectId(
    String value
) {
    public ProjectId {
        Objects.requireNonNull(value, "專案代號不可為空");
        if (!value.matches("\\d{16}")) {
            throw new IllegalArgumentException("專案代號必須為 16 位數字");
        }
    }

    public String getStoreId() {
        return value.substring(0, 5);
    }

    public String getYear() {
        return value.substring(5, 7);
    }

    public String getMonthDay() {
        return value.substring(7, 11);
    }

    public String getSequence() {
        return value.substring(11, 16);
    }
}
```

#### Customer (客戶資訊)

```java
/**
 * 客戶資訊值物件
 */
public record Customer(
    String memberId,          // 會員卡號
    String cardType,          // 卡別（0: 一般卡, 1: 商務卡）
    String name,              // 姓名
    String gender,            // 性別（M/F）
    String phone,             // 電話
    String cellPhone,         // 手機
    LocalDate birthday,       // 生日
    String contactName,       // 聯絡人
    String contactPhone,      // 聯絡電話
    String vipType,           // VIP 類型
    MemberDiscountType discountType // 折扣類型
) {
    // 驗證: 會員卡號、身分證、電話至少有一項
    public boolean hasIdentification() {
        return memberId != null || phone != null || cellPhone != null;
    }
}
```

#### DeliveryAddress (安運地址)

```java
/**
 * 安運地址值物件
 */
public record DeliveryAddress(
    String zipCode,           // 郵遞區號（3 碼）
    String fullAddress        // 完整地址
) {
    public DeliveryAddress {
        Objects.requireNonNull(zipCode, "郵遞區號不可為空");
        Objects.requireNonNull(fullAddress, "地址不可為空");
        if (!zipCode.matches("\\d{3}")) {
            throw new IllegalArgumentException("郵遞區號必須為 3 碼數字");
        }
    }
}
```

#### Money (金額)

```java
/**
 * 金額值物件
 * 所有金額計算結果四捨五入取整數
 */
public record Money(
    int amount               // 金額（整數，負數表示折扣）
) {
    public static final Money ZERO = new Money(0);

    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    public Money subtract(Money other) {
        return new Money(this.amount - other.amount);
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount * multiplier);
    }

    public Money multiply(BigDecimal rate, RoundingMode roundingMode) {
        BigDecimal result = BigDecimal.valueOf(amount).multiply(rate);
        return new Money(result.setScale(0, roundingMode).intValue());
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isPositive() {
        return amount > 0;
    }
}
```

#### PriceCalculation (價格試算結果)

```java
/**
 * 價格試算結果值物件
 * 對應 6 種 ComputeType
 */
public record PriceCalculation(
    Money productTotal,           // ComputeType 1: 商品小計
    Money installationTotal,      // ComputeType 2: 安裝小計
    Money deliveryTotal,          // ComputeType 3: 運送小計
    Money memberDiscount,         // ComputeType 4: 會員卡折扣
    Money directShipmentTotal,    // ComputeType 5: 直送費用小計
    Money couponDiscount,         // ComputeType 6: 折價券折扣（含免安）
    Money taxAmount,              // 稅額
    Money grandTotal,             // 應付總額
    List<MemberDiscVO> memberDiscounts, // 會員折扣明細
    List<String> warnings,        // 警告訊息（降級時使用）
    boolean promotionSkipped,     // 是否跳過促銷計算
    LocalDateTime calculatedAt    // 試算時間
) {
    public static PriceCalculation empty() {
        return new PriceCalculation(
            Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO,
            Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO,
            List.of(), List.of(), false, LocalDateTime.now()
        );
    }
}
```

#### MemberDiscountType (會員折扣類型)

```java
/**
 * 會員折扣類型枚舉
 */
public enum MemberDiscountType {
    TYPE_0("0", "Discounting", false),     // 折扣率，不修改 actPosAmt
    TYPE_1("1", "Down Margin", true),      // 固定折扣，修改 actPosAmt
    TYPE_2("2", "Cost Markup", true),      // 成本加成，完全替換 actPosAmt
    SPECIAL("SPECIAL", "Special", true);   // 特殊會員折扣

    private final String code;
    private final String name;
    private final boolean modifiesActPosAmt;

    // getters...
}
```

#### DeliveryMethod (運送方式)

```java
/**
 * 運送方式枚舉
 */
public enum DeliveryMethod {
    MANAGED("N", "代運"),              // 需指派工種，可選擇安裝服務
    PURE_DELIVERY("D", "純運"),        // 單純運送，不含安裝
    DIRECT_SHIPMENT("V", "直送"),      // 供應商直接配送
    IMMEDIATE_PICKUP("C", "當場自取"), // 在店內立即取貨
    FREE_DELIVERY("F", "免運"),        // 免運費配送
    LATER_PICKUP("P", "下次自取");     // 擇日至店取貨

    private final String code;
    private final String name;

    // getters...

    /**
     * 檢查與備貨方式的相容性
     */
    public boolean isCompatibleWith(StockMethod stockMethod) {
        return switch (this) {
            case DIRECT_SHIPMENT -> stockMethod == StockMethod.PURCHASE_ORDER;
            case IMMEDIATE_PICKUP -> stockMethod == StockMethod.IN_STOCK;
            default -> true;
        };
    }
}
```

#### StockMethod (備貨方式)

```java
/**
 * 備貨方式枚舉
 */
public enum StockMethod {
    IN_STOCK("X", "現貨"),           // 門市有庫存
    PURCHASE_ORDER("Y", "訂購");     // 需向供應商訂購

    private final String code;
    private final String name;

    // getters...
}
```

#### TaxType (稅別)

```java
/**
 * 稅別枚舉
 */
public enum TaxType {
    TAXABLE("1", "應稅", BigDecimal.valueOf(1.05)),
    TAX_FREE("2", "免稅", BigDecimal.ONE),
    ZERO_TAX("3", "零稅", BigDecimal.ONE);

    private final String code;
    private final String name;
    private final BigDecimal rate;

    // getters...
}
```

#### OrderStatus (訂單狀態)

```java
/**
 * 訂單狀態枚舉
 */
public enum OrderStatus {
    DRAFT("D", "草稿"),
    QUOTATION("Q", "報價"),
    ACTIVE("A", "有效"),
    PAID("P", "已付款"),
    CLOSED("C", "已結案"),
    CANCELLED("X", "作廢");

    private final String code;
    private final String name;

    // getters...

    /**
     * 檢查狀態轉換是否合法
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case DRAFT -> target == QUOTATION || target == ACTIVE;
            case QUOTATION -> target == ACTIVE || target == DRAFT;
            case ACTIVE -> target == PAID || target == CANCELLED;
            case PAID -> target == CLOSED || target == CANCELLED;
            default -> false;
        };
    }

    /**
     * 是否可使用紅利點數
     */
    public boolean canUseBonusPoints() {
        return this == ACTIVE || this == PAID;
    }
}
```

---

## 2. Member Context (支援領域)

### 2.1 MemberDiscVO (會員折扣 VO)

```java
/**
 * 會員折扣明細 VO
 * 用於記錄價格試算中的會員折扣資訊
 */
public record MemberDiscVO(
    String skuNo,             // 商品編號
    String discType,          // 折扣類型（0/1/2/SPECIAL）
    String discTypeName,      // 折扣類型名稱
    Money originalPrice,      // 原價
    Money discountPrice,      // 折扣價
    Money discAmt,            // 折扣金額
    BigDecimal discRate,      // 折扣率（Type 0 用）
    BigDecimal markupRate     // 加成比例（Type 2 用）
) {}
```

### 2.2 MemberResponse (會員回應 DTO)

```java
/**
 * 會員查詢回應 DTO
 */
public record MemberResponse(
    String memberId,
    String cardType,
    String name,
    String birthday,
    String gender,
    String cellPhone,
    String address,
    String discType,
    String discTypeName,
    BigDecimal discRate,
    BigDecimal markupRate
) {}
```

---

## 3. Catalog Context (支援領域)

### 3.1 ProductInfo (商品資訊)

```java
/**
 * 商品資訊（來自商品主檔查詢）
 */
public record ProductInfo(
    String skuNo,
    String skuName,
    ProductCategory category,
    TaxType taxType,
    Money marketPrice,        // 市價
    Money registeredPrice,    // 登錄價
    Money posPrice,           // POS 價格
    Money cost,               // 成本
    boolean allowSales,
    boolean holdOrder,
    boolean isSystemSku,
    boolean isNegativeSku,
    boolean freeDelivery,
    boolean freeDeliveryShipping,
    boolean allowDirectShipment,
    boolean allowHomeDelivery
) {}
```

### 3.2 EligibilityResult (資格驗證結果)

```java
/**
 * 商品資格驗證結果
 */
public record EligibilityResult(
    boolean eligible,
    String failureReason,
    int failureLevel,         // 1-6, 0 表示通過
    ProductInfo product,
    List<InstallationService> availableServices,
    List<StockMethod> availableStockMethods,
    List<DeliveryMethod> availableDeliveryMethods
) {
    public static EligibilityResult success(
            ProductInfo product,
            List<InstallationService> services,
            List<StockMethod> stockMethods,
            List<DeliveryMethod> deliveryMethods) {
        return new EligibilityResult(true, null, 0, product,
            services, stockMethods, deliveryMethods);
    }

    public static EligibilityResult failure(int level, String reason) {
        return new EligibilityResult(false, reason, level, null,
            List.of(), List.of(), List.of());
    }
}
```

### 3.3 InstallationService (安裝服務)

```java
/**
 * 安裝服務類型
 */
public record InstallationService(
    String serviceType,       // I, IA, IE, IC, IS, FI
    String serviceName,
    String serviceSku,        // 服務商品 SKU
    Money basePrice,
    boolean isMandatory,
    BigDecimal discountBase,  // 標安成本折數
    BigDecimal discountExtra  // 非標成本折數
) {
    public Money calculateCost() {
        BigDecimal costRate = "IE".equals(serviceType)
            ? discountExtra
            : discountBase;
        return basePrice.multiply(costRate, RoundingMode.FLOOR);
    }
}
```

---

## 4. Pricing Context (支援領域)

### 4.1 ComputeTypeVO (試算類型 VO)

```java
/**
 * 試算類型 VO
 * 對應 6 種 ComputeType
 */
public record ComputeTypeVO(
    String computeType,       // 1-6
    String computeName,       // 類型名稱
    Money totalPrice,         // 原始價格
    Money discount,           // 折扣金額
    Money actTotalPrice       // 實際價格
) {
    public static ComputeTypeVO of(String type, String name,
            Money total, Money discount) {
        return new ComputeTypeVO(type, name, total, discount,
            total.add(discount)); // discount 為負數
    }
}
```

### 4.2 CouponValidation (優惠券驗證)

```java
/**
 * 優惠券驗證結果
 */
public record CouponValidation(
    boolean valid,
    String failureReason,
    Money discountAmount,
    List<String> applicableSkus
) {}
```

---

## 5. Fulfillment Context (支援領域)

### 5.1 WorkType (工種)

```java
/**
 * 工種資訊
 */
public record WorkType(
    String workTypeId,
    String workTypeName,
    WorkCategory category,
    Money minimumWage,
    BigDecimal basicDiscount,     // 標安成本折數
    BigDecimal advancedDiscount,  // 非標成本折數
    BigDecimal deliveryDiscount   // 運送成本折數
) {
    /**
     * 計算安裝成本
     */
    public Money calculateInstallationCost(Money basePrice, boolean isBasic) {
        BigDecimal rate = isBasic ? basicDiscount : advancedDiscount;
        return basePrice.multiply(rate, RoundingMode.FLOOR);
    }

    /**
     * 檢查最低工資
     */
    public boolean meetsMinimumWage(Money installationTotal) {
        if (isPureDelivery() || isHomeDelivery()) {
            return true; // 純運與宅配不檢查最低工資
        }
        return installationTotal.amount >= minimumWage.amount;
    }

    public boolean isPureDelivery() {
        return "0000".equals(workTypeId);
    }

    public boolean isHomeDelivery() {
        return category == WorkCategory.HOME_DELIVERY;
    }
}
```

### 5.2 WorkCategory (工種類別)

```java
/**
 * 工種類別枚舉
 */
public enum WorkCategory {
    INSTALLATION("I", "安裝工種"),
    DELIVERY("D", "運送工種"),
    HOME_DELIVERY("H", "宅配工種"),
    PURE_DELIVERY("P", "純運工種");

    private final String code;
    private final String name;

    // getters...
}
```

---

## 6. Database Entity Mapping

> **重要**: 以下對應來自 MyBatisGenerator 產生的 Entity 檔案，欄位已驗證
>
> **驗證來源**:
> - `backend/src/main/java/com/tgfc/som/entity/TblOrder.java`
> - `backend/src/main/java/com/tgfc/som/entity/TblOrderDetl.java`
> - `backend/src/main/java/com/tgfc/som/entity/TblOrderCompute.java`
> - `backend/src/main/java/com/tgfc/som/entity/TblSku.java`
> - `backend/src/main/java/com/tgfc/som/entity/TblSkuStore.java`

### 6.1 TBL_ORDER 對應 (訂單主檔)

**PK**: ORDER_ID

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| orderId | ORDER_ID | VARCHAR2(10) | NOT NULL | 訂單單號 (流水號從3000000000開始) |
| projectId | PROJECT_ID | VARCHAR2(20) | NOT NULL | 專案代號 店別(5碼)+年(2碼)+月日(4碼)+流水號(5碼) |
| channelId | CHANNEL_ID | VARCHAR2(5) | NOT NULL | 通路別 |
| storeId | STORE_ID | VARCHAR2(5) | NOT NULL | 店別 |
| systemFlag | SYSTEM_FLAG | VARCHAR2(5) | NOT NULL | 系統別 |
| orderStatusId | ORDER_STATUS_ID | VARCHAR2(5) | NOT NULL | 客戶訂單狀態代碼 |
| outStoreId | OUT_STORE_ID | VARCHAR2(5) | NOT NULL | 出貨店別 |
| handleEmpId | HANDLE_EMP_ID | VARCHAR2(20) | NULL | 接單人工號 |
| handleEmpName | HANDLE_EMP_NAME | VARCHAR2(40) | NULL | 接單人姓名 |
| specialistEmpId | SPECIALIST_EMP_ID | VARCHAR2(20) | NULL | 專區專員工號 |
| specialistEmpName | SPECIALIST_EMP_NAME | VARCHAR2(40) | NULL | 專區專員姓名 |
| memberCardId | MEMBER_CARD_ID | VARCHAR2(13) | NULL | 會員卡號 |
| memberCardType | MEMBER_CARD_TYPE | VARCHAR2(1) | NULL | 會員卡別 |
| memberName | MEMBER_NAME | VARCHAR2(40) | NULL | 會員姓名 |
| memberBirthday | MEMBER_BIRTHDAY | DATE | NULL | 會員生日 YYYYMMDD |
| memberGender | MEMBER_GENDER | VARCHAR2(1) | NULL | 會員姓別 |
| memberContact | MEMBER_CONTACT | VARCHAR2(45) | NULL | 會員聯絡人 |
| memberContactPhone | MEMBER_CONTACT_PHONE | VARCHAR2(20) | NULL | 聯絡電話 |
| memberPhone | MEMBER_PHONE | VARCHAR2(20) | NULL | 會員電話 |
| memberCellPhone | MEMBER_CELL_PHONE | VARCHAR2(20) | NULL | 會員手機 |
| memberAddrZip | MEMBER_ADDR_ZIP | VARCHAR2(3) | NULL | 會員地址郵編 |
| memberAddr | MEMBER_ADDR | VARCHAR2(60) | NULL | 會員地址 |
| installAddrZip | INSTALL_ADDR_ZIP | VARCHAR2(3) | NULL | 安運地址郵編 |
| installAddr | INSTALL_ADDR | VARCHAR2(60) | NULL | 安運地址 |
| orderSource | ORDER_SOURCE | VARCHAR2(5) | NULL | 訂單來源 |
| installCreatedFlag | INSTALL_CREATED_FLAG | VARCHAR2(1) | NULL | 產生安運單flag |
| poCreatedFlag | PO_CREATED_FLAG | VARCHAR2(1) | NULL | 產生PO單flag |
| expiredDate | EXPIRED_DATE | DATE | NULL | 過期日期 |
| remark | REMARK | VARCHAR2(2000) | NULL | 訂單備註 |
| trackingNo | TRACKING_NO | VARCHAR2(20) | NULL | 客怨單號 (FROM CRM) |
| totalPrice | TOTAL_PRICE | NUMBER(9,2) | NULL | 訂單總金額_應稅 |
| totalPriceNtx | TOTAL_PRICE_NTX | NUMBER(9,2) | NULL | 訂單總金額_免稅/零稅 |
| taxZero | TAX_ZERO | VARCHAR2(1) | NULL | 是否零稅 |
| reinstall | REINSTALL | VARCHAR2(1) | NULL | 二安單註記 (Y=二安訂單, N=一般訂單) |
| parentOrderId | PARENT_ORDER_ID | VARCHAR2(10) | NULL | 原訂單單號 (二安) |
| reinstallReason | REINSTALL_REASON | VARCHAR2(20) | NULL | 二安原因 |
| ecFlag | EC_FLAG | VARCHAR2(1) | NULL | EC 訂單註記 Y/N |
| ecOrderId | EC_ORDER_ID | VARCHAR2(20) | NULL | EC 主訂單號碼 |
| paymentType | PAYMENT_TYPE | VARCHAR2(2) | NULL | EC 付款別 |
| payOnDelivery | PAY_ON_DELIVERY | VARCHAR2(1) | NULL | 貨到付款 (Y/N) |
| companyCode | COMPANY_CODE | VARCHAR2(8) | NULL | 統一編號 |
| isDonate | IS_DONATE | VARCHAR2(1) | NULL | EC 是否捐贈發票 (Y/N) |
| donateCompnayCode | DONATE_COMPNAY_CODE | VARCHAR2(10) | NULL | EC 愛心碼/社福統編 |
| prnInvoice | PRN_INVOICE | VARCHAR2(1) | NULL | EC 是否列印發票 |
| prnDetl | PRN_DETL | VARCHAR2(1) | NULL | 是否列印明細 (Y/N) |
| commonId | COMMON_ID | VARCHAR2(20) | NULL | EC 共通載具 |
| closeEmpId | CLOSE_EMP_ID | VARCHAR2(20) | NULL | 結案人員工號 |
| closeEmpName | CLOSE_EMP_NAME | VARCHAR2(40) | NULL | 結案人員姓名 |
| closeDate | CLOSE_DATE | DATE | NULL | 結案日期 |
| closeReasonId | CLOSE_REASON_ID | VARCHAR2(5) | NULL | SO結案原因 |
| invalidReasonId | INVALID_REASON_ID | VARCHAR2(5) | NULL | SO作廢原因 |
| invalidDate | INVALID_DATE | DATE | NULL | SO作廢日期 |
| invalidEmpId | INVALID_EMP_ID | VARCHAR2(20) | NULL | 作廢者工號 |
| invalidEmpName | INVALID_EMP_NAME | VARCHAR2(40) | NULL | 作廢者姓名 |
| tzAuthEmpId | TZ_AUTH_EMP_ID | VARCHAR2(20) | NULL | 零稅授權者工號 |
| tzAuthEmpName | TZ_AUTH_EMP_NAME | VARCHAR2(40) | NULL | 零稅授權者姓名 |
| createDate | CREATE_DATE | DATE | NULL | 建立日期時間 |
| createEmpId | CREATE_EMP_ID | VARCHAR2(20) | NULL | 建立人員工號 |
| createEmpName | CREATE_EMP_NAME | VARCHAR2(40) | NULL | 建立人員姓名 |
| updateDate | UPDATE_DATE | DATE | NULL | 更新日期時間 |
| updateEmpId | UPDATE_EMP_ID | VARCHAR2(20) | NULL | 更新人員工號 |
| updateEmpName | UPDATE_EMP_NAME | VARCHAR2(40) | NULL | 更新人員姓名 |

### 6.2 TBL_ORDER_DETL 對應 (訂單商品明細檔)

**PK**: ORDER_ID + DETL_SEQ_ID

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| orderId | ORDER_ID | VARCHAR2(10) | NOT NULL | 客戶訂單編號 |
| detlSeqId | DETL_SEQ_ID | VARCHAR2(5) | NOT NULL | 流水號 |
| storeId | STORE_ID | VARCHAR2(5) | NULL | 店別 |
| skuNo | SKU_NO | VARCHAR2(9) | NULL | SKU_NO |
| skuName | SKU_NAME | VARCHAR2(50) | NULL | 品名 |
| subDeptId | SUB_DEPT_ID | VARCHAR2(3) | NULL | 次部門 |
| classId | CLASS_ID | VARCHAR2(3) | NULL | 主類別 |
| subClassId | SUB_CLASS_ID | VARCHAR2(3) | NULL | 次類別 |
| taxType | TAX_TYPE | VARCHAR2(1) | NULL | 稅別 (0=零稅,1=應稅,2=免稅) |
| worktypeId | WORKTYPE_ID | VARCHAR2(4) | NULL | 工種 |
| worktypeName | WORKTYPE_NAME | VARCHAR2(60) | NULL | 工種名稱 |
| deliveryWorktypeId | DELIVERY_WORKTYPE_ID | VARCHAR2(4) | NULL | 運送工種 |
| deliveryDate | DELIVERY_DATE | DATE | NULL | 出貨日 |
| tradeStatus | TRADE_STATUS | VARCHAR2(1) | NULL | 備貨方式 (X=現貨, Y=訂購) |
| quantity | QUANTITY | NUMBER(8) | NULL | 數量 |
| unitCost | UNIT_COST | NUMBER(9,2) | NULL | 成本 |
| installFlag | INSTALL_FLAG | VARCHAR2(1) | NULL | 安裝註記 (Y/N) |
| deliveryFlag | DELIVERY_FLAG | VARCHAR2(1) | NULL | 運送註記 (N=運送, D=純運, V=直送, C=當場自取, P=下次自取) |
| parentSeqId | PARENT_SEQ_ID | VARCHAR2(5) | NULL | 母流水號 |
| goodsType | GOODS_TYPE | VARCHAR2(3) | NULL | 商品性質分類 (P/I/IA/IC/IS/IE/D/DD/VD/VT/CP/CK/CI/BP/TT/FI) |
| barcode | BARCODE | VARCHAR2(14) | NULL | 國際碼 |
| skuStatus | SKU_STATUS | VARCHAR2(1) | NULL | 商品狀態 (A/D) |
| holdOrder | HOLD_ORDER | VARCHAR2(1) | NULL | 不可採購 (A/B/C/N) |
| remark | REMARK | VARCHAR2(200) | NULL | 商品備註 |
| vendorId | VENDOR_ID | VARCHAR2(10) | NULL | 廠商代號 |
| mktAmt | MKT_AMT | NUMBER(9,2) | NULL | 市場價 |
| regAmt | REG_AMT | NUMBER(9,2) | NULL | 一般售價 |
| posAmt | POS_AMT | NUMBER(9,2) | NULL | 原POS售價 |
| actPosAmt | ACT_POS_AMT | NUMBER(9,2) | NULL | POS售價 (實際售價) |
| installPrice | INSTALL_PRICE | NUMBER(9,2) | NULL | 安裝金額小計 |
| deliveryPrice | DELIVERY_PRICE | NUMBER(9,2) | NULL | 運送金額小計 |
| actInstallPrice | ACT_INSTALL_PRICE | NUMBER(9,2) | NULL | 實際安裝金額小計 |
| actDeliveryPrice | ACT_DELIVERY_PRICE | NUMBER(9,2) | NULL | 實際運送金額小計 |
| openPrice | OPEN_PRICE | VARCHAR2(1) | NULL | 是否可變價商品 (Y/N) |
| totalPrice | TOTAL_PRICE | NUMBER(9,2) | NULL | 商品售價小計 |
| discountAmt | DISCOUNT_AMT | NUMBER(9,2) | NULL | 折扣金額 (組促用) |
| discountQty | DISCOUNT_QTY | NUMBER(8) | NULL | 折扣數量 (組促用) |
| discountType | DISCOUNT_TYPE | VARCHAR2(2) | NULL | 折扣類型 (組促Code) |
| worktypeDiscountBase | WORKTYPE_DISCOUNT_BASE | NUMBER(9,2) | NULL | 標安工種成本折數 |
| worktypeDiscountExtra | WORKTYPE_DISCOUNT_EXTRA | NUMBER(9,2) | NULL | 非標工種成本折數 |
| deliveryDiscount | DELIVERY_DISCOUNT | NUMBER(9,2) | NULL | 運送成本折數 |
| eventNos | EVENT_NOS | VARCHAR2(10) | NULL | 降價促銷 Event |
| eventNosp | EVENT_NOSP | VARCHAR2(10) | NULL | 多重促銷 Event |
| crmDiscountId | CRM_DISCOUNT_ID | VARCHAR2(12) | NULL | 會員折扣ID |
| bonusPoints | BONUS_POINTS | NUMBER(8) | NULL | 商品紅利使用點數 |
| dmPointsAmt | DM_POINTS_AMT | NUMBER(9,2) | NULL | 商品紅利點數折扣總金額 |
| height | HEIGHT | NUMBER(9,2) | NULL | 高 |
| width | WIDTH | NUMBER(9,2) | NULL | 寬 |
| deep | DEEP | NUMBER(9,2) | NULL | 深 |
| weight | WEIGHT | NUMBER(9,2) | NULL | 重量 |
| totalVolume | TOTAL_VOLUME | NUMBER(9,2) | NULL | 總材積 (單元材積*數量) |
| totalWeight | TOTAL_WEIGHT | NUMBER(9,2) | NULL | 總重量 (重量*數量) |
| discountCode | DISCOUNT_CODE | VARCHAR2(20) | NULL | 折扣碼 (手動輸入) |
| limitFlag | LIMIT_FLAG | VARCHAR2(1) | NULL | 商品限量註記 (Y/N) |
| goodsAuthEmpId | GOODS_AUTH_EMP_ID | VARCHAR2(20) | NULL | 商品變價授權者工號 |
| goodsAuthEmpName | GOODS_AUTH_EMP_NAME | VARCHAR2(40) | NULL | 商品變價授權者姓名 |
| goodsAuthReason | GOODS_AUTH_REASON | VARCHAR2(5) | NULL | 商品變價原因 |
| goodsAuthDate | GOODS_AUTH_DATE | DATE | NULL | 商品變價日期 |
| installAuthEmpId | INSTALL_AUTH_EMP_ID | VARCHAR2(20) | NULL | 安裝變價授權者工號 |
| installAuthEmpName | INSTALL_AUTH_EMP_NAME | VARCHAR2(40) | NULL | 安裝變價授權者姓名 |
| installAuthReason | INSTALL_AUTH_REASON | VARCHAR2(5) | NULL | 安裝變價原因 |
| installAuthDate | INSTALL_AUTH_DATE | DATE | NULL | 安裝變價日期 |
| deliveryAuthEmpId | DELIVERY_AUTH_EMP_ID | VARCHAR2(20) | NULL | 運送變價授權者工號 |
| deliveryAuthEmpName | DELIVERY_AUTH_EMP_NAME | VARCHAR2(40) | NULL | 運送變價授權者姓名 |
| deliveryAuthReason | DELIVERY_AUTH_REASON | VARCHAR2(5) | NULL | 運送變價原因 |
| deliveryAuthDate | DELIVERY_AUTH_DATE | DATE | NULL | 運送變價日期 |
| stampFlag | STAMP_FLAG | VARCHAR2(1) | NULL | 印花價格使用標記 |
| salesType | SALES_TYPE | VARCHAR2(1) | NULL | 業績別 (0=訂金, 1=業績) |
| eventNosStamp | EVENT_NOS_STAMP | VARCHAR2(10) | NULL | 選擇印花商品之Event |
| nskuFlag | NSKU_FLAG | VARCHAR2(1) | NULL | 負向SKU標記 (Y/N, 預設N) |
| nskuDetlSeqId | NSKU_DETL_SEQ_ID | VARCHAR2(5) | NULL | 負向SKU對應的原SKU DETL_SEQ_ID |
| poInQueue | PO_IN_QUEUE | VARCHAR2(1) | NULL | 是否已轉PO但未呼叫完成WS (Y/N) |
| preApportion | PRE_APPORTION | NUMBER(9,2) | NULL | 分攤前安運金額 |
| dmCouponAmt | DM_COUPON_AMT | NUMBER(9,2) | NULL | Down Margin折價券分攤金額 |
| createDate | CREATE_DATE | DATE | NULL | 建立日期時間 |
| createEmpId | CREATE_EMP_ID | VARCHAR2(20) | NULL | 建立人員工號 |
| createEmpName | CREATE_EMP_NAME | VARCHAR2(40) | NULL | 建立人員姓名 |
| updateDate | UPDATE_DATE | DATE | NULL | 更新日期時間 |
| updateEmpId | UPDATE_EMP_ID | VARCHAR2(20) | NULL | 更新人員工號 |
| updateEmpName | UPDATE_EMP_NAME | VARCHAR2(40) | NULL | 更新人員姓名 |

### 6.3 TBL_ORDER_COMPUTE 對應 (訂單試算記錄檔)

**PK**: ORDER_ID + COMPUTE_TYPE

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| orderId | ORDER_ID | VARCHAR2(10) | NOT NULL | 訂單 |
| computeType | COMPUTE_TYPE | VARCHAR2(1) | NOT NULL | 試算項目分類碼 (1=商品, 2=安裝, 3=運送, 4=會員卡折扣, 5=直送費用, 6=折價券) |
| storeId | STORE_ID | VARCHAR2(5) | NULL | 店別 |
| totalPrice | TOTAL_PRICE | NUMBER(9,2) | NULL | 總額 |
| discount | DISCOUNT | NUMBER(9,2) | NULL | 折扣 |
| actTotalPrice | ACT_TOTAL_PRICE | NUMBER(9,2) | NULL | 實際總額 |
| actTotalPriceTx | ACT_TOTAL_PRICE_TX | NUMBER(9,2) | NULL | 應稅總額 |
| actTotalPriceNtx | ACT_TOTAL_PRICE_NTX | NUMBER(9,2) | NULL | 免稅/零稅總額 |
| authorizedEmpId | AUTHORIZED_EMP_ID | VARCHAR2(20) | NULL | 總額折扣授權者工號 |
| authorizedEmpName | AUTHORIZED_EMP_NAME | VARCHAR2(40) | NULL | 總額折扣授權者姓名 |
| authorizedReason | AUTHORIZED_REASON | VARCHAR2(5) | NULL | 總額折扣原因 |
| authorizedDate | AUTHORIZED_DATE | DATE | NULL | 總額折扣日期 |
| updateDate | UPDATE_DATE | DATE | NULL | 更新日期 |

### 6.4 TBL_SKU 對應 (商品檔)

**PK**: SKU_NO

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| skuNo | SKU_NO | VARCHAR2(9) | NOT NULL | 商品編號 |
| skuName | SKU_NAME | VARCHAR2(50) | NULL | 商品名稱 |
| barcode | BARCODE | VARCHAR2(14) | NULL | 條碼 (商品主條碼) |
| skuType | SKU_TYPE | VARCHAR2(10) | NULL | 商品類型 |
| subDeptId | SUB_DEPT_ID | VARCHAR2(3) | NULL | 子部門 |
| classId | CLASS_ID | VARCHAR2(3) | NULL | 類別 |
| subClassId | SUB_CLASS_ID | VARCHAR2(3) | NULL | 子類別 |
| soFlag | SO_FLAG | VARCHAR2(1) | NULL | 特別訂購商品 (N/Y) |
| vendorId | VENDOR_ID | VARCHAR2(10) | NULL | 主要供應商 |
| skuStatus | SKU_STATUS | VARCHAR2(10) | NULL | 商品狀態 (A=正常, D=停止採購但可銷售) |
| taxType | TAX_TYPE | VARCHAR2(1) | NULL | 稅別 (0=零稅, 1=應稅, 2=免稅) |
| dangerFlag | DANGER_FLAG | VARCHAR2(1) | NULL | 危險商品 (N/Y) |
| openPrice | OPEN_PRICE | VARCHAR2(1) | NULL | 是否開放售價 (N/Y) |
| freeDeliver | FREE_DELIVER | VARCHAR2(1) | NULL | 免費宅配 (N/Y) |
| skuUnit | SKU_UNIT | VARCHAR2(10) | NULL | 基本單位 |
| height | HEIGHT | NUMBER(9,2) | NULL | 高 |
| width | WIDTH | NUMBER(9,2) | NULL | 寬 |
| deepth | DEEPTH | NUMBER(9,2) | NULL | 深 |
| weight | WEIGHT | NUMBER(9,2) | NULL | 重 |
| dcType | DC_TYPE | VARCHAR2(2) | NULL | 採購屬性 (XD/DC/VD) |
| skuCat | SKU_CAT | VARCHAR2(2) | NULL | 物料種類 |
| modifyTime | MODIFY_TIME | DATE | NULL | 最後修改日期 |

### 6.5 TBL_SKU_STORE 對應 (門店商品檔)

**PK**: SKU_NO + STORE_ID

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| storeId | STORE_ID | VARCHAR2(5) | NOT NULL | 店別代碼 |
| skuNo | SKU_NO | VARCHAR2(9) | NOT NULL | 商品編號 |
| channelId | CHANNEL_ID | VARCHAR2(5) | NULL | 通路代碼 |
| marketPrice | MARKET_PRICE | NUMBER(9,2) | NULL | 市價 |
| regularPrice | REGULAR_PRICE | NUMBER(9,2) | NULL | 原價 |
| posAmt | POS_AMT | NUMBER(9,2) | NULL | POS價 |
| avgCost | AVG_COST | NUMBER(9,2) | NULL | 成本 |
| skuStatus | SKU_STATUS | VARCHAR2(10) | NULL | 商品狀態 (A/D) |
| allowReturn | ALLOW_RETURN | VARCHAR2(10) | NULL | 允許退貨 (A/B/C) |
| holdOrder | HOLD_ORDER | VARCHAR2(1) | NULL | 不可採購 (A/B/C/D/E/N) |
| eventNo | EVENT_NO | VARCHAR2(10) | NULL | 降價促銷檔期編號 |
| promEventNo | PROM_EVENT_NO | VARCHAR2(10) | NULL | 組合促銷檔期編號 |
| stampEventNo | STAMP_EVENT_NO | VARCHAR2(10) | NULL | 印花促銷檔期編號 |
| displayFlag | DISPLAY_FLAG | VARCHAR2(1) | NULL | 展示品旗標 (N/Y) |
| allowSales | ALLOW_SALES | VARCHAR2(1) | NULL | 允許銷售 (N/Y) |
| mdSelection | MD_SELECTION | VARCHAR2(1) | NULL | 採購自選 (N/Y, 5選1) |
| promotion | PROMOTION | VARCHAR2(1) | NULL | 促銷中 (N/Y, 5選1) |
| futurePromotion | FUTURE_PROMOTION | VARCHAR2(1) | NULL | 即將促銷 (N/Y, 5選1) |
| storeSelection | STORE_SELECTION | VARCHAR2(1) | NULL | 門店自選 (N/Y, 5選1) |
| pogFlag | POG_FLAG | VARCHAR2(1) | NULL | POG商品旗標 (N/Y, 5選1) |
| modifyTime | MODIFY_TIME | DATE | NULL | 最後修改日期 |

### 6.6 TBL_ORDER_COMPUTE 補充欄位

| Entity Field | Table Column | Type | Nullable | Description |
|--------------|--------------|------|----------|-------------|
| discountFrcms | DISCOUNT_FRCMS | NUMBER(9,2) | NULL | 加盟折扣 |

---

## 6.7 SQL JOIN Reference (Implicit Key)

> **來源**: 從 C:/projects/som Legacy 程式碼的 MyBatis XML Mapper 檔案中提取

### 6.7.1 資料表關聯圖

```text
┌─────────────────┐       SKU_NO        ┌─────────────────────┐
│    TBL_SKU      │◄──────────────────►│   TBL_SKU_STORE     │
│  (商品主檔)      │      1 : M         │  (門店商品檔)        │
│                 │                     │                      │
│  PK: SKU_NO     │                     │  PK: SKU_NO,        │
│                 │                     │      STORE_ID        │
└─────────────────┘                     └─────────────────────┘
                                              │
                                              │ SKU_NO, STORE_ID
                                              ▼
┌─────────────────┐      ORDER_ID       ┌─────────────────────┐
│   TBL_ORDER     │◄───────────────────►│  TBL_ORDER_DETL     │
│  (訂單主檔)      │      1 : M         │  (訂單明細檔)        │
│                 │                     │                      │
│  PK: ORDER_ID   │                     │  PK: ORDER_ID,      │
│                 │                     │      DETL_SEQ_ID     │
└─────────────────┘                     └─────────────────────┘
        │
        │ ORDER_ID
        ▼
┌─────────────────────┐
│ TBL_ORDER_COMPUTE   │
│ (訂單試算記錄檔)     │
│                      │
│ PK: ORDER_ID,       │
│     COMPUTE_TYPE     │
└─────────────────────┘
```

### 6.7.2 JOIN 條件對照表

| 主表 | 從表 | JOIN 條件 | 關聯類型 | 使用場景 |
|------|------|-----------|---------|---------|
| TBL_SKU | TBL_SKU_STORE | `SK.SKU_NO = SKS.SKU_NO` | 1:M | 查詢商品的店鋪級別資訊 |
| TBL_SKU_STORE | TBL_SKU | `SKS.SKU_NO = SK.SKU_NO` | M:1 | 取得門店商品主檔資料 |
| TBL_ORDER | TBL_ORDER_DETL | `TOR.ORDER_ID = TOD.ORDER_ID` | 1:M | 查詢訂單明細 |
| TBL_ORDER | TBL_ORDER_COMPUTE | `TOR.ORDER_ID = TOC.ORDER_ID` | 1:M | 查詢訂單試算 |
| TBL_ORDER_DETL | TBL_SKU | `TOD.SKU_NO = SK.SKU_NO` | M:1 | 取得明細商品主檔 |
| TBL_ORDER_DETL | TBL_SKU_STORE | `TOD.SKU_NO = SKS.SKU_NO AND TOD.STORE_ID = SKS.STORE_ID` | M:1 | 取得明細門店商品資料 |

### 6.7.3 常用 SQL 查詢模式

**商品查詢 (SKU + Store 資訊)**:

```sql
-- Pattern 1: 基本商品店鋪 JOIN (最常用)
SELECT SK.SKU_NO, SK.SKU_NAME, SKS.POS_AMT, SKS.SKU_STATUS
FROM TBL_SKU SK
LEFT OUTER JOIN TBL_SKU_STORE SKS
    ON SK.SKU_NO = SKS.SKU_NO
WHERE SKS.STORE_ID = #{storeId}
  AND SKS.ALLOW_SALES <> 'N'

-- Pattern 2: 含通路過濾
SELECT SK.SKU_NO, SK.SKU_NAME, SKS.POS_AMT
FROM TBL_SKU SK
INNER JOIN TBL_SKU_STORE SKS
    ON SK.SKU_NO = SKS.SKU_NO
WHERE SKS.STORE_ID = #{storeId}
  AND SKS.CHANNEL_ID = #{channelId}
```

**訂單查詢 (Order + Detail + Compute)**:

```sql
-- Pattern 1: 訂單含明細
SELECT TOR.ORDER_ID, TOR.MEMBER_NAME,
       TOD.SKU_NO, TOD.SKU_NAME, TOD.QUANTITY, TOD.ACT_POS_AMT
FROM TBL_ORDER TOR
INNER JOIN TBL_ORDER_DETL TOD
    ON TOR.ORDER_ID = TOD.ORDER_ID
WHERE TOR.ORDER_ID = #{orderId}

-- Pattern 2: 訂單含試算總計
SELECT TOR.ORDER_ID, TOC.COMPUTE_TYPE, TOC.ACT_TOTAL_PRICE
FROM TBL_ORDER TOR
INNER JOIN TBL_ORDER_COMPUTE TOC
    ON TOR.ORDER_ID = TOC.ORDER_ID
WHERE TOR.ORDER_ID = #{orderId}

-- Pattern 3: 訂單明細含商品主檔
SELECT TOD.*, SK.SKU_NAME, SK.SKU_TYPE, SKS.POS_AMT
FROM TBL_ORDER_DETL TOD
INNER JOIN TBL_SKU SK
    ON TOD.SKU_NO = SK.SKU_NO
LEFT OUTER JOIN TBL_SKU_STORE SKS
    ON TOD.SKU_NO = SKS.SKU_NO
   AND TOD.STORE_ID = SKS.STORE_ID
WHERE TOD.ORDER_ID = #{orderId}
```

### 6.7.4 複合主鍵說明

| 資料表 | 複合主鍵 | 說明 |
|--------|----------|------|
| TBL_SKU_STORE | SKU_NO, STORE_ID | 同一商品在不同店別有不同價格/狀態 |
| TBL_ORDER_DETL | ORDER_ID, DETL_SEQ_ID | DETL_SEQ_ID 為該訂單內的流水號 |
| TBL_ORDER_COMPUTE | ORDER_ID, COMPUTE_TYPE | COMPUTE_TYPE 區分不同計算項目 |

---

## 7. Validation Rules Summary

| Rule ID | Entity | Field | Rule | Error Message |
|---------|--------|-------|------|---------------|
| OR-001 | Order | customer | 會員卡號/身分證/電話至少一項 | 請輸入客戶識別資訊 |
| OR-002 | Order | customer | 聯絡人和聯絡電話必填 | 請輸入聯絡資訊 |
| OR-003 | Order | deliveryAddress | 地址和郵遞區號必填 | 請輸入安運地址 |
| OR-004 | Order | storeId | 出貨店必填 | 請選擇出貨店 |
| OR-005 | Order | lines | 至少一項商品 | 訂單必須包含至少一項商品 |
| OR-006 | Order | lines | 商品數量 ≤ 50 | 訂單已達商品上限（50項） |
| PR-001 | Order | calculation | 提交前必須試算 | 請先執行價格試算 |
| PR-002 | OrderLine | actualUnitPrice | 變價需授權 | 變價商品需要授權 |
| ST-001 | OrderLine | stockMethod | 直送僅限訂購 | 直送商品僅能選擇訂購 |
| ST-002 | OrderLine | stockMethod | 當場自取僅限現貨 | 當場自取僅能選擇現貨 |
