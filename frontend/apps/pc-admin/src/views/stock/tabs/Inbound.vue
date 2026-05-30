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
        新建入库
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
    title="入库单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="入库单号">{{ currentRecord.inboundNo }}</a-descriptions-item>
      <a-descriptions-item label="入库类型">{{ currentRecord.inboundType }}</a-descriptions-item>
      <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
      <a-descriptions-item label="入库日期">{{ currentRecord.inboundDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="来源单号">{{ currentRecord.sourceNo || '-' }}</a-descriptions-item>
      <a-descriptions-item label="经手人">{{ currentRecord.handlerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建入库弹窗 -->
  <a-modal
    v-model:open="addVisible"
    title="新建入库单"
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
          <a-form-item label="采购订单" name="orderNo">
            <a-select
              v-model:value="addForm.orderNo"
              show-search
              placeholder="请选择采购订单"
              :options="purchaseOrderOptions"
              :filter-option="filterPurchaseOrder"
              allow-clear
              @change="handleOrderChange"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="入库类型" name="inboundType">
            <a-select
              v-model:value="addForm.inboundType"
              placeholder="请选择入库类型"
              :options="inboundTypeOptions"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="入库仓库" name="warehouseId">
            <a-select
              v-model:value="addForm.warehouseId"
              placeholder="请选择仓库"
              :options="warehouseOptions"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="预计到货日期" name="expectedDate">
            <a-date-picker
              v-model:value="addForm.expectedDate"
              style="width: 100%"
              placeholder="请选择预计到货日期"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="addForm.remark" :rows="2" placeholder="请输入备注" />
      </a-form-item>
    </a-form>

    <a-divider style="margin: 12px 0">入库明细</a-divider>

    <div style="margin-bottom: 12px">
      <a-button type="dashed" size="small" @click="addInboundItem">
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
        <template v-if="column.key === 'isNew'">
          <a-tag v-if="record.isNew" color="green">新增</a-tag>
          <span v-else>-</span>
        </template>
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
        <template v-else-if="column.key === 'expectedQty'">
          <a-input-number
            v-model:value="addForm.items[index].expectedQty"
            :min="1"
            style="width: 100%"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'actualQty'">
          <a-input-number
            v-if="record.isNew"
            v-model:value="addForm.items[index].actualQty"
            :min="1"
            style="width: 100%"
            size="small"
          />
          <a-input-number
            v-else
            v-model:value="addForm.items[index].actualQty"
            :min="0"
            style="width: 100%"
            size="small"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-button type="link" danger size="small" @click="removeInboundItem(index)">删除</a-button>
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
import { inboundApi } from '@/api/erp'
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
  { text: '入库管理' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '入库单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 180 },
  { title: '入库类型', dataIndex: 'inboundType', key: 'inboundType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 120 },
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
    2: '已入库'
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

// ── 新建入库 ──────────────────────────────────────────
const addVisible = ref(false)
const addSubmitting = ref(false)
const addFormRef = ref<FormInstance>()

interface AddInboundItem {
  key: number
  productId: number | undefined
  productName: string
  isNew: boolean
  expectedQty: number
  actualQty: number
}

const addForm = reactive({
  orderNo: undefined as string | undefined,
  inboundType: 1,
  warehouseId: undefined as number | undefined,
  expectedDate: dayjs(),
  remark: '',
  items: [] as AddInboundItem[]
})

const addFormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  expectedDate: [{ required: true, message: '请选择预计到货日期', trigger: 'change' }]
}

let itemKeyCounter = 0

const inboundTypeOptions = [
  { value: 1, label: '采购入库' },
  { value: 2, label: '退货入库' },
  { value: 3, label: '调拨入库' },
  { value: 4, label: '其他入库' }
]

const warehouseOptions = [
  { value: 1, label: '主仓库' },
  { value: 2, label: '备品仓库' },
  { value: 3, label: '半成品仓库' },
  { value: 4, label: '成品仓库' }
]

const purchaseOrderOptions = [
  { value: 'PO-2024-001', label: 'PO-2024-001 / 供应商A' },
  { value: 'PO-2024-002', label: 'PO-2024-002 / 供应商B' },
  { value: 'PO-2024-003', label: 'PO-2024-003 / 供应商C' }
]

const productOptions = [
  { value: 1, label: 'PROD-001 螺丝螺母套装' },
  { value: 2, label: 'PROD-002 不锈钢板材' },
  { value: 3, label: 'PROD-003 电子元件A型' },
  { value: 4, label: 'PROD-004 包装箱(大)' }
]

const addItemColumns = [
  { title: '来源', key: 'isNew', width: 60 },
  { title: '产品名称', key: 'productName' },
  { title: '预计数量', key: 'expectedQty', width: 100 },
  { title: '实收数量', key: 'actualQty', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const filterPurchaseOrder = (input: string, option: any) => {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

const handleOrderChange = (value: string | undefined) => {
  if (value) {
    // 根据采购订单自动填充明细
    addForm.items = [
      { key: itemKeyCounter++, productId: 1, productName: 'PROD-001', isNew: false, expectedQty: 100, actualQty: 0 },
      { key: itemKeyCounter++, productId: 2, productName: 'PROD-002', isNew: false, expectedQty: 50, actualQty: 0 }
    ]
  }
}

const handleItemProductChange = (index: number, productId: number) => {
  const product = productOptions.find(p => p.value === productId)
  if (product) {
    addForm.items[index].productName = product.label
  }
}

const addInboundItem = () => {
  addForm.items.push({
    key: itemKeyCounter++,
    productId: undefined,
    productName: '',
    isNew: true,
    expectedQty: 1,
    actualQty: 0
  })
}

const removeInboundItem = (index: number) => {
  addForm.items.splice(index, 1)
}

const handleAdd = () => {
  addForm.orderNo = undefined
  addForm.inboundType = 1
  addForm.warehouseId = undefined
  addForm.expectedDate = dayjs()
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
    message.warning('请至少添加一条入库明细')
    return
  }

  addSubmitting.value = true
  try {
    const payload = {
      orderNo: addForm.orderNo,
      inboundType: addForm.inboundType,
      warehouseId: addForm.warehouseId,
      expectedDate: addForm.expectedDate.format('YYYY-MM-DD'),
      remark: addForm.remark,
      items: addForm.items.map(item => ({
        productId: item.productId,
        expectedQty: item.expectedQty,
        actualQty: item.actualQty
      }))
    }
    await inboundApi.create(payload)
    message.success('入库单创建成功')
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
    content: `确定要审批通过入库单 "${record.inboundNo}" 吗？`,
    okText: '确认通过',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await inboundApi.approve(record.id)
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
    const blob = await fetch('/api/erp/purchase/inbound/export', {
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
    link.download = `入库单报表_${dayjs().format('YYYY-MM-DD_HHmmss')}.xlsx`
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
