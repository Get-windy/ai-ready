const { chromium } = require('playwright');

async function checkPermissions() {
  const browser = await chromium.launch({ headless: false, slowMo: 100 });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  
  try {
    // 登录
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(3000);
    
    // 检查 localStorage 中的权限
    const permissions = await page.evaluate(() => {
      const userStoreStr = localStorage.getItem('user-store');
      if (userStoreStr) {
        try {
          const parsed = JSON.parse(userStoreStr);
          return {
            permissions: parsed.permissions || [],
            roles: parsed.roles || [],
            userId: parsed.userId
          };
        } catch (e) {
          return { error: e.message };
        }
      }
      return { error: 'no user-store in localStorage' };
    });
    
    console.log('\n=== 用户权限信息 ===');
    console.log('用户ID:', permissions.userId);
    console.log('角色:', permissions.roles);
    console.log('权限数量:', permissions.permissions?.length || 0);
    console.log('权限列表 (前20个):', permissions.permissions?.slice(0, 20));
    
    // 检查是否有产品相关权限
    const productPerms = permissions.permissions?.filter(p => p.includes('product'));
    console.log('\n产品相关权限:', productPerms);
    
    // 检查是否有 erp 相关权限
    const erpPerms = permissions.permissions?.filter(p => p.includes('erp'));
    console.log('\nERP相关权限:', erpPerms?.slice(0, 10));
    
    // 检查是否有通配符权限
    const wildcardPerms = permissions.permissions?.filter(p => p.includes('*'));
    console.log('\n通配符权限:', wildcardPerms);
    
    // 产品管理页面
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    // 检查操作按钮状态
    const editBtn = await page.$('button:has-text("编辑")');
    const deleteBtn = await page.$('button:has-text("删除")');
    const viewBtn = await page.$('button:has-text("查看")');
    
    console.log('\n=== 操作按钮状态 ===');
    console.log('查看按钮:', viewBtn ? '存在' : '不存在 (可能被权限隐藏)');
    console.log('编辑按钮:', editBtn ? '存在' : '不存在');
    console.log('删除按钮:', deleteBtn ? '存在' : '不存在');
    
    if (editBtn) {
      const isDisabled = await editBtn.getAttribute('disabled');
      console.log('编辑按钮是否禁用:', isDisabled ? '是' : '否');
      
      // 尝试获取按钮的 class
      const btnClass = await editBtn.getAttribute('class');
      console.log('编辑按钮class:', btnClass);
    }
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

checkPermissions();
