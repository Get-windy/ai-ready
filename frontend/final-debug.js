const { chromium } = require('playwright');

async function finalDebug() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(8000);
    
    // 直接从 Vue app 获取 store
    const storeDebug = await page.evaluate(() => {
      // 获取 pinia 实例
      const pinia = window.__pinia__;
      if (!pinia) return { error: 'no pinia' };
      
      const userStoreState = pinia.state.value.user;
      
      // 尝试获取 userStore 实例并检查
      const stores = pinia._s;
      let userStore = null;
      for (const [name, store] of stores) {
        if (name === 'user') {
          userStore = store;
        }
      }
      
      return {
        piniaState: {
          token: userStoreState?.token,
          userId: userStoreState?.userId,
          userInfo: userStoreState?.userInfo,
          permissions: userStoreState?.permissions,
          roles: userStoreState?.roles
        },
        storeInstance: userStore ? {
          hasPermission: typeof userStore.hasPermission,
          isLoggedIn: userStore.isLoggedIn,
          permissions: userStore.permissions,
          roles: userStore.roles
        } : null
      };
    });
    
    console.log('\n=== Store 状态 ===');
    console.log('piniaState:', JSON.stringify(storeDebug.piniaState, null, 2));
    console.log('storeInstance:', JSON.stringify(storeDebug.storeInstance, null, 2));
    
    // 检查 v-permission 指令
    const permissionCheck = await page.evaluate(() => {
      // 模拟权限检查
      const userStore = window.__pinia__?.state.value.user;
      if (!userStore) return { error: 'no store' };
      
      // 检查是否有 "*" 通配符
      const hasWildcard = userStore.permissions?.includes('*');
      
      // 检查 erp:product:view 权限
      const hasViewPerm = userStore.permissions?.includes('erp:product:view') || hasWildcard;
      
      return {
        permissions: userStore.permissions,
        hasWildcard,
        hasViewPerm
      };
    });
    
    console.log('\n=== 权限检查 ===');
    console.log('permissions:', permissionCheck.permissions);
    console.log('hasWildcard:', permissionCheck.hasWildcard);
    console.log('hasViewPerm:', permissionCheck.hasViewPerm);
    
    // 产品页面检查按钮
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(5000);
    
    const buttons = await page.$$eval('.vxe-body--column:last-child button', els => 
      els.map(e => ({ text: e.textContent, disabled: e.disabled, visible: e.offsetParent !== null }))
    );
    
    console.log('\n=== 操作列按钮 ===');
    console.log('按钮数量:', buttons.length);
    if (buttons.length > 0) {
      console.log('第一行按钮:', buttons.slice(0, 5));
    }
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

finalDebug();
