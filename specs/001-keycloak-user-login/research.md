# Research: Keycloak 使用者登入與系統選擇

**Feature**: 001-keycloak-user-login
**Date**: 2025-12-17
**Status**: Complete

## Research Topics

1. Keycloak Angular 整合最佳實踐
2. Spring Boot Keycloak 整合
3. 6-checkpoint 驗證實作方式
4. 單一分頁限制實作方式
5. Token 自動刷新機制

---

## 1. Keycloak Angular 整合

### Decision: 使用 keycloak-angular 官方套件

### Rationale
- `keycloak-angular` 是 Keycloak 官方維護的 Angular 整合套件
- 提供 Angular Guard、HTTP Interceptor、初始化流程
- 支援 OAuth2 Authorization Code Flow with PKCE
- 內建 Token 自動刷新機制

### Alternatives Considered
| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| keycloak-angular | 官方支援、功能完整 | 依賴 keycloak-js | **採用** |
| 自行實作 OAuth2 | 無額外依賴 | 開發成本高、易出錯 | 拒絕 |
| angular-oauth2-oidc | 通用 OIDC 套件 | 需額外設定 Keycloak | 拒絕 |

### Implementation Pattern

```typescript
// app.config.ts
import { provideKeycloak, withAutoRefreshToken } from 'keycloak-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideKeycloak({
      config: {
        url: 'https://authempsit02.testritegroup.com/auth',
        realm: 'testritegroup-employee',
        clientId: 'epos-frontend'
      },
      initOptions: {
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html'
      }
    }),
    withAutoRefreshToken({
      onInactivityTimeout: 'logout',
      sessionTimeout: 60000 * 60 // 60 minutes
    })
  ]
};
```

---

## 2. Spring Boot Keycloak 整合

### Decision: 使用 Spring Security OAuth2 Resource Server

### Rationale
- Spring Boot 3.x 不再支援 Keycloak Adapter（已廢棄）
- 改用 Spring Security OAuth2 Resource Server 驗證 JWT Token
- 更符合標準 OAuth2/OIDC 規範

### Alternatives Considered
| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| Spring Security OAuth2 | 標準方式、長期支援 | 需自行設定 | **採用** |
| Keycloak Adapter (deprecated) | 設定簡單 | 已廢棄、不支援 Spring Boot 3 | 拒絕 |
| 自行驗證 JWT | 完全控制 | 開發成本高 | 拒絕 |

### Implementation Pattern

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://authempsit02.testritegroup.com/auth/realms/testritegroup-employee
          jwk-set-uri: https://authempsit02.testritegroup.com/auth/realms/testritegroup-employee/protocol/openid-connect/certs
```

```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );
        return http.build();
    }
}
```

---

## 3. 6-Checkpoint 驗證實作

### Decision: 後端 Domain Service 實作驗證邏輯

### Rationale
- 驗證邏輯屬於業務規則，應在 Domain Layer 實作
- 與資料庫查詢分離，便於測試
- 明確的錯誤訊息對應每個 checkpoint

### Implementation Pattern

```java
// UserValidationService.java (Domain Service)
@Service
public class UserValidationService {

    public ValidationResult validate(User user) {
        // Checkpoint 1: User exists (handled by caller)

        // Checkpoint 2: SYSTEM_FLAG not null
        if (user.getSystemFlag() == null || user.getSystemFlag().isEmpty()) {
            return ValidationResult.fail("SYSTEM_FLAG_NULL", "使用者未被授權任何系統別");
        }

        // Checkpoint 3: Not disabled
        if ("Y".equals(user.getDisableFlag())) {
            return ValidationResult.fail("USER_DISABLED", "帳號已被停用，請聯繫管理員");
        }

        // Checkpoint 4: Dates set
        if (user.getEnableDate() == null || user.getDisableDate() == null) {
            return ValidationResult.fail("DATES_NOT_SET", "帳號啟用日期未設定，請聯繫管理員");
        }

        // Checkpoint 5: Within date range
        LocalDate today = LocalDate.now();
        if (today.isBefore(user.getEnableDate()) || today.isAfter(user.getDisableDate())) {
            return ValidationResult.fail("OUT_OF_DATE_RANGE", "帳號不在有效使用期間，請聯繫管理員");
        }

        // Checkpoint 6: Has function permissions (checked separately)

        return ValidationResult.success();
    }
}
```

---

## 4. 單一分頁限制實作

### Decision: 使用 BroadcastChannel API + LocalStorage

### Rationale
- BroadcastChannel API 允許同源分頁間通訊
- LocalStorage 作為備援（某些瀏覽器不支援 BroadcastChannel）
- 新分頁取代舊分頁，舊分頁顯示警告

### Alternatives Considered
| 方案 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| BroadcastChannel + LocalStorage | 即時通訊、廣泛支援 | 需處理相容性 | **採用** |
| SharedWorker | 更強大的共享狀態 | 支援度較差 | 拒絕 |
| 後端 Session 鎖定 | 集中控制 | 違反 Stateless 原則 | 拒絕 |
| WebSocket 通知 | 即時性好 | 複雜度高 | 拒絕 |

### Implementation Pattern

```typescript
// tab-manager.service.ts
@Injectable({ providedIn: 'root' })
export class TabManagerService {
  private readonly TAB_ID_KEY = 'som_active_tab_id';
  private readonly CHANNEL_NAME = 'som_tab_channel';
  private channel: BroadcastChannel;
  private tabId: string;

  constructor() {
    this.tabId = crypto.randomUUID();
    this.channel = new BroadcastChannel(this.CHANNEL_NAME);
    this.initializeTabControl();
  }

  private initializeTabControl(): void {
    // Announce this tab is active
    localStorage.setItem(this.TAB_ID_KEY, this.tabId);
    this.channel.postMessage({ type: 'NEW_TAB', tabId: this.tabId });

    // Listen for other tabs
    this.channel.onmessage = (event) => {
      if (event.data.type === 'NEW_TAB' && event.data.tabId !== this.tabId) {
        // Another tab opened - this tab should deactivate
        this.showBlockedMessage();
      }
    };

    // Also listen to storage changes (fallback)
    window.addEventListener('storage', (event) => {
      if (event.key === this.TAB_ID_KEY && event.newValue !== this.tabId) {
        this.showBlockedMessage();
      }
    });
  }

  private showBlockedMessage(): void {
    // Show modal and disable interactions
    // Navigate to blocked page or overlay
  }
}
```

---

## 5. Token 自動刷新機制

### Decision: 使用 keycloak-angular 內建機制 + HTTP Interceptor

### Rationale
- keycloak-angular 提供 `withAutoRefreshToken` 功能
- HTTP Interceptor 自動附加 Bearer Token
- 統一處理 Token 過期錯誤

### Implementation Pattern

```typescript
// auth.interceptor.ts
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private keycloak: KeycloakService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip for public endpoints
    if (this.isPublicEndpoint(req.url)) {
      return next.handle(req);
    }

    return from(this.keycloak.getToken()).pipe(
      switchMap(token => {
        const authReq = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next.handle(authReq);
      }),
      catchError(error => {
        if (error.status === 401) {
          // Token expired, redirect to login
          this.keycloak.login();
        }
        return throwError(() => error);
      })
    );
  }

  private isPublicEndpoint(url: string): boolean {
    const publicPaths = ['/api/health', '/api/public'];
    return publicPaths.some(path => url.includes(path));
  }
}
```

---

## 6. 主店別 NULL 邏輯（全區權限）

### Decision: 前端顯示「全區」選項，後端查詢時不加店別過濾

### Rationale
- NULL 在資料庫代表「全區」權限
- 前端需將 NULL 轉換為使用者可理解的「全區」選項
- 後端查詢時，全區使用者不加 STORE_ID 過濾條件

### Implementation Pattern

```typescript
// store.model.ts
export interface UserMastStore {
  storeId: string | null;  // null = 全區
  storeName: string;
}

// store-selection.component.ts
getDisplayName(store: UserMastStore): string {
  return store.storeId === null ? '全區' : store.storeName;
}
```

```java
// StoreQueryService.java
public List<Order> findOrdersByUser(String userId, String storeId) {
    if (storeId == null) {
        // 全區權限 - 不加店別過濾
        return orderRepository.findByUserId(userId);
    } else {
        return orderRepository.findByUserIdAndStoreId(userId, storeId);
    }
}
```

---

## Summary of Decisions

| Topic | Decision | Key Library/Pattern |
|-------|----------|---------------------|
| Frontend Keycloak | keycloak-angular | withAutoRefreshToken |
| Backend Keycloak | Spring Security OAuth2 Resource Server | JWT validation |
| 6-Checkpoint | Domain Service | ValidationResult pattern |
| 單一分頁限制 | BroadcastChannel + LocalStorage | TabManagerService |
| Token 刷新 | keycloak-angular 內建 + Interceptor | AuthInterceptor |
| 全區權限 | NULL = 全區，查詢不加過濾 | Conditional query |
