/**
 * 深度CRUD测试共享工具
 */
import { type Page } from '@playwright/test';

// ==================== 导航 ====================

/** 通过侧边栏菜单点击导航 */
export async function navigateByMenuClick(page: Page, menuText: string, parentMenu?: string): Promise<boolean> {
  try {
    await page.waitForSelector('.ant-menu', { timeout: 10000 }).catch(() => { throw new Error('菜单未找到'); });

    if (parentMenu) {
      const subMenu = page.locator('.ant-menu-submenu-title', { hasText: parentMenu }).first();
      if (await subMenu.isVisible({ timeout: 3000 }).catch(() => false)) {
        const isOpen = await subMenu.locator('..').locator('..')
          .evaluate(el => el.classList.contains('ant-menu-submenu-open')).catch(() => false);
        if (!isOpen) { await subMenu.click(); await page.waitForTimeout(500); }
      }
    }

    let item = page.locator('.ant-menu-item', { hasText: menuText }).first();
    if (!(await item.isVisible({ timeout: 1500 }).catch(() => false))) {
      item = page.locator('.ant-menu-submenu .ant-menu-item', { hasText: menuText }).first();
    }

    if (await item.isVisible({ timeout: 1500 }).catch(() => false)) {
      await item.click();
      await page.waitForTimeout(2000);
      return true;
    }
    return false;
  } catch { return false; }
}

/** URL导航（备选） */
export async function navigateByUrl(page: Page, path: string): Promise<void> {
  try { await page.goto(path, { waitUntil: 'load', timeout: 20000 }); await page.waitForTimeout(1500); } catch {}
}

/** 综合导航 */
export async function navigateToPage(page: Page, pageDef: { menuText: string; path: string; parentMenu?: string }): Promise<boolean> {
  const ok = await navigateByMenuClick(page, pageDef.menuText, pageDef.parentMenu);
  if (!ok) await navigateByUrl(page, pageDef.path);
  await page.waitForTimeout(1000);
  try { await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 10000 }); } catch {}
  return ok;
}

// ==================== 页面检查 ====================

/** 检查表格 */
export async function checkTableHasRows(page: Page): Promise<{ visible: boolean; count: number }> {
  const vxe = page.locator('.vxe-table');
  const ant = page.locator('.ant-table');
  const vxeOk = await vxe.isVisible({ timeout: 2000 }).catch(() => false);
  const antOk = await ant.isVisible({ timeout: 2000 }).catch(() => false);
  if (!vxeOk && !antOk) return { visible: false, count: -1 };
  const rows = page.locator('.vxe-body--row, .ant-table-row');
  return { visible: true, count: await rows.count().catch(() => 0) };
}

/** 等待表格 */
export async function waitForTable(page: Page): Promise<void> {
  try { await page.waitForSelector('.vxe-table, .ant-table', { timeout: 12000 }); await page.waitForTimeout(1000); } catch {}
}

/** 生产级检查 */
export async function checkProductionGrade(page: Page): Promise<string[]> {
  const issues: string[] = [];
  if (await page.locator('.ant-spin-spinning').isVisible({ timeout: 500 }).catch(() => false)) issues.push('页面持续加载中');
  if (await page.locator('.ant-alert-error').isVisible({ timeout: 500 }).catch(() => false)) issues.push('页面显示错误提示');
  return issues;
}

// ==================== CRUD ====================

/** 点击"新增/新建"按钮 */
export async function clickNewButton(page: Page): Promise<boolean> {
  const btn = page.locator('button').filter({ hasText: /新增|新建|创建/ }).first();
  if (await btn.isVisible({ timeout: 3000 }).catch(() => false)) {
    if (await btn.isEnabled().catch(() => false)) {
      await btn.click(); await page.waitForTimeout(1000); return true;
    }
  }
  return false;
}

/** 检查弹窗是否打开 */
export async function isDetailOpen(page: Page): Promise<boolean> {
  if (await page.locator('.fullscreen-detail-overlay').isVisible({ timeout: 1500 }).catch(() => false)) return true;
  if (await page.locator('.ant-modal-wrap').first().isVisible({ timeout: 1000 }).catch(() => false)) return true;
  if (await page.locator('.ant-drawer').first().isVisible({ timeout: 1000 }).catch(() => false)) return true;
  return false;
}

/** 重置页面状态 */
export async function resetPageState(page: Page): Promise<void> {
  try {
    await page.evaluate(() => {
      document.querySelectorAll('.ant-modal-wrap, .ant-modal-mask, .fullscreen-detail-overlay, [role="dialog"]').forEach(el => el.remove());
      document.body.style.overflow = '';
    }).catch(() => {});
    await page.goto('/dashboard', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(800);
  } catch {}
}
