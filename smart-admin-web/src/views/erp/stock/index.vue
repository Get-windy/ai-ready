<template>
  <div class="stock-page">
    <a-card title="库存管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="商品编码">
            <a-input v-model:value="queryParams.productCode" placeholder="请输入商品编码" allow-clear />
          </a-form-item>
          <a-form-item label="商品名称">
            <a-input v-model:value="queryParams.productName" placeholder="请输入商品名称" allow-clear />
          </a-form-item>
          <a-form-item label="仓库">
            <a-select v-model:value="queryParams.warehouseId" placeholder="请选择" allow-clear style="width: 150px">
              <a-select-option :value="1">主仓库</a-select-option>
              <a-select-option :value="2">分仓库</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="库存预警">
            <a-select v-model:value="queryParams.warningStatus" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option :value="1">低库存</a-select-option>
              <a-select-option :value="2">超储</a-select-option>
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
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quantity'">
            <span :class="{ 'low-stock': record.quantity < record.minQuantity, 'over-stock': record.quantity > record.maxQuantity }">
              {{ record.quantity }} {{ record.unit }}
            </span>
          </template>
          <template v-else-if="column.key === 'warningStatus'">
            <a-tag v-if="record.quantity < record.minQuantity" color="red">低库存</a-tag>
            <a-tag v-else-if="record.quantity > record.maxQuantity" color="orange">超储</a-tag>
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
      <a-table :columns="logColumns" :data-source="stockLogs" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'in' ? 'green' : 'red'">
              {{ record.type === 'in' ? '入库' : '出库' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined, AuditOutlined, ExportOutlined } from '@ant-design/icons-vue'

interface StockItem {
  id: number
  productCode: string
  productName: string
  specification: string
  unit: string
  quantity: number
  minQuantity: number
  maxQuantity: number
  warehouseName: string
  lastInTime: string
  lastOutTime: string
}

const loading = ref(false)
const logModalVisible = ref(false)

const queryParams = reactive({
  productCode: '',
  productName: '',
  warehouseId: undefined as number | undefined,
  warningStatus: undefined as number | undefined
})

const dataSource = ref<StockItem[]>([
  { id: 1, productCode: 'P001', productName: '商品A', specification: '500g', unit: '件', quantity: 150, minQuantity: 50, maxQuantity: 500, warehouseName: '主仓库', lastInTime: '2026-03-28', lastOutTime: '2026-03-27' },
  { id: 2, productCode: 'P002', productName: '商品B', specification: '1kg', unit: '箱', quantity: 30, minQuantity: 100, maxQuantity: 300, warehouseName: '主仓库', lastInTime: '2026-03-26', lastOutTime: '2026-03-28' },
  { id: 3, productCode: 'P003', productName: '商品C', specification: '100ml', unit: '瓶', quantity: 600, minQuantity: 200, maxQuantity: 500, warehouseName: '分仓库', lastInTime: '2026-03-25', lastOutTime: '2026-03-20' },
  { id: 4, productCode: 'P004', productName: '商品D', specification: '2L', unit: '桶', quantity: 250, minQuantity: 100, maxQuantity: 400, warehouseName: '主仓库', lastInTime: '2026-03-27', lastOutTime: '2026-03-26' }
])

const stockLogs = ref([
  { id: 1, type: 'in', quantity: 100, orderNo: 'PO20260328001', time: '2026-03-28 10:00', operator: '张三' },
  { id: 2, type: 'out', quantity: 50, orderNo: 'SO20260328001', time: '2026-03-27 14:30', operator: '李四' },
  { id: 3, type: 'in', quantity: 200, orderNo: 'PO20260327002', time: '2026-03-26 09:15', operator: '王五' }
])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 4,
  showSizeChanger: true
})

const columns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 80 },
  { title: '库存数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '最低库存', dataIndex: 'minQuantity', key: 'minQuantity', width: 100 },
  { title: '最高库存', dataIndex: 'maxQuantity', key: 'maxQuantity', width: 100 },
  { title: '预警状态', dataIndex: 'warningStatus', key: 'warningStatus', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName' },
  { title: '最后入库', dataIndex: 'lastInTime', key: 'lastInTime', width: 110 },
  { title: '最后出库', dataIndex: 'lastOutTime', key: 'lastOutTime', width: 110 },
  { title: '操作', key: 'action', fixed: 'right', width: 120 }
]

const logColumns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '关联单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' }
]

const handleSearch = () => message.info('查询')
const handleReset = () => { queryParams.productCode = ''; queryParams.productName = ''; queryParams.warehouseId = undefined; queryParams.warningStatus = undefined }
const handleTableChange = (pag: any) => { pagination.current = pag.current }

const handleInbound = () => message.info('入库操作')
const handleOutbound = () => message.info('出库操作')
const handleStocktake = () => message.info('库存盘点')
const handleExport = () => message.info('导出库存')
const handleView = (record: StockItem) => message.info('查看: ' + record.productName)
const handleStockLog = (record: StockItem) => { logModalVisible.value = true }
</script>

<style scoped>
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.low-stock { color: #ff4d4f; font-weight: bold; }
.over-stock { color: #fa8c16; font-weight: bold; }
</style>