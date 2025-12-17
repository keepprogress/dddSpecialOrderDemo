import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';

/**
 * 功能開發中佔位元件 (Angular 21+ Standalone, OnPush, Signals)
 */
@Component({
  selector: 'app-placeholder',
  standalone: true,
  imports: [],
  template: `
    <div class="placeholder-container">
      <div class="placeholder-icon">🚧</div>
      <h2 class="placeholder-title">{{ title() }}</h2>
      <p class="placeholder-message">功能開發中，敬請期待</p>
    </div>
  `,
  styles: [
    `
      .placeholder-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 400px;
        padding: 2rem;
        text-align: center;
      }

      .placeholder-icon {
        font-size: 4rem;
        margin-bottom: 1rem;
      }

      .placeholder-title {
        font-size: 1.5rem;
        color: #2c3e50;
        margin-bottom: 0.5rem;
      }

      .placeholder-message {
        font-size: 1rem;
        color: #7f8c8d;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);

  /** 從路由 data 讀取標題 */
  private readonly routeData = toSignal(
    this.route.data.pipe(map((data) => data['title'] as string))
  );

  /** 頁面標題 */
  readonly title = computed(() => this.routeData() || '功能開發中');
}
