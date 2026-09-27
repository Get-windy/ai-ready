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
      collapsible-fields
      :collapsed-rows="2"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @tab-suffix-btn="handleTabSuffixBtn"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ Zone 3: 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="detailColumns"
          v-model:data-source="formData.products"
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
        <!-- 单据信息行（底部）：各字段由「页面配置」对应区域独立控制显隐 -->
        <div class="doc-info-row">
          <span v-if="isFieldVisible('creatorName')" class="doc-info-item">制单人 <a-tag
            color="blue"
            size="small"
          >{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
          <span v-if="isFieldVisible('bookkeepingTime')" class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          <span v-if="isFieldVisible('printCount')" class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <span v-if="isFieldVisible('sourceOrder')" class="doc-info-item">源单 <a-tag size="small">{{ formData.sourceOrder || '0' }}</a-tag></span>
          <span v-if="isFieldVisible('auditorName')" class="doc-info-item">审核人 <a-tag size="small">{{ formData.auditorName || '-' }}</a-tag></span>
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

    <!-- ═══ 配置弹窗（齿轮触发）：页面配置 / 录单默认值 / 打印设置 ═══ -->
    <a-modal
      v-model:open="showFormConfig"
      title="配置"
      :width="780"
      :footer="null"
      destroy-on-close
    >
      <a-tabs v-model:active-key="configModalTab" size="small">
        <!-- Tab 1: 页面配置 -->
        <a-tab-pane key="pageConfig" tab="页面配置">
          <p class="config-hint">
            勾选后自动保存（该设置对本页所有字段生效）
            <a-button type="link" size="small" @click="openItemColumnConfig">明细列配置</a-button>
          </p>
          <div class="form-config-table-wrap">
          <a-table
            :columns="pageConfigTableColumns"
            :data-source="pageConfigFields"
            :pagination="false"
            size="small"
            row-key="key"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'displayName'">
                <a-input
                  :value="record.displayName"
                  size="small"
                  placeholder="默认同名称"
                  @change="(e: any) => handleDisplayNameChange(record.key, e.target.value)"
                  @blur="saveFormConfig"
                />
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
          </div>
        </a-tab-pane>
        <!-- Tab 2: 录单默认值 -->
        <a-tab-pane key="defaultValues" tab="录单默认值">
          <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-item label="默认发货仓库">
              <a-select
                v-model:value="formData.defaultWarehouseId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="optionRefs.warehouses.map((w: any) => ({ label: w.name || w.warehouseName, value: w.id }))"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认经手人">
              <a-select
                v-model:value="formData.defaultSalespersonId"
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
                v-model:value="formData.defaultSaleType"
                size="small"
                style="width:100%"
                :options="SALE_TYPE_OPTIONS"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认结款方式">
              <a-select
                v-model:value="formData.defaultSettlementMethod"
                size="small"
                style="width:100%"
                :options="SETTLEMENT_OPTIONS"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认配送方式">
              <a-select
                v-model:value="formData.defaultDeliveryMethod"
                size="small"
                style="width:100%"
                :options="DELIVERY_METHOD_OPTIONS"
                @change="saveFormConfig"
              />
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <!-- Tab 3: 打印设置 -->
        <a-tab-pane key="printSettings" tab="打印设置">
          <p class="config-hint">打印配置设置后只针对当前操作员有效</p>
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
              <a-input-number v-model:value="formData.printCopies" :min="1" :max="99" :precision="0" size="small" style="width:100%" @change="saveFormConfig" />
            </a-form-item>
            <a-form-item label="纸张大小">
              <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option value="A4">A4</a-select-option>
                <a-select-option value="A5">A5</a-select-option>
                <a-select-option value="B5">B5</a-select-option>
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

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 主数据快速查询弹窗（字段 +Q / Q 触发） ═══ -->
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
      page-code="sale"
      :document-id="formData.id"
      :print-data="printData"
      :default-template-id="formData.printTemplate"
      :default-copies="formData.printCopies"
      :always-last-template="formData.printAlwaysLastTemplate"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'

defineOptions({ name: 'SaleForm' })
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { exportCsvWithLoading } from '@/utils/exportCsv'
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
import MasterSelectModal from '@/components/MasterSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, TabField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleOrderApi, userPageConfigApi } from '@/api/erp'
import { printingApi } from '@/api/printing'
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

// ═══ 表单配置（页面配置 / 录单默认值 / 打印设置，按操作员持久化到后端） ═══
const FORM_CONFIG_MODULE = 'sale-order-form'
const FORM_CONFIG_PAGE = 'config'

const SALE_TYPE_OPTIONS = [
  { label: '正常销售', value: 1 }, { label: '样品销售', value: 2 },
  { label: '促销销售', value: 3 }, { label: '退货物料', value: 4 },
]
const SETTLEMENT_OPTIONS = [
  { label: '现结', value: '现结' }, { label: '月结', value: '月结' },
  { label: '预收', value: '预收' }, { label: '货到付款', value: '货到付款' },
]
const DELIVERY_METHOD_OPTIONS = [
  { label: '自提', value: '自提' }, { label: '送货上门', value: '送货上门' },
  { label: '物流配送', value: '物流配送' }, { label: '快递', value: '快递' },
]

/**
 * 表单页可配置字段清单（68 项，对齐开发文档「页面配置字段列表」）
 * group = 该字段在页面上的实际渲染区域，配置弹窗据此展示，避免把底部字段误标成头部字段
 */
const ALL_FORM_CONFIG_FIELDS: Array<{ key: string; label: string; group: string }> = [
  // ── 头部基本信息区（默认 2 行，超出折叠可展开） ──
  { key: 'orderNo', label: '编号', group: '头部基本信息' },
  { key: 'customerId', label: '客户', group: '头部基本信息' },
  { key: 'customerCode', label: '客户编号', group: '头部基本信息' },
  { key: 'warehouseId', label: '发货仓库', group: '头部基本信息' },
  { key: 'salespersonId', label: '经手人', group: '头部基本信息' },
  { key: 'deptName', label: '部门', group: '头部基本信息' },
  { key: 'orderDate', label: '单据日期', group: '头部基本信息' },
  { key: 'saleType', label: '销售类型', group: '头部基本信息' },
  { key: 'receiverName', label: '收货人', group: '头部基本信息' },
  { key: 'receiverPhone', label: '联系电话', group: '头部基本信息' },
  { key: 'receiverAddress', label: '收货地址', group: '头部基本信息' },
  { key: 'customerLevel', label: '客户级别', group: '头部基本信息' },
  { key: 'summary', label: '摘要', group: '头部基本信息' },
  // ── 底部「收款」Tab ──
  { key: 'depositAccountId', label: '订金账户', group: '收款' },
  { key: 'depositAmount', label: '订金金额', group: '收款' },
  { key: 'moreAccounts', label: '更多账户', group: '收款' },
  { key: 'useAdvancePayment', label: '使用预订货款', group: '收款' },
  { key: 'prevAdvance', label: '此前预收', group: '收款' },
  { key: 'advanceBalance', label: '预收余额', group: '收款' },
  { key: 'creditLimit', label: '信用额度', group: '收款' },
  { key: 'availableCredit', label: '可用额度', group: '收款' },
  { key: 'prevDebt', label: '此前欠款', group: '收款' },
  { key: 'paymentDate', label: '收款日', group: '收款' },
  { key: 'reconciliationDate', label: '对账日', group: '收款' },
  // ── 底部「物流信息」Tab ──
  { key: 'deliveryMethod', label: '配送方式', group: '物流信息' },
  { key: 'deliveryRoute', label: '配送线路', group: '物流信息' },
  { key: 'driverName', label: '司机', group: '物流信息' },
  { key: 'deliveryVehicle', label: '车辆', group: '物流信息' },
  { key: 'logisticsCompany', label: '物流公司', group: '物流信息' },
  { key: 'freightPayer', label: '运费承担方', group: '物流信息' },
  { key: 'shippingFee', label: '运费', group: '物流信息' },
  { key: 'waybillNo', label: '运单号', group: '物流信息' },
  { key: 'codAmount', label: '代收货款', group: '物流信息' },
  { key: 'estimatedShipDate', label: '预计发货', group: '物流信息' },
  { key: 'contactName', label: '联系人', group: '物流信息' },
  { key: 'contactPhone', label: '联系电话(提货)', group: '物流信息' },
  { key: 'pickupAddress', label: '提货地址', group: '物流信息' },
  // ── 底部「会员信息」Tab ──
  { key: 'memberCardNo', label: '会员卡号', group: '会员信息' },
  { key: 'memberName', label: '会员姓名', group: '会员信息' },
  { key: 'prevPoints', label: '此前积分', group: '会员信息' },
  { key: 'salePoints', label: '销售积分', group: '会员信息' },
  { key: 'returnPoints', label: '退货积分', group: '会员信息' },
  { key: 'exchangePointsHeader', label: '兑换积分', group: '会员信息' },
  { key: 'usedPointsHeader', label: '使用积分', group: '会员信息' },
  { key: 'currentPoints', label: '当前积分', group: '会员信息' },
  // ── 底部「扩展信息」Tab ──
  { key: 'bankName', label: '开户行', group: '扩展信息' },
  { key: 'bankAccount', label: '银行账号', group: '扩展信息' },
  { key: 'taxNo', label: '税号', group: '扩展信息' },
  { key: 'customerRemark', label: '客户备注', group: '扩展信息' },
  { key: 'customerTicket', label: '客户一票通', group: '扩展信息' },
  { key: 'extNum1', label: '自定义字段1(数字)', group: '扩展信息' },
  { key: 'extNum2', label: '自定义字段2(数字)', group: '扩展信息' },
  { key: 'extText1', label: '自定义字段3(文本)', group: '扩展信息' },
  { key: 'extText2', label: '自定义字段4(文本)', group: '扩展信息' },
  { key: 'extText3', label: '自定义字段5(文本)', group: '扩展信息' },
  { key: 'region', label: '区域', group: '扩展信息' },
  { key: 'attachment', label: '附件', group: '扩展信息' },
  // ── 备注区（明细表下方） ──
  { key: 'orderRemark', label: '单据备注', group: '备注区' },
  { key: 'buyerRemark', label: '买家备注', group: '备注区' },
  { key: 'footerExtText1', label: '表尾自定义1', group: '备注区' },
  { key: 'footerExtText2', label: '表尾自定义2', group: '备注区' },
  // ── 单据信息行（备注区下方：制单人/时间/打印/源单/审核人） ──
  { key: 'creatorName', label: '制单人', group: '单据信息' },
  { key: 'bookkeepingTime', label: '制单时间', group: '单据信息' },
  { key: 'printCount', label: '打印次数', group: '单据信息' },
  { key: 'sourceOrder', label: '源单', group: '单据信息' },
  { key: 'auditorName', label: '审核人', group: '单据信息' },
  // ── 右侧摘要面板 ──
  { key: 'saleQuantity', label: '销售数量', group: '摘要面板' },
  { key: 'returnQuantity', label: '退货数量', group: '摘要面板' },
  { key: 'productAmount', label: '商品金额', group: '摘要面板' },
  { key: 'promoDiscount', label: '促销优惠', group: '摘要面板' },
  { key: 'discountAmount', label: '优惠金额', group: '摘要面板' },
  { key: 'otherFee', label: '其他费用', group: '摘要面板' },
  { key: 'billAmount', label: '本单金额', group: '摘要面板' },
]

/** 字段配置：{ 字段key: { visible, enterJump, displayName } }，未配置视为显示 */
const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean; displayName: string }>>({})
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const printTemplates = ref<any[]>([])

ALL_FORM_CONFIG_FIELDS.forEach(f => { pageConfig[f.key] = { visible: true, enterJump: false, displayName: '' } })

function isFieldVisible(key: string, defaultValue = true): boolean {
  return pageConfig[key]?.visible ?? defaultValue
}

/** 字段显示名：配置了自定义显示名则覆盖默认 label（对标「显示名」列） */
function displayNameOf(key: string, fallback: string): string {
  const n = pageConfig[key]?.displayName
  return n && n.trim() ? n.trim() : fallback
}

function handleDisplayNameChange(key: string, value: string) {
  if (!pageConfig[key]) pageConfig[key] = { visible: true, enterJump: false, displayName: '' }
  pageConfig[key].displayName = value
}

const pageConfigFields = computed(() =>
  ALL_FORM_CONFIG_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    group: f.group,
    name: f.label,
    displayName: pageConfig[f.key]?.displayName || '',
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump === true,
  }))
)
const pageConfigTableColumns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 56 },
  { title: '区域', dataIndex: 'group', key: 'group', width: 110 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName', width: 150 },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 60, align: 'center' as const },
  { title: '回车键跳转', dataIndex: 'enterJump', key: 'enterJump', width: 96, align: 'center' as const },
]

function handleFieldVisibleChange(key: string, checked: boolean) {
  if (!pageConfig[key]) pageConfig[key] = { visible: true, enterJump: false, displayName: '' }
  pageConfig[key].visible = checked
  saveFormConfig()
}
function handleEnterJumpChange(key: string, checked: boolean) {
  if (!pageConfig[key]) pageConfig[key] = { visible: true, enterJump: false, displayName: '' }
  pageConfig[key].enterJump = checked
  saveFormConfig()
}

/** 保存表单配置（页面配置 + 录单默认值 + 打印设置） */
async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(
      Object.entries(pageConfig).map(([k, v]) => [k, {
        visible: v.visible,
        enterJump: v.enterJump,
        displayName: v.displayName || '',
      }])
    ),
    defaults: {
      defaultWarehouseId: formData.defaultWarehouseId ?? null,
      defaultSalespersonId: formData.defaultSalespersonId ?? null,
      defaultSaleType: formData.defaultSaleType ?? null,
      defaultSettlementMethod: formData.defaultSettlementMethod ?? null,
      defaultDeliveryMethod: formData.defaultDeliveryMethod ?? null,
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
  } catch { /* 静默失败：配置不可用时保持默认 */ }
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
          pageConfig[k].displayName = parsed.fields[k].displayName || ''
        }
      })
    }
    if (parsed?.defaults) {
      Object.assign(formData, {
        defaultWarehouseId: parsed.defaults.defaultWarehouseId ?? undefined,
        defaultSalespersonId: parsed.defaults.defaultSalespersonId ?? undefined,
        defaultSaleType: parsed.defaults.defaultSaleType ?? 1,
        defaultSettlementMethod: parsed.defaults.defaultSettlementMethod ?? '',
        defaultDeliveryMethod: parsed.defaults.defaultDeliveryMethod ?? '',
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
  } catch { /* 静默失败：保持默认 */ }
}

/** 应用录单默认值（新增单据且用户尚未填写时） */
function applyEntryDefaults() {
  if (effectiveMode.value === 'edit') return
  if (!formData.warehouseId && formData.defaultWarehouseId) {
    formData.warehouseId = formData.defaultWarehouseId
    const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
    formData.warehouseName = w?.name || w?.warehouseName || ''
  }
  if (!formData.salespersonId && formData.defaultSalespersonId) {
    formData.salespersonId = formData.defaultSalespersonId
    const u = optionRefs.users.find((x: any) => x.id === formData.defaultSalespersonId)
    formData.salespersonName = u?.name || ''
  }
  if (formData.defaultSaleType) formData.saleType = formData.defaultSaleType
  if (!formData.settlementMethod && formData.defaultSettlementMethod) {
    formData.settlementMethod = formData.defaultSettlementMethod
  }
  if (!formData.deliveryMethod && formData.defaultDeliveryMethod) {
    formData.deliveryMethod = formData.defaultDeliveryMethod
  }
}

/** 打开明细列配置页（明细列由 BillDetailTable 齿轮 + 独立配置页共同维护） */
function openItemColumnConfig() {
  router.push({ name: 'ErpSaleOrderItemColumnConfig' })
}

/** 打印模板列表（打印设置的模板下拉取真实已发布模板） */
async function loadPrintTemplates() {
  try {
    // 「只有已发布才能打印」这条规则由服务端定，前端不再自己拼 status 参数
    const res: any = await printingApi.getDocumentTemplates('sale')
    printTemplates.value = res?.data?.templates || res?.templates || []
    if (!formData.printTemplate && printTemplates.value.length) {
      const def = printTemplates.value.find((t: any) => t.isDefault) || printTemplates.value[0]
      formData.printTemplate = def.templateId
    }
  } catch { printTemplates.value = [] }
}

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const returnQty = ref(0)
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
  // 单据编号来自后端号段（GET /erp/sale/order/next-no），严禁前端自增演示号
  codeApiPath: '/erp/sale/order/next-no',
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
  transformPayload: (fd, status) => {
    // 配置项（录单默认值 / 打印设置）属于本地设置，不进入单据 payload
    const bill: Record<string, any> = { ...fd }
    ;['defaultWarehouseId', 'defaultSalespersonId', 'defaultSaleType', 'defaultSettlementMethod',
      'defaultDeliveryMethod', 'printTemplate', 'printCopies', 'printPaperSize', 'printAlwaysLastTemplate',
    ].forEach(k => { delete bill[k] })
    return {
    ...bill,
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
    }
  },
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
  // 录单默认值（配置弹窗维护，非单据业务字段，提交前剔除）
  defaultWarehouseId: undefined, defaultSalespersonId: undefined,
  defaultSaleType: 1, defaultSettlementMethod: '', defaultDeliveryMethod: '',
  // 打印设置（配置弹窗维护，非单据业务字段，提交前剔除）
  printTemplate: undefined, printCopies: 1, printPaperSize: 'A4', printAlwaysLastTemplate: false,
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
  // 编号受「页面配置 → 编号」显隐控制
  orderNo: isFieldVisible('orderNo') ? formData.orderNo : '',
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
  // 第一行：核心字段（编号显示在标题栏 NO. 处，不占字段区，见 headerConfig）
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
])
// 头部字段区：按配置过滤 + 应用「显示名」；系统字段（审核人/制单人/制单时间/打印次数/源单）在底部「单据信息」行展示，不占头部
const basicInfoFields = computed<BasicInfoField[]>(() =>
  allBasicInfoFields.value
    .filter(f => isFieldVisible(f.key))
    .map(f => ({ ...f, label: displayNameOf(f.key, f.label) }))
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
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)).map(f => ({ ...f, label: displayNameOf(f.key, f.label) })) },
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
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)).map(f => ({ ...f, label: displayNameOf(f.key, f.label) })) },
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
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)).map(f => ({ ...f, label: displayNameOf(f.key, f.label) })) },
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
  ] satisfies TabField[]).filter(f => isFieldVisible(f.key)).map(f => ({ ...f, label: displayNameOf(f.key, f.label) })) },
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
  return allRows
    .filter(r => isFieldVisible(r._filterKey))
    .map(({ _filterKey, ...row }) => ({ ...row, label: displayNameOf(_filterKey, row.label) }))
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

// ═══════════════════════════════════════
// 主数据快速查询（字段 +Q / Q）
// ═══════════════════════════════════════
const masterSelect = reactive({
  open: false,
  field: '' as string,
  title: '选择',
  placeholder: '请输入关键字过滤',
  columns: [] as any[],
  dataSource: [] as any[],
})

/** 各字段对应的主数据源与列（数据取自表单已加载的下拉选项，避免重复请求） */
const MASTER_SELECT_CONFIG: Record<string, { title: string; placeholder: string; columns: any[]; source: () => any[] }> = {
  customerId: {
    title: '选择客户',
    placeholder: '输入客户名称 / 编号',
    columns: [
      { title: '客户名称', dataIndex: 'name', key: 'name', width: 220 },
      { title: '客户编号', dataIndex: 'code', key: 'code', width: 140 },
      { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
      { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 140 },
    ],
    source: () => optionRefs.customers.map((c: any) => ({ ...c, name: c.name || c.customerName, code: c.code || c.customerCode })),
  },
  warehouseId: {
    title: '选择发货仓库',
    placeholder: '输入仓库名称 / 编码',
    columns: [
      { title: '仓库名称', dataIndex: 'name', key: 'name', width: 220 },
      { title: '仓库编码', dataIndex: 'code', key: 'code', width: 140 },
      { title: '仓库地址', dataIndex: 'address', key: 'address', width: 240 },
    ],
    source: () => optionRefs.warehouses.map((w: any) => ({ ...w, name: w.name || w.warehouseName, code: w.code || w.warehouseCode })),
  },
  salespersonId: {
    title: '选择经手人',
    placeholder: '输入姓名 / 账号',
    columns: [
      { title: '姓名', dataIndex: 'name', key: 'name', width: 160 },
      { title: '账号', dataIndex: 'username', key: 'username', width: 160 },
      { title: '所属部门', dataIndex: 'deptName', key: 'deptName', width: 180 },
    ],
    source: () => optionRefs.users.map((u: any) => ({ ...u, code: u.username })),
  },
  depositAccountId: {
    title: '选择订金账户',
    placeholder: '输入账户名称',
    columns: [
      { title: '账户名称', dataIndex: 'name', key: 'name', width: 220 },
      { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 140 },
    ],
    source: () => accountOptions.value.map((a: any) => ({ ...a, name: a.name || a.accountName })),
  },
  receiverName: {
    title: '选择收货人（按客户联系人带入）',
    placeholder: '输入客户名称 / 联系人',
    columns: [
      { title: '客户名称', dataIndex: 'name', key: 'name', width: 220 },
      { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 140 },
      { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 160 },
      { title: '收货地址', dataIndex: 'address', key: 'address', width: 240 },
    ],
    source: () => optionRefs.customers.map((c: any) => ({ ...c, name: c.name || c.customerName })),
  },
}

function handleSearchBtn(fieldKey: string) {
  const conf = MASTER_SELECT_CONFIG[fieldKey]
  if (!conf) return
  masterSelect.field = fieldKey
  masterSelect.title = conf.title
  masterSelect.placeholder = conf.placeholder
  masterSelect.columns = conf.columns
  masterSelect.dataSource = conf.source()
  masterSelect.open = true
}

function handleMasterSelect(record: any) {
  const field = masterSelect.field
  if (field === 'depositAccountId') {
    formData.depositAccountId = record.id
    formData.depositAccount = record.name || ''
    if (record.balance != null) formData.depositBalance = Number(record.balance)
    return
  }
  if (field === 'moreAccounts') {
    const name = record.name || ''
    if (!name) return
    const cur = String(formData.moreAccounts || '').trim()
    formData.moreAccounts = cur ? `${cur}, ${name}` : name
    return
  }
  if (field === 'receiverName') {
    formData.receiverName = record.contactName || record.name || ''
    formData.receiverPhone = record.contactPhone || formData.receiverPhone || ''
    formData.receiverAddress = record.address || formData.receiverAddress || ''
    return
  }
  if (field === 'warehouseId') {
    baseFieldChange('warehouseId', record.id)
    formData.warehouseName = record.name || ''
    return
  }
  if (field === 'salespersonId') {
    formData.salespersonId = record.id
    formData.salespersonName = record.name || ''
    return
  }
  // 客户：走统一字段变更逻辑，自动带出等级/银行/税号/信用额度等
  baseFieldChange(field, record.id)
}

/** Tab 字段后缀按钮（+Q 选择 / 全 全额 / ··· 明细设置）真实行为 */
function handleTabSuffixBtn(fieldKey: string, btnText: string) {
  if (btnText === '+Q') {
    handleSearchBtn(fieldKey)
    return
  }
  if (btnText === '全') {
    // 全额填入：订金金额 / 使用预收款 等金额字段按本单金额带入
    if (fieldKey === 'depositAmount' || fieldKey === 'usePreReceipt') {
      formData[fieldKey] = Number(billAmount.value.toFixed(2))
    }
    return
  }
  if (btnText === '···') {
    if (fieldKey === 'moreAccounts') {
      // 追加一个收款账户到「更多账户」列表
      masterSelect.field = 'moreAccounts'
      masterSelect.title = '追加收款账户'
      masterSelect.placeholder = '输入账户名称'
      masterSelect.columns = [
        { title: '账户名称', dataIndex: 'name', key: 'name', width: 220 },
        { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 140 },
      ]
      masterSelect.dataSource = accountOptions.value.map((a: any) => ({ ...a, name: a.name || a.accountName }))
      masterSelect.open = true
      return
    }
    if (fieldKey === 'useAdvancePayment') {
      // 使用预订货款：取「预收余额」与「本单未结金额」的较小值
      const available = Number(formData.advanceBalance || formData.prevAdvance || 0)
      const need = Number(billAmount.value.toFixed(2))
      formData.useAdvancePayment = Number(Math.min(available, need).toFixed(2))
      if (available <= 0) message.warning('该客户当前没有可用的预收余额')
      return
    }
  }
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
      // 跳转订单处理中心（金标准列表页）
      router.push('/sales/order-center')
      break
    case 'save-draft':
      handleSaveDraft()
      break
    case 'print-order':
    case 'print-summary':
      printDialogRef.value?.open()
      break
    case 'import':
      // 录入明细：打开商品选择弹窗批量追加（复用商品选择能力）
      currentSelectRowIndex.value = -1
      showProductSelect.value = true
      break
    case 'copy-order':
      handleCopyOrder()
      break
    case 'export':
      handleExportOrder()
      break
    case 'config':
      showFormConfig.value = true
      break
  }
}

/** 复制订单：保留客户/商品/物流等业务内容，重置单号为后端新号段，状态回到草稿 */
async function handleCopyOrder() {
  if (!formData.customerId && formData.products.filter((p: any) => p.productId != null).length === 0) {
    message.warning('当前单据没有可复制的内容')
    return
  }
  Modal.confirm({
    title: '复制订单',
    content: '将当前单据内容复制为一张新单据（不含原单号、审批与打印信息），是否继续？',
    okText: '复制', cancelText: '取消',
    onOk: async () => {
      const sourceNo = formData.orderNo
      // 清空单据身份与流转字段
      formData.id = undefined
      formData.status = 0
      formData.auditorId = undefined
      formData.auditorName = ''
      formData.auditTime = ''
      formData.submitterId = undefined
      formData.submitterName = ''
      formData.submitTime = ''
      formData.printCount = 0
      formData.settledAmount = 0
      formData.receivedAmount = 0
      formData.sourceOrder = sourceNo || ''
      // 单号重新取后端号段
      try {
        formData.orderNo = await saleOrderApi.nextNo()
      } catch {
        formData.orderNo = ''
        message.warning('新单号获取失败，保存时将由后端生成')
      }
      // 退出编辑态：去掉路由上的 id，避免保存时走更新
      if (route.query.id) {
        await router.replace({ path: route.path, query: {} })
      }
      message.success('已复制为新单据，请确认后提交')
    },
  })
}

/** 导出当前单据明细为 CSV（复用通用导出工具） */
async function handleExportOrder() {
  const rows = formData.products.filter((p: any) => p.productId != null)
  if (!rows.length) {
    message.warning('当前单据没有可导出的商品明细')
    return
  }
  const headers = [
    '商品名称', '货号', '条码', '规格', '计价单位', '数量',
    '单价', '金额', '折扣(%)', '折后单价', '折后金额', '备注',
  ]
  const data = rows.map((p: any) => [
    p.productName ?? '', p.itemCode ?? '', p.barcode ?? '', p.specification ?? '', p.unit ?? '',
    p.quantity ?? 0, p.unitPrice ?? 0, p.amount ?? 0,
    p.discountRate ?? 0, p.discountedUnitPrice ?? 0, p.discountedAmount ?? 0, p.remark ?? '',
  ])
  await exportCsvWithLoading(headers, data, `销售订单_${formData.orderNo || '明细'}`)
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
onMounted(async () => {
  await loadFormConfig()
  await loadPrintTemplates()
  // 订金账户下拉：取真实财务账户列表
  try {
    accountOptions.value = await optionsApi.getAccounts()
  } catch { accountOptions.value = [] }
  // 录单默认值：仅在新增且字段为空时套用，不覆盖用户/详情数据
  if (optionRefs.warehouses.length === 0 || optionRefs.users.length === 0) {
    window.setTimeout(applyEntryDefaults, 600)
  } else {
    applyEntryDefaults()
  }
  // 编辑模式下不初始化空白行，等loadDetail加载真实数据
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
  }
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

/* 页面配置字段表：外层滚动，避免 a-table scroll.y 在弹窗内产生空高 */
.form-config-table-wrap {
  max-height: 420px;
  overflow-y: auto;
}
</style>
