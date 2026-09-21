#!/usr/bin/env node
/**
 * 采购合同（菜单 81010）四重断点修复 · 重启后验证（2026-09-21）
 *
 * 覆盖 PUR-BREAK-01：菜单在、页面在，但 `/page`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`/export`
 * 五个端点后端缺失，且 `purchase_contract` 表在库中不存在 —— 点进去即 404/500。
 *
 * 本脚本走**完整闭环**：建 → 查 → 列 → 改 → 删。
 * 其中「创建后必须能在列表里查到」是**租户口径的关键断言**：
 *   `PurchaseContractMapper` 用自定义 `@Insert` 注解 SQL，不经过 MyBatis-Plus 的 `insertFill`，
 *   若 Service 漏写 `tenant_id`，行会落成 `tenant_id = 0`，
 *   而多租户插件会给列表查询注入 `tenant_id = 1` ⇒ **建成功但列表查不到**。
 *   只断言「创建返回 200」会漏掉这个 P0，所以必须回查。
 *
 * 用法：node tools/verify-purchase-contract.cjs
 *
 * 纪律：用完即删（脚本内 DELETE 自己建的那条），不留测试残留。
 */
const PORT = process.env.PORT || '5655'
const BASE = process.env.BASE || `http://localhost:${PORT}/api`
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }
const TENANT2 = { u: 'e2e_hr_t2', p: 'admin123', t: 'E2E验收租户2' }

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

async function req(method, p, { token, headers = {}, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' } : {}),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON（如 CSV 导出） */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: { username: user.u, password: user.p, tenantName: user.t, captcha: code, captchaKey: cap.json.data.uuid },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

;(async () => {
  console.log(`验证目标: ${BASE}`)
  const admin = await login(ADMIN)
  console.log(`登录成功: ${ADMIN.u}（超管 / 租户 1）`)

  let createdId = null
  try {
    // ══ ① 列表接口可达（原来 404）══
    section('① GET /erp/purchase/contract/page')
    const page0 = await req('GET', '/erp/purchase/contract/page?current=1&size=10', { token: admin })
    ok('接口不再 404，返回 200', page0.status === 200, `status=${page0.status} ${page0.json?.message || ''}`)
    // ⚠️ 本仓全局把 Long 序列化成**字符串**（防 JS BigInt 精度丢失，见 js-bigint-precision-fix），
    //    所以 total/size/current 是 "0"/"10" 这样的字符串，不是 number —— 断言要按可数值化判断。
    ok('返回分页结构 data.records / data.total',
      Array.isArray(page0.json?.data?.records)
      && Number.isFinite(Number(page0.json?.data?.total)),
      `records=${Array.isArray(page0.json?.data?.records)}, total=${page0.json?.data?.total}`)

    // ══ ② 统计接口（原来 totalCount/totalAmount 字段缺失）══
    section('② GET /erp/purchase/contract/statistics')
    const stats = await req('GET', '/erp/purchase/contract/statistics', { token: admin })
    ok('接口 200', stats.status === 200, `status=${stats.status}`)
    ok('统计含 totalCount / totalAmount（前端卡片所需）',
      stats.json?.data?.totalCount !== undefined && stats.json?.data?.totalAmount !== undefined,
      `totalCount=${stats.json?.data?.totalCount}, totalAmount=${stats.json?.data?.totalAmount}`)

    // ══ ③ 新增 ══
    section('③ POST /erp/purchase/contract（新增草稿）')
    const contractNo = `E2E-PC-${Date.now()}`
    const created = await req('POST', '/erp/purchase/contract', {
      token: admin,
      body: {
        contractNo,
        supplierId: 1,
        supplierName: 'E2E 验证供应商',
        contractTitle: 'E2E 采购合同验证',
        contractType: 'FRAMEWORK',
        totalAmount: 12345.67,
        remark: 'verify-purchase-contract.cjs 自动创建，脚本结束即删',
      },
    })
    createdId = created.json?.data?.id ?? null
    ok('创建返回 200', created.status === 200, `status=${created.status} ${created.json?.message || ''}`)
    ok('创建返回了新合同 ID', createdId != null, `id=${createdId}`)
    ok('新建状态被强制为草稿（不接受前端直传 ACTIVE）',
      created.json?.data?.contractStatus === 'DRAFT', `status=${created.json?.data?.contractStatus}`)

    // ══ ④ 租户口径关键断言：建了必须能查到 ══
    section('④ 回查（租户口径：tenant_id 未落成 0 才会可见）')
    const detail = await req('GET', `/erp/purchase/contract/${createdId}`, { token: admin })
    ok('按 ID 能查到刚建的合同', detail.status === 200 && detail.json?.data?.id === createdId,
      `status=${detail.status}`)

    const listed = await req('GET',
      `/erp/purchase/contract/page?current=1&size=10&contractNo=${encodeURIComponent(contractNo)}`,
      { token: admin })
    const rows = listed.json?.data?.records || []
    ok('列表按合同号能命中（若 tenant_id 落成 0 则此处查不到）',
      rows.some(r => r.id === createdId), `命中 ${rows.length} 行`)

    // ══ ⑤ 编辑 ══
    section('⑤ PUT /erp/purchase/contract/{id}（编辑）')
    const updated = await req('PUT', `/erp/purchase/contract/${createdId}`, {
      token: admin,
      body: {
        contractTitle: 'E2E 采购合同验证（已改）',
        supplierId: 1,
        supplierName: 'E2E 验证供应商（改名）',
        contractType: 'FRAMEWORK',
        totalAmount: 999.99,
        warrantyPeriod: '24个月',
        remark: '编辑后回查',
      },
    })
    ok('编辑返回 200', updated.status === 200, `status=${updated.status} ${updated.json?.message || ''}`)

    const after = await req('GET', `/erp/purchase/contract/${createdId}`, { token: admin })
    ok('标题改动已落库',
      after.json?.data?.contractTitle === 'E2E 采购合同验证（已改）', `title=${after.json?.data?.contractTitle}`)
    ok('供应商名称改动已落库（原 update 语句漏写该列，会静默丢改动）',
      after.json?.data?.supplierName === 'E2E 验证供应商（改名）', `supplierName=${after.json?.data?.supplierName}`)
    ok('质保期改动已落库（原 update 语句漏写该列）',
      after.json?.data?.warrantyPeriod === '24个月', `warrantyPeriod=${after.json?.data?.warrantyPeriod}`)

    // ══ ⑥ 导出 ══
    section('⑥ GET /erp/purchase/contract/export')
    const exp = await req('GET', '/erp/purchase/contract/export', { token: admin })
    ok('导出返回 200 且是 CSV 文本', exp.status === 200 && /合同编号/.test(exp.text),
      `status=${exp.status}, 长度=${exp.text.length}`)

    // ══ ⑦ 鉴权 ══
    section('⑦ 鉴权：非超管无 purchase:contract:* 码')
    const t2 = await login(TENANT2)
    const t2page = await req('GET', '/erp/purchase/contract/page?current=1&size=10',
      { token: t2, headers: { 'X-Tenant-Id': '2' } })
    ok('【拒绝路径】非超管调 /page → 403', t2page.status === 403,
      `status=${t2page.status} ${t2page.json?.message || ''}`)
  } finally {
    // ══ ⑧ 清理：删掉脚本自己建的那条 ══
    section('⑧ 清理测试数据')
    if (createdId != null) {
      const del = await req('DELETE', `/erp/purchase/contract/${createdId}`, { token: admin })
      ok('删除草稿合同 → 200', del.status === 200, `status=${del.status} ${del.json?.message || ''}`)
      const gone = await req('GET', `/erp/purchase/contract/${createdId}`, { token: admin })
      ok('删除后详情查不到（data 为 null）', gone.json?.data == null, `data=${JSON.stringify(gone.json?.data)}`)
    } else {
      console.log('  （未成功创建，无需清理）')
    }
  }

  console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
  process.exit(fail === 0 ? 0 : 1)
})().catch(e => {
  console.error('\n验证脚本异常:', e.message)
  process.exit(2)
})
