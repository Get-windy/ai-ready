<template>
  <div
    class="form-page-container"
    style="height:100%;display:flex;flex-direction:column;"
  >
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
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ 换入仓库数据表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换入仓库数据表</span>
            <span class="warehouse-count">{{ formData.inWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            :columns="inWarehouseColumns"
            :data-source="formData.inWarehouseItems"
            :max-height="tableMaxHeight"
            :summary-columns="inWarehouseSummaryColumns"
            @cell-change="handleInWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenInWarehouseProductModal"
          >
            <!-- 操作列 -->
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleInsertInWarehouseProduct(index)"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    @click="handleRemoveInWarehouseProduct(index)"
                  >
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleAddInWarehouseProduct()"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    disabled
                  >
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
          </BillDetailTable>
        </div>

        <!-- ═══ 换出仓库数据表格 ═══ -->
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换出仓库数据表</span>
            <span class="warehouse-count">{{ formData.outWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            :columns="outWarehouseColumns"
            :data-source="formData.outWarehouseItems"
            :max-height="tableMaxHeight"
            :summary-columns="outWarehouseSummaryColumns"
            @cell-change="handleOutWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenOutWarehouseProductModal"
          >
            <!-- 操作列 -->
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleInsertOutWarehouseProduct(index)"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    @click="handleRemoveOutWarehouseProduct(index)"
                  >
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleAddOutWarehouseProduct()"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    disabled
                  >
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
          </BillDetailTable>
        </div>
      </template>

      <!-- ═══ 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input
              v-model:value="formData.remark"
              size="small"
              class="remark-input"
            />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag
            color="blue"
            size="small"
          >{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <a-button
            type="link"
            size="small"
            class="doc-info-link"
          >
            打印记录
          </a-button>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'

defineOptions({ name: 'SaleExchangeForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  ExportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleExchangeApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)

const tableMaxHeight = ref(400)

// 弹窗当前操作的仓库类型: 'in' | 'out'
const currentWarehouseType = ref<'in' | 'out'>('in')

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

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
  billPrefix: 'XSHHD',
  api: {
    create: saleExchangeApi.create,
    update: saleExchangeApi.update,
    getById: saleExchangeApi.getById,
  },
  redirectPath: '/sales/exchange',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '',
    unit: '', batchCode: '',
    conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    exchangeGift: '', exchangePoints: 0,
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || ''
        fd.contactName = c.contactName || ''
        fd.contactPhone = c.contactPhone || ''
        fd.contactAddress = c.address || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        fd.creditLimit = c.creditLimit || 0
        fd.prevDebt = c.balance || 0
      }
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      if (u) {
        fd.handlerName = u.name
        fd.deptId = u.deptId
        fd.deptName = u.deptName || ''
      }
    }
    if (fieldKey === 'inWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.inWarehouseName = w.name
    }
    if (fieldKey === 'outWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.outWarehouseName = w.name
    }
  },
  transformPayload: (fd, status) => {
    const inItems = (fd.inWarehouseItems || []).filter((p: any) => p.productId != null).map((p: any) => ({
      ...buildItemPayload(p),
      warehouseType: 1,
    }))
    const outItems = (fd.outWarehouseItems || []).filter((p: any) => p.productId != null).map((p: any) => ({
      ...buildItemPayload(p),
      warehouseType: 2,
    }))
    return {
      ...fd,
      status,
      totalAmount: calcTotalDiscountAmount(),
      productAmount: calcTotalAmount(),
      discountAmount: calcTotalDiscountAmount(),
      inQuantityTotal: calcInQuantityTotal(),
      outQuantityTotal: calcOutQuantityTotal(),
      totalWeight: calcTotalWeight(),
      totalVolume: calcTotalVolume(),
      items: [...inItems, ...outItems],
    }
  },
})

// ── 明细行构建辅助 ──
function buildItemPayload(p: any) {
  return {
    productId: p.productId,
    productCode: p.itemCode || p.productCode || '',
    productName: p.productName || '',
    barcode: p.barcode || '',
    specification: p.specification || '',
    model: p.model || '',
    origin: p.origin || '',
    brand: p.brand || '',
    unit: p.unit || '',
    productLineAttr: p.productLineAttr || '',
    location: p.location || '',
    area: p.area || '',
    image: p.image || '',
    availableStock: p.availableStock || 0,
    stockConverted: p.stockConverted || 0,
    bookStock: p.bookStock || 0,
    batchBarcode: p.batchBarcode || '',
    productionDate: p.productionDate || null,
    shelfLife: p.shelfLife || 0,
    expiryDate: p.expiryDate || null,
    quantity: p.quantity || 0,
    conversionRate: p.conversionRate || '',
    pieceScatterQty: p.pieceScatterQty || 0,
    largePackage: p.largePackage || 0,
    mediumPackage: p.mediumPackage || 0,
    smallPackage: p.smallPackage || 0,
    recentSaleDate: p.recentSaleDate || null,
    recentSalePrice: p.recentSalePrice || 0,
    retailPrice: p.retailPrice || 0,
    wholesalePrice: p.wholesalePrice || 0,
    minSalePrice: p.minSalePrice || 0,
    unitPrice: p.unitPrice || 0,
    amount: (p.quantity || 0) * (p.unitPrice || 0),
    smallUnit: p.smallUnit || '',
    smallUnitPrice: p.smallUnitPrice || 0,
    smallUnitQty: p.smallUnitQty || 0,
    costPrice: p.costPrice || 0,
    costAmount: p.costAmount || 0,
    discount: p.discount || 0,
    discountPrice: p.discountPrice || 0,
    discountAmount: (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100),
    volume: p.volume || 0,
    weight: p.weight || 0,
    isGift: p.isGift || false,
    remark: p.remark || '',
    priceLevel1: p.priceLevel1 || 0,
    priceLevel2: p.priceLevel2 || 0,
    priceLevel3: p.priceLevel3 || 0,
    priceLevel4: p.priceLevel4 || 0,
    priceLevel5: p.priceLevel5 || 0,
    priceLevel6: p.priceLevel6 || 0,
    priceLevel7: p.priceLevel7 || 0,
    priceLevel8: p.priceLevel8 || 0,
    extNum1: p.extNum1 || 0,
    extNum2: p.extNum2 || 0,
    extNum3: p.extNum3 || 0,
    extText1: p.extText1 || '',
    extText2: p.extText2 || '',
    extNum6: p.extNum6 || 0,
    extNum7: p.extNum7 || 0,
    extPartner: p.extPartner || null,
    extStaff: p.extStaff || null,
    extDept: p.extDept || null,
  }
}

// ── 汇总计算辅助 ──
function calcTotalAmount(): number {
  const inAmt = (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0)
  const outAmt = (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0)
  return inAmt + outAmt
}

function calcTotalDiscountAmount(): number {
  const inAmt = (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0)
  const outAmt = (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0)
  return inAmt + outAmt
}

function calcInQuantityTotal(): number {
  return (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0), 0)
}

function calcOutQuantityTotal(): number {
  return (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0), 0)
}

function calcTotalWeight(): number {
  const inW = (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.weight || 0), 0)
  const outW = (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.weight || 0), 0)
  return inW + outW
}

function calcTotalVolume(): number {
  const inV = (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.volume || 0), 0)
  const outV = (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.volume || 0), 0)
  return inV + outV
}

// 初始化销售换货单特有字段
if (!('customerId' in formData)) {Object.assign(formData, {
  customerId: undefined, customerName: '', customerCode: '',
  inWarehouseId: undefined, inWarehouseName: '',
  outWarehouseId: undefined, outWarehouseName: '',
  handlerId: undefined, handlerName: '',
  deptId: undefined, deptName: '',
  exchangeDate: new Date().toISOString().slice(0, 10),
  salesType: '',
  bankName: '', bankAccount: '', taxNo: '',
  contactName: '', contactPhone: '', contactAddress: '',
  paymentAccount: undefined, receivedAmount: 0,
  prevAdvance: 0, useAdvance: 0, availableAdvance: 0, advanceBalance: 0,
  receivableIncrease: 0, creditLimit: 0, availableCredit: 0,
  prevDebt: 0, currentDebt: 0, debtBalance: 0, collectionDeadline: '',
  sourceOrderId: undefined, sourceOrderType: '',
  remark: '',
  printCount: 0,
  summary: '',
  attachment: '',
  inWarehouseItems: [],
  outWarehouseItems: [],
})}

// ── 双仓库表格合计 ──
const inTotalQuantity = computed(() => calcInQuantityTotal())
const outTotalQuantity = computed(() => calcOutQuantityTotal())
const inTotalAmount = computed(() =>
  (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0)
)
const outTotalAmount = computed(() =>
  (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0)
)
const inTotalDiscountAmount = computed(() =>
  (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0)
)
const outTotalDiscountAmount = computed(() =>
  (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0)
)

const inWarehouseSummaryColumns = computed(() => [
  { key: 'quantity', value: inTotalQuantity.value, highlight: true },
  { key: 'amount', value: inTotalAmount.value, highlight: true },
  { key: 'discountAmount', value: inTotalDiscountAmount.value, highlight: true },
])

const outWarehouseSummaryColumns = computed(() => [
  { key: 'quantity', value: outTotalQuantity.value, highlight: true },
  { key: 'amount', value: outTotalAmount.value, highlight: true },
  { key: 'discountAmount', value: outTotalDiscountAmount.value, highlight: true },
])

// ── 覆盖 useBillForm 暴露的 totalQuantity / totalAmount 使之反映双仓库 ──
const combinedQuantity = computed(() => inTotalQuantity.value + outTotalQuantity.value)
const combinedAmount = computed(() => calcTotalDiscountAmount())

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售换货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'customerCode', label: '客户编号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankName', label: '开户行', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankAccount', label: '银行账号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'taxNo', label: '税号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'inWarehouseId', label: '换入仓库', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'outWarehouseId', label: '换出仓库', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'deptName', label: '部门', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'exchangeDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'salesType', label: '销售类型', type: 'select', inlineLabel: true, width: 210, options: [
    { label: '普通销售', value: '普通销售' },
    { label: '赊销', value: '赊销' },
    { label: '代销', value: '代销' },
  ]},
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 435, readonly: true },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '收款', fields: [
    { key: 'paymentAccount', label: '收款账户', type: 'select', inlineLabel: true, width: 210 },
    { key: 'receivedAmount', label: '收款金额', type: 'number', inlineLabel: true, width: 210, precision: 2 },
    { key: 'prevAdvance', label: '此前预收', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'useAdvance', label: '使用预收款', type: 'number', inlineLabel: true, width: 210, precision: 2 },
    { key: 'availableAdvance', label: '可用预收', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'advanceBalance', label: '预收余额', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'receivableIncrease', label: '应收款增加', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'creditLimit', label: '信用额度', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'availableCredit', label: '可用额度', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
    { key: 'collectionDeadline', label: '收款期限', type: 'date', inlineLabel: true, width: 210 },
  ]},
  { key: 'source', tab: '源单', fields: [
    { key: 'sourceOrderType', label: '源单类型', type: 'select', inlineLabel: true, width: 210, options: [
      { label: '销售订单', value: 'salesOrder' },
      { label: '销售出库单', value: 'salesOutbound' },
    ]},
    { key: 'sourceOrderId', label: '源单', type: 'select', inlineLabel: true, width: 435 },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '换入数量', value: inTotalQuantity.value },
  { label: '换出数量', value: outTotalQuantity.value },
  { label: '商品金额', value: calcTotalAmount().toFixed(2) },
  { label: '折后金额', value: calcTotalDiscountAmount().toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `\u00a5${calcTotalDiscountAmount().toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 换入仓库数据表格列配置（63列）
// 关键差异：商品行属性在第12位（品牌之后）
// ═══════════════════════════════════════

const inWarehouseColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60 },
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
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'productLineAttr', title: '商品行属性', type: 'select', width: 100, options: [
    { label: '普通', value: 'normal' }, { label: '赠品', value: 'gift' }, { label: '促销品', value: 'promo' },
  ]},
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'stockConverted', title: '可用库存换算', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 100 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 100 },
  { key: 'shelfLife', title: '保质期', type: 'number', width: 80, precision: 0 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 100 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRate', title: '换算关系', type: 'input', width: 80 },
  { key: 'pieceScatterQty', title: '件散数量', type: 'number', width: 80, precision: 2 },
  { key: 'largePackage', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'mediumPackage', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPackage', title: '小包装', type: 'number', width: 70, precision: 0 },
  { key: 'recentSaleDate', title: '最近销售日期', type: 'date', width: 100, readonly: true },
  { key: 'recentSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 80, precision: 2, align: 'right', readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'discount', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'discountPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountAmount', title: '折后金额', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 80, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 80, precision: 4 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 80, precision: 2 },
  { key: 'extNum1', title: '自定义1', type: 'number', width: 80, precision: 2 },
  { key: 'extNum2', title: '自定义2', type: 'number', width: 80, precision: 2 },
  { key: 'extNum3', title: '自定义3', type: 'number', width: 80, precision: 2 },
  { key: 'extText1', title: '自定义4', type: 'input', width: 100 },
  { key: 'extText2', title: '自定义5', type: 'input', width: 100 },
  { key: 'extNum6', title: '自定义6', type: 'number', width: 80, precision: 2 },
  { key: 'extNum7', title: '自定义7', type: 'number', width: 80, precision: 2 },
  { key: 'extPartner', title: '自定义8(往来单位)', type: 'select', width: 120 },
  { key: 'extStaff', title: '自定义9(职员)', type: 'select', width: 110 },
  { key: 'extDept', title: '自定义10(部门)', type: 'select', width: 110 },
])

// ═══════════════════════════════════════
// 换出仓库数据表格列配置（63列）
// 关键差异：商品行属性在第43位（重量之后、赠品之前）
// ═══════════════════════════════════════

const outWarehouseColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60 },
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
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'stockConverted', title: '可用库存换算', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 100 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 100 },
  { key: 'shelfLife', title: '保质期', type: 'number', width: 80, precision: 0 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 100 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRate', title: '换算关系', type: 'input', width: 80 },
  { key: 'pieceScatterQty', title: '件散数量', type: 'number', width: 80, precision: 2 },
  { key: 'largePackage', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'mediumPackage', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPackage', title: '小包装', type: 'number', width: 70, precision: 0 },
  { key: 'recentSaleDate', title: '最近销售日期', type: 'date', width: 100, readonly: true },
  { key: 'recentSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 80, precision: 2, align: 'right', readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'discount', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'discountPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountAmount', title: '折后金额', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 80, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 80, precision: 4 },
  { key: 'productLineAttr', title: '商品行属性', type: 'select', width: 100, options: [
    { label: '普通', value: 'normal' }, { label: '赠品', value: 'gift' }, { label: '促销品', value: 'promo' },
  ]},
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 80, precision: 2 },
  { key: 'extNum1', title: '自定义1', type: 'number', width: 80, precision: 2 },
  { key: 'extNum2', title: '自定义2', type: 'number', width: 80, precision: 2 },
  { key: 'extNum3', title: '自定义3', type: 'number', width: 80, precision: 2 },
  { key: 'extText1', title: '自定义4', type: 'input', width: 100 },
  { key: 'extText2', title: '自定义5', type: 'input', width: 100 },
  { key: 'extNum6', title: '自定义6', type: 'number', width: 80, precision: 2 },
  { key: 'extNum7', title: '自定义7', type: 'number', width: 80, precision: 2 },
  { key: 'extPartner', title: '自定义8(往来单位)', type: 'select', width: 120 },
  { key: 'extStaff', title: '自定义9(职员)', type: 'select', width: 110 },
  { key: 'extDept', title: '自定义10(部门)', type: 'select', width: 110 },
])

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

// ═══ 创建空行辅助 ═══
function createEmptyItem() {
  return {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    productCode: '', productName: '', itemCode: '', barcode: '',
    specification: '', model: '', origin: '', brand: '',
    unit: '', productLineAttr: '', location: '', area: '', image: '',
    availableStock: 0, stockConverted: 0, bookStock: 0,
    batchBarcode: '', productionDate: null, shelfLife: 0, expiryDate: null,
    quantity: 0, conversionRate: '', pieceScatterQty: 0,
    largePackage: 0, mediumPackage: 0, smallPackage: 0,
    recentSaleDate: null, recentSalePrice: 0, retailPrice: 0,
    wholesalePrice: 0, minSalePrice: 0, unitPrice: 0, amount: 0,
    smallUnit: '', smallUnitPrice: 0, smallUnitQty: 0,
    costPrice: 0, costAmount: 0,
    discount: 100, discountPrice: 0, discountAmount: 0,
    volume: 0, weight: 0,
    isGift: false, remark: '',
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    extNum1: 0, extNum2: 0, extNum3: 0,
    extText1: '', extText2: '',
    extNum6: 0, extNum7: 0,
    extPartner: null, extStaff: null, extDept: null,
  }
}

// ═══ 填充商品信息 ═══
function fillProductInfo(row: any, p: any) {
  row.productId = p.id
  row.productName = p.name || ''
  row.itemCode = p.code || ''
  row.productCode = p.code || ''
  row.barcode = p.barcode || ''
  row.specification = p.specification || ''
  row.model = p.model || ''
  row.origin = p.origin || ''
  row.brand = p.brand || ''
  row.unit = p.unit || ''
  row.location = p.location || ''
  row.area = p.area || ''
  row.image = p.image || ''
  row.retailPrice = p.retailPrice || 0
  row.wholesalePrice = p.wholesalePrice || 0
  row.minSalePrice = p.minSalePrice || 0
  row.costPrice = p.costPrice || 0
  row.smallUnit = p.smallUnit || ''
  row.conversionRate = p.conversionRate || ''
  // 自动设置单价
  row.unitPrice = p.salePrice || p.retailPrice || p.price || 0
  // 自动计算金额
  calcRowAmount(row)
}

// ═══ 行金额计算 ═══
function calcRowAmount(row: any) {
  const qty = row.quantity || 0
  const price = row.unitPrice || 0
  row.amount = qty * price
  const disc = row.discount || 100
  row.discountPrice = price * (disc / 100)
  row.discountAmount = qty * row.discountPrice
}

// ═══ 换入仓库事件 ═══
function handleInWarehouseCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) fillProductInfo(record, p)
  } else if (['quantity', 'unitPrice', 'discount'].includes(fieldKey)) {
    record[fieldKey] = value
    calcRowAmount(record)
  }
}

function handleOpenInWarehouseProductModal(_record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentWarehouseType.value = 'in'
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleAddInWarehouseProduct() {
  formData.inWarehouseItems.push(createEmptyItem())
}

function handleInsertInWarehouseProduct(index: number) {
  formData.inWarehouseItems.splice(index + 1, 0, createEmptyItem())
}

function handleRemoveInWarehouseProduct(index: number) {
  formData.inWarehouseItems.splice(index, 1)
}

// ═══ 换出仓库事件 ═══
function handleOutWarehouseCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) fillProductInfo(record, p)
  } else if (['quantity', 'unitPrice', 'discount'].includes(fieldKey)) {
    record[fieldKey] = value
    calcRowAmount(record)
  }
}

function handleOpenOutWarehouseProductModal(_record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentWarehouseType.value = 'out'
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleAddOutWarehouseProduct() {
  formData.outWarehouseItems.push(createEmptyItem())
}

function handleInsertOutWarehouseProduct(index: number) {
  formData.outWarehouseItems.splice(index + 1, 0, createEmptyItem())
}

function handleRemoveOutWarehouseProduct(index: number) {
  formData.outWarehouseItems.splice(index, 1)
}

// ═══ 公共事件 ═══
function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能待完善`)
}

// ═══ 产品选择弹窗确认 ═══
function handleProductSelectConfirm(products: any[]) {
  const targetArray = currentWarehouseType.value === 'in'
    ? formData.inWarehouseItems
    : formData.outWarehouseItems
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : targetArray.length

  // 确保有足够行
  while (targetArray.length < startIndex + products.length) {
    targetArray.push(createEmptyItem())
  }

  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < targetArray.length) {
      fillProductInfo(targetArray[rowIndex], p)
    }
  })

  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history':
      router.push('/sales/exchange')
      break
    case 'print': {
      const id = route.query.id as string
      if (id) {
        saleExchangeApi.print(Number(id))
          .then(() => message.success('打印成功'))
          .catch((e: any) => message.error(e.message || '打印失败'))
      } else {
        message.warning('请先保存换货单后再打印')
      }
      break
    }
    case 'config':
      message.info('请使用底部Tab中的列配置功能')
      break
    case 'import':
      message.info('导入功能请使用列表页的导入按钮')
      break
    case 'export': {
      const id = route.query.id as string
      if (id) {
        saleExchangeApi.export({ id })
          .then(() => message.success('导出成功'))
          .catch((e: any) => message.error(e.message || '导出失败'))
      }
      break
    }
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(async () => {
  // 编辑模式加载数据
  const id = route.query.id as string
  if (id) {
    try {
      const res = await saleExchangeApi.getById(Number(id))
      if (res) {
        // 回填主表字段
        Object.keys(res).forEach(key => {
          if (key !== 'items' && key in formData) {
            (formData as any)[key] = res[key]
          }
        })
        // 分仓回填明细
        if (res.items && Array.isArray(res.items)) {
          formData.inWarehouseItems = res.items
            .filter((it: any) => it.warehouseType === 1)
            .map((it: any) => ({ ...createEmptyItem(), ...it }))
          formData.outWarehouseItems = res.items
            .filter((it: any) => it.warehouseType === 2)
            .map((it: any) => ({ ...createEmptyItem(), ...it }))
        }
      }
    } catch (e) {
      message.error('加载换货单失败')
    }
  }
  // 确保至少有空行
  if (formData.inWarehouseItems.length === 0) {
    for (let i = 0; i < 10; i++) formData.inWarehouseItems.push(createEmptyItem())
  }
  if (formData.outWarehouseItems.length === 0) {
    for (let i = 0; i < 10; i++) formData.outWarehouseItems.push(createEmptyItem())
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 480)
  })
})
</script>

<style scoped>
.warehouse-section {
  margin-bottom: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fff;
}

.warehouse-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.warehouse-count {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
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

.doc-info-item {
  white-space: nowrap;
}

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}
</style>
