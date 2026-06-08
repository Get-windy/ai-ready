<template>
  <div
    ref="containerRef"
    class="table-list-container"
    :class="{ 'table-list-container--no-toolbar': !showToolbar }"
  >
    <!-- 顶部工具栏 -->
    <div v-if="showToolbar" class="table-toolbar">
      <div class="toolbar-left">
        <slot name="toolbar-left">
          <a-space>
            <template v-if="showAdd">
              <a-button v-if="addPermission" v-permission="addPermission" type="primary" @click="handleAdd">
                <template #icon><PlusOutlined /></template>
                {{ addText }}
              </a-button>
              <a-button v-else type="primary" @click="handleAdd">
                <template #icon><PlusOutlined /></template>
                {{ addText }}
              </a-button>
            </template>
            <slot name="toolbar-actions" />
          </a-space>
        </slot>
      </div>
      <div class="toolbar-right">
        <slot name="toolbar-right">
          <a-space>
            <!-- 筛选面板切换 -->
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

            <!-- 列设置 -->
            <a-tooltip title="列设置">
              <a-popover
                v-model:open="columnSettingsVisible"
                trigger="click"
                placement="bottomRight"
                overlay-class-name="column-settings-popover"
              >
                <template #content>
                  <div class="column-settings">
                    <div class="column-settings-header">
                      <span>显示列</span>
                      <a-button type="link" size="small" @click="resetColumnConfig">重置</a-button>
                    </div>
                    <div class="column-settings-list">
                      <div
                        v-for="(col, index) in editableColumns"
                        :key="col.dataIndex"
                        class="column-settings-item"
                        draggable="true"
                        @dragstart="handleDragStart(index, $event)"
                        @dragover.prevent="handleDragOver(index)"
                        @drop="handleDrop(index)"
                      >
                        <a-checkbox
                          v-model:checked="col._hidden"
                          :disabled="col.required"
                        >
                          <span class="column-drag-handle">⠿</span>
                          {{ col.title }}
                        </a-checkbox>
                      </div>
                    </div>
                  </div>
                </template>
                <a-button size="small">
                  <template #icon><SettingOutlined /></template>
                  列
                </a-button>
              </a-popover>
            </a-tooltip>

            <!-- 视图保存 -->
            <a-tooltip title="保存当前视图">
              <a-button size="small" @click="showSaveViewModal = true">
                <template #icon><SaveOutlined /></template>
              </a-button>
            </a-tooltip>

            <!-- 视图切换 -->
            <a-dropdown v-if="savedViews.length > 0">
              <a-button size="small">
                <template #icon><AppstoreOutlined /></template>
                视图
              </a-button>
              <template #overlay>
                <a-menu @click="handleViewSelect">
                  <a-menu-item
                    v-for="view in savedViews"
                    :key="view.name"
                  >
                    <CheckOutlined v-if="currentViewName === view.name" />
                    {{ view.name }}
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="__manage__">
                    <SettingOutlined /> 管理视图
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>

            <a-input-search
              v-if="showSearch"
              v-model:value="searchKeyword"
              :placeholder="searchPlaceholder"
              style="width: 220px"
              size="small"
              allow-clear
              @search="handleSearch"
              @press-enter="handleSearch(searchKeyword)"
            />
            <a-tooltip title="刷新">
              <a-button size="small" @click="handleRefresh">
                <template #icon><ReloadOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="showExport" title="导出">
              <a-button size="small" @click="handleExport">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <slot name="toolbar-extra" />
          </a-space>
        </slot>
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
            <!-- 文本输入 -->
            <a-input
              v-if="filter.type === 'input'"
              v-model:value="filterValues[filter.key]"
              :placeholder="filter.placeholder || '请输入'"
              size="small"
              @change="handleFilterChange"
            />
            <!-- 下拉选择 -->
            <a-select
              v-else-if="filter.type === 'select'"
              v-model:value="filterValues[filter.key]"
              :placeholder="filter.placeholder || '请选择'"
              :options="filter.options"
              size="small"
              allow-clear
              @change="handleFilterChange"
            />
            <!-- 日期范围 -->
            <a-date-picker
              v-else-if="filter.type === 'date'"
              v-model:value="filterValues[filter.key]"
              :placeholder="filter.placeholder || '选择日期'"
              size="small"
              style="width: 100%"
              @change="handleFilterChange"
            />
            <!-- 日期区间 -->
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
    <div v-if="selectedRowKeys.length > 0" class="batch-bar">
      <a-space>
        <span class="batch-info">已选择 {{ selectedRowKeys.length }} 项</span>
        <a-button v-if="showBatchDelete" danger size="small" @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除
        </a-button>
        <a-button size="small" @click="handleBatchEdit">
          <template #icon><EditOutlined /></template>
          批量编辑
        </a-button>
        <slot name="batch-actions" />
        <a-button type="link" size="small" @click="clearSelection">取消选择</a-button>
      </a-space>
    </div>

    <!-- 表格主体 -->
    <a-table
      ref="tableRef"
      :columns="processedColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="paginationConfig"
      :row-selection="rowSelection"
      :row-key="rowKey"
      :scroll="scrollConfigComputed"
      :bordered="true"
      :size="tableSize"
      :custom-row="customRowFn"
      :locale="tableLocale"
      @change="handleTableChange"
      @resize-column="handleColumnResize"
    >
      <!-- 自定义列插槽 / 主体单元格渲染 -->
      <template #bodyCell="{ column, record, index }">
        <!-- 空占位行：不渲染内容，只保留边框 -->
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <!-- 自定义插槽列 -->
        <template v-else-if="column.slotName">
          <slot
            :name="column.slotName"
            :record="record"
            :index="index"
            :column="column"
          />
        </template>
        <!-- 操作列 -->
        <template v-else-if="column.type === 'action'">
          <div class="action-cell-inner">
            <slot name="action" :record="record" :index="index">
              <a-space :size="4">
                <a-tooltip v-if="showView" title="查看">
                  <a-button type="link" size="small" @click="handleView(record)">
                    <template #icon><EyeOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-tooltip v-if="showEdit" title="编辑">
                  <a-button type="link" size="small" @click="handleEdit(record)">
                    <template #icon><EditOutlined /></template>
                  </a-button>
                </a-tooltip>
                <a-popconfirm
                  v-if="showDelete"
                  title="确定要删除该项吗？"
                  @confirm="handleDelete(record)"
                >
                  <a-tooltip title="删除">
                    <a-button type="link" size="small" danger>
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-tooltip>
                </a-popconfirm>
              </a-space>
            </slot>
          </div>
        </template>
        <!-- 状态列 -->
        <template v-else-if="column.type === 'status'">
          <span class="status-cell-inner">
            <a-tag :color="getStatusColor(record[column.dataIndex], column.statusMap)">
              {{ getStatusText(record[column.dataIndex], column.statusMap) }}
            </a-tag>
          </span>
        </template>
        <!-- 日期列 -->
        <template v-else-if="column.type === 'date'">
          <span :title="formatDate(record[column.dataIndex], column.dateFormat)">
            {{ formatDate(record[column.dataIndex], column.dateFormat) }}
          </span>
        </template>
        <!-- 金额列 -->
        <template v-else-if="column.type === 'currency'">
          <span class="currency-value">
            ¥{{ formatNumber(record[column.dataIndex], 'currency') }}
          </span>
        </template>
        <!-- 数字列 -->
        <template v-else-if="column.type === 'number'">
          <span class="number-value">
            {{ formatNumber(record[column.dataIndex], column.numberFormat) }}
          </span>
        </template>
        <!-- 省略文本 -->
        <template v-else-if="column.type === 'ellipsis'">
          <a-tooltip :title="record[column.dataIndex]">
            <span class="ellipsis-text">{{ record[column.dataIndex] }}</span>
          </a-tooltip>
        </template>
        <!-- 链接列 -->
        <template v-else-if="column.type === 'link'">
          <a class="cell-link" @click="handleCellClick(record, column)">{{ record[column.dataIndex] }}</a>
        </template>
      </template>

      <!-- 汇总行（集成在表格内部，固定在底部不参与滚动） -->
      <template v-if="hasSummary || $slots.summary" #summary>
        <tr class="table-summary-row">
          <template v-if="$slots.summary">
            <slot name="summary" />
          </template>
          <template v-else>
            <td
              v-for="(col, idx) in processedColumns"
              :key="col.key || col.dataIndex || idx"
              :class="getSummaryCellClass(col)"
            >
              {{ getSummaryCellValue(col) }}
            </td>
          </template>
        </tr>
      </template>

      <!-- 空状态 -->
      <template #emptyText>
        <slot name="empty">
          <a-empty :description="emptyText">
            <template #image>
              <component :is="emptyIcon" v-if="emptyIcon" />
            </template>
          </a-empty>
        </slot>
      </template>

      <!-- 展开行 -->
      <template v-if="$slots.expandedRowRender" #expandedRowRender="{ record }">
        <slot name="expandedRowRender" :record="record" />
      </template>
    </a-table>

    <!-- 保存视图 Modal -->
    <a-modal
      v-model:open="showSaveViewModal"
      title="保存视图"
      :width="400"
      @ok="handleSaveView"
    >
      <a-form layout="vertical">
        <a-form-item label="视图名称">
          <a-input v-model:value="newViewName" placeholder="输入视图名称..." />
        </a-form-item>
        <a-form-item label="默认视图">
          <a-switch v-model:checked="newViewIsDefault" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 管理视图 Modal -->
    <a-modal
      v-model:open="showManageViewModal"
      title="管理视图"
      :width="500"
      :footer="null"
    >
      <a-list :data-source="savedViews">
        <template #renderItem="{ item, index }">
          <a-list-item>
            <a-list-item-meta :title="item.name">
              <template #description>
                {{ item.columnCount }}列 · {{ item.isDefault ? '默认' : '' }}
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button
                type="link"
                danger
                size="small"
                @click="savedViews.splice(index, 1); persistSavedViews()"
              >
                删除
              </a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ExportOutlined,
  FilterOutlined,
  SettingOutlined,
  SaveOutlined,
  AppstoreOutlined,
  EyeOutlined,
  EditOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { saveColumnConfig, loadColumnConfig, type ColumnConfig } from '@/utils/columnStorage'

// ========== 类型定义 ==========

export type ColumnType = 'text' | 'action' | 'status' | 'date' | 'number' | 'currency' | 'ellipsis' | 'link'

export interface TableColumn {
  title: string
  dataIndex: string
  key?: string
  width?: number | string
  minWidth?: number
  maxWidth?: number
  fixed?: 'left' | 'right' | false
  align?: 'left' | 'center' | 'right'
  ellipsis?: boolean
  sortable?: boolean
  type?: ColumnType
  slotName?: string
  dateFormat?: string
  numberFormat?: string
  statusMap?: Record<string | number, { text: string; color: string }>
  hidden?: boolean
  /** 是否必选列（不可隐藏） */
  required?: boolean
  /** 列分组 */
  children?: TableColumn[]
  /** 汇总值 */
  summaryValue?: string | number
  summaryType?: 'default' | 'currency' | 'number'
  [key: string]: any
}

export interface FilterField {
  key: string
  label: string
  type: 'input' | 'select' | 'date' | 'dateRange'
  placeholder?: string
  options?: { label: string; value: any }[]
  span?: number
  defaultValue?: any
}

export interface SummaryItem {
  label: string
  value: number | string
  type?: 'default' | 'currency' | 'danger' | 'warning'
}

export interface SavedView {
  name: string
  isDefault: boolean
  columnConfig: ColumnConfig[]
  filters: Record<string, any>
  sortField?: string
  sortOrder?: string
}

// ========== Props ==========

const props = defineProps({
  columns: { type: Array as PropType<TableColumn[]>, required: true },
  dataSource: { type: Array as PropType<any[]>, default: () => [] },
  loading: { type: Boolean, default: false },
  rowKey: { type: String, default: 'id' },
  tableSize: { type: String as PropType<'small' | 'middle' | 'large'>, default: 'middle' },
  showToolbar: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  showAdd: { type: Boolean, default: true },
  addText: { type: String, default: '新增' },
  addPermission: { type: String, default: undefined },
  showEdit: { type: Boolean, default: true },
  showView: { type: Boolean, default: false },
  showDelete: { type: Boolean, default: true },
  showBatchDelete: { type: Boolean, default: true },
  showExport: { type: Boolean, default: false },
  selectable: { type: Boolean, default: true },
  emptyText: { type: String, default: '暂无数据' },
  emptyIcon: { type: Object, default: undefined },

  // 分页
  pagination: {
    type: Object as PropType<{
      current?: number; pageSize?: number; total?: number
      showSizeChanger?: boolean; showQuickJumper?: boolean
      pageSizeOptions?: string[]; showTotal?: boolean | ((total: number, range: [number, number]) => string)
    }>,
    default: () => ({
      current: 1, pageSize: 10, total: 0,
      showSizeChanger: true, showQuickJumper: true,
      pageSizeOptions: ['10', '20', '50', '100'],
      showTotal: (total: number) => `共 ${total} 条`
    })
  },

  // 筛选
  filterFields: { type: Array as PropType<FilterField[]>, default: () => [] },

  // 表格
  scroll: { type: Object as PropType<{ x?: number | string; y?: number | string }>, default: () => ({ x: '100%' }) },
  customRow: { type: Function, default: undefined },

  // 表格标识（用于持久化列配置）
  tableKey: { type: String, default: '' },

  // 行内编辑
  editable: { type: Boolean, default: false },

  // 汇总行（内置）
  showSummary: { type: Boolean, default: false },
  summaryData: { type: Array as PropType<SummaryItem[]>, default: undefined },
})

const emit = defineEmits<{
  'add': []
  'edit': [record: any]
  'view': [record: any]
  'delete': [record: any]
  'batch-delete': [ids: any[]]
  'batch-edit': [ids: any[]]
  'refresh': []
  'search': [keyword: string]
  'export': []
  'page-change': [page: number, pageSize: number]
  'selection-change': [keys: any[], rows: any[]]
  'sort-change': [field: string, order: string]
  'filter-change': [filters: Record<string, any>]
  'cell-click': [record: any, column: TableColumn]
}>()

// ========== State ==========

const searchKeyword = ref('')
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])
const showFilterPanel = ref(false)
const columnSettingsVisible = ref(false)
const showSaveViewModal = ref(false)
const showManageViewModal = ref(false)
const newViewName = ref('')
const newViewIsDefault = ref(false)
const tableRef = ref()

const filterValues = reactive<Record<string, any>>({})
const currentViewName = ref('')

// 容器及表格高度计算
const containerRef = ref<HTMLDivElement>()
const containerHeight = ref(0)
const tableScrollY = ref<number | undefined>(undefined)

// 行高常量（用于计算空行数）
const ROW_HEIGHT_MAP: Record<string, number> = { small: 40, middle: 50, large: 60 }
const getRowHeight = () => ROW_HEIGHT_MAP[props.tableSize] || 50

// 列配置存储键
const storageKey = computed(() => props.tableKey || `table-${props.columns.map(c => c.dataIndex).join('-')}`)

// ========== 筛选初始化 ==========

watch(() => props.filterFields, (fields) => {
  for (const f of fields) {
    if (!(f.key in filterValues)) {
      filterValues[f.key] = f.defaultValue ?? undefined
    }
  }
}, { immediate: true })

// ========== 列处理 ==========

interface EditableColumn extends TableColumn {
  _hidden?: boolean
  _sortOrder?: number
}

const editableColumns = ref<EditableColumn[]>([])

// 初始化列配置
watch(() => props.columns, (cols) => {
  const savedConfig = props.tableKey ? loadColumnConfig(storageKey.value) : null
  editableColumns.value = cols.map((col, idx) => {
    const saved = savedConfig?.find(c => c.dataIndex === col.dataIndex)
    return {
      ...col,
      _hidden: saved ? saved.hidden : (col.hidden || false),
      _sortOrder: saved?.sortOrder ?? idx
    }
  })
  // 按保存的排序
  editableColumns.value.sort((a, b) => (a._sortOrder ?? 0) - (b._sortOrder ?? 0))
}, { immediate: true })

const processedColumns = computed(() => {
  return editableColumns.value
    .filter(col => !col._hidden)
    .map(col => {
      const base: any = {
        ...col,
        key: col.key || col.dataIndex,
        ellipsis: col.ellipsis ?? (col.type === 'ellipsis'),
        sorter: col.sortable || undefined,
        // 业务类型转为 className，用于全局 CSS 对齐
        className: getColumnClassName(col),
      }
      // 移除内部属性
      delete base._hidden
      delete base._sortOrder
      delete base.slotName
      delete base.statusMap
      return base
    })
})

// 根据列类型生成 CSS 类名
function getColumnClassName(col: TableColumn): string {
  if (col.className) return col.className
  const type = col.type
  if (type === 'currency' || type === 'number') return 'amount-cell'
  if (type === 'status') return 'status-cell'
  if (type === 'action') return 'action-cell'
  if (col.align === 'right') return 'number-cell'
  return ''
}

// ========== 空行填充逻辑 ==========

const hasSummary = computed(() => {
  return props.showSummary && props.summaryData && props.summaryData.length > 0
})

const emptyRowCount = computed(() => {
  if (!tableScrollY.value || tableScrollY.value <= 0) return 0
  const dataCount = props.dataSource.length
  // 如果有汇总行，汇总行占一行
  const summaryOffset = hasSummary.value ? 1 : 0
  const rowHeight = getRowHeight()
  const visibleRowCount = Math.max(1, Math.floor(tableScrollY.value / rowHeight))
  return Math.max(0, visibleRowCount - dataCount - summaryOffset)
})

// 扩展数据源：真实数据 + 占位空行
const tableDataSource = computed(() => {
  const data = [...props.dataSource]

  // 仅在非加载状态下注入空行
  if (!props.loading && emptyRowCount.value > 0) {
    for (let i = 0; i < emptyRowCount.value; i++) {
      data.push({
        __empty_row: true,
        __empty_index: i,
        [props.rowKey]: `__empty_${i}`
      })
    }
  }

  return data
})

// ========== 拖拽排序列 ==========

const dragIndex = ref(-1)

function handleDragStart(index: number, event: DragEvent) {
  dragIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
  }
}

function handleDragOver(index: number) {
  if (dragIndex.value === index) return
  const items = [...editableColumns.value]
  const [moved] = items.splice(dragIndex.value, 1)
  items.splice(index, 0, moved)
  editableColumns.value = items
  dragIndex.value = index
}

function handleDrop(index: number) {
  dragIndex.value = -1
  persistColumnConfig()
}

// ========== 视图保存 ==========

const savedViews = ref<SavedView[]>([])

onMounted(() => {
  loadSavedViews()
  // 初始计算高度并设置 ResizeObserver
  nextTick(() => {
    updateScrollY()
    setupResizeObserver()
    // 延迟修正列宽度
    debouncedFixColumnWidths()
  })
})

// ========== 修正 AntDV 4.x 的 col width bug ==========

/**
 * 手动修正 colgroup 中 col 元素的宽度
 * Ant Design Vue 4.x 在 scroll 模式下不会正确设置 col 的 width
 */
function fixColumnWidths() {
  if (!containerRef.value) return

  const headerTable = containerRef.value.querySelector('.ant-table-header table')
  const bodyTable = containerRef.value.querySelector('.ant-table-body table')

  if (!headerTable && !bodyTable) return

  // 获取列宽度数组
  const widths: number[] = []

  // 选择列宽度（如果启用）
  if (props.selectable) {
    widths.push(40)
  }

  processedColumns.value.forEach(col => {
    const w = col.width
    if (typeof w === 'number') widths.push(w)
    else if (typeof w === 'string' && w.endsWith('px')) widths.push(parseInt(w))
    else widths.push(100)
  })

  // 滚动条占位列
  widths.push(8)

  // 设置 header table 的 colgroup
  if (headerTable) {
    const headerCols = headerTable.querySelectorAll('colgroup col')
    headerCols.forEach((col, i) => {
      if (i < widths.length) {
        col.setAttribute('width', String(widths[i]))
        col.setAttribute('style', `width: ${widths[i]}px; min-width: ${widths[i]}px;`)
      }
    })
  }

  // 设置 body table 的 colgroup
  if (bodyTable) {
    const bodyCols = bodyTable.querySelectorAll('colgroup col')
    bodyCols.forEach((col, i) => {
      if (i < widths.length) {
        col.setAttribute('width', String(widths[i]))
        col.setAttribute('style', `width: ${widths[i]}px; min-width: ${widths[i]}px;`)
      }
    })
  }
}

// 防抖修正列宽度
let fixTimer: number | null = null
function debouncedFixColumnWidths() {
  if (fixTimer) clearTimeout(fixTimer)
  fixTimer = window.setTimeout(() => {
    fixColumnWidths()
    fixTimer = null
  }, 100)
}

function loadSavedViews() {
  try {
    const raw = localStorage.getItem(`table-views:${storageKey.value}`)
    if (raw) savedViews.value = JSON.parse(raw)
  } catch { /* ignore */ }
}

function persistSavedViews() {
  try {
    localStorage.setItem(`table-views:${storageKey.value}`, JSON.stringify(savedViews.value))
  } catch { /* ignore */ }
}

function persistColumnConfig() {
  if (!props.tableKey) return
  const configs: ColumnConfig[] = editableColumns.value.map((col, idx) => ({
    dataIndex: col.dataIndex,
    width: col.width,
    fixed: col.fixed || false,
    sortOrder: idx,
    hidden: col._hidden || false
  }))
  saveColumnConfig(storageKey.value, configs)
}

function resetColumnConfig() {
  editableColumns.value = props.columns.map((col, idx) => ({
    ...col,
    _hidden: col.hidden || false,
    _sortOrder: idx
  }))
  if (props.tableKey) {
    import('@/utils/columnStorage').then(m => m.clearColumnConfig(storageKey.value))
  }
  columnSettingsVisible.value = false
  message.success('列配置已重置')
}

function handleSaveView() {
  if (!newViewName.value.trim()) {
    message.warning('请输入视图名称')
    return
  }
  const view: SavedView = {
    name: newViewName.value.trim(),
    isDefault: newViewIsDefault.value,
    columnConfig: editableColumns.value.map((col, idx) => ({
      dataIndex: col.dataIndex,
      width: col.width,
      fixed: col.fixed || false,
      sortOrder: idx,
      hidden: col._hidden || false
    })),
    filters: { ...filterValues }
  }
  if (view.isDefault) {
    savedViews.value.forEach(v => v.isDefault = false)
  }
  savedViews.value.push(view)
  persistSavedViews()
  showSaveViewModal.value = false
  newViewName.value = ''
  newViewIsDefault.value = false
  message.success('视图已保存')
}

function handleViewSelect({ key }: { key: string }) {
  if (key === '__manage__') {
    showManageViewModal.value = true
    return
  }
  const view = savedViews.value.find(v => v.name === key)
  if (!view) return
  currentViewName.value = view.name
  // 应用列配置
  const configMap = new Map(view.columnConfig.map(c => [c.dataIndex, c]))
  editableColumns.value.forEach(col => {
    const cfg = configMap.get(col.dataIndex)
    if (cfg) {
      col._hidden = cfg.hidden
      col._sortOrder = cfg.sortOrder
    }
  })
  editableColumns.value.sort((a, b) => (a._sortOrder ?? 0) - (b._sortOrder ?? 0))
  // 应用筛选
  Object.assign(filterValues, view.filters)
  emit('filter-change', { ...filterValues })
  message.success(`已切换到视图: ${view.name}`)
}

// ========== 行选择 ==========

const rowSelection = computed(() => {
  if (!props.selectable) return undefined
  return {
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: any[], rows: any[]) => {
      selectedRowKeys.value = keys
      selectedRows.value = rows
      emit('selection-change', keys, rows)
    },
    selections: [
      { key: 'all', text: '全选', onSelect: () => {
        selectedRowKeys.value = props.dataSource.map((r: any) => r[props.rowKey])
      }},
      { key: 'invert', text: '反选', onSelect: () => {
        const allKeys = props.dataSource.map((r: any) => r[props.rowKey])
        selectedRowKeys.value = selectedRowKeys.value.filter(k => !allKeys.includes(k))
      }},
      { key: 'none', text: '清空' }
    ]
  }
})

// ========== 分页 ==========

const paginationConfig = computed(() => {
  if (!props.pagination) return false
  return {
    ...props.pagination,
    showTotal: typeof props.pagination?.showTotal === 'function'
      ? props.pagination.showTotal
      : (total: number) => `共 ${total} 条`,
    onChange: (page: number, pageSize: number) => {
      emit('page-change', page, pageSize)
    },
    onShowSizeChange: (current: number, size: number) => {
      emit('page-change', current, size)
    }
  }
})

// ========== 滚动与高度计算 ==========

const scrollConfigComputed = computed(() => {
  // 计算所有列的总宽度
  const totalWidth = processedColumns.value.reduce((sum, col) => {
    const w = col.width
    if (typeof w === 'number') return sum + w
    if (typeof w === 'string' && w.endsWith('px')) return sum + parseInt(w)
    return sum + 100 // 默认宽度
  }, 0)
  const selectionWidth = props.selectable ? 40 : 0
  const x = Math.max(totalWidth + selectionWidth + 20, 600)

  // 设置 y 值，启用固定表头
  const y = tableScrollY.value && tableScrollY.value > 100 ? tableScrollY.value : 300
  return { x, y }
})

/**
 * 计算表格可滚动区域高度
 * 公式：容器高度 - 工具栏高度 - 批量操作栏高度 - 筛选面板高度 - 分页高度
 */
function updateScrollY() {
  if (!containerRef.value) return

  const container = containerRef.value
  const containerH = container.clientHeight

  // 获取各个固定元素的高度
  let occupiedHeight = 0

  // 工具栏
  const toolbar = container.querySelector('.table-toolbar') as HTMLElement
  if (toolbar) occupiedHeight += toolbar.offsetHeight

  // 筛选面板（展开时）
  const filterPanel = container.querySelector('.filter-panel') as HTMLElement
  if (filterPanel && showFilterPanel.value) occupiedHeight += filterPanel.offsetHeight

  // 批量操作栏（显示时）
  const batchBar = container.querySelector('.batch-bar') as HTMLElement
  if (batchBar) occupiedHeight += batchBar.offsetHeight + 8 // margin

  // 分页区域（AntDV 分页在表格内部，但 AntDV 4 会把分页放在表格底部的 ant-table-pagination 中）
  // 分页高度大约 48px（含 padding）
  const hasPagination = props.pagination !== false && props.pagination !== undefined
  if (hasPagination) occupiedHeight += 48 + 1 // +1 for border-top

  // 额外预留边距
  occupiedHeight += 2

  const scrollY = Math.max(100, containerH - occupiedHeight)
  tableScrollY.value = scrollY
}

// 监听窗口 resize
let resizeObserver: ResizeObserver | null = null

function setupResizeObserver() {
  if (!containerRef.value) return

  resizeObserver = new ResizeObserver(() => {
    updateScrollY()
  })
  resizeObserver.observe(containerRef.value)
}

// 监听筛选面板展开/折叠，重新计算高度
watch(showFilterPanel, () => {
  nextTick(() => {
    updateScrollY()
    fixColumnWidths()
  })
})

// 数据加载完成后重新计算和修正列宽度
watch(() => props.loading, () => {
  if (!props.loading) {
    nextTick(() => {
      updateScrollY()
      fixColumnWidths()
    })
  }
})

// 数据源变化后重新计算和修正列宽度
watch(() => props.dataSource.length, () => {
  nextTick(() => {
    updateScrollY()
    fixColumnWidths()
  })
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (colWidthObserver) {
    colWidthObserver.disconnect()
    colWidthObserver = null
  }
})

// ========== 汇总行处理 ==========

function getSummaryCellClass(col: any): string {
  const type = col.type
  if (type === 'currency' || type === 'number') return 'amount-cell'
  if (type === 'status') return 'status-cell'
  if (type === 'action') return 'action-cell'
  return ''
}

function getSummaryCellValue(col: any): string {
  // 列级别的 summaryValue 优先
  if (col.summaryValue !== undefined && col.summaryValue !== null) {
    if (col.summaryType === 'currency') return `¥${Number(col.summaryValue).toFixed(2)}`
    if (col.summaryType === 'number') return Number(col.summaryValue).toLocaleString()
    return String(col.summaryValue)
  }

  // 第一列显示"合计"
  const firstDataIndex = processedColumns.value[0]?.dataIndex
  if (col.dataIndex === firstDataIndex) {
    return '合计'
  }

  return ''
}

// ========== 自定义行处理（空行禁止交互） ==========

const customRowFn = computed(() => {
  // 如果用户提供了 customRow，先包装一层
  const userFn = props.customRow
  return (record: any, index: number) => {
    if (record.__empty_row) {
      return { class: 'production-empty-row' }
    }
    if (userFn) return userFn(record, index)
    return {}
  }
})

// ========== 表格本地化 ==========

const tableLocale = {
  triggerDesc: '点击降序',
  triggerAsc: '点击升序',
  cancelSort: '取消排序',
}

// ========== 格式化工具 ==========

function getStatusColor(value: string | number, map?: Record<string | number, { text: string; color: string }>) {
  const m = map || (props as any).statusMap?.default
  return m?.[value]?.color || 'default'
}

function getStatusText(value: string | number, map?: Record<string | number, { text: string; color: string }>) {
  const m = map || (props as any).statusMap?.default
  return m?.[value]?.text || String(value || '-')
}

function formatDate(value: string | number | Date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!value) return '-'
  return dayjs(value).format(format)
}

function formatNumber(value: any, format?: string): string {
  if (value === undefined || value === null) return '-'
  const num = Number(value)
  if (isNaN(num)) return String(value)
  if (format === 'currency') return num.toFixed(2)
  if (format === 'percent') return `${(num * 100).toFixed(1)}%`
  if (format === 'integer') return Math.round(num).toLocaleString()
  return num.toLocaleString()
}

function formatSummaryValue(s: SummaryItem): string {
  if (typeof s.value === 'number') {
    if (s.type === 'currency') return `¥${s.value.toFixed(2)}`
    return s.value.toLocaleString()
  }
  return String(s.value)
}

// ========== 事件处理 ==========

const handleAdd = () => emit('add')
const handleEdit = (record: any) => emit('edit', record)
const handleView = (record: any) => emit('view', record)
const handleDelete = (record: any) => emit('delete', record)
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的项')
    return
  }
  emit('batch-delete', [...selectedRowKeys.value])
}
const handleBatchEdit = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要编辑的项')
    return
  }
  emit('batch-edit', selectedRowKeys.value)
}
const handleRefresh = () => emit('refresh')
const handleSearch = (keyword: string) => emit('search', keyword)
const handleExport = () => emit('export')
const handleCellClick = (record: any, column: TableColumn) => emit('cell-click', record, column)
const clearSelection = () => {
  selectedRowKeys.value = []
  selectedRows.value = []
}

function handleFilterChange() {
  // 自动查询（防抖由父组件处理）
}

function handleFilterSubmit() {
  emit('filter-change', { ...filterValues })
}

function handleFilterReset() {
  for (const key of Object.keys(filterValues)) {
    filterValues[key] = undefined
  }
  emit('filter-change', {})
}

function handleTableChange(pagination: any, filters: Record<string, any[]>, sorter: any) {
  if (sorter.field) {
    emit('sort-change', sorter.field, sorter.order)
  }
}

function handleColumnResize(widths: Record<string, number>) {
  // 列宽变化持久化
  if (props.tableKey) {
    const configs: ColumnConfig[] = editableColumns.value.map(col => ({
      dataIndex: col.dataIndex,
      width: widths[col.dataIndex] || col.width,
      fixed: col.fixed || false,
      sortOrder: col._sortOrder ?? 0,
      hidden: col._hidden || false
    }))
    saveColumnConfig(storageKey.value, configs)
  }
}

// ========== 暴露方法 ==========

defineExpose({
  clearSelection,
  selectedRowKeys,
  selectedRows,
  refresh: () => emit('refresh'),
  getTable: () => tableRef.value,
  updateScrollY,
})
</script>

<style scoped>
.table-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background: #fff;
  overflow: hidden;
}

.table-list-container--no-toolbar {
  /* 无工具栏时依然保持 flex 列 */
}

/* ===== 顶部工具栏 ===== */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  padding: 8px 12px;
  border-bottom: 1px solid #e8e8e8;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 44px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

/* ===== 筛选面板 ===== */
.filter-panel {
  flex-shrink: 0;
  background: #fafafa;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

/* ===== 批量操作栏 ===== */
.batch-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 6px 12px;
  margin: 0;
  background: #e6f7ff;
  border-bottom: 1px solid #91d5ff;
}

.batch-info {
  font-size: 13px;
  color: #1890ff;
  font-weight: 500;
}

/* ===== 核心表格样式 ===== */

/* 强制使用 auto layout 来修复 AntDV 4.x 的 col width bug */
:deep(.ant-table-wrapper .ant-table) {
  table-layout: auto !important;
}

:deep(.ant-table-header) {
  background: #fafafa !important;
  overflow-x: auto !important;
  overflow-y: hidden !important;
}

:deep(.ant-table-body) {
  overflow-x: auto !important;
  overflow-y: auto !important;
}

/* 表头单元格样式 - 强制设置最小宽度 */
:deep(.ant-table-thead > tr > th) {
  background: #fafafa !important;
  border-top: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  border-left: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  padding: 10px 12px !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  white-space: nowrap !important;
}

/* 表体单元格样式 */
:deep(.ant-table-tbody > tr > td) {
  border-left: 1px solid #e8e8e8 !important;
  border-right: 1px solid #e8e8e8 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 10px 12px !important;
  font-size: 13px !important;
}

/* ===== 固定列分隔线（2px solid） ===== */
:deep(.ant-table-cell-fix-left-last) {
  border-right: 2px solid #c0c0c0 !important;
}
:deep(.ant-table-cell-fix-right-first) {
  border-left: 2px solid #c0c0c0 !important;
}

/* ===== 斑马纹（极浅 #fafafa，永不遮挡边框） ===== */
:deep(.ant-table-tbody > tr:nth-child(even):not(.production-empty-row) > td) {
  background-color: #fafafa !important;
}

/* ===== 行 hover 反馈（覆盖斑马纹，确保所有行 hover 都可见） ===== */
:deep(.ant-table-tbody > tr.ant-table-row:hover > td) {
  background-color: #f0f0f0 !important;
}

/* ===== 空占位行 ===== */
:deep(.production-empty-row) {
  cursor: default !important;
  pointer-events: none !important;
  user-select: none !important;
}
:deep(.production-empty-row > td) {
  border-left: 1px solid #e0e0e0 !important;
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  background: #fff !important;
}

/* ===== 汇总行（sticky 固定在底部） ===== */
:deep(.ant-table-summary) {
  position: sticky !important;
  bottom: 0 !important;
  z-index: 2 !important;
}
:deep(.table-summary-row) {
  background: #f5f5f5 !important;
}
:deep(.table-summary-row > td) {
  border-top: 2px solid #c0c0c0 !important;
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: none !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  padding: 9px 12px !important;
  background: #f5f5f5 !important;
}
:deep(.table-summary-row > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}
:deep(.table-summary-row > td:last-child) {
  /* 保留右边界 */
}

/* ===== 单元格对齐 ===== */
/* 金额/数字：右对齐 + 等宽字体 */
:deep(.amount-cell) {
  text-align: right !important;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace !important;
  font-variant-numeric: tabular-nums !important;
}
:deep(.number-cell) {
  text-align: right !important;
}
:deep(.status-cell) {
  text-align: center !important;
}
:deep(.action-cell) {
  text-align: center !important;
  white-space: nowrap !important;
}

/* ===== 分页条（sticky 固定在底部，始终可见） ===== */
:deep(.ant-table-pagination.ant-pagination) {
  position: sticky !important;
  bottom: 0 !important;
  z-index: 1 !important;
  margin: 0 !important;
  padding: 10px 16px !important;
  background: #fff !important;
  border-top: 1px solid #e8e8e8 !important;
}

/* ===== 滚动条美化 ===== */
:deep(.ant-table-body::-webkit-scrollbar) {
  width: 8px !important;
  height: 8px !important;
}
:deep(.ant-table-body::-webkit-scrollbar-track) {
  background: #f5f5f5 !important;
  border-radius: 4px !important;
}
:deep(.ant-table-body::-webkit-scrollbar-thumb) {
  background: #d9d9d9 !important;
  border-radius: 4px !important;
}
:deep(.ant-table-body::-webkit-scrollbar-thumb:hover) {
  background: #bfbfbf !important;
}

/* ===== 列设置弹窗 ===== */
.column-settings {
  min-width: 220px;
  max-height: 360px;
  overflow-y: auto;
}

.column-settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 13px;
}

.column-settings-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.column-settings-item {
  padding: 4px 0;
  cursor: move;
  user-select: none;
  font-size: 13px;
}

.column-settings-item:hover {
  color: #1890ff;
}

.column-drag-handle {
  margin-right: 6px;
  color: #bbb;
  font-size: 14px;
  letter-spacing: 2px;
}

/* ===== 金额/数字等宽字体 ===== */
:deep(.currency-value) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

:deep(.number-value) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

/* ===== 链接 ===== */
:deep(.cell-link) {
  color: #1890ff;
  cursor: pointer;
}

:deep(.cell-link:hover) {
  text-decoration: underline;
}

/* ===== 省略号文本 ===== */
:deep(.ellipsis-text) {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

/* ===== 操作列内部 ===== */
:deep(.action-cell-inner) {
  white-space: nowrap;
  display: flex;
  justify-content: center;
}

/* ===== 状态标签居中 ===== */
:deep(.status-cell-inner) {
  display: flex;
  justify-content: center;
}

/* ===== 全局覆盖列设置弹窗 ===== */
:deep(.column-settings-popover .ant-popover-inner-content) {
  padding: 12px;
}
</style>
