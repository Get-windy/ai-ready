#!/usr/bin/env node
/**
 * 细粒度权限接线 · 重启后验证（2026-09-20）
 *
 * 用途：验证本轮后端改动在**运行时**真的生效 —— 编译通过不等于能启动、更不等于能用
 *      （本轮就踩到：`MybatisPlusInterceptor.getInterceptors()` 返回不可变列表，
 *       直接 add 会让应用启动失败，编译期完全看不出来）。
 *
 * 覆盖：
 *   ① 数据权限拦截器是否真的挂进了 MyBatis 插件链（读启动日志的埋点）
 *   ② 权限生效性清单接口（P0-1 的数据源）
 *   ③ 三个细粒度配置接口可达（字段权限 / 数据范围 / 记录规则）
 *   ④ SoD 规则接口可达 + 校验端点可用（P1-8）
 *   ⑤ 权限模拟 start/status/stop 闭环（P0-4）
 *   ⑥ 角色列表返回 dataScope（P0-3 的数据前提）
 *   ⑦ 回归：核心列表接口仍 200（确认拦截器挂载没有破坏既有查询）
 *
 * 用法：
 *   node tools/verify-permission-changes.cjs
 *   PORT=5655 node tools/verify-permission-changes.cjs
 *
 * 纪律：只读验证；模拟闭环测完立即 stop，不留残留会话态。
 */
const fs = require('fs')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'
const LOG = process.env.BACKEND_LOG
  || path.join(__dirname, '..', 'logs', 'backend-restart-20260920.log')

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', tenantId: '1' } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login() {
  const cap = await req('GET', '/auth/captcha')
  if (!cap.json?.data?.img) throw new Error('验证码接口异常: ' + cap.text.slice(0, 200))
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: USER, password: PWD, tenantName: '系统租户', captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + res.text.slice(0, 300))
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}`)

  // ══ ① 拦截器是否真的挂上了（编译期看不出来，只有启动日志能证明） ══
  section('① 数据权限拦截器挂载（启动日志埋点）')
  let logText = ''
  try {
    logText = fs.readFileSync(LOG, 'utf8')
  } catch {
    ok('读取启动日志', false, `日志不存在: ${LOG}（可用 BACKEND_LOG 指定）`)
  }
  const mounted = /数据权限拦截器已挂载到 MyBatis 插件链：位置 (\d+)，链路共 (\d+) 个插件/.exec(logText)
  ok('日志出现「数据权限拦截器已挂载」', !!mounted, mounted ? `位置 ${mounted[1]} / 共 ${mounted[2]} 个插件` : '')
  if (mounted) {
    // 位置必须早于分页插件：分页会额外生成 count 语句，排在它后面会让 count 缺数据权限条件
    ok('插在分页插件之前（位置 0..2 之间合理）', Number(mounted[1]) <= 2, `实际位置 ${mounted[1]}`)
  }
  ok('应用启动成功（无 Application run failed）', !/Application run failed/.test(logText))

  const token = await login()
  console.log(`登录成功（${USER} @ :${PORT}）`)

  // ══ ② 权限生效性清单（P0-1 的数据源） ══
  section('② 权限生效性清单 GET /permission/effectivity')
  const eff = await req('GET', '/permission/effectivity', { token })
  ok('接口 200', eff.status === 200, `status=${eff.status}`)
  const effData = eff.json?.data
  if (effData && effData.available !== false) {
    const sum = effData.summary || {}
    ok('返回 ineffective 列表', Array.isArray(effData.ineffective), `未生效 ${effData.ineffective?.length} 条`)
    ok('summary 自洽（effective+ineffective+groupNodes = db）',
      sum.effective + sum.ineffective + sum.groupNodes === sum.db,
      `${sum.effective}+${sum.ineffective}+${sum.groupNodes} vs db=${sum.db}`)
    ok('refCounts 非空', effData.refCounts && Object.keys(effData.refCounts).length > 0,
      `${Object.keys(effData.refCounts || {}).length} 条生效权限有引用计数`)
  } else {
    ok('清单已生成（未回落到 available=false）', false, '请先跑 tools/gen-permission-effectivity.py 再打包')
  }

  // ══ ③ 三个细粒度配置接口 ══
  section('③ 细粒度配置接口可达')
  // 用 /role/list（无必填参数）取样本角色；/role/page 的 tenantId 是必填，写死反而容易误报
  const rolePage = await req('GET', '/role/list', { token })
  ok('GET /role/list 200', rolePage.status === 200, `status=${rolePage.status}`)
  const roles = rolePage.json?.data?.records || rolePage.json?.data || []
  const sampleRoleId = roles[0]?.id
  ok('角色列表返回 dataScope 字段（P0-3 前提）',
    roles.length > 0 && 'dataScope' in roles[0],
    `样本: dataScope=${roles[0]?.dataScope}`)

  const fp = await req('GET', `/field-permission/list?roleId=${sampleRoleId}`, { token })
  ok('GET /field-permission/list 200', fp.status === 200, `status=${fp.status}`)

  const ds = await req('GET', `/data-scope/list?roleId=${sampleRoleId}`, { token })
  ok('GET /data-scope/list 200', ds.status === 200, `status=${ds.status}`)

  const rr = await req('GET', `/permission/record/group/${sampleRoleId}`, { token })
  ok('GET /permission/record/group/{id} 200', rr.status === 200, `status=${rr.status}`)

  // ══ ④ SoD ══
  section('④ 职责分离（SoD）')
  const sod = await req('GET', '/sod-rule/page?pageNum=1&pageSize=10', { token })
  ok('GET /sod-rule/page 200', sod.status === 200, `status=${sod.status}`)
  const sodValidate = await req('POST', '/sod-rule/validate', { token, body: roles.slice(0, 2).map(r => r.id) })
  ok('POST /sod-rule/validate 200', sodValidate.status === 200,
    `status=${sodValidate.status}，冲突规则 ${sodValidate.json?.data?.length ?? 0} 条`)

  // ══ ⑤ 权限模拟闭环 ══
  section('⑤ 权限模拟（以用户身份预览）')
  const st0 = await req('GET', '/simulate/status', { token })
  ok('GET /simulate/status 200', st0.status === 200, `status=${st0.status}`)
  ok('初始未在模拟中', st0.json?.data?.simulating === false, `simulating=${st0.json?.data?.simulating}`)

  // 找一个非 admin 的目标用户来模拟（不能模拟自己）
  const userPage = await req('GET', '/user/page?current=1&size=20', { token })
  const users = userPage.json?.data?.records || userPage.json?.records || []
  const target = users.find(u => u.username && u.username !== USER)
  if (target) {
    const started = await req('POST', '/simulate/start', {
      token, body: { targetUserId: target.id, reason: '自动化验证：权限模拟闭环' },
    })
    ok('POST /simulate/start 200', started.status === 200, `status=${started.status} 目标=${target.username}`)
    const st1 = await req('GET', '/simulate/status', { token })
    ok('模拟态**跨请求保持**（本轮修复的核心）',
      st1.json?.data?.simulating === true && String(st1.json?.data?.targetUserId) === String(target.id),
      `simulating=${st1.json?.data?.simulating}, target=${st1.json?.data?.targetUserId}`)
    const stopped = await req('POST', '/simulate/stop', { token })
    ok('POST /simulate/stop 200', stopped.status === 200, `status=${stopped.status}`)
    const st2 = await req('GET', '/simulate/status', { token })
    ok('已恢复未模拟态', st2.json?.data?.simulating === false, `simulating=${st2.json?.data?.simulating}`)
  } else {
    ok('找到可模拟的目标用户', false, '用户列表为空或只有 admin')
  }

  // ══ ⑥ 回归：拦截器挂载没有破坏既有查询 ══
  section('⑥ 回归：核心查询仍正常')
  for (const p of ['/menu/tree?tenantId=1', '/permission/page?tenantId=0&current=1&size=1', '/department/list']) {
    const r = await req('GET', p, { token })
    ok(`GET ${p.split('?')[0]} 200`, r.status === 200, `status=${r.status}`)
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('验证脚本异常:', e.message)
  process.exit(2)
})
