// 银行账户（资料 → 财务账户 → 银行账户）金标准验收：接口 + 真实浏览器 UI
// 后端：独立端口 5801（避免与并行会话共用端口）；前端 Vite 5657（5656 被并行会话占用）
const path = require('path')
const fs = require('fs')
const os = require('os')
const { execFileSync } = require('child_process')
const { chromium } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/playwright'))
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const APP = process.env.BANK_APP || 'http://localhost:5657'
const API = process.env.BANK_API || 'http://localhost:5801'
const SHOT = path.join(__dirname, '../../tool-results')
const TMP = os.tmpdir()

let pass = 0, fail = 0
function check(name, ok, detail) {
  if (ok) { pass++; console.log(`  ✅ ${name}${detail ? ' — ' + detail : ''}`) }
  else { fail++; console.log(`  ❌ ${name}${detail ? ' — ' + detail : ''}`) }
}
const sleep = ms => new Promise(r => setTimeout(r, ms))

function curl(args) {
  return execFileSync('curl', ['-s', '--max-time', '30', ...args], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
}
let TOKEN = ''
function apiGet(p) { return JSON.parse(curl(['-H', `Authorization: Bearer ${TOKEN}`, `${API}${p}`]) || '{}') }
function apiSend(method, p, body) {
  const args = ['-X', method, '-H', `Authorization: Bearer ${TOKEN}`, '-H', 'Content-Type: application/json; charset=utf-8']
  let f = null
  if (body !== undefined) {
    f = path.join(TMP, `bank-body-${Date.now()}-${Math.random().toString(36).slice(2)}.json`)
    fs.writeFileSync(f, JSON.stringify(body), 'utf8')
    args.push('-d', `@${f}`)
  }
  args.push(`${API}${p}`)
  try { return JSON.parse(curl(args) || '{}') } finally { if (f) { try { fs.unlinkSync(f) } catch {} } }
}
function captchaText(imgSrc) {
  const m = (imgSrc || '').match(/base64,([A-Za-z0-9+/]+=*)/)
  if (!m) return ''
  const svg = Buffer.from(m[1], 'base64').toString('utf8')
  return [...svg.matchAll(/<text[^>]*>([^<]*)<\/text>/g)].map(x => x[1]).join('')
}
// ⚠️ a-modal 关闭后 DOM 仍保留（仅 wrap display:none），必须用 :visible 判定与定位
const visibleModals = (page) => page.locator('.ant-modal-content:visible')
async function closeAllModals(page) {
  for (let i = 0; i < 4; i++) {
    if ((await visibleModals(page).count().catch(() => 0)) === 0) return
    await page.locator('.ant-modal-close:visible').last().click({ force: true }).catch(() => {})
    await page.waitForTimeout(800)
    if ((await visibleModals(page).count().catch(() => 0)) === 0) return
    await page.mouse.click(420, 870) // 点遮罩（mask-closable）
    await page.waitForTimeout(800)
  }
}

async function apiLogin() {
  for (let i = 1; i <= 12; i++) {
    let cap
    try { cap = JSON.parse(curl([`${API}/api/auth/captcha`])).data || {} } catch { await sleep(6000); continue }
    const f = path.join(TMP, `bank-login-${Date.now()}.json`)
    fs.writeFileSync(f, JSON.stringify({
      username: 'sex_e2e', password: 'admin123', tenantName: '系统租户',
      captcha: captchaText(cap.img), captchaKey: cap.uuid,
    }), 'utf8')
    let res = {}
    try { res = JSON.parse(curl(['-X', 'POST', '-H', 'Content-Type: application/json; charset=utf-8', '-d', `@${f}`, `${API}/api/auth/login`]) || '{}') }
    finally { try { fs.unlinkSync(f) } catch {} }
    const d = res.data || {}
    if (d.token) return d
    console.log(`  ⚠️ 登录未成功(${res.message || 'no-token'})，重试…`)
    await sleep(5000)
  }
  throw new Error('接口登录失败')
}

;(async () => {
  fs.mkdirSync(SHOT, { recursive: true })
  const pg = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await pg.connect()
  // 清理历史验收残留（上次失败run的 UI 数据），保证基线干净
  await pg.query(`DELETE FROM finance_account WHERE account_name LIKE 'UI银行%' OR account_name LIKE 'E2E银行%'`)
  const scalar = async (q, params) => {
    const r = await pg.query(q, params)
    const rows = Array.isArray(r) ? r[r.length - 1].rows : r.rows
    return rows.length ? Object.values(rows[0])[0] : null
  }

  // ═══════════════════════════════════════════
  console.log('\n【数据库 · 单一口径与列结构】')
  const colCnt = Number(await scalar(`SELECT COUNT(*)::int FROM information_schema.columns
     WHERE table_name='finance_account' AND column_name IN
     ('subject_code','parent_id','mall_transfer_enabled','easy_code','brief_name','account_holder','qrcode_url','is_system','sort_no')`))
  check('finance_account 补全 9 列（复用既有表，未另建重复表）', colCnt === 9, `${colCnt} 列`)
  const dupTable = Number(await scalar(`SELECT COUNT(*)::int FROM information_schema.tables
     WHERE table_schema='public' AND table_name LIKE '%bank_account%' AND table_name <> 'finance_account'`))
  check('不存在重复银行账户表（单一口径红线）', dupTable === 0, `重复表 ${dupTable} 个`)
  const fk = Number(await scalar(`SELECT COUNT(*)::int FROM information_schema.tables WHERE table_name='finance_account'`))
  check('finance_account 表存在', fk === 1)
  const hist = await pg.query(`SELECT version, success FROM flyway_schema_history WHERE version='11.138.0'`)
  check('Flyway 迁移 11.138.0 已登记且 success', hist.rows.length === 1 && hist.rows[0].success === true, JSON.stringify(hist.rows))

  // ═══════════════════════════════════════════
  console.log('\n【接口 · 认证】')
  const sess = await apiLogin()
  TOKEN = sess.token
  check('登录成功', !!TOKEN, `token=${String(TOKEN).slice(0, 8)}…`)

  console.log('\n【接口 · 分页查询（对标 4 列 + 树形）】')
  const page1 = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=20&showTree=1')
  const recs = page1.data?.records || []
  check('返回分页结构', !!page1.data && Array.isArray(recs), `total=${page1.data?.total}`)
  check('分页有真实数据（非空壳）', recs.length > 0, `${recs.length} 条`)
  const first = recs[0] || {}
  check('对标列字段齐备：科目编号/科目名称/账户类型/是否用于商城线下转账收款',
    'subjectCode' in first && 'accountName' in first && 'accountType' in first && 'mallTransferEnabled' in first,
    Object.keys(first).filter(k => ['subjectCode', 'accountName', 'accountType', 'mallTransferEnabled'].includes(k)).join(','))
  check('树形派生字段 level/hasChildren 下发', 'level' in first && 'hasChildren' in first)
  const codes = recs.map(r => r.subjectCode)
  check('科目编号按编码升序（同级排序）', JSON.stringify(codes) === JSON.stringify([...codes].sort()), codes.join(' / '))

  console.log('\n【接口 · 查询条件】')
  const only1001 = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=20&keyword=1001')
  const k1 = only1001.data?.records || []
  check('筛选条件（关键字）生效', k1.length >= 1 && k1.every(r => `${r.subjectCode}${r.accountName}`.includes('1001')), `${k1.length} 条`)
  const kwName = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=20&keyword=' + encodeURIComponent('招行'))
  check('关键字支持科目名称模糊匹配', (kwName.data?.records || []).some(r => (r.accountName || '').includes('招行')))
  const typeFilter = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=20&accountType=2')
  check('账户类型过滤生效', (typeFilter.data?.records || []).every(r => r.accountType === 2), `${(typeFilter.data?.records || []).length} 条`)

  console.log('\n【接口 · 银行编号生成】')
  const nextRoot = apiGet('/api/erp/finance/bank-account/next-code')
  check('顶级编号生成（未占用）', typeof nextRoot.data === 'string' && /^\d+$/.test(nextRoot.data), String(nextRoot.data))
  const occupied = Number(await scalar(`SELECT COUNT(*)::int FROM finance_account WHERE subject_code=$1 AND deleted_flag=0`, [nextRoot.data]))
  check('生成编号未被占用', occupied === 0, nextRoot.data)

  console.log('\n【接口 · 新增（全字段落库）】')
  const stamp = Date.now().toString().slice(-6)
  const newCode = `9${stamp}`
  const createRes = apiSend('POST', '/api/erp/finance/bank-account', {
    subjectCode: newCode,
    accountName: `E2E银行-${stamp}`,
    accountType: 1,
    easyCode: `E2E${stamp}`,
    briefName: 'E2E简称',
    bankName: '中国银行',
    accountHolder: '测试户主',
    bankAccount: `6222${stamp}0001`,
    currency: 'CNY',
    qrcodeUrl: 'https://example.com/qr.png',
    mallTransferEnabled: 1,
    status: 1,
    sortNo: 5,
    remark: '金标准验收数据',
  })
  const newId = createRes.data?.id
  check('新增返回 200 且带 id', createRes.code === 200 && !!newId, `id=${newId}`)
  const dbRow = (await pg.query('SELECT * FROM finance_account WHERE id=$1', [newId])).rows[0] || {}
  check('落库字段逐项正确（编号/名称/类型/助记码/简称/开户行/户主/账号/收款码/商城收款/排序/备注）',
    dbRow.subject_code === newCode && dbRow.account_name === `E2E银行-${stamp}` && dbRow.account_type === 1 &&
    dbRow.easy_code === `E2E${stamp}` && dbRow.brief_name === 'E2E简称' && dbRow.bank_name === '中国银行' &&
    dbRow.account_holder === '测试户主' && dbRow.bank_account === `6222${stamp}0001` &&
    dbRow.qrcode_url === 'https://example.com/qr.png' && dbRow.mall_transfer_enabled === 1 &&
    dbRow.sort_no === 5 && dbRow.remark === '金标准验收数据',
    `subject_code=${dbRow.subject_code}, mall=${dbRow.mall_transfer_enabled}`)
  check('租户/审计字段自动填充', Number(dbRow.tenant_id) === 1 && !!dbRow.created_at, `tenant=${dbRow.tenant_id}, created_at=${dbRow.created_at}`)

  console.log('\n【接口 · 唯一性校验】')
  const dupRes = apiSend('POST', '/api/erp/finance/bank-account', { subjectCode: newCode, accountName: '重复编号' })
  check('重复银行编号被拒（返回可读错误）', dupRes.code !== 200 && String(dupRes.message).includes('已存在'), dupRes.message)
  const noName = apiSend('POST', '/api/erp/finance/bank-account', { subjectCode: `8${stamp}` })
  check('银行全称必填校验', noName.code !== 200 && String(noName.message).includes('银行全称'), noName.message)

  console.log('\n【接口 · 树形层级（父子挂接）】')
  const childCodeRes = apiGet(`/api/erp/finance/bank-account/next-code?parentId=${newId}`)
  check('子账户编号 = 父编号 + 两位序号', String(childCodeRes.data).startsWith(newCode) && String(childCodeRes.data).length === newCode.length + 2, String(childCodeRes.data))
  const childRes = apiSend('POST', '/api/erp/finance/bank-account', {
    subjectCode: childCodeRes.data, accountName: `E2E子账户-${stamp}`, accountType: 1, parentId: Number(newId),
  })
  const childId = childRes.data?.id
  check('新增子账户成功', childRes.code === 200 && !!childId, `id=${childId}`)
  const treePage = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=50&showTree=1')
  const tRecs = treePage.data?.records || []
  const parentRow = tRecs.find(r => String(r.id) === String(newId)) || {}
  const childRow = tRecs.find(r => String(r.id) === String(childId)) || {}
  check('父账户 hasChildren=true', parentRow.hasChildren === true)
  check('子账户 level=2 且紧邻父账户之后', childRow.level === 2 && tRecs.indexOf(childRow) === tRecs.indexOf(parentRow) + 1,
    `level=${childRow.level}, idx=${tRecs.indexOf(childRow)}`)
  const flatPage = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=50&showTree=0')
  check('未勾选「显示层次结构」时 level 不下发（平铺）', (flatPage.data?.records || []).every(r => r.level == null))

  console.log('\n【接口 · 修改】')
  const updRes = apiSend('PUT', `/api/erp/finance/bank-account/${newId}`, {
    accountName: `E2E银行-改-${stamp}`, bankName: '工商银行', mallTransferEnabled: 0, status: 1, remark: '已修改',
  })
  check('修改返回 200', updRes.code === 200, updRes.message)
  const updRow = (await pg.query('SELECT * FROM finance_account WHERE id=$1', [newId])).rows[0] || {}
  check('修改落库（名称/开户行/商城收款开关）',
    updRow.account_name === `E2E银行-改-${stamp}` && updRow.bank_name === '工商银行' && updRow.mall_transfer_enabled === 0,
    `name=${updRow.account_name}`)
  const badParent = apiSend('PUT', `/api/erp/finance/bank-account/${newId}`, { parentId: Number(childId) })
  check('上级账户不能是自己的下级（防环）', badParent.code !== 200, badParent.message)
  const selfParent = apiSend('PUT', `/api/erp/finance/bank-account/${newId}`, { parentId: Number(newId) })
  check('上级账户不能是自身', selfParent.code !== 200, selfParent.message)

  console.log('\n【接口 · 停用 / 显示停用】')
  const offRes = apiSend('PUT', `/api/erp/finance/bank-account/${childId}/status`, 0)
  check('停用返回 200', offRes.code === 200 && offRes.data?.status === 0, `status=${offRes.data?.status}`)
  const defPage = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=50&showTree=0')
  check('未勾选「显示停用」时不返回停用数据', !(defPage.data?.records || []).some(r => String(r.id) === String(childId)))
  const withDisabled = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=50&showTree=0&showDisabled=1')
  check('勾选「显示停用」时返回停用数据', (withDisabled.data?.records || []).some(r => String(r.id) === String(childId)))
  const onRes = apiSend('PUT', `/api/erp/finance/bank-account/${childId}/status`, 1)
  check('恢复启用', onRes.code === 200 && onRes.data?.status === 1)

  console.log('\n【接口 · 删除保护与删除】')
  const delParent = apiSend('DELETE', `/api/erp/finance/bank-account/${newId}`)
  check('存在下级账户时禁止删除', delParent.code !== 200 && String(delParent.message).includes('下级'), delParent.message)
  const preset = tRecs.find(r => r.subjectCode === '1001')
  if (preset) {
    const delPreset = apiSend('DELETE', `/api/erp/finance/bank-account/${preset.id}`)
    check('系统预置账户（1001/1002）禁止删除', delPreset.code !== 200 && String(delPreset.message).includes('预置'), delPreset.message)
  } else {
    check('系统预置账户（1001/1002）禁止删除', false, '未找到 1001 账户')
  }
  const delChild = apiSend('DELETE', `/api/erp/finance/bank-account/${childId}`)
  const delChildRow = (await pg.query('SELECT deleted_flag FROM finance_account WHERE id=$1', [childId])).rows[0] || {}
  check('删除子账户 → 逻辑删除（deleted_flag=1）', delChild.code === 200 && delChildRow.deleted_flag === 1, `flag=${delChildRow.deleted_flag}`)
  const delNew = apiSend('DELETE', `/api/erp/finance/bank-account/${newId}`)
  const delNewRow = (await pg.query('SELECT deleted_flag FROM finance_account WHERE id=$1', [newId])).rows[0] || {}
  check('删除父账户（无下级后）成功', delNew.code === 200 && delNewRow.deleted_flag === 1)
  const afterDel = apiGet('/api/erp/finance/bank-account/page?pageNum=1&pageSize=50&showTree=0')
  check('删除后列表不再返回', !(afterDel.data?.records || []).some(r => String(r.id) === String(newId)))
  // 清理
  await pg.query('DELETE FROM finance_account WHERE id IN ($1,$2)', [newId, childId])

  console.log('\n【接口 · 导出（真实 xlsx）】')
  const outFile = path.join(TMP, `bank-export-${Date.now()}.xlsx`)
  const hdr = curl(['-D', '-', '-o', outFile, '-H', `Authorization: Bearer ${TOKEN}`, `${API}/api/erp/finance/bank-account/export`])
  const size = fs.existsSync(outFile) ? fs.statSync(outFile).size : 0
  const magic = size > 4 ? fs.readFileSync(outFile).subarray(0, 2).toString('latin1') : ''
  check('导出返回 sheet 内容类型', /spreadsheetml\.sheet/.test(hdr), (hdr.match(/content-type:([^\r\n]+)/i) || [])[1])
  check('导出为真实 xlsx（ZIP magic + 非空）', magic === 'PK' && size > 3000, `${size} bytes, magic=${magic}`)
  try { fs.unlinkSync(outFile) } catch {}

  console.log('\n【接口 · 全局列配置（跨终端服务端持久化）】')
  const CFG_KEY = 'md-bank-account-columns'
  const cfgFile = path.join(TMP, `bank-cfg-${Date.now()}.json`)
  fs.writeFileSync(cfgFile, JSON.stringify({
    value: JSON.stringify([{ key: 'subjectCode', visible: true }, { key: 'bankName', visible: true }]),
  }), 'utf8')
  let cfgSave = {}
  try {
    cfgSave = JSON.parse(curl(['-X', 'POST', '-H', `Authorization: Bearer ${TOKEN}`,
      '-H', 'Content-Type: application/json; charset=utf-8', '-d', `@${cfgFile}`,
      `${API}/api/system/user-config/col-config/${CFG_KEY}`]) || '{}')
  } finally { try { fs.unlinkSync(cfgFile) } catch {} }
  check('全局列配置写入成功（服务端，跨浏览器/终端）', cfgSave.code === 200, `code=${cfgSave.code}`)
  const cfgRead = curl(['-H', `Authorization: Bearer ${TOKEN}`, `${API}/api/system/user-config/col-config/${CFG_KEY}`])
  check('全局列配置读回一致（非 localStorage 本地态）',
    cfgRead.includes('bankName') && cfgRead.includes('subjectCode'), cfgRead.replace(/\s+/g, '').slice(0, 110))
  const cfgResetFile = path.join(TMP, `bank-cfg0-${Date.now()}.json`)
  fs.writeFileSync(cfgResetFile, JSON.stringify({ value: '[]' }), 'utf8')
  try {
    curl(['-X', 'POST', '-H', `Authorization: Bearer ${TOKEN}`,
      '-H', 'Content-Type: application/json; charset=utf-8', '-d', `@${cfgResetFile}`,
      `${API}/api/system/user-config/col-config/${CFG_KEY}`])
  } finally { try { fs.unlinkSync(cfgResetFile) } catch {} }

  // ═══════════════════════════════════════════
  console.log('\n【浏览器 · 页面渲染与对标结构】')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 900 } })
  const page = await ctx.newPage()
  const pageErrors = []
  page.on('pageerror', e => pageErrors.push(e.message))

  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    if (new URL(route.request().url()).pathname.startsWith('/api/sse')) return route.continue()
    try {
      const resp = await route.fetch({ url: route.request().url().replace(APP, API) })
      await route.fulfill({ response: resp })
    } catch { await route.abort() }
  })
  await page.addInitScript(s => {
    localStorage.setItem('token', s.token)
    localStorage.setItem('tenantId', String(s.tenantId || 1))
    localStorage.setItem('tenantName', s.tenantName || '系统租户')
  }, sess)

  await page.goto(`${APP}/md/bank-account`, { waitUntil: 'domcontentloaded' })
  // Vite 首次按需编译新页面较慢，等表格真正渲染（避免把加载空白误判为缺结构）
  await page.waitForSelector('.ss-grid tbody tr.ss-row', { timeout: 120000 }).catch(() => {})
  await page.waitForTimeout(3000)
  await page.screenshot({ path: path.join(SHOT, 'bank-account-list.png') })
  check('页面无 JS 运行错误', pageErrors.length === 0, pageErrors.slice(0, 2).join(' | '))

  const bodyText = await page.locator('body').innerText()
  check('工具栏含 新增银行/刷新/打印(F8)/导出', ['新增银行', '刷新', '打印(F8)', '导出'].every(t => bodyText.includes(t)))
  const toolbarTxt = await page.locator('.toolbar-section').innerText().catch(() => '')
  check('工具栏分组：左 新增银行 / 右 刷新·打印(F8)·导出·展开收起',
    (await page.locator('.toolbar-left .ant-btn', { hasText: '新增银行' }).count()) === 1 &&
    ['刷新', '打印(F8)', '导出'].every(t => toolbarTxt.includes(t)),
    toolbarTxt.replace(/\s+/g, ' ').trim())
  const searchTxt = await page.locator('.search-section').innerText().catch(() => '')
  const searchFlat = searchTxt.replace(/\s+/g, '')
  // ⚠️ ant-design-vue 会在「两字中文按钮」间插入空格（查 询），比较前需去空白
  check('查询区含 筛选条件/查询/显示停用/显示层次结构',
    ['筛选条件', '查询', '显示停用', '显示层次结构'].every(t => searchFlat.includes(t)),
    searchTxt.replace(/\s+/g, ' ').trim())
  check('筛选条件输入框 placeholder 对齐对标', (await page.locator('.search-section input[placeholder="请输入科目编号/科目名称"]').count()) === 1)
  check('表尾含 当前路径', bodyText.includes('当前路径'))

  const headers = await page.locator('.ss-grid thead th').allInnerTexts().catch(() => [])
  const hTxt = headers.join('|').replace(/\s+/g, '')
  check('表头含对标 4 列（科目编号/科目名称/账户类型/是否用于商城线下转账收款）',
    ['科目编号', '科目名称', '账户类型', '是否用于商城线下转账收款'].every(t => hTxt.includes(t)), hTxt)

  const rowCount = await page.locator('.ss-grid tbody tr.ss-row').count()
  check('明细行渲染真实数据', rowCount >= 3, `${rowCount} 行（含 __ghost 补齐行）`)
  // 组件默认 minRows=20：不足 20 条时补 __ghost 空行（对标 ql361 每页固定 20 行）
  check('数据表按 BillDetailTable 组件默认补齐 20 行', rowCount === 20, `${rowCount} 行`)
  const rowTxt = (await page.locator('.ss-grid tbody tr.ss-row').allInnerTexts().catch(() => [])).join(' | ')
  check('行内含真实科目编号 1001/1002', rowTxt.includes('1001') && rowTxt.includes('1002'))

  // 列配置齿轮（表头 rowNo 列内）→ 个人配置 / 全局配置
  const gear = page.locator('.ss-header-settings').first()
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1500)
    await page.screenshot({ path: path.join(SHOT, 'bank-account-colcfg.png') })
    const modalTxt = await page.locator('.ant-modal-content').first().innerText().catch(() => '')
    check('列配置弹窗含 个人配置 / 全局配置 两个 Tab', modalTxt.includes('个人配置') && modalTxt.includes('全局配置'))
    const colNames = await page.locator('.col-setting-title').allInnerTexts().catch(() => [])
    const colTxt = colNames.join('|').replace(/\s+/g, '')
    check('列配置弹窗列出对标 4 列', ['科目编号', '科目名称', '账户类型', '是否用于商城线下转账收款'].every(t => colTxt.includes(t)), colNames.slice(0, 6).join(','))
    check('列配置弹窗含本系统可选列（开户行/银行账号/账户余额）', ['开户行', '银行账号', '账户余额'].every(t => colTxt.includes(t)))
    await closeAllModals(page)
  } else {
    check('列配置齿轮存在（表头 rowNo 列）', false, '未找到 .ss-header-settings')
  }
  check('列配置弹窗已关闭（不残留遮挡）', (await visibleModals(page).count()) === 0,
    `剩余可见弹窗 ${await visibleModals(page).count()}`)

  // 新增银行弹窗
  await page.locator('.toolbar-left .ant-btn', { hasText: '新增银行' }).first().click()
  await page.waitForTimeout(2500)
  await page.screenshot({ path: path.join(SHOT, 'bank-account-form.png') })
  const formTxt = await visibleModals(page).last().innerText().catch(() => '')
  check('弹窗标题为「银行信息」', formTxt.includes('银行信息'))
  check('表单含对标字段：银行编号/银行全称/账户类型/助记码/银行简称/开户行/户主名/银行账号/二维码/是否用于商城线下转账收款',
    ['银行编号', '银行全称', '账户类型', '助记码', '银行简称', '开户行', '户主名', '银行账号', '二维码', '商城线下转账收款'].every(t => formTxt.includes(t)),
    formTxt.replace(/\n+/g, ' ').slice(0, 160))
  const codeVal = await page.locator('.ant-modal-content:visible input[placeholder="自动生成，可修改"]').inputValue().catch(() => '')
  check('银行编号自动预填', /^\d{3,}$/.test(codeVal), codeVal)
  const okTxt = await page.locator('.ant-modal-content:visible .ant-btn-primary').first().innerText().catch(() => '')
  check('按钮文案为 保存(Enter) / 关闭(Esc)', okTxt.includes('保存(Enter)'), okTxt)

  // UI 新增 → 列表出现
  const uiCode = `7${String(Date.now()).slice(-6)}`
  await page.locator('.ant-modal-content:visible input[placeholder="自动生成，可修改"]').fill(uiCode)
  await page.locator('.ant-modal-content:visible input[placeholder="请输入银行全称"]').fill(`UI银行-${uiCode}`)
  await page.locator('.ant-modal-content:visible .ant-btn-primary').first().click()
  await page.waitForTimeout(3000)
  const afterCreate = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('UI 新增后列表出现新记录', afterCreate.includes(uiCode), uiCode)
  await page.screenshot({ path: path.join(SHOT, 'bank-account-created.png') })

  // UI 修改
  const rowLocator = page.locator('.ss-grid tbody tr.ss-row', { hasText: uiCode }).first()
  await rowLocator.locator('.ant-btn-link', { hasText: '修改' }).first().click()
  await page.waitForTimeout(3000)
  await page.locator('.ant-modal-content:visible input[placeholder="请输入银行全称"]').fill(`UI银行-改-${uiCode}`)
  await page.locator('.ant-modal-content:visible .ant-btn-primary').first().click()
  await page.waitForTimeout(3000)
  const afterEdit = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('UI 修改后列表显示新名称', afterEdit.includes(`UI银行-改-${uiCode}`), `UI银行-改-${uiCode}`)

  // UI 删除
  const rowLocator2 = page.locator('.ss-grid tbody tr.ss-row', { hasText: uiCode }).first()
  await rowLocator2.locator('.ant-btn-link', { hasText: '删除' }).first().click()
  await page.waitForSelector('.ant-modal-confirm-btns', { timeout: 15000 }).catch(() => {})
  await page.waitForTimeout(800)
  check('删除前弹出二次确认', (await page.locator('.ant-modal-confirm-btns').count()) > 0)
  const okBtn = page.getByRole('button', { name: '确认删除' }).last()
  if (await okBtn.count()) {
    await okBtn.click()
  } else {
    await page.locator('.ant-modal-confirm-btns button').last().click()
  }
  await page.waitForTimeout(3000)
  const afterDelete = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('UI 删除后列表不再显示', !afterDelete.includes(uiCode))
  const gone = Number(await scalar(`SELECT deleted_flag FROM finance_account WHERE subject_code=$1`, [uiCode]))
  check('UI 删除落库为逻辑删除', gone === 1, `deleted_flag=${gone}`)
  await pg.query('DELETE FROM finance_account WHERE subject_code=$1', [uiCode])

  // 显示层次结构开关
  const hCb = page.locator('.search-row .ant-checkbox-wrapper', { hasText: '显示层次结构' }).first()
  const checked = await hCb.locator('input').isChecked().catch(() => null)
  check('「显示层次结构」默认勾选（对标默认）', checked === true, String(checked))
  await hCb.click()
  await page.waitForTimeout(2500)
  await page.screenshot({ path: path.join(SHOT, 'bank-account-flat.png') })
  const flatRows = await page.locator('.ss-grid tbody tr.ss-row').count()
  check('取消勾选后仍正常渲染平铺列表', flatRows >= 3, `${flatRows} 行`)
  await hCb.click()
  await page.waitForTimeout(2000)

  // 树形逐级展开/收起 + 层级缩进（造父子数据 → 刷新 → 断言 → 清理）
  // ⚠️ 前置：上一段「显示层次结构」开关测试会取消/恢复勾选，此处必须确保已复位为勾选态，
  //    否则 showTree=0（后端不下发 level/hasChildren）→ 缩进 0px、展开收起按钮 disabled。
  const treeCb = page.locator('.search-row .ant-checkbox-wrapper', { hasText: '显示层次结构' }).first()
  if (!(await treeCb.locator('input').isChecked().catch(() => false))) {
    await treeCb.click()
    await page.waitForTimeout(2500)
  }
  check('「显示层次结构」已复位为勾选（树形断言前提）', await treeCb.locator('input').isChecked().catch(() => false))

  const tParent = apiSend('POST', '/api/erp/finance/bank-account', {
    subjectCode: `5${stamp}`, accountName: `UI树父-${stamp}`, accountType: 1,
  })
  const tChild = apiSend('POST', '/api/erp/finance/bank-account', {
    subjectCode: `5${stamp}01`, accountName: `UI树子-${stamp}`, accountType: 1, parentId: Number(tParent.data?.id),
  })
  check('树形测试数据就绪（父 + 子）', tParent.code === 200 && tChild.code === 200, `p=${tParent.data?.id}, c=${tChild.data?.id}`)
  await page.locator('.toolbar-right .ant-btn', { hasText: '刷新' }).first().click()
  await page.waitForTimeout(3000)
  let treeTxt = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('树形：默认全展开，子账户可见', treeTxt.includes(`UI树子-${stamp}`))
  const indent = await page.locator('.ss-grid tbody .name-cell').filter({ hasText: `UI树子-${stamp}` })
    .first().evaluate(el => el.style.paddingLeft).catch(() => '')
  check('树形：子账户按层级缩进 16px（对标口径）', indent === '16px', `paddingLeft=${indent}`)
  await page.locator('.toolbar-right .ant-btn').nth(1).click()
  await page.waitForTimeout(1500)
  treeTxt = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('「全部收起」后子账户隐藏', !treeTxt.includes(`UI树子-${stamp}`))
  await page.locator('.toolbar-right .ant-btn').nth(0).click()
  await page.waitForTimeout(1500)
  treeTxt = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('「全部展开」后子账户恢复', treeTxt.includes(`UI树子-${stamp}`))

  // 筛选条件回车即查询（对标：筛选条件输入框回车触发查询）
  const kwInput = page.locator('.search-section input[placeholder="请输入科目编号/科目名称"]')
  await kwInput.fill('1001')
  await kwInput.press('Enter')
  await page.waitForTimeout(2500)
  const kwTxt = await page.locator('.ss-grid tbody').innerText().catch(() => '')
  check('筛选条件回车即查询（结果集被过滤）', kwTxt.includes('1001') && !kwTxt.includes('招行'),
    kwTxt.replace(/\s+/g, ' ').slice(0, 70))
  await kwInput.fill('')
  await kwInput.press('Enter')
  await page.waitForTimeout(2500)
  await pg.query('DELETE FROM finance_account WHERE subject_code IN ($1,$2)', [`5${stamp}`, `5${stamp}01`])

  // 导出（UI → 后端真实 xlsx 下载）
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 30000 }).catch(() => null),
    page.locator('.toolbar-right .ant-btn', { hasText: '导出' }).first().click(),
  ])
  let dlSize = 0
  if (download) {
    const dlPath = path.join(TMP, `bank-ui-export-${Date.now()}.xlsx`)
    await download.saveAs(dlPath).catch(() => {})
    dlSize = fs.existsSync(dlPath) ? fs.statSync(dlPath).size : 0
    try { fs.unlinkSync(dlPath) } catch {}
    check('UI 导出下载真实 xlsx', /\.xlsx$/.test(download.suggestedFilename()) && dlSize > 3000, `${download.suggestedFilename()} ${dlSize} bytes`)
  } else {
    check('UI 导出下载真实 xlsx', false, '未触发下载事件')
  }

  // 打印(F8)：弹出打印窗口并写入渲染好的列表
  const [printWin] = await Promise.all([
    page.waitForEvent('popup', { timeout: 15000 }).catch(() => null),
    page.keyboard.press('F8'),
  ])
  if (printWin) {
    await printWin.waitForLoadState('domcontentloaded').catch(() => {})
    const printTxt = await printWin.evaluate(() => document.body.innerText).catch(() => '')
    check('打印(F8) 弹出打印页并含真实数据', printTxt.includes('银行账户') && printTxt.includes('科目编号') && /100[12]/.test(printTxt),
      printTxt.replace(/\s+/g, ' ').slice(0, 80))
    await printWin.close().catch(() => {})
  } else {
    check('打印(F8) 弹出打印页并含真实数据', false, '未捕获打印窗口')
  }

  check('全程无 JS 运行错误', pageErrors.length === 0, pageErrors.slice(0, 2).join(' | '))

  await browser.close()
  await pg.end()
  console.log(`\n════════ 银行账户金标准验收：通过 ${pass} / 失败 ${fail} ════════`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => { console.error('FATAL', e.stack || e.message); process.exit(1) })
