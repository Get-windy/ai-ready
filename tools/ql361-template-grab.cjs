/* 抓取 ql361 打印设计器页面上的模板定义（只读）
 *
 * 用户直接给出了设计器 URL（销售出库单）：
 *   https://22stable.ql361.com/v_22stable/vue/web.print/dist/index.html?
 *     project=ql&v=v=20260915220834&billtype=601&billid=1902974114&templateid=50251&sumfields=...
 * 参数含义：billtype=单据类型(601=销售出库单) · billid=样例单据 · templateid=模板ID ·
 *          sumfields=可用于打印的字段清单（逗号分隔）
 *
 * ⚠️ 只读：打开页面 / 截图 / 记录网络响应，**不做任何保存操作**。
 *
 * 用法： NODE_PATH=I:/AI-Ready/frontend/node_modules node tools/ql361-template-grab.cjs "<设计器URL>" [输出名]
 */
const { chromium } = require('playwright')
const fs = require('fs')
const path = require('path')

const URL_ = process.argv[2]
const NAME = process.argv[3] || 'sale-outbound'
if (!URL_) { console.error('用法: node tools/ql361-template-grab.cjs "<设计器URL>" [输出名]'); process.exit(1) }

const OUT = path.resolve(__dirname, `../tool-results/ql361/print-template/designer/${NAME}`)
fs.mkdirSync(OUT, { recursive: true })
const log = (...a) => console.log('[grab]', ...a)

;(async () => {
  const STATE = path.resolve(__dirname, '../tool-results/ql361/product/ql361-state.json')
  const browser = await chromium.launch({ headless: false, args: ['--window-size=1920,1000'] })
  const ctx = await browser.newContext({
    viewport: { width: 1920, height: 1000 },
    storageState: fs.existsSync(STATE) ? STATE : undefined,
  })
  const page = await ctx.newPage()

  const api = []
  page.on('response', async (res) => {
    const u = res.url()
    if (/\.(js|css|png|jpg|jpeg|svg|woff2?|ico)($|\?)/i.test(u)) return
    let body = ''
    try { body = (await res.text()).slice(0, 800000) } catch { /* ignore */ }
    api.push({ url: u, status: res.status(), body })
  })

  await page.goto(URL_, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(12000)   // 设计器是 Vue SPA，给它时间拉模板
  await page.screenshot({ path: path.join(OUT, '1-设计器.png') }).catch(() => {})
  // 再等一下（有的模板加载更慢），补一张
  await page.waitForTimeout(6000)
  await page.screenshot({ path: path.join(OUT, '2-设计器.png') }).catch(() => {})

  const txt = await page.evaluate(() => (document.body.innerText || '').slice(0, 6000)).catch(() => '')
  fs.writeFileSync(path.join(OUT, '_页面文本.txt'), txt, 'utf8')
  fs.writeFileSync(path.join(OUT, '_api.json'), JSON.stringify(api, null, 2), 'utf8')

  log('页面 URL:', page.url())
  log('API 响应', api.length, '条')
  // 立刻把"像模板定义"的响应单独摘出来，省得后面再翻大文件
  const hits = api.filter(r => /template|design|print|json|model|layout|element/i.test(r.url) && r.body && r.body.length > 200)
  for (const h of hits.slice(0, 15)) {
    log(`  · ${h.status} [${h.body.length}B] ${h.url.slice(0, 150)}`)
  }
  fs.writeFileSync(path.join(OUT, '_candidates.json'), JSON.stringify(hits, null, 2), 'utf8')
  log('完成 →', OUT)
  await browser.close()
})().catch(e => { console.error(e); process.exit(1) })
