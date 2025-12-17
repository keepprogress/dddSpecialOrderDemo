# Email Draft: Product Manager - Auto-Trigger Pricing Confirmation

## Email Metadata

**To**: [Product Manager Name] <pm@company.com>
**CC**: [Business Manager], [Technical Lead]
**Subject**: Confirmation Needed: Auto-Trigger Pricing Rules for Rewrite-Spec v1.2
**Priority**: High
**Attachments**:
- WORKTYPE-PRICE-APPORTIONMENT-TRACE.md
- SPECIAL-MEMBER-DISCOUNT-TRACE.md
- TYPE2-COST-MARKUP-DISCOUNT-TRACE.md

---

## Email Body

Hi [PM Name],

I hope this email finds you well.

As part of our **business logic coverage improvement initiative** (Phase 1), we've completed code tracing for three critical pricing domains. During this analysis, we've identified several **auto-trigger pricing rules** that need Product Manager confirmation for inclusion in Rewrite-Spec v1.2.

### Background

We're improving test coverage from **87% to 95%+** by documenting missing business rules discovered through systematic code tracing. Phase 1 Week 1 has been completed with three comprehensive trace documents (attached).

### Confirmation Needed

Below are **10 auto-trigger pricing rules** discovered in the code that require your confirmation. Please review and confirm whether each rule is:
- ✅ **Correct** - Should be documented in Rewrite-Spec as-is
- ⚠️ **Needs Clarification** - Requires additional business context
- ❌ **Incorrect** - Should be changed in the rewrite

---

## Section 1: Member Discount Auto-Trigger Rules

### Rule 1: Member Discount Type Priority Order

**Current Behavior** (discovered in code):
```
Execution order:
1. Type 0 (Discounting) - Applied first
2. Type 1 (Down Margin) - Applied second
3. Type 2 (Cost Markup) - Applied third
4. Type CT (Special Member Discount) - Applied ONLY if Type 0/1/2 all return empty
```

**Code Location**: `BzSoServices.java:4459-4466`

**Question**: Is this priority order documented in product requirements?
- Is Type CT intended as a "fallback" mechanism?
- Should it be "best discount wins" instead of "first match wins"?

**Your Confirmation**:
- [ ] ✅ Correct - Priority order is as designed
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should be: [Your suggestion]

---

### Rule 2: Mutual Exclusion Between Member Discounts

**Current Behavior**:
If **any item** in the order matches Type 0/1/2 discount, **all items** skip Type CT (special discount).

**Example**:
- Order has 10 items
- Item 1 matches Type 0 discount config
- Items 2-10 do NOT match Type 0/1/2
- **Result**: Items 2-10 get NO discount (Type CT is skipped for entire order)

**Code Location**: `BzSoServices.java:4464-4466`

**Question**: Is this "all or nothing" behavior intentional?
- Alternative: Per-item fallback (Item 1 uses Type 0, Items 2-10 use Type CT)

**Your Confirmation**:
- [ ] ✅ Correct - All or nothing is intentional
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should allow per-item fallback

---

### Rule 3: Category Exclusion from Type 1/2 Discounts

**Current Behavior**:
SUB_DEPT_ID **025** and **026** are **automatically excluded** from Type 1 (Down Margin) and Type 2 (Cost Markup) discounts.

**Code Location**: `SoFunctionMemberDisServices.java:533-539`

**Questions**:
1. What are categories 025 and 026? (e.g., transport, special services)
2. Why are they excluded from Type 1/2 but allowed in Type 0?
3. Should any additional categories be excluded?
4. Is this rule documented in product requirements?

**Your Confirmation**:
- [ ] ✅ Correct - Categories 025/026 should be excluded
  - Business reason: [Please explain]
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should not be excluded / Different categories

---

### Rule 4: Two-Phase Member Discount Matching

**Current Behavior**:
Member discount matching uses two-phase algorithm:
1. **Phase 1**: Exact SKU match in TBL_CDISC (SKU_NO = actual SKU)
2. **Phase 2**: Category wildcard match (SKU_NO = '000000000' + CLASS/SUB_DEPT/SUB_CLASS)

**Code Location**: `SoFunctionMemberDisServices.java:293-360`

**Question**: Is wildcard SKU '000000000' a standard convention?
- Should this be documented as a configuration standard?
- Are there other wildcard patterns in use?

**Your Confirmation**:
- [ ] ✅ Correct - '000000000' is standard wildcard
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Different wildcard pattern: [Your suggestion]

---

## Section 2: Work Type Price Apportionment Rules

### Rule 5: Free Installation Auto-Override

**Current Behavior**:
When `actualInstallAmt = 0` (free installation), **all install SKU apportioned costs are automatically set to 0**, regardless of calculated apportionment.

**Code Location**: `BzSoServices.java:4619-4628`

**Business Logic**:
- Installation fee = 500 TWD calculated
- Promotion makes installation free (actualInstallAmt = 0)
- **Result**: All install SKUs have preApportion = 0

**Question**: Should free installation still apportion costs for margin tracking purposes?
- Current: Free installation → zero apportionment (no cost tracking)
- Alternative: Apportion calculated costs even if final price is zero (for analytics)

**Your Confirmation**:
- [ ] ✅ Correct - Free installation should zero all apportionment
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should apportion for margin tracking

---

### Rule 6: Remainder Absorption by Last Item

**Current Behavior**:
When apportioning work type prices proportionally, **rounding remainders are absorbed by the last item** in the order.

**Example**:
- Total installation: 501 TWD
- Item 1 (50%): 250.5 → rounds to 251
- Item 2 (30%): 150.3 → rounds to 150
- Item 3 (20%): 100.2 → rounds to 100 + remainder
- **Last item absorbs**: 100 + (501 - 251 - 150) = 100 TWD

**Code Location**: `BzSoServices.java:4663-4681`

**Question**: Is last-item remainder absorption acceptable?
- Can cause ±5-10 TWD variance on last item
- Alternative: Distribute remainders proportionally across all items

**Your Confirmation**:
- [ ] ✅ Correct - Last item absorption is acceptable
- [ ] ⚠️ Needs Clarification: Acceptable range = ±[X] TWD
- [ ] ❌ Incorrect - Should distribute remainders proportionally

---

## Section 3: Type 2 Cost Markup Rules

### Rule 7: Unit Cost Source Auto-Loading

**Current Behavior**:
Type 2 (Cost Markup) discount automatically loads unit cost from **TBL_SKU_STORE.AVG_COST** when order is created.

**Data Flow**:
1. Frontend queries SKU info → Backend returns `avgCost`
2. Frontend stores in `lstSkuInfo[].unitCost`
3. Backend persists to `TBL_ORDER_DETL.UNIT_COST`
4. Type 2 discount uses stored `unitCost` for markup calculation

**Code Location**: `BzSoServices.java:906-999`, `soSKUSubPage.jsp:626`

**Questions**:
1. Is AVG_COST the correct source? (vs. latest purchase cost, standard cost)
2. How often is AVG_COST updated? (daily, weekly, monthly)
3. What happens if AVG_COST is NULL or 0?
4. Should stale cost data trigger alerts?

**Your Confirmation**:
- [ ] ✅ Correct - AVG_COST is the correct source
  - Update frequency: [Please specify]
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should use: [Different cost source]

---

### Rule 8: Type 2 Tax Rounding Auto-Application

**Current Behavior**:
Type 2 discount applies sales tax **after markup calculation** using **ROUND_FLOOR** (round down).

**Formula**:
```
1. markupPrice = ceil(unitCost × (1 + markupPercent))
2. If taxable: finalPrice = floor(markupPrice × 1.05)
3. discountAmt = originalPrice - finalPrice
```

**Code Location**: `SoFunctionMemberDisServices.java:488-492`

**Change History**: 2020-05-07 changed from ROUND_HALF_UP to ROUND_FLOOR

**Question**: Why is Type 2 different from Type 0/1 (which use ceil)?
- Type 0/1: ceil (round up)
- Type 2: floor (round down)
- Should all discount types use consistent rounding?

**Your Confirmation**:
- [ ] ✅ Correct - Type 2 should use floor (customer-friendly)
  - Business reason: [Please explain]
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should standardize to: [ceil or floor]

---

## Section 4: Price Change Flag Auto-Setting

### Rule 9: Type 2 Price Change Flag Behavior

**Current Behavior**:
Type 2 discount **directly replaces** selling price and sets price change flags:
- GoodsType.P → sets `posAmtChangePrice = true`
- Install SKUs → sets `installChangePrice = true`
- Delivery SKUs → sets `deliveryChangePrice = true`

**Impact**:
Once price change flag is set, item is excluded from:
- Subsequent member discount attempts
- Certain promotions
- Price adjustments

**Code Location**: `SoFunctionMemberDisServices.java:493-507`

**Question**: Should Type 2 set price change flags?
- Type 0 (Discounting): Does NOT set flags (applies discount amount)
- Type 1 (Down Margin): Sets flags (changes price)
- Type 2 (Cost Markup): Sets flags (changes price)
- Consistency with Type 1?

**Your Confirmation**:
- [ ] ✅ Correct - Type 2 should set price change flags
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should behave like Type 0 (discount only)

---

### Rule 10: Type 2 Tax-Free Total Exclusion

**Current Behavior**:
Type 2 discounts are **automatically excluded** from member discount tax-free total calculation.

**Code Location**: `SoFunctionMemberDisServices.java:602-610`
```java
for (MemberDiscVO sku : memberDiscSkus) {
    if(MEMBER_DISCOUNT_TYPE_2.equals(sku.getDiscType())) {
        continue;  // Skip Type 2
    }
    // Calculate tax-free totals for Type 0 only
}
```

**Question**: Why is Type 2 excluded from tax-free calculation?
- Hypothesis: Type 2 changes prices, not discounts (already tax-adjusted)
- Type 0/1: Discount amounts need separate tax calculation

**Your Confirmation**:
- [ ] ✅ Correct - Type 2 should be excluded (price change, not discount)
  - Business reason: [Please explain]
- [ ] ⚠️ Needs Clarification: [Your comments]
- [ ] ❌ Incorrect - Should be included in tax-free calculation

---

## Section 5: Critical Issue Requiring PM Input

### 🚨 CRITICAL: Type 2 Negative Discounts

**Issue Discovered**:
Type 2 discount can result in **price increases** (negative discounts) when markup exceeds original price.

**Example**:
- Unit cost: 90 TWD
- Markup: 30%
- Calculated price: ceil(90 × 1.3) × 1.05 = 122 TWD
- Original POS price: 100 TWD
- **Discount amount: -22 TWD** (member pays MORE!)

**Current Code**: No validation - allows negative discounts

**Business Impact**:
- Customer satisfaction risk
- Potential legal issues (false advertising)
- Brand reputation damage

**Question**: Should we prevent negative discounts?
- **Option A**: Skip discount if newPrice > originalPrice (recommended)
- **Option B**: Cap at original price (member pays original, no discount)
- **Option C**: Allow negative discounts (current behavior)

**Your Input**:
- [ ] Option A: Prevent negative discounts
- [ ] Option B: Cap at original price
- [ ] Option C: Allow negative discounts (needs business justification)
- [ ] Other: [Your suggestion]

**Urgency**: This requires **immediate decision** before finalizing Rewrite-Spec v1.2.

---

## Response Format

Please reply by [Date - suggest 5 business days] with your confirmations using this format:

```
Rule 1: ✅ Correct
Rule 2: ⚠️ Needs Clarification - Should allow per-item fallback for better customer experience
Rule 3: ✅ Correct - Categories 025 (Express Delivery) and 026 (Special Transport) are standardized fees
Rule 4: ✅ Correct
Rule 5: ⚠️ Needs Clarification - Let's discuss margin tracking requirements
Rule 6: ✅ Correct - Acceptable range ±10 TWD
Rule 7: ✅ Correct - AVG_COST updated nightly via batch job
Rule 8: ✅ Correct - Floor rounding was Finance requirement from 2020 audit
Rule 9: ✅ Correct
Rule 10: ✅ Correct - Type 2 excluded because prices are pre-tax-adjusted

CRITICAL Issue: Option A - Prevent negative discounts
```

## Next Steps

After receiving your confirmations:

1. **Week 2 Actions**:
   - Update Rewrite-Spec v1.2 with confirmed rules
   - Schedule Business Manager meeting to review decisions
   - Create test scenarios based on confirmed behavior

2. **Week 3 Actions**:
   - Rewrite-Spec v1.2 review and approval
   - Begin Phase 2: Promotion logic tracing

## Questions?

If you have any questions or need additional context, please don't hesitate to reach out. I'm available for a call to discuss any of these items in detail.

The three attached trace documents provide complete implementation details, code references, and test scenarios for your review.

Thank you for your time and input!

Best regards,
[Your Name]
[Your Title]
[Contact Information]

---

## Attachments Summary

1. **WORKTYPE-PRICE-APPORTIONMENT-TRACE.md** (892 lines)
   - Covers Rules 5-6
   - Complete work type apportionment logic trace
   - 4 test scenarios included

2. **SPECIAL-MEMBER-DISCOUNT-TRACE.md** (1,096 lines)
   - Covers Rules 1-4
   - Special member discount fallback mechanism
   - 5 test scenarios included

3. **TYPE2-COST-MARKUP-DISCOUNT-TRACE.md** (1,596 lines)
   - Covers Rules 3, 7-10, and CRITICAL issue
   - Type 2 cost markup complete analysis
   - 5 test scenarios + negative discount examples

**Total**: 3,584 lines of comprehensive documentation

---

## Email Metadata (for tracking)

**Sent Date**: [To be sent]
**Response Due**: [5 business days from sent date]
**Follow-up**: [Schedule if no response after 3 days]
**Related Tickets**: [Link to Jira/tracking system]
**Phase**: Phase 1 Week 2 - Business Confirmation
**Goal**: Complete Rewrite-Spec v1.2 with confirmed auto-trigger pricing rules

---

**End of Email Draft**
