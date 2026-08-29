// 批量抓取双入口菜单页面的 页面配置 + 列配置（表单页 + 列表页）
// 用法: node _probe_cfg2.js <outDir>
// 读取 _probe_targets.json: [{domain, group, item, hasList}]
const { chromium } = require('playwright');
const fs = require('fs');

const targets = JSON.parse(fs.readFileSync('_probe_targets.json', 'utf-8'));
const outDir = process.argv[2] || '../tool-results/ql361/pages';

function dumpDialog(page) {
  return page.evaluate(() => {
    const vis = el => el && el.offsetParent !== null;
    const dialogs = [...document.querySelectorAll('[role=dialog], [class*=dialog], [class*=popup], [class*=drawer]')]
      .filter(vis).map(c => ({ cls: String(c.className).slice(0, 60), text: (c.innerText || '') }))
      .filter(d => d.text && d.text.length > 30);
    dialogs.sort((a, b) => b.text.length - a.text.length);
    const main = dialogs[0] || {};
    const lines = (main.text || '').split('\n').map(s => s.trim()).filter(s => s && s.length < 45);
    const checks = [...document.querySelectorAll('[role=dialog] label, [class*=dialog] label, [class*=popup] label, [class*=dialog] [class*=checkbox], [role=dialog] [class*=checkbox]')]
      .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 40);
    const tabs = [...document.querySelectorAll('[role=tab], [class*=dialog] [class*=tab], [class*=tab-nav]')]
      .filter(vis).map(e => (e.innerText || '').trim()).filter(t => t && t.length < 15);
    return { checks, tabs, lines, dialogClass: main.cls };
  });
}

async function grabPageConfig(page) {
  // 限定在可见的 tab 面板内找齿轮（多标签架构下避免点到其他标签页）
  const gear = page.locator('[class*=title] button:has-text("配置"), [class*=title] [class*=shezhi], .title_bar .icon-shezhi2, .lk-titlebar [class*=shezhi]').first();
  const cnt = await page.locator('[class*=title] [class*=shezhi], .title_bar .icon-shezhi2').count();
  if (cnt === 0) return { pageConfig: 'NO_GEAR' };
  await gear.click({ force: true }).catch(() => {});
  await page.waitForTimeout(2000);
  const data = await dumpDialog(page);
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(600);
  return { pageConfig: data };
}

async function grabColumnConfig(page) {
  const gear = page.locator('.dgrid-column-set .icon-shezhi2, .dgrid-header .icon-shezhi2, .grid-header [class*=shezhi], [class*=grid] [class*=shezhi]').first();
  const cnt = await page.locator('.dgrid-column-set .icon-shezhi2, .dgrid-header .icon-shezhi2').count();
  if (cnt === 0) return { columnConfig: 'NO_GEAR' };
  await gear.click({ force: true }).catch(() => {});
  await page.waitForTimeout(2000);
  const data = await dumpDialog(page);
  await page.keyboard.press('Escape').catch(() => {});
  await page.waitForTimeout(600);
  return { columnConfig: data };
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN' });
  const page = await ctx.newPage();

  // 登录
  await page.goto('https://www.ql361.com/Account/Logon', { waitUntil: 'networkidle', timeout: 45000 }).catch(() => {});
  await page.fill('input[placeholder="请输入手机号/用户名"]', '15095664266').catch(() => {});
  await page.fill('#pwd', 'Ysh790921').catch(() => {});
  await page.click('button.logon_button').catch(() => {});
  for (let i = 0; i < 25; i++) {
    await page.waitForTimeout(1000);
    if (!page.url().includes('/Account/Logon')) break;
  }
  await page.waitForTimeout(7000);
  await page.keyboard.press('Escape').catch(() => {});
  await page.evaluate(() => { document.querySelectorAll('.lk-dialog-close,[aria-label=Close],[title=关闭]').forEach(b => b.click()); }).catch(() => {});
  console.log('LOGGED_IN');

  async function handleRelogin() {
    for (let i = 0; i < 3; i++) {
      const has = await page.evaluate(() => {
        const vis = el => el && el.offsetParent !== null;
        return [...document.querySelectorAll('[class*=dialog], [role=dialog]')]
          .some(e => vis(e) && (e.innerText || '').includes('重新登录'));
      }).catch(() => false);
      if (!has) return true;
      console.log('  RE-LOGIN detected, retrying password...');
      await page.evaluate(() => {
        const vis = el => el && el.offsetParent !== null;
        const btns = [...document.querySelectorAll('[class*=dialog] button, [role=dialog] button')].filter(vis);
        const ok = btns.find(b => (b.innerText || '').includes('确定'));
        if (ok) ok.click();
      }).catch(() => {});
      await page.waitForTimeout(800);
      await page.fill('input[type=password]', 'Ysh790921').catch(() => {});
      await page.keyboard.press('Enter').catch(() => {});
      await page.waitForTimeout(3000);
    }
    return false;
  }

  for (const t of targets) {
    try {
      await handleRelogin();
      // 打开菜单
      await page.locator('a.menu-0-item', { hasText: t.domain }).first().click({ force: true }).catch(() => {});
      await page.waitForTimeout(1300);
      await page.evaluate(({ group, item }) => {
        const items = [...document.querySelectorAll('.popupmenu__nav-text')];
        // 先找组标题，再找组内的项；否则全局匹配
        const groups = [...document.querySelectorAll('.popupmenu__nav-item, .popupmenu__nav-title, [class*=popupmenu] [class*=title]')]
          .map(e => (e.innerText || '').trim().replace(/\s+/g, ''));
        const el = items.find(e => {
          const txt = (e.innerText || '').trim().replace(/\s+/g, '');
          return txt === item || (txt.includes(item) && item.length > 1);
        });
        if (el) el.click();
      }, { group: t.group, item: t.item }).catch(() => {});
      await page.waitForTimeout(6000);

      // 校验活动标签是否目标页
      const activeTab = await page.evaluate(() => {
        const vis = el => el && el.offsetParent !== null;
        const tabs = [...document.querySelectorAll('.dijitTab')].filter(vis);
        const act = tabs.find(t => t.classList.contains('dijitTabSelected'));
        return act ? (act.innerText || '').trim().slice(0, 20) : 'none';
      }).catch(() => 'err');
      console.log('  ACTIVE_TAB:', activeTab);

      // 关闭其他标签页（多标签桌面架构，避免齿轮点到别的页面）
      await page.evaluate(({ item }) => {
        const vis = el => el && el.offsetParent !== null;
        const tabs = [...document.querySelectorAll('.dijitTab')].filter(vis);
        for (const t of tabs) {
          const text = (t.innerText || '').trim();
          const selected = t.classList.contains('dijitTabSelected');
          // 保留：活动标签 或 含目标页面名的标签
          if (selected || text.includes(item)) continue;
          const close = t.querySelector('.dijitTabCloseButton');
          if (close) close.click();
        }
      }, { item: t.item }).catch(() => {});
      await page.waitForTimeout(1500);

      const pc = await grabPageConfig(page);
      const cc = await grabColumnConfig(page);

      let list = {};
      if (t.hasList) {
        // 点"历史"标签切到列表页（标签按钮）
        const tab = page.locator('[class*=tab]', { hasText: '历史' }).first();
        const tabCnt = await page.locator('[class*=tab]', { hasText: '历史' }).count();
        if (tabCnt > 0) {
          await tab.click({ force: true }).catch(() => {});
          await page.waitForTimeout(3500);
          const lpc = await grabPageConfig(page);
          const lcc = await grabColumnConfig(page);
          const shotName = `${t.domain}__${t.group}__${t.item}__list.png`.replace(/[\/:*?"<>|]/g, '_');
          const shot = `${outDir}/${shotName}`;
          await page.screenshot({ path: shot }).catch(() => {});
          list = { listPageConfig: lpc.pageConfig, listColumnConfig: lcc.columnConfig, listScreenshot: shot };
          // 切回表单页
          const back = page.locator('[class*=tab]', { hasText: t.item }).first();
          await back.click({ force: true }).catch(() => {});
          await page.waitForTimeout(1200);
        }
      }

      const result = { domain: t.domain, group: t.group, item: t.item, ts: new Date().toISOString(), ...pc, ...cc, ...list };
      const safe = `${t.domain}__${t.group}__${t.item}__cfg.json`.replace(/[\\/:*?"<>|]/g, '_');
      const empty = (v) => v === 'NO_GEAR' || !v || !v.lines || v.lines.length < 5;
      if (!empty(result.pageConfig) || !empty(result.listColumnConfig)) {
        fs.writeFileSync(`${outDir}/${safe}`, JSON.stringify(result, null, 1), 'utf-8');
      } else { console.log('SKIP empty:', safe); }
      console.log(`OK: ${t.domain}/${t.group}/${t.item} pc=${pc.pageConfig==='NO_GEAR'?'-':Object.keys(pc.pageConfig).length} cc=${cc.columnConfig==='NO_GEAR'?'-':Object.keys(cc.columnConfig).length} list=${t.hasList?'Y':'N'}`);
      // 关闭当前页标签
      await page.locator('[class*=tab] [class*=close], [class*=tab] [class*=icon-close]').first().click({ force: true }).catch(() => {});
      await page.waitForTimeout(800);
    } catch (e) {
      console.log(`FAIL: ${t.domain}/${t.item} ${e.message.slice(0, 80)}`);
    }
  }
  await browser.close();
  console.log('DONE');
})().catch(e => { console.error('FATAL', e.message); process.exit(1); });
