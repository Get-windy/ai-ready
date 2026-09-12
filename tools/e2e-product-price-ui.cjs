/* 商品价格管理（资料 → 商品管理 → 商品价格管理）UI 端到端验证
 *
 * 前端：FE_URL（默认 http://localhost:5656）
 * 后端：API_PORT（默认 5720，本次验证实例；前端 /api/** 由 playwright route 转发）
 * 运行： node tools/e2e-product-price-ui.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const API_PORT = Number(process.env.API_PORT || 5720)
const API_BASE = process.env.API_URL || `http://localhost:${API_PORT}`
const SHOTS = 'I:/AI-Ready/tool-results/product-price-ui'
const TENANT = 1

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

let TOKEN = null
let PASS = 0
let FAIL = 0
const FAILURES = []

function rawReq(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: API_PORT, path: '/api' + path, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8')
        try { resolve(JSON.parse(text)) } catch { resolve({ raw: text.slice(0, 200), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 200))
  TOKEN = token
}

const flat = (s) => String(s).replace(/\s+/g, '')

function check(name, cond, detail) {
  if (cond) { PASS++; console.log('  ✓', name) }
  else { const d = detail === undefined ? '' : (typeof detail === 'string' ? detail : JSON.stringify(detail)); FAIL++; FAILURES.push(name + (d ? ' → ' + d.slice(0, 200) : '')); console.log('  ✗', name, d.slice(0, 200)) }
}

async function main() {
  await login()
  console.log('登录成功\n=== UI 验收 ===')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const p = await ctx.newPage()
  const errors = []
  p.on('pageerror', e => errors.push(String(e)))
  const priceReqs = []
  p.on('request', r => { if (r.url().includes('/product-price/batch-modify')) priceReqs.push(r.postData() || '') })

  /**
   * 代理 /api/** 到验证实例，并在 401（sa-token 被并行会话顶下线）时自动重登 + 重试一次：
   * 否则前端一旦拿到 401 就会跳登录页，后续断言全部落空（伪失败）。
   */
  await p.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    const target = API_BASE + u.pathname + u.search
    // SSE 等长连接请求不能走 fetch（不会结束会超时），直接转发
    const accept = route.request().headers()['accept'] || ''
    if (accept.includes('text/event-stream')) {
      return route.continue({ url: target })
    }
    const reqOpts = { url: target, method: route.request().method() }
    let resp = await route.fetch(reqOpts)
    if (resp.status() === 401) {
      await login()
      await p.evaluate((tk) => localStorage.setItem('token', tk), TOKEN).catch(() => {})
      const headers = { ...route.request().headers(), authorization: 'Bearer ' + TOKEN }
      resp = await route.fetch({ ...reqOpts, headers })
    }
    await route.fulfill({ response: resp })
  })

  await p.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })

  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 4; attempt++) {
      await login()
      await p.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, TENANT])
      await p.goto(url, { waitUntil: 'domcontentloaded' })
      await p.waitForTimeout(waitMs)
      if (!new URL(p.url()).pathname.includes('/login')) return
      console.log('  [被踢回登录页，重试注入 token]')
      await p.waitForTimeout(1500)
    }
    throw new Error('openPage 多次重试后仍未进入业务页: ' + url)
  }

  /** 自愈：被并行会话踢回登录页时重新注入 token 并回到目标页 */
  async function ensureLogged() {
    const path = new URL(p.url()).pathname
    const onLogin = path.includes('/login')
    if (!onLogin) return true
    console.log('  [检测到登录页，重新注入 token]')
    await login()
    await p.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [TOKEN, TENANT])
    await p.goto(`${FE}/md/product-price`, { waitUntil: 'domcontentloaded' })
    await p.waitForTimeout(5000)
    return !new URL(p.url()).pathname.includes('/login')
  }

  /** 确保停留在指定子标签（被踢出重建后需重新切换） */
  async function ensureTab(label) {
    await ensureLogged()
    const active = await p.evaluate(() => {
      const el = document.querySelector('.tab-item.active')
      return el ? (el.innerText || '').trim() : ''
    })
    if (active !== label) {
      await clickText(label)
      await p.waitForTimeout(3000)
    }
  }

  /** 点击可见文本（精确） */
  async function clickText(text, exact = true) {
    await ensureLogged()
    const ok = await p.evaluate(({ t, ex }) => {
      const els = [...document.querySelectorAll('button,a,span,div,li')]
        .filter(e => e.offsetParent !== null && e.children.length === 0)
        .filter(e => { const s = (e.innerText || '').trim(); return ex ? s === t : s.includes(t) })
      if (!els.length) return false
      els[els.length - 1].click()
      return true
    }, { t: text, ex: exact })
    await p.waitForTimeout(1200)
    return ok
  }

  const bodyText = () => p.evaluate(() => document.body.innerText)

  /** 点击最上层弹窗内的按钮（避免命中页面其他同名元素） */
  async function clickModalButton(label) {
    const ok = await p.evaluate((t) => {
      const btns = [...document.querySelectorAll('.ant-modal button, .ant-modal-root button')]
        .filter(b => b.offsetParent !== null && (b.innerText || '').trim() === t)
      if (!btns.length) return false
      btns[btns.length - 1].click()
      return true
    }, label)
    await p.waitForTimeout(1200)
    return ok
  }

  // ═══ A. 页面骨架 ═══
  await openPage(`${FE}/md/product-price`)
  await p.screenshot({ path: `${SHOTS}/ui-01-batch.png` })
  const t0 = flat(await bodyText())
  for (const label of ['商品价格批量修改', '客户级别折扣设置', '级别指定价设置', '客户指定价设置']) {
    check(`A1 含子标签「${label}」`, t0.includes(flat(label)))
  }
  check('A2 左侧分类树标题「商品分类」', t0.includes('商品分类'))
  check('A3 路径栏「当前路径:全部商品」', t0.includes('当前路径'))
  for (const label of ['刷新', '打印(F8)', '批量修改', '更多']) {
    check(`A4 工具栏含「${label}」`, t0.includes(flat(label)))
  }
  {
    // 对标：子标签 1 的「导出」收在「更多」下拉中
    await p.locator('button:visible', { hasText: '更多' }).first().click({ timeout: 8000 }).catch(() => {})
    await p.waitForTimeout(900)
    const t = flat(await bodyText())
    check('A4b「更多」下拉含「导出」', t.includes('导出'))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(600)
  }
  for (const label of ['筛选条件', '品牌', '单位类型', '商品', '上架状态', '最近进货日期', '库存数量', '查询', '显示层次结构']) {
    check(`A5 查询区含「${label}」`, t0.includes(flat(label)))
  }

  const headers = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').trim().replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['图片', '商品名称', '单位', '品牌', '换算关系', '最近进价', '账面库存', '成本均价', '批发价', '最低折扣(%)', '最低售价', '零售价', '规格']) {
    check(`A6 表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/').slice(0, 120))
  }
  const rowCount = await p.evaluate(() => document.querySelectorAll('.ss-grid tbody tr').length)
  check('A7 表格有数据行', rowCount > 0, rowCount)

  // ═══ B. 列配置齿轮 ═══
  {
    const gear = await p.$('.ss-grid th .th-settings-btn')
    check('B1 表头存在列配置齿轮', !!gear)
    if (gear) {
      await gear.click()
      await p.waitForTimeout(1200)
      await p.screenshot({ path: `${SHOTS}/ui-02-column-config.png` })
      const dlg = flat(await bodyText())
      check('B2 列配置含个人配置/全局配置/恢复默认', dlg.includes('个人配置') && dlg.includes('全局配置') && dlg.includes('恢复默认'))
      check('B3 列配置含隐藏列「上架/商品货号/条码/产地」', ['上架', '商品货号', '条码', '产地'].every(x => dlg.includes(x)))
      await p.keyboard.press('Escape')
      await p.waitForTimeout(600)
    }
  }

  // ═══ C. 批量修改（勾选 → 弹窗 → 保存 → 生效） ═══
  {
    const beforeRetail = await p.evaluate(() => {
      const cell = document.querySelector('.ss-grid tbody tr .field-retailprice, .ss-grid tbody tr td:nth-child(18)')
      return cell ? (cell.innerText || '').trim() : ''
    })
    // 数据表复选框为自绘原生 input.ss-checkbox（非 antd）
    await p.locator('.ss-grid tbody tr td.ss-cell-checkbox input.ss-checkbox').first()
      .click({ timeout: 8000 }).catch(e => console.log('  [勾选失败]', e.message.slice(0, 80)))
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-03-row-selected.png` })
    const btnDisabled = await p.evaluate(() => {
      const btn = [...document.querySelectorAll('button')].find(b => (b.innerText || '').trim() === '批量修改')
      return btn ? btn.disabled : null
    })
    check('C1 勾选后「批量修改」按钮可用', btnDisabled === false, { btnDisabled })

    await p.locator('button:visible', { hasText: '批量修改' }).first().click({ timeout: 8000 }).catch(e => console.log('  [批量修改点击失败]', e.message.slice(0, 80)))
    await p.waitForTimeout(1500)
    const opened = flat(await bodyText()).includes('已选中')
    await p.screenshot({ path: `${SHOTS}/ui-04-batch-modal.png` })
    const modalText = flat(await bodyText())
    check('C2 批量修改弹窗可打开且提示已选中', opened && modalText.includes('已选中'))
    check('C3 弹窗含「价格项下拉/直接改价/新价格/添加价格项」',
      modalText.includes('零售价') && modalText.includes('直接改价') && modalText.includes('添加价格项'))

    // 填新零售价 12.34 → 保存 → 校验落库
    const filled = await p.evaluate(() => {
      const num = document.querySelector('.batch-item .ant-input-number-input')
      if (!num) return false
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set
      setter.call(num, '12.34')
      num.dispatchEvent(new Event('input', { bubbles: true }))
      num.dispatchEvent(new Event('change', { bubbles: true }))
      return true
    })
    check('C4 批量修改弹窗可填写新价格', filled)
    await p.waitForTimeout(800)
    const clickedSave = await p.evaluate(() => {
      // 弹窗按钮文案为「保存(Enter)」等，按前缀匹配
      const btns = [...document.querySelectorAll('.ant-modal-footer button, .ant-modal button')]
        .filter(b => b.offsetParent !== null && /^保存/.test((b.innerText || '').replace(/\s+/g, '')))
      if (!btns.length) return false
      btns[btns.length - 1].click()
      return true
    })
    check('C4b 弹窗内「保存」可点击', clickedSave)
    await p.waitForTimeout(700)
    const toastText = flat(await p.evaluate(() => document.body.innerText))
    await p.waitForTimeout(1800)
    await p.screenshot({ path: `${SHOTS}/ui-04b-batch-saved.png` })
    check('C5 保存后提示已修改', toastText.includes('已修改'), toastText.slice(-160))

    // 校验接口侧已落库（与 UI 同口径，轮询避免时序误差）
    let apiRow = null
    let changed = null
    for (let i = 0; i < 5 && !changed; i++) {
      apiRow = await rawReq('GET', '/erp/md/product-price/page?pageNum=1&pageSize=50', null, TOKEN)
      changed = (apiRow?.data?.records || []).find(r => Math.abs(Number(r.retailPrice) - 12.34) < 0.01)
      if (!changed) await p.waitForTimeout(1000)
    }
    check('C6 批量修改真实落库（存在零售价=12.34 的行）', !!changed,
      { total: apiRow?.data?.total, beforeRetail })
    const firstRow = changed
    // 回滚原值
    if (firstRow && beforeRetail !== '') {
      await rawReq('PUT', '/erp/md/product-price/batch-modify', {
        unitIds: [String(firstRow.unitId)],
        items: [{ field: 'retailPrice', mode: 'FIXED', value: Number(beforeRetail.replace(/,/g, '')) || 0 }],
      }, TOKEN)
    }
    await p.keyboard.press('Escape')
    await p.waitForTimeout(600)
  }

  // ═══ D. 子标签 2：客户级别折扣设置 ═══
  {
    await clickText('客户级别折扣设置')
    await p.waitForTimeout(3000)
    await ensureTab('客户级别折扣设置')
    await p.screenshot({ path: `${SHOTS}/ui-05-grade-discount.png` })
    const t = flat(await bodyText())
    check('D1 子标签 2 无查询区（无「筛选条件」）', !t.includes('筛选条件'))
    const hs = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(x => (x.innerText || '').trim().replace(/\s+/g, '')).filter(Boolean))
    check('D2 表头含「客户级别/级别默认价」', hs.some(x => x.includes('客户级别')) && hs.some(x => x.includes('级别默认价')), hs.join('/'))
    check('D3 工具栏含「新增」', t.includes('新增'))
    const opened = await clickText('新增')
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-06-grade-modal.png` })
    const mt = flat(await bodyText())
    check('D4 新增弹窗含「客户级别/默认级别价/规则预览」',
      ['客户级别', '默认级别价', '规则预览'].every(x => mt.includes(x)))
    check('D5 规则预览文案（订货价格=）', mt.includes('订货价格'))
    await clickText('关闭', false)
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  }

  // ═══ E. 子标签 3：级别指定价设置 ═══
  {
    await clickText('级别指定价设置')
    await p.waitForTimeout(3000)
    await ensureTab('级别指定价设置')
    await p.screenshot({ path: `${SHOTS}/ui-07-level-price.png` })
    const t = flat(await bodyText())
    for (const label of ['客户级别', '商品/分类名称', '品牌', '查询', '新增', '导入', '批量删除']) {
      check(`E1 含「${label}」`, t.includes(flat(label)))
    }
    const hs = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(x => (x.innerText || '').trim().replace(/\s+/g, '')).filter(Boolean))
    for (const h of ['客户级别', '商品/分类名称', '货号', '单位', '价格规则', '条码', '规格', '型号', '品牌', '零售价', '批发价']) {
      check(`E2 表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/').slice(0, 140))
    }
    const opened = await clickText('新增')
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-08-level-modal.png` })
    const mt = flat(await bodyText())
    check('E3 指定价弹窗含「客户级别/商品/单位/指定价/单位价格自动换算」',
      ['客户级别', '商品', '单位', '指定价', '单位价格自动换算'].every(x => mt.includes(x)))
    check('E4 规则预览（商品指导价=）', mt.includes('商品指导价'))
    await clickText('取消', false)
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  }

  // ═══ F. 子标签 4：客户指定价设置 ═══
  {
    await clickText('客户指定价设置')
    await p.waitForTimeout(3000)
    await ensureTab('客户指定价设置')
    await p.screenshot({ path: `${SHOTS}/ui-09-customer-price.png` })
    const t = flat(await bodyText())
    for (const label of ['客户', '商品', '品牌', '查询', '新增', '导入', '批量删除']) {
      check(`F1 含「${label}」`, t.includes(flat(label)))
    }
    const hs = await p.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(x => (x.innerText || '').trim().replace(/\s+/g, '')).filter(Boolean))
    for (const h of ['客户名称', '客户编号', '商品/分类名称', '货号', '单位', '价格规则', '零售价', '批发价']) {
      check(`F2 表头含「${h}」`, hs.some(x => x.includes(h)), hs.join('/').slice(0, 140))
    }
    await clickText('新增')
    await p.waitForTimeout(1200)
    await p.screenshot({ path: `${SHOTS}/ui-10-customer-modal.png` })
    const mt = flat(await bodyText())
    check('F3 客户指定价弹窗含「客户/商品/单位/指定价」', ['客户', '商品', '单位', '指定价'].every(x => mt.includes(x)))
    await clickText('取消', false)
    await p.keyboard.press('Escape')
    await p.waitForTimeout(800)
  }

  // ═══ G. 导入向导（三步） ═══
  {
    const opened = await clickText('导入')
    await p.waitForTimeout(1500)
    await p.screenshot({ path: `${SHOTS}/ui-11-import-wizard.png` })
    const t = flat(await bodyText())
    check('G1 导入向导打开', opened)
    check('G2 三步标题齐全', ['下载模板', '导入Excel', '完成'].every(x => t.includes(flat(x))))
    check('G3 含「下载模版/下一步」', t.includes('下载模版') && t.includes('下一步'))
    await p.keyboard.press('Escape')
    await p.waitForTimeout(600)
  }

  // ═══ H. 子标签 1：等级价行内改价 + 前 5 列冻结 ═══
  {
    await clickText('商品价格批量修改')
    await p.waitForTimeout(3000)
    await ensureTab('商品价格批量修改')

    // H1/H2：前 5 列左冻结且 left 偏移递增（多列冻结不得重叠）
    const frozen = await p.evaluate(() => {
      return [...document.querySelectorAll('.ss-grid thead th')]
        .filter(t => getComputedStyle(t).position === 'sticky')
        .map(t => ({ key: t.getAttribute('data-col-key') || (t.innerText || '').trim(), left: t.style.left }))
    })
    check('H1 前 5 列默认左冻结', frozen.length >= 5, frozen)
    check('H2 冻结列 left 偏移依次递增（不重叠）',
      frozen.length >= 5 && frozen.slice(0, 5).every((f, i, arr) => i === 0 || parseFloat(f.left || '0') > parseFloat(arr[i - 1].left || '0')),
      frozen)

    // H3：横向滚动后冻结列仍贴左可见
    await p.evaluate(() => {
      const el = document.querySelector('.spreadsheet-table')
      if (el) el.scrollLeft = 800
    })
    await p.waitForTimeout(700)
    await p.screenshot({ path: `${SHOTS}/ui-12-frozen-scroll.png` })
    const pos = await p.evaluate(() => {
      const ths = [...document.querySelectorAll('.ss-grid thead th')]
      const idx = ths.findIndex(t => (t.innerText || '').includes('商品名称'))
      const grid = document.querySelector('.spreadsheet-table')
      if (idx < 0 || !grid) return null
      // 期望偏移 = 排在该列之前的左冻结列宽度之和（多列冻结应依次排开而非重叠在 0）
      let expected = 0
      for (let i = 0; i < idx; i++) {
        if (getComputedStyle(ths[i]).position === 'sticky') expected += ths[i].getBoundingClientRect().width
      }
      const th = ths[idx]
      return {
        scrolled: Math.round(grid.scrollLeft),
        thLeft: Math.round(th.getBoundingClientRect().left),
        gridLeft: Math.round(grid.getBoundingClientRect().left),
        expected: Math.round(expected),
      }
    })
    check('H3 横向滚动后「商品名称」按累计冻结宽度贴左可见',
      pos && pos.scrolled > 100 && Math.abs((pos.thLeft - pos.gridLeft) - pos.expected) < 15, pos)
    await p.evaluate(() => {
      const el = document.querySelector('.spreadsheet-table')
      if (el) el.scrollLeft = 0
    })
    await p.waitForTimeout(500)

    // H4~H6：等级价列行内改价并校验落库
    /**
     * 行内改价校验口径：填一个全表唯一值，再用整表反查命中行，
     * 避免"页面上编辑的行"与"接口返回第 N 行"不是同一条造成误判；命中行即真实 unitId，可直接回滚。
     */
    async function editCellAndVerify(field, expected) {
      const sel = `.ss-grid tbody tr td[data-col-key="${field}"]`
      // 先退出上一格的编辑态，避免残留编辑态在重渲染时用旧值二次提交
      await p.keyboard.press('Escape').catch(() => {})
      await p.waitForTimeout(300)
      priceReqs.length = 0
      await p.locator(sel).first().click({ timeout: 8000 }).catch(() => {})
      await p.waitForTimeout(600)
      const editor = await p.locator(`${sel} input.ss-native-number`).count()
      if (!editor) return { editor: 0 }
      await p.locator(`${sel} input.ss-native-number`).first().fill(String(expected))
      await p.waitForTimeout(400)   // 等 Vue 把新值写回 editingCell，否则 Enter 时判定"值未变"不提交
      await p.keyboard.press('Enter')
      await p.waitForTimeout(600)
      const toasts = await p.evaluate(() => [...document.querySelectorAll('.ant-message-notice-content')].map(e => (e.innerText || '').trim()))
      await p.waitForTimeout(500)
      let hit = null
      for (let k = 0; k < 8 && !hit; k++) {
        const res = await rawReq('GET', '/erp/md/product-price/page?pageNum=1&pageSize=200', null, TOKEN)
        hit = (res?.data?.records || []).find(r => Math.abs(Number(r[field]) - expected) < 0.01)
        if (!hit) await p.waitForTimeout(700)
      }
      await p.keyboard.press('Escape').catch(() => {})
      const reqSummary = priceReqs.map((b) => {
        try {
          const j = JSON.parse(b)
          const it = j.items?.[0] || {}
          return `${it.field}=${it.value}`
        } catch { return String(b).slice(0, 60) }
      })
      return { editor, hit, reqs: reqSummary, toasts }
    }

    const cellSel = '.ss-grid tbody tr td[data-col-key="gradePrice1"]'
    const cellCnt = await p.locator(cellSel).count()
    check('H4 等级价列存在可编辑单元格', cellCnt > 0, cellCnt)
    if (cellCnt > 0) {
      const r = await editCellAndVerify('gradePrice1', 601)
      check('H5 点击等级价单元格进入编辑态', r.editor > 0, r.editor)
      check('H6 等级价行内改价真实落库', !!r.hit, { hit: r.hit?.unitId, reqs: r.reqs })
      await p.screenshot({ path: `${SHOTS}/ui-13-grade-cell-edit.png` })
      if (r.hit) {
        await rawReq('PUT', '/erp/md/product-price/batch-modify', {
          unitIds: [String(r.hit.unitId)],
          items: [{ field: 'gradePrice1', mode: 'FIXED', value: 0 }],
        }, TOKEN)
      }
    }

    // H7：批发价 / 最低售价 / 零售价 同样可行内改价并落库
    const BASIC_CASES = [['wholesalePrice', '批发价', 602], ['minSalePrice', '最低售价', 603], ['retailPrice', '零售价', 604]]
    for (const [field, label, expected] of BASIC_CASES) {
      const r = await editCellAndVerify(field, expected)
      check(`H7 ${label}列可进入编辑态`, r.editor > 0, r.editor)
      check(`H8 ${label}行内改价真实落库`, !!r.hit, { expect: expected, reqs: r.reqs })
      if (r.hit) {
        await rawReq('PUT', '/erp/md/product-price/batch-modify', {
          unitIds: [String(r.hit.unitId)],
          items: [{ field, mode: 'FIXED', value: 0 }],
        }, TOKEN)
      }
    }
    await p.screenshot({ path: `${SHOTS}/ui-14-basic-price-edit.png` })
  }

  await p.screenshot({ path: `${SHOTS}/ui-99-final.png` })
  check('Z1 页面无 JS 运行时错误', errors.length === 0, errors.slice(0, 3).join(' | '))

  await browser.close()
  console.log(`\n结果：${PASS} 通过 / ${FAIL} 失败`)
  if (FAILURES.length) {
    console.log('失败项：')
    FAILURES.forEach(f => console.log(' -', f))
    process.exit(1)
  }
}

main().catch(e => { console.error('FATAL', e.message); process.exit(1) })
