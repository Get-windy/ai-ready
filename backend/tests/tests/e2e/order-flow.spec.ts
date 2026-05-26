import { test, expect } from '@playwright/test';

/**
 * AI-Ready 订单创建支付流程 E2E测试
 * Sprint 27+1测试环境配置 - E2E测试脚本开发
 * 
 * 测试场景：
 * 1. 创建订单
 * 2. 支付流程
 * 3. 订单详情
 * 4. 订单取消
 */

test.describe('订单创建支付流程', () => {
  
  // 测试配置
  const testCustomer = {
    customerId: 1,
    customerName: '张三'
  };

  const testProduct = {
    productId: 1,
    productName: '商品A',
    price: 999.99
  };

  const loginPath = '/login';
  const ordersPath = '/orders';
  const createOrderPath = '/orders/create';

  test.beforeEach(async ({ page }) => {
    test.setTimeout(30000);
    
    // 登录
    await page.goto(loginPath);
    await page.fill('[name="username"]', 'testuser001');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL('/dashboard');
  });

  /**
   * 测试1: 创建订单
   */
  test('用户可以创建新订单', async ({ page }) => {
    // 访问创建订单页面
    await page.goto(createOrderPath);
    
    // 验证创建订单页面元素
    await expect(page.locator('[data-testid="create-order-form"]')).toBeVisible();

    // 选择客户
    await page.click('[data-testid="customer-selector"]');
    await page.fill('[data-testid="customer-search"]', testCustomer.customerName);
    await page.click(`[data-testid="customer-option-${testCustomer.customerId}"]`);

    // 选择商品
    await page.click('[data-testid="product-selector"]');
    await page.fill('[data-testid="product-search"]', testProduct.productName);
    await page.click(`[data-testid="product-option-${testProduct.productId}"]`);

    // 输入数量
    await page.fill('[data-testid="order-quantity"]', '10');

    // 验证订单金额计算
    const totalAmount = await page.locator('[data-testid="order-total"]').textContent();
    expect(totalAmount).toContain('9999.90');

    // 提交订单
    await page.click('[data-testid="submit-order-button"]');

    // 验证订单创建成功
    await expect(page.locator('[data-testid="order-success-message"]')).toBeVisible({ timeout: 10000 });
    
    // 验证跳转到订单详情
    await page.waitForURL(/\/orders\/\d+/);
    await expect(page.locator('[data-testid="order-detail-panel"]')).toBeVisible();
  });

  /**
   * 测试2: 支付流程
   */
  test('用户可以支付订单', async ({ page }) => {
    // 先创建一个订单
    await page.goto(createOrderPath);
    await page.click('[data-testid="customer-selector"]');
    await page.click('[data-testid="customer-option-1"]');
    await page.click('[data-testid="product-selector"]');
    await page.click('[data-testid="product-option-1"]');
    await page.fill('[data-testid="order-quantity"]', '1');
    await page.click('[data-testid="submit-order-button"]');
    await page.waitForURL(/\/orders\/\d+/);

    // 获取订单ID
    const orderId = page.url().match(/\/orders\/(\d+)/)?.[1];

    // 点击支付按钮
    await page.click('[data-testid="pay-order-button"]');

    // 选择支付方式
    await page.click('[data-testid="payment-method-alipay"]');
    
    // 验证支付金额
    const paymentAmount = await page.locator('[data-testid="payment-amount"]').textContent();
    expect(paymentAmount).toContain('999.99');

    // 模拟支付成功
    await page.click('[data-testid="confirm-payment-button"]');

    // 等待支付完成
    await page.waitForSelector('[data-testid="payment-success"]', { timeout: 15000 });

    // 验证订单状态更新
    await page.goto(`/orders/${orderId}`);
    const orderStatus = await page.locator('[data-testid="order-status"]').textContent();
    expect(orderStatus).toContain('已支付');
  });

  /**
   * 测试3: 订单详情
   */
  test('用户可以查看订单详情', async ({ page }) => {
    // 访问订单列表
    await page.goto(ordersPath);
    
    // 验证订单列表显示
    await expect(page.locator('[data-testid="order-list"]')).toBeVisible();

    // 点击第一个订单
    await page.click('[data-testid="order-item"]:first-child');

    // 验证订单详情页面
    await page.waitForURL(/\/orders\/\d+/);
    await expect(page.locator('[data-testid="order-detail-panel"]')).toBeVisible();

    // 验证订单基本信息
    await expect(page.locator('[data-testid="order-code"]')).toBeVisible();
    await expect(page.locator('[data-testid="order-customer"]')).toBeVisible();
    await expect(page.locator('[data-testid="order-product"]')).toBeVisible();
    await expect(page.locator('[data-testid="order-quantity"]')).toBeVisible();
    await expect(page.locator('[data-testid="order-amount"]')).toBeVisible();
    await expect(page.locator('[data-testid="order-status"]')).toBeVisible();

    // 验证订单时间信息
    await expect(page.locator('[data-testid="order-created-time"]')).toBeVisible();
  });

  /**
   * 测试4: 订单取消
   */
  test('用户可以取消未支付的订单', async ({ page }) => {
    // 创建一个订单
    await page.goto(createOrderPath);
    await page.click('[data-testid="customer-selector"]');
    await page.click('[data-testid="customer-option-1"]');
    await page.click('[data-testid="product-selector"]');
    await page.click('[data-testid="product-option-1"]');
    await page.fill('[data-testid="order-quantity"]', '1');
    await page.click('[data-testid="submit-order-button"]');
    await page.waitForURL(/\/orders\/\d+/);

    // 点击取消订单按钮
    await page.click('[data-testid="cancel-order-button"]');

    // 确认取消
    await page.click('[data-testid="confirm-cancel-button"]');

    // 验证取消成功
    await expect(page.locator('[data-testid="cancel-success-message"]')).toBeVisible();

    // 验证订单状态更新
    const orderStatus = await page.locator('[data-testid="order-status"]').textContent();
    expect(orderStatus).toContain('已取消');
  });

  /**
   * 测试5: 订单列表分页
   */
  test('用户可以浏览订单列表并分页', async ({ page }) => {
    await page.goto(ordersPath);
    
    // 验证分页控件
    await expect(page.locator('[data-testid="pagination"]')).toBeVisible();

    // 验证第一页
    const currentPage = await page.locator('[data-testid="current-page"]').textContent();
    expect(currentPage).toBe('1');

    // 点击下一页
    await page.click('[data-testid="next-page-button"]');

    // 验证第二页
    const newPage = await page.locator('[data-testid="current-page"]').textContent();
    expect(newPage).toBe('2');

    // 返回第一页
    await page.click('[data-testid="prev-page-button"]');
    expect(await page.locator('[data-testid="current-page"]').textContent()).toBe('1');
  });

  /**
   * 测试6: 订单搜索
   */
  test('用户可以搜索订单', async ({ page }) => {
    await page.goto(ordersPath);
    
    // 输入搜索关键词
    await page.fill('[data-testid="order-search"]', 'ORD001');
    await page.click('[data-testid="search-button"]');

    // 等待搜索结果
    await page.waitForSelector('[data-testid="search-results"]', { timeout: 5000 });

    // 验证搜索结果包含关键词
    const searchResults = await page.locator('[data-testid="order-item"]').allTextContents();
    expect(searchResults.some(text => text.includes('ORD001'))).toBeTruthy();
  });

  /**
   * 测试7: 订单筛选
   */
  test('用户可以按状态筛选订单', async ({ page }) => {
    await page.goto(ordersPath);
    
    // 选择筛选条件
    await page.click('[data-testid="order-filter"]');
    await page.click('[data-testid="filter-status-created"]');

    // 验证筛选结果
    await page.waitForSelector('[data-testid="filtered-results"]');

    // 验证所有显示的订单状态都是"已创建"
    const orderStatuses = await page.locator('[data-testid="order-status"]').allTextContents();
    expect(orderStatuses.every(status => status.includes('已创建'))).toBeTruthy();
  });

  /**
   * 测试8: 订单金额验证
   */
  test('订单金额计算正确', async ({ page }) => {
    await page.goto(createOrderPath);
    
    // 选择客户和商品
    await page.click('[data-testid="customer-selector"]');
    await page.click('[data-testid="customer-option-1"]');
    await page.click('[data-testid="product-selector"]');
    await page.click('[data-testid="product-option-1"]');

    // 输入数量并验证实时计算
    await page.fill('[data-testid="order-quantity"]', '5');
    
    // 等待金额更新
    await page.waitForTimeout(500);

    // 验证单价显示
    const unitPrice = await page.locator('[data-testid="unit-price"]').textContent();
    expect(parseFloat(unitPrice.replace(/[^\d.]/g, ''))).toBe(999.99);

    // 验证总价计算
    const totalAmount = await page.locator('[data-testid="order-total"]').textContent();
    expect(parseFloat(totalAmount.replace(/[^\d.]/g, ''))).toBe(4999.95);
  });
});

/**
 * 高级订单场景测试
 */
test.describe('高级订单场景', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'testuser001');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL('/dashboard');
  });

  test('批量创建订单', async ({ page }) => {
    await page.goto('/orders/create');
    
    // 添加多个商品
    await page.click('[data-testid="add-product-button"]');
    await page.click('[data-testid="product-option-1"]');
    await page.fill('[data-testid="quantity-0"]', '2');

    await page.click('[data-testid="add-product-button"]');
    await page.click('[data-testid="product-option-2"]');
    await page.fill('[data-testid="quantity-1"]', '3');

    // 验证总价包含所有商品
    const total = await page.locator('[data-testid="order-total"]').textContent();
    expect(parseFloat(total.replace(/[^\d.]/g, ''))).toBeGreaterThan(0);
  });

  test('订单支付失败处理', async ({ page }) => {
    // 创建订单
    await page.goto('/orders/create');
    await page.click('[data-testid="customer-option-1"]');
    await page.click('[data-testid="product-option-1"]');
    await page.fill('[data-testid="order-quantity"]', '1');
    await page.click('[data-testid="submit-order-button"]');
    await page.waitForURL(/\/orders\/\d+/);

    // 点击支付
    await page.click('[data-testid="pay-order-button"]');

    // 模拟支付失败（通过测试数据）
    await page.click('[data-testid="simulate-payment-failure"]');

    // 验证错误处理
    await expect(page.locator('[data-testid="payment-error"]')).toBeVisible();
    
    // 验证订单状态未改变
    const status = await page.locator('[data-testid="order-status"]').textContent();
    expect(status).toContain('待支付');
  });

  test('订单退款流程', async ({ page }) => {
    // 假设有一个已支付的订单
    await page.goto('/orders/1');
    
    // 点击申请退款
    await page.click('[data-testid="refund-button"]');

    // 填写退款原因
    await page.fill('[data-testid="refund-reason"]', '商品质量问题');
    await page.click('[data-testid="submit-refund-button"]');

    // 验证退款申请提交
    await expect(page.locator('[data-testid="refund-success"]')).toBeVisible();

    // 验证订单状态更新
    const status = await page.locator('[data-testid="order-status"]').textContent();
    expect(status).toContain('退款申请');
  });
});