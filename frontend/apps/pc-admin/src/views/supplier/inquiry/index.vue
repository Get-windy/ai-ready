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

    <a-card :bordered="false" v-if="supplier" style="margin-bottom: 16px">
      <a-descriptions size="small" :column="4">
        <a-descriptions-item label="供应商名称">{{ supplier.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ supplier.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="询价总数">{{ inquiries.length }}</a-descriptions-item>
        <a-descriptions-item label="待报价">{{ pendingCount }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="inquiries"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'inquiryStatus'">
            <a-tag :color="getStatusColor(record.inquiryStatus)">{{ getStatusLabel(record.inquiryStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'quotationStatus'">
            <a-tag v-if="record.quotationStatus === 1" color="success">已报价</a-tag>
            <a-tag v-else color="default">待报价</a-tag>
          </template>
          <template v-else-if="column.key === 'quotationAmount'">
            {{ record.quotationAmount ? `¥${record.quotationAmount.toLocaleString()}` : '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
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
        </template>
      </a-table>
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
import { PlusOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { supplierApi } from '@/api/supplier'
import { requiredRule } from '@/utils/formRules'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const supplierId = route.params.id as string

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

const columns = [
  { title: '询价单号', dataIndex: 'inquiryNo', key: 'inquiryNo', width: 160 },
  { title: '询价标题', dataIndex: 'inquiryTitle', key: 'inquiryTitle' },
  { title: '状态', key: 'inquiryStatus', width: 100 },
  { title: '报价金额', key: 'quotationAmount', width: 130 },
  { title: '报价状态', key: 'quotationStatus', width: 100 },
  { title: '截止日期', dataIndex: 'deadline', key: 'deadline', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 120 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
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
  try {
    const res = await supplierApi.getById(Number(supplierId))
    supplier.value = res as any
  } catch {
    supplier.value = { supplierCode: 'SUP001', supplierName: '示例供应商' }
  }
}

const loadInquiries = async () => {
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
    await request.post('/supplier-portal/inquiries', {
      supplierId: Number(supplierId),
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
        await request.post(`/supplier-portal/inquiries/${inquiry.id}/accept`)
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
        await request.post(`/supplier-portal/inquiries/${inquiry.id}/reject`, { reason })
        message.success('报价已拒绝')
        await loadInquiries()
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    }
  })
}

const handleBack = () => {
  router.push(`/supplier/detail/${supplierId}`)
}

import request from '@/utils/request'
</script>

<style scoped>
.inquiry-page {
  padding: 0;
}
</style>
