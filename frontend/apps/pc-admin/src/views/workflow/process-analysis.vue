<template>
  <div class="workflow-analysis">
    <!-- 统计卡片 -->
    <a-row :gutter="16">
      <a-col
        v-for="card in statisticCards"
        :key="card.title"
        :span="6"
      >
        <a-card>
          <a-statistic
            :title="card.title"
            :value="card.value"
          >
            <template #suffix>
              <span class="suffix">{{ card.suffix }}</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row
      :gutter="16"
      style="margin-top: 16px"
    >
      <!-- 流程耗时统计 -->
      <a-col :span="12">
        <a-card>
          <template #title>
            <div class="card-header">
              <span>流程耗时统计</span>
              <a-button
                type="link"
                @click="handleRefresh"
              >
                刷新
              </a-button>
            </div>
          </template>
          <a-table
            :columns="processDurationColumns"
            :data-source="processDurationData"
            :pagination="false"
            size="small"
            :scroll="{ y: 300 }"
          />
        </a-card>
      </a-col>

      <!-- 节点耗时分析 -->
      <a-col :span="12">
        <a-card>
          <template #title>
            <div class="card-header">
              <span>节点耗时分析</span>
              <a-select
                v-model:value="selectedProcess"
                placeholder="选择流程"
                size="small"
                style="width: 200px"
              >
                <a-select-option
                  v-for="item in processOptions"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </div>
          </template>
          <a-table
            :columns="nodeDurationColumns"
            :data-source="nodeDurationData"
            :pagination="false"
            size="small"
            :scroll="{ y: 300 }"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-row
      :gutter="16"
      style="margin-top: 16px"
    >
      <!-- 审批效率报表 -->
      <a-col :span="24">
        <a-card>
          <template #title>
            <div class="card-header">
              <span>审批效率报表</span>
              <a-range-picker
                v-model:value="reportDateRange"
                value-format="YYYY-MM-DD"
                size="small"
                @change="handleReportDateChange"
              />
            </div>
          </template>
          <a-table
            :columns="efficiencyColumns"
            :data-source="efficiencyData"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'completionRate'">
                <a-progress
                  :percent="record.completionRate"
                  :stroke-color="getProgressColor(record.completionRate)"
                />
              </template>
              <template v-else-if="column.key === 'efficiency'">
                <a-rate
                  v-model:value="record.efficiency"
                  disabled
                />
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-row
      :gutter="16"
      style="margin-top: 16px"
    >
      <!-- 流程趋势图 -->
      <a-col :span="12">
        <a-card title="流程实例趋势">
          <div class="chart-container">
            <a-empty description="图表组件开发中..." />
          </div>
        </a-card>
      </a-col>

      <!-- 节点分布图 -->
      <a-col :span="12">
        <a-card title="节点任务分布">
          <div class="chart-container">
            <a-empty description="图表组件开发中..." />
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps } from 'ant-design-vue'
import request from '@/utils/request'

// 统计卡片数据
const statisticCards = ref([
  { title: '流程实例总数', value: 1234, suffix: '个' },
  { title: '运行中实例', value: 56, suffix: '个' },
  { title: '待办任务数', value: 234, suffix: '个' },
  { title: '平均处理时长', value: 2.5, suffix: '天' }
])

// 流程耗时数据
const processDurationColumns = [
  { title: '流程名称', dataIndex: 'processName', key: 'processName', width: 150 },
  { title: '实例数', dataIndex: 'instanceCount', key: 'instanceCount', width: 80, align: 'center' as const },
  { title: '平均耗时', dataIndex: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' as const },
  { title: '最长耗时', dataIndex: 'maxDuration', key: 'maxDuration', width: 100, align: 'right' as const },
  { title: '最短耗时', dataIndex: 'minDuration', key: 'minDuration', width: 100, align: 'right' as const }
]

const processDurationData = ref([
  {
    processName: '请假审批流程',
    instanceCount: 120,
    avgDuration: '1.2天',
    maxDuration: '3天',
    minDuration: '0.5天'
  },
  {
    processName: '报销审批流程',
    instanceCount: 89,
    avgDuration: '2.5天',
    maxDuration: '5天',
    minDuration: '1天'
  },
  {
    processName: '采购审批流程',
    instanceCount: 67,
    avgDuration: '3.8天',
    maxDuration: '7天',
    minDuration: '2天'
  },
  {
    processName: '出差审批流程',
    instanceCount: 45,
    avgDuration: '1.8天',
    maxDuration: '4天',
    minDuration: '0.8天'
  }
])

// 节点耗时数据
const selectedProcess = ref('leave')
const processOptions = ref([
  { label: '请假审批流程', value: 'leave' },
  { label: '报销审批流程', value: 'expense' },
  { label: '采购审批流程', value: 'purchase' }
])

const nodeDurationColumns = [
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', width: 120 },
  { title: '任务数', dataIndex: 'taskCount', key: 'taskCount', width: 80, align: 'center' as const },
  { title: '平均耗时', dataIndex: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' as const },
  { title: '超时数', dataIndex: 'overdueCount', key: 'overdueCount', width: 80, align: 'center' as const },
  { title: '超时率', dataIndex: 'overdueRate', key: 'overdueRate', width: 80, align: 'center' as const }
]

const nodeDurationData = ref([
  { nodeName: '发起人提交', taskCount: 120, avgDuration: '0.5h', overdueCount: 2, overdueRate: '1.7%' },
  { nodeName: '部门经理审批', taskCount: 118, avgDuration: '4.2h', overdueCount: 8, overdueRate: '6.8%' },
  { nodeName: '人事审批', taskCount: 110, avgDuration: '2.5h', overdueCount: 3, overdueRate: '2.7%' }
])

// 审批效率数据
const reportDateRange = ref<any[]>([])
const efficiencyColumns = [
  { title: '日期', dataIndex: 'date', key: 'date', width: 120 },
  { title: '总任务数', dataIndex: 'totalTasks', key: 'totalTasks', width: 100, align: 'center' as const },
  { title: '完成数', dataIndex: 'completedTasks', key: 'completedTasks', width: 100, align: 'center' as const },
  { title: '完成率', dataIndex: 'completionRate', key: 'completionRate', width: 150 },
  { title: '平均耗时', dataIndex: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' as const },
  { title: '超时任务', dataIndex: 'overdueTasks', key: 'overdueTasks', width: 100, align: 'center' as const },
  { title: '效率评分', dataIndex: 'efficiency', key: 'efficiency', width: 150 }
]

const efficiencyData = ref([
  {
    date: '2024-04-08',
    totalTasks: 50,
    completedTasks: 48,
    completionRate: 96,
    avgDuration: '1.8天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-09',
    totalTasks: 55,
    completedTasks: 52,
    completionRate: 95,
    avgDuration: '2.1天',
    overdueTasks: 3,
    efficiency: 4.5
  },
  {
    date: '2024-04-10',
    totalTasks: 60,
    completedTasks: 58,
    completionRate: 97,
    avgDuration: '1.9天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-11',
    totalTasks: 48,
    completedTasks: 45,
    completionRate: 94,
    avgDuration: '2.3天',
    overdueTasks: 3,
    efficiency: 4
  },
  {
    date: '2024-04-12',
    totalTasks: 52,
    completedTasks: 50,
    completionRate: 96,
    avgDuration: '2.0天',
    overdueTasks: 2,
    efficiency: 4.5
  },
  {
    date: '2024-04-13',
    totalTasks: 58,
    completedTasks: 56,
    completionRate: 97,
    avgDuration: '1.7天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-14',
    totalTasks: 65,
    completedTasks: 62,
    completionRate: 95,
    avgDuration: '2.2天',
    overdueTasks: 3,
    efficiency: 4.5
  }
])

// 刷新数据
const handleRefresh = async () => {
  try {
    const res = await request.get('/workflow/analysis/refresh')
    if (res.data) {
      if (res.data.statistics) {
        const stats = res.data.statistics
        statisticCards.value = [
          { title: '流程实例总数', value: stats.totalInstances || 0, suffix: '个' },
          { title: '运行中实例', value: stats.runningInstances || 0, suffix: '个' },
          { title: '待办任务数', value: stats.todoTasks || 0, suffix: '个' },
          { title: '平均处理时长', value: stats.avgDuration || 0, suffix: '天' }
        ]
      }
      if (res.data.processDuration) {
        processDurationData.value = res.data.processDuration
      }
      if (res.data.nodeDuration) {
        nodeDurationData.value = res.data.nodeDuration
      }
    }
    message.success('数据刷新成功')
  } catch {
    message.error('刷新数据失败')
  }
}

// 报表日期变化
const handleReportDateChange = async () => {
  try {
    const res = await request.get('/workflow/analysis/report', {
      params: {
        startDate: reportDateRange.value?.[0],
        endDate: reportDateRange.value?.[1]
      }
    })
    if (res.data?.records?.length) {
      efficiencyData.value = res.data.records
    }
    message.success('报表数据加载成功')
  } catch {
    message.error('加载报表数据失败')
  }
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 95) return '#52c41a'
  if (percentage >= 90) return '#faad14'
  return '#f5222d'
}
</script>

<style scoped>
.workflow-analysis {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.suffix {
  font-size: 12px;
  color: #999;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
}
</style>
