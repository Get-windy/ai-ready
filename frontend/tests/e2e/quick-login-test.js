/**
 * 快速登录验证测试
 * 验证 Playwright + 真实后端登录流程
 */
const { chromium } = require('playwright');

async function main() {
  console.log('启动浏览器...');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
  });
  const page = await context.newPage();

  // 收集错误
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('  [控制台错误]', msg.text().slice(0, 200));
    }
  });
  page.on('pageerror', error => {
    console.log('  [页面错误]', error.message);
  });

  try {
    // 导航到登录页
    console.log('1. 导航到登录页...');
    await page.goto('http://localhost:5656/login', { waitUntil: 'networkidle' });
    await page.waitForTimeout(2000);

    // 截屏
    await page.screenshot({ path: 'screenshots/login-page.png' });
    console.log('   登录页已加载');

    // 填写表单
    console.log('2. 填写登录表单...');
    const tenantInput = page.locator('input[placeholder="请输入租户名称"]');
    if (await tenantInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await tenantInput.fill('系统租户');
      console.log('   已填写租户名称');
    }

    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    console.log('   已填写用户名');

    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    console.log('   已填写密码');

    const captchaInput = page.locator('input[placeholder="请输入验证码"]');
    if (await captchaInput.isVisible({ timeout: 3000 }).catch(() => false)) {
      await captchaInput.fill('ABCD');
      console.log('   已填写验证码');
    }

    // 点击登录
    console.log('3. 点击登录按钮...');
    await page.click('button[type="submit"]');

    // 等待导航
    console.log('4. 等待登录完成...');
    try {
      await page.waitForURL('**/dashboard**', { timeout: 15000 });
      console.log('   已跳转到仪表盘');
    } catch {
      console.log('   导航超时，当前URL:', page.url());
      await page.screenshot({ path: 'screenshots/login-failed.png' });
    }

    await page.waitForTimeout(3000);

    // 检查当前页面
    console.log('5. 当前页面URL:', page.url());
    const bodyText = await page.locator('body').textContent().catch(() => '') || '';
    console.log('   页面内容长度:', bodyText.length);

    // 检查侧边栏菜单
    const menuItems = page.locator('.ant-menu-item, .ant-menu-submenu-title');
    const menuCount = await menuItems.count();
    console.log('   侧边栏菜单项数:', menuCount);

    // 列出可见菜单
    for (let i = 0; i < menuCount; i++) {
      const text = await menuItems.nth(i).textContent().catch(() => '') || '';
      if (text.trim()) {
        console.log(`   [${i}] ${text.trim().slice(0, 50)}`);
      }
    }

    // 截屏仪表盘
    await page.screenshot({ path: 'screenshots/dashboard.png', fullPage: true });
    console.log('6. 仪表盘截图已保存');

    // 测试多个页面导航
    console.log('\n7. 测试页面导航...');

    // 尝试导航到几个关键页面
    const testRoutes = [
      '/dashboard',
      '/sale',
      '/purchase',
      '/erp/product',
      '/system/user',
    ];

    for (const route of testRoutes) {
      console.log(`\n   导航到 ${route}...`);
      await page.goto(`http://localhost:5656${route}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(3000);
      const title = await page.title().catch(() => '');
      const currentUrl = page.url();
      console.log(`   标题: ${title}`);
      console.log(`   URL: ${currentUrl}`);

      // 检查页面是否正常
      const hasError = await page.locator('.ant-alert-error, .ant-result-error').isVisible().catch(() => false);
      if (hasError) {
        console.log('   ⚠️ 页面显示错误提示');
      }

      // 截屏
      await page.screenshot({ path: `screenshots/route-${route.replace(/\//g, '-')}.png`, fullPage: true });
    }

    console.log('\n✅ 测试完成');
  } catch (e) {
    console.error('❌ 错误:', e);
  } finally {
    await browser.close();
  }
}

main();
