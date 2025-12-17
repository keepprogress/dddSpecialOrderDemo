# 24. Roadmap - Team Resources & Organization

## 目錄

- [1. 團隊架構](#1-團隊架構)
- [2. 角色與職責](#2-角色與職責)
- [3. 人力配置](#3-人力配置)
- [4. 技能需求](#4-技能需求)
- [5. 培訓計畫](#5-培訓計畫)

---

## 1. 團隊架構

### 1.1 組織架構

```plaintext
                    Project Manager
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
   Tech Lead         Product Owner      QA Lead
        │                                   │
   ├────┴────┐                         ├────┴────┐
   │         │                         │         │
Backend  Frontend                  Manual    Automation
 Team     Team                       QA         QA
   │         │
DevOps    UI/UX
```

### 1.2 團隊規模

**總人力**: 15-20 人

| 角色 | 人數 | 配置 |
|-----|------|------|
| Project Manager | 1 | 專案管理 |
| Product Owner | 1 | 需求管理 |
| Tech Lead | 1 | 技術決策 |
| Backend Engineer | 4-5 | 後端開發 |
| Frontend Engineer | 3-4 | 前端開發 |
| DevOps Engineer | 2 | 基礎設施 |
| QA Engineer | 2-3 | 測試 |
| UI/UX Designer | 1 | 設計 |
| DBA | 1 (兼職) | 資料庫管理 |

---

## 2. 角色與職責

### 2.1 Project Manager (專案經理)

**職責**:
- 制定專案計畫與時程
- 協調團隊資源
- 風險管理
- 進度追蹤與報告
- 跨部門溝通

**所需技能**:
- PMP / Scrum Master 認證
- 5+ 年專案管理經驗
- 熟悉敏捷開發流程

### 2.2 Tech Lead (技術負責人)

**職責**:
- 技術架構設計
- 技術選型決策
- Code Review
- 技術難題解決
- 技術文件撰寫

**所需技能**:
- 10+ 年軟體開發經驗
- 熟悉微服務架構
- Spring Boot + Angular 實戰經驗
- 良好的溝通能力

### 2.3 Backend Engineer (後端工程師)

**職責**:
- API 開發
- 業務邏輯實作
- 資料庫設計
- 單元測試撰寫
- 效能優化

**所需技能** (必備):
- Java 8+ / Spring Boot 3
- MyBatis / JPA
- RESTful API 設計
- Redis / RabbitMQ
- Git / Maven

**所需技能** (加分):
- 微服務架構經驗
- Kubernetes / Docker
- Oracle 資料庫

**人力分配**:
```plaintext
Backend Team (4-5 人)
├── Team Member 1: Order Service (訂單服務)
├── Team Member 2: Pricing Service (計價服務)
├── Team Member 3: Payment Service (付款服務)
├── Team Member 4: Member & Inventory Service (會員 & 庫存)
└── Team Member 5: External Integration (外部整合)
```

### 2.4 Frontend Engineer (前端工程師)

**職責**:
- 前端頁面開發
- 狀態管理 (NgRx)
- 表單驗證
- 單元測試
- UI/UX 實作

**所需技能** (必備):
- Angular 8+ / TypeScript
- RxJS / NgRx
- HTML5 / CSS3 / SCSS
- Angular Material
- Responsive Design

**所需技能** (加分):
- Webpack / npm
- Cypress E2E 測試
- UI/UX 設計敏感度

**人力分配**:
```plaintext
Frontend Team (3-4 人)
├── Team Member 1: Order Module (訂單模組)
├── Team Member 2: Pricing & Payment Module (計價 & 付款)
├── Team Member 3: Member & Common Components (會員 & 共用元件)
└── Team Member 4: Testing & Optimization (測試 & 優化)
```

### 2.5 DevOps Engineer

**職責**:
- CI/CD Pipeline 建置
- Kubernetes 集群管理
- 監控系統維護
- 生產環境部署
- 故障排除

**所需技能**:
- Kubernetes / Docker
- Jenkins / GitLab CI
- Prometheus / Grafana
- Linux 系統管理
- Shell Scripting

### 2.6 QA Engineer

**職責**:
- 測試計畫制定
- 測試案例設計
- 手動測試執行
- 自動化測試開發
- Bug 追蹤管理

**所需技能**:
- 測試理論與實踐
- JUnit / Mockito (自動化)
- Cypress / Selenium (E2E)
- JMeter / Gatling (效能)
- SQL / API 測試

---

## 3. 人力配置

### 3.1 各階段人力需求

```plaintext
Phase 1: Infrastructure (4 週)
├── DevOps: 2 人 (全時)
├── Backend: 2 人 (全時)
├── Frontend: 1 人 (全時)
└── QA: 1 人 (半時)
總人力: 5.5 人

Phase 2: Order Core (4 週)
├── Backend: 3 人 (全時)
├── Frontend: 2 人 (全時)
├── QA: 2 人 (全時)
└── DevOps: 1 人 (半時)
總人力: 7.5 人

Phase 3: Pricing Refactor (6 週)
├── Backend: 4 人 (全時)
├── Frontend: 2 人 (全時)
├── QA: 2 人 (全時)
└── DevOps: 1 人 (半時)
總人力: 8.5 人

Phase 4: Payment & Fulfillment (6 週)
├── Backend: 5 人 (全時)
├── Frontend: 2 人 (全時)
├── QA: 3 人 (全時)
└── DevOps: 1 人 (全時)
總人力: 11 人

Phase 5: Testing & Launch (4 週)
├── QA: 3 人 (全時)
├── DevOps: 2 人 (全時)
├── Backend: 2 人 (半時)
└── Frontend: 1 人 (半時)
總人力: 6.5 人
```

### 3.2 成本估算

**人月成本假設**:
- Senior Engineer: NT$ 150,000 / 月
- Mid-level Engineer: NT$ 100,000 / 月
- Junior Engineer: NT$ 70,000 / 月

**總成本估算**:

| 階段 | 人月 | 成本 (萬) |
|-----|------|----------|
| Phase 1 (4 週) | 5.5 | 55 |
| Phase 2 (4 週) | 7.5 | 75 |
| Phase 3 (6 週) | 12.75 | 128 |
| Phase 4 (6 週) | 16.5 | 165 |
| Phase 5 (4 週) | 6.5 | 65 |
| **總計 (24 週)** | **48.75** | **488 萬** |

**其他成本**:
- 雲端基礎設施: 50 萬 / 年
- 軟體授權: 30 萬 / 年
- 培訓費用: 20 萬
- **總預算**: **~600 萬 / 年**

---

## 4. 技能需求

### 4.1 技能矩陣

| 技能 | 必備等級 | 當前團隊 | 缺口 | 培訓 |
|-----|---------|---------|------|------|
| Java 17 | ★★★★☆ | ★★★☆☆ | -1 | ✅ |
| Spring Boot 3 | ★★★★☆ | ★★★☆☆ | -1 | ✅ |
| Angular 8 | ★★★★☆ | ★★☆☆☆ | -2 | ✅ |
| TypeScript | ★★★★☆ | ★★★☆☆ | -1 | ✅ |
| NgRx | ★★★☆☆ | ★★☆☆☆ | -1 | ✅ |
| MyBatis | ★★★★☆ | ★★★★☆ | 0 | - |
| Redis | ★★★★☆ | ★★☆☆☆ | -2 | ✅ |
| Kubernetes | ★★★★☆ | ★★☆☆☆ | -2 | ✅ |
| Microservices | ★★★★☆ | ★★★☆☆ | -1 | ✅ |

**解決方案**:
1. **內部培訓**: Java 17, Spring Boot 3, Angular 8
2. **外部顧問**: Kubernetes, Microservices 架構
3. **招募新人**: 具備 Angular + NgRx 經驗

### 4.2 關鍵技能人員

**必須招募**:
- [ ] 1 位 Angular 8 + NgRx 專家 (Senior)
- [ ] 1 位 Kubernetes 專家 (Mid-level)

**可內部培養**:
- [x] Java 17 新特性 (內部培訓)
- [x] Spring Boot 3 升級 (內部培訓)
- [x] Redis 快取策略 (內部培訓)

---

## 5. 培訓計畫

### 5.1 培訓課程

#### 5.1.1 Backend 培訓 (2 週)

**課程大綱**:
```plaintext
Week 1: Java 17 + Spring Boot 3
├── Day 1-2: Java 17 新特性
│   ├── Record Classes
│   ├── Pattern Matching
│   ├── Sealed Classes
│   └── Text Blocks
│
├── Day 3-4: Spring Boot 3 升級
│   ├── Jakarta EE 9+ (javax → jakarta)
│   ├── Spring Framework 6
│   ├── Native Image Support
│   └── Observability (Micrometer)
│
└── Day 5: 實戰練習
    └── 建立 Spring Boot 3 專案

Week 2: Microservices + Redis
├── Day 1-2: 微服務架構
│   ├── Service Decomposition
│   ├── API Gateway
│   ├── Service Discovery
│   └── Circuit Breaker
│
├── Day 3-4: Redis 快取策略
│   ├── Cache-Aside Pattern
│   ├── TTL 策略
│   ├── 快取穿透/雪崩
│   └── Redis Cluster
│
└── Day 5: 實戰練習
    └── 實作計價服務快取
```

#### 5.1.2 Frontend 培訓 (2 週)

**課程大綱**:
```plaintext
Week 1: Angular 8 Fundamentals
├── Day 1-2: Angular 8 基礎
│   ├── Components & Templates
│   ├── Services & DI
│   ├── Routing
│   └── Forms (Reactive Forms)
│
├── Day 3-4: RxJS
│   ├── Observables
│   ├── Operators (map, filter, switchMap)
│   ├── Subjects
│   └── Error Handling
│
└── Day 5: 實戰練習
    └── 建立訂單查詢頁面

Week 2: NgRx State Management
├── Day 1-2: NgRx 核心概念
│   ├── Store
│   ├── Actions
│   ├── Reducers
│   └── Selectors
│
├── Day 3-4: NgRx Effects
│   ├── Side Effects
│   ├── API Integration
│   ├── Error Handling
│   └── Testing
│
└── Day 5: 實戰練習
    └── 實作訂單狀態管理
```

#### 5.1.3 DevOps 培訓 (1 週)

**課程大綱**:
```plaintext
Day 1-2: Docker & Kubernetes 基礎
├── Docker 容器化
├── Kubernetes 架構
├── Deployment & Service
└── ConfigMap & Secret

Day 3-4: CI/CD Pipeline
├── Jenkins Pipeline
├── GitLab CI/CD
├── 自動化測試整合
└── 藍綠部署

Day 5: 監控與日誌
├── Prometheus
├── Grafana
├── ELK Stack
└── Alerting
```

### 5.2 培訓時程

```plaintext
Month 1 (開工前)
├── Week 1-2: Backend 培訓
├── Week 3-4: Frontend 培訓
└── Week 4: DevOps 培訓 (並行)

培訓成本:
- 內部講師: 30 人天 × NT$ 5,000 = NT$ 150,000
- 外部顧問: 5 天 × NT$ 30,000 = NT$ 150,000
- 場地 & 設備: NT$ 50,000
總計: NT$ 350,000
```

---

## 總結

### 團隊資源核心要點

1. **團隊規模**: 15-20 人
2. **總預算**: ~600 萬 / 年
3. **關鍵招募**: Angular + Kubernetes 專家
4. **培訓投資**: 35 萬 (4 週培訓)
5. **人力高峰**: Phase 4 (11 人)

### Onboarding Checklist

**新成員加入檢查清單**:

- [ ] 開發環境設置 (IDE, Git, Docker)
- [ ] 權限申請 (GitLab, Kubernetes, 資料庫)
- [ ] 閱讀技術文件 (37 份規格文件)
- [ ] 參加培訓課程 (2 週)
- [ ] Pair Programming (1 週)
- [ ] 完成 Hello World 任務
- [ ] Code Review 流程熟悉

---

**參考文件**:
- `08-Architecture-Overview.md`: 整體架構
- `09-Frontend-Tech-Stack-Angular8.md`: 前端技術
- `10-Backend-Tech-Stack.md`: 後端技術

---

**文件版本**: v1.0
**最後更新**: 2025-10-27
**作者**: AI Architecture Team
