<template>
  <div class="log-management">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'system-log-list'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :show-export="true"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @export="handleExport"
    >
      <template #toolbar-actions>
        <a-button danger @click="handleClearLogs">
          <template #icon><DeleteOutlined /></template>
          清空日志
        </a-button>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 0 ? 'success' : 'error'">
            {{ record.status === 0 ? '成功' : '失败' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'costTime'">
          <span v-if="record.costTime > 1000" style="color: #ff4d4f">
            {{ record.costTime }}ms
          </span>
          <span v-else-if="record.costTime > 500" style="color: #faad14">
            {{ record.costTime }}ms
          </span>
          <span v-else>
            {{ record.costTime }}ms
          </span>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-button type="link" size="small" @click="handleDetail(record)">
            详情
          </a-button>
        </template>
      </template>
    </TableList>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="操作日志详情"
      :footer="null"
      width="700px"
    >
      <a-descriptions :column="1" bordered size="small" v-if="currentLog">
        <a-descriptions-item label="模块">{{ currentLog.module }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{ currentLog.operationType }}</a-descriptions-item>
        <a-descriptions-item label="操作描述">{{ currentLog.description }}</a-descriptions-item>
        <a-descriptions-item label="请求URL">{{ currentLog.requestUrl }}</a-descriptions-item>
        <a-descriptions-item label="请求方法">
          <a-tag :color="getMethodColor(currentLog.requestMethod)">
            {{ currentLog.requestMethod }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentLog.operatorName }}</a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</a-descriptions-item>
        <a-descriptions-item label="操作时间">{{ currentLog.operationTime }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ currentLog.costTime }}ms</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="currentLog.status === 0 ? 'success' : 'error'">
            {{ currentLog.status === 0 ? '成功' : '失败' }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
      <a-divider v-if="currentLog">请求/响应详情</a-divider>
      <a-tabs v-if="currentLog">
        <a-tab-pane key="request" tab="请求参数">
          <pre class="json-content">{{ formatJson(currentLog.requestParams) }}</pre>
        </a-tab-pane>
        <a-tab-pane key="response" tab="响应结果">
          <pre class="json-content">{{ formatJson(currentLog.responseResult) }}</pre>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined
} from '@ant-design/icons-vue'
import TableList, { type FilterField } from '@/components/TableList/TableList.vue'
import { logApi, type OperationLog } from '@/api/log'

// 搜索表单
const searchForm = reactive({
  module: undefined as string | undefined,
  operationType: undefined as string | undefined,
  operatorName: '',
  dateRange: undefined as [string, string] | undefined
})

const moduleOptions = ref<string[]>([])

// 表格数据
const tableData = ref<OperationLog[]>([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列定义
const columns: any[] = [
  { title: '模块', dataIndex: 'module', width: 120, ellipsis: true },
  { title: '操作类型', dataIndex: 'operationType', width: 100 },
  { title: '操作描述', dataIndex: 'description', width: 200, ellipsis: true },
  { title: '请求URL', dataIndex: 'requestUrl', width: 200, ellipsis: true },
  { title: '请求方法', dataIndex: 'requestMethod', width: 100 },
  { title: '操作人', dataIndex: 'operatorName', width: 100 },
  { title: 'IP地址', dataIndex: 'ipAddress', width: 130 },
  { title: '操作时间', dataIndex: 'operationTime', width: 160 },
  { title: '耗时(ms)', key: 'costTime', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

// 筛选字段
const moduleSelectOptions = computed(() =>
  moduleOptions.value.map(m => ({ label: m, value: m }))
)

const filterFields = computed<FilterField[]>(() => [
  { key: 'module', label: '模块', type: 'select', options: moduleSelectOptions.value, placeholder: '请选择模块' },
  { key: 'operationType', label: '操作类型', type: 'select', options: [
    { label: '新增', value: 'ADD' }, { label: '修改', value: 'UPDATE' }, { label: '删除', value: 'DELETE' },
    { label: '查询', value: 'QUERY' }, { label: '导入', value: 'IMPORT' }, { label: '导出', value: 'EXPORT' },
    { label: '登录', value: 'LOGIN' }, { label: '登出', value: 'LOGOUT' }, { label: '其他', value: 'OTHER' }
  ]},
  { key: 'operatorName', label: '操作人', type: 'input', placeholder: '请输入操作人' },
  { key: 'dateRange', label: '日期范围', type: 'dateRange' },
])

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const params: any = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    delete params.dateRange

    const res = await logApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const fetchModules = async () => {
  try {
    const res = await logApi.getModules()
    if (res.data) {
      moduleOptions.value = res.data
    }
  } catch {
    // 忽略模块列表加载失败
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    module: undefined,
    operationType: undefined,
    operatorName: '',
    dateRange: undefined
  })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { module: undefined, operationType: undefined, operatorName: '', dateRange: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 详情
const handleDetail = (record: OperationLog) => {
  currentLog.value = record
  detailVisible.value = true
}

// 清空日志
const handleClearLogs = () => {
  Modal.confirm({
    title: '确认清空',
    content: '确定要清空所有操作日志吗？此操作不可恢复。',
    okType: 'danger',
    async onOk() {
      try {
        await logApi.clearLogs()
        message.success('日志已清空')
        fetchData()
      } catch {
        message.error('清空失败')
      }
    }
  })
}

// 导出
const handleExport = async () => {
  try {
    message.loading({ content: '正在导出...', key: 'export' })
    const res = await logApi.exportLogs(searchForm as any)
    const blob = res as any
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success({ content: '导出成功', key: 'export' })
  } catch {
    message.error({ content: '导出失败', key: 'export' })
  }
}

// 辅助函数
const getMethodColor = (method: string) => {
  const colors: Record<string, string> = {
    GET: 'green',
    POST: 'blue',
    PUT: 'orange',
    PATCH: 'purple',
    DELETE: 'red'
  }
  return colors[method] || 'default'
}

const formatJson = (jsonStr: string | undefined) => {
  if (!jsonStr) return '无数据'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(() => {
  fetchData()
  fetchModules()
})
</script>

<style scoped>
.log-management {
  padding: 0;
}

.json-content {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
