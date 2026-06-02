<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'sale-outbound-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建出库"
    @add="handleAdd"
    @view="handleView"
    @delete="handleDelete"
    @batch-delete="handleBatchDelete"
    @refresh="fetchData"
    @search="handleSearch"
    @page-change="handlePageChange"
    @sort-change="handleSortChange"
    @filter-change="handleFilterChange"
  >
    <template #toolbar-actions>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button size="small" @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #status="{ record }">
      <a-tag :color="getStatusColor(record.status)">
        {{ getStatusText(record.status) }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 1" title="审批">
          <a-button type="link" size="small" @click="handleApprove(record)">
            <template #icon><CheckCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

  <a-modal v-model:open="detailVisible" title="出库单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="出库单号">{{ currentRecord.outboundNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="出库日期">{{ currentRecord.outboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="物流单号">{{ currentRecord.trackingNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <a-modal v-model:open="formModalVisible" title="新建出库单" width="800px" centered
    :confirm-loading="formSubmitting" ok-text="确认创建" cancel-text="取消"
    @ok="handleFormSubmit" @cancel="formModalVisible = false">
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }">
      <a-form-item label="销售订单" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" />
      </a-form-item>
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="仓库" name="warehouseName" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.warehouseName" placeholder="请输入仓库名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="物流单号" name="trackingNo" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.trackingNo" placeholder="请输入物流单号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row>
        <a-col :span="12">
          <a-form-item label="出库日期" name="outboundDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.outboundDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="承运商" name="carrier" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-input v-model:value="formData.carrier" placeholder="请输入承运商" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="出库明细" required>
        <div style="margin-bottom: 8px">
          <a-button type="dashed" size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>添加产品
          </a-button>
        </div>
        <a-table :data-source="formData.items" :pagination="false" row-key="key" size="small" bordered :columns="itemColumns">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              <a-input v-model:value="record.productName" placeholder="产品名称" size="small" />
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'unit'">
              <a-input v-model:value="record.unit" placeholder="单位" size="small" />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formData.items.length <= 1">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { outboundApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 160, sortable: true },
  { title: '销售订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'outboundNo', label: '出库单号', type: 'input' as const, placeholder: '输入出库单号' },
  { key: 'orderNo', label: '销售订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '输入客户' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已出库', value: 2 }, { label: '部分出库', value: 3 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已出库', 3: '部分出库' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单 ──
interface OutboundItem { key: number; productName: string; quantity: number; unit: string }
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({ orderNo: '', customerName: '', warehouseName: '', outboundDate: undefined as any, trackingNo: '', carrier: '', items: [] as OutboundItem[], remark: '' })
const defaultItem = (): OutboundItem => ({ key: Date.now() + Math.random(), productName: '', quantity: 1, unit: '' })
const formRules = { orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }], customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }], warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }], outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }] }
const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 100 },
  { title: '单位', key: 'unit', dataIndex: 'unit', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]
const addItem = () => { formData.items.push(defaultItem()) }
const removeItem = (index: number) => { if (formData.items.length > 1) formData.items.splice(index, 1) }

async function fetchData() {
  loading.value = true; error.value = null
  try {
    const res = await outboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    dataSource.value = (res as any).records || []; pagination.total = (res as any).total || 0
  } catch { error.value = '获取数据失败' }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  formData.orderNo = ''; formData.customerName = ''; formData.warehouseName = ''
  formData.outboundDate = undefined; formData.trackingNo = ''; formData.carrier = ''
  formData.items = [defaultItem()]; formData.remark = ''; formModalVisible.value = true
}

async function handleDelete(record: any) {
  try { await outboundApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleBatchDelete(ids: number[]) {
  let successCount = 0; let failCount = 0
  for (const id of ids) { try { await outboundApi.delete(id); successCount++ } catch { failCount++ } }
  if (failCount === 0) { message.success(`批量删除完成，成功 ${successCount} 个`) }
  else { message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`) }
  fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    await outboundApi.create({
      orderNo: formData.orderNo, customerName: formData.customerName,
      warehouseName: formData.warehouseName, outboundDate: formData.outboundDate,
      trackingNo: formData.trackingNo, carrier: formData.carrier, remark: formData.remark,
      items: formData.items.map(item => ({ productName: item.productName, quantity: item.quantity, unit: item.unit }))
    })
    message.success('新建出库单成功'); formModalVisible.value = false; fetchData()
  } catch { message.error('新建出库单失败') }
  finally { formSubmitting.value = false }
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批出库单', content: `审批出库单 "${record.outboundNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await outboundApi.approve(record.id); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择出库单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      let success = 0; let fail = 0
      for (const id of keys) { try { await outboundApi.approve(id); success++ } catch { fail++ } }
      if (fail === 0) { message.success(`批量审批完成，成功 ${success} 个`) }
      else { message.warning(`审批完成: 成功 ${success} 个, 失败 ${fail} 个`) }
      fetchData()
    }
  })
}

function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['出库单号', '销售订单', '客户', '出库日期', '状态', '创建时间']
    const rows = dataSource.value.map((row: any) => [
      row.outboundNo || '', row.orderNo || '', row.customerName || '', row.outboundDate || '',
      getStatusText(row.status), row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'; const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob); const link = document.createElement('a')
    link.href = url; link.download = `出库单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(url); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
