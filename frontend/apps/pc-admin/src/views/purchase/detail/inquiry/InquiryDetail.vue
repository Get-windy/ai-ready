<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems" :title="inquiry?.inquiryNo || ''"
    :status="getStatusText(inquiry?.status)" :status-type="getStatusType(inquiry?.status)"
    :tabs="tabs" :active-tab="activeTab" :loading="loading" :error="error"
    :related-documents="relatedDocuments" :activity-logs="activityLogs"
    @breadcrumb-click="handleBreadcrumbClick" @tab-change="handleTabChange"
    @related-click="handleRelatedClick" @retry="fetchDetail"
  >
    <template #header-extra>
      <a-tag v-if="inquiry" color="blue">供应商: {{ inquiry.supplierName }}</a-tag>
    </template>
    <template #actions>
      <a-button v-if="inquiry?.status === 0 && !isEditing" type="primary" @click="handleEdit">编辑</a-button>
      <a-button v-if="inquiry?.status === 0 && !isEditing" @click="handleSend">发送询价</a-button>
      <a-button v-if="isEditing" type="primary" @click="handleSave">保存</a-button>
      <a-button v-if="isEditing" @click="handleCancelEdit">取消</a-button>
      <PrintButton template-type="inquiry" :business-id="inquiry?.id" business-type="purchase_inquiry" button-text="打印" />
    </template>
    <template #tab-basic>
      <a-descriptions v-if="!isEditing" :column="2" bordered size="small">
        <a-descriptions-item label="询价单号">{{ inquiry?.inquiryNo }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ inquiry?.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="询价日期">{{ inquiry?.inquiryDate }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="getStatusColor(inquiry?.status)">{{ getStatusText(inquiry?.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="创建人">{{ inquiry?.creatorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ inquiry?.createTime }}</a-descriptions-item>
      </a-descriptions>
      <a-form v-else :model="editForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="询价单号">
          <a-input v-model:value="editForm.inquiryNo" disabled />
        </a-form-item>
        <a-form-item label="供应商" required>
          <a-input v-model:value="editForm.supplierName" />
        </a-form-item>
        <a-form-item label="询价日期">
          <a-date-picker v-model:value="editForm.inquiryDate" style="width: 100%" />
        </a-form-item>
      </a-form>
    </template>

    <template #tab-items>
      <a-table
        :columns="itemColumns"
        :data-source="inquiry?.items || []"
        row-key="id"
        :pagination="false"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
        </template>
      </a-table>
      <a-empty v-if="!inquiry?.items || inquiry.items.length === 0" description="暂无询价明细" style="margin-top: 16px" />
    </template>

    <template #tab-quotations>
      <a-table
        :columns="quotationColumns"
        :data-source="inquiry?.quotations || []"
        row-key="id"
        :pagination="false"
        size="small"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : record.status === 2 ? 'red' : 'default'">
              {{ record.status === 1 ? '已接受' : record.status === 2 ? '已拒绝' : '待回复' }}
            </a-tag>
          </template>
        </template>
      </a-table>
      <a-empty v-if="!inquiry?.quotations || inquiry.quotations.length === 0" description="暂无报价记录" style="margin-top: 16px" />
    </template>
  </DetailLayout>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInquiryDetail' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import { inquiryApi, type PurchaseInquiry } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter(); const route = useRoute()
const inquiry = ref<PurchaseInquiry | null>(null)
const loading = ref(false); const error = ref<string | null>(null); const activeTab = ref('basic')
const isEditing = ref(false)
const editForm = ref({ inquiryNo: '', supplierName: '', inquiryDate: '' })

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase?tab=inquiry' },
  { text: '询价单', path: '/purchase?tab=inquiry' },
  { text: inquiry.value?.inquiryNo || '' }
])
const tabs = computed(() => [
  { key: 'basic', label: '基本信息' },
  { key: 'items', label: '询价明细' },
  { key: 'quotations', label: '报价记录', count: inquiry.value?.quotations?.length || 0 }
])

const itemColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '参考单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
]

const quotationColumns = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '报价日期', dataIndex: 'quotationDate', key: 'quotationDate', width: 120 },
  { title: '报价金额', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '交货期', dataIndex: 'deliveryDate', key: 'deliveryDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '备注', dataIndex: 'remark', key: 'remark' }
]
const relatedDocuments = computed(() => [])
const activityLogs = computed(() => inquiry.value ? [
  { id: 1, time: inquiry.value.createTime, user: inquiry.value.creatorName || '系统', action: '创建询价单' }
] : [])

const getStatusColor = (s?: number) => ({ 0: 'default', 1: 'blue', 2: 'green' } as any)[s ?? 0] || 'default'
const getStatusText = (s?: number) => ({ 0: '草稿', 1: '已发送', 2: '已报价' } as any)[s ?? 0] || '未知'
const getStatusType = (s?: number): any => ({ 0: 'default', 1: 'info', 2: 'success' } as any)[s ?? 0] || 'default'

const fetchDetail = async () => {
  loading.value = true; error.value = null
  try { inquiry.value = await inquiryApi.getById(Number(route.params.id)) as any }
  catch (err: any) { error.value = err?.message || '获取详情失败' }
  finally { loading.value = false }
}

const handleBreadcrumbClick = (item: any) => { if (item.path) router.push(item.path) }
const handleTabChange = (k: string) => { activeTab.value = k }
const handleRelatedClick = (doc: any) => {
  if (doc?.type === '采购订单') {
    router.push(`/purchase/order/${doc.id}`)
  }
}
const handleEdit = () => {
  isEditing.value = true
  editForm.value = {
    inquiryNo: inquiry.value?.inquiryNo || '',
    supplierName: inquiry.value?.supplierName || '',
    inquiryDate: inquiry.value?.inquiryDate || ''
  }
}
const handleCancelEdit = () => { isEditing.value = false }
const handleSave = async () => {
  try {
    await inquiryApi.update(inquiry.value!.id, editForm.value)
    message.success('保存成功')
    isEditing.value = false
    fetchDetail()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  }
}
const handleSend = async () => {
  try { await inquiryApi.send(inquiry.value!.id); message.success('发送成功'); fetchDetail() }
  catch { message.error('发送失败') }
}
onMounted(() => {
  fetchDetail()
  window.addEventListener('purchase:refresh', fetchDetail)
})
onUnmounted(() => {
  window.removeEventListener('purchase:refresh', fetchDetail)
})
</script>
