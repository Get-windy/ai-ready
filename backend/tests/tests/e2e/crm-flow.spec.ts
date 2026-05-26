import { test, expect } from '@playwright/test';

/**
 * AI-Ready CRM客户管理流程 E2E测试
 * Sprint 27+1测试环境配置 - E2E测试脚本开发
 * 
 * 测试场景：
 * 1. 客户创建
 * 2. 客户查询
 * 3. 客户详情
 * 4. 客户跟进
 */

test.describe('CRM客户管理流程', () => {
  
  // 测试配置
  const testCustomer = {
    name: '测试客户张三',
    phone: '13800138000',
    email: 'zhangsan@test.com',
    company: '测试公司A',
    address: '北京市朝阳区测试路100号'
  };

  const loginPath = '/login';
  const crmPath = '/crm/customers';

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
   * 测试1: 客户创建
   */
  test('用户可以创建新客户', async ({ page }) => {
    // 访问CRM客户页面
    await page.goto(crmPath);
    
    // 点击创建客户按钮
    await page.click('[data-testid="create-customer-button"]');

    // 验证创建客户对话框
    await expect(page.locator('[data-testid="create-customer-dialog"]')).toBeVisible();

    // 填写客户信息
    await page.fill('[data-testid="customer-name"]', testCustomer.name);
    await page.fill('[data-testid="customer-phone"]', testCustomer.phone);
    await page.fill('[data-testid="customer-email"]', testCustomer.email);
    await page.fill('[data-testid="customer-company"]', testCustomer.company);
    await page.fill('[data-testid="customer-address"]', testCustomer.address);

    // 选择客户类型
    await page.click('[data-testid="customer-type-selector"]');
    await page.click('[data-testid="customer-type-enterprise"]');

    // 选择客户来源
    await page.click('[data-testid="customer-source-selector"]');
    await page.click('[data-testid="customer-source-website"]');

    // 提交创建
    await page.click('[data-testid="submit-customer-button"]');

    // 验证创建成功
    await expect(page.locator('[data-testid="create-success-message"]')).toBeVisible({ timeout: 10000 });

    // 验证跳转到客户详情
    await page.waitForURL(/\/crm\/customers\/\d+/);
    await expect(page.locator('[data-testid="customer-detail-panel"]')).toBeVisible();
  });

  /**
   * 测试2: 客户查询
   */
  test('用户可以查询客户列表', async ({ page }) => {
    await page.goto(crmPath);
    
    // 验证客户列表页面
    await expect(page.locator('[data-testid="customer-panel"]')).toBeVisible();
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible();

    // 验证搜索功能
    await page.fill('[data-testid="customer-search"]', testCustomer.name);
    await page.click('[data-testid="search-button"]');

    // 验证搜索结果
    await page.waitForSelector('[data-testid="search-results"]');

    // 验证搜索结果包含关键词
    const searchResults = await page.locator('[data-testid="customer-item"]').allTextContents();
    expect(searchResults.some(text => text.includes(testCustomer.name))).toBeTruthy();
  });

  /**
   * 测试3: 客户详情
   */
  test('用户可以查看客户详情', async ({ page }) => {
    await page.goto(crmPath);
    
    // 点击客户详情
    await page.click('[data-testid="customer-item"]:first-child');

    // 验证客户详情页面
    await page.waitForURL(/\/crm\/customers\/\d+/);
    await expect(page.locator('[data-testid="customer-detail-panel"]')).toBeVisible();

    // 验证基本信息
    await expect(page.locator('[data-testid="customer-name"]')).toBeVisible();
    await expect(page.locator('[data-testid="customer-phone"]')).toBeVisible();
    await expect(page.locator('[data-testid="customer-email"]')).toBeVisible();
    await expect(page.locator('[data-testid="customer-company"]')).toBeVisible();

    // 验证客户状态
    await expect(page.locator('[data-testid="customer-status"]')).toBeVisible();

    // 验证客户来源
    await expect(page.locator('[data-testid="customer-source"]')).toBeVisible();

    // 验证创建时间
    await expect(page.locator('[data-testid="customer-created-time"]')).toBeVisible();
  });

  /**
   * 测试4: 客户跟进
   */
  test('用户可以添加客户跟进记录', async ({ page }) => {
    await page.goto(crmPath);
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击添加跟进
    await page.click('[data-testid="add-follow-up-button"]');

    // 验证跟进对话框
    await expect(page.locator('[data-testid="follow-up-dialog"]')).toBeVisible();

    // 选择跟进类型
    await page.click('[data-testid="follow-up-type-selector"]');
    await page.click('[data-testid="follow-up-type-phone"]');

    // 填写跟进内容
    await page.fill('[data-testid="follow-up-content"]', '电话联系客户，确认产品需求');

    // 填写跟进结果
    await page.click('[data-testid="follow-up-result-selector"]');
    await page.click('[data-testid="follow-up-result-positive"]');

    // 设置下次跟进时间
    await page.fill('[data-testid="next-follow-up-time"]', '2026-04-25');

    // 提交跟进
    await page.click('[data-testid="submit-follow-up-button"]');

    // 验证跟进成功
    await expect(page.locator('[data-testid="follow-up-success"]')).toBeVisible();

    // 验证跟进记录显示
    await expect(page.locator('[data-testid="follow-up-record"]')).toBeVisible();
  });

  /**
   * 测试5: 客户编辑
   */
  test('用户可以编辑客户信息', async ({ page }) => {
    await page.goto(crmPath);
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击编辑按钮
    await page.click('[data-testid="edit-customer-button"]');

    // 验证编辑对话框
    await expect(page.locator('[data-testid="edit-customer-dialog"]')).toBeVisible();

    // 修改客户信息
    await page.fill('[data-testid="customer-phone"]', '13900139000');
    await page.fill('[data-testid="customer-email"]', 'newemail@test.com');

    // 提交修改
    await page.click('[data-testid="submit-edit-button"]');

    // 验证修改成功
    await expect(page.locator('[data-testid="edit-success-message"]')).toBeVisible();

    // 验证信息更新
    await expect(page.locator('[data-testid="customer-phone"]')).toContainText('13900139000');
    await expect(page.locator('[data-testid="customer-email"]')).toContainText('newemail@test.com');
  });

  /**
   * 测试6: 客户筛选
   */
  test('用户可以按条件筛选客户', async ({ page }) => {
    await page.goto(crmPath);
    
    // 点击筛选按钮
    await page.click('[data-testid="customer-filter"]');

    // 选择筛选条件
    await page.click('[data-testid="filter-customer-type"]');
    await page.click('[data-testid="filter-type-enterprise"]');

    // 点击应用筛选
    await page.click('[data-testid="apply-filter-button"]');

    // 验证筛选结果
    await page.waitForSelector('[data-testid="filtered-results"]');

    // 验证所有显示的客户类型一致
    const customerTypes = await page.locator('[data-testid="customer-type"]').allTextContents();
    expect(customerTypes.every(type => type.includes('企业客户'))).toBeTruthy();
  });

  /**
   * 测试7: 客户状态变更
   */
  test('用户可以变更客户状态', async ({ page }) => {
    await page.goto(crmPath);
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击状态变更按钮
    await page.click('[data-testid="change-status-button"]');

    // 选择新状态
    await page.click('[data-testid="status-selector"]');
    await page.click('[data-testid="status-active"]');

    // 填写变更原因
    await page.fill('[data-testid="status-change-reason"]', '客户已签约');

    // 提交状态变更
    await page.click('[data-testid="submit-status-change-button"]');

    // 验证状态变更成功
    await expect(page.locator('[data-testid="status-change-success"]')).toBeVisible();

    // 验证新状态显示
    const newStatus = await page.locator('[data-testid="customer-status"]').textContent();
    expect(newStatus).toContain('活跃');
  });

  /**
   * 测试8: 客户分配
   */
  test('用户可以分配客户给销售人员', async ({ page }) => {
    await page.goto(crmPath);
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击分配按钮
    await page.click('[data-testid="assign-customer-button"]');

    // 验证分配对话框
    await expect(page.locator('[data-testid="assign-dialog"]')).toBeVisible();

    // 选择销售人员
    await page.click('[data-testid="sales-person-selector"]');
    await page.click('[data-testid="sales-person-option-1"]');

    // 提交分配
    await page.click('[data-testid="submit-assign-button"]');

    // 验证分配成功
    await expect(page.locator('[data-testid="assign-success"]')).toBeVisible();

    // 验证负责人显示
    await expect(page.locator('[data-testid="customer-owner"]')).toBeVisible();
  });
});

/**
 * 高级CRM场景测试
 */
test.describe('高级CRM场景', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'testuser001');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL('/dashboard');
  });

  test('客户批量导入', async ({ page }) => {
    await page.goto('/crm/customers');
    
    // 点击导入按钮
    await page.click('[data-testid="import-customer-button"]');

    // 验证导入对话框
    await expect(page.locator('[data-testid="import-dialog"]')).toBeVisible();

    // 上传文件
    const fileInput = page.locator('[data-testid="import-file-input"]');
    await fileInput.setInputFiles('tests/data/e2e/customers_import.csv');

    // 点击导入
    await page.click('[data-testid="start-import-button"]');

    // 等待导入完成
    await expect(page.locator('[data-testid="import-progress"]')).toBeVisible();
    await expect(page.locator('[data-testid="import-complete"]')).toBeVisible({ timeout: 30000 });

    // 验证导入结果
    const importResult = await page.locator('[data-testid="import-result"]').textContent();
    expect(importResult).toContain('成功导入');
  });

  test('客户统计报表', async ({ page }) => {
    await page.goto('/crm/customers');
    
    // 点击报表按钮
    await page.click('[data-testid="customer-report-button"]');

    // 选择报表类型
    await page.click('[data-testid="report-type-statistics"]');

    // 选择时间范围
    await page.fill('[data-testid="report-start-date"]', '2026-01-01');
    await page.fill('[data-testid="report-end-date"]', '2026-04-24');

    // 生成报表
    await page.click('[data-testid="generate-report-button"]');

    // 等待报表生成
    await page.waitForSelector('[data-testid="report-container"]', { timeout: 15000 });

    // 验证报表内容
    await expect(page.locator('[data-testid="report-chart"]')).toBeVisible();
    await expect(page.locator('[data-testid="report-statistics"]')).toBeVisible();

    // 验证统计数据
    await expect(page.locator('[data-testid="total-customers"]')).toBeVisible();
    await expect(page.locator('[data-testid="new-customers"]')).toBeVisible();
    await expect(page.locator('[data-testid="active-customers"]')).toBeVisible();
  });

  test('客户跟进历史查看', async ({ page }) => {
    await page.goto('/crm/customers');
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击跟进历史标签
    await page.click('[data-testid="follow-up-history-tab"]');

    // 验证跟进历史列表
    await expect(page.locator('[data-testid="follow-up-history-list"]')).toBeVisible();

    // 验证跟进记录详情
    const followUpRecords = await page.locator('[data-testid="follow-up-record"]').count();
    expect(followUpRecords).toBeGreaterThanOrEqual(0);

    // 如果有记录，验证详情显示
    if (followUpRecords > 0) {
      await page.click('[data-testid="follow-up-record"]:first-child');
      await expect(page.locator('[data-testid="follow-up-detail"]')).toBeVisible();
    }
  });

  test('客户标签管理', async ({ page }) => {
    await page.goto('/crm/customers');
    await page.click('[data-testid="customer-item"]:first-child');
    await page.waitForURL(/\/crm\/customers\/\d+/);

    // 点击添加标签
    await page.click('[data-testid="add-tag-button"]');

    // 选择标签
    await page.click('[data-testid="tag-selector"]');
    await page.click('[data-testid="tag-option-important"]');

    // 提交标签
    await page.click('[data-testid="submit-tag-button"]');

    // 验证标签添加成功
    await expect(page.locator('[data-testid="customer-tag"]')).toBeVisible();
    await expect(page.locator('[data-testid="customer-tag"]')).toContainText('重要客户');
  });

  test('客户合并功能', async ({ page }) => {
    await page.goto('/crm/customers');
    
    // 选择多个客户（假设有重复客户）
    await page.click('[data-testid="customer-checkbox-1"]');
    await page.click('[data-testid="customer-checkbox-2"]');

    // 点击合并按钮
    await page.click('[data-testid="merge-customer-button"]');

    // 验证合并对话框
    await expect(page.locator('[data-testid="merge-dialog"]')).toBeVisible();

    // 选择主客户
    await page.click('[data-testid="primary-customer-selector"]');
    await page.click('[data-testid="primary-customer-1"]');

    // 确认合并
    await page.click('[data-testid="confirm-merge-button"]');

    // 验证合并成功
    await expect(page.locator('[data-testid="merge-success"]')).toBeVisible();
  });
});