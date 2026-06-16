/**
 * 深度CRUD测试共享工具
 */
import { type Page } from '@playwright/test';
import { loginViaUi } from '../fixtures/auth.fixture';

// ==================== 导航 ====================

/** 通过侧边栏菜单点击导航（支持 display group 和 sub-menu） */
export async function navigateByMenuClick(page: Page, menuText: string, parentMenu?: string): Promise<boolean> {
  try {
    await page.waitForSelector('.ant-menu', { timeout: 10000 }).catch(() => { throw new Error('菜单未找到'); });

    if (parentMenu) {
      // 尝试展开父级 sub-menu（对 display group 无影响）
      const subMenu = page.locator('.ant-menu-submenu-title', { hasText: parentMenu }).first();
      if (await subMenu.isVisible({ timeout: 2000 }).catch(() => false)) {
        const isOpen = await subMenu.locator('..').locator('..')
          .evaluate(el => el.classList.contains('ant-menu-submenu-open')).catch(() => false);
        if (!isOpen) { await subMenu.click(); await page.waitForTimeout(600); }
      }
    }

    // 查找匹配的可见菜单项（解决同文本项在折叠子菜单中的干扰）
    let targetItem = page.locator('.ant-menu-item', { hasText: menuText });
    if (await targetItem.count().catch(() => 0) === 0) return false;

    // 遍历寻找可见项
    const count = await targetItem.count().catch(() => 0);
    let found = false;
    for (let i = 0; i < count; i++) {
      const candidate = targetItem.nth(i);
      if (await candidate.isVisible().catch(() => false)) {
        // 滚动到视图内再点击（解决侧边栏过长溢出问题）
        await candidate.evaluate(el => el.scrollIntoView({ block: 'center', behavior: 'instant' }));
        await page.waitForTimeout(400);
        await candidate.click();
        await page.waitForTimeout(2000);
        found = true;
        break;
      }
    }
    if (found) return true;

    // 兜底 1：在 sub-menu 内查找（可折叠的子菜单项）
    const subFallback = page.locator('.ant-menu-submenu .ant-menu-item', { hasText: menuText }).first();
    if (await subFallback.isVisible({ timeout: 1500 }).catch(() => false)) {
      await subFallback.evaluate(el => el.scrollIntoView({ block: 'center', behavior: 'instant' }));
      await page.waitForTimeout(400);
      await subFallback.click();
      await page.waitForTimeout(2000);
      return true;
    }

    // 兜底 2：在 displayGroup=1 分组内查找（始终展开的分组如"客户关系""财务管理"）
    const groupFallback = page.locator('.ant-menu-item-group-list .ant-menu-item', { hasText: menuText }).first();
    if (await groupFallback.isVisible({ timeout: 1500 }).catch(() => false)) {
      await groupFallback.evaluate(el => el.scrollIntoView({ block: 'center', behavior: 'instant' }));
      await page.waitForTimeout(400);
      await groupFallback.click();
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

/** 综合导航：优先菜单点击，验证 URL，失败回退 URL 导航 */
export async function navigateToPage(page: Page, pageDef: { menuText: string; path: string; parentMenu?: string }): Promise<boolean> {
  await navigateByMenuClick(page, pageDef.menuText, pageDef.parentMenu);
  await page.waitForTimeout(1500);

  // 验证 URL 是否已跳转到目标路径
  const currentUrl = page.url();
  if (!currentUrl.includes(pageDef.path)) {
    // 菜单导航未生效，回退到 URL 直跳
    await navigateByUrl(page, pageDef.path);
    await page.waitForTimeout(1500);
  }

  await page.waitForTimeout(1000);
  try { await page.waitForSelector('.ant-spin-spinning', { state: 'hidden', timeout: 10000 }); } catch {}
  const finalUrl = page.url();
  return finalUrl.includes(pageDef.path);
}

// ==================== 页面检查 ====================

/** 检查表格（支持 vxe-table / ant-table / 普通 table） */
export async function checkTableHasRows(page: Page): Promise<{ visible: boolean; count: number }> {
  const selectors = ['.vxe-table', '.ant-table', '.ant-table-wrapper', 'table'];
  let found = false;
  for (const sel of selectors) {
    if (await page.locator(sel).first().isVisible({ timeout: 3000 }).catch(() => false)) { found = true; break; }
  }
  if (!found) return { visible: false, count: -1 };

  // 尝试多种行选择器
  const rowSelectors = ['.vxe-body--row', '.ant-table-row', 'table tr', '.vxe-row'];
  for (const rs of rowSelectors) {
    const rows = page.locator(rs);
    const cnt = await rows.count().catch(() => 0);
    if (cnt > 0) return { visible: true, count: cnt };
  }
  return { visible: true, count: 0 };
}

/** 等待表格出现（等待可见，确保完全渲染） */
export async function waitForTable(page: Page): Promise<void> {
  try { await page.waitForSelector('.vxe-table, .ant-table, .ant-table-wrapper, table', { state: 'visible', timeout: 20000 }); await page.waitForTimeout(1000); } catch {}
}

/** 生产级检查 */
export async function checkProductionGrade(page: Page): Promise<string[]> {
  const issues: string[] = [];
  if (await page.locator('.ant-spin-spinning').isVisible({ timeout: 500 }).catch(() => false)) issues.push('页面持续加载中');
  if (await page.locator('.ant-alert-error').isVisible({ timeout: 500 }).catch(() => false)) issues.push('页面显示错误提示');
  return issues;
}

/** 页面质量断言：收集所有问题并在最后统一抛出 */
export async function assertPageQuality(page: Page, pageDef: {
  name: string; hasTable: boolean; hasNewBtn: boolean;
  menuText: string; path: string; parentMenu?: string;
}): Promise<void> {
  const errs: string[] = [];

  // 菜单导航：如果 menuText 为空，直接走 URL 导航，不检查菜单
  let menuOk = true;
  if (pageDef.menuText) {
    menuOk = await navigateToPage(page, pageDef);
    if (!menuOk) errs.push(`菜单导航失败（${pageDef.menuText}）`);
  } else {
    await navigateByUrl(page, pageDef.path);
    await page.waitForTimeout(1000);
  }

  let rows = -1;
  if (pageDef.hasTable) {
    await waitForTable(page);
    const t = await checkTableHasRows(page);
    rows = t.count;
    if (!t.visible) errs.push('表格未渲染');
  }

  const prodIssues = await checkProductionGrade(page);
  errs.push(...prodIssues);

  let newOk = false, detailOk = false;
  if (pageDef.hasNewBtn) {
    const urlBefore = page.url();
    newOk = await clickNewButton(page);
    if (newOk) {
      await page.waitForTimeout(800);
      detailOk = await isDetailOpen(page);
      // 有些页面使用路由跳转（如 router.push('/xxx/create')）而非弹窗
      if (!detailOk) {
        const urlAfter = page.url();
        if (urlAfter !== urlBefore) detailOk = true;
      }
      if (!detailOk) errs.push('未弹出详情');
    } else {
      errs.push('未找到新增按钮');
    }
  }

  console.log(`[结果] ${pageDef.name} 菜单=${menuOk ? 'Y':'N'} 行=${rows} 新=${newOk ? 'Y':'N'} 详=${detailOk ? 'Y':'N'} 误=${errs.length}`);

  if (errs.length > 0) {
    throw new Error(`${pageDef.name}: ${errs.join('; ')}`);
  }
}

// ==================== CRUD ====================

/** 点击"新增/新建"按钮 */
export async function clickNewButton(page: Page): Promise<boolean> {
  const btn = page.locator('button').filter({ hasText: /新增|新建|创建|生成|发起/ }).first();
  try {
    // 等待按钮出现并可交互
    await btn.waitFor({ state: 'visible', timeout: 8000 }).catch(() => {});
    if (await btn.isVisible({ timeout: 2000 }).catch(() => false)) {
      if (await btn.isEnabled().catch(() => false)) {
        await btn.evaluate(el => el.scrollIntoView({ block: 'center', behavior: 'instant' }));
        await page.waitForTimeout(300);
        await btn.click();
        await page.waitForTimeout(1200);
        return true;
      }
    }
  } catch {}
  return false;
}

/** 检查弹窗是否打开（支持多种弹窗类型） */
export async function isDetailOpen(page: Page): Promise<boolean> {
  // 等待一下让弹窗动画完成
  await page.waitForTimeout(800);
  const selectors = [
    '.fullscreen-detail-overlay',
    '.ant-modal-wrap',
    '.ant-modal',
    '.ant-modal-content',
    '.ant-drawer',
    '.ant-drawer-content-wrapper',
    '[role="dialog"]',
  ];
  for (const sel of selectors) {
    if (await page.locator(sel).first().isVisible({ timeout: 2000 }).catch(() => false)) return true;
  }
  return false;
}

/** 重置页面状态（确保下一个测试能正常导航） */
export async function resetPageState(page: Page): Promise<void> {
  try {
    // 1. 强制清除所有弹窗
    await page.evaluate(() => {
      document.querySelectorAll('.ant-modal-wrap, .ant-modal-mask, .fullscreen-detail-overlay, [role="dialog"], .ant-drawer, .ant-drawer-content-wrapper').forEach(el => el.remove());
      document.body.style.overflow = '';
      document.body.style.position = '';
    }).catch(() => {});
    // 2. 重新加载应用（goto + reload 确保 Vue 完全重新初始化）
    await page.goto('/dashboard', { waitUntil: 'load', timeout: 15000 }).catch(() => {});
    await page.reload({ waitUntil: 'networkidle', timeout: 20000 }).catch(() => {});
    // 3. 检查是否被重定向到登录页（会话丢失），自动重新登录
    const currentUrl = page.url();
    if (currentUrl.includes('/login') || currentUrl.includes('/auth')) {
      console.log('[resetPageState] 检测到登录页，重新登录...');
      await loginViaUi(page, 'admin', 'admin123', '系统租户');
      await page.goto('/dashboard', { waitUntil: 'load', timeout: 15000 }).catch(() => {});
      await page.reload({ waitUntil: 'networkidle', timeout: 20000 }).catch(() => {});
    }
    // 4. 等待菜单完全渲染
    try { await page.waitForSelector('.ant-menu', { state: 'visible', timeout: 15000 }); } catch {}
    await page.waitForTimeout(2000);
  } catch {}
}
