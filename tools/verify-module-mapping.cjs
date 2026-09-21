#!/usr/bin/env node
/**
 * 模块目录（13 条）与「模块 → 权限码」映射的完整性验证。
 *
 * 背景：授权是**两层** —— ① 模块授权（平台方决定某租户有没有这个模块）；
 * ② 权限（租户内管理员决定某角色能不能做某件事）。两层之间的桥是
 * `sys_module_permission`（模块 → 权限码前缀）。这张表一旦有洞，未来的
 * entitlement 拦截就会"按模块关掉一批接口"却说不清关掉了哪些 —— 故必须能对账。
 *
 * 本脚本核对六件事（全部从真库读，不采信迁移注释）：
 *   ① 模块目录 = 13 条，且 `crm` 已改名「客户服务」、`warehouse` 描述已收窄；
 *   ② 映射表里每个 module_code 都真实存在于 sys_module（防拼错模块码）；
 *   ③ **100% 覆盖**：在役的 type<>1 权限码，每一条都能被某个前缀接住（不许有"(未归属)"）；
 *   ④ 归属判定**确定**：按「最长前缀优先、同长取 sort 小」解析时，不存在歧义并列；
 *   ⑤ `analytics` 映射到的码数 **必须是 0** —— 这是**已知缺口**（分析模块整域还没有码族，
 *      见 E-08），断言为 0 是为了让"将来补了码族"或"有人误删了别的模块的码"都能被发现；
 *   ⑥ 既有租户的开通记录覆盖全部 13 个模块（迁移按"现状不降级"回填）。
 *
 * 用法：node tools/verify-module-mapping.cjs
 */
const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

const EXPECTED_MODULES = {
  'sale': '销售管理',
  'purchase': '采购管理',
  'warehouse': '仓储管理',
  'finance': '财务管理',
  'crm': '客户服务',        // 原名「客户关系」，2026-09-21 改名
  'marketing': '营销管理',
  'master-data': '资料',
  'trade': '交易',
  'dms': '配送',
  'hr': '人力资源',
  'analytics': '分析',
  'settings': '设置',
  'system': '系统',
}

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

function rowsOf(stmt) {
  const out = execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
  const lines = out.replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}
const one = (stmt) => rowsOf(stmt)[0][0]
const N = (stmt) => Number(one(stmt))

;(async () => {
  try {
    section('① 模块目录 = 13 条，改名/收窄已生效')
    const mods = rowsOf(`SELECT module_code, module_name FROM sys_module WHERE deleted = 0 ORDER BY sort_order`)
    const got = Object.fromEntries(mods)
    ok(`模块目录共 13 条`, mods.length === 13, `实际 ${mods.length} 条`)
    for (const [code, name] of Object.entries(EXPECTED_MODULES)) {
      ok(`模块 ${code} = 「${name}」`, got[code] === name, `实际「${got[code] || '缺失'}」`)
    }
    const whDesc = one(`SELECT COALESCE(description,'') FROM sys_module WHERE deleted=0 AND module_code='warehouse'`)
    ok('warehouse 描述已收窄（明示商品档案/往来单位已划走）',
      whDesc.includes('资料'), `描述="${whDesc.slice(0, 60)}…"`)

    section('② 映射表的 module_code 必须都在 sys_module 里')
    const orphans = rowsOf(`SELECT DISTINCT p.module_code FROM sys_module_permission p
      WHERE p.deleted = 0
        AND NOT EXISTS (SELECT 1 FROM sys_module m WHERE m.module_code = p.module_code AND m.deleted = 0)`)
      .map(r => r[0])
    ok('无指向不存在模块码的映射行', orphans.length === 0, `孤儿=${JSON.stringify(orphans)}`)

    section('③ 覆盖率：在役权限码必须 100% 被某个前缀接住')
    const total = N(`SELECT count(*) FROM sys_permission WHERE deleted = 0 AND permission_type <> 1`)
    const unmapped = rowsOf(`SELECT permission_code FROM sys_permission c
      WHERE c.deleted = 0 AND c.permission_type <> 1
        AND NOT EXISTS (SELECT 1 FROM sys_module_permission p
                        WHERE p.deleted = 0 AND c.permission_code LIKE p.permission_prefix || '%')`)
      .map(r => r[0])
    ok(`全部 ${total} 条在役码都能归属到某个模块`, unmapped.length === 0,
      unmapped.length ? `未归属 ${unmapped.length} 条：${JSON.stringify(unmapped.slice(0, 8))}` : `已归属 ${total}/${total}`)

    section('④ 归属判定必须确定（最长前缀优先下不得有并列歧义）')
    const ambiguous = rowsOf(`SELECT c.permission_code, count(DISTINCT p.module_code) AS n
      FROM sys_permission c
      JOIN sys_module_permission p ON p.deleted = 0 AND c.permission_code LIKE p.permission_prefix || '%'
      WHERE c.deleted = 0 AND c.permission_type <> 1
      GROUP BY c.permission_code
      HAVING count(DISTINCT p.module_code) > 1
         AND (SELECT count(*) FROM sys_module_permission p2
              WHERE p2.deleted = 0 AND c.permission_code LIKE p2.permission_prefix || '%'
                AND length(p2.permission_prefix) = (
                    SELECT max(length(p3.permission_prefix)) FROM sys_module_permission p3
                    WHERE p3.deleted = 0 AND c.permission_code LIKE p3.permission_prefix || '%')) > 1`)
    ok('无「同长前缀指向不同模块」的歧义码', ambiguous.length === 0,
      ambiguous.length ? `歧义 ${ambiguous.length} 条：${JSON.stringify(ambiguous.slice(0, 5))}` : '0 条歧义')

    section('⑤ 各模块码数（analytics 必须是 0 —— 已知缺口，不许被"顺手"填上假码）')
    const dist = rowsOf(`
      WITH codes AS (SELECT permission_code FROM sys_permission WHERE deleted = 0 AND permission_type <> 1),
      best AS (SELECT c.permission_code,
                 (SELECT p.module_code FROM sys_module_permission p
                  WHERE p.deleted = 0 AND c.permission_code LIKE p.permission_prefix || '%'
                  ORDER BY length(p.permission_prefix) DESC, p.sort LIMIT 1) AS module_code
               FROM codes c)
      SELECT module_code, count(*) FROM best GROUP BY 1 ORDER BY 2 DESC`)
    const byMod = Object.fromEntries(dist.map(r => [r[0], Number(r[1])]))
    for (const code of Object.keys(EXPECTED_MODULES)) {
      if (code === 'analytics') continue
      ok(`模块 ${code} 至少认领 1 个码`, (byMod[code] || 0) > 0, `${byMod[code] || 0} 个`)
    }
    ok('analytics 当前映射到 0 个码（已知缺口，补码族后本行会红，届时更新脚本）',
      !byMod['analytics'] || byMod['analytics'] === 0, `实际 ${byMod['analytics'] || 0} 个`)
    const sum = Object.values(byMod).reduce((a, b) => a + b, 0)
    ok(`各模块码数之和 = 在役码总数（${sum} = ${total}）`, sum === total, `${sum} vs ${total}`)

    section('⑥ 租户模块开通的语义（用户裁定 2026-09-21）')
    // 裁定：平台级「系统」模块**只开给系统租户**（tenant 1）；「设置」才是租户级的管理设置模块。
    const sysTenants = rowsOf(`SELECT DISTINCT tenant_id FROM sys_tenant_module
      WHERE deleted = 0 AND module_code = 'system' ORDER BY tenant_id`).map(r => r[0])
    ok('「系统」模块只开给系统租户(1)', JSON.stringify(sysTenants) === JSON.stringify(['1']),
      `实际开通租户 = ${JSON.stringify(sysTenants)}`)

    const t1 = N(`SELECT count(*) FROM sys_tenant_module WHERE tenant_id = 1 AND deleted = 0`)
    ok('系统租户(1) 拥有全部 13 个模块', t1 === 13, `实际 ${t1} 条`)

    const t2 = N(`SELECT count(*) FROM sys_tenant_module WHERE tenant_id = 2 AND deleted = 0`)
    ok('业务租户(2) 拥有 12 个模块（13 减去平台级「系统」）', t2 === 12, `实际 ${t2} 条`)

    const t2Settings = N(`SELECT count(*) FROM sys_tenant_module
      WHERE tenant_id = 2 AND module_code = 'settings' AND deleted = 0`)
    ok('业务租户(2) 拥有租户级「设置」模块（它是租户自己的管理设置入口）', t2Settings === 1,
      `实际 ${t2Settings} 条`)

    const crmName = one(`SELECT DISTINCT module_name FROM sys_tenant_module WHERE module_code='crm' AND deleted=0`)
    ok('租户开通记录里 crm 名称已同步为「客户服务」', crmName === '客户服务', `实际「${crmName}」`)

    section('⑦ 租户管理码族 `tenant-admin:`（用户裁定：另立码族，归「设置」模块）')
    const taOwner = rowsOf(`SELECT DISTINCT module_code FROM sys_module_permission
      WHERE deleted = 0 AND permission_prefix = 'tenant-admin:'`).map(r => r[0])
    ok('tenant-admin: 前缀归属「设置」模块', JSON.stringify(taOwner) === JSON.stringify(['settings']),
      `实际归属 = ${JSON.stringify(taOwner)}`)

    // 72 = 70（V11.456.0：7 个从 system: 剥离的子域 + 4 个原裸前缀子域）
    //       + 2（V11.457.0 补漏：裸前缀的 data-scope:{view,set}）
    // 数字写死是故意的：码族规模变化必须有人来看一眼，而不是被"≥1"之类的宽断言放过去。
    const taCount = N(`SELECT count(*) FROM sys_permission WHERE deleted = 0 AND permission_code LIKE 'tenant-admin:%'`)
    ok('tenant-admin: 码族共 72 条（70 + 补漏 2）', taCount === 72, `实际 ${taCount} 条`)

    const oldLeft = N(`SELECT count(*) FROM sys_permission WHERE deleted = 0 AND (
        permission_code LIKE 'system:user:%' OR permission_code LIKE 'system:role:%'
        OR permission_code LIKE 'system:permission:%' OR permission_code LIKE 'system:data-scope:%'
        OR permission_code LIKE 'system:field-permission:%' OR permission_code LIKE 'system:record-rule:%'
        OR permission_code LIKE 'system:sod-rule:%' OR permission_code LIKE 'department:%'
        OR permission_code LIKE 'position:%' OR permission_code LIKE 'permission-template:%'
        OR permission_code LIKE 'role-inheritance:%')`)
    ok('11 个旧前缀族零残留（源码已同步改名，残留即会与非超管 403 并存）', oldLeft === 0, `残留 ${oldLeft} 条`)

    const staleMap = N(`SELECT count(*) FROM sys_module_permission WHERE deleted = 0
      AND module_code = 'system' AND permission_prefix IN
      ('data-scope:', 'department:', 'position:', 'permission-template:', 'role-inheritance:')`)
    ok('「系统」模块不再残留已迁走的码前缀', staleMap === 0, `残留 ${staleMap} 条`)

    section('⑧ 数据完整性（回收开通记录不得误删模块本身 / 不得留下悬空引用）')
    const sysModRows = N(`SELECT count(*) FROM sys_module WHERE deleted = 0 AND module_code = 'system'`)
    ok('sys_module 里的「系统」模块仍在（回收的是开通记录，不是模块）', sysModRows === 1, `实际 ${sysModRows} 条`)
    const dangling = N(`SELECT count(*) FROM sys_tenant_module tm
      WHERE tm.deleted = 0
        AND NOT EXISTS (SELECT 1 FROM sys_module m WHERE m.module_code = tm.module_code AND m.deleted = 0)`)
    ok('开通记录里无指向不存在模块的悬空行', dangling === 0, `悬空 ${dangling} 条`)
  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  }
  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
