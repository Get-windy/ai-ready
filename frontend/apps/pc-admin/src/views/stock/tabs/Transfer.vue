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
        新建调拨
      </a-button>
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
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
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

  <a-modal
    v-model:open="detailVisible"
    title="调拨单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="调拨单号">{{ currentRecord.transferNo }}</a-descriptions-item>
      <a-descriptions-item label="调出仓库">{{ currentRecord.fromWarehouse }}</a-descriptions-item>
      <a-descriptions-item label="调入仓库">{{ currentRecord.toWarehouse }}</a-descriptions-item>
      <a-descriptions-item label="调拨日期">{{ currentRecord.transferDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="调拨数量">{{ currentRecord.quantity || '-' }}</a-descriptions-item>
      <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建调拨弹窗 -->
  <a-modal
    v-model:open="addVisible"
    title="新建调拨单"
    width="800px"
    :confirm-loading="addSubmitting"
    @ok="handleAddSubmit"
    @cancel="handleAddCancel"
  >
    <a-form
      ref="addFormRef"
      :model="addForm"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      :rules="addFormRules"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="调出仓库" name="fromWarehouseId">
            <a-select
              v-model:value="addForm.fromWarehouseId"
              placeholder="请选择调出仓库"
              :options="warehouseOptions"
              @change="handleFromWarehouseChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="调入仓库" name="toWarehouseId">
            <a-select
              v-model:value="addForm.toWarehouseId"
              placeholder="请选择调入仓库"
              :options="filteredToWarehouseOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="调拨日期" name="transferDate">
            <a-date-picker
              v-model:value="addForm.transferDate"
              style="width: 100%"
              placeholder="请选择调拨日期"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="调拨类型" name="transferType">
            <a-select
              v-model:value="addForm.transferType"
              placeholder="请选择调拨类型"
              :options="transferTypeOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="调拨原因" name="reason" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.reason" :rows="2" placeholder="请输入调拨原因" />
      </a-form-item>
    </a-form>

    <a-divider style="margin: 12px 0">调拨明细</a-divider>

    <div style="margin-bottom: 12px">
      <a-button type="dashed" size="small" @click="addTransferItem">
        <template #icon><PlusOutlined /></template>
        添加产品
      </a-button>
      <span v-if="addForm.items.length > 0" style="margin-left: 8px; color: #888; font-size: 12px">
        共 {{ addForm.items.length }} 条，合计数量: {{ totalTransferQty }}
      </span>
    </div>

    <a-table
      :columns="addItemColumns"
      :data-source="addForm.items"
      :pagination="false"
      size="small"
      row-key="key"
      :scroll="{ y: 250 }"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'productName'">
          <a-select
            v-model:value="addForm.items[index].productId"
            show-search
            placeholder="选择产品"
            :options="productOptions"
            style="width: 100%"
            size="small"
            @change="(val: number) => handleItemProductChange(index, val)"
          />
        </template>
        <template v-else-if="column.key === 'availableQty'">
          <a-tag :color="record.availableQty > 0 ? 'green' : 'red'">
            {{ record.availableQty }} {{ record.unit || '' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-model:value="addForm.items[index].quantity"
            :min="1"
            :max="record.availableQty"
            style="width: 100%"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="removeTransferItem(index)">删除</a-button>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import { stockTransferApi } from '@/api/erp'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const breadcrumbItems = computed(() => [
  { text: '库存管理', path: '/stock' },
  { text: '库存调拨' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '调拨单号', dataIndex: 'transferNo', key: 'transferNo', width: 180 },
  { title: '调出仓库', dataIndex: 'fromWarehouse', key: 'fromWarehouse', width: 120 },
  { title: '调入仓库', dataIndex: 'toWarehouse', key: 'toWarehouse', width: 120 },
  { title: '调拨日期', dataIndex: 'transferDate', key: 'transferDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys
  }
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已完成'
  }
  return texts[status] || '未知'
}

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

// ── 新建调拨 ──────────────────────────────────────────
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()

interface AddTransferItem {
  key: number
  productId: number | undefined
  productName: string
  unit: string
  availableQty: number
  quantity: number
}

const addForm = reactive({
  fromWarehouseId: undefined as number | undefined,
  toWarehouseId: undefined as number | undefined,
  transferDate: dayjs(),
  transferType: 1,
  reason: '',
  items: [] as AddTransferItem[]
})

const addFormRules = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  transferDate: [{ required: true, message: '请选择调拨日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入调拨原因', trigger: 'blur' }]
}

let itemKeyCounter = 0

const warehouseOptions = [
  { value: 1, label: '主仓库' },
  { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' },
  { value: 4, label: '成品仓库' }
]

const filteredToWarehouseOptions = computed(() => {
  return warehouseOptions.filter(w => w.value !== addForm.fromWarehouseId)
})

const transferTypeOptions = [
  { value: 1, label: '仓库间调拨' },
  { value: 2, label: '生产领料' },
  { value: 3, label: '退料回库' }
]

const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' },
  { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' },
  { value: 4, label: 'PROD-004 包装箱(大)' }
]

const addItemColumns = [
  { title: '产品名称', key: 'productName' },
  { title: '可用库存', key: 'availableQty', width: 100 },
  { title: '调拨数量', key: 'quantity', width: 110 },
  { title: '操作', key: 'action', width: 60 }
]

const totalTransferQty = computed(() => {
  return addForm.items.reduce((sum, item) => sum + (item.quantity || 0), 0)
})

const handleFromWarehouseChange = () => {
  if (addForm.toWarehouseId === addForm.fromWarehouseId) {
    addForm.toWarehouseId = undefined
  }
  addForm.items = []
}

const handleItemProductChange = (index: number, productId: number) => {
  const product = productOptions.find(p => p.value === productId)
  if (product) {
    addForm.items[index].productName = product.label
    const stockMap: Record<number, { qty: number; unit: string }> = {
      1: { qty: 500, unit: '套' },
      2: { qty: 200, unit: '张' },
      3: { qty: 10000, unit: '个' },
      4: { qty: 300, unit: '个' }
    }
    const stock = stockMap[productId] || { qty: 0, unit: '-' }
    addForm.items[index].availableQty = stock.qty
    addForm.items[index].unit = stock.unit
  }
}

const addTransferItem = () => {
  addForm.items.push({
    key: itemKeyCounter++,
    productId: undefined,
    productName: '',
    unit: '',
    availableQty: 0,
    quantity: 1
  })
}

const removeTransferItem = (index: number) => {
  addForm.items.splice(index, 1)
}

const handleAdd = () => {
  addForm.fromWarehouseId = undefined
  addForm.toWarehouseId = undefined
  addForm.transferDate = dayjs()
  addForm.transferType = 1
  addForm.reason = ''
  addForm.items = []
  itemKeyCounter = 0
  addVisible.value = true
}

const handleAddSubmit = async () => {
  try {
    await addFormRef.value?.validate()
  } catch {
    return
  }

  if (addForm.items.length === 0) {
    message.warning('请至少添加一条调拨明细')
    return
  }

  const overStockItem = addForm.items.find(item => item.quantity > item.availableQty)
  if (overStockItem) {
    message.warning(`产品 "${overStockItem.productName}" 的调拨数量超过可用库存`)
    return
  }

  addSubmitting.value = true
  try {
    const payload = {
      fromWarehouseId: addForm.fromWarehouseId,
      toWarehouseId: addForm.toWarehouseId,
      transferDate: addForm.transferDate.format('YYYY-MM-DD'),
      transferType: addForm.transferType,
      reason: addForm.reason,
      items: addForm.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    }
    await stockTransferApi.create(payload)
    message.success('调拨单创建成功')
    addVisible.value = false
    fetchData()
  } catch (err: any) {
    message.error(err?.message || '创建失败')
  } finally {
    addSubmitting.value = false
  }
}

const handleAddCancel = () => {
  addVisible.value = false
}

const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

// ── 审批 ──────────────────────────────────────────────
const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定要审批通过调拨单 "${record.transferNo}" 吗？审批通过后将自动生成对应的出库单和入库单。`,
    okText: '确认通过',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await stockTransferApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch (err: any) {
        message.error(err?.message || '审批失败')
      }
    }
  })
}

// ── 导出 ──────────────────────────────────────────────
const handleExport = async () => {
  const hide = message.loading('正在导出调拨数据...', 0)
  try {
    const blob = await fetch('/api/erp/stock/transfer/export', {
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
    link.download = `调拨单报表_${dayjs().format('YYYY-MM-DD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    hide()
    message.success('导出成功，文件下载中')
  } catch (err: any) {
    hide()
    message.error(err?.message || '导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>
