const { chromium } = require('playwright');

async function checkPinia() {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(2000);
    
    // 检查全局变量
    const globals = await page.evaluate(() => {
      return {
        __pinia__: typeof window.__pinia__,
        __APP__: typeof window.__APP__,
        piniaKeys: window.__pinia__ ? Object.keys(window.__pinia__) : null,
        piniaStateKeys: window.__pinia__?.state ? Object.keys(window.__pinia__.state.value) : null
      };
    });
    
    console.log('全局变量检查:');
    console.log('  __pinia__:', globals.__pinia__);
    console.log('  __APP__:', globals.__APP__);
    console.log('  piniaKeys:', globals.piniaKeys);
    console.log('  piniaStateKeys:', globals.piniaStateKeys);
    
    // 如果没有 __pinia__，尝试其他方式获取
    if (globals.__pinia__ === 'undefined') {
      const appData = await page.evaluate(() => {
        // 尝试从 Vue app 获取
        const app = document.querySelector('#app')?.__vue_app__;
        if (app) {
          const pinia = app._context.provides?.pinia;
          return {
            hasApp: true,
            hasPinia: !!pinia,
            piniaState: pinia?.state?.value
          };
        }
        return { hasApp: false };
      });
      
      console.log('\nVue App 数据:');
      console.log('  hasApp:', appData.hasApp);
      console.log('  hasPinia:', appData.hasPinia);
      if (appData.piniaState) {
        console.log('  piniaState keys:', Object.keys(appData.piniaState));
        if (appData.piniaState.user) {
          console.log('  user state:', JSON.stringify(appData.piniaState.user, null, 2));
        }
      }
    }
    
    await page.waitForTimeout(5000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

checkPinia();
