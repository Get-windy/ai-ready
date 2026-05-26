<template>
  <div class="customer-reconciliation">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="客户">
          <a-input
            v-model:value="queryParams.customerName"
            placeholder="请输入客户名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleSearch"
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
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'

interface CustomerRecord {
  id: number
  customerName: string
  orderNo: string
  systemAmount: number
  customerAmount: number
  difference: number
}

const loading = ref(false)
const dataSource = ref<CustomerRecord[]>([])

const queryParams = reactive({
  customerName: ''
})

const columns = [
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '系统金额', dataIndex: 'systemAmount', key: 'systemAmount' },
  { title: '客户金额', dataIndex: 'customerAmount', key: 'customerAmount' },
  { title: '差异', dataIndex: 'difference', key: 'difference' }
]

const handleSearch = () => {
  message.info('查询客户对账记录')
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      customerName: '客户A',
      orderNo: 'SO20260328001',
      systemAmount: 10000,
      customerAmount: 10000,
      difference: 0
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.customer-reconciliation {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>