import { ref } from 'vue'

/**
 * 分析模块「页面配置」弹窗状态（查询条件显隐 / 功能按钮启停）
 *
 * 分析模块各页共用同一段样板：默认清单常量 → 可变副本 → 按 key 查显隐 → 承接弹窗回传。
 * 抽到一处，避免每页重复 30 行；持久化由 PageConfigPanel 自身按 storageKey 负责。
 */
export interface QueryFieldSetting {
  key: string
  label: string
  visible: boolean
}

export interface FunctionButtonSetting {
  key: string
  label: string
  enabled: boolean
}

export interface AnalyticsPageConfigOptions {
  /** 页面配置 storageKey（**必须**与数据表列配置 storageKey 不同值） */
  storageKey: string
  defaultQueryFields: QueryFieldSetting[]
  defaultFunctionButtons: FunctionButtonSetting[]
}

export function useAnalyticsPageConfig(options: AnalyticsPageConfigOptions) {
  const { storageKey, defaultQueryFields, defaultFunctionButtons } = options

  const showPageConfig = ref(false)
  const queryFields = ref<QueryFieldSetting[]>(defaultQueryFields.map(f => ({ ...f })))
  const functionButtons = ref<FunctionButtonSetting[]>(defaultFunctionButtons.map(b => ({ ...b })))

  /** 查询条件是否显示（未登记的 key 默认隐藏，与 PageConfigPanel 一致） */
  function isQueryVisible(key: string): boolean {
    return queryFields.value.find(f => f.key === key)?.visible ?? false
  }

  /** 功能按钮是否启用 */
  function isButtonEnabled(key: string): boolean {
    return functionButtons.value.find(b => b.key === key)?.enabled ?? false
  }

  function handlePageConfigChange(config: {
    queryFields?: QueryFieldSetting[]
    functionButtons?: FunctionButtonSetting[]
  }) {
    if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
    if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
  }

  return {
    showPageConfig,
    queryFields,
    functionButtons,
    isQueryVisible,
    isButtonEnabled,
    handlePageConfigChange,
    pageConfigStorageKey: storageKey
  }
}
