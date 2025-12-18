import { test as base, expect } from '@playwright/test';

// 建立未完成選擇的 fixture
const test = base.extend({
  pageWithPartialLogin: async ({ page }, use) => {
    // Mock API endpoints
    await page.route('**/api/stores/mast', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            { storeId: null, storeName: '全區', channelId: null },
            { storeId: 'S001', storeName: '台北門市', channelId: 'SO' },
            { storeId: 'S002', storeName: '新竹門市', channelId: 'SO' },
          ],
        }),
      });
    });

    await page.route('**/api/stores/support*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [
            { storeId: 'S003', storeName: '台中門市', channelId: 'SO' },
            { storeId: 'S004', storeName: '高雄門市', channelId: 'SO' },
          ],
        }),
      });
    });

    await page.route('**/api/stores/select', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      });
    });

    // 模擬已通過驗證但未選擇店別的狀態
    await page.addInitScript(() => {
      localStorage.setItem(
        'som_login_context',
        JSON.stringify({
          userId: 'test.user',
          userName: 'Test User',
          systemFlags: ['SO', 'TTS'],
          selectedStore: null,
          supportStores: [],
          selectedChannel: null,
          loginTime: new Date().toISOString(),
        })
      );
    });

    await use(page);
  },
});

test.describe('Store Selection', () => {
  test('should display master store dropdown', async ({ pageWithPartialLogin }) => {
    await pageWithPartialLogin.goto('/store-selection');

    // 等待頁面載入
    await pageWithPartialLogin.waitForLoadState('networkidle');

    // 驗證主店別下拉選單存在
    const dropdown = pageWithPartialLogin.locator('select, [role="combobox"]').first();
    await expect(dropdown).toBeVisible();
  });

  test('should show 全區 option for admin users', async ({ pageWithPartialLogin }) => {
    await pageWithPartialLogin.goto('/store-selection');
    await pageWithPartialLogin.waitForLoadState('networkidle');

    // 驗證「全區」選項存在
    await expect(pageWithPartialLogin.getByText('全區')).toBeVisible();
  });

  test('should enable confirm button when store selected', async ({ pageWithPartialLogin }) => {
    await pageWithPartialLogin.goto('/store-selection');
    await pageWithPartialLogin.waitForLoadState('networkidle');

    // 選擇一個店別
    await pageWithPartialLogin.getByText('台北門市').click();

    // 驗證確認按鈕可用
    const confirmButton = pageWithPartialLogin.getByRole('button', { name: /確認|確定|選擇/i });
    await expect(confirmButton).toBeEnabled();
  });
});
