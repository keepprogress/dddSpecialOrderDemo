import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  CalculationResponse,
  ComputeTypeVO,
  MemberDiscVO,
  MEMBER_DISCOUNT_TYPE_MAP
} from '../../models/order.model';

/**
 * 價格試算結果元件
 *
 * 顯示 6 種 ComputeType 和會員折扣明細：
 * 1. 商品小計
 * 2. 安裝小計
 * 3. 運送小計
 * 4. 會員卡折扣
 * 5. 直送費用小計
 * 6. 折價券折扣
 */
@Component({
  selector: 'app-price-calculation',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="price-calculation">
      <!-- ComputeTypes 表格 -->
      <div class="compute-types-section">
        <h4>價格明細</h4>
        <table class="compute-types-table">
          <thead>
            <tr>
              <th class="col-type">類型</th>
              <th class="col-price">原價</th>
              <th class="col-discount">折扣</th>
              <th class="col-actual">實際價格</th>
            </tr>
          </thead>
          <tbody>
            @for (item of computeTypes(); track item.computeType) {
              <tr [class.discount-row]="isDiscountType(item)">
                <td class="col-type">{{ item.computeName }}</td>
                <td class="col-price">{{ item.totalPrice | number }}</td>
                <td class="col-discount" [class.negative]="item.discount < 0">
                  @if (item.discount !== 0) {
                    {{ item.discount | number }}
                  } @else {
                    -
                  }
                </td>
                <td class="col-actual">{{ item.actTotalPrice | number }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="4" class="empty-row">尚未進行試算</td>
              </tr>
            }
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td colspan="3" class="label">應付總額</td>
              <td class="col-actual grand-total">{{ grandTotal() | number }} 元</td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- 會員折扣明細 -->
      @if (hasMemberDiscounts()) {
        <div class="member-discounts-section">
          <h4>會員折扣明細</h4>
          <table class="member-discounts-table">
            <thead>
              <tr>
                <th class="col-sku">商品編號</th>
                <th class="col-disc-type">折扣類型</th>
                <th class="col-original">原價</th>
                <th class="col-disc-price">折扣價</th>
                <th class="col-disc-amt">折扣金額</th>
              </tr>
            </thead>
            <tbody>
              @for (disc of memberDiscounts(); track disc.skuNo) {
                <tr>
                  <td class="col-sku">{{ disc.skuNo }}</td>
                  <td class="col-disc-type">
                    <span class="disc-type-badge" [class]="'type-' + disc.discType">
                      {{ disc.discTypeName }}
                    </span>
                  </td>
                  <td class="col-original">{{ disc.originalPrice | number }}</td>
                  <td class="col-disc-price">{{ disc.discountPrice | number }}</td>
                  <td class="col-disc-amt" [class.negative]="disc.discAmt < 0">
                    {{ disc.discAmt | number }}
                  </td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr>
                <td colspan="4" class="label">會員折扣小計</td>
                <td class="col-disc-amt total-discount">
                  {{ totalMemberDiscount() | number }} 元
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      }

      <!-- 警告訊息 -->
      @if (hasWarnings()) {
        <div class="warnings-section">
          <h4>注意事項</h4>
          <ul class="warnings-list">
            @for (warning of warnings(); track warning) {
              <li class="warning-item">{{ warning }}</li>
            }
          </ul>
        </div>
      }

      <!-- 促銷跳過提示 -->
      @if (promotionSkipped()) {
        <div class="promotion-skipped-notice">
          促銷引擎連線逾時，本次未套用促銷折扣
        </div>
      }
    </div>
  `,
  styles: [`
    .price-calculation {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    h4 {
      margin: 0 0 0.75rem 0;
      font-size: 1rem;
      font-weight: 600;
      color: #333;
      border-bottom: 1px solid #e0e0e0;
      padding-bottom: 0.5rem;
    }

    /* ComputeTypes 表格 */
    .compute-types-table,
    .member-discounts-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 0.875rem;
    }

    th, td {
      padding: 0.75rem 0.5rem;
      text-align: left;
      border-bottom: 1px solid #e0e0e0;
    }

    th {
      background: #f5f5f5;
      font-weight: 500;
      color: #666;
    }

    .col-type { width: 35%; }
    .col-price,
    .col-discount,
    .col-actual { width: 20%; text-align: right; }

    .col-sku { width: 20%; }
    .col-disc-type { width: 25%; }
    .col-original,
    .col-disc-price,
    .col-disc-amt { width: 18%; text-align: right; }

    .discount-row {
      background: #fff8e1;
    }

    .negative {
      color: #c62828;
    }

    .empty-row {
      text-align: center;
      color: #666;
      font-style: italic;
    }

    tfoot td {
      font-weight: 600;
      background: #fafafa;
    }

    .label {
      text-align: right;
    }

    .total-row {
      background: #e3f2fd;
    }

    .grand-total {
      font-size: 1rem;
      color: #1565c0;
    }

    .total-discount {
      color: #c62828;
    }

    /* 折扣類型標籤 */
    .disc-type-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;

      &.type-0 {
        background: #e3f2fd;
        color: #1565c0;
      }

      &.type-1 {
        background: #e8f5e9;
        color: #2e7d32;
      }

      &.type-2 {
        background: #fff3e0;
        color: #e65100;
      }

      &.type-SPECIAL {
        background: #fce4ec;
        color: #c2185b;
      }
    }

    /* 警告訊息 */
    .warnings-section {
      background: #fff8e1;
      border: 1px solid #ffe082;
      border-radius: 4px;
      padding: 1rem;
    }

    .warnings-section h4 {
      color: #f57c00;
      border-bottom-color: #ffe082;
    }

    .warnings-list {
      margin: 0;
      padding-left: 1.5rem;
    }

    .warning-item {
      color: #e65100;
      margin-bottom: 0.25rem;
    }

    /* 促銷跳過提示 */
    .promotion-skipped-notice {
      padding: 0.75rem 1rem;
      background: #ffebee;
      border: 1px solid #ef9a9a;
      border-radius: 4px;
      color: #c62828;
      font-size: 0.875rem;
    }
  `]
})
export class PriceCalculationComponent {
  // Inputs
  calculation = input<CalculationResponse | null>(null);

  // Computed
  computeTypes = computed(() => this.calculation()?.computeTypes ?? []);
  memberDiscounts = computed(() => this.calculation()?.memberDiscounts ?? []);
  warnings = computed(() => this.calculation()?.warnings ?? []);
  grandTotal = computed(() => this.calculation()?.grandTotal ?? 0);
  promotionSkipped = computed(() => this.calculation()?.promotionSkipped ?? false);

  hasMemberDiscounts = computed(() => this.memberDiscounts().length > 0);
  hasWarnings = computed(() => this.warnings().length > 0);

  totalMemberDiscount = computed(() => {
    return this.memberDiscounts().reduce((sum, d) => sum + d.discAmt, 0);
  });

  /**
   * 判斷是否為折扣類型的 ComputeType
   */
  isDiscountType(item: ComputeTypeVO): boolean {
    // ComputeType 4 (會員卡折扣) 和 6 (折價券折扣) 為折扣類型
    return item.computeType === '4' || item.computeType === '6';
  }
}
