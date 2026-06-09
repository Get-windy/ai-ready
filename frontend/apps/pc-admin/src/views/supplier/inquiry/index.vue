<template>
  <div class="inquiry-page">
    <a-page-header
      title="供应商询价报价"
      @back="handleBack"
    >
      <template #extra>
        <a-button type="primary" @click="handleCreateInquiry">
          <template #icon><PlusOutlined /></template>
          发起询价
        </a-button>
      </template>
    </a-page-header>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ inquiries.length }}</div>
          <div class="stat-card-label">询价总数</div>
        </div>
        <FileTextOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pendingCount }}</div>
          <div class="stat-card-label">待报价</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-quoted">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ quotedCount }}</div>
          <div class="stat-card-label">已报价</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-accepted">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ acceptedCount }}</div>
          <div class="stat-card-label">已接受</div>
        </div>
        <CheckOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card :bordered="false" v-if="supplier" style="margin-bottom: 16px">
      <a-descriptions size="small" :column="4">
        <a-descriptions-item label="供应商名称">{{ supplier.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplier.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="询价总数">{{ inquiries.length }}</a-descriptions-item>
        <a-descriptions-item label="待报价">{{ pendingCount }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false" class="table-card">
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="inquiries"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #inquiryStatusCell="{ record }">
          <a-tag :color="getStatusColor(record.inquiryStatus)">{{ getStatusLabel(record.inquiryStatus) }}</a-tag>
        </template>
        <template #quotationStatusCell="{ record }">
          <a-tag v-if="record.quotationStatus === 1" color="success">已报价</a-tag>
          <a-tag v-else color="default">待报价</a-tag>
        </template>
        <template #quotationAmountCell="{ record }">
          {{ record.quotationAmount ? `¥${record.quotationAmount.toLocaleString()}` : '-' }}
        </template>
        <template #action="{ record }">
          <a-space>
            <a-button
              v-if="record.quotationStatus === 1"
              size="small"
              @click="handleViewQuotation(record)"
            >查看报价</a-button>
            <a-button
              v-if="record.quotationStatus === 1 && record.inquiryStatus === 1"
              size="small"
              type="primary"
              @click="handleAcceptQuotation(record)"
            >接受</a-button>
            <a-button
              v-if="record.quotationStatus === 1 && record.inquiryStatus === 1"
              size="small"
              danger
              @click="handleRejectQuotation(record)"
            >拒绝</a-button>
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <a-modal
      v-model:open="showCreateModal"
      title="发起询价"
      @ok="submitInquiry"
      :confirm-loading="submitLoading"
    >
      <a-form ref="formRef" :model="createForm" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="询价标题" name="inquiryTitle">
          <a-input v-model:value="createForm.inquiryTitle" placeholder="请输入询价标题" />
        </a-form-item>
        <a-form-item label="截止日期" name="deadline">
          <a-date-picker v-model:value="createForm.deadline" style="width: 100%" placeholder="选择截止日期" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="createForm.remark" :rows="3" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, CheckOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const supplierId = (route.params.id as string) || (route.query.id as string) || ''

if (!supplierId) {
  console.warn('[供应商询价] 缺少供应商ID参数，将返回列表')
  router.replace('/supplier/index')
}

interface InquiryRecord {
  id: number
  inquiryNo: string
  inquiryTitle: string
  inquiryStatus: number
  createTime: string
  deadline: string
  quotationAmount: number
  quotationStatus: number
  quotationTime: string
  remark: string
}

const supplier = ref<{ supplierCode: string; supplierName: string } | null>(null)
const inquiries = ref<InquiryRecord[]>([])
const loading = ref(false)
const submitLoading = ref(false)
const showCreateModal = ref(false)
const formRef = ref<FormInstance>()

const createForm = ref({
  inquiryTitle: '',
  deadline: undefined as dayjs.Dayjs | undefined,
  remark: ''
})

const formRules = {
  inquiryTitle: [requiredRule('询价标题')],
  deadline: [{ required: true, message: '请选择截止日期' }]
}

const pendingCount = computed(() => inquiries.value.filter(i => i.inquiryStatus === 0).length)
const quotedCount = computed(() => inquiries.value.filter(i => i.quotationStatus === 1).length)
const acceptedCount = computed(() => inquiries.value.filter(i => i.inquiryStatus === 2).length)



const vxeColumns = [
  { field: 'inquiryNo', title: '询价单号', width: 160 },
  { field: 'inquiryTitle', title: '询价标题', width: 180 },
  { field: 'inquiryStatus', title: '状态', width: 100, slotName: 'inquiryStatusCell' },
  { field: 'quotationAmount', title: '报价金额', width: 130, slotName: 'quotationAmountCell' },
  { field: 'quotationStatus', title: '报价状态', width: 100, slotName: 'quotationStatusCell' },
  { field: 'deadline', title: '截止日期', width: 120 },
  { field: 'createTime', title: '创建时间', width: 120 },
  { field: 'action', title: '操作', width: 220, fixed: 'right', type: 'action' },
]

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待报价', 1: '已报价', 2: '已接受', 3: '已拒绝', 4: '已过期' }
  return map[status] || '未知'
}

const getStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'processing', 1: 'success', 2: 'success', 3: 'error', 4: 'default' }
  return map[status] || 'default'
}

onMounted(async () => {
  await Promise.allSettled([loadSupplier(), loadInquiries()])
})

const loadSupplier = async () => {
  if (!supplierId) return
  try {
    const res = await supplierApi.getById(Number(supplierId))
    supplier.value = res as any
  } catch {
    supplier.value = { supplierCode: 'SUP001', supplierName: '示例供应商' }
  }
}

const loadInquiries = async () => {
  if (!supplierId) return
  loading.value = true
  try {
    const res = await supplierApi.getInquiries(Number(supplierId))
    inquiries.value = (res as any)?.data || (res as any) || []
  } catch (err: any) {
    message.error(err?.message || '获取询价记录失败')
    inquiries.value = []
  } finally {
    loading.value = false
  }
}

const handleCreateInquiry = () => {
  createForm.value = { inquiryTitle: '', deadline: undefined, remark: '' }
  showCreateModal.value = true
}

const submitInquiry = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    await request.post('/v1/supplier-portal/inquiries', {
      supplierId: Number(supplierId || 0),
      inquiryTitle: createForm.value.inquiryTitle,
      deadline: createForm.value.deadline?.format('YYYY-MM-DD'),
      remark: createForm.value.remark
    })
    message.success('询价单创建成功')
    showCreateModal.value = false
    await loadInquiries()
  } catch (err: any) {
    if (err?.errorFields) return // 表单验证错误，不提示
    message.error(err?.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleViewQuotation = (inquiry: InquiryRecord) => {
  Modal.info({
    title: '报价详情',
    content: `报价金额: ¥${inquiry.quotationAmount?.toLocaleString() || '0'}`
  })
}

const handleAcceptQuotation = async (inquiry: InquiryRecord) => {
  Modal.confirm({
    title: '确认接受报价',
    content: `确定接受报价 ¥${inquiry.quotationAmount?.toLocaleString() || '0'}？`,
    onOk: async () => {
      try {
        await request.post(`/v1/supplier-portal/quotations/${inquiry.id}/accept`)
        message.success('报价已接受')
        await loadInquiries()
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    }
  })
}

const handleRejectQuotation = async (inquiry: InquiryRecord) => {
  let reason = ''
  Modal.confirm({
    title: '拒绝报价',
    content: '请输入拒绝原因',
    onOk: async () => {
      if (!reason.trim()) {
        message.warning('请输入拒绝原因')
        throw new Error('请输入拒绝原因')
      }
      try {
        await request.post(`/v1/supplier-portal/quotations/${inquiry.id}/reject`, null, { params: { reason } })
        message.success('报价已拒绝')
        await loadInquiries()
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    }
  })
}

const handleBack = () => {
  if (!supplierId) { router.push('/supplier/index'); return }
  router.push(`/supplier/detail/${supplierId}`)
}

</script>

<style scoped>
.inquiry-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-quoted { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-accepted { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.table-card {
  flex: 1;
  border-radius: 8px;
}

/* 空行占位符 */





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>
