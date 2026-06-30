<template>
  <div class="form-page-container" style="height:100%;display:flex;flex-direction:column;">
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :tabs="tabsConfig"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleHoldOrder"
      @submit="handleSettle"
    >
      <!-- ═══ Zone 3: 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="detailColumns"
          :data-source="formData.products"
          :max-height="tableMaxHeight"
          :summary-columns="tableSummaryColumns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
          @open-select-modal="handleOpenProductSelectModal"
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

      <!-- ═══ Zone 4 补充: 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input v-model:value="formData.orderRemark" size="small" class="remark-input" />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
          <span class="doc-info-item">打印次数 0</span>
          <a-button type="link" size="small" class="doc-info-link">打印记录</a-button>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 会员选择弹窗 ═══ -->
    <a-modal v-model:open="showMemberSearch" title="选择会员" @ok="confirmMemberSelect" width="520px">
      <a-input-search
        v-model:value="memberSearchKeyword"
        placeholder="输入手机号 / 会员卡号 / 姓名搜索"
        enter-button="搜索"
        @search="doMemberSearch"
        style="margin-bottom: 12px"
      />
      <a-list :data-source="memberSearchResults" :loading="memberSearching" size="small">
        <template #renderItem="{ item }">
          <a-list-item
            style="cursor:pointer"
            :class="{ 'member-selected': selectedMemberId === item.id }"
            @click="selectedMemberId = item.id; selectedMember = item"
          >
            <a-list-item-meta
              :title="item.partyName || item.name"
              :description="`手机: ${item.phone || '-'} | 卡号: ${item.memberCardNo || item.member_card_no || '-'}`"
            />
            <template #extra>
              <a-tag v-if="selectedMemberId === item.id" color="blue">已选</a-tag>
            </template>
          </a-list-item>
        </template>
        <template #header>
          <div v-if="memberSearchResults.length === 0 && !memberSearching" style="color:#999;text-align:center;padding:16px 0">
            未找到会员，可继续以散客下单
          </div>
        </template>
      </a-list>
      <div style="margin-top:8px;color:#999;font-size:12px">
        提示：不选择会员则按散客下账
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'

defineOptions({ name: 'RetailForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { retailOrderApi, memberApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)

// ═══ 会员搜索状态 ═══
const showMemberSearch = ref(false)
const memberSearchKeyword = ref('')
const memberSearchResults = ref<any[]>([])
const memberSearching = ref(false)
const selectedMemberId = ref<number | null>(null)
const selectedMember = ref<any>(null)

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const tableMaxHeight = ref(400)
const directDiscount = ref(0)
const couponDiscount = ref(0)
const promoDiscount = ref(0)
const prevPoints = ref(0)

// 支付方式与金额
const cashAmount = ref(0)
const cardAmount = ref(0)
const prepaidAmount = ref(0)
const transferAmount = ref(0)
const changeAmount = ref(0)

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
} = useBillForm({
  billPrefix: 'LSDD',
  api: {
    create: retailOrderApi.create,
    update: retailOrderApi.update,
    getById: (id: number) => retailOrderApi.getDetail(id).then((res: any) => res?.order || res),
  },
  redirectPath: '/sales/retail',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, scanMode: false,
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status: status ?? 1,
    retailNo: fd.orderNo || fd.retailNo || '',
    customerId: fd.customerId,
    customerName: fd.customerName,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    orderDate: fd.orderDate,
    saleType: fd.saleType || 'NORMAL',
    memberCardNo: fd.memberCardNo || '',
    memberName: fd.memberName || '',
    directDiscount: directDiscount.value,
    couponDiscount: couponDiscount.value,
    promoDiscount: promoDiscount.value,
    prevPoints: prevPoints.value,
    payableAmount: payableAmount.value,
    cashAmount: cashAmount.value,
    cardAmount: cardAmount.value,
    prepaidAmount: prepaidAmount.value,
    transferAmount: transferAmount.value,
    combinedPayment: (cashAmount.value > 0 && cardAmount.value > 0) ||
                     (cashAmount.value > 0 && transferAmount.value > 0) ||
                     (cardAmount.value > 0 && transferAmount.value > 0),
    changeAmount: changeAmount.value,
    prepaidBalance: fd.prepaidBalance || 0,
    remark: fd.orderRemark,
    items: fd.products.filter((p: any) => p.productId != null || p.productName).map((p: any) => ({
      productId: p.productId,
      productName: p.productName,
      itemCode: p.itemCode,
      barcode: p.barcode,
      unit: p.unit,
      batchCode: p.batchCode,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      remark: p.remark,
    })),
  }),
})

// 初始化零售单特有字段
if (!('customerId' in formData)) Object.assign(formData, {
  customerId: undefined,
  customerName: '散客',
  warehouseId: undefined,
  warehouseName: '',
  handlerId: undefined,
  handlerName: '',
  orderDate: new Date().toISOString().slice(0, 10),
  saleType: 'NORMAL',
  memberCardNo: '',
  memberName: '',
  orderRemark: '',
  prepaidBalance: 0,
})

// ── 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))

const totalDiscount = computed(() => directDiscount.value + couponDiscount.value + promoDiscount.value)
const payableAmount = computed(() => Math.max(0, totalAmount.value - totalDiscount.value))
const totalPaid = computed(() => cashAmount.value + cardAmount.value + prepaidAmount.value + transferAmount.value)

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value },
  { key: 'midPack', value: totalMidPack.value },
  { key: 'smallPack', value: totalSmallPack.value },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '零售单',
  orderNo: formData.orderNo || formData.retailNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-receipt', label: '打印小票' },
      { key: 'print-summary', label: '打印汇总' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'more', label: '更多', children: [
      { key: 'copy-order', label: '复制零售单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  // 客户/会员行：默认散客，点击Q按钮打开会员搜索弹窗
  {
    key: 'customerName',
    label: '客户/会员',
    type: 'input',
    inlineLabel: true,
    width: 300,
    placeholder: '散客',
    searchBtn: 'Q',
  },
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [{ label: '正常零售', value: 'NORMAL' }, { label: '退货', value: 'RETURN' }] },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '收款', fields: [
    { key: '_cashAmount', label: '现金', type: 'number', placeholder: '0.00', precision: 2 },
    { key: '_cardAmount', label: '银行卡', type: 'number', placeholder: '0.00', precision: 2 },
    { key: '_prepaidAmount', label: '预收款', type: 'number', placeholder: '0.00', precision: 2 },
    { key: '_transferAmount', label: '转账', type: 'number', placeholder: '0.00', precision: 2 },
    { key: '_changeAmount', label: '找零', type: 'number', disabled: true, precision: 2 },
    { key: '_prepaidBalance', label: '预收余额', type: 'number', disabled: true, precision: 2 },
  ]},
  { key: 'discount', tab: '优惠', fields: [
    { key: '_directDiscount', label: '直接优惠', type: 'number', placeholder: '手动折扣金额', precision: 2 },
    { key: '_couponDiscount', label: '优惠券抵扣', type: 'number', placeholder: '0.00', precision: 2 },
    { key: '_promoDiscount', label: '促销优惠', type: 'number', placeholder: '活动折扣', precision: 2 },
    { key: '_prevPoints', label: '此前积分', type: 'number', disabled: true, precision: 0 },
  ]},
  { key: 'member', tab: '会员信息', fields: [
    { key: 'memberCardNo', label: '会员卡号', type: 'input', placeholder: '读卡/输入卡号', suffixBtn: 'Q' },
    { key: 'memberName', label: '会员姓名', type: 'input', disabled: true },
    { key: '_prevPoints', label: '当前积分', type: 'number', disabled: true, precision: 0 },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '零售数量', value: totalQuantity.value, statusLabel: payableAmount.value > 0 ? '待收款' : '已结清' },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '优惠合计', value: totalDiscount.value.toFixed(2), divider: true, showMore: true },
  { label: '应收金额', value: payableAmount.value.toFixed(2), highlight: true },
  { label: '已收金额', value: totalPaid.value.toFixed(2) },
  { label: '找零', value: changeAmount.value.toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单应收',
  amountValue: `¥${payableAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '挂单',
  draftShortcut: 'F3',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置
// ═══════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name,
      value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true,
  },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2 },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 0 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 90, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 120 },
])

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '', barcode: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, scanMode: false,
    productName: '', quantity: 0, remark: '',
  }
  formData.products.splice(index + 1, 0, newProduct)
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  // 处理收款字段
  if (fieldKey === '_cashAmount') cashAmount.value = val || 0
  if (fieldKey === '_cardAmount') cardAmount.value = val || 0
  if (fieldKey === '_prepaidAmount') prepaidAmount.value = val || 0
  if (fieldKey === '_transferAmount') transferAmount.value = val || 0
  // 处理优惠字段
  if (fieldKey === '_directDiscount') directDiscount.value = val || 0
  if (fieldKey === '_couponDiscount') couponDiscount.value = val || 0
  if (fieldKey === '_promoDiscount') promoDiscount.value = val || 0
  // 更新找零
  updateChangeAmount()
}

function updateChangeAmount() {
  const change = totalPaid.value - payableAmount.value
  changeAmount.value = change > 0 ? change : 0
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.unit = p.unit || ''
      record.unitPrice = p.retailPrice || p.salePrice || p.price || 0
    }
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'customerName' || fieldKey === 'memberCardNo') {
    showMemberSearch.value = true
    memberSearchKeyword.value = ''
    memberSearchResults.value = []
    selectedMemberId.value = null
    selectedMember.value = null
  } else {
    message.info(`${fieldKey} 快速查询功能开发中`)
  }
}

// ═══ 会员搜索 ═══
async function doMemberSearch() {
  if (!memberSearchKeyword.value.trim()) return
  memberSearching.value = true
  try {
    memberSearchResults.value = await memberApi.search(memberSearchKeyword.value)
  } catch {
    memberSearchResults.value = []
  } finally {
    memberSearching.value = false
  }
}

function confirmMemberSelect() {
  if (selectedMember.value) {
    const m = selectedMember.value
    formData.customerId = m.id
    formData.customerName = m.partyName || m.name || '会员'
    formData.memberCardNo = m.memberCardNo || m.member_card_no || ''
    formData.memberName = m.partyName || m.name || ''
    prevPoints.value = m.points || 0
    message.success(`已选择会员：${formData.customerName}`)
  } else {
    // 未选择，保持散客
    formData.customerId = undefined
    formData.customerName = '散客'
    formData.memberCardNo = ''
    formData.memberName = ''
  }
  showMemberSearch.value = false
}

// ═══ 产品选择弹窗 ═══
function handleOpenProductSelectModal(record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length
  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) handleAddProduct()
  }
  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      const row = formData.products[rowIndex]
      row.productId = p.id
      row.productName = p.name || ''
      row.itemCode = p.code || ''
      row.barcode = p.barcode || ''
      row.unit = p.unit || ''
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
    }
  })
  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

// ═══ 挂单（F3）═══════
function handleHoldOrder() {
  handleSaveDraft()
  message.info('零售单已挂单，可在列表中查看')
}

// ═══ 记账（Ctrl+Enter）═══════
function handleSettle() {
  if (payableAmount.value > 0 && totalPaid.value < payableAmount.value) {
    message.warning(`收款不足：应收 ${payableAmount.value.toFixed(2)}，已收 ${totalPaid.value.toFixed(2)}`)
    return
  }
  handleSubmit()
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history':
      router.push('/sales/retail')
      break
    case 'print-receipt':
    case 'print-summary':
      message.info('打印功能开发中')
      break
    case 'copy-order':
    case 'export':
      message.info(`${actionKey} 功能开发中`)
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(async () => {
  // 初始化默认散客
  try {
    const walkin = await memberApi.getWalkIn()
    if (walkin && !formData.customerId) {
      formData.customerId = walkin.id
      formData.customerName = walkin.partyName || '散客'
    }
  } catch {
    // 获取散客失败，使用默认
    formData.customerName = '散客'
  }

  if (formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
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
}

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}

.member-selected {
  background: #e6f7ff;
}
</style>
