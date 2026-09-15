<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商品上架（商城 → 基础业务 → 商品上架，菜单 80360 / `mall:product-shelf`）
        · 对标：ql361「商城 → 基础业务 → 商品上架」；本页为商城在售商品统一管理（单视图列表页 = 左侧分类树 + 右侧商品列表）
        · 骨架（金标准路线 A）：ErrorBoundary > PageContainer(full-height) > CategoryListLayout(show-category-panel) + BillDetailTable + StandardPagination
        · 无子 Tab（对标单视图），列配置齿轮在数据表表头序号列（个人/全局配置）
        · 列口径严格取自 docs/Yh-Spec/手动整理对标开发文档/交易模块/商品上架开发文档.md：
          - 固定列 = 基础 30 列：序号/勾选/上架/图片/商品名称/商品货号/条码/规格/型号/产地/品牌/单位/可用库存/
            零售价/批发价/预设进价 + 8 个客户类型价格列（餐饮店…特价客户）+ 排序/排序值/起订量/商品积分/备注/关键字
            （其中 14 列 defaultHidden：型号/产地/品牌/预设进价 + 8 个客户类型价格列 + 商品积分 + 关键字）
          - 8 个客户类型价格列为 √/× 勾选展示
          - **「商品标签」列为 √/× 勾选展示，且按本租户标签字典动态生成**（见下方「动态标签列」说明）；
            **列数随字典变化 —— 不存在固定的「19 列」或「20 列」口径**
        · 分类树（对标 14 分类）：全部商品 + 低温熟食/饮品原料/调理冻品/米面制品/烘焙西点/预制品/常温食品/
          生鲜冻品/包装耗材/吧台用品/餐具器皿/餐饮设备/其他；优先用后端 /erp/product-category/tree 真实分类，
          接口无数据时回落到对标 14 分类静态兜底
        · 功能按钮：批量上架、批量下架、设置商城默认排序方式、刷新、打印(F8)、导出
        · 查询条件下拉「文案 + 取值」已按对标 ql361 实测校准（2026-09-14）：
          上架状态 全部(-1)/已上架(1)/未上架(0)（默认「全部」）、
          使用优惠券 全部(-1)/允许(1)/不允许(2)（默认「全部」）、
          商品类型 普通商品(0)/套餐商品(1)（默认「普通商品」）。
          「-1=全部」一律**不传参**（后端空即不过滤）；对标值 → 后端参数字段的值映射见
          文件内 `SHELF_STATUS_TO_API` / `COUPON_USED_TO_API` / `PRODUCT_TYPE_TO_API`。
        · API：mallProductApi（/erp/mall/admin/product/page、POST /product、PUT /product/{id}、DELETE /product/{id}）
          + productApi：行内与批量上下架均走 `PUT /erp/product/batch-status`
            （body `{ids:[…], status:'ON_SHELF'|'OFF_SHELF'}`，后端归一后落
             `erp_product.mall_shelf_status` 1/0 —— 即列表视图 `v_mall_product.status` 的数据源列）
          + productApi：商城默认排序 `GET /erp/product/mall-sort` / `PUT /erp/product/mall-sort`（body `{field,direction}`，
            落配置中心 KV `sys_project_config.mall.product.default.sort`，租户级单值，商城端与管理端共用）
        · 后端字段现状：列表数据源为 v_mall_product 视图（ErpProductMall）。
          V11.361.5 追加 specification / unit_name；V11.361.6 追加 barcode / model_no / origin_place /
          brand / wholesale_price / preset_cost_price / sort / sort_value / min_order_quantity /
          product_points / remark / keyword / use_coupon / coupon_used / product_type / visible_status /
          product_tag / industry_category / grade_price_1..8 / customer_grade_codes —— 上列固定列全部由真实字段驱动
          （8 个客户类型价格列 ← grade_price_1..8 = MAX(erp_product_unit.grade_price_N)）。
        · 动态「商品标签」列（本页 2026 本轮改造）：
          「商品标签」列**不是**内置的「食品分类」枚举 —— 项目内 `V11.161.0__Standardize_Mall_Tag_Slots.sql`
          头部已写明：对标租户在「商品辅助资料 → 商品标签」自建了 20 条**自定义昵称**（早餐面点/酒席宴席/…），
          标准槽位 = `erp_mall_tag.tag_code`（`TAG_1..TAG_20`），`tag_name` 为用户可改的昵称，
          商品侧 `erp_product.mall_tags` 存**槽位编码**；本页视图列 `product_tag` 即该串。
          故本页列由 `GET /erp/mall-tag/list`（`mallTagApi.list()`）返回的**本租户**字典动态生成：
          `key = tagCode`、`title = tagName`（空则回落 `tagCode`），√/× 由 `product_tag` 是否含该 `tagCode` 精确匹配。
          字典加载失败或为空时**不渲染任何标签列**（绝不回落成对标昵称，见 `fetchMallTagDict`）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        :category-title="'商品分类'"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="categoryExpandedKeys"
        :current-path="currentCategoryPath"
        :show-table-footer="true"
        :category-editable="false"
        @category-select="onCategorySelect"
        @category-expand="onCategoryExpand"
        @category-retry="fetchCategoryTree"
      >
        <!-- ═══ 工具栏右侧：页面配置 / 批量上架 / 批量下架 / 默认排序 / 刷新 / 打印(F8) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              title="页面配置"
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
              v-if="isButtonEnabled('batchOnShelf')"
              size="small"
              :loading="batchShelfLoading"
              :disabled="checkedRecords.length === 0"
              @click="handleBatchShelf('ON_SHELF')"
            >
              <VerticalAlignTopOutlined /> 批量上架
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchOffShelf')"
              size="small"
              :loading="batchShelfLoading"
              :disabled="checkedRecords.length === 0"
              @click="handleBatchShelf('OFF_SHELF')"
            >
              <VerticalAlignBottomOutlined /> 批量下架
            </a-button>
            <a-button
              v-if="isButtonEnabled('defaultSort')"
              size="small"
              @click="openSortSetting"
            >
              <OrderedListOutlined /> 设置商城默认排序方式
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
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

        <!-- ═══ 查询区（对标固定项：筛选条件/品牌/商品/商品标签/上架状态/使用优惠券/商品类型/显示状态 + 显示层次结构/商品码） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="商品名称/货号/条码/规格/型号"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('brand')"
                class="search-item"
              >
                <span class="search-label">品牌</span>
                <a-input
                  v-model:value="searchForm.brand"
                  placeholder="品牌"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('productName')"
                class="search-item"
              >
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="searchForm.productName"
                  placeholder="商品名称"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('productTag')"
                class="search-item"
              >
                <span class="search-label">商品标签</span>
                <!-- 选项来源 GET /erp/product/mall-tags（erp_product.mall_tags 已打标槽位编码去重）；
                     显示名优先取本租户标签字典的 tag_name（昵称），字典未就绪/未命中时按槽位编码展示 -->
                <a-select
                  v-model:value="searchForm.productTag"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="tagOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('shelfStatus')"
                class="search-item"
              >
                <span class="search-label">上架状态</span>
                <!-- 对标实测取值 全部(-1) / 已上架(1) / 未上架(0)；-1=全部 → 不传参（见 loadTable 值映射） -->
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="SHELF_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('couponUsed')"
                class="search-item"
              >
                <span class="search-label">使用优惠券</span>
                <!-- 对标实测取值 全部(-1) / 允许(1) / 不允许(2) → 请求映射为后端 couponUsed：yes / no -->
                <a-select
                  v-model:value="searchForm.couponUsed"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="COUPON_USED_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('productType')"
                class="search-item"
              >
                <span class="search-label">商品类型</span>
                <!-- 对标实测取值 普通商品(0) / 套餐商品(1)（对标默认「普通商品」）
                     → 请求映射为 erp_product.product_type 真实枚举 SINGLE / KIT -->
                <a-select
                  v-model:value="searchForm.productType"
                  placeholder="普通商品"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PRODUCT_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('visibleStatus')"
                class="search-item"
              >
                <span class="search-label">显示状态</span>
                <!-- 后端参数 visibleStatus（视图 visible_status ← erp_product.status：ENABLED/DISABLED） -->
                <a-select
                  v-model:value="searchForm.visibleStatus"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="VISIBLE_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
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
            <div class="search-row second-row">
              <a-checkbox v-model:checked="searchForm.showHierarchy">
                显示层次结构
              </a-checkbox>
              <a-checkbox v-model:checked="searchForm.showProductCode">
                商品码
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头序号列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              ref="tableRef"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              storage-key="mall-product-shelf-table-columns"
              global-config-key="mall-product-shelf-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <!-- 上架：行内 √/× 开关（读侧判定 ON_SHELF；写侧请求 ON_SHELF / OFF_SHELF，见 handleToggleShelf） -->
              <template #shelfCell="{ record }">
                <a-switch
                  v-if="!record.__ghost"
                  :checked="isOnShelf(record)"
                  :loading="togglingId === record.id"
                  size="small"
                  checked-children="上架"
                  un-checked-children="下架"
                  @change="(checked: any) => handleToggleShelf(record, checked)"
                />
              </template>
              <template #imageCell="{ record }">
                <a-image
                  v-if="record.imageUrl"
                  :src="record.imageUrl"
                  :width="36"
                  :height="36"
                  style="object-fit: cover; border-radius: 4px"
                />
                <span v-else>-</span>
              </template>
              <template #productNameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.productName || '-' }}</a>
              </template>
              <template #moneyCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #qtyCell="{ record, column }">
                {{ fmtQty(record[column.key]) }}
              </template>
              <!-- √/× 勾选型列（客户类型价格 8 列 + 动态「商品标签」列）：对标以 √/× 展示 -->
              <!-- 商品标签列的 record[column.key]：key = tagCode，值由 product_tag 是否含该槽位编码派生 -->
              <template #flagCell="{ record, column }">
                <span :class="record[column.key] ? 'flag-on' : 'flag-off'">
                  {{ record[column.key] ? '✓' : '×' }}
                </span>
              </template>
              <template #textCell="{ record, column }">
                <span>{{ fmtText(record[column.key]) }}</span>
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="mall-product-shelf-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新增/编辑商品弹窗（保留既有实现） ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑商城商品' : '新增商城商品'"
        :confirm-loading="saving"
        width="640px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="商品编码"
            name="productId"
          >
            <a-input
              v-model:value="form.productId"
              placeholder="关联ERP商品编码"
              :disabled="!!editingId"
            />
          </a-form-item>
          <a-form-item
            label="商品名称"
            name="productName"
          >
            <a-input
              v-model:value="form.productName"
              placeholder="请输入商品名称"
            />
          </a-form-item>
          <a-form-item
            label="商品图片"
            name="imageUrl"
          >
            <a-input
              v-model:value="form.imageUrl"
              placeholder="图片URL"
            />
          </a-form-item>
          <a-form-item
            label="销售价"
            name="salePrice"
          >
            <a-input-number
              v-model:value="form.salePrice"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="商城销售价"
            />
          </a-form-item>
          <a-form-item
            label="市场价"
            name="marketPrice"
          >
            <a-input-number
              v-model:value="form.marketPrice"
              :min="0"
              :precision="2"
              style="width: 100%"
              placeholder="划线市场价"
            />
          </a-form-item>
          <a-form-item
            label="分类名称"
            name="categoryName"
          >
            <a-input
              v-model:value="form.categoryName"
              placeholder="商城展示分类"
            />
          </a-form-item>
          <a-form-item
            label="可售库存"
            name="stockQuantity"
          >
            <a-input-number
              v-model:value="form.stockQuantity"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            label="上架状态"
            name="status"
          >
            <a-radio-group v-model:value="form.status">
              <a-radio value="ON_SHELF">
                上架
              </a-radio>
              <a-radio value="OFF_SHELF">
                下架
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item
            label="商品描述"
            name="description"
          >
            <a-textarea
              v-model:value="form.description"
              :rows="2"
              placeholder="商城详情描述"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 设置商城默认排序方式弹窗 ═══ -->
      <a-modal
        v-model:open="sortSettingVisible"
        title="设置商城默认排序方式"
        :confirm-loading="sortSettingSaving"
        @ok="handleSortSettingSave"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 15 }"
        >
          <a-form-item label="默认排序方式">
            <a-select
              v-model:value="defaultSort"
              placeholder="请选择默认排序方式"
              :options="DEFAULT_SORT_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="排序方向">
            <a-radio-group v-model:value="defaultSortDirection">
              <a-radio value="asc">
                升序
              </a-radio>
              <a-radio value="desc">
                降序
              </a-radio>
            </a-radio-group>
          </a-form-item>
          <a-alert
            type="info"
            show-icon
            message="保存后落配置中心（sys_project_config.mall.product.default.sort），商城端与管理端共用。"
          />
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { Rule } from 'ant-design-vue/es/form'
import {
  ReloadOutlined, DownloadOutlined, PrinterOutlined, SettingOutlined,
  VerticalAlignTopOutlined, VerticalAlignBottomOutlined, OrderedListOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { mallProductApi, type MallProduct } from '@/api/erp/mall'
import { productCategoryApi, productApi, mallTagApi, type MallTag } from '@/api/erp/product'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'MallProductShelf' })

/**
 * 页面行数据 = 后端 v_mall_product 视图字段（ErpProductMall，V11.361.5/V11.361.6 已扩列）
 *             + 由真实字段派生的 √/× 标记（客户类型价格 8 列、动态商品标签列）。
 * 视图未提供的列仍渲染 '-'（不做假数据），派生口径见 deriveRowFlags()。
 */
interface ShelfRow {
  /**
   * 动态「商品标签」列标记：键 = 标签槽位编码（`erp_mall_tag.tag_code`，如 `TAG_1`），值 = √/×。
   * 列数随本租户标签字典变化，故**不声明任何固定标签字段**（早期曾硬编码 19 个昵称字段，已删除）。
   */
  [tagCode: string]: any
  id?: number
  rowKey: string
  /** BillDetailTable 空数据占位行标记（由组件生成，不参与行内操作渲染） */
  __ghost?: boolean
  productId?: string
  productCode?: string
  productName?: string
  imageUrl?: string
  salePrice?: number
  marketPrice?: number
  stockQuantity?: number
  categoryId?: string
  categoryName?: string
  /** 上架状态（读侧视图值 `ON_SHELF` / `INACTIVE` ← `erp_product.mall_shelf_status` 1/0；写侧口径见 handleToggleShelf） */
  status?: string
  description?: string
  salesCount?: number
  // ── V11.361.5 视图列 ──
  specification?: string            // 规格（视图 specification ← erp_product.spec）
  unitName?: string                 // 单位（视图 unit_name ← erp_product.unit）
  // ── V11.361.6 视图列（对标列，已闭环） ──
  barcode?: string                  // 条码 ← erp_product.barcode
  modelNo?: string                  // 型号 ← erp_product.model
  originPlace?: string              // 产地 ← erp_product.origin
  brand?: string                    // 品牌 ← erp_product.brand
  wholesalePrice?: number           // 批发价 ← erp_product.wholesale_price
  presetCostPrice?: number          // 预设进价 ← COALESCE(erp_product.purchase_price, cost_price, 0)
  sort?: string                     // 排序（排序方式）← erp_product.mall_sort_type
  sortValue?: number                // 排序值 ← erp_product.mall_sort_order
  minOrderQuantity?: number         // 起订量 ← erp_product.mall_min_order_qty
  productPoints?: number            // 商品积分 ← erp_product.mall_points
  remark?: string                   // 备注 ← erp_product.remark
  keyword?: string                  // 关键字 ← erp_product.keywords
  couponUsed?: boolean              // 使用优惠券 ← erp_product.use_coupon = 1
  productType?: string              // 商品类型 ← erp_product.product_type
  visibleStatus?: string            // 显示状态 ← erp_product.status（ENABLED/DISABLED）
  productTag?: string               // 商品标签 ← erp_product.mall_tags（逗号分隔槽位编码）
  industryCategory?: string         // 所属行业类别 ← erp_product.industry_category
  /**
   * 客户类型价格 8 列的判定依据（后端视图 grade_price_1..8）：
   * 口径 = MAX(erp_product_unit.grade_price_N)（该商品多单位取最大值，
   * V11.139.0 明确 erp_product_unit.grade_price_N 为单位标准等级价，商品表单维护）。
   * 列序与 V9.23.0 旧列名映射一致，与对标 8 列一一对应。
   */
  gradePrice1?: number              // 价格等级1 → 餐饮店
  gradePrice2?: number              // 价格等级2 → 食堂团餐
  gradePrice3?: number              // 价格等级3 → 外围餐饮店
  gradePrice4?: number              // 价格等级4 → 自助vip
  gradePrice5?: number              // 价格等级5 → 大团餐
  gradePrice6?: number              // 价格等级6 → 重点|vip01
  gradePrice7?: number              // 价格等级7 → 连锁|vip
  gradePrice8?: number              // 价格等级8 → 特价客户
  /** 级别指定价客户等级昵称集合（补充佐证列，未参与 √/× 判定） */
  customerGradeCodes?: string
  /** 客户类型价格 8 列（√/× 展示，由 gradePrice1..8 派生：>0 视为已配价） */
  restaurant?: boolean              // 餐饮店
  canteen?: boolean                 // 食堂团餐
  outRestaurant?: boolean           // 外围餐饮店
  vipSelf?: boolean                 // 自助vip
  largeGroup?: boolean              // 大团餐
  vipLevel1?: boolean               // 重点|vip01
  vipLevel2?: boolean               // 连锁|vip
  specialCustomer?: boolean         // 特价客户
  // ── 动态「商品标签」列：不在此声明固定字段（键 = tagCode，见上方索引签名），列集合来自本租户标签字典 ──
}

// ═══ 字典（下拉「文案 + 取值」= 对标 ql361 实测口径；请求参数在 loadTable 内做值映射） ═══
/**
 * 上架状态 —— ✅ 已按对标实测校准：`全部`(-1) / `已上架`(1) / `未上架`(0)。
 * （对标实测 2026-09-14；下架态对标文案是「未上架」，不是「已下架」）
 *
 * 取值与后端真实列同值域：`erp_product.mall_shelf_status`（V8.1.0：0=下架 1=上架）→
 * `已上架`(1) / `未上架`(0) 可直接对表；`-1`（全部）= 不传参（后端空即不过滤）。
 * ⚠️ 本页列表走 `/erp/mall/admin/product/page`，该端点 `status` 参数吃的是**视图**
 *    `v_mall_product.status` 的两值串（`ON_SHELF` / `INACTIVE`），故请求前按
 *    `SHELF_STATUS_TO_API` 映射（`-1` 不传参）。
 */
const SHELF_STATUS_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 1, label: '已上架' },
  { value: 0, label: '未上架' }
]
/** 上架状态：对标实测值 → `/erp/mall/admin/product/page` 的 status 串（读侧视图值为 ON_SHELF / INACTIVE） */
const SHELF_STATUS_TO_API: Record<number, 'ON_SHELF' | 'OFF_SHELF'> = {
  1: 'ON_SHELF',
  0: 'OFF_SHELF' // 后端 normalizeShelfStatus 归一为视图值 INACTIVE
}
/**
 * 使用优惠券 —— ✅ 已按对标实测校准：`全部`(-1) / `允许`(1) / `不允许`(2)。
 * 后端 `couponUsed` 走 `parseYesNo`（`yes`/`1`/`是` = 是；`no`/`0`/`否` = 否；无法识别 = 不过滤），
 * 故 `允许`(1) / `不允许`(2) 请求前映射为 `yes` / `no`；`-1`（全部）不传参。
 */
const COUPON_USED_OPTIONS = [
  { value: -1, label: '全部' },
  { value: 1, label: '允许' },
  { value: 2, label: '不允许' }
]
/** 使用优惠券：对标实测值 → 后端 couponUsed 串 */
const COUPON_USED_TO_API: Record<number, 'yes' | 'no'> = {
  1: 'yes',
  2: 'no'
}
/**
 * 商品类型 —— ✅ 已按对标实测校准：`普通商品`(0) / `套餐商品`(1)（对标默认显示「普通商品」）。
 *
 * 值映射依据：我方 `erp_product.product_type` 真实枚举是 V1.5.0 落的
 * `SINGLE`单品 / `KIT`套件 / `SERVICE`服务（后端为字符串等值匹配），
 * 故请求前按 `PRODUCT_TYPE_TO_API` 映射为真实枚举值。
 * ⚠️ 仍保留的缺口：对标下拉只有 2 项，我方值域多一个 `SERVICE`（服务商品），
 *    无对标对应项 → 选「普通商品」只筛 `SINGLE`，见开发文档「未闭环」。
 */
const PRODUCT_TYPE_OPTIONS = [
  { value: 0, label: '普通商品' },
  { value: 1, label: '套餐商品' }
]
/** 商品类型：对标实测值 → erp_product.product_type 真实枚举 */
const PRODUCT_TYPE_TO_API: Record<number, 'SINGLE' | 'KIT'> = {
  0: 'SINGLE',
  1: 'KIT'
}
/** 对标实测「商品类型」默认值 = 普通商品(0) */
const DEFAULT_PRODUCT_TYPE = 0
/** 显示状态——取值口径为 erp_product.status 真实枚举（ENABLED/DISABLED，见 V1.1.9 建表默认值） */
const VISIBLE_STATUS_OPTIONS = [
  { value: 'ENABLED', label: '启用' },
  { value: 'DISABLED', label: '停用' }
]
// ═══ 商品标签字典（动态「商品标签」列与筛选选项昵称的唯一来源） ═══
/** 槽位号：`TAG_10` → 10；非法编码回落 MAX（排序置后）。避免字符串排序把 TAG_10 排在 TAG_2 前 */
function mallTagSlotNo(tagCode?: string): number {
  const m = /^TAG_(\d+)$/.exec(String(tagCode ?? '').trim().toUpperCase())
  return m ? Number(m[1]) : Number.MAX_SAFE_INTEGER
}

/**
 * 本租户「商品标签」字典 —— 来源 `GET /erp/mall-tag/list`（`mallTagApi.list()`，表 `erp_mall_tag`）。
 * 后端 `selectByTenantId` 已过滤 `deleted = 0`；本页再按 **status = 1（启用）** 过滤。
 * 事实口径：`tag_code` = 标准槽位编码 `TAG_1..TAG_20`（商品侧 `erp_product.mall_tags` 存的就是它）；
 *          `tag_name` = **租户自定义昵称**（默认「标签N」），即列标题。
 */
const mallTagDict = ref<MallTag[]>([])

/** 已启用标签，按 `sort_order` 升序、同序按槽位号升序 —— 列序与 √/× 派生都按它走 */
const enabledMallTags = computed(() => {
  const list = (Array.isArray(mallTagDict.value) ? mallTagDict.value : [])
    .filter(t => Number(t?.status ?? 1) === 1)
    .filter(t => String(t?.tagCode ?? '').trim() !== '')
  return list.slice().sort((a, b) => {
    const bySort = Number(a.sortOrder ?? Number.MAX_SAFE_INTEGER) - Number(b.sortOrder ?? Number.MAX_SAFE_INTEGER)
    if (bySort !== 0) return bySort
    return mallTagSlotNo(a.tagCode) - mallTagSlotNo(b.tagCode)
  })
})

/** 槽位编码 → 租户昵称；昵称为空时回落编码本身（不造假名） */
function mallTagName(tagCode: string): string {
  const hit = mallTagDict.value.find(t => String(t?.tagCode ?? '') === tagCode)
  const name = String(hit?.tagName ?? '').trim()
  return name !== '' ? name : tagCode
}

/** 字典异常提示只给一次（非阻塞；不做硬编码回落） */
let mallTagDictWarned = false

/**
 * 加载本租户标签字典（在 onMounted 中与列表**并行**发起，不阻塞首屏）。
 * 失败或为空 → 字典保持空 → **不渲染任何标签列**，并给一次非阻塞提示；
 * **绝不回落成对标租户的餐饮分类昵称**（那是别人的数据，不是我们的 schema）。
 */
async function fetchMallTagDict() {
  try {
    const data: any = await mallTagApi.list()
    const list: MallTag[] = Array.isArray(data) ? data : (data?.data ?? [])
    mallTagDict.value = Array.isArray(list) ? list : []
    if (mallTagDict.value.length === 0 && !mallTagDictWarned) {
      mallTagDictWarned = true
      message.warning('本租户「商品标签」字典为空，已隐藏商品标签列')
    }
  } catch (error) {
    console.warn('[商品上架] 商品标签字典加载失败', error)
    mallTagDict.value = []
    if (!mallTagDictWarned) {
      mallTagDictWarned = true
      message.warning('「商品标签」字典加载失败，已隐藏商品标签列（不回落硬编码昵称）')
    }
  } finally {
    // 字典与列表是两条并行异步链路：字典后到时，已渲染的行上没有 tagCode 标记字段，标签列会整列 ×
    // → 字典就绪后按当前行重算一次 √/×（复用行对象，不重新请求列表）
    tableData.value = tableData.value.map((row, i) => toShelfRow(row, i))
  }
}

/**
 * 商品标签筛选选项（对标「商品标签（全部）」）：来源 GET /erp/product/mall-tags
 * （erp_product.mall_tags 已打标的槽位编码去重，如 TAG_1）。
 * 显示名优先取本租户标签字典的 `tag_name`（昵称），字典未就绪/未命中时回落槽位编码（不造假名）。
 */
const tagCodesUsed = ref<string[]>([])
const tagOptions = computed(() =>
  tagCodesUsed.value.map(code => ({ value: code, label: mallTagName(code) }))
)

async function fetchTagOptions() {
  try {
    const data: any = await productApi.getMallTags()
    const list: string[] = Array.isArray(data) ? data : (data?.data ?? [])
    tagCodesUsed.value = (list || [])
      .filter((v: any) => v !== null && v !== undefined && String(v).trim() !== '')
      .map((v: any) => String(v))
  } catch (error) {
    // 无已打标数据或接口失败时保持空选项（不做假选项）
    console.warn('[商品上架] 商品标签选项加载失败', error)
    tagCodesUsed.value = []
  }
}
/** 商城默认排序方式（对标「设置商城默认排序方式」） */
const DEFAULT_SORT_OPTIONS = [
  { value: 'sort', label: '按排序值' },
  { value: 'createTime', label: '按创建时间' },
  { value: 'salesCount', label: '按销量' },
  { value: 'salePrice', label: '按销售价' }
]

// ═══ 分类树（对标 14 分类；优先后端真实分类，无数据时静态兜底） ═══
const ROOT_CATEGORY_ID = '0'
/** 对标实测左侧分类树（全部商品 + 13 个业务分类 = 14 项） */
const FALLBACK_CATEGORY_NAMES = [
  '低温熟食', '饮品原料', '调理冻品', '米面制品', '烘焙西点', '预制品', '常温食品',
  '生鲜冻品', '包装耗材', '吧台用品', '餐具器皿', '餐饮设备', '其他'
]
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryTreeData = ref<any[]>([])
const categoryFlat = ref<any[]>([])
const selectedCategoryId = ref<string>(ROOT_CATEGORY_ID)
const categoryExpandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const currentCategoryPath = computed(() => {
  if (!selectedCategoryId.value || selectedCategoryId.value === ROOT_CATEGORY_ID) return '全部商品'
  const node = categoryFlat.value.find(c => String(c.id) === String(selectedCategoryId.value))
  return node ? (node.categoryName || '全部商品') : '全部商品'
})

function normalizeTree(nodes: any[]): any[] {
  return (nodes || []).map(n => ({
    ...n,
    id: String(n.id),
    children: n.children?.length ? normalizeTree(n.children) : undefined
  }))
}

async function fetchCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const data: any = await productCategoryApi.getTree()
    const realTree = normalizeTree(Array.isArray(data) ? data : data?.records || [])
    const tree = realTree.length
      ? realTree
      : FALLBACK_CATEGORY_NAMES.map((name, i) => ({ id: `fallback-${i + 1}`, categoryName: name }))
    categoryTreeData.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: tree }]
    const flat: any[] = []
    const walk = (nodes: any[]) => {
      nodes.forEach(n => {
        flat.push(n)
        if (n.children?.length) walk(n.children)
      })
    }
    walk(categoryTreeData.value)
    categoryFlat.value = flat
    categoryExpandedKeys.value = [ROOT_CATEGORY_ID, ...tree.map(n => n.id)]
  } catch (error) {
    console.warn('[商品上架] 分类树加载失败，回落对标静态分类', error)
    categoryError.value = false
    const tree = FALLBACK_CATEGORY_NAMES.map((name, i) => ({ id: `fallback-${i + 1}`, categoryName: name }))
    categoryTreeData.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: tree }]
    categoryFlat.value = categoryTreeData.value
  } finally {
    categoryLoading.value = false
  }
}

function onCategorySelect(keys: (string | number)[]) {
  selectedCategoryId.value = keys.length ? String(keys[0]) : ROOT_CATEGORY_ID
  pagination.current = 1
  loadTable()
}
function onCategoryExpand(keys: (string | number)[]) {
  categoryExpandedKeys.value = keys
}

// ═══ 查询条件（初值 = 对标实测默认态） ═══
const searchForm = reactive<Record<string, any>>({
  /**
   * 对标实测默认态：上架状态「全部」(-1)、使用优惠券「全部」(-1)、商品类型「普通商品」(0)。
   * -1 一律**不传参**（后端空即不过滤），见 loadTable 的值映射。
   */
  status: -1,
  couponUsed: -1,
  productType: DEFAULT_PRODUCT_TYPE,
  showHierarchy: false,
  showProductCode: false
})

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
/** 批量上下架请求中（端点：PUT /erp/product/batch-status） */
const batchShelfLoading = ref(false)
const tableData = ref<ShelfRow[]>([])
const checkedRecords = ref<ShelfRow[]>([])
const togglingId = ref<number | undefined>()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const tableRef = ref<InstanceType<typeof BillDetailTable> | null>(null)

function handleCheckboxChange() {
  checkedRecords.value = tableRef.value?.getCheckedRecords?.() || []
}
function handleCheckboxAll(_checked: boolean, records: ShelfRow[]) {
  checkedRecords.value = records || []
}

// ═══ 列定义（固定列取自商品上架开发文档「全部配置数据列」+ 动态「商品标签」列） ═══
/**
 * 动态「商品标签」列：由本租户标签字典（`enabledMallTags`）生成，**列数随字典变化**。
 *   `key = tagCode`（如 `TAG_1`）、`title = tagName`（租户昵称，空则回落 tagCode）；
 *   列序 = sort_order 升序 → 槽位号升序（与字典页一致）。
 *
 * ⚠️ 列配置持久化（`storage-key="mall-product-shelf-table-columns"`，BillDetailTable 既有行为）：
 *   列设置以**当前列定义为准**重建（`loadStoredSettings`），因此：
 *     · 旧配置里存的硬编码列 key（breakfast/banquet/…）在新列定义中已不存在 → 自然丢弃；
 *     · 用户对列宽/显隐/显示名的自定义按 **key = tagCode** 匹配 —— 同一槽位改昵称后 key 不变，
 *       自定义设置保留；跨槽位（TAG_1 → TAG_2）不迁移；
 *     · 用户手工改过显示名（displayName ≠ title）时优先保留用户设置，不会跟随新昵称刷新
 *       （BillDetailTable `isRenamed` 既有语义，本页不改）。
 */
const mallTagColumns = computed<DetailColumnConfig[]>(() =>
  enabledMallTags.value.map(tag => {
    const tagCode = String(tag.tagCode)
    const tagName = String(tag.tagName ?? '').trim()
    return {
      key: tagCode,
      title: tagName !== '' ? tagName : tagCode,
      type: 'slot' as const,
      slotName: 'flagCell',
      width: 90,
      align: 'center' as const
    }
  })
)

const columns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  // 勾选列（key 必须为 checkbox —— BillDetailTable LOCKED_COLUMNS 锁定列口径；对标固定列「多选勾选框」）
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'shelf', title: '上架', type: 'slot', slotName: 'shelfCell', width: 90, align: 'center', fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageCell', width: 70 },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'productNameCell', width: 200 },
  { key: 'productCode', title: '商品货号', type: 'slot', slotName: 'textCell', width: 130 },
  { key: 'barcode', title: '条码', type: 'slot', slotName: 'textCell', width: 130 },
  { key: 'specification', title: '规格', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'modelNo', title: '型号', type: 'slot', slotName: 'textCell', width: 110, defaultHidden: true },
  { key: 'originPlace', title: '产地', type: 'slot', slotName: 'textCell', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'slot', slotName: 'textCell', width: 100, defaultHidden: true },
  { key: 'unitName', title: '单位', type: 'slot', slotName: 'textCell', width: 70 },
  { key: 'stockQuantity', title: '可用库存', type: 'slot', slotName: 'qtyCell', width: 100, align: 'right' },
  { key: 'salePrice', title: '零售价', type: 'slot', slotName: 'moneyCell', width: 100, align: 'right' },
  { key: 'wholesalePrice', title: '批发价', type: 'slot', slotName: 'moneyCell', width: 100, align: 'right' },
  { key: 'presetCostPrice', title: '预设进价', type: 'slot', slotName: 'moneyCell', width: 100, align: 'right', defaultHidden: true },
  // ── 15-22：客户类型价格列（8 列，√/× 展示；默认全部隐藏） ──
  { key: 'restaurant', title: '餐饮店', type: 'slot', slotName: 'flagCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'canteen', title: '食堂团餐', type: 'slot', slotName: 'flagCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'outRestaurant', title: '外围餐饮店', type: 'slot', slotName: 'flagCell', width: 100, align: 'center', defaultHidden: true },
  { key: 'vipSelf', title: '自助vip', type: 'slot', slotName: 'flagCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'largeGroup', title: '大团餐', type: 'slot', slotName: 'flagCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'vipLevel1', title: '重点|vip01', type: 'slot', slotName: 'flagCell', width: 100, align: 'center', defaultHidden: true },
  { key: 'vipLevel2', title: '连锁|vip', type: 'slot', slotName: 'flagCell', width: 100, align: 'center', defaultHidden: true },
  { key: 'specialCustomer', title: '特价客户', type: 'slot', slotName: 'flagCell', width: 90, align: 'center', defaultHidden: true },
  { key: 'sort', title: '排序', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'sortValue', title: '排序值', type: 'slot', slotName: 'textCell', width: 90, align: 'right' },
  { key: 'minOrderQuantity', title: '起订量', type: 'slot', slotName: 'qtyCell', width: 90, align: 'right' },
  { key: 'productPoints', title: '商品积分', type: 'slot', slotName: 'textCell', width: 90, align: 'right', defaultHidden: true },
  { key: 'remark', title: '备注', type: 'slot', slotName: 'textCell', width: 150 },
  // ── 商品标签列（动态，√/× 展示）：紧邻「关键字」之前，key = tagCode、title = 租户昵称 ──
  //    ⚠️ 这里**不得**出现任何硬编码昵称：早期实现把对标租户的 19 个餐饮分类昵称写死成固定 key + 固定 title，
  //    等于把别人的数据当成我们的静态 schema；现改为完全由 enabledMallTags 生成（字典为空 → 0 列）。
  ...mallTagColumns.value,
  { key: 'keyword', title: '关键字', type: 'slot', slotName: 'textCell', width: 140, defaultHidden: true }
])

// ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

/** 对标查询条件为固定项（无「页面配置」弹窗），此处按对标 8 项固定查询条件落页面配置基准 */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件（名称/货号/条码/规格/型号）', visible: true },
  { key: 'brand', label: '品牌', visible: true },
  { key: 'productName', label: '商品', visible: true },
  { key: 'productTag', label: '商品标签', visible: true },
  { key: 'shelfStatus', label: '上架状态', visible: true },
  { key: 'couponUsed', label: '使用优惠券', visible: true },
  { key: 'productType', label: '商品类型', visible: true },
  { key: 'visibleStatus', label: '显示状态', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'batchOnShelf', label: '批量上架', enabled: true },
  { key: 'batchOffShelf', label: '批量下架', enabled: true },
  { key: 'defaultSort', label: '设置商城默认排序方式', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
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

// ═══ 格式化 ═══
function fmtMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtQty(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}
function fmtText(val: any): string {
  if (val === null || val === undefined || val === '') return '-'
  return String(val)
}

// ═══ 数据加载 ═══
function extract<T>(res: any, fallback: T): T {
  const payload = res?.data?.data ?? res?.data ?? res
  return (payload === undefined || payload === null ? fallback : payload) as T
}

/** 逗号分隔集合串 → 去空白、去空的数组 */
function splitCodes(raw: any): string[] {
  if (raw === null || raw === undefined || raw === '') return []
  return String(raw).split(',').map(s => s.trim()).filter(s => s !== '')
}

/**
 * 客户类型价格 8 列判定：对标列 15-22 直接对应后端视图 grade_price_1..grade_price_8。
 *
 * 口径（真实数据源，非昵称匹配）：
 *   视图 grade_price_N = MAX(erp_product_unit.grade_price_N)（该商品多单位取最大值）。
 *   V11.139.0 明确 erp_product_unit.grade_price_N = 单位标准等级价（商品表单维护）；
 *   V9.23.0 的旧列名映射给出列序：1 餐饮店 / 2 食堂团餐 / 3 外围餐饮店 / 4 自助vip /
 *   5 大团餐 / 6 重点|vip01 / 7 连锁|vip / 8 特价客户 —— 与对标 8 列列序一致。
 *   > 0 视为「该等级已配价」→ 渲染 √；NULL/0 → ×。
 */
const CUSTOMER_TYPE_GRADE_PRICE_KEYS: Record<string, keyof ShelfRow> = {
  restaurant: 'gradePrice1',
  canteen: 'gradePrice2',
  outRestaurant: 'gradePrice3',
  vipSelf: 'gradePrice4',
  largeGroup: 'gradePrice5',
  vipLevel1: 'gradePrice6',
  vipLevel2: 'gradePrice7',
  specialCustomer: 'gradePrice8'
}

/**
 * 由真实后端字段派生 √/× 标记：
 *  · 客户类型价格 8 列 ← gradePrice1..8（视图 MAX(erp_product_unit.grade_price_N)），>0 为 √
 *  · 商品标签列       ← productTag（= `erp_product.mall_tags`，逗号分隔的**槽位编码**串，如 `TAG_1,TAG_5`）
 *                       是否**包含该列的 tagCode**（精确匹配，非昵称匹配）：含 → √，不含/为空 → ×
 * 值缺失时渲染 ×（与对标 √/× 语义一致）。
 */
function deriveRowFlags(row: Record<string, any>): Record<string, boolean> {
  const tags = new Set(splitCodes(row.productTag))
  const flags: Record<string, boolean> = {}
  Object.keys(CUSTOMER_TYPE_GRADE_PRICE_KEYS).forEach(key => {
    const price = row[CUSTOMER_TYPE_GRADE_PRICE_KEYS[key] as string]
    flags[key] = price !== null && price !== undefined && price !== '' && Number(price) > 0
  })
  // 动态标签列：key 与单元格取值的键**同为 tagCode**，列集合完全来自本租户字典（字典为空则无标签列）
  enabledMallTags.value.forEach(tag => {
    const tagCode = String(tag.tagCode)
    flags[tagCode] = tags.has(tagCode)
  })
  return flags
}

/** 分页结果行 → ShelfRow（视图字段 + 派生标记 + 行键） */
function toShelfRow(r: any, i: number): ShelfRow {
  return {
    ...r,
    ...deriveRowFlags(r),
    rowKey: String(r.id ?? `row-${i}`)
  }
}

async function loadTable() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    // 对标 8 项查询条件（空值 / 「全部」(-1) 不传参 —— 后端为空则不过滤）
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.brand) params.brand = searchForm.brand
    if (searchForm.productName) params.productName = searchForm.productName
    if (searchForm.productTag) params.productTag = searchForm.productTag
    // 上架状态：对标值 -1/1/0 → 后端 status 串 ON_SHELF / OFF_SHELF（-1=全部 不传参）
    const shelfStatusApi = SHELF_STATUS_TO_API[Number(searchForm.status)]
    if (shelfStatusApi) params.status = shelfStatusApi
    // 使用优惠券：对标值 -1/1/2 → 后端 couponUsed：yes / no（-1=全部 不传参）
    const couponUsedApi = COUPON_USED_TO_API[Number(searchForm.couponUsed)]
    if (couponUsedApi) params.couponUsed = couponUsedApi
    // 商品类型：对标值 0/1 → erp_product.product_type 真实枚举 SINGLE / KIT
    const productTypeApi = PRODUCT_TYPE_TO_API[Number(searchForm.productType)]
    if (productTypeApi) params.productType = productTypeApi
    if (searchForm.visibleStatus) params.visibleStatus = searchForm.visibleStatus
    if (selectedCategoryId.value && selectedCategoryId.value !== ROOT_CATEGORY_ID
      && !String(selectedCategoryId.value).startsWith('fallback-')) {
      params.categoryId = selectedCategoryId.value
    }
    const res: any = await mallProductApi.page(params)
    const payload = extract<{ records?: any[]; total?: number }>(res, {})
    tableData.value = (payload.records || []).map((r: any, i: number) => toShelfRow(r, i))
    pagination.total = Number(payload.total) || 0
    checkedRecords.value = []
  } catch (error: any) {
    tableData.value = []
    pagination.total = 0
    console.error('[商品上架] 列表加载失败', error)
    message.error(error?.response?.data?.message || '列表加载失败')
  } finally {
    loading.value = false
  }
}

// ═══ 行内上下架 / 批量上下架 / 新增编辑删除 ═══
/**
 * 上架态判定（读侧）：视图 `v_mall_product.status` **只有** `ON_SHELF`（上架）与 `INACTIVE`（下架）两值
 * （V9.0.0 / V11.361.5：`CASE WHEN p.mall_shelf_status = 1 THEN 'ON_SHELF' ELSE 'INACTIVE' END`）。
 * 故只认 `ON_SHELF`；`INACTIVE` / 空值一律视为未上架。
 */
function isOnShelf(record: ShelfRow): boolean {
  return String(record?.status ?? '').trim().toUpperCase() === 'ON_SHELF'
}

/**
 * 行内上架 / 下架
 *
 * ✅ 写入端点与工具栏「批量上架 / 批量下架」**同口径**：`PUT /erp/product/batch-status`
 *    （body `{ids, status}`，后端 `shelfStatusOf()` 把 `ON_SHELF`/`OFF_SHELF` 归一后写
 *     `erp_product.mall_shelf_status` 1/0 —— 即列表视图的数据源列）。
 * ⚠️ 写侧**不能**传视图值 `INACTIVE`：后端 `shelfStatusOf()` 只识别 `ON_SHELF`/`OFF_SHELF`，
 *    认不出会退化成写 `erp_product.status = 'INACTIVE'`（污染 ENABLED/DISABLED 列）。
 *    故「读侧 INACTIVE ↔ 写侧 OFF_SHELF」是**双向映射**，两侧取值不同名但指向同一列。
 * ⚠️ 也不再走 `mallProductApi.update`（`PUT /erp/mall/admin/product/{id}`）：该端点落**旧表**
 *    `mall_product`，而本页列表读 `v_mall_product`（数据源 `erp_product`），写旧表不会反映到列表。
 */
async function handleToggleShelf(record: ShelfRow, checked: boolean) {
  if (!record.id) return
  togglingId.value = record.id
  const target = checked ? 'ON_SHELF' : 'OFF_SHELF'
  try {
    await productApi.batchUpdateStatus([Number(record.id)], target)
    message.success(checked ? `「${record.productName}」已上架` : `「${record.productName}」已下架`)
    loadTable()
  } catch (error) {
    console.warn('[商品上架] 上下架失败', error)
    message.error('上下架失败')
  } finally {
    togglingId.value = undefined
  }
}

/**
 * 批量上架 / 批量下架
 * 真实端点：`PUT /erp/product/batch-status`，body `{ids:[…], status:'ON_SHELF'|'OFF_SHELF'}`
 * （与既有 `/erp/product/batch-shelf` 同口径，后端落 erp_product.mall_shelf_status）
 */
async function handleBatchShelf(target: 'ON_SHELF' | 'OFF_SHELF') {
  const ids = checkedRecords.value
    .filter(r => !r.__ghost && r.id != null)
    .map(r => Number(r.id))
    .filter(id => !Number.isNaN(id))
  if (!ids.length) {
    message.warning('请先勾选商品')
    return
  }
  batchShelfLoading.value = true
  try {
    await productApi.batchUpdateStatus(ids, target)
    message.success(`已${target === 'ON_SHELF' ? '上架' : '下架'} ${ids.length} 个商品`)
    loadTable()
  } catch (error: any) {
    console.error('[商品上架] 批量上下架失败', error)
    message.error(error?.response?.data?.message || error?.message || '批量上下架失败')
    loadTable()
  } finally {
    batchShelfLoading.value = false
  }
}

// ═══ 设置商城默认排序方式（配置中心 KV：sys_project_config.mall.product.default.sort） ═══
const sortSettingVisible = ref(false)
const sortSettingSaving = ref(false)
const defaultSort = ref<'sort' | 'createTime' | 'salesCount' | 'salePrice'>('sort')
const defaultSortDirection = ref<'asc' | 'desc'>('asc')

const SORT_FIELDS = ['sort', 'createTime', 'salesCount', 'salePrice'] as const

/** 打开弹窗时读取服务端配置（响应拦截器对裸 Map 不解包，此处兼容 wrapper/裸两种形态） */
async function openSortSetting() {
  sortSettingVisible.value = true
  try {
    const res: any = await productApi.getMallSort()
    const cfg = res?.data ?? res ?? {}
    if (cfg.field && (SORT_FIELDS as readonly string[]).includes(String(cfg.field))) {
      defaultSort.value = cfg.field
    }
    if (cfg.direction === 'asc' || cfg.direction === 'desc') {
      defaultSortDirection.value = cfg.direction
    }
  } catch (error: any) {
    console.error('[商品上架] 读取商城默认排序配置失败', error)
    message.error(error?.response?.data?.message || error?.message || '读取商城默认排序配置失败')
  }
}

async function handleSortSettingSave() {
  sortSettingSaving.value = true
  try {
    await productApi.saveMallSort(defaultSort.value, defaultSortDirection.value)
    message.success('商城默认排序方式已保存')
    sortSettingVisible.value = false
  } catch (error: any) {
    console.error('[商品上架] 保存商城默认排序配置失败', error)
    message.error(error?.response?.data?.message || error?.message || '保存商城默认排序配置失败')
  } finally {
    sortSettingSaving.value = false
  }
}

// ═══ 新增 / 编辑（保留既有实现） ═══
const formRef = ref()
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  productId: '',
  productName: '',
  imageUrl: '',
  salePrice: 0 as number,
  marketPrice: 0 as number,
  categoryName: '',
  stockQuantity: 0 as number,
  status: 'OFF_SHELF' as string,
  description: ''
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  productId: [{ required: true, message: '请输入商品编码', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(record: ShelfRow) {
  editingId.value = record.id ?? null
  // 只取可编辑字段：视图新增列（条码/品牌/批发价/预设进价/排序/起订量/积分/备注/标签等）不属于
  // mall_product 可写列，弹窗未提供编辑入口，故不写入表单亦不回传，避免污染保存载荷
  Object.assign(form, emptyForm(), {
    productId: record.productId || '',
    productName: record.productName || '',
    imageUrl: record.imageUrl || '',
    salePrice: record.salePrice ?? 0,
    marketPrice: record.marketPrice ?? 0,
    categoryName: record.categoryName || '',
    stockQuantity: record.stockQuantity ?? 0,
    // 读侧视图值是 ON_SHELF / INACTIVE，弹窗单选钮用写侧口径 ON_SHELF / OFF_SHELF → 此处归一，
    // 否则下架商品（INACTIVE）两个单选都不命中，弹窗上架状态为空
    status: isOnShelf(record) ? 'ON_SHELF' : 'OFF_SHELF',
    description: record.description || ''
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await mallProductApi.update(editingId.value, { ...form } as MallProduct)
      message.success('商品已更新')
    } else {
      await mallProductApi.create({ ...form } as MallProduct)
      message.success('商品已创建')
    }
    modalOpen.value = false
    loadTable()
  } catch (error) {
    console.error('[商品上架] 保存失败', error)
    message.error((error as any)?.response?.data?.message || (error as any)?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 查询 / 分页 ═══
function handleRefresh() {
  loadTable()
}
function handleSearch() {
  pagination.current = 1
  loadTable()
}
function handleReset() {
  Object.keys(searchForm).forEach(k => {
    if (k !== 'showHierarchy' && k !== 'showProductCode') delete searchForm[k]
  })
  // 恢复对标实测默认态（与首次进入一致）：上架状态/使用优惠券 = 全部(-1)、商品类型 = 普通商品(0)
  searchForm.status = -1
  searchForm.couponUsed = -1
  searchForm.productType = DEFAULT_PRODUCT_TYPE
  searchForm.showHierarchy = false
  searchForm.showProductCode = false
  selectedCategoryId.value = ROOT_CATEGORY_ID
  pagination.current = 1
  loadTable()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadTable()
}

// ═══ 导出（CSV + BOM） ═══
async function handleExport() {
  exporting.value = true
  try {
    const rows = tableData.value
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    // columns 为 computed（含动态商品标签列），此处取当前值
    const cols = () => columns.value.filter(
      c => c.type !== 'rowNo' && c.type !== 'action' && c.type !== 'checkbox' && c.key !== '__filler__'
    )
    const headers = cols().map(c => c.title || c.key)
    const body = rows.map(r => cols().map(c => (r[c.key as keyof ShelfRow] ?? '') as string | number))
    exportCsv(headers, body, '商品上架')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8)：window.open + 内联 HTML 打印模板 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = tableData.value.filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.productCode || r.productId || '')}</td>
      <td>${escapeHtml(r.productName || '')}</td>
      <td>${escapeHtml(r.specification || '')}</td>
      <td>${escapeHtml(r.unitName || '')}</td>
      <td>${fmtQty(r.stockQuantity)}</td>
      <td>${fmtMoney(r.salePrice)}</td>
      <td>${isOnShelf(r) ? '上架' : '下架'}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>商品上架</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>商品上架</h2>
    <div class="meta">
      <span>分类：${escapeHtml(currentCategoryPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>商品货号</th><th>商品名称</th><th>规格</th><th>单位</th><th>可用库存</th><th>零售价</th><th>上架</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=720')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[商品上架] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  // 四条链路并行发起（互不 await，字典/分类树后到不阻塞首屏）：
  // fetchMallTagDict 就绪后自行重建标签列并按当前行重算 √/×
  fetchMallTagDict()
  fetchCategoryTree()
  fetchTagOptions()
  loadTable()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-row.second-row { margin-top: 8px; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.cell-link { color: #1890ff; cursor: pointer; }
.flag-on { color: #52c41a; font-weight: 600; }
.flag-off { color: #bfbfbf; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
