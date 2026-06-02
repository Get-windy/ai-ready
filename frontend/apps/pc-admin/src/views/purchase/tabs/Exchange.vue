<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'purchase-exchange-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建换货"
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
        <a-tooltip v-if="record.status === 0" title="编辑">
          <a-button type="link" size="small" @click="handleEdit(record)">
            <template #icon><EditOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip v-if="record.status === 0" title="提交">
          <a-button type="link" size="small" @click="handleSubmit(record)">
            <template #icon><SendOutlined /></template>
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

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="换货单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="换货单号">{{ currentRecord.exchangeNo }}</a-descriptions-item>
      <a-descriptions-item label="关联订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="换货日期">{{ currentRecord.exchangeDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建人">{{ currentRecord.creatorName }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="换货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建/编辑换货弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    :title="formMode === 'edit' ? '编辑换货单' : '新建换货单'"
    width="750px"
    centered
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
      <a-form-item label="采购订单号" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入采购订单号" />
      </a-form-item>
      <a-form-item label="换货原因" name="reason">
        <a-select v-model:value="formData.reason" placeholder="请选择换货原因">
          <a-select-option value="质量问题">质量问题</a-select-option>
          <a-select-option value="规格不符">规格不符</a-select-option>
          <a-select-option value="数量错误">数量错误</a-select-option>
          <a-select-option value="其他">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="换货日期" name="exchangeDate">
        <a-date-picker v-model:value="formData.exchangeDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
    <div style="margin-top: 16px;">
      <div style="font-weight: 500; margin-bottom: 8px;">换货物料明细</div>
      <a-table
        :columns="exchangeItemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'outProductName'">
            <a-input v-model:value="record.outProductName" placeholder="换出物料" size="small" />
          </template>
          <template v-else-if="column.key === 'inProductName'">
            <a-input v-model:value="record.inProductName" placeholder="换入物料" size="small" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveExchangeItem(index)">删除</a-button>
          </template>
        </template>
      </a-table>
      <a-button size="small" type="dashed" @click="handleAddExchangeItem" style="margin-top: 8px;">
        <template #icon><PlusOutlined /></template>
        添加换货物料
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, EyeOutlined, EditOutlined, DeleteOutlined, SendOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import TableList from '@/components/TableList/TableList.vue'
import { purchaseExchangeApi } from '@/api/purchase-exchange'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tableRef = ref()
const loading = ref(false)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '换货单号', dataIndex: 'exchangeNo', key: 'exchangeNo', width: 160, sortable: true },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 140 },
  { title: '换货日期', dataIndex: 'exchangeDate', key: 'exchangeDate', width: 110, type: 'date' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'exchangeNo', label: '换货单号', type: 'input' as const, placeholder: '输入换货单号' },
  { key: 'orderNo', label: '关联订单', type: 'input' as const, placeholder: '输入订单号' },
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '输入供应商' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已审批', value: 2 },
    { label: '换货中', value: 3 }, { label: '完成', value: 4 }, { label: '已取消', value: 5 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '换货中', 4: '完成', 5: '已取消' }

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [
    { label: '本页数量', value: dataSource.value.length, type: 'default' as const }
  ]
})

function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

// ── 详情弹窗 ────────────────────────────────────────────
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

let itemCounter = 0
const genTempKey = () => `exchange_item_${++itemCounter}_${Date.now()}`

interface ExchangeItemForm {
  tempKey: string
  outProductName: string
  inProductName: string
  quantity: number
}

const formData = reactive({
  orderNo: '',
  reason: undefined as string | undefined,
  exchangeDate: undefined as any,
  remark: '',
  items: [] as ExchangeItemForm[]
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择换货原因', trigger: 'change' }],
  exchangeDate: [{ required: true, message: '请选择换货日期', trigger: 'change' }]
}

const exchangeItemColumns = [
  { title: '换出物料', key: 'outProductName', width: 150 },
  { title: '换入物料', key: 'inProductName', width: 150 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const handleAddExchangeItem = () => {
  formData.items.push({ tempKey: genTempKey(), outProductName: '', inProductName: '', quantity: 1 })
}
const handleRemoveExchangeItem = (index: number) => { formData.items.splice(index, 1) }

const resetForm = () => {
  formData.orderNo = ''; formData.reason = undefined; formData.exchangeDate = undefined
  formData.remark = ''; formData.items = []; editingId.value = null
}

async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseExchangeApi.page({ current: pagination.current, size: pagination.pageSize, tenantId: userStore.tenantId, ...searchFilters })
    dataSource.value = (res as any).data?.records || (res as any).records || []
    pagination.total = (res as any).data?.total || (res as any).total || 0
  } catch { message.error('获取数据失败') }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }

function handleAdd() { formMode.value = 'add'; resetForm(); formModalVisible.value = true }

function handleEdit(record: any) {
  formMode.value = 'edit'; editingId.value = record.id
  formData.orderNo = record.orderNo || ''; formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate; formData.remark = record.remark || ''
  formData.items = (record.items || []).map((item: any) => ({
    tempKey: genTempKey(), outProductName: item.outProductName || '', inProductName: item.inProductName || '', quantity: item.quantity || 1
  }))
  formModalVisible.value = true
}

async function handleDelete(record: any) {
  try { await purchaseExchangeApi.delete(record.id); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}

async function handleBatchDelete(ids: number[]) {
  let successCount = 0; let failCount = 0
  for (const id of ids) {
    try { await purchaseExchangeApi.delete(id); successCount++ } catch { failCount++ }
  }
  if (failCount === 0) { message.success(`批量删除完成，成功 ${successCount} 个`) }
  else { message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`) }
  fetchData()
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    const data = {
      orderNo: formData.orderNo, reason: formData.reason, exchangeDate: formData.exchangeDate,
      remark: formData.remark,
      items: formData.items.map(item => ({ outProductName: item.outProductName, inProductName: item.inProductName, quantity: item.quantity }))
    }
    if (formMode.value === 'edit' && editingId.value) {
      await purchaseExchangeApi.update(editingId.value, data as any)
      message.success('编辑成功')
    } else {
      await purchaseExchangeApi.create(data as any)
      message.success('新建换货单成功')
    }
    formModalVisible.value = false; fetchData()
  } catch { message.error(formMode.value === 'edit' ? '编辑失败' : '新建失败') }
  finally { formSubmitting.value = false }
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交换货单', content: `提交换货单 "${record.exchangeNo}" ？`, okText: '确认提交', centered: true,
    async onOk() { try { await purchaseExchangeApi.submit(record.id); message.success('提交成功'); fetchData() } catch { message.error('提交失败') } }
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批换货单', content: `审批换货单 "${record.exchangeNo}" ？`, okText: '确认审批', centered: true,
    async onOk() { try { await purchaseExchangeApi.approve(record.id, { approved: true }); message.success('审批成功'); fetchData() } catch { message.error('审批失败') } }
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择换货单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      let success = 0; let fail = 0
      for (const id of keys) {
        try { await purchaseExchangeApi.approve(id, { approved: true }); success++ } catch { fail++ }
      }
      if (fail === 0) { message.success(`批量审批完成，成功 ${success} 个`) }
      else { message.warning(`审批完成: 成功 ${success} 个, 失败 ${fail} 个`) }
      fetchData()
    }
  })
}

function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['换货单号', '关联订单', '供应商', '换货日期', '状态', '创建人', '创建时间']
    const rows = dataSource.value.map((row: any) => [
      row.exchangeNo || '', row.orderNo || '', row.supplierName || '', row.exchangeDate || '',
      getStatusText(row.status), row.creatorName || '', row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'; const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob); const link = document.createElement('a')
    link.href = url; link.download = `换货单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(url); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
