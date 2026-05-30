<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :selected-count="selectedRowKeys.length"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
    empty-text="暂无入库单"
    @clear-selection="handleClearSelection"
    @search-submit="handleSearchSubmit"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd"><PlusOutlined /> 新建入库</a-button>
      <a-button @click="handleExport"><ExportOutlined /> 导出</a-button>
    </template>
    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
    </template>
    <template #list-view>
      <a-table :columns="columns" :data-source="dataSource" :loading="loading" :row-selection="rowSelection" row-key="id" :scroll="{ x: 1200 }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">¥{{ record.totalAmount?.toFixed(2) || '0.00' }}</template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleApprove(record)">审批</a>
              <PrintButton v-if="record.status >= 1" template-type="inbound" :business-id="record.id" business-type="purchase_inbound" button-text="打印" button-size="small" />
              <a v-if="record.status === 0" @click="handleDeleteConfirm(record)" class="danger">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <!-- 新建入库弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建入库单"
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
      <a-form-item label="关联订单号" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入采购订单号" @blur="handleOrderNoBlur" />
      </a-form-item>
      <a-form-item label="供应商">
        <a-input v-model:value="formData.supplierName" placeholder="自动关联" disabled />
      </a-form-item>
      <a-form-item label="仓库" name="warehouseId">
        <a-select v-model:value="formData.warehouseId" placeholder="请选择入库仓库">
          <a-select-option v-for="w in warehouseList" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="入库日期" name="inboundDate">
        <a-date-picker v-model:value="formData.inboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
    <div style="margin-top: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <span style="font-weight: 500;">入库物料明细</span>
        <a-button size="small" type="dashed" @click="handleAddInboundItem">
          <template #icon><PlusOutlined /></template>
          添加物料
        </a-button>
      </div>
      <a-table
        :columns="inboundItemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        row-key="tempKey"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productName'">
            <a-input v-model:value="record.productName" placeholder="物料名称" size="small" />
          </template>
          <template v-else-if="column.key === 'expectedQty'">
            <a-input-number v-model:value="record.expectedQty" :min="0" size="small" style="width: 100%" disabled />
          </template>
          <template v-else-if="column.key === 'actualQty'">
            <a-input-number v-model:value="record.actualQty" :min="0" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger size="small" @click="handleRemoveInboundItem(index)">删除</a-button>
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
import { inboundApi, type PurchaseInbound } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<PurchaseInbound[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

// ── 表单状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

let itemCounter = 0
const genTempKey = () => `inbound_item_${++itemCounter}_${Date.now()}`

interface InboundItemForm {
  tempKey: string
  productName: string
  expectedQty: number
  actualQty: number
  unitPrice: number
}

const formData = reactive({
  orderNo: '',
  supplierName: '',
  warehouseId: undefined as number | undefined,
  inboundDate: undefined as any,
  remark: '',
  items: [] as InboundItemForm[]
})

const formRules = {
  orderNo: [{ required: true, message: '请输入关联订单号', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  inboundDate: [{ required: true, message: '请选择入库日期', trigger: 'change' }]
}

const warehouseList = ref([
  { id: 1, name: '主仓库' },
  { id: 2, name: '备品仓库' },
  { id: 3, name: '原料仓库' }
])

const inboundItemColumns = [
  { title: '物料名称', key: 'productName', width: 150 },
  { title: '应入库数量', key: 'expectedQty', width: 100 },
  { title: '实际入库数量', key: 'actualQty', width: 120 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

const handleAddInboundItem = () => {
  formData.items.push({
    tempKey: genTempKey(),
    productName: '',
    expectedQty: 0,
    actualQty: 0,
    unitPrice: 0
  })
}

const handleRemoveInboundItem = (index: number) => {
  formData.items.splice(index, 1)
}

const handleOrderNoBlur = () => {
  if (formData.orderNo) {
    const matched = dataSource.value.find(d => d.orderNo === formData.orderNo)
    if (matched) {
      formData.supplierName = matched.supplierName
    }
  }
}

const breadcrumbItems = computed(() => [{ text: '采购管理', path: '/purchase' }, { text: '入库单' }])

const columns = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 180 },
  { title: '关联订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 120 },
  { title: '金额', key: 'totalAmount', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建人', dataIndex: 'creatorName', key: 'creatorName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 }
]

const rowSelection = computed(() => ({ selectedRowKeys: selectedRowKeys.value, onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys } }))
const getStatusColor = (s: number) => ({ 0: 'default', 1: 'orange', 2: 'green' } as any)[s] || 'default'
const getStatusText = (s: number) => ({ 0: '草稿', 1: '待收货', 2: '已入库' } as any)[s] || '未知'

const fetchData = async () => {
  loading.value = true; error.value = null
  try {
    const res = await inboundApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, tenantId: 1 })
    dataSource.value = res.records || []; pagination.total = res.total || 0
  } catch (err: any) { error.value = err?.message || '获取数据失败' }
  finally { loading.value = false }
}

const searchValue = ref('')
const handleSearchSubmit = (val: string) => { searchValue.value = val; pagination.current = 1; fetchData() }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handlePageChange = (p: number) => { pagination.current = p; fetchData() }
const handlePageSizeChange = (s: number) => { pagination.pageSize = s; pagination.current = 1; fetchData() }

// ── 新建入库 ──────────────────────────────────────────
const handleAdd = () => {
  formData.orderNo = ''
  formData.supplierName = ''
  formData.warehouseId = undefined
  formData.inboundDate = undefined
  formData.remark = ''
  formData.items = []
  formModalVisible.value = true
}

// ── 查看详情 ──────────────────────────────────────────
const handleView = (record: PurchaseInbound) => {
  Modal.info({
    title: '入库单详情',
    content: `入库单号: ${record.inboundNo}\n供应商: ${record.supplierName}\n入库日期: ${record.inboundDate}\n状态: ${getStatusText(record.status)}`,
    centered: true
  })
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
    await inboundApi.create({
      orderNo: formData.orderNo,
      supplierName: formData.supplierName,
      warehouseId: formData.warehouseId,
      inboundDate: formData.inboundDate,
      remark: formData.remark,
      items: formData.items.map(item => ({
        productName: item.productName,
        expectedQty: item.expectedQty,
        actualQty: item.actualQty,
        unitPrice: item.unitPrice
      }))
    })
    message.success('新建入库单成功')
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('新建入库单失败')
  } finally {
    formSubmitting.value = false
  }
}

// ── 审批 ──────────────────────────────────────────────
const handleApprove = async (r: PurchaseInbound) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批入库单 "${r.inboundNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await inboundApi.approve(r.id)
        message.success('审批成功')
        fetchData()
      } catch { message.error('审批失败') }
    }
  })
}

// ── 删除 ──────────────────────────────────────────────
const handleDeleteConfirm = (r: PurchaseInbound) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除入库单 "${r.inboundNo}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try { await inboundApi.delete(r.id); message.success('删除成功'); fetchData() } catch { message.error('删除失败') }
    }
  })
}

// ── 导出功能 ──────────────────────────────────────────
const handleExport = () => {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['入库单号', '关联订单', '供应商', '入库日期', '金额', '状态', '创建人', '创建时间']
    const rows = dataSource.value.map(row => [
      row.inboundNo || '',
      row.orderNo || '',
      row.supplierName || '',
      row.inboundDate || '',
      row.totalAmount?.toFixed(2) || '0.00',
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
    link.download = `入库单_${new Date().toISOString().slice(0, 10)}.csv`
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
    message.warning('请选择要审批的入库单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审批',
    content: `确定要批量审批选中的 ${count} 个入库单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await inboundApi.approve(id as number)
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

// ── 批量删除 ──────────────────────────────────────────
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的入库单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个入库单吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await inboundApi.delete(id as number)
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
<style scoped>.danger { color: #ff4d4f; }</style>
