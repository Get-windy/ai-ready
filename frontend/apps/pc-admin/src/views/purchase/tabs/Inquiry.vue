<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
    empty-text="暂无询价单"
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建询价
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchSend">批量发送</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
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
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSend(record)">发送</a>
              <a v-if="record.status === 0" @click="handleDeleteConfirm(record)" class="danger">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <!-- 详情弹窗 -->
  <a-modal
    v-model:open="detailVisible"
    title="询价单详情"
    width="700px"
    centered
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="询价单号">{{ currentRecord.inquiryNo }}</a-descriptions-item>
      <a-descriptions-item label="供应商">{{ currentRecord.supplierName }}</a-descriptions-item>
      <a-descriptions-item label="询价日期">{{ currentRecord.inquiryDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="更新时间">{{ currentRecord.updateTime || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建/编辑询价弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    :title="formMode === 'edit' ? '编辑询价单' : '新建询价单'"
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
      <a-form-item label="供应商" name="supplierId">
        <a-select
          v-model:value="formData.supplierId"
          placeholder="请选择供应商"
          show-search
          @change="handleSupplierChange"
        >
          <a-select-option v-for="s in supplierList" :key="s.id" :value="s.id">
            {{ s.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="询价日期" name="inquiryDate">
        <a-date-picker v-model:value="formData.inquiryDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
    <div style="margin-top: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <span style="font-weight: 500;">询价物料明细</span>
        <a-button size="small" type="dashed" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </div>
      <a-table
        :columns="itemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template v-else-if="column.key === 'specification'">
            <a-input v-model:value="record.specification" placeholder="规格" size="small" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" placeholder="数量" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unit'">
            <a-input v-model:value="record.unit" placeholder="单位" size="small" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveItem(index)">删除</a-button>
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
import { inquiryApi } from '@/api/erp'

const loading = ref(false)
const dataSource = ref<any[]>([])
const error = ref<string | null>(null)
const selectedRowKeys = ref<(string | number)[]>([])
const currentView = ref('list')
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 表单弹窗状态 ──────────────────────────────────────
const formModalVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

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

const supplierList = ref([
  { id: 1, name: '供应商A' },
  { id: 2, name: '供应商B' },
  { id: 3, name: '供应商C' }
])

const itemColumns = [
  { title: '物料名称', key: 'productName', width: 150 },
  { title: '规格', key: 'specification', width: 120 },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '单位', key: 'unit', width: 80 },
  { title: '操作', key: 'action', width: 80 }
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

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '询价单' }
])

const columns = [
  { title: '询价单号', dataIndex: 'inquiryNo', key: 'inquiryNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '询价日期', dataIndex: 'inquiryDate', key: 'inquiryDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 150 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
  return colors[status] || 'default'
}
const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '已发送', 2: '已报价' }
  return texts[status] || '未知'
}

const fetchData = async () => {
  loading.value = true; error.value = null
  try {
    const res = await inquiryApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: 1 })
    dataSource.value = res.records || []; pagination.total = res.total || 0
  }
  catch { error.value = '获取数据失败' }
  finally { loading.value = false }
}

const resetForm = () => {
  formData.supplierId = undefined
  formData.inquiryDate = undefined
  formData.remark = ''
  formData.items = []
  editingId.value = null
}

const handleViewChange = (view: string) => { currentView.value = view }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }

// ── 新建询价 ──────────────────────────────────────────
const handleAdd = () => {
  formMode.value = 'add'
  resetForm()
  formModalVisible.value = true
}

// ── 查看详情 ──────────────────────────────────────────
const handleView = (record: any) => { currentRecord.value = record; detailVisible.value = true }

// ── 编辑询价 ──────────────────────────────────────────
const handleEdit = (record: any) => {
  formMode.value = 'edit'
  editingId.value = record.id
  formData.supplierId = record.supplierId
  formData.inquiryDate = record.inquiryDate
  formData.remark = record.remark || ''
  formData.items = (record.items || []).map((item: any) => ({
    tempKey: genTempKey(),
    productName: item.productName || '',
    specification: item.specification || '',
    quantity: item.quantity || 1,
    unit: item.unit || '个'
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

// ── 发送询价 ──────────────────────────────────────────
const handleSend = (record: any) => {
  Modal.confirm({
    title: '发送询价单',
    content: `确定发送询价单 "${record.inquiryNo}" 吗？`,
    okText: '确认发送',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await inquiryApi.send(record.id)
        message.success(`询价单 "${record.inquiryNo}" 发送成功`)
        fetchData()
      } catch { message.error('发送失败') }
    }
  })
}

// ── 删除询价 ──────────────────────────────────────────
const handleDeleteConfirm = (record: any) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除询价单 "${record.inquiryNo}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await inquiryApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch { message.error('删除失败') }
    }
  })
}

// ── 导出功能 ──────────────────────────────────────────
const handleExport = () => {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['询价单号', '供应商', '询价日期', '状态', '创建时间']
    const rows = dataSource.value.map(row => [
      row.inquiryNo || '',
      row.supplierName || '',
      row.inquiryDate || '',
      getStatusText(row.status),
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `询价单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    hideLoading()
    message.success('导出成功，文件下载中')
  } catch {
    hideLoading()
    message.error('导出失败')
  }
}

// ── 批量发送 ──────────────────────────────────────────
const handleBatchSend = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要发送的询价单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量发送',
    content: `确定要批量发送选中的 ${count} 个询价单吗？`,
    okText: '确认发送',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await inquiryApi.send(id as number)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量发送完成，成功 ${successCount} 个`)
      } else {
        message.warning(`发送完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
      }
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// ── 批量删除 ──────────────────────────────────────────
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的询价单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个询价单吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await inquiryApi.delete(id as number)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量删除完成，成功 ${successCount} 个`)
      } else {
        message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
      }
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

onMounted(() => fetchData())
</script>

<style scoped>
.danger { color: #ff4d4f; }
</style>
