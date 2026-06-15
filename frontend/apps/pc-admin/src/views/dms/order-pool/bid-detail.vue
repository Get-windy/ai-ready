<template>
  <ErrorBoundary>
    <PageContainer>
      <template #header>
        <a-space>
          <a-button @click="goBack">返回</a-button>
          <span style="font-weight: bold;">
            竞标详情 - 任务 #{{ taskId }}
          </span>
        </a-space>
      </template>

      <a-table
        :data-source="dataSource"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="{ pageSize: 20 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'is_win'">
            <a-tag :color="record.is_win ? 'green' : 'default'">
              {{ record.is_win ? '中标' : '未中标' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { request } from '@/utils/request'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

const route = useRoute()
const router = useRouter()

const poolId = route.query.poolId as string
const taskId = route.query.taskId as string

const dataSource = ref<any[]>([])
const loading = ref(false)

const columns = [
  { title: '骑手名称', dataIndex: 'rider_name', key: 'rider_name', width: 150 },
  { title: '竞标价格', dataIndex: 'bid_price', key: 'bid_price', width: 120 },
  { title: '竞标时间', dataIndex: 'bid_time', key: 'bid_time', width: 170 },
  { title: '是否中标', dataIndex: 'is_win', key: 'is_win', width: 100 }
]

const fetchData = async () => {
  if (!poolId) return
  loading.value = true
  try {
    const res = await request.get(`/dms/order-pool/${poolId}/bids`)
    dataSource.value = res?.data ?? res ?? []
  } catch {
    dataSource.value = []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
</style>
