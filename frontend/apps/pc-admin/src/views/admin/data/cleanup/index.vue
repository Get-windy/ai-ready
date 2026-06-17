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

function toggleRule(record: any) {
  message.success(record.enabled ? '规则已启用' : '规则已停用')
}

function deleteRule(record: any) {
  list.value = list.value.filter((l: any) => l.id !== record.id)
  message.success('清理规则已删除')
}

function handleCleanNow() {
  cleaning.value = true
  message.loading('正在执行数据清理...', 1.5)
  setTimeout(() => {
    cleaning.value = false
    message.success('数据清理完成')
    fetchCleanLogs()
  }, 3000)
}

function fetchCleanLogs() {
  cleanLogs.value = [
    { id: 1, ruleName: '操作日志清理', cleanedCount: 15230, duration: '3.2s', status: 'success', executedAt: new Date(Date.now() - 300000).toLocaleString() },
    { id: 2, ruleName: '登录日志清理', cleanedCount: 8910, duration: '1.8s', status: 'success', executedAt: new Date(Date.now() - 600000).toLocaleString() },
    { id: 3, ruleName: '临时数据清理', cleanedCount: 0, duration: '0.5s', status: 'success', executedAt: new Date(Date.now() - 3600000).toLocaleString() },
  ]
}

function fetchData() {
  loading.value = true
  setTimeout(() => {
    list.value = [
      { id: 1, name: '操作日志清理', tableName: 'sys_log', condition: 'create_time < 当前时间-90天', retentionDays: 90, schedule: '每日03:00', lastRun: new Date(Date.now() - 3600000).toLocaleString(), enabled: true },
      { id: 2, name: '登录日志清理', tableName: 'sys_login_log', condition: 'create_time < 当前时间-180天', retentionDays: 180, schedule: '每日04:00', lastRun: new Date(Date.now() - 7200000).toLocaleString(), enabled: true },
      { id: 3, name: '审计日志清理', tableName: 'sys_audit_log', condition: 'create_time < 当前时间-365天', retentionDays: 365, schedule: '每日05:00', lastRun: new Date(Date.now() - 86400000).toLocaleString(), enabled: true },
      { id: 4, name: '临时数据清理', tableName: 'temp_*', condition: 'expire_time < 当前时间', retentionDays: 7, schedule: '每小时', lastRun: new Date(Date.now() - 1800000).toLocaleString(), enabled: true },
      { id: 5, name: '消息通知清理', tableName: 'sys_notification', condition: 'is_read = 1 AND create_time < 当前时间-30天', retentionDays: 30, schedule: '每日06:00', lastRun: new Date(Date.now() - 43200000).toLocaleString(), enabled: false },
      { id: 6, name: 'API调用日志清理', tableName: 'sys_api_log', condition: 'create_time < 当前时间-60天', retentionDays: 60, schedule: '每日03:30', lastRun: null, enabled: true },
    ]
    fetchCleanLogs()
    loading.value = false
  }, 300)
}

onMounted(fetchData)
</script>
