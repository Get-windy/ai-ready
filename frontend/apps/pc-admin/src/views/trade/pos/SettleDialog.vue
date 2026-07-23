<template>
  <a-modal
    :open="open"
    title="收银结算"
    :width="600"
    :confirm-loading="loading"
    ok-text="确认收款"
    cancel-text="取消"
    @ok="handleOk"
    @cancel="emit('update:open', false)"
  >
    <div class="settle-receivable">
      <span>应收金额</span>
      <span class="receivable-amount">¥{{ fmtMoney(receivable) }}</span>
    </div>

    <div
      v-for="(row, idx) in rows"
      :key="row.key"
      class="pay-row"
    >
      <a-select
        v-model:value="row.method"
        class="pay-method"
        :options="methodOptions(row)"
      />
      <a-input-number
        v-model:value="row.amount"
        class="pay-amount"
        :min="0"
        :precision="2"
        :controls="false"
        placeholder="0.00"
        @press-enter="handleOk"
      />
      <a-button
        type="link"
        danger
        :disabled="rows.length <= 1"
        @click="rows.splice(idx, 1)"
      >
        删除
      </a-button>
    </div>

    <a-button
      v-if="rows.length < PAY_METHODS.length"
      type="dashed"
      block
      @click="addRow"
    >
      + 添加支付方式
    </a-button>

    <div class="settle-summary">
      <div class="summary-line">
        <span>已收合计</span>
        <span>¥{{ fmtMoney(totalPaid) }}</span>
      </div>
      <div
        v-if="shortage > 0"
        class="summary-line shortage"
      >
        <span>未收</span>
        <span>¥{{ fmtMoney(shortage) }}</span>
      </div>
      <div
        v-else
        class="summary-line change"
      >
        <span>找零</span>
        <span>¥{{ fmtMoney(change) }}</span>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PAY_METHODS, fmtMoney, round2, type SettleResult } from './pos-shared'

interface PayRow {
  key: number
  method: string
  amount: number | null
}

const props = defineProps<{
  open: boolean
  receivable: number
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
  (e: 'confirm', result: SettleResult): void
}>()

let rowKey = 0
const rows = ref<PayRow[]>([])

watch(
  () => props.open,
  (v) => {
    if (v) {
      rows.value = [{ key: ++rowKey, method: 'CASH', amount: round2(props.receivable) }]
    }
  }
)

const totalPaid = computed(() => round2(rows.value.reduce((s, r) => s + (Number(r.amount) || 0), 0)))
const change = computed(() => Math.max(0, round2(totalPaid.value - props.receivable)))
const shortage = computed(() => Math.max(0, round2(props.receivable - totalPaid.value)))

/** 每种方式只允许一行：其余行已占用的方式置灰 */
function methodOptions(row: PayRow) {
  const used = new Set(rows.value.filter((r) => r !== row).map((r) => r.method))
  return PAY_METHODS.map((m) => ({ value: m.value, label: m.label, disabled: used.has(m.value) }))
}

function addRow() {
  const used = new Set(rows.value.map((r) => r.method))
  const next = PAY_METHODS.find((m) => !used.has(m.value))
  rows.value.push({ key: ++rowKey, method: next?.value || 'TRANSFER', amount: null })
}

function handleOk() {
  if (rows.value.some((r) => !r.method)) {
    message.warning('请选择支付方式')
    return
  }
  if (shortage.value > 0) {
    message.warning(`未收足款，还差 ¥${fmtMoney(shortage.value)}`)
    return
  }
  // 找零只可能来自现金多付：从现金行扣减找零得到净额（后端校验支付合计=应收）
  const net = rows.value.map((r) => ({ paymentMethod: r.method, paymentAmount: round2(Number(r.amount) || 0) }))
  if (change.value > 0) {
    const cashRow = net.find((p) => p.paymentMethod === 'CASH')
    if (!cashRow) {
      message.warning('存在找零时必须包含现金支付')
      return
    }
    if (round2(cashRow.paymentAmount - change.value) < 0) {
      message.warning('找零金额不能超过现金支付金额')
      return
    }
    cashRow.paymentAmount = round2(cashRow.paymentAmount - change.value)
  }
  const payments = net.filter((p) => p.paymentAmount > 0)
  if (payments.length === 0) {
    message.warning('请录入支付金额')
    return
  }
  emit('confirm', { payments, received: totalPaid.value, change: change.value })
}
</script>

<style scoped>
.settle-receivable {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff2f0;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 15px;
}
.receivable-amount {
  font-size: 26px;
  font-weight: 700;
  color: #ff4d4f;
  font-family: 'SFMono-Regular', Consolas, monospace;
}
.pay-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.pay-method {
  width: 140px;
}
.pay-amount {
  flex: 1;
}
.settle-summary {
  margin-top: 16px;
  border-top: 1px dashed #f0f0f0;
  padding-top: 12px;
}
.summary-line {
  display: flex;
  justify-content: space-between;
  font-size: 15px;
  line-height: 28px;
}
.summary-line.change {
  color: #52c41a;
  font-weight: 600;
  font-size: 17px;
}
.summary-line.shortage {
  color: #ff4d4f;
  font-weight: 600;
  font-size: 17px;
}
</style>
