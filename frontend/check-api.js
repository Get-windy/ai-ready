const { chromium } = require('playwright');

async function checkApi() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // 监听网络请求
  page.on('response', async (response) => {
    if (response.url().includes('/auth/userinfo')) {
      try {
        const data = await response.json();
        console.log('\n=== /auth/userinfo 响应 ===');
        console.log(JSON.stringify(data, null, 2));
      } catch (e) {}
    }
  });
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(5000);
    
    // 直接从页面获取数据
    const storeData = await page.evaluate(() => {
      // 检查 pinia store
      const app = window.__APP__;
      if (app && app.$pinia) {
        const stores = app.$pinia._s;
        for (const [name, store] of stores) {
          if (name === 'user') {
            return {
              permissions: store.permissions,
              roles: store.roles,
              userId: store.userId,
              userInfo: store.userInfo
            };
          }
        }
      }
      return null;
    });
    
    console.log('\n=== Pinia Store 状态 ===');
    console.log(JSON.stringify(storeData, null, 2));
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

checkApi();
