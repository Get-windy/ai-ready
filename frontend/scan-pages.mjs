/**
 * ERP 页面扫描脚本
 * 模拟用户操作，收集运行时错误
 */
import { chromium } from '@playwright/test';
import fs from 'fs';

const BASE_URL = 'http://localhost:5656';
const LOGIN_CREDENTIALS = { username: 'admin', password: 'admin123', tenantName: 'SYSTEM' };

// 需要扫描的功能页面（不包含登录页、错误页）
const PAGES = [
  // Dashboard
  { path: '/dashboard', name: '工作台' },

  // ERP 模块
  { path: '/erp/dashboard', name: 'ERP仪表盘' },
  { path: '/erp/sale', name: 'ERP销售管理' },
  { path: '/erp/product', name: '产品管理' },
  { path: '/erp/partner', name: '往来单位管理' },
  { path: '/erp/batch', name: '批次管理' },
  { path: '/erp/serial', name: '序列号管理' },
  { path: '/erp/stock', name: '库存管理' },
  { path: '/erp/purchase', name: 'ERP采购管理' },
  { path: '/erp/purchase-exchange', name: '采购换货' },

  // ERP 子模块
  { path: '/erp/stock-in', name: '入库管理' },
  { path: '/erp/stocktake', name: '库存盘点' },
  { path: '/erp/return', name: '退货管理' },
  { path: '/erp/shipment', name: '发货管理' },
  { path: '/erp/sales-analysis', name: '销售分析' },
  { path: '/erp/sales-report', name: '销售报表' },
  { path: '/erp/pricing/customer-grade', name: '客户等级定价' },
  { path: '/erp/pricing/approval', name: '定价审批' },
  { path: '/erp/pricing/tiers', name: '价格层级' },
  { path: '/erp/fixed-asset', name: '固定资产' },

  // 库存扩展
  { path: '/erp/stock-cost-adjust', name: '库存成本调整' },
  { path: '/erp/stock-overflow', name: '库存溢余' },
  { path: '/erp/stock-damage', name: '库存报损' },
  { path: '/erp/stock-transfer', name: '库存调拨' },
  { path: '/erp/stock-alert-config', name: '库存预警' },
  { path: '/erp/stock-bom', name: 'BOM管理' },
  { path: '/erp/stock-assemble', name: '组装管理' },
  { path: '/erp/stock-split', name: '拆分管理' },

  // 预算
  { path: '/erp/budget', name: '预算管理' },

  // 商城
  { path: '/erp/mall/config', name: '商城配置' },
  { path: '/erp/mall/user-audit', name: '商城用户审核' },
  { path: '/erp/mall/banner', name: '轮播图管理' },
  { path: '/erp/mall/order', name: '商城订单' },
  { path: '/erp/mall/product', name: '商城商品' },

  // 打印模块
  { path: '/erp/printing/template', name: '打印模板' },
  { path: '/erp/printing/chain', name: '打印链路' },
  { path: '/erp/printing/client', name: '打印客户端' },
  { path: '/erp/printing/task', name: '打印任务' },

  // 费用管理
  { path: '/erp/expense/application', name: '费用申请' },
  { path: '/erp/expense/reimbursement', name: '费用报销' },
  { path: '/erp/expense/approval', name: '费用审批' },
  { path: '/erp/expense/payment', name: '费用付款' },
  { path: '/erp/expense/statistics', name: '费用统计' },

  // 财务管理
  { path: '/erp/finance', name: '财务管理首页' },
  { path: '/erp/finance/subject', name: '科目管理' },
  { path: '/erp/finance/voucher', name: '凭证管理' },
  { path: '/erp/finance/receivable', name: '应收账款' },
  { path: '/erp/finance/payable', name: '应付账款' },
  { path: '/erp/finance/report', name: '财务报表' },
  { path: '/erp/finance/receipt', name: '收款单' },
  { path: '/erp/finance/payment', name: '付款单' },
  { path: '/erp/finance/pre-receipt', name: '预收款' },
  { path: '/erp/finance/pre-payment', name: '预付款' },
  { path: '/erp/finance/deposit', name: '定金押金' },
  { path: '/erp/finance/write-off', name: '收付款核销' },
  { path: '/erp/finance/offset', name: '往来对冲' },
  { path: '/erp/finance/capital-flow', name: '资金流水' },

  // CRM 模块
  { path: '/crm/customer', name: '客户管理' },
  { path: '/crm/contract', name: '合同管理' },
  { path: '/crm/lead', name: '线索管理' },
  { path: '/crm/opportunity', name: '商机管理' },
  { path: '/crm/quotation', name: '报价管理' },
  { path: '/crm/invoice', name: '发票管理' },
  { path: '/crm/supplier', name: 'CRM供应商' },

  // 采购模块
  { path: '/purchase', name: '采购管理' },
  { path: '/sale', name: '销售管理' },
  { path: '/stock', name: '库存管理' },
  { path: '/supplier', name: '供应商管理' },
  { path: '/supplier/inquiry', name: '供应商询价' },
  { path: '/supplier/performance', name: '供应商绩效' },

  // 系统管理
  { path: '/system/user', name: '用户管理' },
  { path: '/system/role', name: '角色管理' },
  { path: '/system/menu', name: '菜单管理' },
  { path: '/system/permission', name: '权限管理' },
  { path: '/system/department', name: '部门管理' },
  { path: '/system/position', name: '岗位管理' },
  { path: '/system/dict', name: '字典管理' },
  { path: '/system/config', name: '系统配置' },
  { path: '/system/log', name: '系统日志' },
  { path: '/system/tenant', name: '租户管理' },
  { path: '/system/tenant-approval', name: '租户审批' },
  { path: '/system/data-import', name: '数据导入' },

  // 其他
  { path: '/notification', name: '通知公告' },
  { path: '/profile', name: '个人中心' },
  { path: '/charts', name: '图表' },
  { path: '/order-center', name: '订单中心' },

  // 固定资产
  { path: '/fixed-asset/asset', name: '固定资产列表' },
  { path: '/fixed-asset/category', name: '资产分类' },
  { path: '/fixed-asset/depreciation', name: '折旧管理' },
  { path: '/fixed-asset/transfer', name: '资产调拨' },
  { path: '/fixed-asset/disposal', name: '资产处置' },
  { path: '/fixed-asset/inventory', name: '资产盘点' },
  { path: '/fixed-asset/report', name: '资产报表' },

  // Admin
  { path: '/admin/sys/permissions', name: '系统权限管理' },
  { path: '/admin/tenant/permissions', name: '租户权限管理' },

  // 工作流
  { path: '/workflow/task-management', name: '工作流任务' },
  { path: '/workflow/instance-monitor', name: '流程监控' },
  { path: '/workflow/process-analysis', name: '流程分析' },
];

class PageScanner {
  constructor(batchSize = 10) {
    this.batchSize = batchSize;
    this.allErrors = { console: [], network: [], visual: [], api: [] };
    this.totalPagesScanned = 0;
    this.fixedErrors = [];
  }

  async login(page) {
    console.log('正在通过 API 登录...');

    try {
      // 1. 获取验证码
      const captchaRes = await fetch(`${BASE_URL.replace('5656', '5655')}/api/auth/captcha`, {
        headers: { 'Content-Type': 'application/json' }
      });
      const captchaData = await captchaRes.json();
      console.log(`验证码获取: ${JSON.stringify(captchaData).substring(0, 100)}`);

      // 直接调用登录 API（绕过验证码校验）
      const loginRes = await fetch(`${BASE_URL.replace('5656', '5655')}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(LOGIN_CREDENTIALS)
      });
      const loginData = await loginRes.json();
      console.log(`登录响应: code=${loginData.code}, message=${loginData.message}`);

      if (loginData.code === 200 && loginData.data?.token) {
        const token = loginData.data.token;
        const tenantId = loginData.data.tenantId || 1;
        const userId = loginData.data.userId || 1;

        // 2. 导航到首页并设置 token
        await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });

        // 3. 设置 localStorage
        await page.evaluate(({ token, tenantId, userId }) => {
          localStorage.setItem('token', token);
          localStorage.setItem('tenantId', String(tenantId));
          localStorage.setItem('userId', String(userId));
          localStorage.setItem('rememberedTenantName', 'SYSTEM');
        }, { token, tenantId, userId });

        console.log('Token 已设置到 localStorage');

        // 4. 导航到仪表盘
        await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'load', timeout: 60000 });
        await page.waitForTimeout(3000);

        const currentUrl = page.url();
        console.log(`登录后URL: ${currentUrl}`);

        // 截图验证
        await page.screenshot({ path: `screenshots/login-verify.png` });

        return !currentUrl.includes('/login');
      } else {
        console.error(`登录失败: ${loginData.message}`);
        return false;
      }
    } catch (err) {
      console.error(`登录异常: ${err.message}`);
      return false;
    }
  }

  setupErrorCollectors(page, pageName) {
    // 收集 console 错误
    page.on('console', msg => {
      if (msg.type() === 'error') {
        const text = msg.text();

        // 忽略已知噪音
        // 1. /auth/logout 401 — token 有效期转换期间的正常行为
        if (text.includes('/auth/logout') && text.includes('401')) return;
        // 2. "Request failed with status code 401" 伴随 logout
        if (text.includes('Request failed with status code 401') && text.includes('settle')) return;

        const error = { page: pageName, message: text, timestamp: new Date().toISOString() };
        this.allErrors.console.push(error);
        console.error(`  [CONSOLE ERROR] ${pageName}: ${text}`);
      }
    });

    // 收集 page error
    page.on('pageerror', err => {
      const text = err.message;
      // 忽略已知噪音
      if (text === 'Request failed with status code 401') return;

      const error = { page: pageName, message: text, stack: err.stack, timestamp: new Date().toISOString() };
      this.allErrors.console.push(error);
      console.error(`  [PAGE ERROR] ${pageName}: ${text}`);
    });

    // 收集失败的请求
    page.on('requestfailed', request => {
      const failureText = request.failure()?.errorText || '';
      // 忽略 Vite HMR 相关的正常中止
      if (failureText.includes('ERR_ABORTED')) return;
      if (request.url().includes('/ws') && failureText.includes('ERR_CONNECTION_REFUSED')) return;
      const error = {
        page: pageName,
        url: request.url(),
        method: request.method(),
        failure: failureText,
        timestamp: new Date().toISOString()
      };
      this.allErrors.network.push(error);
      if (!failureText.includes('ERR_ABORTED')) {
        console.error(`  [NETWORK ERROR] ${pageName}: ${request.url()} - ${failureText}`);
      }
    });
  }

  async scanSinglePage(page, { path, name }) {
    const url = `${BASE_URL}${path}`;
    console.log(`\n扫描页面 [${this.totalPagesScanned + 1}]: ${name} (${url})`);

    try {
      await page.goto(url, { waitUntil: 'load', timeout: 60000 });
      await page.waitForTimeout(3000);

      // 检查白屏
      const bodyText = await page.evaluate(() => document.body?.innerText || '');
      const hasContent = bodyText && bodyText.length > 0;
      if (!hasContent || bodyText.trim() === '') {
        this.allErrors.visual.push({ page: name, url, type: '白屏', timestamp: new Date().toISOString() });
        console.warn(`  [白屏] ${name}: 页面无内容`);
        return;
      }

      // 检查 Vue 崩溃
      const hasVueError = await page.evaluate(() => {
        return !!document.querySelector('.v-error, .vue-error, [data-v-error]');
      });
      if (hasVueError) {
        this.allErrors.visual.push({ page: name, url, type: 'Vue组件崩溃', timestamp: new Date().toISOString() });
        console.warn(`  [Vue崩溃] ${name}`);
      }

      // 查找可见的交互元素并点击
      await this.interactWithPage(page, name);

      this.totalPagesScanned++;
      console.log(`  ✓ ${name} 扫描完成`);
    } catch (err) {
      console.error(`  ✗ ${name} 扫描异常: ${err.message}`);
      this.allErrors.console.push({
        page: name,
        message: `扫描异常: ${err.message}`,
        timestamp: new Date().toISOString()
      });
    }
  }

  async interactWithPage(page, pageName) {
    try {
      // 点击 Tab/标签页
      const tabs = await page.locator('.ant-tabs-tab, .ant-tab, [role="tab"]').all();
      for (let i = 0; i < Math.min(tabs.length, 3); i++) {
        try {
          await tabs[i].click();
          await page.waitForTimeout(1500);
        } catch { }
      }

      // 展开下拉菜单
      const dropdowns = await page.locator('.ant-select-selector, .ant-dropdown-trigger').all();
      for (let i = 0; i < Math.min(dropdowns.length, 2); i++) {
        try {
          await dropdowns[i].click();
          await page.waitForTimeout(1000);
          // 按 ESC 关闭
          await page.keyboard.press('Escape');
          await page.waitForTimeout(500);
        } catch { }
      }

      // 点击分页
      const pagers = await page.locator('.ant-pagination-item').all();
      if (pagers.length > 1) {
        try {
          await pagers[1].click();
          await page.waitForTimeout(1500);
        } catch { }
      }

      // 尝试搜索
      const searchInputs = await page.locator('input[placeholder*="搜索"], input[placeholder*="查询"], input[placeholder*="关键词"]').all();
      if (searchInputs.length > 0) {
        try {
          await searchInputs[0].fill('测试');
          await page.waitForTimeout(500);
          const searchBtn = await page.locator('button:has-text("搜索"), button:has-text("查询")').first();
          if (await searchBtn.isVisible()) {
            await searchBtn.click();
            await page.waitForTimeout(1500);
          }
        } catch { }
      }
    } catch (err) {
      console.warn(`  ${pageName} 交互过程出错: ${err.message}`);
    }
  }

  getBatches() {
    const batches = [];
    for (let i = 0; i < PAGES.length; i += this.batchSize) {
      batches.push(PAGES.slice(i, i + this.batchSize));
    }
    return batches;
  }

  printReport() {
    console.log('\n' + '='.repeat(60));
    console.log('扫描报告');
    console.log('='.repeat(60));
    console.log(`扫描页面总数: ${this.totalPagesScanned}`);
    console.log(`控制台错误数: ${this.allErrors.console.length}`);
    console.log(`网络错误数: ${this.allErrors.network.length}`);
    console.log(`视觉异常数: ${this.allErrors.visual.length}`);

    if (this.allErrors.console.length > 0) {
      console.log('\n--- 控制台错误 ---');
      this.allErrors.console.forEach((e, i) => {
        console.log(`  [${i + 1}] ${e.page}: ${e.message}`);
      });
    }
    if (this.allErrors.network.length > 0) {
      console.log('\n--- 网络错误 ---');
      this.allErrors.network.forEach((e, i) => {
        console.log(`  [${i + 1}] ${e.page}: ${e.url} - ${e.failure}`);
      });
    }
    if (this.allErrors.visual.length > 0) {
      console.log('\n--- 视觉异常 ---');
      this.allErrors.visual.forEach((e, i) => {
        console.log(`  [${i + 1}] ${e.page}: ${e.type}`);
      });
    }

    // 输出 JSON 供分析
    fs.writeFileSync('scan-results.json', JSON.stringify(this.allErrors, null, 2));
    console.log('\n详细结果已保存到 scan-results.json');
  }

  async run() {
    console.log('ERP 页面扫描启动');
    console.log(`批次大小: ${this.batchSize}, 共 ${Math.ceil(PAGES.length / this.batchSize)} 批`);
    console.log(`页面总数: ${PAGES.length}`);

    const batches = this.getBatches();
    const browser = await chromium.launch({ headless: true });

    for (let batchIdx = 0; batchIdx < batches.length; batchIdx++) {
      const batch = batches[batchIdx];
      console.log(`\n${'='.repeat(60)}`);
      console.log(`第 ${batchIdx + 1} 批 (${batch.length} 个页面)`);
      console.log(`${'='.repeat(60)}`);

      const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        ignoreHTTPSErrors: true,
      });
      const page = await context.newPage();

      // 设置错误收集
      this.setupErrorCollectors(page, `第${batchIdx + 1}批`);

      // 登录
      const loggedIn = await this.login(page);
      if (!loggedIn) {
        console.error('登录失败，跳过本批次');
        await context.close();
        continue;
      }

      // 等待路由加载
      await page.waitForTimeout(3000);

      // 扫描本批次所有页面
      for (const pageInfo of batch) {
        await this.scanSinglePage(page, pageInfo);
      }

      await context.close();
    }

    await browser.close();

    // 输出报告
    this.printReport();
  }
}

// 执行
const scanner = new PageScanner(10);
scanner.run().catch(console.error);
