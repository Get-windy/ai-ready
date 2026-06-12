import { chromium } from 'playwright';
import fs from 'fs';

//第一批测试页面（核心模块）
const pages = [
  { path: '/dashboard/dashboard', name: '工作台', category: '工作台' },
  { path: '/erp/product', name: '产品管理', category: '产品数据' },
  { path: '/erp/partner', name: '往来单位管理', category: '产品数据' },
  { path: '/sale', name: '销售订单', category: '销售作业' },
  { path: '/stock', name: '销售出库', category: '销售作业' },
  { path: '/erp/shipment', name: '发货管理', category: '销售作业' },
  { path: '/purchase', name: '采购订单', category: '采购作业' },
  { path: '/erp/stock-in', name: '入库管理', category: '采购作业' },
  { path: '/system/user', name: '用户管理', category: '系统管理' },
  { path: '/system/role', name: '角色管理', category: '系统管理' },
];

(async () => {
  const browser = await chromium.launch({ headless: false }); // 显示浏览器便于观察
  const context = await browser.newContext({ locale: 'zh-CN' });
  const page = await context.newPage();

  // 收集所有错误
  const allErrors = [];

  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    // 只收集真正的错误，排除调试日志
    if (type === 'error') {
      // 排除开发调试信息
      if (!text.includes('[DEBUG:') && !text.includes('[MockServer]') && !text.includes('[SSE]')) {
        console.log(`[Console Error] ${text}`);
        allErrors.push({ type: 'console', message: text });
      }
    }
  });

  page.on('response', response => {
    const status = response.status();
    const url = response.url();
    if (status >= 400) {
      console.log(`[API Error] ${url} → ${status}`);
      allErrors.push({ type: 'network', url, status });
    }
  });

  page.on('pageerror', error => {
    console.log(`[Page Error] ${error.message}`);
    allErrors.push({ type: 'pageerror', message: error.message });
  });

  console.log('=== 1. 登录 ===');
  await page.goto('http://localhost:5656/login', { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(2000);

  // 填写登录表单
  const inputs = await page.$$('input');
  await inputs[0].fill('系统租户');
  await inputs[1].fill('admin');
  await inputs[2].fill('admin123');
  await inputs[3].fill('ABCD');

  const loginBtn = await page.$('button[type="submit"]');
  await loginBtn.click();
  await page.waitForTimeout(8000);

  const currentUrl = page.url();
  console.log('登录后URL:', currentUrl);

  if (currentUrl.includes('login')) {
    console.log('登录失败，停止测试');
    await browser.close();
    return;
  }

  console.log('登录成功！');

  // 逐页测试
  for (const pageInfo of pages) {
    console.log(`\n=== 测试: ${pageInfo.name} (${pageInfo.path}) ===`);
    allErrors.length = 0; // 清空错误

    try {
      await page.goto(`http://localhost:5656${pageInfo.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await page.waitForTimeout(5000);

      // 截图
      await page.screenshot({ path: `screenshots/${pageInfo.path.replace(/\//g, '_')}.png` });
      console.log('截图已保存');

      // 检查页面元素
      const title = await page.title();
      console.log('页面标题:', title);

      // 输出本页错误
      if (allErrors.length > 0) {
        console.log(`发现 ${allErrors.length} 个错误:`);
        for (const err of allErrors) {
          console.log(`  - ${err.type}: ${err.message || err.url}`);
        }
      } else {
        console.log('无错误');
      }

    } catch (error) {
      console.log(`页面访问失败: ${error.message}`);
    }

    await page.waitForTimeout(1000);
  }

  console.log('\n=== 测试完成 ===');
  await browser.close();
})();