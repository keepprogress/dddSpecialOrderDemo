import { test as base, Page } from '@playwright/test';

/**
 * Mock 認證 Fixture
 * 使用 Route Mock + localStorage 模擬已登入狀態
 */
export const test = base.extend<{ authenticatedPage: Page }>({
  authenticatedPage: async ({ page }, use) => {
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

    await use(page);
  },
});

export { expect } from '@playwright/test';
