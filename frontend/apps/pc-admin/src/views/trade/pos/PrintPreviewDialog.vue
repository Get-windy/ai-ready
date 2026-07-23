<template>
  <a-modal
    :open="open"
    title="小票预览"
    :width="420"
    :footer="null"
    @cancel="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <div v-if="detail" class="receipt">
        <div class="receipt-header">
          <div class="store-name">企智连·零售小票</div>
          <div>单号：{{ detail.order.retailNo }}</div>
          <div>时间：{{ detail.order.completedTime || detail.order.createTime || '-' }}</div>
          <div>收银员：{{ detail.order.cashierName || '-' }}　仓库：{{ detail.order.warehouseName || '-' }}</div>
          <div v-if="detail.order.memberName || detail.order.customerName">
            会员/客户：{{ detail.order.memberName || detail.order.customerName }}
            <template v-if="detail.order.memberCardNo">（{{ detail.order.memberCardNo }}）</template>
          </div>
        </div>

        <table class="receipt-items">
          <thead>
            <tr>
              <th class="col-name">商品</th>
              <th class="col-num">数量</th>
              <th class="col-num">单价</th>
              <th class="col-num">金额</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in detail.items" :key="item.id ?? idx">
              <td class="col-name">{{ item.productName }}</td>
              <td class="col-num">{{ item.quantity }}</td>
              <td class="col-num">{{ fmtMoney(item.unitPrice) }}</td>
              <td class="col-num">{{ fmtMoney(item.amount) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="receipt-summary">
          <div><span>合计</span><span>¥{{ fmtMoney(detail.order.amount) }}</span></div>
          <div v-if="Number(detail.order.directDiscount) > 0">
            <span>优惠</span><span>-¥{{ fmtMoney(detail.order.directDiscount) }}</span>
          </div>
          <div class="receipt-payable"><span>应收</span><span>¥{{ fmtMoney(detail.order.payableAmount) }}</span></div>
          <div v-for="(p, idx) in detail.payments || []" :key="idx">
            <span>{{ payMethodLabel(p.paymentMethod) }}</span><span>¥{{ fmtMoney(p.paymentAmount) }}</span>
          </div>
          <div v-if="settlement">
            <span>实收</span><span>¥{{ fmtMoney(settlement.received) }}</span>
          </div>
          <div v-if="settlement && settlement.change > 0">
            <span>找零</span><span>¥{{ fmtMoney(settlement.change) }}</span>
          </div>
        </div>

        <div class="receipt-footer">谢谢惠顾，欢迎再次光临</div>
      </div>
      <a-empty v-else-if="!loading" description="打印数据加载失败" />
    </a-spin>

    <div class="preview-footer">
      <a-button @click="emit('update:open', false)">关闭</a-button>
      <a-button type="primary" :disabled="!detail" :loading="printing" @click="handlePrint">
        打印（模拟）
      </a-button>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { retailApi, type RetailOrderDetailVO } from '@/api/retail'
import { fmtMoney, payMethodLabel } from './pos-shared'

const props = defineProps<{
  open: boolean
  orderId: number | null
  /** 结算完成时的本地实收/找零（后端按净额记录支付，找零仅在此处展示） */
  settlement?: { received: number; change: number } | null
}>()

const emit = defineEmits<{
  (e: 'update:open', v: boolean): void
}>()

const detail = ref<RetailOrderDetailVO | null>(null)
const loading = ref(false)
const printing = ref(false)

watch(
  () => [props.open, props.orderId] as [boolean, number | null],
  async ([v, id]) => {
    if (!v || !id) return
    detail.value = null
    loading.value = true
    try {
      detail.value = await retailApi.getPrintData(id)
    } catch (e: any) {
      message.error(e?.message || '打印数据加载失败')
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

async function handlePrint() {
  if (!props.orderId) return
  printing.value = true
  try {
    await retailApi.afterPrint(props.orderId)
    message.success('已模拟打印（未连接真实打印机）')
  } catch (e: any) {
    message.error(e?.message || '打印失败')
  } finally {
    printing.value = false
  }
}
</script>

<style scoped>
.receipt {
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 12px;
  color: #333;
  background: #fff;
  padding: 8px 4px;
}
.receipt-header {
  text-align: center;
  border-bottom: 1px dashed #999;
  padding-bottom: 8px;
  margin-bottom: 8px;
  line-height: 20px;
}
.store-name {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
}
.receipt-items {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
}
.receipt-items th,
.receipt-items td {
  padding: 2px 4px;
  border-bottom: 1px dashed #eee;
}
.col-name {
  text-align: left;
  word-break: break-all;
}
.col-num {
  text-align: right;
  white-space: nowrap;
}
.receipt-summary {
  border-top: 1px dashed #999;
  padding-top: 6px;
}
.receipt-summary > div {
  display: flex;
  justify-content: space-between;
  line-height: 20px;
}
.receipt-payable {
  font-size: 14px;
  font-weight: 700;
}
.receipt-footer {
  text-align: center;
  margin-top: 10px;
  border-top: 1px dashed #999;
  padding-top: 8px;
}
.preview-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
