<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        营销自动化（营销 → 会员中心 → 营销自动化，菜单 80303）
        ⚠️ **本系统建模页**：ql361「营销」域实测仅 17 页，无对应页（见 README §1.3）。
        口径来源：有赞「营销画布」/ 微盟营销中心 / SAP Emarsys Win-Back / 畅捷通「100+ 场景自动化」的通用形态。
        双视图 Tab：① 自动化规则 ② 执行记录；逐 Tab 独立列配置。
        执行链路：按触发点筛候选 → 频控 / 每人仅一次判定 → 执行动作（复用券发放 / 短信群发 / 积分台账）→ 写执行台账。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'rule'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增自动化规则
          </a-button>
          <a-button
            v-if="activeTab === 'rule'"
            size="small"
            :loading="runAllLoading"
            @click="handleRunAll"
          >
            执行全部启用规则
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="activeTab === 'rule'"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="activeTab === 'rule'">
                <span class="search-label">规则名称</span>
                <a-input
                  v-model:value="searchForm.name"
                  placeholder="请输入规则名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">触发点</span>
                <a-select
                  v-model:value="searchForm.triggerType"
                  placeholder="全部触发点"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  :options="TRIGGER_OPTIONS"
                  @change="handleSearch"
                />
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-else>
                <span class="search-label">会员</span>
                <a-input
                  v-model:value="logSearch.memberName"
                  placeholder="请输入会员/客户名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">执行结果</span>
                <a-select
                  v-model:value="logSearch.result"
                  placeholder="全部结果"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="RESULT_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <!-- ═══ Tab1 自动化规则 ═══ -->
            <BillDetailTable
              v-if="activeTab === 'rule'"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-auto-campaign-table-columns"
              global-config-key="marketing-auto-campaign-table-columns"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.name }}</a>
              </template>
              <template #triggerCell="{ record }">
                <span v-if="!record.__ghost">{{ AUTO_TRIGGER_MAP[record.triggerType] || record.triggerType || '-' }}</span>
              </template>
              <template #actionTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="ACTION_COLOR[record.actionType] || 'default'"
                >
                  {{ AUTO_ACTION_MAP[record.actionType] || record.actionType || '-' }}
                </a-tag>
              </template>
              <template #statusCell="{ record }">
                <a-switch
                  v-if="!record.__ghost"
                  :checked="record.status === 1"
                  :loading="togglingId === record.id"
                  checked-children="启用"
                  un-checked-children="停用"
                  @change="(c: any) => handleToggle(record, c)"
                />
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openCandidates(record)"
                  >
                    候选预览
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :loading="runningId === record.id"
                    @click="handleRun(record)"
                  >
                    执行一次
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e: any) => handleMore(e, record)">
                        <a-menu-item key="edit">修改</a-menu-item>
                        <a-menu-item key="logs">查看执行记录</a-menu-item>
                        <a-menu-item key="delete">删除</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab2 执行记录 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="logData"
              :columns="logColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-auto-campaign-log-table-columns"
              global-config-key="marketing-auto-campaign-log-table-columns"
            >
              <template #resultCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="RESULT_COLOR[record.result] || 'default'"
                >
                  {{ RESULT_MAP[record.result] || record.result }}
                </a-tag>
              </template>
              <template #triggerCell="{ record }">
                <span v-if="!record.__ghost">{{ AUTO_TRIGGER_MAP[record.triggerType] || record.triggerType || '-' }}</span>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 新增/修改规则 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改自动化规则' : '新增自动化规则'"
        :confirm-loading="saving"
        width="720px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="规则名称"
                required
              >
                <a-input
                  v-model:value="form.name"
                  placeholder="如 生日送券"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="触发点">
                <a-select
                  v-model:value="form.triggerType"
                  :options="TRIGGER_OPTIONS"
                  placeholder="请选择"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="triggerParamLabel">
                <a-input-number
                  v-model:value="form.triggerDays"
                  :min="1"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="动作">
                <a-select
                  v-model:value="form.actionType"
                  :options="ACTION_OPTIONS"
                  placeholder="请选择"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="form.actionType === 'COUPON'"
              :span="12"
            >
              <a-form-item label="优惠券">
                <a-select
                  v-model:value="form.couponTemplateId"
                  :options="couponOptions"
                  placeholder="请选择券模板"
                  show-search
                  option-filter-prop="label"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="form.actionType === 'POINTS'"
              :span="12"
            >
              <a-form-item label="赠送积分">
                <a-input-number
                  v-model:value="form.pointsValue"
                  :min="1"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="form.actionType === 'SMS'"
              :span="24"
            >
              <a-form-item
                label="短信内容"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="form.smsContent"
                  :rows="3"
                  :maxlength="320"
                  placeholder="支持占位符：{会员名称} {客户名称} {会员卡号}"
                />
                <div class="field-tip">
                  可用占位符：{会员名称} {客户名称} {会员卡号}；营销类短信建议在《发短信》页统一配置签名与合规设置。
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="频控天数">
                <a-input-number
                  v-model:value="form.freqDays"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                  placeholder="0＝不限"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="频控次数">
                <a-input-number
                  v-model:value="form.freqCount"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                  placeholder="0＝不限"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="每人仅一次">
                <a-switch
                  v-model:checked="form.oncePerMemberBool"
                  checked-children="是"
                  un-checked-children="否"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="启用">
                <a-switch
                  v-model:checked="form.statusBool"
                  checked-children="启用"
                  un-checked-children="停用"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-input v-model:value="form.remark" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <!-- ═══ 候选会员预览 ═══ -->
      <a-modal
        v-model:open="candOpen"
        :title="`候选会员预览 · ${candCampaign?.name || ''}`"
        width="960px"
        :footer="null"
      >
        <a-alert
          type="info"
          show-icon
          message="预览只按触发点筛选，不执行任何动作，也不计入频控"
        />
        <a-table
          :columns="CANDIDATE_COLUMNS"
          :data-source="candRows"
          :loading="candLoading"
          :pagination="{ pageSize: 10 }"
          row-key="partnerId"
          size="small"
          style="margin-top: 10px"
          :scroll="{ y: 340 }"
        />
        <div class="detail-tip">
          共 {{ candRows.length }} 位候选会员；执行时仍会再按「频控 / 每人仅一次 / 合规」过滤。
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import {
  autoCampaignApi, couponTemplateApi,
  AUTO_TRIGGER_MAP, AUTO_ACTION_MAP, AUTO_TRIGGER_PARAM_LABEL,
  type AutoCampaign,
} from '@/api/marketing'

defineOptions({ name: 'MarketingAutoCampaign' })

const TABS = [
  { key: 'rule', label: '自动化规则' },
  { key: 'log', label: '执行记录' },
]
const activeTab = ref('rule')

const TRIGGER_OPTIONS = Object.entries(AUTO_TRIGGER_MAP).map(([value, label]) => ({ value, label }))
const ACTION_OPTIONS = Object.entries(AUTO_ACTION_MAP).map(([value, label]) => ({ value, label }))
const STATUS_OPTIONS = [
  { value: 1, label: '启用' },
  { value: 0, label: '停用' },
]
const RESULT_OPTIONS = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'SKIPPED', label: '跳过' },
  { value: 'FAILED', label: '失败' },
]
const RESULT_MAP: Record<string, string> = { SUCCESS: '成功', SKIPPED: '跳过', FAILED: '失败' }
const RESULT_COLOR: Record<string, string> = { SUCCESS: 'green', SKIPPED: 'default', FAILED: 'red' }
const ACTION_COLOR: Record<string, string> = { COUPON: 'blue', SMS: 'purple', POINTS: 'orange' }

const loading = ref(false)
const tableData = ref<AutoCampaign[]>([])
const logData = ref<any[]>([])
const searchForm = reactive({
  name: '', triggerType: undefined as string | undefined, status: undefined as number | undefined,
})
const logSearch = reactive({ memberName: '', result: undefined as string | undefined })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const togglingId = ref<any>(null)
const runningId = ref<any>(null)
const runAllLoading = ref(false)

// Tab1 自动化规则（列：对标无此页 → 本系统建模列）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 230, fixed: 'left' },
  { key: 'name', title: '规则名称', type: 'slot', slotName: 'nameCell', width: 200 },
  { key: 'triggerType', title: '触发点', type: 'slot', slotName: 'triggerCell', width: 140 },
  { key: 'triggerDays', title: '触发参数(天)', type: 'input', width: 120 },
  { key: 'actionType', title: '动作', type: 'slot', slotName: 'actionTypeCell', width: 110 },
  { key: 'freqDays', title: '频控天数', type: 'input', width: 100 },
  { key: 'freqCount', title: '频控次数', type: 'input', width: 100 },
  { key: 'oncePerMember', title: '每人仅一次', type: 'input', width: 110 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'lastRunTime', title: '最近执行时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'lastRunCount', title: '最近候选数', type: 'input', width: 110 },
  { key: 'lastRunSuccess', title: '最近成功数', type: 'input', width: 110 },
  { key: 'remark', title: '备注', type: 'input', width: 180 },
]

// Tab2 执行记录
const logColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'campaignName', title: '规则名称', type: 'input', width: 200 },
  { key: 'triggerType', title: '触发点', type: 'slot', slotName: 'triggerCell', width: 130 },
  { key: 'actionType', title: '动作', type: 'input', width: 100 },
  { key: 'memberName', title: '会员/客户', type: 'input', width: 200 },
  { key: 'mobile', title: '手机号', type: 'input', width: 130 },
  { key: 'triggerNote', title: '触发依据', type: 'input', width: 260 },
  { key: 'result', title: '执行结果', type: 'slot', slotName: 'resultCell', width: 100 },
  { key: 'resultMsg', title: '结果说明', type: 'input', width: 260 },
  { key: 'createTime', title: '执行时间', type: 'slot', slotName: 'timeCell', width: 170 },
]

const CANDIDATE_COLUMNS = [
  { title: '客户编号', dataIndex: 'partyCode', key: 'partyCode', width: 140 },
  { title: '客户名称', dataIndex: 'partyName', key: 'partyName' },
  { title: '会员名称', dataIndex: 'memberName', key: 'memberName', width: 160 },
  { title: '会员卡号', dataIndex: 'memberCardNo', key: 'memberCardNo', width: 160 },
  { title: '手机号', dataIndex: 'mobile', key: 'mobile', width: 130 },
  { title: '触发依据', dataIndex: 'triggerNote', key: 'triggerNote', width: 280 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const triggerParamLabel = computed(() =>
  AUTO_TRIGGER_PARAM_LABEL[form.triggerType || ''] || '触发参数(天)')

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'rule') {
      const res: any = await autoCampaignApi.page({
        name: searchForm.name || undefined,
        triggerType: searchForm.triggerType || undefined,
        status: searchForm.status,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await autoCampaignApi.logPage({
        memberName: logSearch.memberName || undefined,
        result: logSearch.result || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      logData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[营销自动化] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'rule') tableData.value = []
    else logData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchList()
}
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 表单 ═══
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const couponOptions = ref<any[]>([])

const emptyForm = () => ({
  name: '',
  triggerType: 'BIRTHDAY' as string | undefined,
  triggerDays: 7 as number | undefined,
  actionType: 'COUPON' as string | undefined,
  couponTemplateId: undefined as any,
  smsContent: '',
  pointsValue: 100 as number | undefined,
  freqDays: 0 as number | undefined,
  freqCount: 0 as number | undefined,
  oncePerMemberBool: true,
  statusBool: true,
  remark: '',
})
const form = reactive(emptyForm())

async function loadCouponOptions() {
  try {
    const res: any = await couponTemplateApi.page({ pageNum: 1, pageSize: 200, status: 'NORMAL' })
    couponOptions.value = (res?.records || []).map((c: any) => ({ value: c.id, label: c.couponName }))
  } catch (e) {
    console.error('[营销自动化] 加载券模板失败', e)
    couponOptions.value = []
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}
function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    name: record.name || '',
    triggerType: record.triggerType || 'BIRTHDAY',
    triggerDays: record.triggerDays ?? 7,
    actionType: record.actionType || 'COUPON',
    couponTemplateId: record.couponTemplateId,
    smsContent: record.smsContent || '',
    pointsValue: record.pointsValue ?? 100,
    freqDays: record.freqDays ?? 0,
    freqCount: record.freqCount ?? 0,
    oncePerMemberBool: record.oncePerMember === 1,
    statusBool: record.status === 1,
    remark: record.remark || '',
  })
  formOpen.value = true
}

async function handleSave() {
  if (!form.name?.trim()) {
    message.warning('请输入规则名称')
    return
  }
  if (!form.triggerType) {
    message.warning('请选择触发点')
    return
  }
  if (!form.actionType) {
    message.warning('请选择动作')
    return
  }
  if (form.actionType === 'COUPON' && !form.couponTemplateId) {
    message.warning('动作为「发优惠券」时必须选择券模板')
    return
  }
  if (form.actionType === 'SMS' && !form.smsContent?.trim()) {
    message.warning('动作为「发短信」时必须填写短信内容')
    return
  }
  if (form.actionType === 'POINTS' && (!form.pointsValue || form.pointsValue <= 0)) {
    message.warning('动作为「赠积分」时必须填写正数积分')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      name: form.name,
      triggerType: form.triggerType,
      triggerDays: form.triggerDays ?? 7,
      actionType: form.actionType,
      couponTemplateId: form.actionType === 'COUPON' ? form.couponTemplateId : null,
      smsContent: form.actionType === 'SMS' ? form.smsContent : null,
      pointsValue: form.actionType === 'POINTS' ? form.pointsValue : null,
      freqDays: form.freqDays ?? 0,
      freqCount: form.freqCount ?? 0,
      oncePerMember: form.oncePerMemberBool ? 1 : 0,
      status: form.statusBool ? 1 : 0,
      remark: form.remark,
    }
    if (editingId.value) {
      await autoCampaignApi.update(editingId.value, payload)
      message.success('规则已更新')
    } else {
      await autoCampaignApi.create(payload)
      message.success('规则已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启停 / 执行 / 候选 ═══
async function handleToggle(record: any, checked: boolean | string | number) {
  const next = checked ? 1 : 0
  togglingId.value = record.id
  try {
    await autoCampaignApi.changeStatus(record.id, next)
    message.success(next === 1 ? '规则已启用' : '规则已停用')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

function handleRun(record: any) {
  Modal.confirm({
    title: '立即执行一次',
    content: `将按规则「${record.name}」筛选候选会员并执行动作（${AUTO_ACTION_MAP[record.actionType] || ''}）。是否继续？`,
    okText: '执行',
    onOk: async () => {
      runningId.value = record.id
      try {
        const r = await autoCampaignApi.run(record.id, 500)
        message.success(`执行完成：候选 ${r?.candidateCount ?? 0} / 成功 ${r?.success ?? 0} / 跳过 ${r?.skipped ?? 0} / 失败 ${r?.failed ?? 0}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '执行失败')
      } finally {
        runningId.value = null
      }
    },
  })
}

const runAll = async () => {
  runAllLoading.value = true
  try {
    const r: any = await autoCampaignApi.runAll(500)
    message.success(`已执行 ${r?.campaignCount ?? 0} 条规则，成功触达 ${r?.success ?? 0} 人次`)
    fetchList()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '执行失败')
  } finally {
    runAllLoading.value = false
  }
}

function handleRunAll() {
  Modal.confirm({
    title: '执行全部启用规则',
    content: '将依次执行所有「启用」状态的自动化规则。是否继续？',
    okText: '执行',
    onOk: runAll,
  })
}

const candOpen = ref(false)
const candLoading = ref(false)
const candRows = ref<any[]>([])
const candCampaign = ref<any>(null)

async function openCandidates(record: any) {
  candCampaign.value = record
  candOpen.value = true
  candLoading.value = true
  try {
    const res: any = await autoCampaignApi.candidates(record.id, 200)
    candRows.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载候选失败')
    candRows.value = []
  } finally {
    candLoading.value = false
  }
}

function handleMore(e: any, record: any) {
  if (e.key === 'edit') {
    openEdit(record)
  } else if (e.key === 'logs') {
    activeTab.value = 'log'
    pagination.current = 1
    fetchList()
  } else if (e.key === 'delete') {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除规则「${record.name}」吗？（执行记录会保留以便追溯）`,
      okText: '确认删除',
      okType: 'danger',
      onOk: async () => {
        try {
          await autoCampaignApi.remove(record.id)
          message.success('删除成功')
          fetchList()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '删除失败')
        }
      },
    })
  }
}

// ═══ 打印(F8) ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>
    <td>${escapeHtml(r.name)}</td>
    <td>${escapeHtml(AUTO_TRIGGER_MAP[r.triggerType] || '')}</td>
    <td>${escapeHtml(r.triggerDays ?? '')}</td>
    <td>${escapeHtml(AUTO_ACTION_MAP[r.actionType] || '')}</td>
    <td>${r.status === 1 ? '启用' : '停用'}</td>
    <td>${escapeHtml(fmtTime(r.lastRunTime))}</td>
    <td>${escapeHtml(r.lastRunSuccess ?? 0)}</td></tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>营销自动化规则</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>营销自动化规则</h2>
    <table><thead><tr><th>#</th><th>规则名称</th><th>触发点</th><th>触发参数</th><th>动作</th>
    <th>状态</th><th>最近执行</th><th>最近成功数</th></tr></thead><tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    if (activeTab.value === 'rule') handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[营销自动化] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  loadCouponOptions()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.field-tip { font-size: 12px; color: #999; margin-top: 4px; }
.detail-tip { margin-top: 8px; font-size: 12px; color: #999; }
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
