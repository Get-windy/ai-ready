/**
 * 人力资源模块 · 菜单下发内容盘点（只读）
 *
 * 目的：以指定账号登录，打印**后端实际下发的菜单树**，用于判断某个角色能拿到哪些 HR 菜单。
 *   判定口径只有一处：`SysMenuServiceImpl.getUserMegaMenus` → `MenuPermissionDeriver`
 *   （权限码按 `:` 边界展开成前缀集合；菜单码在集合里才算「有对应权限码」并需要持有，
 *    权限码库里没有任何权限码命中它的菜单码一律保持可见 —— fail-open）。
 *
 * ⚠️ 2026-09-23 之前此脚本还会模拟 `MegaMenuPanel` 里那份**全等匹配**的前端副本，
 *    并把「后端下发但前端会隐藏」当成结论 —— 那份副本已删除（详见 MegaMenuPanel 内注释）。
 *    前端渲染结果的真机回归改用 `tools/verify-menu-visibility.cjs`。
 *
 * 用法：node tools/audit-hr-menu-visibility.cjs
 *   账号：默认 e2e_hr_ta（非超管，SYSTEM_ADMIN）；可用 E2E_USER / E2E_PWD / E2E_TENANT 覆盖
 */
const http = require('http')

const API = process.env.E2E_API || 'http://localhost:5655/api'
const USER = process.env.E2E_USER || 'e2e_hr_ta'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'

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

async function login() {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', { username: USER, password: PWD, tenantName: TENANT, captcha: code, captchaKey: cap.json.data.uuid })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 300))
  return token
}

/** 复刻前端 stores/user.ts 的 hasPermission（全等匹配） */
function feHasPermission(permissions, menuCode) {
  if (!menuCode) return true
  return permissions.includes(menuCode) || permissions.includes('*')
}

function walk(menus, out = [], depth = 0) {
  for (const m of menus || []) {
    out.push({ ...m, _depth: depth })
    if (m.children) walk(m.children, out, depth + 1)
  }
  return out
}

;(async () => {
  const token = await login()
  const info = await req('GET', '/auth/userinfo', null, token)
  const permissions = info.json?.data?.permissions || []
  const roles = info.json?.data?.roles || []
  console.log(`账号=${USER} 租户=${TENANT} 角色=${JSON.stringify(roles)} 权限码数=${permissions.length}`)

  const tenantId = info.json?.data?.tenantId ?? 1
  const menuPath = `/menu/user/mega/tenant-admin?tenantId=${tenantId}`
  const menuRes = await req('GET', menuPath, null, token)
  const menus = menuRes.json?.data || []
  console.log(`后端 ${menuPath} 返回顶层菜单数=${menus.length}（HTTP ${menuRes.status}）`)

  const flat = walk(menus)
  const hr = flat.filter(m => String(m.menuCode || '').startsWith('mega:hr') || String(m.menuCode || '').startsWith('hr') || String(m.menuCode || '').startsWith('md:staff'))
  console.log('\n— 后端已下发的人力资源相关菜单 —')
  for (const m of hr) {
    console.log(`  ${'  '.repeat(m._depth)}[${m.menuType}] id=${m.id} ${m.menuName} menuCode=${m.menuCode}`)
  }

  // 全站口径抽样：叶子菜单里有多少 menu_code 恰好等于一条真实权限码
  // （只有这批才可能被「有权限码却没授予」的规则挡掉；其余是 fail-open 的）
  const leaves = flat.filter(m => m.menuType === 1 && m.menuCode)
  const exact = leaves.filter(m => permissions.includes(m.menuCode) || permissions.includes('*'))
  console.log(`\n全站：后端下发叶子菜单 ${leaves.length} 个，其中 menu_code 恰好等于一条所持权限码的 ${exact.length} 个`)

  console.log('\n结论：以上即前端实际渲染集合（前端不再做二次权限过滤）。')
  console.log('      真机渲染回归请看 node tools/verify-menu-visibility.cjs')
})().catch(e => { console.error(e); process.exit(1) })
