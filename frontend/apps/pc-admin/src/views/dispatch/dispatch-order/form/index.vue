<template>
  <BillFormPage
    :model-value="formData"
    mode="create"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :summary="summaryConfig"
    :footer="footerConfig"
    @update:model-value="(val: Record<string, any>) => Object.assign(formData, val)"
    @submit="handleSubmit"
  >
    <template #bottom-extra>
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
        <span class="doc-info-item">保存后任务进入「待分配」，可在调度任务中指派骑手</span>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { taskApi } from '@/api/dms/task'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'DispatchOrderForm' })

const userStore = useUserStore()
const currentUserName = computed(() => (userStore?.userInfo as any)?.nickname || (userStore?.userInfo as any)?.username || '')

// ═══ 配送单号（任务编号，后端必填；格式 PS+时间戳） ═══
const taskNo = ref(genTaskNo())
function genTaskNo(): string {
  return `PS${dayjs().format('YYYYMMDDHHmmss')}`
}

function genEmptyForm() {
  return {
    orderNo: '',
    orderType: 1,
    priority: 1,
    customerName: '',
    customerPhone: '',
    customerAddress: '',
    sourceAddress: '',
    totalItems: undefined as number | undefined,
    totalQuantity: undefined as number | undefined,
    goodsAmount: undefined as number | undefined,
    deliveryFee: undefined as number | undefined,
    collectOnDelivery: undefined as number | undefined,
    deadlineTime: undefined as string | undefined,
    remark: ''
  }
}

const formData = reactive(genEmptyForm())
const saving = ref(false)

// ═══ 头部 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '配送单',
  orderNo: taskNo.value
}))

// ═══ 基本信息字段 ═══
const basicInfoFields: BasicInfoField[] = [
  { key: 'orderNo', label: '关联订单号', type: 'input', required: true, placeholder: '销售订单/调拨单号', row: 1 },
  {
    key: 'orderType', label: '订单类型', type: 'select', required: true, row: 1,
    options: [
      { label: '销售配送', value: 1 },
      { label: '调拨', value: 2 },
      { label: '退货', value: 3 }
    ]
  },
  {
    key: 'priority', label: '优先级', type: 'select', row: 1,
    options: [
      { label: '普通', value: 1 },
      { label: '紧急', value: 2 },
      { label: '加急', value: 3 }
    ]
  },
  { key: 'customerName', label: '客户名称', type: 'input', required: true, placeholder: '收货客户', row: 2 },
  { key: 'customerPhone', label: '联系电话', type: 'input', placeholder: '收货人电话', row: 2 },
  { key: 'customerAddress', label: '收货地址', type: 'input', width: 'wide', placeholder: '客户收货详细地址', row: 3 },
  { key: 'sourceAddress', label: '发货地址', type: 'input', width: 'wide', placeholder: '仓库/门店发货地址', row: 3 },
  { key: 'totalItems', label: '商品行数', type: 'number', min: 0, precision: 0, row: 4 },
  { key: 'totalQuantity', label: '总数量', type: 'number', min: 0, row: 4 },
  { key: 'goodsAmount', label: '货品金额', type: 'number', min: 0, precision: 2, row: 4 },
  { key: 'deliveryFee', label: '配送费', type: 'number', min: 0, precision: 2, row: 4 },
  { key: 'collectOnDelivery', label: '代收货款', type: 'number', min: 0, precision: 2, row: 4 },
  { key: 'deadlineTime', label: '要求送达日期', type: 'date', row: 5 },
  { key: 'remark', label: '备注', type: 'input', width: 'wide', placeholder: '配送要求等备注信息', row: 5 }
]

// ═══ 摘要 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '货品金额', value: formatMoney(formData.goodsAmount) },
  { label: '配送费', value: formatMoney(formData.deliveryFee) },
  { label: '代收货款', value: formatMoney(formData.collectOnDelivery) },
  { label: '应收合计', value: formatMoney((Number(formData.goodsAmount) || 0) + (Number(formData.deliveryFee) || 0)), divider: true }
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '货品金额',
  amountValue: formatMoney(formData.goodsAmount),
  primaryBtnText: '保存配送单',
  saving: saving.value
}))

function formatMoney(val: number | null | undefined): string {
  const num = Number(val) || 0
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatNow(): string {
  return dayjs().format('YYYY-MM-DD HH:mm')
}

// ═══ 提交（创建 DMS 配送任务，后端初始化为待分配） ═══
async function handleSubmit() {
  if (!formData.orderNo?.trim()) {
    message.warning('请输入关联订单号')
    return
  }
  if (!formData.customerName?.trim()) {
    message.warning('请输入客户名称')
    return
  }
  saving.value = true
  try {
    await taskApi.create({
      taskNo: taskNo.value,
      orderNo: formData.orderNo.trim(),
      orderType: formData.orderType,
      priority: formData.priority,
      customerName: formData.customerName.trim(),
      customerPhone: formData.customerPhone || undefined,
      customerAddress: formData.customerAddress || undefined,
      sourceAddress: formData.sourceAddress || undefined,
      totalItems: formData.totalItems,
      totalQuantity: formData.totalQuantity,
      goodsAmount: formData.goodsAmount,
      deliveryFee: formData.deliveryFee,
      collectOnDelivery: formData.collectOnDelivery,
      deadlineTime: formData.deadlineTime ? `${formData.deadlineTime}T00:00:00` : undefined,
      remark: formData.remark || undefined
    })
    message.success(`配送单 ${taskNo.value} 已保存，状态：待分配`)
    Object.assign(formData, genEmptyForm())
    taskNo.value = genTaskNo()
  } catch (e) {
    console.warn('[配送单] 保存失败', e)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.doc-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
