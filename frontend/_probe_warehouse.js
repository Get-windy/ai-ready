// 抓取 ql361「资料 → 仓库管理 → 仓库规划」两个 Tab 的完整对标信息
// 用法: node _probe_warehouse.js
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT_DIR = process.argv[2] || 'I:/AI-Ready/tool-results/ql361/pages';
const SHOT_DIR = process.argv[3] || 'I:/AI-Ready/tool-results/ql361/warehouse-shots';
const ACC = '15095664266';
const PWD = 'Ysh790921';

const log = (...a) => console.log(...a);

function ensureDir(d) { if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true }); }

async function isReloginVisible(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('.dijitDialog, [role=dialog]')]
      .some(e => vis(e) && (e.innerText || '').includes('重新登录'));
  }).catch(() => false);
}

async function handleRelogin(page) {
  for (let i = 0; i < 4; i++) {
    if (!(await isReloginVisible(page))) return true;
    log('  [relogin] detected, re-auth...');
    await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlg = [...document.querySelectorAll('.dijitDialog, [role=dialog]')].find(e => vis(e) && (e.innerText || '').includes('重新登录'));
      if (!dlg) return;
      const inp = dlg.querySelector('input');
      if (inp) {
        const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
        setter.call(inp, 'Ysh790921');
        inp.dispatchEvent(new Event('input', { bubbles: true }));
        inp.dispatchEvent(new Event('change', { bubbles: true }));
      }
      const span = [...dlg.querySelectorAll('span.dijitButtonText')].find(s => s.textContent.trim() === '确定');
      (span && (span.closest('.dijitButton') || span) || { click() {} }).click();
    }).catch(() => {});
    await page.waitForTimeout(3500);
  }
  return !(await isReloginVisible(page));
}

async function closePopupDialogs(page) {
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitDialog')].filter(vis).forEach(d => {
      const t = (d.innerText || '').slice(0, 40);
      if (/预警|提醒|公告|移动端/.test(t)) {
        const c = d.querySelector('.dojoxDialogCloseIcon, .dijitDialogCloseIcon, [class*=CloseIcon]');
        if (c) c.click();
      }
    });
  }).catch(() => {});
  await page.waitForTimeout(800);
}

// 打开资料 → 仓库管理 → 仓库规划
async function openWarehousePlan(page) {
  const menu = page.locator('a.menu-0-item', { hasText: '资料' }).first();
  await menu.hover().catch(() => {});
  await page.waitForTimeout(500);
  await menu.click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
  const item = page.locator('.popupmenu__nav-text', { hasText: '仓库规划' }).first();
  await item.waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
  await item.click({ force: true }).catch(() => {});
  await page.waitForTimeout(9000);
}

// 页面内子标签（1.仓库 / 2.货位）
async function clickInnerTab(page, label) {
  const el = page.locator(`text=${label}`).first();
  const ok = await el.click({ force: true, timeout: 8000 }).then(() => true).catch(() => false);
  await page.waitForTimeout(4000);
  return ok;
}

// 关闭所有可见 dijit 弹窗（列配置/新增表单等）
async function closeAllDialogs(page) {
  for (let i = 0; i < 4; i++) {
    const n = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlgs = [...document.querySelectorAll('.dijitDialog')].filter(vis);
      dlgs.forEach(d => {
        const c = d.querySelector('.dijitDialogCloseIcon, .dojoxDialogCloseIcon, [class*=CloseIcon], [class*=close]');
        if (c) c.click();
      });
      document.querySelectorAll('.dijitDialogUnderlay').forEach(u => { u.style.display = 'none'; });
      return dlgs.length;
    }).catch(() => 0);
    await page.waitForTimeout(900);
    if (!n) break;
  }
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(700);
}

// 抓取工具栏按钮
async function dumpButtons(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const btns = [...document.querySelectorAll('span.dijitButtonText, button, a.lk-btn, [class*=btn]')]
      .filter(vis).map(b => (b.innerText || '').trim().replace(/\s+/g, ' '))
      .filter(t => t && t.length < 20);
    return [...new Set(btns)];
  }).catch(() => []);
}

// 抓取查询区（label + 控件类型）
async function dumpFilters(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    // 找到当前活动页签内容面板，缩小范围
    const panels = [...document.querySelectorAll('.module-box, [class*=content-pane], [class*=desktop-pane]')].filter(vis);
    const anchors = [...document.querySelectorAll('input')].filter(vis);
    const inputs = anchors.map(i => {
      let box = i.parentElement, txt = '';
      for (let k = 0; k < 4 && box; k++) {
        const t = (box.innerText || '').trim();
        if (t && t.length < 60) { txt = t; break; }
        box = box.parentElement;
      }
      return { type: i.type, ph: i.placeholder || '', val: i.value || '', near: txt.replace(/\s+/g, ' ') };
    }).filter(i => i.type !== 'hidden');
    const checks = [...document.querySelectorAll('input[type=checkbox]')].filter(vis).map(c => {
      const box = c.closest('label') || c.parentElement;
      return { label: (box?.innerText || '').trim().slice(0, 25), checked: c.checked };
    });
    const selects = [...document.querySelectorAll('.dijitSelect, .dijitDropDownButton')].filter(vis).map(s => (s.innerText || '').trim().slice(0, 30)).filter(Boolean);
    return { panels: panels.map(p => String(p.className).slice(0, 60)).slice(0, 6), inputs, checks, selects: [...new Set(selects)] };
  }).catch(e => ({ error: String(e) }));
}

// 抓取左侧分类树
async function dumpTree(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const roots = [...document.querySelectorAll('[class*=tree]')].filter(vis);
    if (!roots.length) return { found: false };
    const root = roots[0];
    const nodes = [...root.querySelectorAll('[class*=treeitem], [class*=node], li, .dijitTreeNode')]
      .filter(vis).map(n => (n.innerText || '').trim().replace(/\s+/g, ' ')).filter(t => t && t.length < 30);
    const head = [...document.querySelectorAll('[class*=tree] [class*=title], [class*=tree-header]')].filter(vis).map(h => (h.innerText || '').trim()).filter(Boolean);
    return { found: true, head: [...new Set(head)], nodes: [...new Set(nodes)].slice(0, 40), rootCls: String(root.className).slice(0, 80) };
  }).catch(e => ({ error: String(e) }));
}

// 抓取数据表格列头与首行数据
async function dumpGrid(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const headers = [...document.querySelectorAll('[class*=columnheader], .dojoxGridHeader th, .dgrid-header th, [role=columnheader]')]
      .filter(vis).map(h => (h.innerText || '').trim().replace(/\s+/g, ' ')).filter(Boolean);
    const rowCount = document.querySelectorAll('[role=row]').length;
    const firstRows = [...document.querySelectorAll('[role=row]')].slice(1, 6).map(r => (r.innerText || '').replace(/\n/g, ' | ').slice(0, 300));
    const pager = [...document.querySelectorAll('[class*=page], [class*=pager]')].filter(vis).map(p => (p.innerText || '').trim().replace(/\s+/g, ' ')).filter(t => t && t.length < 200);
    return { headers, rowCount, firstRows, pager: pager.slice(0, 3) };
  }).catch(e => ({ error: String(e) }));
}

// 查找表格齿轮（列配置入口）
async function findGearInfo(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const gears = [...document.querySelectorAll('[class*=shezhi], [class*=icon-setting], [title*=配置], [title*=设置]')]
      .filter(vis).map(g => ({
        cls: String(g.className).slice(0, 80),
        title: g.getAttribute('title') || '',
        rect: (() => { const r = g.getBoundingClientRect(); return [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height)]; })(),
        parentCls: String(g.parentElement?.className || '').slice(0, 80)
      }));
    return gears.slice(0, 20);
  }).catch(e => ({ error: String(e) }));
}

// 列配置弹窗内容
async function dumpColumnConfigDialog(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dialogs = [...document.querySelectorAll('[class*=dialog], [role=dialog], [class*=popup]')].filter(vis)
      .map(d => ({ cls: String(d.className).slice(0, 70), text: d.innerText || '' }))
      .filter(d => d.text.length > 40).sort((a, b) => b.text.length - a.text.length);
    const main = dialogs[0] || {};
    const lines = (main.text || '').split('\n').map(s => s.trim()).filter(Boolean);
    const tabs = [...document.querySelectorAll('[role=tab], [class*=tab]')].filter(vis).map(t => (t.innerText || '').trim()).filter(t => t && t.length < 20);
    const headerCells = [...document.querySelectorAll('[class*=columnheader], th, [role=columnheader], [class*=grid-header] [class*=cell]')]
      .filter(vis).map(h => (h.innerText || '').trim()).filter(Boolean);
    return { dialogClass: main.cls, lines, tabs: [...new Set(tabs)], headerCells: [...new Set(headerCells)].slice(0, 40) };
  }).catch(e => ({ error: String(e) }));
}

// 弹窗表单字段
async function dumpFormDialog(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dialogs = [...document.querySelectorAll('.dijitDialog, [role=dialog]')].filter(vis)
      .map(d => ({ cls: String(d.className).slice(0, 70), title: (d.querySelector('[class*=Title]') || {}).innerText || '', text: d.innerText || '' }))
      .filter(d => d.text.length > 20).sort((a, b) => b.text.length - a.text.length);
    const main = dialogs[0] || {};
    const fields = [...document.querySelectorAll('.dijitDialog label, .dijitDialog .lk-label, .dijitDialog [class*=label]')]
      .filter(vis).map(l => (l.innerText || '').trim()).filter(t => t && t.length < 25);
    const inputs = [...document.querySelectorAll('.dijitDialog input, .dijitDialog textarea, .dijitDialog select')]
      .filter(vis).map(i => ({ tag: i.tagName, type: i.type || '', ph: i.placeholder || '', val: i.value || '', cls: String(i.className).slice(0, 50) }));
    const required = [...document.querySelectorAll('.dijitDialog *')].filter(vis)
      .filter(e => e.children.length === 0 && /^\*$/.test((e.innerText || '').trim())).length;
    const buttons = [...document.querySelectorAll('.dijitDialog span.dijitButtonText')].filter(vis).map(b => b.innerText.trim());
    return { title: main.title, fields: [...new Set(fields)], inputs, requiredMarkCount: required, buttons, textPreview: (main.text || '').slice(0, 1500) };
  }).catch(e => ({ error: String(e) }));
}

async function shot(page, name) {
  ensureDir(SHOT_DIR);
  const p = path.join(SHOT_DIR, name);
  await page.screenshot({ path: p }).catch(() => {});
  return p;
}

(async () => {
  ensureDir(OUT_DIR); ensureDir(SHOT_DIR);
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.evaluate(() => {
    const box = document.querySelector('.clause .check_box'); if (box && !box.classList.contains('active')) box.click();
    const set = (el, v) => { if (!el) return; const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; s.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); };
    set(document.querySelector('input[placeholder="请输入手机号/用户名"]'), '15095664266');
    set(document.getElementById('pwd'), 'Ysh790921');
  }).catch(() => {});
  await page.waitForTimeout(600);
  await page.evaluate(() => { const b = document.querySelector('button.logon_button'); if (b && !b.disabled) b.click(); }).catch(() => {});
  for (let i = 0; i < 30; i++) { await page.waitForTimeout(1000); if (page.url().includes('desktop')) break; }
  await page.waitForTimeout(6000);
  log('LOGGED_IN', page.url());
  await closePopupDialogs(page);
  await page.keyboard.press('Escape').catch(() => {});

  const out = { ts: new Date().toISOString(), url: page.url() };

  // 若已有该页标签，先关掉重新开
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('span.tabLabel')].filter(vis).forEach(l => {
      if (l.textContent.trim() === '仓库规划') {
        const c = l.closest('.dijitTab')?.querySelector('.dijitTabCloseButton');
        if (c) c.click();
      }
    });
  }).catch(() => {});
  await page.waitForTimeout(1200);

  await openWarehousePlan(page);
  await handleRelogin(page);
  out.afterOpen = { url: page.url() };
  out.shotTab1 = await shot(page, 'wh-tab1-warehouse.png');

  // ── Tab1 1.仓库 ──
  out.tab1 = {
    buttons: await dumpButtons(page),
    filters: await dumpFilters(page),
    tree: await dumpTree(page),
    grid: await dumpGrid(page),
    gears: await findGearInfo(page)
  };

  // 列配置弹窗
  const ccClicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const g = [...document.querySelectorAll('[class*=dgrid-column-set] [class*=shezhi], [class*=grid] [class*=shezhi], [class*=header] [class*=shezhi]')].filter(vis)[0]
      || [...document.querySelectorAll('[class*=shezhi]')].filter(vis)[0];
    if (g) { g.click(); return String(g.className).slice(0, 60); }
    return null;
  }).catch(() => null);
  out.tab1.colConfigTrigger = ccClicked;
  if (ccClicked) {
    await page.waitForTimeout(2500);
    out.tab1.colConfigDialog = await dumpColumnConfigDialog(page);
    out.shotTab1ColCfg = await shot(page, 'wh-tab1-colconfig.png');
    await closeAllDialogs(page);
  }

  // 页面配置齿轮（顶部）
  const pcTrigger = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const cands = [...document.querySelectorAll('[class*=title] [class*=shezhi], .title_bar [class*=shezhi], [class*=toolbar] [class*=shezhi], [class*=iconButtonConfig]')].filter(vis);
    if (cands.length) { cands[0].click(); return String(cands[0].className).slice(0, 60); }
    return null;
  }).catch(() => null);
  out.tab1.pageConfigTrigger = pcTrigger;
  if (pcTrigger) {
    await page.waitForTimeout(2500);
    out.tab1.pageConfigDialog = await dumpColumnConfigDialog(page);
    await closeAllDialogs(page);
  }

  // 行操作菜单
  out.tab1.rowActions = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const rows = [...document.querySelectorAll('[role=row]')].filter(vis);
    if (rows.length < 2) return [];
    const cells = [...rows[1].querySelectorAll('*')].filter(e => e.children.length === 0 && /修改|删除|停用|启用/.test(e.innerText || ''));
    return cells.map(c => ({ text: c.innerText.trim(), tag: c.tagName, cls: String(c.className).slice(0, 50) }));
  }).catch(() => []);

  // 新增仓库弹窗
  const addClicked = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const s = [...document.querySelectorAll('span.dijitButtonText')].filter(vis).find(x => x.textContent.trim() === '新增');
    if (s) { (s.closest('.dijitButton') || s).click(); return true; }
    return false;
  }).catch(() => false);
  out.tab1.addClicked = addClicked;
  if (addClicked) {
    await page.waitForTimeout(3000);
    out.tab1.addDialog = await dumpFormDialog(page);
    out.shotTab1Add = await shot(page, 'wh-tab1-add.png');
    await closeAllDialogs(page);
  }

  // ── Tab2 2.货位 ──
  const tab2ok = await clickInnerTab(page, '2.货位');
  out.tab2ok = tab2ok;
  await handleRelogin(page);
  await page.waitForTimeout(2000);
  out.shotTab2 = await shot(page, 'wh-tab2-location.png');
  out.tab2 = {
    buttons: await dumpButtons(page),
    filters: await dumpFilters(page),
    tree: await dumpTree(page),
    grid: await dumpGrid(page),
    gears: await findGearInfo(page)
  };
  const cc2 = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const g = [...document.querySelectorAll('[class*=dgrid-column-set] [class*=shezhi], [class*=grid] [class*=shezhi], [class*=header] [class*=shezhi]')].filter(vis)[0]
      || [...document.querySelectorAll('[class*=shezhi]')].filter(vis)[0];
    if (g) { g.click(); return String(g.className).slice(0, 60); }
    return null;
  }).catch(() => null);
  out.tab2.colConfigTrigger = cc2;
  if (cc2) {
    await page.waitForTimeout(2500);
    out.tab2.colConfigDialog = await dumpColumnConfigDialog(page);
    out.shotTab2ColCfg = await shot(page, 'wh-tab2-colconfig.png');
    await closeAllDialogs(page);
  }

  // 新增货位弹窗
  const add2 = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const s = [...document.querySelectorAll('span.dijitButtonText')].filter(vis).find(x => x.textContent.trim() === '新增货位');
    if (s) { (s.closest('.dijitButton') || s).click(); return true; }
    return false;
  }).catch(() => false);
  out.tab2.addClicked = add2;
  if (add2) {
    await page.waitForTimeout(3000);
    out.tab2.addDialog = await dumpFormDialog(page);
    out.shotTab2Add = await shot(page, 'wh-tab2-add.png');
    await closeAllDialogs(page);
  }

  fs.writeFileSync(path.join(OUT_DIR, '资料__仓库管理__仓库规划__capture.json'), JSON.stringify(out, null, 1), 'utf-8');
  log('SAVED', path.join(OUT_DIR, '资料__仓库管理__仓库规划__capture.json'));
  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
