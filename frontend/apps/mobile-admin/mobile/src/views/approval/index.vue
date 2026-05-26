<template>
  <div class="approval-page">
    <van-search
      v-model="searchText"
      placeholder="搜索审批"
      show-action
      @search="onSearch"
      @cancel="onCancel"
    />

    <van-tabs v-model:active="activeTab" sticky @change="onTabChange">
      <van-tab title="待审批" :badge="pendingCount">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="finished"
            finished-text="没有更多了"
            @load="onLoad"
          >
            <van-cell-group inset>
              <van-swipe-cell v-for="item in approvalList" :key="item.id">
                <van-cell
                  :title="item.title"
                  :label="item.createTime"
                  is-link
                  @click="goDetail(item)"
                >
                  <template #icon>
                    <div class="approval-icon" :class="getTypeClass(item.type)">
                      <van-icon :name="getTypeIcon(item.type)" />
                    </div>
                  </template>
                  <template #value>
                    <div class="approval-info">
                      <div class="approval-amount">{{ item.amount }}</div>
                      <van-tag :type="getStatusType(item.status)">
                        {{ item.statusLabel }}
                      </van-tag>
                    </div>
                  </template>
                  <template #title>
                    <div class="approval-title">
                      <span>{{ item.title }}</span>
                      <van-tag plain size="small">{{ item.typeLabel }}</van-tag>
                    </div>
                  </template>
                </van-cell>
                <template #right>
                  <van-button square type="primary" text="通过" class="swipe-btn" @click="handleApprove(item)" />
                  <van-button square type="danger" text="拒绝" class="swipe-btn" @click="handleReject(item)" />
                </template>
              </van-swipe-cell>
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>

      <van-tab title="已审批">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="finished"
            finished-text="没有更多了"
            @load="onLoad"
          >
            <van-cell-group inset>
              <van-cell
                v-for="item in approvedList"
                :key="item.id"
                :title="item.title"
                :label="item.approveTime"
                is-link
                @click="goDetail(item)"
              >
                <template #icon>
                  <div class="approval-icon" :class="getTypeClass(item.type)">
                    <van-icon :name="getTypeIcon(item.type)" />
                  </div>
                </template>
                <template #value>
                  <div class="approval-result" :class="item.approveResult">
                    {{ item.approveResult === 'approved' ? '已通过' : '已拒绝' }}
                  </div>
                </template>
              </van-cell>
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>

      <van-tab title="我发起的">
        <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
          <van-list
            v-model:loading="loading"
            :finished="finished"
            finished-text="没有更多了"
            @load="onLoad"
          >
            <van-cell-group inset>
              <van-cell
                v-for="item in myApprovalList"
                :key="item.id"
                :title="item.title"
                :label="item.createTime"
                is-link
                @click="goDetail(item)"
              >
                <template #icon>
                  <div class="approval-icon" :class="getTypeClass(item.type)">
                    <van-icon :name="getTypeIcon(item.type)" />
                  </div>
                </template>
                <template #value>
                  <van-tag :type="getStatusType(item.status)">
                    {{ item.statusLabel }}
                  </van-tag>
                </template>
              </van-cell>
            </van-cell-group>
          </van-list>
        </van-pull-refresh>
      </van-tab>
    </van-tabs>

    <van-action-bar>
      <van-action-bar-button type="default" text="筛选" icon="filter-o" @click="showFilter = true" />
    </van-action-bar>

    <van-popup v-model:show="showFilter" position="bottom" round>
      <div class="filter-popup">
        <div class="filter-header">
          <span>筛选条件</span>
          <van-icon name="cross" @click="showFilter = false" />
        </div>
        <van-cell-group>
          <van-field label="审批类型">
            <template #input>
              <van-dropdown-menu>
                <van-dropdown-item v-model="filter.type" :options="typeOptions" />
              </van-dropdown-menu>
            </template>
          </van-field>
          <van-field label="时间范围">
            <template #input>
              <van-dropdown-menu>
                <van-dropdown-item v-model="filter.timeRange" :options="timeRangeOptions" />
              </van-dropdown-menu>
            </template>
          </van-field>
          <van-field label="申请人">
            <template #input>
              <van-dropdown-menu>
                <van-dropdown-item v-model="filter.applicant" :options="applicantOptions" />
              </van-dropdown-menu>
            </template>
          </van-field>
        </van-cell-group>
        <div class="filter-actions">
          <van-button block type="default" @click="resetFilter">重置</van-button>
          <van-button block type="primary" @click="applyFilter">确定</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'

const router = useRouter()

const searchText = ref('')
const activeTab = ref(0)
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)
const showFilter = ref(false)
const pendingCount = ref(5)

const filter = reactive({
  type: 0,
  timeRange: 0,
  applicant: 0
})

const typeOptions = [
  { text: '全部类型', value: 0 },
  { text: '采购审批', value: 1 },
  { text: '销售审批', value: 2 },
  { text: '费用报销', value: 3 },
  { text: '请假申请', value: 4 },
  { text: '合同审批', value: 5 }
]

const timeRangeOptions = [
  { text: '全部时间', value: 0 },
  { text: '今天', value: 1 },
  { text: '本周', value: 2 },
  { text: '本月', value: 3 }
]

const applicantOptions = [
  { text: '全部申请人', value: 0 },
  { text: '张三', value: 1 },
  { text: '李四', value: 2 },
  { text: '王五', value: 3 }
]

const approvalList = ref<any[]>([])
const approvedList = ref<any[]>([])
const myApprovalList = ref<any[]>([])

onMounted(() => {
  loadData()
})

const loadData = () => {
  approvalList.value = [
    { id: 1, title: '采购订单审批', type: 'purchase', typeLabel: '采购', amount: '¥58,000', status: 'pending', statusLabel: '待审批', createTime: '2024-01-15 10:30', applicant: '张三' },
    { id: 2, title: '差旅费用报销', type: 'expense', typeLabel: '报销', amount: '¥3,500', status: 'pending', statusLabel: '待审批', createTime: '2024-01-15 09:20', applicant: '李四' },
    { id: 3, title: '销售合同审批', type: 'contract', typeLabel: '合同', amount: '¥128,000', status: 'pending', statusLabel: '待审批', createTime: '2024-01-15 08:15', applicant: '王五' },
    { id: 4, title: '年假申请', type: 'leave', typeLabel: '请假', amount: '3天', status: 'pending', statusLabel: '待审批', createTime: '2024-01-14 16:30', applicant: '赵六' },
    { id: 5, title: '付款申请', type: 'payment', typeLabel: '付款', amount: '¥25,000', status: 'pending', statusLabel: '待审批', createTime: '2024-01-14 14:20', applicant: '钱七' }
  ]

  approvedList.value = [
    { id: 6, title: '采购订单审批', type: 'purchase', typeLabel: '采购', amount: '¥32,000', approveResult: 'approved', approveTime: '2024-01-14 15:30' },
    { id: 7, title: '费用报销', type: 'expense', typeLabel: '报销', amount: '¥1,200', approveResult: 'rejected', approveTime: '2024-01-14 11:20' }
  ]

  myApprovalList.value = [
    { id: 8, title: '销售报价审批', type: 'quotation', typeLabel: '报价', amount: '¥85,000', status: 'approved', statusLabel: '已通过', createTime: '2024-01-13 10:00' },
    { id: 9, title: '请假申请', type: 'leave', typeLabel: '请假', amount: '2天', status: 'pending', statusLabel: '审批中', createTime: '2024-01-12 09:30' }
  ]

  pendingCount.value = approvalList.value.length
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

const onTabChange = () => {
  loadData()
}

const getTypeClass = (type: string) => {
  const classes: Record<string, string> = {
    purchase: 'type-purchase',
    expense: 'type-expense',
    contract: 'type-contract',
    leave: 'type-leave',
    payment: 'type-payment',
    quotation: 'type-quotation'
  }
  return classes[type] || 'type-default'
}

const getTypeIcon = (type: string) => {
  const icons: Record<string, string> = {
    purchase: 'shopping-cart-o',
    expense: 'balance-list-o',
    contract: 'description',
    leave: 'clock-o',
    payment: 'paid',
    quotation: 'notes-o'
  }
  return icons[type] || 'todo-list-o'
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return types[status] || 'default'
}

const goDetail = (item: any) => {
  router.push(`/approval/${item.id}`)
}

const handleApprove = async (item: any) => {
  try {
    await showConfirmDialog({
      title: '确认通过',
      message: `确定要通过"${item.title}"吗？`
    })
    showSuccessToast('审批通过')
    loadData()
  } catch {
    // 用户取消
  }
}

const handleReject = async (item: any) => {
  try {
    await showConfirmDialog({
      title: '确认拒绝',
      message: `确定要拒绝"${item.title}"吗？`
    })
    showSuccessToast('已拒绝')
    loadData()
  } catch {
    // 用户取消
  }
}

const resetFilter = () => {
  filter.type = 0
  filter.timeRange = 0
  filter.applicant = 0
}

const applyFilter = () => {
  showFilter.value = false
  loadData()
}
</script>

<style scoped lang="scss">
.approval-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

.approval-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 18px;

  &.type-purchase {
    background: #e8f4ff;
    color: #1989fa;
  }

  &.type-expense {
    background: #fff7e8;
    color: #ff976a;
  }

  &.type-contract {
    background: #e8ffea;
    color: #07c160;
  }

  &.type-leave {
    background: #f5f5f5;
    color: #969799;
  }

  &.type-payment {
    background: #ffe8e8;
    color: #ee0a24;
  }

  &.type-quotation {
    background: #f0e8ff;
    color: #7232dd;
  }
}

.approval-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.approval-info {
  text-align: right;

  .approval-amount {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }
}

.approval-result {
  font-size: 14px;
  font-weight: 500;

  &.approved {
    color: #07c160;
  }

  &.rejected {
    color: #ee0a24;
  }
}

.swipe-btn {
  height: 100%;
}

.filter-popup {
  padding: 16px;

  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    margin-bottom: 16px;
  }

  .filter-actions {
    display: flex;
    gap: 12px;
    margin-top: 16px;
  }
}
</style>