import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Skeleton Loader Component
 *
 * 用於載入狀態的骨架屏顯示
 * 支援 card, text, avatar, button 等類型
 */
@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="skeleton-loader" [class]="'skeleton-' + type()">
      @switch (type()) {
        @case ('card') {
          <div class="skeleton-card">
            <div class="skeleton-header"></div>
            <div class="skeleton-body">
              <div class="skeleton-line"></div>
              <div class="skeleton-line short"></div>
            </div>
          </div>
        }
        @case ('text') {
          <div class="skeleton-text">
            @for (line of lines(); track $index) {
              <div class="skeleton-line" [class.short]="$last"></div>
            }
          </div>
        }
        @case ('avatar') {
          <div class="skeleton-avatar"></div>
        }
        @case ('button') {
          <div class="skeleton-button"></div>
        }
        @case ('table') {
          <div class="skeleton-table">
            @for (row of rows(); track $index) {
              <div class="skeleton-row">
                @for (col of columns(); track $index) {
                  <div class="skeleton-cell"></div>
                }
              </div>
            }
          </div>
        }
        @default {
          <div class="skeleton-default"></div>
        }
      }
    </div>
  `,
  styles: [`
    .skeleton-loader {
      --skeleton-bg: #e0e0e0;
      --skeleton-shimmer: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.4) 50%, transparent 100%);
    }

    .skeleton-line,
    .skeleton-header,
    .skeleton-avatar,
    .skeleton-button,
    .skeleton-cell,
    .skeleton-default {
      background: var(--skeleton-bg);
      border-radius: 4px;
      position: relative;
      overflow: hidden;
    }

    .skeleton-line::after,
    .skeleton-header::after,
    .skeleton-avatar::after,
    .skeleton-button::after,
    .skeleton-cell::after,
    .skeleton-default::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: var(--skeleton-shimmer);
      animation: shimmer 1.5s infinite;
    }

    @keyframes shimmer {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }

    .skeleton-card {
      padding: 16px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
    }

    .skeleton-header {
      height: 24px;
      margin-bottom: 16px;
      width: 60%;
    }

    .skeleton-body .skeleton-line {
      height: 16px;
      margin-bottom: 8px;
    }

    .skeleton-line.short {
      width: 40%;
    }

    .skeleton-text .skeleton-line {
      height: 14px;
      margin-bottom: 8px;
      width: 100%;
    }

    .skeleton-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
    }

    .skeleton-button {
      width: 120px;
      height: 36px;
    }

    .skeleton-table {
      width: 100%;
    }

    .skeleton-row {
      display: flex;
      gap: 8px;
      margin-bottom: 8px;
    }

    .skeleton-cell {
      flex: 1;
      height: 32px;
    }

    .skeleton-default {
      height: 100px;
      width: 100%;
    }
  `]
})
export class SkeletonLoaderComponent {
  /** 骨架屏類型 */
  type = input<'card' | 'text' | 'avatar' | 'button' | 'table' | 'default'>('default');

  /** 文字行數 (用於 text 類型) */
  lines = input<number[]>([1, 2, 3]);

  /** 表格列數 (用於 table 類型) */
  rows = input<number[]>([1, 2, 3, 4, 5]);

  /** 表格欄數 (用於 table 類型) */
  columns = input<number[]>([1, 2, 3, 4]);
}
