import { test, expect } from '@playwright/test';

/**
 * AI-Ready 库存管理流程 E2E测试
 * Sprint 27+1测试环境配置 - E2E测试脚本开发
 * 
 * 测试场景：
 * 1. 库存查询
 * 2. 库存详情
 * 3. 库存调整
 * 4. 库存预警
 */

test.describe('库存管理流程', () => {
  
  // 测试配置
  const testWarehouse = {
    warehouseId: 1,
    warehouseName: '北京仓库'
  };

  const testProduct = {
    productId: 1,
    productName: '商品A'
  };

  const loginPath = '/login';
  const inventoryPath = '/inventory';

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
   * 测试1: 库存查询
   */
  test('用户可以查看库存列表', async ({ page }) => {
    // 访问库存页面
    await page.goto(inventoryPath);
    
    // 验证库存页面元素
    await expect(page.locator('[data-testid="inventory-panel"]')).toBeVisible();
    await expect(page.locator('[data-testid="inventory-list"]')).toBeVisible();

    // 验证仓库选择器
    await expect(page.locator('[data-testid="warehouse-selector"]')).toBeVisible();

    // 选择仓库
    await page.click('[data-testid="warehouse-selector"]');
    await page.click(`[data-testid="warehouse-option-${testWarehouse.warehouseId}"]`);

    // 验证库存列表加载
    await page.waitForSelector('[data-testid="inventory-item"]', { timeout: 5000 });

    // 验证库存列表包含商品信息
    const inventoryItems = await page.locator('[data-testid="inventory-item"]').count();
    expect(inventoryItems).toBeGreaterThan(0);
  });

  /**
   * 测试2: 库存详情
   */
  test('用户可以查看商品库存详情', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 选择仓库
    await page.click('[data-testid="warehouse-selector"]');
    await page.click(`[data-testid="warehouse-option-${testWarehouse.warehouseId}"]`);

    // 点击商品详情
    await page.click(`[data-testid="inventory-item-${testProduct.productId}"]`);

    // 验证详情面板
    await expect(page.locator('[data-testid="inventory-detail-panel"]')).toBeVisible();

    // 验证详情信息
    await expect(page.locator('[data-testid="product-name"]')).toContainText(testProduct.productName);
    await expect(page.locator('[data-testid="stock-quantity"]')).toBeVisible();
    await expect(page.locator('[data-testid="min-stock"]')).toBeVisible();
    await expect(page.locator('[data-testid="max-stock"]')).toBeVisible();
    await expect(page.locator('[data-testid="warehouse-name"]')).toBeVisible();
  });

  /**
   * 测试3: 库存调整
   */
  test('用户可以调整库存数量', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 选择仓库
    await page.click('[data-testid="warehouse-selector"]');
    await page.click('[data-testid="warehouse-option-1"]');

    // 选择商品
    await page.click('[data-testid="inventory-item-1"]');

    // 点击调整库存
    await page.click('[data-testid="adjust-stock-button"]');

    // 验证调整对话框
    await expect(page.locator('[data-testid="adjust-stock-dialog"]')).toBeVisible();

    // 选择调整类型（入库）
    await page.click('[data-testid="adjust-type-inbound"]');

    // 输入调整数量
    await page.fill('[data-testid="adjust-quantity"]', '50');

    // 输入调整原因
    await page.fill('[data-testid="adjust-reason"]', '采购入库');

    // 提交调整
    await page.click('[data-testid="submit-adjust-button"]');

    // 验证调整成功
    await expect(page.locator('[data-testid="adjust-success-message"]')).toBeVisible({ timeout: 10000 });

    // 验证库存数量更新
    const newStock = await page.locator('[data-testid="stock-quantity"]').textContent();
    expect(parseInt(newStock)).toBeGreaterThan(100); // 原库存100 + 50
  });

  /**
   * 测试4: 库存预警
   */
  test('用户可以查看库存预警商品', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 点击预警标签
    await page.click('[data-testid="low-stock-tab"]');

    // 验证预警列表
    await expect(page.locator('[data-testid="low-stock-list"]')).toBeVisible();

    // 验证预警商品状态
    const lowStockItems = await page.locator('[data-testid="low-stock-item"]').count();
    expect(lowStockItems).toBeGreaterThanOrEqual(0);

    // 如果有预警商品，验证预警标识
    if (lowStockItems > 0) {
      const firstItem = page.locator('[data-testid="low-stock-item"]:first-child');
      await expect(firstItem.locator('[data-testid="warning-badge"]')).toBeVisible();
    }
  });

  /**
   * 测试5: 库存搜索
   */
  test('用户可以搜索商品库存', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 输入搜索关键词
    await page.fill('[data-testid="inventory-search"]', testProduct.productName);
    await page.click('[data-testid="search-button"]');

    // 验证搜索结果
    await page.waitForSelector('[data-testid="search-results"]');

    // 验证搜索结果包含关键词
    const searchResults = await page.locator('[data-testid="inventory-item"]').allTextContents();
    expect(searchResults.some(text => text.includes(testProduct.productName))).toBeTruthy();
  });

  /**
   * 测试6: 库存筛选
   */
  test('用户可以按库存状态筛选', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 选择筛选条件
    await page.click('[data-testid="inventory-filter"]');
    await page.click('[data-testid="filter-status-normal"]');

    // 验证筛选结果
    await page.waitForSelector('[data-testid="filtered-results"]');

    // 验证所有显示的商品库存状态正常
    const inventoryStatuses = await page.locator('[data-testid="stock-status"]').allTextContents();
    expect(inventoryStatuses.every(status => status.includes('正常'))).toBeTruthy();
  });

  /**
   * 测试7: 库存出入库记录
   */
  test('用户可以查看库存出入库记录', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 选择商品
    await page.click('[data-testid="inventory-item-1"]');

    // 点击查看记录
    await page.click('[data-testid="view-records-button"]');

    // 验证记录面板
    await expect(page.locator('[data-testid="stock-records-panel"]')).toBeVisible();

    // 验证记录列表
    await expect(page.locator('[data-testid="stock-record-item"]')).toBeVisible();

    // 验证记录详情
    await expect(page.locator('[data-testid="record-type"]')).toBeVisible();
    await expect(page.locator('[data-testid="record-quantity"]')).toBeVisible();
    await expect(page.locator('[data-testid="record-time"]')).toBeVisible();
    await expect(page.locator('[data-testid="record-operator"]')).toBeVisible();
  });

  /**
   * 测试8: 库存盘点
   */
  test('用户可以发起库存盘点', async ({ page }) => {
    await page.goto(inventoryPath);
    
    // 点击盘点按钮
    await page.click('[data-testid="stock-check-button"]');

    // 验证盘点对话框
    await expect(page.locator('[data-testid="stock-check-dialog"]')).toBeVisible();

    // 选择盘点范围
    await page.click('[data-testid="check-range-partial"]');
    await page.click('[data-testid="select-product-1"]');

    // 开始盘点
    await page.click('[data-testid="start-check-button"]');

    // 验证盘点任务创建
    await expect(page.locator('[data-testid="check-task-created"]')).toBeVisible();

    // 输入盘点数量
    await page.fill('[data-testid="actual-stock-1"]', '95');

    // 提交盘点结果
    await page.click('[data-testid="submit-check-button"]');

    // 验证盘点完成
    await expect(page.locator('[data-testid="check-complete-message"]')).toBeVisible();
  });
});

/**
 * 高级库存场景测试
 */
test.describe('高级库存场景', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'testuser001');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL('/dashboard');
  });

  test('库存调拨流程', async ({ page }) => {
    await page.goto('/inventory');
    
    // 点击调拨按钮
    await page.click('[data-testid="transfer-button"]');

    // 选择源仓库
    await page.click('[data-testid="source-warehouse-selector"]');
    await page.click('[data-testid="warehouse-option-1"]');

    // 选择目标仓库
    await page.click('[data-testid="target-warehouse-selector"]');
    await page.click('[data-testid="warehouse-option-2"]');

    // 选择商品
    await page.click('[data-testid="transfer-product-selector"]');
    await page.click('[data-testid="product-option-1"]');

    // 输入调拨数量
    await page.fill('[data-testid="transfer-quantity"]', '20');

    // 提交调拨
    await page.click('[data-testid="submit-transfer-button"]');

    // 验证调拨成功
    await expect(page.locator('[data-testid="transfer-success"]')).toBeVisible();
  });

  test('库存锁定功能', async ({ page }) => {
    await page.goto('/inventory');
    
    // 选择商品
    await page.click('[data-testid="inventory-item-1"]');

    // 点击锁定库存
    await page.click('[data-testid="lock-stock-button"]');

    // 输入锁定数量
    await page.fill('[data-testid="lock-quantity"]', '10');
    await page.fill('[data-testid="lock-reason"]', '订单预占');

    // 提交锁定
    await page.click('[data-testid="submit-lock-button"]');

    // 验证锁定成功
    await expect(page.locator('[data-testid="lock-success"]')).toBeVisible();

    // 验证可用库存减少
    const availableStock = await page.locator('[data-testid="available-stock"]').textContent();
    expect(parseInt(availableStock)).toBeLessThan(100);
  });

  test('库存报表生成', async ({ page }) => {
    await page.goto('/inventory');
    
    // 点击报表按钮
    await page.click('[data-testid="inventory-report-button"]');

    // 选择报表类型
    await page.click('[data-testid="report-type-summary"]');

    // 选择时间范围
    await page.fill('[data-testid="report-start-date"]', '2026-01-01');
    await page.fill('[data-testid="report-end-date"]', '2026-04-24');

    // 生成报表
    await page.click('[data-testid="generate-report-button"]');

    // 等待报表生成
    await page.waitForSelector('[data-testid="report-container"]', { timeout: 15000 });

    // 验证报表内容
    await expect(page.locator('[data-testid="report-chart"]')).toBeVisible();
    await expect(page.locator('[data-testid="report-table"]')).toBeVisible();
  });

  test('库存批量调整', async ({ page }) => {
    await page.goto('/inventory');
    
    // 选择多个商品
    await page.click('[data-testid="inventory-item-1"]');
    await page.click('[data-testid="inventory-item-2"]');

    // 点击批量调整
    await page.click('[data-testid="batch-adjust-button"]');

    // 选择调整类型
    await page.click('[data-testid="batch-adjust-type-inbound"]');

    // 输入调整数量
    await page.fill('[data-testid="batch-adjust-quantity"]', '10');

    // 提交批量调整
    await page.click('[data-testid="submit-batch-adjust-button"]');

    // 验证批量调整成功
    await expect(page.locator('[data-testid="batch-success-message"]')).toBeVisible();
  });
});