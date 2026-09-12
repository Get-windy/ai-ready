<template>
  <a-modal
    :open="open"
    :title="title"
    :width="width"
    :footer="null"
    :destroy-on-close="true"
    @cancel="handleClose"
  >
    <div class="master-select-toolbar">
      <a-input-search
        v-model:value="keyword"
        :placeholder="searchPlaceholder"
        allow-clear
        size="small"
        style="width: 260px"
      />
      <a-button size="small" :disabled="!selectedRecord" type="primary" @click="handleConfirm">
        确定
      </a-button>
    </div>
    <a-table
      :columns="columns"
      :data-source="filteredData"
      :loading="loading"
      :pagination="{ pageSize: 10, showSizeChanger: false, size: 'small' }"
      :row-key="rowKey"
      size="small"
      :custom-row="customRow"
      :row-class-name="rowClassName"
      :scroll="{ y: 320 }"
    />
  </a-modal>
</template>

<script setup lang="ts">
/**
 * MasterSelectModal — 通用主数据选择弹窗
 *
 * 用于表单字段的「快速查询」按钮（+Q / Q）：在弹窗中按关键字过滤主数据列表，
 * 选中后回填到表单字段。各业务页复用同一组件，避免每个页面各写一套选择弹窗。
 */
import { ref, computed, watch } from 'vue'

const props = withDefaults(defineProps<{
  open: boolean
  title?: string
  /** 表格列（a-table 列定义） */
  columns: any[]
  /** 候选数据源 */
  dataSource: any[]
  /** 加载中 */
  loading?: boolean
  /** 唯一键字段名 */
  rowKey?: string
  /** 搜索框占位提示 */
  searchPlaceholder?: string
  /** 参与关键字过滤的字段名（默认 name / code / customerName / warehouseName） */
  keywordFields?: string[]
  /** 弹窗宽度 */
  width?: number
}>(), {
  title: '选择',
  loading: false,
  rowKey: 'id',
  searchPlaceholder: '请输入关键字过滤',
  keywordFields: () => ['name', 'code', 'customerName', 'warehouseName', 'contactName'],
  width: 760,
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  'select': [record: any]
}>()

const keyword = ref('')
const selectedRecord = ref<any>(null)

// 弹窗重新打开时清空上一次的搜索与选中
watch(() => props.open, (val) => {
  if (val) {
    keyword.value = ''
    selectedRecord.value = null
  }
})

const filteredData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.dataSource
  return props.dataSource.filter((row: any) =>
    props.keywordFields.some(f => String(row?.[f] ?? '').toLowerCase().includes(kw))
  )
})

function customRow(record: any) {
  return {
    onClick: () => { selectedRecord.value = record },
    onDblclick: () => { selectedRecord.value = record; handleConfirm() },
    style: { cursor: 'pointer' },
  }
}

function rowClassName(record: any) {
  return selectedRecord.value && record?.[props.rowKey] === selectedRecord.value?.[props.rowKey]
    ? 'master-select-row-active'
    : ''
}

function handleConfirm() {
  if (!selectedRecord.value) return
  emit('select', selectedRecord.value)
  handleClose()
}

function handleClose() {
  emit('update:open', false)
}
</script>

<style scoped>
.master-select-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
:deep(.master-select-row-active) > td {
  background-color: #e6f7ff !important;
}
</style>
