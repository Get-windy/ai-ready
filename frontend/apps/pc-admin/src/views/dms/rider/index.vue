<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        配送员管理（配送 → 人车管理 → 配送员管理，菜单 80790 / dms:rider，双入口）
        · 配送员两类来源：企业员工（关联系统账号 + 部门）、外部平台配送员（归属渠道 + 平台骑手ID + 背书有效期）
        · 列配置齿轮在数据表表头（个人/全局，storage-key=dms-rider-columns）；页面配置只管查询条件 + 功能按钮
        · 资质门控：审核未通过 / 证照过期不可接单（后端状态机强制）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('add')"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="btnEnabled('import')"
              size="small"
              @click="showImportModal"
            >
              <UploadOutlined /> 导入
            </a-button>
            <a-button
              v-if="btnEnabled('syncChannel')"
              size="small"
              @click="goChannel"
            >
              <ApiOutlined /> 同步外部运力
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('keyword')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="配送员编号/姓名/手机号"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('riderType')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.riderTypes"
                  mode="multiple"
                  size="small"
                  placeholder="配送员类型"
                  :max-tag-count="2"
                  allow-clear
                  :options="RIDER_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('channelId')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.channelId"
                  size="small"
                  placeholder="归属渠道"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="channelOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('status')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.status"
                  size="small"
                  placeholder="接单状态"
                  allow-clear
                  :options="RIDER_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('verifyStatus')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.verifyStatus"
                  size="small"
                  placeholder="审核状态"
                  allow-clear
                  :options="verifyOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('onlineStatus')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.onlineStatus"
                  size="small"
                  placeholder="在线状态"
                  allow-clear
                  :options="onlineOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('qualifyExpireAlert')"
                class="search-field-item search-checkbox"
              >
                <a-checkbox
                  v-model:checked="searchForm.qualifyExpireAlert"
                  @change="handleSearch"
                >
                  资质到期提醒
                </a-checkbox>
              </div>
              <div
                v-if="fieldVisible('createTime')"
                class="search-field-item search-range"
              >
                <a-range-picker
                  v-model:value="searchForm.createTimeRange"
                  size="small"
                  style="width: 100%"
                  @change="handleSearch"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleReset"
                >
                  重置
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="btnEnabled('batchDelete')"
              storage-key="dms-rider-columns"
              global-config-key="dms-rider-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
            >
              <template #riderNoCell="{ record }">
                <a
                  v-if="record.riderNo"
                  class="cell-link"
                  @click="handleView(record)"
                >{{ record.riderNo }}</a>
              </template>

              <template #realNameCell="{ record }">
                <a
                  class="cell-link"
                  @click="handleView(record)"
                >{{ record.realName }}</a>
              </template>

              <template #riderTypeCell="{ record }">
                <a-tag :color="RIDER_TYPE_MAP[record.riderType]?.color || 'default'">
                  {{ RIDER_TYPE_MAP[record.riderType]?.text || '未知' }}
                </a-tag>
              </template>

              <template #belongCell="{ record }">
                <span v-if="record.channelName">{{ record.channelName }}</span>
                <span v-else-if="record.userName">{{ record.userName }}</span>
                <span
                  v-else
                  class="cell-empty"
                >-</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag :color="RIDER_STATUS_MAP[record.status]?.color || 'default'">
                  {{ RIDER_STATUS_MAP[record.status]?.text || '未知' }}
                </a-tag>
              </template>

              <template #onlineStatusCell="{ record }">
                <a-badge
                  :status="record.onlineStatus === 1 ? 'success' : 'default'"
                  :text="record.onlineStatus === 1 ? '在线' : '离线'"
                />
              </template>

              <template #verifyStatusCell="{ record }">
                <a-tooltip
                  :title="record.verifyRemark || undefined"
                  placement="bottom"
                >
                  <a-tag :color="RIDER_VERIFY_MAP[record.verifyStatus]?.color || 'default'">
                    {{ RIDER_VERIFY_MAP[record.verifyStatus]?.text || '未知' }}
                  </a-tag>
                </a-tooltip>
              </template>

              <template #qualificationCell="{ record }">
                <span v-if="!record.qualificationExpireDate">-</span>
                <span
                  v-else
                  :class="{ 'text-danger': record.qualificationExpired }"
                >
                  {{ record.qualificationExpireDate }}
                  <span class="qualify-tip">{{ qualifyTip(record) }}</span>
                </span>
              </template>

              <template #lastReportCell="{ record }">
                {{ record.lastReportTime ? formatDateTime(record.lastReportTime) : '-' }}
              </template>

              <template #createTimeCell="{ record }">
                {{ record.createTime ? formatDateTime(record.createTime) : '-' }}
              </template>

              <template #actionCell="{ record }">
                <a-space :size="0">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.verifyStatus === 0"
                    type="link"
                    size="small"
                    @click="handleApprove(record)"
                  >
                    审核
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>

              <template #batch-actions="{ selectedRows }">
                <a-button
                  v-if="btnEnabled('batchApprove')"
                  size="small"
                  @click="handleBatchApprove(selectedRows)"
                >
                  批量审核
                </a-button>
                <a-button
                  v-if="btnEnabled('batchStatus')"
                  size="small"
                  @click="handleBatchStatus(selectedRows)"
                >
                  批量启停
                </a-button>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
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
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :hide-print-config="true"
      storage-key="dms-rider-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 审核弹窗（单个） ═══ -->
    <a-modal
      v-model:open="approveVisible"
      title="配送员审核"
      :width="480"
      :confirm-loading="approveLoading"
      ok-text="确认"
      @ok="submitApprove"
    >
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        size="small"
      >
        <a-form-item label="配送员">
          <span>{{ approveState.realName }}（{{ approveState.riderNo }}）</span>
        </a-form-item>
        <a-form-item label="资质到期">
          <span>{{ approveState.qualificationExpireDate || '长期有效' }}</span>
        </a-form-item>
        <a-form-item
          label="审核结果"
          required
        >
          <a-radio-group v-model:value="approveState.verifyStatus">
            <a-radio :value="1">
              通过
            </a-radio>
            <a-radio :value="2">
              拒绝
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="approveState.remark"
            :rows="3"
            placeholder="审核备注（拒绝时建议填写原因）"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 批量审核弹窗 ═══ -->
    <a-modal
      v-model:open="batchApproveVisible"
      title="批量审核"
      :width="480"
      :confirm-loading="approveLoading"
      ok-text="确认"
      @ok="submitBatchApprove"
    >
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        size="small"
      >
        <a-form-item label="已选配送员">
          <span>{{ batchIds.length }} 名</span>
        </a-form-item>
        <a-form-item
          label="审核结果"
          required
        >
          <a-radio-group v-model:value="batchApproveState.verifyStatus">
            <a-radio :value="1">
              通过
            </a-radio>
            <a-radio :value="2">
              拒绝
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="batchApproveState.remark"
            :rows="3"
            placeholder="审核备注"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 批量启停弹窗 ═══ -->
    <a-modal
      v-model:open="batchStatusVisible"
      title="批量启停"
      :width="440"
      :confirm-loading="statusLoading"
      ok-text="确认"
      @ok="submitBatchStatus"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-form-item label="已选配送员">
          <span>{{ batchIds.length }} 名</span>
        </a-form-item>
        <a-form-item label="目标状态">
          <a-select
            v-model:value="batchStatusValue"
            :options="RIDER_STATUS_OPTIONS"
          />
        </a-form-item>
        <div class="modal-tip">
          审核未通过或资质已过期的配送员无法置为「空闲 / 忙碌」（资质门控）。
        </div>
      </a-form>
    </a-modal>

    <!-- ═══ 详情抽屉 ═══ -->
    <a-drawer
      v-model:open="detailVisible"
      title="配送员详情"
      :width="640"
      placement="right"
    >
      <a-spin :spinning="detailLoading">
        <template v-if="detail">
          <a-descriptions
            title="基本信息"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="配送员编号">
              {{ detail.riderNo || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="姓名">
              {{ detail.realName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="手机号">
              {{ detail.phone || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="类型">
              {{ detail.riderTypeText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="归属">
              {{ detail.channelName || detail.userName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="接单状态">
              {{ detail.statusText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="在线状态">
              {{ detail.onlineStatusText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="审核状态">
              {{ detail.verifyStatusText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="审核备注">
              {{ detail.verifyRemark || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="最近上报">
              {{ detail.lastReportTime ? formatDateTime(detail.lastReportTime) : '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="资质信息"
            :column="2"
            size="small"
            bordered
            class="detail-block"
          >
            <a-descriptions-item label="身份证号">
              {{ detail.idCard || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="资质到期">
              {{ detail.qualificationExpireDate || '长期有效' }}
            </a-descriptions-item>
            <a-descriptions-item label="驾驶证号">
              {{ detail.driverLicense || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="健康证号">
              {{ detail.healthCertNo || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="归属与结算"
            :column="2"
            size="small"
            bordered
            class="detail-block"
          >
            <a-descriptions-item label="所属部门">
              {{ detail.deptName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="入职日期">
              {{ detail.entryDate || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="归属渠道">
              {{ detail.channelName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="平台骑手ID">
              {{ detail.platformRiderId || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="结算方式">
              {{ detail.settleMethodText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="当前车辆">
              {{ detail.vehiclePlate || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="绩效与设置"
            :column="3"
            size="small"
            bordered
            class="detail-block"
          >
            <a-descriptions-item label="评分">
              {{ detail.ratingScore ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="累计单量">
              {{ detail.totalOrders ?? 0 }}
            </a-descriptions-item>
            <a-descriptions-item label="今日单量">
              {{ detail.todayOrders ?? 0 }}
            </a-descriptions-item>
            <a-descriptions-item label="准时率">
              {{ detail.punctualRate != null ? detail.punctualRate + '%' : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="服务半径">
              {{ detail.serviceRadius != null ? detail.serviceRadius + ' km' : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="最大并行">
              {{ detail.maxConcurrent ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="工作时间">
              {{ detail.workHoursStart && detail.workHoursEnd ? `${detail.workHoursStart} - ${detail.workHoursEnd}` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="保证金">
              {{ detail.depositAmount ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ detail.createTime ? formatDateTime(detail.createTime) : '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            title="备注"
            :column="1"
            size="small"
            bordered
            class="detail-block"
          >
            <a-descriptions-item label="备注">
              {{ detail.remark || '-' }}
            </a-descriptions-item>
          </a-descriptions>
        </template>
      </a-spin>
    </a-drawer>

    <!-- ═══ 导入向导（三步：下载模板 / 导入 Excel / 完成） ═══ -->
    <BaseDataImportWizard
      v-model:open="importModalVisible"
      title="配送员导入"
      template-url="/dms/rider/import-template"
      template-file-name="配送员导入模板"
      import-url="/dms/rider/import-excel"
      @success="handleImportSuccess"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, DownloadOutlined, UploadOutlined,
  SettingOutlined, ApiOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import {
  riderApi, RIDER_TYPE_MAP, RIDER_TYPE_OPTIONS, RIDER_STATUS_MAP, RIDER_STATUS_OPTIONS,
  RIDER_VERIFY_MAP, type DmsRider,
} from '@/api/dms/rider'
import { channelApi } from '@/api/dms/channel'

defineOptions({ name: 'DmsRiderList' })

const router = useRouter()

// ═══ 格式化 ═══
function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('zh-CN', { hour12: false })
}

function qualifyTip(record: DmsRider): string {
  if (record.qualificationRemainDays == null) return ''
  const days = Number(record.qualificationRemainDays)
  if (days < 0) return `（已过期 ${Math.abs(days)} 天）`
  if (days <= 30) return `（剩 ${days} 天）`
  return ''
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<DmsRider[]>([])
const selectedIds = ref<number[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询条件 ═══
const searchForm = reactive<Record<string, any>>({
  keyword: '',
  riderTypes: [],
  channelId: undefined,
  status: undefined,
  verifyStatus: undefined,
  onlineStatus: undefined,
  qualifyExpireAlert: false,
  createTimeRange: undefined,
})

const verifyOptions = Object.entries(RIDER_VERIFY_MAP).map(([value, item]) => ({
  label: item.text,
  value: Number(value),
}))
const onlineOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
]

// ═══ 渠道下拉（外部平台配送员归属） ═══
const channelOptions = ref<{ label: string; value: number }[]>([])

async function loadChannels() {
  try {
    const res: any = await channelApi.page({ pageNum: 1, pageSize: 200 })
    const rows = res?.records || []
    channelOptions.value = rows.map((c: any) => ({ label: c.channelName, value: c.id }))
  } catch (error) {
    console.warn('[配送员] 渠道加载失败', error)
    channelOptions.value = []
  }
}

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PAGE_CONFIG_KEY = 'dms-rider-page-config'

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '配送员编号/姓名/手机号', visible: true },
  { key: 'riderType', label: '配送员类型（多选）', visible: true },
  { key: 'channelId', label: '归属渠道', visible: true },
  { key: 'status', label: '接单状态', visible: true },
  { key: 'verifyStatus', label: '审核状态', visible: true },
  { key: 'onlineStatus', label: '在线状态', visible: true },
  { key: 'qualifyExpireAlert', label: '资质到期提醒', visible: true },
  { key: 'createTime', label: '创建时间', visible: false },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'import', label: '导入', enabled: true },
  { key: 'syncChannel', label: '同步外部运力', enabled: true },
  { key: 'batchApprove', label: '批量审核', enabled: true },
  { key: 'batchStatus', label: '批量启停', enabled: true },
  { key: 'batchDelete', label: '批量删除', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面把「当前配置」传给 queryFieldsConfig，必须另给出厂值否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))
const showPageConfig = ref(false)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldsConfig.value = QUERY_FIELDS.map(def => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === def.key)
        return saved ? { ...def, visible: saved.visible !== false } : { ...def }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(def => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === def.key)
        return saved ? { ...def, enabled: saved.enabled !== false } : { ...def }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 数据表列（金标准；齿轮列配置走表头） ═══
const columns: any[] = [
  { key: 'rowCheckbox', title: '', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 190, fixed: 'right', slotName: 'actionCell' },
  { title: '配送员编号', field: 'riderNo', key: 'riderNo', width: 130, type: 'slot', slotName: 'riderNoCell', sortable: true },
  { title: '姓名', field: 'realName', key: 'realName', width: 110, type: 'slot', slotName: 'realNameCell', sortable: true },
  { title: '手机号', field: 'phone', key: 'phone', width: 130 },
  { title: '类型', field: 'riderType', key: 'riderType', width: 130, type: 'slot', slotName: 'riderTypeCell' },
  { title: '归属', field: 'channelName', key: 'belong', width: 150, type: 'slot', slotName: 'belongCell' },
  { title: '接单状态', field: 'status', key: 'status', width: 90, type: 'slot', slotName: 'statusCell' },
  { title: '在线状态', field: 'onlineStatus', key: 'onlineStatus', width: 90, type: 'slot', slotName: 'onlineStatusCell' },
  { title: '审核状态', field: 'verifyStatus', key: 'verifyStatus', width: 100, type: 'slot', slotName: 'verifyStatusCell' },
  { title: '资质到期', field: 'qualificationExpireDate', key: 'qualificationExpireDate', width: 170, type: 'slot', slotName: 'qualificationCell' },
  { title: '评分', field: 'ratingScore', key: 'ratingScore', width: 80, align: 'right', sortable: true },
  { title: '累计单量', field: 'totalOrders', key: 'totalOrders', width: 100, align: 'right', sortable: true },
  { title: '今日单量', field: 'todayOrders', key: 'todayOrders', width: 100, align: 'right', defaultHidden: true },
  { title: '准时率(%)', field: 'punctualRate', key: 'punctualRate', width: 100, align: 'right', sortable: true, defaultHidden: true },
  { title: '当前车辆', field: 'vehiclePlate', key: 'vehiclePlate', width: 130 },
  { title: '最近上报时间', field: 'lastReportTime', key: 'lastReportTime', width: 160, type: 'slot', slotName: 'lastReportCell', defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 160, ellipsis: true, defaultHidden: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160, type: 'slot', slotName: 'createTimeCell', defaultHidden: true },
]

// ═══ 查询参数 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (Array.isArray(searchForm.riderTypes) && searchForm.riderTypes.length) {
    params.riderTypes = searchForm.riderTypes.join(',')
  }
  if (searchForm.channelId != null) params.channelId = searchForm.channelId
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.verifyStatus != null) params.verifyStatus = searchForm.verifyStatus
  if (searchForm.onlineStatus != null) params.onlineStatus = searchForm.onlineStatus
  if (searchForm.qualifyExpireAlert) params.qualifyExpireAlert = 1
  const range = searchForm.createTimeRange
  if (Array.isArray(range) && range.length === 2) {
    params.createTimeStart = range[0].format('YYYY-MM-DD')
    params.createTimeEnd = range[1].format('YYYY-MM-DD')
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await riderApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[配送员] 加载列表失败', error)
    message.error(error?.response?.data?.message || error?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  Object.assign(searchForm, {
    keyword: '',
    riderTypes: [],
    channelId: undefined,
    status: undefined,
    verifyStatus: undefined,
    onlineStatus: undefined,
    qualifyExpireAlert: false,
    createTimeRange: undefined,
  })
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSelectionChange(_rows: any[], ids: number[]) {
  selectedIds.value = Array.isArray(ids) ? ids : []
}

// ═══ 新增 / 编辑（双入口 → 独立表单页） ═══
function handleAdd() {
  router.push('/dms/rider/form')
}

function handleEdit(record: DmsRider) {
  router.push(`/dms/rider/form?id=${record.id}`)
}

function goChannel() {
  router.push('/dms/channel')
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<DmsRider | null>(null)

async function handleView(record: DmsRider) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res: any = await riderApi.getById(record.id)
    detail.value = res
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 审核 ═══
const approveVisible = ref(false)
const approveLoading = ref(false)
const approveState = reactive<Record<string, any>>({
  id: 0, riderNo: '', realName: '', qualificationExpireDate: '', verifyStatus: 1, remark: '',
})

function handleApprove(record: DmsRider) {
  Object.assign(approveState, {
    id: record.id,
    riderNo: record.riderNo,
    realName: record.realName,
    qualificationExpireDate: record.qualificationExpireDate,
    verifyStatus: 1,
    remark: '',
  })
  approveVisible.value = true
}

async function submitApprove() {
  if (!approveState.verifyStatus) {
    message.warning('请选择审核结果')
    return
  }
  approveLoading.value = true
  try {
    await riderApi.approve(approveState.id, {
      verifyStatus: approveState.verifyStatus,
      remark: approveState.remark || undefined,
    })
    message.success('审核完成')
    approveVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审核失败')
  } finally {
    approveLoading.value = false
  }
}

// ═══ 批量审核 / 批量启停 ═══
const batchApproveVisible = ref(false)
const batchApproveState = reactive({ verifyStatus: 1, remark: '' })
const batchStatusVisible = ref(false)
const batchStatusValue = ref<number>(1)
const statusLoading = ref(false)
const batchIds = ref<number[]>([])

function handleBatchApprove(rows?: any[]) {
  const ids = (rows && rows.length ? rows.map(r => r.id) : selectedIds.value) || []
  if (!ids.length) {
    message.warning('请先选择配送员')
    return
  }
  batchIds.value = ids
  batchApproveState.verifyStatus = 1
  batchApproveState.remark = ''
  batchApproveVisible.value = true
}

async function submitBatchApprove() {
  approveLoading.value = true
  try {
    await riderApi.batchApprove(batchIds.value, {
      verifyStatus: batchApproveState.verifyStatus,
      remark: batchApproveState.remark || undefined,
    })
    message.success(`已审核 ${batchIds.value.length} 名配送员`)
    batchApproveVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量审核失败')
  } finally {
    approveLoading.value = false
  }
}

function handleBatchStatus(rows?: any[]) {
  const ids = (rows && rows.length ? rows.map(r => r.id) : selectedIds.value) || []
  if (!ids.length) {
    message.warning('请先选择配送员')
    return
  }
  batchIds.value = ids
  batchStatusValue.value = 1
  batchStatusVisible.value = true
}

async function submitBatchStatus() {
  statusLoading.value = true
  try {
    await riderApi.batchStatus(batchIds.value, batchStatusValue.value)
    message.success('批量操作成功')
    batchStatusVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量操作失败')
  } finally {
    statusLoading.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: DmsRider) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除配送员「${record.realName}（${record.riderNo}）」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await riderApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const blob: any = await riderApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `配送员_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 导入 ═══
const importModalVisible = ref(false)

function showImportModal() {
  importModalVisible.value = true
}

function handleImportSuccess(result: any) {
  if (!result) return
  if (result.failure > 0) {
    message.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failure} 条`)
  } else {
    message.success(`导入成功 ${result.success} 条`)
  }
  fetchList()
}

function handleError(error: Error) {
  console.error('[配送员] 页面错误', error)
}

onMounted(async () => {
  loadPageConfig()
  await nextTick()
  fetchList()
  loadChannels()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-checkbox { display: flex; align-items: center; }
.search-range { grid-column: span 2; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.cell-empty { color: #bbb; }
.text-danger { color: #ff4d4f; }
.qualify-tip { color: #fa8c16; font-size: 12px; }
.detail-block { margin-top: 16px; }
.modal-tip { color: #fa8c16; font-size: 12px; line-height: 1.6; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-multiple.ant-select-sm .ant-select-selector) { min-height: 28px; }
</style>
