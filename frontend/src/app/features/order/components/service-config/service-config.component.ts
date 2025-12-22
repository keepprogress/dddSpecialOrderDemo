import { Component, input, output, signal, inject, computed, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  OrderLineResponse,
  InstallationService,
  WorkTypeResponse,
  UpdateOrderLineRequest,
  DeliveryMethod,
  DELIVERY_METHOD_MAP,
  StockMethod,
  STOCK_METHOD_MAP
} from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../../../shared/services/toast.service';

/**
 * Service Config Component
 *
 * 設定訂單行項的安裝與運送服務
 */
@Component({
  selector: 'app-service-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="service-config-modal" (click)="onBackdropClick($event)">
      <div class="modal-content">
        <div class="modal-header">
          <h3>服務設定 - {{ line().skuName }}</h3>
          <button class="close-btn" (click)="close.emit()">✕</button>
        </div>

        <div class="modal-body">
          <!-- 安裝服務設定 -->
          <section class="config-section">
            <h4>安裝服務</h4>

            @if (loading()) {
              <div class="loading">載入中...</div>
            } @else {
              <!-- 工種選擇 -->
              <div class="form-group">
                <label>工種</label>
                <select
                  [(ngModel)]="selectedWorkTypeId"
                  (ngModelChange)="onWorkTypeChange($event)"
                >
                  <option value="">-- 請選擇 --</option>
                  @for (wt of installationWorkTypes(); track wt.workTypeId) {
                    <option [value]="wt.workTypeId">
                      {{ wt.workTypeName }} (最低 {{ wt.minimumWage | currency:'TWD':'symbol':'1.0-0' }})
                    </option>
                  }
                </select>
              </div>

              <!-- 安裝服務選擇 -->
              @if (availableServices().length > 0) {
                <div class="form-group">
                  <label>安裝服務</label>
                  <div class="checkbox-group">
                    @for (service of availableServices(); track service.serviceType) {
                      <label class="checkbox-label">
                        <input
                          type="checkbox"
                          [value]="service.serviceType"
                          [checked]="selectedServices().includes(service.serviceType)"
                          (change)="toggleService(service.serviceType)"
                        />
                        <span>
                          {{ service.serviceName }}
                          ({{ service.basePrice | currency:'TWD':'symbol':'1.0-0' }})
                          @if (service.isMandatory) {
                            <span class="mandatory-badge">必選</span>
                          }
                        </span>
                      </label>
                    }
                  </div>
                </div>
              } @else {
                <p class="no-services">此商品無可用安裝服務</p>
              }
            }
          </section>

          <!-- 運送服務設定 -->
          <section class="config-section">
            <h4>運送方式</h4>

            <div class="form-group">
              <label>運送方式</label>
              <select
                [ngModel]="selectedDeliveryMethod()"
                (ngModelChange)="onDeliveryMethodChange($event)"
              >
                @for (method of deliveryMethods; track method.code) {
                  <option [value]="method.code">{{ method.name }}</option>
                }
              </select>
            </div>

            <!-- 收件人資訊（直送/宅配時顯示） -->
            @if (showReceiverInfo()) {
              <div class="form-group">
                <label>收件人姓名</label>
                <input type="text" [(ngModel)]="receiverName" />
              </div>

              <div class="form-group">
                <label>收件人電話</label>
                <input type="tel" [(ngModel)]="receiverPhone" />
              </div>

              <div class="form-group">
                <label>配送地址</label>
                <input type="text" [(ngModel)]="deliveryAddress" />
              </div>

              <div class="form-group">
                <label>郵遞區號</label>
                <input type="text" [(ngModel)]="deliveryZipCode" maxlength="5" />
              </div>
            }
          </section>

          <!-- 備貨方式設定 -->
          <section class="config-section">
            <h4>備貨方式</h4>

            <div class="form-group">
              <label>備貨方式</label>
              <select
                [ngModel]="selectedStockMethod()"
                (ngModelChange)="onStockMethodChange($event)"
              >
                @for (method of stockMethods; track method.code) {
                  <option [value]="method.code">{{ method.name }}</option>
                }
              </select>
              <span class="compatibility-hint">
                @if (selectedDeliveryMethod() === 'V') {
                  (直送商品建議使用訂購)
                } @else if (selectedDeliveryMethod() === 'C') {
                  (當場自取建議使用現貨)
                }
              </span>
            </div>
          </section>
        </div>

        <div class="modal-footer">
          <button class="btn-secondary" (click)="close.emit()">取消</button>
          <button
            class="btn-primary"
            (click)="onSave()"
            [disabled]="saving()"
          >
            @if (saving()) {
              儲存中...
            } @else {
              儲存
            }
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .service-config-modal {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .modal-content {
      background: white;
      border-radius: 8px;
      width: 100%;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #e0e0e0;
    }

    .modal-header h3 {
      margin: 0;
      font-size: 18px;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 20px;
      cursor: pointer;
      color: #666;
    }

    .modal-body {
      padding: 20px;
    }

    .config-section {
      margin-bottom: 24px;
    }

    .config-section h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #333;
      border-bottom: 1px solid #e0e0e0;
      padding-bottom: 8px;
    }

    .form-group {
      margin-bottom: 16px;
    }

    .form-group label {
      display: block;
      margin-bottom: 8px;
      font-weight: 500;
      color: #444;
    }

    .form-group input,
    .form-group select {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 14px;
    }

    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
    }

    .mandatory-badge {
      background: #dc3545;
      color: white;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 4px;
      margin-left: 4px;
    }

    .compatibility-hint {
      display: block;
      font-size: 12px;
      color: #6c757d;
      margin-top: 4px;
      font-style: italic;
    }

    .no-services {
      color: #666;
      font-style: italic;
    }

    .loading {
      color: #666;
      font-style: italic;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 16px 20px;
      border-top: 1px solid #e0e0e0;
    }

    .btn-primary,
    .btn-secondary {
      padding: 10px 20px;
      border-radius: 6px;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-primary {
      background: #007bff;
      color: white;
      border: none;
    }

    .btn-primary:hover:not(:disabled) {
      background: #0056b3;
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-secondary {
      background: white;
      color: #333;
      border: 1px solid #ddd;
    }

    .btn-secondary:hover {
      background: #f5f5f5;
    }
  `]
})
export class ServiceConfigComponent implements OnInit {
  private orderService = inject(OrderService);
  private toastService = inject(ToastService);

  // Inputs
  orderId = input.required<string>();
  line = input.required<OrderLineResponse>();

  // Outputs
  close = output<void>();
  saved = output<OrderLineResponse>();

  // State
  loading = signal(true);
  saving = signal(false);
  availableServices = signal<InstallationService[]>([]);
  installationWorkTypes = signal<WorkTypeResponse[]>([]);
  selectedServices = signal<string[]>([]);

  // Form fields (converted to signals for EC-008 compatibility validation)
  selectedWorkTypeId = '';
  selectedStockMethod = signal<StockMethod>('X');
  selectedDeliveryMethod = signal<DeliveryMethod>('N');
  receiverName = '';
  receiverPhone = '';
  deliveryAddress = '';
  deliveryZipCode = '';

  // Flag to prevent Toast on initial load or auto-correction
  private isAutoCorrection = false;

  /**
   * EC-008: 運送方式與備貨方式相容性驗證 (Signal Effect)
   *
   * 規則：
   * - 選擇直送(V) → 備貨方式自動預設為訂購(Y)，無提示
   * - 選擇當場自取(C) → 備貨方式自動預設為現貨(X)，無提示
   * - 直送(V) + 手動改為現貨(X) → 自動切換回訂購(Y)，Toast 提示「直送只能訂購」
   * - 當場自取(C) + 手動改為訂購(Y) → 自動切換回現貨(X)，Toast 提示「當場自取只能現貨」
   */
  private compatibilityEffect = effect(() => {
    const delivery = this.selectedDeliveryMethod();
    const stock = this.selectedStockMethod();

    // Skip validation during auto-correction to avoid loops
    if (this.isAutoCorrection) {
      this.isAutoCorrection = false;
      return;
    }

    // 直送(V) + 現貨(X) → 自動切換回訂購(Y)
    if (delivery === 'V' && stock === 'X') {
      this.isAutoCorrection = true;
      this.selectedStockMethod.set('Y');
      this.toastService.warning('直送只能訂購', 3000);
    }

    // 當場自取(C) + 訂購(Y) → 自動切換回現貨(X)
    if (delivery === 'C' && stock === 'Y') {
      this.isAutoCorrection = true;
      this.selectedStockMethod.set('X');
      this.toastService.warning('當場自取只能現貨', 3000);
    }
  });

  // Stock methods
  stockMethods = Object.entries(STOCK_METHOD_MAP).map(([code, name]) => ({
    code: code as StockMethod,
    name
  }));

  // Delivery methods
  deliveryMethods = Object.entries(DELIVERY_METHOD_MAP).map(([code, name]) => ({
    code: code as DeliveryMethod,
    name
  }));

  // Computed
  showReceiverInfo = computed(() => {
    const method = this.selectedDeliveryMethod();
    return method === 'V' || method === 'F';
  });

  async ngOnInit() {
    // 初始化表單值 (使用 isAutoCorrection 防止初始化時觸發 Toast)
    this.isAutoCorrection = true;
    const lineData = this.line();
    this.selectedWorkTypeId = lineData.workTypeId || '';
    this.selectedDeliveryMethod.set(lineData.deliveryMethod);
    this.selectedStockMethod.set(lineData.stockMethod || 'X');
    this.selectedServices.set(lineData.serviceTypes || []);
    this.receiverName = lineData.receiverName || '';
    this.receiverPhone = lineData.receiverPhone || '';
    this.deliveryAddress = lineData.deliveryAddress || '';

    // 載入資料
    await this.loadData();
  }

  private async loadData() {
    this.loading.set(true);
    try {
      const [services, workTypes] = await Promise.all([
        this.orderService.getAvailableServices(this.orderId(), this.line().lineId),
        this.orderService.getInstallationWorkTypes()
      ]);
      this.availableServices.set(services);
      this.installationWorkTypes.set(workTypes);

      // 自動選擇必選服務
      const mandatoryServices = services
        .filter(s => s.isMandatory)
        .map(s => s.serviceType);
      if (mandatoryServices.length > 0) {
        this.selectedServices.update(current => {
          const updated = [...current];
          mandatoryServices.forEach(s => {
            if (!updated.includes(s)) {
              updated.push(s);
            }
          });
          return updated;
        });
      }
    } catch (error) {
      console.error('載入服務資料失敗:', error);
    } finally {
      this.loading.set(false);
    }
  }

  onWorkTypeChange(workTypeId: string) {
    this.selectedWorkTypeId = workTypeId;
  }

  toggleService(serviceType: string) {
    this.selectedServices.update(current => {
      if (current.includes(serviceType)) {
        // 檢查是否為必選服務
        const service = this.availableServices().find(s => s.serviceType === serviceType);
        if (service?.isMandatory) {
          return current; // 不允許取消必選服務
        }
        return current.filter(s => s !== serviceType);
      } else {
        return [...current, serviceType];
      }
    });
  }

  /**
   * 運送方式變更時，自動調整備貨方式 (EC-008 預設行為)
   * - 選擇直送(V) → 備貨方式自動預設為訂購(Y)，無提示
   * - 選擇當場自取(C) → 備貨方式自動預設為現貨(X)，無提示
   */
  onDeliveryMethodChange(deliveryMethod: DeliveryMethod): void {
    // 預設行為：自動設定相容的備貨方式（無 Toast）
    this.isAutoCorrection = true;
    this.selectedDeliveryMethod.set(deliveryMethod);

    if (deliveryMethod === 'V') {
      // 直送 → 預設訂購
      this.selectedStockMethod.set('Y');
    } else if (deliveryMethod === 'C') {
      // 當場自取 → 預設現貨
      this.selectedStockMethod.set('X');
    }
  }

  /**
   * 備貨方式變更 (EC-008 手動覆寫)
   * - 直送(V) + 手動改為現貨(X) → effect 會自動切換回訂購(Y) + Toast
   * - 當場自取(C) + 手動改為訂購(Y) → effect 會自動切換回現貨(X) + Toast
   */
  onStockMethodChange(stockMethod: StockMethod): void {
    // 這是使用者手動變更，不設定 isAutoCorrection
    // effect 會檢查相容性並在需要時顯示 Toast
    this.selectedStockMethod.set(stockMethod);
  }

  async onSave() {
    this.saving.set(true);
    try {
      // 儲存安裝服務
      if (this.selectedWorkTypeId || this.selectedServices().length > 0) {
        await this.orderService.attachInstallation(
          this.orderId(),
          this.line().lineId,
          {
            workTypeId: this.selectedWorkTypeId || undefined,
            serviceTypes: this.selectedServices()
          }
        );
      }

      // 儲存備貨與運送服務 (使用 signal 值)
      const deliveryRequest: UpdateOrderLineRequest = {
        stockMethod: this.selectedStockMethod(),
        deliveryMethod: this.selectedDeliveryMethod()
      };

      if (this.showReceiverInfo()) {
        deliveryRequest.receiverName = this.receiverName;
        deliveryRequest.receiverPhone = this.receiverPhone;
        deliveryRequest.deliveryAddress = this.deliveryAddress;
        deliveryRequest.deliveryZipCode = this.deliveryZipCode;
      }

      const result = await this.orderService.attachDelivery(
        this.orderId(),
        this.line().lineId,
        deliveryRequest
      );

      this.saved.emit(result);
      this.close.emit();
    } catch (error) {
      console.error('儲存服務設定失敗:', error);
      this.toastService.error('儲存失敗，請稍後再試');
    } finally {
      this.saving.set(false);
    }
  }

  onBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('service-config-modal')) {
      this.close.emit();
    }
  }
}
