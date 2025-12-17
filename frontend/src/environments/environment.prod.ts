/**
 * 正式環境設定
 */
export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    url: 'https://auth.example.com',
    realm: 'som',
    clientId: 'som-frontend'
  }
};
