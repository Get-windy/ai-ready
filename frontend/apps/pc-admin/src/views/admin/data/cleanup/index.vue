<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>清理规则</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">清理规则</h2>
        </div>
        <div class="page-header-right">
          <a-button type="primary" size="small" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新增规则
          </a-button>
          <a-button size="small" @click="fetchData" :loading="loading" style="margin-left:8px">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false" title="数据清理规则">
      <template #extra>
        <a-button size="small" @click="handleCleanNow" :loading="cleaning">
          <template #icon><ClearOutlined /></template>
          立即清理
        </a-button>
      </template>

      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'enabled'">
            <a-switch v-model:checked="record.enabled" size="small" @change="toggleRule(record)" />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="editRule(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm title="确定删除此规则?" @confirm="deleteRule(record)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 清理执行日志 -->
    <a-card :bordered="false" title="清理执行日志" style="margin-top:16px">
      <a-table
        :data-source="cleanLogs"
        :columns="logColumns"
        row-key="id"
        :pagination="{ pageSize: 5 }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'green' : 'red'">{{ record.status === 'success' ? '成功' : '失败' }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ClearOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const cleaning = ref(false)
const list = ref<any[]>([])
const cleanLogs = ref<any[]>([])

const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name', minWidth: 160 },
  { title: '数据表', dataIndex: 'tableName', key: 'tableName', width: 150 },
  { title: '清理条件', dataIndex: 'condition', key: 'condition', width: 200 },
  { title: '保留天数', dataIndex: 'retentionDays', key: 'retentionDays', width: 100 },
  { title: '执行周期', dataIndex: 'schedule', key: 'schedule', width: 120 },
  { title: '上次执行', dataIndex: 'lastRun', key: 'lastRun', width: 170 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 60 },
  { title: '操作', key: 'action', width: 140 },
]

const logColumns = [
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', minWidth: 160 },
  { title: '清理数据量', dataIndex: 'cleanedCount', key: 'cleanedCount', width: 120 },
  { title: '执行耗时', dataIndex: 'duration', key: 'duration', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '执行时间', dataIndex: 'executedAt', key: 'executedAt', width: 170 },
]

function handleCreate() {
  message.info('新增清理规则功能开发中')
}

function editRule(record: any) {
  message.info('编辑清理规则: ' + record.name)
}

async function toggleRule(record: any) {
  try {
    await request.put('/data-source/cleanup/' + record.id, {
      status: record.enabled ? 'running' : 'paused'
    })
    message.success(record.enabled ? '规则已启用' : '规则已停用')
  } catch {
    message.error('操作失败')
  }
}

async function deleteRule(record: any) {
  try {
    await request.delete('/data-source/cleanup/' + record.id)
    message.success('清理规则已删除')
  } catch {
    message.error('删除失败')
  }
  fetchData()
}

async function handleCleanNow() {
  cleaning.value = true
  const enabledRule = list.value.find((r: any) => r.enabled)
  if (enabledRule) {
    try {
      await request.post('/data-source/cleanup/' + enabledRule.id + '/execute')
      message.success('数据清理完成')
    } catch {
      message.error('清理执行失败')
    }
  } else {
    message.warning('没有启用的清理规则')
  }
  cleaning.value = false
  fetchCleanLogs()
}

async function fetchCleanLogs() {
  try {
    const res = await request.get('/data-source/cleanup/logs', { params: { page: 1, pageSize: 10 } })
    cleanLogs.value = res?.records || []
  } catch {
    cleanLogs.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/data-source/cleanup/list', {
      params: { page: 1, pageSize: 50 }
    })
    const records = res?.records || []
    list.value = records.map((r: any) => ({
      id: r.id,
      name: r.ruleName || '',
      tableName: r.targetTable || '',
      condition: r.conditionColumn || '',
      retentionDays: r.retentionDays || 0,
      schedule: r.cronExpression || '',
      lastRun: r.updateTime || '',
      enabled: r.status === 'running',
    }))
  } catch {
    list.value = []
  } finally {
    fetchCleanLogs()
    loading.value = false
  }
}

onMounted(fetchData)
</script>
