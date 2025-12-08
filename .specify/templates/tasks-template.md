---

description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), data-model.md (Schema definition)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel
- **[Story]**: Which user story this task belongs to

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Create project structure / update dependencies (Spring Boot/MyBatis)
- [ ] T002 Configure MyBatis Generator (MBG) for new tables
- [ ] T003 [P] Configure Angular environment

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure & Schema

- [ ] T004 **DB Schema**: Create/Update DDL scripts (Flyway/Liquibase)
- [ ] T005 **Generate**: Run MyBatis Generator to create Mappers/XML/POJOs
- [ ] T006 [P] Setup Authentication/Authorization basics
- [ ] T007 Create base Repository interfaces

**Checkpoint**: Mappers and POJOs generated.

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description]

### Tests for User Story 1 ⚠️

> **Test-First**: Define Repository behavior before implementation.

- [ ] T010 [P] [US1] Integration test for Repository (H2/TestContainer)
- [ ] T011 [P] [US1] Controller/Service Contract Test

### Implementation for User Story 1

- [ ] T012 [P] [US1] **Domain**: Extend MBG POJO to create Rich Entity (add behaviors)
- [ ] T013 [P] [US1] **Repo**: Implement Repository using Mappers (Infrastructure Layer)
- [ ] T014 [US1] **Service**: Implement Business Logic (Delegates to Repo/Entity)
- [ ] T015 [US1] **API**: Implement Controller & DTOs
- [ ] T016 [US1] **Frontend**: Angular Service & Component
- [ ] T017 [US1] Verify Explicit Save (`repository.save()`) logic

**Checkpoint**: User Story 1 functional and tested.

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

### Tests for User Story 2

- [ ] T018 [P] [US2] Integration test for Repository
- [ ] T019 [P] [US2] Contract/Unit Tests

### Implementation for User Story 2

- [ ] T020 [P] [US2] **Domain**: Define/Extend Rich Entities
- [ ] T021 [US2] **Repo**: Implement Repository
- [ ] T022 [US2] **Service/API**: Implement Logic & Endpoint
- [ ] T023 [US2] **Frontend**: Angular UI

---

## Phase N: Polish & Cross-Cutting

- [ ] TXXX Code cleanup (Check for SQL Injection/String concat)
- [ ] TXXX Performance optimization (Check generated SQL)
- [ ] TXXX Security hardening

---

## Implementation Strategy

1. **Database First**: Design schema, run MBG.
2. **Infrastructure**: Build Repositories wrapping Mappers.
3. **Domain**: Add logic to Entities/Services.
4. **API/UI**: Expose functionality.