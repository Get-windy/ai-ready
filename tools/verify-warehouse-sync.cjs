/**
 * D7 仓库主数据同步验收（2026-09-23 新增的 WarehouseChangedEvent + WarehouseSyncListener）
 *
 * 验证闭环：
 *   1. 在「仓库规划」侧新建仓库（POST /api/erp/warehouse/save）
 *      → 断言 wms_warehouse 自动出现对应扩展行（id 与 warehouse_id 都等于 erp 侧 id）
 *   2. 改名（POST /api/erp/warehouse/update）→ 断言名称同步
 *   3. 删除（DELETE /api/erp/warehouse/{id}）→ 断言 WMS 扩展行一并消失
 *   收尾：无论断言成败都会清理自造数据。
 *
 * 背景：全站业务单据的仓库下拉取自 GET /wms/warehouse/list-all（读 wms_warehouse），
 *       而新建仓库的入口在仓库规划页（写 erp_warehouse）——此前无同步机制。
 *
 * 用法: node tools/verify-warehouse-sync.cjs
 */
const http = require('http')
const { execSync } = require('child_process')

const BASE = { host: '127.0.0.1', port: Number(process.env.API_PORT || 5655) }
const USER = process.env.E2E_USER || 'admin'
const PWD = process.env.E2E_PWD || 'admin123'
const TENANT = process.env.E2E_TENANT || '系统租户'
const CODE = 'E2E-SYNC-' + Date.now().toString().slice(-6)

function req(method, path, body, token) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null
    const r = http.request({
      ...BASE, method, path,
      headers: {
        'Content-Type': 'application/json',
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    }, res => {
      let b = ''
      res.on('data', c => b += c)
      res.on('end', () => {
        let json = null
        try { json = JSON.parse(b) } catch { /* ignore */ }
        resolve({ status: res.statusCode, json, raw: b.slice(0, 200) })
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

/** 直查 devdb（避免为验收脚本引入额外依赖）。
 *  注意：Windows 下 execSync 走 cmd，不能用 `VAR=1 cmd` 前缀语法 —— 环境变量必须经 env 传入。 */
function py(cmd) {
  return execSync(cmd, {
    cwd: process.cwd(),
    encoding: 'utf8',
    env: { ...process.env, PYTHONIOENCODING: 'utf-8' },
  })
}

function sql(q) {
  return JSON.parse(py(`python tools/dbq.py "${q.replace(/"/g, '\\"')}"`))
}

async function login() {
  const cap = await req('GET', '/api/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/api/auth/login', {
    username: USER, password: PWD, tenantName: TENANT, captcha: code, captchaKey: cap.json.data.uuid,
  })
  const t = res.json?.data?.token || res.json?.data?.accessToken
  if (!t) throw new Error('登录失败: ' + JSON.stringify(res.json).slice(0, 200))
  return t
}

const results = []
function check(name, ok, detail) {
  results.push(ok)
  console.log(`${ok ? '  PASS' : '  FAIL'}  ${name}    ${detail}`)
}

async function main() {
  console.log(`\n== D7 仓库同步验收（测试编号 ${CODE}）==\n`)
  const token = await login()
  let erpId = null

  try {
    // 1) 新建
    const created = await req('POST', '/api/erp/warehouse/save',
      { warehouseName: 'E2E同步验证仓', warehouseCode: CODE }, token)
    erpId = created.json?.data?.id ?? created.json?.id
    check('① 新建仓库成功', created.status === 200 && !!erpId, `HTTP ${created.status} id=${erpId}`)

    const w1 = sql(`SELECT id,warehouse_id,warehouse_code,warehouse_name,is_wms_enabled FROM wms_warehouse WHERE warehouse_code='${CODE}'`)
    check('② WMS 扩展行已自动补建', w1.length === 1, JSON.stringify(w1[0] || {}))
    if (w1.length === 1) {
      check('③ id 与 warehouse_id 对齐 erp 侧', String(w1[0].id) === String(erpId) && String(w1[0].warehouse_id) === String(erpId),
        `wms.id=${w1[0].id}, wms.warehouse_id=${w1[0].warehouse_id}, erp.id=${erpId}`)
    }

    // 2) 改名
    await req('POST', '/api/erp/warehouse/update', { id: erpId, warehouseName: 'E2E同步验证仓-改名' }, token)
    const w2 = sql(`SELECT warehouse_name FROM wms_warehouse WHERE warehouse_code='${CODE}'`)
    check('④ 改名已同步', w2.length === 1 && w2[0].warehouse_name === 'E2E同步验证仓-改名', JSON.stringify(w2[0] || {}))

    // 3) 删除
    const del = await req('DELETE', `/api/erp/warehouse/${erpId}`, null, token)
    check('⑤ 删除仓库成功', del.status === 200, `HTTP ${del.status}`)
    const w3 = sql(`SELECT id FROM wms_warehouse WHERE warehouse_code='${CODE}' AND deleted=0`)
    check('⑥ WMS 扩展行已一并删除', w3.length === 0, `剩余 ${w3.length} 行`)
    erpId = null
  } finally {
    // 收尾清理（断言中途失败也要清干净）
    if (erpId) {
      await req('DELETE', `/api/erp/warehouse/${erpId}`, null, token).catch(() => {})
    }
    try {
      py(`python tools/dbq.py "DELETE FROM wms_warehouse WHERE warehouse_code='${CODE}'"`)
      py(`python tools/dbq.py "DELETE FROM erp_warehouse WHERE warehouse_code='${CODE}'"`)
      console.log('\n  (已清理测试数据)')
    } catch (e) { console.log('\n  (清理警告: ' + e.message.slice(0, 80) + ')') }
  }

  const failed = results.filter(x => !x).length
  console.log(`\n== 合计 ${results.length} 项，通过 ${results.length - failed}，失败 ${failed} ==\n`)
  process.exit(failed ? 1 : 0)
}

main().catch(e => { console.error('脚本异常:', e.message); process.exit(2) })
