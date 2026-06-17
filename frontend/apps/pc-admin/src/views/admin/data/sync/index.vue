<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>同步任务</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">同步任务</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增任务
          </a-button>
          <a-button size="small" @click="fetchData" :loading="loading" style="margin-left:8px">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false" title="同步任务列表">
      <template #extra>
        <a-space>
          <a-tag color="green">运行中: {{ runningCount }}</a-tag>
          <a-tag color="default">已停止: {{ stoppedCount }}</a-tag>
          <a-tag color="red">异常: {{ errorCount }}</a-tag>
        </a-space>
      </template>

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
          <template v-if="column.key === 'status'">
            <a-badge
              :status="record.status === 'running' ? 'processing' : record.status === 'stopped' ? 'default' : 'error'"
              :text="{ running: '运行中', stopped: '已停止', error: '异常' }[record.status] || record.status"
            />
          </template>
          <template v-if="column.key === 'lastRun'">
            <span v-if="record.lastRun">{{ record.lastRun }}</span>
            <span v-else style="color:#999">-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a v-if="record.status === 'running'" @click="toggleTask(record)">停止</a>
              <a v-else @click="toggleTask(record)">启动</a>
              <a-divider type="vertical" />
              <a @click="editTask(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除此任务?" @confirm="deleteTask(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])
const runningCount = computed(() => list.value.filter((l: any) => l.status === 'running').length)
const stoppedCount = computed(() => list.value.filter((l: any) => l.status === 'stopped').length)
const errorCount = computed(() => list.value.filter((l: any) => l.status === 'error').length)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', minWidth: 160 },
  { title: '源数据库', dataIndex: 'sourceDb', key: 'sourceDb', width: 130 },
  { title: '目标数据库', dataIndex: 'targetDb', key: 'targetDb', width: 130 },
  { title: '同步方式', dataIndex: 'syncMode', key: 'syncMode', width: 100 },
  { title: '同步周期', dataIndex: 'cron', key: 'cron', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '上次执行', dataIndex: 'lastRun', key: 'lastRun', width: 170 },
  { title: '操作', key: 'action', width: 180 },
]

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function handleCreate() {
  message.info('新增同步任务功能开发中')
}

function editTask(record: any) {
  message.info('编辑同步任务: ' + record.name)
}

function toggleTask(record: any) {
  const newStatus = record.status === 'running' ? 'stopped' : 'running'
  record.status = newStatus
  message.success(newStatus === 'running' ? '任务已启动' : '任务已停止')
}

function deleteTask(record: any) {
  list.value = list.value.filter((l: any) => l.id !== record.id)
  message.success('同步任务已删除')
}

function fetchData() {
  loading.value = true
  setTimeout(() => {
    list.value = [
      { id: 1, name: '订单数据同步', sourceDb: '业务数据库', targetDb: '分析数据库', syncMode: '增量同步', cron: '每5分钟', status: 'running', lastRun: new Date().toLocaleString() },
      { id: 2, name: '用户信息同步', sourceDb: '核心数据库', targetDb: '备份数据库', syncMode: '全量同步', cron: '每日02:00', status: 'running', lastRun: new Date(Date.now() - 3600000).toLocaleString() },
      { id: 3, name: '日志归档同步', sourceDb: '业务数据库', targetDb: '日志数据库', syncMode: '增量同步', cron: '每小时', status: 'stopped', lastRun: new Date(Date.now() - 86400000).toLocaleString() },
      { id: 4, name: '产品数据同步', sourceDb: '核心数据库', targetDb: '缓存数据库', syncMode: '实时同步', cron: '实时', status: 'running', lastRun: new Date(Date.now() - 300000).toLocaleString() },
      { id: 5, name: '报表数据同步', sourceDb: '业务数据库', targetDb: '报表数据库', syncMode: '全量同步', cron: '每周一03:00', status: 'error', lastRun: new Date(Date.now() - 172800000).toLocaleString() },
      { id: 6, name: '库存数据同步', sourceDb: 'WMS数据库', targetDb: 'ERP数据库', syncMode: '增量同步', cron: '每10分钟', status: 'running', lastRun: new Date(Date.now() - 600000).toLocaleString() },
    ]
    pagination.total = list.value.length
    loading.value = false
  }, 300)
}

onMounted(fetchData)
</script>
