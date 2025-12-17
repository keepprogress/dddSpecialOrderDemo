# 28. Frontend - Project Setup

## Angular 8 專案初始化

```bash
# 建立專案
ng new som-frontend --routing --style=scss
cd som-frontend

# 安裝依賴
npm install @ngrx/store@8.6.0 @ngrx/effects@8.6.0
npm install @angular/material@8.2.3 @angular/cdk@8.2.3
npm install rxjs@6.5.5

# 建立模組
ng g module core
ng g module shared
ng g module features/order --routing
ng g module features/pricing --routing
```

## 專案結構

```plaintext
som-frontend/
├── src/
│   ├── app/
│   │   ├── core/                # 核心模組 (單例服務)
│   │   │   ├── services/
│   │   │   │   ├── api.service.ts
│   │   │   │   └── auth.service.ts
│   │   │   ├── guards/
│   │   │   │   └── auth.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   └── core.module.ts
│   │   ├── shared/              # 共用模組
│   │   │   ├── components/
│   │   │   │   ├── header/
│   │   │   │   └── footer/
│   │   │   ├── pipes/
│   │   │   └── shared.module.ts
│   │   ├── features/            # 功能模組
│   │   │   ├── order/
│   │   │   │   ├── components/
│   │   │   │   ├── services/
│   │   │   │   └── order.module.ts
│   │   │   └── pricing/
│   │   ├── store/               # NgRx Store
│   │   │   ├── order/
│   │   │   │   ├── order.actions.ts
│   │   │   │   ├── order.reducer.ts
│   │   │   │   ├── order.effects.ts
│   │   │   │   └── order.selectors.ts
│   │   │   └── app.state.ts
│   │   ├── app.component.ts
│   │   └── app.module.ts
│   ├── assets/
│   ├── environments/
│   │   ├── environment.ts       # Dev
│   │   ├── environment.sit.ts   # SIT
│   │   └── environment.prod.ts  # Prod
│   └── styles.scss
└── angular.json
```

## 核心設定

**environment.ts**:
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  apiTimeout: 30000
};
```

**app.module.ts**:
```typescript
@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    CoreModule,
    SharedModule,
    StoreModule.forRoot({}),
    EffectsModule.forRoot([]),
    StoreDevtoolsModule.instrument({
      maxAge: 25,
      logOnly: environment.production
    })
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {}
```

---

**參考文件**: `09-Frontend-Tech-Stack-Angular8.md`

**文件版本**: v1.0
