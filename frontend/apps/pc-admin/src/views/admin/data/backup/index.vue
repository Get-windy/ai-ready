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
            @click="openCreate"
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

    <a-alert
      type="info"
      show-icon
      style="margin-bottom:16px"
      message="自动备份策略配置与备份文件下载功能需后端补全对应端点，当前支持手动创建备份、恢复与删除。"
    />

    <!-- 统计卡片（基于当前数据源过滤后的全量记录计算） -->
    <ARStatCards
      :items="statCards"
      :loading="loading"
    />

    <a-card
      :bordered="false"
      title="备份记录"
    >
      <template #extra>
        <a-select
          v-model:value="query.dataSourceId"
          placeholder="全部数据源"
          style="width:200px"
          allow-clear
          size="small"
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
      </template>

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
          <template v-if="column.key === 'backupType'">
            <a-tag :color="record.backupType === 'full' ? 'blue' : 'cyan'">
              {{ record.backupType === 'full' ? '全量备份' : record.backupType === 'incremental' ? '增量备份' : record.backupType }}
            </a-tag>
          </template>
          <template v-if="column.key === 'dataSourceId'">
            {{ dataSourceName(record.dataSourceId) }}
          </template>
          <template v-if="column.key === 'fileSize'">
            {{ formatSize(record.fileSize) }}
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
              {{ STATUS_MAP[record.status]?.label || record.status }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-popconfirm
                title="恢复将覆盖目标库当前数据，确定继续?"
                ok-text="确定恢复"
                cancel-text="取消"
                @confirm="restoreBackup(record as BackupRecordItem)"
              >
                <a :class="{ 'text-disabled': record.status !== 'success' }">恢复</a>
              </a-popconfirm>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此备份记录?"
                @confirm="deleteBackup(record as BackupRecordItem)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建备份弹窗 -->
    <a-modal
      v-model:open="createVisible"
      title="创建备份"
      width="480px"
      :confirm-loading="creating"
      @ok="handleCreate"
    >
      <a-form layout="vertical">
        <a-form-item
          label="数据源"
          required
        >
          <a-select
            v-model:value="createForm.dataSourceId"
            placeholder="选择要备份的数据源"
          >
            <a-select-option
              v-for="ds in dataSources"
              :key="ds.id"
              :value="ds.id"
            >
              {{ ds.name }}（{{ ds.dbType }} · {{ ds.databaseName }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备份名称">
          <a-input
            v-model:value="createForm.backupName"
            placeholder="留空则由系统自动生成"
          />
        </a-form-item>
        <a-form-item label="备份类型">
          <a-select v-model:value="createForm.backupType">
            <a-select-option value="full">
              全量备份
            </a-select-option>
            <a-select-option value="incremental">
              增量备份
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, CloudUploadOutlined } from '@ant-design/icons-vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { backupApi, dataSourceApi, type BackupRecordItem, type DataSourceItem } from '@/api/admin'

// 备份状态（与后端 BackupRecord.status 一致：running/success/failed）
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  success: { label: '成功', color: 'green' },
  running: { label: '进行中', color: 'blue' },
  failed: { label: '失败', color: 'red' }
}

const loading = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const allRows = ref<BackupRecordItem[]>([])
const dataSources = ref<DataSourceItem[]>([])

const query = reactive({
  dataSourceId: undefined as number | undefined,
})

const createForm = reactive({
  dataSourceId: undefined as number | undefined,
  backupName: '',
  backupType: 'full',
})

const paginationState = reactive({ current: 1, pageSize: 20 })

const pagination = computed(() => ({
  current: paginationState.current,
  pageSize: paginationState.pageSize,
  total: allRows.value.length,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const pagedList = computed(() => {
  const start = (paginationState.current - 1) * paginationState.pageSize
  return allRows.value.slice(start, start + paginationState.pageSize)
})

const statCards = computed<StatCardItem[]>(() => {
  const rows = allRows.value
  const totalSize = rows.reduce((s, r) => s + (Number(r.fileSize) || 0), 0)
  const failed = rows.filter(r => r.status === 'failed').length
  const latest = rows.map(r => r.startTime || r.createTime || '').filter(Boolean).sort().pop()
  return [
    { label: '备份总数', value: rows.length, suffix: '份' },
    { label: '备份总大小', value: formatSize(totalSize) },
    { label: '失败备份', value: failed, suffix: '份', valueStyle: failed ? { color: '#ff4d4f' } : undefined },
    { label: '最近备份时间', value: latest || '-' },
  ]
})

const columns = [
  { title: '备份名称', dataIndex: 'backupName', key: 'backupName', minWidth: 180, ellipsis: true },
  { title: '数据源', dataIndex: 'dataSourceId', key: 'dataSourceId', width: 150 },
  { title: '备份类型', dataIndex: 'backupType', key: 'backupType', width: 100 },
  { title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100, align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

function formatSize(bytes: number | null | undefined): string {
  const n = Number(bytes)
  if (!n || isNaN(n) || n <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0
  let size = n
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(2) + ' ' + units[i]
}

function dataSourceName(id: number): string {
  const ds = dataSources.value.find(d => d.id === id)
  return ds ? ds.name : `数据源#${id}`
}

function handleTableChange(pag: any) {
  paginationState.current = pag.current
  paginationState.pageSize = pag.pageSize
}

function openCreate() {
  createForm.dataSourceId = dataSources.value[0]?.id
  createForm.backupName = ''
  createForm.backupType = 'full'
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.dataSourceId) { message.warning('请选择数据源'); return }
  creating.value = true
  try {
    await backupApi.create({
      dataSourceId: createForm.dataSourceId,
      backupName: createForm.backupName || undefined,
      backupType: createForm.backupType,
    })
    message.success('备份任务已创建')
    createVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '创建备份失败')
  } finally {
    creating.value = false
  }
}

async function restoreBackup(record: BackupRecordItem) {
  if (record.status !== 'success') return
  try {
    await backupApi.restore(record.id)
    message.success('恢复操作已启动')
  } catch (e: any) {
    message.error(e?.message || '恢复失败')
  }
  fetchData()
}

async function deleteBackup(record: BackupRecordItem) {
  try {
    await backupApi.remove(record.id)
    message.success('备份已删除')
  } catch (e: any) {
    message.error(e?.message || '删除备份失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    // 后端无统计端点且为内存分页，拉全量（上限1000条）用于统计卡片 + 前端分页
    const res = await backupApi.page({ dataSourceId: query.dataSourceId, page: 1, pageSize: 1000 })
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
    console.warn('[备份管理] 数据源列表获取失败', e)
  }
})
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
.text-disabled {
  color: #bbb;
  pointer-events: none;
}
</style>
