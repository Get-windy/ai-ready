<template>
  <div class="search-bar-container">
    <a-form layout="inline" :model="searchForm" @finish="handleSearch">
      <slot name="search-form">
        <!-- 搜索字段 -->
        <template v-for="item in fields" :key="item.name">
          <a-form-item v-if="shouldShowField(item)" :label="item.label">
            <template v-if="item.type === 'select'">
              <a-select
                v-model:value="searchForm[item.name]"
                :placeholder="item.placeholder || `请选择${item.label}`"
                :options="item.options"
                :allow-clear="item.allowClear !== false"
                style="min-width: 200px"
                @change="handleFieldChange(item.name, $event)"
              />
            </template>
            <template v-else-if="item.type === 'date-range'">
              <a-range-picker
                v-model:value="searchForm[item.name]"
                :placeholder="['开始日期', '结束日期']"
                style="min-width: 260px"
                @change="handleDateRangeChange(item.name, $event)"
              />
            </template>
            <template v-else-if="item.type === 'input'">
              <a-input
                v-model:value="searchForm[item.name]"
                :placeholder="item.placeholder || `请输入${item.label}`"
                :allow-clear="item.allowClear !== false"
                @press-enter="handleSearch"
              />
            </template>
            <template v-else-if="item.type === 'number'">
              <a-input-number
                v-model:value="searchForm[item.name]"
                :placeholder="`请输入${item.label}`"
                :style="{ width: '160px' }"
                :allow-clear="item.allowClear !== false"
              />
            </template>
            <template v-else>
              <a-input
                v-model:value="searchForm[item.name]"
                :placeholder="`请输入${item.label}`"
                :allow-clear="item.allowClear !== false"
              />
            </template>
          </a-form-item>
        </template>
      </slot>
      
      <!-- 操作按钮 -->
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit" :loading="loading">
            搜索
          </a-button>
          <a-button @click="handleReset">
            重置
          </a-button>
          <a-button
            v-if="expandable"
            type="link"
            @click="isExpanded = !isExpanded"
          >
            {{ isExpanded ? '收起' : '展开' }} <span v-if="expandedFields.length > 0">({{ expandedFields.length }})</span>
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>
    
    <!-- 展开的搜索字段 -->
    <a-divider v-if="isExpanded && expandedFields.length > 0" />
    <div v-if="isExpanded" class="expanded-fields">
      <a-space direction="vertical" :size="12">
        <template v-for="item in expandedFields" :key="item.name">
          <a-form-item :label="item.label">
            <template v-if="item.type === 'select'">
              <a-select
                v-model:value="searchForm[item.name]"
                :options="item.options"
                :allow-clear="true"
                style="width: 100%"
                @change="handleFieldChange(item.name, $event)"
              />
            </template>
            <template v-else-if="item.type === 'date-range'">
              <a-range-picker
                v-model:value="searchForm[item.name]"
                style="width: 100%"
              />
            </template>
            <template v-else>
              <a-input
                v-model:value="searchForm[item.name]"
                :placeholder="`请输入${item.label}`"
                :allow-clear="true"
              />
            </template>
          </a-form-item>
        </template>
      </a-space>
    </div>
    
    <!-- 搜索结果统计 -->
    <div v-if="showResultCount" class="result-count">
      <span class="count-text">共 {{ total }} 条结果</span>
      <a-link v-if="showClear" @click="handleClearAll">清空条件</a-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, type PropType } from 'vue'

// 搜索字段类型
export type SearchField = {
  name: string
  label: string
  type?: 'input' | 'select' | 'number' | 'date-range' | 'custom'
  options?: Array<{ label: string; value: any }>
  placeholder?: string
  allowClear?: boolean
  initial?: any
  fullWidth?: boolean
  [key: string]: any
}

// Props
const props = defineProps({
  fields: {
    type: Array as PropType<SearchField[]>,
    required: true
  },
  modelValue: {
    type: Object as PropType<Record<string, any>>,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  expandable: {
    type: Boolean,
    default: true
  },
  showResultCount: {
    type: Boolean,
    default: false
  },
  total: {
    type: Number,
    default: 0
  },
  showClear: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: any]
  'search': [formData: any]
  'reset': []
  'clear': []
  'field-change': [field: string, value: any]
}>()

// State
const isExpanded = ref(false)
const searchForm = ref<Record<string, any>>({})

// 计算展开字段
const expandedFields = computed(() => {
  return props.fields.filter(item => item.fullWidth)
})

// 初始化表单数据
watch(() => props.modelValue, (newVal) => {
  searchForm.value = { ...searchForm.value, ...newVal }
}, { immediate: true })

// 是否显示字段
const shouldShowField = (field: SearchField) => {
  if (field.fullWidth) return false
  return true
}

// 字段变化处理
const handleFieldChange = (field: string, value: any) => {
  searchForm.value[field] = value
  emit('update:modelValue', searchForm.value)
  emit('field-change', field, value)
}

// 日期范围变化处理
const handleDateRangeChange = (field: string, dates: [string, string]) => {
  if (dates && dates.length === 2) {
    searchForm.value[`${field}_start`] = dates[0]
    searchForm.value[`${field}_end`] = dates[1]
  } else {
    delete searchForm.value[`${field}_start`]
    delete searchForm.value[`${field}_end`]
  }
  emit('update:modelValue', searchForm.value)
  emit('field-change', field, dates)
}

// 搜索提交
const handleSearch = () => {
  emit('search', searchForm.value)
}

// 重置
const handleReset = () => {
  searchForm.value = props.fields.reduce((acc, item) => {
    acc[item.name] = item.initial || undefined
    return acc
  }, {} as Record<string, any>)
  emit('update:modelValue', searchForm.value)
  emit('reset')
}

// 清空所有条件
const handleClearAll = () => {
  searchForm.value = {}
  emit('update:modelValue', searchForm.value)
  emit('clear')
}
</script>

<style scoped>
.search-bar-container {
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.expanded-fields {
  margin-top: 16px;
}

.result-count {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.count-text {
  color: #666;
  font-size: 14px;
}
</style>