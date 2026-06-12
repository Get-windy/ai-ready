import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ locale: 'zh-CN' });
  const page = await context.newPage();

  // 拦截网络请求，记录菜单API响应
  page.on('response', async response => {
    if (response.url().includes('/api/menu/user/client/pc-admin')) {
      try {
        const data = await response.json();
        console.log('\n=== 菜单API响应 ===');
        console.log('URL:', response.url());
        console.log('Status:', response.status());
        console.log('Response:', JSON.stringify(data, null, 2));
        console.log('菜单数量:', data.data?.length || 0);
        if (data.data && data.data.length > 0) {
          console.log('第一个菜单:', JSON.stringify(data.data[0], null, 2));
        }
      } catch (e) {
        console.log('解析菜单响应失败:', e.message);
      }
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

  // 填写表单
  console.log('3. Filling form...');
  const inputs = await page.$$('input');
  await inputs[0].fill('系统租户');  // 租户名称
  await inputs[1].fill('admin');  // 用户名
  await inputs[2].fill('admin123');  // 密码
  await inputs[3].fill('ABCD');  // 验证码

  // 点击登录
  console.log('4. Clicking login button...');
  const loginBtn = await page.$('button[type="submit"]');
  await loginBtn.click();

  // 等待菜单API响应
  await page.waitForTimeout(5000);

  console.log('\n5. Checking page state...');
  const currentUrl = page.url();
  console.log('   Current URL:', currentUrl);

  // 检查菜单元素
  const menuItems = await page.$$('.ant-menu-item');
  const subMenus = await page.$$('.ant-menu-submenu');
  const allMenus = await page.$$('.ant-menu-item, .ant-menu-submenu');
  console.log('   Menu items:', menuItems.length);
  console.log('   Sub menus:', subMenus.length);
  console.log('   Total menu elements:', allMenus.length);

  // 检查侧边栏
  const sidebar = await page.$('.ant-layout-sider');
  if (sidebar) {
    const sidebarContent = await sidebar.innerHTML();
    console.log('   Sidebar has content:', sidebarContent.length > 100 ? 'Yes' : 'No');
  } else {
    console.log('   Sidebar not found');
  }

  // 截图
  await page.screenshot({ path: 'menu-test-result.png', fullPage: true });
  console.log('\n6. Screenshot saved: menu-test-result.png');

  await browser.close();
})();