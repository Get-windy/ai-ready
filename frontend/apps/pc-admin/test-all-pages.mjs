import { chromium } from 'playwright';
import fs from 'fs';

// 页面清单（从componentMap和getRequiredRoutes提取）
const pages = [
  // 批次1：工作台和核心模块
  { path: '/dashboard/dashboard', name: '工作台', category: '工作台' },
  { path: '/erp/product', name: '产品管理', category: '产品数据' },
  { path: '/erp/partner', name: '往来单位管理', category: '产品数据' },
  { path: '/sale', name: '销售订单', category: '销售作业' },
  { path: '/stock', name: '销售出库', category: '销售作业' },
  { path: '/erp/shipment', name: '发货管理', category: '销售作业' },
  { path: '/erp/return', name: '退货管理', category: '销售作业' },
  { path: '/erp/sales-analysis', name: '销售分析', category: '销售作业' },
  { path: '/erp/sales-report', name: '销售报表', category: '销售作业' },
  { path: '/purchase', name: '采购订单', category: '采购作业' },

  // 批次2：采购和仓储模块
  { path: '/erp/stock-in', name: '入库管理', category: '采购作业' },
  { path: '/erp/purchase-exchange', name: '采购换货', category: '采购作业' },
  { path: '/erp/stock', name: '库存管理(ERP)', category: '仓储作业' },
  { path: '/erp/stocktake', name: '库存盘点', category: '仓储作业' },
  { path: '/erp/batch', name: '批次管理', category: '仓储作业' },
  { path: '/erp/serial', name: '序列号管理', category: '仓储作业' },
  { path: '/erp/stock-cost-adjust', name: '成本调整', category: '仓储作业' },
  { path: '/erp/stock-overflow', name: '溢余处理', category: '仓储作业' },
  { path: '/erp/stock-damage', name: '报损处理', category: '仓储作业' },
  { path: '/erp/stock-transfer', name: '调拨管理', category: '仓储作业' },

  // 批次3：库存扩展和WMS
  { path: '/erp/stock-replenishment', name: '补货管理', category: '仓储作业' },
  { path: '/erp/stock-alert-config', name: '预警配置', category: '仓储作业' },
  { path: '/erp/stock-bom', name: 'BOM管理', category: '仓储作业' },
  { path: '/erp/stock-assemble', name: '组装管理', category: '仓储作业' },
  { path: '/erp/stock-split', name: '拆分管理', category: '仓储作业' },
  { path: '/wms/warehouse', name: '仓库管理', category: 'WMS仓储执行' },
  { path: '/wms/location', name: '库位管理', category: 'WMS仓储执行' },
  { path: '/wms/receipt', name: '收货管理', category: 'WMS仓储执行' },
  { path: '/wms/putaway', name: '上架管理', category: 'WMS仓储执行' },
  { path: '/wms/pick', name: '拣货管理', category: 'WMS仓储执行' },

  // 批次4：WMS继续
  { path: '/wms/wave', name: '波次管理', category: 'WMS仓储执行' },
  { path: '/wms/ship', name: '出库管理', category: 'WMS仓储执行' },
  { path: '/wms/inventory', name: '库存管理', category: 'WMS仓储执行' },
  { path: '/wms/move', name: '移库管理', category: 'WMS仓储执行' },
  { path: '/wms/check', name: '盘点管理', category: 'WMS仓储执行' },
  { path: '/wms/event', name: '事件日志', category: 'WMS仓储执行' },
  { path: '/crm/customer', name: '客户管理', category: '客户关系' },
  { path: '/crm/lead', name: '线索管理', category: '客户关系' },
  { path: '/crm/opportunity', name: '商机管理', category: '客户关系' },
  { path: '/crm/quotation', name: '报价管理', category: '客户关系' },

  // 批次5：客户关系和财务
  { path: '/crm/contract', name: '合同管理', category: '客户关系' },
  { path: '/crm/invoice', name: '发票管理', category: '客户关系' },
  { path: '/crm/supplier', name: '供应商管理', category: '客户关系' },
  { path: '/finance', name: '财务管理', category: '财务管理' },
  { path: '/finance/receivable', name: '应收管理', category: '财务管理' },
  { path: '/finance/payable', name: '应付管理', category: '财务管理' },
  { path: '/finance/pre-receipt', name: '预收款', category: '财务管理' },
  { path: '/finance/pre-payment', name: '预付款', category: '财务管理' },
  { path: '/finance/deposit', name: '保证金', category: '财务管理' },
  { path: '/finance/write-off', name: '核销管理', category: '财务管理' },

  // 批次6：财务继续
  { path: '/finance/offset', name: '冲销管理', category: '财务管理' },
  { path: '/finance/capital-flow', name: '资金流水', category: '财务管理' },
  { path: '/finance/receipt', name: '收款单', category: '财务管理' },
  { path: '/finance/payment', name: '付款单', category: '财务管理' },
  { path: '/finance/report', name: '财务报表', category: '财务管理' },
  { path: '/finance/voucher', name: '凭证管理', category: '财务管理' },
  { path: '/finance/subject', name: '科目管理', category: '财务管理' },
  { path: '/finance/reconciliation', name: '对账管理', category: '财务管理' },
  { path: '/finance/accounts-receivable', name: '应收账款', category: '财务管理' },
  { path: '/finance/accounts-payable', name: '应付账款', category: '财务管理' },

  // 批次7：费用和资产
  { path: '/erp/expense/application', name: '费用申请', category: '费用管理' },
  { path: '/erp/expense/reimbursement', name: '费用报销', category: '费用管理' },
  { path: '/erp/expense/approval', name: '费用审批', category: '费用管理' },
  { path: '/erp/expense/payment', name: '费用支付', category: '费用管理' },
  { path: '/erp/expense/statistics', name: '费用统计', category: '费用管理' },
  { path: '/fixed-asset/asset', name: '资产列表', category: '资产管理' },
  { path: '/fixed-asset/category', name: '资产分类', category: '资产管理' },
  { path: '/fixed-asset/depreciation', name: '折旧管理', category: '资产管理' },
  { path: '/fixed-asset/transfer', name: '资产调拨', category: '资产管理' },
  { path: '/fixed-asset/disposal', name: '资产处置', category: '资产管理' },

  // 批次8：资产继续和预算
  { path: '/fixed-asset/inventory', name: '资产盘点', category: '资产管理' },
  { path: '/fixed-asset/report', name: '资产报表', category: '资产管理' },
  { path: '/fixed-asset/purchase', name: '资产采购', category: '资产管理' },
  { path: '/budget', name: '预算管理', category: '预算管理' },
  { path: '/budget/template', name: '预算模板', category: '预算管理' },
  { path: '/budget/annual', name: '年度预算', category: '预算管理' },
  { path: '/budget/adjustment', name: '预算调整', category: '预算管理' },
  { path: '/budget/report', name: '预算报表', category: '预算管理' },
  { path: '/mall/config', name: '商城配置', category: '商城管理' },
  { path: '/mall/user-audit', name: '用户审核', category: '商城管理' },

  // 批次9：商城和订单
  { path: '/mall/banner', name: '轮播图管理', category: '商城管理' },
  { path: '/mall/order', name: '订单管理', category: '商城管理' },
  { path: '/mall/product', name: '商品管理', category: '商城管理' },
  { path: '/order-center', name: '订单中心', category: '订单中心' },
  { path: '/workflow/instance-monitor', name: '流程监控', category: '工作流' },
  { path: '/workflow/task-management', name: '任务管理', category: '工作流' },
  { path: '/workflow/process-analysis', name: '流程分析', category: '工作流' },
  { path: '/printing/template', name: '打印模板', category: '打印管理' },
  { path: '/printing/chain', name: '打印链路', category: '打印管理' },
  { path: '/printing/client', name: '打印客户端', category: '打印管理' },

  // 批次10：打印和系统
  { path: '/printing/task', name: '打印任务', category: '打印管理' },
  { path: '/printing/designer', name: '模板设计', category: '打印管理' },
  { path: '/system/user', name: '用户管理', category: '系统管理' },
  { path: '/system/role', name: '角色管理', category: '系统管理' },
  { path: '/system/menu', name: '菜单管理', category: '系统管理' },
  { path: '/system/permission', name: '权限管理', category: '系统管理' },
  { path: '/system/department', name: '部门管理', category: '系统管理' },
  { path: '/system/position', name: '岗位管理', category: '系统管理' },
  { path: '/system/dict', name: '字典管理', category: '系统管理' },
  { path: '/system/config', name: '系统配置', category: '系统管理' },

  // 批次11：系统继续和配送
  { path: '/system/log', name: '操作日志', category: '系统管理' },
  { path: '/system/tenant', name: '租户管理', category: '系统管理' },
  { path: '/system/tenant-approval', name: '租户审批', category: '系统管理' },
  { path: '/system/data-import', name: '数据导入', category: '系统管理' },
  { path: '/dms/dashboard', name: '配送工作台', category: '配送管理' },
  { path: '/dms/channel', name: '渠道管理', category: '配送管理' },
  { path: '/dms/rider', name: '骑手管理', category: '配送管理' },
  { path: '/dms/vehicle', name: '车辆管理', category: '配送管理' },
  { path: '/dms/verification', name: '核销管理', category: '配送管理' },
  { path: '/dms/route', name: '线路管理', category: '配送管理' },

  // 批次12：配送继续和图表
  { path: '/dms/dispatch', name: '调度管理', category: '配送管理' },
  { path: '/dms/order-pool', name: '订单池', category: '配送管理' },
  { path: '/dms/config', name: '配送配置', category: '配送管理' },
  { path: '/dms/tracking', name: '轨迹追踪', category: '配送管理' },
  { path: '/charts', name: '图表分析', category: '图表' },
  { path: '/notification', name: '通知公告', category: '系统管理' },
  { path: '/profile', name: '个人中心', category: '系统管理' },
  { path: '/erp/pricing/approval', name: '价格审批', category: '价格引擎' },
  { path: '/erp/pricing/tiers', name: '价格层级', category: '价格引擎' },
  { path: '/erp/dashboard', name: 'ERP工作台', category: '工作台' },

  // 批次13：价格引擎和定价
  { path: '/pricing', name: '定价管理', category: '价格引擎' },
  { path: '/supplier', name: '供应商管理', category: '采购作业' },
  { path: '/supplier/inquiry', name: '供应商询价', category: '采购作业' },
  { path: '/supplier/performance', name: '供应商绩效', category: '采购作业' },
];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ locale: 'zh-CN' });
  const page = await context.newPage();

  // 收集所有错误
  const allErrors = [];

  page.on('console', msg => {
    if (msg.type() === 'error') {
      allErrors.push({ type: 'console', message: msg.text(), location: msg.location() });
    }
  });

  page.on('response', response => {
    if (response.status() >= 400) {
      allErrors.push({ type: 'network', url: response.url(), status: response.status() });
    }
  });

  page.on('pageerror', error => {
    allErrors.push({ type: 'pageerror', message: error.message, stack: error.stack });
  });

  // 1. 登录
  console.log('=== 开始登录 ===');
  await page.goto('http://localhost:5656/login', { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);

  // 获取验证码并登录
  const captchaResponse = await page.evaluate(async () => {
    const resp = await fetch('/api/auth/captcha');
    return await resp.json();
  });

  const inputs = await page.$$('input');
  await inputs[0].fill('系统租户');
  await inputs[1].fill('admin');
  await inputs[2].fill('admin123');
  await inputs[3].fill('ABCD');

  const loginBtn = await page.$('button[type="submit"]');
  await loginBtn.click();
  await page.waitForTimeout(5000);

  const currentUrl = page.url();
  console.log('登录后URL:', currentUrl);

  if (currentUrl.includes('login')) {
    console.log('登录失败，停止测试');
    await browser.close();
    return;
  }

  console.log('登录成功');

  // 2. 逐批次测试页面
  const batchSize = 10;
  const totalBatches = Math.ceil(pages.length / batchSize);

  for (let batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
    const batchStart = batchIndex * batchSize;
    const batchEnd = Math.min(batchStart + batchSize, pages.length);
    const batchPages = pages.slice(batchStart, batchEnd);

    console.log(`\n=== 批次 ${batchIndex + 1}/${totalBatches} (${batchStart + 1}-${batchEnd}) ===`);

    for (const pageInfo of batchPages) {
      console.log(`\n测试页面: ${pageInfo.name} (${pageInfo.path})`);

      // 清空当前批次的错误
      const batchErrors = [];

      try {
        // 访问页面 - 使用load等待而非networkidle，避免超时
        await page.goto(`http://localhost:5656${pageInfo.path}`, { waitUntil: 'load', timeout: 30000 });
        await page.waitForTimeout(5000);

        // 检查是否有错误
        if (allErrors.length > 0) {
          // 获取最近5秒内的错误
          const recentErrors = allErrors.filter(e => e.url?.includes(pageInfo.path) || e.location?.url?.includes(pageInfo.path));
          if (recentErrors.length > 0) {
            console.log(`  发现 ${recentErrors.length} 个错误:`);
            for (const err of recentErrors) {
              console.log(`    - ${err.type}: ${err.message || err.url}`);
              batchErrors.push({ page: pageInfo.name, path: pageInfo.path, ...err });
            }
          }
        }

        // 模拟基本操作
        // 尝试点击表格、按钮等
        const buttons = await page.$$('button:not([disabled])');
        const tables = await page.$$('.ant-table');
        const forms = await page.$$('.ant-form');

        if (tables.length > 0) {
          console.log(`  发现 ${tables.length} 个表格`);
          // 尝试点击表格行
          const rows = await page.$$('.ant-table-row');
          if (rows.length > 0) {
            console.log(`  点击第一行表格数据`);
            await rows[0].click();
            await page.waitForTimeout(1000);
          }
        }

        if (forms.length > 0) {
          console.log(`  发现 ${forms.length} 个表单`);
        }

        // 检查是否有新增/编辑弹窗按钮
        const addBtn = await page.$('button:has-text("新增"), button:has-text("添加"), button:has-text("新建")');
        if (addBtn) {
          console.log('  点击新增按钮');
          await addBtn.click();
          await page.waitForTimeout(2000);

          // 检查是否有弹窗
          const modal = await page.$('.ant-modal');
          if (modal) {
            console.log('  弹窗已打开');

            // 关闭弹窗
            const closeBtn = await page.$('.ant-modal-close');
            if (closeBtn) {
              await closeBtn.click();
              await page.waitForTimeout(1000);
            }
          }
        }

        // 截图保存
        await page.screenshot({ path: `screenshots/${pageInfo.path.replace(/\//g, '_')}.png`, fullPage: true });

        console.log(`  页面测试完成`);

      } catch (error) {
        console.log(`  页面访问失败: ${error.message}`);
        batchErrors.push({ page: pageInfo.name, path: pageInfo.path, type: 'access', message: error.message });
      }

      // 输出本批次错误汇总
      if (batchErrors.length > 0) {
        console.log(`\n批次 ${batchIndex + 1} 错误汇总:`, JSON.stringify(batchErrors, null, 2));
      }
    }

    // 等待一会儿再进入下一批次
    await page.waitForTimeout(2000);
  }

  // 输出所有错误汇总
  console.log('\n=== 所有错误汇总 ===');
  console.log(`总计错误数: ${allErrors.length}`);

  // 保存错误报告到文件
  fs.writeFileSync('error-report.json', JSON.stringify(allErrors, null, 2));

  await browser.close();
})();