<template>
  <ErrorBoundary>
    <PageContainer title="绩效管理">
      <template #extra>
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>新建考核
        </a-button>
      </template>

      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="考核周期">
            <a-input v-model:value="searchForm.reviewPeriod" placeholder="如：2026-Q1" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item label="考核类型">
            <a-select v-model:value="searchForm.reviewType" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option value="MONTHLY">月度考核</a-select-option>
              <a-select-option value="QUARTERLY">季度考核</a-select-option>
              <a-select-option value="ANNUAL">年度考核</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'score'">
              <span :style="{ fontWeight: 'bold', color: record.score >= 90 ? '#52c41a' : record.score >= 70 ? '#1890ff' : '#faad14' }">{{ record.score }}</span>
            </template>
            <template v-if="column.key === 'level'">
              <a-tag :color="PERFORMANCE_LEVEL_MAP[record.level]?.color">{{ PERFORMANCE_LEVEL_MAP[record.level]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
                <a-button v-if="record.status === 0" type="link" size="small" @click="handleConfirm(record)">确认</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 新建考核弹窗 -->
    <a-modal v-model:open="modalVisible" title="新建考核" width="600px" :confirm-loading="saving" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="考核周期" name="reviewPeriod" required>
              <a-input v-model:value="form.reviewPeriod" placeholder="如：2026-Q1" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="考核类型" name="reviewType" required>
              <a-select v-model:value="form.reviewType">
                <a-select-option value="MONTHLY">月度考核</a-select-option>
                <a-select-option value="QUARTERLY">季度考核</a-select-option>
                <a-select-option value="ANNUAL">年度考核</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="工作态度" name="attitudeScore">
              <a-input-number v-model:value="form.attitudeScore" :min="0" :max="100" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="工作能力" name="abilityScore">
              <a-input-number v-model:value="form.abilityScore" :min="0" :max="100" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="工作业绩" name="achievementScore">
              <a-input-number v-model:value="form.achievementScore" :min="0" :max="100" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="综合评价">
          <a-textarea v-model:value="form.comment" :rows="3" placeholder="请输入综合评价" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="考核详情" width="550px" :footer="null">
      <a-descriptions v-if="currentDetail" :column="2" bordered size="small">
        <a-descriptions-item label="考核周期">{{ currentDetail.reviewPeriod }}</a-descriptions-item>
        <a-descriptions-item label="考核类型">{{ currentDetail.reviewType }}</a-descriptions-item>
        <a-descriptions-item label="考核评分">{{ currentDetail.score }}</a-descriptions-item>
        <a-descriptions-item label="考核等级">
          <a-tag :color="PERFORMANCE_LEVEL_MAP[currentDetail.level]?.color">{{ PERFORMANCE_LEVEL_MAP[currentDetail.level]?.text }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="工作态度">{{ currentDetail.attitudeScore }}</a-descriptions-item>
        <a-descriptions-item label="工作能力">{{ currentDetail.abilityScore }}</a-descriptions-item>
        <a-descriptions-item label="工作业绩">{{ currentDetail.achievementScore }}</a-descriptions-item>
        <a-descriptions-item label="考核人">{{ currentDetail.reviewerName }}</a-descriptions-item>
        <a-descriptions-item label="综合评价" :span="2">{{ currentDetail.comment || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { hrPerformanceApi, type HrPerformance, PERFORMANCE_LEVEL_MAP } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrPerformance[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const searchForm = reactive({ reviewPeriod: '', reviewType: undefined as string | undefined })

const modalVisible = ref(false)
const detailVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const currentDetail = ref<HrPerformance | null>(null)

const form = reactive({ reviewPeriod: '', reviewType: 'MONTHLY', attitudeScore: 80, abilityScore: 80, achievementScore: 80, comment: '' })
const rules: Record<string, any> = {
  reviewPeriod: [{ required: true, message: '请输入考核周期', trigger: 'blur' }],
  reviewType: [{ required: true, message: '请选择考核类型', trigger: 'change' }],
}

const columns: any[] = [
  { title: '考核周期', dataIndex: 'reviewPeriod', key: 'reviewPeriod', width: 110 },
  { title: '考核类型', dataIndex: 'reviewType', key: 'reviewType', width: 100 },
  { title: '考核评分', dataIndex: 'score', key: 'score', width: 80 },
  { title: '考核等级', dataIndex: 'level', key: 'level', width: 90 },
  { title: '工作态度', dataIndex: 'attitudeScore', key: 'attitudeScore', width: 80 },
  { title: '工作能力', dataIndex: 'abilityScore', key: 'abilityScore', width: 80 },
  { title: '工作业绩', dataIndex: 'achievementScore', key: 'achievementScore', width: 80 },
  { title: '考核人', dataIndex: 'reviewerName', key: 'reviewerName', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrPerformanceApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.reviewPeriod = ''; searchForm.reviewType = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  form.reviewPeriod = ''; form.reviewType = 'MONTHLY'; form.attitudeScore = 80; form.abilityScore = 80; form.achievementScore = 80; form.comment = ''
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await hrPerformanceApi.submit(form as any)
    message.success('创建成功')
    modalVisible.value = false
    loadData()
  } catch { message.error('提交失败') }
  finally { saving.value = false }
}

function showDetail(record: any) {
  currentDetail.value = record
  detailVisible.value = true
}

async function handleConfirm(record: any) {
  Modal.confirm({ title: '确认考核', content: '确定确认该考核结果吗？', onOk: async () => {
    await hrPerformanceApi.confirm(record.id)
    message.success('确认成功'); loadData()
  }})
}

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
