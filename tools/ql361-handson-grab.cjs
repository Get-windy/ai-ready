/* 开一个有界面的 ql361 浏览器，**由用户手动操作**，脚本在旁边持续记录全部网络响应。
 *
 * 用途：打印模板的取法需要人工点击（列表 → 单据编号格 → 打印 → 模板设置），
 *       脚本不再猜 DOM，只负责把模板相关的 API 响应完整落盘。
 *
 * ⚠️ 只读：脚本自己不做任何填写/保存；用户手动操作时也**不要改动对标数据**。
 *
 * 运行（建议后台）： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-handson-grab.cjs [保持分钟数]
 * 产出： tool-results/ql361/print-template/handson/_api.json（每 10 秒落盘一次，可随时查看）
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const HOLD_MIN = Number(process.argv[2] || 10)
const OUT = path.resolve(__dirname, '../tool-results/ql361/print-template/handson')
fs.mkdirSync(OUT, { recursive: true })
const API_FILE = path.join(OUT, '_api.json')
const log = (...a) => console.log('[handson]', ...a)

const api = []
function wire(p) {
  p.on('response', async (res) => {
    const u = res.url()
    if (/\.(js|css|png|jpg|jpeg|gif|svg|woff2?|ico)($|\?)/i.test(u)) return
    let body = ''
    try { body = (await res.text()).slice(0, 900000) } catch { /* ignore */ }
    api.push({ t: new Date().toISOString(), url: u, status: res.status(), body })
    // 模板相关立刻单独落一份，方便随时看
    if (/print|template|format|billinfo/i.test(u)) {
      try {
        fs.writeFileSync(path.join(OUT, `hit-${api.length}.json`),
          JSON.stringify({ url: u, status: res.status(), body }, null, 2), 'utf8')
      } catch { /* ignore */ }
    }
  })
}

;(async () => {
  const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({
    viewport: { width: 1920, height: 1000 },
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  })
  const page = await ctx.newPage()
  wire(page)
  ctx.on('page', (p) => { log('↗ 用户新开了标签页:', p.url().slice(0, 120)); wire(p) })

  await page.goto('https://22stable.ql361.com/desktop.html', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(4000)
  log('浏览器已打开，工作台 URL:', page.url())
  log('★ 请在这个窗口里手动操作：销售 → 销售出库单「历史」→ 点单据编号格 → 打印(F8) → 模板设置')
  log(`★ 脚本会持续记录，保持 ${HOLD_MIN} 分钟；产物目录：${OUT}`)

  const steps = Math.ceil((HOLD_MIN * 60 * 1000) / 10000)
  for (let i = 0; i < steps; i++) {
    await page.waitForTimeout(10000)
    try { fs.writeFileSync(API_FILE, JSON.stringify(api, null, 2), 'utf8') } catch { /* ignore */ }
    if (i % 6 === 0) log(`… 已记录 ${api.length} 条响应（${(i * 10 / 60).toFixed(1)} 分钟）`)
  }
  try { fs.writeFileSync(API_FILE, JSON.stringify(api, null, 2), 'utf8') } catch { /* ignore */ }
  log('记录结束，共', api.length, '条 →', API_FILE)
  await browser.close()
})().catch(e => { console.error(e); process.exit(1) })
