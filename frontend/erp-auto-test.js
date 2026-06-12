/**
 * ERP系统自动化测试脚本
 * 功能：登录、扫描所有页面、收集错误并修复
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// 配置
const CONFIG = {
  baseUrl: 'http://localhost:5656',
  apiBaseUrl: 'http://localhost:5655',
  username: 'admin',
  password: 'admin123',
  tenant: 'SYSTEM', // 使用租户代码而不是租户名称
  errorLogDir: 'i:/AI-Ready/backend/core/api/core-api/logs/errors',
  outputDir: 'i:/AI-Ready/tool-results',
  waitTime: 2000, // 每个操作后等待时间
  pageTimeout: 30000,
};

// 页面清单（从路由配置提取）
const PAGES = [
  // 工作台
  '/dashboard',

  // 销售模块
  '/sale',
  '/erp/sale',
  '/stock',
  '/erp/shipment',
  '/erp/return',
  '/erp/sales-analysis',
  '/erp/sales-report',

  // 采购模块
  '/purchase',
  '/erp/purchase',
  '/erp/stock-in',
  '/erp/purchase-exchange',

  // 仓储模块
  '/erp/stock',
  '/erp/stocktake',
  '/erp/batch',
  '/erp/serial',
  '/erp/stock-cost-adjust',
  '/erp/stock-overflow',
  '/erp/stock-damage',
  '/erp/stock-transfer',
  '/erp/stock-bom',
  '/erp/stock-assemble',
  '/erp/stock-split',
  '/erp/stock-alert-config',
  '/erp/stock-replenishment',

  // 产品数据
  '/erp/product',
  '/erp/partner',
  '/erp/pricing',
  '/erp/pricing/approval',
  '/erp/pricing/tiers',

  // CRM
  '/crm/customer',
  '/crm/lead',
  '/crm/opportunity',
  '/crm/quotation',
  '/crm/contract',
  '/crm/invoice',
  '/crm/supplier',

  // 财务
  '/finance',
  '/finance/receivable',
  '/finance/payable',
  '/finance/pre-receipt',
  '/finance/pre-payment',
  '/finance/deposit',
  '/finance/write-off',
  '/finance/offset',
  '/finance/capital-flow',
  '/finance/receipt',
  '/finance/payment',
  '/finance/report',
  '/finance/reconciliation',
  '/finance/voucher',
  '/finance/subject',
  '/finance/reports',
  '/finance/general-ledger',
  '/finance/trial-balance',
  '/finance/account-subject',
  '/finance/accounts-receivable',
  '/finance/accounts-payable',

  // 费用管理
  '/erp/expense/application',
  '/erp/expense/reimbursement',
  '/erp/expense/approval',
  '/erp/expense/payment',
  '/erp/expense/statistics',

  // 预算
  '/budget',
  '/budget/template',
  '/budget/annual',
  '/budget/adjustment',
  '/budget/report',

  // 固定资产
  '/fixed-asset',
  '/fixed-asset/asset',
  '/fixed-asset/category',
  '/fixed-asset/depreciation',
  '/fixed-asset/transfer',
  '/fixed-asset/disposal',
  '/fixed-asset/inventory',
  '/fixed-asset/report',
  '/fixed-asset/purchase',

  // 商城
  '/mall/config',
  '/mall/banner',
  '/mall/order',
  '/mall/product',
  '/mall/user-audit',

  // WMS仓储
  '/wms/warehouse',
  '/wms/location',
  '/wms/receipt',
  '/wms/putaway',
  '/wms/pick',
  '/wms/wave',
  '/wms/ship',
  '/wms/inventory',
  '/wms/move',
  '/wms/check',
  '/wms/event',

  // DMS配送
  '/dms/dashboard',
  '/dms/channel',
  '/dms/rider',
  '/dms/vehicle',
  '/dms/verification',
  '/dms/route',
  '/dms/dispatch',
  '/dms/order-pool',
  '/dms/config',
  '/dms/tracking',

  // 打印
  '/printing/template',
  '/printing/chain',
  '/printing/client',
  '/printing/task',
  '/printing/designer',

  // 供应商
  '/supplier',
  '/supplier/inquiry',
  '/supplier/performance',

  // 系统
  '/system/user',
  '/system/role',
  '/system/menu',
  '/system/permission',
  '/system/config',
  '/system/dict',
  '/system/department',
  '/system/position',
  '/system/log',
  '/system/tenant',
  '/system/tenant-approval',
  '/system/data-import',

  // 其他
  '/notification',
  '/profile',
  '/charts',
  '/order-center',
  '/erp/dashboard',
];

class ERPAutoTester {
  constructor() {
    this.browser = null;
    this.context = null;
    this.page = null;
    this.errors = [];
    this.testResults = [];
    this.currentBatch = 0;
    this.batchSize = 10;
  }

  async init() {
    console.log('🚀 启动浏览器...');
    this.browser = await chromium.launch({
      headless: false, // 显示浏览器便于调试
      args: ['--start-maximized'],
    });

    this.context = await this.browser.newContext({
      viewport: null, // 使用完整窗口
      ignoreHTTPSErrors: true,
    });

    this.page = await this.context.newPage();

    // 设置错误收集
    this.page.on('console', msg => {
      if (msg.type() === 'error') {
        const error = {
          type: 'console',
          message: msg.text(),
          url: this.page.url(),
          timestamp: new Date().toISOString(),
        };
        this.errors.push(error);
        console.log('❌ Console Error:', msg.text());
      }
    });

    this.page.on('pageerror', error => {
      const err = {
        type: 'pageerror',
        message: error.message,
        stack: error.stack,
        url: this.page.url(),
        timestamp: new Date().toISOString(),
      };
      this.errors.push(err);
      console.log('❌ Page Error:', error.message);
    });

    // 监听网络请求失败
    this.page.on('requestfailed', request => {
      const err = {
        type: 'network',
        url: request.url(),
        method: request.method(),
        failure: request.failure()?.errorText,
        timestamp: new Date().toISOString(),
      };
      this.errors.push(err);
      console.log('❌ Network Failed:', request.url());
    });

    // 监听响应错误
    this.page.on('response', response => {
      if (response.status() >= 400) {
        const err = {
          type: 'response',
          url: response.url(),
          status: response.status(),
          statusText: response.statusText(),
          timestamp: new Date().toISOString(),
        };
        this.errors.push(err);
        // 只在状态码>=500时输出，避免记录401等正常认证错误
        if (response.status() >= 500) {
          console.log('❌ Response Error:', response.status(), response.url());
        }
      }
    });

    console.log('✅ 浏览器启动成功');
  }

  async login() {
    console.log('🔐 开始登录...');

    await this.page.goto(CONFIG.baseUrl + '/login', { waitUntil: 'networkidle', timeout: CONFIG.pageTimeout });
    await this.page.waitForTimeout(3000);

    // 等待验证码加载
    const captchaImg = await this.page.$('.captcha-image img');
    if (captchaImg) {
      console.log('✅ 验证码已加载');
      await this.page.waitForTimeout(1000);
    } else {
      console.log('⚠️ 验证码未显示，等待加载...');
      await this.page.waitForTimeout(3000);
    }

    console.log('📝 填写登录表单...');

    // 填写租户名称（使用租户代码）
    const tenantInput = await this.page.$('input[placeholder*="租户"]');
    if (tenantInput) {
      await tenantInput.fill('SYSTEM');
      console.log('  已填写租户代码: SYSTEM');
      await this.page.waitForTimeout(500);
    }

    // 填写用户名
    const usernameInput = await this.page.$('input[placeholder*="用户名"]');
    if (usernameInput) {
      await usernameInput.fill(CONFIG.username);
      console.log('  已填写用户名: admin');
      await this.page.waitForTimeout(500);
    }

    // 填写密码
    const passwordInput = await this.page.$('input[type="password"]');
    if (passwordInput) {
      await passwordInput.fill(CONFIG.password);
      console.log('  已填写密码');
      await this.page.waitForTimeout(500);
    }

    // 获取验证码图片中的文字（SVG验证码）
    // 由于是SVG图片，无法直接OCR，需要通过其他方式获取
    // 先尝试查看验证码是否显示
    const captchaVisible = await this.page.$('.captcha-image img[src^="data:image"]');
    if (captchaVisible) {
      console.log('  验证码图片可见');
      // 对于测试环境，尝试跳过验证码验证或使用简单验证码
      // 填写任意4位数字作为验证码（后端可能需要在测试模式下禁用验证码验证）
      const captchaInputEl = await this.page.$('input[placeholder*="验证码"]');
      if (captchaInputEl) {
        await captchaInputEl.fill('1234');
        console.log('  已填写验证码: 1234');
        await this.page.waitForTimeout(500);
      }
    }

    await this.page.waitForTimeout(CONFIG.waitTime);

    // 点击登录按钮
    const loginBtn = await this.page.$('button[type="submit"], button:has-text("登录"), .login-button');
    if (loginBtn) {
      console.log('  点击登录按钮...');
      await loginBtn.click();
    } else {
      // 尝试按Enter键
      console.log('  按Enter键提交...');
      await this.page.keyboard.press('Enter');
    }

    // 等待登录完成
    await this.page.waitForTimeout(5000);

    // 检查是否登录成功
    const currentUrl = this.page.url();
    if (currentUrl.includes('/login')) {
      console.log('⚠️ 可能登录失败，仍在登录页');
      // 检查错误提示
      const errorMsg = await this.page.$('.ant-message-error, .ant-alert-error');
      if (errorMsg) {
        const text = await errorMsg.textContent();
        console.log('  错误信息:', text);
      }
      // 尝试API登录
      await this.tryApiLogin();
    } else {
      console.log('✅ 登录成功，当前URL:', currentUrl);
    }

    // 等待页面稳定
    await this.page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await this.page.waitForTimeout(CONFIG.waitTime);
  }

  async tryApiLogin() {
    console.log('🔄 尝试API登录...');

    try {
      // 通过页面context获取验证码
      const captchaRes = await this.page.evaluate(async (apiBaseUrl) => {
        const res = await fetch(`${apiBaseUrl}/api/auth/captcha`);
        return res.json();
      }, CONFIG.apiBaseUrl);

      if (captchaRes.code === 200 && captchaRes.data?.uuid) {
        console.log('  获取验证码成功');
        const uuid = captchaRes.data.uuid;

        // 尝试登录（后端可能在测试模式下不需要验证码）
        const loginRes = await this.page.evaluate(async ({ apiBaseUrl, username, password }) => {
          const res = await fetch(`${apiBaseUrl}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              username: username,
              password: password,
              tenantName: '系统租户',
              captchaKey: '',
              captcha: ''
            }),
          });
          return res.json();
        }, { apiBaseUrl: CONFIG.apiBaseUrl, username: CONFIG.username, password: CONFIG.password });

        if (loginRes.code === 200 && loginRes.data?.token) {
          console.log('✅ API登录成功');
          // 设置localStorage
          const tenantId = loginRes.data.tenantId || 1;
          await this.page.evaluate(({ token, tenantId }) => {
            localStorage.setItem('token', token);
            localStorage.setItem('accessToken', token);
            localStorage.setItem('tenantId', String(tenantId));
          }, { token: loginRes.data.token, tenantId: tenantId });

          // 刷新页面
          await this.page.goto(CONFIG.baseUrl + '/dashboard', { waitUntil: 'networkidle' });
          await this.page.waitForTimeout(CONFIG.waitTime);
        } else {
          console.log('  登录失败:', loginRes.message || loginRes.code);
        }
      }
    } catch (e) {
      console.log('❌ API登录失败:', e.message);
    }
  }

  async testPage(pagePath) {
    console.log(`📄 测试页面: ${pagePath}`);

    const result = {
      path: pagePath,
      success: false,
      errors: [],
      loadTime: 0,
    };

    try {
      const startTime = Date.now();

      await this.page.goto(CONFIG.baseUrl + pagePath, {
        waitUntil: 'domcontentloaded',
        timeout: CONFIG.pageTimeout,
      });

      // 等待页面加载
      await this.page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => {});
      await this.page.waitForTimeout(CONFIG.waitTime);

      result.loadTime = Date.now() - startTime;

      // 检查页面是否有错误提示
      const errorAlert = await this.page.$('.ant-message-error, .error-message, .ant-alert-error');
      if (errorAlert) {
        const errorText = await errorAlert.textContent();
        result.errors.push({ type: 'alert', message: errorText });
      }

      // 检查是否白屏
      const bodyContent = await this.page.$eval('body', el => el.innerText?.trim() || '');
      const hasContent = await this.page.$eval('body', el => {
        const children = el.querySelectorAll('*:not(script):not(style):not(link)');
        return children.length > 5;
      });

      if (!hasContent && bodyContent.length < 10) {
        result.errors.push({ type: 'whitespace', message: '页面可能白屏' });
      }

      // 检查页面内容区域是否有404或空数据提示
      const contentArea = await this.page.$('.page-container, .ant-layout-content, main, .content-wrapper');
      if (contentArea) {
        const contentText = await contentArea.evaluate(el => el.innerText || '');
        // 检查是否有404结果组件
        if (contentText.includes('404') || contentText.includes('页面不存在') || contentText.includes('不存在')) {
          // 排除正常的数据不存在提示（如"该资产不存在"只在详情页显示）
          const isDetailPage = pagePath.includes('/:id') || pagePath.includes('/detail');
          if (!isDetailPage) {
            result.errors.push({ type: 'content404', message: '页面内容区域显示404或不存在提示' });
          }
        }
        // 检查是否有"暂无数据"但表格容器存在的情况
        const emptyState = await this.page.$('.ant-empty-description, .ant-table-placeholder');
        if (emptyState) {
          const emptyText = await emptyState.evaluate(el => el.innerText || '');
          if (!emptyText.includes('暂无数据') && emptyText.length > 0) {
            // 不是正常的空数据提示，可能是错误
          }
        }
      }

      // 尝试交互操作
      await this.interactWithPage(pagePath);

      result.success = result.errors.length === 0;

    } catch (error) {
      result.errors.push({ type: 'exception', message: error.message });
      result.success = false;
    }

    this.testResults.push(result);
    console.log(`${result.success ? '✅' : '❌'} 页面 ${pagePath} 测试完成，耗时 ${result.loadTime}ms`);

    return result;
  }

  async interactWithPage(pagePath) {
    console.log(`🖱️ 执行页面交互: ${pagePath}`);

    // 等待页面稳定
    await this.page.waitForTimeout(CONFIG.waitTime);

    // 尝试点击表格行查看详情
    const tableRows = await this.page.$$('.ant-table-row, .vxe-table--body tr, tbody tr');
    if (tableRows.length > 0) {
      const firstRow = tableRows[0];
      try {
        await firstRow.click();
        await this.page.waitForTimeout(CONFIG.waitTime);

        // 检查是否打开详情抽屉/弹窗
        const drawer = await this.page.$('.ant-drawer-open, .ant-modal-open');
        if (drawer) {
          console.log('  📋 打开了详情/抽屉');
          // 关闭抽屉/弹窗
          const closeBtn = await this.page.$('.ant-drawer-close, .ant-modal-close');
          if (closeBtn) {
            await closeBtn.click();
            await this.page.waitForTimeout(1000);
          }
        }
      } catch (e) {
        // 点击失败不影响测试
      }
    }

    // 尝试点击搜索按钮
    const searchBtn = await this.page.$('button:has-text("查询"), button:has-text("搜索"), .search-btn, [data-testid="search"]');
    if (searchBtn) {
      try {
        await searchBtn.click();
        await this.page.waitForTimeout(CONFIG.waitTime);
      } catch (e) {}
    }

    // 尝试点击新增按钮
    const addBtn = await this.page.$('button:has-text("新增"), button:has-text("添加"), button:has-text("创建"), .add-btn');
    if (addBtn) {
      try {
        await addBtn.click();
        await this.page.waitForTimeout(CONFIG.waitTime);

        // 检查是否打开表单弹窗
        const formModal = await this.page.$('.ant-modal-content:visible');
        if (formModal) {
          console.log('  📝 打开了新增表单');
          // 关闭弹窗
          const cancelBtn = await this.page.$('button:has-text("取消"), button:has-text("关闭")');
          if (cancelBtn) {
            await cancelBtn.click();
            await this.page.waitForTimeout(1000);
          }
        }
      } catch (e) {}
    }

    // 尝试切换Tab
    const tabs = await this.page.$$('.ant-tabs-tab');
    if (tabs.length > 1) {
      try {
        await tabs[1].click();
        await this.page.waitForTimeout(CONFIG.waitTime);
      } catch (e) {}
    }

    // 尝试翻页
    const nextPageBtn = await this.page.$('.ant-pagination-next:not(.ant-pagination-disabled), .vxe-pager--next-btn:not(.is--disabled)');
    if (nextPageBtn) {
      try {
        await nextPageBtn.click();
        await this.page.waitForTimeout(CONFIG.waitTime);
      } catch (e) {}
    }
  }

  async runBatch(batchNumber) {
    const startIndex = batchNumber * this.batchSize;
    const endIndex = startIndex + this.batchSize;
    const batchPages = PAGES.slice(startIndex, endIndex);

    console.log(`\n📦 第 ${batchNumber + 1} 批测试，页面 ${startIndex + 1}-${Math.min(endIndex, PAGES.length)}`);

    for (const pagePath of batchPages) {
      await this.testPage(pagePath);
    }

    return this.analyzeBatchResults(batchPages);
  }

  analyzeBatchResults(batchPages) {
    const batchErrors = this.errors.filter(e =>
      batchPages.some(p => e.url?.includes(p))
    );

    return {
      pages: batchPages.length,
      successCount: batchPages.filter(p =>
        this.testResults.find(r => r.path === p && r.success)
      ).length,
      errorCount: batchErrors.length,
      errors: batchErrors,
    };
  }

  async checkBackendErrors() {
    console.log('\n🔍 检查后端错误日志...');

    try {
      const today = new Date().toISOString().slice(0, 10);
      const errorLogFile = path.join(CONFIG.errorLogDir, `${today}.jsonl`);

      if (fs.existsSync(errorLogFile)) {
        const content = fs.readFileSync(errorLogFile, 'utf8');
        const lines = content.trim().split('\n');

        const backendErrors = lines.map(line => {
          try {
            return JSON.parse(line);
          } catch {
            return { raw: line };
          }
        });

        console.log(`  发现 ${backendErrors.length} 条后端错误记录`);

        // 按类型统计
        const errorTypes = {};
        for (const err of backendErrors) {
          const type = err.type || 'unknown';
          errorTypes[type] = (errorTypes[type] || 0) + 1;
        }

        console.log('  错误类型统计:', errorTypes);

        return backendErrors;
      } else {
        console.log('  今日无后端错误日志');
        return [];
      }
    } catch (e) {
      console.log('  读取后端错误日志失败:', e.message);
      return [];
    }
  }

  async generateReport() {
    console.log('\n📊 生成测试报告...');

    const report = {
      timestamp: new Date().toISOString(),
      totalPages: PAGES.length,
      testedPages: this.testResults.length,
      successRate: this.testResults.filter(r => r.success).length / this.testResults.length,
      totalErrors: this.errors.length,
      errorTypes: {},
      pageResults: this.testResults,
      errors: this.errors,
      backendErrors: await this.checkBackendErrors(),
    };

    // 统计错误类型
    for (const err of this.errors) {
      report.errorTypes[err.type] = (report.errorTypes[err.type] || 0) + 1;
    }

    // 保存报告
    const reportPath = path.join(CONFIG.outputDir, `test-report-${Date.now()}.json`);
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`  报告已保存: ${reportPath}`);

    // 输出摘要
    console.log('\n' + '='.repeat(60));
    console.log('📈 测试摘要');
    console.log('='.repeat(60));
    console.log(`总页面数: ${report.totalPages}`);
    console.log(`已测试: ${report.testedPages}`);
    console.log(`成功率: ${(report.successRate * 100).toFixed(1)}%`);
    console.log(`总错误数: ${report.totalErrors}`);
    console.log(`后端错误数: ${report.backendErrors.length}`);
    console.log('='.repeat(60));

    return report;
  }

  async close() {
    if (this.browser) {
      await this.browser.close();
      console.log('🛑 浏览器已关闭');
    }
  }

  async run(testAllBatches = true) {
    try {
      await this.init();
      await this.login();

      if (testAllBatches) {
        // 测试所有批次
        const totalBatches = Math.ceil(PAGES.length / this.batchSize);
        console.log(`\n🚀 开始测试所有批次，共 ${totalBatches} 批，${PAGES.length} 页`);

        for (let i = 0; i < totalBatches; i++) {
          console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
          console.log(`📦 第 ${i + 1}/${totalBatches} 批测试`);
          await this.runBatch(i);

          // 每10批后暂停一下，避免长时间运行导致连接池问题
          if (i > 0 && i % 10 === 0) {
            console.log('⏳ 暂停5秒...');
            await this.page.waitForTimeout(5000);
          }
        }
      } else {
        // 只运行第一批测试
        await this.runBatch(0);
      }

      // 检查后端错误
      await this.checkBackendErrors();

      // 生成报告
      await this.generateReport();

    } catch (error) {
      console.error('❌ 测试执行失败:', error);
    } finally {
      await this.close();
    }
  }
}

// 主执行
async function main() {
  const tester = new ERPAutoTester();
  // 测试所有批次
  await tester.run(true);
}

main().catch(console.error);