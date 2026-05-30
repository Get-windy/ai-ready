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
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quantity'">
            {{ record.quantity }} {{ record.unit }}
          </template>
          <template v-else-if="column.key === 'availableQuantity'">
            <a-tag :color="record.availableQuantity > 0 ? 'green' : 'red'">
              {{ record.availableQuantity }} {{ record.unit }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleCheck(record)">盘点</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

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
            <a-select
              v-model:value="checkForm.warehouseId"
              placeholder="请选择仓库"
              :options="warehouseOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="盘点日期" name="checkDate">
            <a-date-picker
              v-model:value="checkForm.checkDate"
              style="width: 100%"
              placeholder="请选择盘点日期"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea
          v-model:value="checkForm.remark"
          :rows="2"
          placeholder="请输入盘点备注"
        />
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
          <a-input-number
            v-model:value="checkItems[index].actualQty"
            :min="0"
            style="width: 100%"
            placeholder="实盘数量"
          />
        </template>
        <template v-else-if="column.key === 'diff'">
          <a-tag :color="getDiffColor(index)">
            {{ getDiffQty(index) }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import { stockCheckApi, type StockItem } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')

const breadcrumbItems = computed(() => [
  { text: '库存管理', path: '/stock' },
  { text: '库存查询' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '库存数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100 },
  { title: '冻结数量', dataIndex: 'frozenQuantity', key: 'frozenQuantity', width: 100 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const fetchData = async () => {
  loading.value = true
  try {
    dataSource.value = []
    pagination.total = 0
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

const handleViewChange = (view: string) => {
  currentView.value = view
}

const handleClearSelection = () => {
  selectedRowKeys.value = []
}

const handlePageChange = (page: number) => {
  pagination.current = page
  fetchData()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.current = 1
  fetchData()
}

const handleView = (record: any) => {
  router.push(`/stock/detail/${record.id}`)
}

// ── 盘点状态 ──────────────────────────────────────────
const checkVisible = ref(false)
const checkSubmitting = ref(false)
const checkFormRef = ref<FormInstance>()
const checkForm = reactive({
  warehouseId: undefined as number | undefined,
  checkDate: dayjs(),
  remark: ''
})

const checkFormRules = {
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }]
}

const warehouseOptions = [
  { value: 1, label: '主仓库' },
  { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' },
  { value: 4, label: '成品仓库' }
]

interface CheckItem {
  id: number
  productCode: string
  productName: string
  specification?: string
  unit?: string
  quantity: number
  actualQty: number | null
  warehouseName?: string
}

const checkItems = ref<CheckItem[]>([])

const checkItemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 140 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 100 },
  { title: '系统库存', key: 'systemQty', width: 110 },
  { title: '实盘数量', key: 'actualQty', width: 120 },
  { title: '差异', key: 'diff', width: 100 }
]

const getDiffQty = (index: number) => {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return '-'
  const diff = item.actualQty - (item.quantity || 0)
  return diff === 0 ? '无差异' : (diff > 0 ? `+${diff}` : `${diff}`)
}

const getDiffColor = (index: number) => {
  const item = checkItems.value[index]
  if (item.actualQty === null || item.actualQty === undefined) return 'default'
  const diff = item.actualQty - (item.quantity || 0)
  if (diff === 0) return 'green'
  if (diff > 0) return 'blue'
  return 'red'
}

const handleCheck = (record: any) => {
  checkForm.warehouseId = undefined
  checkForm.checkDate = dayjs()
  checkForm.remark = ''

  // 根据选中产品的仓库预填
  if (record.warehouseName) {
    const matched = warehouseOptions.find(w => w.label === record.warehouseName)
    if (matched) checkForm.warehouseId = matched.value
  }

  // 加载产品列表作为盘点明细——实际应调用API获取该仓库产品列表
  checkItems.value = [
    {
      id: record.id || 1,
      productCode: record.productCode || 'PROD-001',
      productName: record.productName || '产品名称',
      specification: record.specification || '-',
      unit: record.unit || '个',
      quantity: record.quantity || 0,
      actualQty: null
    }
  ]

  checkVisible.value = true
}

const handleCheckSubmit = async () => {
  try {
    await checkFormRef.value?.validate()
  } catch {
    return
  }

  checkSubmitting.value = true
  try {
    const payload = {
      warehouseId: checkForm.warehouseId,
      checkDate: checkForm.checkDate.format('YYYY-MM-DD'),
      remark: checkForm.remark,
      items: checkItems.value.map(item => ({
        productId: item.id,
        systemQty: item.quantity,
        actualQty: item.actualQty ?? 0
      }))
    }
    await stockCheckApi.create(payload)
    message.success('盘点单创建成功')
    checkVisible.value = false
    fetchData()
  } catch (err: any) {
    message.error(err?.message || '盘点提交失败')
  } finally {
    checkSubmitting.value = false
  }
}

const handleCheckCancel = () => {
  checkVisible.value = false
}

// ── 导出 ──────────────────────────────────────────────
const exportLoading = ref(false)

const handleExport = async () => {
  exportLoading.value = true
  try {
    const blob = await fetch('/api/erp/stock/export', {
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token') || ''}`
      }
    }).then(res => {
      if (!res.ok) throw new Error('导出请求失败')
      return res.blob()
    })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `库存报表_${dayjs().format('YYYY-MM-DD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err: any) {
    message.error(err?.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>
