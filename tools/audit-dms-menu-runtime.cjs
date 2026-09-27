/**
 * 配送（DMS）模块 · 运行期可用性验证（只读）
 *
 * 目的：验证「20 条配送自建菜单 menu_level=3 → 0」的修复在**真机**上生效。
 *   必须换「非系统租户 + 非超管」账号实测 —— dev 的 admin 同时命中 isSystemTenant
 *   与 isSuperAdmin 双豁免（走 getUserMegaMenus:259 早退分支取全量），本地看不出问题。
 *
 * 口径依据：core-base `SysMenuServiceImpl.getUserMegaMenus`
 *   L277-278  if (!isSystemTenant && !isSuperAdmin) wrapper.eq(SysMenu::getMenuLevel, 0)
 *   L259      if (isSystemTenant && isSuperAdmin) → 早退，取全量
 *
 * 用法：node tools/audit-dms-menu-runtime.cjs
 *   E2E_API 覆盖后端地址（默认 http://localhost:5655/api）
 */
const http = require('http')

const API = process.env.E2E_API || 'http://localhost:5655/api'

/** 配送自建 20 页的菜单 id（本次 menu_level 3→0 修复对象） */
const DMS_IDS = ['80760', '80820', '80700', '80830', '80770', '80780', '80790', '80840', '80847',
  '80730', '80850', '80860', '80740', '80870', '80900', '80750', '80880', '80890', '80910', '80920']

/** 配送域分组节点 */
const DMS_GROUPS = ['60401', '60501', '60502', '60402', '60403', '60504', '60505', '60506', '60507', '61506']

/** 每页一个主取数接口（20 条菜单各覆盖一个） */
const PROBES = [
  ['GET', '/dms/task/page?pageNum=1&pageSize=1', '配送查询/配送单/调度任务'],
  ['GET', '/dms/dashboard/stats', '配送仪表盘'],
  ['GET', '/delivery/route/page?pageNum=1&pageSize=1', '配送路线单'],
  ['GET', '/dms/route/config', '路线规划'],
  ['GET', '/dms/vehicle/page?pageNum=1&pageSize=1', '车辆管理'],
  ['GET', '/dms/vehicle/maintenance/page?pageNum=1&pageSize=1', '车辆维护'],
  ['GET', '/dms/rider/page?pageNum=1&pageSize=1', '配送员管理'],
  ['GET', '/dms/vehicle/energy/page?pageNum=1&pageSize=1', '用车管理'],
  ['GET', '/dms/verification/kyc/page?pageNum=1&pageSize=1', '人员核验'],
  ['GET', '/dms/dispatch/strategy', '智能调度'],
  ['GET', '/dms/order-pool/page?pageNum=1&pageSize=1', '订单池'],
  ['GET', '/dms/tracking/rider-page?pageNum=1&pageSize=1', '实时跟踪'],
  ['GET', '/dms/tracking/page?pageNum=1&pageSize=1', '配送跟踪'],
  ['GET', '/dms/sign/page?pageNum=1&pageSize=1', '签收管理'],
  ['GET', '/dms/config/page?pageNum=1&pageSize=1', '配送参数'],
  ['GET', '/dms/channel/page?pageNum=1&pageSize=1', '渠道管理'],
  ['GET', '/dms/config/list', '配送配置'],
  ['GET', '/dms/settlement/page?pageNum=1&pageSize=1', '配送结算'],
  ['GET', '/dms/payment/page?pageNum=1&pageSize=1', '收款管理'],
  ['GET', '/trade/api-monitor/stat', 'API监控(交易域码)'],
]

const ACCOUNTS = [
  { user: 'admin', pwd: 'admin123', tenant: '系统租户', label: 'admin（系统租户+超管）' },
  { user: 'e2e_hr_ta', pwd: 'admin123', tenant: '系统租户', label: 'e2e_hr_ta（系统租户+SYSTEM_ADMIN，非超管）' },
  { user: 'e2e_hr_t2', pwd: 'admin123', tenant: 'E2E验收租户2', label: 'e2e_hr_t2（租户2+E2E_T2_ADMIN，非超管）★关键' },
]

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const url = new URL(API + path)
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: url.hostname, port: url.port, path: url.pathname + url.search, method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, res => {
      let buf = ''
      res.on('data', c => { buf += c })
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(buf) } catch { /* 非 JSON */ }
        resolve({ status: res.statusCode, json, raw: buf })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(acc) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username: acc.user, password: acc.pwd, tenantName: acc.tenant, captcha: code, captchaKey: cap.json.data.uuid,
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${acc.user} 登录失败: ` + JSON.stringify(res.json).slice(0, 300))
  return token
}

function walk(menus, out = []) {
  for (const m of menus || []) {
    out.push(m)
    if (m.children) walk(m.children, out)
  }
  return out
}

;(async () => {
  let fail = 0
  for (const acc of ACCOUNTS) {
    console.log(`\n${'='.repeat(80)}\n【${acc.label}】`)
    let token
    try { token = await login(acc) } catch (e) { console.log('  ' + e.message); fail++; continue }

    const info = await req('GET', '/auth/userinfo', null, token)
    const d = info.json?.data || {}
    const perms = d.permissions || []
    const isWildcard = perms.includes('*')
    console.log(`  角色=${JSON.stringify(d.roles)} 权限码数=${perms.length}${isWildcard ? '(通配符*)' : ''} tenantId=${d.tenantId}`)

    const tenantId = d.tenantId ?? 1
    const menuRes = await req('GET', `/menu/user/mega/tenant-admin?tenantId=${tenantId}`, null, token)
    const flat = walk(menuRes.json?.data || [])
    const pages = flat.filter(m => DMS_IDS.includes(String(m.id)))
    const groups = flat.filter(m => DMS_GROUPS.includes(String(m.id)))
    const missing = DMS_IDS.filter(id => !pages.some(m => String(m.id) === id))
    console.log(`  菜单下发：配送自建页 ${pages.length}/20，配送分组 ${groups.length}/10（HTTP ${menuRes.status}）`)
    if (missing.length) console.log(`    ❌ 未下发: ${missing.join(', ')}`)
    else console.log(`    ✅ 20 条自建菜单全部下发`)

    if (process.env.SKIP_PROBE) { console.log('  （SKIP_PROBE=1，跳过接口探测）'); continue }

    let ok = 0, denied = 0, other = 0
    const lines = []
    for (const [method, path, name] of PROBES) {
      const r = await req(method, path, null, token)
      const tag = r.status === 200 ? '✅200' : (r.status === 403 ? '⛔403' : `❓${r.status}`)
      if (r.status === 200) ok++; else if (r.status === 403) denied++; else other++
      lines.push(`    ${tag}  ${name.padEnd(26, ' ')} ${method} ${path}`)
    }
    console.log(`  接口探测：200=${ok} / 403=${denied} / 其他=${other}`)
    console.log(lines.join('\n'))
  }
  console.log(`\n${'='.repeat(80)}\n（只读验证，未改动任何数据）`)
  process.exit(0)
})()
