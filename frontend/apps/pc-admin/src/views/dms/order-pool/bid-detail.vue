<template>
  <ErrorBoundary>
    <PageContainer>
      <template #header>
        <a-space>
          <a-button @click="goBack">
            返回
          </a-button>
          <span style="font-weight: bold;">
            竞标详情 - 任务 {{ taskNo || ('#' + taskId) }}
          </span>
          <a-tag color="purple">
            价低优先
          </a-tag>
          <a-tag :color="POOL_STATUS_MAP[poolStatus]?.color || 'default'">
            池状态：{{ POOL_STATUS_MAP[poolStatus]?.label || '-' }}
          </a-tag>
        </a-space>
      </template>

      <a-table
        :data-source="dataSource"
        :columns="columns"
        :loading="loading"
        row-key="id"
        size="small"
        :pagination="{ pageSize: 20, showSizeChanger: true, showQuickJumper: true, showTotal: (t: number) => `共 ${t} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rank'">
            <a-tag
              v-if="record.isWin === 1"
              color="green"
            >
              中标
            </a-tag>
            <span v-else>{{ record.__rank }}</span>
          </template>
          <template v-else-if="column.key === 'bidPrice'">
            <span class="currency-value">{{ formatMoney(record.bidPrice) }}</span>
          </template>
          <template v-else-if="column.key === 'isWin'">
            <a-tag :color="record.isWin === 1 ? 'green' : 'default'">
              {{ record.isWin === 1 ? '中标' : '未中标' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              v-if="canCancel(record)"
              title="确定取消该配送员的出价？"
              @confirm="handleCancelBid(record)"
            >
              <a-button
                type="link"
                size="small"
                danger
              >
                取消出价
              </a-button>
            </a-popconfirm>
            <span v-else>-</span>
          </template>
        </template>
      </a-table>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { orderPoolApi, POOL_STATUS_MAP } from '@/api/dms/order-pool'

defineOptions({ name: 'DmsOrderPoolBidDetail' })

const route = useRoute()
const router = useRouter()

const poolId = route.query.poolId as string
const taskId = route.query.taskId as string
const taskNo = route.query.taskNo as string

const dataSource = ref<any[]>([])
const loading = ref(false)
/** 池状态：决定出价是否还能取消（仅「竞价中(1)」可撤） */
const poolStatus = ref<number | undefined>(undefined)

const columns = [
  { title: '名次', key: 'rank', width: 90 },
  { title: '配送员', dataIndex: 'riderName', key: 'riderName', width: 150 },
  { title: '报价', dataIndex: 'bidPrice', key: 'bidPrice', width: 120, align: 'right', sorter: (a: any, b: any) => Number(a.bidPrice) - Number(b.bidPrice) },
  { title: '报价时间', dataIndex: 'bidTime', key: 'bidTime', width: 180, sorter: (a: any, b: any) => String(a.bidTime).localeCompare(String(b.bidTime)) },
  { title: '中标结果', dataIndex: 'isWin', key: 'isWin', width: 110 },
  { title: '操作', key: 'action', width: 120 },
]

/**
 * 仅「竞价中」且未中标的出价可取消
 * （池一结算/下架/过期，报价即成为历史凭证：再撤会与中标结果和「竞价数」口径打架，后端同样拦截）
 */
function canCancel(record: any): boolean {
  return poolStatus.value === 1 && record.isWin !== 1
}

async function fetchData() {
  if (!poolId) {
    message.warning('缺少订单池ID')
    return
  }
  loading.value = true
  try {
    // 池状态（决定「取消出价」是否可用）
    try {
      const poolRes: any = await orderPoolApi.getById(Number(poolId))
      const pool = poolRes?.data ?? poolRes
      poolStatus.value = pool?.poolStatus
    } catch { /* 池信息拿不到时不阻断竞价列表 */ }
    // 后端 /bid-list（价低优先返回），修复历史 /bids 路径不一致
    const res: any = await orderPoolApi.bidList(Number(poolId))
    const list: any[] = res?.data ?? res ?? []
    list.forEach((b, i) => { b.__rank = i + 1 })
    dataSource.value = list
  } catch (e: any) {
    dataSource.value = []
    message.error(e?.response?.data?.message || '竞价记录加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCancelBid(record: any) {
  try {
    await orderPoolApi.cancelBid(Number(poolId), { bidId: record.id, riderId: record.riderId })
    message.success('已取消出价')
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '取消失败')
  }
}

function formatMoney(val: any): string {
  if (val === undefined || val === null || val === '') return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function goBack() {
  router.back()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
</style>
