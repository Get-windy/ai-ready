<template>
  <div class="customer-detail-page">
    <van-nav-bar
      title="客户详情"
      left-arrow
      @click-left="goBack"
    >
      <template #right>
        <van-icon name="edit" size="18" @click="goEdit" />
      </template>
    </van-nav-bar>

    <div class="detail-content" v-if="customerDetail">
      <div class="customer-header">
        <van-image
          round
          width="64"
          height="64"
          :src="customerDetail.avatar || defaultAvatar"
        />
        <div class="customer-basic">
          <div class="customer-name">{{ customerDetail.name }}</div>
          <van-tag :type="getLevelType(customerDetail.level)">
            {{ customerDetail.levelLabel }}
          </van-tag>
        </div>
        <div class="customer-actions">
          <van-icon name="phone-o" size="24" @click="callCustomer" />
          <van-icon name="chat-o" size="24" @click="sendMessage" />
        </div>
      </div>

      <van-cell-group inset title="基本信息">
        <van-cell title="客户名称" :value="customerDetail.name" />
        <van-cell title="客户等级" :value="customerDetail.levelLabel" />
        <van-cell title="客户状态" :value="customerDetail.statusLabel" />
        <van-cell title="客户来源" :value="customerDetail.sourceLabel" />
        <van-cell title="所属行业" :value="customerDetail.industry" />
        <van-cell title="公司规模" :value="customerDetail.scale" />
      </van-cell-group>

      <van-cell-group inset title="联系信息">
        <van-cell title="联系人" :value="customerDetail.contact" />
        <van-cell title="联系电话" :value="customerDetail.phone" is-link @click="callCustomer" />
        <van-cell title="电子邮箱" :value="customerDetail.email" />
        <van-cell title="公司地址" :value="customerDetail.address" is-link @click="openMap" />
      </van-cell-group>

      <van-cell-group inset title="业务统计">
        <van-grid :column-num="4" :border="false">
          <van-grid-item>
            <div class="stat-value">{{ customerDetail.orderCount }}</div>
            <div class="stat-label">订单数</div>
          </van-grid-item>
          <van-grid-item>
            <div class="stat-value">{{ customerDetail.totalAmount }}</div>
            <div class="stat-label">总金额</div>
          </van-grid-item>
          <van-grid-item>
            <div class="stat-value">{{ customerDetail.paidAmount }}</div>
            <div class="stat-label">已收款</div>
          </van-grid-item>
          <van-grid-item>
            <div class="stat-value">{{ customerDetail.unpaidAmount }}</div>
            <div class="stat-label">待收款</div>
          </van-grid-item>
        </van-grid>
      </van-cell-group>

      <van-cell-group inset title="最近订单">
        <van-cell
          v-for="order in customerDetail.recentOrders"
          :key="order.id"
          :title="order.orderNo"
          :label="order.createTime"
          is-link
          @click="goOrderDetail(order)"
        >
          <template #value>
            <div class="order-info">
              <div class="order-amount">{{ order.amount }}</div>
              <van-tag :type="getOrderStatusType(order.status)" size="small">
                {{ order.statusLabel }}
              </van-tag>
            </div>
          </template>
        </van-cell>
        <van-cell v-if="customerDetail.recentOrders?.length === 0" title="暂无订单" />
      </van-cell-group>

      <van-cell-group inset title="跟进记录">
        <van-cell
          v-for="record in customerDetail.followRecords"
          :key="record.id"
          :title="record.content"
          :label="record.createTime"
        >
          <template #icon>
            <van-icon name="clock-o" class="record-icon" />
          </template>
        </van-cell>
        <van-cell v-if="customerDetail.followRecords?.length === 0" title="暂无跟进记录" />
      </van-cell-group>
    </div>

    <van-action-bar>
      <van-action-bar-button type="default" text="添加跟进" icon="edit" @click="addFollow" />
      <van-action-bar-button type="primary" text="新建订单" icon="orders-o" @click="createOrder" />
    </van-action-bar>

    <van-popup v-model:show="showMessagePopup" position="bottom" round>
      <div class="message-popup">
        <div class="popup-header">
          <span>发送消息</span>
          <van-icon name="cross" @click="showMessagePopup = false" />
        </div>
        <div class="recipient-info">
          收信人: {{ customerDetail?.name }}
        </div>
        <van-field
          v-model="messageTitle"
          placeholder="消息标题（可选）"
        />
        <van-field
          v-model="messageContent"
          rows="4"
          autosize
          type="textarea"
          placeholder="请输入消息内容"
        />
        <div class="popup-actions">
          <van-button block type="primary" @click="confirmSendMessage">发送</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showSuccessToast, showDialog } from 'vant'

const router = useRouter()
const route = useRoute()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const customerDetail = ref<any>(null)
const showMessagePopup = ref(false)
const messageTitle = ref('')
const messageContent = ref('')

onMounted(() => {
  loadDetail()
})

const loadDetail = () => {
  const id = route.params.id
  customerDetail.value = {
    id,
    name: '北京科技有限公司',
    level: 1,
    levelLabel: 'A类客户',
    status: 1,
    statusLabel: '活跃',
    source: 1,
    sourceLabel: '线上推广',
    industry: '信息技术',
    scale: '50-100人',
    contact: '张经理',
    phone: '13800123456',
    email: 'zhangsan@example.com',
    address: '北京市朝阳区建国路88号',
    avatar: '',
    orderCount: 28,
    totalAmount: '¥258万',
    paidAmount: '¥220万',
    unpaidAmount: '¥38万',
    recentOrders: [
      { id: 1, orderNo: 'SO20240115001', amount: '¥58,000', status: 'shipped', statusLabel: '已发货', createTime: '2024-01-15' },
      { id: 2, orderNo: 'SO20240110002', amount: '¥32,500', status: 'completed', statusLabel: '已完成', createTime: '2024-01-10' },
      { id: 3, orderNo: 'SO20240105003', amount: '¥45,800', status: 'completed', statusLabel: '已完成', createTime: '2024-01-05' }
    ],
    followRecords: [
      { id: 1, content: '电话沟通，客户对新产品感兴趣，约定下周上门演示', createTime: '2024-01-15 14:30' },
      { id: 2, content: '发送产品报价单和公司介绍资料', createTime: '2024-01-12 10:20' },
      { id: 3, content: '初次联系，了解客户基本需求', createTime: '2024-01-10 09:00' }
    ]
  }
}

const getLevelType = (level: number) => {
  const types: Record<number, string> = {
    1: 'danger',
    2: 'warning',
    3: 'primary',
    4: 'default'
  }
  return types[level] || 'default'
}

const getOrderStatusType = (status: string) => {
  const types: Record<string, string> = {
    pending: 'warning',
    confirmed: 'primary',
    shipped: 'success',
    completed: 'success',
    cancelled: 'danger'
  }
  return types[status] || 'default'
}

const goBack = () => router.back()
const goEdit = () => router.push(`/customer/${customerDetail.value.id}/edit`)

const callCustomer = () => {
  showToast(`拨打电话: ${customerDetail.value.phone}`)
}

const sendMessage = () => {
  messageTitle.value = ''
  messageContent.value = ''
  showMessagePopup.value = true
}

const confirmSendMessage = () => {
  if (!messageContent.value.trim()) {
    showToast('请输入消息内容')
    return
  }
  showSuccessToast('消息已发送')
  showMessagePopup.value = false
}

const openMap = () => {
  const address = customerDetail.value?.address || ''
  const encodedAddress = encodeURIComponent(address)
  showDialog({
    title: '导航到地址',
    message: address,
    confirmButtonText: '打开地图',
    showCancelButton: true
  }).then(() => {
    window.open(`https://uri.amap.com/marker?position=&name=${encodedAddress}`, '_blank')
    showSuccessToast('已打开地图')
  }).catch(() => {})
}

const goOrderDetail = (order: any) => {
  router.push(`/order/${order.id}`)
}

const addFollow = () => {
  router.push(`/customer/${customerDetail.value.id}/follow`)
}

const createOrder = () => {
  router.push(`/order/add?customerId=${customerDetail.value.id}`)
}
</script>

<style scoped lang="scss">
.customer-detail-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

.customer-header {
  display: flex;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  .customer-basic {
    flex: 1;
    margin-left: 16px;

    .customer-name {
      font-size: 18px;
      font-weight: 500;
      margin-bottom: 8px;
    }
  }

  .customer-actions {
    display: flex;
    gap: 16px;
  }
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.order-info {
  text-align: right;

  .order-amount {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }
}

.record-icon {
  margin-right: 8px;
  color: #1989fa;
}

.message-popup {
  padding: 16px;

  .popup-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 16px;
  }

  .recipient-info {
    font-size: 14px;
    color: #666;
    padding: 8px 16px;
    margin-bottom: 8px;
    background: #f7f8fa;
    border-radius: 8px;
  }

  .popup-actions {
    margin-top: 16px;
  }
}
</style>