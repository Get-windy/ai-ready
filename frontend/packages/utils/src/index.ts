/**
 * @ai-ready/utils
 * 共享工具函数库
 */

/** 格式化金额 */
export function formatCurrency(value: number, currency: string = 'CNY'): string {
  const locale = currency === 'CNY' ? 'zh-CN' : 'en-US'
  return new Intl.NumberFormat(locale, {
    style: 'currency', currency,
  }).format(value)
}

/** 格式化日期 */
export function formatDate(date: Date | string | number, fmt: string = 'YYYY-MM-DD'): string {
  const d = new Date(date)
  const map: Record<string, string> = {
    YYYY: String(d.getFullYear()),
    MM: String(d.getMonth() + 1).padStart(2, '0'),
    DD: String(d.getDate()).padStart(2, '0'),
    HH: String(d.getHours()).padStart(2, '0'),
    mm: String(d.getMinutes()).padStart(2, '0'),
    ss: String(d.getSeconds()).padStart(2, '0'),
  }
  return fmt.replace(/YYYY|MM|DD|HH|mm|ss/g, (m) => map[m] || m)
}

/** 截断文本 */
export function truncateText(text: string, max: number, suffix: string = '...'): string {
  return text.length <= max ? text : text.slice(0, max - suffix.length) + suffix
}

/** 复制到剪贴板 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard) { await navigator.clipboard.writeText(text); return true }
    const ta = document.createElement('textarea')
    ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0'
    document.body.appendChild(ta); ta.select(); document.execCommand('copy'); document.body.removeChild(ta)
    return true
  } catch { return false }
}

/** 格式化文件大小 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1) + ' ' + ['B', 'KB', 'MB', 'GB'][i]
}

/** 是否外部链接 */
export function isExternalLink(url: string): boolean { return /^(https?:)?\/\//.test(url) }

/** 随机 ID */
export function randomId(len: number = 8): string { return Math.random().toString(36).substring(2, 2 + len) }

/** 防抖 */
export function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout>
  return (...args) => { clearTimeout(timer); timer = setTimeout(() => fn(...args), delay) }
}

/** 节流 */
export function throttle<T extends (...args: any[]) => any>(fn: T, limit: number): (...args: Parameters<T>) => void {
  let inThrottle = false
  return (...args) => { if (!inThrottle) { fn(...args); inThrottle = true; setTimeout(() => { inThrottle = false }, limit) } }
}

/** 获取嵌套对象值 */
export function get(obj: any, path: string, def?: any): any {
  return path.replace(/\[(\d+)\]/g, '.$1').split('.').reduce((o, k) => o?.[k] ?? def, obj)
}
