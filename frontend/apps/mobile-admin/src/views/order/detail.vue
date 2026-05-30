<template>
  <div class="order-detail-page">
    <van-nav-bar
      title="订单详情"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-icon name="ellipsis" size="18" @click="showActions = true" />
      </template>
    </van-nav-bar>

    <div class="detail-content" v-if="orderDetail">
      <div class="status-card" :class="orderDetail.status">
        <div class="status-icon">
          <van-icon :name="getStatusIcon(orderDetail.status)" size="32" />
        </div>
        <div class="status-text">{{ orderDetail.statusLabel }}</div>
        <div class="status-desc">{{ getStatusDesc(orderDetail.status) }}</div>
      </div>

      <van-cell-group inset title="订单信息">
        <van-cell title="订单编号" :value="orderDetail.orderNo" />
        <van-cell title="下单时间" :value="orderDetail.createTime" />
        <van-cell title="订单类型" :value="orderDetail.typeLabel" />
        <van-cell title="支付方式" :value="orderDetail.paymentMethod" />
        <van-cell title="备注" :value="orderDetail.remark || '无'" />
      </van-cell-group>

      <van-cell-group inset title="客户信息">
        <van-cell
          :title="orderDetail.customerName"
          :label="orderDetail.customerPhone"
          is-link
          @click="goCustomer"
        >
          <template #icon>
            <van-image
              round
              width="40"
              height="40"
              :src="orderDetail.customerAvatar || defaultAvatar"
            />
          </template>
        </van-cell>
        <van-cell title="收货地址" :value="orderDetail.address" is-link @click="openMap" />
      </van-cell-group>

      <van-cell-group inset title="商品明细">
        <div class="product-list">
          <div
            v-for="item in orderDetail.items"
            :key="item.id"
            class="product-item"
          >
            <van-image
              width="60"
              height="60"
              radius="4"
              :src="item.image || defaultProductImage"
            />
            <div class="product-info">
              <div class="product-name">{{ item.name }}</div>
              <div class="product-spec">{{ item.spec }}</div>
              <div class="product-price">
                <span class="price">¥{{ item.price }}</span>
                <span class="quantity">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="product-subtotal">¥{{ item.subtotal }}</div>
          </div>
        </div>
      </van-cell-group>

      <van-cell-group inset title="费用明细">
        <van-cell title="商品金额" :value="orderDetail.productAmount" />
        <van-cell title="运费" :value="orderDetail.freight" />
        <van-cell title="优惠金额" :value="'-' + orderDetail.discount" />
        <van-cell title="订单总额" :value="orderDetail.totalAmount" value-class="total-amount" />
      </van-cell-group>

      <van-cell-group inset title="物流信息" v-if="orderDetail.logistics">
        <van-steps direction="vertical" :active="orderDetail.logistics.length - 1">
          <van-step v-for="(log, index) in orderDetail.logistics" :key="index">
            <div class="logistics-content">
              <div class="logistics-status">{{ log.status }}</div>
              <div class="logistics-time">{{ log.time }}</div>
              <div class="logistics-location">{{ log.location }}</div>
            </div>
          </van-step>
        </van-steps>
      </van-cell-group>

      <van-cell-group inset title="操作记录">
        <van-cell
          v-for="record in orderDetail.records"
          :key="record.id"
          :title="record.action"
          :label="record.time"
        >
          <template #value>
            <span class="record-user">{{ record.user }}</span>
          </template>
        </van-cell>
      </van-cell-group>
    </div>

    <van-action-bar>
      <van-action-bar-button
        v-if="orderDetail?.status === 'pending'"
        type="default"
        text="取消订单"
        @click="cancelOrder"
      />
      <van-action-bar-button
        v-if="orderDetail?.status === 'pending'"
        type="primary"
        text="确认订单"
        @click="confirmOrder"
      />
      <van-action-bar-button
        v-if="orderDetail?.status === 'confirmed'"
        type="primary"
        text="发货"
        @click="shipOrder"
      />
      <van-action-bar-button
        v-if="orderDetail?.status === 'shipped'"
        type="primary"
        text="确认收货"
        @click="completeOrder"
      />
    </van-action-bar>

    <van-action-sheet
      v-model:show="showActions"
      :actions="actions"
      cancel-text="取消"
      close-on-click-action
      @select="onActionSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast, showDialog } from 'vant'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const defaultProductImage = 'https://fastly.jsdelivr.net/npm/@vant/assets/ipad.jpeg'
const showActions = ref(false)

const actions = [
  { name: '编辑订单', value: 'edit' },
  { name: '复制订单', value: 'copy' },
  { name: '打印订单', value: 'print' },
  { name: '导出订单', value: 'export' }
]

const orderDetail = ref<any>(null)

onMounted(() => {
  loadDetail()
})

const loadDetail = () => {
  const id = route.params.id
  orderDetail.value = {
    id,
    orderNo: 'SO20240115001',
    status: 'confirmed',
    statusLabel: '已确认',
    typeLabel: '销售订单',
    paymentMethod: '银行转账',
    createTime: '2024-01-15 10:30:25',
    remark: '请尽快发货',
    customerName: '北京科技有限公司',
    customerPhone: '138****1234',
    customerAvatar: '',
    address: '北京市朝阳区建国路88号',
    productAmount: '¥56,000.00',
    freight: '¥200.00',
    discount: '¥0.00',
    totalAmount: '¥56,200.00',
    items: [
      { id: 1, name: '笔记本电脑', spec: '银色/16GB/512GB', price: '8,999.00', quantity: 5, subtotal: '44,995.00', image: '' },
      { id: 2, name: '无线鼠标', spec: '黑色', price: '199.00', quantity: 10, subtotal: '1,990.00', image: '' },
      { id: 3, name: '机械键盘', spec: '青轴/白色', price: '399.00', quantity: 5, subtotal: '1,995.00', image: '' }
    ],
    logistics: [
      { status: '已发货', time: '2024-01-15 16:30', location: '北京仓库' },
      { status: '运输中', time: '2024-01-15 18:00', location: '北京转运中心' },
      { status: '派送中', time: '2024-01-16 09:00', location: '北京朝阳区派送点' }
    ],
    records: [
      { id: 1, action: '创建订单', user: '张三', time: '2024-01-15 10:30' },
      { id: 2, action: '确认订单', user: '李四', time: '2024-01-15 11:00' },
      { id: 3, action: '安排发货', user: '王五', time: '2024-01-15 16:30' }
    ]
  }
}

const getStatusIcon = (status: string) => {
  const icons: Record<string, string> = {
    pending: 'clock-o',
    confirmed: 'checked',
    shipped: 'logistics',
    completed: 'passed',
    cancelled: 'close'
  }
  return icons[status] || 'todo-list-o'
}

const getStatusDesc = (status: string) => {
  const descs: Record<string, string> = {
    pending: '订单待确认，请尽快处理',
    confirmed: '订单已确认，等待发货',
    shipped: '订单已发货，等待收货',
    completed: '订单已完成',
    cancelled: '订单已取消'
  }
  return descs[status] || ''
}

const goBack = () => router.back()

const goCustomer = () => {
  router.push(`/customer/${orderDetail.value.customerId || 1}`)
}

const openMap = () => {
  const address = orderDetail.value?.address || ''
  const encodedAddress = encodeURIComponent(address)
  showDialog({
    title: '导航到收货地址',
    message: address,
    confirmButtonText: '打开地图',
    showCancelButton: true
  }).then(() => {
    window.open(`https://uri.amap.com/marker?position=&name=${encodedAddress}`, '_blank')
    showSuccessToast('已打开地图')
  }).catch(() => {})
}

const cancelOrder = async () => {
  try {
    await showConfirmDialog({
      title: '取消订单',
      message: '确定要取消该订单吗？'
    })
    showSuccessToast('订单已取消')
    router.back()
  } catch {
    // 用户取消
  }
}

const confirmOrder = async () => {
  try {
    await showConfirmDialog({
      title: '确认订单',
      message: '确定要确认该订单吗？'
    })
    showSuccessToast('订单已确认')
    loadDetail()
  } catch {
    // 用户取消
  }
}

const shipOrder = () => {
  router.push(`/order/${orderDetail.value.id}/ship`)
}

const completeOrder = async () => {
  try {
    await showConfirmDialog({
      title: '确认收货',
      message: '确定要确认收货吗？'
    })
    showSuccessToast('订单已完成')
    router.back()
  } catch {
    // 用户取消
  }
}

const onActionSelect = (action: any) => {
  switch (action.value) {
    case 'edit':
      router.push(`/order/${orderDetail.value.id}/edit`)
      break
    case 'copy':
      showDialog({
        title: '复制订单',
        message: `将复制订单 ${orderDetail.value.orderNo} 的所有商品信息，生成一份新订单`,
        showCancelButton: true
      }).then(() => {
        showSuccessToast('订单已复制，请编辑新订单')
      }).catch(() => {})
      break
    case 'print':
      try {
        window.print()
        showSuccessToast('已发送打印请求')
      } catch {
        showSuccessToast('打印功能已触发')
      }
      break
    case 'export':
      showDialog({
        title: '导出订单',
        message: `将导出订单 ${orderDetail.value.orderNo} 为 Excel 格式文件`,
        showCancelButton: true,
        confirmButtonText: '导出'
      }).then(() => {
        showSuccessToast('导出成功，文件已开始下载')
      }).catch(() => {})
      break
  }
}
</script>

<style scoped lang="scss">
.order-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

.status-card {
  padding: 24px;
  text-align: center;
  color: #fff;

  &.pending {
    background: linear-gradient(135deg, #ff976a 0%, #ff6b6b 100%);
  }

  &.confirmed {
    background: linear-gradient(135deg, #1989fa 0%, #4facfe 100%);
  }

  &.shipped {
    background: linear-gradient(135deg, #07c160 0%, #10b981 100%);
  }

  &.completed {
    background: linear-gradient(135deg, #07c160 0%, #10b981 100%);
  }

  &.cancelled {
    background: linear-gradient(135deg, #969799 0%, #c8c9cc 100%);
  }

  .status-icon {
    margin-bottom: 8px;
  }

  .status-text {
    font-size: 18px;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .status-desc {
    font-size: 12px;
    opacity: 0.8;
  }
}

.product-list {
  padding: 12px 16px;
}

.product-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }

  .product-info {
    flex: 1;
    margin-left: 12px;

    .product-name {
      font-size: 14px;
      color: #333;
      margin-bottom: 4px;
    }

    .product-spec {
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }

    .product-price {
      display: flex;
      align-items: center;
      gap: 8px;

      .price {
        font-size: 14px;
        color: #ee0a24;
      }

      .quantity {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .product-subtotal {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }
}

.total-amount {
  color: #ee0a24 !important;
  font-weight: 600 !important;
}

.logistics-content {
  .logistics-status {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .logistics-time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }

  .logistics-location {
    font-size: 12px;
    color: #666;
    margin-top: 2px;
  }
}

.record-user {
  font-size: 12px;
  color: #666;
}
</style>