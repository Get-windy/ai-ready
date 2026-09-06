<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="质检标准" full-height>
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
            <a-button v-if="buttonEnabled('add')" type="primary" size="small" @click="handleAdd">
              <template #icon><PlusOutlined /></template>新增标准
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
                <div v-if="queryFieldVisible('standardCode')" class="search-field-item">
                  <span class="search-label">标准编码</span>
                  <a-input v-model:value="searchParams.standardCode" placeholder="标准编码" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('standardName')" class="search-field-item">
                  <span class="search-label">标准名称</span>
                  <a-input v-model:value="searchParams.standardName" placeholder="标准名称" allow-clear size="small" @press-enter="handleSearch" />
                </div>
                <div v-if="queryFieldVisible('inspectionType')" class="search-field-item">
                  <span class="search-label">检验类型</span>
                  <a-select v-model:value="searchParams.inspectionType" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option value="INBOUND">入库检验</a-select-option>
                    <a-select-option value="OUTBOUND">出库检验</a-select-option>
                    <a-select-option value="PROCESS">过程检验</a-select-option>
                  </a-select>
                </div>
                <div v-if="queryFieldVisible('status')" class="search-field-item">
                  <span class="search-label">状态</span>
                  <a-select v-model:value="searchParams.status" size="small" allow-clear placeholder="全部">
                    <a-select-option value="">全部</a-select-option>
                    <a-select-option :value="1">启用</a-select-option>
                    <a-select-option :value="0">禁用</a-select-option>
                  </a-select>
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
              <template #inspectionTypeCell="{ record }">
                <a-tag :color="typeColor(record.inspectionType)">{{ typeText(record.inspectionType) }}</a-tag>
              </template>
              <template #sampleRateCell="{ record }">
                {{ record.sampleRate != null ? record.sampleRate + '%' : '-' }}
              </template>
              <template #passThresholdCell="{ record }">
                {{ record.passThreshold != null ? record.passThreshold + '%' : '-' }}
              </template>
              <template #statusCell="{ record }">
                <a-switch :checked="record.status === 1" size="small" :loading="statusLoadingId === record.id" @change="(checked) => handleStatusToggle(record, checked)" />
              </template>
              <template #inspectionItemsCell="{ record }">
                <span class="cell-ellipsis">{{ formatInspectionItems(record.inspectionItems) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                  <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-popconfirm title="确定删除该标准？" ok-text="删除" cancel-text="取消" @confirm="handleDelete(record)">
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
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, TableOutlined, SettingOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useColumnConfig } from '@/composables/useColumnConfig'
import { qualityStandardApi } from '@/api/quality'

defineOptions({ name: 'QualityStandardList' })

const router = useRouter()

// ── 布局/配置弹窗状态 ──
const showColumnConfig = ref(false)
const showPageConfig = ref(false)
const PAGE_CONFIG_STORAGE_KEY = 'quality-standard-page-config'

// ── 查询状态 ──
const searchParams = reactive<any>({ standardCode: '', standardName: '', inspectionType: '', status: '' })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))
const tableData = ref<any[]>([])
const loading = ref(false)

// ── 多选 ──
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

async function fetchData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchParams.standardCode) params.standardCode = searchParams.standardCode
    if (searchParams.standardName) params.standardName = searchParams.standardName
    if (searchParams.inspectionType) params.inspectionType = searchParams.inspectionType
    if (searchParams.status !== '' && searchParams.status !== undefined && searchParams.status !== null) params.status = searchParams.status
    const res: any = await qualityStandardApi.page(params)
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
  searchParams.standardCode = ''; searchParams.standardName = ''; searchParams.inspectionType = ''; searchParams.status = ''
  handleSearch()
}
function handlePageChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; fetchData() }

// ── 检验类型 ──
function typeText(t: string) {
  const map: Record<string, string> = { INBOUND: '入库检验', OUTBOUND: '出库检验', PROCESS: '过程检验' }
  return map[t] || t || '-'
}
function typeColor(t: string) {
  const map: Record<string, string> = { INBOUND: 'blue', OUTBOUND: 'green', PROCESS: 'orange' }
  return map[t] || 'default'
}

// ── 页面配置（查询条件/功能按钮显隐） ──
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
const queryConfig = ref<QueryFieldSetting[]>([
  { key: 'standardCode', label: '标准编码', visible: true },
  { key: 'standardName', label: '标准名称', visible: true },
  { key: 'inspectionType', label: '检验类型', visible: true },
  { key: 'status', label: '状态', visible: true },
])
const buttonConfig = ref<FunctionButtonSetting[]>([
  { key: 'add', label: '新增标准', enabled: true },
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
  { title: '标准编码', key: 'standardCode', width: 150, sortable: true },
  { title: '标准名称', key: 'standardName', width: 180, ellipsis: true },
  { title: '检验类型', key: 'inspectionType', width: 100, type: 'slot', slotName: 'inspectionTypeCell' },
  { title: '抽检比例', key: 'sampleRate', width: 90, align: 'right', type: 'slot', slotName: 'sampleRateCell' },
  { title: '合格阈值', key: 'passThreshold', width: 90, align: 'right', type: 'slot', slotName: 'passThresholdCell' },
  { title: '状态', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '检验项目', key: 'inspectionItems', width: 220, ellipsis: true, type: 'slot', slotName: 'inspectionItemsCell' },
  { title: '描述', key: 'description', width: 200, ellipsis: true },
  { title: '操作', key: 'action', width: 170, fixed: 'right', type: 'action', slotName: 'actionCell' },
]
const columnDefs = computed(() => columns.map(c => ({ ...c })))
const { onSettingChange, resetSettings, settingsColumns, visibleColumns, isLockedColumn } = useColumnConfig(columnDefs.value, 'quality-standard-list-columns')
const currentColumns = computed(() => visibleColumns.value)
const panelColumns = computed(() => settingsColumns.value)
function handleColumnConfigChange() { onSettingChange() }
function handleColumnConfigReset() { resetSettings() }

// ── 检验项目（JSON 数组）解析展示 ──
function parseInspectionItems(json?: string): any[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.filter((i: any) => i && typeof i === 'object') : []
  } catch {
    return []
  }
}
function formatInspectionItems(json?: string) {
  const items = parseInspectionItems(json).filter(i => i.name && i.name.trim())
  if (items.length === 0) return '-'
  return items.map(i => i.name).join(' / ')
}

// ── 操作：跳转 form 页 ──
function handleAdd() { router.push('/quality/standard/form') }
function handleView(record: any) { router.push(`/quality/standard/form?id=${record.id}`) }
function handleEdit(record: any) { router.push(`/quality/standard/form?id=${record.id}`) }

// ── 行内启用/停用 ──
const statusLoadingId = ref<number | null>(null)
async function handleStatusToggle(record: any, checked: boolean) {
  const newStatus = checked ? 1 : 0
  if (record.status === newStatus) return
  statusLoadingId.value = record.id
  try {
    await qualityStandardApi.updateStatus(record.id, newStatus)
    record.status = newStatus
    message.success(checked ? '已启用' : '已停用')
  } catch (e: any) {
    message.error(e?.message || '操作失败')
    fetchData()
  } finally {
    statusLoadingId.value = null
  }
}

// ── 删除 / 批量删除 ──
async function handleDelete(record: any) {
  try {
    await qualityStandardApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}
function handleBatchDelete() {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条标准吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await qualityStandardApi.delete(id)
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
  if (key === 'inspectionItems') return formatInspectionItems(record.inspectionItems)
  if (key === 'sampleRate' || key === 'passThreshold') return record[key] != null ? record[key] + '%' : ''
  if (key === 'status') return record.status === 1 ? '启用' : '禁用'
  if (key === 'inspectionType') return typeText(record.inspectionType)
  return record[key] ?? ''
}
async function handleExport() {
  try {
    const res: any = await qualityStandardApi.page({ pageNum: 1, pageSize: 9999 })
    const rows = res?.records || []
    const exportCols = columns.filter(c => c.key && c.key !== 'action')
    const headers = exportCols.map(c => c.title)
    const lines = rows.map((r: any) => exportCols.map(c => getExportValue(r, c.key)).join(','))
    const csv = '\ufeff' + [headers.join(','), ...lines].join('\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = '质检标准.csv'
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
.cell-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-block; max-width: 100%; }
</style>
