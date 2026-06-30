import { pinyin } from 'pinyin-pro'

/**
 * 将中文文本转换为拼音首字母助记码
 * 例如: "阿里巴巴" → "ALBB"
 */
export function toMnemonicCode(text: string): string {
  if (!text) return ''
  const initials = pinyin(text, { pattern: 'first', toneType: 'none', type: 'array' })
  return initials.join('').toUpperCase()
}
