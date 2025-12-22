import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  CreateOrderRequest,
  OrderResponse,
  AddOrderLineRequest,
  OrderLineResponse,
  CalculationResponse,
  UpdateOrderLineRequest,
  InstallationService,
  WorkTypeResponse,
  ApplyCouponRequest,
  CouponValidation,
  RedeemBonusRequest,
  BonusRedemption
} from '../models/order.model';
import { environment } from '../../../../environments/environment';

/**
 * Order Service
 *
 * 處理訂單相關的 API 呼叫
 */
@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/orders`;

  /**
   * 建立新訂單
   */
  async createOrder(
    request: CreateOrderRequest,
    idempotencyKey: string
  ): Promise<OrderResponse> {
    const headers = new HttpHeaders({
      'X-Idempotency-Key': idempotencyKey
    });

    return firstValueFrom(
      this.http.post<OrderResponse>(this.baseUrl, request, { headers })
    );
  }

  /**
   * 取得訂單
   */
  async getOrder(orderId: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.get<OrderResponse>(`${this.baseUrl}/${orderId}`)
    );
  }

  /**
   * 新增訂單行項
   */
  async addLine(
    orderId: string,
    request: AddOrderLineRequest
  ): Promise<OrderLineResponse> {
    return firstValueFrom(
      this.http.post<OrderLineResponse>(
        `${this.baseUrl}/${orderId}/lines`,
        request
      )
    );
  }

  /**
   * 更新訂單行項
   */
  async updateLine(
    orderId: string,
    lineId: string,
    request: Partial<AddOrderLineRequest>
  ): Promise<OrderLineResponse> {
    return firstValueFrom(
      this.http.put<OrderLineResponse>(
        `${this.baseUrl}/${orderId}/lines/${lineId}`,
        request
      )
    );
  }

  /**
   * 刪除訂單行項
   */
  async removeLine(orderId: string, lineId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.baseUrl}/${orderId}/lines/${lineId}`)
    );
  }

  /**
   * 執行價格試算
   */
  async calculate(orderId: string): Promise<CalculationResponse> {
    return firstValueFrom(
      this.http.post<CalculationResponse>(
        `${this.baseUrl}/${orderId}/calculate`,
        {}
      )
    );
  }

  /**
   * 提交訂單
   */
  async submit(orderId: string): Promise<OrderResponse> {
    return firstValueFrom(
      this.http.post<OrderResponse>(
        `${this.baseUrl}/${orderId}/submit`,
        {}
      )
    );
  }

  /**
   * 設定安裝服務
   */
  async attachInstallation(
    orderId: string,
    lineId: string,
    request: UpdateOrderLineRequest
  ): Promise<OrderLineResponse> {
    return firstValueFrom(
      this.http.put<OrderLineResponse>(
        `${this.baseUrl}/${orderId}/lines/${lineId}/installation`,
        request
      )
    );
  }

  /**
   * 設定運送服務
   */
  async attachDelivery(
    orderId: string,
    lineId: string,
    request: UpdateOrderLineRequest
  ): Promise<OrderLineResponse> {
    return firstValueFrom(
      this.http.put<OrderLineResponse>(
        `${this.baseUrl}/${orderId}/lines/${lineId}/delivery`,
        request
      )
    );
  }

  /**
   * 取得可用安裝服務
   */
  async getAvailableServices(
    orderId: string,
    lineId: string
  ): Promise<InstallationService[]> {
    return firstValueFrom(
      this.http.get<InstallationService[]>(
        `${this.baseUrl}/${orderId}/lines/${lineId}/available-services`
      )
    );
  }

  /**
   * 取得所有工種
   */
  async getWorkTypes(): Promise<WorkTypeResponse[]> {
    return firstValueFrom(
      this.http.get<WorkTypeResponse[]>(`${environment.apiUrl}/api/v1/work-types`)
    );
  }

  /**
   * 取得安裝工種
   */
  async getInstallationWorkTypes(): Promise<WorkTypeResponse[]> {
    return firstValueFrom(
      this.http.get<WorkTypeResponse[]>(`${environment.apiUrl}/api/v1/work-types/installation`)
    );
  }

  /**
   * 取得運送工種
   */
  async getDeliveryWorkTypes(): Promise<WorkTypeResponse[]> {
    return firstValueFrom(
      this.http.get<WorkTypeResponse[]>(`${environment.apiUrl}/api/v1/work-types/delivery`)
    );
  }

  /**
   * 產生冪等鍵
   */
  generateIdempotencyKey(): string {
    return crypto.randomUUID();
  }

  // ============ 優惠券 ============

  /**
   * 套用優惠券
   */
  async applyCoupon(
    orderId: string,
    request: ApplyCouponRequest
  ): Promise<CouponValidation> {
    return firstValueFrom(
      this.http.post<CouponValidation>(
        `${this.baseUrl}/${orderId}/coupons`,
        request
      )
    );
  }

  /**
   * 驗證優惠券
   */
  async validateCoupon(
    orderId: string,
    couponId: string
  ): Promise<CouponValidation> {
    return firstValueFrom(
      this.http.get<CouponValidation>(
        `${this.baseUrl}/${orderId}/coupons/validate`,
        { params: { couponId } }
      )
    );
  }

  /**
   * 移除優惠券
   */
  async removeCoupon(orderId: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.baseUrl}/${orderId}/coupons`)
    );
  }

  // ============ 紅利點數 ============

  /**
   * 查詢可用紅利點數
   */
  async getAvailableBonusPoints(orderId: string): Promise<number> {
    return firstValueFrom(
      this.http.get<number>(`${this.baseUrl}/${orderId}/bonus/available`)
    );
  }

  /**
   * 紅利折抵
   */
  async redeemBonusPoints(
    orderId: string,
    request: RedeemBonusRequest
  ): Promise<BonusRedemption> {
    return firstValueFrom(
      this.http.post<BonusRedemption>(
        `${this.baseUrl}/${orderId}/bonus`,
        request
      )
    );
  }

  /**
   * 取消紅利折抵
   */
  async cancelBonusRedemption(orderId: string, skuNo: string): Promise<void> {
    return firstValueFrom(
      this.http.delete<void>(`${this.baseUrl}/${orderId}/bonus/${skuNo}`)
    );
  }
}
