<template>
  <div class="exchange-form-page">
    <!-- 顶部操作栏 -->
    <div class="form-header">
      <div class="header-left">
        <a-button size="small" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <span class="order-no">NO. {{ formData.exchangeNo || '待生成' }}</span>
      </div>
      <div class="header-center">
        <h2 class="form-title">采购换货单</h2>
      </div>
      <div class="header-right">
        <a-button size="small" :loading="saving" @click="handleSaveDraft">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button size="small" type="primary" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
        </a-button>
      </div>
    </div>

    <!-- 表单内容区 -->
    <div class="form-body">
      <!-- 基础信息 -->
      <div class="form-section">
        <h3 class="section-title">基础信息</h3>
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="原采购订单" required>
                <a-select
                  v-model:value="formData.originalOrderId"
                  placeholder="请搜索选择原采购订单"
                  show-search
                  :filter-option="false"
                  :loading="loadingOrders"
                  size="small"
                  @search="handleOrderSearch"
                  @change="handleOrderChange"
                >
                  <a-select-option v-for="order in orderOptions" :key="order.id" :value="order.id">
                    {{ order.orderNo }} - {{ order.supplierName }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="供应商">
                <a-input v-model:value="formData.supplierName" size="small" disabled />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="换货日期" required>
                <a-date-picker
                  v-model:value="formData.exchangeDate"
                  style="width: 100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="换货类型" required>
                <a-select v-model:value="formData.exchangeType" placeholder="请选择换货类型" size="small">
                  <a-select-option :value="1">质量问题</a-select-option>
                  <a-select-option :value="2">规格不符</a-select-option>
                  <a-select-option :value="3">数量错误</a-select-option>
                  <a-select-option :value="4">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="16">
              <a-form-item label="换货原因" required :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
                <a-textarea
                  v-model:value="formData.exchangeReason"
                  :rows="1"
                  placeholder="请输入换货原因"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </div>

      <!-- 换货商品明细 -->
      <div class="form-section">
        <h3 class="section-title">换货商品明细</h3>
        <BillDetailTable
          :columns="detailColumns"
          :data-source="formData.items"
          :max-height="400"
          @cell-change="handleCellChange"
        >
          <template #exchangeQuantityCell="{ record }">
            <a-input-number
              v-model:value="record.exchangeQuantity"
              :min="1"
              :max="record.originalQuantity"
              style="width: 80px"
              size="small"
            />
          </template>
          <template #exchangePriceCell="{ record }">
            <a-input-number
              v-model:value="record.exchangePrice"
              :min="0"
              :precision="2"
              style="width: 100px"
              size="small"
            />
          </template>
          <template #subtotalCell="{ record }">
            <span class="amount-text">¥{{ ((record.exchangeQuantity || 0) * (record.exchangePrice || 0)).toFixed(2) }}</span>
          </template>
        </BillDetailTable>
        <div v-if="formData.items.length === 0" class="empty-tip">
          请先选择原采购订单，系统将自动加载订单明细
        </div>
      </div>

      <!-- 备注 -->
      <div class="form-section">
        <h3 class="section-title">备注</h3>
        <a-textarea
          v-model:value="formData.remark"
          :rows="2"
          placeholder="请输入备注信息"
          :maxlength="500"
          show-count
          size="small"
        />
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-footer">
      <div class="footer-left">
        <span class="total-label">换货总金额：</span>
        <span class="total-amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
      <div class="footer-right">
        <a-button size="large" :loading="saving" @click="handleSaveDraft">
          <template #icon><SaveOutlined /></template>
          保存
          <span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button type="primary" size="large" :loading="saving" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
          <span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, defineOptions } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { SaveOutlined, SendOutlined, ArrowLeftOutlined } from '@ant-design/icons-vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import dayjs from 'dayjs'
import {
  purchaseExchangeApi,
  type PurchaseExchange,
  type CreateExchangeRequest,
} from '@/api/purchase-exchange'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'
import { useUserStore } from '@/stores/user'

interface ExchangeItem {
  id: string
  originalItemId: number
  productId: number
  productName: string
  productCode: string
  originalQuantity: number
  exchangeQuantity: number
  originalPrice: number
  exchangePrice: number
  unit: string
}

defineOptions({ name: 'PurchaseExchangeForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const saving = ref(false)
const loadingOrders = ref(false)
const orderOptions = ref<PurchaseOrder[]>([])

const formData = reactive<{
  id?: number
  exchangeNo: string
  originalOrderId: number | undefined
  supplierName: string
  exchangeDate: string
  exchangeType: number
  exchangeReason: string
  remark: string
  items: ExchangeItem[]
}>({
  exchangeNo: '',
  originalOrderId: undefined,
  supplierName: '',
  exchangeDate: dayjs().format('YYYY-MM-DD'),
  exchangeType: 1,
  exchangeReason: '',
  remark: '',
  items: [],
})

const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'productName', title: '商品名称', type: 'input', width: 180, readonly: true },
  { key: 'productCode', title: '商品编码', type: 'input', width: 120, readonly: true },
  { key: 'unit', title: '单位', type: 'input', width: 80, readonly: true },
  { key: 'originalQuantity', title: '原数量', type: 'number', width: 100, readonly: true, precision: 0 },
  { key: 'exchangeQuantity', title: '换货数量', type: 'slot', slotName: 'exchangeQuantityCell', width: 120 },
  { key: 'originalPrice', title: '原单价', type: 'number', width: 100, readonly: true, precision: 2 },
  { key: 'exchangePrice', title: '换货单价', type: 'slot', slotName: 'exchangePriceCell', width: 120 },
  { key: 'subtotal', title: '小计', type: 'slot', slotName: 'subtotalCell', width: 100 },
]

const totalAmount = computed(() =>
  formData.items.reduce((sum, item) => sum + (item.exchangeQuantity || 0) * (item.exchangePrice || 0), 0)
)

// 生成换货单号
function generateExchangeNo() {
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const random = Math.random().toString(36).substring(2, 8).toUpperCase()
  formData.exchangeNo = `EX${dateStr}${random}`
}

// 搜索原采购订单
async function handleOrderSearch(keyword: string) {
  if (!keyword) return
  loadingOrders.value = true
  try {
    const res = await purchaseOrderApi.page({
      current: 1,
      size: 20,
      tenantId: userStore.tenantId,
      orderNo: keyword,
    })
    orderOptions.value = (res as any).data?.records || []
  } catch (err) {
    console.warn('[采购换货] 搜索采购订单失败', err)
  } finally {
    loadingOrders.value = false
  }
}

// 选择原采购订单 → 加载明细
async function handleOrderChange(orderId: number) {
  const order = orderOptions.value.find(o => o.id === orderId)
  if (!order) return
  formData.supplierName = order.supplierName || ''
  try {
    const itemsRes = await purchaseOrderApi.getItems(orderId)
    const items = (itemsRes as any).data || itemsRes || []
    formData.items = items.map((item: any) => ({
      id: `ex-${item.id}`,
      originalItemId: item.id,
      productId: item.productId || 0,
      productName: item.materialName || item.productName || '',
      productCode: item.productCode || '',
      originalQuantity: item.quantity || 0,
      exchangeQuantity: Math.min(1, item.quantity || 1),
      originalPrice: item.unitPrice ? Number(item.unitPrice) : 0,
      exchangePrice: item.unitPrice ? Number(item.unitPrice) : 0,
      unit: item.unit || '',
    }))
  } catch (err) {
    message.error('获取订单明细失败')
  }
}

// 验证
function validate(): boolean {
  if (!formData.originalOrderId) {
    message.warning('请选择原采购订单')
    return false
  }
  if (!formData.exchangeDate) {
    message.warning('请选择换货日期')
    return false
  }
  if (!formData.exchangeReason) {
    message.warning('请输入换货原因')
    return false
  }
  if (formData.items.length === 0) {
    message.warning('请添加换货商品')
    return false
  }
  return true
}

// 构建提交数据
function buildPayload(status: number): CreateExchangeRequest {
  return {
    originalOrderId: formData.originalOrderId!,
    exchangeDate: formData.exchangeDate,
    exchangeReason: formData.exchangeReason,
    exchangeType: formData.exchangeType,
    remark: formData.remark,
    items: formData.items.map(item => ({
      originalItemId: item.originalItemId,
      productId: item.productId,
      exchangeQuantity: item.exchangeQuantity,
      exchangePrice: item.exchangePrice,
      warehouseId: 0,
      remark: '',
    })),
  }
}

// 保存草稿
async function handleSaveDraft() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(0)
    if (formData.id) {
      await purchaseExchangeApi.update(formData.id, payload)
      message.success('更新成功')
    } else {
      await purchaseExchangeApi.create(payload)
      message.success('创建成功')
    }
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 提交
async function handleSubmit() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(1)
    if (formData.id) {
      await purchaseExchangeApi.update(formData.id, payload)
      message.success('提交成功')
    } else {
      await purchaseExchangeApi.create(payload)
      message.success('提交成功')
    }
    handleBack()
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  } finally {
    saving.value = false
  }
}

function handleBack() {
  router.push('/erp/purchase-exchange')
}

// 加载换货单详情（编辑模式）
async function loadDetail(id: number) {
  try {
    const res = await purchaseExchangeApi.get(id)
    const data = (res as any).data || res
    if (data) {
      formData.id = data.id
      formData.exchangeNo = data.exchangeNo || ''
      formData.originalOrderId = data.originalOrderId
      formData.supplierName = data.supplierName || ''
      formData.exchangeDate = data.exchangeDate || dayjs().format('YYYY-MM-DD')
      formData.exchangeType = data.exchangeType || 1
      formData.exchangeReason = data.exchangeReason || ''
      formData.remark = data.remark || ''
      // 加载明细
      if (data.id) {
        try {
          const itemsRes = await purchaseExchangeApi.getItems(data.id)
          const items = (itemsRes as any).data || itemsRes || []
          formData.items = items.map((item: any) => ({
            id: `ex-${item.id}`,
            originalItemId: item.originalItemId,
            productId: item.productId || 0,
            productName: item.productName || '',
            productCode: item.productCode || '',
            originalQuantity: item.originalQuantity || 0,
            exchangeQuantity: item.exchangeQuantity || 1,
            originalPrice: item.originalPrice || 0,
            exchangePrice: item.exchangePrice || 0,
            unit: item.unit || '',
          }))
        } catch (err) {
          console.warn('[采购换货] 加载明细失败', err)
        }
      }
    }
  } catch (err: any) {
    message.error('加载详情失败: ' + (err?.message || ''))
  }
}

function handleCellChange(_record: any, _fieldKey: string, _value: any) {}

// 快捷键
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(() => {
  generateExchangeNo()
  document.addEventListener('keydown', handleKeydown)
  // 编辑模式：加载详情
  const editId = route.params.id || route.query.id
  if (editId) {
    loadDetail(Number(editId))
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.exchange-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-center {
  flex: 1;
  text-align: center;
}

.header-right {
  display: flex;
  gap: 8px;
}

.order-no {
  font-size: 14px;
  color: #8c8c8c;
  font-family: monospace;
}

.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.form-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.form-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.empty-tip {
  text-align: center;
  padding: 24px;
  color: #8c8c8c;
  font-size: 14px;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.footer-left {
  display: flex;
  align-items: center;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.total-label {
  font-size: 14px;
  color: #595959;
}

.total-amount {
  font-size: 20px;
  font-weight: 700;
  color: #f5222d;
}

.amount-text {
  font-weight: 600;
  color: #1890ff;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small) {
  height: 28px;
  line-height: 28px;
}

:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}

:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
