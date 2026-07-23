<template>
  <a-modal
    :open="open"
    :title="mode === 'open' ? '开班' : mode === 'close' ? '交班' : '交班完成'"
    :width="560"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <!-- 开班 -->
    <div v-if="mode === 'open'">
      <a-form layout="vertical">
        <a-form-item label="收银员">
          <a-input :value="cashierName" disabled />
        </a-form-item>
        <a-form-item label="期初现金（备用金）" required>
          <a-input-number
            v-model:value="openForm.openingCash"
            style="width: 100%"
            :min="0"
            :precision="2"
            placeholder="0.00"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="openForm.remark" placeholder="选填" />
        </a-form-item>
      </a-form>
      <div class="dialog-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="handleOpen">开班</a-button>
      </div>
    </div>

    <!-- 交班（系统汇总 vs 实点现金） -->
    <div v-else-if="mode === 'close'">
      <a-spin :spinning="detailLoading">
        <a-descriptions :column="2" size="small" bordered class="shift-desc">
          <a-descriptions-item label="班次号">{{ shift?.shiftNo }}</a-descriptions-item>
          <a-descriptions-item label="开班时间">{{ shift?.openTime || '-' }}</a-descriptions-item>
          <a-descriptions-item label="期初现金">¥{{ fmtMoney(shift?.openingCash) }}</a-descriptions-item>
          <a-descriptions-item label="已结算单数">{{ summary.orderCount }}</a-descriptions-item>
          <a-descriptions-item label="现金销售">¥{{ fmtMoney(summary.cashSales) }}</a-descriptions-item>
          <a-descriptions-item label="扫码销售">¥{{ fmtMoney(summary.qrSales) }}</a-descriptions-item>
          <a-descriptions-item label="其他支付">¥{{ fmtMoney(summary.otherSales) }}</a-descriptions-item>
          <a-descriptions-item label="实收总额">¥{{ fmtMoney(summary.totalSales) }}</a-descriptions-item>
        </a-descriptions>
        <div class="expected-line">
          <span>应收现金（期初+现金销售）</span>
          <span class="expected-amount">¥{{ fmtMoney(summary.expectedCash) }}</span>
        </div>
        <a-form layout="vertical" style="margin-top: 12px">
          <a-form-item label="实点现金" required>
            <a-input-number
              v-model:value="closeForm.closingCash"
              style="width: 100%"
              :min="0"
              :precision="2"
              placeholder="清点钱箱现金后录入"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="closeForm.remark" placeholder="选填" />
          </a-form-item>
        </a-form>
        <div class="difference-line" :class="differenceClass">
          <span>长短款</span>
          <span>¥{{ fmtMoney(difference) }}</span>
        </div>
      </a-spin>
      <div class="dialog-footer">
        <a-button @click="emit('update:open', false)">取消</a-button>
        <a-button type="primary" danger :loading="submitting" :disabled="detailLoading" @click="handleClose">
          确认交班
        </a-button>
      </div>
    </div>

    <!-- 交班结果 -->
    <div v-else-if="mode === 'result' && resultShift">
      <a-result
        :status="Math.abs(resultShift.difference || 0) < 0.005 ? 'success' : 'warning'"
        :title="`班次 ${resultShift.shiftNo} 已交班`"
      >
        <template #subTitle>
          <div class="result-lines">
            <div>结算单数：{{ resultShift.orderCount || 0 }}　实收总额：¥{{ fmtMoney(resultShift.totalAmount) }}</div>
            <div>应收现金：¥{{ fmtMoney(resultShift.expectedCash) }}　实点现金：¥{{ fmtMoney(resultShift.closingCash) }}</div>
            <div :class="['result-diff', differenceClass]">
              长短款：¥{{ fmtMoney(resultShift.difference) }}
            </div>
          </div>
        </template>
      </a-result>
      <div class="dialog-footer">
        <a-button type="primary" @click="emit('update:open', false)">完成</a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { retailShiftApi, type RetailShift, type RetailOrder } from '@/api/retail'
import { fmtMoney, round2 } from './pos-shared'

const props = defineProps<{
  open: boolean
  /** 当前营业中班次（null → 开班模式） */
  shift: RetailShift | null
  cashierId: number
  cashierName: string
  warehouseId?: number
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'opened', shift: RetailShift): void
  (e: 'closed', shift: RetailShift): void
}>()

type Mode = 'open' | 'close' | 'result'
const mode = ref<Mode>('open')
const submitting = ref(false)
const detailLoading = ref(false)
const resultShift = ref<RetailShift | null>(null)

const openForm = reactive<{ openingCash: number | null; remark: string }>({ openingCash: 0, remark: '' })
const closeForm = reactive<{ closingCash: number | null; remark: string }>({ closingCash: null, remark: '' })
const shiftOrders = ref<RetailOrder[]>([])

watch(
  () => props.open,
  async (v) => {
    if (!v) return
    resultShift.value = null
    if (props.shift) {
      mode.value = 'close'
      closeForm.closingCash = null
      closeForm.remark = ''
      detailLoading.value = true
      try {
        const detail = await retailShiftApi.getDetail(props.shift.id)
        shiftOrders.value = detail?.orders || []
      } catch (e: any) {
        shiftOrders.value = []
        message.error(e?.message || '班次汇总加载失败')
      } finally {
        detailLoading.value = false
      }
    } else {
      mode.value = 'open'
      openForm.openingCash = 0
      openForm.remark = ''
    }
  }
)

/** 系统汇总：与后端 closeShift 同口径（期初+现金销售=应收现金） */
const summary = computed(() => {
  let cashSales = 0
  let qrSales = 0
  let otherSales = 0
  for (const o of shiftOrders.value) {
    cashSales += Number(o.cashAmount) || 0
    qrSales += (Number(o.alipayAmount) || 0) + (Number(o.wechatAmount) || 0)
    otherSales += (Number(o.cardAmount) || 0) + (Number(o.transferAmount) || 0)
  }
  const totalSales = cashSales + qrSales + otherSales
  return {
    orderCount: shiftOrders.value.length,
    cashSales: round2(cashSales),
    qrSales: round2(qrSales),
    otherSales: round2(otherSales),
    totalSales: round2(totalSales),
    expectedCash: round2((Number(props.shift?.openingCash) || 0) + cashSales),
  }
})

const difference = computed(() => round2((Number(closeForm.closingCash) || 0) - summary.value.expectedCash))
const differenceClass = computed(() => {
  const d = mode.value === 'result' ? Number(resultShift.value?.difference) || 0 : difference.value
  return Math.abs(d) < 0.005 ? 'diff-zero' : d > 0 ? 'diff-over' : 'diff-short'
})

async function handleOpen() {
  if (openForm.openingCash == null) {
    message.warning('请录入期初现金')
    return
  }
  submitting.value = true
  try {
    const shift = await retailShiftApi.open({
      cashierId: props.cashierId,
      cashierName: props.cashierName,
      warehouseId: props.warehouseId,
      openingCash: openForm.openingCash,
      remark: openForm.remark || undefined,
    })
    message.success(`开班成功，班次号 ${shift.shiftNo}`)
    emit('opened', shift)
    emit('update:open', false)
  } catch (e: any) {
    message.error(e?.message || '开班失败')
  } finally {
    submitting.value = false
  }
}

async function handleClose() {
  if (!props.shift) return
  if (closeForm.closingCash == null) {
    message.warning('请录入实点现金')
    return
  }
  submitting.value = true
  try {
    const closed = await retailShiftApi.close({
      shiftId: props.shift.id,
      closingCash: closeForm.closingCash,
      remark: closeForm.remark || undefined,
    })
    resultShift.value = closed
    mode.value = 'result'
    emit('closed', closed)
  } catch (e: any) {
    message.error(e?.message || '交班失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
.shift-desc {
  margin-bottom: 12px;
}
.expected-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f6ffed;
  border-radius: 8px;
  margin-top: 12px;
  font-size: 14px;
}
.expected-amount {
  font-size: 20px;
  font-weight: 700;
  color: #52c41a;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.difference-line {
  display: flex;
  justify-content: space-between;
  font-size: 16px;
  font-weight: 600;
  padding: 8px 14px;
  border-radius: 8px;
  background: #fafafa;
  margin-top: 4px;
}
.diff-zero { color: #52c41a; }
.diff-over { color: #faad14; }
.diff-short { color: #ff4d4f; }
.result-lines {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.result-diff {
  font-weight: 600;
}
</style>
