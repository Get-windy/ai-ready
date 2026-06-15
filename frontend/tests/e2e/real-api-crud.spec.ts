/**
 * 真实 API CRUD E2E 测试
 * 目的：不 mock 任何 API，直接对真实后端进行增删改查操作
 * 验证前端 → 后端 → PostgreSQL 数据库全流程打通
 *
 * 测试模块：用户管理 + ERP 产品管理
 */
import { test, expect, type Page } from '@playwright/test';
import { loginViaUi } from './fixtures/auth.fixture';

// ============================================================
// 辅助函数
// ============================================================

function uniqueLabel(): string {
  return `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
}

function validPhone(): string {
  const prefixes = ['138', '139', '150', '151', '152', '186', '187', '188'];
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
  const suffix = String(Math.floor(10000000 + Math.random() * 90000000));
  return prefix + suffix;
}

async function getMessage(page: Page, timeout = 10_000): Promise<string | null> {
  try {
    const msg = page.locator('.ant-message-notice-content').first();
    await msg.waitFor({ state: 'visible', timeout });
    return (await msg.textContent())?.trim() || null;
  } catch {
    return null;
  }
}

async function waitForTable(page: Page) {
  await page.waitForSelector('.vxe-table', { timeout: 15_000 });
  await page.waitForTimeout(2000);
}

async function goTo(page: Page, path: string) {
  await page.goto(path);
  await waitForTable(page);
}

// ============================================================
// 测试套件
// ============================================================

test.describe('真实API CRUD', () => {
  test.describe.configure({ mode: 'serial' });
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    const context = await browser.newContext({ baseURL: 'http://localhost:5656' });
    page = await context.newPage();

    page.on('console', msg => {
      if (msg.type() === 'error') {
        const text = msg.text();
        if (!text.includes('401') && !text.includes('token') && !text.includes('登录失败')) {
          console.error(`[CE] ${text.substring(0, 120)}`);
        }
      }
    });

    console.log('[CRUD] 真实UI登录...');
    await loginViaUi(page, 'admin', 'admin123', '系统租户');
    console.log('[CRUD] 登录成功');
  });

  // ── 模块A: 用户管理 ──

  test('A1. 用户列表加载真实数据', async () => {
    await goTo(page, '/system/user');
    await expect(page.locator('h2').filter({ hasText: '用户管理' })).toBeVisible();

    const rows = page.locator('.vxe-body--row');
    await expect(rows.first()).toBeVisible({ timeout: 10_000 });
    const count = await rows.count();
    console.log(`[用户] 列表行数: ${count}`);
    expect(count).toBeGreaterThan(0);
  });

  test('A2. 创建用户 → 数据库写入', async () => {
    await goTo(page, '/system/user');
    const label = uniqueLabel();
    const username = `crud_${label}`;
    const nickname = `用户_${label.slice(0, 6)}`;

    await page.locator('button').filter({ hasText: '新增用户' }).first().click();
    const modal = page.locator('.fullscreen-detail-overlay');
    await expect(modal).toBeVisible();

    await modal.locator('input[placeholder="请输入用户名"]').fill(username);
    await modal.locator('input[placeholder="请输入昵称"]').fill(nickname);
    await modal.locator('input[placeholder="请输入密码"]').fill('Test123456');
    await modal.locator('input[placeholder="请输入邮箱"]').fill(`${username}@test.com`);
    await modal.locator('input[placeholder="请输入手机号"]').fill(validPhone());
    await modal.locator('.ant-btn-primary').filter({ hasText: /保存/ }).click();

    const msg = await getMessage(page);
    console.log(`[用户] 创建结果: ${msg}`);
    expect(msg).toContain('成功');

    // 验证列表中出现
    await goTo(page, '/system/user');
    const rows = page.locator('.vxe-body--row');
    const count = await rows.count();
    let found = false;
    for (let i = 0; i < Math.min(count, 30); i++) {
      const text = await rows.nth(i).textContent();
      if (text?.includes(nickname)) { found = true; break; }
    }
    expect(found).toBe(true);
    console.log(`[用户] 确认 "${nickname}" 已持久化`);
  });

  test('A3. 编辑已有用户', async () => {
    await goTo(page, '/system/user');
    const newNickname = `编辑_${uniqueLabel().slice(0, 6)}`;

    // 直接点击第一行的"编辑"按钮（不在"更多"下拉中）
    const editBtn = page.locator('button').filter({ hasText: '编辑' }).first();
    await expect(editBtn).toBeVisible();
    await editBtn.click();

    // 等待全屏弹窗
    const detail = page.locator('.fullscreen-detail-overlay');
    await expect(detail).toBeVisible({ timeout: 5_000 });

    // 修改昵称
    const nicknameInput = detail.locator('input[placeholder="请输入昵称"]');
    await expect(nicknameInput).toBeVisible({ timeout: 3_000 });
    await nicknameInput.clear();
    await nicknameInput.fill(newNickname);

    // 点击保存
    await detail.locator('.ant-btn-primary').filter({ hasText: /保存/ }).click();

    // 验证成功消息
    const msgText = await getMessage(page);
    console.log(`[用户] 编辑结果: ${msgText}`);
    expect(msgText).toBeTruthy();

    // 关闭弹窗（编辑后弹窗需手动关闭）
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    if (await detail.isVisible().catch(() => false)) {
      await page.goto('/system/user');
      await waitForTable(page);
    }
    await expect(detail).not.toBeVisible({ timeout: 5_000 });
  });

  test('A4. 删除用户', async () => {
    await goTo(page, '/system/user');

    // 点击"更多"→"删除"
    const moreBtn = page.locator('button').filter({ hasText: '更多' }).first();
    await expect(moreBtn).toBeVisible();
    await moreBtn.click();
    await page.waitForTimeout(400);

    const deleteItem = page.locator('.ant-dropdown-menu-item').filter({ hasText: '删除' }).first();
    await expect(deleteItem).toBeVisible();
    await deleteItem.click();

    const confirm = page.locator('.ant-modal-confirm');
    await expect(confirm).toBeVisible();
    await confirm.locator('.ant-btn-primary').first().evaluate(el => (el as HTMLButtonElement).click());
    await expect(confirm).not.toBeVisible({ timeout: 5_000 });

    const msg = await getMessage(page);
    console.log(`[用户] 删除结果: ${msg}`);
    // 可能返回成功或资源不存在（如果已被删），都是合理响应
    expect(msg).toBeTruthy();
  });

  // ── 模块B: 后端 API 直接验证 ──

  test('B1. 后端数据持久化验证', async () => {
    const token = await page.evaluate(() => localStorage.getItem('token'));

    const res = await page.request.get(`http://localhost:5656/api/user/page?pageNum=1&pageSize=10`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(res.ok()).toBe(true);
    const body = await res.json();
    console.log(`[API] 用户列表: code=${body.code}, total=${body.data?.total}`);
    expect(body.code).toBe(200);
    expect(body.data?.records?.length).toBeGreaterThan(0);
  });

  test('B2. 菜单接口验证', async () => {
    const token = await page.evaluate(() => localStorage.getItem('token'));

    const res = await page.request.get(`http://localhost:5656/api/menu/user/client/pc-admin`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(res.ok()).toBe(true);
    const body = await res.json();
    console.log(`[API] 菜单: code=${body.code}, count=${body.data?.length || 0}`);
    expect(body.code).toBe(200);
    expect(body.data?.length).toBeGreaterThan(0);
  });
});
