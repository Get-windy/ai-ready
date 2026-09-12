// 抓取 ql361 仓库规划的表单弹窗（新增仓库 / 新增货位 / 树操作 / 更多菜单）
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT_DIR = 'I:/AI-Ready/tool-results/ql361/pages';
const SHOT_DIR = 'I:/AI-Ready/tool-results/ql361/warehouse-shots';
const log = (...a) => console.log(...a);
const ensureDir = d => { if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true }); };

async function isRelogin(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    return [...document.querySelectorAll('.dijitDialog,[role=dialog]')].some(e => vis(e) && (e.innerText || '').includes('重新登录'));
  }).catch(() => false);
}
async function relogin(page) {
  for (let i = 0; i < 4; i++) {
    if (!(await isRelogin(page))) return true;
    log('  [relogin]');
    await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const dlg = [...document.querySelectorAll('.dijitDialog,[role=dialog]')].find(e => vis(e) && (e.innerText || '').includes('重新登录'));
      if (!dlg) return;
      const inp = dlg.querySelector('input');
      if (inp) { const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; s.call(inp, 'Ysh790921'); inp.dispatchEvent(new Event('input', { bubbles: true })); inp.dispatchEvent(new Event('change', { bubbles: true })); }
      const span = [...dlg.querySelectorAll('span.dijitButtonText')].find(x => x.textContent.trim() === '确定');
      (span && (span.closest('.dijitButton') || span) || { click() {} }).click();
    }).catch(() => {});
    await page.waitForTimeout(3500);
  }
  return !(await isRelogin(page));
}
async function closeDialogs(page) {
  for (let i = 0; i < 3; i++) {
    const n = await page.evaluate(() => {
      const vis = el => el && el.offsetParent !== null;
      const ds = [...document.querySelectorAll('.dijitDialog')].filter(vis);
      ds.forEach(d => { const c = d.querySelector('.dijitDialogCloseIcon,.dojoxDialogCloseIcon,[class*=CloseIcon]'); if (c) c.click(); });
      document.querySelectorAll('.dijitDialogUnderlay').forEach(u => u.style.display = 'none');
      return ds.length;
    }).catch(() => 0);
    await page.waitForTimeout(800);
    if (!n) break;
  }
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(600);
}

// 通用浮层 dump：找出所有可见浮层及其表单内容
async function dumpOverlays(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const cands = [...document.querySelectorAll('div')].filter(d => {
      if (!vis(d)) return false;
      const z = parseInt(getComputedStyle(d).zIndex || '0', 10);
      const cls = String(d.className || '');
      const isOverlay = z >= 100 || /dialog|popup|window|modal|overlay/i.test(cls);
      if (!isOverlay) return false;
      const t = (d.innerText || '').trim();
      return t.length > 10 && t.length < 3000;
    });
    const seen = new Set();
    const out = [];
    for (const d of cands) {
      const cls = String(d.className).slice(0, 90);
      if (seen.has(cls)) continue;
      seen.add(cls);
      const labels = [...d.querySelectorAll('label,.lk-label,[class*=label]')].filter(vis).map(l => (l.innerText || '').trim()).filter(t => t && t.length < 30);
      const reqMark = [...d.querySelectorAll('span,i,em,b')].filter(vis).filter(e => e.children.length === 0 && /^\*$/.test((e.innerText || '').trim())).length;
      const inputs = [...d.querySelectorAll('input,textarea,select')].filter(vis).map(i => ({
        tag: i.tagName, type: i.type || '', ph: i.placeholder || '', val: (i.value || '').slice(0, 30), cls: String(i.className).slice(0, 40), disabled: !!i.disabled
      }));
      out.push({
        cls, z: parseInt(getComputedStyle(d).zIndex || '0', 10),
        title: (d.querySelector('[class*=Title],[class*=title]')?.innerText || '').trim().slice(0, 30),
        labels: [...new Set(labels)], reqMark, inputs,
        buttons: [...d.querySelectorAll('span.dijitButtonText,button')].filter(vis).map(b => (b.innerText || '').trim()).filter(Boolean),
        text: (d.innerText || '').replace(/\n{2,}/g, '\n').slice(0, 1200)
      });
    }
    out.sort((a, b) => b.z - a.z);
    return out.slice(0, 5);
  }).catch(e => [{ error: String(e) }]);
}

(async () => {
  ensureDir(OUT_DIR); ensureDir(SHOT_DIR);
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();
  const out = { ts: new Date().toISOString() };
  const shot = async n => { const p = path.join(SHOT_DIR, n); await page.screenshot({ path: p }).catch(() => {}); return p; };

  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'domcontentloaded', timeout: 60000 }).catch(() => {});
  await page.waitForTimeout(3000);
  await page.evaluate(() => {
    const box = document.querySelector('.clause .check_box'); if (box && !box.classList.contains('active')) box.click();
    const set = (el, v) => { if (!el) return; const s = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set; s.call(el, v); el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); };
    set(document.querySelector('input[placeholder="请输入手机号/用户名"]'), '15095664266');
    set(document.getElementById('pwd'), 'Ysh790921');
  }).catch(() => {});
  await page.waitForTimeout(700);
  await page.evaluate(() => { const b = document.querySelector('button.logon_button'); if (b && !b.disabled) b.click(); }).catch(() => {});
  for (let i = 0; i < 30; i++) { await page.waitForTimeout(1000); if (page.url().includes('desktop')) break; }
  await page.waitForTimeout(7000);
  log('LOGGED_IN');
  await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    [...document.querySelectorAll('.dijitDialog')].filter(vis).forEach(d => { const c = d.querySelector('.dojoxDialogCloseIcon,.dijitDialogCloseIcon'); if (c) c.click(); });
  }).catch(() => {});
  await page.waitForTimeout(1000);
  await page.keyboard.press('Escape').catch(() => {});

  // 打开仓库规划
  const menu = page.locator('a.menu-0-item', { hasText: '资料' }).first();
  await menu.hover().catch(() => {});
  await menu.click({ force: true }).catch(() => {});
  await page.waitForTimeout(2500);
  const item = page.locator('.popupmenu__nav-text', { hasText: '仓库规划' }).first();
  await item.waitFor({ state: 'visible', timeout: 15000 }).catch(() => {});
  await item.click({ force: true }).catch(() => {});
  await page.waitForTimeout(9000);
  await relogin(page);
  await page.waitForTimeout(2000);

  // ── Tab1 新增仓库 ──
  let btn = page.locator('.dijitButton', { hasText: '新增' }).first();
  await btn.click({ force: true, timeout: 8000 }).catch(e => log('add click fail', e.message.slice(0, 60)));
  await page.waitForTimeout(3500);
  await relogin(page);
  out.addWarehouse = { overlays: await dumpOverlays(page), shot: await shot('wh-form-add-warehouse.png') };
  await closeDialogs(page);
  await relogin(page);

  // ── Tab1 编辑仓库（第一行「修改」）──
  const editLink = page.locator('a.grid-cell-link', { hasText: '修改' }).first();
  await editLink.click({ force: true, timeout: 8000 }).catch(e => log('edit click fail', e.message.slice(0, 60)));
  await page.waitForTimeout(3500);
  out.editWarehouse = { overlays: await dumpOverlays(page), shot: await shot('wh-form-edit-warehouse.png') };
  await closeDialogs(page);
  await relogin(page);

  // ── 树操作图标 ──
  out.treeOps = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const box = [...document.querySelectorAll('.module-tree, [class*=tree]')].filter(vis)[0];
    if (!box) return [];
    return [...box.querySelectorAll('*')].filter(vis).filter(e => /^(修改|删除|添加|收起树形菜单|展开树形菜单)$/.test((e.getAttribute('title') || '').trim()) || /修改|删除|添加|收起/.test(String(e.className)))
      .map(e => ({ tag: e.tagName, cls: String(e.className).slice(0, 60), title: e.getAttribute('title') || '', text: (e.innerText || '').trim().slice(0, 12) }));
  }).catch(() => []);

  // ── Tab2 货位：切标签 ──
  await page.locator('text=2.货位').first().click({ force: true, timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(4000);
  await relogin(page);
  out.shotTab2 = await shot('wh-form-tab2.png');

  // 「更多」菜单
  const moreBtn = page.locator('.dijitButton, .dijitDropDownButton, .dijitButtonNode', { hasText: '更多' }).first();
  await moreBtn.click({ force: true, timeout: 8000 }).catch(e => log('more click fail', e.message.slice(0, 60)));
  await page.waitForTimeout(2500);
  out.moreMenu = await page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const menus = [...document.querySelectorAll('[class*=menu],[class*=dropdown],[class*=popup]')].filter(vis)
      .map(m => ({ cls: String(m.className).slice(0, 70), text: (m.innerText || '').trim().slice(0, 300) }))
      .filter(m => m.text && m.text.length < 300);
    return menus.slice(0, 5);
  }).catch(() => []);
  out.shotMore = await shot('wh-form-more.png');
  await page.keyboard.press('Escape').catch(() => {});

  // 新增货位
  const add2 = page.locator('.dijitButton', { hasText: '新增货位' }).first();
  await add2.click({ force: true, timeout: 8000 }).catch(e => log('add2 click fail', e.message.slice(0, 60)));
  await page.waitForTimeout(3500);
  out.addLocation = { overlays: await dumpOverlays(page), shot: await shot('wh-form-add-location.png') };
  await closeDialogs(page);

  fs.writeFileSync(path.join(OUT_DIR, '资料__仓库管理__仓库规划__forms.json'), JSON.stringify(out, null, 1), 'utf-8');
  log('SAVED forms json');
  await browser.close();
  log('DONE');
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
