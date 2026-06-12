const { chromium } = require('playwright');

async function deepCheck() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(3000);
    
    // 登录
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(5000);
    
    // 深度检查
    const deepData = await page.evaluate(() => {
      const app = document.querySelector('#app');
      if (!app) return { error: 'no #app' };
      
      // Vue 3 使用 __vue_app__
      const vueApp = app.__vue_app__;
      if (!vueApp) return { error: 'no __vue_app__' };
      
      // 获取 provides
      const provides = vueApp._context?.provides || {};
      
      // Pinia 的 key 可能是 Symbol 或字符串
      const piniaKeys = Object.getOwnPropertySymbols(provides);
      const stringKeys = Object.keys(provides);
      
      // 查找 pinia
      let pinia = null;
      for (const key of [...stringKeys, ...piniaKeys]) {
        const value = provides[key];
        if (value && typeof value === 'object' && value.state) {
          pinia = value;
          break;
        }
      }
      
      return {
        providesKeys: stringKeys,
        providesSymbolKeys: piniaKeys.map(s => s.toString()),
        foundPinia: !!pinia,
        piniaState: pinia?.state?.value,
        piniaStores: pinia ? (pinia._s ? Array.from(pinia._s.keys()) : null) : null
      };
    });
    
    console.log('深度检查结果:');
    console.log('  providesKeys:', deepData.providesKeys);
    console.log('  providesSymbolKeys:', deepData.providesSymbolKeys);
    console.log('  foundPinia:', deepData.foundPinia);
    console.log('  piniaStores:', deepData.piniaStores);
    
    if (deepData.piniaState) {
      console.log('  piniaState keys:', Object.keys(deepData.piniaState));
      if (deepData.piniaState.user) {
        console.log('  user store:');
        console.log('    permissions:', deepData.piniaState.user.permissions);
        console.log('    roles:', deepData.piniaState.user.roles);
      }
    }
    
    // 直接使用 useUserStore
    const userStoreData = await page.evaluate(() => {
      try {
        // 尝试导入并使用 userStore
        // 在浏览器环境中，这可能不工作，但我们可以检查 localStorage
        const stored = localStorage.getItem('user-store');
        const parsed = stored ? JSON.parse(stored) : null;
        
        return {
          localStorageUserStore: parsed,
          localStorageToken: localStorage.getItem('token'),
          localStorageTenantId: localStorage.getItem('tenantId')
        };
      } catch (e) {
        return { error: e.message };
      }
    });
    
    console.log('\nlocalStorage 数据:');
    console.log('  user-store:', JSON.stringify(userStoreData.localStorageUserStore, null, 2));
    console.log('  token:', userStoreData.localStorageToken ? '存在' : '不存在');
    console.log('  tenantId:', userStoreData.localStorageTenantId);
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

deepCheck();
