# DDD Special Order Demo

這是一個基於 **Domain-Driven Design (DDD)** 概念的特殊訂單演示專案，採用現代化的前後端分離架構。

## 技術堆疊 (Tech Stack)

### Backend (後端)
- **Framework**: Spring Boot 3.4.0
- **Language**: Java 21 (Microsoft OpenJDK 21)
- **Build Tool**: Maven (Wrapper included)
- **Structure**: DDD Layered Architecture (Planned)

### Frontend (前端)
- **Framework**: Angular 21+
- **Language**: TypeScript 5.9
- **Build Tool**: Angular CLI (via Maven frontend-plugin)
- **Style**: CSS

## 專案結構 (Structure)

```
.
├── backend/            # Spring Boot 後端程式碼
├── frontend/           # Angular 前端程式碼
├── pom.xml             # Maven Root Aggregator
└── mvnw                # Maven Wrapper
```

## 快速開始 (Quick Start)

### 前置需求
- Java 21+
- (Optional) Node.js v22+ (Maven 會自動在 target 目錄安裝 Node，但本機有會更好除錯)

### 一鍵建置與執行 (Build All)
在專案根目錄執行：

```bash
# Windows
.\mvnw clean install

# Linux/Mac
./mvnw clean install
```

此指令會：
1. 下載 Node.js 與 NPM。
2. 安裝前端依賴 (`npm install`)。
3. 建置前端 (`npm run build`)。
4. 建置後端 (`mvn package`)。

### 分開開發 (Development)

**啟動後端：**
```bash
cd backend
mvn spring-boot:run
```

**啟動前端：**
```bash
cd frontend
npm start
# 預設網址: http://localhost:4200
```
