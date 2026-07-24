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
        :data-source="formData.products"
        :max-height="tableMaxHeight"
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
        <template #productCell="{ record, index }">
          <a-select
            v-model:value="record.productId"
            placeholder="搜索选择产品"
            show-search
            :filter-option="filterOption"
            style="width:100%"
            :loading="loadingOptions"
            size="small"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">
              {{ p.name }}
            </a-select-option>
          </a-select>
        </template>
      </BillDetailTable>
    </template>

    <template #bottom-extra>
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
        <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
        <a-button type="link" size="small" class="print-record-link">打印记录</a-button>
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
    :width="720"
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
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'displayName'">
              <a-input v-model:value="record.displayName" size="small" />
            </template>
            <template v-if="column.key === 'visible'">
              <a-checkbox v-model:checked="record.visible" />
            </template>
            <template v-if="column.key === 'enterJump'">
              <a-checkbox v-model:checked="record.enterJump" />
            </template>
          </template>
        </a-table>
      </a-tab-pane>
      <!-- Tab 2: 录单默认值 -->
      <a-tab-pane key="defaultValues" tab="录单默认值">
        <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="默认仓库">
            <a-select
              v-model:value="formData.defaultWarehouseId"
              show-search
              size="small"
              style="width: 100%"
              :loading="loadingOptions"
            >
              <a-select-option v-for="w in optionRefs.warehouses" :key="w.id" :value="w.id">
                {{ w.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="默认经手人">
            <a-select
              v-model:value="formData.defaultBuyerId"
              show-search
              size="small"
              style="width: 100%"
              :loading="loadingOptions"
            >
              <a-select-option v-for="u in optionRefs.users" :key="u.id" :value="u.id">
                {{ u.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="默认付款方式">
            <a-input v-model:value="formData.defaultPaymentMethod" size="small" />
          </a-form-item>
          <a-form-item label="默认税率">
            <a-input-number v-model:value="formData.defaultTaxRate" :precision="0" size="small" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-tab-pane>
      <!-- Tab 3: 打印设置 -->
      <a-tab-pane key="printSettings" tab="打印设置">
        <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="打印模板">
            <a-select v-model:value="formData.printTemplate" size="small" style="width: 100%">
              <a-select-option value="standard">标准模板</a-select-option>
              <a-select-option value="simple">简化模板</a-select-option>
              <a-select-option value="detailed">详细模板</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="打印份数">
            <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width: 100%" />
          </a-form-item>
          <a-form-item label="纸张大小">
            <a-select v-model:value="formData.printPaperSize" size="small" style="width: 100%">
              <a-select-option value="A4">A4</a-select-option>
              <a-select-option value="A5">A5</a-select-option>
              <a-select-option value="B5">B5</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-modal>

  <!-- 批量导入隐藏文件输入 -->
  <input ref="fileInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleFileChange" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
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
import request from '@/utils/request'

defineOptions({ name: 'PurchaseOrderForm' })
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)
const detailTableRef = ref<InstanceType<typeof BillDetailTable> | null>(null)

// ── useBillForm ──
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
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
  billPrefix: 'CG',
  api: {
    create: (data: any) => request.post('/erp/purchase/order', data),
    update: (id: number, data: any) => request.put(`/erp/purchase/order/${id}`, data),
    getById: (id: number) => request.get(`/erp/purchase/order/${id}`),
  },
  redirectPath: '/purchase/order',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', unit: '', smallUnit: '',
    quantity: 0, unitPrice: 0, taxRate: 13, brand: '', origin: '', model: '',
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
      orderDate: fd.date,
      expectedReceiveTime: fd.expectedReceiveDate || null,
      purchaseType: fd.purchaseType,
      sourceBillNo: fd.sourceBillNo,
      productAmount: totalAmount.value,
      discountAmount: fd.discountAmount || 0,
      billAmount: totalWithTax.value,
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
    // 默认值Tab
    defaultWarehouseId: undefined, defaultBuyerId: undefined, defaultPaymentMethod: undefined, defaultTaxRate: 13,
    // 打印设置Tab
    printTemplate: 'standard', printCopies: 1, printPaperSize: 'A5',
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

// ── 基本信息字段（35个，分组） ──
const basicInfoFields = computed<BasicInfoField[]>(() => [
  // 基本信息区
  { key: 'supplierId', label: '供应商', type: 'select', required: true, width: 300, options: (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'supplierCode', label: '供应商编号', type: 'display', width: 120 },
  { key: 'bankName', label: '开户行', type: 'display', width: 180 },
  { key: 'bankAccount', label: '银行账号', type: 'display', width: 180 },
  { key: 'taxNo', label: '税号', type: 'display', width: 160 },
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, width: 180, options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'buyerId', label: '经手人', type: 'select', required: true, width: 150, options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'deptId', label: '部门', type: 'select', width: 150, options: (optionRefs.departments || []).map((d: any) => ({ label: d.name, value: d.id })) },
  { key: 'date', label: '单据日期', type: 'date', required: true, width: 150 },
  { key: 'contactName', label: '联系人', type: 'display', width: 100 },
  { key: 'contactPhone', label: '联系电话', type: 'display', width: 140 },
  { key: 'contactAddress', label: '联系地址', type: 'display', width: 240 },
  // 自定义字段
  { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extText1', label: '自定义字段3(文本)', type: 'input', width: 140 },
  { key: 'extText2', label: '自定义字段4(文本)', type: 'input', width: 140 },
  { key: 'extText3', label: '自定义字段5(文本)', type: 'input', width: 140 },
  // 审核/摘要
  { key: 'auditorName', label: '审核人', type: 'display', width: 120 },
  { key: 'summary', label: '摘要', type: 'input', width: 300 },
  // 付款区（对标系统字段顺序）
  { key: 'depositAccount1', label: '订金账户1', type: 'select', width: 160, options: (optionRefs.accounts || []).map((a: any) => ({ label: a.name, value: a.id })) },
  { key: 'depositAmount1', label: '订金金额1', type: 'number', width: 130, precision: 2, suffixBtn: '全' },
  { key: 'moreAccounts', label: '更多账户', type: 'input', width: 130, disabled: true },
  { key: 'depositBalance', label: '订金余额', type: 'number', width: 120, disabled: true, precision: 2 },
  { key: 'prevPrepaid', label: '此前预付', type: 'number', width: 120, disabled: true, precision: 2 },
  { key: 'usePrepaid', label: '使用预付款', type: 'number', width: 120, precision: 2 },
  { key: 'prevDebt', label: '此前欠款', type: 'number', width: 120, disabled: true, precision: 2 },
  { key: 'currentDebt', label: '本次欠款', type: 'number', width: 120, disabled: true, precision: 2 },
  { key: 'otherExpense', label: '其他费用', type: 'number', width: 120, precision: 2 },
  // 业务信息
  { key: 'expectedReceiveDate', label: '预计到货', type: 'date', width: 150 },
  { key: 'paymentTerm', label: '付款期限', type: 'input', width: 150 },
  { key: 'otherExpense', label: '其他费用', type: 'number', width: 120, precision: 2 },
  { key: 'sourceBillNo', label: '源单', type: 'input', width: 160 },
  // 单据信息
  { key: 'remark', label: '单据备注', type: 'input', width: 300 },
  { key: 'createByName', label: '制单人', type: 'display', width: 100 },
  { key: 'createTime', label: '制单时间', type: 'display', width: 160 },
  { key: 'printCount', label: '打印次数', type: 'display', width: 90 },
  { key: 'discountAmount', label: '优惠金额', type: 'number', width: 120, precision: 2 },
])

// ── 摘要 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '总数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '税额', value: totalTaxAmount.value.toFixed(2) },
  { label: '价税合计', value: totalWithTax.value.toFixed(2), divider: true },
])

// ── 页脚 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ── 明细表格列（57列） ──
const detailColumns: DetailColumnConfig[] = [
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
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
  // 8个价格等级列（默认隐藏，销售模块专属）
  { key: 'restaurant', title: '餐饮店', type: 'checkbox', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'checkbox', width: 90, defaultHidden: true },
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
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
])

// ── 处理函数 ──
function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
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

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(rowIndex: number, fieldKey: string, value: any) {
  const item = formData.products?.[rowIndex]
  if (!item) return
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    item.amount = (item.quantity || 0) * (item.unitPrice || 0)
  }
  if (fieldKey === 'discountRate' && item.unitPrice) {
    const rate = item.discountRate || 0
    item.discountedUnitPrice = item.unitPrice * (1 - rate / 100)
    item.discountedAmount = (item.quantity || 0) * (item.discountedUnitPrice || 0)
  }
  if (fieldKey === 'costPrice') {
    item.costAmount = (item.quantity || 0) * (item.costPrice || 0)
  }
}

// ── 打印/配置/导入 ──
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const fileInputRef = ref<HTMLInputElement | null>(null)
const importLoading = ref(false)

// ── 配置弹窗：页面配置表格 ──
const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

const pageConfigFields = computed(() =>
  basicInfoFields.value.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
    visible: f.type !== 'display' || true,
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }))
)

const printData = computed(() => ({
  ...formData,
  orderNo: formData.orderNo || '待生成',
  creatorName: currentUserName.value || '系统',
}))

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history': router.push('/erp/purchase'); break
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

onMounted(() => {
  const query = route.query
  if (query.supplierId) {
    formData.supplierId = Number(query.supplierId)
    formData.supplierName = query.supplierName || ''
    if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
  }
  if (query.warehouseId) {
    formData.warehouseId = Number(query.warehouseId)
    formData.warehouseName = query.warehouseName || ''
  }
  if (formData.products.length === 0) handleAddProduct()
  nextTick(() => { tableMaxHeight.value = Math.max(200, window.innerHeight - 420) })
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.print-record-link { color: #1890ff; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; }
.config-hint { font-size: 12px; color: #ff4d4f; margin-bottom: 8px; }
</style>
