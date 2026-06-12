<template>
  <div class="purchase-suppliers-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-active">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.active }}</div>
          <div class="stat-card-label">正常合作</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-inactive">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.inactive }}</div>
          <div class="stat-card-label">已停用</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-a">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ levelCounts.A }}</div>
          <div class="stat-card-label">A级供应商</div>
        </div>
        <StarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">供应商总数</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'purchase-supplier-list'"
      :filter-fields="filterFields"
      :show-summary="true"
      :summary-data="summaryData"
      :show-export="true"
      :selectable="true"
      add-text="新建供应商"
      @add="handleAdd"
      @edit="handleEdit"
      @view="handleView"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @sort-change="handleSortChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @cell-dblclick="handleView"
      @selection-change="(keys: number[]) => { selectedRowKeys = keys }"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #batch-actions>
        <a-button size="small" type="primary" ghost @click="handleBatchEdit">
          <template #icon><EditOutlined /></template>
          批量编辑
        </a-button>
        <a-button size="small" @click="handleBatchStatus">
          <template #icon><SwitcherOutlined /></template>
          批量调整状态
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <template v-if="hasError">
            <WarningOutlined class="table-empty-icon" style="color: #faad14" />
            <p class="table-empty-text">加载失败</p>
            <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
              <ReloadOutlined /> 重试
            </a-button>
          </template>
          <template v-else>
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的供应商，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无供应商数据，点击右上角「新建供应商」开始创建
            </p>
          </template>
        </div>
      </template>

      <template #action="{ record }">
        <a-space :size="4">
          <a-tooltip title="查看详情">
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="编辑">
            <a-button type="link" size="small" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                <a-menu-item key="viewOrders">
                  <FileTextOutlined /> 查看订单
                </a-menu-item>
                <a-menu-item key="viewProducts">
                  <AppstoreOutlined /> 合作产品
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-if="record.status === 1" key="disable">
                  <StopOutlined /> 停用
                </a-menu-item>
                <a-menu-item v-else key="enable">
                  <CheckCircleOutlined /> 启用
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-drawer
      v-model:open="detailVisible"
      title="供应商详情"
      placement="right"
      width="80vw"
      :footer="null"
      @close="handleDetailClose"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" size="small" @click="handleDetailRefresh" :loading="detailLoading">
            <template #icon><ReloadOutlined /></template>
          </a-button>
          <a-button size="small" @click="handleViewOrders(currentRecord)">查看订单</a-button>
          <a-button type="primary" size="small" @click="handleEdit(currentRecord)">编辑</a-button>
        </a-space>
      </template>

      <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 12 }">
        <template v-if="detailData">
          <a-descriptions bordered :column="2">
            <a-descriptions-item label="供应商编码">{{ detailData.supplierCode }}</a-descriptions-item>
            <a-descriptions-item label="供应商名称">{{ detailData.supplierName }}</a-descriptions-item>
            <a-descriptions-item label="联系人">{{ detailData.contactPerson }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ detailData.contactPhone }}</a-descriptions-item>
            <a-descriptions-item label="等级">
              <a-tag :color="getLevelColor(detailData.supplierLevel)">{{ getLevelText(detailData.supplierLevel) }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="detailData.status === 1 ? 'green' : 'default'">
                {{ detailData.status === 1 ? '正常' : '停用' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ detailData.email || '-' }}</a-descriptions-item>
            <a-descriptions-item label="地址">{{ detailData.address || '-' }}</a-descriptions-item>
            <a-descriptions-item label="开户银行">{{ detailData.bankName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="银行账号">{{ detailData.bankAccount || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
            <a-descriptions-item label="更新时间">{{ detailData.updateTime || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>

          <!-- 合作产品列表 -->
          <div class="detail-products-section">
            <div class="detail-products-title">合作产品</div>
            <VxeTableList
              :columns="productColumns"
              :data-source="supplierProducts"
              :pagination="false as any"
              row-key="id"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #priceCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.price) }}</span>
              </template>
            </VxeTableList>
          </div>
        </template>
        <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
          <template #extra>
            <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.id)">重试</a-button>
          </template>
        </a-result>
      </a-skeleton>
    </a-drawer>

    <!-- 批量编辑弹窗 -->
    <a-modal
      v-model:open="batchEditModalVisible"
      title="批量编辑供应商"
      width="700px"
      centered
      :confirm-loading="batchEditSubmitting"
      ok-text="确认修改"
      cancel-text="取消"
      @ok="handleBatchEditConfirm"
      @cancel="batchEditModalVisible = false"
    >
      <a-alert :message="`已选择 ${selectedRowKeys.length} 个供应商`" type="info" show-icon style="margin-bottom: 16px;" />
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="供应商等级">
          <a-select v-model:value="batchEditData.level" size="small" placeholder="请选择等级（留空不修改）" allow-clear>
            <a-select-option :value="1">A级</a-select-option>
            <a-select-option :value="2">B级</a-select-option>
            <a-select-option :value="3">C级</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合作状态">
          <a-select v-model:value="batchEditData.status" size="small" placeholder="请选择状态（留空不修改）" allow-clear>
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="类别标签">
          <a-transfer
            :data-source="transferData"
            :titles="['可选标签', '已选标签']"
            :target-keys="batchEditData.tags"
            :render="(item: any) => item.title"
            @change="(nextTargetKeys: string[]) => { batchEditData.tags = nextTargetKeys }"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="batchEditData.remark" placeholder="批量备注（将覆盖原有备注）" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量调整状态弹窗 -->
    <a-modal
      v-model:open="batchStatusModalVisible"
      title="批量调整状态"
      width="500px"
      centered
      :confirm-loading="batchStatusSubmitting"
      ok-text="确认"
      cancel-text="取消"
      @ok="handleBatchStatusConfirm"
      @cancel="batchStatusModalVisible = false"
    >
      <a-alert :message="`已选择 ${selectedRowKeys.length} 个供应商`" type="info" show-icon style="margin-bottom: 16px;" />
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="调整为">
          <a-radio-group v-model:value="batchStatusValue">
            <a-radio :value="1">正常合作</a-radio>
            <a-radio :value="0">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseSuppliersTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  EyeOutlined, EditOutlined, DeleteOutlined, SearchOutlined, InboxOutlined,
  EllipsisOutlined, CheckCircleOutlined, StopOutlined, TeamOutlined,
  StarOutlined, SwitcherOutlined, FileTextOutlined, AppstoreOutlined,
  WarningOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { supplierApi } from '@/api/supplier'
import { dictItemApi } from '@/api/dict'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'
import { executeBatch } from '@/utils/batchOperations'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const active = dataSource.value.filter(r => r.status === 1).length
  const inactive = dataSource.value.filter(r => r.status === 0).length
  return { active, inactive }
})

const levelCounts = computed(() => {
  const A = dataSource.value.filter(r => r.supplierLevel === 'A' || r.supplierLevel === 'A级').length
  return { A }
})

const vxeColumns = computed(() => [
  { title: '供应商编码', field: 'supplierCode', width: 130 },
  { title: '供应商名称', field: 'supplierName', width: 160 },
  { title: '联系人', field: 'contactPerson', width: 100 },
  { title: '联系电话', field: 'contactPhone', width: 120 },
  { title: '等级', field: 'supplierLevel', width: 80, align: 'center', formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${getLevelColor(cellValue)}">${getLevelText(cellValue)}</span>` },
  { title: '状态', field: 'status', width: 80, align: 'center', formatter: ({ cellValue }: any) => `<span class="ant-tag ant-tag-${cellValue === 1 ? 'green' : 'default'}">${cellValue === 1 ? '正常' : '停用'}</span>` },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 160, fixed: 'right' }
])

const filterFields = [
  { key: 'supplierCode', label: '供应商编码', type: 'input' as const, placeholder: '输入编码' },
  { key: 'supplierName', label: '供应商名称', type: 'input' as const, placeholder: '输入名称' },
  { key: 'keyword', label: '关键词', type: 'input' as const, placeholder: '搜索关键词' },
  { key: 'supplierLevel', label: '等级', type: 'select' as const, options: [
    { label: 'A级', value: 'A' }, { label: 'B级', value: 'B' }, { label: 'C级', value: 'C' }
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '正常', value: 1 }, { label: '停用', value: 0 }
  ]}
]

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

const levelColorMap: Record<string, string> = { 'A': 'gold', 'B': 'blue', 'C': 'default', 'A级': 'gold', 'B级': 'blue', 'C级': 'default' }
const levelTextMap: Record<string, string> = { 'A': 'A级', 'B': 'B级', 'C': 'C级', 'A级': 'A级', 'B级': 'B级', 'C级': 'C级' }

function getLevelColor(level: string): string { return levelColorMap[level] || 'default' }
function getLevelText(level: string): string { return levelTextMap[level] || level }
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)
const supplierProducts = ref<any[]>([])

const productColumns = [
  { title: '产品编码', field: 'productCode', width: 120 },
  { title: '产品名称', field: 'productName', width: 150 },
  { title: '规格型号', field: 'spec', width: 100 },
  { title: '供应价', field: 'price', width: 100, align: 'right', slotName: 'priceCell' },
  { title: '最近采购', field: 'lastOrderDate', width: 100 }
]

async function fetchDetail(id: number) {
  detailLoading.value = true
  detailError.value = null
  try {
    const res = await supplierApi.getById(id) as any
    const data = (res as any).data ?? res
    detailData.value = data
    supplierProducts.value = data.products || []
  } catch (err: any) {
    console.warn('[供应商] 获取详情失败', err)
    detailError.value = err?.message || '获取详情失败'
    detailData.value = null
    supplierProducts.value = []
  } finally {
    detailLoading.value = false
  }
}

function handleView(record: any) {
  currentRecord.value = record
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.id)
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = null
  detailError.value = null
  supplierProducts.value = []
}

function handleDetailRefresh() {
  if (detailRecord.value?.id) fetchDetail(detailRecord.value.id)
}

function handleViewOrders(record: any) {
  router.push({ path: '/purchase', query: { tab: 'orders', supplierId: record.id } })
  detailVisible.value = false
}

function handleAdd() { router.push('/supplier/create') }
function handleEdit(record: any) { router.push(`/supplier/edit/${record.id}`) }

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除供应商', content: `确认删除供应商 "${record.supplierName}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await supplierApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch (e) { console.warn('[供应商] 删除失败', e); message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'viewOrders': handleViewOrders(record); break
    case 'viewProducts': handleView(record); break
    case 'enable': handleToggleStatus(record, 1); break
    case 'disable': handleToggleStatus(record, 0); break
    case 'delete': handleDelete(record); break
  }
}

function handleToggleStatus(record: any, status: number) {
  const statusText = status === 1 ? '启用' : '停用'
  Modal.confirm({
    title: `${statusText}供应商`, content: `${statusText}供应商 "${record.supplierName}"？`, okText: '确认', centered: true,
    async onOk() {
      try { await supplierApi.update(record.id, { status } as any); message.success(`${statusText}成功`); fetchData() }
      catch (e) { console.warn('[供应商] 状态变更失败', e); message.error(`${statusText}失败`) }
    }
  })
}

async function handleBatchDelete(ids: number[]) {
  const result = await executeBatch(ids, (id) => supplierApi.delete(id), '批量删除')
  if (result.successCount > 0) fetchData()
}

function handleExport() {
  executeExport({
    fileName: '供应商',
    headers: ['供应商编码', '供应商名称', '联系人', '联系电话', '等级', '状态', '创建时间'],
    fetchAll: () => supplierApi.page({ pageNum: 1, pageSize: pagination.total, tenantId: userStore.tenantId, ...searchFilters }),
    mapToRows: (list: any[]) => list.map((row: any) => [
      row.supplierCode || '', row.supplierName || '', row.contactPerson || '', row.contactPhone || '',
      getLevelText(row.supplierLevel), row.status === 1 ? '正常' : '停用', row.createTime || ''
    ]),
    fallbackRows: () => dataSource.value.map((row: any) => [
      row.supplierCode || '', row.supplierName || '', row.contactPerson || '', row.contactPhone || '',
      getLevelText(row.supplierLevel), row.status === 1 ? '正常' : '停用', row.createTime || ''
    ]),
    total: pagination.total,
  })
}

// ── 批量编辑状态 ──────────────────────────────────────
const batchEditModalVisible = ref(false)
const batchEditSubmitting = ref(false)
const batchEditData = reactive({
  level: undefined as number | undefined,
  status: undefined as number | undefined,
  tags: [] as string[],
  remark: ''
})

const transferData = ref<{ key: string; title: string }[]>([])

async function loadTagOptions() {
  try {
    const res = await dictItemApi.getByDictCode('SUPPLIER_TAG')
    if (res.data) {
      transferData.value = res.map(item => ({ key: item.itemValue, title: item.itemText }))
    }
  } catch (err) {
    console.warn('[供应商] 加载标签失败', err)
  }
}

function handleBatchEdit() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择供应商'); return }
  batchEditData.level = undefined; batchEditData.status = undefined
  batchEditData.tags = []; batchEditData.remark = ''
  batchEditModalVisible.value = true
}

const handleBatchEditConfirm = async () => {
  const updateData: any = {}
  if (batchEditData.level !== undefined) updateData.supplierLevel = batchEditData.level
  if (batchEditData.status !== undefined) updateData.cooperationStatus = batchEditData.status
  if (batchEditData.tags.length > 0) updateData.tags = batchEditData.tags
  if (batchEditData.remark) updateData.remark = batchEditData.remark
  if (Object.keys(updateData).length === 0) { message.warning('请至少选择一个要修改的字段'); return }

  batchEditSubmitting.value = true
  const result = await executeBatch(selectedRowKeys.value, (id) => supplierApi.update(id, updateData), '批量编辑')
  batchEditSubmitting.value = false
  if (result.successCount > 0) fetchData()
  batchEditModalVisible.value = false
}

// ── 批量调整状态 ──────────────────────────────────────
const batchStatusModalVisible = ref(false)
const batchStatusSubmitting = ref(false)
const batchStatusValue = ref(1)

function handleBatchStatus() {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择供应商'); return }
  batchStatusValue.value = 1
  batchStatusModalVisible.value = true
}

const handleBatchStatusConfirm = async () => {
  batchStatusSubmitting.value = true
  const statusText = batchStatusValue.value === 1 ? '启用' : '停用'
  const result = await executeBatch(selectedRowKeys.value, (id) => supplierApi.update(id, { status: batchStatusValue.value } as any), `批量${statusText}`)
  batchStatusSubmitting.value = false
  if (result.successCount > 0) fetchData()
  batchStatusModalVisible.value = false
}

async function fetchData() {
  loading.value = true
  try {
    const res = await supplierApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || []
    pagination.total = pageData.total || 0
    lastUpdated.value = new Date().toISOString()
    hasError.value = false
  } catch (e) {
    console.warn('[供应商] 获取列表失败', e)
    message.error('获取供应商列表失败')
    dataSource.value = []
    hasError.value = true
  } finally { loading.value = false }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }

const debouncedFetch = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleParentCreate() { handleAdd() }

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd) }
}

onMounted(() => {
  fetchData()
  loadTagOptions()
  refreshTimer = setInterval(() => fetchData(), 30000)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:create', handleParentCreate)
  window.addEventListener('purchase:refresh', fetchData)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  clearTimeout(debouncedFetch.value)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:create', handleParentCreate)
  window.removeEventListener('purchase:refresh', fetchData)
})

defineExpose({ handleQuery: fetchData })

</script>

<style scoped>
.purchase-suppliers-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 让 VxeTableList 填满剩余空间 */
.purchase-suppliers-tab > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-card-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-card-inactive { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-card-a { background: linear-gradient(135deg, #fffbe6 0%, #fff1b8 100%); }
.stat-card-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 32px;
  color: #999;
}

/* 空状态 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.supplier-code, .supplier-name {
  font-weight: 500;
}

.phone-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

/* 详情弹窗 */
.detail-products-section {
  margin-top: 16px;
}

.detail-products-title {
  font-weight: 500;
  margin-bottom: 8px;
}

/* drawer 内容样式 */
.detail-drawer-body {
  padding: 0;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

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
