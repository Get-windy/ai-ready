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
        新建收款
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
          <template v-else-if="column.key === 'receiptAmount'">
            ¥{{ record.receiptAmount?.toFixed(2) }}
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
    title="收款单详情"
    width="700px"
    :footer="null"
  >
    <a-descriptions bordered :column="2" v-if="currentRecord">
      <a-descriptions-item label="收款单号">{{ currentRecord.receiptNo }}</a-descriptions-item>
      <a-descriptions-item label="销售订单">{{ currentRecord.orderNo }}</a-descriptions-item>
      <a-descriptions-item label="客户">{{ currentRecord.customerName }}</a-descriptions-item>
      <a-descriptions-item label="收款日期">{{ currentRecord.receiptDate }}</a-descriptions-item>
      <a-descriptions-item label="收款金额">¥{{ currentRecord.receiptAmount?.toFixed(2) }}</a-descriptions-item>
      <a-descriptions-item label="收款方式">{{ currentRecord.receiptMethod }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ currentRecord.createTime }}</a-descriptions-item>
      <a-descriptions-item label="收款账户">{{ currentRecord.bankAccount || '-' }}</a-descriptions-item>
      <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
    </a-descriptions>
    <div style="text-align: right; margin-top: 16px">
      <a-button @click="detailVisible = false">关闭</a-button>
    </div>
  </a-modal>

  <!-- 新建收款弹窗 -->
  <a-modal
    v-model:open="receiptFormVisible"
    title="新建收款单"
    width="600px"
    centered
    :confirm-loading="receiptFormSubmitting"
    ok-text="确认创建"
    cancel-text="取消"
    @ok="handleReceiptFormSubmit"
    @cancel="receiptFormVisible = false"
  >
    <a-form
      ref="receiptFormRef"
      :model="receiptFormData"
      :rules="receiptFormRules"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 19 }"
    >
      <a-form-item label="销售订单" name="orderNo">
        <a-input v-model:value="receiptFormData.orderNo" placeholder="请输入销售订单号" />
      </a-form-item>
      <a-form-item label="客户" name="customerName">
        <a-input v-model:value="receiptFormData.customerName" placeholder="请输入客户名称" />
      </a-form-item>
      <a-form-item label="收款金额" name="receiptAmount">
        <a-input-number v-model:value="receiptFormData.receiptAmount" :min="0" :precision="2" style="width: 100%" prefix="¥" placeholder="请输入收款金额" />
      </a-form-item>
      <a-row>
        <a-col :span="12">
          <a-form-item label="收款方式" name="receiptMethod" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-select v-model:value="receiptFormData.receiptMethod" placeholder="请选择收款方式">
              <a-select-option value="银行转账">银行转账</a-select-option>
              <a-select-option value="现金">现金</a-select-option>
              <a-select-option value="微信">微信</a-select-option>
              <a-select-option value="支付宝">支付宝</a-select-option>
              <a-select-option value="支票">支票</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收款日期" name="receiptDate" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <a-date-picker v-model:value="receiptFormData.receiptDate" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="收款账户" name="bankAccount">
        <a-input v-model:value="receiptFormData.bankAccount" placeholder="请输入收款账户" />
      </a-form-item>
      <a-form-item label="备注" name="remark">
        <a-textarea v-model:value="receiptFormData.remark" placeholder="请输入备注" :rows="2" />
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
  { text: '收款单' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const columns = [
  { title: '收款单号', dataIndex: 'receiptNo', key: 'receiptNo', width: 180 },
  { title: '销售订单', dataIndex: 'orderNo', key: 'orderNo', width: 180 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '收款日期', dataIndex: 'receiptDate', key: 'receiptDate', width: 120 },
  { title: '收款金额', dataIndex: 'receiptAmount', key: 'receiptAmount', width: 120 },
  { title: '收款方式', dataIndex: 'receiptMethod', key: 'receiptMethod', width: 100 },
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
    3: 'blue'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已收款',
    3: '部分收款'
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

// ========== 收款表单弹窗 ==========
const receiptFormVisible = ref(false)
const receiptFormSubmitting = ref(false)
const receiptFormRef = ref<FormInstance>()
const receiptFormData = reactive({
  orderNo: '',
  customerName: '',
  receiptAmount: 0,
  receiptMethod: undefined as string | undefined,
  receiptDate: undefined as any,
  bankAccount: '',
  remark: ''
})

const receiptFormRules = {
  orderNo: [{ required: true, message: '请输入销售订单号', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  receiptAmount: [{ required: true, type: 'number' as const, message: '请输入收款金额', trigger: 'blur' }],
  receiptMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }]
}

const handleAdd = () => {
  receiptFormData.orderNo = ''
  receiptFormData.customerName = ''
  receiptFormData.receiptAmount = 0
  receiptFormData.receiptMethod = undefined
  receiptFormData.receiptDate = undefined
  receiptFormData.bankAccount = ''
  receiptFormData.remark = ''
  receiptFormVisible.value = true
}

const handleView = (record: any) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleReceiptFormSubmit = async () => {
  try { await receiptFormRef.value?.validate() } catch { return }
  receiptFormSubmitting.value = true
  try {
    // await receiptApi.save(receiptFormData)
    message.success('新建收款单成功')
    receiptFormVisible.value = false
    fetchData()
  } catch {
    message.error('新建收款单失败')
  } finally {
    receiptFormSubmitting.value = false
  }
}

const handleApprove = (record: any) => {
  Modal.confirm({
    title: '确认审批',
    content: `确定审批通过收款单 "${record.receiptNo}" 吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        // await receiptApi.approve(record.id)
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
    // await receiptApi.export(pagination.current, pagination.pageSize)
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
