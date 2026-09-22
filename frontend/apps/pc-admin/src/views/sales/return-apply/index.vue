<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ Tab栏 + 工具栏 ═══ -->
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="activeTab === 'detail'"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-title="'商品分类'"
        :category-editable="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
        @category-select="handleCategorySelect"
        @category-expand="handleCategoryExpand"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">
                --查询方案--
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space
            :size="4"
            class="quick-dates"
          >
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ══ 工具栏右侧：操作按钮（对标截图：全部平铺，无"更多"下拉） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="activeTab === 'doc' && isButtonEnabled('batchPrint')" size="small" @click="handleBatchPrint">
              <PrinterOutlined /> 批量打印
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrintF8">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
            <a-button v-if="isButtonEnabled('config')" size="small" @click="showPageConfig = true">
              <SettingOutlined /> 配置
            </a-button>
            <!-- 批量操作（仅在有选中行时显示） -->
            <a-button v-if="activeTab === 'doc' && selectedRowKeys.length > 0" size="small" @click="handleBatchApprove">
              <CheckOutlined /> 批量审核
            </a-button>
            <a-button v-if="selectedRowKeys.length > 0" size="small" danger @click="handleBatchDelete">
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- 按单据 Tab 搜索行 -->
            <template v-if="activeTab === 'doc'">
              <div class="search-container" :data-expanded="showMoreDocConditions || null">
              <div ref="docGridRef" class="search-grid">
                <div v-if="isDocFieldVisible('orderDate')" class="search-field-item">
                  <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
                </div>
                <div v-if="isDocFieldVisible('orderNo')" class="search-field-item">
                  <a-input v-model:value="searchParams.orderNo" placeholder="单据编号" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDocFieldVisible('customerName')" class="search-field-item">
                  <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDocFieldVisible('handlerName')" class="search-field-item">
                  <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDocFieldVisible('deptName')" class="search-field-item">
                  <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDocFieldVisible('warehouseName')" class="search-field-item">
                  <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDocFieldVisible('generateType')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">产生方式</span>
                    <a-select v-model:value="searchParams.generateType" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="手动创建">手动创建</a-select-option>
                      <a-select-option value="销售订单">销售订单</a-select-option>
                      <a-select-option value="销售出库">销售出库</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div v-if="isDocFieldVisible('status')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">审核中</a-select-option>
                      <a-select-option :value="2">审核通过</a-select-option>
                      <a-select-option :value="3">已完成</a-select-option>
                      <a-select-option :value="4">已取消</a-select-option>
                    </a-select>
                  </div>
                </div>
                <!-- 扩展查询条件 -->
                <template v-if="showMoreDocConditions && hasMoreDocConditions">
                  <div v-if="isDocFieldVisible('settleStatus')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">结算状态</span>
                      <a-select v-model:value="searchParams.settleStatus" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="未结算">未结算</a-select-option>
                        <a-select-option value="部分结算">部分结算</a-select-option>
                        <a-select-option value="已结算">已结算</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDocFieldVisible('salesType')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">销售类型</span>
                      <a-select v-model:value="searchParams.salesType" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="正常销售">正常销售</a-select-option>
                        <a-select-option value="换货">换货</a-select-option>
                        <a-select-option value="调拨">调拨</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDocFieldVisible('printCount')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">打印次数</span>
                      <a-select v-model:value="searchParams.printCount" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="0">0</a-select-option>
                        <a-select-option :value="1">≥1</a-select-option>
                        <a-select-option :value="3">≥3</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDocFieldVisible('creatorName')" class="search-field-item">
                    <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('auditorName')" class="search-field-item">
                    <a-input v-model:value="searchParams.auditorName" placeholder="审核人" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('submitBy')" class="search-field-item">
                    <a-input v-model:value="searchParams.submitBy" placeholder="提交人" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('productLineAttr')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">商品行属性</span>
                      <a-select v-model:value="searchParams.productLineAttr" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="正常">正常</a-select-option>
                        <a-select-option value="赠品">赠品</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDocFieldVisible('remark')" class="search-field-item">
                    <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('summary')" class="search-field-item">
                    <a-input v-model:value="searchParams.summary" placeholder="摘要" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('deliveryMethod')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">配送方式</span>
                      <a-select v-model:value="searchParams.deliveryMethod" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="自提">自提</a-select-option>
                        <a-select-option value="配送">配送</a-select-option>
                        <a-select-option value="快递">快递</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDocFieldVisible('extNum1')" class="search-field-item">
                    <a-input-number v-model:value="searchParams.extNum1" placeholder="自定义字段1" size="small" style="width: 100%" />
                  </div>
                  <div v-if="isDocFieldVisible('extNum2')" class="search-field-item">
                    <a-input-number v-model:value="searchParams.extNum2" placeholder="自定义字段2" size="small" style="width: 100%" />
                  </div>
                  <div v-if="isDocFieldVisible('extText1')" class="search-field-item">
                    <a-input v-model:value="searchParams.extText1" placeholder="自定义字段3" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('extText2')" class="search-field-item">
                    <a-input v-model:value="searchParams.extText2" placeholder="自定义字段4" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('extText3')" class="search-field-item">
                    <a-input v-model:value="searchParams.extText3" placeholder="自定义字段5" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('contactName')" class="search-field-item">
                    <a-input v-model:value="searchParams.contactName" placeholder="联系人" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('contactPhone')" class="search-field-item">
                    <a-input v-model:value="searchParams.contactPhone" placeholder="联系电话" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('contactAddress')" class="search-field-item">
                    <a-input v-model:value="searchParams.contactAddress" placeholder="联系地址" allow-clear size="small" />
                  </div>
                  <div v-if="isDocFieldVisible('auditTime')" class="search-field-item">
                    <a-date-picker v-model:value="searchParams.auditTime" size="small" style="width: 100%" />
                  </div>
                </template>
              </div>
                <div class="search-action-bar">
                <div ref="docActionRef" class="search-action-group" :style="{ gridColumn: 'span ' + docActionSpan }">
                <div class="search-field-item search-action-item">
                  <a-space :size="4">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    <a-button size="small" @click="fetchData">刷新</a-button>
                  </a-space>
                </div>
                </div>
                <div v-if="hasMoreDocConditions" class="search-more-toggle">
                  <a-button type="link" size="small" @click="showMoreDocConditions = !showMoreDocConditions">
                    {{ showMoreDocConditions ? '收起' : '更多条件' }}
                    <component :is="showMoreDocConditions ? 'UpOutlined' : 'DownOutlined'" style="font-size: 10px; margin-left: 2px;" />
                  </a-button>
                </div>
                </div>
              </div>
            </template>

            <!-- 按明细 Tab 搜索行 -->
            <template v-else>
              <div class="search-container" :data-expanded="showMoreDetailConditions || null">
              <div ref="detailGridRef" class="search-grid">
                <div v-if="isDetailFieldVisible('orderDate')" class="search-field-item">
                  <a-range-picker v-model:value="dateRange" size="small" style="width: 100%" @change="handleDateChange" />
                </div>
                <div v-if="isDetailFieldVisible('orderNo')" class="search-field-item">
                  <a-input v-model:value="searchParams.orderNo" placeholder="单据编号" allow-clear size="small" />
                </div>
                <div v-if="isDetailFieldVisible('productName')" class="search-field-item">
                  <a-input v-model:value="searchParams.productName" placeholder="商品" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDetailFieldVisible('customerName')" class="search-field-item">
                  <a-input v-model:value="searchParams.customerName" placeholder="客户" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDetailFieldVisible('handlerName')" class="search-field-item">
                  <a-input v-model:value="searchParams.handlerName" placeholder="经手人" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDetailFieldVisible('deptName')" class="search-field-item">
                  <a-input v-model:value="searchParams.deptName" placeholder="部门" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDetailFieldVisible('warehouseName')" class="search-field-item">
                  <a-input v-model:value="searchParams.warehouseName" placeholder="仓库" allow-clear size="small" :suffix="h(SearchOutlined, { style: 'color:#bbb' })" />
                </div>
                <div v-if="isDetailFieldVisible('status')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="searchParams.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option :value="0">草稿</a-select-option>
                      <a-select-option :value="1">审核中</a-select-option>
                      <a-select-option :value="2">审核通过</a-select-option>
                    </a-select>
                  </div>
                </div>
                <!-- 扩展查询条件 -->
                <template v-if="showMoreDetailConditions && hasMoreDetailConditions">
                  <div v-if="isDetailFieldVisible('salesType')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">销售类型</span>
                      <a-select v-model:value="searchParams.salesType" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="正常销售">正常销售</a-select-option>
                        <a-select-option value="换货">换货</a-select-option>
                        <a-select-option value="调拨">调拨</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDetailFieldVisible('productLineAttr')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">商品行属性</span>
                      <a-select v-model:value="searchParams.productLineAttr" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option value="正常">正常</a-select-option>
                        <a-select-option value="赠品">赠品</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDetailFieldVisible('itemRemark')" class="search-field-item">
                    <a-input v-model:value="searchParams.itemRemark" placeholder="明细备注" allow-clear size="small" />
                  </div>
                  <div v-if="isDetailFieldVisible('remark')" class="search-field-item">
                    <a-input v-model:value="searchParams.remark" placeholder="单据备注" allow-clear size="small" />
                  </div>
                  <div v-if="isDetailFieldVisible('isGift')" class="search-field-item">
                    <div class="search-select-wrap">
                      <span class="search-select-label">是否赠品</span>
                      <a-select v-model:value="searchParams.isGift" size="small" allow-clear>
                        <a-select-option value="">全部</a-select-option>
                        <a-select-option :value="true">是</a-select-option>
                        <a-select-option :value="false">否</a-select-option>
                      </a-select>
                    </div>
                  </div>
                  <div v-if="isDetailFieldVisible('creatorName')" class="search-field-item">
                    <a-input v-model:value="searchParams.creatorName" placeholder="制单人" allow-clear size="small" />
                  </div>
                  <div v-if="isDetailFieldVisible('auditorName')" class="search-field-item">
                    <a-input v-model:value="searchParams.auditorName" placeholder="审核人" allow-clear size="small" />
                  </div>
                  <div v-if="isDetailFieldVisible('auditTime')" class="search-field-item">
                    <a-date-picker v-model:value="searchParams.auditTime" size="small" style="width: 100%" />
                  </div>
                </template>
              </div>
                <div class="search-action-bar">
                <div ref="detailActionRef" class="search-action-group" :style="{ gridColumn: 'span ' + detailActionSpan }">
                <div class="search-field-item search-action-item">
                  <a-space :size="4">
                    <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                    <a-button size="small" @click="fetchData">刷新</a-button>
                  </a-space>
                </div>
                </div>
                <div v-if="hasMoreDetailConditions" class="search-more-toggle">
                  <a-button type="link" size="small" @click="showMoreDetailConditions = !showMoreDetailConditions">
                    {{ showMoreDetailConditions ? '收起' : '更多条件' }}
                    <component :is="showMoreDetailConditions ? 'UpOutlined' : 'DownOutlined'" style="font-size: 10px; margin-left: 2px;" />
                  </a-button>
                </div>
                </div>
              </div>
            </template>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="currentColumns"
              :storage-key="activeTab === 'doc' ? 'sale-return-apply-table-columns-doc' : 'sale-return-apply-table-columns-detail'"
              :data-source="tableData"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :row-selection="rowSelection"
              :row-key="activeTab === 'detail' ? 'itemId' : 'id'"
              @page-change="handlePageChange"
            >
              <!-- 单据编号 -->
              <template #orderNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.returnNo }}
                </a-button>
              </template>
              <!-- 单据状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    详情
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryConfig"
      :function-buttons-config="activeFunctionButtons"
      :storage-key="activePageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  SearchOutlined,
  DownOutlined,
  UpOutlined,
  CheckOutlined,
  DeleteOutlined,
  ExportOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { saleReturnApi } from '@/api/erp'
import { productCategoryApi } from '@/api/erp/product'
import { useRouter } from 'vue-router'

const router = useRouter()

// ═══ Tab 配置 ═══
const tabs = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' },
]
const activeTab = ref('doc')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('week')

// ═══ 查询方案 ═══
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs(), dayjs()])

// ═══ 搜索参数 ═══
const searchParams = reactive({
  orderNo: '',
  customerName: '',
  handlerName: '',
  deptName: '',
  warehouseName: '',
  productName: '',
  itemRemark: '',
  status: undefined as number | '' | undefined,
  generateType: '',
  settleStatus: '',
  salesType: '',
  productLineAttr: '',
  printCount: undefined as number | undefined,
  startDate: dayjs().subtract(7, 'day').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
  // 按单据tab 新增查询条件
  creatorName: '',
  auditorName: '',
  submitBy: '',
  remark: '',
  summary: '',
  deliveryMethod: '',
  extNum1: undefined as number | undefined,
  extNum2: undefined as number | undefined,
  extText1: '',
  extText2: '',
  extText3: '',
  contactName: '',
  contactPhone: '',
  contactAddress: '',
  auditTime: undefined as string | undefined,
  // 按明细tab 新增查询条件
  isGift: undefined as any,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({ current: pagination.current, pageSize: pagination.pageSize, total: pagination.total }))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => {
    selectedRowKeys.value = keys
  }
}))

// ═══ 配置弹窗状态 ═══
const showPageConfig = ref(false)
const showMoreDocConditions = ref(false)
const showMoreDetailConditions = ref(false)

// ═══ 动态 grid-column span ═══
const docGridRef = ref<HTMLElement | null>(null)
const docActionRef = ref<HTMLElement | null>(null)
const detailGridRef = ref<HTMLElement | null>(null)
const detailActionRef = ref<HTMLElement | null>(null)
const { span: docActionSpan } = useAutoGridSpan(docActionRef, docGridRef)
const { span: detailActionSpan } = useAutoGridSpan(detailActionRef, detailGridRef)

// ═══ 页面配置存储键（按 Tab 独立：查询条件互不覆盖；功能按钮全局共用） ═══
const PAGE_CONFIG_STORAGE_KEY = 'sale-return-apply-page-config'
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_STORAGE_KEY}-${activeTab.value}`)
const activeQueryConfig = computed(() =>
  activeTab.value === 'detail' ? detailQueryConfig.value : docQueryConfig.value
)
/** 按明细 Tab 无「批量打印」（对标文档：按明细功能按钮 5 个） */
const activeFunctionButtons = computed(() =>
  activeTab.value === 'detail'
    ? functionButtonConfig.value.filter(b => b.key !== 'batchPrint')
    : functionButtonConfig.value
)

// ═══ 页面配置默认值（按 Tab 独立，供存档合并/恢复默认使用） ═══
const DEFAULT_DOC_QUERY_FIELDS = [
  { key: 'orderDate', label: '单据时间', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'generateType', label: '产生方式', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'settleStatus', label: '结算状态', visible: true },
  { key: 'salesType', label: '销售类型', visible: true },
  { key: 'printCount', label: '打印次数', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'submitBy', label: '提交人', visible: true },
  { key: 'productLineAttr', label: '商品行属性', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'summary', label: '摘要', visible: true },
  { key: 'deliveryMethod', label: '配送方式', visible: true },
  { key: 'extNum1', label: '自定义字段1(数字)', visible: true },
  { key: 'extNum2', label: '自定义字段2(数字)', visible: true },
  { key: 'extText1', label: '自定义字段3(文本)', visible: true },
  { key: 'extText2', label: '自定义字段4(文本)', visible: true },
  { key: 'extText3', label: '自定义字段5(文本)', visible: true },
  { key: 'contactName', label: '联系人', visible: true },
  { key: 'contactPhone', label: '联系电话', visible: true },
  { key: 'contactAddress', label: '联系地址', visible: true },
  { key: 'auditTime', label: '审核时间', visible: true },
]
const docQueryConfig = ref(DEFAULT_DOC_QUERY_FIELDS.map(f => ({ ...f })))

// 按明细tab查询条件配置（16个，默认全部勾选）
const DEFAULT_DETAIL_QUERY_FIELDS = [
  { key: 'orderDate', label: '单据时间', visible: true },
  { key: 'orderNo', label: '单据编号', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'warehouseName', label: '仓库', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'salesType', label: '销售类型', visible: true },
  { key: 'productLineAttr', label: '商品行属性', visible: true },
  { key: 'remark', label: '单据备注', visible: true },
  { key: 'itemRemark', label: '明细备注', visible: true },
  { key: 'isGift', label: '是否赠品', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'auditorName', label: '审核人', visible: true },
  { key: 'auditTime', label: '审核时间', visible: true },
]
const detailQueryConfig = ref(DEFAULT_DETAIL_QUERY_FIELDS.map(f => ({ ...f })))

const functionButtonConfig = ref([
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
])

// ═══ 商品分类树 ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const categoryExpandedKeys = ref<(string | number)[]>([])

// ═══ 状态映射 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '审核中', color: 'processing' },
  2: { text: '审核通过', color: 'blue' },
  3: { text: '已完成', color: 'green' },
  4: { text: '已取消', color: 'orange' },
}

function getStatusText(status: number): string {
  return STATUS_MAP[status]?.text || '未知'
}

function getStatusColor(status: number): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 列定义 ═══

/** 按单据 Tab 列 (47列) */
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'returnNo', key: 'returnNo', width: 170, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  // ── 客户 ──
  { title: '客户', field: 'customerName', key: 'customerName', width: 200, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 120, sortable: true },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 100, sortable: true },
  { title: '联系人', field: 'contactName', key: 'contactName', width: 100, sortable: true },
  { title: '联系电话', field: 'contactPhone', key: 'contactPhone', width: 120, sortable: true },
  { title: '联系地址', field: 'contactAddress', key: 'contactAddress', width: 150, sortable: true },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100, defaultHidden: true },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  // ── 经手人/部门 ──
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100 },
  // ── 金额 ──
  { title: '金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right', sortable: true, defaultHidden: true },
  { title: '折后金额', field: 'discountBillAmount', key: 'discountBillAmount', width: 120, align: 'right', sortable: true, defaultHidden: true },
  { title: '运费承担方', field: 'freightPayer', key: 'freightPayer', width: 100, defaultHidden: true },
  { title: '运费', field: 'shippingFee', key: 'shippingFee', width: 80, align: 'right', defaultHidden: true },
  { title: '配送方式', field: 'deliveryMethod', key: 'deliveryMethod', width: 100, defaultHidden: true },
  { title: '本单金额', field: 'billAmount', key: 'billAmount', width: 120, align: 'right', sortable: true },
  // ── 结算 ──
  { title: '结算状态', field: 'settleStatus', key: 'settleStatus', width: 100, sortable: true, defaultHidden: true },
  // ── 数量 ──
  { title: '订货数量', field: 'orderedQuantity', key: 'orderedQuantity', width: 100, align: 'right' },
  { title: '已收数量', field: 'receivedQuantity', key: 'receivedQuantity', width: 100, align: 'right' },
  { title: '未收数量', field: 'unreceivedQuantity', key: 'unreceivedQuantity', width: 100, align: 'right' },
  // ── 物理属性 ──
  { title: '重量(kg)', field: 'totalWeight', key: 'totalWeight', width: 90, align: 'right', defaultHidden: true },
  { title: '体积(m³)', field: 'totalVolume', key: 'totalVolume', width: 90, align: 'right', defaultHidden: true },
  // ── 备注/摘要 ──
  { title: '摘要', field: 'summary', key: 'summary', width: 150, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80 },
  // ── 自定义字段 ──
  { title: '表头自定义字段1(数字)', field: 'extNum1', key: 'extNum1', width: 120, defaultHidden: true },
  { title: '表头自定义字段2(数字)', field: 'extNum2', key: 'extNum2', width: 120, defaultHidden: true },
  { title: '表头自定义字段3(文本)', field: 'extText1', key: 'extText1', width: 120, defaultHidden: true },
  { title: '表头自定义字段4(文本)', field: 'extText2', key: 'extText2', width: 120, defaultHidden: true },
  { title: '表头自定义字段5(文本)', field: 'extText3', key: 'extText3', width: 120, defaultHidden: true },
  { title: '表尾自定义字段1', field: 'footerExtText1', key: 'footerExtText1', width: 120, defaultHidden: true },
  { title: '表尾自定义字段2', field: 'footerExtText2', key: 'footerExtText2', width: 120, defaultHidden: true },
  // ── 物流 ──
  { title: '物流公司', field: 'logisticsCompany', key: 'logisticsCompany', width: 120, defaultHidden: true },
  { title: '运单号', field: 'waybillNo', key: 'waybillNo', width: 140, defaultHidden: true },
  // ── 时间/人员 ──
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 140, defaultHidden: true },
  { title: '产生方式', field: 'generateType', key: 'generateType', width: 100, defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, sortable: true, defaultHidden: true },
  { title: '提交人', field: 'submitByName', key: 'submitByName', width: 80, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80, sortable: true, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 80, align: 'right' },
  { title: '商品行数', field: 'lineCount', key: 'lineCount', width: 80, align: 'right', defaultHidden: true },
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120 },
  { title: '审核时间', field: 'auditTime', key: 'auditTime', width: 140, defaultHidden: true },
]

/** 按明细 Tab 列 (62列) */
const detailColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 120, fixed: 'right', slotName: 'actionCell' },
  { title: '单据日期', field: 'orderDate', key: 'orderDate', width: 110, sortable: true },
  { title: '单据编号', field: 'returnNo', key: 'returnNo', width: 170, type: 'slot', slotName: 'orderNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '仓库', field: 'warehouseName', key: 'warehouseName', width: 120, sortable: true },
  // ── 客户 ──
  { title: '客户', field: 'customerName', key: 'customerName', width: 160, sortable: true },
  { title: '客户编号', field: 'customerCode', key: 'customerCode', width: 120 },
  { title: '客户级别', field: 'customerLevel', key: 'customerLevel', width: 80 },
  { title: '联系人', field: 'contactName', key: 'contactName', width: 100 },
  { title: '联系电话', field: 'contactPhone', key: 'contactPhone', width: 120 },
  { title: '联系地址', field: 'contactAddress', key: 'contactAddress', width: 150 },
  { title: '客户一票通', field: 'customerTicket', key: 'customerTicket', width: 100, defaultHidden: true },
  { title: '客户备注', field: 'customerRemark', key: 'customerRemark', width: 120 },
  // ── 经手人/部门 ──
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 80, sortable: true },
  { title: '部门', field: 'deptName', key: 'deptName', width: 100, defaultHidden: true },
  // ── 商品快照 ──
  { title: '商品名称', field: 'productName', key: 'productName', width: 200, sortable: true },
  { title: '货号', field: 'productCode', key: 'productCode', width: 100 },
  { title: '条码', field: 'barcode', key: 'barcode', width: 110 },
  { title: '规格', field: 'specification', key: 'specification', width: 100, defaultHidden: true },
  { title: '型号', field: 'modelNo', key: 'modelNo', width: 80, defaultHidden: true },
  { title: '产地', field: 'originPlace', key: 'originPlace', width: 80, defaultHidden: true },
  // ── 自定义字段 ──
  { title: '单据自定义1(数字字段)', field: 'extNum1', key: 'extNum1', width: 100, defaultHidden: true },
  { title: '单据自定义2(数字字段)', field: 'extNum2', key: 'extNum2', width: 100, defaultHidden: true },
  { title: '单据自定义3(数字字段)', field: 'extNum3', key: 'extNum3', width: 100, defaultHidden: true },
  { title: '单据自定义4(文本字段)', field: 'extText1', key: 'extText1', width: 100, defaultHidden: true },
  { title: '单据自定义5(文本字段)', field: 'extText2', key: 'extText2', width: 100, defaultHidden: true },
  { title: '单据自定义6(数字字段)', field: 'extNum4', key: 'extNum4', width: 100, defaultHidden: true },
  { title: '单据自定义7(数字字段)', field: 'extNum5', key: 'extNum5', width: 100, defaultHidden: true },
  { title: '单据自定义8(往来单位)', field: 'extPartner', key: 'extPartner', width: 100, defaultHidden: true },
  { title: '单据自定义9(职员)', field: 'extStaff', key: 'extStaff', width: 100, defaultHidden: true },
  { title: '单据自定义10(部门)', field: 'extDept', key: 'extDept', width: 100, defaultHidden: true },
  // ── 单位/包装 ──
  { title: '单位', field: 'unit', key: 'unit', width: 80 },
  { title: '小单位', field: 'smallUnit', key: 'smallUnit', width: 70, defaultHidden: true },
  { title: '小单位数量', field: 'smallUnitQuantity', key: 'smallUnitQuantity', width: 90, align: 'right', defaultHidden: true },
  { title: '换算关系', field: 'conversionRelation', key: 'conversionRelation', width: 100, defaultHidden: true },
  { title: '换算结果', field: 'conversionResult', key: 'conversionResult', width: 90, align: 'right', defaultHidden: true },
  { title: '大包装', field: 'bigPack', key: 'bigPack', width: 70, align: 'right', defaultHidden: true },
  { title: '中包装', field: 'midPack', key: 'midPack', width: 70, align: 'right', defaultHidden: true },
  { title: '小包装', field: 'smallPack', key: 'smallPack', width: 70, align: 'right', defaultHidden: true },
  // ── 数量 ──
  { title: '退货数量', field: 'returnQuantity', key: 'returnQuantity', width: 90, align: 'right' },
  { title: '已收数量', field: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right' },
  { title: '未收数量', field: 'unreceivedQuantity', key: 'unreceivedQuantity', width: 90, align: 'right' },
  { title: '终止数量', field: 'terminatedQuantity', key: 'terminatedQuantity', width: 90, align: 'right', defaultHidden: true },
  { title: '终止金额', field: 'terminatedAmount', key: 'terminatedAmount', width: 100, align: 'right', defaultHidden: true },
  // ── 价格 ──
  { title: '单价', field: 'unitPrice', key: 'unitPrice', width: 80, align: 'right' },
  { title: '小单位单价', field: 'smallUnitPrice', key: 'smallUnitPrice', width: 90, align: 'right', defaultHidden: true },
  { title: '金额', field: 'lineAmount', key: 'lineAmount', width: 100, align: 'right', sortable: true },
  { title: '折扣(%)', field: 'discountRate', key: 'discountRate', width: 70, align: 'right', defaultHidden: true },
  { title: '折后单价', field: 'discountedPrice', key: 'discountedPrice', width: 80, align: 'right', defaultHidden: true },
  { title: '折后金额', field: 'discountedAmount', key: 'discountedAmount', width: 100, align: 'right', defaultHidden: true },
  // ── 物理属性 ──
  { title: '重量(kg)', field: 'weight', key: 'weight', width: 80, align: 'right', defaultHidden: true },
  { title: '体积(m³)', field: 'volume', key: 'volume', width: 80, align: 'right', defaultHidden: true },
  // ── 销售类型/属性 ──
  { title: '销售类型', field: 'salesType', key: 'salesType', width: 100, defaultHidden: true },
  { title: '商品行属性', field: 'productLineAttr', key: 'productLineAttr', width: 100, defaultHidden: true },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 120 },
  { title: '单据备注', field: 'remark', key: 'remark', width: 120, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 150, defaultHidden: true },
  { title: '附件', field: 'attachment', key: 'attachment', width: 80, defaultHidden: true },
  // ── 人员 ──
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 80, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 80, defaultHidden: true },
  { title: '提交时间', field: 'submitTime', key: 'submitTime', width: 140, defaultHidden: true },
  { title: '产生方式', field: 'generateType', key: 'generateType', width: 100, defaultHidden: true },
  { title: '审核时间', field: 'auditTime', key: 'auditTime', width: 140, defaultHidden: true },
]

// ═══ 列配置走数据表表头齿轮（storage-key=sale-return-apply-table-columns-doc/-detail） ═══
// 当前显示的列
const currentColumns = computed(() => {
  if (activeTab.value === 'doc') return docColumns
  return detailColumns
})

// ═══ 页面配置变更处理 ═══
function handlePageConfigChange(config: any) {
  const tab = activeTab.value === 'detail' ? 'detail' : 'doc'
  const targetConfig = tab === 'detail' ? detailQueryConfig.value : docQueryConfig.value
  if (config.queryFields) {
    config.queryFields.forEach((f: any) => {
      const target = targetConfig.find(d => d.key === f.key)
      if (target) target.visible = f.visible
    })
    // 拖拽排序真实生效：按配置面板顺序重排查询字段
    const ordered = config.queryFields
      .map((f: any) => targetConfig.find(d => d.key === f.key))
      .filter((d: any) => !!d)
    if (ordered.length === targetConfig.length) {
      targetConfig.splice(0, targetConfig.length, ...ordered)
    }
    localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-${tab}`, JSON.stringify({ queryFields: config.queryFields }))
  }
  if (config.functionButtons) {
    config.functionButtons.forEach((b: any) => {
      const target = functionButtonConfig.value.find(d => d.key === b.key)
      if (target) target.enabled = b.enabled
    })
    localStorage.setItem(`${PAGE_CONFIG_STORAGE_KEY}-buttons`, JSON.stringify({ functionButtons: config.functionButtons }))
  }
}

// 查询条件显隐控制
function isDocFieldVisible(key: string): boolean {
  const field = docQueryConfig.value.find(f => f.key === key)
  return field ? field.visible : true
}
function isDetailFieldVisible(key: string): boolean {
  const field = detailQueryConfig.value.find(f => f.key === key)
  return field ? field.visible : true
}
function isButtonEnabled(key: string): boolean {
  const btn = functionButtonConfig.value.find(b => b.key === key)
  return btn ? btn.enabled : true
}
// 可见查询字段数量（用于控制折叠）
const visibleDocQueryCount = computed(() => docQueryConfig.value.filter(f => f.visible).length)
const visibleDetailQueryCount = computed(() => detailQueryConfig.value.filter(f => f.visible).length)

// 动态折叠：前8个visible字段默认显示，其余折叠
const DEFAULT_VISIBLE_COUNT = 8
const docDefaultVisibleKeys = computed(() =>
  docQueryConfig.value.filter(f => f.visible).slice(0, DEFAULT_VISIBLE_COUNT).map(f => f.key)
)
const docExtraVisibleKeys = computed(() =>
  docQueryConfig.value.filter(f => f.visible).slice(DEFAULT_VISIBLE_COUNT).map(f => f.key)
)
const hasMoreDocConditions = computed(() => docExtraVisibleKeys.value.length > 0)

const detailDefaultVisibleKeys = computed(() =>
  detailQueryConfig.value.filter(f => f.visible).slice(0, DEFAULT_VISIBLE_COUNT).map(f => f.key)
)
const detailExtraVisibleKeys = computed(() =>
  detailQueryConfig.value.filter(f => f.visible).slice(DEFAULT_VISIBLE_COUNT).map(f => f.key)
)
const hasMoreDetailConditions = computed(() => detailExtraVisibleKeys.value.length > 0)

function isDocDefaultVisible(key: string): boolean {
  return docDefaultVisibleKeys.value.includes(key)
}
function isDetailDefaultVisible(key: string): boolean {
  return detailDefaultVisibleKeys.value.includes(key)
}

// ═══ 数据加载 ═══

async function fetchData() {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.orderNo) params.keyword = searchParams.orderNo
    if (searchParams.customerName) params.customerName = searchParams.customerName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.warehouseName) params.warehouseName = searchParams.warehouseName
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    if (searchParams.generateType) params.generateType = searchParams.generateType
    if (searchParams.settleStatus) params.settleStatus = searchParams.settleStatus
    if (searchParams.salesType) params.salesType = searchParams.salesType
    if (searchParams.printCount !== undefined) params.printCount = searchParams.printCount
    // 按单据tab 新增参数
    if (searchParams.deptName) params.deptName = searchParams.deptName
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.auditorName) params.auditorName = searchParams.auditorName
    if (searchParams.submitBy) params.submitBy = searchParams.submitBy
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.summary) params.summary = searchParams.summary
    if (searchParams.deliveryMethod) params.deliveryMethod = searchParams.deliveryMethod
    if (searchParams.productLineAttr) params.productLineAttr = searchParams.productLineAttr
    if (searchParams.extNum1 !== undefined) params.extNum1 = searchParams.extNum1
    if (searchParams.extNum2 !== undefined) params.extNum2 = searchParams.extNum2
    if (searchParams.extText1) params.extText1 = searchParams.extText1
    if (searchParams.extText2) params.extText2 = searchParams.extText2
    if (searchParams.extText3) params.extText3 = searchParams.extText3
    if (searchParams.contactName) params.contactName = searchParams.contactName
    if (searchParams.contactPhone) params.contactPhone = searchParams.contactPhone
    if (searchParams.contactAddress) params.contactAddress = searchParams.contactAddress
    if (searchParams.auditTime) params.auditTime = searchParams.auditTime

    let res
    if (activeTab.value === 'detail') {
      // 按明细tab
      if (searchParams.productName) params.productName = searchParams.productName
      if (searchParams.itemRemark) params.itemRemark = searchParams.itemRemark
      if (searchParams.isGift !== undefined) params.isGift = searchParams.isGift
      if (searchParams.deptName) params.deptName = searchParams.deptName
      if (searchParams.creatorName) params.creatorName = searchParams.creatorName
      if (searchParams.auditorName) params.auditorName = searchParams.auditorName
      if (searchParams.remark) params.remark = searchParams.remark
      if (searchParams.auditTime) params.auditTime = searchParams.auditTime
      if (selectedCategoryId.value && selectedCategoryId.value !== '0') {
        params.categoryId = selectedCategoryId.value
      }
      res = await saleReturnApi.pageDetail(params)
    } else {
      res = await saleReturnApi.page(params)
    }

    if (res) {
      tableData.value = res.records || res.data?.records || []
      pagination.total = res.total || res.data?.total || 0
    }
  } catch (error: any) {
    hasError.value = true
    console.warn('[销售退货申请] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const tree = await productCategoryApi.getTree()
    categoryTreeData.value = tree || []
    if (categoryTreeData.value.length > 0) {
      categoryExpandedKeys.value = [categoryTreeData.value[0].id]
    }
  } catch {
    categoryTreeData.value = []
  } finally {
    categoryLoading.value = false
  }
}

// ═══ 事件处理 ═══

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday':
      start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today':
      start = now; end = now; break
    case 'week':
      start = now.startOf('week'); end = now; break
    case 'lastWeek':
      start = now.subtract(7, 'day'); end = now; break
    case 'month':
      start = now.startOf('month'); end = now; break
    case 'lastMonth':
      start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month':
      start = now.subtract(3, 'month'); end = now; break
    case 'year':
      start = now.startOf('year'); end = now; break
    default:
      start = now.subtract(7, 'day'); end = now
  }
  dateRange.value = [start, end]
  searchParams.startDate = start.format('YYYY-MM-DD')
  searchParams.endDate = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleCategorySelect(keys: (string | number)[]) {
  if (keys.length > 0) {
    selectedCategoryId.value = keys[0]
  } else {
    selectedCategoryId.value = '0'
  }
  pagination.current = 1
  fetchData()
}

function handleCategoryExpand(keys: (string | number)[]) {
  categoryExpandedKeys.value = keys
}

// ═══ 操作 ═══

function handleAdd() {
  router.push('/sales/return-apply/create')
}

/** 导出当前 Tab 的可见列数据为 CSV（Excel 可直接打开） */
function handleExport() {
  const rows = tableData.value
  if (!rows.length) {
    message.warning('暂无数据可导出')
    return
  }
  const cols = currentColumns.value.filter((c: any) => c.field && c.type !== 'action')
  const headers = cols.map((c: any) => c.title)
  const csvRows = rows.map((r: any) =>
    cols.map((c: any) => {
      const v = r[c.field]
      if (v === null || v === undefined) return ''
      return String(v).replace(/[",\n\r]/g, ' ')
    })
  )
  const csv = [headers.join(','), ...csvRows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `销售退货申请_${activeTab.value === 'doc' ? '按单据' : '按明细'}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success(`已导出 ${rows.length} 行`)
}

/** 打印：跳转单据表单页并携带打印标记，由表单页调用打印组件 */
function openPrintForm(id: any) {
  router.push(`/sales/return-apply/form/${id}?print=1`)
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的单据')
    return
  }
  openPrintForm(selectedRowKeys.value[0])
}

/** 打印（含批量入口）：跳转单据表单页打印。
 *  ⚠️ 2026-09-22 文案修正：本页打印走「跳转表单页渲染 PrintDialog」，**一次只能打一张**，
 *  而原弹窗文案写的是「将依次打开选中的 N 张」——承诺与实际不符（onOk 只取 [0]）。
 *  真正批量打印的范本见 `sales/exchange/index.vue`（列表直接挂 PrintDialog，打印成功后
 *  调 /batch-print 回写次数），本页列表未挂该组件，登记为待完善。 */
function handleBatchPrint() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要批量打印的单据')
    return
  }
  const n = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量打印',
    content: n === 1
      ? '将打开选中的单据进行打印，确定继续吗？'
      : `当前选中 ${n} 张。本页一次只打印第一张，打印完成后可返回列表继续选择其余单据。`,
    okText: '开始打印',
    cancelText: '取消',
    onOk: () => openPrintForm(selectedRowKeys.value[0]),
  })
}

function handleView(record: any) {
  router.push(`/sales/return-apply/form/${record.id}`)
}

function handleEdit(record: any) {
  router.push(`/sales/return-apply/form/${record.id}`)
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除退货申请 ${record.returnNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await saleReturnApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error: any) {
        console.warn('[销售退货申请] 删除失败', error)
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

/** 批量审核（对标金蝶/管家婆：批量审核选中单据） */
async function handleBatchApprove() {
  const ids = selectedRowKeys.value
  if (!ids.length) { message.warning('请先选择单据'); return }
  Modal.confirm({
    title: '批量审核',
    content: `确定要审核选中的 ${ids.length} 条单据吗？`,
    onOk: async () => {
      try {
        const res = await saleReturnApi.batchApprove(ids)
        const approved = res?.approved ?? ids.length
        message.success(`已审核 ${approved} 条单据${approved < ids.length ? `，${ids.length - approved} 条跳过(非审核中状态)` : ''}`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量审核失败')
      }
    },
  })
}

/** 批量删除（对标金蝶/管家婆：批量删除选中单据） */
async function handleBatchDelete() {
  const ids = selectedRowKeys.value
  if (!ids.length) { message.warning('请先选择单据'); return }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 条单据吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(ids.map((id: number) => saleReturnApi.delete(id)))
        message.success(`已删除 ${ids.length} 条单据`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量删除失败')
      }
    },
  })
}

const handleError = (error: Error) => {
  hasError.value = true
  console.error('[销售退货申请] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
/** 合并存档配置：按存档顺序重排，新增字段沿用默认，已被移除的字段丢弃 */
function mergeQueryFields(defaults: Array<{ key: string; label: string; visible: boolean }>, saved: any[]) {
  const merged = saved
    .map(s => {
      const def = defaults.find(d => d.key === s.key)
      return def ? { ...def, ...s } : null
    })
    .filter((f): f is { key: string; label: string; visible: boolean } => !!f)
  defaults.forEach(def => {
    if (!merged.find(m => m.key === def.key)) merged.push({ ...def })
  })
  return merged
}

function loadPageConfigFromStorage() {
  try {
    const docRaw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-doc`)
    if (docRaw) {
      const parsed = JSON.parse(docRaw)
      if (Array.isArray(parsed.queryFields)) {
        docQueryConfig.value = mergeQueryFields(DEFAULT_DOC_QUERY_FIELDS, parsed.queryFields)
      }
    }
    const detailRaw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-detail`)
    if (detailRaw) {
      const parsed = JSON.parse(detailRaw)
      if (Array.isArray(parsed.queryFields)) {
        detailQueryConfig.value = mergeQueryFields(DEFAULT_DETAIL_QUERY_FIELDS, parsed.queryFields)
      }
    }
    const btnRaw = localStorage.getItem(`${PAGE_CONFIG_STORAGE_KEY}-buttons`)
    if (btnRaw) {
      const parsed = JSON.parse(btnRaw)
      if (Array.isArray(parsed.functionButtons)) {
        parsed.functionButtons.forEach((b: any) => {
          const target = functionButtonConfig.value.find(d => d.key === b.key)
          if (target) target.enabled = b.enabled
        })
      }
    }
  } catch {
    // ignore
  }
}

onMounted(() => {
  loadPageConfigFromStorage()
  setQuickDate('week')
  fetchData()
  loadCategoryTree()
})
</script>

<style scoped>
/* ═══ 查询方案 ═══ */
.query-scheme-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ═══ 快捷日期 ═══ */
.quick-dates :deep(.ant-btn) {
  font-size: 13px;
  padding: 2px 8px;
}
.quick-dates :deep(.ant-btn-primary) {
  color: #fff;
  background: #ff7a45;
  border-color: #ff7a45;
}

/* ═══ 搜索区域 ═══ */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container:not(:has([data-expanded])) > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.search-action-item { flex-shrink: 0; }
.search-action-group {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
}
.search-action-group .search-field-item {
  flex: 0 0 auto;
  width: auto;
  margin-right: 4px;
}
.search-action-group .search-field-item:last-child {
  margin-right: 0;
}
.search-more-toggle {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.search-more-toggle::before,
.search-more-toggle::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #e8e8e8;
}
.search-more-toggle :deep(.ant-btn) {
  font-size: 13px;
}

/* ═══ 表格区域 ═══ */
.table-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* ═══ 紧凑尺寸 ═══ */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
