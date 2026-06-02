<template>
  <TableList
    ref="tableRef"
    :columns="columns"
    :data-source="dataSource"
    :loading="loading"
    :pagination="pagination"
    :table-key="'stock-stock-list'"
    :filter-fields="filterFields"
    :show-summary="true"
    :summary-data="summaryData"
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

    <template #availableQuantity="{ record }">
      <a-tag :color="record.availableQuantity > 0 ? 'green' : 'red'">
        {{ record.availableQuantity }} {{ record.unit || '' }}
      </a-tag>
    </template>

    <template #action="{ record }">
      <a-space :size="4">
        <a-tooltip title="查看">
          <a-button type="link" size="small" @click="handleView(record)">
            <template #icon><EyeOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="盘点">
          <a-button type="link" size="small" @click="handleCheck(record)">
            <template #icon><CheckCircleOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
    </template>
  </TableList>

  <!-- 盘点弹窗 -->
  <a-modal
    v-model:open="checkVisible"
    title="库存盘点"
    width="900px"
    :confirm-loading="checkSubmitting"
    @ok="handleCheckSubmit"
    @cancel="handleCheckCancel"
  >
    <a-form
      ref="checkFormRef"
      :model="checkForm"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      :rules="checkFormRules"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="盘点仓库" name="warehouseId">
            <a-select v-model:value="checkForm.warehouseId" placeholder="请选择仓库" :options="warehouseOptions" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="盘点日期" name="checkDate">
            <a-date-picker v-model:value="checkForm.checkDate" style="width: 100%" placeholder="请选择盘点日期" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="checkForm.remark" :rows="2" placeholder="请输入盘点备注" />
      </a-form-item>
    </a-form>

    <a-divider style="margin: 12px 0">盘点明细</a-divider>

    <a-table
      :columns="checkItemColumns"
      :data-source="checkItems"
      :pagination="false"
      size="small"
      row-key="id"
      :scroll="{ y: 300 }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'systemQty'">
          {{ record.quantity || 0 }} {{ record.unit || '' }}
        </template>
        <template v-else-if="column.key === 'actualQty'">
          <a-input-number v-model:value="checkItems[index].actualQty" :min="0" style="width: 100%" placeholder="实盘数量" />
        </template>
        <template v-else-if="column.key === 'diff'">
          <a-tag :color="getDiffColor(index)">{{ getDiffQty(index) }}</a-tag>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ExportOutlined, EyeOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import { stockApi, stockCheckApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const router = useRouter()
const tableRef = ref()
const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 160, sortable: true },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '库存数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100, slotName: 'availableQuantity' },
  { title: '冻结数量', dataIndex: 'frozenQuantity', key: 'frozenQuantity', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const, type: 'action' as const }
]

const filterFields = [
  { key: 'productCode', label: '产品编码', type: 'input' as const, placeholder: '输入产品编码' },
  { key: 'productName', label: '产品名称', type: 'input' as const, placeholder: '输入产品名称' },
  { key: 'warehouseName', label: '仓库', type: 'input' as const, placeholder: '输入仓库名称' }
]

const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  const totalQty = dataSource.value.reduce((s, r) => s + (r.quantity || 0), 0)
  const totalAvail = dataSource.value.reduce((s, r) => s + (r.availableQuantity || 0), 0)
  return [
    { label: '库存合计', value: totalQty, type: 'default' as const },
    { label: '可用合计', value: totalAvail, type: 'default' as const }
  ]
})

async function fetchData() {
  loading.value = true; error.value = null
  try {
    const res = await stockApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchFilters })
    dataSource.value = (res as any).records || []; pagination.total = (res as any).total || 0
  } catch { error.value = '获取数据失败' }
  finally { loading.value = false }
}

function handleView(record: any) {
  router.push(`/stock/detail/${record.id}`)
}

// ── 盘点 ──
const checkVisible = ref(false)
const checkSubmitting = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = reactive({ warehouseId: undefined as number | undefined, checkDate: dayjs(), remark: '' })
const checkFormRules = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }]
}
const warehouseOptions = [
  { value: 1, label: '主仓库' }, { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' }, { value: 4, label: '成品仓库' }
]
interface CheckItem { id: number; productCode: string; productName: string; specification?: string; unit?: string; quantity: number; actualQty: number | null; warehouseName?: string }
const checkItems = ref<CheckItem[]>([])
const checkItemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 100 },
  { title: '系统库存', key: 'systemQty', width: 110 },
  { title: '实盘数量', key: 'actualQty', width: 120 },
  { title: '差异', key: 'diff', width: 100 }
]
function getDiffQty(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return '-'
  const diff = item.actualQty - (item.quantity || 0)
  return diff === 0 ? '无差异' : (diff > 0 ? `+${diff}` : `${diff}`)
}
function getDiffColor(index: number) {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return 'default'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return 'green'
  if (diff > 0) return 'blue'
  return 'red'
}
function handleCheck(record: any) {
  checkForm.warehouseId = undefined; checkForm.checkDate = dayjs(); checkForm.remark = ''
  if (record.warehouseName) {
    const matched = warehouseOptions.find(w => w.label === record.warehouseName)
    if (matched) checkForm.warehouseId = matched.value
  }
  checkItems.value = [{
    id: record.id || 1, productCode: record.productCode || '', productName: record.productName || '',
    specification: record.specification || '', unit: record.unit || '个', quantity: record.quantity || 0, actualQty: null
  }]
  checkVisible.value = true
}
async function handleCheckSubmit() {
  try { await checkFormRef.value?.validate() } catch { return }
  checkSubmitting.value = true
  try {
    await stockCheckApi.create({
      warehouseId: checkForm.warehouseId, checkDate: checkForm.checkDate.format('YYYY-MM-DD'),
      remark: checkForm.remark, items: checkItems.value.map(item => ({
        productId: item.id, systemQty: item.quantity, actualQty: item.actualQty ?? 0
      }))
    })
    message.success('盘点单创建成功'); checkVisible.value = false; fetchData()
  } catch (err: any) { message.error(err?.message || '盘点提交失败') }
  finally { checkSubmitting.value = false }
}
function handleCheckCancel() { checkVisible.value = false }

// ── 导出 ──
function handleExport() {
  const hideLoading = message.loading('正在生成导出文件...', 0)
  try {
    const headers = ['产品编码', '产品名称', '规格型号', '仓库', '库存数量', '可用数量', '冻结数量']
    const rows = dataSource.value.map((row: any) => [
      row.productCode || '', row.productName || '', row.specification || '', row.warehouseName || '',
      row.quantity || 0, row.availableQuantity || 0, row.frozenQuantity || 0
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v}"`).join(','))].join('\n')
    const BOM = '﻿'; const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob); const link = document.createElement('a')
    link.href = url; link.download = `库存报表_${new Date().toISOString().slice(0, 10)}.csv`
    link.click(); URL.revokeObjectURL(url); hideLoading(); message.success('导出成功')
  } catch { hideLoading(); message.error('导出失败') }
}

function handleSearch(keyword: string) { searchFilters.keyword = keyword || undefined; pagination.current = 1; fetchData() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

onMounted(() => fetchData())
</script>
