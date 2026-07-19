<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>操作审计</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            操作审计
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            size="small"
            :loading="loading"
            @click="fetchData"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <!-- 搜索区域 -->
      <a-form
        layout="inline"
        class="search-form"
      >
        <a-form-item label="审计类型">
          <a-select
            v-model:value="query.auditType"
            placeholder="全部类型"
            style="width:140px"
            allow-clear
          >
            <a-select-option value="LOGIN">
              登录
            </a-select-option>
            <a-select-option value="CREATE">
              新增
            </a-select-option>
            <a-select-option value="UPDATE">
              修改
            </a-select-option>
            <a-select-option value="DELETE">
              删除
            </a-select-option>
            <a-select-option value="EXPORT">
              导出
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="模块">
          <a-select
            v-model:value="query.module"
            placeholder="全部模块"
            style="width:140px"
            allow-clear
          >
            <a-select-option value="system">
              系统管理
            </a-select-option>
            <a-select-option value="sale">
              销售管理
            </a-select-option>
            <a-select-option value="finance">
              财务管理
            </a-select-option>
            <a-select-option value="warehouse">
              仓储管理
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="query.timeRange"
            show-time
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            size="small"
            @click="fetchData"
          >
            <template #icon>
              <SearchOutlined />
            </template>
            查询
          </a-button>
          <a-button
            size="small"
            style="margin-left:8px"
            @click="resetQuery"
          >
            <template #icon>
              <ClearOutlined />
            </template>
            重置
          </a-button>
        </a-form-item>
      </a-form>

      <a-divider />

      <!-- 审计统计 -->
      <a-row
        :gutter="16"
        style="margin-bottom:16px"
      >
        <a-col :span="8">
          <a-statistic
            title="审计日志总数"
            :value="auditStats.total || 0"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="成功操作"
            :value="auditStats.successCount || 0"
            :value-style="{ color: '#52c41a' }"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="失败操作"
            :value="auditStats.failCount || 0"
            :value-style="{ color: '#ff4d4f' }"
          />
        </a-col>
      </a-row>

      <!-- 审计日志列表 -->
      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'auditType'">
            <a-tag :color="auditTypeColor(record.auditType)">
              {{ record.auditType }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'SUCCESS' ? 'success' : 'error'"
              :text="record.status === 'SUCCESS' ? '成功' : '失败'"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a @click="showDetail(record)">详情</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="审计日志详情"
      width="640px"
      :footer="null"
    >
      <a-descriptions
        :column="1"
        size="small"
        bordered
      >
        <a-descriptions-item label="日志ID">
          {{ detailItem?.id }}
        </a-descriptions-item>
        <a-descriptions-item label="审计类型">
          {{ detailItem?.auditType }}
        </a-descriptions-item>
        <a-descriptions-item label="操作模块">
          {{ detailItem?.module }}
        </a-descriptions-item>
        <a-descriptions-item label="操作用户">
          {{ detailItem?.userName || detailItem?.userId }}
        </a-descriptions-item>
        <a-descriptions-item label="操作时间">
          {{ detailItem?.createTime }}
        </a-descriptions-item>
        <a-descriptions-item label="操作状态">
          {{ detailItem?.status }}
        </a-descriptions-item>
        <a-descriptions-item label="IP地址">
          {{ detailItem?.ipAddress }}
        </a-descriptions-item>
        <a-descriptions-item
          v-if="detailItem?.detail"
          label="操作详情"
        >
          <pre style="max-height:200px;overflow:auto;background:#f5f5f5;padding:8px;border-radius:4px">{{ JSON.stringify(detailItem.detail, null, 2) }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])
const detailVisible = ref(false)
const detailItem = ref<any>({})

const query = reactive({
  auditType: undefined as string | undefined,
  module: undefined as string | undefined,
  timeRange: undefined as any,
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const auditStats = reactive({
  total: 0,
  successCount: 0,
  failCount: 0,
})

const columns = [
  { title: '日志ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '审计类型', dataIndex: 'auditType', key: 'auditType', width: 100 },
  { title: '操作模块', dataIndex: 'module', key: 'module', width: 120 },
  { title: '操作用户', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: 'IP地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 140 },
  { title: '操作', key: 'action', width: 60 },
]

function auditTypeColor(type: string): string {
  const map: Record<string, string> = { LOGIN: 'blue', CREATE: 'green', UPDATE: 'orange', DELETE: 'red', EXPORT: 'purple' }
  return map[type] || 'default'
}

function resetQuery() {
  query.auditType = undefined
  query.module = undefined
  query.timeRange = undefined
  fetchData()
}

function showDetail(record: any) {
  detailItem.value = record
  detailVisible.value = true
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (query.auditType) params.auditType = query.auditType
    if (query.module) params.module = query.module
    if (query.timeRange?.[0]) params.startTime = query.timeRange[0].format('YYYY-MM-DD HH:mm:ss')
    if (query.timeRange?.[1]) params.endTime = query.timeRange[1].format('YYYY-MM-DD HH:mm:ss')

    const res = await request.get('/audit/query', { params })
    if (res) {
      list.value = res.records || []
      pagination.total = res.total || 0
    }

    // 获取统计
    const statsRes = await request.get('/audit/statistics')
    if (statsRes) {
      auditStats.total = statsRes.total || 0
      auditStats.successCount = statsRes.successCount || 0
      auditStats.failCount = statsRes.failCount || 0
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
