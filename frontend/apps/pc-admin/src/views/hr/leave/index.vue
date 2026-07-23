<template>
  <ErrorBoundary>
    <PageContainer title="请假管理">
      <template #extra>
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>新建请假
        </a-button>
      </template>

      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="员工姓名">
            <a-input v-model:value="searchForm.employeeName" placeholder="请输入" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item label="请假类型">
            <a-select v-model:value="searchForm.leaveType" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option v-for="(label, key) in LEAVE_TYPE_MAP" :key="key" :value="key">{{ label }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="0">待审批</a-select-option>
              <a-select-option :value="1">已批准</a-select-option>
              <a-select-option :value="2">已拒绝</a-select-option>
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
            <template v-if="column.key === 'leaveType'">
              <span>{{ LEAVE_TYPE_MAP[record.leaveType] || record.leaveType }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="LEAVE_STATUS_MAP[record.status]?.color">{{ LEAVE_STATUS_MAP[record.status]?.text }}</a-tag>
            </template>
            <template v-if="column.key === 'days'">
              <span style="font-weight:bold">{{ record.days }}天</span>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button v-if="record.status === 0" type="link" size="small" @click="handleApprove(record)">批准</a-button>
                <a-button v-if="record.status === 0" type="link" size="small" danger @click="handleReject(record)">拒绝</a-button>
                <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 新建/编辑请假弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑请假' : '新建请假'" width="560px" :confirm-loading="saving" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="请假类型" name="leaveType" required>
              <a-select v-model:value="form.leaveType">
                <a-select-option v-for="(label, key) in LEAVE_TYPE_MAP" :key="key" :value="key">{{ label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="请假天数" name="days" required>
              <a-input-number v-model:value="form.days" :min="0.5" :max="365" :step="0.5" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="开始日期" name="startDate" required>
              <a-date-picker v-model:value="form.startDate" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="结束日期" name="endDate" required>
              <a-date-picker v-model:value="form.endDate" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="请假原因" name="reason" required>
          <a-textarea v-model:value="form.reason" :rows="3" placeholder="请输入请假原因" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="请假详情" width="500px" :footer="null">
      <a-descriptions v-if="currentDetail" :column="2" bordered size="small">
        <a-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</a-descriptions-item>
        <a-descriptions-item label="请假类型">{{ LEAVE_TYPE_MAP[currentDetail.leaveType] || currentDetail.leaveType }}</a-descriptions-item>
        <a-descriptions-item label="开始日期">{{ currentDetail.startDate }}</a-descriptions-item>
        <a-descriptions-item label="结束日期">{{ currentDetail.endDate }}</a-descriptions-item>
        <a-descriptions-item label="请假天数">{{ currentDetail.days }}天</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="LEAVE_STATUS_MAP[currentDetail.status]?.color">{{ LEAVE_STATUS_MAP[currentDetail.status]?.text }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="请假原因" :span="2">{{ currentDetail.reason }}</a-descriptions-item>
        <a-descriptions-item v-if="currentDetail.approveComment" label="审批意见" :span="2">{{ currentDetail.approveComment }}</a-descriptions-item>
        <a-descriptions-item v-if="currentDetail.approveTime" label="审批时间">{{ currentDetail.approveTime }}</a-descriptions-item>
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
import { hrLeaveRequestApi, type HrLeaveRequest, LEAVE_TYPE_MAP, LEAVE_STATUS_MAP } from '@/api/hr'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<HrLeaveRequest[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const searchForm = reactive({ employeeName: '', leaveType: undefined as string | undefined, status: undefined as number | undefined })

const modalVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()
const currentDetail = ref<HrLeaveRequest | null>(null)

const form = reactive({ leaveType: 'ANNUAL', days: 1, startDate: null as any, endDate: null as any, reason: '' })
const rules: Record<string, any> = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  days: [{ required: true, message: '请输入请假天数', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假原因', trigger: 'blur' }],
}

const columns: any[] = [
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  { title: '请假类型', dataIndex: 'leaveType', key: 'leaveType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '天数', dataIndex: 'days', key: 'days', width: 70 },
  { title: '请假原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrLeaveRequestApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.employeeName = ''; searchForm.leaveType = undefined; searchForm.status = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  form.leaveType = 'ANNUAL'; form.days = 1; form.startDate = null; form.endDate = null; form.reason = ''
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    await hrLeaveRequestApi.submit(form as any)
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

async function handleApprove(record: any) {
  Modal.confirm({ title: '确认批准', content: '确定批准该请假申请吗？', onOk: async () => {
    await hrLeaveRequestApi.approve(record.id)
    message.success('批准成功'); loadData()
  }})
}

async function handleReject(record: any) {
  Modal.confirm({ title: '确认拒绝', content: '确定拒绝该请假申请吗？', okType: 'danger', onOk: async () => {
    await hrLeaveRequestApi.reject(record.id)
    message.success('已拒绝'); loadData()
  }})
}

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
