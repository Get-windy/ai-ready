<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏左侧：批量操作（选中行后可用） ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <span class="selected-hint">已选 {{ selectedRowKeys.length }} 项</span>
            <a-button
              size="small"
              :disabled="!selectedRowKeys.length"
              @click="handleBatchStatus(1)"
            >
              <CheckCircleOutlined /> 批量启用
            </a-button>
            <a-button
              size="small"
              :disabled="!selectedRowKeys.length"
              @click="handleBatchStatus(0)"
            >
              <StopOutlined /> 批量停用
            </a-button>
            <a-button
              size="small"
              danger
              :disabled="!selectedRowKeys.length"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('add')"
              type="primary"
              size="small"
              @click="openForm()"
            >
              <PlusOutlined /> 新增渠道
            </a-button>
            <a-tooltip
              title="连通性测试（选中一条渠道）"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                v-if="btnEnabled('test')"
                size="small"
                :disabled="selectedRowKeys.length !== 1"
                :loading="testing"
                @click="handleTest()"
              >
                <ApiOutlined /> 连通性测试
              </a-button>
            </a-tooltip>
            <a-tooltip
              title="同步该渠道的外部平台运力"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                v-if="btnEnabled('sync')"
                size="small"
                :disabled="selectedRowKeys.length !== 1"
                :loading="syncing"
                @click="handleSync()"
              >
                <SyncOutlined /> 同步运力
              </a-button>
            </a-tooltip>
            <a-tooltip
              title="向该渠道（外部运力平台）派单：选择待配送任务后下单"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                v-if="btnEnabled('push')"
                size="small"
                :disabled="selectedRowKeys.length !== 1"
                :loading="pushing"
                @click="openPush()"
              >
                <SendOutlined /> 渠道派单
              </a-button>
            </a-tooltip>
            <a-tooltip
              title="外部单台账 + 回调日志（验签/防重放留痕）"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                v-if="btnEnabled('ledger')"
                size="small"
                :disabled="selectedRowKeys.length !== 1"
                @click="openLedger()"
              >
                <ProfileOutlined /> 台账
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
            <a-tooltip
              title="页面配置"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 查询区（按页面配置显隐） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="fieldVisible('channelCode')"
                class="search-item"
              >
                <a-input
                  v-model:value="search.channelCode"
                  placeholder="渠道编码"
                  size="small"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('channelName')"
                class="search-item"
              >
                <a-input
                  v-model:value="search.channelName"
                  placeholder="渠道名称"
                  size="small"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('channelType')"
                class="search-item"
              >
                <span class="search-label">渠道类型</span>
                <a-select
                  v-model:value="search.channelType"
                  size="small"
                  style="width: 140px"
                  placeholder="全部类型"
                  allow-clear
                  :options="CHANNEL_TYPE_OPTIONS"
                />
              </div>
              <div
                v-if="fieldVisible('linkStatus')"
                class="search-item"
              >
                <span class="search-label">对接状态</span>
                <a-select
                  v-model:value="search.linkStatus"
                  size="small"
                  style="width: 130px"
                  placeholder="全部状态"
                  allow-clear
                  :options="LINK_STATUS_OPTIONS"
                />
              </div>
              <div
                v-if="fieldVisible('status')"
                class="search-item"
              >
                <span class="search-label">启用状态</span>
                <a-select
                  v-model:value="search.status"
                  size="small"
                  style="width: 110px"
                  placeholder="全部"
                  allow-clear
                  :options="ENABLE_STATUS_OPTIONS"
                />
              </div>
              <div
                v-if="fieldVisible('createTime')"
                class="search-item"
              >
                <a-range-picker
                  v-model:value="createTimeRange"
                  size="small"
                  style="width: 230px"
                  :placeholder="['创建日期起', '创建日期止']"
                  @change="handleDateChange"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                :loading="loading"
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
        </template>

        <!-- ═══ 数据表：13 列 + 勾选 + 序号 + 操作 ═══ -->
        <template #table>
          <BillTableList
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="billPagination"
            storage-key="dms-channel-columns"
            global-config-key="dms-channel-columns"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <template #channelTypeCell="{ record }">
              {{ channelTypeText(record.channelType) }}
            </template>
            <template #linkStatusCell="{ record }">
              <a-tag :color="linkStatusMeta(record.linkStatus).color">
                {{ linkStatusMeta(record.linkStatus).text }}
              </a-tag>
            </template>
            <template #billingTypeCell="{ record }">
              {{ billingTypeText(record.billingType) }}
            </template>
            <template #riderCell="{ record }">
              {{ record.riderOnline }} / {{ record.riderTotal }}
            </template>
            <template #statusCell="{ record }">
              <a-switch
                :checked="record.status === 1"
                size="small"
                @change="(v: any) => handleStatusChange(record, v)"
              />
            </template>
            <template #lastTestTimeCell="{ record }">
              {{ formatDateTime(record.lastTestTime) }}
            </template>
            <template #actionCell="{ record }">
              <a-space :size="4">
                <a-button
                  type="link"
                  size="small"
                  @click="openForm(record)"
                >
                  编辑
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="openPush(record)"
                >
                  派单
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="openLedger(record)"
                >
                  台账
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  :loading="testingId === String(record.id)"
                  @click="handleTest(record)"
                >
                  测试
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  :loading="syncingId === String(record.id)"
                  @click="handleSync(record)"
                >
                  同步
                </a-button>
                <a-popconfirm
                  title="删除后不可恢复，确定删除该渠道？"
                  ok-text="删除"
                  cancel-text="取消"
                  @confirm="handleDelete(record)"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                  >
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </BillTableList>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :function-buttons-config="functionButtonsConfig"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      storage-key="dms-channel-page-config"
      :hide-print-config="true"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 新增/编辑渠道（4 Tab） ═══ -->
    <a-modal
      v-model:open="showForm"
      :title="formState.id ? '编辑渠道' : '新增渠道'"
      :width="720"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSave"
    >
      <a-tabs v-model:active-key="formTab">
        <a-tab-pane
          key="basic"
          tab="基础信息"
        >
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item
              label="渠道编码"
              required
            >
              <a-input
                v-model:value="formState.channelCode"
                :disabled="!!formState.id"
                placeholder="如 dada / meituan / own_staff"
              />
              <div
                v-if="formState.id"
                class="form-tip"
              >
                渠道编码为对接主键，创建后不可修改
              </div>
            </a-form-item>
            <a-form-item
              label="渠道名称"
              required
            >
              <a-input
                v-model:value="formState.channelName"
                placeholder="如 达达快送"
              />
            </a-form-item>
            <a-form-item label="渠道类型">
              <a-select
                v-model:value="formState.channelType"
                :options="CHANNEL_TYPE_OPTIONS"
              />
            </a-form-item>
            <a-form-item label="调度优先级">
              <a-input-number
                v-model:value="formState.priority"
                :min="0"
                :max="999"
                style="width: 100%"
              />
              <div class="form-tip">
                数字越小越优先被派单
              </div>
            </a-form-item>
            <a-form-item label="启用状态">
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="1">
                  启用
                </a-radio>
                <a-radio :value="0">
                  禁用
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="覆盖区域">
              <a-input
                v-model:value="formState.coverageArea"
                placeholder="行政区划名称，多个用逗号分隔"
              />
            </a-form-item>
            <a-form-item label="备注">
              <a-textarea
                v-model:value="formState.remark"
                :rows="2"
              />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane
          key="link"
          tab="对接配置"
        >
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="适配器 Bean">
              <a-input
                v-model:value="formState.adapterBean"
                placeholder="Spring Bean 名称，如 dadaAdapter"
              />
              <div class="form-tip">
                留空表示该渠道暂不接第三方平台（内部运力）
              </div>
            </a-form-item>
            <a-form-item label="对接配置(JSON)">
              <a-textarea
                v-model:value="formState.configJson"
                :rows="8"
                placeholder='{"appKey":"xxx","appSecret":"yyy","callbackUrl":"https://..."}'
              />
              <div class="form-tip">
                凭据（AppSecret / token / password 等）以 ****** 展示，原值不会随表单回写覆盖
              </div>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane
          key="billing"
          tab="计费规则"
        >
          <a-form
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="计费方式">
              <a-select
                v-model:value="formState.billingType"
                :options="BILLING_TYPE_OPTIONS"
              />
            </a-form-item>
            <a-form-item label="计费配置(JSON)">
              <a-textarea
                v-model:value="formState.billingConfig"
                :rows="6"
                placeholder='{"basePrice":5,"unitPrice":2,"extraPerKm":1}'
              />
              <div class="form-tip">
                起步价 / 单价 / 加价规则等，按计费方式约定字段
              </div>
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <a-tab-pane
          key="status"
          tab="对接状态"
        >
          <a-descriptions
            :column="1"
            bordered
            size="small"
          >
            <a-descriptions-item label="对接状态">
              <a-tag :color="linkStatusMeta(formState.linkStatus).color">
                {{ linkStatusMeta(formState.linkStatus).text }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="最近测试时间">
              {{ formatDateTime(formState.lastTestTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="最近测试结果">
              {{ formState.lastTestResult || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="在线运力">
              {{ formState.riderOnline }} / {{ formState.riderTotal }}
            </a-descriptions-item>
          </a-descriptions>
          <div class="form-tip">
            最近回调（验签 / 防重放 / 处理结果留痕，完整台账见「台账」入口）
          </div>
          <a-table
            size="small"
            :columns="callbackColumns"
            :data-source="recentCallbacks"
            :loading="callbackLoading"
            :pagination="false"
            row-key="id"
          />
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- ═══ 渠道派单（向外部运力平台下单：幂等 + 失败原因如实返回） ═══ -->
    <a-modal
      v-model:open="showPush"
      title="渠道派单（向外部运力平台下单）"
      :width="680"
      :confirm-loading="pushing"
      ok-text="提交派单"
      cancel-text="取消"
      @ok="handlePushSubmit"
    >
      <a-alert
        v-if="pushChannel"
        type="info"
        show-icon
        :message="`目标渠道：${pushChannel.channelName}（${pushChannel.channelCode}）`"
        :description="`适配器：${pushChannel.adapterBean || '未配置'}；下单为幂等操作（同任务同渠道只下一次），失败会如实返回原因`"
      />
      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 19 }"
        class="push-form"
      >
        <a-form-item
          label="待配送任务"
          required
        >
          <a-select
            v-model:value="pushTaskId"
            show-search
            allow-clear
            placeholder="按任务号 / 客户名称搜索「待配送」任务"
            :filter-option="false"
            :options="taskOptions"
            :loading="taskLoading"
            @search="searchPendingTasks"
          />
        </a-form-item>
        <a-form-item label="派单结果">
          <div
            v-if="pushResult"
            class="push-result"
          >
            {{ pushResult }}
          </div>
          <span
            v-else
            class="form-tip"
          >
            提交后显示外部平台单号或失败原因（不伪造成功）
          </span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 渠道台账（外部单 + 回调日志） ═══ -->
    <a-drawer
      v-model:open="showLedger"
      :width="960"
      :title="`渠道台账 · ${ledgerChannel?.channelName || ''}`"
      placement="right"
    >
      <a-tabs v-model:active-key="ledgerTab">
        <a-tab-pane
          key="orders"
          tab="外部单台账"
        >
          <a-table
            size="small"
            :columns="orderColumns"
            :data-source="orders"
            :loading="orderLoading"
            :pagination="orderPager"
            row-key="id"
            @change="handleOrderPage"
          />
        </a-tab-pane>
        <a-tab-pane
          key="callbacks"
          tab="回调日志"
        >
          <a-table
            size="small"
            :columns="callbackColumns"
            :data-source="callbacks"
            :loading="callbackLoading"
            :pagination="callbackPager"
            row-key="id"
            @change="handleCallbackPage"
          />
        </a-tab-pane>
      </a-tabs>
    </a-drawer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 渠道管理（配送 → 配送配置 → 渠道管理，菜单 80880）
 *
 * 配送渠道 = 运力来源：企业自有员工 / 众包 / 外部平台 / 社会车辆。
 * 金标准要点（见《渠道管理开发文档》§3）：
 * · 字典与后端 ChannelTypeEnum 对齐（原页面硬编码 0 线上/1 线下/2 API/3 其他，与后端语义完全冲突）；
 * · 真删除（后端引用保护）+ 批量启停/删除，不再用「改状态」冒充删除；
 * · 连通性测试 / 同步运力走适配器，未对接时如实提示，不伪造成功；
 * · 对接凭据脱敏展示，回传掩码不覆盖库中原值；
 * · 在线运力按渠道实时统计（在线 / 总数）。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import * as XLSX from 'xlsx'
import {
  PlusOutlined, ReloadOutlined, ExportOutlined, SettingOutlined,
  ApiOutlined, SyncOutlined, DeleteOutlined, CheckCircleOutlined, StopOutlined,
  SendOutlined, ProfileOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  channelApi,
  type DmsChannel,
  type DmsChannelOrder,
  type DmsChannelCallbackLog,
} from '@/api/dms/channel'
import { taskApi } from '@/api/dms/task'
import { formatDateTime } from '@/views/dms/shared'

defineOptions({ name: 'DmsChannel' })

function handleError(e: Error) {
  console.error('[渠道管理] 页面错误', e)
}

// ═══════════════════════════════════════════════
// 字典（与后端 ChannelTypeEnum 一一对应）
// ═══════════════════════════════════════════════
const CHANNEL_TYPE_OPTIONS = [
  { label: '自有员工', value: 1 },
  { label: '众包兼职', value: 2 },
  { label: '外部平台', value: 3 },
  { label: '社会车辆', value: 4 },
]

const CHANNEL_TYPE_MAP: Record<number, string> = {
  1: '自有员工',
  2: '众包兼职',
  3: '外部平台',
  4: '社会车辆',
}

const LINK_STATUS_OPTIONS = [
  { label: '未对接', value: 0 },
  { label: '已对接', value: 1 },
  { label: '对接异常', value: 2 },
]

const LINK_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '未对接', color: 'default' },
  1: { text: '已对接', color: 'green' },
  2: { text: '对接异常', color: 'red' },
}

const BILLING_TYPE_OPTIONS = [
  { label: '按单', value: 1 },
  { label: '按距', value: 2 },
  { label: '按重', value: 3 },
]

const BILLING_TYPE_MAP: Record<number, string> = { 1: '按单', 2: '按距', 3: '按重' }

const ENABLE_STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

/** 外部单状态（与后端 DmsChannelOrder.STATUS_* 一一对应） */
const ORDER_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待提交', color: 'default' },
  1: { text: '已提交', color: 'blue' },
  2: { text: '已接单', color: 'cyan' },
  3: { text: '配送中', color: 'processing' },
  4: { text: '已完成', color: 'green' },
  5: { text: '已取消', color: 'default' },
  6: { text: '提交失败', color: 'red' },
}

/** 回调处理结果（与后端 DmsChannelCallbackLog.RESULT_* 对齐） */
const CALLBACK_RESULT_MAP: Record<string, { text: string; color: string }> = {
  OK: { text: '已处理', color: 'green' },
  REPLAY: { text: '重放已忽略', color: 'orange' },
  REJECT: { text: '已拒绝', color: 'red' },
}

function orderStatusMeta(status?: number | null) {
  return ORDER_STATUS_MAP[status ?? -1] || { text: '未知', color: 'default' }
}

function callbackResultMeta(result?: string | null) {
  return CALLBACK_RESULT_MAP[result || ''] || { text: result || '-', color: 'default' }
}

function channelTypeText(type?: number | null) {
  if (type === null || type === undefined) return '-'
  return CHANNEL_TYPE_MAP[type] || `类型${type}`
}

function billingTypeText(type?: number | null) {
  if (type === null || type === undefined) return '-'
  return BILLING_TYPE_MAP[type] || `方式${type}`
}

function linkStatusMeta(status?: number | null) {
  return LINK_STATUS_MAP[status ?? -1] || { text: '未对接', color: 'default' }
}

// ═══════════════════════════════════════════════
// 页面配置
// ═══════════════════════════════════════════════
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig {
  queryFields?: QueryFieldSetting[]
  functionButtons?: FunctionButtonSetting[]
  printConfig?: Record<string, boolean>
}

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'channelCode', label: '渠道编码', visible: true },
  { key: 'channelName', label: '渠道名称', visible: true },
  { key: 'channelType', label: '渠道类型', visible: true },
  { key: 'linkStatus', label: '对接状态', visible: true },
  { key: 'status', label: '启用状态', visible: true },
  { key: 'createTime', label: '创建时间', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增渠道', enabled: true },
  { key: 'test', label: '连通性测试', enabled: true },
  { key: 'sync', label: '同步运力', enabled: true },
  { key: 'push', label: '渠道派单', enabled: true },
  { key: 'ledger', label: '外部单台账', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

function handlePageConfigChange(config: PageConfig) {
  if (config.queryFields) queryFieldsConfig.value = config.queryFields
  if (config.functionButtons) functionButtonsConfig.value = config.functionButtons
}

// ═══════════════════════════════════════════════
// 查询与列表
// ═══════════════════════════════════════════════
const search = reactive<{
  channelCode: string
  channelName: string
  channelType: number | null
  linkStatus: number | null
  status: number | null
  startDate?: string
  endDate?: string
}>({
  channelCode: '',
  channelName: '',
  channelType: null,
  linkStatus: null,
  status: null,
  startDate: undefined,
  endDate: undefined,
})

const createTimeRange = ref<[Dayjs, Dayjs] | null>(null)

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates?.length === 2) {
    search.startDate = dates[0].format('YYYY-MM-DD')
    search.endDate = dates[1].format('YYYY-MM-DD')
  } else {
    search.startDate = undefined
    search.endDate = undefined
  }
}

const loading = ref(false)
const tableData = ref<DmsChannel[]>([])
const selectedRows = ref<DmsChannel[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

const columns = [
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' as const },
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 320, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '渠道编码', field: 'channelCode', key: 'channelCode', width: 140, sortable: true },
  { title: '渠道名称', field: 'channelName', key: 'channelName', width: 150 },
  { title: '渠道类型', field: 'channelType', key: 'channelType', width: 110, slots: { default: 'channelTypeCell' } },
  { title: '对接状态', field: 'linkStatus', key: 'linkStatus', width: 100, slots: { default: 'linkStatusCell' } },
  { title: '覆盖区域', field: 'coverageArea', key: 'coverageArea', width: 160, ellipsis: true },
  { title: '计费方式', field: 'billingType', key: 'billingType', width: 100, slots: { default: 'billingTypeCell' } },
  { title: '在线运力', field: 'riderOnline', key: 'riderOnline', width: 100, align: 'center' as const, slots: { default: 'riderCell' } },
  { title: '优先级', field: 'priority', key: 'priority', width: 90, align: 'right' as const, sortable: true },
  { title: '启用状态', field: 'status', key: 'status', width: 100, slots: { default: 'statusCell' } },
  { title: '最近探测时间', field: 'lastTestTime', key: 'lastTestTime', width: 160, slots: { default: 'lastTestTimeCell' }, defaultHidden: true },
  { title: '最近探测结果', field: 'lastTestResult', key: 'lastTestResult', width: 220, ellipsis: true, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 160, ellipsis: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 160, sortable: true, formatter: (v: any) => formatDateTime(v) },
]

function buildParams() {
  return {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    channelCode: search.channelCode || undefined,
    channelName: search.channelName || undefined,
    channelType: search.channelType ?? undefined,
    linkStatus: search.linkStatus ?? undefined,
    status: search.status ?? undefined,
    startDate: search.startDate,
    endDate: search.endDate,
  }
}

async function fetchData() {
  loading.value = true
  try {
    const page = await channelApi.page(buildParams())
    tableData.value = page?.records || []
    pagination.total = Number(page?.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    message.error('查询失败，请稍后重试')
    console.warn('[渠道管理] 查询失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  search.channelCode = ''
  search.channelName = ''
  search.channelType = null
  search.linkStatus = null
  search.status = null
  search.startDate = undefined
  search.endDate = undefined
  createTimeRange.value = null
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleSelectionChange(rows: DmsChannel[], keys: (string | number)[]) {
  selectedRows.value = rows
  selectedRowKeys.value = keys
}

// ═══════════════════════════════════════════════
// 行操作
// ═══════════════════════════════════════════════
async function handleStatusChange(record: DmsChannel, checked: boolean) {
  const next = checked ? 1 : 0
  try {
    await channelApi.updateStatus(record.id, next)
    record.status = next
    message.success(next === 1 ? '已启用' : '已禁用')
  } catch (e) {
    console.warn('[渠道管理] 状态更新失败', e)
  }
}

async function handleBatchStatus(status: number) {
  if (!selectedRowKeys.value.length) return
  try {
    await channelApi.batchStatus(selectedRowKeys.value, status)
    message.success(status === 1 ? '批量启用成功' : '批量停用成功')
    fetchData()
  } catch (e) {
    console.warn('[渠道管理] 批量启停失败', e)
  }
}

async function handleDelete(record: DmsChannel) {
  try {
    await channelApi.remove(record.id)
    message.success('已删除')
    fetchData()
  } catch (e) {
    console.warn('[渠道管理] 删除失败', e)
  }
}

function handleBatchDelete() {
  if (!selectedRowKeys.value.length) return
  Modal.confirm({
    title: '批量删除渠道',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条渠道？被配送员引用的渠道会被拒绝并提示原因。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await channelApi.batchRemove(selectedRowKeys.value)
        message.success('批量删除完成')
        fetchData()
      } catch (e) {
        console.warn('[渠道管理] 批量删除失败', e)
      }
    },
  })
}

// ═══════════════════════════════════════════════
// 连通性测试 / 运力同步
// ═══════════════════════════════════════════════
const testing = ref(false)
const testingId = ref('')
const syncing = ref(false)
const syncingId = ref('')

async function handleTest(record?: DmsChannel) {
  const target = record || selectedRows.value[0]
  if (!target) {
    message.warning('请先选择一条渠道')
    return
  }
  testing.value = !record
  testingId.value = String(target.id)
  try {
    const res = await channelApi.test(target.id)
    if (res?.success) {
      message.success(res.message || '连通性正常')
    } else {
      message.warning(res?.message || '渠道未接通')
    }
    fetchData()
  } catch (e) {
    console.warn('[渠道管理] 连通性测试失败', e)
  } finally {
    testing.value = false
    testingId.value = ''
  }
}

async function handleSync(record?: DmsChannel) {
  const target = record || selectedRows.value[0]
  if (!target) {
    message.warning('请先选择一条渠道')
    return
  }
  syncing.value = !record
  syncingId.value = String(target.id)
  try {
    const res = await channelApi.syncRiders(target.id)
    const ok = (res?.inserted || 0) + (res?.updated || 0) > 0
    if (ok) {
      message.success(res?.message || '同步完成')
    } else {
      message.warning(res?.message || '该渠道暂无可同步运力')
    }
    fetchData()
  } catch (e) {
    console.warn('[渠道管理] 运力同步失败', e)
  } finally {
    syncing.value = false
    syncingId.value = ''
  }
}

// ═══════════════════════════════════════════════
// 渠道派单（向外部平台下单；幂等 + 失败原因如实返回）
// ═══════════════════════════════════════════════
const showPush = ref(false)
const pushing = ref(false)
const pushChannel = ref<DmsChannel | null>(null)
const pushTaskId = ref<string | number | undefined>(undefined)
const pushResult = ref('')
const taskOptions = ref<{ label: string; value: string | number }[]>([])
const taskLoading = ref(false)

/** 待配送任务选择器（禁止手输 ID；按任务号/客户服务端搜索） */
async function searchPendingTasks(keyword?: string) {
  taskLoading.value = true
  try {
    const res: any = await taskApi.page({
      pageNum: 1,
      pageSize: 50,
      statusList: '0', // 待分配
      keyword: keyword || undefined,
    } as any)
    taskOptions.value = (res?.records || []).map((t: any) => ({
      label: `${t.taskNo}｜${t.customerName || t.customerAddress || '未填客户'}${t.channelId ? '（已指定渠道）' : ''}`,
      value: String(t.id),
    }))
  } catch (e) {
    taskOptions.value = []
    console.warn('[渠道管理] 待配送任务加载失败', e)
  } finally {
    taskLoading.value = false
  }
}

function openPush(record?: DmsChannel) {
  const target = record || selectedRows.value[0]
  if (!target) {
    message.warning('请先选择一条渠道')
    return
  }
  pushChannel.value = target
  pushTaskId.value = undefined
  pushResult.value = ''
  taskOptions.value = []
  showPush.value = true
  searchPendingTasks()
}

async function handlePushSubmit() {
  if (!pushChannel.value) return
  if (!pushTaskId.value) {
    message.warning('请选择要派单的待配送任务')
    return
  }
  pushing.value = true
  try {
    const res = await channelApi.pushOrder(pushChannel.value.id, pushTaskId.value)
    const head = res?.success ? '成功' : '失败'
    const no = res?.channelOrderNo ? `（外部单号 ${res.channelOrderNo}）` : ''
    const reuse = res?.reused ? '［幂等复用台账］' : ''
    pushResult.value = `${head}：${res?.message || ''}${no}${reuse}`
    if (res?.success) {
      message.success('渠道派单已受理')
      fetchData()
    } else {
      message.warning(res?.message || '渠道下单失败')
    }
  } catch (e) {
    console.warn('[渠道管理] 渠道派单失败', e)
  } finally {
    pushing.value = false
  }
}

// ═══════════════════════════════════════════════
// 渠道台账（外部单 + 回调日志）
// ═══════════════════════════════════════════════
const showLedger = ref(false)
const ledgerChannel = ref<DmsChannel | null>(null)
const ledgerTab = ref('orders')
const orders = ref<DmsChannelOrder[]>([])
const callbacks = ref<DmsChannelCallbackLog[]>([])
const recentCallbacks = ref<DmsChannelCallbackLog[]>([])
const orderLoading = ref(false)
const callbackLoading = ref(false)
const orderPager = reactive({ current: 1, pageSize: 10, total: 0 })
const callbackPager = reactive({ current: 1, pageSize: 10, total: 0 })

const orderColumns = [
  { title: '外部单号', dataIndex: 'channelOrderNo', key: 'channelOrderNo', width: 160, customRender: ({ text }: any) => text || '-' },
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 150, customRender: ({ text }: any) => text || '-' },
  { title: '状态', dataIndex: 'orderStatus', key: 'orderStatus', width: 90, customRender: ({ text }: any) => orderStatusMeta(text).text },
  { title: '尝试次数', dataIndex: 'attempts', key: 'attempts', width: 84, align: 'center' as const },
  { title: '最近失败原因', dataIndex: 'lastError', key: 'lastError', ellipsis: true, customRender: ({ text }: any) => text || '-' },
  { title: '提交时间', dataIndex: 'submitTime', key: 'submitTime', width: 160, customRender: ({ text }: any) => formatDateTime(text) },
]

const callbackColumns = [
  { title: '接收时间', dataIndex: 'receiveTime', key: 'receiveTime', width: 160, customRender: ({ text }: any) => formatDateTime(text) },
  { title: '外部单号', dataIndex: 'channelOrderNo', key: 'channelOrderNo', width: 150, customRender: ({ text }: any) => text || '-' },
  { title: '事件/状态', dataIndex: 'eventType', key: 'eventType', width: 140, customRender: ({ text, record }: any) => text || record?.externalStatus || '-' },
  { title: '验签', dataIndex: 'signOk', key: 'signOk', width: 70, customRender: ({ text }: any) => (Number(text) === 1 ? '通过' : '失败') },
  {
    title: '结果', dataIndex: 'processResult', key: 'processResult', width: 120,
    customRender: ({ text, record }: any) =>
      callbackResultMeta(text).text + (Number(record?.replayed) === 1 ? '（重放）' : ''),
  },
  { title: '处理说明', dataIndex: 'processMessage', key: 'processMessage', ellipsis: true, customRender: ({ text }: any) => text || '-' },
]

async function loadOrders() {
  if (!ledgerChannel.value) return
  orderLoading.value = true
  try {
    const res = await channelApi.orders(ledgerChannel.value.id, {
      pageNum: orderPager.current,
      pageSize: orderPager.pageSize,
    })
    orders.value = res?.records || []
    orderPager.total = Number(res?.total) || 0
  } catch (e) {
    orders.value = []
    orderPager.total = 0
    console.warn('[渠道管理] 外部单台账加载失败', e)
  } finally {
    orderLoading.value = false
  }
}

async function loadCallbacks() {
  if (!ledgerChannel.value) return
  callbackLoading.value = true
  try {
    const res = await channelApi.callbackLogs(ledgerChannel.value.id, {
      pageNum: callbackPager.current,
      pageSize: callbackPager.pageSize,
    })
    callbacks.value = res?.records || []
    callbackPager.total = Number(res?.total) || 0
  } catch (e) {
    callbacks.value = []
    callbackPager.total = 0
    console.warn('[渠道管理] 回调日志加载失败', e)
  } finally {
    callbackLoading.value = false
  }
}

/** 对接状态 Tab 的最近回调（只取 5 条，完整台账在「台账」抽屉） */
async function loadRecentCallbacks(channelId: number | string) {
  callbackLoading.value = true
  try {
    const res = await channelApi.callbackLogs(channelId, { pageNum: 1, pageSize: 5 })
    recentCallbacks.value = res?.records || []
  } catch (e) {
    recentCallbacks.value = []
    console.warn('[渠道管理] 最近回调加载失败', e)
  } finally {
    callbackLoading.value = false
  }
}

function openLedger(record?: DmsChannel) {
  const target = record || selectedRows.value[0]
  if (!target) {
    message.warning('请先选择一条渠道')
    return
  }
  ledgerChannel.value = target
  ledgerTab.value = 'orders'
  orderPager.current = 1
  callbackPager.current = 1
  orders.value = []
  callbacks.value = []
  showLedger.value = true
  loadOrders()
  loadCallbacks()
}

function handleOrderPage(p: any) {
  orderPager.current = p?.current || 1
  orderPager.pageSize = p?.pageSize || 10
  loadOrders()
}

function handleCallbackPage(p: any) {
  callbackPager.current = p?.current || 1
  callbackPager.pageSize = p?.pageSize || 10
  loadCallbacks()
}

// ═══════════════════════════════════════════════
// 新增 / 编辑
// ═══════════════════════════════════════════════
const showForm = ref(false)
const saving = ref(false)
const formTab = ref('basic')

const emptyForm = () => ({
  id: undefined as number | string | undefined,
  channelCode: '',
  channelName: '',
  channelType: 3,
  adapterBean: '',
  configJson: '',
  priority: 100,
  status: 1,
  coverageArea: '',
  billingType: 1,
  billingConfig: '',
  linkStatus: 0,
  lastTestTime: null as string | null,
  lastTestResult: '',
  riderOnline: 0,
  riderTotal: 0,
  remark: '',
})

const formState = reactive(emptyForm())

async function openForm(record?: DmsChannel) {
  Object.assign(formState, emptyForm())
  formTab.value = 'basic'
  recentCallbacks.value = []
  if (record) {
    try {
      const detail = await channelApi.getById(record.id)
      Object.assign(formState, emptyForm(), detail)
    } catch (e) {
      console.warn('[渠道管理] 详情加载失败', e)
      Object.assign(formState, emptyForm(), record)
    }
    loadRecentCallbacks(record.id)
  }
  showForm.value = true
}

async function handleSave() {
  if (!formState.channelCode?.trim()) {
    message.warning('请填写渠道编码')
    formTab.value = 'basic'
    return
  }
  if (!formState.channelName?.trim()) {
    message.warning('请填写渠道名称')
    formTab.value = 'basic'
    return
  }
  saving.value = true
  try {
    const payload: Partial<DmsChannel> = {
      channelCode: formState.channelCode.trim(),
      channelName: formState.channelName.trim(),
      channelType: formState.channelType,
      adapterBean: formState.adapterBean || null,
      configJson: formState.configJson || null,
      priority: formState.priority,
      status: formState.status,
      coverageArea: formState.coverageArea || null,
      billingType: formState.billingType,
      billingConfig: formState.billingConfig || null,
      remark: formState.remark || null,
    }
    if (formState.id) {
      await channelApi.update(formState.id, payload)
      message.success('保存成功')
    } else {
      await channelApi.create(payload)
      message.success('创建成功')
    }
    showForm.value = false
    fetchData()
  } catch (e) {
    console.warn('[渠道管理] 保存失败', e)
  } finally {
    saving.value = false
  }
}

// ═══════════════════════════════════════════════
// 导出（真实 xlsx，按当前查询条件全量拉取）
// ═══════════════════════════════════════════════
const EXPORT_PAGE_SIZE = 5000

async function handleExport() {
  loading.value = true
  try {
    const page = await channelApi.page({ ...buildParams(), pageNum: 1, pageSize: EXPORT_PAGE_SIZE })
    const list = page?.records || []
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    const rows = list.map(r => ({
      渠道编码: r.channelCode,
      渠道名称: r.channelName,
      渠道类型: channelTypeText(r.channelType),
      对接状态: linkStatusMeta(r.linkStatus).text,
      覆盖区域: r.coverageArea || '',
      计费方式: billingTypeText(r.billingType),
      在线运力: `${r.riderOnline} / ${r.riderTotal}`,
      优先级: r.priority,
      启用状态: r.status === 1 ? '启用' : '禁用',
      最近探测时间: formatDateTime(r.lastTestTime),
      最近探测结果: r.lastTestResult || '',
      备注: r.remark || '',
      创建时间: formatDateTime(r.createTime),
    }))
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '渠道管理')
    XLSX.writeFile(wb, `渠道管理_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${list.length} 条`)
  } catch (e) {
    message.error('导出失败')
    console.warn('[渠道管理] 导出失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 插槽内容样式需自备（Vue scoped 不作用到布局组件内部的插槽内容） */
.search-area {
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
}
.selected-hint {
  font-size: 12px;
  color: #909399;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 2px;
}
.push-form {
  margin-top: 12px;
}
.push-result {
  padding: 8px 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.7;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
