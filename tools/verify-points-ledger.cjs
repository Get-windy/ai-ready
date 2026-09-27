#!/usr/bin/env node
/**
 * 积分台账对账：**"余额绝不落主档"这条口径落地后的硬控制**。
 *
 * 【为什么必须有这个脚本】换成"只追加台账 + 派生余额"之后，业界那条纪律是
 *   **"派生余额必须随时能被台账求和证明"** —— 否则你只是把一个没有一致性保证的字段，
 *   换成了一张没有一致性保证的表。所以每次动积分相关的代码，都要能一键回答：
 *   ① 期初有没有漏搬/搬错？② 账实是否相符？③ 有多少积分**根本兑不了**（有分没卡）？
 *
 * 【三类检查，性质不同（别混着读）】
 *   ✅ **断言**（不一致就 exit 1）：
 *      · 期初对平：`biz_party.points <> 0` 的每个主体，恰好有一条 `source='OPENING'` 批次，
 *        且**金额逐主体相等**（比总量强：总量相等也可能"张三的搬到李四头上"）。
 *      · 期初批次都有配套 `OPENING` 流水（没有流水＝审计链断了）。
 *   📋 **报告**（只打印，不判失败）：
 *      · 台账合计 vs `biz_party.points` 合计 —— ⚠️ 两者**本来就允许不等**：
 *        `biz_party.points` 已停写（冻结在最后一刻），而台账会随零售继续变。
 *        相等只说明"自停写以来没有新活动"；不等**不是错误**，是"新活动已经发生"的证据。
 *      · **有积分但没卡**的主体清单 —— 它们进得了台账、**走不了卡路径兑付**
 *        （台账的 `earn/use/available` 是卡号版 API）⇒ 要么补发卡，要么明确"这些分不兑"。
 *
 * 用法：node tools/verify-points-ledger.cjs
 *   前置：`V11.522.0` / `V11.523.0` 已由 Flyway 应用。
 *   退出码：0 = 断言全过；1 = 有断言失败；2 = 脚本自身失败。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

function sql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 8 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new Error(`SQL 失败: ${stmt.slice(0, 120)} —— ${first}`)
  }
}
const clean = (s) => s.replace(/\r/g, '')
/** 取单元格（⚠️ 不能取最后一行：末尾还有 `(N rows)` 统计行） */
function scalar(stmt) {
  const lines = clean(sql(stmt)).split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
  return lines.length > 1 ? lines[1] : '(空)'
}
const num = (s) => Number(scalar(s))
/** 取多行（只取第 3 行起的数据行） */
function rows(stmt) {
  return clean(sql(stmt)).split('\n').map(l => l.trim())
    .filter(l => l && !/^-+(\+-+)*$/.test(l) && !/^\(\d+ rows?\)$/.test(l))
    .slice(1)
}

let pass = 0, fail = 0
const ok = (n, c, e = '') => {
  if (c) { pass++; console.log(`  [PASS] ${n}${e ? ' — ' + e : ''}`) }
  else { fail++; console.log(`  [FAIL] ${n}${e ? ' — ' + e : ''}`) }
}

;(() => {
  try {
    console.log('积分台账对账（口径：**余额绝不落主档**，台账为权威）\n')

    console.log('⓪ 前置：台账三张表在')
    const n = num(`SELECT count(*) FROM information_schema.tables
      WHERE table_schema='public' AND table_name IN ('mkt_points_batch','mkt_points_journal','erp_loyalty_card')`)
    ok('mkt_points_batch / mkt_points_journal / erp_loyalty_card 齐备', n === 3, `实际 ${n} 张`)
    if (n !== 3) process.exit(2)
    const seq = num(`SELECT count(*) FROM information_schema.columns
      WHERE table_schema='public' AND table_name IN ('mkt_points_batch','mkt_points_journal')
        AND column_name='id' AND column_default LIKE 'nextval%'`)
    ok('台账两表主键已挂序列默认值（V11.522.0；否则 SQL 侧搬数据插不进去）', seq === 2, `实际 ${seq}`)

    console.log('\n① 期初对平（这是 V11.523.0 的验收，永久有效）')
    const srcRows = num(`SELECT count(*) FROM biz_party WHERE deleted=0 AND COALESCE(points,0) <> 0`)
    const openingBatch = num(`SELECT count(*) FROM mkt_points_batch WHERE deleted=0 AND source='OPENING'`)
    const srcSum = num(`SELECT COALESCE(sum(points),0) FROM biz_party WHERE deleted=0 AND COALESCE(points,0) <> 0`)
    const dstSum = num(`SELECT COALESCE(sum(remaining_points),0) FROM mkt_points_batch WHERE deleted=0 AND source='OPENING'`)
    console.log(`  源：${srcRows} 个主体 / 合计 ${srcSum} 分；台账期初：${openingBatch} 个批次 / 合计 ${dstSum} 分`)
    // ⚠️ 逐主体比对（比总量强）
    const mismatch = num(`SELECT count(*) FROM biz_party b
       WHERE b.deleted=0 AND COALESCE(b.points,0) <> 0
         AND COALESCE((SELECT sum(x.remaining_points) FROM mkt_points_batch x
                        WHERE x.tenant_id=b.tenant_id AND x.partner_id=b.id
                          AND x.source='OPENING' AND x.deleted=0), 0) <> COALESCE(b.points,0)`)
    ok('每个有积分的主体：期初批次金额与 biz_party.points **逐主体**相等', mismatch === 0, `不一致 ${mismatch} 个`)
    ok('期初批次条数与源主体数一致', openingBatch === srcRows, `${openingBatch} vs ${srcRows}`)
    ok('期初批次合计与源合计一致', dstSum === srcSum, `${dstSum} vs ${srcSum}`)
    const noJournal = num(`SELECT count(*) FROM mkt_points_batch x
       WHERE x.deleted=0 AND x.source='OPENING'
         AND NOT EXISTS (SELECT 1 FROM mkt_points_journal j
                          WHERE j.batch_id = x.id AND j.change_type='OPENING' AND j.deleted=0)`)
    ok('每个期初批次都有配套 OPENING 流水（否则审计链断了）', noJournal === 0, `缺流水 ${noJournal} 个`)

    console.log('\n② 账实对照（📋 报告，不判失败 —— 两者本就允许不等）')
    const ledgerSum = num(`SELECT COALESCE(sum(remaining_points),0) FROM mkt_points_batch
                            WHERE deleted=0 AND status='ACTIVE'`)
    const frozen = num(`SELECT COALESCE(sum(points),0) FROM biz_party WHERE deleted=0`)
    console.log(`  台账可用合计 = ${ledgerSum}；biz_party.points 合计 = ${frozen}（已停写，冻结在最后一刻）`)
    console.log(`  ⇒ 相等＝自停写以来没有新活动；不等＝**新活动已经发生**（这是正常的，不是错）`)
    const stale = rows(`SELECT id, tenant_id, points FROM biz_party
                         WHERE deleted=0 AND COALESCE(points,0) <> 0
                           AND COALESCE((SELECT sum(x.remaining_points) FROM mkt_points_batch x
                                          WHERE x.partner_id=biz_party.id AND x.deleted=0),0) <> points
                         ORDER BY id LIMIT 20`)
    console.log(`  已与台账不一致的主体（biz_party.points 已冻结，仅作对照）：${stale.length} 行` +
      (stale.length ? '\n    ' + stale.join('\n    ') : ''))

    console.log('\n③ ⚠️ 待业务跟进：**有积分但没有卡**（进得了台账、走不了卡路径兑付）')
    const noCard = rows(`SELECT x.partner_id, x.tenant_id, x.remaining_points
                           FROM mkt_points_batch x
                          WHERE x.deleted=0 AND x.status='ACTIVE'
                            AND COALESCE(x.member_card_no,'') = ''
                          ORDER BY x.remaining_points DESC LIMIT 50`)
    console.log(`  ${noCard.length} 个批次（台账 API 的 earn/use/available 都是**卡号版**，`
      + '这些分要用必须先补发会员卡再调整；不要去改台账数据来"绕过"）')
    if (noCard.length) console.log('    ' + noCard.join('\n    '))

    console.log(`\n===== 结果：PASS ${pass} / FAIL ${fail} =====`)
    process.exit(fail ? 1 : 0)
  } catch (e) {
    console.error(`脚本异常: ${e.message}`)
    process.exit(2)
  }
})()
