import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T058: Basic Order Creation E2E Test
 *
 * 測試流程：輸入會員 → 新增商品 → 試算 → 提交
 */
test.describe('Create Order - Basic Flow', () => {
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
      taxType: '1',
      taxTypeName: '應稅',
      deliveryMethod: 'N',
      deliveryMethodName: '運送',
      stockMethod: 'X',
      stockMethodName: '現貨',
      hasInstallation: false,
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
    quantity: 2,
    unitPrice: 1000,
    actualUnitPrice: 1000,
    subtotal: 2000,
    taxType: '1',
    taxTypeName: '應稅',
    deliveryMethod: 'N',
    deliveryMethodName: '運送',
    stockMethod: 'X',
    stockMethodName: '現貨',
  };

  const mockCalculation = {
    orderId: 'SO20240101001',
    productTotal: 2000,
    installationTotal: 0,
    deliveryTotal: 100,
    memberDiscount: -100,
    directShipmentTotal: 0,
    couponDiscount: 0,
    grandTotal: 2000,
    promotionSkipped: false,
    memberDiscounts: [
      {
        lineId: 'LINE001',
        originalPrice: 2000,
        discountedPrice: 1900,
        discountAmount: 100,
        discountRate: 0.95,
      },
    ],
  };

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

    await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCalculation),
      });
    });

    await authenticatedPage.route('**/api/v1/orders/*/submit', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...mockOrder,
          status: '4',
          statusName: '有效',
        }),
      });
    });
  });

  test('should navigate to create order page', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 驗證頁面標題
    await expect(authenticatedPage.getByRole('heading', { name: /新增訂單/i })).toBeVisible();

    // 驗證會員資訊區塊存在
    await expect(authenticatedPage.getByRole('heading', { name: /會員資訊/i })).toBeVisible();
  });

  test('should lookup member by card number', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 輸入會員卡號
    const memberInput = authenticatedPage.getByPlaceholder(/請輸入會員卡號/i);
    await memberInput.fill('H00199');

    // 點擊查詢
    const searchButton = authenticatedPage.getByRole('button', { name: /查詢會員/i });
    await searchButton.click();

    // 等待會員資料顯示
    await authenticatedPage.waitForTimeout(500);

    // 驗證會員資料顯示
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();
    await expect(authenticatedPage.getByText('會員', { exact: true })).toBeVisible();
  });

  test('should show error for non-existent member', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 輸入不存在的會員卡號
    const memberInput = authenticatedPage.getByPlaceholder(/請輸入會員卡號/i);
    await memberInput.fill('NOTEXIST');

    // 點擊查詢
    const searchButton = authenticatedPage.getByRole('button', { name: /查詢會員/i });
    await searchButton.click();

    // 等待錯誤訊息
    await authenticatedPage.waitForTimeout(500);

    // 驗證顯示查無會員的提示
    await expect(authenticatedPage.getByText(/查無會員.*NOTEXIST/i)).toBeVisible();
  });

  test('should add product to order after member lookup', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 先查詢會員
    const memberInput = authenticatedPage.getByPlaceholder(/請輸入會員卡號/i);
    await memberInput.fill('H00199');
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證商品列表區塊出現
    await expect(authenticatedPage.getByRole('heading', { name: /商品列表/i })).toBeVisible();

    // 輸入商品編號
    const skuInput = authenticatedPage.getByPlaceholder(/請輸入商品編號/i);
    await skuInput.fill('014014014');

    // 設定數量
    const qtyInput = authenticatedPage.locator('#qty');
    await qtyInput.clear();
    await qtyInput.fill('2');

    // 點擊新增商品
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證商品已新增到列表
    await expect(authenticatedPage.getByRole('cell', { name: '測試商品A' })).toBeVisible();
    await expect(authenticatedPage.getByRole('cell', { name: '014014014' })).toBeVisible();
  });

  test('should calculate order total', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 完成會員查詢與商品新增
    await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00199');
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 點擊試算
    const calculateButton = authenticatedPage.getByRole('button', { name: /執行試算/i });
    await calculateButton.click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證訂單摘要顯示
    await expect(authenticatedPage.locator('.order-summary').getByText(/商品小計/i)).toBeVisible();
    await expect(authenticatedPage.locator('.order-summary').getByText(/應付總額/i)).toBeVisible();
  });

  test('should submit order successfully', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 完成完整流程：會員查詢 → 新增商品 → 試算
    await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00199');
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);

    await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 提交訂單
    const submitButton = authenticatedPage.getByRole('button', { name: /提交訂單/i });
    await submitButton.click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證成功訊息
    await expect(
      authenticatedPage.getByText(/訂單.*已提交成功/i)
    ).toBeVisible();
  });

  test('should display order summary section', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 完成會員查詢與商品新增
    await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00199');
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證訂單摘要區塊存在
    await expect(authenticatedPage.getByRole('heading', { name: /訂單摘要/i })).toBeVisible();

    // 試算後驗證金額顯示
    await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證各項金額欄位 (在 order-summary 區塊內)
    const summary = authenticatedPage.locator('.order-summary');
    await expect(summary.getByText(/商品小計/i)).toBeVisible();
    await expect(summary.getByText(/應付總額/i)).toBeVisible();
  });
});
