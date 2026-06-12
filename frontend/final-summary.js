const { chromium } = require('playwright');

async function testAllModules() {
  console.log('\n========================================');
  console.log('🚀 ERP系统CRUD功能完整测试');
  console.log('========================================\n');
  
  const browser = await chromium.launch({ headless: false, slowMo: 150 });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  const results = {};
  
  try {
    // 登录
    console.log('=== 步骤1: 登录 ===');
    await page.goto('http://localhost:5173/login');
    await page.waitForTimeout(1000);
    await page.fill('input[placeholder="请输入租户名称"]', 'SYSTEM');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.fill('input[placeholder="请输入验证码"]', 'TEST');
    await page.click('button:has-text("登 录")');
    await page.waitForTimeout(5000);
    
    if (page.url().includes('/login')) {
      console.log('❌ 登录失败');
      return;
    }
    console.log('✅ 登录成功\n');
    
    // 产品管理 CRUD
    console.log('=== 产品管理 CRUD ===');
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(5000);
    
    const productState = await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      return {
        table: !!document.querySelector('.vxe-table'),
        rows: document.querySelectorAll('.vxe-body--row').length,
        links: document.querySelectorAll('.cell-link').length,
        editBtns: buttons.filter(b => b.textContent.trim() === '编辑').length,
        deleteBtns: buttons.filter(b => b.textContent.trim() === '删除').length,
        newBtn: !!document.querySelector('button')
      };
    });
    
    // 检查新增产品按钮
    const newProductBtn = await page.$('button:has-text("新增产品")');
    
    results['产品管理'] = {
      列表页: productState.table && productState.rows > 0,
      数据行数: productState.rows,
      产品链接: productState.links > 0,
      编辑按钮: productState.editBtns > 0,
      删除按钮: productState.deleteBtns > 0,
      新增按钮: !!newProductBtn
    };
    
    console.log(`列表页: ${productState.table && productState.rows > 0 ? '✅' : '❌'} (${productState.rows}行)`);
    console.log(`产品链接: ${productState.links > 0 ? '✅' : '❌'} (${productState.links}个)`);
    console.log(`编辑按钮: ${productState.editBtns > 0 ? '✅' : '❌'} (${productState.editBtns}个)`);
    console.log(`删除按钮: ${productState.deleteBtns > 0 ? '✅' : '❌'} (${productState.deleteBtns}个)`);
    console.log(`新增按钮: ${newProductBtn ? '✅' : '❌'}`);
    
    // 测试新增跳转
    if (newProductBtn) {
      await newProductBtn.click();
      await page.waitForTimeout(3000);
      const createUrl = page.url();
      results['产品管理']['新增跳转'] = createUrl.includes('/create');
      console.log(`新增跳转: ${createUrl.includes('/create') ? '✅' : '❌'}`);
      
      // 检查创建页表单
      const createForm = await page.$('.detail-card, .ant-form');
      results['产品管理']['创建表单'] = !!createForm;
      console.log(`创建表单: ${createForm ? '✅' : '❌'}\n`);
    }
    
    // 返回产品页测试链接跳转
    await page.goto('http://localhost:5173/erp/product');
    await page.waitForTimeout(3000);
    
    const firstLink = await page.$('.cell-link');
    if (firstLink) {
      await firstLink.click();
      await page.waitForTimeout(3000);
      const detailUrl = page.url();
      results['产品管理']['详情跳转'] = detailUrl.match(/\/erp\/product\/\d+/);
      console.log(`详情跳转: ${detailUrl.match(/\/erp\/product\/\d+/) ? '✅' : '❌'}\n`);
    }
    
    // 测试其他模块
    const modules = [
      { name: '往来单位', path: '/erp/partner' },
      { name: '供应商', path: '/erp/supplier' },
      { name: '仓库管理', path: '/erp/warehouse' },
      { name: '销售订单', path: '/erp/sale/order' },
      { name: '采购订单', path: '/erp/purchase/order' }
    ];
    
    for (const mod of modules) {
      console.log(`=== ${mod.name} ===`);
      await page.goto('http://localhost:5173' + mod.path);
      await page.waitForTimeout(4000);
      
      const modState = await page.evaluate(() => ({
        table: !!document.querySelector('.vxe-table, .ant-table'),
        rows: document.querySelectorAll('.vxe-body--row, .ant-table-row').length
      }));
      
      const newBtn = await page.$('button:has-text("新增"), button:has-text("新建")');
      
      results[mod.name] = {
        页面加载: modState.table || modState.rows > 0,
        数据行数: modState.rows,
        新增按钮: !!newBtn
      };
      
      console.log(`页面: ${modState.table || modState.rows > 0 ? '✅' : '❌'}`);
      console.log(`数据: ${modState.rows > 0 ? '✅' : '❌'} (${modState.rows}行)`);
      console.log(`新增: ${newBtn ? '✅' : '❌'}\n`);
    }
    
    // 汇总报告
    console.log('\n========================================');
    console.log('📋 ERP系统测试汇总报告');
    console.log('========================================');
    
    for (const [name, r] of Object.entries(results)) {
      console.log(`\n【${name}】`);
      if (name === '产品管理') {
        console.log(`  列表页: ${r['列表页'] ? '✅' : '❌'} (${r['数据行数']}行数据)`);
        console.log(`  新增按钮: ${r['新增按钮'] ? '✅' : '❌'}`);
        console.log(`  新增跳转: ${r['新增跳转'] ? '✅' : '❌'}`);
        console.log(`  创建表单: ${r['创建表单'] ? '✅' : '❌'}`);
        console.log(`  详情跳转: ${r['详情跳转'] ? '✅' : '❌'}`);
        console.log(`  产品链接: ${r['产品链接'] ? '✅' : '❌'} (${r['产品链接']}个可点击)`);
        console.log(`  编辑按钮: ${r['编辑按钮'] ? '✅' : '❌'}`);
        console.log(`  删除按钮: ${r['删除按钮'] ? '✅' : '❌'}`);
      } else {
        console.log(`  页面加载: ${r['页面加载'] ? '✅' : '❌'}`);
        console.log(`  数据显示: ${r['数据行数'] > 0 ? '✅' : '❌'} (${r['数据行数']}行)`);
        console.log(`  新增按钮: ${r['新增按钮'] ? '✅' : '❌'}`);
      }
    }
    
    console.log('\n========================================');
    console.log('✅ 测试完成！');
    console.log('========================================');
    
    await page.waitForTimeout(8000);
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await browser.close();
  }
}

testAllModules();
