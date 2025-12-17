# 30. Frontend - Order Components

## Order List Component

```typescript
// order-list.component.ts
@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.scss']
})
export class OrderListComponent implements OnInit {
  orders$ = this.store.select(selectAllOrders);
  loading$ = this.store.select(selectOrderLoading);

  displayedColumns = ['orderId', 'memberCardId', 'orderDate', 'status', 'finalTotal', 'actions'];

  constructor(
    private store: Store<AppState>,
    private router: Router
  ) {}

  ngOnInit() {
    this.loadOrders();
  }

  loadOrders() {
    this.store.dispatch(loadOrders({ page: 0, size: 20 }));
  }

  viewOrder(orderId: string) {
    this.router.navigate(['/orders', orderId]);
  }
}
```

**Template**:
```html
<!-- order-list.component.html -->
<mat-card>
  <mat-card-title>訂單清單</mat-card-title>

  <mat-table [dataSource]="orders$ | async" *ngIf="!(loading$ | async)">
    <ng-container matColumnDef="orderId">
      <mat-header-cell *matHeaderCellDef>訂單編號</mat-header-cell>
      <mat-cell *matCellDef="let order">{{ order.orderId }}</mat-cell>
    </ng-container>

    <ng-container matColumnDef="status">
      <mat-header-cell *matHeaderCellDef>狀態</mat-header-cell>
      <mat-cell *matCellDef="let order">
        <mat-chip>{{ order.statusName }}</mat-chip>
      </mat-cell>
    </ng-container>

    <ng-container matColumnDef="finalTotal">
      <mat-header-cell *matHeaderCellDef>金額</mat-header-cell>
      <mat-cell *matCellDef="let order">
        {{ order.finalTotal | currency:'TWD' }}
      </mat-cell>
    </ng-container>

    <ng-container matColumnDef="actions">
      <mat-header-cell *matHeaderCellDef>操作</mat-header-cell>
      <mat-cell *matCellDef="let order">
        <button mat-button (click)="viewOrder(order.orderId)">查看</button>
      </mat-cell>
    </ng-container>

    <mat-header-row *matHeaderRowDef="displayedColumns"></mat-header-row>
    <mat-row *matRowDef="let row; columns: displayedColumns"></mat-row>
  </mat-table>

  <mat-spinner *ngIf="loading$ | async"></mat-spinner>
</mat-card>
```

## Order Create Component

```typescript
// order-create.component.ts
@Component({
  selector: 'app-order-create',
  templateUrl: './order-create.component.html'
})
export class OrderCreateComponent implements OnInit {
  orderForm: FormGroup;
  pricingResult: PricingResponse | null = null;

  constructor(
    private fb: FormBuilder,
    private store: Store<AppState>
  ) {
    this.orderForm = this.fb.group({
      memberCardId: ['', [Validators.required, Validators.pattern(/^[A-Z]\d{9}$/)]],
      items: this.fb.array([], Validators.minLength(1))
    });
  }

  get items(): FormArray {
    return this.orderForm.get('items') as FormArray;
  }

  addItem() {
    this.items.push(this.fb.group({
      skuNo: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(1)]]
    }));
  }

  submitOrder() {
    if (this.orderForm.valid) {
      this.store.dispatch(createOrder({
        request: this.orderForm.value
      }));
    }
  }
}
```

---

**參考文件**: `02-Order-Creation-Flow.md`

**文件版本**: v1.0
