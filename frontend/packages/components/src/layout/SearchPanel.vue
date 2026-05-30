<template>
  <div class="ar-search-panel">
    <div class="ar-search-panel-header">
      <h3 class="ar-search-panel-title">筛选条件</h3>
      <button
        v-if="hasActiveFilters"
        class="ar-search-panel-clear"
        @click="handleClearAll"
      >清除全部</button>
    </div>

    <div v-if="loading" class="ar-search-panel-loading">
      <span class="ar-search-panel-loading-text">加载中...</span>
    </div>

    <div v-else class="ar-search-panel-body">
      <div
        v-for="filter in filters"
        :key="filter.key"
        class="ar-search-filter-group"
      >
        <div class="ar-search-filter-header">
          <h4 class="ar-search-filter-label">{{ filter.label }}</h4>
          <span
            v-if="getFilterActiveCount(filter) > 0"
            class="ar-search-filter-active-count"
          >{{ getFilterActiveCount(filter) }}</span>
        </div>

        <!-- 复选框组 -->
        <div
          v-if="filter.type === 'checkbox'"
          class="ar-search-filter-checkbox"
        >
          <label
            v-for="option in filter.options"
            :key="option.value"
            class="ar-search-checkbox-item"
          >
            <input
              type="checkbox"
              :value="option.value"
              :checked="getCheckboxChecked(filter.key, option.value)"
              @change="handleCheckboxChange(filter.key, option.value, $event)"
            />
            <span class="ar-search-checkbox-text">{{ option.label }}</span>
          </label>
        </div>

        <!-- 下拉选择 -->
        <div
          v-if="filter.type === 'select'"
          class="ar-search-filter-select"
        >
          <select
            :value="getFilterValue(filter.key)"
            @change="handleSelectChange(filter.key, $event)"
          >
            <option value="">全部</option>
            <option
              v-for="option in filter.options"
              :key="option.value"
              :value="option.value"
            >{{ option.label }}</option>
          </select>
        </div>

        <!-- 日期选择 -->
        <div
          v-if="filter.type === 'date'"
          class="ar-search-filter-date"
        >
          <input
            type="date"
            :value="getFilterValue(filter.key)"
            @change="handleDateChange(filter.key, $event)"
          />
        </div>

        <!-- 日期范围 -->
        <div
          v-if="filter.type === 'daterange'"
          class="ar-search-filter-daterange"
        >
          <input
            type="date"
            :value="getFilterValue(filter.key)?.start"
            @change="handleDateRangeChange(filter.key, 'start', $event)"
          />
          <span class="ar-search-daterange-separator">至</span>
          <input
            type="date"
            :value="getFilterValue(filter.key)?.end"
            @change="handleDateRangeChange(filter.key, 'end', $event)"
          />
        </div>
      </div>
    </div>

    <slot name="footer" />

    <div class="ar-search-panel-footer">
      <button
        class="ar-search-panel-apply"
        @click="handleApply"
      >应用筛选</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FilterOption, FilterItem } from './types'

interface Props {
  filters?: FilterItem[]
  activeFilters?: Record<string, any>
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  filters: () => [],
  activeFilters: () => ({}),
  loading: false
})

const emit = defineEmits<{
  (e: 'filterChange', key: string, value: any): void
  (e: 'clearAll'): void
  (e: 'apply'): void
}>()

// ── computed ──────────────────────────────────────────
const hasActiveFilters = computed(() => {
  return Object.keys(props.activeFilters).some(key => {
    const value = props.activeFilters[key]
    if (Array.isArray(value)) {
      return value.length > 0
    }
    if (typeof value === 'object' && value !== null) {
      return value.start || value.end
    }
    return value !== undefined && value !== null && value !== ''
  })
})

const getFilterValue = (key: string, optionValue?: any) => {
  const value = props.activeFilters[key]
  if (optionValue !== undefined && Array.isArray(value)) {
    return value.includes(optionValue)
  }
  return value
}

/** 获取 checkbox 选中状态 */
const getCheckboxChecked = (key: string, optionValue: any): boolean => {
  const value = props.activeFilters[key]
  if (Array.isArray(value)) {
    return value.includes(optionValue)
  }
  return false
}

/** 获取筛选项激活数量 */
const getFilterActiveCount = (filter: FilterItem): number => {
  const value = props.activeFilters[filter.key]
  if (!value) return 0
  if (filter.type === 'checkbox' && Array.isArray(value)) {
    return value.length
  }
  if (filter.type === 'daterange' && typeof value === 'object') {
    return (value.start || value.end) ? 1 : 0
  }
  return value ? 1 : 0
}

// ── handlers ──────────────────────────────────────────
const handleCheckboxChange = (key: string, optionValue: any, event: Event) => {
  const target = event.target as HTMLInputElement
  const currentValue = props.activeFilters[key] || []
  let newValue: any[]

  if (target.checked) {
    newValue = [...currentValue, optionValue]
  } else {
    newValue = currentValue.filter((v: any) => v !== optionValue)
  }

  emit('filterChange', key, newValue)
}

const handleSelectChange = (key: string, event: Event) => {
  const target = event.target as HTMLSelectElement
  emit('filterChange', key, target.value)
}

const handleDateChange = (key: string, event: Event) => {
  const target = event.target as HTMLInputElement
  emit('filterChange', key, target.value)
}

const handleDateRangeChange = (key: string, field: 'start' | 'end', event: Event) => {
  const target = event.target as HTMLInputElement
  const currentValue = { ...(props.activeFilters[key] || {}) }
  currentValue[field] = target.value
  emit('filterChange', key, currentValue)
}

const handleClearAll = () => {
  emit('clearAll')
}

const handleApply = () => {
  emit('apply')
}
</script>

<style lang="scss" scoped>
.ar-search-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color, #ffffff);
}

.ar-search-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-search-panel-title {
  font-size: var(--ar-font-size-medium, 16px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
  margin: 0;
}

.ar-search-panel-clear {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: none;
  background: transparent;
  color: var(--ar-color-primary, #409eff);
  font-size: var(--ar-font-size-small, 13px);
  cursor: pointer;

  &:hover {
    color: var(--ar-color-primary-dark-2, #3a8ee6);
  }
}

.ar-search-panel-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ar-spacing-lg, 16px);
}

.ar-search-panel-loading-text {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-secondary, #909399);
}

.ar-search-panel-body {
  flex: 1;
  overflow: auto;
  padding: var(--ar-spacing-lg, 16px);
}

// ── filter group ──────────────────────────────────────
.ar-search-filter-group {
  margin-bottom: var(--ar-spacing-lg, 16px);

  &:last-child {
    margin-bottom: 0;
  }
}

.ar-search-filter-header {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  margin-bottom: var(--ar-spacing-sm, 8px);
}

.ar-search-filter-label {
  font-size: var(--ar-font-size-base, 14px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
  margin: 0;
}

.ar-search-filter-active-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background-color: var(--ar-color-primary, #409eff);
  color: var(--ar-color-white, #ffffff);
  font-size: 11px;
  border-radius: 8px;
  line-height: 1;
}

// ── filter types ──────────────────────────────────────
.ar-search-filter-checkbox {
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-search-checkbox-item {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
  cursor: pointer;

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
    cursor: pointer;
    accent-color: var(--ar-color-primary, #409eff);
  }
}

.ar-search-checkbox-text {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-regular, #606266);
}

.ar-search-filter-select {
  select {
    width: 100%;
    padding: 6px var(--ar-spacing-sm, 8px);
    border: 1px solid var(--ar-border-color-base, #dcdfe6);
    border-radius: var(--ar-border-radius-base, 4px);
    font-size: var(--ar-font-size-base, 14px);
    color: var(--ar-text-color-primary, #303133);
    background-color: var(--ar-bg-color, #ffffff);
    cursor: pointer;

    &:focus {
      border-color: var(--ar-color-primary, #409eff);
      outline: none;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.ar-search-filter-date {
  input[type="date"] {
    width: 100%;
    padding: 6px var(--ar-spacing-sm, 8px);
    border: 1px solid var(--ar-border-color-base, #dcdfe6);
    border-radius: var(--ar-border-radius-base, 4px);
    font-size: var(--ar-font-size-base, 14px);
    color: var(--ar-text-color-primary, #303133);
    background-color: var(--ar-bg-color, #ffffff);

    &:focus {
      border-color: var(--ar-color-primary, #409eff);
      outline: none;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.ar-search-filter-daterange {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);

  input[type="date"] {
    flex: 1;
    min-width: 0;
    padding: 6px var(--ar-spacing-sm, 8px);
    border: 1px solid var(--ar-border-color-base, #dcdfe6);
    border-radius: var(--ar-border-radius-base, 4px);
    font-size: var(--ar-font-size-base, 14px);
    color: var(--ar-text-color-primary, #303133);
    background-color: var(--ar-bg-color, #ffffff);

    &:focus {
      border-color: var(--ar-color-primary, #409eff);
      outline: none;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
    }
  }
}

.ar-search-daterange-separator {
  font-size: var(--ar-font-size-base, 14px);
  color: var(--ar-text-color-placeholder, #c0c4cc);
  flex-shrink: 0;
}

// ── footer ────────────────────────────────────────────
.ar-search-panel-footer {
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-search-panel-apply {
  width: 100%;
  padding: var(--ar-spacing-sm, 8px) var(--ar-spacing-md, 12px);
  border: 1px solid var(--ar-color-primary, #409eff);
  background-color: var(--ar-color-primary, #409eff);
  color: var(--ar-color-white, #ffffff);
  font-size: var(--ar-font-size-base, 14px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    background-color: var(--ar-color-primary-dark-2, #3a8ee6);
    border-color: var(--ar-color-primary-dark-2, #3a8ee6);
  }
}

@media (max-width: 768px) {
  .ar-search-panel-header {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-search-panel-body {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-search-panel-footer {
    padding: var(--ar-spacing-sm, 8px);
  }
}
</style>
