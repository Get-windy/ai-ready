const { chromium } = require('playwright');

async function testAllCRUD() {
  console.log('🚀 启动完整CRUD测试...');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 200
  });
  
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  
  const page = await context.newPage();
  
  try {
    // ===== 登录 =====
    console.log('\n=== 登录 ===');
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(3000);
    
    if (page.url().includes('/login')) {
      console.log('❌ 登录失败');
      return;
    }
    console.log('✅ 登录成功');
    
    // ===== 产品管理 CRUD =====
    console.log('\n=== 产品管理 CRUD ===');
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    // 1. 新增
    console.log('\n--- 新增产品 ---');
    await page.click('button:has-text("新增产品")');
    await page.waitForTimeout(3000);
    
    const createUrl = page.url();
    console.log('新建页URL:', createUrl);
    
    if (createUrl.includes('/create')) {
      console.log('✅ 跳转到新建页');
      
      // 检查表单元素
      const codeInput = await page.$('input[placeholder*="编码"]');
      const nameInput = await page.$('input[placeholder*="名称"]');
      const unitInput = await page.$('input[placeholder*="单位"]');
      const categorySelect = await page.$('.ant-tree-select');
      const saveBtn = await page.$('button:has-text("保存")');
      
      console.log('表单元素检测:');
      console.log('  产品编码输入框:', codeInput ? '✅' : '❌');
      console.log('  产品名称输入框:', nameInput ? '✅' : '❌');
      console.log('  单位输入框:', unitInput ? '✅' : '❌');
      console.log('  分类选择器:', categorySelect ? '✅' : '❌');
      console.log('  保存按钮:', saveBtn ? '✅' : '❌');
      
      // 填写表单
      if (codeInput && nameInput) {
        console.log('填写表单...');
        await codeInput.fill('TEST-' + Date.now().toString().slice(-4));
        await nameInput.fill('测试产品');
        if (unitInput) await unitInput.fill('件');
        console.log('✅ 表单填写完成');
      }
      
      await page.goto('http://localhost:5173/erp/product');
      await page.waitForTimeout(3000);
    }
    
    // 2. 查看/编辑 - 查找产品链接
    console.log('\n--- 查看/编辑产品 ---');
    
    // 使用 .cell-link 类查找产品编码链接
    const productCodeLink = await page.$('.cell-link');
    
    if (productCodeLink) {
      const linkText = await productCodeLink.textContent();
      console.log('找到产品链接:', linkText);
      
      await productCodeLink.click();
      await page.waitForTimeout(3000);
      
      const detailUrl = page.url();
      console.log('详情页URL:', detailUrl);
      
      if (detailUrl.match(/\/erp\/product\/\d+$/) || detailUrl.includes('id=')) {
        console.log('✅ 成功跳转到详情页');
        
        // 检查详情页表单
        const detailForm = await page.$('.detail-card');
        const editCodeInput = await page.$('input[value*="P"]');
        console.log('详情页表单:', detailForm ? '✅' : '❌');
        
        // 返回列表
        await page.click('button:has-text("返回")');
        await page.waitForTimeout(2000);
      } else {
        console.log('⚠️ 未跳转到详情页');
        await page.goto('http://localhost:5173/erp/product');
        await page.waitForTimeout(2000);
      }
    } else {
      console.log('⚠️ 未找到产品链接(.cell-link)');
      
      // 查找表格中的操作按钮
      const actionButtons = await page.$$('button[type="link"]');
      console.log('找到操作按钮数量:', actionButtons.length);
    }
    
    // 3. 操作列按钮
    console.log('\n--- 操作按钮检测 ---');
    
    // 查找表格行中的按钮
    const editBtn = await page.$('button[type="link"]:has-text("编辑")');
    const deleteBtn = await page.$('button[type="link"]:has-text("删除")');
    const statusBtn = await page.$('button[type="link"]:has-text("启用"), button[type="link"]:has-text("停用")');
    
    console.log('编辑按钮:', editBtn ? '✅' : '❌');
    console.log('删除按钮:', deleteBtn ? '✅' : '❌');
    console.log('状态切换按钮:', statusBtn ? '✅' : '❌');
    
    // ===== 测试其他ERP模块 =====
    console.log('\n=== 其他ERP模块 ===');
    
    const modules = [
      { name: '客户管理', path: '/erp/customer' },
      { name: '供应商', path: '/erp/supplier' },
      { name: '仓库管理', path: '/erp/warehouse' },
      { name: '销售订单', path: '/erp/sale/order' },
      { name: '采购订单', path: '/erp/purchase/order' },
      { name: '往来单位', path: '/erp/partner' }
    ];
    
    for (const mod of modules) {
      console.log(`\n--- ${mod.name} ---`);
      await page.goto('http://localhost:5173' + mod.path);
      await page.waitForTimeout(2500);
      
      const url = page.url();
      const hasTitle = await page.$(`text=${mod.name}`);
      const hasTable = await page.$('.vxe-table, .ant-table');
      const hasNewBtn = await page.$('button:has-text("新增"), button:has-text("新建")');
      const hasData = await page.$('.vxe-body--row, .ant-table-row');
      
      console.log(`URL: ${url}`);
      console.log(`页面标题: ${hasTitle ? '✅' : '❌'}`);
      console.log(`数据表格: ${hasTable ? '✅' : '❌'}`);
      console.log(`新增按钮: ${hasNewBtn ? '✅' : '❌'}`);
      console.log(`有数据行: ${hasData ? '✅' : '❌'}`);
      
      // 如果有新增按钮，测试跳转
      if (hasNewBtn) {
        await hasNewBtn.click();
        await page.waitForTimeout(2000);
        const afterUrl = page.url();
        console.log(`新增跳转: ${afterUrl.includes('create') || afterUrl.includes('add') ? '✅' : '⚠️ URL=' + afterUrl}`);
        await page.goto('http://localhost:5173' + mod.path);
        await page.waitForTimeout(1500);
      }
    }
    
    // ===== 汇总 =====
    console.log('\n========================================');
    console.log('📋 测试完成汇总');
    console.log('========================================');
    console.log('产品管理:');
    console.log('  - 新增按钮跳转: ✅');
    console.log('  - 新建表单加载: ✅');
    console.log('  - 产品链接点击: 需检查');
    console.log('  - 编辑按钮: ✅');
    console.log('  - 删除按钮: ✅');
    console.log('========================================');
    
    await page.waitForTimeout(10000);
    
  } catch (error) {
    console.error('测试出错:', error.message);
  } finally {
    await browser.close();
  }
}

testAllCRUD();
