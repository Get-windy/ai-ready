<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="false"
        @tab-change="handleTabChange"
        @search="handleSearch"
      >
        <!-- ═══ 工具栏左侧：新增 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isBtnEnabled('add')"
            type="primary"
            size="small"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增移库单
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：列/页面配置 + 刷新 + 打印 + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置">
              <a-button size="small" @click="showColumnConfig = true">
                <TableOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip title="页面配置">
              <a-button size="small" @click="showPageConfig = true">
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="isBtnEnabled('refresh')" size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isBtnEnabled('print')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isBtnEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="search-row-inner">
            <template v-for="f in activeQueryFields" :key="f.key">
              <div v-if="f.visible && f.key === 'keyword'" class="search-item">
                <a-input
                  v-model:value="searchParams.keyword"
                  placeholder="单号"
                  size="small"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="f.visible && f.key === 'status'" class="search-item">
                <a-select
                  v-model:value="searchParams.status"
                  placeholder="状态"
                  size="small"
                  allow-clear
                  style="width: 120px"
                  :options="STATUS_OPTIONS"
                />
              </div>
              <div v-if="f.visible && f.key === 'productName'" class="search-item">
                <a-input
                  v-model:value="searchParams.productName"
                  placeholder="商品名称"
                  size="small"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="f.visible && f.key === 'sourceNo'" class="search-item">
                <a-input
                  v-model:value="searchParams.sourceNo"
                  placeholder="来源单号"
                  size="small"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="f.visible && f.key === 'moveType'" class="search-item">
                <a-select
                  v-model:value="searchParams.moveType"
                  placeholder="移库类型"
                  size="small"
                  allow-clear
                  style="width: 120px"
                  :options="MOVE_TYPE_OPTIONS"
                />
              </div>
              <div v-if="f.visible && f.key === 'assigneeName'" class="search-item">
                <a-input
                  v-model:value="searchParams.assigneeName"
                  placeholder="执行人"
                  size="small"
                  allow-clear
                  style="width: 120px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="f.visible && f.key === 'dateRange'" class="search-item">
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  style="width: 240px"
                  @change="handleDateChange"
                />
              </div>
            </template>
            <a-button type="primary" size="small" class="btn-search-slot" @click="handleSearch">查询</a-button>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <BillTableList
            :columns="currentColumns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="false"
            row-key="id"
            @page-change="handlePageChange"
          >
            <template #routeCell="{ record }">
              <span>{{ record.fromLocationCode || '-' }} → {{ record.toLocationCode || '-' }}</span>
            </template>
            <template #sourceNoCell="{ record }">
              <span>{{ record.sourceNo || '-' }}</span>
            </template>
            <template #totalQuantityCell="{ record }">
              <span class="num-value">{{ formatQty(record.totalQuantity) }}</span>
            </template>
            <template #movedQuantityCell="{ record }">
              <span class="num-value">{{ formatQty(record.movedQuantity) }}</span>
            </template>
            <template #quantityCell="{ record }">
              <span class="num-value">{{ formatQty(record.quantity) }}</span>
            </template>
            <template #moveTypeCell="{ record }">
              <a-tag :color="moveTypeColor(record.moveType)">{{ moveTypeText(record.moveType) }}</a-tag>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template #itemStatusCell="{ record }">
              <a-tag :color="detailStatusColor(record.itemStatus)">{{ detailStatusText(record.itemStatus) }}</a-tag>
            </template>
            <template #createTimeCell="{ record }">
              {{ formatTime(record.createTime) }}
            </template>
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button size="small" type="link" @click="handleEdit(record)">查看</a-button>
                <a-button v-if="record.status === 0" size="small" type="link" @click="handleEdit(record)">编辑</a-button>
                <a-button v-if="record.status === 0" size="small" type="link" @click="handleStart(record)">开始</a-button>
                <a-button v-if="record.status === 1" size="small" type="link" @click="handleExecute(record)">执行</a-button>
                <a-popconfirm v-if="record.status === 0 || record.status === 1" title="确认取消该移库单？" @confirm="handleCancel(record)">
                  <a-button size="small" type="link">取消</a-button>
                </a-popconfirm>
                <a-popconfirm v-if="record.status === 0" title="确认删除该单据？" @confirm="handleDelete(record)">
                  <a-button size="small" type="link" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </BillTableList>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 数据表列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="panelColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'
import {
  PlusOutlined, SettingOutlined, TableOutlined, ReloadOutlined, PrinterOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { moveApi, type WmsMoveTask } from '@/api/wms/move'
import { WMS_STATUS_MAP, MOVE_TYPE_MAP, DETAIL_STATUS_MAP, formatQty, formatTime } from '../whTask'

defineOptions({ name: 'WhMoveOrderList' })

const router = useRouter()
const userStore = useUserStore()

// ═══ 方向 Tab（按单据 / 按明细） ═══
const tabs = [{ key: 'doc', label: '按单据' }, { key: 'detail', label: '按明细' }]
const activeTab = ref<'doc' | 'detail'>('doc')

// ═══ 字典 ═══
const STATUS_OPTIONS = Object.entries(WMS_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
const MOVE_TYPE_OPTIONS = Object.entries(MOVE_TYPE_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
function statusText(s: number) { return WMS_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }
function moveTypeText(t: number) { return MOVE_TYPE_MAP[t]?.text || '未知' }
function moveTypeColor(t: number) { return MOVE_TYPE_MAP[t]?.color || 'default' }
function detailStatusText(s: number) { return DETAIL_STATUS_MAP[s]?.text || '未知' }
function detailStatusColor(s: number) { return DETAIL_STATUS_MAP[s]?.color || 'default' }

// ═══ 查询参数 ═══
const searchParams = reactive<Record<string, any>>({
  keyword: '', status: undefined, sourceNo: '', moveType: undefined,
  assigneeName: '', productName: '', itemRemark: '', dateStart: '', dateEnd: '',
})
const dateRange = ref<[any, any] | null>(null)

// ═══ 分页（后端 pageNum/pageSize） ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
const loading = ref(false)

// ═══ 列配置弹窗 ═══
const showColumnConfig = ref(false)
const showPageConfig = ref(false)

// ═══ 列定义（按单据 / 按明细） ═══
interface ColDef { title: string; key: string; field?: string; type?: string; slotName?: string; width?: number; fixed?: string; align?: string; ellipsis?: boolean }

const docColumnDefs: ColDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', key: 'taskNo', field: 'taskNo', width: 170 },
  { title: '仓库', key: 'warehouseName', field: 'warehouseName', width: 110 },
  { title: '货位路径', key: 'route', field: 'route', width: 190, type: 'slot', slotName: 'routeCell' },
  { title: '来源单号', key: 'sourceNo', field: 'sourceNo', width: 130, type: 'slot', slotName: 'sourceNoCell' },
  { title: '总数量', key: 'totalQuantity', field: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'totalQuantityCell' },
  { title: '已移库', key: 'movedQuantity', field: 'movedQuantity', width: 90, align: 'right', type: 'slot', slotName: 'movedQuantityCell' },
  { title: '移库类型', key: 'moveType', field: 'moveType', width: 100, type: 'slot', slotName: 'moveTypeCell' },
  { title: '执行人', key: 'assigneeName', field: 'assigneeName', width: 100 },
  { title: '状态', key: 'status', field: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '备注', key: 'remark', field: 'remark', width: 140 },
  { title: '创建时间', key: 'createTime', field: 'createTime', width: 130, type: 'slot', slotName: 'createTimeCell' },
  { title: '操作', key: 'action', field: 'action', width: 150, fixed: 'right', type: 'slot', slotName: 'actionCell' },
]

const detailColumnDefs: ColDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', key: 'taskNo', field: 'taskNo', width: 160 },
  { title: '商品编码', key: 'productCode', field: 'productCode', width: 110 },
  { title: '商品名称', key: 'productName', field: 'productName', width: 170, ellipsis: true },
  { title: '规格', key: 'productSpec', field: 'productSpec', width: 90 },
  { title: '单位', key: 'productUnit', field: 'productUnit', width: 60 },
  { title: '移库数量', key: 'quantity', field: 'quantity', width: 100, align: 'right', type: 'slot', slotName: 'quantityCell' },
  { title: '源货位', key: 'fromLocationCode', field: 'fromLocationCode', width: 100 },
  { title: '目标货位', key: 'toLocationCode', field: 'toLocationCode', width: 100 },
  { title: '批次号', key: 'batchNo', field: 'batchNo', width: 100 },
  { title: '序列号', key: 'serialNo', field: 'serialNo', width: 110 },
  { title: '明细状态', key: 'itemStatus', field: 'itemStatus', width: 90, type: 'slot', slotName: 'itemStatusCell' },
  { title: '移库类型', key: 'moveType', field: 'moveType', width: 100, type: 'slot', slotName: 'moveTypeCell' },
  { title: '执行人', key: 'assigneeName', field: 'assigneeName', width: 100 },
  { title: '单据状态', key: 'status', field: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '创建时间', key: 'createTime', field: 'createTime', width: 130, type: 'slot', slotName: 'createTimeCell' },
  { title: '操作', key: 'action', field: 'action', width: 150, fixed: 'right', type: 'slot', slotName: 'actionCell' },
]

const docCC = useColumnConfig(docColumnDefs, 'move-order-list-columns-doc')
const detailCC = useColumnConfig(detailColumnDefs, 'move-order-list-columns-detail')

const currentColumns = computed(() => (activeTab.value === 'doc' ? docCC.visibleColumns.value : detailCC.visibleColumns.value))
const panelColumns = computed(() => (activeTab.value === 'doc' ? docCC.settingsColumns.value : detailCC.settingsColumns.value))

function handleColumnConfigChange() {
  if (activeTab.value === 'doc') docCC.onSettingChange()
  else detailCC.onSettingChange()
}
function handleColumnConfigReset() {
  if (activeTab.value === 'doc') docCC.resetSettings()
  else detailCC.resetSettings()
}

// ═══ 页面配置（查询字段显隐/功能按钮/打印） ═══
const PAGE_CONFIG_STORAGE_KEY = 'move-order-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS_DOC: QueryFieldSetting[] = [
  { key: 'keyword', label: '单号', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'sourceNo', label: '来源单号', visible: true },
  { key: 'moveType', label: '移库类型', visible: true },
  { key: 'assigneeName', label: '执行人', visible: true },
  { key: 'dateRange', label: '单据日期', visible: true },
]
const DEFAULT_QUERY_FIELDS_DETAIL: QueryFieldSetting[] = [
  { key: 'keyword', label: '单号', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'productName', label: '商品名称', visible: true },
  { key: 'sourceNo', label: '来源单号', visible: true },
  { key: 'moveType', label: '移库类型', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'config', label: '列配置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryConfigDoc = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS_DOC.map(f => ({ ...f })))
const queryConfigDetail = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS_DETAIL.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => (activeTab.value === 'doc' ? queryConfigDoc.value : queryConfigDetail.value))
const activePageConfigStorageKey = computed(() => `page-config:${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)

function loadPageConfig() {
  const qKey = `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`
  try {
    const raw = localStorage.getItem(qKey)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      const defs = activeTab.value === 'doc' ? DEFAULT_QUERY_FIELDS_DOC : DEFAULT_QUERY_FIELDS_DETAIL
      const target = activeTab.value === 'doc' ? queryConfigDoc : queryConfigDetail
      if (parsed.queryFields) {
        target.value = defs.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
  } catch { /* ignore */ }
  try {
    const raw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-buttons`)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch { /* ignore */ }
}
function handlePageConfigChange(config: any) {
  localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`, JSON.stringify({
    queryFields: config.queryFields || [],
  }))
  localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-buttons`, JSON.stringify({
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}
function isBtnEnabled(key: string) {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 数据加载 ═══
function buildParams() {
  const base: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    keyword: searchParams.keyword || undefined,
    status: searchParams.status ?? undefined,
    sourceNo: searchParams.sourceNo || undefined,
    moveType: searchParams.moveType ?? undefined,
    assigneeName: searchParams.assigneeName || undefined,
    dateStart: searchParams.dateStart || undefined,
    dateEnd: searchParams.dateEnd || undefined,
  }
  return base
}
async function fetchData() {
  loading.value = true
  try {
    const params = buildParams()
    let res: any
    if (activeTab.value === 'doc') {
      res = await moveApi.page(params)
    } else {
      res = await moveApi.pageDetail({
        ...params,
        productName: searchParams.productName || undefined,
        itemRemark: searchParams.itemRemark || undefined,
      })
    }
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e: any) {
    console.warn('[移库单] 列表获取失败', e)
    message.error(e?.data?.message || '获取数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}
function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}
function handleDateChange(_dates: any) {
  if (Array.isArray(dateRange.value) && dateRange.value.length === 2) {
    searchParams.dateStart = (dateRange.value[0] as any)?.format?.('YYYY-MM-DD') || ''
    searchParams.dateEnd = (dateRange.value[1] as any)?.format?.('YYYY-MM-DD') || ''
  } else {
    searchParams.dateStart = ''
    searchParams.dateEnd = ''
  }
}

// ═══ Tab 切换 ═══
function handleTabChange(key: string) {
  activeTab.value = key as 'doc' | 'detail'
  pagination.current = 1
  loadPageConfig()
  fetchData()
}

// ═══ 行为 ═══
function handleAdd() {
  router.push('/wms/move/form')
}
function recordId(record: any) {
  return activeTab.value === 'detail' ? record.taskId : record.id
}
function handleEdit(record: any) {
  router.push({ path: '/wms/move/form', query: { id: recordId(record) } })
}
async function handleDelete(record: any) {
  try {
    await moveApi.remove(recordId(record))
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}
function currentOperator() {
  return { id: userStore?.userId || userStore?.userInfo?.id || 0, name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统' }
}
async function handleStart(record: any) {
  try {
    const u = currentOperator()
    await moveApi.startMove(recordId(record), u.id, u.name)
    message.success('已开始移库')
    fetchData()
  } catch (e: any) { message.error(e?.data?.message || '操作失败') }
}
async function handleExecute(record: any) {
  try {
    const u = currentOperator()
    await moveApi.executeMove(recordId(record), u.id, u.name)
    message.success('移库已执行')
    fetchData()
  } catch (e: any) { message.error(e?.data?.message || '操作失败') }
}
async function handleCancel(record: any) {
  try {
    await moveApi.cancelMove(recordId(record), 'PC端取消')
    message.success('移库单已取消')
    fetchData()
  } catch (e: any) { message.error(e?.data?.message || '操作失败') }
}

// ═══ 打印 / 导出 ═══
function handlePrint() {
  if (tableData.value.length === 0) { message.warning('没有可打印的数据'); return }
  window.print()
}
function handleExport() {
  if (tableData.value.length === 0) { message.warning('没有可导出的数据'); return }
  const cols = currentColumns.value.filter((c: any) => c.key !== 'rowNo' && c.key !== 'action' && c.title)
  const header = cols.map((c: any) => c.title).join(',')
  const lines = tableData.value.map((r: any) => cols.map((c: any) => {
    if (c.key === 'route') return `${r.fromLocationCode || ''}->${r.toLocationCode || ''}`
    const v = c.field ? r[c.field] : ''
    return v === null || v === undefined ? '' : String(typeof v === 'number' ? Number(v).toLocaleString('zh-CN') : v).replace(/,/g, '')
  }).join(','))
  const csv = '\uFEFF' + [header, ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `移库单-${dayjs().format('YYYYMMDDHHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 工具 ═══
function handleError(error: Error) {
  console.error('[移库单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadPageConfig()
  fetchData()
})
</script>

<style scoped>
.search-row-inner { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; padding: 10px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; }
.search-item { display: flex; align-items: center; gap: 6px; }
.btn-search-slot { margin-left: 8px; }
.num-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
