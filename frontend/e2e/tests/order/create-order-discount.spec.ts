import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T087: Member Discount E2E Test
 *
 * 測試流程：會員類型折扣計算
 * - Type 0 (Discounting) 折價計算
 * - Type 1 (Down Margin) 下降計算
 * - Type 2 (Cost Markup) 成本加成計算
 */
test.describe('Create Order - Member Discount', () => {
  // Type 0 會員 (折價 Discounting)
  const mockMemberType0 = {
    memberId: 'K00123',
    discType: '0',
    name: 'SIT測試人員',
    cellPhone: '0912345678',
    zipCode: '100',
    address: '台北市中正區測試路1號',
    discTypeName: '折價 (Discounting)',
    discRate: 0.95, // 95 折
    specialRate: null,
    isTempCard: false,
  };

  // Type 1 會員 (下降 Down Margin)
  const mockMemberType1 = {
    memberId: 'H00200',
    discType: '1',
    name: 'Type1測試人員',
    cellPhone: '0912345679',
    zipCode: '100',
    address: '台北市中正區測試路2號',
    discTypeName: '下降 (Down Margin)',
    discRate: null,
    specialRate: null,
    downMarginRate: 0.05, // 下降 5%
    isTempCard: false,
  };

  // Type 2 會員 (成本加成 Cost Markup)
  const mockMemberType2 = {
    memberId: 'H00201',
    discType: '2',
    name: 'Type2測試人員',
    cellPhone: '0912345680',
    zipCode: '100',
    address: '台北市中正區測試路3號',
    discTypeName: '成本加成 (Cost Markup)',
    discRate: null,
    specialRate: null,
    markupRate: 1.05, // 成本加成 5%
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
      cost: 800, // 成本價
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
    memberId: 'K00123',
    memberName: 'SIT測試人員',
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
    memberDisc: 0,
    bonusDisc: 0,
    couponDisc: 0,
  };

  // Type 0 會員的試算結果 (95折 = -50)
  const mockCalculationType0 = {
    orderId: 'SO20240101001',
    computeTypes: [
      { computeType: '1', computeName: '商品小計', totalPrice: 1000, discount: -50, actTotalPrice: 950 },
      { computeType: '2', computeName: '安裝小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '3', computeName: '運送小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '4', computeName: '會員卡折扣', totalPrice: 0, discount: -50, actTotalPrice: -50 },
      { computeType: '5', computeName: '直送費用小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '6', computeName: '折價券折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '7', computeName: '紅利點數折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
    ],
    memberDiscounts: [
      {
        skuNo: '014014014',
        discType: '0',
        discTypeName: '折價 (Discounting)',
        originalPrice: 1000,
        discountPrice: 950,
        discAmt: -50,
        discRate: 0.95,
        markupRate: null,
      },
    ],
    grandTotal: 950,
    taxAmount: 45,
    promotionSkipped: false,
    warnings: [],
    calculatedAt: new Date().toISOString(),
  };

  // Type 1 會員的試算結果 (下降 5% = -50)
  const mockCalculationType1 = {
    ...mockCalculationType0,
    computeTypes: [
      { computeType: '1', computeName: '商品小計', totalPrice: 1000, discount: -50, actTotalPrice: 950 },
      { computeType: '2', computeName: '安裝小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '3', computeName: '運送小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '4', computeName: '會員卡折扣', totalPrice: 0, discount: -50, actTotalPrice: -50 },
      { computeType: '5', computeName: '直送費用小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '6', computeName: '折價券折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '7', computeName: '紅利點數折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
    ],
    memberDiscounts: [
      {
        skuNo: '014014014',
        discType: '1',
        discTypeName: '下降 (Down Margin)',
        originalPrice: 1000,
        discountPrice: 950,
        discAmt: -50,
        discRate: null,
        markupRate: null,
      },
    ],
    grandTotal: 950,
  };

  // Type 2 會員的試算結果 (成本800 * 1.05 = 840，折扣 = -160)
  const mockCalculationType2 = {
    ...mockCalculationType0,
    computeTypes: [
      { computeType: '1', computeName: '商品小計', totalPrice: 1000, discount: -160, actTotalPrice: 840 },
      { computeType: '2', computeName: '安裝小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '3', computeName: '運送小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '4', computeName: '會員卡折扣', totalPrice: 0, discount: -160, actTotalPrice: -160 },
      { computeType: '5', computeName: '直送費用小計', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '6', computeName: '折價券折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
      { computeType: '7', computeName: '紅利點數折扣', totalPrice: 0, discount: 0, actTotalPrice: 0 },
    ],
    memberDiscounts: [
      {
        skuNo: '014014014',
        discType: '2',
        discTypeName: '成本加成 (Cost Markup)',
        originalPrice: 1000,
        discountPrice: 840,
        discAmt: -160,
        discRate: null,
        markupRate: 1.05,
      },
    ],
    grandTotal: 840,
    taxAmount: 40,
  };

  test.beforeEach(async ({ authenticatedPage }) => {
    // 設定 Mock API 路由
    await authenticatedPage.route('**/api/v1/members/*', (route) => {
      const url = route.request().url();
      if (url.includes('K00123')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMemberType0),
        });
      } else if (url.includes('H00200')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMemberType1),
        });
      } else if (url.includes('H00201')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMemberType2),
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
  });

  /**
   * 輔助函數：完成會員查詢和商品新增
   */
  async function setupOrderWithMemberAndProduct(authenticatedPage: any, memberId: string) {
    await authenticatedPage.goto('/orders/create');
    await authenticatedPage.waitForLoadState('networkidle');

    // 查詢會員
    await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill(memberId);
    await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 新增商品
    await authenticatedPage.getByPlaceholder(/請輸入商品編號/i).fill('014014014');
    await authenticatedPage.getByRole('button', { name: /新增商品/i }).click();
    await authenticatedPage.waitForTimeout(500);
  }

  // ============================================
  // Type 0: Discounting 折價測試
  // ============================================

  test.describe('Type 0: Discounting (折價)', () => {
    test('should display member type 0 info correctly', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/orders/create');
      await authenticatedPage.waitForLoadState('networkidle');

      // 查詢 Type 0 會員
      await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('K00123');
      await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證會員資料顯示
      await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();
      // 驗證顯示折扣類型
      await expect(authenticatedPage.getByText(/折價|Discounting/i)).toBeVisible();
    });

    test('should calculate Type 0 member discount correctly', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType0),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'K00123');

      // 點擊試算
      const calculateButton = authenticatedPage.getByRole('button', { name: /執行試算/i });
      await calculateButton.click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證試算結果
      const summary = authenticatedPage.locator('.order-summary, .price-calculation');

      // 驗證商品小計顯示
      await expect(summary.getByText(/商品小計/i)).toBeVisible();

      // 驗證會員折扣顯示
      await expect(summary.getByText(/會員.*折扣|會員卡折扣/i)).toBeVisible();

      // 驗證應付總額顯示
      await expect(summary.getByText(/應付總額|總計/i)).toBeVisible();
    });

    test('should show 95% discount for Type 0 member (discRate = 0.95)', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType0),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'K00123');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證折扣金額或折後價格
      // 原價 1000 * 0.95 = 950，折扣 = -50
      await expect(
        authenticatedPage.locator('.order-summary, .price-calculation').getByText(/950|50/)
      ).toBeVisible();
    });
  });

  // ============================================
  // Type 1: Down Margin 下降測試
  // ============================================

  test.describe('Type 1: Down Margin (下降)', () => {
    test('should display member type 1 info correctly', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/orders/create');
      await authenticatedPage.waitForLoadState('networkidle');

      // 查詢 Type 1 會員
      await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00200');
      await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證會員資料顯示
      await expect(authenticatedPage.getByText('Type1測試人員')).toBeVisible();
      // 驗證顯示折扣類型
      await expect(authenticatedPage.getByText(/下降|Down Margin/i)).toBeVisible();
    });

    test('should calculate Type 1 member discount correctly', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType1),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'H00200');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證試算結果
      const summary = authenticatedPage.locator('.order-summary, .price-calculation');
      await expect(summary.getByText(/會員.*折扣|會員卡折扣/i)).toBeVisible();
    });
  });

  // ============================================
  // Type 2: Cost Markup 成本加成測試
  // ============================================

  test.describe('Type 2: Cost Markup (成本加成)', () => {
    test('should display member type 2 info correctly', async ({ authenticatedPage }) => {
      await authenticatedPage.goto('/orders/create');
      await authenticatedPage.waitForLoadState('networkidle');

      // 查詢 Type 2 會員
      await authenticatedPage.getByPlaceholder(/請輸入會員卡號/i).fill('H00201');
      await authenticatedPage.getByRole('button', { name: /查詢會員/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證會員資料顯示
      await expect(authenticatedPage.getByText('Type2測試人員')).toBeVisible();
      // 驗證顯示折扣類型
      await expect(authenticatedPage.getByText(/成本加成|Cost Markup/i)).toBeVisible();
    });

    test('should calculate Type 2 member discount correctly', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType2),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'H00201');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證試算結果
      const summary = authenticatedPage.locator('.order-summary, .price-calculation');
      await expect(summary.getByText(/會員.*折扣|會員卡折扣/i)).toBeVisible();

      // 驗證成本加成計算結果
      // 成本 800 * 1.05 = 840，原價 1000，折扣 = -160
      await expect(
        authenticatedPage.locator('.order-summary, .price-calculation').getByText(/840|160/)
      ).toBeVisible();
    });
  });

  // ============================================
  // 折扣明細顯示測試
  // ============================================

  test.describe('Discount Details Display', () => {
    test('should display member discount details in calculation result', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType0),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'K00123');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證 ComputeType 顯示
      const summary = authenticatedPage.locator('.order-summary, .price-calculation');

      // 應該顯示所有試算類型
      await expect(summary.getByText(/商品小計/i)).toBeVisible();
      await expect(summary.getByText(/應付總額|總計/i)).toBeVisible();
    });

    test('should show correct grand total after member discount', async ({ authenticatedPage }) => {
      // 設定試算 API
      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculationType0),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'K00123');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證總額正確 (原價 1000，95 折 = 950)
      await expect(
        authenticatedPage.locator('.order-summary, .price-calculation, .grand-total')
      ).toContainText(/950/);
    });

    test('should handle multiple products with member discount', async ({ authenticatedPage }) => {
      // 設定試算 API 回傳多商品折扣
      const multiProductCalculation = {
        ...mockCalculationType0,
        computeTypes: [
          { computeType: '1', computeName: '商品小計', totalPrice: 2000, discount: -100, actTotalPrice: 1900 },
          ...mockCalculationType0.computeTypes.slice(1),
        ],
        memberDiscounts: [
          {
            skuNo: '014014014',
            discType: '0',
            discTypeName: '折價 (Discounting)',
            originalPrice: 1000,
            discountPrice: 950,
            discAmt: -50,
            discRate: 0.95,
            markupRate: null,
          },
          {
            skuNo: '014014015',
            discType: '0',
            discTypeName: '折價 (Discounting)',
            originalPrice: 1000,
            discountPrice: 950,
            discAmt: -50,
            discRate: 0.95,
            markupRate: null,
          },
        ],
        grandTotal: 1900,
      };

      await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(multiProductCalculation),
        });
      });

      await setupOrderWithMemberAndProduct(authenticatedPage, 'K00123');

      // 點擊試算
      await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
      await authenticatedPage.waitForTimeout(500);

      // 驗證試算結果顯示
      const summary = authenticatedPage.locator('.order-summary, .price-calculation');
      await expect(summary.getByText(/商品小計/i)).toBeVisible();
    });
  });
});
