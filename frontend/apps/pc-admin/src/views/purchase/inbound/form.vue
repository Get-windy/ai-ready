<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 非草稿状态锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批/执行流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handleSubmit"
      >
        <!-- ═══ 明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.products"
            :max-height="tableMaxHeight"
            :summary-columns="tableSummaryColumns"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct()">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" disabled>
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
            <template #productCell="{ record, index }">
              <a-select
                v-model:value="record.productId"
                placeholder="搜索选择商品"
                show-search
                :filter-option="filterOption"
                style="width:100%"
                :loading="loadingOptions"
                size="small"
                :disabled="isLocked"
                @change="(val: number) => handleProductChange(val, index)"
              >
                <a-select-option
                  v-for="p in optionRefs.products"
                  :key="p.id"
                  :value="p.id"
                >
                  {{ p.name }}
                </a-select-option>
              </a-select>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 底部备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="请输入单据备注"
                :disabled="isLocked"
              />
            </div>
            <div class="remark-row">
              <span class="remark-label">内部备注</span>
              <a-input
                v-model:value="formData.internalNote"
                size="small"
                class="remark-input"
                placeholder="内部备注（不对外）"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span v-if="formData.approveTime" class="doc-info-item">审核时间 {{ formData.approveTime }}</span>
            <span v-if="formData.receiveTime" class="doc-info-item">收货时间 {{ formData.receiveTime }}</span>
            <span v-if="formData.qualityCheckTime" class="doc-info-item">质检时间 {{ formData.qualityCheckTime }}</span>
            <span v-if="formData.warehouseConfirmTime" class="doc-info-item">入库确认时间 {{ formData.warehouseConfirmTime }}</span>
            <span class="doc-info-item">
              来源订单 <a-tag>{{ formData.sourceOrderNo || '-' }}</a-tag>
            </span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 源采购订单选择弹窗 ═══ -->
      <a-modal
        v-model:open="showOrderSelect"
        title="选择来源采购订单"
        :width="900"
        :footer="null"
        destroy-on-close
      >
        <div class="order-select-search">
          <a-input
            v-model:value="orderSearchKeyword"
            placeholder="搜索订单编号/供应商"
            allow-clear
            style="width:280px"
            @press-enter="loadOrders"
          />
          <a-button type="primary" @click="loadOrders">查询</a-button>
        </div>
        <a-table
          :columns="orderSelectColumns"
          :data-source="orderList"
          :loading="orderLoading"
          :pagination="orderPagination"
          row-key="id"
          size="small"
          @change="handleOrderPageChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="confirmSourceOrder(record)">选择</a-button>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getOrderStatusColor(record.status)">{{ getOrderStatusText(record.status) }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 质检结果弹窗 ═══ -->
      <a-modal
        v-model:open="showQCModal"
        title="质检结果录入"
        :width="420"
        @ok="confirmQC"
        :confirm-loading="qcLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="质检结果" required>
            <a-select v-model:value="qcResult" placeholder="请选择质检结果">
              <a-select-option value="pass">合格</a-select-option>
              <a-select-option value="fail">不合格</a-select-option>
              <a-select-option value="partial">部分合格</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="质检说明">
            <a-textarea v-model:value="qcNote" placeholder="请输入质检说明" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 红冲确认弹窗 ═══ -->
      <a-modal
        v-model:open="showReverseModal"
        title="红冲确认"
        :width="400"
        @ok="confirmReverse"
        :confirm-loading="reverseLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="红冲原因" required>
            <a-input v-model:value="reverseReason" placeholder="请输入红冲原因" />
          </a-form-item>
          <p style="color:#ff4d4f;font-size:12px;">
            红冲将生成一张负数入库单，冲减原单的库存和应付。确认执行？
          </p>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, h, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  ImportOutlined,
  DownloadOutlined,
  SwapOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { inboundApi } from '@/api/erp'
import type { PurchaseInboundPayload } from '@/api/erp'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'PurchaseInboundForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

// ════════════════════════════════════════════
// 状态枚举
// ════════════════════════════════════════════

const INBOUND_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '待收货', color: 'orange' },
  4: { text: '已收货', color: 'processing' },
  5: { text: '待质检', color: 'orange' },
  6: { text: '已质检', color: 'processing' },
  7: { text: '待入库', color: 'orange' },
  8: { text: '已入库', color: 'processing' },
  9: { text: '已完成', color: 'success' },
  10: { text: '已取消', color: 'error' },
}

/**
 * 当前状态下可执行的下一个操作列表
 * 返回 { actionKey, label, icon }
 */
function getAvailableActions(status: number) {
  switch (status) {
    case 0: return [
      { key: 'submit', label: '提交审批', icon: AuditOutlined },
    ]
    case 1: return [
      { key: 'approve', label: '审核通过', icon: CheckOutlined },
      { key: 'reject', label: '驳回', icon: CloseCircleOutlined },
    ]
    case 2:
    case 3: return [
      { key: 'receive', label: '确认收货', icon: DownloadOutlined },
    ]
    case 4:
    case 5: return [
      { key: 'quality-check', label: '质检', icon: CheckOutlined },
    ]
    case 6:
    case 7: return [
      { key: 'confirm-warehouse', label: '入库确认', icon: CheckOutlined },
    ]
    case 8: return [
      { key: 'complete', label: '完成', icon: CheckOutlined },
    ]
    case 9:
    case 10: return []
    default: return []
  }
}

// ════════════════════════════════════════════
// 源单选择相关状态
// ════════════════════════════════════════════

const showOrderSelect = ref(false)
const orderLoading = ref(false)
const orderSearchKeyword = ref('')
const orderList = ref<any[]>([])
const orderPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const selectedOrder = ref<any>(null)

const orderSelectColumns = [
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 160 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '商品金额', dataIndex: 'productAmount', key: 'productAmount', width: 110, align: 'right' },
  { title: '本单金额', dataIndex: 'billAmount', key: 'billAmount', width: 110, align: 'right' },
  { title: '已收数量', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right' },
  { title: '未收数量', dataIndex: 'unreceiveQuantity', key: 'unreceiveQuantity', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 70, fixed: 'right' },
]

async function loadOrders() {
  orderLoading.value = true
  try {
    const params: Record<string, any> = {
      current: orderPagination.current,
      size: orderPagination.pageSize,
      status: 2, // 只查询已审批订单
    }
    if (orderSearchKeyword.value) {
      params.orderNo = orderSearchKeyword.value
      params.supplierName = orderSearchKeyword.value
    }
    const res: any = await request.get('/erp/purchase/order/page', { params })
    const data = res?.data || res || {}
    orderList.value = data?.records || []
    orderPagination.total = data?.total || 0
  } catch {
    message.error('加载采购订单失败')
    orderList.value = []
  } finally {
    orderLoading.value = false
  }
}

function handleOrderPageChange(pagination: any) {
  orderPagination.current = pagination.current || 1
  orderPagination.pageSize = pagination.pageSize || 10
  loadOrders()
}

async function confirmSourceOrder(order: any) {
  selectedOrder.value = order
  showOrderSelect.value = false

  // 填充表头信息
  formData.sourceOrderId = order.id
  formData.sourceOrderNo = order.orderNo
  formData.supplierId = order.supplierId
  formData.supplierName = order.supplierName
  formData.warehouseId = order.warehouseId
  formData.warehouseName = order.warehouseName
  formData.purchaserId = order.purchaserId || order.buyerId
  formData.purchaserName = order.purchaserName || order.buyerName

  // 加载订单明细
  try {
    const res: any = await request.get(`/erp/purchase/order/${order.id}`)
    const detail = res?.data || res
    const items = detail?.items || detail?.details || []

    if (items.length > 0) {
      // 清空现有明细
      formData.products.splice(0, formData.products.length)

      items.forEach((item: any) => {
        const pendingQty = (item.quantity || 0) - (item.receivedQuantity || 0) - (item.inboundQuantity || 0)
        if (pendingQty <= 0) return // 跳过已全部入库的行
        formData.products.push({
          id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
          productId: item.productId,
          productName: item.productName || '',
          productCode: item.productCode || item.itemCode || '',
          itemCode: item.itemCode || item.productCode || '',
          barcode: item.barcode || '',
          specification: item.specification || item.productSpec || '',
          model: item.model || '',
          origin: item.origin || '',
          brand: item.brand || '',
          unit: item.unit || item.productUnit || '',
          smallUnit: item.smallUnit || '',
          batchNo: '',
          productionDate: '',
          expiryDate: '',
          shelfLife: '',
          location: '',
          quantity: pendingQty,
          unitPrice: item.unitPrice || 0,
          amount: pendingQty * (item.unitPrice || 0),
          taxRate: item.taxRate ?? 13,
          costPrice: item.costPrice || 0,
          costAmount: pendingQty * (item.costPrice || 0),
          orderItemId: item.id || item.lineNo,
          gift: item.gift || false,
          weight: item.weight || 0,
          volume: item.volume || 0,
          remark: item.remark || '',
        })
      })

      message.success(`已加载订单 ${order.orderNo}，共 ${formData.products.length} 条待入库明细`)
    }
  } catch {
    message.warning('加载订单明细失败，请手动添加商品')
  }
}

// ════════════════════════════════════════════
// 质检弹窗
// ════════════════════════════════════════════

const showQCModal = ref(false)
const qcResult = ref('pass')
const qcNote = ref('')
const qcLoading = ref(false)

// ════════════════════════════════════════════
// 红冲弹窗
// ════════════════════════════════════════════

const showReverseModal = ref(false)
const reverseReason = ref('')
const reverseLoading = ref(false)

// ════════════════════════════════════════════
// useBillForm composable
// ════════════════════════════════════════════

async function createWithStatus(data: PurchaseInboundPayload & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await inboundApi.create(payload)
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await inboundApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: PurchaseInboundPayload & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await inboundApi.update(id, payload)
  if (status === 1) {
    await inboundApi.submit(id)
  }
  return res?.data || res
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
  totalTaxAmount,
  totalWithTax,
} = useBillForm({
  billPrefix: 'CGRK',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await inboundApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        date: data.inboundDate || '',
        sourceOrderId: data.orderId || undefined,
        sourceOrderNo: data.orderNo || '',
        purchaserId: data.purchaserId || data.buyerId,
        purchaserName: data.purchaserName || data.buyerName || '',
        departmentId: data.departmentId || undefined,
        departmentName: data.departmentName || '',
        trackingNumber: data.trackingNumber || '',
        logisticsCompany: data.logisticsCompany || '',
        internalNote: data.internalNote || '',
        approveTime: data.approveTime || data.approvedTime || '',
        receiveTime: data.receiveTime || '',
        qualityCheckTime: data.qualityCheckTime || '',
        warehouseConfirmTime: data.warehouseConfirmTime || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || d.itemCode || '',
          specification: d.productSpec || d.specification || '',
          unit: d.productUnit || d.unit || '',
          quantity: d.orderQuantity ?? d.inboundQuantity ?? d.quantity ?? 0,
          amount: (d.orderQuantity ?? d.inboundQuantity ?? d.quantity ?? 0) * (d.unitPrice || 0),
          costPrice: d.costPrice || d.unitCost || 0,
          costAmount: ((d.orderQuantity ?? d.quantity ?? 0) || 0) * (d.costPrice || d.unitCost || 0),
          batchNo: d.batchNo || '',
          productionDate: d.productionDate || '',
          expiryDate: d.validityDate || d.expiryDate || '',
          shelfLife: d.shelfLife || '',
          location: d.location || '',
          orderItemId: d.orderItemId || undefined,
        })),
      }
    },
  },
  redirectPath: '/purchase/inbound',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  fields: [
    { key: 'supplierId', label: '供应商', type: 'select', required: true },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true },
    { key: 'purchaserId', label: '采购员', type: 'select', required: true },
    { key: 'date', label: '入库日期', type: 'date', required: true },
    { key: 'inboundType', label: '入库类型', type: 'select', required: true },
  ],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', model: '',
    origin: '', brand: '', unit: '', smallUnit: '',
    batchNo: '', productionDate: '', expiryDate: '',
    shelfLife: '', location: '',
    quantity: 0, unitPrice: 0, amount: 0,
    taxRate: 13, costPrice: 0, costAmount: 0,
    weight: 0, volume: 0, gift: false,
    orderItemId: undefined, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'supplierId') {
      const s = optionRefs.suppliers.find((x: any) => x.id === val)
      fd.supplierName = s?.name || ''
    }
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.name || ''
    }
    if (fieldKey === 'purchaserId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.purchaserName = u?.name || ''
    }
  },
  transformPayload: (fd, status): PurchaseInboundPayload & { status: number } => ({
    status,
    orderId: fd.sourceOrderId || undefined,
    orderNo: fd.sourceOrderNo || undefined,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    purchaserId: fd.purchaserId,
    purchaserName: fd.purchaserName,
    departmentId: fd.departmentId || undefined,
    departmentName: fd.departmentName || undefined,
    inboundDate: fd.date || undefined,
    inboundType: fd.inboundType,
    trackingNumber: fd.trackingNumber || undefined,
    logisticsCompany: fd.logisticsCompany || undefined,
    remark: fd.remark || undefined,
    internalNote: fd.internalNote || undefined,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      orderItemId: p.orderItemId || undefined,
      orderQuantity: p.quantity,
      inboundQuantity: p.inboundQuantity ?? p.quantity,
      unitPrice: p.unitPrice,
      unitCost: p.costPrice,
      taxRate: p.taxRate,
      batchNo: p.batchNo || undefined,
      productionDate: p.productionDate || undefined,
      validityDate: p.expiryDate || undefined,
      shelfLife: p.shelfLife || undefined,
      location: p.location || undefined,
      weight: p.weight || 0,
      volume: p.volume || 0,
      gift: p.gift || false,
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化入库单默认值
if (formData.inboundType === undefined) formData.inboundType = 1
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'supplierName', 'warehouseName', 'purchaserName', 'departmentName',
  'sourceOrderNo', 'trackingNumber', 'logisticsCompany',
  'remark', 'internalNote', 'createTime', 'approveTime',
  'receiveTime', 'qualityCheckTime', 'warehouseConfirmTime',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
if (formData.status === undefined) formData.status = 0

// ════════════════════════════════════════════
// 计算属性
// ════════════════════════════════════════════

const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => INBOUND_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => INBOUND_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// 汇总计算
const totalCostAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.quantity || 0) * (p.costPrice || 0)), 0)
)
const totalWeight = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.weight || 0) * (p.quantity || 0)), 0)
)

const availableActions = computed(() => getAvailableActions(currentStatus.value))

// ════════════════════════════════════════════
// 页眉配置
// ════════════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购入库单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    {
      key: 'source-order',
      label: '选择源单',
      icon: ImportOutlined,
    },
    ...availableActions.value.map((act: any) => ({
      key: act.key,
      label: act.label,
      icon: act.icon,
    })),
    ...(currentStatus.value === 9
      ? [{ key: 'reverse', label: '红冲', icon: SwapOutlined }]
      : []),
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
  ],
}))

// ════════════════════════════════════════════
// 基本信息字段
// ════════════════════════════════════════════

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  {
    key: 'supplierId', label: '供应商', type: 'select', required: true,
    inlineLabel: true, width: 300,
    options: optionRefs.suppliers.map((s: any) => ({ label: s.name, value: s.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  {
    key: 'warehouseId', label: '入库仓库', type: 'select', required: true,
    inlineLabel: true, width: 210,
    options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  {
    key: 'purchaserId', label: '采购员', type: 'select', required: true,
    inlineLabel: true, width: 210,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value,
  },
  { key: 'date', label: '入库日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  {
    key: 'inboundType', label: '入库类型', type: 'select', required: true,
    inlineLabel: true, width: 210,
    options: [
      { label: '采购入库', value: 1 },
      { label: '退货入库', value: 2 },
      { label: '调拨入库', value: 3 },
      { label: '其他入库', value: 4 },
    ],
  },
  { key: 'sourceOrderNo', label: '来源订单号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'departmentName', label: '部门', type: 'input', inlineLabel: true, width: 160 },
  { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
  { key: 'trackingNumber', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'approveTime', label: '审批时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
])

// ════════════════════════════════════════════
// 摘要面板
// ════════════════════════════════════════════

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '入库数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '税额', value: totalTaxAmount.value.toFixed(2) },
  { label: '价税合计', value: totalWithTax.value.toFixed(2), divider: true },
  { label: '成本金额', value: totalCostAmount.value.toFixed(2) },
  { label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
  { label: '总件数', value: formData.products.filter((p: any) => p.productId != null).length },
])

// ════════════════════════════════════════════
// 页脚
// ════════════════════════════════════════════

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '价税合计',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交审批',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ════════════════════════════════════════════
// 明细表格列（含批次/货位/质检等字段）
// ════════════════════════════════════════════

const detailColumns: DetailColumnConfig[] = [
  // ── 固定列 ──
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },

  // ── 商品信息 ──
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },

  // ── 单位 ──
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },

  // ── 批次/生产日期/保质期（入库时必须） ──
  { key: 'batchNo', title: '批次号', type: 'input', width: 120, placeholder: '请输入批次号' },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期(天)', type: 'input', width: 100, defaultHidden: true },

  // ── 货位 ──
  { key: 'location', title: '货位', type: 'input', width: 100, placeholder: '如A-01-01' },

  // ── 数量/包装 ──
  { key: 'quantity', title: '入库数量', type: 'number', width: 100, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, defaultHidden: true },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, defaultHidden: true },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, defaultHidden: true },

  // ── 价格 ──
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'taxRate', title: '税率%', type: 'number', width: 80, precision: 1, min: 0, max: 100 },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },

  // ── 物理属性 ──
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'gift', title: '赠品', type: 'checkbox', width: 60 },

  // ── 质检（显示用） ──
  { key: 'qualityStatus', title: '质检状态', type: 'input', width: 80, readonly: true, defaultHidden: true },
  { key: 'qualityNote', title: '质检说明', type: 'input', width: 120, readonly: true, defaultHidden: true },

  // ── 自定义字段 ──
  { key: 'customField1', title: '自定义1(数字)', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'customField2', title: '自定义2(文本)', type: 'input', width: 120, defaultHidden: true },
  { key: 'customField3', title: '自定义3(文本)', type: 'input', width: 120, defaultHidden: true },

  // ── 备注 ──
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
  { key: 'costAmount', value: totalCostAmount.value.toFixed(2) },
])

// ════════════════════════════════════════════
// 事件处理
// ════════════════════════════════════════════

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.model = p.model || ''
    row.unit = p.unit || ''
    row.smallUnit = p.smallUnit || ''
    row.brand = p.brand || ''
    row.origin = p.origin || ''
    row.unitPrice = p.purchasePrice || p.price || 0
    row.costPrice = p.costPrice || 0
    row.amount = (row.quantity || 0) * (row.unitPrice || 0)
    row.costAmount = (row.quantity || 0) * (row.costPrice || 0)
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  // 数量/单价变化时自动计算金额和成本
  if (['quantity', 'unitPrice'].includes(fieldKey)) {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
  }
  if (['quantity', 'costPrice'].includes(fieldKey)) {
    record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'sourceOrderNo' || fieldKey === 'source-order') {
    showOrderSelect.value = true
    loadOrders()
  } else {
    message.info(`${fieldKey} 快速查询功能暂不可用`)
  }
}

// ════════════════════════════════════════════
// 操作处理
// ════════════════════════════════════════════

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/purchase/inbound')
      break
    case 'source-order':
      showOrderSelect.value = true
      loadOrders()
      break
    case 'submit':
      handleSubmit()
      break
    case 'approve':
      handleApprove()
      break
    case 'reject':
      handleReject()
      break
    case 'receive':
      handleReceive()
      break
    case 'quality-check':
      showQCModal.value = true
      qcResult.value = 'pass'
      qcNote.value = ''
      break
    case 'confirm-warehouse':
      handleConfirmWarehouse()
      break
    case 'complete':
      handleComplete()
      break
    case 'reverse':
      showReverseModal.value = true
      reverseReason.value = ''
      break
  }
}

// ── 审核 ──
function handleApprove() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认审核',
    content: `确认审核通过入库单 ${formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.approve(id)
        message.success('审核成功')
        if (route.query.id) {
          // 重新加载详情
          const res: any = await inboundApi.getById(id)
          const data = res?.data || res || {}
          Object.assign(formData, { ...data, status: 2 })
        } else {
          router.push('/purchase/inbound')
        }
      } catch (err: any) {
        message.error(err?.message || '审核失败')
      }
    },
  })
}

// ── 驳回 ──
function handleReject() {
  const id = Number(formData.id)
  if (!id) return
  let reason = ''
  Modal.confirm({
    title: '驳回入库单',
    content: h('div', [
      h('p', `确认驳回入库单 ${formData.orderNo} 吗？`),
      h('a-input', {
        placeholder: '请输入驳回原因',
        value: reason,
        'onUpdate:value': (v: string) => { reason = v },
      }),
    ]),
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.reject(id, reason || '驳回')
        message.success('已驳回')
        router.push('/purchase/inbound')
      } catch (err: any) {
        message.error(err?.message || '驳回失败')
      }
    },
  })
}

// ── 收货确认 ──
function handleReceive() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认收货',
    content: `确认已收到入库单 ${formData.orderNo} 的商品吗？`,
    okText: '确认收货',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.receive(id)
        message.success('收货成功')
        formData.status = 4
        formData.receiveTime = new Date().toISOString()
      } catch (err: any) {
        message.error(err?.message || '收货失败')
      }
    },
  })
}

// ── 质检确认 ──
async function confirmQC() {
  if (!qcResult.value) {
    message.warning('请选择质检结果')
    return
  }
  const id = Number(formData.id)
  if (!id) return
  qcLoading.value = true
  try {
    await inboundApi.qualityCheck(id, qcResult.value)
    message.success('质检完成')
    formData.status = 6
    formData.qualityCheckTime = new Date().toISOString()
    showQCModal.value = false
  } catch (err: any) {
    message.error(err?.message || '质检提交失败')
  } finally {
    qcLoading.value = false
  }
}

// ── 入库确认 ──
function handleConfirmWarehouse() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '入库确认',
    content: '确认将质检合格的商品正式入库？此操作将增加库存并产生应付账款。',
    okText: '确认入库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.confirmWarehouse(id)
        message.success('入库确认成功，已更新库存')
        formData.status = 8
        formData.warehouseConfirmTime = new Date().toISOString()
      } catch (err: any) {
        message.error(err?.message || '入库确认失败')
      }
    },
  })
}

// ── 完成 ──
function handleComplete() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '完成入库单',
    content: `确认完成入库单 ${formData.orderNo} 吗？`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.complete(id)
        message.success('入库单已完成')
        formData.status = 9
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    },
  })
}

// ── 红冲 ──
async function confirmReverse() {
  if (!reverseReason.value) {
    message.warning('请输入红冲原因')
    return
  }
  const id = Number(formData.id)
  if (!id) return
  reverseLoading.value = true
  try {
    await inboundApi.cancel(id, reverseReason.value)
    message.success('红冲成功，已生成冲减单据')
    showReverseModal.value = false
    router.push('/purchase/inbound')
  } catch (err: any) {
    message.error(err?.message || '红冲失败')
  } finally {
    reverseLoading.value = false
  }
}

// ════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════

const getOrderStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'green', 4: 'red', 5: 'processing', 6: 'green' }
  return map[status] || 'default'
}

const getOrderStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '已下达', 4: '已取消', 5: '履行中', 6: '已完成' }
  return map[status] || '未知'
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[采购入库单] ErrorBoundary:', err)
}

// ════════════════════════════════════════════
// 键盘快捷键
// ════════════════════════════════════════════

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

// ════════════════════════════════════════════
// 生命周期
// ════════════════════════════════════════════

onMounted(() => {
  // 编辑模式下不初始化空行
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
  window.addEventListener('keydown', handleKeydown)
  // 如果路由携带 orderId 参数，自动加载源单
  const orderId = route.query.orderId
  if (orderId) {
    request.get(`/erp/purchase/order/${orderId}`).then((res: any) => {
      const order = res?.data || res
      if (order) confirmSourceOrder(order)
    }).catch(() => {
      // 静默失败
    })
  }
})
</script>

<style scoped>
.action-add-btn {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
}

.action-del-btn {
  color: #ff4d4f;
  padding: 0;
  font-size: 14px;
}

.remark-section {
  padding: 4px 0;
}

.remark-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.remark-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  min-width: 60px;
}

.remark-input {
  flex: 1;
}

.doc-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
  font-size: 12px;
  color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.order-select-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
