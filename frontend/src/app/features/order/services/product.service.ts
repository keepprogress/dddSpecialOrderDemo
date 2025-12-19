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
   */
  getEligibilityErrorMessage(level: number): string {
    const messages: Record<number, string> = {
      1: '商品編號格式錯誤',
      2: '商品不存在',
      3: '系統商品無法銷售',
      4: '稅別設定錯誤',
      5: '商品已禁止銷售',
      6: '商品類別限制銷售'
    };
    return messages[level] || '商品不符合銷售資格';
  }
}
