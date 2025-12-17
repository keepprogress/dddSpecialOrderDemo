# 29. Frontend - State Management (NgRx)

## NgRx Store 架構

```typescript
// app.state.ts
export interface AppState {
  order: OrderState;
  pricing: PricingState;
  auth: AuthState;
}
```

## Order State 實作

### Actions
```typescript
// order.actions.ts
import { createAction, props } from '@ngrx/store';

// Load Orders
export const loadOrders = createAction(
  '[Order List] Load Orders',
  props<{ page: number; size: number }>()
);

export const loadOrdersSuccess = createAction(
  '[Order API] Load Orders Success',
  props<{ orders: Order[] }>()
);

export const loadOrdersFailure = createAction(
  '[Order API] Load Orders Failure',
  props<{ error: any }>()
);

// Create Order
export const createOrder = createAction(
  '[Order Create] Create Order',
  props<{ request: OrderRequest }>()
);

export const createOrderSuccess = createAction(
  '[Order API] Create Order Success',
  props<{ order: Order }>()
);
```

### Reducer
```typescript
// order.reducer.ts
export interface OrderState {
  orders: Order[];
  selectedOrder: Order | null;
  loading: boolean;
  error: any;
}

const initialState: OrderState = {
  orders: [],
  selectedOrder: null,
  loading: false,
  error: null
};

export const orderReducer = createReducer(
  initialState,
  on(loadOrders, (state) => ({
    ...state,
    loading: true
  })),
  on(loadOrdersSuccess, (state, { orders }) => ({
    ...state,
    orders,
    loading: false
  })),
  on(loadOrdersFailure, (state, { error }) => ({
    ...state,
    error,
    loading: false
  }))
);
```

### Effects
```typescript
// order.effects.ts
@Injectable()
export class OrderEffects {
  loadOrders$ = createEffect(() =>
    this.actions$.pipe(
      ofType(loadOrders),
      switchMap(({ page, size }) =>
        this.orderService.getOrders(page, size).pipe(
          map(response => loadOrdersSuccess({ orders: response.data })),
          catchError(error => of(loadOrdersFailure({ error })))
        )
      )
    )
  );

  createOrder$ = createEffect(() =>
    this.actions$.pipe(
      ofType(createOrder),
      switchMap(({ request }) =>
        this.orderService.createOrder(request).pipe(
          map(response => createOrderSuccess({ order: response.data })),
          catchError(error => of(createOrderFailure({ error })))
        )
      )
    )
  );

  constructor(
    private actions$: Actions,
    private orderService: OrderService
  ) {}
}
```

### Selectors
```typescript
// order.selectors.ts
export const selectOrderState = (state: AppState) => state.order;

export const selectAllOrders = createSelector(
  selectOrderState,
  (state) => state.orders
);

export const selectOrderLoading = createSelector(
  selectOrderState,
  (state) => state.loading
);

export const selectSelectedOrder = createSelector(
  selectOrderState,
  (state) => state.selectedOrder
);
```

## 使用範例

```typescript
// order-list.component.ts
@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html'
})
export class OrderListComponent implements OnInit {
  orders$ = this.store.select(selectAllOrders);
  loading$ = this.store.select(selectOrderLoading);

  constructor(private store: Store<AppState>) {}

  ngOnInit() {
    this.store.dispatch(loadOrders({ page: 0, size: 20 }));
  }
}
```

---

**參考文件**: `09-Frontend-Tech-Stack-Angular8.md`

**文件版本**: v1.0
