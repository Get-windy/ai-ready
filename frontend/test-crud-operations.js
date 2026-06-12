const { chromium } = require('playwright');

async function testCRUDOperations() {
  console.log('🚀 启动浏览器测试CRUD操作...');
  
  const browser = await chromium.launch({ 
    headless: false,
    slowMo: 300
  });
  
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  
  const page = await context.newPage();
  let results = { login: false, product: {}, otherPages: {} };
  
  try {
    // 步骤1: 登录（验证码可选）
    console.log('\n=== 步骤1: 登录系统 ===');
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    
    // 填写表单
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    
    // 验证码可选，填写任意值
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(3000);
    
    const currentUrl = page.url();
    console.log('登录后URL:', currentUrl);
    
    if (!currentUrl.includes('/login')) {
      console.log('✅ 登录成功');
      results.login = true;
    } else {
      console.log('❌ 登录失败');
      
      // 检查错误消息
      const errorMsg = await page.$('.ant-message-error');
      if (errorMsg) {
        const errorText = await errorMsg.textContent();
        console.log('错误信息:', errorText);
      }
      return;
    }
    
    // 步骤2: 产品管理页面
    console.log('\n=== 步骤2: 测试产品管理 ===');
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    const productPageTitle = await page.$('text=产品管理');
    results.product.pageLoaded = !!productPageTitle;
    
    // 测试新增按钮
    console.log('\n--- 测试新增功能 ---');
    const newBtn = await page.$('button:has-text("新增产品")');
    results.product.hasNewBtn = !!newBtn;
    
    if (newBtn) {
      console.log('点击"新增产品"按钮...');
      await page.click('button:has-text("新增产品")');
      await page.waitForTimeout(2000);
      
      const afterUrl = page.url();
      console.log('跳转后URL:', afterUrl);
      results.product.newBtnNavigated = afterUrl.includes('/create') || afterUrl.includes('/add');
      
      // 检查是否有新建表单
      const createForm = await page.$('.ant-form, form');
      results.product.hasCreateForm = !!createForm;
      
      if (createForm) {
        console.log('✅ 新建表单存在，尝试填写...');
        try {
          // 查找输入框
          const codeInput = await page.$('input[placeholder*="编码"]');
          const nameInput = await page.$('input[placeholder*="名称"]');
          
          if (codeInput) await codeInput.fill('TEST-' + Date.now().toString().slice(-6));
          if (nameInput) await nameInput.fill('测试产品');
          results.product.canFillForm = true;
          console.log('✅ 表单填写成功');
        } catch (e) {
          console.log('⚠️ 表单填写失败:', e.message);
        }
      }
      
      // 返回列表
      await page.goto('http://localhost:5173/erp/product');
      await page.waitForTimeout(2000);
    }
    
    // 测试查看功能
    console.log('\n--- 测试查看功能 ---');
    const productLink = await page.$('a:has-text("P001")');
    results.product.hasProductLink = !!productLink;
    
    if (productLink) {
      console.log('点击产品编码P001链接...');
      await productLink.click();
      await page.waitForTimeout(2000);
      
      const detailUrl = page.url();
      console.log('详情页URL:', detailUrl);
      results.product.viewNavigated = detailUrl.includes('/detail') || detailUrl.includes('id=');
      
      // 返回列表
      await page.goto('http://localhost:5173/erp/product');
      await page.waitForTimeout(2000);
    }
    
    // 测试操作列按钮
    console.log('\n--- 测试操作列按钮 ---');
    const viewBtn = await page.$('button:has-text("查看")');
    const editBtn = await page.$('button:has-text("编辑")');
    const deleteBtn = await page.$('button:has-text("删除")');
    
    results.product.hasViewBtn = !!viewBtn;
    results.product.hasEditBtn = !!editBtn;
    results.product.hasDeleteBtn = !!deleteBtn;
    
    console.log('查看按钮:', viewBtn ? '✅' : '❌');
    console.log('编辑按钮:', editBtn ? '✅' : '❌');
    console.log('删除按钮:', deleteBtn ? '✅' : '❌');
    
    // 测试其他ERP列表页
    console.log('\n=== 步骤3: 测试其他ERP模块 ===');
    const otherPages = [
      { name: '客户管理', path: '/erp/customer' },
      { name: '供应商管理', path: '/erp/supplier' },
      { name: '仓库管理', path: '/erp/warehouse' },
      { name: '销售订单', path: '/erp/sale/order' },
      { name: '采购订单', path: '/erp/purchase/order' }
    ];
    
    for (const p of otherPages) {
      console.log(`\n--- ${p.name} ---`);
      await page.goto('http://localhost:5173' + p.path);
      await page.waitForTimeout(2000);
      
      const pageTitle = await page.$(`text=${p.name}`);
      const newBtn = await page.$('button:has-text("新增"), button:has-text("新建")');
      const table = await page.$('.vxe-table, .ant-table');
      
      results.otherPages[p.name] = {
        loaded: !!pageTitle,
        hasNewBtn: !!newBtn,
        hasTable: !!table
      };
      
      console.log(`页面加载: ${pageTitle ? '✅' : '❌'}`);
      console.log(`新增按钮: ${newBtn ? '✅' : '❌'}`);
      console.log(`数据表格: ${table ? '✅' : '❌'}`);
    }
    
    // 汇总
    console.log('\n========================================');
    console.log('📋 CRUD测试结果汇总');
    console.log('========================================');
    console.log(`登录状态: ${results.login ? '✅ 成功' : '❌ 失败'}`);
    console.log('\n产品管理模块:');
    console.log(`  页面加载: ${results.product.pageLoaded ? '✅' : '❌'}`);
    console.log(`  新增按钮: ${results.product.hasNewBtn ? '✅' : '❌'}`);
    console.log(`  新增跳转: ${results.product.newBtnNavigated ? '✅' : '❌'}`);
    console.log(`  新建表单: ${results.product.hasCreateForm ? '✅' : '❌'}`);
    console.log(`  产品链接: ${results.product.hasProductLink ? '✅' : '❌'}`);
    console.log(`  查看跳转: ${results.product.viewNavigated ? '✅' : '❌'}`);
    console.log(`  编辑按钮: ${results.product.hasEditBtn ? '✅' : '❌'}`);
    console.log(`  删除按钮: ${results.product.hasDeleteBtn ? '✅' : '❌'}`);
    console.log('\n其他ERP模块:');
    for (const [name, r] of Object.entries(results.otherPages)) {
      console.log(`  ${name}: 页面${r.loaded ? '✅' : '❌'} 新增${r.hasNewBtn ? '✅' : '❌'} 表格${r.hasTable ? '✅' : '❌'}`);
    }
    console.log('========================================');
    
    await page.waitForTimeout(15000);
    
  } catch (error) {
    console.error('测试出错:', error.message);
  } finally {
    await browser.close();
  }
}

testCRUDOperations();
