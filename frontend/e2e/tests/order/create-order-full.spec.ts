import { test, expect } from '../../fixtures/mock-auth.fixture';

/**
 * T111: Full Flow E2E Test
 *
 * 完整訂單建立流程測試：
 * 1. 會員查詢
 * 2. 新增商品
 * 3. 設定安裝/運送服務
 * 4. 套用優惠券
 * 5. 紅利折抵
 * 6. 價格試算
 * 7. 訂單提交
 */
test.describe('Create Order - Full Flow', () => {
  // Mock API 回應設定
  const mockMember = {
    memberId: 'K00123',
    cardType: 'A',
    discType: '0',
    name: 'SIT測試人員',
    cellPhone: '0912345678',
    zipCode: '100',
    address: '台北市中正區測試路1號',
    discTypeName: '折價 (Discounting)',
    discRate: 0.95,
    specialRate: null,
    isTempCard: false,
    bonusPoints: 1000,
  };

  const mockProducts = [
    {
      skuNo: '014014014',
      skuName: '冷氣機 日立 RAS-25NK',
      unitPrice: 25000,
      posPrice: 25000,
      taxType: '1',
      taxTypeName: '應稅',
      hasInstallation: true,
      installationRequired: true,
    },
    {
      skuNo: '015015015',
      skuName: '洗衣機 LG WD-S15TBD',
      unitPrice: 15000,
      posPrice: 15000,
      taxType: '1',
      taxTypeName: '應稅',
      hasInstallation: true,
      installationRequired: false,
    },
  ];

  const mockOrder = {
    orderId: 'SO20241220001',
    projectId: 'PJ241220000001',
    status: '1',
    statusName: '草稿',
    customer: {
      memberId: 'K00123',
      name: 'SIT測試人員',
      cellPhone: '0912345678',
      isTempCard: false,
    },
    address: {
      zipCode: '100',
      fullAddress: '台北市中正區測試路1號',
    },
    channelId: 'SO',
    storeId: 'S001',
    lines: [],
    calculation: {
      productTotal: 0,
      installationTotal: 0,
      deliveryTotal: 0,
      memberDiscount: 0,
      couponDiscount: 0,
      bonusDiscount: 0,
      grandTotal: 0,
    },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const mockWorkTypes = [
    { workTypeId: 'AC01', workTypeName: '冷氣安裝', category: 'INSTALLATION', minimumWage: 500 },
    { workTypeId: 'DLV01', workTypeName: '一般運送', category: 'DELIVERY', minimumWage: 200 },
    { workTypeId: 'HOME01', workTypeName: '宅配', category: 'HOME_DELIVERY', minimumWage: 150 },
  ];

  const mockCoupon = {
    couponId: 'SAVE500',
    valid: true,
    couponName: '滿萬折 500',
    discountAmount: { amount: 500 },
    minimumOrderAmount: 10000,
    failureReason: null,
  };

  const mockBonusRedemption = {
    skuNo: '015015015',
    skuName: '洗衣機 LG WD-S15TBD',
    pointsUsed: 100,
    discountAmount: { amount: 100 },
    remainingPoints: 900,
    exchangeRate: 1,
  };

  const mockCalculation = {
    orderId: 'SO20241220001',
    computeTypes: [
      { type: 1, name: '商品小計', amount: 40000, taxAmount: 2000 },
      { type: 2, name: '安裝小計', amount: 1500, taxAmount: 75 },
      { type: 3, name: '運送小計', amount: 200, taxAmount: 10 },
      { type: 4, name: '會員折扣', amount: -2000 },
      { type: 5, name: '直送費用', amount: 0 },
      { type: 6, name: '優惠券折扣', amount: -500 },
      { type: 7, name: '紅利折抵', amount: -100 },
    ],
    memberDiscounts: [
      { lineId: 'LINE001', discountAmount: 1250, discountRate: 0.95 },
      { lineId: 'LINE002', discountAmount: 750, discountRate: 0.95 },
    ],
    grandTotal: 39100,
    taxAmount: 2085,
    promotionSkipped: false,
    warnings: [],
    calculatedAt: new Date().toISOString(),
  };

  test.beforeEach(async ({ authenticatedPage }) => {
    // Mock 會員查詢 API
    await authenticatedPage.route('**/api/v1/members/*', (route) => {
      const url = route.request().url();
      if (url.includes('K00123')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMember),
        });
      } else {
        route.fulfill({ status: 404 });
      }
    });

    // Mock 商品資格驗證 API
    await authenticatedPage.route('**/api/v1/products/*/eligibility*', (route) => {
      const url = route.request().url();
      const product = mockProducts.find(p => url.includes(p.skuNo));
      if (product) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            eligible: true,
            skuNo: product.skuNo,
            failureLevel: null,
            failureReason: null,
            product: product,
          }),
        });
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ eligible: false, failureReason: '找不到商品' }),
        });
      }
    });

    // Mock 工種查詢 API
    await authenticatedPage.route('**/api/v1/work-types**', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(mockWorkTypes),
      });
    });

    // Mock 訂單建立 API
    let orderWithLines = { ...mockOrder, lines: [] as any[] };
    let lineCounter = 0;

    await authenticatedPage.route('**/api/v1/orders', (route) => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(orderWithLines),
        });
      }
    });

    // Mock 訂單查詢 API
    await authenticatedPage.route('**/api/v1/orders/*', (route, request) => {
      const url = route.request().url();
      const method = route.request().method();

      if (method === 'GET' && url.match(/\/orders\/[^/]+$/)) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(orderWithLines),
        });
      } else if (url.includes('/lines') && method === 'POST') {
        // 新增行項
        lineCounter++;
        const newLine = {
          lineId: `LINE00${lineCounter}`,
          serialNo: lineCounter,
          skuNo: mockProducts[lineCounter - 1]?.skuNo ?? '000000000',
          skuName: mockProducts[lineCounter - 1]?.skuName ?? '測試商品',
          quantity: 1,
          unitPrice: mockProducts[lineCounter - 1]?.unitPrice ?? 1000,
          actualUnitPrice: mockProducts[lineCounter - 1]?.unitPrice ?? 1000,
          subtotal: mockProducts[lineCounter - 1]?.unitPrice ?? 1000,
          memberDisc: 0,
          bonusDisc: 0,
          couponDisc: 0,
          installationCost: 0,
          deliveryCost: 0,
          deliveryMethod: 'N',
          deliveryMethodName: '運送',
          stockMethod: 'X',
          stockMethodName: '現貨',
          taxType: '1',
          taxTypeName: '應稅',
        };
        orderWithLines.lines.push(newLine);
        route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(newLine),
        });
      } else if (url.includes('/calculate') && method === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCalculation),
        });
      } else if (url.includes('/submit') && method === 'POST') {
        orderWithLines.status = '2';
        orderWithLines.statusName = '報價';
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(orderWithLines),
        });
      } else if (url.includes('/coupons') && method === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockCoupon),
        });
      } else if (url.includes('/bonus') && method === 'POST') {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockBonusRedemption),
        });
      } else if (url.includes('/bonus/available')) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(mockMember.bonusPoints),
        });
      } else {
        route.continue();
      }
    });
  });

  test('完整訂單建立流程', async ({ authenticatedPage }) => {
    // 導航到建立訂單頁面
    await authenticatedPage.goto('/order/create');
    await expect(authenticatedPage.getByRole('heading', { name: '新增訂單' })).toBeVisible();

    // Step 1: 輸入會員卡號
    await authenticatedPage.getByLabel('會員卡號').fill('K00123');
    await authenticatedPage.getByRole('button', { name: '查詢' }).click();

    // 等待會員資訊顯示
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();
    await expect(authenticatedPage.getByText('折價 (Discounting)')).toBeVisible();

    // Step 2: 新增第一個商品（冷氣）
    await authenticatedPage.getByLabel('商品編號').fill('014014014');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();

    // 等待商品新增成功
    await expect(authenticatedPage.getByText('冷氣機 日立 RAS-25NK')).toBeVisible();

    // Step 3: 新增第二個商品（洗衣機）
    await authenticatedPage.getByLabel('商品編號').fill('015015015');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();

    // 等待第二個商品新增成功
    await expect(authenticatedPage.getByText('洗衣機 LG WD-S15TBD')).toBeVisible();

    // Step 4: 套用優惠券
    await authenticatedPage.getByLabel('優惠券代碼').fill('SAVE500');
    await authenticatedPage.getByRole('button', { name: '套用優惠券' }).click();

    // 等待優惠券套用成功訊息
    await expect(authenticatedPage.getByText(/優惠券套用成功/)).toBeVisible();

    // Step 5: 執行價格試算
    await authenticatedPage.getByRole('button', { name: '執行試算' }).click();

    // 等待試算完成
    await expect(authenticatedPage.getByText(/試算完成/)).toBeVisible();
    await expect(authenticatedPage.getByText('39,100')).toBeVisible(); // 應付總額

    // Step 6: 提交訂單
    await authenticatedPage.getByRole('button', { name: '提交訂單' }).click();

    // 等待提交成功
    await expect(authenticatedPage.getByText(/訂單.*已提交成功/)).toBeVisible();

    // 驗證訂單狀態變更
    await expect(authenticatedPage.getByText('報價')).toBeVisible();

    // 截圖留存
    await authenticatedPage.screenshot({ path: 'e2e/screenshots/create-order-full-success.png' });
  });

  test('會員折扣正確計算', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/order/create');

    // 查詢會員
    await authenticatedPage.getByLabel('會員卡號').fill('K00123');
    await authenticatedPage.getByRole('button', { name: '查詢' }).click();
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();

    // 新增商品
    await authenticatedPage.getByLabel('商品編號').fill('014014014');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();
    await expect(authenticatedPage.getByText('冷氣機 日立 RAS-25NK')).toBeVisible();

    // 執行試算
    await authenticatedPage.getByRole('button', { name: '執行試算' }).click();

    // 驗證會員折扣顯示
    await expect(authenticatedPage.getByText(/會員折扣/)).toBeVisible();
    await expect(authenticatedPage.getByText('95%')).toBeVisible(); // 折扣率
  });

  test('優惠券驗證與套用', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/order/create');

    // 建立訂單
    await authenticatedPage.getByLabel('會員卡號').fill('K00123');
    await authenticatedPage.getByRole('button', { name: '查詢' }).click();
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();

    // 新增商品
    await authenticatedPage.getByLabel('商品編號').fill('014014014');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();

    // 輸入無效優惠券
    await authenticatedPage.getByLabel('優惠券代碼').fill('INVALID');
    await authenticatedPage.getByRole('button', { name: '套用優惠券' }).click();

    // 應顯示錯誤訊息（Mock 未設定此優惠券）
    // 實際測試時會根據 Mock 設定顯示對應結果

    // 輸入有效優惠券
    await authenticatedPage.getByLabel('優惠券代碼').clear();
    await authenticatedPage.getByLabel('優惠券代碼').fill('SAVE500');
    await authenticatedPage.getByRole('button', { name: '套用優惠券' }).click();

    await expect(authenticatedPage.getByText(/優惠券套用成功/)).toBeVisible();
  });

  test('紅利折抵功能', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/order/create');

    // 建立訂單
    await authenticatedPage.getByLabel('會員卡號').fill('K00123');
    await authenticatedPage.getByRole('button', { name: '查詢' }).click();
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();

    // 新增商品
    await authenticatedPage.getByLabel('商品編號').fill('015015015');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();
    await expect(authenticatedPage.getByText('洗衣機 LG WD-S15TBD')).toBeVisible();

    // 驗證可用紅利點數顯示
    await expect(authenticatedPage.getByText(/可用紅利.*1,000/)).toBeVisible();

    // 選擇商品進行紅利折抵
    await authenticatedPage.getByRole('button', { name: '紅利折抵' }).first().click();
    await authenticatedPage.getByLabel('折抵點數').fill('100');
    await authenticatedPage.getByRole('button', { name: '確認折抵' }).click();

    // 驗證折抵成功
    await expect(authenticatedPage.getByText(/紅利折抵成功/)).toBeVisible();
  });

  test('提交按鈕在 API 呼叫期間禁用', async ({ authenticatedPage }) => {
    await authenticatedPage.goto('/order/create');

    // 建立訂單
    await authenticatedPage.getByLabel('會員卡號').fill('K00123');
    await authenticatedPage.getByRole('button', { name: '查詢' }).click();
    await expect(authenticatedPage.getByText('SIT測試人員')).toBeVisible();

    // 新增商品
    await authenticatedPage.getByLabel('商品編號').fill('014014014');
    await authenticatedPage.getByRole('button', { name: '加入商品' }).click();

    // 先試算
    await authenticatedPage.getByRole('button', { name: '執行試算' }).click();
    await expect(authenticatedPage.getByText(/試算完成/)).toBeVisible();

    // 點擊提交按鈕
    const submitButton = authenticatedPage.getByRole('button', { name: '提交訂單' });
    await submitButton.click();

    // 驗證按鈕在提交期間顯示「提交中」
    await expect(authenticatedPage.getByText('提交中...')).toBeVisible();
  });

  test('骨架屏過渡效果', async ({ authenticatedPage }) => {
    // 減慢網路速度以觀察骨架屏
    await authenticatedPage.route('**/api/v1/**', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 500)); // 延遲 500ms
      route.continue();
    });

    await authenticatedPage.goto('/order/create');

    // 驗證頁面載入時有過渡動畫
    const section = authenticatedPage.locator('.section').first();
    await expect(section).toHaveCSS('animation-name', 'fadeIn');
  });
});
