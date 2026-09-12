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
      :collapsible-fields="true"
      :collapsed-rows="2"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @tab-suffix-btn="handleTabSuffixBtn"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ 换入仓库数据表 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换入仓库数据表</span>
            <span class="warehouse-count">{{ formData.inWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            v-model:data-source="formData.inWarehouseItems"
            :columns="inWarehouseColumns"
            :summary-columns="inWarehouseSummaryColumns"
            storage-key="sale-exchange-in-detail-columns"
            global-config-key="sale-exchange-in-detail-columns"
            @cell-change="handleInWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenInWarehouseProductModal"
          >
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

        <!-- ═══ 换出仓库数据表 ═══ -->
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换出仓库数据表</span>
            <span class="warehouse-count">{{ formData.outWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            v-model:data-source="formData.outWarehouseItems"
            :columns="outWarehouseColumns"
            :summary-columns="outWarehouseSummaryColumns"
            storage-key="sale-exchange-out-detail-columns"
            global-config-key="sale-exchange-out-detail-columns"
            @cell-change="handleOutWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenOutWarehouseProductModal"
          >
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
        <div v-if="isFieldVisible('remark')" class="remark-section">
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
          <span v-if="isFieldVisible('creatorName')" class="doc-info-item">制单人 <a-tag
            color="blue"
            size="small"
          >{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
          <span v-if="isFieldVisible('createTime')" class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          <span v-if="isFieldVisible('printCount')" class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 主数据快速查询弹窗（字段 +Q 触发） ═══ -->
    <MasterSelectModal
      v-model:open="masterSelect.open"
      :title="masterSelect.title"
      :columns="masterSelect.columns"
      :data-source="masterSelect.dataSource"
      :search-placeholder="masterSelect.placeholder"
      @select="handleMasterSelect"
    />

    <!-- ═══ 打印弹窗 ═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="sale-exchange"
      :print-data="printPayload"
      :default-template-id="formData.printTemplate"
      :default-copies="formData.printCopies"
      :always-last-template="formData.printAlwaysLastTemplate"
      @print-success="handlePrintSuccess"
    />

    <!-- ═══ 配置弹窗：页面配置 / 录单默认值 / 打印设置 ═══ -->
    <a-modal
      v-model:open="showFormConfig"
      title="配置"
      :width="780"
      :footer="null"
      destroy-on-close
    >
      <a-tabs v-model:active-key="configModalTab" size="small">
        <!-- Tab 1: 页面配置（35 个字段，对标文档「页面配置 Tab」） -->
        <a-tab-pane key="pageConfig" tab="页面配置">
          <p class="config-hint">
            勾选后自动保存（该设置对本页所有字段生效）。「显示区域」标明该字段在页面上的实际位置：顶部基本信息 / 收款Tab / 源单Tab / 备注区 / 单据信息。
          </p>
          <a-table
            :columns="pageConfigTableColumns"
            :data-source="pageConfigFields"
            :pagination="false"
            size="small"
            row-key="key"
            :row-class-name="pageConfigRowClass"
            :scroll="{ y: 380 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'group'">
                <span v-if="record.isGroupStart" class="config-group-label">{{ record.group }}</span>
              </template>
              <template v-if="column.key === 'visible'">
                <a-checkbox
                  :checked="record.visible"
                  @change="(e: any) => handleFieldVisibleChange(record.key, e.target.checked)"
                />
              </template>
              <template v-if="column.key === 'enterJump'">
                <a-checkbox
                  :checked="record.enterJump"
                  @change="(e: any) => handleEnterJumpChange(record.key, e.target.checked)"
                />
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <!-- Tab 2: 录单默认值 -->
        <a-tab-pane key="defaultValues" tab="录单默认值">
          <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-item label="默认换入仓库">
              <a-select
                v-model:value="formData.defaultInWarehouseId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="warehouseOptions"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认换出仓库">
              <a-select
                v-model:value="formData.defaultOutWarehouseId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="warehouseOptions"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认经手人">
              <a-select
                v-model:value="formData.defaultHandlerId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="optionRefs.users.map((u: any) => ({ label: u.name, value: u.id }))"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认销售类型">
              <a-select
                v-model:value="formData.defaultSalesType"
                size="small"
                style="width:100%"
                :options="SALES_TYPE_OPTIONS"
                @change="saveFormConfig"
              />
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <!-- Tab 3: 打印设置 -->
        <a-tab-pane key="printSettings" tab="打印设置">
          <p class="config-hint">
            打印配置设置后只针对当前操作员有效
          </p>
          <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-item label="打印模板">
              <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option
                  v-for="tpl in printTemplates"
                  :key="tpl.templateId"
                  :value="tpl.templateId"
                >
                  {{ tpl.templateName }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="打印份数">
              <a-input-number
                v-model:value="formData.printCopies"
                :min="1"
                :max="99"
                :precision="0"
                size="small"
                style="width:100%"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="纸张大小">
              <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option value="A4">
                  A4
                </a-select-option>
                <a-select-option value="A5">
                  A5
                </a-select-option>
                <a-select-option value="B5">
                  B5
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="打印选项">
              <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">
                始终使用最后一次打印的模板，打印时不再选择
              </a-checkbox>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch, onMounted } from 'vue'

defineOptions({ name: 'SaleExchangeForm' })
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ExportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import MasterSelectModal from '@/components/MasterSelectModal/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleExchangeApi, userPageConfigApi } from '@/api/erp'
import { printingApi } from '@/api/printing'
import { exportCsvWithLoading } from '@/utils/exportCsv'
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
const currentWarehouseType = ref<'in' | 'out'>('in')
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const printTemplates = ref<any[]>([])

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
} = useBillForm({
  billPrefix: 'XSHHD',
  // 单据编号来自后端号段（GET /erp/sale/exchange/next-no），严禁前端演示号
  codeApiPath: '/erp/sale/exchange/next-no',
  api: {
    create: saleExchangeApi.create,
    update: saleExchangeApi.update,
    getById: saleExchangeApi.getById,
  },
  redirectPath: '/sales/exchange',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_SALES_DEFAULTS,
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
      if (w) fd.inWarehouseName = w.name || w.warehouseName || ''
    }
    if (fieldKey === 'outWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.outWarehouseName = w.name || w.warehouseName || ''
    }
  },
  onDetailLoaded: (data: any) => {
    // 后端字段名 → 表单字段名
    if (data.exchangeNo) formData.orderNo = data.exchangeNo
    const items: any[] = data.items || []
    formData.inWarehouseItems = items
      .filter((it: any) => it.warehouseType === 1)
      .map((it: any) => ({ ...createEmptyItem(), ...normalizeItem(it) }))
    formData.outWarehouseItems = items
      .filter((it: any) => it.warehouseType !== 1)
      .map((it: any) => ({ ...createEmptyItem(), ...normalizeItem(it) }))
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
      // 表单「编号」字段名(orderNo) → 后端单据编号字段(exchangeNo)
      exchangeNo: fd.orderNo,
      status,
      productAmount: calcTotalAmount(),
      discountAmount: calcTotalDiscountAmount(),
      totalAmount: calcTotalDiscountAmount(),
      inQuantityTotal: calcInQuantityTotal(),
      outQuantityTotal: calcOutQuantityTotal(),
      totalWeight: calcTotalWeight(),
      totalVolume: calcTotalVolume(),
      items: [...inItems, ...outItems],
    }
  },
})

// ── 明细字段名归一（后端 productCode → 前端货号列 itemCode） ──
function normalizeItem(it: any) {
  return {
    ...it,
    itemCode: it.productCode || it.itemCode || '',
    productCode: it.productCode || it.itemCode || '',
  }
}

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
    costAmount: (p.quantity || 0) * (p.costPrice || 0),
    discount: p.discount ?? 100,
    discountPrice: p.discountPrice || 0,
    discountAmount: (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100),
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
  const inAmt = (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100), 0)
  const outAmt = (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100), 0)
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
if (!('customerId' in formData)) {
  Object.assign(formData, {
    customerId: undefined, customerName: '', customerCode: '',
    inWarehouseId: undefined, inWarehouseName: '',
    outWarehouseId: undefined, outWarehouseName: '',
    handlerId: undefined, handlerName: '',
    deptId: undefined, deptName: '',
    exchangeDate: new Date().toISOString().slice(0, 10),
    salesType: '',
    bankName: '', bankAccount: '', taxNo: '',
    contactName: '', contactPhone: '', contactAddress: '',
    paymentAccount: '', moreAccounts: '', receivedAmount: 0,
    prevAdvance: 0, useAdvance: 0, availableAdvance: 0, advanceBalance: 0,
    receivableIncrease: 0, creditLimit: 0, availableCredit: 0,
    prevDebt: 0, currentDebt: 0, debtBalance: 0, collectionDeadline: '',
    sourceOrderId: undefined,
    remark: '',
    printCount: 0,
    summary: '',
    attachment: '',
    creatorName: '',
    createTime: '',
    // 配置项
    defaultInWarehouseId: undefined, defaultOutWarehouseId: undefined,
    defaultHandlerId: undefined, defaultSalesType: undefined,
    printTemplate: undefined, printCopies: 1, printPaperSize: 'A4',
    printAlwaysLastTemplate: false,
    inWarehouseItems: [],
    outWarehouseItems: [],
  })
}

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
  (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100), 0)
)
const outTotalDiscountAmount = computed(() =>
  (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100), 0)
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

const warehouseOptions = computed(() =>
  optionRefs.warehouses.map((w: any) => ({ label: w.name || w.warehouseName || '', value: w.id }))
)

// ═══════════════════════════════════════
// 表单配置（页面配置 / 录单默认值 / 打印设置）
// ═══════════════════════════════════════

const FORM_CONFIG_MODULE = 'sale-exchange'
const FORM_CONFIG_PAGE = 'form'

/**
 * 页面配置字段清单（35 个，顺序与开发文档「页面配置 Tab」一致）。
 * group = 该字段在页面上的实际显示区域（对标文档「字段分组：顶部基本信息 → 收款Tab → 备注区 → 单据信息」），
 * 避免把底部/页签区域的字段误认成头部字段。
 */
const ALL_FORM_CONFIG_FIELDS: { key: string; label: string; group: string }[] = [
  { key: 'orderNo', label: '编号', group: '顶部基本信息' },
  { key: 'customerId', label: '客户', group: '顶部基本信息' },
  { key: 'customerCode', label: '客户编号', group: '顶部基本信息' },
  { key: 'bankName', label: '开户行', group: '顶部基本信息' },
  { key: 'bankAccount', label: '银行账号', group: '顶部基本信息' },
  { key: 'taxNo', label: '税号', group: '顶部基本信息' },
  { key: 'inWarehouseId', label: '换入仓库', group: '顶部基本信息' },
  { key: 'outWarehouseId', label: '换出仓库', group: '顶部基本信息' },
  { key: 'handlerId', label: '经手人', group: '顶部基本信息' },
  { key: 'deptName', label: '部门', group: '顶部基本信息' },
  { key: 'exchangeDate', label: '单据日期', group: '顶部基本信息' },
  { key: 'salesType', label: '销售类型', group: '顶部基本信息' },
  { key: 'contactName', label: '联系人', group: '顶部基本信息' },
  { key: 'contactPhone', label: '联系电话', group: '顶部基本信息' },
  { key: 'contactAddress', label: '收货地址', group: '顶部基本信息' },
  { key: 'paymentAccount', label: '收款账户', group: '收款Tab' },
  { key: 'receivedAmount', label: '收款金额', group: '收款Tab' },
  { key: 'moreAccounts', label: '更多账户', group: '收款Tab' },
  { key: 'prevAdvance', label: '此前预收', group: '收款Tab' },
  { key: 'useAdvance', label: '使用预收款', group: '收款Tab' },
  { key: 'availableAdvance', label: '可用预收', group: '收款Tab' },
  { key: 'advanceBalance', label: '预收余额', group: '收款Tab' },
  { key: 'receivableIncrease', label: '应收款增加', group: '收款Tab' },
  { key: 'creditLimit', label: '信用额度', group: '收款Tab' },
  { key: 'availableCredit', label: '可用额度', group: '收款Tab' },
  { key: 'prevDebt', label: '此前欠款', group: '收款Tab' },
  { key: 'currentDebt', label: '本次欠款', group: '收款Tab' },
  { key: 'debtBalance', label: '欠款余额', group: '收款Tab' },
  { key: 'collectionDeadline', label: '收款期限', group: '收款Tab' },
  { key: 'sourceOrderId', label: '源单', group: '源单Tab' },
  { key: 'remark', label: '单据备注', group: '备注区' },
  { key: 'creatorName', label: '制单人', group: '单据信息' },
  { key: 'createTime', label: '制单时间', group: '单据信息' },
  { key: 'printCount', label: '打印次数', group: '单据信息' },
  { key: 'totalAmount', label: '本单金额', group: '单据信息' },
]

const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>({})
ALL_FORM_CONFIG_FIELDS.forEach(f => { pageConfig[f.key] = { visible: true, enterJump: false } })

function isFieldVisible(key: string, defaultValue = true): boolean {
  return pageConfig[key]?.visible ?? defaultValue
}

/** 分组信息只在每组首行展示一次，视觉上等价于分组表头 */
let lastConfigGroup = ''
const pageConfigFields = computed(() =>
  ALL_FORM_CONFIG_FIELDS.map((f, i) => {
    const isGroupStart = f.group !== lastConfigGroup
    lastConfigGroup = f.group
    return {
      key: f.key,
      index: i + 1,
      name: f.label,
      group: f.group,
      isGroupStart,
      visible: pageConfig[f.key]?.visible !== false,
      enterJump: pageConfig[f.key]?.enterJump === true,
    }
  })
)

const pageConfigTableColumns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 60 },
  { title: '显示区域', dataIndex: 'group', key: 'group', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', dataIndex: 'enterJump', key: 'enterJump', width: 100, align: 'center' as const },
]

function pageConfigRowClass(record: any) {
  return record?.isGroupStart ? 'config-group-start' : ''
}

function handleFieldVisibleChange(key: string, checked: boolean) {
  if (!pageConfig[key]) pageConfig[key] = { visible: true, enterJump: false }
  pageConfig[key].visible = checked
  saveFormConfig()
}

function handleEnterJumpChange(key: string, checked: boolean) {
  if (!pageConfig[key]) pageConfig[key] = { visible: true, enterJump: false }
  pageConfig[key].enterJump = checked
  saveFormConfig()
}

/** 保存表单配置（页面配置 + 录单默认值 + 打印设置） */
async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(
      Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])
    ),
    defaults: {
      defaultInWarehouseId: formData.defaultInWarehouseId ?? null,
      defaultOutWarehouseId: formData.defaultOutWarehouseId ?? null,
      defaultHandlerId: formData.defaultHandlerId ?? null,
      defaultSalesType: formData.defaultSalesType ?? null,
    },
    print: {
      printTemplate: formData.printTemplate ?? null,
      printCopies: formData.printCopies ?? 1,
      printPaperSize: formData.printPaperSize ?? 'A4',
      printAlwaysLastTemplate: formData.printAlwaysLastTemplate === true,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 配置不可用时保持默认 */ }
}

/** 加载表单配置并应用 */
async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed?.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = parsed.fields[k].enterJump === true
        }
      })
    }
    if (parsed?.defaults) {
      Object.assign(formData, {
        defaultInWarehouseId: parsed.defaults.defaultInWarehouseId ?? undefined,
        defaultOutWarehouseId: parsed.defaults.defaultOutWarehouseId ?? undefined,
        defaultHandlerId: parsed.defaults.defaultHandlerId ?? undefined,
        defaultSalesType: parsed.defaults.defaultSalesType ?? undefined,
      })
    }
    if (parsed?.print) {
      Object.assign(formData, {
        printTemplate: parsed.print.printTemplate ?? undefined,
        printCopies: parsed.print.printCopies ?? 1,
        printPaperSize: parsed.print.printPaperSize ?? 'A4',
        printAlwaysLastTemplate: parsed.print.printAlwaysLastTemplate === true,
      })
    }
  } catch { /* 配置不可用时保持默认 */ }
}

/** 新建单据时套用「录单默认值」；仅填空缺，避免覆盖用户已选值（选项异步到达后再次调用即可补齐名称） */
function applyRecordDefaults() {
  if (hasBillContent()) return
  if (formData.defaultInWarehouseId && !formData.inWarehouseName) {
    baseFieldChange('inWarehouseId', formData.defaultInWarehouseId)
  }
  if (formData.defaultOutWarehouseId && !formData.outWarehouseName) {
    baseFieldChange('outWarehouseId', formData.defaultOutWarehouseId)
  }
  if (formData.defaultHandlerId && !formData.handlerName) {
    baseFieldChange('handlerId', formData.defaultHandlerId)
  }
  if (formData.defaultSalesType && !formData.salesType) {
    formData.salesType = formData.defaultSalesType
  }
}

function hasBillContent(): boolean {
  return !!route.query.id || !!formData.id
}

/** 打印模板列表（取真实已发布模板） */
async function loadPrintTemplates() {
  try {
    const res: any = await printingApi.getTemplates({ page: 1, size: 50, pageCode: 'sale-exchange', status: 1 })
    printTemplates.value = res?.data?.records || res?.records || []
    if (!formData.printTemplate && printTemplates.value.length) {
      const def = printTemplates.value.find((t: any) => t.isDefault) || printTemplates.value[0]
      formData.printTemplate = def.templateId
    }
  } catch { printTemplates.value = [] }
}

// ═══════════════════════════════════════
// 主数据快速查询
// ═══════════════════════════════════════

const masterSelect = reactive({
  open: false,
  field: '',
  title: '',
  placeholder: '',
  columns: [] as any[],
  dataSource: [] as any[],
})

const MASTER_SELECT_CONFIG: Record<string, { title: string; placeholder: string; columns: any[]; source: () => any[] }> = {
  customerId: {
    title: '选择客户',
    placeholder: '按客户名称 / 编号过滤',
    columns: [
      { title: '客户名称', dataIndex: 'name', key: 'name' },
      { title: '客户编号', dataIndex: 'code', key: 'code', width: 140 },
      { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 110 },
      { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
    ],
    source: () => optionRefs.customers.map((c: any) => ({ ...c, name: c.name || c.customerName })),
  },
  handlerId: {
    title: '选择经手人',
    placeholder: '按姓名 / 部门过滤',
    columns: [
      { title: '姓名', dataIndex: 'name', key: 'name' },
      { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 180 },
    ],
    source: () => optionRefs.users,
  },
  inWarehouseId: {
    title: '选择换入仓库',
    placeholder: '按仓库名称过滤',
    columns: [
      { title: '仓库名称', dataIndex: 'name', key: 'name' },
      { title: '仓库编号', dataIndex: 'code', key: 'code', width: 140 },
    ],
    source: () => optionRefs.warehouses.map((w: any) => ({ ...w, name: w.name || w.warehouseName })),
  },
  outWarehouseId: {
    title: '选择换出仓库',
    placeholder: '按仓库名称过滤',
    columns: [
      { title: '仓库名称', dataIndex: 'name', key: 'name' },
      { title: '仓库编号', dataIndex: 'code', key: 'code', width: 140 },
    ],
    source: () => optionRefs.warehouses.map((w: any) => ({ ...w, name: w.name || w.warehouseName })),
  },
  paymentAccount: {
    title: '选择收款账户',
    placeholder: '按账户名称过滤',
    columns: [
      { title: '账户名称', dataIndex: 'name', key: 'name' },
      { title: '账户编号', dataIndex: 'code', key: 'code', width: 140 },
      { title: '余额', dataIndex: 'balance', key: 'balance', width: 120 },
    ],
    source: () => accountOptions.value.map((a: any) => ({ ...a, name: a.name || a.accountName })),
  },
}

const accountOptions = ref<any[]>([])

async function loadExtraOptions() {
  try {
    accountOptions.value = await optionsApi.getAccounts()
  } catch {
    accountOptions.value = []
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  const conf = MASTER_SELECT_CONFIG[fieldKey]
  if (!conf) {
    message.info(`「${fieldKey}」暂不支持快速查询`)
    return
  }
  masterSelect.field = fieldKey
  masterSelect.title = conf.title
  masterSelect.placeholder = conf.placeholder
  masterSelect.columns = conf.columns
  masterSelect.dataSource = conf.source()
  masterSelect.open = true
}

function handleMasterSelect(record: any) {
  const field = masterSelect.field
  if (field === 'paymentAccount') {
    formData.paymentAccount = record.name || record.accountName || ''
    return
  }
  if (field === 'moreAccounts') {
    const name = record.name || ''
    if (!name) return
    const cur = String(formData.moreAccounts || '').trim()
    formData.moreAccounts = cur ? `${cur}, ${name}` : name
    return
  }
  baseFieldChange(field, record.id)
}

/** Tab 字段后缀按钮（+Q 快速查询 / ··· 更多账户） */
function handleTabSuffixBtn(fieldKey: string, btnText: string) {
  if (btnText === '+Q') {
    handleSearchBtn(fieldKey, btnText)
    return
  }
  if (btnText === '···' && fieldKey === 'moreAccounts') {
    masterSelect.field = 'moreAccounts'
    const conf = MASTER_SELECT_CONFIG.paymentAccount
    masterSelect.title = '追加更多账户'
    masterSelect.placeholder = conf.placeholder
    masterSelect.columns = conf.columns
    masterSelect.dataSource = conf.source()
    masterSelect.open = true
  }
}

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => {
  const docId = (route.query.id as string) || formData.id
  const status = formData.status
  const actions: any[] = []
  if (docId) {
    if (status === 0) {
      actions.push({ key: 'submit', label: '提交审批' })
    } else if (status === 1) {
      actions.push({ key: 'approve', label: '审核通过' })
      actions.push({ key: 'reject', label: '审核拒绝' })
    } else if (status === 2) {
      actions.push({ key: 'complete', label: '完成换货' })
    }
    if (status === 0 || status === 1 || status === 2) {
      actions.push({ key: 'cancel', label: '取消' })
    }
  }
  actions.push(
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
  )
  return {
    title: '销售换货单',
    orderNo: isFieldVisible('orderNo') ? formData.orderNo : '',
    showAttachment: true,
    actions,
  }
})

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435, searchBtn: '+Q' },
  { key: 'customerCode', label: '客户编号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankName', label: '开户行', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankAccount', label: '银行账号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'taxNo', label: '税号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'inWarehouseId', label: '换入仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'outWarehouseId', label: '换出仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q' },
  { key: 'deptName', label: '部门', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'exchangeDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'salesType', label: '销售类型', type: 'select', inlineLabel: true, width: 210 },
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 435, readonly: true },
]

const SALES_TYPE_OPTIONS = [
  { label: '普通销售', value: '普通销售' },
  { label: '赊销', value: '赊销' },
  { label: '代销', value: '代销' },
]

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => isFieldVisible(f.key))
    .map(f => {
      let options: any[] | undefined
      let loading: boolean | undefined
      let searchBtn: string | undefined = f.searchBtn
      if (f.key === 'customerId') {
        options = optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id }))
        loading = loadingOptions.value
      } else if (f.key === 'inWarehouseId' || f.key === 'outWarehouseId') {
        options = warehouseOptions.value
        loading = loadingOptions.value
      } else if (f.key === 'handlerId') {
        options = optionRefs.users.map((u: any) => ({ label: u.name, value: u.id }))
        loading = loadingOptions.value
      } else if (f.key === 'salesType') {
        options = SALES_TYPE_OPTIONS
      }
      return { ...f, options, loading, searchBtn }
    })
)

const ALL_TAB_FIELDS: Record<string, { key: string; label: string; type: any; readonly?: boolean; searchBtn?: string; suffixBtn?: string; options?: any[]; width: number; precision?: number }[]> = {
  payment: [
    { key: 'paymentAccount', label: '收款账户', type: 'select', searchBtn: '+Q', suffixBtn: '+Q', width: 210 },
    { key: 'receivedAmount', label: '收款金额', type: 'number', precision: 2, width: 210 },
    { key: 'moreAccounts', label: '更多账户', type: 'input', suffixBtn: '···', width: 260 },
    { key: 'prevAdvance', label: '此前预收', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'useAdvance', label: '使用预收款', type: 'number', precision: 2, width: 210 },
    { key: 'availableAdvance', label: '可用预收', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'advanceBalance', label: '预收余额', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'receivableIncrease', label: '应收款增加', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'creditLimit', label: '信用额度', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'availableCredit', label: '可用额度', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'prevDebt', label: '此前欠款', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'currentDebt', label: '本次欠款', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'debtBalance', label: '欠款余额', type: 'number', precision: 2, readonly: true, width: 210 },
    { key: 'collectionDeadline', label: '收款期限', type: 'date', width: 210 },
  ],
  source: [
    { key: 'sourceOrderId', label: '源单', type: 'input', searchBtn: '+Q', width: 435 },
  ],
}

const tabsConfig = computed<BillTabConfig[]>(() => [
  {
    key: 'payment',
    tab: '收款',
    fields: ALL_TAB_FIELDS.payment.filter(f => isFieldVisible(f.key)).map(f => ({ ...f })),
  },
  {
    key: 'source',
    tab: '源单',
    fields: ALL_TAB_FIELDS.source.filter(f => isFieldVisible(f.key)).map(f => ({ ...f })),
  },
].filter(t => t.fields.length > 0))

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
// 换入仓库数据表格列配置（对标 63 列，商品行属性在第 12 位）
// ═══════════════════════════════════════

const PRODUCT_LINE_ATTR_OPTIONS = [
  { label: '普通', value: 'normal' },
  { label: '赠品', value: 'gift' },
  { label: '促销品', value: 'promo' },
]

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
  { key: 'productLineAttr', title: '商品行属性', type: 'select', width: 110, options: PRODUCT_LINE_ATTR_OPTIONS },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'stockConverted', title: '可用库存换算结果', type: 'number', width: 130, precision: 2, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2, readonly: true },
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
  { key: 'recentSaleDate', title: '最近销售日期', type: 'date', width: 110, readonly: true },
  { key: 'recentSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2, align: 'right', readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'discount', title: '折扣(%)', type: 'number', width: 80, precision: 2 },
  { key: 'discountPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountAmount', title: '折后金额', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 90, precision: 2 },
  { key: 'extNum1', title: '单据自定义1(数字字段)', type: 'number', width: 150, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字字段)', type: 'number', width: 150, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字字段)', type: 'number', width: 150, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本字段)', type: 'input', width: 150 },
  { key: 'extText2', title: '单据自定义5(文本字段)', type: 'input', width: 150 },
  { key: 'extNum6', title: '单据自定义6(数字字段)', type: 'number', width: 150, precision: 2 },
  { key: 'extNum7', title: '单据自定义7(数字字段)', type: 'number', width: 150, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'select', width: 150, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })) },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'select', width: 130, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })) },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'select', width: 130 },
])

// ═══════════════════════════════════════
// 换出仓库数据表格列配置（对标 63 列，商品行属性在第 43 位）
// ═══════════════════════════════════════

const outWarehouseColumns = computed<DetailColumnConfig[]>(() => {
  const cols = inWarehouseColumns.value.filter(c => c.key !== 'productLineAttr')
  const weightIdx = cols.findIndex(c => c.key === 'weight')
  cols.splice(weightIdx + 1, 0, {
    key: 'productLineAttr', title: '商品行属性', type: 'select', width: 110,
    options: PRODUCT_LINE_ATTR_OPTIONS,
  } as DetailColumnConfig)
  return cols
})

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
  row.unitPrice = p.salePrice || p.retailPrice || p.price || 0
  calcRowAmount(row)
}

// ═══ 行金额计算 ═══
function calcRowAmount(row: any) {
  const qty = row.quantity || 0
  const price = row.unitPrice || 0
  row.amount = qty * price
  const disc = row.discount ?? 100
  row.discountPrice = price * (disc / 100)
  row.discountAmount = qty * row.discountPrice
  row.costAmount = qty * (row.costPrice || 0)
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

// ═══ 产品选择弹窗确认 ═══
function handleProductSelectConfirm(products: any[]) {
  const targetArray = currentWarehouseType.value === 'in'
    ? formData.inWarehouseItems
    : formData.outWarehouseItems
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : targetArray.length

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
  currentSelectRowIndex.value = -1
  message.success(`已选择 ${products.length} 个商品`)
}

// ═══ 打印 ═══
const printPayload = computed(() => ({
  doc: { ...formData },
  inItems: formData.inWarehouseItems,
  outItems: formData.outWarehouseItems,
  items: [...(formData.inWarehouseItems || []), ...(formData.outWarehouseItems || [])],
}))

function openPrintDialog() {
  if (!formData.id) {
    message.warning('请先保存换货单后再打印')
    return
  }
  printDialogRef.value?.open()
}

async function handlePrintSuccess() {
  if (formData.id) {
    try {
      await saleExchangeApi.print(formData.id)
      formData.printCount = (formData.printCount || 0) + 1
    } catch { /* 打印次数回写失败不影响打印结果 */ }
  }
}

// ═══ 工作流动作 ═══
async function handleWorkflow(action: string) {
  const id = formData.id
  if (!id) {
    message.warning('请先保存换货单')
    return
  }
  const labelMap: Record<string, string> = {
    submit: '提交审批', approve: '审核通过', reject: '审核拒绝',
    complete: '完成换货', cancel: '取消换货单',
  }
  const confirmText = action === 'approve'
    ? '审核通过后换入仓库将增加库存、换出仓库将扣减库存，确定继续？'
    : action === 'cancel'
      ? '取消后（如已过账）将回滚库存，确定继续？'
      : `确定${labelMap[action]}吗？`
  Modal.confirm({
    title: labelMap[action],
    content: confirmText,
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        let res: any
        if (action === 'submit') res = await saleExchangeApi.submit(id)
        else if (action === 'approve') res = await saleExchangeApi.approve(id, '审核通过')
        else if (action === 'reject') res = await saleExchangeApi.reject(id, '审核拒绝')
        else if (action === 'complete') res = await saleExchangeApi.complete(id)
        else res = await saleExchangeApi.cancel(id, '手动取消')
        formData.status = res?.status ?? formData.status
        message.success(`${labelMap[action]}成功`)
      } catch (e: any) {
        message.error(e?.response?.data?.message || e?.message || `${labelMap[action]}失败`)
      }
    },
  })
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'submit':
    case 'approve':
    case 'reject':
    case 'complete':
    case 'cancel':
      handleWorkflow(actionKey)
      break
    case 'history':
      router.push('/sales/exchange/index')
      break
    case 'print':
      openPrintDialog()
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'export': {
      const id = route.query.id as string
      if (id) {
        // 明细导出：真实写入 CSV（含换入/换出全部商品行）
        const headers = ['仓库类型', '货号', '商品名称', '规格', '单位', '数量', '单价', '金额', '折后金额', '备注']
        const rowsIn = (formData.inWarehouseItems || [])
          .filter((p: any) => p.productId != null)
          .map((p: any) => ['换入', p.itemCode || '', p.productName || '', p.specification || '', p.unit || '',
            p.quantity || 0, p.unitPrice || 0, p.amount || 0, p.discountAmount || 0, p.remark || ''])
        const rowsOut = (formData.outWarehouseItems || [])
          .filter((p: any) => p.productId != null)
          .map((p: any) => ['换出', p.itemCode || '', p.productName || '', p.specification || '', p.unit || '',
            p.quantity || 0, p.unitPrice || 0, p.amount || 0, p.discountAmount || 0, p.remark || ''])
        exportCsvWithLoading(headers, [...rowsIn, ...rowsOut], `销售换货单_${formData.orderNo || '明细'}`)
      } else {
        message.warning('请先保存换货单后再导出')
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
// 下拉选项异步到达后补齐默认值对应的名称快照
watch(
  () => [optionRefs.warehouses.length, optionRefs.users.length],
  () => { applyRecordDefaults() }
)

onMounted(async () => {
  await loadFormConfig()
  loadPrintTemplates()
  loadExtraOptions()

  // 新建：套用录单默认值（选项未就绪时由 watch 补齐）
  applyRecordDefaults()

  // 确保至少有空行
  if (formData.inWarehouseItems.length === 0) {
    for (let i = 0; i < 10; i++) formData.inWarehouseItems.push(createEmptyItem())
  }
  if (formData.outWarehouseItems.length === 0) {
    for (let i = 0; i < 10; i++) formData.outWarehouseItems.push(createEmptyItem())
  }
})
</script>

<style scoped>
.warehouse-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
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

.config-hint {
  font-size: 12px;
  color: #fa8c16;
  margin-bottom: 12px;
}

/* 页面配置：按「显示区域」分组展示 */
:deep(.config-group-start) > td {
  border-top: 1px solid #d9d9d9;
  background: #fafafa;
}
.config-group-label {
  font-weight: 600;
  color: #262626;
}
</style>
