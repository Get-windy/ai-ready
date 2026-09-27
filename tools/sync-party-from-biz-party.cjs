#!/usr/bin/env node
/**
 * `party` / `party_tenant` / `party_cert` / `party_bank` / `party_address`
 *   ← `biz_party` 的**对账与补齐**（阶段 3 并存期的兜底工具）
 *
 * 【它解决什么】并存期是"写 `biz_party`，同时镜像到新表"。镜像一旦失败（`PartyMirrorWriter`
 *   按用户裁定「选项乙」**记 ERROR 不阻断建档**），新表就缺一块 ⇒ 必须有工具能随时补平。
 *   它也是**漂移监控**：差额持续增长，就说明镜像在成片失效。
 *
 * 【⚠️ 它与双写必须逐字同口径，否则会"补出另一种形状的行"】
 *   · `party.id` **复用 `biz_party.id`**；`party.tenant_id` 恒 0（共享层，§6.5）；
 *   · 边：方向**可并存**——`roles` 含 CUSTOMER ⇒ SALE，含 SUPPLIER 或 `party_type=2` ⇒ PURCHASE
 *     （与 `PartyMirrorWriter.directionsOf` 一致）。⚠️ **不是**建表回填那种
 *     `CASE ... THEN 'SALE' ELSE 'PURCHASE'` 的单边口径 —— 两者在"既是客户又是供应商"的行上
 *     结果不同，早期版本就是单边口径，会让这类行**永远缺一条边**；
 *   · 商务条件**按方向归属**（裁定 ⑲「账期不可传递」）：`credit_*`/`current_debt` 只落 SALE 边、
 *     `payment_*` 只落 PURCHASE 边 —— 与 `PartyMirrorMapper.upsertEdge` 的 CASE 同口径；
 *   · 子表：证件各一条（`BUSINESS_LICENSE`/`TAX`）、银行一条默认账户、地址一条注册地址
 *     （`address_type=1`），与 `V11.506.0` §5.2~5.4 的回填一致；
 *   · **方向判不出来的一律不建边**（如 `party_type=3` 第三方服务主体），并在报告里列出来 ——
 *     **不猜**。
 *
 * 【幂等】默认只做"补缺"（已存在的不重插、不改值）；`--refresh` 才反过来用 `biz_party`
 *   覆盖新表已有行的值。
 *
 * 用法：
 *   node tools/sync-party-from-biz-party.cjs            # 看差额 + 补齐（默认执行）
 *   node tools/sync-party-from-biz-party.cjs --dry-run  # 只看，不写
 *   node tools/sync-party-from-biz-party.cjs --refresh  # 额外用 biz_party 覆盖新表已有行
 *   前置：本地 dev 库可连（走 tools/sql.cjs）；`V11.520.0`/`V11.521.0` 已应用。
 *   退出码：0 = 补齐后一致；1 = 仍有差额；2 = 脚本自身失败。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

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

// ─────────────────────────── 口径常量（改一处，双写那边也要一起改） ───────────────────────────

/**
 * 方向展开：一行 biz_party 最多展开成两条边（⑬ 角色可并存）。
 * ⚠️ 这里**必须**与 `PartyMirrorWriter.directionsOf` 同口径；且 `roles` 为 NULL 时
 *    `LIKE` 返回 NULL 而不是 false —— 但作为 JOIN 条件时 NULL 等价于不匹配，正是我们要的
 *    （roles 为空 → 只看 party_type），所以这里不需要额外 IS NULL 处理。
 */
const DIRECTION_ROWS = `
       JOIN LATERAL (
         SELECT 'SALE'::varchar AS direction WHERE b.roles LIKE '%CUSTOMER%'
         UNION ALL
         SELECT 'PURCHASE'::varchar WHERE b.roles LIKE '%SUPPLIER%' OR b.party_type = 2
       ) d ON TRUE`

/** 边的列清单（与 `PartyMirrorMapper.upsertEdge` 的 INSERT 逐字一致） */
const EDGE_COLS = `tenant_id, party_id, direction, party_level, settlement_type, settlement_days,
       statement_day, settlement_day, payment_term_type, price_track_enabled, status,
       credit_limit, current_debt, credit_days, fixed_credit_day,
       payment_days, fixed_payment_day,
       default_handler_id, default_handler_name, promoter_id, promoter_name,
       buyer_account, customer_source, roles, category_id, warehouse_name, last_trade_time,
       create_time, update_time, deleted`

/** 边的取值（方向独占列用 CASE 钉在自己方向上 —— 裁定 ⑲） */
const EDGE_VALUES = `b.tenant_id, b.id, d.direction, b.party_level,
       CASE WHEN b.settlement_type IS NULL THEN NULL
            WHEN b.settlement_type = 0 THEN '现结' ELSE '挂账' END,
       b.settlement_days, b.statement_day, b.settlement_day, b.payment_term_type,
       COALESCE(b.price_track_enabled, 0) <> 0, COALESCE(b.status, 1),
       CASE WHEN d.direction = 'SALE' THEN b.credit_limit END,
       CASE WHEN d.direction = 'SALE' THEN b.current_debt END,
       CASE WHEN d.direction = 'SALE' THEN b.credit_days END,
       CASE WHEN d.direction = 'SALE' THEN b.fixed_credit_day END,
       CASE WHEN d.direction = 'PURCHASE' THEN b.payment_days END,
       CASE WHEN d.direction = 'PURCHASE' THEN b.fixed_payment_day END,
       b.default_handler_id, b.default_handler_name, b.promoter_id, b.promoter_name,
       b.buyer_account, b.customer_source, b.roles, b.category_id, b.warehouse_name,
       b.last_trade_time, now(), now(), 0`

/** A 组落到 `party` 的全部列（`--refresh` 时整体回刷） */
const PARTY_COLS = ['party_code', 'unified_code', 'party_name', 'short_name', 'company_full_name',
  'mnemonic_code', 'party_type', 'legal_person', 'phone', 'email', 'website', 'fax',
  'legal_person_phone', 'status', 'remark']

// ─────────────────────────── 差额快照 ───────────────────────────

function gap() {
  return {
    业务表未删: num(`SELECT count(*) FROM biz_party WHERE deleted = 0`),
    新主档: num(`SELECT count(*) FROM party WHERE deleted = 0`),
    未进新表: num(`SELECT count(*) FROM biz_party b WHERE b.deleted = 0
                    AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = b.id)`),
    缺贸易边: num(`SELECT count(*) FROM biz_party b ${DIRECTION_ROWS}
                    WHERE b.deleted = 0
                      -- ⚠️ 没有归属租户的主体**建不了边**（边必须有 tenant_id），
                      --    双写那边也是这样跳过并记 WARN 的 ⇒ 这里必须同样排除，
                      --    否则会报一个永远补不上的假差额
                      AND b.tenant_id IS NOT NULL
                      AND NOT EXISTS (SELECT 1 FROM party_tenant t
                                       WHERE t.party_id = b.id AND t.tenant_id = b.tenant_id
                                         AND t.direction = d.direction AND t.deleted = 0)`),
    主档无租户: num(`SELECT count(*) FROM biz_party WHERE deleted = 0 AND tenant_id IS NULL`),
    边缺商务条件: num(`SELECT count(*) FROM party_tenant t
                        JOIN biz_party b ON b.id = t.party_id
                       WHERE t.deleted = 0 AND b.deleted = 0
                         AND t.roles IS NULL AND b.roles IS NOT NULL`),
    // ⚠️ "多出来的边"也要数：角色会变（客户改成供应商），历史版本的双写只 upsert 适用方向、
    //    不清理旧方向 ⇒ 会留下"曾经是客户"的陈旧 SALE 边。这类漂移**只数"缺"是看不见的**。
    //    判据与 `PartyMirrorWriter.directionsOf` 一致：SALE 要 roles 含 CUSTOMER；
    //    PURCHASE 要 roles 含 SUPPLIER 或 party_type = 2。（用 COALESCE 避开 NULL LIKE 的三值逻辑）
    多余贸易边: num(`SELECT count(*) FROM party_tenant t
                        JOIN biz_party b ON b.id = t.party_id AND b.tenant_id = t.tenant_id
                       WHERE t.deleted = 0 AND b.deleted = 0
                         AND ((t.direction = 'SALE'
                               AND COALESCE(b.roles, '') NOT LIKE '%CUSTOMER%')
                           OR (t.direction = 'PURCHASE'
                               AND COALESCE(b.roles, '') NOT LIKE '%SUPPLIER%'
                               AND COALESCE(b.party_type, 0) <> 2))`),
    子表缺行: num(`SELECT
        (SELECT count(*) FROM biz_party b WHERE b.deleted = 0
           AND btrim(coalesce(b.business_license, '')) <> ''
           AND NOT EXISTS (SELECT 1 FROM party_cert c WHERE c.party_id = b.id
                            AND c.cert_type = 'BUSINESS_LICENSE' AND c.deleted = 0))
      + (SELECT count(*) FROM biz_party b WHERE b.deleted = 0
           AND btrim(coalesce(b.tax_number, '')) <> ''
           AND NOT EXISTS (SELECT 1 FROM party_cert c WHERE c.party_id = b.id
                            AND c.cert_type = 'TAX' AND c.deleted = 0))
      + (SELECT count(*) FROM biz_party b WHERE b.deleted = 0
           AND (btrim(coalesce(b.bank_name, '')) <> '' OR btrim(coalesce(b.bank_account, '')) <> '')
           AND NOT EXISTS (SELECT 1 FROM party_bank k WHERE k.party_id = b.id
                            AND k.deleted = 0 AND k.is_default = 1))
      + (SELECT count(*) FROM biz_party b WHERE b.deleted = 0
           AND (btrim(coalesce(b.address, '')) <> '' OR btrim(coalesce(b.city, '')) <> ''
                OR btrim(coalesce(b.province, '')) <> '')
           AND NOT EXISTS (SELECT 1 FROM party_address a WHERE a.party_id = b.id
                            AND a.deleted = 0 AND a.address_type = 1))`),
  }
}

const clean = (n) => n === 0
const allClean = (g) => clean(g.未进新表) && clean(g.缺贸易边) && clean(g.边缺商务条件)
  && clean(g.子表缺行) && clean(g.多余贸易边)

// ─────────────────────────── 补齐 ───────────────────────────

function backfill() {
  // ① 主档（A 组全列；id 复用 biz_party.id、tenant_id 恒 0）
  sql(`INSERT INTO party (id, tenant_id, party_code, unified_code, party_name, short_name,
                          company_full_name, mnemonic_code, party_type, legal_person, phone, email,
                          website, fax, legal_person_phone, status, remark,
                          create_time, update_time, deleted)
       SELECT b.id, 0, b.party_code, b.unified_code, b.party_name, b.short_name,
              b.company_full_name, b.mnemonic_code, b.party_type, b.legal_person, b.phone, b.email,
              b.website, b.fax, b.legal_person_phone, COALESCE(b.status, 1), b.remark,
              now(), now(), 0
       FROM biz_party b
       WHERE b.deleted = 0 AND NOT EXISTS (SELECT 1 FROM party p WHERE p.id = b.id)`)

  // ② 贸易边（方向可并存；商务条件按方向归属）
  sql(`INSERT INTO party_tenant (${EDGE_COLS})
       SELECT ${EDGE_VALUES}
       FROM biz_party b ${DIRECTION_ROWS}
       WHERE b.deleted = 0
         AND NOT EXISTS (SELECT 1 FROM party_tenant t
                          WHERE t.party_id = b.id AND t.tenant_id = b.tenant_id
                            AND t.direction = d.direction AND t.deleted = 0)`)

  // ②b 清掉**不再适用**的方向的边（角色变了：客户 → 供应商）
  //     ⚠️ 与 `PartyMirrorWriter.onWrite` 的反向清理同口径；漏了这一步，对账脚本自己
  //     就会把"多余的边"永久留在库里（因为它默认只补缺、不删多）。
  sql(`UPDATE party_tenant t SET deleted = 1, update_time = now()
       FROM biz_party b
       WHERE t.party_id = b.id AND t.tenant_id = b.tenant_id
         AND t.deleted = 0 AND b.deleted = 0
         AND ((t.direction = 'SALE'
               AND COALESCE(b.roles, '') NOT LIKE '%CUSTOMER%')
           OR (t.direction = 'PURCHASE'
               AND COALESCE(b.roles, '') NOT LIKE '%SUPPLIER%'
               AND COALESCE(b.party_type, 0) <> 2))`)

  // ③ 子表：证件 —— 执照 / 税务各一条
  sql(`INSERT INTO party_cert (party_id, cert_type, cert_no, valid_to, remark,
                               create_time, update_time, deleted)
       SELECT b.id, 'BUSINESS_LICENSE', b.business_license, b.business_license_expiry, '对账补齐',
              now(), now(), 0
       FROM biz_party b
       WHERE b.deleted = 0 AND btrim(coalesce(b.business_license, '')) <> ''
         AND NOT EXISTS (SELECT 1 FROM party_cert c WHERE c.party_id = b.id
                          AND c.cert_type = 'BUSINESS_LICENSE' AND c.deleted = 0)`)
  sql(`INSERT INTO party_cert (party_id, cert_type, cert_no, remark,
                               create_time, update_time, deleted)
       SELECT b.id, 'TAX', b.tax_number, '对账补齐', now(), now(), 0
       FROM biz_party b
       WHERE b.deleted = 0 AND btrim(coalesce(b.tax_number, '')) <> ''
         AND NOT EXISTS (SELECT 1 FROM party_cert c WHERE c.party_id = b.id
                          AND c.cert_type = 'TAX' AND c.deleted = 0)`)

  // ③ 子表：银行（源表那唯一一个账户 = 默认账户）
  sql(`INSERT INTO party_bank (party_id, bank_name, bank_account, bank_address, is_default,
                               create_time, update_time, deleted)
       SELECT b.id, b.bank_name, b.bank_account, b.bank_address, 1, now(), now(), 0
       FROM biz_party b
       WHERE b.deleted = 0
         AND (btrim(coalesce(b.bank_name, '')) <> '' OR btrim(coalesce(b.bank_account, '')) <> '')
         AND NOT EXISTS (SELECT 1 FROM party_bank k WHERE k.party_id = b.id
                          AND k.deleted = 0 AND k.is_default = 1)`)

  // ③ 子表：注册地址
  sql(`INSERT INTO party_address (party_id, address_type, province, city, district, detail_address,
                                  is_default, remark, create_time, update_time, deleted)
       SELECT b.id, 1, b.province, b.city, b.district, b.address, 1, '对账补齐', now(), now(), 0
       FROM biz_party b
       WHERE b.deleted = 0
         AND (btrim(coalesce(b.address, '')) <> '' OR btrim(coalesce(b.city, '')) <> ''
              OR btrim(coalesce(b.province, '')) <> '')
         AND NOT EXISTS (SELECT 1 FROM party_address a WHERE a.party_id = b.id
                          AND a.deleted = 0 AND a.address_type = 1)`)
}

/** `--refresh`：用 `biz_party` **覆盖**新表已有行的值（默认不做，怕盖掉人工修正） */
function refresh() {
  sql(`UPDATE party p SET
         ${PARTY_COLS.map(c => `${c} = b.${c}`).join(', ')},
         update_time = now()
       FROM biz_party b WHERE b.id = p.id AND b.deleted = 0`)

  sql(`UPDATE party_tenant t SET
         party_level = b.party_level,
         settlement_type = CASE WHEN b.settlement_type IS NULL THEN NULL
                                WHEN b.settlement_type = 0 THEN '现结' ELSE '挂账' END,
         settlement_days = b.settlement_days, statement_day = b.statement_day,
         settlement_day = b.settlement_day, payment_term_type = b.payment_term_type,
         price_track_enabled = (COALESCE(b.price_track_enabled, 0) <> 0),
         credit_limit  = CASE WHEN t.direction = 'SALE'     THEN b.credit_limit     END,
         current_debt  = CASE WHEN t.direction = 'SALE'     THEN b.current_debt     END,
         credit_days   = CASE WHEN t.direction = 'SALE'     THEN b.credit_days      END,
         fixed_credit_day = CASE WHEN t.direction = 'SALE'  THEN b.fixed_credit_day END,
         payment_days  = CASE WHEN t.direction = 'PURCHASE' THEN b.payment_days     END,
         fixed_payment_day = CASE WHEN t.direction = 'PURCHASE' THEN b.fixed_payment_day END,
         default_handler_id = b.default_handler_id, default_handler_name = b.default_handler_name,
         promoter_id = b.promoter_id, promoter_name = b.promoter_name,
         buyer_account = b.buyer_account, customer_source = b.customer_source,
         roles = b.roles, category_id = b.category_id, warehouse_name = b.warehouse_name,
         last_trade_time = b.last_trade_time,
         update_time = now()
       FROM biz_party b
       WHERE t.party_id = b.id AND t.tenant_id = b.tenant_id AND t.deleted = 0 AND b.deleted = 0`)

  sql(`UPDATE party_cert c SET cert_no = b.business_license,
         valid_to = b.business_license_expiry, update_time = now()
       FROM biz_party b
       WHERE c.party_id = b.id AND c.cert_type = 'BUSINESS_LICENSE' AND c.deleted = 0 AND b.deleted = 0`)
  sql(`UPDATE party_cert c SET cert_no = b.tax_number, update_time = now()
       FROM biz_party b
       WHERE c.party_id = b.id AND c.cert_type = 'TAX' AND c.deleted = 0 AND b.deleted = 0`)
  sql(`UPDATE party_bank k SET bank_name = b.bank_name, bank_account = b.bank_account,
         bank_address = b.bank_address, update_time = now()
       FROM biz_party b
       WHERE k.party_id = b.id AND k.is_default = 1 AND k.deleted = 0 AND b.deleted = 0`)
  sql(`UPDATE party_address a SET province = b.province, city = b.city, district = b.district,
         detail_address = b.address, update_time = now()
       FROM biz_party b
       WHERE a.party_id = b.id AND a.address_type = 1 AND a.deleted = 0 AND b.deleted = 0`)
}

// ─────────────────────────── 主流程 ───────────────────────────

;(() => {
  const args = process.argv.slice(2)
  const dry = args.includes('--dry-run')
  const doRefresh = args.includes('--refresh')
  console.log(`party/party_tenant/子表 ⇄ biz_party 对账${dry ? '（--dry-run：只看不写）' : ''}`)

  try {
    const before = gap()
    console.log(`  补齐前：${JSON.stringify(before)}`)

    // 方向判不出来的（不建边），单独列出来 —— 这是"不猜"的证据，也是"缺贸易边"的合法来源
    const undecidable = sql(`SELECT b.id, b.party_name, b.party_type, b.roles FROM biz_party b
      WHERE b.deleted = 0
        -- ⚠️ 必须显式处理 NULL：roles 为 NULL 时 NOT LIKE 返回 NULL 而不是 TRUE
        --    ⇒ 只用 NOT LIKE 会把 roles 为空的行漏掉（本脚本第一版就是，
        --    症状是"缺边 N 行 vs 方向判不出 0 行"自相矛盾）
        --    （注意：本段是 JS 模板字符串，SQL 注释里不要写反引号）
        AND (b.roles IS NULL OR (b.roles NOT LIKE '%CUSTOMER%' AND b.roles NOT LIKE '%SUPPLIER%'))
        AND (b.party_type IS NULL OR b.party_type <> 2)
      ORDER BY b.id`).replace(/\r/g, '').split('\n').slice(2)
      .map(l => l.trim()).filter(l => l && !/^\(\d+ rows?\)$/.test(l))
    console.log(`  方向判不出、将**不建边**的：${undecidable.length} 行`
      + (undecidable.length ? ' ⇒ ' + undecidable.join(' | ') : ''))

    if (before.主档无租户 > 0) {
      console.log(`  ⚠️ 有 ${before.主档无租户} 行 biz_party 没有 tenant_id ⇒ 建不了贸易边`
        + '（双写同样会跳过并记 WARN）；这不是本脚本能补的缺口')
    }
    if (allClean(before) && !doRefresh) {
      console.log('  ✅ 各项差额均为 0，无需补齐')
      process.exit(0)
    }
    if (dry) {
      console.log('  （--dry-run）未写库。')
      process.exit(0)
    }

    backfill()
    if (doRefresh) {
      refresh()
      console.log('  --refresh：已用 biz_party 覆盖 party / party_tenant / 三张子表')
    }

    const after = gap()
    console.log(`  补齐后：${JSON.stringify(after)}`)
    const bad = []
    if (!clean(after.未进新表)) bad.push(`未进新表 ${after.未进新表}`)
    if (!clean(after.缺贸易边)) bad.push(`缺贸易边 ${after.缺贸易边}`)
    if (!clean(after.多余贸易边)) bad.push(`多余贸易边 ${after.多余贸易边}`)
    if (!clean(after.边缺商务条件)) bad.push(`边缺商务条件 ${after.边缺商务条件}`)
    if (!clean(after.子表缺行)) bad.push(`子表缺行 ${after.子表缺行}`)
    if (bad.length) {
      console.error(`  ❌ 补齐后仍有差额：${bad.join('、')}`)
      process.exit(1)
    }
    console.log(`  ✅ 主档 / 贸易边 / 三张子表均已对齐（"方向判不出"的 ${undecidable.length} 行本来就不该有边）`)
    process.exit(0)
  } catch (e) {
    console.error(`脚本异常: ${e instanceof Fail ? e.message : e}`)
    process.exit(2)
  }
})()
