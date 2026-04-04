import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'
import jaJP from './locales/ja-JP'
import koKR from './locales/ko-KR'

// 支持的语言类型
export type LocaleCode = 'zh-CN' | 'en-US' | 'ja-JP' | 'ko-KR'

// 语言配置
export interface LocaleConfig {
  value: LocaleCode
  label: string
  labelNative: string  // 本地语言显示名称
  flag: string
  lang: string  // HTML lang属性值
}

// 语言配置列表
export const localeConfigs: LocaleConfig[] = [
  { value: 'zh-CN', label: '简体中文', labelNative: '简体中文', flag: '🇨🇳', lang: 'zh-CN' },
  { value: 'en-US', label: 'English', labelNative: 'English', flag: '🇺🇸', lang: 'en' },
  { value: 'ja-JP', label: '日本語', labelNative: '日本語', flag: '🇯🇵', lang: 'ja' },
  { value: 'ko-KR', label: '한국어', labelNative: '한국어', flag: '🇰🇷', lang: 'ko' }
]

// 浏览器语言到应用语言的映射
const browserLangMap: Record<string, LocaleCode> = {
  'zh': 'zh-CN',
  'zh-cn': 'zh-CN',
  'zh-tw': 'zh-CN',
  'zh-hk': 'zh-CN',
  'en': 'en-US',
  'en-us': 'en-US',
  'en-gb': 'en-US',
  'ja': 'ja-JP',
  'ja-jp': 'ja-JP',
  'ko': 'ko-KR',
  'ko-kr': 'ko-KR'
}

// 获取存储的语言或浏览器语言（增强版自动检测）
export function getDefaultLocale(): LocaleCode {
  // 1. 优先从本地存储获取
  const stored = localStorage.getItem('locale')
  if (stored && isValidLocale(stored)) {
    return stored as LocaleCode
  }
  
  // 2. 检测浏览器语言
  const browserLang = navigator.language.toLowerCase()
  
  // 精确匹配
  if (browserLangMap[browserLang]) {
    return browserLangMap[browserLang]
  }
  
  // 前缀匹配（如 'zh-Hans' -> 'zh-CN'）
  const prefix = browserLang.split('-')[0]
  if (browserLangMap[prefix]) {
    return browserLangMap[prefix]
  }
  
  // 3. 检测 URL 参数
  const urlParams = new URLSearchParams(window.location.search)
  const urlLocale = urlParams.get('lang')
  if (urlLocale && isValidLocale(urlLocale)) {
    return urlLocale as LocaleCode
  }
  
  // 4. 默认语言
  return 'zh-CN'
}

// 验证语言是否支持
function isValidLocale(locale: string): boolean {
  return localeConfigs.some(config => config.value === locale)
}

// 获取语言信息
export function getLocaleInfo(locale: LocaleCode): LocaleConfig | undefined {
  return localeConfigs.find(config => config.value === locale)
}

// 获取支持的语言列表（用于下拉选择）
export function getSupportedLocales(): { code: LocaleCode; name: string; icon: string }[] {
  return localeConfigs.map(config => ({
    code: config.value,
    name: config.labelNative,
    icon: config.flag
  }))
}

// 创建 i18n 实例
const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  globalInjection: true, // 全局注入 $t
  locale: getDefaultLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'ja-JP': jaJP,
    'ko-KR': koKR
  },
  datetimeFormats: {
    'zh-CN': {
      short: { year: 'numeric', month: 'short', day: 'numeric' },
      long: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' },
      full: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    },
    'en-US': {
      short: { year: 'numeric', month: 'short', day: 'numeric' },
      long: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' },
      full: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    },
    'ja-JP': {
      short: { year: 'numeric', month: 'short', day: 'numeric' },
      long: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' },
      full: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    },
    'ko-KR': {
      short: { year: 'numeric', month: 'short', day: 'numeric' },
      long: { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' },
      full: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    }
  },
  numberFormats: {
    'zh-CN': {
      currency: { style: 'currency', currency: 'CNY' },
      decimal: { style: 'decimal', minimumFractionDigits: 2 },
      percent: { style: 'percent', useGrouping: false }
    },
    'en-US': {
      currency: { style: 'currency', currency: 'USD' },
      decimal: { style: 'decimal', minimumFractionDigits: 2 },
      percent: { style: 'percent', useGrouping: false }
    },
    'ja-JP': {
      currency: { style: 'currency', currency: 'JPY' },
      decimal: { style: 'decimal', minimumFractionDigits: 2 },
      percent: { style: 'percent', useGrouping: false }
    },
    'ko-KR': {
      currency: { style: 'currency', currency: 'KRW' },
      decimal: { style: 'decimal', minimumFractionDigits: 2 },
      percent: { style: 'percent', useGrouping: false }
    }
  }
})

// 更新 HTML lang 属性（SEO 优化）
function updateHtmlLang(locale: LocaleCode) {
  const config = getLocaleInfo(locale)
  if (config) {
    document.documentElement.lang = config.lang
    document.documentElement.setAttribute('xml:lang', config.lang)
  }
}

// 更新 Meta 标签（SEO 优化）
function updateSeoMeta(locale: LocaleCode) {
  const config = getLocaleInfo(locale)
  if (!config) return
  
  // 更新或创建og:locale meta标签
  let ogLocale = document.querySelector('meta[property="og:locale"]')
  if (!ogLocale) {
    ogLocale = document.createElement('meta')
    ogLocale.setProperty('property', 'og:locale')
    document.head.appendChild(ogLocale)
  }
  ogLocale.setAttribute('content', config.lang)
}

// 获取当前语言
export function getCurrentLocale(): LocaleCode {
  return i18n.global.locale.value as LocaleCode
}

// 切换语言（带动画支持）
export async function setI18nLanguage(locale: LocaleCode): Promise<void> {
  if (!isValidLocale(locale)) {
    console.warn(`Unsupported locale: ${locale}`)
    return
  }
  
  // 触发语言切换前事件
  window.dispatchEvent(new CustomEvent('locale:before-change', { detail: { locale } }))
  
  // 设置语言
  i18n.global.locale.value = locale
  
  // 保存到本地存储
  localStorage.setItem('locale', locale)
  
  // 更新 HTML lang 属性（SEO）
  updateHtmlLang(locale)
  updateSeoMeta(locale)
  
  // 更新 URL 参数（可选）
  updateUrlLocale(locale)
  
  // 触发语言切换后事件
  window.dispatchEvent(new CustomEvent('locale:changed', { detail: { locale } }))
}

// 更新 URL 参数
function updateUrlLocale(locale: LocaleCode) {
  const url = new URL(window.location.href)
  if (locale !== 'zh-CN') {  // 默认语言不显示在URL中
    url.searchParams.set('lang', locale)
  } else {
    url.searchParams.delete('lang')
  }
  window.history.replaceState({}, '', url.toString())
}

// 切换语言（兼容旧API）
export function setLocale(locale: string) {
  setI18nLanguage(locale as LocaleCode)
}

// 获取当前语言（兼容旧API）
export function getLocale(): string {
  return i18n.global.locale.value
}

// 语言列表（兼容旧API）
export const locales = localeConfigs.map(config => ({
  value: config.value,
  label: config.label,
  flag: config.flag
}))

// 设置 i18n
export async function setupI18n() {
  // 初始化 SEO 标签
  const currentLocale = getCurrentLocale()
  updateHtmlLang(currentLocale)
  updateSeoMeta(currentLocale)
  
  return i18n
}

// 导出默认实例
export default i18n

// 导出类型
export type { Ref } from 'vue'

/**
 * 国际化 SEO 优化工具
 */
export const seoUtils = {
  // 生成 hreflang 链接（多语言SEO）
  generateHreflang(locale: LocaleCode): string {
    const config = getLocaleInfo(locale)
    if (!config) return ''
    return `<link rel="alternate" hreflang="${config.lang}" href="${window.location.origin}${window.location.pathname}?lang=${locale}" />`
  },
  
  // 生成所有 hreflang 标签
  generateAllHreflangs(): string {
    return localeConfigs.map(config => 
      `<link rel="alternate" hreflang="${config.lang}" href="${window.location.origin}${window.location.pathname}?lang=${config.value}" />`
    ).join('\n')
  },
  
  // 更新页面标题（国际化）
  updateTitle(title: string, locale: LocaleCode) {
    const config = getLocaleInfo(locale)
    document.title = `${title} | AI-Ready`
  }
}