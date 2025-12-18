import { Component, inject, input, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';

/**
 * 驗證錯誤元件 (Angular 21+ Standalone, OnPush, Signal Inputs)
 * 顯示 6-checkpoint 驗證失敗訊息
 * 使用新的 keycloak-angular API (注入 Keycloak 而非 deprecated KeycloakService)
 */
@Component({
  selector: 'app-validation-error',
  standalone: true,
  imports: [],
  template: `
    <div class="error-container">
      <div class="error-card">
        <div class="icon">&#10060;</div>
        <h2>無法登入系統</h2>

        @if (errorCode()) {
          <div class="error-code">錯誤代碼: {{ errorCode() }}</div>
        }

        <p class="error-message">{{ errorMessage() }}</p>

        <div class="actions">
          <button class="btn-secondary" (click)="retry()">重新嘗試</button>
          <button class="btn-primary" (click)="logout()">登出</button>
        </div>

        <p class="help-text">如需協助，請聯繫系統管理員</p>
      </div>
    </div>
  `,
  styles: [
    `
      .error-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
        background-color: #f5f5f5;
      }

      .error-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        text-align: center;
        max-width: 450px;
      }

      .icon {
        font-size: 4rem;
        margin-bottom: 1rem;
      }

      .error-card h2 {
        margin-bottom: 1rem;
        color: #333;
      }

      .error-code {
        background-color: #f8f8f8;
        padding: 0.5rem 1rem;
        border-radius: 4px;
        font-family: monospace;
        color: #666;
        margin-bottom: 1rem;
        display: inline-block;
      }

      .error-message {
        color: #e74c3c;
        margin-bottom: 1.5rem;
        line-height: 1.6;
        font-size: 1.1rem;
      }

      .actions {
        display: flex;
        gap: 1rem;
        justify-content: center;
        margin-bottom: 1.5rem;
      }

      .btn-primary {
        background-color: #3498db;
        color: white;
        border: none;
        padding: 0.75rem 1.5rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        transition: background-color 0.2s;
      }

      .btn-primary:hover {
        background-color: #2980b9;
      }

      .btn-secondary {
        background-color: white;
        color: #333;
        border: 1px solid #ddd;
        padding: 0.75rem 1.5rem;
        border-radius: 4px;
        cursor: pointer;
        font-size: 1rem;
        transition: all 0.2s;
      }

      .btn-secondary:hover {
        background-color: #f5f5f5;
        border-color: #ccc;
      }

      .help-text {
        color: #999;
        font-size: 0.9rem;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ValidationErrorComponent {
  private readonly router = inject(Router);
  private readonly keycloak = inject(Keycloak);

  // Signal Inputs
  errorCode = input<string>('');
  errorMessage = input<string>('驗證失敗，請聯繫管理員');

  /**
   * 重新嘗試
   */
  retry(): void {
    this.router.navigate(['/login']);
  }

  /**
   * 登出
   */
  async logout(): Promise<void> {
    await this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
