/**
 * 系统模块审计 · 租户/菜单可见性实测（只读）
 *
 * 验证：menu_level 过滤是否让普通租户丢失大量租户级菜单
 *   ① 租户2非超管 e2e_hr_t2：GET /api/menu/user/mega/tenant-admin
 *   ② 系统租户超管 admin 对照
 *   ③ 逐一核对 devdb 中 menu_level<>0 的 65 个菜单 ID 是否出现在两边
 *
 * 运行：node tools/verify-system-tenancy.cjs   （后端 5655 需在跑）
 */
const http = require('http')
const { execFileSync } = require('child_process')

const BE = Number(process.env.BE_PORT || 5655)

function req(method, p, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      hostname: 'localhost', port: BE, path: '/api' + p, method,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      }
    }, res => {
      const chunks = []
      res.on('data', c => chunks.push(Buffer.isBuffer(c) ? c : Buffer.from(c)))
      res.on('end', () => {
        const s = Buffer.concat(chunks).toString('utf8')
        try { resolve({ status: res.statusCode, body: JSON.parse(s) }) } catch { resolve({ status: res.statusCode, body: s }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login(username, tenantName) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.body.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    username, password: 'admin123', tenantName,
    captcha: code, captchaKey: cap.body.data.uuid
  })
  const d = res.body && res.body.data
  if (!d || !(d.token || d.accessToken)) throw new Error(`${username} 登录失败: ` + JSON.stringify(res.body).slice(0, 200))
  return d.token || d.accessToken
}

// 从 devdb 读 menu_level<>0 且 tenant-admin 的菜单（用 psql 不可用时退化为内置快照）
function dbMenus() {
  const py = `
import json,io,psycopg2,sys
c=psycopg2.connect(host='localhost',port=5432,dbname='devdb',user='devuser',password='devuser123').cursor()
c.execute("SELECT id,menu_name,path,menu_level,parent_id FROM sys_menu WHERE deleted=0 AND client_type='tenant-admin' AND menu_level<>0 ORDER BY id")
print(json.dumps([{'id':str(r[0]),'name':r[1],'path':r[2],'lvl':r[3],'pid':str(r[4])} for r in c.fetchall()],ensure_ascii=False))
`
  const out = execFileSync('python', ['-c', py], { encoding: 'utf8', cwd: __dirname })
  return JSON.parse(out)
}

function flatten(nodes, acc = []) {
  for (const n of nodes || []) { acc.push(n); if (n.children) flatten(n.children, acc) }
  return acc
}

async function menuIds(token, clientType) {
  const r = await req('GET', `/menu/user/mega/${clientType}`, null, token)
  const data = (r.body && (r.body.data || r.body)) || []
  const flat = flatten(Array.isArray(data) ? data : [])
  return { status: r.status, total: flat.length, ids: new Set(flat.map(m => String(m.id))) }
}

;(async () => {
  const lvl3 = dbMenus()
  console.log(`devdb: tenant-admin 且 menu_level<>0 的菜单 ${lvl3.length} 条`)

  const t2 = await login('e2e_hr_t2', 'E2E验收租户2')
  console.log('✓ 租户2 e2e_hr_t2 登录成功')
  const m2 = await menuIds(t2, 'tenant-admin')
  console.log(`  菜单接口 HTTP ${m2.status}，返回节点 ${m2.total} 个`)

  let admin
  try {
    admin = await login('admin', '系统租户')
    const ma = await menuIds(admin, 'tenant-admin')
    console.log(`✓ 系统租户 admin 登录成功；菜单 HTTP ${ma.status}，返回节点 ${ma.total} 个`)
  } catch (e) {
    console.log('⚠ admin 登录失败（跳过超管对照）:', String(e).slice(0, 120))
  }

  console.log('\n=== menu_level<>0 菜单在租户2 菜单树中的可见性 ===')
  let missingDueLvl = []
  for (const m of lvl3) {
    if (!m2.ids.has(m.id)) missingDueLvl.push(m)
  }
  console.log(`租户2 看不到的: ${missingDueLvl.length}/${lvl3.length}`)
  // 父节点是否也缺失（判断是整块消失还是叶子消失）
  const parentMissing = missingDueLvl.filter(m => !m2.ids.has(m.pid)).length
  console.log(`  其中父节点也不可见的: ${parentMissing}（整块消失）`)
  if (missingDueLvl.length) {
    console.log('\n样例（前 15）:')
    for (const m of missingDueLvl.slice(0, 15)) {
      console.log(`   ${m.id} lvl=${m.lvl} ${m.name} (${m.path})`)
    }
  }
})().catch(e => { console.error('✗ 失败:', e && e.message || e); process.exit(1) })
