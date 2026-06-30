<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">薪资发放</h2>
      </div>
      <div class="page-header__right">
        <a-month-picker v-model:value="generateMonth" placeholder="选择月份" style="width: 140px; margin-right: 8px" />
        <a-button type="primary" @click="handleGenerate">
          <template #icon><PlusOutlined /></template>
          生成薪资
        </a-button>
      </div>
    </div>
    <div class="page-container__body">
      <a-card :bordered="false" class="search-card">
        <a-form layout="inline">
          <a-form-item label="发放月份">
            <a-month-picker v-model:value="filterMonth" placeholder="选择月份" @change="() => { pagination.current = 1; loadData() }" allow-clear />
          </a-form-item>
          <a-form-item>
            <a-button @click="filterMonth = null as any; pagination.current = 1; loadData()">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
      <a-card :bordered="false" class="table-card">
        <a-table :columns="columns" :data-source="tableData" :loading="loading" :pagination="pagination" row-key="id" @change="handleTableChange">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actualAmount'">
              <span style="color: #52c41a; font-weight: bold">¥{{ record.actualAmount }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'warning'">{{ record.status === 1 ? '已发放' : '待发放' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" v-if="record.status === 0" @click="handleConfirm(record)">确认发放</a-button>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { hrSalaryApi, type HrSalaryPayment } from '@/api/hr'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref<HrSalaryPayment[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const generateMonth = ref<any>(dayjs())
const filterMonth = ref<any>(null)

const columns: any[] = [
  { title: '发放月份', dataIndex: 'paymentMonth', key: 'paymentMonth', width: 100 },
  { title: '员工ID', dataIndex: 'employeeId', key: 'employeeId', width: 80 },
  { title: '基本工资', dataIndex: 'baseAmount', key: 'baseAmount', width: 100 },
  { title: '绩效工资', dataIndex: 'performanceAmount', key: 'performanceAmount', width: 100 },
  { title: '津贴补贴', dataIndex: 'allowanceAmount', key: 'allowanceAmount', width: 100 },
  { title: '加班工资', dataIndex: 'overtimeAmount', key: 'overtimeAmount', width: 100 },
  { title: '扣款合计', dataIndex: 'deductAmount', key: 'deductAmount', width: 100 },
  { title: '社保扣款', dataIndex: 'socialDeduct', key: 'socialDeduct', width: 100 },
  { title: '公积金扣款', dataIndex: 'fundDeduct', key: 'fundDeduct', width: 100 },
  { title: '个税扣款', dataIndex: 'taxDeduct', key: 'taxDeduct', width: 100 },
  { title: '实发金额', dataIndex: 'actualAmount', key: 'actualAmount', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 100 }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrSalaryApi.pagePayments({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      paymentMonth: filterMonth.value || undefined
    })
    tableData.value = result.records
    pagination.total = result.total
  } finally { loading.value = false }
}

function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

async function handleGenerate() {
  const month = generateMonth.value ? dayjs(generateMonth.value).format('YYYY-MM') : dayjs().format('YYYY-MM')
  Modal.confirm({
    title: '生成月度薪资',
    content: `确定要生成 ${month} 月份的薪资数据吗？（已存在的记录不会重复生成）`,
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
    content: `确定要发放月份 "${record.paymentMonth}" 的薪资吗？`,
    onOk: async () => {
      await hrSalaryApi.confirmPayment(record.id)
      message.success('发放成功')
      loadData()
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.search-card { margin-bottom: 16px; }
.table-card { background: #fff; }
</style>