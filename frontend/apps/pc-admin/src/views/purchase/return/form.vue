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
            v-model:data-source="formData.products"
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
          </BillDetailTable>
        </template>

        <!-- ═══ 底部备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">退货原因</span>
              <a-input
                v-model:value="formData.reason"
                size="small"
                class="remark-input"
                placeholder="请输入退货原因"
                :disabled="isLocked"
              />
            </div>
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
            <span class="doc-info-item">
              源单 <a-tag>{{ formData.sourceOrderNo || '-' }}</a-tag>
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

      <!-- ═══ 驳回原因弹窗 ═══ -->
      <a-modal
        v-model:open="showRejectModal"
        title="驳回退货单"
        :width="420"
        @ok="confirmReject"
        :confirm-loading="rejectLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="驳回原因" required>
            <a-input v-model:value="rejectReason" placeholder="请输入驳回原因" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 完成出库确认弹窗 ═══ -->
      <a-modal
        v-model:open="showCompleteModal"
        title="完成退货出库"
        :width="420"
        @ok="confirmComplete"
        :confirm-loading="completeLoading"
      >
        <p style="color:#722ed1;font-size:12px;">
          确认执行退还供应商操作？此操作将从出库仓库减少库存。
        </p>
      </a-modal>

      <!-- ═══ 配置弹窗（顶部齿轮触发）：页面配置 / 录单默认值 / 打印设置 ═══ -->
      <FormPageConfigModal
        v-model:open="showFormConfig"
        :fields="pageConfigFields"
        :table-columns="pageConfigTableColumns"
        v-model:print-config="printConfig"
        @field-visible-change="setFieldVisible"
        @field-enter-jump-change="setEnterJump"
        @field-display-name-change="setDisplayName"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, h, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  ImportOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useFormPageConfig } from '@/components/BillFormPage/useFormPageConfig'
import FormPageConfigModal from '@/components/BillFormPage/FormPageConfigModal.vue'
import { purchaseReturnApi } from '@/api/erp'
import { PRODUCT_PURCHASE_DEFAULTS } from '@/utils/productDefaults'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'PurchaseReturnForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
// ═══ 状态枚举 ═══
const RETURN_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已驳回', color: 'red' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已取消', color: 'error' },
}

function getAvailableActions(status: number) {
  switch (status) {
    case 0: return [
      { key: 'submit', label: '提交审批', icon: CheckOutlined },
    ]
    case 1: return [
      { key: 'approve', label: '审核通过', icon: CheckOutlined },
      { key: 'reject', label: '驳回', icon: CloseCircleOutlined },
    ]
    case 2: return [
      { key: 'complete', label: '完成出库', icon: CheckOutlined },
    ]
    case 3:
    case 4:
    case 5: return []
    default: return []
  }
}

// ═══ 源单选择相关状态 ═══
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
  formData.supplierNo = order.supplierNo || ''
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
      formData.products.splice(0, formData.products.length)
      items.forEach((item: any) => {
        const qty = (item.quantity || 0) - (item.receivedQuantity || 0) - (item.inboundQuantity || 0)
        const returnQty = qty > 0 ? qty : (item.quantity || 0)
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
          quantity: returnQty,
          unitPrice: item.unitPrice || 0,
          amount: returnQty * (item.unitPrice || 0),
          taxRate: item.taxRate ?? 13,
          costPrice: item.costPrice || 0,
          costAmount: returnQty * (item.costPrice || 0),
          orderItemId: item.id || item.lineNo,
          gift: item.gift || false,
          weight: item.weight || 0,
          volume: item.volume || 0,
          remark: item.remark || '',
        })
      })
      message.success(`已加载订单 ${order.orderNo}，共 ${formData.products.length} 条待退明细`)
    }
  } catch {
    message.warning('加载订单明细失败，请手动添加商品')
  }
}

// ═══ 驳回 / 完成出库弹窗 ═══
const showRejectModal = ref(false)
const rejectReason = ref('')
const rejectLoading = ref(false)
const showCompleteModal = ref(false)
const completeLoading = ref(false)

// ═══ useBillForm ═══
async function createWithStatus(data: any & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await purchaseReturnApi.create(payload)
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await purchaseReturnApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await purchaseReturnApi.update(id, payload)
  if (status === 1) {
    await purchaseReturnApi.submit(id)
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
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
  totalTaxAmount,
  totalWithTax,
} = useBillForm({
  billPrefix: 'PR',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await purchaseReturnApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        returnNo: data.returnNo,
        date: data.returnDate || '',
        sourceOrderId: data.purchaseOrderId || undefined,
        sourceOrderNo: data.purchaseOrderNo || '',
        supplierNo: data.supplierNo || '',
        bankName: data.bankName || '',
        bankAccount: data.bankAccount || '',
        taxNo: data.taxNo || '',
        contactName: data.contactName || '',
        contactPhone: data.contactPhone || '',
        contactAddress: data.contactAddress || '',
        supplierRemark: data.supplierRemark || '',
        departmentName: data.departmentName || '',
        summary: data.summary || '',
        extNum1: data.extNum1 || undefined,
        extNum2: data.extNum2 || undefined,
        extText1: data.extText1 || '',
        extText2: data.extText2 || '',
        extText3: data.extText3 || '',
        paymentAccount: data.paymentAccount || '',
        paymentAmount: data.paymentAmount || 0,
        moreAccounts: data.moreAccounts || '',
        prevPrepaid: data.prevPrepaid || 0,
        refundPrepay: data.refundPrepay || 0,
        prepaidBalance: data.prepaidBalance || 0,
        currentDebt: data.currentDebt || 0,
        prevDebt: data.prevDebt || 0,
        debtBalance: data.debtBalance || 0,
        paymentDeadline: data.paymentDeadline || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || d.itemCode || '',
          specification: d.productSpec || d.specification || '',
          unit: d.productUnit || d.unit || '',
          quantity: d.returnQuantity ?? d.quantity ?? 0,
          amount: (d.returnQuantity ?? d.quantity ?? 0) * (d.unitPrice || 0),
          costPrice: d.costPrice || d.unitCost || 0,
          costAmount: ((d.returnQuantity ?? d.quantity ?? 0) || 0) * (d.costPrice || d.unitCost || 0),
          batchNo: d.batchNo || '',
          productionDate: d.productionDate || '',
          expiryDate: d.expiryDate || '',
          shelfLife: d.shelfLife || '',
          location: d.location || '',
          orderItemId: d.orderItemId || undefined,
        })),
      }
    },
  },
  redirectPath: '/purchase/return',
  codeApiPath: '/erp/purchase/return/next-no',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  fields: [
    { key: 'supplierId', label: '供应商', type: 'select', required: true },
    { key: 'warehouseId', label: '出库仓库', type: 'select', required: true },
    { key: 'purchaserId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'returnType', label: '退货类型', type: 'select', required: true },
  ],
  productDefaults: PRODUCT_PURCHASE_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'supplierId') {
      const s: any = optionRefs.suppliers.find((x: any) => x.id === val)
      fd.supplierName = s?.name || ''
      fd.supplierNo = s?.code || s?.supplierNo || s?.number || ''
      fd.bankName = s?.bankName || ''
      fd.bankAccount = s?.bankAccount || ''
      fd.taxNo = s?.taxNo || ''
      fd.contactName = s?.contactName || s?.linkman || ''
      fd.contactPhone = s?.contactPhone || s?.phone || ''
      fd.contactAddress = s?.contactAddress || s?.address || ''
    }
    if (fieldKey === 'warehouseId') {
      const w: any = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.name || ''
    }
    if (fieldKey === 'purchaserId') {
      const u: any = optionRefs.users.find((x: any) => x.id === val)
      fd.purchaserName = u?.name || ''
    }
  },
  transformPayload: (fd, status): any => ({
    status,
    purchaseOrderId: fd.sourceOrderId || undefined,
    purchaseOrderNo: fd.sourceOrderNo || undefined,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    supplierNo: fd.supplierNo || undefined,
    bankName: fd.bankName || undefined,
    bankAccount: fd.bankAccount || undefined,
    taxNo: fd.taxNo || undefined,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    purchaserId: fd.purchaserId,
    purchaserName: fd.purchaserName,
    departmentId: fd.departmentId || undefined,
    departmentName: fd.departmentName || undefined,
    returnDate: fd.date || undefined,
    returnType: fd.returnType,
    contactName: fd.contactName || undefined,
    contactPhone: fd.contactPhone || undefined,
    contactAddress: fd.contactAddress || undefined,
    supplierRemark: fd.supplierRemark || undefined,
    summary: fd.summary || undefined,
    extNum1: fd.extNum1 || undefined,
    extNum2: fd.extNum2 || undefined,
    extText1: fd.extText1 || undefined,
    extText2: fd.extText2 || undefined,
    extText3: fd.extText3 || undefined,
    paymentAccount: fd.paymentAccount || undefined,
    paymentAmount: fd.paymentAmount || undefined,
    moreAccounts: fd.moreAccounts || undefined,
    prevPrepaid: fd.prevPrepaid || undefined,
    refundPrepay: fd.refundPrepay || undefined,
    prepaidBalance: fd.prepaidBalance || undefined,
    currentDebt: fd.currentDebt || undefined,
    prevDebt: fd.prevDebt || undefined,
    debtBalance: fd.debtBalance || undefined,
    paymentDeadline: fd.paymentDeadline || undefined,
    remark: fd.remark || undefined,
    reason: fd.reason || undefined,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      image: p.image || undefined,
      barcode: p.barcode || undefined,
      model: p.model || undefined,
      origin: p.origin || undefined,
      brand: p.brand || undefined,
      region: p.region || undefined,
      location: p.location || undefined,
      availableStock: p.availableStock || undefined,
      availableStockConverted: p.availableStockConverted || undefined,
      bookStock: p.bookStock || undefined,
      batchNo: p.batchNo || undefined,
      batchCode: p.batchCode || undefined,
      productionDate: p.productionDate || undefined,
      shelfLife: p.shelfLife || undefined,
      expiryDate: p.expiryDate || undefined,
      returnQuantity: p.quantity,
      conversionRelation: p.conversionRelation || undefined,
      pieceQuantity: p.pieceQuantity || undefined,
      bigPack: p.bigPack || undefined,
      midPack: p.midPack || undefined,
      smallPack: p.smallPack || undefined,
      latestPurchaseDate: p.latestPurchaseDate || undefined,
      retailPrice: p.retailPrice || undefined,
      wholesalePrice: p.wholesalePrice || undefined,
      unitPrice: p.unitPrice,
      smallUnit: p.smallUnit || undefined,
      smallUnitPrice: p.smallUnitPrice || undefined,
      smallUnitQuantity: p.smallUnitQuantity || undefined,
      unitCost: p.costPrice || p.unitCost || undefined,
      costAmount: p.costAmount || undefined,
      taxRate: p.taxRate,
      volume: p.volume || undefined,
      weight: p.weight || undefined,
      gift: p.gift || false,
      restaurant: p.restaurant || false,
      canteen: p.canteen || false,
      outRestaurant: p.outRestaurant || false,
      vipSelf: p.vipSelf || false,
      largeGroup: p.largeGroup || false,
      vipLevel1: p.vipLevel1 || false,
      vipLevel2: p.vipLevel2 || false,
      specialCustomer: p.specialCustomer || false,
      customField1: p.customField1 || undefined,
      customField2: p.customField2 || undefined,
      customField3: p.customField3 || undefined,
      customField4: p.customField4 || undefined,
      customField5: p.customField5 || undefined,
      customField6: p.customField6 || undefined,
      customField7: p.customField7 || undefined,
      customField8: p.customField8 || undefined,
      customField9: p.customField9 || undefined,
      customField10: p.customField10 || undefined,
      reason: p.reason || undefined,
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化退货单默认值
if (formData.returnType === undefined) formData.returnType = 1
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'supplierName', 'supplierNo', 'bankName', 'bankAccount', 'taxNo', 'warehouseName', 'purchaserName',
  'departmentName', 'sourceOrderNo', 'contactName', 'contactPhone', 'contactAddress', 'supplierRemark',
  'summary', 'reason', 'remark', 'paymentAccount', 'moreAccounts', 'createTime', 'approveTime',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
if (formData.status === undefined) formData.status = 0

// ═══ 计算属性 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => RETURN_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => RETURN_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

const totalCostAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.quantity || 0) * (p.costPrice || 0)), 0)
)
const totalWeight = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.weight || 0) * (p.quantity || 0)), 0)
)

const availableActions = computed(() => getAvailableActions(currentStatus.value))

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购退货单',
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
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（页面配置35字段，静态定义；动态选项由 decorate 注入）═══
const BASE_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'supplierId', label: '供应商', type: 'select', required: true, inlineLabel: true, width: 300 },
  { key: 'supplierNo', label: '供应商编号', type: 'input', inlineLabel: true, width: 160, disabled: true },
  { key: 'bankName', label: '开户行', type: 'input', inlineLabel: true, width: 210 },
  { key: 'bankAccount', label: '银行账号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'taxNo', label: '税号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 210 },
  { key: 'purchaserId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210 },
  { key: 'departmentName', label: '部门', type: 'input', inlineLabel: true, width: 160 },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'returnType', label: '退货类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [
    { label: '质量退货', value: 1 }, { label: '数量退货', value: 2 }, { label: '其他退货', value: 3 },
  ]},
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 160 },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 160 },
  { key: 'contactAddress', label: '联系地址', type: 'input', inlineLabel: true, width: 300 },
  { key: 'supplierRemark', label: '供应商备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'sourceOrderNo', label: '源单', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  // 自定义字段
  { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', inlineLabel: true, width: 150, precision: 2 },
  { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', inlineLabel: true, width: 150, precision: 2 },
  { key: 'extText1', label: '自定义字段3(文本)', type: 'input', inlineLabel: true, width: 160 },
  { key: 'extText2', label: '自定义字段4(文本)', type: 'input', inlineLabel: true, width: 160 },
  { key: 'extText3', label: '自定义字段5(文本)', type: 'input', inlineLabel: true, width: 160 },
  // 收款区
  { key: 'paymentAccount', label: '收款账户', type: 'select', inlineLabel: true, width: 210 },
  { key: 'paymentAmount', label: '收款金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
  { key: 'moreAccounts', label: '更多账户', type: 'input', inlineLabel: true, width: 130, disabled: true },
  { key: 'prevPrepaid', label: '此前预付', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'refundPrepay', label: '退回预付款', type: 'number', inlineLabel: true, width: 130, precision: 2 },
  { key: 'prepaidBalance', label: '预付余额', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 120, precision: 2 },
  { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'paymentDeadline', label: '付款期限', type: 'date', inlineLabel: true, width: 160 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
]

// ═══ 页面配置弹窗（页面配置/录单默认值/打印设置）═══
// 共用 useFormPageConfig + FormPageConfigModal：此前本页**完全没有**这个弹窗
// （文档要求 35 个可配置字段，落地为 0）
const FORM_CONFIG_MODULE = 'purchase-return-form'
const showFormConfig = ref(false)
const printConfig = reactive({
  template: 'standard', copies: 1, paperSize: 'A4',
  alwaysLastTemplate: false, afterSubmit: false,
})

const {
  basicInfoFields,
  pageConfigFields,
  pageConfigTableColumns,
  loadConfig: loadFormConfig,
  setFieldVisible,
  setEnterJump,
  setDisplayName,
} = useFormPageConfig({
  baseFields: BASE_INFO_FIELDS,
  module: FORM_CONFIG_MODULE,
  decorate: (f) => {
    switch (f.key) {
      case 'supplierId':
        return { options: (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value }
      case 'warehouseId':
        return { options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value }
      case 'purchaserId':
        return { options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value }
      case 'paymentAccount':
        return { options: (optionRefs.accounts || []).map((a: any) => ({ label: a.name, value: a.id })) }
      default:
        return {}
    }
  },
  collectDefaults: () => ({ ...printConfig }),
  applyDefaults: (d) => {
    if (d.template) printConfig.template = d.template
    if (d.copies != null) printConfig.copies = Number(d.copies)
    if (d.paperSize) printConfig.paperSize = d.paperSize
    printConfig.alwaysLastTemplate = !!d.alwaysLastTemplate
    printConfig.afterSubmit = !!d.afterSubmit
  },
})

// ═══ 摘要面板 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '退货数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '金额', value: totalAmount.value.toFixed(2) },
  { label: '税额', value: totalTaxAmount.value.toFixed(2) },
  { label: '本单金额', value: totalWithTax.value.toFixed(2), divider: true },
  { label: '成本金额', value: totalCostAmount.value.toFixed(2) },
  { label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
  { label: '总件数', value: formData.products.filter((p: any) => p.productId != null).length },
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交审批',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 明细表格列（文档57列） ═══
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: (optionRefs.products || []).map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 90, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 100, placeholder: '如A-01-01' },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  // 库存
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 140, precision: 2, readonly: true, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 2, readonly: true, defaultHidden: true },
  // 批次
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120, defaultHidden: true },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期(天)', type: 'input', width: 100 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  // 数量/包装
  { key: 'quantity', title: '退货数量', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },
  { key: 'latestPurchaseDate', title: '最近采购日期', type: 'date', width: 120, readonly: true, defaultHidden: true },
  // 价格
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'gift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  // 价格等级（标准化产品价格等级，默认隐藏）
  { key: 'restaurant', title: '餐饮店', type: 'checkbox', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'checkbox', width: 90, defaultHidden: true },
  // 单据自定义字段
  { key: 'customField1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField4', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField5', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField6', title: '单据自定义6(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField7', title: '单据自定义7(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField8', title: '单据自定义8(往来单位)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField9', title: '单据自定义9(职员)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField10', title: '单据自定义10(部门)', type: 'input', width: 140, defaultHidden: true },
])

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
  { key: 'costAmount', value: totalCostAmount.value.toFixed(2) },
])

// ═══ 事件处理 ═══
function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p: any = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.model = p.model || ''
      record.unit = p.unit || ''
      record.smallUnit = p.smallUnit || ''
      record.brand = p.brand || ''
      record.origin = p.origin || ''
      record.unitPrice = p.purchasePrice || p.price || 0
      record.costPrice = p.costPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.retailPrice = p.retailPrice || 0
      record.latestPurchaseDate = p.latestPurchaseDate || ''
      record.amount = (record.quantity || 0) * (record.unitPrice || 0)
      record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
    }
  }
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

// ═══ 操作处理 ═══
async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/purchase/return')
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
      showRejectModal.value = true
      rejectReason.value = ''
      break
    case 'complete':
      showCompleteModal.value = true
      break
    case 'config':
      showFormConfig.value = true
      break
  }
}

function handleApprove() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认审核',
    content: `确认审核通过退货单 ${formData.returnNo || formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseReturnApi.approve(id)
        message.success('审核成功')
        if (route.query.id) {
          const res: any = await purchaseReturnApi.getById(id)
          const data = res?.data || res || {}
          Object.assign(formData, { ...data, status: 2 })
        } else {
          router.push('/purchase/return')
        }
      } catch (err: any) {
        message.error(err?.message || '审核失败')
      }
    },
  })
}

async function confirmReject() {
  if (!rejectReason.value) {
    message.warning('请输入驳回原因')
    return
  }
  const id = Number(formData.id)
  if (!id) return
  rejectLoading.value = true
  try {
    await purchaseReturnApi.reject(id, rejectReason.value)
    message.success('已驳回')
    showRejectModal.value = false
    router.push('/purchase/return')
  } catch (err: any) {
    message.error(err?.message || '驳回失败')
  } finally {
    rejectLoading.value = false
  }
}

async function confirmComplete() {
  const id = Number(formData.id)
  if (!id) return
  completeLoading.value = true
  try {
    await purchaseReturnApi.complete(id)
    message.success('退货出库完成，已更新库存')
    showCompleteModal.value = false
    formData.status = 4
  } catch (err: any) {
    message.error(err?.message || '出库完成失败')
  } finally {
    completeLoading.value = false
  }
}

// ═══ 辅助函数 ═══
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
  console.warn('[采购退货] ErrorBoundary:', err)
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

// ═══ 生命周期 ═══
onMounted(() => {
  loadFormConfig()
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    Array.from({ length: 20 }, () => handleAddProduct())
  }
  window.addEventListener('keydown', handleKeydown)
  const orderId = route.query.orderId
  if (orderId) {
    request.get(`/erp/purchase/order/${orderId}`).then((res: any) => {
      const order = res?.data || res
      if (order) confirmSourceOrder(order)
    }).catch(() => { /* 静默失败 */ })
  }
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.order-select-search { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
</style>
