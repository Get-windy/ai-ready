import { chromium, FullConfig } from '@playwright/test';

/**
 * 全局设置 - 在所有测试之前执行
 * 用于创建全局认证状态
 */
async function globalSetup(config: FullConfig) {
  const { baseURL, storageState } = config.projects[0].use;
  
  // 创建浏览器实例
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  // 登录测试用户
  await page.goto(`${baseURL}/login`);
  await page.fill('[name="username"]', 'test_user');
  await page.fill('[name="password"]', 'Test@123456');
  await page.click('button[type="submit"]');
  
  // 等待登录成功
  await page.waitForURL('/dashboard');
  
  // 保存认证状态
  await context.storageState({ path: storageState as string });
  
  await browser.close();
  
  console.log('Global setup completed: Authentication state saved');
}

export default globalSetup;
