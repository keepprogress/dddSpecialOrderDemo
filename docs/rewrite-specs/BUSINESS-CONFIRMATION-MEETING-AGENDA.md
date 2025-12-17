# Business Confirmation Meeting - Pricing Logic Review

## Meeting Information

- **Purpose**: Confirm business rules and resolve critical issues from pricing logic code trace
- **Date**: [To be scheduled - Week 2]
- **Duration**: 90 minutes
- **Required Attendees**:
  - Business Manager (Decision Maker)
  - Finance Manager (Financial Impact)
  - Product Manager (Business Rules Owner)
  - Technical Lead (Implementation)
  - QA Lead (Test Coverage)

## Meeting Objectives

1. Confirm critical business rules from code trace findings
2. Resolve identified issues requiring business decisions
3. Validate pricing logic assumptions
4. Approve Rewrite-Spec v1.2 pricing sections
5. Sign off on test scenarios and acceptance criteria

---

## Agenda

### 1. Introduction (5 minutes)

**Context**: Completed Phase 1 Week 1 code tracing for pricing logic coverage improvement (87% → 95%+ target).

**Documents for Review**:
- WORKTYPE-PRICE-APPORTIONMENT-TRACE.md (892 lines)
- SPECIAL-MEMBER-DISCOUNT-TRACE.md (1,096 lines)
- TYPE2-COST-MARKUP-DISCOUNT-TRACE.md (1,596 lines)

---

### 2. CRITICAL ISSUE: Type 2 Negative Discounts (20 minutes) 🚨

**Priority**: CRITICAL - Requires immediate decision

**Issue Description**:
Type 2 (Cost Markup) member discount can result in **price increases** instead of savings when the markup calculation exceeds the original selling price.

**Current Behavior**:
```
Example:
- Unit cost from DB: 90 TWD
- Markup rate: 30% (configured in TBL_CDISC)
- Calculated price: ceil(90 × 1.3) = 117 TWD
- With tax (1.05): floor(117 × 1.05) = 122 TWD
- Original POS price: 100 TWD
- Discount amount: 100 - 122 = -22 TWD ❌

Result: Member pays 122 TWD instead of 100 TWD (22% MORE!)
```

**Code Location**: `SoFunctionMemberDisServices.java:509`
```java
memberDiscVO.setDiscAmt((posAmt - disconut) + StringUtils.EMPTY);
// No validation - allows negative values
```

**Impact Analysis**:
- ✅ **Customer Satisfaction**: Members expect discounts, not price increases
- ✅ **Legal Compliance**: Advertising "member discount" but charging more may violate consumer protection laws
- ✅ **Brand Trust**: Price increases disguised as "discounts" damage brand reputation
- ✅ **Financial Impact**: Unknown - requires data analysis (see Query below)

**Data Analysis Query** (to be run before meeting):
```sql
-- Find historical cases of negative discounts
SELECT
    COUNT(*) AS negative_discount_count,
    SUM(od.POS_AMT - od.ACT_POS_AMT) AS total_overcharge,
    AVG(od.POS_AMT - od.ACT_POS_AMT) AS avg_overcharge
FROM TBL_ORDER_DETL od
INNER JOIN TBL_ORDER o ON od.ORDER_ID = o.ORDER_ID
WHERE od.POS_AMT_CHANGE_PRICE = 'Y'
  AND (od.POS_AMT - od.ACT_POS_AMT) < 0  -- Negative discount
  AND o.MEMBER_CARD_ID IS NOT NULL
  AND o.CREATE_DATE >= ADD_MONTHS(SYSDATE, -12);  -- Last 12 months
```

**Decision Required**: ✅ Choose One Option

**Option A: Prevent Negative Discounts (RECOMMENDED)**
```java
// Add validation before applying discount
int newPrice = (int)Math.ceil(unitCost * (1 + discPer));
if(!taxZero && SKU_TAX_TYPE_1.equals(orderDetlVO.getTaxType())) {
    newPrice = new BigDecimal((double)newPrice * salesTax)
        .setScale(0, BigDecimal.ROUND_FLOOR).intValue();
}

if(newPrice > posAmt) {
    logger.warn("Cost markup would increase price for SKU " + skuNo
        + ": " + newPrice + " > " + posAmt + ", skipping discount");
    return;  // Skip this discount
}
```
- ✅ **Pros**: Protects customers, prevents legal issues, simple to implement
- ❌ **Cons**: Some members may not receive Type 2 discount (will fall back to special discount if Type 0/1 also empty)

**Option B: Cap at Original Price**
```java
if(newPrice > posAmt) {
    logger.warn("Cost markup would increase price, capping at original");
    newPrice = posAmt;  // Member pays original price (no discount, no increase)
}
```
- ✅ **Pros**: Member never pays more, transparent behavior
- ❌ **Cons**: No discount given (neutral), may not meet member expectations

**Option C: Allow Negative Discounts (Current Behavior)**
```java
// No change - keep allowing price increases
memberDiscVO.setDiscAmt((posAmt - disconut) + StringUtils.EMPTY);
```
- ✅ **Pros**: No code change needed
- ❌ **Cons**: Legal risk, customer complaints, brand damage

**Recommendation**: **Option A - Prevent Negative Discounts**

**Action Items**:
- [ ] Business decision recorded in meeting minutes
- [ ] Update Rewrite-Spec v1.2 with chosen option
- [ ] Add validation code to implementation backlog
- [ ] Create test cases for negative discount scenarios

---

### 3. Type 2 Category Exclusion Review (10 minutes)

**Issue**: SUB_DEPT_ID 025 and 026 are excluded from Type 1 (Down Margin) and Type 2 (Cost Markup) discounts.

**Current Implementation**:
```java
// SoFunctionMemberDisServices.java:533-539
private boolean checkSkuSubDeptId(String discType, OrderDetlVO item) {
    if(("2".equals(discType) || "1".equals(discType)) &&
       ("025".equals(item.getSubDeptId()) || "026".equals(item.getSubDeptId()))) {
        return true;  // Exclude from Type 1/2
    }
    return false;
}
```

**Questions for Business**:
1. **What are categories 025 and 026?** (e.g., transport services, installation fees)
2. **Why excluded from Type 1/2 but allowed in Type 0?** (Discounting allowed, margin reduction not allowed)
3. **Should any additional categories be excluded?** (e.g., 027, special services)
4. **Is this documented in business rules?** (Not found in OpenSpec)

**Recommendation**:
- Document category exclusion rules in Rewrite-Spec
- Add business rationale (e.g., "standardized transport fees cannot be cost-marked up")
- Create configuration table for excluded categories (easier to maintain)

**Action Items**:
- [ ] Business provides official category list and rationale
- [ ] Update Rewrite-Spec with category exclusion section
- [ ] Consider configuration-driven exclusion list

---

### 4. Work Type Price Apportionment Rules (15 minutes)

**Issue**: Work type installation/delivery prices are apportioned proportionally across items, with remainder absorbed by the last item.

**Current Implementation**:
- Proportional distribution: `itemShare = totalWorkPrice × (itemPrice / orderTotal)`
- Remainder handling: Last item absorbs all rounding remainders
- Free installation: When `actualInstallAmt = 0`, all apportioned amounts = 0

**Business Rules Confirmed from Code**:
1. **R1**: Free installation → All install SKUs have preApportion = 0
2. **R2**: Paid installation → Proportional distribution by item value
3. **R3**: Last item absorbs all remainders (can be ±10 TWD difference)
4. **R4**: Applies to both installation and delivery work types

**Questions for Business**:
1. **Is remainder absorption by last item acceptable?**
   - Alternative: Distribute remainders across items proportionally
   - Alternative: Always round remainders toward customer benefit

2. **Should free installation always override apportionment?**
   - Current: If actualInstallAmt = 0, all items get 0 apportioned cost
   - Alternative: Use cost-based apportionment even for free installation (for margin tracking)

3. **Maximum acceptable remainder for last item?**
   - Current: No limit (can be ±10 TWD or more)
   - Proposal: Cap at ±5 TWD, distribute excess across items

**Example Scenario**:
```
Order total: 10,000 TWD
Installation total: 500 TWD
Items:
  - Item 1: 5,000 TWD (50%) → 250 TWD install
  - Item 2: 3,000 TWD (30%) → 150 TWD install
  - Item 3: 2,000 TWD (20%) → 100 TWD install

After rounding:
  - Item 1: 250 TWD
  - Item 2: 150 TWD
  - Item 3: 100 TWD
  Total: 500 TWD ✅ (no remainder in this case)

With remainder example:
Order total: 10,001 TWD
Installation: 501 TWD
  - Item 1: 250.475 → round to 250 TWD
  - Item 2: 150.285 → round to 150 TWD
  - Item 3: 100.19 → 101 TWD (absorbs +0.05 remainder) ✅
```

**Recommendation**: Confirm current behavior is acceptable or define new remainder handling policy.

**Action Items**:
- [ ] Business confirms remainder absorption policy
- [ ] Document rationale in Rewrite-Spec
- [ ] Add test scenarios for edge cases (large remainders)

---

### 5. Special Member Discount vs Type 0/1/2 Priority (10 minutes)

**Issue**: Special member discount (Type CT) only executes when Type 0/1/2 **all return empty results**.

**Current Implementation**:
```java
// BzSoServices.java:4459-4466
//會員折扣-Discounting (Type 0)
memberDiscSkus.addAll(soComputeFunctionMemberDis(..., "0", ...));
//會員折扣-Down Margin (Type 1)
memberDiscSkus.addAll(soComputeFunctionMemberDis(..., "1", ...));
//會員折扣-Cost Markup (Type 2)
memberDiscSkus.addAll(soComputeFunctionMemberDis(..., "2", ...));

if(memberDiscSkus.isEmpty()) {
    //特殊會員折扣 (fallback)
    memberDiscSkus.addAll(soComputeMemberDisForSpecial(...));
}
```

**Business Logic**:
- Type CT = "Catch-all" fallback for members without specific discount configs
- Mutual exclusion: If ANY item gets Type 0/1/2 discount, ALL items skip Type CT
- Two-phase matching: Exact SKU → Category wildcard ('000000000')

**Questions for Business**:
1. **Is "all or nothing" behavior intended?**
   - Current: If 1 out of 10 items matches Type 0, all 10 items skip Type CT
   - Alternative: Allow per-item fallback (Item A uses Type 0, Item B uses Type CT)

2. **Should Type CT have lower priority than Type 0/1/2?**
   - Current: Yes (only executes when 0/1/2 all empty)
   - Alternative: Highest discount wins (compare Type 0/1/2 vs CT, apply best for customer)

3. **Is wildcard SKU '000000000' standard convention?**
   - Used for category-level discounts across the system
   - Should be documented as standard practice

**Recommendation**: Document mutual exclusion logic clearly in Rewrite-Spec with business rationale.

**Action Items**:
- [ ] Confirm "all or nothing" behavior is intentional
- [ ] Document Type CT as fallback mechanism
- [ ] Add test scenarios for mixed discount scenarios

---

### 6. Tax Rounding Policy Consistency (10 minutes)

**Issue**: Different discount types use different tax rounding methods.

**Current Implementation**:

| Discount Type | Tax Rounding | Code Location |
|--------------|--------------|---------------|
| Type 0 (Discounting) | Math.ceil (round up) | SoFunctionMemberDisServices:430-434 |
| Type 1 (Down Margin) | Math.ceil (round up) | SoFunctionMemberDisServices:450, 457, 464 |
| Type 2 (Cost Markup) | **Math.floor (round down)** | SoFunctionMemberDisServices:491 |
| Special (Type CT) | Math.ceil (round up) | SoFunctionMemberDisServices:213 |

**Type 2 Change History**:
```java
// Before 2020-05-07: ROUND_HALF_UP
disconut = new BigDecimal((double)disconut * salesTax)
    .setScale(0, BigDecimal.ROUND_HALF_UP).intValue();

// After 2020-05-07: ROUND_FLOOR
disconut = new BigDecimal((double)disconut * salesTax)
    .setScale(0, BigDecimal.ROUND_FLOOR).intValue();
```

**Impact Example**:
```
Pre-tax price: 58 TWD
Tax calculation: 58 × 1.05 = 60.9 TWD

Type 0/1/CT: ceil(60.9) = 61 TWD
Type 2:      floor(60.9) = 60 TWD

Difference: 1 TWD per item (adds up across large orders)
```

**Questions for Business**:
1. **Why does Type 2 use different rounding?**
   - Possible reason: More favorable to customer (cost markup should benefit member)
   - Alternative reason: Finance accounting requirement

2. **Should all discount types use consistent rounding?**
   - **Option A**: All use floor (most customer-friendly)
   - **Option B**: All use ceil (traditional, conservative)
   - **Option C**: Keep different (current behavior, requires documentation)

3. **What was the business rationale for 2020-05-07 change?**
   - Need to review change request / email from that date
   - Document in Rewrite-Spec for future reference

**Recommendation**: Standardize rounding policy across all discount types OR document clear business rationale for differences.

**Action Items**:
- [ ] Research 2020-05-07 change reason
- [ ] Business decides: standardize or document differences
- [ ] Update Rewrite-Spec with tax rounding policy section

---

### 7. Line 508 Duplicate Assignment (5 minutes) 🔍

**Issue**: Potential code bug in Type 2 discount calculation.

**Code Location**: `SoFunctionMemberDisServices.java:493-509`
```java
// Lines 493-507: Update price based on goods type
if(GoodsType.P.equals(orderDetlVO.getGoodsType())) {
    orderDetlVO.setActPosAmt(disconut + StringUtils.EMPTY);  // Set here
    orderDetlVO.setTotalPrice((disconut * qty) + StringUtils.EMPTY);
    orderDetlVO.setPosAmtChangePrice(true);
} else if(isInstallSku(orderDetlVO.getGoodsType())) {
    orderDetlVO.setPreApportion(disconut + StringUtils.EMPTY);
    orderDetlVO.setInstallPrice(disconut + StringUtils.EMPTY);
    orderDetlVO.setActInstallPrice((disconut * qty) + StringUtils.EMPTY);
    orderDetlVO.setInstallChangePrice(true);
} else if(isDeliverySku(orderDetlVO.getGoodsType())) {
    orderDetlVO.setPreApportion(disconut + StringUtils.EMPTY);
    orderDetlVO.setDeliveryPrice(disconut + StringUtils.EMPTY);
    orderDetlVO.setActDeliveryPrice((disconut * qty) + StringUtils.EMPTY);
    orderDetlVO.setDeliveryChangePrice(true);
}

// Line 508: DUPLICATE - Always executes regardless of goods type
orderDetlVO.setActPosAmt(disconut + StringUtils.EMPTY);  // Set again!
```

**Impact Analysis**:
- **For GoodsType.P**: Redundant but harmless (sets same value twice)
- **For Install SKUs**: Incorrect - overwrites install price into actPosAmt
- **For Delivery SKUs**: Incorrect - overwrites delivery price into actPosAmt

**Questions for Business**:
1. **Is line 508 a bug or intentional?**
   - Likely bug (duplicate code)
   - Should be removed

2. **Have there been reports of incorrect pricing for install/delivery Type 2 discounts?**
   - Check support tickets / customer complaints
   - Check financial reconciliation reports

**Recommendation**: Remove line 508 (duplicate assignment) and add test coverage for install/delivery SKU Type 2 discounts.

**Action Items**:
- [ ] Technical team reviews code history (when was line 508 added?)
- [ ] QA tests install/delivery SKU Type 2 discounts in production
- [ ] Add fix to implementation backlog if confirmed as bug

---

### 8. Missing Business Rules in OpenSpec (10 minutes)

**Issue**: Critical business rules discovered in code are not documented in OpenSpec (legacy specification).

**Missing Documentation**:

| Rule | Found in Code | Missing from OpenSpec |
|------|---------------|----------------------|
| Type 2 category exclusion (025/026) | ✅ Yes | ❌ No |
| Special discount mutual exclusion | ✅ Yes | ❌ No |
| Work type remainder absorption | ✅ Yes | ❌ No |
| Wildcard SKU '000000000' pattern | ✅ Yes | ❌ No |
| Type 2 cost source (AVG_COST) | ✅ Yes | ❌ No |
| Tax rounding differences | ✅ Yes | ❌ No |
| Free installation zero apportionment | ✅ Yes | ❌ No |
| Negative discount possibility | ✅ Yes | ❌ No |

**Impact**:
- New developers cannot understand business logic from documentation
- Risk of incorrect implementation in system rewrite
- Knowledge loss when original developers leave

**Questions for Business**:
1. **Is OpenSpec the authoritative business rules document?**
   - If yes: Need to update OpenSpec with code-discovered rules
   - If no: What is the source of truth?

2. **Should Rewrite-Spec replace OpenSpec going forward?**
   - Recommendation: Yes, with version control and change tracking

3. **Who is responsible for maintaining business rules documentation?**
   - Product Manager? Business Analyst? Technical Writer?

**Recommendation**: Establish Rewrite-Spec v1.2 as the new authoritative specification and deprecate outdated OpenSpec sections.

**Action Items**:
- [ ] Business confirms Rewrite-Spec as authoritative document
- [ ] Assign documentation owner for future updates
- [ ] Create documentation review process

---

### 9. Test Scenarios Approval (10 minutes)

**Request**: Business approval of test scenarios documented in trace reports.

**Test Coverage by Category**:

**Work Type Apportionment** (4 scenarios):
1. Standard proportional distribution
2. Free installation (zero apportionment)
3. Remainder absorption by last item
4. Single item order (no distribution needed)

**Special Member Discount** (5 scenarios):
1. Exact SKU match (Type CT)
2. Category wildcard match ('000000000')
3. Mutual exclusion (Type 0 exists, skip CT)
4. Tax-free item discount
5. Price-changed item exclusion

**Type 2 Cost Markup** (5 scenarios):
1. Standard product cost markup (taxable)
2. Category-level discount (tax-free)
3. Transport category exclusion (025)
4. Install SKU cost markup
5. **Negative discount (price increase)** ⚠️

**Total**: 14 test scenarios covering 3 pricing domains

**Questions for Business**:
1. **Are test scenarios realistic and representative?**
2. **Are there additional edge cases to test?**
3. **What are acceptable pass/fail criteria?**
4. **Should we add performance test scenarios?** (e.g., 1000-item orders)

**Action Items**:
- [ ] Business approves test scenarios
- [ ] QA Lead creates detailed test plans from scenarios
- [ ] Add scenarios to Phase 6 test suite implementation

---

### 10. Rewrite-Spec v1.2 Review (10 minutes)

**Request**: Pre-approval for Rewrite-Spec v1.2 content outline.

**Planned Sections** (based on completed traces):

```markdown
## 3.2 Member Discount Logic

### 3.2.1 Overview
- Execution order: Type 0 → Type 1 → Type 2 → Type CT (fallback)
- Mutual exclusion rules
- CRM integration points

### 3.2.2 Type 0: Discounting
- Percentage discount on selling price
- Promotion amount add-back
- Math.ceil rounding
- [Existing content from OpenSpec]

### 3.2.3 Type 1: Down Margin
- Direct price reduction
- Category exclusion (025/026)
- Price change flag setting
- [Existing content from OpenSpec]

### 3.2.4 Type 2: Cost Markup ⭐ NEW
- Cost-based pricing formula
- Unit cost source (TBL_SKU_STORE.AVG_COST)
- Tax handling (ROUND_FLOOR)
- Category exclusion (025/026)
- Negative discount prevention ⚠️
- Test scenarios

### 3.2.5 Type CT: Special Member Discount ⭐ NEW
- Fallback mechanism
- Two-phase matching (exact → wildcard)
- Wildcard SKU '000000000' convention
- Mutual exclusion with Type 0/1/2
- Test scenarios

### 3.3 Work Type Price Apportionment ⭐ NEW
- Proportional distribution algorithm
- Remainder absorption by last item
- Free installation special handling
- Install vs delivery work type differences
- Test scenarios

### 3.4 Tax Calculation Policy ⭐ NEW
- Rounding policy by discount type
- Sales tax rate (1.05)
- Tax-free transaction handling
- Zero-tax mode
```

**Questions for Business**:
1. **Is this structure appropriate?**
2. **Should we separate into multiple documents?** (one per domain)
3. **What level of detail is required?** (summary vs. implementation detail)
4. **Who reviews and approves spec updates?** (single approver or committee)

**Timeline**:
- Draft completion: End of Week 2
- Review period: Week 3
- Approval target: End of Week 3

**Action Items**:
- [ ] Business approves content outline
- [ ] Assign reviewers for Week 3
- [ ] Schedule follow-up review meeting

---

### 11. Next Steps & Action Items Summary (5 minutes)

**Immediate Decisions Needed (Today)**:
1. ✅ **CRITICAL**: Type 2 negative discount policy (Option A/B/C)
2. ✅ Tax rounding policy standardization
3. ✅ Test scenarios approval

**Week 2 Actions**:
1. Document meeting decisions in minutes
2. Update Rewrite-Spec v1.2 with approved content
3. Run data analysis queries for historical negative discounts
4. Research 2020-05-07 tax rounding change rationale
5. Email Product Manager for auto-trigger pricing confirmations

**Week 3 Actions**:
1. Rewrite-Spec v1.2 review and approval
2. Begin Phase 2: Promotion logic tracing
3. Implement code fixes for confirmed issues (if any)

**Open Questions Log**:
- [ ] Type 2 negative discount policy
- [ ] Category exclusion rationale (025/026)
- [ ] Remainder absorption acceptability
- [ ] Tax rounding policy consistency
- [ ] Line 508 duplicate assignment
- [ ] OpenSpec vs Rewrite-Spec authority
- [ ] Test scenario approval
- [ ] Rewrite-Spec v1.2 content structure

---

## Meeting Materials Checklist

**Before Meeting**:
- [ ] Send meeting invite with agenda (this document)
- [ ] Attach three trace documents for pre-reading
- [ ] Run data analysis queries (negative discounts, category usage)
- [ ] Research 2020-05-07 change history
- [ ] Prepare presentation slides (optional)

**During Meeting**:
- [ ] Record meeting (with consent)
- [ ] Take detailed minutes
- [ ] Document all decisions with rationale
- [ ] Assign action items with owners and due dates

**After Meeting**:
- [ ] Distribute meeting minutes within 24 hours
- [ ] Update Rewrite-Spec v1.2 based on decisions
- [ ] Create implementation tickets for code fixes
- [ ] Schedule follow-up meeting if needed

---

## Appendix: Data Analysis Queries

### Query 1: Type 2 Negative Discount Historical Analysis
```sql
SELECT
    o.ORDER_ID,
    o.MEMBER_CARD_ID,
    od.SKU_NO,
    od.UNIT_COST,
    od.POS_AMT AS ORIGINAL_PRICE,
    od.ACT_POS_AMT AS NEW_PRICE,
    (od.POS_AMT - od.ACT_POS_AMT) AS DISCOUNT_AMT,
    (od.POS_AMT - od.ACT_POS_AMT) * od.QUANTITY AS TOTAL_OVERCHARGE,
    od.QUANTITY,
    o.CREATE_DATE
FROM TBL_ORDER_DETL od
INNER JOIN TBL_ORDER o ON od.ORDER_ID = o.ORDER_ID
WHERE od.POS_AMT_CHANGE_PRICE = 'Y'
  AND (od.POS_AMT - od.ACT_POS_AMT) < 0
  AND o.MEMBER_CARD_ID IS NOT NULL
  AND o.CREATE_DATE >= ADD_MONTHS(SYSDATE, -12)
ORDER BY TOTAL_OVERCHARGE;
```

### Query 2: Category 025/026 Usage Analysis
```sql
SELECT
    od.SUB_DEPT_ID,
    COUNT(DISTINCT od.ORDER_ID) AS order_count,
    COUNT(*) AS item_count,
    COUNT(CASE WHEN od.POS_AMT_CHANGE_PRICE = 'Y' THEN 1 END) AS price_changed_count,
    SUM(od.POS_AMT * od.QUANTITY) AS total_revenue
FROM TBL_ORDER_DETL od
INNER JOIN TBL_ORDER o ON od.ORDER_ID = o.ORDER_ID
WHERE od.SUB_DEPT_ID IN ('025', '026', '027')
  AND o.CREATE_DATE >= ADD_MONTHS(SYSDATE, -3)
GROUP BY od.SUB_DEPT_ID
ORDER BY od.SUB_DEPT_ID;
```

### Query 3: Type 2 Discount Configuration Audit
```sql
SELECT
    cd.DISCOUNT_ID,
    cd.CHANNEL_ID,
    cd.SKU_NO,
    cd.SUB_DEPT_ID,
    cd.DISC_PER,
    CASE
        WHEN cd.SKU_NO = '000000000' THEN 'Category-Level'
        ELSE 'SKU-Level'
    END AS MATCH_LEVEL,
    CASE
        WHEN cd.SUB_DEPT_ID IN ('025', '026') THEN 'WARNING: Excluded Category'
        ELSE 'Valid'
    END AS STATUS
FROM TBL_CDISC cd
WHERE cd.DISC_TYPE = '2'
  AND SYSDATE BETWEEN cd.START_DATE AND cd.END_DATE
ORDER BY STATUS, cd.DISCOUNT_ID;
```

---

**End of Meeting Agenda**
