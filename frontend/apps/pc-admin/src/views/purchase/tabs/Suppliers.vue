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
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的供应商，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无供应商数据，点击右上角「新建供应商」开始创建
          </p>
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
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
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
    <a-modal
      v-model:open="detailVisible"
      title="供应商详情"
      width="800px"
      centered
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="供应商编码">{{ currentRecord.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="供应商名称">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ currentRecord.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ currentRecord.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="等级">
          <a-tag :color="getLevelColor(currentRecord.supplierLevel)">{{ getLevelText(currentRecord.supplierLevel) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentRecord.status === 1 ? 'green' : 'default'">
            {{ currentRecord.status === 1 ? '正常' : '停用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="邮箱">{{ currentRecord.email || '-' }}</a-descriptions-item>
        <a-descriptions-item label="地址">{{ currentRecord.address || '-' }}</a-descriptions-item>
        <a-descriptions-item label="开户银行">{{ currentRecord.bankName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="银行账号">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>

      <!-- 合作产品列表 -->
      <div class="detail-products-section">
        <div class="detail-products-title">合作产品</div>
        <VxeTableList
          :columns="productColumns"
          :data-source="supplierProducts"
          :pagination="false"
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

      <div class="detail-modal-footer">
        <a-button @click="handleViewOrders(currentRecord)">查看订单</a-button>
        <a-button type="primary" @click="handleEdit(currentRecord)">编辑</a-button>
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>

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
          <a-select v-model:value="batchEditData.level" placeholder="请选择等级（留空不修改）" allow-clear>
            <a-select-option :value="1">A级</a-select-option>
            <a-select-option :value="2">B级</a-select-option>
            <a-select-option :value="3">C级</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合作状态">
          <a-select v-model:value="batchEditData.status" placeholder="请选择状态（留空不修改）" allow-clear>
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
  StarOutlined, SwitcherOutlined, FileTextOutlined, AppstoreOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { supplierApi } from '@/api/supplier'
import { useUserStore } from '@/stores/user'
import { useExport } from '@/composables/useExport'
import { executeBatch } from '@/utils/batchOperations'

const { execute: executeExport } = useExport()
const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()
const loading = ref(false)
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
  { title: '等级', field: 'supplierLevel', width: 80, align: 'center', formatter: ({ cellValue }) => getLevelText(cellValue) },
  { title: '状态', field: 'status', width: 80, align: 'center', formatter: ({ cellValue }) => cellValue === 1 ? '正常' : '停用' },
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
const supplierProducts = ref<any[]>([])

const productColumns = [
  { title: '产品编码', field: 'productCode', width: 120 },
  { title: '产品名称', field: 'productName', width: 150 },
  { title: '规格型号', field: 'spec', width: 100 },
  { title: '供应价', field: 'price', width: 100, align: 'right', slotName: 'priceCell' },
  { title: '最近采购', field: 'lastOrderDate', width: 100 }
]

function handleView(record: any) {
  currentRecord.value = record
  supplierProducts.value = record.products || mockProducts()
  detailVisible.value = true
}

function handleViewOrders(record: any) {
  router.push({ path: '/purchase', query: { tab: 'orders', supplierId: record.id } })
  detailVisible.value = false
}

function mockProducts(): any[] {
  return [
    { id: 1, productCode: 'P001', productName: '物料A', spec: '规格1', price: 100, lastOrderDate: '2024-01-15' },
    { id: 2, productCode: 'P002', productName: '物料B', spec: '规格2', price: 200, lastOrderDate: '2024-01-10' },
    { id: 3, productCode: 'P003', productName: '物料C', spec: '规格3', price: 150, lastOrderDate: '2024-01-08' }
  ]
}

function handleAdd() { router.push('/supplier/create') }
function handleEdit(record: any) { router.push(`/supplier/edit/${record.id}`) }

async function handleDelete(record: any) {
  Modal.confirm({
    title: '删除供应商', content: `确认删除供应商 "${record.supplierName}"？删除后数据不可恢复。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await supplierApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
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
      try { await supplierApi.update(record.id, { status }); message.success(`${statusText}成功`); fetchData() }
      catch { message.error(`${statusText}失败`) }
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

const transferData = ref([
  { key: '1', title: '优质供应商' }, { key: '2', title: '长期合作' },
  { key: '3', title: '战略伙伴' }, { key: '4', title: '紧急备用' },
  { key: '5', title: '新品开发' }, { key: '6', title: '低优先级' }
])

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
  const result = await executeBatch(selectedRowKeys.value, (id) => supplierApi.update(id, { status: batchStatusValue.value }), `批量${statusText}`)
  batchStatusSubmitting.value = false
  if (result.successCount > 0) fetchData()
  batchStatusModalVisible.value = false
}

async function fetchData() {
  loading.value = true
  try {
    const res = await supplierApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || mockData()
    pagination.total = pageData.total || mockData().length
    lastUpdated.value = new Date().toISOString()
  } catch {
    message.error('获取供应商列表失败')
    dataSource.value = mockData()
  } finally { loading.value = false }
}

const mockData = (): any[] => [
  { id: 1, supplierCode: 'SUP001', supplierName: '北京优质供应商', contactPerson: '张经理', contactPhone: '13800138001', supplierLevel: 'A', status: 1, createTime: '2024-01-10 10:00' },
  { id: 2, supplierCode: 'SUP002', supplierName: '上海贸易公司', contactPerson: '李主管', contactPhone: '13800138002', supplierLevel: 'B', status: 1, createTime: '2024-01-12 11:00' },
  { id: 3, supplierCode: 'SUP003', supplierName: '广州制造企业', contactPerson: '王总', contactPhone: '13800138003', supplierLevel: 'A', status: 1, createTime: '2024-01-15 09:00' },
  { id: 4, supplierCode: 'SUP004', supplierName: '深圳电子公司', contactPerson: '赵经理', contactPhone: '13800138004', supplierLevel: 'C', status: 0, createTime: '2024-01-08 14:00' },
  { id: 5, supplierCode: 'SUP005', supplierName: '杭州供应商', contactPerson: '孙经理', contactPhone: '13800138005', supplierLevel: 'B', status: 1, createTime: '2024-01-18 15:00' },
  { id: 6, supplierCode: 'SUP006', supplierName: '成都材料厂', contactPerson: '周总', contactPhone: '13800138006', supplierLevel: 'A', status: 1, createTime: '2024-01-20 16:00' },
  { id: 7, supplierCode: 'SUP007', supplierName: '武汉配件商', contactPerson: '吴经理', contactPhone: '13800138007', supplierLevel: 'B', status: 0, createTime: '2024-01-22 17:00' }
]

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }

const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters); pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:refresh', fetchData)
})
</script>

<style scoped>
.purchase-suppliers-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
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

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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
</style>
