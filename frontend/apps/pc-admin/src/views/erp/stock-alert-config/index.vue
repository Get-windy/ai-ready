<template>
  <ErrorBoundary @reset="fetchData" @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="alert-config-header">
        <div class="alert-config-header__left">
          <span class="alert-config-header__breadcrumb">ERP / 库存管理 / 库存预警配置</span>
          <h2 class="alert-config-header__title">库存预警配置</h2>
        </div>
        <div class="alert-config-header__right">
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"><AlertOutlined /></div>
          <div class="summary-content"><div class="summary-title">总配置数</div><div class="summary-value">{{ statistics.totalConfigs }}</div></div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"><CheckCircleOutlined /></div>
          <div class="summary-content"><div class="summary-title">已启用</div><div class="summary-value">{{ statistics.activeConfigs }}</div></div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"><ExclamationCircleOutlined /></div>
          <div class="summary-content"><div class="summary-title">低库存预警</div><div class="summary-value warning">{{ statistics.lowStockCount }}</div></div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);"><FireOutlined /></div>
          <div class="summary-content"><div class="summary-title">超储预警</div><div class="summary-value warning">{{ statistics.overStockCount }}</div></div>
        </div>
      </a-col>
    </a-row>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :selectable="true"
      :filter-fields="filterFields"
      add-text="新建预警配置"
      @add="handleCreate"
      @search="handleSearch"
      @filter-change="handleFilterChange"
      @page-change="handlePageChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <a-button size="small" v-permission="'erp:stock:checkalerts'" @click="handleCheckAlerts"><BellOutlined /> 立即检查</a-button>
      </template>

      <template #empty>
        <div v-if="hasError" class="table-empty">
          <WarningOutlined class="table-empty-icon" />
          <p class="table-empty-text">数据加载异常，请重试</p>
          <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
        </div>
        <div v-else class="table-empty">
          <BellOutlined class="table-empty-icon" />
          <p class="table-empty-text">暂无预警配置，点击右上角「新建预警配置」开始创建</p>
        </div>
      </template>

      <template #activeCell="{ record }">
        <a-switch :checked="record.active === 1" size="small" @change="(v: any) => handleToggleActive(record, v)" />
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <a-button type="link" size="small" v-permission="'erp:stock:edit'" @click="handleEdit(record)">编辑</a-button>
          <a-popconfirm title="确认删除该预警配置？" @confirm="handleDelete(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 编辑/新建弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑预警配置' : '新建预警配置'" @ok="handleSave" :confirm-loading="saving" width="600px" destroy-on-close>
      <a-form :model="form" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" layout="horizontal">
        <a-form-item label="产品" required>
          <a-select v-model:value="form.productId" show-search :filter-option="filterOption" placeholder="搜索选择产品" :disabled="!!editingId">
            <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">[{{ p.productCode }}] {{ p.productName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="仓库" required>
          <a-select v-model:value="form.warehouseId" placeholder="选择仓库" :disabled="!!editingId">
            <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.warehouseName || w.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="最低库存">
          <a-input-number v-model:value="form.minStock" :min="0" style="width: 100%" placeholder="低于此值触发预警" />
        </a-form-item>
        <a-form-item label="最高库存">
          <a-input-number v-model:value="form.maxStock" :min="0" style="width: 100%" placeholder="高于此值触发预警" />
        </a-form-item>
        <a-form-item label="安全库存">
          <a-input-number v-model:value="form.safetyStock" :min="0" style="width: 100%" placeholder="建议补货点" />
        </a-form-item>
        <a-form-item label="预警类型">
          <a-select v-model:value="form.alertType" placeholder="预警类型">
            <a-select-option value="LOW_STOCK">低库存</a-select-option>
            <a-select-option value="OVER_STOCK">超储</a-select-option>
            <a-select-option value="BOTH">两者</a-select-option>
            <a-select-option value="EXPIRY">保质期</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="启用">
          <a-switch v-model:checked="form.active" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined, AlertOutlined, CheckCircleOutlined, ExclamationCircleOutlined, FireOutlined, BellOutlined, WarningOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

function handleError(err: any) { hasError.value = true; console.warn('[预警配置]', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
}

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const loading = ref(false)
const refreshLoading = ref(false)
const hasError = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const tableData = ref<any[]>([])
const tableRef = ref()
const selectedRows = ref<any[]>([])

const statistics = ref({ totalConfigs: 0, activeConfigs: 0, lowStockCount: 0, overStockCount: 0 })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const searchFilters = reactive<Record<string, any>>({})

const vxeColumns: any = computed(() => [
  { field: 'productCode', title: '产品编码', width: 130 },
  { field: 'productName', title: '产品名称', width: 150 },
  { field: 'warehouseName', title: '仓库', width: 120 },
  { field: 'minStock', title: '最低库存', width: 90, align: 'right' },
  { field: 'maxStock', title: '最高库存', width: 90, align: 'right' },
  { field: 'safetyStock', title: '安全库存', width: 90, align: 'right' },
  { field: 'currentQty', title: '当前库存', width: 90, align: 'right' },
  { field: 'status', title: '预警状态', width: 100, align: 'center' },
  { field: 'active', title: '启用', width: 70, align: 'center', slotName: 'activeCell' },
  { field: 'action', title: '操作', width: 140, fixed: 'right', type: 'action' },
])

const filterFields = computed(() => [
  { key: 'keyword', label: '产品编码/名称', type: 'input' as const, placeholder: '请输入' },
  { key: 'warehouseId', label: '仓库', type: 'select' as const, options: warehouseOptions.value },
  { key: 'active', label: '状态', type: 'select' as const, options: [{ label: '全部', value: '' }, { label: '启用', value: true }, { label: '停用', value: false }] },
])

const warehouseOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ productId: undefined as number | undefined, warehouseId: undefined as number | undefined, minStock: 0, maxStock: 999999, safetyStock: 0, alertType: 'LOW_STOCK', active: true, remark: '' })

const filterOption = (input: string, option: any) => (option.children?.toString() || '').toLowerCase().includes(input.toLowerCase())

async function loadWarehouses() {
  try { const res = await request.get('/erp/stock/warehouses'); warehouseOptions.value = res?.data || [] } catch { warehouseOptions.value = [] }
}
async function loadProducts() {
  try { const res = await request.get('/erp/product/list', { params: { pageSize: 200 } }); const data = res?.data || res; productOptions.value = Array.isArray(data) ? data : (data?.records || []) } catch { productOptions.value = [] }
}

const fetchData = async () => {
  hasError.value = false; loading.value = true
  try {
    const res = await request.get('/erp/stock-alert-config/page', { params: { ...searchFilters, pageNum: pagination.current, pageSize: pagination.pageSize } })
    const data = res?.data || res
    tableData.value = data?.records || []
    pagination.total = data?.total || 0
    // Fetch statistics
    const statsRes = await request.get('/erp/stock-alert-config/statistics')
    if (statsRes?.data) Object.assign(statistics.value, statsRes.data)
    const alertRes = await request.get('/erp/stock/alert')
    if (Array.isArray(alertRes?.data)) {
      statistics.value.lowStockCount = alertRes.data.filter((s: any) => s.quantity < (s.minStock ?? 0)).length
      statistics.value.overStockCount = alertRes.data.filter((s: any) => s.quantity > (s.maxStock ?? 999999)).length
    }
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (err) { hasError.value = true; console.warn('[预警配置] 加载失败', err); message.error('加载预警配置失败') }
  finally { loading.value = false; refreshLoading.value = false }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleFilterChange = (filters: Record<string, any>) => { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => { pagination.current = page; pagination.pageSize = size; fetchData() }
const handleSelectionChange = (rows: any[]) => { selectedRows.value = rows }

const handleCreate = () => {
  editingId.value = null
  form.productId = undefined; form.warehouseId = undefined; form.minStock = 0; form.maxStock = 999999; form.safetyStock = 0
  form.alertType = 'LOW_STOCK'; form.active = true; form.remark = ''
  loadProducts()
  modalVisible.value = true
}

const handleEdit = (record: any) => {
  editingId.value = record.id
  form.productId = record.productId; form.warehouseId = record.warehouseId
  form.minStock = record.minStock ?? 0; form.maxStock = record.maxStock ?? 999999
  form.safetyStock = record.safetyStock ?? 0; form.alertType = record.alertType || 'LOW_STOCK'
  form.active = record.active === 1 || record.active === true; form.remark = record.remark || ''
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.productId) { message.warning('请选择产品'); return }
  if (!form.warehouseId) { message.warning('请选择仓库'); return }
  saving.value = true
  try {
    const payload = { ...form, active: form.active ? 1 : 0 }
    if (editingId.value) {
      await request.put(`/erp/stock-alert-config/${editingId.value}`, payload)
      message.success('更新成功')
    } else {
      await request.post('/erp/stock-alert-config', payload)
      message.success('创建成功')
    }
    modalVisible.value = false; fetchData()
  } catch (err: any) { console.warn('[预警配置] 保存失败', err); message.error(err?.message || '保存失败') }
  finally { saving.value = false }
}

const handleToggleActive = async (record: any, active: boolean) => {
  try {
    if (active) await request.post(`/erp/stock-alert-config/${record.id}/activate`)
    else await request.post(`/erp/stock-alert-config/${record.id}/deactivate`)
    message.success(active ? '已启用' : '已停用'); fetchData()
  } catch { message.error('操作失败') }
}

const handleDelete = async (record: any) => {
  try { await request.delete(`/erp/stock-alert-config/${record.id}`); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

const handleCheckAlerts = async () => {
  try {
    const res = await request.get('/erp/stock-alert-config/check')
    const alerts = res?.data || []
    Modal.info({ title: '预警检查结果', content: `共发现 ${alerts.length} 条预警`, okText: '知道了' })
    fetchData()
  } catch { message.error('检查失败') }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  loadWarehouses(); fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => { fetchData(); autoRefreshCountdown.value = 30 }, 30000)
  countdownTimer = setInterval(() => { if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value-- }, 1000)
})
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.alert-config-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.alert-config-header__left { display: flex; flex-direction: column; gap: 2px; }
.alert-config-header__breadcrumb { font-size: 12px; color: #999; }
.alert-config-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.alert-config-header__right { display: flex; align-items: center; gap: 12px; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #52c41a; white-space: nowrap; }
.data-status { display: inline-flex; align-items: center; gap: 6px; }
.update-time { font-size: 12px; color: #999; white-space: nowrap; }

.summary-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: all 0.3s; }
.summary-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); transform: translateY(-2px); }
.summary-card.highlight { background: linear-gradient(135deg, #fff2f0 0%, #fff1f0 100%); border: 1px solid #ffa39e; }
.summary-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.summary-content { flex: 1; }
.summary-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.summary-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }
.summary-value.warning { color: #f5222d; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; margin-bottom: 12px; }
.table-empty-text { color: #999; margin-bottom: 16px; }

:deep(.vxe-table-list-container) { flex: 1; min-height: 0; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
