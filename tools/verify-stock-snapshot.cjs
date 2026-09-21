#!/usr/bin/env node
/**
 * STK-BREAK-04 验证：erp_stock 展示快照列不再为空（2026-09-21）
 *
 * 要防的是这条链路上真实存在的问题：`erp_stock` 有一组冗余展示列
 * （product_name / product_code / unit / warehouse_name），但写入口口径不一致 ——
 * `recordStockIn`、`saveInitialStock` 会带上它们，而 `increaseStock`、`checkStock`
 * 只收 id（调用方如 wms 的 ERP 轨镜像、盘点调整**无从传入**）⇒ 新建的行天然空快照；
 * 而期初库存列表的「仓库」列是**直接取快照、没有 COALESCE 回退**的。
 *
 * 三向断言（缺一条都可能是假绿）：
 *   ① 存量回填：迁移 V11.449.0 之后，所有行的 product_name / warehouse_name 非空；
 *   ② 新建补齐：走 increaseStock 建出的**新行**快照有值（证明确实改在写入口上，
 *      而不是只靠一条 UPDATE 迁移糊过去）；
 *   ③ 自愈：把某行快照人为清空后再触发一次写路径，值应被补回（证明不是一次性的）。
 *
 * 用法：node tools/verify-stock-snapshot.cjs
 * 副作用：会创建 1 行 (测试商品 × 华东分仓) 的库存并在 finally 里硬删除。
 */
const { execFileSync } = require('child_process')
const path = require('path')

const PORT = process.env.PORT || '5655'
const BASE = `http://localhost:${PORT}/api`
const REPO = path.resolve(__dirname, '..')
const ADMIN = { u: 'admin', p: 'admin123', t: '系统租户' }

// 测试用「商品 × 仓库」：选一对**当前不存在库存行**的组合，保证 increase 走的是"新建"分支
const TEST_PRODUCT_ID = '990000000000000001'
const TEST_WAREHOUSE_ID = '2'
const EXPECT_NAME = '博多家园百香果果酱'
const EXPECT_CODE = 'SP-TEST-001'
const EXPECT_UNIT = '瓶'
const EXPECT_WAREHOUSE = '华东分仓'

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}
const section = (t) => console.log(`\n${t}`)

function sql(stmt) {
  return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt], {
    cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024,
  })
}

/** 解析 sql.cjs 的表格输出（表头 / 分隔线 / 数据行 / 末尾 `(N rows)`） */
function rowsOf(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').trim().split('\n')
    .filter(l => l.trim() && !/^\(\d+ rows\)$/.test(l.trim()) && !/^-+$/.test(l.trim()))
  return lines.slice(1).map(l => l.split('|').map(s => s.trim()))
}

async function req(method, p, { token, body } = {}) {
  const res = await fetch(BASE + p, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch { /* 非 JSON */ }
  return { status: res.status, json, text }
}

async function login(user) {
  const cap = await req('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.json.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await req('POST', '/auth/login', {
    body: {
      username: user.u, password: user.p, tenantName: user.t,
      captcha: code, captchaKey: cap.json.data.uuid,
    },
  })
  const token = res.json?.data?.token || res.json?.data?.accessToken
  if (!token) throw new Error(`${user.u} 登录失败: ${res.text.slice(0, 300)}`)
  return token
}

const TEST_WHERE = `product_id = ${TEST_PRODUCT_ID} AND warehouse_id = ${TEST_WAREHOUSE_ID} AND tenant_id = 1`
const cleanup = () => { try { sql(`DELETE FROM erp_stock WHERE ${TEST_WHERE}`) } catch (e) { console.log('  [清理失败] ' + e.message) } }

;(async () => {
  console.log(`验证目标: ${BASE}`)
  try {
    cleanup() // 先清上一次可能的残留，保证 increase 一定走"新建"

    const token = await login(ADMIN)
    console.log(`登录成功: ${ADMIN.u}`)

    // ══════════ ① 存量回填（迁移 V11.449.0）══════════
    section('① 存量回填 —— 迁移后所有行的展示列都不为空')
    const nullRows = rowsOf(`SELECT count(*) FROM erp_stock WHERE deleted = 0 `
      + `AND (product_name IS NULL OR product_name = '' OR warehouse_name IS NULL OR warehouse_name = '')`)
    ok('没有「商品名或仓库名为空」的库存行', Number(nullRows[0][0]) === 0, `空行数=${nullRows[0][0]}`)

    const total = rowsOf(`SELECT count(*) FROM erp_stock WHERE deleted = 0`)
    ok('前置：库里确实有库存行（不是靠"没有数据"蒙对的）', Number(total[0][0]) > 0, `行数=${total[0][0]}`)

    // ══════════ ② 新建行补齐（走真实写路径）══════════
    section('② 新建补齐 —— 只传 id 的 increaseStock 也要写全快照')
    const inc = await req('POST', `/erp/stock/increase?productId=${TEST_PRODUCT_ID}`
      + `&warehouseId=${TEST_WAREHOUSE_ID}&quantity=7`, { token })
    ok('increaseStock 返回成功', inc.status === 200 && inc.json?.data === true,
      `status=${inc.status} data=${JSON.stringify(inc.json?.data)}`)

    const created = rowsOf(`SELECT product_name, product_code, unit, warehouse_name, quantity FROM erp_stock WHERE ${TEST_WHERE}`)
    ok('新行已建出且只有 1 行', created.length === 1, JSON.stringify(created))
    if (created.length === 1) {
      const [name, code, unit, whName, qty] = created[0]
      ok('product_name 由档案补齐', name === EXPECT_NAME, `实际=${name}`)
      ok('product_code 由档案补齐', code === EXPECT_CODE, `实际=${code}`)
      ok('unit 由档案补齐', unit === EXPECT_UNIT, `实际=${unit}`)
      ok('warehouse_name 由档案补齐', whName === EXPECT_WAREHOUSE, `实际=${whName}`)
      ok('数量写入正确', Number(qty) === 7, `实际=${qty}`)
    }

    // ══════════ ③ 自愈：清空后再写一次应补回 ══════════
    section('③ 自愈 —— 历史空快照行被任何写路径碰过即补齐')
    sql(`UPDATE erp_stock SET product_name = NULL, product_code = NULL, warehouse_name = NULL WHERE ${TEST_WHERE}`)
    const cleared = rowsOf(`SELECT product_name FROM erp_stock WHERE ${TEST_WHERE}`)
    ok('前置：已人为把快照清空', cleared[0][0] === 'null', `实际=${cleared[0][0]}`)

    await req('POST', `/erp/stock/increase?productId=${TEST_PRODUCT_ID}`
      + `&warehouseId=${TEST_WAREHOUSE_ID}&quantity=1`, { token })
    const healed = rowsOf(`SELECT product_name, warehouse_name, quantity FROM erp_stock WHERE ${TEST_WHERE}`)
    ok('再次写入后 product_name 被补回', healed[0][0] === EXPECT_NAME, `实际=${healed[0][0]}`)
    ok('再次写入后 warehouse_name 被补回', healed[0][1] === EXPECT_WAREHOUSE, `实际=${healed[0][1]}`)
    ok('数量累加为 8（未被补快照的动作改坏）', Number(healed[0][2]) === 8, `实际=${healed[0][2]}`)

  } catch (e) {
    fail++
    console.error(`\n!! 异常中止: ${e.message}`)
  } finally {
    section('清理')
    cleanup()
    const left = rowsOf(`SELECT count(*) FROM erp_stock WHERE ${TEST_WHERE}`)
    console.log(`  [清理] 测试行已删除（剩余 ${left[0][0]} 行）`)
  }

  console.log(`\n结果: PASS=${pass} FAIL=${fail}`)
  if (fail > 0) process.exitCode = 1
})()
