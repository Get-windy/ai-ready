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
      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="inquiry?.items || []"
        row-key="id"
        :pagination="false as any"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #amountCell="{ record }">
          ¥{{ record.amount?.toFixed(2) }}
        </template>
      </VxeTableList>
      <a-empty v-if="!inquiry?.items || (inquiry as any).items.length === 0" description="暂无询价明细" style="margin-top: 16px" />
    </template>

    <template #tab-quotations>
      <VxeTableList
        :columns="quotationVxeColumns"
        :data-source="inquiry?.quotations || []"
        row-key="id"
        :pagination="false as any"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #amountCell="{ record }">
          ¥{{ record.amount?.toFixed(2) }}
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : record.status === 2 ? 'red' : 'default'">
            {{ record.status === 1 ? '已接受' : record.status === 2 ? '已拒绝' : '待回复' }}
          </a-tag>
        </template>
      </VxeTableList>
      <a-empty v-if="!inquiry?.quotations || (inquiry as any).quotations.length === 0" description="暂无报价记录" style="margin-top: 16px" />
    </template>
  </DetailLayout>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInquiryDetail' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inquiryApi, type PurchaseInquiry } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const router = useRouter(); const route = useRoute()
let refreshTimer: ReturnType<typeof setInterval> | null = null
const inquiry = ref<any>(null)
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

const itemVxeColumns = [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'unit', title: '单位', width: 60 },
  { field: 'unitPrice', title: '参考单价', width: 100 },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'remark', title: '备注' }
]

const quotationVxeColumns = [
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'quotationDate', title: '报价日期', width: 120 },
  { field: 'amount', title: '报价金额', width: 120, slotName: 'amountCell' },
  { field: 'deliveryDate', title: '交货期', width: 120 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'remark', title: '备注' }
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
  catch (err: any) { console.warn('[询价详情] 获取失败', err); error.value = err?.message || '获取详情失败' }
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
    console.warn('[询价详情] 保存失败', err)
    message.error(err?.message || '保存失败')
  }
}
const handleSend = async () => {
  try { await inquiryApi.send(inquiry.value!.id); message.success('发送成功'); fetchDetail() }
  catch (e) { console.warn('[询价详情] 发送失败', e); message.error('发送失败') }
}
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchDetail)
  }
}
onMounted(() => {
  fetchDetail()
  refreshTimer = setInterval(() => fetchDetail(), 30000)
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('purchase:refresh', fetchDetail)
})
onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('purchase:refresh', fetchDetail)
})
</script>
