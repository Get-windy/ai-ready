<template>
  <DetailLayout
    :breadcrumb-items="breadcrumbItems"
    :title="order?.orderNo || ''"
    :status="getStatusText(order?.status)"
    :status-type="getStatusType(order?.status)"
    :show-pager="true"
    :current-index="currentIndex"
    :total-count="totalCount"
    :tabs="tabs"
    :active-tab="activeTab"
    :show-related-documents="true"
    :related-documents="relatedDocuments"
    :show-activity-log="true"
    :activity-logs="activityLogs"
    :loading="loading"
    :error="error"
    @breadcrumb-click="handleBreadcrumbClick"
    @prev="handlePrev"
    @next="handleNext"
    @tab-change="handleTabChange"
    @related-click="handleRelatedClick"
    @retry="fetchOrderDetail"
  >
    <template #header-extra>
      <a-tag v-if="order" color="green">客户: {{ order.customerName }}</a-tag>
    </template>

    <template #actions>
      <a-button v-if="order?.status === 0" type="primary" @click="handleEdit">编辑</a-button>
      <a-button v-if="order?.status === 0" @click="handleSubmit">提交</a-button>
      <a-button v-if="order?.status === 1" type="primary" @click="handleApprove">审批</a-button>
      <a-button v-if="order?.status >= 2" @click="handleCreateOutbound">创建出库单</a-button>
      <PrintButton
        v-if="order?.status >= 2"
        templateType="order"
        :businessId="order?.id"
        businessType="sale_order"
        buttonText="打印"
        @print-success="handlePrintSuccess"
        @print-error="handlePrintError"
      />
    </template>

    <template #more-actions>
      <a-dropdown>
        <a-button>更多操作</a-button>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="handleCopy">复制</a-menu-item>
            <a-menu-item v-if="order?.status === 0" @click="handleDelete">删除</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </template>

    <template #tab-basic>
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="订单号">{{ order?.orderNo }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ order?.customerName }}</a-descriptions-item>
        <a-descriptions-item label="订单日期">{{ order?.orderDate }}</a-descriptions-item>
        <a-descriptions-item label="预计发货日期">{{ order?.deliveryDate }}</a-descriptions-item>
        <a-descriptions-item label="销售员">{{ order?.salesperson }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(order?.status)">
            {{ getStatusText(order?.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="订单金额">¥{{ order?.totalAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="折扣金额">¥{{ order?.discountAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="税额">¥{{ order?.taxAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="最终金额">¥{{ order?.finalAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ order?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ order?.updateTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ order?.remark || '无' }}</a-descriptions-item>
      </a-descriptions>
    </template>

    <template #tab-detail>
      <a-table
        :columns="detailColumns"
        :data-source="order?.details || []"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ record.amount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'taxAmount'">
            ¥{{ record.taxAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            ¥{{ record.unitPrice?.toFixed(2) }}
          </template>
          <template v-else-if="column.key === 'discount'">
            {{ record.discount }}%
          </template>
          <template v-else-if="column.key === 'taxRate'">
            {{ record.taxRate }}%
          </template>
        </template>
      </a-table>
    </template>

    <template #tab-accounting>
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="收款方式">{{ order?.paymentMethod || '待确定' }}</a-descriptions-item>
        <a-descriptions-item label="收款状态">{{ order?.paymentStatus || '未收款' }}</a-descriptions-item>
        <a-descriptions-item label="已收款金额">¥{{ order?.paidAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="未收款金额">¥{{ (order?.finalAmount - order?.paidAmount)?.toFixed(2) || order?.finalAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
      </a-descriptions>
    </template>

    <template #tab-attachments>
      <a-empty v-if="!order?.attachments || order.attachments.length === 0" description="暂无附件" />
      <div v-else class="attachments-list">
        <div
          v-for="file in order.attachments"
          :key="file.id"
          class="attachment-item"
        >
          <span class="attachment-name">{{ file.name }}</span>
          <span class="attachment-size">{{ file.size }}</span>
        </div>
      </div>
    </template>
  </DetailLayout>

  <SaleOrderFormModal
    v-model:open="formModalVisible"
    :edit-data="order"
    @success="handleFormSuccess"
  />

  <a-modal
    v-model:open="outboundModalVisible"
    title="创建出库单"
    width="600px"
    @ok="handleOutboundOk"
    @cancel="outboundModalVisible = false"
    :confirm-loading="outboundSubmitting"
  >
    <a-form :model="outboundForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="来源订单">{{ order?.orderNo }}</a-form-item>
      <a-form-item label="客户">{{ order?.customerName }}</a-form-item>
      <a-form-item label="出库日期">
        <a-date-picker v-model:value="outboundForm.outboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="仓库">
        <a-input v-model:value="outboundForm.warehouseName" placeholder="请输入仓库" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="outboundForm.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import { salesOrderApi, type SalesOrder } from '@/api/order'
import { outboundApi } from '@/api/erp'
import SaleOrderFormModal from '../components/SaleOrderFormModal.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter()
const route = useRoute()

const order = ref<SalesOrder | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const formModalVisible = ref(false)
const outboundModalVisible = ref(false)
const outboundSubmitting = ref(false)
const outboundForm = ref({
  outboundDate: undefined as string | undefined,
  warehouseName: '',
  remark: ''
})
const currentIndex = ref(1)
const totalCount = ref(1)
const activeTab = ref('basic')

const breadcrumbItems = computed(() => [
  { text: '销售管理', path: '/sale' },
  { text: '销售订单', path: '/sale?tab=orders' },
  { text: order.value?.orderNo || '' }
])

const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'detail', label: '明细信息' },
  { key: 'accounting', label: '会计信息' },
  { key: 'attachments', label: '附件' }
]

const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '折扣', dataIndex: 'discount', key: 'discount', width: 80 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '税率', dataIndex: 'taxRate', key: 'taxRate', width: 80 },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100 },
  { title: '价税合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 }
]

const relatedDocuments = computed(() => {
  const docs = []
  if (order.value?.outboundNo) {
    docs.push({
      id: order.value.outboundId,
      type: '出库单',
      no: order.value.outboundNo,
      status: order.value.outboundStatus
    })
  }
  if (order.value?.receiptNo) {
    docs.push({
      id: order.value.receiptId,
      type: '收款单',
      no: order.value.receiptNo,
      status: order.value.receiptStatus
    })
  }
  if (order.value?.invoiceNo) {
    docs.push({
      id: order.value.invoiceId,
      type: '发票',
      no: order.value.invoiceNo,
      status: order.value.invoiceStatus
    })
  }
  return docs
})

const activityLogs = computed(() => {
  const logs = []
  if (order.value?.createTime) {
    logs.push({ id: 1, time: order.value.createTime, user: order.value.salesperson || '系统', action: '创建订单' })
  }
  if (order.value?.submitTime) {
    logs.push({ id: 2, time: order.value.submitTime, user: order.value.submitterName || '', action: '提交订单' })
  }
  if (order.value?.approveTime) {
    logs.push({ id: 3, time: order.value.approveTime, user: order.value.approverName || '', action: '审批订单' })
  }
  if (order.value?.shipTime) {
    logs.push({ id: 4, time: order.value.shipTime, user: order.value.shipperName || '', action: '确认出库' })
  }
  if (order.value?.completeTime) {
    logs.push({ id: 5, time: order.value.completeTime, user: '系统', action: '订单完成' })
  }
  return logs.length > 0 ? logs : [{ id: 1, time: order.value?.createTime || '', user: '系统', action: '创建订单' }]
})

const getStatusColor = (status?: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status ?? 0] || 'default'
}

const getStatusText = (status?: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '部分发货',
    4: '已完成',
    5: '已取消'
  }
  return texts[status ?? 0] || '未知'
}

const getStatusType = (status?: number) => {
  const types: Record<number, 'success' | 'warning' | 'danger' | 'info' | 'default'> = {
    0: 'default',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return types[status ?? 0] || 'default'
}

const fetchOrderDetail = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await salesOrderApi.getById(Number(route.params.id))
    order.value = res.data || res
  } catch (err: any) {
    error.value = err?.message || '获取订单详情失败'
  } finally {
    loading.value = false
  }
}

const handleBreadcrumbClick = (item: any, index: number) => {
  if (item.path) {
    router.push(item.path)
  }
}

const handlePrev = async () => {
  const prevId = order.value ? order.value.id - 1 : 0
  if (prevId < 1) {
    message.warning('已是第一条')
    return
  }
  try {
    await salesOrderApi.getById(prevId)
    router.push(`/sale/order/${prevId}`)
  } catch {
    message.warning('已是第一条')
  }
}

const handleNext = async () => {
  const nextId = order.value ? order.value.id + 1 : 1
  try {
    await salesOrderApi.getById(nextId)
    router.push(`/sale/order/${nextId}`)
  } catch {
    message.warning('已是最后一条')
  }
}

const handleTabChange = (key: string) => {
  activeTab.value = key
}

const handleRelatedClick = (doc: any) => {
  if (doc.type === '出库单') {
    router.push(`/sale/outbound/${doc.id}`)
  } else if (doc.type === '收款单') {
    router.push(`/sale/receipt/${doc.id}`)
  } else if (doc.type === '发票') {
    router.push(`/sale/invoice/${doc.id}`)
  }
}

const handleEdit = () => {
  formModalVisible.value = true
}

const handleSubmit = async () => {
  try {
    await salesOrderApi.submit(order.value!.id)
    message.success('提交成功')
    fetchOrderDetail()
  } catch (err: any) {
    message.error('提交失败')
  }
}

const handleApprove = async () => {
  try {
    await salesOrderApi.approve(order.value!.id)
    message.success('审批成功')
    fetchOrderDetail()
  } catch (err: any) {
    message.error('审批失败')
  }
}

const handleCreateOutbound = () => {
  outboundForm.value = {
    outboundDate: new Date().toISOString().slice(0, 10),
    warehouseName: '',
    remark: ''
  }
  outboundModalVisible.value = true
}

const handleOutboundOk = async () => {
  outboundSubmitting.value = true
  try {
    const payload = {
      orderId: order.value!.id,
      orderNo: order.value!.orderNo,
      customerId: order.value!.customerId,
      customerName: order.value!.customerName,
      outboundDate: outboundForm.value.outboundDate,
      warehouseName: outboundForm.value.warehouseName,
      remark: outboundForm.value.remark,
      details: order.value!.details?.map((d: any) => ({
        productCode: d.productCode,
        productName: d.productName,
        specification: d.specification,
        quantity: d.quantity,
        unitPrice: d.unitPrice,
        amount: d.amount,
        taxRate: d.taxRate,
        taxAmount: d.taxAmount
      }))
    }
    const res = await outboundApi.create(payload)
    const newId = (res as any).data?.id || (res as any).data || (res as any).id
    message.success('出库单创建成功')
    outboundModalVisible.value = false
    if (typeof newId === 'number' && newId > 0) {
      router.push(`/sale/outbound/${newId}`)
    } else {
      fetchOrderDetail()
    }
  } catch (err: any) {
    message.error(err?.message || '创建出库单失败')
  } finally {
    outboundSubmitting.value = false
  }
}

const handleCopy = async () => {
  if (!order.value) return
  try {
    const copyData: any = { ...order.value }
    delete copyData.id
    copyData.orderNo = (copyData.orderNo || '') + '-副本'
    copyData.status = 0
    copyData.createTime = undefined
    copyData.updateTime = undefined
    copyData.details = undefined
    const res = await salesOrderApi.create(copyData)
    const newId = (res as any).data?.id || (res as any).data
    message.success('复制成功')
    if (typeof newId === 'number' && newId > 0) {
      router.push(`/sale/order/${newId}`)
    } else {
      router.push('/sale?tab=orders')
    }
  } catch (err: any) {
    message.error(err?.message || '复制失败')
  }
}

const handleDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除订单「${order.value?.orderNo}」吗？删除后不可恢复。`,
    okText: '确定删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await salesOrderApi.delete(order.value!.id)
        message.success('删除成功')
        router.push('/sale?tab=orders')
      } catch (err: any) {
        message.error('删除失败')
      }
    }
  })
}

const handleFormSuccess = () => {
  fetchOrderDetail()
}

const handlePrintSuccess = () => {
  message.success('打印成功')
}

const handlePrintError = (err: any) => {
  message.error(`打印失败: ${err.message || '未知错误'}`)
}

onMounted(() => {
  fetchOrderDetail()
})
</script>

<style scoped>
.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #fafafa;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
}

.attachment-name {
  font-size: 14px;
  color: #1890ff;
  cursor: pointer;
}

.attachment-name:hover {
  text-decoration: underline;
}

.attachment-size {
  font-size: 12px;
  color: #999;
}
</style>
