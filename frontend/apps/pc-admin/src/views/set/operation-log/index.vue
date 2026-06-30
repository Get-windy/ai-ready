<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">操作日志</h2>
      </div>
      <div class="page-header__right">
        <a-button @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出
        </a-button>
        <a-button danger @click="handleClear">
          <template #icon><DeleteOutlined /></template>
          清空
        </a-button>
      </div>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="search-card">
        <a-form layout="inline">
          <a-form-item label="模块">
            <a-select v-model:value="queryForm.module" placeholder="全部模块" allow-clear style="width: 140px" @change="handleSearch">
              <a-select-option v-for="m in modules" :key="m" :value="m">{{ m }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作类型">
            <a-select v-model:value="queryForm.operationType" placeholder="全部类型" allow-clear style="width: 140px" @change="handleSearch">
              <a-select-option v-for="t in operationTypes" :key="t" :value="t">{{ t }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="操作人">
            <a-input v-model:value="queryForm.operatorName" placeholder="请输入操作人" allow-clear style="width: 140px" />
          </a-form-item>
          <a-form-item label="日期范围">
            <a-range-picker v-model:value="dateRange" @change="handleSearch" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
      <a-card :bordered="false" class="table-card">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'success' : 'error'">{{ record.status === 0 ? '成功' : '失败' }}</a-tag>
            </template>
            <template v-if="column.key === 'costTime'">
              <span>{{ record.costTime }}ms</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="detailVisible" title="操作详情" width="700px" :footer="null">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="操作编号" :span="2">{{ currentDetail?.id }}</a-descriptions-item>
        <a-descriptions-item label="模块">{{ currentDetail?.module }}</a-descriptions-item>
        <a-descriptions-item label="操作类型">{{ currentDetail?.operationType }}</a-descriptions-item>
        <a-descriptions-item label="操作描述" :span="2">{{ currentDetail?.description }}</a-descriptions-item>
        <a-descriptions-item label="操作人">{{ currentDetail?.operatorName }}</a-descriptions-item>
        <a-descriptions-item label="操作时间">{{ currentDetail?.operationTime }}</a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ currentDetail?.ipAddress }}</a-descriptions-item>
        <a-descriptions-item label="请求方法">{{ currentDetail?.requestMethod }}</a-descriptions-item>
        <a-descriptions-item label="请求URL" :span="2">{{ currentDetail?.requestUrl }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ currentDetail?.costTime }}ms</a-descriptions-item>
        <a-descriptions-item label="状态"><a-tag :color="currentDetail?.status === 0 ? 'success' : 'error'">{{ currentDetail?.status === 0 ? '成功' : '失败' }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="请求参数" :span="2"><pre style="max-height: 200px; overflow: auto; white-space: pre-wrap;">{{ currentDetail?.requestParams || '-' }}</pre></a-descriptions-item>
        <a-descriptions-item label="响应结果" :span="2"><pre style="max-height: 200px; overflow: auto; white-space: pre-wrap;">{{ currentDetail?.responseResult || '-' }}</pre></a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { logApi, type OperationLog, type LogQuery } from '@/api/log'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })
const queryForm = reactive<LogQuery>({})
const dateRange = ref<any>(null)
const modules = ref<string[]>([])
const operationTypes = ref<string[]>([])

const detailVisible = ref(false)
const currentDetail = ref<OperationLog | null>(null)

const columns: any[] = [
  { title: '操作编号', dataIndex: 'id', key: 'id', width: 80 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 100 },
  { title: '操作类型', dataIndex: 'operationType', key: 'operationType', width: 100 },
  { title: '操作描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '操作时间', dataIndex: 'operationTime', key: 'operationTime', width: 160 },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 130 },
  { title: '耗时', dataIndex: 'costTime', key: 'costTime', width: 70 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 70 },
  { title: '操作', key: 'action', width: 60, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const params: LogQuery = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...queryForm
    }
    if (dateRange.value) {
      params.startDate = dayjs(dateRange.value[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(dateRange.value[1]).format('YYYY-MM-DD')
    }
    const result = await logApi.getPage(params)
    tableData.value = result.data.records
    pagination.total = result.data.total
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() {
  queryForm.module = undefined
  queryForm.operationType = undefined
  queryForm.operatorName = undefined
  dateRange.value = null
  handleSearch()
}
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showDetail(record: any) {
  currentDetail.value = record
  detailVisible.value = true
}

async function handleExport() {
  try {
    const params: LogQuery = { ...queryForm }
    if (dateRange.value) {
      params.startDate = dayjs(dateRange.value[0]).format('YYYY-MM-DD')
      params.endDate = dayjs(dateRange.value[1]).format('YYYY-MM-DD')
    }
    await logApi.exportLogs(params)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  }
}

async function handleClear() {
  Modal.confirm({
    title: '确认清空',
    content: '确定要清空所有操作日志吗？此操作不可恢复。',
    okType: 'danger',
    onOk: async () => {
      await logApi.clearLogs()
      message.success('已清空')
      loadData()
    }
  })
}

async function loadOptions() {
  try {
    modules.value = (await logApi.getModules()).data || []
    operationTypes.value = (await logApi.getOperationTypes()).data || []
  } catch (e) {
    // 静默失败
  }
}

onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.search-card { margin-bottom: 16px; }
.table-card { background: #fff; }
pre { margin: 0; background: #f5f5f5; padding: 8px; border-radius: 4px; font-size: 12px; }
</style>
