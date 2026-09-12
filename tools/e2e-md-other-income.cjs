/*
 * 其他收入（资料 → 财务账户 → 其他收入，菜单 70542）金标准端到端验证
 *   · API 验收：收入类科目口径（损益类+贷方）/分页/关键字/停用过滤/新增(强制口径)/重号/必填/
 *                越界保护(非收入科目不可改删)/启停/导出真实 xlsx/删除
 *   · UI 验收：列表 4 列(操作|科目编号|科目名称|核算项) + 固定查询项(仅显示停用) + 表头齿轮列配置
 *               + 新增收入编辑器真实保存 + 行内 修改/删除/更多 + 打印(F8)
 *
 * 口径（红线）：本页是「收入类会计科目」视图，直接读写 finance_account_subject，
 *   income = subject_type=5（损益类） AND direction=2（贷方）；严禁另建收入类型字典。
 *
 * 用法：node tools/e2e-md-other-income.cjs
 *      BE_PORT=5671 FE_URL=http://localhost:5656 node tools/e2e-md-other-income.cjs
 */
const http = require('http')
const fs = require('fs')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.BE_PORT || 5671)
const SHOTS = 'I:/AI-Ready/tool-results/md-other-income'
const E2E_USER = process.env.E2E_USER || 'e2e_income'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null || body instanceof Buffer ? body : JSON.stringify(body)
    const headers = {
      ...(body instanceof Buffer ? { 'Content-Type': 'application/octet-stream' } : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {}),
    }
    if (data && !(body instanceof Buffer)) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        resolve({ status: res.statusCode, buf, headers: res.headers, json: tryJson(buf) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

function tryJson(buf) {
  try { return JSON.parse(buf.toString('utf8')) } catch (e) { return null }
}

let TOKEN = null

async function api(method, reqPath, body, extraHeaders) {
  let r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN, extraHeaders)
  }
  return r
}

/** 取接口 data */
function data(res) { return res.json ? res.json.data : null }

async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER,
    password: E2E_PWD,
    tenantName: '系统租户',
    captcha: code,
    captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
const NEW_CODE = '6T' + STAMP          // 测试收入科目编号（6 开头，损益类）
const NEW_NAME = 'E2E其他收入科目' + STAMP

// ══════════════ 一、接口验收 ══════════════
let createdId = null

async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 0) 前置清理：删除历史遗留的测试收入科目（6T/6U 开头），保证基线干净
  const stale = await api('GET', '/erp/md/other-income/list?includeDisabled=true')
  const staleRows = (data(stale) || []).filter(r => /^6[TU]/.test(String(r.subjectCode || '')))
  for (const r of staleRows) await api('DELETE', `/erp/md/other-income/${r.id}`)
  if (staleRows.length) console.log(`（前置清理历史测试收入科目 ${staleRows.length} 条）`)

  // 1) 基线：收入类科目口径（对标示例 4 条 + 系统既有收入科目）
  const p1 = await api('GET', '/erp/md/other-income/page?pageNum=1&pageSize=200&includeDisabled=true')
  const rows1 = data(p1)?.records || []
  check('分页接口可用且返回收入科目', p1.json?.code === 200 && rows1.length >= 4, `total=${data(p1)?.total}`)
  const codes = rows1.map(r => r.subjectCode)
  check('含对标示例 6101/6111/6301/6902',
    ['6101', '6111', '6301', '6902'].every(c => codes.includes(c)), codes.join(','))
  check('口径=损益类(5)且贷方(2)（全部记录）',
    rows1.every(r => r.subjectType === 5 && r.direction === 2),
    JSON.stringify(rows1.map(r => `${r.subjectCode}:${r.subjectType}/${r.direction}`).slice(0, 6)))
  check('不混入费用类科目（6602 管理费用 / 6604 折旧费）',
    !codes.includes('6602') && !codes.includes('6604'), codes.join(','))

  // 2) 关键字（编号 / 名称）
  const k1 = await api('GET', '/erp/md/other-income/page?pageNum=1&pageSize=20&keyword=6301&includeDisabled=true')
  const k1rows = data(k1)?.records || []
  check('关键字按科目编号命中', k1rows.length === 1 && k1rows[0].subjectCode === '6301', JSON.stringify(k1rows.map(r => r.subjectCode)))
  const k2 = await api('GET', `/erp/md/other-income/page?pageNum=1&pageSize=20&keyword=营业外&includeDisabled=true`)
  check('关键字按科目名称命中', (data(k2)?.records || []).some(r => r.subjectName.includes('营业外')), JSON.stringify((data(k2)?.records || []).map(r => r.subjectName)))

  // 3) 分页生效
  const pg = await api('GET', '/erp/md/other-income/page?pageNum=1&pageSize=2&includeDisabled=true')
  check('分页 pageSize=2 生效',
    (data(pg)?.records || []).length === 2 && Number(data(pg)?.total) >= 4,
    `records=${(data(pg)?.records || []).length} total=${data(pg)?.total}`)

  // 4) 新增（不传 subjectType/direction → 后端强制 损益类 + 贷方）
  const c1 = await api('POST', '/erp/md/other-income', { subjectCode: NEW_CODE, subjectName: NEW_NAME })
  createdId = data(c1)?.id
  check('新增收入成功（仅传编号+名称）', c1.json?.code === 200 && !!createdId, c1.json?.message)
  check('新增强制口径 损益类(5)/贷方(2)',
    data(c1)?.subjectType === 5 && data(c1)?.direction === 2,
    `type=${data(c1)?.subjectType} dir=${data(c1)?.direction}`)

  // 5) 校验：重号 / 必填
  const dup = await api('POST', '/erp/md/other-income', { subjectCode: NEW_CODE, subjectName: '重号收入' })
  check('科目编号重复被拒', dup.json?.code !== 200 && /已存在/.test(dup.json?.message || ''), dup.json?.message)
  const noCode = await api('POST', '/erp/md/other-income', { subjectCode: '', subjectName: '缺编号' })
  check('科目编号必填校验', noCode.json?.code !== 200 && /科目编号/.test(noCode.json?.message || ''), noCode.json?.message)
  const noName = await api('POST', '/erp/md/other-income', { subjectCode: NEW_CODE + 'X', subjectName: '' })
  check('科目名称必填校验', noName.json?.code !== 200 && /科目名称/.test(noName.json?.message || ''), noName.json?.message)

  // 6) 新增后进入列表
  const afterCreate = await api('GET', `/erp/md/other-income/list?keyword=${NEW_CODE}&includeDisabled=true`)
  check('新增后出现在收入科目列表', (data(afterCreate) || []).some(r => r.id === createdId), JSON.stringify((data(afterCreate) || []).map(r => r.subjectCode)))

  // 7) 修改
  const upd = await api('PUT', `/erp/md/other-income/${createdId}`, {
    subjectCode: NEW_CODE, subjectName: NEW_NAME + '改', direction: 2, isEnabled: true,
  })
  check('修改收入科目成功', upd.json?.code === 200, upd.json?.message)
  const detail = await api('GET', `/erp/md/other-income/${createdId}`)
  check('修改后名称生效且仍为收入口径',
    data(detail)?.subjectName === NEW_NAME + '改' && data(detail)?.subjectType === 5 && data(detail)?.direction === 2,
    `${data(detail)?.subjectName} / ${data(detail)?.subjectType}/${data(detail)?.direction}`)

  // 8) 越界保护：非收入科目（6602 管理费用）不可通过本页改/删
  const fee = await api('GET', `/erp/finance/subject/list?keyword=6602`)
  const feeRow = (data(fee) || []).find(r => r.subjectCode === '6602')
  if (feeRow) {
    const cross = await api('PUT', `/erp/md/other-income/${feeRow.id}`, { subjectName: '越界改名' })
    check('越界保护：非收入科目不可修改', cross.json?.code !== 200, cross.json?.message)
    const crossDel = await api('DELETE', `/erp/md/other-income/${feeRow.id}`)
    check('越界保护：非收入科目不可删除', crossDel.json?.code !== 200, crossDel.json?.message)
  } else {
    check('越界保护：非收入科目不可修改', false, '未找到 6602 管理费用，无法验证')
  }

  // 9) 停用 / 启用 + 显示停用过滤
  const off = await api('PUT', `/erp/md/other-income/${createdId}/enable?enabled=false`)
  check('停用收入科目成功', off.json?.code === 200 && data(off)?.isEnabled === false, JSON.stringify(data(off)?.isEnabled))
  const hidden = await api('GET', `/erp/md/other-income/page?pageNum=1&pageSize=20&keyword=${NEW_CODE}&includeDisabled=false`)
  check('显示停用=否 时不返回已停用收入', (data(hidden)?.records || []).length === 0, `records=${(data(hidden)?.records || []).length}`)
  const shown = await api('GET', `/erp/md/other-income/page?pageNum=1&pageSize=20&keyword=${NEW_CODE}&includeDisabled=true`)
  check('显示停用=是 时返回已停用收入', (data(shown)?.records || []).length === 1, `records=${(data(shown)?.records || []).length}`)
  const on = await api('PUT', `/erp/md/other-income/${createdId}/enable?enabled=true`)
  check('启用收入科目成功', on.json?.code === 200 && data(on)?.isEnabled === true, JSON.stringify(data(on)?.isEnabled))

  // 10) 核算项下拉 / 平铺列表
  const aux = await api('GET', '/erp/md/other-income/aux-types')
  const auxRef = await api('GET', '/erp/finance/subject/aux-types')
  check('核算项下拉可用且与会计科目同源',
    aux.json?.code === 200 && Array.isArray(data(aux)) && (data(aux) || []).length === (data(auxRef) || []).length,
    `aux=${(data(aux) || []).length} ref=${(data(auxRef) || []).length} code=${aux.json?.code}/${auxRef.json?.code} raw=${JSON.stringify(aux.json).slice(0, 120)}`)
  const lst = await api('GET', '/erp/md/other-income/list?includeDisabled=true')
  check('平铺列表接口可用', lst.json?.code === 200 && Array.isArray(data(lst)) && data(lst).length >= 4, `count=${(data(lst) || []).length}`)

  // 11) 导出真实 xlsx
  const exp = await api('GET', '/erp/md/other-income/export?includeDisabled=true')
  const isXlsx = exp.buf && exp.buf.length > 1000 && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b
  check('导出返回真实 xlsx 流', !!isXlsx, `${exp.headers['content-type']} ${exp.buf?.length}B`)
  check('导出文件名带日期且中文正确编码', /filename\*=UTF-8''/.test(exp.headers['content-disposition'] || ''), exp.headers['content-disposition'])

  // 12) 删除
  const del = await api('DELETE', `/erp/md/other-income/${createdId}`)
  check('删除收入科目成功', del.json?.code === 200, del.json?.message)
  const afterDel = await api('GET', `/erp/md/other-income/list?keyword=${NEW_CODE}&includeDisabled=true`)
  check('删除后列表不再包含该科目', !(data(afterDel) || []).some(r => r.id === createdId))
  createdId = null
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
  console.log(`\n═══ 二、UI 验收（vite ${FE} → api ${PORT}） ═══`)
  const ui = []
  const ck = (name, ok, detail = '') => { ui.push({ name, ok, detail }); console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`) }

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()
  page.on('pageerror', e => console.log('[pageerror]', (e.message || '').slice(0, 200)))

  // 打印窗口拦截：捕获 window.open 的 HTML
  await page.addInitScript(() => {
    window.__printHtml = ''
    window.open = function () {
      return {
        document: { write: (h) => { window.__printHtml += h }, close: () => {} },
        focus: () => {},
        print: () => { window.__printCalled = true },
      }
    }
  })

  // vite 代理指向 5655；本会话后端在 PORT，用精确前缀转发（勿用 **/api/** 会误伤 /src/api/*.ts）
  await page.route(`${FE}/api/**`, async (route) => {
    const target = route.request().url().replace(`${FE}/api/`, `http://localhost:${PORT}/api/`)
    try { await route.continue({ url: target }) } catch { await route.abort() }
  })

  // 复用 API 阶段的 token（sa-token is-concurrent=false：再次登录会踢掉旧 token，反而制造 401）
  const token = TOKEN || (await login())
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([t]) => {
    localStorage.setItem('token', t)
    localStorage.setItem('access_token', t)
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('tenantName', '系统租户')
    localStorage.setItem('userTenants', JSON.stringify([{ id: 1, tenantName: '系统租户' }]))
  }, [token])

  // ⚠️ sa-token is-concurrent=false：同一账号重复登录会互踢，本套件全程只登一次，必要时自愈重登
  const gotoPage = async (attempt = 0) => {
    await page.goto(`${FE}/md/other-income`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(4500)
    const onLogin = page.url().includes('/login')
    const noPage = (await page.locator('text=新增收入').count()) === 0
    if (onLogin || noPage) {
      if (attempt > 4) throw new Error('重登超过上限，页面未打开')
      const t = await login()
      await page.evaluate(([tk]) => {
        localStorage.setItem('token', tk); localStorage.setItem('access_token', tk)
        localStorage.setItem('tenantId', '1'); localStorage.setItem('tenantName', '系统租户')
      }, [t])
      return gotoPage(attempt + 1)
    }
  }

  await gotoPage()
  await page.waitForTimeout(1500)
  await page.screenshot({ path: `${SHOTS}/ui-01-page.png` })

  const txt = async (sel) => (await page.locator(sel).first().innerText().catch(() => '')).trim()
  const rowTexts = () => page.$$eval('.ss-grid tbody tr', trs => trs
    .map(tr => (tr.innerText || '').trim().replace(/\s+/g, ' '))
    .filter(t => t && !/^(\d+)$/.test(t)))

  // ── 1. 工具栏（对标：新增收入 / 刷新 / 打印(F8) / 导出） ──
  const btns = (await page.locator('button').allInnerTexts()).map(s => s.trim()).filter(Boolean)
  ck('工具栏含 新增收入 / 刷新 / 打印(F8) / 导出',
    ['新增收入', '刷新', '打印(F8)', '导出'].every(b => btns.some(t => t.includes(b))), btns.slice(0, 8).join(' | '))

  // ── 2. 查询区（对标固定项：仅「显示停用」，无文本查询框） ──
  ck('查询区含「显示停用」勾选框', (await page.locator('text=显示停用').count()) > 0)
  const searchInputs = await page.locator('.search-area input[type=text], .search-area input:not([type])').count()
  ck('查询区无文本查询框（对标仅开关项）', searchInputs === 0, `inputs=${searchInputs}`)
  const disabledChecked = await page.evaluate(() => {
    const cb = [...document.querySelectorAll('input[type=checkbox]')].find(el => /显示停用/.test((el.closest('label') || el.parentElement)?.innerText || ''))
    return !!cb && cb.checked
  })
  ck('「显示停用」默认不勾选（默认只看启用）', disabledChecked === false)

  // ── 3. 数据表列（对标：操作 | 科目编号 | 科目名称 | 核算项） ──
  const headers = await page.$$eval('.ss-grid thead th', ths => ths.map(t => (t.innerText || '').trim()).filter(Boolean))
  ck('数据表含 操作/科目编号/科目名称/核算项',
    ['操作', '科目编号', '科目名称', '核算项'].every(h => headers.includes(h)), headers.join(' | '))
  ck('表头齿轮（列配置入口）存在', (await page.locator('.th-settings-btn').count()) > 0, `count=${await page.locator('.th-settings-btn').count()}`)

  // ── 4. 对标数据（收入类科目） ──
  let rows = await rowTexts()
  const joined = rows.join('|')
  ck('列表含对标示例 6101/6111/6301/6902',
    ['6101', '6111', '6301', '6902'].every(c => joined.includes(c)), joined.slice(0, 160))
  ck('列表不含费用类科目（6602 管理费用 / 6604 折旧费）',
    !joined.includes('6602') && !joined.includes('6604'))

  // ── 5. 行内操作（对标：修改 / 删除 / 更多） ──
  ck('行内操作含 修改/删除/更多',
    (await page.locator('.ss-grid tbody button:has-text("修改")').count()) >= 1 &&
    (await page.locator('.ss-grid tbody button:has-text("删除")').count()) >= 1 &&
    (await page.locator('.ss-grid tbody button:has-text("更多")').count()) >= 1)

  // ── 6. 列配置弹窗（个人配置 / 全局配置，可配 3 列） ──
  await page.locator('.th-settings-btn').first().click({ force: true })
  await page.waitForTimeout(1800)
  await page.screenshot({ path: `${SHOTS}/ui-02-colconfig.png` })
  const dlgText = await page.locator('.ant-modal-content:visible, .ant-drawer-content:visible').first().innerText().catch(() => '')
  ck('列配置弹窗含 个人配置/全局配置 双 Tab', /个人配置/.test(dlgText) && /全局配置/.test(dlgText))
  ck('列配置可配列 = 3 列（科目编号/科目名称/核算项）',
    ['科目编号', '科目名称', '核算项'].every(c => dlgText.includes(c)), dlgText.replace(/\s+/g, ' ').slice(0, 150))
  const closeCfg = page.locator('.ant-modal-close').last()
  if (await closeCfg.count()) await closeCfg.click({ force: true })
  await page.waitForTimeout(1200)

  // ── 7. 新增收入 → 会计科目编辑器（6 字段 + 默认贷方） ──
  await page.locator('.btn-add').click()
  await page.waitForTimeout(1800)
  const modalTitle = await page.locator('.ant-modal-title:visible').first().innerText().catch(() => '')
  ck('新增弹窗标题 = 新增收入', /新增收入/.test(modalTitle), modalTitle)
  const editorText = await page.locator('.ant-modal-content:visible').first().innerText().catch(() => '')
  ck('编辑器字段 = 科目编号*/科目名称*/助记码/科目全名/核算项/借-贷',
    ['科目编号', '科目名称', '助记码', '科目全名', '核算项', '借/贷'].every(f => editorText.includes(f)),
    editorText.replace(/\s+/g, ' ').slice(0, 180))
  const creditChecked = await page.evaluate(() => {
    const radios = [...document.querySelectorAll('.ant-modal-content .ant-radio-wrapper')]
    const credit = radios.find(r => /贷方/.test(r.innerText || ''))
    return !!credit && credit.querySelector('input')?.checked === true
  })
  ck('新增收入默认余额方向 = 贷方（收入口径）', creditChecked)

  // ── 8. 真实保存 → 列表出现新科目 ──
  const UI_CODE = '6U' + STAMP
  const UI_NAME = 'UI验收收入' + STAMP
  await page.locator('.ant-modal-content:visible input').nth(0).fill(UI_CODE)
  await page.locator('.ant-modal-content:visible input').nth(1).fill(UI_NAME)
  await page.screenshot({ path: `${SHOTS}/ui-03-editor.png` })
  await page.locator('.ant-modal-content:visible .ant-modal-footer button.ant-btn-primary').last().click()
  await page.waitForTimeout(3500)
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(4500)
  rows = await rowTexts()
  ck('新增收入保存后出现在列表', rows.join('|').includes(UI_CODE), rows.join('|').slice(0, 160))
  await page.screenshot({ path: `${SHOTS}/ui-04-saved.png` })

  // ── 9. 编辑（行内「修改」→ 会计科目编辑器，编辑态标题） ──
  const rowSel = page.locator('.ss-grid tbody tr').filter({ hasText: UI_CODE }).first()
  await rowSel.locator('button:has-text("修改")').first().click()
  await page.waitForTimeout(1800)
  const editTitle = await page.locator('.ant-modal-title:visible').first().innerText().catch(() => '')
  ck('编辑态弹窗标题 = 会计科目', /会计科目/.test(editTitle), editTitle)
  const editCode = await page.locator('.ant-modal-content:visible input').nth(0).inputValue().catch(() => '')
  ck('编辑态回填科目编号', editCode === UI_CODE, editCode)
  await page.locator('.ant-modal-content:visible .ant-modal-footer button').first().click()
  await page.waitForTimeout(1200)

  // ── 10. 打印(F8)：真实打印模板 ──
  await page.keyboard.press('F8')
  await page.waitForTimeout(1500)
  const printHtml = await page.evaluate(() => window.__printHtml || '')
  ck('打印(F8) 生成真实打印模板（含标题与表头）',
    /其他收入/.test(printHtml) && /科目编号/.test(printHtml) && /核算项/.test(printHtml),
    printHtml.replace(/\s+/g, ' ').slice(0, 120))

  // ── 11. 清理：删除 UI 新增的科目 ──
  const rowDel = page.locator('.ss-grid tbody tr').filter({ hasText: UI_CODE }).first()
  if (await rowDel.count()) {
    await rowDel.locator('button:has-text("删除")').first().click()
    await page.waitForTimeout(900)
    await page.locator('.ant-modal-confirm .ant-btn-dangerous, .ant-modal-confirm button.ant-btn-primary').last().click()
    await page.waitForTimeout(2500)
  }
  const afterUiDel = await api('GET', `/erp/md/other-income/list?keyword=${UI_CODE}&includeDisabled=true`)
  ck('UI 删除后接口不再返回该科目（清理完成）', !(data(afterUiDel) || []).some(r => r.subjectCode === UI_CODE))
  await page.screenshot({ path: `${SHOTS}/ui-05-final.png` })

  await browser.close()
  return ui
}

// ══════════════ 主流程 ══════════════
async function main() {
  console.log(`其他收入金标准 E2E ｜ 后端 :${PORT} ｜ 前端 ${FE}`)
  await login()
  await apiSuite().catch(e => check('接口验收异常', false, e.message))

  let ui = []
  if (process.argv[2] !== 'api') {
    ui = await uiSuite().catch(e => { check('UI 验收异常', false, e.message); return [] })
  }

  const all = [...results, ...ui]
  const fail = all.filter(r => !r.ok)
  console.log('\n═══ 汇总 ═══')
  console.log(`合计 ${all.length} 项，通过 ${all.length - fail.length}，失败 ${fail.length}`)
  if (fail.length) fail.forEach(f => console.log(`  ❌ ${f.name} — ${f.detail}`))
  console.log(`截图目录：${SHOTS}`)
  process.exit(fail.length ? 1 : 0)
}

main()
