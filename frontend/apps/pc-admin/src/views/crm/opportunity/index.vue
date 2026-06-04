<template>
  <div class="opportunity-management">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>商机管理</h2>
        <a-button type="primary" @click="handleAdd"><template #icon><PlusOutlined /></template>新建商机</a-button>
      </div>

      <a-form layout="inline" class="search-form">
        <a-form-item label="商机名称"><a-input v-model:value="searchForm.name" placeholder="请输入商机名称" allow-clear /></a-form-item>
        <a-form-item label="客户名称"><a-input v-model:value="searchForm.customerName" placeholder="请输入客户名称" allow-clear /></a-form-item>
        <a-form-item label="商机阶段">
          <a-select v-model:value="searchForm.stage" placeholder="请选择阶段" allow-clear style="width:150px">
            <a-select-option v-for="s in pipelineStages" :key="s.key" :value="s.key">{{ s.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计金额">
          <a-input-number v-model:value="searchForm.minAmount" placeholder="最小金额" style="width:120px" />
          <span style="margin:0 8px">-</span>
          <a-input-number v-model:value="searchForm.maxAmount" placeholder="最大金额" style="width:120px" />
        </a-form-item>
        <a-form-item label="负责人">
          <a-select v-model:value="searchForm.ownerId" placeholder="请选择负责人" allow-clear show-search :filter-option="filterOption" style="width:150px">
            <a-select-option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
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
                <div v-for="opp in getStageOpportunities(stage.key)" :key="opp.id" class="opportunity-card" @click="handleView(opp)">
                  <div class="card-header">
                    <span class="card-title">{{ opp.name }}</span>
                    <a-tag :color="getPriorityColor(opp.priority)">{{ getPriorityText(opp.priority) }}</a-tag>
                  </div>
                  <div class="card-body">
                    <div class="card-row"><span class="label">客户:</span><span class="value">{{ opp.customerName }}</span></div>
                    <div class="card-row"><span class="label">金额:</span><span class="value amount">¥{{ formatAmount(opp.expectedAmount) }}</span></div>
                    <div class="card-row"><span class="label">负责人:</span><span class="value">{{ opp.ownerName }}</span></div>
                  </div>
                  <div class="card-footer">
                    <span class="close-date">预计成交: {{ opp.expectedCloseDate }}</span>
                    <div class="card-actions">
                      <a-button size="small" type="link" @click.stop="handleEdit(opp)">编辑</a-button>
                      <a-button size="small" type="link" @click.stop="handleMove(opp)">移动</a-button>
                    </div>
                  </div>
                </div>
                <div v-if="getStageOpportunities(stage.key).length === 0" class="empty-stage">暂无商机</div>
              </div>
            </div>
          </div>
        </a-tab-pane>
        <a-tab-pane key="list" tab="列表视图">
          <TableList
            ref="tableRef"
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            :table-key="'crm-opportunity-list'"
            add-text="新建商机"
            @add="handleAdd"
            @refresh="fetchData"
            @search="handleSearch"
            @page-change="handlePageChange"
            @sort-change="handleSortChange"
            @filter-change="handleFilterChange"
          >
            <template #name="{ record }"><a @click="handleView(record)">{{ record.name }}</a></template>
            <template #stage="{ record }"><a-tag :color="getStageColor(record.stage)">{{ record.stageLabel }}</a-tag></template>
            <template #expectedAmount="{ record }"><span class="amount">¥{{ formatAmount(record.expectedAmount) }}</span></template>
            <template #winProbability="{ record }"><a-progress :percent="record.winProbability" :show-info="true" size="small" /></template>
            <template #priority="{ record }"><a-tag :color="getPriorityColor(record.priority)">{{ getPriorityText(record.priority) }}</a-tag></template>
            <template #action="{ record }">
              <a-space :size="4">
                <a-tooltip title="查看"><a-button type="link" size="small" @click="handleView(record)"><template #icon><EyeOutlined /></template></a-button></a-tooltip>
                <a-tooltip title="编辑"><a-button type="link" size="small" @click="handleEdit(record)"><template #icon><EditOutlined /></template></a-button></a-tooltip>
                <a-tooltip title="移动阶段"><a-button type="link" size="small" @click="handleMove(record)"><template #icon><SwapRightOutlined /></template></a-button></a-tooltip>
                <a-tooltip v-if="record.stage === 'closing'" title="转订单"><a-button type="link" size="small" @click="handleConvert(record)"><template #icon><FileProtectOutlined /></template></a-button></a-tooltip>
                <a-tooltip title="关闭"><a-button type="link" danger size="small" @click="handleCloseConfirm(record)"><template #icon><StopOutlined /></template></a-button></a-tooltip>
              </a-space>
            </template>
          </TableList>
        </a-tab-pane>
        <a-tab-pane key="statistics" tab="统计分析">
          <a-row :gutter="16">
            <a-col :span="6"><a-statistic title="商机总数" :value="statistics.total" /></a-col>
            <a-col :span="6"><a-statistic title="预计金额" :value="statistics.totalAmount" :precision="2" prefix="¥" /></a-col>
            <a-col :span="6"><a-statistic title="成交金额" :value="statistics.wonAmount" :precision="2" prefix="¥" :value-style="{ color: '#3f8600' }" /></a-col>
            <a-col :span="6"><a-statistic title="成交率" :value="statistics.winRate" suffix="%" /></a-col>
          </a-row>
          <a-divider>阶段分布</a-divider>
          <div ref="stageChartRef" class="chart-container"></div>
          <a-divider>金额趋势</a-divider>
          <div ref="trendChartRef" class="chart-container"></div>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <a-modal v-model:open="modalVisible" :title="modalTitle" width="800px" :confirm-loading="submitLoading" @ok="handleSubmit" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="商机名称" name="name"><a-input v-model:value="formData.name" placeholder="请输入商机名称" /></a-form-item>
        <a-form-item label="客户名称" name="customerId">
          <a-select v-model:value="formData.customerId" placeholder="请选择客户" show-search :filter-option="filterOption">
            <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
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
        <a-form-item label="预计金额" name="expectedAmount"><a-input-number v-model:value="formData.expectedAmount" :min="0" :precision="2" style="width:100%" /></a-form-item>
        <a-form-item label="成交概率" name="winProbability"><a-slider v-model:value="formData.winProbability" :min="0" :max="100" :marks="{ 0:'0%',25:'25%',50:'50%',75:'75%',100:'100%' }" /></a-form-item>
        <a-form-item label="优先级" name="priority">
          <a-select v-model:value="formData.priority" placeholder="请选择优先级">
            <a-select-option value="high">高</a-select-option>
            <a-select-option value="medium">中</a-select-option>
            <a-select-option value="low">低</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="负责人" name="ownerId">
          <a-select v-model:value="formData.ownerId" placeholder="请选择负责人" show-search :filter-option="filterOption">
            <a-select-option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="预计成交日期" name="expectedCloseDate"><a-date-picker v-model:value="formData.expectedCloseDate" style="width:100%" /></a-form-item>
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
        <a-form-item label="备注" name="remark"><a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="detailVisible" title="商机详情" width="900px" :footer="null">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="商机名称">{{ opportunityDetail.name }}</a-descriptions-item>
        <a-descriptions-item label="商机阶段"><a-tag :color="getStageColor(opportunityDetail.stage)">{{ opportunityDetail.stageLabel }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="客户名称">{{ opportunityDetail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="负责人">{{ opportunityDetail.ownerName }}</a-descriptions-item>
        <a-descriptions-item label="预计金额"><span class="amount">¥{{ formatAmount(opportunityDetail.expectedAmount) }}</span></a-descriptions-item>
        <a-descriptions-item label="成交概率"><a-progress :percent="opportunityDetail.winProbability" size="small" /></a-descriptions-item>
        <a-descriptions-item label="优先级"><a-tag :color="getPriorityColor(opportunityDetail.priority)">{{ getPriorityText(opportunityDetail.priority) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="商机来源">{{ opportunityDetail.sourceLabel }}</a-descriptions-item>
        <a-descriptions-item label="预计成交日期">{{ opportunityDetail.expectedCloseDate }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ opportunityDetail.createTime }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">{{ opportunityDetail.remark }}</a-descriptions-item>
      </a-descriptions>
      <a-divider>商机跟进记录</a-divider>
      <a-timeline>
        <a-timeline-item v-for="r in opportunityDetail.followRecords" :key="r.id" :color="r.type === 'stage_change' ? 'blue' : 'green'">
          <div class="timeline-content"><div class="timeline-title">{{ r.title }}</div><div class="timeline-desc">{{ r.content }}</div><div class="timeline-time">{{ r.createTime }} - {{ r.userName }}</div></div>
        </a-timeline-item>
      </a-timeline>
      <a-divider>关联报价单</a-divider>
      <a-table :columns="quotationColumns" :data-source="opportunityDetail.quotations" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalAmount'"><span class="amount">¥{{ formatAmount(record.totalAmount) }}</span></template>
          <template v-if="column.key === 'status'"><a-tag :color="getQuotationStatusColor(record.status)">{{ record.statusLabel }}</a-tag></template>
        </template>
      </a-table>
    </a-modal>

    <a-modal v-model:open="moveVisible" title="移动商机阶段" width="500px" @ok="handleMoveConfirm">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="当前阶段"><a-tag :color="getStageColor(moveData.currentStage)">{{ getStageText(moveData.currentStage) }}</a-tag></a-form-item>
        <a-form-item label="目标阶段">
          <a-select v-model:value="moveData.targetStage" placeholder="请选择目标阶段">
            <a-select-option v-for="s in pipelineStages" :key="s.key" :value="s.key">{{ s.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="移动原因"><a-textarea v-model:value="moveData.reason" placeholder="请输入移动原因" :rows="3" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, EyeOutlined, EditOutlined, SwapRightOutlined, FileProtectOutlined, StopOutlined } from '@ant-design/icons-vue'
import TableList from '@/components/TableList/TableList.vue'
import type { FormInstance } from 'ant-design-vue'
import * as echarts from 'echarts'
import { opportunityApi } from '@/api/crm'

const tableRef = ref()
const loading = ref(false)
const submitLoading = ref(false)
const modalVisible = ref(false)
const detailVisible = ref(false)
const moveVisible = ref(false)
const modalTitle = ref('新建商机')
const activeTab = ref('pipeline')
const formRef = ref<FormInstance>()
const searchFilters = reactive<Record<string, any>>({})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const searchForm = reactive({ name: '', customerName: '', stage: undefined, minAmount: undefined, maxAmount: undefined, ownerId: undefined })
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
  { title: '商机名称', dataIndex: 'name', key: 'name', width: 180, slotName: 'name' },
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '阶段', dataIndex: 'stage', key: 'stage', width: 100, type: 'status' as const, slotName: 'stage' },
  { title: '预计金额', dataIndex: 'expectedAmount', key: 'expectedAmount', width: 120, slotName: 'expectedAmount' },
  { title: '成交概率', dataIndex: 'winProbability', key: 'winProbability', width: 120, slotName: 'winProbability' },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80, slotName: 'priority' },
  { title: '负责人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
  { title: '预计成交日期', dataIndex: 'expectedCloseDate', key: 'expectedCloseDate', width: 120, type: 'date' as const },
  { title: '操作', key: 'action', width: 180, fixed: 'right' as const, type: 'action' as const }
]

const quotationColumns = [
  { title: '报价单号', dataIndex: 'quotationNo', width: 150 },
  { title: '报价金额', key: 'totalAmount', dataIndex: 'totalAmount', width: 120 },
  { title: '报价日期', dataIndex: 'quotationDate', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 }
]

const stageColorMap: Record<string, string> = { lead: 'default', qualification: 'orange', proposal: 'blue', negotiation: 'purple', closing: 'cyan', won: 'green', lost: 'red' }
const stageTextMap: Record<string, string> = { lead: '线索', qualification: '资格确认', proposal: '方案报价', negotiation: '商务谈判', closing: '成交阶段', won: '已成交', lost: '已流失' }
const priorityColorMap: Record<string, string> = { high: 'red', medium: 'orange', low: 'default' }
const priorityTextMap: Record<string, string> = { high: '高', medium: '中', low: '低' }

function getStageColor(stage: string): string { return stageColorMap[stage] || 'default' }
function getStageText(stage: string): string { return stageTextMap[stage] || stage }
function getPriorityColor(p: string): string { return priorityColorMap[p] || 'default' }
function getPriorityText(p: string): string { return priorityTextMap[p] || p }
function getQuotationStatusColor(status: string): string { const m: Record<string, string> = { draft: 'default', sent: 'blue', accepted: 'green', rejected: 'red' }; return m[status] || 'default' }
function formatAmount(amount: number): string { return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }

const tableData = ref<any[]>([])
const formData = reactive({ id: undefined, name: '', customerId: undefined, stage: 'lead', expectedAmount: undefined, winProbability: 20, priority: 'medium', ownerId: undefined, expectedCloseDate: undefined, source: undefined, remark: '' })
const formRules = { name: [{ required: true, message: '请输入商机名称' }], customerId: [{ required: true, message: '请选择客户' }], stage: [{ required: true, message: '请选择商机阶段' }], expectedAmount: [{ required: true, message: '请输入预计金额' }], ownerId: [{ required: true, message: '请选择负责人' }] }
const moveData = reactive({ opportunityId: undefined, currentStage: '', targetStage: undefined, reason: '' })
const customerList = ref([{ id: 1, name: '北京科技有限公司' }, { id: 2, name: '上海贸易公司' }, { id: 3, name: '广州制造企业' }, { id: 4, name: '深圳电子公司' }, { id: 5, name: '杭州互联网公司' }])
const userList = ref([{ id: 1, name: '张三' }, { id: 2, name: '李四' }, { id: 3, name: '王五' }])
const opportunityDetail = ref<any>({})
const statistics = ref({ total: 25, totalAmount: 1580000, wonAmount: 520000, winRate: 32 })
const stageChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let stageChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const filterOption = (input: string, option: any) => option.name?.toLowerCase().includes(input.toLowerCase())

function getStageOpportunities(stage: string) { return tableData.value.filter(o => o.stage === stage) }
function getStageCount(stage: string) { return getStageOpportunities(stage).length }
function getStageAmount(stage: string) { return getStageOpportunities(stage).reduce((s, o) => s + (o.expectedAmount || 0), 0) }

onMounted(() => { fetchData(); initCharts() })
onUnmounted(() => { stageChart?.dispose(); trendChart?.dispose() })

async function fetchData() {
  loading.value = true
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
  } catch { message.error('获取商机数据失败') }
  finally { loading.value = false }
}

function initCharts() { initStageChart(); initTrendChart() }
function initStageChart() {
  if (!stageChartRef.value) return
  stageChart = echarts.init(stageChartRef.value)
  stageChart.setOption({
    tooltip: { trigger: 'item' }, legend: { orient: 'vertical', left: 'left' },
    series: [{ name: '商机阶段', type: 'pie', radius: ['40%', '70%'], data: pipelineStages.map(s => ({ name: s.name, value: getStageCount(s.key), itemStyle: { color: s.color } })) }]
  })
}
function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' }, legend: { data: ['预计金额', '成交金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [{ name: '预计金额', type: 'line', data: [280000, 320000, 380000, 420000, 480000, 520000] }, { name: '成交金额', type: 'line', data: [120000, 150000, 180000, 200000, 220000, 250000] }]
  })
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() { Object.assign(searchForm, { name: '', customerName: '', stage: undefined, minAmount: undefined, maxAmount: undefined, ownerId: undefined }); handleSearch() }
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSortChange(field: string, order: string) { searchFilters.sortField = field; searchFilters.sortOrder = order; fetchData() }
function handleFilterChange(filters: Record<string, any>) { Object.assign(searchFilters, filters); pagination.current = 1; fetchData() }

function handleAdd() {
  modalTitle.value = '新建商机'
  Object.assign(formData, { id: undefined, name: '', customerId: undefined, stage: 'lead', expectedAmount: undefined, winProbability: 20, priority: 'medium', ownerId: undefined, expectedCloseDate: undefined, source: undefined, remark: '' })
  modalVisible.value = true
}
function handleView(record: any) {
  opportunityDetail.value = {
    ...record, sourceLabel: '客户转介',
    followRecords: [
      { id: 1, title: '创建商机', content: '从线索池创建商机', createTime: record.createTime, userName: record.ownerName, type: 'create' },
      { id: 2, title: '阶段变更', content: `商机从"线索"移动到"${record.stageLabel}"`, createTime: '2024-01-12 10:30', userName: record.ownerName, type: 'stage_change' },
      { id: 3, title: '跟进记录', content: '与客户进行了初步沟通', createTime: '2024-01-14 14:20', userName: record.ownerName, type: 'follow' }
    ],
    quotations: [{ quotationNo: 'QT20240115001', totalAmount: 55000, quotationDate: '2024-01-15', status: 'sent', statusLabel: '已发送' }]
  }
  detailVisible.value = true
}
function handleEdit(record: any) { modalTitle.value = '编辑商机'; Object.assign(formData, record); modalVisible.value = true }
function handleMove(record: any) { moveData.opportunityId = record.id; moveData.currentStage = record.stage; moveData.targetStage = undefined; moveData.reason = ''; moveVisible.value = true }
function handleMoveConfirm() {
  if (!moveData.targetStage) { message.warning('请选择目标阶段'); return }
  message.success('商机阶段已更新'); moveVisible.value = false; fetchData()
}
function handleConvert(record: any) {
  Modal.confirm({ title: '确认转订单', content: `确定要将商机 "${record.name}" 转为订单吗？`, okText: '确认转换', cancelText: '取消', centered: true, async onOk() { message.success('商机已成功转为订单'); fetchData() } })
}
function handleCloseConfirm(record: any) {
  Modal.confirm({ title: '确认关闭', content: `确定要关闭商机 "${record.name}" 吗？`, okText: '确认关闭', okType: 'danger', cancelText: '取消', centered: true, async onOk() { message.success('商机已关闭'); fetchData() } })
}

async function handleSubmit() {
  try { await formRef.value?.validate() } catch { return }
  submitLoading.value = true
  try { message.success('保存成功'); modalVisible.value = false; fetchData() }
  finally { submitLoading.value = false }
}
function handleModalCancel() { formRef.value?.resetFields(); modalVisible.value = false }
</script>

<style scoped lang="scss">
.opportunity-management { padding: 24px; }
.page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; h2 { margin:0 } }
.search-form { margin-bottom:16px; }
.pipeline-container { display:flex; gap:16px; overflow-x:auto; padding:16px 0; }
.pipeline-stage { min-width:280px; background:#fafafa; border-radius:8px; padding:12px;
  .stage-header { display:flex; justify-content:space-between; align-items:center; padding-bottom:12px; border-bottom:1px solid #e8e8e8;
    .stage-name { font-size:14px; font-weight:600; color:#333; }
    .stage-count { font-size:12px; color:#666; margin-left:8px; }
    .stage-amount { font-size:12px; color:#1890ff; margin-left:8px; }
  }
  .stage-cards { max-height:400px; overflow-y:auto; }
  .opportunity-card { background:#fff; border-radius:6px; padding:12px; margin-top:12px; cursor:pointer; border:1px solid #e8e8e8;
    &:hover { border-color:#1890ff; box-shadow:0 2px 8px rgba(24,144,255,0.2); }
    .card-header { display:flex; justify-content:space-between; align-items:center;
      .card-title { font-size:14px; font-weight:500; color:#333; }
    }
    .card-body { .card-row { display:flex; margin-top:8px;
      .label { width:50px; color:#999; font-size:12px; }
      .value { color:#333; font-size:12px; &.amount { color:#f5222d; font-weight:500; } }
    } }
    .card-footer { display:flex; justify-content:space-between; align-items:center; margin-top:12px; padding-top:8px; border-top:1px solid #f0f0f0;
      .close-date { font-size:12px; color:#999; }
      .card-actions { display:flex; gap:8px; }
    }
  }
  .empty-stage { text-align:center; padding:40px 20px; color:#999; font-size:12px; }
}
.amount { color:#f5222d; font-weight:500; }
.chart-container { height:300px; }
.timeline-content {
  .timeline-title { font-size:14px; font-weight:500; color:#333; }
  .timeline-desc { font-size:12px; color:#666; margin-top:4px; }
  .timeline-time { font-size:12px; color:#999; margin-top:4px; }
}
</style>
