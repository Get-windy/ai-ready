<template>
  <div>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :summary="summaryConfig"
    :footer="footerConfig"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <template #detail-table="{ onExpandChange }">
      <BillDetailTable
        ref="detailTableRef"
        :columns="detailColumns"
        v-model:data-source="formData.products"
        :summary-columns="tableSummaryColumns"
        :storage-key="'purchase-order-form-columns'"
        @cell-change="handleCellChange"
        @expand-change="onExpandChange"
      >
        <template #actionCell="{ index }">
          <a-space :size="2">
            <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
              <PlusCircleOutlined />
            </a-button>
            <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
              <MinusCircleOutlined />
            </a-button>
          </a-space>
        </template>
      </BillDetailTable>
    </template>

    <template #bottom-extra>
      <!-- ═══ 底部收款区域（对标：明细表下方平铺） ═══ -->
      <div class="pay-area">
        <div class="pay-row">
          <span v-if="pageConfig['depositAccount1']?.visible !== false" class="pay-item">
            <label>订金账户1</label>
            <a-select v-model:value="formData.depositAccount1" size="small" show-search allow-clear style="width:130px" placeholder="+Q">
              <a-select-option v-for="a in accountOptions" :key="a.id" :value="a.id">{{ a.name }}</a-select-option>
            </a-select>
          </span>
          <span v-if="pageConfig['depositAmount1']?.visible !== false" class="pay-item">
            <label>订金金额1</label>
            <a-input-number v-model:value="formData.depositAmount1" size="small" :precision="2" style="width:110px" />
          </span>
          <span v-if="pageConfig['moreAccounts']?.visible !== false" class="pay-item">
            <label>更多账户</label>
            <a-input v-model:value="formData.moreAccounts" size="small" style="width:110px" disabled />
          </span>
          <span v-if="pageConfig['prevPrepaid']?.visible !== false" class="pay-item">
            <label>此前预付</label>
            <a-input-number v-model:value="formData.prevPrepaid" size="small" :precision="2" style="width:110px" disabled />
          </span>
          <span v-if="pageConfig['prepaidBalance']?.visible !== false" class="pay-item">
            <label>预付余额</label>
            <a-input-number v-model:value="formData.prepaidBalance" size="small" :precision="2" style="width:110px" disabled />
          </span>
          <span v-if="pageConfig['prevDebt']?.visible !== false" class="pay-item">
            <label>此前欠款</label>
            <a-input-number v-model:value="formData.prevDebt" size="small" :precision="2" style="width:110px" disabled />
          </span>
        </div>
        <div class="pay-row">
          <span v-if="pageConfig['expectedReceiveDate']?.visible !== false" class="pay-item">
            <label>预计到货</label>
            <a-date-picker v-model:value="formData.expectedReceiveDate" size="small" style="width:130px" value-format="YYYY-MM-DD" />
          </span>
          <span v-if="pageConfig['paymentTerm']?.visible !== false" class="pay-item">
            <label>付款期限</label>
            <a-input v-model:value="formData.paymentTerm" size="small" style="width:150px" />
          </span>
          <span v-if="pageConfig['otherExpense']?.visible !== false" class="pay-item">
            <label>其他费用</label>
            <a-input-number v-model:value="formData.otherExpense" size="small" :precision="2" style="width:110px" />
          </span>
        </div>
      </div>
      <!-- ═══ 备注区 ═══ -->
      <div v-if="pageConfig['remark']?.visible !== false" class="remark-row">
        <label class="remark-label">单据备注</label>
        <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入备注" />
      </div>
      <!-- ═══ 单据信息 ═══ -->
      <div class="doc-info-row">
        <span v-if="pageConfig['sourceBillNo']?.visible !== false" class="doc-info-item">源单 <a-tag size="small">{{ formData.sourceBillNo || '0' }}</a-tag></span>
        <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
        <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
        <a-button type="link" size="small" class="print-record-link" @click="printDialogRef?.open()">打印记录</a-button>
      </div>
    </template>
  </BillFormPage>

  <!-- 打印弹窗 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="purchase"
    :print-data="printData"
  />

  <!-- 配置弹窗（齿轮图标触发） -->
  <a-modal
    v-model:open="showFormConfig"
    title="配置"
    :width="760"
    :footer="null"
    destroy-on-close
  >
    <a-tabs v-model:active-key="configModalTab" size="small">
      <!-- Tab 1: 页面配置 -->
      <a-tab-pane key="pageConfig" tab="页面配置">
        <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
        <a-table
          :columns="pageConfigTableColumns"
          :data-source="pageConfigFields"
          :pagination="false"
          size="small"
          row-key="key"
          :scroll="{ y: 400 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'displayName'">
              <a-input v-model:value="record.displayName" size="small" />
            </template>
            <template v-if="column.key === 'visible'">
              <a-checkbox
                :checked="record.visible"
                @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)"
              />
            </template>
            <template v-if="column.key === 'enterJump'">
              <a-checkbox
                :checked="record.enterJump"
                @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)"
              />
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <!-- Tab 2: 录单默认值 -->
      <a-tab-pane key="defaultValues" tab="录单默认值">
        <p class="config-hint">当前操作员新增单据时，对应字段带出默认值，可更改</p>
        <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="供应商">
            <a-select
              v-model:value="formData.defaultSupplierId"
              show-search
              size="small"
              style="width: 100%"
              :loading="loadingOptions"
              @change="saveFormConfig"
            >
              <a-select-option v-for="s in optionRefs.suppliers" :key="s.id" :value="s.id">{{ s.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="仓库">
            <a-select
              v-model:value="formData.defaultWarehouseId"
              show-search
              size="small"
              style="width: 100%"
              :loading="loadingOptions"
              @change="saveFormConfig"
            >
              <a-select-option v-for="w in optionRefs.warehouses" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="经手人">
            <a-select
              v-model:value="formData.defaultBuyerId"
              show-search
              size="small"
              style="width: 100%"
              :loading="loadingOptions"
              @change="saveFormConfig"
            >
              <a-select-option v-for="u in optionRefs.users" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <!-- Tab 3: 打印设置 -->
      <a-tab-pane key="printSettings" tab="打印设置">
        <p class="config-hint">打印配置设置后只针对当前操作员有效</p>
        <a-checkbox v-model:checked="formData.printAlwaysUseLastTemplate" @change="saveFormConfig" style="display:block;margin-bottom:12px">
          始终使用最后一次打印的模板，打印时不再选择
        </a-checkbox>
        <a-checkbox v-model:checked="formData.printImmediatelyAfterSubmit" @change="saveFormConfig">
          提单后立即打印
        </a-checkbox>
      </a-tab-pane>
    </a-tabs>
  </a-modal>

  <!-- 批量导入隐藏文件输入 -->
  <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileChange" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined, ClockCircleOutlined, ImportOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillFooterConfig, SummaryRow } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useUserStore } from '@/stores/user'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { showImportResult } from '@/utils/importResult'
import { PRODUCT_PURCHASE_DEFAULTS } from '@/utils/productDefaults'

defineOptions({ name: 'PurchaseOrderForm' })
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const detailTableRef = ref<InstanceType<typeof BillDetailTable> | null>(null)

// ── useBillForm ──
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
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
  billPrefix: 'CG',
  api: {
    create: (data: any) => request.post('/erp/purchase/order', data),
    update: (id: number, data: any) => request.put(`/erp/purchase/order/${id}`, data),
    getById: (id: number) => request.get(`/erp/purchase/order/${id}`),
  },
  redirectPath: '/purchase/order',
  codeApiPath: '/erp/purchase/order/next-no',
  // 采购单金额：本单金额 = 商品金额 - 优惠金额 + 其他费用（采购单默认价内税，不另计税额）
  // 见下方 billAmount computed
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_PURCHASE_DEFAULTS,
  // 编辑模式：把后端嵌套 DTO（order/快照/结算/扩展信息）映射回表单字段
  onDetailLoaded: (d: any, fd: any) => {
    const order = d?.order
    if (order) {
      if (order.orderDate) fd.date = String(order.orderDate).slice(0, 10)
      if (order.purchaserId !== undefined && order.purchaserId !== null) fd.buyerId = order.purchaserId
      if (order.expectedReceiveTime) fd.expectedReceiveDate = String(order.expectedReceiveTime).slice(0, 10)
      if (order.warehouseId !== undefined) fd.warehouseId = order.warehouseId
      if (order.deptId !== undefined) fd.deptId = order.deptId
      if (order.sourceBillNo !== undefined) fd.sourceBillNo = order.sourceBillNo
      if (order.purchaseType !== undefined) fd.purchaseType = order.purchaseType
      if (order.remark !== undefined) fd.remark = order.remark
      if (order.printCount !== undefined) fd.printCount = order.printCount
      if (order.orderNo !== undefined) fd.orderNo = order.orderNo
    }
    const snap = d?.partnerSnapshot
    if (snap) {
      fd.supplierName = snap.supplierName; fd.supplierCode = snap.supplierCode
      fd.contactName = snap.contactName; fd.contactPhone = snap.contactPhone
      fd.contactAddress = snap.contactAddress
      fd.bankName = snap.bankName; fd.bankAccount = snap.bankAccount; fd.taxNo = snap.taxNo
    }
    const sett = d?.settlement
    if (sett) {
      fd.depositAccount1 = sett.depositAccount1; fd.depositAmount1 = sett.depositAmount1
      fd.prevPrepaid = sett.prevPrepaid; fd.prepaidBalance = sett.prepaidBalance
      fd.prevDebt = sett.prevDebt; fd.paymentTerm = sett.paymentTerm; fd.otherExpense = sett.otherExpense
    }
    const ext = d?.extInfo
    if (ext) {
      fd.summary = ext.summary; fd.extNum1 = ext.extNum1; fd.extNum2 = ext.extNum2
      fd.extText1 = ext.extText1; fd.extText2 = ext.extText2; fd.extText3 = ext.extText3
    }
  },
  onFieldChange: (fieldKey: string, val: any, fd: Record<string, any>) => {
    if (fieldKey === 'supplierId' && val) {
      const s = (optionRefs.suppliers || []).find((x: any) => x.id === val)
      if (s) {
        fd.supplierName = s.name || ''; fd.supplierCode = s.code || ''
        fd.bankName = s.bankName || ''; fd.bankAccount = s.bankAccount || ''
        fd.taxNo = s.taxNo || ''; fd.contactName = s.contactName || ''
        fd.contactPhone = s.contactPhone || ''; fd.contactAddress = s.contactAddress || ''
        fd.prevDebt = s.currentDebt || 0; fd.prevPrepaid = s.prepaidAmount || 0
        fd.prepaidBalance = s.prepaidBalance || 0
      }
    }
  },
  transformPayload: (fd: any, status: number) => ({
    order: {
      id: fd.id || undefined,
      supplierId: fd.supplierId,
      warehouseId: fd.warehouseId,
      purchaserId: fd.buyerId,
      deptId: fd.deptId,
      orderDate: fd.date ? `${fd.date}T00:00:00` : null,
      expectedReceiveTime: fd.expectedReceiveDate ? `${fd.expectedReceiveDate}T00:00:00` : null,
      purchaseType: fd.purchaseType,
      sourceBillNo: fd.sourceBillNo,
      productAmount: totalAmount.value,
      discountAmount: fd.discountAmount || 0,
      billAmount: billAmount.value,
      status,
    },
    partnerSnapshot: {
      supplierName: fd.supplierName,
      supplierCode: fd.supplierCode,
      contactName: fd.contactName,
      contactPhone: fd.contactPhone,
      contactAddress: fd.contactAddress,
      bankName: fd.bankName,
      bankAccount: fd.bankAccount,
      taxNo: fd.taxNo,
    },
    settlement: {
      paymentMethodId: fd.paymentMethodId,
      depositAccount1: fd.depositAccount1,
      depositAmount1: fd.depositAmount1,
      prevPrepaid: fd.prevPrepaid,
      prepaidBalance: fd.prepaidBalance,
      prevDebt: fd.prevDebt,
      paymentTerm: fd.paymentTerm,
      otherExpense: fd.otherExpense,
    },
    extInfo: {
      summary: fd.summary,
      extNum1: fd.extNum1,
      extNum2: fd.extNum2,
      extText1: fd.extText1,
      extText2: fd.extText2,
      extText3: fd.extText3,
    },
    items: fd.products.filter((p: any) => p.productId != null).map((p: any, idx: number) => ({
      lineNo: idx + 1,
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      itemCode: p.itemCode,
      barcode: p.barcode,
      specification: p.specification,
      model: p.model,
      origin: p.origin,
      brand: p.brand,
      unit: p.unit,
      smallUnit: p.smallUnit,
      quantity: p.quantity,
      bigPack: p.bigPack,
      midPack: p.midPack,
      smallPack: p.smallPack,
      smallUnitQuantity: p.smallUnitQuantity,
      unitPrice: p.unitPrice,
      smallUnitPrice: p.smallUnitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      costPrice: p.costPrice,
      costAmount: (p.quantity || 0) * (p.costPrice || 0),
      discountRate: p.discountRate,
      discountedUnitPrice: p.discountedUnitPrice,
      discountedAmount: p.discountedAmount,
      volume: p.volume,
      weight: p.weight,
      gift: p.gift || false,
      remark: p.remark,
      warehouseId: fd.warehouseId,
      // 价格等级字段（标准化产品价格等级）
      restaurant: p.restaurant || false,
      canteen: p.canteen || false,
      outRestaurant: p.outRestaurant || false,
      vipSelf: p.vipSelf || false,
      largeGroup: p.largeGroup || false,
      vipLevel1: p.vipLevel1 || false,
      vipLevel2: p.vipLevel2 || false,
      specialCustomer: p.specialCustomer || false,
      customField1: p.customField1,
      customField2: p.customField2,
      customField3: p.customField3,
      customField4: p.customField4,
      customField5: p.customField5,
      customField6: p.customField6,
      customField7: p.customField7,
      customField8: p.customField8,
      customField9: p.customField9,
      customField10: p.customField10,
    })),
  }),
})

// 初始化采购订单特有字段
if (!('supplierId' in formData)) {
  Object.assign(formData, {
    supplierId: undefined, supplierName: '', supplierCode: '',
    warehouseId: undefined, warehouseName: '',
    buyerId: undefined, buyerName: '', deptId: undefined, deptName: '',
    date: '', purchaseType: 1, sourceBillNo: '',
    expectedReceiveDate: '',
    contactName: '', contactPhone: '', contactAddress: '',
    bankName: '', bankAccount: '', taxNo: '',
    paymentMethodId: undefined, depositAccount1: '', depositAmount1: 0,
    prevPrepaid: 0, prepaidBalance: 0, prevDebt: 0, paymentTerm: '', otherExpense: 0,
    summary: '', extNum1: 0, extNum2: 0, extText1: '', extText2: '', extText3: '',
    remark: '', discountAmount: 0, printCount: 0,
    // 录单默认值Tab
    defaultSupplierId: undefined, defaultWarehouseId: undefined, defaultBuyerId: undefined,
    // 打印设置Tab
    printAlwaysUseLastTemplate: false, printImmediatelyAfterSubmit: false,
  })
}

// ── 页眉配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购订单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-order', label: '打印订单' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ── 页面配置（35 字段，控制全部配置项显隐，持久化） ──
type FieldGroup = 'header' | 'top' | 'payment' | 'remark' | 'docinfo' | 'summary'

interface PageFieldSpec {
  key: string
  label: string
  group: FieldGroup
  /** 字段在页面上的渲染类型（供顶部基本信息区使用） */
  fieldType: 'select' | 'input' | 'display' | 'date' | 'number' | 'textarea'
  defaultVisible: boolean
  defaultEnterJump?: boolean
}

const PAGE_FIELD_SPEC: PageFieldSpec[] = [
  // 1-20 顶部/头部基本信息
  { key: 'orderNo', label: '编号', group: 'header', fieldType: 'input', defaultVisible: true },
  { key: 'supplierId', label: '供应商', group: 'top', fieldType: 'select', defaultVisible: true, defaultEnterJump: true },
  { key: 'supplierCode', label: '供应商编号', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'bankName', label: '开户行', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'bankAccount', label: '银行账号', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'taxNo', label: '税号', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'warehouseId', label: '仓库', group: 'top', fieldType: 'select', defaultVisible: true, defaultEnterJump: true },
  { key: 'buyerId', label: '经手人', group: 'top', fieldType: 'select', defaultVisible: true, defaultEnterJump: true },
  { key: 'deptId', label: '部门', group: 'top', fieldType: 'select', defaultVisible: false },
  { key: 'date', label: '单据日期', group: 'top', fieldType: 'date', defaultVisible: true, defaultEnterJump: true },
  { key: 'contactName', label: '联系人', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'contactPhone', label: '联系电话', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'contactAddress', label: '联系地址', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'extNum1', label: '自定义字段1(数字)', group: 'top', fieldType: 'number', defaultVisible: false },
  { key: 'extNum2', label: '自定义字段2(数字)', group: 'top', fieldType: 'number', defaultVisible: false },
  { key: 'extText1', label: '自定义字段3(文本)', group: 'top', fieldType: 'input', defaultVisible: false },
  { key: 'extText2', label: '自定义字段4(文本)', group: 'top', fieldType: 'input', defaultVisible: false },
  { key: 'extText3', label: '自定义字段5(文本)', group: 'top', fieldType: 'input', defaultVisible: false },
  { key: 'auditorName', label: '审核人', group: 'top', fieldType: 'display', defaultVisible: false },
  { key: 'summary', label: '摘要', group: 'top', fieldType: 'input', defaultVisible: false },
  // 21-29 底部收款区域
  { key: 'depositAccount1', label: '订金账户1', group: 'payment', fieldType: 'select', defaultVisible: true },
  { key: 'depositAmount1', label: '订金金额1', group: 'payment', fieldType: 'number', defaultVisible: true },
  { key: 'moreAccounts', label: '更多账户', group: 'payment', fieldType: 'input', defaultVisible: true },
  { key: 'prevPrepaid', label: '此前预付', group: 'payment', fieldType: 'number', defaultVisible: true },
  { key: 'prepaidBalance', label: '预付余额', group: 'payment', fieldType: 'number', defaultVisible: true },
  { key: 'prevDebt', label: '此前欠款', group: 'payment', fieldType: 'number', defaultVisible: true },
  { key: 'expectedReceiveDate', label: '预计到货', group: 'payment', fieldType: 'date', defaultVisible: true },
  { key: 'paymentTerm', label: '付款期限', group: 'payment', fieldType: 'input', defaultVisible: true },
  { key: 'otherExpense', label: '其他费用', group: 'payment', fieldType: 'number', defaultVisible: true },
  // 30-34 单据信息 + 备注区
  { key: 'sourceBillNo', label: '源单', group: 'docinfo', fieldType: 'input', defaultVisible: true },
  { key: 'remark', label: '单据备注', group: 'remark', fieldType: 'textarea', defaultVisible: true },
  { key: 'createByName', label: '制单人', group: 'docinfo', fieldType: 'display', defaultVisible: true },
  { key: 'createTime', label: '制单时间', group: 'docinfo', fieldType: 'display', defaultVisible: true },
  { key: 'printCount', label: '打印次数', group: 'docinfo', fieldType: 'display', defaultVisible: true },
  // 35 摘要面板
  { key: 'discountAmount', label: '优惠金额', group: 'summary', fieldType: 'number', defaultVisible: true },
]

// 运行时的页面配置显隐状态
const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>({})
for (const f of PAGE_FIELD_SPEC) {
  pageConfig[f.key] = { visible: f.defaultVisible, enterJump: f.defaultEnterJump || false }
}

const FORM_CONFIG_MODULE = 'purchase-form'
const FORM_CONFIG_PAGE = 'order'

async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = !!parsed.fields[k].enterJump
        }
      })
    }
    if (parsed.defaults) {
      Object.assign(formData, {
        defaultSupplierId: parsed.defaults.defaultSupplierId,
        defaultWarehouseId: parsed.defaults.defaultWarehouseId,
        defaultBuyerId: parsed.defaults.defaultBuyerId,
      })
    }
    if (parsed.print) {
      Object.assign(formData, {
        printAlwaysUseLastTemplate: parsed.print.printAlwaysUseLastTemplate,
        printImmediatelyAfterSubmit: parsed.print.printImmediatelyAfterSubmit,
      })
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultSupplierId: formData.defaultSupplierId,
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultBuyerId: formData.defaultBuyerId,
    },
    print: {
      printAlwaysUseLastTemplate: formData.printAlwaysUseLastTemplate,
      printImmediatelyAfterSubmit: formData.printImmediatelyAfterSubmit,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}

function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ── 顶部基本信息字段（Zone 2，只含 group==='top' 的字段） ──
const ALL_TOP_FIELDS: BasicInfoField[] = PAGE_FIELD_SPEC
  .filter((f) => f.group === 'top')
  .map((f) => ({
    key: f.key,
    label: f.label,
    type: f.fieldType,
    required: ['supplierId', 'warehouseId', 'buyerId', 'date'].includes(f.key),
    width: f.fieldType === 'select' && f.key === 'supplierId' ? 300 : 160,
    searchBtn: f.fieldType === 'select' ? '+Q' : undefined,
  }))

// 顶部基本信息渲染（过滤被配置隐藏的字段）
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_TOP_FIELDS
    .filter((f) => pageConfig[f.key]?.visible !== false)
    .map((f) => ({
      ...f,
      options: f.key === 'supplierId'
        ? (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id }))
        : f.key === 'warehouseId'
          ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id }))
          : f.key === 'buyerId'
            ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
            : f.key === 'deptId'
              ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
              : f.options,
      loading: (f.key === 'supplierId' || f.key === 'warehouseId' || f.key === 'buyerId' || f.key === 'deptId') ? loadingOptions.value : undefined,
    }))
)

// 页面配置字段列表（配置弹窗表格）
const pageConfigFields = computed(() =>
  PAGE_FIELD_SPEC.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
    visible: pageConfig[f.key].visible,
    enterJump: pageConfig[f.key].enterJump,
  }))
)

// 配置弹窗表格列
const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 130 },
  { title: '显示名', key: 'displayName', width: 150 },
  { title: '显示', key: 'visible', width: 60, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 90, align: 'center' as const },
]

// ── 部门/账户下拉选项 ──
const departmentOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
  try { accountOptions.value = await optionsApi.getAccounts() } catch { accountOptions.value = [] }
}

// 本单金额 = 商品金额 - 优惠金额 + 其他费用
const billAmount = computed(() => totalAmount.value - (formData.discountAmount || 0) + (formData.otherExpense || 0))

// ── 摘要 ──
const summaryConfig = computed<SummaryRow[]>(() => {
  const rows: SummaryRow[] = []
  if (pageConfig['discountAmount']?.visible !== false) {
    rows.push({ label: '商品金额', value: totalAmount.value.toFixed(2) })
    rows.push({ label: '优惠金额', value: (formData.discountAmount || 0).toFixed(2) })
  } else {
    rows.push({ label: '商品金额', value: totalAmount.value.toFixed(2) })
  }
  return rows
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

// ── 明细表格列（57列 + 行号/操作列） ──
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productId', title: '商品名称', type: 'select', width: 200, options: optionRefs.products.map((p: any) => ({ value: p.id, label: p.name })) },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'input', width: 120, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'baseQuantity', title: '件散数量', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },
  { key: 'latestPurchaseDate', title: '最近采购日期', type: 'input', width: 120, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'wholesalePrice', title: '批发价', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 110, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100 },
  { key: 'unshippedQuantity', title: '未收数量', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'receivedQuantityDetail', title: '已收数量', type: 'input', width: 90, align: 'right', readonly: true },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'discountRate', title: '优惠折扣(%)', type: 'number', width: 100, precision: 0 },
  { key: 'discountedUnitPrice', title: '惠后单价', type: 'number', width: 100, precision: 2 },
  { key: 'discountedAmount', title: '优惠后金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4 },
  { key: 'gift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  // 价格等级字段（用户自定义昵称，按标准化产品价格等级处理）
  { key: 'restaurant', title: '餐饮店', type: 'boolean', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'boolean', width: 90, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'boolean', width: 110, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'boolean', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'boolean', width: 70, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'boolean', width: 100, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'boolean', width: 90, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'boolean', width: 90, defaultHidden: true },
  // 10个自定义字段
  { key: 'customField1', title: '单据自定义1(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'customField2', title: '单据自定义2(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'customField3', title: '单据自定义3(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'customField4', title: '单据自定义4(文本)', type: 'input', width: 120 },
  { key: 'customField5', title: '单据自定义5(文本)', type: 'input', width: 120 },
  { key: 'customField6', title: '单据自定义6(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'customField7', title: '单据自定义7(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'customField8', title: '单据自定义8(往来单位)', type: 'input', width: 130 },
  { key: 'customField9', title: '单据自定义9(职员)', type: 'input', width: 120 },
  { key: 'customField10', title: '单据自定义10(部门)', type: 'input', width: 120 },
])

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
])

// ── 处理函数 ──
function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (!record) return
  if (fieldKey === 'productId') {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productName = p.name || ''
      record.productCode = p.code || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.unit = p.unit || ''
      record.smallUnit = p.smallUnit || ''
      record.brand = p.brand || ''
      record.origin = p.origin || ''
      record.model = p.model || ''
      record.unitPrice = p.purchasePrice || p.price || 0
      record.retailPrice = p.retailPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.costPrice = p.costPrice || 0
    }
  }
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
  }
  if (fieldKey === 'discountRate' && record.unitPrice) {
    const rate = record.discountRate || 0
    record.discountedUnitPrice = record.unitPrice * (1 - rate / 100)
    record.discountedAmount = (record.quantity || 0) * (record.discountedUnitPrice || 0)
  }
  if (fieldKey === 'costPrice') {
    record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
  }
}

// ── 打印/配置/导入 ──
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const fileInputRef = ref<HTMLInputElement | null>(null)
const importLoading = ref(false)

const printData = computed(() => ({
  ...formData,
  orderNo: formData.orderNo || '待生成',
  creatorName: currentUserName.value || '系统',
}))

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history': router.push('/purchase/order'); break
    case 'print-order':
      printDialogRef.value?.open()
      break
    case 'import':
      fileInputRef.value?.click()
      break
    case 'config':
      showFormConfig.value = true
      break
    default: break
  }
}

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  importLoading.value = true
  try {
    const res: any = await request.post('/erp/purchase/order/import', fd)
    message.success(`导入成功: ${res?.data?.count || 0} 条`)
  } catch {
    message.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
    input.value = ''
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 从缺货补货页跳转预填明细商品 ──
async function prefillProducts(ids: number[]) {
  if (!ids.length) return
  if (!optionRefs.products || !optionRefs.products.length) {
    try { optionRefs.products = await optionsApi.getProducts() } catch { optionRefs.products = [] }
  }
  const list = optionRefs.products || []
  for (const pid of ids) {
    if (formData.products.some((r: any) => r.productId === pid)) continue
    handleAddProduct()
    const row = formData.products[formData.products.length - 1]
    const p = list.find((x: any) => Number(x?.id) === Number(pid))
    row.productId = pid
    if (p) {
      row.itemCode = p.code || ''
      row.barcode = p.barcode || ''
      row.productName = p.name || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.smallUnit = p.smallUnit || ''
      row.brand = p.brand || ''
      row.origin = p.origin || ''
      row.model = p.model || ''
      row.unitPrice = p.purchasePrice || p.price || 0
      row.retailPrice = p.retailPrice || 0
      row.wholesalePrice = p.wholesalePrice || 0
      row.costPrice = p.costPrice || 0
    }
  }
}

// 新增单据时应用录单默认值
function applyDefaultValues() {
  if (effectiveMode.value === 'edit') return
  if (formData.defaultSupplierId !== undefined && formData.defaultSupplierId !== null && !formData.supplierId) {
    formData.supplierId = formData.defaultSupplierId
  }
  if (formData.defaultWarehouseId !== undefined && formData.defaultWarehouseId !== null && !formData.warehouseId) {
    formData.warehouseId = formData.defaultWarehouseId
  }
  if (formData.defaultBuyerId !== undefined && formData.defaultBuyerId !== null && !formData.buyerId) {
    formData.buyerId = formData.defaultBuyerId
  }
}

onMounted(async () => {
  await loadFormConfig()
  await loadExtraOptions()
  const query = route.query
  // 主数据 id 全站以字符串传递（后端 Long 序列化为 string），下拉选项 value 也是字符串。
  // 这里做 Number() 会让 select 匹配不到 label、只显示 ID（供应商「订货」下推曾出现该问题）。
  if (query.supplierId) {
    formData.supplierId = String(query.supplierId)
    formData.supplierName = query.supplierName || ''
    if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
  }
  if (query.warehouseId) {
    formData.warehouseId = String(query.warehouseId)
    formData.warehouseName = query.warehouseName || ''
  }
  if (query.productIds) {
    const ids = String(query.productIds).split(',').map(Number).filter(Boolean)
    await prefillProducts(ids)
  }
  applyDefaultValues()
  if (formData.products.length === 0) handleAddProduct()
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.print-record-link { color: #1890ff; }
.pay-area { padding: 4px 0; }
.pay-row { display: flex; align-items: center; gap: 12px; padding: 3px 0; flex-wrap: wrap; }
.pay-item { display: flex; align-items: center; gap: 4px; }
.pay-item label { font-size: 12px; color: #595959; white-space: nowrap; }
.remark-row { display: flex; align-items: center; gap: 8px; padding: 3px 0; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 80px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #ff4d4f; margin-bottom: 8px; }
</style>
