import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  MastStoreResponse,
  SupportStoreResponse,
  StoreSelectionRequest,
} from '../models';

/**
 * 店別服務
 * 處理店別相關 API 呼叫
 */
@Injectable({ providedIn: 'root' })
export class StoreService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * 取得主店別
   */
  getMastStore(): Observable<ApiResponse<MastStoreResponse>> {
    return this.http.get<ApiResponse<MastStoreResponse>>(
      `${this.apiUrl}/stores/mast`
    );
  }

  /**
   * 取得支援店別清單
   */
  getSupportStores(): Observable<ApiResponse<SupportStoreResponse[]>> {
    return this.http.get<ApiResponse<SupportStoreResponse[]>>(
      `${this.apiUrl}/stores/support`
    );
  }

  /**
   * 送出店別選擇
   */
  selectStore(request: StoreSelectionRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/stores/select`,
      request
    );
  }
}
