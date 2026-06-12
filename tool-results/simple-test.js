/**
 * ERP系统简单自动化测试脚本
 * 使用 Node.js 直接测试后端API和前端页面
 */

const http = require('http');
const https = require('https');
const { execSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

// 配置
const API_BASE = 'http://localhost:5655';
const FRONTEND_BASE = 'http://localhost:5656';
const CREDENTIALS = {
  tenantName: '默认租户',
  username: 'admin',
  password: 'admin123'
};

// 错误收集
const errors = [];
const pageResults = [];

// HTTP请求函数
function httpRequest(url, options = {}) {
  return new Promise((resolve, reject) => {
    const urlObj = new URL(url);
    const req = http.request({
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + urlObj.search,
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    }, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({
            status: res.statusCode,
            headers: res.headers,
            body: data,
            json: data ? JSON.parse(data) : null
          });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, body: data, json: null });
        }
      });
    });
    req.on('error', reject);
    if (options.body) req.write(options.body);
    req.end();
  });
}

// 页面清单
const PAGE_LIST = [
  // API接口测试
  '/api/menu/user/client/pc-admin',
  '/api/sys/user/list',
  '/api/sys/role/list',
  '/api/sys/menu/list',
  '/api/erp/product/list',
  '/api/erp/partner/list',
  '/api/erp/stock/list',
  '/api/crm/customer/list',
  '/api/finance/receivable/list',
  '/api/system/config/list',
];

// 测试登录并获取Token
async function testLogin() {
  console.log('\n========== 测试登录 ==========');

  try {
    // 获取验证码
    const captchaRes = await httpRequest(`${API_BASE}/api/auth/captcha`);
    console.log('验证码获取:', captchaRes.status === 200 ? '成功' : '失败');

    // 尝试登录（开发环境可能不需要验证码）
    const loginRes = await httpRequest(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      body: JSON.stringify(CREDENTIALS)
    });

    console.log('登录响应:', loginRes.status, loginRes.json?.message || loginRes.body);

    if (loginRes.status === 200 && loginRes.json?.data?.token) {
      console.log('登录成功！Token:', loginRes.json.data.token.substring(0, 20) + '...');
      return loginRes.json.data.token;
    }

    // 登录失败，记录错误
    errors.push({
      type: 'auth',
      page: '/login',
      message: `登录失败: ${loginRes.json?.message || loginRes.body}`,
      timestamp: new Date().toISOString()
    });

    return null;
  } catch (error) {
    console.error('登录测试失败:', error.message);
    errors.push({
      type: 'auth',
      page: '/login',
      message: error.message,
      timestamp: new Date().toISOString()
    });
    return null;
  }
}

// 测试API接口
async function testApiEndpoints(token) {
  console.log('\n========== 测试API接口 ==========');

  const headers = token ? { 'Authorization': `Bearer ${token}` } : {};

  for (const endpoint of PAGE_LIST) {
    try {
      const res = await httpRequest(`${API_BASE}${endpoint}`, { headers });

      const result = {
        endpoint,
        status: res.status,
        success: res.status >= 200 && res.status < 300,
        message: res.json?.message || '',
        timestamp: new Date().toISOString()
      };

      pageResults.push(result);

      if (res.status === 401) {
        console.log(`[${res.status}] ${endpoint} - 需要登录`);
      } else if (res.status === 404) {
        console.log(`[${res.status}] ${endpoint} - 接口不存在`);
        errors.push({
          type: 'api',
          page: endpoint,
          message: `接口不存在 (404)`,
          timestamp: new Date().toISOString(),
          status: 404
        });
      } else if (res.status >= 500) {
        console.log(`[${res.status}] ${endpoint} - 服务器错误`);
        errors.push({
          type: 'api',
          page: endpoint,
          message: `服务器错误 (${res.status}): ${res.json?.message || res.body}`,
          timestamp: new Date().toISOString(),
          status: res.status
        });
      } else if (res.status === 200) {
        console.log(`[${res.status}] ${endpoint} - 成功`);
      } else {
        console.log(`[${res.status}] ${endpoint} - ${res.json?.message || res.body}`);
      }
    } catch (error) {
      console.error(`[ERROR] ${endpoint} - ${error.message}`);
      errors.push({
        type: 'api',
        page: endpoint,
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }

    // 添加延迟避免请求过快
    await new Promise(resolve => setTimeout(resolve, 200));
  }
}

// 测试前端页面（简单HTTP检查）
async function testFrontendPages() {
  console.log('\n========== 测试前端页面 ==========');

  const frontendPages = [
    '/',
    '/login',
    '/dashboard',
    '/sale',
    '/purchase',
    '/stock',
    '/erp/product',
    '/crm/customer',
    '/finance',
    '/system/user',
  ];

  for (const page of frontendPages) {
    try {
      const res = await httpRequest(`${FRONTEND_BASE}${page}`);

      const result = {
        page,
        status: res.status,
        success: res.status === 200,
        hasContent: res.body && res.body.length > 100,
        timestamp: new Date().toISOString()
      };

      pageResults.push(result);

      if (res.status === 200) {
        if (res.body.includes('vite.svg') || res.body.includes('企智连')) {
          console.log(`[${res.status}] ${page} - 正常（前端页面）`);
        } else {
          console.log(`[${res.status}] ${page} - 可能有问题（内容异常）`);
          errors.push({
            type: 'frontend',
            page,
            message: '页面内容异常',
            timestamp: new Date().toISOString()
          });
        }
      } else {
        console.log(`[${res.status}] ${page} - 错误`);
        errors.push({
          type: 'frontend',
          page,
          message: `HTTP ${res.status}`,
          timestamp: new Date().toISOString(),
          status: res.status
        });
      }
    } catch (error) {
      console.error(`[ERROR] ${page} - ${error.message}`);
      errors.push({
        type: 'frontend',
        page,
        message: error.message,
        timestamp: new Date().toISOString()
      });
    }
  }
}

// 生成报告
function generateReport() {
  console.log('\n========== 测试报告 ==========');
  console.log(`测试时间: ${new Date().toISOString()}`);
  console.log(`测试页面数: ${pageResults.length}`);
  console.log(`发现错误数: ${errors.length}`);

  // 按类型统计错误
  const errorTypes = {};
  errors.forEach(e => {
    errorTypes[e.type] = (errorTypes[e.type] || 0) + 1;
  });

  console.log('\n错误类型统计:');
  Object.entries(errorTypes).forEach(([type, count]) => {
    console.log(`  ${type}: ${count}`);
  });

  // 输出详细错误
  if (errors.length > 0) {
    console.log('\n详细错误列表:');
    errors.forEach((e, i) => {
      console.log(`\n[${i + 1}] ${e.type.toUpperCase()} - ${e.page}`);
      console.log(`    消息: ${e.message}`);
      if (e.status) console.log(`    状态: ${e.status}`);
      console.log(`    时间: ${e.timestamp}`);
    });
  }

  // 保存JSON报告
  const reportPath = 'i:/AI-Ready/tool-results/simple-test-report.json';
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      totalPages: pageResults.length,
      totalErrors: errors.length,
      errorTypes
    },
    errors,
    pageResults
  };

  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`\n报告已保存到: ${reportPath}`);
}

// 主测试流程
async function main() {
  console.log('========================================');
  console.log('ERP系统自动化测试脚本');
  console.log('========================================');
  console.log(`API地址: ${API_BASE}`);
  console.log(`前端地址: ${FRONTEND_BASE}`);

  // 1. 测试登录
  const token = await testLogin();

  // 2. 测试API接口
  await testApiEndpoints(token);

  // 3. 测试前端页面
  await testFrontendPages();

  // 4. 生成报告
  generateReport();
}

// 执行测试
main().catch(console.error);