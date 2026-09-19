/**
 * 系统模块（平台控制台，client_type=system-admin）· 模块级 E2E
 *
 * 覆盖「系统」菜单 60013 下 7 个分组 / 33 个页面（30 原页 + 2026-09-19 新挂 3 个孤儿页）：
 *   租户管理(62001-62005) · 模块管理(62101-62104) · 系统监控(62201-62206)
 *   数据管理(62301-62305) · 开发工具(62402-62405) · 平台设置(62501-62505) · 系统管理(6130701)
 *
 * 特点：
 *  - 直连 devdb 对账（API 的 total / 字段必须与 SELECT 一致），不只看 HTTP 状态码；
 *  - 写路径一律「改库/调接口 → 三级回读 → 按原值复原」，跑完现场干净；
 *  - 断言口径写死「用 admin（超管）」，因为系统模块的菜单只对超管可见
 *    （平台级表的会话级豁免见 MyBatisPlusConfig.isTenantScopeExempt）。
 *
 * 用法：node tools/e2e-system.cjs [端口，默认 5655]
 * 前置：后端跑在 5655（dev profile）、devdb 可连。
 */
const PORT = Number(process.argv[2] || 5655)
const BASE = `http://localhost:${PORT}/api`
const { Client } = require('I:/AI-Ready/frontend/apps/pc-admin/node_modules/pg')

const db = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  ✅ ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  ❌ ${n}${e ? ' — ' + e : ''}`) }
}

async function req(method, path, { token, body } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', tenantId: '1' } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
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
const q = (sql, p) => db.query(sql, p).then(r => r.rows)
const cnt = (sql, p) => q(sql, p).then(r => Number(r[0].c))

// ════════════════════════════════════════════════════════════════════
async function main() {
  await db.connect()
  const token = await login()
  console.log(`登录成功（${USER}）\n`)

  // ── 0. 菜单完整性 ──
  console.log('【0】sys_menu 系统模块菜单完整性')
  {
    // ⚠️ 2026-09-19 起为 33 页 / 41 行：孤儿页处置（V11.419.0）新挂 3 个 system-admin 页面
    //    （6130702 数据字典 / 6130703 系统配置 / 6130704 权限配置，父菜单 61307 系统管理）。
    //    它们此前是「写了但无菜单、用户不可达」的孤儿页，见系统模块 README §2.5 / §10.4-7。
    const total = await cnt("SELECT count(*) c FROM sys_menu WHERE deleted=0 AND (id=60013 OR parent_id=60013 OR parent_id IN (61301,61302,61303,61304,61305,61306,61307))")
    ok('菜单数 = 1 顶级 + 7 分组 + 33 页 = 41', total === 41, `实际 ${total}`)
    const pages = await cnt("SELECT count(*) c FROM sys_menu WHERE deleted=0 AND parent_id IN (61301,61302,61303,61304,61305,61306,61307) AND menu_type=1")
    ok('页面项 33 个（30 原页 + 3 个新挂孤儿页）', pages === 33, `实际 ${pages}`)
    const clientTypes = await q("SELECT DISTINCT client_type FROM sys_menu WHERE id=60013 OR parent_id=60013 OR parent_id IN (61301,61302,61303,61304,61305,61306,61307)")
    ok('除菜单管理外均为 system-admin', clientTypes.filter(r => r.client_type !== 'system-admin').length <= 1,
      clientTypes.map(r => r.client_type).join(','))
  }

  // ── 1. 租户管理 62001-62005 ──
  console.log('\n【1】租户管理 62001-62005')
  {
    const a = await req('GET', '/tenant/page?pageNum=1&pageSize=20', { token })
    const dbCnt = await cnt('SELECT count(*) c FROM sys_tenant WHERE deleted=0')
    ok('租户列表 total 与 DB 一致', Number(a.json?.data?.total) === dbCnt, `api=${a.json?.data?.total} db=${dbCnt}`)
    const rec = a.json?.data?.records?.[0] || {}
    ok('字段名与实体对齐（contactPerson/expireTime）', 'contactPerson' in rec && 'expireTime' in rec)
    ok('不再出现错位字段 contactName/expireDate', !('contactName' in rec) && !('expireDate' in rec))

    for (const [p, name] of [
      ['/tenant-registration/pending', '租户审批'],
      ['/tenant-package/list', '租户套餐'],
      ['/tenant-quota/list', '配额管理'],
    ]) {
      const r = await req('GET', p, { token })
      ok(`${name} ${p} 可达`, r.status === 200, `status=${r.status}`)
    }

    // 模块授权：写 → 回读 → 复原（曾因 3 个缺陷整链路不可用）
    const menus = await req('GET', '/menu/list?tenantId=1', { token })
    ok('菜单候选 /menu/list 可用', Array.isArray(menus.json?.data) && menus.json.data.length > 300,
      `count=${menus.json?.data?.length}`)
    const mid = Number(menus.json.data[0].id)
    const w = await req('PUT', '/tenant-menu/2', { token, body: [mid] })
    ok('模块授权 PUT 200', w.status === 200)
    const rows = await q('SELECT tenant_id FROM sys_tenant_menu WHERE menu_id=$1', [mid])
    ok('授权写入归属正确（tenant_id=2，非会话租户 1）', rows.some(r => String(r.tenant_id) === '2'),
      `rows=${rows.map(r => r.tenant_id).join(',')}`)
    const w2 = await req('PUT', '/tenant-menu/2', { token, body: [mid] })
    ok('重复保存不再 400（唯一索引冲突已修）', w2.status === 200, `status=${w2.status}`)
    await req('PUT', '/tenant-menu/2', { token, body: [] })
    ok('空数组 = 撤销全部授权', (await cnt('SELECT count(*) c FROM sys_tenant_menu WHERE tenant_id=2')) === 0)
    ok('原有 4 行（tenant_id=0）未受影响', (await cnt('SELECT count(*) c FROM sys_tenant_menu')) === 4)
  }

  // ── 2. 模块管理 62101-62104 ──
  console.log('\n【2】模块管理 62101-62104')
  {
    const dbCnt = await cnt('SELECT count(*) c FROM sys_module WHERE deleted=0')
    const a = await req('GET', '/module/list?pageNum=1&pageSize=20', { token })
    ok('模块列表 total 与 DB 一致', Number(a.json?.total) === dbCnt, `api=${a.json?.total} db=${dbCnt}`)
    const b = await req('GET', '/module/list?keyword=营销', { token })
    ok('keyword 筛选生效', Number(b.json?.total) === 1)
    const c = await req('GET', '/module/list?status=0', { token })
    ok('status 筛选生效', Number(c.json?.total) === 1)
    const d = await req('GET', '/module/list?pageNum=1&pageSize=2', { token })
    ok('分页生效', d.json?.records?.length === 2)

    const vCnt = await cnt('SELECT count(*) c FROM sys_module_version')
    const v = await req('GET', '/module/versions?pageNum=1&pageSize=100', { token })
    ok('版本列表 total 与 DB 一致', Number(v.json?.total) === vCnt, `api=${v.json?.total} db=${vCnt}`)
    const times = v.json.records.map(r => r.releaseTime)
    const firstNull = times.findIndex(t => !t)
    const lastNonNull = times.reduce((acc, t, i) => (t ? i : acc), -1)
    ok('NULLS LAST（未发布草稿沉底）', firstNull === -1 || firstNull > lastNonNull)
    const rel = await req('GET', '/module/releases?pageNum=1&pageSize=100', { token })
    ok('releases 与 versions 同源', Number(rel.json?.total) === Number(v.json?.total))

    const u = await req('GET', '/module/usage?days=30', { token })
    ok('使用统计 summary 真实', Number(u.json?.summary?.totalModules) === dbCnt, `totalModules=${u.json?.summary?.totalModules}`)
    ok('统计窗口可调', Number((await req('GET', '/module/usage?days=7', { token })).json?.windowDays) === 7)

    // 发布 → 回读 → 复原
    const verBefore = (await q('SELECT version FROM sys_module WHERE id=6'))[0]?.version
    const cntBefore = await cnt('SELECT count(*) c FROM sys_module_version')
    const p = await req('POST', '/module/6/publish', { token, body: { version: '9.9.9-e2e', changelog: 'E2E' } })
    ok('发布成功', p.json?.success === true)
    ok('模块当前版本被同步', (await q('SELECT version FROM sys_module WHERE id=6'))[0]?.version === '9.9.9-e2e')
    ok('新增 1 行版本记录', (await cnt('SELECT count(*) c FROM sys_module_version')) === cntBefore + 1)
    await db.query("DELETE FROM sys_module_version WHERE module_id=6 AND version='9.9.9-e2e'")
    await db.query('UPDATE sys_module SET version=$1 WHERE id=6', [verBefore])
    ok('已复原', (await q('SELECT version FROM sys_module WHERE id=6'))[0]?.version === verBefore
      && (await cnt('SELECT count(*) c FROM sys_module_version')) === cntBefore)

    // 回滚 → 回读 → 复原
    const rb = await req('POST', '/module/1/rollback', { token, body: { version: '2.2.0-beta' } })
    ok('回滚成功', rb.json?.success === true)
    ok('当前版本已回滚', (await q('SELECT version FROM sys_module WHERE id=1'))[0]?.version === '2.2.0-beta')
    ok('目标版本不存在时拒绝', (await req('POST', '/module/1/rollback', { token, body: { version: '不存在' } })).json?.success === false)
    await db.query("UPDATE sys_module SET version='2.1.0' WHERE id=1")
    ok('已复原', (await q('SELECT version FROM sys_module WHERE id=1'))[0]?.version === '2.1.0')
  }

  // ── 3. 系统监控 62201-62206 ──
  console.log('\n【3】系统监控 62201-62206')
  {
    for (const p of ['/monitor/health/status', '/monitor/health/dependencies', '/monitor/health/jvm', '/monitor/health/disk',
      '/monitor/performance/realtime', '/monitor/alerts/rules', '/monitor/alerts/statistics', '/monitor/overview']) {
      const r = await req('GET', p, { token })
      ok(`${p} 200`, r.status === 200, `status=${r.status}`)
    }
    const log = await req('GET', '/log/page?pageNum=1&pageSize=5', { token })
    const logCnt = await cnt('SELECT count(*) c FROM sys_oper_log')
    const logTotal = Number(log.json?.total ?? log.json?.data?.total)
    ok('系统日志 total 与 DB 一致（解包已修）', logTotal === logCnt, `api=${logTotal} db=${logCnt}`)
    const audit = await req('GET', '/audit/query?pageNum=1&pageSize=5', { token })
    ok('操作审计查询 200', audit.status === 200)

    const cache = await req('GET', '/cache/status', { token })
    const d = cache.json?.data || {}
    ok('缓存概览为真实 Redis 数据（非 9 条硬编码区域）',
      cache.json?.code === 200 && Number(d.totalKeys) > 0 && Array.isArray(d.regions),
      `totalKeys=${d.totalKeys} regions=${(d.regions || []).length}`)
  }

  // ── 4. 数据管理 62301-62305 ──
  console.log('\n【4】数据管理 62301-62305')
  {
    for (const [p, name] of [
      ['/data-source/list?pageNum=1&pageSize=10', '连接管理'],
      ['/data-source/slow-query/list?pageNum=1&pageSize=10', '慢查询'],
      ['/data-source/backup/list?pageNum=1&pageSize=10', '备份管理'],
      ['/data-source/sync/list?pageNum=1&pageSize=10', '同步任务'],
      ['/data-source/cleanup/list?pageNum=1&pageSize=10', '清理规则'],
    ]) {
      const r = await req('GET', p, { token })
      ok(`${name} 200`, r.status === 200, `status=${r.status}`)
    }
    // 清理规则写路径（曾因 status 列 integer/实体 String 冲突必 500）
    const before = await cnt('SELECT count(*) c FROM sys_data_cleanup_rule')
    const created = await req('POST', '/data-source/cleanup/', {
      token,
      body: { ruleName: 'E2E-临时规则', targetTable: 'sys_oper_log', conditionColumn: 'oper_time', retentionDays: 90, status: 'enabled' },
    })
    ok('清理规则新增不再 500', created.status === 200, `status=${created.status}`)
    ok('新增真实落库', (await cnt('SELECT count(*) c FROM sys_data_cleanup_rule')) === before + 1)
    await db.query("DELETE FROM sys_data_cleanup_rule WHERE rule_name='E2E-临时规则'")
    ok('已复原', (await cnt('SELECT count(*) c FROM sys_data_cleanup_rule')) === before)
  }

  // ── 5. 开发工具 + 平台设置 + 系统管理 ──
  console.log('\n【5】开发工具 62402-62405 / 平台设置 62501-62505 / 系统管理 6130701')
  {
    ok('模板管理 200', (await req('GET', '/import-templates', { token })).status === 200)
    const openapi = await fetch(`http://localhost:${PORT}/v3/api-docs`).then(r => r.status).catch(() => 0)
    ok('API文档可取的 OpenAPI spec', openapi === 200, `/v3/api-docs → ${openapi}`)
    const st = await req('GET', '/scheduler/task/page?current=1&size=5', { token })
    const stCnt = await cnt('SELECT count(*) c FROM scheduled_task')
    ok('定时任务分页 total 与 DB 一致', Number(st.json?.data?.total ?? st.json?.total) === stCnt,
      `api=${st.json?.data?.total ?? st.json?.total} db=${stCnt}`)

    const cfg = await req('GET', '/config/page?pageNum=1&pageSize=5', { token })
    const cfgCnt = await cnt('SELECT count(*) c FROM sys_config')
    ok('平台参数分页 total 与 DB 一致（已真实落库）', Number(cfg.json?.total) === cfgCnt,
      `api=${cfg.json?.total} db=${cfgCnt}`)

    for (const [p, name] of [
      ['/mail/config', '邮件配置'],
      ['/sms/config', '短信配置'],
      ['/storage-config/config', '存储配置'],
      ['/system/security/policy', '安全策略'],
    ]) {
      const r = await req('GET', p, { token })
      ok(`${name} 200`, r.status === 200 && (r.json?.code === 200 || r.json?.data !== undefined), `status=${r.status}`)
    }

    // 安全策略写路径 → 回读 → 复原（含 rate_limit 类型修复 + 全局租户口径）
    const spBefore = (await q('SELECT * FROM sys_security_policy WHERE tenant_id=0 ORDER BY id LIMIT 1'))[0]
    if (spBefore) {
      const body = {
        enabled: spBefore.enabled, lockThreshold: spBefore.lock_threshold, lockDuration: spBefore.lock_duration,
        captchaEnabled: spBefore.captcha_enabled, twoFactorEnabled: spBefore.two_factor_enabled,
        passwordMinLength: spBefore.password_min_length, requireUpper: spBefore.require_upper,
        requireLower: spBefore.require_lower, requireDigit: spBefore.require_digit,
        requireSpecial: spBefore.require_special, passwordExpireDays: spBefore.password_expire_days,
        sessionTimeout: spBefore.session_timeout, singleDevice: spBefore.single_device,
        rateLimit: 1234, ipWhitelist: spBefore.ip_whitelist,
        auditRetentionDays: spBefore.audit_retention_days, logSensitiveOps: spBefore.log_sensitive_ops,
        logLogin: spBefore.log_login,
      }
      const w = await req('POST', '/system/security/policy/save', { token, body })
      const after = (await q('SELECT rate_limit FROM sys_security_policy WHERE tenant_id=0'))[0]
      ok('安全策略保存落库（全局租户口径 tenant_id=0）', w.status === 200 && Number(after?.rate_limit) === 1234,
        `status=${w.status} rate_limit=${after?.rate_limit}`)
      await db.query('UPDATE sys_security_policy SET rate_limit=$1 WHERE tenant_id=0', [spBefore.rate_limit])
      ok('已复原', Number((await q('SELECT rate_limit FROM sys_security_policy WHERE tenant_id=0'))[0]?.rate_limit) === Number(spBefore.rate_limit))
      ok('未产生 tenant_id=1 影子行', (await cnt('SELECT count(*) c FROM sys_security_policy WHERE tenant_id=1')) === 0)
    }
    ok('菜单管理树 200', (await req('GET', '/menu/tree?tenantId=0', { token })).status === 200)
  }

  // ── 6. 权限码种子 ──
  console.log('\n【6】权限码种子（P0-16）')
  {
    for (const [like, min, name] of [
      ['system:menu%', 6, 'system:menu:*'],
      ['system:module%', 4, 'system:module:*'],
      ['datasource:%', 16, 'datasource:*'],
      ['tenant:menu%', 3, 'tenant:menu:*'],
    ]) {
      const n = await cnt(`SELECT count(*) c FROM sys_permission WHERE deleted=0 AND permission_code LIKE '${like}'`)
      ok(`${name} 已落库（≥${min}）`, n >= min, `实际 ${n}`)
    }
  }

  console.log(`\n═════════════════\n通过 ${pass} / 失败 ${fail}`)
  await db.end()
  process.exit(fail === 0 ? 0 : 1)
}

main().catch(async e => {
  console.error('E2E 异常:', e.message)
  try { await db.end() } catch { /* noop */ }
  process.exit(1)
})
