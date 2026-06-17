<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>慢查询</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">慢查询</h2>
        </div>
        <div class="page-header-right">
          <a-button size="small" @click="fetchData" :loading="loading">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <!-- 统计顶部 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="6">
          <a-card size="small">
            <a-statistic title="慢查询总数" :value="stats.total" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small">
            <a-statistic title="平均耗时" :value="stats.avgDuration" suffix="ms" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small">
            <a-statistic title="最长耗时" :value="stats.maxDuration" suffix="ms" :value-style="{ color: '#ff4d4f' }" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small">
            <a-statistic title="查询超时" :value="stats.timeoutCount" :value-style="{ color: '#faad14' }" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 搜索 -->
      <a-form layout="inline" class="search-form">
        <a-form-item label="慢查询阈值">
          <a-select v-model:value="query.threshold" style="width:120px">
            <a-select-option value="100">>100ms</a-select-option>
            <a-select-option value="500">>500ms</a-select-option>
            <a-select-option value="1000">>1s</a-select-option>
            <a-select-option value="5000">>5s</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数据源">
          <a-select v-model:value="query.dataSource" placeholder="全部" style="width:160px" allowClear>
            <a-select-option v-for="ds in dataSources" :key="ds" :value="ds">{{ ds }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" size="small" @click="fetchData">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
        </a-form-item>
      </a-form>

      <a-divider />

      <!-- 慢查询列表 -->
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
          <template v-if="column.key === 'duration'">
            <a-tag :color="record.duration > 5000 ? 'red' : record.duration > 1000 ? 'orange' : 'blue'">{{ record.duration }}ms</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a @click="showDetail(record)">详情</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="慢查询详情" width="800px" :footer="null">
      <a-descriptions :column="1" size="small" bordered>
        <a-descriptions-item label="SQL语句">
          <pre style="max-height:300px;overflow:auto;background:#f5f5f5;padding:8px;border-radius:4px;font-size:12px">{{ detailItem?.sql }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="执行耗时">{{ detailItem?.duration }}ms</a-descriptions-item>
        <a-descriptions-item label="数据源">{{ detailItem?.dataSource }}</a-descriptions-item>
        <a-descriptions-item label="执行时间">{{ detailItem?.executedAt }}</a-descriptions-item>
        <a-descriptions-item label="返回行数">{{ detailItem?.rows }}</a-descriptions-item>
        <a-descriptions-item label="用户">{{ detailItem?.userName }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])
const detailVisible = ref(false)
const detailItem = ref<any>({})
const dataSources = ref<string[]>(['核心数据库', '业务数据库', '日志数据库'])

const query = reactive({
  threshold: '1000',
  dataSource: undefined as string | undefined,
})

const stats = reactive({
  total: 0,
  avgDuration: 0,
  maxDuration: 0,
  timeoutCount: 0,
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: 'SQL语句', dataIndex: 'sql', key: 'sql', ellipsis: true, minWidth: 300 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 100, sorter: true },
  { title: '数据源', dataIndex: 'dataSource', key: 'dataSource', width: 130 },
  { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt', width: 170 },
  { title: '返回行', dataIndex: 'rows', key: 'rows', width: 80 },
  { title: '操作用户', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '操作', key: 'action', width: 60 },
]

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function showDetail(record: any) {
  detailItem.value = record
  detailVisible.value = true
}

function fetchData() {
  loading.value = true
  // Simulate API call with mock data
  setTimeout(() => {
    const sqls = [
      'SELECT * FROM orders WHERE status = 0 ORDER BY create_time DESC LIMIT 1000',
      'SELECT o.*, u.name, p.title FROM orders o LEFT JOIN users u ON o.user_id = u.id LEFT JOIN products p ON o.product_id = p.id WHERE o.create_time > ?',
      'UPDATE inventory SET quantity = quantity - ? WHERE product_id = ? AND warehouse_id = ?',
      'SELECT DISTINCT category_id, COUNT(*) as cnt FROM products GROUP BY category_id HAVING COUNT(*) > 100',
      'INSERT INTO audit_log (user_id, action, target_type, target_id, detail, create_time) VALUES (?, ?, ?, ?, ?, ?)',
      'SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY create_time DESC) as rn FROM login_log) t WHERE rn = 1',
    ]
    list.value = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      sql: sqls[i % sqls.length],
      duration: [120, 1500, 350, 5200, 80, 2100, 450, 3200, 180, 620, 4100, 90][i],
      dataSource: dataSources.value[i % 3],
      executedAt: new Date(Date.now() - Math.random() * 86400000 * 7).toLocaleString(),
      rows: Math.floor(Math.random() * 5000) + 10,
      userName: ['admin', '张三', '李四', '王五'][i % 4],
    }))
    pagination.total = list.value.length

    stats.total = 156
    stats.avgDuration = 890
    stats.maxDuration = 5200
    stats.timeoutCount = 12

    loading.value = false
  }, 300)
}

onMounted(fetchData)
</script>
