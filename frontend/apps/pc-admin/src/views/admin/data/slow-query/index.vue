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
            <a-breadcrumb-item>慢查询</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            慢查询
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

    <!-- 统计卡片（基于当前数据源过滤后的全量数据计算） -->
    <ARStatCards
      :items="statCards"
      :loading="loading"
    />

    <a-card :bordered="false">
      <!-- 搜索 -->
      <a-form
        layout="inline"
        class="search-form"
      >
        <a-form-item label="慢查询阈值">
          <a-select
            v-model:value="query.threshold"
            style="width:120px"
            @change="handleFilterChange"
          >
            <a-select-option :value="0">
              全部
            </a-select-option>
            <a-select-option :value="100">
              >100ms
            </a-select-option>
            <a-select-option :value="500">
              >500ms
            </a-select-option>
            <a-select-option :value="1000">
              >1s
            </a-select-option>
            <a-select-option :value="5000">
              >5s
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据源">
          <a-select
            v-model:value="query.dataSourceId"
            placeholder="全部数据源"
            style="width:200px"
            allow-clear
            @change="fetchData"
          >
            <a-select-option
              v-for="ds in dataSources"
              :key="ds.id"
              :value="ds.id"
            >
              {{ ds.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <a-divider />

      <!-- 慢查询列表（全量拉取后按阈值前端过滤、前端分页） -->
      <a-table
        :data-source="pagedList"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        size="small"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'queryTimeMs'">
            <a-tag :color="record.queryTimeMs > 5000 ? 'red' : record.queryTimeMs > 1000 ? 'orange' : 'blue'">
              {{ formatNumber(record.queryTimeMs) }}ms
            </a-tag>
          </template>
          <template v-if="column.key === 'rowsSent'">
            {{ formatNumber(record.rowsSent) }}
          </template>
          <template v-if="column.key === 'action'">
            <a @click="showDetail(record as SlowQueryItem)">详情</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="慢查询详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions
        :column="1"
        size="small"
        bordered
      >
        <a-descriptions-item label="SQL语句">
          <pre style="max-height:300px;overflow:auto;background:#f5f5f5;padding:8px;border-radius:4px;font-size:12px">{{ detailItem?.queryText }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="执行耗时">
          {{ formatNumber(detailItem?.queryTimeMs) }}ms
        </a-descriptions-item>
        <a-descriptions-item label="锁等待耗时">
          {{ formatNumber(detailItem?.lockTimeMs) }}ms
        </a-descriptions-item>
        <a-descriptions-item label="数据库">
          {{ detailItem?.databaseName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="执行时间">
          {{ detailItem?.queryTime || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="扫描行数">
          {{ formatNumber(detailItem?.rowsExamined) }}
        </a-descriptions-item>
        <a-descriptions-item label="返回行数">
          {{ formatNumber(detailItem?.rowsSent) }}
        </a-descriptions-item>
        <a-descriptions-item label="用户">
          {{ detailItem?.userName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="主机信息">
          {{ detailItem?.hostInfo || '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { slowQueryApi, dataSourceApi, type SlowQueryItem, type DataSourceItem } from '@/api/admin'

const loading = ref(false)
// 全量记录（后端 /export 返回过滤后全量，阈值过滤与分页在前端进行）
const allRows = ref<SlowQueryItem[]>([])
const dataSources = ref<DataSourceItem[]>([])
const detailVisible = ref(false)
const detailItem = ref<SlowQueryItem | null>(null)

const query = reactive({
  threshold: 1000,
  dataSourceId: undefined as number | undefined,
})

const paginationState = reactive({
  current: 1,
  pageSize: 20,
})

// ═══ 阈值过滤 + 前端分页 ═══
const filteredList = computed(() =>
  allRows.value.filter(r => (Number(r.queryTimeMs) || 0) > query.threshold)
)

// total 由阈值过滤后的数据集派生，过滤条件变化时自动同步
const pagination = computed(() => ({
  current: paginationState.current,
  pageSize: paginationState.pageSize,
  total: filteredList.value.length,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const pagedList = computed(() => {
  const start = (paginationState.current - 1) * paginationState.pageSize
  return filteredList.value.slice(start, start + paginationState.pageSize)
})

const columns = [
  { title: 'SQL语句', dataIndex: 'queryText', key: 'queryText', ellipsis: true, minWidth: 300 },
  { title: '耗时', dataIndex: 'queryTimeMs', key: 'queryTimeMs', width: 110 },
  { title: '数据库', dataIndex: 'databaseName', key: 'databaseName', width: 130 },
  { title: '执行时间', dataIndex: 'queryTime', key: 'queryTime', width: 170 },
  { title: '返回行', dataIndex: 'rowsSent', key: 'rowsSent', width: 90, align: 'right' as const },
  { title: '操作用户', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '操作', key: 'action', width: 60 },
]

// ═══ 统计卡片（基于阈值过滤后的数据集） ═══
const statCards = computed<StatCardItem[]>(() => {
  const durations = filteredList.value.map(r => Number(r.queryTimeMs) || 0)
  const total = durations.length
  const avg = total ? Math.round(durations.reduce((a, b) => a + b, 0) / total) : 0
  const max = total ? Math.max(...durations) : 0
  const over5s = durations.filter(d => d > 5000).length
  return [
    { label: '慢查询总数', value: total, suffix: '条' },
    { label: '平均耗时', value: avg, suffix: 'ms' },
    { label: '最长耗时', value: max, suffix: 'ms', valueStyle: { color: '#ff4d4f' } },
    { label: '超过5s', value: over5s, suffix: '条', valueStyle: { color: '#faad14' } },
  ]
})

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function handleFilterChange() {
  paginationState.current = 1
}

function handleTableChange(pag: any) {
  paginationState.current = pag.current
  paginationState.pageSize = pag.pageSize
}

function showDetail(record: SlowQueryItem) {
  detailItem.value = record
  detailVisible.value = true
}

async function fetchData() {
  loading.value = true
  try {
    const res = await slowQueryApi.exportAll(query.dataSourceId)
    allRows.value = res?.records || []
    paginationState.current = 1
  } catch {
    allRows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  fetchData()
  try {
    const res = await dataSourceApi.page({ page: 1, pageSize: 100 })
    dataSources.value = res?.records || []
  } catch (e) {
    console.warn('[慢查询] 数据源列表获取失败', e)
  }
})
</script>

<style scoped>
.search-form {
  row-gap: 8px;
}
</style>
