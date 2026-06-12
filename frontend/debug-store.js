const { chromium } = require('playwright');

async function debugStore() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  // 添加控制台日志监听
  page.on('console', msg => console.log('[浏览器]', msg.text()));
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    
    console.log('等待登录完成...');
    await page.waitForURL('**/dashboard**', { timeout: 10000 });
    await page.waitForTimeout(3000);
    
    console.log('\n=== 检查 Pinia Store ===');
    
    // 直接调用 window 方法检查 store
    const storeState = await page.evaluate(() => {
      const pinia = window.__pinia__;
      if (!pinia) return { error: 'pinia not found' };
      
      const userStore = pinia.state.value.user;
      if (!userStore) return { error: 'userStore not found' };
      
      return {
        token: userStore.token,
        userId: userStore.userId,
        tenantId: userStore.tenantId,
        userInfo: userStore.userInfo,
        permissions: userStore.permissions,
        roles: userStore.roles,
        billTypes: userStore.billTypes
      };
    });
    
    console.log('Store 状态:');
    console.log('  token:', storeState.token ? '存在' : '不存在');
    console.log('  userId:', storeState.userId);
    console.log('  tenantId:', storeState.tenantId);
    console.log('  userInfo:', storeState.userInfo);
    console.log('  permissions:', storeState.permissions);
    console.log('  roles:', storeState.roles);
    console.log('  billTypes:', storeState.billTypes);
    
    // 检查 localStorage
    const localStorageData = await page.evaluate(() => {
      return {
        'user-store': localStorage.getItem('user-store'),
        'token': localStorage.getItem('token'),
        'tenantId': localStorage.getItem('tenantId')
      };
    });
    
    console.log('\n=== localStorage ===');
    if (localStorageData['user-store']) {
      const parsed = JSON.parse(localStorageData['user-store']);
      console.log('user-store:', JSON.stringify(parsed, null, 2));
    }
    
    await page.waitForTimeout(15000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

debugStore();
