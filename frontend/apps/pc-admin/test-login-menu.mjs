import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ locale: 'zh-CN' });
  const page = await context.newPage();

  page.on('console', msg => {
    if (msg.type() === 'error' || msg.type() === 'warning') {
      console.log('[Console]', msg.type(), msg.text());
    }
  });

  page.on('response', response => {
    if (response.url().includes('/api/') && response.status() >= 400) {
      console.log('[API Error]', response.url(), response.status());
    }
  });

  console.log('1. Navigating to login page...');
  await page.goto('http://localhost:5656/login', { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  // 获取验证码
  console.log('2. Getting captcha...');
  const captchaResponse = await page.evaluate(async () => {
    const resp = await fetch('/api/auth/captcha');
    return await resp.json();
  });
  console.log('   Captcha UUID:', captchaResponse.data?.uuid);

  // 输入框顺序：租户名称、用户名、密码、验证码
  const inputs = await page.$$('input');
  console.log('3. Found inputs:', inputs.length);

  // 填写表单
  console.log('4. Filling form...');
  await inputs[0].fill('系统租户');  // 租户名称
  await inputs[1].fill('admin');  // 用户名
  await inputs[2].fill('admin123');  // 密码

  // 验证码 - 直接输入一个随机值（后端可能不验证）
  await inputs[3].fill('ABCD');  // 验证码

  // 点击登录按钮 - 使用包含空格的文本
  console.log('5. Looking for login button...');
  const loginBtn = await page.$('button[type="submit"]');
  if (loginBtn) {
    const btnText = await loginBtn.textContent();
    console.log('   Button text:', btnText);

    console.log('6. Clicking login button...');
    await loginBtn.click();

    // 等待响应
    await page.waitForTimeout(5000);

    // 检查结果
    const currentUrl = page.url();
    console.log('7. Current URL:', currentUrl);

    // 截图
    await page.screenshot({ path: 'login-result.png', fullPage: true });

    if (!currentUrl.includes('login')) {
      console.log('8. Login successful!');

      // 检查 localStorage
      const token = await page.evaluate(() => localStorage.getItem('token'));
      const menus = await page.evaluate(() => localStorage.getItem('menus'));
      console.log('   Token:', token ? 'exists' : 'null');
      console.log('   Menus:', menus);

      // 检查菜单 API 调用
      await page.waitForTimeout(3000);
      const menuElements = await page.$$('.ant-menu-item, .ant-menu-submenu');
      console.log('   Menu elements found:', menuElements.length);

      // 截图首页
      await page.screenshot({ path: 'homepage.png', fullPage: true });
    } else {
      console.log('8. Login failed, still on login page');

      // 检查是否有错误提示
      const errorMsg = await page.$('.ant-message-error, .ant-alert-error');
      if (errorMsg) {
        const text = await errorMsg.textContent();
        console.log('   Error message:', text);
      }
    }
  }

  await browser.close();
})();