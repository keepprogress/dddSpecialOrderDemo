import { Routes } from '@angular/router';
import { authGuard, loginContextGuard, storeSelectionGuard } from './auth/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/components/login/login.component').then(
        (m) => m.LoginComponent
      ),
  },
  {
    path: 'validate',
    loadComponent: () =>
      import('./auth/components/validate/validate.component').then(
        (m) => m.ValidateComponent
      ),
    canActivate: [authGuard],
  },
  {
    path: 'validation-error',
    loadComponent: () =>
      import(
        './auth/components/validation-error/validation-error.component'
      ).then((m) => m.ValidationErrorComponent),
  },
  {
    path: 'tab-blocked',
    loadComponent: () =>
      import('./auth/components/tab-blocked/tab-blocked.component').then(
        (m) => m.TabBlockedComponent
      ),
  },
  {
    path: 'store-selection',
    loadComponent: () =>
      import(
        './auth/components/store-selection/store-selection.component'
      ).then((m) => m.StoreSelectionComponent),
    canActivate: [authGuard, storeSelectionGuard],
  },
  {
    path: 'channel-selection',
    loadComponent: () =>
      import(
        './auth/components/channel-selection/channel-selection.component'
      ).then((m) => m.ChannelSelectionComponent),
    canActivate: [authGuard, storeSelectionGuard],
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./core/home/home.component').then((m) => m.HomeComponent),
    canActivate: [authGuard, loginContextGuard],
  },
  // 導航列選單項目 (功能開發中)
  {
    path: 'orders',
    loadComponent: () =>
      import('./shared/components/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    canActivate: [authGuard, loginContextGuard],
    data: { title: '訂單管理' },
  },
  {
    path: 'returns',
    loadComponent: () =>
      import('./shared/components/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    canActivate: [authGuard, loginContextGuard],
    data: { title: '退貨管理' },
  },
  {
    path: 'shipping',
    loadComponent: () =>
      import('./shared/components/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    canActivate: [authGuard, loginContextGuard],
    data: { title: '安運單管理' },
  },
  {
    path: 'master',
    loadComponent: () =>
      import('./shared/components/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    canActivate: [authGuard, loginContextGuard],
    data: { title: '主檔維護' },
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./shared/components/placeholder/placeholder.component').then(
        (m) => m.PlaceholderComponent
      ),
    canActivate: [authGuard, loginContextGuard],
    data: { title: '報表' },
  },
  {
    path: '**',
    redirectTo: 'home',
  },
];
