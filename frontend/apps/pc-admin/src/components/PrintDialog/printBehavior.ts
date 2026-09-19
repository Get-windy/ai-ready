import { printConfigApi, type PrintBehaviorConfig } from '@/api/set/print-config'

/**
 * 打印设置 → 打印行为适配层（`PrintDialog` 专用）
 *
 * ═══════════════════════════════════════════════════════════════
 * 为什么单独一个文件
 * ═══════════════════════════════════════════════════════════════
 * `PrintDialog` 是**跨模块共享组件**（20 多个页面复用：销售/采购/配送/财务/资料…），
 * 把「读打印设置 + 把设置作用到打印数据」的逻辑集中在此处，组件里只留调用点，
 * 便于一眼看清**哪些配置真的生效了、哪些没有**。除读配置外全部是纯函数，可直接断言。
 *
 * ═══════════════════════════════════════════════════════════════
 * 三条硬纪律（改这个文件前请先读）
 * ═══════════════════════════════════════════════════════════════
 * ① **读配置失败一律不影响打印**：无权限(403)/接口异常/超时 → 返回 null →
 *    调用方完全走**改造前的原有行为**。绝不允许「配置读不到就把打印弄挂」。
 * ② **只做格式化与派生字段注入**，不碰任何打印执行链路：模板渲染 `/v2/print/format/render`、
 *    本地打印（iframe + window.print）、远程打印（打印链 `/v2/print/tasks/by-chain`）
 *    一律复用既有能力，本文件不重复实现（《功能/模块不重复开发》）。
 * ③ **口径逐字取自 ql361 实测**：`tool-results/ql361/设置-deep/_summary.md` §三（2026-09-18）。
 *    不发明字段、不发明选项。
 *
 * ═══════════════════════════════════════════════════════════════
 * 7 个配置项的接线状态（本文件的「账」）
 * ═══════════════════════════════════════════════════════════════
 * | 配置项                   | 状态 | 落点 |
 * | ---                     | --- | --- |
 * | 允许打印草稿             | 已接线 | `isDraftDocument` + 组件 `open()` 门控 |
 * | 单据打印小数位数         | 已接线 | `applyPrintBehavior` → 数量/单价列 `toFixed(N)` |
 * | 打印内容                 | 已接线 | `applyPrintBehavior` → 明细行派生 `batchEffectiveText` |
 * | 助手打印                 | 已接线 | 组件 `handlePrint`：开启时跳过「必须先预览」 |
 * | 远程打印                 | 已接线 | 组件：远程模式门控 + 提交打印链任务 |
 * | 属性商品汇总打印         | **未接线** | 本系统打印引擎逐行直出，**无「按商品汇总行」能力** |
 * | 批次效期商品汇总打印     | **未接线** | 同上。不硬造汇总逻辑（造了会与模板列口径冲突） |
 */

// ═══════════════════════════════════════════════════════════════
// 一、读配置（带缓存 + 静默降级）
// ═══════════════════════════════════════════════════════════════

/** 缓存时长：5 分钟内复用同一次读取结果（含「读失败」的结果，避免每个弹窗都打一次失败请求） */
const CACHE_TTL_MS = 5 * 60 * 1000

let cachedConfig: PrintBehaviorConfig | null = null
let cachedAt = 0
let inflight: Promise<PrintBehaviorConfig | null> | null = null

/**
 * 读取打印设置（一次会话内带缓存）。
 *
 * 走的是**打印组件专用**端点 `GET /set/print-config/behavior`（不需要 `set:print-config:view`）——
 * 配置必须对**所有会打印的人**生效，不能只对管理员生效；同时避免普通账号拿 403
 * 被 axios 拦截器弹成「没有操作权限」的全局提示。
 *
 * @param force 强制刷新（「打印设置」保存成功后调用，让改动立刻生效）
 * @returns 读到时返回配置；**读失败返回 null**（调用方必须把 null 当作「沿用原有行为」）
 */
export async function loadPrintBehaviorConfig(force = false): Promise<PrintBehaviorConfig | null> {
  const now = Date.now()
  if (!force && cachedAt > 0 && now - cachedAt < CACHE_TTL_MS) {
    return cachedConfig
  }
  if (inflight) return inflight

  inflight = (async (): Promise<PrintBehaviorConfig | null> => {
    try {
      const cfg = await printConfigApi.getBehavior()
      cachedConfig = cfg ?? null
      return cachedConfig
    } catch (e) {
      // 静默降级（纪律 ①）：接口异常/超时 → 本次打印沿用改造前的行为
      console.warn('[打印设置] 读取失败，本次打印沿用原有行为', e)
      cachedConfig = null
      return null
    } finally {
      cachedAt = Date.now()
      inflight = null
    }
  })()

  return inflight
}

/** 作废缓存（「打印设置」页保存成功后调用：配置改动无需等 5 分钟才生效） */
export function invalidatePrintBehaviorCache(): void {
  cachedConfig = null
  cachedAt = 0
}

// ═══════════════════════════════════════════════════════════════
// 二、允许打印草稿 → 草稿单据的打印门控
// ═══════════════════════════════════════════════════════════════

/** 明确的草稿文本标记（大小写不敏感；数值型状态码见下。本库真实用例：`purchase-contract.DRAFT='DRAFT'`、`marketing.draft='draft'`） */
const DRAFT_TEXT_MARKERS = ['draft', '草稿']

/** 状态字段候选名（各模块命名不一，这里只收敛「单据状态」语义的名字） */
const STATUS_KEYS = ['status', 'billStatus', 'docStatus', 'orderStatus', 'auditStatus', 'state']

/** 单号字段候选名：用于确认「这是一张已保存的单据」，而不是主数据/列表数据 */
const DOC_NO_KEYS = ['docNo', 'billNo', 'orderNo', 'documentNo', 'billCode', 'orderCode', 'docNumber']

/** 数值 0 视为草稿的状态字段名（本库口径：OrderStatus.DRAFT=0、借出/零售/商城订单 0=草稿） */
const ZERO_MEANS_DRAFT_KEYS = ['status', 'docStatus', 'billStatus', 'orderStatus']

function isPlainObject(value: unknown): value is Record<string, any> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function firstNonNull(record: Record<string, any>, keys: string[]): unknown {
  for (const key of keys) {
    const value = record[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return undefined
}

function matchesDraftText(value: unknown): boolean {
  if (typeof value !== 'string') return false
  const text = value.trim().toLowerCase()
  return DRAFT_TEXT_MARKERS.includes(text)
}

function looksLikeSavedBill(record: Record<string, any>): boolean {
  if (isPlainObject(record.doc) && firstNonNull(record.doc, DOC_NO_KEYS) !== undefined) return true
  return firstNonNull(record, DOC_NO_KEYS) !== undefined
}

/**
 * 判定单据是否为「草稿」。
 *
 * 判定顺序（前者优先）：
 * 1. 调用方**显式声明**的 `explicit`（组件 prop `isDraft`，布尔才作数）；
 * 2. 数据里的布尔标记 `isDraft === true` / `draft === true`；
 * 3. 状态字段的**文本**标记：`DRAFT` / `draft` / `草稿`（大小写不敏感）；
 * 4. 状态字段的**数值 0**：仅当同时能识别出单号（确认是已保存单据）时才算草稿 ——
 *    本库多个模块以 0 表示草稿（如 `OrderStatus.DRAFT = 0`），但主数据（商品/货位等）的 `status`
 *    另有语义，故必须配合单号识别，避免误拦主数据打印。
 *
 * ⚠️ 风险与兜底：各模块状态字面量并不统一，判定必然存在误差；因此
 * ① 本判定**只在租户显式关闭「允许打印草稿」时才可能拦人**（默认开启，见库默认值）；
 * ② 拦下时给出可读提示，管理员把「允许打印草稿」打开即可恢复，
 *    绝不会出现「打印功能整体不可用」。
 *
 * @param data     传给模板渲染的打印数据（`PrintDialog` 的 `printData`）
 * @param explicit 组件 prop `isDraft`（显式声明，优先级最高）
 */
export function isDraftDocument(data: unknown, explicit?: boolean | null): boolean {
  if (typeof explicit === 'boolean') return explicit
  if (!isPlainObject(data)) return false

  // 打印数据可能直接是单据，也可能包一层 doc（如换货单的 printPayload.doc）
  const candidates: Record<string, any>[] = [data]
  if (isPlainObject(data.doc)) candidates.push(data.doc)

  for (const record of candidates) {
    if (record.isDraft === true || record.draft === true) return true
    for (const key of STATUS_KEYS) {
      if (matchesDraftText(record[key])) return true
    }
  }

  if (looksLikeSavedBill(data)) {
    for (const record of candidates) {
      for (const key of ZERO_MEANS_DRAFT_KEYS) {
        const value = record[key]
        if (value === 0 || value === '0') return true
      }
    }
  }

  return false
}

// ═══════════════════════════════════════════════════════════════
// 三、单据打印小数位数 → 数量 / 单价列格式化
// ═══════════════════════════════════════════════════════════════

/** 数量列字段名（大小写不敏感的包含匹配；中文键一并支持） */
const QTY_KEY_PATTERN = /(qty|quantity|数量)/i

/** 单价列字段名（`unitPrice` / `price` / `salePrice` / `costPrice` … ；中文键一并支持） */
const PRICE_KEY_PATTERN = /(price|单价)/i

/**
 * 金额类字段**永不格式化**（ql361 帮助文案原文：「此设置对有数量/单价字段的业务单据打印有效」，
 * 金额另有口径）——这些键即使名字里同时含 price/qty 也排除：
 * 如 `discountAmount` / `taxAmount` / `costAmount` / `totalAmount`。
 *
 * 注意：这里**只按「金额」语义排除**，不排除 `total`/`tax` 词根 —— 本库真实列里
 * `totalQty`（数量）、`unitPriceWithTax`（含税单价）都属「数量/单价」口径，应当格式化。
 */
const AMOUNT_KEY_PATTERN = /(amount|money|subtotal|金额|合计|小计)/i

/** 支持的最大深度（打印数据是「单据 + 明细数组」的浅结构，超过 4 层不再下钻） */
const MAX_DEPTH = 4

/** 合法的小数位数（与后端 `PrintConfigService.DECIMAL_MIN/MAX` 同口径：0~4） */
function normalizeDigits(value: unknown): number | null {
  const n = Number(value)
  if (!Number.isFinite(n)) return null
  const digits = Math.trunc(n)
  if (digits < 0 || digits > 8) return null
  return digits
}

function isQtyKey(key: string): boolean {
  return QTY_KEY_PATTERN.test(key) && !AMOUNT_KEY_PATTERN.test(key)
}

function isPriceKey(key: string): boolean {
  return PRICE_KEY_PATTERN.test(key) && !AMOUNT_KEY_PATTERN.test(key)
}

/**
 * 按位数格式化一个数值；**非数值原样返回**。
 *
 * 例：`'12.5'` + 2 → `'12.50'`；`'-'` / `'/'` / `''` / null → 原值（不制造 `-0`，见《a-statistic 字符串值陷阱》）。
 */
export function formatDecimal(value: unknown, digits: number): unknown {
  if (value === null || value === undefined || value === '') return value
  if (typeof value === 'boolean') return value
  const num = typeof value === 'number' ? value : Number(String(value).replace(/,/g, '').trim())
  if (!Number.isFinite(num)) return value
  return num.toFixed(digits)
}

/**
 * 深拷贝打印数据。
 *
 * 优先 JSON 往返：渲染接口收到的本就是 `JSON.stringify(printData)`，所以 JSON 往返与「原样传给接口」
 * 语义完全一致（含 `undefined` 字段被丢弃的既有行为）。
 * 兜底顺序：结构化克隆 → 浅拷贝（**绝不返回原对象引用**，避免加工时改到组件 props）。
 */
function deepClone<T>(value: T): T {
  try {
    const text = JSON.stringify(value)
    if (typeof text === 'string') return JSON.parse(text) as T
  } catch { /* 落到结构化克隆 */ }
  try {
    if (typeof structuredClone === 'function') return structuredClone(value)
  } catch { /* 落到浅拷贝 */ }
  if (Array.isArray(value)) return [...value] as unknown as T
  if (value && typeof value === 'object') return { ...(value as object) } as T
  return value
}

/** 深度遍历普通对象/数组（有深度上限与环保护），对每个对象节点执行 visit */
function walkObjects(
  node: unknown,
  visit: (record: Record<string, any>) => void,
  depth = 0,
  seen: WeakSet<object> = new WeakSet(),
): void {
  if (depth > MAX_DEPTH || !node || typeof node !== 'object') return
  if (seen.has(node as object)) return
  seen.add(node as object)

  if (Array.isArray(node)) {
    for (const item of node) walkObjects(item, visit, depth + 1, seen)
    return
  }

  visit(node as Record<string, any>)
  for (const value of Object.values(node as Record<string, any>)) {
    if (value && typeof value === 'object') walkObjects(value, visit, depth + 1, seen)
  }
}

// ═══════════════════════════════════════════════════════════════
// 四、打印内容 → 明细行「批次效期」文本
// ═══════════════════════════════════════════════════════════════

/**
 * 「打印内容」三个选项 → 参与拼接的字段（**下标顺序即拼接顺序**，逐字对应选项文案）。
 *
 * 三个选项文案取自 ql361 实测（`_summary.md` §三 3.1），此处不新增、不改写：
 * `批号 *数量` / `生产日期 *数量` / `批号 生产日期~到期日期 *数量`。
 * 选项的键名 == 值 == 文案（后端 `PRINT_CONTENT_OPTIONS` 同口径）。
 *
 * ⚠️ 用 {@link Map} 而不是普通对象：库里的值来自外部输入，普通对象查
 * `MAP['constructor']`/`MAP['toString']` 会命中原型成员并返回函数（本库踩过的
 * 《JS 原型链键陷阱》，曾写坏 147 个文件），Map 天然只认自己写入的键。
 */
const PRINT_CONTENT_FIELDS = new Map<string, Array<'batchNo' | 'productionDate' | 'productionToExpiry'>>([
  ['批号 *数量', ['batchNo']],
  ['生产日期 *数量', ['productionDate']],
  // 「生产日期~到期日期」是**一个**拼接段（不是两个字段），对应选项文案里的 `生产日期~到期日期`
  ['批号 生产日期~到期日期 *数量', ['batchNo', 'productionToExpiry']],
])

/** 明细行里可能的字段别名（各模块实体命名不完全一致，这里只收敛同义名） */
const ROW_FIELD_ALIASES = {
  batchNo: ['batchNo', 'batchCode', 'batchNumber', 'lotNo', 'lotNumber', '批号'],
  productionDate: ['productionDate', 'produceDate', 'productionTime', 'mfgDate', '生产日期'],
  expiryDate: ['expiryDate', 'expireDate', 'expirationDate', 'validUntil', 'validDate', '到期日期'],
  qty: ['qty', 'quantity', 'billQty', 'outQty', 'inQty', '数量'],
} as const

function readRowField(row: Record<string, any>, aliases: readonly string[]): unknown {
  for (const key of aliases) {
    const value = row[key]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return undefined
}

/**
 * 拼一行明细的「批次效期」文本（口径 = 选项文案：字段按文案顺序用空格连接，末尾 `*` + 数量）。
 *
 * 例（选项 `批号 生产日期~到期日期 *数量`）→ `P20240101 2024-01-01~2025-01-01 *2.00`。
 * 行内没有该选项所需的任何批次/效期字段时返回 null（非批次商品不注入，避免污染渲染数据）。
 */
export function buildBatchEffectiveText(
  row: Record<string, any>,
  printContent: string,
): string | null {
  const fields = PRINT_CONTENT_FIELDS.get(printContent)
  if (!fields) return null

  const parts: string[] = []
  for (const field of fields) {
    if (field === 'productionToExpiry') {
      // 「生产日期~到期日期」整段：两端都在 → `起~止`；只有一端 → 退化为该端（不打出半截波浪号）
      const start = readRowField(row, ROW_FIELD_ALIASES.productionDate)
      const end = readRowField(row, ROW_FIELD_ALIASES.expiryDate)
      if (start !== undefined && end !== undefined) parts.push(`${start}~${end}`)
      else if (start !== undefined) parts.push(String(start))
      else if (end !== undefined) parts.push(String(end))
      continue
    }
    const value = readRowField(row, ROW_FIELD_ALIASES[field])
    if (value !== undefined) parts.push(String(value))
  }

  if (parts.length === 0) return null

  const qty = readRowField(row, ROW_FIELD_ALIASES.qty)
  return parts.join(' ') + (qty !== undefined ? ` *${qty}` : '')
}

/**
 * 往明细行注入派生字段 `batchEffectiveText`（模板用该字段名引用「批次效期」文本）。
 * 只注入这一个新字段，不改动既有字段名，避免与各模块明细实体重名冲突。
 */
function injectRowBatchText(row: Record<string, any>, printContent: string): void {
  const text = buildBatchEffectiveText(row, printContent)
  if (text !== null) row.batchEffectiveText = text
}

// ═══════════════════════════════════════════════════════════════
// 五、对外唯一入口：把打印设置作用到打印数据
// ═══════════════════════════════════════════════════════════════

/**
 * 按打印设置加工传给模板渲染的数据（**返回新对象，不改入参**），随后
 * `PrintDialog` 用加工后的数据调 `/v2/print/format/render` → 预览/本地打印/远程链路看到的都是加工后的内容。
 *
 * 生效项：
 * · 单据打印小数位数（`decimalEnabled` 为真时）→ 数量列按 `qtyDecimal`、单价列按 `priceDecimal` 定长格式化；
 * · 打印内容 → 明细行注入 `batchEffectiveText`（拼接口径见 `buildBatchEffectiveText`），
 *   顶层注入 `printContentText`（选项文案，可供表头引用）。
 *
 * ⚠️ 与「批次效期商品汇总打印」的关系（刻意如此，不是漏改）：ql361 帮助文案说「批次效期」字段是在
 * **开启批次效期汇总打印后**才增加的，而本系统没有汇总行能力（见文件头接线表），
 * 故这里**不拿该开关当判据**：只要「打印内容」取到实测的 3 项之一，就按该口径为每条明细行派生
 * `batchEffectiveText`（逐行口径，不含任何汇总/合并语义）。日后若补上汇总能力，再把开关接上。
 *
 * @param data 打印数据（原样返回 = 未启用任何加工）
 * @param cfg  打印设置；为 null（未读到）时**原样返回**
 */
export function applyPrintBehavior(
  data: Record<string, any>,
  cfg: PrintBehaviorConfig | null,
): Record<string, any> {
  if (!isPlainObject(data) || !cfg) return data

  const qtyDigits = cfg.decimalEnabled ? normalizeDigits(cfg.qtyDecimal) : null
  const priceDigits = cfg.decimalEnabled ? normalizeDigits(cfg.priceDecimal) : null
  const printContent = (cfg.printContent || '').trim()
  const hasContentRule = PRINT_CONTENT_FIELDS.has(printContent)

  if (qtyDigits === null && priceDigits === null && !hasContentRule) return data

  const output = deepClone(data)

  walkObjects(output, (record) => {
    for (const key of Object.keys(record)) {
      const value = record[key]
      if (value === null || value === undefined || typeof value === 'object') continue
      if (qtyDigits !== null && isQtyKey(key)) record[key] = formatDecimal(value, qtyDigits)
      else if (priceDigits !== null && isPriceKey(key)) record[key] = formatDecimal(value, priceDigits)
    }
    // 「打印内容」只作用于**明细行**（有批次/效期字段的行）；根对象是单据头，不注入批次字段
    if (hasContentRule && record !== output) injectRowBatchText(record, printContent)
  })

  if (hasContentRule) output.printContentText = printContent
  return output
}

// ═══════════════════════════════════════════════════════════════
// 六、远程打印（打印链）参数
// ═══════════════════════════════════════════════════════════════

/**
 * 「远程打印」提交的打印链：取该 pageCode 下**已启用**的链路，按后端返回顺序取第一条。
 *
 * <p>后端既有能力：`GET /v2/print/chains?pageCode=xxx`（列表）、
 * `POST /v2/print/tasks/by-chain`（执行，多级步骤 + 打印客户端轮询）。
 * 本函数只做「选一条」，不重复实现链路编排（《功能/模块不重复开发》）。</p>
 */
export function pickActiveChain(records: any[]): any | null {
  if (!Array.isArray(records) || records.length === 0) return null
  const active = records.find(r => String(r?.status || '').toUpperCase() === 'ACTIVE')
  return active || null
}

/** 从打印数据里尽力取单据号（远程打印任务留痕用；取不到则留空，不阻断） */
export function resolveDocumentNo(data: Record<string, any>): string {
  if (!isPlainObject(data)) return ''
  const scope = isPlainObject(data.doc) ? data.doc : data
  const value = firstNonNull(scope, ['orderNo', 'docNo', 'billNo', 'documentNo', 'billCode'])
    ?? firstNonNull(data, ['orderNo', 'docNo', 'billNo', 'documentNo', 'billCode'])
  return value === undefined ? '' : String(value)
}
