<template>
  <div class="asset-list">
    <!-- Statistics Cards -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-statistic title="资产总数" :value="statistics.totalCount || 0" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="已启用" :value="statistics.activeCount || 0" :value-style="{ color: '#3f8600' }" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="资产原值" :value="statistics.totalOriginalValue || 0" :precision="2" prefix="¥" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="资产净值" :value="statistics.totalNetValue || 0" :precision="2" prefix="¥" />
      </a-col>
    </a-row>

    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'fixed-asset-asset-list'"
      :filter-fields="filterFields"
      add-text="新增资产"
      @add="showCreateModal"
      @edit="editAsset"
      @delete="handleDeleteWithConfirm"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
        <a-button danger ghost @click="handleBatchDelete">批量删除</a-button>
      </template>
      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配资产记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无资产数据">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button @click="showCreateModal">新增资产</a-button>
        </a-empty>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusMap[record.status] || record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'useStatus'">
          <a-tag>{{ useStatusMap[record.useStatus] || record.useStatus }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="编辑">
              <a-button type="link" size="small" @click="editAsset(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="depreciate">折旧</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" danger>删除</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </TableList>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑资产' : '新增资产'"
      :width="800"
      @ok="handleModalOk"
      :confirmLoading="modalLoading"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="资产编码" required>
              <a-input v-model:value="formData.assetCode" placeholder="自动生成可不填" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="资产名称" required>
              <a-input v-model:value="formData.assetName" placeholder="请输入资产名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="分类">
              <a-select v-model:value="formData.categoryId" placeholder="选择分类" allow-clear>
                <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.categoryName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="购置日期">
              <a-date-picker v-model:value="formData.purchaseDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="原值">
              <a-input-number v-model:value="formData.originalValue" :precision="2" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="净值">
              <a-input-number v-model:value="formData.netValue" :precision="2" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="折旧方法">
              <a-select v-model:value="formData.depreciationMethod" placeholder="选择折旧方法">
                <a-select-option value="straight_line">直线法</a-select-option>
                <a-select-option value="double_declining">双倍余额递减法</a-select-option>
                <a-select-option value="sum_of_years">年数总和法</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="使用年限(月)">
              <a-input-number v-model:value="formData.usefulLife" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="残值">
              <a-input-number v-model:value="formData.salvageValue" :precision="2" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="残值率(%)">
              <a-input-number v-model:value="formData.salvageRate" :precision="2" style="width: 100%" :min="0" :max="100" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="使用部门">
              <a-input v-model:value="formData.departmentName" placeholder="部门名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保管人">
              <a-input v-model:value="formData.custodianName" placeholder="保管人姓名" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="存放地点">
              <a-input v-model:value="formData.location" placeholder="存放地点" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="formData.status" placeholder="选择状态">
                <a-select-option value="draft">草稿</a-select-option>
                <a-select-option value="active">已启用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="规格型号">
              <a-input v-model:value="formData.specification" placeholder="规格型号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="品牌">
              <a-input v-model:value="formData.brand" placeholder="品牌" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="供应商">
              <a-input v-model:value="formData.supplierName" placeholder="供应商" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="发票号">
              <a-input v-model:value="formData.invoiceNo" placeholder="发票号" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="使用状态">
              <a-select v-model:value="formData.useStatus" placeholder="使用状态">
                <a-select-option value="in_use">使用中</a-select-option>
                <a-select-option value="idle">闲置</a-select-option>
                <a-select-option value="maintenance">维修中</a-select-option>
                <a-select-option value="disposed">已处置</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保修到期">
              <a-date-picker v-model:value="formData.warrantyEndDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import { fixedAssetApi, fixedAssetCategoryApi } from '@/api/fixed-asset'

interface FixedAssetRecord {
  id: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName: string
  purchaseDate: string
  originalValue: number
  netValue: number
  depreciationMethod: string
  usefulLife: number
  salvageValue: number
  salvageRate: number
  monthlyDepreciation: number
  accumulatedDepreciation: number
  status: string
  location: string
  departmentId: string
  departmentName: string
  custodianId: string
  custodianName: string
  specification: string
  brand: string
  supplierName: string
  invoiceNo: string
  warrantyEndDate: string
  description: string
  useStatus: string
  assetPhoto: string
  remark: string
}

interface Category {
  id: number
  categoryName: string
}

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref<FixedAssetRecord[]>([])
const categories = ref<Category[]>([])
const selectedRowKeys = ref<number[]>([])
const statistics = ref<any>({})
const tableRef = ref()
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const formData = reactive<FixedAssetRecord>({
  id: 0,
  assetCode: '',
  assetName: '',
  categoryId: 0,
  categoryName: '',
  purchaseDate: undefined as any,
  originalValue: undefined as any,
  netValue: undefined as any,
  depreciationMethod: 'straight_line',
  usefulLife: undefined as any,
  salvageValue: undefined as any,
  salvageRate: undefined as any,
  monthlyDepreciation: undefined as any,
  accumulatedDepreciation: undefined as any,
  status: 'draft',
  location: '',
  departmentId: '',
  departmentName: '',
  custodianId: '',
  custodianName: '',
  specification: '',
  brand: '',
  supplierName: '',
  invoiceNo: '',
  warrantyEndDate: undefined as any,
  description: '',
  useStatus: 'in_use',
  assetPhoto: '',
  remark: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const columns = [
  { title: '资产编码', dataIndex: 'assetCode', key: 'assetCode', width: 140 },
  { title: '资产名称', dataIndex: 'assetName', key: 'assetName', width: 180 },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 120 },
  { title: '原值', dataIndex: 'originalValue', key: 'originalValue', width: 120 },
  { title: '净值', dataIndex: 'netValue', key: 'netValue', width: 120 },
  { title: '月折旧额', dataIndex: 'monthlyDepreciation', key: 'monthlyDepreciation', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '使用状态', dataIndex: 'useStatus', key: 'useStatus', width: 100 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '保管人', dataIndex: 'custodianName', key: 'custodianName', width: 100 },
  { title: '购置日期', dataIndex: 'purchaseDate', key: 'purchaseDate', width: 120 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
]

const filterFields = computed(() => [
  { key: 'assetCode', label: '资产编码', type: 'input' as const, placeholder: '资产编码' },
  { key: 'assetName', label: '资产名称', type: 'input' as const, placeholder: '资产名称' },
  { key: 'categoryId', label: '分类', type: 'select' as const, options: categories.value.map(c => ({ label: c.categoryName, value: c.id })) },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已启用', value: 'active' },
    { label: '已转移', value: 'transferred' },
    { label: '已处置', value: 'disposed' },
    { label: '已报废', value: 'scrapped' },
  ]},
  { key: 'departmentId', label: '部门', type: 'input' as const, placeholder: '部门ID' },
])

const statusMap: Record<string, string> = {
  draft: '草稿',
  active: '已启用',
  transferred: '已转移',
  disposed: '已处置',
  scrapped: '已报废',
}

const statusColorMap: Record<string, string> = {
  draft: 'default',
  active: 'green',
  transferred: 'blue',
  disposed: 'red',
  scrapped: 'orange',
}

const useStatusMap: Record<string, string> = {
  in_use: '使用中',
  idle: '闲置',
  maintenance: '维修中',
  disposed: '已处置',
}

onMounted(() => {
  fetchData()
  fetchStatistics()
  fetchCategories()
  document.addEventListener('keydown', handleKeydown)
})

function fetchData() {
  loading.value = true
  const params: any = {
    ...searchFilters,
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  fixedAssetApi.getPage(params).then((res: any) => {
    if (res.data) {
      tableData.value = res.data.content || res.data.records || []
      pagination.total = res.data.totalElements || res.data.total || 0
      lastUpdated.value = new Date().toISOString()
    }
  }).finally(() => {
    loading.value = false
  })
}

function fetchStatistics() {
  fixedAssetApi.getStatistics().then((res: any) => {
    if (res.data) {
      statistics.value = res.data
    }
  })
}

function fetchCategories() {
  fixedAssetCategoryApi.getList().then((res: any) => {
    if (res.data) {
      categories.value = res.data
    }
  })
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    id: 0,
    assetCode: '',
    assetName: '',
    categoryId: undefined,
    categoryName: '',
    purchaseDate: undefined,
    originalValue: undefined,
    netValue: undefined,
    depreciationMethod: 'straight_line',
    usefulLife: undefined,
    salvageValue: undefined,
    salvageRate: undefined,
    monthlyDepreciation: undefined,
    accumulatedDepreciation: undefined,
    status: 'draft',
    location: '',
    departmentId: '',
    departmentName: '',
    custodianId: '',
    custodianName: '',
    specification: '',
    brand: '',
    supplierName: '',
    invoiceNo: '',
    warrantyEndDate: undefined,
    description: '',
    useStatus: 'in_use',
    assetPhoto: '',
    remark: '',
  })
  modalVisible.value = true
}

function editAsset(record: FixedAssetRecord) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function viewDetail(record: FixedAssetRecord) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? fixedAssetApi.update(editId.value, formData)
    : fixedAssetApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleDelete(id: number) {
  fixedAssetApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '删除失败')
  })
}

function handleDeleteWithConfirm(record: FixedAssetRecord) {
  handleDelete(record.id)
}

function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的资产')
    return
  }
  message.success('批量删除成功')
  fetchData()
}

function handleDepreciate(record: FixedAssetRecord) {
  fixedAssetApi.depreciate(record.id).then(() => {
    message.success('折旧计提成功')
    fetchData()
  }).catch((err: any) => {
    message.error(err.message || '折旧失败')
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: FixedAssetRecord) {
  switch (key) {
    case 'depreciate':
      handleDepreciate(record)
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: '删除后数据不可恢复，确定要删除该资产吗？',
        okType: 'danger',
        onOk: () => handleDelete(record.id)
      })
      break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.asset-list :deep(.ant-statistic) {
  text-align: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-more-btn {
  border: none; box-shadow: none; padding: 4px 8px;
}
.action-cell-inner {
  display: inline-flex; align-items: center;
}
</style>
