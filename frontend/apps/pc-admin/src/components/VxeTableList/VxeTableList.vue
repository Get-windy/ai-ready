<template>
  <div ref="containerRef" class="vxe-table-list-container">
    <!-- 顶部工具栏 -->
    <div v-if="showToolbar" class="table-toolbar">
      <div class="toolbar-left">
        <a-space>
          <a-button v-if="showAdd" type="primary" @click="emit('add')">
            <template #icon><PlusOutlined /></template>
            {{ addText }}
          </a-button>
          <slot name="toolbar-actions" />
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-space>
          <a-tooltip title="筛选面板">
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
            v-if="showSearch"
            v-model:value="searchKeyword"
            :placeholder="searchPlaceholder"
            style="width: 200px"
            size="small"
            allow-clear
            @search="handleSearch"
          />
          <a-tooltip title="刷新">
            <a-button size="small" @click="emit('refresh')">
              <template #icon><ReloadOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="showExport" title="导出">
            <a-button size="small" @click="emit('export')">
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
    <div v-if="selectedRows.length > 0" class="batch-bar">
      <a-space>
        <span class="batch-info">已选择 {{ selectedRows.length }} 项</span>
        <a-button v-if="showBatchDelete" danger size="small" @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
        <slot name="batch-actions" :selected-rows="selectedRows" />
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
    >
      <!-- 列定义：使用 v-for + v-bind 替代 :columns 动态 prop -->
      <vxe-column
        v-for="col in vxeColumns"
        :key="col.field || col.type"
        v-bind="col"
      />

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

// ═══════════════════════════════════════════════════════════
// 🐛 调试：追踪 VxeTableList 渲染链路
const DEBUG_TAG = '[VxeTableList:DEBUG]'
const debugLog = (...args: any[]) => console.log(DEBUG_TAG, ...args)
// ═══════════════════════════════════════════════════════════
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ExportOutlined,
  FilterOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import type { VxeTableInstance, VxeTablePropTypes } from 'vxe-table'

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
}>()

// ========== State ==========
const tableRef = ref<VxeTableInstance>()
const searchKeyword = ref('')
const showFilterPanel = ref(false)
const selectedRows = ref<any[]>([])
const filterValues = reactive<Record<string, any>>({})

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

  // 🐛 调试：检查输入列
  debugLog('props.columns:', props.columns, 'isArray:', Array.isArray(props.columns), 'selectable:', props.selectable)

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

  // 🐛 调试：输出最终列定义
  debugLog('vxeColumns 产出:', cols.length, '列')
  if (cols.length > 0) {
    debugLog('  列 fields:', cols.map(c => c.field || c.type).join(', '))
    debugLog('  列 titles:', cols.map(c => c.title || c.type).join(', '))
  }

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

  // 🐛 调试
  debugLog('tableData: dataSource 长度 =', props.dataSource?.length)

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

  // 空行填充（最少 20 行以保证表格视觉完整）
  const minRows = 20
  const emptyCount = Math.max(0, minRows - deduped.length)
  for (let i = 0; i < emptyCount; i++) {
    deduped.push({
      [keyField]: `${_fillerNs}${_fillerSeq++}`,
      __empty_row: true,
    })
  }

  // 🐛 调试
  debugLog('tableData: 最终行数 =', deduped.length, '(真实:', deduped.length - emptyCount, '填充:', emptyCount, ')')

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
        // 查找对应的汇总数据
        const summaryItem = props.summaryData?.find((s: any) => s.label && col.field)
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
  selectedRows.value = records
  emit('selection-change', records, records.map(r => r[props.rowKey]))
}

function handleCheckboxAll({ records }: any) {
  selectedRows.value = records
  emit('selection-change', records, records.map(r => r[props.rowKey]))
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

function handleBatchDelete() {
  if (selectedRows.value.length === 0) return
  emit('batch-delete', selectedRows.value.map(r => r[props.rowKey]))
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
  // 🐛 调试
  debugLog('updateTableHeight 开始执行')

  // 获取容器元素
  nextTick(() => {
    const container = containerRef.value
    if (!container) {
      const fallback = Math.max(300, window.innerHeight - 300)
      // 🐛 调试
      debugLog('updateTableHeight: container 不存在，使用 fallback height =', fallback)
      tableHeight.value = fallback
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

    // 🐛 调试
    debugLog('updateTableHeight: containerHeight =', containerHeight, 'fixedHeight =', fixedHeight, 'tableHeight =', tableHeight.value)
  })
}

// ========== 暴露方法 ==========
defineExpose({
  clearSelection,
  selectedRows,
  getTableRef: () => tableRef.value,
  refresh: () => emit('refresh'),
})

// ========== 生命周期 ==========
onMounted(() => {
  // 🐛 调试
  debugLog('>>>> 组件挂载 <<<<')
  debugLog('  props.columns:', props.columns)
  debugLog('  props.dataSource:', props.dataSource)
  debugLog('  props.rowKey:', props.rowKey)
  debugLog('  tableRef.value 存在:', !!tableRef.value)
  debugLog('  containerRef.value 存在:', !!containerRef.value)

  updateTableHeight()

  // 🐛 调试（延迟检查 DOM + 样式）
  setTimeout(() => {
    if (containerRef.value) {
      const vxeTableEl = containerRef.value.querySelector('.vxe-table')
      const vxeHeaderWrapper = containerRef.value.querySelector('.vxe-table--header-wrapper')
      const vxeHeader = containerRef.value.querySelector('.vxe-table--header')
      const vxeBody = containerRef.value.querySelector('.vxe-table--body')
      debugLog('  [延迟检查] .vxe-table 存在:', !!vxeTableEl)
      debugLog('  [延迟检查] .vxe-header-wrapper 存在:', !!vxeHeaderWrapper)
      debugLog('  [延迟检查] .vxe-header 存在:', !!vxeHeader)
      debugLog('  [延迟检查] .vxe-body 存在:', !!vxeBody)
      debugLog('  [延迟检查] container 高度:', containerRef.value.getBoundingClientRect().height)
      debugLog('  [延迟检查] tableHeight:', tableHeight.value)

      if (vxeTableEl) {
        const cs = window.getComputedStyle(vxeTableEl)
        debugLog('  [CSS] table display:', cs.display, 'visibility:', cs.visibility, 'opacity:', cs.opacity)
        debugLog('  [CSS] table height:', cs.height, 'width:', cs.width)

        if (vxeHeaderWrapper) {
          const hws = window.getComputedStyle(vxeHeaderWrapper)
          debugLog('  [CSS] header-wrapper display:', hws.display, 'height:', hws.height, 'overflow:', hws.overflow)
          debugLog('  [HTML] header-wrapper innerHTML:', vxeHeaderWrapper.innerHTML.substring(0, 500))
        } else {
          debugLog('  [HTML] ⚠️ header-wrapper 不存在，vxe-table 完整 innerHTML:')
          debugLog('  [HTML]', vxeTableEl.innerHTML.substring(0, 800))
        }

        if (vxeHeader) {
          const hcs = window.getComputedStyle(vxeHeader)
          debugLog('  [CSS] header display:', hcs.display, 'height:', hcs.height)
          debugLog('  [HTML] header innerHTML:', vxeHeader.innerHTML.substring(0, 500))
        }

        if (vxeBody) {
          debugLog('  [HTML] body innerHTML 前300字:', vxeBody.innerHTML.substring(0, 300))
        }
      }
    }
  }, 500)

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
})

onUnmounted(() => {
  // 🐛 调试
  debugLog('>>>> 组件卸载 <<<<')
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
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
  border-top: 2px solid #d9d9d9 !important;
  border-right: 1px solid #e0e0e0 !important;
}

:deep(.vxe-table--footer .vxe-footer--column:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

</style>