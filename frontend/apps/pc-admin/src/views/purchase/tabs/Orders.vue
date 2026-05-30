<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :show-search-panel="showSearchPanel"
    :search-panel-collapsed="searchPanelCollapsed"
    :filters="filters"
    :active-filters="activeFilters"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
    empty-text="暂无采购订单"
    search-placeholder="搜索订单号 / 供应商..."
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @search-submit="handleSearchSubmit"
    @filter-toggle="handleFilterToggle"
    @search-panel-toggle="handleSearchPanelToggle"
    @filter-change="handleFilterChange"
    @clear-all-filters="handleClearAllFilters"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新建订单
      </a-button>
      <a-button @click="handleImport">
        <template #icon><ImportOutlined /></template>
        导入
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
      <a-button @click="handleBatchPrint">批量打印</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
    </template>

    <!-- 列表视图 -->
    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmountWithTax'">
            ¥{{ record.totalAmountWithTax?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSubmit(record)">提交</a>
              <a v-if="record.status === 1" @click="handleApprove(record)">审批</a>
              <PrintButton
                v-if="record.status >= 2"
                template-type="order"
                :business-id="record.id"
                business-type="purchase_order"
                button-text="打印"
                button-size="small"
                @print-success="handlePrintSuccess(record)"
                @print-error="handlePrintError"
              />
              <a v-if="record.status === 0" @click="handleDeleteConfirm(record)" class="danger">删除</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>

    <!-- 看板视图 -->
    <template #kanban-view>
      <div class="kanban-container">
        <div
          v-for="status in statusGroups"
          :key="status.value"
          class="kanban-column"
        >
          <div class="kanban-column-header">
            <span class="kanban-column-title">{{ status.label }}</span>
            <span class="kanban-column-count">{{ getOrdersByStatus(status.value).length }}</span>
          </div>
          <div class="kanban-column-body">
            <div
              v-for="order in getOrdersByStatus(status.value)"
              :key="order.id"
              class="kanban-card"
              @click="handleView(order)"
            >
              <div class="kanban-card-header">
                <span class="kanban-card-no">{{ order.orderNo }}</span>
                <a-tag :color="getStatusColor(order.status)" size="small">
                  {{ getStatusText(order.status) }}
                </a-tag>
              </div>
              <div class="kanban-card-body">
                <div class="kanban-card-row">
                  <span class="kanban-card-label">供应商:</span>
                  <span class="kanban-card-value">{{ order.supplierName }}</span>
                </div>
                <div class="kanban-card-row">
                  <span class="kanban-card-label">金额:</span>
                  <span class="kanban-card-value">¥{{ order.totalAmountWithTax?.toFixed(2) }}</span>
                </div>
                <div class="kanban-card-row">
                  <span class="kanban-card-label">日期:</span>
                  <span class="kanban-card-value">{{ order.orderDate }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </ModuleLayout>

  <PurchaseOrderFormModal
    v-model:open="formModalVisible"
    :edit-data="currentEditData"
    @success="handleFormSuccess"
  />

  <!-- 导入弹窗 -->
  <a-modal
    v-model:open="importModalVisible"
    title="导入采购订单"
    width="750px"
    centered
    :confirm-loading="importSubmitting"
    ok-text="确认导入"
    cancel-text="取消"
    @ok="handleImportConfirm"
    @cancel="handleImportCancel"
  >
    <a-form layout="vertical">
      <a-form-item label="选择文件 (.csv)">
        <a-upload-dragger
          v-model:file-list="importFileList"
          :before-upload="handleBeforeUpload"
          :max-count="1"
          accept=".csv"
          @remove="handleImportFileRemove"
        >
          <p class="ant-upload-drag-icon"><inbox-outlined /></p>
          <p class="ant-upload-text">点击或拖拽 CSV 文件到此区域上传</p>
          <p class="ant-upload-hint">支持 .csv 格式，第一行为表头</p>
        </a-upload-dragger>
      </a-form-item>
    </a-form>
    <div v-if="importParseError" style="margin-top: 8px;">
      <a-alert :message="importParseError" type="error" show-icon />
    </div>
    <div v-if="importPreviewData.length > 0" style="margin-top: 16px;">
      <a-alert
        :message="`已识别 ${importPreviewData.length} 条数据，请确认后导入`"
        type="info"
        show-icon
        style="margin-bottom: 12px;"
      />
      <a-table
        :columns="importPreviewColumns"
        :data-source="importPreviewData"
        :pagination="false"
        :scroll="{ y: 300 }"
        size="small"
        row-key="rowIndex"
      />
    </div>
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
      暂无选中订单
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
              template-type="order"
              :business-id="record.id"
              business-type="purchase_order"
              button-text="打印"
              button-size="small"
              @print-success="() => handleSinglePrintSuccess(record)"
              @print-error="handlePrintError"
            />
          </template>
        </template>
      </a-table>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ImportOutlined, ExportOutlined, InboxOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import type { FilterItem } from '@ai-ready/components'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'
import { useModulePage } from '@/composables/useModulePage'
import PurchaseOrderFormModal from '../components/PurchaseOrderFormModal.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter()

// ── 筛选器配置 ────────────────────────────────────────
const filters: FilterItem[] = [
  {
    key: 'status',
    label: '状态',
    type: 'checkbox',
    options: [
      { value: 0, label: '草稿' },
      { value: 1, label: '待审批' },
      { value: 2, label: '已审批' },
      { value: 3, label: '部分入库' },
      { value: 4, label: '完成' },
      { value: 5, label: '已取消' }
    ]
  },
  {
    key: 'supplierId',
    label: '供应商',
    type: 'select',
    options: [
      { value: 1, label: '供应商A' },
      { value: 2, label: '供应商B' }
    ]
  },
  {
    key: 'dateRange',
    label: '日期范围',
    type: 'daterange'
  }
]

// ── 通用页面状态 ──────────────────────────────────────
const {
  loading,
  dataSource,
  pagination,
  error,
  selectedRowKeys,
  currentView,
  activeFilters,
  showSearchPanel,
  searchPanelCollapsed,
  searchValue,
  breadcrumbItems,
  fetchData,
  handleSearchSubmit,
  handleViewChange,
  handleClearSelection,
  handleFilterToggle,
  handleFilterChange,
  handleClearAllFilters,
  handlePageChange,
  handlePageSizeChange,
  handleSearchPanelToggle
} = useModulePage<PurchaseOrder>({
  fetchFn: purchaseOrderApi.page as any,
  moduleName: '采购管理',
  pageTitle: '采购订单',
  modulePath: '/purchase',
  filters
})

// ── 本地状态 ──────────────────────────────────────────
const formModalVisible = ref(false)
const currentEditData = ref<PurchaseOrder | null>(null)

// ── 导入状态 ──────────────────────────────────────────
const importModalVisible = ref(false)
const importFileList = ref<any[]>([])
const importPreviewData = ref<any[]>([])
const importParseError = ref<string | null>(null)
const importSubmitting = ref(false)

const importPreviewColumns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 100 }
]

// ── 批量打印状态 ──────────────────────────────────────
const batchPrintModalVisible = ref(false)
const printItems = ref<any[]>([])

const printTableColumns = [
  { title: '选择', key: 'checked', width: 60 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 100 },
  { title: '操作', key: 'action', width: 100 }
]

// ── 表格列 ────────────────────────────────────────────
const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '采购员', dataIndex: 'purchaserName', key: 'purchaserName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 }
]

// ── 看板分组 ──────────────────────────────────────────
const statusGroups = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待审批' },
  { value: 2, label: '已审批' },
  { value: 3, label: '部分入库' },
  { value: 4, label: '完成' }
]

const getOrdersByStatus = (status: number) => {
  return dataSource.value.filter(order => order.status === status)
}

// ── 行选择 ────────────────────────────────────────────
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys
  }
}))

// ── 状态映射 ──────────────────────────────────────────
const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green', 3: 'blue', 4: 'success', 5: 'red' }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '部分入库', 4: '完成', 5: '已取消' }
  return texts[status] || '未知'
}

// ── 导入相关函数 ──────────────────────────────────────
const parseCSV = (text: string): any[] => {
  const lines = text.split(/\r?\n/).filter(line => line.trim())
  if (lines.length < 2) throw new Error('CSV 文件至少需要包含表头和一行数据')
  const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''))
  const result: any[] = []
  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''))
    const row: any = { rowIndex: i }
    headers.forEach((h, idx) => { row[h] = values[idx] || '' })
    result.push(row)
  }
  return result
}

const handleBeforeUpload = (file: any) => {
  importParseError.value = null
  importPreviewData.value = []
  const isCSV = file.name.toLowerCase().endsWith('.csv')
  if (!isCSV) {
    importParseError.value = '目前仅支持 .csv 格式文件'
    return false
  }
  const reader = new FileReader()
  reader.onload = (e: ProgressEvent<FileReader>) => {
    try {
      const text = e.target?.result as string
      const parsed = parseCSV(text)
      importPreviewData.value = parsed
    } catch (err: any) {
      importParseError.value = `文件解析失败: ${err.message || '未知错误'}`
    }
  }
  reader.onerror = () => { importParseError.value = '文件读取失败' }
  reader.readAsText(file)
  return false
}

const handleImportFileRemove = () => {
  importPreviewData.value = []
  importParseError.value = null
}

const handleImport = () => {
  importModalVisible.value = true
  importFileList.value = []
  importPreviewData.value = []
  importParseError.value = null
}

const handleImportConfirm = async () => {
  if (importPreviewData.value.length === 0) {
    message.warning('请先选择文件并解析数据')
    return
  }
  importSubmitting.value = true
  let successCount = 0
  let failCount = 0
  try {
    for (const row of importPreviewData.value) {
      try {
        await purchaseOrderApi.create({
          orderNo: row.orderNo || `PO-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          supplierId: row.supplierId ? Number(row.supplierId) : 1,
          supplierName: row.supplierName || '未知供应商',
          orderDate: row.orderDate || new Date().toISOString().slice(0, 10),
          totalAmountWithTax: row.totalAmountWithTax ? Number(row.totalAmountWithTax) : 0,
          status: 0
        } as any)
        successCount++
      } catch {
        failCount++
      }
    }
    if (failCount === 0) {
      message.success(`成功导入 ${successCount} 条订单`)
    } else {
      message.warning(`导入完成: 成功 ${successCount} 条, 失败 ${failCount} 条`)
    }
    importModalVisible.value = false
    fetchData()
  } catch {
    message.error('导入过程发生错误')
  } finally {
    importSubmitting.value = false
  }
}

const handleImportCancel = () => {
  importModalVisible.value = false
}

// ── 导出功能 ──────────────────────────────────────────
const handleExport = () => {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['订单号', '供应商', '订单日期', '订单金额', '状态', '采购员', '创建时间']
    const rows = dataSource.value.map(row => [
      row.orderNo || '',
      row.supplierName || '',
      row.orderDate || '',
      row.totalAmountWithTax?.toFixed(2) || '0.00',
      getStatusText(row.status),
      row.purchaserName || '',
      row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `采购订单_${new Date().toISOString().slice(0, 10)}.csv`
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
    message.warning('请选择要审批的订单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量审批',
    content: `确定要批量审批选中的 ${count} 个订单吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await purchaseOrderApi.approve(id as number)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量审批完成，成功 ${successCount} 个订单`)
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
  printItems.value.forEach(item => { item.checked = checked })
}

const handlePrintAll = () => {
  const toPrint = printItems.value.filter(item => item.checked)
  if (toPrint.length === 0) {
    message.warning('请选择要打印的订单')
    return
  }
  message.success(`正在发送 ${toPrint.length} 个订单的打印任务...`)
}

const handleSinglePrintSuccess = (record: any) => {
  message.success(`订单 ${record.orderNo} 打印成功`)
}

const handleBatchPrint = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要打印的订单')
    return
  }
  const selected = dataSource.value.filter(item => selectedRowKeys.value.includes(item.id))
  printItems.value = selected.map(item => ({
    ...item,
    checked: true
  }))
  batchPrintModalVisible.value = true
}

// ── 批量删除 ──────────────────────────────────────────
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要删除的订单')
    return
  }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个订单吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      let successCount = 0
      let failCount = 0
      for (const id of selectedRowKeys.value) {
        try {
          await purchaseOrderApi.delete(id as number)
          successCount++
        } catch {
          failCount++
        }
      }
      if (failCount === 0) {
        message.success(`批量删除完成，成功 ${successCount} 个订单`)
      } else {
        message.warning(`删除完成: 成功 ${successCount} 个, 失败 ${failCount} 个`)
      }
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// ── CRUD 操作 ─────────────────────────────────────────
const handleAdd = () => {
  currentEditData.value = null
  formModalVisible.value = true
}
const handleView = (record: PurchaseOrder) => {
  router.push(`/purchase/order/${record.id}`)
}
const handleEdit = (record: PurchaseOrder) => {
  currentEditData.value = record
  formModalVisible.value = true
}
const handleSubmit = async (record: PurchaseOrder) => {
  try {
    await purchaseOrderApi.submit(record.id)
    message.success('提交成功')
    fetchData()
  } catch { message.error('提交失败') }
}
const handleApprove = async (record: PurchaseOrder) => {
  try {
    await purchaseOrderApi.approve(record.id)
    message.success('审批成功')
    fetchData()
  } catch { message.error('审批失败') }
}
const handleDeleteConfirm = (record: PurchaseOrder) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除订单 "${record.orderNo}" 吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await purchaseOrderApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch { message.error('删除失败') }
    }
  })
}

const handleFormSuccess = () => fetchData()

const handlePrintSuccess = (record: PurchaseOrder) => {
  message.success(`订单 ${record.orderNo} 打印成功`)
}
const handlePrintError = (err: any) => {
  message.error(`打印失败: ${err?.message || '未知错误'}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.kanban-container {
  display: flex;
  gap: 16px;
  height: 100%;
  overflow: auto;
}

.kanban-column {
  flex: 1;
  min-width: 280px;
  background-color: #f5f5f5;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}

.kanban-column-header {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kanban-column-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-column-count {
  font-size: 12px;
  color: #909399;
}

.kanban-column-body {
  flex: 1;
  padding: 8px;
  overflow: auto;
}

.kanban-card {
  background-color: #fff;
  border-radius: 4px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.kanban-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.kanban-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kanban-card-no {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-card-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kanban-card-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.kanban-card-label {
  color: #909399;
}

.kanban-card-value {
  color: #606266;
}

.danger {
  color: #ff4d4f;
}
</style>
