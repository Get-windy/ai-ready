<template>
  <ErrorBoundary>
    <PageContainer title="不合格处理">
      <ARReportPage
        ref="reportRef"
        title="不合格处理"
        :query-fields="queryFields"
        :columns="columns"
        :fetcher="fetcher"
        export-file-name="不合格处理记录"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'defectType'">
            <a-tag>{{ defectTypeText(record.defectType) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'handleType'">
            <a-tag v-if="record.handleType" :color="handleTypeColor(record.handleType)">{{ handleTypeText(record.handleType) }}</a-tag>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 0 || record.status === 'PENDING' ? 'warning' : 'success'">
              {{ record.status === 0 || record.status === 'PENDING' ? '待处理' : '已处理' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button v-if="record.status === 0 || record.status === 'PENDING'" type="link" size="small" @click="showHandleModal(record)">处理</a-button>
          </template>
        </template>
      </ARReportPage>

      <a-modal v-model:open="handleModalVisible" title="处理不合格" @ok="handleProcess" :confirm-loading="processing">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item v-if="currentRecord?.bizNo" label="来源单号">
            <a-input :value="currentRecord?.bizNo" disabled />
          </a-form-item>
          <a-form-item label="处理方式" required>
            <a-select v-model:value="handleForm.handleType">
              <a-select-option value="RETURN">退货</a-select-option>
              <a-select-option value="CONCESSION">让步接收</a-select-option>
              <a-select-option value="REWORK">返工</a-select-option>
              <a-select-option value="SCRAP">报废</a-select-option>
              <a-select-option value="SPECIAL_RELEASE">特采</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="处理数量">
            <a-input-number v-model:value="handleForm.handleQuantity" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="处理结果" required>
            <a-textarea v-model:value="handleForm.handleResult" :rows="3" placeholder="请输入处理结果" />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { qualityDefectHandleApi } from '@/api/quality'

const reportRef = ref<any>(null)
const handleModalVisible = ref(false)
const processing = ref(false)
const currentRecord = ref<any>(null)
const handleForm = reactive({ id: 0, handleType: 'REWORK' as string, handleQuantity: 0, handleResult: '' })

function defectTypeText(t: string) {
  const map: Record<string, string> = { QUALITY: '质量缺陷', PACKAGING: '包装缺陷', LABELING: '标签缺陷' }
  return map[t] || t || '-'
}
function handleTypeText(t: string) {
  const map: Record<string, string> = { RETURN: '退货', CONCESSION: '让步接收', REWORK: '返工', SCRAP: '报废', SPECIAL_RELEASE: '特采' }
  return map[t] || t || '-'
}
function handleTypeColor(t: string) {
  const map: Record<string, string> = { RETURN: 'red', CONCESSION: 'blue', REWORK: 'orange', SCRAP: 'default', SPECIAL_RELEASE: 'purple' }
  return map[t] || 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'inspectionId', type: 'input', label: '检验记录ID', placeholder: '关联检验ID', width: 120 },
  { key: 'defectType', type: 'select', label: '缺陷类型', placeholder: '全部', options: [
    { label: '全部', value: '' }, { label: '质量缺陷', value: 'QUALITY' }, { label: '包装缺陷', value: 'PACKAGING' }, { label: '标签缺陷', value: 'LABELING' },
  ]},
  { key: 'status', type: 'select', label: '状态', placeholder: '全部', options: [
    { label: '全部', value: '' }, { label: '待处理', value: 'PENDING' }, { label: '已处理', value: 'HANDLED' },
  ]},
]

const columns: any[] = [
  { title: '检验记录ID', dataIndex: 'inspectionId', key: 'inspectionId', width: 100 },
  { title: '来源单号', dataIndex: 'bizNo', key: 'bizNo', width: 150, ellipsis: true },
  { title: '缺陷类型', dataIndex: 'defectType', key: 'defectType', width: 100 },
  { title: '缺陷描述', dataIndex: 'defectDesc', key: 'defectDesc', width: 200, ellipsis: true },
  { title: '缺陷数量', dataIndex: 'defectQuantity', key: 'defectQuantity', width: 90, align: 'right' },
  { title: '处理方式', dataIndex: 'handleType', key: 'handleType', width: 100 },
  { title: '处理数量', dataIndex: 'handleQuantity', key: 'handleQuantity', width: 90, align: 'right' },
  { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' },
]

function fetcher(params: Record<string, any>) {
  return qualityDefectHandleApi.page({ ...params, pageNum: params.page, pageSize: params.size })
}

function showHandleModal(record: any) {
  currentRecord.value = record
  handleForm.id = record.id
  handleForm.handleType = record.handleType || 'REWORK'
  handleForm.handleQuantity = record.defectQuantity || 0
  handleForm.handleResult = ''
  handleModalVisible.value = true
}

async function handleProcess() {
  if (!handleForm.handleResult) { message.warning('请输入处理结果'); return }
  processing.value = true
  try {
    await qualityDefectHandleApi.handle(handleForm.id, handleForm)
    message.success('处理完成')
    handleModalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '处理失败')
  } finally {
    processing.value = false
  }
}
</script>
