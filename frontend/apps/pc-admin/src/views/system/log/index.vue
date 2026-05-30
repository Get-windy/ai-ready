<template>
  <div class="log-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="模块">
              <a-select
                v-model:value="searchForm.module"
                placeholder="请选择模块"
                allow-clear
                style="width: 100%"
              >
                <a-select-option v-for="m in moduleOptions" :key="m" :value="m">
                  {{ m }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="操作类型">
              <a-select
                v-model:value="searchForm.operationType"
                placeholder="请选择操作类型"
                allow-clear
                style="width: 100%"
              >
                <a-select-option value="ADD">新增</a-select-option>
                <a-select-option value="UPDATE">修改</a-select-option>
                <a-select-option value="DELETE">删除</a-select-option>
                <a-select-option value="QUERY">查询</a-select-option>
                <a-select-option value="IMPORT">导入</a-select-option>
                <a-select-option value="EXPORT">导出</a-select-option>
                <a-select-option value="LOGIN">登录</a-select-option>
                <a-select-option value="LOGOUT">登出</a-select-option>
                <a-select-option value="OTHER">其他</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="操作人">
              <a-input
                v-model:value="searchForm.operatorName"
                placeholder="请输入操作人"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="日期范围">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                style="width: 100%"
                :placeholder="['开始日期', '结束日期']"
                format="YYYY-MM-DD"
                :valueFormat="['startDate', 'endDate']"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <div class="table-header">
          <span class="title">操作日志</span>
          <a-space>
            <a-button danger @click="handleClearLogs">
              <template #icon><DeleteOutlined /></template>
              清空日志
            </a-button>
            <a-button @click="handleExport">
              <template #icon><ExportOutlined /></template>
              导出
            </a-button>
          </a-space>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
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
      </a-table>
    </a-card>

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
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  ExportOutlined
} from '@ant-design/icons-vue'
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
const columns: TableProps['columns'] = [
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

// 表格变化
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
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

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: -24px;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header .title {
  font-size: 16px;
  font-weight: 500;
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

@media (max-width: 768px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 16px;
  }

  .table-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
