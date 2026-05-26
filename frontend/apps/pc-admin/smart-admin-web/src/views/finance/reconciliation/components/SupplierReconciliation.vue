<template>
  <div class="supplier-reconciliation">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="供应商">
          <a-input
            v-model:value="queryParams.supplierName"
            placeholder="请输入供应商名称"
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

interface SupplierRecord {
  id: number
  supplierName: string
  orderNo: string
  systemAmount: number
  supplierAmount: number
  difference: number
}

const loading = ref(false)
const dataSource = ref<SupplierRecord[]>([])

const queryParams = reactive({
  supplierName: ''
})

const columns = [
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '系统金额', dataIndex: 'systemAmount', key: 'systemAmount' },
  { title: '供应商金额', dataIndex: 'supplierAmount', key: 'supplierAmount' },
  { title: '差异', dataIndex: 'difference', key: 'difference' }
]

const handleSearch = () => {
  message.info('查询供应商对账记录')
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      supplierName: '供应商A',
      orderNo: 'PO20260328001',
      systemAmount: 8000,
      supplierAmount: 8000,
      difference: 0
    }
  ]
  loading.value = false
}, 500)
</script>

<style scoped>
.supplier-reconciliation {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}
</style>