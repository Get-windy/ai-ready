#!/usr/bin/env node
/**
 * `party` / `party_tenant` ← `biz_party` **对账与补齐**（阶段 3 · 双写落地前的过渡工具）
 *
 * 【它解决什么】阶段 3 的 `party`/`party_tenant` 已建并回填（V11.497.0），但**双写还没做** ——
 *   于是 `biz_party` 里**新**建的往来单位不会进新表，两边**必然越落越远**
 *   （实测 2026-09-26：未删 21 行 vs `party` 19 行，差额 2 行）。
 *
 * 【它的定位】**过渡工具，不是终态**。终态是"写路径双写"（那样两边不会再漂）。
 *   在此之前：① 它能随时把差额补平；② 它输出的差额本身就是**漂移监控**
 *   —— 差额持续增长，就是在提醒"双写还没落地"。
 *
 * 【口径（与 V11.497.0 的建表回填完全一致，别在这里另立一套）】
 *   · `party.id` **复用 `biz_party.id`**；`party.tenant_id` 恒 0（共享层，§6.5）；
 *   · `party_tenant` 一行 = 一条有向边：`roles` 含 CUSTOMER ⇒ SALE、含 SUPPLIER ⇒ PURCHASE；
 *     `roles` 为空时按 `party_type`（2 ⇒ PURCHASE）；
 *   · **方向判不出来的一律不建边**（如 `party_type=3` 第三方服务主体），并在报告里列出来 ——
 *     **不猜**（与迁移自检里那条"承运商无贸易边"同源）。
 *
 * 【幂等】已存在的不重插、不改值（除非显式 `--refresh`）；只做"补缺"。
 *
 * 用法：
 *   node tools/sync-party-from-biz-party.cjs            # 只看差额 + 补齐（默认执行）
 *   node tools/sync-party-from-biz-party.cjs --dry-run  # 只看，不写
 *   node tools/sync-party-from-biz-party.cjs --refresh  # 额外用 biz_party 刷新 party 的 A 组 8 列
 *   前置：本地 dev 库可连（走 tools/sql.cjs）。
 *   退出码：0 = 成功；2 = 脚本自身失败。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

/** A 组里已落到 `party` 的 8 列（与 V11.506.0 一致） */
const REFRESH_COLS = ['company_full_name', 'mnemonic_code', 'phone', 'email', 'website', 'fax',
  'legal_person_phone', 'remark']

class Fail extends Error {}

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new Fail(`SQL 失败: ${stmt.slice(0, 140)} —— ${first}`)
  }
}

function scalar(stmt) {
  const lines = sql(stmt).replace(/\r/g, '').split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}
const num = (s) => Number(scalar(s))

/** 漂移快照（三个数：业务表未删 / 新主档 / 差额） */
function gap() {
  return {
    业务表未删: num(`SELECT count(*) FROM biz_party WHERE deleted = 0`),
    新主档: num(`SELECT count(*) FROM party WHERE deleted = 0`),
    未进新表: num(`SELECT count(*) FROM biz_party b WHERE b.deleted = 0
                    AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = b.id)`),
    未建边的: num(`SELECT count(*) FROM party p WHERE p.deleted = 0
                    AND NOT EXISTS (SELECT 1 FROM party_tenant t WHERE t.party_id = p.id AND t.deleted = 0)`),
  }
}

;(() => {
  const args = process.argv.slice(2)
  const dry = args.includes('--dry-run')
  const refresh = args.includes('--refresh')
  console.log(`party/party_tenant ⇄ biz_party 对账${dry ? '（--dry-run：只看不写）' : ''}`)

  try {
    const before = gap()
    console.log(`  补齐前：${JSON.stringify(before)}`)

    if (before.未进新表 === 0 && before.未建边的 === 0 && !refresh) {
      console.log('  ✅ 两边一致（差额 0），无需补齐')
      process.exit(0)
    }

    // 方向判不出来的（不建边），单独列出来 —— 这是"不猜"的证据
    const undecidable = sql(`SELECT b.id, b.party_name, b.party_type, b.roles FROM biz_party b
      WHERE b.deleted = 0
        -- ⚠️ 必须显式处理 NULL：roles 为 NULL 时 NOT LIKE 返回 NULL 而不是 TRUE
        --    ⇒ 只用 NOT LIKE 会把 roles 为空的行漏掉（本脚本第一版就是，
        --    症状是"未建边 1 行 vs 方向判不出 0 行"自相矛盾）
        --    （注意：本段是 JS 模板字符串，SQL 注释里不要写反引号）
        AND (b.roles IS NULL OR (b.roles NOT LIKE '%CUSTOMER%' AND b.roles NOT LIKE '%SUPPLIER%'))
        AND (b.party_type IS NULL OR b.party_type <> 2)
      ORDER BY b.id`).replace(/\r/g, '').split('\n').slice(2)
      .map(l => l.trim()).filter(l => l && !/^\(\d+ rows?\)$/.test(l))
    console.log(`  方向判不出、将**不建边**的：${undecidable.length} 行`
      + (undecidable.length ? ' ⇒ ' + undecidable.join(' | ') : ''))

    if (dry) {
      console.log('  （--dry-run）未写库。')
      process.exit(0)
    }

    // ① 补 party（id 复用；tenant_id 恒 0；已存在的不动）
    sql(`INSERT INTO party (id, tenant_id, party_code, unified_code, party_name, short_name,
                            party_type, legal_person, status, phone, email, remark,
                            create_time, update_time, deleted)
         SELECT b.id, 0, b.party_code, b.unified_code, b.party_name, b.short_name,
                b.party_type, b.legal_person, COALESCE(b.status, 1), b.phone, b.email, b.remark,
                now(), now(), 0
         FROM biz_party b
         WHERE b.deleted = 0 AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = b.id)`)

    // ② 补 party_tenant 边（方向口径与建表回填逐字一致）
    sql(`INSERT INTO party_tenant (id, tenant_id, party_id, direction, party_level,
                                   create_time, update_time, deleted)
         SELECT b.id, b.tenant_id, b.id,
                CASE WHEN b.roles LIKE '%CUSTOMER%' THEN 'SALE' ELSE 'PURCHASE' END,
                b.party_level, now(), now(), 0
         FROM biz_party b
         WHERE b.deleted = 0
           AND (b.roles LIKE '%CUSTOMER%' OR b.roles LIKE '%SUPPLIER%' OR b.party_type = 2)
           AND NOT EXISTS (SELECT 1 FROM party_tenant t WHERE t.party_id = b.id AND t.deleted = 0)`)

    // ③ 可选：用 biz_party 刷新 A 组 8 列（默认不做 —— 不覆盖已有值）
    if (refresh) {
      sql(`UPDATE party p SET
             ${REFRESH_COLS.map(c => `${c} = b.${c}`).join(', ')},
             update_time = now()
           FROM biz_party b WHERE b.id = p.id AND b.deleted = 0`)
      console.log(`  已用 biz_party 刷新 party 的 ${REFRESH_COLS.length} 列`)
    }

    const after = gap()
    console.log(`  补齐后：${JSON.stringify(after)}`)
    if (after.未进新表 !== 0) {
      console.error('  ❌ 仍有未进新表的主体，请查看上面的 SQL 输出')
      process.exit(1)
    }
    // 未建边的**允许 >0**：那就是"方向判不出来"的那些，属正常
    console.log(`  ✅ 主档已对齐；仍未建边 ${after.未建边的} 行`
      + `（应等于上面"方向判不出"的行数 ${undecidable.length}）`)
    if (after.未建边的 !== undecidable.length) {
      console.error('  ⚠️ 未建边数与"方向判不出"的行数不一致，请人工看一眼')
      process.exit(1)
    }
    process.exit(0)
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }
})()
