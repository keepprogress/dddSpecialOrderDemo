import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoadingComponent } from './shared/components/loading/loading.component';
import { ErrorToastComponent } from './shared/components/error-toast/error-toast.component';
import { ToastComponent } from './shared/components/toast/toast.component';

/**
 * 根元件 (Angular 21+ Standalone, OnPush)
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, LoadingComponent, ErrorToastComponent, ToastComponent],
  template: `
    <app-loading />
    <app-error-toast />
    <app-toast />
    <router-outlet />
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100vh;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
