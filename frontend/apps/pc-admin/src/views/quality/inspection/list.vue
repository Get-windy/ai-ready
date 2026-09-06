<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
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
            <a-button type="primary" size="small" @click="goCreate">
              <PlusOutlined /> 新增
            </a-button>
            <a-button size="small" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid" ref="gridRef">
                <div class="search-field-item">
                  <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.bizNo" placeholder="来源单号" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.productName" placeholder="产品名称" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <a-input v-model:value="searchParams.inspectorName" placeholder="检验员" allow-clear size="small" />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">检验结果</span>
                    <a-select v-model:value="searchParams.result" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="PENDING">待检验</a-select-option>
                      <a-select-option value="PASS">合格</a-select-option>
                      <a-select-option value="CONCESSION">让步接收</a-select-option>
                      <a-select-option value="FAIL">不合格</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-action-group" ref="actionRef" :style="{ gridColumn: 'span ' + actionSpan }">
                  <div class="search-field-item search-action-item">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="false"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #qualityNoCell="{ record }">
                <a-button type="link" size="small" @click="showDetail(record)">
                  {{ record.qualityNo || '-' }}
                </a-button>
              </template>
              <template #resultCell="{ record }">
                <a-tag :color="resultColor(record.inspectionResult)">{{ resultText(record.inspectionResult) }}</a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button v-if="record.status === 0" type="link" size="small" @click="showCompleteModal(record)">完成检验</a-button>
                  <a-button v-else type="link" size="small" @click="showDetail(record)">查看</a-button>
                  <a-button v-if="record.inspectionResult === 'FAIL'" type="link" size="small" danger @click="showDefectModal(record)">不合格处理</a-button>
                  <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- 列配置 -->
    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="settingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="onSettingChange"
      @reset="resetSettings"
    />

    <!-- 页面配置 -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- 完成检验弹窗 -->
    <a-modal v-model:open="completeModalVisible" title="完成检验" @ok="handleComplete" :confirm-loading="completing">
      <a-alert type="info" show-icon style="margin-bottom: 12px" message="入库单关联的待检明细，检验结果将同步反馈至入库单状态。" />
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item v-if="currentInspection?.bizNo" label="来源单号">
          <a-input :value="currentInspection?.bizNo" disabled />
        </a-form-item>
        <a-form-item label="检验结果" required>
          <a-select v-model:value="completeForm.result">
            <a-select-option value="PASS">合格</a-select-option>
            <a-select-option value="CONCESSION">让步接收</a-select-option>
            <a-select-option value="FAIL">不合格（拒收）</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合格数量">
          <a-input-number v-model:value="completeForm.passQuantity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="不合格数量">
          <a-input-number v-model:value="completeForm.failQuantity" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="检验备注">
          <a-textarea v-model:value="completeForm.remark" :rows="2" placeholder="检验备注（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 不合格处理弹窗 -->
    <a-modal v-model:open="defectModalVisible" title="不合格处理" @ok="handleDefect" :confirm-loading="defecting">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="来源单号">
          <a-input :value="currentInspection?.bizNo" disabled />
        </a-form-item>
        <a-form-item label="缺陷类型" required>
          <a-select v-model:value="defectForm.defectType">
            <a-select-option value="QUALITY">质量缺陷</a-select-option>
            <a-select-option value="PACKAGING">包装缺陷</a-select-option>
            <a-select-option value="LABELING">标签缺陷</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="缺陷描述" required>
          <a-textarea v-model:value="defectForm.defectDesc" :rows="3" placeholder="请描述缺陷详情" />
        </a-form-item>
        <a-form-item label="缺陷数量">
          <a-input-number v-model:value="defectForm.defectQuantity" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 质检单详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="质检单详情" :footer="null" width="640">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="质检单号">{{ currentDetail?.qualityNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源单号">{{ currentDetail?.bizNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="检验类型">{{ inspectionTypeText(currentDetail?.inspectionType) }}</a-descriptions-item>
        <a-descriptions-item label="产品名称">{{ currentDetail?.productName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="批次号">{{ currentDetail?.batchNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="检验数量">{{ currentDetail?.quantity ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="抽检数量">{{ currentDetail?.sampleQuantity ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="检验结果">
          <a-tag :color="resultColor(currentDetail?.inspectionResult)">{{ resultText(currentDetail?.inspectionResult) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="合格 / 不合格">
          <span :style="{ color: '#52c41a' }">{{ currentDetail?.passQuantity ?? 0 }}</span>
          /
          <span :style="{ color: '#f5222d' }">{{ currentDetail?.failQuantity ?? 0 }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="检验员">{{ currentDetail?.inspectorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="检验时间">{{ currentDetail?.inspectionTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentDetail?.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider style="margin: 12px 0" />
      <div style="font-weight: 600; margin-bottom: 8px">关联缺陷记录</div>
      <a-table
        v-if="defectRecords.length"
        :columns="defectColumns"
        :data-source="defectRecords"
        :pagination="false"
        size="small"
        row-key="id"
      />
      <a-empty v-else description="无缺陷记录" :image-style="{ height: '40px' }" />
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import {
  TableOutlined, SettingOutlined, PlusOutlined, ReloadOutlined, ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { qualityInspectionApi, qualityDefectHandleApi } from '@/api/quality'

const router = useRouter()
const activeTab = ref('pending')
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tabs = [
  { key: 'pending', label: '待检验' },
  { key: 'finished', label: '已检验' },
  { key: 'failed', label: '不合格' },
]

// ── 搜索区 ──
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(7, 'day'), dayjs()])
const searchParams = reactive({
  bizNo: '',
  productName: '',
  inspectorName: '',
  result: undefined as string | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
})

// ── 弹窗状态 ──
const showColumnConfig = ref(false)
const showPageConfig = ref(false)
const completeModalVisible = ref(false)
const defectModalVisible = ref(false)
const detailVisible = ref(false)
const completing = ref(false)
const defecting = ref(false)
const currentInspection = ref<any>(null)
const currentDetail = ref<any>(null)
const defectRecords = ref<any[]>([])
const completeForm = reactive({ result: 'PASS' as string, passQuantity: 0, failQuantity: 0, remark: '' })
const defectForm = reactive({ inspectionId: 0, defectType: 'QUALITY' as string, defectDesc: '', defectQuantity: 0 })

// ── 列定义（质检单三 Tab 共用一套） ──
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 170, fixed: 'right', slotName: 'actionCell' },
  { title: '质检单号', field: 'qualityNo', key: 'qualityNo', width: 170, type: 'slot', slotName: 'qualityNoCell', sortable: true },
  { title: '来源单号', field: 'bizNo', key: 'bizNo', width: 150, sortable: true },
  { title: '产品名称', field: 'productName', key: 'productName', width: 200, ellipsis: true },
  { title: '批次号', field: 'batchNo', key: 'batchNo', width: 120 },
  { title: '检验类型', field: 'inspectionType', key: 'inspectionType', width: 110 },
  { title: '检验数量', field: 'quantity', key: 'quantity', width: 110, align: 'right' },
  { title: '抽检数量', field: 'sampleQuantity', key: 'sampleQuantity', width: 110, align: 'right' },
  { title: '检验结果', field: 'inspectionResult', key: 'inspectionResult', width: 100, align: 'center', type: 'slot', slotName: 'resultCell' },
  { title: '检验员', field: 'inspectorName', key: 'inspectorName', width: 100 },
  { title: '检验时间', field: 'inspectionTime', key: 'inspectionTime', width: 160 },
  { title: '备注', field: 'remark', key: 'remark', width: 150, ellipsis: true },
]
const { visibleColumns, settingsColumns, onSettingChange, resetSettings } = useColumnConfig(columns, 'quality-inspection-list-columns')
const currentColumns = computed(() => visibleColumns.value)

// ── 页面配置 ──
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const PAGE_CONFIG_STORAGE_KEY = 'quality-inspection-page-config'
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '检验日期', visible: true },
  { key: 'bizNo', label: '来源单号', visible: true },
  { key: 'productName', label: '产品名称', visible: true },
  { key: 'inspectorName', label: '检验员', visible: true },
  { key: 'result', label: '检验结果', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]
const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const activeQueryFields = computed(() => queryConfig.value)
const pageConfigStorageKey = computed(() => PAGE_CONFIG_STORAGE_KEY)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      if (parsed.queryFields) {
        queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({ queryFields: config.queryFields || [] }))
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY + '-buttons', JSON.stringify({ functionButtons: config.functionButtons || functionButtonConfig.value }))
  loadPageConfig()
}

// ── 数据加载 ──
async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (activeTab.value === 'pending') params.result = 'PENDING'
    else if (activeTab.value === 'finished') params.excludeResult = 'PENDING'
    else if (activeTab.value === 'failed') params.result = 'FAIL'
    if (searchParams.bizNo) params.bizNo = searchParams.bizNo
    if (searchParams.productName) params.productName = searchParams.productName
    if (searchParams.inspectorName) params.inspectorName = searchParams.inspectorName
    if (searchParams.result !== undefined && searchParams.result !== '') params.result = searchParams.result
    if (searchParams.startDate) params.dateStart = searchParams.startDate
    if (searchParams.endDate) params.dateEnd = searchParams.endDate

    const res: any = await qualityInspectionApi.page(params)
    const body = res?.data ?? res
    tableData.value = body?.records || []
    pagination.total = Number(body?.total) || 0
  } catch (error: any) {
    message.error(error?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
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

// ── 行操作 ──
function resultText(r: string) {
  const map: Record<string, string> = { PENDING: '待检验', PASS: '合格', CONCESSION: '让步接收', FAIL: '不合格' }
  return map[r] || r || '待检验'
}
function resultColor(r: string) {
  const map: Record<string, string> = { PENDING: 'orange', PASS: 'green', CONCESSION: 'blue', FAIL: 'red' }
  return map[r] || 'default'
}
function inspectionTypeText(t: string) {
  const map: Record<string, string> = { INBOUND: '入库检验', OUTBOUND: '出库检验', PROCESS: '过程检验' }
  return map[t] || t || '-'
}

const defectColumns = [
  { title: '缺陷类型', dataIndex: 'defectType', key: 'defectType' },
  { title: '缺陷描述', dataIndex: 'defectDesc', key: 'defectDesc' },
  { title: '缺陷数量', dataIndex: 'defectQuantity', key: 'defectQuantity', align: 'right' as const },
  { title: '处理状态', dataIndex: 'status', key: 'status', width: 90 },
]

async function loadDefects(inspectionId: number) {
  try {
    const res: any = await qualityDefectHandleApi.page({ pageNum: 1, pageSize: 50, inspectionId })
    const body = res?.data ?? res
    defectRecords.value = body?.records || []
  } catch {
    defectRecords.value = []
  }
}

function showCompleteModal(record: any) {
  currentInspection.value = record
  completeForm.result = record.inspectionResult === 'CONCESSION' ? 'CONCESSION' : 'PASS'
  completeForm.passQuantity = record.sampleQuantity || record.quantity
  completeForm.failQuantity = 0
  completeForm.remark = ''
  completeModalVisible.value = true
}

async function handleComplete() {
  if (!completeForm.result) { message.warning('请选择检验结果'); return }
  completing.value = true
  try {
    await qualityInspectionApi.complete(currentInspection.value.id, completeForm)
    message.success('检验完成')
    completeModalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '检验完成失败')
  } finally {
    completing.value = false
  }
}

function showDefectModal(record: any) {
  currentInspection.value = record
  defectForm.inspectionId = record.id
  defectForm.defectType = 'QUALITY'
  defectForm.defectDesc = ''
  defectForm.defectQuantity = record.failQuantity || 0
  defectModalVisible.value = true
}

async function handleDefect() {
  if (!defectForm.defectDesc) { message.warning('请输入缺陷描述'); return }
  defecting.value = true
  try {
    await qualityDefectHandleApi.create(defectForm)
    message.success('已创建不合格处理记录')
    defectModalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '创建不合格记录失败')
  } finally {
    defecting.value = false
  }
}

function showDetail(record: any) {
  currentDetail.value = record
  detailVisible.value = true
  loadDefects(record.id)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '删除质检单',
    content: `确认删除质检单 ${record.qualityNo || record.bizNo || ''} 吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await qualityInspectionApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

function goCreate() {
  router.push('/quality/inspection/form')
}

function handleExport() {
  try {
    const headers = ['质检单号', '来源单号', '产品名称', '批次号', '检验类型', '检验数量', '抽检数量', '检验结果', '检验员', '检验时间', '备注']
    const rows = tableData.value.map((r: any) => [
      r.qualityNo, r.bizNo, r.productName, r.batchNo, inspectionTypeText(r.inspectionType),
      r.quantity, r.sampleQuantity, resultText(r.inspectionResult), r.inspectorName, r.inspectionTime, r.remark,
    ])
    const csv = [headers.join(','), ...rows.map(row => row.map(v => `"${v === null || v === undefined ? '' : String(v).replace(/"/g, '""')}"`).join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '质检单.csv'
    a.click()
    URL.revokeObjectURL(a.href)
  } catch {
    message.error('导出失败')
  }
}

function handleError(err: any) {
  console.error('[质检单列表] 渲染异常', err)
}

onMounted(() => {
  loadPageConfig()
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; }
.search-action-item { flex-shrink: 0; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.table-area { height: 100%; }
</style>
