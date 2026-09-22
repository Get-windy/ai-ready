#!/usr/bin/env node
/**
 * 商标注册初步核查：多源轮换 + 拟人节奏 + 结果缓存
 *
 * 用途：给候选品牌名做商标"初步可用性"筛查，重点看第 09 / 35 / 42 类
 *      （09 计算机软件、35 广告销售/商业管理、42 软件设计与开发）
 *
 * 用法：
 *   node tools/tm-clearance-check.mjs 羚铺 羚掌柜 羚小铺
 *   node tools/tm-clearance-check.mjs --file tools/tm-clearance-names.txt
 *   node tools/tm-clearance-check.mjs --classes 09,35,42 --pace 6000 羚铺
 *
 * ⚠️ 可靠性警告（2026-09 实测，务必先读）
 *   本脚本在 headless 下对阿里云的结果**不稳定**：同一个关键词，交互式浏览器返回
 *   2 条真实记录，本脚本却渲染出"共有 0 个搜索结果"。原因疑似阿里云风控对自动化
 *   客户端返回降级/空结果（页面有 ynuf.aliapp.org 设备指纹上报）。
 *   因此：
 *     - 脚本报出的"0 命中"必须人工复核后才能采信
 *     - 命中数 > 0 的行是自洽的（解析到了精确同名记录），可信度较高
 *     - 正式结论一律以交互式人工检索为准，本脚本只作批量初筛的辅助
 *
 * 防封要点（实测踩过的坑）：
 *   1. 用持久化 profile（.tm-clearance-profile/），让 cookie 与阿里云设备指纹（ynuf）保持稳定
 *      —— 每次全新 session 反而更像机器人
 *   2. 查询间隔 4.5~8 秒随机抖动，每 8 次长休 45 秒；绝不打满并发
 *   3. 命中风控特征词立即熔断该源并长冷却，不重试
 *   4. 结果落盘缓存，重跑不重复请求
 *   5. 只能像真人一样在搜索框里逐字输入再回车；直接改 location.hash 或走深链 URL
 *      不会重新解析查询参数，"精准查询"不生效，拿到的是模糊结果（命中数虚高）
 *
 * 说明：这是初筛，不是法律意见。近似判断（尤其图形商标、跨类近似）必须由商标代理做全类检索。
 */
import { createRequire } from 'node:module';
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND = resolve(__dirname, '../frontend/package.json');
const CACHE_FILE = join(__dirname, 'tm-clearance-cache.json');
const PROFILE_DIR = join(__dirname, '.tm-clearance-profile');

// playwright 装在 frontend 下，从那里解析
const require = createRequire(FRONTEND);
const { chromium } = require('playwright');

// ─────────────────────────── 检索源登记表 ───────────────────────────
// 2026-09 实测：国内大厂云里做商标在线检索的只剩阿里云一家。
// 其余源保留在此处是为了记录结论，避免以后重复踩坑。
const SOURCES = [
  {
    id: 'aliyun',
    label: '阿里云商标',
    enabled: true,
    note: '免登录可用，数据带注册号/申请日/申请人，实测可靠',
  },
  {
    id: 'cnipa',
    label: '中国商标网官方',
    enabled: false,
    note: 'wcjs.sbj.cnipa.gov.cn 已接入 SSO，必须账号登录',
  },
  {
    id: 'quandashi',
    label: '权大师',
    enabled: false,
    note: '搜索结果页强制跳登录',
  },
  {
    id: 'tmkoo',
    label: '标库网',
    enabled: false,
    note: '查询前弹登录框',
  },
  {
    id: 'tencent',
    label: '腾讯云商标',
    enabled: false,
    note: '业务已下线',
  },
  {
    id: 'huawei',
    label: '华为云商标',
    enabled: false,
    note: '2025-12-15 停售',
  },
  {
    id: 'wipo',
    label: 'WIPO 全球品牌数据库',
    enabled: false,
    note: '不含中国国内注册，只有马德里体系；仅可作补充',
  },
];

// 风控/拦截特征词：命中即熔断
const BLOCKED_RE = /安全验证|滑动验证|滑块|人机验证|访问过于频繁|请稍后再试|系统繁忙|操作频繁|robot|verify/i;

const CRITICAL_CLASSES = ['09', '35', '42'];

// ─────────────────────────── 页面侧逻辑 ───────────────────────────
// 在浏览器上下文里执行，必须是自包含函数
const PAGE_STATE = () => ({ url: location.href, text: document.body.innerText, blocked: false });

/** 解析阿里云结果页，抽出精确命中的记录与关键类别占用情况 */
function parseAliyun(name, text, classes) {
  const m = text.match(/共有\s*(\d+)\s*个搜索结果/);
  const total = m ? parseInt(m[1], 10) : 0;
  const lines = text.split('\n').map((s) => s.trim());
  const recs = [];
  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].startsWith(name)) continue;
    const seg = lines.slice(i, i + 8);
    const clsText = (seg.find((l) => /^商标类别：/.test(l)) || '').replace('商标类别：', '');
    const clsNo = (clsText.match(/第(\d+)类/) || [])[1] || '?';
    const status = (seg.find((l) => /^当前状态：/.test(l)) || '').replace('当前状态：', '');
    const regNo = (seg.find((l) => /^注册号：/.test(l)) || '').replace('注册号：', '');
    recs.push({ classNo: clsNo, classText: clsText, status, regNo });
  }

  const occupancy = {};
  for (const c of classes) {
    const hit = recs.filter((r) => r.classNo === c);
    occupancy[c] = hit.map((r) => r.status);
  }
  // 关键类别里有"已注册/待审中/初审公告"就是硬障碍
  const blocked = recs.some(
    (r) => classes.includes(r.classNo) && /已注册|待审中|初审公告|异议|驳回复审/.test(r.status),
  );
  const risk = blocked ? 'high' : total >= 20 ? 'medium-high' : total >= 5 ? 'medium' : 'low';
  return { total, records: recs, occupancy, risk };
}

// ─────────────────────────── 适配器 ───────────────────────────
const SEARCH_INPUT = 'input[placeholder*="检索"]';

// 阿里云这套页面的两个坑（都踩过）：
//   1. 直接改 location.hash 或走深链 URL：页面不会重新解析查询参数，
//      "精准查询"开关不生效，返回的是模糊结果（命中数虚高、结果里根本没有关键词）
//   2. 冷启动时 SPA 还没渲染完就读 innerText：拿到空结果，被误判成 0 命中
// 结论：只能像真人一样在搜索框里逐字输入再回车。
let booted = false;
async function boot(page) {
  if (booted) return;
  await page.goto('https://tm.aliyun.com/channel/search#/search', { waitUntil: 'domcontentloaded' });
  await page.waitForSelector(SEARCH_INPUT, { timeout: 30000 });
  await page.waitForTimeout(1500);
  booted = true;
}

const ADAPTERS = {
  aliyun: {
    async search(page, name, classes) {
      await boot(page);

      const input = page.locator(SEARCH_INPUT).first();
      await input.click();
      await input.fill('');
      await input.type(name, { delay: 90 }); // 逐字输入，贴近真人节奏
      await page.waitForTimeout(300 + Math.random() * 400);
      await page.keyboard.press('Enter');

      const deadline = Date.now() + 25000;
      let ready = false;
      while (Date.now() < deadline) {
        await page.waitForTimeout(400);
        const st = await page.evaluate(() => ({
          has: /共有\s*\d+\s*个搜索结果/.test(document.body.innerText),
          idle: document.querySelectorAll('.next-loading-component').length === 0,
        }));
        if (!st.has || !st.idle) continue;
        // 输入框里还是本次关键词，才说明结果确实属于本次查询
        const val = await input.inputValue().catch(() => '');
        if (val.trim() === name) { ready = true; await page.waitForTimeout(500); break; }
      }
      if (!ready) throw new Error('结果区未在 25s 内渲染，本次结果不可信');

      const state = await page.evaluate(PAGE_STATE);
      if (BLOCKED_RE.test(state.text)) {
        const err = new Error('触发风控');
        err.blocked = true;
        throw err;
      }
      return parseAliyun(name, state.text, classes);
    },
  },
};

// ─────────────────────────── 命令行 ───────────────────────────
function parseArgs(argv) {
  const opts = { names: [], classes: CRITICAL_CLASSES, pace: 4500, jitter: 3500, restEvery: 8, restMs: 45000, refresh: false };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--file') {
      const content = readFileSync(argv[++i], 'utf8');
      opts.names.push(...content.split(/\r?\n/).map((s) => s.trim()).filter((s) => s && !s.startsWith('#')));
    } else if (a === '--classes') {
      opts.classes = argv[++i].split(',').map((s) => s.trim().padStart(2, '0'));
    } else if (a === '--pace') {
      opts.pace = parseInt(argv[++i], 10);
    } else if (a === '--headed') {
      // 阿里云对 headless 疑似返回降级结果，复核时用有头模式
      opts.headed = true;
    } else if (a === '--refresh') {
      opts.refresh = true; // 忽略缓存，强制重查（用于复核可疑结果）
    } else if (a.startsWith('--')) {
      throw new Error('未知参数：' + a);
    } else {
      opts.names.push(a);
    }
  }
  if (!opts.names.length) throw new Error('请给出候选名称，例如：node tools/tm-clearance-check.mjs 羚铺 羚掌柜');
  return opts;
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function loadCache() {
  if (!existsSync(CACHE_FILE)) return {};
  try { return JSON.parse(readFileSync(CACHE_FILE, 'utf8')); } catch { return {}; }
}

const RISK_LABEL = {
  low: '低 · 可试',
  medium: '中 · 需代理复核',
  'medium-high': '中高 · 慎报',
  high: '高 · 不建议',
};

// ─────────────────────────── 主流程 ───────────────────────────
async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const cache = loadCache();
  if (!existsSync(PROFILE_DIR)) mkdirSync(PROFILE_DIR, { recursive: true });

  const enabled = SOURCES.filter((s) => s.enabled);
  console.log(`检索源：${enabled.map((s) => s.label).join(' / ')}`);
  console.log(`关键类别：第 ${opts.classes.join(' / ')} 类   候选数：${opts.names.length}\n`);

  const context = await chromium.launchPersistentContext(PROFILE_DIR, {
    headless: !opts.headed,
    locale: 'zh-CN',
    viewport: { width: 1440, height: 900 },
    // 去掉自动化特征，降低被判定为机器人的概率
    args: ['--disable-blink-features=AutomationControlled'],
  });
  const page = context.pages()[0] || (await context.newPage());
  await page.addInitScript(() => {
    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
  });

  const results = [];
  let sinceRest = 0;
  let sourceIdx = 0;

  for (const name of opts.names) {
    const cached = opts.refresh ? null : cache[name];
    if (cached) {
      console.log(`[缓存] ${formatRow(name, cached)}`);
      results.push({ name, ...cached, fromCache: true });
      continue;
    }

    const src = enabled[sourceIdx % enabled.length];
    const adapter = ADAPTERS[src.id];
    let row;
    try {
      const r = await adapter.search(page, name, opts.classes);
      row = { ...r, source: src.id, checkedAt: new Date().toISOString() };
      cache[name] = row;
      writeFileSync(CACHE_FILE, JSON.stringify(cache, null, 2), 'utf8');
      console.log(formatRow(name, r));
    } catch (err) {
      if (err.blocked) {
        console.error(`\n[熔断] ${src.label} 触发风控，冷却 5 分钟后改用下一个源。`);
        await sleep(5 * 60 * 1000);
        sourceIdx++;
        row = { error: 'blocked', source: src.id };
      } else {
        console.error(`[失败] ${name}：${err.message}`);
        row = { error: err.message, source: src.id };
      }
      results.push({ name, ...row });
      continue;
    }

    results.push({ name, ...row });
    sinceRest++;
    const wait = opts.pace + Math.random() * opts.jitter;
    if (sinceRest >= opts.restEvery) {
      console.log(`  …已查 ${sinceRest} 个，长休 ${opts.restMs / 1000}s`);
      await sleep(opts.restMs);
      sinceRest = 0;
    } else {
      await sleep(wait);
    }
  }

  await context.close();

  const outFile = join(__dirname, 'tm-clearance-result.json');
  writeFileSync(outFile, JSON.stringify(results, null, 2), 'utf8');
  console.log(`\n明细已写入 ${outFile}`);
}

function formatRow(name, r) {
  if (r.error) return `${name} | 失败(${r.error})`;
  const occ = Object.entries(r.occupancy || {})
    .map(([c, v]) => `第${c}类=${v.length ? v.join('/') : '空'}`)
    .join('  ');
  return `${name} | 精确命中 ${r.total} | ${occ} | ${RISK_LABEL[r.risk] || r.risk}`;
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
