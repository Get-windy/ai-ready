const { chromium } = require('playwright');

async function verifyButtons() {
  const browser = await chromium.launch({ headless: false, slowMo: 100 });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  try {
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(5000);
    
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(5000);
    
    // 检查权限指令状态
    const permissionStatus = await page.evaluate(() => {
      const app = document.querySelector('#app')?.__vue_app__;
      if (!app) return { error: 'no app' };
      
      // 获取 pinia
      const pinia = app._context?.provides?.[Symbol.for('pinia')];
      if (!pinia) return { error: 'no pinia' };
      
      const userState = pinia.state.value.user;
      
      // 检查按钮的 display 样式
      const actionCol = document.querySelector('.vxe-body--column:last-child');
      const buttons = actionCol ? actionCol.querySelectorAll('button') : [];
      
      const buttonInfo = Array.from(buttons).map(btn => ({
        text: btn.textContent,
        display: btn.style.display,
        visible: btn.offsetParent !== null,
        classList: btn.classList.toString(),
        hasPermissionDenied: btn.classList.contains('permission-denied')
      }));
      
      return {
        permissions: userState.permissions,
        roles: userState.roles,
        buttonInfo
      };
    });
    
    console.log('\n=== 权限状态 ===');
    console.log('permissions:', permissionStatus.permissions);
    console.log('roles:', permissionStatus.roles);
    
    console.log('\n=== 操作按钮信息 ===');
    console.log('按钮数量:', permissionStatus.buttonInfo?.length || 0);
    if (permissionStatus.buttonInfo && permissionStatus.buttonInfo.length > 0) {
      permissionStatus.buttonInfo.forEach(btn => {
        console.log(`  "${btn.text}": display="${btn.display}", visible=${btn.visible}, hasDenied=${btn.hasPermissionDenied}`);
      });
    }
    
    // 刷新页面，触发 updated 检查
    await page.reload();
    await page.waitForTimeout(5000);
    
    const afterReload = await page.evaluate(() => {
      const actionCol = document.querySelector('.vxe-body--column:last-child');
      const buttons = actionCol ? actionCol.querySelectorAll('button') : [];
      return Array.from(buttons).map(btn => ({
        text: btn.textContent,
        display: btn.style.display,
        visible: btn.offsetParent !== null
      }));
    });
    
    console.log('\n=== 刷新后按钮状态 ===');
    console.log('按钮数量:', afterReload.length);
    afterReload.forEach(btn => {
      console.log(`  "${btn.text}": display="${btn.display}", visible=${btn.visible}`);
    });
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

verifyButtons();
