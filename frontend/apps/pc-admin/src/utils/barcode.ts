/**
 * Code128-B 条码编码（零依赖，用于条码打印真实渲染可扫描条码）
 *
 * 返回条与空的宽度序列，调用方按需渲染 SVG。
 * 说明：Code128-B 覆盖 ASCII 32~126；非 ASCII 字符（中文等）无法编码，
 * 此时由 {@link encodeCode128B} 返回 null，界面回退为文本展示。
 */

/** CODE128 标准编码表（索引 0~106，106 为停止符，7 位宽） */
const CODE128_PATTERNS = [
  '212222', '222122', '222221', '121223', '121322', '131222', '122213', '122312', '132212', '221213',
  '221312', '231212', '112232', '122132', '122231', '113222', '123122', '123221', '223211', '221132',
  '221231', '213212', '223112', '312131', '311222', '321122', '321221', '312212', '322112', '322211',
  '212123', '212321', '232121', '111323', '131123', '131321', '112313', '132113', '132311', '211313',
  '231113', '231311', '112133', '112331', '132131', '113123', '113321', '133121', '313121', '211331',
  '231131', '213113', '213311', '213131', '311123', '311321', '331121', '312113', '312311', '332111',
  '314111', '221411', '431111', '111224', '111422', '121124', '121421', '141122', '141221', '112214',
  '112412', '122114', '122411', '142112', '142211', '241211', '221114', '413111', '241112', '134111',
  '111242', '121142', '121241', '114212', '124112', '124211', '411212', '421112', '421211', '212141',
  '214121', '412121', '111143', '111341', '131141', '114113', '114311', '411113', '411311', '113141',
  '114131', '311141', '411131', '211412', '211214', '211232', '2331112'
]

const START_B = 104
const STOP = 106

export interface BarcodeBar {
  /** 起始位置（模块数） */
  x: number
  /** 宽度（模块数） */
  width: number
}

export interface Code128Result {
  /** 黑条矩形（已换算为模块坐标） */
  bars: BarcodeBar[]
  /** 总模块数（渲染时乘以模块宽度即为像素宽） */
  modules: number
}

/**
 * 编码为 Code128-B，返回黑条矩形坐标。
 * 含非 ASCII 字符或为空时返回 null。
 */
export function encodeCode128B(text: string | null | undefined): Code128Result | null {
  const value = (text ?? '').trim()
  if (!value) return null
  for (const ch of value) {
    const code = ch.charCodeAt(0)
    if (code < 32 || code > 126) return null
  }

  const values = [START_B]
  for (const ch of value) values.push(ch.charCodeAt(0) - 32)

  // 校验位 = (起始符 + Σ(数据符值 × 位置)) % 103，位置自 1 起
  let sum = values[0]
  for (let i = 1; i < values.length; i++) {
    sum += values[i] * i
  }
  const codes = [...values, sum % 103, STOP]

  const bars: BarcodeBar[] = []
  let x = 0
  for (const code of codes) {
    const pattern = CODE128_PATTERNS[code]
    for (let i = 0; i < pattern.length; i++) {
      const width = pattern.charCodeAt(i) - 48
      if (i % 2 === 0) {
        bars.push({ x, width })
      }
      x += width
    }
  }
  return { bars, modules: x }
}

/** 生成条码 SVG 字符串（用于打印窗口） */
export function renderBarcodeSvg(text: string, moduleWidth = 1.6, height = 48): string {
  const encoded = encodeCode128B(text)
  if (!encoded) return ''
  const width = encoded.modules * moduleWidth
  const rects = encoded.bars
    .map(bar => `<rect x="${(bar.x * moduleWidth).toFixed(2)}" y="0" width="${(bar.width * moduleWidth).toFixed(2)}" height="${height}"/>`)
    .join('')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width.toFixed(2)}" height="${height}" viewBox="0 0 ${width.toFixed(2)} ${height}"><g fill="#000">${rects}</g></svg>`
}
