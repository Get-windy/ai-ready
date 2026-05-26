import { ref, watch, onMounted } from 'vue'

interface PanelLayout {
  id: string
  type: string
  title: string
  position: {
    x: number
    y: number
    w: number
    h: number
  }
  config: Record<string, any>
  visible: boolean
}

interface KpiConfig {
  id: string
  label: string
  metric: string
  format: 'number' | 'percentage' | 'duration' | 'bytes'
  thresholds?: {
    warning: number
    critical: number
  }
}

interface PersonalizationConfig {
  layouts: PanelLayout[]
  customKpis: KpiConfig[]
  gridColumns: number
  gridGap: number
  autoRefresh: boolean
  refreshInterval: number
  defaultTimeRange: string
  theme: 'light' | 'dark' | 'auto'
}

interface SavedConfiguration {
  id: string
  name: string
  description?: string
  config: PersonalizationConfig
  createdAt: string
  updatedAt: string
  isDefault?: boolean
}

const STORAGE_KEY = 'dashboard_personalization'
const CONFIGS_KEY = 'dashboard_saved_configs'

export function usePersonalization() {
  const config = ref<PersonalizationConfig>({
    layouts: [],
    customKpis: [],
    gridColumns: 12,
    gridGap: 16,
    autoRefresh: true,
    refreshInterval: 30,
    defaultTimeRange: '1h',
    theme: 'auto'
  })

  const savedConfigs = ref<SavedConfiguration[]>([])
  const currentConfigId = ref<string | null>(null)

  // Load configuration
  const loadConfig = () => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        config.value = { ...config.value, ...parsed }
      }

      const savedConfigsData = localStorage.getItem(CONFIGS_KEY)
      if (savedConfigsData) {
        savedConfigs.value = JSON.parse(savedConfigsData)
      }
    } catch (e) {
      console.warn('Failed to load personalization config:', e)
    }
  }

  // Save current configuration
  const saveConfig = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(config.value))
    } catch (e) {
      console.warn('Failed to save personalization config:', e)
    }
  }

  // Update configuration
  const updateConfig = (updates: Partial<PersonalizationConfig>) => {
    config.value = { ...config.value, ...updates }
    saveConfig()
  }

  // Update panel layout
  const updatePanelLayout = (panelId: string, layout: Partial<PanelLayout>) => {
    const index = config.value.layouts.findIndex(p => p.id === panelId)
    if (index !== -1) {
      config.value.layouts[index] = { ...config.value.layouts[index], ...layout }
      saveConfig()
    }
  }

  // Add custom KPI
  const addCustomKpi = (kpi: KpiConfig) => {
    config.value.customKpis.push(kpi)
    saveConfig()
  }

  // Remove custom KPI
  const removeCustomKpi = (kpiId: string) => {
    config.value.customKpis = config.value.customKpis.filter(k => k.id !== kpiId)
    saveConfig()
  }

  // Save configuration as named preset
  const saveAsPreset = (name: string, description?: string): SavedConfiguration => {
    const preset: SavedConfiguration = {
      id: `config-${Date.now()}`,
      name,
      description,
      config: JSON.parse(JSON.stringify(config.value)),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    savedConfigs.value.push(preset)
    saveConfigsList()
    return preset
  }

  // Load preset configuration
  const loadPreset = (presetId: string): boolean => {
    const preset = savedConfigs.value.find(c => c.id === presetId)
    if (preset) {
      config.value = JSON.parse(JSON.stringify(preset.config))
      currentConfigId.value = presetId
      saveConfig()
      return true
    }
    return false
  }

  // Delete preset
  const deletePreset = (presetId: string) => {
    savedConfigs.value = savedConfigs.value.filter(c => c.id !== presetId)
    if (currentConfigId.value === presetId) {
      currentConfigId.value = null
    }
    saveConfigsList()
  }

  // Set default preset
  const setDefaultPreset = (presetId: string) => {
    savedConfigs.value.forEach(c => {
      c.isDefault = c.id === presetId
    })
    saveConfigsList()
  }

  // Save configs list
  const saveConfigsList = () => {
    try {
      localStorage.setItem(CONFIGS_KEY, JSON.stringify(savedConfigs.value))
    } catch (e) {
      console.warn('Failed to save configs list:', e)
    }
  }

  // Export configuration
  const exportConfig = (): string => {
    const exportData = {
      config: config.value,
      exportedAt: new Date().toISOString(),
      version: '1.0'
    }
    return JSON.stringify(exportData, null, 2)
  }

  // Import configuration
  const importConfig = (jsonString: string): boolean => {
    try {
      const imported = JSON.parse(jsonString)
      if (imported.config) {
        config.value = { ...config.value, ...imported.config }
        saveConfig()
        return true
      }
      return false
    } catch (e) {
      console.error('Failed to import config:', e)
      return false
    }
  }

  // Reset to defaults
  const resetToDefaults = () => {
    config.value = {
      layouts: [],
      customKpis: [],
      gridColumns: 12,
      gridGap: 16,
      autoRefresh: true,
      refreshInterval: 30,
      defaultTimeRange: '1h',
      theme: 'auto'
    }
    saveConfig()
  }

  // Share configuration (generate shareable link)
  const generateShareLink = (): string => {
    const shareData = {
      config: config.value,
      timestamp: Date.now()
    }
    const encoded = btoa(JSON.stringify(shareData))
    return `${window.location.origin}${window.location.pathname}?config=${encoded}`
  }

  // Load from share link
  const loadFromShareLink = (encodedConfig: string): boolean => {
    try {
      const decoded = JSON.parse(atob(encodedConfig))
      if (decoded.config) {
        config.value = { ...config.value, ...decoded.config }
        saveConfig()
        return true
      }
      return false
    } catch (e) {
      console.error('Failed to load shared config:', e)
      return false
    }
  }

  onMounted(() => {
    loadConfig()
  })

  return {
    config,
    savedConfigs,
    currentConfigId,
    updateConfig,
    updatePanelLayout,
    addCustomKpi,
    removeCustomKpi,
    saveAsPreset,
    loadPreset,
    deletePreset,
    setDefaultPreset,
    exportConfig,
    importConfig,
    resetToDefaults,
    generateShareLink,
    loadFromShareLink
  }
}

// Default panel configurations
export const defaultPanelTypes = [
  {
    id: 'kpi-health',
    type: 'kpi',
    title: '系统健康度',
    defaultConfig: { metric: 'health', showTrend: true, thresholds: { warning: 70, critical: 50 } }
  },
  {
    id: 'kpi-response',
    type: 'kpi',
    title: '平均响应时间',
    defaultConfig: { metric: 'response_time', showTrend: true, format: 'duration' }
  },
  {
    id: 'kpi-throughput',
    type: 'kpi',
    title: '当前吞吐量',
    defaultConfig: { metric: 'throughput', showTrend: true, format: 'number' }
  },
  {
    id: 'kpi-error',
    type: 'kpi',
    title: '错误率',
    defaultConfig: { metric: 'error_rate', showTrend: true, format: 'percentage' }
  },
  {
    id: 'chart-cpu',
    type: 'chart',
    title: 'CPU使用率',
    defaultConfig: { metric: 'cpu', chartType: 'area', timeRange: '1h' }
  },
  {
    id: 'chart-memory',
    type: 'chart',
    title: '内存使用情况',
    defaultConfig: { metric: 'memory', chartType: 'line', timeRange: '1h' }
  },
  {
    id: 'chart-requests',
    type: 'chart',
    title: '请求趋势',
    defaultConfig: { metric: 'requests', chartType: 'bar', timeRange: '24h' }
  },
  {
    id: 'alert-recent',
    type: 'alert',
    title: '最近告警',
    defaultConfig: { limit: 10, levels: ['P1', 'P2', 'P3'], autoRefresh: true }
  },
  {
    id: 'status-services',
    type: 'status',
    title: '服务状态',
    defaultConfig: { services: ['api', 'web', 'worker', 'scheduler'] }
  }
]

// Available metrics for custom KPIs
export const availableMetrics = [
  { id: 'health', label: '系统健康度', format: 'percentage' as const },
  { id: 'response_time', label: '平均响应时间', format: 'duration' as const },
  { id: 'throughput', label: '吞吐量', format: 'number' as const },
  { id: 'error_rate', label: '错误率', format: 'percentage' as const },
  { id: 'cpu', label: 'CPU使用率', format: 'percentage' as const },
  { id: 'memory', label: '内存使用率', format: 'percentage' as const },
  { id: 'disk', label: '磁盘使用率', format: 'percentage' as const },
  { id: 'network_in', label: '网络入流量', format: 'bytes' as const },
  { id: 'network_out', label: '网络出流量', format: 'bytes' as const },
  { id: 'active_users', label: '活跃用户', format: 'number' as const },
  { id: 'requests', label: '请求数', format: 'number' as const }
]