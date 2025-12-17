# 32. Frontend - Form Validation

## Reactive Forms Validation

### 基本驗證
```typescript
// order-create.component.ts
this.orderForm = this.fb.group({
  memberCardId: ['', [
    Validators.required,
    Validators.pattern(/^[A-Z]\d{9}$/)
  ]],
  channelId: ['', Validators.required],
  items: this.fb.array([], Validators.minLength(1)),
  deliveryPhone: ['', [
    Validators.required,
    Validators.pattern(/^09\d{8}$/)
  ]]
});
```

### 自定義驗證器
```typescript
// validators/member-card.validator.ts
export function memberCardValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;

    if (!value) return null;

    // 檢查格式: A123456789
    const pattern = /^[A-Z]\d{9}$/;
    if (!pattern.test(value)) {
      return { invalidFormat: true };
    }

    // 檢查檢查碼 (假設有檢查邏輯)
    if (!validateChecksum(value)) {
      return { invalidChecksum: true };
    }

    return null;
  };
}

// 使用
this.orderForm = this.fb.group({
  memberCardId: ['', [Validators.required, memberCardValidator()]]
});
```

### 非同步驗證器
```typescript
// validators/member-exists.validator.ts
@Injectable({ providedIn: 'root' })
export class MemberExistsValidator implements AsyncValidator {
  constructor(private memberService: MemberService) {}

  validate(control: AbstractControl): Observable<ValidationErrors | null> {
    if (!control.value) {
      return of(null);
    }

    return this.memberService.checkMemberExists(control.value).pipe(
      map(exists => exists ? null : { memberNotFound: true }),
      catchError(() => of(null))
    );
  }
}

// 使用
this.orderForm = this.fb.group({
  memberCardId: ['',
    [Validators.required],
    [this.memberExistsValidator]
  ]
});
```

### 錯誤訊息顯示
```typescript
// error-message.component.ts
@Component({
  selector: 'app-error-message',
  template: `
    <mat-error *ngIf="control.hasError('required')">
      此欄位必填
    </mat-error>
    <mat-error *ngIf="control.hasError('pattern')">
      格式錯誤
    </mat-error>
    <mat-error *ngIf="control.hasError('invalidFormat')">
      會員卡號格式錯誤 (應為 A123456789)
    </mat-error>
    <mat-error *ngIf="control.hasError('memberNotFound')">
      會員不存在
    </mat-error>
  `
})
export class ErrorMessageComponent {
  @Input() control: AbstractControl;
}
```

### 使用範例
```html
<mat-form-field>
  <input matInput placeholder="會員卡號"
         formControlName="memberCardId">
  <app-error-message [control]="orderForm.get('memberCardId')">
  </app-error-message>
</mat-form-field>
```

## 表單狀態管理

```typescript
get isFormValid(): boolean {
  return this.orderForm.valid;
}

get isFormTouched(): boolean {
  return this.orderForm.touched;
}

markAllAsTouched() {
  Object.keys(this.orderForm.controls).forEach(key => {
    this.orderForm.get(key)?.markAsTouched();
  });
}
```

---

**參考文件**: `09-Frontend-Tech-Stack-Angular8.md`

**文件版本**: v1.0
