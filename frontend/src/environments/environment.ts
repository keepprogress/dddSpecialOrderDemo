/**
 * 預設開發環境設定 (同 SIT)
 * Database: H2
 * Keycloak: Local (localhost:8180)
 *
 * 使用其他環境:
 * - ng serve --configuration=sit  (本地 Keycloak)
 * - ng serve --configuration=uat  (測試區 Keycloak)
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'som',
    clientId: 'som-frontend'
  }
};
