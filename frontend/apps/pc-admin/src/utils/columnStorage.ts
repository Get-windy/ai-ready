/**
 * 表格列配置存储工具
 * 将用户自定义的列显示/隐藏/排序等配置持久化到 localStorage
 */
const STORAGE_PREFIX = 'table-column-config:'

export interface ColumnConfig {
  dataIndex: string
  width?: number | string
  fixed?: 'left' | 'right' | false
  sortOrder?: number
  hidden: boolean
}

export function saveColumnConfig(tableKey: string, configs: ColumnConfig[]) {
  try {
    localStorage.setItem(STORAGE_PREFIX + tableKey, JSON.stringify(configs))
  } catch {
    // localStorage 已满等异常忽略
  }
}

export function loadColumnConfig(tableKey: string): ColumnConfig[] | null {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + tableKey)
    if (raw) return JSON.parse(raw)
  } catch {
    // 解析失败忽略
  }
  return null
}

export function clearColumnConfig(tableKey: string) {
  localStorage.removeItem(STORAGE_PREFIX + tableKey)
}
