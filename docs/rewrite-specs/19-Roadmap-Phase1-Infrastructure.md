# 19. Roadmap Phase 1 - Infrastructure Setup

## 目錄

- [1. 階段概述](#1-階段概述)
- [2. 目標與交付成果](#2-目標與交付成果)
- [3. 技術任務](#3-技術任務)
- [4. 時程規劃](#4-時程規劃)
- [5. 驗收標準](#5-驗收標準)

---

## 1. 階段概述

### 1.1 階段定位

**Phase 1: Infrastructure Setup (基礎建設階段)**

```plaintext
目標: 建立微服務基礎設施, 為後續開發奠定基礎

時程: 4 週 (Sprint 1-2)

關鍵成果:
├── 開發環境建置完成
├── CI/CD Pipeline 建立
├── 微服務框架搭建
├── 資料庫 Schema 初始化
└── 監控系統部署

風險等級: 🟢 低
- 無業務邏輯, 純基礎建設
- 不影響現有系統運作
```

### 1.2 為什麼先做基礎建設

```plaintext
原因:
1. 建立統一開發標準
   → 避免各模組風格不一致

2. 及早發現環境問題
   → 避免後期開發受阻

3. 自動化部署流程
   → 加速後續迭代速度

4. 建立監控能力
   → 即時發現問題

5. 團隊技術磨合
   → 熟悉新技術棧
```

---

## 2. 目標與交付成果

### 2.1 主要目標

| 目標 | 說明 | 優先級 |
|-----|------|-------|
| G1 | 建立 Spring Boot 3 + Angular 8 專案骨架 | P0 |
| G2 | 部署 Redis 7.x 集群 | P0 |
| G3 | 建立 CI/CD Pipeline (Jenkins/GitLab CI) | P0 |
| G4 | 初始化資料庫 Schema (Flyway) | P0 |
| G5 | 建立 Kubernetes 開發環境 | P1 |
| G6 | 部署 Prometheus + Grafana 監控 | P1 |
| G7 | 建立日誌收集系統 (ELK Stack) | P2 |

### 2.2 交付成果

```plaintext
1. 程式碼專案
   ├── som-frontend/               # Angular 8 專案
   ├── som-order-service/          # 訂單服務 (Spring Boot 3)
   ├── som-pricing-service/        # 計價服務
   ├── som-payment-service/        # 付款服務
   ├── som-member-service/         # 會員服務
   └── som-inventory-service/      # 庫存服務

2. 基礎設施
   ├── Kubernetes manifests/       # K8s 部署檔案
   ├── Docker Compose/             # 本地開發環境
   ├── Redis Cluster/              # 3 Master + 3 Replica
   └── Database Migration/         # Flyway scripts

3. CI/CD Pipeline
   ├── Jenkinsfile                 # 建構流程
   ├── Dockerfile                  # 容器化
   └── Helm Charts/                # K8s 部署

4. 監控系統
   ├── Prometheus                  # 指標收集
   ├── Grafana Dashboards/         # 可視化
   └── Alertmanager                # 告警

5. 文件
   ├── Developer Guide.md          # 開發指南
   ├── Deployment Guide.md         # 部署指南
   └── Architecture Decision Records/ # ADR
```

---

## 3. 技術任務

### 3.1 Task 1: 建立專案骨架 (1 週)

#### 3.1.1 Frontend - Angular 8 專案

```bash
# 建立 Angular 8 專案
ng new som-frontend --routing --style=scss
cd som-frontend

# 安裝依賴
npm install @ngrx/store@8.6.0
npm install @ngrx/effects@8.6.0
npm install @angular/material@8.2.3
npm install rxjs@6.5.5

# 建立專案結構
ng g module core
ng g module shared
ng g module features/order
ng g module features/pricing
```

**專案結構**:

```plaintext
som-frontend/
├── src/
│   ├── app/
│   │   ├── core/                # 核心模組 (單例)
│   │   │   ├── services/        # API 服務
│   │   │   ├── guards/          # 路由守衛
│   │   │   ├── interceptors/    # HTTP 攔截器
│   │   │   └── core.module.ts
│   │   ├── shared/              # 共用模組
│   │   │   ├── components/      # 共用元件
│   │   │   ├── directives/      # 指令
│   │   │   ├── pipes/           # 管道
│   │   │   └── shared.module.ts
│   │   ├── features/            # 功能模組
│   │   │   ├── order/           # 訂單模組
│   │   │   ├── pricing/         # 計價模組
│   │   │   └── payment/         # 付款模組
│   │   ├── store/               # NgRx State
│   │   │   ├── actions/
│   │   │   ├── reducers/
│   │   │   ├── effects/
│   │   │   └── selectors/
│   │   └── app.module.ts
│   ├── assets/
│   ├── environments/
│   │   ├── environment.ts       # 開發環境
│   │   ├── environment.sit.ts   # SIT 環境
│   │   └── environment.prod.ts  # 生產環境
│   └── styles.scss
├── angular.json
├── package.json
└── tsconfig.json
```

**Dockerfile**:

```dockerfile
# Stage 1: Build
FROM node:14-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Runtime
FROM nginx:1.21-alpine
COPY --from=builder /app/dist/som-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### 3.1.2 Backend - Spring Boot 3 專案

```bash
# 使用 Spring Initializr 建立專案
# https://start.spring.io/

Project: Maven
Language: Java
Spring Boot: 3.1.5
Java: 17
Packaging: Jar

Dependencies:
- Spring Web
- Spring Data JPA
- MyBatis Framework
- Spring Security
- Spring Cache (Redis)
- Spring Boot Actuator
- Validation
- Lombok
```

**專案結構 (以 order-service 為例)**:

```plaintext
som-order-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/trihome/som/order/
│   │   │       ├── OrderServiceApplication.java
│   │   │       ├── config/          # 設定
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── MyBatisConfig.java
│   │   │       ├── controller/      # REST API
│   │   │       │   └── OrderController.java
│   │   │       ├── service/         # 業務邏輯
│   │   │       │   ├── OrderService.java
│   │   │       │   └── impl/
│   │   │       ├── repository/      # 資料存取
│   │   │       │   └── OrderRepository.java
│   │   │       ├── mapper/          # MyBatis Mapper
│   │   │       │   └── OrderMapper.java
│   │   │       ├── model/           # 資料模型
│   │   │       │   ├── entity/      # 實體
│   │   │       │   ├── dto/         # DTO
│   │   │       │   └── vo/          # VO
│   │   │       ├── exception/       # 例外處理
│   │   │       │   └── GlobalExceptionHandler.java
│   │   │       └── util/            # 工具類
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-sit.yml
│   │       ├── application-prod.yml
│   │       ├── db/
│   │       │   └── migration/       # Flyway scripts
│   │       │       ├── V1.0.0__create_orders_table.sql
│   │       │       └── V1.0.1__create_order_items_table.sql
│   │       └── mapper/              # MyBatis XML
│   │           └── OrderMapper.xml
│   └── test/
│       └── java/
│           └── com/trihome/som/order/
│               ├── controller/
│               ├── service/
│               └── repository/
├── pom.xml
└── Dockerfile
```

**pom.xml**:

```xml
<properties>
    <java.version>17</java.version>
    <spring.boot.version>3.1.5</spring.boot.version>
    <mybatis.version>3.5.13</mybatis.version>
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

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.2</version>
    </dependency>

    <!-- Oracle JDBC -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>

    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Actuator (Health Check) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer (Prometheus) -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Dockerfile**:

```dockerfile
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 Task 2: 部署 Redis 集群 (3 天)

#### 3.2.1 Docker Compose (開發環境)

```yaml
# docker-compose.redis.yml
version: '3.8'

services:
  redis-master-1:
    image: redis:7-alpine
    container_name: redis-master-1
    command: redis-server --port 6379 --cluster-enabled yes --cluster-config-file nodes.conf
    ports:
      - "6379:6379"
    volumes:
      - redis-master-1-data:/data

  redis-master-2:
    image: redis:7-alpine
    container_name: redis-master-2
    command: redis-server --port 6380 --cluster-enabled yes --cluster-config-file nodes.conf
    ports:
      - "6380:6380"
    volumes:
      - redis-master-2-data:/data

  redis-master-3:
    image: redis:7-alpine
    container_name: redis-master-3
    command: redis-server --port 6381 --cluster-enabled yes --cluster-config-file nodes.conf
    ports:
      - "6381:6381"
    volumes:
      - redis-master-3-data:/data

  redis-cluster-init:
    image: redis:7-alpine
    depends_on:
      - redis-master-1
      - redis-master-2
      - redis-master-3
    command: >
      sh -c "sleep 5 &&
      redis-cli --cluster create
      redis-master-1:6379
      redis-master-2:6380
      redis-master-3:6381
      --cluster-replicas 0 --cluster-yes"

volumes:
  redis-master-1-data:
  redis-master-2-data:
  redis-master-3-data:
```

#### 3.2.2 Kubernetes (生產環境)

```yaml
# redis-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis-cluster
spec:
  serviceName: redis-cluster
  replicas: 6
  selector:
    matchLabels:
      app: redis-cluster
  template:
    metadata:
      labels:
        app: redis-cluster
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - containerPort: 6379
          name: client
        - containerPort: 16379
          name: gossip
        command:
        - redis-server
        - /conf/redis.conf
        volumeMounts:
        - name: conf
          mountPath: /conf
        - name: data
          mountPath: /data
      volumes:
      - name: conf
        configMap:
          name: redis-cluster-config
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
```

### 3.3 Task 3: 建立 CI/CD Pipeline (1 週)

#### 3.3.1 Jenkinsfile

```groovy
pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'registry.som.com'
        DOCKER_IMAGE_PREFIX = 'som'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'develop', url: 'https://github.com/trihome/som.git'
            }
        }

        stage('Build Backend') {
            parallel {
                stage('Order Service') {
                    steps {
                        dir('som-order-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                stage('Pricing Service') {
                    steps {
                        dir('som-pricing-service') {
                            sh 'mvn clean package -DskipTests'
                        }
                    }
                }
                // ... 其他服務
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('som-frontend') {
                    sh 'npm ci'
                    sh 'npm run build -- --configuration=production'
                }
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    def services = ['order', 'pricing', 'payment', 'member', 'inventory']
                    services.each { service ->
                        sh """
                            docker build -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_PREFIX}-${service}:${BUILD_NUMBER} \
                                som-${service}-service/
                        """
                    }

                    sh """
                        docker build -t ${DOCKER_REGISTRY}/${DOCKER_IMAGE_PREFIX}-frontend:${BUILD_NUMBER} \
                            som-frontend/
                    """
                }
            }
        }

        stage('Push to Registry') {
            steps {
                script {
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        def services = ['order', 'pricing', 'payment', 'member', 'inventory', 'frontend']
                        services.each { service ->
                            sh "docker push ${DOCKER_REGISTRY}/${DOCKER_IMAGE_PREFIX}-${service}:${BUILD_NUMBER}"
                        }
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            steps {
                script {
                    sh """
                        kubectl set image deployment/order-service \
                            order-service=${DOCKER_REGISTRY}/${DOCKER_IMAGE_PREFIX}-order:${BUILD_NUMBER} \
                            --namespace=dev
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded!'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
```

### 3.4 Task 4: 初始化資料庫 Schema (3 天)

**參考文件**: `16-Database-Design.md`

```sql
-- V1.0.0__create_orders_table.sql
CREATE TABLE orders (
    order_id VARCHAR2(20) PRIMARY KEY,
    member_card_id VARCHAR2(20),
    channel_id VARCHAR2(10) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status_id VARCHAR2(2) NOT NULL,
    -- ... 其他欄位
    created_by VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_orders_member_id ON orders(member_card_id);
CREATE INDEX idx_orders_status ON orders(status_id);
```

### 3.5 Task 5: 部署監控系統 (3 天)

#### 3.5.1 Prometheus + Grafana

```yaml
# prometheus-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - name: prometheus
        image: prom/prometheus:v2.45.0
        ports:
        - containerPort: 9090
        volumeMounts:
        - name: config
          mountPath: /etc/prometheus
        - name: data
          mountPath: /prometheus
      volumes:
      - name: config
        configMap:
          name: prometheus-config
      - name: data
        emptyDir: {}
```

**Prometheus 設定**:

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'som-order-service'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: order-service
        action: keep
      - source_labels: [__meta_kubernetes_pod_ip]
        target_label: __address__
        replacement: ${1}:8080

  - job_name: 'som-pricing-service'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: pricing-service
        action: keep
```

---

## 4. 時程規劃

### 4.1 Gantt Chart

```plaintext
Week 1              Week 2              Week 3              Week 4
│                   │                   │                   │
├─ Task 1: 專案骨架 ─┤
│  ├─ Frontend      │
│  └─ Backend       │
│                   │
│  ├─ Task 2: Redis Cluster ─┤
│                   │         │
│                   ├─ Task 3: CI/CD Pipeline ─────────┤
│                   │         │                        │
│                   │         ├─ Task 4: DB Schema ────┤
│                   │         │                        │
│                   │         │  ├─ Task 5: Monitoring ┤
│                   │         │  │                     │
│                   │         │  │  ├─ 整合測試        │
│                   │         │  │  │                  │
├───────────────────┼─────────┼──┼──┼──────────────────┤
Sprint 1            Sprint 2
```

### 4.2 詳細時程

| 週次 | 任務 | 負責人 | 工時 (人天) | 狀態 |
|-----|------|-------|------------|------|
| W1 | Task 1.1: Angular 8 專案骨架 | Frontend Team | 3 | 🟡 待開始 |
| W1 | Task 1.2: Spring Boot 3 專案骨架 | Backend Team | 3 | 🟡 待開始 |
| W1 | Task 2: Redis 集群部署 | DevOps | 3 | 🟡 待開始 |
| W2 | Task 3: CI/CD Pipeline | DevOps | 5 | 🟡 待開始 |
| W2-W3 | Task 4: 資料庫 Schema 初始化 | DBA + Backend | 3 | 🟡 待開始 |
| W3 | Task 5: Prometheus + Grafana | DevOps | 3 | 🟡 待開始 |
| W4 | 整合測試 | QA Team | 3 | 🟡 待開始 |
| W4 | 文件撰寫 | All | 2 | 🟡 待開始 |

**總工時**: 25 人天

---

## 5. 驗收標準

### 5.1 功能驗收

| 編號 | 驗收項目 | 驗收標準 | 驗收方式 |
|-----|---------|---------|---------|
| AC-1 | Angular 8 專案啟動 | `npm start` 成功啟動, 瀏覽器開啟 http://localhost:4200 | 手動測試 |
| AC-2 | Spring Boot 3 服務啟動 | 5 個微服務啟動成功, Health Check 回傳 200 | `curl http://localhost:8080/actuator/health` |
| AC-3 | Redis 集群運作 | 3 Master 節點正常, 資料寫入讀取成功 | `redis-cli cluster info` |
| AC-4 | CI/CD Pipeline | 程式碼提交後自動建構、測試、部署到 Dev 環境 | 觀察 Jenkins Pipeline 執行 |
| AC-5 | 資料庫 Schema | Flyway 成功執行, 所有資料表建立完成 | 查詢 `flyway_schema_history` |
| AC-6 | Prometheus 監控 | 可查詢到微服務 metrics | 開啟 Prometheus UI 查詢 |
| AC-7 | Grafana Dashboard | 可視化顯示 CPU、Memory、Request Rate | 開啟 Grafana 查看 |

### 5.2 效能驗收

| 編號 | 指標 | 目標值 | 實際值 | 狀態 |
|-----|------|-------|-------|------|
| P-1 | 應用啟動時間 | < 30 秒 | - | 🟡 待測試 |
| P-2 | Health Check 回應時間 | < 100ms | - | 🟡 待測試 |
| P-3 | Redis 讀寫延遲 | < 5ms (p95) | - | 🟡 待測試 |
| P-4 | Docker 建構時間 | < 5 分鐘 | - | 🟡 待測試 |
| P-5 | CI/CD Pipeline 總時長 | < 15 分鐘 | - | 🟡 待測試 |

### 5.3 文件驗收

| 編號 | 文件名稱 | 狀態 |
|-----|---------|------|
| D-1 | Developer Guide.md | 🟡 待撰寫 |
| D-2 | API Documentation (Swagger) | 🟡 待撰寫 |
| D-3 | Database Schema Design | 🟡 待撰寫 |
| D-4 | Deployment Guide.md | 🟡 待撰寫 |
| D-5 | Architecture Decision Records (ADR) | 🟡 待撰寫 |

---

## 總結

### Phase 1 核心成果

1. ✅ **專案骨架**: Angular 8 + Spring Boot 3 專案初始化完成
2. ✅ **基礎設施**: Redis 集群、資料庫 Schema 建立完成
3. ✅ **CI/CD**: 自動化建構、測試、部署流程建立
4. ✅ **監控**: Prometheus + Grafana 監控系統部署
5. ✅ **文件**: 開發與部署指南完成

### 下一階段預告

**Phase 2: Order Core (訂單核心功能)**
- 訂單 CRUD API 實作
- 訂單狀態管理
- 前端訂單頁面開發

---

**參考文件**:
- `08-Architecture-Overview.md`: 整體架構
- `09-Frontend-Tech-Stack-Angular8.md`: 前端技術棧
- `10-Backend-Tech-Stack.md`: 後端技術棧
- `16-Database-Design.md`: 資料庫設計

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
