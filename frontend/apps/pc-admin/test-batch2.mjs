import { chromium } from 'playwright';
import fs from 'fs';

// 第二批测试页面
const pages = [
  { path: '/erp/stock', name: '库存管理(ERP)', category: '仓储作业' },
  { path: '/erp/stocktake', name: '库存盘点', category: '仓储作业' },
  { path: '/erp/batch', name: '批次管理', category: '仓储作业' },
  { path: '/erp/serial', name: '序列号管理', category: '仓储作业' },
  { path: '/crm/customer', name: '客户管理', category: '客户关系' },
  { path: '/crm/lead', name: '线索管理', category: '客户关系' },
  { path: '/crm/opportunity', name: '商机管理', category: '客户关系' },
  { path: '/crm/quotation', name: '报价管理', category: '客户关系' },
  { path: '/crm/contract', name: '合同管理', category: '客户关系' },
  { path: '/finance', name: '财务管理', category: '财务管理' },
];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ locale: 'zh-CN' });
  const page = await context.newPage();

  const allErrors = [];

  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    if (type === 'error') {
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

  const inputs = await page.$$('input');
  await inputs[0].fill('系统租户');
  await inputs[1].fill('admin');
  await inputs[2].fill('admin123');
  await inputs[3].fill('ABCD');

  const loginBtn = await page.$('button[type="submit"]');
  await loginBtn.click();
  await page.waitForTimeout(8000);

  if (page.url().includes('login')) {
    console.log('登录失败');
    await browser.close();
    return;
  }
  console.log('登录成功！');

  // 逐页测试
  for (const pageInfo of pages) {
    console.log(`\n=== 测试: ${pageInfo.name} (${pageInfo.path}) ===`);
    allErrors.length = 0;

    try {
      await page.goto(`http://localhost:5656${pageInfo.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await page.waitForTimeout(5000);

      await page.screenshot({ path: `screenshots/${pageInfo.path.replace(/\//g, '_')}.png` });

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