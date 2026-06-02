<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-check-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
    add-text="新建盘点"
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

  <a-modal v-model:open="detailVisible" title="盘点单详情" width="700px" :footer="null">
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="盘点单号">{{ currentRecord.checkNo }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="盘点日期">{{ currentRecord.checkDate }}</a-descriptions-item>
      <a-descriptions-item label="盘点状态"><a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag></a-descriptions-item>
      <a-descriptions-item label="盘点人">{{ currentRecord.checkerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="盘点数量">{{ currentRecord.checkQuantity || '-' }}</a-descriptions-item>
      <a-descriptions-item label="差异数量">{{ currentRecord.diffQuantity || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px"><a-button @click="detailVisible = false">关闭</a-button></div>
  </a-modal>

  <!-- 新建盘点弹窗 -->
  <a-modal v-model:open="addVisible" title="新建盘点单" width="900px" :confirm-loading="addSubmitting"
    @ok="handleAddSubmit" @cancel="handleAddCancel">
    <a-form ref="addFormRef" :model="addForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" :rules="addFormRules">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="盘点仓库" name="warehouseId">
            <a-select v-model:value="addForm.warehouseId" placeholder="请选择仓库" :options="warehouseOptions" @change="handleWarehouseChange" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="盘点日期" name="checkDate">
            <a-date-picker v-model:value="addForm.checkDate" style="width: 100%" placeholder="请选择盘点日期" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="产品类别" name="categoryId">
            <a-select v-model:value="addForm.categoryId" placeholder="请选择产品类别筛选（可选）" :options="categoryOptions" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="盘点方式" name="checkMode">
            <a-radio-group v-model:value="addForm.checkMode">
              <a-radio :value="1">全量盘点</a-radio>
              <a-radio :value="2">抽盘</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>

    <div v-if="addForm.warehouseId" style="text-align: center; margin: 16px 0">
      <a-button type="primary" :loading="generatingList" @click="handleGenerateCheckList">生成盘点清单</a-button>
      <span v-if="checkItems.length > 0" style="margin-left: 12px; color: #666">共 {{ checkItems.length }} 条产品记录</span>
    </div>

    <a-divider v-if="checkItems.length > 0" style="margin: 12px 0">盘点明细</a-divider>
    <a-table v-if="checkItems.length > 0" :columns="addItemColumns" :data-source="checkItems" :pagination="false" size="small" row-key="id" :scroll="{ y: 300 }">
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'systemQty'">{{ record.quantity || 0 }} {{ record.unit || '' }}</template>
        <template v-else-if="column.key === 'actualQty'">
          <a-input-number v-model:value="checkItems[index].actualQty" :min="0" style="width: 100%" placeholder="实盘数量" />
        </template>
        <template v-else-if="column.key === 'diff'">
          <a-tag :color="getDiffColor(index)">{{ getDiffText(index) }}</a-tag>
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
import { stockCheckApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const tableRef = ref()
const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '盘点单号', dataIndex: 'checkNo', key: 'checkNo', width: 160, sortable: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '盘点日期', dataIndex: 'checkDate', key: 'checkDate', width: 110, type: 'date' as const },
  { title: '盘点状态', dataIndex: 'status', key: 'status', width: 100, type: 'status' as const, slotName: 'status' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160, type: 'date' as const },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'checkNo', label: '盘点单号', type: 'input' as const, placeholder: '输入盘点单号' },
  { key: 'warehouseName', label: '仓库', type: 'input' as const, placeholder: '输入仓库名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 0 }, { label: '待审批', value: 1 }, { label: '已完成', value: 2 }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

const statusColorMap: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'green' }
const statusTextMap: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已完成' }
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [{ label: '本页数量', value: dataSource.value.length, type: 'default' as const }]
})
function getStatusColor(status: number): string { return statusColorMap[status] || 'default' }
function getStatusText(status: number): string { return statusTextMap[status] || '未知' }

const detailVisible = ref(false)
const currentRecord = ref<any>(null)

// ── 新建盘点 ──
interface CheckItem { id: number; productCode: string; productName: string; specification?: string; unit?: string; categoryId?: number; quantity: number; actualQty: number | null }
const addVisible = ref(false)
const addSubmitting = ref(false)
const generatingList = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ warehouseId: undefined as number | undefined, checkDate: dayjs(), categoryId: undefined as number | undefined, checkMode: 1, remark: '' })
const addFormRules = { warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }], checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }] }
const checkItems = ref<CheckItem[]>([])
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
const categoryOptions = [
  { value: 1, label: '原材料' }, { value: 2, label: '半成品' }, { value: 3, label: '成品' },
  { value: 4, label: '包装材料' }, { value: 5, label: '备品备件' }
]
const addItemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 100 },
  { title: '系统库存', key: 'systemQty', width: 110 },
  { title: '实盘数量', key: 'actualQty', width: 120 },
  { title: '差异', key: 'diff', width: 100 }
]

const mockProductData: CheckItem[] = [
  { id: 1, productCode: 'PROD-001', productName: '螺丝螺母套装', specification: 'M6x20', unit: '套', categoryId: 1, quantity: 500, actualQty: null },
  { id: 2, productCode: 'PROD-002', productName: '不锈钢板材', specification: '2mm', unit: '张', categoryId: 1, quantity: 200, actualQty: null },
  { id: 3, productCode: 'PROD-003', productName: '电子元件A型', specification: 'SMD-0805', unit: '个', categoryId: 3, quantity: 10000, actualQty: null },
  { id: 4, productCode: 'PROD-004', productName: '包装箱(大)', specification: '600x400x300', unit: '个', categoryId: 4, quantity: 300, actualQty: null },
  { id: 5, productCode: 'PROD-005', productName: '轴承6205', specification: '25x52x15', unit: '个', categoryId: 5, quantity: 150, actualQty: null },
  { id: 6, productCode: 'PROD-006', productName: '电机组件B', specification: '220V/1.5kW', unit: '台', categoryId: 3, quantity: 45, actualQty: null },
  { id: 7, productCode: 'PROD-007', productName: '铝合金型材', specification: '40x40', unit: '米', categoryId: 1, quantity: 800, actualQty: null },
  { id: 8, productCode: 'PROD-008', productName: '密封圈', specification: 'DN50', unit: '个', categoryId: 5, quantity: 2000, actualQty: null }
]

function getDiffText(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return '-'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return '无差异'
  return diff > 0 ? `+${diff}` : `${diff}`
}
function getDiffColor(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return 'default'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return 'green'
  if (diff > 0) return 'blue'
  return 'red'
}
function handleWarehouseChange() { checkItems.value = [] }
async function handleGenerateCheckList() {
  if (!addForm.warehouseId) { message.warning('请先选择盘点仓库'); return }
  generatingList.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 800))
    let products = [...mockProductData]
    if (addForm.categoryId) products = products.filter(p => p.categoryId === addForm.categoryId)
    checkItems.value = products.map(p => ({ ...p, actualQty: null }))
    message.success(`已生成 ${checkItems.value.length} 条盘点记录`)
  } catch (err: any) { message.error(err?.message || '生成盘点清单失败') }
  finally { generatingList.value = false }
}

async function fetchData() {
  loading.value = true; error.value = null
  try {
    const res = await stockCheckApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    dataSource.value = (res as any).records || []; pagination.total = (res as any).total || 0
  } catch { error.value = '获取数据失败' }
  finally { loading.value = false }
}

function handleView(record: any) { currentRecord.value = record; detailVisible.value = true }
function handleAdd() {
  addForm.warehouseId = undefined; addForm.checkDate = dayjs(); addForm.categoryId = undefined
  addForm.checkMode = 1; addForm.remark = ''; checkItems.value = []; addVisible.value = true
}

async function handleDelete(record: any) {
  try { await stockCheckApi.create({ id: record.id, action: 'delete' } as any); message.success('删除成功'); fetchData() }
  catch { message.error('删除失败') }
}
async function handleBatchDelete(ids: number[]) {
  let successCount = 0
  for (const id of ids) { try { await stockCheckApi.create({ id, action: 'delete' } as any); successCount++ } catch {} }
  message.success(`批量删除完成，成功 ${successCount} 个`); fetchData()
}

const handleAddSubmit = async () => {
  try { await addFormRef.value?.validate() } catch { return }
  if (checkItems.value.length === 0) { message.warning('请先生成盘点清单'); return }
  addSubmitting.value = true
  try {
    await stockCheckApi.create({
      warehouseId: addForm.warehouseId, checkDate: addForm.checkDate.format('YYYY-MM-DD'),
      checkMode: addForm.checkMode, remark: addForm.remark,
      items: checkItems.value.map(item => ({ productId: item.id, systemQty: item.quantity, actualQty: item.actualQty ?? 0 }))
    })
    message.success('盘点单创建成功'); addVisible.value = false; fetchData()
  } catch (err: any) { message.error(err?.message || '盘点单创建失败') }
  finally { addSubmitting.value = false }
}
const handleAddCancel = () => { addVisible.value = false }

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批盘点结果', content: `确定为盘点单 "${record.checkNo}" 的盘点结果？审批后库存将按实盘数量更新。`,
    okText: '确认审批', cancelText: '取消', centered: true,
    async onOk() {
      try { await stockCheckApi.create({ id: record.id, action: 'approve' } as any); message.success('盘点结果审批成功'); fetchData() }
      catch (err: any) { message.error(err?.message || '审批失败') }
    }
  })
}
function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) { message.warning('请选择盘点单'); return }
  Modal.confirm({
    title: '批量审批', content: `审批选中的 ${keys.length} 条记录？`, okText: '确认', centered: true,
    async onOk() {
      let success = 0; let fail = 0
      for (const id of keys) {
        try { await stockCheckApi.create({ id, action: 'approve' } as any); success++ } catch { fail++ }
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
    const headers = ['盘点单号', '仓库', '盘点日期', '盘点状态', '创建时间']
    const rows = dataSource.value.map((row: any) => [
      row.checkNo || '', row.warehouseName || '', row.checkDate || '',
      getStatusText(row.status), row.createTime || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'; const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob); const link = document.createElement('a')
    link.href = url; link.download = `盘点单_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(url); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
