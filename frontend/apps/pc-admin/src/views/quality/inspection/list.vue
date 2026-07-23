<template>
  <ErrorBoundary>
    <PageContainer title="质量检验">
      <template #header-extra>
        <a-space>
          <a-button size="small" @click="activeTab = 'pending'">
            <template #icon><ClockCircleOutlined /></template>待检验
            <template v-if="pendingCount > 0"> ({{ pendingCount }})</template>
          </a-button>
          <a-button size="small" @click="activeTab = 'finished'">
            <template #icon><CheckCircleOutlined /></template>已检验
          </a-button>
          <a-button size="small" @click="activeTab = 'failed'">
            <template #icon><CloseCircleOutlined /></template>不合格
          </a-button>
        </a-space>
      </template>

      <a-tabs v-model:active-key="activeTab">
        <!-- 待检验 -->
        <a-tab-pane key="pending" tab="待检验">
          <ARReportPage
            ref="pendingRef"
            title="待检验"
            :query-fields="queryFields"
            :columns="inspectionColumns"
            :fetcher="pendingFetcher"
            export-file-name="待检验记录"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'inspectionResult'">
                <a-tag :color="resultColor(record.inspectionResult)">{{ resultText(record.inspectionResult) }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'quantity'">
                {{ record.quantity }} / 抽检: {{ record.sampleQuantity || record.quantity }}
              </template>
              <template v-else-if="column.dataIndex === 'resultCount'">
                <span :style="{ color: '#52c41a' }">{{ record.passQuantity || 0 }}</span>
                /
                <span :style="{ color: '#f5222d' }">{{ record.failQuantity || 0 }}</span>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button v-if="record.inspectionResult === 'PENDING'" type="link" size="small" @click="showCompleteModal(record)">完成检验</a-button>
                <a-button v-else type="link" size="small" @click="showDetail(record)">查看</a-button>
              </template>
            </template>
          </ARReportPage>
        </a-tab-pane>

        <!-- 已检验 -->
        <a-tab-pane key="finished" tab="已检验">
          <ARReportPage
            ref="finishedRef"
            title="已检验"
            :query-fields="queryFields"
            :columns="inspectionColumns"
            :fetcher="finishedFetcher"
            export-file-name="已检验记录"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'inspectionResult'">
                <a-tag :color="resultColor(record.inspectionResult)">{{ resultText(record.inspectionResult) }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'quantity'">
                {{ record.quantity }} / 抽检: {{ record.sampleQuantity || record.quantity }}
              </template>
              <template v-else-if="column.dataIndex === 'resultCount'">
                <span :style="{ color: '#52c41a' }">{{ record.passQuantity || 0 }}</span>
                /
                <span :style="{ color: '#f5222d' }">{{ record.failQuantity || 0 }}</span>
              </template>
            </template>
          </ARReportPage>
        </a-tab-pane>

        <!-- 不合格 -->
        <a-tab-pane key="failed" tab="不合格">
          <ARReportPage
            ref="failedRef"
            title="不合格"
            :query-fields="queryFields"
            :columns="inspectionColumns"
            :fetcher="failedFetcher"
            export-file-name="不合格记录"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'inspectionResult'">
                <a-tag :color="resultColor(record.inspectionResult)">{{ resultText(record.inspectionResult) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="showDefectModal(record)">不合格处理</a-button>
              </template>
            </template>
          </ARReportPage>
        </a-tab-pane>
      </a-tabs>

      <!-- 完成检验弹窗 -->
      <a-modal v-model:open="completeModalVisible" title="完成检验" @ok="handleComplete" :confirm-loading="completing">
        <a-alert type="info" show-icon style="margin-bottom: 12px" message="入库单关联的待检明细，检验结果将同步反馈至入库单状态。" />
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item v-if="currentInspection?.bizNo" label="来源单号">
            <a-input :value="currentInspection?.bizNo" disabled />
          </a-form-item>
          <a-form-item label="检验结果" required>
            <a-select v-model:value="completeForm.result">
              <a-select-option value="PASS">合格</a-select-option>
              <a-select-option value="CONCESSION">让步接收</a-select-option>
              <a-select-option value="FAIL">不合格（拒收）</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="合格数量">
            <a-input-number v-model:value="completeForm.passQuantity" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="不合格数量">
            <a-input-number v-model:value="completeForm.failQuantity" :min="0" style="width: 100%" />
          </a-form-item>
          <a-form-item label="检验备注">
            <a-textarea v-model:value="completeForm.remark" :rows="2" placeholder="检验备注（可选）" />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 不合格处理弹窗 -->
      <a-modal v-model:open="defectModalVisible" title="不合格处理" @ok="handleDefect" :confirm-loading="defecting">
        <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
          <a-form-item label="来源单号">
            <a-input :value="currentInspection?.bizNo" disabled />
          </a-form-item>
          <a-form-item label="缺陷类型" required>
            <a-select v-model:value="defectForm.defectType">
              <a-select-option value="QUALITY">质量缺陷</a-select-option>
              <a-select-option value="PACKAGING">包装缺陷</a-select-option>
              <a-select-option value="LABELING">标签缺陷</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="缺陷描述" required>
            <a-textarea v-model:value="defectForm.defectDesc" :rows="3" placeholder="请描述缺陷详情" />
          </a-form-item>
          <a-form-item label="缺陷数量">
            <a-input-number v-model:value="defectForm.defectQuantity" :min="0" style="width: 100%" />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { qualityInspectionApi, qualityDefectHandleApi } from '@/api/quality'

const activeTab = ref('pending')
const pendingRef = ref<any>(null)
const finishedRef = ref<any>(null)
const failedRef = ref<any>(null)
const pendingCount = ref(0)
const completeModalVisible = ref(false)
const defectModalVisible = ref(false)
const completing = ref(false)
const defecting = ref(false)
const currentInspection = ref<any>(null)
const completeForm = reactive({ result: 'PASS' as string, passQuantity: 0, failQuantity: 0, remark: '' })
const defectForm = reactive({ inspectionId: 0, defectType: 'QUALITY' as string, defectDesc: '', defectQuantity: 0 })

import { reactive } from 'vue'

function resultText(r: string) {
  const map: Record<string, string> = { PENDING: '待检验', PASS: '合格', CONCESSION: '让步接收', FAIL: '不合格' }
  return map[r] || r || '待检验'
}
function resultColor(r: string) {
  const map: Record<string, string> = { PENDING: 'orange', PASS: 'green', CONCESSION: 'blue', FAIL: 'red' }
  return map[r] || 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'bizNo', type: 'input', label: '来源单号', placeholder: '关联入库/采购单号', width: 160 },
  { key: 'productName', type: 'input', label: '产品名称', placeholder: '产品名称', width: 140 },
  { key: 'inspectorName', type: 'input', label: '检验员', placeholder: '检验员', width: 120 },
]

const inspectionColumns: any[] = [
  { title: '来源单号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150, ellipsis: true },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 110 },
  { title: '检验数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '检验结果', dataIndex: 'inspectionResult', key: 'inspectionResult', width: 100 },
  { title: '合格/不合格', dataIndex: 'resultCount', key: 'resultCount', width: 110 },
  { title: '检验员', dataIndex: 'inspectorName', key: 'inspectorName', width: 100 },
  { title: '检验时间', dataIndex: 'inspectionTime', key: 'inspectionTime', width: 160 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

function pendingFetcher(params: Record<string, any>) {
  return qualityInspectionApi.page({ ...params, pageNum: params.page, pageSize: params.size, result: 'PENDING' })
}
function finishedFetcher(params: Record<string, any>) {
  return qualityInspectionApi.page({ ...params, pageNum: params.page, pageSize: params.size })
}
function failedFetcher(params: Record<string, any>) {
  return qualityInspectionApi.page({ ...params, pageNum: params.page, pageSize: params.size, result: 'FAIL' })
}

function showCompleteModal(record: any) {
  currentInspection.value = record
  completeForm.result = record.inspectionResult === 'CONCESSION' ? 'CONCESSION' : 'PASS'
  completeForm.passQuantity = record.sampleQuantity || record.quantity
  completeForm.failQuantity = 0
  completeForm.remark = ''
  completeModalVisible.value = true
}

async function handleComplete() {
  if (!completeForm.result) { message.warning('请选择检验结果'); return }
  completing.value = true
  try {
    await qualityInspectionApi.complete(currentInspection.value.id, completeForm)
    message.success('检验完成')
    completeModalVisible.value = false
    reload()
  } catch (e: any) {
    message.error(e?.message || '检验完成失败')
  } finally {
    completing.value = false
  }
}

function showDefectModal(record: any) {
  currentInspection.value = record
  defectForm.inspectionId = record.id
  defectForm.defectType = 'QUALITY'
  defectForm.defectDesc = ''
  defectForm.defectQuantity = record.failQuantity || 0
  defectModalVisible.value = true
}

async function handleDefect() {
  if (!defectForm.defectDesc) { message.warning('请输入缺陷描述'); return }
  defecting.value = true
  try {
    await qualityDefectHandleApi.create(defectForm)
    message.success('已创建不合格处理记录')
    defectModalVisible.value = false
    reload()
  } catch (e: any) {
    message.error(e?.message || '创建不合格记录失败')
  } finally {
    defecting.value = false
  }
}

function showDetail(record: any) {
  message.info(`检验单: ${record.bizNo}, 结果: ${resultText(record.inspectionResult)}, 合格: ${record.passQuantity || 0}, 不合格: ${record.failQuantity || 0}`)
}

function reload() {
  pendingRef.value?.reload()
  finishedRef.value?.reload()
  failedRef.value?.reload()
  loadPendingCount()
}

async function loadPendingCount() {
  try {
    const res = await qualityInspectionApi.page({ pageNum: 1, pageSize: 1, result: 'PENDING' })
    pendingCount.value = res?.data?.data?.total || res?.total || 0
  } catch { /* ignore */ }
}

onMounted(loadPendingCount)
</script>
