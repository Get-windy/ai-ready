<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="质量证书" full-height>
      <CategoryListLayout :show-category-panel="false" :show-table-footer="false">
        <template #toolbar-left>
          <a-space v-if="selectedRowKeys.length > 0" :size="8">
            <a-button danger size="small" @click="handleBatchDelete">
              <template #icon><DeleteOutlined /></template>删除已选 ({{ selectedRowKeys.length }})
            </a-button>
          </a-space>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip v-if="buttonEnabled('config')" title="列配置">
              <a-button size="small" @click="showColumnConfig = true"><TableOutlined /></a-button>
            </a-tooltip>
            <a-tooltip v-if="buttonEnabled('config')" title="页面配置">
              <a-button size="small" @click="showPageConfig = true"><SettingOutlined /></a-button>
            </a-tooltip>
            <a-button v-if="buttonEnabled('add')" type="primary" size="small" @click="showCreateModal">
              <template #icon><PlusOutlined /></template>新增证书
            </a-button>
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
                <div v-if="queryFieldVisible('productName')" class="search-field-item">
                  <span class="search-label">产品名称</span>
                  <a-input v-model:value="searchParams.productName" placeholder="产品名称" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('batchNo')" class="search-field-item">
                  <span class="search-label">批次号</span>
                  <a-input v-model:value="searchParams.batchNo" placeholder="批次号" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('result')" class="search-field-item">
                  <span class="search-label">检验结论</span>
                  <a-select v-model:value="searchParams.result" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="QUALIFIED">合格</a-select-option>
                    <a-select-option value="UNQUALIFIED">不合格</a-select-option>
                    <a-select-option value="CONDITIONAL">有条件放行</a-select-option>
                  </a-select>
                </div>
                <div v-if="queryFieldVisible('date')" class="search-field-item">
                  <span class="search-label">检验日期</span>
                  <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
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
              :selectable="true"
              :row-selection="rowSelection"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #certificateTypeCell="{ record }">
                <a-tag :color="certTypeColor(record.certificateType)">{{ certTypeText(record.certificateType) }}</a-tag>
              </template>
              <template #resultCell="{ record }">
                <a-tag :color="resultColor(record.result)">{{ resultText(record.result) }}</a-tag>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
              </template>
              <template #certificateUrlCell="{ record }">
                <a v-if="record.certificateUrl" :href="record.certificateUrl" target="_blank">查看附件</a>
                <span v-else>-</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="showViewModal(record)">查看</a-button>
                  <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                  <a-popconfirm title="确定删除该证书？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
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

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑证书' : '新增证书'" width="700px" @ok="handleSave" :confirm-loading="saving">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="证书编号">
              <a-input v-model:value="form.certificateNo" placeholder="留空自动生成" :disabled="!!editingId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="证书类型" required>
              <a-select v-model:value="form.certificateType" placeholder="请选择证书类型">
                <a-select-option value="COA">COA 分析证书</a-select-option>
                <a-select-option value="COC">COC 合格证书</a-select-option>
                <a-select-option value="ISO">ISO 认证</a-select-option>
                <a-select-option value="OTHER">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="产品名称" required>
              <a-input v-model:value="form.productName" placeholder="请输入产品名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="产品编码">
              <a-input v-model:value="form.productCode" placeholder="请输入产品编码" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="批次号">
              <a-input v-model:value="form.batchNo" placeholder="请输入批次号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="供应商名称">
              <a-input v-model:value="form.supplierName" placeholder="请输入供应商名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检验日期">
              <a-date-picker v-model:value="form.inspectionDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发证日期">
              <a-date-picker v-model:value="form.issueDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="有效期至">
              <a-date-picker v-model:value="form.expiryDate" style="width: 100%" value-format="YYYY-MM-DD" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="检验结论" required>
              <a-select v-model:value="form.result" placeholder="请选择检验结论">
                <a-select-option value="QUALIFIED">合格</a-select-option>
                <a-select-option value="UNQUALIFIED">不合格</a-select-option>
                <a-select-option value="CONDITIONAL">有条件放行</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="检验员">
              <a-input v-model:value="form.inspectorName" placeholder="请输入检验员" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status" placeholder="请选择状态">
                <a-select-option :value="0">草稿</a-select-option>
                <a-select-option :value="1">已生效</a-select-option>
                <a-select-option :value="2">已过期</a-select-option>
                <a-select-option :value="3">已撤销</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="证书附件" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
          <a-input v-model:value="form.certificateUrl" placeholder="请输入证书附件URL" />
        </a-form-item>
        <a-form-item label="备注" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
          <a-textarea v-model:value="form.remark" placeholder="请输入备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { PlusOutlined, ReloadOutlined, ExportOutlined, TableOutlined, SettingOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig } from '@/composables/useColumnConfig'
import { qualityCertificateApi } from '@/api/quality'

defineOptions({ name: 'QualityCertificateList' })

// ── 弹窗/配置状态 ──
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const showColumnConfig = ref(false)
const showPageConfig = ref(false)
const PAGE_CONFIG_STORAGE_KEY = 'quality-certificate-page-config'

// ── 查询/分页 ──
const searchParams = reactive<any>({ productName: '', batchNo: '', result: '' })
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tableData = ref<any[]>([])
const loading = ref(false)
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.productName) params.productName = searchParams.productName
    if (searchParams.batchNo) params.batchNo = searchParams.batchNo
    if (searchParams.result) params.result = searchParams.result
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]?.format('YYYY-MM-DD') || ''
      params.endDate = dateRange.value[1]?.format('YYYY-MM-DD') || ''
    }
    const res: any = await qualityCertificateApi.page(params)
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
  searchParams.productName = ''; searchParams.batchNo = ''; searchParams.result = ''
  dateRange.value = null
  handleSearch()
}
function handleDateChange(dates: [Dayjs, Dayjs] | null) { dateRange.value = dates }
function handlePageChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchData() }

// ── 字典 ──
function certTypeText(t: string) {
  const map: Record<string, string> = { COA: 'COA 分析证书', COC: 'COC 合格证书', ISO: 'ISO 认证', OTHER: '其他' }
  return map[t] || t || '-'
}
function certTypeColor(t: string) {
  const map: Record<string, string> = { COA: 'blue', COC: 'green', ISO: 'purple', OTHER: 'default' }
  return map[t] || 'default'
}
function resultText(r: string) {
  const map: Record<string, string> = { QUALIFIED: '合格', UNQUALIFIED: '不合格', CONDITIONAL: '有条件放行' }
  return map[r] || r || '-'
}
function resultColor(r: string) {
  const map: Record<string, string> = { QUALIFIED: 'success', UNQUALIFIED: 'error', CONDITIONAL: 'warning' }
  return map[r] || 'default'
}
function statusText(s: number | string) {
  const map: Record<number, string> = { 0: '草稿', 1: '已生效', 2: '已过期', 3: '已撤销' }
  return map[Number(s)] ?? '-'
}
function statusColor(s: number | string) {
  const map: Record<number, string> = { 0: 'default', 1: 'success', 2: 'error', 3: 'warning' }
  return map[Number(s)] ?? 'default'
}

// ── 页面配置（查询/按钮显隐） ──
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const queryConfig = ref<QueryFieldSetting[]>([
  { key: 'productName', label: '产品名称', visible: true },
  { key: 'batchNo', label: '批次号', visible: true },
  { key: 'result', label: '检验结论', visible: true },
  { key: 'date', label: '检验日期', visible: true },
])
const buttonConfig = ref<FunctionButtonSetting[]>([
  { key: 'add', label: '新增证书', enabled: true },
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
  { title: '证书编号', key: 'certificateNo', width: 180, sortable: true },
  { title: '证书类型', key: 'certificateType', width: 110, type: 'slot', slotName: 'certificateTypeCell' },
  { title: '产品名称', key: 'productName', width: 160, ellipsis: true },
  { title: '产品编码', key: 'productCode', width: 120 },
  { title: '批次号', key: 'batchNo', width: 120 },
  { title: '供应商', key: 'supplierName', width: 150, ellipsis: true },
  { title: '检验日期', key: 'inspectionDate', width: 110 },
  { title: '有效期至', key: 'expiryDate', width: 110 },
  { title: '检验结论', key: 'result', width: 100, type: 'slot', slotName: 'resultCell' },
  { title: '检验员', key: 'inspectorName', width: 100 },
  { title: '状态', key: 'status', width: 80, type: 'slot', slotName: 'statusCell' },
  { title: '附件', key: 'certificateUrl', width: 90, type: 'slot', slotName: 'certificateUrlCell' },
  { title: '操作', key: 'action', width: 160, fixed: 'right', type: 'action', slotName: 'actionCell' },
]
const columnDefs = computed(() => columns.map(c => ({ ...c })))
const { onSettingChange, resetSettings, settingsColumns, visibleColumns, isLockedColumn } = useColumnConfig(columnDefs.value, 'quality-certificate-list-columns')
const currentColumns = computed(() => visibleColumns.value)
const panelColumns = computed(() => settingsColumns.value)
function handleColumnConfigChange() { onSettingChange() }
function handleColumnConfigReset() { resetSettings() }

// ── 表单 ──
const defaultForm = () => ({
  certificateNo: '',
  certificateType: 'COA' as string,
  productName: '',
  productCode: '',
  batchNo: '',
  supplierName: '',
  inspectionDate: '',
  issueDate: '',
  expiryDate: '',
  result: '' as string,
  inspectorId: null as number | null,
  inspectorName: '',
  certificateUrl: '',
  remark: '',
  status: 0,
})
const form = reactive(defaultForm())
function resetForm() { Object.assign(form, defaultForm()) }

function showCreateModal() {
  editingId.value = null
  resetForm()
  modalVisible.value = true
}
function showEditModal(record: any) {
  editingId.value = record.id
  form.certificateNo = record.certificateNo || ''
  form.certificateType = record.certificateType || 'COA'
  form.productName = record.productName || ''
  form.productCode = record.productCode || ''
  form.batchNo = record.batchNo || ''
  form.supplierName = record.supplierName || ''
  form.inspectionDate = record.inspectionDate || ''
  form.issueDate = record.issueDate || ''
  form.expiryDate = record.expiryDate || ''
  form.result = record.result || ''
  form.inspectorId = record.inspectorId || null
  form.inspectorName = record.inspectorName || ''
  form.certificateUrl = record.certificateUrl || ''
  form.remark = record.remark || ''
  form.status = record.status ?? 0
  modalVisible.value = true
}
function showViewModal(record: any) { showEditModal(record) }

async function handleSave() {
  if (!form.productName) { message.warning('请填写产品名称'); return }
  if (!form.certificateType) { message.warning('请选择证书类型'); return }
  if (!form.result) { message.warning('请选择检验结论'); return }
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) {
      await qualityCertificateApi.update(editingId.value, payload as any)
      message.success('更新成功')
    } else {
      await qualityCertificateApi.create(payload as any)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: any) {
  try {
    await qualityCertificateApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}
function handleBatchDelete() {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条证书吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await qualityCertificateApi.delete(id)
        }
        message.success('批量删除成功')
        selectedRowKeys.value = []
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '批量删除失败')
      }
    },
  })
}

// ── 导出 ──
function getExportValue(record: any, key: string) {
  if (key === 'certificateType') return certTypeText(record.certificateType)
  if (key === 'result') return resultText(record.result)
  if (key === 'status') return statusText(record.status)
  if (key === 'certificateUrl') return record.certificateUrl || ''
  return record[key] ?? ''
}
async function handleExport() {
  try {
    const res: any = await qualityCertificateApi.page({ pageNum: 1, pageSize: 9999 })
    const rows = res?.records || []
    const exportCols = columns.filter(c => c.key && c.key !== 'action')
    const headers = exportCols.map(c => c.title)
    const lines = rows.map((r: any) => exportCols.map(c => getExportValue(r, c.key)).join(','))
    const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '质量证书.csv'
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
