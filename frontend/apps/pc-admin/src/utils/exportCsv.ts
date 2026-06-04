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
