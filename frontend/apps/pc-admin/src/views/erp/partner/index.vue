<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>往来单位管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header__title">往来单位管理</h2>
          </div>
          <div class="page-header__right">
            <a-space :size="12">
              <span class="data-status">
                <a-badge :status="loading ? 'processing' : 'success'" />
                <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
              </span>
              <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
                <SyncOutlined /> {{ autoRefreshCountdown }}s
              </span>
              <a-button size="small" :loading="loading" @click="debounceClick('refresh', refreshAll)">
                <template #icon><ReloadOutlined /></template>
                刷新
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <!-- 统计卡片 -->
      <a-row :gutter="12" style="margin-bottom: 12px;">
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #1890ff;">
            <div class="stat-value" style="color:#1890ff">{{ loading ? '-' : statistics.total }}</div>
            <div class="stat-label">单位总数</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #52c41a;">
            <div class="stat-value" style="color:#52c41a">{{ loading ? '-' : statistics.enabled }}</div>
            <div class="stat-label">启用</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #ff4d4f;">
            <div class="stat-value" style="color:#ff4d4f">{{ loading ? '-' : statistics.disabled }}</div>
            <div class="stat-label">停用</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card" style="border-top: 3px solid #722ed1;">
            <div class="stat-value" style="color:#722ed1">¥{{ formatAmount(statistics.creditTotal) }}</div>
            <div class="stat-label">总信用额度</div>
          </div>
        </a-col>
      </a-row>

      <div class="partner-main">
        <!-- 左侧分类/标签 -->
        <div class="partner-sidebar">
          <a-card size="small" title="单位类型" :body-style="{ padding: '8px' }">
            <a-tree
              :tree-data="typeTreeData"
              :selected-keys="[selectedType]"
              @select="onTypeSelect"
              block-node
            />
          </a-card>
          <a-divider style="margin: 8px 0" />
          <a-card size="small" title="分类" :body-style="{ padding: '8px' }">
            <a-tree
              v-if="categoryTree.length"
              :tree-data="categoryTree"
              :selected-keys="[selectedCategoryId ? [selectedCategoryId] : []]"
              :field-names="{ children: 'children', title: 'categoryName', key: 'id' }"
              @select="onCategorySelect"
              block-node
            />
            <a-empty v-else description="暂无分类" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </a-card>
        </div>

        <!-- 右侧列表 -->
        <div class="partner-content">
          <div class="partner-toolbar">
            <SearchBar
              :fields="searchFields"
              :loading="loading"
              @search="handleSearch"
              @reset="handleReset"
            />
            <a-space>
              <a-button type="primary" size="small" @click="router.push('/erp/partner/create')">
                <template #icon><PlusOutlined /></template>
                新增单位
              </a-button>
            </a-space>
          </div>

          <VxeTableList
            ref="tableRef"
            :columns="vxeColumns"
            :data-source="list"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            :show-toolbar="false"
            :selectable="true"
            :show-add="false"
            :show-search="false"
            :show-export="true"
            @export="handleExport"
            @page-change="handlePageChange"
            @cell-dblclick="handleView"
            @selection-change="handleSelectionChange"
          >
            <template #empty>
              <EmptyState v-if="hasError" image="error" title="数据加载异常" description="数据获取失败，请检查后重试" :show-add="false" size="small" @refresh="fetchPartners" />
              <EmptyState v-else image="no-data" title="暂无往来单位" description="当前没有单位数据" add-text="新增单位" size="small" @refresh="fetchPartners" @add="() => router.push('/erp/partner/create')" />
            </template>
            <template #typeCell="{ record }">
              <a-tag>{{ typeLabel(record.partnerType) }}</a-tag>
            </template>
            <template #statusCell="{ record }">
              <StatusTag :status="record.status" :map="PARTNER_STATUS" />
            </template>
            <template #action="{ record }">
              <a-space :size="4">
                <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
                <a-button type="link" size="small" @click="router.push(`/erp/partner/${record.id}`)">编辑</a-button>
                <a-button type="link" size="small" @click="handleToggleStatus(record)">
                  {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                </a-button>
                <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
              </a-space>
            </template>
          </VxeTableList>
        </div>
      </div>

      <!-- 详情抽屉 -->
      <a-drawer
        v-model:open="detailVisible"
        title="单位详情"
        placement="right"
        width="60vw"
        :footer="null"
        destroy-on-close
      >
        <a-spin :spinning="detailLoading">
          <a-descriptions bordered :column="2" size="small" v-if="detailData">
            <a-descriptions-item label="编码">{{ detailData.partnerCode }}</a-descriptions-item>
            <a-descriptions-item label="名称">{{ detailData.partnerName }}</a-descriptions-item>
            <a-descriptions-item label="类型">{{ typeLabel(detailData.partnerType) }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ detailData.categoryName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="等级">{{ detailData.gradeName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <StatusTag :status="detailData.status" :map="PARTNER_STATUS" />
            </a-descriptions-item>
            <a-descriptions-item label="联系人">{{ detailData.contactPerson || '-' }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ detailData.contactPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="邮箱">{{ detailData.contactEmail || '-' }}</a-descriptions-item>
            <a-descriptions-item label="地址">{{ detailData.province || '' }}{{ detailData.city || '' }}{{ detailData.district || '' }}{{ detailData.detailAddress || '' }}</a-descriptions-item>
            <a-descriptions-item label="信用额度">¥{{ formatAmount(detailData.creditLimit) }}</a-descriptions-item>
            <a-descriptions-item label="当前欠款">
              <span :style="{ color: detailData.currentBalance > 0 ? '#f5222d' : '#52c41a' }">
                ¥{{ formatAmount(detailData.currentBalance) }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="社会信用代码">{{ detailData.unifiedSocialCode || '-' }}</a-descriptions-item>
            <a-descriptions-item label="纳税人识别号">{{ detailData.taxId || '-' }}</a-descriptions-item>
            <a-descriptions-item label="法人代表">{{ detailData.legalPerson || '-' }}</a-descriptions-item>
            <a-descriptions-item label="结算方式">{{ detailData.settleType || '-' }}</a-descriptions-item>
            <a-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</a-descriptions-item>
          </a-descriptions>
          <a-empty v-if="!detailLoading && !detailData" description="未找到单位信息" />
          <div v-if="detailData" style="margin-top: 16px; text-align: right;">
            <PrintButton
              :business-id="detailData.id"
              business-type="PARTNER"
              :record="detailData"
              button-text="打印单位信息"
              button-size="small"
            />
          </div>
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Empty } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { PARTNER_STATUS } from '@/utils/statusConfig'
import { partnerApi, partnerCategoryApi, type Partner, type PartnerCategory } from '@/api/erp/partner'
import request from '@/utils/request'

// ── 类型 ──────────────────────────────────────────────
interface StatSummary {
  total: number
  enabled: number
  disabled: number
  creditTotal: number
}

// ── 防抖 ──────────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ─────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', refreshAll); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); router.push('/erp/partner/create'); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); debounceClick('export', handleExport); return }
}

const router = useRouter()
const loading = ref(false)
const hasError = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const list = ref<Partner[]>([])
const selectedType = ref('ALL')
const selectedCategoryId = ref<number>(0)
const categoryTree = ref<PartnerCategory[]>([])

const statistics = ref<StatSummary>({ total: 0, enabled: 0, disabled: 0, creditTotal: 0 })

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: true,
})

const tableRef = ref()

const searchFields = [
  { key: 'keyword', label: '编码/名称/联系人/电话', type: 'input', span: 2 },
  { key: 'status', label: '状态', type: 'select', span: 1, options: [
      { value: '', label: '全部' },
      { value: 'ENABLED', label: '启用' },
      { value: 'DISABLED', label: '停用' },
    ]
  },
]

const typeTreeData = [
  { title: '全部类型', key: 'ALL' },
  { title: '客户', key: 'CUSTOMER' },
  { title: '供应商', key: 'SUPPLIER' },
  { title: '购销(客户+供应商)', key: 'BOTH' },
  { title: '其他', key: 'OTHER' },
]

const vxeColumns = [
  { type: 'seq', title: '#', width: 50 },
  { field: 'partnerCode', title: '编码', width: 110 },
  { field: 'partnerName', title: '单位名称', minWidth: 140 },
  { field: 'partnerType', title: '类型', width: 80, slots: { default: 'typeCell' } },
  { field: 'gradeName', title: '等级', width: 80 },
  { field: 'contactPerson', title: '联系人', width: 100 },
  { field: 'contactPhone', title: '联系电话', width: 120 },
  { field: 'creditLimit', title: '信用额度', width: 110, align: 'right', formatter: ({ cellValue }: any) => cellValue ? '¥' + cellValue.toFixed(2) : '-' },
  { field: 'currentBalance', title: '欠款', width: 110, align: 'right', formatter: ({ cellValue }: any) => cellValue ? '¥' + cellValue.toFixed(2) : '-' },
  { field: 'status', title: '状态', width: 70, slots: { default: 'statusCell' } },
  { title: '操作', width: 240, fixed: 'right', slots: { default: 'action' } },
]

// ── 详情抽屉 ──────────────────────────────────────────
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<Partner | null>(null)

// ── 工具函数 ──────────────────────────────────────────
function typeLabel(type: string) {
  const map: Record<string, string> = { CUSTOMER: '客户', SUPPLIER: '供应商', BOTH: '购销', OTHER: '其他' }
  return map[type] || type
}

function formatAmount(val: number | undefined | null) {
  return (val || 0).toFixed(2)
}

function handleError(err: any) {
  hasError.value = true
  console.warn('[往来单位] ErrorBoundary 捕获异常:', err)
}

// ── 数据加载 ──────────────────────────────────────────
async function fetchPartners() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (selectedType.value !== 'ALL') params.partnerType = selectedType.value
    if (selectedCategoryId.value > 0) params.categoryId = selectedCategoryId.value

    const res = await partnerApi.page(params)
    list.value = res.records || []
    pagination.total = res.total || 0

    // 统计
    const all = res.records || []
    let enabled = 0, disabled = 0, creditSum = 0
    all.forEach((p: Partner) => {
      if (p.status === 'ENABLED') enabled++
      else if (p.status === 'DISABLED') disabled++
      creditSum += p.creditLimit || 0
    })
    statistics.value = { total: res.total || 0, enabled, disabled, creditTotal: creditSum }
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (err) {
    console.warn('[往来单位] 加载失败', err)
    message.error('加载单位数据失败')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    categoryTree.value = await partnerCategoryApi.getTree()
  } catch { /* ignore */ }
}

function refreshAll() {
  fetchPartners()
  fetchCategories()
}

// ── 自动刷新 ──────────────────────────────────────────
function startCountdown() {
  autoRefreshCountdown.value = 300
  countdownTimer = setInterval(() => {
    autoRefreshCountdown.value--
    if (autoRefreshCountdown.value <= 0) {
      autoRefreshCountdown.value = 300
    }
  }, 1000)
}

// ── 搜索/筛选 ─────────────────────────────────────────
// 从 SearchBar 中读取搜索值
const searchKeyword = ref('')
const searchStatus = ref('')

function handleSearch(values: Record<string, any>) {
  searchKeyword.value = values.keyword || ''
  searchStatus.value = values.status || ''
  pagination.current = 1
  fetchPartners()
}

function handleReset() {
  searchKeyword.value = ''
  searchStatus.value = ''
  pagination.current = 1
  fetchPartners()
}

function onTypeSelect(keys: (string | number)[]) {
  selectedType.value = (keys[0] as string) || 'ALL'
  pagination.current = 1
  fetchPartners()
}

function onCategorySelect(keys: (string | number)[]) {
  selectedCategoryId.value = (keys[0] as number) || 0
  pagination.current = 1
  fetchPartners()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchPartners()
}

function handleSelectionChange(rows: any[]) {
  // 选择变更处理
}

// ── 查看详情 ──────────────────────────────────────────
async function handleView(record: Partner) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res = await partnerApi.getById(record.id)
    detailData.value = res
  } catch (err) {
    console.warn('[往来单位] 获取详情失败', err)
    message.error('获取单位详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ── 业务操作 ──────────────────────────────────────────
function handleDelete(record: Partner) {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除单位 "${record.partnerName}" 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        fetchPartners()
      } catch (err) {
        console.warn('[往来单位] 删除失败', err)
        message.error('删除失败')
      }
    },
  })
}

async function handleToggleStatus(record: Partner) {
  const newStatus = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const label = record.status === 'ENABLED' ? '停用' : '启用'
  Modal.confirm({
    title: `${label}确认`,
    content: `确认${label}单位 "${record.partnerName}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await partnerApi.updateStatus(record.id, newStatus)
        message.success(`${label}成功`)
        fetchPartners()
      } catch (err) {
        console.warn('[往来单位] 状态变更失败', err)
        message.error(`${label}失败`)
      }
    },
  })
}

// ── 导出 ──────────────────────────────────────────────
async function handleExport() {
  try {
    const res = await request.get('/erp/partner/export', {
      params: {
        keyword: searchKeyword.value || undefined,
        status: searchStatus.value || undefined,
        partnerType: selectedType.value !== 'ALL' ? selectedType.value : undefined,
        categoryId: selectedCategoryId.value > 0 ? selectedCategoryId.value : undefined,
      },
      responseType: 'blob',
    })
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `往来单位_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err) {
    console.warn('[往来单位] 导出失败', err)
    message.error('导出失败')
  }
}

// ── 生命周期 ──────────────────────────────────────────
onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  fetchCategories()
  fetchPartners()
  refreshTimer = setInterval(refreshAll, 300000)
  startCountdown()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 4px 0 0 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }

.data-status { display: inline-flex; align-items: center; gap: 6px; }
.update-time { font-size: 12px; color: #999; white-space: nowrap; }

.auto-refresh-badge {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 统计卡片 */
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: default;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  line-height: 1.2;
}
.stat-label { font-size: 13px; color: #666; margin-top: 4px; }

/* 左右布局 */
.partner-main {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
}
.partner-sidebar {
  width: 200px;
  min-width: 160px;
  overflow-y: auto;
}
.partner-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.partner-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px 0;
}
</style>
