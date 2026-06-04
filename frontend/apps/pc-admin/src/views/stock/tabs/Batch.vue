<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-batch-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
    :show-export="true"
    @export="handleExport"
  >

    <template #status="{ record }">
      <a-tag :color="getBatchStatusColor(record.status)">{{ getBatchStatusText(record.status) }}</a-tag>
    </template>

    <template #expiryDate="{ record }">
      <a-tag :color="getExpiryColor(record.expiryDate)">{{ record.expiryDate }}</a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="detailVisible" title="批次详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="批次号">{{ currentRecord.batchNo }}</a-descriptions-item>
      <a-descriptions-item label="产品编码">{{ currentRecord.productCode }}</a-descriptions-item>
      <a-descriptions-item label="产品名称">{{ currentRecord.productName }}</a-descriptions-item>
      <a-descriptions-item label="生产日期">{{ currentRecord.productionDate }}</a-descriptions-item>
      <a-descriptions-item label="有效期"><a-tag :color="getExpiryColor(currentRecord.expiryDate)">{{ currentRecord.expiryDate }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getBatchStatusColor(currentRecord.status)">{{ getBatchStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="库存数量">{{ currentRecord.quantity || '-' }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { EyeOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { batchApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'

const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 180, sortable: true },
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 160 },
  { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', width: 110, type: 'date' as const },
  { title: '有效期', dataIndex: 'expiryDate', key: 'expiryDate', width: 110, type: 'date' as const, slotName: 'expiryDate' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'batchNo', label: '批次号', type: 'input' as const, placeholder: '输入批次号' },
  { key: 'productCode', label: '产品编码', type: 'input' as const, placeholder: '输入产品编码' },
  { key: 'productName', label: '产品名称', type: 'input' as const, placeholder: '输入产品名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待检', value: 0 }, { label: '合格', value: 1 }, { label: '临期', value: 2 }, { label: '过期', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})

function getBatchStatusColor(status: number): string {
  const colors: Record<number, string> = { 0: 'default', 1: 'green', 2: 'orange', 3: 'red' }
  return colors[status] || 'default'
}
function getBatchStatusText(status: number): string {
  const texts: Record<number, string> = { 0: '待检', 1: '合格', 2: '临期', 3: '过期' }
  return texts[status] || '未知'
}
function getExpiryColor(expiryDate: string): string {
  if (!expiryDate) return 'default'
  const today = new Date()
  const expiry = new Date(expiryDate)
  const days = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'red'
  if (days < 30) return 'orange'
  return 'green'
}

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await batchApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    const pageData = (res as any).data ?? res
    dataSource.value = pageData?.records || []; pagination.total = pageData?.totalElements ?? pageData?.total ?? 0
  } catch { /* 获取数据失败 */ }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleExport() {
  const headers = ['批次号', '产品编码', '产品名称', '生产日期', '有效期', '状态', '创建时间']
  const rows = dataSource.value.map((row: any) => [
    row.batchNo || '', row.productCode || '', row.productName || '', row.productionDate || '',
    row.expiryDate || '', getBatchStatusText(row.status), row.createTime || ''
  ])
  exportCsv(headers, rows, '批次')
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
