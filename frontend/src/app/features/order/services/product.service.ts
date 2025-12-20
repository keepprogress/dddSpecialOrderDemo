import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { EligibilityResponse } from '../models/order.model';
import { environment } from '../../../../environments/environment';

/**
 * Product Service
 *
 * 處理商品相關的 API 呼叫
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/products`;

  /**
   * 檢查商品銷售資格
   *
   * @param skuNo 商品編號
   * @param channelId 通路代號
   * @param storeId 店別代號
   * @returns 資格驗證結果
   */
  async checkEligibility(
    skuNo: string,
    channelId?: string,
    storeId?: string
  ): Promise<EligibilityResponse> {
    const params: Record<string, string> = {};
    if (channelId) params['channelId'] = channelId;
    if (storeId) params['storeId'] = storeId;

    return firstValueFrom(
      this.http.get<EligibilityResponse>(
        `${this.baseUrl}/${skuNo}/eligibility`,
        { params }
      )
    );
  }

  /**
   * 驗證商品編號格式
   */
  isValidSkuNo(skuNo: string): boolean {
    if (!skuNo) return false;
    // SKU 格式: 英數字，至少 5 碼
    return /^[A-Za-z0-9]{5,}$/.test(skuNo);
  }

  /**
   * 取得資格驗證失敗訊息
   *
   * 來源: product-query-spec.md 8-Layer 驗證
   */
  getEligibilityErrorMessage(level: number): string {
    const messages: Record<number, string> = {
      1: '商品編號格式錯誤',
      2: '商品不存在',
      3: '系統商品無法銷售',
      4: '稅別設定錯誤',
      5: '商品已禁止銷售',
      6: '商品類別限制銷售',
      7: '廠商已凍結，商品無法訂購',
      8: '商品不在門市採購組織內'
    };
    return messages[level] || '商品不符合銷售資格';
  }

  /**
   * 判斷是否為 DC 商品廠商凍結（需特殊處理備貨方式）
   */
  isDcVendorFrozen(eligibility: EligibilityResponse): boolean {
    return eligibility.orderability?.isDcVendorFrozen ?? false;
  }

  /**
   * 判斷 DC 商品廠商凍結是否需強制現貨
   * DC商品廠商凍結 + 非大型家具 + 庫存不足 → 強制現貨
   */
  requiresSpotStock(eligibility: EligibilityResponse, requestedQuantity: number): boolean {
    const orderability = eligibility.orderability;
    if (!orderability?.isDcVendorFrozen) return false;
    if (orderability.isLargeFurniture) return false;
    return orderability.stockAoh < requestedQuantity;
  }
}
