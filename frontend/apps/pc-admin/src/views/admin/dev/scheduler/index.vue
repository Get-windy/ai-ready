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
            <a-breadcrumb-item>定时任务</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            定时任务
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="handleCreate"
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
      title="定时任务列表"
    >
      <template #extra>
        <a-space>
          <a-tag color="blue">
            启用中: {{ enabledCount }}
          </a-tag>
          <a-tag>停用: {{ disabledCount }}</a-tag>
        </a-space>
      </template>

      <a-alert
        type="info"
        show-icon
        style="margin-bottom:12px"
        message="任务只执行「已注册的任务处理器」，任务行不携带类名/方法名；需要新目标时由后端实现 JobHandler 注册。"
      >
        <template #description>
          <span>超时升级等改数据的任务，建议先用 <code>{"dryRun": true}</code> 演练（参数内一并配置）。</span>
        </template>
      </a-alert>

      <a-table
        :data-source="list"
        :columns="columns"
        :loading="loading"
        row-key="id"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'jobKey'">
            <a-tooltip :title="record.jobKey">
              {{ handlerName(record.jobKey) }}
            </a-tooltip>
          </template>
          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled === 1"
              checked-children="启用"
              un-checked-children="停用"
              size="small"
              @change="(checked: boolean) => toggleTask(record, checked)"
            />
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">
              {{ statusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'stat'">
            <span :class="{ 'text-danger': Number(record.failCount) > 0 }">
              {{ record.successCount ?? 0 }} / {{ record.failCount ?? 0 }}
            </span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="executeNow(record)">立即执行</a>
              <a-divider type="vertical" />
              <a @click="showLogs(record)">执行日志</a>
              <a-divider type="vertical" />
              <a @click="handleEdit(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除?"
                @confirm="handleDelete(record)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑任务弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingTask ? '编辑任务' : '新增任务'"
      :confirm-loading="modalLoading"
      :width="620"
      @ok="handleModalOk"
      @cancel="modalVisible = false"
    >
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 17 }"
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
          label="任务处理器"
          required
        >
          <a-select
            v-model:value="modalForm.jobKey"
            placeholder="请选择已注册的处理器"
            :options="handlerOptions"
            show-search
            option-filter-prop="label"
          />
        </a-form-item>
        <a-form-item
          label="Cron表达式"
          required
        >
          <a-input
            v-model:value="modalForm.cronExpression"
            placeholder="如: 0 */10 * * * ?"
          >
            <template #suffix>
              <a-tooltip title="秒 分 时 日 月 周">
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="任务类型">
          <a-select
            v-model:value="modalForm.taskType"
            :options="[
              { label: 'CRON（按表达式）', value: 'CRON' },
              { label: 'FIXED_RATE（固定频率）', value: 'FIXED_RATE' },
              { label: 'FIXED_DELAY（固定延迟）', value: 'FIXED_DELAY' },
            ]"
          />
        </a-form-item>
        <a-form-item label="执行参数">
          <a-textarea
            v-model:value="modalForm.executeParams"
            :rows="3"
            placeholder='JSON，原样传给处理器，如 {"dryRun": true}'
          />
        </a-form-item>
        <a-form-item label="任务描述">
          <a-textarea
            v-model:value="modalForm.taskDesc"
            :rows="2"
            placeholder="请输入任务描述"
          />
        </a-form-item>
        <a-form-item label="启用">
          <a-switch
            :checked="modalForm.enabled === 1"
            @change="(checked: boolean) => (modalForm.enabled = checked ? 1 : 0)"
          />
          <span style="margin-left:8px;color:#8c8c8c">停用时只保留配置，不参与调度</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 执行日志抽屉 -->
    <a-drawer
      v-model:open="logVisible"
      :title="`执行日志 — ${logTaskName}`"
      width="720"
      destroy-on-close
    >
      <a-table
        :data-source="logs"
        :columns="logColumns"
        :loading="logLoading"
        row-key="id"
        size="small"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'executeStatus'">
            <a-tag :color="record.executeStatus === 'SUCCESS' ? 'green' : 'red'">
              {{ record.executeStatus }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const API_BASE = '/scheduler/task'
const loading = ref(false)
const list = ref<any[]>([])
const handlers = ref<any[]>([])
const enabledCount = computed(() => list.value.filter((l: any) => l.enabled === 1).length)
const disabledCount = computed(() => list.value.filter((l: any) => l.enabled !== 1).length)

// ── 弹窗状态 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingTask = ref<any>(null)
const modalForm = reactive<Record<string, any>>({})

// ── 执行日志 ──
const logVisible = ref(false)
const logLoading = ref(false)
const logTaskName = ref('')
const logs = ref<any[]>([])

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', minWidth: 180 },
  { title: '任务处理器', dataIndex: 'jobKey', key: 'jobKey', minWidth: 180 },
  { title: 'Cron表达式', dataIndex: 'cronExpression', key: 'cronExpression', width: 150 },
  { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 90 },
  { title: '运行状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '上次执行', dataIndex: 'lastExecuteTime', key: 'lastExecuteTime', width: 170 },
  { title: '成功/失败', key: 'stat', width: 110 },
  { title: '操作', key: 'action', width: 260 },
]

const logColumns = [
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 170 },
  { title: '状态', dataIndex: 'executeStatus', key: 'executeStatus', width: 100 },
  { title: '执行结果', dataIndex: 'executeResult', key: 'executeResult', minWidth: 200 },
  { title: '失败原因', dataIndex: 'errorMessage', key: 'errorMessage', minWidth: 200 },
  { title: '耗时(ms)', dataIndex: 'executeTime', key: 'executeTime', width: 90 },
]

const handlerOptions = computed(() =>
  handlers.value.map((h: any) => ({ label: `${h.name}（${h.key}）`, value: h.key })))

function handlerName(key: string) {
  const hit = handlers.value.find((h: any) => h.key === key)
  return hit ? hit.name : (key || '—')
}

function statusText(status: string) {
  const map: Record<string, string> = { RUNNING: '运行中', STOPPED: '已停止', PAUSED: '已暂停', ERROR: '异常' }
  return map[status] || status || '—'
}

function statusColor(status: string) {
  const map: Record<string, string> = { RUNNING: 'green', STOPPED: 'default', PAUSED: 'orange', ERROR: 'red' }
  return map[status] || 'default'
}

function resetModalForm() {
  Object.keys(modalForm).forEach(key => delete modalForm[key])
  modalForm.taskType = 'CRON'
  modalForm.enabled = 0
  modalForm.executeParams = ''
}

function handleCreate() {
  editingTask.value = null
  resetModalForm()
  modalVisible.value = true
}

function handleEdit(record: any) {
  editingTask.value = record
  Object.assign(modalForm, { ...record })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.taskName?.trim()) { message.warning('请输入任务名称'); return }
  if (!modalForm.jobKey) { message.warning('请选择任务处理器'); return }
  if (!modalForm.cronExpression?.trim()) { message.warning('请输入Cron表达式'); return }
  if (modalForm.executeParams?.trim()) {
    try {
      JSON.parse(modalForm.executeParams)
    } catch {
      message.warning('执行参数必须是合法 JSON（留空表示无参数）')
      return
    }
  }

  modalLoading.value = true
  try {
    const payload = { ...modalForm, executeClass: undefined, executeMethod: undefined }
    if (editingTask.value) {
      await request.put(`${API_BASE}/${editingTask.value.id}`, payload)
      message.success('更新成功')
    } else {
      await request.post(API_BASE, payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || e?.response?.data?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

function handleDelete(record: any) {
  request.delete(`${API_BASE}/${record.id}`).then(() => {
    message.success('任务已删除')
    fetchData()
  }).catch(() => {
    message.error('删除失败')
  })
}

function toggleTask(record: any, checked: boolean) {
  const url = checked ? `${API_BASE}/${record.id}/enable` : `${API_BASE}/${record.id}/disable`
  request.post(url).then(() => {
    message.success(checked ? '任务已启用' : '任务已禁用')
    fetchData()
  }).catch((e: any) => {
    message.error(e?.message || e?.response?.data?.message || (checked ? '启用失败' : '禁用失败'))
  })
}

function executeNow(record: any) {
  message.loading('已提交执行: ' + record.taskName, 1)
  request.post(`${API_BASE}/${record.id}/execute`).then(() => {
    message.success('任务已触发，结果见「执行日志」')
    fetchData()
  }).catch((e: any) => {
    message.error(e?.message || e?.response?.data?.message || '执行失败')
  })
}

async function showLogs(record: any) {
  logTaskName.value = record.taskName
  logVisible.value = true
  logLoading.value = true
  try {
    const res = await request.get(`${API_BASE}/log/page`, { current: 1, size: 50, taskId: record.id })
    logs.value = res?.records || res?.data?.records || []
  } catch {
    logs.value = []
  } finally {
    logLoading.value = false
  }
}

async function fetchHandlers() {
  try {
    const res = await request.get(`${API_BASE}/handlers`)
    handlers.value = res || res?.data || []
  } catch {
    handlers.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get(`${API_BASE}/page`, { current: 1, size: 100 })
    const records = res?.records || res?.data?.records || []
    list.value = Array.isArray(records) ? records : []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHandlers()
  fetchData()
})
</script>
