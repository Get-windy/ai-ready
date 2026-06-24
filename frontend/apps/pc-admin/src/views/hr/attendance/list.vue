<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">考勤管理</h2>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="ATTENDANCE_STATUS_MAP[record.status]?.color">{{ ATTENDANCE_STATUS_MAP[record.status]?.text }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { hrAttendanceApi, type HrAttendance, ATTENDANCE_STATUS_MAP } from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrAttendance[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })

const columns: any[] = [
  { title: '考勤日期', dataIndex: 'attendanceDate', key: 'attendanceDate', width: 120 },
  { title: '上班时间', dataIndex: 'clockInTime', key: 'clockInTime', width: 100 },
  { title: '下班时间', dataIndex: 'clockOutTime', key: 'clockOutTime', width: 100 },
  { title: '工作时长', dataIndex: 'workHours', key: 'workHours', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrAttendanceApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize })
    tableData.value = result.records
    pagination.total = result.total
  } catch (e) { message.error('查询失败') }
  finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }
onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.table-card { background: #fff; }
</style>