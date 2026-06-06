<template>
  <div class="stocktake-page">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'erp-stocktake-list'"
      :filter-fields="filterFields"
      add-text="新建盘点单"
      @add="handleCreate"
      @refresh="fetchData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span class="list-update-timestamp">最后更新：{{ dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss') }}</span>
      </template>

      <template #empty>
        <div class="table-empty">
          <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
          <InboxOutlined v-else class="table-empty-icon" />
          <p v-if="hasActiveFilters" class="table-empty-text">
            没有符合条件的盘点单，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无盘点单数据，点击右上角「新建盘点单」开始创建
          </p>
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <StatusTag :status="record.status" :map="STOCKTAKE_STATUS_ORDER" />
        </template>
        <template v-else-if="column.key === 'difference'">
          <span :class="{ 'positive': record.difference > 0, 'negative': record.difference < 0 }">
            {{ record.difference > 0 ? '+' : '' }}{{ record.difference }}
          </span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-tooltip title="查看">
              <a-button type="link" size="small" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 0" title="开始盘点">
              <a-button type="link" size="small" @click="handleStart(record)">
                <template #icon><FormOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 1" title="完成盘点">
              <a-button type="link" size="small" @click="handleComplete(record)">
                <template #icon><CheckOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item key="delete">
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </TableList>

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
          <StatusTag :status="currentRecord.status" :map="STOCKTAKE_STATUS_ORDER" />
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
      <div class="detail-modal-footer">
        <a-button @click="detailVisible = false">关闭</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { STOCKTAKE_STATUS_ORDER } from '@/utils/statusConfig'
import { message, Modal } from 'ant-design-vue'
import { stockCheckApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import {
  EyeOutlined,
  FormOutlined,
  CheckOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  SearchOutlined,
  InboxOutlined
} from '@ant-design/icons-vue'

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
  remark?: string
}

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref<Stocktake[]>([])
const detailVisible = ref(false)
const currentRecord = ref<Stocktake | null>(null)
const tableRef = ref()
const lastUpdated = ref(new Date().toISOString())

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
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

const filterFields = [
  { key: 'stocktakeNo', label: '盘点单号', type: 'input' as const, placeholder: '请输入盘点单号' },
  { key: 'warehouseId', label: '仓库', type: 'select' as const, options: [
    { label: '主仓库', value: 1 },
    { label: '分仓库', value: 2 },
  ]},
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待审核', value: 0 },
    { label: '盘点中', value: 1 },
    { label: '已完成', value: 2 },
  ]},
]

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

const handleResetFilters = () => {
  for (const key of Object.keys(searchFilters)) {
    searchFilters[key] = undefined
  }
  pagination.current = 1
  fetchData()
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

const handleDelete = async (record: Stocktake) => {
  try {
    await request.delete(`/erp/stockCheck/${record.id}`)
    message.success('删除成功')
    fetchData()
  } catch (error) {
    message.error('删除失败')
  }
}

const handleActionMenuClick = (key: string, record: Stocktake) => {
  if (key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: '删除后数据不可恢复，确定要删除该盘点单吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => handleDelete(record)
    })
  }
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleCreate()
  }
}

window.addEventListener('keydown', handleKeydown)
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await stockCheckApi.page({
      tenantId: userStore.tenantId,
      keyword: searchFilters.stocktakeNo || undefined,
      status: searchFilters.status,
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
        operator: item.creatorName || '',
        remark: item.remark || ''
      }))
      pagination.total = res.data.total || 0
    } else {
      dataSource.value = []
      pagination.total = 0
    }
    lastUpdated.value = new Date().toISOString()
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

.list-update-timestamp {
  color: #999;
  font-size: 12px;
  margin-right: 12px;
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.action-more-btn {
  padding: 0 4px;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
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
