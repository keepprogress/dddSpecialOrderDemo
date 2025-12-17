# 17. Cache Strategy - Redis 快取策略

## 目錄

- [1. 快取架構](#1-快取架構)
- [2. 快取層級設計](#2-快取層級設計)
- [3. 快取鍵設計](#3-快取鍵設計)
- [4. TTL 策略](#4-ttl-策略)
- [5. 快取更新策略](#5-快取更新策略)
- [6. 快取預熱](#6-快取預熱)
- [7. 快取穿透與雪崩防護](#7-快取穿透與雪崩防護)
- [8. 效能監控](#8-效能監控)

---

## 1. 快取架構

### 1.1 多層快取架構

```plaintext
┌─────────────────────────────────────────────────────────────┐
│                    Multi-Layer Cache                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  L1: Application Cache (Caffeine)                           │
│  ┌────────────────────────────────────────────────────┐    │
│  │ TTL: 1-5 分鐘                                       │    │
│  │ Size: 10,000 entries                               │    │
│  │ Eviction: LRU                                      │    │
│  │ Use Case: 熱點資料 (商品資訊、會員等級)             │    │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│                           ▼ (Miss)                           │
│                                                              │
│  L2: Distributed Cache (Redis)                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │ TTL: 5-30 分鐘                                      │    │
│  │ Memory: 8 GB                                       │    │
│  │ Persistence: AOF + RDB                             │    │
│  │ Use Case: 計價結果、會員資訊、促銷規則              │    │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│                           ▼ (Miss)                           │
│                                                              │
│  L3: Database Cache (Oracle Result Cache)                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │ TTL: 自動管理                                       │    │
│  │ Use Case: 熱門查詢結果                              │    │
│  └────────────────────────────────────────────────────┘    │
│                           │                                  │
│                           ▼ (Miss)                           │
│                                                              │
│  L4: Database (Oracle)                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │ SSD Storage                                        │    │
│  │ B-Tree Index                                       │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘

查詢流程:
1. 檢查 L1 (Caffeine) → Hit: 回傳 (1-2ms)
2. Miss → 檢查 L2 (Redis) → Hit: 回傳 + 寫入 L1 (5-10ms)
3. Miss → 檢查 L3 (Oracle Cache) → Hit: 回傳 + 寫入 L2 + L1 (20-50ms)
4. Miss → 查詢 L4 (Database) → 回傳 + 寫入 L3 + L2 + L1 (100-500ms)
```

### 1.2 Redis 集群架構

```yaml
Redis Cluster Configuration:
  Mode: Cluster (3 Master + 3 Replica)

  Master Nodes:
    - redis-master-1:6379  # Pricing Service
    - redis-master-2:6379  # Member Service
    - redis-master-3:6379  # Order/Payment Service

  Replica Nodes:
    - redis-replica-1:6379  # Master-1 備援
    - redis-replica-2:6379  # Master-2 備援
    - redis-replica-3:6379  # Master-3 備援

  Sentinel:
    - redis-sentinel-1:26379
    - redis-sentinel-2:26379
    - redis-sentinel-3:26379
    Quorum: 2
    Down-After: 5000ms
    Failover-Timeout: 60000ms
```

**Spring Boot 設定**:

```yaml
spring:
  redis:
    cluster:
      nodes:
        - redis-master-1:6379
        - redis-master-2:6379
        - redis-master-3:6379
      max-redirects: 3
    lettuce:
      pool:
        max-active: 50      # 最大連線數
        max-idle: 20        # 最大閒置連線
        min-idle: 5         # 最小閒置連線
        max-wait: 2000ms    # 最大等待時間
      shutdown-timeout: 100ms
    timeout: 2000ms         # 命令超時
```

---

## 2. 快取層級設計

### 2.1 Pricing Service - 計價快取

#### 2.1.1 完整計價結果快取

```java
/**
 * 快取完整計價結果
 * Key: pricing:result:{memberCardId}:{skusHash}
 * TTL: 5 分鐘
 * Hit Rate: 65%
 * Improvement: 1560ms → 420ms (-73%)
 */
@Cacheable(
    value = "pricing:result",
    key = "#memberCardId + ':' + T(com.trihome.som.util.HashUtil).md5(#request.toString())",
    unless = "#result == null"
)
public PricingResponse calculatePrice(String memberCardId, PricingRequest request) {
    // 執行完整計價邏輯 (12 步驟)
    return pricingEngine.calculate(request);
}

// Redis 儲存格式 (JSON)
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "summary": {
    "originalTotal": 12000.00,
    "discountTotal": 2500.00,
    "finalTotal": 9500.00
  },
  "computes": [
    {"type": "1", "name": "商品小計", "amount": 10000.00},
    {"type": "2", "name": "安裝小計", "amount": 2000.00}
    // ... 其他 ComputeTypes
  ],
  "items": [...],
  "calculatedAt": "2025-10-27T10:30:00Z",
  "cacheHit": false
}
```

**效能數據**:

| 場景 | 無快取 | Redis 快取 | 改善 |
|-----|--------|-----------|------|
| 首次計價 | 1560ms | 1560ms | 0% |
| 重複計價 | 1560ms | 420ms | -73% |
| 並發 100 req/s | CPU 80% | CPU 20% | -75% |

#### 2.1.2 會員折扣快取

```java
/**
 * 快取會員折扣資訊
 * Key: pricing:member:discount:{memberCardId}
 * TTL: 30 分鐘 (與 CRM 同步週期一致)
 * Hit Rate: 95%
 * Improvement: 200ms (CRM API) → 5ms (-97.5%)
 */
@Cacheable(
    value = "pricing:member:discount",
    key = "#memberCardId",
    unless = "#result == null"
)
public MemberDiscount getMemberDiscount(String memberCardId) {
    // 呼叫 CRM API 或 Member Service
    return memberService.getDiscount(memberCardId);
}

// Redis 儲存格式 (JSON)
{
  "memberCardId": "A123456789",
  "discountType": "2",           // 0:折扣率 1:固定折扣 2:成本加成
  "discountValue": 1.35,         // 成本加成 1.35 倍
  "priority": 1,
  "effectiveDate": "2025-01-01",
  "expiryDate": "2025-12-31",
  "cachedAt": "2025-10-27T10:30:00Z"
}
```

#### 2.1.3 促銷規則快取

```java
/**
 * 快取促銷規則
 * Key: pricing:promotion:active
 * TTL: 10 分鐘
 * Hit Rate: 90%
 * Improvement: 300ms (DB query) → 5ms (-98%)
 */
@Cacheable(
    value = "pricing:promotion:active",
    key = "'all'",
    unless = "#result == null || #result.isEmpty()"
)
public List<Promotion> getActivePromotions() {
    LocalDateTime now = LocalDateTime.now();
    return promotionRepository.findActivePromotions(now);
}

// Redis 儲存格式 (JSON Array)
[
  {
    "promotionId": "PROMO001",
    "name": "滿萬折千",
    "type": "AMOUNT_OFF",
    "condition": {"minAmount": 10000},
    "discount": {"amount": 1000},
    "startDate": "2025-10-01T00:00:00Z",
    "endDate": "2025-10-31T23:59:59Z",
    "priority": 10
  },
  {
    "promotionId": "PROMO002",
    "name": "第二件五折",
    "type": "BUY_X_GET_Y",
    "condition": {"quantity": 2},
    "discount": {"percentage": 0.5},
    "priority": 20
  }
]
```

### 2.2 Member Service - 會員快取

#### 2.2.1 會員基本資訊快取

```java
/**
 * 快取會員基本資訊 (來自 CRM)
 * Key: member:info:{memberCardId}
 * TTL: 30 分鐘
 * Hit Rate: 95%
 * Improvement: 200ms (CRM SOAP API) → 5ms (-97.5%)
 */
@Cacheable(
    value = "member:info",
    key = "#memberCardId",
    unless = "#result == null"
)
public MemberInfo getMemberInfo(String memberCardId) {
    // 呼叫 CRM SOAP API
    return crmClient.getMemberInfo(memberCardId);
}

// Redis 儲存格式 (JSON)
{
  "memberCardId": "A123456789",
  "name": "王小明",
  "level": "VIP",
  "phone": "0912345678",
  "email": "wang@example.com",
  "birthday": "1985-05-20",
  "registeredDate": "2020-01-15",
  "totalPoints": 15000,
  "availablePoints": 12500,
  "crmSyncedAt": "2025-10-27T10:30:00Z"
}
```

#### 2.2.2 會員訂單統計快取

```java
/**
 * 快取會員訂單統計
 * Key: member:stats:{memberCardId}
 * TTL: 60 分鐘
 * Hit Rate: 80%
 */
@Cacheable(
    value = "member:stats",
    key = "#memberCardId"
)
public MemberStats getMemberStats(String memberCardId) {
    return memberStatsRepository.calculateStats(memberCardId);
}

// Redis 儲存格式 (JSON)
{
  "memberCardId": "A123456789",
  "totalOrders": 25,
  "totalAmount": 250000.00,
  "averageOrderAmount": 10000.00,
  "lastOrderDate": "2025-10-15",
  "favoriteCategories": ["家具", "家電"],
  "calculatedAt": "2025-10-27T10:30:00Z"
}
```

### 2.3 Order Service - 訂單快取

#### 2.3.1 訂單詳情快取 (只讀訂單)

```java
/**
 * 快取訂單詳情 (狀態 = 已結案/作廢)
 * Key: order:detail:{orderId}
 * TTL: 永久 (直到明確刪除)
 * Hit Rate: 85%
 * Use Case: 已完成訂單不會再變更, 可永久快取
 */
@Cacheable(
    value = "order:detail",
    key = "#orderId",
    condition = "#result != null && (#result.status == '5' || #result.status == '6')"
)
public OrderDetail getOrderDetail(String orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    return OrderDetail.from(order);
}
```

#### 2.3.2 會員訂單清單快取

```java
/**
 * 快取會員訂單清單 (分頁)
 * Key: order:list:{memberCardId}:page:{page}:size:{size}
 * TTL: 5 分鐘
 * Hit Rate: 60%
 */
@Cacheable(
    value = "order:list",
    key = "#memberCardId + ':page:' + #page + ':size:' + #size"
)
public PageResponse<OrderSummary> getOrdersByMember(
    String memberCardId, int page, int size
) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    Page<Order> orders = orderRepository.findByMemberCardId(memberCardId, pageable);
    return PageResponse.from(orders, OrderSummary::from);
}
```

### 2.4 Payment Service - 付款快取

#### 2.4.1 冪等鍵快取

```java
/**
 * 快取冪等鍵處理結果
 * Key: payment:idempotency:{idempotencyKey}
 * TTL: 24 小時
 * Hit Rate: 5-10% (網路重試、用戶重複點擊)
 * Impact: 防止重複扣款
 */
@Cacheable(
    value = "payment:idempotency",
    key = "#idempotencyKey",
    unless = "#result == null"
)
public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
    // 檢查是否已處理過
    String cacheKey = "payment:idempotency:" + idempotencyKey;
    PaymentResponse cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        log.info("Idempotency key hit: {}", idempotencyKey);
        return cached;  // 回傳快取結果, 不重複處理
    }

    // 處理付款
    PaymentResponse response = paymentProcessor.process(request);

    // 快取結果 (24 小時)
    redisTemplate.opsForValue().set(
        cacheKey, response, Duration.ofHours(24)
    );

    return response;
}
```

---

## 3. 快取鍵設計

### 3.1 命名規範

```plaintext
格式: {service}:{resource}:{identifier}:{subkey}

範例:
- pricing:result:A123456789:md5hash123        # 計價結果
- pricing:member:discount:A123456789          # 會員折扣
- pricing:promotion:active                    # 活動促銷
- member:info:A123456789                      # 會員資訊
- member:stats:A123456789                     # 會員統計
- order:detail:SO20251027001                  # 訂單詳情
- order:list:A123456789:page:0:size:20        # 訂單清單
- payment:idempotency:unique-key-123          # 冪等鍵
```

**優點**:

1. **可讀性高**: 一眼看出資料類型與歸屬服務
2. **便於管理**: 支援批次刪除 (`DEL pricing:*`)
3. **避免衝突**: 服務前綴確保跨服務鍵不重複
4. **監控友善**: 可按服務統計快取命中率

### 3.2 鍵長度優化

```java
// ❌ 錯誤: 鍵過長 (84 bytes)
String key = "pricing:result:memberCardId:A123456789:skus:[{skuNo:SKU001,qty:2},{skuNo:SKU002,qty:1}]";

// ✅ 正確: 使用 Hash 縮短鍵 (48 bytes)
String skusJson = objectMapper.writeValueAsString(request.getSkus());
String skusHash = DigestUtils.md5Hex(skusJson);
String key = "pricing:result:A123456789:" + skusHash;  // 48 bytes

// 效能改善:
// - 記憶體使用: 84 bytes → 48 bytes (-43%)
// - 網路傳輸: 減少 36 bytes/key
// - 10M keys: 840 MB → 480 MB 記憶體節省
```

### 3.3 鍵過期策略

```java
@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))  // 預設 TTL: 5 分鐘
            .disableCachingNullValues()       // 不快取 null 值
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            );
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> builder
            .withCacheConfiguration("pricing:result",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("pricing:member:discount",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("pricing:promotion:active",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration("member:info",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("order:detail",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ZERO))  // 永不過期 (已結案訂單)
            .withCacheConfiguration("payment:idempotency",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(24)));
    }
}
```

---

## 4. TTL 策略

### 4.1 TTL 決策矩陣

| 資料類型 | 變更頻率 | 查詢頻率 | TTL | 原因 |
|---------|---------|---------|-----|------|
| 計價結果 | 中 (促銷異動) | 極高 | 5 分鐘 | 平衡即時性與效能 |
| 會員折扣 | 低 (合約期間) | 高 | 30 分鐘 | CRM 同步週期 |
| 促銷規則 | 中 (每日更新) | 高 | 10 分鐘 | 確保促銷即時生效 |
| 會員資訊 | 低 (基本資料) | 高 | 30 分鐘 | CRM 同步週期 |
| 訂單詳情 (已結案) | 無 | 中 | 永久 | 不可變資料 |
| 訂單清單 | 高 (新增訂單) | 中 | 5 分鐘 | 確保清單更新 |
| 冪等鍵 | 低 (一次性) | 極低 | 24 小時 | 防止誤刪 |

### 4.2 動態 TTL 策略

```java
/**
 * 根據業務邏輯動態調整 TTL
 */
public void cacheOrderDetail(String orderId, OrderDetail detail) {
    String key = "order:detail:" + orderId;

    // 根據訂單狀態決定 TTL
    Duration ttl = switch (detail.getStatus()) {
        case "5", "6" -> Duration.ZERO;           // 已結案/作廢: 永久
        case "3" -> Duration.ofHours(24);         // 已付款: 24 小時
        case "4" -> Duration.ofHours(1);          // 有效: 1 小時
        case "1", "2" -> Duration.ofMinutes(5);   // 草稿/報價: 5 分鐘
        default -> Duration.ofMinutes(5);
    };

    if (ttl.isZero()) {
        redisTemplate.opsForValue().set(key, detail);  // 永不過期
    } else {
        redisTemplate.opsForValue().set(key, detail, ttl);
    }
}
```

---

## 5. 快取更新策略

### 5.1 Cache-Aside Pattern (推薦)

```java
/**
 * Cache-Aside Pattern
 * 1. 讀取: 先查快取 → Miss 則查 DB → 寫入快取
 * 2. 更新: 先更新 DB → 刪除快取 (不更新快取)
 */
@Service
public class OrderService {

    @Autowired
    private RedisTemplate<String, OrderDetail> redisTemplate;

    @Autowired
    private OrderRepository orderRepository;

    // 讀取
    public OrderDetail getOrderDetail(String orderId) {
        String key = "order:detail:" + orderId;

        // 1. 查詢快取
        OrderDetail cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // 2. 查詢資料庫
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderDetail detail = OrderDetail.from(order);

        // 3. 寫入快取
        redisTemplate.opsForValue().set(key, detail, Duration.ofMinutes(5));

        return detail;
    }

    // 更新
    @CacheEvict(value = "order:detail", key = "#orderId")
    public void updateOrderStatus(String orderId, String newStatus) {
        // 1. 更新資料庫
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setStatus(newStatus);
        orderRepository.save(order);

        // 2. 刪除快取 (由 @CacheEvict 自動處理)
        // 下次讀取時會重新載入最新資料
    }
}
```

**優點**:

- 邏輯簡單, 易於實作
- 資料一致性高 (刪除而非更新)
- 避免快取與 DB 資料不一致

**缺點**:

- 刪除後首次讀取需查詢 DB (可接受)

### 5.2 Write-Through Pattern (不推薦)

```java
/**
 * Write-Through Pattern
 * 更新: 同時更新 DB 與快取
 *
 * ⚠️ 不推薦原因:
 * 1. 更新順序問題: 先更新 DB 或快取?
 * 2. 並發衝突: 兩個請求同時更新導致不一致
 * 3. 複雜度高: 需處理事務與回滾
 */
public void updateOrderStatus(String orderId, String newStatus) {
    String key = "order:detail:" + orderId;

    // 1. 更新資料庫
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.setStatus(newStatus);
    orderRepository.save(order);

    // 2. 更新快取 ❌ 問題: 並發時可能不一致
    OrderDetail detail = OrderDetail.from(order);
    redisTemplate.opsForValue().set(key, detail, Duration.ofMinutes(5));
}
```

### 5.3 批次失效策略

```java
/**
 * 批次刪除相關快取
 * Use Case: 促銷活動更新後, 刪除所有計價快取
 */
@CacheEvict(value = "pricing:result", allEntries = true)
public void updatePromotion(Promotion promotion) {
    promotionRepository.save(promotion);
    // 刪除所有計價快取, 確保促銷立即生效
}

/**
 * 使用 Redis SCAN 批次刪除
 * Use Case: 會員等級變更後, 刪除該會員所有相關快取
 */
public void evictMemberCaches(String memberCardId) {
    String pattern = "*:" + memberCardId + "*";

    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("Evicted {} cache keys for member: {}", keys.size(), memberCardId);
    }
}
```

---

## 6. 快取預熱

### 6.1 應用啟動時預熱

```java
@Component
public class CacheWarmer implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private MemberService memberService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Starting cache warming...");

        // 1. 預熱促銷規則 (全部載入)
        List<Promotion> promotions = promotionService.getActivePromotions();
        log.info("Warmed {} active promotions", promotions.size());

        // 2. 預熱熱門會員資訊 (Top 1000)
        List<String> topMembers = memberService.getTopMemberIds(1000);
        topMembers.forEach(memberCardId -> {
            try {
                memberService.getMemberInfo(memberCardId);  // 觸發快取
            } catch (Exception e) {
                log.warn("Failed to warm member: {}", memberCardId, e);
            }
        });
        log.info("Warmed {} top members", topMembers.size());

        log.info("Cache warming completed");
    }
}
```

### 6.2 定時預熱

```java
@Component
public class ScheduledCacheWarmer {

    @Autowired
    private PromotionService promotionService;

    /**
     * 每 10 分鐘重新載入促銷規則
     */
    @Scheduled(fixedRate = 600000)  // 10 分鐘
    public void warmPromotions() {
        log.info("Scheduled warming: promotions");
        promotionService.refreshPromotionsCache();
    }

    /**
     * 每小時重新載入熱門會員
     */
    @Scheduled(cron = "0 0 * * * *")  // 每小時整點
    public void warmTopMembers() {
        log.info("Scheduled warming: top members");
        List<String> topMembers = memberService.getTopMemberIds(1000);
        topMembers.forEach(memberService::getMemberInfo);
    }
}
```

---

## 7. 快取穿透與雪崩防護

### 7.1 快取穿透 (Cache Penetration)

**問題**: 大量查詢不存在的資料, 每次都打到 DB

```java
/**
 * 解決方案 1: 快取空值 (推薦)
 */
@Cacheable(
    value = "member:info",
    key = "#memberCardId",
    unless = "false"  // ✅ 也快取 null 值
)
public MemberInfo getMemberInfo(String memberCardId) {
    return memberRepository.findById(memberCardId)
        .orElse(null);  // 回傳 null, 也會被快取 (TTL 5 分鐘)
}

/**
 * 解決方案 2: 布隆過濾器 (Bloom Filter)
 */
@Component
public class MemberBloomFilter {

    private final BloomFilter<String> bloomFilter;

    public MemberBloomFilter() {
        // 預期 100 萬會員, 誤判率 1%
        this.bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            1_000_000,
            0.01
        );
    }

    @PostConstruct
    public void init() {
        // 應用啟動時載入所有會員 ID 到布隆過濾器
        List<String> allMemberIds = memberRepository.findAllMemberCardIds();
        allMemberIds.forEach(bloomFilter::put);
        log.info("Bloom filter initialized with {} members", allMemberIds.size());
    }

    public boolean mightExist(String memberCardId) {
        return bloomFilter.mightContain(memberCardId);
    }
}

@Service
public class MemberService {

    @Autowired
    private MemberBloomFilter bloomFilter;

    public MemberInfo getMemberInfo(String memberCardId) {
        // 1. 先檢查布隆過濾器
        if (!bloomFilter.mightExist(memberCardId)) {
            throw new MemberNotFoundException(memberCardId);  // 一定不存在
        }

        // 2. 查詢快取與資料庫
        return getMemberFromCacheOrDb(memberCardId);
    }
}
```

### 7.2 快取雪崩 (Cache Avalanche)

**問題**: 大量快取同時過期, 瞬間流量打到 DB

```java
/**
 * 解決方案 1: TTL 加隨機值 (推薦)
 */
public void cachePricingResult(String key, PricingResponse result) {
    // TTL: 5 分鐘 + 隨機 0-60 秒
    long baseTtl = 300;  // 5 分鐘
    long randomTtl = ThreadLocalRandom.current().nextLong(0, 60);  // 0-60 秒
    Duration ttl = Duration.ofSeconds(baseTtl + randomTtl);

    redisTemplate.opsForValue().set(key, result, ttl);
}

/**
 * 解決方案 2: 互斥鎖 (Mutex Lock)
 * 防止快取失效時大量請求同時查詢 DB
 */
public PricingResponse calculatePriceWithLock(PricingRequest request) {
    String cacheKey = "pricing:result:" + request.getCacheKey();

    // 1. 查詢快取
    PricingResponse cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return cached;
    }

    // 2. 快取 Miss, 使用分散式鎖
    String lockKey = "lock:pricing:" + request.getCacheKey();
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
        lockKey, "1", Duration.ofSeconds(10)
    );

    if (Boolean.TRUE.equals(acquired)) {
        try {
            // 2.1 獲得鎖, 執行計價
            PricingResponse result = pricingEngine.calculate(request);

            // 2.2 寫入快取
            cachePricingResult(cacheKey, result);

            return result;
        } finally {
            // 2.3 釋放鎖
            redisTemplate.delete(lockKey);
        }
    } else {
        // 2.4 未獲得鎖, 等待 100ms 後重試
        try {
            Thread.sleep(100);
            return calculatePriceWithLock(request);  // 遞迴重試
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for lock", e);
        }
    }
}
```

### 7.3 快取擊穿 (Cache Breakdown)

**問題**: 熱點資料過期, 瞬間大量請求打到 DB

```java
/**
 * 解決方案: 熱點資料永不過期 + 後台定時更新
 */
@Service
public class HotDataCacheService {

    /**
     * 熱門商品快取 (永不過期)
     */
    @PostConstruct
    public void initHotSkus() {
        // 載入熱門商品 (Top 100)
        List<Sku> hotSkus = skuRepository.findTop100ByOrderBySalesDesc();
        hotSkus.forEach(sku -> {
            String key = "sku:info:" + sku.getSkuNo();
            // 永不過期
            redisTemplate.opsForValue().set(key, sku);
        });
    }

    /**
     * 每 5 分鐘更新熱門商品快取
     */
    @Scheduled(fixedRate = 300000)  // 5 分鐘
    public void refreshHotSkus() {
        log.info("Refreshing hot SKUs cache");
        initHotSkus();
    }
}
```

---

## 8. 效能監控

### 8.1 監控指標

```java
@Component
@Aspect
public class CacheMonitoringAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
        String cacheName = cacheable.value()[0];
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = pjp.proceed();

            // 記錄快取命中
            meterRegistry.counter("cache.requests",
                "cache", cacheName,
                "result", "hit"
            ).increment();

            sample.stop(Timer.builder("cache.latency")
                .tag("cache", cacheName)
                .tag("result", "hit")
                .register(meterRegistry));

            return result;
        } catch (Exception e) {
            // 記錄快取 Miss
            meterRegistry.counter("cache.requests",
                "cache", cacheName,
                "result", "miss"
            ).increment();

            sample.stop(Timer.builder("cache.latency")
                .tag("cache", cacheName)
                .tag("result", "miss")
                .register(meterRegistry));

            throw e;
        }
    }
}
```

### 8.2 Prometheus Metrics

```yaml
# Prometheus 監控指標
cache_requests_total{cache="pricing:result", result="hit"} 85000
cache_requests_total{cache="pricing:result", result="miss"} 15000
cache_hit_rate{cache="pricing:result"} 0.85

cache_latency_seconds{cache="pricing:result", result="hit", quantile="0.5"} 0.005
cache_latency_seconds{cache="pricing:result", result="hit", quantile="0.95"} 0.010
cache_latency_seconds{cache="pricing:result", result="miss", quantile="0.5"} 1.560
cache_latency_seconds{cache="pricing:result", result="miss", quantile="0.95"} 2.100

redis_connected_clients 50
redis_memory_used_bytes 2147483648  # 2 GB
redis_commands_processed_total 1000000
```

### 8.3 Grafana Dashboard

```json
{
  "dashboard": {
    "title": "SOM Cache Monitoring",
    "panels": [
      {
        "title": "Cache Hit Rate",
        "targets": [
          {
            "expr": "sum(rate(cache_requests_total{result=\"hit\"}[5m])) / sum(rate(cache_requests_total[5m]))"
          }
        ]
      },
      {
        "title": "Cache Latency (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, cache_latency_seconds_bucket)"
          }
        ]
      },
      {
        "title": "Redis Memory Usage",
        "targets": [
          {
            "expr": "redis_memory_used_bytes"
          }
        ]
      }
    ]
  }
}
```

---

## 總結

### 快取策略核心要點

1. **多層快取**: Caffeine (L1) + Redis (L2) + DB Cache (L3)
2. **TTL 設計**: 根據資料變更頻率決定過期時間 (5分鐘 ~ 永久)
3. **Cache-Aside**: 讀取查快取, 更新刪快取 (不更新)
4. **防護機制**:
   - 穿透: 快取空值 + 布隆過濾器
   - 雪崩: TTL 加隨機值 + 分散式鎖
   - 擊穿: 熱點資料永不過期 + 後台更新
5. **效能提升**:
   - 計價: 1560ms → 420ms (-73%)
   - 會員查詢: 200ms → 5ms (-97.5%)
   - 並發能力: CPU 80% → 20% (-75%)

---

**參考文件**:
- `16-Database-Design.md`: 資料庫設計
- `13-API-Pricing-Service.md`: 計價服務 API
- `15-API-Member-Service.md`: 會員服務 API

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
