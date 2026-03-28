<template>
  <div class="sale-order-page">
    <a-card title="销售订单管理">
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleEdit(record)">编辑</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

const loading = ref(false)
const dataSource = ref([
  { id: 1, orderNo: 'SO20260328001', customerName: '客户A', orderDate: '2026-03-28', status: 0, totalAmount: 10000 },
  { id: 2, orderNo: 'SO20260328002', customerName: '客户B', orderDate: '2026-03-28', status: 1, totalAmount: 20000 }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 2
})

const columns = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate' },
  { title: '订单金额', dataIndex: 'totalAmount', key: 'totalAmount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
]

const getStatusColor = (status: number) => status === 0 ? 'blue' : 'green'
const getStatusText = (status: number) => status === 0 ? '待处理' : '已完成'

const handleView = (record: any) => console.log('view', record)
const handleEdit = (record: any) => console.log('edit', record)

onMounted(() => {})
</script>