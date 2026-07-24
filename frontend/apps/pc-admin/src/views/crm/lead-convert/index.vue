<template>
  <PageContainer full-height>
    <template #header>
      <div class="convert-page-header">
        <div class="convert-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">首页</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>
              <router-link to="/crm/lead">线索管理</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>线索转化</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="convert-page-header-title">线索转化</h2>
        </div>
        <div class="convert-page-header-right">
          <a-tooltip title="F5: 刷新">
            <a-button size="small" :loading="loading" @click="() => fetchData()">
              <template #icon><ReloadOutlined /></template>刷新
            </a-button>
          </a-tooltip>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <ErrorBoundary @reset="fetchData">
      <!-- 统计 -->
      <div class="stats-cards">
        <a-row :gutter="16">
          <a-col :span="6">
            <div class="stat-card stat-card-blue">
              <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
                <TeamOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">待转化线索</div>
                <div class="stat-value">{{ leadStats.pending }}</div>
                <div class="stat-desc">可转化为客户</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-green">
              <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
                <SwapRightOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">已转化</div>
                <div class="stat-value">{{ leadStats.converted }}</div>
                <div class="stat-desc">已成功转化</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-orange">
              <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
                <DollarOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">预估金额</div>
                <div class="stat-value">¥{{ formatAmount(leadStats.estimatedAmount) }}</div>
                <div class="stat-desc">转化商机预估</div>
              </div>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="stat-card stat-card-purple">
              <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
                <PercentageOutlined />
              </div>
              <div class="stat-content">
                <div class="stat-title">转化率</div>
                <div class="stat-value">{{ leadStats.rate }}%</div>
                <div class="stat-desc">整体转化率</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 筛选 -->
      <a-collapse v-model:active-key="filterExpanded" class="filter-collapse">
        <a-collapse-panel key="1" header="筛选条件">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="线索名称">
                <a-input v-model:value="searchForm.name" placeholder="请输入" allow-clear size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="来源">
                <a-select v-model:value="searchForm.source" placeholder="全部来源" allow-clear size="small">
                  <a-select-option value="website">官网咨询</a-select-option>
                  <a-select-option value="referral">客户介绍</a-select-option>
                  <a-select-option value="marketing">营销活动</a-select-option>
                  <a-select-option value="phone">电话营销</a-select-option>
                  <a-select-option value="other">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6">
              <a-form-item label="线索状态">
                <a-select v-model:value="searchForm.status" placeholder="全部状态" allow-clear size="small">
                  <a-select-option :value="0">新线索</a-select-option>
                  <a-select-option :value="1">跟进中</a-select-option>
                  <a-select-option :value="2">已转化</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="6" class="filter-actions">
              <a-space>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>

      <!-- 线索列表 -->
      <BillTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :selectable="true"
        :min-empty-rows="12"
        add-text="新建线索"
        @add="handleAddLead"
        @refresh="fetchData"
        @page-change="handlePageChange"
        @selection-change="handleSelectionChange"
      >
        <template #batch-actions>
          <a-button v-permission="'crm:lead:batchconvert'" size="small" type="primary" ghost @click="handleBatchConvert">
            <template #icon><SwapRightOutlined /></template>批量转化 ({{ selectedRowKeys.length }})
          </a-button>
        </template>

        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">数据加载失败，请重试</p>
              <a-button type="primary" size="small" @click="() => fetchData()">
                <template #icon><ReloadOutlined /></template>重试
              </a-button>
            </template>
            <template v-else>
              <InboxOutlined class="table-empty-icon" />
              <p class="table-empty-text">暂无线索数据</p>
            </template>
          </div>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <template v-if="record.status !== 2 && record.status !== 3">
              <a-tooltip title="转化向导">
                <a-button type="link" size="small" @click="openConvertWizard(record)">
                  <template #icon><SwapRightOutlined /></template>转化
                </a-button>
              </a-tooltip>
            </template>
            <a-tag v-else color="green">已转化</a-tag>
          </a-space>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusTextMap[record.status] || '未知' }}</a-tag>
        </template>
      </BillTableList>
    </ErrorBoundary>

    <!-- 转化向导弹窗 -->
    <a-modal v-model:open="wizardVisible" title="线索转化向导" width="720px" :footer="null" :closable="true" @cancel="handleWizardClose">
      <a-steps v-model:current="wizardStep" size="small" style="margin-bottom: 24px">
        <a-step title="选择线索" />
        <a-step title="客户信息" />
        <a-step title="创建商机" />
        <a-step title="完成" />
      </a-steps>

      <!-- Step 1: 选择线索 -->
      <div v-if="wizardStep === 0">
        <a-descriptions v-if="wizardLead" bordered :column="2" size="small">
          <a-descriptions-item label="线索名称">{{ wizardLead.name }}</a-descriptions-item>
          <a-descriptions-item label="公司名称">{{ wizardLead.companyName }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ wizardLead.contactName }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ wizardLead.phone }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ sourceTextMap[wizardLead.source] || wizardLead.source }}</a-descriptions-item>
          <a-descriptions-item label="评分">{{ wizardLead.score }}</a-descriptions-item>
        </a-descriptions>
        <div v-else class="step-empty">
          <p>请选择一个线索进行转化</p>
        </div>
      </div>

      <!-- Step 2: 客户信息 -->
      <div v-if="wizardStep === 1">
        <a-form :model="wizardForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="客户名称" required>
            <a-input v-model:value="wizardForm.customerName" placeholder="请输入客户名称" size="small" />
          </a-form-item>
          <a-form-item label="客户等级">
            <a-select v-model:value="wizardForm.customerGrade" placeholder="请选择客户等级" size="small">
              <a-select-option value="A">A级(重要客户)</a-select-option>
              <a-select-option value="B">B级(普通客户)</a-select-option>
              <a-select-option value="C">C级(潜在客户)</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="客户来源">
            <a-input v-model:value="wizardForm.customerSource" placeholder="客户来源" disabled size="small" />
          </a-form-item>
          <a-form-item label="联系电话">
            <a-input v-model:value="wizardForm.customerPhone" placeholder="请输入联系电话" size="small" />
          </a-form-item>
          <a-form-item label="邮箱">
            <a-input v-model:value="wizardForm.customerEmail" placeholder="请输入邮箱" size="small" />
          </a-form-item>
          <a-form-item label="地址">
            <a-input v-model:value="wizardForm.customerAddress" placeholder="请输入地址" size="small" />
          </a-form-item>
        </a-form>
      </div>

      <!-- Step 3: 创建商机 -->
      <div v-if="wizardStep === 2">
        <a-alert message="是否为此客户创建商机?" type="info" show-icon style="margin-bottom: 16px" />
        <a-form :model="wizardForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="创建商机">
            <a-switch v-model:checked="wizardForm.createOpportunity" checked-children="是" un-checked-children="否" />
          </a-form-item>
          <template v-if="wizardForm.createOpportunity">
            <a-form-item label="商机名称" required>
              <a-input v-model:value="wizardForm.opportunityName" placeholder="请输入商机名称" size="small" />
            </a-form-item>
            <a-form-item label="预计金额">
              <a-input-number v-model:value="wizardForm.expectedAmount" :min="0" :precision="2" style="width: 100%" size="small" />
            </a-form-item>
            <a-form-item label="成交概率">
              <a-slider v-model:value="wizardForm.winProbability" :min="0" :max="100" :marks="{ 0: '0%', 50: '50%', 100: '100%' }" />
            </a-form-item>
            <a-form-item label="预计成交日期">
              <a-date-picker v-model:value="wizardForm.expectedCloseDate" style="width: 100%" size="small" />
            </a-form-item>
            <a-form-item label="商机阶段">
              <a-select v-model:value="wizardForm.opportunityStage" placeholder="请选择阶段" size="small">
                <a-select-option value="qualification">资格确认</a-select-option>
                <a-select-option value="proposal">方案报价</a-select-option>
                <a-select-option value="negotiation">商务谈判</a-select-option>
              </a-select>
            </a-form-item>
          </template>
        </a-form>
      </div>

      <!-- Step 4: 完成 -->
      <div v-if="wizardStep === 3">
        <a-result status="success" title="转化成功!">
          <template #subTitle>
            <p>线索已成功转化为客户</p>
            <p v-if="wizardForm.createOpportunity">商机已创建</p>
          </template>
          <template #extra>
            <a-button type="primary" @click="handleWizardDone">完成</a-button>
            <a-button @click="openNewWizard">继续转化</a-button>
          </template>
        </a-result>
      </div>

      <!-- 向导按钮 -->
      <div v-if="wizardStep < 3" class="wizard-footer">
        <a-space>
          <a-button v-if="wizardStep > 0" @click="wizardStep--">上一步</a-button>
          <a-button v-if="wizardStep < 2" type="primary" :disabled="!wizardLead" @click="wizardStep++">下一步</a-button>
          <a-button v-if="wizardStep === 2" type="primary" :loading="converting" @click="handleConvertConfirm">确认转化</a-button>
        </a-space>
      </div>
    </a-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { leadApi } from '@/api/crm'
import dayjs from 'dayjs'
import {
  ReloadOutlined, SwapRightOutlined, TeamOutlined, DollarOutlined,
  WarningOutlined, InboxOutlined, PercentageOutlined, PlusOutlined
} from '@ant-design/icons-vue'

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const converting = ref(false)
const lastUpdateTime = ref('')
const filterExpanded = ref<string[]>([])
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({ name: '', source: undefined as string | undefined, status: undefined as number | undefined })
const tableData = ref<any[]>([])
// PercentageOutlined 别名
const FundOutlined = PercentageOutlined

const statusColorMap: Record<number, string> = { 0: 'blue', 1: 'orange', 2: 'green', 3: 'red' }
const statusTextMap: Record<number, string> = { 0: '新线索', 1: '跟进中', 2: '已转化', 3: '无效' }
const sourceTextMap: Record<string, string> = { website: '官网咨询', weixin: '微信公众号', email: '邮件咨询', phone: '电话咨询', social: '社交媒体', other: '其他' }

const leadStats = computed(() => {
  const pending = tableData.value.filter(l => l.status !== 2 && l.status !== 3).length
  const converted = tableData.value.filter(l => l.status === 2).length
  const estimatedAmount = tableData.value.reduce((s, l) => s + (l.estimatedAmount || 0), 0)
  const total = tableData.value.length
  const rate = total ? Math.round((converted / total) * 100) : 0
  return { pending, converted, estimatedAmount, rate }
})

const vxeColumns = computed(() => [
  { field: 'name', title: '线索名称', width: 160, fixed: 'left' },
  { field: 'companyName', title: '公司', width: 160 },
  { field: 'contactName', title: '联系人', width: 100 },
  { field: 'phone', title: '电话', width: 130 },
  { field: 'source', title: '来源', width: 100, formatter: ({ cellValue }: any) => sourceTextMap[cellValue] || cellValue },
  { field: 'score', title: '评分', width: 80 },
  { field: 'estimatedAmount', title: '预估金额', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${(cellValue).toLocaleString('zh-CN')}` : '-' },
  { field: 'status', title: '状态', width: 90, align: 'center', slotName: 'statusCell' },
  { field: 'action', title: '操作', width: 120, fixed: 'right', type: 'action' }
])

function formatAmount(val: number): string {
  return val?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// 转化向导
const wizardVisible = ref(false)
const wizardStep = ref(0)
const wizardLead = ref<any>(null)
const wizardForm = reactive({
  customerName: '',
  customerGrade: 'B',
  customerSource: '',
  customerPhone: '',
  customerEmail: '',
  customerAddress: '',
  createOpportunity: false,
  opportunityName: '',
  expectedAmount: undefined as number | undefined,
  winProbability: 50,
  expectedCloseDate: undefined as any,
  opportunityStage: 'qualification'
})

function openConvertWizard(record: any) {
  wizardLead.value = record
  wizardStep.value = 0
  wizardForm.customerName = record.companyName || record.name || ''
  wizardForm.customerSource = sourceTextMap[record.source] || record.source || ''
  wizardForm.customerPhone = record.phone || ''
  wizardForm.customerEmail = record.email || ''
  wizardForm.opportunityName = `${record.name} - 商机`
  wizardForm.expectedAmount = record.estimatedAmount || undefined
  wizardVisible.value = true
}

function openNewWizard() {
  wizardLead.value = null
  wizardStep.value = 0
  wizardForm.customerName = ''
  wizardForm.customerGrade = 'B'
  wizardForm.customerSource = ''
  wizardForm.customerPhone = ''
  wizardForm.customerEmail = ''
  wizardForm.customerAddress = ''
  wizardForm.createOpportunity = false
  wizardForm.opportunityName = ''
  wizardForm.expectedAmount = undefined
  wizardForm.winProbability = 50
  wizardForm.expectedCloseDate = undefined
  wizardForm.opportunityStage = 'qualification'
  selectedRowKeys.value = []
  fetchData()
}

async function handleConvertConfirm() {
  if (!wizardLead.value) return
  converting.value = true
  try {
    const payload: any = {
      leadId: wizardLead.value.id,
      customerName: wizardForm.customerName,
      customerGrade: wizardForm.customerGrade,
      customerPhone: wizardForm.customerPhone,
      customerEmail: wizardForm.customerEmail,
      customerAddress: wizardForm.customerAddress,
      createOpportunity: wizardForm.createOpportunity
    }
    if (wizardForm.createOpportunity) {
      payload.opportunityName = wizardForm.opportunityName
      payload.expectedAmount = wizardForm.expectedAmount
      payload.winProbability = wizardForm.winProbability
      payload.expectedCloseDate = wizardForm.expectedCloseDate ? dayjs(wizardForm.expectedCloseDate).format('YYYY-MM-DD') : undefined
      payload.opportunityStage = wizardForm.opportunityStage
    }
    await leadApi.convertToCustomer(wizardLead.value.id)
    message.success('转化成功!')
    wizardStep.value = 3
    fetchData()
  } catch (e: any) {
    console.warn('[线索转化] 转化失败', e)
    message.error(e?.message || '转化失败')
  } finally {
    converting.value = false
  }
}

function handleWizardClose() {
  if (wizardStep.value === 3) {
    wizardVisible.value = false
    openNewWizard()
  } else {
    wizardVisible.value = false
  }
}

function handleWizardDone() {
  wizardVisible.value = false
  openNewWizard()
}

function handleBatchConvert() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要转化的线索')
    return
  }
  Modal.confirm({
    title: '批量转化',
    content: `确定要转化选中的 ${selectedRowKeys.value.length} 个线索吗？`,
    onOk: async () => {
      try {
        await leadApi.batchConvert(selectedRowKeys.value)
        message.success(`成功转化 ${selectedRowKeys.value.length} 个线索`)
        selectedRowKeys.value = []
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '批量转化失败')
      }
    }
  })
}

function handleAddLead() {
  window.dispatchEvent(new CustomEvent('crm:create'))
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleReset() {
  Object.assign(searchForm, { name: '', source: undefined, status: undefined })
  handleSearch()
}
function handlePageChange(page: number, size: number) { pagination.current = page; pagination.pageSize = size; fetchData() }
function handleSelectionChange(rows: any[], ids: any[]) { selectedRowKeys.value = ids }

async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const res = await leadApi.page({
      keyword: searchForm.name || undefined,
      source: searchForm.source,
      leadStatus: searchForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    const result = res as any
    tableData.value = result.records || result.data?.records || []
    pagination.total = result.total ?? result.data?.total ?? 0
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    if (!silent) { hasError.value = true; message.error('获取线索数据失败') }
    tableData.value = []
  } finally {
    if (!silent) loading.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); fetchData() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('crm:refresh' as any, fetchData as any)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('crm:refresh' as any, fetchData as any)
})
</script>

<style scoped>
.convert-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.convert-page-header-left { display: flex; align-items: center; gap: 12px; }
.convert-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.convert-page-header-right { display: flex; align-items: center; gap: 12px; }
.stats-cards { flex-shrink: 0; margin-bottom: 16px; }
.stat-card { display: flex; align-items: center; padding: 16px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); transition: all 0.3s; }
.stat-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.12); transform: translateY(-2px); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; }
.stat-content { flex: 1; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace; }
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.filter-collapse { flex-shrink: 0; margin-bottom: 16px; }
.filter-actions { display: flex; align-items: flex-end; padding-bottom: 4px; }
.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }
.wizard-footer { display: flex; justify-content: flex-end; margin-top: 24px; padding-top: 16px; border-top: 1px solid #f0f0f0; }
.step-empty { text-align: center; padding: 48px 0; color: #999; }
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd { display: inline-flex; align-items: center; justify-content: center; min-width: 18px; height: 18px; padding: 0 3px; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 11px; color: #606266; background: #fff; border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
