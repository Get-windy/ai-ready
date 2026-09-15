<script setup lang="ts">
/**
 * 司机端「配送详情」—— 真实读取 DMS 配送单（`GET /api/dms/task/{id}`）。
 *
 * 2026-09-13：原页面为原型假数据（`/api/v1/delivery/deliveries/{id}` 端点不存在），
 * 现改为真实字段：任务编号 / 客户 / 地址 / 商品明细 / 代收货款 / 配送费 / 签收状态。
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Cell, CellGroup, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { dmsApi } from '@/api/dms'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.params.id as string)

const detail = ref<any>(null)
const sign = ref<any>(null)
const loading = ref(false)

const STATUS_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '已分配', color: '#969799' },
  2: { label: '已接单', color: '#1988fa' },
  3: { label: '取货中', color: '#ff976a' },
  4: { label: '配送中', color: '#07c160' },
  5: { label: '已签收', color: '#1988fa' },
  6: { label: '已完成', color: '#07c160' },
}
const statusText = (s: number) => (STATUS_MAP[s] || { label: '待处理' }).label

const SIGN_TYPE_TEXT: Record<number, string> = { 1: '正常签收', 2: '部分签收', 3: '拒收' }
const AUDIT_TEXT: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }

const money = (v: any) => (v == null || v === '' ? '0.00' : Number(v).toFixed(2))
const qty = (v: any) => (v == null || v === '' ? '0' : String(Number(v)))

const loadDetail = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res: any = await dmsApi.getTaskDetail(taskId.value)
    detail.value = res?.data ?? res ?? null
    // 已有签收记录（含被驳回的）时展示，避免司机重复提交
    try {
      const sres: any = await dmsApi.getTaskSign(taskId.value)
      sign.value = sres?.data ?? null
    } catch {
      sign.value = null
    }
  } catch (e: any) {
    detail.value = null
    showToast(e?.message || '加载配送详情失败')
  } finally {
    loading.value = false
    closeToast()
  }
}

onMounted(loadDetail)

const goSign = () => router.push(`/delivery/${taskId.value}/sign`)
const goCollect = () => router.push(`/delivery/${taskId.value}/collect`)
const goNavigate = () => router.push(`/map/navigation/${taskId.value}`)

const callCustomer = () => {
  if (detail.value?.customerPhone) {
    window.location.href = `tel:${detail.value.customerPhone}`
  }
}
</script>

<template>
  <div class="delivery-detail-page">
    <NavBar title="配送详情" left-arrow @click-left="router.back()" />

    <Empty v-if="!detail" description="未找到配送任务" />

    <div v-else class="detail-content">
      <Card class="info-card">
        <template #title>
          <div class="card-header">
            <span class="order-no">{{ detail.taskNo }}</span>
            <Tag :color="(STATUS_MAP[detail.status] || {}).color || '#969799'">
              {{ statusText(detail.status) }}
            </Tag>
          </div>
        </template>
        <template #desc>
          <CellGroup inset>
            <Cell title="客户姓名" :value="detail.customerName || '-'" />
            <Cell title="联系电话" :value="detail.customerPhone || '-'" is-link @click="callCustomer" />
            <Cell title="配送地址" :value="detail.customerAddress || '-'" />
            <Cell title="来源单据" :value="detail.sourceBillNo || detail.orderNo || '-'" />
            <Cell title="配送日期" :value="detail.deliveryDate || '-'" />
            <Cell title="配送车辆" :value="detail.vehicleName || '-'" />
            <Cell title="配送里程" :value="detail.estimatedDistance != null ? `${detail.estimatedDistance} km` : '-'" />
            <Cell v-if="detail.remark" title="备注" :value="detail.remark" />
          </CellGroup>
        </template>
      </Card>

      <Card class="items-card">
        <template #title><span class="card-title">配送商品</span></template>
        <template #desc>
          <Empty v-if="!(detail.items || []).length" description="无商品明细" image-size="60" />
          <div v-else class="items-list">
            <div v-for="item in detail.items" :key="item.id" class="item-row">
              <div class="item-name">{{ item.productName }}<span class="spec" v-if="item.spec"> / {{ item.spec }}</span></div>
              <div class="item-quantity">x{{ qty(item.quantity) }}</div>
              <div class="item-price">¥{{ money(item.amount) }}</div>
            </div>
          </div>
          <div class="items-total">
            <span>发货数量:</span>
            <span class="total-amount">{{ qty(detail.totalQuantity) }}</span>
          </div>
        </template>
      </Card>

      <Card class="amount-card">
        <template #title><span class="card-title">收付信息</span></template>
        <template #desc>
          <div class="amount-info">
            <div class="amount-row">
              <span class="label">代收货款:</span>
              <span class="value collect">¥{{ money(detail.collectOnDelivery) }}</span>
            </div>
            <div class="amount-row">
              <span class="label">配送费:</span>
              <span class="value">¥{{ money(detail.deliveryFee) }}</span>
            </div>
            <div class="amount-row">
              <span class="label">货款金额:</span>
              <span class="value">¥{{ money(detail.goodsAmount) }}</span>
            </div>
          </div>
        </template>
      </Card>

      <Card v-if="sign" class="sign-card">
        <template #title><span class="card-title">签收记录</span></template>
        <template #desc>
          <CellGroup inset>
            <Cell title="签收类型" :value="SIGN_TYPE_TEXT[sign.signType] || '-'" />
            <Cell title="签收数量" :value="sign.actualQuantity != null ? `${qty(sign.actualQuantity)} / ${qty(sign.plannedQuantity)}` : qty(sign.plannedQuantity)" />
            <Cell title="签收时间" :value="sign.signTime || '-'" />
            <Cell title="定位偏差" :value="sign.locationDeviation != null ? `${sign.locationDeviation} 米${sign.locationWarning === 1 ? '（超阈值）' : ''}` : '-'" />
            <Cell title="审核状态" :value="AUDIT_TEXT[sign.auditStatus] ?? '-'" />
            <Cell v-if="sign.auditRemark" title="审核意见" :value="sign.auditRemark" />
            <Cell v-if="sign.remark" title="签收备注" :value="sign.remark" />
          </CellGroup>
        </template>
      </Card>

      <div class="action-buttons">
        <Button type="primary" block @click="goNavigate">开始导航</Button>
        <Button
          v-if="detail.status === 4"
          type="success"
          block
          @click="goSign"
        >
          签收
        </Button>
        <Button
          v-if="Number(detail.collectOnDelivery || 0) > 0"
          type="warning"
          block
          @click="goCollect"
        >
          收款
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.delivery-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.detail-content {
  padding: 12px;
}

.info-card,
.items-card,
.amount-card,
.sign-card {
  margin-bottom: 12px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;

    .order-no {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .card-title {
    font-size: 14px;
    font-weight: 600;
  }
}

.items-list {
  .item-row {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid #eee;

    .item-name {
      flex: 1;

      .spec {
        color: #969799;
        font-size: 12px;
      }
    }

    .item-quantity {
      width: 60px;
      text-align: center;
    }

    .item-price {
      width: 80px;
      text-align: right;
      color: #f44;
    }
  }
}

.items-total {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  font-weight: 600;

  .total-amount {
    color: #f44;
    margin-left: 8px;
  }
}

.amount-info {
  .amount-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;

    .label {
      color: #969799;
    }

    .value.collect {
      color: #f44;
      font-weight: 600;
      font-size: 18px;
    }
  }
}

.action-buttons {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
