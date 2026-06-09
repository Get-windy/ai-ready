<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems" :title="data?.inboundNo || ''"
    :status="getStatusText(data?.status)" :status-type="getStatusType(data?.status)"
    :tabs="tabs" :active-tab="activeTab" :loading="loading" :error="error"
    :related-documents="relatedDocuments" :activity-logs="activityLogs"
    @breadcrumb-click="handleBreadcrumbClick" @tab-change="handleTabChange"
    @related-click="handleRelatedClick" @retry="fetchDetail"
  >
    <template #header-extra><a-tag v-if="data" color="blue">{{ data.supplierName }}</a-tag></template>
    <template #actions>
      <a-button v-if="data?.status === 0" type="primary" @click="handleApprove">审批入库</a-button>
      <PrintButton template-type="inbound" :business-id="data?.id" business-type="purchase_inbound" button-text="打印" />
    </template>
    <template #tab-basic>
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="入库单号">{{ data?.inboundNo }}</a-descriptions-item>
        <a-descriptions-item label="关联订单">{{ data?.orderNo || '-' }}</a-descriptions-item>
        <a-descriptions-item label="供应商">{{ data?.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="入库日期">{{ data?.inboundDate }}</a-descriptions-item>
        <a-descriptions-item label="金额">¥{{ data?.totalAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="getStatusColor(data?.status)">{{ getStatusText(data?.status) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="创建人">{{ data?.creatorName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ data?.createTime }}</a-descriptions-item>
      </a-descriptions>
    </template>
    <template #tab-items>
      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="data?.items || []"
        row-key="id"
        :pagination="false"
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
      <a-empty v-if="!data?.items || data.items.length === 0" description="暂无入库明细" style="margin-top: 16px" />
    </template>
  </DetailLayout>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInboundDetail' })

import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inboundApi, type PurchaseInbound } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter(); const route = useRoute()
const data = ref<PurchaseInbound | null>(null)
const loading = ref(false); const error = ref<string | null>(null); const activeTab = ref('basic')
const breadcrumbItems = computed(() => [{ text: '采购管理', path: '/purchase?tab=inbound' }, { text: '入库单', path: '/purchase?tab=inbound' }, { text: data.value?.inboundNo || '' }])
const tabs = computed(() => [
  { key: 'basic', label: '基本信息' },
  { key: 'items', label: '入库明细' }
])

const itemVxeColumns = [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'unit', title: '单位', width: 60 },
  { field: 'unitPrice', title: '单价', width: 100 },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'remark', title: '备注' }
]
const relatedDocuments = computed(() => data.value?.orderNo ? [{ id: 1, type: '采购订单', no: data.value.orderNo }] : [])
const activityLogs = computed(() => [{ id: 1, time: data.value?.createTime || '', user: data.value?.creatorName || '系统', action: '创建入库单' }])

const getStatusColor = (s?: number) => ({ 0: 'default', 1: 'orange', 2: 'green' } as any)[s ?? 0] || 'default'
const getStatusText = (s?: number) => ({ 0: '草稿', 1: '待收货', 2: '已入库' } as any)[s ?? 0] || '未知'
const getStatusType = (s?: number): any => ({ 0: 'default', 1: 'warning', 2: 'success' } as any)[s ?? 0] || 'default'

const fetchDetail = async () => {
  loading.value = true; error.value = null
  try { data.value = await inboundApi.getById(Number(route.params.id)) as any }
  catch (err: any) { error.value = err?.message || '获取详情失败' }
  finally { loading.value = false }
}
const handleBreadcrumbClick = (item: any) => { if (item.path) router.push(item.path) }
const handleTabChange = (k: string) => { activeTab.value = k }
const handleRelatedClick = (doc: any) => { if (doc.type === '采购订单') router.push(`/purchase/order/${doc.id}`) }
const handleApprove = async () => { try { await inboundApi.approve(data.value!.id); message.success('审批成功'); fetchDetail() } catch { message.error('审批失败') } }
onMounted(() => {
  fetchDetail()
  window.addEventListener('purchase:refresh', fetchDetail)
})
onUnmounted(() => {
  window.removeEventListener('purchase:refresh', fetchDetail)
})
</script>
