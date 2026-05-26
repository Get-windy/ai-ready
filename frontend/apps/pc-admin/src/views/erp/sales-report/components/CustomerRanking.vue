<template>
  <div class="customer-ranking">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="queryParams.dateRange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="false"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'totalAmount'">
          ¥{{ record.totalAmount?.toFixed(2) }}
        </template>
        <template v-else-if="column.key === 'rank'">
          <a-tag :color="getRankColor(record.rank)">
            TOP {{ record.rank }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface CustomerRanking {
  id: number
  rank: number
  customerName: string
  totalAmount: number
  orderCount: number
  lastOrderDate: string
}

const loading = ref(false)
const dataSource = ref<CustomerRanking[]>([])

const queryParams = reactive({
  dateRange: [] as string[]
})

const columns = [
  { title: '排名', key: 'rank', width: 100 },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName' },
  { title: '销售总额', key: 'totalAmount', width: 150 },
  { title: '订单数量', dataIndex: 'orderCount', key: 'orderCount', width: 120 },
  { title: '最后订单日期', dataIndex: 'lastOrderDate', key: 'lastOrderDate', width: 150 }
]

const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'orange'
  if (rank === 3) return 'cyan'
  return 'default'
}

const handleQuery = () => {
  message.info('查询客户排行')
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      rank: 1,
      customerName: '客户A',
      totalAmount: 500000,
      orderCount: 50,
      lastOrderDate: '2026-04-13'
    },
    {
      id: 2,
      rank: 2,
      customerName: '客户B',
      totalAmount: 400000,
      orderCount: 40,
      lastOrderDate: '2026-04-12'
    },
    {
      id: 3,
      rank: 3,
      customerName: '客户C',
      totalAmount: 350000,
      orderCount: 35,
      lastOrderDate: '2026-04-11'
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.customer-ranking {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>