import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import Keycloak from 'keycloak-js';

/**
 * Auth Guard (Angular 21+ Functional Guard)
 * 驗證使用者是否已登入
 * 使用新的 keycloak-angular API (注入 Keycloak 而非 deprecated KeycloakService)
 */
export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);

  const isLoggedIn = keycloak.authenticated ?? false;

  if (!isLoggedIn) {
    // 未登入，導向 Keycloak 登入頁
    await keycloak.login({
      redirectUri: window.location.origin,
    });
    return false;
  }

  return true;
};

/**
 * Login Context Guard
 * 驗證使用者是否已完成 6-checkpoint 驗證
 */
export const loginContextGuard: CanActivateFn = () => {
  const router = inject(Router);

  // 檢查 LocalStorage 是否有 LoginContext
  const loginContextJson = localStorage.getItem('som_login_context');

  if (!loginContextJson) {
    // 尚未完成驗證，導向登入頁
    router.navigate(['/login']);
    return false;
  }

  try {
    const loginContext = JSON.parse(loginContextJson);

    // 檢查是否已選擇店別和系統別
    if (!loginContext.selectedStore || !loginContext.selectedChannel) {
      router.navigate(['/store-selection']);
      return false;
    }

    return true;
  } catch {
    // JSON 解析失敗，清除並重新登入
    localStorage.removeItem('som_login_context');
    router.navigate(['/login']);
    return false;
  }
};

/**
 * Store Selection Guard
 * 驗證使用者是否需要選擇店別
 */
export const storeSelectionGuard: CanActivateFn = () => {
  const router = inject(Router);
  const keycloak = inject(Keycloak);

  // 必須先登入
  if (!(keycloak.authenticated ?? false)) {
    router.navigate(['/login']);
    return false;
  }

  // 檢查是否已完成 6-checkpoint 驗證
  const loginContextJson = localStorage.getItem('som_login_context');
  if (!loginContextJson) {
    router.navigate(['/login']);
    return false;
  }

  return true;
};
