import { defineConfig, devices } from '@playwright/test';

/**
 * AI-Ready Playwright配置
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './e2e',
  
  /* 并行执行测试 */
  fullyParallel: true,
  
  /* 禁止在CI中遗留test.only */
  forbidOnly: !!process.env.CI,
  
  /* 失败重试次数 */
  retries: process.env.CI ? 2 : 0,
  
  /* 工作进程数 */
  workers: process.env.CI ? 1 : undefined,
  
  /* 报告器配置 */
  reporter: [
    ['html', { outputFolder: 'reports/html', open: 'never' }],
    ['junit', { outputFile: 'reports/junit/results.xml' }],
    ['allure-playwright', { outputFolder: 'reports/allure' }],
    ['list']
  ],
  
  /* 共享配置 */
  use: {
    /* 基础URL */
    baseURL: process.env.TEST_BASE_URL || 'http://localhost:3000',
    
    /* 追踪配置 */
    trace: 'on-first-retry',
    
    /* 截图配置 */
    screenshot: 'only-on-failure',
    
    /* 视频配置 */
    video: 'retain-on-failure',
    
    /* 视口大小 */
    viewport: { width: 1920, height: 1080 },
    
    /* 动作超时 */
    actionTimeout: 15000,
    
    /* 导航超时 */
    navigationTimeout: 30000,
  },
  
  /* 项目配置 */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],
  
  /* Web服务器配置 */
  webServer: {
    command: 'npm run start:test',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },
  
  /* 全局设置 */
  globalSetup: require.resolve('./utils/global-setup'),
  globalTeardown: require.resolve('./utils/global-teardown'),
});
