#!/usr/bin/env node
/**
 * 单据快照规范审计（DOMAIN-MODEL §11.6「单据快照规范 · 阶段 2 · U2」）
 *
 * 【本脚本只给事实 + 守棘轮，不做裁定】规范说"哪些字段**必须**快照"，本脚本把**真库**里
 *   与规范不符的表列出来；**该不该收敛、怎么收敛**由人按 §11.6.6 判 —— 与
 *   `check-menu-targets.py` / `audit-pagination-expand.cjs` 同一分工。
 *
 * 【为什么要它】§11.6 是唯一"晚做就要全表回填"的规范。没有可执行检查，规范就只是一段文字，
 *   下一张新表照样会漏（本仓最贵的历史包袱就是"配了不生效 / 定了不做"）。
 *
 * 【为什么用**登记制**而不是猜"哪些是单据"】第一版按"有 `_no` 列"自动判定"单据表"，
 *   真库上判出 80+ 张 —— 明细表 / 台账 / 缓存 / 日志全被卷进来（`wms_pick_detail`、
 *   `serial_status_cache`…）。**一条 80% 误报的检查比没有检查更糟**（会训练所有人忽略它）。
 *   因此受约束的表**由人显式登记**（`BILL_TABLES`，§11.6.8 的镜像），规则只在登记表上生效。
 *
 * 【棘轮】存量缺口登记在 `KNOWN_GAPS`（每条带理由，§11.6.6/§11.6.7 有对应行）：
 *   它们**不**触发失败；**只有出现清单之外的新违规才 exit 1**。
 *   清单**只许缩不许涨** —— 新增一条 = 放宽规范，必须先改文档再改这里。
 *
 * 【四条规则】
 *   R1 主体名称快照：有 `<role>_id` 就必须有 `<role>_name`（`role ∈ customer|supplier`；全库）
 *   R2 业务发生日  ：登记的单据主表必须有业务日期列（业务日期 ≠ create_time）
 *   R3 税率时点    ：登记的单据主表有 `tax_amount` 就应有 `tax_rate`（只有税额算不出当时税率）
 *   R4 协议版本号  ：登记的单据主表应能回答"当时按哪一版协议"（全新要求，只报覆盖率）
 *
 * ⚠️ 【踩过的坑：`tools/sql.cjs` 硬上限 **500 行且静默截断**】
 *   第一版想"一次把全库 表→列 拉回来"，14146 行被截成前 500 行 ⇒ 只看到 29 张表
 *   （真实 568 张），**且不报错** —— 报出来的"覆盖率 100%"是假的。
 *   因此：① 每个查询都压到几十行（GROUP BY / IN 收窄）；② `rows()` 命中上限直接抛错。
 *
 * 用法：node tools/audit-snapshot-spec.cjs
 *   前置：本地 dev 库可连（走 tools/sql.cjs，与其它 verify-*.cjs 同一条通道）。
 *   退出码：0 = 无新增违规；1 = 出现**清单之外**的违规；2 = 脚本自身失败（连不上库 / 结果被截断）。
 */

const { execFileSync } = require('child_process')
const path = require('path')

const REPO = path.resolve(__dirname, '..')

// ══════════════════════ 规范参数（与 §11.6 逐条对应） ══════════════════════

/** R1 检查的"主体角色"列前缀。 */
const ROLES = ['customer', 'supplier']

/**
 * §11.6.8 受本规范约束的**单据主表**（登记制）。
 * 判据（人判）：这张表是"一笔业务发生后要留痕、要认当时账"的单据主表。
 * ⚠️ 新增单据主表**必须登记进来**（否则等于漏了规范）；明细表 / 台账 / 关联表 / 档案**不登记**。
 */
const BILL_TABLES = [
  // 销售
  'erp_sale_order', 'erp_sale_outbound', 'erp_sale_return', 'erp_sale_return_doc',
  'erp_sale_exchange', 'erp_sale_pre_order', 'erp_retail_order',
  // 采购
  'erp_purchase_order', 'erp_purchase_inbound', 'erp_purchase_return', 'erp_purchase_exchange',
  // 资金
  'erp_receipt', 'erp_payment', 'erp_pre_receipt', 'erp_pre_payment',
  'erp_ar_ap_adjust', 'erp_write_off', 'erp_offset', 'erp_cash_transfer', 'erp_expense_doc',
  // 库存
  'erp_stock_in', 'erp_stock_out', 'erp_stock_transfer', 'erp_stock_damage',
  'erp_stock_overflow', 'erp_stock_take', 'erp_stock_assemble', 'erp_stock_split',
  // 配送
  'dms_ship_order', 'dms_settlement',
  // 合同 / 报价
  'crm_contract', 'crm_quotation',
]

/**
 * §11.6.6 R1 豁免：**不是"已生效单据"**（台账 / 档案 / 关联表 / 子表 / 现势表）。
 * 每一条都对应 §11.6.6 表格里的一行。
 */
const EXEMPT = new Map([
  ['erp_balance_log', '余额变动台账'],
  ['erp_customer_product_price', '客户商品价格档案'],
  ['erp_price_memory', '比价记忆（过程记录）'],
  ['erp_group_buy_participant', '拼团参与记录（子表，父单承载快照）'],
  ['invoice_payment_record', '发票×付款核销关联表'],
  ['mkt_presale_order', '预售单↔商城单↔客户关联（真单据是它指向的商城订单）'],
  ['erp_supplier_performance', '供应商绩效台账'],
  ['erp_supplier_points_record', '供应商积分台账'],
  ['erp_stock', '库存现势表（不是单据）'],
  ['erp_delivery_rating', '配送评分记录（子记录）'],
  ['erp_supplier_notification', '供应商通知（消息，非单据）'],
  ['batch_number', '批次主档（主数据，非单据）'],
  ['batch_flow_record', '批次流转记录（台账）'],
])

/**
 * §11.6.7 **存量已知缺口（棘轮）** —— 只许缩不许涨。
 * `[表名, 规则, 理由]`；新增一条必须先写进 §11.6.7 的表格并给出理由。
 */
const KNOWN_GAPS = [
  ['dms_ship_order', 'R2', '配送发货单：只有 create_time，无业务发生日（配送域遗留，待 DMS 侧补）'],
  ['dms_settlement', 'R2', '配送结算单：只有 create_time，无业务发生日（同上）'],
  ['erp_sale_order', 'R3', '有 tax_amount 无 tax_rate（税率是时点属性，待补快照）'],
  ['erp_purchase_order', 'R3', '有 tax_amount 无 tax_rate'],
  ['erp_purchase_inbound', 'R3', '有 tax_amount 无 tax_rate'],
  ['erp_purchase_return', 'R3', '有 tax_amount 无 tax_rate'],
  ['finance_tax_declaration', 'R3', '纳税申报表：有 tax_amount 无 tax_rate（申报口径，待定）'],
]

/** 业务日期的机械判据：列名以 `_date` 结尾即视为业务日期候选（域内语义名，§11.6.4）。 */
const BUSINESS_DATE_SUFFIX = '_date'

// ══════════════════════ 取数 ══════════════════════

class AuditFailure extends Error {}

function rawSql(stmt) {
  try {
    return execFileSync('node', [path.join(REPO, 'tools', 'sql.cjs'), stmt],
      { cwd: REPO, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024 })
  } catch (e) {
    const first = (String(e.stderr || e.message || e).replace(/\r/g, '').split('\n')
      .map(l => l.trim()).filter(Boolean)[0]) || '(无输出)'
    throw new AuditFailure(`SQL 失败: ${stmt.slice(0, 120)} —— ${first}`)
  }
}

/** 解析 sql.cjs 的表格输出；**命中 500 行上限时抛错**（见文件头的坑）。 */
function rows(stmt) {
  const text = rawSql(stmt).replace(/\r/g, '')
  const trailer = text.match(/\((\d+) rows?\)/)
  if (trailer && Number(trailer[1]) >= 500) {
    throw new AuditFailure(`查询命中 tools/sql.cjs 的 500 行硬上限（≥500）⇒ 结果**不可信**，`
      + `请收窄查询：${stmt.slice(0, 120)}`)
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

/** 「表名 → 命中列集合」——按表聚合，避免触及 500 行上限。 */
function tableColMap(columnNames) {
  const out = new Map()
  for (const r of rows(`SELECT c.table_name AS t, string_agg(c.column_name, ',' ORDER BY c.column_name) AS cols
      FROM information_schema.columns c
      JOIN information_schema.tables tb
        ON tb.table_schema = c.table_schema AND tb.table_name = c.table_name
     WHERE c.table_schema = 'public' AND tb.table_type = 'BASE TABLE'
       AND c.column_name IN (${columnNames.map(c => `'${c}'`).join(',')})
     GROUP BY c.table_name ORDER BY c.table_name`)) {
    out.set(r.t, new Set(String(r.cols).split(',')))
  }
  return out
}

/** 只取存在性：给出表名清单，返回 `表名 → 列集合`（用 VALUES 收窄，绝不拉全库）。 */
function colsOf(tables, columnNames) {
  const values = tables.map(t => `('${t}')`).join(',')
  const out = new Map()
  for (const r of rows(`SELECT c.table_name AS t,
         string_agg(c.column_name, ',' ORDER BY c.column_name) AS cols
      FROM information_schema.columns c
      JOIN (VALUES ${values}) v(name) ON v.name = c.table_name
     WHERE c.table_schema = 'public' AND c.column_name IN (${columnNames.map(c => `'${c}'`).join(',')})
     GROUP BY c.table_name ORDER BY c.table_name`)) {
    out.set(r.t, new Set(String(r.cols).split(',')))
  }
  return out
}

/** 全库：所有 `*_date` 列的宿主表。 */
function tablesWithDateColumn() {
  return new Set(rows(`SELECT c.table_name AS t
      FROM information_schema.columns c
      JOIN information_schema.tables tb
        ON tb.table_schema = c.table_schema AND tb.table_name = c.table_name
     WHERE c.table_schema = 'public' AND tb.table_type = 'BASE TABLE'
       AND c.column_name LIKE '%${BUSINESS_DATE_SUFFIX}'
     GROUP BY c.table_name ORDER BY c.table_name`).map(r => r.t))
}

// ══════════════════════ 主流程 ══════════════════════

const gapKeys = new Set(KNOWN_GAPS.map(([t, r]) => `${t}|${r}`))
const newViolations = []   // 清单之外 ⇒ exit 1
const knownHits = []       // 已知存量 ⇒ 只报事实
const exemptHits = []      // R1 豁免表 ⇒ 只报事实

function judge(table, rule, detail) {
  if (gapKeys.has(`${table}|${rule}`)) knownHits.push({ table, rule, detail })
  else newViolations.push({ table, rule, detail })
}

const section = (t) => console.log(`\n${t}`)

;(() => {
  console.log('单据快照规范审计（DOMAIN-MODEL §11.6 · 阶段 2 / U2）')

  let roleCols, billCols, dateHosts, snapshotTables
  try {
    roleCols = tableColMap(ROLES.flatMap(r => [`${r}_id`, `${r}_name`]))
    billCols = colsOf(BILL_TABLES,
      ['tax_amount', 'tax_rate', 'agreement_id', 'agreement_version_no'])
    dateHosts = tablesWithDateColumn()
    snapshotTables = new Set(rows(`SELECT table_name AS t FROM information_schema.tables
        WHERE table_schema='public' AND table_name LIKE '%\\_partner_snapshot' ORDER BY table_name`)
      .map(r => r.t))
  } catch (e) {
    console.error(`脚本异常: ${e instanceof AuditFailure ? e.message : e}`)
    process.exit(2)
  }
  console.log(`  登记单据主表 ${BILL_TABLES.length} 张 · 伴生快照表 ${snapshotTables.size} 张`)

  // ── R1 主体名称快照（全库事实 + 豁免） ──
  section('① R1 主体名称快照：有 <role>_id 就必须有 <role>_name（全库）')
  let r1Checked = 0
  for (const role of ROLES) {
    const hosts = [...roleCols.entries()].filter(([, c]) => c.has(`${role}_id`)).map(([t]) => t)
    const missing = hosts.filter(t => !roleCols.get(t).has(`${role}_name`)
      && ![...snapshotTables].some(s => s.startsWith(t)))
    r1Checked += hosts.length
    console.log(`  ${role}_id 宿主表 ${hosts.length} 张，其中有 ${role}_name 的 ${hosts.length - missing.length} 张`
      + `（覆盖率 ${hosts.length ? Math.round((hosts.length - missing.length) / hosts.length * 100) : 100}%）`)
    for (const t of missing) {
      if (EXEMPT.has(t)) exemptHits.push({ table: t, rule: 'R1', detail: `${role}_id 无 ${role}_name` })
      else judge(t, 'R1', `${role}_id 无 ${role}_name`)
    }
  }
  console.log(`  共查 ${r1Checked} 张宿主表`)

  // ── R2 业务发生日（仅登记的单据主表） ──
  section('② R2 业务发生日：登记的单据主表必须有业务日期列')
  const missingDate = BILL_TABLES.filter(t => !dateHosts.has(t))
  console.log(`  登记表 ${BILL_TABLES.length} 张，缺业务日期列的 ${missingDate.length} 张`)
  for (const t of missingDate) judge(t, 'R2', '无任何 `*_date` 列（业务日期 ≠ create_time）')

  // ── R3 税率时点（仅登记的单据主表） ──
  section('③ R3 税率时点：登记的单据主表有 tax_amount 就应有 tax_rate')
  const missingRate = BILL_TABLES.filter(t => billCols.get(t)?.has('tax_amount')
    && !billCols.get(t).has('tax_rate'))
  console.log(`  有税额无税率的登记表 ${missingRate.length} 张`)
  for (const t of missingRate) judge(t, 'R3', '有税额无税率（税率是时点属性，缺了算不出开票当时是多少）')

  // ── R4 协议版本号（全新要求，存量一律豁免，只报覆盖率） ──
  section('④ R4 协议版本号覆盖（全新要求 · 存量豁免，只报事实）')
  const withAgrId = BILL_TABLES.filter(t => billCols.get(t)?.has('agreement_id'))
  const withAgrVer = BILL_TABLES.filter(t => billCols.get(t)?.has('agreement_version_no'))
  console.log(`  登记表 ${BILL_TABLES.length} 张：有 agreement_id 的 ${withAgrId.length} 张，`
    + `有 agreement_version_no 的 ${withAgrVer.length} 张`)
  console.log('  ⚠️ 新增要求（阶段 5/6 落 `seller_party_id`/`buyer_party_id` 时一并加），存量不算违规。')

  // ── 结论 ──
  section('—— 结论 ——')
  if (exemptHits.length) {
    console.log(`  R1 豁免（§11.6.6 已逐张裁定，不是违规）${exemptHits.length} 处：`)
    for (const h of exemptHits) console.log(`    · ${h.table} [${h.rule}] ${h.detail} —— ${EXEMPT.get(h.table)}`)
  }
  if (knownHits.length) {
    console.log(`  已知存量缺口（棘轮内，§11.6.7）${knownHits.length} 处：`)
    for (const h of knownHits) {
      const reason = KNOWN_GAPS.find(([t, r]) => t === h.table && r === h.rule)?.[2] || ''
      console.log(`    · ${h.table} [${h.rule}] ${reason}`)
    }
    console.log('  口径：**存量不回改**；但它们只许缩不许涨（收敛掉一条就从 KNOWN_GAPS 删一条）。')
  }
  if (newViolations.length) {
    console.log(`  ❌ **清单之外**的新违规 ${newViolations.length} 处：`)
    for (const v of newViolations) console.log(`    · ${v.table} [${v.rule}] ${v.detail}`)
    console.log('\n  处置：新表立即补（§11.6.5）；若确属"不是单据"，'
      + '把它写进 EXEMPT **并同步 §11.6.6 的表格** —— 放宽规范必须先有文档理由。')
    process.exit(1)
  }
  console.log('  ✅ 无清单之外的新违规。')
  process.exit(0)
})()
