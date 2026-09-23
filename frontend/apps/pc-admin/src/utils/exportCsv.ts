/**
 * CSV 导出通用工具
 * 统一处理 BOM、CSV 生成、Blob 下载
 */

/**
 * 导出数据为 CSV 文件
 * @param headers 表头数组
 * @param rows 数据行数组（每个元素与表头一一对应）
 * @param filename 文件名（不含扩展名）
 */
export function exportCsv(
  headers: string[],
  rows: (string | number)[][],
  filename: string
): void {
  const csvContent = [
    headers.join(','),
    ...rows.map(r => r.map(v => `"${v}"`).join(','))
  ].join('\n')

  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 按列表页列配置导出 CSV（列定义 → 表头 + 数据行）。
 *
 * 用途：列表页「导出」按钮。行为约定与各页数据表保持一致——
 * 跳过不可导出的列（序号/选择框/操作/图片/插槽列），有 formatter 的走 formatter。
 *
 * 为什么放在这里：采购退货/入库/换货三个列表页原先各自写了「只弹提示、不产出文件」的假导出
 * （2026-09-22 审计 P0），另外 5 个页面又各写了一遍几乎相同的 CSV 拼装。
 * 统一到本函数，后续同类页面直接复用，不要再各写一份。
 */
export function exportCsvFromColumns(
  columns: Array<{ key?: string; title?: string; type?: string; formatter?: (v: any) => any }>,
  rows: any[],
  filename: string,
  resolvers?: Record<string, (row: any) => any>
): number {
  const exportCols = columns.filter(
    c => c.key && c.title && !['rowNo', 'checkbox', 'action', 'image'].includes(c.type || '')
  )
  const headers = exportCols.map(c => c.title as string)
  const body = rows.map(row =>
    exportCols.map(col => {
      const key = col.key as string
      // 插槽列的显示文本由页面的 slot 决定，导出时用 resolver 兜底（如 status → statusDesc）
      const display = resolvers?.[key]
        ? resolvers[key](row)
        : col.formatter ? col.formatter(row[key]) : row[key]
      return display === null || display === undefined ? '' : String(display)
    })
  )
  exportCsv(headers, body, filename)
  return body.length
}

/**
 * 带 loading 消息的 CSV 导出
 */
export async function exportCsvWithLoading(
  headers: string[],
  rows: (string | number)[][],
  filename: string
): Promise<void> {
  const { message } = await import('ant-design-vue')
  const hide = message.loading('正在生成导出文件...', 0)
  try {
    exportCsv(headers, rows, filename)
    hide()
    message.success('导出成功')
  } catch {
    hide()
    message.error('导出失败')
  }
}
