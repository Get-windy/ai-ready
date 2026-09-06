<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="false"
        @tab-change="handleTabChange"
      >
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip title="列配置"><a-button size="small" @click="showColumnConfig = true"><TableOutlined /></a-button></a-tooltip>
            <a-tooltip title="页面配置"><a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button></a-tooltip>
            <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增上架单</a-button>
            <a-button size="small" @click="fetchData"><ReloadOutlined /> 刷新</a-button>
            <a-button size="small" @click="handleExport"><ExportOutlined /> 导出</a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div ref="gridRef" class="search-grid">
                <div class="search-field-item"><a-input v-model:value="searchParams.keyword" placeholder="单号" allow-clear size="small" @press-enter="handleSearch" /></div>
                <div class="search-field-item"><a-input v-model:value="searchParams.sourceOrderNo" placeholder="来源单号" allow-clear size="small" @press-enter="handleSearch" /></div>
                <div class="search-field-item">
                  <a-select v-model:value="searchParams.sourceType" size="small" allow-clear placeholder="来源类型">
                    <a-select-option value="">全部</a-select-option><a-select-option v-for="o in sourceTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
                  </a-select>
                </div>
                <div class="search-field-item">
                  <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="状态">
                    <a-select-option value="">全部</a-select-option><a-select-option v-for="(m, k) in WMS_STATUS_MAP" :key="k" :value="Number(k)">{{ m.text }}</a-select-option>
                  </a-select>
                </div>
                <div v-if="activeTab === 'detail'" class="search-field-item"><a-input v-model:value="searchParams.productName" placeholder="商品名称" allow-clear size="small" @press-enter="handleSearch" /></div>
                <div v-if="activeTab === 'detail'" class="search-field-item"><a-input v-model:value="searchParams.locationCode" placeholder="货位编码" allow-clear size="small" @press-enter="handleSearch" /></div>
                <div ref="actionRef" class="search-action-group" :style="{ gridColumn: 'span ' + actionSpan }"><a-button type="primary" size="small" @click="handleSearch">查询</a-button></div>
              </div>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <BillTableList :columns="currentColumns" :data-source="tableData" :loading="loading" :pagination="billPagination" :show-toolbar="false" :show-search="false" :show-add="false" :show-export="false" :show-batch-delete="false" row-key="id" @page-change="handlePageChange">
              <template #taskNoCell="{ record }"><a-button type="link" size="small" @click="handleView(record)">{{ record.taskNo }}</a-button></template>
              <template #sourceTypeCell="{ record }">{{ sourceTypeText(record.sourceType) }}</template>
              <template #totalQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.totalQuantity) }}</span></template>
              <template #putawayQuantityCell="{ record }"><span class="currency-value">{{ formatQty(record.putawayQuantity) }}</span></template>
              <template #quantityCell="{ record }"><span class="currency-value">{{ formatQty(record.quantity) }}</span></template>
              <template #statusCell="{ record }"><a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag></template>
              <template #taskStatusCell="{ record }"><a-tag :color="statusColor(record.taskStatus)">{{ statusText(record.taskStatus) }}</a-tag></template>
              <template #createTimeCell="{ record }">{{ formatTime(record.createTime) }}</template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button type="link" size="small" @click="goQuality(record)">生成质检单</a-button>
                  <a-button v-if="(record.status || 0) === 0" type="link" size="small" @click="handleStart(record)">开始上架</a-button>
                  <a-button v-if="(record.status || 0) === 1" type="link" size="small" @click="handleConfirm(record)">确认上架</a-button>
                  <a-button v-if="(record.status || 0) === 0" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>
    <ColumnConfigPanel :open="showColumnConfig" :settings-columns="panelColumns" :is-locked-column="isLockedColumn" @update:open="showColumnConfig = $event" @change="handleColumnConfigChange" @reset="handleColumnConfigReset" @drag-end="handleColumnConfigChange" />
    <PageConfigPanel :open="showPageConfig" :query-fields-config="activeQueryFields" :function-buttons-config="buttonConfig" :storage-key="activePageConfigStorageKey" @update:open="showPageConfig = $event" @change="handlePageConfigChange" />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, TableOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { putawayApi } from '@/api/wms/putaway'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, formatQty, formatTime } from '../whTask'

defineOptions({ name: 'WhPutawayOrderList' })

const router = useRouter()

function goQuality(record: any) {
  router.push({
    path: '/quality/inspection/form',
    query: {
      bizType: 'PURCHASE_ORDER',
      bizId: record.sourceOrderId || record.sourceId || record.id,
      bizNo: record.sourceOrderNo || record.sourceNo || record.orderNo,
      warehouseId: record.warehouseId || undefined,
    },
  })
}
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userId || 0)
const currentUserName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统')
const STATUS_MAP = WMS_STATUS_MAP
const sourceTypeOptions = [
  { label: '收货上架', value: 0 }, { label: '退货上架', value: 1 }, { label: '调拨上架', value: 2 }, { label: '其他', value: 3 },
]
function sourceTypeText(t: number | undefined) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }
function statusText(s: number | undefined) { return STATUS_MAP[s ?? 0]?.text || '未知' }
function statusColor(s: number | undefined) { return STATUS_MAP[s ?? 0]?.color || 'default' }

// ═══ 双Tab ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

const showColumnConfig = ref(false)
const showPageConfig = ref(false)
const searchParams = reactive<any>({ keyword: '', sourceType: undefined, status: undefined, productName: '', locationCode: '', sourceOrderNo: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)
const tableData = ref<any[]>([])
const loading = ref(false)

async function fetchData() {
  loading.value = true
  try {
    if (activeTab.value === 'detail') {
      const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
      if (searchParams.keyword) params.taskNo = searchParams.keyword
      if (searchParams.sourceOrderNo) params.sourceOrderNo = searchParams.sourceOrderNo
      if (searchParams.sourceType !== undefined && searchParams.sourceType !== '') params.sourceType = searchParams.sourceType
      if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.locationCode) params.locationCode = searchParams.locationCode
      const res: any = await putawayApi.pageDetail(params)
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const params: any = { current: pagination.current, size: pagination.pageSize }
      if (searchParams.keyword) params.taskNo = searchParams.keyword
      if (searchParams.sourceOrderNo) params.sourceOrderNo = searchParams.sourceOrderNo
      if (searchParams.sourceType !== undefined && searchParams.sourceType !== '') params.sourceType = searchParams.sourceType
      if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
      const res: any = await putawayApi.page(params)
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (e: any) { message.error(e?.message || '查询失败') } finally { loading.value = false }
}
function handleSearch() { pagination.current = 1; fetchData() }
function handlePageChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchData() }
function handleTabChange(key: string) { activeTab.value = key; pagination.current = 1; fetchData() }

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const PAGE_CONFIG_STORAGE_KEY = 'putaway-order-page-config'

const docQueryConfig = ref<QueryFieldSetting[]>([
  { key: 'keyword', label: '单号', visible: true }, { key: 'sourceOrderNo', label: '来源单号', visible: true }, { key: 'sourceType', label: '来源类型', visible: true }, { key: 'status', label: '状态', visible: true },
])
const detailQueryConfig = ref<QueryFieldSetting[]>([
  { key: 'keyword', label: '单号', visible: true }, { key: 'sourceOrderNo', label: '来源单号', visible: true }, { key: 'productName', label: '商品名称', visible: true }, { key: 'locationCode', label: '货位编码', visible: true },
  { key: 'sourceType', label: '来源类型', visible: true }, { key: 'status', label: '状态', visible: true },
])
const buttonConfig = ref<FunctionButtonSetting[]>([
  { key: 'add', label: '新增', enabled: true }, { key: 'refresh', label: '刷新', enabled: true }, { key: 'export', label: '导出', enabled: true }, { key: 'config', label: '配置', enabled: true },
])
const activeQueryFields = computed(() => activeTab.value === 'doc' ? docQueryConfig.value : detailQueryConfig.value)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)
function handlePageConfigChange(config: any) {
  const q = config.queryFields || []
  if (activeTab.value === 'doc') docQueryConfig.value = q; else detailQueryConfig.value = q
  buttonConfig.value = config.functionButtons || buttonConfig.value
  localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}-qf`, JSON.stringify(q))
  localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-btn`, JSON.stringify(buttonConfig.value))
}
function loadPageConfig() {
  try {
    const docQf = JSON.parse(localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-doc-qf`) || 'null'); if (docQf) docQueryConfig.value = docQf
    const detailQf = JSON.parse(localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-detail-qf`) || 'null'); if (detailQf) detailQueryConfig.value = detailQf
    const btn = JSON.parse(localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-btn`) || 'null'); if (btn) buttonConfig.value = btn
  } catch (e) { /* ignore */ }
}

// ═══ 按单据列 ═══
const docColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 180, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '来源类型', field: 'sourceType', key: 'sourceType', width: 100, type: 'slot', slotName: 'sourceTypeCell' },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 150 },
  { title: '总数量', field: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right', type: 'slot', slotName: 'totalQuantityCell' },
  { title: '已上架', field: 'putawayQuantity', key: 'putawayQuantity', width: 90, align: 'right', type: 'slot', slotName: 'putawayQuantityCell' },
  { title: '执行人', field: 'assigneeName', key: 'assigneeName', width: 110 },
  { title: '状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '备注', field: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 130, type: 'slot', slotName: 'createTimeCell' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
]

// ═══ 按明细列 ═══
const detailColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '单号', field: 'taskNo', key: 'taskNo', width: 180, type: 'slot', slotName: 'taskNoCell', sortable: true },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '来源类型', field: 'sourceType', key: 'sourceType', width: 100, type: 'slot', slotName: 'sourceTypeCell' },
  { title: '来源单号', field: 'sourceOrderNo', key: 'sourceOrderNo', width: 150 },
  { title: '商品编码', field: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', field: 'productName', key: 'productName', width: 180 },
  { title: '规格', field: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', field: 'productUnit', key: 'productUnit', width: 64 },
  { title: '上架数量', field: 'quantity', key: 'quantity', width: 100, align: 'right', type: 'slot', slotName: 'quantityCell' },
  { title: '来源货位', field: 'fromLocationCode', key: 'fromLocationCode', width: 110 },
  { title: '目标货位', field: 'toLocationCode', key: 'toLocationCode', width: 110 },
  { title: '批次号', field: 'batchNo', key: 'batchNo', width: 120 },
  { title: '明细状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '任务状态', field: 'taskStatus', key: 'taskStatus', width: 90, type: 'slot', slotName: 'taskStatusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 130, type: 'slot', slotName: 'createTimeCell' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
]

const docColumnDefs = computed(() => docColumns.map(c => ({ ...c })))
const detailColumnDefs = computed(() => detailColumns.map(c => ({ ...c })))
const { onSettingChange, resetSettings, settingsColumns, visibleColumns } = useColumnConfig(docColumnDefs.value, 'putaway-order-list-columns-doc')
const { onSettingChange: onDetailSettingChange, resetSettings: resetDetailSettings, settingsColumns: detailSettingsColumns, visibleColumns: detailVisibleColumns } = useColumnConfig(detailColumnDefs.value, 'putaway-order-list-columns-detail')
const currentColumns = computed(() => activeTab.value === 'doc' ? visibleColumns.value : detailVisibleColumns.value)
const panelColumns = computed(() => activeTab.value === 'doc' ? settingsColumns.value : detailSettingsColumns.value)
function handleColumnConfigChange() { if (activeTab.value === 'doc') onSettingChange(); else onDetailSettingChange() }
function handleColumnConfigReset() { if (activeTab.value === 'doc') resetSettings(); else resetDetailSettings() }

function handleAdd() { router.push('/wh/putaway-order/form') }
function handleView(record: any) { router.push(`/wh/putaway-order/form?id=${record.id}`) }
async function handleStart(record: any) { try { await putawayApi.startPutaway(record.id, currentUserId.value, currentUserName.value); message.success('已开始上架'); fetchData() } catch (e: any) { message.error(e?.message || '操作失败') } }
async function handleConfirm(record: any) { try { await putawayApi.confirmPutaway(record.id, currentUserId.value, currentUserName.value); message.success('上架已确认'); fetchData() } catch (e: any) { message.error(e?.message || '操作失败') } }
async function handleDelete(record: any) { try { await putawayApi.remove(record.id); message.success('删除成功'); fetchData() } catch (e: any) { message.error(e?.message || '删除失败') } }
async function handleExport() {
  const res: any = activeTab.value === 'detail'
    ? await putawayApi.pageDetail({ pageNum: 1, pageSize: 9999 })
    : await putawayApi.page({ current: 1, size: 9999 })
  const rows = res?.records || []
  const cols = activeTab.value === 'detail' ? detailColumns : docColumns
  const headers = cols.filter(c => c.key && c.key !== 'rowNo' && c.key !== 'action').map(c => c.title)
  const lines = rows.map((r: any) => cols.filter(c => c.key && c.key !== 'rowNo' && c.key !== 'action').map(c => r[c.key] ?? '').join(','))
  const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '上架单列表.csv'; a.click()
}
function handleError(e: any) { console.error(e) }
onMounted(async () => { loadPageConfig(); await nextTick(); fetchData() })
</script>

<style scoped>
.search-area { display: flex; flex-direction: column; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(170px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 130px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; min-width: 170px; }
.currency-value { font-variant-numeric: tabular-nums; }
.table-area { width: 100%; }
</style>
