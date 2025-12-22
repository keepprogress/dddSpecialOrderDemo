import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, catchError, of } from 'rxjs';
import { MemberResponse, TempMemberRequest } from '../models/order.model';
import { environment } from '../../../../environments/environment';

/**
 * Member Service
 *
 * 處理會員相關的 API 呼叫
 */
@Injectable({ providedIn: 'root' })
export class MemberService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/members`;

  /**
   * 查詢會員
   *
   * @param memberId 會員卡號
   * @returns 會員資料，若查無資料則返回 null
   */
  async getMember(memberId: string): Promise<MemberResponse | null> {
    try {
      return await firstValueFrom(
        this.http.get<MemberResponse>(`${this.baseUrl}/${memberId}`).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status === 404) {
              return of(null);
            }
            throw error;
          })
        )
      );
    } catch (error) {
      console.error('查詢會員失敗:', error);
      throw error;
    }
  }

  /**
   * 建立臨時卡
   *
   * @param request 臨時卡資料
   * @returns 臨時卡會員資料
   */
  async createTempMember(request: TempMemberRequest): Promise<MemberResponse> {
    return firstValueFrom(
      this.http.post<MemberResponse>(`${this.baseUrl}/temp`, request)
    );
  }

  /**
   * 驗證會員卡號格式
   */
  isValidMemberId(memberId: string): boolean {
    if (!memberId) return false;
    // 一般會員卡號格式: 英文字母開頭 + 數字
    return /^[A-Za-z]\d{5,}$/.test(memberId);
  }

  /**
   * 驗證手機號碼格式
   */
  isValidCellPhone(cellPhone: string): boolean {
    if (!cellPhone) return false;
    // 台灣手機號碼格式: 09 開頭，共 10 碼
    return /^09\d{8}$/.test(cellPhone);
  }

  /**
   * 驗證郵遞區號格式
   */
  isValidZipCode(zipCode: string): boolean {
    if (!zipCode) return false;
    return /^\d{3}$/.test(zipCode);
  }
}
