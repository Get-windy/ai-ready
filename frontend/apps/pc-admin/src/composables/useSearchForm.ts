/**
 * useSearchForm - 通用搜索表单 Hook
 *
 * 用于所有列表页面的搜索条件管理
 */
import { reactive, ref } from 'vue'
import type { Ref } from 'vue'

export interface SearchFieldConfig {
  name: string
  label: string
  type: 'input' | 'select' | 'date' | 'daterange' | 'number'
  width?: number
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  defaultValue?: any
}

export function useSearchForm(fields: SearchFieldConfig[], options?: {
  onSearch?: (formData: Record<string, any>) => void
  onReset?: () => void
}) {
  const initialValues: Record<string, any> = {}
  const fieldConfigMap: Record<string, SearchFieldConfig> = {}

  fields.forEach(field => {
    initialValues[field.name] = field.defaultValue !== undefined ? field.defaultValue : undefined
    fieldConfigMap[field.name] = field
  })

  const searchForm = reactive<Record<string, any>>({ ...initialValues })
  const showHierarchy = ref(false)

  function setSearchField(name: string, value: any) {
    searchForm[name] = value
  }

  function getSearchField(name: string) {
    return searchForm[name]
  }

  function resetSearchForm() {
    fields.forEach(field => {
      searchForm[field.name] = field.defaultValue !== undefined ? field.defaultValue : undefined
    })
    options?.onReset?.()
  }

  function getSearchParams(): Record<string, any> {
    const params: Record<string, any> = {}

    fields.forEach(field => {
      const value = searchForm[field.name]
      if (value === undefined || value === null || value === '') return

      if (field.type === 'daterange' && Array.isArray(value) && value.length === 2) {
        if (value[0]) params[`${field.name}Start`] = typeof value[0].format === 'function' ? value[0].format('YYYY-MM-DD') : value[0]
        if (value[1]) params[`${field.name}End`] = typeof value[1].format === 'function' ? value[1].format('YYYY-MM-DD') : value[1]
      } else if (field.type === 'date' && value) {
        params[field.name] = typeof value.format === 'function' ? value.format('YYYY-MM-DD') : value
      } else {
        params[field.name] = value
      }
    })

    return params
  }

  return {
    searchForm,
    showHierarchy,
    fieldConfigMap,
    setSearchField,
    getSearchField,
    resetSearchForm,
    getSearchParams,
  }
}
