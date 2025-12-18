import { test, expect } from '../../fixtures/mock-auth.fixture';

test.describe('Channel Selection', () => {
  test('should display channel selection page', async ({ pageWithStoreSelected }) => {
    // 驗證頁面標題
    await expect(pageWithStoreSelected.getByText('選擇系統別')).toBeVisible();
  });

  test('should display available channels', async ({ pageWithStoreSelected }) => {
    // 等待頁面載入完成
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 驗證系統別卡片存在
    const channelCards = pageWithStoreSelected.locator('.channel-card');
    await expect(channelCards.first()).toBeVisible();
  });

  test('should be able to select a channel', async ({ pageWithStoreSelected }) => {
    // 等待頁面載入完成
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 點擊第一個系統別卡片
    const channelCard = pageWithStoreSelected.locator('.channel-card').first();
    await channelCard.click();

    // 驗證卡片被選中 (有 selected class)
    await expect(channelCard).toHaveClass(/selected/);
  });

  test('should navigate to home after channel selection', async ({ pageWithStoreSelected }) => {
    // 等待頁面載入完成
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 點擊第一個系統別卡片
    const channelCard = pageWithStoreSelected.locator('.channel-card').first();
    await channelCard.click();

    // 點擊進入系統按鈕
    const enterButton = pageWithStoreSelected.getByRole('button', { name: /進入系統/i });
    await enterButton.click();

    // 等待導航到首頁
    await pageWithStoreSelected.waitForURL('**/home**', { timeout: 10000 });
    await expect(pageWithStoreSelected).toHaveURL(/home/);
  });

  test('should have enter system button disabled initially', async ({ pageWithStoreSelected }) => {
    // 等待頁面載入完成
    await pageWithStoreSelected.waitForLoadState('networkidle');

    // 驗證「進入系統」按鈕存在
    const enterButton = pageWithStoreSelected.getByRole('button', { name: /進入系統/i });
    await expect(enterButton).toBeVisible();

    // 在選擇系統別前，按鈕應該是禁用的
    await expect(enterButton).toBeDisabled();
  });
});
