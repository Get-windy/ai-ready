<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已进入配送执行的单据锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入配送执行，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink: 0"
      />

      <BillFormPage
        :model-value="formData"
        :mode="pageMode"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @update:model-value="handleModelUpdate"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 来源上游单据 + 货物明细 ═══
             配送单是运输执行单：货值（数量/金额/重量/体积）与商品明细归上游销售出库单，
             本单只负责「怎么运」。选中来源单据后，明细只读穿透上游，不在本单重复录入。 -->
        <template #detail-table>
          <div class="source-doc-bar">
            <span class="sd-label">来源单据</span>
            <a-select
              v-model:value="sourceDocIds"
              mode="multiple"
              size="small"
              class="sd-select"
              placeholder="选择销售出库单（可多选，数量/金额/重量/体积自动带出）"
              show-search
              allow-clear
              :disabled="isLocked"
              :loading="sourceDocLoading"
              :options="sourceDocOptions"
              :max-tag-count="3"
              option-filter-prop="label"
              @change="handleSourceDocChange"
            />
            <span
              v-if="hasSourceDocs"
              class="sd-hint"
            >
              货值由来源单据聚合，货物明细只读（来自销售出库单，单一数据源）
            </span>
            <span
              v-else
              class="sd-hint sd-hint-warn"
            >
              未选来源单据 = 临时配送，货物信息在本单录入
            </span>
          </div>
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'dispatch-order-form-columns'"
            :view-mode="isLocked || hasSourceDocs"
            @cell-change="handleCellChange"
            @open-select-modal="handleOpenProductSelectModal"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty && !readonlyDetail">
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleInsertRow(index)"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    @click="handleRemoveRow(index)"
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
                    @click="handleAddRow()"
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

        <!-- ═══ 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="配送要求等备注信息"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 商品选择弹窗 ═══ -->
      <ProductSelectModal
        v-model:open="showProductSelect"
        :multiple="true"
        @confirm="handleProductSelectConfirm"
      />

      <!-- ═══ 配置弹窗：页面配置 / 录单默认值 / 打印设置 ═══ -->
      <a-modal
        v-model:open="showFormConfig"
        title="配置"
        :width="760"
        :footer="null"
        destroy-on-close
      >
        <a-tabs
          v-model:active-key="configModalTab"
          size="small"
        >
          <a-tab-pane
            key="pageConfig"
            tab="页面配置"
          >
            <p class="config-hint">
              勾选后自动保存，该设置对所有操作员生效
            </p>
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
                    v-model:value="record.displayName"
                    size="small"
                    @blur="saveFormConfig"
                  />
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

          <a-tab-pane
            key="defaultValues"
            tab="录单默认值"
          >
            <a-form
              layout="horizontal"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
            >
              <a-form-item label="默认客户">
                <a-select
                  v-model:value="formData.defaultCustomerId"
                  show-search
                  allow-clear
                  size="small"
                  style="width: 100%"
                  :loading="loadingOptions"
                  :options="(optionRefs.customers || []).map((c: any) => ({ label: c.name, value: c.id }))"
                  @change="saveFormConfig"
                />
              </a-form-item>
              <a-form-item label="默认订单类型">
                <a-select
                  v-model:value="formData.defaultOrderType"
                  size="small"
                  style="width: 100%"
                  @change="saveFormConfig"
                >
                  <a-select-option
                    v-for="t in ORDER_TYPE_OPTIONS"
                    :key="t.value"
                    :value="t.value"
                  >
                    {{ t.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="默认优先级">
                <a-select
                  v-model:value="formData.defaultPriority"
                  size="small"
                  style="width: 100%"
                  @change="saveFormConfig"
                >
                  <a-select-option
                    v-for="p in PRIORITY_OPTIONS"
                    :key="p.value"
                    :value="p.value"
                  >
                    {{ p.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </a-tab-pane>

          <a-tab-pane
            key="printSettings"
            tab="打印设置"
          >
            <a-form
              layout="horizontal"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
            >
              <a-form-item label="打印模板">
                <a-select
                  v-model:value="formData.printTemplate"
                  size="small"
                  style="width: 100%"
                  @change="saveFormConfig"
                >
                  <a-select-option value="standard">
                    标准模板
                  </a-select-option>
                  <a-select-option value="simple">
                    简化模板
                  </a-select-option>
                  <a-select-option value="detailed">
                    详细模板
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number
                  v-model:value="formData.printCopies"
                  :precision="0"
                  :min="1"
                  size="small"
                  style="width: 100%"
                  @change="saveFormConfig"
                />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select
                  v-model:value="formData.printPaperSize"
                  size="small"
                  style="width: 100%"
                  @change="saveFormConfig"
                >
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
                <a-checkbox
                  v-model:checked="formData.printAlwaysLastTemplate"
                  @change="saveFormConfig"
                >
                  始终使用最后一次打印的模板，打印时不再选择
                </a-checkbox>
                <a-checkbox
                  v-model:checked="formData.printAfterSubmit"
                  @change="saveFormConfig"
                >
                  审核后立即打印
                </a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="dispatch-task"
    :document-id="printData.id"
    :print-data="printData"
    @print-success="handlePrintSuccess"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  ClockCircleOutlined, PrinterOutlined, SettingOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { userPageConfigApi, outboundApi } from '@/api/erp'
import { taskApi } from '@/api/dms/task'
import optionsApi from '@/api/options'
import { mdRouteApi } from '@/api/md'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'DispatchOrderForm' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 状态映射（执行态 9 → 对外三值口径） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待配送', color: 'default' },
  1: { text: '待配送', color: 'blue' },
  2: { text: '待配送', color: 'blue' },
  3: { text: '配送中', color: 'processing' },
  4: { text: '配送中', color: 'processing' },
  5: { text: '已配送', color: 'success' },
  6: { text: '已配送', color: 'success' },
  7: { text: '已取消', color: 'default' },
  8: { text: '异常', color: 'error' },
}

const ORDER_TYPE_OPTIONS = [
  { label: '销售配送', value: 1 },
  { label: '调拨', value: 2 },
  { label: '退货', value: 3 },
]
const PRIORITY_OPTIONS = [
  { label: '普通', value: 1 },
  { label: '紧急', value: 2 },
  { label: '加急', value: 3 },
]

// ═══ 明细合计 ═══
const billTotal = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (Number(p.quantity) || 0) * (Number(p.unitPrice) || 0), 0)
)
const totalQty = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (Number(p.quantity) || 0), 0)
)

function toDateStr(v: any): string {
  if (!v) return ''
  return String(v).slice(0, 10)
}

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const created: any = await taskApi.save(payload)
  if (status >= 1 && Number(created?.status) === 0 && created?.id) {
    await taskApi.audit(created.id)
  }
  return created
}

async function updateWithStatus(id: number | string, data: any) {
  const { status, ...payload } = data
  const saved: any = await taskApi.save({ ...payload, id: Number(id) })
  if (status >= 1 && Number(saved?.status) === 0 && saved?.id) {
    await taskApi.audit(saved.id)
  }
  return saved
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  effectiveMode,
  loadDetail,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
} = useBillForm({
  billPrefix: 'PSD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number | string) => {
      const data: any = await taskApi.detail(Number(id))
      return data || {}
    },
  },
  redirectPath: '/dispatch/dispatch-order/index',
  codeApiPath: '/dms/task/next-no',
  optionTypes: ['customers', 'products'],
  fields: [
    { key: 'deliveryDate', label: '指定配送日期', type: 'date', required: true },
    { key: 'orderType', label: '订单类型', type: 'select', required: true },
    { key: 'relatedOrderNo', label: '关联订单号', type: 'input', required: true },
    { key: 'customerName', label: '客户名称', type: 'input', required: true },
    { key: 'routeId', label: '配送线路', type: 'select' },
    { key: 'riderId', label: '配送司机', type: 'select' },
    { key: 'vehicleId', label: '配送车辆', type: 'select' },
    { key: 'deliverymanId', label: '送货员', type: 'select' },
    { key: 'deliveryFee', label: '配送费', type: 'number' },
    { key: 'collectOnDelivery', label: '代收货款', type: 'number' },
    { key: 'depositAmount', label: '订金金额', type: 'number' },
    { key: 'boxQuantity', label: '装箱数量', type: 'number' },
    { key: 'estimatedDistance', label: '配送里程', type: 'number' },
  ],
  productDefaults: {
    productCode: '', productName: '', barcode: '', spec: '', unit: '',
    quantity: 0, unitPrice: 0, amount: 0, weight: 0, volume: 0, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      fd.customerName = c?.name || ''
    }
    if (fieldKey === 'routeId') {
      const r = routeOptions.value.find((x: any) => x.value === val)
      fd.routeArea = (r as any)?.areaName || fd.routeArea || ''
    }
    if (fieldKey === 'orderType') {
      fd.orderType = val
    }
  },
  onDetailLoaded: (data: any, fd: Record<string, any>) => {
    // DmsTask.taskNo = 单据编号；DmsTask.orderNo = 关联订单号
    fd.orderNo = data.taskNo || ''
    fd.relatedOrderNo = data.orderNo || ''
    fd.deliveryDate = toDateStr(data.deliveryDate) || dayjs().format('YYYY-MM-DD')
    fd.deadlineTime = data.deadlineTime ? String(data.deadlineTime).slice(0, 10) : ''
    fd.status = data.status
    fd.creatorName = data.creatorName || ''
    fd.createTime = data.createTime ? dayjs(data.createTime).format('YYYY-MM-DD HH:mm') : ''
    fd.printCount = data.printCount ?? 0
    fd.sourceBillNo = data.sourceBillNo || ''
    fd.routeId = data.routeId ?? undefined
    fd.routeArea = data.routeArea || ''
    fd.riderId = data.riderId ?? undefined
    fd.riderName = data.riderName || ''
    fd.vehicleId = data.vehicleId ?? undefined
    fd.vehicleName = data.vehicleName || ''
    fd.deliverymanId = data.deliverymanId ?? undefined
    fd.deliverymanName = data.deliverymanName || ''
    fd.customerId = data.customerId ?? undefined
    fd.customerName = data.customerName || ''
    fd.customerPhone = data.customerPhone || ''
    fd.customerAddress = data.customerAddress || ''
    fd.sourceAddress = data.sourceAddress || ''
    fd.deliveryFee = Number(data.deliveryFee) || 0
    fd.collectOnDelivery = Number(data.collectOnDelivery) || 0
    fd.depositAmount = Number(data.depositAmount) || 0
    fd.boxQuantity = Number(data.boxQuantity) || 0
    fd.estimatedDistance = Number(data.estimatedDistance) || 0
    fd.remark = data.remark || ''
    // 来源上游单据（配送单表头数量/金额/重量/体积的聚合来源；有值时货物明细只读穿透）
    fd.sourceDocs = data.sourceDocs || []
    fd.products = (data.items || []).map((d: any, i: number) => ({
      id: d.id || `detail-${i}`,
      productId: d.productId,
      productCode: d.productCode || '',
      productName: d.productName || '',
      barcode: d.barcode || '',
      spec: d.spec || '',
      unit: d.unit || '',
      quantity: Number(d.quantity) || 0,
      unitPrice: Number(d.unitPrice) || 0,
      amount: Number(d.amount) || 0,
      weight: Number(d.weight) || 0,
      volume: Number(d.volume) || 0,
      remark: d.remark || '',
    }))
  },
  transformPayload: (fd: Record<string, any>) => ({
    id: fd.id || undefined,
    taskNo: fd.orderNo || undefined,
    deliveryDate: fd.deliveryDate || dayjs().format('YYYY-MM-DD'),
    orderType: Number(fd.orderType) || 1,
    priority: Number(fd.priority) || 1,
    orderNo: (fd.relatedOrderNo || '').trim(),
    sourceBillNo: fd.sourceBillNo || undefined,
    customerId: fd.customerId || undefined,
    customerName: fd.customerName || '',
    customerPhone: fd.customerPhone || undefined,
    customerAddress: fd.customerAddress || undefined,
    sourceAddress: fd.sourceAddress || undefined,
    deadlineTime: fd.deadlineTime ? `${String(fd.deadlineTime).slice(0, 10)}T00:00:00` : undefined,
    routeId: fd.routeId || undefined,
    routeArea: fd.routeArea || undefined,
    riderId: fd.riderId || undefined,
    riderName: fd.riderName || undefined,
    vehicleId: fd.vehicleId || undefined,
    vehicleName: fd.vehicleName || undefined,
    deliverymanId: fd.deliverymanId || undefined,
    deliverymanName: fd.deliverymanName || undefined,
    deliveryFee: Number(fd.deliveryFee) || 0,
    collectOnDelivery: Number(fd.collectOnDelivery) || 0,
    depositAmount: Number(fd.depositAmount) || 0,
    boxQuantity: Number(fd.boxQuantity) || 0,
    estimatedDistance: Number(fd.estimatedDistance) || 0,
    remark: fd.remark || undefined,
    // 有来源单据：表头货值由后端按来源单据聚合，货物明细由上游穿透，本单不再上报明细
    sourceDocs: (fd.sourceDocs || []).length ? fd.sourceDocs : undefined,
    items: (fd.sourceDocs || []).length ? [] : (fd.products || [])
      .filter((p: any) => (p.productName || '').trim() || Number(p.quantity) > 0)
      .map((p: any) => ({
        productId: p.productId || undefined,
        productCode: p.productCode || '',
        productName: p.productName || '',
        barcode: p.barcode || '',
        spec: p.spec || '',
        unit: p.unit || '',
        quantity: Number(p.quantity) || 0,
        unitPrice: Number(p.unitPrice) || 0,
        weight: Number(p.weight) || 0,
        volume: Number(p.volume) || 0,
        remark: p.remark || '',
      })),
  }),
})

// ═══ 初始化字段默认值 ═══
if (formData.orderType === undefined) formData.orderType = 1
if (formData.priority === undefined) formData.priority = 1
if (formData.deliveryDate === undefined || formData.deliveryDate === '') {
  formData.deliveryDate = dayjs().format('YYYY-MM-DD')
}
for (const key of ['relatedOrderNo', 'sourceBillNo', 'customerName', 'customerPhone', 'customerAddress',
  'sourceAddress', 'deadlineTime', 'routeArea', 'riderName', 'vehicleName', 'deliverymanName',
  'remark', 'creatorName', 'createTime']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['deliveryFee', 'collectOnDelivery', 'depositAmount', 'boxQuantity', 'estimatedDistance',
  'printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.defaultOrderType === undefined) formData.defaultOrderType = 1
if (formData.defaultPriority === undefined) formData.defaultPriority = 1

// ═══ 选择器数据（复用 DMS 配送查询下拉 + 线路主数据） ═══
const riderOptions = ref<any[]>([])
const vehicleOptions = ref<any[]>([])
const deliverymanOptions = ref<any[]>([])
const routeOptions = ref<any[]>([])
const optionLoading = ref(false)

async function loadDispatchOptions() {
  optionLoading.value = true
  try {
    const res: any = await taskApi.filterOptions()
    const data = res?.data ?? res ?? {}
    riderOptions.value = (data.drivers || []).map((r: any) => ({ label: r.name, value: r.id, name: r.name }))
    deliverymanOptions.value = (data.deliverymen || []).map((r: any) => ({ label: r.name, value: r.id, name: r.name }))
    vehicleOptions.value = (data.vehicles || []).map((v: any) => ({ label: v.extra ? `${v.name}(${v.extra})` : v.name, value: v.id, name: v.name }))
  } catch (e) {
    console.warn('[配送单] 配送资源下拉加载失败', e)
  } finally {
    optionLoading.value = false
  }
  try {
    const res: any = await mdRouteApi.options()
    const list = res?.data ?? res ?? []
    routeOptions.value = (list || []).map((r: any) => ({ label: r.routeName || r.name, value: r.id }))
  } catch (e) {
    console.warn('[配送单] 线路下拉加载失败', e)
  }
}

// ═══ 来源上游单据（销售出库单）═══
// 建模口径：配送单 = 运输执行单（怎么运），货值（数量/金额/重量/体积）与商品明细归上游单据。
//  · 选中来源单据 → 表头货值由后端按来源单据聚合，货物明细只读穿透上游（不再手工录入）
//  · 未选来源单据 → 临时配送，货物信息在本单录入（定位为装载/货物描述）
const sourceDocIds = ref<any[]>([])
const sourceDocOptions = ref<any[]>([])
const sourceDocLoading = ref(false)
const sourceDocs = computed<any[]>(() => formData.sourceDocs || [])
const hasSourceDocs = computed(() => sourceDocs.value.length > 0)
/** 货物明细只读：单据已进入执行，或货值/明细来自上游单据 */
const readonlyDetail = computed(() => isLocked.value || hasSourceDocs.value)

watch(sourceDocs, (list) => {
  const ids = (list || []).map((d: any) => d.docId).filter((v: any) => v !== null && v !== undefined)
  if (ids.join(',') !== sourceDocIds.value.join(',')) sourceDocIds.value = ids
}, { immediate: true, deep: true })

async function loadSourceDocOptions() {
  sourceDocLoading.value = true
  try {
    const res: any = await outboundApi.page({ pageNum: 1, pageSize: 200 } as any)
    const data = res?.data ?? res ?? {}
    const list = data.records || data.list || []
    sourceDocOptions.value = list.map((o: any) => ({
      label: `${o.outboundNo}｜${o.customerName || ''}｜${Number(o.totalAmount || 0).toFixed(2)}`,
      value: o.id,
      raw: o,
    }))
  } catch (e) {
    console.warn('[配送单] 来源单据候选加载失败', e)
  } finally {
    sourceDocLoading.value = false
  }
}

/** 上游单据 → 关联行（数量/金额/重量/体积/箱数快照，供后端聚合） */
function toSourceDoc(o: any) {
  return {
    docType: 1,
    docId: o.id,
    docNo: o.outboundNo,
    docDate: o.outboundDate ? String(o.outboundDate).slice(0, 10) : undefined,
    customerName: o.customerName || '',
    quantity: Number(o.totalQuantity) || 0,
    amount: Number(o.totalAmount) || 0,
    weight: Number(o.totalWeight) || 0,
    volume: Number(o.totalVolume) || 0,
    boxCount: Number(o.boxCount) || 0,
  }
}

/** 穿透上游单据明细（只读展示，单一数据源） */
async function loadSourceDocItems(ids: any[]) {
  const rows: any[] = []
  for (const id of ids) {
    try {
      const res: any = await outboundApi.getItems(id)
      const list = res?.data ?? res ?? []
      ;(list || []).forEach((it: any, i: number) => rows.push({
        id: `src-${id}-${i}`,
        productId: it.productId,
        productCode: it.productCode || '',
        productName: it.productName || '',
        barcode: it.barcode || '',
        spec: it.productSpec || it.specification || it.spec || '',
        unit: it.productUnit || it.unit || '',
        quantity: Number(it.outboundQuantity ?? it.quantity) || 0,
        unitPrice: Number(it.unitPrice) || 0,
        amount: Number(it.lineAmount ?? it.amount) || 0,
        weight: Number(it.weight) || 0,
        volume: Number(it.volume) || 0,
        remark: it.remark || '',
      }))
    } catch (e) {
      console.warn('[配送单] 上游单据明细加载失败', id, e)
    }
  }
  formData.products = rows
}

/** 选中/取消来源单据：刷新关联行 + 回填收货信息 + 穿透明细 */
async function handleSourceDocChange(ids: any[]) {
  sourceDocIds.value = ids || []
  const picked = sourceDocOptions.value
    .filter(o => sourceDocIds.value.includes(o.value))
    .map(o => (o as any).raw)
  formData.sourceDocs = picked.map(toSourceDoc)
  formData.sourceBillNo = picked.map((o: any) => o.outboundNo).join(',')
  if (!picked.length) {
    formData.products = []
    return
  }
  const first = picked[0]
  // 关联订单号 / 客户 / 收货信息从上游单据带出（配送单不重复录入）
  formData.relatedOrderNo = first.orderNo || formData.relatedOrderNo
  formData.customerId = first.customerId ?? formData.customerId
  formData.customerName = first.customerName || formData.customerName
  formData.customerPhone = first.receiverPhone || formData.customerPhone
  formData.customerAddress = first.shippingAddress || formData.customerAddress
  await loadSourceDocItems(picked.map((o: any) => o.id))
}

// ═══ 商品选择 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)

function handleOpenProductSelectModal(_record: any, rowIndex: number, fieldKey: string) {
  if (isLocked.value) return
  if (fieldKey === 'productName' || fieldKey === 'productId') {
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length
  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) handleAddProduct()
  }
  products.forEach((p: any, i: number) => {
    const row = formData.products[startIndex + i]
    if (!row) return
    const master = optionRefs.products.find((x: any) => x.id === p.id)
    row.productId = p.id
    row.productCode = p.code || master?.code || ''
    row.productName = p.name || master?.name || ''
    row.barcode = p.barcode || master?.barcode || ''
    row.spec = p.specification || master?.specification || ''
    row.unit = p.unit || master?.unit || ''
    row.unitPrice = Number(p.salePrice ?? master?.salePrice ?? 0)
    row.weight = Number(master?.weight ?? 0)
    row.volume = Number(master?.volume ?? 0)
    if (!row.quantity) row.quantity = 1
    row.amount = round4((Number(row.quantity) || 0) * (Number(row.unitPrice) || 0))
  })
  showProductSelect.value = false
  currentSelectRowIndex.value = -1
}

// ═══ 明细列（10 列 + 序号 + 操作） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'input', width: 220, searchable: true, showScanToggle: true },
  { key: 'productCode', title: '货号', type: 'input', width: 130 },
  { key: 'barcode', title: '条码', type: 'input', width: 140 },
  { key: 'spec', title: '规格', type: 'input', width: 120 },
  { key: 'unit', title: '单位', type: 'input', width: 70 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 4 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 4 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 4, readonly: true },
  { key: 'weight', title: '重量（kg）', type: 'number', width: 100, precision: 4 },
  { key: 'volume', title: '体积（m³）', type: 'number', width: 100, precision: 4 },
  { key: 'remark', title: '明细备注', type: 'input', width: 160 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: round4(totalQty.value), highlight: true },
  { key: 'amount', value: round4(billTotal.value), highlight: true },
])

function round4(val: number): number {
  return Math.round((Number(val) || 0) * 10000) / 10000
}

// ═══ 明细行操作 ═══
// 有来源上游单据时货物明细只读（来自销售出库单），禁止在本单增删行
function handleAddRow() {
  if (hasSourceDocs.value) {
    message.info('货物明细来自来源单据，只读不可增删；如需调整请在上游单据修改')
    return
  }
  handleAddProduct()
}
function handleRemoveRow(index: number) {
  if (hasSourceDocs.value) return
  handleRemoveProduct(index)
}
function handleInsertRow(index: number) {
  if (hasSourceDocs.value) return
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    record.amount = round4((Number(record.quantity) || 0) * (Number(record.unitPrice) || 0))
  }
  if (fieldKey === 'amount') {
    const qty = Number(record.quantity) || 0
    if (qty > 0) record.unitPrice = round4((Number(value) || 0) / qty)
  }
}

// ═══ 模式与状态 ═══
const pageMode = computed<'create' | 'edit' | 'view'>(() => {
  if (route.query.mode === 'view') return 'view'
  return effectiveMode.value
})
const currentStatus = computed(() => formData.status)
const statusText = computed(() => STATUS_MAP[Number(currentStatus.value)]?.text || '新建')
const statusColor = computed(() => STATUS_MAP[Number(currentStatus.value)]?.color || 'default')
const isLocked = computed(() =>
  pageMode.value === 'view' ||
  (effectiveMode.value === 'edit' && ![0, 1].includes(Number(currentStatus.value)))
)

// ═══ 页眉 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '配送单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（页面配置可显隐） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  // 基本信息
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 200, disabled: true, row: 1 },
  { key: 'deliveryDate', label: '指定配送日期', type: 'date', required: true, inlineLabel: true, width: 160, row: 1 },
  { key: 'orderType', label: '订单类型', type: 'select', required: true, inlineLabel: true, width: 130, row: 1, options: ORDER_TYPE_OPTIONS },
  { key: 'priority', label: '优先级', type: 'select', inlineLabel: true, width: 110, row: 1, options: PRIORITY_OPTIONS },
  // 来源单据
  { key: 'relatedOrderNo', label: '关联订单号', type: 'input', required: true, inlineLabel: true, width: 180, row: 2 },
  { key: 'sourceBillNo', label: '来源单据编号', type: 'input', inlineLabel: true, width: 180, row: 2 },
  { key: 'sourceAddress', label: '发货地址', type: 'input', inlineLabel: true, width: 260, row: 2 },
  // 收货信息
  { key: 'customerId', label: '客户', type: 'select', inlineLabel: true, width: 200, searchBtn: '+Q', loading: true, row: 3 },
  { key: 'customerName', label: '客户名称', type: 'input', required: true, inlineLabel: true, width: 200, row: 3 },
  { key: 'customerPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 150, row: 3 },
  { key: 'customerAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 300, row: 4 },
  { key: 'deadlineTime', label: '要求送达时间', type: 'date', inlineLabel: true, width: 160, row: 4 },
  // 配送资源
  { key: 'routeId', label: '配送线路', type: 'select', inlineLabel: true, width: 160, row: 5 },
  { key: 'routeArea', label: '配送区域', type: 'input', inlineLabel: true, width: 160, row: 5 },
  { key: 'riderId', label: '配送司机', type: 'select', inlineLabel: true, width: 140, row: 5 },
  { key: 'vehicleId', label: '配送车辆', type: 'select', inlineLabel: true, width: 160, row: 6 },
  { key: 'deliverymanId', label: '送货员', type: 'select', inlineLabel: true, width: 140, row: 6 },
  // 费用
  { key: 'deliveryFee', label: '配送费', type: 'number', inlineLabel: true, width: 120, precision: 2, row: 6 },
  { key: 'collectOnDelivery', label: '代收货款', type: 'number', inlineLabel: true, width: 130, precision: 2, row: 7 },
  { key: 'depositAmount', label: '订金金额', type: 'number', inlineLabel: true, width: 130, precision: 2, row: 7 },
  { key: 'boxQuantity', label: '装箱数量', type: 'number', inlineLabel: true, width: 120, precision: 4, row: 7 },
  { key: 'estimatedDistance', label: '配送里程(km)', type: 'number', inlineLabel: true, width: 130, precision: 4, row: 7 },
  // 审计
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100, row: 8 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 180, disabled: true, row: 8 },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90, row: 8 },
]

const DEFAULT_HIDDEN_FIELDS = ['sourceAddress', 'sourceBillNo', 'depositAmount', 'boxQuantity', 'estimatedDistance']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'dispatch-order'
const FORM_CONFIG_PAGE = 'form'

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
        defaultCustomerId: parsed.defaults.defaultCustomerId,
        defaultOrderType: parsed.defaults.defaultOrderType ?? 1,
        defaultPriority: parsed.defaults.defaultPriority ?? 1,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultCustomerId: formData.defaultCustomerId,
      defaultOrderType: formData.defaultOrderType,
      defaultPriority: formData.defaultPriority,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

const basicInfoFields = computed<BasicInfoField[]>(() => {
  const dynamic: Record<string, any> = {
    customerId: (optionRefs.customers || []).map((c: any) => ({ label: c.code ? `${c.name}(${c.code})` : c.name, value: c.id })),
    routeId: routeOptions.value,
    riderId: riderOptions.value,
    vehicleId: vehicleOptions.value,
    deliverymanId: deliverymanOptions.value,
  }
  const loadingKeys = ['customerId', 'routeId', 'riderId', 'vehicleId', 'deliverymanId']
  return ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      disabled: f.disabled || isLocked.value,
      options: dynamic[f.key] ?? f.options,
      loading: loadingKeys.includes(f.key) ? (loadingOptions.value || optionLoading.value) : f.loading,
      searchBtn: f.searchBtn,
    }))
})

const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
)

const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 130 },
  { title: '显示名', key: 'displayName', width: 170 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}

function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 摘要 / 页脚 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '货品金额', value: `¥${billTotal.value.toFixed(2)}`, statusLabel: statusText.value, divider: false },
  { label: '配送费', value: `¥${(Number(formData.deliveryFee) || 0).toFixed(2)}` },
  { label: '代收货款', value: `¥${(Number(formData.collectOnDelivery) || 0).toFixed(2)}` },
  {
    label: '应收合计',
    value: `¥${(billTotal.value + (Number(formData.deliveryFee) || 0)).toFixed(2)}`,
    divider: true,
  },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '货品金额',
  amountValue: `¥${billTotal.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : (Number(currentStatus.value) === 0 ? '审核' : '保存'),
  primaryBtnAudit: Number(currentStatus.value) === 0,
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 事件 ═══
function handleModelUpdate(val: Record<string, any>) {
  Object.assign(formData, val)
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  // 配送资源：把名称快照回写到表单（司机与送货员是两个角色）
  if (fieldKey === 'riderId') {
    formData.riderName = riderOptions.value.find((r: any) => r.value === val)?.name || ''
  }
  if (fieldKey === 'vehicleId') {
    formData.vehicleName = vehicleOptions.value.find((v: any) => v.value === val)?.name || ''
  }
  if (fieldKey === 'deliverymanId') {
    formData.deliverymanName = deliverymanOptions.value.find((r: any) => r.value === val)?.name || ''
  }
  if (fieldKey === 'customerId') {
    const c = optionRefs.customers.find((x: any) => x.id === val)
    formData.customerName = c?.name || formData.customerName
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'customerId') {
    message.info('客户快速查询请在客户档案中检索')
    return
  }
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSubmit()
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/dispatch/dispatch-order/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    default:
      message.info(`${actionKey} 功能暂不可用`)
  }
}

function handlePrint() {
  if (!formData.id) {
    message.warning('请先保存配送单后再打印')
    return
  }
  printCurrent()
}

// ═══ 打印（按单据打印） ═══
// 配送任务单：后端已为 pageCode='dispatch-task' 登记装配器并有已发布模板，
// 页面只给单据主键 —— 取数 / 挑模板 / 渲染都在服务端。
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})

function printCurrent() {
  if (!formData.id) {
    message.warning('请先保存单据后再打印')
    return
  }
  printData.value = { id: formData.id }
  printDialogRef.value?.open?.()
}

/** 打印成功 → 回写打印次数（次数只在真的出纸之后加） */
async function handlePrintSuccess() {
  if (!formData.id) return
  try {
    await taskApi.print(Number(formData.id))
    formData.printCount = (Number(formData.printCount) || 0) + 1
  } catch (e) {
    console.warn('[配送单] 打印次数回写失败', e)
  }
}

function formatNow(): string {
  return dayjs().format('YYYY-MM-DD HH:mm')
}

function handleError(err: any) {
  console.warn('[配送单] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
onMounted(async () => {
  loadFormConfig()
  loadDispatchOptions()
  loadSourceDocOptions()

  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 3; i++) handleAddRow()
    // 应用录单默认值
    if (formData.defaultCustomerId) {
      formData.customerId = formData.defaultCustomerId
      const c = optionRefs.customers.find((x: any) => x.id === formData.defaultCustomerId)
      if (c) formData.customerName = c.name || ''
    }
    if (formData.defaultOrderType) formData.orderType = formData.defaultOrderType
    if (formData.defaultPriority) formData.priority = formData.defaultPriority
  }
  if (formData.creatorName === '') formData.creatorName = currentUserName.value || ''

  // 列表「打印(F8)」跳转：?id=xx&print=1 → 打开后直接打印
  const printId = route.query.print
  const rid = route.query.id
  if (printId && rid) {
    setTimeout(() => {
      printCurrent()
    }, 600)
  }
})

// 同路由切换 ?id 时重载详情
watch(() => route.query.id, (val) => {
  if (val && effectiveMode.value === 'edit') {
    loadDetail(val as string)
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
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }

/* ═══ 来源单据栏（配送单=运输执行单：货值与商品明细归上游销售出库单） ═══ */
.source-doc-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-bottom: none;
  font-size: 12px;
  flex-wrap: wrap;
}
.sd-label { color: #333; font-weight: 600; flex-shrink: 0; }
.sd-select { min-width: 320px; }
.sd-hint { color: #999; }
.sd-hint-warn { color: #d46b08; }
</style>
