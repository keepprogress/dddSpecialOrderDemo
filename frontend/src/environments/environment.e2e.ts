/**
 * E2E 測試環境設定
 * 跳過 Keycloak 初始化，使用 Mock 認證
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    url: '',  // Empty URL to skip Keycloak
    realm: 'som',
    clientId: 'som-frontend'
  },
  mockAuth: true  // Flag to indicate mock auth mode
};
