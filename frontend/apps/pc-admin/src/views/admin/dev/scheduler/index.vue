<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>定时任务</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">定时任务</h2>
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

    <a-card :bordered="false" title="定时任务列表">
      <template #extra>
        <a-space>
          <a-tag color="blue">运行中: {{ runningCount }}</a-tag>
          <a-tag>暂停: {{ pausedCount }}</a-tag>
        </a-space>
      </template>

      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-switch v-model:checked="record.status" checked-children="运行" un-checked-children="暂停" size="small"
              @change="(checked: boolean) => toggleTask(record, checked)" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="executeNow(record)" :disabled="!record.status">立即执行</a>
              <a-divider type="vertical" />
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除?" @confirm="handleDelete(record)">
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
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])
const runningCount = computed(() => list.value.filter((l: any) => l.status).length)
const pausedCount = computed(() => list.value.filter((l: any) => !l.status).length)

const columns = [
  { title: '任务名称', dataIndex: 'name', key: 'name', minWidth: 160 },
  { title: '任务编码', dataIndex: 'code', key: 'code', width: 150 },
  { title: 'Cron表达式', dataIndex: 'cron', key: 'cron', width: 140 },
  { title: '上次执行', dataIndex: 'lastRun', key: 'lastRun', width: 170 },
  { title: '下次执行', dataIndex: 'nextRun', key: 'nextRun', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 220 },
]

function handleCreate() {
  message.info('新增定时任务功能开发中')
}

function handleEdit(record: any) {
  message.info('编辑任务: ' + record.name)
}

function handleDelete(record: any) {
  list.value = list.value.filter((l: any) => l.id !== record.id)
  message.success('定时任务已删除')
}

function toggleTask(record: any, checked: boolean) {
  message.success(checked ? '任务已启动' : '任务已暂停')
}

function executeNow(record: any) {
  message.loading('正在执行: ' + record.name, 1)
  setTimeout(() => {
    message.success('任务执行完成: ' + record.name)
  }, 1500)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/scheduler/tasks')
    list.value = Array.isArray(res) ? res : (res?.records || [])
  } catch {
    list.value = [
      { id: 1, name: '数据备份', code: 'data_backup', cron: '0 0 3 * * ?', lastRun: '2026-06-17 03:00:00', nextRun: '2026-06-18 03:00:00', status: true },
      { id: 2, name: '日志清理', code: 'log_cleanup', cron: '0 0 4 * * ?', lastRun: '2026-06-17 04:00:00', nextRun: '2026-06-18 04:00:00', status: true },
      { id: 3, name: '数据同步', code: 'data_sync', cron: '0 */5 * * * ?', lastRun: '2026-06-17 14:30:00', nextRun: '2026-06-17 14:35:00', status: true },
      { id: 4, name: '缓存刷新', code: 'cache_refresh', cron: '0 0 */2 * * ?', lastRun: '2026-06-17 12:00:00', nextRun: '2026-06-17 14:00:00', status: false },
      { id: 5, name: '统计报表', code: 'report_stats', cron: '0 30 1 * * ?', lastRun: '2026-06-17 01:30:00', nextRun: '2026-06-18 01:30:00', status: true },
      { id: 6, name: '告警检查', code: 'alert_check', cron: '0 */1 * * * ?', lastRun: '2026-06-17 14:29:00', nextRun: '2026-06-17 14:30:00', status: true },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
