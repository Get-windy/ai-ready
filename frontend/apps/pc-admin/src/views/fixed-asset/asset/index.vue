<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="asset-page-header">
        <div class="asset-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>固定资产</a-breadcrumb-item>
            <a-breadcrumb-item>资产管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="asset-page-header-title">资产管理</h2>
        </div>
        <div class="asset-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <PrintButton business-type="fixed_asset" button-type="link" button-size="small" tooltip="打印资产列表" />
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)" v-permission="'erp:fixed-asset:asset:list'">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
<span class="shortcut-hints">
                                                <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
                                                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                                              </span>
        </div>

      </div>
    </template>

    <div class="asset-list-page">
      <!-- Statistics Cards -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statistics.totalCount || 0 }}</div>
            <div class="stat-card-label">资产总数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-active">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statistics.activeCount || 0 }}</div>
            <div class="stat-card-label">已启用</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-original">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statistics.totalOriginalValue) }}</div>
            <div class="stat-card-label">资产原值</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-net">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statistics.totalNetValue) }}</div>
            <div class="stat-card-label">资产净值</div>
          </div>
          <CalculatorOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :table-key="'fixed-asset-asset-list'"
        :filter-fields="filterFields"
        :show-export="true"
        export-permission="erp:fixed-asset:asset:export"
        :selectable="true"
        add-text="新增资产"
        add-permission="erp:fixed-asset:asset:create"
        delete-permission="erp:fixed-asset:asset:delete"
        @add="showCreateModal"
        @cell-dblclick="viewDetail"
        @edit="editAsset"
        @delete="handleDeleteWithConfirm"
        @batch-delete="handleBatchDelete"
        :min-empty-rows="12"
        @refresh="debounceClick('refresh', fetchData)"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @export="handleExport"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>

        <template #batch-actions>
          <a-button size="small" type="primary" ghost @click="handleBatchDepreciate">
            <template #icon><CalculatorOutlined /></template>
            批量折旧
          </a-button>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty table-empty-error">
            <WarningOutlined class="table-empty-icon table-empty-icon-error" />
            <p class="table-empty-text">数据加载失败，请重试</p>
            <a-button size="small" @click="fetchData">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </div>
          <div v-else class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的资产，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无资产数据，点击右上角「新增资产」开始创建
            </p>
          </div>
        </template>

        <template #assetCodeCell="{ record }">
          <a @click="viewDetail(record)" class="asset-code">{{ record.assetCode }}</a>
        </template>
        <template #assetNameCell="{ record }">
          <a @click="viewDetail(record)" class="asset-name">{{ record.assetName }}</a>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusMap[record.status] || record.status }}</a-tag>
        </template>
        <template #useStatusCell="{ record }">
          <a-tag :color="useStatusColorMap[record.useStatus] || 'default'">{{ useStatusMap[record.useStatus] || record.useStatus }}</a-tag>
        </template>
        <template #originalValueCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.originalValue) }}</span>
        </template>
        <template #netValueCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.netValue) }}</span>
        </template>
        <template #monthlyDepreciationCell="{ record }">
          <span class="amount-cell depreciation">¥{{ formatAmount(record.monthlyDepreciation) }}</span>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'draft'" title="编辑">
              <a-button type="link" size="small" @click="editAsset(record)" v-permission="'erp:fixed-asset:asset:update'">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              template-type="asset"
              :business-id="record.id"
              business-type="fixed_asset"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }: any) => handleActionMenuClick(key as string, record)">
                  <a-menu-item v-if="record.status === 'draft'" key="activate" v-permission="'erp:fixed-asset:asset:update'">
                    <CheckCircleOutlined /> 启用
                  </a-menu-item>
                  <a-menu-item key="depreciate" v-permission="'erp:fixed-asset:asset:update'">
                    <CalculatorOutlined /> 折旧计提
                  </a-menu-item>
                  <a-menu-item key="transfer" v-permission="'erp:fixed-asset:asset:update'">
                    <SwapOutlined /> 资产转移
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- Create/Edit Modal -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="isEdit ? '编辑资产' : '新增资产'"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="资产编码">
                <a-input v-model:value="formData.assetCode" placeholder="自动生成可不填" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="资产名称" required>
                <a-input v-model:value="formData.assetName" placeholder="请输入资产名称" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="分类">
                <a-select v-model:value="formData.categoryId" placeholder="选择分类" allow-clear size="small">
                  <a-select-option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.categoryName }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="购置日期">
                <a-date-picker v-model:value="formData.purchaseDate" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="原值">
                <a-input-number v-model:value="formData.originalValue" :precision="2" style="width: 100%" :min="0" size="small">
                  <template #addonBefore>¥</template>
                </a-input-number>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="净值">
                <a-input-number v-model:value="formData.netValue" :precision="2" style="width: 100%" :min="0" size="small">
                  <template #addonBefore>¥</template>
                </a-input-number>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="折旧方法">
                <a-select v-model:value="formData.depreciationMethod" placeholder="选择折旧方法" size="small">
                  <a-select-option value="straight_line">直线法</a-select-option>
                  <a-select-option value="double_declining">双倍余额递减法</a-select-option>
                  <a-select-option value="sum_of_years">年数总和法</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="使用年限(月)">
                <a-input-number v-model:value="formData.usefulLife" :min="1" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="残值">
                <a-input-number v-model:value="formData.salvageValue" :precision="2" style="width: 100%" :min="0" size="small">
                  <template #addonBefore>¥</template>
                </a-input-number>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="残值率(%)">
                <a-input-number v-model:value="formData.salvageRate" :precision="2" style="width: 100%" :min="0" :max="100" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="使用部门">
                <a-input v-model:value="formData.departmentName" placeholder="部门名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="保管人">
                <a-input v-model:value="formData.custodianName" placeholder="保管人姓名" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="存放地点">
                <a-input v-model:value="formData.location" placeholder="存放地点" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="状态">
                <a-select v-model:value="formData.status" placeholder="选择状态" size="small">
                  <a-select-option value="draft">草稿</a-select-option>
                  <a-select-option value="active">已启用</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="规格型号">
                <a-input v-model:value="formData.specification" placeholder="规格型号" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="品牌">
                <a-input v-model:value="formData.brand" placeholder="品牌" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="供应商">
                <a-input v-model:value="formData.supplierName" placeholder="供应商" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="发票号">
                <a-input v-model:value="formData.invoiceNo" placeholder="发票号" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="使用状态">
                <a-select v-model:value="formData.useStatus" placeholder="使用状态" size="small">
                  <a-select-option value="in_use">使用中</a-select-option>
                  <a-select-option value="idle">闲置</a-select-option>
                  <a-select-option value="maintenance">维修中</a-select-option>
                  <a-select-option value="disposed">已处置</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="保修到期">
                <a-date-picker v-model:value="formData.warrantyEndDate" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="备注" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
            <a-textarea v-model:value="formData.remark" :rows="2" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 详情弹窗 -->
      <a-drawer
        v-model:open="detailVisible"
        title="资产详情"
        placement="right"
        width="80vw"
      >
        <a-descriptions bordered :column="2" v-if="currentAsset">
          <a-descriptions-item label="资产编码">{{ currentAsset.assetCode }}</a-descriptions-item>
          <a-descriptions-item label="资产名称">{{ currentAsset.assetName }}</a-descriptions-item>
          <a-descriptions-item label="分类">{{ currentAsset.categoryName }}</a-descriptions-item>
          <a-descriptions-item label="购置日期">{{ currentAsset.purchaseDate }}</a-descriptions-item>
          <a-descriptions-item label="资产原值">
            <span class="amount-cell">¥{{ formatAmount(currentAsset.originalValue) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="资产净值">
            <span class="amount-cell">¥{{ formatAmount(currentAsset.netValue) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="累计折旧">
            <span class="amount-cell depreciation">¥{{ formatAmount(currentAsset.accumulatedDepreciation) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="月折旧额">
            <span class="amount-cell depreciation">¥{{ formatAmount(currentAsset.monthlyDepreciation) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="折旧方法">{{ depreciationMethodMap[currentAsset.depreciationMethod] }}</a-descriptions-item>
          <a-descriptions-item label="使用年限">{{ currentAsset.usefulLife }}月</a-descriptions-item>
          <a-descriptions-item label="残值">¥{{ formatAmount(currentAsset.salvageValue) }}</a-descriptions-item>
          <a-descriptions-item label="残值率">{{ currentAsset.salvageRate }}%</a-descriptions-item>
          <a-descriptions-item label="使用部门">{{ currentAsset.departmentName }}</a-descriptions-item>
          <a-descriptions-item label="保管人">{{ currentAsset.custodianName }}</a-descriptions-item>
          <a-descriptions-item label="存放地点">{{ currentAsset.location }}</a-descriptions-item>
          <a-descriptions-item label="规格型号">{{ currentAsset.specification || '-' }}</a-descriptions-item>
          <a-descriptions-item label="品牌">{{ currentAsset.brand || '-' }}</a-descriptions-item>
          <a-descriptions-item label="供应商">{{ currentAsset.supplierName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发票号">{{ currentAsset.invoiceNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="使用状态">
            <a-tag :color="useStatusColorMap[currentAsset.useStatus]">{{ useStatusMap[currentAsset.useStatus] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[currentAsset.status]">{{ statusMap[currentAsset.status] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="保修到期">{{ currentAsset.warrantyEndDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ currentAsset.remark || '-' }}</a-descriptions-item>
        </a-descriptions>

        <div class="detail-modal-footer">
          <a-button v-if="currentAsset?.status === 'draft'" type="primary" @click="handleActivate(currentAsset)">启用</a-button>
          <a-button @click="handleDepreciate(currentAsset)">
            <template #icon><CalculatorOutlined /></template>
            折旧计提
          </a-button>
          <PrintButton :business-id="currentAsset?.id" business-type="fixed_asset" button-size="small" tooltip="打印" />
          <a-button @click="detailVisible = false">关闭</a-button>
        </div>
      </a-drawer>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined,
  CheckCircleOutlined, CalculatorOutlined, SwapOutlined,
  DeleteOutlined, FileTextOutlined, DollarOutlined, SyncOutlined, ReloadOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { fixedAssetApi, fixedAssetCategoryApi } from '@/api/fixed-asset'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const router = useRouter()
const emit = defineEmits(['update-count'])

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

interface FixedAssetRecord {
  id: number
  assetCode: string
  assetName: string
  categoryId: number
  categoryName: string
  purchaseDate: string
  originalValue: number
  netValue: number
  depreciationMethod: string
  usefulLife: number
  salvageValue: number
  salvageRate: number
  monthlyDepreciation: number
  accumulatedDepreciation: number
  status: string
  location: string
  departmentName: string
  custodianName: string
  specification: string
  brand: string
  supplierName: string
  invoiceNo: string
  warrantyEndDate: string
  useStatus: string
  remark: string
}

interface Category {
  id: number
  categoryName: string
}

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const detailVisible = ref(false)
const currentAsset = ref<FixedAssetRecord | null>(null)
const tableData = ref<FixedAssetRecord[]>([])
const categories = ref<Category[]>([])
const selectedRowKeys = ref<number[]>([])
const statistics = ref<any>({})
const tableRef = ref()
const lastUpdated = ref('')
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const formData = reactive<any>({
  assetCode: '',
  assetName: '',
  categoryId: undefined,
  purchaseDate: undefined,
  originalValue: undefined,
  netValue: undefined,
  depreciationMethod: 'straight_line',
  usefulLife: undefined,
  salvageValue: undefined,
  salvageRate: undefined,
  status: 'draft',
  location: '',
  departmentName: '',
  custodianName: '',
  specification: '',
  brand: '',
  supplierName: '',
  invoiceNo: '',
  warrantyEndDate: undefined,
  useStatus: 'in_use',
  remark: '',
})

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})

function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formData)
}

watchReady = true
saveFormSnapshot()

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const searchFilters = reactive<Record<string, any>>({
  keyword: undefined,
  status: undefined,
  useStatus: undefined,
  categoryId: undefined,
  assetCode: undefined,
  assetName: undefined,
})

const hasActiveFilters = computed(() =>
  Object.values(searchFilters).some(v => v !== undefined && v !== '' && v !== null)
)

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

const vxeColumns = computed(() => [
  { field: 'assetCode', title: '资产编码', width: 140, slotName: 'assetCodeCell' },
  { field: 'assetName', title: '资产名称', width: 180, slotName: 'assetNameCell' },
  { field: 'categoryName', title: '分类', width: 120 },
  { field: 'originalValue', title: '原值', width: 120, align: 'right', slotName: 'originalValueCell' },
  { field: 'netValue', title: '净值', width: 120, align: 'right', slotName: 'netValueCell' },
  { field: 'monthlyDepreciation', title: '月折旧额', width: 100, align: 'right', slotName: 'monthlyDepreciationCell' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { field: 'useStatus', title: '使用状态', width: 80, align: 'center', slotName: 'useStatusCell' },
  { field: 'departmentName', title: '部门', width: 100 },
  { field: 'custodianName', title: '保管人', width: 80 },
  { field: 'purchaseDate', title: '购置日期', width: 100 },
  { type: 'action', title: '操作', width: 160, fixed: 'right' },
])

const filterFields = computed(() => [
  { key: 'assetCode', label: '资产编码', type: 'input' as const, placeholder: '资产编码' },
  { key: 'assetName', label: '资产名称', type: 'input' as const, placeholder: '资产名称' },
  { key: 'categoryId', label: '分类', type: 'select' as const, options: categories.value.map(c => ({ label: c.categoryName, value: c.id })) },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已启用', value: 'active' },
    { label: '已转移', value: 'transferred' },
    { label: '已处置', value: 'disposed' },
    { label: '已报废', value: 'scrapped' },
  ]},
  { key: 'useStatus', label: '使用状态', type: 'select' as const, options: [
    { label: '使用中', value: 'in_use' },
    { label: '闲置', value: 'idle' },
    { label: '维修中', value: 'maintenance' },
    { label: '已处置', value: 'disposed' },
  ]},
])

const statusMap: Record<string, string> = {
  draft: '草稿', active: '已启用', transferred: '已转移', disposed: '已处置', scrapped: '已报废',
}
const statusColorMap: Record<string, string> = {
  draft: 'default', active: 'green', transferred: 'blue', disposed: 'red', scrapped: 'orange',
}
const useStatusMap: Record<string, string> = {
  in_use: '使用中', idle: '闲置', maintenance: '维修中', disposed: '已处置',
}
const useStatusColorMap: Record<string, string> = {
  in_use: 'green', idle: 'default', maintenance: 'orange', disposed: 'red',
}
const depreciationMethodMap: Record<string, string> = {
  straight_line: '直线法', double_declining: '双倍余额递减法', sum_of_years: '年数总和法',
}

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

function handleParentCreate() {
  showCreateModal()
}

// ── 表单关闭/保存并新建 ──────────────────────────────────
function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的修改，确定要关闭吗？',
      okText: '关闭',
      cancelText: '继续编辑',
      onOk: () => { modalVisible.value = false }
    })
  } else {
    modalVisible.value = false
  }
}

function handleFormSaveAndNew() {
  handleModalOk(true)
}

onMounted(() => {
  fetchData()
  fetchStatistics()
  fetchCategories()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:create', handleParentCreate)
  window.addEventListener('fixed-asset:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onBeforeRouteLeave((to, from, next) => {
  if (modalVisible.value && formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的修改，确定要离开吗？',
      okText: '离开',
      cancelText: '继续编辑',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

async function fetchData() {
  loading.value = true
  hasError.value = false
  const params: any = {
    ...searchFilters,
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  try {
    const res = await fixedAssetApi.getPage(params)
    if (res.data) {
      tableData.value = res.content || res.records || []
      pagination.total = res.totalElements || res.total || 0
      lastUpdated.value = new Date().toISOString()
      emit('update-count', pagination.total)
    }
  } catch {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[资产管理] 加载资产数据失败')
    message.error('加载资产数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

function fetchStatistics() {
  fixedAssetApi.getStatistics().then((res: any) => {
    if (res.data) {
      statistics.value = res.data
    } else {
      statistics.value = { totalCount: 0, activeCount: 0, totalOriginalValue: 0, totalNetValue: 0 }
    }
  }).catch(() => {
    statistics.value = { totalCount: 0, activeCount: 0, totalOriginalValue: 0, totalNetValue: 0 }
    console.warn('[资产管理] 加载统计数据失败')
  })
}

function fetchCategories() {
  fixedAssetCategoryApi.getList().then((res: any) => {
    if (res.data) {
      categories.value = res.data
    } else {
      categories.value = []
    }
  }).catch(() => {
    categories.value = []
    console.warn('[资产管理] 加载分类数据失败')
  })
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    assetCode: '', assetName: '', categoryId: undefined, purchaseDate: undefined,
    originalValue: undefined, netValue: undefined, depreciationMethod: 'straight_line',
    usefulLife: undefined, salvageValue: undefined, salvageRate: undefined,
    status: 'draft', location: '', departmentName: '', custodianName: '',
    specification: '', brand: '', supplierName: '', invoiceNo: '',
    warrantyEndDate: undefined, useStatus: 'in_use', remark: '',
  })
  nextTick(() => saveFormSnapshot())
  modalVisible.value = true
}

function editAsset(record: FixedAssetRecord) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  nextTick(() => saveFormSnapshot())
  modalVisible.value = true
}

async function viewDetail(record: FixedAssetRecord) {
  detailVisible.value = true
  try {
    const res = await fixedAssetApi.getById(record.id)
    currentAsset.value = res.data
  } catch {
    message.error('获取资产详情失败')
  }
}

function handleModalOk(stayOpen?: boolean) {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? fixedAssetApi.update(editId.value, formData)
    : fixedAssetApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    if (!stayOpen) {
      modalVisible.value = false
    } else {
      // Reset form for new entry
      Object.assign(formData, {
        assetCode: '', assetName: '', categoryId: undefined, purchaseDate: undefined,
        originalValue: undefined, netValue: undefined, depreciationMethod: 'straight_line',
        usefulLife: undefined, salvageValue: undefined, salvageRate: undefined,
        status: 'draft', location: '', departmentName: '', custodianName: '',
        specification: '', brand: '', supplierName: '', invoiceNo: '',
        warrantyEndDate: undefined, useStatus: 'in_use', remark: '',
      })
      nextTick(() => saveFormSnapshot())
    }
    fetchData()
    fetchStatistics()
  }).catch((err: any) => {
    console.warn('[资产管理] 操作失败', err)
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleDelete(id: number) {
  fixedAssetApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
    fetchStatistics()
  }).catch((err: any) => {
    console.warn('[资产管理] 删除失败', err)
    message.error(err.message || '删除失败')
  })
}

function handleDeleteWithConfirm(record: FixedAssetRecord) {
  Modal.confirm({
    title: '删除资产',
    content: `确认删除资产 "${record.assetName}"？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    centered: true,
    onOk: () => handleDelete(record.id)
  })
}

function handleBatchDelete() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要删除的资产')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确认删除选中的 ${keys.length} 条资产记录？`,
    okText: '确认删除',
    okType: 'danger',
    centered: true,
    onOk: async () => {
      try {
        await Promise.all(keys.map((id: number) => fixedAssetApi.delete(id)))
        message.success('批量删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err.message || '批量删除失败')
      }
    }
  })
}

function handleDepreciate(record: FixedAssetRecord) {
  Modal.confirm({
    title: '折旧计提',
    content: `对资产 "${record.assetName}" 进行折旧计提？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try {
        await fixedAssetApi.depreciate(record.id)
        message.success('折旧计提成功')
        fetchData()
        fetchStatistics()
      } catch {
        console.warn('[资产管理] 折旧计提失败')
        message.error('折旧计提失败')
      }
    }
  })
}

function handleBatchDepreciate() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要折旧的资产')
    return
  }
  Modal.confirm({
    title: '批量折旧',
    content: `对选中的 ${keys.length} 条资产进行折旧计提？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try {
        await Promise.all(keys.map((id: number) => fixedAssetApi.depreciate(id)))
        message.success('批量折旧计提完成')
        fetchData()
        fetchStatistics()
      } catch (err: any) {
        message.error(err.message || '批量折旧计提失败')
      }
    }
  })
}

function handleActivate(record: FixedAssetRecord) {
  Modal.confirm({
    title: '启用资产',
    content: `启用资产 "${record.assetName}"？`,
    okText: '确认启用',
    centered: true,
    onOk: async () => {
      try {
        await fixedAssetApi.update(record.id, { status: 'active' })
        message.success('已启用')
        fetchData()
        fetchStatistics()
        detailVisible.value = false
      } catch {
        console.warn('[资产管理] 启用失败')
        message.error('启用失败')
      }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: FixedAssetRecord) {
  switch (key) {
    case 'activate': handleActivate(record); break
    case 'depreciate': handleDepreciate(record); break
    case 'transfer': router.push({ path: '/fixed-asset', query: { tab: 'transfer' } }); break
    case 'delete': handleDeleteWithConfirm(record); break
  }
}

function handleExport() {
  const headers = ['资产编码', '资产名称', '分类', '原值', '净值', '月折旧额', '状态', '使用状态', '部门', '保管人', '购置日期', '存放地点']
  const rows = tableData.value.map(r => [
    r.assetCode, r.assetName, r.categoryName, formatAmount(r.originalValue),
    formatAmount(r.netValue), formatAmount(r.monthlyDepreciation),
    statusMap[r.status], useStatusMap[r.useStatus], r.departmentName,
    r.custodianName, r.purchaseDate, r.location
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `资产列表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    showCreateModal()
  }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:create', handleParentCreate)
  window.removeEventListener('fixed-asset:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.asset-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.asset-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.asset-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.asset-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.asset-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.asset-list-page :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-original { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-net { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 空状态 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.table-empty-error {
  padding: 48px 0;
}

.table-empty-icon-error {
  color: #faad14;
}

.asset-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.asset-name {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.depreciation {
  color: #faad14;
}

.action-more-btn {
  padding: 0 4px;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.ant-input-sm), :deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small), :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
