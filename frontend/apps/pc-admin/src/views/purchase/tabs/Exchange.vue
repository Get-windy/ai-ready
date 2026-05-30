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
        新建换货
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
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSubmit(record)">提交</a>
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
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { ModuleLayout } from '@ai-ready/components'
import { purchaseExchangeApi } from '@/api/purchase-exchange'

const loading = ref(false)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
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
  formData.items.push({
    tempKey: genTempKey(),
    outProductName: '',
    inProductName: '',
    quantity: 1
  })
}

const handleRemoveExchangeItem = (index: number) => {
  formData.items.splice(index, 1)
}

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '换货单' }
])

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const columns = [
  { title: '换货单号', dataIndex: 'exchangeNo', key: 'exchangeNo', width: 180 },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '换货日期', dataIndex: 'exchangeDate', key: 'exchangeDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => { selectedRowKeys.value = keys }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '换货中', 4: '完成', 5: '已取消' }
  return texts[status] || '未知'
}

const resetForm = () => {
  formData.orderNo = ''
  formData.reason = undefined
  formData.exchangeDate = undefined
  formData.remark = ''
  formData.items = []
  editingId.value = null
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await purchaseExchangeApi.page({ current: pagination.current, size: pagination.pageSize, tenantId: 1 })
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => { currentView.value = view }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }

// ── 新建换货 ──────────────────────────────────────────
const handleAdd = () => {
  formMode.value = 'add'
  resetForm()
  formModalVisible.value = true
}

// ── 查看详情 ──────────────────────────────────────────
const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

// ── 编辑换货 ──────────────────────────────────────────
const handleEdit = (record: any) => {
  formMode.value = 'edit'
  editingId.value = record.id
  formData.orderNo = record.orderNo || ''
  formData.reason = record.reason || undefined
  formData.exchangeDate = record.exchangeDate
  formData.remark = record.remark || ''
  formData.items = (record.items || []).map((item: any) => ({
    tempKey: genTempKey(),
    outProductName: item.outProductName || '',
    inProductName: item.inProductName || '',
    quantity: item.quantity || 1
  }))
  formModalVisible.value = true
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
    const data = {
      orderNo: formData.orderNo,
      reason: formData.reason,
      exchangeDate: formData.exchangeDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        outProductName: item.outProductName,
        inProductName: item.inProductName,
        quantity: item.quantity
      }))
    }
    if (formMode.value === 'edit' && editingId.value) {
      await purchaseExchangeApi.update(editingId.value, data as any)
      message.success('编辑成功')
    } else {
      await purchaseExchangeApi.create(data as any)
      message.success('新建换货单成功')
    }
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error(formMode.value === 'edit' ? '编辑失败' : '新建失败')
  } finally {
    formSubmitting.value = false
  }
}

// ── 提交换货 ──────────────────────────────────────────
const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '提交换货单',
    content: `确定提交换货单 "${record.exchangeNo}" 吗？`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await purchaseExchangeApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch { message.error('提交失败') }
    }
  })
}

// ── 审批换货 ──────────────────────────────────────────
const handleApprove = (record: any) => {
  Modal.confirm({
    title: '审批换货单',
    content: `确定审批换货单 "${record.exchangeNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await purchaseExchangeApi.approve(record.id, { approved: true })
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
    const headers = ['换货单号', '关联订单', '供应商', '换货日期', '状态', '创建人', '创建时间']
    const rows = dataSource.value.map(row => [
      row.exchangeNo || '',
      row.orderNo || '',
      row.supplierName || '',
      row.exchangeDate || '',
      getStatusText(row.status),
      row.creatorName || '',
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `换货单_${new Date().toISOString().slice(0, 10)}.csv`
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
    message.warning('请选择要审批的换货单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审批',
    content: `确定要批量审批选中的 ${count} 个换货单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await purchaseExchangeApi.approve(id, { approved: true })
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
