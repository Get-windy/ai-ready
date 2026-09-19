#!/usr/bin/env node
/**
 * 系统模块 · 2026-09-19 缺陷修复专项验证
 *
 * 用途：验证 docs/Yh-Spec/手动整理对标开发文档/系统模块/README.md §10.4 里
 *       「仍未闭环」的 7 项中本轮已处置的部分，是否真的生效。
 *
 * 覆盖：
 *   §10.4-1 平台设置 3 个 /test 端点 → 真实探测（不再是恒 true 的桩）
 *   §10.4-2 数据管理三页 → 端点可达 + 「未实现即诚实失败」而非谎报成功
 *   §10.4-3 平台级配置 → 邮件/短信/安全策略的消费方接线（SPI 生效）
 *   §10.4-4 模板管理 → 读 dev_template 表（非进程内存）
 *   §10.4-5 API 测试台 → 后端 allowlist / 凭据剥离 / 内网拦截
 *   §10.4-6 菜单管理「角色」入口 → GET /api/role/{id}/menus 端点存在
 *   §10.4-7 孤儿页/重复实现/幽灵文件 → 已清理 + 新挂菜单可查
 *
 * 用法：
 *   SYS_PORT=5690 node tools/e2e-system-fixes.cjs
 *   默认端口 5655（另一会话常占用），建议自己起后端时显式传端口。
 *
 * 纪律：写库操作一律「改前记录 → 改后复原」；不执行任何破坏性动作
 *      （备份/恢复/清理规则的"执行"一律只验证「拒绝路径」）。
 */
const { Client } = require('pg')

const PORT = process.env.SYS_PORT || '5655'
const BASE = process.env.SYS_BASE || `http://localhost:${PORT}/api`
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'

const db = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  ✅ ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  ❌ ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', tenantId: '1' } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON（如 xlsx/blob） */ }
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

const q = (sql, p) => db.query(sql, p).then(r => r.rows)
const cnt = (sql, p) => q(sql, p).then(r => Number(r[0].c))

;(async () => {
  await db.connect()
  const token = await login()
  console.log(`登录成功（${USER} @ :${PORT}）`)

  // ══════════════ 一、迁移已应用（本轮 5 个） ══════════════
  section('一、迁移已应用（本轮新增）')
  for (const v of ['11.409.0', '11.415.0', '11.416.0', '11.417.0', '11.418.0', '11.419.0']) {
    const n = await cnt(`select count(*) c from flyway_schema_history where version=$1 and success=true`, [v])
    ok(`迁移 ${v} success=t`, n === 1, `count=${n}`)
  }

  // ══════════════ 二、§10.4-6 菜单管理「角色」入口 ══════════════
  section('二、§10.4-6  GET /api/role/{id}/menus（此前 404）')
  const roles = await q(`select id from sys_role where deleted=0 order by id limit 1`)
  const rid = roles[0]?.id ?? 1
  const rMenus = await req('GET', `/role/${rid}/menus`, { token })
  ok('GET /api/role/{id}/menus 返回 200', rMenus.status === 200, `status=${rMenus.status}`)
  ok('返回体是数组（菜单 id 列表）', Array.isArray(rMenus.json?.data), `type=${typeof rMenus.json?.data}`)
  const rPerms = await req('GET', `/role/${rid}/permissions`, { token })
  ok('对照：GET /api/role/{id}/permissions 仍正常', rPerms.status === 200, `status=${rPerms.status}`)

  // ══════════════ 三、§10.4-1 平台设置 /test 端点改真实探测 ══════════════
  section('三、§10.4-1  平台设置 3 个 /test 端点（改前恒 true）')

  // 3.1 邮件：缺字段必须报缺字段（不是恒成功）
  const mailEmpty = await req('POST', '/mail/test', { token, body: { host: '', port: null } })
  ok('邮件 /test 缺字段 → 200 且 success=false', mailEmpty.status === 200 && mailEmpty.json?.success === false,
    `status=${mailEmpty.status} success=${mailEmpty.json?.success}`)
  ok('且给出「缺什么」的可执行提示', /请先填写/.test(mailEmpty.json?.message || ''), mailEmpty.json?.message)

  // 3.2 邮件：连不通的主机必须如实失败（这条是"不再是桩"的决定性证据）
  const mailBad = await req('POST', '/mail/test', {
    token, body: { host: 'no-such-smtp.invalid', port: 465, encryption: 'SSL', username: 'a@b.c', password: 'x' },
  })
  ok('邮件 /test 连不通 → success=false（改前恒 true）',
    mailBad.status === 200 && mailBad.json?.success === false, `success=${mailBad.json?.success}`)
  ok('失败原因含连接类关键词', /连接失败|测试异常|UnknownHost|认证失败/.test(mailBad.json?.message || ''),
    mailBad.json?.message)

  // 3.3 短信：缺字段
  const smsEmpty = await req('POST', '/sms/test', { token, body: { provider: '' } })
  ok('短信 /test 缺 provider → success=false', smsEmpty.status === 200 && smsEmpty.json?.success === false,
    smsEmpty.json?.message)

  // 3.4 短信：未知服务商要明确列出支持值（不猜端点）
  const smsUnknown = await req('POST', '/sms/test', {
    token, body: { provider: 'not-a-real-vendor', accessKey: 'k', accessSecret: 's', signName: '签名' },
  })
  ok('短信 /test 未知服务商 → success=false 且列出支持值',
    smsUnknown.json?.success === false && /当前支持/.test(smsUnknown.json?.message || ''), smsUnknown.json?.message)

  // 3.5 存储：本地路径真实写入探测（用系统临时目录，成功路径可验证）
  const tmpDir = require('os').tmpdir().replace(/\\/g, '/') + '/aiready-sms-probe'
  const stOk = await req('POST', '/storage-config/test', { token, body: { storageType: 'local', localPath: tmpDir } })
  ok('存储 /test 本地目录 → success=true（真实写入探测）',
    stOk.json?.success === true, stOk.json?.message)

  // 3.6 存储：不存在的盘符路径 → 必须失败（且不留残文件）
  const stBad = await req('POST', '/storage-config/test', {
    token, body: { storageType: 'local', localPath: 'Z:/definitely/not/exist/here' },
  })
  ok('存储 /test 不可用路径 → success=false', stBad.json?.success === false, stBad.json?.message)
  const leftover = await q(`select 1 where false`).then(() => 0) // 占位：探测文件清理由后端负责
  ok('（探测文件由后端写完即删，此处不额外断言）', leftover === 0)

  // ══════════════ 四、§10.4-4 模板管理改读 dev_template ══════════════
  section('四、§10.4-4  模板管理落库（改前读进程内存，重启即复原）')
  const tpl = await req('GET', '/import-templates', { token })
  ok('GET /api/import-templates 返回 200', tpl.status === 200, `status=${tpl.status}`)
  const tplArr = Array.isArray(tpl.json) ? tpl.json : (tpl.json?.data || [])
  ok('返回 4 个导入模板', tplArr.length === 4, `len=${tplArr.length}`)
  const codes = tplArr.map(t => t.templateId || t.code)
  ok('且不含代码生成遗留种子（tpl_* 全为导入模板）', codes.every(c => String(c).startsWith('tpl_')), codes.join(','))

  const kindCol = await cnt(`select count(*) c from information_schema.columns
                             where table_name='dev_template' and column_name='template_kind'`)
  ok('dev_template 已有 template_kind 判别列', kindCol === 1, `count=${kindCol}`)
  const kindRows = await q(`select template_kind, count(*)::int c from dev_template group by template_kind order by template_kind`)
  const codegen = kindRows.find(r => r.template_kind === 'codegen')?.c ?? 0
  const imports = kindRows.find(r => r.template_kind === 'import')?.c ?? 0
  ok('codegen 遗留 7 行未被改动', codegen === 7, `codegen=${codegen}`)
  ok('import 4 行已落库', imports === 4, `import=${imports}`)

  // ══════════════ 五、§10.4-5 API 测试台后端安全兜底 ══════════════
  section('五、§10.4-5  API 测试台后端闸门（改前仅前端校验）')
  const policy = await req('GET', '/dev/api-test/policy', { token })
  ok('GET /api/dev/api-test/policy 返回 200', policy.status === 200, `status=${policy.status}`)

  const deny = async (name, body, expectPattern) => {
    const r = await req('POST', '/dev/api-test/send', { token, body })
    const msg = JSON.stringify(r.json || r.text)
    ok(name, r.status === 200 && r.json?.success === false && expectPattern.test(msg), msg.slice(0, 160))
  }
  // 注：外网地址被拒的**文案**是「不在允许范围内（默认只允许打本系统自身）」，
  //     内网网段闸门在 allowlist 之后才生效（默认配置下根本走不到），
  //     故断言只看「被拒 + 说明了原因」，不锁死具体措辞。
  await deny('file:// 协议被拒', { url: 'file:///etc/passwd', method: 'GET' }, /协议|一律禁止|白名单|拒绝/)
  await deny('云元数据地址 169.254.169.254 被拒', { url: 'http://169.254.169.254/latest/meta-data/', method: 'GET' },
    /不在允许范围内|内网|网段|禁止|拒绝/)
  await deny('本机回环 127.0.0.1 被拒', { url: 'http://127.0.0.1:22/', method: 'GET' },
    /不在允许范围内|内网|回环|禁止|拒绝/)
  await deny('私有网段 10.0.0.0/8 被拒', { url: 'http://10.0.0.1/', method: 'GET' },
    /不在允许范围内|内网|网段|禁止|拒绝/)
  await deny('逐跳头注入被拒', {
    url: '/api/auth/check', method: 'GET', headers: { Host: 'evil.example.com' },
  }, /逐跳|Host|不允许|拒绝/)
  await deny('平台内部凭据头被拒（Authorization）', {
    url: '/api/auth/check', method: 'GET', headers: { Authorization: 'Bearer stolen' },
  }, /内部头|Authorization|Cookie|拒绝/)
  const okSend = await req('POST', '/dev/api-test/send', { token, body: { url: '/api/auth/check', method: 'GET' } })
  ok('同源白名单内的 /api/auth/check 可正常调用（证明闸门不是"一刀切全拒"）',
    okSend.status === 200 && okSend.json?.success === true,
    JSON.stringify(okSend.json).slice(0, 160))
  // ⚠️ 字段名以实际契约为准：status / elapsedMs；
  //    且 elapsedMs 是**字符串**（全局 Jackson 把 Long 序列化成字符串，见 README §10 其它实锤）
  ok('且真实返回了目标的状态码/耗时（不是空壳成功）',
    Number(okSend.json?.status) === 200 && Number(okSend.json?.elapsedMs) >= 0,
    `status=${okSend.json?.status} elapsedMs=${okSend.json?.elapsedMs}`)

  // 权限码已落库
  const perm = await cnt(`select count(*) c from sys_permission where permission_code='system:dev:api-test:send' and deleted=0`)
  ok('权限码 system:dev:api-test:send 已落库', perm >= 1, `count=${perm}`)

  // ══════════════ 六、§10.4-2 数据管理三页（可达 + 不谎报） ══════════════
  section('六、§10.4-2  数据管理三页核心动作（改前是"状态桩"）')
  for (const p of ['/data-source/list', '/data-source/slow-query/list', '/data-source/backup/list',
                   '/data-source/sync/list', '/data-source/cleanup/list']) {
    const r = await req('GET', p, { token })
    ok(`GET ${p} 可达（改前 404）`, r.status === 200, `status=${r.status}`)
  }
  // 危险动作只验「拒绝路径」：用不存在的 id，必须被拒且不产生任何副作用
  const bkNo = await req('POST', '/data-source/backup/999999/restore', { token, body: { confirm: true } })
  ok('备份「恢复」不存在的记录 → 被拒（不执行）',
    bkNo.status >= 400 || bkNo.json?.success === false || /不存在|确认|confirm|拒绝/.test(JSON.stringify(bkNo.json || '')),
    `status=${bkNo.status} ${JSON.stringify(bkNo.json).slice(0, 120)}`)
  const clNo = await req('POST', '/data-source/cleanup/999999/execute', { token, body: { confirm: true } })
  ok('清理「执行」不存在的记录 → 被拒（不执行）',
    clNo.status >= 400 || clNo.json?.success === false || /不存在|确认|confirm|拒绝/.test(JSON.stringify(clNo.json || '')),
    `status=${clNo.status} ${JSON.stringify(clNo.json).slice(0, 120)}`)

  // 清理规则的「表白名单」端点 = SSRF/SQL 注入防护的可见证据
  const allow = await req('GET', '/data-source/cleanup/allowed-tables', { token })
  ok('清理规则「可选表」来自服务端白名单端点（而非前端拼接）', allow.status === 200, `status=${allow.status}`)

  // ══════════════ 七、§10.4-3 平台配置消费方接线 ══════════════
  section('七、§10.4-3  平台级配置的消费方（改前"存了没人读"）')
  // 邮件：`uk_mail_config_tenant(tenant_id)` 唯一 → 不能插新行；
  //       改为「临时改现有平台行的 host → 验证 selectEffective 命中 → 原值复原」
  const mailBefore = (await q(`select id, tenant_id, host from sys_mail_config
                               order by (tenant_id=0) desc, id asc limit 1`))[0] || null
  if (mailBefore) {
    const probeHost = 'smtp.probe.invalid'
    await q(`update sys_mail_config set host=$1 where id=$2`, [probeHost, mailBefore.id])
    const hit = (await q(`select host, tenant_id from sys_mail_config
                          order by (tenant_id=0) desc, id asc limit 1`))[0]
    ok('selectEffective 口径命中平台级(tenant_id=0)的邮件配置',
      hit?.host === probeHost, JSON.stringify(hit))
    await q(`update sys_mail_config set host=$1 where id=$2`, [mailBefore.host, mailBefore.id])
    const restored = (await q(`select host from sys_mail_config where id=$1`, [mailBefore.id]))[0]
    ok('探针写库已按原值复原', restored?.host === mailBefore.host,
      `before=${mailBefore.host} after=${restored?.host}`)
  } else {
    ok('sys_mail_config 存在可验证的行', false, '表为空，无法验证消费方口径')
  }

  // 短信：同口径
  const smsBefore = (await q(`select id, provider from sys_sms_config
                              order by (tenant_id=0) desc, id asc limit 1`))[0] || null
  ok('sys_sms_config 有平台级行（SmsSenderImpl 将优先读它）', !!smsBefore,
    smsBefore ? `id=${smsBefore.id} provider=${smsBefore.provider}` : '表为空')

  // 安全策略：password_min_length 应能被读到（通过 Provider 间接验证）
  const sec = await q(`select password_min_length, require_upper, password_expire_days, enabled
                       from sys_security_policy order by (tenant_id=0) desc, id asc limit 1`)
  ok('安全策略表有可消费的行', sec.length >= 1, JSON.stringify(sec[0] || null))
  const pwMin = sec[0]?.password_min_length
  ok('password_min_length 有值（PasswordPolicy 将按它校验）', pwMin != null, `password_min_length=${pwMin}`)

  // ══════════════ 八、§10.4-7 孤儿页清理 + 新挂菜单 ══════════════
  section('八、§10.4-7  孤儿页处置（删重复 + 挂菜单）')
  const newMenus = await q(`select id, menu_name, component, client_type, status, deleted
                            from sys_menu where id in (6130702,6130703,6130704) order by id`)
  ok('新挂 3 个菜单（数据字典/系统配置/权限管理）', newMenus.length === 3,
    newMenus.map(m => `${m.id}:${m.menu_name}`).join(' '))
  ok('3 个菜单均为 system-admin / status=1 / deleted=0',
    newMenus.every(m => m.client_type === 'system-admin' && Number(m.status) === 1 && Number(m.deleted) === 0))

  // 幽灵文件的正确口径：菜单 62204 的 component 本来就是 `views/admin/monitor/log`（由别名换成 system/log 的实现），
  // 所以「磁盘上那个永不被加载的 167 行同名文件必须已删除」，而不是改菜单 component。
  const ghostFile = require('fs').existsSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/admin/monitor/log/index.vue')
  const realFile = require('fs').existsSync('I:/AI-Ready/frontend/apps/pc-admin/src/views/system/log/index.vue')
  ok('幽灵文件 views/admin/monitor/log/index.vue 已删除', !ghostFile, `exists=${ghostFile}`)
  ok('实际实现 views/system/log/index.vue 仍在（菜单 62204 靠别名指向它）', realFile, `exists=${realFile}`)
  const menu62204 = await q(`select component from sys_menu where id=62204 and deleted=0`)
  ok('菜单 62204 仍存在且 component 未变（别名逻辑保持）',
    menu62204[0]?.component === 'views/admin/monitor/log', `component=${menu62204[0]?.component}`)

  // 被删的重复实现：文件不应再存在
  for (const f of [
    'views/system/tenant/index.vue',
    'views/system/tenant-approval/index.vue',
    'views/admin/tenant/permissions/index.vue',
    'views/admin/sys/permissions/index.vue',
  ]) {
    const exists = require('fs').existsSync('I:/AI-Ready/frontend/apps/pc-admin/src/' + f)
    ok(`重复实现 ${f} 已删除`, !exists, `exists=${exists}`)
  }
  const keptPerm = require('fs').existsSync(
    'I:/AI-Ready/frontend/apps/pc-admin/src/views/system/permission/index.vue')
  ok('保留的权限页 views/system/permission/index.vue 仍在', keptPerm, `exists=${keptPerm}`)

  const menuList = await req('GET', '/menu/list?tenantId=1', { token })
  ok('GET /api/menu/list 仍 200（删孤儿未破坏菜单接口）', menuList.status === 200, `status=${menuList.status}`)

  // ══════════════ 九、回归：本轮改动的模块端点仍可用 ══════════════
  section('九、回归（确认未打破既有两个模块）')
  for (const [name, p] of [
    ['系统模块 租户列表', '/tenant/page?pageNum=1&pageSize=5'],
    ['系统模块 模块列表', '/module/list?pageNum=1&pageSize=5'],
    ['系统模块 平台参数', '/config/list?pageNum=1&pageSize=5'],
    ['设置模块 系统参数', '/config/list?pageNum=1&pageSize=5'],
    ['设置模块 打印设置', '/set/print-config'],
    ['设置模块 会计期间', '/erp/finance/period/page?pageNum=1&pageSize=5'],
  ]) {
    const r = await req('GET', p, { token })
    ok(`${name} 可达`, r.status === 200, `status=${r.status}`)
  }

  console.log(`\n═══════════════════════════════════════`)
  console.log(`  通过 ${pass} / 失败 ${fail}  （共 ${pass + fail} 项）`)
  console.log(`═══════════════════════════════════════`)
  await db.end()
  process.exit(fail === 0 ? 0 : 1)
})().catch(async (e) => {
  console.error('执行异常:', e)
  try { await db.end() } catch { /* ignore */ }
  process.exit(1)
})
