<template>
  <div class="retail-form-page">
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :tabs="[]"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleHoldOrder"
      @submit="handleSettle"
    >
      <!-- ═══ 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <!-- 商品搜索行 -->
        <div class="product-search-row">
          <span class="search-label">商品 条码/名称/编码(F1)</span>
          <a-input
            ref="productSearchInput"
            v-model:value="productSearchText"
            placeholder="条码/名称/编码"
            size="small"
            class="product-search-input"
            @pressEnter="handleProductSearch"
          />
        </div>
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

      <!-- ═══ 底部面板：优惠 + 收款 ═══ -->
      <template #bottom-extra>
        <div class="retail-bottom-layout">
          <!-- 左侧：优惠 + 备注 + 单据信息 -->
          <div class="retail-bottom-left">
            <!-- 优惠行 -->
            <div class="discount-row">
              <div class="discount-field">
                <span class="discount-label">直接优惠</span>
                <a-input-number
                  v-model:value="directDiscount"
                  :min="0"
                  :precision="2"
                  size="small"
                  style="width: 140px"
                  placeholder="0"
                />
              </div>
              <div class="discount-field">
                <span class="discount-label">优惠券</span>
                <a-input size="small" style="width: 140px" placeholder="选择优惠券" readonly>
                  <template #suffix><SearchOutlined style="color:#bbb;cursor:pointer" /></template>
                </a-input>
              </div>
              <div class="discount-field">
                <span class="discount-label">促销优惠</span>
                <a-input-number :value="promoDiscount" :min="0" :precision="2" size="small" disabled style="width: 100px" />
              </div>
              <div class="discount-field">
                <span class="discount-label">此前积分</span>
                <span class="discount-value">{{ prevPoints }}</span>
              </div>
            </div>
            <!-- 单据备注 -->
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input v-model:value="formData.orderRemark" size="small" class="remark-input" />
            </div>
            <!-- 单据信息 -->
            <div class="doc-info-row">
              <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
              <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
              <span class="doc-info-item">打印次数 0</span>
              <a-button type="link" size="small" class="doc-info-link">打印记录</a-button>
            </div>
          </div>

          <!-- 右侧：收款面板 -->
          <div class="retail-payment-panel">
            <div class="payment-title">收款</div>
            <div class="payment-buttons">
              <button
                class="pay-btn pay-btn-cash"
                :class="{ active: cashAmount > 0 }"
                @click="focusPayment('cash')"
              >
                <span class="pay-btn-icon">💵</span>
                <span>现金</span>
              </button>
              <button
                class="pay-btn pay-btn-card"
                :class="{ active: cardAmount > 0 }"
                @click="focusPayment('card')"
              >
                <span class="pay-btn-icon">💳</span>
                <span>银行卡</span>
              </button>
              <button
                class="pay-btn pay-btn-prepaid"
                :class="{ active: prepaidAmount > 0 }"
                @click="focusPayment('prepaid')"
              >
                <span class="pay-btn-icon">📥</span>
                <span>预收款</span>
              </button>
            </div>
            <div class="payment-buttons">
              <button
                class="pay-btn pay-btn-transfer-green"
                :class="{ active: transferAmount > 0 }"
                @click="focusPayment('transfer')"
              >
                <span class="pay-btn-icon">✅</span>
                <span>转账</span>
              </button>
              <button
                class="pay-btn pay-btn-transfer-blue"
                @click="message.info('支付宝收款开发中')"
              >
                <span class="pay-btn-icon">🔵</span>
                <span>转账</span>
              </button>
              <label class="combined-payment-label">
                <a-checkbox v-model:checked="combinedPayment" />
                <span>组合收款</span>
              </label>
            </div>
            <!-- 收款明细 -->
            <div class="payment-details">
              <div class="payment-detail-row">
                <span class="payment-detail-label">应收金额</span>
                <span class="payment-detail-value text-red">{{ payableAmount.toFixed(2) }}</span>
                <div class="payment-input-wrap">
                  <span class="payment-input-label">现金</span>
                  <a-input-number
                    v-model:value="cashAmount"
                    :min="0"
                    :precision="2"
                    size="small"
                    style="width: 110px"
                    @change="updateChangeAmount"
                  />
                  <span class="payment-input-unit">元</span>
                </div>
              </div>
              <div class="payment-detail-row">
                <span class="payment-detail-label">可用预收</span>
                <span class="payment-detail-value">{{ formData.prepaidBalance || 0 }}</span>
                <div class="payment-input-wrap">
                  <span class="payment-input-label">预收余额</span>
                  <span class="payment-input-value">{{ prepaidBalanceDisplay }}</span>
                </div>
              </div>
              <div class="payment-detail-row">
                <span class="payment-detail-label">找零金额</span>
                <span class="payment-detail-value">{{ changeAmount.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 会员选择弹窗 ═══ -->
    <a-modal v-model:open="showMemberSearch" title="选择会员" @ok="confirmMemberSelect" width="520px">
      <a-input-search
        v-model:value="memberSearchKeyword"
        placeholder="输入手机号 / 会员卡号 / 姓名搜索"
        enter-button="搜索"
        @search="doMemberSearch"
        style="margin-bottom: 12px"
      />
      <a-list :data-source="memberSearchResults" :loading="memberSearching" size="small">
        <template #renderItem="{ item }">
          <a-list-item
            style="cursor:pointer"
            :class="{ 'member-selected': selectedMemberId === item.id }"
            @click="selectedMemberId = item.id; selectedMember = item"
          >
            <a-list-item-meta
              :title="item.partyName || item.name"
              :description="`手机: ${item.phone || '-'} | 卡号: ${item.memberCardNo || '-'}`"
            />
            <template #extra>
              <a-tag v-if="selectedMemberId === item.id" color="blue">已选</a-tag>
            </template>
          </a-list-item>
        </template>
        <template #header>
          <div v-if="memberSearchResults.length === 0 && !memberSearching" style="color:#999;text-align:center;padding:16px 0">
            未找到会员，可继续以散客下单
          </div>
        </template>
      </a-list>
      <div style="margin-top:8px;color:#999;font-size:12px">
        提示：不选择会员则按散客下账
      </div>
    </a-modal>
  </div>

  <!-- ═══ POS收银模式全屏覆盖层 ═══ -->
  <Teleport to="body">
    <div v-if="posMode" class="pos-overlay">
      <!-- POS 顶部栏 -->
      <div class="pos-header">
        <button class="pos-exit-btn" @click="exitPosMode">
          <span class="pos-exit-icon"></span> 退出POS收银模式(Shift+Esc)
        </button>
        <h1 class="pos-title">POS收银模式</h1>
        <div class="pos-header-actions">
          <button class="pos-shortcut-btn pos-shortcut-f10" @click="message.info('快捷键面板开发中')">
            ⌨ 快捷键(F10)
          </button>
          <button class="pos-shortcut-btn pos-shortcut-f2" @click="message.info('零售单历史开发中')">
            🕐 零售单历史(F2)
          </button>
        </div>
      </div>

      <!-- POS 商品表格 -->
      <div class="pos-table-container">
        <table class="pos-table">
          <thead>
            <tr>
              <th class="pos-th-settings"><SettingOutlined /></th>
              <th class="pos-th-action">操作</th>
              <th class="pos-th-product">商品名称</th>
              <th class="pos-th-barcode">条码</th>
              <th class="pos-th-stock">可用库存</th>
              <th class="pos-th-qty">数量</th>
              <th class="pos-th-price">单价</th>
              <th class="pos-th-amount">金额</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, idx) in posDisplayRows"
              :key="idx"
              class="pos-tr"
              :class="{
                'pos-tr-first': idx === 0,
                'pos-tr-selected': idx === posSelectedRow,
                'pos-tr-empty': !row.productId && !row.productName
              }"
              @click="posSelectedRow = idx"
              @dblclick="handlePosProductSelect(idx)"
            >
              <td class="pos-td">{{ idx + 1 }}</td>
              <td class="pos-td pos-td-action">
                <button class="pos-action-btn pos-action-add" @click.stop="handleInsertProduct(idx)"></button>
                <button class="pos-action-btn pos-action-del" @click.stop="handleRemoveProduct(idx)">✕</button>
              </td>
              <td class="pos-td pos-td-product">
                <span v-if="row.productId || row.productName" class="pos-product-name">{{ row.productName || '未选择' }}</span>
                <span v-else class="pos-empty-cell" @click.stop="handlePosProductSelect(idx)">双击选择商品</span>
              </td>
              <td class="pos-td">{{ row.barcode || '' }}</td>
              <td class="pos-td">{{ row.availableStock != null ? row.availableStock : '' }}</td>
              <td class="pos-td pos-td-qty">
                <input
                  v-if="row.productId"
                  type="number"
                  class="pos-input"
                  :value="row.quantity || 0"
                  @change="(e: any) => { row.quantity = Number(e.target.value); updateChangeAmount() }"
                  min="0"
                  step="1"
                />
                <span v-else class="pos-empty-cell"></span>
              </td>
              <td class="pos-td pos-td-price">{{ row.unitPrice != null ? row.unitPrice.toFixed(2) : '' }}</td>
              <td class="pos-td pos-td-amount">{{ ((row.quantity || 0) * (row.unitPrice || 0)).toFixed(2) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="pos-tr pos-tr-total">
              <td class="pos-td pos-td-footer-label" colspan="2">合计</td>
              <td class="pos-td"></td>
              <td class="pos-td"></td>
              <td class="pos-td pos-td-total-val">{{ totalQuantity }}</td>
              <td class="pos-td pos-td-total-val">{{ totalAmount.toFixed(2) }}</td>
              <td class="pos-td"></td>
              <td class="pos-td pos-td-total-val pos-td-total-amount">{{ totalAmount.toFixed(2) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- POS 底部栏 -->
      <div class="pos-bottom-bar">
        <div class="pos-bottom-info">
          <div class="pos-info-row">
            <span class="pos-info-label">客户名称：</span>
            <span class="pos-info-value">{{ formData.customerName || '散客' }}</span>
            <button class="pos-info-btn" @click="showMemberSearch = true" title="详情">详</button>
            <button class="pos-info-btn" @click="showMemberSearch = true" title="搜索"></button>
          </div>
          <div class="pos-info-row">
            <span class="pos-info-label">会员卡号：</span>
            <span class="pos-info-placeholder">F6 输入卡号/电话号码</span>
            <span class="pos-info-points">此前积分：<span class="pos-points-value">{{ prevPoints }}</span></span>
          </div>
        </div>
        <div class="pos-bottom-actions">
          <button class="pos-action-btn-large pos-btn-hold" @click="handleHoldOrder">
            挂单<br/><span class="pos-shortcut-text">(F3)</span>
          </button>
          <button class="pos-action-btn-large pos-btn-settle" @click="handleSettle">
            收款<br/><span class="pos-shortcut-text">(空格键)</span>
          </button>
          <div class="pos-total-display">
            <span class="pos-total-symbol">¥</span>
            <span class="pos-total-amount">{{ payableAmount.toFixed(0) }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick, watch } from 'vue'

defineOptions({ name: 'RetailForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusCircleOutlined,
  MinusCircleOutlined,
  SearchOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { retailOrderApi, memberApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)
const productSearchText = ref('')
const productSearchInput = ref()

// ═══ 会员搜索状态 ═══
const showMemberSearch = ref(false)
const memberSearchKeyword = ref('')
const memberSearchResults = ref<any[]>([])
const memberSearching = ref(false)
const selectedMemberId = ref<number | null>(null)
const selectedMember = ref<any>(null)

// ═══ POS 模式 ═══
const posMode = ref(true)
const simpleMode = ref(false)

// ═══ 支付金额 ═══
const cashAmount = ref(0)
const cardAmount = ref(0)
const prepaidAmount = ref(0)
const transferAmount = ref(0)
const changeAmount = ref(0)
const combinedPayment = ref(false)

// ══ 优惠 ═══
const directDiscount = ref(0)
const promoDiscount = ref(0)
const prevPoints = ref(0)

const tableMaxHeight = ref(400)

// ── useBillForm ──
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
  billPrefix: 'LSD',
  api: {
    create: retailOrderApi.create,
    update: retailOrderApi.update,
    getById: (id: number) => retailOrderApi.getDetail(id).then((res: any) => res?.order || res),
  },
  redirectPath: '/sales/retail',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, scanMode: false, quantity: 0,
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status: status ?? 1,
    retailNo: fd.orderNo || fd.retailNo || '',
    customerId: fd.customerId,
    customerName: fd.customerName,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    orderDate: fd.orderDate,
    saleType: fd.saleType || 'NORMAL',
    memberCardNo: fd.memberCardNo || '',
    memberName: fd.memberName || '',
    directDiscount: directDiscount.value,
    promoDiscount: promoDiscount.value,
    prevPoints: prevPoints.value,
    payableAmount: payableAmount.value,
    cashAmount: cashAmount.value,
    cardAmount: cardAmount.value,
    prepaidAmount: prepaidAmount.value,
    transferAmount: transferAmount.value,
    combinedPayment: combinedPayment.value,
    changeAmount: changeAmount.value,
    prepaidBalance: fd.prepaidBalance || 0,
    remark: fd.orderRemark,
    items: fd.products.filter((p: any) => p.productId != null || p.productName).map((p: any) => ({
      productId: p.productId,
      productName: p.productName,
      itemCode: p.itemCode,
      barcode: p.barcode,
      unit: p.unit,
      batchCode: p.batchCode,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      remark: p.remark,
    })),
  }),
})

// ── 初始化零售单字段 ─
if (!('customerId' in formData)) Object.assign(formData, {
  customerId: undefined,
  customerName: '散客',
  warehouseId: undefined,
  warehouseName: '',
  handlerId: undefined,
  handlerName: '',
  orderDate: new Date().toISOString().slice(0, 10),
  saleType: 'NORMAL',
  memberCardNo: '',
  memberName: '',
  orderRemark: '',
  prepaidBalance: 0,
})

// ── 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const totalDiscount = computed(() => directDiscount.value + promoDiscount.value)
const payableAmount = computed(() => Math.max(0, totalAmount.value - totalDiscount.value))
const totalPaid = computed(() => cashAmount.value + cardAmount.value + prepaidAmount.value + transferAmount.value)
const prepaidBalanceDisplay = computed(() => (formData.prepaidBalance || 0).toFixed(2))

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value },
  { key: 'midPack', value: totalMidPack.value },
  { key: 'smallPack', value: totalSmallPack.value },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '零售单',
  orderNo: formData.orderNo || formData.retailNo,
  showAttachment: true,
  actions: [
    { key: 'pos-mode', label: 'POS收银模式', icon: 'ThunderboltOutlined' },
    { key: 'simple-mode', label: simpleMode.value ? '关闭简易' : '简易模式' },
    { key: 'keyboard', label: '键盘' },
    { key: 'settings', label: '设置' },
    { key: 'more', label: '更多', children: [
      { key: 'copy-order', label: '复制零售单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  {
    key: 'customerName',
    label: '客户',
    type: 'input',
    inlineLabel: true,
    width: 220,
    placeholder: '散客',
    searchBtn: '+Q',
  },
  { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 200, placeholder: '输入卡号/电话号码' },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 180, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 160, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 160 },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 160, options: [{ label: '正常销售', value: 'NORMAL' }, { label: '退货', value: 'RETURN' }] },
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '零售数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '应收金额', value: payableAmount.value.toFixed(2), highlight: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单应收',
  amountValue: `\u00a5${payableAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '挂单',
  draftShortcut: 'F3',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置
// ══════════════════════════════════════

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
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 70 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 0 },
])

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '', barcode: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, scanMode: false, quantity: 0,
    productName: '', remark: '',
  }
  formData.products.splice(index + 1, 0, newProduct)
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  if (fieldKey === 'customerName' && val) {
    // 客户变更时，如果不是散客则尝试匹配会员
  }
  updateChangeAmount()
}

function updateChangeAmount() {
  const change = totalPaid.value - payableAmount.value
  changeAmount.value = change > 0 ? change : 0
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.unit = p.unit || ''
      record.availableStock = p.stock || 0
      record.unitPrice = p.retailPrice || p.salePrice || p.price || 0
    }
  }
}

function handleProductSearch() {
  if (!productSearchText.value.trim()) return
  const keyword = productSearchText.value.trim().toLowerCase()
  const product = optionRefs.products.find((p: any) =>
    (p.name || '').toLowerCase().includes(keyword) ||
    (p.code || '').toLowerCase().includes(keyword) ||
    (p.barcode || '').toLowerCase().includes(keyword)
  )
  if (product) {
    // 找到空行或追加
    const emptyIdx = formData.products.findIndex((p: any) => !p.productId && !p.productName)
    if (emptyIdx >= 0) {
      const row = formData.products[emptyIdx]
      row.productId = product.id
      row.productName = product.name
      row.itemCode = product.code || ''
      row.barcode = product.barcode || ''
      row.unit = product.unit || ''
      row.availableStock = product.stock || 0
      row.unitPrice = product.retailPrice || product.salePrice || product.price || 0
    } else {
      handleAddProduct()
      const lastIdx = formData.products.length - 1
      const row = formData.products[lastIdx]
      row.productId = product.id
      row.productName = product.name
      row.itemCode = product.code || ''
      row.barcode = product.barcode || ''
      row.unit = product.unit || ''
      row.availableStock = product.stock || 0
      row.unitPrice = product.retailPrice || product.salePrice || product.price || 0
    }
    productSearchText.value = ''
    message.success(`已添加：${product.name}`)
  } else {
    message.warning('未找到匹配商品')
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'customerName') {
    showMemberSearch.value = true
    memberSearchKeyword.value = ''
    memberSearchResults.value = []
    selectedMemberId.value = null
    selectedMember.value = null
  } else {
    message.info(`${fieldKey} 快速查询功能开发中`)
  }
}

// ═══ 会员搜索 ═══
async function doMemberSearch() {
  if (!memberSearchKeyword.value.trim()) return
  memberSearching.value = true
  try {
    memberSearchResults.value = await memberApi.search(memberSearchKeyword.value)
  } catch {
    memberSearchResults.value = []
  } finally {
    memberSearching.value = false
  }
}

function confirmMemberSelect() {
  if (selectedMember.value) {
    const m = selectedMember.value
    formData.customerId = m.id
    formData.customerName = m.partyName || m.name || '会员'
    formData.memberCardNo = m.memberCardNo || ''
    formData.memberName = m.partyName || m.name || ''
    prevPoints.value = m.points || 0
    message.success(`已选择会员：${formData.customerName}`)
  } else {
    formData.customerId = undefined
    formData.customerName = '散客'
    formData.memberCardNo = ''
    formData.memberName = ''
  }
  showMemberSearch.value = false
}

// ═══ 产品选择弹窗 ═══
function handleOpenProductSelectModal(record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
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
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      const row = formData.products[rowIndex]
      row.productId = p.id
      row.productName = p.name || ''
      row.itemCode = p.code || ''
      row.barcode = p.barcode || ''
      row.unit = p.unit || ''
      row.availableStock = p.stock || 0
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
    }
  })
  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

// ═══ 收款按钮点击 ═══
function focusPayment(type: string) {
  if (type === 'cash') {
    if (cashAmount.value === 0) cashAmount.value = payableAmount.value
  } else if (type === 'card') {
    if (cardAmount.value === 0) cardAmount.value = payableAmount.value
  } else if (type === 'prepaid') {
    if (prepaidAmount.value === 0) prepaidAmount.value = Math.min(payableAmount.value, formData.prepaidBalance || 0)
  } else if (type === 'transfer') {
    if (transferAmount.value === 0) transferAmount.value = payableAmount.value
  }
  updateChangeAmount()
}

// ═══ 挂单（F3）═══════
function handleHoldOrder() {
  handleSaveDraft()
  message.info('零售单已挂单，可在列表中查看')
}

// ═══ 记账（Ctrl+Enter）═══════
function handleSettle() {
  if (payableAmount.value > 0 && totalPaid.value < payableAmount.value) {
    message.warning(`收款不足：应收 ${payableAmount.value.toFixed(2)}，已收 ${totalPaid.value.toFixed(2)}`)
    return
  }
  handleSubmit()
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'copy-order':
    case 'export':
      message.info(`${actionKey} 功能开发中`)
      break
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(async () => {
  // 初始化默认散客
  try {
    const walkin = await memberApi.getWalkIn()
    if (walkin && !formData.customerId) {
      formData.customerId = walkin.id
      formData.customerName = walkin.partyName || '散客'
    }
  } catch {
    formData.customerName = '散客'
  }

  if (formData.products.length === 0) {
    for (let i = 0; i < 12; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 440)
  })
})
</script>

<style scoped>
.retail-form-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ═══ 商品搜索行 ══ */
.product-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-bottom: none;
  padding-left: 12px;
}
.search-label {
  font-size: 13px;
  color: #595959;
  white-space: nowrap;
  font-weight: 500;
}
.product-search-input {
  width: 260px;
}

/* ═══ 操作按钮 ═══ */
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

/* ═══ 底部左右布局 ═══ */
.retail-bottom-layout {
  display: flex;
  gap: 16px;
  padding: 12px 0 0;
}
.retail-bottom-left {
  flex: 1;
  min-width: 0;
}
.retail-payment-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fafbfc;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
}

/* ═══ 优惠行 ═══ */
.discount-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.discount-field {
  display: flex;
  align-items: center;
  gap: 4px;
}
.discount-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}
.discount-value {
  font-size: 13px;
  color: #262626;
  min-width: 40px;
}

/* ═══ 备注 ═══ */
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

/* ═══ 单据信息 ═══ */
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

/* ═══ 收款面板 ═══ */
.payment-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 10px;
}
.payment-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.pay-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 52px;
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  transition: all 0.2s;
  padding: 4px;
}
.pay-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.pay-btn.active {
  border-color: #262626;
  box-shadow: 0 0 0 2px rgba(0,0,0,0.1);
}
.pay-btn-icon {
  font-size: 18px;
  line-height: 1;
  margin-bottom: 2px;
}
.pay-btn-cash {
  background: #fa8c16;
}
.pay-btn-card {
  background: #faad14;
}
.pay-btn-prepaid {
  background: #1890ff;
}
.pay-btn-transfer-green {
  background: #52c41a;
}
.pay-btn-transfer-blue {
  background: #1890ff;
}
.combined-payment-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #595959;
  margin-left: 4px;
  cursor: pointer;
}

/* ══ 收款明细 ═══ */
.payment-details {
  margin-top: 8px;
  border-top: 1px solid #e8e8e8;
  padding-top: 8px;
}
.payment-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}
.payment-detail-label {
  color: #8c8c8c;
  min-width: 60px;
}
.payment-detail-value {
  color: #262626;
  font-weight: 500;
  text-align: right;
  min-width: 60px;
}
.payment-input-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}
.payment-input-label {
  font-size: 11px;
  color: #8c8c8c;
}
.payment-input-value {
  font-size: 12px;
  color: #262626;
}
.payment-input-unit {
  font-size: 11px;
  color: #8c8c8c;
}

/* ═══ 辅助样式 ═══ */
.text-red {
  color: #ff4d4f !important;
}
.member-selected {
  background: #e6f7ff;
}
</style>
