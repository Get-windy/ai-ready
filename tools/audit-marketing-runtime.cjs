/**
 * 营销模块 · 运行期可用性盘点（只读）
 *
 * 目的：回答两个「E2E 全绿也证明不了」的问题，必须换**非超管/非系统租户**账号实测：
 *   1. 后端真正下发了多少个营销菜单给该账号？（`menu_level=3` 会被 `getUserMegaMenus` 过滤掉）
 *   2. 该账号调营销接口是否 403？（营销权限码是否只授了 SUPER_ADMIN）
 *
 * 口径依据：`core-base` `SysMenuServiceImpl.getUserMegaMenus` L277-278
 *   `if (!isSystemTenant && !isSuperAdmin) wrapper.eq(SysMenu::getMenuLevel, 0)`
 * 以及 `sys_role_permission` 对 `marketing:%` 的授权矩阵。
 *
 * 用法：node tools/audit-marketing-runtime.cjs
 *   E2E_API 覆盖后端地址（默认 http://localhost:5655/api）
 */
const http = require('http')

const API = process.env.E2E_API || 'http://localhost:5655/api'

/** 营销域自有 19 页的菜单 id（80300~80331） */
const MKT_IDS = ['80300', '80301', '80302', '80303', '80304', '80310', '80311', '80312',
  '80313', '80314', '80315', '80320', '80321', '80322', '80323', '80324', '80325', '80330', '80331']

/** 代表性营销端点（覆盖 17 页各自的主取数接口 + 试算端点 + 跨模块复用端点） */
const PROBES = [
  ['GET', '/erp/marketing/member-config', '会员设置'],
  ['GET', '/erp/marketing/points-exchange/page?pageNum=1&pageSize=1', '积分兑换'],
  ['GET', '/erp/marketing/coupon-template/page?pageNum=1&pageSize=1', '优惠券'],
  ['GET', '/erp/marketing/promotion-activity/page?pageNum=1&pageSize=1', '商品/整单促销/特价'],
  ['GET', '/erp/marketing/sms/history/page?pageNum=1&pageSize=1', '发短信'],
  ['GET', '/erp/marketing/share/page?pageNum=1&pageSize=1', '推广历史查询'],
  ['GET', '/erp/marketing/promote/product/page?pageNum=1&pageSize=1', '我要推广'],
  ['GET', '/erp/marketing/auto-campaign/page?pageNum=1&pageSize=1', '营销自动化'],
  ['GET', '/erp/marketing/stored-card/page?pageNum=1&pageSize=1', '储值卡'],
  ['GET', '/erp/marketing/member-level/rules', '会员等级'],
  ['GET', '/erp/marketing/points-ledger/batch/list', '积分批次'],
  ['GET', '/erp/marketing/addon-rule/page?pageNum=1&pageSize=1', '加价购'],
  ['GET', '/erp/marketing/flash-sale/page?pageNum=1&pageSize=1', '商城秒杀'],
  ['GET', '/erp/marketing/presale/page?pageNum=1&pageSize=1', '商城预售'],
  ['POST', '/erp/marketing/promotion/calc', '促销引擎试算'],
  ['GET', '/erp/md/customer/member/page?pageNum=1&pageSize=1', '会员管理(复用资料域)'],
  ['GET', '/erp/mall/admin/popup-ad/page?pageNum=1&pageSize=1', '商城弹窗广告(复用商城域)'],
  ['GET', '/erp/product-kit/page?pageNum=1&pageSize=1', '套餐(复用资料域)'],
]

const ACCOUNTS = [
  { user: 'admin', pwd: 'admin123', tenant: '系统租户', label: 'admin（系统租户+超管）' },
  { user: 'e2e_hr_ta', pwd: 'admin123', tenant: '系统租户', label: 'e2e_hr_ta（系统租户+SYSTEM_ADMIN，非超管）' },
  { user: 'e2e_hr_t2', pwd: 'admin123', tenant: 'E2E验收租户2', label: 'e2e_hr_t2（租户2+E2E_T2_ADMIN，非超管）' },
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
  const summary = []
  for (const acc of ACCOUNTS) {
    console.log(`\n${'='.repeat(78)}\n【${acc.label}】`)
    let token
    try { token = await login(acc) } catch (e) { console.log('  ' + e.message); continue }

    const info = await req('GET', '/auth/userinfo', null, token)
    const d = info.json?.data || {}
    const perms = d.permissions || []
    console.log(`  角色=${JSON.stringify(d.roles)} 权限码数=${perms.length} tenantId=${d.tenantId}`)

    const tenantId = d.tenantId ?? 1
    const menuRes = await req('GET', `/menu/user/mega/tenant-admin?tenantId=${tenantId}`, null, token)
    const flat = walk(menuRes.json?.data || [])
    const mkt = flat.filter(m => MKT_IDS.includes(String(m.id)))
    const groups = flat.filter(m => ['60008', '60801', '60802', '60803', '60804'].includes(String(m.id)))
    console.log(`  菜单下发：营销子页 ${mkt.length}/19，营销分组节点 ${groups.length}/5（HTTP ${menuRes.status}）`)
    if (mkt.length) console.log(`    子页 id：${mkt.map(m => m.id).join(', ')}`)
    if (groups.length) console.log(`    分组 id：${groups.map(m => `${m.id}(${m.menuName})`).join(', ')}`)

    let ok = 0, denied = 0, other = 0
    const lines = []
    for (const [method, path, name] of PROBES) {
      const body = method === 'POST' ? { orderAmount: 100, items: [] } : null
      const r = await req(method, path, body, token)
      const tag = r.status === 200 ? '✅200' : (r.status === 403 ? '⛔403' : `❓${r.status}`)
      if (r.status === 200) ok++; else if (r.status === 403) denied++; else other++
      lines.push(`    ${tag}  ${name.padEnd(24, ' ')} ${method} ${path}`)
    }
    console.log(`  接口探测：200=${ok} / 403=${denied} / 其它=${other}`)
    console.log(lines.join('\n'))

    summary.push({ label: acc.label, pages: `${mkt.length}/19`, groups: `${groups.length}/5`, ok, denied, other })
  }

  console.log(`\n${'='.repeat(78)}\n汇总（营销域）`)
  console.log('账号'.padEnd(46) + '菜单页  分组   200  403  其它')
  for (const s of summary) {
    console.log(s.label.padEnd(46) + String(s.pages).padEnd(8) + String(s.groups).padEnd(7)
      + String(s.ok).padEnd(5) + String(s.denied).padEnd(5) + String(s.other))
  }
})().catch(e => { console.error(e); process.exit(1) })
