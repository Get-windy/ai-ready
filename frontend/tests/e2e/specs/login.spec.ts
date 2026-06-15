import { test, expect, ADMIN_CREDENTIALS } from '../fixtures/auth.fixture';
import {
  loginViaUi,
  setAuthToken,
  clearAuthToken,
} from '../fixtures/auth.fixture';

/** 真实后端登录凭据（数据库初始化时写入） */
// 使用租户编码 SYSTEM（Spring Cloud Gateway 对含中文的 JSON Body 存在编码解析问题）
// 后端 getTenantIdByName() 同时支持通过租户编码和名称查询
const REAL_CREDENTIALS = {
  username: 'admin',
  password: 'admin123',
  tenantName: 'SYSTEM',
};

test.describe('登录页面', () => {
  // 串行执行，避免同一账户并发登录导致 token 冲突
  test.describe.configure({ mode: 'serial' });
  test('显示登录页面', async ({ page }) => {
    await page.goto('/login');

    // 验证页面标题/logo
    await expect(page.locator('text=企智连·AI-Ready')).toBeVisible();

    // 验证表单元素存在
    await expect(page.locator('input[placeholder="请输入用户名"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入密码"]')).toBeVisible();
    await expect(page.locator('input[placeholder="请输入验证码"]')).toBeVisible();

    // 验证登录按钮存在
    await expect(page.locator('button[type="submit"]')).toBeVisible();

    // 验证注册链接存在
    await expect(page.locator('text=立即注册')).toBeVisible();
  });

  test('验证必填字段校验', async ({ page }) => {
    await page.goto('/login');

    // 不填写任何内容直接点击提交
    await page.click('button[type="submit"]');

    // 检查 Ant Design 表单验证错误提示
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入用户名' })).toBeVisible();
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入密码' })).toBeVisible();
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入验证码' })).toBeVisible();
  });

  test('成功登录后跳转到 dashboard', async ({ page }) => {
    // 真实后端登录（不依赖 mock）
    await loginViaUi(page, REAL_CREDENTIALS.username, REAL_CREDENTIALS.password, REAL_CREDENTIALS.tenantName);

    // 验证 URL 跳转到 dashboard
    expect(page.url()).toContain('/dashboard');

    // 验证 token 已存储到 localStorage
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('登录失败显示错误消息', async ({ page }) => {
    await page.goto('/login');
    await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 });

    // 等待验证码图片加载
    await page.waitForSelector('.captcha-image img', { timeout: 10_000 }).catch(() => {});

    // 填写错误的凭据
    await page.fill('input[placeholder="请输入租户名称"]', REAL_CREDENTIALS.tenantName);
    await page.fill('input[placeholder="请输入用户名"]', 'wronguser');
    await page.fill('input[placeholder="请输入密码"]', 'WrongPassword');
    await page.fill('input[placeholder="请输入验证码"]', 'ABCD');

    // 提交
    await page.click('button[type="submit"]');

    // 验证出现错误消息（Ant Design message 组件）
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 10_000 });
  });

  test('未认证用户访问受保护页面跳转到登录页', async ({ page }) => {
    // 不设置 token，直接访问受保护的 dashboard
    await page.goto('/dashboard');

    // 应该被路由守卫重定向到 /login
    await page.waitForURL('**/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/login');

    // 验证 query 参数中包含 redirect
    expect(page.url()).toContain('redirect=');
  });

  test('退出登录流程正确', async ({ page }) => {
    // 通过 UI 完成真实登录
    await loginViaUi(page, REAL_CREDENTIALS.username, REAL_CREDENTIALS.password, REAL_CREDENTIALS.tenantName);
    await page.waitForSelector('.basic-layout', { timeout: 20_000 });

    // 点击右上角用户菜单按钮（可能使用不同 class）
    await page.locator('button:has-text("User menu"), .user-info, .header-right .ant-dropdown-link').first().click();

    // 等待下拉菜单出现，点击"退出登录"（支持中英文）
    await page.locator('.ant-dropdown-menu-item').filter({ hasText: /退出登录|Logout|logout/ }).click();

    // 验证跳转到登录页
    await page.waitForURL('**/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/login');

    // 验证 token 已清除
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeFalsy();
  });
});
