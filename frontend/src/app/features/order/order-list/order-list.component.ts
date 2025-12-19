import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * 訂單列表頁面
 *
 * 功能：
 * - 顯示訂單列表 (待開發)
 * - 提供新增訂單入口
 */
@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="order-list-page">
      <header class="page-header">
        <h1>訂單管理</h1>
        <a routerLink="create" class="btn btn-primary">
          + 新增訂單
        </a>
      </header>

      <section class="order-list-section">
        <div class="placeholder-message">
          <p>訂單列表功能開發中...</p>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .order-list-page {
      padding: 1.5rem;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .page-header h1 {
      margin: 0;
      font-size: 1.5rem;
      color: #2c3e50;
    }

    .btn {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      border: none;
      border-radius: 6px;
      font-size: 0.95rem;
      font-weight: 500;
      cursor: pointer;
      text-decoration: none;
      transition: all 0.2s;
    }

    .btn-primary {
      background-color: #3498db;
      color: white;
    }

    .btn-primary:hover {
      background-color: #2980b9;
    }

    .order-list-section {
      background: #fff;
      border-radius: 8px;
      padding: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .placeholder-message {
      text-align: center;
      color: #7f8c8d;
      padding: 3rem;
    }

    .placeholder-message p {
      margin: 0;
      font-size: 1.1rem;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class OrderListComponent {}
