<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-inbound-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建入库"
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
      <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
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

  <a-modal v-model:open="detailVisible" title="入库单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="入库单号">{{ currentRecord.inboundNo }}</a-descriptions-item>
      <a-descriptions-item label="入库类型">{{ currentRecord.inboundType }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="入库日期">{{ currentRecord.inboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="来源单号">{{ currentRecord.sourceNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <!-- 新建入库弹窗 -->
  <a-modal v-model:open="addVisible" title="新建入库单" width="800px" :confirm-loading="addSubmitting"
    @ok="handleAddSubmit" @cancel="handleAddCancel">
    <a-form ref="addFormRef" :model="addForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="addFormRules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="采购订单" name="orderNo">
            <a-select v-model:value="addForm.orderNo" show-search placeholder="请选择采购订单"
              :options="purchaseOrderOptions" :filter-option="filterOption" allow-clear @change="handleOrderChange" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="入库类型" name="inboundType">
            <a-select v-model:value="addForm.inboundType" placeholder="请选择入库类型" :options="inboundTypeOptions" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="入库仓库" name="warehouseId">
            <a-select v-model:value="addForm.warehouseId" placeholder="请选择仓库" :options="warehouseOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="预计到货日期" name="expectedDate">
            <a-date-picker v-model:value="addForm.expectedDate" style="width: 100%" placeholder="请选择预计到货日期" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
    <a-divider style="margin: 12px 0">入库明细</a-divider>
    <div style="margin-bottom: 12px">
      <a-button type="dashed" size="small" @click="addInboundItem"><template #icon><PlusOutlined /></template>添加明细</a-button>
    </div>
    <a-table :columns="addItemColumns" :data-source="addForm.items" :pagination="false" size="small" row-key="key" :scroll="{ y: 250 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'isNew'"><a-tag v-if="record.isNew" color="green">新增</a-tag><span v-else>-</span></template>
        <template v-if="column.key === 'productName'">
          <a-select v-model:value="addForm.items[index].productId" show-search placeholder="选择产品"
            :options="productOptions" style="width: 100%" size="small"
            @change="(val: number) => handleItemProductChange(index, val)" />
        </template>
        <template v-else-if="column.key === 'expectedQty'">
          <a-input-number v-model:value="addForm.items[index].expectedQty" :min="1" style="width: 100%" size="small" />
        </template>
        <template v-else-if="column.key === 'actualQty'">
          <a-input-number v-model:value="addForm.items[index].actualQty"
            :min="record.isNew ? 1 : 0" style="width: 100%" size="small" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="removeInboundItem(index)">删除</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, EyeOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { inboundApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 160, sortable: true },
  { title: '入库类型', dataIndex: 'inboundType', key: 'inboundType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'inboundNo', label: '入库单号', type: 'input' as const, placeholder: '输入入库单号' },
  { key: 'orderNo', label: '采购订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已入库', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已入库' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 新建入库 ──
interface AddInboundItem { key: number; productId: number | undefined; productName: string; isNew: boolean; expectedQty: number; actualQty: number }
let itemKeyCounter = 0
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ orderNo: undefined as string | undefined, inboundType: 1, warehouseId: undefined as number | undefined, expectedDate: dayjs(), remark: '', items: [] as AddInboundItem[] })
const addFormRules = { warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }], expectedDate: [{ required: true, message: '请选择预计到货日期', trigger: 'change' }] }
const inboundTypeOptions = [
  { value: 1, label: '采购入库' }, { value: 2, label: '退货入库' },
  { value: 3, label: '调拨入库' }, { value: 4, label: '其他入库' }
]
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
const purchaseOrderOptions = [
  { value: 'PO-2024-001', label: 'PO-2024-001 / 供应商A' },
  { value: 'PO-2024-002', label: 'PO-2024-002 / 供应商B' },
  { value: 'PO-2024-003', label: 'PO-2024-003 / 供应商C' }
]
const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' }, { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' }, { value: 4, label: 'PROD-004 包装箱(大)' }
]
const addItemColumns = [
  { title: '来源', key: 'isNew', width: 60 }, { title: '产品名称', key: 'productName' },
  { title: '预计数量', key: 'expectedQty', width: 100 }, { title: '实收数量', key: 'actualQty', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const filterOption = (input: string, option: any) => option.label.toLowerCase().includes(input.toLowerCase())

function handleOrderChange(value: string | undefined) {
  if (value) {
    addForm.items = [
      { key: itemKeyCounter++, productId: 1, productName: 'PROD-001', isNew: false, expectedQty: 100, actualQty: 0 },
      { key: itemKeyCounter++, productId: 2, productName: 'PROD-002', isNew: false, expectedQty: 50, actualQty: 0 }
    ]
  }
}
function handleItemProductChange(index: number, productId: number) {
  const product = productOptions.find(p => p.value === productId)
  if (product) addForm.items[index].productName = product.label
}
function addInboundItem() {
  addForm.items.push({ key: itemKeyCounter++, productId: undefined, productName: '', isNew: true, expectedQty: 1, actualQty: 0 })
}
function removeInboundItem(index: number) { addForm.items.splice(index, 1) }

async function fetchData() {
  loading.value = true; error.value = null
  try {
    const res = await inboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    dataSource.value = (res as any).records || []; pagination.total = (res as any).total || 0
  } catch { error.value = '获取数据失败' }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  addForm.orderNo = undefined; addForm.inboundType = 1; addForm.warehouseId = undefined
  addForm.expectedDate = dayjs(); addForm.remark = ''; addForm.items = []; itemKeyCounter = 0; addVisible.value = true
}

async function handleDelete(record: any) {
  try { await inboundApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleBatchDelete(ids: number[]) {
  let successCount = 0; let failCount = 0
  for (const id of ids) { try { await inboundApi.delete(id); successCount++ } catch { failCount++ } }
  if (failCount === 0) { message.success(`批量删除完成，成功 ${successCount} 个`) }
  else { message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`) }
  fetchData()
}

const handleAddSubmit = async () => {
  try { await addFormRef.value?.validate() } catch { return }
  if (addForm.items.length === 0) { message.warning('请至少添加一条入库明细'); return }
  addSubmitting.value = true
  try {
    await inboundApi.create({
      orderNo: addForm.orderNo, inboundType: addForm.inboundType, warehouseId: addForm.warehouseId,
      expectedDate: addForm.expectedDate.format('YYYY-MM-DD'), remark: addForm.remark,
      items: addForm.items.map(item => ({ productId: item.productId, expectedQty: item.expectedQty, actualQty: item.actualQty }))
    })
    message.success('入库单创建成功'); addVisible.value = false; fetchData()
  } catch (err: any) { message.error(err?.message || '创建失败') }
  finally { addSubmitting.value = false }
}
const handleAddCancel = () => { addVisible.value = false }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批', content: `确定要审批通过入库单 "${record.inboundNo}" 吗？`,
    okText: '确认通过', cancelText: '取消', centered: true,
    async onOk() { try { await inboundApi.approve(record.id); message.success('审批成功'); fetchData() } catch (err: any) { message.error(err?.message || '审批失败') } }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择入库单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      let success = 0; let fail = 0
      for (const id of keys) { try { await inboundApi.approve(id); success++ } catch { fail++ } }
      if (fail === 0) { message.success(`批量审批完成，成功 ${success} 个`) }
      else { message.warning(`审批完成: 成功 ${success} 个, 失败 ${fail} 个`) }
      fetchData()
    }
  })
}

function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['入库单号', '入库类型', '仓库', '入库日期', '状态', '创建时间']
    const rows = dataSource.value.map((row: any) => [
      row.inboundNo || '', row.inboundType || '', row.warehouseName || '', row.inboundDate || '',
      getStatusText(row.status), row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'; const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob); const link = document.createElement('a')
    link.href = url; link.download = `入库单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(url); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
