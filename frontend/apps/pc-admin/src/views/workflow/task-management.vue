<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        我的待办 / 我的已办（设置 → 审批，菜单 803 `my-task` / 804 `my-done`）
        · 两菜单 DB `component` 不同写法（带 / 不带 `.vue`）归一化后**同指本组件**（dynamicRoutes 去
          `views/` 前缀与 `.vue` 后缀）→ 组件**必须自己读路由**决定默认 Tab，否则从「我的已办」进入
          永远停在「待办」（历史 P0，见《我的已办开发文档》§9.2）。
        · 骨架：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillTableList
        · 两个 Tab 各自独立一套列定义 / 查询条件 / 功能按钮 / storage-key（列配置 + 页面配置都按 Tab 分存）
        · 统计卡口径：服务端 GET /workflow/task/stat **全量真聚合**（与列表同筛选条件），不是当页条数
        · 行内操作按后端真实能力接线：查看详情 / 审批（同意·驳回·退回）/ 转办
          —— 本系统无 owner 字段，「委托」不可实现，故不暴露（见 api/workflow/index.ts 注释）
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：页标题（随菜单语义）/ 更新时间 / 自动刷新倒计时 ═══ -->
        <template #toolbar-left>
          <a-space :size="12">
            <span class="page-title">{{ pageTitle }}</span>
            <span
              v-if="lastUpdateTime"
              class="toolbar-tip"
            >更新于 {{ lastUpdateTime }}</span>
            <span
              v-if="autoRefreshCountdown > 0"
              class="toolbar-tip"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置（查询条件 / 功能按钮）"
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
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="debounceClick('refresh', handleQuery)()"
            >
              <ReloadOutlined /> 刷新(F5)
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格（两个 Tab 各自独立一套条件） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- Tab1 待办任务 -->
            <div
              v-if="activeTab === 'todo'"
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('taskName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="todoQuery.taskName"
                  placeholder="任务名称（节点名）"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('processName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="todoQuery.processName"
                  placeholder="流程名称"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('priority')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="todoQuery.priority"
                  placeholder="优先级"
                  size="small"
                  allow-clear
                  :options="PRIORITY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('createTime')"
                class="search-field-item search-range"
              >
                <a-range-picker
                  v-model:value="todoQuery.dateRange"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
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

            <!-- Tab2 已办任务 -->
            <div
              v-else
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('taskName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="doneQuery.taskName"
                  placeholder="任务名称（节点名）"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('processName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="doneQuery.processName"
                  placeholder="流程名称"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('priority')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="doneQuery.priority"
                  placeholder="优先级"
                  size="small"
                  allow-clear
                  :options="PRIORITY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('createTime')"
                class="search-field-item search-range"
              >
                <a-range-picker
                  v-model:value="doneQuery.dateRange"
                  size="small"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
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

        <!-- ═══ 数据区：统计卡（全量真聚合）+ 数据表 ═══ -->
        <template #table>
          <div class="task-body">
            <!-- 统计卡：口径 = 当前 Tab + 当前筛选条件下的**全量**聚合 -->
            <div class="stat-cards">
              <div
                v-for="card in statCards"
                :key="card.key"
                class="stat-card"
                :class="card.cls"
              >
                <div class="stat-card-body">
                  <div class="stat-card-value">
                    {{ card.value }}
                  </div>
                  <div class="stat-card-label">
                    <a-tooltip
                      :title="card.tip"
                      placement="bottom"
                      :mouse-enter-delay="0.4"
                    >
                      <span>{{ card.label }}</span>
                    </a-tooltip>
                  </div>
                </div>
                <component
                  :is="card.icon"
                  class="stat-card-icon"
                />
              </div>
            </div>

            <!-- 列表状态条：加载失败 / 空结果（BillTableList 无 `empty` 插槽，且本页传了 min-empty-rows，
                 故不依赖表格内置空态；原先挂在 `#empty` 上的空态是**死代码**，从未渲染） -->
            <div
              v-if="hasError"
              class="table-status table-status-error"
            >
              <WarningOutlined />
              加载失败，请重试
              <a
                class="table-status-action"
                @click="debounceClick('refresh', handleQuery)()"
              >重新加载</a>
            </div>
            <div
              v-else-if="!loading && tableData.length === 0"
              class="table-status"
            >
              <SearchOutlined v-if="hasActiveFilters" />
              <InboxOutlined v-else />
              <template v-if="hasActiveFilters">
                没有符合条件的任务记录，
                <a
                  class="table-status-action"
                  @click="handleReset"
                >清除筛选</a>
              </template>
              <template v-else>
                {{ emptyText }}
              </template>
            </div>

            <div
              ref="tableWrap"
              class="table-area"
            >
              <BillTableList
                :columns="columns"
                :data-source="tableData"
                :loading="loading"
                :pagination="false"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                :selectable="false"
                :min-empty-rows="12"
                :storage-key="tableStorageKey"
                :global-config-key="tableStorageKey"
                row-key="taskId"
              >
                <!-- 任务名称：点击打开只读详情（行双击同样打开该详情，入口在 script 的 useRowDblclick） -->
                <template #taskNameCell="{ record }">
                  <a
                    v-if="record.taskName"
                    class="cell-link"
                    @click="handleViewDetail(record)"
                  >{{ record.taskName }}</a>
                  <span v-else>-</span>
                </template>

                <!-- 优先级：后端如实回传 workflow_task.priority（可空列），空值显示「-」 -->
                <template #priorityCell="{ record }">
                  <a-tag
                    v-if="record.priority"
                    :color="getPriorityColor(record.priority)"
                  >
                    {{ getPriorityLabel(record.priority) }}
                  </a-tag>
                  <span v-else>-</span>
                </template>

                <!-- 审批结果（仅已办 Tab）：action 为后端 taskActionToString 输出串 -->
                <template #actionResultCell="{ record }">
                  <a-tag :color="TASK_ACTION_TEXT_MAP[record.action]?.color || 'default'">
                    {{ TASK_ACTION_TEXT_MAP[record.action]?.text || record.action || '-' }}
                  </a-tag>
                </template>

                <!-- 审批意见（仅已办 Tab） -->
                <template #commentCell="{ record }">
                  <a-tooltip
                    v-if="record.comment"
                    :title="record.comment"
                    placement="bottom"
                  >
                    <span class="cell-ellipsis">{{ record.comment }}</span>
                  </a-tooltip>
                  <span v-else>-</span>
                </template>

                <!-- 任务状态 -->
                <template #statusCell="{ record }">
                  <a-tag :color="TASK_STATUS_TEXT_MAP[record.status]?.color || 'default'">
                    {{ TASK_STATUS_TEXT_MAP[record.status]?.text || '-' }}
                  </a-tag>
                </template>

                <!-- 行内操作：按后端真实能力接线（已办 Tab 只读，仅保留查看详情） -->
                <template #actionCell="{ record }">
                  <a-space :size="0">
                    <a-button
                      v-permission="'workflow:task:view'"
                      type="link"
                      size="small"
                      @click="handleViewDetail(record)"
                    >
                      查看详情
                    </a-button>
                    <a-button
                      v-if="activeTab === 'todo'"
                      v-permission="'workflow:task:approve'"
                      type="link"
                      size="small"
                      @click="handleApprove(record)"
                    >
                      审批
                    </a-button>
                    <a-button
                      v-if="activeTab === 'todo'"
                      v-permission="'workflow:task:transfer'"
                      type="link"
                      size="small"
                      @click="handleTransfer(record)"
                    >
                      转办
                    </a-button>
                  </a-space>
                </template>

              </BillTableList>
            </div>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[10, 20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（查询条件 + 功能按钮）；按 Tab 分存，切 Tab 换 key ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonsConfig"
        :default-query-fields-config="defaultQueryFields"
        :default-function-buttons-config="defaultFunctionButtons"
        :storage-key="pageConfigStorageKey"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 任务详情抽屉（只读） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="任务详情"
        placement="right"
        :width="720"
        :footer="null"
      >
        <template #extra>
          <a-button
            size="small"
            :loading="detailLoading"
            :disabled="!detailRecord"
            @click="fetchDetail(detailRecord?.taskId)"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </template>
        <a-skeleton
          active
          :loading="detailLoading"
          :paragraph="{ rows: 12 }"
        >
          <template v-if="detailData">
            <a-descriptions
              bordered
              :column="2"
              size="small"
            >
              <a-descriptions-item label="任务ID">
                {{ detailData.taskId }}
              </a-descriptions-item>
              <a-descriptions-item label="任务名称">
                {{ detailData.taskName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="流程名称">
                {{ detailData.processName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="处理人">
                {{ detailData.assignee || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="当前节点">
                {{ detailData.currentNode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="任务状态">
                <a-tag :color="TASK_STATUS_TEXT_MAP[detailData.status]?.color || 'default'">
                  {{ TASK_STATUS_TEXT_MAP[detailData.status]?.text || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="审批结果">
                <a-tag :color="TASK_ACTION_TEXT_MAP[detailData.action]?.color || 'default'">
                  {{ TASK_ACTION_TEXT_MAP[detailData.action]?.text || detailData.action || '-' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="优先级">
                <a-tag
                  v-if="detailData.priority"
                  :color="getPriorityColor(detailData.priority)"
                >
                  {{ getPriorityLabel(detailData.priority) }}
                </a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ detailData.createTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="截止时间">
                {{ detailData.dueTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="处理时间"
                :span="2"
              >
                {{ detailData.handleTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="审批意见"
                :span="2"
              >
                {{ detailData.comment || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="任务描述"
                :span="2"
              >
                {{ detailData.description || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="业务数据"
                :span="2"
              >
                <pre>{{ prettyBusinessData }}</pre>
              </a-descriptions-item>
            </a-descriptions>
          </template>
          <a-result
            v-else-if="detailError"
            status="warning"
            title="加载失败"
            :sub-title="detailError"
          >
            <template #extra>
              <a-button
                type="primary"
                size="small"
                @click="fetchDetail(detailRecord?.taskId)"
              >
                重试
              </a-button>
            </template>
          </a-result>
        </a-skeleton>
      </a-drawer>

      <!-- ═══ 审批弹窗（同意 / 驳回 / 退回；三项均可填写意见） ═══ -->
      <a-modal
        v-model:open="approveVisible"
        title="任务审批"
        :width="560"
        :confirm-loading="approveSubmitting"
        @ok="handleConfirmApprove"
        @cancel="handleCancelApprove"
      >
        <a-form
          :model="approveForm"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item label="待办任务">
            <span>{{ approveTaskName || '-' }}</span>
          </a-form-item>
          <a-form-item label="审批动作">
            <a-radio-group v-model:value="approveForm.approval">
              <a-radio value="approve">
                同意
              </a-radio>
              <a-radio value="reject">
                驳回
              </a-radio>
              <a-radio value="return">
                退回
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            v-if="approveForm.approval === 'return'"
            label="退回节点"
          >
            <a-select
              v-model:value="approveForm.returnNode"
              size="small"
              placeholder="请选择退回节点"
              :options="RETURN_NODE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="审批意见">
            <a-textarea
              v-model:value="approveForm.comment"
              size="small"
              :rows="4"
              :maxlength="200"
              placeholder="请输入审批意见"
            />
          </a-form-item>
          <div class="modal-tip">
            同意：推进到下一节点。驳回：终止流程（终态）。退回：流程回到所选节点并重建待办，不终止流程。
          </div>
        </a-form>
      </a-modal>

      <!-- ═══ 转办弹窗（本系统只有「转办」，没有「委托」：任务所有权转移给目标用户） ═══ -->
      <a-modal
        v-model:open="transferVisible"
        title="转办任务"
        :width="520"
        :confirm-loading="transferSubmitting"
        @ok="handleConfirmTransfer"
        @cancel="handleCancelTransfer"
      >
        <a-form
          :model="transferForm"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item label="待办任务">
            <span>{{ transferTaskName || '-' }}</span>
          </a-form-item>
          <a-form-item
            label="目标用户"
            required
          >
            <a-select
              v-model:value="transferForm.targetUser"
              size="small"
              placeholder="请选择用户"
              show-search
              :filter-option="filterUserOption"
              :loading="userLoading"
            >
              <a-select-option
                v-for="u in userList"
                :key="u.id"
                :value="u.id"
              >
                {{ u.nickname || u.username }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="transferForm.comment"
              size="small"
              :rows="4"
              :maxlength="200"
              placeholder="请输入备注"
            />
          </a-form-item>
          <div class="modal-tip">
            转办后任务所有权转移给目标用户，本人在「已办」中可看到该条「转交」记录。
          </div>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, FireOutlined, InboxOutlined, ReloadOutlined, ScheduleOutlined,
  SearchOutlined, SettingOutlined, SyncOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { useRowDblclick } from '@/composables/useRowDblclick'
import { userApi } from '@/api/user'
import {
  TASK_ACTION_TEXT_MAP, TASK_STATUS_TEXT_MAP, workflowTaskApi,
  type WorkflowTaskStat
} from '@/api/workflow'

defineOptions({ name: 'WorkflowTaskManagement' })

const route = useRoute()

// ═══════════════════════════════════════════════════════════════
// 一、路由 → 默认 Tab（P0：803「我的待办」进 todo，804「我的已办」进 done）
// ═══════════════════════════════════════════════════════════════
//
// 803 / 804 的 DB `component` 分别是 `views/workflow/task-management` 与
// `views/workflow/task-management.vue`，归一化（去 `views/` 前缀与 `.vue` 后缀）后**同一个 key**
// → 落到**同一个组件实例**。组件此前完全不读路由、Tab 初值硬编码 `ref('todo')`
// → 从「我的已办」菜单进入看到的却是待办数据（用户不手点第二个 Tab 永远看不到已办）。
//
// 路由名取菜单 `menu_code`（`routeName` 为空时 dynamicRoutes.ts 用 `menuCode` 作 name，devdb 实测
// `sys_menu.route_name` 为 NULL）→ 803 = `my-task`，804 = `my-done`；path 兜底。
type TabKey = 'todo' | 'done'

const DONE_ROUTE_NAMES = ['my-done', 'MyDone']
const DONE_ROUTE_PATH = '/workflow/done'

function isDoneEntry(): boolean {
  const name = String((route.name as string) || '')
  const path = String(route.path || '')
  return DONE_ROUTE_NAMES.includes(name) || path === DONE_ROUTE_PATH || path.endsWith(DONE_ROUTE_PATH)
}

const TABS = [
  { key: 'todo', label: '待办任务' },
  { key: 'done', label: '已办任务' }
]

const activeTab = ref<TabKey>(isDoneEntry() ? 'done' : 'todo')

/** 页标题随菜单/标签语义变化（历史缺陷：两个菜单页头都是「任务管理」，连页面都分不出） */
const pageTitle = computed(() => (activeTab.value === 'todo' ? '我的待办' : '我的已办'))

// ═══════════════════════════════════════════════════════════════
// 二、横向自适应查询网格
// ═══════════════════════════════════════════════════════════════
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

const PRIORITY_OPTIONS = [
  { label: '高', value: 'high' },
  { label: '中', value: 'medium' },
  { label: '低', value: 'low' }
]

const RETURN_NODE_OPTIONS = [
  { label: '发起人', value: 'start' },
  { label: '上一节点', value: 'previous' }
]

// 两个 Tab 各自独立一套查询条件（切 Tab 不串值）
const todoQuery = reactive({
  taskName: '',
  processName: '',
  priority: undefined as string | undefined,
  dateRange: [] as string[]
})
const doneQuery = reactive({
  taskName: '',
  processName: '',
  priority: undefined as string | undefined,
  dateRange: [] as string[]
})

const activeQuery = computed(() => (activeTab.value === 'todo' ? todoQuery : doneQuery))

const hasActiveFilters = computed(() => {
  const q = activeQuery.value
  return Boolean(q.taskName || q.processName || q.priority || (q.dateRange && q.dateRange.length > 0))
})

const emptyText = computed(() => (activeTab.value === 'todo' ? '暂无待办任务' : '暂无已办记录'))

// ═══════════════════════════════════════════════════════════════
// 三、页面配置（查询条件 + 功能按钮，逐 Tab 独立存储）
// ═══════════════════════════════════════════════════════════════
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskName', label: '任务名称', visible: true },
  { key: 'processName', label: '流程名称', visible: true },
  { key: 'priority', label: '优先级', visible: true },
  { key: 'createTime', label: '创建时间', visible: true }
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true }
]

const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面把「当前配置」传给 query-fields-config，必须另给出厂值） */
const defaultQueryFields: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const defaultFunctionButtons: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))

const pageConfigStorageKey = computed(() => `workflow-task-page-config-${activeTab.value}`)

function loadPageConfig() {
  const key = pageConfigStorageKey.value
  // 先复位到出厂值：切 Tab 后若目标 Tab 没有存储，不能沿用上一个 Tab 的显隐
  queryFieldsConfig.value = QUERY_FIELDS.map(f => ({ ...f }))
  functionButtonsConfig.value = FUNCTION_BUTTONS.map(f => ({ ...f }))
  try {
    const raw = localStorage.getItem(key)
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
  } catch { /* 存储损坏时用出厂值 */ }
}

function handlePageConfigChange(config: any) {
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
  localStorage.setItem(pageConfigStorageKey.value, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || []
  }))
}

const fieldVisible = (key: string) => queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
const btnEnabled = (key: string) => functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false

// ═══════════════════════════════════════════════════════════════
// 四、列定义（两个 Tab 各自一套；插槽列必须 type: 'slot'）
// ═══════════════════════════════════════════════════════════════
// 待办列：任务ID / 任务名称 / 流程名称 / 优先级 / 处理人 / 创建时间 / 截止时间 / 状态 / 操作
const TODO_COLUMNS: any[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 200, fixed: 'right', slotName: 'actionCell' },
  { title: '任务ID', field: 'taskId', key: 'taskId', width: 190 },
  { title: '任务名称', field: 'taskName', key: 'taskName', width: 180, type: 'slot', slotName: 'taskNameCell' },
  { title: '流程名称', field: 'processName', key: 'processName', width: 160 },
  { title: '优先级', field: 'priority', key: 'priority', width: 90, type: 'slot', slotName: 'priorityCell' },
  { title: '处理人', field: 'assignee', key: 'assignee', width: 110 },
  { title: '任务状态', field: 'status', key: 'status', width: 100, type: 'slot', slotName: 'statusCell' },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 170 },
  { title: '截止时间', field: 'dueTime', key: 'dueTime', width: 170 }
]

// 已办列：在待办列基础上补「审批结果 / 处理时间 / 审批意见」（后端已返回，此前三列全缺）
const DONE_COLUMNS: any[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 110, fixed: 'right', slotName: 'actionCell' },
  { title: '任务ID', field: 'taskId', key: 'taskId', width: 190 },
  { title: '任务名称', field: 'taskName', key: 'taskName', width: 180, type: 'slot', slotName: 'taskNameCell' },
  { title: '流程名称', field: 'processName', key: 'processName', width: 160 },
  { title: '审批结果', field: 'action', key: 'actionResult', width: 100, type: 'slot', slotName: 'actionResultCell' },
  { title: '审批意见', field: 'comment', key: 'comment', width: 240, type: 'slot', slotName: 'commentCell' },
  { title: '处理时间', field: 'handleTime', key: 'handleTime', width: 170 },
  { title: '任务状态', field: 'status', key: 'status', width: 100, type: 'slot', slotName: 'statusCell' },
  { title: '处理人', field: 'assignee', key: 'assignee', width: 110 },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 170 },
  { title: '截止时间', field: 'dueTime', key: 'dueTime', width: 170, defaultHidden: true },
  { title: '优先级', field: 'priority', key: 'priority', width: 90, type: 'slot', slotName: 'priorityCell', defaultHidden: true }
]

const columns = computed(() => (activeTab.value === 'todo' ? TODO_COLUMNS : DONE_COLUMNS))

// 逐 Tab 独立的列配置 storage-key（切 Tab 换 key，BillDetailTable 已 watch storageKey 并重新加载）
const tableStorageKey = computed(() => `workflow-task-columns-${activeTab.value}`)

// ═══════════════════════════════════════════════════════════════
// 五、数据加载（列表 + 统计卡）
// ═══════════════════════════════════════════════════════════════
const loading = ref(false)
const tableData = ref<any[]>([])
const hasError = ref(false)
const statData = ref<WorkflowTaskStat | null>(null)

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

/** 构造与后端 pageTasks / statTasks 签名一一对应的查询参数（两个端点必须同参数，保证口径一致） */
function buildParams() {
  const q = activeQuery.value
  return {
    tab: activeTab.value,
    taskName: q.taskName || undefined,
    processName: q.processName || undefined,
    priority: q.priority || undefined,
    startDate: q.dateRange?.[0] || undefined,
    endDate: q.dateRange?.[1] || undefined
  }
}

async function fetchList() {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await workflowTaskApi.page({
      ...buildParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    // 注意：axios 拦截器已拆包（utils/request.ts 返回 ApiResponse.data），记录直接挂在 res 上
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[工作流] 获取任务列表失败', error)
    message.error(error?.response?.data?.message || '获取任务列表失败，请稍后重试')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  }
}

/** 统计卡：服务端全量真聚合（不再用「当页条数」冒充全量，已办 Tab 也不再用待办口径硬套） */
async function fetchStat() {
  try {
    const res: any = await workflowTaskApi.stat({ ...buildParams() })
    statData.value = res || null
  } catch (error) {
    console.warn('[工作流] 获取任务统计失败', error)
    statData.value = null
  }
}

function loadAll() {
  loadPageConfig()
  fetchList()
  fetchStat()
}

// 统计卡（逐 Tab 独立口径；数值全部来自 /task/stat）
const statCards = computed(() => {
  const s = statData.value
  if (activeTab.value === 'todo') {
    return [
      { key: 'total', label: '待办任务', value: s?.total ?? 0, cls: 'stat-total', icon: ScheduleOutlined, tip: '当前筛选条件下指派给我的未处理任务总数' },
      {
        key: 'high', label: '高优先级', value: s?.priorityHigh ?? 0, cls: 'stat-high', icon: FireOutlined,
        tip: '按 workflow_task.priority 真实聚合；本系统暂无优先级录入来源，未设置的行不计入'
      },
      { key: 'overdue', label: '超时任务', value: s?.overdue ?? 0, cls: 'stat-overdue', icon: WarningOutlined, tip: '截止时间（创建时间 + 节点时限）已过期的任务数' },
      { key: 'today', label: '今日新增', value: s?.today ?? 0, cls: 'stat-todo', icon: ClockCircleOutlined, tip: '今天创建的任务数' }
    ]
  }
  return [
    { key: 'total', label: '已办任务', value: s?.total ?? 0, cls: 'stat-total', icon: ScheduleOutlined, tip: '当前筛选条件下我已处理 + 已转交的任务总数' },
    { key: 'approve', label: '审批通过', value: s?.actionApprove ?? 0, cls: 'stat-todo', icon: ClockCircleOutlined, tip: '动作 = 同意 的条数' },
    { key: 'reject', label: '已驳回', value: s?.actionReject ?? 0, cls: 'stat-high', icon: FireOutlined, tip: '动作 = 驳回（流程终止）的条数' },
    { key: 'transfer', label: '已转交', value: s?.actionTransfer ?? 0, cls: 'stat-overdue', icon: WarningOutlined, tip: '动作 = 转交 的条数' }
  ]
})

// ═══════════════════════════════════════════════════════════════
// 六、查询 / 重置 / 分页 / Tab 切换
// ═══════════════════════════════════════════════════════════════
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const AUTO_REFRESH_SECONDS = 30

function handleQuery() {
  pagination.current = 1
  loadAll()
}

function handleSearch() {
  handleQuery()
}

function handleReset() {
  const q = activeQuery.value
  q.taskName = ''
  q.processName = ''
  q.priority = undefined
  q.dateRange = []
  handleQuery()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchList()
}

function handleTabChange(key: string) {
  const next = (key === 'done' ? 'done' : 'todo') as TabKey
  if (next === activeTab.value) return
  activeTab.value = next
  pagination.current = 1
  // 切 Tab：换 storage-key（列配置 + 页面配置）并重新加载
  loadAll()
  autoRefreshCountdown.value = AUTO_REFRESH_SECONDS
}

// 同一组件实例在两个菜单间跳转时（Vue Router 复用组件），把 Tab 重置为该菜单语义对应的视图
watch(() => route.path, () => {
  const next: TabKey = isDoneEntry() ? 'done' : 'todo'
  if (next !== activeTab.value) {
    activeTab.value = next
    pagination.current = 1
    loadAll()
  }
})

// ═══════════════════════════════════════════════════════════════
// 七、详情抽屉
// ═══════════════════════════════════════════════════════════════
const detailVisible = ref(false)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

const prettyBusinessData = computed(() => {
  const raw = detailData.value?.businessData
  if (!raw) return '无'
  try {
    return JSON.stringify(typeof raw === 'string' ? JSON.parse(raw) : raw, null, 2)
  } catch {
    // 业务数据可能不是合法 JSON（本系统允许任意文本）→ 原文展示，不要让页面报错
    return String(raw)
  }
})

function handleViewDetail(record: any) {
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.taskId)
}

// 双击行打开只读详情 —— 页面侧自行实现（不依赖共享表格组件派发事件）
// 待办/已办双 Tab 共用同一张表：tableData 始终是当前 Tab 的行；行标识 = row-key 指定的 taskId
const tableWrap = ref<HTMLElement | null>(null)
useRowDblclick(tableWrap, () => tableData.value, handleViewDetail, 'taskId')

async function fetchDetail(taskId: string | undefined) {
  if (!taskId) return
  detailLoading.value = true
  detailError.value = null
  try {
    const res: any = await workflowTaskApi.detail(taskId)
    detailData.value = res || null
  } catch (err: any) {
    console.warn('[工作流] 获取任务详情失败', err)
    detailError.value = err?.response?.data?.message || err?.message || '获取任务详情失败'
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

// ═══════════════════════════════════════════════════════════════
// 八、审批（同意 / 驳回 / 退回）
// ═══════════════════════════════════════════════════════════════
const approveVisible = ref(false)
const approveSubmitting = ref(false)
const approveTaskId = ref<string | null>(null)
const approveTaskName = ref('')
const approveForm = reactive({
  approval: 'approve' as 'approve' | 'reject' | 'return',
  returnNode: 'start' as 'start' | 'previous',
  comment: ''
})

function handleApprove(record: any) {
  approveTaskId.value = record.taskId
  approveTaskName.value = record.taskName || ''
  approveForm.approval = 'approve'
  approveForm.returnNode = 'start'
  approveForm.comment = ''
  approveVisible.value = true
}

function handleCancelApprove() {
  approveTaskId.value = null
}

async function handleConfirmApprove() {
  const taskId = approveTaskId.value
  if (!taskId) return
  approveSubmitting.value = true
  try {
    const comment = approveForm.comment
    const res: any = approveForm.approval === 'approve'
      ? await workflowTaskApi.approve(taskId, comment)
      : approveForm.approval === 'reject'
        ? await workflowTaskApi.reject(taskId, comment)
        : await workflowTaskApi.returnTask(taskId, approveForm.returnNode, comment)
    if (res?.success === false) {
      // 后端 HTTP 恒 200，成败在 body.success —— 必须展示后端原话，不能吞成笼统文案
      message.error(res?.message || '审批失败')
      return
    }
    message.success(res?.message || '审批成功')
    approveVisible.value = false
    approveTaskId.value = null
    loadAll()
  } catch (error: any) {
    console.warn('[工作流] 审批失败', error)
    message.error(error?.response?.data?.message || '审批失败')
  } finally {
    approveSubmitting.value = false
  }
}

// ═══════════════════════════════════════════════════════════════
// 九、转办（唯一真实支持的转交语义）
// ═══════════════════════════════════════════════════════════════
const transferVisible = ref(false)
const transferSubmitting = ref(false)
const transferTaskId = ref<string | null>(null)
const transferTaskName = ref('')
const transferForm = reactive({
  targetUser: undefined as string | undefined,
  comment: ''
})

// 用户下拉（仅转办弹窗需要；此处按需加载，不再进页面就无条件拉 1000 条）
const userList = ref<{ id: string; username: string; nickname: string }[]>([])
const userLoading = ref(false)
let userListLoaded = false

async function fetchUserList() {
  if (userListLoaded) return
  userLoading.value = true
  try {
    const res: any = await userApi.getList({ pageSize: 1000 })
    // 拦截器拆包后可能是裸数组，也可能被包成 { records } —— 两种都兼容
    const rows = Array.isArray(res) ? res : (res?.records || res?.data || [])
    userList.value = (rows || []).map((u: any) => ({ id: String(u.id), username: u.username, nickname: u.nickname }))
    userListLoaded = true
  } catch (err) {
    userList.value = []
    console.warn('[工作流] 加载用户列表失败', err)
  } finally {
    userLoading.value = false
  }
}

const filterUserOption = (input: string, option: any) => {
  const label = option?.children?.toString() || ''
  return label.toLowerCase().includes(input.toLowerCase())
}

function handleTransfer(record: any) {
  transferTaskId.value = record.taskId
  transferTaskName.value = record.taskName || ''
  transferForm.targetUser = undefined
  transferForm.comment = ''
  transferVisible.value = true
  fetchUserList()
}

function handleCancelTransfer() {
  transferTaskId.value = null
}

async function handleConfirmTransfer() {
  const targetUser = transferForm.targetUser
  if (!targetUser) {
    message.warning('请选择目标用户')
    return
  }
  const taskId = transferTaskId.value
  if (!taskId) return
  transferSubmitting.value = true
  try {
    const res: any = await workflowTaskApi.transfer(taskId, targetUser, transferForm.comment)
    if (res?.success === false) {
      message.error(res?.message || '转办失败')
      return
    }
    message.success(res?.message || '转办成功')
    transferVisible.value = false
    transferTaskId.value = null
    loadAll()
  } catch (error: any) {
    console.warn('[工作流] 转办失败', error)
    message.error(error?.response?.data?.message || '转办失败')
  } finally {
    transferSubmitting.value = false
  }
}

// ═══════════════════════════════════════════════════════════════
// 十、展示工具
// ═══════════════════════════════════════════════════════════════
const getPriorityColor = (priority: string) => ({ high: 'red', medium: 'orange', low: 'default' } as Record<string, string>)[priority] || 'default'
const getPriorityLabel = (priority: string) => ({ high: '高', medium: '中', low: '低' } as Record<string, string>)[priority] || priority

const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

function handleError(err: Error) {
  console.warn('[工作流] 任务管理页面错误', err)
  hasError.value = true
}

function handleKeydown(e: KeyboardEvent) {
  // F5 / Ctrl+R 只刷新列表数据，不整页刷新
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', handleQuery)()
  }
}

// ═══════════════════════════════════════════════════════════════
// 十一、生命周期
// ═══════════════════════════════════════════════════════════════
onMounted(() => {
  loadAll()
  autoRefreshCountdown.value = AUTO_REFRESH_SECONDS
  refreshTimer = setInterval(() => {
    fetchList()
    fetchStat()
    autoRefreshCountdown.value = AUTO_REFRESH_SECONDS
  }, AUTO_REFRESH_SECONDS * 1000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery })
</script>

<style scoped>
/* 查询区：横向自适应网格（禁止纵向单列） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-range { grid-column: span 2; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }

/* 数据区：统计卡 + 表格（父容器必须是 flex 纵向容器，否则表格高度塌陷为 0） */
.task-body { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 12px 16px 16px; }
.table-area { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }

.page-title { font-size: 14px; font-weight: 600; color: #303133; }
.toolbar-tip { font-size: 12px; color: #909399; }

/* 统计卡片（数值全部来自 /workflow/task/stat 的服务端真聚合） */
.stat-cards { display: flex; gap: 12px; margin-bottom: 12px; flex-shrink: 0; }
.stat-card {
  flex: 1; display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-radius: 8px; min-width: 0;
}
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-todo { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-high { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-overdue { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-card-value { font-size: 20px; font-weight: 600; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; color: #333; }
.stat-card-label { font-size: 12px; color: #666; margin-top: 4px; }
.stat-card-icon { font-size: 26px; color: rgba(0, 0, 0, 0.15); }

/* 列表状态条（加载失败 / 空结果） */
.table-status {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  margin-bottom: 8px; padding: 6px 10px; border-radius: 4px;
  font-size: 12px; color: #8c8c8c; background: #fafafa;
}
.table-status-error { color: #d46b08; background: #fff7e6; }
.table-status-action { color: #1890ff; cursor: pointer; }
.table-status-action:hover { text-decoration: underline; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.cell-ellipsis { display: inline-block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }

.modal-tip { color: #8c8c8c; font-size: 12px; line-height: 1.7; padding-left: 8px; }

pre { background: #f5f5f5; padding: 10px; border-radius: 4px; font-size: 12px; max-height: 240px; overflow: auto; margin: 0; }

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* ── vxe-table 表头 2px 边框 ── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }
</style>
