import { Component, inject, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../services/order.service';
import {
  OrderLineResponse,
  CouponValidation,
  BonusRedemption,
  CalculationResponse
} from '../../models/order.model';

/**
 * 訂單摘要組件
 *
 * 功能：
 * - 顯示訂單金額摘要
 * - 優惠券輸入與套用
 * - 紅利點數折抵
 */
@Component({
  selector: 'app-order-summary',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-summary.component.html',
  styleUrl: './order-summary.component.scss'
})
export class OrderSummaryComponent {
  private orderService = inject(OrderService);

  // 讓 Math 可在 template 中使用
  protected Math = Math;

  // === Inputs ===
  orderId = input.required<string>();
  lines = input.required<OrderLineResponse[]>();
  calculation = input<CalculationResponse | null>(null);
  isTempCard = input<boolean>(false);

  // === Outputs ===
  couponApplied = output<CouponValidation>();
  couponRemoved = output<void>();
  bonusRedeemed = output<BonusRedemption>();
  bonusCancelled = output<string>();
  errorOccurred = output<string>();

  // === State ===
  couponId = signal('');
  couponValidation = signal<CouponValidation | null>(null);
  isCouponApplied = signal(false);

  selectedSkuNo = signal<string | null>(null);
  bonusPoints = signal(0);
  availableBonusPoints = signal(0);
  bonusRedemptions = signal<Map<string, BonusRedemption>>(new Map());

  isApplyingCoupon = signal(false);
  isRedeemingBonus = signal(false);
  isLoadingBonus = signal(false);

  // === Computed ===

  /**
   * 商品小計
   */
  productTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.subtotal, 0);
  });

  /**
   * 會員折扣總額
   */
  memberDiscountTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.memberDisc, 0);
  });

  /**
   * 優惠券折扣總額
   */
  couponDiscountTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.couponDisc, 0);
  });

  /**
   * 紅利折扣總額
   */
  bonusDiscountTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.bonusDisc, 0);
  });

  /**
   * 安裝費用總額
   */
  installationTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.installationCost, 0);
  });

  /**
   * 運送費用總額
   */
  deliveryTotal = computed(() => {
    return this.lines().reduce((sum, line) => sum + line.deliveryCost, 0);
  });

  /**
   * 應付總額
   */
  grandTotal = computed(() => {
    const calc = this.calculation();
    if (calc) {
      return calc.grandTotal;
    }
    return this.productTotal() +
           this.installationTotal() +
           this.deliveryTotal() -
           Math.abs(this.memberDiscountTotal()) -
           Math.abs(this.couponDiscountTotal()) -
           Math.abs(this.bonusDiscountTotal());
  });

  /**
   * 可使用紅利的商品清單
   */
  eligibleLinesForBonus = computed(() => {
    if (this.isTempCard()) {
      return [];
    }
    return this.lines().filter(line => line.bonusDisc === 0);
  });

  /**
   * 是否可使用紅利
   */
  canUseBonus = computed(() => {
    return !this.isTempCard() && this.availableBonusPoints() >= 10;
  });

  // === Methods ===

  /**
   * 載入可用紅利點數
   */
  async loadAvailableBonusPoints(): Promise<void> {
    if (this.isTempCard()) {
      this.availableBonusPoints.set(0);
      return;
    }

    this.isLoadingBonus.set(true);
    try {
      const points = await this.orderService.getAvailableBonusPoints(this.orderId());
      this.availableBonusPoints.set(points);
    } catch (error: any) {
      this.errorOccurred.emit(error.message ?? '查詢紅利點數失敗');
    } finally {
      this.isLoadingBonus.set(false);
    }
  }

  /**
   * 套用優惠券
   */
  async applyCoupon(): Promise<void> {
    const couponId = this.couponId().trim();
    if (!couponId) {
      this.errorOccurred.emit('請輸入優惠券代碼');
      return;
    }

    this.isApplyingCoupon.set(true);
    try {
      const validation = await this.orderService.applyCoupon(this.orderId(), { couponId });

      this.couponValidation.set(validation);

      if (validation.valid) {
        this.isCouponApplied.set(true);
        this.couponApplied.emit(validation);
      } else {
        this.errorOccurred.emit(validation.failureReason ?? '優惠券無效');
      }
    } catch (error: any) {
      this.errorOccurred.emit(error.message ?? '套用優惠券失敗');
    } finally {
      this.isApplyingCoupon.set(false);
    }
  }

  /**
   * 移除優惠券
   */
  async removeCoupon(): Promise<void> {
    try {
      await this.orderService.removeCoupon(this.orderId());
      this.couponId.set('');
      this.couponValidation.set(null);
      this.isCouponApplied.set(false);
      this.couponRemoved.emit();
    } catch (error: any) {
      this.errorOccurred.emit(error.message ?? '移除優惠券失敗');
    }
  }

  /**
   * 選擇商品進行紅利折抵
   */
  selectLineForBonus(skuNo: string): void {
    this.selectedSkuNo.set(skuNo);
    this.bonusPoints.set(0);
  }

  /**
   * 執行紅利折抵
   */
  async redeemBonus(): Promise<void> {
    const skuNo = this.selectedSkuNo();
    const points = this.bonusPoints();

    if (!skuNo) {
      this.errorOccurred.emit('請選擇要折抵的商品');
      return;
    }

    if (points < 10) {
      this.errorOccurred.emit('紅利點數至少需要 10 點');
      return;
    }

    if (points > this.availableBonusPoints()) {
      this.errorOccurred.emit('紅利點數不足');
      return;
    }

    this.isRedeemingBonus.set(true);
    try {
      const redemption = await this.orderService.redeemBonusPoints(
        this.orderId(),
        { skuNo, points }
      );

      // 更新可用點數
      this.availableBonusPoints.set(redemption.remainingPoints);

      // 記錄折抵
      this.bonusRedemptions.update(map => {
        const newMap = new Map(map);
        newMap.set(skuNo, redemption);
        return newMap;
      });

      // 重置選擇
      this.selectedSkuNo.set(null);
      this.bonusPoints.set(0);

      this.bonusRedeemed.emit(redemption);
    } catch (error: any) {
      this.errorOccurred.emit(error.message ?? '紅利折抵失敗');
    } finally {
      this.isRedeemingBonus.set(false);
    }
  }

  /**
   * 取消紅利折抵
   */
  async cancelBonus(skuNo: string): Promise<void> {
    try {
      await this.orderService.cancelBonusRedemption(this.orderId(), skuNo);

      // 從記錄中移除
      this.bonusRedemptions.update(map => {
        const newMap = new Map(map);
        newMap.delete(skuNo);
        return newMap;
      });

      // 重新載入可用點數
      await this.loadAvailableBonusPoints();

      this.bonusCancelled.emit(skuNo);
    } catch (error: any) {
      this.errorOccurred.emit(error.message ?? '取消紅利折抵失敗');
    }
  }

  /**
   * 取得商品的紅利折抵記錄
   */
  getRedemption(skuNo: string): BonusRedemption | undefined {
    return this.bonusRedemptions().get(skuNo);
  }

  /**
   * 格式化金額
   */
  formatAmount(amount: number): string {
    return new Intl.NumberFormat('zh-TW').format(amount);
  }
}
