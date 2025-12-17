/**
 * UAT 環境設定 (測試環境)
 * Database: Oracle UAT
 * Keycloak: Test Environment (authempsit02.testritegroup.com)
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  keycloak: {
    url: 'https://authempsit02.testritegroup.com/auth',
    realm: 'testritegroup-employee',
    clientId: 'epos-frontend'
  }
};
