<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">检验记录</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'quantity'">
              <span>{{ record.quantity }} / 抽检: {{ record.sampleQuantity }}</span>
            </template>
            <template v-if="column.key === 'inspectionResult'">
              <a-tag :color="INSPECTION_RESULT_MAP[record.inspectionResult]?.color">{{ INSPECTION_RESULT_MAP[record.inspectionResult]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" v-if="record.inspectionResult === 'PENDING'" @click="showCompleteModal(record)">完成检验</a-button>
                <a-button type="link" size="small" v-if="record.inspectionResult === 'FAIL'" @click="showDefectModal(record)">不合格处理</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="completeModalVisible" title="完成检验" @ok="handleComplete">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="检验结果">
          <a-select v-model:value="completeForm.result">
            <a-select-option value="PASS">合格</a-select-option>
            <a-select-option value="FAIL">不合格</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合格数量">
          <a-input-number v-model:value="completeForm.passQuantity" min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="不合格数量">
          <a-input-number v-model:value="completeForm.failQuantity" min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="defectModalVisible" title="不合格处理" @ok="handleDefect">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="缺陷类型">
          <a-select v-model:value="defectForm.defectType">
            <a-select-option value="QUALITY">质量缺陷</a-select-option>
            <a-select-option value="PACKAGING">包装缺陷</a-select-option>
            <a-select-option value="LABELING">标签缺陷</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="缺陷描述">
          <a-textarea v-model:value="defectForm.defectDesc" :rows="3" />
        </a-form-item>
        <a-form-item label="缺陷数量">
          <a-input-number v-model:value="defectForm.defectQuantity" min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { qualityInspectionApi, qualityDefectHandleApi, INSPECTION_RESULT_MAP, type QualityInspection } from '@/api/quality'

const loading = ref(false)
const tableData = ref<QualityInspection[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const completeModalVisible = ref(false)
const defectModalVisible = ref(false)
const completeForm = reactive({ id: 0, result: 'PASS', passQuantity: 0, failQuantity: 0 })
const defectForm = reactive({ inspectionId: 0, defectType: 'QUALITY', defectDesc: '', defectQuantity: 0 })

const columns: any[] = [
  { title: '业务单号', dataIndex: 'bizNo', key: 'bizNo', width: 120 },
  { title: '产品名称', dataIndex: 'productName', key: 'productName', width: 150 },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 100 },
  { title: '检验数量', dataIndex: 'quantity', key: 'quantity', width: 120 },
  { title: '检验结果', dataIndex: 'inspectionResult', key: 'inspectionResult', width: 80 },
  { title: '合格/不合格', key: 'resultCount', width: 100, customRender: ({ record }: any) => `${record.passQuantity || 0}/${record.failQuantity || 0}` },
  { title: '检验员', dataIndex: 'inspectorName', key: 'inspectorName', width: 100 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await qualityInspectionApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCompleteModal(record: QualityInspection) {
  completeForm.id = record.id
  completeForm.result = 'PASS'
  completeForm.passQuantity = record.sampleQuantity
  completeForm.failQuantity = 0
  completeModalVisible.value = true
}

async function handleComplete() {
  await qualityInspectionApi.complete(completeForm.id, completeForm)
  message.success('检验完成')
  completeModalVisible.value = false
  loadData()
}

function showDefectModal(record: QualityInspection) {
  defectForm.inspectionId = record.id
  defectForm.defectType = 'QUALITY'
  defectForm.defectDesc = ''
  defectForm.defectQuantity = record.failQuantity
  defectModalVisible.value = true
}

async function handleDefect() {
  if (!defectForm.defectDesc) { message.warning('请输入缺陷描述'); return }
  await qualityDefectHandleApi.create(defectForm)
  message.success('已创建不合格处理记录')
  defectModalVisible.value = false
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>