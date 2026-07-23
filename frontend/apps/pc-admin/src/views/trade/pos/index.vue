<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="零售收银"
      full-height
    >
      <template #headerExtra>
        <a-space :size="16">
          <span class="cashier-info">收银员：{{ cashierName || '未登录' }}</span>
          <a-select
            v-model:value="warehouseId"
            :options="warehouseOptions"
            placeholder="选择收银仓库"
            style="width: 180px"
            @change="handleWarehouseChange"
          />
          <a-badge
            :status="currentShift ? 'success' : 'default'"
            :text="currentShift ? `营业中 ${currentShift.shiftNo}` : '未开班'"
          />
          <a-button size="small" @click="shiftOpen = true">
            {{ currentShift ? '交班' : '开班' }}
          </a-button>
        </a-space>
      </template>

      <div class="pos-layout">
        <!-- 左侧：扫码/搜索 + 商品区 -->
        <div class="pos-main">
          <div class="search-bar">
            <a-input
              ref="searchInputRef"
              v-model:value="keyword"
              size="large"
              allow-clear
              placeholder="扫码枪扫描或输入条码/编码/名称，回车加入购物车"
              @press-enter="handleScan"
            >
              <template #prefix>
                <BarcodeOutlined />
              </template>
            </a-input>
          </div>
          <a-spin :spinning="searching">
            <div v-if="searchResults.length > 0" class="product-grid">
              <div
                v-for="p in searchResults"
                :key="p.id"
                class="product-card"
                @click="addToCart(p)"
              >
                <div class="product-name" :title="p.productName">{{ p.productName }}</div>
                <div class="product-code">{{ p.productCode || p.barcode || '-' }}</div>
                <div class="product-price">¥{{ fmtMoney(p.retailPrice ?? p.unitPrice) }}</div>
                <div class="product-stock" :class="{ 'stock-out': !p.availableStock }">
                  库存 {{ p.availableStock ?? '-' }}
                </div>
              </div>
            </div>
            <a-empty
              v-else
              class="product-empty"
              :description="searching ? '检索中…' : '扫描条码或输入关键词回车检索商品'"
            />
          </a-spin>
        </div>

        <!-- 右侧：购物车 -->
        <div class="pos-sidebar">
          <div class="cart-header">
            <h3>购物车</h3>
            <span class="cart-count">{{ cartItems.length }} 项</span>
            <span v-if="currentOrderNo" class="cart-order-no">{{ currentOrderNo }}</span>
            <a-button type="link" size="small" danger :disabled="cartItems.length === 0" @click="handleClearCart">
              清空
            </a-button>
          </div>

          <div class="member-bar">
            <a-select
              v-model:value="selectedMemberId"
              show-search
              allow-clear
              :filter-option="false"
              :options="memberOptions"
              :not-found-content="memberSearching ? '搜索中…' : '输入手机号/卡号/姓名搜索会员'"
              placeholder="会员（默认散客）"
              style="width: 100%"
              @search="onMemberSearch"
              @change="onMemberChange"
            />
          </div>

          <div class="cart-list">
            <div
              v-for="(item, index) in cartItems"
              :key="item.productId"
              class="cart-item"
            >
              <div class="cart-item-info">
                <div class="cart-item-name">{{ item.productName }}</div>
                <div class="cart-item-meta">
                  ¥{{ fmtMoney(item.unitPrice) }} / {{ item.unit || '件' }}
                </div>
                <div class="cart-item-qty">
                  <a-button size="small" :disabled="item.quantity <= 1" @click="changeQty(item, -1)">-</a-button>
                  <a-input-number
                    v-model:value="item.quantity"
                    size="small"
                    :min="1"
                    :precision="0"
                    :controls="false"
                    class="qty-input"
                    @blur="normalizeQty(item)"
                  />
                  <a-button size="small" @click="changeQty(item, 1)">+</a-button>
                </div>
              </div>
              <div class="cart-item-amount">
                ¥{{ fmtMoney(item.unitPrice * item.quantity) }}
              </div>
              <a-button type="link" size="small" danger @click="cartItems.splice(index, 1)">
                ×
              </a-button>
            </div>
            <a-empty v-if="cartItems.length === 0" description="请扫码或搜索添加商品" />
          </div>

          <div class="cart-footer">
            <div class="cart-line">
              <span>合计（{{ totalQty }} 件）</span>
              <span>¥{{ fmtMoney(totalAmount) }}</span>
            </div>
            <div class="cart-line">
              <span>优惠</span>
              <a-input-number
                v-model:value="discount"
                size="small"
                :min="0"
                :precision="2"
                :controls="false"
                class="discount-input"
              />
            </div>
            <div class="cart-total">
              <span>应收</span>
              <span class="total-amount">¥{{ fmtMoney(receivable) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部操作条 -->
      <div class="pos-actions">
        <a-button :disabled="cartItems.length === 0" :loading="holding" @click="handleHold">
          挂单 <span class="key-hint">F1</span>
        </a-button>
        <a-button @click="holdOpen = true">
          取单 <span class="key-hint">F2</span>
        </a-button>
        <a-button
          type="primary"
          size="large"
          class="settle-btn"
          :disabled="cartItems.length === 0"
          @click="openSettle"
        >
          结算 <span class="key-hint">F9</span>
        </a-button>
        <a-button danger :disabled="cartItems.length === 0 && !currentOrderId" @click="handleVoid">
          作废
        </a-button>
        <a-button @click="shiftOpen = true">交班</a-button>
        <a-button :disabled="!lastPrintableOrderId" @click="handlePrint">打印小票</a-button>
      </div>

      <SettleDialog
        v-model:open="settleOpen"
        :receivable="receivable"
        :loading="settling"
        @confirm="handleSettle"
      />
      <HoldListDialog
        v-model:open="holdOpen"
        :warehouse-id="warehouseId"
        @resumed="handleResumed"
      />
      <ShiftDialog
        v-model:open="shiftOpen"
        :shift="currentShift"
        :cashier-id="cashierId"
        :cashier-name="cashierName"
        :warehouse-id="warehouseId"
        @opened="handleShiftOpened"
        @closed="handleShiftClosed"
      />
      <PrintPreviewDialog
        v-model:open="printOpen"
        :order-id="printOrderId"
        :settlement="lastSettlement"
      />
      <ProductPickerDialog
        v-model:open="pickerOpen"
        :products="pickerProducts"
        :keyword="pickerKeyword"
        @select="addToCart"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { BarcodeOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { useUserStore } from '@/stores/user'
import { stockApi, memberApi } from '@/api/erp'
import {
  retailApi,
  retailShiftApi,
  type QuickProduct,
  type RetailShift,
  type RetailOrderPayload,
  type RetailItemPayload,
  type RetailOrderDetailVO,
} from '@/api/retail'
import { fmtMoney, round2, type SettleResult } from './pos-shared'
import SettleDialog from './SettleDialog.vue'
import HoldListDialog from './HoldListDialog.vue'
import ShiftDialog from './ShiftDialog.vue'
import PrintPreviewDialog from './PrintPreviewDialog.vue'
import ProductPickerDialog from './ProductPickerDialog.vue'

interface CartItem {
  productId: number
  productName: string
  productCode?: string
  barcode?: string
  unit?: string
  unitPrice: number
  quantity: number
  /** 可用库存快照（ undefined 表示未知，不做限制） */
  stock?: number
}

const userStore = useUserStore()
const cashierId = computed(() => userStore.userId || 0)
const cashierName = computed(() => userStore.nickname || userStore.username || '')

// ── 仓库 / 班次 ──────────────────────────────────────────
const warehouseList = ref<{ id: number; warehouseName: string }[]>([])
const warehouseId = ref<number | undefined>(undefined)
const warehouseOptions = computed(() =>
  warehouseList.value.map((w) => ({ label: w.warehouseName, value: w.id }))
)
const warehouseName = computed(
  () => warehouseList.value.find((w) => w.id === warehouseId.value)?.warehouseName || ''
)
const currentShift = ref<RetailShift | null>(null)

// ── 商品检索 ─────────────────────────────────────────────
const searchInputRef = ref()
const keyword = ref('')
const searching = ref(false)
const searchResults = ref<QuickProduct[]>([])
const pickerOpen = ref(false)
const pickerProducts = ref<QuickProduct[]>([])
const pickerKeyword = ref('')

// ── 购物车 ───────────────────────────────────────────────
const cartItems = ref<CartItem[]>([])
const discount = ref<number | null>(0)
const currentOrderId = ref<number | null>(null)
const currentOrderNo = ref<string | undefined>(undefined)

const totalQty = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() =>
  round2(cartItems.value.reduce((s, i) => s + i.unitPrice * i.quantity, 0))
)
const receivable = computed(() => Math.max(0, round2(totalAmount.value - (Number(discount.value) || 0))))

// ── 会员 ─────────────────────────────────────────────────
const selectedMemberId = ref<number | undefined>(undefined)
const memberOptions = ref<{ label: string; value: number; raw: any }[]>([])
const memberSearching = ref(false)
let memberSearchTimer: ReturnType<typeof setTimeout> | undefined

// ── 弹窗 / 流程状态 ──────────────────────────────────────
const settleOpen = ref(false)
const holdOpen = ref(false)
const shiftOpen = ref(false)
const printOpen = ref(false)
const settling = ref(false)
const holding = ref(false)
const printOrderId = ref<number | null>(null)
const lastPrintableOrderId = ref<number | null>(null)
const lastSettlement = ref<{ received: number; change: number } | null>(null)

// ── 初始化 ───────────────────────────────────────────────
onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  focusSearch()
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseList.value = Array.isArray(list) ? list : []
    if (warehouseList.value.length > 0 && !warehouseId.value) {
      warehouseId.value = warehouseList.value[0].id
    }
    if (warehouseList.value.length === 0) {
      message.warning('未获取到仓库，请先在库存模块维护仓库')
    }
  } catch {
    message.error('仓库列表加载失败')
  }
  await refreshShift()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  if (memberSearchTimer) clearTimeout(memberSearchTimer)
})

async function refreshShift() {
  if (!cashierId.value) return
  try {
    currentShift.value = await retailShiftApi.current(cashierId.value)
  } catch {
    currentShift.value = null
  }
}

function focusSearch() {
  searchInputRef.value?.focus?.()
}

function handleWarehouseChange() {
  searchResults.value = []
  focusSearch()
}

// ── 扫码 / 检索 ──────────────────────────────────────────
async function handleScan() {
  const kw = keyword.value.trim()
  if (!kw || searching.value) return
  if (!warehouseId.value) {
    message.warning('请先选择收银仓库')
    return
  }
  searching.value = true
  try {
    const res = await retailApi.quickSearch(kw, warehouseId.value)
    const list = Array.isArray(res) ? res : []
    searchResults.value = list
    keyword.value = ''
    if (list.length === 0) {
      message.warning(`未找到商品：${kw}`)
      return
    }
    // 条码精确命中直接加入（扫码枪主场景）
    const exact = list.filter((p) => p.barcode && p.barcode === kw)
    if (exact.length > 0) {
      addToCart(exact[0])
      return
    }
    if (list.length === 1) {
      addToCart(list[0])
      return
    }
    // 多个命中弹选择
    pickerProducts.value = list
    pickerKeyword.value = kw
    pickerOpen.value = true
  } catch (e: any) {
    message.error(e?.message || '商品检索失败')
  } finally {
    searching.value = false
  }
}

// ── 购物车操作 ───────────────────────────────────────────
function addToCart(p: QuickProduct) {
  const price = round2(Number(p.retailPrice ?? p.unitPrice) || 0)
  const stock = p.availableStock != null ? Number(p.availableStock) : undefined
  const exist = cartItems.value.find((i) => i.productId === p.id)
  const nextQty = (exist?.quantity || 0) + 1
  if (stock != null && nextQty > stock) {
    message.warning(`「${p.productName}」库存不足（可用 ${stock}）`)
    return
  }
  if (exist) {
    exist.quantity = nextQty
  } else {
    cartItems.value.push({
      productId: p.id,
      productName: p.productName,
      productCode: p.productCode,
      barcode: p.barcode,
      unit: p.unit,
      unitPrice: price,
      quantity: 1,
      stock,
    })
  }
  focusSearch()
}

function changeQty(item: CartItem, delta: number) {
  const next = item.quantity + delta
  if (next < 1) return
  if (item.stock != null && next > item.stock) {
    message.warning(`「${item.productName}」库存不足（可用 ${item.stock}）`)
    return
  }
  item.quantity = next
}

function normalizeQty(item: CartItem) {
  if (!item.quantity || item.quantity < 1) item.quantity = 1
  if (item.stock != null && item.quantity > item.stock) {
    message.warning(`「${item.productName}」库存不足（可用 ${item.stock}），已调整为上限`)
    item.quantity = Math.max(1, item.stock)
  }
}

function resetBill() {
  cartItems.value = []
  discount.value = 0
  currentOrderId.value = null
  currentOrderNo.value = undefined
  selectedMemberId.value = undefined
  memberOptions.value = []
}

function handleClearCart() {
  if (currentOrderId.value) {
    message.warning('当前为取出的挂单，请使用「作废」或完成结算')
    return
  }
  resetBill()
}

// ── 会员 ─────────────────────────────────────────────────
function onMemberSearch(kw: string) {
  if (memberSearchTimer) clearTimeout(memberSearchTimer)
  const v = kw.trim()
  if (!v) {
    memberOptions.value = []
    return
  }
  memberSearching.value = true
  memberSearchTimer = setTimeout(async () => {
    try {
      const list = await memberApi.search(v)
      memberOptions.value = (list || []).map((p: any) => {
        const name = p.partyName || p.name || '会员'
        const phone = p.contactPhone || p.phone || p.mobile || ''
        return { label: `${name}${phone ? '　' + phone : ''}`, value: p.id, raw: p }
      })
    } catch {
      memberOptions.value = []
    } finally {
      memberSearching.value = false
    }
  }, 300)
}

function onMemberChange(id: number | undefined) {
  if (!id) {
    selectedMemberId.value = undefined
    return
  }
  selectedMemberId.value = id
}

function currentMember(): { customerId?: number; customerName: string; memberCardNo?: string; memberName?: string } {
  const opt = memberOptions.value.find((o) => o.value === selectedMemberId.value)
  if (!opt) return { customerName: '散客' }
  const p = opt.raw
  const name = p.partyName || p.name || '会员'
  return {
    customerId: p.id,
    customerName: name,
    memberCardNo: p.memberCardNo || p.cardNo || undefined,
    memberName: name,
  }
}

// ── 单据构建 / 保存 ──────────────────────────────────────
function buildPayload(): { order: RetailOrderPayload; items: RetailItemPayload[] } {
  const d = new Date()
  const orderDate = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const member = currentMember()
  const order: RetailOrderPayload = {
    warehouseId: warehouseId.value!,
    warehouseName: warehouseName.value || undefined,
    cashierId: cashierId.value,
    cashierName: cashierName.value || undefined,
    ...member,
    saleType: 'NORMAL',
    status: 0,
    generationMethod: 'POS',
    posMode: true,
    directDiscount: round2(Number(discount.value) || 0),
    payableAmount: receivable.value,
    // 后端结算校验：支付合计 = payableAmount - changeAmount，找零从现金行净额扣减，故此处恒为 0
    changeAmount: 0,
    orderDate,
  }
  const items: RetailItemPayload[] = cartItems.value.map((i) => ({
    productId: i.productId,
    productCode: i.productCode,
    productName: i.productName,
    barcode: i.barcode,
    unit: i.unit,
    quantity: i.quantity,
    unitPrice: i.unitPrice,
  }))
  return { order, items }
}

/** 新建或更新当前单据，返回单据 ID */
async function saveOrder(): Promise<number> {
  const { order, items } = buildPayload()
  if (currentOrderId.value) {
    const saved = await retailApi.update(currentOrderId.value, order, items)
    return saved.id ?? currentOrderId.value
  }
  const saved = await retailApi.create(order, items)
  currentOrderId.value = saved.id
  currentOrderNo.value = saved.retailNo
  return saved.id
}

function ensureReady(): boolean {
  if (!cashierId.value) {
    message.error('未获取到登录用户，请重新登录')
    return false
  }
  if (!warehouseId.value) {
    message.warning('请先选择收银仓库')
    return false
  }
  if (cartItems.value.length === 0) {
    message.warning('购物车为空')
    return false
  }
  return true
}

// ── 结算 ─────────────────────────────────────────────────
function openSettle() {
  if (!ensureReady()) return
  if (receivable.value <= 0) {
    message.warning('应收金额需大于 0')
    return
  }
  if (!currentShift.value) {
    message.warning('当前未开班，请先开班再收银')
    shiftOpen.value = true
    return
  }
  settleOpen.value = true
}

async function handleSettle(result: SettleResult) {
  settling.value = true
  try {
    const orderId = await saveOrder()
    await retailApi.settle(orderId, result.payments)
    message.success(`结算成功${result.change > 0 ? `，找零 ¥${fmtMoney(result.change)}` : ''}`)
    settleOpen.value = false
    lastSettlement.value = { received: result.received, change: result.change }
    lastPrintableOrderId.value = orderId
    printOrderId.value = orderId
    resetBill()
    printOpen.value = true
    focusSearch()
  } catch (e: any) {
    message.error(e?.message || '结算失败')
  } finally {
    settling.value = false
  }
}

// ── 挂单 / 取单 ──────────────────────────────────────────
async function handleHold() {
  if (!ensureReady() || holding.value) return
  holding.value = true
  try {
    const orderId = await saveOrder()
    await retailApi.hold(orderId)
    message.success(`已挂单 ${currentOrderNo.value || ''}`)
    resetBill()
    focusSearch()
  } catch (e: any) {
    message.error(e?.message || '挂单失败')
  } finally {
    holding.value = false
  }
}

function handleResumed(detail: RetailOrderDetailVO) {
  const { order, items } = detail
  cartItems.value = (items || []).map((i) => ({
    productId: i.productId!,
    productName: i.productName || '',
    productCode: i.productCode,
    barcode: i.barcode,
    unit: i.unit,
    unitPrice: round2(Number(i.unitPrice) || 0),
    quantity: Number(i.quantity) || 1,
    stock: undefined,
  }))
  discount.value = round2(Number(order.directDiscount) || 0)
  currentOrderId.value = order.id
  currentOrderNo.value = order.retailNo
  if (order.customerId) {
    const label = order.memberName || order.customerName || '会员'
    memberOptions.value = [{ label, value: order.customerId, raw: { id: order.customerId, partyName: label, memberCardNo: order.memberCardNo } }]
    selectedMemberId.value = order.customerId
  } else {
    selectedMemberId.value = undefined
  }
  focusSearch()
}

// ── 作废 ─────────────────────────────────────────────────
function handleVoid() {
  Modal.confirm({
    title: '作废确认',
    content: currentOrderId.value
      ? `确定作废零售单 ${currentOrderNo}？作废后不可恢复。`
      : '当前商品尚未保存为单据，确定清空？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        if (currentOrderId.value) {
          await retailApi.voidOrder(currentOrderId.value, 'POS收银作废')
        }
        message.success(currentOrderId.value ? '单据已作废' : '已清空')
        resetBill()
        focusSearch()
      } catch (e: any) {
        message.error(e?.message || '作废失败')
        throw e
      }
    },
  })
}

// ── 交班 ─────────────────────────────────────────────────
function handleShiftOpened(shift: RetailShift) {
  currentShift.value = shift
}

function handleShiftClosed() {
  currentShift.value = null
}

// ── 打印 ─────────────────────────────────────────────────
function handlePrint() {
  if (!lastPrintableOrderId.value) return
  lastSettlement.value = null
  printOrderId.value = lastPrintableOrderId.value
  printOpen.value = true
}

// ── 快捷键 ───────────────────────────────────────────────
function onKeydown(e: KeyboardEvent) {
  if (!['F1', 'F2', 'F9'].includes(e.key)) return
  e.preventDefault()
  if (settleOpen.value || holdOpen.value || shiftOpen.value || printOpen.value || pickerOpen.value) return
  if (e.key === 'F1') handleHold()
  else if (e.key === 'F2') holdOpen.value = true
  else if (e.key === 'F9') openSettle()
}

const handleError = (e: Error) => console.error('[POS]', e)
</script>

<style scoped>
.pos-layout {
  display: flex;
  gap: 12px;
  height: calc(100vh - 260px);
  min-height: 420px;
}
.cashier-info {
  color: #666;
  font-size: 13px;
}

/* 左侧商品区 */
.pos-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.search-bar {
  margin-bottom: 12px;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  overflow-y: auto;
  flex: 1;
  align-content: start;
}
.product-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.product-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.15);
}
.product-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-code {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}
.product-price {
  font-size: 18px;
  font-weight: 600;
  color: #ff4d4f;
  font-family: 'SFMono-Regular', Consolas, monospace;
  margin-top: 4px;
}
.product-stock {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
.stock-out {
  color: #ff4d4f;
}
.product-empty {
  margin-top: 80px;
}

/* 右侧购物车 */
.pos-sidebar {
  width: 380px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}
.cart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
}
.cart-header h3 {
  margin: 0;
  flex: 0 0 auto;
}
.cart-count {
  font-size: 12px;
  color: #999;
  flex: 1;
}
.cart-order-no {
  font-size: 11px;
  color: #1890ff;
}
.member-bar {
  padding: 8px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.cart-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}
.cart-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #f5f5f5;
}
.cart-item-info {
  flex: 1;
  min-width: 0;
}
.cart-item-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cart-item-meta {
  font-size: 11px;
  color: #999;
  margin: 2px 0;
}
.cart-item-qty {
  display: flex;
  align-items: center;
  gap: 4px;
}
.qty-input {
  width: 56px;
  text-align: center;
}
.cart-item-amount {
  font-size: 14px;
  font-weight: 600;
  color: #ff4d4f;
  font-family: 'SFMono-Regular', Consolas, monospace;
  white-space: nowrap;
}
.cart-footer {
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}
.cart-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}
.discount-input {
  width: 120px;
}
.cart-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
}
.total-amount {
  font-size: 24px;
  font-weight: 700;
  color: #ff4d4f;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

/* 底部操作条 */
.pos-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  align-items: center;
}
.settle-btn {
  flex: 1;
  font-size: 18px;
  height: 48px;
}
.key-hint {
  font-size: 11px;
  opacity: 0.65;
  margin-left: 4px;
}
</style>
