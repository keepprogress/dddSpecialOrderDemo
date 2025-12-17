import { Component, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

/**
 * 登入元件 (Angular 21+ Standalone, OnPush)
 * 檢查登入狀態並導向 Keycloak 或進行驗證
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [],
  template: `
    <div class="login-container">
      <div class="login-card">
        <h1>特殊訂單系統</h1>
        <p>正在驗證您的身份...</p>
        <div class="spinner"></div>
      </div>
    </div>
  `,
  styles: [
    `
      .login-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
        background-color: #f5f5f5;
      }

      .login-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        text-align: center;
      }

      .login-card h1 {
        margin-bottom: 1rem;
        color: #333;
      }

      .login-card p {
        color: #666;
        margin-bottom: 1.5rem;
      }

      .spinner {
        width: 40px;
        height: 40px;
        margin: 0 auto;
        border: 4px solid #f3f3f3;
        border-top: 4px solid #3498db;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }

      @keyframes spin {
        0% {
          transform: rotate(0deg);
        }
        100% {
          transform: rotate(360deg);
        }
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent implements OnInit {
  private readonly keycloak = inject(KeycloakService);
  private readonly router = inject(Router);

  async ngOnInit(): Promise<void> {
    const isLoggedIn = this.keycloak.isLoggedIn();

    if (!isLoggedIn) {
      // 未登入，導向 Keycloak 登入頁
      await this.keycloak.login({
        redirectUri: window.location.origin + '/login',
      });
    } else {
      // 已登入，導向驗證流程
      this.router.navigate(['/validate']);
    }
  }
}
