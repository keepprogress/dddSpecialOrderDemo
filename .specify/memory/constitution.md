<!--
  Sync Impact Report:

  Version Change: 1.0.1 → 1.1.0

  Modified Principles: N/A

  Added Sections:
  - Core Principles IV: Database Schema Documentation (資料表文件規範)

  Removed Sections: N/A

  Changes in v1.1.0:
  - 新增「Database Schema Documentation」原則：強制查閱 docs/tables 資料表文件
  - 明確查閱時機：設計、實作、除錯、Code Review 階段
  - 強調文件為唯一真實來源（Single Source of Truth）

  Templates Requiring Updates:
  - ✅ plan-template.md - Technical Context 應包含資料表文件參考檢查
  - ✅ spec-template.md - Key Entities 應參考 docs/tables 文件
  - ⚠ tasks-template.md - Implementation tasks 應包含查閱資料表文件的提醒

  Follow-up TODOs: None

  ---

  Version History:

  v1.1.0 (2025-12-17):
  - 新增 Database Schema Documentation 原則（MINOR: 新原則）

  v1.0.1 (2025-12-17):
  - 補充 3-Table Rule 的性能考量與例外情況（PATCH: 澄清）

  v1.0.0 (2025-12-17):
  - Initial creation
  - Core Principles (Pragmatic DDD, 3-Table Rule, KISS Principle)
  - Architecture Standards
  - Development Workflow
  - Governance
-->

# DDD Special Order Demo Constitution

## Core Principles

### I. Pragmatic DDD (Domain-Driven Design)

務實地套用 DDD 戰術模式與分層架構：
- **實體 (Entity)**、**值物件 (Value Object)**、**聚合 (Aggregate)** 僅在必要時使用
- **領域服務 (Domain Service)** 用於協調跨實體的業務邏輯
- **分層架構** 維持清晰的依賴方向：Presentation → Application → Domain → Infrastructure
- **反腐層 (Anti-Corruption Layer)** 用於隔離外部系統的複雜性
- **避免過度設計**：不為了 DDD 而 DDD，優先考慮可讀性與可維護性

**理由**：DDD 提供了清晰的業務邏輯組織方式，但過度使用會導致不必要的複雜性。務實的 DDD 在保持業務清晰的同時，避免過度抽象。

### II. 3-Table Rule (三表原則)

**關聯在 3 個 Table 以內的領域應套用 DDD，超過 3 個 Table 的關聯需要重新審視設計：**
- **1-3 Table**：套用 DDD 戰術模式（Entity, Value Object, Aggregate）
- **3+ Table**：考慮是否需要拆分 Bounded Context（限界上下文）或使用更輕量的模式
- **理由**：複雜的關聯通常意味著領域邊界不清晰，或混合了多個業務概念

**性能考量（Performance Considerations）：**
- **索引風險**：多表關聯若無法有效使用資料庫索引，會將過濾條件轉移到應用層處理
- **I/O 風險**：這會導致百萬筆資料的 input/output，造成嚴重的性能瓶頸
- **查詢計畫**：超過 3 個 Table 的 JOIN 查詢，資料庫優化器難以生成最佳執行計畫
- **網路傳輸**：大量資料在資料庫與應用層之間傳輸，消耗網路頻寬與記憶體

**例外情況（Exceptions）：**
- **非歷史資料**：若 Table 確定不是會持續增長的歷史資料類型（例如：設定檔、代碼表、元資料），則無百萬筆資料的風險
- **小資料集**：已確認資料量小且成長可控的情況下（例如：< 10,000 筆），可放寬此限制
- **必須記錄**：即使符合例外情況，仍需在 `plan.md` 明確記錄資料量評估與成長預測

**理由**：3-Table Rule 是基於性能與設計的雙重考量。除了領域邊界的清晰性外，更重要的是避免因多表關聯導致的性能災難，尤其在處理大量歷史資料時。

### III. KISS Principle (Keep It Simple, Stupid)

**簡單優於複雜，可讀性優於聰明：**
- 優先選擇最簡單的解決方案
- 不為未來可能不會發生的需求設計（YAGNI - You Aren't Gonna Need It）
- 程式碼應該一目了然，避免過度抽象
- 抽象層級應該有明確的業務價值，而非技術炫技
- **測試優先**：複雜的設計必須通過測試證明其必要性

**理由**：簡單的程式碼更容易理解、測試、維護與除錯。過早的抽象與過度設計通常會增加技術債，而非減少。

### IV. Database Schema Documentation (資料表文件規範)

**當對資料表欄位名稱、長度、意義有疑問時，必須查閱 `docs/tables` 目錄：**
- **強制查閱**：在設計或實作涉及資料庫欄位的功能前，必須先查閱相關資料表的文件
- **文件位置**：`docs/tables/[TABLE_NAME].html` 包含完整的欄位定義、資料型別、長度限制、註解
- **避免假設**：不可根據欄位名稱猜測其意義、長度或限制，必須以文件為準
- **發現不一致**：若發現資料庫實際結構與文件不一致，必須立即記錄並通知團隊更新文件

**查閱時機（When to Consult）：**
1. **設計階段**：規劃新功能涉及資料表查詢或寫入時
2. **實作階段**：撰寫 SQL 查詢、Mapper XML 或 Entity 類別時
3. **除錯階段**：發現資料異常或欄位截斷問題時
4. **Code Review**：審查涉及資料庫操作的程式碼時

**理由**：資料表文件是資料庫結構的唯一真實來源（Single Source of Truth）。依賴文件而非假設，可避免欄位長度溢位、資料型別錯誤、業務邏輯誤解等常見問題。

## Architecture Standards

### Technology Stack Requirements

**Backend (後端)**：
- Framework: Spring Boot 3.x+
- Language: Java 21+
- Build Tool: Maven
- Database: Oracle 21c (Production), H2 (Development)
- Security: Jasypt encryption for sensitive configuration

**Frontend (前端)**：
- Framework: Angular 21+
- Language: TypeScript 5.9+
- Build Tool: Angular CLI (via Maven frontend-plugin)

### DDD Layer Architecture

```
Presentation Layer (展示層)
└── API Controllers / Angular Components

Application Layer (應用層)
└── Application Services / Use Cases

Domain Layer (領域層)
└── Entities, Value Objects, Domain Services, Aggregates

Infrastructure Layer (基礎設施層)
└── Repositories, External API Clients, Database Access
```

**Dependencies Flow**: Presentation → Application → Domain ← Infrastructure
**Rule**: 領域層 (Domain Layer) 不可依賴基礎設施層 (Infrastructure Layer)

### Bounded Context Guidelines

- **識別標準**：當發現超過 3-Table 關聯時，優先考慮拆分 Bounded Context
- **Context Map**：不同 Bounded Context 之間使用明確的 Context Map 定義關係
- **整合方式**：Bounded Context 間透過 Anti-Corruption Layer 或 Published Language 整合

## Development Workflow

### Constitution Compliance Gates

**Phase 0 (Research)**：
1. 確認功能範圍是否超過 3-Table 關聯
2. 若超過，明確記錄拆分 Bounded Context 的理由或設計決策

**Phase 1 (Design)**：
1. 驗證 DDD 戰術模式的使用是否合理（避免過度設計）
2. 確認分層架構的依賴方向正確
3. 檢查是否遵循 KISS 原則（是否有不必要的抽象）

**Phase 2 (Implementation)**：
1. Code Review 必須檢查 3-Table Rule 合規性
2. 複雜設計必須有測試證明其必要性
3. 拒絕未通過 Constitution Check 的 Pull Request

### Testing Strategy

- **單元測試 (Unit Tests)**：測試領域層的業務邏輯
- **整合測試 (Integration Tests)**：測試跨層的互動與資料庫操作
- **契約測試 (Contract Tests)**：測試 API 契約的一致性

### Complexity Justification

任何違反 Constitution 的設計（例如：超過 3-Table 關聯、過度抽象）必須在 `plan.md` 的 **Complexity Tracking** 表格中記錄：
- 違反的原則
- 為何需要此複雜度
- 為何較簡單的替代方案不可行

## Governance

### Amendment Process

1. **提案**：任何團隊成員可提出 Constitution 修正案
2. **討論**：團隊討論修正案的影響範圍與必要性
3. **批准**：需要技術負責人批准
4. **遷移**：批准後，更新相關文件與模板，並提供遷移計畫

### Versioning Policy

- **MAJOR 版本**：原則的刪除或重大重新定義（例如：移除 3-Table Rule）
- **MINOR 版本**：新增原則或擴充現有指導方針
- **PATCH 版本**：澄清、措辭修改、錯字修正

### Compliance Review

- **Pull Request Review**：所有 PR 必須驗證 Constitution 合規性
- **定期審查**：每季度檢視 Constitution 的有效性與適用性
- **違規處理**：違反 Constitution 的程式碼必須重構或提供明確的違規理由

**Version**: 1.1.0 | **Ratified**: 2025-12-17 | **Last Amended**: 2025-12-17
