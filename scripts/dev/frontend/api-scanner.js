/**
 * API端点快速扫描脚本 - 通过API测试所有后端接口
 * 比Playwright浏览器方式快10倍以上
 */
const http = require('http');

const API_BASE = 'http://localhost:5655';

async function login() {
  const data = JSON.stringify({
    username: 'admin',
    password: process.env.ADMIN_PASSWORD || 'admin123',
    tenantName: '\u7cfb\u7edf\u79df\u6237'
  });
  const dataBuffer = Buffer.from(data, 'utf-8');

  return new Promise((resolve, reject) => {
    const req = http.request(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json; charset=utf-8', 'Content-Length': dataBuffer.length },
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(body)); } catch(e) { resolve({ error: body }); }
      });
    });
    req.on('error', reject);
    req.write(dataBuffer);
    req.end();
  });
}

function apiGet(path, token) {
  return new Promise((resolve) => {
    const timer = setTimeout(() => resolve({ status: 408, path, error: 'timeout' }), 10000);
    const req = http.request(`${API_BASE}${path}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'tenantId': '1',
      },
    }, res => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        clearTimeout(timer);
        let data = null;
        try { data = JSON.parse(body); } catch(e) {}
        resolve({ status: res.statusCode, path, data, body: body.substring(0, 500) });
      });
    });
    req.on('error', (e) => { clearTimeout(timer); resolve({ status: 0, path, error: e.message }); });
    req.end();
  });
}

// 所有API端点
const ENDPOINTS = [
  // 工作台
  '/api/dashboard/stats',
  // 产品
  '/api/erp/product/list?pageNum=1&pageSize=5',
  // 往来单位
  '/api/erp/partner/list?pageNum=1&pageSize=5',
  // 价格
  '/api/erp/pricing/list?pageNum=1&pageSize=5',
  '/api/erp/pricing/approval/list?pageNum=1&pageSize=5',
  '/api/erp/pricing/tiers/list?pageNum=1&pageSize=5',
  // 销售
  '/api/erp/sale/list?pageNum=1&pageSize=5',
  '/api/sale/list?pageNum=1&pageSize=5',
  '/api/erp/sales-analysis/list?pageNum=1&pageSize=5',
  '/api/erp/sales-report/list?pageNum=1&pageSize=5',
  // 出库
  '/api/stock/list?pageNum=1&pageSize=5',
  '/api/erp/stock/list?pageNum=1&pageSize=5',
  '/api/erp/shipment/list?pageNum=1&pageSize=5',
  '/api/erp/return/list?pageNum=1&pageSize=5',
  // 采购
  '/api/purchase/list?pageNum=1&pageSize=5',
  '/api/erp/purchase/list?pageNum=1&pageSize=5',
  '/api/erp/stock-in/list?pageNum=1&pageSize=5',
  '/api/erp/purchase-exchange/list?pageNum=1&pageSize=5',
  '/api/erp/purchase/return/page?pageNum=1&pageSize=5',
  // 库存扩展
  '/api/erp/stocktake/list?pageNum=1&pageSize=5',
  '/api/erp/batch/list?pageNum=1&pageSize=5',
  '/api/erp/serial/list?pageNum=1&pageSize=5',
  '/api/erp/stock-cost-adjust/list?pageNum=1&pageSize=5',
  '/api/erp/stock-overflow/list?pageNum=1&pageSize=5',
  '/api/erp/stock-damage/list?pageNum=1&pageSize=5',
  '/api/erp/stock-transfer/list?pageNum=1&pageSize=5',
  '/api/erp/stock-bom/list?pageNum=1&pageSize=5',
  '/api/erp/stock-assemble/list?pageNum=1&pageSize=5',
  '/api/erp/stock-split/list?pageNum=1&pageSize=5',
  '/api/erp/stock-alert-config/list?pageNum=1&pageSize=5',
  '/api/erp/stock-replenishment/list?pageNum=1&pageSize=5',
  // CRM
  '/api/customer/list?pageNum=1&pageSize=5',
  '/api/lead/list?pageNum=1&pageSize=5',
  '/api/opportunity/list?pageNum=1&pageSize=5',
  '/api/quotation/list?pageNum=1&pageSize=5',
  '/api/contract/list?pageNum=1&pageSize=5',
  '/api/invoice/list?pageNum=1&pageSize=5',
  // 财务
  '/api/finance/list?pageNum=1&pageSize=5',
  '/api/finance/receivable/list?pageNum=1&pageSize=5',
  '/api/finance/payable/list?pageNum=1&pageSize=5',
  '/api/finance/pre-receipt/list?pageNum=1&pageSize=5',
  '/api/finance/pre-payment/list?pageNum=1&pageSize=5',
  '/api/finance/deposit/list?pageNum=1&pageSize=5',
  '/api/finance/write-off/list?pageNum=1&pageSize=5',
  '/api/finance/offset/list?pageNum=1&pageSize=5',
  '/api/erp/capital-flow/list?pageNum=1&pageSize=5',
  '/api/finance/receipt/list?pageNum=1&pageSize=5',
  '/api/finance/payment/list?pageNum=1&pageSize=5',
  '/api/finance/report/list?pageNum=1&pageSize=5',
  '/api/finance/reconciliation/list?pageNum=1&pageSize=5',
  '/api/finance/voucher/list?pageNum=1&pageSize=5',
  '/api/finance/subject/list?pageNum=1&pageSize=5',
  '/api/finance/reports/list?pageNum=1&pageSize=5',
  '/api/finance/accounts-receivable/list?pageNum=1&pageSize=5',
  '/api/finance/accounts-payable/list?pageNum=1&pageSize=5',
  // 费用
  '/api/erp/expense/application/list?pageNum=1&pageSize=5',
  '/api/erp/expense/reimbursement/list?pageNum=1&pageSize=5',
  '/api/erp/expense/approval/list?pageNum=1&pageSize=5',
  '/api/erp/expense/payment/list?pageNum=1&pageSize=5',
  '/api/erp/expense/statistics/list?pageNum=1&pageSize=5',
  // 预算
  '/api/budget/list?pageNum=1&pageSize=5',
  '/api/budget/template/list?pageNum=1&pageSize=5',
  '/api/budget/annual/list?pageNum=1&pageSize=5',
  '/api/budget/adjustment/list?pageNum=1&pageSize=5',
  '/api/budget/report/list?pageNum=1&pageSize=5',
  // 固定资产
  '/api/fixed-asset/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/asset/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/category/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/depreciation/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/transfer/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/disposal/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/inventory/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/report/list?pageNum=1&pageSize=5',
  '/api/fixed-asset/purchase/list?pageNum=1&pageSize=5',
  // 商城
  '/api/mall/config/list?pageNum=1&pageSize=5',
  '/api/mall/banner/list?pageNum=1&pageSize=5',
  '/api/mall/order/list?pageNum=1&pageSize=5',
  '/api/mall/product/list?pageNum=1&pageSize=5',
  '/api/mall/user-audit/list?pageNum=1&pageSize=5',
  // WMS
  '/api/wms/warehouse/list?pageNum=1&pageSize=5',
  '/api/wms/location/list?pageNum=1&pageSize=5',
  '/api/wms/receipt/list?pageNum=1&pageSize=5',
  '/api/wms/putaway/list?pageNum=1&pageSize=5',
  '/api/wms/pick/list?pageNum=1&pageSize=5',
  '/api/wms/wave/list?pageNum=1&pageSize=5',
  '/api/wms/ship/list?pageNum=1&pageSize=5',
  '/api/wms/inventory/list?pageNum=1&pageSize=5',
  '/api/wms/move/list?pageNum=1&pageSize=5',
  '/api/wms/check/list?pageNum=1&pageSize=5',
  '/api/wms/event/list?pageNum=1&pageSize=5',
  // DMS
  '/api/dms/dashboard/stats',
  '/api/dms/channel/list?pageNum=1&pageSize=5',
  '/api/dms/rider/list?pageNum=1&pageSize=5',
  '/api/dms/vehicle/list?pageNum=1&pageSize=5',
  '/api/dms/verification/list?pageNum=1&pageSize=5',
  '/api/dms/route/list?pageNum=1&pageSize=5',
  '/api/dms/dispatch/list?pageNum=1&pageSize=5',
  '/api/dms/order-pool/list?pageNum=1&pageSize=5',
  '/api/dms/config/list?pageNum=1&pageSize=5',
  '/api/dms/tracking/list?pageNum=1&pageSize=5',
  // 打印
  '/api/v2/print/templates?pageNum=1&pageSize=5',
  '/api/v2/print/clients?pageNum=1&pageSize=5',
  // 供应商
  '/api/supplier/list?pageNum=1&pageSize=5',
  '/api/supplier/inquiry/list?pageNum=1&pageSize=5',
  '/api/supplier/performance/list?pageNum=1&pageSize=5',
  // 系统
  '/api/system/user/list?pageNum=1&pageSize=5',
  '/api/system/role/list?pageNum=1&pageSize=5',
  '/api/system/menu/list',
  '/api/system/config/list?pageNum=1&pageSize=5',
  '/api/system/dict/list?pageNum=1&pageSize=5',
  '/api/system/department/list?pageNum=1&pageSize=5',
  '/api/system/position/list?pageNum=1&pageSize=5',
  '/api/system/log/list?pageNum=1&pageSize=5',
  '/api/system/tenant/list?pageNum=1&pageSize=5',
  // 通知
  '/api/notification/list?pageNum=1&pageSize=5',
  // 订单中心
  '/api/order-center/list?pageNum=1&pageSize=5',
  // ERP换货
  '/api/erp/sale/exchange/page?pageNum=1&pageSize=5',
  // 库存预警
  '/api/erp/stock/alert',
];

async function main() {
  console.log('🔍 开始API端点扫描...\n');

  const loginRes = await login();
  if (loginRes.code !== 200 || !loginRes.data?.token) {
    console.log('❌ 登录失败:', loginRes);
    process.exit(1);
  }
  const token = loginRes.data.token;
  console.log('✅ 登录成功\n');

  const results = [];
  let ok = 0, err = 0, notFound = 0;

  for (const ep of ENDPOINTS) {
    const res = await apiGet(ep, token);
    const isError = res.status >= 500 || res.status === 408;
    const is404 = res.status === 404;
    const isOk = res.status === 200 && (!res.data || res.data.code === 200 || res.data.code === 0);

    if (isError) err++;
    else if (is404) notFound++;
    else ok++;

    const icon = isError ? '❌' : is404 ? '⚠️' : isOk ? '✅' : '🟡';
    const statusInfo = isError || is404 ? ` [${res.status}]` : '';
    const errorInfo = res.data?.message && isError ? ` - ${res.data.message.substring(0, 80)}` : '';

    if (isError || is404) {
      console.log(`${icon} ${ep}${statusInfo}${errorInfo}`);
    }

    results.push({ endpoint: ep, status: res.status, error: isError, message: res.data?.message });
  }

  console.log(`\n========== 扫描摘要 ==========`);
  console.log(`总计: ${ENDPOINTS.length} 个端点`);
  console.log(`正常: ${ok}`);
  console.log(`500错误: ${err}`);
  console.log(`404: ${notFound}`);

  // Save detailed results
  const fs = require('fs');
  fs.writeFileSync('i:/AI-Ready/tool-results/api-scan-results.json', JSON.stringify({ summary: { total: ENDPOINTS.length, ok, err, notFound }, results: results.filter(r => r.error || r.status === 404) }, null, 2));
  console.log('\n📊 详细结果已保存到 api-scan-results.json');
}

main().catch(console.error);
