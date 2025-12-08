# dddSpecilOrderDemo Constitution
<!-- Sync Impact Report:
- Version Change: 1.0.0 -> 1.1.0 (MyBatis Edition Specification)
- Modified Principles:
  - Pragmatic DDD -> Architecture & Integration (Specific MyBatis Rules)
  - GRASP/RDD -> Domain Modeling (Rich Domain vs Anemic POJO)
  - Test-First -> Database-First Workflow
- Added Sections: Tech Stack, Persistence Rules, Query Safety
- Templates Updated:
  - .specify/templates/plan-template.md (Tech stack defaults set)
  - .specify/templates/tasks-template.md (Workflow alignment: Schema -> MBG -> Test)
-->

## Core Principles

### I. Architecture & Integration (The MyBatis Rule)
**Controller ➡ Repository ➡ MyBatis Mapper**.
MyBatis Mappers and generated artifacts (XML, POJOs) are **Infrastructure**. They must never be exposed to the Web layer (Controllers) or directly used by Services without a Repository wrapper. This ensures the domain is decoupled from persistence details.

### II. Domain Modeling Strategy (Rich Domain)
**Domain Entity ≠ MBG POJO**.
Avoid Anemic Domain Models. Do not use raw MyBatis Generator (MBG) POJOs as business objects if they only contain getters/setters.
*   **Do**: Create "Rich" entities that inherit from POJOs or wrap them, adding business behaviors.
*   **Don't**: Scatter business logic across Services while keeping objects empty.

### III. Explicit Persistence
**No Dirty Checking — Save Explicitly**.
Unlike JPA, MyBatis does not automatically track changes. You **MUST** explicitly call `repository.save()` (or `update`) to persist changes to modified entities.
*   **Workflow**: Retrieve → Modify → **Save** → Return.

### IV. Safe Querying & Interaction
**Use Dynamic SQL / Example Classes**.
For search and filtering, use MyBatis Dynamic SQL or MBG `Example` classes.
*   **Prohibited**: String concatenation for SQL construction (SQL Injection risk).
*   **Prohibited**: Complex joins in XML for simple reads (Keep it maintainable).

### V. Database-First Workflow (Test-Driven)
**Schema ➡ Generator ➡ Test ➡ Implementation**.
1.  Design Table Schema.
2.  Run MyBatis Generator (MBG).
3.  **Write Test**: Define how the Repository should behave.
4.  Implement Repository using Mappers.

## Technology Stack

*   **Language**: Java 21 (LTS)
*   **Framework**: Spring Boot 3.3+
*   **Persistence**: MyBatis 3.5+ & MyBatis Generator (MBG)
*   **Frontend**: Angular (Latest, Standalone)

## Governance

### Amendment Process
This constitution defines the "MyBatis Edition" architectural style for the project. Amendments changing the fundamental stack or layering require a Major version bump and migration plan.

### Compliance
Code reviews must reject:
1.  Controllers injecting Mappers directly.
2.  Service logic performing raw SQL string manipulation.
3.  "Anemic" usage where business logic exists solely in Services.

**Version**: 1.1.0 | **Ratified**: 2025-12-08 | **Last Amended**: 2025-12-08
