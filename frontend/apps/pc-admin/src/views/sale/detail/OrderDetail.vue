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
      <a-button v-if="order?.status === 0" v-permission="'sale:order:update'" type="primary" @click="handleEdit">编辑</a-button>
      <a-button v-if="order?.status === 0" v-permission="'sale:order:submit'" :loading="submitting" @click="handleSubmit">提交</a-button>
      <a-button v-if="order?.status === 1" v-permission="'sale:order:approve'" type="primary" :loading="approving" @click="handleApprove">审批</a-button>
      <a-button v-if="order?.status >= 2" v-permission="'sale:order:ship'" @click="handleCreateOutbound">创建出库单</a-button>
      <PrintButton
        v-if="order?.status >= 2"
        v-permission="'sale:order:list'"
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
            <a-menu-item v-permission="'sale:order:create'" @click="handleCopy">复制订单</a-menu-item>
            <a-menu-item v-permission="'sale:order:delete'" :disabled="order?.status !== 0" @click="handleDelete">
              删除订单
              <span v-if="order?.status !== 0" style="color: #999; font-size: 12px; margin-left: 4px">(仅草稿可删除)</span>
            </a-menu-item>
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
        <a-descriptions-item label="更新时间">
          {{ order?.updateTime }}
          <span v-if="order?.updateTime" class="update-time-hint">
            ({{ dayjs(order.updateTime).fromNow() }})
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ order?.remark || '无' }}</a-descriptions-item>
      </a-descriptions>
    </template>

    <template #tab-detail>
      <VxeTableList
        :columns="detailVxeColumns"
        :data-source="order?.details || []"
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
        <template #taxAmountCell="{ record }">
          ¥{{ record.taxAmount?.toFixed(2) }}
        </template>
        <template #totalAmountCell="{ record }">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template #unitPriceCell="{ record }">
          ¥{{ record.unitPrice?.toFixed(2) }}
        </template>
        <template #discountCell="{ record }">
          {{ record.discount }}%
        </template>
        <template #taxRateCell="{ record }">
          {{ record.taxRate }}%
        </template>
      </VxeTableList>
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
          <span class="attachment-name" @click="handleDownloadAttachment(file)">{{ file.name }}</span>
          <span class="attachment-size">{{ formatFileSize(file.size) }}</span>
          <a-button type="link" size="small" @click="handleDownloadAttachment(file)">
            <template #icon><DownloadOutlined /></template>
          </a-button>
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
    <a-form ref="outboundFormRef" :model="outboundForm" :rules="outboundRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="来源订单">{{ order?.orderNo }}</a-form-item>
      <a-form-item label="客户">{{ order?.customerName }}</a-form-item>
      <a-form-item label="出库日期" name="outboundDate">
        <a-date-picker v-model:value="outboundForm.outboundDate" style="width: 100%" />
      </a-form-item>
      <a-form-item label="仓库" name="warehouseName">
        <a-input v-model:value="outboundForm.warehouseName" placeholder="请输入仓库" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="outboundForm.remark" placeholder="请输入备注" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { DetailLayout } from '@ai-ready/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { salesOrderApi, type SalesOrder } from '@/api/order'
import { outboundApi } from '@/api/erp'
import { logApi } from '@/api/log'
import { ORDER_STATUS_TEXT, ORDER_STATUS_COLOR } from '@/views/sale/constants/orderStatus'
import SaleOrderFormModal from '../components/SaleOrderFormModal.vue'
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

function formatFileSize(bytes: number): string {
  if (!bytes || bytes <= 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function validateId(id: string | string[] | undefined): number | null {
  if (!id) return null
  const num = Number(id)
  return Number.isFinite(num) && num > 0 ? num : null
}

const router = useRouter()
const route = useRoute()

const order = ref<any>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const formModalVisible = ref(false)
const submitting = ref(false)
const approving = ref(false)
const outboundModalVisible = ref(false)
const outboundSubmitting = ref(false)
const creatingOutbound = ref(false)
const outboundForm = ref({
  outboundDate: undefined as string | undefined,
  warehouseName: '',
  remark: ''
})
const outboundFormRef = ref<FormInstance>()
const outboundRules: any = {
  outboundDate: [{ required: true, message: '请选择出库日期', trigger: 'change' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}
const listIds = ref<number[]>([])
const currentIndex = ref(1)
const totalCount = ref(1)
const activeTab = ref('basic')

// 从 sessionStorage 读取列表 ID 顺序
try {
  const stored = sessionStorage.getItem('sale_order_list_ids')
  if (stored) {
    listIds.value = JSON.parse(stored)
    totalCount.value = listIds.value.length
  }
} catch (err) { console.warn("[销售订单详情] 列表ID加载失败", err) }

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

const detailVxeColumns = [
  { field: 'productCode', title: '产品编码', width: 150 },
  { field: 'productName', title: '产品名称' },
  { field: 'specification', title: '规格型号', width: 120 },
  { field: 'quantity', title: '数量', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'discount', title: '折扣', width: 80, slotName: 'discountCell' },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'taxRate', title: '税率', width: 80, slotName: 'taxRateCell' },
  { field: 'taxAmount', title: '税额', width: 100, slotName: 'taxAmountCell' },
  { field: 'totalAmount', title: '价税合计', width: 120, slotName: 'totalAmountCell' }
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

const activityLogsList = ref<{ id: number; time: string; user: string; action: string }[]>([])

const activityLogs = computed(() => {
  if (activityLogsList.value.length > 0) return activityLogsList.value
  // 降级：从订单字段生成活动日志
  const logs = []
  if (order.value?.createTime) {
    logs.push({ id: 1, time: `${order.value.createTime} (${dayjs(order.value.createTime).fromNow()})`, user: order.value.salesperson || '系统', action: '创建订单' })
  }
  if (order.value?.submitTime) {
    logs.push({ id: 2, time: `${order.value.submitTime} (${dayjs(order.value.submitTime).fromNow()})`, user: order.value.submitterName || '', action: '提交订单' })
  }
  if (order.value?.approveTime) {
    logs.push({ id: 3, time: `${order.value.approveTime} (${dayjs(order.value.approveTime).fromNow()})`, user: order.value.approverName || '', action: '审批订单' })
  }
  if (order.value?.shipTime) {
    logs.push({ id: 4, time: `${order.value.shipTime} (${dayjs(order.value.shipTime).fromNow()})`, user: order.value.shipperName || '', action: '确认出库' })
  }
  if (order.value?.completeTime) {
    logs.push({ id: 5, time: `${order.value.completeTime} (${dayjs(order.value.completeTime).fromNow()})`, user: '系统', action: '订单完成' })
  }
  return logs.length > 0 ? logs : [{ id: 1, time: order.value?.createTime ? `${order.value.createTime} (${dayjs(order.value.createTime).fromNow()})` : '', user: '系统', action: '创建订单' }]
})

const getStatusColor = (status?: number) => ORDER_STATUS_COLOR[status ?? 0] || 'default'

const getStatusText = (status?: number) => ORDER_STATUS_TEXT[status ?? 0] || '未知'

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
  const id = validateId(route.params.id)
  if (!id) {
    error.value = '无效的订单ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await salesOrderApi.getById(id)
    order.value = res.data || res
    fetchActivityLogs()
  } catch (err: any) {
    error.value = err?.message || '获取订单详情失败'
  } finally {
    loading.value = false
  }
}

const fetchActivityLogs = async () => {
  if (!route.params.id) return
  try {
    const res = await logApi.getPage({
      module: '销售订单管理',
      pageNum: 1,
      pageSize: 20
    })
    const pageData = (res as any)?.data ?? res
    const records = pageData?.records ?? []
    if (records.length > 0) {
      activityLogsList.value = records.map((log: any) => ({
        id: log.id,
        time: `${log.operationTime || log.createTime} (${dayjs(log.operationTime || log.createTime).fromNow()})`,
        user: log.operatorName || log.operator || '系统',
        action: log.description || log.action || ''
      }))
    }
  } catch {
    // 活动日志为非关键信息，静默失败
  }
}

const handleBreadcrumbClick = (item: any, index: number) => {
  if (item.path) {
    router.push(item.path)
  }
}

const handlePrev = () => {
  if (!order.value) return
  const idx = listIds.value.indexOf(order.value.id)
  if (idx <= 0) {
    message.warning('已是第一条记录')
    return
  }
  router.push(`/sale/order/${listIds.value[idx - 1]}`)
}

const handleNext = () => {
  if (!order.value) return
  const idx = listIds.value.indexOf(order.value.id)
  if (idx < 0 || idx >= listIds.value.length - 1) {
    message.warning('已是最后一条记录')
    return
  }
  router.push(`/sale/order/${listIds.value[idx + 1]}`)
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
  submitting.value = true
  try {
    await salesOrderApi.submit(order.value!.id)
    message.success('提交成功')
    fetchOrderDetail()
  } catch (err: any) {
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const handleApprove = async () => {
  approving.value = true
  try {
    await salesOrderApi.approve(order.value!.id)
    message.success('审批成功')
    fetchOrderDetail()
  } catch (err: any) {
    message.error('审批失败')
  } finally {
    approving.value = false
  }
}

const handleCreateOutbound = () => {
  creatingOutbound.value = true
  outboundForm.value = {
    outboundDate: new Date().toISOString().slice(0, 10),
    warehouseName: '',
    remark: ''
  }
  outboundModalVisible.value = true
  creatingOutbound.value = false
}

const handleOutboundOk = async () => {
  try {
    await outboundFormRef.value?.validate()
  } catch {
    return
  }
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
    const { id, createTime, updateTime, ...rest } = order.value
    const copyData: any = {
      ...rest,
      orderNo: (order.value.orderNo || '') + '-副本',
      status: 0,
      // 保留明细（移除 ID 重新生成）
      details: order.value.details?.map(({ id: _id, ...item }: any) => ({ ...item })) || []
    }
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
    content: `确定要删除订单「${order.value?.orderNo}」吗？删除后数据不可恢复。`,
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

const handleDownloadAttachment = (file: any) => {
  if (file.url) {
    window.open(file.url, '_blank')
  } else if (file.id) {
    // 调用附件下载接口
    const link = document.createElement('a')
    link.href = `/api/file/download/${file.id}`
    link.download = file.name || `附件-${file.id}`
    link.click()
  } else {
    message.warning('该附件暂无下载地址')
  }
}

// 计算当前记录在列表中的位置
watch(() => order.value?.id, (id) => {
  if (id && listIds.value.length > 0) {
    const idx = listIds.value.indexOf(id)
    currentIndex.value = idx >= 0 ? idx + 1 : 1
  }
})

// 路由离开拦截：编辑表单未保存时提醒
onBeforeRouteLeave((to, from, next) => {
  if (formModalVisible.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前有未保存的编辑内容，确定要离开吗？',
      okText: '确认离开',
      cancelText: '继续编辑',
      centered: true,
      onOk() { next() },
      onCancel() { next(false) }
    })
  } else {
    next()
  }
})

// 路由变化时重新加载（prev/next 导航）
watch(() => route.params.id, () => {
  fetchOrderDetail()
})

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchOrderDetail)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    handleEdit()
  }
}

onMounted(() => {
  fetchOrderDetail()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
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

.update-time-hint {
  color: #999;
  font-size: 12px;
  margin-left: 8px;
  white-space: nowrap;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
