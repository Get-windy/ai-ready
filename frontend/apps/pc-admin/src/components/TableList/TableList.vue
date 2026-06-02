<template>
  <div class="table-list-container">
    <!-- 顶部工具栏 -->
    <div v-if="showToolbar" class="table-toolbar">
      <div class="toolbar-left">
        <slot name="toolbar-left">
          <a-space>
            <a-button v-if="showAdd" type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              {{ addText }}
            </a-button>
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

    <!-- 汇总行 -->
    <div v-if="showSummary && summaryData" class="summary-row">
      <a-space wrap :size="[16, 4]">
        <span
          v-for="s in summaryData"
          :key="s.label"
          class="summary-item"
        >
          <span class="summary-label">{{ s.label }}:</span>
          <span :class="['summary-value', s.type === 'currency' ? 'text-primary' : '', s.type === 'danger' ? 'text-danger' : '']">
            {{ s.type === 'currency' ? '¥' : '' }}{{ formatSummaryValue(s) }}
          </span>
        </span>
      </a-space>
    </div>

    <!-- 表格主体 -->
    <a-table
      ref="tableRef"
      :columns="processedColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="paginationConfig"
      :row-selection="rowSelection"
      :row-key="rowKey"
      :scroll="scrollConfig"
      :bordered="bordered"
      :size="tableSize"
      :custom-row="customRow"
      :locale="tableLocale"
      @change="handleTableChange"
      @resize-column="handleColumnResize"
    >
      <!-- 自定义列插槽 -->
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.slotName">
          <slot
            :name="column.slotName"
            :record="record"
            :index="index"
            :column="column"
          />
        </template>
        <template v-else-if="column.type === 'action'">
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
        </template>
        <template v-else-if="column.type === 'status'">
          <a-tag :color="getStatusColor(record[column.dataIndex], column.statusMap)">
            {{ getStatusText(record[column.dataIndex], column.statusMap) }}
          </a-tag>
        </template>
        <template v-else-if="column.type === 'date'">
          <span :title="formatDate(record[column.dataIndex], column.dateFormat)">
            {{ formatDate(record[column.dataIndex], column.dateFormat) }}
          </span>
        </template>
        <template v-else-if="column.type === 'currency'">
          <span class="currency-value">
            ¥{{ formatNumber(record[column.dataIndex], 'currency') }}
          </span>
        </template>
        <template v-else-if="column.type === 'number'">
          {{ formatNumber(record[column.dataIndex], column.numberFormat) }}
        </template>
        <template v-else-if="column.type === 'ellipsis'">
          <a-tooltip :title="record[column.dataIndex]">
            <span class="ellipsis-text">{{ record[column.dataIndex] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.type === 'link'">
          <a class="cell-link" @click="handleCellClick(record, column)">{{ record[column.dataIndex] }}</a>
        </template>
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
import { ref, reactive, computed, watch, onMounted, type PropType } from 'vue'
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
  bordered: { type: Boolean, default: true },
  tableSize: { type: String as PropType<'small' | 'middle' | 'large'>, default: 'middle' },
  showToolbar: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  showAdd: { type: Boolean, default: true },
  addText: { type: String, default: '新增' },
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

  // 汇总
  showSummary: { type: Boolean, default: false },
  summaryData: { type: Array as PropType<SummaryItem[]>, default: undefined },

  // 表格
  scroll: { type: Object as PropType<{ x?: number | string; y?: number | string }>, default: () => ({ x: '100%' }) },
  customRow: { type: Function, default: undefined },

  // 表格标识（用于持久化列配置）
  tableKey: { type: String, default: '' },

  // 行内编辑
  editable: { type: Boolean, default: false },
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
      }
      // 移除内部属性
      delete base._hidden
      delete base._sortOrder
      delete base.slotName
      delete base.statusMap
      return base
    })
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
})

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

// ========== 滚动配置 ==========

const scrollConfig = computed(() => {
  if (props.editable) {
    return { ...props.scroll, y: props.scroll?.y || 400 }
  }
  return props.scroll
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
    if (s.type === 'currency') return s.value.toFixed(2)
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
  emit('batch-delete', selectedRowKeys.value)
  selectedRowKeys.value = []
  selectedRows.value = []
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
  getTable: () => tableRef.value
})
</script>

<style scoped>
.table-list-container {
  background: #fff;
  padding: 16px;
  border-radius: 6px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

/* 筛选面板 */
.filter-panel {
  background: var(--color-bg-layout, #fafafa);
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 6px;
  border: 1px solid var(--color-border-secondary, #f0f0f0);
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

/* 批量操作栏 */
.batch-bar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  margin-bottom: 8px;
  background: var(--color-primary-bg, #e6f7ff);
  border: 1px solid var(--color-primary-border, #91d5ff);
  border-radius: 4px;
}

.batch-info {
  font-size: 13px;
  color: var(--color-primary, #1890ff);
  font-weight: 500;
}

/* 汇总行 */
.summary-row {
  padding: 8px 12px;
  background: var(--color-bg-layout, #fafafa);
  border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);
}

.summary-item {
  font-size: 13px;
  white-space: nowrap;
}

.summary-label {
  color: var(--color-text-secondary, #666);
  margin-right: 4px;
}

.summary-value {
  font-weight: 600;
  color: var(--color-text, #333);
}

/* 列设置 */
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
  border-bottom: 1px solid var(--color-border-secondary, #f0f0f0);
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
  color: var(--color-primary, #1890ff);
}

.column-drag-handle {
  margin-right: 6px;
  color: var(--color-text-quaternary, #bbb);
  font-size: 14px;
  letter-spacing: 2px;
}

/* 金额样式 */
.currency-value {
  font-family: 'Menlo', 'Monaco', monospace;
  font-size: 13px;
}

/* 链接样式 */
.cell-link {
  color: var(--color-primary, #1890ff);
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

/* 省略号文本 */
.ellipsis-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

/* 全局覆盖列设置弹窗 */
:deep(.column-settings-popover .ant-popover-inner-content) {
  padding: 12px;
}
</style>
