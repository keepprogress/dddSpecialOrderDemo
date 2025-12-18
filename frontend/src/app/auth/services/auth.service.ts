import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { Observable, tap, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApiResponse,
  UserValidationResponse,
  LoginContext,
  StoreInfo,
  ChannelInfo,
} from '../models';

const LOGIN_CONTEXT_KEY = 'som_login_context';

/**
 * 認證服務 (Angular 21+ Signals)
 * 管理使用者認證狀態和 LoginContext
 * 使用新的 keycloak-angular API (注入 Keycloak 而非 deprecated KeycloakService)
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly keycloak = inject(Keycloak);
  private readonly router = inject(Router);

  // Signals
  private readonly _loginContext = signal<LoginContext | null>(
    this.loadLoginContext()
  );
  private readonly _isValidating = signal(false);
  private readonly _validationError = signal<string | null>(null);

  // Computed
  readonly loginContext = computed(() => this._loginContext());
  readonly isValidating = computed(() => this._isValidating());
  readonly validationError = computed(() => this._validationError());
  readonly isAuthenticated = computed(() => this._loginContext() !== null);
  readonly userName = computed(() => this._loginContext()?.userName ?? '');
  readonly selectedStore = computed(() => this._loginContext()?.selectedStore);
  readonly selectedChannel = computed(
    () => this._loginContext()?.selectedChannel
  );

  /**
   * 驗證使用者 (呼叫後端 6-checkpoint 驗證)
   */
  validateUser(): Observable<ApiResponse<UserValidationResponse>> {
    this._isValidating.set(true);
    this._validationError.set(null);

    return this.http
      .post<ApiResponse<UserValidationResponse>>(
        `${environment.apiUrl}/auth/validate`,
        {}
      )
      .pipe(
        tap((response) => {
          this._isValidating.set(false);

          if (response.success && response.data) {
            // 建立初始 LoginContext
            // 使用 keycloak.tokenParsed 取得使用者名稱
            const context: LoginContext = {
              userId: this.keycloak.tokenParsed?.['preferred_username'] || '',
              userName: response.data.userName,
              systemFlags: response.data.systemFlags,
              selectedStore: null,
              supportStores: [],
              selectedChannel: null,
              loginTime: new Date().toISOString(),
            };
            this.saveLoginContext(context);
          } else {
            this._validationError.set(response.message);
          }
        }),
        catchError((error) => {
          this._isValidating.set(false);
          this._validationError.set(
            error.error?.message || '驗證失敗，請稍後再試'
          );
          return throwError(() => error);
        })
      );
  }

  /**
   * 更新選擇的店別
   */
  updateSelectedStore(
    mastStore: StoreInfo | null,
    supportStores: StoreInfo[]
  ): void {
    const context = this._loginContext();
    if (context) {
      const updated: LoginContext = {
        ...context,
        selectedStore: mastStore,
        supportStores,
      };
      this.saveLoginContext(updated);
    }
  }

  /**
   * 更新選擇的系統別
   */
  updateSelectedChannel(channel: ChannelInfo): void {
    const context = this._loginContext();
    if (context) {
      const updated: LoginContext = {
        ...context,
        selectedChannel: channel,
      };
      this.saveLoginContext(updated);
    }
  }

  /**
   * 登出
   */
  async logout(): Promise<void> {
    this.clearLoginContext();
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  /**
   * 清除 LoginContext
   */
  clearLoginContext(): void {
    localStorage.removeItem(LOGIN_CONTEXT_KEY);
    this._loginContext.set(null);
  }

  /**
   * 儲存 LoginContext
   */
  private saveLoginContext(context: LoginContext): void {
    localStorage.setItem(LOGIN_CONTEXT_KEY, JSON.stringify(context));
    this._loginContext.set(context);
  }

  /**
   * 載入 LoginContext
   */
  private loadLoginContext(): LoginContext | null {
    const json = localStorage.getItem(LOGIN_CONTEXT_KEY);
    if (!json) return null;

    try {
      return JSON.parse(json) as LoginContext;
    } catch {
      return null;
    }
  }
}
