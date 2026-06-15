import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // 测试文件目录
  testDir: './tests/e2e',

  // 全局超时时间
  timeout: 30_000,

  // 每个测试的超时
  expect: {
    timeout: 10_000,
  },

  // 完全并行执行
  fullyParallel: true,

  // CI 中不允许 test.only
  forbidOnly: !!process.env.CI,

  // CI 中失败重试 1 次
  retries: process.env.CI ? 1 : 0,

  // CI 中单 worker，本地 1 个 worker 避免登录并发冲突
  workers: process.env.CI ? 1 : 1,

  // 报告器
  reporter: [
    ['html', { open: 'never' }],
    ['list'],
  ],

  // 全局使用的配置
  use: {
    // 基础 URL，所有 page.goto() 的相对路径都基于此
    baseURL: 'http://localhost:5656',

    // 失败时截图
    screenshot: 'only-on-failure',

    // 失败时保留录像
    video: 'retain-on-failure',

    // 首次重试时记录 trace
    trace: 'on-first-retry',
  },

  // 浏览器项目
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],

  // 开发服务器配置
  webServer: {
    command: 'pnpm --filter ai-ready-admin dev --port 5656',
    url: 'http://localhost:5656',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
    cwd: __dirname,
  },
});
