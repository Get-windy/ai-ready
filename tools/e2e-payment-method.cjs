/*
 * 支付方式（资料 → 支付管理 → 支付方式，菜单 80550）金标准端到端验证
 *   · API 验收：分页多条件 / 详情 / 新增 / 唯一校验 / 必填与取值校验 / 修改 /
 *               默认唯一 / 启停不污染字段 / 批量启停 / 引用保护删除 / 删除 / 导出 xlsx / 下拉
 *   · UI 验收：列表骨架（工具栏 + 查询区 + 数据表 + 经典分页）+ 表头齿轮列配置 +
 *              新增弹窗真实保存 + 行内修改 / 停用 + 导出 xlsx
 *   · DB 核对：落库终态（deleted_flag / is_default 唯一性 / 字段未被状态切换清零）
 *
 * 用法：node tools/e2e-payment-method.cjs            （默认后端 5680、前端 5656）
 *      ERP_PORT=5680 FE_URL=http://localhost:5656 node tools/e2e-payment-method.cjs
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const path = require('path')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5680)
const SHOTS = 'I:/AI-Ready/tool-results/md-payment-method'
const XLSX = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/xlsx')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = {
  host: 'localhost',
  port: 5432,
  database: 'devdb',
  user: 'devuser',
  password: 'devuser123',
}

// 专用验收账号：并行会话共用 admin 会被 sa-token 互踢（验收中途 401），见 tools/e2e-payment-method-user.sql
const E2E_USER = process.env.E2E_USER || 'e2e_paymethod'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally {
    await client.end()
  }
}

/** 幂等创建验收账号（等权 admin：role_id=1 SUPER_ADMIN + 系统租户） */
async function ensureE2EUser() {
  const exist = await dbQuery(`SELECT id FROM sys_user WHERE username = $1`, [E2E_USER])
  if (exist.length > 0) return
  const admin = await dbQuery(`SELECT id FROM sys_user WHERE username = 'admin'`)
  if (admin.length === 0) throw new Error('未找到 admin 账号，无法复制密码哈希')
  await dbQuery(
    `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                           user_type, is_super_admin, is_tenant_admin, status, data_scope)
     SELECT 2099000000000000981, 1, 0, now(), now(), $1, password, 'E2E支付方式', 'E2E支付方式',
            1, false, false, 1, 'ALL'
     FROM sys_user WHERE username = 'admin'`, [E2E_USER])
  await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                 VALUES (2099000000000000982, 2099000000000000981, 1, 1, now())`)
  await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                 VALUES (2099000000000000983, 2099000000000000981, 1, true, 1, now(), now())`)
  console.log(`已创建验收账号 ${E2E_USER}（密码同 admin）`)
}

// ══════════════ HTTP 基础设施 ══════════════
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

/** 取接口 data（后端 Result 包一层） */
function data(res) {
  return res.json ? res.json.data : null
}

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
  if (!token) {
    throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300)
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}），见 tools/e2e-payment-method-user.sql。`)
  }
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}

const STAMP = Date.now().toString().slice(-6)
const CODE_A = 'E2EPM' + STAMP
const CODE_B = 'E2EPM' + STAMP + 'B'
let ORIGINAL_DEFAULT_ID = null

// ══════════════ 一、接口验收 ══════════════
async function apiSuite() {
  console.log('\n═══ 一、接口验收 ═══')

  // 记录原始默认项，验收结束恢复
  const defaults = await dbQuery(`SELECT id FROM md_payment_method WHERE is_default = 1 AND deleted_flag = 0`)
  ORIGINAL_DEFAULT_ID = defaults[0] ? String(defaults[0].id) : null

  // 0) 基准分页
  const page0 = await api('GET', '/erp/md/payment-method/page?pageNum=1&pageSize=20')
  check('分页接口返回 records/total', Array.isArray(data(page0)?.records) && typeof data(page0)?.total !== 'undefined',
    JSON.stringify({ total: data(page0)?.total }))
  const CASH = (data(page0)?.records || []).find(r => r.methodCode === 'CASH')
  check('列表回填 methodType 原值（CASH）', !!CASH, CASH?.methodCode)

  // 1) 新增（字段齐全 + 默认入账账户回填名称）
  const accountRows = await dbQuery(`SELECT id, account_name FROM finance_account WHERE deleted_flag = 0 ORDER BY id LIMIT 1`)
  const ACCOUNT_ID = accountRows[0] ? String(accountRows[0].id) : null
  const ACCOUNT_NAME = accountRows[0] ? accountRows[0].account_name : ''

  const createRes = await api('POST', '/erp/md/payment-method', {
    methodCode: CODE_A,
    methodName: 'E2E 支付方式' + STAMP,
    methodType: 'WECHAT',
    accountId: ACCOUNT_ID,
    feeRate: 0.006,
    isDefault: 0,
    sort: 96,
    status: 1,
    remark: 'E2E 新增备注',
  })
  const created = data(createRes)
  check('新增支付方式成功', created && created.id, JSON.stringify(created).slice(0, 200))
  const idA = created?.id
  check('新增回填默认入账账户名称', !!ACCOUNT_ID && created?.accountName === ACCOUNT_NAME,
    `accountName=${created?.accountName} 期望=${ACCOUNT_NAME}`)
  check('新增费率按小数落库（0.006）', Number(created?.feeRate) === 0.006, created?.feeRate)

  // 2) 必填 / 取值校验
  const noCode = await api('POST', '/erp/md/payment-method', { methodName: '缺编码', methodType: 'CASH' })
  check('编码必填校验', noCode.json?.code !== 200, noCode.json?.message)
  const noName = await api('POST', '/erp/md/payment-method', { methodCode: CODE_B + 'N', methodType: 'CASH' })
  check('名称必填校验', noName.json?.code !== 200, noName.json?.message)
  const noType = await api('POST', '/erp/md/payment-method', { methodCode: CODE_B + 'T', methodName: '缺类型' })
  check('类型必填校验', noType.json?.code !== 200, noType.json?.message)
  const badType = await api('POST', '/erp/md/payment-method', { methodCode: CODE_B + 'X', methodName: '非法类型', methodType: 'BITCOIN' })
  check('非法类型校验', badType.json?.code !== 200, badType.json?.message)
  const badFee = await api('POST', '/erp/md/payment-method', { methodCode: CODE_B + 'F', methodName: '费率越界', methodType: 'CASH', feeRate: 1.5 })
  check('费率越界校验（>100%）', badFee.json?.code !== 200, badFee.json?.message)
  const badAccount = await api('POST', '/erp/md/payment-method', { methodCode: CODE_B + 'A', methodName: '账户不存在', methodType: 'CASH', accountId: 99999999 })
  check('默认入账账户存在性校验', badAccount.json?.code !== 200, badAccount.json?.message)
  const dup = await api('POST', '/erp/md/payment-method', { methodCode: CODE_A, methodName: '重号', methodType: 'CASH' })
  check('编码重复校验', dup.json?.code !== 200, dup.json?.message)

  // 3) 详情
  const detail = await api('GET', `/erp/md/payment-method/${idA}`)
  check('详情返回备注与排序', data(detail)?.remark === 'E2E 新增备注' && Number(data(detail)?.sort) === 96,
    JSON.stringify({ remark: data(detail)?.remark, sort: data(detail)?.sort }))
  const notFound = await api('GET', '/erp/md/payment-method/99999999')
  check('详情不存在返回非 200', notFound.json?.code !== 200, notFound.json?.message)

  // 4) 分页多条件
  const p1 = await api('GET', `/erp/md/payment-method/page?pageNum=1&pageSize=20&keyword=${CODE_A}`)
  check('按编码检索命中', Number(data(p1)?.total) === 1, `total=${data(p1)?.total}`)
  // 注意：直接拼中文/空格，rawReq 内 encodeURI 负责编码；再 encodeURIComponent 会双重编码导致检索不到
  const p2 = await api('GET', `/erp/md/payment-method/page?pageNum=1&pageSize=20&keyword=E2E 支付方式${STAMP}`)
  check('按名称检索命中', Number(data(p2)?.total) >= 1, `total=${data(p2)?.total}`)
  const p3 = await api('GET', `/erp/md/payment-method/page?pageNum=1&pageSize=20&keyword=${CODE_A}&methodType=WECHAT`)
  check('按类型过滤命中', Number(data(p3)?.total) === 1, `total=${data(p3)?.total}`)
  const p4 = await api('GET', `/erp/md/payment-method/page?pageNum=1&pageSize=20&keyword=${CODE_A}&methodType=ALIPAY`)
  check('按类型过滤排除不匹配', Number(data(p4)?.total) === 0, `total=${data(p4)?.total}`)
  const p5 = await api('GET', `/erp/md/payment-method/page?pageNum=1&pageSize=20&keyword=${CODE_A}&status=0`)
  check('按状态过滤（停用）排除启用项', Number(data(p5)?.total) === 0, `total=${data(p5)?.total}`)

  // 5) 修改（改名 + 改类型 + 费率 + 备注）
  const upd = await api('PUT', `/erp/md/payment-method/${idA}`, {
    methodCode: CODE_A,
    methodName: 'E2E 支付方式改' + STAMP,
    methodType: 'ALIPAY',
    accountId: null,
    feeRate: 0.012,
    isDefault: 0,
    sort: 95,
    status: 1,
    remark: 'E2E 修改备注',
  })
  check('修改支付方式成功', upd.json?.code === 200, upd.json?.message)
  const afterUpd = data(await api('GET', `/erp/md/payment-method/${idA}`))
  check('修改后名称/类型/费率生效', afterUpd?.methodName === 'E2E 支付方式改' + STAMP
    && afterUpd?.methodType === 'ALIPAY' && Number(afterUpd?.feeRate) === 0.012,
    JSON.stringify({ n: afterUpd?.methodName, t: afterUpd?.methodType, f: afterUpd?.feeRate }))
  check('修改后清空默认入账账户真正落库（null）', afterUpd?.accountId === null && afterUpd?.accountName === null,
    JSON.stringify({ accountId: afterUpd?.accountId, accountName: afterUpd?.accountName }))
  check('修改后备注更新', afterUpd?.remark === 'E2E 修改备注', afterUpd?.remark)
  const dupUpd = await api('PUT', `/erp/md/payment-method/${idA}`, {
    methodCode: 'CASH', methodName: '改成重号', methodType: 'CASH', feeRate: 0, sort: 1, status: 1, isDefault: 0,
  })
  check('修改时编码唯一校验（排除自身）', dupUpd.json?.code !== 200, dupUpd.json?.message)

  // 6) 默认唯一：置默认时自动清除其它默认
  const setDefault = await api('PUT', `/erp/md/payment-method/${idA}`, {
    methodCode: CODE_A, methodName: 'E2E 支付方式改' + STAMP, methodType: 'ALIPAY',
    feeRate: 0.012, isDefault: 1, sort: 95, status: 1, remark: 'E2E 修改备注',
  })
  check('置为默认成功', Number(data(setDefault)?.isDefault) === 1, data(setDefault)?.isDefault)
  const defaultCount = await dbQuery(
    `SELECT COUNT(*)::int AS c FROM md_payment_method WHERE is_default = 1 AND deleted_flag = 0`)
  check('全库默认支付方式唯一', defaultCount[0].c === 1, `count=${defaultCount[0].c}`)

  // 7) 启停（回归：不得把 feeRate / sort / remark 清零）
  const st1 = await api('PUT', `/erp/md/payment-method/${idA}/status`, { status: 0 })
  const s1 = data(st1)
  check('停用成功且状态=0', Number(s1?.status) === 0, s1?.status)
  check('停用不污染费率/排序/备注', Number(s1?.feeRate) === 0.012 && Number(s1?.sort) === 95 && s1?.remark === 'E2E 修改备注',
    JSON.stringify({ feeRate: s1?.feeRate, sort: s1?.sort, remark: s1?.remark }))
  check('停用同时清除默认标记', Number(s1?.isDefault) === 0, s1?.isDefault)
  const stBad = await api('PUT', `/erp/md/payment-method/${idA}/status`, { status: 9 })
  check('非法状态校验', stBad.json?.code !== 200, stBad.json?.message)
  const st2 = await api('PUT', `/erp/md/payment-method/${idA}/status`, { status: 1 })
  check('启用成功且状态=1', Number(data(st2)?.status) === 1, data(st2)?.status)

  // 8) 下拉（仅启用项）
  const opts = await api('GET', '/erp/md/payment-method/list')
  check('下拉接口返回启用项', Array.isArray(data(opts)) && data(opts).length >= 1, `count=${(data(opts) || []).length}`)
  check('下拉项含 methodCode/methodName（收付款单引用口径）',
    (data(opts) || []).every(o => !!o.methodCode && !!o.methodName))

  // 9) 批量启停
  const batch = await api('PUT', '/erp/md/payment-method/batch-status', { ids: [idA], status: 0 })
  check('批量停用成功', batch.json?.code === 200 && Number(data(batch)) === 1, JSON.stringify(data(batch)))
  const afterBatch = data(await api('GET', `/erp/md/payment-method/${idA}`))
  check('批量停用落库', Number(afterBatch?.status) === 0, afterBatch?.status)
  const batchBad = await api('PUT', '/erp/md/payment-method/batch-status', { ids: [], status: 1 })
  check('批量操作为空校验', batchBad.json?.code !== 200, batchBad.json?.message)
  await api('PUT', '/erp/md/payment-method/batch-status', { ids: [idA], status: 1 })

  // 10) 引用保护：造一条支付渠道引用后应拒绝删除
  await dbQuery(
    `INSERT INTO md_payment_channel (tenant_id, channel_code, channel_name, method_id, status, deleted_flag)
     VALUES (1, $1, $2, $3, 1, 0)`, ['E2ECH' + STAMP, 'E2E 渠道' + STAMP, idA])
  const blocked = await api('DELETE', `/erp/md/payment-method/${idA}`)
  check('被支付渠道引用时拒绝删除', blocked.json?.code !== 200, blocked.json?.message)
  check('拒绝删除提示含引用来源', String(blocked.json?.message || '').includes('支付渠道'), blocked.json?.message)
  const stillThere = await api('GET', `/erp/md/payment-method/${idA}`)
  check('拒绝删除后记录仍存在', stillThere.json?.code === 200 && !!data(stillThere)?.id, data(stillThere)?.methodCode)
  await dbQuery(`DELETE FROM md_payment_channel WHERE channel_code = $1`, ['E2ECH' + STAMP])

  // 11) 导出真实 xlsx
  const exp = await api('GET', `/erp/md/payment-method/export?keyword=${CODE_A}`)
  check('导出返回 xlsx（非 JSON）', exp.buf && exp.buf[0] === 0x50 && exp.buf[1] === 0x4b, `status=${exp.status} type=${exp.headers['content-type']}`)
  if (exp.buf && exp.buf[0] === 0x50) {
    fs.writeFileSync(path.join(SHOTS, 'export.xlsx'), exp.buf)
    try {
      const wb = XLSX.read(exp.buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('导出含 10 列', Object.keys(rows[0] || {}).length === 10, JSON.stringify(Object.keys(rows[0] || {})))
      check('导出内容含目标编码行', rows.some(r => String(r['编码']) === CODE_A), JSON.stringify(rows[0] || {}))
      const row = rows.find(r => String(r['编码']) === CODE_A) || {}
      check('导出费率按百分比呈现', String(row['手续费率']).includes('%'), row['手续费率'])
      check('导出类型列翻译为中文', row['类型'] === '支付宝', row['类型'])
      check('导出状态列翻译为中文', row['状态'] === '启用', row['状态'])
    } catch (e) {
      check('导出 xlsx 可解析', false, e.message)
    }
  }

  // 12) 删除（无引用）
  const del = await api('DELETE', `/erp/md/payment-method/${idA}`)
  check('无引用时删除成功', del.json?.code === 200, del.json?.message)
  const afterDel = await api('GET', `/erp/md/payment-method/${idA}`)
  check('删除后详情不可查', afterDel.json?.code !== 200, afterDel.json?.message)
  const delRow = await dbQuery(`SELECT deleted_flag FROM md_payment_method WHERE method_code = $1`, [CODE_A])
  check('删除为逻辑删除（deleted_flag=1）', delRow[0] && Number(delRow[0].deleted_flag) === 1, JSON.stringify(delRow[0]))

  // 恢复原始默认项
  if (ORIGINAL_DEFAULT_ID) {
    await dbQuery(`UPDATE md_payment_method SET is_default = 1 WHERE id = $1`, [ORIGINAL_DEFAULT_ID])
    const restored = await dbQuery(`SELECT COUNT(*)::int AS c FROM md_payment_method WHERE is_default = 1 AND deleted_flag = 0`)
    check('验收结束恢复原默认项', restored[0].c === 1, `count=${restored[0].c}`)
  }

  return { idA }
}

// ══════════════ 二、UI 验收 ══════════════
async function uiSuite() {
  console.log('\n═══ 二、UI 验收 ═══')
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  const page = await ctx.newPage()

  // antd 会给「无图标 + 恰好两个汉字」的按钮插空格（保存→「保 存」、查询→「查 询」），
  // 故一律用正则匹配空白，避免 :has-text("查询") 永远匹配不上
  page.clickBtn = (re, scope) => (scope || page).locator('button').filter({ hasText: re }).first().click()

  // /api 转发到验收实例（前端 dev server 默认代理指向别的后端）
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  /** 注入 token 打开页面（并行会话共用 admin 会被互踢，失败则重登一次） */
  async function openPage(url, waitMs = 6000) {
    for (let attempt = 0; attempt < 4; attempt++) {
      await login()
      await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
      await page.evaluate(([tk, tid]) => {
        localStorage.setItem('token', tk)
        localStorage.setItem('tenantId', String(tid))
      }, [TOKEN, 1])
      await page.goto(url, { waitUntil: 'domcontentloaded' })
      await page.waitForTimeout(waitMs)
      const kicked = page.url().includes('/login') || (await page.locator('input[type=password]').count()) > 0
      if (!kicked) return
      console.log('  [页面被互踢，自愈重登]')
    }
  }

  await openPage(`${FE}/md/payment-method`)
  const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('页面可打开（无 404/白屏）', !bodyText.includes('页面不存在') && bodyText.length > 50, page.url())

  // 工具栏
  for (const b of ['新增', '刷新', '打印(F8)', '导出', '更多']) {
    check(`工具栏含「${b}」`, bodyText.includes(b.replace(/\s+/g, '')))
  }
  // 查询区
  for (const q of ['筛选条件', '类型', '状态', '查询']) {
    check(`查询区含「${q}」`, bodyText.includes(q))
  }
  // 表头
  const headers = await page.evaluate(() => [...document.querySelectorAll('.ss-grid th')].map(t => (t.innerText || '').replace(/\s+/g, '')).filter(Boolean))
  for (const h of ['支付方式编码', '支付方式名称', '类型', '默认入账账户', '手续费率', '默认', '排序', '状态', '备注', '创建时间']) {
    check(`表头含「${h}」`, headers.some(x => x.includes(h)), headers.join('/'))
  }
  check('列表不出现「页面配置」齿轮（本页无页面配置弹窗）', !bodyText.includes('页面配置'))
  await page.screenshot({ path: path.join(SHOTS, 'ui-list.png'), fullPage: true })

  // 经典分页栏
  const pagerText = await page.evaluate(() => {
    const el = document.querySelector('.pagination-classic, .classic-pagination, .table-footer')
    return el ? el.innerText.replace(/\s+/g, '') : ''
  })
  check('底部为经典分页栏', pagerText.includes('共') || pagerText.includes('页'), pagerText.slice(0, 80))

  // 列配置齿轮（表头 rowNo 列右上角）
  const gear = page.locator('.ss-grid .th-settings-btn').first()
  check('数据表存在表头齿轮（列配置入口）', (await gear.count()) > 0)
  if (await gear.count()) {
    await gear.click()
    await page.waitForTimeout(1500)
    const colPanel = page.locator('.ant-modal-content:visible').last()
    const colPanelText = (await colPanel.innerText()).replace(/\s+/g, '')
    check('列配置弹窗含「个人配置 / 全局配置」双 Tab',
      colPanelText.includes('个人配置') && colPanelText.includes('全局配置'), colPanelText.slice(0, 120))
    check('列配置含本页列名',
      ['支付方式编码', '支付方式名称', '手续费率', '默认入账账户'].every(c => colPanelText.includes(c)),
      colPanelText.replace(/\n/g, '|').slice(0, 200))
    await page.screenshot({ path: path.join(SHOTS, 'ui-colconfig.png'), fullPage: true })
    await page.locator('.ant-modal-content:visible .ant-modal-close').last().click()
    await page.waitForTimeout(1000)
  }

  // ── 新增弹窗 ──
  const UI_CODE = 'E2EUI' + STAMP
  const UI_NAME = 'E2E UI 支付方式' + STAMP
  await page.clickBtn(/新\s*增/)
  await page.waitForTimeout(1800)
  const modal = page.locator('.ant-modal-content:visible').last()
  const modalText = (await modal.innerText()).replace(/\s+/g, '')
  check('新增弹窗标题=新增支付方式', modalText.includes('新增支付方式'), (await modal.innerText()).split('\n')[0])
  for (const f of ['编码', '名称', '类型', '默认入账账户', '手续费率(%)', '排序', '默认', '状态', '备注']) {
    check(`弹窗含字段「${f}」`, modalText.includes(f.replace(/\s+/g, '')))
  }

  await modal.locator('input[placeholder*="CASH"]').fill(UI_CODE)
  await modal.locator('input[placeholder*="现金 /"]').fill(UI_NAME)
  // 类型下拉（弹窗内第一个可见 select）
  await modal.locator('.ant-form-item:has-text("类型") .ant-select').first().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option:has-text("微信")').first().click()
  await page.waitForTimeout(400)
  // 默认入账账户下拉（接 finance_account；按名称精确选中，避免并行会话造出的临时账户抢占「第一项」）
  const accList = data(await api('GET', '/erp/finance/account/list')) || []
  const acc = accList.find(a => a.accountName === '库存现金') || accList[0]
  check('财务账户下拉数据源可用（非支付方式自身）', !!acc, JSON.stringify(acc?.accountName))
  await modal.locator('.ant-form-item:has-text("默认入账账户") .ant-select').first().click()
  await page.waitForTimeout(800)
  const accountOptionCount = await page.locator('.ant-select-dropdown:visible .ant-select-item-option').count()
  check('默认入账账户下拉有财务账户数据', accountOptionCount > 0, `count=${accountOptionCount}`)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option')
    .filter({ hasText: acc.accountName }).first().click()
  await page.waitForTimeout(300)
  // 手续费率 = 0.6%
  await modal.locator('.ant-form-item:has-text("手续费率") input').first().fill('0.6')
  await page.waitForTimeout(200)
  await page.screenshot({ path: path.join(SHOTS, 'ui-modal.png'), fullPage: true })

  // antd 会给两字中文按钮插空格（「保 存」），用 footer 结构定位更稳
  const saveBtn = modal.locator('.ant-modal-footer .ant-btn-primary').last()
  check('弹窗存在保存按钮', (await saveBtn.count()) > 0, `count=${await saveBtn.count()}`)
  await saveBtn.click()
  await page.waitForTimeout(3000)
  const afterSave = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('新增保存后列表出现该支付方式', afterSave.includes(UI_NAME.replace(/\s+/g, '')), UI_NAME)
  const dbRow = await dbQuery(`SELECT method_code, method_name, method_type, fee_rate, account_id FROM md_payment_method WHERE method_code = $1 AND deleted_flag = 0`, [UI_CODE])
  check('UI 新增真实落库', dbRow.length === 1, JSON.stringify(dbRow[0]))
  check('UI 新增类型落库 WECHAT', dbRow[0]?.method_type === 'WECHAT', dbRow[0]?.method_type)
  check('UI 新增费率按小数落库 0.0060', Number(dbRow[0]?.fee_rate) === 0.006, dbRow[0]?.fee_rate)
  check('UI 新增默认入账账户落库为所选账户', String(dbRow[0]?.account_id) === String(acc.id),
    `落库=${dbRow[0]?.account_id} 期望=${acc.id}`)
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-save.png'), fullPage: true })

  // ── 行内「修改」：点名称链接触发 ──
  await page.click(`.ss-grid a:has-text("${UI_NAME}")`)
  await page.waitForTimeout(2000)
  const editModal = page.locator('.ant-modal-content:visible').last()
  const editCode = await editModal.locator('input[placeholder*="CASH"]').inputValue()
  check('修改弹窗回填编码', editCode === UI_CODE, editCode)
  check('修改弹窗编码禁用（不可改）', await editModal.locator('input[placeholder*="CASH"]').isDisabled())
  const editName = await editModal.locator('input[placeholder*="现金 /"]').inputValue()
  check('修改弹窗回填名称', editName === UI_NAME, editName)
  const editFee = await editModal.locator('.ant-form-item:has-text("手续费率") input').first().inputValue()
  check('修改弹窗费率按百分比回填（0.60）', String(editFee).startsWith('0.6'), editFee)
  await editModal.locator('input[placeholder*="现金 /"]').fill(UI_NAME + '改')
  await page.waitForTimeout(300)
  await page.screenshot({ path: path.join(SHOTS, 'ui-edit.png'), fullPage: true })
  await editModal.locator('.ant-modal-footer .ant-btn-primary').last().click()
  await page.waitForTimeout(3000)
  const afterEdit = (await page.locator('body').innerText()).replace(/\s+/g, '')
  check('修改保存后列表更新为新名称', afterEdit.includes((UI_NAME + '改').replace(/\s+/g, '')), UI_NAME + '改')

  // ── 导出（UI 触发下载，真实 xlsx；此时记录为启用态，默认导出仅含启用项） ──
  const [download] = await Promise.all([
    page.waitForEvent('download', { timeout: 15000 }).catch(() => null),
    page.clickBtn(/导\s*出/, page.locator('.toolbar-right')),
  ])
  check('UI 导出触发文件下载', !!download, download ? download.suggestedFilename() : '未触发下载')
  if (download) {
    const p = path.join(SHOTS, 'ui-export.xlsx')
    await download.saveAs(p)
    const buf = fs.readFileSync(p)
    check('UI 导出文件为真实 xlsx', buf[0] === 0x50 && buf[1] === 0x4b, `size=${buf.length}`)
    try {
      const wb = XLSX.read(buf, { type: 'buffer' })
      const rows = XLSX.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]])
      check('UI 导出内容含表头列（编码/名称/状态）',
        rows.length > 0 && ['编码', '名称', '状态'].every(c => c in rows[0]), JSON.stringify(rows[0] || {}))
      check('UI 导出内容含刚新增的支付方式', rows.some(r => String(r['编码']) === UI_CODE),
        JSON.stringify(rows.map(r => r['编码']).slice(0, 8)))
    } catch (e) {
      check('UI 导出 xlsx 可解析', false, e.message)
    }
  }

  // ── 行内「更多 → 停用」 ──
  const row = page.locator('.ss-grid tr', { hasText: UI_NAME + '改' }).first()
  await page.clickBtn(/更\s*多/, row)
  await page.waitForTimeout(800)
  await page.locator('.ant-dropdown:visible .ant-dropdown-menu-item').filter({ hasText: /停\s*用/ }).first().click()
  await page.waitForTimeout(800)
  const confirmBox = page.locator('.ant-modal-confirm:visible').last()
  if (await confirmBox.count()) {
    await confirmBox.locator('.ant-btn-primary').click()
    await page.waitForTimeout(2500)
  }
  const statusRow = await dbQuery(`SELECT status FROM md_payment_method WHERE method_code = $1`, [UI_CODE])
  check('行内停用落库（status=0）', statusRow[0] && Number(statusRow[0].status) === 0, JSON.stringify(statusRow[0]))
  await page.screenshot({ path: path.join(SHOTS, 'ui-after-disable.png'), fullPage: true })

  // ── 查询区过滤：状态=停用 能查回该行 ──
  await page.locator('.search-area .ant-select').last().click()
  await page.waitForTimeout(600)
  await page.locator('.ant-select-dropdown:visible .ant-select-item-option').filter({ hasText: /停\s*用/ }).first().click()
  await page.clickBtn(/查\s*询/, page.locator('.search-area'))
  await page.waitForTimeout(2500)
  const afterFilter = (await page.locator('body').innerText()).replace(/\s+/g, '')
  const filterHit = afterFilter.includes((UI_NAME + '改').replace(/\s+/g, ''))
  check('查询区按状态=停用过滤生效', filterHit, filterHit ? '命中停用行' : '未命中停用行')

  // ── 行内删除（清场） ──
  const row2 = page.locator('.ss-grid tr', { hasText: UI_NAME + '改' }).first()
  await page.clickBtn(/删\s*除/, row2)
  await page.waitForTimeout(1000)
  const delBox = page.locator('.ant-modal-confirm:visible').last()
  if (await delBox.count()) {
    await page.clickBtn(/确认删除/, delBox)
    await page.waitForTimeout(2500)
  }
  const afterDelete = (await page.locator('body').innerText()).replace(/\s+/g, '')
  const stillVisible = afterDelete.includes((UI_NAME + '改').replace(/\s+/g, ''))
  check('行内删除后列表不再出现', !stillVisible, stillVisible ? '仍在列表' : '已移除')
  const delRow = await dbQuery(`SELECT deleted_flag FROM md_payment_method WHERE method_code = $1`, [UI_CODE])
  check('UI 删除为逻辑删除', delRow[0] && Number(delRow[0].deleted_flag) === 1, JSON.stringify(delRow[0]))

  await page.screenshot({ path: path.join(SHOTS, 'ui-final.png'), fullPage: true })
  await browser.close()
}

// ══════════════ 主流程 ══════════════
;(async () => {
  try {
    await ensureE2EUser()
    await login()
    console.log('登录成功')
    await apiSuite()
    try {
      await uiSuite()
    } catch (e) {
      check('UI 验收执行', false, e.message)
      console.error(e)
    }
  } catch (e) {
    check('主流程', false, e.message)
    console.error(e)
  }

  // 清场：删除本次遗留的临时数据
  try {
    await dbQuery(`DELETE FROM md_payment_method WHERE method_code LIKE 'E2EPM%' OR method_code LIKE 'E2EUI%'`)
    await dbQuery(`DELETE FROM md_payment_channel WHERE channel_code LIKE 'E2ECH%'`)
  } catch (e) {
    console.warn('清场失败（可忽略）:', e.message)
  }

  const pass = results.filter(r => r.ok).length
  console.log(`\n═══ 验收汇总：${pass}/${results.length} ═══`)
  results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  fs.writeFileSync(path.join(SHOTS, 'result.json'), JSON.stringify(results, null, 2))
  process.exit(pass === results.length ? 0 : 1)
})()
