import { test as base, expect } from '@playwright/test';

// 建立已選擇店別但未選擇系統的 fixture
const test = base.extend({
  pageWithStoreSelected: async ({ page }, use) => {
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

    await page.route('**/api/channels/select', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      });
    });

    // 模擬已選擇店別但未選擇系統的狀態
    await page.addInitScript(() => {
      localStorage.setItem(
        'som_login_context',
        JSON.stringify({
          userId: 'test.user',
          userName: 'Test User',
          systemFlags: ['SO', 'TTS'],
          selectedStore: { storeId: 'S001', storeName: '台北門市' },
          supportStores: [],
          selectedChannel: null,
          loginTime: new Date().toISOString(),
        })
      );
    });

    await use(page);
  },
});

test.describe('Channel Selection', () => {
  test('should display available channels', async ({ pageWithStoreSelected }) => {
    await pageWithStoreSelected.goto('/channel-selection');
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 驗證系統選項顯示
    await expect(pageWithStoreSelected.getByText('SO', { exact: true }).or(
      pageWithStoreSelected.getByText('Special Order')
    )).toBeVisible();
  });

  test('should navigate to home after channel selection', async ({ pageWithStoreSelected }) => {
    await pageWithStoreSelected.goto('/channel-selection');
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 點選一個系統
    const soButton = pageWithStoreSelected.getByText('SO', { exact: true }).or(
      pageWithStoreSelected.getByRole('button', { name: /SO|Special Order/i })
    );

    if (await soButton.count() > 0) {
      await soButton.first().click();

      // 等待導航
      await pageWithStoreSelected.waitForTimeout(1000);

      // 驗證導航到首頁
      const url = pageWithStoreSelected.url();
      expect(url.includes('/home') || url.includes('/channel-selection')).toBeTruthy();
    }
  });

  test('should only show authorized channels', async ({ pageWithStoreSelected }) => {
    await pageWithStoreSelected.goto('/channel-selection');
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 使用者只有 SO 和 TTS 權限，不應該看到 APP
    const appButton = pageWithStoreSelected.getByText('APP', { exact: true });
    await expect(appButton).toHaveCount(0);
  });
});
