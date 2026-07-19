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
            运行中: {{ runningCount }}
          </a-tag>
          <a-tag>暂停: {{ pausedCount }}</a-tag>
        </a-space>
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
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 1"
              checked-children="运行"
              un-checked-children="暂停"
              size="small"
              @change="(checked: boolean) => toggleTask(record, checked)"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a
                :disabled="!record.status"
                @click="executeNow(record)"
              >立即执行</a>
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
      :width="560"
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
        <a-form-item label="任务分组">
          <a-input
            v-model:value="modalForm.taskGroup"
            placeholder="请输入任务分组"
          />
        </a-form-item>
        <a-form-item
          label="任务类名"
          required
        >
          <a-input
            v-model:value="modalForm.taskClass"
            placeholder="全限定类名"
          />
        </a-form-item>
        <a-form-item
          label="Cron表达式"
          required
        >
          <a-input
            v-model:value="modalForm.cronExpression"
            placeholder="如: 0 0/30 * * * ?"
          >
            <template #suffix>
              <a-tooltip title="秒 分 时 日 月 周">
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="任务参数">
          <a-textarea
            v-model:value="modalForm.taskParams"
            :rows="2"
            placeholder="JSON格式，如 {&quot;key&quot;:&quot;value&quot;}"
          />
        </a-form-item>
        <a-form-item label="任务描述">
          <a-textarea
            v-model:value="modalForm.description"
            :rows="2"
            placeholder="请输入任务描述"
          />
        </a-form-item>
      </a-form>
    </a-modal>
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
const runningCount = computed(() => list.value.filter((l: any) => l.status === 1).length)
const pausedCount = computed(() => list.value.filter((l: any) => l.status === 0).length)

// ── 弹窗状态 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingTask = ref<any>(null)
const modalForm = reactive<Record<string, any>>({})

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName', minWidth: 160 },
  { title: '任务分组', dataIndex: 'taskGroup', key: 'taskGroup', width: 120 },
  { title: 'Cron表达式', dataIndex: 'cronExpression', key: 'cronExpression', width: 160 },
  { title: '上次执行', dataIndex: 'lastExecuteTime', key: 'lastExecuteTime', width: 170 },
  { title: '下次执行', dataIndex: 'nextExecuteTime', key: 'nextExecuteTime', width: 170 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 220 },
]

function resetModalForm() {
  Object.keys(modalForm).forEach(key => delete modalForm[key])
  modalForm.status = 1
  modalForm.taskGroup = 'DEFAULT'
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
  if (!modalForm.taskClass?.trim()) { message.warning('请输入任务类名'); return }
  if (!modalForm.cronExpression?.trim()) { message.warning('请输入Cron表达式'); return }

  modalLoading.value = true
  try {
    if (editingTask.value) {
      await request.put(`${API_BASE}/${editingTask.value.id}`, modalForm)
      message.success('更新成功')
    } else {
      await request.post(API_BASE, modalForm)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e.message || '操作失败')
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
  }).catch(() => {
    message.error(checked ? '启用失败' : '禁用失败')
  })
}

function executeNow(record: any) {
  message.loading('正在执行: ' + record.taskName, 1)
  request.post(`${API_BASE}/${record.id}/execute`).then(() => {
    message.success('任务执行完成: ' + record.taskName)
    fetchData()
  }).catch(() => {
    message.error('任务执行失败: ' + record.taskName)
  })
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

onMounted(fetchData)
</script>
