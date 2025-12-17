import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { TabManagerService } from '../../services/tab-manager.service';

/**
 * Tab 封鎖元件 (Angular 21+ Standalone, OnPush, Signals)
 * 顯示「已在其他分頁開啟」訊息
 */
@Component({
  selector: 'app-tab-blocked',
  standalone: true,
  imports: [],
  template: `
    <div class="blocked-container">
      <div class="blocked-card">
        <div class="icon">&#9888;</div>
        <h2>已在其他分頁開啟</h2>
        <p>
          特殊訂單系統只允許在一個分頁中使用。<br />
          請關閉此分頁，或點擊下方按鈕繼續在此分頁使用。
        </p>
        <button class="btn-primary" (click)="continueHere()">
          繼續在此分頁使用
        </button>
      </div>
    </div>
  `,
  styles: [
    `
      .blocked-container {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100vh;
        background-color: #f5f5f5;
      }

      .blocked-card {
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        text-align: center;
        max-width: 400px;
      }

      .icon {
        font-size: 4rem;
        color: #f39c12;
        margin-bottom: 1rem;
      }

      .blocked-card h2 {
        margin-bottom: 1rem;
        color: #333;
      }

      .blocked-card p {
        color: #666;
        margin-bottom: 1.5rem;
        line-height: 1.6;
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
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TabBlockedComponent {
  private readonly tabManager = inject(TabManagerService);
  private readonly router = inject(Router);

  /**
   * 繼續在此分頁使用
   */
  continueHere(): void {
    this.tabManager.reclaim();
    this.router.navigate(['/']);
  }
}
