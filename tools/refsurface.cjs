#!/usr/bin/env node
/**
 * 引用面判定（阶段 3 施工方案的**第 0 步**，见 `docs/PHASE-3-5-MIGRATION-PLAN-v1.md` §3.2）
 *
 * 【本脚本回答一个问题】库里那 100 张表的 `customer_id` / `supplier_id` / `partner_id` /
 *   `party_id` / `contact_id` / `*_party_id` …… **到底哪一列的哪一些，真的存的是
 *   `biz_party.id`**？
 *
 * 【为什么必须先回答】本仓**几乎没有外键**，列名也不统一：
 *   · 按列名猜引用关系 ⇒ 猜错就改错数据，而阶段 3/5 的改动**不可逆**；
 *   · `partner_id` 有 25 张表在用，但它**可能指向别的表**；
 *   ⇒ 方案把"引用面判定"排在**建表之前**。
 *
 * 【本脚本只给"数据证据"这一条，不是全部】完整的判定是三证并立（方案 §3.2）：
 *   ① **数据证据**（本脚本：命中率）② **代码证据**（谁写、谁读，需 grep 到文件行号）
 *   ③ **注释/文档证据**。CSV 后两列刻意留空由人填 —— **不让脚本替人下结论**。
 *
 * 【判定口径】对每个候选列，取它的**非空值**，看有多少能命中候选主档：
 *   · 命中率 100% 且非空行数 > 0 ⇒ `指向 <表>`（数据侧成立）
 *   · 命中率低但非空很少 ⇒ `样本太少`（不足以判定，别据此改代码）
 *   · 命中 `sys_user` 比 `biz_party` 高 ⇒ 那它多半是**账号 id**，不是往来单位
 *   · 全空 ⇒ `全空`（无从判定；本仓 `biz_party.unified_code` 就是这样）
 *
 * ⚠️ 与 `audit-snapshot-spec.cjs` 同源的两个坑（都已在本脚本里规避）：
 *   · `tools/sql.cjs` 有 **500 行硬上限且静默截断** ⇒ 查询必须分块，命中上限直接抛错；
 *   · 逐列查要跑上百次 ⇒ **拼 UNION ALL 分块**，一次拿一批。
 *
 * 用法：node tools/refsurface.cjs            # 打印结论并写 tools/refsurface.csv
 *   前置：本地 dev 库可连（走 tools/sql.cjs）。
 *   退出码：0 = 完成；2 = 脚本自身失败（连不上库 / 结果被截断）。
 */

const { execFileSync } = require('child_process')
const fs = require('fs')
const path = require('path')

const REPO = path.resolve(__dirname, '..')
const OUT = path.join(REPO, 'tools', 'refsurface.csv')

/** 参与判定的候选列（阶段 3/5 会碰到的"疑似主体引用列"）。 */
const CANDIDATE_COLUMNS = [
  'customer_id', 'supplier_id', 'partner_id', 'party_id', 'contact_id',
  'member_id', 'member_party_id', 'seller_party_id', 'buyer_party_id',
  'acting_party_id', 'accepted_party_id', 'target_party_id',
]

/** 候选主档（命中率用来判断"这列指向谁"）。按优先级排列，先命中先算。 */
const MASTERS = ['biz_party', 'sys_user', 'biz_party_contact']

/** 每批拼多少个 UNION 分支（避免单条 SQL 过肥）。 */
const CHUNK = 25

class Fail extends Error {}

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new Fail(`SQL 失败: ${stmt.slice(0, 120)} —— ${first}`)
  }
}

function rows(stmt) {
  const text = sql(stmt).replace(/\r/g, '')
  const trailer = text.match(/\((\d+) rows?\)/)
  if (trailer && Number(trailer[1]) >= 500) {
    throw new Fail(`查询命中 tools/sql.cjs 的 500 行硬上限 ⇒ 结果不可信，请把批次调小：${stmt.slice(0, 100)}`)
  }
  const lines = text.split('\n').map(l => l.trimEnd())
    .filter(l => l.trim() && !/^-+(\+-+)*$/.test(l.trim()) && !/^\(\d+ rows?\)$/.test(l.trim()))
  if (lines.length === 0) return []
  const headers = lines[0].split('|').map(s => s.trim())
  return lines.slice(1).map(l => {
    const cells = l.split('|').map(s => s.trim())
    const o = {}
    headers.forEach((h, i) => { o[h] = cells[i] })
    return o
  })
}

const esc = (s) => s.replace(/'/g, "''")
const chunk = (arr, n) => arr.reduce((a, x, i) => (i % n ? a[a.length - 1].push(x) : a.push([x]), a), [])

/** 候选列的宿主清单 */
function candidateColumns() {
  const list = CANDIDATE_COLUMNS.map(c => `'${c}'`).join(',')
  return rows(`SELECT c.table_name AS t, c.column_name AS col, c.data_type AS typ
      FROM information_schema.columns c
      JOIN information_schema.tables tb
        ON tb.table_schema = c.table_schema AND tb.table_name = c.table_name
     WHERE c.table_schema = 'public' AND tb.table_type = 'BASE TABLE'
       AND c.column_name IN (${list})
     ORDER BY c.column_name, c.table_name`).map(r => ({ t: r.t, col: r.col, typ: r.typ }))
}

/** 一批候选列的命中统计（一条 UNION ALL 查询拿回来） */
function probe(batch) {
  const branches = batch.map(({ t, col }) => {
    const hits = MASTERS.map(m =>
      `count(m_${m}.id) AS ${m}`).join(', ')
    // ⚠️ 一律按**文本**比较：本仓 id 在 Java 侧一律是 String，落到库里有的列是 bigint、
    //    有的列却是 varchar（如 `erp_pre_receipt.customer_id`）⇒ 直接相等会报
    //    「操作符不存在: bigint = character varying」。文本比较对两种形态都成立。
    const joins = MASTERS.map(m =>
      `LEFT JOIN ${m} m_${m} ON m_${m}.id::text = x.${col}::text`).join('\n       ')
    return `SELECT '${esc(t)}' AS t, '${esc(col)}' AS col, count(*) FILTER (WHERE x.${col} IS NOT NULL) AS nonnull,
       ${hits}
  FROM ${t} x
       ${joins}`.replace(/\n\s+/g, ' ')
  }).join('\n  UNION ALL\n  ')
  return rows(branches)
}

;(() => {
  console.log('引用面判定（阶段 3 第 0 步 · 只出清单，不改任何东西）')

  let cols
  try {
    cols = candidateColumns()
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }
  console.log(`  候选列共 ${cols.length} 处（表×列）`)

  const out = []
  try {
    for (const batch of chunk(cols, CHUNK)) {
      const typOf = new Map(batch.map(c => [c.t + '|' + c.col, c.typ]))
      for (const r of probe(batch)) {
        const typ = typOf.get(r.t + '|' + r.col) || ''
        const nonnull = Number(r.nonnull)
        const hit = {}
        for (const m of MASTERS) hit[m] = Number(r[m] || 0)

        // 判定：**只表述数据事实**
        //
        // ⚠️ 这里有个必须说清的局限（首轮实测踩到）：dev 库里大量主键是**小整数**
        // （如 1~35），同一个值会**同时**命中 `biz_party.id` 与 `sys_user.id` ——
        // 例如 `dms_task.customer_id` 35 个非空值**两边都 100% 命中**。
        // 此时**数据证据本身就无法区分**它指向谁，只能靠代码证据（三证的第二证）。
        // ⇒ 绝不能因为"先命中了 biz_party"就宣布指向它（第一版就是这么写的，已修正）。
        const fullHits = MASTERS.filter(m => nonnull > 0 && hit[m] === nonnull)
        let verdict
        if (nonnull === 0) {
          verdict = '全空（无从判定）'
        } else if (fullHits.length === 1) {
          verdict = `指向 ${fullHits[0]}`
        } else if (fullHits.length > 1) {
          verdict = `同时命中 ${fullHits.join('/')}（数据侧不可区分，必须看代码证据）`
        } else if (Math.max(...MASTERS.map(m => hit[m])) === 0) {
          verdict = '不命中任何候选主档（指向别处/脏数据）'
        } else {
          verdict = '部分命中（需人工判定）'
        }
        out.push({
          t: r.t, col: r.col, nonnull, ...hit, verdict, typ,
          codeEvidence: '', docEvidence: '',
        })
      }
    }
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }

  // 报告：按判定分组打印（人看结论，csv 给后续填两列证据）
  const byVerdict = new Map()
  for (const r of out) {
    if (!byVerdict.has(r.verdict)) byVerdict.set(r.verdict, [])
    byVerdict.get(r.verdict).push(r)
  }
  for (const [v, list] of [...byVerdict.entries()].sort((a, b) => b[1].length - a[1].length)) {
    console.log(`\n【${v}】${list.length} 处`)
    for (const r of list.slice(0, 12)) {
      console.log(`  ${r.t}.${r.col}(${r.typ})  非空=${r.nonnull} 命中 biz_party=${r.biz_party}`
        + ` sys_user=${r.sys_user} contact=${r.biz_party_contact}`)
    }
    if (list.length > 12) console.log(`  …（其余 ${list.length - 12} 处见 CSV）`)
  }

  const header = 'table,column,column_type,nonnull,hit_biz_party,hit_sys_user,hit_biz_party_contact,verdict,code_evidence,doc_evidence'
  const csv = [header, ...out.map(r =>
    [r.t, r.col, r.typ, r.nonnull, r.biz_party, r.sys_user, r.biz_party_contact, r.verdict, r.codeEvidence, r.docEvidence]
      .join(','))].join('\n')
  fs.writeFileSync(OUT, csv + '\n', 'utf8')

  const hitBiz = out.filter(r => r.verdict === '指向 biz_party').length
  console.log(`\n===== 小计：候选 ${out.length} 处，其中数据侧确认指向 biz_party 的 ${hitBiz} 处 =====`)
  console.log(`CSV 已写：${path.relative(REPO, OUT)}（code_evidence / doc_evidence 两列**刻意留空**，`
    + '由人按方案 §3.2 的三证并立去填 —— 数据侧成立 ≠ 引用关系成立）')
  process.exit(0)
})()
