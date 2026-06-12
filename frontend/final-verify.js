const { chromium } = require('playwright');

async function finalVerify() {
  const browser = await chromium.launch({ headless: false, slowMo: 200 });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  page.on('console', msg => {
    if (msg.text().includes('permission') || msg.text().includes('userStore')) {
      console.log('[浏览器]', msg.text());
    }
  });
  
  try {
    // 登录
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(8000);
    
    // 产品页面
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(8000);
    
    // 截图
    await page.screenshot({ path: 'product-page-final.png' });
    
    // 检查渲染内容
    const renderState = await page.evaluate(() => {
      // 检查表格
      const table = document.querySelector('.vxe-table');
      const rows = document.querySelectorAll('.vxe-body--row');
      
      // 检查链接
      const links = document.querySelectorAll('.cell-link');
      
      // 检查按钮（更精确的选择器）
      const allButtons = document.querySelectorAll('button');
      const viewBtns = document.querySelectorAll('button');
      const actionButtons = [];
      
      allButtons.forEach(btn => {
        const text = btn.textContent.trim();
        if (text === '查看' || text === '编辑' || text === '删除' || text === '启用' || text === '停用') {
          actionButtons.push({
            text,
            display: btn.style.display,
            visible: btn.offsetParent !== null,
            parent: btn.parentElement?.className
          });
        }
      });
      
      return {
        hasTable: !!table,
        rowCount: rows.length,
        linkCount: links.length,
        actionButtons,
        linkTexts: Array.from(links).slice(0, 5).map(l => l.textContent)
      };
    });
    
    console.log('\n=== 产品页面渲染状态 ===');
    console.log('表格存在:', renderState.hasTable);
    console.log('数据行数量:', renderState.rowCount);
    console.log('链接数量:', renderState.linkCount);
    console.log('链接文本:', renderState.linkTexts);
    console.log('操作按钮:', renderState.actionButtons.length);
    
    if (renderState.actionButtons.length > 0) {
      renderState.actionButtons.forEach(btn => {
        console.log(`  "${btn.text}": display="${btn.display}", visible=${btn.visible}`);
      });
    }
    
    // 点击产品链接测试
    if (renderState.linkCount > 0) {
      console.log('\n=== 测试产品链接 ===');
      const firstLink = await page.$('.cell-link');
      if (firstLink) {
        await firstLink.click();
        await page.waitForTimeout(3000);
        const detailUrl = page.url();
        console.log('点击后URL:', detailUrl);
        console.log('跳转到详情页:', detailUrl.match(/\/erp\/product\/\d+/) ? '✅' : '❌');
      }
    }
    
    // 返回并测试新增
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    console.log('\n=== 测试新增按钮 ===');
    const newBtn = await page.$('button:has-text("新增产品")');
    if (newBtn) {
      await newBtn.click();
      await page.waitForTimeout(3000);
      const createUrl = page.url();
      console.log('点击后URL:', createUrl);
      console.log('跳转到创建页:', createUrl.includes('/create') ? '✅' : '❌');
    }
    
    // 最终汇总
    console.log('\n========================================');
    console.log('📋 CRUD 功能最终验证');
    console.log('========================================');
    console.log('数据表格:', renderState.hasTable && renderState.rowCount > 0 ? '✅' : '❌');
    console.log('产品链接:', renderState.linkCount > 0 ? '✅' : '❌');
    console.log('操作按钮:', renderState.actionButtons.length > 0 ? '✅' : '❌');
    console.log('新增按钮:', newBtn ? '✅' : '❌');
    console.log('========================================');
    
    await page.waitForTimeout(15000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

finalVerify();
