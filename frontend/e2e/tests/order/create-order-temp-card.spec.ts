import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T058a: Temp Card Order Creation E2E Test
 *
 * 測試流程：查無會員 → 使用臨時卡 → 輸入資料 → 新增商品 → 提交訂單
 */
test.describe('Create Order - Temp Card Flow', () => {
  // Mock API 回應設定
  const mockTempMember = {
    memberId: 'TEMP20240101001',
    discType: '0',
    name: '臨時客戶',
    cellPhone: '0987654321',
    zipCode: '110',
    address: '台北市信義區臨時路99號',
    discTypeName: '折價 (Discounting)',
    discRate: 1.0,
    specialRate: null,
    isTempCard: true,
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
    orderId: 'SO20240101002',
    projectId: 'PJ240101000002',
    status: '1',
    statusName: '草稿',
    memberId: 'TEMP20240101001',
    memberName: '臨時客戶',
    channelId: 'A',
    storeId: '001',
    lines: [],
    calculation: null,
    createdAt: new Date().toISOString(),
    createdBy: 'K00123',
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
  };

  const mockCalculation = {
    orderId: 'SO20240101002',
    productTotal: 1000,
    installationTotal: 0,
    deliveryTotal: 100,
    memberDiscount: 0,
    directShipmentTotal: 0,
    couponDiscount: 0,
    grandTotal: 1100,
    promotionSkipped: false,
    memberDiscounts: [],
  };

  test.beforeEach(async ({ authenticatedPage }) => {
    // 設定 Mock API 路由
    await authenticatedPage.route('**/api/v1/members/*', (route) => {
      const url = route.request().url();
      // 模擬查無會員的情況
      if (url.includes('NOTEXIST') || url.includes('temp')) {
        route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error: '查無會員' }),
        });
      } else {
        route.fulfill({
          status: 404,
          contentType: 'application/json',
          body: JSON.stringify({ error: '查無會員' }),
        });
      }
    });

    await authenticatedPage.route('**/api/v1/members/temp', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(mockTempMember),
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

  test('should show temp card option when member not found', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 輸入不存在的會員卡號
    const memberInput = authenticatedPage.getByPlaceholder(/請輸入會員卡號/i);
    await memberInput.fill('NOTEXIST');

    // 點擊查詢
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證顯示使用臨時卡選項
    await expect(authenticatedPage.getByRole('button', { name: /使用臨時卡/i })).toBeVisible();
  });

  test('should display temp card form when clicking temp card button', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 直接點擊使用臨時卡按鈕
    const tempCardButton = authenticatedPage.getByRole('button', { name: /使用臨時卡/i });
    await tempCardButton.click();
    await authenticatedPage.waitForTimeout(300);

    // 驗證臨時卡表單顯示
    await expect(authenticatedPage.getByText(/建立臨時卡/i)).toBeVisible();
    await expect(authenticatedPage.getByLabel(/姓名/i)).toBeVisible();
    await expect(authenticatedPage.getByLabel(/手機號碼/i)).toBeVisible();
    await expect(authenticatedPage.getByLabel(/郵遞區號/i)).toBeVisible();
    await expect(authenticatedPage.getByLabel(/地址/i)).toBeVisible();
  });

  test('should create temp card with valid data', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 點擊使用臨時卡
    await authenticatedPage.getByRole('button', { name: /使用臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(300);

    // 填入臨時卡資料
    await authenticatedPage.getByLabel(/姓名/i).fill('臨時客戶');
    await authenticatedPage.getByLabel(/手機號碼/i).fill('0987654321');
    await authenticatedPage.getByLabel(/郵遞區號/i).fill('110');
    await authenticatedPage.getByLabel(/地址/i).fill('台北市信義區臨時路99號');

    // 點擊建立臨時卡
    await authenticatedPage.getByRole('button', { name: /建立臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證臨時卡建立成功（顯示臨時卡標籤）
    await expect(authenticatedPage.getByText('臨時卡')).toBeVisible();
    await expect(authenticatedPage.getByText('臨時客戶')).toBeVisible();
  });

  test('should validate temp card form fields', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 點擊使用臨時卡
    await authenticatedPage.getByRole('button', { name: /使用臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(300);

    // 驗證建立按鈕在未填寫時是禁用的
    const createButton = authenticatedPage.getByRole('button', { name: /建立臨時卡/i });
    await expect(createButton).toBeDisabled();

    // 填入部分資料
    await authenticatedPage.getByLabel(/姓名/i).fill('測試');
    await expect(createButton).toBeDisabled();

    // 填入無效手機號碼
    await authenticatedPage.getByLabel(/手機號碼/i).fill('123');
    await expect(createButton).toBeDisabled();

    // 填入有效手機號碼
    await authenticatedPage.getByLabel(/手機號碼/i).clear();
    await authenticatedPage.getByLabel(/手機號碼/i).fill('0912345678');

    // 填入郵遞區號和地址
    await authenticatedPage.getByLabel(/郵遞區號/i).fill('100');
    await authenticatedPage.getByLabel(/地址/i).fill('台北市中正區測試路');

    // 驗證按鈕變為可用
    await expect(createButton).toBeEnabled();
  });

  test('should return to search form when clicking back button', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 點擊使用臨時卡
    await authenticatedPage.getByRole('button', { name: /使用臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(300);

    // 驗證在臨時卡表單
    await expect(authenticatedPage.getByText(/建立臨時卡/i)).toBeVisible();

    // 點擊返回查詢
    await authenticatedPage.getByRole('button', { name: /返回查詢/i }).click();
    await authenticatedPage.waitForTimeout(300);

    // 驗證返回查詢表單
    await expect(authenticatedPage.getByPlaceholder(/請輸入會員卡號/i)).toBeVisible();
    await expect(authenticatedPage.getByRole('button', { name: /查詢會員/i })).toBeVisible();
  });

  test('should complete full order flow with temp card', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // Step 1: 建立臨時卡
    await authenticatedPage.getByRole('button', { name: /使用臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(300);

    await authenticatedPage.getByLabel(/姓名/i).fill('臨時客戶');
    await authenticatedPage.getByLabel(/手機號碼/i).fill('0987654321');
    await authenticatedPage.getByLabel(/郵遞區號/i).fill('110');
    await authenticatedPage.getByLabel(/地址/i).fill('台北市信義區臨時路99號');

    await authenticatedPage.getByRole('button', { name: /建立臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證臨時卡建立成功
    await expect(authenticatedPage.getByText('臨時卡')).toBeVisible();

    // Step 2: 新增商品
    await expect(authenticatedPage.getByRole('heading', { name: /商品列表/i })).toBeVisible();

    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證商品已新增
    await expect(authenticatedPage.getByText('測試商品A')).toBeVisible();

    // Step 3: 試算
    await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證試算結果
    await expect(authenticatedPage.getByText(/商品小計/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/應付總額/i)).toBeVisible();

    // Step 4: 提交訂單
    await authenticatedPage.getByRole('button', { name: /提交訂單/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證提交成功
    await expect(
      authenticatedPage.getByText(/提交成功|有效|訂單已提交/i)
    ).toBeVisible();
  });

  test('should show temp card badge after creation', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 建立臨時卡
    await authenticatedPage.getByRole('button', { name: /使用臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(300);

    await authenticatedPage.getByLabel(/姓名/i).fill('臨時客戶');
    await authenticatedPage.getByLabel(/手機號碼/i).fill('0987654321');
    await authenticatedPage.getByLabel(/郵遞區號/i).fill('110');
    await authenticatedPage.getByLabel(/地址/i).fill('台北市信義區臨時路99號');

    await authenticatedPage.getByRole('button', { name: /建立臨時卡/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證臨時卡標籤樣式
    const tempBadge = authenticatedPage.locator('.badge.temp, [class*="temp"]');
    await expect(tempBadge).toBeVisible();
  });
});
