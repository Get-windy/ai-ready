import { ref, watch, onMounted } from 'vue'

type Theme = 'light' | 'dark' | 'auto'

interface ThemeConfig {
  theme: Theme
  followSystem: boolean
}

const STORAGE_KEY = 'dashboard_theme_config'

export function useTheme() {
  const theme = ref<Theme>('light')
  const followSystem = ref(true)
  const isDark = ref(false)

  // Check if system prefers dark mode
  const getSystemPreference = (): boolean => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }

  // Apply theme to document
  const applyTheme = (isDarkMode: boolean) => {
    isDark.value = isDarkMode
    const html = document.documentElement
    
    if (isDarkMode) {
      html.setAttribute('data-theme', 'dark')
      html.classList.add('dark')
      html.classList.remove('light')
    } else {
      html.setAttribute('data-theme', 'light')
      html.classList.add('light')
      html.classList.remove('dark')
    }

    // Dispatch custom event for components to react
    window.dispatchEvent(new CustomEvent('theme-change', { 
      detail: { isDark: isDarkMode }
    }))
  }

  // Update theme based on current settings
  const updateTheme = () => {
    if (followSystem.value) {
      applyTheme(getSystemPreference())
    } else {
      applyTheme(theme.value === 'dark')
    }
  }

  // Set theme manually
  const setTheme = (newTheme: Theme) => {
    theme.value = newTheme
    followSystem.value = newTheme === 'auto'
    updateTheme()
    saveConfig()
  }

  // Toggle between light and dark
  const toggleTheme = () => {
    if (followSystem.value) {
      // If following system, switch to manual with opposite of current
      theme.value = isDark.value ? 'light' : 'dark'
      followSystem.value = false
    } else {
      theme.value = theme.value === 'light' ? 'dark' : 'light'
    }
    updateTheme()
    saveConfig()
  }

  // Save configuration to localStorage
  const saveConfig = () => {
    try {
      const config: ThemeConfig = {
        theme: theme.value,
        followSystem: followSystem.value
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(config))
    } catch (e) {
      console.warn('Failed to save theme config:', e)
    }
  }

  // Load configuration from localStorage
  const loadConfig = () => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        const config: ThemeConfig = JSON.parse(saved)
        theme.value = config.theme
        followSystem.value = config.followSystem
      }
    } catch (e) {
      console.warn('Failed to load theme config:', e)
    }
    updateTheme()
  }

  // Listen for system theme changes
  const setupSystemListener = () => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', (e) => {
      if (followSystem.value) {
        applyTheme(e.matches)
      }
    })
  }

  onMounted(() => {
    loadConfig()
    setupSystemListener()
  })

  return {
    theme,
    isDark,
    followSystem,
    setTheme,
    toggleTheme,
    updateTheme,
    applyTheme
  }
}

// CSS Variables for theming
export const themeCssVariables = {
  light: {
    '--bg-primary': '#ffffff',
    '--bg-secondary': '#f5f7fa',
    '--bg-tertiary': '#e4e7ed',
    '--text-primary': '#303133',
    '--text-secondary': '#606266',
    '--text-tertiary': '#909399',
    '--border-color': '#dcdfe6',
    '--border-light': '#e4e7ed',
    '--primary-color': '#409eff',
    '--success-color': '#67c23a',
    '--warning-color': '#e6a23c',
    '--danger-color': '#f56c6c',
    '--info-color': '#909399',
    '--shadow-color': 'rgba(0, 0, 0, 0.1)',
    '--overlay-bg': 'rgba(0, 0, 0, 0.5)'
  },
  dark: {
    '--bg-primary': '#1a1a2e',
    '--bg-secondary': '#16213e',
    '--bg-tertiary': '#0f3460',
    '--text-primary': '#e8e8e8',
    '--text-secondary': '#b0b0b0',
    '--text-tertiary': '#808080',
    '--border-color': '#434343',
    '--border-light': '#2a2a3e',
    '--primary-color': '#3c9ae8',
    '--success-color': '#85ce61',
    '--warning-color': '#ebb563',
    '--danger-color': '#f78989',
    '--info-color': '#a6a9ad',
    '--shadow-color': 'rgba(0, 0, 0, 0.3)',
    '--overlay-bg': 'rgba(0, 0, 0, 0.7)'
  }
}
