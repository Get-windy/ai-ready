<template>
  <PageContainer full-height>
    <template #header>
      <div class="opportunity-page-header">
        <div class="opportunity-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>商机管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="opportunity-page-header-title">商机管理</h2>
        </div>
        <div class="opportunity-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-radio-group v-model:value="activeTab" button-style="solid" size="small">
            <a-radio-button value="pipeline"><AppstoreOutlined /> 管道</a-radio-button>
            <a-radio-button value="list"><UnorderedListOutlined /> 列表</a-radio-button>
            <a-radio-button value="statistics"><BarChartOutlined /> 统计</a-radio-button>
          </a-radio-group>
          <a-button size="small" :loading="refreshLoading" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 统计卡片 -->
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <FundOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">商机总数</div>
                <div class="stat-value">{{ statistics.total }}</div>
                <div class="stat-desc">全部商机</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <DollarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">预计金额</div>
                <div class="stat-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
                <div class="stat-desc">预估成交总额</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <CheckCircleOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">成交金额</div>
                <div class="stat-value">¥{{ formatAmount(statistics.wonAmount) }}</div>
                <div class="stat-desc positive">已实际成交</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <PercentageOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">成交率</div>
                <div class="stat-value">{{ statistics.winRate }}%</div>
                <div class="stat-desc">整体转化率</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 筛选区 -->
      <a-collapse v-model:activeKey="filterExpanded" class="filter-collapse">
        <a-collapse-panel key="1" header="筛选条件">
          <a-row :gutter="16">
            <a-col :span="4">
              <a-form-item label="商机名称">
                <a-input v-model:value="searchForm.name" placeholder="请输入" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="客户名称">
                <a-input v-model:value="searchForm.customerName" placeholder="请输入" allow-clear />
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="商机阶段">
                <a-select v-model:value="searchForm.stage" placeholder="全部阶段" allow-clear style="width: 100%">
                  <a-select-option v-for="s in pipelineStages" :key="s.key" :value="s.key">{{ s.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="负责人">
                <a-select v-model:value="searchForm.ownerId" placeholder="全部负责人" allow-clear show-search :filter-option="filterOption" style="width: 100%">
                  <a-select-option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="预计金额">
                <a-space>
                  <a-input-number v-model:value="searchForm.minAmount" placeholder="最小" style="width: 90px" />
                  <span>-</span>
                  <a-input-number v-model:value="searchForm.maxAmount" placeholder="最大" style="width: 90px" />
                </a-space>
              </a-form-item>
            </a-col>
            <a-col :span="4" class="filter-actions">
              <a-space>
                <a-button type="primary" :loading="loading" @click="handleSearch">查询</a-button>
                <a-button @click="handleReset">重置</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>

      <!-- 管道视图 -->
      <template v-if="activeTab === 'pipeline'">
        <div class="pipeline-container">
          <div class="pipeline-stage" v-for="stage in pipelineStages.filter(s => s.key !== 'won' && s.key !== 'lost')" :key="stage.key">
            <div class="stage-header" :style="{ borderBottomColor: stage.color }">
              <a-space>
                <span class="stage-name">{{ stage.name }}</span>
                <a-badge :count="getStageCount(stage.key)" :overflow-count="99" :number-style="{ backgroundColor: stage.color }" />
              </a-space>
              <span class="stage-amount">¥{{ formatAmount(getStageAmount(stage.key)) }}</span>
            </div>
            <div class="stage-cards">
              <div
                v-for="opp in getStageOpportunities(stage.key)"
                :key="opp.id"
                class="opportunity-card"
                @click="handleView(opp)"
              >
                <div class="card-header">
                  <span class="card-title">{{ opp.name }}</span>
                  <a-tag :color="getPriorityColor(opp.priority)" size="small">{{ getPriorityText(opp.priority) }}</a-tag>
                </div>
                <div class="card-body">
                  <div class="card-row">
                    <span class="label"><UserOutlined /> 客户:</span>
                    <span class="value">{{ opp.customerName }}</span>
                  </div>
                  <div class="card-row">
                    <span class="label"><DollarOutlined /> 金额:</span>
                    <span class="value amount">¥{{ formatAmount(opp.expectedAmount) }}</span>
                  </div>
                  <div class="card-row">
                    <span class="label"><TeamOutlined /> 负责人:</span>
                    <span class="value">{{ opp.ownerName }}</span>
                  </div>
                </div>
                <div class="card-footer">
                  <span class="close-date"><CalendarOutlined /> {{ opp.expectedCloseDate }}</span>
                  <div class="card-actions">
                    <a-button size="small" type="link" @click.stop="handleEdit(opp)">编辑</a-button>
                    <a-button size="small" type="link" @click.stop="handleMove(opp)">移动</a-button>
                  </div>
                </div>
              </div>
              <div v-if="getStageOpportunities(stage.key).length === 0" class="empty-stage">
                <InboxOutlined class="empty-icon" />
                <span>暂无商机</span>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 列表视图 -->
      <template v-if="activeTab === 'list'">
        <VxeTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableDataSource"
          :loading="loading"
          :pagination="pagination"
          :filter-fields="filterFields"
          :show-export="true"
          :selectable="true"
          add-text="新建商机"
          @add="handleAdd"
          @refresh="fetchData"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="handleSelectionChange"
          @export="handleExport"
        >
          <template #empty>
            <div class="table-empty">
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的商机，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无商机数据，点击右上角「新建商机」开始创建
              </p>
            </div>
          </template>

          <template #action="{ record }">
            <a-space :size="4">
              <a-tooltip title="查看详情">
                <a-button type="link" size="small" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="编辑">
                <a-button type="link" size="small" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="移动阶段">
                <a-button type="link" size="small" @click="handleMove(record)">
                  <template #icon><SwapRightOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.stage === 'closing'" title="转订单">
                <a-button type="link" size="small" @click="handleConvert(record)">
                  <template #icon><FileProtectOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><MoreOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                    <a-menu-item key="follow"><MessageOutlined /> 添加跟进</a-menu-item>
                    <a-menu-item key="quotation"><FileTextOutlined /> 创建报价</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="close" danger><StopOutlined /> 关闭商机</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </VxeTableList>
      </template>

      <!-- 统计分析 -->
      <template v-if="activeTab === 'statistics'">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-card title="阶段分布" size="small" :loading="chartLoading">
              <div ref="stageChartRef" class="chart-container"></div>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="金额趋势" size="small" :loading="chartLoading">
              <div ref="trendChartRef" class="chart-container"></div>
            </a-card>
          </a-col>
        </a-row>
        <a-card title="阶段详情统计" size="small" style="margin-top: 16px">
          <VxeTableList :columns="stageStatsVxeColumns" :data-source="stageStatsData" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false">
            <template #amountCell="{ record }">
              <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
            </template>
            <template #avgAmountCell="{ record }">
              <span class="amount-cell">¥{{ formatAmount(record.avgAmount) }}</span>
            </template>
          </VxeTableList>
        </a-card>
      </template>
    </ErrorBoundary>

    <!-- 商机表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="800px"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="商机名称" name="name">
              <a-input v-model:value="formData.name" placeholder="请输入商机名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="客户名称" name="customerId">
              <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
                <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="商机阶段" name="stage">
              <a-select v-model:value="formData.stage" placeholder="请选择商机阶段">
                <a-select-option value="lead">线索</a-select-option>
                <a-select-option value="qualification">资格确认</a-select-option>
                <a-select-option value="proposal">方案报价</a-select-option>
                <a-select-option value="negotiation">商务谈判</a-select-option>
                <a-select-option value="closing">成交阶段</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计金额" name="expectedAmount">
              <a-input-number v-model:value="formData.expectedAmount" :min="0" :precision="2" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="成交概率" name="winProbability" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-slider v-model:value="formData.winProbability" :min="0" :max="100" :marks="{ 0: '0%', 25: '25%', 50: '50%', 75: '75%', 100: '100%' }" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优先级" name="priority">
              <a-select v-model:value="formData.priority" placeholder="请选择优先级">
                <a-select-option value="high">高</a-select-option>
                <a-select-option value="medium">中</a-select-option>
                <a-select-option value="low">低</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="负责人" name="ownerId">
              <a-select v-model:value="formData.ownerId" placeholder="请选择负责人" show-search :filter-option="filterOption">
                <a-select-option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计成交日期" name="expectedCloseDate">
              <a-date-picker v-model:value="formData.expectedCloseDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
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
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
              <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 详情弹窗 -->
    <a-drawer v-model:open="detailVisible" title="商机详情" placement="right" width="80vw" :footer="null">
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="商机名称">
          <span class="opp-name">{{ opportunityDetail.name }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="商机阶段">
          <a-tag :color="getStageColor(opportunityDetail.stage)">{{ opportunityDetail.stageLabel }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ opportunityDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="负责人">{{ opportunityDetail.ownerName }}</a-descriptions-item>
        <a-descriptions-item label="预计金额">
          <span class="amount-cell">¥{{ formatAmount(opportunityDetail.expectedAmount) }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="成交概率">
          <a-progress :percent="opportunityDetail.winProbability" size="small" />
        </a-descriptions-item>
        <a-descriptions-item label="优先级">
          <a-tag :color="getPriorityColor(opportunityDetail.priority)">{{ getPriorityText(opportunityDetail.priority) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="商机来源">{{ opportunityDetail.sourceLabel }}</a-descriptions-item>
        <a-descriptions-item label="预计成交日期">{{ opportunityDetail.expectedCloseDate }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ opportunityDetail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ opportunityDetail.remark || '无' }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>商机跟进记录</a-divider>
      <a-timeline>
        <a-timeline-item v-for="r in opportunityDetail.followRecords" :key="r.id" :color="r.type === 'stage_change' ? 'blue' : 'green'">
          <div class="timeline-content">
            <div class="timeline-title">{{ r.title }}</div>
            <div class="timeline-desc">{{ r.content }}</div>
            <div class="timeline-time">{{ r.createTime }} - {{ r.userName }}</div>
          </div>
        </a-timeline-item>
      </a-timeline>

      <a-divider>关联报价单</a-divider>
      <VxeTableList :columns="quotationVxeColumns" :data-source="opportunityDetail.quotations" :pagination="false" :show-toolbar="false" :selectable="false" :show-add="false" :show-search="false" :show-export="false" :show-batch-delete="false">
        <template #totalAmountCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="getQuotationStatusColor(record.status)">{{ record.statusLabel }}</a-tag>
        </template>
      </VxeTableList>
    </a-drawer>

    <!-- 移动阶段弹窗 -->
    <a-modal v-model:open="moveVisible" title="移动商机阶段" width="500px" :confirm-loading="moveLoading" @ok="handleMoveConfirm">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="当前阶段">
          <a-tag :color="getStageColor(moveData.currentStage)">{{ getStageText(moveData.currentStage) }}</a-tag>
        </a-form-item>
        <a-form-item label="目标阶段">
          <a-select v-model:value="moveData.targetStage" placeholder="请选择目标阶段">
            <a-select-option v-for="s in pipelineStages" :key="s.key" :value="s.key">{{ s.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="移动原因">
          <a-textarea v-model:value="moveData.reason" placeholder="请输入移动原因" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { PageContainer } from '@/components'
import * as echarts from 'echarts'
import { opportunityApi } from '@/api/crm'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  SwapRightOutlined,
  FileProtectOutlined,
  StopOutlined,
  ReloadOutlined,
  SearchOutlined,
  InboxOutlined,
  MoreOutlined,
  MessageOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  BarChartOutlined,
  FundOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  UserOutlined,
  CalendarOutlined,
  SyncOutlined
} from '@ant-design/icons-vue'

// 使用 FundOutlined 代替 PercentageOutlined
const PercentageOutlined = FundOutlined

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const chartLoading = ref(false)
const submitLoading = ref(false)
const moveLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const moveVisible = ref(false)
const modalTitle = ref('新建商机')
const activeTab = ref('pipeline')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdateTime = ref<string>('')
const filterExpanded = ref<string[]>([])
const selectedRowKeys = ref<number[]>([])
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  name: '',
  customerName: '',
  stage: undefined,
  minAmount: undefined,
  maxAmount: undefined,
  ownerId: undefined
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

const vxeColumns = computed(() => [
  { field: 'name', title: '商机名称', width: 180, formatter: ({ row }: any) => row.name || '' },
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'stage', title: '阶段', width: 100, align: 'center', formatter: ({ row }: any) => row.stageLabel || getStageText(row.stage) },
  { field: 'expectedAmount', title: '预计金额', width: 120, align: 'right', formatter: ({ cellValue }: any) => `¥${formatAmount(cellValue)}` },
  { field: 'winProbability', title: '成交概率', width: 130, formatter: ({ cellValue }: any) => `${cellValue || 0}%` },
  { field: 'priority', title: '优先级', width: 80, align: 'center', formatter: ({ cellValue }: any) => getPriorityText(cellValue) },
  { field: 'ownerName', title: '负责人', width: 100 },
  { field: 'expectedCloseDate', title: '预计成交日期', width: 120 },
  { field: 'action', title: '操作', width: 180, fixed: 'right', type: 'action' }
])

const filterFields = [
  { key: 'name', label: '商机名称', type: 'input' as const, placeholder: '输入商机名称' },
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '输入客户名称' },
  { key: 'stage', label: '商机阶段', type: 'select' as const, options: pipelineStages.map(s => ({ label: s.name, value: s.key })) }
]

const quotationVxeColumns = [
  { field: 'quotationNo', title: '报价单号', width: 150 },
  { field: 'totalAmount', title: '报价金额', width: 120, align: 'right', slotName: 'totalAmountCell' },
  { field: 'quotationDate', title: '报价日期', width: 100 },
  { field: 'status', title: '状态', width: 100, align: 'center', slotName: 'statusCell' }
]

const stageStatsVxeColumns = [
  { field: 'name', title: '阶段', width: 120 },
  { field: 'count', title: '商机数', width: 80, align: 'right' },
  { field: 'amount', title: '金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'avgAmount', title: '平均金额', width: 120, align: 'right', slotName: 'avgAmountCell' },
  { field: 'percent', title: '占比', width: 80, align: 'right' }
]

const stageColorMap: Record<string, string> = {
  lead: 'default', qualification: 'orange', proposal: 'blue', negotiation: 'purple', closing: 'cyan', won: 'green', lost: 'red'
}
const stageTextMap: Record<string, string> = {
  lead: '线索', qualification: '资格确认', proposal: '方案报价', negotiation: '商务谈判', closing: '成交阶段', won: '已成交', lost: '已流失'
}
const priorityColorMap: Record<string, string> = { high: 'red', medium: 'orange', low: 'default' }
const priorityTextMap: Record<string, string> = { high: '高', medium: '中', low: '低' }

function getStageColor(stage: string): string { return stageColorMap[stage] || 'default' }
function getStageText(stage: string): string { return stageTextMap[stage] || stage }
function getPriorityColor(p: string): string { return priorityColorMap[p] || 'default' }
function getPriorityText(p: string): string { return priorityTextMap[p] || p }
function getQuotationStatusColor(status: string): string {
  const m: Record<string, string> = { draft: 'default', sent: 'blue', accepted: 'green', rejected: 'red' }
  return m[status] || 'default'
}
function getProbabilityColor(prob: number): string {
  if (prob >= 80) return '#52c41a'
  if (prob >= 60) return '#1890ff'
  if (prob >= 40) return '#faad14'
  return '#ff4d4f'
}
function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

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
const moveData = reactive({ opportunityId: undefined, currentStage: '', targetStage: undefined, reason: '' })
const customerList = ref([
  { id: 1, name: '北京科技有限公司' },
  { id: 2, name: '上海贸易公司' },
  { id: 3, name: '广州制造企业' },
  { id: 4, name: '深圳电子公司' },
  { id: 5, name: '杭州互联网公司' }
])
const userList = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' },
  { id: 3, name: '王五' }
])
const opportunityDetail = ref<any>({})
const statistics = ref({ total: 0, totalAmount: 0, wonAmount: 0, winRate: 0 })

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// 数据源
const tableDataSource = tableData

// 阶段统计数据
const stageStatsData = computed(() => {
  return pipelineStages.map(s => {
    const count = getStageCount(s.key)
    const amount = getStageAmount(s.key)
    const avgAmount = count > 0 ? amount / count : 0
    const total = statistics.value.totalAmount
    const percent = total > 0 ? Math.round(amount / total * 100) : 0
    return { name: s.name, count, amount, avgAmount, percent: `${percent}%` }
  })
})

const stageChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let stageChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

function getStageOpportunities(stage: string) { return tableData.value.filter(o => o.stage === stage) }
function getStageCount(stage: string) { return getStageOpportunities(stage).length }
function getStageAmount(stage: string) { return getStageOpportunities(stage).reduce((s, o) => s + (o.expectedAmount || 0), 0) }

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  stageChart?.dispose()
  trendChart?.dispose()
})

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = {
      keyword: searchForm.name || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.stage) params.opportunityStage = pipelineStages.findIndex((s: any) => s.key === searchForm.stage)
    const res = await opportunityApi.page(params)
    const result = res as any
    tableData.value = result.records || result.data?.records || []
    pagination.total = result.total ?? result.data?.total ?? 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    if (activeTab.value === 'statistics') {
      initCharts()
    }
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('获取商机数据失败')
    }
    console.warn('[CRM商机] 获取商机列表失败', err)
    tableData.value = []
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

const handleRefresh = () => {
  lastUpdateTime.value = ''
  fetchData()
}

function initCharts() {
  nextTick(() => {
    initStageChart()
    initTrendChart()
  })
}

function initStageChart() {
  if (!stageChartRef.value) return
  if (stageChart) stageChart.dispose()
  stageChart = echarts.init(stageChartRef.value)
  stageChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      name: '商机阶段',
      type: 'pie',
      radius: ['40%', '70%'],
      data: pipelineStages.filter(s => getStageCount(s.key) > 0).map(s => ({
        name: s.name,
        value: getStageCount(s.key),
        itemStyle: { color: s.color }
      }))
    }]
  })
}

function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['预计金额', '成交金额'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value', name: '金额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
    series: [
      { name: '预计金额', type: 'line', data: [], smooth: true, itemStyle: { color: '#1890ff' } },
      { name: '成交金额', type: 'line', data: [], smooth: true, itemStyle: { color: '#52c41a' } }
    ]
  })
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  Object.assign(searchForm, { name: '', customerName: '', stage: undefined, minAmount: undefined, maxAmount: undefined, ownerId: undefined })
  handleSearch()
}
function handleResetFilters() { handleReset() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }
function handleSelectionChange(rows: any[], ids: any[]) { selectedRowKeys.value = ids }

function handleAdd() {
  modalTitle.value = '新建商机'
  Object.assign(formData, { id: undefined, name: '', customerId: undefined, stage: 'lead', expectedAmount: undefined, winProbability: 20, priority: 'medium', ownerId: undefined, expectedCloseDate: undefined, source: undefined, remark: '' })
  modalVisible.value = true
}

function handleView(record: any) {
  opportunityDetail.value = {
    ...record,
    sourceLabel: '客户转介',
    followRecords: [
      { id: 1, title: '创建商机', content: '从线索池创建商机', createTime: record.createTime, userName: record.ownerName, type: 'create' },
      { id: 2, title: '阶段变更', content: `商机从"线索"移动到"${record.stageLabel}"`, createTime: '2024-01-12 10:30', userName: record.ownerName, type: 'stage_change' },
      { id: 3, title: '跟进记录', content: '与客户进行了初步沟通', createTime: '2024-01-14 14:20', userName: record.ownerName, type: 'follow' }
    ],
    quotations: [{ quotationNo: 'QT20240115001', totalAmount: 55000, quotationDate: '2024-01-15', status: 'sent', statusLabel: '已发送' }]
  }
  detailVisible.value = true
}

function handleEdit(record: any) {
  modalTitle.value = '编辑商机'
  Object.assign(formData, record)
  modalVisible.value = true
}

function handleMove(record: any) {
  moveData.opportunityId = record.id
  moveData.currentStage = record.stage
  moveData.targetStage = undefined
  moveData.reason = ''
  moveVisible.value = true
}

function handleMoveConfirm() {
  if (!moveData.targetStage) { message.warning('请选择目标阶段'); return }
  moveLoading.value = true
  setTimeout(() => {
    message.success('商机阶段已更新')
    moveVisible.value = false
    moveLoading.value = false
    fetchData()
  }, 500)
}

function handleConvert(record: any) {
  Modal.confirm({
    title: '确认转订单',
    content: `确定要将商机 "${record.name}" 转为订单吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    onOk() { message.success('商机已成功转为订单'); fetchData() }
  })
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'follow':
      message.info(`添加跟进: ${record.name}`)
      break
    case 'quotation':
      message.info(`创建报价: ${record.name}`)
      break
    case 'close':
      Modal.confirm({
        title: '确认关闭',
        content: `确定要关闭商机 "${record.name}" 吗？`,
        okText: '确认关闭',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk() { message.success('商机已关闭'); fetchData() }
      })
      break
  }
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch (err) { console.warn('[CRM商机] 表单验证失败', err); return }
  submitLoading.value = true
  setTimeout(() => {
    message.success('保存成功')
    modalVisible.value = false
    submitLoading.value = false
    fetchData()
  }, 500)
}

function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }

function handleExport() {
  message.info('导出商机数据')
}
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.opportunity-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.opportunity-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.opportunity-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.opportunity-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.stats-cards {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card.stat-card-blue {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border: 1px solid #91d5ff;
}

.stat-card.stat-card-green {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
}

.stat-card.stat-card-orange {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.stat-card.stat-card-purple {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.stat-desc.positive {
  color: #52c41a;
}

.filter-collapse {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

/* 管道视图 */
.pipeline-container {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
}

.pipeline-stage {
  min-width: 280px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.stage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 2px solid;
  margin-bottom: 12px;
}

.stage-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.stage-amount {
  font-size: 12px;
  color: #1890ff;
  font-weight: 500;
}

.stage-cards {
  max-height: 450px;
  overflow-y: auto;
}

.opportunity-card {
  background: #fafafa;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  border: 1px solid #e8e8e8;
  transition: all 0.2s;
}

.opportunity-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
  background: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.card-body {
  margin-top: 8px;
}

.card-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
}

.card-row .label {
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-row .value {
  color: #606266;
}

.card-row .amount {
  color: #f5222d;
  font-weight: 500;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.close-date {
  font-size: 12px;
  color: #999;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.empty-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
  font-size: 12px;
}

.empty-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

/* 列表视图 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}


.opp-name {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

/* 统计视图 */
.chart-container {
  height: 300px;
}

.timeline-content .timeline-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.timeline-content .timeline-desc {
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}

.timeline-content .timeline-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
