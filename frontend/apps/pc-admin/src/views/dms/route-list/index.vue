<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        配送路线单（配送 → 配送路线 → 配送路线单，菜单 80700 / dms:route-list）
        · 多点配送**执行单**台账：谁跑、跑到哪、开始/完成/取消 + 多点签收
        · 红线：与「资料 → 配送管理 → 线路」（线路档案主数据 erp_route）严格区分，二者不重复
        · 骨架：CategoryListLayout（无分类树）+ BillTableList（表头齿轮列配置）+ PageConfigPanel
        · 工具栏：新增路线 / 批量开始配送 / 批量完成 / 批量取消 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        ⚠️ 对标状态：ql361 无「配送」模块，本页为本系统自主建模，不臆造对标字段。
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
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增路线
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchStart')"
              size="small"
              :disabled="selectedIds.length === 0"
              @click="handleBatch('start')"
            >
              批量开始配送
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchComplete')"
              size="small"
              :disabled="selectedIds.length === 0"
              @click="handleBatch('complete')"
            >
              批量完成
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchCancel')"
              size="small"
              :disabled="selectedIds.length === 0"
              @click="handleBatch('cancel')"
            >
              批量取消
            </a-button>
            <a-button
              v-if="isButtonEnabled('collect')"
              size="small"
              @click="collectVisible = true"
            >
              <ClusterOutlined /> 围栏归集
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div v-if="isQueryVisible('keyword')" class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="路线编号/线路/配送员/车牌/起终点"
                  size="small"
                  style="width: 240px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('routeId')" class="search-item">
                <span class="search-label">配送线路</span>
                <a-select
                  v-model:value="searchForm.routeId"
                  placeholder="全部线路"
                  size="small"
                  style="width: 180px"
                  show-search
                  allow-clear
                  :options="routeOptions"
                  :filter-option="filterOption"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('deliveryPersonId')" class="search-item">
                <span class="search-label">配送员</span>
                <a-select
                  v-model:value="searchForm.deliveryPersonId"
                  placeholder="全部配送员"
                  size="small"
                  style="width: 170px"
                  show-search
                  allow-clear
                  :options="riderOptions"
                  :filter-option="filterOption"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('vehicleId')" class="search-item">
                <span class="search-label">车辆</span>
                <a-select
                  v-model:value="searchForm.vehicleId"
                  placeholder="全部车辆"
                  size="small"
                  style="width: 150px"
                  show-search
                  allow-clear
                  :options="vehicleOptions"
                  :filter-option="filterOption"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('status')" class="search-item">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  mode="multiple"
                  placeholder="全部状态"
                  size="small"
                  style="min-width: 170px"
                  allow-clear
                  :max-tag-count="1"
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
            </div>
            <div class="search-row">
              <div v-if="isQueryVisible('planDate')" class="search-item">
                <span class="search-label">计划日期</span>
                <a-range-picker
                  v-model:value="searchForm.planDate"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('createTime')" class="search-item">
                <span class="search-label">创建时间</span>
                <a-range-picker
                  v-model:value="searchForm.createTime"
                  size="small"
                  style="width: 300px"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('showCancelled')" class="search-item">
                <a-checkbox
                  v-model:checked="searchForm.showCancelled"
                  @change="handleSearch"
                >
                  显示已取消
                </a-checkbox>
              </div>
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
              :show-batch-delete="false"
              :selectable="true"
              storage-key="dms-route-list-columns"
              global-config-key="dms-route-list-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
            >
              <template #codeCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleDetail(record)"
                >{{ record.routeCode }}</a>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ record.statusText || STATUS_MAP[record.status]?.label || record.status }}
                </a-tag>
              </template>
              <template #routeTypeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.routeTypeText || '-' }}</span>
              </template>
              <template #distanceCell="{ record }">
                <span v-if="!record.__ghost">{{ record.totalDistance != null ? `${record.totalDistance} km` : '-' }}</span>
              </template>
              <template #durationCell="{ record }">
                <span v-if="!record.__ghost">{{ record.totalDuration != null ? `${record.totalDuration} 分钟` : '-' }}</span>
              </template>
              <template #actualDurationCell="{ record }">
                <span v-if="!record.__ghost">{{ record.actualDuration != null ? `${record.actualDuration} 分钟` : '-' }}</span>
              </template>
              <template #progressCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="{ 'progress-done': isAllDelivered(record) }"
                >
                  {{ record.progress || `0 / ${record.totalPoints ?? 0}` }}
                </span>
              </template>
              <template #timeCell="{ record }">
                <span v-if="!record.__ghost">{{ formatTime(record.createTime) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleDetail(record)"
                  >
                    详情
                  </a-button>
                  <a-button
                    v-if="canStart(record)"
                    type="link"
                    size="small"
                    @click="handleStart(record)"
                  >
                    开始配送
                  </a-button>
                  <a-button
                    v-if="record.status === 'IN_PROGRESS'"
                    type="link"
                    size="small"
                    @click="handleComplete(record)"
                  >
                    完成
                  </a-button>
                  <a-dropdown :trigger="['click']">
                    <a-button type="link" size="small">
                      更多 <DownOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(info: any) => handleMore(info.key, record)">
                        <a-menu-item v-if="canEdit(record)" key="edit">
                          修改
                        </a-menu-item>
                        <a-menu-item v-if="canCancel(record)" key="cancel">
                          <span class="danger-text">取消</span>
                        </a-menu-item>
                        <a-menu-item key="print">
                          打印路线单
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
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

      <!-- ═══ 新增 / 修改 ═══ -->
      <RouteFormModal
        v-model:open="formVisible"
        :record-id="editingId"
        @success="fetchList"
      />

      <!-- ═══ 点位明细 / 多点签收 ═══ -->
      <RoutePointDrawer
        v-model:open="drawerVisible"
        :route-id="activeRouteId"
        @changed="fetchList"
        @print="handlePrint"
      />

      <!-- ═══ 配送需求归集（围栏自动 + 手动添加围栏外单据） ═══ -->
      <RouteCollectModal
        v-model:open="collectVisible"
        @success="fetchList"
      />

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-route-list-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="dms-route-list"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
  DownOutlined,
  ClusterOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import RouteFormModal from './components/RouteFormModal.vue'
import RoutePointDrawer from './components/RoutePointDrawer.vue'
import RouteCollectModal from './components/RouteCollectModal.vue'
import { deliveryRouteApi, type DeliveryRouteQuery } from '@/api/dms/route'
import { mdRouteApi } from '@/api/md'
import { riderApi } from '@/api/dms/rider'
import { vehicleApi } from '@/api/dms/vehicle'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'DmsRouteList' })

// ═══ 状态枚举（与后端 RouteStatus 一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PLANNING: { label: '规划中', color: 'default' },
  READY: { label: '待出发', color: 'orange' },
  IN_PROGRESS: { label: '配送中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'green' },
  CANCELLED: { label: '已取消', color: 'red' },
}
const STATUS_OPTIONS = Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value }))

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const selectedIds = ref<any[]>([])

const searchForm = reactive({
  keyword: '',
  routeId: undefined as number | undefined,
  deliveryPersonId: undefined as string | undefined,
  vehicleId: undefined as number | undefined,
  status: [] as string[],
  planDate: null as any,
  createTime: null as any,
  showCancelled: false,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })

// ═══ 数据表列（18 默认可见 + 7 隐藏） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 210, fixed: 'left' },
  { title: '路线编号', key: 'routeCode', type: 'slot', slotName: 'codeCell', width: 180, sortable: true },
  { title: '线路名称', key: 'routeName', width: 160 },
  { title: '线路类型', key: 'routeTypeText', type: 'slot', slotName: 'routeTypeCell', width: 90 },
  { title: '配送员', key: 'deliveryPersonName', width: 110 },
  { title: '车辆', key: 'vehicleNo', width: 110 },
  { title: '配送围栏', key: 'fenceName', width: 140, defaultHidden: true },
  { title: '计划配送日期', key: 'planDate', width: 120, sortable: true },
  { title: '总点位', key: 'totalPoints', width: 80, align: 'center', sortable: true },
  { title: '已送达', key: 'completedPoints', width: 80, align: 'center', sortable: true },
  { title: '配送失败', key: 'failedPoints', width: 90, align: 'center' },
  { title: '完成进度', key: 'progress', type: 'slot', slotName: 'progressCell', width: 100, align: 'center' },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 100, sortable: true },
  { title: '开始时间', key: 'startTime', width: 165, sortable: true },
  { title: '完成时间', key: 'completeTime', width: 165, sortable: true },
  { title: '总里程', key: 'totalDistance', type: 'slot', slotName: 'distanceCell', width: 100, align: 'right', sortable: true },
  { title: '预计时长', key: 'totalDuration', type: 'slot', slotName: 'durationCell', width: 100, align: 'right' },
  { title: '创建时间', key: 'createTime', type: 'slot', slotName: 'timeCell', width: 165, sortable: true },
  // ── 默认隐藏 ──
  { title: '实际时长', key: 'actualDuration', type: 'slot', slotName: 'actualDurationCell', width: 110, defaultHidden: true },
  { title: '起点', key: 'startPoint', width: 200, ellipsis: true, defaultHidden: true },
  { title: '终点', key: 'endPoint', width: 200, ellipsis: true, defaultHidden: true },
  { title: '取消时间', key: 'cancelTime', width: 165, defaultHidden: true },
  { title: '取消原因', key: 'cancelReason', width: 180, ellipsis: true, defaultHidden: true },
  { title: '创建人', key: 'createByName', width: 110, defaultHidden: true },
  { title: '备注', key: 'remark', width: 200, ellipsis: true, defaultHidden: true },
]

// ═══ 下拉选项（线路档案 / 配送员 / 车辆 —— 一律选择器，禁止手输 ID） ═══
const routeOptions = ref<any[]>([])
const riderOptions = ref<any[]>([])
const vehicleOptions = ref<any[]>([])

function filterOption(input: string, option: any) {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function loadRouteOptions() {
  try {
    const res: any = await mdRouteApi.options()
    const list: any[] = Array.isArray(res) ? res : res?.records || []
    routeOptions.value = list.map(r => ({
      label: `${r.routeCode || ''} ${r.routeName || ''}`.trim(),
      value: r.id,
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载线路下拉失败', e)
  }
}

async function loadRiderOptions() {
  try {
    const res: any = await riderApi.page({ pageNum: 1, pageSize: 500 })
    riderOptions.value = (res?.records || []).map((r: any) => ({
      label: r.realName ? `${r.realName}${r.phone ? ' ' + r.phone : ''}` : `#${r.id}`,
      value: String(r.id),
    }))
  } catch (e) {
    console.warn('[配送路线单] 加载配送员下拉失败', e)
  }
}

async function loadVehicleOptions() {
  try {
    const res: any = await vehicleApi.page({ pageNum: 1, pageSize: 500 })
    vehicleOptions.value = (res?.records || []).map((v: any) => ({ label: v.plateNo || `#${v.id}`, value: v.id }))
  } catch (e) {
    console.warn('[配送路线单] 加载车辆下拉失败', e)
  }
}

// ═══ 查询参数 ═══
function buildParams(): DeliveryRouteQuery {
  const p: DeliveryRouteQuery = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) p.keyword = searchForm.keyword
  if (searchForm.routeId != null) p.routeId = searchForm.routeId
  if (searchForm.deliveryPersonId != null) p.deliveryPersonId = searchForm.deliveryPersonId
  if (searchForm.vehicleId != null) p.vehicleId = searchForm.vehicleId
  if (searchForm.status.length > 0) p.status = searchForm.status.join(',')
  if (searchForm.planDate?.[0]) {
    p.planDateStart = searchForm.planDate[0]
    p.planDateEnd = searchForm.planDate[1]
  }
  if (searchForm.createTime?.[0]) {
    p.createTimeStart = searchForm.createTime[0]
    p.createTimeEnd = searchForm.createTime[1]
  }
  p.showCancelled = searchForm.showCancelled ? 1 : 0
  if (sortState.field) {
    p.sortField = sortState.field
    p.sortOrder = sortState.order
  }
  return p
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await deliveryRouteApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedIds.value = []
  } catch (e: any) {
    console.error('[配送路线单] 加载列表失败', e)
    message.error(e?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSelectionChange(_rows: any[], ids: any[]) {
  selectedIds.value = ids || []
}

function handleSortChange(key: string | null, order: string | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

function isAllDelivered(record: any): boolean {
  return Number(record.totalPoints) > 0 && Number(record.completedPoints) >= Number(record.totalPoints)
}

function canStart(record: any): boolean {
  return record.status === 'PLANNING' || record.status === 'READY'
}

function canEdit(record: any): boolean {
  return canStart(record)
}

function canCancel(record: any): boolean {
  return record.status !== 'COMPLETED' && record.status !== 'CANCELLED'
}

// ═══ 新增 / 修改 ═══
const formVisible = ref(false)
const editingId = ref<number | null>(null)
const collectVisible = ref(false)

function handleAdd() {
  editingId.value = null
  formVisible.value = true
}

// ═══ 详情 / 点位 ═══
const drawerVisible = ref(false)
const activeRouteId = ref<number | null>(null)

function handleDetail(record: any) {
  activeRouteId.value = record.id
  drawerVisible.value = true
}

// ═══ 行操作：开始 / 完成 / 更多 ═══
async function handleStart(record: any) {
  Modal.confirm({
    title: '确认开始配送',
    content: `确定开始配送路线「${record.routeCode}」吗？`,
    onOk: async () => {
      try {
        await deliveryRouteApi.start(record.id)
        message.success('已开始配送')
        fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '操作失败')
        throw e
      }
    },
  })
}

async function handleComplete(record: any) {
  const pending = Number(record.totalPoints || 0) - Number(record.completedPoints || 0) - Number(record.failedPoints || 0)
  const tip = pending > 0
    ? `该路线还有 ${pending} 个点位未处理，完成后将自动标记为「已跳过」。确定完成？`
    : `确定完成配送路线「${record.routeCode}」吗？`
  Modal.confirm({
    title: '确认完成配送',
    content: tip,
    onOk: async () => {
      try {
        await deliveryRouteApi.complete(record.id)
        message.success('已完成配送')
        fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '操作失败')
        throw e
      }
    },
  })
}

async function handleCancel(record: any) {
  let reason = ''
  Modal.confirm({
    title: '确认取消路线',
    content: () => h('div', [
      h('p', `确定取消配送路线「${record.routeCode}」吗？`),
      h('input', {
        class: 'ant-input',
        placeholder: '取消原因（选填）',
        onInput: (e: any) => { reason = e.target.value },
      }),
    ]),
    onOk: async () => {
      try {
        await deliveryRouteApi.cancel(record.id, reason || undefined)
        message.success('已取消')
        fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '操作失败')
        throw e
      }
    },
  })
}

function handleMore(key: string, record: any) {
  if (key === 'edit') {
    editingId.value = record.id
    formVisible.value = true
  } else if (key === 'cancel') {
    handleCancel(record)
  } else if (key === 'print') {
    printRoute(record)
  }
}

// ═══ 批量操作 ═══
function handleBatch(action: 'start' | 'complete' | 'cancel') {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择要操作的配送路线')
    return
  }
  const label = action === 'start' ? '开始配送' : action === 'complete' ? '完成' : '取消'
  Modal.confirm({
    title: `确认批量${label}`,
    content: `已选择 ${selectedIds.value.length} 条配送路线，确定批量${label}吗？`,
    onOk: async () => {
      try {
        const res: any = await deliveryRouteApi.batchStatus({ ids: selectedIds.value, action })
        const failure = Number(res?.failure || 0)
        if (failure > 0) {
          message.warning(`成功 ${res.success} 条，失败 ${failure} 条：${(res.errors || []).slice(0, 3).join('；')}`)
        } else {
          message.success(`已批量${label} ${res?.success ?? selectedIds.value.length} 条`)
        }
        fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '批量操作失败')
        throw e
      }
    },
  })
}

// ═══ 打印 ═══
// ═══ 打印（按单据打印） ═══
// 这页是**配送路线列表**（没有 formData）——打印的是「选中/打开的那条路线」：
// 后端已为 pageCode='dms-route-list' 登记装配器（路线主档 + 它的点位），页面只给路线主键。
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})

/** 打一条路线；没传就取勾选的第一条 */
function handlePrint(record?: any) {
  const id = record?.id ?? selectedIds.value?.[0]
  if (!id) {
    message.warning('请先勾选一条配送路线再打印')
    return
  }
  printData.value = { id }
  printDialogRef.value?.open?.()
}

// 行级「打印路线单」：与工具栏、详情抽屉三个入口走同一条路（都是「按单据打印」），
// 一律自己拼 HTML 之外交给 PrintDialog；装配器按路线带出主档 + 全部点位，无需抽屉另打一份。
function printRoute(record: any) {
  handlePrint(record)
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    delete params.sortField
    delete params.sortOrder
    const blob: any = await deliveryRouteApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `配送路线单_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'routeId', label: '配送线路', visible: true },
  { key: 'deliveryPersonId', label: '配送员', visible: true },
  { key: 'vehicleId', label: '车辆', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'planDate', label: '计划日期', visible: true },
  { key: 'createTime', label: '创建时间', visible: true },
  { key: 'showCancelled', label: '显示已取消', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增路线', enabled: true },
  { key: 'batchStart', label: '批量开始配送', enabled: true },
  { key: 'batchComplete', label: '批量完成', enabled: true },
  { key: 'batchCancel', label: '批量取消', enabled: true },
  { key: 'collect', label: '围栏归集', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[配送路线单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadRouteOptions()
  loadRiderOptions()
  loadVehicleOptions()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-row + .search-row { margin-top: 8px; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.progress-done { color: #52c41a; font-weight: 600; }
.danger-text { color: #ff4d4f; }

/* 橙色新增按钮（配送模块统一） */
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
