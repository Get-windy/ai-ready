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
    @breadcrumb-click="handleBreadcrumbClick"
    @prev="handlePrev"
    @next="handleNext"
    @tab-change="handleTabChange"
    @related-click="handleRelatedClick"
  >
    <template #actions>
      <a-button v-if="order?.status === 0" type="primary" @click="handleEdit">编辑</a-button>
      <a-button v-if="order?.status === 0" @click="handleSubmit">提交</a-button>
      <a-button v-if="order?.status === 1" type="primary" @click="handleApprove">审批</a-button>
      <a-button v-if="order?.status >= 2" @click="handleCreateInbound">创建入库单</a-button>
      <PrintButton
        v-if="order?.status >= 2"
        templateType="order"
        :businessId="order?.id"
        businessType="purchase_order"
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
        <a-descriptions-item label="供应商">{{ order?.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="订单日期">{{ order?.orderDate }}</a-descriptions-item>
        <a-descriptions-item label="预计到货日期">{{ order?.expectedDeliveryDate }}</a-descriptions-item>
        <a-descriptions-item label="采购员">{{ order?.purchaserName }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(order?.status)">
            {{ getStatusText(order?.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="订单金额">¥{{ order?.totalAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="税额">¥{{ order?.taxAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="价税合计">¥{{ order?.totalAmountWithTax?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ order?.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ order?.remark }}</a-descriptions-item>
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
        </template>
      </a-table>
    </template>

    <template #tab-accounting>
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="付款方式">{{ order?.paymentMethod || '待确定' }}</a-descriptions-item>
        <a-descriptions-item label="付款状态">{{ order?.paymentStatus || '未付款' }}</a-descriptions-item>
        <a-descriptions-item label="已付款金额">¥{{ order?.paidAmount?.toFixed(2) || '0.00' }}</a-descriptions-item>
        <a-descriptions-item label="未付款金额">¥{{ (order?.totalAmountWithTax - order?.paidAmount)?.toFixed(2) || order?.totalAmountWithTax?.toFixed(2) }}</a-descriptions-item>
      </a-descriptions>
    </template>
  </DetailLayout>

  <PurchaseOrderFormModal
    v-model:open="formModalVisible"
    :edit-data="order"
    @success="handleFormSuccess"
  />

  <a-modal
    v-model:open="inboundModalVisible"
    title="创建入库单"
    width="600px"
    @ok="handleInboundOk"
    @cancel="inboundModalVisible = false"
    :confirm-loading="inboundSubmitting"
  >
    <a-form :model="inboundForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="来源订单">{{ order?.orderNo }}</a-form-item>
      <a-form-item label="供应商">{{ order?.supplierName }}</a-form-item>
      <a-form-item label="入库日期">
        <a-date-picker v-model:value="inboundForm.inboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="仓库">
        <a-input v-model:value="inboundForm.warehouseName" placeholder="请输入仓库" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="inboundForm.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'
import { inboundApi } from '@/api/erp'
import PurchaseOrderFormModal from '../components/PurchaseOrderFormModal.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const router = useRouter()
const route = useRoute()

const order = ref<PurchaseOrder | null>(null)
const formModalVisible = ref(false)
const inboundModalVisible = ref(false)
const inboundSubmitting = ref(false)
const inboundForm = ref({
  inboundDate: undefined as string | undefined,
  warehouseName: '',
  remark: ''
})
const currentIndex = ref(1)
const totalCount = ref(1)
const activeTab = ref('basic')

const breadcrumbItems = computed(() => [
  { text: '采购管理', path: '/purchase' },
  { text: '采购订单', path: '/purchase?tab=orders' },
  { text: order.value?.orderNo || '' }
])

const tabs = [
  { key: 'basic', label: '基本信息' },
  { key: 'detail', label: '明细信息' },
  { key: 'accounting', label: '会计信息' }
]

const detailColumns = [
  { title: '产品编码', dataIndex: 'productCode', key: 'productCode', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格型号', dataIndex: 'specification', key: 'specification', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '税率', dataIndex: 'taxRate', key: 'taxRate', width: 80 },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 100 },
  { title: '价税合计', dataIndex: 'totalAmount', key: 'totalAmount', width: 120 }
]

const relatedDocuments = computed(() => {
  const docs = []
  if (order.value?.inboundNo) {
    docs.push({
      id: order.value.inboundId,
      type: '入库单',
      no: order.value.inboundNo,
      status: order.value.inboundStatus
    })
  }
  if (order.value?.paymentNo) {
    docs.push({
      id: order.value.paymentId,
      type: '付款单',
      no: order.value.paymentNo,
      status: order.value.paymentStatus
    })
  }
  return docs
})

const activityLogs = computed(() => [
  { id: 1, time: order.value?.createTime || '', user: order.value?.purchaserName || '', action: '创建订单' },
  { id: 2, time: order.value?.submitTime || '', user: order.value?.submitterName || '', action: '提交订单' },
  { id: 3, time: order.value?.approveTime || '', user: order.value?.approverName || '', action: '审批订单' }
])

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '草稿',
    1: '待审批',
    2: '已审批',
    3: '部分入库',
    4: '完成',
    5: '已取消'
  }
  return texts[status] || '未知'
}

const getStatusType = (status: number) => {
  const types: Record<number, 'success' | 'warning' | 'danger' | 'info' | 'default'> = {
    0: 'default',
    1: 'warning',
    2: 'success',
    3: 'info',
    4: 'success',
    5: 'danger'
  }
  return types[status] || 'default'
}

const fetchOrderDetail = async () => {
  const orderId = route.params.id as string
  try {
    const res = await purchaseOrderApi.getById(Number(orderId))
    order.value = res.data
  } catch (error) {
    message.error('获取订单详情失败')
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
    await purchaseOrderApi.get(prevId)
    router.push(`/purchase/order/${prevId}`)
  } catch {
    message.warning('已是第一条')
  }
}

const handleNext = async () => {
  const nextId = order.value ? order.value.id + 1 : 1
  try {
    await purchaseOrderApi.get(nextId)
    router.push(`/purchase/order/${nextId}`)
  } catch {
    message.warning('已是最后一条')
  }
}

const handleTabChange = (key: string) => {
  activeTab.value = key
}

const handleRelatedClick = (doc: any) => {
  if (doc.type === '入库单') {
    router.push(`/purchase/inbound/${doc.id}`)
  } else if (doc.type === '付款单') {
    router.push(`/purchase/payment/${doc.id}`)
  }
}

const handleEdit = () => {
  formModalVisible.value = true
}

const handleSubmit = async () => {
  try {
    await purchaseOrderApi.submit(order.value!.id)
    message.success('提交成功')
    fetchOrderDetail()
  } catch (error) {
    message.error('提交失败')
  }
}

const handleApprove = async () => {
  try {
    await purchaseOrderApi.approve(order.value!.id)
    message.success('审批成功')
    fetchOrderDetail()
  } catch (error) {
    message.error('审批失败')
  }
}

const handleCreateInbound = () => {
  inboundForm.value = {
    inboundDate: new Date().toISOString().slice(0, 10),
    warehouseName: '',
    remark: ''
  }
  inboundModalVisible.value = true
}

const handleInboundOk = async () => {
  inboundSubmitting.value = true
  try {
    const payload = {
      orderId: order.value!.id,
      orderNo: order.value!.orderNo,
      supplierId: order.value!.supplierId,
      supplierName: order.value!.supplierName,
      inboundDate: inboundForm.value.inboundDate,
      warehouseName: inboundForm.value.warehouseName,
      remark: inboundForm.value.remark,
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
    const res = await inboundApi.create(payload)
    const newId = (res as any).data || (res as any).id
    message.success('入库单创建成功')
    inboundModalVisible.value = false
    if (newId) {
      router.push(`/purchase/inbound/${newId}`)
    } else {
      fetchOrderDetail()
    }
  } catch (err: any) {
    message.error(err?.message || '创建入库单失败')
  } finally {
    inboundSubmitting.value = false
  }
}

const handleCopy = async () => {
  if (!order.value) return
  try {
    const copyData = { ...order.value }
    delete (copyData as any).id
    copyData.orderNo = (copyData.orderNo || '') + '-副本'
    copyData.status = 0
    copyData.createTime = undefined as any
    copyData.updateTime = undefined as any
    const res = await purchaseOrderApi.create(copyData)
    const newId = (res as any).data || (res as any).id
    message.success('复制成功')
    if (newId) {
      router.push(`/purchase/order/${newId}`)
    }
  } catch (err: any) {
    message.error(err?.message || '复制失败')
  }
}

const handleDelete = async () => {
  try {
    await purchaseOrderApi.delete(order.value!.id)
    message.success('删除成功')
    router.push('/purchase?tab=orders')
  } catch (error) {
    message.error('删除失败')
  }
}

const handleFormSuccess = () => {
  fetchOrderDetail()
}

const handlePrintSuccess = () => {
  message.success('打印成功')
}

const handlePrintError = (error: any) => {
  message.error(`打印失败: ${error.message || '未知错误'}`)
}

onMounted(() => {
  fetchOrderDetail()
})
</script>