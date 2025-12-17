# 33. Backend - Project Setup

## Spring Boot 3 專案初始化

```bash
# 使用 Spring Initializr
curl https://start.spring.io/starter.zip \
  -d dependencies=web,data-jpa,security,actuator,validation \
  -d bootVersion=3.1.5 \
  -d javaVersion=17 \
  -d type=maven-project \
  -d groupId=com.trihome.som \
  -d artifactId=order-service \
  -o order-service.zip

unzip order-service.zip
cd order-service
```

## pom.xml 設定

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.1.5</spring-boot.version>
    <mybatis-spring-boot.version>3.0.2</mybatis-spring-boot.version>
</properties>

<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>${mybatis-spring-boot.version}</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Oracle JDBC -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator (監控) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

## application.yml

```yaml
spring:
  application:
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  datasource:
    url: jdbc:oracle:thin:@localhost:1521:orcl
    username: som_user
    password: ${DB_PASSWORD}
    driver-class-name: oracle.jdbc.OracleDriver

  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD}

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.trihome.som.order.model
  configuration:
    map-underscore-to-camel-case: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.trihome.som: DEBUG
    org.springframework.web: INFO
```

## 專案結構

```plaintext
order-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/trihome/som/order/
│   │   │       ├── OrderServiceApplication.java
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── MyBatisConfig.java
│   │   │       ├── controller/
│   │   │       │   └── OrderController.java
│   │   │       ├── service/
│   │   │       │   ├── OrderService.java
│   │   │       │   └── impl/
│   │   │       ├── repository/
│   │   │       │   └── OrderRepository.java
│   │   │       ├── mapper/
│   │   │       │   └── OrderMapper.java
│   │   │       ├── model/
│   │   │       │   ├── entity/
│   │   │       │   ├── dto/
│   │   │       │   └── vo/
│   │   │       ├── exception/
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       └── util/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-sit.yml
│   │       ├── application-prod.yml
│   │       ├── db/migration/
│   │       │   └── V1.0.0__create_orders_table.sql
│   │       └── mapper/
│   │           └── OrderMapper.xml
│   └── test/
├── pom.xml
└── Dockerfile
```

---

**參考文件**: `10-Backend-Tech-Stack.md`

**文件版本**: v1.0
