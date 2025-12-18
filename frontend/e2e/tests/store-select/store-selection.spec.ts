import { test, expect } from '../../fixtures/mock-auth.fixture';

test.describe('Store Selection', () => {
  test('should display store selection page', async ({ pageWithPartialLogin }) => {
    // 驗證頁面標題
    await expect(pageWithPartialLogin.getByText('選擇店別')).toBeVisible();
  });

  test('should display master store info', async ({ pageWithPartialLogin }) => {
    // 等待頁面載入完成
    await pageWithPartialLogin.waitForLoadState('networkidle');

    // 驗證主店別標籤存在
    await expect(pageWithPartialLogin.getByText('主店別')).toBeVisible();

    // 主店別可能顯示「全區」或實際店名
    const storeDisplay = pageWithPartialLogin.locator('.store-display');
    await expect(storeDisplay).toBeVisible();
  });

  test('should have next button', async ({ pageWithPartialLogin }) => {
    // 驗證「下一步」按鈕存在
    const nextButton = pageWithPartialLogin.getByRole('button', { name: /下一步/i });
    await expect(nextButton).toBeVisible();
  });

  test('should navigate to channel selection when clicking next', async ({ pageWithPartialLogin }) => {
    // 點擊下一步
    const nextButton = pageWithPartialLogin.getByRole('button', { name: /下一步/i });
    await nextButton.click();

    // 等待導航到系統別選擇頁
    await pageWithPartialLogin.waitForURL('**/channel-selection**', { timeout: 10000 });
    await expect(pageWithPartialLogin).toHaveURL(/channel-selection/);
  });
});
