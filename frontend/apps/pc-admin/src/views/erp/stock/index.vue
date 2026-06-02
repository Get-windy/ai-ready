<template>
  <div class="stock-page">
    <a-card title="库存管理">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="商品编码">
            <a-input
              v-model:value="queryParams.productCode"
              placeholder="请输入商品编码"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="商品名称">
            <a-input
              v-model:value="queryParams.productName"
              placeholder="请输入商品名称"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="仓库">
            <a-select
              v-model:value="queryParams.warehouseId"
              placeholder="请选择"
              allow-clear
              style="width: 150px"
            >
              <a-select-option :value="1">
                主仓库
              </a-select-option>
              <a-select-option :value="2">
                分仓库
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="库存预警">
            <a-select
              v-model:value="queryParams.warningStatus"
              placeholder="请选择"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">
                低库存
              </a-select-option>
              <a-select-option :value="2">
                超储
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button @click="handleReset">
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button
            type="primary"
            @click="handleInbound"
          >
            <template #icon>
              <LoginOutlined />
            </template>
            入库
          </a-button>
          <a-button @click="handleOutbound">
            <template #icon>
              <LogoutOutlined />
            </template>
            出库
          </a-button>
          <a-button @click="handleStocktake">
            <template #icon>
              <AuditOutlined />
            </template>
            盘点
          </a-button>
          <a-button @click="handleExport">
            <template #icon>
              <ExportOutlined />
            </template>
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
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'quantity'">
            <span :class="{ 'low-stock': record.quantity < (record.minStock ?? 0), 'over-stock': record.quantity > (record.maxStock ?? 999999) }">
              {{ record.quantity }} {{ record.unit }}
            </span>
          </template>
          <template v-else-if="column.key === 'warningStatus'">
            <a-tag
              v-if="record.quantity < (record.minStock ?? 0)"
              color="red"
            >
              低库存
            </a-tag>
            <a-tag
              v-else-if="record.quantity > (record.maxStock ?? 999999)"
              color="orange"
            >
              超储
            </a-tag>
            <a-tag
              v-else
              color="green"
            >
              正常
            </a-tag>
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
    <a-modal
      v-model:open="logModalVisible"
      title="库存明细"
      :footer="null"
      width="800px"
    >
      <a-table
        :columns="logColumns"
        :data-source="stockLogs"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'type'">
            <a-tag :color="record.type === 'in' ? 'green' : 'red'">
              {{ record.type === 'in' ? '入库' : '出库' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 库存详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="库存详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="商品编码">{{ currentRecord.productCode }}</a-descriptions-item>
        <a-descriptions-item label="商品名称">{{ currentRecord.productName }}</a-descriptions-item>
        <a-descriptions-item label="规格">{{ currentRecord.specification }}</a-descriptions-item>
        <a-descriptions-item label="单位">{{ currentRecord.unit }}</a-descriptions-item>
        <a-descriptions-item label="库存数量">
          <span :class="{ 'low-stock': currentRecord.quantity < (currentRecord.minStock ?? 0), 'over-stock': currentRecord.quantity > (currentRecord.maxStock ?? 999999) }">
            {{ currentRecord.quantity }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="最低库存">{{ currentRecord.minStock }}</a-descriptions-item>
        <a-descriptions-item label="最高库存">{{ currentRecord.maxStock }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="最后入库">{{ currentRecord.lastInboundDate }}</a-descriptions-item>
        <a-descriptions-item label="最后出库">{{ currentRecord.lastOutboundDate }}</a-descriptions-item>
      </a-descriptions>
      <div style="text-align: right; margin-top: 16px">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LoginOutlined, LogoutOutlined, AuditOutlined, ExportOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import { stockApi, type StockItem, type PageQuery } from '@/api/erp'

const router = useRouter()
const loading = ref(false)
const logModalVisible = ref(false)
const detailVisible = ref(false)
const currentRecord = ref<StockItem | null>(null)

const queryParams = reactive<Record<string, any>>({
  productCode: '',
  productName: '',
  warehouseId: undefined as number | undefined,
  warningStatus: undefined as number | undefined
})

const dataSource = ref<StockItem[]>([])
const stockLogs = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 80 },
  { title: '库存数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '最低库存', dataIndex: 'minStock', key: 'minStock', width: 100 },
  { title: '最高库存', dataIndex: 'maxStock', key: 'maxStock', width: 100 },
  { title: '预警状态', dataIndex: 'warningStatus', key: 'warningStatus', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName' },
  { title: '最后入库', dataIndex: 'lastInboundDate', key: 'lastInboundDate', width: 110 },
  { title: '最后出库', dataIndex: 'lastOutboundDate', key: 'lastOutboundDate', width: 110 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 120 }
]

const logColumns = [
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '关联单号', dataIndex: 'orderNo', key: 'orderNo' },
  { title: '时间', dataIndex: 'time', key: 'time' },
  { title: '操作人', dataIndex: 'operator', key: 'operator' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const params: PageQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: queryParams.productName || undefined,
      ...queryParams
    }
    const res = await stockApi.page(params)
    dataSource.value = res.records || []
    pagination.total = res.total
  } catch (err: any) {
    message.error('加载库存数据失败: ' + (err?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => { queryParams.productCode = ''; queryParams.productName = ''; queryParams.warehouseId = undefined; queryParams.warningStatus = undefined }
const handleTableChange = (pag: any) => { pagination.current = pag.current; fetchData() }

const handleInbound = () => router.push('/erp/stock-in')
const handleOutbound = () => router.push('/erp/stock-out')
const handleStocktake = () => router.push('/erp/stocktake')
const handleExport = async () => {
  try {
    await request.get('/erp/stock/export', { responseType: 'blob' })
    message.success('导出成功')
  } catch {
    message.info('导出功能将在后续版本实现')
  }
}
const handleView = (record: StockItem) => { currentRecord.value = record; detailVisible.value = true }
const handleStockLog = () => { message.info('库存流水功能将在后续版本实现') }

onMounted(() => { fetchData() })
</script>

<style scoped>
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.low-stock { color: #ff4d4f; font-weight: bold; }
.over-stock { color: #fa8c16; font-weight: bold; }
</style>