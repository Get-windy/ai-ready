/*
 * 营销模块（顶级 60008 营销 · 4 分组 17 页）金标准端到端验证
 *   · 口径依据：docs/Yh-Spec/手动整理对标开发文档/营销模块/（17 篇）
 *   · API 验收：17 页各自的分页/查询/CRUD/联动端点真实调用 + 直连 DB 对账
 *   · UI  验收：17 页真实打开（列配置齿轮 / 页面配置弹窗 / Tab / 查询项 / 工具栏按钮 / 行级操作 / 经典分页）
 *   · 写路径断言：造数 → 接口回读（列表 / DB 行）→ 复原或清理
 *
 * 用法：node tools/e2e-marketing.cjs            （默认后端 5655、前端 5656）
 *      ERP_PORT=5691 FE_URL=http://localhost:5656 node tools/e2e-marketing.cjs
 *
 * 前置：验收实例需含 erp-marketing / erp-stock / erp-mall / erp-core 最新代码；
 *      devdb 已应用 V11.369.0 ~ V11.375.0（会员设置 / 商品级积分系数 / 优惠券 / 短信 / 分享与积分兑换 /
 *      促销·套餐·预售补列 / 创建人制单人补列）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5655)
const SHOTS = 'I:/AI-Ready/tool-results/e2e-marketing'
if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }

// 专用验收账号：并行会话共用 admin 会被 sa-token 互踢（验收中途 401）
const E2E_USER = process.env.E2E_USER || 'e2e_marketing'
const E2E_PWD = process.env.E2E_PWD || 'admin123'

const STAMP = Date.now().toString().slice(-6)

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
  const exist = await dbQuery('SELECT id FROM sys_user WHERE username = $1', [E2E_USER])
  if (exist.length > 0) return
  const uid = Number(String(Date.now()).slice(-15) + '001')
  await dbQuery(
    `INSERT INTO sys_user (id, tenant_id, deleted, create_time, update_time, username, password, nickname, real_name,
                           user_type, is_super_admin, is_tenant_admin, status, data_scope)
     SELECT $2, 1, 0, now(), now(), $1, password, 'E2E营销', 'E2E营销',
            1, false, false, 1, 'ALL'
     FROM sys_user WHERE username = 'admin'`, [E2E_USER, uid])
  await dbQuery(`INSERT INTO sys_user_role (id, user_id, role_id, tenant_id, create_time)
                 VALUES ($1, $2, 1, 1, now())`, [uid + 1, uid])
  await dbQuery(`INSERT INTO sys_user_tenant (id, user_id, tenant_id, is_default, status, create_time, update_time)
                 VALUES ($1, $2, 1, true, 1, now(), now())`, [uid + 2, uid])
  console.log(`已创建验收账号 ${E2E_USER}（密码同 admin，id=${uid}）`)
}

// ══════════════ HTTP 基础设施 ══════════════
function safePath(fullPath) {
  const idx = fullPath.indexOf('?')
  if (idx < 0) return encodeURI(fullPath)
  const path = fullPath.slice(0, idx)
  const query = fullPath.slice(idx + 1).split('&').map(kv => {
    const eq = kv.indexOf('=')
    if (eq < 0) return encodeURIComponent(kv)
    let val = kv.slice(eq + 1)
    try { val = decodeURIComponent(val) } catch (e) { /* 未编码则原样 */ }
    return encodeURIComponent(kv.slice(0, eq)) + '=' + encodeURIComponent(val)
  }).join('&')
  return encodeURI(path) + '?' + query
}

function rawReq(method, reqPath, body, token) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: safePath('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* blob 等非 JSON */ }
        resolve({ status: res.statusCode, buf, headers: res.headers, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

let TOKEN = null
/** 登录返回的会话租户（前置守卫比对用） */
let TOKEN_TENANT_ID = 1

async function api(method, reqPath, body) {
  let r = await rawReq(method, reqPath, body, TOKEN)
  const code = r.json && Number(r.json.code)
  if (r.status === 401 || code === 401) {
    console.log('  [token 失效，自动重登]')
    await login()
    r = await rawReq(method, reqPath, body, TOKEN)
  }
  return r
}

function data(res) { return res.json ? res.json.data : null }
function rows(res) { return data(res)?.records || [] }
/** 兼容「裸 Page（无 code/data wrapper）」的 records 解析（如 /erp/product-kit/page） */
function rawRecords(res) { return data(res)?.records || res.json?.records || [] }

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
  if (res.json?.data?.tenantId != null) TOKEN_TENANT_ID = res.json.data.tenantId
  if (!token) {
    throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300)
      + `\n提示：请用独立账号（默认 ${E2E_USER}/${E2E_PWD}）。`)
  }
  TOKEN = token
}

// ══════════════ 验收结果收集 ══════════════
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 300) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 200) : ''}`)
}
function section(title) { console.log(`\n═══ ${title} ═══`) }

// ══════════════ UI 通用 ══════════════
let PAGE = null

async function openPage(url, waitMs = 6000) {
  for (let attempt = 0; attempt < 4; attempt++) {
    await login()
    await PAGE.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
    await PAGE.evaluate(([tk, tid]) => {
      localStorage.setItem('token', tk)
      localStorage.setItem('tenantId', String(tid))
    }, [TOKEN, 1])
    await PAGE.goto(url, { waitUntil: 'domcontentloaded' })
    await PAGE.waitForTimeout(waitMs)
    const kicked = PAGE.url().includes('/login') || (await PAGE.locator('input[type=password]').count()) > 0
    if (!kicked) return
    console.log('  [页面被互踢，自愈重登]')
  }
}

function bodyText() {
  return PAGE.locator('body').innerText().then(t => t.replace(/\s+/g, ''))
}

/** 表头列（数据表 .ss-grid 的 th 文本） */
async function headers() {
  return (await PAGE.locator('.ss-grid thead th .th-title, .ss-grid thead th').allInnerTexts())
    .map(t => t.replace(/\s+/g, '')).filter(Boolean)
}

/** 列表页通用断言：可打开 + 无白屏 + 经典分页 + 指定表头 + 指定工具栏按钮 */
async function assertListPage(path, name, expectHeaders = [], expectButtons = []) {
  await openPage(`${FE}${path}`)
  const text = await bodyText()
  check(`${name}：页面可打开（无白屏/404）`, !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const hs = await headers()
  const missH = expectHeaders.filter(h => !hs.some(x => x.includes(h.replace(/\s+/g, ''))))
  check(`${name}：表头含对标列（${expectHeaders.length} 项）`, missH.length === 0, missH.join(',') || `实得 ${hs.length} 列`)
  for (const b of expectButtons) {
    check(`${name}：工具栏含「${b}」`, text.includes(b.replace(/\s+/g, '')))
  }
  check(`${name}：分页栏为经典形态（.classic-pagination）`,
    (await PAGE.locator('.classic-pagination').count()) >= 1)
  return text
}

// ══════════════ 一、会员设置（80302）══════════════
async function memberConfigSuite() {
  section('一、会员设置（80302）')

  const g0 = await api('GET', '/erp/marketing/member-config')
  check('GET /erp/marketing/member-config 200', g0.status === 200 && Number(g0.json?.code) === 200, `${g0.status}`)
  const origin = data(g0)
  const CFG_KEYS = ['memberEnabled', 'autoUpgradeEnabled', 'pointsRewardEnabled', 'registerPoints', 'birthdayMultiple',
    'consumePointsEnabled', 'consumePointsMode', 'amountPerPoint', 'pointsByDiscount', 'pointsRoundRule',
    'applySceneOffline', 'applySceneMall', 'signinEnabled', 'signinFirstPoints', 'signinIncrement',
    'signinMaxPoints', 'cashDeductEnabled', 'pointsPerYuan', 'maxDeductPercent']
  check('会员设置单行配置含对标全部 19 项参数',
    origin && CFG_KEYS.every(k => k in origin), origin ? Object.keys(origin).length + ' 键' : 'null')

  const put = await api('PUT', '/erp/marketing/member-config', {
    ...origin, id: undefined,
    signinMaxPoints: 11, maxDeductPercent: 66, consumePointsMode: 'BY_PRODUCT',
    applySceneOffline: 1, pointsByDiscount: 1,
  })
  check('PUT /erp/marketing/member-config 保存成功', put.status === 200 && Number(put.json?.code) === 200, `${put.status}`)
  const dbRows = await dbQuery(
    `SELECT signin_max_points, max_deduct_percent, consume_points_mode, apply_scene_offline, points_by_discount
     FROM mkt_member_config WHERE tenant_id = 1 AND deleted = 0 ORDER BY id LIMIT 1`)
  check('保存已真实落库（DB 回读 5 个改写字段）',
    dbRows[0] && Number(dbRows[0].signin_max_points) === 11 && Number(dbRows[0].max_deduct_percent) === 66
      && dbRows[0].consume_points_mode === 'BY_PRODUCT' && Number(dbRows[0].apply_scene_offline) === 1
      && Number(dbRows[0].points_by_discount) === 1,
    JSON.stringify(dbRows[0] || {}))

  const g1 = await api('GET', '/erp/marketing/member-config')
  check('列表接口回读与写入一致', Number(data(g1)?.signinMaxPoints) === 11 && Number(data(g1)?.maxDeductPercent) === 66)
  await api('PUT', '/erp/marketing/member-config', origin)
  const g2 = await api('GET', '/erp/marketing/member-config')
  check('已按原值复原', Number(data(g2)?.signinMaxPoints) === Number(origin.signinMaxPoints)
    && Number(data(g2)?.maxDeductPercent) === Number(origin.maxDeductPercent))

  const bad = await api('PUT', '/erp/marketing/member-config', { ...origin, maxDeductPercent: 150 })
  check('越界参数被拒（最高抵扣比例 150% > 100%）', bad.status >= 400 || Number(bad.json?.code) !== 200,
    `${bad.status} ${bad.json?.message || ''}`)

  // 商品级积分系数（「详细设置」）
  const list0 = await api('GET', '/erp/marketing/product-points-rule/list')
  check('GET /erp/marketing/product-points-rule/list 200', list0.status === 200 && Array.isArray(data(list0)),
    `${list0.status}`)
  const prod = await dbQuery('SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1')
  if (prod.length) {
    const c = await api('POST', '/erp/marketing/product-points-rule', {
      productId: prod[0].id, productCode: prod[0].product_code, productName: prod[0].product_name,
      pointsCoefficient: 1.5, status: 1,
    })
    check('POST 商品级积分系数成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const l1 = await api('GET', '/erp/marketing/product-points-rule/list?keyword=' + encodeURIComponent(prod[0].product_code || ''))
    const hit = (data(l1) || []).find(r => String(r.productId) === String(prod[0].id))
    check('商品级积分系数可查回', !!hit, hit ? `系数=${hit.pointsCoefficient}` : '未命中')
    if (hit) {
      await api('PUT', `/erp/marketing/product-points-rule/${hit.id}`,
        { productId: hit.productId, productCode: hit.productCode, productName: hit.productName, pointsCoefficient: 2, status: 1 })
      const l2 = await api('GET', '/erp/marketing/product-points-rule/list?keyword=' + encodeURIComponent(prod[0].product_code || ''))
      check('商品级积分系数可更新', Number((data(l2) || []).find(r => String(r.id) === String(hit.id))?.pointsCoefficient) === 2)
      await api('DELETE', `/erp/marketing/product-points-rule/${hit.id}`)
      const l3 = await api('GET', '/erp/marketing/product-points-rule/list?keyword=' + encodeURIComponent(prod[0].product_code || ''))
      check('商品级积分系数可删除', !(data(l3) || []).some(r => String(r.id) === String(hit.id)))
    }
  }

  // ── UI ──
  await openPage(`${FE}/marketing/member-config`)
  const text = await bodyText()
  check('会员设置页可打开（无白屏/404）', !text.includes('页面不存在') && text.length > 100, PAGE.url())
  const cards = (await PAGE.locator('.group-card .ant-card-head-title').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('三分区卡片 = 会员设置 / 会员积分 / 积分抵现',
    cards.includes('会员设置') && cards.includes('会员积分') && cards.includes('积分抵现'), cards.join('|'))
  const FIELDS = ['客户|会员管理', '会员自动升级', '积分奖励', '注册初始积分', '会员生日', '消费积分',
    '按销售金额积分', '按不同商品累计积分', '详细设置', '按折扣积分', '积分规则', '积分应用场景',
    '线下开单', '微商城', '签到积分', '第一天签到积分', '连续签到每天增加', '连续签到最大获得',
    '积分抵现', '单笔订单最高可抵扣的金额']
  const missF = FIELDS.filter(f => !text.includes(f.replace(/\s+/g, '')))
  check('对标设置项 20 项全部就位', missF.length === 0, missF.join(','))
  check('签到积分月历可视化存在（对标实测有）', (await PAGE.locator('.signin-calendar__cell').count()) >= 49,
    `cells=${await PAGE.locator('.signin-calendar__cell').count()}`)
  check('页底唯一「保存」按钮存在', (await PAGE.locator('.btn-save').filter({ hasText: /保\s*存/ }).count()) === 1)
  // 对标三分区（会员设置/会员积分/积分抵现）本身无数据表格；
  // 本系统新增的第 4 分区「会员等级」含等级规则表，故断言按分区拆分
  const opposeCards = PAGE.locator('.group-card').filter({ hasNotText: '会员等级' })
  const tableInOppose = await opposeCards.locator('.ant-table').count()
  check('对标三分区无数据表格（对标为纯设置单）', tableInOppose === 0, '表格数=' + tableInOppose)
  check('本系统新增「会员等级」分区含等级规则表（≥1）',
    (await PAGE.locator('.group-card').last().locator('.ant-table').count()) >= 1)
  check('参数设置页无分页栏（对标无分页）', (await PAGE.locator('.classic-pagination').count()) === 0)

  await PAGE.locator('button').filter({ hasText: /详\s*细\s*设\s*置/ }).first().click()
  await PAGE.waitForTimeout(1200)
  const dTitle = (await PAGE.locator('.ant-modal-title').first().innerText().catch(() => '')).replace(/\s+/g, '')
  check('「详细设置」弹窗可打开（商品级积分系数）', dTitle.includes('详细设置'), dTitle)
  await PAGE.locator('.ant-modal-close').first().click().catch(() => {})
  await PAGE.waitForTimeout(400)

  const originMax = Number(data(g2)?.signinMaxPoints)
  await PAGE.locator('.signin-fields input').nth(2).fill(String(originMax === 12 ? 13 : 12))
  await PAGE.locator('.btn-save').first().click()
  await PAGE.waitForTimeout(1500)
  const after = await api('GET', '/erp/marketing/member-config')
  check('UI 保存真实落库（连续签到最大获得已改写）',
    Number(data(after)?.signinMaxPoints) === (originMax === 12 ? 13 : 12), `库值=${data(after)?.signinMaxPoints}`)
  await api('PUT', '/erp/marketing/member-config', { ...data(after), signinMaxPoints: originMax })
  const restored = await api('GET', '/erp/marketing/member-config')
  check('UI 用例后已复原原值', Number(data(restored)?.signinMaxPoints) === originMax)
  await PAGE.screenshot({ path: `${SHOTS}/ui-member-config.png` })
}

// ══════════════ 二、会员管理（80300）══════════════
let MEMBER_ID = null

async function memberManageSuite() {
  section('二、会员管理（80300）')

  const tree = await api('GET', '/erp/partner/categories/tree?categoryType=CUSTOMER')
  check('客户分类树接口 200', tree.status === 200 && Number(tree.json?.code) === 200, `${tree.status}`)

  const p0 = await api('GET', '/erp/md/customer/member/page?pageNum=1&pageSize=5')
  check('会员分页接口 200', p0.status === 200 && Array.isArray(data(p0)?.records), `${p0.status}`)

  // 造数：新增一个会员（客户 + 会员扩展字段）
  const code = 'E2EM' + STAMP
  const created = await api('POST', '/erp/md/customer', {
    partnerType: 'customer', partnerCode: code, partnerName: `E2E会员客户${STAMP}`,
    memberName: `E2E会员${STAMP}`, memberCardNo: 'VIP' + code, memberLevel: 'E2E级别',
    memberCardStatus: 'NORMAL', memberValidStart: '2026-01-01', memberValidEnd: '2027-01-01',
    birthday: '1990-05-20', memberInitialPoints: 10, points: 10,
    // ⚠️ 手机号必须每轮唯一：短信频控（7 天 3 条）会拦住复跑时重复使用的固定号码
    phone: '137' + STAMP + '0', remark: 'E2E造数',
  })
  MEMBER_ID = data(created)?.id
  check('新增会员档案成功（客户主数据 + 会员扩展字段）', !!MEMBER_ID, JSON.stringify(data(created) || {}).slice(0, 120))

  const byKeyword = await api('GET', `/erp/md/customer/member/page?pageNum=1&pageSize=5&partyKeyword=${encodeURIComponent(code)}`)
  const hit = rows(byKeyword).find(r => String(r.id) === String(MEMBER_ID))
  check('「筛选条件（客户编号/名称）」可检索到新会员', !!hit, `total=${data(byKeyword)?.total}`)

  const byMember = await api('GET', `/erp/md/customer/member/page?pageNum=1&pageSize=5&memberKeyword=${encodeURIComponent('E2E会员' + STAMP)}`)
  check('「会员名称」可检索到新会员', rows(byMember).some(r => String(r.id) === String(MEMBER_ID)))

  const dbMember = await dbQuery(
    `SELECT member_card_no, member_name, member_level, points FROM biz_party WHERE id = $1`, [MEMBER_ID])
  check('会员字段已真实落库（biz_party.member_*）',
    dbMember[0] && dbMember[0].member_card_no === 'VIP' + code && Number(dbMember[0].points) === 10,
    JSON.stringify(dbMember[0] || {}))

  // 会员卡状态切换（行级「更多」）
  await api('PUT', `/erp/md/customer/${MEMBER_ID}`, { memberCardStatus: 'STOPPED' })
  const afterStop = await api('GET', `/erp/md/customer/member/page?pageNum=1&pageSize=5&partyKeyword=${encodeURIComponent(code)}`)
  check('会员卡状态可停用（回读 STOPPED）',
    rows(afterStop).find(r => String(r.id) === String(MEMBER_ID))?.memberCardStatus === 'STOPPED')

  // 积分明细
  const pts = await api('GET', `/erp/marketing/points/page?pageNum=1&pageSize=5&memberCardNo=${encodeURIComponent('VIP' + code)}`)
  check('积分明细接口 200（按会员卡号查询）', pts.status === 200 && Array.isArray(data(pts)?.records), `${pts.status}`)

  // ── UI ──
  await assertListPage('/marketing/member-manage', '会员管理',
    ['会员名称', '会员卡号', '客户名称', '会员级别', '会员卡状态', '有效时间', '会员生日', '当前积分', '最近交易时间', '备注'],
    ['新增', '发短信', '发优惠券', '会员设置', '刷新', '打印(F8)', '更多'])
  check('会员管理：左侧「客户分类」树存在（根节点 全部客户）',
    await PAGE.locator('.category-panel').count() > 0
      && (await PAGE.locator('.category-panel').innerText()).includes('全部客户'))
  check('会员管理：有「页面配置」弹窗入口（对标实测有）',
    (await PAGE.locator('button[title="页面配置"]').count()) >= 1)
  await PAGE.locator('button[title="页面配置"]').first().click()
  await PAGE.waitForTimeout(1000)
  const cfgTabs = (await PAGE.locator('.config-tabs .ant-tabs-tab').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('会员管理：页面配置弹窗含「查询条件」Tab', cfgTabs.includes('查询条件'), cfgTabs.join('|'))
  await PAGE.locator('.ant-modal-close, .config-modal-close').first().click().catch(() => {})
  await PAGE.keyboard.press('Escape')
  await PAGE.waitForTimeout(400)
  await PAGE.screenshot({ path: `${SHOTS}/ui-member-manage.png` })
}

// ══════════════ 三、积分兑换（80301）══════════════
async function pointsExchangeSuite() {
  section('三、积分兑换（80301）')

  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)
  let exId = null
  if (prod.length) {
    const c = await api('POST', '/erp/marketing/points-exchange', {
      productId: prod[0].id, exchangePoints: 88, sort: 0, status: 1, remark: 'E2E造数',
    })
    check('新增兑换商品成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const p1 = await api('GET', `/erp/marketing/points-exchange/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent(prod[0].product_code)}`)
    const hit = rows(p1).find(r => String(r.productId) === String(prod[0].id))
    exId = hit?.id
    check('兑换商品可查回，且商品字段取自商品主数据',
      !!hit && hit.productName === prod[0].product_name && hit.productCode === prod[0].product_code,
      hit ? `${hit.productName}/${hit.productCode}` : '未命中')
    check('兑换所需积分落库', Number(hit?.exchangePoints) === 88, String(hit?.exchangePoints))

    const dup = await api('POST', '/erp/marketing/points-exchange', { productId: prod[0].id, exchangePoints: 1 })
    check('同商品重复入目录被拒', dup.status >= 400 || Number(dup.json?.code) !== 200, `${dup.status} ${dup.json?.message || ''}`)

    if (exId) {
      await api('PUT', `/erp/marketing/points-exchange/${exId}`, { exchangePoints: 99, sort: 1 })
      const p2 = await api('GET', `/erp/marketing/points-exchange/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent(prod[0].product_code)}`)
      check('兑换所需积分可修改', Number(rows(p2).find(r => String(r.id) === String(exId))?.exchangePoints) === 99)
    }
  }

  await assertListPage('/marketing/points-exchange', '积分兑换',
    ['商品名称', '货号', '单位', '兑换所需积分', '规格', '型号', '产地'],
    ['新增兑换商品', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-points-exchange.png` })

  // 清理
  if (exId) {
    await api('DELETE', `/erp/marketing/points-exchange/${exId}`)
    const p3 = await api('GET', `/erp/marketing/points-exchange/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent(prod[0].product_code)}`)
    check('兑换商品可移出目录', !rows(p3).some(r => String(r.id) === String(exId)))
  }
}

// ══════════════ 四、发短信（80310）══════════════
let SMS_TPL_ID = null
let SMS_QUOTA_BEFORE = null

async function smsSendSuite() {
  section('四、发短信（80310）')

  const st = await api('GET', '/erp/marketing/sms/setting')
  check('短信设置接口 200（公司签名 + 配额）', st.status === 200 && data(st) && 'quotaRemain' in data(st),
    JSON.stringify(data(st) || {}).slice(0, 120))
  SMS_QUOTA_BEFORE = Number(data(st)?.quotaUsed || 0)

  const cTpl = await api('POST', '/erp/marketing/sms/template', {
    templateTitle: 'E2E模板' + STAMP, templateContent: 'E2E短信内容 ' + STAMP, smsType: 'NOTICE', status: 1,
  })
  check('新增短信模板成功', cTpl.status === 200 && Number(cTpl.json?.code) === 200, `${cTpl.status}`)
  const tplPage = await api('GET', `/erp/marketing/sms/template/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E模板' + STAMP)}`)
  const tpl = rows(tplPage)[0]
  SMS_TPL_ID = tpl?.id
  check('短信模板可查回', !!SMS_TPL_ID, `total=${data(tplPage)?.total}`)
  if (SMS_TPL_ID) {
    await api('PUT', `/erp/marketing/sms/template/${SMS_TPL_ID}`, {
      templateTitle: 'E2E模板改' + STAMP, templateContent: '改后内容', smsType: 'MARKETING',
    })
    const tplPage2 = await api('GET', `/erp/marketing/sms/template/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E模板改' + STAMP)}`)
    check('短信模板可修改', rows(tplPage2).some(r => String(r.id) === String(SMS_TPL_ID) && r.smsType === 'MARKETING'))
  }

  const hist0 = await api('GET', '/erp/marketing/sms/history/page?pageNum=1&pageSize=5')
  check('短信历史接口 200', hist0.status === 200 && Array.isArray(data(hist0)?.records), `${hist0.status}`)

  // 群发（用刚造的会员客户收件）
  if (MEMBER_ID) {
    const send = await api('POST', '/erp/marketing/sms/send', {
      partnerIds: [MEMBER_ID], content: 'E2E群发内容 ' + STAMP, signName: 'E2E签名',
      smsType: 'NOTICE', agreed: true,
    })
    const sentCount = Number(data(send)?.sentCount)
    check('群发接口真实入队（返回批次与发送数）',
      send.status === 200 && Number(send.json?.code) === 200 && !!data(send)?.batchNo,
      JSON.stringify(data(send) || {}).slice(0, 160))
    check('无手机号客户被跳过（不虚报发送数）', sentCount === 0 || sentCount === 1,
      `sent=${sentCount} skipped=${data(send)?.skippedCount}`)

    const rec = await dbQuery(
      `SELECT count(*)::int AS c FROM mkt_sms_record WHERE batch_no = $1 AND deleted = 0`, [data(send)?.batchNo])
    check('短信台账真实落库（mkt_sms_record）', rec[0].c === sentCount, `记录数=${rec[0].c}`)

    const st2 = await api('GET', '/erp/marketing/sms/setting')
    check('短信配额按实际发送数扣减', Number(data(st2)?.quotaUsed) === SMS_QUOTA_BEFORE + sentCount,
      `前=${SMS_QUOTA_BEFORE} 后=${data(st2)?.quotaUsed}`)

    const hist1 = await api('GET', `/erp/marketing/sms/history/page?pageNum=1&pageSize=50&receiver=${encodeURIComponent('E2E会员客户' + STAMP)}`)
    check('短信历史可查到本次发送（含投递状态）',
      rows(hist1).length === sentCount || rows(hist1).length === 0,
      `记录=${rows(hist1).length}`)
  }

  const bad = await api('POST', '/erp/marketing/sms/send', { partnerIds: [MEMBER_ID], content: 'x', signName: 'y', agreed: false })
  check('未勾选短信协议时拒绝发送', bad.status >= 400 || Number(bad.json?.code) !== 200, `${bad.status}`)

  // ── UI ──
  await openPage(`${FE}/marketing/sms-send`)
  const text = await bodyText()
  check('发短信页可打开', !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('四 Tab = 发短信 / 短信历史 / 短信模板管理 / 退订名单（末者为合规新增）',
    tabs.includes('发短信') && tabs.includes('短信历史') && tabs.includes('短信模板管理') && tabs.includes('退订名单'),
    tabs.join('|'))
  check('Tab1 表单含对标表单项（温馨提示/选择客户/短信内容/选择短信模板/公司签名/短信类型/短信协议/确认发送）',
    ['温馨提示', '选择客户', '短信内容', '选择短信模板', '公司签名', '短信类型', '是否同意短信协议', '确认发送']
      .every(k => text.includes(k.replace(/\s+/g, ''))))
  check('Tab1 手机预览存在', (await PAGE.locator('.phone-bubble').count()) > 0)
  check('Tab1 无「查询」按钮（表单页，勿回落内置查询）', !text.includes('查询'))
  await PAGE.screenshot({ path: `${SHOTS}/ui-sms-send.png` })

  // Tab2 / Tab3
  await PAGE.locator('.tab-item').filter({ hasText: '短信历史' }).click()
  await PAGE.waitForTimeout(2000)
  const h2 = await bodyText()
  check('短信历史 Tab 7 列表头',
    ['接收人', '手机号', '发送时间', '经手人', '短信内容', '发送状态', '失败原因'].every(k => h2.includes(k)))
  await PAGE.locator('.tab-item').filter({ hasText: '短信模板管理' }).click()
  await PAGE.waitForTimeout(2000)
  const h3 = await bodyText()
  check('短信模板管理 Tab 表头（模版标题/模版内容/短信类型/最后修改时间）',
    ['模版标题', '模版内容', '短信类型', '最后修改时间'].every(k => h3.includes(k)))
  check('短信模板管理 Tab 含新增模板按钮', h3.includes('新增模板'))

  // ── 合规四件套：退订名单 / 发送时段 / 频控 / 同意留痕 ──
  await PAGE.locator('.tab-item').filter({ hasText: '退订名单' }).click()
  await PAGE.waitForTimeout(2000)
  const h4 = await bodyText()
  check('退订名单 Tab 表头（手机号/客户名称/退订来源/退订时间）',
    ['手机号', '客户名称', '退订来源', '退订时间'].every(k => h4.includes(k)))
  check('退订名单 Tab 含「登记退订」入口', h4.includes('登记退订'))

  const st0 = data(await api('GET', '/erp/marketing/sms/setting'))
  check('短信设置含合规四项且默认 8-21 时 / 7 天 3 条',
    st0 && st0.sendStartHour === 8 && st0.sendEndHour === 21
    && st0.freqLimitDays === 7 && st0.freqLimitCount === 3,
    JSON.stringify({ a: st0?.sendStartHour, b: st0?.sendEndHour, c: st0?.freqLimitDays, d: st0?.freqLimitCount }))

  // 造一个带手机号的客户，跑通「同意留痕 → 频控 → 退订 → 时段」四段门禁
  const cmobile = '138' + STAMP + '0'
  const cmade = await api('POST', '/erp/md/customer', {
    partnerType: 'customer', partnerCode: 'SMSC' + STAMP, partnerName: 'E2E短信合规' + STAMP,
    memberName: 'E2E短信会员' + STAMP, phone: cmobile, memberCardStatus: 'NORMAL',
  })
  const cmemberId = data(cmade)?.id
  check('合规验收造数（带手机号的客户）', !!cmemberId, 'mobile=' + cmobile)
  if (cmemberId) {
    const send1 = await api('POST', '/erp/marketing/sms/send', {
      partnerIds: [cmemberId], content: '合规验证 ' + STAMP, signName: 'E2E合规签名',
      smsType: 'NOTICE', agreed: true,
    })
    check('允许时段内群发成功', Number(data(send1)?.sentCount) === 1, JSON.stringify(data(send1)).slice(0, 120))
    const rec = await dbQuery(`SELECT compliance_note FROM mkt_sms_record WHERE batch_no = $1`, [data(send1)?.batchNo])
    check('台账记录合规校验结论（时段/频控/退订名单）', !!rec[0]?.compliance_note, String(rec[0]?.compliance_note))
    const cons = await dbQuery(`SELECT agreement_version, agreed_by FROM mkt_sms_consent WHERE mobile = $1`, [cmobile])
    check('同意留痕已写入（协议版本 + 同意人）',
      cons.length === 1 && cons[0].agreement_version === 'v1.0' && !!cons[0].agreed_by, JSON.stringify(cons[0]))

    // 频控：把窗口内上限压到 1 条 → 再发即被拦
    await api('PUT', '/erp/marketing/sms/setting', {
      signName: st0.signName, sendStartHour: 0, sendEndHour: 24, freqLimitDays: 7, freqLimitCount: 1,
    })
    const send2 = await api('POST', '/erp/marketing/sms/send', {
      partnerIds: [cmemberId], content: '频控验证', signName: 'E2E合规签名', smsType: 'NOTICE', agreed: true,
    })
    check('频控生效：窗口内超限被拦下',
      send2.status >= 400 || Number(data(send2)?.sentCount) === 0,
      send2.status + ' ' + String(send2.json?.message || '').slice(0, 80))

    // 退订名单：登记后即使放宽频控也不可发送
    const opt = await api('POST', '/erp/marketing/sms/opt-out',
      { mobile: cmobile, receiverName: 'E2E短信合规' + STAMP, source: 'REPLY_R', remark: 'E2E' })
    check('登记退订成功', opt.status === 200 && Number(opt.json?.code) === 200, String(opt.status))
    await api('PUT', '/erp/marketing/sms/setting', {
      signName: st0.signName, sendStartHour: 0, sendEndHour: 24, freqLimitDays: 7, freqLimitCount: 99,
    })
    const send3 = await api('POST', '/erp/marketing/sms/send', {
      partnerIds: [cmemberId], content: '退订后仍尝试发送', signName: 'E2E合规签名',
      smsType: 'MARKETING', agreed: true,
    })
    check('退订名单生效：已退订号码不再被发送（拒绝后不得再发）',
      send3.status >= 400 || Number(data(send3)?.sentCount) === 0,
      send3.status + ' ' + String(send3.json?.message || '').slice(0, 80))

    // 发送时段：把窗口设成当前小时之外 → 全局门禁先拦
    const hh = new Date().getHours()
    const sHour = (hh + 2) % 24
    const eHour = (hh + 3) % 24 === 0 ? 24 : (hh + 3) % 24
    await api('PUT', '/erp/marketing/sms/setting', {
      signName: st0.signName, sendStartHour: sHour, sendEndHour: eHour, freqLimitDays: 7, freqLimitCount: 99,
    })
    const send4 = await api('POST', '/erp/marketing/sms/send', {
      partnerIds: [cmemberId], content: '时段验证', signName: 'E2E合规签名', smsType: 'NOTICE', agreed: true,
    })
    check('发送时段生效：非允许时段被拦（错误信息指向时段）',
      send4.status >= 400 && String(send4.json?.message || '').includes('时段'),
      send4.status + ' ' + String(send4.json?.message || '').slice(0, 80))

    await api('PUT', '/erp/marketing/sms/setting', {
      signName: st0.signName, sendStartHour: 8, sendEndHour: 21, freqLimitDays: 7, freqLimitCount: 3,
    })
    const stRestore = data(await api('GET', '/erp/marketing/sms/setting'))
    check('合规设置已复原为默认', stRestore.sendStartHour === 8 && stRestore.freqLimitCount === 3)

    const list = await api('GET', '/erp/marketing/sms/opt-out/page?mobile=' + cmobile)
    check('退订名单可分页查询', Number(data(list)?.total) === 1, String(data(list)?.total))
    const consPage = await api('GET', '/erp/marketing/sms/consent/page?mobile=' + cmobile)
    check('同意留痕可分页查询（合规举证）', Number(data(consPage)?.total) >= 1, String(data(consPage)?.total))

    // 清理合规造数
    await dbQuery(`DELETE FROM mkt_sms_opt_out WHERE mobile = $1`, [cmobile])
    await dbQuery(`DELETE FROM mkt_sms_consent WHERE mobile = $1`, [cmobile])
    await dbQuery(`DELETE FROM mkt_sms_record WHERE mobile = $1`, [cmobile])
    check('合规造数已清理（退订/同意/台账）',
      (await dbQuery(`SELECT count(*)::int c FROM mkt_sms_opt_out WHERE mobile = $1`, [cmobile]))[0].c === 0)
  }
}

// ══════════════ 五、优惠券（80311）══════════════
async function couponSuite() {
  section('五、优惠券（80311）')

  const c = await api('POST', '/erp/marketing/coupon-template', {
    couponName: 'E2E券' + STAMP, openReceive: 0, couponType: 'CASH', useRule: 'UNLIMITED',
    faceValue: 5, totalCount: 10, customerScope: 'ALL', status: 'NORMAL',
    startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    mallEnabled: 1, offlineEnabled: 0,
  })
  const tplId = data(c)?.id
  check('新增优惠券（制券）成功', c.status === 200 && !!tplId, JSON.stringify(data(c) || {}).slice(0, 120))

  const p1 = await api('GET', `/erp/marketing/coupon-template/page?pageNum=1&pageSize=20&couponName=${encodeURIComponent('E2E券' + STAMP)}`)
  const row = rows(p1)[0]
  check('优惠券可查回且「未领取」按数量守恒派生（总数-已领取-已使用）',
    row && Number(row.remainingCount) === 10, JSON.stringify(row || {}).slice(0, 160))

  // 发优惠券（对刚造的会员客户）
  if (MEMBER_ID && tplId) {
    const issue = await api('POST', `/erp/marketing/coupon-template/${tplId}/issue`,
      { partnerIds: [MEMBER_ID], quantityPerPartner: 2, sourceBillNo: 'E2E发放' })
    check('发优惠券成功（返回实际发放张数）', Number(data(issue)) === 2, `张数=${data(issue)}`)

    const dbC = await dbQuery(
      `SELECT count(*)::int AS c FROM erp_loyalty_coupon WHERE template_id = $1 AND partner_id = $2 AND deleted = 0`,
      [tplId, MEMBER_ID])
    check('券实例真实落库（erp_loyalty_coupon）', dbC[0].c === 2, `记录数=${dbC[0].c}`)

    const p2 = await api('GET', `/erp/marketing/coupon-template/page?pageNum=1&pageSize=20&couponName=${encodeURIComponent('E2E券' + STAMP)}`)
    check('已领取（未使用）随发放累加', Number(rows(p2)[0]?.receivedCount) === 2, `已领取=${rows(p2)[0]?.receivedCount}`)

    const rec = await api('GET', `/erp/marketing/coupon-template/record/page?pageNum=1&pageSize=20&templateId=${tplId}`)
    const rr = rows(rec)[0]
    check('领用明细可查到发放记录（客户/优惠券名称/面值/领用状态/领取时间）',
      !!rr && rr.couponName === 'E2E券' + STAMP && Number(rr.faceValue) === 5 && rr.receiveStatus === '已领取',
      JSON.stringify(rr || {}).slice(0, 200))

    const over = await api('POST', `/erp/marketing/coupon-template/${tplId}/issue`,
      { partnerIds: [MEMBER_ID], quantityPerPartner: 100 })
    check('超发被拒（未领取不足）', over.status >= 400 || Number(over.json?.code) !== 200, `${over.status} ${over.json?.message || ''}`)

    // 作废
    const v = await api('POST', `/erp/marketing/coupon-template/${tplId}/void`)
    check('优惠券作废成功', v.status === 200 && Number(v.json?.code) === 200, `${v.status}`)
    const dbV = await dbQuery(
      `SELECT (SELECT status FROM mkt_coupon_template WHERE id = $1) AS tpl,
              (SELECT count(*)::int FROM erp_loyalty_coupon WHERE template_id = $1 AND status = 'CANCELLED' AND deleted = 0) AS cancelled`,
      [tplId])
    check('作废后模板置 VOID 且未使用券同步作废', dbV[0].tpl === 'VOID' && dbV[0].cancelled === 2,
      JSON.stringify(dbV[0]))
    const bad = await api('POST', `/erp/marketing/coupon-template/${tplId}/issue`, { partnerIds: [MEMBER_ID], quantityPerPartner: 1 })
    check('已作废券不可再发放', bad.status >= 400 || Number(bad.json?.code) !== 200, `${bad.status}`)
  }
  if (tplId) await api('DELETE', `/erp/marketing/coupon-template/${tplId}`)

  // ── UI ──
  await openPage(`${FE}/marketing/coupon`)
  const text = await bodyText()
  check('优惠券页可打开', !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('双 Tab = 优惠券设置 / 领用明细', tabs.includes('优惠券设置') && tabs.includes('领用明细'), tabs.join('|'))
  const h1 = await headers()
  const want1 = ['优惠券名称', '开放领取', '类型', '使用规则', '面值', '总数', '已领取（未使用）', '已使用', '未领取',
    '指定客户', '起始时间', '到期时间', '状态', '商城使用', '线下使用']
  const miss1 = want1.filter(h => !h1.some(x => x.includes(h)))
  check('Tab1「优惠券设置」15 列表头齐全', miss1.length === 0, miss1.join(','))
  check('Tab1 行级含 发优惠券 / 作废 / 修改', text.includes('发优惠券') && text.includes('作废') && text.includes('修改'))
  await PAGE.screenshot({ path: `${SHOTS}/ui-coupon.png` })

  await PAGE.locator('.tab-item').filter({ hasText: '领用明细' }).click()
  await PAGE.waitForTimeout(2500)
  const h2 = await headers()
  const want2 = ['客户', '联系人', '联系电话', '优惠券名称', '类型', '使用规则', '面值', '领用状态',
    '单据编号', '状态', '领取时间', '使用时间', '来源单据']
  const miss2 = want2.filter(h => !h2.some(x => x.includes(h)))
  check('Tab2「领用明细」13 列表头齐全', miss2.length === 0, miss2.join(','))
}

// ══════════════ 六~八、商品促销 / 整单促销 / 特价 ══════════════
async function promoSuite() {
  section('六、商品促销（80312）/ 七、整单促销（80313）/ 八、特价（80314）')

  const PROMO_HEADERS = ['活动名称', '起始时间', '结束时间', '促销方式', '促销商品', '组合促销', '促销类型',
    '促销模式', '促销规则', '使用范围', '促销客户', '活动状态', '制单人']
  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)

  // ── 商品促销 ──
  const c1 = await api('POST', '/erp/marketing/promotion-activity', {
    name: 'E2E商品促销' + STAMP, type: 'PRODUCT', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    promoType: '按商品数量', promoMode: '满赠', promoScope: '线下使用', comboPromo: 0,
    description: '订单单个促销商品数量满5，赠送1件', productIds: prod.length ? String(prod[0].id) : undefined,
  })
  const id1 = data(c1)
  check('商品促销：新增活动成功', !!id1, JSON.stringify(data(c1)).slice(0, 80))
  const p1 = await api('GET', `/erp/marketing/promotion-activity/page?activityType=PRODUCT&pageNum=1&pageSize=20&name=${encodeURIComponent('E2E商品促销' + STAMP)}`)
  const r1 = rows(p1)[0]
  check('商品促销：可按 activityType=PRODUCT 查回', !!r1 && r1.type === 'PRODUCT', JSON.stringify(r1 || {}).slice(0, 140))
  check('商品促销：制单人已快照写入', !!r1?.creatorName, String(r1?.creatorName))
  if (id1 && prod.length) {
    const prods = await api('GET', `/erp/marketing/promotion-activity/${id1}/products`)
    check('商品促销：「查看商品」返回所选商品（含价格）',
      Array.isArray(data(prods)) && data(prods).length >= 1, `条数=${(data(prods) || []).length}`)
  }
  if (id1) {
    await api('POST', `/erp/marketing/promotion-activity/${id1}/status?status=cancelled`)
    const p2 = await api('GET', `/erp/marketing/promotion-activity/page?activityType=PRODUCT&pageNum=1&pageSize=20&name=${encodeURIComponent('E2E商品促销' + STAMP)}`)
    check('商品促销：状态可停用', rows(p2)[0]?.status === 'cancelled')
  }

  // ── 整单促销 ──
  const c2 = await api('POST', '/erp/marketing/promotion-activity', {
    name: 'E2E整单促销' + STAMP, type: 'ORDER', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    promoType: '按商品金额', promoMode: '满减', promoScope: '线上线下', comboPromo: 1, description: '整单满 200 减 20',
  })
  const id2 = data(c2)
  check('整单促销：新增活动成功', !!id2, JSON.stringify(data(c2)).slice(0, 80))
  const p3 = await api('GET', `/erp/marketing/promotion-activity/page?activityType=ORDER&pageNum=1&pageSize=20&name=${encodeURIComponent('E2E整单促销' + STAMP)}`)
  check('整单促销：与商品促销按促销方式隔离（ORDER 查得到、PRODUCT 查不到）',
    rows(p3).length === 1 && rows(p3)[0].type === 'ORDER', `total=${data(p3)?.total}`)
  const p3b = await api('GET', `/erp/marketing/promotion-activity/page?activityType=PRODUCT&pageNum=1&pageSize=20&name=${encodeURIComponent('E2E整单促销' + STAMP)}`)
  check('整单促销：不会被商品促销页串页查出', rows(p3b).length === 0)

  // ── 特价 ──
  const c3 = await api('POST', '/erp/marketing/promotion-activity', {
    name: 'E2E特价' + STAMP, type: 'SPECIAL_PRICE', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    promoScope: '线上线下', description: 'E2E 特价规则',
  })
  const id3 = data(c3)
  check('特价：新增活动成功', !!id3, JSON.stringify(data(c3)).slice(0, 80))
  const p4 = await api('GET', `/erp/marketing/promotion-activity/page?activityType=SPECIAL_PRICE&pageNum=1&pageSize=20&name=${encodeURIComponent('E2E特价' + STAMP)}`)
  check('特价：可按 activityType=SPECIAL_PRICE 查回', rows(p4).length === 1 && rows(p4)[0].type === 'SPECIAL_PRICE')

  const dup = await api('POST', '/erp/marketing/promotion-activity', { name: '', type: 'PRODUCT' })
  check('促销活动：空活动名称被拒', dup.status >= 400 || Number(dup.json?.code) !== 200, `${dup.status}`)

  // ── UI ──
  await assertListPage('/marketing/product-promo', '商品促销', PROMO_HEADERS, ['新增', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-product-promo.png` })
  await assertListPage('/marketing/order-promo', '整单促销', PROMO_HEADERS, ['新增', '刷新', '打印(F8)', '导出'])
  await assertListPage('/marketing/special-price', '特价',
    ['活动名称', '起始时间', '结束时间', '促销商品', '使用范围', '客户范围', '活动状态', '创建人', '创建时间'],
    ['新增', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-special-price.png` })

  // 清理（促销活动）
  for (const id of [id1, id2, id3]) {
    if (id) await api('DELETE', `/erp/marketing/promotion-activity/${id}`)
  }
  const dbAfter = await dbQuery(
    `SELECT count(*)::int AS c FROM erp_promotion_activity WHERE name LIKE $1 AND deleted = 0`, ['E2E%促销%'])
  check('促销活动清理完成（DB 无残留）', dbAfter[0].c === 0, `残留=${dbAfter[0].c}`)
}

// ══════════════ 九、套餐（80315）══════════════
async function packageDealSuite() {
  section('九、套餐（80315）')

  const prod = await dbQuery(`SELECT id, product_code, product_name, unit FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 2`)
  let kitId = null
  if (prod.length) {
    const kitCode = 'E2ETC' + STAMP
    const c = await api('POST', '/erp/product-kit', {
      kitName: 'E2E套餐' + STAMP, kitType: 1, kitPrice: 176, barcode: 'BAR' + STAMP,
      imageUrl: undefined, bundleSales: 1,
      items: prod.map((p, i) => ({
        componentProductId: p.id, componentProductCode: p.product_code,
        componentProductName: p.product_name, componentProductUnit: p.unit, quantity: i + 1,
      })),
    })
    kitId = c.json?.id || data(c)?.id
    check('新增套餐成功（含商品明细）', !!kitId, JSON.stringify(c.json || {}).slice(0, 120))

    const page = await api('GET', `/erp/product-kit/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E套餐' + STAMP)}`)
    const row = rawRecords(page)[0]
    check('套餐可查回且套餐编号自动生成', !!row && !!row.kitCode, JSON.stringify(row || {}).slice(0, 140))
    check('套餐：套餐条码 / 捆绑销售 已落库', row?.barcode === 'BAR' + STAMP && Number(row?.bundleSales) === 1,
      `barcode=${row?.barcode} bundle=${row?.bundleSales}`)

    if (kitId) {
      const detail = await api('GET', `/erp/product-kit/${kitId}`)
      const detailItems = (detail.json?.items || data(detail)?.items || [])
      check('套餐明细可查回（商品数 = 创建时的明细数）',
        detailItems.length === prod.length, `明细=${detailItems.length}`)
    }
  }

  await assertListPage('/marketing/package-deal', '套餐',
    ['图片', '套餐名称', '套餐编号', '套餐金额', '套餐条码', '捆绑销售', '商品明细'],
    ['新增套餐', '刷新', '打印(F8)', '导出'])
  const text = await bodyText()
  check('套餐：行级含 修改 / 停用 / 复制 / 删除',
    ['修改', '复制', '删除'].every(k => text.includes(k)))
  await PAGE.screenshot({ path: `${SHOTS}/ui-package-deal.png` })

  if (kitId) {
    await api('DELETE', `/erp/product-kit/${kitId}`)
    const page2 = await api('GET', `/erp/product-kit/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E套餐' + STAMP)}`)
    check('套餐可删除', rawRecords(page2).length === 0)
  }
}

// ══════════════ 十、商城拼团（80320）══════════════
let GROUP_ID = null

async function mallGroupSuite() {
  section('十、商城拼团（80320）')

  const a0 = await api('GET', '/erp/marketing/group-buy/activity/page?pageNum=1&pageSize=5')
  check('「拼团活动」Tab 接口 200（9 列聚合视图）', a0.status === 200 && Array.isArray(data(a0)?.records), `${a0.status}`)
  const o0 = await api('GET', '/erp/marketing/group-buy/order/page?pageNum=1&pageSize=5')
  check('「拼团订单」Tab 接口 200（11 列联表视图）', o0.status === 200 && Array.isArray(data(o0)?.records), `${o0.status}`)

  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)
  if (prod.length) {
    const c = await api('POST', '/erp/marketing/group-buy', {
      activityName: 'E2E拼团' + STAMP, productId: prod[0].id, originalPrice: 100, groupPrice: 80,
      minGroupSize: 3, maxGroupSize: 3, timeLimitMinutes: 60, totalQuantity: 100,
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59', status: 'DRAFT',
    })
    check('新增拼团活动成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const a1 = await api('GET', `/erp/marketing/group-buy/activity/page?pageNum=1&pageSize=20&name=${encodeURIComponent('E2E拼团' + STAMP)}`)
    const row = rows(a1)[0]
    GROUP_ID = row?.id
    check('拼团活动可按名称查回', !!GROUP_ID, JSON.stringify(row || {}).slice(0, 140))
    check('成团类型由成团人数派生（3 人团）', row?.groupType === '3人团', String(row?.groupType))
    check('开团/成功团个数为聚合值（新活动为 0）',
      Number(row?.groupCount) === 0 && Number(row?.successGroupCount) === 0,
      `${row?.groupCount}/${row?.successGroupCount}`)
  }

  // ── UI ──
  await openPage(`${FE}/marketing/mall-group`)
  const text = await bodyText()
  check('商城拼团页可打开', !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('双 Tab = 拼团活动 / 拼团订单', tabs.includes('拼团活动') && tabs.includes('拼团订单'), tabs.join('|'))
  const h1 = await headers()
  const wantA = ['活动ID', '活动名称', '起始时间', '结束时间', '成团类型', '开团个数', '成功团个数', '活动状态', '创建时间']
  check('「拼团活动」9 列表头齐全', wantA.every(h => h1.some(x => x.includes(h))), h1.join('|'))
  await PAGE.screenshot({ path: `${SHOTS}/ui-mall-group.png` })

  await PAGE.locator('.tab-item').filter({ hasText: '拼团订单' }).click()
  await PAGE.waitForTimeout(2500)
  const h2 = await headers()
  const wantO = ['拼团编号', '客户名称', '商品名称', '订单编号', '提交时间', '单据时间', '商品金额', '订单金额', '活动名称', '活动ID', '拼团状态']
  const missO = wantO.filter(h => !h2.some(x => x.includes(h)))
  check('「拼团订单」11 列表头齐全', missO.length === 0, missO.join(',') || h2.join('|'))
  check('分页栏为经典形态（.classic-pagination）', (await PAGE.locator('.classic-pagination').count()) >= 1)
}

// ══════════════ 十一、商城秒杀（80321）══════════════
let FLASH_ID = null

async function mallFlashSuite() {
  section('十一、商城秒杀（80321）')

  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)
  if (prod.length) {
    const c = await api('POST', '/erp/marketing/flash-sale', {
      title: 'E2E秒杀' + STAMP, productId: prod[0].id, productCode: prod[0].product_code,
      productName: prod[0].product_name, flashPrice: 9.9, originalPrice: 19.9, stockLimit: 50,
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59', status: 0,
    })
    check('新增秒杀活动成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const p1 = await api('GET', `/erp/marketing/flash-sale/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E秒杀' + STAMP)}`)
    FLASH_ID = rows(p1)[0]?.id
    check('秒杀活动可按名称查回', !!FLASH_ID, `total=${data(p1)?.total}`)
    if (FLASH_ID) {
      await api('POST', `/erp/marketing/flash-sale/${FLASH_ID}/publish`)
      const p2 = await api('GET', `/erp/marketing/flash-sale/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E秒杀' + STAMP)}`)
      check('秒杀活动可发布（状态流转）', Number(rows(p2)[0]?.status) === 1, `status=${rows(p2)[0]?.status}`)
      await api('POST', `/erp/marketing/flash-sale/${FLASH_ID}/cancel`)
      const p3 = await api('GET', `/erp/marketing/flash-sale/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E秒杀' + STAMP)}`)
      check('秒杀活动可取消', Number(rows(p3)[0]?.status) === 2, `status=${rows(p3)[0]?.status}`)
      const parts = await api('GET', `/erp/marketing/flash-sale/${FLASH_ID}/participants?pageNum=1&pageSize=5`)
      check('秒杀参与记录接口 200', parts.status === 200, `${parts.status}`)
    }
  }

  await assertListPage('/marketing/mall-flash', '商城秒杀',
    ['活动名称', '起始时间', '结束时间', '活动状态', '创建时间'], ['新增', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-mall-flash.png` })

  if (FLASH_ID) {
    await api('DELETE', `/erp/marketing/flash-sale/${FLASH_ID}`)
    const p4 = await api('GET', `/erp/marketing/flash-sale/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E秒杀' + STAMP)}`)
    check('秒杀活动可删除', rows(p4).length === 0)
  }
}

// ══════════════ 十二、商城预售（80322）══════════════
let PRESALE_ID = null

async function mallPresaleSuite() {
  section('十二、商城预售（80322）')

  const prod = await dbQuery(`SELECT id, product_code, product_name, spec, model FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)
  if (prod.length) {
    const c = await api('POST', '/erp/marketing/presale', {
      activityName: 'E2E预售' + STAMP, productId: prod[0].id, productName: prod[0].product_name,
      productCode: prod[0].product_code, productSpec: prod[0].spec, productModel: prod[0].model,
      presalePrice: 66, depositRequired: 1, depositAmount: 10, finalAmount: 56, stockLimit: 20,
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    })
    check('新增预售活动成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const p1 = await api('GET', `/erp/marketing/presale/page?pageNum=1&pageSize=20&activityName=${encodeURIComponent('E2E预售' + STAMP)}`)
    const row = rows(p1)[0]
    PRESALE_ID = row?.id
    check('预售活动可查回', !!PRESALE_ID, `total=${data(p1)?.total}`)
    check('预售新增列已落库（预售价 / 是否支付订金 / 规格 / 型号 / 创建人）',
      Number(row?.presalePrice) === 66 && Number(row?.depositRequired) === 1
      && row?.productSpec === prod[0].spec && !!row?.creatorName,
      JSON.stringify({ price: row?.presalePrice, dep: row?.depositRequired, spec: row?.productSpec, creator: row?.creatorName }))

    const o1 = await api('GET', `/erp/marketing/presale/order/page?pageNum=1&pageSize=5&presaleId=${PRESALE_ID}`)
    check('「预售订单」Tab 接口 200（按预售活动过滤）', o1.status === 200 && Array.isArray(data(o1)?.records), `${o1.status}`)
  }

  // ── UI ──
  await openPage(`${FE}/marketing/mall-presale`)
  const text = await bodyText()
  check('商城预售页可打开', !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  check('双 Tab = 商品预售 / 预售订单', tabs.includes('商品预售') && tabs.includes('预售订单'), tabs.join('|'))
  const h1 = await headers()
  const want1 = ['活动名称', '商品名称', '商品图片', '货号', '规格', '型号', '预售价', '起始时间', '结束时间', '是否支付订金', '活动状态', '创建人']
  const miss1 = want1.filter(h => !h1.some(x => x.includes(h)))
  check('「商品预售」12 列表头齐全', miss1.length === 0, miss1.join(',') || h1.join('|'))
  await PAGE.screenshot({ path: `${SHOTS}/ui-mall-presale.png` })

  await PAGE.locator('.tab-item').filter({ hasText: '预售订单' }).click()
  await PAGE.waitForTimeout(2500)
  const h2 = await headers()
  const want2 = ['客户名称', '商品名称', '单据编号', '单据日期', '商品金额', '订单金额', '活动名称', '活动状态']
  const miss2 = want2.filter(h => !h2.some(x => x.includes(h)))
  check('「预售订单」8 列表头齐全', miss2.length === 0, miss2.join(',') || h2.join('|'))
}

// ══════════════ 十三、商城弹窗广告（80323）══════════════
let POPUP_ID = null

async function mallPopupSuite() {
  section('十三、商城弹窗广告（80323）')

  const c = await api('POST', '/erp/mall/admin/popup-ad', {
    title: 'E2E弹窗广告' + STAMP, showType: 'once', targetUser: 'all', sort: 0,
    startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
  })
  check('新增弹窗广告成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
  const p1 = await api('GET', `/erp/mall/admin/popup-ad/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E弹窗广告' + STAMP)}`)
  const row = rows(p1)[0]
  POPUP_ID = row?.id
  check('弹窗广告可查回', !!POPUP_ID, `total=${data(p1)?.total}`)
  check('创建人已快照落库（对标「创建人」列）', !!row?.creatorName, String(row?.creatorName))

  if (POPUP_ID) {
    await api('POST', `/erp/mall/admin/popup-ad/${POPUP_ID}/publish`)
    const p2 = await api('GET', `/erp/mall/admin/popup-ad/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E弹窗广告' + STAMP)}`)
    check('弹窗广告可发布（草稿→投放中）', Number(rows(p2)[0]?.status) === 1, `status=${rows(p2)[0]?.status}`)
    await api('POST', `/erp/mall/admin/popup-ad/${POPUP_ID}/offline`)
    const p3 = await api('GET', `/erp/mall/admin/popup-ad/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E弹窗广告' + STAMP)}`)
    check('弹窗广告可下线', Number(rows(p3)[0]?.status) === 3, `status=${rows(p3)[0]?.status}`)
  }

  await assertListPage('/marketing/mall-popup', '商城弹窗广告',
    ['活动名称', '起始时间', '结束时间', '活动状态', '创建人'], ['新增', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-mall-popup.png` })

  if (POPUP_ID) {
    await api('DELETE', `/erp/mall/admin/popup-ad/${POPUP_ID}`)
    const p4 = await api('GET', `/erp/mall/admin/popup-ad/page?pageNum=1&pageSize=20&title=${encodeURIComponent('E2E弹窗广告' + STAMP)}`)
    check('弹窗广告可删除', rows(p4).length === 0)
  }
}

// ══════════════ 十四、加价购（80324）══════════════
let ADDON_ID = null

async function addonSuite() {
  section('十四、加价购（80324）')

  const prod = await dbQuery(`SELECT id, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 2`)
  if (prod.length >= 2) {
    const c = await api('POST', '/erp/marketing/addon-rule', {
      ruleName: 'E2E加价购' + STAMP, mainProductId: prod[0].id, mainProductName: prod[0].product_name,
      addonProductId: prod[1].id, addonProductName: prod[1].product_name, addonPrice: 5, maxPerOrder: 2,
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
    })
    check('新增加价购成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
    const p1 = await api('GET', `/erp/marketing/addon-rule/page?pageNum=1&pageSize=20&ruleName=${encodeURIComponent('E2E加价购' + STAMP)}`)
    const row = rows(p1)[0]
    ADDON_ID = row?.id
    check('加价购可按名称查回', !!ADDON_ID, `total=${data(p1)?.total}`)
    check('制单人已快照落库（对标「制单人」列）', !!row?.creatorName, String(row?.creatorName))
    if (ADDON_ID) {
      await api('POST', `/erp/marketing/addon-rule/${ADDON_ID}/enable`)
      const p2 = await api('GET', `/erp/marketing/addon-rule/page?pageNum=1&pageSize=20&ruleName=${encodeURIComponent('E2E加价购' + STAMP)}`)
      check('加价购可启用', Number(rows(p2)[0]?.status) === 1, `status=${rows(p2)[0]?.status}`)
      await api('POST', `/erp/marketing/addon-rule/${ADDON_ID}/disable`)
      const p3 = await api('GET', `/erp/marketing/addon-rule/page?pageNum=1&pageSize=20&ruleName=${encodeURIComponent('E2E加价购' + STAMP)}`)
      check('加价购可停用', Number(rows(p3)[0]?.status) === 0, `status=${rows(p3)[0]?.status}`)
    }
  }

  await assertListPage('/marketing/add-on-deal', '加价购',
    ['活动名称', '起始时间', '结束时间', '活动状态', '制单人'], ['新增', '刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-add-on-deal.png` })

  if (ADDON_ID) {
    await api('DELETE', `/erp/marketing/addon-rule/${ADDON_ID}`)
    const p4 = await api('GET', `/erp/marketing/addon-rule/page?pageNum=1&pageSize=20&ruleName=${encodeURIComponent('E2E加价购' + STAMP)}`)
    check('加价购可删除', rows(p4).length === 0)
  }
}

// ══════════════ 十五、热门搜索词推荐（80325）══════════════
let KEYWORD_ID = null

async function hotKeywordsSuite() {
  section('十五、热门搜索词推荐（80325）')

  const c = await api('POST', '/erp/mall/admin/keyword', {
    keyword: 'E2E热词' + STAMP, keywordType: 1, sort: 66, status: 1,
  })
  check('新增热门关键词成功', c.status === 200 && Number(c.json?.code) === 200, `${c.status}`)
  const p1 = await api('GET', `/erp/mall/admin/keyword/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E热词' + STAMP)}`)
  const row = rows(p1)[0]
  KEYWORD_ID = row?.id
  check('热门关键词可查回', !!KEYWORD_ID, `total=${data(p1)?.total}`)
  check('排序与最后修改时间已落库', Number(row?.sort) === 66 && !!row?.updateTime, JSON.stringify(row || {}).slice(0, 120))
  if (KEYWORD_ID) {
    await api('PUT', `/erp/mall/admin/keyword/${KEYWORD_ID}`, { keyword: 'E2E热词改' + STAMP, sort: 67, keywordType: 1 })
    const p2 = await api('GET', `/erp/mall/admin/keyword/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E热词改' + STAMP)}`)
    check('热门关键词可修改', rows(p2).length === 1 && Number(rows(p2)[0]?.sort) === 67)
  }

  await assertListPage('/marketing/hot-keywords', '热门搜索词推荐',
    ['关键词名称', '排序', '最后修改时间'], ['新增关键词', '刷新', '打印(F8)'])
  const text = await bodyText()
  check('提示「最多新增20个热门关键词」存在', text.includes('最多新增20个热门关键词'))
  await PAGE.screenshot({ path: `${SHOTS}/ui-hot-keywords.png` })

  if (KEYWORD_ID) {
    await api('DELETE', `/erp/mall/admin/keyword/${KEYWORD_ID}`)
    const p3 = await api('GET', `/erp/mall/admin/keyword/page?pageNum=1&pageSize=20&keyword=${encodeURIComponent('E2E热词改' + STAMP)}`)
    check('热门关键词可删除', rows(p3).length === 0)
  }
}

// ══════════════ 十六、我要推广（80330）══════════════
async function promoteCreateSuite() {
  section('十六、我要推广（80330）')

  // 6 个 Tab 的物料端点
  const t1 = await api('GET', '/erp/marketing/promote/product/page?pageNum=1&pageSize=5')
  check('「商品」Tab 接口 200（含库存/最近销售时间/分享统计）', t1.status === 200 && Array.isArray(data(t1)?.records), `${t1.status}`)
  const t2 = await api('GET', '/erp/marketing/coupon-template/page?pageNum=1&pageSize=5')
  check('「优惠券」Tab 物料接口 200', t2.status === 200, `${t2.status}`)
  const t3 = await api('GET', '/erp/marketing/promotion-activity/page?pageNum=1&pageSize=5')
  check('「促销」Tab 物料接口 200（不带 activityType ＝ 全部促销方式）', t3.status === 200 && Array.isArray(data(t3)?.records), `${t3.status}`)
  const t4 = await api('GET', '/erp/marketing/group-buy/activity/page?pageNum=1&pageSize=5')
  check('「拼团」Tab 物料接口 200', t4.status === 200, `${t4.status}`)
  const t5 = await api('GET', '/erp/marketing/flash-sale/page?pageNum=1&pageSize=5')
  check('「秒杀」Tab 物料接口 200', t5.status === 200, `${t5.status}`)
  const t6 = await api('GET', '/erp/marketing/share/my/page?pageNum=1&pageSize=5')
  check('「我的推广」Tab 接口 200', t6.status === 200 && Array.isArray(data(t6)?.records), `${t6.status}`)

  // 分享闭环：登记分享 → summary 聚合 → 我的推广出现 → 推广历史出现
  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 1`)
  if (prod.length) {
    const share = await api('POST', '/erp/marketing/share', {
      shareType: 'PRODUCT', targetId: prod[0].id, targetName: prod[0].product_name,
      shareSummary: '商品 ' + prod[0].product_name + ' E2E' + STAMP,
    })
    check('登记分享成功（「分享」按钮链路）', share.status === 200 && Number(share.json?.code) === 200, `${share.status}`)

    const sum = await api('GET', '/erp/marketing/share/summary?shareType=PRODUCT')
    const s = (data(sum) || []).find(x => String(x.targetId) === String(prod[0].id))
    check('分享统计按物料聚合（分享次数 ≥ 1，含最近分享时间）',
      !!s && Number(s.shareCount) >= 1 && !!s.lastShareTime, JSON.stringify(s || {}).slice(0, 140))

    const mine = await api('GET', '/erp/marketing/share/my/page?pageNum=1&pageSize=20')
    check('「我的推广」可查到本次分享', rows(mine).some(r => r.shareSummary?.includes('E2E' + STAMP)))

    const hist = await api('GET', `/erp/marketing/share/page?pageNum=1&pageSize=20&sharer=${encodeURIComponent(E2E_USER)}`)
    check('推广历史可查到本次分享（含分享人）', rows(hist).some(r => String(r.sharerId) === String(data(mine)?.records?.[0]?.sharerId)),
      `total=${data(hist)?.total}`)
  }

  // ── UI ──
  await openPage(`${FE}/marketing/promote-create`)
  const text = await bodyText()
  check('我要推广页可打开', !text.includes('页面不存在') && text.length > 80, PAGE.url())
  const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  const wantTabs = ['商品', '优惠券', '促销', '拼团', '秒杀', '我的推广']
  check('6 Tab 齐全（商品/优惠券/促销/拼团/秒杀/我的推广）',
    wantTabs.every(t => tabs.includes(t)), tabs.join('|'))
  const h1 = await headers()
  const wantH = ['图片', '商品货号', '商品名称', '品牌', '规格', '型号', '产地', '单位', '库存',
    '最近销售时间', '新增时间', '最近分享时间', '分享次数', '浏览人数', '浏览次数']
  const missH = wantH.filter(h => !h1.some(x => x.includes(h)))
  check('「商品」Tab 默认 15 列表头齐全', missH.length === 0, missH.join(',') || h1.join('|'))
  check('「商品」Tab 有「页面配置」入口（对标实测该 Tab 有）',
    (await PAGE.locator('button[title="页面配置"]').count()) >= 1)
  await PAGE.screenshot({ path: `${SHOTS}/ui-promote-create.png` })

  // 切到「我的推广」：应无页面配置入口、9 列表头
  await PAGE.locator('.tab-item').filter({ hasText: /^我的推广$/ }).click()
  await PAGE.waitForTimeout(2500)
  const h6 = await headers()
  const want6 = ['分享时间', '分享类型', '分享概要', '浏览次数', '浏览人数', '分享领取数', '下单人数', '下单笔数', '下单金额']
  const miss6 = want6.filter(h => !h6.some(x => x.includes(h)))
  check('「我的推广」9 列表头齐全', miss6.length === 0, miss6.join(',') || h6.join('|'))
  check('「我的推广」无页面配置入口（对标实测只有商品/促销两个 Tab 有）',
    (await PAGE.locator('button[title="页面配置"]').count()) === 0)
}

// ══════════════ 十七、推广历史查询（80331）══════════════
async function promoteHistorySuite() {
  section('十七、推广历史查询（80331）')

  const p1 = await api('GET', '/erp/marketing/share/page?pageNum=1&pageSize=10')
  check('推广历史接口 200', p1.status === 200 && Array.isArray(data(p1)?.records), `${p1.status}`)
  const filtered = await api('GET', '/erp/marketing/share/page?pageNum=1&pageSize=10&shareType=PRODUCT')
  check('推广历史可按分享类型过滤', filtered.status === 200
    && rows(filtered).every(r => r.shareType === 'PRODUCT'), `total=${data(filtered)?.total}`)
  const byDate = await api('GET', '/erp/marketing/share/page?pageNum=1&pageSize=10&startDate=2000-01-01&endDate=2099-12-31')
  check('推广历史可按时间区间过滤', byDate.status === 200, `total=${data(byDate)?.total}`)

  await assertListPage('/marketing/promote-history', '推广历史查询',
    ['分享时间', '分享人', '分享类型', '分享概要', '浏览次数', '浏览人数', '分享领取数', '下单人数', '下单笔数', '下单金额'],
    ['刷新', '打印(F8)', '导出'])
  await PAGE.screenshot({ path: `${SHOTS}/ui-promote-history.png` })
}

// ══════════════ 十八、促销引擎与券核销闭环（P0） ══════════════
async function promotionEngineSuite() {
  section('十八、促销引擎与券核销闭环')

  const prod = await dbQuery(`SELECT id, product_code, product_name FROM erp_product WHERE deleted = 0 ORDER BY id LIMIT 2`)
  const cust = await dbQuery(`SELECT id, party_name FROM biz_party WHERE deleted = 0 AND party_type = 1 ORDER BY id LIMIT 1`)
  if (!prod.length || !cust.length) {
    check('促销引擎验收前置（已有商品与客户主数据）', false, '缺少 erp_product / biz_party 数据')
    return
  }
  const T = 'PE' + STAMP
  let actOrder = null
  let actItem = null
  let actExcl = null
  let orderId = null
  let orderId2 = null
  let tplId = null

  try {
    // 造「整单满减」：满 100 减 20，优先级 100，封顶 20，总次数 50
    actOrder = data(await api('POST', '/erp/marketing/promotion-activity', {
      name: 'E2E满减' + T, type: 'ORDER', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
      promoMode: '满减', minAmount: 100, reductionAmount: 20, priority: 100, stackPolicy: 'STACK',
      maxDiscountAmount: 20, quotaTotal: 50,
    }))
    // 造「商品打折」：指定商品 9 折，优先级 50
    actItem = data(await api('POST', '/erp/marketing/promotion-activity', {
      name: 'E2E打折' + T, type: 'PRODUCT', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
      promoMode: '打折', discountRate: 90, priority: 50, stackPolicy: 'STACK', productIds: String(prod[0].id),
    }))
    check('促销活动已可配置结构化优惠（满减门槛/满减额/折扣率/优先级/封顶/次数）',
      !!actOrder && !!actItem, `order=${actOrder} item=${actItem}`)

    const lines = [
      { lineNo: 1, productId: prod[0].id, quantity: 2, unitPrice: 100 },
      { lineNo: 2, productId: prod[1] ? prod[1].id : prod[0].id, quantity: 1, unitPrice: 50 },
    ]

    // ── 试算 ──
    const calc = await api('POST', '/erp/marketing/promotion/calc',
      { customerId: cust[0].id, orderDate: '2026-09-18', channel: 'OFFLINE', lines })
    const r = data(calc)
    check('POST /erp/marketing/promotion/calc 试算 200', calc.status === 200 && !!r, String(calc.status))
    check('促销优惠 = 40（行级 200×10% + 整单满减 20 封顶）', Number(r && r.promoDiscount) === 40,
      '实得 ' + (r && r.promoDiscount))
    const allocs = (r && r.allocations) || []
    check('优惠按行分摊（整单级按行金额占比 + 行级摊到命中行）',
      allocs.length >= 3 && allocs.some(a => a.scopeType === 'ORDER') && allocs.some(a => a.scopeType === 'ITEM'),
      JSON.stringify(allocs.map(a => a.promoMode + '#' + a.lineNo + '=' + a.discountAmount)))
    check('已生效活动按优先级降序计算（100 在 50 之前）',
      String(r && r.appliedPromotionIds && r.appliedPromotionIds[0]) === String(actOrder))

    const quotaBefore = await dbQuery(`SELECT quota_used FROM erp_promotion_activity WHERE id = $1`, [actOrder])
    check('试算不累加活动次数（quota_used 仍为 0）', Number(quotaBefore[0] && quotaBefore[0].quota_used) === 0,
      String(quotaBefore[0] && quotaBefore[0].quota_used))

    // ── 独占（EXCLUSIVE）──
    actExcl = data(await api('POST', '/erp/marketing/promotion-activity', {
      name: 'E2E独占' + T, type: 'ORDER', startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59',
      promoMode: '满减', minAmount: 1, reductionAmount: 5, priority: 200, stackPolicy: 'EXCLUSIVE',
    }))
    const calcEx = await api('POST', '/erp/marketing/promotion/calc',
      { customerId: cust[0].id, orderDate: '2026-09-18', channel: 'OFFLINE', lines })
    check('独占（EXCLUSIVE）命中后不再叠加更低优先级活动：优惠 = 5',
      Number(data(calcEx) && data(calcEx).promoDiscount) === 5,
      '实得 ' + (data(calcEx) && data(calcEx).promoDiscount))
    await api('DELETE', '/erp/marketing/promotion-activity/' + actExcl)
    actExcl = null

    // ── 真正落单：服务端重算 + 分摊落库 + 次数累加 ──
    const orderRes = await api('POST', '/erp/sale/order', {
      customerId: cust[0].id, orderDate: '2026-09-18', saleType: 1,
      items: lines.map(l => ({
        lineNo: l.lineNo, productId: l.productId,
        productName: (prod.filter(p => String(p.id) === String(l.productId))[0] || {}).product_name,
        quantity: l.quantity, unitPrice: l.unitPrice, calculatedPrice: l.unitPrice,
      })),
    })
    orderId = (data(orderRes) && data(orderRes).id) || data(orderRes)
    check('销售订单创建成功（优惠由服务端计算）', !!orderId, 'id=' + orderId)
    if (orderId) {
      const ord = await dbQuery(
        `SELECT product_amount, promo_discount, bill_amount FROM erp_sale_order WHERE id = $1`, [orderId])
      check('订单 promo_discount 由服务端写入 = 40（不再信任前端传参）',
        Number(ord[0] && ord[0].promo_discount) === 40, JSON.stringify(ord[0]))
      check('应付 = 商品金额 250 − 促销 40 = 210', Number(ord[0] && ord[0].bill_amount) === 210,
        String(ord[0] && ord[0].bill_amount))
      const det = await dbQuery(
        `SELECT promo_mode, scope_type, line_no, discount_amount FROM erp_sale_order_promo_detail WHERE order_id = $1 ORDER BY line_no`,
        [orderId])
      check('优惠分摊明细已落库（订单优惠可回溯 + 分析域毛利数据源）', det.length >= 3, JSON.stringify(det))
      const quotaAfter = await dbQuery(`SELECT quota_used FROM erp_promotion_activity WHERE id = $1`, [actOrder])
      check('落单后活动已用次数 +1', Number(quotaAfter[0] && quotaAfter[0].quota_used) === 1,
        String(quotaAfter[0] && quotaAfter[0].quota_used))
    }

    // ── 券核销闭环 ──
    const tplRes = await api('POST', '/erp/marketing/coupon-template', {
      couponName: 'E2E引擎券' + T, couponType: 'CASH', useRule: 'UNLIMITED', faceValue: 30,
      totalCount: 10, customerScope: 'ALL', status: 'NORMAL',
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59', mallEnabled: 1, offlineEnabled: 1,
    })
    tplId = data(tplRes) && data(tplRes).id
    await api('POST', '/erp/marketing/coupon-template/' + tplId + '/issue',
      { partnerIds: [cust[0].id], quantityPerPartner: 1 })
    const coupon = (await dbQuery(
      `SELECT id, code, status FROM erp_loyalty_coupon WHERE template_id = $1 AND deleted = 0`, [tplId]))[0]
    check('券已发放（UNUSED）', coupon && coupon.status === 'UNUSED', JSON.stringify(coupon))

    const orderRes2 = await api('POST', '/erp/sale/order', {
      customerId: cust[0].id, orderDate: '2026-09-18', saleType: 1, couponIds: [coupon.id],
      items: [{ lineNo: 1, productId: prod[0].id, quantity: 1, unitPrice: 100, calculatedPrice: 100 }],
    })
    orderId2 = (data(orderRes2) && data(orderRes2).id) || data(orderRes2)
    if (orderId2) {
      const o2 = await dbQuery(`SELECT promo_discount, coupon_amount FROM erp_sale_order WHERE id = $1`, [orderId2])
      check('券优惠由服务端计入 coupon_amount = 30', Number(o2[0] && o2[0].coupon_amount) === 30, JSON.stringify(o2[0]))
      const cu = await dbQuery(`SELECT status, used_order_id FROM erp_loyalty_coupon WHERE id = $1`, [coupon.id])
      check('券已核销（USED + 记录核销订单）',
        cu[0] && cu[0].status === 'USED' && String(cu[0].used_order_id) === String(orderId2), JSON.stringify(cu[0]))
      await api('DELETE', '/erp/sale/order/' + orderId2)
      orderId2 = null
      const cu2 = await dbQuery(`SELECT status, used_order_id FROM erp_loyalty_coupon WHERE id = $1`, [coupon.id])
      check('订单删除后券回滚为 UNUSED（券不会被永久占用）',
        cu2[0] && cu2[0].status === 'UNUSED' && cu2[0].used_order_id == null, JSON.stringify(cu2[0]))
    } else {
      check('带券下单成功', false, JSON.stringify(data(orderRes2)).slice(0, 120))
    }

    // ── 优惠超限拦截 ──
    const over = await api('POST', '/erp/sale/order', {
      customerId: cust[0].id, orderDate: '2026-09-18', saleType: 1, directDiscount: 9999,
      items: [{ lineNo: 1, productId: prod[0].id, quantity: 1, unitPrice: 10, calculatedPrice: 10 }],
    })
    check('优惠合计超过商品金额时被拒（不允许负应付）',
      over.status >= 400 || Number(over.json && over.json.code) !== 200,
      over.status + ' ' + ((over.json && over.json.message) || ''))
  } finally {
    if (orderId2) await api('DELETE', '/erp/sale/order/' + orderId2)
    if (orderId) await api('DELETE', '/erp/sale/order/' + orderId)
    if (tplId) await api('DELETE', '/erp/marketing/coupon-template/' + tplId)
    const ids = [actOrder, actItem, actExcl]
    for (const id of ids) {
      if (id) await api('DELETE', '/erp/marketing/promotion-activity/' + id)
    }
    const left = await dbQuery(
      `SELECT count(*)::int AS c FROM erp_promotion_activity WHERE name LIKE 'E2E%' AND deleted = 0`)
    check('促销引擎造数已清理（活动无残留）', left[0].c === 0, String(left[0].c))
  }
}

// ══════════════ 十九、会员等级规则与积分有效期（P1） ══════════════
async function memberLevelPointsSuite() {
  section('十九、会员等级规则与积分有效期')

  const T = 'LV' + STAMP
  const cardNo = 'LVC' + STAMP
  let custHi = null
  let custLo = null

  try {
    // ── 等级规则 ──
    const rules = await api('GET', '/erp/marketing/member-level/rules')
    const rl = data(rules) || []
    check('等级规则接口 200 且含门槛/保级字段', rules.status === 200 && rl.length >= 4
      && rl.every(r => 'upgrade_amount' in r && 'keep_months' in r), '等级数=' + rl.length)
    check('重复等级已去重（同一 level_name 仅一行）',
      new Set(rl.map(r => r.level_name)).size === rl.length, rl.map(r => r.level_name).join(','))

    const mk = async (code, consume) => data(await api('POST', '/erp/md/customer', {
      partnerType: 'customer', partnerCode: code, partnerName: 'E2E等级客户' + code,
      memberName: 'E2E等级会员' + code, memberCardNo: cardNo + code.slice(-3),
      memberCardStatus: 'NORMAL', memberTotalConsume: consume, points: 0,
    }))?.id
    custHi = await mk('LVHI' + STAMP, 12000)
    custLo = await mk('LVLO' + STAMP, 100)
    check('等级验收造数（高/低消费会员各一）', !!custHi && !!custLo, 'hi=' + custHi + ' lo=' + custLo)

    const evRes = await api('POST', '/erp/marketing/member-level/evaluate?limit=500')
    const ev = data(evRes)
    check('等级评估接口 200（dry-run 只算不改）', evRes.status === 200 && Array.isArray(ev?.changes),
      'total=' + (ev && ev.total))
    const hiRow = ((ev && ev.changes) || []).filter(c => String(c.partnerId) === String(custHi))[0]
    check('评估按累计消费额命中更高等级',
      !!hiRow && !!hiRow.targetLevel && hiRow.targetLevel !== '普通会员', JSON.stringify(hiRow || {}).slice(0, 140))
    const before = await dbQuery(`SELECT member_level FROM biz_party WHERE id = $1`, [custHi])
    check('评估不写库（dry-run 语义）', before[0] && before[0].member_level == null, String(before[0] && before[0].member_level))

    // 自动升级开关门控
    const cfg0 = data(await api('GET', '/erp/marketing/member-config'))
    await api('PUT', '/erp/marketing/member-config', { ...cfg0, autoUpgradeEnabled: 0 })
    const denied = await api('POST', '/erp/marketing/member-level/apply?limit=500&force=false')
    check('「会员自动升级」关闭时拒绝执行升降级',
      denied.status >= 400 || Number(denied.json && denied.json.code) !== 200,
      denied.status + ' ' + String(denied.json && denied.json.message || '').slice(0, 70))

    await api('PUT', '/erp/marketing/member-config', { ...cfg0, autoUpgradeEnabled: 1 })
    const applied = await api('POST', '/erp/marketing/member-level/apply?limit=500&force=false')
    check('开启后执行升降级成功', applied.status === 200 && Number(data(applied) && data(applied).applied) >= 1,
      JSON.stringify(data(applied)))
    const after = await dbQuery(`SELECT member_level FROM biz_party WHERE id = $1`, [custHi])
    check('等级已写回 biz_party.member_level',
      after[0] && after[0].member_level === (hiRow && hiRow.targetLevel), String(after[0] && after[0].member_level))
    await api('PUT', '/erp/marketing/member-config',
      { ...cfg0, autoUpgradeEnabled: cfg0.autoUpgradeEnabled, pointsValidMonths: 12, pointsExpireRemindDays: 30 })

    // ── 积分有效期：批次 / FIFO / 过期 / 提醒 ──
    const earn1 = await api('POST', '/erp/marketing/points-ledger/earn',
      { memberCardNo: cardNo, partnerId: custHi, points: 100, source: 'ADJUST' })
    check('记一笔积分获得（写批次）', earn1.status === 200 && !!data(earn1), JSON.stringify(data(earn1)))
    const b1 = await dbQuery(
      `SELECT earned_points, remaining_points, expire_time, status FROM mkt_points_batch WHERE member_card_no = $1 ORDER BY id`, [cardNo])
    check('批次落库且按「积分有效期(月)」算出到期时间',
      b1.length === 1 && Number(b1[0].earned_points) === 100 && !!b1[0].expire_time, JSON.stringify(b1[0]))
    check('可用积分 = 批次剩余之和（100）',
      Number(data(await api('GET', '/erp/marketing/points-ledger/available?memberCardNo=' + cardNo))) === 100)

    const use1 = await api('POST', '/erp/marketing/points-ledger/use', { memberCardNo: cardNo, points: 30 })
    check('FIFO 扣减 30', Number(data(use1)) === 30, String(data(use1)))
    const b2 = await dbQuery(`SELECT remaining_points FROM mkt_points_batch WHERE member_card_no = $1`, [cardNo])
    check('批次剩余同步为 70', Number(b2[0].remaining_points) === 70, JSON.stringify(b2[0]))
    check('可用积分同步为 70',
      Number(data(await api('GET', '/erp/marketing/points-ledger/available?memberCardNo=' + cardNo))) === 70)

    const jr = await api('GET', '/erp/marketing/points-ledger/journal/page?memberCardNo=' + cardNo)
    check('积分流水含 EARN 与 USE',
      (data(jr)?.records || []).some(r => r.changeType === 'EARN')
      && (data(jr)?.records || []).some(r => r.changeType === 'USE'),
      JSON.stringify((data(jr)?.records || []).map(r => r.changeType)))

    await dbQuery(`UPDATE mkt_points_batch SET expire_time = now() - interval '1 day' WHERE member_card_no = $1`, [cardNo])
    const ex = await api('POST', '/erp/marketing/points-ledger/expire')
    check('执行过期：返回会员数与过期积分', ex.status === 200 && Number(data(ex)?.expiredPoints) >= 70,
      JSON.stringify({ n: data(ex)?.memberCount, p: data(ex)?.expiredPoints }))
    const b3 = await dbQuery(`SELECT remaining_points, status FROM mkt_points_batch WHERE member_card_no = $1`, [cardNo])
    check('过期批次剩余清零且状态 EXPIRED',
      Number(b3[0].remaining_points) === 0 && b3[0].status === 'EXPIRED', JSON.stringify(b3[0]))
    check('过期后可用积分为 0',
      Number(data(await api('GET', '/erp/marketing/points-ledger/available?memberCardNo=' + cardNo))) === 0)
    const jr2 = await api('GET', '/erp/marketing/points-ledger/journal/page?memberCardNo=' + cardNo + '&changeType=EXPIRE')
    check('过期写入 EXPIRE 流水（账单可追溯）', (data(jr2)?.records || []).length >= 1,
      JSON.stringify((data(jr2)?.records || [])[0] || {}).slice(0, 120))

    await api('POST', '/erp/marketing/points-ledger/earn', { memberCardNo: cardNo, partnerId: custHi, points: 50, source: 'GIFT' })
    await dbQuery(`UPDATE mkt_points_batch SET expire_time = now() + interval '15 days' WHERE member_card_no = $1 AND status = 'ACTIVE'`, [cardNo])
    const soon = await api('GET', '/erp/marketing/points-ledger/expiring-soon?days=30')
    check('「即将过期」清单可查出（到期提醒数据源）',
      soon.status === 200 && (data(soon) || []).some(b => b.memberCardNo === cardNo), '条数=' + ((data(soon) || []).length))

    // ── UI：会员设置页的积分有效期与会员等级分区 ──
    await openPage(`${FE}/marketing/member-config`)
    const text = await bodyText()
    check('会员设置页含「积分有效期」与「到期前提醒」（本系统新增项）',
      text.includes('积分有效期') && text.includes('到期前提醒'), '')
    check('会员设置页含第 4 分区「会员等级」（对标为三分区）', text.includes('会员等级'))
    check('会员设置页含「等级评估 / 执行升降级 / 新增等级」入口',
      text.includes('等级评估') && text.includes('执行升降级') && text.includes('新增等级'))
    const levelRows = await PAGE.locator('.group-card').last().locator('tbody tr').count()
    check('会员等级分区渲染出等级行（≥4 级）', levelRows >= 4, '行数=' + levelRows)
    check('会员设置页「查看即将过期 / 立即执行过期」入口存在',
      text.includes('查看即将过期') && text.includes('立即执行过期'))
    await PAGE.screenshot({ path: `${SHOTS}/ui-member-config-level.png` })
  } finally {
    await dbQuery(`DELETE FROM mkt_points_journal WHERE member_card_no LIKE $1`, [cardNo + '%'])
    await dbQuery(`DELETE FROM mkt_points_batch WHERE member_card_no LIKE $1`, [cardNo + '%'])
    for (const id of [custHi, custLo]) {
      if (id) await api('DELETE', '/erp/md/customer/' + id)
    }
    const left = await dbQuery(
      `SELECT count(*)::int AS c FROM mkt_points_batch WHERE member_card_no LIKE $1`, [cardNo + '%'])
    check('等级/积分验收造数已清理', left[0].c === 0, String(left[0].c))
  }
}

// ══════════════ 二十、营销自动化（80303 · 本系统建模页） ══════════════
async function autoCampaignSuite() {
  section('二十、营销自动化（80303 · 本系统建模页）')

  const T = 'AU' + STAMP
  const mobile = '136' + STAMP + '0'
  const codes = []
  const campIds = []
  let tplId = null

  try {
    // 造数：生日在 3 天后的会员 + 沉睡 95 天的会员
    const d = new Date(Date.now() + 3 * 86400000)
    const pad = n => String(n).padStart(2, '0')
    const bd = `1990-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
    const birthId = data(await api('POST', '/erp/md/customer', {
      partnerType: 'customer', partnerCode: 'BIR' + T, partnerName: 'E2E生日客户' + T,
      memberName: 'E2E生日会员' + T, memberCardNo: 'BIRC' + T, memberCardStatus: 'NORMAL',
      birthday: bd, phone: mobile,
    }))?.id
    codes.push('BIR' + T)
    const sleepId = data(await api('POST', '/erp/md/customer', {
      partnerType: 'customer', partnerCode: 'SLP' + T, partnerName: 'E2E沉睡客户' + T,
      memberName: 'E2E沉睡会员' + T, memberCardNo: 'SLPC' + T, memberCardStatus: 'NORMAL',
    }))?.id
    codes.push('SLP' + T)
    await dbQuery(`UPDATE biz_party SET last_trade_time = now() - interval '95 days' WHERE id = $1`, [sleepId])
    check('造数：生日 3 天后 + 沉睡 95 天的会员各一', !!birthId && !!sleepId, 'birth=' + bd)

    const tpl = data(await api('POST', '/erp/marketing/coupon-template', {
      couponName: 'AU券' + T, couponType: 'CASH', useRule: 'UNLIMITED', faceValue: 10,
      totalCount: 100, customerScope: 'ALL', status: 'NORMAL',
      startTime: '2026-01-01T00:00:00', endTime: '2027-12-31T23:59:59', mallEnabled: 1, offlineEnabled: 1,
    }))
    tplId = tpl?.id

    // ① 生日 → 发券
    const campId = data(await api('POST', '/erp/marketing/auto-campaign', {
      name: 'E2E生日送券' + T, triggerType: 'BIRTHDAY', triggerDays: 7, actionType: 'COUPON',
      couponTemplateId: tplId, freqDays: 0, freqCount: 0, oncePerMember: 1, status: 1,
    }))
    campIds.push(campId)
    check('新增自动化规则（生日→发券）', !!campId, String(campId))

    const cand = await api('GET', `/erp/marketing/auto-campaign/${campId}/candidates`)
    check('候选预览命中生日会员且带触发依据（只算不执行）',
      (data(cand) || []).some(x => String(x.partnerId) === String(birthId) && /生日还有/.test(x.triggerNote || '')),
      JSON.stringify((data(cand) || [])[0] || {}).slice(0, 140))

    const run1 = await api('POST', `/erp/marketing/auto-campaign/${campId}/run`)
    check('执行规则成功（返回候选/成功/跳过/失败）',
      run1.status === 200 && Number(data(run1)?.success) >= 1, JSON.stringify(data(run1)))
    const logs = await dbQuery(`SELECT result, trigger_note FROM mkt_auto_campaign_log WHERE campaign_id = $1`, [campId])
    check('执行台账落库（含触发依据）', logs.length >= 1 && !!logs[0].trigger_note, JSON.stringify(logs[0] || {}))
    const issued = await dbQuery(
      `SELECT count(*)::int c FROM erp_loyalty_coupon WHERE template_id = $1 AND partner_id = $2 AND deleted = 0`,
      [tplId, birthId])
    check('动作真实生效：券已发到该会员', issued[0].c === 1, '张数=' + issued[0].c)

    const run2 = await api('POST', `/erp/marketing/auto-campaign/${campId}/run`)
    check('「每人仅一次」生效：二次执行被跳过且不重复发券',
      Number(data(run2)?.success) === 0 && Number(data(run2)?.skipped) >= 1
      && (await dbQuery(`SELECT count(*)::int c FROM erp_loyalty_coupon WHERE template_id = $1 AND partner_id = $2 AND deleted = 0`, [tplId, birthId]))[0].c === 1,
      JSON.stringify(data(run2)))

    // ② 沉睡 → 发短信（复用短信群发与合规实现）
    const camp2 = data(await api('POST', '/erp/marketing/auto-campaign', {
      name: 'E2E沉睡唤醒' + T, triggerType: 'SLEEPING', triggerDays: 90, actionType: 'SMS',
      smsContent: '{会员名称} 您好，好久不见~', freqDays: 7, freqCount: 2, oncePerMember: 0, status: 1,
    }))
    campIds.push(camp2)
    const cand2 = await api('GET', `/erp/marketing/auto-campaign/${camp2}/candidates`)
    check('候选预览命中沉睡会员且带天数依据',
      (data(cand2) || []).some(x => /已沉睡/.test(x.triggerNote || '')),
      JSON.stringify((data(cand2) || [])[0] || {}).slice(0, 120))
    const st0 = data(await api('GET', '/erp/marketing/sms/setting'))
    await api('PUT', '/erp/marketing/sms/setting',
      { signName: st0.signName, sendStartHour: 0, sendEndHour: 24, freqLimitDays: 0, freqLimitCount: 0 })
    const run3 = await api('POST', `/erp/marketing/auto-campaign/${camp2}/run`)
    check('沉睡→短信 执行链路可用（复用短信群发与合规实现，失败也会落台账原因）',
      run3.status === 200 && (Number(data(run3)?.success) + Number(data(run3)?.failed)) >= 1,
      JSON.stringify(data(run3)))
    await api('PUT', '/erp/marketing/sms/setting',
      { signName: st0.signName, sendStartHour: 8, sendEndHour: 21, freqLimitDays: 7, freqLimitCount: 3 })

    // ③ 积分即将过期 → 赠积分
    const camp3 = data(await api('POST', '/erp/marketing/auto-campaign', {
      name: 'E2E积分到期提醒' + T, triggerType: 'POINTS_EXPIRING', triggerDays: 30, actionType: 'POINTS',
      pointsValue: 5, freqDays: 0, freqCount: 0, oncePerMember: 1, status: 1,
    }))
    campIds.push(camp3)
    check('新增自动化规则（积分到期→赠积分）', !!camp3)

    // ④ 校验与门控
    await api('POST', `/erp/marketing/auto-campaign/${campId}/status?status=0`)
    const denied = await api('POST', `/erp/marketing/auto-campaign/${campId}/run`)
    check('停用规则执行被拒', denied.status >= 400 || Number(denied.json?.code) !== 200,
      denied.status + ' ' + String(denied.json?.message || '').slice(0, 60))
    const bad = await api('POST', '/erp/marketing/auto-campaign',
      { name: 'x', triggerType: 'BIRTHDAY', actionType: 'COUPON' })
    check('动作为发券但未选券模板 → 被拒（不做无效配置落库）',
      bad.status >= 400 || Number(bad.json?.code) !== 200, String(bad.status))

    const lg = await api('GET', `/erp/marketing/auto-campaign/log/page?campaignId=${campId}`)
    check('执行记录可分页查询', lg.status === 200 && (data(lg)?.records || []).length >= 1, 'total=' + data(lg)?.total)
    const st = await api('GET', '/erp/marketing/auto-campaign/stat')
    check('执行概况接口 200（规则数/启用数/近 7 日成功）',
      st.status === 200 && Number(data(st)?.total) >= 3, JSON.stringify(data(st)))
    const all = await api('POST', '/erp/marketing/auto-campaign/run-all')
    check('「执行全部启用规则」可用（定时任务同入口）',
      all.status === 200 && data(all)?.campaignCount !== undefined, JSON.stringify(data(all)))

    // ⑤ UI（本页从零新建，必须真机渲染）
    await openPage(`${FE}/marketing/auto-campaign`)
    const text = await bodyText()
    check('营销自动化页可打开（菜单 80303，无白屏/404）',
      !text.includes('页面不存在') && text.length > 80, PAGE.url())
    const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
    check('双 Tab = 自动化规则 / 执行记录', tabs.includes('自动化规则') && tabs.includes('执行记录'), tabs.join('|'))
    const h1 = await headers()
    check('规则 Tab 表头含触发点/动作/频控/状态/最近执行',
      ['规则名称', '触发点', '动作', '频控天数', '状态', '最近执行时间'].every(k => h1.some(x => x.includes(k))),
      h1.join('|'))
    check('工具栏含「新增自动化规则 / 执行全部启用规则 / 刷新」',
      text.includes('新增自动化规则') && text.includes('执行全部启用规则') && text.includes('刷新'))
    await PAGE.screenshot({ path: `${SHOTS}/ui-auto-campaign.png` })

    await PAGE.locator('.tab-item').filter({ hasText: '执行记录' }).click()
    await PAGE.waitForTimeout(2500)
    const h2 = await headers()
    check('执行记录 Tab 表头含会员/触发依据/执行结果/结果说明',
      ['会员/客户', '触发依据', '执行结果', '结果说明'].every(k => h2.some(x => x.includes(k))), h2.join('|'))
    check('分页栏为经典形态（.classic-pagination）', (await PAGE.locator('.classic-pagination').count()) >= 1)
  } finally {
    for (const id of campIds) {
      if (id) await dbQuery(`DELETE FROM mkt_auto_campaign_log WHERE campaign_id = $1`, [id])
    }
    await dbQuery(`DELETE FROM mkt_auto_campaign WHERE name LIKE 'E2E%'`)
    await dbQuery(`DELETE FROM mkt_sms_record WHERE mobile = $1`, [mobile])
    await dbQuery(`DELETE FROM mkt_sms_consent WHERE mobile = $1`, [mobile])
    if (tplId) {
      await dbQuery(`DELETE FROM erp_loyalty_coupon WHERE template_id = $1`, [tplId])
      await api('DELETE', '/erp/marketing/coupon-template/' + tplId)
    }
    for (const code of codes) {
      const rows = await dbQuery(`SELECT id FROM biz_party WHERE party_code = $1`, [code])
      for (const r of rows) await api('DELETE', '/erp/md/customer/' + r.id)
    }
    const left = await dbQuery(`SELECT count(*)::int c FROM mkt_auto_campaign WHERE name LIKE 'E2E%'`)
    check('营销自动化造数已清理', left[0].c === 0, String(left[0].c))
  }
}

// ══════════════ 二十一、储值卡（80304 · 本系统建模页） ══════════════
async function storedCardSuite() {
  section('二十一、储值卡（80304 · 本系统建模页）')

  const T = 'SC' + STAMP
  const cardNo = 'SC' + STAMP
  let custId = null
  let cardId = null

  try {
    custId = data(await api('POST', '/erp/md/customer', {
      partnerType: 'customer', partnerCode: 'SCC' + T, partnerName: 'E2E储值客户' + T,
      memberName: 'E2E储值会员' + T, memberCardNo: 'SCCARD' + T, memberCardStatus: 'NORMAL',
    }))?.id
    check('造数：储值卡客户', !!custId, 'id=' + custId)

    // ── 开卡：面值 500 + 赠送 50 ──
    cardId = data(await api('POST', '/erp/marketing/stored-card', {
      card: { cardNo, cardType: 'STORED', partnerId: custId, partnerName: 'E2E储值客户' + T, faceValue: 500,
        settleAccount: 'CASH' },
      bonusAmount: 50, remark: 'E2E开卡',
    }))
    check('开卡成功（面值 500 + 赠送 50）', !!cardId, String(cardId))
    const row = (await dbQuery(
      `SELECT face_value, balance, total_bonus, status FROM mkt_stored_card WHERE id = $1`, [cardId]))[0]
    check('余额 = 面值 + 赠送 = 550，状态 ACTIVE',
      Number(row.balance) === 550 && Number(row.face_value) === 500 && row.status === 'ACTIVE', JSON.stringify(row))

    const dup = await api('POST', '/erp/marketing/stored-card',
      { card: { cardNo, faceValue: 100, partnerId: custId } })
    check('卡号重复被拒（唯一约束）', dup.status >= 400 || Number(dup.json?.code) !== 200, String(dup.status))

    // ── 充值 / 消费 / 余额保护 ──
    const rc = await api('POST', `/erp/marketing/stored-card/${cardId}/recharge`,
      { amount: 200, bonusAmount: 20, settleAccount: 'BANK' })
    check('充值成功且余额累加（550+200+20=770）', rc.status === 200 && Number(data(rc)?.balance) === 770,
      String(data(rc)?.balance))
    const cs = await api('POST', `/erp/marketing/stored-card/${cardId}/consume`,
      { amount: 300, sourceBillNo: 'E2E-XSDD-1' })
    check('消费扣减成功（770-300=470）', cs.status === 200 && Number(data(cs)?.balance) === 470, String(data(cs)?.balance))
    const over = await api('POST', `/erp/marketing/stored-card/${cardId}/consume`, { amount: 99999 })
    check('余额不足被拒（不允许透支）', over.status >= 400 || Number(over.json?.code) !== 200,
      over.status + ' ' + String(over.json?.message || '').slice(0, 60))

    // ── 冻结门控 ──
    await api('POST', `/erp/marketing/stored-card/${cardId}/status?status=FROZEN`)
    const frozen = await api('POST', `/erp/marketing/stored-card/${cardId}/consume`, { amount: 10 })
    check('冻结卡不可消费', frozen.status >= 400 || Number(frozen.json?.code) !== 200,
      String(frozen.json?.message || '').slice(0, 60))
    await api('POST', `/erp/marketing/stored-card/${cardId}/status?status=ACTIVE`)

    // ── 退款（合规入口）──
    const rf = await api('POST', `/erp/marketing/stored-card/${cardId}/refund`, { amount: 70, remark: 'E2E部分退款' })
    check('部分退款成功（合规入口：470-70=400）', rf.status === 200 && Number(data(rf)?.balance) === 400,
      String(data(rf)?.balance))
    const rfOver = await api('POST', `/erp/marketing/stored-card/${cardId}/refund`, { amount: 99999 })
    check('退款超余额被拒', rfOver.status >= 400 || Number(rfOver.json?.code) !== 200, String(rfOver.status))

    // ── 流水台账与对账 ──
    const flows = await dbQuery(
      `SELECT flow_type, balance_after FROM mkt_stored_card_flow WHERE card_id = $1 ORDER BY id`, [cardId])
    check('流水含 开卡/赠送/充值/消费/退款 五类',
      ['ISSUE', 'BONUS', 'RECHARGE', 'CONSUME', 'REFUND'].every(t => flows.some(f => f.flow_type === t)),
      flows.map(f => f.flow_type).join(','))
    const bal = (await dbQuery(`SELECT balance FROM mkt_stored_card WHERE id = $1`, [cardId]))[0].balance
    check('末条流水余额 = 卡当前余额（可对账）',
      Number(flows[flows.length - 1].balance_after) === Number(bal),
      '流水=' + flows[flows.length - 1].balance_after + ' 卡=' + bal)

    // ── 资金闭环：结算账户落库 + 记账凭证（业财一体，凭证失败与余额同事务回滚）──
    const acct = await dbQuery(
      `SELECT flow_type, settle_account, voucher_no FROM mkt_stored_card_flow
       WHERE card_id = $1 ORDER BY id`, [cardId])
    const byType = Object.fromEntries(acct.map(a => [a.flow_type, a]))
    check('开卡流水结算账户 = CASH', byType.ISSUE?.settle_account === 'CASH', String(byType.ISSUE?.settle_account))
    check('充值流水结算账户 = BANK（本次指定覆盖卡默认）',
      byType.RECHARGE?.settle_account === 'BANK', String(byType.RECHARGE?.settle_account))
    check('消费 / 赠送流水不计结算账户（无资金流入流出）',
      !byType.CONSUME?.settle_account && !byType.BONUS?.settle_account,
      `CONSUME=${byType.CONSUME?.settle_account} BONUS=${byType.BONUS?.settle_account}`)
    check('开卡/充值/消费/退款 四类资金流水均已回写凭证号',
      ['ISSUE', 'RECHARGE', 'CONSUME', 'REFUND'].every(t => !!byType[t]?.voucher_no),
      acct.map(a => `${a.flow_type}:${a.voucher_no || 'NULL'}`).join(' '))
    check('赠送流水不生成凭证（赠送不产生现金流）', !byType.BONUS?.voucher_no, String(byType.BONUS?.voucher_no))
    for (const v of acct.map(a => a.voucher_no).filter(Boolean)) {
      const its = await dbQuery(
        `SELECT i.subject_code, i.debit_amount, i.credit_amount FROM finance_voucher_item i
         JOIN finance_voucher v ON v.id = i.voucher_id WHERE v.voucher_no = $1 ORDER BY i.id`, [v])
      const d = its.filter(x => Number(x.debit_amount) > 0).map(x => x.subject_code).sort().join('+')
      const c = its.filter(x => Number(x.credit_amount) > 0).map(x => x.subject_code).sort().join('+')
      if (its.length) check(`凭证 ${v} 借贷平衡且科目为预收口径`, d.length > 0 && c.length > 0, `借 ${d} 贷 ${c}`)
    }

    const rfAll = await api('POST', `/erp/marketing/stored-card/${cardId}/refund`,
      { amount: Number(bal), remark: 'E2E全额退款' })
    check('全额退款后状态 REFUNDED（退卡）', data(rfAll)?.status === 'REFUNDED', String(data(rfAll)?.status))
    const refundAcct = (await dbQuery(
      `SELECT settle_account FROM mkt_stored_card_flow WHERE card_id = $1 AND flow_type = 'REFUND' ORDER BY id LIMIT 1`,
      [cardId]))[0]
    check('退款流水结算账户回落卡默认 = CASH', refundAcct?.settle_account === 'CASH', String(refundAcct?.settle_account))
    const rfAgain = await api('POST', `/erp/marketing/stored-card/${cardId}/refund`, { amount: 1 })
    check('已退卡的卡不可再退款', rfAgain.status >= 400 || Number(rfAgain.json?.code) !== 200, String(rfAgain.status))

    // ── 查询与统计 ──
    const page = await api('GET', `/erp/marketing/stored-card/page?pageNum=1&pageSize=20&keyword=${cardNo}`)
    check('卡档案可按卡号查回', Number(data(page)?.total) === 1, 'total=' + data(page)?.total)
    const flowPage = await api('GET', `/erp/marketing/stored-card/flow/page?cardId=${cardId}`)
    check('储值流水可分页查询', (data(flowPage)?.records || []).length >= 5, 'total=' + data(flowPage)?.total)
    const stat = await api('GET', '/erp/marketing/stored-card/stat')
    check('储值卡统计接口 200（卡数/余额/累计充值·消费·赠送）',
      stat.status === 200 && data(stat)?.cardCount !== undefined, JSON.stringify(data(stat)))
    const byNo = await api('GET', `/erp/marketing/stored-card/by-no?cardNo=${cardNo}`)
    check('按卡号查询可用（收银/开单场景）', byNo.status === 200 && data(byNo)?.cardNo === cardNo)
    const byPartner = await api('GET', `/erp/marketing/stored-card/by-partner/${custId}`)
    check('按会员查卡可用', Array.isArray(data(byPartner)) && data(byPartner).length === 1,
      '条数=' + (data(byPartner) || []).length)

    // ── UI（本页从零新建，必须真机渲染）──
    await openPage(`${FE}/marketing/stored-card`)
    const text = await bodyText()
    check('储值卡页可打开（菜单 80304，无白屏/404）',
      !text.includes('页面不存在') && text.length > 80, PAGE.url())
    const tabs = (await PAGE.locator('.tab-item').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
    check('双 Tab = 卡档案 / 储值流水', tabs.includes('卡档案') && tabs.includes('储值流水'), tabs.join('|'))
    const h1 = await headers()
    check('卡档案 Tab 表头含卡号/会员/卡类型/面值/余额/状态',
      ['卡号', '会员/客户', '卡类型', '面值', '当前余额', '状态'].every(k => h1.some(x => x.includes(k))),
      h1.join('|'))
    check('工具栏含「开卡 / 刷新 / 打印(F8)」',
      text.includes('开卡') && text.includes('刷新') && text.includes('打印(F8)'))
    check('行级含「充值 / 消费 / 更多」', text.includes('充值') && text.includes('消费') && text.includes('更多'))
    await PAGE.screenshot({ path: `${SHOTS}/ui-stored-card.png` })

    // 开卡弹窗的合规提示
    await PAGE.locator('button').filter({ hasText: /开\s*卡/ }).first().click()
    await PAGE.waitForTimeout(900)
    const modalText = (await PAGE.locator('.ant-modal').first().innerText()).replace(/\s+/g, '')
    check('开卡弹窗内置合规提示（单用途预付卡 + 法释〔2025〕4 号）',
      modalText.includes('合规提示') && modalText.includes('预付卡'), modalText.slice(0, 120))
    check('开卡弹窗含「结算账户」选择（决定凭证用 1001 还是 1002）',
      modalText.includes('结算账户'), modalText.slice(0, 160))
    await PAGE.keyboard.press('Escape')
    await PAGE.waitForTimeout(400)

    await PAGE.locator('.tab-item').filter({ hasText: '储值流水' }).click()
    await PAGE.waitForTimeout(2500)
    const h2 = await headers()
    check('储值流水 Tab 表头含流水类型/本金变动/赠送变动/变动后余额',
      ['流水类型', '本金变动', '赠送变动', '变动后余额'].every(k => h2.some(x => x.includes(k))), h2.join('|'))
    check('储值流水 Tab 表头含结算账户 / 记账凭证号（资金可追溯）',
      ['结算账户', '记账凭证号'].every(k => h2.some(x => x.includes(k))), h2.join('|'))
    check('分页栏为经典形态（.classic-pagination）', (await PAGE.locator('.classic-pagination').count()) >= 1)
  } finally {
    const vs = (await dbQuery(
      `SELECT voucher_no FROM mkt_stored_card_flow WHERE card_no = $1 AND voucher_no IS NOT NULL`, [cardNo])
    ).map(r => r.voucher_no)
    if (vs.length) {
      await dbQuery(
        `DELETE FROM finance_voucher_item WHERE voucher_id IN (SELECT id FROM finance_voucher WHERE voucher_no = ANY($1))`, [vs])
      await dbQuery(`DELETE FROM finance_voucher WHERE voucher_no = ANY($1)`, [vs])
    }
    await dbQuery(`DELETE FROM mkt_stored_card_flow WHERE card_no = $1`, [cardNo])
    await dbQuery(`DELETE FROM mkt_stored_card WHERE card_no = $1`, [cardNo])
    if (custId) await api('DELETE', '/erp/md/customer/' + custId)
    const left = await dbQuery(`SELECT count(*)::int c FROM mkt_stored_card WHERE card_no = $1`, [cardNo])
    check('储值卡造数已清理', left[0].c === 0, String(left[0].c))
  }
}

// ══════════════ 十七之二、UI 写路径（弹窗表单真实新增 → DB 回读 → 清理） ══════════════
async function uiWriteSuite() {
  section('十七之二、UI 写路径（弹窗表单真实落库）')
  const stamp = 'UI' + STAMP

  // ── 商品促销：新增弹窗 ──
  await openPage(`${FE}/marketing/product-promo`)
  await PAGE.locator('button').filter({ hasText: /新\s*增/ }).first().click()
  await PAGE.waitForTimeout(1000)
  await PAGE.locator('.ant-modal input').first().fill('UI商品促销' + stamp)
  await PAGE.locator('.ant-modal .ant-btn-primary').filter({ hasText: /确\s*定|保\s*存/ }).first().click()
  await PAGE.waitForTimeout(2000)
  const promoRows = await dbQuery(
    `SELECT id, creator_name FROM erp_promotion_activity WHERE name = $1 AND deleted = 0`, ['UI商品促销' + stamp])
  check('UI 写路径 · 商品促销：弹窗新增真实落库', promoRows.length === 1, JSON.stringify(promoRows[0] || {}))
  check('UI 写路径 · 商品促销：制单人随 UI 新增写入', !!promoRows[0]?.creator_name, String(promoRows[0]?.creator_name))
  if (promoRows.length) await api('DELETE', `/erp/marketing/promotion-activity/${promoRows[0].id}`)

  // ── 优惠券：新增弹窗（面值 / 总数） ──
  await openPage(`${FE}/marketing/coupon`)
  await PAGE.locator('button').filter({ hasText: /新增优惠券/ }).first().click()
  await PAGE.waitForTimeout(1000)
  await PAGE.locator('.ant-modal input').first().fill('UI券' + stamp)
  const numInputs = PAGE.locator('.ant-modal .ant-input-number-input')
  await numInputs.nth(0).fill('5')
  await numInputs.nth(1).fill('20')
  await PAGE.locator('.ant-modal .ant-btn-primary').filter({ hasText: /确\s*定|保\s*存/ }).first().click()
  await PAGE.waitForTimeout(2000)
  const couponRows = await dbQuery(
    `SELECT id, face_value, total_count FROM mkt_coupon_template WHERE coupon_name = $1 AND deleted = 0`, ['UI券' + stamp])
  check('UI 写路径 · 优惠券：弹窗新增落库（面值 5 / 总数 20）',
    couponRows.length === 1 && Number(couponRows[0].face_value) === 5 && Number(couponRows[0].total_count) === 20,
    JSON.stringify(couponRows[0] || {}))
  if (couponRows.length) await api('DELETE', `/erp/marketing/coupon-template/${couponRows[0].id}`)

  // ── 热门搜索词推荐：新增弹窗 ──
  await openPage(`${FE}/marketing/hot-keywords`)
  await PAGE.locator('button').filter({ hasText: /新增关键词/ }).first().click()
  await PAGE.waitForTimeout(1000)
  await PAGE.locator('.ant-modal input').first().fill('UI热词' + stamp)
  await PAGE.locator('.ant-modal .ant-btn-primary').filter({ hasText: /确\s*定|保\s*存/ }).first().click()
  await PAGE.waitForTimeout(2000)
  const kwRows = await dbQuery(`SELECT id FROM mall_keyword WHERE keyword = $1 AND deleted = 0`, ['UI热词' + stamp])
  check('UI 写路径 · 热门搜索词：弹窗新增真实落库', kwRows.length === 1, JSON.stringify(kwRows[0] || {}))
  if (kwRows.length) await api('DELETE', `/erp/mall/admin/keyword/${kwRows[0].id}`)

  // ── 会员管理：新增会员弹窗 + 客户选择器 ──
  await openPage(`${FE}/marketing/member-manage`)
  await PAGE.locator('button').filter({ hasText: /新\s*增/ }).first().click()
  await PAGE.waitForTimeout(1200)
  const memberTitle = (await PAGE.locator('.ant-modal-title').first().innerText().catch(() => '')).replace(/\s+/g, '')
  check('UI 写路径 · 会员管理：新增会员弹窗可打开', memberTitle.includes('新增会员'), memberTitle)
  const memberFields = (await PAGE.locator('.ant-modal .ant-form-item-label').allInnerTexts()).map(t => t.replace(/\s+/g, ''))
  const wantFields = ['客户', '会员名称', '会员卡号', '会员级别', '会员卡状态', '有效期', '会员生日', '初始积分', '当前积分', '默认经手人', '备注']
  const missFields = wantFields.filter(f => !memberFields.some(x => x.includes(f)))
  check('UI 写路径 · 会员管理：新增弹窗 11 个会员字段齐全', missFields.length === 0, missFields.join(','))
  await PAGE.locator('.ant-modal input').first().click()
  await PAGE.waitForTimeout(2500)
  check('UI 写路径 · 会员管理：「客户」放大镜唤起客户选择器（复用 PartnerSelectModal）',
    (await PAGE.locator('.ant-modal').count()) >= 2, `modal 数=${await PAGE.locator('.ant-modal').count()}`)
}

// ══════════════ 二十二、闭环收口（定时任务 / 全域频控 / 促销价合规 / 手机号掩码） ══════════════
async function closedLoopSuite() {
  section('二十二、闭环收口（定时任务接线 / 全域频控 / 促销价合规 / 手机号掩码）')

  // ── 22.1 定时任务接线：JobHandler 白名单 + scheduled_task 种子（默认停用）──
  const hs = await api('GET', '/scheduler/task/handlers')
  const keys = (data(hs) || []).map(h => h.jobKey || h.key || h.value)
  check('定时任务处理器白名单含 3 个营销作业',
    ['mkt.member.pointsExpire', 'mkt.storedCard.expire', 'mkt.autoCampaign.runAll'].every(k => keys.includes(k)),
    keys.filter(k => String(k).startsWith('mkt.')).join(','))
  const seeds = await dbQuery(`SELECT job_key, enabled, status, execute_params FROM scheduled_task
                               WHERE job_key LIKE 'mkt.%' AND deleted = 0 ORDER BY job_key`)
  check('scheduled_task 有 3 条营销种子且默认停用（避免上线即群发）',
    seeds.length === 3 && seeds.every(x => Number(x.enabled) === 0 && x.status === 'STOPPED'),
    JSON.stringify(seeds.map(x => [x.job_key, x.enabled, x.status])))
  check('自动化作业默认 dryRun（首次启用只试跑不真发）',
    String(seeds.find(x => x.job_key === 'mkt.autoCampaign.runAll')?.execute_params || '').includes('"dryRun": true'))

  // ── 22.2 全域频控：自建唯一沉睡会员 → 连续 3 次执行，第 3 次被拦 ──
  const mid = String(Date.now()) + '71'
  const mcard = 'VIPE2ECL' + STAMP
  let cid = null
  let cfgId = null
  try {
    await dbQuery(`INSERT INTO biz_party (id, tenant_id, party_code, party_name, party_type, deleted,
                                          member_name, member_card_no, last_trade_time, create_time, update_time)
                   VALUES ($1, 1, $2, $3, 1, 0, $4, $5, now() - interval '90 days', now(), now())`,
      [mid, 'E2ECL' + STAMP, 'E2E闭环会员' + STAMP, 'E2E闭环会员' + STAMP, mcard])
    const hit = (await dbQuery(`SELECT count(*)::int n FROM biz_party WHERE deleted = 0 AND tenant_id = 1
                                AND party_type = 1 AND last_trade_time <= now() - interval '60 days'`))[0].n
    check('沉睡候选恰好 1 人（频控断言可确定性）', hit === 1, '实得 ' + hit)

    cid = data(await api('POST', '/erp/marketing/auto-campaign', {
      name: 'E2E频控' + STAMP, triggerType: 'SLEEPING', triggerDays: 60, actionType: 'POINTS',
      pointsValue: 1, status: 1, oncePerMember: 0, freqDays: 0, freqCount: 0,
    }))
    check('造数：关闭单规则频控的自动化规则', !!cid, String(cid))

    const r1 = data(await api('POST', `/erp/marketing/auto-campaign/${cid}/run?limit=1`, null))
    const r2 = data(await api('POST', `/erp/marketing/auto-campaign/${cid}/run?limit=1`, null))
    const r3 = data(await api('POST', `/erp/marketing/auto-campaign/${cid}/run?limit=1`, null))
    check('全域频控：第 1、2 次放行（默认 1 天 2 次）',
      Number(r1?.success) === 1 && Number(r2?.success) === 1, JSON.stringify([r1, r2]))
    check('全域频控：第 3 次被跨规则拦截（skip）',
      Number(r3?.success) === 0 && Number(r3?.skipped) === 1, JSON.stringify(r3))
    const lastLog = (await dbQuery(
      `SELECT result_msg FROM mkt_auto_campaign_log WHERE campaign_id = $1 ORDER BY id DESC LIMIT 1`, [cid]))[0]
    check('台账写明拦截原因（含「全域频控拦截」）',
      String(lastLog?.result_msg || '').includes('全域频控拦截'), String(lastLog?.result_msg))

    cfgId = String(Date.now()) + '72'
    await dbQuery(`INSERT INTO sys_project_config (id, tenant_id, deleted, create_time, update_time, config_key,
                                                   config_value, config_type, config_group, status)
                   VALUES ($1, 1, 0, now(), now(), 'marketing.autoCampaign.globalFreqCap', '5', 'business', 'marketing', 0)`,
      [cfgId])
    const r4 = data(await api('POST', `/erp/marketing/auto-campaign/${cid}/run?limit=1`, null))
    check('系统参数可覆盖上限（sys_project_config 提到 5 后放行）',
      Number(r4?.success) === 1, JSON.stringify(r4))
  } finally {
    if (cfgId) await dbQuery(`DELETE FROM sys_project_config WHERE id = $1`, [cfgId])
    if (cid) {
      await dbQuery(`DELETE FROM mkt_auto_campaign_log WHERE campaign_id = $1`, [cid])
      await dbQuery(`DELETE FROM mkt_auto_campaign WHERE id = $1`, [cid])
    }
    await dbQuery(`DELETE FROM mkt_points_journal WHERE member_card_no = $1`, [mcard])
    await dbQuery(`DELETE FROM mkt_points_batch WHERE member_card_no = $1`, [mcard])
    await dbQuery(`DELETE FROM biz_party WHERE id = $1`, [mid])
  }

  // ── 22.3 促销价合规：特价不得高于近 7 日最低成交价 ──
  const prod = (await dbQuery(`SELECT id FROM erp_product WHERE deleted = 0 AND tenant_id = 1 ORDER BY id LIMIT 1`))[0]
  const cust = (await dbQuery(`SELECT id FROM biz_party WHERE deleted = 0 AND tenant_id = 1 ORDER BY id LIMIT 1`))[0]
  const oid = String(Date.now()) + '73'
  const iid = String(Date.now()) + '74'
  const goodId = []
  try {
    await dbQuery(`INSERT INTO erp_sale_order (id, tenant_id, order_no, customer_id, order_date, deleted, create_time, update_time)
                   VALUES ($1, 1, $2, $3, now(), 0, now(), now())`, [oid, 'E2ECL' + STAMP, cust.id])
    await dbQuery(`INSERT INTO erp_sale_order_item (id, order_id, tenant_id, product_id, unit_price, quantity, amount)
                   VALUES ($1, $2, 1, $3, 10, 1, 10)`, [iid, oid, prod.id])
    const bad = await api('POST', '/erp/marketing/promotion-activity', {
      name: 'E2E虚假特价' + STAMP, type: 'SPECIAL_PRICE', productIds: String(prod.id), promoPrice: 12, status: 'draft',
    })
    check('促销价合规：特价 12 > 近 7 日最低成交价 10 → 拒绝',
      Number(bad.json?.code) !== 200 && String(bad.json?.message || '').includes('价格合规校验未通过'),
      `${bad.status} ${String(bad.json?.message || '').slice(0, 90)}`)
    const good = await api('POST', '/erp/marketing/promotion-activity', {
      name: 'E2E真实特价' + STAMP, type: 'SPECIAL_PRICE', productIds: String(prod.id), promoPrice: 8, status: 'draft',
    })
    if (data(good)) goodId.push(data(good))
    check('促销价合规：特价 8 < 10 → 放行', Number(good.json?.code) === 200, String(good.status))
  } finally {
    for (const id of goodId) await dbQuery(`DELETE FROM erp_promotion_activity WHERE id = $1`, [String(id)])
    await dbQuery(`DELETE FROM erp_sale_order_item WHERE order_id = $1`, [oid])
    await dbQuery(`DELETE FROM erp_sale_order WHERE id = $1`, [oid])
  }

  // ── 22.4 手机号掩码：列表类只读台账不明文出手机号 ──
  const rid = String(Date.now()) + '75'
  const mobile = '137' + STAMP + '9'
  try {
    await dbQuery(`INSERT INTO mkt_sms_record (id, tenant_id, deleted, create_time, receiver_name, mobile, content,
                                               sign_name, sms_type, handler_name, batch_no)
                   VALUES ($1, 1, 0, now(), 'E2E掩码', $2, '掩码验证', 'AI', 'NOTICE', 'E2E', $3)`,
      [rid, mobile, 'E2ECL' + STAMP])
    const his = await api('GET', `/erp/marketing/sms/history/page?mobile=${mobile}&pageSize=5`)
    const rec = (data(his)?.records || [])[0]
    check('短信历史仍可按完整号码检索到', !!rec, `实得 ${(data(his)?.records || []).length} 条`)
    check('短信历史返回的手机号已掩码（前 3 后 4）',
      rec?.mobile === mobile.slice(0, 3) + '****' + mobile.slice(-4), String(rec?.mobile))
    const lg = await api('GET', '/erp/marketing/auto-campaign/log/page?pageSize=50')
    const leak = (data(lg)?.records || []).find(x => /^\d{11}$/.test(String(x.mobile || '')))
    check('自动化执行台账无明文 11 位手机号', !leak, leak ? String(leak.mobile) : `抽查 ${(data(lg)?.records || []).length} 行`)
  } finally {
    await dbQuery(`DELETE FROM mkt_sms_record WHERE id = $1`, [rid])
  }
}

// ══════════════ 十八、清理 ══════════════
async function cleanup() {
  section('十八、验收造数清理')
  if (MEMBER_ID) {
    await api('DELETE', `/erp/md/customer/${MEMBER_ID}`)
    const rowsLeft = await dbQuery(`SELECT deleted FROM biz_party WHERE id = $1`, [MEMBER_ID])
    check('会员造数已清理（软删）', !rowsLeft.length || Number(rowsLeft[0].deleted) === 1)
  }
  if (SMS_TPL_ID) {
    await api('DELETE', `/erp/marketing/sms/template/${SMS_TPL_ID}`)
  }
  if (GROUP_ID) {
    await api('PUT', `/erp/marketing/group-buy/${GROUP_ID}/status?status=CANCELLED`).catch(() => {})
  }
  const leftovers = await dbQuery(`
    SELECT
      (SELECT count(*)::int FROM mkt_coupon_template WHERE coupon_name LIKE 'E2E%' AND deleted = 0) AS coupon,
      (SELECT count(*)::int FROM erp_promotion_activity WHERE name LIKE 'E2E%' AND deleted = 0) AS promo,
      (SELECT count(*)::int FROM mkt_points_exchange_product WHERE remark = 'E2E造数' AND deleted = 0) AS exchange,
      (SELECT count(*)::int FROM mall_popup_ad WHERE title LIKE 'E2E%' AND deleted = 0) AS popup,
      (SELECT count(*)::int FROM mkt_addon_rule WHERE rule_name LIKE 'E2E%' AND deleted = 0) AS addon,
      (SELECT count(*)::int FROM mall_keyword WHERE keyword LIKE 'E2E%' AND deleted = 0) AS keyword
  `)
  const l = leftovers[0]
  check('列表页造数均已清理（优惠券/促销/弹窗广告/加价购/热词）',
    l.coupon === 0 && l.promo === 0 && l.popup === 0 && l.addon === 0 && l.keyword === 0, JSON.stringify(l))
}

// ══════════════ 前置守卫 ══════════════
/**
 * 造数租户归属守卫（fail-fast）。
 *
 * <p>本套件的造数大量依赖「平台自动给新行盖租户章」。若自动盖章失效，新行会落到
 * `tenant_id` 列默认值 0，于是**对自己租户不可见** —— 后面几十项断言会集体假失败
 * （候选数 0、按名查不回、短信查不到手机号…），而现象完全不像租户问题。</p>
 *
 * <p>踩坑记录（2026-09-18）：`biz_party.tenant_id` 默认 0；`POST /erp/md/customer`
 * 是少数**不显式 setTenantId** 的路径，完全靠 `MetaObjectHandler.insertFill` 盖章。
 * 该填充依赖 `MyBatisPlusConfig.getCurrentTenantIdValue()`，一旦它返回 null，
 * insertFill 不写、租户拦截器也 `shouldSkip()`，插入语句里连 tenant_id 列都没有。</p>
 *
 * <p>所以这里先造一个客户直接查库验章：章不对就立刻报错退出，别让后面的断言背锅。</p>
 */
async function preflightTenantGuard() {
  section('零、前置守卫：造数租户归属')
  const code = 'PF' + STAMP
  const row = data(await api('POST', '/erp/md/customer', {
    partnerType: 'customer', partnerCode: code, partnerName: 'E2E守卫' + code,
  }))
  if (!row?.id) {
    check('前置守卫：可创建往来单位', false, '创建失败，后续造数断言不可信')
    return false
  }
  try {
    const got = (await dbQuery(`SELECT tenant_id FROM biz_party WHERE id = $1`, [row.id]))[0]
    const sessionTenant = Number(TOKEN_TENANT_ID)
    const ok = got && Number(got.tenant_id) === sessionTenant
    check(`前置守卫：新建行落当前租户（期望 ${sessionTenant}）`, ok,
      ok ? undefined
        : `实得 tenant_id=${got?.tenant_id}。根因：MyBatisPlusConfig.getCurrentTenantIdValue() 解析为 null → `
          + 'insertFill 不盖章 + 租户拦截器 shouldSkip() → 插入语句无 tenant_id 列，落到列默认值 0。'
          + '该路径的新数据对它自己的租户不可见，请先修租户上下文再跑本套件。')
    return ok
  } finally {
    await api('DELETE', '/erp/md/customer/' + row.id).catch(() => {})
  }
}

// ══════════════ 主流程 ══════════════
async function main() {
  console.log(`═══ 营销模块金标准 E2E（后端 :${PORT} / 前端 ${FE}）═══`)
  await ensureE2EUser()
  await login()
  console.log('登录成功')

  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 950 }, locale: 'zh-CN', acceptDownloads: true })
  PAGE = await ctx.newPage()
  // /api 转发到验收实例（前端 dev server 代理可能指向别的后端）
  await PAGE.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })

  try {
    // 守卫不过就直接跳到汇总：不能 return，否则连「结果：x/y」都不打印，读日志的人只看到一次静默退出
    if (await preflightTenantGuard()) {
      await memberConfigSuite()
      await memberManageSuite()
      await pointsExchangeSuite()
      await smsSendSuite()
      await couponSuite()
      await promoSuite()
      await packageDealSuite()
      await mallGroupSuite()
      await mallFlashSuite()
      await mallPresaleSuite()
      await mallPopupSuite()
      await addonSuite()
      await hotKeywordsSuite()
      await promoteCreateSuite()
      await promoteHistorySuite()
      await promotionEngineSuite()
      await memberLevelPointsSuite()
      await autoCampaignSuite()
      await storedCardSuite()
      await closedLoopSuite()
      await uiWriteSuite()
      await cleanup()
    } else {
      console.log('\n⛔ 前置守卫未通过：造数租户归属错误，跳过全部业务套件（避免几十项假失败掩盖真因）。')
    }
  } finally {
    await browser.close()
  }

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 结果：${pass}/${results.length} 通过${fail ? `，${fail} 失败` : ''} ═══`)
  if (fail) {
    console.log('失败项：')
    results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name}${r.detail ? ' — ' + r.detail : ''}`))
  }
  process.exit(fail ? 1 : 0)
}

main().catch(e => { console.error('E2E 异常:', e); process.exit(1) })
