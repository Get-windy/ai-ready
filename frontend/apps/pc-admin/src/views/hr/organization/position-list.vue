<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">岗位管理</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增岗位
        </a-button>
      </div>
    </div>

    <div class="page-container__body">
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
            <template v-if="column.key === 'positionLevel'">
              <span>{{ POSITION_LEVEL_MAP[record.positionLevel] || '基层' }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'error'">
                {{ record.status === 1 ? '启用' : '禁用' }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑岗位' : '新增岗位'" width="500px" @ok="handleSave">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="岗位编码">
          <a-input v-model:value="form.positionCode" placeholder="请输入岗位编码" :disabled="editingId" />
        </a-form-item>
        <a-form-item label="岗位名称" required>
          <a-input v-model:value="form.positionName" placeholder="请输入岗位名称" />
        </a-form-item>
        <a-form-item label="岗位级别">
          <a-select v-model:value="form.positionLevel" placeholder="请选择岗位级别">
            <a-select-option :value="1">高管</a-select-option>
            <a-select-option :value="2">中层</a-select-option>
            <a-select-option :value="3">基层</a-select-option>
            <a-select-option :value="4">普通</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="编制人数">
          <a-input-number v-model:value="form.quotaCount" min="1" max="100" />
        </a-form-item>
        <a-form-item label="岗位职责">
          <a-textarea v-model:value="form.responsibility" placeholder="请输入岗位职责" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { hrPositionApi, type HrPosition, POSITION_LEVEL_MAP } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrPosition[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ positionCode: '', positionName: '', positionLevel: 3, quotaCount: 1, responsibility: '' })

const columns: any[] = [
  { title: '岗位编码', dataIndex: 'positionCode', key: 'positionCode', width: 120 },
  { title: '岗位名称', dataIndex: 'positionName', key: 'positionName', width: 150 },
  { title: '岗位级别', dataIndex: 'positionLevel', key: 'positionLevel', width: 100 },
  { title: '编制人数', dataIndex: 'quotaCount', key: 'quotaCount', width: 100 },
  { title: '在岗人数', dataIndex: 'currentCount', key: 'currentCount', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrPositionApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.records
    pagination.total = result.total
  } finally {
    loading.value = false
  }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { positionCode: '', positionName: '', positionLevel: 3, quotaCount: 1, responsibility: '' })
  modalVisible.value = true
}

function showEditModal(record: HrPosition) {
  editingId.value = record.id
  Object.assign(form, {
    positionCode: record.positionCode,
    positionName: record.positionName,
    positionLevel: record.positionLevel,
    quotaCount: record.quotaCount,
    responsibility: record.responsibility || ''
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.positionName) { message.warning('请填写岗位名称'); return }
  try {
    if (editingId.value) {
      await hrPositionApi.update(editingId.value, form)
      message.success('更新成功')
    } else {
      await hrPositionApi.create(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) { message.error('保存失败') }
}

async function handleDelete(record: HrPosition) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除岗位 "${record.positionName}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await hrPositionApi.delete(record.id)
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
.table-card { background: #fff; }
</style>