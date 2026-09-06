<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已记账锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入记账流程，内容不可修改`"
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
        <!-- ═══ 借出明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <div class="detail-toolbar">
            <a-checkbox v-model:checked="scannerMode">扫描枪录入</a-checkbox>
            <span class="detail-toolbar-hint">借出需从可用库存批次中拣选，录入批次条码</span>
          </div>
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.products"
            :max-height="tableMaxHeight"
            :summary-columns="tableSummaryColumns"
            :storage-key="'borrow-out-form-columns'"
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
            <template #batchCodeCell="{ record }">
              <a-select
                v-model:value="record.batchCode"
                placeholder="选择批次"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :disabled="isLocked"
                :options="record.batchOptions || []"
                @change="(val: any) => handleBatchChange(val, record)"
              />
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
              经手人 <a-tag color="blue">{{ formData.handlerName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
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
              <a-form-item label="默认仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.name||w.warehouseName,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认往来单位">
                <a-select v-model:value="formData.defaultPartnerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.customers||[]).map((c:any)=>({label:c.name,value:c.id}))" @change="saveFormConfig" />
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

    <!-- ═══ 归还登记弹窗 ═══ -->
    <a-modal
      v-model:open="returnModalOpen"
      :title="`归还登记 - ${formData.orderNo || formData.id}`"
      width="760px"
      :confirm-loading="returning"
      @ok="handleReturnSubmit"
    >
      <a-form layout="inline" class="return-form">
        <a-form-item label="归还日期">
          <a-date-picker v-model:value="returnForm.returnDate" value-format="YYYY-MM-DD" style="width:150px" />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="returnForm.remark" style="width:320px" placeholder="归还备注" />
        </a-form-item>
      </a-form>
      <a-table
        :columns="returnColumns"
        :data-source="returnItems"
        :pagination="false"
        row-key="orderItemId"
        size="small"
        :locale="{ emptyText: '该单暂无明细' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="['quantity','returnedQuantity','remaining'].includes(String(column.dataIndex))">
            {{ record[String(column.dataIndex)] }}
          </template>
          <template v-else-if="column.dataIndex === 'returnQuantity'">
            <a-input-number v-model:value="record.returnQuantity" :min="0" :max="record.remaining" style="width:120px" :disabled="record.remaining <= 0" placeholder="0" />
          </template>
        </template>
      </a-table>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, PrinterOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
  ImportOutlined, DownloadOutlined, SettingOutlined, UndoOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { borrowApi, type WmsBorrowOrderItem } from '@/api/wms/borrow'
import { inventoryApi, type WmsInventory } from '@/api/wms/inventory'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'WhBorrowOutForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)
const tableMaxHeight = ref(400)
/** 扫描枪录入开关（默认关；开启时建议从商品扫描条码快速带出，数据录入仍以商品选择为准） */
const scannerMode = ref(false)

const DIRECTION_OUT = 2

// ═══ 状态枚举 ═══
const BORROW_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已记账', color: 'blue' },
  3: { text: '部分归还', color: 'gold' },
  4: { text: '已归还', color: 'green' },
  5: { text: '已取消', color: 'red' },
}

function currentOperator() {
  return {
    id: userStore?.userId || userStore?.userInfo?.id || 0,
    name: currentUserName.value || '系统',
  }
}

function toDateStr(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v.slice(0, 10)
  return String(v).slice(0, 10)
}

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await borrowApi.create({ ...payload, status: 0 })
  const created = res?.data || res
  if (status >= 1 && created?.id) {
    const u = currentOperator()
    await borrowApi.post(created.id, u.id, u.name)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await borrowApi.update(id, { ...payload, status: 0 })
  if (status >= 1) {
    const u = currentOperator()
    await borrowApi.post(id, u.id, u.name)
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
  billPrefix: 'JCD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await borrowApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        orderNo: data.orderNo || '',
        date: toDateStr(data.borrowDate) || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        warehouseId: data.warehouseId,
        warehouseName: data.warehouseName || '',
        partnerId: data.partnerId,
        partnerName: data.partnerName || '',
        partnerCode: data.partnerCode || '',
        deptId: data.deptId || undefined,
        deptName: data.deptName || '',
        expectedReturnDate: toDateStr(data.expectedReturnDate) || undefined,
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          productCode: d.productCode || '',
          productName: d.productName || '',
          specification: d.productSpec || d.specification || '',
          unit: d.unit || '',
          quantity: d.quantity ?? 0,
          price: d.price ?? 0,
          amount: d.amount ?? 0,
          barcode: d.barcode || '',
          model: d.model || '',
          origin: d.origin || '',
          region: d.region || '',
          location: d.location || '',
          batchCode: d.batchCode || '',
          taste: d.taste || '',
          bigPack: d.bigPack ?? 0,
          midPack: d.midPack ?? 0,
          smallPack: d.smallPack ?? 0,
          pieceQuantity: d.pieceQuantity ?? 0,
          smallUnit: d.smallUnit || '',
          smallUnitQuantity: d.smallUnitQuantity ?? 0,
          smallUnitPrice: d.smallUnitPrice ?? 0,
          retailPrice: d.retailPrice ?? 0,
          wholesalePrice: d.wholesalePrice ?? 0,
          minPrice: d.minPrice ?? 0,
          conversionRelation: d.conversionRelation || '',
          conversionResult: d.conversionResult ?? 0,
          availableStock: d.availableStock ?? 0,
          bookStock: d.bookStock ?? 0,
          productionDate: d.productionDate || '',
          shelfLife: d.shelfLife || '',
          expiryDate: d.expiryDate || '',
          weight: d.weight ?? 0,
          volume: d.volume ?? 0,
          itemExtNum1: d.itemExtNum1 ?? 0,
          itemExtNum2: d.itemExtNum2 ?? 0,
          itemExtNum3: d.itemExtNum3 ?? 0,
          itemExtText1: d.itemExtText1 || '',
          itemExtText2: d.itemExtText2 || '',
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/wh/borrow-out/index',
  codeApiPath: '/wms/borrow/next-no',
  optionTypes: ['warehouses', 'users', 'products', 'customers'],
  fields: [
    { key: 'partnerId', label: '往来单位', type: 'select', required: true },
    { key: 'warehouseId', label: '出库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'deptId', label: '部门', type: 'select' },
  ],
  productDefaults: {
    productCode: '', productName: '', barcode: '', specification: '', model: '', origin: '', region: '',
    location: '', unit: '', smallUnit: '', taste: '', batchCode: '', productionDate: '', shelfLife: '',
    expiryDate: '', quantity: 0, price: 0, amount: 0, bigPack: 0, midPack: 0, smallPack: 0,
    pieceQuantity: 0, conversionRelation: '', conversionResult: 0, availableStock: 0, bookStock: 0,
    retailPrice: 0, wholesalePrice: 0, minPrice: 0, smallUnitQuantity: 0, smallUnitPrice: 0,
    weight: 0, volume: 0, itemExtNum1: 0, itemExtNum2: 0, itemExtNum3: 0,
    itemExtText1: '', itemExtText2: '', remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.warehouseName || w?.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'partnerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      fd.partnerName = c?.name || ''
      fd.partnerCode = c?.code || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
  },
  transformPayload: (fd, status) => ({
    status,
    direction: DIRECTION_OUT,
    partnerId: fd.partnerId,
    partnerName: fd.partnerName,
    partnerCode: fd.partnerCode || undefined,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    deptId: fd.deptId || undefined,
    deptName: fd.deptName || undefined,
    borrowDate: fd.date || undefined,
    expectedReturnDate: fd.expectedReturnDate || undefined,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    totalQuantity: totalQuantity.value,
    borrowQuantity: totalQuantity.value,
    items: (fd.products || []).filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || p.itemCode,
      productName: p.productName,
      productSpec: p.specification,
      unit: p.unit,
      quantity: p.quantity,
      price: p.price || p.unitPrice || 0,
      amount: (p.quantity || 0) * (p.price || p.unitPrice || 0),
      barcode: p.barcode || undefined,
      model: p.model || undefined,
      origin: p.origin || undefined,
      region: p.region || undefined,
      location: p.location || undefined,
      batchCode: p.batchCode || undefined,
      taste: p.taste || undefined,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      pieceQuantity: p.pieceQuantity || 0,
      conversionRelation: p.conversionRelation || undefined,
      conversionResult: p.conversionResult ?? 0,
      availableStock: p.availableStock ?? 0,
      bookStock: p.bookStock ?? 0,
      productionDate: p.productionDate || undefined,
      shelfLife: p.shelfLife || undefined,
      expiryDate: p.expiryDate || undefined,
      retailPrice: p.retailPrice || 0,
      wholesalePrice: p.wholesalePrice || 0,
      minPrice: p.minPrice || 0,
      smallUnit: p.smallUnit || undefined,
      smallUnitQuantity: p.smallUnitQuantity || 0,
      smallUnitPrice: p.smallUnitPrice || 0,
      weight: p.weight ?? 0,
      volume: p.volume ?? 0,
      itemExtNum1: p.itemExtNum1 ?? 0,
      itemExtNum2: p.itemExtNum2 ?? 0,
      itemExtNum3: p.itemExtNum3 ?? 0,
      itemExtText1: p.itemExtText1 || undefined,
      itemExtText2: p.itemExtText2 || undefined,
      remark: p.remark || undefined,
    }) as WmsBorrowOrderItem[]),
  }),
})

// 初始化借出单默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'warehouseName', 'handlerName', 'deptName', 'partnerName', 'partnerCode', 'summary', 'remark',
  'createTime', 'creatorName', 'bookkeepingTime', 'printCount', 'defaultWarehouseId', 'defaultHandlerId',
  'defaultPartnerId', 'printTemplate', 'printPaperSize',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = 0

// ═══ 计算属性 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => BORROW_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => BORROW_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '借出单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'return', label: '归还', icon: UndoOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'export', label: '导出', icon: DownloadOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（14个） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'partnerId', label: '往来单位', type: 'select', required: true, inlineLabel: true, width: 220, searchBtn: '+Q', loading: true },
  { key: 'partnerCode', label: '往来编号', type: 'input', inlineLabel: true, width: 120, disabled: true },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 180 },
  { key: 'expectedReturnDate', label: '预计还回时间', type: 'date', inlineLabel: true, width: 180 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'totalAmount', label: '本单金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
]

const DEFAULT_HIDDEN_FIELDS = [
  'deptId', 'summary', 'remark', 'creatorName', 'createTime', 'printCount', 'totalAmount',
]
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'borrow-out-form'
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
        defaultWarehouseId: parsed.defaults.defaultWarehouseId,
        defaultHandlerId: parsed.defaults.defaultHandlerId,
        defaultPartnerId: parsed.defaults.defaultPartnerId,
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
      defaultWarehouseId: formData.defaultWarehouseId,
      defaultHandlerId: formData.defaultHandlerId,
      defaultPartnerId: formData.defaultPartnerId,
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
const departmentOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
}

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'warehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.warehouseName || w.name, value: w.id }))
        : f.key === 'handlerId'
          ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
          : f.key === 'deptId'
            ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
            : f.key === 'partnerId'
              ? (optionRefs.customers || []).map((c: any) => ({ label: c.name, value: c.id }))
              : (f as any).options,
      loading: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId' || f.key === 'partnerId')
        ? loadingOptions.value
        : (f as any).loading,
      searchBtn: (f.key === 'warehouseId' || f.key === 'handlerId' || f.key === 'deptId' || f.key === 'partnerId')
        ? '+Q'
        : (f as any).searchBtn,
    }))
)

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

// ═══ 摘要面板 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '借出数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '本单金额', value: `¥${totalAmount.value.toFixed(2)}`, divider: true },
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 明细表格列（默认20列 · 全部45列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100, defaultHidden: true },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'region', title: '区域', type: 'input', width: 100, defaultHidden: true },
  { key: 'location', title: '货位', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 90 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 4 },
  { key: 'availableStockConversion', title: '可用库存换算结果', type: 'input', width: 110, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 4, defaultHidden: true },
  { key: 'batchCode', title: '批次条码', type: 'slot', slotName: 'batchCodeCell', width: 160, required: false },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 100 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '借出数量', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'minPrice', title: '最低售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'price', title: '借出单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '借出金额', type: 'number', width: 110, precision: 2, readonly: true, defaultHidden: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel3', title: '自助vip', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel4', title: '大团餐', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel5', title: '特价客户', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel6', title: '外围餐饮店', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel7', title: '重点|vip01', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'priceLevel8', title: '连锁|vip', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'itemExtNum1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtNum2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtNum3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'itemExtText1', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'itemExtText2', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: Number(totalAmount.value.toFixed(2)), highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 事件处理 ═══
function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.productCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.model = p.model || ''
    row.origin = p.origin || ''
    row.brand = p.brand || ''
    row.unit = p.unit || ''
    row.taste = p.taste || ''
    row.retailPrice = p.retailPrice || 0
    row.wholesalePrice = p.wholesalePrice || 0
    row.weight = p.weight || 0
    row.volume = p.volume || 0
    row.shelfLife = p.shelfLife || ''
    row.price = p.purchasePrice || p.costPrice || 0
    row.amount = (row.quantity || 0) * (row.price || 0)
    ensureBatchOptions(row)
  }
}

// ═══ 批次拣选：加载商品×仓库可用库存批次，回填空缺批次，校验数量 ≤ 批次可用库存 ═══
function toBatchOptions(batches: WmsInventory[]) {
  return batches.map((b) => ({
    label: `${b.batchNo || ''} | 可用:${b.availableQuantity ?? 0} ${b.unit ? '' : ''}${b.locationCode ? ` | 货位:${b.locationCode}` : ''}`,
    value: b.batchNo,
    batchNo: b.batchNo,
    locationCode: b.locationCode,
    availableQuantity: b.availableQuantity,
    productionDate: b.productionDate,
    validityDate: b.validityDate,
  }))
}

async function ensureBatchOptions(row: any) {
  if (!row?.productId || !formData.warehouseId || isLocked.value) {
    if (row) row.batchOptions = row.batchOptions || []
    return
  }
  try {
    const res: any = await inventoryApi.batchList(row.productId, formData.warehouseId)
    const list = Array.isArray(res) ? res : (res?.data || res?.records || [])
    row.batchOptions = toBatchOptions(list)
    // 若已有批次值且在选项中则保留；否则若唯一批次自动回填
    if (!row.batchCode && row.batchOptions.length === 1) {
      handleBatchChange(row.batchOptions[0].batchNo, row)
    }
  } catch (e) {
    row.batchOptions = row.batchOptions || []
    console.warn('[借出单] 加载库存批次失败', e)
  }
}

function handleBatchChange(batchNo: any, row: any) {
  const opt = (row.batchOptions || []).find((o: any) => o.value === batchNo)
  row.batchCode = batchNo || ''
  if (opt) {
    row.location = opt.locationCode || row.location || ''
    row.availableStock = opt.availableQuantity
  } else {
    row.availableStock = row.availableStock ?? 0
  }
  // 校验借出数量不超过该批次可用库存
  if ((Number(row.quantity) || 0) > (Number(row.availableStock) || 0) && Number(row.availableStock) >= 0) {
    message.warning(`商品[${row.productName || ''}]借出数量 ${row.quantity} 超过批次可用库存 ${row.availableStock}`)
  }
}

// 仓库变化时刷新各已选商品行的库存批次
watch(() => formData.warehouseId, () => {
  ;(formData.products || []).forEach((r: any) => { if (r.productId) ensureBatchOptions(r) })
})

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (['quantity', 'price'].includes(fieldKey)) {
    record.amount = (record.quantity || 0) * (record.price || 0)
  }
  // 批次拣选校验：借出数量不得超过所选批次可用库存
  if (fieldKey === 'quantity') {
    const available = Number(record.availableStock)
    const qty = Number(record.quantity) || 0
    if (available > 0 && qty > available) {
      message.warning(`商品[${record.productName || ''}]借出数量 ${qty} 超过批次可用库存 ${available}`)
    }
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSubmit()
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/wh/borrow-out/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    case 'return':
      openReturnModal()
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExport()
      break
  }
}

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
  const [qtyIdx, priceIdx, nameIdx] = idx(['数量', '单价', '商品'])
  if (nameIdx < 0) {
    message.warning('导入文件需包含「商品」列')
    return
  }
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(/[,\t]/).map((c: string) => c.trim())
    const row: any = {
      id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
      productId: undefined,
      productName: cols[nameIdx] || '',
      quantity: qtyIdx >= 0 ? Number(cols[qtyIdx] || 0) : 1,
      price: priceIdx >= 0 ? Number(cols[priceIdx] || 0) : 0,
      amount: 0,
    }
    row.amount = (row.quantity || 0) * (row.price || 0)
    formData.products.push({ ...row, ...productDefaultsForImport() })
  }
  message.success(`已导入 ${lines.length - 1} 行`)
  if (formData.products.length === 0) message.warning('导入数据为空')
}

function productDefaultsForImport() {
  return {
    productCode: '', barcode: '', specification: '', model: '', origin: '', region: '',
    location: '', unit: '', smallUnit: '', taste: '', batchCode: '', productionDate: '', shelfLife: '',
    expiryDate: '', bigPack: 0, midPack: 0, smallPack: 0, pieceQuantity: 0, conversionRelation: '',
    conversionResult: 0, availableStock: 0, bookStock: 0, retailPrice: 0, wholesalePrice: 0,
    minPrice: 0, smallUnitQuantity: 0, smallUnitPrice: 0, weight: 0, volume: 0,
    itemExtNum1: 0, itemExtNum2: 0, itemExtNum3: 0, itemExtText1: '', itemExtText2: '', remark: '',
  }
}

// ── 导出：当前单据明细导出 CSV ──
function handleExport() {
  if (formData.products.length === 0) {
    message.warning('没有可导出的明细')
    return
  }
  const headers = ['商品名称', '货号', '条码', '规格', '借出数量', '借出单价', '借出金额', '备注']
  const rows = formData.products
    .filter((p: any) => p.productName)
    .map((p: any) => [
      p.productName, p.productCode || '', p.barcode || '', p.specification || '',
      p.quantity, p.price, p.amount, p.remark || '',
    ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `借出单明细_${formData.orderNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

// ═══ 归还登记（还回入库） ═══
const returnModalOpen = ref(false)
const returnItems = ref<any[]>([])
const returning = ref(false)
const returnForm = reactive({ returnDate: '', remark: '' })
const returnColumns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', width: 110 },
  { title: '商品名称', dataIndex: 'productName', width: 180, ellipsis: true },
  { title: '借出数量', dataIndex: 'quantity', width: 90, align: 'right' },
  { title: '已还', dataIndex: 'returnedQuantity', width: 80, align: 'right' },
  { title: '未还', dataIndex: 'remaining', width: 80, align: 'right' },
  { title: '本次归还', dataIndex: 'returnQuantity', width: 130 },
]

function openReturnModal() {
  if (!formData.id) { message.warning('请先保存并记账后再归还'); return }
  const today = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  returnForm.returnDate = `${today.getFullYear()}-${p(today.getMonth() + 1)}-${p(today.getDate())}`
  returnForm.remark = ''
  returnItems.value = (formData.products || [])
    .map((it: any) => {
      const qty = Number(it.quantity) || 0
      const returned = Number(it.returnedQuantity) || 0
      return {
        orderItemId: it.id,
        productCode: it.productCode,
        productName: it.productName,
        quantity: qty,
        returnedQuantity: returned,
        remaining: Math.max(0, qty - returned),
        returnQuantity: 0,
      }
    })
    .filter((it: any) => it.orderItemId)
  returnModalOpen.value = true
}

async function handleReturnSubmit() {
  const items = returnItems.value
    .filter((r: any) => Number(r.returnQuantity) > 0)
    .map((r: any) => ({ orderItemId: r.orderItemId, quantity: Number(r.returnQuantity) }))
  if (items.length === 0) { message.warning('请填写至少一行的本次归还数量'); return }
  returning.value = true
  try {
    const u = currentOperator()
    await borrowApi.returnOrder({
      orderId: formData.id,
      returnDate: returnForm.returnDate,
      operatorId: u.id,
      operatorName: u.name,
      remark: returnForm.remark,
      items,
    })
    message.success('归还登记成功，已还回入库')
    returnModalOpen.value = false
    reloadDetail(formData.id)
  } catch (e: any) {
    message.error(e?.response?.data?.message || '归还登记失败')
  } finally {
    returning.value = false
  }
}

async function reloadDetail(id: number) {
  try {
    const res: any = await borrowApi.getById(id)
    const data = res?.data || res || {}
    if (data.orderNo === undefined || data.orderNo === '') data.orderNo = formData.orderNo
    Object.assign(formData, { ...data, status: data.status, orderNo: data.orderNo || formData.orderNo })
  } catch {
    // 静默
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[借出单] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
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
    if (formData.defaultWarehouseId) {
      formData.warehouseId = formData.defaultWarehouseId
      const w = optionRefs.warehouses.find((x: any) => x.id === formData.defaultWarehouseId)
      if (w) formData.warehouseName = w.warehouseName || w.name || ''
    }
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
    // 编辑已保存单据：为已有商品行加载库存批次选项
    ;(formData.products || []).forEach((r: any) => { if (r.productId) ensureBatchOptions(r) })
  })
  loadFormConfig()
  loadExtraOptions()
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.detail-toolbar { display: flex; align-items: center; gap: 12px; padding-bottom: 6px; }
.detail-toolbar-hint { font-size: 12px; color: #8c8c8c; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
.return-form { margin-bottom: 12px; }
</style>
