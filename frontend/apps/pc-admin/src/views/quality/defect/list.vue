<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="缺陷记录" full-height>
      <CategoryListLayout :show-category-panel="false" :show-table-footer="false">
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip v-if="buttonEnabled('config')" title="列配置">
              <a-button size="small" @click="showColumnConfig = true"><TableOutlined /></a-button>
            </a-tooltip>
            <a-tooltip v-if="buttonEnabled('config')" title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <a-button v-if="buttonEnabled('refresh')" size="small" @click="fetchData">
              <template #icon><ReloadOutlined /></template>刷新
            </a-button>
            <a-button v-if="buttonEnabled('export')" size="small" @click="handleExport">
              <template #icon><ExportOutlined /></template>导出
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div class="search-grid">
                <div v-if="queryFieldVisible('bizNo')" class="search-field-item">
                  <span class="search-label">来源单号</span>
                  <a-input v-model:value="searchParams.bizNo" placeholder="来源单号" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('inspectionId')" class="search-field-item">
                  <span class="search-label">检验记录ID</span>
                  <a-input v-model:value="searchParams.inspectionId" placeholder="关联检验ID" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('defectType')" class="search-field-item">
                  <span class="search-label">缺陷类型</span>
                  <a-select v-model:value="searchParams.defectType" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="QUALITY">质量缺陷</a-select-option>
                    <a-select-option value="PACKAGING">包装缺陷</a-select-option>
                    <a-select-option value="LABELING">标签缺陷</a-select-option>
                  </a-select>
                </div>
                <div v-if="queryFieldVisible('defectLevel')" class="search-field-item">
                  <span class="search-label">缺陷等级</span>
                  <a-select v-model:value="searchParams.defectLevel" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="S">严重(S)</a-select-option>
                    <a-select-option value="Ma">主要(Ma)</a-select-option>
                    <a-select-option value="Mi">次要(Mi)</a-select-option>
                  </a-select>
                </div>
                <div v-if="queryFieldVisible('status')" class="search-field-item">
                  <span class="search-label">状态</span>
                  <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="PENDING">待处理</a-select-option>
                    <a-select-option value="HANDLED">已处理</a-select-option>
                  </a-select>
                </div>
                <div v-if="queryFieldVisible('handlerName')" class="search-field-item">
                  <span class="search-label">处理人</span>
                  <a-input v-model:value="searchParams.handlerName" placeholder="处理人" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('createTime')" class="search-field-item">
                  <span class="search-label">创建日期</span>
                  <a-range-picker
                    v-model:value="createDateRange"
                    size="small"
                    :allow-clear="true"
                    @change="handleDateChange"
                  />
                </div>
                <div class="search-action-group">
                  <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                  <a-button size="small" @click="handleReset">重置</a-button>
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
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #defectTypeCell="{ record }">
                <a-tag>{{ defectTypeText(record.defectType) }}</a-tag>
              </template>
              <template #defectLevelCell="{ record }">
                <a-tag v-if="record.defectLevel" :color="defectLevelColor(record.defectLevel)">{{ defectLevelText(record.defectLevel) }}</a-tag>
                <span v-else>-</span>
              </template>
              <template #handleTypeCell="{ record }">
                <a-tag v-if="record.handleType" :color="handleTypeColor(record.handleType)">{{ handleTypeText(record.handleType) }}</a-tag>
                <span v-else>-</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 0 || record.status === 'PENDING' ? 'warning' : 'success'">
                  {{ record.status === 0 || record.status === 'PENDING' ? '待处理' : '已处理' }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="showDetailModal(record)">查看</a-button>
                  <a-button v-if="record.status === 0 || record.status === 'PENDING'" type="link" size="small" @click="showHandleModal(record)">处理</a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>

      <a-modal v-model:open="handleModalVisible" title="处理不合格" @ok="handleProcess" :confirm-loading="processing">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item v-if="currentRecord?.bizNo" label="来源单号">
            <a-input :value="currentRecord?.bizNo" disabled />
          </a-form-item>
          <a-form-item label="处理方式" required>
            <a-select v-model:value="handleForm.handleType">
              <a-select-option value="RETURN">退货</a-select-option>
              <a-select-option value="CONCESSION">让步接收</a-select-option>
              <a-select-option value="REWORK">返工</a-select-option>
              <a-select-option value="SCRAP">报废</a-select-option>
              <a-select-option value="SPECIAL_RELEASE">特采</a-select-option>
            </a-select>
          </a-form-item>
          <a-alert v-if="handleForm.handleType === 'RETURN'" type="info" show-icon style="margin-bottom: 12px" message="处置将联动生成采购退货单" />
          <a-alert v-if="handleForm.handleType === 'SCRAP'" type="warning" show-icon style="margin-bottom: 12px" message="处置将联动生成报损单并扣减/冻结库存" />
          <a-form-item label="处理数量" required>
            <a-input-number v-model:value="handleForm.handleQuantity" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="处理结果" required>
            <a-textarea v-model:value="handleForm.handleResult" :rows="3" placeholder="请输入处理结果" />
          </a-form-item>
          <a-form-item label="纠正措施">
            <a-textarea v-model:value="handleForm.correctiveAction" :rows="2" placeholder="纠正措施(CAPA，可选)" />
          </a-form-item>
          <a-form-item label="预防措施">
            <a-textarea v-model:value="handleForm.preventiveAction" :rows="2" placeholder="预防措施(CAPA，可选)" />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal v-model:open="detailModalVisible" title="缺陷详情" width="760px" :footer="null">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="检验记录ID">{{ currentDetail?.inspectionId }}</a-descriptions-item>
          <a-descriptions-item label="来源单号">{{ currentDetail?.bizNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="缺陷类型">{{ defectTypeText(currentDetail?.defectType) }}</a-descriptions-item>
          <a-descriptions-item label="缺陷等级">{{ defectLevelText(currentDetail?.defectLevel) }}</a-descriptions-item>
          <a-descriptions-item label="缺陷描述" :span="2">{{ currentDetail?.defectDesc || '-' }}</a-descriptions-item>
          <a-descriptions-item label="缺陷数量">{{ currentDetail?.defectQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理方式">{{ handleTypeText(currentDetail?.handleType) }}</a-descriptions-item>
          <a-descriptions-item label="处理数量">{{ currentDetail?.handleQuantity ?? '-' }}</a-descriptions-item>
          <a-descriptions-item label="处理人">{{ currentDetail?.handlerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ statusText(currentDetail?.status) }}</a-descriptions-item>
          <a-descriptions-item label="采购退货单号">{{ currentDetail?.returnNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="报损单号">{{ currentDetail?.damageNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="纠正措施" :span="2">{{ currentDetail?.correctiveAction || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预防措施" :span="2">{{ currentDetail?.preventiveAction || '-' }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>质检单信息</a-divider>
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="质检单号">{{ inspectionInfo?.qualityNo || inspectionInfo?.bizNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="产品">{{ inspectionInfo?.productName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="批次">{{ inspectionInfo?.batchNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="检验结果">{{ inspectionResultText(inspectionInfo?.inspectionResult) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>质检标准</a-divider>
        <template v-if="standardList.length">
          <a-tag v-for="s in standardList" :key="s.id" color="blue">{{ s.standardName }}</a-tag>
        </template>
        <span v-else>-</span>
        <a-divider>处理历史</a-divider>
        <a-table size="small" :data-source="historyList" :pagination="false" row-key="id" :loading="historyLoading">
          <a-table-column title="处理方式" data-index="handleType" :width="100">
            <template #default="{ record }">{{ handleTypeText(record.handleType) }}</template>
          </a-table-column>
          <a-table-column title="数量" data-index="handleQuantity" :width="80" />
          <a-table-column title="处理人" data-index="handlerName" :width="100" />
          <a-table-column title="处理时间" data-index="handleTime" :width="160" />
          <a-table-column title="纠正措施" data-index="correctiveAction" />
          <a-table-column title="预防措施" data-index="preventiveAction" />
        </a-table>
      </a-modal>
    </PageContainer>

    <ColumnConfigPanel
      :open="showColumnConfig"
      :settings-columns="panelColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showColumnConfig = $event"
      @change="handleColumnConfigChange"
      @reset="handleColumnConfigReset"
      @drag-end="handleColumnConfigChange"
    />
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryConfig"
      :function-buttons-config="buttonConfig"
      :storage-key="PAGE_CONFIG_STORAGE_KEY"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined, ExportOutlined, TableOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig } from '@/composables/useColumnConfig'
import { qualityDefectHandleApi, qualityInspectionApi, qualityStandardApi } from '@/api/quality'

defineOptions({ name: 'QualityDefectList' })

// ── 布局/配置弹窗状态 ──
const showColumnConfig = ref(false)
const showPageConfig = ref(false)
const PAGE_CONFIG_STORAGE_KEY = 'quality-defect-page-config'

// ── 查询/表格状态 ──
const searchParams = reactive<any>({ bizNo: '', inspectionId: '', defectType: '', defectLevel: '', status: '', handlerName: '' })
const createDateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tableData = ref<any[]>([])
const loading = ref(false)

function handleDateChange(dates: any) {
  if (Array.isArray(dates) && dates.length === 2 && dates[0] && dates[1]) {
    searchParams.createTimeStart = dates[0].format('YYYY-MM-DD')
    searchParams.createTimeEnd = dates[1].format('YYYY-MM-DD')
  } else {
    searchParams.createTimeStart = ''
    searchParams.createTimeEnd = ''
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.bizNo) params.bizNo = searchParams.bizNo
    if (searchParams.inspectionId) params.inspectionId = searchParams.inspectionId
    if (searchParams.defectType) params.defectType = searchParams.defectType
    if (searchParams.defectLevel) params.defectLevel = searchParams.defectLevel
    if (searchParams.status) params.status = searchParams.status
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.createTimeStart) params.createTimeStart = searchParams.createTimeStart
    if (searchParams.createTimeEnd) params.createTimeEnd = searchParams.createTimeEnd
    const res: any = await qualityDefectHandleApi.page(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e: any) {
    message.error(e?.message || '查询失败')
  } finally {
    loading.value = false
  }
}
function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  searchParams.bizNo = ''; searchParams.inspectionId = ''; searchParams.defectType = ''; searchParams.defectLevel = ''; searchParams.status = ''; searchParams.handlerName = ''
  searchParams.createTimeStart = ''; searchParams.createTimeEnd = ''
  createDateRange.value = null
  handleSearch()
}
function handlePageChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchData() }

// ── 文本映射 ──
function defectTypeText(t: string) {
  const map: Record<string, string> = { QUALITY: '质量缺陷', PACKAGING: '包装缺陷', LABELING: '标签缺陷' }
  return map[t] || t || '-'
}
function handleTypeText(t: string) {
  const map: Record<string, string> = { RETURN: '退货', CONCESSION: '让步接收', REWORK: '返工', SCRAP: '报废', SPECIAL_RELEASE: '特采' }
  return map[t] || t || '-'
}
function handleTypeColor(t: string) {
  const map: Record<string, string> = { RETURN: 'red', CONCESSION: 'blue', REWORK: 'orange', SCRAP: 'default', SPECIAL_RELEASE: 'purple' }
  return map[t] || 'default'
}
function defectLevelText(t: string) {
  const map: Record<string, string> = { S: '严重(S)', Ma: '主要(Ma)', Mi: '次要(Mi)' }
  return map[t] || t || '-'
}
function defectLevelColor(t: string) {
  const map: Record<string, string> = { S: 'red', Ma: 'orange', Mi: 'blue' }
  return map[t] || 'default'
}
function inspectionResultText(r: string) {
  const map: Record<string, string> = { PASS: '合格', FAIL: '不合格', PENDING: '待检', CONCESSION: '让步接收' }
  return map[r] || r || '-'
}
function statusText(s: any) {
  return s === 0 || s === 'PENDING' ? '待处理' : '已处理'
}

// ── 页面配置（查询条件/功能按钮显隐） ──
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const queryConfig = ref<QueryFieldSetting[]>([
  { key: 'bizNo', label: '来源单号', visible: true },
  { key: 'inspectionId', label: '检验记录ID', visible: true },
  { key: 'defectType', label: '缺陷类型', visible: true },
  { key: 'defectLevel', label: '缺陷等级', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'handlerName', label: '处理人', visible: true },
  { key: 'createTime', label: '创建日期', visible: true },
])
const buttonConfig = ref<FunctionButtonSetting[]>([
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
])
function handlePageConfigChange(config: any) {
  queryConfig.value = config.queryFields || queryConfig.value
  buttonConfig.value = config.functionButtons || buttonConfig.value
}
function queryFieldVisible(key: string) {
  return queryConfig.value.find(f => f.key === key)?.visible !== false
}
function buttonEnabled(key: string) {
  return buttonConfig.value.find(b => b.key === key)?.enabled !== false
}

// ── 列配置 ──
const columns: any[] = [
  { title: '检验记录ID', key: 'inspectionId', width: 100 },
  { title: '来源单号', key: 'bizNo', width: 150, ellipsis: true },
  { title: '缺陷类型', key: 'defectType', width: 100, type: 'slot', slotName: 'defectTypeCell' },
  { title: '缺陷等级', key: 'defectLevel', width: 90, type: 'slot', slotName: 'defectLevelCell' },
  { title: '缺陷描述', key: 'defectDesc', width: 220, ellipsis: true },
  { title: '缺陷数量', key: 'defectQuantity', width: 90, align: 'right' },
  { title: '处理方式', key: 'handleType', width: 100, type: 'slot', slotName: 'handleTypeCell' },
  { title: '处理数量', key: 'handleQuantity', width: 90, align: 'right' },
  { title: '处理人', key: 'handlerName', width: 90 },
  { title: '状态', key: 'status', width: 80, type: 'slot', slotName: 'statusCell' },
  { title: '操作', key: 'action', width: 80, fixed: 'right', type: 'action', slotName: 'actionCell' },
]
const columnDefs = computed(() => columns.map(c => ({ ...c })))
const { onSettingChange, resetSettings, settingsColumns, visibleColumns, isLockedColumn } = useColumnConfig(columnDefs.value, 'quality-defect-list-columns')
const currentColumns = computed(() => visibleColumns.value)
const panelColumns = computed(() => settingsColumns.value)
function handleColumnConfigChange() { onSettingChange() }
function handleColumnConfigReset() { resetSettings() }

// ── 处置 ├
const handleModalVisible = ref(false)
const processing = ref(false)
const currentRecord = ref<any>(null)
const handleForm = reactive({ id: 0, handleType: 'REWORK' as string, handleQuantity: 0, handleResult: '', correctiveAction: '', preventiveAction: '' })

function showHandleModal(record: any) {
  currentRecord.value = record
  handleForm.id = record.id
  handleForm.handleType = record.handleType || 'REWORK'
  handleForm.handleQuantity = record.defectQuantity || 0
  handleForm.handleResult = ''
  handleForm.correctiveAction = ''
  handleForm.preventiveAction = ''
  handleModalVisible.value = true
}

async function handleProcess() {
  if (!handleForm.handleResult) { message.warning('请输入处理结果'); return }
  if (handleForm.handleQuantity === null || handleForm.handleQuantity === undefined) { message.warning('请输入处理数量'); return }
  processing.value = true
  try {
    await qualityDefectHandleApi.handle(handleForm.id, handleForm)
    message.success('处理完成')
    handleModalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '处理失败')
  } finally {
    processing.value = false
  }
}

// ── 缺陷详情/处理历史 ──
const detailModalVisible = ref(false)
const currentDetail = ref<any>(null)
const inspectionInfo = ref<any>(null)
const standardList = ref<any[]>([])
const historyList = ref<any[]>([])
const historyLoading = ref(false)

async function showDetailModal(record: any) {
  currentDetail.value = record
  detailModalVisible.value = true
  inspectionInfo.value = null
  standardList.value = []
  historyList.value = []
  try {
    if (record.inspectionId) {
      const ins: any = await qualityInspectionApi.get(record.inspectionId)
      inspectionInfo.value = ins?.data || null
      if (inspectionInfo.value?.inspectionType) {
        const std: any = await qualityStandardApi.listByType(inspectionInfo.value.inspectionType)
        standardList.value = std?.data || []
      }
    }
  } catch { /* 质检单/标准回查失败不影响主信息 */ }
  try {
    historyLoading.value = true
    const hs: any = await qualityDefectHandleApi.history(record.id)
    historyList.value = hs?.data || []
  } catch {
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

// ── 导出 ──
function getExportValue(record: any, key: string) {
  if (key === 'defectType') return defectTypeText(record.defectType)
  if (key === 'defectLevel') return defectLevelText(record.defectLevel)
  if (key === 'handleType') return handleTypeText(record.handleType)
  if (key === 'status') return statusText(record.status)
  return record[key] ?? ''
}
async function handleExport() {
  try {
    const res: any = await qualityDefectHandleApi.page({ pageNum: 1, pageSize: 9999 })
    const rows = res?.records || []
    const exportCols = columns.filter(c => c.key && c.key !== 'action')
    const headers = exportCols.map(c => c.title)
    const lines = rows.map((r: any) => exportCols.map(c => getExportValue(r, c.key)).join(','))
    const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '缺陷记录.csv'
    a.click()
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  }
}

function handleError(e: any) { console.error(e) }

onMounted(() => { fetchData() })
</script>

<style scoped>
.search-area { display: flex; flex-direction: column; }
.search-container { width: 100%; }
.search-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px 12px;
  align-items: center;
}
.search-field-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  flex-shrink: 0;
}
.search-action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.table-area { width: 100%; }
</style>
