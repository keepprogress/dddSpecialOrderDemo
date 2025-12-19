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

### 6.1 TBL_ORDER_MAST 對應

| Entity Field | Table Column | Type | Description |
|--------------|--------------|------|-------------|
| id.value | ORDER_ID | VARCHAR2(10) | 訂單編號 |
| projectId.value | PROJECT_ID | VARCHAR2(16) | 專案代號 |
| customer.memberId | MEMBER_CARD_ID | VARCHAR2(20) | 會員卡號 |
| customer.name | MEMBER_NAME | VARCHAR2(100) | 會員姓名 |
| deliveryAddress.zipCode | INSTALL_ZIP | VARCHAR2(3) | 郵遞區號 |
| deliveryAddress.fullAddress | INSTALL_ADDR | VARCHAR2(200) | 安運地址 |
| storeId | STORE_ID | VARCHAR2(5) | 出貨店 |
| channelId | CHANNEL_ID | VARCHAR2(4) | 通路代號 |
| status.code | ORDER_STATUS | VARCHAR2(1) | 訂單狀態 |
| calculation.grandTotal.amount | TOTAL_AMT | NUMBER(10) | 應付總額 |

### 6.2 TBL_ORDER_DETL 對應

| Entity Field | Table Column | Type | Description |
|--------------|--------------|------|-------------|
| id.value | LINE_ID | VARCHAR2(20) | 行項編號 |
| skuNo | SKU_NO | VARCHAR2(20) | 商品編號 |
| quantity | QUANTITY | NUMBER(5) | 數量 |
| unitPrice.amount | POS_AMT | NUMBER(10) | 原始單價 |
| actualUnitPrice.amount | ACT_POS_AMT | NUMBER(10) | 實際單價 |
| deliveryMethod.code | DELIVERY_FLAG | VARCHAR2(1) | 運送方式 |
| stockMethod.code | STOCK_METHOD | VARCHAR2(1) | 備貨方式 |
| memberDisc.amount | MEMBER_DISC | NUMBER(10) | 會員折扣 |

### 6.3 TBL_ORDER_COMPUTE 對應

| Entity Field | Table Column | Type | Description |
|--------------|--------------|------|-------------|
| computeType | COMPUTE_TYPE | VARCHAR2(1) | 試算類型 |
| computeName | COMPUTE_NAME | VARCHAR2(50) | 類型名稱 |
| totalPrice.amount | TOTAL_PRICE | NUMBER(10) | 原始價格 |
| discount.amount | DISCOUNT | NUMBER(10) | 折扣金額 |
| actTotalPrice.amount | ACT_TOTAL_PRICE | NUMBER(10) | 實際價格 |

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
