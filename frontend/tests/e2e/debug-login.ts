import { chromium } from '@playwright/test';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  console.log('=== 打开登录页面 ===');
  await page.goto('/login');
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 });

  // 截图1: 初始状态
  await page.screenshot({ path: 'test-results/debug-login-1.png', fullPage: true });

  // 检查所有 input 元素
  const inputs = await page.locator('input').all();
  console.log(`找到 ${inputs.length} 个 input 元素`);
  for (const input of inputs) {
    const placeholder = await input.getAttribute('placeholder');
    const type = await input.getAttribute('type');
    const name = await input.getAttribute('name');
    const id = await input.getAttribute('id');
    console.log(`  input: placeholder="${placeholder}" type="${type}" name="${name}" id="${id}"`);
  }

  // 填写租户名称
  console.log('\n=== 填写表单 ===');
  await page.fill('input[placeholder="请输入租户名称"]', '系统租户');
  console.log('已填写 租户名称');

  // 填写用户名
  await page.fill('input[placeholder="请输入用户名"]', 'admin');
  console.log('已填写 用户名');

  // 填写密码
  await page.fill('input[placeholder="请输入密码"]', 'admin123');
  console.log('已填写 密码');

  // 填写验证码
  await page.fill('input[placeholder="请输入验证码"]', 'ABCD');
  console.log('已填写 验证码');

  // 截图2: 填写后状态
  await page.screenshot({ path: 'test-results/debug-login-2.png', fullPage: true });

  // 点击登录
  console.log('\n=== 点击登录 ===');
  await page.click('button[type="submit"]');

  // 等待2秒截图
  await page.waitForTimeout(2000);

  // 截图3: 提交后状态
  await page.screenshot({ path: 'test-results/debug-login-3.png', fullPage: true });
  console.log(`当前 URL: ${page.url()}`);

  // 输出页面内容前1000字符
  const content = await page.content();
  console.log(`页面标题: ${await page.title()}`);

  // 检查是否有 .ant-message 错误提示
  const errorMsgs = await page.locator('.ant-message-notice-content').all();
  console.log(`错误消息数: ${errorMsgs.length}`);
  for (const msg of errorMsgs) {
    console.log(`  消息: ${await msg.textContent()}`);
  }

  // 检查表单验证错误
  const formErrors = await page.locator('.ant-form-item-explain-error').all();
  console.log(`表单验证错误数: ${formErrors.length}`);
  for (const err of formErrors) {
    console.log(`  错误: ${await err.textContent()}`);
  }

  await browser.close();
  console.log('\n=== 调试完成 ===');
})().catch(e => {
  console.error('调试失败:', e.message);
  process.exit(1);
});
