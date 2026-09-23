/*
 * 财务总览工作台「快速入口」跳转验证（2026-09-23 财务审计用）
 *
 * 背景：views/finance/index.vue:309-319 的 navigateTo() 用一张硬编码路径表跳转，
 *       其中多数目标不在 sys_menu 里。本仓已有教训——未注册的路径会被 Layout 下的
 *       catch-all `/:pathMatch(.*)*` 承接，**URL 照样变化、只是渲染 404 页**，
 *       所以「URL 变了」不能作为「路由存在」的证据。
 *       本脚本用四重判定：① 最终 URL；② `.not-found` 元素；③ 正文 404 文案；④ 页面主内容是否渲染。
 *
 * 用法：node tools/verify-finance-dashboard-links.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = 'http://localhost:5656'
const OUT = 'I:/AI-Ready/tool-results/finance-audit'

// [卡片标题, 期望目标路径]
// 2026-09-23 修复：原先 4 条指向 `/erp/finance/**`（不存在的路径），已改为与 sys_menu.path 一致。
const CARDS = [
  ['科目管理', '/md/accounting-subject'],
  ['凭证管理', '/finance/voucher/index'],
  ['应收账款', '/finance/receivable'],
  ['应付账款', '/finance/payable'],
  ['财务报表', '/finance/balance-report'],
  ['对账管理', '/finance/reconciliation'],
]

function apiReq(method, path, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: 5655, path: '/api' + path, method,
      headers: { 'Content-Type': 'application/json', ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}) },
    }, res => { let s = ''; res.on('data', c => { s += c }); res.on('end', () => { try { resolve(JSON.parse(s)) } catch (e) { resolve({ raw: s }) } }) })
    r.on('error', reject); if (data) r.write(data); r.end()
  })
}

async function judge(page) {
  return await page.evaluate(() => {
    const text = document.body.innerText || ''
    const notFoundEl = !!document.querySelector('.not-found, [class*=not-found]')
    const notFoundText = /页面不存在|找不到页面|404|Not Found|页面走丢了/i.test(text.slice(0, 2000))
    // 页面主内容：真实业务页会有表格/表单/卡片容器。
    // ⚠️ 不能只认固定的容器类名 —— 各页根类名不同（如会计科目页是 `md-subject-page`），
    //    只按 class 判定会把「已正常渲染但类名不匹配」的页误报为空白。
    //    这里改为「有表格行 或 有表单 或 正文足够长」三选一。
    const hasContent = document.querySelectorAll('tbody tr, form, .ant-table, .ant-form').length > 0 || text.length > 500
    return { url: location.pathname + location.search, notFoundEl, notFoundText, hasContent, head: text.slice(0, 120).replace(/\s+/g, ' ') }
  })
}

;(async () => {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const lg = await apiReq('POST', '/auth/login', {
    username: 'admin', password: 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = lg.data.token

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 950 } })
  await ctx.addInitScript(t => {
    localStorage.setItem('token', t); localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户')
  }, token)
  const page = await ctx.newPage()

  await page.goto(FE + '/finance/index', { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForTimeout(3500)

  const rows = []
  for (const [label, target] of CARDS) {
    // 每次都从工作台出发
    if (!page.url().endsWith('/finance/index')) {
      await page.goto(FE + '/finance/index', { waitUntil: 'domcontentloaded', timeout: 60000 })
      await page.waitForTimeout(2500)
    }
    let clicked = true
    try {
      await page.locator(`.quick-link-card:has-text("${label}")`).first().click({ timeout: 8000 })
    } catch (e) { clicked = false }
    await page.waitForTimeout(3000)
    const j = await judge(page)
    const is404 = j.notFoundEl || j.notFoundText
    // 三重判定：① 是否渲染 404；② 落地 URL 是否等于期望；③ 主内容是否渲染
    const urlOk = j.url === target
    const verdict = !clicked ? '未找到卡片'
      : is404 ? '**404**'
      : !urlOk ? '**路径不符**'
      : j.hasContent ? 'OK' : '空白?'
    rows.push({ label, target, clicked, urlOk, ...j, verdict })
    console.log(`${(verdict === 'OK' ? 'OK' : verdict).padEnd(10)} ${label.padEnd(8)} 期望=${target.padEnd(28)} 实际=${j.url.padEnd(28)} content=${j.hasContent}`)
  }

  const lines = ['# 财务总览工作台「快速入口」跳转实跑结果', '',
    `- 时间：${new Date().toISOString()}`,
    `- 判定四重：最终 URL · \`.not-found\` 元素 · 正文 404 文案 · 主内容是否渲染`,
    `- 结论：**${rows.filter(r => r.verdict === '**404**').length} / ${rows.length} 个入口点了会落到 404**`, '',
    '| 卡片 | 代码写死的目标 | 实际落地 URL | 判定 | 判定依据 |', '|---|---|---|---|---|']
  for (const r of rows) {
    const why = r.verdict === '**404**' ? [r.notFoundEl ? '`.not-found` 元素命中' : '', r.notFoundText ? '正文含 404 文案' : ''].filter(Boolean).join(' + ') : (r.verdict === 'OK' ? '主内容已渲染' : r.verdict)
    lines.push(`| ${r.label} | \`${r.target}\` | \`${r.url}\` | ${r.verdict} | ${why} |`)
  }
  lines.push('', '## 页面首屏文案（判定佐证）', '')
  for (const r of rows) lines.push(`- **${r.label}** → \`${r.url}\`：${r.head}`)
  fs.writeFileSync(OUT + '/dashboard-links-result.md', lines.join('\n'))
  fs.writeFileSync(OUT + '/dashboard-links-result.json', JSON.stringify(rows, null, 2))

  console.log('\n===== 汇总 =====')
  console.log(rows.map(r => `${r.label}:${r.verdict}`).join(' | '))
  await browser.close()
})().catch(e => { console.error('FAILED:', e.message); process.exit(1) })
