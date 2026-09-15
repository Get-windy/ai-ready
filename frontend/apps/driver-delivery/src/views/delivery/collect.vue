<script setup lang="ts">
/**
 * 司机端「收款」—— 真实接入末端收款（`POST /api/dms/payment/confirm`）。
 *
 * 2026-09-13：原页面提交到不存在的 `POST /orders/{id}/collect`，支付方式是自造的字符串枚举
 * （cash/wechat…），与后端 `payChannel` 数字字典不一致。本次对齐《收款管理开发文档》：
 *   · 收款类型 paymentType：1 代收货款 / 2 配送费；
 *   · 支付方式 payChannel：1 微信 2 支付宝 3 现金 4 POS 5 银行转账 9 其他（取 `/dms/payment/dict`）；
 *   · 金额缺省按任务代收货款（无代收则按配送费）带出，可改。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NavBar, Field, RadioGroup, Radio, Button, CellGroup, Cell,
  showToast, showLoadingToast, closeToast, showConfirmDialog,
} from 'vant'
import { dmsApi } from '@/api/dms'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.params.id as string)

const detail = ref<any>(null)
const amount = ref<string>('')
const paymentType = ref<number>(1)
const payChannel = ref<number>(3)
const externalOrderNo = ref('')
const channels = ref<Array<{ value: number; label: string }>>([])
const paymentTypes = ref<Array<{ value: number; label: string }>>([
  { value: 1, label: '代收货款' },
  { value: 2, label: '配送费' },
])
const submitting = ref(false)

const toNumber = (v: any) => (v == null || v === '' ? 0 : Number(v))

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const [detailRes, dictRes]: any[] = await Promise.all([
      dmsApi.getTaskDetail(taskId.value).catch(() => null),
      dmsApi.getPaymentDict().catch(() => null),
    ])
    detail.value = detailRes?.data ?? detailRes ?? null

    const dict = dictRes?.data ?? null
    if (dict?.payChannels) {
      channels.value = Object.entries(dict.payChannels).map(([k, v]) => ({ value: Number(k), label: String(v) }))
    } else {
      channels.value = [
        { value: 1, label: '微信' }, { value: 2, label: '支付宝' }, { value: 3, label: '现金' },
        { value: 4, label: 'POS' }, { value: 5, label: '银行转账' }, { value: 9, label: '其他' },
      ]
    }
    if (dict?.paymentTypes) {
      paymentTypes.value = Object.entries(dict.paymentTypes).map(([k, v]) => ({ value: Number(k), label: String(v) }))
    }
    // 默认待收口径：有代收货款 → 代收货款；否则 → 配送费
    const cod = toNumber(detail.value?.collectOnDelivery)
    paymentType.value = cod > 0 ? 1 : 2
    amount.value = String(cod > 0 ? cod : toNumber(detail.value?.deliveryFee))
  } finally {
    closeToast()
  }
})

const handleSubmit = () => {
  const amt = Number(amount.value)
  if (!amount.value || Number.isNaN(amt) || amt <= 0) {
    showToast({ type: 'fail', message: '请输入收款金额' })
    return
  }
  showConfirmDialog({ title: '收款确认', message: `确定收款 ¥${amt.toFixed(2)} 吗？` })
    .then(async () => { await doSubmit(amt) })
    .catch(() => { /* 取消 */ })
}

const doSubmit = async (amt: number) => {
  submitting.value = true
  showLoadingToast({ message: '提交中...', forbidClick: true })
  try {
    await dmsApi.confirmPayment({
      taskId: taskId.value,
      amount: amt,
      payChannel: payChannel.value,
      paymentType: paymentType.value,
      externalOrderNo: externalOrderNo.value || undefined,
    })
    showToast({ type: 'success', message: '收款成功' })
    setTimeout(() => router.replace(`/delivery/${taskId.value}`), 600)
  } catch (e: any) {
    showToast({ type: 'fail', message: e?.message || '收款提交失败' })
  } finally {
    submitting.value = false
    closeToast()
  }
}
</script>

<template>
  <div class="collect-page">
    <NavBar title="收款" left-arrow @click-left="router.back()" />

    <div class="collect-content">
      <div class="amount-display">
        <div class="amount-label">待收金额</div>
        <div class="amount-value">¥{{ amount || '0.00' }}</div>
      </div>

      <CellGroup inset>
        <Cell title="任务编号" :value="detail?.taskNo || taskId" />
        <Cell title="客户" :value="detail?.customerName || '-'" />
        <Cell title="代收货款" :value="`¥${toNumber(detail?.collectOnDelivery).toFixed(2)}`" />
        <Cell title="配送费" :value="`¥${toNumber(detail?.deliveryFee).toFixed(2)}`" />
      </CellGroup>

      <div class="section-title">收款金额</div>
      <Field v-model="amount" label="金额" placeholder="请输入收款金额" type="number" />

      <div class="section-title">收款类型</div>
      <CellGroup inset>
        <Cell>
          <RadioGroup v-model="paymentType" direction="horizontal">
            <Radio v-for="t in paymentTypes" :key="t.value" :name="t.value">{{ t.label }}</Radio>
          </RadioGroup>
        </Cell>
      </CellGroup>

      <div class="section-title">支付方式</div>
      <CellGroup inset>
        <Cell>
          <RadioGroup v-model="payChannel" direction="horizontal">
            <Radio v-for="c in channels" :key="c.value" :name="c.value">{{ c.label }}</Radio>
          </RadioGroup>
        </Cell>
      </CellGroup>

      <div class="section-title">外部单号（POS 流水 / 转账凭证号，可选）</div>
      <Field v-model="externalOrderNo" label="单号" placeholder="选填" />

      <div class="submit-section">
        <Button
          type="primary"
          size="large"
          block
          :loading="submitting"
          :disabled="submitting"
          @click="handleSubmit"
        >
          确认收款
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.collect-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.collect-content {
  padding: 12px;
}

.amount-display {
  background: linear-gradient(135deg, #ff976a, #ffb88c);
  padding: 24px;
  border-radius: 8px;
  text-align: center;
  color: #fff;
  margin-bottom: 16px;

  .amount-label {
    font-size: 14px;
  }

  .amount-value {
    font-size: 32px;
    font-weight: 600;
    margin-top: 8px;
  }
}

.section-title {
  font-size: 14px;
  color: #969799;
  padding: 12px 4px 8px;
}

.submit-section {
  padding: 24px 0;
}
</style>
