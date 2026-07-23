<template>
  <ErrorBoundary>
    <PageContainer title="考勤管理">
      <template #extra>
        <a-space>
          <a-button @click="handleClockIn">
            <template #icon><ClockCircleOutlined /></template>签到
          </a-button>
          <a-button @click="handleClockOut">
            <template #icon><ClockCircleOutlined /></template>签退
          </a-button>
        </a-space>
      </template>

      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="员工姓名">
            <a-input v-model:value="searchForm.employeeName" placeholder="请输入" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item label="月份">
            <a-month-picker v-model:value="searchForm.month" style="width: 140px" />
          </a-form-item>
          <a-form-item label="考勤状态">
            <a-select v-model:value="searchForm.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option value="NORMAL">正常</a-select-option>
              <a-select-option value="LATE">迟到</a-select-option>
              <a-select-option value="EARLY">早退</a-select-option>
              <a-select-option value="ABSENT">缺勤</a-select-option>
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
            <template v-if="column.key === 'status'">
              <a-tag :color="ATTENDANCE_STATUS_MAP[record.status]?.color">
                {{ ATTENDANCE_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'lateMinutes'">
              <span v-if="record.lateMinutes > 0" style="color:#faad14">{{ record.lateMinutes }}分钟</span>
              <span v-else>-</span>
            </template>
            <template v-if="column.key === 'earlyMinutes'">
              <span v-if="record.earlyMinutes > 0" style="color:#faad14">{{ record.earlyMinutes }}分钟</span>
              <span v-else>-</span>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ClearOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { hrAttendanceApi, type HrAttendance, ATTENDANCE_STATUS_MAP } from '@/api/hr'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<HrAttendance[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const searchForm = reactive({
  employeeName: '',
  month: null as any,
  status: undefined as string | undefined,
})

const columns: any[] = [
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  { title: '考勤日期', dataIndex: 'attendanceDate', key: 'attendanceDate', width: 120 },
  { title: '上班时间', dataIndex: 'clockInTime', key: 'clockInTime', width: 100 },
  { title: '下班时间', dataIndex: 'clockOutTime', key: 'clockOutTime', width: 100 },
  { title: '工作时长', dataIndex: 'workHours', key: 'workHours', width: 80 },
  { title: '迟到', dataIndex: 'lateMinutes', key: 'lateMinutes', width: 80 },
  { title: '早退', dataIndex: 'earlyMinutes', key: 'earlyMinutes', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
]

async function loadData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm }
    if (searchForm.month) params.month = dayjs(searchForm.month).format('YYYY-MM')
    const result = await hrAttendanceApi.page(params)
    tableData.value = result.records
    pagination.total = result.total
  } catch { message.error('查询失败') }
  finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.employeeName = ''; searchForm.month = null; searchForm.status = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleClockIn() {
  try {
    await hrAttendanceApi.clockIn(0)
    message.success('签到成功')
    loadData()
  } catch { message.error('签到失败') }
}

async function handleClockOut() {
  try {
    await hrAttendanceApi.clockOut(0)
    message.success('签退成功')
    loadData()
  } catch { message.error('签退失败') }
}

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
