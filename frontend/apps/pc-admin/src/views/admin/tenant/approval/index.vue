<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        租户审批（系统 → 租户管理 → 租户审批，菜单 62002）
        · 平台控制台页面：平台运营方的租户准入工作台，ql361 无对标（ql361 只暴露租户级「设置」域）
          按用友「企业认证 / 企业新建」+ SAP BTP「租用（Subscribe）」建模
        · 单入口单视图列表页；数据源 sys_tenant（与《租户列表》62001 同表，但筛选口径不同）
        · 后端仅提供 GET /api/tenant-registration/pending（无入参、不分页）→ 查询与分页均在前端本地完成
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/租户审批开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：刷新 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('refresh')"
            size="small"
            :loading="loading"
            @click="handleRefresh"
          >
            <ReloadOutlined /> 刷新
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 ═══ -->
        <template #toolbar-right>
          <a-button
            size="small"
            title="页面配置"
            @click="showPageConfig = true"
          >
            <SettingOutlined />
          </a-button>
        </template>

        <!-- ═══ 查询区（仅「企业名称」，前端本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('tenantName')">
                <span class="search-label">企业名称</span>
                <a-input
                  v-model:value="searchForm.tenantName"
                  placeholder="请输入企业名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
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
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-tenant-approval-table-columns"
              global-config-key="system-tenant-approval-table-columns"
            >
              <!-- 注册时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!--
                状态：必须读 record.status 渲染。
                口径（TenantRegistrationService 注册写 0 / 通过写 1）：
                  0 = 待审批、1 = 已通过；未知值原样展示，便于发现上游状态扩展。
                改造前此处写死「待审批」，导致审批通过后仍显示待审批。
              -->
              <template #statusCell="{ record }">
                <a-tag
                  v-if="STATUS_MAP[record.status]"
                  :color="STATUS_MAP[record.status].color"
                >
                  {{ STATUS_MAP[record.status].label }}
                </a-tag>
                <span v-else>{{ record.status ?? '-' }}</span>
              </template>

              <!-- 操作列：通过（二次确认）/ 驳回（弹窗填原因） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-popconfirm
                    title="确定通过该租户的注册申请?通过后将启用租户及其管理员账号。"
                    ok-text="通过"
                    cancel-text="取消"
                    @confirm="handleApprove(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      :loading="approvingId === record.id"
                    >
                      通过
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    type="link"
                    danger
                    size="small"
                    @click="openReject(record)"
                  >
                    驳回
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（后端不分页 → 前端本地分页） ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 驳回原因弹窗（后端 Reject.reason 为 @NotBlank，前端不得再提交固定文案） ═══ -->
      <a-modal
        v-model:open="rejectVisible"
        title="驳回租户注册申请"
        width="520px"
        :confirm-loading="rejecting"
        ok-text="确定驳回"
        cancel-text="取消"
        @ok="handleRejectSubmit"
      >
        <a-alert
          type="warning"
          show-icon
          message="驳回为破坏性操作"
          description="驳回后该租户的注册申请将被删除，其管理员账号与租户关联一并移除，且不可恢复。"
          style="margin-bottom:12px"
        />
        <div class="reject-tenant">
          申请企业：{{ rejectRecord?.tenantName || '-' }}
        </div>
        <a-form
          ref="rejectFormRef"
          :model="rejectForm"
          :rules="rejectRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item
            label="驳回原因"
            name="reason"
          >
            <a-textarea
              v-model:value="rejectForm.reason"
              :rows="3"
              :maxlength="200"
              show-count
              placeholder="请填写驳回原因（必填，将作为审批意见返回给申请人）"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { tenantApprovalApi, type SysTenant } from '@/api/tenant'

defineOptions({ name: 'AdminTenantApproval' })

const PAGE_CONFIG_STORAGE_KEY = 'system-tenant-approval-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/**
 * 租户状态口径（TenantRegistrationService：注册写 0，审批通过写 1）。
 * 注意与《租户列表》62001 的差异：那边 0 = 禁用，本页 0 = 待审批（同表不同语义，见开发文档 §5.1）。
 */
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审批', color: 'orange' },
  1: { label: '已通过', color: 'green' },
}

// ═══ 状态 ═══
const loading = ref(false)
const approvingId = ref<number | null>(null)
const rejecting = ref(false)
/** 后端返回的全量待审数据（后端不分页，一次性返回全部） */
const allRecords = ref<SysTenant[]>([])
/** 当前页展示的数据（前端过滤 + 前端分页后的结果） */
const tableData = ref<SysTenant[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({ tenantName: '' })

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'tenantName', label: '企业名称', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 160, fixed: 'left' },
  { key: 'tenantName', title: '企业名称', type: 'input', width: 180 },
  { key: 'tenantCode', title: '企业编码', type: 'input', width: 150 },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 120 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 140 },
  // 注册时间需按 YYYY-MM-DD HH:mm 归一化展示 → 自定义渲染列必须 type:'slot'
  { key: 'createTime', title: '注册时间', type: 'slot', slotName: 'createTimeCell', width: 180 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
]

// ═══ 驳回弹窗 ═══
const rejectVisible = ref(false)
const rejectRecord = ref<SysTenant | null>(null)
const rejectFormRef = ref()
const rejectForm = reactive({ reason: '' })
const rejectRules: Record<string, any> = {
  reason: [
    { required: true, message: '请输入驳回原因', trigger: 'blur' },
    { max: 200, message: '驳回原因最长 200 个字符', trigger: 'blur' },
  ],
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载 ═══
/**
 * 后端 `GET /tenant-registration/pending` **无任何入参、也不分页** → 只能全量拉取。
 * 因此查询条件（企业名称）与分页都只能在前端本地做。
 * ⚠️ 响应解包：request 拦截器已对 wrapper 响应拆包（返回 data 字段，裸数组则原样返回），
 *    所以这里拿到的 res 已经是数组本身，不能再写 res.data（旧代码写 res.data 恒为 undefined）。
 */
async function fetchList() {
  loading.value = true
  try {
    const res: any = await tenantApprovalApi.getPending()
    allRecords.value = Array.isArray(res) ? res : (Array.isArray(res?.data) ? res.data : [])
    applyLocalView()
  } catch (error: any) {
    console.error('[租户审批] 加载待审批列表失败', error)
    message.error(error?.message || '加载待审批列表失败')
    allRecords.value = []
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 本地过滤 + 本地分页：后端无筛选参数与分页参数，全部在前端完成 */
function applyLocalView() {
  const keyword = searchForm.tenantName.trim().toLowerCase()
  const filtered = keyword
    ? allRecords.value.filter(r => String(r.tenantName || '').toLowerCase().includes(keyword))
    : allRecords.value

  pagination.total = filtered.length
  // 过滤后当前页可能已越界（例如在第 3 页时把结果筛到 1 页）
  const maxPage = Math.max(1, Math.ceil(filtered.length / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage

  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filtered.slice(start, start + pagination.pageSize)
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  applyLocalView()
}
function handleReset() {
  searchForm.tenantName = ''
  pagination.current = 1
  applyLocalView()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyLocalView()
}

/** 操作失败即清空本地数据：不保留可能已过期的列表，也不写死假数据兜底 */
function clearList() {
  allRecords.value = []
  tableData.value = []
  pagination.total = 0
}

// ═══ 审批通过 ═══
async function handleApprove(record: SysTenant) {
  approvingId.value = record.id
  try {
    await tenantApprovalApi.approve(record.id)
    message.success('已审批通过')
    await fetchList()
  } catch (error: any) {
    console.error('[租户审批] 审批通过失败', error)
    message.error(error?.message || '操作失败')
    clearList()
  } finally {
    approvingId.value = null
  }
}

// ═══ 审批驳回 ═══
function openReject(record: SysTenant) {
  rejectRecord.value = record
  rejectForm.reason = ''
  rejectVisible.value = true
  // 清掉上一次可能残留的校验红字
  rejectFormRef.value?.clearValidate?.()
}

async function handleRejectSubmit() {
  try {
    await rejectFormRef.value?.validate()
  } catch {
    return
  }
  const target = rejectRecord.value
  if (!target) {
    message.error('未选择要驳回的申请')
    return
  }
  rejecting.value = true
  try {
    await tenantApprovalApi.reject(target.id, rejectForm.reason.trim())
    message.success('已驳回')
    rejectVisible.value = false
    rejectRecord.value = null
    await fetchList()
  } catch (error: any) {
    console.error('[租户审批] 驳回失败', error)
    message.error(error?.message || '操作失败')
    clearList()
  } finally {
    rejecting.value = false
  }
}

function handleError(error: Error) {
  console.error('[租户审批] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchList)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.reject-tenant { margin-bottom: 12px; font-size: 13px; color: #333; }
</style>
