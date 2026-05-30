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
        新建报价
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
              <a v-if="record.status === 0" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 0" @click="handleSend(record)">发送</a>
              <a v-if="record.status === 1" @click="handleConvert(record)">转订单</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>
  </ModuleLayout>

  <a-modal
    v-model:open="detailVisible"
    title="报价单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="报价单号">{{ currentRecord.quotationNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="报价日期">{{ currentRecord.quotationDate }}</a-descriptions-item>
      <a-descriptions-item label="有效期">{{ currentRecord.validDate }}</a-descriptions-item>
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

  <!-- 新建/编辑报价弹窗 -->
  <a-modal
    v-model:open="formVisible"
    :title="isEdit ? '编辑报价单' : '新建报价单'"
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
          <a-form-item label="报价日期" name="quotationDate" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
            <a-date-picker v-model:value="formState.quotationDate" style="width: 100%" placeholder="选择日期" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="有效期至" name="validUntil" :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
            <a-date-picker v-model:value="formState.validUntil" style="width: 100%" placeholder="选择日期" />
          </a-form-item>
        </a-col>
      </a-row>
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
          :columns="itemColumns"
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
            <template v-else-if="column.key === 'discount'">
              <a-input-number v-model:value="record.discount" :min="0" :max="100" size="small" style="width: 100%" />%
            </template>
            <template v-else-if="column.key === 'amount'">
              ¥{{ (record.quantity * record.unitPrice * (1 - record.discount / 100)).toFixed(2) }}
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
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '报价单' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '报价单号', dataIndex: 'quotationNo', key: 'quotationNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '报价日期', dataIndex: 'quotationDate', key: 'quotationDate', width: 120 },
  { title: '有效期', dataIndex: 'validDate', key: 'validDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
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
    3: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '已发送',
    2: '已接受',
    3: '已拒绝'
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

// ========== 报价表单弹窗 ==========
const formVisible = ref(false)
const formSubmitting = ref(false)
const isEdit = ref(false)
const editRecordId = ref<number | null>(null)
const formRef = ref()

interface QuotationItem {
  key: number
  productName: string
  quantity: number
  unitPrice: number
  discount: number
}

interface QuotationFormState {
  customerName: string
  quotationDate: Dayjs | undefined
  validUntil: Dayjs | undefined
  items: QuotationItem[]
  remark: string
}

const defaultItem = (): QuotationItem => ({
  key: Date.now() + Math.random(),
  productName: '',
  quantity: 1,
  unitPrice: 0,
  discount: 0
})

const formState = reactive<QuotationFormState>({
  customerName: '',
  quotationDate: undefined,
  validUntil: undefined,
  items: [defaultItem()],
  remark: ''
})

const formRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  quotationDate: [{ required: true, message: '请选择报价日期', trigger: 'change', type: 'object' as const }],
  validUntil: [{ required: true, message: '请选择有效期', trigger: 'change', type: 'object' as const }]
}

const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 80 },
  { title: '单价', key: 'unitPrice', dataIndex: 'unitPrice', width: 120 },
  { title: '折扣(%)', key: 'discount', dataIndex: 'discount', width: 100 },
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
  formState.quotationDate = undefined
  formState.validUntil = undefined
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
  currentRecord.value = record
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  resetForm()
  isEdit.value = true
  editRecordId.value = record.id
  formState.customerName = record.customerName || ''
  formState.remark = record.remark || ''
  formState.items = record.items?.length
    ? record.items.map((item: any, idx: number) => ({
        key: idx,
        productName: item.productName || '',
        quantity: item.quantity || 1,
        unitPrice: item.unitPrice || 0,
        discount: item.discount || 0
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
    // await quotationApi.save({ ...formState, id: editRecordId.value })
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

const handleSend = (record: any) => {
  Modal.confirm({
    title: '确认发送',
    content: `确定要发送报价单 "${record.quotationNo}" 给客户吗？`,
    okText: '确认发送',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await quotationApi.send(record.id)
        message.success('发送成功')
        fetchData()
      } catch {
        message.error('发送失败')
      }
    }
  })
}

const handleConvert = (record: any) => {
  Modal.confirm({
    title: '确认转订单',
    content: `确定要将报价 "${record.quotationNo}" 转为销售订单吗？将创建新的销售订单。`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // const order = await quotationApi.convertToOrder(record.id)
        message.success('转订单成功')
        router.push(`/sale/order/new`)
      } catch {
        message.error('转换失败')
      }
    }
  })
}

const handleExport = async () => {
  const hide = message.loading('正在导出...', 0)
  try {
    // await quotationApi.export(pagination.current, pagination.pageSize)
    await new Promise(resolve => setTimeout(resolve, 1000))
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}

onMounted(() => {
  fetchData()
})
</script>
