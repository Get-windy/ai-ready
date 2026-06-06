<template>
  <div class="erp-page">
    <a-card title="库存管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="searchParams">
          <a-form-item label="商品编码">
            <a-input v-model:value="searchParams.productCode" placeholder="请输入商品编码" allow-clear />
          </a-form-item>
          <a-form-item label="商品名称">
            <a-input v-model:value="searchParams.productName" placeholder="请输入商品名称" allow-clear />
          </a-form-item>
          <a-form-item label="仓库">
            <a-select v-model:value="searchParams.warehouseId" placeholder="请选择" allow-clear style="width: 150px">
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleInbound">
            <template #icon><LoginOutlined /></template>
            入库
          </a-button>
          <a-button @click="handleOutbound">
            <template #icon><LogoutOutlined /></template>
            出库
          </a-button>
          <a-button @click="handleStocktake">
            <template #icon><AuditOutlined /></template>
            盘点
          </a-button>
          <a-button @click="handleExport">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quantity'">
            <span :class="getStockClass(record)">
              {{ record.quantity }} {{ record.unit }}
            </span>
          </template>
          <template v-else-if="column.key === 'warningStatus'">
            <a-tag v-if="record.quantity < (record.minStock ?? 0)" color="red">低库存</a-tag>
            <a-tag v-else-if="record.quantity > (record.maxStock ?? 999999)" color="orange">超储</a-tag>
            <a-tag v-else color="green">正常</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a @click="handleStockLog(record)">库存明细</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 库存明细弹窗 -->
    <a-modal v-model:open="logModalVisible" title="库存明细" :footer="null" width="800px">
      <a-table :columns="logColumns" :data-source="stockLogs" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'in' ? 'green' : 'red'">{{ record.type === 'in' ? '入库' : '出库' }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 库存详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="库存详情" width="700px" :footer="null">
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="商品编码">{{ currentRecord.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ currentRecord.productName }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ currentRecord.specification }}</a-descriptions-item>
        <a-descriptions-item label="单位">{{ currentRecord.unit }}</a-descriptions-item>
        <a-descriptions-item label="库存数量">
          <span :class="getStockClass(currentRecord)">{{ currentRecord.quantity }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="最低库存">{{ currentRecord.minStock }}</a-descriptions-item>
        <a-descriptions-item label="最高库存">{{ currentRecord.maxStock }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="最后入库">{{ currentRecord.lastInboundDate || '-' }}</a-descriptions-item>
        <a-descriptions-item label="最后出库">{{ currentRecord.lastOutboundDate || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined, AuditOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { stockApi } from '@/api/erp'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const logModalVisible = ref(false)
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const stockLogs = ref<any[]>([])
const warehouseOptions = ref<{ id: number; name: string }[]>([])

const searchParams = reactive({
  productCode: '',
  productName: '',
  warehouseId: undefined as number | undefined
})

const pagination = reactive({
  current: 1, pageSize: 10, total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 80 },
  { title: '库存数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '最低库存', dataIndex: 'minStock', key: 'minStock', width: 100 },
  { title: '最高库存', dataIndex: 'maxStock', key: 'maxStock', width: 100 },
  { title: '预警', key: 'warningStatus', width: 80 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName' },
  { title: '最后入库', dataIndex: 'lastInboundDate', key: 'lastInboundDate', width: 110 },
  { title: '最后出库', dataIndex: 'lastOutboundDate', key: 'lastOutboundDate', width: 110 },
  { title: '操作', key: 'action', fixed: 'right', width: 130 }
]

const logColumns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '关联单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' }
]

const getStockClass = (record: any) => ({
  'low-stock': record.quantity < (record.minStock ?? 0),
  'over-stock': record.quantity > (record.maxStock ?? 999999)
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await stockApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchParams
    })
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (err: any) {
    message.error(err?.message || '加载库存数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { Object.assign(searchParams, { productCode: '', productName: '', warehouseId: undefined }); handleSearch() }
const handleTableChange = (pag: any) => { pagination.current = pag.current; fetchData() }

const handleInbound = () => router.push('/erp/stock-in')
const handleOutbound = () => router.push('/erp/outbound')
const handleStocktake = () => router.push('/erp/stocktake')

const handleView = (record: any) => { currentRecord.value = record; detailVisible.value = true }

const handleStockLog = async (record: any) => {
  try {
    const res = await request.get(`/erp/stock/${record.id}/logs`)
    stockLogs.value = res?.data || []
    logModalVisible.value = true
  } catch { message.warning('暂无库存流水数据') }
}

const handleExport = async () => {
  try {
    const blob = await request.get('/erp/stock/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url; a.download = `库存_${new Date().toISOString().slice(0, 10)}.xlsx`; a.click()
    window.URL.revokeObjectURL(url); message.success('导出成功')
  } catch { message.warning('导出失败') }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.erp-page { padding: 24px; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.low-stock { color: #ff4d4f; font-weight: bold; }
.over-stock { color: #fa8c16; font-weight: bold; }
</style>
