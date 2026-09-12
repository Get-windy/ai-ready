<template>
  <ErrorBoundary>
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="activeTab === 'batch'"
        category-title="商品分类"
        :category-editable="false"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentCategoryPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-retry="fetchCategoryTree"
        @category-select="onCategorySelect"
        @category-expand="onExpand"
        @search="handleSearch"
      >
        <!-- ═══ 工具栏左侧：子标签 2/3/4 的新增 / 导入（对标：只有 1 号子标签无新增） ═══ -->
        <template #toolbar-left>
          <a-space v-if="activeTab !== 'batch'">
            <a-button
              class="btn-add"
              type="primary"
              size="small"
              @click="onAddClick"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="activeTab !== 'gradeDiscount'"
              size="small"
              @click="importVisible = true"
            >
              <UploadOutlined /> 导入
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：各子标签统一的 刷新 / 打印(F8) / 导出（+ 批量修改 / 批量删除） ═══ -->
        <template #toolbar-right>
          <a-space>
            <a-button
              size="small"
              @click="refreshAll"
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
              v-if="activeTab === 'batch'"
              size="small"
              :disabled="!selectedRows.length"
              @click="openBatchModify"
            >
              <EditOutlined /> 批量修改
            </a-button>
            <a-button
              v-if="activeTab !== 'batch'"
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-dropdown v-else>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }: any) => onMoreAction(key)">
                  <a-menu-item key="export">
                    <DownloadOutlined /> 导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button
              v-if="activeTab === 'levelPrice' || activeTab === 'customerPrice'"
              size="small"
              danger
              :disabled="!selectedRows.length"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询条件（对标：子标签 1/3/4 有查询区，子标签 2 无） ═══ -->
        <template #search-fields>
          <!-- 子标签 1：商品价格批量修改 -->
          <div
            v-if="activeTab === 'batch'"
            class="search-rows"
          >
            <div class="search-row">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="batchQuery.keyword"
                  placeholder="商品名称/货号/条码/规格/型号"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">品牌</span>
                <a-select
                  v-model:value="batchQuery.brand"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  show-search
                  :options="brandOptions"
                  :filter-option="filterOption"
                />
              </div>
              <div class="search-item">
                <span class="search-label">单位类型</span>
                <a-select
                  v-model:value="batchQuery.unitType"
                  size="small"
                  style="width: 110px"
                  :options="UNIT_TYPE_OPTIONS"
                />
              </div>
              <div class="search-item">
                <span class="search-label">商品</span>
                <a-select
                  v-model:value="batchQuery.productId"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  show-search
                  :filter-option="false"
                  :options="productOptions"
                  placeholder="商品"
                  @search="searchProducts"
                />
              </div>
              <div class="search-item">
                <span class="search-label">上架状态</span>
                <a-select
                  v-model:value="batchQuery.shelfStatus"
                  size="small"
                  style="width: 110px"
                  :options="SHELF_STATUS_OPTIONS"
                />
              </div>
              <div class="search-item">
                <span class="search-label">最近进货日期</span>
                <a-select
                  v-model:value="batchQuery.purchaseDateOp"
                  size="small"
                  style="width: 62px"
                  :options="COMPARE_OPTIONS"
                />
                <a-date-picker
                  v-model:value="batchQuery.purchaseDate"
                  size="small"
                  style="width: 140px"
                />
              </div>
              <div class="search-item">
                <span class="search-label">库存数量</span>
                <a-select
                  v-model:value="batchQuery.stockQtyOp"
                  size="small"
                  style="width: 62px"
                  :options="COMPARE_OPTIONS"
                />
                <a-input
                  v-model:value="batchQuery.stockQty"
                  size="small"
                  style="width: 100px"
                  placeholder="数量"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
            <div class="search-row">
              <a-checkbox
                v-model:checked="batchQuery.showHierarchy"
                @change="handleSearch"
              >
                显示层次结构
              </a-checkbox>
            </div>
          </div>

          <!-- 子标签 3：级别指定价设置 -->
          <div
            v-else-if="activeTab === 'levelPrice'"
            class="search-row"
          >
            <div class="search-item">
              <span class="search-label">客户级别</span>
              <a-select
                v-model:value="levelQuery.gradeName"
                size="small"
                style="width: 160px"
                allow-clear
                show-search
                :options="customerGradeOptions"
                :filter-option="filterOption"
              />
            </div>
            <div class="search-item">
              <span class="search-label">商品/分类名称</span>
              <a-input
                v-model:value="levelQuery.keyword"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">品牌</span>
              <a-select
                v-model:value="levelQuery.brand"
                size="small"
                style="width: 140px"
                allow-clear
                show-search
                :options="brandOptions"
                :filter-option="filterOption"
              />
            </div>
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
          </div>

          <!-- 子标签 4：客户指定价设置 -->
          <div
            v-else-if="activeTab === 'customerPrice'"
            class="search-row"
          >
            <div class="search-item">
              <span class="search-label">客户</span>
              <a-select
                v-model:value="customerQuery.customerId"
                size="small"
                style="width: 180px"
                allow-clear
                show-search
                :options="customerOptions"
                :filter-option="filterOption"
                placeholder="客户"
              />
            </div>
            <div class="search-item">
              <span class="search-label">商品</span>
              <a-select
                v-model:value="customerQuery.productId"
                size="small"
                style="width: 180px"
                allow-clear
                show-search
                :options="productOptions"
                :filter-option="filterOption"
                placeholder="商品"
              />
            </div>
            <div class="search-item">
              <span class="search-label">品牌</span>
              <a-select
                v-model:value="customerQuery.brand"
                size="small"
                style="width: 140px"
                allow-clear
                show-search
                :options="brandOptions"
                :filter-option="filterOption"
              />
            </div>
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
          </div>
        </template>

        <!-- ═══ 数据表（4 个子标签各一张，独立列配置） ═══ -->
        <template #table>
          <div class="table-holder">
            <!-- 子标签 1 -->
            <BillDetailTable
              v-if="activeTab === 'batch'"
              ref="batchTableRef"
              :columns="batchColumns"
              v-model:data-source="batchData"
              :loading="loading"
              :view-mode="false"
              storage-key="md-product-price-batch-columns"
              global-config-key="md-product-price-batch"
              @checkbox-change="onCheckboxChange"
              @checkbox-all="onCheckboxAll"
              @cell-change="onPriceCellChange"
            >
              <template #imageCell="{ record }">
                <a-image
                  v-if="record.imageUrl"
                  :src="record.imageUrl"
                  :width="36"
                  :height="36"
                  style="border-radius: 4px; object-fit: cover;"
                  :preview="({ mask: false } as any)"
                  fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="
                />
                <span
                  v-else
                  class="text-muted"
                >-</span>
              </template>
              <template #shelfStatusCell="{ record }">
                <a-tag
                  v-if="record.shelfStatus === 1"
                  color="green"
                >
                  已上架
                </a-tag>
                <a-tag v-else>
                  未上架
                </a-tag>
              </template>
            </BillDetailTable>

            <!-- 子标签 2 -->
            <BillDetailTable
              v-else-if="activeTab === 'gradeDiscount'"
              ref="gradeTableRef"
              :columns="gradeDiscountColumns"
              v-model:data-source="gradeDiscountData"
              :loading="loading"
              :view-mode="true"
              storage-key="md-product-price-grade-columns"
              global-config-key="md-product-price-grade"
              :show-pagination="false"
            >
              <template #actionCell="{ record }">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="openGradeDiscountModal(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteGradeDiscount(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- 子标签 3 -->
            <BillDetailTable
              v-else-if="activeTab === 'levelPrice'"
              ref="levelTableRef"
              :columns="ruleColumns"
              v-model:data-source="levelPriceData"
              :loading="loading"
              :view-mode="true"
              storage-key="md-product-price-level-columns"
              global-config-key="md-product-price-level"
              @checkbox-change="onCheckboxChange"
              @checkbox-all="onCheckboxAll"
            >
              <template #actionCell="{ record }">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="openRuleModal(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteRule(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- 子标签 4 -->
            <BillDetailTable
              v-else
              ref="customerTableRef"
              :columns="ruleColumns"
              v-model:data-source="customerPriceData"
              :loading="loading"
              :view-mode="true"
              storage-key="md-product-price-customer-columns"
              global-config-key="md-product-price-customer"
              @checkbox-change="onCheckboxChange"
              @checkbox-all="onCheckboxAll"
            >
              <template #actionCell="{ record }">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    @click="openRuleModal(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteRule(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <!-- ═══ 批量修改弹窗（对标「批量修改」） ═══ -->
      <a-modal
        v-model:open="batchModalVisible"
        title="批量修改"
        :width="820"
        :mask-closable="false"
      >
        <a-alert
          type="info"
          show-icon
          :message="`已选中 ${selectedRows.length} 条商品价格，修改后将实时生效到销售/采购取价`"
          style="margin-bottom: 12px"
        />
        <div
          v-for="(item, idx) in batchItems"
          :key="idx"
          class="batch-item"
        >
          <a-select
            v-model:value="item.field"
            size="small"
            style="width: 150px"
            :options="priceFieldOptions"
            placeholder="价格项"
          />
          <a-select
            v-model:value="item.mode"
            size="small"
            style="width: 110px"
            :options="[{ value: 'FIXED', label: '直接改价' }, { value: 'RULE', label: '按规则改价' }]"
          />
          <template v-if="item.mode === 'FIXED'">
            <a-input-number
              v-model:value="item.value"
              size="small"
              style="width: 140px"
              :min="0"
              :precision="2"
              placeholder="新价格"
            />
          </template>
          <template v-else>
            <a-select
              v-model:value="item.basePriceField"
              size="small"
              style="width: 150px"
              :options="priceFieldOptions"
              placeholder="基础价"
            />
            <a-select
              v-model:value="item.calcOperator"
              size="small"
              style="width: 70px"
              :options="OPERATOR_OPTIONS"
            />
            <a-input-number
              v-model:value="item.calcValue"
              size="small"
              style="width: 110px"
              placeholder="数值"
            />
          </template>
          <a-button
            type="link"
            danger
            size="small"
            :disabled="batchItems.length === 1"
            @click="batchItems.splice(idx, 1)"
          >
            移除
          </a-button>
        </div>
        <a-button
          type="dashed"
          size="small"
          block
          @click="batchItems.push({ field: 'retailPrice', mode: 'FIXED', value: null, basePriceField: 'retailPrice', calcOperator: '*', calcValue: null })"
        >
          <PlusOutlined /> 添加价格项
        </a-button>
        <template #footer>
          <a-button @click="batchModalVisible = false">
            取消
          </a-button>
          <a-button
            type="primary"
            :loading="saving"
            @click="submitBatchModify"
          >
            保存
          </a-button>
        </template>
      </a-modal>

      <!-- ═══ 客户级别折扣 新增/编辑（对标「客户级别」弹窗） ═══ -->
      <a-modal
        v-model:open="gradeModalVisible"
        :title="gradeForm.id ? '修改客户级别折扣' : '新增客户级别折扣'"
        :width="520"
        :mask-closable="false"
      >
        <a-form
          layout="horizontal"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="客户级别"
            required
          >
            <a-select
              v-model:value="gradeForm.gradeName"
              size="small"
              show-search
              :options="customerGradeOptions"
              :filter-option="filterOption"
              placeholder="请选择客户级别"
            />
          </a-form-item>
          <a-form-item
            label="默认级别价"
            required
          >
            <div class="rule-inline">
              <span class="rule-eq">=</span>
              <a-select
                v-model:value="gradeForm.basePriceType"
                size="small"
                style="width: 130px"
                :options="basePriceOptions"
              />
              <a-select
                v-model:value="gradeForm.calcOperator"
                size="small"
                style="width: 64px"
                :options="OPERATOR_OPTIONS"
              />
              <a-input-number
                v-model:value="gradeForm.calcValue"
                size="small"
                style="width: 110px"
                :precision="2"
              />
            </div>
          </a-form-item>
          <a-form-item label="规则预览">
            <span class="rule-preview">{{ gradeRulePreview }}</span>
          </a-form-item>
        </a-form>
        <template #footer>
          <a-button @click="gradeModalVisible = false">
            关闭(Esc)
          </a-button>
          <a-button
            type="primary"
            :loading="saving"
            @click="submitGradeDiscount"
          >
            保存(Enter)
          </a-button>
        </template>
      </a-modal>

      <!-- ═══ 指定价 新增/编辑（子标签 3/4 共用，对标「指定价编辑」） ═══ -->
      <a-modal
        v-model:open="ruleModalVisible"
        :title="ruleForm.id ? '指定价编辑' : '指定价新增'"
        :width="620"
        :mask-closable="false"
      >
        <a-form
          layout="horizontal"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-form-item
            v-if="activeTab === 'levelPrice'"
            label="客户级别"
            required
          >
            <a-select
              v-model:value="ruleForm.gradeName"
              size="small"
              show-search
              :options="customerGradeOptions"
              :filter-option="filterOption"
              placeholder="请选择客户级别"
            />
          </a-form-item>
          <a-form-item
            v-else-if="activeTab === 'customerPrice'"
            label="客户"
            required
          >
            <a-select
              v-model:value="ruleForm.customerId"
              size="small"
              show-search
              :options="customerOptions"
              :filter-option="filterOption"
              placeholder="请选择客户"
            />
          </a-form-item>
          <a-form-item
            label="商品"
            required
          >
            <div class="rule-inline">
              <a-input
                :value="ruleForm.productName"
                size="small"
                readonly
                placeholder="请选择商品"
                style="width: 260px"
              />
              <a-button
                size="small"
                @click="productPickerVisible = true"
              >
                选择商品
              </a-button>
            </div>
          </a-form-item>
          <a-form-item label="单位">
            <a-input
              v-model:value="ruleForm.unitName"
              size="small"
              placeholder="选填，如 箱 / 瓶"
            />
          </a-form-item>
          <a-form-item
            label="指定价"
            required
          >
            <div class="rule-inline">
              <span class="rule-eq">=</span>
              <a-select
                v-model:value="ruleForm.basePriceType"
                size="small"
                style="width: 140px"
                allow-clear
                :options="basePriceOptions"
                placeholder="直接指定价"
              />
              <a-select
                v-model:value="ruleForm.calcOperator"
                size="small"
                style="width: 64px"
                :options="OPERATOR_OPTIONS"
              />
              <a-input-number
                v-model:value="ruleForm.calcValue"
                size="small"
                style="width: 110px"
                :precision="2"
              />
            </div>
          </a-form-item>
          <a-form-item label=" ">
            <a-checkbox v-model:checked="ruleForm.autoConvert">
              单位价格自动换算
            </a-checkbox>
          </a-form-item>
          <a-form-item label="规则预览">
            <span class="rule-preview">{{ rulePreview }}</span>
          </a-form-item>
        </a-form>
        <template #footer>
          <a-button @click="ruleModalVisible = false">
            取消(Esc)
          </a-button>
          <a-button
            type="primary"
            :loading="saving"
            @click="submitRule"
          >
            保存(Enter)
          </a-button>
        </template>
      </a-modal>

      <!-- ═══ 商品选择（指定价弹窗内） ═══ -->
      <ProductSelectModal
        v-model:open="productPickerVisible"
        :selected-ids="ruleForm.productId ? [Number(ruleForm.productId)] : []"
        @confirm="onProductPicked"
      />

      <!-- ═══ 导入向导（对标「基本信息导入」三步） ═══ -->
      <BaseDataImportWizard
        v-if="activeTab !== 'gradeDiscount'"
        v-model:open="importVisible"
        title="基本信息导入"
        template-url="/erp/md/product-price/import-template"
        :template-params="{ type: activeTab === 'levelPrice' ? 'level' : 'customer' }"
        :template-file-name="activeTab === 'levelPrice' ? '级别指定价导入模板' : '客户指定价导入模板'"
        import-url="/erp/md/product-price/import-excel"
        :import-params="{ type: activeTab === 'levelPrice' ? 'level' : 'customer' }"
        @success="handleImportSuccess"
      />

      <!-- 打印(F8) -->
      <PrintDialog
        ref="printDialogRef"
        page-code="md:product-price"
        :print-data="printData"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  UploadOutlined,
  DeleteOutlined,
  EditOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import { productCategoryApi } from '@/api/erp/product'
import { optionsApi } from '@/api/options'
import request from '@/utils/request'
import {
  productPriceApi,
  type ProductPriceRow,
  type GradePriceRow,
  type CustomerGradeDiscount,
  type PriceModifyItem,
} from '@/api/md-product-price'

/** 子标签（对标 4 个，顺序固定） */
const TABS = [
  { key: 'batch', label: '商品价格批量修改' },
  { key: 'gradeDiscount', label: '客户级别折扣设置' },
  { key: 'levelPrice', label: '级别指定价设置' },
  { key: 'customerPrice', label: '客户指定价设置' },
]

/** 单位类型（对标下拉：全部/小单位/中单位/大单位） */
const UNIT_TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: 'SMALL', label: '小单位' },
  { value: 'MEDIUM', label: '中单位' },
  { value: 'LARGE', label: '大单位' },
]
const SHELF_STATUS_OPTIONS = [
  { value: undefined, label: '全部' },
  { value: 1, label: '已上架' },
  { value: 0, label: '未上架' },
]
/** 比较符（对标下拉：< = > ≠ ≤ ≥，默认 ≥） */
const COMPARE_OPTIONS = [
  { value: '<', label: '<' },
  { value: '=', label: '=' },
  { value: '>', label: '>' },
  { value: '!=', label: '≠' },
  { value: '<=', label: '≤' },
  { value: '>=', label: '≥' },
]
const OPERATOR_OPTIONS = [
  { value: '*', label: '*' },
  { value: '+', label: '+' },
  { value: '-', label: '-' },
  { value: '/', label: '/' },
]

const activeTab = ref('batch')
const loading = ref(false)
const saving = ref(false)
const selectedRows = ref<any[]>([])

// ═══ 分类树（仅子标签 1 展示，口径与《商品条码》一致） ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTree = ref<any[]>([])
const selectedCategoryId = ref('0')
const expandedKeys = ref<string[]>([])
const categoryTreeData = computed(() => categoryTree.value)

const currentCategoryPath = computed(() => {
  if (selectedCategoryId.value === '0' || !categoryTree.value.length) return '全部商品'
  const path: string[] = []
  const walk = (nodes: any[], target: string): boolean => {
    for (const node of nodes) {
      path.push(node.categoryName)
      if (String(node.id) === target) return true
      if (node.children?.length && walk(node.children, target)) return true
      path.pop()
    }
    return false
  }
  walk(categoryTree.value, selectedCategoryId.value)
  return path.length ? path.join(' / ') : '全部商品'
})

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data = await productCategoryApi.getTree()
    categoryTree.value = Array.isArray(data) ? data : []
    const firstLevel = categoryTree.value.map(n => String(n.id))
    if (firstLevel.length) expandedKeys.value = [...new Set([...firstLevel, ...expandedKeys.value])]
  } catch (e) {
    console.error('[商品价格管理] 加载商品分类失败', e)
    categoryError.value = true
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  selectedCategoryId.value = keys[0] != null ? String(keys[0]) : '0'
  pagination.current = 1
  fetchList()
}

function onExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

// ═══ 下拉数据（品牌 / 客户级别 / 商品 / 客户 / 价格等级） ═══
const brandOptions = ref<{ label: string; value: string }[]>([])
const customerGradeOptions = ref<{ label: string; value: string }[]>([])
/** 商品下拉：远程搜索（商品量大时按关键字取前 50 条，避免一次拉全量） */
const productOptions = ref<{ label: string; value: string }[]>([])
let productSearchTimer: ReturnType<typeof setTimeout> | null = null

async function searchProducts(keyword: string) {
  if (productSearchTimer) clearTimeout(productSearchTimer)
  productSearchTimer = setTimeout(async () => {
    try {
      const res: any = await request.get('/erp/product/page', {
        params: { pageNum: 1, pageSize: 50, keyword: keyword || undefined },
      })
      productOptions.value = (res?.records || []).map((p: any) => ({
        label: p.productCode ? `${p.productName} / ${p.productCode}` : p.productName,
        value: String(p.id),
      }))
    } catch (e) {
      console.warn('[商品价格管理] 商品搜索失败', e)
    }
  }, 300)
}
const customerOptions = ref<{ label: string; value: string }[]>([])
/** 价格等级（8 个槽位，名称可自定义 → 列标题动态取此处名称） */
const productGrades = ref<{ key: string; name: string }[]>([])
const priceFieldOptions = computed(() => {
  const base = [
    { value: 'presetPurchasePrice', label: '预设进价' },
    { value: 'referenceCost', label: '参考成本' },
    { value: 'wholesalePrice', label: '批发价' },
    { value: 'minDiscount', label: '最低折扣(%)' },
    { value: 'minSalePrice', label: '最低售价' },
    { value: 'retailPrice', label: '零售价' },
  ]
  return [...base, ...productGrades.value.map(g => ({ value: g.key, label: g.name }))]
})
const basePriceOptions = computed(() => [
  { value: '零售价', label: '零售价' },
  { value: '批发价', label: '批发价' },
  { value: '最低售价', label: '最低售价' },
  ...productGrades.value.map(g => ({ value: g.name, label: g.name })),
])

function filterOption(input: string, option: any) {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function loadOptions() {
  try {
    const [brands, grades, products, customers] = await Promise.all([
      productPriceApi.getBrands(),
      productPriceApi.getCustomerGrades(),
      optionsApi.getProducts(),
      optionsApi.getCustomers(),
    ])
    brandOptions.value = (brands || []).map(b => ({ label: b, value: b }))
    customerGradeOptions.value = (grades || []).map(g => ({ label: g, value: g }))
    productOptions.value = (products || []).slice(0, 50).map((p: any) => ({ label: p.name, value: p.value ?? p.id }))
    customerOptions.value = (customers || []).map((c: any) => ({ label: c.name, value: c.value ?? c.id }))
  } catch (e) {
    console.warn('[商品价格管理] 下拉数据加载失败', e)
  }
}

async function loadGrades() {
  try {
    // 等级名与槽位顺序统一由后端给出（与导出表头同源）
    const [grades, names] = await Promise.all([
      request.get('/erp/product-grade/list') as Promise<any>,
      productPriceApi.getGrades(),
    ])
    const rows = Array.isArray(grades) ? grades : []
    productGrades.value = rows
      .slice()
      .sort((a: any, b: any) => (a.gradeLevel ?? 99) - (b.gradeLevel ?? 99))
      .map((g: any, idx: number) => ({
        key: `gradePrice${(g.gradeLevel ?? idx + 1)}`,
        name: (names && names[idx]) || g.gradeName || `价格等级${idx + 1}`,
      }))
  } catch (e) {
    console.warn('[商品价格管理] 价格等级加载失败', e)
  }
}

// ═══ 分页（各子标签共用一套，切换时重置） ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 子标签 1：商品价格批量修改 ═══
const batchQuery = reactive({
  keyword: '',
  brand: undefined as string | undefined,
  unitType: '',
  productId: undefined as string | undefined,
  shelfStatus: undefined as number | undefined,
  purchaseDateOp: '>=',
  purchaseDate: null as any,
  stockQtyOp: '>=',
  stockQty: '',
  showHierarchy: false,
})
const batchTableRef = ref<any>(null)
const batchData = ref<ProductPriceRow[]>([])

/** 对标默认展开的价格等级列（其余等级列默认隐藏） */
const DEFAULT_VISIBLE_GRADES = ['gradePrice1', 'gradePrice2', 'gradePrice3', 'gradePrice4', 'gradePrice5', 'gradePrice6', 'gradePrice7', 'gradePrice8']

/** 子标签 1 列（对标全量 29 列，默认显示 21 列） */
const batchColumns = computed<DetailColumnConfig[]>(() => {
  const fixed: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'checkbox', title: '', type: 'checkbox', width: 44, fixed: 'left' },
  ]
  // 8 个价格等级列：行内直接输入即可改价（其余列一律只读）
  const gradeColumns: DetailColumnConfig[] = productGrades.value.map(g => ({
    key: g.key,
    title: g.name,
    type: 'number',
    width: 110,
    align: 'right',
    placeholder: '-',
    defaultHidden: !DEFAULT_VISIBLE_GRADES.includes(g.key),
  }))
  const data: DetailColumnConfig[] = [
    { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 60, fixed: 'left' },
    { key: 'shelfStatusText', title: '上架', type: 'slot', slotName: 'shelfStatusCell', width: 80, defaultHidden: true },
    { key: 'productCode', title: '商品货号', type: 'input', width: 140, defaultHidden: true, sortable: true, readonly: true },
    { key: 'productName', title: '商品名称', type: 'input', width: 220, sortable: true, fixed: 'left', readonly: true },
    { key: 'unitName', title: '单位', type: 'input', width: 70, fixed: 'left', readonly: true },
    { key: 'brand', title: '品牌', type: 'input', width: 110, readonly: true },
    { key: 'conversionRelation', title: '换算关系', type: 'input', width: 120, readonly: true, headerTip: '非基础单位显示与基础单位的换算，如 1箱=12瓶；基础单位不显示' },
    { key: 'barcode', title: '条码', type: 'input', width: 160, defaultHidden: true, readonly: true },
    { key: 'recentPurchasePrice', title: '最近进价', type: 'input', width: 100, align: 'right', readonly: true },
    { key: 'presetPurchasePrice', title: '预设进价', type: 'input', width: 100, align: 'right', defaultHidden: true, readonly: true },
    { key: 'referenceCost', title: '参考成本', type: 'input', width: 100, align: 'right', defaultHidden: true, readonly: true },
    { key: 'stockQty', title: '账面库存', type: 'input', width: 100, align: 'right', readonly: true },
    { key: 'costAvgPrice', title: '成本均价', type: 'input', width: 100, align: 'right', readonly: true },
    // 批发价 / 最低售价 / 零售价：与价格等级列一样支持行内直接输入改价
    { key: 'wholesalePrice', title: '批发价', type: 'number', width: 100, align: 'right', placeholder: '-' },
    { key: 'minDiscount', title: '最低折扣(%)', type: 'input', width: 110, align: 'right', readonly: true },
    { key: 'minSalePrice', title: '最低售价', type: 'number', width: 100, align: 'right', placeholder: '-' },
    { key: 'retailPrice', title: '零售价', type: 'number', width: 100, align: 'right', placeholder: '-' },
    ...gradeColumns,
    { key: 'spec', title: '规格', type: 'input', width: 120, readonly: true },
    { key: 'lastPurchaseDate', title: '最近进货日期', type: 'input', width: 120, defaultHidden: true, readonly: true },
    { key: 'model', title: '型号', type: 'input', width: 100, defaultHidden: true, readonly: true },
    { key: 'origin', title: '产地', type: 'input', width: 100, defaultHidden: true, readonly: true },
  ]
  return [...fixed, ...data]
})

// ═══ 子标签 2：客户级别折扣设置 ═══
const gradeTableRef = ref<any>(null)
const gradeDiscountData = ref<CustomerGradeDiscount[]>([])

const gradeDiscountColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  { key: 'gradeName', title: '客户级别', type: 'input', width: 260 },
  { key: 'previewText', title: '级别默认价', type: 'input', width: 300 },
  { key: 'lastModifierName', title: '最后修改人', type: 'input', width: 140, defaultHidden: true },
  {
    key: 'updateTime',
    title: '最后修改时间',
    type: 'input',
    width: 170,
    defaultHidden: true,
    formatter: (value: any) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : ''),
  },
])

// ═══ 子标签 3/4：指定价（共用列配置，子标签 4 多客户名称/客户编号两列） ═══
const levelTableRef = ref<any>(null)
const customerTableRef = ref<any>(null)
const levelPriceData = ref<GradePriceRow[]>([])
const customerPriceData = ref<GradePriceRow[]>([])

const ruleColumns = computed<DetailColumnConfig[]>(() => {
  const fixed: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'checkbox', title: '', type: 'checkbox', width: 44, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  ]
  const customerColumns: DetailColumnConfig[] = activeTab.value === 'customerPrice'
    ? [
        { key: 'customerName', title: '客户名称', type: 'input', width: 200, sortable: true },
        { key: 'customerCode', title: '客户编号', type: 'input', width: 140 },
      ]
    : []
  const levelColumns: DetailColumnConfig[] = activeTab.value === 'levelPrice'
    ? [{ key: 'gradeName', title: '客户级别', type: 'input', width: 160, sortable: true }]
    : []
  const gradeColumns: DetailColumnConfig[] = productGrades.value.map(g => ({
    key: g.key,
    title: g.name,
    type: 'input',
    width: 110,
    align: 'right',
    defaultHidden: true,
  }))
  const data: DetailColumnConfig[] = [
    ...customerColumns,
    ...levelColumns,
    { key: 'targetName', title: '商品/分类名称', type: 'input', width: 220, sortable: true },
    { key: 'productCode', title: '货号', type: 'input', width: 140 },
    { key: 'unitName', title: '单位', type: 'input', width: 70 },
    { key: 'priceRule', title: '价格规则', type: 'input', width: 150 },
    { key: 'barcode', title: '条码', type: 'input', width: 160 },
    { key: 'spec', title: '规格', type: 'input', width: 120 },
    { key: 'model', title: '型号', type: 'input', width: 100 },
    { key: 'brand', title: '品牌', type: 'input', width: 110 },
    { key: 'presetPurchasePrice', title: '预设进价', type: 'input', width: 100, align: 'right', defaultHidden: true },
    { key: 'retailPrice', title: '零售价', type: 'input', width: 100, align: 'right' },
    { key: 'wholesalePrice', title: '批发价', type: 'input', width: 100, align: 'right' },
    ...gradeColumns,
    { key: 'lastModifierName', title: '最后修改人', type: 'input', width: 120, defaultHidden: true },
    {
      key: 'lastModifyTime',
      title: '最后修改时间',
      type: 'input',
      width: 170,
      defaultHidden: true,
      formatter: (value: any) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : ''),
    },
  ]
  return [...fixed, ...data]
})

// ═══ 查询 / 列表加载 ═══
const levelQuery = reactive({ gradeName: undefined as string | undefined, keyword: '', brand: undefined as string | undefined })
const customerQuery = reactive({ customerId: undefined as string | undefined, productId: undefined as string | undefined, brand: undefined as string | undefined })

/**
 * 勾选状态按事件参数维护（表格按行号维护选中，父层按行对象维护），
 * 避免依赖组件内部 checkedRecords 的读取时序。
 */
function onCheckboxChange(record: any, rowIndex: number, checked: boolean) {
  const list = selectedRows.value.filter(r => r.__rowIndex !== rowIndex)
  if (checked) {
    list.push({ ...record, __rowIndex: rowIndex })
  }
  selectedRows.value = list
}

function onCheckboxAll(_checked: boolean, rows: any[]) {
  selectedRows.value = (rows || []).map((r: any, i: number) => ({ ...r, __rowIndex: i }))
}

function onTabChange(key: string) {
  activeTab.value = key
  selectedRows.value = []
  pagination.current = 1
  fetchList()
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'batch') {
      const res = await productPriceApi.page({
        categoryId: selectedCategoryId.value !== '0' ? selectedCategoryId.value : undefined,
        keyword: batchQuery.keyword || undefined,
        brand: batchQuery.brand || undefined,
        unitType: batchQuery.unitType || undefined,
        productId: batchQuery.productId || undefined,
        shelfStatus: batchQuery.shelfStatus,
        purchaseDateOp: batchQuery.purchaseDate ? batchQuery.purchaseDateOp : undefined,
        purchaseDate: batchQuery.purchaseDate ? dayjs(batchQuery.purchaseDate).format('YYYY-MM-DD') : undefined,
        stockQtyOp: batchQuery.stockQty ? batchQuery.stockQtyOp : undefined,
        stockQty: batchQuery.stockQty || undefined,
        showHierarchy: batchQuery.showHierarchy || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      batchData.value = res?.records || []
      pagination.total = res?.total || 0
    } else if (activeTab.value === 'gradeDiscount') {
      const res = await productPriceApi.gradeDiscountPage({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      gradeDiscountData.value = res?.records || []
      pagination.total = res?.total || 0
    } else if (activeTab.value === 'levelPrice') {
      const res = await productPriceApi.levelPricePage({
        gradeName: levelQuery.gradeName || undefined,
        keyword: levelQuery.keyword || undefined,
        brand: levelQuery.brand || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      levelPriceData.value = res?.records || []
      pagination.total = res?.total || 0
    } else {
      const res = await productPriceApi.customerPricePage({
        customerId: customerQuery.customerId || undefined,
        productId: customerQuery.productId || undefined,
        brand: customerQuery.brand || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      customerPriceData.value = res?.records || []
      pagination.total = res?.total || 0
    }
  } catch (e) {
    console.error('[商品价格管理] 加载列表失败', e)
    message.error('加载商品价格数据失败')
    if (activeTab.value === 'batch') batchData.value = []
    else if (activeTab.value === 'gradeDiscount') gradeDiscountData.value = []
    else if (activeTab.value === 'levelPrice') levelPriceData.value = []
    else customerPriceData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function onMoreAction(key: string) {
  if (key === 'export') handleExport()
}

function refreshAll() {
  fetchCategoryTree()
  fetchList()
}

// ═══ 导出（各子标签真实 Excel） ═══
async function handleExport() {
  try {
    let blob: any
    let fileName = '商品价格管理'
    if (activeTab.value === 'batch') {
      blob = await productPriceApi.exportExcel({
        categoryId: selectedCategoryId.value !== '0' ? selectedCategoryId.value : undefined,
        keyword: batchQuery.keyword || undefined,
        brand: batchQuery.brand || undefined,
        unitType: batchQuery.unitType || undefined,
        productId: batchQuery.productId || undefined,
        shelfStatus: batchQuery.shelfStatus,
        purchaseDateOp: batchQuery.purchaseDate ? batchQuery.purchaseDateOp : undefined,
        purchaseDate: batchQuery.purchaseDate ? dayjs(batchQuery.purchaseDate).format('YYYY-MM-DD') : undefined,
        stockQtyOp: batchQuery.stockQty ? batchQuery.stockQtyOp : undefined,
        stockQty: batchQuery.stockQty || undefined,
        showHierarchy: batchQuery.showHierarchy || undefined,
      })
      fileName = '商品价格管理'
    } else if (activeTab.value === 'gradeDiscount') {
      blob = await productPriceApi.gradeDiscountExport()
      fileName = '客户级别折扣设置'
    } else if (activeTab.value === 'levelPrice') {
      blob = await productPriceApi.levelPriceExport({
        gradeName: levelQuery.gradeName || undefined,
        keyword: levelQuery.keyword || undefined,
        brand: levelQuery.brand || undefined,
      })
      fileName = '级别指定价设置'
    } else {
      blob = await productPriceApi.customerPriceExport({
        customerId: customerQuery.customerId || undefined,
        productId: customerQuery.productId || undefined,
        brand: customerQuery.brand || undefined,
      })
      fileName = '客户指定价设置'
    }
    const url = window.URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${fileName}_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.error('[商品价格管理] 导出失败', e)
    message.error('导出失败')
  }
}

// ═══ 打印(F8) ═══
const printDialogRef = ref<any>(null)
const printData = computed<Record<string, any>>(() => {
  const rows = currentTabRows()
  return {
    pageTitle: TABS.find(t => t.key === activeTab.value)?.label || '商品价格管理',
    rows,
    total: pagination.total,
    printTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
  }
})

function currentTabRows(): any[] {
  if (activeTab.value === 'batch') return batchData.value
  if (activeTab.value === 'gradeDiscount') return gradeDiscountData.value
  if (activeTab.value === 'levelPrice') return levelPriceData.value
  return customerPriceData.value
}

function handlePrint() {
  printDialogRef.value?.open?.()
}

// ═══ 子标签 1：价格列行内改价（点击单元格 → 输入 → 回车/失焦即保存） ═══
/** 行内可改价字段：8 个价格等级 + 批发价/零售价/最低售价（其余列只读） */
const GRADE_PRICE_FIELDS = ['gradePrice1', 'gradePrice2', 'gradePrice3', 'gradePrice4', 'gradePrice5', 'gradePrice6', 'gradePrice7', 'gradePrice8']
const EDITABLE_PRICE_FIELDS = [...GRADE_PRICE_FIELDS, 'wholesalePrice', 'retailPrice', 'minSalePrice']
const BASIC_PRICE_LABELS: Record<string, string> = {
  wholesalePrice: '批发价',
  retailPrice: '零售价',
  minSalePrice: '最低售价',
}

async function onPriceCellChange(record: any, field: string, value: any) {
  if (!EDITABLE_PRICE_FIELDS.includes(field) || !record?.unitId) return
  const num = value === '' || value === null || value === undefined ? null : Number(value)
  if (num === null || Number.isNaN(num) || num < 0) {
    message.warning('请输入有效的价格')
    fetchList()
    return
  }
  const normalized = Math.round(num * 100) / 100
  try {
    await productPriceApi.batchModify({
      unitIds: [String(record.unitId)],
      items: [{ field, mode: 'FIXED', value: normalized }],
    })
    record[field] = normalized
    const fieldLabel = BASIC_PRICE_LABELS[field] || productGrades.value.find(g => g.key === field)?.name || field
    message.success(`${record.productName}「${fieldLabel}」已保存为 ${normalized}`)
  } catch (e: any) {
    message.error(e?.message || '价格保存失败，已恢复原值')
    fetchList()
  }
}

// ═══ 子标签 1：批量修改 ═══
const batchModalVisible = ref(false)
const batchItems = ref<any[]>([])

function openBatchModify() {
  if (!selectedRows.value.length) {
    message.warning('请选中至少一条数据！')
    return
  }
  batchItems.value = [{ field: 'retailPrice', mode: 'FIXED', value: null, basePriceField: 'retailPrice', calcOperator: '*', calcValue: null }]
  batchModalVisible.value = true
}

async function submitBatchModify() {
  const items: PriceModifyItem[] = batchItems.value
    .filter(item => !!item.field)
    .map(item => ({
      field: item.field,
      mode: item.mode,
      value: item.mode === 'FIXED' ? item.value : null,
      basePriceField: item.mode === 'RULE' ? item.basePriceField : undefined,
      calcOperator: item.mode === 'RULE' ? item.calcOperator : undefined,
      calcValue: item.mode === 'RULE' ? item.calcValue : undefined,
    }))
  if (!items.length) {
    message.warning('请至少填写一个价格修改项')
    return
  }
  saving.value = true
  try {
    const count = await productPriceApi.batchModify({
      unitIds: selectedRows.value.map(r => r.unitId),
      items,
    })
    message.success(`已修改 ${count} 条商品价格`)
    batchModalVisible.value = false
    selectedRows.value = []
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '批量修改失败')
  } finally {
    saving.value = false
  }
}

// ═══ 子标签 2：客户级别折扣 新增/编辑 ═══
const gradeModalVisible = ref(false)
const gradeForm = reactive<CustomerGradeDiscount>({
  id: undefined,
  gradeName: '',
  basePriceType: '批发价',
  calcOperator: '*',
  calcValue: 1,
})

const gradeRulePreview = computed(() => {
  const value = gradeForm.calcValue ?? 1
  return `客户级别【${gradeForm.gradeName || ''}】的订货价格=${gradeForm.basePriceType}${gradeForm.calcOperator}${value}`
})

function onAddClick() {
  if (activeTab.value === 'gradeDiscount') {
    openGradeDiscountModal()
  } else {
    openRuleModal()
  }
}

function openGradeDiscountModal(record?: CustomerGradeDiscount) {
  if (record) {
    Object.assign(gradeForm, {
      id: record.id,
      gradeName: record.gradeName,
      basePriceType: record.basePriceType || '批发价',
      calcOperator: record.calcOperator || '*',
      calcValue: record.calcValue ?? 1,
    })
  } else {
    Object.assign(gradeForm, { id: undefined, gradeName: '', basePriceType: '批发价', calcOperator: '*', calcValue: 1 })
  }
  gradeModalVisible.value = true
}

async function submitGradeDiscount() {
  if (!gradeForm.gradeName) {
    message.warning('请选择客户级别')
    return
  }
  saving.value = true
  try {
    await productPriceApi.saveGradeDiscount({ ...gradeForm })
    message.success('保存成功')
    gradeModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDeleteGradeDiscount(record: CustomerGradeDiscount) {
  Modal.confirm({
    title: '删除客户级别折扣',
    content: `确定删除「${record.gradeName}」的级别默认价设置吗？`,
    okType: 'danger',
    onOk: async () => {
      await productPriceApi.deleteGradeDiscount(String(record.id))
      message.success('删除成功')
      fetchList()
    },
  })
}

// ═══ 子标签 3/4：指定价 新增/编辑 ═══
const ruleModalVisible = ref(false)
const productPickerVisible = ref(false)
const ruleForm = reactive({
  id: undefined as string | undefined,
  gradeName: '',
  customerId: undefined as string | undefined,
  productId: undefined as string | undefined,
  productName: '',
  productCode: '',
  unitName: '',
  basePriceType: undefined as string | undefined,
  calcOperator: '+',
  calcValue: 0,
  autoConvert: false,
})

const rulePreview = computed(() => {
  const owner = activeTab.value === 'levelPrice'
    ? `客户级别【${ruleForm.gradeName || ''}】`
    : `客户【${customerOptions.value.find(c => String(c.value) === String(ruleForm.customerId))?.label || ''}】`
  const value = ruleForm.calcValue ?? 0
  if (!ruleForm.basePriceType) {
    return `${owner}的商品指导价=指定价 ${value}`
  }
  return `${owner}的商品指导价=${ruleForm.basePriceType}${ruleForm.calcOperator}${value}`
})

function openRuleModal(record?: GradePriceRow) {
  if (record) {
    Object.assign(ruleForm, {
      id: record.id,
      gradeName: record.gradeName || '',
      customerId: record.customerId,
      productId: record.productId,
      productName: record.productName || record.targetName || '',
      productCode: record.productCode || '',
      unitName: record.unitName || '',
      basePriceType: record.basePriceType,
      calcOperator: record.calcOperator || '+',
      calcValue: record.calcValue ?? 0,
      autoConvert: false,
    })
  } else {
    Object.assign(ruleForm, {
      id: undefined,
      gradeName: '',
      customerId: undefined,
      productId: undefined,
      productName: '',
      productCode: '',
      unitName: '',
      basePriceType: '零售价',
      calcOperator: '+',
      calcValue: 0,
      autoConvert: false,
    })
  }
  ruleModalVisible.value = true
}

function onProductPicked(products: any[]) {
  const first = products?.[0]
  if (!first) return
  ruleForm.productId = String(first.id)
  ruleForm.productName = first.productName || first.name || ''
  ruleForm.productCode = first.productCode || first.code || ''
  if (!ruleForm.unitName && first.unit) ruleForm.unitName = first.unit
}

async function submitRule() {
  if (activeTab.value === 'levelPrice' && !ruleForm.gradeName) {
    message.warning('请选择客户级别')
    return
  }
  if (activeTab.value === 'customerPrice' && !ruleForm.customerId) {
    message.warning('请选择客户')
    return
  }
  if (!ruleForm.productId) {
    message.warning('请选择商品')
    return
  }
  if (ruleForm.calcValue === null || ruleForm.calcValue === undefined) {
    message.warning('请填写计算数')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, any> = {
      id: ruleForm.id,
      productId: ruleForm.productId,
      productCode: ruleForm.productCode,
      productName: ruleForm.productName,
      unitName: ruleForm.unitName || undefined,
      basePriceType: ruleForm.basePriceType,
      calcOperator: ruleForm.calcOperator,
      calcValue: ruleForm.calcValue,
    }
    if (activeTab.value === 'levelPrice') {
      payload.gradeName = ruleForm.gradeName
      await productPriceApi.saveLevelPrice(payload)
    } else {
      payload.customerId = ruleForm.customerId
      await productPriceApi.saveCustomerPrice(payload)
    }
    message.success('保存成功')
    ruleModalVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDeleteRule(record: GradePriceRow) {
  Modal.confirm({
    title: '删除指定价',
    content: `确定删除「${record.targetName || record.productName}」的指定价吗？`,
    okType: 'danger',
    onOk: async () => {
      if (activeTab.value === 'levelPrice') {
        await productPriceApi.deleteLevelPrices([String(record.id)])
      } else {
        await productPriceApi.deleteCustomerPrices([String(record.id)])
      }
      message.success('删除成功')
      fetchList()
    },
  })
}

function handleBatchDelete() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选需要删除的数据')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedRows.value.length} 条指定价吗？`,
    okType: 'danger',
    onOk: async () => {
      const ids = selectedRows.value.map(r => String(r.id))
      if (activeTab.value === 'levelPrice') {
        await productPriceApi.deleteLevelPrices(ids)
      } else {
        await productPriceApi.deleteCustomerPrices(ids)
      }
      message.success('删除成功')
      selectedRows.value = []
      fetchList()
    },
  })
}

// ═══ 导入 ═══
const importVisible = ref(false)

function handleImportSuccess(result: any) {
  if (!result) return
  if (result.failure > 0) {
    message.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failure} 条`)
  } else {
    message.success(`导入成功 ${result.success} 条`)
  }
  fetchList()
}

// ═══ 快捷键 F8 打印 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(async () => {
  await Promise.all([loadGrades(), loadOptions()])
  fetchCategoryTree()
  fetchList()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.search-rows {
  display: flex;
  flex-direction: column;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row + .search-row {
  margin-top: 8px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 8px;
}
/* 资料模块统一橙色新增按钮 */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
.table-holder {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.text-muted {
  color: #bbb;
}
.batch-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.rule-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rule-eq {
  color: #666;
}
.rule-preview {
  font-size: 13px;
  color: #888;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
