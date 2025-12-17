/**
 * SIT 環境設定 (本地開發)
 * Database: H2
 * Keycloak: Local (localhost:8180)
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
