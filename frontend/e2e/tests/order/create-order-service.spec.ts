import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T075/T075a: Order Service Configuration E2E Test
 *
 * 測試流程：
 * - T075: 選擇運送方式 → 新增安裝服務 → 驗證費用
 * - T075a: EC-008 運送/備貨方式相容性驗證
 */
test.describe('Create Order - Service Configuration', () => {
  // Mock API 回應設定
  const mockMember = {
    memberId: 'H00199',
    discType: '0',
    name: 'SIT測試人員',
    cellPhone: '0912345678',
    zipCode: '100',
    address: '台北市中正區測試路1號',
    discTypeName: '折價 (Discounting)',
    discRate: 0.95,
    specialRate: null,
    isTempCard: false,
  };

  const mockEligibility = {
    eligible: true,
    skuNo: '014014014',
    failureLevel: null,
    failureReason: null,
    product: {
      skuNo: '014014014',
      skuName: '測試商品A',
      unitPrice: 1000,
      posPrice: 1000,
      taxType: '1',
      taxTypeName: '應稅',
      deliveryMethod: 'N',
      deliveryMethodName: '運送',
      stockMethod: 'X',
      stockMethodName: '現貨',
      hasInstallation: true,
    },
  };

  const mockOrder = {
    orderId: 'SO20240101001',
    projectId: 'PJ240101000001',
    status: '1',
    statusName: '草稿',
    memberId: 'H00199',
    memberName: 'SIT測試人員',
    channelId: 'A',
    storeId: '001',
    lines: [],
    calculation: null,
    createdAt: new Date().toISOString(),
    createdBy: 'H00199',
  };

  const mockOrderLine = {
    lineId: 'LINE001',
    serialNo: 1,
    skuNo: '014014014',
    skuName: '測試商品A',
    quantity: 1,
    unitPrice: 1000,
    actualUnitPrice: 1000,
    subtotal: 1000,
    taxType: '1',
    taxTypeName: '應稅',
    deliveryMethod: 'N',
    deliveryMethodName: '運送',
    stockMethod: 'X',
    stockMethodName: '現貨',
    workTypeId: null,
    workTypeName: null,
    serviceTypes: [],
    hasInstallation: false,
    installationCost: 0,
    deliveryCost: 0,
  };

  const mockWorkTypes = [
    {
      workTypeId: 'WT001',
      workTypeName: '標準安裝',
      category: 'INSTALLATION',
      minimumWage: 500,
      basicDiscount: 0.9,
    },
    {
      workTypeId: 'WT002',
      workTypeName: '宅配運送',
      category: 'HOME_DELIVERY',
      minimumWage: 300,
      basicDiscount: 1.0,
    },
  ];

  const mockInstallationServices = [
    {
      serviceType: 'BASIC_INSTALL',
      serviceName: '基本安裝',
      basePrice: 300,
      isMandatory: false,
      isExtraInstallation: false,
    },
    {
      serviceType: 'ADVANCED_INSTALL',
      serviceName: '進階安裝',
      basePrice: 500,
      isMandatory: false,
      isExtraInstallation: true,
    },
  ];

  test.beforeEach(async ({ authenticatedPage }) => {
    // 設定 Mock API 路由
    await authenticatedPage.route('**/api/v1/members/*', (route) => {
      const url = route.request().url();
      if (url.includes('H00199')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMember),
        });
      } else {
        route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error: '查無會員' }),
        });
      }
    });

    await authenticatedPage.route('**/api/v1/products/*/eligibility*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockEligibility),
      });
    });

    await authenticatedPage.route('**/api/v1/orders', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(mockOrder),
        });
      }
    });

    await authenticatedPage.route('**/api/v1/orders/*/lines', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(mockOrderLine),
        });
      }
    });

    await authenticatedPage.route('**/api/v1/orders/*/lines/*', (route) => {
      if (route.request().method() === 'PUT') {
        const requestBody = route.request().postDataJSON();
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            ...mockOrderLine,
            deliveryMethod: requestBody?.deliveryMethod || mockOrderLine.deliveryMethod,
            stockMethod: requestBody?.stockMethod || mockOrderLine.stockMethod,
          }),
        });
      }
    });

    await authenticatedPage.route('**/api/v1/orders/*/lines/*/services', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockInstallationServices),
      });
    });

    await authenticatedPage.route('**/api/v1/worktypes*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockWorkTypes),
      });
    });

    await authenticatedPage.route('**/api/v1/orders/*/lines/*/installation', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...mockOrderLine,
          hasInstallation: true,
          installationCost: 300,
          workTypeId: 'WT001',
          workTypeName: '標準安裝',
        }),
      });
    });

    await authenticatedPage.route('**/api/v1/orders/*/lines/*/delivery', (route) => {
      const requestBody = route.request().postDataJSON();
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...mockOrderLine,
          deliveryMethod: requestBody?.deliveryMethod || mockOrderLine.deliveryMethod,
          stockMethod: requestBody?.stockMethod || mockOrderLine.stockMethod,
        }),
      });
    });
  });

  /**
   * 輔助函數：完成會員查詢和商品新增
   */
  async function setupOrderWithProduct(authenticatedPage: any) {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 查詢會員
    await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00199');
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 新增商品
    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);
  }

  /**
   * 輔助函數：開啟服務設定 Modal
   */
  async function openServiceConfigModal(authenticatedPage: any) {
    // 點擊服務設定按鈕
    const configButton = authenticatedPage.getByRole('button', { name: /服務設定|設定服務/i });
    await configButton.click();
    await authenticatedPage.waitForTimeout(300);
  }

  // ============================================
  // T075: 基本服務設定測試
  // ============================================

  test('should display service config modal', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);
    await openServiceConfigModal(authenticatedPage);

    // 驗證 Modal 顯示
    await expect(authenticatedPage.getByRole('heading', { name: /服務設定/i })).toBeVisible();
    await expect(authenticatedPage.getByText(/安裝服務/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/運送方式/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/備貨方式/i)).toBeVisible();
  });

  test('should select delivery method', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);
    await openServiceConfigModal(authenticatedPage);

    // 選擇運送方式下拉選單
    const deliverySelect = authenticatedPage.locator('select').filter({ has: authenticatedPage.locator('option:has-text("運送")') }).first();
    await expect(deliverySelect).toBeVisible();

    // 選擇宅配
    await deliverySelect.selectOption('F');
    await authenticatedPage.waitForTimeout(200);

    // 驗證宅配被選中
    await expect(deliverySelect).toHaveValue('F');
  });

  test('should select installation work type', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);
    await openServiceConfigModal(authenticatedPage);

    // 等待載入完成
    await authenticatedPage.waitForTimeout(500);

    // 選擇工種
    const workTypeSelect = authenticatedPage.locator('select').filter({ has: authenticatedPage.locator('option:has-text("請選擇")') }).first();
    await workTypeSelect.selectOption('WT001');
    await authenticatedPage.waitForTimeout(200);

    // 驗證工種被選中
    await expect(workTypeSelect).toHaveValue('WT001');
  });

  // ============================================
  // T075a: EC-008 運送/備貨方式相容性測試
  // ============================================

  test.describe('EC-008: Delivery/Stock Compatibility', () => {
    test('直送(V) should auto-set 訂購(Y)', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();
      await expect(deliverySelect).toBeVisible();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();
      await expect(stockSelect).toBeVisible();

      // 選擇直送(V)
      await deliverySelect.selectOption('V');
      await authenticatedPage.waitForTimeout(300);

      // 驗證備貨方式自動切換為訂購(Y)
      await expect(stockSelect).toHaveValue('Y');

      // 驗證沒有 Toast 提示（自動預設不顯示 Toast）
      const toastContainer = authenticatedPage.locator('.toast-container');
      await expect(toastContainer.locator('.toast')).toHaveCount(0);
    });

    test('當場自取(C) should auto-set 現貨(X)', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();
      await expect(deliverySelect).toBeVisible();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();
      await expect(stockSelect).toBeVisible();

      // 選擇當場自取(C)
      await deliverySelect.selectOption('C');
      await authenticatedPage.waitForTimeout(300);

      // 驗證備貨方式自動切換為現貨(X)
      await expect(stockSelect).toHaveValue('X');

      // 驗證沒有 Toast 提示（自動預設不顯示 Toast）
      const toastContainer = authenticatedPage.locator('.toast-container');
      await expect(toastContainer.locator('.toast')).toHaveCount(0);
    });

    test('直送(V) + 手動改為現貨(X) should show Toast and auto-correct to 訂購(Y)', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();
      await expect(deliverySelect).toBeVisible();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();
      await expect(stockSelect).toBeVisible();

      // 先選擇直送(V)
      await deliverySelect.selectOption('V');
      await authenticatedPage.waitForTimeout(300);

      // 確認備貨方式已自動設為訂購(Y)
      await expect(stockSelect).toHaveValue('Y');

      // 手動嘗試改為現貨(X)
      await stockSelect.selectOption('X');
      await authenticatedPage.waitForTimeout(500);

      // 驗證 Toast 提示出現
      const toast = authenticatedPage.locator('.toast-warning');
      await expect(toast).toBeVisible();
      await expect(toast).toContainText('直送只能訂購');

      // 驗證備貨方式自動更正回訂購(Y)
      await expect(stockSelect).toHaveValue('Y');
    });

    test('當場自取(C) + 手動改為訂購(Y) should show Toast and auto-correct to 現貨(X)', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();
      await expect(deliverySelect).toBeVisible();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();
      await expect(stockSelect).toBeVisible();

      // 先選擇當場自取(C)
      await deliverySelect.selectOption('C');
      await authenticatedPage.waitForTimeout(300);

      // 確認備貨方式已自動設為現貨(X)
      await expect(stockSelect).toHaveValue('X');

      // 手動嘗試改為訂購(Y)
      await stockSelect.selectOption('Y');
      await authenticatedPage.waitForTimeout(500);

      // 驗證 Toast 提示出現
      const toast = authenticatedPage.locator('.toast-warning');
      await expect(toast).toBeVisible();
      await expect(toast).toContainText('當場自取只能現貨');

      // 驗證備貨方式自動更正回現貨(X)
      await expect(stockSelect).toHaveValue('X');
    });

    test('Toast should auto-dismiss after 3 seconds', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();

      // 觸發 Toast（直送 + 手動改現貨）
      await deliverySelect.selectOption('V');
      await authenticatedPage.waitForTimeout(300);
      await stockSelect.selectOption('X');
      await authenticatedPage.waitForTimeout(200);

      // 驗證 Toast 出現
      const toast = authenticatedPage.locator('.toast-warning');
      await expect(toast).toBeVisible();

      // 等待 Toast 自動消失 (3秒 + buffer)
      await authenticatedPage.waitForTimeout(3500);

      // 驗證 Toast 已消失
      await expect(toast).not.toBeVisible();
    });

    test('Toast can be manually dismissed', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();

      // 觸發 Toast
      await deliverySelect.selectOption('V');
      await authenticatedPage.waitForTimeout(300);
      await stockSelect.selectOption('X');
      await authenticatedPage.waitForTimeout(200);

      // 驗證 Toast 出現
      const toast = authenticatedPage.locator('.toast-warning');
      await expect(toast).toBeVisible();

      // 點擊關閉按鈕
      const closeButton = toast.locator('.toast-close');
      await closeButton.click();
      await authenticatedPage.waitForTimeout(200);

      // 驗證 Toast 已消失
      await expect(toast).not.toBeVisible();
    });

    test('other delivery methods should not restrict stock method', async ({ authenticatedPage }) => {
      await setupOrderWithProduct(authenticatedPage);
      await openServiceConfigModal(authenticatedPage);

      // 找到運送方式下拉選單
      const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();

      // 找到備貨方式下拉選單
      const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();

      // 選擇一般運送(N)
      await deliverySelect.selectOption('N');
      await authenticatedPage.waitForTimeout(300);

      // 手動選擇訂購(Y) - 應該可以正常選擇，不會被更正
      await stockSelect.selectOption('Y');
      await authenticatedPage.waitForTimeout(300);
      await expect(stockSelect).toHaveValue('Y');

      // 手動選擇現貨(X) - 應該可以正常選擇，不會被更正
      await stockSelect.selectOption('X');
      await authenticatedPage.waitForTimeout(300);
      await expect(stockSelect).toHaveValue('X');

      // 驗證沒有 Toast 出現
      const toastContainer = authenticatedPage.locator('.toast-container');
      await expect(toastContainer.locator('.toast')).toHaveCount(0);
    });
  });

  // ============================================
  // 服務設定儲存測試
  // ============================================

  test('should save service configuration', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);
    await openServiceConfigModal(authenticatedPage);

    // 等待載入完成
    await authenticatedPage.waitForTimeout(500);

    // 選擇運送方式
    const deliverySelect = authenticatedPage.locator('.config-section').filter({ hasText: '運送方式' }).locator('select').first();
    await deliverySelect.selectOption('N');

    // 選擇備貨方式
    const stockSelect = authenticatedPage.locator('.config-section').filter({ hasText: '備貨方式' }).locator('select').first();
    await stockSelect.selectOption('X');

    // 點擊儲存
    const saveButton = authenticatedPage.getByRole('button', { name: /儲存/i });
    await saveButton.click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證 Modal 關閉
    await expect(authenticatedPage.locator('.service-config-modal')).not.toBeVisible();
  });
});
