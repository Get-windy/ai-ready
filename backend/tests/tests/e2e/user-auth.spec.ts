import { test, expect } from '@playwright/test';

/**
 * AI-Ready 用户注册登录流程 E2E测试
 * Sprint 27+1测试环境配置 - E2E测试脚本开发
 * 
 * 测试场景：
 * 1. 用户注册
 * 2. 用户登录
 * 3. 获取用户信息
 * 4. 用户登出
 */

test.describe('用户注册登录流程', () => {
  
  // 测试配置
  const testUser = {
    username: 'testuser001',
    password: 'Test@123456',
    email: 'testuser001@ai-ready.test',
    userType: 'normal'
  };

  const loginPath = '/login';
  const registerPath = '/register';
  const dashboardPath = '/dashboard';

  test.beforeEach(async ({ page }) => {
    // 设置默认超时
    test.setTimeout(30000);
  });

  /**
   * 测试1: 用户注册
   */
  test('用户可以通过注册页面创建新账户', async ({ page }) => {
    // 访问注册页面
    await page.goto(registerPath);
    
    // 验证注册页面元素存在
    await expect(page.locator('[data-testid="register-form"]')).toBeVisible();
    await expect(page.locator('[name="username"]')).toBeVisible();
    await expect(page.locator('[name="password"]')).toBeVisible();
    await expect(page.locator('[name="email"]')).toBeVisible();

    // 填写注册信息
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', testUser.password);
    await page.fill('[name="email"]', testUser.email);

    // 提交注册
    await page.click('[data-testid="register-submit-button"]');

    // 验证注册成功（跳转到登录页或直接登录）
    await page.waitForURL(/\/(login|dashboard)/, { timeout: 10000 });
    
    // 验证成功消息
    const successMessage = page.locator('[data-testid="success-message"]');
    if (await successMessage.isVisible()) {
      await expect(successMessage).toContainText('注册成功');
    }
  });

  /**
   * 测试2: 用户登录
   */
  test('已注册用户可以通过登录页面登录系统', async ({ page }) => {
    // 访问登录页面
    await page.goto(loginPath);
    
    // 验证登录页面元素存在
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
    await expect(page.locator('[name="username"]')).toBeVisible();
    await expect(page.locator('[name="password"]')).toBeVisible();

    // 填写登录信息
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', testUser.password);

    // 提交登录
    await page.click('[data-testid="login-submit-button"]');

    // 验证登录成功（跳转到Dashboard）
    await page.waitForURL(dashboardPath, { timeout: 10000 });
    
    // 验证Dashboard页面元素
    await expect(page.locator('[data-testid="user-menu"]')).toBeVisible();
    await expect(page.locator('[data-testid="user-avatar"]')).toBeVisible();
  });

  /**
   * 测试3: 获取用户信息
   */
  test('登录用户可以查看自己的用户信息', async ({ page }) => {
    // 先登录
    await page.goto(loginPath);
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', testUser.password);
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL(dashboardPath);

    // 点击用户菜单
    await page.click('[data-testid="user-menu"]');
    
    // 点击个人信息选项
    await page.click('[data-testid="user-profile-link"]');
    
    // 验证用户信息页面
    await page.waitForURL(/\/profile|\/user-info/);
    await expect(page.locator('[data-testid="user-info-panel"]')).toBeVisible();

    // 验证用户信息显示正确
    await expect(page.locator('[data-testid="user-username"]')).toContainText(testUser.username);
    await expect(page.locator('[data-testid="user-email"]')).toContainText(testUser.email);
  });

  /**
   * 测试4: 用户登出
   */
  test('登录用户可以成功登出系统', async ({ page }) => {
    // 先登录
    await page.goto(loginPath);
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', testUser.password);
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL(dashboardPath);

    // 点击用户菜单
    await page.click('[data-testid="user-menu"]');
    
    // 点击登出选项
    await page.click('[data-testid="logout-button"]');
    
    // 验证登出成功（跳转到登录页）
    await page.waitForURL(loginPath);
    
    // 验证登出成功消息
    const logoutMessage = page.locator('[data-testid="logout-message"]');
    if (await logoutMessage.isVisible()) {
      await expect(logoutMessage).toContainText('登出成功');
    }

    // 验证无法访问Dashboard
    await page.goto(dashboardPath);
    await page.waitForURL(loginPath);
  });

  /**
   * 测试5: 登录失败处理
   */
  test('使用错误的密码登录会显示错误提示', async ({ page }) => {
    await page.goto(loginPath);
    
    // 填写正确的用户名但错误的密码
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', 'WrongPassword123');

    // 提交登录
    await page.click('[data-testid="login-submit-button"]');

    // 验证错误提示
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('[data-testid="error-message"]')).toContainText(/密码错误|登录失败/);
    
    // 验证仍然在登录页
    expect(page.url()).toContain(loginPath);
  });

  /**
   * 测试6: 表单验证
   */
  test('登录表单会验证必填字段', async ({ page }) => {
    await page.goto(loginPath);
    
    // 不填写任何信息直接提交
    await page.click('[data-testid="login-submit-button"]');

    // 验证表单验证错误
    await expect(page.locator('[data-testid="validation-error"]')).toBeVisible();
    
    // 验证用户名错误提示
    const usernameError = page.locator('[data-testid="username-error"]');
    if (await usernameError.isVisible()) {
      await expect(usernameError).toContainText(/请输入|必填/);
    }
    
    // 验证密码错误提示
    const passwordError = page.locator('[data-testid="password-error"]');
    if (await passwordError.isVisible()) {
      await expect(passwordError).toContainText(/请输入|必填/);
    }
  });

  /**
   * 测试7: 密码可见性切换
   */
  test('用户可以切换密码的可见性', async ({ page }) => {
    await page.goto(loginPath);
    
    // 输入密码
    await page.fill('[name="password"]', testUser.password);
    
    // 验证密码默认隐藏
    const passwordInput = page.locator('[name="password"]');
    expect(await passwordInput.getAttribute('type')).toBe('password');

    // 点击显示密码按钮
    await page.click('[data-testid="toggle-password-visibility"]');
    
    // 验证密码显示
    expect(await passwordInput.getAttribute('type')).toBe('text');

    // 再次点击隐藏密码
    await page.click('[data-testid="toggle-password-visibility"]');
    expect(await passwordInput.getAttribute('type')).toBe('password');
  });

  /**
   * 测试8: 记住登录状态
   */
  test('用户可以选择记住登录状态', async ({ page, context }) => {
    await page.goto(loginPath);
    
    // 勾选记住登录
    await page.check('[data-testid="remember-me-checkbox"]');
    
    // 登录
    await page.fill('[name="username"]', testUser.username);
    await page.fill('[name="password"]', testUser.password);
    await page.click('[data-testid="login-submit-button"]');
    await page.waitForURL(dashboardPath);

    // 关闭页面
    await page.close();

    // 打开新页面验证登录状态保持
    const newPage = await context.newPage();
    await newPage.goto(dashboardPath);
    
    // 验证直接进入Dashboard（未跳转到登录页）
    await expect(newPage.locator('[data-testid="user-menu"]')).toBeVisible({ timeout: 5000 });
  });
});

/**
 * 高级登录场景测试
 */
test.describe('高级登录场景', () => {
  
  test('用户登录后会跳转到之前的页面', async ({ page }) => {
    // 先访问一个需要登录的页面
    await page.goto('/orders');
    await page.waitForURL(loginPath);

    // 登录
    await page.fill('[name="username"]', 'testuser001');
    await page.fill('[name="password"]', 'Test@123456');
    await page.click('[data-testid="login-submit-button"]');

    // 验证跳转回订单页面
    await page.waitForURL('/orders');
  });

  test('登录页面支持多语言', async ({ page }) => {
    // 切换语言
    await page.goto('/login?lang=en');
    await expect(page.locator('[data-testid="login-title"]')).toContainText('Login');
    
    // 切换到中文
    await page.goto('/login?lang=zh');
    await expect(page.locator('[data-testid="login-title"]')).toContainText('登录');
  });

  test('登录页面响应式设计', async ({ page }) => {
    // 测试移动端视图
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/login');
    
    // 验证移动端元素布局
    await expect(page.locator('[data-testid="mobile-login-form"]')).toBeVisible();
    
    // 验证桌面端视图
    await page.setViewportSize({ width: 1280, height: 720 });
    await expect(page.locator('[data-testid="login-form"]')).toBeVisible();
  });
});