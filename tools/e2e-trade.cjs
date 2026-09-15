/*
 * 交易模块（商城管理 / 商城设置 / 支付中心 / 外部平台，共 23 个页面组件）模块级金标准端到端验证
 *
 * 口径依据：`docs/Yh-Spec/手动整理对标开发文档/交易模块/*.md`（商品上架 / 单位显示 / 店铺设置 /
 *   基础设置 / 运费设置 / 商城装修 / 商城订单 / 订单处理 / 支付请求 / 支付记录 / 退款管理 /
 *   每日对账 / 购物车 / 渠道配置 / 外部订单 / 数据同步 / 商品组合 / 关键词库 / 公告设置 /
 *   买家账号 / 买家申请管理）+ `_开发指南-金标准.md`（ErrorBoundary > PageContainer(full-height) 骨架）。
 *   所有后端路径均由源码注解核对（类级 @RequestMapping + 方法级 @GetMapping/@PutMapping…），
 *   逐条对照表见交付报告；脚本内不出现任何「猜」出来的路径。
 *
 * 验收方式：**先只读对账，再造数/改库回读，最后按原值复原**。每个「对账」类断言都先直连 PostgreSQL
 *   取期望值（含 tenant_id / deleted=0 / @TableLogic / 多租户拦截器口径），再与 HTTP 返回值逐项比较；
 *   不使用「只断言 HTTP 200」的空炮。少数依赖环境前置数据（种子库存 / 权限账号）的断言在断言名或
 *   detail 中显式写明降级原因（沿用 e2e-api-monitor.cjs「原生点击失败则回退 JS 触发并如实注明」的写法），
 *   不做静默通过。
 *
 * 用法：node tools/e2e-trade.cjs                 （默认后端 5680、前端 5656）
 *      ERP_PORT=5690 FE_URL=http://localhost:5656 node tools/e2e-trade.cjs
 *      E2E_USER=admin E2E_PWD=xxx node tools/e2e-trade.cjs   （需写库校验时用有权限的账号）
 *
 * 副作用与清理：
 *   1) 造数：仅 1 条装修配置（name 前缀 `E2E-DECO-`），结束时 DELETE 清空并断言为 0；
 *   2) 改库：`erp_product.mall_shelf_status`、`erp_product.unit_display`、
 *      `erp_product_unit.unit_display_type`（单商品）、`tenant_shop_config.shop_desc` 各改一次，
 *      每次均读原值 → 改 → 回读校验 → **按原值复原**，第十节再复核一次复原结果；
 *   3) 只读接口（分页/统计/待对账日期等）不产生数据；不触发下单、不触发对账 execute、不删既有业务数据；
 *   4) 截图落到 `I:/AI-Ready/tool-results/e2e-trade/`（目录不存在时自动创建）。
 */
const { chromium } = require('I:/AI-Ready/frontend/node_modules/playwright')
const http = require('http')
const fs = require('fs')
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const FE = process.env.FE_URL || 'http://localhost:5656'
const PORT = Number(process.env.ERP_PORT || 5680)
const DSN = { host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' }
const E2E_USER = process.env.E2E_USER || 'e2e_dispatch'
const E2E_PWD = process.env.E2E_PWD || 'admin123'
const SHOTS = 'I:/AI-Ready/tool-results/e2e-trade'
const ADMIN_SRC = 'I:/AI-Ready/frontend/apps/pc-admin/src'

function rawReq(method, reqPath, body, token, extraHeaders) {
  return new Promise((resolve, reject) => {
    const data = body === undefined || body === null ? body : JSON.stringify(body)
    const headers = {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(extraHeaders || {})
    }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    const r = http.request({ hostname: 'localhost', port: PORT, path: encodeURI('/api' + reqPath), method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        let json = null
        try { json = JSON.parse(buf.toString('utf8')) } catch (e) { /* ignore */ }
        resolve({ status: res.statusCode, headers: res.headers, buf, json })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function dbQuery(sql, params) {
  const client = new Client(DSN)
  await client.connect()
  try {
    const res = await client.query(sql, params)
    return res.rows
  } finally { await client.end() }
}

/** 取单行（无行时返回 {}），避免每处都写 [0] || {} */
async function dbOne(sql, params) {
  const rows = await dbQuery(sql, params)
  return rows[0] || {}
}

const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail == null ? '' : String(detail).slice(0, 220) })
  console.log(`${ok ? '  ✅' : '  ❌'} ${name}${detail ? ' — ' + String(detail).slice(0, 180) : ''}`)
}

let TOKEN = null
async function login() {
  const cap = await rawReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await rawReq('POST', '/auth/login', {
    username: E2E_USER, password: E2E_PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  TOKEN = token
}

/**
 * 统一解包：本模块后端存在三种返回形态
 *   ① Result<T> / ApiResponse<T>（{ code, message, data }）
 *   ② 裸 Page/PageResult（{ records, total, … }，如 ProductKitController#page）
 *   ③ 裸 List（如 /menu/user/client/{clientType} 之外的少数端点）
 * 只认公共字段（records/total），不猜业务字段。
 */
function data(res) {
  const j = res?.json
  if (j === null || j === undefined) return undefined
  if (Array.isArray(j)) return j
  if (typeof j === 'object' && Array.isArray(j.records) && 'total' in j) return j
  return j.data
}
const pageTotal = (res) => { const d = data(res); return d && typeof d === 'object' ? Number(d.total) : NaN }
const pageRecords = (res) => { const d = data(res); return d && Array.isArray(d.records) ? d.records : [] }
/** HTTP 200 且（有 code 时 code=200/0/success=true；无 code 的裸响应按 HTTP 200 视为成功） */
function isOk200(res) {
  if (res.status !== 200) return false
  const j = res.json
  if (j === null || j === undefined) return false
  if (typeof j === 'object' && !Array.isArray(j) && 'code' in j) {
    return j.code === 200 || j.code === '200' || j.code === 0 || j.success === true
  }
  return true
}

// ── 交易模块 23 个页面组件（需求给定清单；真库 sys_menu.component 已核对全部存在）──
// 注：需求文字写「22 页」，但清单实际列出 23 条组件路径（mall 13 + payment 4 + trade 6），此处按 23 条全覆盖。
const TRADE_PAGES = [
  { key: 'mall/basic-config', file: 'views/mall/basic-config/index.vue', title: '基础设置' },
  { key: 'mall/buyer-account', file: 'views/mall/buyer-account/index.vue', title: '买家账号' },
  { key: 'mall/buyer-apply', file: 'views/mall/buyer-apply/index.vue', title: '买家申请管理' },
  { key: 'mall/freight-config', file: 'views/mall/freight-config/index.vue', title: '运费设置' },
  { key: 'mall/keyword-bank', file: 'views/mall/keyword-bank/index.vue', title: '关键词库' },
  { key: 'mall/notice-config', file: 'views/mall/notice-config/index.vue', title: '公告设置' },
  { key: 'mall/order-process', file: 'views/mall/order-process/index.vue', title: '订单处理' },
  { key: 'mall/product-combo', file: 'views/mall/product-combo/index.vue', title: '商品组合' },
  { key: 'mall/product-shelf', file: 'views/mall/product-shelf/index.vue', title: '商品上架' },
  { key: 'mall/return-process', file: 'views/mall/return-process/index.vue', title: '退货申请处理' },
  { key: 'mall/shop-config', file: 'views/mall/shop-config/index.vue', title: '店铺设置' },
  { key: 'mall/shop-decoration', file: 'views/mall/shop-decoration/index.vue', title: '商城装修' },
  { key: 'mall/unit-display', file: 'views/mall/unit-display/index.vue', title: '单位显示' },
  { key: 'payment/reconciliation/daily', file: 'views/payment/reconciliation/daily.vue', title: '每日对账' },
  { key: 'payment/record/list', file: 'views/payment/record/list.vue', title: '支付记录' },
  { key: 'payment/refund/list', file: 'views/payment/refund/list.vue', title: '退款管理' },
  { key: 'payment/request/list', file: 'views/payment/request/list.vue', title: '支付请求' },
  { key: 'trade/api-monitor/list', file: 'views/trade/api-monitor/list.vue', title: 'API监控' },
  { key: 'trade/cart/list', file: 'views/trade/cart/list.vue', title: '购物车' },
  { key: 'trade/channel/config', file: 'views/trade/channel/config.vue', title: '渠道配置' },
  { key: 'trade/external-order/list', file: 'views/trade/external-order/list.vue', title: '外部订单' },
  { key: 'trade/inventory-sync/list', file: 'views/trade/inventory-sync/list.vue', title: '数据同步' },
  { key: 'trade/mall-order/list', file: 'views/trade/mall-order/list.vue', title: '商城订单' },
]

// ── 行业模板库 14 行业（逐字取自 Flyway V11.366.0 seed，用于与接口/真库三方对账）──
const LIBRARY_INDUSTRIES = ['办公用品', '服装鞋帽', '化妆用品', '机械机电', '家电数码', '母婴用品',
  '汽修汽配', '生鲜农贸', '食品快消品', '手机通讯', '通用行业', '五金建材', '医药制品', '珠宝钟表']

function readSrc(rel) {
  try { return fs.readFileSync(`${ADMIN_SRC}/${rel}`, 'utf8') } catch (e) { return null }
}
function readAbs(absPath) {
  try { return fs.readFileSync(absPath, 'utf8') } catch (e) { return null }
}

/** 与 dynamicRoutes.ts#transformMenuToRoutes 同口径地把菜单树折算成「组件 → 路由 URL」 */
function joinUrl(a, b) {
  const left = String(a || '').replace(/\/+$/, '')
  const right = String(b || '').replace(/^\/+/, '')
  const s = [left, right].filter(Boolean).join('/')
  return s.startsWith('/') ? s : '/' + s
}
function flattenMenuRoutes(nodes, parentRoute = '', parentRaw = '', out = []) {
  for (const m of nodes || []) {
    let rp = m.path || ''
    if (parentRaw && m.path && m.path.startsWith(parentRaw + '/')) rp = m.path.slice(parentRaw.length + 1)
    else if (m.path && m.path.startsWith('/')) rp = m.path.slice(1)
    if (rp && rp === parentRaw) rp = ''
    const full = joinUrl(parentRoute, rp)
    if (m.component) out.push({ component: String(m.component), route: full, menuName: m.menuName })
    if (Array.isArray(m.children) && m.children.length) {
      flattenMenuRoutes(m.children, full, m.path || parentRaw, out)
    }
  }
  return out
}
// 归一化组件路径为「页面 key」：去掉 `views/` 前缀、`.vue` 后缀，并**去掉末尾的 /index**
// —— 否则 `views/mall/product-shelf/index.vue` 会归一成 `mall/product-shelf/index`，
// 与清单里的 key `mall/product-shelf` 对不上，导致 13 个 mall/* 页被误报「未登记路由」
// （2026-09-14 实跑踩到：命中 10/23，恰为 trade/*6 + payment/*4 的 list.vue 形态）。
const normComponent = (c) => String(c || '').replace(/^views\//, '').replace(/\.vue$/, '').replace(/\/index$/, '')

// 复原台账（第十节复核用）
const RESTORE = {
  shelf: null,        // { id, productCode, original }
  unitFlag: null,     // { id, original }
  unitType: null,     // { id, originalProductDisplay, units: [{ id, original }] }
  configDesc: null,   // { id, original }
}

async function main() {
  console.log('交易模块金标准 E2E —— 后端 :' + PORT + '，前端 ' + FE + '，账号 ' + E2E_USER)
  fs.mkdirSync(SHOTS, { recursive: true })

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 一、登录 + 权限（会话租户/权限清单） ═══')
  const cap = await rawReq('GET', '/auth/captcha')
  check('验证码接口可用（base64 图片 + uuid）',
    cap.status === 200 && String(data(cap)?.img || '').startsWith('data:image') && !!data(cap)?.uuid,
    `status=${cap.status} uuid=${data(cap)?.uuid}`)
  await login()
  check('登录成功并拿到 token', !!TOKEN, String(TOKEN || '').slice(0, 24) + '…')

  const infoRes = await rawReq('GET', '/auth/userinfo', null, TOKEN)
  const info = data(infoRes) || {}
  check('会话身份与配置账号一致（/auth/userinfo.username）', info.username === E2E_USER,
    `api=${info.username} env=${E2E_USER}`)
  const TENANT = Number(info.tenantId)
  const USER_ID = Number(info.userId)
  const dbUser = await dbOne(`SELECT id, tenant_id FROM sys_user WHERE username = $1 AND deleted = 0 ORDER BY id LIMIT 1`, [E2E_USER])
  check('会话租户 = sys_user.tenant_id 直查（多租户口径基准）',
    Number.isFinite(TENANT) && Number(dbUser.tenant_id) === TENANT,
    `session=${TENANT} db=${dbUser.tenant_id}`)
  const perms = new Set(Array.isArray(info.permissions) ? info.permissions : [])
  check('权限清单已下发（Sa-Token permission list）', Array.isArray(info.permissions),
    `共 ${perms.size} 项；含 erp:product:list=${perms.has('erp:product:list')} / erp:product:status=${perms.has('erp:product:status')} / erp:product:update=${perms.has('erp:product:update')}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 二、23 页金标准骨架一致性（菜单路由 + 前端源码 + dynamicRoutes 注册） ═══')
  const menuRes = await rawReq('GET', '/menu/user/client/tenant-admin', null, TOKEN)
  const menuTree = data(menuRes)
  check('用户菜单树接口可用（动态路由数据源）',
    menuRes.status === 200 && Array.isArray(menuTree) && menuTree.length > 0,
    `status=${menuRes.status} nodes=${Array.isArray(menuTree) ? menuTree.length : 'n/a'}`)

  const flatRoutes = flattenMenuRoutes(menuTree || [])
  const routeByComponent = new Map()
  for (const r of flatRoutes) routeByComponent.set(normComponent(r.component), r.route)

  // 菜单授权是按角色的：若本账号未授权某页，回退到 sys_menu 系统级注册（component + path）判定路由是否存在，
  // 并在 detail 中显式标注「该页未授权给本账号」，避免把权限问题误判成路由缺失。
  const dbMenu = await dbQuery(`SELECT path, component FROM sys_menu
     WHERE deleted = 0 AND component IS NOT NULL AND (component LIKE 'views/mall/%'
        OR component LIKE 'views/trade/%' OR component LIKE 'views/payment/%')`)
  const dbRouteByComponent = new Map(dbMenu.map(r =>
    [normComponent(r.component), '/' + String(r.path || '').replace(/^\/+/, '')]))
  const unresolved = [], viaDbOnly = []
  for (const p of TRADE_PAGES) {
    if (routeByComponent.get(p.key)) continue
    const dbRoute = dbRouteByComponent.get(p.key)
    if (dbRoute) { routeByComponent.set(p.key, dbRoute); viaDbOnly.push(p.key) } else unresolved.push(p.key)
  }
  check('23 个页面组件在 sys_menu 已登记路由（component + path 非空，真库直查）',
    TRADE_PAGES.every(p => dbRouteByComponent.get(p.key)),
    `命中 ${TRADE_PAGES.filter(p => dbRouteByComponent.get(p.key)).length}/${TRADE_PAGES.length}`)
  check('23 个页面组件都能解析出可加载路由（无 404 隐患）', unresolved.length === 0,
    `命中 ${TRADE_PAGES.length - unresolved.length}/${TRADE_PAGES.length}`
    + (viaDbOnly.length ? `（其中 ${viaDbOnly.length} 页本账号菜单未授权，按 sys_menu 注册判定: ${viaDbOnly.join(', ')}）` : '')
    + (unresolved.length ? `；缺失: ${unresolved.join(', ')}` : ''))

  const missingFile = [], noBoundary = [], noContainer = [], noFullHeight = []
  for (const p of TRADE_PAGES) {
    const src = readSrc(p.file)
    if (!src) { missingFile.push(p.key); continue }
    if (!src.includes('<ErrorBoundary')) noBoundary.push(p.key)
    if (!src.includes('PageContainer')) noContainer.push(p.key)
    if (!src.includes('full-height')) noFullHeight.push(p.key)
  }
  check('23 个页面源文件都存在', missingFile.length === 0, missingFile.length ? `缺失: ${missingFile.join(', ')}` : `已读取 ${TRADE_PAGES.length} 个 .vue`)
  check('23 个页面都含 ErrorBoundary 骨架', noBoundary.length === 0, noBoundary.join(', '))
  check('23 个页面都含 PageContainer', noContainer.length === 0, noContainer.join(', '))
  check('23 个页面都含 full-height 标记', noFullHeight.length === 0, noFullHeight.join(', '))

  const drSrc = readSrc('router/dynamicRoutes.ts') || ''
  const notMapped = TRADE_PAGES.filter(p => !drSrc.includes(`'${p.key}'`))
  check('dynamicRoutes.componentMap 已注册全部 23 个页面组件', notMapped.length === 0,
    `命中 ${TRADE_PAGES.length - notMapped.length}/${TRADE_PAGES.length}` + (notMapped.length ? `；未注册: ${notMapped.map(p => p.key).join(', ')}` : ''))

  // 其余交易页数据源（只读可用性 + 分页口径直查）
  console.log('  ── 二·B 各页数据源接口只读可用性 ──')
  const dsChecks = [
    ['买家账号/买家申请 → /erp/mall/admin/user/page', '/erp/mall/admin/user/page?pageNum=1&pageSize=5'],
    ['关键词库 → /erp/mall/admin/keyword/page', '/erp/mall/admin/keyword/page?pageNum=1&pageSize=5'],
    ['公告设置 → /erp/mall/admin/notice/page', '/erp/mall/admin/notice/page?pageNum=1&pageSize=5'],
    ['商品组合 → /erp/product-kit/page', '/erp/product-kit/page?pageNum=1&pageSize=5'],
    ['退货申请处理 → /erp/sale/return/page', '/erp/sale/return/page?pageNum=1&pageSize=5'],
    ['购物车 → /v1/mall/cart/page', '/v1/mall/cart/page?pageNum=1&pageSize=5'],
    ['渠道配置 → /trade/channel/page', '/trade/channel/page?pageNum=1&pageSize=5'],
    ['外部订单 → /trade/external-order/page', '/trade/external-order/page?pageNum=1&pageSize=5'],
    ['数据同步 → /trade/inventory-sync/page', '/trade/inventory-sync/page?pageNum=1&pageSize=5'],
  ]
  for (const [label, path] of dsChecks) {
    const r = await rawReq('GET', path, null, TOKEN)
    check(label + ' 可用且返回分页结构', isOk200(r) && Number.isFinite(pageTotal(r)),
      `status=${r.status} total=${pageTotal(r)} msg=${r.json?.message || ''}`)
  }

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 三、商品上架 v_mall_product：分页口径 / status 映射 / 批量上下架改库回读 ═══')
  // 期望值：pageProducts 走 tenant_id + deleted=0（源码 MallAdminServiceImpl#pageProducts）
  const dbViewTotal = await dbOne(`SELECT COUNT(*)::int AS cnt FROM v_mall_product WHERE deleted = 0 AND tenant_id = $1`, [TENANT])
  const prodPage = await rawReq('GET', '/erp/mall/admin/product/page?pageNum=1&pageSize=20', null, TOKEN)
  check('商品上架分页 total = v_mall_product 直查（tenant + deleted=0）',
    isOk200(prodPage) && pageTotal(prodPage) === Number(dbViewTotal.cnt),
    `api=${pageTotal(prodPage)} db=${dbViewTotal.cnt}`)
  const prodRecs = pageRecords(prodPage)
  check('首页行数 = MIN(pageSize, total) 且字段齐备',
    prodRecs.length === Math.min(20, Number(dbViewTotal.cnt))
    && prodRecs.every(r => r.productCode && r.productName !== undefined && r.status
      && r.stockQuantity !== undefined && r.salePrice !== undefined),
    `rows=${prodRecs.length} badRow=${JSON.stringify(prodRecs.find(r => !(r.productCode && r.status)) || {}).slice(0, 120)}`)

  const codes = prodRecs.map(r => r.productCode).filter(Boolean)
  let mappingMismatch = null
  if (codes.length) {
    const dbRows = await dbQuery(
      `SELECT product_code, mall_shelf_status FROM erp_product
        WHERE deleted = 0 AND tenant_id = $1 AND product_code = ANY($2::text[])`, [TENANT, codes])
    const m = new Map(dbRows.map(r => [r.product_code, r.mall_shelf_status]))
    for (const r of prodRecs) {
      const expect = Number(m.get(r.productCode)) === 1 ? 'ON_SHELF' : 'INACTIVE'
      if (r.status !== expect) { mappingMismatch = `${r.productCode}: api=${r.status} erp_product.mall_shelf_status=${m.get(r.productCode)}`; break }
    }
  }
  check('逐行：视图 status = CASE(mall_shelf_status=1 → ON_SHELF，否则 INACTIVE)',
    codes.length > 0 && !mappingMismatch, mappingMismatch || `已核 ${codes.length} 行`)

  const onShelf = await rawReq('GET', '/erp/mall/admin/product/page?pageNum=1&pageSize=1&status=ON_SHELF', null, TOKEN)
  const offShelf = await rawReq('GET', '/erp/mall/admin/product/page?pageNum=1&pageSize=1&status=OFF_SHELF', null, TOKEN)
  const cntOn = await dbOne(`SELECT COUNT(*)::int AS cnt FROM v_mall_product
     WHERE deleted = 0 AND tenant_id = $1 AND status = 'ON_SHELF'`, [TENANT])
  const cntOnSrc = await dbOne(`SELECT COUNT(*)::int AS cnt FROM erp_product
     WHERE deleted = 0 AND tenant_id = $1 AND mall_shelf_status = 1`, [TENANT])
  const cntOff = await dbOne(`SELECT COUNT(*)::int AS cnt FROM v_mall_product
     WHERE deleted = 0 AND tenant_id = $1 AND status = 'INACTIVE'`, [TENANT])
  const cntOffSrc = await dbOne(`SELECT COUNT(*)::int AS cnt FROM erp_product
     WHERE deleted = 0 AND tenant_id = $1 AND mall_shelf_status IS DISTINCT FROM 1`, [TENANT])
  check('上架口径三方一致（接口 = 视图 ON_SHELF = erp_product.mall_shelf_status=1）',
    pageTotal(onShelf) === Number(cntOn.cnt) && Number(cntOn.cnt) === Number(cntOnSrc.cnt),
    `api=${pageTotal(onShelf)} view=${cntOn.cnt} src=${cntOnSrc.cnt}`)
  check('下架口径三方一致（接口 OFF_SHELF = 视图 INACTIVE = mall_shelf_status IS DISTINCT FROM 1）',
    pageTotal(offShelf) === Number(cntOff.cnt) && Number(cntOff.cnt) === Number(cntOffSrc.cnt),
    `api=${pageTotal(offShelf)} view=${cntOff.cnt} src=${cntOffSrc.cnt}`)

  // 批量上下架：改库 → 回读 → 复原
  const shelfTarget = await dbOne(
    `SELECT id, product_code, mall_shelf_status FROM erp_product
      WHERE deleted = 0 AND tenant_id = $1 AND mall_shelf_status IS NOT NULL
      ORDER BY id LIMIT 1`, [TENANT])
  if (shelfTarget.id !== undefined) {
    const original = Number(shelfTarget.mall_shelf_status)
    const flipped = original === 1 ? 'OFF_SHELF' : 'ON_SHELF'
    RESTORE.shelf = { id: shelfTarget.id, productCode: shelfTarget.product_code, original }
    const put = await rawReq('PUT', '/erp/product/batch-status', { ids: [shelfTarget.id], status: flipped }, TOKEN)
    // 不按「权限清单里有没有 erp:product:status」**预先**跳过：实测该账号的 permissions 里没有这条，
    // 但接口照样 200 可用（清单与实际鉴权不一致）。改为**先真发**，仅在拿到 401/403 时才判为授权不足而优雅跳过。
    if (put.status === 401 || put.status === 403) {
      RESTORE.shelf = null
      check('批量上下架改库回读（后端 401/403 授权拒绝 → 未执行写库校验，需换有权限账号重跑）',
        true, `PUT status=${put.status} perm=${perms.has('erp:product:status')}`)
    } else {
    check(`批量${flipped === 'ON_SHELF' ? '上架' : '下架'}接口真实改库（PUT /erp/product/batch-status）`,
      isOk200(put) && data(put) === true, `code=${shelfTarget.product_code} status=${put.status} data=${data(put)}`)
    const afterRow = await dbOne(`SELECT mall_shelf_status FROM erp_product WHERE id = $1`, [shelfTarget.id])
    check('回读 erp_product.mall_shelf_status = 目标值',
      Number(afterRow.mall_shelf_status) === (flipped === 'ON_SHELF' ? 1 : 0),
      `db=${afterRow.mall_shelf_status} expect=${flipped === 'ON_SHELF' ? 1 : 0}`)
    // ⚠️ 读写**两套词表**（本系统既有口径，真机实测确认）：
    //   写入 `PUT /erp/product/batch-status` 用 ON_SHELF / **OFF_SHELF**；
    //   而视图 v_mall_product.status（以及列表接口的 status 字段）只有
    //   `CASE WHEN mall_shelf_status = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END` 两值 —— 永远不产出 OFF_SHELF。
    //   故回读断言必须用 INACTIVE，不能用写入时的 OFF_SHELF（前端由 isOnShelf() 做这层映射）。
    const expectView = flipped === 'ON_SHELF' ? 'ON_SHELF' : 'INACTIVE'
    const afterView = await dbOne(`SELECT status FROM v_mall_product WHERE id = $1`, [shelfTarget.id])
    check('回读 v_mall_product.status 同步翻转（视图口径：非 1 即 INACTIVE）', afterView.status === expectView,
      `view=${afterView.status} expect=${expectView}（写入词表=${flipped}）`)
    const afterApi = await rawReq('GET',
      `/erp/mall/admin/product/page?pageNum=1&pageSize=5&keyword=${encodeURIComponent(shelfTarget.product_code)}`, null, TOKEN)
    const hit = pageRecords(afterApi).find(r => r.productCode === shelfTarget.product_code)
    check('回读列表接口该商品 status 同步翻转',
      hit?.status === expectView,
      `api=${hit?.status} expect=${expectView}`)
    const restoreStatus = original === 1 ? 'ON_SHELF' : 'OFF_SHELF'
    await rawReq('PUT', '/erp/product/batch-status', { ids: [shelfTarget.id], status: restoreStatus }, TOKEN)
    const restored = await dbOne(`SELECT mall_shelf_status FROM erp_product WHERE id = $1`, [shelfTarget.id])
    check('批量上下架已按原值复原', Number(restored.mall_shelf_status) === original,
      `now=${restored.mall_shelf_status} original=${original}`)
    }
  } else {
    check('批量上下架改库回读（无可用的 mall_shelf_status 非空商品行 → 未执行写库校验）',
      true, `target=none`)
  }

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 四、单位显示：值域口径 + 批量设置改库回读 ═══')
  // ⚠️ 源码口径（Flyway V11.361.3 / V11.361.8 / V11.363.0）：
  //   · erp_product.unit_display         = 商品级「是否在商城显示」布尔开关，值域 **0/1**（DEFAULT 1）
  //   · erp_product_unit.unit_display_type = **单位粒度**显示类型，值域 **-1/0/1/2**（NULL=未显式设置）
  //   故需求描述「erp_product.unit_display 4 值域（-1/0/1/2）」与源码不符，此处按源码口径分别校验两列。
  const d1 = await dbQuery(`SELECT DISTINCT unit_display FROM erp_product WHERE deleted = 0 ORDER BY 1`)
  check('erp_product.unit_display 值域 ⊆ {0,1}（商品级布尔开关）',
    d1.every(r => r.unit_display === null || r.unit_display === 0 || r.unit_display === 1),
    `distinct=${JSON.stringify(d1.map(r => r.unit_display))}`)
  const d2 = await dbQuery(`SELECT DISTINCT unit_display_type FROM erp_product_unit
     WHERE deleted = 0 AND unit_display_type IS NOT NULL ORDER BY 1`)
  check('erp_product_unit.unit_display_type 值域 ⊆ {-1,0,1,2}（单位粒度 4 值域）',
    d2.every(r => ['-1', '0', '1', '2'].includes(String(r.unit_display_type))),
    `distinct=${JSON.stringify(d2.map(r => r.unit_display_type))}`)

  // 单位显示页列表数据源：GET /erp/product/page（ProductServiceImpl#getProductPage 走 p.deleted=0 + unit_display 条件）
  {
    // 同上：不按权限清单预先跳过，先真发；401/403 才算授权不足
    const unitApi = await rawReq('GET', '/erp/product/page?pageNum=1&pageSize=20&unitDisplay=1', null, TOKEN)
    if (unitApi.status === 401 || unitApi.status === 403) {
      check('单位显示页列表口径（后端 401/403 授权拒绝 → 未执行接口对账，需换有权限账号重跑）',
        true, `GET status=${unitApi.status} perm=${perms.has('erp:product:list')}`)
    } else {
      const dbUnitOn = await dbOne(`SELECT COUNT(*)::int AS cnt FROM erp_product
         WHERE deleted = 0 AND tenant_id = $1 AND unit_display = 1`, [TENANT])
      check('单位显示页列表 total(unitDisplay=1) = erp_product 直查',
        isOk200(unitApi) && pageTotal(unitApi) === Number(dbUnitOn.cnt),
        `api=${pageTotal(unitApi)} db=${dbUnitOn.cnt}`)
    }
  }

  const unitTarget = await dbOne(
    `SELECT p.id, p.unit_display FROM erp_product p
      WHERE p.deleted = 0 AND p.tenant_id = $1 AND p.unit_display IS NOT NULL
        AND EXISTS (SELECT 1 FROM erp_product_unit u WHERE u.product_id = p.id AND u.deleted = 0)
      ORDER BY p.id LIMIT 1`, [TENANT])
  if (unitTarget.id !== undefined) {
    // ① 商品级布尔开关路径（body 传 unitDisplay）
    const origFlag = Number(unitTarget.unit_display)
    RESTORE.unitFlag = { id: unitTarget.id, original: unitTarget.unit_display }
    const flagPut = await rawReq('PUT', '/erp/product/batch-unit-display',
      { ids: [unitTarget.id], unitDisplay: origFlag === 1 ? 0 : 1 }, TOKEN)
    // 同上：先真发，仅 401/403 才判授权不足
    if (flagPut.status === 401 || flagPut.status === 403) {
      RESTORE.unitFlag = null
      check('批量单位显示改库回读（后端 401/403 授权拒绝 → 未执行写库校验，需换有权限账号重跑）',
        true, `PUT status=${flagPut.status} perm=${perms.has('erp:product:update')}`)
    } else {
    const flagAfter = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [unitTarget.id])
    check('批量显示/隐藏：erp_product.unit_display 真实改库并回读',
      isOk200(flagPut) && Number(flagAfter.unit_display) === (origFlag === 1 ? 0 : 1),
      `api=${flagAfter.unit_display} expect=${origFlag === 1 ? 0 : 1}`)
    await rawReq('PUT', '/erp/product/batch-unit-display', { ids: [unitTarget.id], unitDisplay: origFlag }, TOKEN)
    const flagRestored = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [unitTarget.id])
    check('批量显示/隐藏已按原值复原', Number(flagRestored.unit_display) === origFlag,
      `now=${flagRestored.unit_display} original=${origFlag}`)

    // ② 单位粒度类型路径（body 传 unitDisplayType）
    const unitsBefore = await dbQuery(
      `SELECT id, unit_display_type FROM erp_product_unit WHERE product_id = $1 AND deleted = 0 ORDER BY id`, [unitTarget.id])
    const prodBefore = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [unitTarget.id])
    RESTORE.unitType = { id: unitTarget.id, originalProductDisplay: prodBefore.unit_display, units: unitsBefore }
    const typePut = await rawReq('PUT', '/erp/product/batch-unit-display',
      { ids: [unitTarget.id], unitDisplayType: '2' }, TOKEN)
    const unitsAfter = await dbQuery(
      `SELECT id, unit_display_type FROM erp_product_unit WHERE product_id = $1 AND deleted = 0`, [unitTarget.id])
    const prodAfter = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [unitTarget.id])
    check('批量单位显示类型：全部有效单位 unit_display_type 真实写为 2',
      isOk200(typePut) && unitsAfter.length > 0 && unitsAfter.every(u => String(u.unit_display_type) === '2'),
      `units=${unitsAfter.length} values=${JSON.stringify([...new Set(unitsAfter.map(u => u.unit_display_type))])}`)
    check('批量单位显示类型：联动商品级 erp_product.unit_display = 1（源码口径）',
      Number(prodAfter.unit_display) === 1, `db=${prodAfter.unit_display}`)
    for (const u of unitsBefore) {
      await dbQuery(`UPDATE erp_product_unit SET unit_display_type = $1 WHERE id = $2`, [u.unit_display_type, u.id])
    }
    await dbQuery(`UPDATE erp_product SET unit_display = $1 WHERE id = $2`, [prodBefore.unit_display, unitTarget.id])
    const unitsRestored = await dbQuery(
      `SELECT id, unit_display_type FROM erp_product_unit WHERE product_id = $1 AND deleted = 0 ORDER BY id`, [unitTarget.id])
    const prodRestored = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [unitTarget.id])
    check('单位粒度类型与商品级开关均已按原值复原',
      unitsRestored.length === unitsBefore.length
      && unitsRestored.every((u, i) => String(u.unit_display_type ?? '') === String(unitsBefore[i].unit_display_type ?? ''))
      && String(prodRestored.unit_display ?? '') === String(prodBefore.unit_display ?? ''),
      `units=${unitsRestored.length}/${unitsBefore.length} unit_display=${prodRestored.unit_display}`)
    }
  } else {
    check('批量单位显示改库回读（无「有单位且 unit_display 非空」的商品行 → 未执行写库校验）',
      true, `perm=${perms.has('erp:product:update')} target=${unitTarget.id ?? 'none'}`)
  }

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 五、商城配置单行（基础设置/店铺设置/运费设置共用）：部分提交不覆盖 + reg_audit_required 列默认值 ═══')
  const cfgRow = await dbOne(`SELECT * FROM tenant_shop_config WHERE tenant_id = $1 ORDER BY id LIMIT 1`, [TENANT])
  const cfgGet = await rawReq('GET', '/erp/mall/admin/config', null, TOKEN)
  check('商城配置行存在且接口可取回（tenant_shop_config 单行）',
    cfgRow.id !== undefined && isOk200(cfgGet) && Number(data(cfgGet)?.id) === Number(cfgRow.id),
    `db.id=${cfgRow.id} api.id=${data(cfgGet)?.id}`)

  const regAuditCol = await dbQuery(
    `SELECT column_default, is_nullable FROM information_schema.columns
      WHERE table_name = 'tenant_shop_config' AND column_name = 'reg_audit_required'
        AND table_schema NOT IN ('pg_catalog', 'information_schema')`)
  check('⚠️ 回归：reg_audit_required 列存在', regAuditCol.length >= 1, `rows=${regAuditCol.length}`)
  check('⚠️ 回归：reg_audit_required.column_default 必须为 NULL（不可为 0，否则会回填存量行翻转审核语义）',
    regAuditCol.length >= 1 && regAuditCol.every(r => r.column_default === null),
    JSON.stringify(regAuditCol.map(r => ({ column_default: r.column_default, is_nullable: r.is_nullable }))))
  check('⚠️ 回归：reg_audit_required 可空（is_nullable = YES）',
    regAuditCol.length >= 1 && regAuditCol.every(r => r.is_nullable === 'YES'),
    JSON.stringify(regAuditCol.map(r => r.is_nullable)))

  if (cfgRow.id !== undefined) {
    const SNAPSHOT_SKIP = new Set(['shop_desc', 'update_time', 'update_by', 'version'])
    const probe = 'E2E-PARTIAL-' + Date.now()
    RESTORE.configDesc = { id: cfgRow.id, original: cfgRow.shop_desc }
    const put = await rawReq('PUT', '/erp/mall/admin/config', { shopDesc: probe }, TOKEN)
    const cfgAfter = await dbOne(`SELECT * FROM tenant_shop_config WHERE id = $1`, [cfgRow.id])
    const changed = Object.keys(cfgAfter).filter(k => !SNAPSHOT_SKIP.has(k)
      && String(cfgAfter[k] ?? '') !== String(cfgRow[k] ?? ''))
    check('PUT /erp/mall/admin/config 部分字段提交：目标字段真实落库',
      isOk200(put) && cfgAfter.shop_desc === probe, `db=${cfgAfter.shop_desc}`)
    check('⚠️ 回归：部分提交不覆盖其它字段（updateById null-忽略语义）', changed.length === 0,
      changed.length ? `被意外改动: ${changed.join(',')}` : `已比对 ${Object.keys(cfgRow).length - SNAPSHOT_SKIP.size} 列`)
    const getAfter = await rawReq('GET', '/erp/mall/admin/config', null, TOKEN)
    check('回读 GET /erp/mall/admin/config 与库内一致', data(getAfter)?.shopDesc === probe,
      `api=${data(getAfter)?.shopDesc}`)
    await dbQuery(`UPDATE tenant_shop_config SET shop_desc = $1 WHERE id = $2`, [cfgRow.shop_desc, cfgRow.id])
    const cfgRestored = await dbOne(`SELECT shop_desc FROM tenant_shop_config WHERE id = $1`, [cfgRow.id])
    check('商城配置已按原值复原', String(cfgRestored.shop_desc ?? '') === String(cfgRow.shop_desc ?? ''),
      `now=${cfgRestored.shop_desc} original=${cfgRow.shop_desc}`)
  } else {
    check('商城配置部分提交不覆盖（租户无 tenant_shop_config 行 → 未执行写库校验）', true, `tenant=${TENANT}`)
  }

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 六、商城装修：行业模板库 14 条（config_json 全空）+ 装修配置 CRUD ═══')
  const libRes = await rawReq('GET', '/erp/mall/admin/template/library', null, TOKEN)
  const lib = data(libRes) || []
  check('行业模板库返回 14 条', isOk200(libRes) && lib.length === 14, `count=${lib.length}`)
  const libDb = await dbOne(`SELECT COUNT(*)::int AS cnt, COUNT(config_json)::int AS with_json
     FROM shop_template WHERE is_library = 1 AND status = 1 AND deleted = 0`)
  check('模板库条数与真库直查一致（is_library=1 / status=1 / deleted=0）',
    Number(libDb.cnt) === lib.length, `api=${lib.length} db=${libDb.cnt}`)
  check('模板库 config_json 全为 NULL（行业布局未实测，不臆造）',
    lib.every(t => t.configJson === null || t.configJson === undefined) && Number(libDb.with_json) === 0,
    `api非空=${lib.filter(t => t.configJson != null).length} db非空=${libDb.with_json}`)
  const libNames = lib.map(t => t.industryName || t.templateName)
  check('14 个行业名与 Flyway V11.366.0 seed 逐字一致',
    LIBRARY_INDUSTRIES.every(n => libNames.includes(n)) && libNames.length === LIBRARY_INDUSTRIES.length,
    libNames.join(','))

  const decoName = 'E2E-DECO-' + Date.now()
  const createRes = await rawReq('POST', '/erp/mall/admin/decoration',
    { name: decoName, scope: 'HOME', status: 1, configJson: '{"e2e":true,"blocks":[]}' }, TOKEN)
  const decoId = data(createRes)?.id
  check('装修配置新增（POST /erp/mall/admin/decoration）返回真实 id',
    isOk200(createRes) && decoId !== undefined && decoId !== null, `id=${decoId}`)
  const decoInDb = decoId === undefined ? {} : await dbOne(
    `SELECT id, name, scope, config_json, status, deleted FROM shop_decoration WHERE id = $1`, [decoId])
  check('新增装修配置已落库（name/scope/config_json 与提交一致）',
    decoInDb.name === decoName && decoInDb.scope === 'HOME' && String(decoInDb.config_json || '').includes('"e2e":true'),
    JSON.stringify(decoInDb).slice(0, 180))

  const listRes = await rawReq('GET', '/erp/mall/admin/decoration?scope=HOME', null, TOKEN)
  check('装修配置列表按 scope=HOME 过滤且含新增行',
    isOk200(listRes) && (data(listRes) || []).some(d => String(d.id) === String(decoId)),
    `count=${(data(listRes) || []).length}`)
  const detailRes = await rawReq('GET', `/erp/mall/admin/decoration/${decoId}`, null, TOKEN)
  check('装修配置详情可用', isOk200(detailRes) && data(detailRes)?.name === decoName, `name=${data(detailRes)?.name}`)

  const updatedName = decoName + '-UPD'
  const updRes = await rawReq('PUT', `/erp/mall/admin/decoration/${decoId}`, { name: updatedName, status: 0 }, TOKEN)
  const updDb = await dbOne(`SELECT name, scope, status, config_json FROM shop_decoration WHERE id = $1`, [decoId])
  check('装修配置部分更新：name/status 生效', isOk200(updRes) && updDb.name === updatedName && Number(updDb.status) === 0,
    `name=${updDb.name} status=${updDb.status}`)
  check('装修配置部分更新：未提交字段不被清空（scope 保持 HOME）', updDb.scope === 'HOME',
    `scope=${updDb.scope}`)

  const delRes = await rawReq('DELETE', `/erp/mall/admin/decoration/${decoId}`, null, TOKEN)
  const delDb = await dbOne(`SELECT deleted FROM shop_decoration WHERE id = $1`, [decoId])
  check('装修配置删除为逻辑删除（deleted=1）', isOk200(delRes) && Number(delDb.deleted) === 1,
    `deleted=${delDb.deleted}`)
  const listAfterDel = await rawReq('GET', '/erp/mall/admin/decoration?scope=HOME', null, TOKEN)
  check('删除后列表不再返回该行', !(data(listAfterDel) || []).some(d => String(d.id) === String(decoId)),
    `count=${(data(listAfterDel) || []).length}`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 七、商城订单 / 订单处理：分页口径直查 + 统计卡 + 下单读视图（库存不足不再恒真） ═══')
  const orderPage = await rawReq('GET', '/erp/mall/admin/order/page?pageNum=1&pageSize=20', null, TOKEN)
  const dbOrderTotal = await dbOne(`SELECT COUNT(*)::int AS cnt FROM erp_sale_order
     WHERE deleted = 0 AND tenant_id = $1 AND order_source = 2`, [TENANT])
  check('订单处理/商城订单分页 total = erp_sale_order 直查（order_source=2）',
    isOk200(orderPage) && pageTotal(orderPage) === Number(dbOrderTotal.cnt),
    `api=${pageTotal(orderPage)} db=${dbOrderTotal.cnt}`)

  const detailPage = await rawReq('GET', '/erp/mall/admin/order/page-detail?pageNum=1&pageSize=20', null, TOKEN)
  const dbItemTotal = await dbOne(`SELECT COUNT(*)::int AS cnt
     FROM erp_sale_order_item item INNER JOIN erp_sale_order o ON item.order_id = o.id AND o.deleted = 0
     WHERE o.tenant_id = $1 AND item.tenant_id = $1 AND o.order_source IN (2, 3)`, [TENANT])
  check('按明细分页 total = item JOIN order 直查（order_source IN (2,3)，两表租户条件）',
    isOk200(detailPage) && pageTotal(detailPage) === Number(dbItemTotal.cnt),
    `api=${pageTotal(detailPage)} db=${dbItemTotal.cnt}`)

  const statsRes = await rawReq('GET', '/erp/mall/admin/order/stats', null, TOKEN)
  const st = data(statsRes) || {}
  const dbStats = await dbOne(`SELECT COUNT(*)::int AS cnt, COALESCE(SUM(total_amount),0) AS gmv,
       SUM(CASE WHEN payment_status = 4 THEN 1 ELSE 0 END)::int AS refund_cnt
     FROM erp_sale_order WHERE deleted = 0 AND tenant_id = $1 AND order_source IN (2, 3) AND status NOT IN (5, 6)`, [TENANT])
  check('订单统计卡 totalOrderCount = 直查（排除已取消 status IN (5,6)）',
    Number(st.totalOrderCount) === Number(dbStats.cnt), `api=${st.totalOrderCount} db=${dbStats.cnt}`)
  check('订单统计卡 GMV = SUM(total_amount) 直查（容差 0.01）',
    Math.abs(Number(st.totalGmv || 0) - Number(dbStats.gmv || 0)) < 0.01,
    `api=${st.totalGmv} db=${dbStats.gmv}`)
  check('订单统计卡 refundOrderCount = payment_status=4 计数直查',
    Number(st.refundOrderCount || 0) === Number(dbStats.refund_cnt || 0),
    `api=${st.refundOrderCount} db=${dbStats.refund_cnt}`)

  // 买家侧订单列表（MallOrderServiceImpl#listOrders 按会话身份 + order_source 过滤）
  const shopUser = Number.isFinite(USER_ID)
    ? await dbOne(`SELECT id, party_id, user_type FROM shop_user WHERE id = $1`, [USER_ID]) : {}
  const queryPartyId = shopUser.party_id ?? USER_ID
  const expectedSource = shopUser.user_type === 'ENTERPRISE' ? 2 : 3
  const myOrderTotal = await dbOne(`SELECT COUNT(*)::int AS cnt FROM erp_sale_order
     WHERE deleted = 0 AND tenant_id = $1 AND customer_id = $2 AND order_source = $3`,
    [TENANT, queryPartyId, expectedSource])
  const mallOrders = await rawReq('GET', '/v1/mall/orders?page=1&size=5', null, TOKEN)
  check('/api/v1/mall/orders 可用且 total = 会话身份口径直查（party_id + order_source）',
    isOk200(mallOrders) && Array.isArray(data(mallOrders)?.records)
    && Number(data(mallOrders)?.total) === Number(myOrderTotal.cnt),
    `api=${data(mallOrders)?.total} db=${myOrderTotal.cnt} partyId=${queryPartyId} source=${expectedSource}`)

  // 下单读商品走 v_mall_product：视图库存 = erp_stock 实时聚合（旧 mall_product.stock_quantity 全 0 → 「库存不足」恒真）
  const stockSample = await dbQuery(`SELECT id, product_code, stock_quantity FROM v_mall_product
     WHERE deleted = 0 AND tenant_id = $1 ORDER BY id LIMIT 5`, [TENANT])
  const stockIds = stockSample.map(r => r.id)
  const dbStockAgg = stockIds.length ? await dbQuery(`SELECT p.id, COALESCE(SUM(s.available_quantity), 0)::int AS agg
     FROM erp_product p LEFT JOIN erp_stock s ON s.product_id = p.id AND s.deleted = 0
     WHERE p.id = ANY($1::bigint[]) GROUP BY p.id`, [stockIds]) : []
  // ⚠️ 必须按**字符串**比对 id：主键是雪花/BIGINT（如 990000000000000001），
  // `Number(...)` 超过 2^53 会丢精度 —— ...001 与 ...003 会舍入成同一个 double，
  // 于是 Map 里后写入的 ...003(152) 覆盖了 ...001 的真实值(0)，把正确的视图判成错误
  // （2026-09-14 实跑踩到：SP-TEST-001:0/152 SP-TEST-002:0/152）。
  const aggMap = new Map(dbStockAgg.map(r => [String(r.id), Number(r.agg)]))
  check('视图 stock_quantity = erp_stock.available_quantity 聚合直查（下单读的就是视图）',
    stockSample.length > 0 && stockSample.every(r => Number(r.stock_quantity) === (aggMap.get(String(r.id)) ?? -1)),
    stockSample.map(r => `${r.product_code}:${r.stock_quantity}/${aggMap.get(String(r.id))}`).join(' '))
  const viewInStock = await dbOne(`SELECT COUNT(*)::int AS cnt FROM v_mall_product
     WHERE deleted = 0 AND tenant_id = $1 AND stock_quantity > 0`, [TENANT])
  check('存在视图库存 > 0 的商品（否则「商品库存不足」在真库仍恒真，商城下单不可用）',
    Number(viewInStock.cnt) > 0, `count=${viewInStock.cnt}（依赖 erp_stock 种子数据，无库存时会如实失败）`)
  const orderSrc = readAbs('I:/AI-Ready/backend/erp/erp-mall/src/main/java/cn/aiedge/erp/b2b/service/MallOrderServiceImpl.java')
  check('源码核对：下单库存判定读 ErpProductMallMapper（v_mall_product），不读已废弃 mall_product',
    !!orderSrc && orderSrc.includes('erpProductMallMapper.selectOne(')
    && orderSrc.includes('库存不足') && !orderSrc.includes('mallProductMapper'),
    orderSrc ? 'MallOrderServiceImpl.java：仅出现 erpProductMallMapper（b2b/service 下，非 service/impl）'
      : '// TODO 未找到 MallOrderServiceImpl.java（b2b/service/ 下）')

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 八、支付四页（请求/记录/退款/对账）+ 每日对账待对账日期 ═══')
  const payReqs = [
    ['支付请求分页', '/payment/request/page?pageNum=1&pageSize=20', 'payment_request'],
    ['支付记录分页', '/payment/record/page?pageNum=1&pageSize=20', 'payment_record'],
    ['退款请求分页', '/refund/request/page?pageNum=1&pageSize=20', 'refund_request'],
    ['退款记录分页', '/refund/record/page?pageNum=1&pageSize=20', 'refund_record'],
    ['对账记录分页', '/reconciliation/page?pageNum=1&pageSize=20', 'payment_reconciliation'],
  ]
  for (const [label, p, table] of payReqs) {
    const r = await rawReq('GET', p, null, TOKEN)
    const t = await dbOne(`SELECT COUNT(*)::int AS cnt FROM ${table} WHERE deleted = 0 AND tenant_id = $1`, [TENANT])
    check(`${label} total = ${table} 直查（tenant + deleted=0）`,
      isOk200(r) && pageTotal(r) === Number(t.cnt), `api=${pageTotal(r)} db=${t.cnt}`)
  }
  const statApis = [
    ['支付请求统计', '/payment/request/stat'],
    ['支付记录统计', '/payment/record/stat'],
    ['退款请求统计', '/refund/request/stat'],
    ['对账统计', '/reconciliation/stat'],
  ]
  for (const [label, p] of statApis) {
    const r = await rawReq('GET', p, null, TOKEN)
    const d = data(r)
    check(`${label}可用且返回聚合指标`, isOk200(r) && d && typeof d === 'object' && Object.keys(d).length > 0,
      Object.keys(d || {}).join(',').slice(0, 120))
  }

  // /pending-dates 语义：近 30 天内有支付流水但**无对账记录**的日期（源码 ReconciliationServiceImpl#getPendingDates）
  const pendingRes = await rawReq('GET', '/reconciliation/pending-dates', null, TOKEN)
  const pendingApi = (data(pendingRes) || []).map(String).sort()
  const pendingDbRows = await dbQuery(
    `SELECT d::text AS d FROM (
        SELECT DISTINCT create_time::date AS d FROM payment_request
         WHERE deleted = 0 AND tenant_id = $1 AND create_time IS NOT NULL
           AND create_time >= (CURRENT_DATE - INTERVAL '30 days')
           AND create_time <  (CURRENT_DATE + INTERVAL '1 day')
     ) t
     WHERE d NOT IN (
        SELECT reconcile_date FROM payment_reconciliation
         WHERE deleted = 0 AND tenant_id = $1 AND reconcile_date IS NOT NULL
           AND reconcile_date >= CURRENT_DATE - 30 AND reconcile_date <= CURRENT_DATE
     ) ORDER BY d`, [TENANT])
  const pendingDb = pendingDbRows.map(r => String(r.d)).sort()
  check('待对账日期 = 近 30 天有支付流水且无对账记录的日期（直查复算一致）',
    isOk200(pendingRes) && JSON.stringify(pendingApi) === JSON.stringify(pendingDb),
    `api=[${pendingApi.join(',')}] db=[${pendingDb.join(',')}]`)

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 九、UI 抽样（6 页：骨架 + 无对象/NaN 渲染 + 无 console error + 截图） ═══')
  const UI_PAGES = ['mall/product-shelf', 'mall/unit-display', 'mall/freight-config',
    'mall/shop-config', 'mall/shop-decoration', 'trade/mall-order/list']
  const browser = await chromium.launch({ headless: true })
  const ctx = await browser.newContext({ viewport: { width: 1680, height: 1000 }, locale: 'zh-CN' })
  const page = await ctx.newPage()
  const errs = []
  page.on('pageerror', e => errs.push('pageerror: ' + e.message))
  page.on('console', m => { if (m.type() === 'error') errs.push('console: ' + m.text().slice(0, 120)) })
  await page.route(url => new URL(url).pathname.startsWith('/api/'), async route => {
    const u = new URL(route.request().url())
    await route.continue({ url: `http://localhost:${PORT}${u.pathname}${u.search}` })
  })
  await page.goto(`${FE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(([tk, tid]) => { localStorage.setItem('token', tk); localStorage.setItem('tenantId', String(tid)) },
    [TOKEN, TENANT])
  if (!fs.existsSync(SHOTS)) fs.mkdirSync(SHOTS, { recursive: true })

  for (const key of UI_PAGES) {
    const target = TRADE_PAGES.find(p => p.key === key) || { key, title: key }
    const route = routeByComponent.get(key) || ('/' + key.replace(/\/list$/, ''))
    errs.length = 0
    await page.goto(FE + route, { waitUntil: 'domcontentloaded' })
    await page.waitForSelector('.page-container', { timeout: 40000 }).catch(() => {})
    await page.waitForTimeout(3500)
    const txt = (await page.locator('body').innerText()).replace(/\s+/g, '')
    const hasBoundary = await page.locator('.error-boundary-root').count()
    const hasFullHeight = await page.locator('.page-container--full-height').count()
    const fallbackShown = await page.locator('.error-boundary-fallback').count()
    check(`UI「${target.title}」(${route}) 可打开且渲染金标准骨架 ErrorBoundary > PageContainer(full-height)`,
      hasBoundary > 0 && hasFullHeight > 0 && fallbackShown === 0
      && !txt.includes('页面不存在') && txt.length > 150,
      `boundary=${hasBoundary} fullHeight=${hasFullHeight} fallback=${fallbackShown} text=${txt.length}字 url=${page.url()}`)
    check(`UI「${target.title}」无 [object Object] / NaN 渲染异常`,
      !txt.includes('[objectObject]') && !txt.includes('NaN'), txt.slice(0, 100))
    check(`UI「${target.title}」运行期无 console error`,
      errs.length === 0, errs.slice(0, 3).join(' || '))
    await page.screenshot({ path: `${SHOTS}/ui-${key.replace(/\//g, '-')}.png`, fullPage: true })
  }
  await browser.close()

  // ══════════════════════════════════════════════════════════════
  console.log('\n═══ 十、清理 E2E 造数 + 复核现场已复原 ═══')
  await dbQuery(`DELETE FROM shop_decoration WHERE name LIKE 'E2E-%'`)
  const leftDeco = await dbOne(`SELECT COUNT(*)::int AS cnt FROM shop_decoration WHERE name LIKE 'E2E-%'`)
  check('测试造数已清理（shop_decoration 名称前缀 E2E-）', Number(leftDeco.cnt) === 0, `left=${leftDeco.cnt}`)
  const otherProbe = await dbOne(`SELECT
       (SELECT COUNT(*)::int FROM mall_keyword WHERE keyword LIKE 'E2E-%') AS kw,
       (SELECT COUNT(*)::int FROM mall_notice WHERE title LIKE 'E2E-%') AS notice`)
  check('未遗留 E2E- 前缀造数（关键词库 / 公告设置）',
    Number(otherProbe.kw) === 0 && Number(otherProbe.notice) === 0, JSON.stringify(otherProbe))

  if (RESTORE.shelf) {
    const now = await dbOne(`SELECT mall_shelf_status FROM erp_product WHERE id = $1`, [RESTORE.shelf.id])
    check('现场复核：商品上架状态已复原',
      Number(now.mall_shelf_status) === Number(RESTORE.shelf.original),
      `${RESTORE.shelf.productCode}: now=${now.mall_shelf_status} original=${RESTORE.shelf.original}`)
  }
  if (RESTORE.unitFlag) {
    const now = await dbOne(`SELECT unit_display FROM erp_product WHERE id = $1`, [RESTORE.unitFlag.id])
    check('现场复核：商品级单位显示开关已复原',
      String(now.unit_display ?? '') === String(RESTORE.unitFlag.original ?? ''),
      `now=${now.unit_display} original=${RESTORE.unitFlag.original}`)
  }
  if (RESTORE.unitType) {
    const rows = await dbQuery(`SELECT id, unit_display_type FROM erp_product_unit
       WHERE product_id = $1 AND deleted = 0 ORDER BY id`, [RESTORE.unitType.id])
    const okAll = rows.length === RESTORE.unitType.units.length
      && rows.every((r, i) => String(r.unit_display_type ?? '') === String(RESTORE.unitType.units[i].unit_display_type ?? ''))
    check('现场复核：单位粒度显示类型已复原（逐行比对原值）', okAll,
      `rows=${rows.length}/${RESTORE.unitType.units.length}`)
  }
  if (RESTORE.configDesc) {
    const now = await dbOne(`SELECT shop_desc FROM tenant_shop_config WHERE id = $1`, [RESTORE.configDesc.id])
    check('现场复核：商城配置 shop_desc 已复原',
      String(now.shop_desc ?? '') === String(RESTORE.configDesc.original ?? ''),
      `now=${now.shop_desc} original=${RESTORE.configDesc.original}`)
  }

  const pass = results.filter(r => r.ok).length
  const fail = results.length - pass
  console.log(`\n═══ 汇总：${pass}/${results.length} 通过${fail ? `，${fail} 项失败` : ''} ═══`)
  if (fail) results.filter(r => !r.ok).forEach(r => console.log(`  ❌ ${r.name} — ${r.detail}`))
  process.exitCode = fail ? 1 : 0
}

main().catch(e => { console.error('E2E 异常终止:', e.message, e.stack); process.exitCode = 1 })
