/**
 * 人力资源模块 · 菜单可见性口径对账（只读）
 *
 * 目的：验证「后端下发」与「前端渲染」两套权限判定是否同口径。
 *   后端 SysMenuServiceImpl.getUserMegaMenus → MenuPermissionDeriver：**前缀匹配**
 *     （权限码 a:b:c 展开出 a:b、a 也进集合；菜单码在集合里即命中）
 *   前端 MegaMenuPanel.hasPermission → userStore.hasPermission：**全等匹配**
 *     （this.permissions.includes(menuCode)）
 * 只要某菜单的 menu_code 不是一条真实存在的权限码本身，前端就渲染不出来。
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

  console.log('\n— 前端 MegaMenuPanel 判定（menuType=1 才校验，全等匹配） —')
  let hidden = 0
  for (const m of hr) {
    if (m.menuType !== 1) continue
    const ok = feHasPermission(permissions, m.menuCode)
    if (!ok) hidden++
    console.log(`  ${ok ? '显示' : '隐藏'} id=${m.id} ${m.menuName} menuCode=${m.menuCode}`)
  }
  console.log(`\n结论：后端下发但前端会隐藏的叶子菜单 = ${hidden} 个`)

  // 全站口径抽样：所有 type=1 菜单里有多少 menuCode 不在权限码清单中
  const leaves = flat.filter(m => m.menuType === 1 && m.menuCode)
  const notExact = leaves.filter(m => !feHasPermission(permissions, m.menuCode))
  console.log(`全站抽样：后端下发叶子菜单 ${leaves.length} 个，其中前端全等匹配不中 ${notExact.length} 个`)
})().catch(e => { console.error(e); process.exit(1) })
