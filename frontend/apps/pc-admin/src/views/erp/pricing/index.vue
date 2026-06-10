<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 价格管理 / 客户等级价格配置</span>
          <h2 class="page-header__title">客户等级与产品价格关联</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于: {{ lastUpdateTime }}</span>
          <a-button size="small" @click="handleExport">
            <template #icon><ExportOutlined /></template>
            导出
          </a-button>
          <a-button size="small" @click="handleImport">
            <template #icon><ImportOutlined /></template>
            导入
          </a-button>
          <PrintButton page-code="erp/pricing" button-size="small" tooltip="打印" />
        </div>
      </div>
    </template>

    <!-- 统计概览卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px" v-if="selectedGrade">
      <a-col :span="8">
        <a-card size="small" :body-style="{ padding: '12px 16px' }">
          <div class="stat-item">
            <div class="stat-label">产品总数</div>
            <div class="stat-value">{{ stats.totalProductCount }}</div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card size="small" :body-style="{ padding: '12px 16px' }">
          <div class="stat-item">
            <div class="stat-label">已配置价格</div>
            <div class="stat-value">{{ stats.configuredCount }}</div>
          </div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card size="small" :body-style="{ padding: '12px 16px' }">
          <div class="stat-item">
            <div class="stat-label">价格覆盖率</div>
            <div class="stat-value" :style="stats.coverageStyle">{{ stats.coverageRate }}%</div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 价格分布图 -->
    <a-card v-if="selectedGrade && filteredPriceList.length > 0" size="small" :body-style="{ padding: '12px 16px' }" style="margin-bottom: 16px">
      <div class="distribution-chart">
        <div class="distribution-title">价格分布</div>
        <div class="distribution-bars">
          <div v-for="cat in priceDistribution" :key="cat.label" class="distribution-bar-row">
            <span class="distribution-bar-label">{{ cat.label }}</span>
            <div class="distribution-bar-track" :title="`${cat.count} 项 (${cat.percentage}%)`">
              <div class="distribution-bar-fill" :style="{ width: cat.barWidth + '%' }"></div>
            </div>
            <span class="distribution-bar-count">{{ cat.count }} 项</span>
            <span class="distribution-bar-pct">{{ cat.percentage }}%</span>
          </div>
        </div>
      </div>
    </a-card>

    <a-row :gutter="16">
      <a-col :span="6">
        <a-card title="客户等级" size="small" :body-style="{ padding: '8px' }">
          <a-spin :spinning="gradeLoading">
            <a-menu v-model:selected-keys="selectedGradeKeys" @click="onGradeSelect" style="border: none">
              <a-menu-item v-for="g in grades" :key="g.id">
                <StatusTag :status="g.gradeCode" :map="GRADE_CODE_MAP" />
                {{ g.gradeName }}
              </a-menu-item>
            </a-menu>
            <EmptyState
              v-if="grades.length === 0 && !gradeLoading"
              title="暂无客户等级"
              description="请在客户管理中添加客户等级"
              size="small"
              :show-add="false"
              :show-refresh="true"
              @refresh="loadGrades"
            />
          </a-spin>
        </a-card>
      </a-col>

      <a-col :span="18">
        <a-card :title="`等级价格配置 - ${selectedGrade?.gradeName || ''}`" size="small">
          <template #extra>
            <a-space>
              <a-button size="small" @click="handleExport">
                <template #icon><ExportOutlined /></template>
                导出
              </a-button>
              <a-button size="small" @click="handleImport">导入</a-button>
              <a-button size="small" @click="handleShowComparison">对比等级</a-button>
              <a-button size="small" @click="showSeasonalAdjustmentModal">季节性调价</a-button>
              <a-button size="small" @click="showChangeHistoryDrawer = true">变更记录</a-button>
              <a-button type="primary" size="small" :loading="saving" @click="handleSave">保存配置</a-button>
            </a-space>
          </template>
          <EmptyState
            v-if="!selectedGrade"
            title="请选择客户等级"
            description="请先在左侧选择需要配置价格的客户等级"
            size="middle"
            :show-actions="false"
          />
          <template v-else>
            <a-spin :spinning="priceListLoading">
              <div class="pricing-toolbar">
                <SearchBar :fields="searchFields" @search="handleSearch" @reset="handleReset" />
                <a-button size="small" :disabled="selectedRows.length === 0" @click="handleBatchToggle">批量启用/禁用</a-button>
                <a-button size="small" @click="showPriceAdjustmentModal">整表调价</a-button>
                <a-button size="small" @click="handleImport">导入</a-button>
                <a-button size="small" @click="handleShowComparison">对比等级</a-button>
                <a-button size="small" @click="showSeasonalAdjustmentModal">季节性调价</a-button>
                <span v-if="selectedRows.length > 0" class="selected-count">已选择 {{ selectedRows.length }} 项</span>
                <span v-if="autoRefreshCountdown > 0" class="countdown-hint">
                  <SyncOutlined :spin="priceListLoading" /> {{ autoRefreshCountdown }}s 后自动刷新
                </span>
              </div>
              <vxe-table
                ref="priceTableRef"
                :data="filteredPriceList"
                border
                size="small"
                max-height="500"
                align="center"
                :edit-config="{ trigger: 'click', mode: 'row' }"
                :checkbox-config="{ highlight: true }"
                show-footer
                :footer-method="footerMethod"
                @checkbox-change="onCheckboxChange"
                @checkbox-all="onCheckboxAll"
              >
                <vxe-column type="checkbox" width="50" />
                <vxe-column type="seq" title="#" width="50" />
                <vxe-column field="productCode" title="产品编码" width="120" sortable />
                <vxe-column field="productName" title="产品名称" min-width="140" sortable />
                <vxe-column field="standardPrice" title="标准售价" width="110" align="right" sortable>
                  <template #default="{ row }">{{ row.standardPrice ? '¥' + row.standardPrice.toFixed(2) : '-' }}</template>
                </vxe-column>
                <vxe-column field="gradeOriginalPrice" title="等级基准价" width="110" align="right" sortable>
                  <template #default="{ row }">{{ row.gradeOriginalPrice ? '¥' + row.gradeOriginalPrice.toFixed(2) : '-' }}</template>
                </vxe-column>
                <vxe-column field="price" title="客户等级售价" width="140" align="right" sortable
                  :edit-render="{ name: 'input', type: 'number', props: { precision: 2, min: 0 } }">
                  <template #default="{ row }">
                    <span :style="getPriceStyle(row)">{{ row.price != null ? '¥' + row.price.toFixed(2) : '-' }}</span>
                  </template>
                </vxe-column>
                <vxe-column title="折扣率" width="90" align="right">
                  <template #default="{ row }">
                    <span v-if="row.price != null && row.standardPrice > 0"
                      :style="{ color: row.price < row.standardPrice ? '#52c41a' : row.price > row.standardPrice ? '#f5222d' : undefined }">
                      {{ (row.price / row.standardPrice * 100).toFixed(1) }}%
                    </span>
                    <span v-else>-</span>
                  </template>
                </vxe-column>
                <vxe-column title="启用" width="70">
                  <template #default="{ row }">
                    <a-switch v-model:checked="row.enabled" size="small" />
                  </template>
                </vxe-column>
              </vxe-table>
              <EmptyState
                v-if="filteredPriceList.length === 0 && !priceListLoading"
                title="暂无产品数据"
                description="当前等级下没有可配置的产品"
                size="small"
                :show-actions="false"
              />
            </a-spin>
          </template>
        </a-card>
      </a-col>
    </a-row>

    <!-- 整表调价弹窗 -->
    <a-modal v-model:visible="priceAdjustmentVisible" title="整表调价" @ok="applyPriceAdjustment" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="调整范围">
          <a-radio-group v-model:value="adjustmentScope">
            <a-radio value="visible">当前所有可见产品（{{ filteredPriceList.length }} 项）</a-radio>
            <a-radio value="selected" :disabled="selectedRows.length === 0">已选产品（{{ selectedRows.length }} 项）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="调整类型">
          <a-radio-group v-model:value="adjustmentType">
            <a-radio value="fixed">固定金额（元）</a-radio>
            <a-radio value="percentage">百分比（%）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="调整方向">
          <a-radio-group v-model:value="adjustmentDirection">
            <a-radio value="increase">涨价</a-radio>
            <a-radio value="decrease">降价</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="调整值">
          <a-input-number v-model:value="adjustmentValue"
            :precision="adjustmentType === 'fixed' ? 2 : 1"
            :min="0.01"
            :max="adjustmentType === 'fixed' ? 99999 : 100"
            :formatter="(value: number | undefined) => adjustmentType === 'fixed' ? `\u00A5${value ?? 0}` : `${value ?? 0}%`"
            :parser="(value: string | undefined) => (value || '').replace(/[¥%]/g, '')"
            style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量导入弹窗 -->
    <a-modal v-model:visible="importModalVisible" title="批量导入价格" width="720px" :footer="null" destroy-on-close>
      <div class="import-modal">
        <div class="import-upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="onFileDrop">
          <UploadOutlined style="font-size: 48px; color: #1890ff;" />
          <p style="margin: 12px 0 4px; color: #333; font-weight: 500;">点击或拖拽文件到此处</p>
          <p style="margin: 0; color: #999; font-size: 12px;">支持 .csv 格式文件</p>
          <a style="margin-top: 8px; font-size: 12px;" @click.stop="downloadTemplate">下载导入模板</a>
        </div>
        <input ref="fileInputRef" type="file" accept=".csv" style="display:none" @change="onFileSelected" />

        <template v-if="importPreviewData.length > 0">
          <a-divider />
          <div class="import-summary">
            <a-alert
              type="info"
              show-icon
              :message="`文件解析完成：总 ${importPreviewData.length} 行，有效 ${validImportCount} 行，无效 ${importPreviewData.length - validImportCount} 行`"
              style="margin-bottom: 12px"
            />
          </div>
          <vxe-table
            :data="importPreviewData.filter(r => r.valid)"
            border
            size="small"
            max-height="300"
            align="center"
          >
            <vxe-column type="seq" title="#" width="50" />
            <vxe-column field="productCode" title="产品编码" width="140" />
            <vxe-column field="price" title="导入价格" width="140" align="right">
              <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
            </vxe-column>
            <vxe-column title="状态" width="80">
              <template #default="{ row }">
                <a-tag :color="row.valid ? 'green' : 'red'">{{ row.valid ? '有效' : '无效' }}</a-tag>
              </template>
            </vxe-column>
          </vxe-table>
          <div class="import-actions">
            <a-button style="margin-right: 8px" @click="importModalVisible = false">取消</a-button>
            <a-button type="primary" :disabled="validImportCount === 0" @click="confirmImport">确认导入</a-button>
          </div>
        </template>
      </div>
    </a-modal>

    <!-- 等级对比抽屉 -->
    <a-drawer
      v-model:visible="comparisonDrawerVisible"
      title="多等级价格对比"
      placement="right"
      width="80%"
    >
      <template v-if="comparisonLoading">
        <div style="text-align: center; padding: 60px 0;">
          <a-spin tip="加载对比数据中..." />
        </div>
      </template>
      <template v-else-if="comparisonData.length === 0">
        <EmptyState title="暂无对比数据" description="请先加载客户等级和产品数据" size="small" :show-actions="false" />
      </template>
      <template v-else>
        <vxe-table
          :data="comparisonData"
          border
          size="small"
          max-height="600"
          align="center"
        >
          <vxe-column type="seq" title="#" width="50" />
          <vxe-column field="productCode" title="产品编码" width="130" sortable />
          <vxe-column field="productName" title="产品名称" min-width="150" sortable />
          <vxe-column field="standardPrice" title="标准售价" width="110" align="right" sortable>
            <template #default="{ row }">{{ row.standardPrice ? '¥' + row.standardPrice.toFixed(2) : '-' }}</template>
          </vxe-column>
          <vxe-column v-for="g in comparisonGrades" :key="g.id" :field="'grade_' + g.id" :title="g.gradeName" width="120" align="right" sortable>
            <template #default="{ row }">
              <span v-if="row['grade_' + g.id] != null" :style="getComparisonPriceStyle(row['grade_' + g.id] as number, row.standardPrice)">
                ¥{{ (row['grade_' + g.id] as number).toFixed(2) }}
              </span>
              <span v-else>-</span>
            </template>
          </vxe-column>
        </vxe-table>
      </template>
    </a-drawer>

    <!-- 变更记录抽屉 -->
    <a-drawer
      v-model:visible="showChangeHistoryDrawer"
      title="变更记录"
      placement="right"
      width="640px"
    >
      <template v-if="pendingChanges.length === 0">
        <EmptyState title="暂无变更记录" description="当前会话中尚未修改任何价格" size="small" :show-actions="false" />
      </template>
      <template v-else>
        <a-alert
          type="info"
          show-icon
          :message="`当前会话共有 ${pendingChanges.length} 条修改，尚未保存`"
          style="margin-bottom: 16px"
        />
        <vxe-table
          :data="pendingChanges"
          border
          size="small"
          max-height="500"
          align="center"
        >
          <vxe-column type="seq" title="#" width="50" />
          <vxe-column field="productCode" title="产品编码" width="100" />
          <vxe-column field="productName" title="产品名称" min-width="110" />
          <vxe-column field="oldPrice" title="原价" width="100" align="right">
            <template #default="{ row }">{{ row.oldPrice != null ? '¥' + row.oldPrice.toFixed(2) : '-' }}</template>
          </vxe-column>
          <vxe-column field="newPrice" title="新价" width="100" align="right">
            <template #default="{ row }">¥{{ row.newPrice.toFixed(2) }}</template>
          </vxe-column>
          <vxe-column field="timestamp" title="变更时间" width="170" />
        </vxe-table>
      </template>
    </a-drawer>

    <!-- 季节性调价弹窗 -->
    <a-modal v-model:visible="seasonalAdjustmentVisible" title="季节性调价" @ok="applySeasonalAdjustment" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="调价比例（%）">
          <a-input-number v-model:value="seasonalPercentage" :min="0.1" :max="999" :precision="1" style="width: 100%" placeholder="输入调价百分比（正数为涨价）" />
        </a-form-item>
        <a-form-item label="生效等级">
          <a-checkbox-group v-model:value="seasonalGradeIds">
            <a-checkbox v-for="g in grades" :key="g.id" :value="g.id">
              <StatusTag :status="g.gradeCode" :map="GRADE_CODE_MAP" />
              {{ g.gradeName }}
            </a-checkbox>
          </a-checkbox-group>
        </a-form-item>
        <a-form-item label="生效日期">
          <a-date-picker v-model:value="seasonalEffectiveDate" value-format="YYYY-MM-DD" style="width: 100%" placeholder="选择生效日期" />
        </a-form-item>
        <a-alert
          v-if="seasonalPercentage > 0 && seasonalTargetCount > 0"
          type="warning"
          show-icon
          :message="`将对当前已筛选产品（${seasonalTargetCount} 项）按 ${seasonalPercentage}% 的比例进行调价`"
        />
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ExportOutlined, SyncOutlined, ImportOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import type { StatusMap } from '@/utils/statusConfig'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { partnerGradeApi, type PartnerGrade } from '@/api/erp/partner'
import { productApi, type Product, type ProductGradePrice } from '@/api/erp/product'
import request from '@/utils/request'
import type { VxeTableInstance } from 'vxe-table'

// ── 类型定义 ──────────────────────────────────────────────
interface PriceRow {
  productId: number
  productCode: string
  productName: string
  standardPrice: number
  gradeOriginalPrice: number
  price: number | undefined
  enabled: boolean
  productGradePriceId?: number
}

interface PartnerGradePrice {
  productGradePriceId: number
  price: number
  isActive: number
}

interface ExportItem {
  序号: number
  产品编码: string
  产品名称: string
  标准售价: number
  等级基准价: number
  客户等级售价: number | undefined
  启用: string
}

/** 价格变更审计记录 */
interface PriceChangeRecord {
  productId: number
  productCode: string
  productName: string
  oldPrice: number | undefined
  newPrice: number
  timestamp: string
}

/** 导入行 */
interface ImportRow {
  productCode: string
  price: number
  valid: boolean
  error?: string
}

/** 对比矩阵行 */
interface ComparisonMatrixRow {
  productCode: string
  productName: string
  standardPrice: number
  [key: string]: string | number | undefined
}

/** 分布图分类 */
interface DistributionCategory {
  label: string
  min: number
  max: number
  count: number
  percentage: number
  barWidth: number
}

/** 表格脚注列定义 */
interface FooterColumn {
  field?: string
}

// ── 状态映射 ──────────────────────────────────────────────
const GRADE_CODE_MAP: StatusMap = {
  VIP: { text: 'VIP', color: 'gold' },
  GOLD: { text: 'GOLD', color: 'orange' },
  SILVER: { text: 'SILVER', color: 'blue' },
  BRONZE: { text: 'BRONZE', color: 'default' },
}

// ── 响应式状态 ──────────────────────────────────────────
const gradeLoading = ref(false)
const priceListLoading = ref(false)
const saving = ref(false)
const searchText = ref('')
const grades = ref<PartnerGrade[]>([])
const selectedGradeKeys = ref<string[]>([])
const selectedGrade = computed(() => {
  const id = Number(selectedGradeKeys.value[0])
  return grades.value.find(g => g.id === id) || null
})

const priceList = ref<PriceRow[]>([])
const priceTableRef = ref<VxeTableInstance>()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: number | undefined
let countdownTimer: number | undefined

const searchFields: SearchField[] = [
  { field: 'keyword', placeholder: '搜索产品编码/名称', width: 220 },
]

function handleSearch(values: Record<string, any>) {
  searchText.value = values.keyword || ''
}

function handleReset() {
  searchText.value = ''
}

function startCountdown() {
  stopCountdown()
  autoRefreshCountdown.value = 300
  countdownTimer = window.setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  refreshTimer = window.setInterval(() => {
    loadProductGradePrices()
  }, 300_000)
}

function stopCountdown() {
  if (countdownTimer !== undefined) { clearInterval(countdownTimer); countdownTimer = undefined }
  if (refreshTimer !== undefined) { clearInterval(refreshTimer); refreshTimer = undefined }
  autoRefreshCountdown.value = 0
}

// ── 统计概览 ──────────────────────────────────────────────
const stats = computed(() => {
  const total = priceList.value.length
  const configured = priceList.value.filter(r => r.enabled && r.price != null).length
  const rate = total > 0 ? Math.round((configured / total) * 100) : 0
  const coverageStyle: Record<string, string> = {}
  if (rate >= 80) coverageStyle.color = '#52c41a'
  else if (rate >= 50) coverageStyle.color = '#faad14'
  else if (rate > 0) coverageStyle.color = '#f5222d'
  return {
    totalProductCount: total,
    configuredCount: configured,
    coverageRate: rate,
    coverageStyle,
  }
})

// ── 价格分布图 ──────────────────────────────────────────────
const priceDistribution = computed<DistributionCategory[]>(() => {
  const categories = [
    { label: '< 50元', min: 0, max: 50 },
    { label: '50-100元', min: 50, max: 100 },
    { label: '100-500元', min: 100, max: 500 },
    { label: '500-1000元', min: 500, max: 1000 },
    { label: '> 1000元', min: 1000, max: Infinity },
  ]

  const prices = filteredPriceList.value
    .map(r => r.price)
    .filter((p): p is number => p != null && p >= 0)

  const result = categories.map(cat => {
    const count = prices.filter(p => p >= cat.min && p < cat.max).length
    return { ...cat, count, percentage: 0, barWidth: 0 }
  })

  const maxCount = Math.max(...result.map(c => c.count), 1)

  return result.map(cat => ({
    ...cat,
    percentage: prices.length > 0 ? Math.round((cat.count / prices.length) * 100) : 0,
    barWidth: Math.round((cat.count / maxCount) * 100),
  }))
})

// ── 价格变更追踪 ──────────────────────────────────────────
let priceSnapshot: Map<number, number | undefined> = new Map()

function takePriceSnapshot(): void {
  priceSnapshot = new Map()
  priceList.value.forEach(row => {
    priceSnapshot.set(row.productId, row.price)
  })
}

const pendingChanges = computed<PriceChangeRecord[]>(() => {
  const changes: PriceChangeRecord[] = []
  const now = new Date().toLocaleString('zh-CN')
  priceList.value.forEach(row => {
    const oldPrice = priceSnapshot.get(row.productId)
    if (oldPrice !== row.price) {
      changes.push({
        productId: row.productId,
        productCode: row.productCode,
        productName: row.productName,
        oldPrice,
        newPrice: row.price ?? 0,
        timestamp: now,
      })
    }
  })
  return changes
})

// ── 行选择状态 ──────────────────────────────────────────
const selectedRowIds = ref<number[]>([])

function onCheckboxChange({ row, checked }: { row: PriceRow; checked: boolean }) {
  if (checked) {
    if (!selectedRowIds.value.includes(row.productId)) {
      selectedRowIds.value.push(row.productId)
    }
  } else {
    selectedRowIds.value = selectedRowIds.value.filter(id => id !== row.productId)
  }
}

function onCheckboxAll({ checked, records }: { checked: boolean; records: PriceRow[] }) {
  const ids = records.map(r => r.productId)
  if (checked) {
    ids.forEach(id => {
      if (!selectedRowIds.value.includes(id)) {
        selectedRowIds.value.push(id)
      }
    })
  } else {
    selectedRowIds.value = selectedRowIds.value.filter(id => !ids.includes(id))
  }
}

const selectedRows = computed(() => priceList.value.filter(r => selectedRowIds.value.includes(r.productId)))

// ── 客户端搜索过滤 ──────────────────────────────────────
const filteredPriceList = computed(() => {
  if (!searchText.value) return priceList.value
  const keyword = searchText.value.toLowerCase()
  return priceList.value.filter(
    row => row.productCode.toLowerCase().includes(keyword) || row.productName.toLowerCase().includes(keyword)
  )
})

function updateTimestamp() {
  lastUpdateTime.value = new Date().toLocaleString('zh-CN')
}

// ── 加载等级列表 ──────────────────────────────────────
async function loadGrades() {
  gradeLoading.value = true
  try {
    grades.value = await partnerGradeApi.list('CUSTOMER')
  } catch (err: unknown) {
    console.warn('[定价配置] 加载等级失败', err)
    message.error('加载客户等级失败，请稍后重试')
  } finally {
    gradeLoading.value = false
  }
}

// ── 加载产品价格数据 ──────────────────────────────────
async function loadProductGradePrices() {
  if (!selectedGrade.value) return
  priceListLoading.value = true
  selectedRowIds.value = []
  try {
    // 1. 获取该等级已有的价格配置
    const existingPrices: PartnerGradePrice[] = await request.get(`/erp/pricing/partner-grade-prices/${selectedGrade.value.id}`)
    const priceMap = new Map<number, PartnerGradePrice>()
    existingPrices.forEach(p => priceMap.set(p.productGradePriceId, p))

    // 2. 获取所有产品及其等级价格
    const gradePrices: ProductGradePrice[] = (await request.get('/erp/product-grade-price/list')) || []
    const gradePriceMap = new Map<number, ProductGradePrice>()
    gradePrices.forEach(gp => gradePriceMap.set(gp.productId, gp))

    // 3. 获取产品列表
    const prodRes = await productApi.page({ pageNum: 1, pageSize: 500 })
    const products: Product[] = prodRes.records || []

    priceList.value = products.map(p => {
      const gp = gradePriceMap.get(p.id)
      const existing = gp ? priceMap.get(gp.id) : null
      return {
        productId: p.id,
        productCode: p.productCode,
        productName: p.productName,
        standardPrice: p.standardPrice,
        gradeOriginalPrice: gp?.price || 0,
        price: existing?.price ?? (gp?.price || undefined),
        enabled: existing ? existing.isActive !== 0 : true,
        productGradePriceId: gp?.id
      }
    })
    takePriceSnapshot()
    updateTimestamp()
    startCountdown()
  } catch (err: unknown) {
    console.warn('[定价配置] 加载产品价格数据失败', err)
    message.error('加载产品价格数据失败')
  } finally {
    priceListLoading.value = false
  }
}

function onGradeSelect({ key }: { key: string }) {
  selectedGradeKeys.value = [key]
  searchText.value = ''
  selectedRowIds.value = []
  loadProductGradePrices()
}

// ── 表格脚注汇总 ──────────────────────────────────────────
function footerMethod({ columns, data }: { columns: FooterColumn[]; data: PriceRow[] }) {
  return [columns.map((col: FooterColumn) => {
    if (col.field === 'productCode') {
      return data.length > 0 ? `共 ${data.length} 项` : ''
    }
    if (col.field && ['standardPrice', 'gradeOriginalPrice', 'price'].includes(col.field)) {
      const field = col.field as keyof PriceRow
      const vals = data.filter(r => {
        const val = r[field]
        return val != null && typeof val === 'number' && val > 0
      }).map(r => r[field] as number)
      if (vals.length === 0) return ''
      const avg = vals.reduce((s, v) => s + v, 0) / vals.length
      return `均 ¥${avg.toFixed(2)}`
    }
    return ''
  })]
}

// ── 价格颜色 ──────────────────────────────────────────────
function getPriceStyle(row: PriceRow): Record<string, string> {
  if (row.price == null || row.standardPrice == null || row.standardPrice === 0) return {}
  if (row.price < row.standardPrice) return { color: '#52c41a', fontWeight: '600' }
  if (row.price > row.standardPrice) return { color: '#f5222d', fontWeight: '600' }
  return {}
}

// ── 批量启用/禁用 ─────────────────────────────────────────
function handleBatchToggle() {
  const rows = selectedRows.value
  if (rows.length === 0) {
    message.warning('请先选择需要操作的产品')
    return
  }

  const allEnabled = rows.every(r => r.enabled)
  const action = allEnabled ? '禁用' : '启用'

  Modal.confirm({
    title: `批量${action}确认`,
    content: `确认${action}已选的 ${rows.length} 项产品？`,
    onOk: () => {
      rows.forEach(r => { r.enabled = !allEnabled })
      message.success(`已${action} ${rows.length} 项`)
    },
  })
}

// ── 整表调价 ──────────────────────────────────────────────
const priceAdjustmentVisible = ref(false)
const adjustmentScope = ref<'visible' | 'selected'>('visible')
const adjustmentType = ref<'fixed' | 'percentage'>('fixed')
const adjustmentDirection = ref<'increase' | 'decrease'>('increase')
const adjustmentValue = ref<number>(0)

function showPriceAdjustmentModal() {
  adjustmentScope.value = 'visible'
  adjustmentType.value = 'fixed'
  adjustmentDirection.value = 'increase'
  adjustmentValue.value = 0
  priceAdjustmentVisible.value = true
}

function applyPriceAdjustment() {
  const scope = adjustmentScope.value === 'visible' ? filteredPriceList.value : selectedRows.value
  if (scope.length === 0) {
    message.warning('没有可调整的产品')
    return
  }

  const value = adjustmentValue.value
  if (!value || value <= 0) {
    message.warning('请输入有效的调整值')
    return
  }

  let changedCount = 0
  scope.forEach(row => {
    const basePrice = row.price ?? row.standardPrice ?? 0
    if (basePrice <= 0) return

    let newPrice: number
    if (adjustmentType.value === 'fixed') {
      newPrice = adjustmentDirection.value === 'increase'
        ? basePrice + value
        : Math.max(0, basePrice - value)
    } else {
      newPrice = adjustmentDirection.value === 'increase'
        ? basePrice * (1 + value / 100)
        : basePrice * (1 - value / 100)
    }
    row.price = Math.round(newPrice * 100) / 100
    row.enabled = true
    changedCount++
  })

  priceAdjustmentVisible.value = false
  if (changedCount > 0) {
    message.success(`已对 ${changedCount} 项产品进行调价`)
  } else {
    message.warning('没有产品符合调价条件')
  }
}

// ── 保存 ──────────────────────────────────────────────
async function handleSave() {
  if (!selectedGrade.value) return

  const enabledItems = priceList.value.filter(row => row.enabled)
  const pendingItems = enabledItems.filter(row => row.price != null && row.productGradePriceId)
  const disabledItems = priceList.value.filter(row => !row.enabled)
  const changeCount = pendingChanges.value.length

  Modal.confirm({
    title: '确认保存价格配置',
    content: `将为等级「${selectedGrade.value.gradeName}」保存以下配置：
• 启用并待保存：${pendingItems.length} 条
• 已启用但无价格（跳过）：${enabledItems.length - pendingItems.length} 条
• 已禁用（跳过）：${disabledItems.length} 条
${changeCount > 0 ? `• 检测到 ${changeCount} 条价格变更` : ''}
确认继续保存？`,
    okText: '确认保存',
    onOk: async () => {
      saving.value = true
      try {
        const items = pendingItems.map(row => ({
          partnerGradeId: selectedGrade.value!.id,
          productGradePriceId: row.productGradePriceId,
          price: row.price,
          isActive: 1
        }))
        await request.put(`/erp/pricing/partner-grade-prices/${selectedGrade.value.id}`, items)
        message.success('价格配置保存成功')
        takePriceSnapshot()
        updateTimestamp()
        startCountdown()
      } catch (err: unknown) {
        console.warn('[定价配置] 保存失败', err)
        message.error('保存价格配置失败，请稍后重试')
      } finally {
        saving.value = false
      }
    }
  })
}

// ── 导出 ──────────────────────────────────────────────
async function handleExport() {
  if (priceList.value.length === 0) {
    message.warning('当前没有可导出的数据')
    return
  }
  try {
    const data = priceList.value.map(row => ({
      productCode: row.productCode,
      productName: row.productName,
      standardPrice: row.standardPrice,
      gradeOriginalPrice: row.gradeOriginalPrice,
      price: row.price,
      enabled: row.enabled ? '是' : '否',
    }))

    const csvHeader = '\uFEFF产品编码,产品名称,标准售价,等级基准价,客户等级售价,启用\n'
    const csvBody = data.map(row =>
      `${row.productCode},${row.productName},${row.standardPrice},${row.gradeOriginalPrice},${row.price ?? ''},${row.enabled}`
    ).join('\n')

    const blob = new Blob([csvHeader + csvBody], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `价格配置_${selectedGrade.value?.gradeName || '全部'}_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('价格配置导出成功')
  } catch (err: unknown) {
    console.warn('[定价配置] 导出失败', err)
    message.error('导出失败')
  }
}

// ── 批量导入 ──────────────────────────────────────────────
const importModalVisible = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const importPreviewData = ref<ImportRow[]>([])

const validImportCount = computed(() => importPreviewData.value.filter(r => r.valid).length)

function handleImport(): void {
  importPreviewData.value = []
  importModalVisible.value = true
}

function triggerFileInput(): void {
  fileInputRef.value?.click()
}

function onFileDrop(e: DragEvent): void {
  const files = e.dataTransfer?.files
  if (files && files.length > 0) {
    parseFile(files[0])
  }
}

function onFileSelected(e: Event): void {
  const target = e.target as HTMLInputElement
  const files = target.files
  if (files && files.length > 0) {
    parseFile(files[0])
  }
  target.value = ''
}

function parseFile(file: File): void {
  if (!file.name.endsWith('.csv')) {
    message.error('请上传 .csv 格式文件')
    return
  }

  const reader = new FileReader()
  reader.onload = (e: ProgressEvent<FileReader>) => {
    const text = e.target?.result as string
    const lines = text.split('\n').filter(line => line.trim())
    const rows: ImportRow[] = []

    // Skip header row if it contains column names
    const startIndex = lines.length > 0 && (lines[0].includes('产品编码') || lines[0].includes('productCode')) ? 1 : 0

    for (let i = startIndex; i < lines.length; i++) {
      const parts = lines[i].split(',')
      if (parts.length < 2) {
        rows.push({ productCode: '', price: 0, valid: false, error: '格式错误' })
        continue
      }
      const productCode = parts[0].trim()
      const priceStr = parts[1].trim().replace(/[¥￥,]/g, '')
      const price = parseFloat(priceStr)

      if (!productCode) {
        rows.push({ productCode: '', price: 0, valid: false, error: '产品编码为空' })
      } else if (isNaN(price) || price < 0) {
        rows.push({ productCode, price: 0, valid: false, error: '价格无效' })
      } else {
        rows.push({ productCode, price, valid: true })
      }
    }

    importPreviewData.value = rows
  }
  reader.onerror = () => {
    message.error('文件读取失败')
  }
  reader.readAsText(file, 'UTF-8')
}

function downloadTemplate(): void {
  const header = '\uFEFF产品编码,价格\n'
  const sample = 'PROD-001,100.00\nPROD-002,200.00\nPROD-003,150.00'
  const blob = new Blob([header + sample], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '价格导入模板.csv'
  a.click()
  window.URL.revokeObjectURL(url)
}

function confirmImport(): void {
  const validRows = importPreviewData.value.filter(r => r.valid)
  if (validRows.length === 0) {
    message.warning('没有有效数据可以导入')
    return
  }

  let matchedCount = 0
  validRows.forEach(importRow => {
    const existing = priceList.value.find(r => r.productCode === importRow.productCode)
    if (existing) {
      existing.price = importRow.price
      existing.enabled = true
      matchedCount++
    }
  })

  importModalVisible.value = false
  message.success(`导入完成：已匹配 ${matchedCount} 项产品价格（${validRows.length - matchedCount} 项未找到对应产品）`)
}

// ── 等级对比 ──────────────────────────────────────────────
const comparisonDrawerVisible = ref(false)
const comparisonLoading = ref(false)
const comparisonGrades = ref<PartnerGrade[]>([])
const comparisonData = ref<ComparisonMatrixRow[]>([])

function getComparisonPriceStyle(price: number, standardPrice: number): Record<string, string> {
  if (price == null || standardPrice == null || standardPrice === 0) return {}
  if (price < standardPrice) return { color: '#52c41a', fontWeight: '600' }
  if (price > standardPrice) return { color: '#f5222d', fontWeight: '600' }
  return {}
}

async function handleShowComparison(): Promise<void> {
  if (grades.value.length === 0) {
    message.warning('请先加载客户等级')
    return
  }
  comparisonDrawerVisible.value = true
  comparisonLoading.value = true

  try {
    comparisonGrades.value = grades.value

    // Load price data for ALL grades
    const allGradePrices: Map<number, Map<number, number>> = new Map()

    for (const grade of grades.value) {
      const prices: PartnerGradePrice[] = await request.get(`/erp/pricing/partner-grade-prices/${grade.id}`)
      const priceByProductGradePriceId = new Map<number, number>()
      prices.forEach((p: PartnerGradePrice) => {
        priceByProductGradePriceId.set(p.productGradePriceId, p.price)
      })
      allGradePrices.set(grade.id, priceByProductGradePriceId)
    }

    // Get product-grade-price mapping to link productId -> gradeId -> price
    const gradePrices: ProductGradePrice[] = (await request.get('/erp/product-grade-price/list')) || []
    const productGradePriceMap = new Map<number, Map<number, number>>()
    gradePrices.forEach(gp => {
      if (!productGradePriceMap.has(gp.productId)) {
        productGradePriceMap.set(gp.productId, new Map())
      }
      productGradePriceMap.get(gp.productId)!.set(gp.gradeId, gp.price)
    })

    // Get all products
    const prodRes = await productApi.page({ pageNum: 1, pageSize: 500 })
    const products: Product[] = prodRes.records || []

    // Build comparison matrix
    comparisonData.value = products.map(product => {
      const row: ComparisonMatrixRow = {
        productCode: product.productCode,
        productName: product.productName,
        standardPrice: product.standardPrice,
      }

      grades.value.forEach(grade => {
        const gradePricesForProduct = productGradePriceMap.get(product.id)
        const priceForGrade = gradePricesForProduct?.get(grade.id)
        row['grade_' + grade.id] = priceForGrade
      })

      return row
    })
  } catch (err: unknown) {
    console.warn('[定价配置] 加载对比数据失败', err)
    message.error('加载对比数据失败')
  } finally {
    comparisonLoading.value = false
  }
}

// ── 季节性调价 ──────────────────────────────────────────
const seasonalAdjustmentVisible = ref(false)
const seasonalPercentage = ref<number>(0)
const seasonalGradeIds = ref<number[]>([])
const seasonalEffectiveDate = ref<string>('')

const seasonalTargetCount = computed(() => filteredPriceList.value.length)

function showSeasonalAdjustmentModal(): void {
  seasonalPercentage.value = 0
  seasonalGradeIds.value = selectedGrade.value ? [selectedGrade.value.id] : []
  seasonalEffectiveDate.value = ''
  seasonalAdjustmentVisible.value = true
}

function applySeasonalAdjustment(): void {
  const percentage = seasonalPercentage.value
  if (!percentage || percentage <= 0) {
    message.warning('请输入有效的调价比例')
    return
  }

  if (seasonalGradeIds.value.length === 0) {
    message.warning('请选择至少一个生效等级')
    return
  }

  const targetProducts = filteredPriceList.value
  if (targetProducts.length === 0) {
    message.warning('当前没有可调整的产品')
    return
  }

  let changedCount = 0
  targetProducts.forEach(row => {
    const basePrice = row.price ?? row.standardPrice ?? 0
    if (basePrice <= 0) return
    row.price = Math.round(basePrice * (1 + percentage / 100) * 100) / 100
    row.enabled = true
    changedCount++
  })

  seasonalAdjustmentVisible.value = false

  const gradeNames = seasonalGradeIds.value
    .map(id => grades.value.find(g => g.id === id)?.gradeName || `ID:${id}`)
    .join(', ')

  message.success(`季节性调价完成：对 ${changedCount} 项产品调价 ${percentage}%（等级：${gradeNames}${seasonalEffectiveDate.value ? `，生效日期：${seasonalEffectiveDate.value}` : ''}）`)
}

// ── 变更记录 ──────────────────────────────────────────────
const showChangeHistoryDrawer = ref(false)
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleError(err: unknown) { console.warn('[定价配置] ErrorBoundary 捕获异常:', err) }

function handleKeydown(e: KeyboardEvent) {
  const isInput = e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement
  if (e.key === 'F5' && !isInput) {
    e.preventDefault(); debounceClick('refresh', loadProductGradePrices)
  }
  // Ctrl+E: 导出CSV
  if ((e.key === 'e' || e.key === 'E') && (e.ctrlKey || e.metaKey) && !isInput) {
    e.preventDefault(); handleExport()
  }
  // Ctrl+F / Ctrl+N: 聚焦搜索框
  if (['f', 'F', 'n', 'N'].includes(e.key) && (e.ctrlKey || e.metaKey) && !isInput) {
    e.preventDefault()
    const input = document.querySelector<HTMLInputElement>('.pricing-toolbar input, .pricing-toolbar .ant-input')
    input?.focus()
  }
}

onMounted(() => { loadGrades(); document.addEventListener('keydown', handleKeydown); startCountdown() })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown); stopCountdown() })
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__breadcrumb { font-size: 12px; color: #999; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }

.pricing-toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.countdown-hint {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.selected-count {
  font-size: 12px;
  color: #1890ff;
  white-space: nowrap;
}
.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.stat-label {
  font-size: 12px;
  color: #999;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

/* 价格分布图 */
.distribution-chart {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.distribution-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.distribution-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.distribution-bar-label {
  width: 80px;
  font-size: 12px;
  color: #666;
  text-align: right;
  flex-shrink: 0;
}
.distribution-bar-track {
  flex: 1;
  height: 20px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.distribution-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1890ff, #40a9ff);
  border-radius: 4px;
  transition: width 0.3s ease;
  min-width: 2px;
}
.distribution-bar-count {
  width: 50px;
  font-size: 12px;
  color: #333;
  text-align: right;
  flex-shrink: 0;
}
.distribution-bar-pct {
  width: 40px;
  font-size: 12px;
  color: #999;
  text-align: right;
  flex-shrink: 0;
}

/* 导入弹窗 */
.import-modal {
  padding: 8px 0;
}
.import-upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s;
}
.import-upload-area:hover {
  border-color: #1890ff;
}
.import-summary {
  margin-bottom: 8px;
}
.import-actions {
  margin-top: 16px;
  text-align: right;
}
</style>
