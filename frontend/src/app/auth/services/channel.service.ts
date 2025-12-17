import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ApiResponse, ChannelResponse, ChannelSelectionRequest } from '../models';

/**
 * 系統別服務
 * 處理系統別相關 API 呼叫
 */
@Injectable({ providedIn: 'root' })
export class ChannelService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * 取得可用系統別清單
   */
  getAvailableChannels(): Observable<ApiResponse<ChannelResponse[]>> {
    return this.http.get<ApiResponse<ChannelResponse[]>>(
      `${this.apiUrl}/channels`
    );
  }

  /**
   * 送出系統別選擇
   */
  selectChannel(request: ChannelSelectionRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.apiUrl}/channels/select`,
      request
    );
  }
}
