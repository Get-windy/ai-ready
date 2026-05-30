<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建退货
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 1" @click="handleApprove(record)">审批</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="退货单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
      <a-descriptions-item label="采购订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
      <a-descriptions-item label="退货金额">¥{{ currentRecord.totalAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="备注">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建退货弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建退货单"
    width="750px"
    centered
    :confirm-loading="formSubmitting"
    ok-text="确认创建"
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
      <a-form-item label="退货原因" name="reason">
        <a-select v-model:value="formData.reason" placeholder="请选择退货原因">
          <a-select-option value="质量问题">质量问题</a-select-option>
          <a-select-option value="规格不符">规格不符</a-select-option>
          <a-select-option value="数量错误">数量错误</a-select-option>
          <a-select-option value="延迟交货">延迟交货</a-select-option>
          <a-select-option value="其他">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="退款方式" name="refundType">
        <a-radio-group v-model:value="formData.refundType">
          <a-radio :value="1">原路退回</a-radio>
          <a-radio :value="2">抵扣货款</a-radio>
          <a-radio :value="3">线下退款</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="退货日期" name="returnDate">
        <a-date-picker v-model:value="formData.returnDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
    <div style="margin-top: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <span style="font-weight: 500;">退货物料明细</span>
        <a-button size="small" type="dashed" @click="handleAddReturnItem">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </div>
      <a-table
        :columns="returnItemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveReturnItem(index)">删除</a-button>
          </template>
        </template>
      </a-table>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { ModuleLayout } from '@ai-ready/components'
import { purchaseReturnApi } from '@/api/erp'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

let itemCounter = 0
const genTempKey = () => `return_item_${++itemCounter}_${Date.now()}`

interface ReturnItemForm {
  tempKey: string
  productName: string
  quantity: number
  unitPrice: number
}

const formData = reactive({
  orderNo: '',
  reason: undefined as string | undefined,
  refundType: 1,
  returnDate: undefined as any,
  remark: '',
  items: [] as ReturnItemForm[]
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const returnItemColumns = [
  { title: '物料名称', key: 'productName', width: 150 },
  { title: '退货数量', key: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const handleAddReturnItem = () => {
  formData.items.push({
    tempKey: genTempKey(),
    productName: '',
    quantity: 1,
    unitPrice: 0
  })
}

const handleRemoveReturnItem = (index: number) => {
  formData.items.splice(index, 1)
}

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '退货单' }
])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 180 },
  { title: '采购订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '退货日期', dataIndex: 'returnDate', key: 'returnDate', width: 120 },
  { title: '退货金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已退货', 3: '已拒绝' }
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await purchaseReturnApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: 1 })
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch {
    error.value = '获取数据失败'
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => { currentView.value = view }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }

// ── 新建退货 ──────────────────────────────────────────
const handleAdd = () => {
  formData.orderNo = ''
  formData.reason = undefined
  formData.refundType = 1
  formData.returnDate = undefined
  formData.remark = ''
  formData.items = []
  formModalVisible.value = true
}

// ── 查看详情 ──────────────────────────────────────────
const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

// ── 表单提交 ──────────────────────────────────────────
const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  formSubmitting.value = true
  try {
    await purchaseReturnApi.create({
      orderNo: formData.orderNo,
      reason: formData.reason,
      refundType: formData.refundType,
      returnDate: formData.returnDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      }))
    })
    message.success('新建退货单成功')
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('新建退货单失败')
  } finally {
    formSubmitting.value = false
  }
}

// ── 审批退货 ──────────────────────────────────────────
const handleApprove = (record: any) => {
  Modal.confirm({
    title: '审批退货单',
    content: `确定审批退货单 "${record.returnNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await purchaseReturnApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch { message.error('审批失败') }
    }
  })
}

// ── 导出功能 ──────────────────────────────────────────
const handleExport = () => {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['退货单号', '采购订单', '供应商', '退货日期', '退货金额', '状态', '创建时间']
    const rows = dataSource.value.map(row => [
      row.returnNo || '',
      row.orderNo || '',
      row.supplierName || '',
      row.returnDate || '',
      row.totalAmount?.toFixed(2) || '0.00',
      getStatusText(row.status),
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `退货单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    hideLoading()
    message.success('导出成功，文件下载中')
  } catch {
    hideLoading()
    message.error('导出失败')
  }
}

// ── 批量审批 ──────────────────────────────────────────
const handleBatchApprove = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要审批的退货单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审批',
    content: `确定要批量审批选中的 ${count} 个退货单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await purchaseReturnApi.approve(id)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量审批完成，成功 ${successCount} 个`)
      } else {
        message.warning(`审批完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
      }
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.danger { color: #ff4d4f; }
</style>
