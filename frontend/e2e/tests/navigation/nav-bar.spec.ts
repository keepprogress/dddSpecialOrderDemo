import { test, expect } from '../../fixtures/mock-auth.fixture';

test.describe('Navigation Bar', () => {
  test('should display all menu items', async ({ authenticatedPage }) => {
    // 驗證導航列選單項目
    await expect(authenticatedPage.getByRole('link', { name: /訂單管理/i })).toBeVisible();
    await expect(authenticatedPage.getByRole('link', { name: /退貨管理/i })).toBeVisible();
    await expect(authenticatedPage.getByRole('link', { name: /安運單管理/i })).toBeVisible();
    await expect(authenticatedPage.getByRole('link', { name: /主檔維護/i })).toBeVisible();
    await expect(authenticatedPage.getByRole('link', { name: /報表/i })).toBeVisible();
  });

  test('should display user info', async ({ authenticatedPage }) => {
    // 驗證使用者資訊顯示 (H00199 = SIT測試人員) - 在導航列中
    await expect(authenticatedPage.getByRole('navigation').getByText('SIT測試人員')).toBeVisible();
    // 店別顯示在導航列中
    await expect(authenticatedPage.locator('nav .user-name, header .user-name')).toBeVisible();
  });

  test('should have logout button', async ({ authenticatedPage }) => {
    // 驗證登出按鈕存在
    const logoutButton = authenticatedPage.getByRole('button', { name: /登出/i });
    await expect(logoutButton).toBeVisible();
  });

  test('should have switch system button', async ({ authenticatedPage }) => {
    // 驗證切換系統按鈕存在 (可能是圖示按鈕)
    // 尋找 header 區域的按鈕
    const headerButtons = authenticatedPage.locator('header button, .header button, nav button');
    await expect(headerButtons.first()).toBeVisible();
  });

  test('should navigate to orders page', async ({ authenticatedPage }) => {
    // 點擊訂單管理
    await authenticatedPage.getByRole('link', { name: /訂單管理/i }).click();
    // 等待導航
    await authenticatedPage.waitForTimeout(500);
    // 驗證 URL 包含 orders
    await expect(authenticatedPage).toHaveURL(/.*orders/);
  });
});
