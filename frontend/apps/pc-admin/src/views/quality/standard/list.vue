<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">质检标准</h2>
      </div>
      <div class="page-header__right">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增标准
        </a-button>
      </div>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'inspectionType'">
              <a-tag :color="INSPECTION_TYPE_MAP[record.inspectionType]?.color">{{ INSPECTION_TYPE_MAP[record.inspectionType]?.name }}</a-tag>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'error'">{{ record.status === 1 ? '启用' : '禁用' }}</a-tag>
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

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑标准' : '新增标准'" width="600px" @ok="handleSave">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="标准编码">
          <a-input v-model:value="form.standardCode" placeholder="请输入标准编码" :disabled="editingId" />
        </a-form-item>
        <a-form-item label="标准名称" required>
          <a-input v-model:value="form.standardName" placeholder="请输入标准名称" />
        </a-form-item>
        <a-form-item label="检验类型" required>
          <a-select v-model:value="form.inspectionType" placeholder="请选择检验类型">
            <a-select-option value="INBOUND">入库检验</a-select-option>
            <a-select-option value="OUTBOUND">出库检验</a-select-option>
            <a-select-option value="PROCESS">过程检验</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="抽检比例">
          <a-input-number v-model:value="form.sampleRate" min="0" max="100" style="width: 100%" />
        </a-form-item>
        <a-form-item label="合格阈值">
          <a-input-number v-model:value="form.passThreshold" min="0" max="100" style="width: 100%" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { qualityStandardApi, INSPECTION_TYPE_MAP, type QualityStandard } from '@/api/quality'

const loading = ref(false)
const tableData = ref<QualityStandard[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ standardCode: '', standardName: '', inspectionType: '', sampleRate: 100, passThreshold: 95, description: '', status: 1 })

const columns: any[] = [
  { title: '标准编码', dataIndex: 'standardCode', key: 'standardCode', width: 120 },
  { title: '标准名称', dataIndex: 'standardName', key: 'standardName', width: 150 },
  { title: '检验类型', dataIndex: 'inspectionType', key: 'inspectionType', width: 100 },
  { title: '抽检比例', dataIndex: 'sampleRate', key: 'sampleRate', width: 80 },
  { title: '合格阈值', dataIndex: 'passThreshold', key: 'passThreshold', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await qualityStandardApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { standardCode: '', standardName: '', inspectionType: '', sampleRate: 100, passThreshold: 95, description: '', status: 1 })
  modalVisible.value = true
}

function showEditModal(record: QualityStandard) {
  editingId.value = record.id
  Object.assign(form, {
    standardCode: record.standardCode,
    standardName: record.standardName,
    inspectionType: record.inspectionType,
    sampleRate: record.sampleRate,
    passThreshold: record.passThreshold,
    description: record.description || '',
    status: record.status
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.standardName || !form.inspectionType) { message.warning('请填写必要字段'); return }
  try {
    if (editingId.value) {
      await qualityStandardApi.update(editingId.value, form)
      message.success('更新成功')
    } else {
      await qualityStandardApi.create(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) { message.error('保存失败') }
}

async function handleDelete(record: QualityStandard) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除标准 "${record.standardName}" 吗？`,
    okType: 'danger',
    onOk: async () => {
      await qualityStandardApi.delete(record.id)
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