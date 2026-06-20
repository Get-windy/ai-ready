<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :tabs="tabsConfig"
    :summary="summaryConfig"
    :footer="footerConfig"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <!-- ═══ Zone 3: 商品明细表格 ═══ -->
    <template #detail-table>
      <div class="table-toolbar">
        <a-button type="link" size="small" class="table-settings-btn">
          <SettingOutlined />
        </a-button>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="productColumns"
        :data-source="formData.products"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        :max-height="tableMaxHeight"
      >
        <template #rowNo="{ rowIndex }">
          <span class="row-no">{{ rowIndex + 1 }}</span>
        </template>
        <template #actionCell="{ index }">
          <a-space :size="2">
            <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct">
              <PlusCircleOutlined />
            </a-button>
            <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
              <MinusCircleOutlined />
            </a-button>
          </a-space>
        </template>
        <template #productCell="{ record, index }">
          <div class="product-cell">
            <a-select
              v-model:value="record.productId"
              placeholder="请选择商品"
              show-search
              :filter-option="filterOption"
              style="flex:1"
              :loading="loadingOptions"
              size="small"
              @change="(val: number) => handleProductChange(val, index)"
            >
              <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
                {{ p.name }}
              </a-select-option>
            </a-select>
            <a-switch v-model:checked="record.scanMode" size="small" class="scan-switch" />
          </div>
        </template>
        <template #itemCodeCell="{ record }">
          <a-input v-model:value="record.itemCode" size="small" />
        </template>
        <template #barcodeCell="{ record }">
          <a-input v-model:value="record.barcode" size="small" />
        </template>
        <template #specCell="{ record }">
          <a-input v-model:value="record.specification" size="small" />
        </template>
        <template #locationCell="{ record }">
          <a-input v-model:value="record.location" size="small" />
        </template>
        <template #unitCell="{ record }">
          <a-input v-model:value="record.unit" size="small" />
        </template>
        <template #lineAttrCell="{ record }">
          <a-input v-model:value="record.lineAttribute" size="small" />
        </template>
        <template #batchCodeCell="{ record }">
          <a-input v-model:value="record.batchCode" size="small" />
        </template>
        <template #prodDateCell="{ record }">
          <a-date-picker v-model:value="record.productionDate" size="small" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </template>
        <template #shelfLifeCell="{ record }">
          <a-input v-model:value="record.shelfLife" size="small" />
        </template>
        <template #expDateCell="{ record }">
          <a-date-picker v-model:value="record.expiryDate" size="small" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </template>
        <template #qtyCell="{ record }">
          <a-input-number v-model:value="record.quantity" :min="0" :precision="2" size="small" style="width:100%" />
        </template>
        <template #bigPackCell="{ record }">
          <a-input-number v-model:value="record.bigPack" :min="0" :precision="0" size="small" style="width:100%" />
        </template>
        <template #midPackCell="{ record }">
          <a-input-number v-model:value="record.midPack" :min="0" :precision="0" size="small" style="width:100%" />
        </template>
        <template #smallPackCell="{ record }">
          <a-input-number v-model:value="record.smallPack" :min="0" :precision="0" size="small" style="width:100%" />
        </template>
        <template #summary>
          <div class="table-summary-row">
            <span class="summary-label">合计</span>
            <span></span><span></span><span></span><span></span><span></span>
            <span></span><span></span><span></span><span></span><span></span>
            <span></span><span></span>
            <span class="summary-red">{{ totalQuantity }}</span>
            <span class="summary-red">{{ totalBigPack }}</span>
            <span class="summary-red">{{ totalMidPack }}</span>
            <span class="summary-red">{{ totalSmallPack }}</span>
          </div>
        </template>
      </VxeTableList>

      <div class="table-expand-link">
        <a-button type="link" size="small" @click="expandTable = !expandTable">
          <FullscreenOutlined />
          {{ expandTable ? '表格收起' : '表格展开显示' }}
        </a-button>
      </div>
    </template>

    <!-- ═══ Zone 4 补充: 备注 + 单据信息 ═══ -->
    <template #bottom-extra>
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">单据备注</span>
          <a-input v-model:value="formData.orderRemark" size="small" class="remark-input" />
        </div>
        <div class="remark-row">
          <span class="remark-label">买家备注</span>
          <a-input v-model:value="formData.buyerRemark" size="small" class="remark-input" />
        </div>
      </div>
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
        <span class="doc-info-item">打印次数 0</span>
        <a-button type="link" size="small" class="doc-info-link">打印记录</a-button>
        <span class="doc-info-item">源单 <a-tag size="small">0</a-tag></span>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  FullscreenOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ── 表单数据 ──
const formData = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  customerName: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  salespersonId: undefined as number | undefined,
  salespersonName: '',
  orderDate: '',
  saleType: 1,
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  paymentAccount: undefined as number | undefined,
  logisticsCompany: '',
  logisticsNo: '',
  shippingFee: 0,
  memberCardNo: '',
  memberName: '',
  memberDiscount: 100,
  orderRemark: '',
  buyerRemark: '',
  products: [] as any[],
})

const loadingOptions = ref(false)
const saving = ref(false)
const expandTable = ref(false)
const tableMaxHeight = ref(400)

// ── 下拉选项 ──
const customerOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])
const returnQty = ref(0)
const promoDiscount = ref(0)
const discountAmount = ref(0)
const otherFee = ref(0)

const currentUserName = computed(() => userStore?.userInfo?.name || '')

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售订单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    {
      key: 'print',
      label: '打印(F8)',
      icon: PrinterOutlined,
      children: [
        { key: 'print-order', label: '打印订单' },
        { key: 'print-summary', label: '打印汇总' },
      ],
    },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    {
      key: 'more',
      label: '更多',
      children: [
        { key: 'save-draft', label: '保存草稿' },
        { key: 'copy-order', label: '复制订单' },
        { key: 'export', label: '导出' },
      ],
    },
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'customerId', label: '客户', type: 'select', required: true, placeholder: '请选择客户', options: customerOptions.value.map(c => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '发货仓库', type: 'select', required: true, placeholder: '请选择仓库', options: warehouseOptions.value.map(w => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'salespersonId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: userOptions.value.map(u => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, options: [{ label: '正常销售', value: 1 }, { label: '样品销售', value: 2 }, { label: '促销销售', value: 3 }] },
  { key: 'receiverName', label: '收货人', type: 'input', placeholder: '请输入收货人', searchBtn: 'Q' },
  { key: 'receiverPhone', label: '联系电话', type: 'input', placeholder: '请输入联系电话', width: 'narrow' },
  { key: 'receiverAddress', label: '收货地址', type: 'input', placeholder: '请输入收货地址', width: 'wide' },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  {
    key: 'payment',
    tab: '收款',
    fields: [
      { key: 'paymentAccount', label: '订单账户', type: 'select', placeholder: '请选择', options: accountOptions.value.map(a => ({ label: a.name, value: a.id })), suffixBtn: '+Q' },
      { key: '_orderAmount', label: '订单金额', type: 'input', disabled: true, suffixBtn: '全清', suffixBtnDanger: true },
      { key: '_moreAccount', label: '更多账户', type: 'input', disabled: true, suffixBtn: '···' },
      { key: '_useAdvance', label: '使用预订货款', type: 'input', disabled: true, suffixBtn: '···' },
      { key: '_prevAdvance', label: '此前预收', type: 'input', disabled: true },
      { key: '_advanceBalance', label: '预收余额', type: 'input', disabled: true },
    ],
  },
  {
    key: 'logistics',
    tab: '物流信息',
    fields: [
      { key: 'logisticsCompany', label: '物流公司', type: 'input', placeholder: '请输入物流公司' },
      { key: 'logisticsNo', label: '物流单号', type: 'input', placeholder: '请输入物流单号' },
      { key: 'shippingFee', label: '运费', type: 'number', placeholder: '请输入运费' },
    ],
  },
  {
    key: 'member',
    tab: '会员信息',
    fields: [
      { key: 'memberCardNo', label: '会员卡号', type: 'input', placeholder: '请输入会员卡号' },
      { key: 'memberName', label: '会员姓名', type: 'input', placeholder: '请输入会员姓名' },
      { key: 'memberDiscount', label: '会员折扣', type: 'number', placeholder: '请输入折扣' },
    ],
  },
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '销售数量', value: totalQuantity.value },
  { label: '退货数量', value: returnQty.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '促销优惠', value: promoDiscount.value.toFixed(2) },
  { label: '优惠金额', value: discountAmount.value.toFixed(2), divider: true, showMore: true },
  { label: '其他费用', value: otherFee.value.toFixed(2), divider: true, showMore: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalAmountWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 商品明细列配置（18 列）
// ═══════════════════════════════════════

const productColumns = [
  { field: 'rowNo', title: '', width: 40, fixed: 'left', slotName: 'rowNo' },
  { field: 'action', title: '操作', width: 70, fixed: 'left', slotName: 'actionCell' },
  { field: 'productName', title: '商品名称', width: 200, slotName: 'productCell' },
  { field: 'itemCode', title: '货号', width: 100, slotName: 'itemCodeCell' },
  { field: 'barcode', title: '条码', width: 120, slotName: 'barcodeCell' },
  { field: 'specification', title: '规格', width: 100, slotName: 'specCell' },
  { field: 'location', title: '货位', width: 80, slotName: 'locationCell' },
  { field: 'unit', title: '计价单位', width: 80, slotName: 'unitCell' },
  { field: 'lineAttribute', title: '商品行属性', width: 100, slotName: 'lineAttrCell' },
  { field: 'batchCode', title: '批次条码', width: 120, slotName: 'batchCodeCell' },
  { field: 'productionDate', title: '生产日期', width: 110, slotName: 'prodDateCell' },
  { field: 'shelfLife', title: '保质期', width: 80, slotName: 'shelfLifeCell' },
  { field: 'expiryDate', title: '到期日期', width: 110, slotName: 'expDateCell' },
  { field: 'quantity', title: '件散数量', width: 90, slotName: 'qtyCell' },
  { field: 'bigPack', title: '大包装', width: 80, slotName: 'bigPackCell' },
  { field: 'midPack', title: '中包装', width: 80, slotName: 'midPackCell' },
  { field: 'smallPack', title: '小包装', width: 80, slotName: 'smallPackCell' },
]

// ── 计算属性 ──
const totalQuantity = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0), 0))
const totalBigPack = computed(() => formData.products.reduce((s, p) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s, p) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s, p) => s + (p.smallPack || 0), 0))
const totalAmount = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0) * (p.unitPrice || 0), 0))
const taxAmount = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value - discountAmount.value + otherFee.value)

// ── 工具函数 ──
function generateOrderNo() {
  const d = new Date()
  const ds = d.toISOString().slice(0, 10).replace(/-/g, '')
  const r = Math.random().toString(36).substring(2, 6).toUpperCase()
  formData.orderNo = `XSDD-${ds}-${r}`
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children?.[0]?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// ── 加载选项 ──
async function loadOptions() {
  loadingOptions.value = true
  try {
    const [customers, warehouses, users, products] = await Promise.all([
      optionsApi.getCustomers().catch(() => []),
      optionsApi.getWarehouses().catch(() => []),
      optionsApi.getUsers('salesman').catch(() => []),
      optionsApi.getProducts().catch(() => []),
    ])
    customerOptions.value = customers || []
    warehouseOptions.value = warehouses || []
    userOptions.value = users || []
    productOptions.value = products || []
  } finally {
    loadingOptions.value = false
  }
}

// ── 事件处理 ──
function handleFieldChange(fieldKey: string, val: any) {
  if (fieldKey === 'customerId') {
    const c = customerOptions.value.find(x => x.id === val)
    if (c) {
      formData.customerName = c.name
      formData.receiverName = c.contactName || ''
      formData.receiverPhone = c.contactPhone || ''
      formData.receiverAddress = c.address || ''
    }
  }
}

function handleProductChange(val: number, index: number) {
  const p = productOptions.value.find(x => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.productName = p.name || ''
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitPrice = p.salePrice || p.price || 0
    row.taxRate = 13
  }
}

function handleAddProduct() {
  formData.products.push({
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    productName: '',
    itemCode: '',
    barcode: '',
    specification: '',
    location: '',
    unit: '',
    lineAttribute: '',
    batchCode: '',
    productionDate: '',
    shelfLife: '',
    expiryDate: '',
    quantity: 0,
    bigPack: 0,
    midPack: 0,
    smallPack: 0,
    unitPrice: 0,
    taxRate: 13,
    scanMode: false,
    remark: '',
  })
}

function handleRemoveProduct(index: number) {
  formData.products.splice(index, 1)
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history':
      router.push('/erp/sale')
      break
    case 'save-draft':
      handleSaveDraft()
      break
    case 'print-order':
    case 'print-summary':
      message.info('打印功能开发中')
      break
    case 'import':
    case 'copy-order':
    case 'export':
      message.info(`${actionKey} 功能开发中`)
      break
  }
}

// ── 验证 & 提交 ──
function validate(): boolean {
  if (!formData.customerId) { message.warning('请选择客户'); return false }
  if (!formData.warehouseId) { message.warning('请选择发货仓库'); return false }
  if (!formData.salespersonId) { message.warning('请选择经手人'); return false }
  if (!formData.orderDate) { message.warning('请选择单据日期'); return false }
  if (formData.products.length === 0) { message.warning('请添加商品明细'); return false }
  return true
}

function buildPayload(status: number) {
  return {
    ...formData,
    status,
    totalAmount: totalAmount.value,
    taxAmount: taxAmount.value,
    finalAmount: totalAmountWithTax.value,
    details: formData.products.map(p => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      specification: p.specification,
      quantity: p.quantity,
      unit: p.unit,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      totalAmount: (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100),
      bigPack: p.bigPack,
      midPack: p.midPack,
      smallPack: p.smallPack,
      barcode: p.barcode,
      batchCode: p.batchCode,
      productionDate: p.productionDate,
      shelfLife: p.shelfLife,
      expiryDate: p.expiryDate,
      location: p.location,
      remark: p.remark,
    })),
  }
}

async function doSubmit(status: number) {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(status)
    if (route.query.id) {
      await saleOrderApi.update(Number(route.query.id), payload)
    } else {
      await saleOrderApi.create(payload)
    }
    message.success(status === 0 ? '保存草稿成功' : '提交成功')
    router.push('/erp/sale')
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function handleSaveDraft() { doSubmit(0) }
function handleSubmit() { doSubmit(1) }

// ── 快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSaveDraft() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
}

// ── 生命周期 ──
onMounted(() => {
  generateOrderNo()
  loadOptions()
  document.addEventListener('keydown', handleKeydown)
  // 编辑模式加载
  const editId = route.params.id || route.query.id
  if (editId) {
    saleOrderApi.getById(Number(editId)).then((res: any) => {
      const data = res?.data || res
      if (data) Object.assign(formData, data)
    }).catch(() => {})
  }
  // 预填充空行
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  // 计算表格最大高度
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ═══ 表格工具栏 ═══ */
.table-toolbar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.table-settings-btn {
  color: #ff4d4f;
  font-size: 16px;
}

.row-no {
  color: #8c8c8c;
  font-size: 12px;
}

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

.product-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.scan-switch {
  flex-shrink: 0;
}

.table-expand-link {
  text-align: center;
  padding: 4px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

/* 合计行 */
.table-summary-row {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.summary-label {
  font-weight: 700;
  color: #262626;
  min-width: 60px;
}

.summary-red {
  color: #ff4d4f;
  font-weight: 700;
  min-width: 50px;
  text-align: right;
}

/* 备注区 */
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

/* 单据信息行 */
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
</style>
