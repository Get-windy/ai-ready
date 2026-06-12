const { chromium } = require('playwright');

async function checkTable() {
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
    await page.waitForTimeout(4000);
    
    console.log('\n=== 检查产品表格渲染 ===');
    
    // 等待更长时间让表格加载
    await page.waitForTimeout(3000);
    
    // 截图检查
    await page.screenshot({ path: 'product-page.png', fullPage: true });
    
    // 检查表格结构
    const vxeTable = await page.$('.vxe-table');
    const vxeBody = await page.$('.vxe-table--body');
    const vxeRows = await page.$$('.vxe-body--row');
    
    console.log('vxe-table:', vxeTable ? '✅' : '❌');
    console.log('vxe-table--body:', vxeBody ? '✅' : '❌');
    console.log('数据行数量:', vxeRows.length);
    
    if (vxeRows.length > 0) {
      // 检查第一行的内容
      const firstRow = vxeRows[0];
      const rowText = await firstRow.textContent();
      console.log('第一行内容:', rowText?.substring(0, 100));
      
      // 检查单元格内的链接
      const cellLinks = await firstRow.$$('.cell-link');
      console.log('第一行链接数量:', cellLinks.length);
      
      // 检查操作列
      const actionCol = await firstRow.$('.vxe-body--column:last-child');
      if (actionCol) {
        const actionText = await actionCol.textContent();
        console.log('操作列内容:', actionText);
      }
    }
    
    // 检查 EmptyState
    const emptyState = await page.$('.empty-state, .ant-empty');
    console.log('空状态组件:', emptyState ? '✅' : '❌');
    
    // 检查错误状态
    const errorState = await page.$('.ant-result-warning, .ant-result-error');
    console.log('错误状态:', errorState ? '✅' : '❌');
    
    // 检查 console 错误
    page.on('console', msg => {
      if (msg.type() === 'error') {
        console.log('Console错误:', msg.text());
      }
    });
    
    await page.waitForTimeout(5000);
    
    // 检查往来单位对比
    console.log('\n=== 检查往来单位表格 ===');
    await page.goto('http://localhost:5173/erp/partner');
    await page.waitForTimeout(4000);
    
    const partnerTable = await page.$('.vxe-table');
    const partnerRows = await page.$$('.vxe-body--row');
    console.log('往来单位表格:', partnerTable ? '✅' : '❌');
    console.log('往来单位数据行:', partnerRows.length);
    
    await page.screenshot({ path: 'partner-page.png', fullPage: true });
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

checkTable();
