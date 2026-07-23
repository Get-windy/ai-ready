<template>
  <ErrorBoundary>
    <PageContainer title="薪资管理">
      <template #extra>
        <a-space>
          <a-month-picker v-model:value="generateMonth" placeholder="选择月份" style="width: 140px" />
          <a-button type="primary" @click="handleGenerate"><template #icon><PlusOutlined /></template>生成薪资</a-button>
        </a-space>
      </template>

      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="发放月份">
            <a-month-picker v-model:value="filterMonth" placeholder="选择月份" allow-clear style="width: 140px" @change="() => { pagination.current = 1; loadData() }" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="0">待发放</a-select-option>
              <a-select-option :value="1">已发放</a-select-option>
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
            <template v-if="column.key === 'actualAmount'">
              <span style="color:#52c41a;font-weight:bold">¥{{ record.actualAmount?.toFixed(2) }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'warning'">{{ record.status === 1 ? '已发放' : '待发放' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button v-if="record.status === 0" type="link" size="small" @click="handleConfirm(record)">确认发放</a-button>
              <a-button v-if="record.status === 1" type="link" size="small" @click="showDetail(record)">查看</a-button>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 薪资详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="薪资详情" width="600px" :footer="null">
      <a-descriptions v-if="currentDetail" :column="2" bordered size="small">
        <a-descriptions-item label="发放月份">{{ currentDetail.paymentMonth }}</a-descriptions-item>
        <a-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</a-descriptions-item>
        <a-descriptions-item label="基本工资">¥{{ currentDetail.baseAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="绩效工资">¥{{ currentDetail.performanceAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="津贴补贴">¥{{ currentDetail.allowanceAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="加班工资">¥{{ currentDetail.overtimeAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="扣款合计">¥{{ currentDetail.deductAmount?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="社保扣款">¥{{ currentDetail.socialDeduct?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="公积金扣款">¥{{ currentDetail.fundDeduct?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="个税扣款">¥{{ currentDetail.taxDeduct?.toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="实发金额" :span="2">
          <span style="color:#52c41a;font-weight:bold;font-size:16px">¥{{ currentDetail.actualAmount?.toFixed(2) }}</span>
        </a-descriptions-item>
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
import { hrSalaryApi, type HrSalaryPayment } from '@/api/hr'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<HrSalaryPayment[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const generateMonth = ref<any>(dayjs())
const filterMonth = ref<any>(null)
const searchForm = reactive({ status: undefined as number | undefined })

const detailVisible = ref(false)
const currentDetail = ref<HrSalaryPayment | null>(null)

const columns: any[] = [
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  { title: '发放月份', dataIndex: 'paymentMonth', key: 'paymentMonth', width: 100 },
  { title: '基本工资', dataIndex: 'baseAmount', key: 'baseAmount', width: 100 },
  { title: '绩效工资', dataIndex: 'performanceAmount', key: 'performanceAmount', width: 100 },
  { title: '津贴补贴', dataIndex: 'allowanceAmount', key: 'allowanceAmount', width: 100 },
  { title: '加班工资', dataIndex: 'overtimeAmount', key: 'overtimeAmount', width: 100 },
  { title: '扣款合计', dataIndex: 'deductAmount', key: 'deductAmount', width: 100 },
  { title: '实发金额', dataIndex: 'actualAmount', key: 'actualAmount', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const params: any = { pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm }
    if (filterMonth.value) params.paymentMonth = dayjs(filterMonth.value).format('YYYY-MM')
    const result = await hrSalaryApi.pagePayments(params)
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.status = undefined; filterMonth.value = null; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleGenerate() {
  const month = generateMonth.value ? dayjs(generateMonth.value).format('YYYY-MM') : dayjs().format('YYYY-MM')
  Modal.confirm({
    title: '生成月度薪资',
    content: `确定要生成 ${month} 月份薪资数据吗？`,
    onOk: async () => {
      await hrSalaryApi.generateMonthlyPayment(month)
      message.success(`薪资生成完成: ${month}`)
      loadData()
    }
  })
}

async function handleConfirm(record: any) {
  Modal.confirm({
    title: '确认发放',
    content: `确定发放 ${record.paymentMonth} 月份薪资吗？`,
    onOk: async () => {
      await hrSalaryApi.confirmPayment(record.id)
      message.success('发放成功'); loadData()
    }
  })
}

function showDetail(record: any) {
  currentDetail.value = record
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
