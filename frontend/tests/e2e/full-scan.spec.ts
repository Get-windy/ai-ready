/**
 * 全页面综合扫描测试
 * 快速遍历所有功能页面，收集错误
 */
import { test, type Page } from '@playwright/test';

const globalErrors: Array<{ page: string; type: string; message: string }> = [];

function setup(page: Page, pageName: string) {
  page.on('pageerror', err => {
    globalErrors.push({ page: pageName, type: 'PAGE_ERROR', message: err.message.substring(0, 200) });
  });
  page.on('response', resp => {
    const s = resp.status();
    if (s >= 400) {
      const url = resp.url().substring(0, 100);
      if (!url.includes('/auth/') && !url.includes('/sse/') && !url.includes('/actuator/')) {
        globalErrors.push({ page: pageName, type: `HTTP_${s}`, message: url });
      }
    }
  });
}

const ALL_PAGES = [
  // Batch 1
  { name: '工作台', path: '/dashboard' },
  { name: '销售订单', path: '/sale' },
  { name: '销售出库', path: '/stock' },
  { name: '销售管理(ERP)', path: '/erp/sale' },
  { name: '销售出库管理(ERP)', path: '/erp/sale-outbound' },
  { name: '销售分析', path: '/erp/sales-analysis' },
  { name: '销售报表', path: '/erp/sales-report' },
  { name: '采购订单', path: '/purchase' },
  { name: '采购管理(ERP)', path: '/erp/purchase' },
  { name: '采购换货', path: '/erp/purchase-exchange' },
  // Batch 2
  { name: '采购退货', path: '/erp/purchase-return' },
  { name: '入库管理', path: '/erp/stock-in' },
  { name: '库存管理(ERP)', path: '/erp/stock' },
  { name: '发货管理', path: '/erp/shipment' },
  { name: '库存盘点', path: '/erp/stocktake' },
  { name: '退货管理', path: '/erp/return' },
  { name: '批次管理', path: '/erp/batch' },
  { name: '序列号管理', path: '/erp/serial' },
  { name: '智能补货', path: '/erp/stock/replenishment' },
  { name: '库存调拨', path: '/erp/stock-transfer' },
  // Batch 3
  { name: '成本调整', path: '/erp/stock-cost-adjust' },
  { name: '溢余管理', path: '/erp/stock-overflow' },
  { name: '报损管理', path: '/erp/stock-damage' },
  { name: '库存预警配置', path: '/erp/stock-alert-config' },
  { name: 'BOM管理', path: '/erp/stock-bom' },
  { name: '组装管理', path: '/erp/stock-assemble' },
  { name: '拆分管理', path: '/erp/stock-split' },
  { name: '产品管理', path: '/erp/product' },
  { name: '往来单位', path: '/erp/partner' },
  { name: '价格管理', path: '/erp/pricing' },
  // Batch 4
  { name: '价格审批', path: '/erp/pricing/approval' },
  { name: '价格层级', path: '/erp/pricing/tiers' },
  { name: '客户管理', path: '/crm/customer' },
  { name: '线索管理', path: '/crm/lead' },
  { name: '商机管理', path: '/crm/opportunity' },
  { name: '报价管理(CRM)', path: '/crm/quotation' },
  { name: '合同管理(CRM)', path: '/crm/contract' },
  { name: '发票管理(CRM)', path: '/crm/invoice' },
  { name: '供应商管理', path: '/supplier' },
  { name: '供应商询价', path: '/supplier/inquiry' },
  // Batch 5
  { name: '供应商绩效', path: '/supplier/performance' },
  { name: '财务管理', path: '/finance' },
  { name: '应收账款', path: '/finance/accounts-receivable' },
  { name: '应付账款', path: '/finance/accounts-payable' },
  { name: '收款管理', path: '/finance/receivable' },
  { name: '付款管理', path: '/finance/payable' },
  { name: '预收款', path: '/finance/pre-receipt' },
  { name: '预付款', path: '/finance/pre-payment' },
  { name: '保证金', path: '/finance/deposit' },
  { name: '核销管理', path: '/finance/write-off' },
  // Batch 6
  { name: '冲账管理', path: '/finance/offset' },
  { name: '资金流水', path: '/finance/capital-flow' },
  { name: '收款记录', path: '/finance/receipt' },
  { name: '付款记录', path: '/finance/payment' },
  { name: '会计科目', path: '/finance/subject' },
  { name: '凭证管理', path: '/finance/voucher' },
  { name: '对账管理', path: '/finance/reconciliation' },
  { name: '财务报表', path: '/finance/report' },
  { name: '费用申请', path: '/erp/expense/application' },
  { name: '费用报销', path: '/erp/expense/reimbursement' },
  // Batch 7
  { name: '费用审批', path: '/erp/expense/approval' },
  { name: '费用付款', path: '/erp/expense/payment' },
  { name: '费用统计', path: '/erp/expense/statistics' },
  { name: '资产列表', path: '/fixed-asset/asset' },
  { name: '资产分类', path: '/fixed-asset/category' },
  { name: '折旧管理', path: '/fixed-asset/depreciation' },
  { name: '资产调拨', path: '/fixed-asset/transfer' },
  { name: '资产处置', path: '/fixed-asset/disposal' },
  { name: '资产盘点', path: '/fixed-asset/inventory' },
  // Batch 8
  { name: '预算管理', path: '/budget' },
  { name: '打印模板', path: '/printing/template' },
  { name: '打印链路', path: '/printing/chain' },
  { name: '打印客户端', path: '/printing/client' },
  { name: '打印任务', path: '/printing/task' },
  { name: '打印设计器', path: '/printing/designer' },
  // Batch 9
  { name: 'WMS仓库', path: '/wms/warehouse' },
  { name: 'WMS库位', path: '/wms/location' },
  { name: 'WMS收货', path: '/wms/receipt' },
  { name: 'WMS上架', path: '/wms/putaway' },
  { name: 'WMS拣货', path: '/wms/pick' },
  { name: 'WMS波次', path: '/wms/wave' },
  { name: 'WMS发货', path: '/wms/ship' },
  { name: 'WMS库存', path: '/wms/inventory' },
  { name: 'WMS移库', path: '/wms/move' },
  { name: 'WMS盘点', path: '/wms/check' },
  // Batch 10
  { name: '商城配置', path: '/mall/config' },
  { name: '商城用户审核', path: '/mall/user-audit' },
  { name: '商城轮播图', path: '/mall/banner' },
  { name: '商城订单', path: '/mall/order' },
  { name: '商城商品', path: '/mall/product' },
  // Batch 11
  { name: '用户管理', path: '/system/user' },
  { name: '角色管理', path: '/system/role' },
  { name: '菜单管理', path: '/system/menu' },
  { name: '系统配置', path: '/system/config' },
  { name: '部门管理', path: '/system/department' },
  // Batch 12
  { name: '字典管理', path: '/system/dict' },
  { name: '日志管理', path: '/system/log' },
  { name: '权限管理', path: '/system/permission' },
  { name: '岗位管理', path: '/system/position' },
  { name: '租户管理', path: '/system/tenant' },
  { name: '数据导入', path: '/system/data-import' },
  { name: '流程监控', path: '/workflow/instance-monitor' },
  { name: '任务管理', path: '/workflow/task-management' },
  { name: '流程分析', path: '/workflow/process-analysis' },
  { name: '通知公告', path: '/notification/index' },
];

test.describe('全页面扫描', () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({
      baseURL: 'http://localhost:5656',
      viewport: { width: 1920, height: 1080 }
    });
    page = await context.newPage();

    console.log('[登录]...');
    await page.goto('/login');
    await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15000 });
    await page.waitForSelector('.captcha-image img', { timeout: 10000 }).catch(() => {});

    const ti = page.locator('input[placeholder="请输入租户名称"]');
    if (await ti.isVisible({ timeout: 3000 }).catch(() => false)) await ti.fill('系统租户');
    await page.fill('input[placeholder="请输入用户名"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    const ci = page.locator('input[placeholder="请输入验证码"]');
    if (await ci.isVisible({ timeout: 3000 }).catch(() => false)) await ci.fill('ABCD');

    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard**', { timeout: 25000 });
    await page.waitForSelector('.basic-layout, .ant-layout', { timeout: 15000 });
    console.log('[登录] 成功');
  });

  for (const pg of ALL_PAGES) {
    test(`扫描: ${pg.name}`, async () => {
      test.setTimeout(30000);
      setup(page, pg.name);

      await page.goto(pg.path);
      await page.waitForTimeout(2000);
      try {
        await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 10000 });
      } catch {}

      const rows = page.locator('.vxe-body--row, .ant-table-row');
      const rc = await rows.count().catch(() => -1);
      console.log(`  [${pg.name.padEnd(16)}] ${page.url().substring(0, 60).padEnd(62)} 行数=${rc}`);
    });
  }

  test.afterAll(async () => {
    console.log(`\n扫描完成. 错误数: ${globalErrors.length}`);
    for (const e of globalErrors) {
      console.log(`  [${e.page}] ${e.type}: ${e.message}`);
    }
    require('fs').writeFileSync('test-results/full-scan-report.json', JSON.stringify({
      timestamp: new Date().toISOString(),
      totalPages: ALL_PAGES.length,
      errorCount: globalErrors.length,
      errors: globalErrors
    }, null, 2));
  });
});
