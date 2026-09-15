<template>
  <div
    ref="containerRef"
    class="bill-table-list-container"
  >
    <!-- 顶部工具栏 -->
    <div
      v-if="showToolbar"
      class="table-toolbar"
    >
      <div class="toolbar-left">
        <a-space>
          <a-button
            v-if="showAdd && (!addPermission || hasPermission(addPermission))"
            ref="addBtnRef"
            type="primary"
            @click="debounceClick('add', () => emit('add'))"
          >
            <template #icon>
              <PlusOutlined />
            </template>
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
              <template #icon>
                <FilterOutlined />
              </template>
              筛选
            </a-button>
          </a-tooltip>
          <a-input-search
            v-if="showSearch"
            ref="searchInputRef"
            v-model:value="searchKeyword"
            :placeholder="searchPlaceholder"
            style="width: 200px"
            size="small"
            allow-clear
            @search="handleSearch"
          />
          <a-tooltip title="刷新 (F5)">
            <a-button
              size="small"
              @click="debounceClick('refresh', () => emit('refresh'))"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
            </a-button>
          </a-tooltip>
          <a-tooltip
            v-if="showExport && (!exportPermission || hasPermission(exportPermission))"
            title="导出"
          >
            <a-button
              size="small"
              @click="debounceClick('export', () => emit('export'))"
            >
              <template #icon>
                <ExportOutlined />
              </template>
            </a-button>
          </a-tooltip>
        </a-space>
      </div>
    </div>

    <!-- 筛选面板 -->
    <div
      v-if="showFilterPanel"
      class="filter-panel"
    >
      <a-row :gutter="[12, 12]">
        <a-col
          v-for="filter in filterFields"
          :key="filter.key"
          :span="filter.span || 6"
        >
          <a-form-item
            :label="filter.label"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
          >
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
        <a-col
          :span="6"
          class="filter-actions"
        >
          <a-space>
            <a-button
              type="primary"
              size="small"
              @click="handleFilterSubmit"
            >
              查询
            </a-button>
            <a-button
              size="small"
              @click="handleFilterReset"
            >
              重置
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </div>

    <!-- 批量操作栏 -->
    <div
      v-if="selectedRecords.length > 0"
      class="batch-bar"
    >
      <a-space>
        <span class="batch-info">已选择 {{ selectedRecords.length }} 项</span>
        <a-button
          v-if="showBatchDelete && (!deletePermission || hasPermission(deletePermission))"
          danger
          size="small"
          @click="handleBatchDelete"
        >
          <template #icon>
            <DeleteOutlined />
          </template>
          批量删除
        </a-button>
        <slot
          name="batch-actions"
          :selected-rows="selectedRecords"
        />
        <a-button
          type="link"
          size="small"
          @click="clearSelection"
        >
          取消选择
        </a-button>
      </a-space>
    </div>

    <!-- BillDetailTable 表格 -->
    <BillDetailTable
      ref="tableRef"
      :columns="convertedColumns"
      :data-source="tableData"
      :loading="loading"
      :view-mode="true"
      :summary-columns="summaryData"
      :storage-key="storageKey"
      :global-config-key="globalConfigKey"
      :row-key="rowKey"
      :min-rows="minEmptyRows"
      :fill-mode="fillMode"
      @checkbox-change="handleCheckboxChange"
      @checkbox-all="handleCheckboxAll"
      @sort-change="handleSortChange"
    >
      <!-- 透传所有插槽 -->
      <template
        v-for="(_, name) in $slots"
        #[name]="slotData"
      >
        <slot
          :name="name"
          v-bind="slotData || {}"
        />
      </template>
    </BillDetailTable>

    <!-- 分页 -->
    <div
      v-if="pagination"
      class="table-pagination"
    >
      <a-pagination
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="paginationTotal"
        :show-size-changer="true"
        :show-quick-jumper="true"
        :page-size-options="['10', '20', '50', '100']"
        :show-total="(total: number) => `共 ${total} 条`"
        size="small"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script lang="ts">
export interface FilterField {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange'
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  span?: number
}
</script>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick, type PropType } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  ReloadOutlined,
  ExportOutlined,
  FilterOutlined,
} from '@ant-design/icons-vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { usePermission } from '@/composables/usePermission'

// ── 防抖工具 ──
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── VxeTableList 列配置兼容转换 ──
function convertColumns(cols: any[]): DetailColumnConfig[] {
  return cols.map(col => {
    const converted: any = { ...col }

    // field → key
    if (col.field && !col.key) {
      converted.key = col.field
    }

    // type: 'seq' → 'rowNo'
    if (col.type === 'seq') {
      converted.type = 'rowNo'
    }

    // slots 转换
    if (col.slots?.default) {
      converted.type = 'slot'
      converted.slotName = col.slots.default
    }

    return converted as DetailColumnConfig
  })
}

// ========== Props ==========
const props = defineProps({
  columns: { type: Array as PropType<any[]>, required: true },
  dataSource: { type: Array as PropType<any[]>, default: () => [] },
  loading: { type: Boolean, default: false },
  showToolbar: { type: Boolean, default: true },
  showSearch: { type: Boolean, default: true },
  searchPlaceholder: { type: String, default: '搜索...' },
  showAdd: { type: Boolean, default: true },
  addText: { type: String, default: '新增' },
  showBatchDelete: { type: Boolean, default: true },
  showExport: { type: Boolean, default: false },
  selectable: { type: Boolean, default: true },
  filterFields: { type: Array as PropType<FilterField[]>, default: () => [] },
  pagination: {
    type: [Object, Boolean] as PropType<{ current?: number; pageSize?: number; total?: number } | boolean>,
    default: () => ({ current: 1, pageSize: 20, total: 0 })
  },
  addPermission: { type: String, default: '' },
  deletePermission: { type: String, default: '' },
  exportPermission: { type: String, default: '' },
  minEmptyRows: { type: Number, default: 20 },
  rowKey: { type: String, default: 'id' },
  treeConfig: { type: Object as PropType<any>, default: undefined },
  defaultSort: { type: Object as PropType<any>, default: undefined },
  showSummary: { type: Boolean, default: false },
  summaryData: { type: Array as PropType<any[]>, default: undefined },
  storageKey: { type: String as PropType<string>, default: '' },
  /** 全局列配置持久化键（传入后「全局配置」Tab 落后端 user-config，跨浏览器生效） */
  globalConfigKey: { type: String as PropType<string>, default: '' },
  /** 是否用 __filler__ 列吸收剩余宽度（防止列少时操作列被剩余空间撑宽，见 BillDetailTable 使用规范） */
  fillMode: { type: Boolean, default: true },
  showDelete: { type: Boolean, default: false },
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
  'sort-change': [field: string | null, order: string | null]
  'filter-change': [filters: Record<string, any>]
}>()

// ========== State ==========
const tableRef = ref<InstanceType<typeof BillDetailTable>>()
const searchInputRef = ref<any>(null)
const addBtnRef = ref<any>(null)
const searchKeyword = ref('')
const showFilterPanel = ref(false)
const selectedRecords = ref<any[]>([])
const filterValues = reactive<Record<string, any>>({})

// 分页
const currentPage = ref((props.pagination as any)?.current || 1)
const pageSize = ref((props.pagination as any)?.pageSize || 20)
const paginationTotal = ref(Number((props.pagination as any)?.total) || 0)

// 处理数据（添加空行）
const tableData = computed(() => props.dataSource)

// 转换列配置（兼容 VxeTableList 格式）
const convertedColumns = computed(() => convertColumns(props.columns))

// ========== 权限检查 ==========
const { checkPermission } = usePermission()
function hasPermission(permission: string): boolean {
  if (!permission) return true
  return checkPermission(permission)
}

// ========== 事件处理 ==========
function handleSearch(keyword: string) {
  emit('search', keyword)
}

function handleFilterChange() {
  // 筛选值变化时自动触发查询
}

function handleFilterSubmit() {
  emit('filter-change', { ...filterValues })
}

function handleFilterReset() {
  Object.keys(filterValues).forEach(key => {
    filterValues[key] = undefined
  })
  emit('filter-change', {})
}

function handleBatchDelete() {
  if (selectedRecords.value.length === 0) {
    message.warning('请先选择要删除的记录')
    return
  }
  const ids = selectedRecords.value.map(r => r.id)
  emit('batch-delete', ids)
}

function handlePageChange(page: number, size: number) {
  currentPage.value = page
  pageSize.value = size
  emit('page-change', page, size)
}

function handleCheckboxChange(record: any, rowIndex: number, checked: boolean) {
  if (checked) {
    selectedRecords.value.push(record)
  } else {
    const idx = selectedRecords.value.indexOf(record)
    if (idx > -1) {
      selectedRecords.value.splice(idx, 1)
    }
  }
  emit('selection-change', [...selectedRecords.value], selectedRecords.value.map(r => r.id))
}

function handleCheckboxAll(checked: boolean, records: any[]) {
  selectedRecords.value = records
  emit('selection-change', [...selectedRecords.value], selectedRecords.value.map(r => r.id))
}

function handleSortChange(key: string | null, order: string | null) {
  emit('sort-change', key, order)
}

function clearSelection() {
  selectedRecords.value = []
  // 同步清空子表勾选态：只清父层会导致「批量条消失、行上仍勾着」，进而勾选漂移
  tableRef.value?.clearSelection?.()
  emit('selection-change', [], [])
}

// ========== 暴露方法 ==========
defineExpose({
  clearSelection,
  getSelectedRecords: () => [...selectedRecords.value],
})

// ========== 生命周期 ==========
watch(() => props.pagination, (val) => {
  if (val && typeof val === 'object') {
    currentPage.value = val.current || 1
    pageSize.value = val.pageSize || 20
    paginationTotal.value = Number(val.total) || 0
  }
}, { deep: true })

onMounted(() => {
  // 初始化筛选值
  props.filterFields.forEach(f => {
    filterValues[f.key] = undefined
  })
})
</script>

<style scoped>
.bill-table-list-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
}

/* ── 工具栏 ── */
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ── 筛选面板 ── */
.filter-panel {
  padding: 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 32px;
}

/* ── 批量操作栏 ── */
.batch-bar {
  padding: 8px 16px;
  background: #e6f7ff;
  border-bottom: 1px solid #91d5ff;
}

.batch-info {
  font-size: 13px;
  color: #1890ff;
  font-weight: 500;
}

/* ── 分页 ── */
.table-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}
</style>
