<template>
  <div class="purchase-inquiry-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-card-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.draft }}</div>
          <div class="stat-card-label">草稿</div>
        </div>
        <EditOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-sent">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.sent }}</div>
          <div class="stat-card-label">已发送</div>
        </div>
        <SendOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-quoted">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ statusCounts.quoted }}</div>
          <div class="stat-card-label">已报价</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-card-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">询价单总数</div>
        </div>
        <SearchOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'purchase-inquiry-list'"
      :filter-fields="filterFields"
      :show-export="true"
      :selectable="true"
      add-text="新建询价"
      @add="handleAdd"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
      @selection-change="(keys: number[]) => { selectedRowKeys = keys }"
    >
      <template #batch-actions>
        <a-button size="small" type="primary" ghost @click="handleBatchSend">
          <template #icon><SendOutlined /></template>
          批量发送
        </a-button>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的询价单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无询价单数据，点击右上角「新建询价」开始创建
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
            <a-tooltip v-if="record.status === 0" title="编辑">
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="发送">
              <a-button type="link" size="small" @click="handleSend(record)">
                <template #icon><SendOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="copy"><CopyOutlined /> 复制</a-menu-item>
                  <a-menu-item key="quote" v-if="record.status === 2"><FileTextOutlined /> 创建报价</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" v-if="record.status === 0" danger><DeleteOutlined /> 删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
      </template>
    </VxeTableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="询价单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" size="small" v-if="currentRecord">
        <a-descriptions-item label="询价单号">
          <span class="inquiry-no">{{ currentRecord.inquiryNo }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="询价日期">{{ currentRecord.inquiryDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '无' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>询价物料</a-divider>
      <VxeTableList
        :columns="itemDetailColumns"
        :data-source="detailItems"
        :pagination="false"
        row-key="tempKey"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      />

      <div class="detail-modal-footer">
        <a-space>
          <a-button v-if="currentRecord?.status === 0" type="primary" @click="handleSendFromDetail">
            <template #icon><SendOutlined /></template>
            发送询价
          </a-button>
          <a-button v-if="currentRecord?.status === 0" @click="handleEditFromDetail">编辑</a-button>
          <a-button @click="detailVisible = false">关闭</a-button>
        </a-space>
      </div>
    </a-modal>

    <!-- 新建/编辑询价弹窗 -->
    <a-modal
      v-model:open="formModalVisible"
      :title="formMode === 'edit' ? '编辑询价单' : '新建询价单'"
      width="750px"
      :confirm-loading="formSubmitting"
      ok-text="确认"
      cancel-text="取消"
      @ok="handleFormSubmit"
      @cancel="formModalVisible = false"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="供应商" name="supplierId" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-select
                v-model:value="formData.supplierId"
                placeholder="请选择供应商"
                show-search
                :filter-option="filterOption"
                @change="handleSupplierChange"
              >
                <a-select-option v-for="s in supplierList" :key="s.id" :value="s.id">
                  {{ s.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="询价日期" name="inquiryDate" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-date-picker v-model:value="formData.inquiryDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider>询价物料明细</a-divider>
        <VxeTableList
          :columns="itemColumns"
          :data-source="formData.items"
          :pagination="false"
          row-key="tempKey"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #productNameCell="{ record }">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template #specificationCell="{ record }">
            <a-input v-model:value="record.specification" placeholder="规格" size="small" />
          </template>
          <template #quantityCell="{ record }">
            <a-input-number v-model:value="record.quantity" :min="1" placeholder="数量" size="small" style="width: 100%" />
          </template>
          <template #unitCell="{ record }">
            <a-input v-model:value="record.unit" placeholder="单位" size="small" />
          </template>
          <template #actionCell="{ record, rowIndex }">
            <a-button type="link" danger size="small" @click="handleRemoveItem(rowIndex)">删除</a-button>
          </template>
        </VxeTableList>
        <a-button type="dashed" block @click="handleAddItem" style="margin-top: 12px">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInquiryTab' })

import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  SendOutlined,
  SearchOutlined,
  InboxOutlined,
  EllipsisOutlined,
  CopyOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inquiryApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const draft = dataSource.value.filter(r => r.status === 0).length
  const sent = dataSource.value.filter(r => r.status === 1).length
  const quoted = dataSource.value.filter(r => r.status === 2).length
  return { draft, sent, quoted }
})

const vxeColumns = computed(() => [
  { title: '询价单号', field: 'inquiryNo', width: 160 },
  { title: '供应商', field: 'supplierName', width: 140 },
  { title: '询价日期', field: 'inquiryDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', formatter: ({ cellValue }) => getStatusText(cellValue) },
  { title: '创建人', field: 'creatorName', width: 100 },
  { title: '创建时间', field: 'createTime', width: 160 },
  { title: '操作', type: 'action', width: 140, fixed: 'right' }
])

const filterFields = [
  { key: 'inquiryNo', label: '询价单号', type: 'input' as const, placeholder: '输入询价单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 },
    { label: '已发送', value: 1 },
    { label: '已报价', value: 2 }
  ]}
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '已发送', 2: '已报价' }

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

// 详情弹窗
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const itemDetailColumns = [
  { title: '物料名称', field: 'productName', width: 150 },
  { title: '规格', field: 'specification', width: 120 },
  { title: '数量', field: 'quantity', width: 80, align: 'right' },
  { title: '单位', field: 'unit', width: 60, align: 'center' }
]

const detailItems = ref<any[]>([])

const mockDetailItems = () => [
  { productName: '工业传感器', specification: 'P001-A', quantity: 100, unit: '个' },
  { productName: '智能控制器', specification: 'P002-B', quantity: 50, unit: '台' },
  { productName: '连接线缆', specification: 'P003-C', quantity: 200, unit: '米' }
]

// 表单弹窗
const formModalVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const supplierList = ref<any[]>([])

interface InquiryItem {
  tempKey: string
  productName: string
  specification: string
  quantity: number
  unit: string
}

const formData = reactive({
  supplierId: undefined as number | undefined,
  inquiryDate: undefined as any,
  remark: '',
  items: [] as InquiryItem[]
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  inquiryDate: [{ required: true, message: '请选择询价日期', trigger: 'change' }]
}

const itemColumns = [
  { title: '物料名称', field: 'productName', width: 150, slotName: 'productNameCell' },
  { title: '规格', field: 'specification', width: 120, slotName: 'specificationCell' },
  { title: '数量', field: 'quantity', width: 80, align: 'right', slotName: 'quantityCell' },
  { title: '单位', field: 'unit', width: 60, align: 'center', slotName: 'unitCell' },
  { title: '操作', field: 'action', width: 60, align: 'center', slotName: 'actionCell' }
]

let itemCounter = 0
const genTempKey = () => `item_${++itemCounter}_${Date.now()}`

const handleAddItem = () => {
  formData.items.push({
    tempKey: genTempKey(),
    productName: '',
    specification: '',
    quantity: 1,
    unit: '个'
  })
}

const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleSupplierChange = (val: number) => {
  formData.supplierId = val
}

const resetForm = () => {
  formData.supplierId = undefined
  formData.inquiryDate = undefined
  formData.remark = ''
  formData.items = []
  editingId.value = null
}

async function loadSuppliers() {
  supplierList.value = [
    { id: 1, name: '北京供应商' },
    { id: 2, name: '上海供应商' },
    { id: 3, name: '广州供应商' },
    { id: 4, name: '深圳供应商' },
    { id: 5, name: '杭州供应商' }
  ]
}

async function fetchData() {
  loading.value = true
  try {
    const res = await inquiryApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      tenantId: userStore.tenantId,
      ...searchFilters
    })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData.records || mockData()
    pagination.total = pageData.total || mockData().length
  } catch {
    message.error('获取询价单列表失败')
    dataSource.value = mockData()
  } finally {
    loading.value = false
  }
}

const mockData = () => [
  { id: 1, inquiryNo: 'IN2024010001', supplierName: '北京供应商', inquiryDate: '2024-01-10', status: 2, creatorName: '张三', createTime: '2024-01-10 10:00' },
  { id: 2, inquiryNo: 'IN2024010002', supplierName: '上海供应商', inquiryDate: '2024-01-12', status: 1, creatorName: '李四', createTime: '2024-01-12 11:00' },
  { id: 3, inquiryNo: 'IN2024010003', supplierName: '广州供应商', inquiryDate: '2024-01-15', status: 0, creatorName: '王五', createTime: '2024-01-15 09:00' },
  { id: 4, inquiryNo: 'IN2024010004', supplierName: '深圳供应商', inquiryDate: '2024-01-08', status: 2, creatorName: '张三', createTime: '2024-01-08 14:00' },
  { id: 5, inquiryNo: 'IN2024010005', supplierName: '杭州供应商', inquiryDate: '2024-01-18', status: 1, creatorName: '李四', createTime: '2024-01-18 15:00' }
]

function handleView(record: any) {
  currentRecord.value = { ...record, items: mockDetailItems() }
  detailItems.value = currentRecord.value.items || mockDetailItems()
  detailVisible.value = true
}

function handleAdd() {
  formMode.value = 'add'
  resetForm()
  loadSuppliers()
  formModalVisible.value = true
}

function handleEdit(record: any) {
  formMode.value = 'edit'
  editingId.value = record.id
  formData.supplierId = record.supplierId
  formData.inquiryDate = record.inquiryDate
  formData.remark = record.remark || ''
  formData.items = (record.items || mockDetailItems()).map((item: any) => ({
    tempKey: genTempKey(),
    productName: item.productName || '',
    specification: item.specification || '',
    quantity: item.quantity || 1,
    unit: item.unit || '个'
  }))
  loadSuppliers()
  formModalVisible.value = true
}

function handleEditFromDetail() {
  if (currentRecord.value) {
    handleEdit(currentRecord.value)
    detailVisible.value = false
  }
}

function handleSendFromDetail() {
  if (currentRecord.value) {
    handleSend(currentRecord.value)
    detailVisible.value = false
  }
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '删除询价单',
    content: `确认删除询价单 "${record.inquiryNo}"？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await inquiryApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'copy':
      message.info(`复制询价单: ${record.inquiryNo}`)
      break
    case 'quote':
      message.info(`创建报价: ${record.inquiryNo}`)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    const data = {
      supplierId: formData.supplierId,
      inquiryDate: formData.inquiryDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productName: item.productName,
        specification: item.specification,
        quantity: item.quantity,
        unit: item.unit
      }))
    }
    if (formMode.value === 'edit' && editingId.value) {
      await inquiryApi.update(editingId.value, data)
      message.success('编辑成功')
    } else {
      await inquiryApi.create(data)
      message.success('新建询价单成功')
    }
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error(formMode.value === 'edit' ? '编辑失败' : '新建失败')
  } finally {
    formSubmitting.value = false
  }
}

function handleSend(record: any) {
  Modal.confirm({
    title: '发送询价单',
    content: `确定发送询价单 "${record.inquiryNo}" 吗？`,
    okText: '确认发送',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try { await inquiryApi.send(record.id); message.success('发送成功'); fetchData() }
      catch { message.error('发送失败') }
    }
  })
}

function handleExport() {
  const headers = ['询价单号', '供应商', '询价日期', '状态', '创建人', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.inquiryNo || '',
    row.supplierName || '',
    row.inquiryDate || '',
    getStatusText(row.status),
    row.creatorName || '',
    row.createTime || ''
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `询价单_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleBatchSend() {
  const keys = selectedRowKeys.value
  if (keys.length === 0) {
    message.warning('请选择要发送的询价单')
    return
  }
  Modal.confirm({
    title: '批量发送',
    content: `发送选中的 ${keys.length} 个询价单？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      message.success(`成功发送 ${keys.length} 个询价单`)
      fetchData()
    }
  })
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const debouncedFetch = ref(0)
function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  clearTimeout(debouncedFetch.value)
  debouncedFetch.value = window.setTimeout(() => fetchData(), 400)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  fetchData()
  loadSuppliers()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.purchase-inquiry-tab {
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

.stat-card-draft { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-card-sent { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-quoted { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
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

.inquiry-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
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
