import { test, expect } from '../fixtures/auth.fixture';

test.describe('用户管理', () => {
  test.beforeEach(async ({ adminPage }) => {
    const page = adminPage;
    // 导航到用户管理页面
    await page.goto('/system/user');
    // 等待表格渲染（vxe-table）
    await page.waitForSelector('.vxe-table', { timeout: 15_000 });
  });

  test('显示用户列表', async ({ adminPage }) => {
    const page = adminPage;

    // 验证页面标题
    await expect(page.locator('.user-page-header-title')).toContainText('用户管理');

    // 验证表格行已加载（mock 数据有 12 条记录，vxe-table 有固定列会重复渲染行）
    const rows = page.locator('.vxe-body--row');
    await expect(rows.first()).toBeVisible();
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);

    // 验证分页组件存在
    await expect(page.locator('.ant-pagination')).toBeVisible();

    // 验证操作按钮存在（批量删除按钮仅在选中行时出现，此处不验证）
    await expect(page.locator('button').filter({ hasText: '新增用户' })).toBeVisible();
  });

  test('搜索过滤用户', async ({ adminPage }) => {
    const page = adminPage;

    // 打开筛选面板
    const filterBtn = page.locator('.table-toolbar button').filter({ hasText: '筛选' });
    await filterBtn.click();
    await page.waitForTimeout(300);

    // 在用户名搜索框中输入
    const searchInput = page.locator('.filter-panel input[placeholder="请输入用户名"]');
    await searchInput.fill('user1');

    // 点击查询按钮（Ant Design 按钮渲染文本时可能有空格，用 first() 定位第一个按钮）
    await page.locator('.filter-panel .ant-btn-primary').click();

    // 等待表格更新
    await page.waitForTimeout(500);

    // 验证过滤后的结果（只有 user1, user10, user11 匹配 "user1"）
    const rows = page.locator('.vxe-body--row');
    const rowCount = await rows.count();
    expect(rowCount).toBeGreaterThan(0);

    // 验证第一条记录包含 "user1"
    await expect(rows.first()).toContainText('user1');

    // 点击重置按钮恢复（"重置" 按钮在 filter-panel 中是最后一个 button）
    await page.locator('.filter-panel .filter-actions .ant-btn:not(.ant-btn-primary)').click();
    await page.waitForTimeout(500);

    // 验证重置后所有数据恢复
    const rowsAfterReset = page.locator('.vxe-body--row');
    await expect(rowsAfterReset.first()).toBeVisible();
  });

  test('创建新用户', async ({ adminPage }) => {
    const page = adminPage;

    // 点击新增用户按钮
    await page.locator('button').filter({ hasText: '新增用户' }).click();

    // 等待全屏弹窗出现
    const detail = page.locator('.fullscreen-detail-overlay');
    await expect(detail).toBeVisible();
    await expect(detail.locator('.detail-title')).toContainText('新增用户');

    // 填写表单
    await detail.locator('input[placeholder="请输入用户名"]').fill('newuser');
    await detail.locator('input[placeholder="请输入昵称"]').fill('新用户');
    await detail.locator('input[placeholder="请输入密码"]').fill('Password123');
    await detail.locator('input[placeholder="请输入邮箱"]').fill('newuser@example.com');
    await detail.locator('input[placeholder="请输入手机号"]').fill('13800000000');

    // 点击保存按钮
    await detail.locator('.ant-btn-primary').filter({ hasText: /保存/ }).click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });

    // 验证弹窗关闭
    await expect(detail).not.toBeVisible({ timeout: 5_000 });
  });

  test('编辑已有用户', async ({ adminPage }) => {
    const page = adminPage;

    // 点击第一行的编辑按钮（vxe-table 固定列会在多个表格中重复渲染 .vxe-body--row，
    // 因此不通过行内查找，而是直接定位页面上第一个"编辑"按钮）
    await page.locator('button').filter({ hasText: '编辑' }).first().click();

    // 等待全屏弹窗出现
    const detail = page.locator('.fullscreen-detail-overlay');
    await expect(detail).toBeVisible();
    await expect(detail.locator('.detail-title')).toContainText('编辑用户');

    // 修改昵称
    const nicknameInput = detail.locator('input[placeholder="请输入昵称"]');
    await nicknameInput.clear();
    await nicknameInput.fill('修改后的昵称');

    // 点击保存按钮
    await detail.locator('.ant-btn-primary').filter({ hasText: /保存/ }).click();

    // 验证成功消息
    await expect(page.locator('.ant-message-notice-content').first()).toBeVisible({ timeout: 5_000 });

    // 验证弹窗关闭
    await expect(detail).not.toBeVisible({ timeout: 5_000 });
  });

  test('删除用户并确认', async ({ adminPage }) => {
    const page = adminPage;

    // 点击第一行的"更多"下拉按钮（直接从页面级别定位第一个"更多"按钮）
    await page.locator('button').filter({ hasText: '更多' }).first().click();

    // 等待下拉菜单出现，点击"删除"
    await page.locator('.ant-dropdown-menu-item').filter({ hasText: '删除' }).click();

    // 等待确认对话框
    const confirmDialog = page.locator('.ant-modal-confirm');
    await expect(confirmDialog).toBeVisible({ timeout: 5_000 });
    await expect(confirmDialog).toContainText('确认删除');

    // 点击确认按钮（Modal.confirm 的按钮渲染在视口外，使用 evaluate 触发原生 click）
    await confirmDialog.locator('.ant-btn-primary').evaluate(el => (el as HTMLButtonElement).click());

    // 验证对话框关闭（表示 onOk 已触发）
    await expect(confirmDialog).not.toBeVisible({ timeout: 5_000 });
  });

  test('批量删除选中用户', async ({ adminPage }) => {
    const page = adminPage;

    // 选中前两行（vxe-table 使用自定义 checkbox 图标元素，类名为 .vxe-checkbox--icon）
    const checkboxes = page.locator('.vxe-body--row .vxe-checkbox--icon');
    const checkboxCount = await checkboxes.count();
    if (checkboxCount >= 2) {
      await checkboxes.nth(0).click();
      await checkboxes.nth(1).click();
    }

    // 等待批量操作栏出现（选中行后 .batch-bar 显示，包含"批量删除"按钮）
    const batchBar = page.locator('.batch-bar');
    await expect(batchBar).toBeVisible({ timeout: 5_000 });
    const batchDeleteBtn = batchBar.locator('button').filter({ hasText: '批量删除' });
    await expect(batchDeleteBtn).toBeEnabled();

    // 点击批量删除
    await batchDeleteBtn.click();

    // 等待第一个确认对话框（VxeTableList 的 handleBatchDelete，有"此操作不可撤销"文本）
    const firstConfirm = page.locator('.ant-modal-confirm').first();
    await expect(firstConfirm).toBeVisible({ timeout: 5_000 });
    await expect(firstConfirm).toContainText('此操作不可撤销');

    // 点击"确认删除"（okType:'danger'，按钮在视口外，使用 evaluate）
    await firstConfirm.locator('button').filter({ hasText: '确认删除' }).evaluate(el => (el as HTMLButtonElement).click());

    // 等待第二个确认对话框（父组件的 handleBatchDelete，有"2 个用户"文本）
    const secondConfirm = page.locator('.ant-modal-confirm').last();
    await expect(secondConfirm).toBeVisible({ timeout: 5_000 });
    await expect(secondConfirm).toContainText('2 个用户');

    // 点击"确 定"按钮（按钮在视口外，使用 evaluate）
    await secondConfirm.locator('.ant-btn-primary').evaluate(el => (el as HTMLButtonElement).click());

    // 验证对话框关闭
    await expect(secondConfirm).not.toBeVisible({ timeout: 5_000 });
  });

  test('分页导航', async ({ adminPage }) => {
    const page = adminPage;

    // 验证分页组件存在
    const pagination = page.locator('.ant-pagination');
    await expect(pagination).toBeVisible();

    // 验证当前页码显示
    const activePage = pagination.locator('.ant-pagination-item-active');
    await expect(activePage).toContainText('1');

    // 总共有 25 条 mock 数据，每页 20 条，应有 2 页
    // 点击第 2 页
    await pagination.locator('.ant-pagination-item').filter({ hasText: '2' }).click();

    // 等待表格更新
    await page.waitForTimeout(500);

    // 验证第 2 页激活
    const newActivePage = pagination.locator('.ant-pagination-item-active');
    await expect(newActivePage).toContainText('2');

    // 验证表格仍有数据
    const rows = page.locator('.vxe-body--row');
    await expect(rows.first()).toBeVisible();
  });
});
