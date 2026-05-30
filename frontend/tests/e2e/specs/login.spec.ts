import { test, expect, ADMIN_CREDENTIALS } from '../fixtures/auth.fixture';
import {
  setupAuthApiMocks,
  loginViaUi,
  setAuthToken,
  clearAuthToken,
} from '../fixtures/auth.fixture';

test.describe('登录页面', () => {
  test('显示登录页面', async ({ mockedPage }) => {
    const page = mockedPage;

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

  test('验证必填字段校验', async ({ mockedPage }) => {
    const page = mockedPage;

    await page.goto('/login');

    // 不填写任何内容直接点击提交
    await page.click('button[type="submit"]');

    // 检查 Ant Design 表单验证错误提示
    // 用户名是必填的
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入用户名' })).toBeVisible();
    // 密码是必填的
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入密码' })).toBeVisible();
    // 验证码是必填的
    await expect(page.locator('.ant-form-item-explain-error').filter({ hasText: '请输入验证码' })).toBeVisible();
  });

  test('成功登录后跳转到 dashboard', async ({ page }) => {
    // 独立设置 Mock（不使用 fixture，更精细控制）
    await setupAuthApiMocks(page, 'admin');

    await loginViaUi(page, ADMIN_CREDENTIALS.username, ADMIN_CREDENTIALS.password);

    // 验证 URL 跳转到 dashboard
    expect(page.url()).toContain('/dashboard');

    // 验证 token 已存储到 localStorage
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeTruthy();
  });

  test('登录失败显示错误消息', async ({ page }) => {
    // 独立设置 Mock
    await setupAuthApiMocks(page, 'admin');

    await page.goto('/login');
    await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 });

    // 填写错误的凭据
    await page.fill('input[placeholder="请输入用户名"]', ADMIN_CREDENTIALS.username);
    await page.fill('input[placeholder="请输入密码"]', 'WrongPassword');
    await page.fill('input[placeholder="请输入验证码"]', 'ABCD');

    // 提交
    await page.click('button[type="submit"]');

    // 验证出现错误消息（Ant Design message 组件）
    // 由于 mock 返回 code 500，axios 拦截器会显示 message.error
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });
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
    // 设置 Mock
    await setupAuthApiMocks(page, 'admin');

    // 先完成登录
    await setAuthToken(page, 'admin');
    await page.goto('/dashboard');
    await page.waitForSelector('.basic-layout', { timeout: 15_000 });

    // 点击右上角用户头像区域打开下拉菜单
    await page.locator('.user-info').click();

    // 等待下拉菜单出现，点击"退出登录"
    await page.locator('.ant-dropdown-menu-item').filter({ hasText: '退出登录' }).click();

    // 验证跳转到登录页
    await page.waitForURL('**/login**', { timeout: 10_000 });
    expect(page.url()).toContain('/login');

    // 验证 token 已清除
    const token = await page.evaluate(() => localStorage.getItem('token'));
    expect(token).toBeFalsy();
  });
});
