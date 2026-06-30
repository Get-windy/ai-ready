<template>
  <div class="inline-field" :class="widthClass" :style="widthStyle">
    <!-- 查看模式 -->
    <template v-if="viewMode">
      <span class="inline-view-text">{{ displayValue }}</span>
    </template>
    <!-- 编辑模式 -->
    <template v-else>
      <a-select
        v-if="type === 'select'"
        :value="modelValue"
        :placeholder="label"
        show-search
        :filter-option="filterOption"
        :loading="loading"
        :disabled="disabled"
        size="small"
        style="flex:1"
        @update:value="emitValue"
        @change="(val: any) => emit('change', val)"
      >
        <a-select-option v-for="opt in options" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </a-select-option>
      </a-select>
      <a-tree-select
        v-else-if="type === 'tree-select'"
        :value="modelValue"
        :placeholder="label"
        :tree-data="treeData"
        :field-names="fieldNames"
        show-search
        :disabled="disabled"
        size="small"
        style="flex:1"
        @update:value="emitValue"
      />
      <a-date-picker
        v-else-if="type === 'date'"
        :value="modelValue"
        :placeholder="label"
        :format="format || 'YYYY-MM-DD'"
        value-format="YYYY-MM-DD"
        :disabled="disabled"
        size="small"
        style="flex:1"
        @update:value="emitValue"
      />
      <a-input-number
        v-else-if="type === 'number'"
        :value="modelValue"
        :placeholder="label"
        :precision="precision"
        :min="min"
        :max="max"
        :disabled="disabled"
        size="small"
        style="flex:1"
        @update:value="emitValue"
      />
      <a-textarea
        v-else-if="type === 'textarea'"
        :value="modelValue"
        :placeholder="label"
        :disabled="disabled"
        size="small"
        :auto-size="{ minRows: 2, maxRows: 4 }"
        style="flex:1"
        @update:value="emitValue"
      />
      <a-input
        v-else
        :value="modelValue"
        :placeholder="label"
        :disabled="disabled"
        size="small"
        style="flex:1"
        @update:value="emitValue"
      />
      <!-- 搜索按钮 -->
      <a-button
        v-if="searchBtn"
        type="link" size="small" class="inline-search-btn"
        @click="emit('searchBtn')"
      >
        {{ searchBtn }}
      </a-button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'InlineField' })

const props = withDefaults(defineProps<{
  /** 字段类型 */
  type: 'select' | 'tree-select' | 'date' | 'number' | 'input' | 'textarea'
  /** 标签文本（显示在 placeholder 中） */
  label: string
  /** 当前值 */
  modelValue?: any
  /** 下拉选项（type=select） */
  options?: { label: string; value: any }[]
  /** 树数据（type=tree-select） */
  treeData?: any[]
  /** 树字段映射 */
  fieldNames?: { label: string; value: string; children: string }
  /** 日期格式 */
  format?: string
  /** 数字精度 */
  precision?: number
  /** 最小值 */
  min?: number
  /** 最大值 */
  max?: number
  /** 是否加载中 */
  loading?: boolean
  /** 是否禁用 */
  disabled?: boolean
  /** 搜索按钮文本 */
  searchBtn?: string
  /** 字段宽度：支持预设值或具体像素数 */
  width?: 'default' | 'narrow' | 'wide' | number
  /** 查看模式 */
  viewMode?: boolean
}>(), {
  type: 'input',
  options: () => [],
  loading: false,
  disabled: false,
  width: 'default',
  viewMode: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
  'change': [value: any]
  'searchBtn': []
}>()

function emitValue(val: any) {
  emit('update:modelValue', val)
}

const filterOption = (input: string, option: any) => {
  const text = option?.label || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

/** 查看模式的显示值 */
const displayValue = computed(() => {
  if (props.modelValue === undefined || props.modelValue === null || props.modelValue === '') return '—'
  if (props.type === 'select' && props.options) {
    const opt = props.options.find(o => o.value === props.modelValue)
    return opt?.label || String(props.modelValue)
  }
  return String(props.modelValue)
})

/** 动态宽度类名 */
const widthClass = computed(() => {
  if (typeof props.width === 'string') {
    return {
      'inline-field--narrow': props.width === 'narrow',
      'inline-field--wide': props.width === 'wide',
    }
  }
  return {}
})

/** 动态宽度样式 */
const widthStyle = computed(() => {
  if (typeof props.width === 'number') {
    return { flex: `0 0 ${props.width}px`, minWidth: `${props.width}px` }
  }
  return {}
})
</script>

<style scoped>
.inline-field {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.inline-field--narrow {
  flex: 0 0 120px;
  min-width: 100px;
}

.inline-field--wide {
  flex: 2;
  min-width: 180px;
}

.inline-search-btn {
  padding: 0 4px;
  font-size: 11px;
  color: #1890ff;
  flex-shrink: 0;
}

.inline-view-text {
  font-size: 13px;
  color: #262626;
  line-height: 26px;
  padding: 0 4px;
}
</style>