# Promotion Event Priority Logic - Complete Code Trace

## Document Metadata

**Created**: 2025-10-27
**Phase**: Phase 2 Week 3 - Promotion Logic Analysis
**Scope**: Event A-H promotion assignment, priority resolution, and execution order
**Code Locations**: so-bzservices, so-batchjob, so-webapp
**Status**: ✅ Completed - Ready for Business Review

---

## Executive Summary

### Key Findings

**🔴 CRITICAL DISCOVERY**: The SOM system **does NOT resolve priority conflicts** between Event A-H promotions. Conflict resolution happens in the **upstream OMS (Order Management System)**, which decides which single promotion gets assigned to each SKU at each store.

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│ OMS System (External)                                           │
│ - Manages promotion catalog                                     │
│ - Assigns MP_EVENT to SKUs based on its own rules              │
│ - Stores in bs_sku_store@oms.MP_EVENT                          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ Daily Batch (after 9:00 AM)
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ SOM Database                                                     │
│ TBL_SKU_STORE.PROM_EVENT_NO ← bs_sku_store@oms.MP_EVENT        │
│ (One event number per SKU per store)                           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ User selects SKU
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ Frontend (JSP)                                                   │
│ lstSkuInfo[].eventNosp = promEventNo                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     │ User clicks "Calculate"
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ Pricing Engine                                                   │
│ - Routes to event calculator A-H based on EVENT_TYPE           │
│ - Calculates discount using event-specific rules               │
└─────────────────────────────────────────────────────────────────┘
```

### What This Means

1. **No Priority Rules in SOM**: Each SKU can have only ONE `PROM_EVENT_NO` at a time
2. **OMS Decides**: When multiple promotions could apply, OMS chooses which one
3. **SOM Executes**: SOM simply calculates the discount for the assigned promotion
4. **A→H Order**: Processing order (not priority order) in `SoComputeFunctionMain.init()`

---

## Event Type Definitions

### Event A: 印花價 (Stamp Price)

**Business Logic**: Items sold at special stamp/seal promotional price

**File**: `SoComputeFunctionMain.java:37, 64`
```java
SoEventA soEventA; // 印花價
soEventA = new SoEventA(bzSoServices, zeroTax);
```

**Implementation**: `SoEventA.java`
**Characteristics**:
- Requires stamp flag validation
- Special error messaging via `stampErrorMsg`
- First event initialized (but not highest priority)

---

### Event B: 發票金額滿額加價購 (Invoice Amount Threshold Add-on Purchase)

**Business Logic**: Buy add-on items at special price when invoice total reaches threshold

**File**: `SoComputeFunctionMain.java:38, 65, 88-96`
```java
SoEventB soEventB; // 發票金額滿額加價購

if (soEventItemsB.size() != 0) {
    long total = 0; // 商品總金額
    int posAmt = 0; // pos價
    int qty = 0; // 數量
    for (OrderDetlVO orderDetlVO : items) {
        posAmt = Integer.parseInt(orderDetlVO.getPosAmt());
        qty = Integer.parseInt(orderDetlVO.getQuantity());
        total += posAmt * qty;
    }
    soEventB.init(soEventItemsB, total, lstOrderEventMsgVO);
}
```

**Implementation**: `SoEventB.java`
**Characteristics**:
- Requires total invoice amount calculation
- Threshold-based activation
- Only event that receives `total` parameter

---

### Event C: 商品滿(金額/數量)以上，全面優惠 (Full Discount When Amount/Quantity Threshold Met)

**Business Logic**: All items get discount when total amount or quantity reaches threshold

**File**: `SoComputeFunctionMain.java:39, 66`
```java
SoEventC soEventC; // 商品滿(金額/數量)以上,全面優惠
```

**Implementation**: `SoEventC.java`
**Characteristics**:
- Can use amount OR quantity thresholds
- Applies discount to all items in promotion
- Cumulative calculation

---

### Event D: 商品每買M個，享其中N個優惠 (Buy M Items, Get N Discounted)

**Business Logic**: Buy M quantity, get N items at discount (e.g., buy 3 get 1 discounted)

**File**: `SoComputeFunctionMain.java:40, 67`
```java
SoEventD soEventD; // 商品每買M個，享其中N個優惠
```

**Implementation**: `SoEventD.java`
**Characteristics**:
- M/N ratio calculation
- Repeatable promotion (multiples of M)
- Selective discount application

---

### Event E: 買條件商品A群組(數量/金額)可享B商品群組優惠 (Buy Group A to Get Group B Discount)

**Business Logic**: Purchase from condition group A to unlock discounts on reward group B

**File**: `SoComputeFunctionMain.java:41, 68`
```java
SoEventE soEventE; // 買條件商品A群組(數量/金額)可享B商品群組優惠
```

**Implementation**: `SoEventE.java`
**Characteristics**:
- Two-group structure (condition + reward)
- Cross-item promotion
- Threshold validation on group A

---

### Event F: 合購價 (Bundle Price)

**Business Logic**: Buy multiple specific items together at special bundle price

**File**: `SoComputeFunctionMain.java:42, 69`
```java
SoEventF soEventF; // 合購價
```

**Implementation**: `SoEventF.java`
**Characteristics**:
- Requires all items in bundle
- Fixed combination
- All-or-nothing activation

---

### Event G: 共用商品合購價 (Shared Item Bundle Price)

**Business Logic**: Bundle price with shared items across multiple bundle options

**File**: `SoComputeFunctionMain.java:43, 70`
```java
SoEventG soEventG; // 共用商品合購價
```

**Implementation**: `SoEventG.java`
**Characteristics**:
- Complex bundle with shared components
- Item can participate in multiple bundle calculations
- Special shared item handling

**Test Coverage**: `SoEventG_IntegrationTest.java`, `SoEventG_UnitTest.java`

---

### Event H: 單品拆價合購價 (Single Item Split-Price Bundle)

**Business Logic**: Single SKU gets split pricing when purchased in bundle

**File**: `SoComputeFunctionMain.java:44, 71`
```java
SoEventH soEventH; // 單品拆價合購價
```

**Implementation**: `SoEventH.java`
**Characteristics**:
- Price allocation within single SKU
- Bundle participation with price splitting
- Complex discount distribution

---

## Data Flow Analysis

### Phase 1: Promotion Data Synchronization (OMS → SOM)

#### 1.1 OMS Data Source

**External Database Link**: `bs_sku_store@oms`

**Key Fields**:
- `MP_EVENT`: Multi-promotion event number (becomes `EVENT_NOSP` in SOM)
- `EVENT_NO`: Single-item promotion number
- `MODIFY_TIME`: Last modification timestamp
- `MW_TIME`: Middleware sync timestamp

#### 1.2 Batch Job: StoreSkuPriceReceiver

**File**: `C:\Projects\som\so-batchjob\src\main\java\com\trihome\som\so\dataexchange\service\StoreSkuPriceReceiver.java`

**Execution Schedule**: Daily, after 9:00 AM, waits for OMS price updates

**Code Location**: Lines 93-115
```java
@Scheduled(cron = "0 */10 9-23 * * ?")  // Every 10 minutes from 9:00 to 23:59
@Async
public void start() {
    // Check if OMS price updates are complete
    int checkSkuStoreCount = customOmsBsSkuStoreMapper.checkOmsStorePrice();
    int checkAvgCostCount = customOmsBsSkuStoreMapper.checkOmsAvgCost();

    if (checkSkuStoreCount == 0 && checkAvgCostCount >= 1) {
        // OMS update complete, proceed with sync
        String storeId = getStoreId(i);
        storeRecord = customOmsBsSkuStoreMapper.updateBsSkuStore(storeId);

        logger.info("Store " + storeId + " sync: " + storeRecord + " records");
    }
}
```

**Waiting Mechanism**:
- Checks OMS status every 10 minutes
- Only syncs when OMS confirms price updates are complete
- Prevents syncing stale or incomplete data

#### 1.3 MERGE SQL Statement

**File**: `C:\Projects\som\so-batchjob\src\main\resources\sqlMap\CustomOmsBsSkuStoreMapper.xml`

**Code Location**: Lines 4-93
```xml
<update id="updateBsSkuStore" parameterType="String">
    MERGE INTO TBL_SKU_STORE tss
    USING (
        SELECT
            STORE_ID AS storeId,
            SKU AS sku,
            EVENT_NO AS eventNo,
            MP_EVENT AS mpEvent,              -- ★ This becomes PROM_EVENT_NO
            POS_AMT AS posAmt,
            AVG_COST AS avgCost,
            ...
        FROM bs_sku_store@oms
        WHERE (MODIFY_TIME >= TRUNC(SYSDATE-2)
               OR MW_TIME >= TRUNC(SYSDATE-2))
          AND STORE_ID = #{storeId}
    ) bssku
    ON (tss.STORE_ID = bssku.storeId AND tss.SKU_NO = bssku.sku)
    WHEN MATCHED THEN UPDATE SET
        tss.EVENT_NO = bssku.eventNo,
        tss.PROM_EVENT_NO = bssku.mpEvent,    -- ★ One event per SKU
        tss.POS_AMT = bssku.posAmt,
        tss.AVG_COST = bssku.avgCost,
        ...
    WHEN NOT MATCHED THEN INSERT (
        STORE_ID, SKU_NO, EVENT_NO, PROM_EVENT_NO, ...
    ) VALUES (
        bssku.storeId, bssku.sku, bssku.eventNo, bssku.mpEvent, ...
    )
</update>
```

**Key Insight**: `PROM_EVENT_NO` column can only store **ONE** value per SKU. If OMS changes the promotion, the old value is overwritten.

---

### Phase 2: Frontend SKU Retrieval

#### 2.1 SKU Query with Promotion Info

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\service\BzSkuInfoServices.java`

**Code Location**: Lines 138-144
```java
public SkuInfoVO qerySkuInfo(String skuNo, String storeId, ...) {
    SkuInfoVO skuInfoVO = customSkuInfoOpenCloseMapper.selectSkuInfo(
        skuNo, storeId
    );

    // Returns SKU with promotion info
    // skuInfoVO.promEventNo = TBL_SKU_STORE.PROM_EVENT_NO
    // skuInfoVO.promEventType = TBL_PROM_EVENT.EVENT_TYPE

    return skuInfoVO;
}
```

#### 2.2 SQL Join with Promotion Table

**File**: `C:\Projects\som\so-bzservices\src\main\resources\sqlMap\CustomSkuInfoOpenCloseMapper.xml`

**Code Location**: Lines 163-246
```xml
<select id="selectSkuInfo" resultType="SkuInfoVO">
    SELECT
        tss.SKU_NO AS skuNo,
        tss.POS_AMT AS posAmt,
        tss.PROM_EVENT_NO AS promEventNo,      -- ★ From TBL_SKU_STORE
        tpe.EVENT_TYPE AS promEventType,       -- ★ Joined from TBL_PROM_EVENT
        tpe.EVENT_NAME AS promEventName,
        tss.AVG_COST AS avgCost,
        ...
    FROM TBL_SKU_STORE tss
    LEFT JOIN TBL_PROM_EVENT tpe
        ON tss.PROM_EVENT_NO = tpe.EVENT_NO    -- ★ Join to get event type
    WHERE tss.SKU_NO = #{skuNo}
      AND tss.STORE_ID = #{storeId}
</select>
```

**Result**: Frontend receives SKU with its assigned promotion event number and type (A-H)

#### 2.3 Frontend Assignment

**File**: `C:\Projects\som\so-webapp\src\main\webapp\WEB-INF\views\so\commonpage\soSKUSubPage.jsp`

**Code Location**: Lines 626-735
```javascript
function getSkuInfo(skuNo) {
    $.ajax({
        url: '/so/getSkuInfo',
        data: { skuNo: skuNo, storeId: storeId },
        success: function(jsonData) {
            var selSkuIdx = lstSkuInfo.length;

            // Assign promotion event info from backend
            lstSkuInfo[selSkuIdx].eventNosp = jsonData.promEventNo;
            lstSkuInfo[selSkuIdx].promEventNo = jsonData.promEventNo;
            lstSkuInfo[selSkuIdx].promEventType = jsonData.promEventType; // A-H

            // Other SKU properties
            lstSkuInfo[selSkuIdx].skuNo = jsonData.skuNo;
            lstSkuInfo[selSkuIdx].posAmt = jsonData.posAmt;
            lstSkuInfo[selSkuIdx].unitCost = jsonData.avgCost;
            ...
        }
    });
}
```

**Key Fields in OrderDetlVO**:
- `eventNosp`: Promotion event number (e.g., "MT22046216")
- `evnetType`: Event type A-H (set later in backend)
- `promEventNo`: Same as eventNosp (duplicate for compatibility)

---

### Phase 3: Event Type Assignment and Classification

#### 3.1 Event Type Lookup

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\service\SoComputeFunctionMainServices.java`

**Code Location**: Lines 44-66
```java
public SoComputeFunctionMain getSoComputeFunctionMain(
    ArrayList<OrderDetlVO> items, boolean zeroTax
) {
    SoComputeFunctionMain soComputeFunctionMain =
        new SoComputeFunctionMain(bzSoServices, zeroTax);

    // ★ Set event type (A-H) for each item
    this.setOrderDtlsEventType(items);

    // Initialize event calculators
    soComputeFunctionMain.init(items);

    return soComputeFunctionMain;
}

/**
 * 設定訂單商品於組合促銷內的促銷方案
 * Set promotion plan for order items in combo promotions
 */
public void setOrderDtlsEventType(ArrayList<OrderDetlVO> items) {
    for (OrderDetlVO orderDetlVO : items) {
        if (StringUtils.isNotBlank(orderDetlVO.getEventNosp())) {
            logger.info("商品:" + orderDetlVO.getSkuNo() +
                       " ,EVENT_NOSP: " + orderDetlVO.getEventNosp());

            // Query TBL_PROM_EVENT for event type
            TblPromEvent promEvent = bzSoServices.queryPromEvent(
                orderDetlVO.getEventNosp()
            );

            if (promEvent != null) {
                // Set A-H event type
                orderDetlVO.setEvnetType(promEvent.getEventType());
            }
        }
    }
}
```

#### 3.2 Promotion Event Query

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\service\BzSoServices.java`

**Code Location**: Lines 770-781
```java
/**
 * 取得 多重促銷主檔
 * Get multi-promotion master data
 *
 * @param promEventNo - Event number (e.g., "MT22046216")
 * @return TblPromEvent with EVENT_TYPE (A-H)
 */
public TblPromEvent queryPromEvent(String promEventNo) {
    logger.info("queryPromEvent start ..");

    TblPromEventCriteria criteria = new TblPromEventCriteria();
    criteria.createCriteria().andEventNoEqualTo(promEventNo);

    List<TblPromEvent> promEvent = tblPromEventMapper.selectByCriteria(criteria);

    if (promEvent.isEmpty()) {
        return null;
    }

    logger.info("queryPromEvent end ..");
    return promEvent.get(0);
}
```

**TBL_PROM_EVENT Structure**:
```java
public class TblPromEvent {
    private String eventNo;       // EVENT_NO: "MT22046216"
    private String eventType;     // EVENT_TYPE: "A", "B", "C", ... "H"
    private String eventName;     // EVENT_NAME: "春季促銷"
    private Date startDat;        // START_DAT: 2024-03-01
    private Date endDat;          // END_DAT: 2024-03-31
    private String invDesc;       // INV_DESC: "春季促銷優惠"
    private Short groupCnt;       // GROUP_CNT: Number of groups
}
```

---

### Phase 4: Event Classification and Initialization

#### 4.1 Classification by Event Type

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\functions\SoComputeFunctionMain.java`

**Code Location**: Lines 78-116
```java
/**
 * 初始化，將所有商品依據促銷種類分群
 * Initialize, classify all items by promotion type
 *
 * @param items - All order items
 */
public void init(ArrayList<OrderDetlVO> items) {
    // Step 1: Separate by goods type (P, I, DD, etc.)
    assortGoodType(items);

    // Step 2: Classify by event type A-H
    assortEventNo(skuItems);           // Product items
    assortEventNo(installSkuItems);    // Installation items
    assortEventNo(deliverSkuItems);    // Delivery items

    // Step 3: Initialize events in A→B→C→D→E→F→G→H order
    if (soEventItemsA.size() != 0) {
        stampErrorMsg = soEventA.init(soEventItemsA, lstOrderEventMsgVO);
    }
    if (soEventItemsB.size() != 0) {
        long total = 0; // Calculate invoice total for Event B
        for (OrderDetlVO orderDetlVO : items) {
            int posAmt = Integer.parseInt(orderDetlVO.getPosAmt());
            int qty = Integer.parseInt(orderDetlVO.getQuantity());
            total += posAmt * qty;
        }
        soEventB.init(soEventItemsB, total, lstOrderEventMsgVO);
    }
    if (soEventItemsC.size() != 0) {
        soEventC.init(soEventItemsC, lstOrderEventMsgVO);
    }
    if (soEventItemsD.size() != 0) {
        soEventD.init(soEventItemsD, lstOrderEventMsgVO);
    }
    if (soEventItemsE.size() != 0) {
        soEventE.init(soEventItemsE, lstOrderEventMsgVO);
    }
    if (soEventItemsF.size() != 0) {
        soEventF.init(soEventItemsF, lstOrderEventMsgVO);
    }
    if (soEventItemsG.size() != 0) {
        soEventG.init(soEventItemsG, lstOrderEventMsgVO);
    }
    if (soEventItemsH.size() != 0) {
        soEventH.init(soEventItemsH, lstOrderEventMsgVO);
    }
}
```

**Event Item Buckets**:
```java
ArrayList<OrderDetlVO> soEventItemsA = new ArrayList<OrderDetlVO>(); // 印花價
ArrayList<OrderDetlVO> soEventItemsB = new ArrayList<OrderDetlVO>(); // 發票金額滿額加價購
ArrayList<OrderDetlVO> soEventItemsC = new ArrayList<OrderDetlVO>(); // 商品滿(金額/數量)以上,全面優惠
ArrayList<OrderDetlVO> soEventItemsD = new ArrayList<OrderDetlVO>(); // 商品每買M個，享其中N個優惠
ArrayList<OrderDetlVO> soEventItemsE = new ArrayList<OrderDetlVO>(); // 買條件商品A群組(數量/金額)可享B商品群組優惠
ArrayList<OrderDetlVO> soEventItemsF = new ArrayList<OrderDetlVO>(); // 合購價
ArrayList<OrderDetlVO> soEventItemsG = new ArrayList<OrderDetlVO>(); // 共用商品合購價
ArrayList<OrderDetlVO> soEventItemsH = new ArrayList<OrderDetlVO>(); // 單品拆價合購價
```

#### 4.2 Classification Logic

**Code Location**: Lines 150-179
```java
/**
 * 將商品依據促銷種類分類
 * Classify items by promotion type
 *
 * @param items - Items to classify (P, I, or DD items)
 */
private void assortEventNo(ArrayList<OrderDetlVO> items) {
    String eventType = StringUtils.EMPTY;

    for (OrderDetlVO orderDetlVO : items) {
        // Only process items with promotion AND not bonus points
        if (StringUtils.isNotBlank(orderDetlVO.getEventNosp()) &&
            (StringUtils.isBlank(orderDetlVO.getBonusTotal()) ||
             Integer.parseInt(orderDetlVO.getBonusTotal()) == 0)) {

            // Initialize discount fields
            orderDetlVO.setDiscountAmt("0");
            orderDetlVO.setDiscountQty("0");

            // Get event type A-H
            eventType = orderDetlVO.getEvnetType();

            // Route to appropriate bucket
            if ("A".equals(eventType) && orderDetlVO.isStampFlag()) {
                soEventItemsA.add(orderDetlVO);
            } else if ("B".equals(eventType)) {
                soEventItemsB.add(orderDetlVO);
            } else if ("C".equals(eventType)) {
                soEventItemsC.add(orderDetlVO);
            } else if ("D".equals(eventType)) {
                soEventItemsD.add(orderDetlVO);
            } else if ("E".equals(eventType)) {
                soEventItemsE.add(orderDetlVO);
            } else if ("F".equals(eventType)) {
                soEventItemsF.add(orderDetlVO);
            } else if ("G".equals(eventType)) {
                soEventItemsG.add(orderDetlVO);
            } else if ("H".equals(eventType)) {
                soEventItemsH.add(orderDetlVO);
            }
        }
    }
}
```

**Key Logic**:
1. **Bonus Points Exclusion**: Items with bonus points (`bonusTotal > 0`) cannot have event promotions
2. **Stamp Flag Requirement**: Event A requires special `stampFlag` validation
3. **Single Bucket Assignment**: Each item goes to exactly ONE event bucket based on `evnetType`
4. **No Conflict**: Since each item has only one `eventNosp`, no conflict resolution needed

---

### Phase 5: Discount Calculation and Retrieval

#### 5.1 Discount Retrieval Interface

**File**: `SoComputeFunctionMain.java`

**Code Location**: Lines 190-240
```java
/**
 * 回傳折扣數量
 * Return discount quantity
 *
 * @param discountType - Event type ("A", "B", "C", ... "H")
 * @param serialNo - Item sequence ID
 * @return String - Discount quantity
 */
public String getDiscQty(String discountType, String serialNo) {
    if (discountType.equals("A")) {
        return this.getDiscInfo(soEventA, serialNo, 0);
    } else if (discountType.equals("B")) {
        return this.getDiscInfo(soEventB, serialNo, 0);
    } else if (discountType.equals("C")) {
        return this.getDiscInfo(soEventC, serialNo, 0);
    } else if (discountType.equals("D")) {
        return this.getDiscInfo(soEventD, serialNo, 0);
    } else if (discountType.equals("E")) {
        return this.getDiscInfo(soEventE, serialNo, 0);
    } else if (discountType.equals("F")) {
        return this.getDiscInfo(soEventF, serialNo, 0);
    } else if (discountType.equals("G")) {
        return this.getDiscInfo(soEventG, serialNo, 0);
    } else if (discountType.equals("H")) {
        return this.getDiscInfo(soEventH, serialNo, 0);
    }
    return "0";
}

/**
 * 回傳折扣金額
 * Return discount amount
 */
public String getDiscAmt(String discountType, String serialNo) {
    if (discountType.equals("A")) {
        return this.getDiscInfo(soEventA, serialNo, 1);
    } else if (discountType.equals("B")) {
        return this.getDiscInfo(soEventB, serialNo, 1);
    }
    // ... C through H follow same pattern
    return "0";
}

/**
 * 得到促銷 type 之促銷資訊（數量、金額）
 * Get promotion info (quantity, amount) for promotion type
 *
 * @param soEventInterface - Event calculator instance
 * @param SerialNo - Item sequence ID
 * @param type - 0: discount quantity, 1: discount amount
 * @return String
 */
private String getDiscInfo(SoEvent soEventInterface, String SerialNo, int type) {
    switch (type) {
        case 0: // Discount quantity
            return (soEventInterface.getSerialDiscQtyMap().containsKey(SerialNo))
                ? soEventInterface.getSerialDiscQtyMap().get(SerialNo) + StringUtils.EMPTY
                : "0";
        case 1: // Discount amount
            return (soEventInterface.getSerialDiscAmtMap().containsKey(SerialNo))
                ? soEventInterface.getSerialDiscAmtMap().get(SerialNo) + StringUtils.EMPTY
                : "0";
        default:
            return "0";
    }
}
```

#### 5.2 Event Calculator Interface

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\functions\SoEvent.java`

Each event calculator (SoEventA through SoEventH) implements the `SoEvent` interface:

```java
public interface SoEvent {
    /**
     * Initialize event calculator with items
     */
    ArrayList<String> init(
        ArrayList<OrderDetlVO> items,
        ArrayList<OrderEventMsgVO> lstOrderEventMsgVO
    );

    /**
     * Get discount amount map
     * Key: detlSeqId (item sequence ID)
     * Value: Discount amount as String
     */
    HashMap<String, String> getSerialDiscAmtMap();

    /**
     * Get discount quantity map
     * Key: detlSeqId (item sequence ID)
     * Value: Discount quantity as String
     */
    HashMap<String, String> getSerialDiscQtyMap();
}
```

---

## Promotion Data Queries

### Query 1: Promotion Conditions

**File**: `BzSoServices.java`
**Code Location**: Lines 814-838

```java
/**
 * 取得 多重促銷條件設定檔
 * Get multi-promotion condition configuration
 *
 * @param promEventNo - Event number
 * @param seqNo - Sequence number (tier)
 * @param groupSeqNo - Group sequence number
 * @param condType - Condition type ("1": condition group, "2": discount group)
 * @return List<TblPromCondition>
 */
public List<TblPromCondition> queryPromCondition(
    String promEventNo,
    String seqNo,
    String groupSeqNo,
    String condType
) {
    logger.info("queryPromCondition start ..");

    TblPromConditionCriteria criteria = new TblPromConditionCriteria();
    Criteria crit = criteria.createCriteria();
    crit.andEventNoEqualTo(promEventNo);

    if (!"0".equals(seqNo)) {
        crit.andSeqNoEqualTo(seqNo);
    }
    if (!"0".equals(groupSeqNo)) {
        crit.andGroupSeqNoEqualTo(groupSeqNo);
    }
    if (!"0".equals(condType)) {
        crit.andCondTypeEqualTo(condType);
    }

    List<TblPromCondition> promCondition =
        tblPromConditionMapper.selectByCriteria(criteria);

    logger.info("queryPromCondition end ..");
    return promCondition;
}
```

**TBL_PROM_CONDITION Fields**:
- `EVENT_NO`: Promotion event number
- `SEQ_NO`: Tier sequence (for multi-tier promotions)
- `GROUP_SEQ_NO`: Group sequence within tier
- `COND_TYPE`: "1" = condition group, "2" = discount group
- `CONDITION_AMT`: Threshold amount or quantity
- `LIMIT_QTY`: Purchase quantity limit
- `HEAP`: "Y" = cumulative, "N" = one-time
- `ALLOCATION_TYPE`: Discount distribution method

---

### Query 2: Promotion SKU Sets

**File**: `BzSoServices.java`
**Code Location**: Lines 848-870

```java
/**
 * 取得 多重促銷商品群組檔
 * Get multi-promotion SKU group configuration
 *
 * @param promEventNo - Event number
 * @param seqNo - Sequence number
 * @param groupSeqNo - Group sequence number
 * @return List<TblPromSet>
 */
public List<TblPromSet> queryPromSet(
    String promEventNo,
    String seqNo,
    String groupSeqNo
) {
    logger.info("queryPromSet start ..");

    TblPromSetCriteria criteria = new TblPromSetCriteria();
    Criteria crit = criteria.createCriteria();
    crit.andEventNoEqualTo(promEventNo);

    if (!"0".equals(seqNo)) {
        crit.andSeqNoEqualTo(seqNo);
    }
    if (!"0".equals(groupSeqNo)) {
        crit.andGroupSeqNoEqualTo(groupSeqNo);
    }

    List<TblPromSet> promSet = tblPromSetMapper.selectByCriteria(criteria);

    logger.info("queryPromSet end ..");
    return promSet;
}
```

**TBL_PROM_SET Fields**:
- `EVENT_NO`: Promotion event number
- `SEQ_NO`: Tier sequence
- `GROUP_SEQ_NO`: Group sequence
- `SKU_NO`: SKU in this group
- `SET_TYPE`: "1" = condition item, "2" = discount item
- `DISCOUNT_TYPE`: "1" = actual price, "2" = percentage, "3" = fixed amount
- `AMT`: Discount value
- `LIMIT_QTY`: Quantity limit for this SKU

---

### Query 3: Promotion Groups

**File**: `BzSoServices.java`
**Code Location**: Lines 789-803

```java
/**
 * 取得 多重促銷級距(組)設定檔
 * Get multi-promotion tier (group) configuration
 *
 * @param promEventNo - Event number
 * @param seqNo - Sequence number ("0" = all tiers)
 * @return List<TblPromGroup>
 */
public List<TblPromGroup> queryPromGroup(String promEventNo, String seqNo) {
    logger.info("queryPromGroup start ..");

    TblPromGroupCriteria criteria = new TblPromGroupCriteria();
    Criteria crit = criteria.createCriteria();
    crit.andEventNoEqualTo(promEventNo);

    if (!"0".equals(seqNo)) {
        crit.andSeqNoEqualTo(seqNo);
    }

    List<TblPromGroup> promGroup = tblPromGroupMapper.selectByCriteria(criteria);

    if (promGroup.isEmpty()) {
        return null;
    }

    logger.info("queryPromGroup end ..");
    return promGroup;
}
```

**TBL_PROM_GROUP Fields**:
- `EVENT_NO`: Promotion event number
- `SEQ_NO`: Tier sequence (e.g., "1", "2", "3" for multi-tier)
- `GROUP_CNT`: Number of groups in this tier
- `COND_AMT_TYPE`: "1" = amount, "2" = quantity

---

## Integration with Pricing Engine

### Call Sequence in BzSoServices

**File**: `C:\Projects\som\so-bzservices\src\main\java\com\trihome\som\bz\service\BzSoServices.java`

**Code Location**: Lines 4434-4478

```java
// ============================================
// PRICING ENGINE EXECUTION ORDER
// ============================================

// Step 1: Calculate free installation total
ArrayList<OrderDetlVO> lstFreeInstallSku = assortSku.getLstFreeInstallSku();
for (OrderDetlVO orderDetlVO : lstFreeInstallSku) {
    totalAmtFI += Integer.parseInt(orderDetlVO.getActInstallPrice());
}
soVO.setTotalAmtIhasFI(totalAmtFI + StringUtils.EMPTY);

// Step 2: Member Discount Type 2 (Cost Markup)
if (!lstComputeSku.isEmpty()) {
    memberDiscSkus.addAll(
        soFunctionMemberDisServices.soComputeFunctionMemberDis(
            lstComputeSku, soBO.getMemberCardId(), channelId, "2", isTaxZero
        )
    );

    if (!memberDiscSkus.isEmpty()) {
        assortSku = new AssortSku(lstAllSku, lstWorkTypeSku);
        lstComputeSku = assortSku.getLstComputeSku();
        lstGoodsSku = assortSku.getLstGoodsSku();
    }
}

// Step 3: Multi-Promotion Event Calculation (A-H)
soVO.setStampErrorMsg(new ArrayList<String>());
soVO.setLstOrderEventMsgVO(new ArrayList<OrderEventMsgVO>());

if (!lstComputeSku.isEmpty()) {
    // ★ This is where Event A-H promotions are calculated
    SoComputeFunctionMain soComputeFunctionMain =
        soComputeFunctionMainServices.getSoComputeFunctionMain(lstComputeSku, isTaxZero);

    // Get stamp price error messages (Event A)
    soVO.setStampErrorMsg(soComputeFunctionMain.getStampErrorMsg());

    // Get promotion description messages (all events)
    soVO.setLstOrderEventMsgVO(soComputeFunctionMain.getOrderEventMsgVO());

    // Step 4: Member Discount Type 0 (Discounting)
    memberDiscSkus.addAll(
        soFunctionMemberDisServices.soComputeFunctionMemberDis(
            lstComputeSku, soBO.getMemberCardId(), channelId, "0", isTaxZero
        )
    );

    // Step 5: Member Discount Type 1 (Down Margin)
    memberDiscSkus.addAll(
        soFunctionMemberDisServices.soComputeFunctionMemberDis(
            lstComputeSku, soBO.getMemberCardId(), channelId, "1", isTaxZero
        )
    );

    // Step 6: Special Member Discount (Type CT - Fallback)
    if (memberDiscSkus.isEmpty()) {
        memberDiscSkus.addAll(
            soFunctionMemberDisServices.soComputeMemberDisForSpecial(
                lstComputeSku, soBO.getMemberCardId(), channelId, isTaxZero
            )
        );
    }

    // Set member discount results
    if (!memberDiscSkus.isEmpty() || !soBO.isO2oTypeD() || !"APP".equals(soBO.getSystemFlag())) {
        soVO.setLstMemberDiscount(memberDiscSkus);
        for (MemberDiscVO memberDiscVO : memberDiscSkus) {
            // Only Type 0 (Discounting) adds to total
            if ("0".equals(memberDiscVO.getDiscType())) {
                totalMemberDisc += Integer.parseInt(memberDiscVO.getDiscAmt());
            }
        }
    }
}
```

**Execution Order**:
1. Free Installation Calculation
2. Member Discount Type 2 (Cost Markup)
3. **Multi-Promotion Events A-H** ← Current focus
4. Member Discount Type 0 (Discounting)
5. Member Discount Type 1 (Down Margin)
6. Special Member Discount Type CT (if no Type 0/1/2 match)

**Key Insight**: Event promotions are calculated **AFTER** Type 2 member discounts but **BEFORE** Type 0/1/CT member discounts.

---

## Business Rules

### PROM-R1: Single Promotion Per SKU

**Rule**: Each SKU can have only ONE active multi-promotion event (`PROM_EVENT_NO`) at any time.

**Database Constraint**:
- `TBL_SKU_STORE.PROM_EVENT_NO` is a single VARCHAR field (not a list)
- MERGE statement overwrites previous value

**Code Evidence**: `CustomOmsBsSkuStoreMapper.xml:43`
```sql
WHEN MATCHED THEN UPDATE SET
    tss.PROM_EVENT_NO = bssku.mpEvent  -- Overwrites previous value
```

**Impact**:
- No priority resolution needed in SOM
- OMS must decide which promotion applies
- Changing promotion requires OMS update + batch sync

---

### PROM-R2: OMS Authority

**Rule**: The OMS system has **sole authority** to assign promotions to SKUs. SOM cannot override or modify promotion assignments.

**Code Evidence**:
- **Data Source**: `bs_sku_store@oms` (external database link)
- **Read-Only**: SOM only reads `MP_EVENT`, never updates it
- **One-Way Sync**: Daily batch imports OMS decisions

**Business Impact**:
- SOM cannot create ad-hoc promotions
- Promotion changes require OMS configuration
- Sync delay: Up to 24 hours for promotion changes

---

### PROM-R3: Event Type Immutability

**Rule**: Once a promotion event number is assigned to an item in an order, its event type (A-H) cannot change during order processing.

**Code Evidence**: `SoComputeFunctionMainServices.java:56-66`
```java
public void setOrderDtlsEventType(ArrayList<OrderDetlVO> items) {
    // Queries TBL_PROM_EVENT once during order calculation
    // Event type is set and never rechecked
    orderDetlVO.setEvnetType(promEvent.getEventType());
}
```

**Impact**:
- Event type is snapshot at calculation time
- Even if TBL_PROM_EVENT changes, existing order items unchanged
- Recalculation requires requerying

---

### PROM-R4: Bonus Points Mutual Exclusion

**Rule**: Items with bonus points (`bonusTotal > 0`) **cannot** participate in multi-promotion events.

**Code Evidence**: `SoComputeFunctionMain.java:153-156`
```java
if (StringUtils.isNotBlank(orderDetlVO.getEventNosp()) &&
    (StringUtils.isBlank(orderDetlVO.getBonusTotal()) ||
     Integer.parseInt(orderDetlVO.getBonusTotal()) == 0)) {
    // Only process if bonusTotal is blank or 0
}
```

**Business Logic**:
- Bonus points and event promotions are mutually exclusive
- If `bonusTotal > 0`, item skips event processing
- This prevents "double dipping" (bonus + promotion)

---

### PROM-R5: Stamp Flag Requirement for Event A

**Rule**: Event A (Stamp Price) requires items to have `stampFlag = true`.

**Code Evidence**: `SoComputeFunctionMain.java:160-161`
```java
if ("A".equals(eventType) && orderDetlVO.isStampFlag()) {
    soEventItemsA.add(orderDetlVO);
}
```

**Business Logic**:
- Not all items with Event A automatically qualify
- Must also have special stamp indicator
- Likely controlled by merchandising rules

---

### PROM-R6: Processing Order ≠ Priority Order

**Rule**: The A→B→C→D→E→F→G→H processing order is **NOT** a priority ranking. It is simply the **initialization sequence** for event calculators.

**Code Evidence**: `SoComputeFunctionMain.java:84-115`
```java
// Events initialized in sequence A→H
// But each item is already assigned to ONE event
// No competition or priority resolution happens
```

**Why This Matters**:
- Developers might assume A has "highest priority"
- **FALSE**: Items are pre-assigned to one event by OMS
- Processing order only affects calculation sequence
- No event "wins" over another (no conflict exists)

---

### PROM-R7: Goods Type Separation

**Rule**: Promotion calculations are separated by goods type: Product (P), Installation (I/IA/IE/IC/IS), Delivery (DD).

**Code Evidence**: `SoComputeFunctionMain.java:78-82`
```java
assortGoodType(items);           // Separate by P, I, DD
assortEventNo(skuItems);         // Process product items
assortEventNo(installSkuItems);  // Process installation items
assortEventNo(deliverSkuItems);  // Process delivery items
```

**Business Logic**:
- Each goods type calculated independently
- Installation items can have different event than parent product
- Delivery items processed separately

---

### PROM-R8: Daily Sync Window

**Rule**: Promotion data syncs daily after 9:00 AM, waiting for OMS price updates to complete.

**Code Evidence**: `StoreSkuPriceReceiver.java:93-115`
```java
@Scheduled(cron = "0 */10 9-23 * * ?")  // Every 10 min, 9:00-23:59
public void start() {
    // Check if OMS updates complete
    int checkSkuStoreCount = customOmsBsSkuStoreMapper.checkOmsStorePrice();

    if (checkSkuStoreCount == 0) {
        // OMS ready, proceed with sync
        updateBsSkuStore(storeId);
    }
}
```

**Business Impact**:
- Promotion changes in OMS may take up to 24 hours to appear in SOM
- Intraday promotion changes not supported
- Stores should not rely on immediate promotion updates

---

## Critical Issues and Recommendations

### 🚨 CRITICAL: No SOM-Side Priority Resolution

**Issue**: The SOM system has **zero capability** to resolve conflicts when multiple promotions could apply to the same SKU.

**Current Behavior**:
- OMS assigns ONE promotion via `MP_EVENT` field
- SOM blindly accepts OMS decision
- No validation or conflict detection

**Risk Scenarios**:

1. **OMS Bug**: If OMS assigns wrong promotion, SOM has no way to detect or correct
2. **Manual Override**: Store staff cannot manually switch promotions
3. **Edge Cases**: Time-sensitive promotions (ending at midnight) might use stale data

**Recommendation**:
- **Document OMS priority rules** in business requirements
- **Add logging** to track which promotions were "rejected" by OMS
- **Create dashboard** showing promotion conflicts (for business analysis)
- **Consider fallback**: If OMS `MP_EVENT` is invalid/expired, should SOM warn or block order?

---

### ⚠️ HIGH: Processing Order Confusion

**Issue**: The A→H initialization order is easily confused with priority order.

**Code Location**: `SoComputeFunctionMain.java:84-115`

**Current Naming**:
```java
// This looks like priority order but ISN'T
if (soEventItemsA.size() != 0) {
    soEventA.init(...);  // "A processed first" ≠ "A has priority"
}
if (soEventItemsB.size() != 0) {
    soEventB.init(...);
}
```

**Risk**:
- Future developers may assume A > B > C priority
- Business may request "prioritize Event C over Event A"
- Code maintenance confusion

**Recommendation**:
- **Add comment** explaining this is NOT priority order
- **Rename method**: `init()` → `calculateDiscounts()`
- **Document in spec**: "Promotion priority is determined by OMS, not SOM processing order"

**Proposed Code Comment**:
```java
// ============================================
// IMPORTANT: Processing order A→H is NOT priority order!
// Each item is already assigned to ONE event by OMS.
// This loop simply initializes calculators for assigned events.
// Priority/conflict resolution happens in OMS, not here.
// ============================================
if (soEventItemsA.size() != 0) {
    soEventA.init(soEventItemsA, lstOrderEventMsgVO);
}
```

---

### ⚠️ MEDIUM: Stale Promotion Data Window

**Issue**: Up to 24-hour delay between OMS promotion changes and SOM visibility.

**Scenario**:
1. Marketing creates new promotion in OMS at 10:00 AM
2. SOM last synced at 9:30 AM (before promotion existed)
3. Next sync at 9:30 AM next day
4. **Delay**: 23.5 hours

**Business Impact**:
- Flash sales cannot start immediately
- Promotion end times have up to 24-hour drift
- Customer complaints about "promotion not working"

**Recommendation**:
- **Add manual sync trigger** for urgent promotions
- **Display sync timestamp** in store POS ("Promotions updated: 2024-10-27 09:30")
- **Create alert** if sync fails or is delayed >2 hours

---

### ⚠️ MEDIUM: No Promotion Expiry Validation

**Issue**: SOM does not validate if promotion is expired at order creation time.

**Code Gap**: `setOrderDtlsEventType()` queries `TBL_PROM_EVENT` but does not check `START_DAT`/`END_DAT`.

**Risk Scenario**:
1. SKU has `PROM_EVENT_NO = "MT22046216"` from yesterday's batch
2. Promotion expired at midnight (END_DAT = 2024-10-26)
3. Customer creates order at 10:00 AM on 2024-10-27
4. **SOM still applies expired promotion** (because batch hasn't run yet)

**Current Code** (Lines 56-66):
```java
TblPromEvent promEvent = bzSoServices.queryPromEvent(orderDetlVO.getEventNosp());
if (promEvent != null) {
    // ❌ No date validation!
    orderDetlVO.setEvnetType(promEvent.getEventType());
}
```

**Recommendation**:
```java
TblPromEvent promEvent = bzSoServices.queryPromEvent(orderDetlVO.getEventNosp());
if (promEvent != null) {
    // ✅ Add date validation
    Date now = new Date();
    if (now.compareTo(promEvent.getStartDat()) >= 0 &&
        now.compareTo(promEvent.getEndDat()) <= 0) {
        orderDetlVO.setEvnetType(promEvent.getEventType());
    } else {
        logger.warn("Promotion expired: " + promEvent.getEventNo() +
                   " (END_DAT: " + promEvent.getEndDat() + ")");
        // Clear promotion assignment
        orderDetlVO.setEventNosp(null);
    }
}
```

---

### ⚠️ LOW: Event Type Typo

**Issue**: Field name `evnetType` should be `eventType` (missing 'e').

**Code Evidence**: Throughout codebase
```java
orderDetlVO.setEvnetType(promEvent.getEventType());  // Typo
String eventType = orderDetlVO.getEvnetType();        // Typo
```

**Impact**:
- Low (functional code works fine)
- Code readability reduced
- Auto-complete confusion

**Recommendation**:
- **Fix in rewrite**: Use correct spelling `eventType`
- **Document in migration guide**: "Legacy field `evnetType` renamed to `eventType`"

---

## Test Scenarios

### Scenario 1: Event G Integration Test

**Test File**: `SoEventG_IntegrationTest.java`

**Test Case**: Shared item bundle discount with Type 2 calculation

**Code Location**: Lines 64-110
```java
@Test
public void discountType2_c1() {
    eventNo = "MT22046216";
    discountType = "2";
    arrangeEventData();

    // Condition SKU
    OrderDetlVO sku = new OrderDetlVO();
    sku.setEventNosp(eventNo);
    sku.setSkuNo(condition_sku_747);
    sku.setQuantity("1");
    sku.setPosAmt("3880");
    sku.setDetlSeqId("001");
    addItem(sku);

    // Discount SKU 1
    sku = new OrderDetlVO();
    sku.setEventNosp(eventNo);
    sku.setSkuNo(discount_sku_575);
    sku.setQuantity("3");
    sku.setPosAmt("390");
    sku.setDetlSeqId("002");
    addItem(sku);

    // Discount SKU 2
    sku = new OrderDetlVO();
    sku.setEventNosp(eventNo);
    sku.setSkuNo(discount_sku_672);
    sku.setQuantity("1");
    sku.setPosAmt("1280");
    sku.setDetlSeqId("003");
    addItem(sku);

    executeTest();

    // Assert discount amounts
    assertEquals("-1476", skuMap.get(condition_sku_747).getDiscountAmt());
    assertEquals("-660", skuMap.get(discount_sku_575).getDiscountAmt());
    assertEquals("-0", skuMap.get(discount_sku_672).getDiscountAmt());
}
```

**Key Insights**:
- All items share same `eventNosp = "MT22046216"`
- Event G Type 2 calculation uses shared item logic
- Discount distributed across condition and discount items

---

### Scenario 2: Event Assignment from OMS

**Test Scenario**: Verify promotion data flows from OMS to SOM

**Setup**:
1. Create promotion in OMS: `MP_EVENT = "MT24100001"`, `EVENT_TYPE = "C"`
2. Assign to SKU: `123456789` in store `001`
3. Update `bs_sku_store@oms.MP_EVENT = "MT24100001"`

**Expected Batch Behavior**:
```sql
-- StoreSkuPriceReceiver executes daily at 9:00+ AM
MERGE INTO TBL_SKU_STORE tss
USING (SELECT MP_EVENT FROM bs_sku_store@oms WHERE ...) bssku
WHEN MATCHED THEN UPDATE SET
    tss.PROM_EVENT_NO = 'MT24100001'  -- Updated
```

**Expected Frontend Behavior**:
1. User scans SKU `123456789`
2. `BzSkuInfoServices.qerySkuInfo()` returns `promEventNo = "MT24100001"`
3. JavaScript sets `lstSkuInfo[0].eventNosp = "MT24100001"`

**Expected Pricing Engine Behavior**:
1. `setOrderDtlsEventType()` queries `TBL_PROM_EVENT` WHERE `EVENT_NO = "MT24100001"`
2. Returns `EVENT_TYPE = "C"`
3. Item routed to `soEventItemsC` bucket
4. `SoEventC.init()` calculates discount

**Validation**:
```sql
-- 1. Check batch sync result
SELECT SKU_NO, PROM_EVENT_NO
FROM TBL_SKU_STORE
WHERE SKU_NO = '123456789' AND STORE_ID = '001';
-- Expected: PROM_EVENT_NO = 'MT24100001'

-- 2. Check event type
SELECT EVENT_NO, EVENT_TYPE
FROM TBL_PROM_EVENT
WHERE EVENT_NO = 'MT24100001';
-- Expected: EVENT_TYPE = 'C'

-- 3. Check order item assignment
SELECT DETL_SEQ_ID, SKU_NO, EVENT_NOSP
FROM TBL_ORDER_DETL
WHERE SO_NUMBER = '[test_order_number]';
-- Expected: EVENT_NOSP = 'MT24100001'
```

---

### Scenario 3: Multiple Events Conflict (OMS Decides)

**Test Scenario**: SKU matches multiple promotions in OMS - verify OMS priority logic

**Setup in OMS**:
- **Promotion A**: Event Type A (Stamp Price) - 10% off
- **Promotion B**: Event Type C (Full Discount) - 15% off
- **SKU**: `987654321` matches BOTH promotions

**OMS Priority Rule** (hypothetical - needs business confirmation):
- Rule: Higher discount % wins
- Result: OMS assigns `MP_EVENT = [Promotion B]` (15% > 10%)

**Expected SOM Behavior**:
```sql
-- Batch sync from OMS
SELECT MP_EVENT FROM bs_sku_store@oms
WHERE SKU = '987654321' AND STORE_ID = '001';
-- Returns: MP_EVENT = [Promotion B Event Number]

-- SOM database after batch
SELECT PROM_EVENT_NO FROM TBL_SKU_STORE
WHERE SKU_NO = '987654321' AND STORE_ID = '001';
-- Returns: PROM_EVENT_NO = [Promotion B Event Number]

-- Event type lookup
SELECT EVENT_TYPE FROM TBL_PROM_EVENT
WHERE EVENT_NO = [Promotion B Event Number];
-- Returns: EVENT_TYPE = 'C'
```

**SOM Processing**:
1. Item assigned to `soEventItemsC` (Event C)
2. Event A calculator never sees this item
3. 15% discount applied via Event C logic

**Business Validation Needed**:
- **Confirm OMS priority rules**: Highest %? Newest? Manual priority field?
- **Document edge cases**: What if both 15%? What if time-overlapping promotions?
- **Create test matrix**: All A-H combinations vs conflict scenarios

---

### Scenario 4: Bonus Points Blocks Promotion

**Test Scenario**: Item with bonus points cannot use event promotion

**Setup**:
```java
OrderDetlVO item = new OrderDetlVO();
item.setSkuNo("111222333");
item.setEventNosp("MT24100002");  // Has promotion assignment
item.setBonusTotal("500");         // ★ Has 500 bonus points

// Add to order items
items.add(item);

// Execute promotion engine
SoComputeFunctionMain engine =
    soComputeFunctionMainServices.getSoComputeFunctionMain(items, false);
```

**Expected Behavior**:
```java
// In assortEventNo() method:
if (StringUtils.isNotBlank(orderDetlVO.getEventNosp()) &&
    (StringUtils.isBlank(orderDetlVO.getBonusTotal()) ||
     Integer.parseInt(orderDetlVO.getBonusTotal()) == 0)) {
    // ❌ Condition FAILS because bonusTotal = "500"
    // Item NOT added to any event bucket
}
```

**Result**:
- Item excluded from all event calculations
- No discount applied from promotion
- Bonus points still awarded (500 points)
- `discountAmt = "0"`, `discountQty = "0"`

**Business Rule**: Bonus points and event promotions are mutually exclusive.

---

### Scenario 5: Expired Promotion Still Applied (Bug)

**Test Scenario**: Batch hasn't run, expired promotion still in `TBL_SKU_STORE`

**Setup**:
```sql
-- Promotion expired yesterday
INSERT INTO TBL_PROM_EVENT VALUES (
    'MT24100003',  -- EVENT_NO
    'D',           -- EVENT_TYPE
    '週年慶',      -- EVENT_NAME
    DATE '2024-10-20',  -- START_DAT
    DATE '2024-10-26',  -- END_DAT ← Expired!
    '週年慶優惠',  -- INV_DESC
    2              -- GROUP_CNT
);

-- SKU still has expired promotion (batch not run yet)
UPDATE TBL_SKU_STORE
SET PROM_EVENT_NO = 'MT24100003'
WHERE SKU_NO = '555666777' AND STORE_ID = '001';
```

**Current Behavior (2024-10-27 10:00 AM)**:
```java
// User creates order with SKU 555666777
OrderDetlVO item = new OrderDetlVO();
item.setEventNosp("MT24100003");  // From TBL_SKU_STORE

// setOrderDtlsEventType() executes
TblPromEvent promEvent = bzSoServices.queryPromEvent("MT24100003");
if (promEvent != null) {
    // ❌ No date validation!
    item.setEvnetType("D");  // Sets to Event D
}

// ❌ EXPIRED PROMOTION STILL APPLIED!
```

**Expected Behavior** (after fix):
```java
TblPromEvent promEvent = bzSoServices.queryPromEvent("MT24100003");
if (promEvent != null) {
    Date now = new Date();  // 2024-10-27 10:00
    Date endDat = promEvent.getEndDat();  // 2024-10-26 23:59

    if (now.compareTo(endDat) > 0) {
        // ✅ Promotion expired
        logger.warn("Promotion expired: MT24100003");
        item.setEventNosp(null);  // Clear assignment
        // Discount NOT applied
    }
}
```

**Business Impact**:
- Customers may get discounts on expired promotions
- Revenue leakage
- Compliance risk (false advertising)

**Recommendation**: Implement date validation in `setOrderDtlsEventType()` method.

---

## Database Schema

### TBL_PROM_EVENT (Promotion Event Master)

**Purpose**: Stores promotion event metadata and type classification

```sql
CREATE TABLE TBL_PROM_EVENT (
    EVENT_NO     VARCHAR2(20) PRIMARY KEY,  -- Event number (e.g., "MT24100001")
    EVENT_TYPE   VARCHAR2(1) NOT NULL,      -- Event type: A, B, C, D, E, F, G, H
    EVENT_NAME   VARCHAR2(100),             -- Event name (e.g., "春季促銷")
    START_DAT    DATE NOT NULL,             -- Promotion start date
    END_DAT      DATE NOT NULL,             -- Promotion end date
    INV_DESC     VARCHAR2(200),             -- Invoice description
    GROUP_CNT    NUMBER(2)                  -- Number of groups in event
);

-- Index for event type queries
CREATE INDEX IDX_PROM_EVENT_TYPE ON TBL_PROM_EVENT(EVENT_TYPE);
```

**Event Type Values**:
- `A`: 印花價 (Stamp Price)
- `B`: 發票金額滿額加價購 (Invoice Amount Threshold Add-on)
- `C`: 商品滿(金額/數量)以上,全面優惠 (Full Discount When Threshold Met)
- `D`: 商品每買M個，享其中N個優惠 (Buy M Get N Discounted)
- `E`: 買條件商品A群組可享B商品群組優惠 (Buy Group A Get Group B Discount)
- `F`: 合購價 (Bundle Price)
- `G`: 共用商品合購價 (Shared Item Bundle Price)
- `H`: 單品拆價合購價 (Single Item Split-Price Bundle)

---

### TBL_SKU_STORE (SKU Store Master)

**Purpose**: Stores SKU info per store including promotion assignment

```sql
CREATE TABLE TBL_SKU_STORE (
    STORE_ID        VARCHAR2(10) NOT NULL,
    SKU_NO          VARCHAR2(13) NOT NULL,
    EVENT_NO        VARCHAR2(20),           -- Single-item promotion
    PROM_EVENT_NO   VARCHAR2(20),           -- Multi-promotion (EVENT_NOSP)
    POS_AMT         NUMBER(10) NOT NULL,    -- POS price
    AVG_COST        NUMBER(10,2),           -- Average cost
    MODIFY_TIME     DATE,
    PRIMARY KEY (STORE_ID, SKU_NO)
);

-- Foreign key to promotion event (optional - may not exist in legacy)
-- ALTER TABLE TBL_SKU_STORE
-- ADD CONSTRAINT FK_SKU_PROM_EVENT
-- FOREIGN KEY (PROM_EVENT_NO) REFERENCES TBL_PROM_EVENT(EVENT_NO);
```

**Key Field**: `PROM_EVENT_NO`
- **Source**: `bs_sku_store@oms.MP_EVENT` (via daily batch)
- **Constraint**: Can only store ONE promotion per SKU
- **Update Frequency**: Daily (after 9:00 AM)

---

### TBL_PROM_GROUP (Promotion Group/Tier Configuration)

**Purpose**: Defines multi-tier promotion structure

```sql
CREATE TABLE TBL_PROM_GROUP (
    EVENT_NO        VARCHAR2(20) NOT NULL,
    SEQ_NO          VARCHAR2(2) NOT NULL,   -- Tier sequence: "1", "2", "3"
    GROUP_CNT       NUMBER(2),              -- Number of groups in tier
    COND_AMT_TYPE   VARCHAR2(1),            -- "1" = amount, "2" = quantity
    PRIMARY KEY (EVENT_NO, SEQ_NO),
    FOREIGN KEY (EVENT_NO) REFERENCES TBL_PROM_EVENT(EVENT_NO)
);
```

**Example**: Buy $1000 get 10% off, Buy $2000 get 20% off
```
EVENT_NO = "MT24100001", SEQ_NO = "1", COND_AMT_TYPE = "1"  (Tier 1: $1000)
EVENT_NO = "MT24100001", SEQ_NO = "2", COND_AMT_TYPE = "1"  (Tier 2: $2000)
```

---

### TBL_PROM_CONDITION (Promotion Condition Configuration)

**Purpose**: Defines thresholds and limits for each promotion tier/group

```sql
CREATE TABLE TBL_PROM_CONDITION (
    EVENT_NO        VARCHAR2(20) NOT NULL,
    SEQ_NO          VARCHAR2(2) NOT NULL,
    GROUP_SEQ_NO    VARCHAR2(2) NOT NULL,
    COND_TYPE       VARCHAR2(1) NOT NULL,  -- "1" = condition, "2" = discount
    CONDITION_AMT   NUMBER(10),            -- Threshold amount or quantity
    LIMIT_QTY       NUMBER(5),             -- Purchase quantity limit
    HEAP            VARCHAR2(1),           -- "Y" = cumulative, "N" = one-time
    ALLOCATION_TYPE VARCHAR2(1),          -- Discount distribution method
    PRIMARY KEY (EVENT_NO, SEQ_NO, GROUP_SEQ_NO, COND_TYPE),
    FOREIGN KEY (EVENT_NO, SEQ_NO) REFERENCES TBL_PROM_GROUP(EVENT_NO, SEQ_NO)
);
```

**Condition Type**:
- `1`: Condition group (must purchase these items to qualify)
- `2`: Discount group (get discount on these items)

**LIMIT_QTY**: Maximum quantity eligible for promotion
**HEAP**:
- `Y`: Cumulative (can apply multiple times)
- `N`: One-time only

---

### TBL_PROM_SET (Promotion SKU Set Configuration)

**Purpose**: Defines which SKUs are in each promotion group

```sql
CREATE TABLE TBL_PROM_SET (
    EVENT_NO        VARCHAR2(20) NOT NULL,
    SEQ_NO          VARCHAR2(2) NOT NULL,
    GROUP_SEQ_NO    VARCHAR2(2) NOT NULL,
    SKU_NO          VARCHAR2(13) NOT NULL,
    SET_TYPE        VARCHAR2(1) NOT NULL,  -- "1" = condition, "2" = discount
    DISCOUNT_TYPE   VARCHAR2(1),           -- "1" = price, "2" = %, "3" = amount
    AMT             NUMBER(10),            -- Discount value
    LIMIT_QTY       NUMBER(5),             -- Quantity limit for this SKU
    PRIMARY KEY (EVENT_NO, SEQ_NO, GROUP_SEQ_NO, SKU_NO),
    FOREIGN KEY (EVENT_NO, SEQ_NO) REFERENCES TBL_PROM_GROUP(EVENT_NO, SEQ_NO)
);
```

**SET_TYPE**:
- `1`: Condition item (must purchase to qualify)
- `2`: Discount item (gets discount)

**DISCOUNT_TYPE**:
- `1`: Actual price (e.g., $99 special price)
- `2`: Percentage (e.g., 20% off)
- `3`: Fixed amount (e.g., $50 off)

---

### TBL_ORDER_DETL (Order Detail)

**Purpose**: Stores order item details including promotion assignment

```sql
CREATE TABLE TBL_ORDER_DETL (
    SO_NUMBER       VARCHAR2(20) NOT NULL,
    DETL_SEQ_ID     VARCHAR2(3) NOT NULL,
    SKU_NO          VARCHAR2(13) NOT NULL,
    QUANTITY        NUMBER(5) NOT NULL,
    POS_AMT         NUMBER(10) NOT NULL,
    EVENT_NOS       VARCHAR2(20),          -- Single-item promotion
    EVENT_NOSP      VARCHAR2(20),          -- Multi-promotion
    DISCOUNT_TYPE   VARCHAR2(1),           -- Event type: A-H
    DISCOUNT_AMT    NUMBER(10),            -- Discount amount (negative)
    DISCOUNT_QTY    NUMBER(5),             -- Discount quantity
    UNIT_COST       NUMBER(10,2),          -- Unit cost (for Type 2)
    BONUS_TOTAL     NUMBER(10),            -- Bonus points
    PRIMARY KEY (SO_NUMBER, DETL_SEQ_ID)
);
```

**Key Fields**:
- `EVENT_NOSP`: Promotion event number (copied from `TBL_SKU_STORE.PROM_EVENT_NO`)
- `DISCOUNT_TYPE`: Event type A-H (looked up from `TBL_PROM_EVENT.EVENT_TYPE`)
- `DISCOUNT_AMT`: Calculated discount (stored as negative value)
- `DISCOUNT_QTY`: Number of items discounted

---

### TBL_ORDER_EVENT_MSG (Order Promotion Message)

**Purpose**: Stores promotion description for invoice printing

```sql
CREATE TABLE TBL_ORDER_EVENT_MSG (
    SO_NUMBER       VARCHAR2(20) NOT NULL,
    EVENT_NO        VARCHAR2(20) NOT NULL,
    EVENT_DESC      VARCHAR2(200),         -- Promotion description
    EVENT_AMT       NUMBER(10),            -- Total discount amount
    PRIMARY KEY (SO_NUMBER, EVENT_NO)
);
```

**Populated By**: `SoEventBase.setOrderEventMsg()` (called by each event calculator)

**Purpose**:
- Print promotion details on invoice
- Show customer which promotions were applied
- Aggregate total discount per promotion

---

## File Reference Summary

### Batch Job Layer

**StoreSkuPriceReceiver.java**
- **Path**: `so-batchjob/src/main/java/com/trihome/som/so/dataexchange/service/StoreSkuPriceReceiver.java`
- **Lines**: 93-115
- **Purpose**: Daily batch sync from OMS, assigns `PROM_EVENT_NO`

**CustomOmsBsSkuStoreMapper.xml**
- **Path**: `so-batchjob/src/main/resources/sqlMap/CustomOmsBsSkuStoreMapper.xml`
- **Lines**: 4-93
- **Purpose**: MERGE SQL to sync `bs_sku_store@oms.MP_EVENT` → `TBL_SKU_STORE.PROM_EVENT_NO`

---

### Service Layer

**BzSoServices.java**
- **Path**: `so-bzservices/src/main/java/com/trihome/som/bz/service/BzSoServices.java`
- **Lines**:
  - 296: Autowired `SoComputeFunctionMainServices`
  - 770-781: `queryPromEvent()` - Get event type
  - 789-803: `queryPromGroup()` - Get tier structure
  - 814-838: `queryPromCondition()` - Get thresholds
  - 848-870: `queryPromSet()` - Get SKU sets
  - 4454-4478: Pricing engine integration
- **Purpose**: Core promotion query services

**SoComputeFunctionMainServices.java**
- **Path**: `so-bzservices/src/main/java/com/trihome/som/bz/service/SoComputeFunctionMainServices.java`
- **Lines**:
  - 44-49: `getSoComputeFunctionMain()` - Main entry point
  - 56-66: `setOrderDtlsEventType()` - Event type assignment
- **Purpose**: Multi-promotion orchestration service

**BzSkuInfoServices.java**
- **Path**: `so-bzservices/src/main/java/com/trihome/som/bz/service/BzSkuInfoServices.java`
- **Lines**: 138-144
- **Purpose**: SKU query with promotion info

---

### Function Layer

**SoComputeFunctionMain.java**
- **Path**: `so-bzservices/src/main/java/com/trihome/som/bz/functions/SoComputeFunctionMain.java`
- **Lines**:
  - 37-44: Event calculator declarations
  - 47-54: Event item bucket declarations
  - 62-72: Event calculator initialization
  - 78-116: `init()` - Event classification and initialization
  - 122-144: `assortGoodType()` - Separate by P, I, DD
  - 150-179: `assortEventNo()` - Classify by event type A-H
  - 190-240: `getDiscQty()`, `getDiscAmt()`, `getDiscInfo()`
- **Purpose**: Event classification and routing

**SoEventBase.java**
- **Path**: `so-bzservices/src/main/java/com/trihome/som/bz/functions/SoEventBase.java`
- **Lines**:
  - 42-68: `assortEventNo()` - Group items by event number
  - 76-133: Sorting utilities (high price, low price, quantity)
  - 169-222: Discount assignment utilities
  - 285-302: `setOrderEventMsg()` - Create promotion message
- **Purpose**: Base utilities for all event calculators

**Individual Event Calculators**:
- `SoEventA.java` - Stamp Price
- `SoEventB.java` - Invoice Amount Threshold Add-on
- `SoEventC.java` - Full Discount When Threshold Met
- `SoEventD.java` - Buy M Get N Discounted
- `SoEventE.java` - Buy Group A Get Group B Discount
- `SoEventF.java` - Bundle Price
- `SoEventG.java` - Shared Item Bundle Price
- `SoEventH.java` - Single Item Split-Price Bundle

---

### Frontend Layer

**soSKUSubPage.jsp**
- **Path**: `so-webapp/src/main/webapp/WEB-INF/views/so/commonpage/soSKUSubPage.jsp`
- **Lines**: 626-735
- **Purpose**: SKU selection and promotion assignment in JavaScript

---

### Test Layer

**SoEventG_IntegrationTest.java**
- **Path**: `so-webapp/src/test/java/com/trihome/som/bz/functions/SoEventG_IntegrationTest.java`
- **Lines**: 64-110 (discountType2_c1)
- **Purpose**: Integration test for Event G shared item bundle

**SoEventG_UnitTest.java**
- **Path**: `so-webapp/src/test/java/com/trihome/som/bz/functions/SoEventG_UnitTest.java`
- **Purpose**: Unit tests for Event G calculator

---

## Key Code Locations

| Topic | File | Lines | Description |
|-------|------|-------|-------------|
| **OMS Batch Sync** | CustomOmsBsSkuStoreMapper.xml | 4-93 | MERGE from OMS to TBL_SKU_STORE |
| **Batch Scheduler** | StoreSkuPriceReceiver.java | 93-115 | Daily sync after 9:00 AM |
| **SKU Query** | BzSkuInfoServices.java | 138-144 | Load SKU with promEventNo |
| **Event Type Lookup** | SoComputeFunctionMainServices.java | 56-66 | setOrderDtlsEventType() |
| **Event Classification** | SoComputeFunctionMain.java | 150-179 | assortEventNo() - Route to A-H |
| **Event Initialization** | SoComputeFunctionMain.java | 78-116 | init() - Process A→H |
| **Promotion Query** | BzSoServices.java | 770-781 | queryPromEvent() |
| **Condition Query** | BzSoServices.java | 814-838 | queryPromCondition() |
| **SKU Set Query** | BzSoServices.java | 848-870 | queryPromSet() |
| **Pricing Integration** | BzSoServices.java | 4454-4478 | Multi-promotion execution |
| **Frontend Assignment** | soSKUSubPage.jsp | 626-735 | JavaScript sets eventNosp |

---

## Conclusion and Next Steps

### Summary of Findings

1. **No SOM Priority Logic**: Priority resolution happens in OMS, not SOM
2. **Single Event Constraint**: `TBL_SKU_STORE.PROM_EVENT_NO` can only store one promotion
3. **Daily Sync**: Promotion changes have up to 24-hour delay
4. **Processing Order ≠ Priority**: A→H order is initialization sequence, not priority ranking
5. **Event Type Immutability**: Once assigned to order item, event type doesn't change
6. **Bonus Points Exclusion**: Items with bonus points cannot use event promotions
7. **No Expiry Validation**: Expired promotions may still be applied (bug)

### Business Decisions Needed

1. **Document OMS Priority Rules**: How does OMS decide when multiple promotions match?
2. **Define Edge Cases**: What happens with time-overlapping promotions?
3. **Sync Frequency**: Should urgent promotions have manual sync trigger?
4. **Expiry Handling**: Should SOM validate promotion dates at order time?
5. **Conflict Logging**: Should SOM log which promotions were "rejected" by OMS?

### Technical Recommendations

1. **Add Date Validation**: Check `START_DAT`/`END_DAT` in `setOrderDtlsEventType()`
2. **Add Processing Order Comment**: Clarify A→H is NOT priority order
3. **Fix Field Name Typo**: Rename `evnetType` to `eventType` in rewrite
4. **Create Promotion Dashboard**: Show conflict analysis for business
5. **Add Manual Sync Trigger**: For urgent promotion launches

### Next Phase Tasks

**Phase 2 Week 3 Remaining**:
- ✅ Task 7: Trace 促銷優先級邏輯 (Completed)
- ⏳ Task 8: Trace LIMIT_QTY overflow handling
- ⏳ Task 9: Trace 跨類促銷疊加規則 (cross-type promotion stacking)
- ⏳ Task 10: Update Rewrite-Spec with promotion logic

---

**Document End**
