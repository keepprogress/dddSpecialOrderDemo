import { Routes } from '@angular/router';

export const CREATE_ORDER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./create-order.component').then(m => m.CreateOrderComponent),
    title: '新增訂單'
  }
];
