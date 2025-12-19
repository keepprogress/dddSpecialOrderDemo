import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OrderService } from '../services/order.service';
import { MemberService } from '../services/member.service';
import { ProductService } from '../services/product.service';
import { MemberInfoComponent } from '../components/member-info/member-info.component';
import { ProductListComponent } from '../components/product-list/product-list.component';
import { PriceCalculationComponent } from '../components/price-calculation/price-calculation.component';
import { OrderSummaryComponent } from '../components/order-summary/order-summary.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import {
  OrderResponse,
  OrderLineResponse,
  CreateOrderRequest,
  MemberResponse,
  EligibilityResponse,
  CalculationResponse,
  CouponValidation,
  BonusRedemption
} from '../models/order.model';

/**
 * 建立訂單頁面
 *
 * 主要功能：
 * - 會員查詢與臨時卡建立
 * - 商品新增與資格驗證
 * - 價格試算
 * - 訂單提交
 */
@Component({
  selector: 'app-create-order',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MemberInfoComponent,
    ProductListComponent,
    PriceCalculationComponent,
    OrderSummaryComponent,
    SkeletonLoaderComponent
  ],
  templateUrl: './create-order.component.html',
  styleUrl: './create-order.component.scss'
})
export class CreateOrderComponent implements OnInit {
  private orderService = inject(OrderService);
  private memberService = inject(MemberService);
  private productService = inject(ProductService);
  private router = inject(Router);

  // === State Signals ===

  // 訂單狀態
  currentOrder = signal<OrderResponse | null>(null);
  orderLines = signal<OrderLineResponse[]>([]);

  // 會員狀態
  member = signal<MemberResponse | null>(null);
  isTempCard = signal(false);

  // 試算結果
  calculationResult = signal<CalculationResponse | null>(null);

  // 載入狀態
  isLoading = signal(false);
  isMemberLoading = signal(false);
  isProductLoading = signal(false);
  isCalculating = signal(false);
  isSubmitting = signal(false);

  // 錯誤訊息
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // 表單資料
  storeId = signal('S001'); // 預設店別
  channelId = signal('SO'); // 預設通路

  // === Computed Signals ===

  hasOrder = computed(() => this.currentOrder() !== null);
  hasLines = computed(() => this.orderLines().length > 0);
  canSubmit = computed(() => {
    const order = this.currentOrder();
    return order !== null &&
           this.orderLines().length > 0 &&
           !this.isSubmitting() &&
           order.status === '1'; // 草稿狀態才能提交
  });

  grandTotal = computed(() => {
    const order = this.currentOrder();
    return order?.calculation?.grandTotal ?? 0;
  });

  ngOnInit(): void {
    // 頁面初始化
  }

  // === 會員相關 ===

  /**
   * 處理會員查詢結果
   */
  onMemberFound(member: MemberResponse): void {
    this.member.set(member);
    this.isTempCard.set(member.isTempCard);
    this.errorMessage.set(null);

    // 如果已有訂單，則不重新建立
    if (!this.hasOrder()) {
      this.createOrder();
    }
  }

  /**
   * 處理臨時卡建立
   */
  onTempCardCreated(member: MemberResponse): void {
    this.member.set(member);
    this.isTempCard.set(true);
    this.errorMessage.set(null);

    if (!this.hasOrder()) {
      this.createOrder();
    }
  }

  /**
   * 處理會員查詢錯誤
   */
  onMemberError(error: string): void {
    this.errorMessage.set(error);
    this.member.set(null);
  }

  // === 訂單相關 ===

  /**
   * 建立訂單
   */
  async createOrder(): Promise<void> {
    const memberData = this.member();
    if (!memberData) {
      this.errorMessage.set('請先輸入會員資料');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    try {
      const request: CreateOrderRequest = {
        memberId: memberData.memberId,
        customer: {
          memberId: memberData.memberId,
          cardType: memberData.cardType,
          name: memberData.name,
          cellPhone: memberData.cellPhone,
          contactName: memberData.name,
          contactPhone: memberData.cellPhone,
          isTempCard: memberData.isTempCard
        },
        address: {
          zipCode: memberData.zipCode ?? '100',
          fullAddress: memberData.address ?? '台北市'
        },
        storeId: this.storeId(),
        channelId: this.channelId()
      };

      const idempotencyKey = this.orderService.generateIdempotencyKey();
      const order = await this.orderService.createOrder(request, idempotencyKey);

      this.currentOrder.set(order);
      this.orderLines.set(order.lines);
      this.successMessage.set(`訂單建立成功: ${order.orderId}`);
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '建立訂單失敗');
    } finally {
      this.isLoading.set(false);
    }
  }

  // === 商品相關 ===

  /**
   * 處理商品新增
   */
  async onProductAdded(product: { skuNo: string; quantity: number }): Promise<void> {
    const order = this.currentOrder();
    if (!order) {
      this.errorMessage.set('請先建立訂單');
      return;
    }

    this.isProductLoading.set(true);
    this.errorMessage.set(null);

    try {
      const line = await this.orderService.addLine(order.orderId, {
        skuNo: product.skuNo,
        quantity: product.quantity,
        deliveryMethod: 'N',
        stockMethod: 'X'
      });

      this.orderLines.update(lines => [...lines, line]);
      this.successMessage.set(`商品 ${line.skuName} 已加入訂單`);
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '新增商品失敗');
    } finally {
      this.isProductLoading.set(false);
    }
  }

  /**
   * 處理商品刪除
   */
  async onProductRemoved(lineId: string): Promise<void> {
    const order = this.currentOrder();
    if (!order) return;

    try {
      await this.orderService.removeLine(order.orderId, lineId);
      this.orderLines.update(lines => lines.filter(l => l.lineId !== lineId));
      this.successMessage.set('商品已移除');
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '移除商品失敗');
    }
  }

  /**
   * 處理商品數量更新
   */
  async onQuantityChanged(event: { lineId: string; quantity: number }): Promise<void> {
    const order = this.currentOrder();
    if (!order) return;

    try {
      const updatedLine = await this.orderService.updateLine(
        order.orderId,
        event.lineId,
        { skuNo: '', quantity: event.quantity, deliveryMethod: 'N', stockMethod: 'X' }
      );

      this.orderLines.update(lines =>
        lines.map(l => l.lineId === event.lineId ? updatedLine : l)
      );
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '更新數量失敗');
    }
  }

  /**
   * 處理服務設定更新
   */
  onServiceConfigUpdated(updatedLine: OrderLineResponse): void {
    this.orderLines.update(lines =>
      lines.map(l => l.lineId === updatedLine.lineId ? updatedLine : l)
    );
    this.successMessage.set('服務設定已更新');
  }

  // === 試算與提交 ===

  /**
   * 執行價格試算
   */
  async calculate(): Promise<void> {
    const order = this.currentOrder();
    if (!order) return;

    this.isCalculating.set(true);
    this.errorMessage.set(null);

    try {
      const result = await this.orderService.calculate(order.orderId);
      // 儲存試算結果
      this.calculationResult.set(result);
      // 重新取得訂單以更新試算結果
      const updatedOrder = await this.orderService.getOrder(order.orderId);
      this.currentOrder.set(updatedOrder);
      this.successMessage.set(`試算完成，應付總額: ${result.grandTotal} 元`);
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '價格試算失敗');
    } finally {
      this.isCalculating.set(false);
    }
  }

  /**
   * 提交訂單
   */
  async submit(): Promise<void> {
    const order = this.currentOrder();
    if (!order) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    try {
      const result = await this.orderService.submit(order.orderId);
      this.currentOrder.set(result);
      this.successMessage.set(`訂單 ${result.orderId} 已提交成功！`);

      // 提交成功後可導向訂單詳情頁
      // this.router.navigate(['/orders', result.orderId]);
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '提交訂單失敗');
    } finally {
      this.isSubmitting.set(false);
    }
  }

  /**
   * 清除訊息
   */
  clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  // === 優惠券與紅利相關 ===

  /**
   * 處理優惠券套用成功
   */
  onCouponApplied(validation: CouponValidation): void {
    this.successMessage.set(`優惠券套用成功，折扣 ${validation.discountAmount?.amount ?? 0} 元`);
    // 重新取得訂單以更新折扣
    this.refreshOrder();
  }

  /**
   * 處理優惠券移除
   */
  onCouponRemoved(): void {
    this.successMessage.set('優惠券已移除');
    this.refreshOrder();
  }

  /**
   * 處理紅利折抵成功
   */
  onBonusRedeemed(redemption: BonusRedemption): void {
    this.successMessage.set(
      `紅利折抵成功：${redemption.skuName} 折抵 ${redemption.discountAmount.amount} 元，剩餘 ${redemption.remainingPoints} 點`
    );
    this.refreshOrder();
  }

  /**
   * 處理紅利折抵取消
   */
  onBonusCancelled(skuNo: string): void {
    this.successMessage.set('紅利折抵已取消');
    this.refreshOrder();
  }

  /**
   * 處理 order-summary 錯誤
   */
  onSummaryError(error: string): void {
    this.errorMessage.set(error);
  }

  /**
   * 重新取得訂單資料
   */
  private async refreshOrder(): Promise<void> {
    const order = this.currentOrder();
    if (!order) return;

    try {
      const updatedOrder = await this.orderService.getOrder(order.orderId);
      this.currentOrder.set(updatedOrder);
      this.orderLines.set(updatedOrder.lines);
    } catch (error: any) {
      this.errorMessage.set(error.message ?? '更新訂單資料失敗');
    }
  }
}
