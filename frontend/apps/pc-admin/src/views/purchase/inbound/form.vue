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
        @submit="handleSubmit"
      >
        <!-- ═══ 明细表格 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.products"
            :max-height="tableMaxHeight"
            :summary-columns="tableSummaryColumns"
            :storage-key="'purchase-inbound-form-columns'"
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

        <!-- ═══ 底部付款区 + 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="pay-area">
            <div class="pay-row">
              <span class="pay-item"><label>付款账户</label>
                <a-select v-model:value="formData.paymentAccountId" show-search allow-clear size="small" style="width:150px" placeholder="+Q" :options="(accountOptions||[]).map((a:any)=>({label:a.name,value:a.id}))" />
              </span>
              <span class="pay-item"><label>付款金额</label>
                <a-input-number v-model:value="formData.paymentAmount" size="small" :precision="2" style="width:120px" />
              </span>
              <span class="pay-item"><label>更多账户</label>
                <a-input v-model:value="formData.moreAccounts" size="small" style="width:120px" disabled />
              </span>
              <span class="pay-item"><label>此前预付</label>
                <a-input-number v-model:value="formData.prevPrepaid" size="small" :precision="2" style="width:120px" disabled />
              </span>
              <span class="pay-item"><label>使用预付款</label>
                <a-input-number v-model:value="formData.usePrepaid" size="small" :precision="2" style="width:120px" />
              </span>
              <span class="pay-item"><label>预付余额</label>
                <a-input-number v-model:value="formData.prepaidBalance" size="small" :precision="2" style="width:120px" disabled />
              </span>
            </div>
            <div class="pay-row">
              <span class="pay-item"><label>本次欠款</label>
                <a-input-number v-model:value="formData.currentDebt" size="small" :precision="2" style="width:120px" disabled />
              </span>
              <span class="pay-item"><label>此前欠款</label>
                <a-input-number v-model:value="formData.prevDebt" size="small" :precision="2" style="width:120px" disabled />
              </span>
              <span class="pay-item"><label>欠款余额</label>
                <a-input-number v-model:value="formData.debtBalance" size="small" :precision="2" style="width:120px" disabled />
              </span>
              <span class="pay-item"><label>付款期限</label>
                <a-input v-model:value="formData.paymentTerm" size="small" style="width:150px" />
              </span>
              <span class="pay-item"><label>其他费用</label>
                <a-input-number v-model:value="formData.otherExpense" size="small" :precision="2" style="width:120px" />
              </span>
              <span class="pay-item"><label>源单</label>
                <a-input v-model:value="formData.sourceBillNo" size="small" style="width:160px" />
              </span>
            </div>
          </div>
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
              <span class="remark-label">内部备注</span>
              <a-input
                v-model:value="formData.internalNote"
                size="small"
                class="remark-input"
                placeholder="内部备注（不对外）"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">
              制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.approveTime" class="doc-info-item">审核时间 {{ formData.approveTime }}</span>
            <span v-if="formData.receiveTime" class="doc-info-item">收货时间 {{ formData.receiveTime }}</span>
            <span v-if="formData.qualityCheckTime" class="doc-info-item">质检时间 {{ formData.qualityCheckTime }}</span>
            <span v-if="formData.warehouseConfirmTime" class="doc-info-item">入库确认时间 {{ formData.warehouseConfirmTime }}</span>
            <span class="doc-info-item">
              来源订单 <a-tag>{{ formData.sourceOrderNo || '-' }}</a-tag>
            </span>
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
              <a-form-item label="默认仓库">
                <a-select v-model:value="formData.defaultWarehouseId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.warehouses||[]).map((w:any)=>({label:w.name,value:w.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultBuyerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认付款账户">
                <a-select v-model:value="formData.defaultPaymentAccountId" show-search allow-clear size="small" style="width:100%" :loading="loadingOptions" :options="(accountOptions||[]).map((a:any)=>({label:a.name,value:a.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认税率">
                <a-input-number v-model:value="formData.defaultTaxRate" :precision="0" size="small" style="width:100%" @change="saveFormConfig" />
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

      <!-- ═══ 源采购订单选择弹窗 ═══ -->
      <a-modal
        v-model:open="showOrderSelect"
        title="选择来源采购订单"
        :width="900"
        :footer="null"
        destroy-on-close
      >
        <div class="order-select-search">
          <a-input
            v-model:value="orderSearchKeyword"
            placeholder="搜索订单编号/供应商"
            allow-clear
            style="width:280px"
            @press-enter="loadOrders"
          />
          <a-button type="primary" @click="loadOrders">查询</a-button>
        </div>
        <a-table
          :columns="orderSelectColumns"
          :data-source="orderList"
          :loading="orderLoading"
          :pagination="orderPagination"
          row-key="id"
          size="small"
          @change="handleOrderPageChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="confirmSourceOrder(record)">选择</a-button>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getOrderStatusColor(record.status)">{{ getOrderStatusText(record.status) }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 质检结果弹窗 ═══ -->
      <a-modal
        v-model:open="showQCModal"
        title="质检结果录入"
        :width="420"
        @ok="confirmQC"
        :confirm-loading="qcLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="质检结果" required>
            <a-select v-model:value="qcResult" placeholder="请选择质检结果">
              <a-select-option value="pass">合格</a-select-option>
              <a-select-option value="fail">不合格</a-select-option>
              <a-select-option value="partial">部分合格</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="质检说明">
            <a-textarea v-model:value="qcNote" placeholder="请输入质检说明" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 红冲确认弹窗 ═══ -->
      <a-modal
        v-model:open="showReverseModal"
        title="红冲确认"
        :width="400"
        @ok="confirmReverse"
        :confirm-loading="reverseLoading"
      >
        <a-form layout="vertical">
          <a-form-item label="红冲原因" required>
            <a-input v-model:value="reverseReason" placeholder="请输入红冲原因" />
          </a-form-item>
          <p style="color:#ff4d4f;font-size:12px;">
            红冲将生成一张负数入库单，冲减原单的库存和应付。确认执行？
          </p>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, h, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ClockCircleOutlined,
  CheckOutlined,
  CloseCircleOutlined,
  MinusCircleOutlined,
  PlusCircleOutlined,
  ImportOutlined,
  DownloadOutlined,
  SwapOutlined,
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
import { inboundApi, userPageConfigApi } from '@/api/erp'
import { PRODUCT_PURCHASE_DEFAULTS } from '@/utils/productDefaults'
import type { PurchaseInboundPayload } from '@/api/erp'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'PurchaseInboundForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

// ════════════════════════════════════════════
// 状态枚举
// ════════════════════════════════════════════

const INBOUND_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '待收货', color: 'orange' },
  4: { text: '已收货', color: 'processing' },
  5: { text: '待质检', color: 'orange' },
  6: { text: '已质检', color: 'processing' },
  7: { text: '待入库', color: 'orange' },
  8: { text: '已入库', color: 'processing' },
  9: { text: '已完成', color: 'success' },
  10: { text: '已取消', color: 'error' },
}

/**
 * 当前状态下可执行的下一个操作列表
 * 返回 { actionKey, label, icon }
 */
function getAvailableActions(status: number) {
  switch (status) {
    case 0: return [
      { key: 'submit', label: '提交审批', icon: AuditOutlined },
    ]
    case 1: return [
      { key: 'approve', label: '审核通过', icon: CheckOutlined },
      { key: 'reject', label: '驳回', icon: CloseCircleOutlined },
    ]
    case 2:
    case 3: return [
      { key: 'receive', label: '确认收货', icon: DownloadOutlined },
    ]
    case 4:
    case 5: return [
      { key: 'quality-check', label: '质检', icon: CheckOutlined },
    ]
    case 6:
    case 7: return [
      { key: 'confirm-warehouse', label: '入库确认', icon: CheckOutlined },
    ]
    case 8: return [
      { key: 'complete', label: '完成', icon: CheckOutlined },
    ]
    case 9:
    case 10: return []
    default: return []
  }
}

// ════════════════════════════════════════════
// 源单选择相关状态
// ════════════════════════════════════════════

const showOrderSelect = ref(false)
const orderLoading = ref(false)
const orderSearchKeyword = ref('')
const orderList = ref<any[]>([])
const orderPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const selectedOrder = ref<any>(null)

const orderSelectColumns = [
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 160 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '商品金额', dataIndex: 'productAmount', key: 'productAmount', width: 110, align: 'right' as const },
  { title: '本单金额', dataIndex: 'billAmount', key: 'billAmount', width: 110, align: 'right' as const },
  { title: '已收数量', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right' as const },
  { title: '未收数量', dataIndex: 'unreceiveQuantity', key: 'unreceiveQuantity', width: 90, align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 70, fixed: 'right' as const },
]

async function loadOrders() {
  orderLoading.value = true
  try {
    const params: Record<string, any> = {
      current: orderPagination.current,
      size: orderPagination.pageSize,
      status: 2, // 只查询已审批订单
    }
    if (orderSearchKeyword.value) {
      params.orderNo = orderSearchKeyword.value
      params.supplierName = orderSearchKeyword.value
    }
    const res: any = await request.get('/erp/purchase/order/page', { params })
    const data = res?.data || res || {}
    orderList.value = data?.records || []
    orderPagination.total = data?.total || 0
  } catch {
    message.error('加载采购订单失败')
    orderList.value = []
  } finally {
    orderLoading.value = false
  }
}

function handleOrderPageChange(pagination: any) {
  orderPagination.current = pagination.current || 1
  orderPagination.pageSize = pagination.pageSize || 10
  loadOrders()
}

async function confirmSourceOrder(order: any) {
  selectedOrder.value = order
  showOrderSelect.value = false

  // 填充表头信息
  formData.sourceOrderId = order.id
  formData.sourceOrderNo = order.orderNo
  formData.supplierId = order.supplierId
  formData.supplierName = order.supplierName
  formData.warehouseId = order.warehouseId
  formData.warehouseName = order.warehouseName
  formData.purchaserId = order.purchaserId || order.buyerId
  formData.purchaserName = order.purchaserName || order.buyerName
  formData.deptId = order.departmentId || order.deptId
  formData.departmentName = order.departmentName || order.deptName

  // 加载订单明细
  try {
    const res: any = await request.get(`/erp/purchase/order/${order.id}`)
    const detail = res?.data || res
    const items = detail?.items || detail?.details || []

    if (items.length > 0) {
      // 清空现有明细
      formData.products.splice(0, formData.products.length)

      items.forEach((item: any) => {
        const pendingQty = (item.quantity || 0) - (item.receivedQuantity || 0) - (item.inboundQuantity || 0)
        if (pendingQty <= 0) return // 跳过已全部入库的行
        formData.products.push({
          id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
          productId: item.productId,
          productName: item.productName || '',
          productCode: item.productCode || item.itemCode || '',
          itemCode: item.itemCode || item.productCode || '',
          barcode: item.barcode || '',
          specification: item.specification || item.productSpec || '',
          model: item.model || '',
          origin: item.origin || '',
          brand: item.brand || '',
          unit: item.unit || item.productUnit || '',
          smallUnit: item.smallUnit || '',
          batchNo: '',
          productionDate: '',
          expiryDate: '',
          shelfLife: '',
          location: '',
          quantity: pendingQty,
          unitPrice: item.unitPrice || 0,
          amount: pendingQty * (item.unitPrice || 0),
          taxRate: item.taxRate ?? 13,
          costPrice: item.costPrice || 0,
          costAmount: pendingQty * (item.costPrice || 0),
          orderItemId: item.id || item.lineNo,
          gift: item.gift || false,
          weight: item.weight || 0,
          volume: item.volume || 0,
          remark: item.remark || '',
        })
      })

      message.success(`已加载订单 ${order.orderNo}，共 ${formData.products.length} 条待入库明细`)
    }
  } catch {
    message.warning('加载订单明细失败，请手动添加商品')
  }
}

// ════════════════════════════════════════════
// 质检弹窗
// ════════════════════════════════════════════

const showQCModal = ref(false)
const qcResult = ref('pass')
const qcNote = ref('')
const qcLoading = ref(false)

// ════════════════════════════════════════════
// 红冲弹窗
// ════════════════════════════════════════════

const showReverseModal = ref(false)
const reverseReason = ref('')
const reverseLoading = ref(false)

// ════════════════════════════════════════════
// useBillForm composable
// ════════════════════════════════════════════

async function createWithStatus(data: PurchaseInboundPayload & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await inboundApi.create(payload)
  const created = res?.data || res
  if (status === 1 && created?.id) {
    await inboundApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: PurchaseInboundPayload & { status?: number }) {
  const { status, ...payload } = data
  const res: any = await inboundApi.update(id, payload)
  if (status === 1) {
    await inboundApi.submit(id)
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
  totalTaxAmount,
  totalWithTax,
} = useBillForm({
  billPrefix: 'CGRKD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await inboundApi.getById(id)
      const data = res?.data || res || {}
      const rawItems = data.items || data.details || []
      return {
        ...data,
        date: data.inboundDate || '',
        sourceOrderId: data.orderId || undefined,
        sourceOrderNo: data.orderNo || '',
        purchaserId: data.purchaserId || data.buyerId,
        purchaserName: data.purchaserName || data.buyerName || '',
        departmentId: data.departmentId || undefined,
        deptId: data.departmentId || undefined,
        departmentName: data.departmentName || '',
        trackingNumber: data.trackingNumber || '',
        logisticsCompany: data.logisticsCompany || '',
        internalNote: data.internalNote || '',
        approveTime: data.approveTime || data.approvedTime || '',
        receiveTime: data.receiveTime || '',
        qualityCheckTime: data.qualityCheckTime || '',
        warehouseConfirmTime: data.warehouseConfirmTime || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          itemCode: d.productCode || d.itemCode || '',
          specification: d.productSpec || d.specification || '',
          unit: d.productUnit || d.unit || '',
          quantity: d.orderQuantity ?? d.inboundQuantity ?? d.quantity ?? 0,
          amount: (d.orderQuantity ?? d.inboundQuantity ?? d.quantity ?? 0) * (d.unitPrice || 0),
          costPrice: d.costPrice || d.unitCost || 0,
          costAmount: ((d.orderQuantity ?? d.quantity ?? 0) || 0) * (d.costPrice || d.unitCost || 0),
          batchNo: d.batchNo || '',
          productionDate: d.productionDate || '',
          expiryDate: d.validityDate || d.expiryDate || '',
          shelfLife: d.shelfLife || '',
          location: d.location || '',
          orderItemId: d.orderItemId || undefined,
        })),
      }
    },
  },
  redirectPath: '/purchase/inbound',
  codeApiPath: '/erp/purchase/inbound/next-no',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  fields: [
    { key: 'supplierId', label: '供应商', type: 'select', required: true },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true },
    { key: 'purchaserId', label: '采购员', type: 'select', required: true },
    { key: 'date', label: '入库日期', type: 'date', required: true },
    { key: 'inboundType', label: '入库类型', type: 'select', required: true },
  ],
  productDefaults: PRODUCT_PURCHASE_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'supplierId') {
      const s = optionRefs.suppliers.find((x: any) => x.id === val)
      if (s) {
        fd.supplierName = s.name || ''
        fd.supplierCode = s.code || ''
        fd.bankName = s.bankName || ''
        fd.bankAccount = s.bankAccount || ''
        fd.taxNo = s.taxNo || ''
        fd.contactName = s.contactName || ''
        fd.contactPhone = s.contactPhone || ''
        fd.contactAddress = s.contactAddress || ''
        fd.prevDebt = s.currentDebt || 0
        fd.prevPrepaid = s.prepaidAmount || 0
        fd.prepaidBalance = s.prepaidBalance || 0
        fd.debtBalance = s.currentDebt || 0
      }
    }
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w?.name || ''
    }
    if (fieldKey === 'purchaserId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.purchaserName = u?.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.departmentName = d?.name || ''
    }
    if (fieldKey === 'paymentAccountId') {
      const acc = accountOptions.value.find((x: any) => x.id === val)
      fd.paymentAccountName = acc?.name || ''
    }
  },
  transformPayload: (fd, status): PurchaseInboundPayload & { status: number } => ({
    status,
    orderId: fd.sourceOrderId || undefined,
    orderNo: fd.sourceOrderNo || undefined,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    purchaserId: fd.purchaserId,
    purchaserName: fd.purchaserName,
    departmentId: fd.deptId || undefined,
    departmentName: fd.departmentName || undefined,
    inboundDate: fd.date || undefined,
    inboundType: fd.inboundType,
    trackingNumber: fd.trackingNumber || undefined,
    logisticsCompany: fd.logisticsCompany || undefined,
    remark: fd.remark || undefined,
    internalNote: fd.internalNote || undefined,
    summary: fd.summary || undefined,
    extNum1: fd.extNum1 || undefined,
    extNum2: fd.extNum2 || undefined,
    extText1: fd.extText1 || undefined,
    extText2: fd.extText2 || undefined,
    extText3: fd.extText3 || undefined,
    discountAmount: fd.discountAmount || undefined,
    fee: fd.otherExpense || undefined,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      orderItemId: p.orderItemId || undefined,
      orderQuantity: p.quantity,
      inboundQuantity: p.inboundQuantity ?? p.quantity,
      unitPrice: p.unitPrice,
      unitCost: p.costPrice,
      taxRate: p.taxRate,
      batchNo: p.batchNo || undefined,
      productionDate: p.productionDate || undefined,
      validityDate: p.expiryDate || undefined,
      shelfLife: p.shelfLife || undefined,
      location: p.location || undefined,
      weight: p.weight || 0,
      volume: p.volume || 0,
      gift: p.gift || false,
      remark: p.remark || undefined,
    })),
  }),
})

// 初始化入库单默认值
if (formData.inboundType === undefined) formData.inboundType = 1
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'supplierName', 'warehouseName', 'purchaserName', 'departmentName',
  'sourceOrderNo', 'trackingNumber', 'logisticsCompany',
  'remark', 'internalNote', 'createTime', 'approveTime',
  'receiveTime', 'qualityCheckTime', 'warehouseConfirmTime',
  'supplierCode', 'bankName', 'bankAccount', 'taxNo', 'contactName', 'contactPhone', 'contactAddress',
  'summary', 'auditorName', 'sourceBillNo', 'paymentTerm', 'createByName',
  'extText1', 'extText2', 'extText3',
  // 录单默认值 / 打印设置
  'defaultWarehouseId', 'defaultBuyerId', 'defaultPaymentAccountId', 'printTemplate', 'printPaperSize',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of [
  'extNum1', 'extNum2', 'paymentAmount', 'moreAccounts', 'prevPrepaid', 'usePrepaid', 'prepaidBalance',
  'currentDebt', 'prevDebt', 'debtBalance', 'otherExpense', 'printCount', 'defaultTaxRate', 'printCopies',
]) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = 0

// ════════════════════════════════════════════
// 计算属性
// ════════════════════════════════════════════

const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => INBOUND_STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => INBOUND_STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// 汇总计算
const totalCostAmount = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.quantity || 0) * (p.costPrice || 0)), 0)
)
const totalWeight = computed(() =>
  formData.products.reduce((s: number, p: any) => s + ((p.weight || 0) * (p.quantity || 0)), 0)
)

const availableActions = computed(() => getAvailableActions(currentStatus.value))

// ════════════════════════════════════════════
// 页眉配置
// ════════════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购入库单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    {
      key: 'source-order',
      label: '选择源单',
      icon: ImportOutlined,
    },
    ...availableActions.value.map((act: any) => ({
      key: act.key,
      label: act.label,
      icon: act.icon,
    })),
    ...(currentStatus.value === 9
      ? [{ key: 'reverse', label: '红冲', icon: SwapOutlined }]
      : []),
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ════════════════════════════════════════════
// 基本信息字段
// ════════════════════════════════════════════

// 全量可配置字段（页面配置弹窗，对齐文档37字段 + 入库单特有）
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  // ── 基本信息区（顶部默认显示） ──
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'supplierId', label: '供应商', type: 'select', required: true, inlineLabel: true, width: 300, searchBtn: '+Q', loading: true },
  { key: 'warehouseId', label: '仓库', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'purchaserId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'inboundType', label: '入库类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [
    { label: '采购入库', value: 1 }, { label: '退货入库', value: 2 },
    { label: '调拨入库', value: 3 }, { label: '其他入库', value: 4 },
  ]},
  { key: 'sourceOrderNo', label: '来源订单号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
  { key: 'trackingNumber', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'approveTime', label: '审批时间', type: 'input', inlineLabel: true, width: 210, disabled: true },
  // ── 伙伴信息（默认隐藏） ──
  { key: 'supplierCode', label: '供应商编号', type: 'display', inlineLabel: true, width: 130 },
  { key: 'bankName', label: '开户行', type: 'display', inlineLabel: true, width: 180 },
  { key: 'bankAccount', label: '银行账号', type: 'display', inlineLabel: true, width: 180 },
  { key: 'taxNo', label: '税号', type: 'display', inlineLabel: true, width: 160 },
  { key: 'contactName', label: '联系人', type: 'display', inlineLabel: true, width: 110 },
  { key: 'contactPhone', label: '联系电话', type: 'display', inlineLabel: true, width: 140 },
  { key: 'contactAddress', label: '联系地址', type: 'display', inlineLabel: true, width: 240 },
  // ── 自定义字段（默认隐藏） ──
  { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', inlineLabel: true, width: 140, precision: 2 },
  { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', inlineLabel: true, width: 140, precision: 2 },
  { key: 'extText1', label: '自定义字段3(文本)', type: 'input', inlineLabel: true, width: 140 },
  { key: 'extText2', label: '自定义字段4(文本)', type: 'input', inlineLabel: true, width: 140 },
  { key: 'extText3', label: '自定义字段5(文本)', type: 'input', inlineLabel: true, width: 140 },
  { key: 'auditorName', label: '审核人', type: 'display', inlineLabel: true, width: 120 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
  // ── 资金区（默认隐藏，底部付款区展示） ──
  { key: 'paymentAccountId', label: '付款账户', type: 'select', inlineLabel: true, width: 210, searchBtn: '+Q', loading: true },
  { key: 'paymentAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
  { key: 'moreAccounts', label: '更多账户', type: 'input', inlineLabel: true, width: 130, disabled: true },
  { key: 'prevPrepaid', label: '此前预付', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'usePrepaid', label: '使用预付款', type: 'number', inlineLabel: true, width: 130, precision: 2 },
  { key: 'prepaidBalance', label: '预付余额', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 120, disabled: true, precision: 2 },
  { key: 'paymentTerm', label: '付款期限', type: 'input', inlineLabel: true, width: 150 },
  { key: 'otherExpense', label: '其他费用', type: 'number', inlineLabel: true, width: 120, precision: 2 },
  { key: 'sourceBillNo', label: '源单', type: 'input', inlineLabel: true, width: 160 },
  // ── 单据信息 ──
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'createByName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'discountAmount', label: '优惠金额', type: 'number', inlineLabel: true, width: 120, precision: 2 },
]

// 页面配置：默认隐藏非核心字段（伙伴/自定义/资金/备注），避免与底部付款区重复显示
const DEFAULT_HIDDEN_FIELDS = [
  'deptId', 'supplierCode', 'bankName', 'bankAccount', 'taxNo', 'contactName', 'contactPhone', 'contactAddress',
  'extNum1', 'extNum2', 'extText1', 'extText2', 'extText3',
  'auditorName', 'summary', 'discountAmount',
  'paymentAccountId', 'paymentAmount', 'moreAccounts', 'prevPrepaid', 'usePrepaid', 'prepaidBalance',
  'currentDebt', 'prevDebt', 'debtBalance', 'paymentTerm', 'otherExpense', 'sourceBillNo',
  'remark', 'createByName', 'printCount',
]
// 页面配置显隐/回车跳转状态（字段 -> {visible, enterJump}，对齐采购订单金标准）
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = { visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key), enterJump: ['select', 'date', 'number', 'input'].includes(f.type) }
}

const FORM_CONFIG_MODULE = 'purchase-inbound-form'
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
        defaultBuyerId: parsed.defaults.defaultBuyerId,
        defaultPaymentAccountId: parsed.defaults.defaultPaymentAccountId,
        defaultTaxRate: parsed.defaults.defaultTaxRate,
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
      defaultBuyerId: formData.defaultBuyerId,
      defaultPaymentAccountId: formData.defaultPaymentAccountId,
      defaultTaxRate: formData.defaultTaxRate,
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

// ── 部门/付款账户下拉选项（档案字段全选择器，红线） ──
const departmentOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
  try { accountOptions.value = await optionsApi.getAccounts() } catch { accountOptions.value = [] }
}

// 实际渲染的基本信息字段：过滤隐藏项 + 注入动态选项/加载态
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'supplierId'
        ? (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id }))
        : f.key === 'warehouseId'
          ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id }))
          : f.key === 'purchaserId'
            ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
            : f.key === 'deptId'
              ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
              : f.key === 'paymentAccountId'
                ? (accountOptions.value || []).map((a: any) => ({ label: a.name, value: a.id }))
                : (f as any).options,
      loading: (f.key === 'supplierId' || f.key === 'warehouseId' || f.key === 'purchaserId' || f.key === 'deptId' || f.key === 'paymentAccountId')
        ? loadingOptions.value
        : (f as any).loading,
      searchBtn: (f.key === 'supplierId' || f.key === 'warehouseId' || f.key === 'purchaserId' || f.key === 'deptId' || f.key === 'paymentAccountId')
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
// 摘要面板
// ════════════════════════════════════════════

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '入库数量', value: totalQuantity.value, statusLabel: statusText.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '税额', value: totalTaxAmount.value.toFixed(2) },
  { label: '价税合计', value: totalWithTax.value.toFixed(2), divider: true },
  { label: '成本金额', value: totalCostAmount.value.toFixed(2) },
  { label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
  { label: '总件数', value: formData.products.filter((p: any) => p.productId != null).length },
])

// ════════════════════════════════════════════
// 页脚
// ════════════════════════════════════════════

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '价税合计',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交审批',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ════════════════════════════════════════════
// 明细表格列（含批次/货位/质检等字段）
// ════════════════════════════════════════════

const detailColumns: DetailColumnConfig[] = [
  // ── 固定列 ──
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },

  // ── 商品信息 ──
  { key: 'image', title: '图片', type: 'input', width: 60, defaultHidden: true },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productCell', width: 220 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true },
  { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'input', width: 100, defaultHidden: true },

  // ── 单位 ──
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70, defaultHidden: true },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2, defaultHidden: true },

  // ── 批次/生产日期/保质期（入库时必须） ──
  { key: 'batchNo', title: '批次号', type: 'input', width: 120, placeholder: '请输入批次号' },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120, defaultHidden: true },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期(天)', type: 'input', width: 100 },

  // ── 货位 ──
  { key: 'location', title: '货位', type: 'input', width: 100, placeholder: '如A-01-01' },

  // ── 库存 ──
  { key: 'availableStock', title: '可用库存', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 140, precision: 2, readonly: true, defaultHidden: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 100, precision: 2, readonly: true, defaultHidden: true },

  // ── 数量/包装 ──
  { key: 'quantity', title: '入库数量', type: 'number', width: 100, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80 },

  // ── 价格 ──
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 110, precision: 2, readonly: true },
  { key: 'discountPercent', title: '优惠折扣(%)', type: 'number', width: 110, precision: 2, defaultHidden: true },
  { key: 'favorableUnitPrice', title: '惠后单价', type: 'number', width: 100, precision: 2, defaultHidden: true },
  { key: 'favorableAmount', title: '优惠后金额', type: 'number', width: 110, precision: 2, readonly: true, defaultHidden: true },
  { key: 'taxRate', title: '税率%', type: 'number', width: 80, precision: 1, min: 0, max: 100 },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'latestPurchaseDate', title: '最近采购日期', type: 'date', width: 120, readonly: true, defaultHidden: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2, defaultHidden: true },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2, defaultHidden: true },

  // ── 物理属性 ──
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4, defaultHidden: true },
  { key: 'gift', title: '赠品', type: 'checkbox', width: 60 },

  // ── 质检（显示用） ──
  { key: 'qualityStatus', title: '质检状态', type: 'input', width: 80, readonly: true, defaultHidden: true },
  { key: 'qualityNote', title: '质检说明', type: 'input', width: 120, readonly: true, defaultHidden: true },

  // ── 单据自定义字段 ──
  { key: 'customField1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField4', title: '单据自定义4(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField5', title: '单据自定义5(文本)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField6', title: '单据自定义6(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField7', title: '单据自定义7(数字)', type: 'number', width: 140, precision: 2, defaultHidden: true },
  { key: 'customField8', title: '单据自定义8(往来单位)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField9', title: '单据自定义9(职员)', type: 'input', width: 140, defaultHidden: true },
  { key: 'customField10', title: '单据自定义10(部门)', type: 'input', width: 140, defaultHidden: true },

  // ── 价格等级（标准化产品价格等级，默认隐藏） ──
  { key: 'restaurant', title: '餐饮店', type: 'checkbox', width: 70, defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'checkbox', width: 90, defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'checkbox', width: 80, defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'checkbox', width: 90, defaultHidden: true },

  // ── 备注 ──
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
  { key: 'costAmount', value: totalCostAmount.value.toFixed(2) },
])

// ════════════════════════════════════════════
// 事件处理
// ════════════════════════════════════════════

function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.model = p.model || ''
    row.unit = p.unit || ''
    row.smallUnit = p.smallUnit || ''
    row.brand = p.brand || ''
    row.origin = p.origin || ''
    row.unitPrice = p.purchasePrice || p.price || 0
    row.costPrice = p.costPrice || 0
    row.amount = (row.quantity || 0) * (row.unitPrice || 0)
    row.costAmount = (row.quantity || 0) * (row.costPrice || 0)
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  // 数量/单价变化时自动计算金额和成本
  if (['quantity', 'unitPrice'].includes(fieldKey)) {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
  }
  if (['quantity', 'costPrice'].includes(fieldKey)) {
    record.costAmount = (record.quantity || 0) * (record.costPrice || 0)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'sourceOrderNo' || fieldKey === 'source-order') {
    showOrderSelect.value = true
    loadOrders()
  } else {
    message.info(`${fieldKey} 快速查询功能暂不可用`)
  }
}

// ════════════════════════════════════════════
// 操作处理
// ════════════════════════════════════════════

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/purchase/inbound')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'source-order':
      showOrderSelect.value = true
      loadOrders()
      break
    case 'submit':
      handleSubmit()
      break
    case 'approve':
      handleApprove()
      break
    case 'reject':
      handleReject()
      break
    case 'receive':
      handleReceive()
      break
    case 'quality-check':
      showQCModal.value = true
      qcResult.value = 'pass'
      qcNote.value = ''
      break
    case 'confirm-warehouse':
      handleConfirmWarehouse()
      break
    case 'complete':
      handleComplete()
      break
    case 'reverse':
      showReverseModal.value = true
      reverseReason.value = ''
      break
  }
}

// ── 审核 ──
function handleApprove() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认审核',
    content: `确认审核通过入库单 ${formData.orderNo} 吗？`,
    okText: '审核通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.approve(id)
        message.success('审核成功')
        if (route.query.id) {
          // 重新加载详情
          const res: any = await inboundApi.getById(id)
          const data = res?.data || res || {}
          Object.assign(formData, { ...data, status: 2 })
        } else {
          router.push('/purchase/inbound')
        }
      } catch (err: any) {
        message.error(err?.message || '审核失败')
      }
    },
  })
}

// ── 驳回 ──
function handleReject() {
  const id = Number(formData.id)
  if (!id) return
  let reason = ''
  Modal.confirm({
    title: '驳回入库单',
    content: h('div', [
      h('p', `确认驳回入库单 ${formData.orderNo} 吗？`),
      h('a-input', {
        placeholder: '请输入驳回原因',
        value: reason,
        'onUpdate:value': (v: string) => { reason = v },
      }),
    ]),
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.reject(id, reason || '驳回')
        message.success('已驳回')
        router.push('/purchase/inbound')
      } catch (err: any) {
        message.error(err?.message || '驳回失败')
      }
    },
  })
}

// ── 收货确认 ──
function handleReceive() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '确认收货',
    content: `确认已收到入库单 ${formData.orderNo} 的商品吗？`,
    okText: '确认收货',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.receive(id)
        message.success('收货成功')
        formData.status = 4
        formData.receiveTime = new Date().toISOString()
      } catch (err: any) {
        message.error(err?.message || '收货失败')
      }
    },
  })
}

// ── 质检确认 ──
async function confirmQC() {
  if (!qcResult.value) {
    message.warning('请选择质检结果')
    return
  }
  const id = Number(formData.id)
  if (!id) return
  qcLoading.value = true
  try {
    await inboundApi.qualityCheck(id, qcResult.value)
    message.success('质检完成')
    formData.status = 6
    formData.qualityCheckTime = new Date().toISOString()
    showQCModal.value = false
  } catch (err: any) {
    message.error(err?.message || '质检提交失败')
  } finally {
    qcLoading.value = false
  }
}

// ── 入库确认 ──
function handleConfirmWarehouse() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '入库确认',
    content: '确认将质检合格的商品正式入库？此操作将增加库存并产生应付账款。',
    okText: '确认入库',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.confirmWarehouse(id)
        message.success('入库确认成功，已更新库存')
        formData.status = 8
        formData.warehouseConfirmTime = new Date().toISOString()
      } catch (err: any) {
        message.error(err?.message || '入库确认失败')
      }
    },
  })
}

// ── 完成 ──
function handleComplete() {
  const id = Number(formData.id)
  if (!id) return
  Modal.confirm({
    title: '完成入库单',
    content: `确认完成入库单 ${formData.orderNo} 吗？`,
    okText: '确认完成',
    cancelText: '取消',
    onOk: async () => {
      try {
        await inboundApi.complete(id)
        message.success('入库单已完成')
        formData.status = 9
      } catch (err: any) {
        message.error(err?.message || '操作失败')
      }
    },
  })
}

// ── 红冲 ──
async function confirmReverse() {
  if (!reverseReason.value) {
    message.warning('请输入红冲原因')
    return
  }
  const id = Number(formData.id)
  if (!id) return
  reverseLoading.value = true
  try {
    await inboundApi.cancel(id, reverseReason.value)
    message.success('红冲成功，已生成冲减单据')
    showReverseModal.value = false
    router.push('/purchase/inbound')
  } catch (err: any) {
    message.error(err?.message || '红冲失败')
  } finally {
    reverseLoading.value = false
  }
}

// ════════════════════════════════════════════
// 辅助函数
// ════════════════════════════════════════════

const getOrderStatusColor = (status: number) => {
  const map: Record<number, string> = { 0: 'default', 1: 'orange', 2: 'blue', 3: 'green', 4: 'red', 5: 'processing', 6: 'green' }
  return map[status] || 'default'
}

const getOrderStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '草稿', 1: '待审批', 2: '已审批', 3: '已下达', 4: '已取消', 5: '履行中', 6: '已完成' }
  return map[status] || '未知'
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[采购入库单] ErrorBoundary:', err)
}

// ════════════════════════════════════════════
// 键盘快捷键
// ════════════════════════════════════════════

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  } else if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

// ════════════════════════════════════════════
// 生命周期
// ════════════════════════════════════════════

onMounted(() => {
  // 编辑模式下不初始化空行
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
  window.addEventListener('keydown', handleKeydown)
  loadFormConfig()
  loadExtraOptions()
  // 如果路由携带 orderId 参数，自动加载源单
  const orderId = route.query.orderId
  if (orderId) {
    request.get(`/erp/purchase/order/${orderId}`).then((res: any) => {
      const order = res?.data || res
      if (order) confirmSourceOrder(order)
    }).catch(() => {
      // 静默失败
    })
  }
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

.order-select-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.pay-area {
  padding: 4px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pay-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pay-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}

.pay-item label {
  white-space: nowrap;
}

.config-hint {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 8px;
}
</style>
