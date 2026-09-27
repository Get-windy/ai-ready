<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        买家申请管理（交易 → 商城 → 基础业务 → 买家申请管理，对标 ql361「商城 → 基础业务 → 买家申请管理」）
        · 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/买家申请管理开发文档.md
        · 页面定位：单入口单视图列表页（商城注册买家/企业客户入驻申请审核，无子 Tab）；对标无「页面配置」弹窗
        · 列配置：走 BillDetailTable 表头 rowNo 列齿轮（个人配置 / 全局配置），storage-key 持久化
        · 列数：可配置 17 列 = 对标 11 列（公司名称/联系人姓名/地址/手机号码/状态/备注/营业执照/绑定客户/申请时间/qq/微信，全默认显示）
                              + 本系统扩展 6 列（用户名/昵称/身份/来源/驳回原因/审核时间，全部默认隐藏）
          默认显示 11 列、默认隐藏 6 列；固定列（非列配置）：序号、操作
        · 工具栏：待审核统计 ｜ 刷新 / 打印(F8) / 导出（对标固定按钮无「新增」，申请由买家端提交）
        · 行级操作：通过 / 驳回（驳回需填原因）
        · 数据来源：shop_user（API /erp/mall/admin/user：page/approve/reject）
        · 后端字段：shop_user 已补 contactName/address/remark/businessLicense/qq/wechat 等 14 个字段（Flyway V11.361.3），
          上述列均有数据源；仍缺的后端能力：
          1) 绑定客户的客户名称连带查询（仅有 erpCustomerId/erpPartnerId/partyId，页面降级显示 '#<id>'）
          2) 查询区「日期范围」参数（/user/page 仅支持 keyword/auditStatus/status，缺 createTimeStart/End）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：审核统计（真实分页接口 total 计数，保留原 ARReportPage 统计口径） ═══ -->
        <template #toolbar-left>
          <a-space :size="12">
            <span class="stat-item">
              待审核 <b style="color: #fa8c16">{{ counts.pending }}</b>
            </span>
            <span class="stat-item">
              已通过 <b style="color: #52c41a">{{ counts.approved }}</b>
            </span>
            <span class="stat-item">
              已驳回 <b style="color: #f5222d">{{ counts.rejected }}</b>
            </span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项：筛选条件 + 日期范围 + 状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="公司名称/联系人/手机号"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
              <span class="search-label">日期范围</span>
              <a-range-picker
                v-model:value="searchForm.dateRange"
                size="small"
                style="width: 230px"
                value-format="YYYY-MM-DD"
                @change="handleSearch"
              />
              <span class="search-label">状态</span>
              <a-select
                v-model:value="searchForm.auditStatus"
                placeholder="全部"
                size="small"
                style="width: 130px"
                allow-clear
                :options="auditStatusOptions"
                @change="handleSearch"
              />
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="mall-buyer-apply-table-columns"
              global-config-key="mall-buyer-apply-table-columns"
            >
              <!-- 公司名称 -->
              <template #companyNameCell="{ record, column }">
                <span v-if="!record.__ghost">{{ record[column.key] || '-' }}</span>
              </template>

              <!-- 联系人姓名（shop_user.contact_name） -->
              <template #contactNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.contactName || '-' }}</span>
              </template>

              <!-- 地址（shop_user.address） -->
              <template #addressCell="{ record }">
                <span v-if="!record.__ghost">{{ record.address || '-' }}</span>
              </template>

              <!-- 手机号码 -->
              <template #phoneCell="{ record }">
                <span v-if="!record.__ghost">{{ record.phone || '-' }}</span>
              </template>

              <!-- 状态：待审核 / 已通过 / 已驳回 -->
              <template #auditStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="AUDIT_STATUS_MAP[record.auditStatus]?.color || 'default'"
                >
                  {{ AUDIT_STATUS_MAP[record.auditStatus]?.label || '未知' }}
                </a-tag>
              </template>

              <!-- 备注（shop_user.remark） -->
              <template #remarkCell="{ record }">
                <span v-if="!record.__ghost">{{ record.remark || '-' }}</span>
              </template>

              <!-- 营业执照：资质图片列（shop_user.business_license） -->
              <template #businessLicenseCell="{ record }">
                <template v-if="!record.__ghost">
                  <a-image
                    v-if="record.businessLicense"
                    :src="record.businessLicense"
                    :width="40"
                    :height="40"
                    style="object-fit: cover; border-radius: 4px"
                  />
                  <span v-else>-</span>
                </template>
              </template>

              <!-- 绑定客户（ERP 往来单位；当前仅有 id，客户名称待补） -->
              <template #customerNameCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.customerName || record.erpCustomerName || (record.erpCustomerId ? `#${record.erpCustomerId}` : '-') }}
                </span>
              </template>

              <!-- 申请时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- qq / 微信（shop_user.qq / shop_user.wechat） -->
              <template #qqCell="{ record }">
                <span v-if="!record.__ghost">{{ record.qq || '-' }}</span>
              </template>
              <template #wechatCell="{ record }">
                <span v-if="!record.__ghost">{{ record.wechat || '-' }}</span>
              </template>

              <!-- 本系统扩展列（默认隐藏） -->
              <template #usernameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.username || '-' }}</span>
              </template>
              <template #nicknameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.nickname || '-' }}</span>
              </template>
              <template #userTypeCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.userType === 'ENTERPRISE' ? '企业客户' : record.userType === 'MEMBER' ? '个人会员' : '-' }}
                </span>
              </template>
              <template #sourceCell="{ record }">
                <span v-if="!record.__ghost">{{ record.source || '-' }}</span>
              </template>
              <template #rejectReasonCell="{ record }">
                <span v-if="!record.__ghost">{{ record.rejectReason || '-' }}</span>
              </template>
              <template #auditTimeCell="{ record }">
                {{ fmtTime(record.auditTime) }}
              </template>

              <!-- 操作列（对标：通过 / 驳回） -->
              <template #actionCell="{ record, column }">
                <a-space
                  v-if="!record.__ghost && record.auditStatus === 0"
                  :size="0"
                  :data-col="column.key"
                >
                  <a-popconfirm
                    title="确认通过该买家的注册申请？"
                    ok-text="通过"
                    cancel-text="取消"
                    @confirm="handleApprove(record as ShopUser)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      style="color: #52c41a"
                    >
                      通过
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="openReject(record as ShopUser)"
                  >
                    驳回
                  </a-button>
                </a-space>
                <span v-else-if="!record.__ghost">-</span>
              </template>
            </BillDetailTable>
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

      <!-- ═══ 驳回原因弹窗 ═══ -->
      <a-modal
        v-model:open="rejectVisible"
        title="驳回买家申请"
        :confirm-loading="rejectLoading"
        @ok="handleRejectConfirm"
      >
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入驳回原因（必填，将展示给买家）"
          :rows="3"
          :maxlength="500"
        />
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="mall-buyer-apply"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { shopUserApi, type ShopUser } from '@/api/erp/mall'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MallBuyerApply' })

// ═══ 审核状态（与后端 ShopUser.auditStatus 一致：0待审核 1通过 2驳回） ═══
const AUDIT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已通过', color: 'green' },
  2: { label: '已驳回', color: 'red' }
}
const auditStatusOptions = Object.entries(AUDIT_STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<ShopUser[]>([])
const counts = ref({ pending: 0, approved: 0, rejected: 0 })

// ═══ 查询条件（日期范围为对标固定项；后端 /user/page 尚无 createTimeStart/End 参数） ═══
const searchForm = reactive({
  keyword: '' as string,
  dateRange: undefined as [string, string] | undefined,
  auditStatus: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 列定义（DetailColumnConfig[]）
 * 对标 11 列（全默认显示）：公司名称/联系人姓名/地址/手机号码/状态/备注/营业执照/绑定客户/申请时间/qq/微信
 * 本系统扩展 6 列（全部默认隐藏）：用户名/昵称/身份/来源/驳回原因/审核时间
 * 固定列（不进列配置面板）：rowNo（承载列配置齿轮）、action
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'companyName', title: '公司名称', type: 'slot', slotName: 'companyNameCell', width: 200, sortable: true },
  { key: 'contactName', title: '联系人姓名', type: 'slot', slotName: 'contactNameCell', width: 110 },
  { key: 'address', title: '地址', type: 'slot', slotName: 'addressCell', width: 220 },
  { key: 'phone', title: '手机号码', type: 'slot', slotName: 'phoneCell', width: 130 },
  { key: 'auditStatus', title: '状态', type: 'slot', slotName: 'auditStatusCell', width: 100 },
  { key: 'remark', title: '备注', type: 'slot', slotName: 'remarkCell', width: 160 },
  { key: 'businessLicense', title: '营业执照', type: 'slot', slotName: 'businessLicenseCell', width: 90, align: 'center' },
  { key: 'customerName', title: '绑定客户', type: 'slot', slotName: 'customerNameCell', width: 160 },
  { key: 'createTime', title: '申请时间', type: 'slot', slotName: 'createTimeCell', width: 160 },
  { key: 'qq', title: 'qq', type: 'slot', slotName: 'qqCell', width: 120 },
  { key: 'wechat', title: '微信', type: 'slot', slotName: 'wechatCell', width: 120 },
  // ── 本系统扩展列（默认隐藏） ──
  { key: 'username', title: '用户名', type: 'slot', slotName: 'usernameCell', width: 130, defaultHidden: true },
  { key: 'nickname', title: '昵称', type: 'slot', slotName: 'nicknameCell', width: 120, defaultHidden: true },
  { key: 'userType', title: '身份', type: 'slot', slotName: 'userTypeCell', width: 100, defaultHidden: true },
  { key: 'source', title: '来源', type: 'slot', slotName: 'sourceCell', width: 90, defaultHidden: true },
  { key: 'rejectReason', title: '驳回原因', type: 'slot', slotName: 'rejectReasonCell', width: 160, defaultHidden: true },
  { key: 'auditTime', title: '审核时间', type: 'slot', slotName: 'auditTimeCell', width: 160, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await shopUserApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      auditStatus: searchForm.auditStatus,
      // 注册时间范围：/user/page 已接收 createTimeStart/createTimeEnd（按 shop_user.create_time，截止日含当天）
      createTimeStart: searchForm.dateRange?.[0] || undefined,
      createTimeEnd: searchForm.dateRange?.[1] || undefined,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[买家申请管理] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 审核统计（对标三态计数，沿用原 ARReportPage 的真实分页 total 口径） */
async function refreshCounts() {
  try {
    const [p, a, r] = await Promise.all([
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 0 }),
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 1 }),
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 2 })
    ])
    counts.value = {
      pending: Number((p as any)?.total) || 0,
      approved: Number((a as any)?.total) || 0,
      rejected: Number((r as any)?.total) || 0
    }
  } catch (e) {
    console.warn('[买家申请管理] 审核统计获取失败', e)
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  fetchList()
  refreshCounts()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 审核动作 ═══
async function handleApprove(record: ShopUser) {
  try {
    await shopUserApi.approve(record.id)
    message.success(`买家「${record.nickname || record.username}」已审核通过`)
    fetchList()
    refreshCounts()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审核通过失败')
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<ShopUser | null>(null)

function openReject(record: ShopUser) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await shopUserApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`买家「${rejectTarget.value.nickname || rejectTarget.value.username}」已驳回`)
    rejectVisible.value = false
    fetchList()
    refreshCounts()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '驳回失败')
  } finally {
    rejectLoading.value = false
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
const printColumns: any[] = [
  { title: '公司名称', key: 'companyName', formatter: (v: any) => v || '' },
  { title: '联系人姓名', key: 'contactName', formatter: (v: any) => v || '' },
  { title: '地址', key: 'address', formatter: (v: any) => v || '' },
  { title: '手机号码', key: 'phone', formatter: (v: any) => v || '' },
  { title: '状态', key: 'auditStatus', formatter: (v: any) => AUDIT_STATUS_MAP[v]?.label || '' },
  { title: '绑定客户', key: 'customerName', formatter: (v: any, record: any) => v || (record.erpCustomerId ? '#' + record.erpCustomerId : '') },
  { title: '申请时间', key: 'createTime', formatter: (v: any) => fmtTime(v) },
]

function currentRows(): ShopUser[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'mall-buyer-apply',
  title: '买家申请管理',
  rows: () => currentRows(),
  columns: () => printColumns,
  // 原打印抬头的筛选/记录数元信息行（打印时间由模板 pageHeader 负责）
  totalText: () => `筛选条件：${searchForm.keyword || '全部'}，记录数：${currentRows().length}`,
  emptyTip: '没有可打印的数据',
})

// ═══ 导出（前端 CSV，\uFEFF BOM 保证 Excel 中文不乱码） ═══
function handleExport() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const header = ['公司名称', '联系人姓名', '地址', '手机号码', '状态', '备注', '绑定客户', '申请时间', 'qq', '微信']
  const lines = rows.map((r: any) => [
    r.companyName ?? '',
    r.contactName ?? '',
    r.address ?? '',
    r.phone ?? '',
    AUDIT_STATUS_MAP[r.auditStatus]?.label || '',
    r.remark ?? '',
    r.customerName ?? (r.erpCustomerId ? `#${r.erpCustomerId}` : ''),
    fmtTime(r.createTime),
    r.qq ?? '',
    r.wechat ?? '',
  ])
  const csv = [header, ...lines]
    .map(cols => cols.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `买家申请管理_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[买家申请管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  refreshCounts()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.stat-item { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
