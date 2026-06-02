<template>
  <div class="stocktake-page">
    <a-card title="库存盘点">
      <!-- 搜索区域 -->
      <div class="search-area">
        <a-form
          layout="inline"
          :model="queryParams"
        >
          <a-form-item label="盘点单号">
            <a-input
              v-model:value="queryParams.stocktakeNo"
              placeholder="请输入盘点单号"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="仓库">
            <a-select
              v-model:value="queryParams.warehouseId"
              placeholder="请选择仓库"
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
          <a-form-item label="状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="0">
                待审核
              </a-select-option>
              <a-select-option :value="1">
                盘点中
              </a-select-option>
              <a-select-option :value="2">
                已完成
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ReloadOutlined />
                </template>
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
            @click="handleCreate"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新建盘点单
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
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'difference'">
            <span :class="{ 'positive': record.difference > 0, 'negative': record.difference < 0 }">
              {{ record.difference > 0 ? '+' : '' }}{{ record.difference }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button
                v-if="record.status === 0"
                type="link"
                size="small"
                @click="handleStart(record)"
              >
                开始盘点
              </a-button>
              <a-button
                v-if="record.status === 1"
                type="link"
                size="small"
                @click="handleComplete(record)"
              >
                完成盘点
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="盘点单详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions bordered :column="2" v-if="currentRecord">
        <a-descriptions-item label="盘点单号">{{ currentRecord.stocktakeNo }}</a-descriptions-item>
        <a-descriptions-item label="仓库">{{ currentRecord.warehouseName }}</a-descriptions-item>
        <a-descriptions-item label="盘点日期">{{ currentRecord.stocktakeDate }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(currentRecord.status)">{{ getStatusText(currentRecord.status) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="系统数量">{{ currentRecord.systemQuantity }}</a-descriptions-item>
        <a-descriptions-item label="实际数量">{{ currentRecord.actualQuantity }}</a-descriptions-item>
        <a-descriptions-item label="差异">
          <span :class="{ 'positive': currentRecord.difference > 0, 'negative': currentRecord.difference < 0 }">
            {{ currentRecord.difference > 0 ? '+' : '' }}{{ currentRecord.difference }}
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentRecord.operator }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div style="text-align: right; margin-top: 16px">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { stockCheckApi, type StockCheck } from '@/api/erp'
import { useUserStore } from '@/stores/user'

interface Stocktake {
  id: number
  stocktakeNo: string
  warehouseName: string
  stocktakeDate: string
  systemQuantity: number
  actualQuantity: number
  difference: number
  status: number
  operator: string
}

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref<Stocktake[]>([])
const detailVisible = ref(false)
const currentRecord = ref<Stocktake | null>(null)

const queryParams = reactive({
  stocktakeNo: '',
  warehouseId: undefined as number | undefined,
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  {
    title: '盘点单号',
    dataIndex: 'stocktakeNo',
    key: 'stocktakeNo',
    width: 150
  },
  {
    title: '仓库',
    dataIndex: 'warehouseName',
    key: 'warehouseName',
    width: 120
  },
  {
    title: '盘点日期',
    dataIndex: 'stocktakeDate',
    key: 'stocktakeDate',
    width: 120
  },
  {
    title: '系统数量',
    dataIndex: 'systemQuantity',
    key: 'systemQuantity',
    width: 100
  },
  {
    title: '实际数量',
    dataIndex: 'actualQuantity',
    key: 'actualQuantity',
    width: 100
  },
  {
    title: '差异',
    key: 'difference',
    width: 100
  },
  {
    title: '状态',
    key: 'status',
    width: 100
  },
  {
    title: '操作人',
    dataIndex: 'operator',
    key: 'operator',
    width: 100
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right'
  }
]

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'processing',
    2: 'success'
  }
  return colors[status] || 'default'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '待审核',
    1: '盘点中',
    2: '已完成'
  }
  return texts[status] || '未知'
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.stocktakeNo = ''
  queryParams.warehouseId = undefined
  queryParams.status = undefined
  handleSearch()
}

const handleCreate = () => {
  message.info('打开新建盘点单表单')
}

const handleView = (record: Stocktake) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleStart = (record: Stocktake) => {
  message.success(`开始盘点: ${record.stocktakeNo}`)
}

const handleComplete = (record: Stocktake) => {
  message.success(`盘点完成: ${record.stocktakeNo}`)
}

const handleExport = () => {
  message.info('导出盘点单')
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await stockCheckApi.page({
      tenantId: userStore.tenantId,
      keyword: queryParams.stocktakeNo || undefined,
      status: queryParams.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data?.records) {
      dataSource.value = res.data.records.map((item) => ({
        id: item.id,
        stocktakeNo: item.checkNo,
        warehouseName: item.warehouseName,
        stocktakeDate: item.checkDate,
        systemQuantity: item.systemQuantity || 0,
        actualQuantity: item.actualQuantity || 0,
        difference: (item.actualQuantity || 0) - (item.systemQuantity || 0),
        status: item.status,
        operator: item.creatorName || ''
      }))
      pagination.total = res.data.total || 0
    } else {
      dataSource.value = []
      pagination.total = 0
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style scoped>
.stocktake-page {
  padding: 24px;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

.positive {
  color: #3f8600;
  font-weight: bold;
}

.negative {
  color: #ff4d4f;
  font-weight: bold;
}
</style>