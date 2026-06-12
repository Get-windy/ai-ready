<template>
  <ErrorBoundary @error="handleError"><PageContainer title="销售分析报表" full-height>
    <!-- 状态栏 -->
    <template #headerExtra>
      <a-space :size="12">
        <!-- 对比期选择 -->
        <a-select v-model:value="comparePeriod" style="width: 120px" size="small" @change="onComparePeriodChange">
          <a-select-option value="none">无对比</a-select-option>
          <a-select-option value="prev_month">环比上月</a-select-option>
          <a-select-option value="prev_year">同比去年</a-select-option>
        </a-select>
        <a-divider type="vertical" />
        <span class="data-status">
          <StatusTag :status="dataFreshness" :map="DATA_FRESHNESS" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-tooltip title="自动刷新 (每60秒)">
          <a-switch v-model:checked="autoRefresh" size="small" />
        </a-tooltip>
        <a-tooltip title="F5 刷新 | Ctrl+E 导出 | Ctrl+N 新建">
          <a-button size="small" @click="debounceClick('refresh', loadData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-tooltip>
        <span class="shortcut-hints">
          <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新建</span>
          <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
        </span>
        <a-tooltip title="Ctrl+N 新建分析">
          <a-button size="small" v-permission="'erp:sales:create'" @click="debounceClick('create', handleCreate)">
            <template #icon><PlusOutlined /></template>
            新建
          </a-button>
        </a-tooltip>
        <PrintButton page-code="erp/sales-analysis" button-size="small" tooltip="打印当前报表" />
        <a-button size="small" @click="showExportModal = true">
          <template #icon><ExportOutlined /></template>
          导出
        </a-button>
      </a-space>
    </template>

    <!-- 搜索筛选 -->
    <template #filter>
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleResetFilter"
        :expandable="false"
        :show-result-count="false"
      />
    </template>

    <!-- 错误提示 -->
    <div v-if="hasError" class="error-banner">
      <a-alert
        type="error"
        message="数据加载失败"
        description="系统异常，请检查网络连接后重试"
        show-icon
        closable
        @close="hasError = false"
      >
        <template #action>
          <a-button size="small" type="primary" @click="loadData">
            <template #icon><ReloadOutlined /></template>
            重试
          </a-button>
        </template>
      </a-alert>
    </div>

    <!-- 汇总统计卡片 -->
    <div v-if="!hasError" class="summary-cards">
      <a-row :gutter="16">
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
              <DollarOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">销售总额</div>
              <div class="summary-value">¥{{ formatAmount(summary.totalAmount) }}</div>
              <div class="summary-change positive" v-if="summary.totalGrowth">
                <ArrowUpOutlined /> {{ summary.totalGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
              <ShoppingOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">订单数量</div>
              <div class="summary-value">{{ summary.orderCount }} 单</div>
              <div class="summary-change positive" v-if="summary.orderGrowth">
                <ArrowUpOutlined /> {{ summary.orderGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
              <TeamOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">平均客单价</div>
              <div class="summary-value">¥{{ formatAmount(summary.avgOrderAmount) }}</div>
              <div class="summary-change positive" v-if="summary.avgGrowth">
                <ArrowUpOutlined /> {{ summary.avgGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card highlight">
            <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
              <PercentageOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">毛利率</div>
              <div class="summary-value">{{ summary.grossMargin.toFixed(1) }}%</div>
              <div class="summary-change positive" v-if="summary.marginGrowth">
                <ArrowUpOutlined /> {{ summary.marginGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- Tab 内容区 -->
    <div v-if="!hasError" class="tab-content">
      <a-tabs v-model:activeKey="activeTab" type="card" size="small">
        <a-tab-pane key="overview" tab="销售概览">
          <template v-if="!hasChartData">
            <EmptyState title="暂无概览数据" description="当前筛选条件下没有销售概览数据" :show-add="false" />
          </template>
          <template v-else>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-card title="销售趋势" size="small" :loading="chartLoading">
                  <div ref="trendChartRef" class="chart-container"></div>
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card title="销售渠道分布" size="small" :loading="chartLoading">
                  <div ref="channelChartRef" class="chart-container"></div>
                </a-card>
              </a-col>
            </a-row>
            <a-row :gutter="16" style="margin-top: 16px">
              <a-col :span="12">
                <a-card title="月度对比" size="small" :loading="chartLoading">
                  <div ref="monthlyChartRef" class="chart-container"></div>
                </a-card>
              </a-col>
              <a-col :span="12">
                <a-card title="同比环比分析" size="small" :loading="chartLoading">
                  <div ref="compareChartRef" class="chart-container"></div>
                </a-card>
              </a-col>
            </a-row>
          </template>
        </a-tab-pane>

        <a-tab-pane key="customer" tab="客户分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="客户等级分布" size="small" :loading="chartLoading">
                <div ref="customerLevelChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户来源分析" size="small" :loading="chartLoading">
                <div ref="customerSourceChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户地区分布" size="small" :loading="chartLoading">
                <div ref="customerRegionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="客户销售排行 TOP10" size="small" style="margin-top: 16px">
            <div v-if="sectionErrors.customerRank" class="section-error-banner">
              <a-alert type="error" message="客户排行加载失败" description="系统异常，请重试" show-icon closable @close="clearSectionError('customerRank')">
                <template #action>
                  <a-button size="small" type="primary" @click="retrySection('customerRank')"><ReloadOutlined /> 重试</a-button>
                </template>
              </a-alert>
            </div>
            <div v-else-if="customerRankData.length === 0" class="empty-table-placeholder">
              <EmptyState title="暂无客户排行数据" description="当前筛选条件下没有客户排行数据" size="small" show-actions :show-add="false" :show-refresh="true" @refresh="loadData" />
            </div>
            <div v-else class="ranking-table-container">
              <VxeTableList
                :columns="customerRankVxeColumns"
                :data-source="customerRankData"
                :pagination="noPagination"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
                </template>
                <template #customerTypeCell="{ record }">
                  <StatusTag :status="record.customerType" :map="CUSTOMER_TYPE_MAP" />
                </template>
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #growthCell="{ record }">
                  <span :class="['growth-cell', { positive: record.growth > 0, negative: record.growth < 0 }]">
                    <ArrowUpOutlined v-if="record.growth > 0" />
                    <ArrowDownOutlined v-if="record.growth < 0" />
                    {{ Math.abs(record.growth) }}%
                  </span>
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="product" tab="产品分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="产品类别销售" size="small" :loading="chartLoading">
                <div ref="productCategoryChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品销量分布" size="small" :loading="chartLoading">
                <div ref="productVolumeChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品毛利率分布" size="small" :loading="chartLoading">
                <div ref="productMarginChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="产品销售排行 TOP10" size="small" style="margin-top: 16px">
            <div v-if="sectionErrors.productRank" class="section-error-banner">
              <a-alert type="error" message="产品排行加载失败" description="系统异常，请重试" show-icon closable @close="clearSectionError('productRank')">
                <template #action>
                  <a-button size="small" type="primary" @click="retrySection('productRank')"><ReloadOutlined /> 重试</a-button>
                </template>
              </a-alert>
            </div>
            <div v-else-if="productRankData.length === 0" class="empty-table-placeholder">
              <EmptyState title="暂无产品排行数据" description="当前筛选条件下没有产品排行数据" size="small" show-actions :show-add="false" :show-refresh="true" @refresh="loadData" />
            </div>
            <div v-else class="ranking-table-container">
              <VxeTableList
                :columns="productRankVxeColumns"
                :data-source="productRankData"
                :pagination="noPagination"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
                </template>
                <template #trendCell="{ record }">
                  <span :class="['trend-cell', { up: (record.trend ?? 0) > 0, down: (record.trend ?? 0) < 0, flat: (record.trend ?? 0) === 0 }]">
                    <ArrowUpOutlined v-if="(record.trend ?? 0) > 0" />
                    <ArrowDownOutlined v-if="(record.trend ?? 0) < 0" />
                    <span v-if="(record.trend ?? 0) === 0">--</span>
                    <span v-if="(record.trend ?? 0) !== 0">{{ Math.abs(record.trend ?? 0) }}%</span>
                  </span>
                </template>
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #marginCell="{ record }">
                  <a-progress
                    :percent="record.margin"
                    :stroke-color="getMarginColor(record.margin)"
                    size="small"
                    :show-info="true"
                    :format="(p: number) => `${p}%`"
                  />
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="salesperson" tab="销售人员分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="销售人员业绩" size="small" :loading="chartLoading">
                <div ref="salespersonChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="销售人员目标达成率" size="small" :loading="chartLoading">
                <div ref="targetChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="销售人员业绩排行" size="small" style="margin-top: 16px">
            <div v-if="sectionErrors.salespersonRank" class="section-error-banner">
              <a-alert type="error" message="人员排行加载失败" description="系统异常，请重试" show-icon closable @close="clearSectionError('salespersonRank')">
                <template #action>
                  <a-button size="small" type="primary" @click="retrySection('salespersonRank')"><ReloadOutlined /> 重试</a-button>
                </template>
              </a-alert>
            </div>
            <div v-else-if="salespersonRankData.length === 0" class="empty-table-placeholder">
              <EmptyState title="暂无人员排行数据" description="当前筛选条件下没有人员排行数据" size="small" show-actions :show-add="false" :show-refresh="true" @refresh="loadData" />
            </div>
            <div v-else class="ranking-table-container">
              <VxeTableList
                :columns="salespersonRankVxeColumns"
                :data-source="salespersonRankData"
                :pagination="noPagination"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
                </template>
                <template #trendCell="{ record }">
                  <span :class="['trend-cell', { up: (record.trend ?? 0) > 0, down: (record.trend ?? 0) < 0, flat: (record.trend ?? 0) === 0 }]">
                    <ArrowUpOutlined v-if="(record.trend ?? 0) > 0" />
                    <ArrowDownOutlined v-if="(record.trend ?? 0) < 0" />
                    <span v-if="(record.trend ?? 0) === 0">--</span>
                    <span v-if="(record.trend ?? 0) !== 0">{{ Math.abs(record.trend ?? 0) }}%</span>
                  </span>
                </template>
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #targetRateCell="{ record }">
                  <a-progress
                    :percent="record.targetRate"
                    :stroke-color="getTargetColor(record.targetRate)"
                    size="small"
                    :show-info="true"
                    :format="(p: number) => `${p}%`"
                  />
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="region" tab="区域分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="区域销售分布" size="small" :loading="chartLoading">
                <div ref="regionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="区域增长对比" size="small" :loading="chartLoading">
                <div ref="regionGrowthChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="区域销售明细" size="small" style="margin-top: 16px">
            <div v-if="sectionErrors.region" class="section-error-banner">
              <a-alert type="error" message="区域数据加载失败" description="系统异常，请重试" show-icon closable @close="clearSectionError('region')">
                <template #action>
                  <a-button size="small" type="primary" @click="retrySection('region')"><ReloadOutlined /> 重试</a-button>
                </template>
              </a-alert>
            </div>
            <div v-else-if="regionData.length === 0" class="empty-table-placeholder">
              <EmptyState title="暂无区域数据" description="当前筛选条件下没有区域销售数据" size="small" show-actions :show-add="false" :show-refresh="true" @refresh="loadData" />
            </div>
            <div v-else class="ranking-table-container">
              <VxeTableList
                :columns="regionVxeColumns"
                :data-source="regionData"
                :pagination="noPagination"
                row-key="name"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #growthCell="{ record }">
                  <span :class="['growth-cell', { positive: record.growth > 0, negative: record.growth < 0 }]">
                    <ArrowUpOutlined v-if="record.growth > 0" />
                    <ArrowDownOutlined v-if="record.growth < 0" />
                    {{ Math.abs(record.growth) }}%
                  </span>
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="time" tab="时间分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="每日销售趋势" size="small" :loading="chartLoading">
                <div ref="dailyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="每周销售对比" size="small" :loading="chartLoading">
                <div ref="weeklyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="时段销售分布" size="small" :loading="chartLoading">
                <div ref="hourlyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </div>
    <!-- 新建分析对话框 -->
    <a-modal
      v-model:open="showCreateModal"
      title="新建分析"
      ok-text="创建"
      cancel-text="取消"
      @ok="confirmCreate"
    >
      <a-input
        v-model:value="newAnalysisName"
        placeholder="请输入分析名称"
        @press-enter="confirmCreate"
      />
    </a-modal>

    <!-- 导出报表对话框 -->
    <a-modal
      v-model:open="showExportModal"
      title="导出报表"
      ok-text="导出"
      cancel-text="取消"
      @ok="confirmExportWithOptions"
      :confirm-loading="exportLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="导出格式">
          <a-radio-group v-model:value="exportOptions.format">
            <a-radio value="xlsx">Excel (.xlsx)</a-radio>
            <a-radio value="csv">CSV (.csv)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="日期范围">
          <a-range-picker
            v-model:value="exportOptions.dateRange"
            :placeholder="['默认使用当前筛选日期', '默认使用当前筛选日期']"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="包含标签页">
          <a-checkbox-group v-model:value="exportOptions.includeTabs">
            <a-checkbox value="overview">销售概览</a-checkbox>
            <a-checkbox value="customer">客户分析</a-checkbox>
            <a-checkbox value="product">产品分析</a-checkbox>
            <a-checkbox value="salesperson">销售人员分析</a-checkbox>
            <a-checkbox value="region">区域分析</a-checkbox>
            <a-checkbox value="time">时间分析</a-checkbox>
          </a-checkbox-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 下钻明细抽屉 -->
    <a-drawer
      v-model:open="drillDownVisible"
      :title="drillDownTitle"
      placement="right"
      width="560"
      @close="drillDownVisible = false"
    >
      <template v-if="drillDownLoading">
        <a-skeleton active :paragraph="{ rows: 6 }" />
      </template>
      <template v-else>
        <VxeTableList
          v-if="drillDownData.length > 0"
          :columns="drillDownColumns"
          :data-source="drillDownData"
          :pagination="noPagination"
          row-key="productName"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #drillAmountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
          </template>
          <template #drillMarginCell="{ record }">
            {{ record.margin?.toFixed?.(1) ?? 0 }}%
          </template>
        </VxeTableList>
        <EmptyState v-else title="暂无明细数据" description="当前筛选条件下没有明细数据" :show-add="false" />
      </template>
    </a-drawer>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import * as echarts from 'echarts'
import {
  ReloadOutlined,
  ExportOutlined,
  DollarOutlined,
  ShoppingOutlined,
  TeamOutlined,
  PercentageOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  WarningOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import type { StatusMap } from '@/utils/statusConfig'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import { salesAnalysisApi, type SalesOverview, type CustomerRankItem, type ProductRankItem, type SalespersonRankItem, type RegionItem, type DrillDownItem, type CompareData, type SalesAnalysisQuery } from '@/api/sales-analysis'

// ── 类型定义 ──────────────────────────────────────────────
interface SummaryData extends SalesOverview {
  totalGrowth?: number
  orderGrowth?: number
  avgGrowth?: number
  marginGrowth?: number
}

interface ExportOptions {
  format: 'xlsx' | 'csv'
  dateRange?: any
  includeTabs: string[]
}

// ── 状态映射表 ─────────────────────────────────────────────
const DATA_FRESHNESS: StatusMap = {
  fresh: { text: '数据正常', color: 'success' },
  error: { text: '数据异常', color: 'error' },
  loading: { text: '加载中', color: 'processing' },
}

const CUSTOMER_TYPE_MAP: StatusMap = {
  A: { text: 'A类客户', color: 'red' },
  B: { text: 'B类客户', color: 'orange' },
  C: { text: 'C类客户', color: 'blue' },
}

// ── ErrorBoundary 回调 ────────────────────────────────────
function handleError(err: unknown) {
  console.warn("[销售分析] ErrorBoundary 捕获异常:", err)
}

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', loadData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); showExportModal.value = true; return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('create', handleCreate); return }
}

// ── 搜索字段配置 ─────────────────────────────────────────
const searchForm = reactive({
  dateRange: [] as any[],
  warehouseId: undefined as number | undefined,
  salespersonId: undefined as number | undefined,
  customerType: undefined as string | undefined,
})

const searchFields = computed<SearchField[]>(() => [
  {
    name: 'dateRange',
    label: '日期范围',
    type: 'date-range',
  },
  {
    name: 'warehouseId',
    label: '仓库',
    type: 'select',
    placeholder: '全部仓库',
    options: warehouses.value.map(w => ({ label: w.name, value: w.id })),
  },
  {
    name: 'salespersonId',
    label: '销售人员',
    type: 'select',
    placeholder: '全部人员',
    options: salespersons.value.map(sp => ({ label: sp.name, value: sp.id })),
  },
  {
    name: 'customerType',
    label: '客户类型',
    type: 'select',
    placeholder: '全部类型',
    options: [
      { label: 'A类客户', value: 'A' },
      { label: 'B类客户', value: 'B' },
      { label: 'C类客户', value: 'C' },
    ],
  },
])

// ── 响应式状态 ──────────────────────────────────────────
const activeTab = ref('overview')
const filterExpanded = ref<string[]>([])
const dateRange = ref<any[]>([])
const selectedWarehouse = ref<number>()
const selectedSalesperson = ref<number>()
const selectedCustomerType = ref<string>()
const loading = ref(false)
const hasError = ref(false)
const chartLoading = ref(false)
const autoRefresh = ref(false)
const lastUpdateTime = ref<string>('')
const showCreateModal = ref(false)
const newAnalysisName = ref('')

// ── 对比期状态 ──────────────────────────────────────
const comparePeriod = ref<'none' | 'prev_month' | 'prev_year'>('none')
const compareData = ref<CompareData | null>(null)
const compareLoading = ref(false)

// ── 导出选项状态 ──────────────────────────────────────
const showExportModal = ref(false)
const exportLoading = ref(false)
const exportOptions = reactive<ExportOptions>({
  format: 'xlsx',
  dateRange: null,
  includeTabs: ['overview', 'customer', 'product', 'salesperson', 'region', 'time'],
})

// ── 下钻状态 ────────────────────────────────────────
const drillDownVisible = ref(false)
const drillDownTitle = ref('')
const drillDownLoading = ref(false)
const drillDownData = ref<DrillDownItem[]>([])

// ── 无分页标记 ─────────────────────────────────────
const noPagination: any = false

// ── 分区块错误状态 ──────────────────────────────────
const sectionErrors = reactive<Record<string, boolean>>({
  summary: false,
  charts: false,
  customerRank: false,
  productRank: false,
  salespersonRank: false,
  region: false,
})

const clearSectionError = (key: string) => { sectionErrors[key] = false }

// ── 分区块重试 ──────────────────────────────────────
const retrySection = async (section: string) => {
  sectionErrors[section] = false
  if (section === 'summary') {
    await loadSummary()
  } else if (['customerRank', 'productRank', 'salespersonRank', 'region'].includes(section)) {
    await loadRankingData()
  } else if (section === 'charts') {
    sectionErrors.charts = false
    await nextTick()
    initAllCharts()
  }
}

const handleCreate = () => {
  showCreateModal.value = true
  newAnalysisName.value = ''
}

const confirmCreate = () => {
  const name = newAnalysisName.value.trim()
  if (!name) {
    message.warning('请输入分析名称')
    return
  }
  showCreateModal.value = false
  message.success(`已创建分析「${name}」`)
  handleResetFilter()
}

const warehouses = ref<{ id: number; name: string }[]>([])
const salespersons = ref<{ id: number; name: string }[]>([])

const summary = ref<SummaryData>({
  totalAmount: 0,
  orderCount: 0,
  avgOrderAmount: 0,
  grossMargin: 0,
})

const dataFreshness = computed(() => {
  if (loading.value) return 'loading'
  if (hasError.value) return 'error'
  return 'fresh'
})

const hasChartData = computed(() => !chartLoading.value && !hasError.value)

let refreshTimer: ReturnType<typeof setInterval> | null = null

// 表格列配置
const customerRankVxeColumns: any = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '客户名称', width: 150 },
  { field: 'customerType', title: '客户类型', width: 100, align: 'center', slotName: 'customerTypeCell' },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'growth', title: '同比增长', width: 100, align: 'right', slotName: 'growthCell' },
])

const productRankVxeColumns: any = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '产品名称', width: 150 },
  { field: 'trend', title: '趋势', width: 80, align: 'center', slotName: 'trendCell' },
  { field: 'volume', title: '销量', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'margin', title: '毛利率', width: 150, slotName: 'marginCell' },
])

const salespersonRankVxeColumns: any = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '销售人员', width: 130 },
  { field: 'trend', title: '趋势', width: 80, align: 'center', slotName: 'trendCell' },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'targetRate', title: '目标达成率', width: 150, slotName: 'targetRateCell' },
])

const regionVxeColumns: any = computed(() => [
  { field: 'name', title: '区域', width: 120 },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'growth', title: '同比增长', width: 100, align: 'right', slotName: 'growthCell' },
])

// 下钻明细列配置
const drillDownColumns = [
  { field: 'productName', title: '产品名称', width: 180 },
  { field: 'quantity', title: '数量', width: 80, align: 'right' },
  { field: 'amount', title: '金额', width: 140, align: 'right', slotName: 'drillAmountCell' },
  { field: 'margin', title: '毛利率', width: 120, align: 'right', slotName: 'drillMarginCell' },
]

// 数据源
const customerRankData = ref<CustomerRankItem[]>([])
const productRankData = ref<ProductRankItem[]>([])
const salespersonRankData = ref<SalespersonRankItem[]>([])
const regionData = ref<RegionItem[]>([])

// 图表引用
const trendChartRef = ref<HTMLElement>()
const channelChartRef = ref<HTMLElement>()
const monthlyChartRef = ref<HTMLElement>()
const compareChartRef = ref<HTMLElement>()
const customerLevelChartRef = ref<HTMLElement>()
const customerSourceChartRef = ref<HTMLElement>()
const customerRegionChartRef = ref<HTMLElement>()
const productCategoryChartRef = ref<HTMLElement>()
const productVolumeChartRef = ref<HTMLElement>()
const productMarginChartRef = ref<HTMLElement>()
const salespersonChartRef = ref<HTMLElement>()
const targetChartRef = ref<HTMLElement>()
const regionChartRef = ref<HTMLElement>()
const regionGrowthChartRef = ref<HTMLElement>()
const dailyChartRef = ref<HTMLElement>()
const weeklyChartRef = ref<HTMLElement>()
const hourlyChartRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

// ── 加载选项数据 ──────────────────────────────────────
const loadOptions = async () => {
  try {
    const whRes = await salesAnalysisApi.getWarehouses()
    warehouses.value = whRes.data || []
  } catch (err) {
    console.warn('[销售分析报表] 加载仓库选项失败', err)
    warehouses.value = []
  }
  try {
    const spRes = await salesAnalysisApi.getSalespersons()
    salespersons.value = spRes.data || []
  } catch (err) {
    console.warn('[销售分析报表] 加载销售人员选项失败', err)
    salespersons.value = []
  }
}

// ── 构建查询参数 ──────────────────────────────────────
const buildParams = (): any => ({
  warehouseId: selectedWarehouse.value,
  salespersonId: selectedSalesperson.value ? String(selectedSalesperson.value) : undefined,
  customerType: selectedCustomerType.value,
  dateRange: dateRange.value?.length === 2
    ? [dateRange.value[0]?.format?.('YYYY-MM-DD') || dateRange.value[0], dateRange.value[1]?.format?.('YYYY-MM-DD') || dateRange.value[1]]
    : undefined
})

// ── 加载汇总数据 ──────────────────────────────────────
const loadSummary = async () => {
  sectionErrors.summary = false
  try {
    const res = await salesAnalysisApi.getOverview(buildParams())
    if (res.data) {
      summary.value = { ...summary.value, ...res.data }
    }
  } catch (err) {
    console.warn('[销售分析报表] 加载汇总数据失败', err)
    sectionErrors.summary = true
    throw err
  }
}

// ── 加载对比期数据 ────────────────────────────────────
const loadCompareData = async () => {
  if (comparePeriod.value === 'none') {
    compareData.value = null
    return
  }
  compareLoading.value = true
  try {
    const res = await salesAnalysisApi.getCompareData({
      ...buildParams(),
      compareType: comparePeriod.value,
    })
    compareData.value = res.data || null
    // 将对比变化更新到汇总卡片的 growth 字段
    if (compareData.value) {
      const ch = compareData.value.changes
      summary.value.totalGrowth = ch.totalAmountChange
      summary.value.orderGrowth = ch.orderCountChange
      summary.value.avgGrowth = ch.avgAmountChange
      summary.value.marginGrowth = ch.marginChange
    }
  } catch (err) {
    console.warn('[销售分析报表] 加载对比期数据失败', err)
    compareData.value = null
  } finally {
    compareLoading.value = false
  }
}

// ── 加载排行数据（含趋势标记）─────────────────────────
const loadRankingData = async () => {
  const params = buildParams()
  try {
    const cr = await salesAnalysisApi.getCustomerRanking(params)
    customerRankData.value = (cr.data || []).map(item => ({
      ...item,
      trend: item.trend ?? (item.growth > 0 ? 1 : item.growth < 0 ? -1 : 0),
    }))
    sectionErrors.customerRank = false
  } catch (err) {
    console.warn('[销售分析报表] 加载客户排行失败', err)
    customerRankData.value = []
    sectionErrors.customerRank = true
  }
  try {
    const pr = await salesAnalysisApi.getProductRanking(params)
    productRankData.value = (pr.data || []).map(item => ({
      ...item,
      trend: item.trend ?? (item.margin >= 20 ? 1 : item.margin >= 10 ? 0 : -1),
    }))
    sectionErrors.productRank = false
  } catch (err) {
    console.warn('[销售分析报表] 加载产品排行失败', err)
    productRankData.value = []
    sectionErrors.productRank = true
  }
  try {
    const sr = await salesAnalysisApi.getSalespersonRanking(params)
    salespersonRankData.value = (sr.data || []).map(item => ({
      ...item,
      trend: item.trend ?? (item.targetRate >= 80 ? 1 : item.targetRate >= 60 ? 0 : -1),
    }))
    sectionErrors.salespersonRank = false
  } catch (err) {
    console.warn('[销售分析报表] 加载销售人员排行失败', err)
    salespersonRankData.value = []
    sectionErrors.salespersonRank = true
  }
  try {
    regionData.value = []
    sectionErrors.region = false
  } catch (err) {
    console.warn('[销售分析报表] 加载区域数据失败', err)
    sectionErrors.region = true
  }
}

// ── 加载所有数据 ──────────────────────────────────────
const loadData = async () => {
  hasError.value = false
  loading.value = true
  chartLoading.value = true
  const hide = message.loading('正在加载数据...', 0)
  try {
    await Promise.allSettled([loadSummary(), loadRankingData(), loadCompareData()])
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    message.success('数据已更新')
  } catch (err) {
    hasError.value = true
    console.warn('[销售分析报表] 数据加载失败', err)
    message.error('数据加载失败，请稍后重试')
  } finally {
    loading.value = false
    hide()
    await nextTick()
    if (!hasError.value) {
      initAllCharts()
    }
    chartLoading.value = false
  }
}

// ── 对比期切换 ────────────────────────────────────────
const onComparePeriodChange = () => {
  loadCompareData()
}

// ── 搜索/重置 ────────────────────────────────────────
const handleSearch = (formData: Record<string, any>) => {
  dateRange.value = formData.dateRange || []
  selectedWarehouse.value = formData.warehouseId
  selectedSalesperson.value = formData.salespersonId
  selectedCustomerType.value = formData.customerType
  loadData()
}

const handleResetFilter = () => {
  dateRange.value = []
  selectedWarehouse.value = undefined
  selectedSalesperson.value = undefined
  selectedCustomerType.value = undefined
  loadData()
}

// ── 导出报表 ──────────────────────────────────────────
const exportReport = async (format: 'xlsx' | 'csv', tabs: string[]) => {
  const hide = message.loading('正在导出销售分析报表...', 0)
  try {
    // 尝试调用后端导出 API（blob 下载）
    const ext = format === 'xlsx' ? 'xlsx' : 'csv'
    const mimeType = format === 'xlsx'
      ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      : 'text/csv'

    const blobData: Blob = await salesAnalysisApi.exportReport({
      ...buildParams(),
      format,
      includeTabs: tabs,
    })

    const blob = blobData.type ? blobData : new Blob([blobData], { type: mimeType })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售分析_${dayjs().format('YYYYMMDD')}.${ext}`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: unknown) {
    console.warn('[销售分析] 导出失败，降级为前端 JSON 导出', error)
    // 降级：前端生成 JSON 文件
    try {
      const data = {
        导出时间: new Date().toLocaleString('zh-CN'),
        筛选条件: {
          日期范围: dateRange.value?.length === 2
            ? `${dateRange.value[0]?.format?.('YYYY-MM-DD') || dateRange.value[0]} ~ ${dateRange.value[1]?.format?.('YYYY-MM-DD') || dateRange.value[1]}`
            : '全部',
          仓库: selectedWarehouse.value ? warehouses.value.find(w => w.id === selectedWarehouse.value)?.name : '全部',
          销售人员: selectedSalesperson.value ? salespersons.value.find(sp => sp.id === selectedSalesperson.value)?.name : '全部',
          客户类型: selectedCustomerType.value ? CUSTOMER_TYPE_MAP[selectedCustomerType.value]?.text : '全部',
        },
        汇总: {
          销售总额: summary.value.totalAmount,
          订单数量: summary.value.orderCount,
          平均客单价: summary.value.avgOrderAmount,
          毛利率: summary.value.grossMargin,
        },
        客户排行: customerRankData.value,
        产品排行: productRankData.value,
        人员排行: salespersonRankData.value,
      }
      const jsonStr = JSON.stringify(data, null, 2)
      const fallbackBlob = new Blob(['\ufeff' + jsonStr], { type: 'application/json;charset=utf-8' })
      const fallbackUrl = window.URL.createObjectURL(fallbackBlob)
      const fallbackA = document.createElement('a')
      fallbackA.href = fallbackUrl
      fallbackA.download = `销售分析报表_${dayjs().format('YYYYMMDD')}.json`
      fallbackA.click()
      window.URL.revokeObjectURL(fallbackUrl)
      message.warning('后端导出不可用，已降级为 JSON 导出')
    } catch (fallbackErr) {
      console.warn('[销售分析] 降级导出也失败', fallbackErr)
      message.error('导出失败')
    }
  } finally {
    hide()
  }
}

const confirmExportWithOptions = () => {
  if (exportOptions.includeTabs.length === 0) {
    message.warning('请至少选择一个标签页')
    return
  }
  exportLoading.value = true
  exportReport(exportOptions.format, exportOptions.includeTabs)
    .finally(() => {
      exportLoading.value = false
      showExportModal.value = false
    })
}

// ── 下钻查看明细 ──────────────────────────────────────
const showDrillDown = async (title: string, params?: any) => {
  drillDownTitle.value = title
  drillDownVisible.value = true
  drillDownLoading.value = true
  drillDownData.value = []
  try {
    const res = await salesAnalysisApi.getDrillDownData({ ...buildParams(), ...params })
    drillDownData.value = res.data || []
  } catch (err) {
    console.warn('[销售分析] 加载下钻数据失败', err)
    message.error('明细数据加载失败')
    drillDownData.value = []
  } finally {
    drillDownLoading.value = false
  }
}

// ── 图表点击下钻处理 ────────────────────────────────
const setupChartDrillDown = (chart: echarts.ECharts, drillKey: string, titlePrefix: string) => {
  chart.on('click', (params: any) => {
    if (params.name) {
      showDrillDown(`${titlePrefix}: ${params.name}`, { [drillKey]: params.name })
    }
  })
}

// ── 格式化/颜色辅助函数 ─────────────────────────────
const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return '#cd7f32'
  return 'default'
}

const getMarginColor = (margin: number) => {
  if (margin >= 30) return '#52c41a'
  if (margin >= 20) return '#1890ff'
  return '#faad14'
}

const getTargetColor = (rate: number) => {
  if (rate >= 90) return '#52c41a'
  if (rate >= 70) return '#1890ff'
  return '#faad14'
}

// ── 自动刷新 ──────────────────────────────────────────
watch(autoRefresh, (enabled) => {
  if (enabled) {
    refreshTimer = setInterval(loadData, 60000)
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }
})

watch(activeTab, () => nextTick(() => initAllCharts()))

// ── 图表初始化 ────────────────────────────────────────
const initAllCharts = () => {
  initTrendChart()
  initChannelChart()
  initMonthlyChart()
  initCompareChart()
  initCustomerLevelChart()
  initCustomerSourceChart()
  initCustomerRegionChart()
  initProductCategoryChart()
  initProductVolumeChart()
  initProductMarginChart()
  initSalespersonChart()
  initTargetChart()
  initRegionChart()
  initRegionGrowthChart()
  initDailyChart()
  initWeeklyChart()
  initHourlyChart()
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  disposeChart(trendChartRef.value)
  const chart = echarts.init(trendChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['销售额', '订单数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'], boundaryGap: false },
    yAxis: [
      { type: 'value', name: '销售额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
      { type: 'value', name: '订单数', axisLabel: { formatter: '{value}' } }
    ],
    series: [
      { name: '销售额', type: 'line', smooth: true, yAxisIndex: 0, data: [280000, 320000, 380000, 420000, 480000, 520000], itemStyle: { color: '#1890ff' } },
      { name: '订单数', type: 'line', smooth: true, yAxisIndex: 1, data: [45, 52, 58, 65, 72, 80], itemStyle: { color: '#52c41a' } }
    ]
  })
}

const initChannelChart = () => {
  if (!channelChartRef.value) return
  disposeChart(channelChartRef.value)
  const chart = echarts.init(channelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { name: '线上直销', value: 45, itemStyle: { color: '#1890ff' } },
        { name: '线下门店', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '渠道代理', value: 20, itemStyle: { color: '#faad14' } },
        { name: '企业客户', value: 5, itemStyle: { color: '#722ed1' } }
      ]
    }]
  })
}

const initMonthlyChart = () => {
  if (!monthlyChartRef.value) return
  disposeChart(monthlyChartRef.value)
  const chart = echarts.init(monthlyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本月', '上月'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['第1周', '第2周', '第3周', '第4周'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [
      { name: '本月', type: 'bar', data: [120000, 150000, 180000, 200000], itemStyle: { color: '#1890ff' } },
      { name: '上月', type: 'bar', data: [100000, 120000, 140000, 160000], itemStyle: { color: '#91d5ff' } }
    ]
  })
}

const initCompareChart = () => {
  if (!compareChartRef.value) return
  disposeChart(compareChartRef.value)
  const chart = echarts.init(compareChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比去年', '环比上月'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['销售额', '订单数', '客单价', '毛利率'] },
    yAxis: { type: 'value', name: '增长率(%)' },
    series: [
      { name: '同比去年', type: 'bar', data: [15.2, 12.5, 8.3, 2.1], itemStyle: { color: '#52c41a' } },
      { name: '环比上月', type: 'bar', data: [8.5, 6.2, 4.1, 1.5], itemStyle: { color: '#faad14' } }
    ]
  })
}

const initCustomerLevelChart = () => {
  if (!customerLevelChartRef.value) return
  disposeChart(customerLevelChartRef.value)
  const chart = echarts.init(customerLevelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['60%', '50%'],
      data: [
        { name: 'A类客户', value: 35, itemStyle: { color: '#f5222d' } },
        { name: 'B类客户', value: 28, itemStyle: { color: '#faad14' } },
        { name: 'C类客户', value: 22, itemStyle: { color: '#1890ff' } },
        { name: '潜在客户', value: 15, itemStyle: { color: '#8c8c8c' } }
      ]
    }]
  })
  setupChartDrillDown(chart, 'customerType', '客户等级')
}

const initCustomerSourceChart = () => {
  if (!customerSourceChartRef.value) return
  disposeChart(customerSourceChartRef.value)
  const chart = echarts.init(customerSourceChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['线上推广', '线下活动', '客户转介', '主动咨询'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [35, 25, 20, 18], itemStyle: { color: '#1890ff' } }]
  })
}

const initCustomerRegionChart = () => {
  if (!customerRegionChartRef.value) return
  disposeChart(customerRegionChartRef.value)
  const chart = echarts.init(customerRegionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['60%', '50%'],
      data: [
        { name: '华北', value: 30, itemStyle: { color: '#1890ff' } },
        { name: '华东', value: 25, itemStyle: { color: '#52c41a' } },
        { name: '华南', value: 20, itemStyle: { color: '#faad14' } },
        { name: '西南', value: 15, itemStyle: { color: '#722ed1' } },
        { name: '西北', value: 10, itemStyle: { color: '#13c2c2' } }
      ]
    }]
  })
}

const initProductCategoryChart = () => {
  if (!productCategoryChartRef.value) return
  disposeChart(productCategoryChartRef.value)
  const chart = echarts.init(productCategoryChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['电脑', '办公家具', '打印设备', '配件', '其他'] },
    series: [{ type: 'bar', data: [520000, 280000, 180000, 80000, 50000], itemStyle: { color: '#1890ff' } }]
  })
  setupChartDrillDown(chart, 'productName', '产品类别')
}

const initProductVolumeChart = () => {
  if (!productVolumeChartRef.value) return
  disposeChart(productVolumeChartRef.value)
  const chart = echarts.init(productVolumeChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data: [
        { name: '高销量', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '中销量', value: 45, itemStyle: { color: '#1890ff' } },
        { name: '低销量', value: 25, itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const initProductMarginChart = () => {
  if (!productMarginChartRef.value) return
  disposeChart(productMarginChartRef.value)
  const chart = echarts.init(productMarginChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['<20%', '20-25%', '25-30%', '>30%'] },
    yAxis: { type: 'value', name: '产品数' },
    series: [{ type: 'bar', data: [15, 35, 40, 10], itemStyle: { color: '#1890ff' } }]
  })
}

const initSalespersonChart = () => {
  if (!salespersonChartRef.value) return
  disposeChart(salespersonChartRef.value)
  const chart = echarts.init(salespersonChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '目标'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [
      { name: '销售额', type: 'bar', data: [520000, 380000, 280000, 180000, 150000], itemStyle: { color: '#1890ff' } },
      { name: '目标', type: 'line', data: [550000, 430000, 370000, 290000, 270000], itemStyle: { color: '#f5222d' } }
    ]
  })
  setupChartDrillDown(chart, 'salespersonName', '销售人员')
}

const initTargetChart = () => {
  if (!targetChartRef.value) return
  disposeChart(targetChartRef.value)
  const chart = echarts.init(targetChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value', max: 100, name: '达成率(%)' },
    series: [{
      type: 'bar',
      data: [
        { value: 95, itemStyle: { color: '#52c41a' } },
        { value: 88, itemStyle: { color: '#52c41a' } },
        { value: 75, itemStyle: { color: '#1890ff' } },
        { value: 62, itemStyle: { color: '#faad14' } },
        { value: 55, itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const initRegionChart = () => {
  if (!regionChartRef.value) return
  disposeChart(regionChartRef.value)
  const chart = echarts.init(regionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [{ type: 'bar', data: [520000, 380000, 280000, 180000, 150000], itemStyle: { color: '#1890ff' } }]
  })
}

const initRegionGrowthChart = () => {
  if (!regionGrowthChartRef.value) return
  disposeChart(regionGrowthChartRef.value)
  const chart = echarts.init(regionGrowthChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比增长', '环比增长'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value', name: '增长率(%)' },
    series: [
      { name: '同比增长', type: 'line', data: [12.5, 8.3, 15.2, -2.1, 5.8], itemStyle: { color: '#1890ff' } },
      { name: '环比增长', type: 'line', data: [5.2, 3.8, 6.5, -1.2, 2.5], itemStyle: { color: '#52c41a' } }
    ]
  })
}

const initDailyChart = () => {
  if (!dailyChartRef.value) return
  disposeChart(dailyChartRef.value)
  const chart = echarts.init(dailyChartRef.value)
  charts.push(chart)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const values = Array.from({ length: 30 }, () => Math.floor(Math.random() * 50000) + 10000)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: days, axisLabel: { interval: 4 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: values, itemStyle: { color: '#1890ff' } }]
  })
}

const initWeeklyChart = () => {
  if (!weeklyChartRef.value) return
  disposeChart(weeklyChartRef.value)
  const chart = echarts.init(weeklyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本周', '上周'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
      { name: '本周', type: 'line', data: [45000, 52000, 48000, 55000, 42000, 28000, 35000], itemStyle: { color: '#1890ff' } },
      { name: '上周', type: 'line', data: [38000, 45000, 42000, 48000, 35000, 22000, 28000], itemStyle: { color: '#91d5ff' } }
    ]
  })
}

const initHourlyChart = () => {
  if (!hourlyChartRef.value) return
  disposeChart(hourlyChartRef.value)
  const chart = echarts.init(hourlyChartRef.value)
  charts.push(chart)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const values = Array.from({ length: 24 }, (_, i) => {
    if (i >= 9 && i <= 18) return Math.floor(Math.random() * 8000) + 2000
    return Math.floor(Math.random() * 2000) + 500
  })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: hours, axisLabel: { interval: 2 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#1890ff' } }]
  })
}

const disposeChart = (el: HTMLElement | undefined) => {
  if (!el) return
  const existing = charts.find(c => c.getDom() === el)
  if (existing) {
    existing.dispose()
    charts = charts.filter(c => c !== existing)
  }
}

const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown)
  await loadOptions()
  loadData()
  window.addEventListener("erp:refresh", () => loadData())
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  charts.forEach(chart => chart.dispose())
  charts = []
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener("erp:refresh", loadData)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.error-banner {
  padding: 12px 0;
}

.filter-collapse {
  background: #fff;
  border-radius: 4px;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.summary-cards {
  padding: 16px 0;
}

@keyframes slideUpFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  animation: slideUpFadeIn 0.4s ease-out both;
}

.summary-card:nth-child(1) { animation-delay: 0s; }
.summary-card:nth-child(2) { animation-delay: 0.08s; }
.summary-card:nth-child(3) { animation-delay: 0.16s; }
.summary-card:nth-child(4) { animation-delay: 0.24s; }

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff9e6 0%, #fff5d6 100%);
  border: 1px solid #ffe58f;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
  flex-shrink: 0;
}

.summary-content {
  flex: 1;
  min-width: 0;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  transition: color 0.3s;
}

.summary-change {
  font-size: 12px;
  margin-top: 4px;
}

.summary-change.positive {
  color: #52c41a;
}

.summary-change.negative {
  color: #f5222d;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}

.chart-container {
  height: 280px;
}

.ranking-table-container {
  max-height: 400px;
  overflow-y: auto;
}

.empty-table-placeholder {
  padding: 20px 0;
}

/* 金额单元格 */
.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

/* 增长率单元格 */
.growth-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.growth-cell.positive {
  color: #52c41a;
}

.growth-cell.negative {
  color: #f5222d;
}

/* 数据状态 */
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 分区块错误横幅 ──────────────────────────────── */
.section-error-banner {
  padding: 8px 0;
}

/* ── 趋势单元格 ────────────────────────────────────── */
.trend-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.trend-cell.up {
  color: #52c41a;
}

.trend-cell.down {
  color: #f5222d;
}

.trend-cell.flat {
  color: #999;
}

/* ── 下钻抽屉表格 ──────────────────────────────────── */
:deep(.ant-drawer-body .vxe-table) {
  margin-top: 8px;
}

/* ── 对比期选择器 ──────────────────────────────────── */
:deep(.ant-select-sm.ant-select) {
  min-width: 100px;
}
</style>
