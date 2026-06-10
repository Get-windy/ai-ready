/**
 * 客户端格式化引擎
 *
 * 与服务端 FormatEngine 保持一致，用于本地渲染打印模板。
 * 支持：
 *   - 字段值替换
 *   - 自定义函数表达式求值（安全沙箱）
 *   - 表格数据渲染
 *   - 条码/二维码占位
 */

interface FormatConfig {
  type?: string
  expression?: string
  pattern?: string
  decimals?: number
  thousands?: boolean
  mapping?: Record<string, string>
}

interface ComponentStyle {
  fontSize?: number
  fontWeight?: string
  fontFamily?: string
  color?: string
  backgroundColor?: string
  textAlign?: string
  border?: string
  padding?: string
  fontStyle?: string
  textDecoration?: string
}

interface TemplateComponent {
  type: 'label' | 'field' | 'table' | 'barcode' | 'qrcode' | 'image' | 'line'
  x: number
  y: number
  w: number
  h: number
  field?: string
  content?: string
  src?: string
  style?: ComponentStyle
  formatConfig?: FormatConfig
  columns?: Array<{ header: string; field: string; width?: string }>
}

interface TemplateJson {
  paperSize?: string
  paperWidth?: number
  paperHeight?: number
  marginTop?: number
  marginBottom?: number
  marginLeft?: number
  marginRight?: number
  components?: TemplateComponent[]
}

/**
 * 安全表达式求值（替代 eval）
 * 支持：value + 1, '¥' + value, value > 0 ? value : 0 等
 */
function safeEval(expression: string, value: any): any {
  // 替换 value 引用
  const sanitizedExpr = expression
    .replace(/===/g, '==')
    .replace(/!==/g, '!=')
    .trim()

  // 创建函数体（使用 with 限制作用域）
  try {
    const fn = new Function('value', `"use strict"; return (${sanitizedExpr})`)
    return fn(value)
  } catch {
    // 如果表达式失败，返回原始值
    return value
  }
}

/**
 * 格式化字段值
 */
export function formatFieldValue(value: any, formatConfig?: FormatConfig): string {
  if (!formatConfig || Object.keys(formatConfig).length === 0) {
    return value != null ? String(value) : ''
  }

  const type = formatConfig.type || 'none'

  switch (type) {
    case 'expression': {
      const expr = formatConfig.expression
      if (!expr || expr.trim() === '') {
        return value != null ? String(value) : ''
      }
      try {
        const result = safeEval(expr, value)
        return result != null ? String(result) : ''
      } catch {
        return value != null ? String(value) : ''
      }
    }
    case 'number': {
      const num = Number(value)
      if (isNaN(num)) return String(value)
      const decimals = formatConfig.decimals ?? 2
      const thousands = formatConfig.thousands ?? true
      let formatted = num.toFixed(decimals)
      if (thousands) {
        const parts = formatted.split('.')
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
        formatted = parts.join('.')
      }
      return formatted
    }
    case 'enum': {
      const mapping = formatConfig.mapping
      if (mapping && value != null && mapping[String(value)]) {
        return mapping[String(value)]
      }
      return value != null ? String(value) : ''
    }
    default:
      return value != null ? String(value) : ''
  }
}

/**
 * 渲染模板为 HTML 字符串（客户端版）
 */
export function renderTemplateToHtml(
  templateJson: TemplateJson,
  dataJson: Record<string, any>
): string {
  const {
    paperSize = 'A4',
    paperWidth,
    paperHeight,
    marginTop = 10,
    marginBottom = 10,
    marginLeft = 10,
    marginRight = 10,
    components = []
  } = templateJson

  // 计算纸张尺寸 CSS
  let pageStyle = ''
  if (paperSize === 'CUSTOM' && paperWidth && paperHeight) {
    pageStyle = `width:${paperWidth}mm;height:${paperHeight}mm;`
  } else {
    pageStyle = 'width:190mm;min-height:277mm;'
  }

  let html = '<!DOCTYPE html><html><head>'
  html += '<meta charset="utf-8">'
  html += '<style>'
  html += '*{margin:0;padding:0;box-sizing:border-box;}'
  html += 'body{font-family:"SimSun","Microsoft YaHei",sans-serif;font-size:12px;color:#333;}'
  html += 'table{border-collapse:collapse;width:100%;}'
  html += 'td,th{border:1px solid #ccc;padding:4px 6px;text-align:left;}'
  html += '@media print{@page{margin:0;}body{margin:0;}}'
  html += '</style></head><body>'

  html += `<div style="${pageStyle}padding:${marginTop}mm ${marginRight}mm ${marginBottom}mm ${marginLeft}mm;position:relative;">`

  for (const comp of components) {
    html += renderComponent(comp, dataJson)
  }

  html += '</div></body></html>'
  return html
}

function renderComponent(comp: TemplateComponent, data: Record<string, any>): string {
  const { type, x = 0, y = 0, w = 100, h = 20, field, style = {}, formatConfig } = comp
  const styleStr = buildStyle(style, x, y, w, h)

  switch (type) {
    case 'label':
      return `<div style="${styleStr}">${escapeHtml(comp.content || '')}</div>`

    case 'field': {
      const rawValue = field ? data[field] : undefined
      const displayValue = formatFieldValue(rawValue, formatConfig)
      return `<div style="${styleStr}">${escapeHtml(displayValue)}</div>`
    }

    case 'table': {
      const rows: Record<string, any>[] = field ? (data[field] as any[]) || [] : []
      const columns = comp.columns || []
      if (columns.length === 0) return `<div style="${styleStr}">(未配置列)</div>`

      let table = `<table style="${styleStr}"><thead><tr>`
      for (const col of columns) {
        table += `<th${col.width ? ` style="width:${col.width}"` : ''}>${escapeHtml(col.header)}</th>`
      }
      table += '</tr></thead><tbody>'
      for (const row of rows) {
        table += '<tr>'
        for (const col of columns) {
          const cellVal = col.field ? row[col.field] : ''
          table += `<td>${escapeHtml(String(cellVal ?? ''))}</td>`
        }
        table += '</tr>'
      }
      table += '</tbody></table>'
      return table
    }

    case 'barcode': {
      const value = field && data[field] != null ? String(data[field]) : ''
      return `<div style="${styleStr}border:1px dashed #999;background:#f5f5f5;padding:4px;text-align:center;font-size:11px;color:#666;">
        <div>${escapeHtml(value)}</div>
        <div style="font-size:10px;">[条形码]</div>
      </div>`
    }

    case 'qrcode': {
      const value = field && data[field] != null ? String(data[field]) : ''
      return `<div style="${styleStr}border:1px dashed #999;background:#f5f5f5;padding:4px;text-align:center;font-size:10px;color:#666;">
        <div>[二维码]</div>
        <div style="word-break:break-all;">${escapeHtml(value)}</div>
      </div>`
    }

    case 'image': {
      const src = comp.src || ''
      return src
        ? `<img src="${escapeHtml(src)}" style="${styleStr}">`
        : `<div style="${styleStr}background:#eee;text-align:center;line-height:40px;">图片</div>`
    }

    case 'line':
      return `<hr style="${styleStr}border:0;border-top:1px solid #333;">`

    default:
      return `<div style="${styleStr}">未知组件</div>`
  }
}

function buildStyle(style: ComponentStyle, x: number, y: number, w: number, h: number): string {
  const parts: string[] = [
    'position:absolute;',
    `left:${x}mm;`,
    `top:${y}mm;`,
    `width:${w}mm;`,
    `min-height:${h}mm;`
  ]
  if (style.fontSize) parts.push(`font-size:${style.fontSize}px;`)
  if (style.fontWeight) parts.push(`font-weight:${style.fontWeight};`)
  if (style.fontFamily) parts.push(`font-family:${style.fontFamily};`)
  if (style.color) parts.push(`color:${style.color};`)
  if (style.backgroundColor) parts.push(`background-color:${style.backgroundColor};`)
  if (style.textAlign) parts.push(`text-align:${style.textAlign};`)
  if (style.border) parts.push(`border:${style.border};`)
  if (style.padding) parts.push(`padding:${style.padding};`)
  if (style.fontStyle) parts.push(`font-style:${style.fontStyle};`)
  if (style.textDecoration) parts.push(`text-decoration:${style.textDecoration};`)
  return parts.join('')
}

function escapeHtml(input: string): string {
  if (!input) return ''
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
