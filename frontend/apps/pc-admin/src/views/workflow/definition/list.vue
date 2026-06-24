<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">流程定义</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增流程
        </a-button>
      </div>
    </div>

    <div class="page-container__body">
      <a-card :bordered="false" class="search-card">
        <a-form layout="inline">
          <a-form-item label="流程名称">
            <a-input v-model:value="searchForm.processName" placeholder="请输入流程名称" allow-clear />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 120px">
              <a-select-option :value="0">草稿</a-select-option>
              <a-select-option :value="1">已发布</a-select-option>
              <a-select-option :value="2">已停用</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card :bordered="false" class="table-card">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'processType'">
              <span>{{ PROCESS_TYPE_MAP[record.processType] || '其他' }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="DEFINITION_STATUS_MAP[record.status]?.color">
                {{ DEFINITION_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-button type="link" size="small" v-if="record.status === 0" @click="handlePublish(record)">发布</a-button>
                <a-button type="link" size="small" v-if="record.status === 1" @click="handleDisable(record)">停用</a-button>
                <a-button type="link" size="small" danger v-if="record.status !== 1" @click="handleDelete(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑流程定义' : '新增流程定义'"
      width="600px"
      @ok="handleSave"
    >
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="流程编码" required>
          <a-input v-model:value="form.processCode" placeholder="请输入流程编码" :disabled="editingId" />
        </a-form-item>
        <a-form-item label="流程名称" required>
          <a-input v-model:value="form.processName" placeholder="请输入流程名称" />
        </a-form-item>
        <a-form-item label="流程类型" required>
          <a-select v-model:value="form.processType" placeholder="请选择流程类型">
            <a-select-option :value="1">请假审批</a-select-option>
            <a-select-option :value="2">报销审批</a-select-option>
            <a-select-option :value="3">采购审批</a-select-option>
            <a-select-option :value="4">销售审批</a-select-option>
            <a-select-option :value="5">费用审批</a-select-option>
            <a-select-option :value="6">其他审批</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="流程描述">
          <a-textarea v-model:value="form.description" placeholder="请输入流程描述" :rows="3" />
        </a-form-item>
        <a-form-item label="流程配置">
          <a-textarea v-model:value="form.processConfig" placeholder="请输入流程节点配置(JSON格式)" :rows="6" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  workflowDefinitionApi,
  type WorkflowDefinition,
  PROCESS_TYPE_MAP,
  DEFINITION_STATUS_MAP
} from '@/api/workflow'

const loading = ref(false)
const tableData = ref<WorkflowDefinition[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })

const searchForm = reactive({ processName: '', status: undefined as number | undefined })
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ processCode: '', processName: '', processType: 1, description: '', processConfig: '' })

const columns: any[] = [
  { title: '流程编码', dataIndex: 'processCode', key: 'processCode', width: 150 },
  { title: '流程名称', dataIndex: 'processName', key: 'processName', width: 200 },
  { title: '流程类型', dataIndex: 'processType', key: 'processType', width: 120 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await workflowDefinitionApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    tableData.value = result.records
    pagination.total = result.total
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.processName = ''; searchForm.status = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { processCode: '', processName: '', processType: 1, description: '', processConfig: '' })
  modalVisible.value = true
}

function showEditModal(record: WorkflowDefinition) {
  editingId.value = record.id
  Object.assign(form, {
    processCode: record.processCode,
    processName: record.processName,
    processType: record.processType,
    description: record.description || '',
    processConfig: record.processConfig || ''
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.processCode || !form.processName) {
    message.warning('请填写必要信息')
    return
  }
  try {
    if (editingId.value) {
      await workflowDefinitionApi.update(editingId.value, form)
      message.success('更新成功')
    } else {
      await workflowDefinitionApi.create(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

async function handlePublish(record: WorkflowDefinition) {
  Modal.confirm({
    title: '确认发布',
    content: `确定要发布流程 "${record.processName}" 吗？`,
    onOk: async () => {
      await workflowDefinitionApi.publish(record.id)
      message.success('发布成功')
      loadData()
    }
  })
}

async function handleDisable(record: WorkflowDefinition) {
  Modal.confirm({
    title: '确认停用',
    content: `确定要停用流程 "${record.processName}" 吗？`,
    onOk: async () => {
      await workflowDefinitionApi.disable(record.id)
      message.success('停用成功')
      loadData()
    }
  })
}

async function handleDelete(record: WorkflowDefinition) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除流程 "${record.processName}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await workflowDefinitionApi.delete(record.id)
      message.success('删除成功')
      loadData()
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.search-card { margin-bottom: 16px; }
.table-card { background: #fff; }
</style>