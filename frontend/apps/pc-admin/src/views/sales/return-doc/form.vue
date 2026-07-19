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
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ 商品明细表格 ═══ -->
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

      <!-- ═══ 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input v-model:value="formData.remark" size="small" class="remark-input" />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
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

    <!-- ═══ 配置弹窗（3个Tab） ═══ -->
    <a-modal v-model:open="showFormConfig" title="配置" width="600px" :footer="null">
      <a-tabs v-model:activeKey="configActiveTab">
        <a-tab-pane key="page" tab="页面配置">
          <div class="config-tip">勾选后自动保存，该设置对所有操作员生效</div>
          <div class="config-table">
            <table class="config-tbl">
              <thead><tr><th></th><th>名称</th><th>显示名</th><th>显示</th><th>回车键跳转</th></tr></thead>
              <tbody>
                <tr v-for="(f, i) in formFieldConfig" :key="f.key">
                  <td>{{ i + 1 }}</td>
                  <td>{{ f.label }}</td>
                  <td><a-input v-model:value="f.displayName" size="small" /></td>
                  <td><a-checkbox v-model:checked="f.visible" /></td>
                  <td><a-checkbox v-model:checked="f.enterJump" /></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="config-actions">
            <a-button @click="resetFormConfig">恢复默认值</a-button>
            <a-button type="primary" @click="showFormConfig = false">关闭</a-button>
          </div>
        </a-tab-pane>
        <a-tab-pane key="defaults" tab="录单设置">
          <div class="defaults-section">
            <div class="defaults-field">
              <span class="defaults-label">客户</span>
              <a-select v-model:value="defaultValues.customerId" size="small" style="width: 100%" allow-clear placeholder="请选择">
                <a-select-option v-for="c in optionRefs.customers" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
              </a-select>
            </div>
            <div class="defaults-field">
              <span class="defaults-label">入库仓库</span>
              <a-select v-model:value="defaultValues.warehouseId" size="small" style="width: 100%" allow-clear placeholder="请选择">
                <a-select-option v-for="w in optionRefs.warehouses" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
              </a-select>
            </div>
            <div class="defaults-field">
              <span class="defaults-label">经手人</span>
              <a-select v-model:value="defaultValues.handlerId" size="small" style="width: 100%" allow-clear placeholder="请选择">
                <a-select-option v-for="u in optionRefs.users" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
              </a-select>
            </div>
            <div class="defaults-field">
              <span class="defaults-label">付款账户</span>
              <a-input v-model:value="defaultValues.paymentAccount1" size="small" placeholder="请输入" />
            </div>
            <div class="defaults-field">
              <span class="defaults-label">付款账户2</span>
              <a-input v-model:value="defaultValues.paymentAccount2" size="small" placeholder="请输入" />
            </div>
            <div class="defaults-field">
              <a-checkbox v-model:checked="defaultValues.priority">录单默认值优先</a-checkbox>
            </div>
          </div>
          <div class="config-actions">
            <a-button type="primary" @click="saveDefaultValues">保存</a-button>
            <a-button @click="showFormConfig = false">取消</a-button>
          </div>
        </a-tab-pane>
        <a-tab-pane key="print" tab="打印设置">
          <div class="print-section">
            <div class="defaults-field">
              <span class="defaults-label">打印模板</span>
              <a-select v-model:value="printSettings.template" size="small" style="width: 100%" allow-clear>
                <a-select-option value="default">标准模板</a-select-option>
                <a-select-option value="compact">紧凑模板</a-select-option>
              </a-select>
            </div>
            <div class="defaults-field">
              <span class="defaults-label">打印份数</span>
              <a-input-number v-model:value="printSettings.copies" size="small" :min="1" :max="10" style="width: 100%" />
            </div>
          </div>
          <div class="config-actions">
            <a-button type="primary" @click="savePrintSettings">保存</a-button>
            <a-button @click="showFormConfig = false">取消</a-button>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick, reactive } from 'vue'
defineOptions({ name: 'SaleReturnDocForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined, ClockCircleOutlined, ImportOutlined, ExportOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleReturnDocApi } from '@/api/erp'
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
const configActiveTab = ref('page')

const tableMaxHeight = ref(400)

// ═══ 录单默认值 ═══
const defaultValues = reactive({
  customerId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  handlerId: undefined as number | undefined,
  paymentAccount1: '',
  paymentAccount2: '',
  priority: false,
})

// ═══ 打印设置 ═══
const printSettings = reactive({
  template: 'default',
  copies: 1,
})

// ═══ 页面配置字段（53个） ═══
const formFieldConfig = ref([
  { key: 'returnDocNo', label: '编号', displayName: '编号', visible: true, enterJump: false },
  { key: 'customerId', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户编号', displayName: '客户编号', visible: false, enterJump: false },
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  { key: 'warehouseId', label: '入库仓库', displayName: '入库仓库', visible: true, enterJump: true },
  { key: 'handlerId', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'deptId', label: '部门', displayName: '部门', visible: false, enterJump: false },
  { key: 'orderDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  { key: 'salesType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  { key: 'contactName', label: '联系人', displayName: '联系人', visible: true, enterJump: false },
  { key: 'contactPhone', label: '联系电话', displayName: '联系电话', visible: true, enterJump: false },
  { key: 'shippingAddress', label: '收货地址', displayName: '收货地址', visible: true, enterJump: false },
  { key: 'extNum1', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: false, enterJump: false },
  { key: 'extNum2', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: false, enterJump: false },
  { key: 'extText1', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: false, enterJump: false },
  { key: 'extText2', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: false, enterJump: false },
  { key: 'extText3', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: false, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: true, enterJump: false },
  { key: 'paymentAccount1', label: '付款账户', displayName: '付款账户', visible: true, enterJump: false },
  { key: 'settledAmount', label: '付款金额', displayName: '付款金额', visible: true, enterJump: false },
  { key: 'paymentAccount2', label: '更多账户', displayName: '更多账户', visible: true, enterJump: false },
  { key: 'prevAdvance', label: '此前预收', displayName: '此前预收', visible: true, enterJump: false },
  { key: 'returnAdvance', label: '退回预收款', displayName: '退回预收款', visible: true, enterJump: false },
  { key: 'availableAdvance', label: '可用预收', displayName: '可用预收', visible: true, enterJump: false },
  { key: 'advanceBalance', label: '预收余额', displayName: '预收余额', visible: true, enterJump: false },
  { key: 'receivableReduce', label: '应收款减少', displayName: '应收款减少', visible: true, enterJump: false },
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: true, enterJump: false },
  { key: 'availableCredit', label: '可用额度', displayName: '可用额度', visible: true, enterJump: false },
  { key: 'prevDebt', label: '此前欠款', displayName: '此前欠款', visible: true, enterJump: false },
  { key: 'currentDebt', label: '本次欠款', displayName: '本次欠款', visible: true, enterJump: false },
  { key: 'debtBalance', label: '欠款余额', displayName: '欠款余额', visible: true, enterJump: false },
  { key: 'collectionDeadline', label: '收款期限', displayName: '收款期限', visible: true, enterJump: false },
  { key: 'sourceOrder', label: '源单', displayName: '源单', visible: true, enterJump: false },
  { key: 'deliveryMethod', label: '配送方式', displayName: '配送方式', visible: true, enterJump: false },
  { key: 'deliveryRoute', label: '配送线路', displayName: '配送线路', visible: true, enterJump: false },
  { key: 'logisticsCompany', label: '物流公司', displayName: '物流公司', visible: true, enterJump: false },
  { key: 'freightPayer', label: '运费承担方', displayName: '运费承担方', visible: true, enterJump: false },
  { key: 'shippingFee', label: '运费', displayName: '运费', visible: true, enterJump: false },
  { key: 'waybillNo', label: '运单号', displayName: '运单号', visible: true, enterJump: false },
  { key: 'deliveryOrderNo', label: '配送单', displayName: '配送单', visible: true, enterJump: false },
  { key: 'memberCardNo', label: '会员卡号', displayName: '会员卡号', visible: true, enterJump: false },
  { key: 'prevPoints', label: '此前积分', displayName: '此前积分', visible: true, enterJump: false },
  { key: 'memberGeneratedPoints', label: '产生积分', displayName: '产生积分', visible: true, enterJump: false },
  { key: 'memberExchangePoints', label: '兑换积分', displayName: '兑换积分', visible: true, enterJump: false },
  { key: 'memberUsedPoints', label: '使用积分', displayName: '使用积分', visible: true, enterJump: false },
  { key: 'currentPoints', label: '剩余积分', displayName: '剩余积分', visible: true, enterJump: false },
  { key: 'remark', label: '单据备注', displayName: '单据备注', visible: true, enterJump: false },
  { key: 'creatorName', label: '制单人', displayName: '制单人', visible: true, enterJump: false },
  { key: 'createTime', label: '制单时间', displayName: '制单时间', visible: true, enterJump: false },
  { key: 'printCount', label: '打印次数', displayName: '打印次数', visible: true, enterJump: false },
  { key: 'billAmount', label: '本单金额', displayName: '本单金额', visible: true, enterJump: false },
])

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
  billPrefix: 'XSTHD',
  api: {
    create: saleReturnDocApi.create,
    update: saleReturnDocApi.update,
    getById: saleReturnDocApi.getById,
  },
  redirectPath: '/sales/return-doc',
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
        fd.customerLevel = c.level || ''
        fd.contactName = c.contactName || ''
        fd.contactPhone = c.contactPhone || ''
        fd.shippingAddress = c.address || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        fd.customerTicket = c.ticket || ''
        fd.customerRemark = c.remark || ''
        fd.prevDebt = c.prevDebt || 0
        fd.creditLimit = c.creditLimit || 0
        fd.prevAdvance = c.prevAdvance || 0
      }
    }
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.warehouseName = w.name
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      if (u) fd.handlerName = u.name
    }
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    totalAmount: totalAmount.value,
    billAmount: totalAmount.value,
    items: fd.products
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
        extNum6: p.extNum6 || 0,
        extNum7: p.extNum7 || 0,
        extText1: p.extText1 || '',
        extText2: p.extText2 || '',
        extPartner: p.extPartner || null,
        extStaff: p.extStaff || null,
        extDept: p.extDept || null,
        remark: p.remark || '',
        itemRemark: p.itemRemark || '',
        lineNo: idx + 1,
      })),
  }),
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
    logisticsCompany: '', freightPayer: '', shippingFee: 0, waybillNo: '', deliveryOrderNo: '',
    memberCardNo: '', memberName: '', memberDiscount: 0,
    prevPoints: 0, memberGeneratedPoints: 0, memberExchangePoints: 0,
    memberUsedPoints: 0, currentPoints: 0,
    remark: '', printCount: 0,
    returnType: 0, reason: '',
    returnApplyId: undefined, returnApplyNo: '',
    receiverName: '', receiverPhone: '',
  })
}

// ─ 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const totalPieceQty = computed(() => formData.products.reduce((s: number, p: any) => s + (p.pieceQuantity || 0), 0))
const totalDiscountAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0))

/** 表格合计列定义 */
const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'pieceQuantity', value: totalPieceQty.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value, highlight: true },
  { key: 'midPack', value: totalMidPack.value, highlight: true },
  { key: 'smallPack', value: totalSmallPack.value, highlight: true },
  { key: 'amount', value: totalAmount.value, highlight: true },
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

const basicInfoFields = computed<BasicInfoField[]>(() => {
  const fields: BasicInfoField[] = [
    { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435,
      options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })),
      searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, inlineLabel: true, width: 210,
      options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })),
      searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210,
      options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })),
      searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
    { key: 'salesType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 210,
      options: [{ label: '正常销售', value: 'normal' }, { label: '退货', value: 'return' }] },
  ]

  // 根据页面配置过滤
  const visibleKeys = new Set(formFieldConfig.value.filter(f => f.visible).map(f => f.key))
  // 核心字段始终显示
  const coreFields = ['customerId', 'warehouseId', 'handlerId', 'orderDate', 'salesType']
  coreFields.forEach(k => visibleKeys.add(k))

  const extraFields: BasicInfoField[] = [
    { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, searchBtn: 'Q' },
    { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 210 },
    { key: 'shippingAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 435 },
    { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 435 },
    { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', inlineLabel: true, width: 210 },
    { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', inlineLabel: true, width: 210 },
    { key: 'extText1', label: '自定义字段3(文本)', type: 'input', inlineLabel: true, width: 210 },
    { key: 'extText2', label: '自定义字段4(文本)', type: 'input', inlineLabel: true, width: 210 },
    { key: 'extText3', label: '自定义字段5(文本)', type: 'input', inlineLabel: true, width: 210 },
  ]

  extraFields.forEach(f => {
    if (visibleKeys.has(f.key)) fields.push(f)
  })

  return fields
})

const tabsConfig = computed<BillTabConfig[]>(() => {
  const allTabs: BillTabConfig[] = [
    // Tab 1: 付款
    { key: 'payment', tab: '付款', fields: [
      { key: 'paymentAccount1', label: '付款账户', type: 'select', inlineLabel: true, width: 210,
        options: [{ label: '现金', value: 'cash' }, { label: '银行转账', value: 'bank' }, { label: '支付宝', value: 'alipay' }, { label: '微信', value: 'wechat' }],
        searchBtn: '+Q' },
      { key: 'settledAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 210 },
      { key: 'paymentAccount2', label: '更多账户', type: 'input', inlineLabel: true, width: 210 },
      { key: 'prevAdvance', label: '此前预收', type: 'number', inlineLabel: true, width: 210 },
      { key: 'returnAdvance', label: '退回预收款', type: 'number', inlineLabel: true, width: 210 },
      { key: 'availableAdvance', label: '可用预收', type: 'number', inlineLabel: true, width: 210 },
      { key: 'advanceBalance', label: '预收余额', type: 'number', inlineLabel: true, width: 210 },
      { key: 'receivableReduce', label: '应收款减少', type: 'number', inlineLabel: true, width: 210 },
      { key: 'creditLimit', label: '信用额度', type: 'number', inlineLabel: true, width: 210 },
      { key: 'availableCredit', label: '可用额度', type: 'number', inlineLabel: true, width: 210 },
      { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210 },
      { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210 },
      { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210 },
      { key: 'collectionDeadline', label: '收款期限', type: 'date', inlineLabel: true, width: 210 },
    ]},
    // Tab 2: 物流信息
    { key: 'logistics', tab: '物流信息', fields: [
      { key: 'deliveryMethod', label: '配送方式', type: 'select', inlineLabel: true, width: 210,
        options: [{ label: '自提', value: 'pickup' }, { label: '配送', value: 'delivery' }, { label: '物流', value: 'logistics' }] },
      { key: 'deliveryRoute', label: '配送线路', type: 'select', inlineLabel: true, width: 210, options: [] },
      { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
      { key: 'freightPayer', label: '运费承担方', type: 'select', inlineLabel: true, width: 210,
        options: [{ label: '我方', value: 'seller' }, { label: '客户', value: 'buyer' }] },
      { key: 'shippingFee', label: '运费', type: 'number', inlineLabel: true, width: 210 },
      { key: 'waybillNo', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
      { key: 'deliveryOrderNo', label: '配送单', type: 'input', inlineLabel: true, width: 210 },
    ]},
    // Tab 3: 会员信息
    { key: 'member', tab: '会员信息', fields: [
      { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 210 },
      { key: 'prevPoints', label: '此前积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'memberGeneratedPoints', label: '产生积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'memberExchangePoints', label: '兑换积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'memberUsedPoints', label: '使用积分', type: 'number', inlineLabel: true, width: 210 },
      { key: 'currentPoints', label: '剩余积分', type: 'number', inlineLabel: true, width: 210 },
    ]},
  ]

  // 根据页面配置过滤每个tab的可见字段
  const visibleKeys = new Set(formFieldConfig.value.filter(f => f.visible).map(f => f.key))
  return allTabs.map(tab => ({
    ...tab,
    fields: tab.fields.filter((f: any) => visibleKeys.has(f.key)),
  })).filter(tab => tab.fields.length > 0)
})

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '退货数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '折后金额', value: totalDiscountAmount.value.toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `\u00a5${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置（67列）
// ═══════════════════════════════════════

const detailColumns = computed(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' as const },
  { key: 'imageUrl', title: '图片', type: 'image', width: 50 },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true, width: 200, showScanToggle: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name, value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
  },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'storageLocation', title: '货位', type: 'input', width: 80 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'modelNo', title: '型号', type: 'input', width: 80 },
  { key: 'originPlace', title: '产地', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 80, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 120, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 80, precision: 2 },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 100 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 100 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 100 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 80, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 70, precision: 0 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 100 },
  { key: 'lastSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 80, precision: 2, align: 'right' as const },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 80, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 80, precision: 2 },
  { key: 'refCostPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'refCostAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 70, precision: 2 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 80, precision: 2 },
  { key: 'exchangeGift', title: '兑换礼品', type: 'input', width: 80 },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 80, precision: 0 },
  { key: 'generatedPoints', title: '产生积分', type: 'number', width: 80, precision: 0 },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 80, precision: 0 },
  { key: 'productLineAttr', title: '商品行属性', type: 'input', width: 100 },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 80, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 80, precision: 2 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 50 },
  { key: 'remark', title: '备注', type: 'input', width: 120 },
  // 价格等级8个（标准化产品价格等级）
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 80, precision: 2 },
  // 单据自定义字段
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 120 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 120 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'number', width: 140 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'number', width: 120 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'number', width: 120 },
] as DetailColumnConfig[])

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined, itemCode: '', barcode: '', specification: '',
    unit: '', batchCode: '', conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    exchangeGift: '', exchangePoints: 0,
    productName: '', quantity: 0, remark: '',
    imageUrl: '', storageLocation: '', area: '', modelNo: '', originPlace: '', brand: '',
    availableStock: 0, availableStockConverted: 0, bookStock: 0,
    batchBarcode: '', productionDate: null, shelfLife: '', expiryDate: null,
    lastSaleDate: null, lastSalePrice: 0, retailPrice: 0, wholesalePrice: 0, minSalePrice: 0,
    smallUnit: '', smallUnitPrice: 0, smallUnitQuantity: 0, conversionResult: 0,
    refCostPrice: 0, refCostAmount: 0,
    discountRate: 0, discountedPrice: 0, discountedAmount: 0,
    generatedPoints: 0, usedPoints: 0,
    productLineAttr: '', volume: 0, weight: 0, isGift: false,
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0, extNum6: 0, extNum7: 0,
    extText1: '', extText2: '',
    extPartner: null, extStaff: null, extDept: null,
    itemRemark: '',
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
      record.productSpec = p.specification || ''
      record.unit = p.unit || ''
      record.productUnit = p.unit || ''
      record.unitPrice = p.salePrice || p.retailPrice || p.price || 0
      record.retailPrice = p.retailPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.minSalePrice = p.minSalePrice || 0
      record.lastSalePrice = p.lastSalePrice || 0
      record.lastSaleDate = p.lastSaleDate || null
      record.refCostPrice = p.costPrice || 0
      record.brand = p.brand || ''
      record.originPlace = p.originPlace || ''
      record.modelNo = p.modelNo || ''
      record.area = p.area || ''
      record.availableStock = p.availableStock || 0
      record.bookStock = p.bookStock || 0
      record.taxRate = 13
      // 价格等级映射
      if (p.priceLevel1) record.priceLevel1 = p.priceLevel1
      if (p.priceLevel2) record.priceLevel2 = p.priceLevel2
      if (p.priceLevel3) record.priceLevel3 = p.priceLevel3
      if (p.priceLevel4) record.priceLevel4 = p.priceLevel4
      if (p.priceLevel5) record.priceLevel5 = p.priceLevel5
      if (p.priceLevel6) record.priceLevel6 = p.priceLevel6
      if (p.priceLevel7) record.priceLevel7 = p.priceLevel7
      if (p.priceLevel8) record.priceLevel8 = p.priceLevel8
    }
  }
  // 金额自动计算
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    const qty = parseFloat(record.quantity) || 0
    const price = parseFloat(record.unitPrice) || 0
    record.amount = qty * price
  }
  if (fieldKey === 'quantity' || fieldKey === 'discountRate') {
    const qty = parseFloat(record.quantity) || 0
    const price = parseFloat(record.unitPrice) || 0
    const rate = parseFloat(record.discountRate) || 0
    record.discountedPrice = price * (1 - rate / 100)
    record.discountedAmount = qty * record.discountedPrice
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能开发中`)
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
      row.refCostPrice = p.costPrice || 0
      row.brand = p.brand || ''
      row.originPlace = p.originPlace || ''
      row.modelNo = p.modelNo || ''
      row.area = p.area || ''
      row.availableStock = p.availableStock || 0
      row.bookStock = p.bookStock || 0
      row.taxRate = 13
      if (p.priceLevel1) row.priceLevel1 = p.priceLevel1
      if (p.priceLevel2) row.priceLevel2 = p.priceLevel2
      if (p.priceLevel3) row.priceLevel3 = p.priceLevel3
      if (p.priceLevel4) row.priceLevel4 = p.priceLevel4
      if (p.priceLevel5) row.priceLevel5 = p.priceLevel5
      if (p.priceLevel6) row.priceLevel6 = p.priceLevel6
      if (p.priceLevel7) row.priceLevel7 = p.priceLevel7
      if (p.priceLevel8) row.priceLevel8 = p.priceLevel8
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
      message.info('打印功能开发中')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'import':
    case 'export':
      message.info(`${actionKey === 'import' ? '导入' : '导出'} 功能开发中`)
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function resetFormConfig() {
  message.info('已恢复默认配置')
}

function saveDefaultValues() {
  message.success('录单默认值已保存')
  showFormConfig.value = false
}

function savePrintSettings() {
  message.success('打印设置已保存')
  showFormConfig.value = false
}

// ── 生命周期 ──
onMounted(() => {
  if (formData.products.length === 0) {
    for (let i = 0; i < 15; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
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

.config-tip { font-size: 12px; color: #fa8c16; margin-bottom: 12px; }
.config-tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.config-tbl th, .config-tbl td { border: 1px solid #f0f0f0; padding: 6px 8px; text-align: left; }
.config-tbl th { background: #fafafa; font-weight: 600; }
.config-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }

.defaults-section { display: flex; flex-direction: column; gap: 12px; }
.defaults-field { display: flex; align-items: center; gap: 8px; }
.defaults-label { font-size: 13px; color: #333; min-width: 80px; }

.print-section { display: flex; flex-direction: column; gap: 12px; }
</style>
