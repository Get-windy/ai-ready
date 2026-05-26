<template>
  <div class="product-ranking">
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

interface ProductRanking {
  id: number
  rank: number
  productName: string
  productCode: string
  totalAmount: number
  quantity: number
  unitPrice: number
}

const loading = ref(false)
const dataSource = ref<ProductRanking[]>([])

const queryParams = reactive({
  dateRange: [] as string[]
})

const columns = [
  { title: '排名', key: 'rank', width: 100 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '销售总额', key: 'totalAmount', width: 150 },
  { title: '销售数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 120 }
]

const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'orange'
  if (rank === 3) return 'cyan'
  return 'default'
}

const handleQuery = () => {
  message.info('查询商品排行')
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      rank: 1,
      productName: '商品A',
      productCode: 'P001',
      totalAmount: 300000,
      quantity: 1000,
      unitPrice: 300
    },
    {
      id: 2,
      rank: 2,
      productName: '商品B',
      productCode: 'P002',
      totalAmount: 250000,
      quantity: 500,
      unitPrice: 500
    },
    {
      id: 3,
      rank: 3,
      productName: '商品C',
      productCode: 'P003',
      totalAmount: 200000,
      quantity: 800,
      unitPrice: 250
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.product-ranking {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>