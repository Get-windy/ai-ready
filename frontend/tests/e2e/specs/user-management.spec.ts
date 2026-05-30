import { test, expect } from '../fixtures/auth.fixture';

test.describe('用户管理', () => {
  test.beforeEach(async ({ adminPage }) => {
    const page = adminPage;
    // 导航到用户管理页面
    await page.goto('/system/user');
    // 等待表格渲染
    await page.waitForSelector('.ant-table', { timeout: 15_000 });
  });

  test('显示用户列表', async ({ adminPage }) => {
    const page = adminPage;

    // 验证页面标题
    await expect(page.locator('.table-header .title')).toContainText('用户列表');

    // 验证表格行已加载（mock 数据有 10 条记录在第 1 页）
    const rows = page.locator('.ant-table-row');
    await expect(rows.first()).toBeVisible();
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);
    expect(rowCount).toBeLessThanOrEqual(10);

    // 验证分页组件存在
    await expect(page.locator('.ant-pagination')).toBeVisible();

    // 验证操作按钮存在
    await expect(page.locator('button').filter({ hasText: '新增用户' })).toBeVisible();
    await expect(page.locator('button').filter({ hasText: '批量删除' })).toBeVisible();
  });

  test('搜索过滤用户', async ({ adminPage }) => {
    const page = adminPage;

    // 在用户名搜索框中输入
    const searchInput = page.locator('.search-card input[placeholder="请输入用户名"]');
    await searchInput.fill('user1');

    // 点击搜索按钮
    await page.locator('.search-card button').filter({ hasText: '搜索' }).click();

    // 等待表格更新
    await page.waitForTimeout(500);

    // 验证过滤后的结果（只有 user1, user10, user11 匹配 "user1"）
    const rows = page.locator('.ant-table-row');
    const rowCount = await rows.count();
    expect(rowCount).toBeLessThanOrEqual(3);

    // 验证第一条记录包含 "user1"
    await expect(rows.first()).toContainText('user1');

    // 点击重置按钮恢复
    await page.locator('.search-card button').filter({ hasText: '重置' }).click();
    await page.waitForTimeout(500);

    // 验证重置后所有数据恢复
    const rowsAfterReset = page.locator('.ant-table-row');
    await expect(rowsAfterReset.first()).toBeVisible();
  });

  test('创建新用户', async ({ adminPage }) => {
    const page = adminPage;

    // 点击新增用户按钮
    await page.locator('button').filter({ hasText: '新增用户' }).click();

    // 等待弹窗出现
    const modal = page.locator('.ant-modal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('.ant-modal-title')).toContainText('新增用户');

    // 填写表单
    await modal.locator('input[placeholder="请输入用户名"]').fill('newuser');
    await modal.locator('input[placeholder="请输入昵称"]').fill('新用户');
    await modal.locator('input[placeholder="请输入密码"]').fill('Password123');
    await modal.locator('input[placeholder="请输入邮箱"]').fill('newuser@example.com');
    await modal.locator('input[placeholder="请输入手机号"]').fill('13800000000');

    // 点击确定按钮提交
    await modal.locator('.ant-modal-footer .ant-btn-primary').click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });

    // 验证弹窗关闭
    await expect(modal).not.toBeVisible({ timeout: 5_000 });
  });

  test('编辑已有用户', async ({ adminPage }) => {
    const page = adminPage;

    // 点击第一行的编辑按钮
    const firstRow = page.locator('.ant-table-row').first();
    await firstRow.locator('.ant-btn-link').filter({ hasText: '编辑' }).click();

    // 等待弹窗出现
    const modal = page.locator('.ant-modal');
    await expect(modal).toBeVisible();
    await expect(modal.locator('.ant-modal-title')).toContainText('编辑用户');

    // 修改昵称
    const nicknameInput = modal.locator('input[placeholder="请输入昵称"]');
    await nicknameInput.clear();
    await nicknameInput.fill('修改后的昵称');

    // 点击确定按钮提交
    await modal.locator('.ant-modal-footer .ant-btn-primary').click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });

    // 验证弹窗关闭
    await expect(modal).not.toBeVisible({ timeout: 5_000 });
  });

  test('删除用户并确认', async ({ adminPage }) => {
    const page = adminPage;

    // 点击第一行的"更多"下拉按钮
    const firstRow = page.locator('.ant-table-row').first();
    await firstRow.locator('button').filter({ hasText: '更多' }).click();

    // 等待下拉菜单出现，点击"删除"
    await page.locator('.ant-dropdown-menu-item').filter({ hasText: '删除' }).click();

    // 等待确认对话框
    const confirmDialog = page.locator('.ant-modal-confirm');
    await expect(confirmDialog).toBeVisible({ timeout: 5_000 });
    await expect(confirmDialog).toContainText('确认删除');

    // 点击确认按钮
    await confirmDialog.locator('.ant-btn-primary').click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });
  });

  test('批量删除选中用户', async ({ adminPage }) => {
    const page = adminPage;

    // 选中前两行
    const rows = page.locator('.ant-table-row');
    await rows.nth(0).locator('.ant-checkbox').click();
    await rows.nth(1).locator('.ant-checkbox').click();

    // 等待批量删除按钮可用
    const batchDeleteBtn = page.locator('button').filter({ hasText: '批量删除' });
    await expect(batchDeleteBtn).toBeEnabled();

    // 点击批量删除
    await batchDeleteBtn.click();

    // 等待确认对话框
    const confirmDialog = page.locator('.ant-modal-confirm');
    await expect(confirmDialog).toBeVisible({ timeout: 5_000 });
    await expect(confirmDialog).toContainText('确认删除');

    // 点击确认
    await confirmDialog.locator('.ant-btn-primary').click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });
  });

  test('分页导航', async ({ adminPage }) => {
    const page = adminPage;

    // 验证第 1 页显示
    const pagination = page.locator('.ant-pagination');
    await expect(pagination).toBeVisible();

    // 验证当前页码显示
    const activePage = pagination.locator('.ant-pagination-item-active');
    await expect(activePage).toContainText('1');

    // 总共有 12 条 mock 数据，每页 10 条，应有 2 页
    // 点击第 2 页
    await pagination.locator('.ant-pagination-item').filter({ hasText: '2' }).click();

    // 等待表格更新
    await page.waitForTimeout(500);

    // 验证第 2 页激活
    const newActivePage = pagination.locator('.ant-pagination-item-active');
    await expect(newActivePage).toContainText('2');

    // 验证表格仍有数据（第 2 页应该有 2 条记录）
    const rows = page.locator('.ant-table-row');
    await expect(rows.first()).toBeVisible();
  });
});
