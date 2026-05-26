<template>
  <div class="order-page">
    <van-search
      v-model="searchText"
      placeholder="搜索订单号/客户名称"
      show-action
      @search="onSearch"
      @cancel="onCancel"
    />

    <van-dropdown-menu>
      <van-dropdown-item v-model="filter.status" :options="statusOptions" title="订单状态" />
      <van-dropdown-item v-model="filter.dateRange" :options="dateRangeOptions" title="时间范围" />
    </van-dropdown-menu>

    <van-tabs v-model:active="activeTab" sticky @change="onTabChange">
      <van-tab title="全部">
        <order-list :orders="orderList" @click="goDetail" />
      </van-tab>
      <van-tab title="待确认">
        <order-list :orders="pendingOrders" @click="goDetail" />
      </van-tab>
      <van-tab title="待发货">
        <order-list :orders="toShipOrders" @click="goDetail" />
      </van-tab>
      <van-tab title="待收款">
        <order-list :orders="toPayOrders" @click="goDetail" />
      </van-tab>
    </van-tabs>

    <van-floating-bubble
      icon="plus"
      @click="goAdd"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const searchText = ref('')
const activeTab = ref(0)

const filter = reactive({
  status: 0,
  dateRange: 0
})

const statusOptions = [
  { text: '全部状态', value: 0 },
  { text: '待确认', value: 1 },
  { text: '已确认', value: 2 },
  { text: '已发货', value: 3 },
  { text: '已完成', value: 4 },
  { text: '已取消', value: 5 }
]

const dateRangeOptions = [
  { text: '全部时间', value: 0 },
  { text: '今天', value: 1 },
  { text: '本周', value: 2 },
  { text: '本月', value: 3 }
]

const orderList = ref<any[]>([])

const pendingOrders = computed(() => orderList.value.filter(o => o.status === 'pending'))
const toShipOrders = computed(() => orderList.value.filter(o => o.status === 'confirmed'))
const toPayOrders = computed(() => orderList.value.filter(o => o.status === 'shipped'))

onMounted(() => {
  loadData()
})

const loadData = () => {
  orderList.value = [
    { id: 1, orderNo: 'SO20240115001', customerName: '北京科技有限公司', amount: '¥58,000', status: 'pending', statusLabel: '待确认', createTime: '2024-01-15 10:30', itemCount: 5 },
    { id: 2, orderNo: 'SO20240115002', customerName: '上海贸易公司', amount: '¥32,500', status: 'confirmed', statusLabel: '已确认', createTime: '2024-01-15 09:20', itemCount: 3 },
    { id: 3, orderNo: 'SO20240114003', customerName: '广州制造企业', amount: '¥128,000', status: 'shipped', statusLabel: '已发货', createTime: '2024-01-14 16:45', itemCount: 8 },
    { id: 4, orderNo: 'SO20240114004', customerName: '深圳电子公司', amount: '¥45,800', status: 'completed', statusLabel: '已完成', createTime: '2024-01-14 14:30', itemCount: 4 },
    { id: 5, orderNo: 'SO20240113005', customerName: '杭州互联网公司', amount: '¥18,200', status: 'pending', statusLabel: '待确认', createTime: '2024-01-13 11:20', itemCount: 2 }
  ]
}

const onSearch = () => {
  loadData()
}

const onCancel = () => {
  searchText.value = ''
  loadData()
}

const onTabChange = () => {
  // Tab切换时重新加载数据
}

const goDetail = (order: any) => {
  router.push(`/order/${order.id}`)
}

const goAdd = () => {
  router.push('/order/add')
}
</script>

<script lang="ts">
import { defineComponent, h } from 'vue'
import { Cell, CellGroup, Tag, Image } from 'vant'

const OrderList = defineComponent({
  name: 'OrderList',
  props: {
    orders: {
      type: Array,
      default: () => []
    }
  },
  emits: ['click'],
  setup(props, { emit }) {
    const getStatusType = (status: string) => {
      const types: Record<string, string> = {
        pending: 'warning',
        confirmed: 'primary',
        shipped: 'success',
        completed: 'success',
        cancelled: 'danger'
      }
      return types[status] || 'default'
    }

    return () => h('div', { class: 'order-list' }, [
      h(CellGroup, { inset: true }, () => 
        props.orders.map((order: any) => 
          h(Cell, {
            key: order.id,
            title: order.orderNo,
            label: order.createTime,
            isLink: true,
            onClick: () => emit('click', order)
          }, {
            icon: () => h('div', { class: 'order-icon' }, [
              h('van-icon', { name: 'orders-o', size: '20' })
            ]),
            title: () => h('div', { class: 'order-title' }, [
              h('span', { class: 'order-no' }, order.orderNo),
              h(Tag, { type: getStatusType(order.status), size: 'small' }, () => order.statusLabel)
            ]),
            value: () => h('div', { class: 'order-value' }, [
              h('div', { class: 'order-amount' }, order.amount),
              h('div', { class: 'order-customer' }, order.customerName),
              h('div', { class: 'order-items' }, `${order.itemCount}件商品`)
            ])
          })
        )
      )
    ])
  }
})

export default {
  components: { OrderList }
}
</script>

<style scoped lang="scss">
.order-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.order-list {
  padding-top: 12px;
}

:deep(.order-icon) {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #e8f4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #1989fa;
}

:deep(.order-title) {
  display: flex;
  align-items: center;
  gap: 8px;

  .order-no {
    font-size: 15px;
    font-weight: 500;
  }
}

:deep(.order-value) {
  text-align: right;

  .order-amount {
    font-size: 15px;
    font-weight: 600;
    color: #ee0a24;
  }

  .order-customer {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
  }

  .order-items {
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }
}
</style>