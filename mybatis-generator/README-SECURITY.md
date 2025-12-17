# MyBatis Generator 密碼安全指南

## 為何不能使用 Jasypt？

MyBatis Generator 是 **build-time** 工具，在 Maven 建置階段執行，而非 Spring Boot runtime。
Jasypt 主要用於 Spring Boot 應用程式的 runtime 配置加密，因此無法直接套用。

## 推薦的密碼保護方式

### 方式 1: 環境變數（推薦）

最簡單且安全的方式，密碼不會出現在任何檔案中。

**Windows (CMD)**:
```cmd
set MBG_DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
set MBG_DB_USER=som_user
set MBG_DB_PASSWORD=your_password
cd mybatis-generator
mvn mybatis-generator:generate
```

**Windows (PowerShell)**:
```powershell
$env:MBG_DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:MBG_DB_USER = "som_user"
$env:MBG_DB_PASSWORD = "your_password"
cd mybatis-generator
mvn mybatis-generator:generate
```

**Linux/macOS**:
```bash
export MBG_DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
export MBG_DB_USER="som_user"
export MBG_DB_PASSWORD="your_password"
cd mybatis-generator
mvn mybatis-generator:generate
```

### 方式 2: Maven Password Encryption

使用 Maven 內建的密碼加密機制。

**步驟 1: 建立 Master Password**
```bash
mvn --encrypt-master-password <your-master-password>
```
將輸出結果存入 `~/.m2/settings-security.xml`:
```xml
<settingsSecurity>
  <master>{encrypted-master-password}</master>
</settingsSecurity>
```

**步驟 2: 加密資料庫密碼**
```bash
mvn --encrypt-password <your-db-password>
```

**步驟 3: 設定 settings.xml**
在 `~/.m2/settings.xml` 中加入:
```xml
<servers>
  <server>
    <id>mbg-db</id>
    <username>som_user</username>
    <password>{encrypted-password}</password>
  </server>
</servers>
```

**步驟 4: 修改 generator.properties**
```properties
jdbc.userId=${settings.servers.server.mbg-db.username}
jdbc.password=${settings.servers.server.mbg-db.password}
```

### 方式 3: CI/CD 環境的 Secret Management

在 CI/CD 環境中，使用平台提供的 Secret 管理功能：

**GitHub Actions**:
```yaml
env:
  MBG_DB_URL: ${{ secrets.MBG_DB_URL }}
  MBG_DB_USER: ${{ secrets.MBG_DB_USER }}
  MBG_DB_PASSWORD: ${{ secrets.MBG_DB_PASSWORD }}
```

**GitLab CI**:
```yaml
variables:
  MBG_DB_URL: $MBG_DB_URL
  MBG_DB_USER: $MBG_DB_USER
  MBG_DB_PASSWORD: $MBG_DB_PASSWORD
```

**Jenkins**:
使用 Credentials Plugin 管理敏感資訊。

## 安全檢查清單

- [ ] `generator.properties` 不包含明碼密碼
- [ ] `generator.properties` 已加入 `.gitignore`（如果有本地覆寫版本）
- [ ] 環境變數或 Maven 加密已正確設定
- [ ] CI/CD pipeline 使用 Secret 管理

## 常見問題

### Q: 可以在 generatorConfig.xml 直接寫密碼嗎？
**A: 絕對不行！** 這會將密碼提交到版控系統。

### Q: generator.properties 可以提交到 Git 嗎？
**A: 可以**，但必須確保只包含 `${env.XXX}` 變數引用，不包含實際密碼。

### Q: 本地開發時每次都要設定環境變數嗎？
**A: 建議這樣做**。或者可以建立一個不提交到 Git 的本地腳本（如 `run-generator.bat`）來設定環境變數。

## 本地開發腳本範例

建立 `mybatis-generator/run-generator.bat`（加入 .gitignore）:
```batch
@echo off
set MBG_DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
set MBG_DB_USER=som_dev
set MBG_DB_PASSWORD=dev_password
mvn mybatis-generator:generate
```
