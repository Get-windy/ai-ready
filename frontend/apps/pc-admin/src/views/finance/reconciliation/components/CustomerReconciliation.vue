<template>
  <div class="customer-reconciliation">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff">
        <div class="summary-card-title">对账客户数</div>
        <div class="summary-card-value">{{ summaryData.customerCount }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14">
        <div class="summary-card-title">应收总额</div>
        <div class="summary-card-value">¥{{ summaryData.totalReceivable.toFixed(2) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a">
        <div class="summary-card-title">已对账金额</div>
        <div class="summary-card-value">¥{{ summaryData.reconciledAmount.toFixed(2) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1">
        <div class="summary-card-title">差异金额</div>
        <div class="summary-card-value">¥{{ summaryData.differenceAmount.toFixed(2) }}</div>
      </div>
    </div>

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

    <VxeTableList
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="false"
      row-key="id"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
    >
      <template #systemAmountCell="{ record }">
        <span>¥{{ record.systemAmount?.toFixed(2) }}</span>
      </template>
      <template #customerAmountCell="{ record }">
        <span>¥{{ record.customerAmount?.toFixed(2) }}</span>
      </template>
      <template #differenceCell="{ record }">
        <span :style="{ color: record.difference !== 0 ? '#f5222d' : '#52c41a' }">
          ¥{{ record.difference?.toFixed(2) }}
        </span>
      </template>
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
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

const summaryData = reactive({
  customerCount: 35,
  totalReceivable: 689500.00,
  reconciledAmount: 672000.00,
  differenceAmount: 3500.00
})

const queryParams = reactive({
  customerName: ''
})

const columns = [
  { field: 'customerName', title: '客户名称' },
  { field: 'orderNo', title: '订单号' },
  { field: 'systemAmount', title: '系统金额', slotName: 'systemAmountCell' },
  { field: 'customerAmount', title: '客户金额', slotName: 'customerAmountCell' },
  { field: 'difference', title: '差异', slotName: 'differenceCell' }
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

/* 统计卡片样式 */
.summary-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.summary-card {
  flex: 1;
  min-width: 180px;
  padding: 16px 20px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--card-color), color-mix(in srgb, var(--card-color) 70%, white));
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.summary-card-title {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-card-value {
  font-size: 24px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
}

.filter-area {
  margin-bottom: 16px;
}

/* 网格边框样式 */

</style>
