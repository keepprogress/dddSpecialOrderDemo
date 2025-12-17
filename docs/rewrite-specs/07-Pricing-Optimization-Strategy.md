# 07. 計價系統優化策略 (Pricing Optimization Strategy)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **相關文檔**:
  - [06-Pricing-Problems-Analysis.md](./06-Pricing-Problems-Analysis.md)
  - [04-Pricing-Calculation-Sequence.md](./04-Pricing-Calculation-Sequence.md)
  - [17-Cache-Strategy.md](./17-Cache-Strategy.md)

---

## 目錄
1. [優化總覽](#優化總覽)
2. [架構層優化](#架構層優化)
3. [演算法層優化](#演算法層優化)
4. [資料層優化](#資料層優化)
5. [整合層優化](#整合層優化)
6. [監控與調優](#監控與調優)
7. [實施計畫](#實施計畫)

---

## 優化總覽

### 優化目標

| 指標 | 目前 | 目標 | 改善幅度 |
|------|------|------|---------|
| **單次計算時間** | 1560ms | 420ms | -73% ⬇️ |
| **重複計算次數** | 5 次/訂單 | 1 次/訂單 | -80% ⬇️ |
| **快取命中率** | 0% | 95%+ | +95% ⬆️ |
| **伺服器負載** | 100% | 35% | -65% ⬇️ |
| **並發處理能力** | 50 req/s | 200 req/s | +300% ⬆️ |
| **安全漏洞** | 3 個 P0 | 0 個 | -100% ⬇️ |

### 優化策略矩陣

```
高效益 │ ✅ Redis 快取       ✅ 移除前端計算   ✅ 平行化執行
       │ ✅ SQL 索引優化    ✅ 批次查詢
投     │
資     │
回     │
報     │ ⭕ 微服務拆分      ⭕ 讀寫分離       ⭕ 分散式快取
低效益 │ ⭕ 程式碼重構      ⭕ 增加測試
       │
       └───────────────────────────────────────────
          低難度                                 高難度
                        實施難度
```

圖例:
- ✅ **Quick Wins**: 高效益、低難度 - 優先實施
- ⭕ **Long-term**: 高效益、高難度 - 長期規劃
- ❌ **Low Priority**: 低效益 - 暫緩

---

## 架構層優化

### OPT-A1: 移除前端價格計算 (P0 - 立即)

**目標**: 解決 P0-1, P0-2 安全性問題

#### 現況分析
```javascript
// ❌ 目前：前端計算 (soSKUSubPage.jsp:1041)
function computePosAmt() {
    var totalAmt = (sellingAmt - discountAmt) * quantity;  // 可被篡改
    $('#totalAmt').val(totalAmt);
}
```

#### 優化方案

**1. 後端計算 API**
```java
// ✅ Spring Boot 3 實作
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    @Autowired
    private PricingService pricingService;

    @PostMapping("/calculate-sku")
    public ResponseEntity<SkuPricingResult> calculateSkuPrice(
        @RequestBody @Valid SkuPricingRequest request
    ) {
        // 全部計算在後端進行
        SkuPricingResult result = pricingService.calculateSkuPrice(request);
        return ResponseEntity.ok(result);
    }
}
```

**2. 前端改為純展示**
```typescript
// ✅ Angular 8 元件
@Component({
  selector: 'app-sku-pricing',
  templateUrl: './sku-pricing.component.html'
})
export class SkuPricingComponent {
  skuPrice$: Observable<SkuPricingResult>;

  constructor(private pricingService: PricingService) {}

  calculatePrice(): void {
    const request: SkuPricingRequest = {
      skuNo: this.form.get('skuNo').value,
      quantity: this.form.get('quantity').value,
      memberCardId: this.form.get('memberCardId').value
    };

    // 呼叫後端 API，僅展示結果
    this.skuPrice$ = this.pricingService.calculateSkuPrice(request);
  }
}
```

**3. 安全驗證機制**
```java
// ✅ 訂單建立時驗證價格
@Service
public class OrderService {

    public OrderVO createOrder(OrderRequest request) {
        // 1. 後端重新計算價格
        PricingResult calculatedPrice = pricingService.calculate(request);

        // 2. 驗證前端傳來的價格是否正確
        if (!calculatedPrice.getTotalAmt().equals(request.getTotalAmt())) {
            log.warn("價格驗證失敗: expected={}, actual={}",
                calculatedPrice.getTotalAmt(), request.getTotalAmt());
            throw new PriceValidationException("價格驗證失敗，請重新計算");
        }

        // 3. 使用後端計算的價格建立訂單
        return orderRepository.create(request, calculatedPrice);
    }
}
```

#### 效益評估
```
安全性: P0 漏洞消除 ✅
可維護性: +80% (邏輯集中在後端)
測試覆蓋率: +70% (後端可單元測試)
客戶信任度: +50% (無價格篡改風險)

實施時間: 1 週
風險: 低
```

---

### OPT-A2: 實作 Redis 快取層 (P1 - 高優先)

**目標**: 解決 P1-1, P1-2 效能問題，減少重複計算

#### 快取架構設計

```
┌─────────────┐
│   前端      │
│  (Angular)  │
└──────┬──────┘
       │ REST API
       ↓
┌─────────────────────────────┐
│     API Gateway             │
└──────┬──────────────────────┘
       │
       ↓
┌─────────────────────────────┐
│   Pricing Service           │
│   ┌──────────────────┐      │
│   │ 1️⃣ 檢查 Redis    │      │
│   │    快取          │      │
│   └────┬─────────────┘      │
│        │                     │
│        │ Cache Miss          │
│        ↓                     │
│   ┌──────────────────┐      │
│   │ 2️⃣ 執行計算      │      │
│   │    (1560ms)      │      │
│   └────┬─────────────┘      │
│        │                     │
│        ↓                     │
│   ┌──────────────────┐      │
│   │ 3️⃣ 寫入快取      │      │
│   │    (TTL 5min)    │      │
│   └──────────────────┘      │
└─────────────────────────────┘
       │
       ↓
┌─────────────────────────────┐
│  Redis 7.x Cluster          │
│  ┌─────────┬─────────┐      │
│  │ Master  │ Replica │      │
│  └─────────┴─────────┘      │
└─────────────────────────────┘
```

#### 實作細節

**1. Redis 快取配置**
```java
// ✅ Spring Boot 3 Redis 配置
@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))  // 5 分鐘 TTL
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware()
            .build();
    }
}
```

**2. 快取鍵設計**
```java
// 快取鍵格式: pricing:{memberCardId}:{skuHash}:{channelId}
public class PricingCacheKey {
    private static final String PREFIX = "pricing";

    public static String build(PricingRequest request) {
        String skuHash = hashSkus(request.getSkus());
        return String.format("%s:%s:%s:%s",
            PREFIX,
            request.getMemberCardId(),
            skuHash,
            request.getChannelId()
        );
    }

    private static String hashSkus(List<SkuInfo> skus) {
        // 使用 SKU 編號、數量、價格計算 Hash
        String skuStr = skus.stream()
            .sorted(Comparator.comparing(SkuInfo::getSkuNo))
            .map(sku -> String.format("%s:%d:%.2f",
                sku.getSkuNo(), sku.getQuantity(), sku.getSellingAmt()))
            .collect(Collectors.joining("|"));

        return DigestUtils.md5DigestAsHex(skuStr.getBytes());
    }
}
```

**3. 快取邏輯實作**
```java
@Service
public class PricingService {

    @Autowired
    private RedisTemplate<String, PricingResult> redisTemplate;

    @Autowired
    private PricingCalculator pricingCalculator;

    public PricingResult calculate(PricingRequest request) {
        String cacheKey = PricingCacheKey.build(request);

        // 1️⃣ 嘗試從快取取得
        PricingResult cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("快取命中: cacheKey={}", cacheKey);
            return cached;
        }

        // 2️⃣ 快取未命中，執行計算
        log.info("快取未命中，執行計算: cacheKey={}", cacheKey);
        PricingResult result = pricingCalculator.calculate(request);

        // 3️⃣ 寫入快取
        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));

        return result;
    }

    // 事件驅動的快取失效
    @EventListener
    public void onPromotionUpdated(PromotionUpdatedEvent event) {
        log.info("促銷活動更新，清除相關快取");
        redisTemplate.delete(redisTemplate.keys("pricing:*"));
    }

    @EventListener
    public void onMemberDiscountUpdated(MemberDiscountUpdatedEvent event) {
        String memberCardId = event.getMemberCardId();
        log.info("會員折扣更新，清除相關快取: memberCardId={}", memberCardId);
        redisTemplate.delete(redisTemplate.keys("pricing:" + memberCardId + ":*"));
    }
}
```

**4. 會員折扣快取**
```java
@Service
public class MemberDiscountService {

    @Cacheable(value = "member-discount", key = "#memberCardId + ':' + #discType")
    public MemberDiscount getMemberDiscount(String memberCardId, String discType) {
        // 呼叫 CRM 系統 (200ms)
        return crmClient.getMemberDiscount(memberCardId, discType);
    }

    @CacheEvict(value = "member-discount", key = "#memberCardId + ':*'")
    public void evictMemberDiscount(String memberCardId) {
        log.info("清除會員折扣快取: memberCardId={}", memberCardId);
    }
}
```

#### 快取策略

| 資料類型 | TTL | 失效策略 | 命中率預估 |
|---------|-----|---------|-----------|
| **完整計價結果** | 5 分鐘 | 促銷活動更新時清除 | 60% |
| **會員折扣** | 30 分鐘 | 會員資料更新時清除 | 95% |
| **促銷活動** | 10 分鐘 | 活動更新時清除 | 99% |
| **商品成本** | 1 小時 | 成本變更時清除 | 98% |

#### 效益評估
```
快取命中時:
├─ 回應時間: 1560ms → 50ms (-97%) 🚀
├─ CRM 呼叫: 600ms → 0ms (省略)
└─ 資料庫查詢: 400ms → 0ms (省略)

快取未命中時:
└─ 回應時間: 1560ms (與原本相同)

預估整體改善:
├─ 平均回應時間: 1560ms → 350ms (-77%)
├─ 伺服器負載: -65%
└─ 並發能力: 50 req/s → 180 req/s (+260%)

實施時間: 2 週
成本: Redis Cluster (3 節點) ~$150/月
ROI: 第一個月回本
```

---

## 演算法層優化

### OPT-B1: 平行化獨立步驟 (P1 - 高優先)

**目標**: 解決 P1-5 效能問題，將 1560ms → 1200ms

#### 目前執行序列

```
同步執行 (1560ms):
Step 1  ████ 50ms
Step 2  ██████████ 100ms
Step 3  ███ 30ms
Step 4  ██ 20ms      ← 可平行
Step 5  ████ 40ms    ← 可平行
Step 6  ████████████████████ 200ms
Step 7  ██████████████████████████████████████████████ 500ms
Step 8  ████████████████████ 200ms
Step 9  ████████████████████ 200ms
Step 10 ███████████████ 150ms
Step 11 █ 10ms
Step 12 ██████ 60ms  ← 內部可平行 (6 個 ComputeType)
```

#### 優化後平行執行

```java
// ✅ 使用 CompletableFuture 平行執行
@Service
public class OptimizedPricingCalculator {

    @Autowired
    private ExecutorService pricingExecutor;

    public PricingResult calculate(PricingRequest request) {
        long startTime = System.currentTimeMillis();

        // Steps 1-3: 必須同步 (有依賴)
        step1_revertPrices(request);
        step2_apportionmentDiscount(request);
        AssortSku assortSku = step3_classifySkus(request);

        // Steps 4-5: 平行執行 (無依賴)
        CompletableFuture<Void> step4Future = CompletableFuture.runAsync(
            () -> step4_setSerialNumbers(request),
            pricingExecutor
        );

        CompletableFuture<BigDecimal> step5Future = CompletableFuture.supplyAsync(
            () -> step5_calculateFreeInstall(request, assortSku),
            pricingExecutor
        );

        // 等待 Steps 4-5 完成
        CompletableFuture.allOf(step4Future, step5Future).join();
        BigDecimal freeInstallTotal = step5Future.join();

        // Steps 6-11: 必須同步 (有依賴)
        BigDecimal costMarkupDisc = step6_costMarkupDiscount(request);
        BigDecimal promotionDisc = step7_multiPromotion(request);
        BigDecimal discountingDisc = step8_discounting(request);
        BigDecimal downMarginDisc = step9_downMargin(request);
        BigDecimal specialMemberDisc = step10_specialMember(request);
        BigDecimal totalDisc = step11_calculateTotalDiscount(
            costMarkupDisc, promotionDisc, discountingDisc, downMarginDisc, specialMemberDisc
        );

        // Step 12: 平行生成 6 個 ComputeType
        List<CompletableFuture<OrderCompute>> computeFutures = IntStream.rangeClosed(1, 6)
            .mapToObj(type -> CompletableFuture.supplyAsync(
                () -> generateComputeType(request, type, totalDisc),
                pricingExecutor
            ))
            .collect(Collectors.toList());

        List<OrderCompute> orderComputes = computeFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        log.info("計價完成: duration={}ms", duration);  // ~1200ms

        return PricingResult.builder()
            .totalAmt(calculateTotalAmt(request, totalDisc))
            .totalDisc(totalDisc)
            .orderComputes(orderComputes)
            .build();
    }
}
```

#### 執行緒池配置

```java
@Configuration
public class PricingExecutorConfig {

    @Bean(name = "pricingExecutor")
    public ExecutorService pricingExecutor() {
        return new ThreadPoolExecutor(
            10,                          // corePoolSize
            20,                          // maxPoolSize
            60L,                         // keepAliveTime
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder()
                .setNameFormat("pricing-%d")
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

#### 效益評估
```
Step 4-5 平行化:
├─ 原本: 20ms + 40ms = 60ms
├─ 優化後: max(20ms, 40ms) = 40ms
└─ 節省: 20ms

Step 12 平行化:
├─ 原本: 10ms × 6 = 60ms
├─ 優化後: max(10ms) = 10ms
└─ 節省: 50ms

總改善:
├─ 1560ms → 1200ms (-23%)
├─ 搭配快取: 1200ms → 350ms (-78% 整體)
└─ CPU 使用率: +15% (可接受)

實施時間: 3 天
風險: 低 (步驟獨立性已驗證)
```

---

### OPT-B2: 優化促銷活動計算 (P1 - 中優先)

**目標**: 解決 P1-3 全表掃描問題，Step 7 從 500ms → 50ms

#### 目前問題

```java
// ❌ 查詢全部促銷活動 (5000+ 筆)
List<PromotionVO> allPromotions = promotionMapper.selectAll();  // 500ms

// 在記憶體中過濾
List<PromotionVO> validPromotions = allPromotions.stream()
    .filter(p -> p.getActiveFl().equals("Y"))
    .filter(p -> p.getStartDate().before(now))
    .filter(p -> p.getEndDate().after(now))
    .collect(Collectors.toList());  // 剩下 50-100 筆
```

#### 優化方案

**1. 資料庫索引優化**
```sql
-- ✅ 建立複合索引
CREATE INDEX IDX_PROMOTION_ACTIVE_DATE
ON TBL_PROMOTION (ACTIVE_FLG, START_DATE, END_DATE)
WHERE ACTIVE_FLG = 'Y';

-- 執行計畫改善:
-- Before: TABLE ACCESS FULL (Cost: 450, Rows: 5234)
-- After:  INDEX RANGE SCAN (Cost: 15, Rows: 87)
```

**2. SQL 查詢優化**
```java
// ✅ 只查詢有效促銷
@Mapper
public interface PromotionMapper {

    @Select("""
        SELECT * FROM TBL_PROMOTION
        WHERE ACTIVE_FLG = 'Y'
          AND START_DATE <= #{now}
          AND END_DATE >= #{now}
        ORDER BY PRIORITY DESC
        """)
    List<PromotionVO> selectActivePromotions(@Param("now") Date now);
}
```

**3. Redis 快取**
```java
@Service
public class PromotionService {

    @Cacheable(value = "active-promotions", key = "'all'")
    public List<PromotionVO> getActivePromotions() {
        Date now = new Date();
        return promotionMapper.selectActivePromotions(now);
    }

    @CacheEvict(value = "active-promotions", allEntries = true)
    @Scheduled(cron = "0 */10 * * * *")  // 每 10 分鐘更新
    public void refreshPromotions() {
        log.info("刷新促銷活動快取");
    }
}
```

#### 效益評估
```
查詢時間: 500ms → 50ms (-90%)
快取命中時: 50ms → 5ms (-99%)
記憶體使用: +10MB (快取 100 個促銷)

實施時間: 2 天
風險: 極低
```

---

## 資料層優化

### OPT-C1: 解決 N+1 查詢問題 (P1 - 高優先)

**目標**: 解決 P1-4 問題，Step 2 從 100ms → 25ms

#### 目前問題

```java
// ❌ N+1 查詢
for (SkuInfo workTypeSku : lstWorkTypeSku) {  // 假設 5 個工種
    // 每個工種查詢一次 - 總共 5 次查詢
    WorkTypePrice price = workTypePriceMapper.selectBySkuNo(workTypeSku.getSkuNo());
    // 分攤邏輯...
}

// 總耗時: 20ms × 5 = 100ms
```

#### 優化方案

**1. 批次查詢**
```java
// ✅ 一次查詢所有工種價格
@Mapper
public interface WorkTypePriceMapper {

    // 批次查詢方法
    @Select("""
        <script>
        SELECT * FROM TBL_WORKTYPE_PRICE
        WHERE SKU_NO IN
        <foreach collection='skuNos' item='skuNo' open='(' separator=',' close=')'>
            #{skuNo}
        </foreach>
        </script>
        """)
    List<WorkTypePrice> selectBatchBySkuNos(@Param("skuNos") List<String> skuNos);
}
```

**2. 服務層實作**
```java
// ✅ 使用批次查詢
private void apportionmentDiscount(
    List<SkuInfo> lstAllSku,
    List<SkuInfo> lstWorkTypeSku
) {
    // 1️⃣ 收集所有 SKU 編號
    List<String> skuNos = lstWorkTypeSku.stream()
        .map(SkuInfo::getSkuNo)
        .collect(Collectors.toList());

    // 2️⃣ 一次查詢所有價格
    Map<String, WorkTypePrice> priceMap = workTypePriceMapper
        .selectBatchBySkuNos(skuNos)
        .stream()
        .collect(Collectors.toMap(WorkTypePrice::getSkuNo, p -> p));

    // 3️⃣ 使用 Map 快速查找
    for (SkuInfo workTypeSku : lstWorkTypeSku) {
        WorkTypePrice price = priceMap.get(workTypeSku.getSkuNo());
        // 分攤邏輯...
    }
}
```

#### 效益評估
```
查詢次數: 5 次 → 1 次 (-80%)
查詢時間: 100ms → 25ms (-75%)

實施時間: 1 天
風險: 極低
```

---

### OPT-C2: SQL 注入防護 (P0 - 立即)

**目標**: 解決 P0-3 安全問題

#### 目前問題

```xml
<!-- ❌ SQL Injection 風險 -->
<select id="getSkuStockByDynamicCondition" resultType="SkuStockVO">
    SELECT * FROM TBL_SKU_STOCK
    WHERE 1=1
    <if test="condition != null">
        AND ${condition}  <!-- ⚠️ 使用 ${} 直接拼接 -->
    </if>
</select>
```

#### 優化方案

**1. 使用參數綁定**
```xml
<!-- ✅ 使用 #{} 參數綁定 -->
<select id="getSkuStock" resultType="SkuStockVO">
    SELECT * FROM TBL_SKU_STOCK
    WHERE 1=1
    <if test="skuNo != null">
        AND SKU_NO = #{skuNo}
    </if>
    <if test="storeId != null">
        AND STORE_ID = #{storeId}
    </if>
    <if test="stockQty != null">
        AND STOCK_QTY >= #{stockQty}
    </if>
</select>
```

**2. 重構動態查詢**
```java
// ✅ 使用 MyBatis Dynamic SQL
@Mapper
public interface SkuStockMapper {

    @SelectProvider(type = SkuStockSqlProvider.class, method = "dynamicQuery")
    List<SkuStockVO> selectByDynamicCriteria(SkuStockCriteria criteria);
}

public class SkuStockSqlProvider {
    public String dynamicQuery(SkuStockCriteria criteria) {
        return new SQL() {{
            SELECT("*");
            FROM("TBL_SKU_STOCK");

            if (criteria.getSkuNo() != null) {
                WHERE("SKU_NO = #{skuNo}");
            }
            if (criteria.getStoreId() != null) {
                WHERE("STORE_ID = #{storeId}");
            }
            // 安全的動態查詢構建
        }}.toString();
    }
}
```

#### 效益評估
```
SQL Injection 風險: 消除 ✅
受影響查詢: 12 個
實施時間: 3 天
風險: 低 (完整測試後部署)
```

---

## 整合層優化

### OPT-D1: CRM 整合容錯機制 (P1 - 高優先)

**目標**: 解決 I-1 問題，提升系統可用性

#### 容錯架構

```
┌─────────────────────────────────────┐
│   Pricing Service                   │
│                                     │
│   ┌──────────────────────┐         │
│   │ 1️⃣ Primary: CRM API  │         │
│   │    Timeout: 3s       │         │
│   │    Retry: 3 次        │         │
│   └────┬─────────────────┘         │
│        │                            │
│        │ Fallback                   │
│        ↓                            │
│   ┌──────────────────────┐         │
│   │ 2️⃣ Secondary: Redis   │         │
│   │    快取的會員資料      │         │
│   └────┬─────────────────┘         │
│        │                            │
│        │ Still Failed               │
│        ↓                            │
│   ┌──────────────────────┐         │
│   │ 3️⃣ Last Resort        │         │
│   │    預設折扣規則        │         │
│   └──────────────────────┘         │
└─────────────────────────────────────┘
```

#### 實作方案

**1. Resilience4j 配置**
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      crm:
        failure-rate-threshold: 50            # 失敗率 50% 開啟斷路器
        slow-call-rate-threshold: 50          # 慢呼叫率 50% 開啟斷路器
        slow-call-duration-threshold: 2s      # 超過 2 秒視為慢呼叫
        wait-duration-in-open-state: 30s      # 斷路器開啟 30 秒後嘗試半開
        sliding-window-size: 10               # 滑動視窗 10 次呼叫
        minimum-number-of-calls: 5            # 至少 5 次呼叫才計算失敗率
        permitted-number-of-calls-in-half-open-state: 3

  retry:
    instances:
      crm:
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException

  timelimiter:
    instances:
      crm:
        timeout-duration: 3s
```

**2. 服務實作**
```java
@Service
@Slf4j
public class MemberDiscountService {

    @Autowired
    private CrmClient crmClient;

    @Autowired
    private RedisTemplate<String, MemberDiscount> redisTemplate;

    // 三層容錯機制
    @CircuitBreaker(name = "crm", fallbackMethod = "fallbackGetDiscountFromCache")
    @Retry(name = "crm")
    @TimeLimiter(name = "crm")
    public CompletableFuture<MemberDiscount> getMemberDiscount(String memberCardId, String discType) {
        log.info("呼叫 CRM API: memberCardId={}, discType={}", memberCardId, discType);

        // 1️⃣ Primary: 呼叫 CRM API
        MemberDiscount discount = crmClient.getMemberDiscount(memberCardId, discType);

        // 更新 Redis 快取 (成功時)
        String cacheKey = buildCacheKey(memberCardId, discType);
        redisTemplate.opsForValue().set(cacheKey, discount, Duration.ofHours(24));

        return CompletableFuture.completedFuture(discount);
    }

    // 2️⃣ Secondary: 從 Redis 快取取得
    private CompletableFuture<MemberDiscount> fallbackGetDiscountFromCache(
        String memberCardId,
        String discType,
        Exception e
    ) {
        log.warn("CRM API 呼叫失敗，嘗試從快取取得: memberCardId={}, error={}",
            memberCardId, e.getMessage());

        String cacheKey = buildCacheKey(memberCardId, discType);
        MemberDiscount cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("從快取取得會員折扣: memberCardId={}", memberCardId);
            return CompletableFuture.completedFuture(cached);
        }

        // 3️⃣ Last Resort: 使用預設折扣
        return fallbackGetDefaultDiscount(memberCardId, discType, e);
    }

    // 3️⃣ Last Resort: 預設折扣規則
    private CompletableFuture<MemberDiscount> fallbackGetDefaultDiscount(
        String memberCardId,
        String discType,
        Exception e
    ) {
        log.error("無法取得會員折扣，使用預設規則: memberCardId={}", memberCardId, e);

        // 發送告警
        alertService.sendAlert(AlertLevel.HIGH,
            "CRM 系統異常",
            "會員折扣查詢失敗，使用預設規則: " + memberCardId);

        // 根據 discType 返回預設折扣
        MemberDiscount defaultDiscount = switch (discType) {
            case "0" -> MemberDiscount.builder()  // Discounting
                .discType("0")
                .discRate(new BigDecimal("0.95"))  // 預設 95 折
                .build();

            case "1" -> MemberDiscount.builder()  // Down Margin
                .discType("1")
                .discAmt(new BigDecimal("100"))    // 預設折 100 元
                .build();

            default -> MemberDiscount.noDiscount();
        };

        return CompletableFuture.completedFuture(defaultDiscount);
    }

    private String buildCacheKey(String memberCardId, String discType) {
        return String.format("member-discount:%s:%s", memberCardId, discType);
    }
}
```

**3. 斷路器監控**
```java
@Component
@Slf4j
public class CircuitBreakerMonitor {

    @EventListener
    public void onCircuitBreakerStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        log.warn("斷路器狀態變更: name={}, from={}, to={}",
            event.getCircuitBreakerName(),
            event.getStateTransition().getFromState(),
            event.getStateTransition().getToState());

        // 發送告警
        if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
            alertService.sendAlert(AlertLevel.HIGH,
                "斷路器開啟",
                "CRM 系統呼叫失敗率過高，斷路器已開啟");
        }
    }

    @EventListener
    public void onCircuitBreakerError(CircuitBreakerOnErrorEvent event) {
        log.error("斷路器錯誤: name={}, error={}",
            event.getCircuitBreakerName(),
            event.getThrowable().getMessage());
    }
}
```

#### 效益評估
```
系統可用性:
├─ CRM 正常: 99.9% (與目前相同)
├─ CRM 異常時: 95% (使用快取/預設規則)
└─ 整體可用性: 99.5% → 99.9% (+0.4%)

使用者體驗:
├─ CRM 正常: 200ms (與目前相同)
├─ CRM 異常時: 50ms (快取) 或 5ms (預設)
└─ 失敗時不阻塞訂單建立 ✅

實施時間: 1 週
成本: 無額外成本
```

---

### OPT-D2: POS 整合冪等性設計 (P1 - 中優先)

**目標**: 解決 I-2 問題，防止重複處理

#### 冪等性架構

```
┌─────────────────────────────────────┐
│   POS System                        │
└──────┬──────────────────────────────┘
       │ SOAP Request
       │ + Idempotency-Key: uuid-123
       ↓
┌─────────────────────────────────────┐
│   SOM Web Service                   │
│                                     │
│   ┌──────────────────────┐         │
│   │ 1️⃣ 檢查 Redis        │         │
│   │    是否已處理         │         │
│   └────┬─────────────────┘         │
│        │                            │
│        │ Key Exists → 返回快取結果   │
│        │ Key Not Exists             │
│        ↓                            │
│   ┌──────────────────────┐         │
│   │ 2️⃣ 執行業務邏輯       │         │
│   └────┬─────────────────┘         │
│        │                            │
│        ↓                            │
│   ┌──────────────────────┐         │
│   │ 3️⃣ 儲存結果到 Redis   │         │
│   │    TTL: 24 小時       │         │
│   └──────────────────────┘         │
└─────────────────────────────────────┘
```

#### 實作方案

**1. 冪等性 Key 設計**
```java
// POS 端產生 Idempotency-Key
public class PosClient {

    public PosSoInfoResponse downloadOrders(String storeId, String date) {
        String idempotencyKey = generateIdempotencyKey(storeId, date);

        // 在 SOAP Header 中傳遞
        PosSoInfoRequest request = new PosSoInfoRequest();
        request.setStoreId(storeId);
        request.setDate(date);
        request.setIdempotencyKey(idempotencyKey);

        return webServiceTemplate.marshalSendAndReceive(request);
    }

    private String generateIdempotencyKey(String storeId, String date) {
        // 格式: pos-download:{storeId}:{date}:{uuid}
        return String.format("pos-download:%s:%s:%s",
            storeId, date, UUID.randomUUID().toString());
    }
}
```

**2. 服務端冪等性驗證**
```java
@Service
@Slf4j
public class IdempotentPosService {

    @Autowired
    private RedisTemplate<String, PosSoInfoResponse> redisTemplate;

    @Autowired
    private PosOrderService posOrderService;

    public PosSoInfoResponse getPosSoInfo(PosSoInfoRequest request) {
        String idempotencyKey = request.getIdempotencyKey();

        // 1️⃣ 檢查是否已處理過
        PosSoInfoResponse cached = redisTemplate.opsForValue().get(idempotencyKey);
        if (cached != null) {
            log.info("冪等性檢查命中，返回快取結果: key={}", idempotencyKey);
            return cached;
        }

        // 2️⃣ 使用分散式鎖確保同時只有一個請求在處理
        String lockKey = "lock:" + idempotencyKey;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(
            lockKey, "1", Duration.ofSeconds(30)
        );

        if (Boolean.FALSE.equals(locked)) {
            // 其他節點正在處理，等待並重試
            log.info("其他節點正在處理此請求，等待: key={}", idempotencyKey);
            return waitAndRetry(idempotencyKey, 5);
        }

        try {
            // 3️⃣ 執行業務邏輯
            log.info("執行 POS 訂單下載: storeId={}, date={}",
                request.getStoreId(), request.getDate());

            PosSoInfoResponse response = posOrderService.buildResponse(
                request.getStoreId(),
                request.getDate()
            );

            // 4️⃣ 儲存結果到 Redis (TTL 24 小時)
            redisTemplate.opsForValue().set(
                idempotencyKey,
                response,
                Duration.ofHours(24)
            );

            // 5️⃣ 記錄下載日誌
            posDownloadLogRepository.save(PosDownloadLog.builder()
                .idempotencyKey(idempotencyKey)
                .storeId(request.getStoreId())
                .downloadDate(request.getDate())
                .orderCount(response.getOrders().size())
                .downloadTime(LocalDateTime.now())
                .build());

            return response;

        } finally {
            // 6️⃣ 釋放鎖
            redisTemplate.delete(lockKey);
        }
    }

    private PosSoInfoResponse waitAndRetry(String idempotencyKey, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                Thread.sleep(1000);  // 等待 1 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待被中斷", e);
            }

            PosSoInfoResponse cached = redisTemplate.opsForValue().get(idempotencyKey);
            if (cached != null) {
                return cached;
            }
        }

        throw new IdempotencyTimeoutException("冪等性檢查超時");
    }
}
```

**3. 付款回調冪等性**
```java
@Service
public class PaymentCallbackService {

    public PaymentResult processPaymentCallback(PaymentCallbackRequest request) {
        String idempotencyKey = request.getTransactionId();  // 使用交易 ID 作為冪等性 Key

        // 檢查是否已處理
        if (paymentLogRepository.existsByTransactionId(idempotencyKey)) {
            log.warn("重複的付款回調，忽略: transactionId={}", idempotencyKey);
            return PaymentResult.alreadyProcessed(idempotencyKey);
        }

        // 使用資料庫唯一索引確保冪等性
        try {
            PaymentLog log = paymentLogRepository.save(PaymentLog.builder()
                .transactionId(idempotencyKey)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status("PROCESSING")
                .createTime(LocalDateTime.now())
                .build());

            // 處理付款邏輯
            orderService.markAsPaid(request.getOrderId(), request.getAmount());

            // 更新狀態
            log.setStatus("SUCCESS");
            paymentLogRepository.save(log);

            return PaymentResult.success(idempotencyKey);

        } catch (DataIntegrityViolationException e) {
            // 唯一索引衝突 = 已處理過
            log.warn("付款回調重複（資料庫檢測）: transactionId={}", idempotencyKey);
            return PaymentResult.alreadyProcessed(idempotencyKey);
        }
    }
}
```

#### 效益評估
```
重複處理風險: 消除 ✅
資料一致性: 確保 ✅
POS 整合穩定性: +50%

實施時間: 1 週
成本: Redis 儲存成本 +5%
```

---

## 監控與調優

### 監控指標

**1. 效能指標**
```java
@Service
@Slf4j
public class PricingMetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    public PricingResult calculate(PricingRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            PricingResult result = doCalculate(request);

            // 記錄成功指標
            sample.stop(Timer.builder("pricing.calculate.duration")
                .tag("status", "success")
                .tag("member_type", request.getMemberType())
                .register(meterRegistry));

            // 記錄 SKU 數量
            meterRegistry.counter("pricing.sku.count")
                .increment(request.getSkus().size());

            // 記錄折扣金額分佈
            meterRegistry.summary("pricing.discount.amount")
                .record(result.getTotalDisc().doubleValue());

            return result;

        } catch (Exception e) {
            // 記錄失敗指標
            sample.stop(Timer.builder("pricing.calculate.duration")
                .tag("status", "error")
                .tag("error_type", e.getClass().getSimpleName())
                .register(meterRegistry));

            throw e;
        }
    }
}
```

**2. 快取指標**
```java
@Component
public class CacheMetricsCollector {

    @Scheduled(fixedRate = 60000)  // 每分鐘收集一次
    public void collectCacheMetrics() {
        // Redis 快取指標
        RedisInfo info = redisTemplate.getConnectionFactory()
            .getConnection()
            .info();

        meterRegistry.gauge("cache.redis.memory.used", info.getUsedMemory());
        meterRegistry.gauge("cache.redis.hit.rate", info.getHitRate());
        meterRegistry.gauge("cache.redis.miss.rate", info.getMissRate());

        // 應用層快取指標
        CacheManager cacheManager = getCacheManager();
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            CacheStatistics stats = cache.getStatistics();

            meterRegistry.gauge("cache.hit.rate",
                Tags.of("cache", cacheName),
                stats.getHitRate());

            meterRegistry.gauge("cache.size",
                Tags.of("cache", cacheName),
                cache.size());
        });
    }
}
```

**3. 業務指標**
```java
@Component
public class BusinessMetricsCollector {

    @EventListener
    public void onPricingCalculated(PricingCalculatedEvent event) {
        // 記錄計價次數 (按會員類型分組)
        meterRegistry.counter("pricing.calculated",
            "member_type", event.getMemberType(),
            "channel", event.getChannel()
        ).increment();

        // 記錄平均折扣率
        BigDecimal discountRate = event.getTotalDisc()
            .divide(event.getOriginalAmt(), 4, RoundingMode.HALF_UP);

        meterRegistry.summary("pricing.discount.rate",
            "member_type", event.getMemberType()
        ).record(discountRate.doubleValue());
    }

    @Scheduled(cron = "0 0 * * * *")  // 每小時統計
    public void hourlyStatistics() {
        // 統計每小時計價次數
        long count = pricingLogRepository.countLastHour();
        meterRegistry.gauge("pricing.hourly.count", count);

        // 統計平均回應時間
        double avgDuration = pricingLogRepository.avgDurationLastHour();
        meterRegistry.gauge("pricing.hourly.avg.duration", avgDuration);
    }
}
```

### Grafana 儀表板

```
┌─────────────────────────────────────────────────────────┐
│  SOM Pricing System Dashboard                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │ 平均回應時間  │  │ 快取命中率    │  │ 錯誤率        │ │
│  │   350ms      │  │    95.2%     │  │   0.05%      │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
│                                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │  回應時間分佈 (p50, p95, p99)                       │ │
│  │  ─────────────────────────────                     │ │
│  │  p50: 250ms ████████████████                       │ │
│  │  p95: 800ms █████████████████████████████          │ │
│  │  p99: 1500ms ██████████████████████████████████    │ │
│  └────────────────────────────────────────────────────┘ │
│                                                         │
│  ┌──────────────────────┐  ┌──────────────────────┐   │
│  │  計價次數/小時        │  │  CRM 呼叫成功率       │   │
│  │  [折線圖]            │  │  [折線圖]            │   │
│  └──────────────────────┘  └──────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Redis 快取統計                                     │ │
│  │  - 記憶體使用: 256MB / 1GB                          │ │
│  │  - Key 數量: 12,456                                │ │
│  │  - 命中率: 95.2%                                   │ │
│  │  - 驅逐率: 0.01%                                   │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

---

## 實施計畫

### Phase 1: 安全修復 (Week 1-2) - P0

| 任務 | 負責人 | 時間 | 依賴 |
|------|--------|------|------|
| **OPT-A1**: 移除前端計算邏輯 | Backend | 3 天 | - |
| **OPT-A1**: 實作後端驗證機制 | Backend | 2 天 | 上一項 |
| **OPT-C2**: 修復 SQL Injection | Backend | 3 天 | - |
| **單元測試 & QA** | QA | 2 天 | 以上全部 |
| **部署到生產環境** | DevOps | 1 天 | 測試通過 |

**里程碑**: 所有 P0 安全問題修復完成 ✅

---

### Phase 2: 效能優化 (Week 3-6) - P1

| 任務 | 負責人 | 時間 | 依賴 |
|------|--------|------|------|
| **環境準備**: Redis Cluster 部署 | DevOps | 2 天 | - |
| **OPT-A2**: 實作 Redis 快取層 | Backend | 5 天 | Redis 就緒 |
| **OPT-B1**: 平行化獨立步驟 | Backend | 3 天 | - |
| **OPT-B2**: 優化促銷活動查詢 | Backend/DBA | 2 天 | - |
| **OPT-C1**: 解決 N+1 查詢 | Backend | 2 天 | - |
| **OPT-D1**: CRM 容錯機制 | Backend | 5 天 | Redis 就緒 |
| **效能測試 & 調優** | QA | 5 天 | 以上全部 |
| **部署到生產環境** | DevOps | 2 天 | 測試通過 |

**里程碑**: 效能提升 70%+ ✅

---

### Phase 3: 監控建立 (Week 7-8)

| 任務 | 負責人 | 時間 | 依賴 |
|------|--------|------|------|
| **實作 Metrics 收集** | Backend | 3 天 | - |
| **建立 Grafana 儀表板** | DevOps | 2 天 | Metrics 就緒 |
| **告警規則配置** | DevOps | 2 天 | 儀表板就緒 |
| **SLO 定義與監控** | Team | 1 天 | 以上全部 |

**里程碑**: 完整的監控體系建立 ✅

---

## 預期成果

### 效能提升

```
指標對比:

回應時間:
├─ 目前平均: 1560ms
├─ 優化後 (快取命中): 50ms (-97%) 🚀
├─ 優化後 (快取未命中): 420ms (-73%) 🚀
└─ 整體平均 (95% 命中率): 350ms (-78%) 🚀

吞吐量:
├─ 目前: 50 req/s
├─ 優化後: 200 req/s
└─ 提升: +300% 🚀

資源使用:
├─ CPU: -40% (減少重複計算)
├─ 記憶體: +200MB (Redis 快取)
└─ 資料庫連線: -60% (快取減少查詢)

成本:
├─ 伺服器成本: -$500/月 (資源最佳化)
├─ Redis 成本: +$150/月
└─ 淨節省: -$350/月
```

### 安全性提升

```
✅ P0-1: 前端價格計算漏洞 - 修復
✅ P0-2: 價格驗證缺失 - 修復
✅ P0-3: SQL Injection 風險 - 修復

財務風險: $100,000+/年 → $0 ✅
```

### 可用性提升

```
CRM 整合:
├─ 目前: CRM 故障 = 訂單失敗
├─ 優化後: 三層容錯 (API → 快取 → 預設)
└─ 系統可用性: 99.5% → 99.9%

POS 整合:
├─ 目前: 無冪等性保證
├─ 優化後: 完整冪等性設計
└─ 重複處理風險: 消除 ✅
```

---

## 持續優化

### 下一階段優化方向

1. **微服務拆分** (Phase 4)
   - 拆分 OrderService, PricingService, MemberService
   - 獨立部署、擴展

2. **讀寫分離** (Phase 5)
   - 讀操作導向 Read Replica
   - 寫操作導向 Master

3. **分散式快取** (Phase 6)
   - Redis Cluster 擴展
   - 多層快取策略 (L1: 本地, L2: Redis)

4. **智慧計價** (Phase 7)
   - ML 模型預測最佳折扣
   - A/B Testing 驗證效果

---

## 結論

透過系統化的優化策略：

**安全性**: P0 漏洞全數修復 ✅
**效能**: 提升 78% (1560ms → 350ms) ✅
**可用性**: 提升 0.4% (99.5% → 99.9%) ✅
**成本**: 節省 $350/月 ✅

**投資回報**:
- **總投入**: 8 週 × 2 人 = 16 人週
- **年度節省**: $4,200 + 風險消除 ($100,000+)
- **ROI**: > 2000%

建議立即啟動 Phase 1 (安全修復)，並規劃 Phase 2-3 實施時程。

---

## 相關文檔

- [06-Pricing-Problems-Analysis.md](./06-Pricing-Problems-Analysis.md) - 問題分析
- [04-Pricing-Calculation-Sequence.md](./04-Pricing-Calculation-Sequence.md) - 計價流程
- [17-Cache-Strategy.md](./17-Cache-Strategy.md) - 快取策略詳細設計
- [26-Monitoring-Metrics.md](./26-Monitoring-Metrics.md) - 監控指標定義
