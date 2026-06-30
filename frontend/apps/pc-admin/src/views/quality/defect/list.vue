<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">不合格处理</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'defectType'">
              <span>{{ DEFECT_TYPE_MAP[record.defectType]?.name || record.defectType }}</span>
            </template>
            <template v-if="column.key === 'handleType'">
              <a-tag v-if="record.handleType" :color="HANDLE_TYPE_MAP[record.handleType]?.color">{{ HANDLE_TYPE_MAP[record.handleType]?.name }}</a-tag>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'warning' : 'success'">{{ record.status === 0 ? '待处理' : '已处理' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" v-if="record.status === 0" @click="showHandleModal(record)">处理</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <a-modal v-model:open="handleModalVisible" title="处理不合格" @ok="handleProcess">
      <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="处理方式">
          <a-select v-model:value="handleForm.handleType">
            <a-select-option value="RETURN">退货</a-select-option>
            <a-select-option value="REWORK">返工</a-select-option>
            <a-select-option value="SCRAP">报废</a-select-option>
            <a-select-option value="SPECIAL_RELEASE">特采</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="处理数量">
          <a-input-number v-model:value="handleForm.handleQuantity" min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="处理结果">
          <a-textarea v-model:value="handleForm.handleResult" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { qualityDefectHandleApi, DEFECT_TYPE_MAP, HANDLE_TYPE_MAP, type QualityDefectHandle } from '@/api/quality'

const loading = ref(false)
const tableData = ref<QualityDefectHandle[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const handleModalVisible = ref(false)
const handleForm = reactive({ id: 0, handleType: 'REWORK', handleQuantity: 0, handleResult: '' })

const columns: any[] = [
  { title: '检验记录ID', dataIndex: 'inspectionId', key: 'inspectionId', width: 100 },
  { title: '缺陷类型', dataIndex: 'defectType', key: 'defectType', width: 100 },
  { title: '缺陷描述', dataIndex: 'defectDesc', key: 'defectDesc', width: 200 },
  { title: '缺陷数量', dataIndex: 'handleQuantity', key: 'handleQuantity', width: 80 },
  { title: '处理方式', dataIndex: 'handleType', key: 'handleType', width: 100 },
  { title: '处理人', dataIndex: 'handlerName', key: 'handlerName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await qualityDefectHandleApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.data.data.records
    pagination.total = result.data.data.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showHandleModal(record: any) {
  handleForm.id = record.id
  handleForm.handleType = 'REWORK'
  handleForm.handleQuantity = record.handleQuantity
  handleForm.handleResult = ''
  handleModalVisible.value = true
}

async function handleProcess() {
  if (!handleForm.handleResult) { message.warning('请输入处理结果'); return }
  await qualityDefectHandleApi.handle(handleForm.id, handleForm)
  message.success('处理完成')
  handleModalVisible.value = false
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