import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T102: Coupon & Bonus E2E Test
 *
 * 測試流程：
 * - 優惠券套用與移除
 * - 紅利點數折抵與取消
 */
test.describe('Create Order - Coupon & Bonus', () => {
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
    cardType: 'VIP',
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
    memberDisc: 0,
    couponDisc: 0,
    bonusDisc: 0,
    installationCost: 0,
    deliveryCost: 100,
    taxType: '1',
    taxTypeName: '應稅',
    deliveryMethod: 'N',
    deliveryMethodName: '運送',
    stockMethod: 'X',
    stockMethodName: '現貨',
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

  const mockCouponValidation = {
    valid: true,
    failureReason: null,
    discountAmount: { amount: 200, currency: 'TWD' },
    applicableSkus: ['014014014'],
    freeInstallation: false,
  };

  const mockInvalidCoupon = {
    valid: false,
    failureReason: '優惠券已過期',
    discountAmount: null,
    applicableSkus: [],
    freeInstallation: false,
  };

  const mockBonusRedemption = {
    memberId: 'H00199',
    skuNo: '014014014',
    skuName: '測試商品A',
    pointsUsed: 100,
    discountAmount: { amount: 100, currency: 'TWD' },
    exchangeRate: 1.0,
    remainingPoints: 900,
  };

  const mockCalculation = {
    orderId: 'SO20240101001',
    productTotal: 2000,
    installationTotal: 0,
    deliveryTotal: 100,
    memberDiscount: 0,
    couponDiscount: -200,
    bonusDiscount: -100,
    directShipmentTotal: 0,
    grandTotal: 1800,
    promotionSkipped: false,
    memberDiscounts: [],
  };

  test.beforeEach(async ({ authenticatedPage }) => {
    // 設定 Mock API 路由

    // 會員查詢
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

    // 商品資格檢查
    await authenticatedPage.route('**/api/v1/products/*/eligibility*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockEligibility),
      });
    });

    // 建立訂單
    await authenticatedPage.route('**/api/v1/orders', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(mockOrder),
        });
      }
    });

    // 取得訂單
    await authenticatedPage.route('**/api/v1/orders/SO*', (route) => {
      if (route.request().method() === 'GET' && !route.request().url().includes('/lines') &&
          !route.request().url().includes('/calculate') && !route.request().url().includes('/coupons') &&
          !route.request().url().includes('/bonus')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            ...mockOrder,
            lines: [mockOrderLine],
            calculation: mockCalculation,
          }),
        });
      }
    });

    // 新增訂單明細
    await authenticatedPage.route('**/api/v1/orders/*/lines', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(mockOrderLine),
        });
      }
    });

    // 試算
    await authenticatedPage.route('**/api/v1/orders/*/calculate', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockCalculation),
      });
    });

    // 優惠券套用
    await authenticatedPage.route('**/api/v1/orders/*/coupons', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCouponValidation),
        });
      } else if (route.request().method() === 'DELETE') {
        route.fulfill({
          status: 204,
        });
      }
    });

    // 優惠券驗證
    await authenticatedPage.route('**/api/v1/orders/*/coupons/validate*', (route) => {
      const url = route.request().url();
      if (url.includes('VALID100')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCouponValidation),
        });
      } else if (url.includes('EXPIRED')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockInvalidCoupon),
        });
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCouponValidation),
        });
      }
    });

    // 紅利查詢
    await authenticatedPage.route('**/api/v1/orders/*/bonus/available', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ points: 1000 }),
      });
    });

    // 紅利折抵
    await authenticatedPage.route('**/api/v1/orders/*/bonus', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockBonusRedemption),
        });
      }
    });

    // 取消紅利折抵
    await authenticatedPage.route('**/api/v1/orders/*/bonus/*', (route) => {
      if (route.request().method() === 'DELETE') {
        route.fulfill({
          status: 204,
        });
      }
    });
  });

  /**
   * 輔助函式：完成會員查詢與商品新增
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

  // ========== 優惠券測試 ==========

  test('should display coupon section in order summary', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 驗證訂單摘要區塊存在
    await expect(authenticatedPage.getByRole('heading', { name: /訂單摘要/i })).toBeVisible();

    // 驗證優惠券區塊存在
    await expect(authenticatedPage.getByText(/優惠券/i)).toBeVisible();
    await expect(authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i)).toBeVisible();
  });

  test('should apply coupon successfully', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 輸入優惠券代碼
    const couponInput = authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i);
    await couponInput.fill('VALID100');

    // 點擊套用
    await authenticatedPage.getByRole('button', { name: /套用/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證成功訊息
    await expect(authenticatedPage.getByText(/優惠券套用成功|折扣.*200/i)).toBeVisible();
  });

  test('should show error for invalid coupon', async ({ authenticatedPage }) => {
    // 重新設定路由讓優惠券驗證失敗
    await authenticatedPage.route('**/api/v1/orders/*/coupons', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockInvalidCoupon),
        });
      }
    });

    await setupOrderWithProduct(authenticatedPage);

    // 輸入過期優惠券代碼
    const couponInput = authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i);
    await couponInput.fill('EXPIRED');

    // 點擊套用
    await authenticatedPage.getByRole('button', { name: /套用/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證錯誤訊息
    await expect(authenticatedPage.getByText(/優惠券已過期|無效/i)).toBeVisible();
  });

  test('should remove applied coupon', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 套用優惠券
    const couponInput = authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i);
    await couponInput.fill('VALID100');
    await authenticatedPage.getByRole('button', { name: /套用/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 點擊移除
    await authenticatedPage.getByRole('button', { name: /移除/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證優惠券輸入框重新顯示
    await expect(authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/優惠券已移除/i)).toBeVisible();
  });

  // ========== 紅利折抵測試 ==========

  test('should display bonus section for regular member', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 驗證紅利區塊存在
    await expect(authenticatedPage.getByText(/紅利折抵/i)).toBeVisible();

    // 驗證可用點數顯示
    await expect(authenticatedPage.getByText(/可用點數/i)).toBeVisible();
  });

  test('should not display bonus section for temp card member', async ({ authenticatedPage }) => {
    // 重新設定路由讓會員為臨時卡
    await authenticatedPage.route('**/api/v1/members/*', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...mockMember,
          isTempCard: true,
        }),
      });
    });

    await setupOrderWithProduct(authenticatedPage);

    // 驗證紅利區塊不存在或顯示無法使用提示
    const bonusSection = authenticatedPage.locator('.bonus-section');
    const isBonusSectionVisible = await bonusSection.isVisible().catch(() => false);

    if (isBonusSectionVisible) {
      // 如果區塊顯示，應該有「無法使用」的提示
      await expect(authenticatedPage.getByText(/臨時卡會員無法使用紅利/i)).toBeVisible();
    }
  });

  test('should redeem bonus points successfully', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 選擇商品
    const productSelect = authenticatedPage.locator('.bonus-section select');
    await productSelect.selectOption({ value: '014014014' });
    await authenticatedPage.waitForTimeout(300);

    // 輸入折抵點數
    const pointsInput = authenticatedPage.locator('.bonus-section input[type="number"]');
    await pointsInput.fill('100');

    // 點擊折抵
    await authenticatedPage.getByRole('button', { name: /確認折抵/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證成功訊息
    await expect(authenticatedPage.getByText(/紅利折抵成功|折抵.*100/i)).toBeVisible();
  });

  test('should show error for insufficient bonus points', async ({ authenticatedPage }) => {
    // 重新設定路由讓可用點數為 5 (低於最低 10 點)
    await authenticatedPage.route('**/api/v1/orders/*/bonus/available', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ points: 5 }),
      });
    });

    await setupOrderWithProduct(authenticatedPage);

    // 驗證顯示點數不足提示
    await expect(authenticatedPage.getByText(/點數不足.*10/i)).toBeVisible();
  });

  test('should cancel bonus redemption', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 先折抵紅利
    const productSelect = authenticatedPage.locator('.bonus-section select');
    await productSelect.selectOption({ value: '014014014' });
    await authenticatedPage.waitForTimeout(300);

    const pointsInput = authenticatedPage.locator('.bonus-section input[type="number"]');
    await pointsInput.fill('100');

    await authenticatedPage.getByRole('button', { name: /確認折抵/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 點擊取消
    await authenticatedPage.getByRole('button', { name: /取消/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證取消成功
    await expect(authenticatedPage.getByText(/紅利折抵已取消/i)).toBeVisible();
  });

  // ========== 整合測試 ==========

  test('should calculate order with coupon and bonus discounts', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 套用優惠券
    const couponInput = authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i);
    await couponInput.fill('VALID100');
    await authenticatedPage.getByRole('button', { name: /套用/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 折抵紅利
    const productSelect = authenticatedPage.locator('.bonus-section select');
    await productSelect.selectOption({ value: '014014014' });
    await authenticatedPage.waitForTimeout(300);

    const pointsInput = authenticatedPage.locator('.bonus-section input[type="number"]');
    await pointsInput.fill('100');

    await authenticatedPage.getByRole('button', { name: /確認折抵/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 執行試算
    await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證金額顯示
    await expect(authenticatedPage.getByText(/應付總額/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/優惠券折扣/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/紅利折抵/i)).toBeVisible();
  });

  test('should display all discount details in summary', async ({ authenticatedPage }) => {
    await setupOrderWithProduct(authenticatedPage);

    // 套用優惠券
    const couponInput = authenticatedPage.getByPlaceholder(/請輸入優惠券代碼/i);
    await couponInput.fill('VALID100');
    await authenticatedPage.getByRole('button', { name: /套用/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 執行試算
    await authenticatedPage.getByRole('button', { name: /執行試算/i }).click();
    await authenticatedPage.waitForTimeout(500);

    // 驗證摘要區塊顯示所有折扣
    await expect(authenticatedPage.getByText(/商品小計/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/優惠券折扣/i)).toBeVisible();
    await expect(authenticatedPage.getByText(/應付總額/i)).toBeVisible();
  });
});
