# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: full-system-scan.spec.ts >> ERP系统全页面扫描测试 >> 第 10 批页面测试 (10 个页面)
- Location: tests\e2e\full-system-scan.spec.ts:474:9

# Error details

```
Error: 登录失败，无法继续测试
```

# Page snapshot

```yaml
- generic [ref=e1]:
  - link "跳到主要内容" [ref=e2] [cursor=pointer]:
    - /url: "#main-content"
  - application [ref=e3]:
    - generic [ref=e6]:
      - generic [ref=e7]:
        - generic [ref=e8]:
          - generic [ref=e10]: AR
          - heading "企智连·AI-Ready" [level=1] [ref=e11]
        - paragraph [ref=e12]: 企业智能管理系统
      - form "Login" [ref=e13]:
        - textbox "租户名称" [ref=e20]:
          - /placeholder: 请输入租户名称
          - text: 默认租户 *
        - textbox "请输入用户名" [ref=e27]: admin *
        - generic [ref=e33]:
          - textbox "请输入密码" [ref=e34]: admin123 *
          - img "eye-invisible" [ref=e36] [cursor=pointer]:
            - img [ref=e37]
        - generic [ref=e45]:
          - textbox "请输入验证码" [ref=e47]: "*"
          - button "刷新验证码" [active] [ref=e48] [cursor=pointer]:
            - status "验证码加载中"
        - generic [ref=e49]:
          - generic [ref=e50] [cursor=pointer]:
            - checkbox "记住我" [ref=e52]
            - generic [ref=e54]: 记住我
          - button "忘记密码" [ref=e55] [cursor=pointer]: 忘记密码？
        - button "登 录" [ref=e61] [cursor=pointer]:
          - generic [ref=e62]: 登 录
        - generic [ref=e63]:
          - text: 还没有账号？
          - button "立即注册" [ref=e64] [cursor=pointer]:
            - generic [ref=e65]: 立即注册
        - generic [ref=e66]:
          - text: 企业用户？
          - link "企业注册" [ref=e67] [cursor=pointer]:
            - /url: /tenant-register
  - generic:
    - generic:
      - generic:
        - generic [ref=e69]:
          - img "close-circle" [ref=e70]:
            - img [ref=e71]
          - text: 请求错误 (429)
        - generic [ref=e74]:
          - img "exclamation-circle" [ref=e75]:
            - img [ref=e76]
          - text: 获取验证码失败，请刷新重试
```

# Test source

```ts
  355 |   try {
  356 |     console.log(`[测试] 开始测试页面: ${pagePath}`);
  357 | 
  358 |     // 设置错误监听
  359 |     page.on('console', msg => {
  360 |       if (msg.type() === 'error') {
  361 |         pageErrors.push({
  362 |           type: 'console',
  363 |           page: pagePath,
  364 |           message: msg.text(),
  365 |           timestamp: new Date().toISOString(),
  366 |           stack: msg.location?.toString()
  367 |         });
  368 |       }
  369 |     });
  370 | 
  371 |     page.on('response', response => {
  372 |       if (response.status() >= 400 && response.status() !== 401) {
  373 |         const url = response.url();
  374 |         if (!url.includes('/captcha') && !url.includes('/favicon')) {
  375 |           pageErrors.push({
  376 |             type: 'network',
  377 |             page: pagePath,
  378 |             message: `HTTP ${response.status()}`,
  379 |             timestamp: new Date().toISOString(),
  380 |             url: url,
  381 |             status: response.status()
  382 |           });
  383 |         }
  384 |       }
  385 |     });
  386 | 
  387 |     // 访问页面
  388 |     await page.goto(BASE_URL + pagePath, { waitUntil: 'networkidle', timeout: 30000 });
  389 |     await page.waitForTimeout(3000);
  390 | 
  391 |     // 检查页面是否白屏
  392 |     const bodyContent = await page.locator('body').innerHTML();
  393 |     if (!bodyContent || bodyContent.trim().length < 100) {
  394 |       pageErrors.push({
  395 |         type: 'visual',
  396 |         page: pagePath,
  397 |         message: '页面可能白屏',
  398 |         timestamp: new Date().toISOString()
  399 |       });
  400 |     }
  401 | 
  402 |     // 尝试点击常见按钮
  403 |     const buttons = await page.locator('button:visible').all();
  404 |     for (const btn of buttons.slice(0, 5)) {
  405 |       try {
  406 |         const btnText = await btn.textContent() || '';
  407 |         if (btnText && !btnText.includes('删除') && !btnText.includes('退出')) {
  408 |           await btn.click({ timeout: 2000 });
  409 |           await page.waitForTimeout(1000);
  410 |         }
  411 |       } catch (e) {
  412 |         // 按钮点击失败，忽略
  413 |       }
  414 |     }
  415 | 
  416 |     // 检查是否有错误弹窗
  417 |     const errorAlert = page.locator('.ant-message-error, .ant-alert-error');
  418 |     if (await errorAlert.isVisible()) {
  419 |       const alertText = await errorAlert.textContent() || '';
  420 |       pageErrors.push({
  421 |         type: 'visual',
  422 |         page: pagePath,
  423 |         message: `错误弹窗: ${alertText}`,
  424 |         timestamp: new Date().toISOString()
  425 |       });
  426 |     }
  427 | 
  428 |     console.log(`[测试] 页面 ${pagePath} 完成，发现 ${pageErrors.length} 个错误`);
  429 |     return pageErrors;
  430 | 
  431 |   } catch (error: any) {
  432 |     console.error(`[测试] 页面 ${pagePath} 测试失败:`, error.message);
  433 |     pageErrors.push({
  434 |       type: 'console',
  435 |       page: pagePath,
  436 |       message: `页面访问失败: ${error.message}`,
  437 |       timestamp: new Date().toISOString()
  438 |     });
  439 |     return pageErrors;
  440 |   }
  441 | }
  442 | 
  443 | // 主测试套件
  444 | test.describe('ERP系统全页面扫描测试', () => {
  445 |   let context: BrowserContext;
  446 |   let page: Page;
  447 | 
  448 |   test.beforeAll(async ({ browser }) => {
  449 |     context = await browser.newContext();
  450 |     page = await context.newPage();
  451 | 
  452 |     // 登录
  453 |     const loginSuccess = await login(page);
  454 |     if (!loginSuccess) {
> 455 |       throw new Error('登录失败，无法继续测试');
      |             ^ Error: 登录失败，无法继续测试
  456 |     }
  457 |   });
  458 | 
  459 |   test.afterAll(async () => {
  460 |     // 保存错误报告
  461 |     const reportPath = 'test-results/error-report.json';
  462 |     fs.writeFileSync(reportPath, JSON.stringify(errorLogs, null, 2));
  463 |     console.log(`[报告] 错误报告已保存到 ${reportPath}`);
  464 | 
  465 |     await context.close();
  466 |   });
  467 | 
  468 |   // 分批测试
  469 |   const batches = splitIntoBatches(PAGE_LIST, 10);
  470 | 
  471 |   for (let batchIndex = 0; batchIndex < batches.length; batchIndex++) {
  472 |     const batch = batches[batchIndex];
  473 | 
  474 |     test(`第 ${batchIndex + 1} 批页面测试 (${batch.length} 个页面)`, async () => {
  475 |       console.log(`\n========== 第 ${batchIndex + 1} 批测试开始 ==========`);
  476 | 
  477 |       for (const pagePath of batch) {
  478 |         const errors = await testPage(page, pagePath);
  479 |         errorLogs.push(...errors);
  480 | 
  481 |         // 保存页面截图
  482 |         const screenshotPath = `test-results/screenshots/${pagePath.replace('/', '_')}.png`;
  483 |         await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  484 |       }
  485 | 
  486 |       console.log(`[报告] 第 ${batchIndex + 1} 批完成，累计错误: ${errorLogs.length}`);
  487 |     });
  488 |   }
  489 | });
  490 | 
  491 | // 导出页面清单和分批信息
  492 | export { PAGE_LIST, splitIntoBatches, errorLogs };
```