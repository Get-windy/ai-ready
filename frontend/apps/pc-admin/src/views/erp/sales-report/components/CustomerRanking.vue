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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { salesReportApi, type CustomerRankItem } from '@/api/sales-report'

const loading = ref(false)
const dataSource = ref<CustomerRankItem[]>([])
const queryParams = reactive({ dateRange: [] as string[] })

const columns = [
  { title: '排名', key: 'rank', width: 100 },
  { title: '客户名称', dataIndex: 'name', key: 'name' },
  { title: '销售总额', key: 'totalAmount', width: 150 },
  { title: '订单数量', dataIndex: 'orderCount', key: 'orderCount', width: 120 },
  { title: '增长率', key: 'growth', width: 100 }
]

const getRankColor = (rank: number) => rank === 1 ? 'gold' : rank === 2 ? 'orange' : rank === 3 ? 'cyan' : 'default'

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await salesReportApi.getCustomerRanking(
      queryParams.dateRange.length === 2 ? { startDate: queryParams.dateRange[0], endDate: queryParams.dateRange[1] } : {})
    dataSource.value = res.data || []
  } catch { message.info('查询失败') }
  finally { loading.value = false }
}

onMounted(() => handleQuery())
</script>

<style scoped>
.customer-ranking {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>