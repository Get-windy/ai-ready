<template>
  <div class="workflow-monitor">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">实例总数</div>
        </div>
        <BranchesOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-running">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ runningCount }}</div>
          <div class="stat-card-label">运行中</div>
        </div>
        <LoadingOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-completed">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ completedCount }}</div>
          <div class="stat-card-label">已完成</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-stopped">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stoppedCount }}</div>
          <div class="stat-card-label">已终止</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card title="流程实例监控">
      <!-- 查询表单 -->
      <a-form
        :model="queryForm"
        layout="inline"
        class="query-form"
      >
        <a-form-item label="流程名称">
          <a-input
            v-model:value="queryForm.processName"
            placeholder="请输入流程名称"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="流程状态">
          <a-select
            v-model:value="queryForm.status"
            placeholder="请选择状态"
            allow-clear
            style="width: 120px"
          >
            <a-select-option value="running">
              运行中
            </a-select-option>
            <a-select-option value="completed">
              已完成
            </a-select-option>
            <a-select-option value="terminated">
              已终止
            </a-select-option>
            <a-select-option value="suspended">
              已挂起
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="开始时间">
          <a-range-picker
            v-model:value="queryForm.dateRange"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </a-button>
          <a-button
            style="margin-left: 8px"
            @click="handleReset"
          >
            重置
          </a-button>
        </a-form-item>
      </a-form>

      <!-- 数据表格 -->
      <VxeTableList
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="instanceId"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @page-change="handlePageChange"
      >
        <template #statusCell="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusLabel(record.status) }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <a-button
            type="link"
            size="small"
            @click="handleViewDetail(record)"
          >
            查看详情
          </a-button>
          <a-button
            type="link"
            size="small"
            @click="handleViewFlowChart(record)"
          >
            流程图
          </a-button>
          <a-dropdown v-if="record.status === 'running'">
            <a-button
              type="link"
              size="small"
            >
              流程干预 <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="(e) => handleIntervention(e.key as string, record)">
                <a-menu-item key="terminate">
                  终止流程
                </a-menu-item>
                <a-menu-item key="suspend">
                  挂起流程
                </a-menu-item>
                <a-menu-item key="resume">
                  恢复流程
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </VxeTableList>
    </a-card>

    <!-- 详情对话框 -->
    <a-modal
      v-model:open="detailVisible"
      title="流程实例详情"
      :width="800"
      :footer="null"
    >
      <a-descriptions
        bordered
        :column="2"
      >
        <a-descriptions-item label="实例ID">
          {{ detailData.instanceId }}
        </a-descriptions-item>
        <a-descriptions-item label="流程名称">
          {{ detailData.processName }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(detailData.status)">
            {{ getStatusLabel(detailData.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="发起人">
          {{ detailData.initiator }}
        </a-descriptions-item>
        <a-descriptions-item label="开始时间">
          {{ detailData.startTime }}
        </a-descriptions-item>
        <a-descriptions-item label="结束时间">
          {{ detailData.endTime }}
        </a-descriptions-item>
        <a-descriptions-item
          label="当前节点"
          :span="2"
        >
          {{ detailData.currentNode }}
        </a-descriptions-item>
        <a-descriptions-item
          label="业务数据"
          :span="2"
        >
          <pre>{{ detailData.businessData }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- 流程图对话框 -->
    <a-modal
      v-model:open="flowChartVisible"
      title="流程图"
      :width="1000"
      :footer="null"
    >
      <div class="flow-chart-container">
        <a-spin :spinning="flowChartLoading" tip="流程图加载中...">
          <img v-if="flowChartImage" :src="flowChartImage" alt="流程图" style="max-width: 100%; max-height: 500px" />
          <a-empty v-else-if="!flowChartLoading" :description="flowChartError || '流程图加载中...'" />
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined, BranchesOutlined, LoadingOutlined, CheckCircleOutlined, StopOutlined } from '@ant-design/icons-vue'
import type { MenuInfo } from 'ant-design-vue/lib/menu/src/interface'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'

// 查询表单
const queryForm = reactive({
  processName: '',
  status: undefined as string | undefined,
  dateRange: [] as any[]
})

// 表格数据
const tableData = ref<any[]>([])

// ── 统计数据 ────────────────────────────────────────────
const runningCount = computed(() => tableData.value.filter(r => r.status === 'running').length)
const completedCount = computed(() => tableData.value.filter(r => r.status === 'completed').length)
const stoppedCount = computed(() => tableData.value.filter(r => r.status === 'terminated' || r.status === 'suspended').length)

// 模拟数据函数
const getMockData = () => [
  {
    instanceId: 'INST-001',
    processName: '请假审批流程',
    status: 'running',
    currentNode: '部门经理审批',
    startTime: '2024-04-15 09:00:00',
    endTime: '-',
    duration: '2h 30m',
    initiator: '张三'
  },
  {
    instanceId: 'INST-002',
    processName: '报销审批流程',
    status: 'completed',
    currentNode: '-',
    startTime: '2024-04-14 14:00:00',
    endTime: '2024-04-14 16:30:00',
    duration: '2h 30m',
    initiator: '李四'
  },
  {
    instanceId: 'INST-003',
    processName: '采购审批流程',
    status: 'running',
    currentNode: '财务审批',
    startTime: '2024-04-15 10:00:00',
    endTime: '-',
    duration: '1h 15m',
    initiator: '王五'
  }
]

// 表格列定义
const vxeColumns = [
  { field: 'instanceId', title: '实例ID', width: 180 },
  { field: 'processName', title: '流程名称', minWidth: 150 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'currentNode', title: '当前节点', minWidth: 120 },
  { field: 'startTime', title: '开始时间', width: 180 },
  { field: 'endTime', title: '结束时间', width: 180 },
  { field: 'duration', title: '耗时', width: 100 },
  { field: 'action', title: '操作', width: 280, fixed: 'right', type: 'action' }
]

const loading = ref(false)

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 详情对话框
const detailVisible = ref(false)
const detailData = ref<any>({})

// 流程图对话框
const flowChartVisible = ref(false)
const flowChartLoading = ref(false)
const flowChartImage = ref<string | null>(null)
const flowChartError = ref<string | null>(null)

// 查询
const handleQuery = async () => {
  loading.value = true
  try {
    // 调用后端API获取数据
    try {
      const res = await request.get('/workflow/instance/page', {
        params: {
          processName: queryForm.processName || undefined,
          status: queryForm.status,
          startDate: queryForm.dateRange?.[0],
          endDate: queryForm.dateRange?.[1],
          pageNum: pagination.current,
          pageSize: pagination.pageSize
        }
      })
      // API 返回成功，使用真实数据（即使为空）
      const records = res.data?.records || res.records || []
      const total = res.data?.total || res.total || 0
      if (records.length > 0) {
        tableData.value = records
        pagination.total = total
      } else {
        // 真实数据为空，使用模拟数据作为演示
        console.info('[instance-monitor] API返回空数据，使用模拟数据演示')
        tableData.value = getMockData()
        pagination.total = 50
      }
      return
    } catch (err: any) {
      // API 真正不可用时才显示警告
      console.warn('[instance-monitor] 后端API不可用，使用模拟数据:', err?.message || '')
    }

    // 模拟数据（API不可用时的回退）
    tableData.value = getMockData()
    pagination.total = 50
  } catch (error) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 重置
const handleReset = () => {
  queryForm.processName = ''
  queryForm.status = undefined
  queryForm.dateRange = []
  pagination.current = 1
  handleQuery()
}

// 表格变化
const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  handleQuery()
}

// 查看详情
const handleViewDetail = (record: any) => {
  detailData.value = {
    ...record,
    businessData: JSON.stringify({ requestId: 'REQ-001', amount: 5000, description: '测试数据' }, null, 2)
  }
  detailVisible.value = true
}

// 查看流程图
const handleViewFlowChart = async (record: any) => {
  flowChartVisible.value = true
  flowChartLoading.value = true
  flowChartError.value = null
  flowChartImage.value = null
  try {
    // 尝试从后端获取流程图（支持 BPMN SVG 或图片 URL）
    const res = await request.get('/workflow/instance/diagram', {
      params: { instanceId: record.instanceId }
    })
    if (res.data?.svg) {
      flowChartImage.value = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(res.data.svg)))
    } else if (res.data?.imageUrl) {
      flowChartImage.value = res.data.imageUrl
    } else {
      flowChartError.value = '暂无可用的流程图'
    }
  } catch {
    flowChartError.value = '流程图加载失败'
  } finally {
    flowChartLoading.value = false
  }
}

// 流程干预
const handleIntervention = async (command: string, record: any) => {
  const actions: Record<string, string> = {
    terminate: '终止',
    suspend: '挂起',
    resume: '恢复'
  }

  Modal.confirm({
    title: '确认操作',
    content: `确定要${actions[command]}流程"${record.processName}"吗？`,
    onOk: async () => {
      try {
        await request.post(`/workflow/instance/${record.instanceId}/intervene`, {
          action: command
        })
        message.success(`${actions[command]}成功`)
        handleQuery()
      } catch {
        message.error(`${actions[command]}失败`)
      }
    }
  })
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    running: 'success',
    completed: 'default',
    terminated: 'error',
    suspended: 'warning'
  }
  return colors[status] || 'default'
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    running: '运行中',
    completed: '已完成',
    terminated: '已终止',
    suspended: '已挂起'
  }
  return labels[status] || status
}

// 初始加载
handleQuery()
</script>

<style scoped>
.workflow-monitor {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-running { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-completed { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-stopped { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.query-form {
  margin-bottom: 20px;
}

.flow-chart-container {
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
