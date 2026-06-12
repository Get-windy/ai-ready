const { chromium } = require('playwright');

async function verifyLinks() {
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
    
    // 产品管理
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(5000);
    
    console.log('\n=== 验证产品表格渲染 ===');
    
    // 检查链接
    const cellLinks = await page.$$('.cell-link');
    console.log('产品链接数量:', cellLinks.length);
    
    if (cellLinks.length > 0) {
      for (let i = 0; i < Math.min(3, cellLinks.length); i++) {
        const linkText = await cellLinks[i].textContent();
        console.log(`链接${i+1}: ${linkText}`);
      }
      console.log('✅ 产品链接渲染成功');
      
      // 点击第一个链接
      await cellLinks[0].click();
      await page.waitForTimeout(3000);
      const detailUrl = page.url();
      console.log('详情页URL:', detailUrl);
      console.log('详情页跳转:', detailUrl.match(/\/erp\/product\/\d+/) ? '✅' : '❌');
    }
    
    // 返回列表页检查操作按钮
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    // 检查操作按钮
    const viewBtns = await page.$$('button:has-text("查看")');
    const editBtns = await page.$$('button:has-text("编辑")');
    const deleteBtns = await page.$$('button:has-text("删除")');
    
    console.log('\n操作按钮数量:');
    console.log('  查看按钮:', viewBtns.length);
    console.log('  编辑按钮:', editBtns.length);
    console.log('  删除按钮:', deleteBtns.length);
    
    // 测试编辑按钮
    if (editBtns.length > 0) {
      console.log('\n测试编辑按钮...');
      await editBtns[0].click();
      await page.waitForTimeout(3000);
      const editUrl = page.url();
      console.log('编辑页URL:', editUrl);
      console.log('编辑页跳转:', editUrl.match(/\/erp\/product\/\d+/) ? '✅' : '❌');
      
      // 检查编辑表单
      const editForm = await page.$('.detail-card');
      console.log('编辑表单:', editForm ? '✅' : '❌');
    }
    
    // 最终汇总
    console.log('\n========================================');
    console.log('📋 CRUD 功能测试结果');
    console.log('========================================');
    console.log(`产品链接渲染: ${cellLinks.length > 0 ? '✅' : '❌'} (${cellLinks.length}个)`);
    console.log(`查看按钮: ${viewBtns.length > 0 ? '✅' : '❌'} (${viewBtns.length}个)`);
    console.log(`编辑按钮: ${editBtns.length > 0 ? '✅' : '❌'} (${editBtns.length}个)`);
    console.log(`删除按钮: ${deleteBtns.length > 0 ? '✅' : '❌'} (${deleteBtns.length}个)`);
    console.log('========================================');
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

verifyLinks();
