<template>
  <div class="price-approval-page">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>价格审批管理</h2>
        <a-button type="primary" @click="handleApply">
          <template #icon><PlusOutlined /></template>
          申请价格变更
        </a-button>
      </div>

      <a-row :gutter="16" class="summary-row">
        <a-col :span="6">
          <a-statistic title="待审批" :value="statistics.pendingCount" :value-style="{ color: '#faad14' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已通过" :value="statistics.approvedCount" :value-style="{ color: '#3f8600' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已拒绝" :value="statistics.rejectedCount" :value-style="{ color: '#f5222d' }" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="总申请数" :value="statistics.totalCount" />
        </a-col>
      </a-row>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="pending" tab="待审批">
          <a-table
            :columns="columns"
            :data-source="pendingList"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'priceChange'">
                <div class="price-change">
                  <span class="old-price">原价: ¥{{ record.oldPrice }}</span>
                  <span class="new-price">新价: ¥{{ record.newPrice }}</span>
                  <span class="change" :class="record.priceChangeType">
                    {{ record.priceChangeType === 'increase' ? '+' : '-' }}¥{{ record.priceChange }}
                  </span>
                </div>
              </template>
              <template v-if="column.key === 'status'">
                <a-tag color="orange">待审批</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button size="small" type="primary" @click="handleApprove(record)">通过</a-button>
                  <a-button size="small" danger @click="handleReject(record)">拒绝</a-button>
                  <a @click="handleView(record)">详情</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="approved" tab="已通过">
          <a-table
            :columns="processedColumns"
            :data-source="approvedList"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag color="green">已通过</a-tag>
              </template>
              <template v-if="column.key === 'approver'">
                {{ record.approverName }} / {{ formatDate(record.approveTime) }}
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="rejected" tab="已拒绝">
          <a-table
            :columns="processedColumns"
            :data-source="rejectedList"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template v-if="column.key === 'status'">
              <a-tag color="red">已拒绝</a-tag>
            </template>
            <template v-if="column.key === 'approver'">
              {{ record.approverName }} / {{ formatDate(record.approveTime) }}
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="my" tab="我的申请">
          <a-table
            :columns="myColumns"
            :data-source="myList"
            :loading="loading"
            :pagination="false"
            row-key="id"
          >
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="applyVisible"
      title="申请价格变更"
      width="600px"
      :confirm-loading="submitLoading"
      @ok="handleApplySubmit"
      @cancel="applyVisible = false"
    >
      <a-form
        ref="applyFormRef"
        :model="applyForm"
        :rules="applyRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="产品" name="productId">
          <a-select
            v-model:value="applyForm.productId"
            placeholder="请选择产品"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="p in productList" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.code }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="客户" name="customerId">
          <a-select
            v-model:value="applyForm.customerId"
            placeholder="请选择客户(可选，不选则为通用价格)"
            show-search
            :filter-option="filterOption"
            allow-clear
          >
            <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">
              {{ c.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="当前价格">
          <span class="current-price">¥{{ currentPrice }}</span>
        </a-form-item>
        <a-form-item label="新价格" name="newPrice">
          <a-input-number v-model:value="applyForm.newPrice" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="价格变动">
          <span :class="{ increase: priceChangeType === 'increase', decrease: priceChangeType === 'decrease' }">
            {{ priceChangeType === 'increase' ? '+' : '-' }}¥{{ priceChangeAmount }}
          </span>
        </a-form-item>
        <a-form-item label="审批类型" name="approvalType">
          <a-select v-model:value="applyForm.approvalType" placeholder="请选择审批类型">
            <a-select-option value="price_adjustment">价格调整</a-select-option>
            <a-select-option value="promotion">促销价格</a-select-option>
            <a-select-option value="contract">合同价格</a-select-option>
            <a-select-option value="discount">折扣价格</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生效时间">
          <a-range-picker v-model:value="applyForm.effectiveRange" show-time />
        </a-form-item>
        <a-form-item label="申请原因" name="approvalReason">
          <a-textarea v-model:value="applyForm.approvalReason" placeholder="请输入申请原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="approveVisible"
      title="审批通过"
      width="500px"
      @ok="handleApproveConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品">
          <span>{{ approveData.productName }}</span>
        </a-form-item>
        <a-form-item label="价格变更">
          <span>¥{{ approveData.oldPrice }} → ¥{{ approveData.newPrice }}</span>
        </a-form-item>
        <a-form-item label="审批备注">
          <a-textarea v-model:value="approveRemark" placeholder="请输入审批备注(可选)" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="rejectVisible"
      title="审批拒绝"
      width="500px"
      @ok="handleRejectConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="产品">
          <span>{{ rejectData.productName }}</span>
        </a-form-item>
        <a-form-item label="价格变更">
          <span>¥{{ rejectData.oldPrice }} → ¥{{ rejectData.newPrice }}</span>
        </a-form-item>
        <a-form-item label="拒绝原因">
          <a-textarea v-model:value="rejectReason" placeholder="请输入拒绝原因" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      title="审批详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="产品名称">{{ detailData.productName }}</a-descriptions-item>
        <a-descriptions-item label="产品编码">{{ detailData.productCode }}</a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ detailData.customerName || '通用价格' }}</a-descriptions-item>
        <a-descriptions-item label="审批类型">{{ detailData.approvalTypeLabel }}</a-descriptions-item>
        <a-descriptions-item label="原价格">¥{{ detailData.oldPrice }}</a-descriptions-item>
        <a-descriptions-item label="新价格">¥{{ detailData.newPrice }}</a-descriptions-item>
        <a-descriptions-item label="价格变动">
          <span :class="detailData.priceChangeType">{{ detailData.priceChangeType === 'increase' ? '+' : '-' }}¥{{ detailData.priceChange }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="审批状态">
          <a-tag :color="getStatusColor(detailData.status)">{{ getStatusText(detailData.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="申请人">{{ detailData.applicantName }}</a-descriptions-item>
        <a-descriptions-item label="申请时间">{{ formatDate(detailData.applyTime) }}</a-descriptions-item>
        <a-descriptions-item label="审批人">{{ detailData.approverName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="审批时间">{{ formatDate(detailData.approveTime) || '-' }}</a-descriptions-item>
        <a-descriptions-item label="申请原因" :span="2">{{ detailData.approvalReason }}</a-descriptions-item>
        <a-descriptions-item label="审批备注" :span="2">{{ detailData.approveRemark || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'

const loading = ref(false)
const submitLoading = ref(false)
const activeTab = ref('pending')
const applyVisible = ref(false)
const approveVisible = ref(false)
const rejectVisible = ref(false)
const detailVisible = ref(false)
const applyFormRef = ref<FormInstance>()

const statistics = ref({
  totalCount: 25,
  pendingCount: 8,
  approvedCount: 15,
  rejectedCount: 2
})

const columns = [
  { title: '产品', dataIndex: 'productName', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 120 },
  { title: '价格变更', key: 'priceChange', width: 180 },
  { title: '审批类型', dataIndex: 'approvalTypeLabel', width: 100 },
  { title: '申请人', dataIndex: 'applicantName', width: 100 },
  { title: '申请时间', dataIndex: 'applyTime', width: 150 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', fixed: 'right', width: 180 }
]

const processedColumns = [
  { title: '产品', dataIndex: 'productName', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 120 },
  { title: '价格变更', key: 'priceChange', width: 180 },
  { title: '状态', key: 'status', width: 80 },
  { title: '审批人', key: 'approver', width: 150 }
]

const myColumns = [
  { title: '产品', dataIndex: 'productName', width: 150 },
  { title: '客户', dataIndex: 'customerName', width: 120 },
  { title: '价格变更', key: 'priceChange', width: 180 },
  { title: '状态', key: 'status', width: 80 },
  { title: '申请时间', dataIndex: 'applyTime', width: 150 }
]

const pendingList = ref<any[]>([])
const approvedList = ref<any[]>([])
const rejectedList = ref<any[]>([])
const myList = ref<any[]>([])

const productList = ref([
  { id: 1, name: '笔记本电脑', code: 'P001', basePrice: 8999 },
  { id: 2, name: '办公桌椅', code: 'P002', basePrice: 2500 },
  { id: 3, name: '打印机', code: 'P003', basePrice: 3500 }
])

const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' }
])

const applyForm = reactive({
  productId: undefined,
  customerId: undefined,
  newPrice: undefined,
  approvalType: undefined,
  effectiveRange: [],
  approvalReason: ''
})

const applyRules = {
  productId: [{ required: true, message: '请选择产品' }],
  newPrice: [{ required: true, message: '请输入新价格' }],
  approvalType: [{ required: true, message: '请选择审批类型' }],
  approvalReason: [{ required: true, message: '请输入申请原因' }]
}

const currentPrice = computed(() => {
  if (!applyForm.productId) return '0.00'
  const product = productList.value.find(p => p.id === applyForm.productId)
  if (!product) return '0.00'
  return product.basePrice.toFixed(2)
})

const priceChangeAmount = computed(() => {
  if (!applyForm.newPrice) return '0.00'
  return Math.abs(applyForm.newPrice - parseFloat(currentPrice.value)).toFixed(2)
})

const priceChangeType = computed(() => {
  if (!applyForm.newPrice) return ''
  return applyForm.newPrice >= parseFloat(currentPrice.value) ? 'increase' : 'decrease'
})

const approveData = ref<any>({})
const approveRemark = ref('')
const rejectData = ref<any>({})
const rejectReason = ref('')
const detailData = ref<any>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

onMounted(() => {
  loadData()
})

const loadData = () => {
  loading.value = true
  pendingList.value = [
    { id: 1, productName: '笔记本电脑', productCode: 'P001', customerName: '北京科技有限公司', oldPrice: 8999, newPrice: 8500, priceChange: 499, priceChangeType: 'decrease', approvalType: 'price_adjustment', approvalTypeLabel: '价格调整', applicantName: '张三', applyTime: '2024-01-15 10:30', status: 'pending' },
    { id: 2, productName: '办公桌椅', productCode: 'P002', customerName: '上海贸易公司', oldPrice: 2500, newPrice: 2800, priceChange: 300, priceChangeType: 'increase', approvalType: 'contract', approvalTypeLabel: '合同价格', applicantName: '李四', applyTime: '2024-01-15 09:20', status: 'pending' },
    { id: 3, productName: '打印机', productCode: 'P003', customerName: null, oldPrice: 3500, newPrice: 3200, priceChange: 300, priceChangeType: 'decrease', approvalType: 'promotion', approvalTypeLabel: '促销价格', applicantName: '王五', applyTime: '2024-01-14 16:45', status: 'pending' }
  ]
  approvedList.value = [
    { id: 4, productName: '笔记本电脑', customerName: '广州制造企业', oldPrice: 8999, newPrice: 9200, priceChange: 201, priceChangeType: 'increase', status: 'approved', approverName: '赵六', approveTime: '2024-01-14 15:30' }
  ]
  rejectedList.value = [
    { id: 5, productName: '办公桌椅', customerName: '深圳电子公司', oldPrice: 2500, newPrice: 2200, priceChange: 300, priceChangeType: 'decrease', status: 'rejected', approverName: '钱七', approveTime: '2024-01-13 14:20' }
  ]
  myList.value = pendingList.value.slice(0, 2)
  pagination.total = pendingList.value.length
  loading.value = false
}

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    pending: 'orange',
    approved: 'green',
    rejected: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    pending: '待审批',
    approved: '已通过',
    rejected: '已拒绝'
  }
  return texts[status] || status
}

const formatDate = (date: string) => {
  if (!date) return ''
  return date.split('T')[0]
}

const filterOption = (input: string, option: any) => {
  return option.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const handleApply = () => {
  Object.assign(applyForm, {
    productId: undefined,
    customerId: undefined,
    newPrice: undefined,
    approvalType: undefined,
    effectiveRange: [],
    approvalReason: ''
  })
  applyVisible.value = true
}

const handleApplySubmit = async () => {
  try {
    await applyFormRef.value?.validate()
    submitLoading.value = true
    message.success('价格变更申请已提交')
    applyVisible.value = false
    loadData()
  } catch (error) {
    console.error('Validation failed:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = (record: any) => {
  approveData.value = record
  approveRemark.value = ''
  approveVisible.value = true
}

const handleApproveConfirm = () => {
  message.success('审批已通过')
  approveVisible.value = false
  loadData()
}

const handleReject = (record: any) => {
  rejectData.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

const handleRejectConfirm = () => {
  if (!rejectReason.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  message.success('审批已拒绝')
  rejectVisible.value = false
  loadData()
}

const handleView = (record: any) => {
  detailData.value = {
    ...record,
    approvalTypeLabel: record.approvalTypeLabel || getApprovalTypeText(record.approvalType)
  }
  detailVisible.value = true
}

const getApprovalTypeText = (type: string) => {
  const texts: Record<string, string> = {
    price_adjustment: '价格调整',
    promotion: '促销价格',
    contract: '合同价格',
    discount: '折扣价格'
  }
  return texts[type] || type
}
</script>

<style scoped lang="scss">
.price-approval-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.summary-row {
  margin-bottom: 16px;
}

.price-change {
  .old-price {
    font-size: 12px;
    color: #999;
  }

  .new-price {
    font-size: 12px;
    color: #333;
    margin-left: 8px;
  }

  .change {
    font-size: 12px;
    font-weight: 500;
    margin-left: 8px;

    &.increase {
      color: #f5222d;
    }

    &.decrease {
      color: #3f8600;
    }
  }
}

.current-price {
  font-size: 16px;
  color: #333;
}

.increase {
  color: #f5222d;
  font-weight: 500;
}

.decrease {
  color: #3f8600;
  font-weight: 500;
}
</style>