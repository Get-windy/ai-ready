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
        新建订单
      </a-button>
      <a-button @click="handleExport">
        <template #icon><ExportOutlined /></template>
        导出
      </a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchApprove">批量审批</a-button>
      <a-button @click="handleBatchPrint">批量打印</a-button>
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
          <template v-else-if="column.key === 'totalAmountWithTax'">
            ¥{{ record.totalAmountWithTax?.toFixed(2) }}
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

  <!-- 新建/编辑订单弹窗 -->
  <a-modal
    v-model:open="formVisible"
    :title="isEdit ? '编辑销售订单' : '新建销售订单'"
    width="800px"
    :confirm-loading="formSubmitting"
    @ok="handleFormSubmit"
    @cancel="handleFormCancel"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="formState.customerName" placeholder="请输入客户名称" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="订单日期" name="orderDate" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
            <a-date-picker v-model:value="formState.orderDate" style="width: 100%" placeholder="选择日期" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="交货日期" name="deliveryDate" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
            <a-date-picker v-model:value="formState.deliveryDate" style="width: 100%" placeholder="选择日期" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="销售员" name="salesmanName">
        <a-input v-model:value="formState.salesmanName" placeholder="请输入销售员" />
      </a-form-item>
      <a-form-item label="产品明细" required>
        <div style="margin-bottom: 8px">
          <a-button type="dashed" size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>
            添加产品
          </a-button>
        </div>
        <a-table
          :data-source="formState.items"
          :pagination="false"
          row-key="key"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'productName'">
              <a-input v-model:value="record.productName" placeholder="产品名称" size="small" />
            </template>
            <template v-else-if="column.key === 'quantity'">
              <a-input-number v-model:value="record.quantity" :min="1" size="small" style="width: 100%" />
            </template>
            <template v-else-if="column.key === 'unitPrice'">
              <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" size="small" style="width: 100%" prefix="¥" />
            </template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ (record.quantity * record.unitPrice).toFixed(2) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formState.items.length <= 1">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formState.remark" :rows="3" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import type { Dayjs } from 'dayjs'

const router = useRouter()

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '销售订单' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 120 },
  { title: '订单金额', dataIndex: 'totalAmountWithTax', key: 'totalAmountWithTax', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '销售员', dataIndex: 'salesmanName', key: 'salesmanName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 }
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
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '部分出库',
    4: '完成',
    5: '已取消'
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

// ========== 订单表单弹窗 ==========
const formVisible = ref(false)
const formSubmitting = ref(false)
const isEdit = ref(false)
const editRecordId = ref<number | null>(null)
const formRef = ref()

interface OrderItem {
  key: number
  productName: string
  quantity: number
  unitPrice: number
}

interface OrderFormState {
  customerName: string
  orderDate: Dayjs | undefined
  deliveryDate: Dayjs | undefined
  salesmanName: string
  items: OrderItem[]
  remark: string
}

const defaultItem = (): OrderItem => ({
  key: Date.now(),
  productName: '',
  quantity: 1,
  unitPrice: 0
})

const formState = reactive<OrderFormState>({
  customerName: '',
  orderDate: undefined,
  deliveryDate: undefined,
  salesmanName: '',
  items: [defaultItem()],
  remark: ''
})

const formRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change', type: 'object' as const }],
  salesmanName: [{ required: true, message: '请输入销售员', trigger: 'blur' }]
}

const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', dataIndex: 'unitPrice', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

const addItem = () => {
  formState.items.push(defaultItem())
}

const removeItem = (index: number) => {
  if (formState.items.length > 1) {
    formState.items.splice(index, 1)
  }
}

const resetForm = () => {
  formState.customerName = ''
  formState.orderDate = undefined
  formState.deliveryDate = undefined
  formState.salesmanName = ''
  formState.items = [defaultItem()]
  formState.remark = ''
  editRecordId.value = null
  isEdit.value = false
  formRef.value?.clearValidate()
}

const handleAdd = () => {
  resetForm()
  formVisible.value = true
}

const handleView = (record: any) => {
  router.push(`/sale/order/${record.id}`)
}

const handleEdit = (record: any) => {
  resetForm()
  isEdit.value = true
  editRecordId.value = record.id
  formState.customerName = record.customerName || ''
  formState.salesmanName = record.salesmanName || ''
  formState.remark = record.remark || ''
  formState.items = record.items?.length
    ? record.items.map((item: any, idx: number) => ({
        key: idx,
        productName: item.productName || '',
        quantity: item.quantity || 1,
        unitPrice: item.unitPrice || 0
      }))
    : [defaultItem()]
  formVisible.value = true
}

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  formSubmitting.value = true
  try {
    // await saleOrderApi.save({ ...formState, id: editRecordId.value })
    message.success(isEdit.value ? '编辑成功' : '创建成功')
    formVisible.value = false
    fetchData()
  } catch {
    message.error(isEdit.value ? '编辑失败' : '创建失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleFormCancel = () => {
  formVisible.value = false
}

const handleSubmit = (record: any) => {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交订单 "${record.orderNo}" 吗？提交后将进入审批流程。`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await saleOrderApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch {
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批通过订单 "${record.orderNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await saleOrderApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch {
        message.error('审批失败')
      }
    }
  })
}

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    // await saleOrderApi.export(pagination.current, pagination.pageSize)
    await new Promise(resolve => setTimeout(resolve, 1000))
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}

const handleBatchApprove = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要审批的订单')
    return
  }
  Modal.confirm({
    title: '确认批量审批',
    content: `确定要批量审批选中的 ${selectedRowKeys.value.length} 条记录吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await saleOrderApi.batchApprove(selectedRowKeys.value)
        message.success(`成功审批 ${selectedRowKeys.value.length} 条记录`)
        selectedRowKeys.value = []
        fetchData()
      } catch { message.error('批量审批失败') }
    }
  })
}

const handleBatchPrint = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要打印的订单')
    return
  }
  Modal.confirm({
    title: '确认批量打印',
    content: `确定要批量打印选中的 ${selectedRowKeys.value.length} 条记录吗？`,
    okText: '确认打印',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await saleOrderApi.batchPrint(selectedRowKeys.value)
        message.success(`成功打印 ${selectedRowKeys.value.length} 条记录`)
      } catch { message.error('批量打印失败') }
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>
