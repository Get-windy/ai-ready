<template>
  <div class="search-fields-grid">
    <!-- 搜索字段网格 -->
    <div class="sfg-grid">
      <!-- 日期起止选择（始终显示在第一个） -->
      <div v-if="showDateRange" class="sfg-field-item">
        <a-range-picker
          :value="dateRange"
          :placeholder="['开始日期', '结束日期']"
          size="small"
          @change="handleDateRangeChange"
        />
      </div>

      <!-- 可见搜索字段 -->
      <template v-for="field in fields" :key="field.key">
        <div class="sfg-field-item">
          <!-- 输入框 -->
          <a-input
            v-if="field.type === 'input'"
            :value="values[field.key]"
            :placeholder="field.placeholder || field.label"
            size="small"
            @input="handleInput(field.key, $event)"
            @press-enter="emitSearch"
          >
            <template v-if="field.suffix === 'search'" #suffix>
              <SearchOutlined />
            </template>
          </a-input>

          <!-- 下拉选择（标签在边框内） -->
          <div v-else-if="field.type === 'select'" class="sfg-select-wrap">
            <span class="sfg-select-label">{{ field.label }}</span>
            <a-select
              :value="values[field.key]"
              size="small"
              allow-clear
              :options="field.options"
              :placeholder="field.placeholder"
              style="width: 100%"
              @change="handleChange(field.key, $event)"
            />
          </div>

          <!-- 日期范围 -->
          <a-range-picker
            v-else-if="field.type === 'dateRange'"
            :value="values[field.key]"
            size="small"
            style="width: 100%"
            :placeholder="field.placeholder || ['开始日期', '结束日期']"
            @change="handleFieldChange(field.key, $event)"
          />
        </div>
      </template>

      <!-- 查询按钮（始终显示） -->
      <div class="sfg-field-item sfg-action-item">
        <a-space :size="8">
          <a-button type="primary" size="small" @click="emitSearch">查询</a-button>
          <a-button size="small" @click="emitRefresh">
            <ReloadOutlined />
          </a-button>
        </a-space>
      </div>

      <!-- 复选框（始终显示） -->
      <template v-if="checkboxes && checkboxes.length > 0">
        <div v-for="cb in checkboxes" :key="cb.key" class="sfg-field-item sfg-checkbox-field">
          <a-checkbox :checked="values[cb.key]" @change="handleCheckboxChange(cb.key, $event)">
            {{ cb.label }}
          </a-checkbox>
        </div>
      </template>

      <!-- 额外插槽 -->
      <div v-if="$slots.extra" class="sfg-field-item sfg-extra-item">
        <slot name="extra" />
      </div>
    </div>

    <!-- 更多条件 -->
    <div v-if="hasMoreFields" class="sfg-more-toggle">
      <a-button type="link" size="small" @click="expanded = !expanded">
        {{ expanded ? '收起' : '更多条件' }}
        <DownOutlined v-if="!expanded" />
        <UpOutlined v-else />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { SearchOutlined, ReloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons-vue'
import type { Dayjs } from 'dayjs'

/** 搜索字段定义 */
export interface SearchFieldItem {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange'
  placeholder?: string | string[]
  span?: number
  options?: { label: string; value: string | number }[]
  suffix?: 'search'
}

/** 复选框定义 */
export interface SearchCheckboxItem {
  key: string
  label: string
}

const props = withDefaults(defineProps<{
  /** 所有搜索字段（页面负责过滤可见字段） */
  fields: SearchFieldItem[]
  /** 搜索值对象 */
  values: Record<string, any>
  /** 默认显示的字段数量（不含日期范围，超出则折叠） */
  defaultVisibleCount?: number
  /** 是否显示日期范围选择器 */
  showDateRange?: boolean
  /** 复选框列表 */
  checkboxes?: SearchCheckboxItem[]
  /** 日期范围值 */
  dateRange?: [Dayjs, Dayjs] | null
}>(), {
  defaultVisibleCount: 7,
  showDateRange: true,
  checkboxes: () => [],
  dateRange: null,
})

const emit = defineEmits<{
  search: []
  refresh: []
  'update:values': [value: Record<string, any>]
  'update:dateRange': [value: [Dayjs, Dayjs] | null]
}>()

// 展开状态
const expanded = ref(false)

// 可见字段（根据展开状态）
const visibleFields = computed(() => {
  if (expanded.value) {
    return props.fields
  }
  return props.fields.slice(0, props.defaultVisibleCount)
})

// 是否有更多字段
const hasMoreFields = computed(() => {
  return props.fields.length > props.defaultVisibleCount
})

// 输入变化
function handleInput(key: string, event: Event) {
  const target = event.target as HTMLInputElement
  const newValues = { ...props.values, [key]: target.value }
  emit('update:values', newValues)
}

// 选择变化
function handleChange(key: string, value: any) {
  const newValues = { ...props.values, [key]: value }
  emit('update:values', newValues)
}

// 字段变化（通用）
function handleFieldChange(key: string, value: any) {
  const newValues = { ...props.values, [key]: value }
  emit('update:values', newValues)
}

// 复选框变化
function handleCheckboxChange(key: string, event: any) {
  const newValues = { ...props.values, [key]: event.target.checked }
  emit('update:values', newValues)
}

// 日期范围变化
function handleDateRangeChange(val: [Dayjs, Dayjs] | null) {
  emit('update:dateRange', val)
}

// 查询
function emitSearch() {
  emit('search')
}

// 刷新
function emitRefresh() {
  emit('refresh')
}
</script>

<style scoped>
.search-fields-grid {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.sfg-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.sfg-field-item {
  display: flex;
  width: calc(100% / 7 - 12px);
  min-width: 0;
}

.sfg-field-item :deep(.ant-input-wrapper),
.sfg-field-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  font-size: 13px;
}

.sfg-field-item :deep(.ant-select) {
  width: 100%;
}

.sfg-field-item :deep(.ant-select .ant-select-selector) {
  font-size: 13px;
}

.sfg-field-item :deep(.ant-picker) {
  width: 100%;
}

/* 下拉框：标签在边框内左侧 */
.sfg-select-wrap {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #fff;
}
.sfg-select-wrap:hover {
  border-color: #4096ff;
}
.sfg-select-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  flex-shrink: 0;
  padding-left: 8px;
}
.sfg-select-wrap :deep(.ant-select) {
  flex: 1;
  min-width: 0;
}
.sfg-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
  display: flex;
  align-items: center;
}

.sfg-action-item {
  width: auto;
  flex-shrink: 0;
}

/* 复选框项 */
.sfg-checkbox-field {
  width: auto;
  flex-shrink: 0;
}

.sfg-more-toggle {
  margin-top: 4px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.sfg-more-toggle::before,
.sfg-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.sfg-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}
</style>
