<template>
  <div class="table-list-container">
    <!-- 顶部工具栏 -->
    <div class="table-toolbar" v-if="showToolbar">
      <div class="toolbar-left">
        <slot name="toolbar-left">
          <a-space>
            <a-button 
              v-if="showAdd" 
              type="primary" 
              @click="handleAdd"
            >
              <template #icon><PlusOutlined /></template>
              新增
            </a-button>
            <a-button 
              v-if="showBatchDelete && selectedRowKeys.length > 0" 
              danger 
              @click="handleBatchDelete"
            >
              <template #icon><DeleteOutlined /></template>
              批量删除 ({{ selectedRowKeys.length })
            </a-button>
            <slot name="toolbar-actions" />
          </a-space>
        </slot>
      </div>
      <div class="toolbar-right">
        <slot name="toolbar-right">
          <a-space>
            <a-input-search
              v-if="showSearch"
              v-model:value="searchKeyword"
              placeholder="搜索..."
              style="width: 200px"
              @search="handleSearch"
              allow-clear
            />
            <a-button v-if="showRefresh" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
            <a-button v-if="showExport" @click="handleExport">
              <template #icon><ExportOutlined /></template>
              导出
            </a-button>
            <slot name="toolbar-extra" />
          </a-space>
        </slot>
      </div>
    </div>
    
    <!-- 表格主体 -->
    <a-table
      :columns="processedColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="paginationConfig"
      :row-selection="rowSelection"
      :row-key="rowKey"
      :scroll="scroll"
      :bordered="bordered"
      :size="tableSize"
      :custom-row="customRow"
      @change="handleTableChange"
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
            <a-space>
              <a-button 
                v-if="showView" 
                type="link" 
                size="small" 
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button 
                v-if="showEdit" 
                type="link" 
                size="small" 
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-popconfirm
                v-if="showDelete"
                title="确定删除?"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </slot>
        </template>
        <template v-else-if="column.type === 'status'">
          <a-tag :color="getStatusColor(record[column.dataIndex])">
            {{ getStatusText(record[column.dataIndex]) }}
          </a-tag>
        </template>
        <template v-else-if="column.type === 'date'">
          {{ formatDate(record[column.dataIndex], column.dateFormat) }}
        </template>
        <template v-else-if="column.type === 'number'">
          {{ formatNumber(record[column.dataIndex], column.numberFormat) }}
        </template>
        <template v-else-if="column.type === 'ellipsis'">
          <a-tooltip :title="record[column.dataIndex]">
            <span class="ellipsis-text">
              {{ record[column.dataIndex] }}
            </span>
          </a-tooltip>
        </template>
      </template>
      
      <!-- 空状态 -->
      <template #emptyText>
        <slot name="empty">
          <a-empty :description="emptyText" />
        </slot>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, type PropType } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  DeleteOutlined, 
  ReloadOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'

// 列配置类型
export type TableColumn = {
  title: string
  dataIndex: string
  key?: string
  width?: number | string
  minWidth?: number
  fixed?: 'left' | 'right'
  align?: 'left' | 'center' | 'right'
  ellipsis?: boolean
  sortable?: boolean
  filterable?: boolean
  type?: 'text' | 'action' | 'status' | 'date' | 'number' | 'ellipsis'
  slotName?: string
  dateFormat?: string
  numberFormat?: string
  statusMap?: Record<string | number, { text: string; color: string }>
  hidden?: boolean
  [key: string]: any
}

// 分页配置类型
export type PaginationConfig = {
  current?: number
  pageSize?: number
  total?: number
  showSizeChanger?: boolean
  showQuickJumper?: boolean
  pageSizeOptions?: string[]
  showTotal?: boolean | ((total: number, range: [number, number]) => string)
}

// Props
const props = defineProps({
  columns: {
    type: Array as PropType<TableColumn[]>,
    required: true
  },
  dataSource: {
    type: Array as PropType<any[]>,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  rowKey: {
    type: String,
    default: 'id'
  },
  bordered: {
    type: Boolean,
    default: true
  },
  tableSize: {
    type: String as PropType<'small' | 'middle' | 'large'>,
    default: 'middle'
  },
  scroll: {
    type: Object as PropType<{ x?: number | string; y?: number | string }>,
    default: () => ({ x: '100%' })
  },
  pagination: {
    type: Object as PropType<PaginationConfig>,
    default: () => ({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true,
      pageSizeOptions: ['10', '20', '50', '100'],
      showTotal: (total: number) => `共 ${total} 条`
    })
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  showSearch: {
    type: Boolean,
    default: true
  },
  showAdd: {
    type: Boolean,
    default: true
  },
  showEdit: {
    type: Boolean,
    default: true
  },
  showView: {
    type: Boolean,
    default: false
  },
  showDelete: {
    type: Boolean,
    default: true
  },
  showBatchDelete: {
    type: Boolean,
    default: true
  },
  showRefresh: {
    type: Boolean,
    default: true
  },
  showExport: {
    type: Boolean,
    default: false
  },
  selectable: {
    type: Boolean,
    default: true
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  statusMap: {
    type: Object as PropType<Record<string, Record<string | number, { text: string; color: string }>>>,
    default: () => ({
      default: {
        0: { text: '禁用', color: 'default' },
        1: { text: '启用', color: 'success' }
      }
    })
  },
  customRow: {
    type: Function,
    default: undefined
  }
})

// Emits
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
  'selection-change': [keys: any[], rows: any[]]
  'sort-change': [field: string, order: string]
  'filter-change': [filters: Record<string, any[]>]
}>()

// State
const searchKeyword = ref('')
const selectedRowKeys = ref<any[]>([])
const selectedRows = ref<any[]>([])

// 处理列配置
const processedColumns = computed(() => {
  return props.columns
    .filter(col => !col.hidden)
    .map(col => ({
      ...col,
      key: col.key || col.dataIndex,
      ellipsis: col.ellipsis || col.type === 'ellipsis',
      sorter: col.sortable ? true : undefined,
      filters: col.filterable ? [] : undefined
    }))
})

// 行选择配置
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
      { key: 'all', text: '全选' },
      { key: 'invert', text: '反选' },
      { key: 'none', text: '清空' }
    ]
  }
})

// 分页配置
const paginationConfig = computed(() => {
  if (!props.pagination) return false
  
  return {
    ...props.pagination,
    onChange: (page: number, pageSize: number) => {
      emit('page-change', page, pageSize)
    },
    onShowSizeChange: (current: number, size: number) => {
      emit('page-change', current, size)
    }
  }
})

// 状态颜色
const getStatusColor = (value: string | number) => {
  const map = props.statusMap.default || {}
  return map[value]?.color || 'default'
}

// 状态文本
const getStatusText = (value: string | number) => {
  const map = props.statusMap.default || {}
  return map[value]?.text || String(value)
}

// 日期格式化
const formatDate = (value: string | number | Date, format = 'YYYY-MM-DD HH:mm:ss') => {
  if (!value) return '-'
  return dayjs(value).format(format)
}

// 数字格式化
const formatNumber = (value: number, format?: string) => {
  if (value === undefined || value === null) return '-'
  if (format === 'currency') return `¥${value.toFixed(2)}`
  if (format === 'percent') return `${(value * 100).toFixed(1)}%`
  return String(value)
}

// 事件处理
const handleAdd = () => emit('add')
const handleEdit = (record: any) => emit('edit', record)
const handleView = (record: any) => emit('view', record)
const handleDelete = (record: any) => emit('delete', record)
const handleBatchDelete = () => {
  emit('batch-delete', selectedRowKeys.value)
  selectedRowKeys.value = []
  selectedRows.value = []
}
const handleRefresh = () => emit('refresh')
const handleSearch = (keyword: string) => emit('search', keyword)
const handleExport = () => emit('export')

const handleTableChange = (
  pagination: any,
  filters: Record<string, any[]>,
  sorter: any
) => {
  if (sorter.field) {
    emit('sort-change', sorter.field, sorter.order)
  }
  if (Object.keys(filters).length > 0) {
    emit('filter-change', filters)
  }
}

// 清空选择
const clearSelection = () => {
  selectedRowKeys.value = []
  selectedRows.value = []
}

// 暴露方法
defineExpose({
  clearSelection,
  selectedRowKeys,
  selectedRows
})
</script>

<style scoped>
.table-list-container {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
}

.ellipsis-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}
</style>