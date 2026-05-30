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
        新建出库
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
    title="出库单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="出库单号">{{ currentRecord.outboundNo }}</a-descriptions-item>
      <a-descriptions-item label="出库类型">{{ currentRecord.outboundType }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="出库日期">{{ currentRecord.outboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="目标单号">{{ currentRecord.targetNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建出库弹窗 -->
  <a-modal
    v-model:open="addVisible"
    title="新建出库单"
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
          <a-form-item label="销售订单" name="orderNo">
            <a-select
              v-model:value="addForm.orderNo"
              show-search
              placeholder="请选择销售订单"
              :options="salesOrderOptions"
              :filter-option="filterSalesOrder"
              allow-clear
              @change="handleOrderChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出库类型" name="outboundType">
            <a-select
              v-model:value="addForm.outboundType"
              placeholder="请选择出库类型"
              :options="outboundTypeOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="出库仓库" name="warehouseId">
            <a-select
              v-model:value="addForm.warehouseId"
              placeholder="请选择仓库"
              :options="warehouseOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="出库日期" name="outboundDate">
            <a-date-picker
              v-model:value="addForm.outboundDate"
              style="width: 100%"
              placeholder="请选择出库日期"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="承运商" name="carrier">
            <a-input v-model:value="addForm.carrier" placeholder="请输入承运商名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="运单号" name="trackingNo">
            <a-input v-model:value="addForm.trackingNo" placeholder="请输入运单号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>

    <a-divider style="margin: 12px 0">出库明细</a-divider>

    <div style="margin-bottom: 12px">
      <a-button type="dashed" size="small" @click="addOutboundItem">
        <template #icon><PlusOutlined /></template>
        添加明细
      </a-button>
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
          <a-tag :color="getAvailableQtyColor(index)">{{ getAvailableQty(index) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'quantity'">
          <a-input-number
            v-model:value="addForm.items[index].quantity"
            :min="1"
            style="width: 100%"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'unitPrice'">
          <a-input-number
            v-model:value="addForm.items[index].unitPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="removeOutboundItem(index)">删除</a-button>
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
import { outboundApi } from '@/api/erp'
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
  { text: '出库管理' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '出库单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 180 },
  { title: '出库类型', dataIndex: 'outboundType', key: 'outboundType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 120 },
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
    2: '已出库'
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

// ── 新建出库 ──────────────────────────────────────────
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()

interface AddOutboundItem {
  key: number
  productId: number | undefined
  productName: string
  availableQty: number
  quantity: number
  unitPrice: number
}

const addForm = reactive({
  orderNo: undefined as string | undefined,
  outboundType: 1,
  warehouseId: undefined as number | undefined,
  outboundDate: dayjs(),
  carrier: '',
  trackingNo: '',
  remark: '',
  items: [] as AddOutboundItem[]
})

const addFormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }]
}

let itemKeyCounter = 0

const outboundTypeOptions = [
  { value: 1, label: '销售出库' },
  { value: 2, label: '退货出库' },
  { value: 3, label: '调拨出库' },
  { value: 4, label: '其他出库' }
]

const warehouseOptions = [
  { value: 1, label: '主仓库' },
  { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' },
  { value: 4, label: '成品仓库' }
]

const salesOrderOptions = [
  { value: 'SO-2024-001', label: 'SO-2024-001 / 客户A' },
  { value: 'SO-2024-002', label: 'SO-2024-002 / 客户B' },
  { value: 'SO-2024-003', label: 'SO-2024-003 / 客户C' }
]

const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' },
  { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' },
  { value: 4, label: 'PROD-004 包装箱(大)' }
]

const addItemColumns = [
  { title: '产品名称', key: 'productName' },
  { title: '可用库存', key: 'availableQty', width: 90 },
  { title: '出库数量', key: 'quantity', width: 110 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const filterSalesOrder = (input: string, option: any) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

const handleOrderChange = (value: string | undefined) => {
  if (value) {
    addForm.items = [
      { key: itemKeyCounter++, productId: 1, productName: 'PROD-001', availableQty: 500, quantity: 10, unitPrice: 12.50 },
      { key: itemKeyCounter++, productId: 2, productName: 'PROD-002', availableQty: 200, quantity: 5, unitPrice: 85.00 }
    ]
  }
}

const handleItemProductChange = (index: number, productId: number) => {
  const product = productOptions.find(p => p.value === productId)
  if (product) {
    addForm.items[index].productName = product.label
    // 模拟设置可用库存
    const stockMap: Record<number, number> = { 1: 500, 2: 200, 3: 1000, 4: 300 }
    addForm.items[index].availableQty = stockMap[productId] || 0
  }
}

const getAvailableQty = (index: number) => {
  const item = addForm.items[index]
  return item.availableQty
}

const getAvailableQtyColor = (index: number) => {
  const item = addForm.items[index]
  if (item.quantity > item.availableQty) return 'red'
  if (item.availableQty < 50) return 'orange'
  return 'green'
}

const addOutboundItem = () => {
  addForm.items.push({
    key: itemKeyCounter++,
    productId: undefined,
    productName: '',
    availableQty: 0,
    quantity: 1,
    unitPrice: 0
  })
}

const removeOutboundItem = (index: number) => {
  addForm.items.splice(index, 1)
}

const handleAdd = () => {
  addForm.orderNo = undefined
  addForm.outboundType = 1
  addForm.warehouseId = undefined
  addForm.outboundDate = dayjs()
  addForm.carrier = ''
  addForm.trackingNo = ''
  addForm.remark = ''
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
    message.warning('请至少添加一条出库明细')
    return
  }

  // 校验出库数量不超过可用库存
  const overStockItem = addForm.items.find(item => item.quantity > item.availableQty)
  if (overStockItem) {
    message.warning(`产品 "${overStockItem.productName}" 的出库数量超过可用库存`)
    return
  }

  addSubmitting.value = true
  try {
    const payload = {
      orderNo: addForm.orderNo,
      outboundType: addForm.outboundType,
      warehouseId: addForm.warehouseId,
      outboundDate: addForm.outboundDate.format('YYYY-MM-DD'),
      carrier: addForm.carrier,
      trackingNo: addForm.trackingNo,
      remark: addForm.remark,
      items: addForm.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.unitPrice
      }))
    }
    await outboundApi.create(payload)
    message.success('出库单创建成功')
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
    content: `确定要审批通过出库单 "${record.outboundNo}" 吗？`,
    okText: '确认通过',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await outboundApi.approve(record.id)
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
  try {
    const blob = await fetch('/api/erp/sale/outbound/export', {
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
    link.download = `出库单报表_${dayjs().format('YYYY-MM-DD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err: any) {
    message.error(err?.message || '导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>
