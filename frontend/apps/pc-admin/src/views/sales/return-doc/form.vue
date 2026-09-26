<template>
  <div class="form-page-container" style="height:100%;display:flex;flex-direction:column;">
    <BillFormPage
      :model-value="formData"
      @update:model-value="(v: any) => Object.assign(formData, v)"
      :header="headerConfig"
      :basic-info-fields="visibleBasicInfoFields"
      :tabs="tabsConfig"
      :summary="summaryConfig"
      :footer="footerConfig"
      collapsible-fields
      :collapsed-rows="2"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleFormDraft"
      @submit="handleFormSubmit"
    >
      <!-- ═══ 商品明细表格（列配置入口在数据表内部：表头首列的齿轮） ═══ -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="detailColumns"
          v-model:data-source="formData.products"
          :summary-columns="tableSummaryColumns"
          :storage-key="'sale-return-doc-form-detail-columns'"
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

      <!-- ═══ 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input v-model:value="formData.remark" size="small" class="remark-input" />
          </div>
          <div class="remark-row">
            <span class="remark-label">内部备注</span>
            <a-input v-model:value="formData.internalNote" size="small" class="remark-input" />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <a-button type="link" size="small" class="doc-info-link" @click="handleShowPrintRecord">打印记录</a-button>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 表单配置弹窗（页面配置 / 录单设置 / 打印设置，localStorage 真实落库） ═══ -->
    <SaleReturnDocFormConfig
      :open="showFormConfig"
      @update:open="showFormConfig = $event"
      @change="handleFormConfigChange"
    />

    <!-- ═══ 快速查询弹窗（+Q / Q 按钮） ═══ -->
    <a-modal
      v-model:open="showQuickSearch"
      :title="quickSearchTitle"
      width="500px"
      :footer="null"
      destroy-on-close
    >
      <a-select
        v-model:value="quickSearchValue"
        show-search
        style="width:100%"
        :placeholder="`搜索${quickSearchTitle}`"
        :options="quickSearchOptions"
        :filter-option="filterOption"
        @change="handleQuickSearchConfirm"
      />
    </a-modal>

    <!-- ═══ 打印弹窗（真实模板打印，打印成功累加打印次数） ═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="sale-return-doc"
      :print-data="printData"
      :always-last-template="printSettings.alwaysLastTemplate"
      @print-success="handlePrintSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'

defineOptions({ name: 'SaleReturnDocForm' })
import { message, Modal } from 'ant-design-vue'
import {
  PrinterOutlined, ClockCircleOutlined, ImportOutlined, ExportOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import SaleReturnDocFormConfig from '@/views/erp/column-config/SaleReturnDocFormConfig.vue'
import {
  SALE_RETURN_DOC_PAGE_FIELDS,
  SRD_FORM_DEFAULTS_KEY,
  SRD_FORM_PRINT_KEY,
  SRD_BOTTOM_AREA_KEYS,
  loadSaleReturnDocFieldVisibility,
  type SaleReturnDocPageField,
} from '@/views/erp/column-config/saleReturnDocFormDefaults'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, TabField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleReturnDocApi, saleReturnApi } from '@/api/erp'
import { PRODUCT_SALES_DEFAULTS } from '@/utils/productDefaults'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)
const showFormConfig = ref(false)
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)

// ═══ 部门下拉选项（来源 sys_dept） ═══
const departmentOptions = ref<any[]>([])
async function loadDepartments() {
  try {
    departmentOptions.value = await optionsApi.getDepartments()
  } catch {
    departmentOptions.value = []
  }
}

// ═══ 表单配置存储键（与 SaleReturnDocFormConfig 共用 saleReturnDocFormDefaults 里的常量） ═══
const FORM_DEFAULTS_KEY = SRD_FORM_DEFAULTS_KEY
const FORM_PRINT_KEY = SRD_FORM_PRINT_KEY

// ═══ 打印设置（从配置弹窗落库的存档读取） ═══
const printSettings = ref<{ alwaysLastTemplate: boolean; printAfterSubmit: boolean }>({
  alwaysLastTemplate: false,
  printAfterSubmit: false,
})

function loadPrintSettings() {
  try {
    const raw = localStorage.getItem(FORM_PRINT_KEY)
    if (raw) printSettings.value = { ...printSettings.value, ...JSON.parse(raw) }
  } catch {
    // 存档损坏时沿用默认
  }
}
loadPrintSettings()

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

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
} = useBillForm({
  billPrefix: 'XSTHD',
  // 单号必须来自后端号段（严禁前端演示自增号）
  codeApiPath: '/erp/sale/return-doc/next-no',
  api: {
    create: saleReturnDocApi.create,
    update: saleReturnDocApi.update,
    getById: saleReturnDocApi.getById,
  },
  // ⚠️ 不在这里写 redirectPath：保存后的去向要在 afterSave 里按「保存后立即打印」开关决定
  //（useBillForm 的顺序是 afterSave() → redirectPath 跳转，配了 redirectPath 就来不及打印）。
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_SALES_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || ''
        fd.customerLevel = c.level || ''
        fd.contactName = c.contactName || ''
        fd.contactPhone = c.contactPhone || ''
        fd.shippingAddress = c.address || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        fd.customerTicket = c.ticket || ''
        fd.customerRemark = c.remark || ''
        fd.prevDebt = c.currentDebt || c.prevDebt || 0
        fd.creditLimit = c.creditLimit || 0
        fd.availableCredit = c.availableCredit ?? (c.creditLimit || 0)
        fd.debtBalance = c.debtBalance ?? (c.creditLimit || 0) - (c.currentDebt || 0)
        fd.prevAdvance = c.prevAdvance || 0
      }
    }
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.warehouseName = w.warehouseName || w.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      if (u) fd.handlerName = u.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
    if (fieldKey === 'returnApplyId') {
      // 雪花 ID 可能以字符串/数字两种形态到达，统一按字符串比较，避免精度形态差异导致匹配失败
      const apply = returnApplyOptions.value.find((x: any) => String(x.value) === String(val))?.raw
      if (!apply) {
        fd.returnApplyNo = ''
        return
      }
      fd.returnApplyNo = apply.returnNo || ''
      // 表头随源单带出；统一从下拉选项反查（雪花 ID 到达时可能是字符串，直接赋值会导致下拉不回显）
      if (apply.customerId != null) {
        const c = optionRefs.customers.find((x: any) => String(x.id) === String(apply.customerId))
        fd.customerId = c ? c.id : apply.customerId
        baseFieldChange('customerId', fd.customerId)
      }
      if (apply.warehouseId != null) {
        const w = optionRefs.warehouses.find((x: any) => String(x.id) === String(apply.warehouseId))
        fd.warehouseId = w ? w.id : apply.warehouseId
        fd.warehouseName = w ? (w.warehouseName || w.name || '') : (apply.warehouseName || '')
      }
      if (apply.handlerId != null) {
        const u = optionRefs.users.find((x: any) => String(x.id) === String(apply.handlerId))
        fd.handlerId = u ? u.id : apply.handlerId
        fd.handlerName = u ? (u.name || '') : (apply.handlerName || '')
      }
      if (apply.deptId != null) fd.deptId = apply.deptId
      if (apply.salesType) fd.salesType = apply.salesType
      // 带出待收明细（已有明细时先确认，避免误覆盖）
      if (formData.products.length > 0) {
        Modal.confirm({
          title: '覆盖商品明细',
          content: '当前已有商品明细，是否用退货申请单的明细覆盖？',
          okText: '覆盖',
          cancelText: '保留',
          onOk: () => fillProductsFromApply(apply),
        })
      } else {
        fillProductsFromApply(apply)
      }
    }
  },
  transformPayload: (fd, status) => {
    // products 是前端行模型，不落后端；后端只接收 items
    const { products, ...header } = fd as any
    return {
      ...header,
      // 单据编号：前端通用字段 orderNo → 后端实体 returnDocNo（号段由后端 /next-no 提供）
      returnDocNo: fd.orderNo,
      status,
      totalAmount: totalAmount.value,
      billAmount: totalAmount.value,
      items: (products || [])
        .filter((p: any) => p.productId != null)
        .map((p: any, idx: number) => ({
          productId: p.productId,
          productCode: p.itemCode || p.productCode,
          productName: p.productName,
          barcode: p.barcode,
          specification: p.specification,
          productSpec: p.specification,
          productUnit: p.unit,
          unit: p.unit,
          imageUrl: p.imageUrl || '',
          storageLocation: p.storageLocation || '',
          area: p.area || '',
          modelNo: p.modelNo || '',
          originPlace: p.originPlace || '',
          brand: p.brand || '',
          availableStock: p.availableStock || 0,
          availableStockConverted: p.availableStockConverted || 0,
          bookStock: p.bookStock || 0,
          batchBarcode: p.batchBarcode || '',
          productionDate: p.productionDate || null,
          shelfLife: p.shelfLife || '',
          expiryDate: p.expiryDate || null,
          returnQuantity: p.quantity,
          conversionRelation: p.conversionRelation || '',
          pieceQuantity: p.pieceQuantity || 0,
          bigPack: p.bigPack || 0,
          midPack: p.midPack || 0,
          smallPack: p.smallPack || 0,
          lastSaleDate: p.lastSaleDate || null,
          lastSalePrice: p.lastSalePrice || 0,
          retailPrice: p.retailPrice || 0,
          wholesalePrice: p.wholesalePrice || 0,
          minSalePrice: p.minSalePrice || 0,
          unitPrice: p.unitPrice,
          lineAmount: (p.quantity || 0) * (p.unitPrice || 0),
          smallUnit: p.smallUnit || '',
          smallUnitPrice: p.smallUnitPrice || 0,
          smallUnitQuantity: p.smallUnitQuantity || 0,
          conversionResult: p.conversionResult || 0,
          refCostPrice: p.refCostPrice || 0,
          refCostAmount: p.refCostAmount || 0,
          discountRate: p.discountRate || 0,
          discountedPrice: p.discountedPrice || 0,
          discountedAmount: p.discountedAmount || 0,
          exchangeGift: p.exchangeGift || '',
          exchangePoints: p.exchangePoints || 0,
          generatedPoints: p.generatedPoints || 0,
          usedPoints: p.usedPoints || 0,
          productLineAttr: p.productLineAttr || '',
          volume: p.volume || 0,
          weight: p.weight || 0,
          isGift: p.isGift || false,
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
          extNum4: p.extNum4 || 0,
          extNum5: p.extNum5 || 0,
          extText1: p.extText1 || '',
          extText2: p.extText2 || '',
          extPartner: p.extPartner || null,
          extStaff: p.extStaff || null,
          extDept: p.extDept || null,
          remark: p.remark || '',
          itemRemark: p.itemRemark || '',
          lineNo: idx + 1,
        })),
    }
  },
  // 详情回填：后端字段 → 前端表单字段
  onDetailLoaded: (data: any, fd: any) => {
    if (data?.returnDocNo) fd.orderNo = data.returnDocNo
    if (data?.status !== undefined) fd.status = data.status
    if (data?.salesType) fd.salesType = data.salesType
  },
  // 保存成功后的去向：默认回列表；勾了「保存后立即打印」则留在本页打印（打印完用户自行返回）。
  // 原先这里的 maybePrintAfterSubmit() 定义了却从未被调用 —— 配置面板上的开关是个死勾选框。
  afterSave: () => {
    resetDirty()
    if (printSettings.value.printAfterSubmit) {
      setTimeout(() => handlePrint(), 500)
    } else {
      router.push('/sales/return-doc')
    }
  },
})

// ═══ 表单 Dirty 标记（对标金蝶/用友/管家婆：未保存修改离开时提示） ═══
const isDirty = ref(false)
let _dirtyInitialized = false
watch(formData, () => {
  if (_dirtyInitialized) isDirty.value = true
  else _dirtyInitialized = true
}, { deep: true })
function resetDirty() { isDirty.value = false }

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) { e.preventDefault(); e.returnValue = '' }
}
onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  loadReturnApplyOptions()
})

onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value && effectiveMode.value !== 'view') {
    Modal.confirm({
      title: '确认离开',
      content: '当前单据有未保存的修改，离开将丢失所有更改，是否继续？',
      okText: '离开',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => next(),
      onCancel: () => next(false),
    })
  } else {
    next()
  }
})

// 初始化销售退货单特有字段
if (!('customerId' in formData)) {
  Object.assign(formData, {
    customerId: undefined, customerName: '', customerCode: '', customerLevel: '',
    warehouseId: undefined, warehouseName: '',
    handlerId: undefined, handlerName: '',
    deptId: undefined, deptName: '',
    orderDate: new Date().toISOString().slice(0, 10),
    salesType: 'normal',
    contactName: '', contactPhone: '', shippingAddress: '',
    bankName: '', bankAccount: '', taxNo: '',
    extNum1: 0, extNum2: 0, extText1: '', extText2: '', extText3: '',
    summary: '',
    paymentAccount1: '', settledAmount: 0, paymentAccount2: '',
    prevAdvance: 0, returnAdvance: 0, availableAdvance: 0, advanceBalance: 0,
    receivableReduce: 0,
    creditLimit: 0, availableCredit: 0,
    prevDebt: 0, currentDebt: 0, debtBalance: 0, collectionDeadline: '',
    sourceOrder: '',
    deliveryMethod: '', deliveryRoute: '', deliveryRouteId: undefined,
    logisticsCompany: '', freightPayer: '', shippingFee: 0, waybillNo: '', deliveryOrderNo: '', deliveryNo: '',
    memberCardNo: '', memberName: '', memberDiscount: 0,
    prevPoints: 0, memberGeneratedPoints: 0, memberExchangePoints: 0,
    memberUsedPoints: 0, currentPoints: 0,
    remark: '', internalNote: '', printCount: 0,
    returnType: 0, reason: '',
    returnApplyId: undefined, returnApplyNo: '',
    receiverName: '', receiverPhone: '',
    generateType: '手动创建',
  })
}

// ─── 退货申请（源单）选项：只列「已审核待收货 / 部分收货」的申请，选后带出商品明细 ───
// 这是《物流退货收货》闭环的操作入口：申请 → 退货单收货 → 回写申请的 已收/未收 数量。
const returnApplyOptions = ref<any[]>([])
const returnApplyLoading = ref(false)

async function loadReturnApplyOptions() {
  returnApplyLoading.value = true
  try {
    const res: any = await saleReturnApi.page({ pageNum: 1, pageSize: 200, status: 2 })
    const records: any[] = res?.records || []
    returnApplyOptions.value = records.map((r: any) => ({
      label: `${r.returnNo || ''}（${r.customerName || ''}）`,
      value: r.id,
      raw: r,
    }))
  } catch {
    returnApplyOptions.value = []
  } finally {
    returnApplyLoading.value = false
  }
}

/** 选择退货申请后带出商品明细行（前端行模型 quantity ↔ 后端 returnQuantity） */
async function fillProductsFromApply(apply: any) {
  if (!apply?.id) return
  try {
    const res: any = await saleReturnApi.getItems(apply.id)
    const items: any[] = Array.isArray(res) ? res : (res?.data || [])
    if (!items.length) return
    const newRows = items.map((it: any, idx: number) => ({
      ...PRODUCT_SALES_DEFAULTS,
      id: `apply-${it.id ?? idx}`,
      productId: it.productId,
      itemCode: it.productCode || '',
      productCode: it.productCode || '',
      productName: it.productName || '',
      barcode: it.barcode || '',
      specification: it.specification || '',
      unit: it.unit || '',
      quantity: Number(it.returnQuantity ?? 0),
      unitPrice: Number(it.unitPrice ?? 0),
      smallUnit: it.smallUnit || '',
      conversionRelation: it.conversionRelation || '',
      retailPrice: it.retailPrice || 0,
      wholesalePrice: it.wholesalePrice || 0,
      minSalePrice: it.minSalePrice || 0,
      refCostPrice: it.refCostPrice || 0,
      refCostAmount: it.refCostAmount || 0,
      weight: it.weight || 0,
      volume: it.volume || 0,
      itemRemark: it.itemRemark || '',
      productLineAttr: it.productLineAttr || '',
      isGift: it.isGift || false,
    }))
    // 原地替换：保持明细数组引用不变，确保 BillDetailTable 的 v-model 数据源同步刷新
    formData.products.splice(0, formData.products.length, ...newRows)
  } catch {
    // 明细加载失败时保留表头带出结果，用户可手工添加明细
  }
}

// ─ 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const totalPieceQty = computed(() => formData.products.reduce((s: number, p: any) => s + (p.pieceQuantity || 0), 0))
const totalWeight = computed(() => formData.products.reduce((s: number, p: any) => s + (p.weight || 0), 0))
const totalVolume = computed(() => formData.products.reduce((s: number, p: any) => s + (p.volume || 0), 0))
const totalDiscountedAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0))

/** 表格合计列定义 */
const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'pieceQuantity', value: totalPieceQty.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value, highlight: true },
  { key: 'midPack', value: totalMidPack.value, highlight: true },
  { key: 'smallPack', value: totalSmallPack.value, highlight: true },
  { key: 'amount', value: totalAmount.value, highlight: true },
  { key: 'discountedAmount', value: totalDiscountedAmount.value, highlight: true },
  { key: 'weight', value: totalWeight.value, highlight: false },
  { key: 'volume', value: totalVolume.value, highlight: false },
  { key: 'refCostAmount', value: formData.products.reduce((s: number, p: any) => s + (p.refCostAmount || 0), 0), highlight: false },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售退货单',
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

/** 完整的页面字段配置（53 字段，与开发文档一致） */
const allBasicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'orderNo', label: '编号', type: 'display', inlineLabel: true, width: 210, value: formData.orderNo },
  { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435,
    options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'customerCode', label: '客户编号', type: 'display', inlineLabel: true, width: 150, value: formData.customerCode },
  { key: 'bankName', label: '开户行', type: 'display', inlineLabel: true, width: 210, value: formData.bankName },
  { key: 'bankAccount', label: '银行账号', type: 'display', inlineLabel: true, width: 210, value: formData.bankAccount },
  { key: 'taxNo', label: '税号', type: 'display', inlineLabel: true, width: 210, value: formData.taxNo },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, inlineLabel: true, width: 210,
    options: optionRefs.warehouses.map((w: any) => ({ label: w.warehouseName || w.name, value: w.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210,
    options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
    searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 210,
    options: departmentOptions.value.map((d: any) => ({ label: d.name, value: d.id })), searchBtn: '+Q' },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'salesType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 210,
    options: [{ label: '正常销售', value: 'normal' }, { label: '退货', value: 'return' }] },
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, searchBtn: 'Q' },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 210 },
  { key: 'shippingAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 435 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 435 },
  { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', inlineLabel: true, width: 210 },
  { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', inlineLabel: true, width: 210 },
  { key: 'extText1', label: '自定义字段3(文本)', type: 'input', inlineLabel: true, width: 210 },
  { key: 'extText2', label: '自定义字段4(文本)', type: 'input', inlineLabel: true, width: 210 },
  { key: 'extText3', label: '自定义字段5(文本)', type: 'input', inlineLabel: true, width: 210 },
  // ═══ 收款 Tab 字段（不在基本信息区重复渲染） ═══
  { key: 'paymentAccount1', label: '付款账户', type: 'input', inlineLabel: true, width: 210 },
  { key: 'settledAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 210 },
  { key: 'paymentAccount2', label: '更多账户', type: 'input', inlineLabel: true, width: 210 },
  { key: 'prevAdvance', label: '此前预收', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'returnAdvance', label: '退回预收款', type: 'number', inlineLabel: true, width: 210 },
  { key: 'availableAdvance', label: '可用预收', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'advanceBalance', label: '预收余额', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'receivableReduce', label: '应收款减少', type: 'number', inlineLabel: true, width: 210 },
  { key: 'creditLimit', label: '信用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'availableCredit', label: '可用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'collectionDeadline', label: '收款期限', type: 'date', inlineLabel: true, width: 210 },
  // ═══ 物流 Tab 字段 ═══
  { key: 'deliveryMethod', label: '配送方式', type: 'select', inlineLabel: true, width: 210,
    options: [{ label: '自提', value: '自提' }, { label: '配送', value: '配送' }, { label: '快递', value: '快递' }] },
  { key: 'deliveryRoute', label: '配送线路', type: 'input', inlineLabel: true, width: 210 },
  { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
  { key: 'freightPayer', label: '运费承担方', type: 'select', inlineLabel: true, width: 210,
    options: [{ label: '我方', value: 'seller' }, { label: '客户', value: 'buyer' }] },
  { key: 'shippingFee', label: '运费', type: 'number', inlineLabel: true, width: 210 },
  { key: 'waybillNo', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'deliveryNo', label: '配送单', type: 'input', inlineLabel: true, width: 210 },
  // ═══ 会员 Tab 字段 ═══
  { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'prevPoints', label: '此前积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'memberGeneratedPoints', label: '产生积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'memberExchangePoints', label: '兑换积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'memberUsedPoints', label: '使用积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'currentPoints', label: '剩余积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
  // ═══ 源单 / 单据信息 ═══
  { key: 'returnApplyId', label: '退货申请', type: 'select', inlineLabel: true, width: 435,
    options: returnApplyOptions.value, searchBtn: '+Q', loading: returnApplyLoading.value,
    placeholder: '选择退货申请单（带出待收明细）' },
  { key: 'sourceOrder', label: '源单', type: 'input', inlineLabel: true, width: 210, placeholder: '请输入源单编号' },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100, value: currentUserName.value },
  { key: 'createTime', label: '制单时间', type: 'display', inlineLabel: true, width: 200, value: formatNow() },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 100, value: formData.printCount || 0 },
  { key: 'billAmount', label: '本单金额', type: 'display', inlineLabel: true, width: 150, value: `\u00a5${totalAmount.value.toFixed(2)}` },
])

// ── 表单字段显隐（默认值来自 saleReturnDocFormDefaults，与配置弹窗同一份定义） ──
const formFieldVisibility = ref<SaleReturnDocPageField[]>(loadSaleReturnDocFieldVisibility())

/** 字段显隐完全由页面配置决定（勾选即生效）；未在配置表内的字段默认不显示 */
function isFieldVisible(key: string): boolean {
  const cfg = formFieldVisibility.value.find(f => f.key === key)
  return cfg ? cfg.visible !== false : false
}

/** 头部字段区只渲染「不属于底部区域」的字段；显隐完全由页面配置决定（无存档时用默认勾选，不再回退成全部显示） */
const visibleBasicInfoFields = computed(() =>
  allBasicInfoFields.value.filter(field =>
    !SRD_BOTTOM_AREA_KEYS.has(field.key) && isFieldVisible(field.key)
  )
)

/** 过滤 Tab 内字段显隐 */
function visibleTabFields(fields: TabField[]): TabField[] {
  return fields.filter(f => isFieldVisible(f.key))
}

const tabsConfig = computed<BillTabConfig[]>(() => {
  const tabs: BillTabConfig[] = [
    // Tab 1: 收款（退货场景收回已付款项）
    { key: 'receipt', tab: '收款', fields: visibleTabFields([
      { key: 'paymentAccount1', label: '付款账户', type: 'select', inlineLabel: true, width: 210,
        options: [
          { label: '现金', value: '现金' },
          { label: '银行转账', value: '银行转账' },
          { label: '支付宝', value: '支付宝' },
          { label: '微信', value: '微信' },
        ], searchBtn: '+Q' },
      { key: 'settledAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 210 },
      { key: 'paymentAccount2', label: '更多账户', type: 'input', inlineLabel: true, width: 210 },
      { key: 'prevAdvance', label: '此前预收', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'returnAdvance', label: '退回预收款', type: 'number', inlineLabel: true, width: 210 },
      { key: 'availableAdvance', label: '可用预收', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'advanceBalance', label: '预收余额', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'receivableReduce', label: '应收款减少', type: 'number', inlineLabel: true, width: 210 },
      { key: 'creditLimit', label: '信用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'availableCredit', label: '可用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'collectionDeadline', label: '收款期限', type: 'date', inlineLabel: true, width: 210 },
    ]) },
    // Tab 2: 物流信息
    { key: 'logistics', tab: '物流信息', fields: visibleTabFields([
      { key: 'deliveryMethod', label: '配送方式', type: 'select', inlineLabel: true, width: 210,
        options: [{ label: '自提', value: '自提' }, { label: '配送', value: '配送' }, { label: '快递', value: '快递' }] },
      { key: 'deliveryRoute', label: '配送线路', type: 'input', inlineLabel: true, width: 210 },
      { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
      { key: 'freightPayer', label: '运费承担方', type: 'select', inlineLabel: true, width: 210,
        options: [{ label: '我方', value: 'seller' }, { label: '客户', value: 'buyer' }] },
      { key: 'shippingFee', label: '运费', type: 'number', inlineLabel: true, width: 210 },
      { key: 'waybillNo', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
      { key: 'deliveryNo', label: '配送单', type: 'input', inlineLabel: true, width: 210 },
    ]) },
    // Tab 3: 会员信息
    { key: 'member', tab: '会员信息', fields: visibleTabFields([
      { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 210 },
      { key: 'prevPoints', label: '此前积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
      { key: 'memberGeneratedPoints', label: '产生积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'memberExchangePoints', label: '兑换积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'memberUsedPoints', label: '使用积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'currentPoints', label: '剩余积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
    ]) },
  ]
  return tabs.filter(t => t.fields.length > 0)
})

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '退货数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '折后金额', value: totalDiscountedAmount.value.toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `\u00a5${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置（67 业务列 + 序号列）
// ═══════════════════════════════════════

const allDetailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'input', width: 60 },
  { key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name,
      value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'storageLocation', title: '货位', type: 'input', width: 80 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'modelNo', title: '型号', type: 'input', width: 80 },
  { key: 'originPlace', title: '产地', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 120, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2 },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 110 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 70, precision: 0 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 120 },
  { key: 'lastSalePrice', title: '最近售价', type: 'number', width: 90, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 90, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2, align: 'right' },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'refCostPrice', title: '参考成本单价', type: 'number', width: 110, precision: 2 },
  { key: 'refCostAmount', title: '参考成本金额', type: 'number', width: 110, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 90, precision: 2 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 90, precision: 2 },
  { key: 'exchangeGift', title: '兑换礼品', type: 'input', width: 100 },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 90, precision: 0 },
  { key: 'generatedPoints', title: '产生积分', type: 'number', width: 90, precision: 0 },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 90, precision: 0 },
  { key: 'productLineAttr', title: '商品行属性', type: 'select', width: 110,
    options: [{ label: '正常', value: '正常' }, { label: '赠品', value: '赠品' }] },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 2 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 120 },
  // 8 个标准化价格等级（文档中以用户自定义昵称出现）
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 90, precision: 2 },
  // 单据自定义字段（行级）
  { key: 'extNum1', title: '单据自定义1(数字字段)', type: 'number', width: 130, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字字段)', type: 'number', width: 130, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字字段)', type: 'number', width: 130, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本字段)', type: 'input', width: 130 },
  { key: 'extText2', title: '单据自定义5(文本字段)', type: 'input', width: 130 },
  { key: 'extNum4', title: '单据自定义6(数字字段)', type: 'number', width: 130, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字字段)', type: 'number', width: 130, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'input', width: 150 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'input', width: 130 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'input', width: 130 },
  // 明细备注（查询条件「明细备注」对应列）
  { key: 'itemRemark', title: '明细备注', type: 'input', width: 120 },
])

// 明细列配置入口在数据表内部（表头首列齿轮，个人/全局双配置 + 宽度），此处只提供全量列定义
const detailColumns = computed<DetailColumnConfig[]>(() => allDetailColumns.value)

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

/** 保存前清理空行（不提交空行数据） */
function cleanEmptyRows() {
  formData.products = formData.products.filter((p: any) => p.productId != null && p.productId !== '')
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
}

/** 保存草稿：清理空行 + 应用录单打印设置 */
function handleFormDraft() {
  cleanEmptyRows()
  handleSaveDraft()
}

/** 提交：清理空行 + 商品行必填校验 */
function handleFormSubmit() {
  cleanEmptyRows()
  if (formData.products.length === 0) {
    message.warning('请至少录入一行退货商品')
    return
  }
  const invalid = formData.products.find((p: any) => !p.quantity || Number(p.quantity) <= 0)
  if (invalid) {
    message.warning(`商品「${invalid.productName || '未命名'}」的退货数量必须大于 0`)
    return
  }
  handleSubmit()
}

/** 插入商品行 */
function handleInsertProduct(index: number) {
  formData.products.splice(index + 1, 0, {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined, itemCode: '', barcode: '', specification: '',
    unit: '', conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    exchangeGift: '', exchangePoints: 0,
    productName: '', quantity: 0, availableStock: 0, remark: '', itemRemark: '',
    imageUrl: '', storageLocation: '', area: '', modelNo: '', originPlace: '', brand: '',
    availableStockConverted: 0, bookStock: 0,
    batchBarcode: '', productionDate: null, shelfLife: '', expiryDate: null,
    lastSaleDate: null, lastSalePrice: 0, retailPrice: 0, wholesalePrice: 0, minSalePrice: 0,
    smallUnit: '', smallUnitPrice: 0, smallUnitQuantity: 0, conversionResult: 0,
    refCostPrice: 0, refCostAmount: 0,
    discountRate: 0, discountedPrice: 0, discountedAmount: 0,
    generatedPoints: 0, usedPoints: 0,
    productLineAttr: '正常', volume: 0, weight: 0, isGift: false,
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0,
    extText1: '', extText2: '', extPartner: null, extStaff: null, extDept: null,
  })
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.productSpec = p.specification || ''
      record.unit = p.unit || ''
      record.productUnit = p.unit || ''
      record.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      record.retailPrice = p.retailPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.minSalePrice = p.minSalePrice || 0
      record.lastSalePrice = p.lastSalePrice || 0
      record.lastSaleDate = p.lastSaleDate || null
      record.refCostPrice = p.costPrice || p.avgCost || 0
      record.brand = p.brand || ''
      record.originPlace = p.originPlace || p.origin || ''
      record.modelNo = p.modelNo || p.model || ''
      record.area = p.area || ''
      record.availableStock = p.stock || p.availableStock || 0
      record.bookStock = p.bookStock || p.stock || 0
      record.taxRate = 13
      record.priceLevel1 = p.priceLevel1 || 0
      record.priceLevel2 = p.priceLevel2 || 0
      record.priceLevel3 = p.priceLevel3 || 0
      record.priceLevel4 = p.priceLevel4 || 0
      record.priceLevel5 = p.priceLevel5 || 0
      record.priceLevel6 = p.priceLevel6 || 0
      record.priceLevel7 = p.priceLevel7 || 0
      record.priceLevel8 = p.priceLevel8 || 0
    }
  }
  // 金额计算：数量 × 单价 = 金额
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
    record.refCostAmount = (record.refCostPrice || 0) * (record.quantity || 0)
    if (record.conversionRelation) {
      const conv = parseFloat(record.conversionRelation) || 1
      record.pieceQuantity = (record.quantity || 0) * conv
      record.smallUnitQuantity = record.pieceQuantity
      record.smallUnitPrice = conv > 0 ? (record.unitPrice || 0) / conv : 0
    }
  }
  // 换算关系变更：重算件散数量与小单位单价
  if (fieldKey === 'conversionRelation') {
    const conv = parseFloat(value) || 1
    record.pieceQuantity = (record.quantity || 0) * conv
    record.smallUnitQuantity = record.pieceQuantity
    record.smallUnitPrice = conv > 0 ? (record.unitPrice || 0) / conv : 0
  }
  // 折扣联动：折后单价 / 折后金额
  if (fieldKey === 'discountRate' || fieldKey === 'unitPrice' || fieldKey === 'quantity') {
    const rate = record.discountRate || 0
    const amount = record.amount || 0
    record.discountedAmount = amount * (1 - rate / 100)
    record.discountedPrice = record.quantity > 0 ? record.discountedAmount / record.quantity : 0
  }
  // 用户直接改折后单价 → 反算折扣率
  if (fieldKey === 'discountedPrice') {
    if (record.unitPrice > 0) {
      record.discountRate = ((record.unitPrice - (record.discountedPrice || 0)) / record.unitPrice) * 100
      record.discountedAmount = (record.discountedPrice || 0) * (record.quantity || 0)
    }
  }
  // 退货入库数量超过账面库存时提示（对标管家婆/金蝶）
  if (fieldKey === 'quantity' && record.quantity > 0 && record.bookStock > 0 && record.quantity > record.bookStock) {
    message.warn(`商品「${record.productName || record.itemCode}」退货数量 ${record.quantity} 超出账面库存 ${record.bookStock}，请确认`)
  }
}

/** 快速查询（+Q / Q 按钮）：打开真实选项弹窗，选项来自后端下拉 */
const showQuickSearch = ref(false)
const quickSearchTitle = ref('')
const quickSearchFieldKey = ref('')
const quickSearchOptions = ref<{ label: string; value: any }[]>([])
const quickSearchValue = ref<any>(undefined)

function handleSearchBtn(fieldKey: string, _btnText: string) {
  const optionMap: Record<string, { title: string; list: any[]; label: (o: any) => string }> = {
    customerId: { title: '客户', list: optionRefs.customers, label: c => `${c.name}${c.code ? `[${c.code}]` : ''}` },
    warehouseId: { title: '入库仓库', list: optionRefs.warehouses, label: w => w.warehouseName || w.name },
    handlerId: { title: '经手人', list: optionRefs.users, label: u => `${u.name}${u.deptName ? `(${u.deptName})` : ''}` },
    deptId: { title: '部门', list: departmentOptions.value, label: d => d.name },
    contactName: { title: '联系人', list: optionRefs.customers, label: c => `${c.name}${c.contactName ? `(${c.contactName})` : ''}` },
    paymentAccount1: { title: '付款账户', list: paymentAccounts.value, label: a => `${a.name}${a.code ? `(${a.code})` : ''}` },
  }
  const target = optionMap[fieldKey]
  if (target && target.list?.length) {
    quickSearchTitle.value = target.title
    quickSearchFieldKey.value = fieldKey
    quickSearchOptions.value = target.list.map(o => ({ label: target.label(o), value: o.id ?? o.name }))
    quickSearchValue.value = (formData as any)[fieldKey]
    showQuickSearch.value = true
    return
  }
  message.info('该字段可直接输入或选择')
}

function handleQuickSearchConfirm(val: any) {
  if (val == null) return
  const fieldKey = quickSearchFieldKey.value
  ;(formData as any)[fieldKey] = val
  handleFieldChange(fieldKey, val)
  showQuickSearch.value = false
}

// ═══ 收款账户下拉（真实数据源） ═══
const paymentAccounts = ref<any[]>([])
async function loadPaymentAccounts() {
  try {
    paymentAccounts.value = await optionsApi.getAccounts()
  } catch {
    paymentAccounts.value = []
  }
}

function handleOpenProductSelectModal(record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length

  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) {
      handleAddProduct()
    }
  }

  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      const row = formData.products[rowIndex]
      row.productId = p.id
      row.productName = p.name || ''
      row.itemCode = p.code || ''
      row.productCode = p.code || ''
      row.barcode = p.barcode || ''
      row.specification = p.specification || ''
      row.productSpec = p.specification || ''
      row.unit = p.unit || ''
      row.productUnit = p.unit || ''
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      row.retailPrice = p.retailPrice || 0
      row.wholesalePrice = p.wholesalePrice || 0
      row.minSalePrice = p.minSalePrice || 0
      row.lastSalePrice = p.lastSalePrice || 0
      row.lastSaleDate = p.lastSaleDate || null
      row.refCostPrice = p.costPrice || p.avgCost || 0
      row.brand = p.brand || ''
      row.originPlace = p.originPlace || p.origin || ''
      row.modelNo = p.modelNo || p.model || ''
      row.area = p.area || ''
      row.availableStock = p.stock || p.availableStock || 0
      row.bookStock = p.bookStock || p.stock || 0
      row.taxRate = 13
      row.priceLevel1 = p.priceLevel1 || 0
      row.priceLevel2 = p.priceLevel2 || 0
      row.priceLevel3 = p.priceLevel3 || 0
      row.priceLevel4 = p.priceLevel4 || 0
      row.priceLevel5 = p.priceLevel5 || 0
      row.priceLevel6 = p.priceLevel6 || 0
      row.priceLevel7 = p.priceLevel7 || 0
      row.priceLevel8 = p.priceLevel8 || 0
    }
  })

  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history':
      router.push('/sales/return-doc')
      break
    case 'print':
      handlePrint()
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExportDoc()
      break
  }
}

/** 配置弹窗变更：字段显隐立即生效；录单默认值 / 打印设置重新回读 */
function handleFormConfigChange(config: any) {
  if (config.pageFields) {
    const incoming = new Map<string, any>(config.pageFields.map((f: any) => [String(f.key), f]))
    formFieldVisibility.value = SALE_RETURN_DOC_PAGE_FIELDS.map(df => {
      const hit = incoming.get(df.key)
      if (!hit) return { ...df }
      return {
        ...df,
        displayName: hit.displayName || df.displayName,
        visible: hit.visible !== false,
        enterJump: hit.enterJump ?? df.enterJump,
      }
    })
  }
  if (config.printSettings) {
    printSettings.value = { ...printSettings.value, ...config.printSettings }
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ═══════════════════════════════════════
// 打印 / 导入 / 导出
// ═══════════════════════════════════════

/** 打印数据：交给打印模板渲染（含主表与明细） */
const printData = computed(() => ({
  ...formData,
  orderNo: formData.orderNo,
  returnDocNo: formData.orderNo,
  items: formData.products.filter((p: any) => p.productId != null),
}))

/** 打印(F8)：调用打印弹窗（真实模板渲染 + 打印） */
function handlePrint() {
  if (!formData.orderNo) {
    message.warning('请先保存单据后再打印')
    return
  }
  printDialogRef.value?.open()
}

/** 打印成功后累加打印次数（随下次保存落库） */
function handlePrintSuccess() {
  formData.printCount = (formData.printCount || 0) + 1
  message.success('打印完成')
}

/** 打印记录：展示当前单据的打印次数与最近打印时间 */
function handleShowPrintRecord() {
  Modal.info({
    title: '打印记录',
    content: `单据编号：${formData.orderNo || '(未保存)'}\n打印次数：${formData.printCount || 0}\n最近打印时间：${formData.printTime || '—'}`,
  })
}

/** 导入：读取 CSV/TXT（列：货号/条码/商品名称, 数量, 单价），匹配系统商品生成明细 */
function handleImport() {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = '.csv,.txt'
  fileInput.onchange = () => {
    const file = fileInput.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e: any) => parseImportText(String(e.target?.result || ''))
    reader.readAsText(file, 'UTF-8')
  }
  fileInput.click()
}

function parseImportText(text: string) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  if (lines.length <= 1) {
    message.warning('未解析到有效明细数据')
    return
  }
  const start = /货号|商品|条码/.test(lines[0]) ? 1 : 0
  let imported = 0
  for (let i = start; i < lines.length; i++) {
    const cols = lines[i].split(',').map(c => c.trim().replace(/^"|"$/g, ''))
    if (!cols[0]) continue
    const product = optionRefs.products.find((p: any) =>
      p.code === cols[0] || p.barcode === cols[0] || p.name === cols[0])
    if (!product) continue
    let row = formData.products.find((p: any) => p.productId == null)
    if (!row) {
      handleAddProduct()
      row = formData.products[formData.products.length - 1]
    }
    row.productId = product.id
    handleCellChange(row, 'productId', product.id)
    row.quantity = Number(cols[1]) || 0
    if (cols[2] !== undefined && cols[2] !== '') row.unitPrice = Number(cols[2]) || 0
    handleCellChange(row, 'quantity', row.quantity)
    imported++
  }
  if (imported > 0) {
    message.success(`已导入 ${imported} 行明细`)
  } else {
    message.warning('未匹配到系统商品，请检查货号/条码列')
  }
}

/** 导出：当前单据明细导出为 CSV（Excel 可直接打开） */
function handleExportDoc() {
  const rows = formData.products.filter((p: any) => p.productId != null)
  if (!rows.length) {
    message.warning('暂无明细可导出')
    return
  }
  const headers = ['货号', '商品名称', '规格', '单位', '退货数量', '单价', '金额', '明细备注']
  const csvRows = rows.map((p: any) => [
    p.itemCode || '', p.productName || '', p.specification || '', p.unit || '',
    p.quantity || 0, p.unitPrice || 0, (p.quantity || 0) * (p.unitPrice || 0), p.itemRemark || '',
  ].map(v => String(v).replace(/[",\n\r]/g, ' ')))
  const csv = [headers.join(','), ...csvRows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${formData.orderNo || '销售退货单'}_${formatNow().replace(/[: ]/g, '-')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success(`已导出 ${rows.length} 行明细`)
}

/** 应用录单默认值（仅新建时） */
function loadAndApplyDefaults() {
  try {
    const raw = localStorage.getItem(FORM_DEFAULTS_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    const defaults: Array<{ key: string; value: any }> = parsed.fields || parsed
    if (!Array.isArray(defaults)) return
    defaults.forEach(d => {
      if (d.value === undefined || d.value === null || d.value === '') return
      const numKeys = ['customerId', 'warehouseId', 'handlerId', 'deptId']
      ;(formData as any)[d.key] = numKeys.includes(d.key) ? Number(d.value) : d.value
    })
    // 联动快照（客户/仓库/经手人/部门名称）
    if (formData.customerId) handleFieldChange('customerId', formData.customerId)
    if (formData.warehouseId) handleFieldChange('warehouseId', formData.warehouseId)
    if (formData.handlerId) handleFieldChange('handlerId', formData.handlerId)
    if (formData.deptId) handleFieldChange('deptId', formData.deptId)
  } catch {
    // 存档损坏时忽略
  }
}

// 「保存后立即打印」已接到 useBillForm 的 afterSave 回调（见上方配置），此处的独立函数已删除。

// ── 生命周期 ──
onMounted(() => {
  // 初始化空行
  if (formData.products.length === 0) {
    for (let i = 0; i < 15; i++) handleAddProduct()
  }
  loadDepartments()
  loadPaymentAccounts()
  loadPrintSettings()
  // 新建模式应用录单默认值
  if (!route.params.id) {
    loadAndApplyDefaults()
  }
  // 列表页「打印(F8)/批量打印」跳转携带 print=1：单据加载完成后自动唤起打印
  if (route.query.print === '1') {
    setTimeout(() => handlePrint(), 800)
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

.doc-info-row {
  display: flex; align-items: center; gap: 16px;
  padding: 6px 0; font-size: 12px; color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
}
.doc-info-link { color: #1890ff; font-size: 12px; }
</style>
