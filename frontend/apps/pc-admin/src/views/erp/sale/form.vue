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
          <!-- 自定义：操作列 -->
          <template #actionCell="{ index, empty }">
            <template v-if="!empty">
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  class="action-add-btn"
                  @click="handleInsertProduct(index)"
                >
                  <PlusCircleOutlined />
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  class="action-del-btn"
                  @click="handleRemoveProduct(index)"
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
                  @click="handleAddProduct()"
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
      </template>

      <!-- ═══ Zone 4 补充: 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div v-if="isFieldVisible('orderRemark')" class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input
              v-model:value="formData.orderRemark"
              size="small"
              class="remark-input"
            />
          </div>
          <div v-if="isFieldVisible('buyerRemark')" class="remark-row">
            <span class="remark-label">买家备注</span>
            <a-input
              v-model:value="formData.buyerRemark"
              size="small"
              class="remark-input"
            />
          </div>
          <div v-if="isFieldVisible('footerExtText1')" class="remark-row">
            <span class="remark-label">表尾自定义1</span>
            <a-input
              v-model:value="formData.footerExtText1"
              size="small"
              class="remark-input"
            />
          </div>
          <div v-if="isFieldVisible('footerExtText2')" class="remark-row">
            <span class="remark-label">表尾自定义2</span>
            <a-input
              v-model:value="formData.footerExtText2"
              size="small"
              class="remark-input"
            />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag
            color="blue"
            size="small"
          >{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <a-button
            type="link"
            size="small"
            class="doc-info-link"
          >
            打印记录
          </a-button>
          <span class="doc-info-item">源单 <a-tag size="small">{{ formData.sourceOrder || '0' }}</a-tag></span>
          <span class="doc-info-item">审核人 <a-tag size="small">{{ formData.auditorName || '-' }}</a-tag></span>
        </div>
        <!-- 信用额度超限警告 -->
        <a-alert
          v-if="creditWarning"
          :message="creditWarning"
          type="warning"
          show-icon
          closable
          banner
          style="margin-top:8px"
        />
      </template>
    </BillFormPage>

    <!-- ═══ 表单配置弹窗（已迁移到 SaleOrderItemColumnConfig 独立页面） ═══ -->

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 打印弹窗 ═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="sale"
      :print-data="printData"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'

defineOptions({ name: 'SaleForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, TabField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 配置弹窗状态 ═══
// showFormConfig 已迁移到 SaleOrderItemColumnConfig 独立页面
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)

// ═══ 页面字段可见性配置（从 localStorage 读取）═══
const PAGE_VISIBILITY_KEY = 'sale-order-form-page-config'
const pageVisibilityMap = ref<Record<string, boolean>>({})

function loadPageVisibility() {
  try {
    const stored = localStorage.getItem(PAGE_VISIBILITY_KEY)
    if (stored) {
      const parsed = JSON.parse(stored) as Array<{ key: string; visible: boolean }>
      const map: Record<string, boolean> = {}
      parsed.forEach(f => { map[f.key] = f.visible })
      pageVisibilityMap.value = map
    }
  } catch {
    pageVisibilityMap.value = {}
  }
}

function isFieldVisible(key: string, defaultValue = true): boolean {
  // 如果配置里没有该字段的配置，使用默认值
  if (!(key in pageVisibilityMap.value)) return defaultValue
  return pageVisibilityMap.value[key]
}

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const returnQty = ref(0)
const tableMaxHeight = ref(400)

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
  billPrefix: 'XSDD',
  api: {
    create: saleOrderApi.create,
    update: saleOrderApi.update,
    getById: async (id: number) => {
      const res = await saleOrderApi.getById(id)
      const data = res?.data || res || {}
      // 后端DTO字段名映射到前端表单字段名
      return {
        ...data,
        // 备注映射
        orderRemark: data.remark || data.orderRemark || '',
        // 收货地址
        receiverAddress: data.shippingAddress || data.receiverAddress || '',
        // 经手人ID映射
        salespersonId: data.salesmanId,
        salespersonName: data.salesmanName || '',
        // 物流信息从冗余字段取
        logisticsCompany: data.logisticsCompany || '',
        shippingFee: data.shippingFee || 0,
        waybillNo: data.waybillNo || '',
        freightPayer: data.freightPayer || '',
        codAmount: data.codAmount || 0,
        // 预计发货
        estimatedShipDate: data.expectedShipTime || '',
        // 配送信息
        deliveryMethod: data.deliveryMethod || '',
        deliveryRoute: data.deliveryRoute || '',
        driverName: data.driverName || '',
        deliveryVehicle: data.deliveryVehicle || '',
        // 结款信息
        settlementMethod: data.settlementMethod || '',
        depositAccount: data.depositAccount || '',
        depositAmount: data.depositAmount || 0,
        creditLimit: data.creditLimit || 0,
        availableCredit: data.availableCredit || 0,
        prevDebt: data.prevDebt || 0,
        paymentDate: data.paymentDate || '',
        reconciliationDate: data.reconciliationDate || '',
        // 会员信息
        memberCardNo: data.memberCardNo || '',
        memberName: data.memberName || '',
        prevPoints: data.prevPoints || 0,
        salePoints: data.salePoints || 0,
        currentPoints: data.currentPoints || 0,
        // 金额
        promoDiscount: data.promoDiscount || 0,
        couponAmount: data.couponAmount || 0,
        directDiscount: data.directDiscount || 0,
        otherFee: data.otherFee || 0,
        // 物理汇总
        totalWeight: data.totalWeight || 0,
        totalVolume: data.totalVolume || 0,
        // 扩展信息
        region: data.region || '',
        attachment: data.attachment || '',
        extNum1: data.extNum1 || 0,
        extNum2: data.extNum2 || 0,
        extText1: data.extText1 || '',
        extText2: data.extText2 || '',
        extText3: data.extText3 || '',
        footerExtText1: data.footerExtText1 || '',
        footerExtText2: data.footerExtText2 || '',
        // 银行/税务
        bankName: data.bankName || '',
        bankAccount: data.bankAccount || '',
        taxNo: data.taxNo || '',
        customerRemark: data.customerRemark || '',
        customerTicket: data.customerTicket || '',
      }
    },
  },
  redirectPath: '/erp/sale',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', location: '',
    unit: '', lineAttribute: '', batchCode: '',
    productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || c.customerCode || ''
        fd.customerLevel = c.level || c.customerLevel || ''
        fd.receiverName = c.contactName || ''
        fd.receiverPhone = c.contactPhone || ''
        fd.receiverAddress = c.address || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        fd.customerRemark = c.remark || ''
      }
      // 异步加载客户信用额度信息
      if (val != null) {
        saleOrderApi.getCustomerCredit(val).then((res: any) => {
          const data = res.data || res
          fd.creditLimit = data.creditLimit || 0
          fd.availableCredit = data.availableCredit || 0
          fd.prevDebt = data.currentDebt || 0
        }).catch(() => {
          fd.creditLimit = 0
          fd.availableCredit = 0
          fd.prevDebt = 0
        })
        // 异步加载客户订金余额
        saleOrderApi.getCustomerDeposits(val).then((res: any) => {
          const list = res.data || res
          if (Array.isArray(list) && list.length > 0) {
            fd.depositAccount = list[0].accountName || ''
            fd.depositAmount = list[0].amount || 0
            fd.prevAdvance = list.reduce((sum: number, d: any) => sum + (d.amount || 0), 0)
            fd.advanceBalance = fd.prevAdvance
          }
        }).catch(() => {
          // 静默失败
        })
      } else {
        fd.creditLimit = 0
        fd.availableCredit = 0
        fd.prevDebt = 0
        fd.depositAccount = ''
        fd.depositAmount = 0
        fd.prevAdvance = 0
        fd.advanceBalance = 0
      }
    }
    // 仓库变更时重新查询所有商品行的可用库存
    if (fieldKey === 'warehouseId' && val != null) {
      refreshAllProductStock(fd.products, val)
    }
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    // 汇总金额
    totalAmount: totalAmount.value,
    productAmount: totalAmount.value,
    promoDiscount: fd.promoDiscount || 0,
    discountAmount: fd.discountAmount || 0,
    couponAmount: fd.couponAmount || 0,
    directDiscount: fd.directDiscount || 0,
    otherFee: fd.otherFee || 0,
    billAmount: billAmount.value,
    settledAmount: fd.settledAmount || 0,
    // 数量汇总
    totalQuantity: totalQuantity.value,
    returnQuantity: returnQty.value,
    // 税额
    taxAmount: taxAmount.value,
    totalAmountWithTax: totalAmountWithTax.value,
    // 物理汇总
    totalWeight: totalWeight.value,
    totalVolume: totalVolume.value,
    // 经手人映射
    salesmanId: fd.salespersonId,
    salesmanName: fd.salespersonName,
    shippingAddress: fd.receiverAddress,
    // 明细行
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      image: p.image || '',
      itemCode: p.itemCode || '',
      barcode: p.barcode || '',
      smallUnitBarcode: p.smallUnitBarcode || '',
      specification: p.specification || '',
      model: p.model || '',
      origin: p.origin || '',
      brand: p.brand || '',
      shelfLife: p.shelfLife || '',
      unit: p.unit || '',
      pricingUnit: p.pricingUnit || '',
      smallUnit: p.smallUnit || '',
      smallUnitQuantity: p.smallUnitQuantity || 0,
      lineAttribute: p.lineAttribute || '',
      area: p.area || '',
      location: p.location || '',
      batchCode: p.batchCode || '',
      productionDate: p.productionDate || null,
      expiryDate: p.expiryDate || null,
      quantity: p.quantity,
      bigPack: p.bigPack,
      midPack: p.midPack,
      smallPack: p.smallPack,
      unitPrice: p.unitPrice,
      smallUnitPrice: p.smallUnitPrice || 0,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      discountRate: p.discountRate || 0,
      discountedUnitPrice: p.discountedUnitPrice || 0,
      discountedAmount: p.discountedAmount || 0,
      discountPercent: p.discountPercent || 0,
      favorableUnitPrice: p.favorableUnitPrice || 0,
      favorableAmount: p.favorableAmount || 0,
      originalPrice: p.originalPrice || 0,
      costPrice: p.costPrice || 0,
      costAmount: p.costAmount || 0,
      grossProfit: p.grossProfit || 0,
      retailPrice: p.retailPrice || 0,
      wholesalePrice: p.wholesalePrice || 0,
      lowestPrice: p.lowestPrice || 0,
      latestSaleDate: p.latestSaleDate || '',
      latestSalePrice: p.latestSalePrice || 0,
      availableStock: p.availableStock || 0,
      availableStockConverted: p.availableStockConverted || 0,
      bookStock: p.bookStock || 0,
      conversionRelation: p.conversionRelation || '',
      unshippedQuantity: p.unshippedQuantity || 0,
      shippedQuantityDetail: p.shippedQuantityDetail || 0,
      // 客户类型（价格等级标准化字段）
      restaurant: p.restaurant || false,
      canteen: p.canteen || false,
      vipSelf: p.vipSelf || false,
      largeGroup: p.largeGroup || false,
      specialCustomer: p.specialCustomer || false,
      outRestaurant: p.outRestaurant || false,
      vipLevel1: p.vipLevel1 || false,
      vipLevel2: p.vipLevel2 || false,
      // 预订货
      preOrderNo: p.preOrderNo || '',
      usePreOrderAmount: p.usePreOrderAmount || 0,
      // 积分/礼品
      giftItem: p.giftItem || '',
      exchangePoints: p.exchangePoints || 0,
      usedPoints: p.usedPoints || 0,
      // 物理属性
      volume: p.volume || 0,
      weight: p.weight || 0,
      gift: p.gift || false,
      // 自定义字段
      customField1: p.customField1 || 0,
      customField2: p.customField2 || 0,
      customField3: p.customField3 || 0,
      customField4: p.customField4 || '',
      customField5: p.customField5 || '',
      customField6: p.customField6 || 0,
      customField7: p.customField7 || 0,
      customField8: p.customField8 || 0,
      customField9: p.customField9 || 0,
      customField10: p.customField10 || 0,
      remark: p.remark || '',
      warehouseId: fd.warehouseId,
    })),
  }),
})

// 初始化销售订单特有字段
if (!('customerId' in formData)) {Object.assign(formData, {
  // 客户
  customerId: undefined, customerName: '', customerCode: '', customerLevel: '',
  customerRemark: '', customerGradeCode: '', customerGradeName: '', customerTicket: '',
  // 银行/税务
  bankName: '', bankAccount: '', taxNo: '',
  // 仓库/经手人
  warehouseId: undefined, warehouseName: '',
  salespersonId: undefined, salespersonName: '',
  deptId: undefined, deptName: '',
  // 日期/类型
  orderDate: '', saleType: 1,
  // 收货
  receiverName: '', receiverPhone: '', receiverAddress: '',
  contactName: '', contactPhone: '', pickupAddress: '',
  // 配送
  deliveryMethod: '', deliveryRoute: '', deliveryRouteId: undefined,
  driverId: undefined, driverName: '', deliveryVehicle: '',
  // 物流
  logisticsCompany: '', logisticsNo: '', freightPayer: '',
  shippingFee: 0, waybillNo: '', codAmount: 0,
  estimatedShipDate: '',
  // 结算
  settlementMethod: '',
  // 收款/订金
  paymentAccountId: undefined, depositAccount: '', depositAmount: 0,
  moreAccounts: '', useAdvancePayment: 0,
  prevAdvance: 0, advanceBalance: 0,
  // 信用
  creditLimit: 0, availableCredit: 0, prevDebt: 0,
  // 收款日/对账日
  paymentDate: '', reconciliationDate: '',
  // 会员/积分
  memberCardNo: '', memberName: '', memberDiscount: 100,
  prevPoints: 0, salePoints: 0, returnPoints: 0,
  exchangePointsHeader: 0, usedPointsHeader: 0, currentPoints: 0,
  // 金额
  productAmount: 0, promoDiscount: 0, couponAmount: 0,
  directDiscount: 0, discountAmount: 0, otherFee: 0,
  billAmount: 0, settledAmount: 0,
  // 数量
  totalQuantity: 0, shippedQuantity: 0, unshippedQuantity: 0,
  returnQuantity: 0, returnAmount: 0,
  // 物理
  totalWeight: 0, totalVolume: 0,
  // 备注
  orderRemark: '', buyerRemark: '', summary: '',
  footerExtText1: '', footerExtText2: '',
  // 自定义字段（表头）
  extNum1: 0, extNum2: 0,
  extText1: '', extText2: '', extText3: '',
  // 审核/制单
  auditorName: '', creatorName: '', printCount: 0, sourceOrder: '',
  // 区域/附件
  region: '', attachment: '',
})}

// ── 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const taxAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value - (formData.discountAmount || 0) + (formData.otherFee || 0))
const billAmount = computed(() => totalAmount.value - (formData.promoDiscount || 0) - (formData.discountAmount || 0) + (formData.otherFee || 0))
const totalWeight = computed(() => formData.products.reduce((s: number, p: any) => s + (p.weight || 0) * (p.quantity || 0), 0))
const totalVolume = computed(() => formData.products.reduce((s: number, p: any) => s + (p.volume || 0) * (p.quantity || 0), 0))

// 信用额度超限警告
const creditWarning = computed(() => {
  const available = Number(formData.availableCredit) || 0
  const bill = billAmount.value
  const creditLimit = Number(formData.creditLimit) || 0
  if (creditLimit > 0 && available < bill) {
    return `客户信用额度不足：可用额度 ${available.toFixed(2)}，本单金额 ${bill.toFixed(2)}，超出 ${(bill - available).toFixed(2)}`
  }
  if (creditLimit > 0 && available > 0 && bill > available * 0.8) {
    return `客户信用额度即将超限：可用额度 ${available.toFixed(2)}，本单金额 ${bill.toFixed(2)}，建议控制订单金额`
  }
  return ''
})

/** 表格合计列定义 */
const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value, highlight: true },
  { key: 'midPack', value: totalMidPack.value, highlight: true },
  { key: 'smallPack', value: totalSmallPack.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售订单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-order', label: '打印订单' },
      { key: 'print-summary', label: '打印汇总' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'more', label: '更多', children: [
      { key: 'save-draft', label: '保存草稿' },
      { key: 'copy-order', label: '复制订单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const accountOptions = ref<any[]>([])

// ── 基本信息字段（Zone 2）── 过滤掉被配置隐藏的字段
const allBasicInfoFields = computed<BasicInfoField[]>(() => [
  // 第一行：核心字段
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'customerCode', label: '客户编号', type: 'input', inlineLabel: true, width: 160 },
  { key: 'warehouseId', label: '发货仓库', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'salespersonId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [{ label: '正常销售', value: 1 }, { label: '样品销售', value: 2 }, { label: '促销销售', value: 3 }, { label: '退货物料', value: 4 }] },
  // 第二行：扩展字段
  { key: 'deptName', label: '部门', type: 'input', inlineLabel: true, width: 160 },
  { key: 'customerLevel', label: '客户级别', type: 'input', inlineLabel: true, width: 140 },
  { key: 'deliveryMethod', label: '配送方式', type: 'select', inlineLabel: true, width: 160, options: [{ label: '自提', value: '自提' }, { label: '送货上门', value: '送货上门' }, { label: '物流配送', value: '物流配送' }, { label: '快递', value: '快递' }] },
  { key: 'deliveryRoute', label: '配送线路', type: 'input', inlineLabel: true, width: 160 },
  { key: 'receiverName', label: '收货人', type: 'input', inlineLabel: true, width: 140, searchBtn: 'Q' },
  { key: 'receiverPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 160 },
  { key: 'receiverAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 520 },
  { key: 'settlementMethod', label: '结款方式', type: 'select', inlineLabel: true, width: 160, options: [{ label: '现结', value: '现结' }, { label: '月结', value: '月结' }, { label: '预收', value: '预收' }, { label: '货到付款', value: '货到付款' }] },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 400 },
  // 第三行：系统字段
  { key: 'auditorName', label: '审核人', type: 'input', inlineLabel: true, width: 160, disabled: true },
  { key: 'creatorName', label: '制单人', type: 'input', inlineLabel: true, width: 160, disabled: true },
  { key: 'bookkeepingTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'number', inlineLabel: true, width: 160, disabled: true },
  { key: 'sourceOrder', label: '源单', type: 'input', inlineLabel: true, width: 160, disabled: true },
])
const basicInfoFields = computed<BasicInfoField[]>(() =>
  allBasicInfoFields.value.filter(f => isFieldVisible(f.key))
)

// ── 底部标签页配置 ── 过滤掉被配置隐藏的字段
const tabsConfig = computed<BillTabConfig[]>(() => [
  // ═══ Tab 1: 收款（对标系统字段顺序） ═══
  { key: 'payment', tab: '收款', fields: ([
    { key: 'depositAccountId', label: '订金账户', type: 'select', placeholder: '请选择', options: accountOptions.value.map((a: any) => ({ label: a.name, value: a.id })), suffixBtn: '+Q' },
    { key: 'depositAmount', label: '订金金额', type: 'number', placeholder: '0.00', precision: 2, suffixBtn: '全' },
    { key: 'moreAccounts', label: '更多账户', type: 'input', disabled: true, suffixBtn: '···' },
    { key: 'depositBalance', label: '订金余额', type: 'number', disabled: true, precision: 2 },
    { key: 'useAdvancePayment', label: '使用预订货款', type: 'number', disabled: true, suffixBtn: '···', precision: 2 },
    { key: 'prevAdvance', label: '此前预收', type: 'number', disabled: true, precision: 2 },
    { key: 'usePreReceipt', label: '使用预收款', type: 'number', placeholder: '0.00', precision: 2 },
    { key: 'advanceBalance', label: '预收余额', type: 'number', disabled: true, precision: 2 },
    { key: 'currentDebt', label: '本次欠款', type: 'number', disabled: true, precision: 2 },
    { key: 'debtBalance', label: '欠款余额', type: 'number', disabled: true, precision: 2 },
    { key: 'unsettledAmount', label: '本单未结金额', type: 'number', disabled: true, precision: 2 },
    { key: 'paymentMethod', label: '付款方式', type: 'select', placeholder: '请选择', options: [{ label: '现金', value: '现金' }, { label: '银行转账', value: '银行转账' }, { label: '微信', value: '微信' }, { label: '支付宝', value: '支付宝' }, { label: '支票', value: '支票' }] },
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)) },
  // ═══ Tab 2: 物流信息 ═══
  { key: 'logistics', tab: '物流信息', fields: ([
    { key: 'deliveryMethod', label: '配送方式', type: 'select', options: [{ label: '自提', value: '自提' }, { label: '送货上门', value: '送货上门' }, { label: '物流配送', value: '物流配送' }, { label: '快递', value: '快递' }] },
    { key: 'deliveryRoute', label: '配送线路', type: 'input', placeholder: '请输入配送线路' },
    { key: 'driverName', label: '司机', type: 'input', placeholder: '请输入司机姓名' },
    { key: 'deliveryVehicle', label: '车辆', type: 'input', placeholder: '请输入车牌号' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', placeholder: '请输入物流公司' },
    { key: 'freightPayer', label: '运费承担方', type: 'select', options: [{ label: '卖方承担', value: '卖方承担' }, { label: '买方承担', value: '买方承担' }, { label: '第三方承担', value: '第三方承担' }] },
    { key: 'shippingFee', label: '运费', type: 'number', placeholder: '0.00', precision: 2 },
    { key: 'waybillNo', label: '运单号', type: 'input', placeholder: '请输入运单号' },
    { key: 'codAmount', label: '代收货款', type: 'number', placeholder: '0.00', precision: 2 },
    { key: 'estimatedShipDate', label: '预计发货', type: 'date' },
    { key: 'contactName', label: '联系人', type: 'input', placeholder: '请输入联系人' },
    { key: 'contactPhone', label: '联系电话(提货)', type: 'input', placeholder: '请输入联系电话' },
    { key: 'pickupAddress', label: '提货地址', type: 'input', placeholder: '请输入提货地址' },
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)) },
  // ═══ Tab 3: 会员信息 ═══
  { key: 'member', tab: '会员信息', fields: ([
    { key: 'memberCardNo', label: '会员卡号', type: 'input', placeholder: '请输入会员卡号' },
    { key: 'memberName', label: '会员姓名', type: 'input', placeholder: '请输入会员姓名' },
    { key: 'prevPoints', label: '此前积分', type: 'number', disabled: true, precision: 2 },
    { key: 'salePoints', label: '销售积分', type: 'number', disabled: true, precision: 2 },
    { key: 'returnPoints', label: '退货积分', type: 'number', disabled: true, precision: 2 },
    { key: 'exchangePointsHeader', label: '兑换积分', type: 'number', disabled: true, precision: 2 },
    { key: 'usedPointsHeader', label: '使用积分', type: 'number', disabled: true, precision: 2 },
    { key: 'currentPoints', label: '当前积分', type: 'number', disabled: true, precision: 2 },
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)) },
  // ═══ Tab 4: 扩展信息 ═══
  { key: 'extended', tab: '扩展信息', fields: ([
    { key: 'bankName', label: '开户行', type: 'input', placeholder: '请输入开户行' },
    { key: 'bankAccount', label: '银行账号', type: 'input', placeholder: '请输入银行账号' },
    { key: 'taxNo', label: '税号', type: 'input', placeholder: '请输入税号' },
    { key: 'customerRemark', label: '客户备注', type: 'input', placeholder: '请输入客户备注' },
    { key: 'customerTicket', label: '客户一票通', type: 'select', options: [{ label: '是', value: '是' }, { label: '否', value: '否' }] },
    { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', precision: 2 },
    { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', precision: 2 },
    { key: 'extText1', label: '自定义字段3(文本)', type: 'input' },
    { key: 'extText2', label: '自定义字段4(文本)', type: 'input' },
    { key: 'extText3', label: '自定义字段5(文本)', type: 'input' },
    { key: 'region', label: '区域', type: 'input', placeholder: '请输入区域' },
    { key: 'attachment', label: '附件', type: 'input', placeholder: '附件路径' },
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)) },
])

// ── 摘要面板 ── 过滤掉被配置隐藏的字段
const summaryConfig = computed<SummaryRow[]>(() => {
  const allRows: Array<SummaryRow & { _filterKey: string }> = [
    { _filterKey: 'saleQuantity', label: '销售数量', value: totalQuantity.value, statusLabel: '待结算' },
    { _filterKey: 'returnQuantity', label: '退货数量', value: returnQty.value },
    { _filterKey: 'productAmount', label: '商品金额', value: totalAmount.value.toFixed(2) },
    { _filterKey: 'promoDiscount', label: '促销优惠', value: (formData.promoDiscount || 0).toFixed(2) },
    { _filterKey: 'discountAmount', label: '优惠金额', value: (formData.discountAmount || 0).toFixed(2), divider: true, showMore: true },
    { _filterKey: 'otherFee', label: '其他费用', value: (formData.otherFee || 0).toFixed(2), divider: true, showMore: true },
    { _filterKey: 'billAmount', label: '本单金额', value: billAmount.value.toFixed(2), divider: true },
    { _filterKey: 'taxAmount', label: '税额', value: (formData.taxAmount || 0).toFixed(2) },
    { _filterKey: 'totalWeight', label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
    { _filterKey: 'totalVolume', label: '总体积(m³)', value: totalVolume.value.toFixed(4) },
  ]
  return allRows.filter(r => isFieldVisible(r._filterKey)).map(({ _filterKey, ...row }) => row)
})

// ── 页脚 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${billAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置 - 完整76字段
// ═══════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => [
  // ── 固定列 ──
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },

  // ── 商品信息 ──
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name,
      value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true,
  },
  { key: 'preOrderNo', title: '预订货单编号', type: 'input', width: 130, defaultHidden: true },
  { key: 'usePreOrderAmount', title: '使用预订货款', type: 'number', width: 120, precision: 2, defaultHidden: true },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'smallUnitBarcode', title: '小单位条码', type: 'input', width: 120, defaultHidden: true },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },

  // ── 单位/包装 ──
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'pricingUnit', title: '计价单位(定价)', type: 'input', width: 110, defaultHidden: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'lineAttribute', title: '商品行属性', type: 'input', width: 100 },
  { key: 'area', title: '区域', type: 'input', width: 80, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 80 },

  // ── 批次 ──
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },

  // ── 数量 ──
  { key: 'quantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 0 },

  // ── 库存 ──
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 150, precision: 2, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100, defaultHidden: true },
  { key: 'unshippedQuantity', title: '未发数量', type: 'number', width: 100, precision: 2 },
  { key: 'shippedQuantityDetail', title: '已发数量', type: 'number', width: 100, precision: 2 },

  // ── 价格 ──
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'latestSaleDate', title: '最近销售日期', type: 'date', width: 120, defaultHidden: true },
  { key: 'latestSalePrice', title: '最近售价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'lowestPrice', title: '最低售价', type: 'number', width: 100, precision: 2, defaultHidden: true },

  // ── 客户类型（价格等级标准化字段，非用户昵称）──
  { key: 'restaurant', title: '餐饮店', type: 'boolean', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'boolean', width: 90, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'boolean', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'boolean', width: 70, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'boolean', width: 90, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'boolean', width: 110, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'boolean', width: 100, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'boolean', width: 90, defaultHidden: true },

  // ── 成本 ──
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 120, precision: 2 },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 120, precision: 2, readonly: true },
  { key: 'grossProfit', title: '参考毛利', type: 'number', width: 100, precision: 2, readonly: true },

  // ── 折扣 ──
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'discountedUnitPrice', title: '折后单价', type: 'number', width: 100, precision: 2 },
  { key: 'originalPrice', title: '折单原价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'discountPercent', title: '优惠折扣(%)', type: 'number', width: 110, precision: 2 },
  { key: 'favorableUnitPrice', title: '惠后单价', type: 'number', width: 100, precision: 2 },
  { key: 'favorableAmount', title: '优惠后金额', type: 'number', width: 110, precision: 2, readonly: true },

  // ── 积分/礼品 ──
  { key: 'giftItem', title: '兑换礼品', type: 'input', width: 100, defaultHidden: true },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 90, precision: 2, defaultHidden: true },

  // ── 物理属性 ──
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4 },
  { key: 'gift', title: '赠品', type: 'input', width: 60 },

  // ── 单据自定义字段(明细行) ──
  { key: 'customField1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField4', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField5', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField6', title: '单据自定义6(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField7', title: '单据自定义7(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField8', title: '单据自定义8(往来单位)', type: 'input', width: 150, defaultHidden: true },
  { key: 'customField9', title: '单据自定义9(职员)', type: 'input', width: 120, defaultHidden: true },
  { key: 'customField10', title: '单据自定义10(部门)', type: 'input', width: 120, defaultHidden: true },

  // ── 备注 ──
  { key: 'remark', title: '备注', type: 'input', width: 150 },
])

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

/** 在当前行后插入一个空行 */
function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '', barcode: '', smallUnitBarcode: '', specification: '', model: '',
    origin: '', brand: '', location: '',
    unit: '', pricingUnit: '', smallUnit: '',
    lineAttribute: '', area: '', batchCode: '',
    productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    quantity: 0, smallUnitQuantity: 0,
    unitPrice: 0, smallUnitPrice: 0, taxRate: 13,
    availableStock: 0, availableStockConverted: 0, bookStock: 0, conversionRelation: '',
    unshippedQuantity: 0, shippedQuantityDetail: 0,
    latestSaleDate: '', latestSalePrice: 0,
    retailPrice: 0, wholesalePrice: 0, lowestPrice: 0,
    costPrice: 0, costAmount: 0, grossProfit: 0,
    discountRate: 0, discountedUnitPrice: 0, originalPrice: 0,
    discountedAmount: 0, discountPercent: 0,
    favorableUnitPrice: 0, favorableAmount: 0,
    restaurant: false, canteen: false, vipSelf: false, largeGroup: false,
    specialCustomer: false, outRestaurant: false, vipLevel1: false, vipLevel2: false,
    preOrderNo: '', usePreOrderAmount: 0,
    giftItem: '', exchangePoints: 0, usedPoints: 0,
    volume: 0, weight: 0, gift: false,
    customField1: 0, customField2: 0, customField3: 0,
    customField4: '', customField5: '', customField6: 0, customField7: 0,
    customField8: 0, customField9: 0, customField10: 0,
    scanMode: false, remark: '',
  }
  formData.products.splice(index + 1, 0, newProduct)
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
      record.unit = p.unit || ''
      record.brand = p.brand || ''
      record.model = p.model || ''
      record.origin = p.origin || ''
      record.image = p.image || p.imageUrl || ''
      record.unitPrice = p.salePrice || p.retailPrice || p.price || 0
      record.retailPrice = p.retailPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.lowestPrice = p.lowestPrice || p.minPrice || 0
      record.costPrice = p.costPrice || 0
      record.taxRate = 13
      record.weight = p.weight || 0
      record.volume = p.volume || 0
      record.shelfLife = p.shelfLife || ''
      record.smallUnit = p.smallUnit || ''
      record.smallUnitBarcode = p.smallUnitBarcode || ''
      record.pricingUnit = p.pricingUnit || ''
      // 自动计算金额
      record.amount = (record.quantity || 0) * (record.unitPrice || 0)
      record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
      record.grossProfit = record.amount - record.costAmount
      // 查询可用库存
      const warehouseId = formData.warehouseId
      if (warehouseId) {
        saleOrderApi.getStockDetail(value, warehouseId).then((res: any) => {
          const stock = res.data || res
          if (stock) {
            record.availableStock = stock.availableQuantity || 0
            record.bookStock = stock.quantity || 0
          }
        }).catch(() => {
          record.availableStock = 0
          record.bookStock = 0
        })
      }
    }
  }
  // 数量/单价变化时自动计算金额
  if (['quantity', 'unitPrice'].includes(fieldKey)) {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
    record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
    record.grossProfit = record.amount - record.costAmount
    record.weight_total = (record.weight || 0) * (record.quantity || 0)
    record.volume_total = (record.volume || 0) * (record.quantity || 0)
  }
  // 折扣计算
  if (fieldKey === 'discountRate') {
    const rate = (value || 0) / 100
    record.discountedUnitPrice = (record.unitPrice || 0) * rate
    record.discountedAmount = (record.amount || 0) * rate
  }
  if (fieldKey === 'discountPercent') {
    const pct = (value || 0) / 100
    record.favorableUnitPrice = (record.discountedUnitPrice || record.unitPrice || 0) * pct
    record.favorableAmount = (record.discountedAmount || record.amount || 0) * pct
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能待完善`)
}

/**
 * 仓库变更时刷新所有商品行的可用库存
 */
function refreshAllProductStock(products: any[], warehouseId: number) {
  if (!products || !warehouseId) return
  products.forEach((row: any) => {
    if (row.productId) {
      saleOrderApi.getStockDetail(row.productId, warehouseId).then((res: any) => {
        const stock = res.data || res
        if (stock) {
          row.availableStock = stock.availableQuantity || 0
          row.bookStock = stock.quantity || 0
        }
      }).catch(() => {
        // 静默失败
      })
    }
  })
}

// ═══════════════════════════════════════
// 产品选择弹窗处理
// ═══════════════════════════════════════

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
      row.barcode = p.barcode || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.brand = p.brand || ''
      row.model = p.model || ''
      row.image = p.image || p.imageUrl || ''
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      row.retailPrice = p.retailPrice || 0
      row.wholesalePrice = p.wholesalePrice || 0
      row.costPrice = p.costPrice || 0
      row.taxRate = 13
      row.weight = p.weight || 0
      row.volume = p.volume || 0
      // 查询可用库存
      const warehouseId = formData.warehouseId
      if (warehouseId && p.id) {
        saleOrderApi.getStockDetail(p.id, warehouseId).then((res: any) => {
          const stock = res.data || res
          if (stock) {
            row.availableStock = stock.availableQuantity || 0
            row.bookStock = stock.quantity || 0
          }
        }).catch(() => {
          row.availableStock = 0
          row.bookStock = 0
        })
      }
    }
  })

  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
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
      printDialogRef.value?.open()
      break
    case 'import':
    case 'copy-order':
    case 'export':
      message.info(`${actionKey} 功能待完善`)
      break
    case 'config':
      router.push({ name: 'ErpSaleOrderItemColumnConfig' })
      break
  }
}

// ═══ 打印数据 ═══
const printData = computed(() => ({
  ...formData,
  orderNo: formData.orderNo || '待生成',
  creatorName: formData.creatorName || currentUserName.value || '系统',
  createTime: formData.createTime || formatNow(),
  products: formData.products.filter((p: any) => p.productId != null),
  totalAmount: totalAmount.value,
  billAmount: billAmount.value,
  totalQuantity: totalQuantity.value,
  taxAmount: taxAmount.value,
  totalWeight: totalWeight.value,
  totalVolume: totalVolume.value,
}))

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ═══ 键盘快捷键 ═══
function handleFormKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

// ── 生命周期 ──
onMounted(() => {
  loadPageVisibility()
  // 编辑模式下不初始化空白行，等loadDetail加载真实数据
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
  window.addEventListener('keydown', handleFormKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleFormKeydown)
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
  min-width: 80px;
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

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}
</style>
