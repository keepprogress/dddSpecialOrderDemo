import { Component, inject, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../services/product.service';
import { OrderLineResponse, EligibilityResponse } from '../../models/order.model';
import { ServiceConfigComponent } from '../service-config/service-config.component';

/**
 * 商品列表元件
 *
 * 功能：
 * - 商品搜尋與資格驗證
 * - 商品列表顯示
 * - 數量修改與刪除
 */
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ServiceConfigComponent],
  template: `
    <div class="product-list">
      <!-- 商品搜尋 -->
      <div class="search-form">
        <div class="input-row">
          <div class="input-group">
            <label for="skuNo">商品編號</label>
            <input
              id="skuNo"
              type="text"
              [(ngModel)]="skuNoInput"
              placeholder="請輸入商品編號 (例如: 014014014)"
              [disabled]="isChecking()"
            />
          </div>
          <div class="input-group quantity">
            <label for="qty">數量</label>
            <input
              id="qty"
              type="number"
              [(ngModel)]="quantityInput"
              min="1"
              [disabled]="isChecking()"
            />
          </div>
          <button
            class="btn btn-primary"
            (click)="checkAndAdd()"
            [disabled]="!skuNoInput || isChecking()"
          >
            @if (isChecking()) {
              檢查中...
            } @else {
              新增商品
            }
          </button>
        </div>

        @if (eligibilityError()) {
          <div class="error-message">
            {{ eligibilityError() }}
          </div>
        }

        @if (eligibilityResult()?.eligible) {
          <div class="success-message">
            商品 {{ eligibilityResult()?.product?.skuName }} 可以銷售
            @if (eligibilityResult()?.isLargeFurniture) {
              <span class="product-badge large-furniture">大型家具</span>
            }
            @if (eligibilityResult()?.isServiceSku) {
              <span class="product-badge service-sku">外包服務</span>
            }
          </div>
          @if (eligibilityResult()?.orderability?.isDcVendorFrozen) {
            <div class="warning-message">
              ⚠️ 此商品廠商已凍結，庫存量: {{ eligibilityResult()?.orderability?.stockAoh }}
              @if (requiresSpotStock()) {
                <strong>（僅可選現貨）</strong>
              }
            </div>
          }
        }
      </div>

      <!-- 商品列表 -->
      @if (lines().length > 0) {
        <div class="lines-table">
          <table>
            <thead>
              <tr>
                <th class="col-no">#</th>
                <th class="col-sku">商品編號</th>
                <th class="col-name">商品名稱</th>
                <th class="col-price">單價</th>
                <th class="col-qty">數量</th>
                <th class="col-stock">備貨</th>
                <th class="col-subtotal">小計</th>
                <th class="col-service">服務</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              @for (line of lines(); track line.lineId) {
                <tr>
                  <td class="col-no">{{ line.serialNo }}</td>
                  <td class="col-sku">{{ line.skuNo }}</td>
                  <td class="col-name">{{ line.skuName }}</td>
                  <td class="col-price">{{ line.actualUnitPrice | number }}</td>
                  <td class="col-qty">
                    <input
                      type="number"
                      [value]="line.quantity"
                      min="1"
                      (change)="onQuantityChange(line.lineId, $event)"
                    />
                  </td>
                  <td class="col-stock">
                    <span class="stock-badge" [class]="'stock-' + line.stockMethod">
                      {{ line.stockMethodName }}
                    </span>
                  </td>
                  <td class="col-subtotal">{{ line.subtotal | number }}</td>
                  <td class="col-service">
                    @if (line.hasInstallation) {
                      <span class="service-badge install">安裝</span>
                    }
                    @if (line.installationCost > 0) {
                      <span class="service-cost">+{{ line.installationCost | number }}</span>
                    }
                    <button
                      class="btn-icon btn-config"
                      (click)="openServiceConfig(line)"
                      title="設定服務"
                    >
                      設定
                    </button>
                  </td>
                  <td class="col-action">
                    <button
                      class="btn-icon btn-delete"
                      (click)="removeLine(line.lineId)"
                      title="刪除"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr>
                <td colspan="7" class="text-right">商品小計</td>
                <td class="col-subtotal">{{ calculateTotal() | number }}</td>
                <td></td>
              </tr>
            </tfoot>
          </table>
        </div>
      } @else {
        <div class="empty-state">
          <p>尚未新增任何商品</p>
        </div>
      }

      <!-- Service Config Modal -->
      @if (showServiceConfig() && selectedLine()) {
        <app-service-config
          [orderId]="orderId()"
          [line]="selectedLine()!"
          (close)="closeServiceConfig()"
          (saved)="onServiceConfigSaved($event)"
        />
      }
    </div>
  `,
  styles: [`
    .product-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .search-form {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .input-row {
      display: flex;
      gap: 0.5rem;
      align-items: flex-end;
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      flex: 1;

      &.quantity {
        flex: 0 0 80px;
      }

      label {
        font-size: 0.875rem;
        color: #666;
      }

      input {
        padding: 0.5rem 0.75rem;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        font-size: 1rem;

        &:focus {
          outline: none;
          border-color: #1565c0;
        }

        &:disabled {
          background: #f5f5f5;
        }
      }
    }

    .btn {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 4px;
      font-size: 0.875rem;
      cursor: pointer;
      white-space: nowrap;

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      &.btn-primary {
        background: #1565c0;
        color: #fff;
      }
    }

    .error-message {
      padding: 0.5rem;
      background: #ffebee;
      color: #c62828;
      border-radius: 4px;
      font-size: 0.875rem;
    }

    .success-message {
      padding: 0.5rem;
      background: #e8f5e9;
      color: #2e7d32;
      border-radius: 4px;
      font-size: 0.875rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .warning-message {
      padding: 0.5rem;
      background: #fff3e0;
      color: #e65100;
      border-radius: 4px;
      font-size: 0.875rem;
    }

    .product-badge {
      display: inline-block;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 500;

      &.large-furniture {
        background: #e8eaf6;
        color: #3949ab;
      }

      &.service-sku {
        background: #fce4ec;
        color: #c2185b;
      }
    }

    .lines-table {
      overflow-x: auto;

      table {
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

      .col-no { width: 40px; text-align: center; }
      .col-sku { width: 100px; }
      .col-name { }
      .col-price { width: 80px; text-align: right; }
      .col-qty {
        width: 80px;

        input {
          width: 60px;
          padding: 0.25rem;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          text-align: center;
        }
      }
      .col-stock {
        width: 60px;
        text-align: center;

        .stock-badge {
          display: inline-block;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 0.75rem;

          &.stock-X {
            background: #e8f5e9;
            color: #2e7d32;
          }

          &.stock-Y {
            background: #fff3e0;
            color: #e65100;
          }
        }
      }
      .col-subtotal { width: 100px; text-align: right; font-weight: 500; }
      .col-service {
        width: 120px;
        text-align: center;

        .service-badge {
          display: inline-block;
          padding: 2px 6px;
          border-radius: 4px;
          font-size: 0.75rem;
          margin-right: 4px;

          &.install {
            background: #e3f2fd;
            color: #1565c0;
          }
        }

        .service-cost {
          font-size: 0.75rem;
          color: #666;
          display: block;
        }
      }
      .col-action { width: 60px; text-align: center; }

      tfoot td {
        font-weight: 600;
        background: #fafafa;

        &.text-right {
          text-align: right;
        }
      }
    }

    .btn-icon {
      width: 24px;
      height: 24px;
      padding: 0;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.875rem;

      &.btn-delete {
        background: #ffebee;
        color: #c62828;

        &:hover {
          background: #ffcdd2;
        }
      }

      &.btn-config {
        width: auto;
        padding: 2px 8px;
        background: #e3f2fd;
        color: #1565c0;

        &:hover {
          background: #bbdefb;
        }
      }
    }

    .empty-state {
      text-align: center;
      padding: 2rem;
      color: #666;
      background: #fafafa;
      border-radius: 4px;
    }
  `]
})
export class ProductListComponent {
  private productService = inject(ProductService);

  // Inputs
  orderId = input<string>('');
  lines = input<OrderLineResponse[]>([]);
  channelId = input<string>('');
  storeId = input<string>('');
  isLoading = input(false);

  // Outputs
  productAdded = output<{ skuNo: string; quantity: number }>();
  productRemoved = output<string>();
  quantityChanged = output<{ lineId: string; quantity: number }>();
  serviceConfigUpdated = output<OrderLineResponse>();

  // Local state
  skuNoInput = '';
  quantityInput = 1;
  isChecking = signal(false);
  eligibilityError = signal<string | null>(null);
  eligibilityResult = signal<EligibilityResponse | null>(null);
  showServiceConfig = signal(false);
  selectedLine = signal<OrderLineResponse | null>(null);

  /**
   * 檢查資格並新增商品
   */
  async checkAndAdd(): Promise<void> {
    if (!this.skuNoInput) return;

    this.isChecking.set(true);
    this.eligibilityError.set(null);
    this.eligibilityResult.set(null);

    try {
      // 驗證商品資格
      const result = await this.productService.checkEligibility(
        this.skuNoInput,
        this.channelId(),
        this.storeId()
      );

      this.eligibilityResult.set(result);

      if (result.eligible) {
        // 資格通過，發送新增事件
        this.productAdded.emit({
          skuNo: this.skuNoInput,
          quantity: this.quantityInput
        });

        // 清空輸入
        this.skuNoInput = '';
        this.quantityInput = 1;
        this.eligibilityResult.set(null);
      } else {
        // 資格不符
        const errorMsg = this.productService.getEligibilityErrorMessage(result.failureLevel);
        this.eligibilityError.set(errorMsg);
      }
    } catch (err: any) {
      this.eligibilityError.set(err.message ?? '檢查商品資格失敗');
    } finally {
      this.isChecking.set(false);
    }
  }

  /**
   * 刪除商品
   */
  removeLine(lineId: string): void {
    this.productRemoved.emit(lineId);
  }

  /**
   * 數量變更
   */
  onQuantityChange(lineId: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const quantity = parseInt(input.value, 10);

    if (quantity > 0) {
      this.quantityChanged.emit({ lineId, quantity });
    }
  }

  /**
   * 計算總金額
   */
  calculateTotal(): number {
    return this.lines().reduce((sum, line) => sum + line.subtotal, 0);
  }

  /**
   * 判斷是否需強制現貨
   * DC商品廠商凍結 + 非大型家具 + 庫存不足 → 強制現貨
   */
  requiresSpotStock(): boolean {
    const result = this.eligibilityResult();
    if (!result) return false;
    return this.productService.requiresSpotStock(result, this.quantityInput);
  }

  /**
   * 開啟服務設定
   */
  openServiceConfig(line: OrderLineResponse): void {
    this.selectedLine.set(line);
    this.showServiceConfig.set(true);
  }

  /**
   * 關閉服務設定
   */
  closeServiceConfig(): void {
    this.showServiceConfig.set(false);
    this.selectedLine.set(null);
  }

  /**
   * 服務設定儲存完成
   */
  onServiceConfigSaved(updatedLine: OrderLineResponse): void {
    this.serviceConfigUpdated.emit(updatedLine);
    this.closeServiceConfig();
  }
}
