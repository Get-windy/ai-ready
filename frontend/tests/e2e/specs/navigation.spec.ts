import { test, expect } from '../fixtures/auth.fixture';
import { setAuthToken, setupAuthApiMocks } from '../fixtures/auth.fixture';

test.describe('导航和布局', () => {
  test.describe('侧边栏导航', () => {
    test('侧边栏菜单项点击后导航到对应页面', async ({ adminPage }) => {
      const page = adminPage;

      // 点击"采购管理"菜单项
      await page.locator('.ant-menu-item').filter({ hasText: '采购管理' }).click();

      // 验证页面跳转
      await page.waitForURL('**/purchase**', { timeout: 10_000 });
      expect(page.url()).toContain('/purchase');

      // 点击"工作台"返回
      await page.locator('.ant-menu-item').filter({ hasText: '工作台' }).click();
      await page.waitForURL('**/dashboard**', { timeout: 10_000 });
      expect(page.url()).toContain('/dashboard');
    });

    test('侧边栏子菜单展开与收起', async ({ adminPage }) => {
      const page = adminPage;

      // "系统管理"是一个子菜单，点击展开
      const systemSubmenu = page.locator('.ant-menu-submenu').filter({ hasText: '系统管理' });
      await systemSubmenu.click();

      // 验证子菜单项出现
      await expect(page.locator('.ant-menu-item').filter({ hasText: '用户管理' })).toBeVisible();

      // 点击子菜单项导航
      await page.locator('.ant-menu-item').filter({ hasText: '角色管理' }).click();
      await page.waitForURL('**/system/role**', { timeout: 10_000 });
      expect(page.url()).toContain('/system/role');
    });
  });

  test.describe('面包屑', () => {
    test('面包屑反映当前页面', async ({ adminPage }) => {
      const page = adminPage;

      // 默认在 dashboard，检查面包屑
      const breadcrumb = page.locator('.ant-breadcrumb').first();
      await expect(breadcrumb).toBeVisible();

      // 导航到用户管理
      await page.goto('/system/user');
      await page.waitForSelector('.vxe-table', { timeout: 15_000 });

      // 面包屑应该更新（具体内容取决于 i18n，但组件应该存在）
      await expect(breadcrumb).toBeVisible();
      const breadcrumbItems = breadcrumb.locator('.ant-breadcrumb-link, .ant-breadcrumb-separator');
      expect(await breadcrumbItems.count()).toBeGreaterThan(0);
    });
  });

  test.describe('侧边栏折叠', () => {
    test('侧边栏展开和折叠切换', async ({ adminPage }) => {
      const page = adminPage;

      // 初始状态：侧边栏应该是展开的
      const sider = page.locator('.ant-layout-sider');
      await expect(sider).toBeVisible();

      // 点击折叠触发器
      const trigger = page.locator('.trigger').first();
      await trigger.click();

      // 等待折叠动画完成，侧边栏应该有 collapsed 类
      await page.waitForTimeout(600);
      const collapsedSider = page.locator('.ant-layout-sider.ant-layout-sider-collapsed');
      await expect(collapsedSider).toBeVisible({ timeout: 5_000 });

      // 再次点击展开
      await trigger.click();

      // 等待展开动画完成
      await page.waitForTimeout(600);
      const expandedSider = page.locator('.ant-layout-sider:not(.ant-layout-sider-collapsed)');
      await expect(expandedSider).toBeVisible({ timeout: 5_000 });
    });
  });

  test.describe('错误页面', () => {
    test('显示 404 页面对于未知路由', async ({ adminPage }) => {
      const page = adminPage;

      // 导航到不存在的路由
      await page.goto('/this-page-does-not-exist-12345');

      // 等待页面渲染
      await page.waitForTimeout(1000);

      // 404 页面应该显示
      // Ant Design Result 组件
      const result = page.locator('.ant-result');
      await expect(result).toBeVisible({ timeout: 10_000 });

      // 验证包含 404 相关内容
      await expect(result).toContainText('404');
    });

    test('显示 403 页面', async ({ adminPage }) => {
      const page = adminPage;

      // 直接导航到 /403 路由
      await page.goto('/403');

      // 等待页面渲染
      await page.waitForSelector('.ant-result', { timeout: 10_000 });
      const result = page.locator('.ant-result');

      // 验证包含 403 相关内容
      await expect(result).toContainText('403');
      await expect(result).toContainText('没有权限');

      // 应该有一个"返回首页"按钮
      await expect(result.locator('button').filter({ hasText: '返回首页' })).toBeVisible();
    });
  });
});
