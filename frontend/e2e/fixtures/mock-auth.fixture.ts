import { test as base, Page } from '@playwright/test';

/**
 * Mock 認證 Fixture
 * 使用 Route Mock + localStorage 模擬已登入狀態
 * 包含 Keycloak 初始化的 Mock
 */
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
    // Mock Keycloak well-known configuration
    await page.route('**/realms/*/.well-known/openid-configuration', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          issuer: 'http://localhost:8180/realms/som',
          authorization_endpoint: 'http://localhost:8180/realms/som/protocol/openid-connect/auth',
          token_endpoint: 'http://localhost:8180/realms/som/protocol/openid-connect/token',
          userinfo_endpoint: 'http://localhost:8180/realms/som/protocol/openid-connect/userinfo',
          end_session_endpoint: 'http://localhost:8180/realms/som/protocol/openid-connect/logout',
          jwks_uri: 'http://localhost:8180/realms/som/protocol/openid-connect/certs',
        }),
      });
    });

    // Mock Keycloak token endpoint
    await page.route('**/realms/*/protocol/openid-connect/**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          access_token: 'mock-access-token',
          token_type: 'Bearer',
          expires_in: 3600,
          refresh_token: 'mock-refresh-token',
        }),
      });
    });

    // Mock silent-check-sso.html
    await page.route('**/silent-check-sso.html', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'text/html',
        body: '<html><body><script>parent.postMessage(location.href, location.origin)</script></body></html>',
      });
    });

    // Mock Keycloak server requests (catch-all for localhost:8180)
    await page.route('**/localhost:8180/**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({}),
      });
    });

    // Mock backend validate endpoint
    await page.route('**/api/auth/validate', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          message: '驗證成功',
          data: {
            userName: 'Test User',
            systemFlags: ['SO', 'TTS'],
            success: true,
          },
        }),
      });
    });

    // Mock stores endpoint
    await page.route('**/api/stores/mast', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            { storeId: 'S001', storeName: '台北門市', channelId: 'SO' },
            { storeId: 'S002', storeName: '新竹門市', channelId: 'SO' },
          ],
        }),
      });
    });

    // Mock channels endpoint
    await page.route('**/api/channels', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            { channelId: 'SO', channelName: 'Special Order' },
            { channelId: 'TTS', channelName: 'TTS' },
          ],
        }),
      });
    });

    // 設定 localStorage (模擬已登入且完成選擇)
    await page.addInitScript(() => {
      localStorage.setItem(
        'som_login_context',
        JSON.stringify({
          userId: 'test.user',
          userName: 'Test User',
          systemFlags: ['SO', 'TTS'],
          selectedStore: { storeId: 'S001', storeName: '台北門市' },
          supportStores: [],
          selectedChannel: { channelId: 'SO', channelName: 'Special Order' },
          loginTime: new Date().toISOString(),
        })
      );
    });

    // Navigate to home and wait for load
    await page.goto('/home');
    await page.waitForLoadState('networkidle');

    await use(page);
  },
});

export { expect } from '@playwright/test';
