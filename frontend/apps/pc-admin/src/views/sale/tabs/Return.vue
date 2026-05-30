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
        新建退货
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
    title="退货单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="退货单号">{{ currentRecord.returnNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="退货日期">{{ currentRecord.returnDate }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="退货原因" :span="2">{{ currentRecord.reason || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建退货弹窗 -->
  <a-modal
    v-model:open="formModalVisible"
    title="新建退货单"
    width="800px"
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
      <a-form-item label="销售订单" name="orderNo">
        <a-input v-model:value="formData.orderNo" placeholder="请输入销售订单号" />
      </a-form-item>
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="formData.customerName" placeholder="请输入客户名称" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="退货原因" name="reason" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="formData.reason" placeholder="请选择退货原因">
              <a-select-option value="质量问题">质量问题</a-select-option>
              <a-select-option value="规格不符">规格不符</a-select-option>
              <a-select-option value="数量错误">数量错误</a-select-option>
              <a-select-option value="客户取消">客户取消</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="退货日期" name="returnDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="formData.returnDate" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="退款金额" name="refundAmount">
        <a-input-number v-model:value="formData.refundAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入退款金额" />
      </a-form-item>
      <a-form-item label="退货明细" required>
        <div style="margin-bottom: 8px">
          <a-button type="dashed" size="small" @click="addItem">
            <template #icon><PlusOutlined /></template>
            添加产品
          </a-button>
        </div>
        <a-table
          :data-source="formData.items"
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
            <template v-else-if="column.key === 'amount'">
              ¥{{ (record.quantity * record.unitPrice).toFixed(2) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" danger size="small" @click="removeItem(index)" :disabled="formData.items.length <= 1">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'

const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])
const currentView = ref('list')
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '退货单' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '退货单号', dataIndex: 'returnNo', key: 'returnNo', width: 180 },
  { title: '销售订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '退货日期', dataIndex: 'returnDate', key: 'returnDate', width: 120 },
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
    2: 'green',
    3: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已退货',
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

// ========== 退货表单弹窗 ==========
interface ReturnItem {
  key: number
  productName: string
  quantity: number
  unitPrice: number
}

const formModalVisible = ref(false)
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<{
  orderNo: string
  customerName: string
  reason: string | undefined
  returnDate: any
  refundAmount: number
  items: ReturnItem[]
  remark: string
}>({
  orderNo: '',
  customerName: '',
  reason: undefined,
  returnDate: undefined,
  refundAmount: 0,
  items: [],
  remark: ''
})

const defaultItem = (): ReturnItem => ({
  key: Date.now() + Math.random(),
  productName: '',
  quantity: 1,
  unitPrice: 0
})

const formRules = {
  orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  reason: [{ required: true, message: '请选择退货原因', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }]
}

const itemColumns = [
  { title: '产品名称', key: 'productName', dataIndex: 'productName' },
  { title: '数量', key: 'quantity', dataIndex: 'quantity', width: 100 },
  { title: '单价', key: 'unitPrice', dataIndex: 'unitPrice', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 }
]

const addItem = () => {
  formData.items.push(defaultItem())
}

const removeItem = (index: number) => {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  }
}

const handleAdd = () => {
  formData.orderNo = ''
  formData.customerName = ''
  formData.reason = undefined
  formData.returnDate = undefined
  formData.refundAmount = 0
  formData.items = [defaultItem()]
  formData.remark = ''
  formModalVisible.value = true
}

const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleFormSubmit = async () => {
  try { await formRef.value?.validate() } catch { return }
  formSubmitting.value = true
  try {
    // await returnApi.save(formData)
    message.success('新建退货单成功')
    formModalVisible.value = false
    fetchData()
  } catch {
    message.error('新建退货单失败')
  } finally {
    formSubmitting.value = false
  }
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批通过退货单 "${record.returnNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await returnApi.approve(record.id)
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
    // await returnApi.export(pagination.current, pagination.pageSize)
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
