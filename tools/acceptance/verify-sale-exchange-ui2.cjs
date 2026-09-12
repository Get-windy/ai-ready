// 销售换货单 UI 复验（2026-09-10 二轮：头部字段区 2 行折叠/展开 · 页面配置「显示区域」分组 · 列表搜索区动作区置尾）
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')

const BASE = process.env.SEX_UI_BASE || 'http://localhost:5656'
const API = process.env.SEX_API || 'http://localhost:5655'

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}

function extractCaptcha(img) {
  const m = (img || '').match(/base64,([A-Za-z0-9+/=]+)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}

async function apiLogin() {
  let last = null
  for (let i = 1; i <= 4; i++) {
    try {
      const cap = await fetch(`${API}/api/auth/captcha`).then(r => r.json())
      const cd = cap.data || cap
      const lg = await fetch(`${API}/api/auth/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'sex_e2e', password: 'admin123', tenantName: '系统租户', captcha: extractCaptcha(cd.img), captchaKey: cd.uuid }),
      }).then(r => r.json())
      last = lg
      const d = lg?.data || {}
      const token = d.token || d.accessToken || d.tokenValue
      if (token) return { token, tenantId: d.tenantId, userId: d.userId }
    } catch (e) { last = String(e) }
    await new Promise(r => setTimeout(r, 1500))
  }
  console.log('  登录响应:', JSON.stringify(last).slice(0, 200))
  return {}
}

async function main() {
  const { token, tenantId, userId } = await apiLogin()
  if (!token) { console.log('登录失败'); process.exit(1) }

  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1760, height: 1000 } })
  const errors = []
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()) })
  page.on('pageerror', e => errors.push(String(e)))

  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t, ti, uid]) => {
    localStorage.setItem('token', t); localStorage.setItem('tenantId', String(ti || 1)); localStorage.setItem('userId', String(uid))
  }, [token, tenantId, userId])

  async function open(url, waitSel) {
    for (let i = 1; i <= 5; i++) {
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(3500)
      const onLogin = await page.evaluate(() => !!document.querySelector('input[type="password"]')).catch(() => true)
      if (!onLogin) { await page.waitForSelector(waitSel, { timeout: 20000 }).catch(() => {}); await page.waitForTimeout(2500); return true }
      const r = await apiLogin()
      await page.evaluate(([t, ti, uid]) => {
        localStorage.setItem('token', t); localStorage.setItem('tenantId', String(ti || 1)); localStorage.setItem('userId', String(uid))
      }, [r.token, r.tenantId, r.userId])
    }
    return false
  }

  // ══════════ 表单页：头部字段区 默认 2 行 + 展开/收起 ══════════
  console.log('\n── 表单页：头部字段区折叠 ──')
  const okForm = await open(`${BASE}/sales/exchange/form`, '.bill-basic-info')
  check('表单页可打开', okForm)

  const collapse = await page.evaluate(() => {
    const flow = document.querySelector('.bill-basic-info .info-flow')
    const toggle = document.querySelector('.bill-basic-info .info-flow-toggle')
    if (!flow) return null
    const items = [...flow.children]
    const tops = [...new Set(items.map(el => el.offsetTop))].sort((a, b) => a - b)
    const s = getComputedStyle(flow)
    return {
      hasToggle: !!toggle,
      toggleTitle: toggle?.getAttribute('title') || '',
      totalRows: tops.length,
      maxHeight: s.maxHeight,
      overflow: s.overflow,
      visibleHeight: flow.clientHeight,
      scrollHeight: flow.scrollHeight,
    }
  })
  check('存在展开/收起按钮', !!collapse?.hasToggle, `title=${collapse?.toggleTitle}`)
  check('默认只显示 2 行（超出部分裁剪）', !!collapse && collapse.totalRows > 2 && collapse.overflow === 'hidden' && collapse.visibleHeight < collapse.scrollHeight,
    collapse ? `总行数=${collapse.totalRows} 可见高=${collapse.visibleHeight} 全高=${collapse.scrollHeight} maxHeight=${collapse.maxHeight}` : '')

  // 点击展开
  await page.evaluate(() => document.querySelector('.bill-basic-info .info-flow-toggle')?.click())
  await page.waitForTimeout(600)
  const expanded = await page.evaluate(() => {
    const flow = document.querySelector('.bill-basic-info .info-flow')
    const items = [...flow.children]
    const tops = [...new Set(items.map(el => el.offsetTop))].sort((a, b) => a - b)
    const s = getComputedStyle(flow)
    return { overflow: s.overflow, visibleHeight: flow.clientHeight, scrollHeight: flow.scrollHeight, totalRows: tops.length }
  })
  check('点击展开后完整显示全部字段行', expanded.scrollHeight <= expanded.visibleHeight + 2,
    `可见高=${expanded.visibleHeight} 全高=${expanded.scrollHeight} 总行数=${expanded.totalRows}`)

  // 收起
  await page.evaluate(() => document.querySelector('.bill-basic-info .info-flow-toggle')?.click())
  await page.waitForTimeout(600)
  const collapsed = await page.evaluate(() => {
    const flow = document.querySelector('.bill-basic-info .info-flow')
    return { visibleHeight: flow.clientHeight, scrollHeight: flow.scrollHeight }
  })
  check('点击收起后恢复只显示 2 行', collapsed.visibleHeight < collapsed.scrollHeight,
    `可见高=${collapsed.visibleHeight} 全高=${collapsed.scrollHeight}`)

  // ══════════ 表单页：页面配置「显示区域」分组 ══════════
  console.log('\n── 表单页：页面配置「显示区域」 ──')
  await page.evaluate(() => {
    const b = [...document.querySelectorAll('button')].find(x => x.innerText.trim().includes('配置'))
    if (b) b.click()
  })
  await page.waitForTimeout(1500)
  const cfg = await page.evaluate(() => {
    const m = document.querySelector('.ant-modal-content')
    if (!m) return null
    const cols = [...m.querySelectorAll('.ant-table-thead th')].map(t => t.innerText.trim())
    const rows = [...m.querySelectorAll('.ant-table-tbody tr')]
      .filter(r => !r.classList.contains('ant-table-measure-row'))
      .map(r => {
        const tds = [...r.querySelectorAll('td')].map(td => td.innerText.trim())
        return { no: tds[0], area: tds[1], name: tds[2] }
      })
    return { cols, rows }
  })
  check('配置表含「显示区域」列', !!cfg && cfg.cols.includes('显示区域'), cfg ? JSON.stringify(cfg.cols) : '')
  check('制单人/制单时间/打印次数/本单金额 归入「单据信息」',
    !!cfg && ['制单人', '制单时间', '打印次数', '本单金额'].every(n => {
      const r = cfg.rows.find(x => x.name === n)
      return r && r.area === '单据信息'
    }),
    cfg ? JSON.stringify(cfg.rows.filter(r => ['制单人', '制单时间', '打印次数', '本单金额'].includes(r.name))) : '')
  check('收款账户等归入「收款Tab」、客户等归入「顶部基本信息」',
    !!cfg && cfg.rows.find(x => x.name === '收款账户')?.area === '收款Tab'
    && cfg.rows.find(x => x.name === '客户')?.area === '顶部基本信息'
    && cfg.rows.find(x => x.name === '源单')?.area === '源单Tab'
    && cfg.rows.find(x => x.name === '单据备注')?.area === '备注区')
  check('全部 35 个字段均标注显示区域', !!cfg && cfg.rows.length === 35 && cfg.rows.every(r => !!r.area),
    cfg ? `未标注=${cfg.rows.filter(r => !r.area).map(r => r.name).join(',') || '无'}` : '')
  await page.evaluate(() => { document.querySelectorAll('.ant-modal-close').forEach(b => b.click()) })
  await page.waitForTimeout(500)

  // ══════════ 列表页：搜索区动作区置尾 ══════════
  console.log('\n── 列表页：搜索区动作区位置 ──')
  const okList = await open(`${BASE}/sales/exchange/index`, '.search-grid')
  check('列表页可打开', okList)

  const gridOrder = await page.evaluate(() => {
    const grid = document.querySelector('.search-grid')
    const cells = [...grid.children]
    const idxAction = cells.findIndex(c => c.classList.contains('search-action-group'))
    const cellsAfter = cells.slice(idxAction + 1).length
    const actionText = cells[idxAction]?.innerText.replace(/\n/g, ' ') || ''
    return { total: cells.length, idxAction, cellsAfter, actionText, isLast: idxAction === cells.length - 1 }
  })
  check('「查询/重置」动作区位于搜索区末尾', gridOrder.isLast,
    `共${gridOrder.total}格，动作区在第${gridOrder.idxAction + 1}格，其后还有${gridOrder.cellsAfter}格`)
  const actionFlat = String(gridOrder.actionText || '').replace(/\s+/g, '')
  check('「显示红冲」勾选框与查询/重置同处动作区（末尾）',
    actionFlat.includes('查询') && actionFlat.includes('重置') && actionFlat.includes('显示红冲'),
    actionFlat)

  const lastField = await page.evaluate(() => {
    const grid = document.querySelector('.search-grid')
    const cells = [...grid.children]
    const idxAction = cells.findIndex(c => c.classList.contains('search-action-group'))
    const before = cells.slice(0, idxAction)
    const lastInput = before[before.length - 1]
    return lastInput?.querySelector('input')?.getAttribute('placeholder')
      || lastInput?.querySelector('.search-select-label')?.innerText
      || lastInput?.innerText?.trim()
  })
  check('动作区前的最后一个条件是查询条件（非「部门」被挤到末尾）', !!lastField, `末位条件=${lastField}`)

  check('页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 3).join(' | '))

  await page.screenshot({ path: 'I:/AI-Ready/exchange-list-final.png' })
  await page.goto(`${BASE}/sales/exchange/form`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4000)
  await page.screenshot({ path: 'I:/AI-Ready/exchange-form-final.png' })
  await browser.close()

  console.log('\n═══ 二轮 UI 复验结果 ═══')
  console.log(`  ✅ 通过 ${pass} 项   ❌ 失败 ${fail} 项\n`)
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('UI 复验异常:', e); process.exit(2) })
