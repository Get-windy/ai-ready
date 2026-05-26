<template>
  <div class="customer-page">
    <van-search
      v-model="searchText"
      placeholder="搜索客户名称/联系人/电话"
      show-action
      @search="onSearch"
      @cancel="onCancel"
    />

    <van-dropdown-menu>
      <van-dropdown-item v-model="filter.level" :options="levelOptions" title="客户等级" />
      <van-dropdown-item v-model="filter.status" :options="statusOptions" title="客户状态" />
      <van-dropdown-item v-model="filter.source" :options="sourceOptions" title="客户来源" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-cell-group inset>
          <van-cell
            v-for="customer in customerList"
            :key="customer.id"
            is-link
            @click="goDetail(customer)"
          >
            <template #icon>
              <van-image
                round
                width="40"
                height="40"
                :src="customer.avatar || defaultAvatar"
              />
            </template>
            <template #title>
              <div class="customer-title">
                <span class="customer-name">{{ customer.name }}</span>
                <van-tag :type="getLevelType(customer.level)" size="small">
                  {{ customer.levelLabel }}
                </van-tag>
              </div>
            </template>
            <template #label>
              <div class="customer-info">
                <div class="info-row">
                  <van-icon name="user-o" />
                  <span>{{ customer.contact }}</span>
                </div>
                <div class="info-row">
                  <van-icon name="phone-o" />
                  <span>{{ customer.phone }}</span>
                </div>
              </div>
            </template>
            <template #value>
              <div class="customer-stats">
                <div class="stat-item">
                  <span class="stat-value">{{ customer.orderCount }}</span>
                  <span class="stat-label">订单</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ customer.totalAmount }}</span>
                  <span class="stat-label">金额</span>
                </div>
              </div>
            </template>
          </van-cell>
        </van-cell-group>
      </van-list>
    </van-pull-refresh>

    <van-floating-bubble
      icon="plus"
      @click="goAdd"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const searchText = ref('')
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)

const filter = reactive({
  level: 0,
  status: 0,
  source: 0
})

const levelOptions = [
  { text: '全部等级', value: 0 },
  { text: 'A类客户', value: 1 },
  { text: 'B类客户', value: 2 },
  { text: 'C类客户', value: 3 },
  { text: '潜在客户', value: 4 }
]

const statusOptions = [
  { text: '全部状态', value: 0 },
  { text: '活跃', value: 1 },
  { text: '沉默', value: 2 },
  { text: '流失', value: 3 }
]

const sourceOptions = [
  { text: '全部来源', value: 0 },
  { text: '线上推广', value: 1 },
  { text: '线下活动', value: 2 },
  { text: '客户转介', value: 3 },
  { text: '主动咨询', value: 4 }
]

const customerList = ref<any[]>([])

onMounted(() => {
  loadData()
})

const loadData = () => {
  customerList.value = [
    { id: 1, name: '北京科技有限公司', contact: '张经理', phone: '138****1234', level: 1, levelLabel: 'A类', status: 1, orderCount: 28, totalAmount: '¥258万', avatar: '' },
    { id: 2, name: '上海贸易公司', contact: '李总', phone: '139****5678', level: 2, levelLabel: 'B类', status: 1, orderCount: 15, totalAmount: '¥128万', avatar: '' },
    { id: 3, name: '广州制造企业', contact: '王主任', phone: '137****9012', level: 1, levelLabel: 'A类', status: 1, orderCount: 42, totalAmount: '¥520万', avatar: '' },
    { id: 4, name: '深圳电子公司', contact: '赵总', phone: '136****3456', level: 3, levelLabel: 'C类', status: 2, orderCount: 5, totalAmount: '¥35万', avatar: '' },
    { id: 5, name: '杭州互联网公司', contact: '钱经理', phone: '135****7890', level: 2, levelLabel: 'B类', status: 1, orderCount: 18, totalAmount: '¥96万', avatar: '' }
  ]
}

const onRefresh = async () => {
  await loadData()
  refreshing.value = false
}

const onLoad = () => {
  loading.value = false
  finished.value = true
}

const onSearch = () => {
  loadData()
}

const onCancel = () => {
  searchText.value = ''
  loadData()
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

const goDetail = (customer: any) => {
  router.push(`/customer/${customer.id}`)
}

const goAdd = () => {
  router.push('/customer/add')
}
</script>

<style scoped lang="scss">
.customer-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 80px;
}

.customer-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .customer-name {
    font-size: 15px;
    font-weight: 500;
  }
}

.customer-info {
  margin-top: 4px;

  .info-row {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }
}

.customer-stats {
  display: flex;
  gap: 12px;
  text-align: center;

  .stat-item {
    display: flex;
    flex-direction: column;

    .stat-value {
      font-size: 14px;
      font-weight: 500;
      color: #333;
    }

    .stat-label {
      font-size: 10px;
      color: #999;
    }
  }
}
</style>