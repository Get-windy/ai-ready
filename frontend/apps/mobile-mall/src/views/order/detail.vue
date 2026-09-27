<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Steps, Step, Cell, CellGroup, Button, Tag, Image, showDialog, showLoadingToast, closeToast } from 'vant'
import { api, type OrderInfo, type LogisticsItem } from '@/api'

const router = useRouter()
const route = useRoute()

const orderId = route.params.id as string
const order = ref<OrderInfo | null>(null)
const logistics = ref<LogisticsItem[]>([])
/** F-06 发货信息：{ logisticsCompany, trackingNo, deliveryStatus, traces, traceTip } */
const tracking = ref<any>(null)

// 显式 Record<string, ...>：状态来自后端字符串，不能让 TS 推导成固定键集合，
// 否则 statusMap[order.status] 会报 TS7053（隐式 any 索引）。
const statusMap: Record<string, { label: string; color: string }> = {
  pending: { label: '待付款', color: '#ff976a' },
  paid: { label: '待发货', color: '#1988fa' },
  shipped: { label: '待收货', color: '#07c160' },
  completed: { label: '已完成', color: '#969799' },
  cancelled: { label: '已取消', color: '#969799' }
}

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.order.getDetail(orderId)
    order.value = res.data
    
    // F-06：取发货信息（物流公司/运单号）。真实轨迹需承运商接口，后端 traces 恒空，
    // 故这里显示"已发货但暂无轨迹"的说明，而不是把有运单号伪装成有轨迹。
    try {
      const trackRes: any = await api.order.track(orderId)
      tracking.value = trackRes?.data ?? trackRes
      logistics.value = tracking.value?.traces || []
    } catch (err: any) {
      console.warn('[订单详情] 物流信息加载失败', err?.response?.data?.message || err?.message)
      tracking.value = null
    }
  } catch (err) {
    console.warn('[订单详情] 加载订单失败', err)
  } finally {
    closeToast()
  }
})

const getStepActive = () => {
  const stepMap: Record<string, number> = {
    pending: 0,
    paid: 1,
    shipped: 2,
    completed: 3
  }
  return stepMap[order.value?.status ?? 'pending'] || 0
}

const handlePay = () => {
  // ⚠️ 原先直接 router.push('/order/{id}/pay')，而该路由**不存在** ⇒ 点「去支付」是白屏。
  //    支付闭环还没通（后端渠道实现仍是桩、回调验签未接线，见审计 P0-2 / 设计文档 F-07），
  //    这里如实提示，等支付通道接入后再改为跳转收银台。
  showDialog({ message: '支付通道建设中，请联系业务员线下付款或稍后再试' })
}

const handleCancel = async () => {
  showLoadingToast({ message: '取消中...', forbidClick: true })
  try {
    await api.order.cancel(orderId)
    closeToast()
    router.back()
  } catch (err) {
    console.warn('[订单详情] 取消订单失败', err)
    closeToast()
  }
}

const handleConfirmReceive = async () => {
  showLoadingToast({ message: '确认中...', forbidClick: true })
  try {
    await api.order.confirm(orderId)
    closeToast()
    loadOrder()
  } catch (err) {
    console.warn('[订单详情] 确认收货失败', err)
    closeToast()
  }
}

const loadOrder = async () => {
  const res = await api.order.getDetail(orderId)
  order.value = res.data
}
</script>

<template>
  <div class="order-detail-page">
    <NavBar 
      title="订单详情" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div v-if="order" class="detail-content">
      <div class="status-section">
        <Steps :active="getStepActive()" active-color="#07c160">
          <Step>提交订单</Step>
          <Step>付款成功</Step>
          <Step>商品发货</Step>
          <Step>交易完成</Step>
        </Steps>
      </div>
      
      <CellGroup inset title="订单状态">
        <Cell :title="statusMap[order.status ?? 'pending']?.label">
          <template #value>
            <Tag :color="statusMap[order.status ?? 'pending']?.color">
              {{ statusMap[order.status ?? 'pending']?.label }}
            </Tag>
          </template>
        </Cell>
      </CellGroup>
      
      <CellGroup v-if="tracking" inset title="物流信息">
        <Cell title="物流公司" :value="tracking.logisticsCompany || '—'" />
        <Cell title="运单号" :value="tracking.trackingNo || '—'" />
        <!-- 轨迹节点：真实数据需对接承运商，暂无时不编造 -->
        <Cell v-if="!logistics.length" title="物流轨迹" :value="tracking.traceTip || '暂无轨迹'" />
        <div v-else class="logistics-list">
          <div
            v-for="(item, index) in logistics"
            :key="index"
            class="logistics-item"
          >
            <div class="logistics-time">{{ item.time }}</div>
            <div class="logistics-content">{{ item.content }}</div>
          </div>
        </div>
      </CellGroup>
      
      <CellGroup inset title="收货信息">
        <Cell title="收货人" :value="order.address?.consignee" />
        <Cell title="联系电话" :value="order.address?.phone" />
        <Cell title="收货地址" :value="`${order.address?.region ?? ''} ${order.address?.address ?? ''}`.trim()" />
      </CellGroup>
      
      <CellGroup inset title="商品信息">
        <div class="items-list">
          <div 
            v-for="item in order.items"
            :key="item.id"
            class="item-row"
          >
            <Image 
              :src="item.image" 
              width="60"
              height="60"
              fit="cover"
            />
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-spec">{{ item.spec }}</div>
              <div class="item-quantity">x{{ item.quantity }}</div>
            </div>
            <div class="item-price">¥{{ item.price }}</div>
          </div>
        </div>
      </CellGroup>
      
      <CellGroup inset title="订单信息">
        <Cell title="订单编号" :value="order.orderNo" />
        <Cell title="创建时间" :value="order.createTime" />
        <Cell title="支付方式" :value="order.paymentMethod" />
        <Cell title="订单备注" :value="order.remark || '无'" />
      </CellGroup>
      
      <CellGroup inset title="金额信息">
        <Cell title="商品金额" :value="`¥${order.productAmount}`" />
        <Cell title="运费" :value="(order.freight ?? 0) > 0 ? `¥${order.freight}` : '免运费'" />
        <Cell title="订单总额" :value="`¥${order.totalAmount}`" value-class="amount-value" />
      </CellGroup>
      
      <div class="action-buttons">
        <Button 
          v-if="order.status === 'pending'"
          type="default" 
          size="large"
          block
          @click="handleCancel"
        >
          取消订单
        </Button>
        <Button 
          v-if="order.status === 'pending'"
          type="primary" 
          size="large"
          block
          @click="handlePay"
        >
          立即付款
        </Button>
        <Button 
          v-if="order.status === 'shipped'"
          type="primary" 
          size="large"
          block
          @click="handleConfirmReceive"
        >
          确认收货
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 20px;
}

.status-section {
  padding: 16px;
  background: #fff;
}

.logistics-list {
  padding: 12px;
  
  .logistics-item {
    padding: 8px 0;
    
    .logistics-time {
      font-size: 12px;
      color: #969799;
    }
    
    .logistics-content {
      font-size: 14px;
      color: #333;
      margin-top: 4px;
    }
  }
}

.items-list {
  padding: 12px;
  
  .item-row {
    display: flex;
    align-items: center;
    padding: 8px 0;
    
    .item-info {
      flex: 1;
      margin-left: 12px;
      
      .item-name {
        font-size: 14px;
        color: #333;
      }
      
      .item-spec {
        font-size: 12px;
        color: #969799;
        margin-top: 2px;
      }
      
      .item-quantity {
        font-size: 12px;
        color: #969799;
        margin-top: 2px;
      }
    }
    
    .item-price {
      font-size: 14px;
      color: #333;
    }
  }
}

.amount-value {
  color: #f44;
  font-weight: 600;
}

.action-buttons {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>