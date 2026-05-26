#!/usr/bin/env node

/**
 * AI-Ready E2E测试执行脚本
 * Sprint 27+1测试环境配置 - E2E测试脚本执行
 * 
 * 使用方式：
 * node run-e2e-tests.js [options]
 * 
 * 选项：
 * --browser=<browser>  指定浏览器 (chromium|firefox|webkit|all)
 * --suite=<suite>      指定测试套件 (user-auth|order-flow|inventory-flow|crm-flow|all)
 * --base-url=<url>     指定测试URL (默认: http://localhost:8080)
 * --headed             以有头模式运行（显示浏览器）
 * --debug              启用调试模式
 * --report             生成测试报告
 */

const { spawn } = require('child_process');
const path = require('path');

// 解析命令行参数
const args = process.argv.slice(2);
const options = {};

args.forEach(arg => {
  if (arg.startsWith('--browser=')) {
    options.browser = arg.split('=')[1];
  } else if (arg.startsWith('--suite=')) {
    options.suite = arg.split('=')[1];
  } else if (arg.startsWith('--base-url=')) {
    options.baseUrl = arg.split('=')[1];
  } else if (arg === '--headed') {
    options.headed = true;
  } else if (arg === '--debug') {
    options.debug = true;
  } else if (arg === '--report') {
    options.report = true;
  }
});

// 默认配置
const config = {
  browser: options.browser || 'chromium',
  suite: options.suite || 'all',
  baseUrl: options.baseUrl || process.env.E2E_BASE_URL || 'http://localhost:8080',
  headed: options.headed || false,
  debug: options.debug || false,
  report: options.report || false
};

console.log('\n========================================');
console.log('AI-Ready E2E测试执行');
console.log('========================================');
console.log(`测试环境: ${config.baseUrl}`);
console.log(`浏览器: ${config.browser}`);
console.log(`测试套件: ${config.suite}`);
console.log(`运行模式: ${config.headed ? '有头模式' : '无头模式'}`);
console.log(`调试模式: ${config.debug ? '启用' : '禁用'}`);
console.log('========================================\n');

// 构建Playwright命令
const playwrightArgs = ['test'];

// 添加项目配置
if (config.browser !== 'all') {
  playwrightArgs.push('--project', config.browser);
}

// 添加测试文件匹配
if (config.suite !== 'all') {
  const suiteFile = `${config.suite}.spec.ts`;
  playwrightArgs.push(suiteFile);
}

// 添加有头模式
if (config.headed) {
  playwrightArgs.push('--headed');
}

// 添加调试模式
if (config.debug) {
  playwrightArgs.push('--debug');
}

// 设置环境变量
const env = {
  ...process.env,
  E2E_BASE_URL: config.baseUrl
};

// 执行Playwright测试
const playwright = spawn('npx', ['playwright', ...playwrightArgs], {
  cwd: path.resolve(__dirname),
  env: env,
  stdio: 'inherit',
  shell: true
});

playwright.on('error', (error) => {
  console.error('执行失败:', error.message);
  process.exit(1);
});

playwright.on('close', (code) => {
  console.log('\n========================================');
  if (code === 0) {
    console.log('✅ 测试执行成功');
    
    if (config.report) {
      console.log('\n生成测试报告...');
      spawn('npx', ['playwright', 'show-report'], {
        cwd: path.resolve(__dirname),
        stdio: 'inherit',
        shell: true
      });
    }
  } else {
    console.log('❌ 测试执行失败');
    console.log('退出码:', code);
  }
  console.log('========================================\n');
  
  process.exit(code);
});