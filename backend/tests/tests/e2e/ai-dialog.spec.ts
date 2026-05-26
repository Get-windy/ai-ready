import { test, expect } from '@playwright/test';

/**
 * AI智能对话E2E测试
 * 测试AI助手的对话功能和上下文理解
 */

test.describe('AI智能对话流程', () => {
  
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('[name="username"]', 'test_user');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('用户可以通过AI助手查询订单', async ({ page }) => {
    // 打开AI助手
    await page.click('[data-testid="ai-assistant-button"]');
    await page.waitForSelector('[data-testid="ai-chat-dialog"]');

    // 输入查询
    await page.fill('[data-testid="ai-input"]', '查询最近5个订单');
    await page.click('[data-testid="ai-send-button"]');

    // 等待AI响应
    await page.waitForSelector('[data-testid="ai-message"]', { timeout: 10000 });

    // 验证响应包含订单列表
    const aiResponse = await page.textContent('[data-testid="ai-message"]:last-child');
    expect(aiResponse).toContain('订单');
    expect(aiResponse).toContain('ORD-');

    // 验证显示订单卡片
    await expect(page.locator('[data-testid="order-card"]')).toHaveCount(5);
  });

  test('用户可以通过AI助手创建订单', async ({ page }) => {
    await page.click('[data-testid="ai-assistant-button"]');
    
    // 自然语言创建订单
    await page.fill('[data-testid="ai-input"]', '为客户张三创建一个新订单，包含商品A 10件');
    await page.click('[data-testid="ai-send-button"]');

    // 等待确认对话框
    await page.waitForSelector('[data-testid="ai-confirmation-dialog"]');
    
    // 验证订单信息
    const confirmationText = await page.textContent('[data-testid="ai-confirmation-content"]');
    expect(confirmationText).toContain('张三');
    expect(confirmationText).toContain('商品A');
    expect(confirmationText).toContain('10');

    // 确认创建
    await page.click('[data-testid="confirm-button"]');

    // 验证创建成功
    await page.waitForSelector('[data-testid="success-message"]');
    const successText = await page.textContent('[data-testid="success-message"]');
    expect(successText).toContain('订单创建成功');
  });

  test('AI助手可以理解上下文', async ({ page }) => {
    await page.click('[data-testid="ai-assistant-button"]');

    // 第一轮对话
    await page.fill('[data-testid="ai-input"]', '查看客户李四的信息');
    await page.click('[data-testid="ai-send-button"]');
    await page.waitForSelector('[data-testid="ai-message"]', { timeout: 10000 });

    // 第二轮对话（引用上下文）
    await page.fill('[data-testid="ai-input"]', '给他创建一个新订单');
    await page.click('[data-testid="ai-send-button"]');

    // 验证AI理解"他"指的是李四
    await page.waitForSelector('[data-testid="ai-confirmation-dialog"]');
    const confirmationText = await page.textContent('[data-testid="ai-confirmation-content"]');
    expect(confirmationText).toContain('李四');
  });

  test('AI助手可以生成报表', async ({ page }) => {
    await page.click('[data-testid="ai-assistant-button"]');

    // 请求生成报表
    await page.fill('[data-testid="ai-input"]', '生成上个月的销售报表');
    await page.click('[data-testid="ai-send-button"]');

    // 等待报表生成
    await page.waitForSelector('[data-testid="ai-report-container"]', { timeout: 15000 });

    // 验证报表内容
    await expect(page.locator('[data-testid="report-chart"]')).toBeVisible();
    await expect(page.locator('[data-testid="report-data-table"]')).toBeVisible();
  });

  test('AI助手可以处理复杂查询', async ({ page }) => {
    await page.click('[data-testid="ai-assistant-button"]');

    // 复杂查询
    await page.fill('[data-testid="ai-input"]', 
      '找出上个月销售额超过10万的客户，并按金额排序');
    await page.click('[data-testid="ai-send-button"]');

    // 等待响应
    await page.waitForSelector('[data-testid="ai-message"]', { timeout: 15000 });

    // 验证结果
    const aiResponse = await page.textContent('[data-testid="ai-message"]:last-child');
    expect(aiResponse).toContain('客户');
    expect(aiResponse).toContain('销售额');
  });
});

/**
 * AI智能报表E2E测试
 */
test.describe('AI智能报表流程', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'test_user');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('用户可以生成自然语言报表', async ({ page }) => {
    // 进入报表页面
    await page.click('[data-testid="nav-reports"]');
    await page.waitForURL('/reports');

    // 使用自然语言生成报表
    await page.fill('[data-testid="report-query-input"]', 
      '显示2026年第一季度各产品类别的销售额对比');
    await page.click('[data-testid="generate-report-button"]');

    // 等待AI生成报表
    await page.waitForSelector('[data-testid="ai-report-container"]', { timeout: 15000 });

    // 验证报表元素
    await expect(page.locator('[data-testid="report-chart"]')).toBeVisible();
    await expect(page.locator('[data-testid="report-data-table"]')).toBeVisible();
    
    // 验证数据
    const tableText = await page.textContent('[data-testid="report-data-table"]');
    expect(tableText).toContain('销售额');
    expect(tableText).toContain('2026');
  });

  test('AI可以自动发现数据异常', async ({ page }) => {
    await page.click('[data-testid="nav-reports"]');
    
    // 查询可能包含异常的数据
    await page.fill('[data-testid="report-query-input"]', 
      '分析本月销售数据，找出异常');
    await page.click('[data-testid="generate-report-button"]');

    await page.waitForSelector('[data-testid="ai-report-container"]', { timeout: 15000 });

    // 验证异常提示
    const insights = await page.textContent('[data-testid="ai-insights"]');
    expect(insights).toContain('异常');
    expect(insights).toContain('发现');
  });
});
