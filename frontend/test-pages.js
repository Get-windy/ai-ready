const puppeteer = require('C:\\Users\\Administrator\\AppData\\Roaming\\npm\\node_modules\\puppeteer-core');
const path = require('path');

const BACKEND_URL = 'http://localhost:5655';
const FRONTEND_URL = 'http://localhost:5656';
const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

// All pages from the inventory (batches of 10)
const ALL_PAGES = [
  // Batch 1: 基础 + 仪表盘 + 系统管理
  '/login', '/register', '/tenant-register', '/403', '/500', '/dashboard',
  '/system/user', '/system/role', '/system/menu', '/system/permission',
  // Batch 2: 系统管理续 + CRM
  '/system/department', '/system/position', '/system/config', '/system/dict',
  '/system/log', '/system/tenant', '/system/tenant-approval', '/system/data-import',
  '/crm/customer', '/crm/contract',
  // Batch 3: CRM续 + 采购 + 销售
  '/crm/lead', '/crm/opportunity', '/crm/quotation', '/crm/invoice', '/crm/supplier',
  '/purchase', '/erp/purchase', '/erp/purchase-exchange',
  '/sale', '/erp/sale',
  // Batch 4: 销售续 + 库存
  '/erp/sales-analysis', '/erp/sales-report', '/stock',
  '/erp/stock', '/erp/stock-in', '/erp/stocktake', '/erp/return', '/erp/shipment',
  '/erp/batch', '/erp/serial',
  // Batch 5: 库存续
  '/erp/stock-cost-adjust', '/erp/stock-overflow', '/erp/stock-damage',
  '/erp/stock-transfer', '/erp/stock-replenishment', '/erp/stock-alert-config',
  '/erp/stock-bom', '/erp/stock-assemble', '/erp/stock-split',
  // Batch 6: 财务
  '/finance', '/finance/subject', '/finance/voucher', '/finance/report',
  '/finance/receivable', '/finance/payable', '/finance/receipt', '/finance/payment',
  '/finance/pre-receipt', '/finance/pre-payment',
  // Batch 7: 财务续
  '/finance/deposit', '/finance/write-off', '/finance/offset', '/finance/capital-flow',
  '/finance/reconciliation', '/finance/accounts-receivable', '/finance/accounts-payable',
  // Batch 8: 产品 + 单位 + 定价
  '/erp/product', '/erp/product/price-batch', '/erp/product/inventory-mode',
  '/erp/partner', '/supplier', '/supplier/inquiry', '/supplier/performance',
  '/erp/pricing/customer-grade', '/erp/pricing/approval', '/erp/pricing/tiers',
  // Batch 9: 打印 + 固定资产
  '/printing/template', '/printing/chain', '/printing/client', '/printing/task',
  '/fixed-asset', '/fixed-asset/asset', '/fixed-asset/category',
  '/fixed-asset/depreciation', '/fixed-asset/transfer', '/fixed-asset/disposal',
  // Batch 10: 固定资产续 + 费用
  '/fixed-asset/inventory', '/fixed-asset/report', '/fixed-asset/purchase',
  '/erp/expense/application', '/erp/expense/reimbursement', '/erp/expense/approval',
  '/erp/expense/payment', '/erp/expense/statistics',
  // Batch 11: 预算 + 商城
  '/budget', '/budget/template', '/budget/annual', '/budget/adjustment', '/budget/report',
  '/erp/mall/config', '/erp/mall/user-audit', '/erp/mall/banner', '/erp/mall/order',
  '/erp/mall/product',
  // Batch 12: 其他
  '/notification', '/profile', '/order-center',
  '/workflow/instance-monitor', '/workflow/task-management', '/workflow/process-analysis',
  '/charts', '/pricing',
];

const batchNum = parseInt(process.argv[2] || '1');
const batchSize = 10;
const startIdx = (batchNum - 1) * batchSize;
const PAGES = ALL_PAGES.slice(startIdx, startIdx + batchSize);

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function login(page) {
  // Navigate to a page that doesn't redirect first
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle0', timeout: 30000 });
  await sleep(2000);

  console.log('Page title:', await page.title());
  console.log('Current URL:', page.url());

  // Use page.evaluate to call the API and set everything up in-browser
  const loginResult = await page.evaluate(async () => {
    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'admin123', tenantName: 'SYSTEM' })
      });
      const json = await resp.json();
      if (json.code === 200 && json.data && json.data.token) {
        const d = json.data;
        // Set all localStorage keys the app reads
        localStorage.setItem('token', d.token);
        localStorage.setItem('tenantId', String(d.tenantId || 1));
        localStorage.setItem('tenantName', d.tenantName || 'SYSTEM');
        localStorage.setItem('userTenants', JSON.stringify(d.tenants || [{ id: d.tenantId || 1, tenantName: d.tenantName || 'SYSTEM', tenantCode: 'SYSTEM', status: 1 }]));
        localStorage.setItem('userId', String(d.userId || 1));
        // Pinia persisted state
        localStorage.setItem('user-store', JSON.stringify({
          token: d.token,
          userId: d.userId || 1,
          tenantId: d.tenantId || 1,
          tenantName: d.tenantName || '系统租户',
          userTenants: d.tenants || [{ id: d.tenantId || 1, tenantName: d.tenantName || '系统租户', tenantCode: 'SYSTEM', status: 1 }]
        }));
        return { success: true, token: d.token.substring(0, 20) + '...' };
      }
      return { success: false, error: JSON.stringify(json) };
    } catch (e) {
      return { success: false, error: e.message };
    }
  });

  console.log('Login result:', JSON.stringify(loginResult));

  if (!loginResult.success) {
    console.error('Login failed');
    return false;
  }

  // Navigate to dashboard to trigger route guard with valid auth
  // Note: use networkidle2, not networkidle0, because SSE connections keep a persistent
  // connection open and networkidle0 will never fire.
  await page.goto(`${FRONTEND_URL}/dashboard`, { waitUntil: 'networkidle2', timeout: 30000 });
  await sleep(3000);

  // If redirected back to login, the route guard's backend checks failed
  const afterLoginUrl = page.url();
  console.log('After login URL:', afterLoginUrl);

  // Try navigating to a page that doesn't require auth check
  // The route guard might need multiple navigation attempts to stabilize
  if (afterLoginUrl.includes('/login')) {
    // Try a second navigation attempt
    console.log('Redirected to login, trying second navigation...');
    await page.goto(`${FRONTEND_URL}/dashboard`, { waitUntil: 'networkidle2', timeout: 30000 });
    await sleep(3000);
    console.log('After second navigation URL:', page.url());
  }

  return !page.url().includes('/login');
}

async function visitPages(page, startBatch, batchSize) {
  const results = [];

  for (const route of PAGES) {
    const url = `${FRONTEND_URL}${route}`;
    console.log(`\n=== Visiting: ${url} ===`);

    try {
      // Use networkidle2 to account for SSE connections that prevent networkidle0
      await page.goto(url, { waitUntil: 'networkidle2', timeout: 30000 });
      await sleep(2000);

      const pageErrors = [];
      page.on('pageerror', err => {
        pageErrors.push(err.message);
      });
      page.on('console', msg => {
        if (msg.type() === 'error') {
          pageErrors.push(`[CONSOLE] ${msg.text()}`);
        }
      });

      await sleep(1000);

      const currentUrl = page.url();
      const status = pageErrors.length === 0 ? 'OK' : 'ERRORS';

      results.push({
        route,
        url: currentUrl,
        status,
        errors: pageErrors,
        screenshot: `batch${startBatch}_${route.replace(/\//g, '_')}.png`
      });

      if (pageErrors.length > 0) {
        console.log(`  Status: ${status}`);
        console.log(`  Errors (${pageErrors.length}):`);
        pageErrors.forEach(e => console.log(`    - ${e}`));
      } else {
        console.log(`  Status: ${status}`);
      }

      // Take screenshot
      await page.screenshot({
        path: path.join(__dirname, `screenshots/batch${startBatch}_${route.replace(/\//g, '_')}.png`),
        fullPage: true
      });

    } catch (e) {
      results.push({
        route,
        url,
        status: 'FAILED',
        errors: [e.message],
      });
      console.log(`  FAILED: ${e.message}`);
    }
  }

  return results;
}

async function main() {
  const batchNum = parseInt(process.argv[2] || '1');
  const batchSize = 10;

  console.log(`Batch ${batchNum} - Pages ${(batchNum-1)*batchSize+1} to ${batchNum*batchSize}`);
  console.log('='.repeat(60));

  // Ensure screenshots directory exists
  const fs = require('fs');
  const screenshotsDir = path.join(__dirname, 'screenshots');
  if (!fs.existsSync(screenshotsDir)) {
    fs.mkdirSync(screenshotsDir, { recursive: true });
  }

  // Check backend is up
  try {
    const http = require('http');
    await new Promise((resolve, reject) => {
      http.get(`${BACKEND_URL}/api/profile`, (res) => {
        console.log(`Backend status: ${res.statusCode}`);
        resolve();
      }).on('error', (err) => {
        console.error(`Backend check failed: ${err.message}`);
        reject(err);
      });
    });
  } catch (e) {
    console.error('Backend is not available. Aborting.');
    process.exit(1);
  }

  const browser = await puppeteer.launch({
    executablePath: EDGE_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  console.log('Browser launched');

  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });

  // Login first
  console.log('\n=== Logging in ===');
  const loggedIn = await login(page);
  console.log('Login result:', loggedIn);

  if (loggedIn) {
    // Visit pages
    const results = await visitPages(page, batchNum, batchSize);

    console.log('\n=== Batch Results ===');
    results.forEach(r => {
      console.log(`${r.status === 'OK' ? '✓' : '✗'} ${r.route} -> ${r.status}`);
      if (r.errors.length > 0) {
        r.errors.forEach(e => console.log(`   ${e.substring(0, 100)}`));
      }
    });
  } else {
    console.log('Login failed, taking screenshot...');
    await page.screenshot({
      path: path.join(__dirname, 'screenshots/login_failed.png'),
      fullPage: true
    });
    const html = await page.content();
    fs.writeFileSync(path.join(__dirname, 'screenshots/login_page.html'), html);
    console.log('Login page HTML saved');
  }

  await browser.close();
  console.log('\nDone.');
}

main().catch(console.error);
