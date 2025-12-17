# 10. 後端技術棧 (Backend Tech Stack)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **相關文檔**:
  - [08-Architecture-Overview.md](./08-Architecture-Overview.md)
  - [33-Backend-Project-Setup.md](./33-Backend-Project-Setup.md)
  - [34-Backend-Security-JWT.md](./34-Backend-Security-JWT.md)

---

## 目錄
1. [技術棧總覽](#技術棧總覽)
2. [核心框架 - Spring Boot 3](#核心框架---spring-boot-3)
3. [資料存取 - MyBatis](#資料存取---mybatis)
4. [快取層 - Redis](#快取層---redis)
5. [安全框架 - Spring Security](#安全框架---spring-security)
6. [API 文檔 - SpringDoc](#api-文檔---springdoc)
7. [監控與日誌](#監控與日誌)
8. [建構工具](#建構工具)

---

## 技術棧總覽

### 完整技術清單

```
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot 3 Backend Stack                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  核心框架:                                                       │
│  ├─ Spring Boot 3.1.5 (Latest Stable)                          │
│  ├─ Spring Framework 6.0.13                                    │
│  ├─ Java 17 LTS                                                │
│  └─ Jakarta EE 9+ (javax → jakarta 遷移)                       │
│                                                                 │
│  資料存取:                                                       │
│  ├─ MyBatis 3.5.13                                             │
│  ├─ MyBatis-Spring-Boot-Starter 3.0.2                          │
│  ├─ HikariCP 5.x (Connection Pool)                             │
│  └─ Oracle JDBC Driver 19.x                                    │
│                                                                 │
│  快取:                                                          │
│  ├─ Spring Data Redis 3.1.5                                    │
│  ├─ Redis 7.x                                                  │
│  ├─ Lettuce 6.x (Redis Client)                                │
│  └─ Spring Cache Abstraction                                   │
│                                                                 │
│  安全:                                                          │
│  ├─ Spring Security 6.1.5                                      │
│  ├─ JWT (jjwt 0.11.5)                                          │
│  └─ BCrypt Password Encoder                                    │
│                                                                 │
│  Web & REST:                                                    │
│  ├─ Spring Web MVC                                             │
│  ├─ Jackson 2.15.x (JSON)                                      │
│  ├─ SpringDoc OpenAPI 2.2.0 (Swagger)                          │
│  └─ Hibernate Validator 8.0.x                                  │
│                                                                 │
│  容錯與韌性:                                                     │
│  ├─ Resilience4j 2.1.0                                         │
│  │  ├─ Circuit Breaker                                         │
│  │  ├─ Retry                                                   │
│  │  ├─ Rate Limiter                                            │
│  │  └─ Time Limiter                                            │
│  └─ Caffeine Cache (本地快取)                                  │
│                                                                 │
│  監控與日誌:                                                     │
│  ├─ Spring Boot Actuator 3.1.5                                │
│  ├─ Micrometer 1.11.x (Metrics)                               │
│  ├─ Logback 1.4.x (Logging)                                   │
│  └─ SLF4J 2.0.x                                                │
│                                                                 │
│  測試:                                                          │
│  ├─ JUnit 5 (Jupiter)                                          │
│  ├─ Mockito 5.x                                                │
│  ├─ Spring Boot Test                                           │
│  ├─ TestContainers 1.19.x (Integration Test)                  │
│  └─ AssertJ 3.x                                                │
│                                                                 │
│  建構工具:                                                       │
│  ├─ Maven 3.9.x                                                │
│  ├─ JaCoCo (Code Coverage)                                     │
│  └─ Checkstyle (Code Quality)                                  │
│                                                                 │
│  API 客戶端:                                                     │
│  ├─ Spring Cloud OpenFeign 4.0.x                               │
│  ├─ RestTemplate (Deprecated, 改用 WebClient)                  │
│  └─ WebClient (Reactive HTTP Client)                           │
│                                                                 │
│  其他:                                                          │
│  ├─ Lombok 1.18.30 (簡化 POJO)                                │
│  ├─ MapStruct 1.5.5 (DTO Mapping)                             │
│  ├─ Apache Commons Lang3 3.13.0                                │
│  └─ Guava 32.1.3-jre                                           │
└─────────────────────────────────────────────────────────────────┘
```

### 版本相容性矩陣

| 元件 | 版本 | Spring Boot 3 相容性 | 備註 |
|------|------|---------------------|------|
| Java | 17 LTS | ✅ | 最低需求 Java 17 |
| Spring Framework | 6.0.x | ✅ | Spring Boot 3 內建 |
| MyBatis | 3.5.13 | ✅ | |
| MyBatis Spring Boot Starter | 3.0.2 | ✅ | Spring Boot 3 專用 |
| Oracle JDBC | 19.x | ✅ | 支援 Oracle 11g+ |
| Redis | 7.x | ✅ | 向下相容 6.x |
| Spring Security | 6.x | ✅ | Spring Boot 3 內建 |

---

## 核心框架 - Spring Boot 3

### Spring Boot 3 重大變更

#### 1. Jakarta EE 9+ 遷移

```java
// ❌ Spring Boot 2.x (javax)
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.persistence.Entity;

// ✅ Spring Boot 3.x (jakarta)
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Entity;
```

**遷移影響**:
- 所有 `javax.*` 套件改為 `jakarta.*`
- 需更新所有相依套件至支援 Jakarta EE 9+ 的版本
- MyBatis 3.5.13 已支援 Jakarta EE

---

#### 2. Java 17 特性運用

**Record Classes (不可變資料類別)**:
```java
// ✅ 使用 Record 替代 DTO
public record PricingRequest(
    String memberCardId,
    List<SkuInfo> skus,
    String channelId
) {}

// 等同於:
public class PricingRequest {
    private final String memberCardId;
    private final List<SkuInfo> skus;
    private final String channelId;

    // Constructor, Getters, equals, hashCode, toString 自動生成
}
```

**Pattern Matching for switch**:
```java
// ✅ Java 17 Pattern Matching
public String getStatusName(String statusId) {
    return switch (statusId) {
        case "1" -> "草稿";
        case "2" -> "報價";
        case "3" -> "已付款";
        case "4" -> "有效";
        case "5" -> "已結案";
        case "6" -> "作廢";
        default -> "未知";
    };
}
```

**Sealed Classes (密封類別)**:
```java
// ✅ 限制繼承階層
public sealed interface OrderEvent
    permits OrderCreatedEvent, OrderUpdatedEvent, OrderCancelledEvent {}

public final class OrderCreatedEvent implements OrderEvent {
    private final String orderId;
    private final LocalDateTime createTime;
}

public final class OrderUpdatedEvent implements OrderEvent {
    private final String orderId;
    private final LocalDateTime updateTime;
}

public final class OrderCancelledEvent implements OrderEvent {
    private final String orderId;
    private final String reason;
}
```

---

#### 3. Native Image 支援 (GraalVM)

Spring Boot 3 支援編譯為 Native Image:

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

```bash
# 編譯為 Native Image
mvn -Pnative native:compile

# 執行 Native Image
./target/pricing-service
```

**效益**:
- 啟動時間: 3 秒 → 0.1 秒 (-97%)
- 記憶體使用: 512MB → 50MB (-90%)
- 部署體積: 150MB → 50MB (-67%)

**限制**:
- 不支援反射 (需配置 reflect-config.json)
- 不支援動態 Proxy
- MyBatis XML Mapper 需特殊配置

---

### 專案結構

```
pricing-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/trihome/som/pricing/
│   │   │       ├── PricingServiceApplication.java
│   │   │       │
│   │   │       ├── config/               # 配置類別
│   │   │       │   ├── RedisConfig.java
│   │   │       │   ├── MyBatisConfig.java
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   └── WebMvcConfig.java
│   │   │       │
│   │   │       ├── controller/           # REST API Controllers
│   │   │       │   ├── PricingController.java
│   │   │       │   └── HealthController.java
│   │   │       │
│   │   │       ├── service/              # 業務邏輯層
│   │   │       │   ├── PricingService.java
│   │   │       │   ├── impl/
│   │   │       │   │   └── PricingServiceImpl.java
│   │   │       │   ├── MemberDiscountService.java
│   │   │       │   └── PromotionService.java
│   │   │       │
│   │   │       ├── domain/               # 領域模型
│   │   │       │   ├── entity/           # 實體 (對應資料庫表)
│   │   │       │   │   ├── Order.java
│   │   │       │   │   ├── OrderCompute.java
│   │   │       │   │   └── SkuInfo.java
│   │   │       │   └── vo/               # Value Object
│   │   │       │       ├── PricingResult.java
│   │   │       │       └── MemberDiscount.java
│   │   │       │
│   │   │       ├── dto/                  # Data Transfer Object
│   │   │       │   ├── request/
│   │   │       │   │   ├── PricingRequest.java
│   │   │       │   │   └── SkuPricingRequest.java
│   │   │       │   └── response/
│   │   │       │       ├── PricingResponse.java
│   │   │       │       └── ApiResponse.java
│   │   │       │
│   │   │       ├── mapper/               # MyBatis Mapper
│   │   │       │   ├── OrderMapper.java
│   │   │       │   ├── OrderComputeMapper.java
│   │   │       │   └── SkuInfoMapper.java
│   │   │       │
│   │   │       ├── repository/           # 資料存取層封裝
│   │   │       │   ├── OrderRepository.java
│   │   │       │   └── impl/
│   │   │       │       └── OrderRepositoryImpl.java
│   │   │       │
│   │   │       ├── client/               # 外部 API 客戶端
│   │   │       │   ├── CrmClient.java
│   │   │       │   ├── MemberServiceClient.java
│   │   │       │   └── adapter/          # 防腐層
│   │   │       │       └── CrmAdapter.java
│   │   │       │
│   │   │       ├── exception/            # 自訂異常
│   │   │       │   ├── PricingException.java
│   │   │       │   ├── MemberNotFoundException.java
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       │
│   │   │       ├── security/             # 安全相關
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   └── UserDetailsServiceImpl.java
│   │   │       │
│   │   │       ├── util/                 # 工具類別
│   │   │       │   ├── DateUtils.java
│   │   │       │   ├── NumberUtils.java
│   │   │       │   └── CacheKeyBuilder.java
│   │   │       │
│   │   │       └── constant/             # 常數定義
│   │   │           ├── OrderConstant.java
│   │   │           ├── PricingConstant.java
│   │   │           └── CacheConstant.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml           # 主配置檔
│   │       ├── application-dev.yml       # 開發環境
│   │       ├── application-sit.yml       # SIT 環境
│   │       ├── application-stg.yml       # Staging 環境
│   │       ├── application-prod.yml      # 生產環境
│   │       │
│   │       ├── mapper/                   # MyBatis XML Mapper
│   │       │   ├── OrderMapper.xml
│   │       │   ├── OrderComputeMapper.xml
│   │       │   └── SkuInfoMapper.xml
│   │       │
│   │       ├── db/migration/             # Flyway 資料庫遷移
│   │       │   ├── V1__Initial_schema.sql
│   │       │   └── V2__Add_pricing_cache_table.sql
│   │       │
│   │       ├── logback-spring.xml        # 日誌配置
│   │       └── banner.txt                # 啟動 Banner
│   │
│   └── test/
│       ├── java/
│       │   └── com/trihome/som/pricing/
│       │       ├── service/
│       │       │   └── PricingServiceTest.java
│       │       ├── controller/
│       │       │   └── PricingControllerTest.java
│       │       └── integration/
│       │           └── PricingIntegrationTest.java
│       └── resources/
│           ├── application-test.yml
│           └── test-data.sql
│
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## 資料存取 - MyBatis

### MyBatis 3.5.13 + Spring Boot 3

#### 配置

```yaml
# application.yml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.trihome.som.pricing.domain.entity
  configuration:
    map-underscore-to-camel-case: true  # 自動轉換駝峰命名
    default-fetch-size: 100
    default-statement-timeout: 30
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
    cache-enabled: true  # 啟用二級快取
    lazy-loading-enabled: true  # 延遲載入
    aggressive-lazy-loading: false
```

```java
// MyBatisConfig.java
@Configuration
@MapperScan("com.trihome.som.pricing.mapper")
public class MyBatisConfig {

    @Bean
    public Interceptor paginationInterceptor() {
        // 自訂分頁攔截器
        return new PaginationInterceptor();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 配置攔截器
        factoryBean.setPlugins(paginationInterceptor());

        // 配置 Type Handlers
        factoryBean.setTypeHandlers(new TypeHandler<?>[] {
            new LocalDateTimeTypeHandler(),
            new BigDecimalTypeHandler()
        });

        return factoryBean.getObject();
    }
}
```

#### Mapper 介面

```java
// OrderMapper.java
@Mapper
public interface OrderMapper {

    // 註解式 SQL (簡單查詢)
    @Select("SELECT * FROM TBL_ORDER WHERE ORDER_ID = #{orderId}")
    Order selectByOrderId(@Param("orderId") String orderId);

    // XML Mapper (複雜查詢)
    List<Order> selectByDynamicCriteria(@Param("criteria") OrderCriteria criteria);

    @Insert("""
        INSERT INTO TBL_ORDER (
            ORDER_ID, MEMBER_CARD_ID, TOTAL_AMT,
            ORDER_STATUS_ID, CREATE_TIME
        ) VALUES (
            #{orderId}, #{memberCardId}, #{totalAmt},
            #{orderStatusId}, #{createTime}
        )
        """)
    int insert(Order order);

    @Update("""
        UPDATE TBL_ORDER
        SET ORDER_STATUS_ID = #{statusId},
            UPDATE_TIME = #{updateTime}
        WHERE ORDER_ID = #{orderId}
        """)
    int updateStatus(@Param("orderId") String orderId,
                     @Param("statusId") String statusId,
                     @Param("updateTime") LocalDateTime updateTime);

    // 批次插入
    int batchInsert(@Param("orders") List<Order> orders);

    // 自訂 ResultMap
    @Results(id = "orderResultMap", value = {
        @Result(property = "orderId", column = "ORDER_ID"),
        @Result(property = "memberCardId", column = "MEMBER_CARD_ID"),
        @Result(property = "totalAmt", column = "TOTAL_AMT"),
        @Result(property = "orderStatusId", column = "ORDER_STATUS_ID"),
        @Result(property = "createTime", column = "CREATE_TIME")
    })
    @Select("SELECT * FROM TBL_ORDER WHERE MEMBER_CARD_ID = #{memberCardId}")
    List<Order> selectByMemberCardId(@Param("memberCardId") String memberCardId);
}
```

#### XML Mapper

```xml
<!-- OrderMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.trihome.som.pricing.mapper.OrderMapper">

    <!-- ResultMap -->
    <resultMap id="BaseResultMap" type="com.trihome.som.pricing.domain.entity.Order">
        <id column="ORDER_ID" property="orderId" jdbcType="VARCHAR"/>
        <result column="MEMBER_CARD_ID" property="memberCardId" jdbcType="VARCHAR"/>
        <result column="TOTAL_AMT" property="totalAmt" jdbcType="DECIMAL"/>
        <result column="TOTAL_DISC" property="totalDisc" jdbcType="DECIMAL"/>
        <result column="ORDER_STATUS_ID" property="orderStatusId" jdbcType="VARCHAR"/>
        <result column="CREATE_TIME" property="createTime" jdbcType="TIMESTAMP"/>
        <result column="UPDATE_TIME" property="updateTime" jdbcType="TIMESTAMP"/>
    </resultMap>

    <!-- 動態 SQL 查詢 -->
    <select id="selectByDynamicCriteria" resultMap="BaseResultMap">
        SELECT * FROM TBL_ORDER
        <where>
            <if test="criteria.orderIds != null and criteria.orderIds.size() > 0">
                AND ORDER_ID IN
                <foreach collection="criteria.orderIds" item="orderId" open="(" separator="," close=")">
                    #{orderId}
                </foreach>
            </if>
            <if test="criteria.memberCardId != null">
                AND MEMBER_CARD_ID = #{criteria.memberCardId}
            </if>
            <if test="criteria.statusIds != null and criteria.statusIds.size() > 0">
                AND ORDER_STATUS_ID IN
                <foreach collection="criteria.statusIds" item="statusId" open="(" separator="," close=")">
                    #{statusId}
                </foreach>
            </if>
            <if test="criteria.startDate != null">
                AND CREATE_TIME &gt;= #{criteria.startDate}
            </if>
            <if test="criteria.endDate != null">
                AND CREATE_TIME &lt;= #{criteria.endDate}
            </if>
        </where>
        ORDER BY CREATE_TIME DESC
    </select>

    <!-- 批次插入 -->
    <insert id="batchInsert" parameterType="java.util.List">
        INSERT INTO TBL_ORDER (
            ORDER_ID, MEMBER_CARD_ID, TOTAL_AMT, TOTAL_DISC,
            ORDER_STATUS_ID, CREATE_TIME
        ) VALUES
        <foreach collection="orders" item="order" separator=",">
            (
                #{order.orderId}, #{order.memberCardId}, #{order.totalAmt}, #{order.totalDisc},
                #{order.orderStatusId}, #{order.createTime}
            )
        </foreach>
    </insert>

    <!-- 複雜查詢：關聯查詢 -->
    <select id="selectOrderWithComputes" resultMap="OrderWithComputesResultMap">
        SELECT
            o.ORDER_ID, o.MEMBER_CARD_ID, o.TOTAL_AMT,
            oc.COMPUTE_TYPE, oc.COMPUTE_AMT
        FROM TBL_ORDER o
        LEFT JOIN TBL_ORDER_COMPUTE oc ON o.ORDER_ID = oc.ORDER_ID
        WHERE o.ORDER_ID = #{orderId}
    </select>

    <resultMap id="OrderWithComputesResultMap" type="com.trihome.som.pricing.domain.entity.Order">
        <id column="ORDER_ID" property="orderId"/>
        <result column="MEMBER_CARD_ID" property="memberCardId"/>
        <result column="TOTAL_AMT" property="totalAmt"/>
        <!-- 一對多關聯 -->
        <collection property="orderComputes" ofType="com.trihome.som.pricing.domain.entity.OrderCompute">
            <result column="COMPUTE_TYPE" property="computeType"/>
            <result column="COMPUTE_AMT" property="computeAmt"/>
        </collection>
    </resultMap>

    <!-- 使用 CDATA 避免 XML 解析問題 -->
    <select id="selectOrdersByAmountRange" resultMap="BaseResultMap">
        <![CDATA[
        SELECT * FROM TBL_ORDER
        WHERE TOTAL_AMT >= #{minAmt}
          AND TOTAL_AMT <= #{maxAmt}
        ORDER BY TOTAL_AMT DESC
        ]]>
    </select>

</mapper>
```

---

## 快取層 - Redis

### Spring Data Redis 配置

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms
        cluster:
          refresh:
            adaptive: true  # 自動更新 Cluster 拓撲
            period: 60s

  cache:
    type: redis
    redis:
      time-to-live: 300000  # 預設 TTL 5 分鐘
      cache-null-values: false  # 不快取 null 值
```

```java
// RedisConfig.java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer (String)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value Serializer (JSON)
        GenericJackson2JsonRedisSerializer jsonSerializer =
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        // 不同快取名稱不同 TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
            "pricing", config.entryTtl(Duration.ofMinutes(5)),
            "member-discount", config.entryTtl(Duration.ofMinutes(30)),
            "promotion", config.entryTtl(Duration.ofMinutes(10))
        );

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware()
            .build();
    }

    // 分散式鎖
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setPassword(redisPassword)
            .setDatabase(0)
            .setConnectionPoolSize(20)
            .setConnectionMinimumIdleSize(5);

        return Redisson.create(config);
    }
}
```

### 快取使用

```java
// PricingService.java
@Service
@CacheConfig(cacheNames = "pricing")
public class PricingServiceImpl implements PricingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PricingCalculator pricingCalculator;

    // 方法級別快取
    @Cacheable(key = "'pricing:' + #request.memberCardId + ':' + #request.skuHash")
    public PricingResult calculate(PricingRequest request) {
        log.info("快取未命中，執行計算: memberCardId={}", request.getMemberCardId());
        return pricingCalculator.calculate(request);
    }

    // 快取失效
    @CacheEvict(key = "'pricing:' + #memberCardId + ':*'", allEntries = true)
    public void evictPricingCache(String memberCardId) {
        log.info("清除計價快取: memberCardId={}", memberCardId);
    }

    // 手動快取操作
    public PricingResult calculateWithManualCache(PricingRequest request) {
        String cacheKey = buildCacheKey(request);

        // 1. 嘗試從快取取得
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("快取命中: key={}", cacheKey);
            return (PricingResult) cached;
        }

        // 2. 快取未命中，執行計算
        PricingResult result = pricingCalculator.calculate(request);

        // 3. 寫入快取 (TTL 5 分鐘)
        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));

        return result;
    }

    // 分散式鎖
    public PricingResult calculateWithLock(PricingRequest request) {
        String lockKey = "lock:pricing:" + request.getMemberCardId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 嘗試取得鎖 (等待 10 秒，鎖定 30 秒)
            boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (acquired) {
                return calculate(request);
            } else {
                throw new PricingException("無法取得計價鎖");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PricingException("計價被中斷", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 安全框架 - Spring Security

### JWT 認證配置

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 啟用方法級別安全
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // API 不需要 CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Stateless (使用 JWT)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()  // 登入不需認證
                .requestMatchers("/api/v1/health/**").permitAll()  // 健康檢查不需認證
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()  // Swagger 不需認證
                .requestMatchers("/api/v1/orders/**").hasAnyRole("USER", "ADMIN")  // 需 USER 或 ADMIN 角色
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")  // 需 ADMIN 角色
                .anyRequest().authenticated())  // 其他都需認證
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "https://som.example.com"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Trace-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### JWT Token Provider

```java
// JwtTokenProvider.java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;  // 毫秒

    // 生成 Token
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .claim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
    }

    // 驗證 Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(jwtSecret)
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.error("無效的 JWT 簽章");
        } catch (MalformedJwtException ex) {
            log.error("無效的 JWT Token");
        } catch (ExpiredJwtException ex) {
            log.error("JWT Token 已過期");
        } catch (UnsupportedJwtException ex) {
            log.error("不支援的 JWT Token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT Token 為空");
        }
        return false;
    }

    // 從 Token 取得使用者名稱
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .setSigningKey(jwtSecret)
            .parseClaimsJws(token)
            .getBody();

        return claims.getSubject();
    }
}
```

### JWT 認證 Filter

```java
// JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. 從 Header 取得 JWT Token
            String jwt = getJwtFromRequest(request);

            // 2. 驗證 Token
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // 3. 從 Token 取得使用者名稱
                String username = tokenProvider.getUsernameFromToken(jwt);

                // 4. 載入使用者詳細資料
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. 建立 Authentication 物件
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. 設定到 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("無法設定使用者認證", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

### 方法級別安全

```java
// PricingController.java
@RestController
@RequestMapping("/api/v1/pricing")
public class PricingController {

    // 需要 ADMIN 角色
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/clear-cache")
    public ResponseEntity<Void> clearCache() {
        pricingService.clearAllCache();
        return ResponseEntity.ok().build();
    }

    // 需要 USER 或 ADMIN 角色
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/calculate")
    public ResponseEntity<PricingResponse> calculate(@RequestBody PricingRequest request) {
        PricingResult result = pricingService.calculate(request);
        return ResponseEntity.ok(PricingResponse.from(result));
    }

    // 只有訂單擁有者或 ADMIN 可以存取
    @PreAuthorize("#memberCardId == authentication.principal.username or hasRole('ADMIN')")
    @GetMapping("/history/{memberCardId}")
    public ResponseEntity<List<PricingHistory>> getHistory(@PathVariable String memberCardId) {
        List<PricingHistory> history = pricingService.getHistory(memberCardId);
        return ResponseEntity.ok(history);
    }
}
```

---

## API 文檔 - SpringDoc

### 配置

```yaml
# application.yml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha
  show-actuator: true
```

```java
// OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI somOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SOM Pricing Service API")
                .description("Store Operation Management - Pricing Service REST API")
                .version("2.0.0")
                .contact(new Contact()
                    .name("SOM Team")
                    .email("som@example.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://example.com/license")))
            .servers(List.of(
                new Server().url("http://localhost:8082").description("Local"),
                new Server().url("https://api-sit.example.com").description("SIT"),
                new Server().url("https://api.example.com").description("Production")
            ))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
```

### API 註解

```java
// PricingController.java
@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Pricing", description = "計價相關 API")
public class PricingController {

    @Operation(
        summary = "計算訂單價格",
        description = "根據會員卡號、商品清單計算訂單總價",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "計算成功",
                content = @Content(schema = @Schema(implementation = PricingResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "請求參數錯誤",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "未授權"
            )
        }
    )
    @PostMapping("/calculate")
    public ResponseEntity<PricingResponse> calculate(
        @RequestBody @Valid PricingRequest request
    ) {
        PricingResult result = pricingService.calculate(request);
        return ResponseEntity.ok(PricingResponse.from(result));
    }
}

// DTO 註解
@Schema(description = "計價請求")
public class PricingRequest {

    @Schema(description = "會員卡號", example = "A123456789", required = true)
    @NotBlank(message = "會員卡號不可為空")
    private String memberCardId;

    @Schema(description = "商品清單", required = true)
    @NotEmpty(message = "至少需要一個商品")
    @Valid
    private List<SkuInfo> skus;

    @Schema(description = "通路代碼", example = "01", required = true)
    @NotBlank(message = "通路代碼不可為空")
    private String channelId;
}
```

**訪問 Swagger UI**: `http://localhost:8082/swagger-ui.html`

---

## 監控與日誌

### Spring Boot Actuator

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true  # Kubernetes Probes
  metrics:
    export:
      prometheus:
        enabled: true
  info:
    env:
      enabled: true
    git:
      mode: full
```

**可用端點**:
- `GET /actuator/health` - 健康檢查
- `GET /actuator/health/liveness` - K8s Liveness Probe
- `GET /actuator/health/readiness` - K8s Readiness Probe
- `GET /actuator/metrics` - 指標列表
- `GET /actuator/metrics/jvm.memory.used` - JVM 記憶體使用
- `GET /actuator/prometheus` - Prometheus 格式指標

### 日誌配置

```xml
<!-- logback-spring.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <!-- 變數定義 -->
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_FILE" value="pricing-service"/>

    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- File Appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE}.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- Error Appender -->
    <appender name="ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <file>${LOG_PATH}/${LOG_FILE}-error.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE}-error-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>60</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 不同環境不同日誌級別 -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR"/>
        </root>
    </springProfile>

    <!-- 第三方套件日誌級別 -->
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.apache.ibatis" level="INFO"/>
    <logger name="com.zaxxer.hikari" level="INFO"/>

    <!-- 應用程式日誌級別 -->
    <logger name="com.trihome.som.pricing" level="DEBUG"/>

</configuration>
```

---

## 建構工具

### Maven POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.5</version>
    </parent>

    <groupId>com.trihome.som</groupId>
    <artifactId>pricing-service</artifactId>
    <version>2.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <mybatis.version>3.0.2</mybatis.version>
        <resilience4j.version>2.1.0</resilience4j.version>
        <springdoc.version>2.2.0</springdoc.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- MyBatis -->
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>${mybatis.version}</version>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>com.oracle.database.jdbc</groupId>
            <artifactId>ojdbc8</artifactId>
        </dependency>

        <!-- Redis Client -->
        <dependency>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>

        <!-- SpringDoc OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.10</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

</project>
```

---

## 效益總結

| 面向 | 現況 (Spring MVC) | 目標 (Spring Boot 3) | 改善 |
|------|------------------|---------------------|------|
| **Java 版本** | Java 8 | Java 17 LTS | ✅ 最新特性 |
| **啟動時間** | ~30 秒 | ~3 秒 | -90% |
| **配置方式** | XML 配置 | 註解 + YAML | +80% 可讀性 |
| **API 文檔** | 無 | Swagger UI | ✅ 自動生成 |
| **監控** | 無 | Actuator + Metrics | ✅ 完整監控 |
| **安全性** | 基礎 | JWT + Spring Security 6 | +200% |
| **快取** | 無 | Redis 多層快取 | +95% 命中率 |
| **容錯** | 無 | Resilience4j | ✅ 高可用 |

---

## 相關文檔

- [08-Architecture-Overview.md](./08-Architecture-Overview.md) - 架構總覽
- [33-Backend-Project-Setup.md](./33-Backend-Project-Setup.md) - 後端專案建置
- [34-Backend-Security-JWT.md](./34-Backend-Security-JWT.md) - JWT 安全實作
- [35-Backend-Order-Service.md](./35-Backend-Order-Service.md) - 訂單服務實作
- [36-Backend-Pricing-Service.md](./36-Backend-Pricing-Service.md) - 計價服務實作
