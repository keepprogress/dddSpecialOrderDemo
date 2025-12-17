# 31. Frontend - Pricing Components

## Pricing Calculator Component

```typescript
// pricing-calculator.component.ts
@Component({
  selector: 'app-pricing-calculator',
  templateUrl: './pricing-calculator.component.html'
})
export class PricingCalculatorComponent {
  pricingForm: FormGroup;
  result: PricingResponse | null = null;
  isCalculating = false;

  constructor(
    private fb: FormBuilder,
    private pricingService: PricingService
  ) {
    this.pricingForm = this.fb.group({
      memberCardId: ['', Validators.required],
      skus: this.fb.array([])
    });
  }

  calculatePrice() {
    if (this.pricingForm.invalid) return;

    this.isCalculating = true;
    this.pricingService.calculatePrice(this.pricingForm.value)
      .pipe(finalize(() => this.isCalculating = false))
      .subscribe(
        response => this.result = response,
        error => console.error('Pricing failed', error)
      );
  }
}
```

**Template**:
```html
<mat-card>
  <mat-card-title>價格計算</mat-card-title>

  <form [formGroup]="pricingForm">
    <mat-form-field>
      <input matInput placeholder="會員卡號" formControlName="memberCardId">
    </mat-form-field>

    <!-- SKU 清單 -->
    <div formArrayName="skus">
      <!-- SKU items -->
    </div>

    <button mat-raised-button color="primary"
            (click)="calculatePrice()"
            [disabled]="isCalculating || pricingForm.invalid">
      計算價格
    </button>
  </form>

  <!-- 計價結果 -->
  <div *ngIf="result">
    <h3>計價結果</h3>
    <p>原始金額: {{ result.summary.originalTotal | currency }}</p>
    <p>折扣金額: {{ result.summary.discountTotal | currency }}</p>
    <p>最終金額: {{ result.summary.finalTotal | currency }}</p>
    <p>計算時間: {{ result.calculationTime }}ms</p>
  </div>
</mat-card>
```

## Pricing Result Component

```typescript
// pricing-result.component.ts
@Component({
  selector: 'app-pricing-result',
  template: `
    <mat-card>
      <mat-card-title>計價明細</mat-card-title>

      <mat-list>
        <mat-list-item *ngFor="let compute of result.computes">
          <h4 matLine>{{ compute.name }}</h4>
          <p matLine>{{ compute.amount | currency }}</p>
        </mat-list-item>
      </mat-list>

      <mat-divider></mat-divider>

      <mat-list-item>
        <h3 matLine>總計</h3>
        <h3 matLine>{{ result.summary.finalTotal | currency }}</h3>
      </mat-list-item>
    </mat-card>
  `
})
export class PricingResultComponent {
  @Input() result: PricingResponse;
}
```

---

**參考文件**: `04-Pricing-Calculation-Sequence.md`

**文件版本**: v1.0
