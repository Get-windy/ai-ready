const { chromium } = require('playwright');

async function debugUserInfo() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // 监听 userinfo 响应
  page.on('response', async (response) => {
    if (response.url().includes('/auth/userinfo')) {
      try {
        const data = await response.json();
        console.log('\n=== userinfo API 响应 ===');
        console.log('完整响应:', JSON.stringify(data, null, 2));
        console.log('data字段:', JSON.stringify(data.data, null, 2));
      } catch (e) {
        console.log('解析响应失败:', e.message);
      }
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
    
    await page.waitForTimeout(10000);
    
    // 手动调用 getUserInfo
    const result = await page.evaluate(async () => {
      try {
        // 直接 fetch userinfo
        const token = localStorage.getItem('token');
        const response = await fetch('/api/auth/userinfo', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'tenantId': '1'
          }
        });
        const json = await response.json();
        return {
          apiResponse: json,
          storeAfter: null // 需要检查
        };
      } catch (e) {
        return { error: e.message };
      }
    });
    
    console.log('\n=== 手动调用 userinfo ===');
    console.log(JSON.stringify(result, null, 2));
    
    await page.waitForTimeout(5000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

debugUserInfo();
