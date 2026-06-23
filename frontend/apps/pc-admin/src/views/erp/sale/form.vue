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
      >
        <!-- 自定义：操作列 -->
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

  <!-- ═══ 表单配置弹窗 ═══ -->
  <SaleOrderFormConfig v-model:open="showFormConfig" />
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
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
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import SaleOrderItemColumnConfig from '@/views/erp/column-config/SaleOrderItemColumnConfig.vue' // 导入列配置组件
import SaleOrderFormConfig from '@/views/erp/column-config/SaleOrderFormConfig.vue' // 导入表单配置组件

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 配置弹窗状态 ═══
const showFormConfig = ref(false)

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const returnQty = ref(0)
const promoDiscount = ref(0)
const discountAmount = ref(0)
const otherFee = ref(0)
const tableMaxHeight = ref(400)

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
  billPrefix: 'XSDD',
  api: {
    create: saleOrderApi.create,
    update: saleOrderApi.update,
    getById: saleOrderApi.getById,
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
        fd.receiverName = c.contactName || ''
        fd.receiverPhone = c.contactPhone || ''
        fd.receiverAddress = c.address || ''
      }
    }
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    totalAmount: totalAmount.value,
    taxAmount: taxAmount.value,
    totalAmountWithTax: totalAmountWithTax.value,
    salesmanId: fd.salespersonId,
    salesmanName: fd.salespersonName,
    shippingAddress: fd.receiverAddress,
    orderRemark: fd.orderRemark,
    buyerRemark: fd.buyerRemark,
    paymentAccountId: fd.paymentAccount,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      barcode: p.barcode,
      specification: p.specification,
      location: p.location,
      unit: p.unit,
      lineAttribute: p.lineAttribute,
      batchCode: p.batchCode,
      productionDate: p.productionDate || null,
      shelfLife: p.shelfLife,
      expiryDate: p.expiryDate || null,
      bigPack: p.bigPack,
      midPack: p.midPack,
      smallPack: p.smallPack,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      amountWithTax: (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100),
      remark: p.remark,
    })),
  }),
})

// 初始化销售订单特有字段
if (!('customerId' in formData)) Object.assign(formData, {
  customerId: undefined, customerName: '',
  warehouseId: undefined, warehouseName: '',
  salespersonId: undefined, salespersonName: '',
  orderDate: '', saleType: 1,
  receiverName: '', receiverPhone: '', receiverAddress: '',
  paymentAccount: undefined,
  logisticsCompany: '', logisticsNo: '', shippingFee: 0,
  memberCardNo: '', memberName: '', memberDiscount: 100,
  orderRemark: '', buyerRemark: '',
})

// ── 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const taxAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value - discountAmount.value + otherFee.value)

/** 表格合计列定义（传递给 BillDetailTable 的 summaryColumns） */
const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value, highlight: true },
  { key: 'midPack', value: totalMidPack.value, highlight: true },
  { key: 'smallPack', value: totalSmallPack.value, highlight: true },
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
    { key: 'config', label: '配置', icon: SettingOutlined }, // 更改按钮名称为"配置"
    { key: 'more', label: '更多', children: [
      { key: 'save-draft', label: '保存草稿' },
      { key: 'copy-order', label: '复制订单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const accountOptions = ref<any[]>([])

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'customerId', label: '客户', type: 'select', required: true, placeholder: '请选择客户', options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '发货仓库', type: 'select', required: true, placeholder: '请选择仓库', options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'salespersonId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, options: [{ label: '正常销售', value: 1 }, { label: '样品销售', value: 2 }, { label: '促销销售', value: 3 }] },
  { key: 'receiverName', label: '收货人', type: 'input', placeholder: '请输入收货人', searchBtn: 'Q' },
  { key: 'receiverPhone', label: '联系电话', type: 'input', placeholder: '请输入联系电话', width: 'narrow' },
  { key: 'receiverAddress', label: '收货地址', type: 'input', placeholder: '请输入收货地址', width: 'wide' },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '收款', fields: [
    { key: 'paymentAccount', label: '订单账户', type: 'select', placeholder: '请选择', options: accountOptions.value.map((a: any) => ({ label: a.name, value: a.id })), suffixBtn: '+Q' },
    { key: '_orderAmount', label: '订单金额', type: 'input', disabled: true, suffixBtn: '全清', suffixBtnDanger: true },
    { key: '_moreAccount', label: '更多账户', type: 'input', disabled: true, suffixBtn: '···' },
    { key: '_useAdvance', label: '使用预订货款', type: 'input', disabled: true, suffixBtn: '···' },
    { key: '_prevAdvance', label: '此前预收', type: 'input', disabled: true },
    { key: '_advanceBalance', label: '预收余额', type: 'input', disabled: true },
  ]},
  { key: 'logistics', tab: '物流信息', fields: [
    { key: 'logisticsCompany', label: '物流公司', type: 'input', placeholder: '请输入物流公司' },
    { key: 'logisticsNo', label: '物流单号', type: 'input', placeholder: '请输入物流单号' },
    { key: 'shippingFee', label: '运费', type: 'number', placeholder: '请输入运费' },
  ]},
  { key: 'member', tab: '会员信息', fields: [
    { key: 'memberCardNo', label: '会员卡号', type: 'input', placeholder: '请输入会员卡号' },
    { key: 'memberName', label: '会员姓名', type: 'input', placeholder: '请输入会员姓名' },
    { key: 'memberDiscount', label: '会员折扣', type: 'number', placeholder: '请输入折扣' },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '销售数量', value: totalQuantity.value, statusLabel: '待结算' },
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
// 明细表格列配置（BillDetailTable 驱动）
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
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'lineAttribute', title: '商品行属性', type: 'input', width: 100 },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 0 },

  // 新增字段：用于列配置演示
  { key: 'image', title: '图片', type: 'input', width: 80 },
  { key: 'preOrderNo', title: '预订货单编号', type: 'input', width: 120 },
  { key: 'smallUnitBarcode', title: '小单位条码', type: 'input', width: 120 },
  { key: 'usePreOrderAmount', title: '使用预订货款', type: 'number', width: 120, precision: 2 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'customField1', title: '单据自定义1(数字字段)', type: 'number', width: 140, precision: 2 },
  { key: 'customField2', title: '单据自定义2(数字字段)', type: 'number', width: 140, precision: 2 },
  { key: 'customField3', title: '单据自定义3(数字字段)', type: 'number', width: 140, precision: 2 },
  { key: 'customField4', title: '单据自定义4(文本字段)', type: 'input', width: 140 },
  { key: 'customField5', title: '单据自定义5(文本字段)', type: 'input', width: 140 },
  { key: 'customField6', title: '单据自定义6(数字字段)', type: 'number', width: 140, precision: 2 },
  { key: 'customField7', title: '单据自定义7(数字字段)', type: 'number', width: 140, precision: 2 },
  { key: 'customField8', title: '单据自定义8(往来单位)', type: 'input', width: 150 },
  { key: 'customField9', title: '单据自定义9(职员)', type: 'input', width: 120 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'customField10', title: '单据自定义10(部门)', type: 'input', width: 120 },
  { key: 'latestSaleDate', title: '最近销售日期', type: 'date', width: 110 },
  { key: 'latestSalePrice', title: '最近售价', type: 'number', width: 100, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 100, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 100, precision: 2 },
  { key: 'lowestPrice', title: '最低售价', type: 'number', width: 100, precision: 2 },
  { key: 'restaurant', title: '餐饮店', type: 'input', width: 80 },
  { key: 'canteen', title: '食堂团餐', type: 'input', width: 100 },
  { key: 'vipSelf', title: '自助vip', type: 'input', width: 100 },
  { key: 'largeGroup', title: '大团餐', type: 'input', width: 80 },
  { key: 'specialCustomer', title: '特价客户', type: 'input', width: 100 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 150, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'unshippedQuantity', title: '未发数量', type: 'number', width: 100, precision: 2 },
  { key: 'shippedQuantityDetail', title: '已发数量', type: 'number', width: 100, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 80, precision: 2 },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 120, precision: 2 },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 120, precision: 2 },
  { key: 'grossProfit', title: '参考毛利', type: 'number', width: 100, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'input', width: 120 },
  { key: 'discountedUnitPrice', title: '折后单价', type: 'number', width: 100, precision: 2 },
  { key: 'originalPrice', title: '折单原价', type: 'number', width: 100, precision: 2 },
  { key: 'vipLevel1', title: '重点|vip01', type: 'input', width: 100 },
  { key: 'vipLevel2', title: '连锁|vip', type: 'input', width: 100 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 100, precision: 2 },
  { key: 'discountPercent', title: '优惠折扣(%)', type: 'number', width: 120, precision: 2 },
  { key: 'favorableUnitPrice', title: '惠后单价', type: 'number', width: 100, precision: 2 },
  { key: 'favorableAmount', title: '优惠后金额', type: 'number', width: 120, precision: 2 },
  { key: 'giftItem', title: '兑换礼品', type: 'input', width: 100 },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 100, precision: 2 },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 100, precision: 2 },
  { key: 'volume', title: '体积（m³）', type: 'number', width: 100, precision: 4 },
  { key: 'weight', title: '重量（kg）', type: 'number', width: 100, precision: 4 },
  { key: 'gift', title: '赠品', type: 'input', width: 80 },
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
    itemCode: '', barcode: '', specification: '', location: '',
    unit: '', lineAttribute: '', batchCode: '',
    productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    productName: '', quantity: 0, remark: '',
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
      record.unitPrice = p.salePrice || p.price || 0
      record.taxRate = 13
    }
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  // 根据 fieldKey 弹出对应的快速选择弹窗
  message.info(`${fieldKey} 快速查询功能开发中`)
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
    case 'config':  // 配置按钮，弹出配置弹窗
      showFormConfig.value = true
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(() => {
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
</style>
