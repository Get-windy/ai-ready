<template>
  <DetailLayout
    :breadcrumb-items="bp" :title="data?.productName || data?.productCode || ''"
    :status="data?.quantity && data?.minStock && data.quantity <= data.minStock ? '低库存' : '正常'"
    :status-type="data?.quantity && data?.minStock && data.quantity <= data.minStock ? 'warning' : 'success'"
    :tabs="tabs" :active-tab="activeTab" :loading="loading" :error="error"
    :related-documents="relatedDocs" :activity-logs="logs"
    @breadcrumb-click="(i:any) => i.path && router.push(i.path)" @tab-change="(k:string) => activeTab = k" @retry="fetchDetail"
  >
    <template #header-extra><a-tag v-if="data" :color="data.quantity > (data.minStock||0) ? 'green' : 'red'">{{ data.unit || '个' }}</a-tag></template>
    <template #actions>
      <a-button type="primary" @click="handleInbound">入库</a-button>
      <a-button @click="handleOutbound">出库</a-button>
      <PrintButton template-type="stock" :business-id="data?.id" business-type="stock_item" button-text="打印标签" />
    </template>
    <template #tab-basic>
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="产品编码">{{ data?.productCode }}</a-descriptions-item>
        <a-descriptions-item label="产品名称">{{ data?.productName }}</a-descriptions-item>
        <a-descriptions-item label="规格型号">{{ data?.specification || '-' }}</a-descriptions-item>
        <a-descriptions-item label="单位">{{ data?.unit || '-' }}</a-descriptions-item>
        <a-descriptions-item label="当前库存"><span :style="{color: data?.quantity <= (data?.minStock||0) ? '#ff4d4f' : '#3f8600', fontWeight: 600}">{{ data?.quantity || 0 }}</span></a-descriptions-item>
        <a-descriptions-item label="可用库存">{{ data?.availableQuantity || 0 }}</a-descriptions-item>
        <a-descriptions-item label="锁定库存">{{ data?.lockedQuantity || 0 }}</a-descriptions-item>
        <a-descriptions-item label="安全库存">{{ data?.minStock || '-' }}</a-descriptions-item>
        <a-descriptions-item label="最大库存">{{ data?.maxStock || '-' }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ data?.warehouseName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="最后入库">{{ data?.lastInboundDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="最后出库">{{ data?.lastOutboundDate || '-' }}</a-descriptions-item>
      </a-descriptions>
    </template>
    <template #tab-transactions>
      <VxeTableList
        :columns="txVxeCols"
        :data-source="transactions"
        row-key="id"
        :pagination="{ pageSize: 5 }"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      />
      <a-empty v-if="transactions.length === 0" description="暂无出入库记录" />
    </template>
  </DetailLayout>

  <a-modal
    v-model:open="inboundVisible"
    title="创建入库单"
    width="500px"
    @ok="handleInboundSubmit"
    @cancel="inboundVisible = false"
    :confirm-loading="inboundSubmitting"
  >
    <a-form :model="inboundForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="产品编码">{{ data?.productCode }}</a-form-item>
      <a-form-item label="产品名称">{{ data?.productName }}</a-form-item>
      <a-form-item label="仓库">{{ data?.warehouseName || '-' }}</a-form-item>
      <a-form-item label="入库数量" required>
        <a-input-number v-model:value="inboundForm.quantity" :min="1" style="width: 100%" placeholder="请输入入库数量" />
      </a-form-item>
      <a-form-item label="入库日期">
        <a-date-picker v-model:value="inboundForm.inboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="inboundForm.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="outboundVisible"
    title="创建出库单"
    width="500px"
    @ok="handleOutboundSubmit"
    @cancel="outboundVisible = false"
    :confirm-loading="outboundSubmitting"
  >
    <a-form :model="outboundForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="产品编码">{{ data?.productCode }}</a-form-item>
      <a-form-item label="产品名称">{{ data?.productName }}</a-form-item>
      <a-form-item label="仓库">{{ data?.warehouseName || '-' }}</a-form-item>
      <a-form-item label="当前库存">{{ data?.availableQuantity ?? data?.quantity ?? 0 }}</a-form-item>
      <a-form-item label="出库数量" required>
        <a-input-number v-model:value="outboundForm.quantity" :min="1" :max="data?.availableQuantity ?? data?.quantity ?? 0" style="width: 100%" placeholder="请输入出库数量" />
      </a-form-item>
      <a-form-item label="出库日期">
        <a-date-picker v-model:value="outboundForm.outboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="outboundForm.remark" placeholder="请输入备注" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { stockApi, inboundApi, outboundApi, type StockItem } from '@/api/erp'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter(); const route = useRoute()
const data = ref<StockItem | null>(null); const loading = ref(false); const error = ref<string | null>(null)
const activeTab = ref('basic'); const transactions = ref<any[]>([])
const bp = computed(() => [{ text: '库存管理', path: '/stock?tab=stock' }, { text: '库存查询', path: '/stock?tab=stock' }, { text: data.value?.productName || '' }])
const tabs = [{ key: 'basic', label: '基本信息' }, { key: 'transactions', label: '出入库记录' }]
const relatedDocs = computed(() => [])
const logs = computed(() => [{ id: 1, time: data.value?.lastInboundDate || '', user: '系统', action: '最近入库' }])
const txVxeCols = [
  { field: 'orderNo', title: '单号', width: 160 },
  { field: 'type', title: '类型', width: 80 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'createTime', title: '时间', width: 160 },
]

const fetchDetail = async () => {
  loading.value = true; error.value = null
  try {
    const id = Number(route.params.id)
    if (isNaN(id)) { error.value = '无效的ID'; return }
    data.value = await stockApi.getById(id) as any }
  catch (err: any) { error.value = err?.message || '获取详情失败' }
  finally { loading.value = false }
}
const inboundVisible = ref(false)
const outboundVisible = ref(false)
const inboundSubmitting = ref(false)
const outboundSubmitting = ref(false)
const inboundForm = ref({ quantity: 1, inboundDate: new Date().toISOString().slice(0, 10), remark: '' })
const outboundForm = ref({ quantity: 1, outboundDate: new Date().toISOString().slice(0, 10), remark: '' })

const handleInbound = () => {
  inboundForm.value = { quantity: 1, inboundDate: new Date().toISOString().slice(0, 10), remark: '' }
  inboundVisible.value = true
}
const handleOutbound = () => {
  outboundForm.value = { quantity: 1, outboundDate: new Date().toISOString().slice(0, 10), remark: '' }
  outboundVisible.value = true
}
const handleInboundSubmit = async () => {
  if (!inboundForm.value.quantity || inboundForm.value.quantity < 1) {
    message.warning('请输入有效的入库数量')
    return
  }
  inboundSubmitting.value = true
  try {
    await inboundApi.create({
      productId: data.value?.id,
      productCode: data.value?.productCode,
      productName: data.value?.productName,
      warehouseName: data.value?.warehouseName,
      quantity: inboundForm.value.quantity,
      inboundDate: inboundForm.value.inboundDate,
      remark: inboundForm.value.remark,
      type: 'stock_in'
    })
    message.success('入库单创建成功')
    inboundVisible.value = false
    fetchDetail()
  } catch (err: any) {
    message.error(err?.message || '创建入库单失败')
  } finally {
    inboundSubmitting.value = false
  }
}
const handleOutboundSubmit = async () => {
  const maxQty = data.value?.availableQuantity ?? data.value?.quantity ?? 0
  if (!outboundForm.value.quantity || outboundForm.value.quantity < 1) {
    message.warning('请输入有效的出库数量')
    return
  }
  if (outboundForm.value.quantity > maxQty) {
    message.warning(`出库数量不能超过可用库存(${maxQty})`)
    return
  }
  outboundSubmitting.value = true
  try {
    await outboundApi.create({
      productId: data.value?.id,
      productCode: data.value?.productCode,
      productName: data.value?.productName,
      warehouseName: data.value?.warehouseName,
      quantity: outboundForm.value.quantity,
      outboundDate: outboundForm.value.outboundDate,
      remark: outboundForm.value.remark,
      type: 'stock_out'
    })
    message.success('出库单创建成功')
    outboundVisible.value = false
    fetchDetail()
  } catch (err: any) {
    message.error(err?.message || '创建出库单失败')
  } finally {
    outboundSubmitting.value = false
  }
}
onMounted(() => fetchDetail())
</script>
