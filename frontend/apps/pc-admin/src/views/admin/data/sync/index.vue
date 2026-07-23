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
            <a-breadcrumb-item>同步任务</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            同步任务
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="openCreate"
          >
            <template #icon>
              <PlusOutlined />
            </template>
            新增任务
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

    <a-card
      :bordered="false"
      title="同步任务列表"
    >
      <template #extra>
        <a-space>
          <a-tag color="green">
            运行中: {{ statusCounts.running }}
          </a-tag>
          <a-tag color="orange">
            已暂停: {{ statusCounts.paused }}
          </a-tag>
          <a-tag color="default">
            已停止: {{ statusCounts.stopped }}
          </a-tag>
        </a-space>
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
          <template v-if="column.key === 'sourceId'">
            {{ dataSourceName(record.sourceId) }}
          </template>
          <template v-if="column.key === 'targetId'">
            {{ dataSourceName(record.targetId) }}
          </template>
          <template v-if="column.key === 'syncType'">
            <a-tag :color="record.syncType === 'full' ? 'blue' : 'cyan'">
              {{ record.syncType === 'full' ? '全量同步' : record.syncType === 'incremental' ? '增量同步' : record.syncType }}
            </a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-badge
              :status="STATUS_MAP[record.status]?.badge || 'default'"
              :text="STATUS_MAP[record.status]?.label || record.status"
            />
          </template>
          <template v-if="column.key === 'lastSyncTime'">
            <span v-if="record.lastSyncTime">{{ record.lastSyncTime }}</span>
            <span
              v-else
              style="color:#999"
            >-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="executeTask(record as SyncTaskItem)">执行</a>
              <a-divider type="vertical" />
              <a @click="toggleTask(record as SyncTaskItem)">{{ record.status === 'running' ? '暂停' : '启用' }}</a>
              <a-divider type="vertical" />
              <a @click="openEdit(record as SyncTaskItem)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此任务?"
                @confirm="deleteTask(record as SyncTaskItem)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑同步任务弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑同步任务' : '新增同步任务'"
      :confirm-loading="modalLoading"
      :width="520"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        style="margin-top: 16px"
      >
        <a-form-item
          label="任务名称"
          required
        >
          <a-input
            v-model:value="modalForm.taskName"
            placeholder="请输入任务名称"
          />
        </a-form-item>
        <a-form-item
          label="源数据源"
          required
        >
          <a-select
            v-model:value="modalForm.sourceId"
            placeholder="请选择源数据源"
          >
            <a-select-option
              v-for="ds in dataSources"
              :key="ds.id"
              :value="ds.id"
            >
              {{ ds.name }}（{{ ds.databaseName }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="目标数据源"
          required
        >
          <a-select
            v-model:value="modalForm.targetId"
            placeholder="请选择目标数据源"
          >
            <a-select-option
              v-for="ds in dataSources"
              :key="ds.id"
              :value="ds.id"
            >
              {{ ds.name }}（{{ ds.databaseName }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同步方式">
          <a-select
            v-model:value="modalForm.syncType"
            placeholder="请选择"
          >
            <a-select-option value="full">
              全量同步
            </a-select-option>
            <a-select-option value="incremental">
              增量同步
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Cron表达式">
          <a-input
            v-model:value="modalForm.cronExpression"
            placeholder="如: 0 0 2 * * ?"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="modalForm.description"
            :rows="2"
            placeholder="任务描述"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { syncTaskApi, dataSourceApi, type SyncTaskItem, type DataSourceItem } from '@/api/admin'

// 任务状态（与后端 SyncTask.status 一致：running/paused/stopped）
const STATUS_MAP: Record<string, { label: string; badge: 'default' | 'error' | 'warning' | 'success' | 'processing' }> = {
  running: { label: '运行中', badge: 'processing' },
  paused: { label: '已暂停', badge: 'warning' },
  stopped: { label: '已停止', badge: 'default' }
}

const loading = ref(false)
const allRows = ref<SyncTaskItem[]>([])
const dataSources = ref<DataSourceItem[]>([])

const statusCounts = computed(() => ({
  running: allRows.value.filter(r => r.status === 'running').length,
  paused: allRows.value.filter(r => r.status === 'paused').length,
  stopped: allRows.value.filter(r => r.status === 'stopped').length,
}))

// ── 弹窗状态 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingRecord = ref<SyncTaskItem | null>(null)
const modalForm = reactive<Partial<SyncTaskItem>>({})

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

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', minWidth: 160, ellipsis: true },
  { title: '源数据源', dataIndex: 'sourceId', key: 'sourceId', width: 140 },
  { title: '目标数据源', dataIndex: 'targetId', key: 'targetId', width: 140 },
  { title: '同步方式', dataIndex: 'syncType', key: 'syncType', width: 100 },
  { title: 'Cron表达式', dataIndex: 'cronExpression', key: 'cronExpression', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '上次执行', dataIndex: 'lastSyncTime', key: 'lastSyncTime', width: 170 },
  { title: '操作', key: 'action', width: 220 },
]

function dataSourceName(id: number): string {
  const ds = dataSources.value.find(d => d.id === id)
  return ds ? ds.name : (id ? `数据源#${id}` : '-')
}

function handleTableChange(pag: any) {
  paginationState.current = pag.current
  paginationState.pageSize = pag.pageSize
}

function resetModalForm() {
  Object.keys(modalForm).forEach(k => delete (modalForm as Record<string, any>)[k])
}

function openCreate() {
  editingRecord.value = null
  resetModalForm()
  modalForm.syncType = 'incremental'
  modalVisible.value = true
}

function openEdit(record: SyncTaskItem) {
  editingRecord.value = record
  resetModalForm()
  Object.assign(modalForm, {
    taskName: record.taskName,
    sourceId: record.sourceId,
    targetId: record.targetId,
    syncType: record.syncType,
    cronExpression: record.cronExpression,
    description: record.description,
    status: record.status,
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.taskName?.trim()) { message.warning('请输入任务名称'); return }
  if (!modalForm.sourceId) { message.warning('请选择源数据源'); return }
  if (!modalForm.targetId) { message.warning('请选择目标数据源'); return }
  modalLoading.value = true
  try {
    if (editingRecord.value) {
      await syncTaskApi.update(editingRecord.value.id, modalForm)
      message.success('更新成功')
    } else {
      await syncTaskApi.create(modalForm)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

async function executeTask(record: SyncTaskItem) {
  try {
    await syncTaskApi.execute(record.id)
    message.success('同步任务已触发执行')
  } catch (e: any) {
    message.error(e?.message || '触发执行失败')
  }
  fetchData()
}

async function toggleTask(record: SyncTaskItem) {
  const nextStatus = record.status === 'running' ? 'paused' : 'running'
  try {
    // 后端无独立启停端点，用整体更新切换 status
    await syncTaskApi.update(record.id, { ...record, status: nextStatus })
    message.success(nextStatus === 'running' ? '任务已启用' : '任务已暂停')
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
  fetchData()
}

async function deleteTask(record: SyncTaskItem) {
  try {
    await syncTaskApi.remove(record.id)
    message.success('同步任务已删除')
  } catch (e: any) {
    message.error(e?.message || '删除同步任务失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    // 后端为内存分页且无状态统计端点，拉全量（上限1000条）用于状态计数 + 前端分页
    const res = await syncTaskApi.page({ page: 1, pageSize: 1000 })
    allRows.value = res?.records || []
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
    console.warn('[同步任务] 数据源列表获取失败', e)
  }
})
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
