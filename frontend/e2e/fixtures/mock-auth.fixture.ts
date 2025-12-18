import { test as base, Page } from '@playwright/test';

/**
 * Keycloak 認證 Fixture
 * 使用真實的 Keycloak 進行登入
 */

// 測試帳號配置
const TEST_USER = {
  username: 'H00199',
  password: 'Aa123456',
};

/**
 * 執行 Keycloak 登入流程
 * 從 /login 開始，處理 Keycloak 重定向並完成登入
 */
async function performKeycloakLogin(page: Page): Promise<void> {
  // 導航到登入頁，等待 Keycloak 重定向
  await page.goto('/login');

  // 等待頁面穩定 - 可能是 Keycloak 登入頁或已經登入後的頁面
  await page.waitForLoadState('networkidle');

  // 檢查是否在 Keycloak 登入頁面
  const url = page.url();
  if (url.includes('localhost:8180') || url.includes('realms/som')) {
    // 等待 Keycloak 登入表單載入
    await page.waitForSelector('#username', { timeout: 10000 });

    // 填入帳號密碼
    await page.fill('#username', TEST_USER.username);
    await page.fill('#password', TEST_USER.password);

    // 點擊登入按鈕
    await page.click('#kc-login');

    // 等待重定向回應用程式並完成驗證
    await page.waitForURL('**/store-selection**', { timeout: 20000 });
  }
}

/**
 * 完整認證流程的 fixture
 * 登入 -> 驗證 -> 店別選擇 -> 系統別選擇 -> 首頁
 */
export const test = base.extend<{
  authenticatedPage: Page;
  pageWithPartialLogin: Page;
  pageWithStoreSelected: Page;
}>({
  // 完整認證的頁面 (已到達首頁)
  authenticatedPage: async ({ page }, use) => {
    await performKeycloakLogin(page);

    // 確認在店別選擇頁
    await page.waitForURL('**/store-selection**', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // 點擊下一步 (店別選擇) - 等待按鈕可點擊
    const nextButton = page.getByRole('button', { name: /下一步|確認|確定/i });
    await nextButton.waitFor({ state: 'visible', timeout: 5000 });
    await nextButton.click();

    // 等待系統別選擇頁面
    await page.waitForURL('**/channel-selection**', { timeout: 10000 });
    await page.waitForLoadState('networkidle');

    // 選擇第一個系統別 - 等待卡片出現
    const channelCard = page.locator('.channel-card, [class*="channel"]').first();
    await channelCard.waitFor({ state: 'visible', timeout: 5000 });
    await channelCard.click();

    // 點擊進入系統
    const enterButton = page.getByRole('button', { name: /進入系統|確認|確定/i });
    await enterButton.waitFor({ state: 'visible', timeout: 5000 });
    await enterButton.click();

    // 等待到達首頁
    await page.waitForURL('**/home**', { timeout: 10000 });
    await page.waitForLoadState('networkidle');

    await use(page);
  },

  // 部分登入的頁面 (停留在店別選擇)
  pageWithPartialLogin: async ({ page }, use) => {
    await performKeycloakLogin(page);

    // 確認在店別選擇頁
    await page.waitForURL('**/store-selection**', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    await use(page);
  },

  // 已選擇店別的頁面 (停留在系統別選擇)
  pageWithStoreSelected: async ({ page }, use) => {
    await performKeycloakLogin(page);

    // 確認在店別選擇頁
    await page.waitForURL('**/store-selection**', { timeout: 15000 });
    await page.waitForLoadState('networkidle');

    // 點擊下一步 - 等待按鈕可點擊
    const nextButton = page.getByRole('button', { name: /下一步|確認|確定/i });
    await nextButton.waitFor({ state: 'visible', timeout: 5000 });
    await nextButton.click();

    // 等待系統別選擇頁面
    await page.waitForURL('**/channel-selection**', { timeout: 10000 });
    await page.waitForLoadState('networkidle');

    await use(page);
  },
});

export { expect } from '@playwright/test';
