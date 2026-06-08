<template>
  <div class="erp-page" style="padding: 16px; height: 100%; display: flex; flex-direction: column;">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <DatabaseOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">总库存SKU</div>
            <div class="summary-value">{{ statistics.totalSku }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #f5222d 0%, #cf1322 100%);">
            <AlertOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">低库存预警</div>
            <div class="summary-value warning">{{ statistics.lowStockCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);">
            <ExclamationCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">超储预警</div>
            <div class="summary-value warning">{{ statistics.overStockCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">正常库存</div>
            <div class="summary-value">{{ statistics.normalCount }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="库存管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
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
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        style="flex: 1; overflow: auto;"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else-if="column.key === 'quantity'">
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined, AuditOutlined, ExportOutlined, DatabaseOutlined, AlertOutlined, ExclamationCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
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

// 统计数据
const statistics = ref({
  totalSku: 0,
  lowStockCount: 0,
  overStockCount: 0,
  normalCount: 0
})

// 空行填充
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

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
    // 更新统计
    statistics.value.totalSku = tableData.value.length
    statistics.value.lowStockCount = tableData.value.filter(r => r.quantity < (r.minStock ?? 0)).length
    statistics.value.overStockCount = tableData.value.filter(r => r.quantity > (r.maxStock ?? 999999)).length
    statistics.value.normalCount = tableData.value.filter(r => r.quantity >= (r.minStock ?? 0) && r.quantity <= (r.maxStock ?? 999999)).length
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
.erp-page { padding: 16px; height: 100%; display: flex; flex-direction: column; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.low-stock { color: #ff4d4f; font-weight: bold; }
.over-stock { color: #fa8c16; font-weight: bold; }

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7e6 100%);
  border: 1px solid #b7eb8f;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #f5222d;
}

.empty-placeholder {
  color: transparent;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}

/* 空占位行 */
:deep(.ant-table-tbody > tr:not(.ant-table-row):has(.empty-placeholder) > td) {
  background: #fff !important;
  height: 40px !important;
}
</style>
