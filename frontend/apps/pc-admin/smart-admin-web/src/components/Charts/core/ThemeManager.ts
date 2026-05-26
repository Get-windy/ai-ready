/**
 * 企智连数据可视化组件库 - 主题系统
 * Author: AI应用开发工程师 | Date: 2026-04-17
 */

import { ref, computed, watch, type Ref } from 'vue'

// 主题颜色配置
export interface ThemeColors {
  primary: string[]
  background: string
  surface: string
  text: string
  textSecondary: string
  border: string
  grid: string
  tooltip: {
    background: string
    text: string
    border: string
  }
}

// 字体配置
export interface ThemeFont {
  family: string
  size: {
    title: number
    subtitle: number
    axis: number
    legend: number
    tooltip: number
  }
}

// 动画配置
export interface ThemeAnimation {
  enabled: boolean
  duration: number
  easing: 'linear' | 'ease' | 'ease-in' | 'ease-out' | 'ease-in-out'
  delay: number
}

// 完整主题配置
export interface ChartTheme {
  name: string
  colors: ThemeColors
  font: ThemeFont
  animation: ThemeAnimation
}

// 默认主题
export const defaultTheme: ChartTheme = {
  name: 'default',
  colors: {
    primary: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
    background: '#ffffff',
    surface: '#fafafa',
    text: '#333333',
    textSecondary: '#666666',
    border: '#e0e0e0',
    grid: '#e5e5e5',
    tooltip: {
      background: 'rgba(50, 50, 50, 0.9)',
      text: '#ffffff',
      border: '#333333'
    }
  },
  font: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial',
    size: {
      title: 16,
      subtitle: 12,
      axis: 12,
      legend: 12,
      tooltip: 12
    }
  },
  animation: {
    enabled: true,
    duration: 800,
    easing: 'ease-out',
    delay: 100
  }
}

// 暗色主题
export const darkTheme: ChartTheme = {
  name: 'dark',
  colors: {
    primary: ['#4992ff', '#7cffb2', '#fddd60', '#ff6e76', '#58d9f9', '#05c091', '#ff8a45', '#8d48e3'],
    background: '#1a1a2e',
    surface: '#16213e',
    text: '#e0e0e0',
    textSecondary: '#a0a0a0',
    border: '#2a2a4a',
    grid: '#2a2a4a',
    tooltip: {
      background: 'rgba(30, 30, 50, 0.95)',
      text: '#e0e0e0',
      border: '#3a3a5a'
    }
  },
  font: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial',
    size: {
      title: 16,
      subtitle: 12,
      axis: 12,
      legend: 12,
      tooltip: 12
    }
  },
  animation: {
    enabled: true,
    duration: 800,
    easing: 'ease-out',
    delay: 100
  }
}

// 复古主题
export const vintageTheme: ChartTheme = {
  name: 'vintage',
  colors: {
    primary: ['#d87c7c', '#919e8b', '#d7ab82', '#6e7074', '#61a0a8', '#efa18d', '#787464', '#cc7e63'],
    background: '#fef8ef',
    surface: '#fffbf0',
    text: '#4a4a4a',
    textSecondary: '#7a7a7a',
    border: '#e0d5c5',
    grid: '#e8e0d5',
    tooltip: {
      background: 'rgba(80, 70, 60, 0.9)',
      text: '#fef8ef',
      border: '#6a5a4a'
    }
  },
  font: {
    family: 'Georgia, "Times New Roman", serif',
    size: {
      title: 18,
      subtitle: 14,
      axis: 12,
      legend: 12,
      tooltip: 12
    }
  },
  animation: {
    enabled: true,
    duration: 1000,
    easing: 'ease-in-out',
    delay: 150
  }
}

// 彩色主题
export const colorfulTheme: ChartTheme = {
  name: 'colorful',
  colors: {
    primary: ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#f0932b', '#eb4d4b', '#6ab04c', '#c7ecee'],
    background: '#ffffff',
    surface: '#f8f9fa',
    text: '#2d3436',
    textSecondary: '#636e72',
    border: '#dfe6e9',
    grid: '#dfe6e9',
    tooltip: {
      background: 'rgba(45, 52, 54, 0.95)',
      text: '#ffffff',
      border: '#2d3436'
    }
  },
  font: {
    family: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    size: {
      title: 16,
      subtitle: 12,
      axis: 12,
      legend: 12,
      tooltip: 12
    }
  },
  animation: {
    enabled: true,
    duration: 600,
    easing: 'ease-out',
    delay: 80
  }
}

// 企业主题
export const corporateTheme: ChartTheme = {
  name: 'corporate',
  colors: {
    primary: ['#1e3a8a', '#3b82f6', '#60a5fa', '#93c5fd', '#2563eb', '#1d4ed8', '#1e40af', '#1e3a8a'],
    background: '#ffffff',
    surface: '#f8fafc',
    text: '#1e293b',
    textSecondary: '#64748b',
    border: '#e2e8f0',
    grid: '#f1f5f9',
    tooltip: {
      background: 'rgba(30, 41, 59, 0.95)',
      text: '#ffffff',
      border: '#334155'
    }
  },
  font: {
    family: '"Inter", -apple-system, BlinkMacSystemFont, sans-serif',
    size: {
      title: 16,
      subtitle: 12,
      axis: 11,
      legend: 11,
      tooltip: 11
    }
  },
  animation: {
    enabled: true,
    duration: 500,
    easing: 'ease-out',
    delay: 50
  }
}

// 主题集合
export const themes: Record<string, ChartTheme> = {
  default: defaultTheme,
  dark: darkTheme,
  vintage: vintageTheme,
  colorful: colorfulTheme,
  corporate: corporateTheme
}

// 获取主题
export function getTheme(name: string): ChartTheme {
  return themes[name] || defaultTheme
}

// 主题管理器 Composable
export function useChartTheme(initialTheme: string = 'default') {
  const currentThemeName = ref(initialTheme)
  const currentTheme = computed(() => getTheme(currentThemeName.value))
  
  // 设置主题
  const setTheme = (name: string) => {
    if (themes[name]) {
      currentThemeName.value = name
    } else {
      console.warn(`Theme "${name}" not found, using default`)
      currentThemeName.value = 'default'
    }
  }
  
  // 获取CSS变量
  const getThemeCSS = computed(() => {
    const theme = currentTheme.value
    return {
      '--chart-bg': theme.colors.background,
      '--chart-surface': theme.colors.surface,
      '--chart-text': theme.colors.text,
      '--chart-text-secondary': theme.colors.textSecondary,
      '--chart-border': theme.colors.border,
      '--chart-grid': theme.colors.grid,
      '--chart-font-family': theme.font.family
    }
  })
  
  // 应用主题到元素
  const applyTheme = (el: HTMLElement) => {
    const css = getThemeCSS.value
    Object.entries(css).forEach(([key, value]) => {
      el.style.setProperty(key, value)
    })
  }
  
  // 获取颜色
  const getColor = (index: number): string => {
    const colors = currentTheme.value.colors.primary
    return colors[index % colors.length]
  }
  
  // 获取动画配置
  const getAnimation = () => currentTheme.value.animation
  
  return {
    currentThemeName,
    currentTheme,
    setTheme,
    getThemeCSS,
    applyTheme,
    getColor,
    getAnimation,
    themes: Object.keys(themes)
  }
}

// 全局主题管理器（单例）
let globalThemeManager: ReturnType<typeof useChartTheme> | null = null

export function getGlobalThemeManager(): ReturnType<typeof useChartTheme> {
  if (!globalThemeManager) {
    globalThemeManager = useChartTheme()
  }
  return globalThemeManager
}

export function setGlobalTheme(themeName: string): void {
  const manager = getGlobalThemeManager()
  manager.setTheme(themeName)
}
