<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 非草稿状态锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批/执行流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 调拨明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.products"
            :max-height="tableMaxHeight"
            :summary-columns="tableSummaryColumns"
            :storage-key="'stock-transfer-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
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
            <template #productCell="{ record, index }">
              <a-select
                v-model:value="record.productId"
                placeholder="搜索选择商品"
                show-search
                :filter-option="filterOption"
                style="width:100%"
                :loading="loadingOptions"
                size="small"
                :disabled="isLocked"
                @change="(val: number) => handleProductChange(val, index)"
              >
                <a-select-option
                  v-for="p in optionRefs.products"
                  :key="p.id"
                  :value="p.id"
                >
                  {{ p.name }}
                </a-select-option>
              </a-select>
            </template>
          </BillDetailTable>
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
                placeholder="请输入单据备注"
                :disabled="isLocked"
              />
            </div>
            <div class="remark-row">
              <span class="remark-label">摘要</span>
              <a-input
                v-model:value="formData.summary"
                size="small"
                class="remark-input"
                placeholder="请输入摘要"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              调拨方式 <a-tag>{{ transferTypeText }}</a-tag>
            </span>
            <span class="doc-info-item">
              经手人 <a-tag color="blue">{{ formData.handlerName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.createByName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.posterTime" class="doc-info-item">记账时间 {{ formData.posterTime }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗（齿轮图标触发）：页面配置/录单默认值/打印设置 ═══ -->
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
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox
                    :checked="record.visible"
                    @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)"
                  />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <!-- Tab 2: 录单默认值 -->
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认出库仓库">
                <a-select v-model:value="formData.defaultFromWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.warehouseName||w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认入库仓库">
                <a-select v-model:value="formData.defaultToWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.warehouseName||w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认调拨方式">
                <a-select v-model:value="formData.defaultTransferType" size="small" style="width:100%" :options="TRANSFER_TYPE_OPTIONS" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <!-- Tab 3: 打印设置 -->
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="standard">标准模板</a-select-option>
                  <a-select-option value="simple">简化模板</a-select-option>
                  <a-select-option value="detailed">详细模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="A4">A4</a-select-option>
                  <a-select-option value="A5">A5</a-select-option>
                  <a-select-option value="B5">B5</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印选项">
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板，打印时不再选择</a-checkbox>
                <a-checkbox v-model:checked="formData.printAfterSubmit" @change="saveFormConfig">记账后立即打印</a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  PrinterOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  ImportOutlined,
  DownloadOutlined,
  AuditOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { stockTransferApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'StockTransferForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)
const tableMaxHeight = ref(400)

// ════════════════════════════════════════════
// 调拨方式字典
// ════════════════════════════════════════════

const TRANSFER_TYPE_MAP: Record<number, string> = {
  1: '同价调拨',
  2: '异价调拨',
}
const TRANSFER_TYPE_OPTIONS = Object.entries(TRANSFER_TYPE_MAP).map(([k, v]) => ({
  label: v,
  value: Number(k),
}))

// ════════════════════════════════════════════
// 状态枚举
// ════════════════════════════════════════════

const TRANSFER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已拒绝', color: 'error' },
  4: { text: '调拨中', color: 'processing' },
  5: { text: '已完成', color: 'success' },
  6: { text: '已取消', color: 'default' },
}

function getAvailableActions(status: number) {
  switch (status) {
    case 0: return [
      { key: 'submit', label: '记帐', icon: CheckOutlined },
    ]
    case 2: return [
      { key: 'execute', label: '执行调拨', icon: AuditOutlined },
    ]
    default: return []
  }
}

// ════════════════════════════════════════════
// useBillForm composable
// ════════════════════════════════════════════

async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await stockTransferApi.create(payload)
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await stockTransferApi.submit(created.id)
    await stockTransferApi.approve(created.id)
    await stockTransferApi.execute(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await stockTransferApi.update(id, payload)
  if (status === 1) {
    await stockTransferApi.submit(id)
    await stockTransferApi.approve(id)
    await stockTransferApi.execute(id)
  }
  return res?.data || res
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
} = useBillForm({
  billPrefix: 'DB',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await stockTransferApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        orderNo: data.transferNo || '',
        date: data.billDate || '',
        fromWarehouseId: data.fromWarehouseId,
        fromWarehouseName: data.fromWarehouseName || '',
        toWarehouseId: data.toWarehouseId,
        toWarehouseName: data.toWarehouseName || '',
        handlerId: data.applicantId || undefined,
        handlerName: data.handlerName || data.applicantName || '',
        deptId: data.departmentId || undefined,
        deptName: data.departmentName || '',
        transferType: data.transferType ?? 1,
        createByName: data.createByName || '',
        posterTime: data.posterTime || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || d.itemCode || '',
          productCode: d.productCode || '',
          productName: d.productName || '',
          barcode: d.barcode || '',
          specification: d.productSpec || d.specification || '',
          model: d.model || '',
          origin: d.origin || '',
          brand: d.brand || '',
          region: d.region || '',
          locationOut: d.locationOut || '',
          locationIn: d.locationIn || '',
          unit: d.productUnit || d.unit || '',
          smallUnit: d.smallUnit || '',
          availableStock: d.availableStock ?? 0,
          availableStockConverted: d.availableStockConverted ?? 0,
          bookStock: d.bookStock ?? 0,
          batchCode: d.batchCode || '',
          batchNo: d.batchNo || '',
          productionDate: d.productionDate || '',
          shelfLife: d.shelfLife || '',
          expiryDate: d.validityDate || d.expiryDate || '',
          quantity: d.quantity ?? 0,
          conversionRelation: d.conversionRelation || '',
          conversionResult: d.conversionResult ?? 0,
          pieceQuantity: d.pieceQuantity ?? 0,
          bigPack: d.bigPack ?? 0,
          midPack: d.midPack ?? 0,
          smallPack: d.smallPack ?? 0,
          smallUnitPrice: d.smallUnitPrice ?? 0,
          smallUnitQuantity: d.smallUnitQuantity ?? 0,
          costPrice: d.unitCost ?? 0,
          costAmount: d.costAmount ?? 0,
          transferPrice: d.transferPrice ?? 0,
          transferAmount: d.transferAmount ?? 0,
          transferDiff: d.transferDiff ?? 0,
          retailPrice: d.retailPrice ?? 0,
          wholesalePrice: d.wholesalePrice ?? 0,
          weight: d.weight ?? 0,
          volume: d.volume ?? 0,
          extNum1: d.extNum1 ?? 0,
          extNum2: d.extNum2 ?? 0,
          extNum3: d.extNum3 ?? 0,
          extText1: d.extText1 || '',
          extText2: d.extText2 || '',
          docCustom1: d.docCustom1 ?? 0,
          docCustom2: d.docCustom2 ?? 0,
          docCustom3: d.docCustom3 ?? 0,
          docCustom4: d.docCustom4 || '',
          docCustom5: d.docCustom5 || '',
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/erp/stock-transfer',
  codeApiPath: '/erp/stock/transfer/next-no',
  optionTypes: ['warehouses', 'users', 'products'],
  fields: [
    { key: 'fromWarehouseId', label: '出库仓库', type: 'select', required: true },
    { key: 'toWarehouseId', label: '入库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
  ],
  productDefaults: {
    itemCode: '', productCode: '', productName: '', barcode: '', specification: '',
    model: '', origin: '', brand: '', region: '',
    locationOut: '', locationIn: '', unit: '', smallUnit: '',
    availableStock: 0, availableStockConverted: 0, bookStock: 0,
    batchCode: '', batchNo: '', productionDate: '', shelfLife: '', expiryDate: '',
    quantity: 0, conversionRelation: '', conversionResult: 0, pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0, smallUnitPrice: 0, smallUnitQuantity: 0,
    costPrice: 0, costAmount: 0, transferPrice: 0, transferAmount: 0, transferDiff: 0,
    retailPrice: 0, wholesalePrice: 0, weight: 0, volume: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extText1: '', extText2: '',
    docCustom1: 0, docCustom2: 0, docCustom3: 0, docCustom4: '', docCustom5: '',
    remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'fromWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.fromWarehouseName = w?.warehouseName || w?.name || ''
    }
    if (fieldKey === 'toWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.toWarehouseName = w?.warehouseName || w?.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
  },
  transformPayload: (fd, status): any => ({
    status,
    transferType: fd.transferType ?? 1,
    fromWarehouseId: fd.fromWarehouseId,
    fromWarehouseName: fd.fromWarehouseName,
    toWarehouseId: fd.toWarehouseId,
    toWarehouseName: fd.toWarehouseName,
    applicantId: fd.handlerId,
    applicantName: fd.handlerName,
    handlerName: fd.handlerName,
    departmentId: fd.deptId || undefined,
    departmentName: fd.deptName || undefined,
    billDate: fd.date || undefined,
    sourceBillNo: fd.sourceBillNo || undefined,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    createByName: fd.createByName || currentUserName.value || undefined,
    totalQuantity: totalQuantity.value,
    totalAmount: totalTransferAmount.value,
    totalCostAmount: totalCostAmount.value,
    totalTransferDiff: totalTransferDiff.value,
    totalWeight: totalWeight.value,
    totalVolume: totalVolume.value,
    items: (fd.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      image: p.image || undefined,
      model: p.model || undefined,
      origin: p.origin || undefined,
      brand: p.brand || undefined,
      region: p.region || undefined,
      locationOut: p.locationOut || undefined,
      locationIn: p.locationIn || undefined,
      barcode: p.barcode || undefined,
      shelfLife: p.shelfLife || undefined,
      conversionRelation: p.conversionRelation || undefined,
      conversionResult: p.conversionResult ?? 0,
      pieceQuantity: p.pieceQuantity || 0,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      smallUnit: p.smallUnit || undefined,
      smallUnitPrice: p.smallUnitPrice || 0,
      smallUnitQuantity: p.smallUnitQuantity || 0,
      batchCode: p.batchCode || undefined,
      batchNo: p.batchNo || undefined,
      productionDate: p.productionDate || undefined,
      validityDate: p.expiryDate || undefined,
      quantity: p.quantity,
      unitCost: p.costPrice || 0,
      costAmount: p.costAmount ?? (p.quantity || 0) * (p.costPrice || 0),
      transferPrice: p.transferPrice || 0,
      transferAmount: p.transferAmount ?? (p.quantity || 0) * (p.transferPrice || 0),
      transferDiff: p.transferDiff ?? ((p.quantity || 0) * (p.transferPrice || 0) - (p.quantity || 0) * (p.costPrice || 0)),
      weight: p.weight ?? 0,
      volume: p.volume ?? 0,
      retailPrice: p.retailPrice ?? 0,
      wholesalePrice: p.wholesalePrice ?? 0,
      extNum1: p.extNum1 ?? 0,
      extNum2: p.extNum2 ?? 0,
      extNum3: p.extNum3 ?? 0,
      extText1: p.extText1 || undefined,
      extText2: p.extText2 || undefined,
      docCustom1: p.docCustom1 ?? 0,
      docCustom2: p.docCustom2 ?? 0,
      docCustom3: p.docCustom3 ?? 0,
      docCustom4: p.docCustom4 || undefined,
      docCustom5: p.docCustom5 || undefined,
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化调拨单默认值
if (formData.transferType === undefined) formData.transferType = 1
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'fromWarehouseName', 'toWarehouseName', 'handlerName', 'deptName', 'summary',
  'remark', 'createTime', 'createByName', 'posterTime', 'printCount', 'sourceBillNo',
  'defaultFromWarehouseId', 'defaultToWarehouseId', 'defaultHandlerId', 'printTemplate', 'printPaperSize',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.defaultTransferType === undefined) formData.defaultTransferType = 1
if (formData.status === undefined) formData.status = 0

// ════════════════════════════════════════════
// 计算属性
// ════════════════════════════════════════════

const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => TRANSFER_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => TRANSFER_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)
const transferTypeText = computed(() => TRANSFER_TYPE_MAP[formData.transferType] || '同价调拨')
const availableActions = computed(() => getAvailableActions(currentStatus.value))

const totalCostAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.quantity || 0) * (p.costPrice || 0)), 0)
)
const totalTransferAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.quantity || 0) * (p.transferPrice || 0)), 0)
)
const totalTransferDiff = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (((p.quantity || 0) * (p.transferPrice || 0)) - ((p.quantity || 0) * (p.costPrice || 0))), 0)
)
const totalWeight = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.weight || 0) * (p.quantity || 0)), 0)
)
const totalVolume = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.volume || 0) * (p.quantity || 0)), 0)
)

// ════════════════════════════════════════════
// 页眉配置
// ════════════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '调拨单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'export', label: '导出', icon: DownloadOutlined },
    ...availableActions.value.map((act: any) => ({
      key: act.key,
      label: act.label,
      icon: act.icon,
    })),
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ════════════════════════════════════════════
// 基本信息字段（页面对标文档13字段）
// ════════════════════════════════════════════

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'fromWarehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'toWarehouseId', label: '入库仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'transferType', label: '调拨方式', type: 'select', inlineLabel: true, width: 180, options: TRANSFER_TYPE_OPTIONS },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'createByName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'totalAmount', label: '本单金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
]

// 默认隐藏非核心字段，避免与底部单据信息区重复显示
const DEFAULT_HIDDEN_FIELDS = [
  'deptId', 'summary', 'remark', 'createByName', 'createTime', 'printCount', 'totalAmount',
]
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'stock-transfer-form'
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
        defaultFromWarehouseId: parsed.defaults.defaultFromWarehouseId,
        defaultToWarehouseId: parsed.defaults.defaultToWarehouseId,
        defaultHandlerId: parsed.defaults.defaultHandlerId,
        defaultTransferType: parsed.defaults.defaultTransferType ?? 1,
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
      defaultFromWarehouseId: formData.defaultFromWarehouseId,
      defaultToWarehouseId: formData.defaultToWarehouseId,
      defaultHandlerId: formData.defaultHandlerId,
      defaultTransferType: formData.defaultTransferType,
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

// ── 部门下拉选项 ──
const departmentOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
}

// 实际渲染的基本信息字段：过滤隐藏项 + 注入动态选项/加载态
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'fromWarehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
        : f.key === 'toWarehouseId'
          ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
          : f.key === 'handlerId'
            ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
            : f.key === 'deptId'
              ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
              : (f as any).options,
      loading: (f.key === 'fromWarehouseId' || f.key === 'toWarehouseId' || f.key === 'handlerId' || f.key === 'deptId')
        ? loadingOptions.value
        : (f as any).loading,
      searchBtn: (f.key === 'fromWarehouseId' || f.key === 'toWarehouseId' || f.key === 'handlerId' || f.key === 'deptId')
        ? '+Q'
        : (f as any).searchBtn,
    }))
)

// 页面配置弹窗字段（含显隐状态）
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
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
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

// ════════════════════════════════════════════
// 摘要面板（仅本单金额/差额）
// ════════════════════════════════════════════

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '调拨数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '成本金额', value: totalCostAmount.value.toFixed(2) },
  { label: '调拨金额', value: totalTransferAmount.value.toFixed(2), divider: true },
  { label: '调拨差额', value: totalTransferDiff.value.toFixed(2) },
  { label: '本单金额', value: totalTransferAmount.value.toFixed(2) },
  { label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
  { label: '总体积(m³)', value: totalVolume.value.toFixed(2) },
])

// ════════════════════════════════════════════
// 页脚
// ════════════════════════════════════════════

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalTransferAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? '记帐' : (currentStatus.value === 0 ? '记帐' : '记帐'),
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ════════════════════════════════════════════
// 明细表格列（默认23列 · 全部60列可配置）
// ════════════════════════════════════════════

const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220, showScanToggle: true },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 100, defaultHidden: true },
  { key: 'locationOut', title: '出库货位', type: 'input', width: 100, defaultHidden: true },
  { key: 'locationIn', title: '入库货位', type: 'input', width: 100, defaultHidden: true },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 140, precision: 2, readonly: true, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 2, readonly: true, defaultHidden: true },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 100 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'conversionResult', title: '换算结果', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'transferPrice', title: '调拨单价', type: 'number', width: 100, precision: 2 },
  { key: 'transferAmount', title: '调拨金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'transferDiff', title: '调拨差额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'restaurant', title: '餐饮店', type: 'checkbox', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'docCustom1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'docCustom2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'docCustom3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'docCustom4', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'docCustom5', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'extNum1', title: '表体自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'extNum2', title: '表体自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'extNum3', title: '表体自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'extText1', title: '表体自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'extText2', title: '表体自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'costAmount', value: Number(totalCostAmount.value.toFixed(2)) },
  { key: 'transferAmount', value: Number(totalTransferAmount.value.toFixed(2)), highlight: true },
  { key: 'transferDiff', value: Number(totalTransferDiff.value.toFixed(2)) },
] as { key: string; value: number; highlight?: boolean }[])

// ════════════════════════════════════════════
// 事件处理
// ════════════════════════════════════════════

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.productCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.model = p.model || ''
    row.origin = p.origin || ''
    row.brand = p.brand || ''
    row.unit = p.unit || ''
    row.smallUnit = p.smallUnit || ''
    row.batchCode = p.batchCode || ''
    row.costPrice = p.costPrice ?? p.purchasePrice ?? 0
    // 同价调拨默认调拨单价=成本单价；异价时可改
    row.transferPrice = (formData.transferType === 2 ? row.transferPrice : row.costPrice) || row.costPrice || 0
    row.costAmount = (row.quantity || 0) * (row.costPrice || 0)
    row.transferAmount = (row.quantity || 0) * (row.transferPrice || 0)
    row.transferDiff = (row.transferAmount || 0) - (row.costAmount || 0)
    // 联动真实库存：出库仓库 + 商品 -> 可用库存/账面库存
    refreshRowStock(row)
  }
}

/** 按 (出库仓库 + 商品) 拉取真实库存回填可用/账面库存 */
function refreshRowStock(row: any) {
  if (!formData.fromWarehouseId || !row.productId) {
    row.availableStock = 0
    row.bookStock = 0
    return
  }
  request.get(`/erp/stock/${row.productId}/${formData.fromWarehouseId}`).then((stockRes: any) => {
    const stock = stockRes?.data || stockRes
    if (stock && stock.quantity !== undefined) {
      row.availableStock = Number(stock.quantity) || 0
      row.bookStock = Number(stock.quantity) || 0
      row.availableStockConverted = Number(stock.quantity) || 0
    }
  }).catch(() => {
    // 库存记录不存在或查询失败时保持默认 0
    row.availableStock = 0
  })
}

// 出库仓库变更时刷新所有已选商品行的可用库存
watch(() => formData.fromWarehouseId, () => {
  if (isLocked.value) return
  formData.products.forEach((row: any) => {
    if (row.productId) refreshRowStock(row)
  })
})

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['quantity', 'costPrice'].includes(fieldKey)) {
    record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
  }
  if (['quantity', 'transferPrice'].includes(fieldKey)) {
    record.transferAmount = (record.quantity || 0) * (record.transferPrice || 0)
  }
  if (['quantity', 'costPrice', 'transferPrice'].includes(fieldKey)) {
    record.transferDiff = (record.transferAmount || 0) - (record.costAmount || 0)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

function handlePrimarySubmit() {
  if (isLocked.value) return
  if (currentStatus.value === 0) {
    handleSubmit()
  }
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/erp/stock-transfer/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExport()
      break
    case 'submit':
      handlePrimarySubmit()
      break
    case 'execute':
      handleExecute()
      break
  }
}

// ── 打印(F8) ──
function handlePrint() {
  if (formData.orderNo) {
    window.print()
  } else {
    message.warning('请先保存单据后再打印')
  }
}

// ── 导入：读本地文本/CSV 解析商品明细 ──
function handleImport() {
  const fileInput = document.createElement('input')
  fileInput.type = 'file'
  fileInput.accept = '.csv,.txt,.xlsx'
  fileInput.onchange = () => {
    const file = fileInput.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = (e: any) => {
      const text = String(e.target?.result || '')
      parseImportText(text)
    }
    reader.readAsText(file)
  }
  fileInput.click()
}

function parseImportText(text: string) {
  const lines = text.split(/\r?\n/).map((l: string) => l.trim()).filter(Boolean)
  if (lines.length <= 1) {
    message.warning('未解析到有效明细数据')
    return
  }
  const header = lines[0].split(/[,\t]/).map((h: string) => h.trim())
  const idx = (names: string[]) => names.map((n) => header.findIndex((h) => h.includes(n)))
  const [qtyIdx, costIdx, transIdx, nameIdx] = idx(['数量', '成本单价', '调拨单价', '商品'])
  if (nameIdx < 0) {
    message.warning('导入文件需包含「商品」列')
    return
  }
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(/[,\t]/).map((c: string) => c.trim())
    const costPrice = costIdx >= 0 ? Number(cols[costIdx] || 0) : 0
    const transferPrice = transIdx >= 0 ? Number(cols[transIdx] || 0) : (formData.transferType === 2 ? 0 : costPrice)
    const quantity = qtyIdx >= 0 ? Number(cols[qtyIdx] || 0) : 1
    const row: any = {
      id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
      productId: undefined,
      productName: cols[nameIdx] || '',
      quantity,
      costPrice,
      transferPrice,
      costAmount: quantity * costPrice,
      transferAmount: quantity * transferPrice,
      transferDiff: quantity * transferPrice - quantity * costPrice,
    }
    formData.products.push({ ...row, ...productDefaultsForImport() })
  }
  message.success(`已导入 ${lines.length - 1} 行`)
  if (formData.products.length === 0) message.warning('导入数据为空')
}

function productDefaultsForImport() {
  return {
    itemCode: '', productCode: '', barcode: '', specification: '', model: '', origin: '', brand: '', region: '',
    locationOut: '', locationIn: '', unit: '', smallUnit: '', availableStock: 0, availableStockConverted: 0, bookStock: 0,
    batchCode: '', batchNo: '', productionDate: '', shelfLife: '', expiryDate: '',
    conversionRelation: '', conversionResult: 0, pieceQuantity: 0, bigPack: 0, midPack: 0, smallPack: 0,
    smallUnitPrice: 0, smallUnitQuantity: 0, retailPrice: 0, wholesalePrice: 0, weight: 0, volume: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extText1: '', extText2: '',
    docCustom1: 0, docCustom2: 0, docCustom3: 0, docCustom4: '', docCustom5: '', remark: '',
  }
}

// ── 导出：当前单据明细导出 CSV ──
function handleExport() {
  if (formData.products.length === 0) {
    message.warning('没有可导出的明细')
    return
  }
  const headers = ['商品名称', '货号', '条码', '规格', '数量', '成本单价', '成本金额', '调拨单价', '调拨金额', '调拨差额', '备注']
  const rows = formData.products
    .filter((p: any) => p.productName)
    .map((p: any) => [
      p.productName, p.itemCode || '', p.barcode || '', p.specification || '',
      p.quantity, p.costPrice, p.costAmount, p.transferPrice, p.transferAmount, p.transferDiff, p.remark || '',
    ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `调拨单明细_${formData.orderNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ── 执行调拨 ──
function handleExecute() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '执行调拨',
    content: `确认执行调拨单 ${formData.orderNo} 吗？执行后两仓库存同步变动。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await stockTransferApi.execute(id)
        message.success('调拨执行成功，库存已更新')
        await reloadDetail(id)
      } catch (err: any) {
        message.error(err?.message || '执行失败')
      }
    },
  })
}

async function reloadDetail(id: number) {
  try {
    const res: any = await stockTransferApi.getById(id)
    const data = res?.data || res || {}
    if (data.orderNo === undefined || data.orderNo === '') data.orderNo = data.transferNo || ''
    Object.assign(formData, { ...data, status: data.status, orderNo: data.orderNo || formData.orderNo })
  } catch {
    // 静默
  }
}

// ════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[调拨单] ErrorBoundary:', err)
}

// ════════════════════════════════════════════
// 生命周期
// ════════════════════════════════════════════

onMounted(async () => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
    // 经手人默认当前登录人
    if (!formData.handlerId && currentUserId.value) {
      formData.handlerId = currentUserId.value
      const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
      formData.handlerName = u?.name || currentUserName.value || ''
    }
    // 应用录单默认值
    if (formData.defaultFromWarehouseId) {
      formData.fromWarehouseId = formData.defaultFromWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultFromWarehouseId)
      if (w) formData.fromWarehouseName = w.warehouseName || w.name || ''
    }
    if (formData.defaultToWarehouseId) {
      formData.toWarehouseId = formData.defaultToWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultToWarehouseId)
      if (w) formData.toWarehouseName = w.warehouseName || w.name || ''
    }
    if (formData.defaultTransferType !== undefined) formData.transferType = formData.defaultTransferType
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
  loadFormConfig()
  loadExtraOptions()
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
  flex-wrap: wrap;
}

.config-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}
</style>
