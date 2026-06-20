<template>
  <div class="sale-order-form-page">
    <!-- ═══ 顶部操作栏 ═══ -->
    <div class="form-header">
      <div class="header-left">
        <span class="order-no">NO. {{ formData.orderNo || '待生成' }}</span>
        <a-button type="link" size="small" class="attachment-btn">
          <template #icon><PaperClipOutlined /></template>
          附件
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="form-title">销售订单</h2>
      </div>
      <div class="header-right">
        <a-dropdown>
          <a-button size="small">
            <template #icon><PrinterOutlined /></template>
            打印(F8)
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handlePrint('order')">打印订单</a-menu-item>
              <a-menu-item @click="handlePrint('summary')">打印汇总</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button size="small" @click="handleHistory">
          <template #icon><ClockCircleOutlined /></template>
          历史
        </a-button>
        <a-button size="small" @click="handleImport">
          <template #icon><ImportOutlined /></template>
          导入
        </a-button>
        <a-dropdown>
          <a-button size="small">
            更多
            <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item @click="handleSaveDraft">保存草稿</a-menu-item>
              <a-menu-item @click="handleCopyOrder">复制订单</a-menu-item>
              <a-menu-item @click="handleExport">导出</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <!-- ═══ 基本信息区（紧凑两行） ═══ -->
    <div class="form-fields-bar">
      <div class="fields-row">
        <div class="field-item field-required">
          <label>客户</label>
          <div class="field-input-wrap">
            <a-select
              v-model:value="formData.customerId"
              placeholder="请选择客户"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
              style="flex:1"
              @change="handleCustomerChange"
            >
              <a-select-option v-for="item in customerOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
            <a-button type="link" size="small" class="field-search-btn">+Q</a-button>
          </div>
        </div>
        <div class="field-item field-required">
          <label>发货仓库</label>
          <div class="field-input-wrap">
            <a-select
              v-model:value="formData.warehouseId"
              placeholder="请选择仓库"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
              style="flex:1"
            >
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">
                {{ w.name }}
              </a-select-option>
            </a-select>
            <a-button type="link" size="small" class="field-search-btn">+Q</a-button>
          </div>
        </div>
        <div class="field-item field-required">
          <label>经手人</label>
          <div class="field-input-wrap">
            <a-select
              v-model:value="formData.salespersonId"
              placeholder="请选择经手人"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
              style="flex:1"
            >
              <a-select-option v-for="item in userOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
            <a-button type="link" size="small" class="field-search-btn">+Q</a-button>
          </div>
        </div>
        <div class="field-item field-required">
          <label>单据日期</label>
          <div class="field-input-wrap">
            <a-date-picker
              v-model:value="formData.orderDate"
              style="width:100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </div>
        </div>
        <div class="field-item field-required">
          <label>销售类型</label>
          <div class="field-input-wrap">
            <a-select v-model:value="formData.saleType" size="small" style="width:100%">
              <a-select-option :value="1">正常销售</a-select-option>
              <a-select-option :value="2">样品销售</a-select-option>
              <a-select-option :value="3">促销销售</a-select-option>
            </a-select>
          </div>
        </div>
        <div class="field-item">
          <label>收货人</label>
          <div class="field-input-wrap">
            <a-input v-model:value="formData.receiverName" placeholder="请输入收货人" size="small" style="flex:1" />
            <a-button type="link" size="small" class="field-search-btn">Q</a-button>
          </div>
        </div>
      </div>
      <div class="fields-row">
        <div class="field-item field-narrow">
          <label>联系电话</label>
          <a-input v-model:value="formData.receiverPhone" placeholder="请输入联系电话" size="small" />
        </div>
        <div class="field-item field-wide">
          <label>收货地址</label>
          <a-input v-model:value="formData.receiverAddress" placeholder="请输入收货地址" size="small" />
        </div>
      </div>
    </div>

    <!-- ═══ 商品明细表格 + 右侧摘要面板 ═══ -->
    <div class="table-with-sidebar">
      <div class="table-area">
        <div class="table-toolbar">
          <a-button type="link" size="small" class="table-settings-btn">
            <SettingOutlined />
          </a-button>
        </div>

        <VxeTableList
          ref="tableRef"
          :columns="productColumns"
          :data-source="formData.products"
          :pagination="false as any"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
          :max-height="tableMaxHeight"
        >
          <template #rowNo="{ rowIndex }">
            <span class="row-no">{{ rowIndex + 1 }}</span>
          </template>
          <template #actionCell="{ index }">
            <a-space :size="2">
              <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct">
                <PlusCircleOutlined />
              </a-button>
              <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
                <MinusCircleOutlined />
              </a-button>
            </a-space>
          </template>
          <template #productCell="{ record, index }">
            <div class="product-cell">
              <a-select
                v-model:value="record.productId"
                placeholder="请选择商品"
                show-search
                :filter-option="filterOption"
                style="flex:1"
                :loading="loadingOptions"
                size="small"
                @change="(val: number) => handleProductChange(val, index)"
              >
                <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
                  {{ p.name }}
                </a-select-option>
              </a-select>
              <a-switch v-model:checked="record.scanMode" size="small" class="scan-switch" />
            </div>
          </template>
          <template #itemCodeCell="{ record }">
            <a-input v-model:value="record.itemCode" size="small" />
          </template>
          <template #barcodeCell="{ record }">
            <a-input v-model:value="record.barcode" size="small" />
          </template>
          <template #specCell="{ record }">
            <a-input v-model:value="record.specification" size="small" />
          </template>
          <template #locationCell="{ record }">
            <a-input v-model:value="record.location" size="small" />
          </template>
          <template #unitCell="{ record }">
            <a-input v-model:value="record.unit" size="small" />
          </template>
          <template #lineAttrCell="{ record }">
            <a-input v-model:value="record.lineAttribute" size="small" />
          </template>
          <template #batchCodeCell="{ record }">
            <a-input v-model:value="record.batchCode" size="small" />
          </template>
          <template #prodDateCell="{ record }">
            <a-date-picker v-model:value="record.productionDate" size="small" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
          </template>
          <template #shelfLifeCell="{ record }">
            <a-input v-model:value="record.shelfLife" size="small" />
          </template>
          <template #expDateCell="{ record }">
            <a-date-picker v-model:value="record.expiryDate" size="small" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
          </template>
          <template #qtyCell="{ record }">
            <a-input-number v-model:value="record.quantity" :min="0" :precision="2" size="small" style="width:100%" />
          </template>
          <template #bigPackCell="{ record }">
            <a-input-number v-model:value="record.bigPack" :min="0" :precision="0" size="small" style="width:100%" />
          </template>
          <template #midPackCell="{ record }">
            <a-input-number v-model:value="record.midPack" :min="0" :precision="0" size="small" style="width:100%" />
          </template>
          <template #smallPackCell="{ record }">
            <a-input-number v-model:value="record.smallPack" :min="0" :precision="0" size="small" style="width:100%" />
          </template>
          <template #summary>
            <div class="table-summary-row">
              <span class="summary-label">合计</span>
              <span></span><span></span><span></span><span></span><span></span>
              <span></span><span></span><span></span><span></span><span></span>
              <span></span><span></span>
              <span class="summary-red">{{ totalQuantity }}</span>
              <span class="summary-red">{{ totalBigPack }}</span>
              <span class="summary-red">{{ totalMidPack }}</span>
              <span class="summary-red">{{ totalSmallPack }}</span>
            </div>
          </template>
        </VxeTableList>

        <div class="table-expand-link">
          <a-button type="link" size="small" @click="expandTable = !expandTable">
            <FullscreenOutlined />
            {{ expandTable ? '表格收起' : '表格展开显示' }}
          </a-button>
        </div>
      </div>

      <!-- 右侧摘要面板 -->
      <div class="summary-sidebar">
        <div class="sidebar-row">
          <span class="sidebar-label">销售数量</span>
          <span class="sidebar-value">{{ totalQuantity }}</span>
        </div>
        <div class="sidebar-row">
          <span class="sidebar-label">退货数量</span>
          <span class="sidebar-value">{{ returnQty }}</span>
        </div>
        <div class="sidebar-row">
          <span class="sidebar-label">商品金额</span>
          <span class="sidebar-value">{{ totalAmount.toFixed(2) }}</span>
        </div>
        <div class="sidebar-row">
          <span class="sidebar-label">促销优惠</span>
          <span class="sidebar-value">{{ promoDiscount.toFixed(2) }}</span>
        </div>
        <div class="sidebar-row sidebar-divider">
          <span class="sidebar-label">优惠金额</span>
          <span class="sidebar-value">{{ discountAmount.toFixed(2) }}</span>
          <a-button type="link" size="small" class="sidebar-more">···</a-button>
        </div>
        <div class="sidebar-row sidebar-divider">
          <span class="sidebar-label">其他费用</span>
          <span class="sidebar-value">{{ otherFee.toFixed(2) }}</span>
          <a-button type="link" size="small" class="sidebar-more">···</a-button>
        </div>
      </div>
    </div>

    <!-- ═══ 底部标签页 + 备注 + 单据信息 ═══ -->
    <div class="bottom-panel">
      <a-tabs v-model:activeKey="activeTab" size="small" class="bottom-tabs">
        <a-tab-pane key="payment" tab="收款">
          <div class="tab-content-row">
            <div class="tab-field">
              <label>订单账户</label>
              <div class="tab-field-input">
                <a-select v-model:value="formData.paymentAccount" placeholder="请选择" show-search size="small" style="flex:1">
                  <a-select-option v-for="acc in accountOptions" :key="acc.id" :value="acc.id">{{ acc.name }}</a-select-option>
                </a-select>
                <a-button type="link" size="small">+Q</a-button>
              </div>
            </div>
            <div class="tab-field">
              <label>订单金额</label>
              <div class="tab-field-input">
                <a-input :value="totalAmountWithTax.toFixed(2)" size="small" disabled style="flex:1" />
                <a-button type="link" size="small" class="btn-clear">全清</a-button>
              </div>
            </div>
            <div class="tab-field">
              <label>更多账户</label>
              <div class="tab-field-input">
                <a-input value="0" size="small" disabled style="flex:1" />
                <a-button type="link" size="small">···</a-button>
              </div>
            </div>
            <div class="tab-field">
              <label>使用预订货款</label>
              <div class="tab-field-input">
                <a-input value="0" size="small" disabled style="flex:1" />
                <a-button type="link" size="small">···</a-button>
              </div>
            </div>
            <div class="tab-field">
              <label>此前预收</label>
              <a-input value="0" size="small" disabled />
            </div>
            <div class="tab-field">
              <label>预收余额</label>
              <a-input value="0" size="small" disabled />
            </div>
          </div>
        </a-tab-pane>
        <a-tab-pane key="logistics" tab="物流信息">
          <div class="tab-content-row">
            <div class="tab-field">
              <label>物流公司</label>
              <a-input v-model:value="formData.logisticsCompany" placeholder="请输入物流公司" size="small" />
            </div>
            <div class="tab-field">
              <label>物流单号</label>
              <a-input v-model:value="formData.logisticsNo" placeholder="请输入物流单号" size="small" />
            </div>
            <div class="tab-field">
              <label>运费</label>
              <a-input-number v-model:value="formData.shippingFee" :min="0" :precision="2" size="small" style="width:100%" />
            </div>
          </div>
        </a-tab-pane>
        <a-tab-pane key="member" tab="会员信息">
          <div class="tab-content-row">
            <div class="tab-field">
              <label>会员卡号</label>
              <a-input v-model:value="formData.memberCardNo" placeholder="请输入会员卡号" size="small" />
            </div>
            <div class="tab-field">
              <label>会员姓名</label>
              <a-input v-model:value="formData.memberName" placeholder="请输入会员姓名" size="small" />
            </div>
            <div class="tab-field">
              <label>会员折扣</label>
              <a-input-number v-model:value="formData.memberDiscount" :min="0" :max="100" :precision="1" size="small" style="width:100%" />
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>

      <!-- 备注区 -->
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">单据备注</span>
          <a-input v-model:value="formData.orderRemark" placeholder="" size="small" class="remark-input" />
        </div>
        <div class="remark-row">
          <span class="remark-label">买家备注</span>
          <a-input v-model:value="formData.buyerRemark" placeholder="" size="small" class="remark-input" />
        </div>
      </div>

      <!-- 单据信息行 -->
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
        <span class="doc-info-item">打印次数 0</span>
        <a-button type="link" size="small" class="doc-info-link">打印记录</a-button>
        <span class="doc-info-item">源单 <a-tag size="small">0</a-tag></span>
      </div>
    </div>

    <!-- ═══ 底部操作栏 ═══ -->
    <div class="form-footer">
      <div class="footer-left">
        <span class="footer-amount-label">本单金额</span>
        <span class="footer-amount-value">¥{{ totalAmountWithTax.toFixed(2) }}</span>
      </div>
      <div class="footer-right">
        <a-button size="large" :loading="saving" @click="handleSaveDraft">
          保存草稿
          <span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button type="primary" size="large" class="btn-submit" :loading="saving" @click="handleSubmit">
          提交
          <span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PaperClipOutlined,
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  DownOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  FullscreenOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ── 表单数据 ──
const formData = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  customerName: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  salespersonId: undefined as number | undefined,
  salespersonName: '',
  orderDate: '',
  saleType: 1,
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  paymentMethod: undefined as number | undefined,
  paymentAccount: undefined as number | undefined,
  logisticsCompany: '',
  logisticsNo: '',
  shippingFee: 0,
  memberCardNo: '',
  memberName: '',
  memberDiscount: 100,
  orderRemark: '',
  buyerRemark: '',
  products: [] as any[],
})

const activeTab = ref('payment')
const loadingOptions = ref(false)
const saving = ref(false)
const expandTable = ref(false)
const tableMaxHeight = ref(400)

// ── 下拉选项 ──
const customerOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])
const returnQty = ref(0)
const promoDiscount = ref(0)
const discountAmount = ref(0)
const otherFee = ref(0)

const currentUserName = computed(() => userStore?.userInfo?.name || '')

// ── 商品明细列配置（对照截图 18 列） ──
const productColumns = [
  { field: 'rowNo', title: '', width: 40, fixed: 'left', slotName: 'rowNo' },
  { field: 'action', title: '操作', width: 70, fixed: 'left', slotName: 'actionCell' },
  { field: 'productName', title: '商品名称', width: 200, slotName: 'productCell' },
  { field: 'itemCode', title: '货号', width: 100, slotName: 'itemCodeCell' },
  { field: 'barcode', title: '条码', width: 120, slotName: 'barcodeCell' },
  { field: 'specification', title: '规格', width: 100, slotName: 'specCell' },
  { field: 'location', title: '货位', width: 80, slotName: 'locationCell' },
  { field: 'unit', title: '计价单位', width: 80, slotName: 'unitCell' },
  { field: 'lineAttribute', title: '商品行属性', width: 100, slotName: 'lineAttrCell' },
  { field: 'batchCode', title: '批次条码', width: 120, slotName: 'batchCodeCell' },
  { field: 'productionDate', title: '生产日期', width: 110, slotName: 'prodDateCell' },
  { field: 'shelfLife', title: '保质期', width: 80, slotName: 'shelfLifeCell' },
  { field: 'expiryDate', title: '到期日期', width: 110, slotName: 'expDateCell' },
  { field: 'quantity', title: '件散数量', width: 90, slotName: 'qtyCell' },
  { field: 'bigPack', title: '大包装', width: 80, slotName: 'bigPackCell' },
  { field: 'midPack', title: '中包装', width: 80, slotName: 'midPackCell' },
  { field: 'smallPack', title: '小包装', width: 80, slotName: 'smallPackCell' },
]

// ── 计算属性 ──
const totalQuantity = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0), 0))
const totalBigPack = computed(() => formData.products.reduce((s, p) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s, p) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s, p) => s + (p.smallPack || 0), 0))
const totalAmount = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0) * (p.unitPrice || 0), 0))
const taxAmount = computed(() => formData.products.reduce((s, p) => s + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value - discountAmount.value + otherFee.value)

// ── 工具函数 ──
function generateOrderNo() {
  const d = new Date()
  const ds = d.toISOString().slice(0, 10).replace(/-/g, '')
  const r = Math.random().toString(36).substring(2, 6).toUpperCase()
  formData.orderNo = `XSDD-${ds}-${r}`
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children?.[0]?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// ── 加载选项 ──
async function loadOptions() {
  loadingOptions.value = true
  try {
    const [customers, warehouses, users, products] = await Promise.all([
      optionsApi.getCustomers().catch(() => []),
      optionsApi.getWarehouses().catch(() => []),
      optionsApi.getUsers('salesman').catch(() => []),
      optionsApi.getProducts().catch(() => []),
    ])
    customerOptions.value = customers || []
    warehouseOptions.value = warehouses || []
    userOptions.value = users || []
    productOptions.value = products || []
  } finally {
    loadingOptions.value = false
  }
}

// ─ 事件处理 ──
function handleCustomerChange(val: number) {
  const c = customerOptions.value.find(x => x.id === val)
  if (c) {
    formData.customerName = c.name
    formData.receiverName = c.contactName || ''
    formData.receiverPhone = c.contactPhone || ''
    formData.receiverAddress = c.address || ''
  }
}

function handleProductChange(val: number, index: number) {
  const p = productOptions.value.find(x => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.productName = p.name || ''
    row.itemCode = p.code || ''
    row.barcode = p.barcode || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitPrice = p.salePrice || p.price || 0
    row.taxRate = 13
  }
}

function handleAddProduct() {
  formData.products.push({
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    productName: '',
    itemCode: '',
    barcode: '',
    specification: '',
    location: '',
    unit: '',
    lineAttribute: '',
    batchCode: '',
    productionDate: '',
    shelfLife: '',
    expiryDate: '',
    quantity: 0,
    bigPack: 0,
    midPack: 0,
    smallPack: 0,
    unitPrice: 0,
    taxRate: 13,
    scanMode: false,
    remark: '',
  })
}

function handleRemoveProduct(index: number) {
  formData.products.splice(index, 1)
}

function handlePrint(_type: string) {
  message.info('打印功能开发中')
}

function handleHistory() {
  router.push('/erp/sale')
}

function handleImport() {
  message.info('导入功能开发中')
}

function handleCopyOrder() {
  message.info('复制订单功能开发中')
}

function handleExport() {
  message.info('导出功能开发中')
}

// ── 验证 & 提交 ──
function validate(): boolean {
  if (!formData.customerId) { message.warning('请选择客户'); return false }
  if (!formData.warehouseId) { message.warning('请选择发货仓库'); return false }
  if (!formData.salespersonId) { message.warning('请选择经手人'); return false }
  if (!formData.orderDate) { message.warning('请选择单据日期'); return false }
  if (formData.products.length === 0) { message.warning('请添加商品明细'); return false }
  return true
}

function buildPayload(status: number) {
  return {
    ...formData,
    status,
    totalAmount: totalAmount.value,
    taxAmount: taxAmount.value,
    finalAmount: totalAmountWithTax.value,
    details: formData.products.map(p => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      specification: p.specification,
      quantity: p.quantity,
      unit: p.unit,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      totalAmount: (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100),
      bigPack: p.bigPack,
      midPack: p.midPack,
      smallPack: p.smallPack,
      barcode: p.barcode,
      batchCode: p.batchCode,
      productionDate: p.productionDate,
      shelfLife: p.shelfLife,
      expiryDate: p.expiryDate,
      location: p.location,
      remark: p.remark,
    })),
  }
}

async function doSubmit(status: number) {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(status)
    if (formData.orderNo && route.query.id) {
      await saleOrderApi.update(Number(route.query.id), payload)
    } else {
      await saleOrderApi.create(payload)
    }
    message.success(status === 0 ? '保存草稿成功' : '提交成功')
    router.push('/erp/sale')
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function handleSaveDraft() { doSubmit(0) }
function handleSubmit() { doSubmit(1) }

// ── 快捷键 ──
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') { e.preventDefault(); handleSaveDraft() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') { e.preventDefault(); handleSubmit() }
}

// ── 生命周期 ──
onMounted(() => {
  generateOrderNo()
  loadOptions()
  document.addEventListener('keydown', handleKeydown)
  // 编辑模式加载
  const editId = route.params.id || route.query.id
  if (editId) {
    saleOrderApi.getById(Number(editId)).then((res: any) => {
      const data = res?.data || res
      if (data) Object.assign(formData, data)
    }).catch(() => {})
  }
  // 预填充几行空行
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  // 计算表格最大高度
  nextTick(() => {
    const vh = window.innerHeight
    tableMaxHeight.value = Math.max(200, vh - 420)
  })
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ═══ 整体布局 ═══ */
.sale-order-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f0f2f5;
  overflow: hidden;
  font-size: 13px;
}

/* ═══ 顶部操作栏 ═══ */
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  font-family: 'Consolas', 'Monaco', monospace;
}

.attachment-btn {
  color: #8c8c8c;
  font-size: 12px;
}

.header-center {
  flex: 1;
  text-align: center;
}

.form-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  gap: 6px;
  align-items: center;
}

/* ═══ 基本信息栏（紧凑两行） ═══ */
.form-fields-bar {
  background: #fff;
  padding: 8px 16px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.fields-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 6px;
}

.fields-row:last-child {
  margin-bottom: 0;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.field-item label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}

.field-required label::after {
  content: '*';
  color: #ff4d4f;
  margin-left: 2px;
}

.field-narrow {
  flex: 0 0 160px;
}

.field-wide {
  flex: 1;
  min-width: 200px;
}

.field-item:not(.field-narrow):not(.field-wide) {
  flex: 1;
  min-width: 120px;
}

.field-input-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.field-search-btn {
  padding: 0 4px;
  font-size: 11px;
  color: #1890ff;
  flex-shrink: 0;
  line-height: 1;
}

/* ═══ 表格 + 右侧摘要 ═══ */
.table-with-sidebar {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  overflow: hidden;
}

.table-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.table-settings-btn {
  color: #ff4d4f;
  font-size: 16px;
}

.row-no {
  color: #8c8c8c;
  font-size: 12px;
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

.product-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}

.scan-switch {
  flex-shrink: 0;
}

.table-expand-link {
  text-align: center;
  padding: 4px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

/* 合计行 */
.table-summary-row {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #fafafa;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.summary-label {
  font-weight: 700;
  color: #262626;
  min-width: 60px;
}

.summary-red {
  color: #ff4d4f;
  font-weight: 700;
  min-width: 50px;
  text-align: right;
}

/* ═══ 右侧摘要面板 ═══ */
.summary-sidebar {
  width: 180px;
  background: #fff5f5;
  border-left: 1px solid #ffccc7;
  padding: 12px 10px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sidebar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.sidebar-divider {
  border-top: 1px dashed #ffccc7;
  padding-top: 8px;
}

.sidebar-label {
  color: #595959;
}

.sidebar-value {
  font-weight: 600;
  color: #262626;
}

.sidebar-more {
  padding: 0 2px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1;
}

/* ═══ 底部面板 ═══ */
.bottom-panel {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.bottom-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 8px;
}

.tab-content-row {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  flex-wrap: wrap;
}

.tab-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 140px;
}

.tab-field label {
  font-size: 12px;
  color: #595959;
}

.tab-field-input {
  display: flex;
  align-items: center;
  gap: 2px;
}

.btn-clear {
  font-size: 11px;
  color: #ff4d4f;
  flex-shrink: 0;
}

/* 备注区 */
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

/* 单据信息行 */
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

/* ═══ 底部操作栏 ═══ */
.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #fff;
  flex-shrink: 0;
}

.footer-left {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.footer-amount-label {
  font-size: 13px;
  color: #595959;
}

.footer-amount-value {
  font-size: 22px;
  font-weight: 700;
  color: #ff4d4f;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.btn-submit {
  background: #ff4d4f;
  border-color: #ff4d4f;
}

.btn-submit:hover {
  background: #ff7875;
  border-color: #ff7875;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 11px;
  color: #8c8c8c;
}

/* ═══ 紧凑输入框 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small) {
  height: 26px;
  line-height: 24px;
}

:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 24px;
}

:deep(.ant-input-number-sm input) {
  height: 24px;
}

:deep(.ant-tabs-small .ant-tabs-tab) {
  padding: 4px 12px;
  font-size: 13px;
}

:deep(.ant-btn-sm) {
  height: 26px;
  line-height: 24px;
}

:deep(.ant-tag) {
  font-size: 11px;
  line-height: 18px;
}
</style>
