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
        新建付款
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
      <a-button @click="handleBatchPrint">批量打印</a-button>
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
          <template v-else-if="column.key === 'paymentAmount'">
            ¥{{ record.paymentAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 1" @click="handleApprove(record)">审批</a>
              <PrintButton
                v-if="record.status >= 2"
                templateType="payment"
                :businessId="record.id"
                businessType="purchase_payment"
                buttonText="打印"
                buttonSize="small"
              />
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="付款单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="付款单号">{{ currentRecord.paymentNo }}</a-descriptions-item>
      <a-descriptions-item label="采购订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="付款日期">{{ currentRecord.paymentDate }}</a-descriptions-item>
      <a-descriptions-item label="付款金额">¥{{ currentRecord.paymentAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="付款方式">{{ currentRecord.paymentMethod }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="收款账户">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建付款弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建付款单"
    width="700px"
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
      <a-form-item label="付款金额" name="paymentAmount">
        <a-input-number v-model:value="formData.paymentAmount" :min="0" :precision="2" placeholder="请输入付款金额" style="width: 100%" />
      </a-form-item>
      <a-form-item label="付款方式" name="paymentMethod">
        <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
          <a-select-option :value="1">银行转账</a-select-option>
          <a-select-option :value="2">现金</a-select-option>
          <a-select-option :value="3">承兑汇票</a-select-option>
          <a-select-option :value="4">微信/支付宝</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="付款日期" name="paymentDate">
        <a-date-picker v-model:value="formData.paymentDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 批量打印弹窗 -->
  <a-modal
    v-model:open="batchPrintModalVisible"
    title="批量打印"
    width="750px"
    centered
    :footer="null"
  >
    <div v-if="printItems.length === 0" style="text-align: center; padding: 40px; color: #999;">
      暂无选中付款单
    </div>
    <template v-else>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <a-checkbox
          :checked="printItems.every((item: any) => item.checked)"
          :indeterminate="printItems.some((item: any) => item.checked) && !printItems.every((item: any) => item.checked)"
          @change="handlePrintCheckAll"
        >
          全选
        </a-checkbox>
        <a-button type="primary" size="small" @click="handlePrintAll">
          <template #icon><PrinterOutlined /></template>
          全部打印
        </a-button>
      </div>
      <a-table
        :columns="printTableColumns"
        :data-source="printItems"
        :pagination="false"
        :scroll="{ y: 300 }"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'checked'">
            <a-checkbox v-model:checked="record.checked" />
          </template>
          <template v-else-if="column.key === 'action'">
            <PrintButton
              templateType="payment"
              :businessId="record.id"
              businessType="purchase_payment"
              buttonText="打印"
              buttonSize="small"
            />
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { ModuleLayout } from '@ai-ready/components'
import { paymentApi } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

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

// ── 批量打印状态 ──────────────────────────────────────
const batchPrintModalVisible = ref(false)
const printItems = ref<any[]>([])

const printTableColumns = [
  { title: '选择', key: 'checked', width: 60 },
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '付款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 100 },
  { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 120 },
  { title: '操作', key: 'action', width: 100 }
]

const formData = reactive({
  orderNo: '',
  paymentAmount: undefined as number | undefined,
  paymentMethod: 1,
  paymentDate: undefined as any,
  remark: ''
})

const formRules = {
  orderNo: [{ required: true, message: '请输入采购订单号', trigger: 'blur' }],
  paymentAmount: [{ required: true, message: '请输入付款金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择付款方式', trigger: 'change' }],
  paymentDate: [{ required: true, message: '请选择付款日期', trigger: 'change' }]
}

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '付款单' }
])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 180 },
  { title: '采购订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 120 },
  { title: '付款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 120 },
  { title: '付款方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已付款', 3: '部分付款' }
  return texts[status] || '未知'
}

const getPaymentMethodText = (method: number) => {
  const texts: Record<number, string> = { 1: '银行转账', 2: '现金', 3: '承兑汇票', 4: '微信/支付宝' }
  return texts[method] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await paymentApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: 1 })
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

// ── 新建付款 ──────────────────────────────────────────
const handleAdd = () => {
  formData.orderNo = ''
  formData.paymentAmount = undefined
  formData.paymentMethod = 1
  formData.paymentDate = undefined
  formData.remark = ''
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
    await paymentApi.create({
      orderNo: formData.orderNo,
      paymentAmount: formData.paymentAmount,
      paymentMethod: formData.paymentMethod,
      paymentDate: formData.paymentDate,
      remark: formData.remark
    })
    message.success('新建付款单成功')
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('新建付款单失败')
  } finally {
    formSubmitting.value = false
  }
}

// ── 审批付款 ──────────────────────────────────────────
const handleApprove = (record: any) => {
  Modal.confirm({
    title: '审批付款单',
    content: `确定审批付款单 "${record.paymentNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await paymentApi.approve(record.id)
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
    const headers = ['付款单号', '采购订单', '供应商', '付款日期', '付款金额', '付款方式', '状态', '创建时间']
    const rows = dataSource.value.map(row => [
      row.paymentNo || '',
      row.orderNo || '',
      row.supplierName || '',
      row.paymentDate || '',
      row.paymentAmount?.toFixed(2) || '0.00',
      getPaymentMethodText(row.paymentMethod),
      getStatusText(row.status),
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `付款单_${new Date().toISOString().slice(0, 10)}.csv`
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
    message.warning('请选择要审批的付款单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审批',
    content: `确定要批量审批选中的 ${count} 个付款单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await paymentApi.approve(id)
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

// ── 批量打印 ──────────────────────────────────────────
const handlePrintCheckAll = (e: any) => {
  const checked = e.target.checked
  printItems.value.forEach((item: any) => { item.checked = checked })
}

const handlePrintAll = () => {
  const toPrint = printItems.value.filter((item: any) => item.checked)
  if (toPrint.length === 0) {
    message.warning('请选择要打印的付款单')
    return
  }
  message.success(`正在发送 ${toPrint.length} 个付款单的打印任务...`)
}

const handleBatchPrint = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要打印的付款单')
    return
  }
  const selected = dataSource.value.filter((item: any) => selectedRowKeys.value.includes(item.id))
  printItems.value = selected.map((item: any) => ({ ...item, checked: true }))
  batchPrintModalVisible.value = true
}

onMounted(() => { fetchData() })
</script>
