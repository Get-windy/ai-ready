<template>
  <ErrorBoundary>
    <PageContainer title="质检标准">
      <template #header-extra>
        <a-button type="primary" size="small" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>新增标准
        </a-button>
      </template>

      <ARReportPage
        ref="reportRef"
        title="质检标准"
        :query-fields="queryFields"
        :columns="columns"
        :fetcher="fetcher"
        export-file-name="质检标准"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'inspectionType'">
            <a-tag :color="typeColor(record.inspectionType)">{{ typeText(record.inspectionType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 1 || record.status === 'ACTIVE' ? 'success' : 'default'">
              {{ record.status === 1 || record.status === 'ACTIVE' ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'sampleRate'">
            {{ record.sampleRate != null ? record.sampleRate + '%' : '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'passThreshold'">
            {{ record.passThreshold != null ? record.passThreshold + '%' : '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
              <a-popconfirm title="确定删除该标准？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </ARReportPage>

      <a-modal v-model:open="modalVisible" :title="editingId ? '编辑标准' : '新增标准'" width="600px" @ok="handleSave" :confirm-loading="saving">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="标准编码">
            <a-input v-model:value="form.standardCode" placeholder="请输入标准编码" :disabled="!!editingId" />
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
            <a-input-number v-model:value="form.sampleRate" :min="0" :max="100" style="width: 100%">
              <template #addonAfter>%</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="合格阈值">
            <a-input-number v-model:value="form.passThreshold" :min="0" :max="100" style="width: 100%">
              <template #addonAfter>%</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="状态">
            <a-switch v-model:checked="form.active" checked-children="启用" un-checked-children="禁用" />
          </a-form-item>
          <a-form-item label="描述">
            <a-textarea v-model:value="form.description" placeholder="请输入描述" :rows="3" />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { qualityStandardApi } from '@/api/quality'

const reportRef = ref<any>(null)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ standardCode: '', standardName: '', inspectionType: '' as string, sampleRate: 100, passThreshold: 95, description: '', active: true })

function typeText(t: string) {
  const map: Record<string, string> = { INBOUND: '入库检验', OUTBOUND: '出库检验', PROCESS: '过程检验' }
  return map[t] || t || '-'
}
function typeColor(t: string) {
  const map: Record<string, string> = { INBOUND: 'blue', OUTBOUND: 'green', PROCESS: 'orange' }
  return map[t] || 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'standardCode', type: 'input', label: '标准编码', placeholder: '标准编码', width: 140 },
  { key: 'standardName', type: 'input', label: '标准名称', placeholder: '标准名称', width: 140 },
  { key: 'inspectionType', type: 'select', label: '检验类型', placeholder: '全部', options: [
    { label: '全部', value: '' }, { label: '入库检验', value: 'INBOUND' }, { label: '出库检验', value: 'OUTBOUND' }, { label: '过程检验', value: 'PROCESS' },
  ]},
]

const columns: any[] = [
  { title: '标准编码', dataIndex: 'standardCode', key: 'standardCode', width: 130 },
  { title: '标准名称', dataIndex: 'standardName', key: 'standardName', width: 180, ellipsis: true },
  { title: '检验类型', dataIndex: 'inspectionType', key: 'inspectionType', width: 100 },
  { title: '抽检比例', dataIndex: 'sampleRate', key: 'sampleRate', width: 90, align: 'right' },
  { title: '合格阈值', dataIndex: 'passThreshold', key: 'passThreshold', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description', width: 200, ellipsis: true },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return qualityStandardApi.page({ ...params, pageNum: params.page, pageSize: params.size })
}

function showCreateModal() {
  editingId.value = null
  form.standardCode = ''; form.standardName = ''; form.inspectionType = ''
  form.sampleRate = 100; form.passThreshold = 95; form.description = ''; form.active = true
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  form.standardCode = record.standardCode
  form.standardName = record.standardName
  form.inspectionType = record.inspectionType || ''
  form.sampleRate = record.sampleRate ?? 100
  form.passThreshold = record.passThreshold ?? 95
  form.description = record.description || ''
  form.active = record.status === 1 || record.status === 'ACTIVE'
  modalVisible.value = true
}

async function handleSave() {
  if (!form.standardName || !form.inspectionType) { message.warning('请填写必要字段'); return }
  if (!form.standardCode) { message.warning('请输入标准编码'); return }
  saving.value = true
  try {
    const payload = { ...form, status: form.active ? 1 : 0 }
    if (editingId.value) {
      await qualityStandardApi.update(editingId.value, payload as any)
      message.success('更新成功')
    } else {
      await qualityStandardApi.create(payload as any)
      message.success('创建成功')
    }
    modalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: any) {
  try {
    await qualityStandardApi.delete(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}
</script>
