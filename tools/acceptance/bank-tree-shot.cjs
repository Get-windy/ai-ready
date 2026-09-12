// 树形层次结构渲染取证：造父子账户 → 截图 → 清理
const path = require('path')
const fs = require('fs')
const os = require('os')
const { execFileSync } = require('child_process')
const { chromium } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/playwright'))
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const APP = 'http://localhost:5657'
const API = 'http://localhost:5801'
const TMP = os.tmpdir()
const sleep = ms => new Promise(r => setTimeout(r, ms))
const curl = a => execFileSync('curl', ['-s', '--max-time', '30', ...a], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
function captchaText(imgSrc) {
  const m = (imgSrc || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}
;(async () => {
  let TOKEN = ''
  for (let i = 0; i < 10 && !TOKEN; i++) {
    const body = curl([`${API}/api/auth/captcha`])
    const cap = JSON.parse(body).data || {}
    const f = path.join(TMP, `tree-${Date.now()}.json`)
    fs.writeFileSync(f, JSON.stringify({ username: 'sex_e2e', password: 'admin123', tenantName: '系统租户', captcha: captchaText(cap.img), captchaKey: cap.uuid }), 'utf8')
    const r = curl(['-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8', '-d', `@${f}`, `${API}/api/auth/login`])
    try { fs.unlinkSync(f) } catch {}
    TOKEN = (JSON.parse(r).data || {}).token || ''
    if (!TOKEN) await sleep(4000)
  }
  if (!TOKEN) throw new Error('登录失败')
  const send = (method, p, body) => {
    const args = ['-X', method, '-H', `Authorization: Bearer ${TOKEN}`, '-H', 'Content-Type: application/json; charset=utf-8']
    let f = null
    if (body !== undefined) { f = path.join(TMP, `tree-b-${Date.now()}.json`); fs.writeFileSync(f, JSON.stringify(body), 'utf8'); args.push('-d', `@${f}`) }
    args.push(`${API}${p}`)
    try { return JSON.parse(curl(args) || '{}') } finally { if (f) { try { fs.unlinkSync(f) } catch {} } }
  }
  const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await pg.connect()
  await pg.query(`DELETE FROM finance_account WHERE account_name LIKE '树形取证%'`)

  const stamp = String(Date.now()).slice(-6)
  const parentCode = `6${stamp}`
  const p = send('POST', '/api/erp/finance/bank-account', { subjectCode: parentCode, accountName: `树形取证-总行-${stamp}`, accountType: 1, bankName: '测试银行' })
  const pid = p.data?.id
  const c1 = send('POST', '/api/erp/finance/bank-account', { subjectCode: `${parentCode}01`, accountName: `树形取证-分行A-${stamp}`, accountType: 1, parentId: Number(pid) })
  const c2 = send('POST', '/api/erp/finance/bank-account', { subjectCode: `${parentCode}02`, accountName: `树形取证-分行B-${stamp}`, accountType: 1, parentId: Number(pid) })
  console.log('created', pid, c1.data?.id, c2.data?.id)
  // 子账户下再挂一级，验证三级缩进
  const c3 = send('POST', '/api/erp/finance/bank-account', { subjectCode: `${parentCode}0101`, accountName: `树形取证-支行A1-${stamp}`, accountType: 2, parentId: Number(c1.data?.id) })
  console.log('created3', c3.data?.id)

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1500, height: 800 } })
  const page = await ctx.newPage()
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    if (new URL(route.request().url()).pathname.startsWith('/api/sse')) return route.continue()
    try { const resp = await route.fetch({ url: route.request().url().replace(APP, API) }); await route.fulfill({ response: resp }) } catch { await route.abort() }
  })
  await page.addInitScript(t => { localStorage.setItem('token', t); localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户') }, TOKEN)
  await page.goto(`${APP}/md/bank-account`, { waitUntil: 'domcontentloaded' })
  await page.waitForSelector('.ss-grid tbody tr.ss-row', { timeout: 120000 })
  await page.waitForTimeout(2500)
  const rows = await page.locator('.ss-grid tbody tr.ss-row').evaluateAll(trs => trs
    .filter(tr => (tr.innerText || '').includes('树形取证'))
    .map(tr => {
      const nameCell = tr.querySelector('.name-cell')
      const treeCell = tr.querySelector('.tree-cell')
      return {
        text: (tr.innerText || '').replace(/\s+/g, ' ').trim(),
        indent: nameCell ? getComputedStyle(nameCell).paddingLeft : null,
        hasFolder: !!treeCell?.querySelector('.tree-icon.folder'),
        caret: treeCell?.querySelector('.tree-toggle')?.textContent || '',
      }
    }))
  console.log(JSON.stringify(rows, null, 1))
  await page.screenshot({ path: path.join(__dirname, '../../tool-results', 'bank-account-tree.png') })

  // 收起父节点 → 子孙行隐藏
  await page.locator('.ss-grid tbody tr.ss-row', { hasText: parentCode }).first().locator('.tree-toggle').first().click()
  await page.waitForTimeout(1500)
  const afterCollapse = await page.locator('.ss-grid tbody tr.ss-row').evaluateAll(trs => trs.filter(tr => (tr.innerText || '').includes('树形取证')).length)
  console.log('收起后可见行数:', afterCollapse)
  await page.screenshot({ path: path.join(__dirname, '../../tool-results', 'bank-account-tree-collapsed.png') })

  await browser.close()
  await pg.query(`DELETE FROM finance_account WHERE account_name LIKE '树形取证%'`)
  await pg.end()
  console.log('DONE')
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1) })
