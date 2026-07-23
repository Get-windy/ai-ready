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
            <a-breadcrumb-item>清理规则</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            清理规则
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
            新增规则
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
      title="数据清理规则"
    >
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
          <template v-if="column.key === 'retentionDays'">
            {{ formatNumber(record.retentionDays) }} 天
          </template>
          <template v-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 'running'"
              size="small"
              @change="(checked: boolean) => toggleRule(record as CleanupRuleItem, checked)"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-popconfirm
                title="立即按此规则清理数据?"
                @confirm="executeRule(record as CleanupRuleItem)"
              >
                <a>执行</a>
              </a-popconfirm>
              <a-divider type="vertical" />
              <a @click="openEdit(record as CleanupRuleItem)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定删除此规则?"
                @confirm="deleteRule(record as CleanupRuleItem)"
              >
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑清理规则弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingRecord ? '编辑清理规则' : '新增清理规则'"
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
          label="规则名称"
          required
        >
          <a-input
            v-model:value="modalForm.ruleName"
            placeholder="请输入规则名称"
          />
        </a-form-item>
        <a-form-item
          label="目标数据表"
          required
        >
          <a-input
            v-model:value="modalForm.targetTable"
            placeholder="如: sys_operation_log"
          />
        </a-form-item>
        <a-form-item label="条件列">
          <a-input
            v-model:value="modalForm.conditionColumn"
            placeholder="如: create_time"
          />
        </a-form-item>
        <a-form-item label="保留天数">
          <a-input-number
            v-model:value="modalForm.retentionDays"
            :min="1"
            style="width: 100%"
            placeholder="超过天数的数据将被清理"
          />
        </a-form-item>
        <a-form-item label="Cron表达式">
          <a-input
            v-model:value="modalForm.cronExpression"
            placeholder="如: 0 0 3 * * ?"
          />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="modalForm.description"
            :rows="2"
            placeholder="规则描述"
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
import { cleanupRuleApi, type CleanupRuleItem } from '@/api/admin'

const loading = ref(false)
const allRows = ref<CleanupRuleItem[]>([])

// ── 弹窗状态 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingRecord = ref<CleanupRuleItem | null>(null)
const modalForm = reactive<Partial<CleanupRuleItem>>({})

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
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', minWidth: 160, ellipsis: true },
  { title: '数据表', dataIndex: 'targetTable', key: 'targetTable', width: 160 },
  { title: '条件列', dataIndex: 'conditionColumn', key: 'conditionColumn', width: 130 },
  { title: '保留天数', dataIndex: 'retentionDays', key: 'retentionDays', width: 100, align: 'right' as const },
  { title: '执行周期', dataIndex: 'cronExpression', key: 'cronExpression', width: 130 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '启用', dataIndex: 'status', key: 'status', width: 70 },
  { title: '操作', key: 'action', width: 180 },
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
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
  modalForm.retentionDays = 90
  modalVisible.value = true
}

function openEdit(record: CleanupRuleItem) {
  editingRecord.value = record
  resetModalForm()
  Object.assign(modalForm, {
    ruleName: record.ruleName,
    targetTable: record.targetTable,
    conditionColumn: record.conditionColumn,
    retentionDays: record.retentionDays,
    cronExpression: record.cronExpression,
    description: record.description,
    status: record.status,
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.ruleName?.trim()) { message.warning('请输入规则名称'); return }
  if (!modalForm.targetTable?.trim()) { message.warning('请输入目标数据表'); return }
  modalLoading.value = true
  try {
    if (editingRecord.value) {
      await cleanupRuleApi.update(editingRecord.value.id, modalForm)
      message.success('更新成功')
    } else {
      await cleanupRuleApi.create(modalForm)
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

async function toggleRule(record: CleanupRuleItem, checked: boolean) {
  const nextStatus = checked ? 'running' : 'paused'
  try {
    // 后端无独立启停端点，用整体更新切换 status
    await cleanupRuleApi.update(record.id, { ...record, status: nextStatus })
    record.status = nextStatus
    message.success(checked ? '规则已启用' : '规则已停用')
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function executeRule(record: CleanupRuleItem) {
  try {
    await cleanupRuleApi.execute(record.id)
    message.success('清理任务已触发执行')
  } catch (e: any) {
    message.error(e?.message || '清理执行失败')
  }
  fetchData()
}

async function deleteRule(record: CleanupRuleItem) {
  try {
    await cleanupRuleApi.remove(record.id)
    message.success('清理规则已删除')
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    // 后端为内存分页，拉全量（上限1000条）做前端分页
    const res = await cleanupRuleApi.page({ page: 1, pageSize: 1000 })
    allRows.value = res?.records || []
  } catch {
    allRows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
