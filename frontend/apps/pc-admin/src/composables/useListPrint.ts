/**
 * 结果集打印：列表 / 查询 / 报表页的「打印我正在看的这批数据」。
 *
 * 页面只要把**自己的列定义与行数据**交出来；模板查询、渲染、预览、份数、
 * 本地/远程打印都由 `PrintDialog` 负责。
 *
 * ```ts
 * const { printDialogRef, printData, handlePrint } = useListPrint({
 *   pageCode: 'purchase-price-track',   // 不含 `/`，它是路径段
 *   title: '采购价格跟踪',
 *   rows: () => visibleTableData.value,
 *   // 可选：合计口径与明细行不同时给一行文字（模板 pageFooter 会打印它）
 *   totalText: () => `共 ${tableData.value.length} 条，金额合计 ${fmt(sum)}`,
 * })
 * ```
 * ```vue
 * <PrintDialog ref="printDialogRef" page-code="purchase-price-track" :print-data="printData" />
 * ```
 *
 * ## 两条与业界对齐的规矩（2026-09-27 定）
 *
 * **① 合计由模板声明、引擎在数据集上算**（Jasper 的 summary band、SSRS 的 `=Sum(Fields!x.Value)`、
 * Odoo QWeb 的 `t-set` 累加都是这个路子）。所以这里**不再把数值列格式化**：列定义里带
 * `agg`/数值语义的字段原样传数值，由模板列的 `digits`/`formatConfig` 负责显示 ——
 * 一旦在前端格式化成字符串，模板的 `agg: "sum"` 就废了（Odoo 社区踩过的经典坑：
 * 累加值成了字符串拼接 `"4644.95$ 557.39"`）。口径特殊的合计走 `totalText`。
 *
 * **② 列默认由模板说了算**（Odoo：模板决定外观、动作决定分发；SAP B1 的打印布局与屏幕解耦；
 * Odoo 想导出「跟屏幕一致」用的是 Export，不是 Print）。所以默认**不发 `columns`**，
 * 引擎用模板里写死的那份列清单；只有当页面显式声明 `useDataColumns: true`
 * （确实要打「用户当前列配置」的查询页）时才把列发过去覆盖模板。
 *
 * ⚠️ `pageCode` 与 `sys_print_template.page_code`、`<PrintDialog page-code>` 三处一致，
 * 且不能含 `/`（会被编成 %2F，Tomcat 判 400）。
 */
import { ref } from 'vue'
import { message } from 'ant-design-vue'

export interface ListPrintColumn {
  key: string
  title: string
  align?: 'left' | 'center' | 'right'
}

/** 单元格格式化函数（与表格同一套：`formatter(value, record)`） */
type CellFormatter = (value: any, record: any) => any

export interface UseListPrintOptions {
  /** 页面编码（= 模板键），不能含 `/` */
  pageCode: string
  /** 打印标题（覆盖模板的 title 字段） */
  title?: string | (() => string)
  /** 全部行（当前筛选结果） */
  rows: () => Array<Record<string, any>>
  /** 勾选行；给了且非空时优先打勾选的 */
  selectedRows?: () => Array<Record<string, any>>
  /**
   * 页面列定义。**只在 `useDataColumns: true` 时才发给模板**（默认模板说了算）。
   */
  columns?: () => any[]
  /** 显式要求「按用户当前列配置打」——会覆盖模板里写死的列清单 */
  useDataColumns?: boolean
  /** 一行合计/说明文字，直接进模板 pageFooter 的 `totalText`（口径特殊时用） */
  totalText?: string | (() => string)
  /** 没数据时的提示语 */
  emptyTip?: string
}

/** 这些列型是给人点/给眼睛看的，上纸没意义 */
const SKIP_TYPES = new Set(['rowNo', 'seq', 'checkbox', 'select', 'action', 'expand', 'index'])

/** 数值语义的字段（原样传值，交给模板格式化，才能参与 agg: sum） */
const NUMERIC_KEY = /(quantity|qty|amount|price|money|rate|rate|weight|volume|stock|balance|debt|fee|total)/i

/** 从页面列定义里筛出可打印的列（含各自的 formatter） */
export function toPrintColumns(columns: any[]): Array<ListPrintColumn & { formatter?: CellFormatter }> {
  const out: Array<ListPrintColumn & { formatter?: CellFormatter }> = []
  for (const col of columns || []) {
    if (!col) continue
    const key = col.key || col.field
    if (!key || !col.title) continue
    if (SKIP_TYPES.has(String(col.type))) continue
    if (col.defaultHidden) continue
    // 图片列（缩略图/占位）打了也没内容
    if (String(col.type) === 'slot' && /image|图片|photo/i.test(`${col.key || ''}${col.title}`)) continue
    out.push({
      key: String(key),
      title: String(col.title),
      align: col.align,
      formatter: typeof col.formatter === 'function' ? col.formatter : undefined,
    })
  }
  return out
}

export function useListPrint(options: UseListPrintOptions) {
  const printDialogRef = ref<any>(null)
  const printData = ref<Record<string, any>>({})

  function handlePrint() {
    const selected = options.selectedRows?.() || []
    const source = selected.length ? selected : options.rows()
    if (!source.length) {
      message.warning(options.emptyTip || '暂无可打印的数据')
      return
    }

    const payload: Record<string, any> = {
      rows: source.map(record => normalizeRow(record, options)),
      printTime: new Date().toLocaleString('zh-CN'),
    }
    if (options.title) {
      payload.title = typeof options.title === 'function' ? options.title() : options.title
    }
    if (options.totalText) {
      payload.totalText = typeof options.totalText === 'function' ? options.totalText() : options.totalText
    }
    if (options.useDataColumns && options.columns) {
      const cols = toPrintColumns(options.columns())
      if (!cols.length) {
        message.warning('没有可打印的列')
        return
      }
      payload.columns = cols.map(({ key, title, align }) => ({ key, title, align }))
    }
    printData.value = payload
    printDialogRef.value?.open?.()
  }

  /**
   * 行数据处理：**数值列原样留着**，展示列才过 formatter。
   *
   * 数值列判断看两处：列定义里有没有 `agg`/数值语义的 key。格式化后的字符串没法再求和，
   * 而模板的合计（`agg: "sum"`）就是靠这批原始数值算的。
   */
  function normalizeRow(record: Record<string, any>, opts: UseListPrintOptions) {
    const cols = opts.columns ? toPrintColumns(opts.columns()) : []
    if (!cols.length) return { ...record }
    const out: Record<string, any> = {}
    for (const col of cols) {
      const value = record[col.key]
      const numeric = NUMERIC_KEY.test(col.key)
      out[col.key] = numeric || !col.formatter ? value : col.formatter(value, record)
    }
    return out
  }

  return { printDialogRef, printData, handlePrint }
}
