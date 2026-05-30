<template>
  <div class="opportunity-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>商机管理</h2>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新建商机
        </a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="商机名称">
          <a-input v-model:value="searchForm.name" placeholder="请输入商机名称" allow-clear />
        </a-form-item>
        <a-form-item label="客户名称">
          <a-input v-model:value="searchForm.customerName" placeholder="请输入客户名称" allow-clear />
        </a-form-item>
        <a-form-item label="商机阶段">
          <a-select v-model:value="searchForm.stage" placeholder="请选择阶段" allow-clear style="width: 150px">
            <a-select-option value="lead">线索</a-select-option>
            <a-select-option value="qualification">资格确认</a-select-option>
            <a-select-option value="proposal">方案报价</a-select-option>
            <a-select-option value="negotiation">商务谈判</a-select-option>
            <a-select-option value="closing">成交阶段</a-select-option>
            <a-select-option value="won">已成交</a-select-option>
            <a-select-option value="lost">已流失</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计金额">
          <a-input-number v-model:value="searchForm.minAmount" placeholder="最小金额" style="width: 120px" />
          <span style="margin: 0 8px">-</span>
          <a-input-number v-model:value="searchForm.maxAmount" placeholder="最大金额" style="width: 120px" />
        </a-form-item>
        <a-form-item label="负责人">
          <a-select v-model:value="searchForm.ownerId" placeholder="请选择负责人" allow-clear show-search :filter-option="filterOption" style="width: 150px">
            <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
              {{ user.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="pipeline" tab="商机管道">
          <div class="pipeline-container">
            <div class="pipeline-stage" v-for="stage in pipelineStages" :key="stage.key">
              <div class="stage-header">
                <span class="stage-name">{{ stage.name }}</span>
                <span class="stage-count">{{ getStageCount(stage.key) }}</span>
                <span class="stage-amount">¥{{ formatAmount(getStageAmount(stage.key)) }}</span>
              </div>
              <div class="stage-cards">
                <div
                  v-for="opportunity in getStageOpportunities(stage.key)"
                  :key="opportunity.id"
                  class="opportunity-card"
                  @click="handleView(opportunity)"
                >
                  <div class="card-header">
                    <span class="card-title">{{ opportunity.name }}</span>
                    <a-tag :color="getPriorityColor(opportunity.priority)">{{ getPriorityText(opportunity.priority) }}</a-tag>
                  </div>
                  <div class="card-body">
                    <div class="card-row">
                      <span class="label">客户:</span>
                      <span class="value">{{ opportunity.customerName }}</span>
                    </div>
                    <div class="card-row">
                      <span class="label">金额:</span>
                      <span class="value amount">¥{{ formatAmount(opportunity.expectedAmount) }}</span>
                    </div>
                    <div class="card-row">
                      <span class="label">负责人:</span>
                      <span class="value">{{ opportunity.ownerName }}</span>
                    </div>
                  </div>
                  <div class="card-footer">
                    <span class="close-date">预计成交: {{ opportunity.expectedCloseDate }}</span>
                    <div class="card-actions">
                      <a-button size="small" type="link" @click.stop="handleEdit(opportunity)">编辑</a-button>
                      <a-button size="small" type="link" @click.stop="handleMove(opportunity)">移动</a-button>
                    </div>
                  </div>
                </div>
                <div v-if="getStageOpportunities(stage.key).length === 0" class="empty-stage">
                  暂无商机
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>
        <a-tab-pane key="list" tab="列表视图">
          <a-table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a @click="handleView(record)">{{ record.name }}</a>
              </template>
              <template v-if="column.key === 'stage'">
                <a-tag :color="getStageColor(record.stage)">{{ record.stageLabel }}</a-tag>
              </template>
              <template v-if="column.key === 'expectedAmount'">
                <span class="amount">¥{{ formatAmount(record.expectedAmount) }}</span>
              </template>
              <template v-if="column.key === 'winProbability'">
                <a-progress :percent="record.winProbability" :show-info="true" size="small" />
              </template>
              <template v-if="column.key === 'priority'">
                <a-tag :color="getPriorityColor(record.priority)">{{ getPriorityText(record.priority) }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a @click="handleView(record)">查看</a>
                  <a @click="handleEdit(record)">编辑</a>
                  <a @click="handleMove(record)">移动阶段</a>
                  <a @click="handleConvert(record)" v-if="record.stage === 'closing'">转订单</a>
                  <a @click="handleCloseConfirm(record)" class="danger-link">关闭</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="statistics" tab="统计分析">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-statistic title="商机总数" :value="statistics.total" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="预计金额" :value="statistics.totalAmount" :precision="2" prefix="¥" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="成交金额" :value="statistics.wonAmount" :precision="2" prefix="¥" :value-style="{ color: '#3f8600' }" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="成交率" :value="statistics.winRate" suffix="%" />
            </a-col>
          </a-row>
          <a-divider>阶段分布</a-divider>
          <div ref="stageChartRef" class="chart-container"></div>
          <a-divider>金额趋势</a-divider>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="商机名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入商机名称" />
        </a-form-item>
        <a-form-item label="客户名称" name="customerId">
          <a-select
            v-model:value="formData.customerId"
            placeholder="请选择客户"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="customer in customerList" :key="customer.id" :value="customer.id">
              {{ customer.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="商机阶段" name="stage">
          <a-select v-model:value="formData.stage" placeholder="请选择商机阶段">
            <a-select-option value="lead">线索</a-select-option>
            <a-select-option value="qualification">资格确认</a-select-option>
            <a-select-option value="proposal">方案报价</a-select-option>
            <a-select-option value="negotiation">商务谈判</a-select-option>
            <a-select-option value="closing">成交阶段</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计金额" name="expectedAmount">
          <a-input-number v-model:value="formData.expectedAmount" :min="0" :precision="2" style="width: 100%" />
        </a-form-item>
        <a-form-item label="成交概率" name="winProbability">
          <a-slider v-model:value="formData.winProbability" :min="0" :max="100" :marks="{ 0: '0%', 25: '25%', 50: '50%', 75: '75%', 100: '100%' }" />
        </a-form-item>
        <a-form-item label="优先级" name="priority">
          <a-select v-model:value="formData.priority" placeholder="请选择优先级">
            <a-select-option value="high">高</a-select-option>
            <a-select-option value="medium">中</a-select-option>
            <a-select-option value="low">低</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="负责人" name="ownerId">
          <a-select
            v-model:value="formData.ownerId"
            placeholder="请选择负责人"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
              {{ user.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计成交日期" name="expectedCloseDate">
          <a-date-picker v-model:value="formData.expectedCloseDate" style="width: 100%" />
        </a-form-item>
        <a-form-item label="商机来源" name="source">
          <a-select v-model:value="formData.source" placeholder="请选择商机来源">
            <a-select-option value="website">网站</a-select-option>
            <a-select-option value="referral">客户转介</a-select-option>
            <a-select-option value="marketing">营销活动</a-select-option>
            <a-select-option value="cold_call">电话营销</a-select-option>
            <a-select-option value="exhibition">展会</a-select-option>
            <a-select-option value="other">其他</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="detailVisible"
      title="商机详情"
      width="900px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="商机名称">{{ opportunityDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="商机阶段">
          <a-tag :color="getStageColor(opportunityDetail.stage)">{{ opportunityDetail.stageLabel }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ opportunityDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="负责人">{{ opportunityDetail.ownerName }}</a-descriptions-item>
        <a-descriptions-item label="预计金额">¥{{ formatAmount(opportunityDetail.expectedAmount) }}</a-descriptions-item>
        <a-descriptions-item label="成交概率">
          <a-progress :percent="opportunityDetail.winProbability" size="small" />
        </a-descriptions-item>
        <a-descriptions-item label="优先级">
          <a-tag :color="getPriorityColor(opportunityDetail.priority)">{{ getPriorityText(opportunityDetail.priority) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="商机来源">{{ opportunityDetail.sourceLabel }}</a-descriptions-item>
        <a-descriptions-item label="预计成交日期">{{ opportunityDetail.expectedCloseDate }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ opportunityDetail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ opportunityDetail.remark }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>商机跟进记录</a-divider>
      <a-timeline>
        <a-timeline-item v-for="record in opportunityDetail.followRecords" :key="record.id" :color="record.type === 'stage_change' ? 'blue' : 'green'">
          <div class="timeline-content">
            <div class="timeline-title">{{ record.title }}</div>
            <div class="timeline-desc">{{ record.content }}</div>
            <div class="timeline-time">{{ record.createTime }} - {{ record.userName }}</div>
          </div>
        </a-timeline-item>
      </a-timeline>

      <a-divider>关联报价单</a-divider>
      <a-table
        :columns="quotationColumns"
        :data-source="opportunityDetail.quotations"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount'">
            <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getQuotationStatusColor(record.status)">{{ record.statusLabel }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>

    <a-modal
      v-model:open="moveVisible"
      title="移动商机阶段"
      width="500px"
      @ok="handleMoveConfirm"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="当前阶段">
          <a-tag :color="getStageColor(moveData.currentStage)">{{ getStageText(moveData.currentStage) }}</a-tag>
        </a-form-item>
        <a-form-item label="目标阶段">
          <a-select v-model:value="moveData.targetStage" placeholder="请选择目标阶段">
            <a-select-option v-for="stage in pipelineStages" :key="stage.key" :value="stage.key">
              {{ stage.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="移动原因">
          <a-textarea v-model:value="moveData.reason" placeholder="请输入移动原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import * as echarts from 'echarts'

const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const moveVisible = ref(false)
const modalTitle = ref('新建商机')
const activeTab = ref('pipeline')
const formRef = ref<FormInstance>()

const searchForm = reactive({
  name: '',
  customerName: '',
  stage: undefined,
  minAmount: undefined,
  maxAmount: undefined,
  ownerId: undefined
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const pipelineStages = [
  { key: 'lead', name: '线索', color: '#969799' },
  { key: 'qualification', name: '资格确认', color: '#faad14' },
  { key: 'proposal', name: '方案报价', color: '#1890ff' },
  { key: 'negotiation', name: '商务谈判', color: '#722ed1' },
  { key: 'closing', name: '成交阶段', color: '#13c2c2' },
  { key: 'won', name: '已成交', color: '#52c41a' },
  { key: 'lost', name: '已流失', color: '#f5222d' }
]

const columns = [
  { title: '商机名称', key: 'name', dataIndex: 'name', width: 180 },
  { title: '客户名称', dataIndex: 'customerName', width: 150 },
  { title: '阶段', key: 'stage', dataIndex: 'stage', width: 100 },
  { title: '预计金额', key: 'expectedAmount', dataIndex: 'expectedAmount', width: 120 },
  { title: '成交概率', key: 'winProbability', dataIndex: 'winProbability', width: 120 },
  { title: '优先级', key: 'priority', dataIndex: 'priority', width: 80 },
  { title: '负责人', dataIndex: 'ownerName', width: 100 },
  { title: '预计成交日期', dataIndex: 'expectedCloseDate', width: 120 },
  { title: '操作', key: 'action', fixed: 'right', width: 200 }
]

const quotationColumns = [
  { title: '报价单号', dataIndex: 'quotationNo', width: 150 },
  { title: '报价金额', key: 'totalAmount', dataIndex: 'totalAmount', width: 120 },
  { title: '报价日期', dataIndex: 'quotationDate', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 }
]

const tableData = ref<any[]>([])

const formData = reactive({
  id: undefined,
  name: '',
  customerId: undefined,
  stage: 'lead',
  expectedAmount: undefined,
  winProbability: 20,
  priority: 'medium',
  ownerId: undefined,
  expectedCloseDate: undefined,
  source: undefined,
  remark: ''
})

const formRules = {
  name: [{ required: true, message: '请输入商机名称' }],
  customerId: [{ required: true, message: '请选择客户' }],
  stage: [{ required: true, message: '请选择商机阶段' }],
  expectedAmount: [{ required: true, message: '请输入预计金额' }],
  ownerId: [{ required: true, message: '请选择负责人' }]
}

const moveData = reactive({
  opportunityId: undefined,
  currentStage: '',
  targetStage: undefined,
  reason: ''
})

const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' }
])

const userList = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' },
  { id: 3, name: '王五' }
])

const opportunityDetail = ref<any>({})
const statistics = ref({
  total: 25,
  totalAmount: 1580000,
  wonAmount: 520000,
  winRate: 32
})

const stageChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let stageChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

onMounted(() => {
  loadTableData()
  initCharts()
})

onUnmounted(() => {
  stageChart?.dispose()
  trendChart?.dispose()
})

const loadTableData = () => {
  loading.value = true
  tableData.value = [
    { id: 1, name: '办公用品采购商机', customerName: '北京科技有限公司', stage: 'negotiation', stageLabel: '商务谈判', expectedAmount: 58000, winProbability: 75, priority: 'high', priorityLabel: '高', ownerName: '张三', expectedCloseDate: '2024-02-15', createTime: '2024-01-10' },
    { id: 2, name: 'IT设备升级商机', customerName: '上海贸易公司', stage: 'proposal', stageLabel: '方案报价', expectedAmount: 128000, winProbability: 60, priority: 'medium', priorityLabel: '中', ownerName: '李四', expectedCloseDate: '2024-03-01', createTime: '2024-01-12' },
    { id: 3, name: '办公家具采购商机', customerName: '广州制造企业', stage: 'qualification', stageLabel: '资格确认', expectedAmount: 256000, winProbability: 40, priority: 'high', priorityLabel: '高', ownerName: '王五', expectedCloseDate: '2024-04-15', createTime: '2024-01-15' },
    { id: 4, name: '软件定制开发商机', customerName: '深圳电子公司', stage: 'lead', stageLabel: '线索', expectedAmount: 85000, winProbability: 20, priority: 'low', priorityLabel: '低', ownerName: '张三', expectedCloseDate: '2024-05-30', createTime: '2024-01-18' },
    { id: 5, name: '年度服务合同商机', customerName: '杭州互联网公司', stage: 'closing', stageLabel: '成交阶段', expectedAmount: 180000, winProbability: 90, priority: 'high', priorityLabel: '高', ownerName: '李四', expectedCloseDate: '2024-01-25', createTime: '2024-01-08' }
  ]
  pagination.total = 5
  loading.value = false
}

const getStageOpportunities = (stage: string) => {
  return tableData.value.filter(o => o.stage === stage)
}

const getStageCount = (stage: string) => {
  return getStageOpportunities(stage).length
}

const getStageAmount = (stage: string) => {
  return getStageOpportunities(stage).reduce((sum, o) => sum + (o.expectedAmount || 0), 0)
}

const getStageColor = (stage: string) => {
  const colors: Record<string, string> = {
    lead: 'default',
    qualification: 'orange',
    proposal: 'blue',
    negotiation: 'purple',
    closing: 'cyan',
    won: 'green',
    lost: 'red'
  }
  return colors[stage] || 'default'
}

const getStageText = (stage: string) => {
  const texts: Record<string, string> = {
    lead: '线索',
    qualification: '资格确认',
    proposal: '方案报价',
    negotiation: '商务谈判',
    closing: '成交阶段',
    won: '已成交',
    lost: '已流失'
  }
  return texts[stage] || stage
}

const getPriorityColor = (priority: string) => {
  const colors: Record<string, string> = {
    high: 'red',
    medium: 'orange',
    low: 'default'
  }
  return colors[priority] || 'default'
}

const getPriorityText = (priority: string) => {
  const texts: Record<string, string> = {
    high: '高',
    medium: '中',
    low: '低'
  }
  return texts[priority] || priority
}

const getQuotationStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    draft: 'default',
    sent: 'blue',
    accepted: 'green',
    rejected: 'red'
  }
  return colors[status] || 'default'
}

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const filterOption = (input: string, option: any) => {
  return option.name.toLowerCase().indexOf(input.toLowerCase()) >= 0
}

const initCharts = () => {
  initStageChart()
  initTrendChart()
}

const initStageChart = () => {
  if (!stageChartRef.value) return
  stageChart = echarts.init(stageChartRef.value)
  const option = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '商机阶段',
      type: 'pie',
      radius: ['40%', '70%'],
      data: pipelineStages.map(stage => ({
        name: stage.name,
        value: getStageCount(stage.key),
        itemStyle: { color: stage.color }
      }))
    }]
  }
  stageChart.setOption(option)
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['预计金额', '成交金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [
      { name: '预计金额', type: 'line', data: [280000, 320000, 380000, 420000, 480000, 520000] },
      { name: '成交金额', type: 'line', data: [120000, 150000, 180000, 200000, 220000, 250000] }
    ]
  }
  trendChart.setOption(option)
}

const handleSearch = () => {
  pagination.current = 1
  loadTableData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    name: '',
    customerName: '',
    stage: undefined,
    minAmount: undefined,
    maxAmount: undefined,
    ownerId: undefined
  })
  handleSearch()
}

const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  loadTableData()
}

const handleAdd = () => {
  modalTitle.value = '新建商机'
  Object.assign(formData, {
    id: undefined,
    name: '',
    customerId: undefined,
    stage: 'lead',
    expectedAmount: undefined,
    winProbability: 20,
    priority: 'medium',
    ownerId: undefined,
    expectedCloseDate: undefined,
    source: undefined,
    remark: ''
  })
  modalVisible.value = true
}

const handleView = (record: any) => {
  opportunityDetail.value = {
    ...record,
    sourceLabel: '客户转介',
    followRecords: [
      { id: 1, title: '创建商机', content: '从线索池创建商机', createTime: record.createTime, userName: record.ownerName, type: 'create' },
      { id: 2, title: '阶段变更', content: `商机从"线索"移动到"${record.stageLabel}"`, createTime: '2024-01-12 10:30', userName: record.ownerName, type: 'stage_change' },
      { id: 3, title: '跟进记录', content: '与客户进行了初步沟通，客户对产品表示感兴趣', createTime: '2024-01-14 14:20', userName: record.ownerName, type: 'follow' }
    ],
    quotations: [
      { quotationNo: 'QT20240115001', totalAmount: 55000, quotationDate: '2024-01-15', status: 'sent', statusLabel: '已发送' }
    ]
  }
  detailVisible.value = true
}

const handleEdit = (record: any) => {
  modalTitle.value = '编辑商机'
  Object.assign(formData, record)
  modalVisible.value = true
}

const handleMove = (record: any) => {
  moveData.opportunityId = record.id
  moveData.currentStage = record.stage
  moveData.targetStage = undefined
  moveData.reason = ''
  moveVisible.value = true
}

const handleMoveConfirm = () => {
  if (!moveData.targetStage) {
    message.warning('请选择目标阶段')
    return
  }
  message.success('商机阶段已更新')
  moveVisible.value = false
  loadTableData()
}

const handleConvert = (record: any) => {
  Modal.confirm({
    title: '确认转订单',
    content: `确定要将商机 "${record.name}" 转为订单吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success('商机已成功转为订单')
      loadTableData()
    }
  })
}

const handleCloseConfirm = (record: any) => {
  Modal.confirm({
    title: '确认关闭',
    content: `确定要关闭商机 "${record.name}" 吗？此操作不可撤销。`,
    okText: '确认关闭',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success('商机已关闭')
      loadTableData()
    }
  })
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    message.success('保存成功')
    modalVisible.value = false
    loadTableData()
  } catch (error) {
    console.error('Validation failed:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleModalCancel = () => {
  formRef.value?.resetFields()
  modalVisible.value = false
}
</script>

<style scoped lang="scss">
.opportunity-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.search-form {
  margin-bottom: 16px;
}

.pipeline-container {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 16px 0;
}

.pipeline-stage {
  min-width: 280px;
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;

  .stage-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 12px;
    border-bottom: 1px solid #e8e8e8;

    .stage-name {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .stage-count {
      font-size: 12px;
      color: #666;
      margin-left: 8px;
    }

    .stage-amount {
      font-size: 12px;
      color: #1890ff;
      margin-left: 8px;
    }
  }

  .stage-cards {
    max-height: 400px;
    overflow-y: auto;
  }

  .opportunity-card {
    background: #fff;
    border-radius: 6px;
    padding: 12px;
    margin-top: 12px;
    cursor: pointer;
    border: 1px solid #e8e8e8;

    &:hover {
      border-color: #1890ff;
      box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .card-title {
        font-size: 14px;
        font-weight: 500;
        color: #333;
      }
    }

    .card-body {
      .card-row {
        display: flex;
        margin-top: 8px;

        .label {
          width: 50px;
          color: #999;
          font-size: 12px;
        }

        .value {
          color: #333;
          font-size: 12px;

          &.amount {
            color: #f5222d;
            font-weight: 500;
          }
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 12px;
      padding-top: 8px;
      border-top: 1px solid #f0f0f0;

      .close-date {
        font-size: 12px;
        color: #999;
      }

      .card-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .empty-stage {
    text-align: center;
    padding: 40px 20px;
    color: #999;
    font-size: 12px;
  }
}

.amount {
  color: #f5222d;
  font-weight: 500;
}

.danger-link {
  color: #f5222d;
}

.chart-container {
  height: 300px;
}

.timeline-content {
  .timeline-title {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }

  .timeline-desc {
    font-size: 12px;
    color: #666;
    margin-top: 4px;
  }

  .timeline-time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}
</style>