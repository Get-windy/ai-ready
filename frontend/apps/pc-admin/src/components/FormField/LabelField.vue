<template>
  <div
    class="label-field"
    :class="{ 'label-field--required': required, 'label-field--narrow': width === 'narrow', 'label-field--wide': width === 'wide' }"
    :style="typeof width === 'number' ? { flex: `0 0 ${width}px`, minWidth: `${width}px` } : {}"
  >
    <label>{{ label }}</label>
    <div class="label-field-input">
      <!-- 查看模式 -->
      <template v-if="viewMode">
        <span class="label-view-text">{{ displayValue }}</span>
      </template>
      <!-- 编辑模式 -->
      <template v-else>
        <a-select
          v-if="type === 'select'"
          :value="modelValue"
          :placeholder="placeholder || '请选择'"
          show-search
          :filter-option="remoteSearch ? false : filterOption"
          :loading="loading"
          :disabled="disabled"
          :allow-clear="remoteSearch"
          size="small"
          style="flex:1"
          @update:value="emitValue"
          @change="(val: any) => emit('change', val)"
          @search="(v: string) => remoteSearch && emit('search', v)"
        >
          <a-select-option
            v-for="opt in options"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-select-option>
        </a-select>
        <a-tree-select
          v-else-if="type === 'tree-select'"
          :value="modelValue"
          :placeholder="placeholder || '请选择'"
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
          :placeholder="placeholder"
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
          :placeholder="placeholder"
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
          :placeholder="placeholder"
          :rows="2"
          :disabled="disabled"
          size="small"
          style="flex:1"
          @update:value="emitValue"
        />
        <!-- display 模式：只读文本框（外观与其它字段一致，不可输入；无值时以标签/占位文本兜底，不再显示破折号） -->
        <a-input
          v-else-if="type === 'display'"
          :value="modelValue !== undefined && modelValue !== null ? modelValue : ''"
          :placeholder="placeholder || label"
          disabled
          size="small"
          style="flex:1"
        />
        <a-input
          v-else
          :value="modelValue"
          :placeholder="placeholder"
          :disabled="disabled"
          size="small"
          style="flex:1"
          @update:value="emitValue"
        />
        <!-- 搜索按钮 -->
        <a-button
          v-if="searchBtn"
          type="link"
          size="small"
          class="label-search-btn"
          @click="emit('searchBtn')"
        >
          {{ searchBtn }}
        </a-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'LabelField' })

const props = withDefaults(defineProps<{
  /** 字段类型 */
  type: 'select' | 'tree-select' | 'date' | 'number' | 'input' | 'textarea' | 'display'
  /** 标签文本（显示在外部） */
  label: string
  /** 是否必填 */
  required?: boolean
  /** 当前值 */
  modelValue?: any
  /** 占位文本 */
  placeholder?: string
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
  /** 远程搜索模式（type=select 时启用，关闭本地过滤，输入关键字通过 search 事件上抛） */
  remoteSearch?: boolean
  /** 字段宽度 */
  width?: 'default' | 'narrow' | 'wide' | number
  /** 查看模式 */
  viewMode?: boolean
}>(), {
  type: 'input',
  required: false,
  options: () => [],
  loading: false,
  disabled: false,
  remoteSearch: false,
  width: 'default',
  viewMode: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: any]
  'change': [value: any]
  'searchBtn': []
  'search': [keyword: string]
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
</script>

<style scoped>
.label-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
  min-width: 120px;
}

.label-field--narrow {
  flex: 0 0 140px;
  min-width: 120px;
}

.label-field--wide {
  flex: 2;
  min-width: 200px;
}

.label-field label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}

.label-field--required label::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 2px;
}

.label-field-input {
  display: flex;
  align-items: center;
  gap: 2px;
}

.label-search-btn {
  padding: 0 4px;
  font-size: 11px;
  color: #1890ff;
  flex-shrink: 0;
}

.label-view-text {
  font-size: 13px;
  color: #262626;
  line-height: 26px;
  padding: 0 4px;
}
</style>