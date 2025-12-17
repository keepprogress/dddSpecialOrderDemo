# 09. 前端技術棧 - Angular 8 (Frontend Tech Stack - Angular 8)

## 文檔資訊
- **版本**: 1.0.0
- **建立日期**: 2025-10-27
- **相關文檔**:
  - [08-Architecture-Overview.md](./08-Architecture-Overview.md)
  - [28-Frontend-Project-Setup.md](./28-Frontend-Project-Setup.md)
  - [29-Frontend-State-Management.md](./29-Frontend-State-Management.md)

---

## 目錄
1. [技術棧總覽](#技術棧總覽)
2. [核心框架 - Angular 8](#核心框架---angular-8)
3. [狀態管理 - NgRx](#狀態管理---ngrx)
4. [UI 元件庫](#ui-元件庫)
5. [表單處理](#表單處理)
6. [HTTP 通訊](#http-通訊)
7. [路由與守衛](#路由與守衛)
8. [開發工具](#開發工具)

---

## 技術棧總覽

### 完整技術清單

```
┌─────────────────────────────────────────────────────────────────┐
│  Angular 8 Frontend Stack                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  核心框架:                                                       │
│  ├─ Angular 8.x (Latest 8.2.14)                                │
│  ├─ TypeScript 3.5.x                                           │
│  ├─ RxJS 6.5.x                                                 │
│  └─ Zone.js 0.9.x                                              │
│                                                                 │
│  狀態管理:                                                       │
│  ├─ @ngrx/store 8.x (Redux pattern)                           │
│  ├─ @ngrx/effects 8.x (Side effects)                          │
│  ├─ @ngrx/router-store 8.x (Router state)                     │
│  └─ @ngrx/store-devtools 8.x (DevTools)                       │
│                                                                 │
│  UI 元件庫:                                                      │
│  ├─ Angular Material 8.x (Primary UI)                         │
│  ├─ CDK (Component Dev Kit)                                    │
│  └─ Flex Layout 8.x (Responsive layout)                       │
│                                                                 │
│  表單處理:                                                       │
│  ├─ Reactive Forms (Angular built-in)                         │
│  └─ Custom Validators                                          │
│                                                                 │
│  HTTP 通訊:                                                      │
│  ├─ HttpClient (Angular built-in)                             │
│  └─ Interceptors (Auth, Error, Logging)                       │
│                                                                 │
│  路由:                                                          │
│  ├─ Angular Router                                             │
│  └─ Route Guards (Auth, Role)                                 │
│                                                                 │
│  測試:                                                          │
│  ├─ Jasmine 3.x (Test framework)                              │
│  ├─ Karma 4.x (Test runner)                                   │
│  └─ Protractor 5.x (E2E testing)                              │
│                                                                 │
│  建構工具:                                                       │
│  ├─ Angular CLI 8.x                                            │
│  ├─ Webpack (bundled with CLI)                                │
│  └─ TypeScript Compiler                                        │
│                                                                 │
│  程式碼品質:                                                     │
│  ├─ TSLint 5.x                                                 │
│  ├─ Prettier (Code formatter)                                  │
│  └─ Husky (Git hooks)                                          │
│                                                                 │
│  其他工具:                                                       │
│  ├─ date-fns (Date utilities)                                 │
│  ├─ lodash-es (Utility library)                               │
│  └─ ng-zorro-antd (Optional: Ant Design)                      │
└─────────────────────────────────────────────────────────────────┘
```

### 版本相容性矩陣

| Package | Version | Angular 8 相容性 |
|---------|---------|------------------|
| Angular | 8.2.14 | ✅ |
| TypeScript | 3.4.x - 3.5.x | ✅ |
| RxJS | 6.4.x - 6.5.x | ✅ |
| Zone.js | 0.9.x | ✅ |
| @ngrx/store | 8.6.x | ✅ |
| Angular Material | 8.2.x | ✅ |
| Node.js | 10.x - 12.x | ✅ |

---

## 核心框架 - Angular 8

### Angular 8 特性

#### 1. Differential Loading

Angular 8 支援差異化載入，為現代瀏覽器和舊版瀏覽器生成不同的 bundle:

```json
// tsconfig.json
{
  "compilerOptions": {
    "target": "es2015",  // 現代瀏覽器
    "module": "esnext"
  }
}

// 生成的 bundle:
// - main-es2015.js (現代瀏覽器，體積較小)
// - main-es5.js (舊版瀏覽器，體積較大)
```

```html
<!-- index.html - 自動載入適當版本 -->
<script src="runtime-es2015.js" type="module"></script>
<script src="runtime-es5.js" nomodule defer></script>
```

**效益**:
- 現代瀏覽器 (Chrome, Firefox, Edge): Bundle 體積 -20%
- 舊版瀏覽器 (IE11): 仍可正常運作

---

#### 2. Ivy Preview

Angular 8 提供 Ivy 渲染引擎預覽版:

```json
// tsconfig.app.json
{
  "angularCompilerOptions": {
    "enableIvy": true  // 啟用 Ivy (實驗性)
  }
}
```

**Ivy 優勢**:
- Bundle 體積更小 (-30% to -40%)
- 編譯速度更快 (+50%)
- 除錯更友善 (更好的錯誤訊息)

**建議**: 正式環境暫不啟用，等待 Angular 9 正式支援

---

#### 3. Lazy Loading 優化

動態 import 語法支援:

```typescript
// app-routing.module.ts - Angular 8 Lazy Loading
const routes: Routes = [
  {
    path: 'orders',
    loadChildren: () => import('./orders/orders.module')
      .then(m => m.OrdersModule)  // ✅ 動態 import
  },
  {
    path: 'pricing',
    loadChildren: () => import('./pricing/pricing.module')
      .then(m => m.PricingModule)
  }
];

// 舊寫法 (Angular 7 以前):
// loadChildren: './orders/orders.module#OrdersModule'  // ❌ 字串路徑
```

**效益**:
- TypeScript 型別檢查
- IDE 自動完成
- 重構更安全

---

### 專案結構

```
som-frontend/
├── src/
│   ├── app/
│   │   ├── core/                    # 核心模組 (單例服務)
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── api.service.ts
│   │   │   │   └── logger.service.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   ├── error.interceptor.ts
│   │   │   │   └── logging.interceptor.ts
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── role.guard.ts
│   │   │   └── core.module.ts
│   │   │
│   │   ├── shared/                  # 共用模組
│   │   │   ├── components/
│   │   │   │   ├── loading-spinner/
│   │   │   │   ├── error-message/
│   │   │   │   └── confirm-dialog/
│   │   │   ├── directives/
│   │   │   │   └── number-only.directive.ts
│   │   │   ├── pipes/
│   │   │   │   ├── currency-format.pipe.ts
│   │   │   │   └── date-format.pipe.ts
│   │   │   ├── models/
│   │   │   │   ├── api-response.model.ts
│   │   │   │   └── error.model.ts
│   │   │   └── shared.module.ts
│   │   │
│   │   ├── features/                # 功能模組
│   │   │   ├── orders/              # 訂單模組
│   │   │   │   ├── components/
│   │   │   │   │   ├── order-list/
│   │   │   │   │   ├── order-create/
│   │   │   │   │   └── order-detail/
│   │   │   │   ├── services/
│   │   │   │   │   └── order.service.ts
│   │   │   │   ├── store/           # NgRx Store
│   │   │   │   │   ├── actions/
│   │   │   │   │   ├── reducers/
│   │   │   │   │   ├── effects/
│   │   │   │   │   └── selectors/
│   │   │   │   ├── models/
│   │   │   │   │   └── order.model.ts
│   │   │   │   ├── orders-routing.module.ts
│   │   │   │   └── orders.module.ts
│   │   │   │
│   │   │   ├── pricing/             # 計價模組
│   │   │   │   ├── components/
│   │   │   │   │   ├── price-calculator/
│   │   │   │   │   ├── discount-detail/
│   │   │   │   │   └── price-summary/
│   │   │   │   ├── services/
│   │   │   │   │   └── pricing.service.ts
│   │   │   │   ├── store/
│   │   │   │   ├── models/
│   │   │   │   ├── pricing-routing.module.ts
│   │   │   │   └── pricing.module.ts
│   │   │   │
│   │   │   └── members/             # 會員模組
│   │   │       ├── components/
│   │   │       ├── services/
│   │   │       ├── store/
│   │   │       ├── models/
│   │   │       ├── members-routing.module.ts
│   │   │       └── members.module.ts
│   │   │
│   │   ├── store/                   # Root Store
│   │   │   ├── app.state.ts
│   │   │   ├── app.effects.ts
│   │   │   └── app.reducers.ts
│   │   │
│   │   ├── app-routing.module.ts
│   │   ├── app.component.ts
│   │   ├── app.component.html
│   │   ├── app.component.scss
│   │   └── app.module.ts
│   │
│   ├── assets/                      # 靜態資源
│   │   ├── images/
│   │   ├── i18n/                    # 國際化檔案
│   │   │   ├── en.json
│   │   │   └── zh-TW.json
│   │   └── styles/
│   │       └── _variables.scss
│   │
│   ├── environments/                # 環境配置
│   │   ├── environment.ts           # Development
│   │   ├── environment.sit.ts       # SIT
│   │   ├── environment.stg.ts       # Staging
│   │   └── environment.prod.ts      # Production
│   │
│   ├── styles.scss                  # 全域樣式
│   ├── index.html
│   └── main.ts
│
├── angular.json                     # Angular CLI 配置
├── package.json
├── tsconfig.json
├── tslint.json
└── README.md
```

---

## 狀態管理 - NgRx

### 為什麼使用 NgRx?

```
問題：前端狀態管理複雜
├─ 多個元件共享狀態 (訂單資料、會員資訊)
├─ 狀態變更追蹤困難
├─ 非同步操作管理 (API 呼叫)
└─ 元件間通訊複雜

解決方案：NgRx (Redux pattern)
├─ ✅ 單一資料來源 (Single Source of Truth)
├─ ✅ 不可變資料 (Immutable State)
├─ ✅ 可預測的狀態變更 (Pure Functions)
└─ ✅ 時光旅行除錯 (Time Travel Debugging)
```

### NgRx 資料流

```
┌─────────────────────────────────────────────────────────────────┐
│  Component (Smart Component)                                    │
│                                                                 │
│  orderList$ = this.store.select(selectOrders);                 │
│  dispatch(loadOrders())  ───────────┐                          │
└─────────────────────────────────────┼───────────────────────────┘
                                      │
                                      │ 1️⃣ Dispatch Action
                                      ↓
┌─────────────────────────────────────────────────────────────────┐
│  Store (NgRx Store)                                             │
│                                                                 │
│  @ngrx/store                                                    │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 2️⃣ Action 進入 Effects
           ↓
┌─────────────────────────────────────────────────────────────────┐
│  Effects (@ngrx/effects)                                        │
│                                                                 │
│  loadOrders$ = createEffect(() =>                              │
│    this.actions$.pipe(                                         │
│      ofType(loadOrders),                                       │
│      switchMap(() =>                                           │
│        this.orderService.getOrders()  ─────> 3️⃣ 呼叫 API        │
│          .pipe(                                                │
│            map(orders => loadOrdersSuccess({ orders })),       │
│            catchError(error => of(loadOrdersFailure({ error })))│
│          )                                                     │
│      )                                                         │
│    )                                                           │
│  );                                                            │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 4️⃣ Dispatch Success/Failure Action
           ↓
┌─────────────────────────────────────────────────────────────────┐
│  Reducer (Pure Function)                                        │
│                                                                 │
│  on(loadOrdersSuccess, (state, { orders }) => ({               │
│    ...state,                                                   │
│    orders: orders,                                             │
│    loading: false                                              │
│  }))                                                           │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 5️⃣ 更新 Store
           ↓
┌─────────────────────────────────────────────────────────────────┐
│  Store (Updated State)                                          │
└──────────┬──────────────────────────────────────────────────────┘
           │
           │ 6️⃣ Selector 選擇資料
           ↓
┌─────────────────────────────────────────────────────────────────┐
│  Component (資料自動更新)                                        │
│                                                                 │
│  <div *ngFor="let order of orderList$ | async">                │
│    {{ order.orderId }}                                         │
│  </div>                                                        │
└─────────────────────────────────────────────────────────────────┘
```

### 實作範例: 訂單模組

**1. Actions 定義**
```typescript
// orders/store/actions/order.actions.ts
import { createAction, props } from '@ngrx/store';
import { Order } from '../../models/order.model';

// 載入訂單列表
export const loadOrders = createAction(
  '[Order List] Load Orders',
  props<{ page: number; size: number }>()
);

export const loadOrdersSuccess = createAction(
  '[Order API] Load Orders Success',
  props<{ orders: Order[]; total: number }>()
);

export const loadOrdersFailure = createAction(
  '[Order API] Load Orders Failure',
  props<{ error: any }>()
);

// 建立訂單
export const createOrder = createAction(
  '[Order Create] Create Order',
  props<{ order: Partial<Order> }>()
);

export const createOrderSuccess = createAction(
  '[Order API] Create Order Success',
  props<{ order: Order }>()
);

export const createOrderFailure = createAction(
  '[Order API] Create Order Failure',
  props<{ error: any }>()
);

// 選擇訂單
export const selectOrder = createAction(
  '[Order Detail] Select Order',
  props<{ orderId: string }>()
);
```

**2. State 定義**
```typescript
// orders/store/order.state.ts
import { Order } from '../models/order.model';

export interface OrderState {
  orders: Order[];
  selectedOrder: Order | null;
  loading: boolean;
  error: any;
  pagination: {
    page: number;
    size: number;
    total: number;
  };
}

export const initialOrderState: OrderState = {
  orders: [],
  selectedOrder: null,
  loading: false,
  error: null,
  pagination: {
    page: 0,
    size: 20,
    total: 0
  }
};
```

**3. Reducer 實作**
```typescript
// orders/store/reducers/order.reducer.ts
import { createReducer, on } from '@ngrx/store';
import * as OrderActions from '../actions/order.actions';
import { initialOrderState } from '../order.state';

export const orderReducer = createReducer(
  initialOrderState,

  // 載入訂單列表
  on(OrderActions.loadOrders, (state, { page, size }) => ({
    ...state,
    loading: true,
    error: null,
    pagination: { ...state.pagination, page, size }
  })),

  on(OrderActions.loadOrdersSuccess, (state, { orders, total }) => ({
    ...state,
    orders,
    loading: false,
    pagination: { ...state.pagination, total }
  })),

  on(OrderActions.loadOrdersFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // 建立訂單
  on(OrderActions.createOrder, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(OrderActions.createOrderSuccess, (state, { order }) => ({
    ...state,
    orders: [order, ...state.orders],
    loading: false
  })),

  on(OrderActions.createOrderFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // 選擇訂單
  on(OrderActions.selectOrder, (state, { orderId }) => ({
    ...state,
    selectedOrder: state.orders.find(o => o.orderId === orderId) || null
  }))
);
```

**4. Effects 實作**
```typescript
// orders/store/effects/order.effects.ts
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, switchMap, tap } from 'rxjs/operators';
import { OrderService } from '../../services/order.service';
import * as OrderActions from '../actions/order.actions';
import { Router } from '@angular/router';

@Injectable()
export class OrderEffects {

  loadOrders$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OrderActions.loadOrders),
      switchMap(({ page, size }) =>
        this.orderService.getOrders(page, size).pipe(
          map(response => OrderActions.loadOrdersSuccess({
            orders: response.data,
            total: response.total
          })),
          catchError(error => of(OrderActions.loadOrdersFailure({ error })))
        )
      )
    )
  );

  createOrder$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OrderActions.createOrder),
      switchMap(({ order }) =>
        this.orderService.createOrder(order).pipe(
          map(createdOrder => OrderActions.createOrderSuccess({
            order: createdOrder
          })),
          catchError(error => of(OrderActions.createOrderFailure({ error })))
        )
      )
    )
  );

  // 建立成功後導航到訂單詳情頁
  createOrderSuccess$ = createEffect(() =>
    this.actions$.pipe(
      ofType(OrderActions.createOrderSuccess),
      tap(({ order }) => {
        this.router.navigate(['/orders', order.orderId]);
      })
    ),
    { dispatch: false }  // 不發送新的 Action
  );

  constructor(
    private actions$: Actions,
    private orderService: OrderService,
    private router: Router
  ) {}
}
```

**5. Selectors 定義**
```typescript
// orders/store/selectors/order.selectors.ts
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { OrderState } from '../order.state';

export const selectOrderState = createFeatureSelector<OrderState>('orders');

export const selectAllOrders = createSelector(
  selectOrderState,
  (state) => state.orders
);

export const selectSelectedOrder = createSelector(
  selectOrderState,
  (state) => state.selectedOrder
);

export const selectOrdersLoading = createSelector(
  selectOrderState,
  (state) => state.loading
);

export const selectOrdersError = createSelector(
  selectOrderState,
  (state) => state.error
);

export const selectOrdersPagination = createSelector(
  selectOrderState,
  (state) => state.pagination
);

// 組合 Selector
export const selectValidOrders = createSelector(
  selectAllOrders,
  (orders) => orders.filter(o => o.status === 'VALID')
);

export const selectOrdersByMember = createSelector(
  selectAllOrders,
  (orders, props: { memberCardId: string }) =>
    orders.filter(o => o.memberCardId === props.memberCardId)
);
```

**6. Component 使用**
```typescript
// orders/components/order-list/order-list.component.ts
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Order } from '../../models/order.model';
import * as OrderActions from '../../store/actions/order.actions';
import * as OrderSelectors from '../../store/selectors/order.selectors';

@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss']
})
export class OrderListComponent implements OnInit {

  // 使用 Observable 訂閱 Store
  orders$: Observable<Order[]>;
  loading$: Observable<boolean>;
  error$: Observable<any>;
  pagination$: Observable<any>;

  constructor(private store: Store) {
    // 選擇需要的 State
    this.orders$ = this.store.select(OrderSelectors.selectAllOrders);
    this.loading$ = this.store.select(OrderSelectors.selectOrdersLoading);
    this.error$ = this.store.select(OrderSelectors.selectOrdersError);
    this.pagination$ = this.store.select(OrderSelectors.selectOrdersPagination);
  }

  ngOnInit(): void {
    // Dispatch Action 載入訂單
    this.store.dispatch(OrderActions.loadOrders({ page: 0, size: 20 }));
  }

  onPageChange(page: number): void {
    this.store.dispatch(OrderActions.loadOrders({ page, size: 20 }));
  }

  onSelectOrder(orderId: string): void {
    this.store.dispatch(OrderActions.selectOrder({ orderId }));
  }

  onCreateOrder(): void {
    // 導航到建立頁面
    // 或開啟 Dialog
  }
}
```

```html
<!-- order-list.component.html -->
<div class="order-list">
  <!-- Loading Spinner -->
  <mat-spinner *ngIf="loading$ | async"></mat-spinner>

  <!-- Error Message -->
  <mat-error *ngIf="error$ | async as error">
    {{ error.message }}
  </mat-error>

  <!-- Order Table -->
  <table mat-table [dataSource]="orders$ | async" *ngIf="!(loading$ | async)">
    <!-- Order ID Column -->
    <ng-container matColumnDef="orderId">
      <th mat-header-cell *matHeaderCellDef>訂單編號</th>
      <td mat-cell *matCellDef="let order">{{ order.orderId }}</td>
    </ng-container>

    <!-- Member Column -->
    <ng-container matColumnDef="memberCardId">
      <th mat-header-cell *matHeaderCellDef>會員卡號</th>
      <td mat-cell *matCellDef="let order">{{ order.memberCardId }}</td>
    </ng-container>

    <!-- Total Amount Column -->
    <ng-container matColumnDef="totalAmt">
      <th mat-header-cell *matHeaderCellDef>總金額</th>
      <td mat-cell *matCellDef="let order">
        {{ order.totalAmt | currency:'TWD' }}
      </td>
    </ng-container>

    <!-- Status Column -->
    <ng-container matColumnDef="status">
      <th mat-header-cell *matHeaderCellDef>狀態</th>
      <td mat-cell *matCellDef="let order">
        <mat-chip [color]="getStatusColor(order.status)">
          {{ order.statusName }}
        </mat-chip>
      </td>
    </ng-container>

    <!-- Actions Column -->
    <ng-container matColumnDef="actions">
      <th mat-header-cell *matHeaderCellDef>操作</th>
      <td mat-cell *matCellDef="let order">
        <button mat-icon-button (click)="onSelectOrder(order.orderId)">
          <mat-icon>visibility</mat-icon>
        </button>
      </td>
    </ng-container>

    <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
    <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
  </table>

  <!-- Pagination -->
  <mat-paginator
    *ngIf="pagination$ | async as pagination"
    [length]="pagination.total"
    [pageSize]="pagination.size"
    [pageIndex]="pagination.page"
    (page)="onPageChange($event.pageIndex)">
  </mat-paginator>
</div>
```

---

## UI 元件庫

### Angular Material 8

#### 安裝配置

```bash
# 安裝 Angular Material
ng add @angular/material

# 選擇主題
? Choose a prebuilt theme name: Indigo/Pink
? Set up global Angular Material typography styles? Yes
? Set up browser animations for Angular Material? Yes
```

#### 主題客製化

```scss
// styles.scss
@import '~@angular/material/theming';

@include mat-core();

// 定義主題色
$som-primary: mat-palette($mat-indigo);
$som-accent: mat-palette($mat-pink, A200, A100, A400);
$som-warn: mat-palette($mat-red);

// 建立主題
$som-theme: mat-light-theme((
  color: (
    primary: $som-primary,
    accent: $som-accent,
    warn: $som-warn,
  )
));

// 套用主題
@include angular-material-theme($som-theme);

// 暗色模式
.dark-theme {
  $dark-theme: mat-dark-theme((
    color: (
      primary: $som-primary,
      accent: $som-accent,
      warn: $som-warn,
    )
  ));

  @include angular-material-theme($dark-theme);
}
```

#### 常用元件

```typescript
// shared.module.ts - 匯入常用 Material 元件
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';

const MATERIAL_MODULES = [
  MatButtonModule,
  MatTableModule,
  MatPaginatorModule,
  MatSortModule,
  MatInputModule,
  MatFormFieldModule,
  MatSelectModule,
  MatDatepickerModule,
  MatNativeDateModule,
  MatDialogModule,
  MatSnackBarModule,
  MatProgressSpinnerModule,
  MatCardModule,
  MatToolbarModule,
  MatSidenavModule,
  MatListModule,
  MatIconModule,
  MatChipsModule,
  MatBadgeModule
];

@NgModule({
  imports: MATERIAL_MODULES,
  exports: MATERIAL_MODULES
})
export class MaterialModule {}
```

---

## 表單處理

### Reactive Forms

```typescript
// order-create.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';

@Component({
  selector: 'app-order-create',
  templateUrl: './order-create.component.html'
})
export class OrderCreateComponent implements OnInit {

  orderForm: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.orderForm = this.fb.group({
      memberCardId: ['', [
        Validators.required,
        Validators.pattern(/^[A-Z0-9]{10}$/)
      ]],
      orderDate: [new Date(), Validators.required],
      remark: ['', Validators.maxLength(500)],
      skus: this.fb.array([])  // Dynamic FormArray
    });

    // 新增第一個 SKU
    this.addSku();
  }

  get skus(): FormArray {
    return this.orderForm.get('skus') as FormArray;
  }

  addSku(): void {
    const skuGroup = this.fb.group({
      skuNo: ['', Validators.required],
      skuName: [''],
      quantity: [1, [Validators.required, Validators.min(1)]],
      sellingAmt: [0, [Validators.required, Validators.min(0)]],
      discountAmt: [0, Validators.min(0)]
    });

    this.skus.push(skuGroup);
  }

  removeSku(index: number): void {
    this.skus.removeAt(index);
  }

  onSubmit(): void {
    if (this.orderForm.valid) {
      const orderData = this.orderForm.value;
      this.store.dispatch(OrderActions.createOrder({ order: orderData }));
    } else {
      // 標記所有欄位為 touched，顯示驗證錯誤
      this.orderForm.markAllAsTouched();
    }
  }

  // 自訂驗證器
  static memberCardValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;

    // 驗證會員卡號格式
    if (!/^[A-Z0-9]{10}$/.test(value)) {
      return { invalidMemberCard: true };
    }

    return null;
  }
}
```

```html
<!-- order-create.component.html -->
<form [formGroup]="orderForm" (ngSubmit)="onSubmit()">
  <!-- 會員卡號 -->
  <mat-form-field>
    <mat-label>會員卡號</mat-label>
    <input matInput formControlName="memberCardId" placeholder="請輸入會員卡號">
    <mat-error *ngIf="orderForm.get('memberCardId').hasError('required')">
      會員卡號為必填
    </mat-error>
    <mat-error *ngIf="orderForm.get('memberCardId').hasError('pattern')">
      會員卡號格式錯誤
    </mat-error>
  </mat-form-field>

  <!-- 訂單日期 -->
  <mat-form-field>
    <mat-label>訂單日期</mat-label>
    <input matInput [matDatepicker]="picker" formControlName="orderDate">
    <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
    <mat-datepicker #picker></mat-datepicker>
  </mat-form-field>

  <!-- 商品列表 (FormArray) -->
  <div formArrayName="skus">
    <div *ngFor="let sku of skus.controls; let i = index" [formGroupName]="i">
      <mat-card>
        <mat-card-title>商品 {{ i + 1 }}</mat-card-title>
        <mat-card-content>
          <!-- SKU 編號 -->
          <mat-form-field>
            <mat-label>商品編號</mat-label>
            <input matInput formControlName="skuNo">
          </mat-form-field>

          <!-- 數量 -->
          <mat-form-field>
            <mat-label>數量</mat-label>
            <input matInput type="number" formControlName="quantity">
            <mat-error *ngIf="sku.get('quantity').hasError('min')">
              數量必須大於 0
            </mat-error>
          </mat-form-field>

          <!-- 單價 -->
          <mat-form-field>
            <mat-label>單價</mat-label>
            <input matInput type="number" formControlName="sellingAmt">
          </mat-form-field>

          <!-- 移除按鈕 -->
          <button mat-icon-button (click)="removeSku(i)" type="button">
            <mat-icon>delete</mat-icon>
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  </div>

  <!-- 新增商品按鈕 -->
  <button mat-stroked-button (click)="addSku()" type="button">
    <mat-icon>add</mat-icon>
    新增商品
  </button>

  <!-- 提交按鈕 -->
  <button mat-raised-button color="primary" type="submit" [disabled]="orderForm.invalid">
    建立訂單
  </button>
</form>
```

---

## HTTP 通訊

### Interceptors

```typescript
// core/interceptors/auth.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // 取得 JWT Token
    const token = this.authService.getToken();

    // 如果有 Token，加到 Authorization Header
    if (token) {
      request = request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(request);
  }
}

// core/interceptors/error.interceptor.ts
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  constructor(
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        let errorMessage = '發生錯誤';

        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = `錯誤: ${error.error.message}`;
        } else {
          // Server-side error
          switch (error.status) {
            case 401:
              errorMessage = '未授權，請重新登入';
              this.router.navigate(['/login']);
              break;
            case 403:
              errorMessage = '無權限執行此操作';
              break;
            case 404:
              errorMessage = '找不到資源';
              break;
            case 500:
              errorMessage = '伺服器錯誤';
              break;
            default:
              errorMessage = error.error?.message || '未知錯誤';
          }
        }

        // 顯示錯誤訊息
        this.snackBar.open(errorMessage, '關閉', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });

        return throwError(error);
      })
    );
  }
}

// app.module.ts - 註冊 Interceptors
@NgModule({
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    }
  ]
})
export class AppModule {}
```

---

## 路由與守衛

### Route Guards

```typescript
// core/guards/auth.guard.ts
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {

    if (this.authService.isAuthenticated()) {
      return true;
    }

    // 未登入，導航到登入頁
    return this.router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url }
    });
  }
}

// core/guards/role.guard.ts
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const requiredRole = route.data.role as string;
    const userRole = this.authService.getUserRole();

    if (userRole === requiredRole) {
      return true;
    }

    // 無權限
    this.snackBar.open('您無權限訪問此頁面', '關閉', { duration: 3000 });
    this.router.navigate(['/']);
    return false;
  }
}

// app-routing.module.ts - 使用 Guards
const routes: Routes = [
  {
    path: 'login',
    loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'orders',
    loadChildren: () => import('./orders/orders.module').then(m => m.OrdersModule),
    canActivate: [AuthGuard]  // 需登入
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard, RoleGuard],  // 需登入 + 管理員權限
    data: { role: 'ADMIN' }
  }
];
```

---

## 開發工具

### Angular DevTools

```bash
# Chrome Extension
Angular DevTools

功能:
├─ Component Inspector (檢查元件狀態)
├─ Change Detection (變更檢測分析)
└─ Performance Profiler (效能分析)
```

### Redux DevTools

```typescript
// app.module.ts - 啟用 Redux DevTools
import { StoreDevtoolsModule } from '@ngrx/store-devtools';

@NgModule({
  imports: [
    StoreModule.forRoot(reducers),
    StoreDevtoolsModule.instrument({
      maxAge: 25, // 保留最近 25 個狀態
      logOnly: environment.production  // 生產環境只記錄，不可時光旅行
    })
  ]
})
export class AppModule {}
```

---

## 效益總結

| 面向 | 現況 (JSP) | 目標 (Angular 8) | 改善 |
|------|-----------|-----------------|------|
| **開發效率** | 低 (混合前後端邏輯) | 高 (純前端) | +150% |
| **程式碼維護** | 困難 (252 行 validateOrder 重複) | 容易 (模組化) | +200% |
| **測試覆蓋率** | 0% (JSP 無法測試) | 80%+ (可單元測試) | +80% |
| **使用者體驗** | 差 (頁面重新載入) | 優 (SPA 流暢) | +100% |
| **型別安全** | 無 (JavaScript) | 有 (TypeScript) | ✅ |
| **Bundle 體積** | 大 (含後端) | 最佳化 (Lazy Loading) | -40% |

---

## 相關文檔

- [08-Architecture-Overview.md](./08-Architecture-Overview.md) - 架構總覽
- [28-Frontend-Project-Setup.md](./28-Frontend-Project-Setup.md) - 前端專案建置
- [29-Frontend-State-Management.md](./29-Frontend-State-Management.md) - 狀態管理詳細說明
- [30-Frontend-Order-Components.md](./30-Frontend-Order-Components.md) - 訂單元件實作
