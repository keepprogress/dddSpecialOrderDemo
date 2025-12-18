import { test, expect } from '../../fixtures/mock-auth.fixture';

test.describe('Navigation Bar', () => {
  test.beforeEach(async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/home');
  });

  test('should display all menu items', async ({ authenticatedPage }) => {
    // 驗證導航列選單項目
    await expect(authenticatedPage.getByText('訂單管理')).toBeVisible();
    await expect(authenticatedPage.getByText('退貨管理')).toBeVisible();
    await expect(authenticatedPage.getByText('安運單管理')).toBeVisible();
    await expect(authenticatedPage.getByText('主檔維護')).toBeVisible();
    await expect(authenticatedPage.getByText('報表')).toBeVisible();
  });

  test('should display user info', async ({ authenticatedPage }) => {
    // 驗證使用者資訊顯示
    await expect(authenticatedPage.getByText('Test User')).toBeVisible();
    await expect(authenticatedPage.getByText('台北門市')).toBeVisible();
  });

  test('should have logout button', async ({ authenticatedPage }) => {
    // 驗證登出按鈕存在
    const logoutButton = authenticatedPage.getByRole('button', { name: /登出/i });
    await expect(logoutButton).toBeVisible();
  });

  test('should have switch system button', async ({ authenticatedPage }) => {
    // 驗證切換系統按鈕存在
    const switchButton = authenticatedPage.getByRole('button', { name: /切換/i });
    await expect(switchButton).toBeVisible();
  });

  test('should navigate to orders page', async ({ authenticatedPage }) => {
    await authenticatedPage.getByText('訂單管理').click();
    await expect(authenticatedPage).toHaveURL(/.*\/orders/);
  });
});
