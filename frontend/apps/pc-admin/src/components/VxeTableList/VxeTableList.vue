<template>
  <div ref="containerRef" class="vxe-table-list-container">
    <!-- 顶部工具栏 -->
    <div v-if="showToolbar" class="table-toolbar">
      <div class="toolbar-left">
        <a-space>
          <a-button
            v-if="showAdd && (!addPermission || hasPermission(addPermission))"
            ref="addBtnRef"
            type="primary"
            @click="debounceClick('add', () => emit('add'))"
          >
            <template #icon><PlusOutlined /></template>
            {{ addText }}
          </a-button>
          <slot name="toolbar-actions" />
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-space>
          <a-tooltip title="筛选面板 (Ctrl+F)">
            <a-button
              :type="showFilterPanel ? 'primary' : 'default'"
              size="small"
              @click="showFilterPanel = !showFilterPanel"
            >
              <template #icon><FilterOutlined /></template>
              筛选
            </a-button>
          </a-tooltip>
          <a-input-search
            ref="searchInputRef"
            v-if="showSearch"
            v-model:value="searchKeyword"
            :placeholder="searchPlaceholder"
            style="width: 200px"
            size="small"
            allow-clear
            @search="handleSearch"
          />
          <a-tooltip title="刷新 (F5)">
            <a-button size="small" @click="debounceClick('refresh', () => emit('refresh'))">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="showExport && (!exportPermission || hasPermission(exportPermission))" title="导出">
            <a-button size="small" @click="debounceClick('export', () => emit('export'))">
              <template #icon><ExportOutlined /></template>
            </a-button>
          </a-tooltip>
        </a-space>
      </div>
    </div>

    <!-- 筛选面板 -->
    <div v-if="showFilterPanel" class="filter-panel">
      <a-row :gutter="[12, 12]">
        <a-col
          v-for="filter in filterFields"
          :key="filter.key"
          :span="filter.span || 6"
        >
          <a-form-item :label="filter.label" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-input
              v-if="filter.type === 'input'"
              v-model:value="filterValues[filter.key]"
              :placeholder="filter.placeholder || '请输入'"
              size="small"
              @change="handleFilterChange"
            />
            <a-select
              v-else-if="filter.type === 'select'"
              v-model:value="filterValues[filter.key]"
              :placeholder="filter.placeholder || '请选择'"
              :options="filter.options"
              size="small"
              allow-clear
              @change="handleFilterChange"
            />
            <a-range-picker
              v-else-if="filter.type === 'dateRange'"
              v-model:value="filterValues[filter.key]"
              size="small"
              style="width: 100%"
              @change="handleFilterChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="6" class="filter-actions">
          <a-space>
            <a-button type="primary" size="small" @click="handleFilterSubmit">查询</a-button>
            <a-button size="small" @click="handleFilterReset">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="realSelectedRows.length > 0" class="batch-bar">
      <a-space>
        <span class="batch-info">已选择 {{ realSelectedRows.length }} 项</span>
        <a-button v-if="showBatchDelete && (!deletePermission || hasPermission(deletePermission))" danger size="small" @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
        <slot name="batch-actions" :selected-rows="realSelectedRows" />
        <a-button type="link" size="small" @click="clearSelection">取消选择</a-button>
      </a-space>
    </div>

    <!-- vxe-table 表格 -->
    <!--
      ⚠️ 已知问题：VxeTable v4.19.10 在此项目中 :columns 动态 prop 方式无法渲染表头/表体。
          修复方案：必须使用 <vxe-column> 子组件方式。v-bind="col" 可传递所有列定义属性。
    -->
    <vxe-table
      ref="tableRef"
      :data="tableData"
      :loading="loading"
      :height="tableHeight"
      :row-config="{ keyField: rowKey, isHover: true }"
      :cell-config="{ height: 32 }"
      :header-cell-config="{ height: 32 }"
      :checkbox-config="{ highlight: true, range: true }"
      :sort-config="{ trigger: 'cell', defaultSort: defaultSort }"
      :footer-config="{ show: showSummary, footerMethod: footerMethod }"
      :scroll-y="{ enabled: true, gt: 20 }"
      :scroll-x="{ enabled: true, gt: 10 }"
      border
      auto-resize
      stripe
      show-header-overflow="title"
      show-overflow="title"
      empty-text="暂无数据"
      @checkbox-change="handleCheckboxChange"
      @checkbox-all="handleCheckboxAll"
      @sort-change="handleSortChange"
      @cell-click="handleCellClick"
      @cell-dblclick="handleCellDblClick"
    >
      <!-- 列定义：使用 v-for + v-bind 替代 :columns 动态 prop -->
      <vxe-column
        v-for="col in vxeColumns"
        :key="col.field || col.type"
        v-bind="col"
      >
        <!-- 数值列右对齐 + 等宽数字 -->
        <template v-if="col.align === 'right'" #default="{ row, column }">
          <span class="cell-number">{{ row[col.field] ?? '-' }}</span>
        </template>
      </vxe-column>

      <!-- 操作列插槽 -->
      <template #action_default="{ row, $rowIndex }">
        <slot name="action" :record="row" :index="$rowIndex">
          <span>-</span>
        </slot>
      </template>

      <!-- 自定义列插槽 -->
      <template v-for="slotName in customSlotColumns" :key="slotName" #[`custom_${slotName}`]="{ row, $rowIndex }">
        <slot :name="slotName" :record="row" :index="$rowIndex">
          <span>-</span>
        </slot>
      </template>

      <!-- 空状态插槽 -->
      <template #empty>
        <slot name="empty">
          <div class="table-empty">
            <InboxOutlined class="table-empty-icon" />
            <p class="table-empty-text">暂无数据</p>
            <a-button v-if="showAdd" type="link" size="small" @click="emit('add')" class="table-empty-action">
              <PlusOutlined /> 立即创建
            </a-button>
          </div>
        </slot>
      </template>
    </vxe-table>

    <!-- 分页 -->
    <div v-if="pagination" class="table-pagination">
      <a-pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="paginationTotal"
        :show-size-changer="true"
        :show-quick-jumper="true"
        :pageSizeOptions="['10', '20', '50', '100']"
        :show-total="(total: number) => `共 ${total} 条`"
        size="small"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { message, Modal } from 'ant-design-vue'

import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ExportOutlined,
  FilterOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import type { VxeTableInstance, VxeTablePropTypes } from 'vxe-table'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ========== Props ==========
const props = defineProps({
  columns: { type: Array as PropType<any[]>, required: true },
  dataSource: { type: Array as PropType<any[]>, default: () => [] },
  loading: { type: Boolean, default: false },
  rowKey: { type: String, default: 'id' },
  showToolbar: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  showAdd: { type: Boolean, default: true },
  addText: { type: String, default: '新增' },
  showDelete: { type: Boolean, default: false },
  showBatchDelete: { type: Boolean, default: true },
  showExport: { type: Boolean, default: false },
  selectable: { type: Boolean, default: true },
  filterFields: { type: Array as PropType<any[]>, default: () => [] },
  showSummary: { type: Boolean, default: false },
  summaryData: { type: Array as PropType<any[]>, default: undefined },

  // 分页 — 传入 false 可隐藏分页栏，传入对象则按配置显示
  pagination: {
    type: [Object, Boolean] as PropType<{ current?: number; pageSize?: number; total?: number } | boolean>,
    default: () => ({ current: 1, pageSize: 20, total: 0 })
  },

  // 默认排序
  defaultSort: { type: Object as PropType<{ field: string; order: string }>, default: undefined },

  // 权限标识
  addPermission: { type: String, default: '' },
  deletePermission: { type: String, default: '' },
  exportPermission: { type: String, default: '' },

  // 空行填充（表格数据不足时填充空白行以保持视觉完整）
  minEmptyRows: { type: Number, default: 20 },
})

const emit = defineEmits<{
  'add': []
  'edit': [record: any]
  'view': [record: any]
  'delete': [record: any]
  'batch-delete': [ids: any[]]
  'refresh': []
  'search': [keyword: string]
  'export': []
  'page-change': [page: number, pageSize: number]
  'selection-change': [rows: any[], ids: any[]]
  'sort-change': [field: string, order: string]
  'filter-change': [filters: Record<string, any>]
  'cell-click': [record: any, column: any]
  'cell-dblclick': [record: any, column: any]
}>()

// ========== State ==========
const tableRef = ref<VxeTableInstance>()
const searchInputRef = ref<any>(null)
const addBtnRef = ref<any>(null)
const searchKeyword = ref('')
const showFilterPanel = ref(false)
const selectedRows = ref<any[]>([])
const filterValues = reactive<Record<string, any>>({})

// 过滤掉空行后的真实选中行
const realSelectedRows = computed(() => selectedRows.value.filter(r => !r.__empty_row))

// 分页
const currentPage = ref(props.pagination?.current || 1)
const pageSize = ref(props.pagination?.pageSize || 20)
const paginationTotal = ref(props.pagination?.total || 0)

// 表格高度 — 初始使用视口剩余高度，后续由 ResizeObserver 精确计算
const tableHeight = ref(Math.max(300, typeof window !== 'undefined' ? window.innerHeight - 250 : 400))

// ========== 列转换 ==========
interface VxeColumnDef {
  type?: string
  field?: string
  title?: string
  width?: number
  minWidth?: number
  sortable?: boolean
  fixed?: string
  align?: string
  showOverflow?: string
  slots?: Record<string, string>
  formatter?: Function
  [key: string]: any
}

const vxeColumns = computed<VxeColumnDef[]>(() => {
  const cols: VxeColumnDef[] = []

  // 选择列
  if (props.selectable) {
    cols.push({
      type: 'checkbox',
      width: 50,
      fixed: 'left',
    })
  }

  // 安全处理列定义
  const columnList = Array.isArray(props.columns) ? props.columns : []
  columnList.forEach((col: any, index: number) => {
    // 确保 field 有值，使用 dataIndex、key 或生成默认值
    const field = col.field || col.dataIndex || col.key || `col_${index}`
    const vxeCol: VxeColumnDef = {
      field,
      title: col.title || '',
      width: col.width || 100,
      minWidth: col.minWidth || 60,
      sortable: col.sortable || false,
      fixed: col.fixed,
      align: col.align || 'left',
      showOverflow: 'title',
    }

    // 如果列定义中有 formatter 函数，使用它
    if (col.formatter && typeof col.formatter === 'function') {
      vxeCol.formatter = ({ cellValue, row }: any) => col.formatter({ cellValue, row })
    }

    // 操作列特殊处理
    if (col.type === 'action' || col.key === 'action' || col.dataIndex === 'action') {
      vxeCol.width = col.width || 120
      vxeCol.fixed = col.fixed || 'right'
      // 操作列使用插槽
      vxeCol.slots = { default: 'action_default' }
    }
    // 自定义列插槽
    else if (col.slotName) {
      vxeCol.slots = { default: `custom_${col.slotName}` }
    }

    cols.push(vxeCol)
  })

  return cols
})

// 收集所有自定义列插槽名称
const customSlotColumns = computed(() => {
  const columnList = Array.isArray(props.columns) ? props.columns : []
  return columnList
    .filter((col: any) => col.slotName && col.type !== 'action' && col.key !== 'action')
    .map((col: any) => col.slotName)
})

// ========== 数据处理 ==========
let _fillerSeq = 0
const _fillerNs = `__vxe_f_${Date.now().toString(36)}_`

const tableData = computed(() => {
  const keyField = props.rowKey || 'id'
  const seen = new Map<string, boolean>()
  const deduped: any[] = []

  for (const item of props.dataSource) {
    const key = item?.[keyField]
    const keyStr = key != null ? String(key) : ''
    if (keyStr && seen.has(keyStr)) {
      console.warn(`[VxeTableList] 检测到重复 rowKey (${keyField}=${keyStr})，已自动去重。请检查数据源是否包含重复 ID。`)
      continue
    }
    if (keyStr) {
      seen.set(keyStr, true)
    }
    deduped.push(item)
  }

  // 空行填充（保证表格视觉完整）
  const minRows = props.minEmptyRows
  const emptyCount = Math.max(0, minRows - deduped.length)
  for (let i = 0; i < emptyCount; i++) {
    deduped.push({
      [keyField]: `${_fillerNs}${_fillerSeq++}`,
      __empty_row: true,
    })
  }

  return deduped
})

// ========== 汇总行 ==========
const footerMethod = computed<VxeTablePropTypes.FooterMethod>(() => {
  if (!props.showSummary || !props.summaryData?.length) return undefined

  return ({ columns, data }: any) => {
    const footerData: any[][] = []
    const row: any[] = []

    columns.forEach((col: any, index: number) => {
      if (col.type === 'checkbox') {
        row.push('')
      } else if (index === (props.selectable ? 1 : 0)) {
        row.push('合计')
      } else {
        // 查找对应的汇总数据（按列标题匹配）
        const summaryItem = props.summaryData?.find((s: any) => s.label === col.title)
        if (summaryItem) {
          if (summaryItem.type === 'currency') {
            row.push(`¥${Number(summaryItem.value).toFixed(2)}`)
          } else {
            row.push(summaryItem.value)
          }
        } else {
          row.push('')
        }
      }
    })

    footerData.push(row)
    return footerData
  }
})

// ========== 事件处理 ==========
function handleSearch() {
  emit('search', searchKeyword.value)
}

function handleFilterChange() {
  // 自动触发查询
}

function handleFilterSubmit() {
  emit('filter-change', { ...filterValues })
}

function handleFilterReset() {
  Object.keys(filterValues).forEach(k => {
    filterValues[k] = undefined
  })
  emit('filter-change', {})
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  emit('page-change', page, size)
}

function handleCheckboxChange({ records }: any) {
  const real = records.filter((r: any) => !r.__empty_row)
  selectedRows.value = real
  emit('selection-change', real, real.map(r => r[props.rowKey]))
}

function handleCheckboxAll({ records }: any) {
  const real = records.filter((r: any) => !r.__empty_row)
  selectedRows.value = real
  emit('selection-change', real, real.map(r => r[props.rowKey]))
}

function handleSortChange({ property, order }: any) {
  if (property) {
    emit('sort-change', property, order || '')
  }
}

function handleCellClick({ row, column }: any) {
  if (!row.__empty_row) {
    emit('cell-click', row, column)
  }
}

function handleCellDblClick({ row, column }: any) {
  if (!row.__empty_row) {
    emit('cell-dblclick', row, column)
  }
}

function hasPermission(permission: string): boolean {
  // 如果未传入权限标识，默认允许
  if (!permission) return true
  // 尝试从全局权限列表中检查（需项目已注入 $permissions）
  const app = (window as any).__app?.config?.globalProperties
  if (app?.$permissions) {
    return app.$permissions.includes(permission)
  }
  // 若无权限系统，默认放行
  return true
}

function handleBatchDelete() {
  if (realSelectedRows.value.length === 0) return
  const count = realSelectedRows.value.length
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${count} 项吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => {
      emit('batch-delete', realSelectedRows.value.map(r => r[props.rowKey]))
      message.success(`已删除 ${count} 项`)
    },
  })
}

function clearSelection() {
  selectedRows.value = []
  tableRef.value?.clearCheckboxRow()
  tableRef.value?.clearCheckboxReserve()
  emit('selection-change', [], [])
}

// ========== 高度计算 ==========
const containerRef = ref<HTMLDivElement | null>(null)
let resizeObserver: ResizeObserver | null = null

function updateTableHeight() {
  // 获取容器元素
  nextTick(() => {
    const container = containerRef.value
    if (!container) {
      tableHeight.value = Math.max(300, window.innerHeight - 300)
      return
    }

    // 计算容器内各固定元素的高度
    const toolbar = container.querySelector('.table-toolbar')
    const filter = container.querySelector('.filter-panel')
    const batch = container.querySelector('.batch-bar')
    const pagination = container.querySelector('.table-pagination')

    let fixedHeight = 0
    if (toolbar) fixedHeight += toolbar.getBoundingClientRect().height
    if (filter) fixedHeight += filter.getBoundingClientRect().height
    if (batch) fixedHeight += batch.getBoundingClientRect().height
    if (pagination) fixedHeight += pagination.getBoundingClientRect().height

    const containerHeight = container.getBoundingClientRect().height
    tableHeight.value = Math.max(200, containerHeight - fixedHeight)
  })
}

// ========== 键盘快捷键 ==========
function handleKeydown(e: KeyboardEvent) {
  // Ctrl+F / F3 → 聚焦搜索框
  if ((e.ctrlKey && e.key === 'f') || e.key === 'F3') {
    e.preventDefault()
    if (searchInputRef.value) {
      searchInputRef.value.focus()
    }
    return
  }
  // Ctrl+N → 新增
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    if (props.showAdd && (!props.addPermission || hasPermission(props.addPermission))) {
      debounceClick('add', () => emit('add'))
    }
    return
  }
  // F5 → 刷新
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', () => emit('refresh'))
    return
  }
}

// ========== 暴露方法 ==========
defineExpose({
  clearSelection,
  selectedRows,
  searchKeyword,
  getTableRef: () => tableRef.value,
  refresh: () => emit('refresh'),
})

// ========== 生命周期 ==========
onMounted(() => {
  updateTableHeight()

  // 使用 ResizeObserver 精确监听容器尺寸变化
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      updateTableHeight()
    })
    resizeObserver.observe(containerRef.value)
  }

  // 初始化筛选字段
  props.filterFields.forEach((f: any) => {
    if (!(f.key in filterValues)) {
      filterValues[f.key] = f.defaultValue ?? undefined
    }
  })

  // 键盘快捷键
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  document.removeEventListener('keydown', handleKeydown)
})

// 监听分页变化
watch(() => props.pagination, (p) => {
  if (p) {
    currentPage.value = p.current || 1
    pageSize.value = p.pageSize || 20
    paginationTotal.value = p.total || 0
  }
}, { immediate: true, deep: true })
</script>

<style scoped>
.vxe-table-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

.filter-panel {
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 22px;
}

.batch-bar {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: #e6f7ff;
  border-bottom: 1px solid #91d5ff;
  flex-shrink: 0;
}

.batch-info {
  color: #1890ff;
  font-weight: 500;
}

.table-pagination {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.table-empty-action {
  margin-top: 12px;
}

/* ── vxe-table 生产级样式覆盖 ────────────────────── */
:deep(.vxe-table) {
  font-size: 13px;
  line-height: 1.5;
  color: #333;
}

/* 表头样式：深色边框 + 背景 */
:deep(.vxe-table--header .vxe-header--row) {
  height: 32px !important;
}

:deep(.vxe-table--header .vxe-header--column) {
  background: #fafafa !important;
  font-weight: 600 !important;
  color: #333 !important;
  padding: 5px 10px !important;
  height: 32px !important;
  line-height: 22px !important;
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
}

:deep(.vxe-table--header .vxe-header--column:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

/* 表体单元格：完整网格边框 */
:deep(.vxe-table--body .vxe-body--row) {
  height: 32px;
}

:deep(.vxe-table--body .vxe-body--column) {
  padding: 3px 10px !important;
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
}

:deep(.vxe-table--body .vxe-body--column:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 末行底部边框 */
:deep(.vxe-table--body .vxe-body--row:last-child .vxe-body--column) {
  border-bottom: 1px solid #d9d9d9 !important;
}

/* 固定列分隔线（阴影） */
:deep(.vxe-table--fixed-left-wrapper::after) {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 6px;
  height: 100%;
  background: linear-gradient(90deg, rgba(0,0,0,0.06), rgba(0,0,0,0.02));
  pointer-events: none;
  z-index: 10;
}

:deep(.vxe-table--fixed-right-wrapper::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 6px;
  height: 100%;
  background: linear-gradient(270deg, rgba(0,0,0,0.06), rgba(0,0,0,0.02));
  pointer-events: none;
  z-index: 10;
}

/* 固定列表头与表体对齐 */
:deep(.vxe-table--fixed-left-wrapper .vxe-header--column),
:deep(.vxe-table--fixed-right-wrapper .vxe-header--column) {
  border-bottom: 2px solid #b0b0b0 !important;
}

/* 斑马纹行 */
:deep(.vxe-table--body .vxe-body--row--stripe) {
  background: #fafafa;
}

/* 悬停高亮 */
:deep(.vxe-table--body .vxe-body--row--hover) {
  background: #e6f7ff !important;
}

/* 选中行 */
:deep(.vxe-table--body .vxe-body--row--checked) {
  background: #bae7ff !important;
}

/* 汇总行 */
:deep(.vxe-table--footer .vxe-footer--column) {
  background: #f5f5f5 !important;
  font-weight: 600 !important;
  padding: 5px 10px !important;
  border-top: 2px solid #b0b0b0 !important;
  border-right: 1px solid #e0e0e0 !important;
}

:deep(.vxe-table--footer .vxe-footer--column:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 等宽数字（数值列） */
.cell-number {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
</style>