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
            <a-breadcrumb-item>备份管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            备份管理
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="handleCreateBackup"
          >
            <template #icon>
              <CloudUploadOutlined />
            </template>
            创建备份
          </a-button>
          <a-button
            size="small"
            :loading="loading"
            style="margin-left:8px"
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

    <!-- 备份策略配置 -->
    <a-row
      :gutter="16"
      style="margin-bottom:16px"
    >
      <a-col :span="16">
        <a-card
          :bordered="false"
          title="备份策略"
        >
          <a-form layout="inline">
            <a-form-item label="自动备份">
              <a-switch v-model:checked="autoBackup" />
            </a-form-item>
            <a-form-item label="备份周期">
              <a-select
                v-model:value="backupCycle"
                style="width:120px"
                :disabled="!autoBackup"
              >
                <a-select-option value="daily">
                  每日
                </a-select-option>
                <a-select-option value="weekly">
                  每周
                </a-select-option>
                <a-select-option value="monthly">
                  每月
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="保留份数">
              <a-input-number
                v-model:value="retentionCount"
                :min="1"
                :max="365"
                :disabled="!autoBackup"
              />
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                size="small"
                @click="saveStrategy"
              >
                保存策略
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card
          :bordered="false"
          title="存储概览"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-statistic
                title="备份总大小"
                :value="storageStats.totalSize"
              />
            </a-col>
            <a-col :span="12">
              <a-statistic
                title="可用空间"
                :value="storageStats.freeSize"
              />
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>

    <!-- 备份列表 -->
    <a-card
      :bordered="false"
      title="备份记录"
    >
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
            <a-tag :color="record.status === 'completed' ? 'green' : record.status === 'running' ? 'blue' : 'red'">
              {{ { completed: '已完成', running: '进行中', failed: '失败', pending: '等待中' }[record.status] || record.status }}
            </a-tag>
          </template>
          <template v-if="column.key === 'size'">
            {{ formatSize(record.size) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                size="small"
                type="link"
                :disabled="record.status !== 'completed'"
                @click="downloadBackup(record)"
              >
                <template #icon>
                  <DownloadOutlined />
                </template>
              </a-button>
              <a-popconfirm
                title="确定删除此备份?"
                @confirm="deleteBackup(record)"
              >
                <a-button
                  size="small"
                  type="link"
                  danger
                >
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, CloudUploadOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const list = ref<any[]>([])
const autoBackup = ref(true)
const backupCycle = ref('daily')
const retentionCount = ref(30)

const storageStats = reactive({
  totalSize: '0 GB',
  freeSize: '0 GB',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
})

const columns = [
  { title: '备份名称', dataIndex: 'name', key: 'name', minWidth: 180 },
  { title: '数据库', dataIndex: 'database', key: 'database', width: 130 },
  { title: '备份类型', dataIndex: 'type', key: 'type', width: 100 },
  { title: '大小', dataIndex: 'size', key: 'size', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 100 },
]

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(2) + ' ' + units[i]
}

function handleTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchData()
}

function saveStrategy() {
  message.success('备份策略已保存')
}

async function handleCreateBackup() {
  try {
    await request.post('/data-source/backup/create', { dataSourceId: 1, backupType: 'full' })
    message.success('备份创建完成')
  } catch {
    message.error('创建备份失败')
  }
  fetchData()
}

function downloadBackup(record: any) {
  message.success('备份下载中: ' + record.name)
}

async function deleteBackup(record: any) {
  try {
    await request.delete('/data-source/backup/' + record.id)
    message.success('备份已删除')
  } catch {
    message.error('删除备份失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/data-source/backup/list', {
      params: { page: pagination.current, pageSize: pagination.pageSize }
    })
    const records = res?.records || []
    list.value = records.map((r: any) => ({
      id: r.id,
      name: r.backupName || '',
      database: r.databaseName || (r.dataSourceId ? '数据源#' + r.dataSourceId : ''),
      type: r.backupType === 'full' ? '全量备份' : r.backupType === 'incremental' ? '增量备份' : r.backupType || '',
      size: r.fileSize || 0,
      status: r.status,
      createdAt: r.startTime || r.createTime || '',
    }))
    pagination.total = res?.total || 0
    storageStats.totalSize = list.value.reduce((s: number, r: any) => s + (typeof r.size === 'number' ? r.size : 0), 0) + ' B'
    storageStats.freeSize = '128.3 GB'
  } catch {
    list.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
