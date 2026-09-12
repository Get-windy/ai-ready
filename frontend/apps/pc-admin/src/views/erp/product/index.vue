<template>
  <ErrorBoundary @error="handleError">
    <PartnerListPage
      ref="listRef"
      page-title="商品"
      category-title="商品分类"
      search-placeholder="商品名称/货号/条码/规格/型号"
      :show-type-sidebar="false"
      :tree-editable="true"
      :hide-default-toolbar="true"
      :view-mode="activeTab !== 'shelf'"
      :tabs="tabs"
      v-model:active-tab="activeTab"
      :columns="activeColumns"
      :storage-key="storageKey"
      :data-adapter="dataAdapter"
      :category-adapter="categoryAdapter"
      :extra-params="extraParams"
      :default-page-size="20"
      @tab-change="onTabChange"
      @cell-change="onCellChange"
    >
      <!-- ═══ 工具栏（按子 Tab 切换，对标 ql361 各子标签按钮） ═══ -->
      <template #toolbar-left>
        <a-space>
          <a-button
            type="primary"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增
          </a-button>
          <template v-if="activeTab === 'all'">
            <a-button
              size="small"
              @click="openImportModal"
            >
              <UploadOutlined /> 导入
            </a-button>
            <a-button
              size="small"
              @click="openCloudImportModal"
            >
              <CloudUploadOutlined /> 云导入
            </a-button>
          </template>
          <template v-else-if="activeTab === 'shelf'">
            <a-button
              size="small"
              @click="handleBatchShelf(1)"
            >
              批量上架
            </a-button>
            <a-button
              size="small"
              @click="handleBatchShelf(0)"
            >
              批量下架
            </a-button>
            <a-button
              size="small"
              @click="openSortModal"
            >
              设置商城默认排序方式
            </a-button>
          </template>
          <template v-else-if="activeTab === 'auth'">
            <a-button
              size="small"
              @click="openShieldModal"
            >
              批量屏蔽
            </a-button>
            <a-button
              size="small"
              @click="handleBatchCancelShield"
            >
              批量取消
            </a-button>
          </template>
        </a-space>
      </template>

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
            v-if="activeTab === 'package'"
            size="small"
            @click="handleBarcodePrint"
          >
            条码打印
          </a-button>
          <a-button
            v-if="activeTab === 'all' || activeTab === 'auth'"
            size="small"
            @click="handleExport"
          >
            <DownloadOutlined /> 导出
          </a-button>
          <a-dropdown>
            <a-button size="small">
              更多 <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <template v-if="activeTab === 'all'">
                  <a-menu-item @click="openMoveModal">
                    批量搬移
                  </a-menu-item>
                  <a-menu-item @click="openBatchEditModal">
                    批量修改
                  </a-menu-item>
                  <a-menu-item @click="handleBatchStatus('DISABLED')">
                    停用
                  </a-menu-item>
                  <a-menu-item @click="handleBatchStatus('ENABLED')">
                    启用
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    danger
                    @click="handleBatchDelete"
                  >
                    批量删除
                  </a-menu-item>
                </template>
                <template v-else>
                  <a-menu-item @click="refreshAll">
                    刷新
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab !== 'auth'"
                    @click="openBatchEditModal"
                  >
                    批量修改
                  </a-menu-item>
                  <a-menu-item
                    danger
                    @click="handleBatchDelete"
                  >
                    批量删除
                  </a-menu-item>
                </template>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>

      <!-- ═══ 查询区（对标 ql361 商品页固定查询条件） ═══ -->
      <template #search-fields>
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">筛选条件</span>
            <a-input
              v-model:value="searchForm.keyword"
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
              v-model:value="searchForm.brand"
              size="small"
              style="width: 150px"
              placeholder="品牌"
              show-search
              allow-clear
              :filter-option="filterOption"
            >
              <a-select-option
                v-for="b in brandOptions"
                :key="b"
                :value="b"
              >
                {{ b }}
              </a-select-option>
            </a-select>
          </div>
          <div class="search-item">
            <span class="search-label">新增日期（起）</span>
            <a-date-picker
              v-model:value="searchForm.createTimeStart"
              size="small"
              style="width: 140px"
              placeholder=""
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="search-item">
            <span class="search-label">新增日期（止）</span>
            <a-date-picker
              v-model:value="searchForm.createTimeEnd"
              size="small"
              style="width: 140px"
              placeholder=""
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="search-item">
            <span class="search-label">显示状态</span>
            <a-select
              v-model:value="searchForm.status"
              size="small"
              style="width: 110px"
              allow-clear
            >
              <a-select-option value="">
                全部
              </a-select-option>
              <a-select-option value="ENABLED">
                已启用
              </a-select-option>
              <a-select-option value="DISABLED">
                已停用
              </a-select-option>
            </a-select>
          </div>
          <div class="search-item">
            <span class="search-label">使用优惠券</span>
            <a-select
              v-model:value="searchForm.useCoupon"
              size="small"
              style="width: 100px"
            >
              <a-select-option :value="''">
                全部
              </a-select-option>
              <a-select-option :value="1">
                是
              </a-select-option>
              <a-select-option :value="0">
                否
              </a-select-option>
            </a-select>
          </div>
        </div>
        <div class="search-row second-row">
          <div class="search-item">
            <span class="search-label">是否标品</span>
            <a-select
              v-model:value="searchForm.isStandardProduct"
              size="small"
              style="width: 100px"
            >
              <a-select-option :value="''">
                全部
              </a-select-option>
              <a-select-option :value="1">
                是
              </a-select-option>
              <a-select-option :value="0">
                否
              </a-select-option>
            </a-select>
          </div>
          <div class="search-item">
            <span class="search-label">所属行业类别</span>
            <a-select
              v-model:value="searchForm.industryCategory"
              size="small"
              style="width: 140px"
              placeholder="全部"
              show-search
              allow-clear
              :filter-option="filterOption"
            >
              <a-select-option
                v-for="item in industryCategoryOptions"
                :key="item"
                :value="item"
              >
                {{ item }}
              </a-select-option>
            </a-select>
          </div>
          <a-button
            type="primary"
            size="small"
            class="btn-search"
            @click="handleSearch"
          >
            查询
          </a-button>
          <a-checkbox
            v-model:checked="showHierarchy"
            style="margin-left: 12px"
          >
            显示层次结构
          </a-checkbox>
        </div>
      </template>

      <!-- ═══ 行级操作 ═══ -->
      <template #actionCell="{ record }">
        <template v-if="activeTab === 'auth'">
          <a-space :size="0">
            <a-button
              type="link"
              size="small"
              @click="handleCancelShield(record)"
            >
              取消屏蔽
            </a-button>
          </a-space>
        </template>
        <template v-else>
          <a-space :size="0">
            <a-button
              type="link"
              size="small"
              @click="handleEdit(record)"
            >
              修改
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleToggleStatus(record)"
            >
              {{ record.status === 'ENABLED' ? '停用' : '启用' }}
            </a-button>
            <a-dropdown>
              <a-button
                type="link"
                size="small"
              >
                更多
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleView(record)">
                    查看详情
                  </a-menu-item>
                  <a-menu-item @click="handleCopy(record)">
                    复制新增
                  </a-menu-item>
                  <a-menu-item @click="openShieldForProduct(record)">
                    商品授权
                  </a-menu-item>
                  <a-menu-item
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>

      <!-- ═══ 图片列 ═══ -->
      <template #imageCell="{ record }">
        <a-image
          v-if="record.imageUrl"
          :src="record.imageUrl"
          :width="36"
          :height="36"
          style="border-radius: 4px; object-fit: cover;"
          :preview="{ mask: false }"
          :fallback="BLANK_PX"
        />
        <span
          v-else
          class="no-image"
        >-</span>
      </template>

      <!-- ═══ 名称列 ═══ -->
      <template #productNameCell="{ record }">
        <a
          class="cell-link"
          @click="handleView(record)"
        >{{ record.productName || '-' }}</a>
      </template>

      <template #kitNameCell="{ record }">
        <a
          class="cell-link"
          @click="handleViewKit(record)"
        >{{ record.kitName || '-' }}</a>
      </template>

      <!-- ═══ 商品上架：上架开关 ═══ -->
      <template #shelfCell="{ record }">
        <a-switch
          :checked="record.mallShelfStatus === 1"
          size="small"
          checked-children="上架"
          un-checked-children="下架"
          @change="(checked: any) => handleShelfToggle(record, checked)"
        />
      </template>

      <!-- ═══ 套餐商品明细 ═══ -->
      <template #itemsSummaryCell="{ record }">
        <a-tooltip :title="record.itemsSummary">
          <span class="ellipsis-cell">{{ record.itemsSummary || '-' }}</span>
        </a-tooltip>
      </template>
    </PartnerListPage>

    <!-- ═══════════ 导入商品 ═══════════ -->
    <FullScreenDetail
      :visible="importModalVisible"
      title="导入商品"
      :save-loading="importLoading"
      @close="importModalVisible = false"
      @save="handleImportOk"
    >
      <div style="padding: 16px 0">
        <a-alert
          type="info"
          show-icon
          message="支持 .xlsx / .csv；表头需包含「商品名称」（必填），可选列：货号、规格、型号、产地、品牌、单位、条码、所属行业类别、零售价、批发价、预设进价、保质期天数、备注。同名商品自动跳过。"
          style="margin-bottom: 12px"
        />
        <a-upload-dragger
          v-model:file-list="importFileList"
          :before-upload="() => false"
          :max-count="1"
          accept=".xlsx,.xls,.csv"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">
            点击或拖拽文件到此区域上传
          </p>
        </a-upload-dragger>
        <a-alert
          v-if="importResult"
          :type="importResult.errors.length ? 'warning' : 'success'"
          show-icon
          style="margin-top: 12px"
          :message="`导入 ${importResult.count} 条，跳过 ${importResult.skipped} 条${importResult.errors.length ? '，' + importResult.errors.length + ' 行异常' : ''}`"
        />
        <div
          v-if="importResult && importResult.errors.length"
          class="import-errors"
        >
          <div
            v-for="(err, i) in importResult.errors.slice(0, 20)"
            :key="i"
          >
            {{ err }}
          </div>
        </div>
      </div>
    </FullScreenDetail>

    <!-- ═══════════ 云导入 ═══════════ -->
    <a-modal
      v-model:open="cloudModalVisible"
      title="云导入商品"
      width="960px"
      :confirm-loading="cloudImporting"
      ok-text="导入选中商品"
      @ok="handleCloudImportOk"
    >
      <a-space style="margin-bottom: 12px">
        <a-input
          v-model:value="cloudKeyword"
          placeholder="商品名称/编码/条码/品牌"
          size="small"
          style="width: 220px"
          allow-clear
          @press-enter="fetchCloudCatalog"
        />
        <a-button
          size="small"
          @click="fetchCloudCatalog"
        >
          查询
        </a-button>
        <span style="color:#888;font-size:12px;">归入分类：</span>
        <a-tree-select
          v-model:value="cloudCategoryId"
          :tree-data="categoryTreeForSelect"
          :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
          placeholder="请选择分类"
          allow-clear
          size="small"
          style="width: 200px"
        />
      </a-space>
      <a-table
        :columns="cloudColumns"
        :data-source="cloudRows"
        :row-selection="{ selectedRowKeys: cloudSelectedIds, onChange: onCloudSelectChange }"
        :pagination="false"
        :loading="cloudLoading"
        size="small"
        row-key="id"
        :scroll="{ y: 360 }"
      />
      <div style="margin-top: 8px; color:#888; font-size:12px;">
        共 {{ cloudTotal }} 条云商品；同名商品导入时自动跳过。
      </div>
    </a-modal>

    <!-- ═══════════ 批量搬移分类 ═══════════ -->
    <a-modal
      v-model:open="moveModalVisible"
      title="批量搬移"
      width="420px"
      @ok="handleMoveOk"
    >
      <p style="color:#888;font-size:13px;">
        将选中的 {{ selectedRows.length }} 个商品搬移到目标分类
      </p>
      <a-tree-select
        v-model:value="moveCategoryId"
        :tree-data="categoryTreeForSelect"
        :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
        placeholder="请选择目标分类"
        allow-clear
        style="width: 100%"
      />
    </a-modal>

    <!-- ═══════════ 批量修改 ═══════════ -->
    <a-modal
      v-model:open="batchEditModalVisible"
      title="批量修改"
      width="460px"
      @ok="handleBatchEditOk"
    >
      <p style="color:#888;font-size:13px;">
        将修改选中的 {{ selectedRows.length }} 个商品，留空的字段保持不变
      </p>
      <a-form
        layout="horizontal"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="品牌">
          <a-input
            v-model:value="batchEdit.brand"
            size="small"
            placeholder="不修改请留空"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="所属行业类别">
          <a-select
            v-model:value="batchEdit.industryCategory"
            size="small"
            placeholder="不修改请留空"
            allow-clear
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="item in industryCategoryOptions"
              :key="item"
              :value="item"
            >
              {{ item }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="商品分类">
          <a-tree-select
            v-model:value="batchEdit.categoryId"
            :tree-data="categoryTreeForSelect"
            :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="不修改请留空"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="是否标品">
          <a-select
            v-model:value="batchEdit.isStandardProduct"
            size="small"
            placeholder="不修改请留空"
            allow-clear
          >
            <a-select-option :value="1">
              标品
            </a-select-option>
            <a-select-option :value="0">
              非标品
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="使用优惠券">
          <a-select
            v-model:value="batchEdit.useCoupon"
            size="small"
            placeholder="不修改请留空"
            allow-clear
          >
            <a-select-option :value="1">
              是
            </a-select-option>
            <a-select-option :value="0">
              否
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══════════ 设置商城默认排序方式 ═══════════ -->
    <a-modal
      v-model:open="sortModalVisible"
      title="设置商城默认排序方式"
      width="420px"
      @ok="handleSortOk"
    >
      <a-radio-group v-model:value="mallSortType">
        <a-radio value="DEFAULT">
          默认排序
        </a-radio>
        <a-radio value="SALES">
          按销量排序
        </a-radio>
        <a-radio value="MANUAL">
          手动排序（按排序值）
        </a-radio>
      </a-radio-group>
    </a-modal>

    <!-- ═══════════ 批量屏蔽（商品授权） ═══════════ -->
    <a-modal
      v-model:open="shieldModalVisible"
      title="批量屏蔽"
      width="520px"
      @ok="handleShieldOk"
    >
      <p style="color:#888;font-size:13px;">
        {{ shieldProductIds.length }} 个商品 × {{ shieldPartnerIds.length }} 个客户建立屏蔽关系
      </p>
      <a-form
        layout="horizontal"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="屏蔽客户">
          <a-select
            v-model:value="shieldPartnerIds"
            mode="multiple"
            size="small"
            placeholder="请选择要屏蔽的客户"
            show-search
            :filter-option="filterOption"
            :loading="partnerLoading"
          >
            <a-select-option
              v-for="p in partnerOptions"
              :key="p.id"
              :value="p.id"
            >
              {{ p.partnerName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="屏蔽级别">
          <a-select
            v-model:value="shieldLevel"
            size="small"
          >
            <a-select-option
              v-for="(v, k) in SHELF_LEVEL_MAP"
              :key="k"
              :value="k"
            >
              {{ v.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="屏蔽区域">
          <a-input
            v-model:value="shieldRegion"
            size="small"
            placeholder="选填，如 兰州/西宁"
            allow-clear
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <PrintDialog
      ref="printDialogRef"
      page-code="md:product"
      :print-data="printData"
    />
    <PrintDialog
      ref="barcodePrintDialogRef"
      page-code="md:barcode"
      :print-data="barcodePrintData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  DownOutlined,
  UploadOutlined,
  CloudUploadOutlined,
  PrinterOutlined,
  InboxOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import PartnerListPage from '@/views/md/components/PartnerListPage.vue'
import type { MdListAdapter, MdCategoryAdapter } from '@/views/md/components/partnerListTypes'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import {
  productApi,
  productCategoryApi,
  productGradeApi,
  productKitApi,
  productShieldApi,
  cloudProductApi,
  mallTagApi,
} from '@/api/erp/product'
import type { ProductGrade, ProductShield, CloudProduct, MallTag } from '@/api/erp/product'
import { partnerApi } from '@/api/erp/partner'
import {
  buildAllColumns,
  buildKitColumns,
  buildShelfColumns,
  buildShieldColumns,
  GRADE_SLOT_CODES,
  PRODUCT_TYPE_MAP,
  SHELF_LEVEL_MAP,
} from './columns'
import request from '@/utils/request'

/** 图片加载失败时的占位（1x1 透明，避免出现碎图图标 + 资源错误噪声） */
const BLANK_PX = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'

const router = useRouter()

// ── 子 Tab（对标 ql361：全部商品 / 套餐 / 商品上架 / 商品授权）──
const tabs = [
  { key: 'all', label: '全部商品' },
  { key: 'package', label: '套餐' },
  { key: 'shelf', label: '商品上架' },
  { key: 'auth', label: '商品授权' },
]
const activeTab = ref('all')

/** 每个子 Tab 独立一套列配置（独立 storage-key） */
const storageKey = computed(() => `erp-product-${activeTab.value}-columns`)

// ── 价格等级 ──
const grades = ref<ProductGrade[]>([])
const gradeMeta = computed(() =>
  grades.value.map(g => ({ gradeCode: g.gradeCode, gradeName: g.gradeName })),
)

// ── 商品标签（标准槽位 TAG_N + 用户自定义昵称，来源「商品辅助资料 → 商品标签」）──
const mallTags = ref<MallTag[]>([])
const mallTagMeta = computed(() =>
  mallTags.value
    .filter(t => t.tagCode)
    .map(t => ({ tagCode: t.tagCode as string, tagName: t.tagName })),
)

// ── 列定义（按子 Tab 切换）──
const activeColumns = computed<DetailColumnConfig[]>(() => {
  switch (activeTab.value) {
    case 'package':
      return buildKitColumns()
    case 'shelf':
      return buildShelfColumns(gradeMeta.value, mallTagMeta.value)
    case 'auth':
      return buildShieldColumns()
    default:
      return buildAllColumns(gradeMeta.value)
  }
})

// ── 查询条件 ──
const searchForm = reactive({
  keyword: '',
  brand: '',
  createTimeStart: null as any,
  createTimeEnd: null as any,
  // 对标默认「已启用」
  status: 'ENABLED' as string,
  useCoupon: '' as any,
  isStandardProduct: '' as any,
  industryCategory: '',
})
const showHierarchy = ref(false)
const brandOptions = ref<string[]>([])
const industryCategoryOptions = ref<string[]>([])

function filterOption(input: string, option: any) {
  return String(option?.children ?? option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

/** 查询参数（PartnerListPage 每次查询时调用） */
function extraParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (searchForm.brand) params.brand = searchForm.brand
  if (searchForm.status) params.status = searchForm.status
  if (searchForm.industryCategory) params.industryCategory = searchForm.industryCategory
  if (searchForm.createTimeStart) params.createTimeStart = searchForm.createTimeStart
  if (searchForm.createTimeEnd) params.createTimeEnd = searchForm.createTimeEnd
  if (searchForm.useCoupon !== '' && searchForm.useCoupon != null) params.useCoupon = Number(searchForm.useCoupon)
  if (searchForm.isStandardProduct !== '' && searchForm.isStandardProduct != null) {
    params.isStandardProduct = Number(searchForm.isStandardProduct)
  }
  return params
}

// ── 列表实例 / 选中行 ──
const listRef = ref<any>(null)
const selectedRows = computed<any[]>(() => listRef.value?.getSelectedRows?.() || [])

/** 展开后端返回的等级价 Map 与商城标签，便于列渲染 */
function decorate(record: any) {
  const gpm = record.gradePriceMap || {}
  // 价格等级列按标准槽位 GRADE_1..8 定位（与后端 grade_price_1..8 列一一对应），
  // 不随用户自定义昵称或接口返回顺序变化
  GRADE_SLOT_CODES.forEach((code, i) => {
    record[`_grade${i + 1}`] = gpm[code] ?? null
  })
  // 商品侧存的是标签槽位编码（TAG_1,TAG_5…）；按槽位打标，列标题取用户自定义昵称
  const tagCodes = String(record.mallTags || '')
    .split(',')
    .map(t => t.trim())
    .filter(Boolean)
  mallTagMeta.value.forEach(t => {
    record[`tag_${t.tagCode}`] = tagCodes.includes(t.tagCode)
  })
  return record
}

// ── 数据适配器：4 个子 Tab 各自的数据源 ──
const dataAdapter: MdListAdapter = {
  async load(params) {
    const tab = activeTab.value
    if (tab === 'package') {
      // 套餐接口只接受 pageNum/pageSize/keyword/kitType/status(数字)，其余条件不适用
      const kitParams = {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        keyword: params.keyword,
      }
      const res: any = await productKitApi.page(kitParams as any)
      const records = res?.records || []
      // 批量补齐「商品明细」列
      const ids = records.map((r: any) => r.id)
      let summary: Record<string, string> = {}
      if (ids.length) {
        try {
          summary = await request_kitSummary(ids)
        } catch {
          summary = {}
        }
      }
      return {
        records: records.map((r: any) => ({
          ...r,
          bundleSale: !(r.allowPartial === true),
          barcode: r.productCode || '',
          itemsSummary: summary[String(r.id)] || '',
        })),
        total: Number(res?.total || 0),
      }
    }
    if (tab === 'auth') {
      const res: any = await productShieldApi.page(params)
      return { records: res?.records || [], total: Number(res?.total || 0) }
    }
    const res: any = await productApi.page(params)
    return { records: (res?.records || []).map(decorate), total: Number(res?.total || 0) }
  },
  updateStatus(id, status) {
    return productApi.updateStatus(id, status)
  },
  remove(id) {
    return productApi.delete(id)
  },
}

/** 套餐商品明细摘要（批量接口） */
function request_kitSummary(kitIds: (string | number)[]) {
  return request.get<any, Record<string, string>>('/erp/product-kit/items-summary', {
    params: { kitIds: kitIds.join(',') },
  })
}

// ── 分类适配器（商品分类树）──
const categoryTreeForSelect = ref<any[]>([])
const categoryAdapter: MdCategoryAdapter = {
  async load() {
    const data = await productCategoryApi.getTree()
    const tree = Array.isArray(data) ? data : []
    categoryTreeForSelect.value = tree
    return tree
  },
  create(payload: any) {
    return productCategoryApi.create(payload)
  },
  update(id: any, payload: any) {
    return productCategoryApi.update(id, payload)
  },
  remove(id: any) {
    return productCategoryApi.delete(id)
  },
}

// ── 查询/刷新 ──
function handleSearch() {
  listRef.value?.search?.()
}

function refreshAll() {
  listRef.value?.refreshAll?.()
}

function onTabChange() {
  selectedRowsClear()
}

function selectedRowsClear() {
  // 切换 Tab 时清空选择（PartnerListPage 内部已清）
}

// ── 行内编辑（商品上架：排序方式/排序值/起订量/积分/关键字、上架开关）──
async function onCellChange(record: any, fieldKey: string, value: any) {
  const editable = ['mallSortType', 'mallSortOrder', 'mallMinOrderQty', 'mallPoints', 'keywords']
  if (!editable.includes(fieldKey)) {
    return
  }
  try {
    await productApi.update(record.id, { [fieldKey]: value })
    message.success('已保存')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
    listRef.value?.refresh?.()
  }
}

async function handleShelfToggle(record: any, checked: boolean) {
  try {
    await productApi.batchShelf([record.id], checked ? 1 : 0)
    record.mallShelfStatus = checked ? 1 : 0
    message.success(checked ? '已上架' : '已下架')
  } catch {
    message.error('操作失败')
  }
}

// ── 跳转 ──
function handleAdd() {
  router.push('/erp/product/create')
}

function handleEdit(record: any) {
  router.push(`/erp/product/form/${record.id}`)
}

function handleView(record: any) {
  if (activeTab.value === 'auth') return
  router.push(`/erp/product/form/${record.id}`)
}

function handleViewKit(record: any) {
  if (record.productId) {
    router.push(`/erp/product/form/${record.productId}`)
  } else {
    message.info('该套餐未关联商品主档')
  }
}

function handleCopy(record: any) {
  router.push(`/erp/product/create?copyFrom=${record.id}`)
}

// ── 状态 / 删除 ──
function handleToggleStatus(record: any) {
  const target = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  Modal.confirm({
    title: '确认',
    content: `确定要${target === 'ENABLED' ? '启用' : '停用'}商品「${record.productName}」吗？`,
    onOk: async () => {
      try {
        await productApi.updateStatus(record.id, target)
        message.success(target === 'ENABLED' ? '已启用' : '已停用')
        listRef.value?.refresh?.()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除商品「${record.productName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productApi.delete(record.id)
        message.success('删除成功')
        listRef.value?.refresh?.()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

// ── 批量状态 ──
function handleBatchStatus(status: 'ENABLED' | 'DISABLED') {
  const rows = selectedRows.value
  if (!rows.length) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: status === 'ENABLED' ? '批量启用' : '批量停用',
    content: `确定要${status === 'ENABLED' ? '启用' : '停用'}选中的 ${rows.length} 个商品吗？`,
    onOk: async () => {
      try {
        await productApi.batchUpdateStatus(rows.map(r => r.id), status)
        message.success('操作成功')
        listRef.value?.refresh?.()
      } catch {
        message.error('操作失败')
      }
    },
  })
}

function handleBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) {
    message.warning('请先选择商品')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${rows.length} 个商品吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productApi.batchDelete(rows.map(r => r.id))
        message.success('批量删除成功')
        listRef.value?.refresh?.()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

// ── 批量搬移 ──
const moveModalVisible = ref(false)
const moveCategoryId = ref<any>(undefined)

function openMoveModal() {
  if (!selectedRows.value.length) {
    message.warning('请先选择商品')
    return
  }
  moveCategoryId.value = undefined
  moveModalVisible.value = true
}

async function handleMoveOk() {
  if (!moveCategoryId.value) {
    message.warning('请选择目标分类')
    return
  }
  try {
    const n = await productApi.batchMove(selectedRows.value.map(r => r.id), moveCategoryId.value)
    message.success(`已搬移 ${n} 个商品`)
    moveModalVisible.value = false
    listRef.value?.refresh?.()
  } catch (e: any) {
    message.error(e?.message || '搬移失败')
  }
}

// ── 批量修改 ──
const batchEditModalVisible = ref(false)
const batchEdit = reactive({
  brand: '',
  industryCategory: undefined as any,
  categoryId: undefined as any,
  isStandardProduct: undefined as any,
  useCoupon: undefined as any,
})

function openBatchEditModal() {
  if (!selectedRows.value.length) {
    message.warning('请先选择商品')
    return
  }
  Object.assign(batchEdit, {
    brand: '',
    industryCategory: undefined,
    categoryId: undefined,
    isStandardProduct: undefined,
    useCoupon: undefined,
  })
  batchEditModalVisible.value = true
}

async function handleBatchEditOk() {
  const fields: Record<string, any> = {}
  if (batchEdit.brand) fields.brand = batchEdit.brand
  if (batchEdit.industryCategory !== undefined && batchEdit.industryCategory !== '') {
    fields.industryCategory = batchEdit.industryCategory
  }
  if (batchEdit.categoryId) fields.categoryId = batchEdit.categoryId
  if (batchEdit.isStandardProduct != null && batchEdit.isStandardProduct !== '') {
    fields.isStandardProduct = batchEdit.isStandardProduct
  }
  if (batchEdit.useCoupon != null && batchEdit.useCoupon !== '') {
    fields.useCoupon = batchEdit.useCoupon
  }
  if (!Object.keys(fields).length) {
    message.warning('请至少填写一个要修改的字段')
    return
  }
  try {
    const n = await productApi.batchUpdateFields(selectedRows.value.map(r => r.id), fields)
    message.success(`已修改 ${n} 个商品`)
    batchEditModalVisible.value = false
    listRef.value?.refresh?.()
  } catch (e: any) {
    message.error(e?.message || '批量修改失败')
  }
}

// ── 商品上架：批量上下架 / 默认排序方式 ──
async function handleBatchShelf(status: number) {
  const rows = selectedRows.value
  if (!rows.length) {
    message.warning('请先选择商品')
    return
  }
  try {
    await productApi.batchShelf(rows.map(r => r.id), status)
    message.success(status === 1 ? '批量上架成功' : '批量下架成功')
    listRef.value?.refresh?.()
  } catch {
    message.error('操作失败')
  }
}

const sortModalVisible = ref(false)
const mallSortType = ref('DEFAULT')

function openSortModal() {
  sortModalVisible.value = true
}

async function handleSortOk() {
  try {
    await productApi.setMallSortType(mallSortType.value)
    message.success('已设置商城默认排序方式')
    sortModalVisible.value = false
  } catch {
    message.error('设置失败')
  }
}

// ── 商品授权（屏蔽）──
const shieldModalVisible = ref(false)
const shieldProductIds = ref<(string | number)[]>([])
const shieldPartnerIds = ref<(string | number)[]>([])
const shieldLevel = ref('ALL')
const shieldRegion = ref('')
const partnerOptions = ref<any[]>([])
const partnerLoading = ref(false)

function openShieldModal() {
  const rows = selectedRows.value
  if (!rows.length) {
    message.warning('请先选择商品')
    return
  }
  shieldProductIds.value = rows.map(r => r.id)
  shieldPartnerIds.value = []
  shieldLevel.value = 'ALL'
  shieldRegion.value = ''
  shieldModalVisible.value = true
  loadPartners()
}

function openShieldForProduct(record: any) {
  shieldProductIds.value = [record.id]
  shieldPartnerIds.value = []
  shieldLevel.value = 'ALL'
  shieldRegion.value = ''
  shieldModalVisible.value = true
  loadPartners()
}

async function loadPartners() {
  if (partnerOptions.value.length) return
  partnerLoading.value = true
  try {
    const res: any = await partnerApi.page({ pageNum: 1, pageSize: 500, partnerType: 'CUSTOMER' })
    partnerOptions.value = (res?.records || []).map((r: any) => ({ id: r.id, partnerName: r.partnerName }))
  } catch {
    partnerOptions.value = []
  } finally {
    partnerLoading.value = false
  }
}

async function handleShieldOk() {
  if (!shieldPartnerIds.value.length) {
    message.warning('请选择要屏蔽的客户')
    return
  }
  try {
    const names = shieldPartnerIds.value
      .map(id => partnerOptions.value.find(p => String(p.id) === String(id))?.partnerName || '')
      .filter(Boolean)
      .join(',')
    const n = await productShieldApi.batchShield({
      productIds: shieldProductIds.value,
      partnerIds: shieldPartnerIds.value,
      partnerNames: names,
      shieldLevel: shieldLevel.value,
      region: shieldRegion.value,
    })
    message.success(`已新增 ${n} 条屏蔽关系`)
    shieldModalVisible.value = false
    if (activeTab.value === 'auth') {
      listRef.value?.refresh?.()
    }
  } catch (e: any) {
    message.error(e?.message || '批量屏蔽失败')
  }
}

async function handleCancelShield(record: ProductShield) {
  try {
    await productShieldApi.batchCancel([record.id as any])
    message.success('已取消屏蔽')
    listRef.value?.refresh?.()
  } catch {
    message.error('取消失败')
  }
}

async function handleBatchCancelShield() {
  const rows = selectedRows.value
  if (!rows.length) {
    message.warning('请先选择授权记录')
    return
  }
  Modal.confirm({
    title: '批量取消',
    content: `确定要取消选中的 ${rows.length} 条屏蔽关系吗？`,
    onOk: async () => {
      try {
        await productShieldApi.batchCancel(rows.map(r => r.id))
        message.success('已取消')
        listRef.value?.refresh?.()
      } catch {
        message.error('取消失败')
      }
    },
  })
}

// ── 云导入 ──
const cloudModalVisible = ref(false)
const cloudLoading = ref(false)
const cloudImporting = ref(false)
const cloudKeyword = ref('')
const cloudRows = ref<CloudProduct[]>([])
const cloudTotal = ref(0)
const cloudSelectedIds = ref<(string | number)[]>([])
const cloudCategoryId = ref<any>(undefined)
const cloudColumns = [
  { title: '云商品编码', dataIndex: 'cloudCode', key: 'cloudCode', width: 120 },
  { title: '云商品名称', dataIndex: 'cloudName', key: 'cloudName', width: 220 },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 120 },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 90 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '条码', dataIndex: 'barcode', key: 'barcode', width: 140 },
  { title: '预设进价', dataIndex: 'presetPurchasePrice', key: 'presetPurchasePrice', width: 100 },
  { title: '零售价', dataIndex: 'retailPrice', key: 'retailPrice', width: 100 },
]

function openCloudImportModal() {
  cloudKeyword.value = ''
  cloudSelectedIds.value = []
  cloudCategoryId.value = undefined
  cloudModalVisible.value = true
  fetchCloudCatalog()
}

async function fetchCloudCatalog() {
  cloudLoading.value = true
  try {
    const res: any = await cloudProductApi.page({ pageNum: 1, pageSize: 100, keyword: cloudKeyword.value })
    cloudRows.value = res?.records || []
    cloudTotal.value = res?.total || 0
  } catch {
    cloudRows.value = []
    cloudTotal.value = 0
  } finally {
    cloudLoading.value = false
  }
}

function onCloudSelectChange(keys: (string | number)[]) {
  cloudSelectedIds.value = keys
}

async function handleCloudImportOk() {
  if (!cloudSelectedIds.value.length) {
    message.warning('请选择要导入的云商品')
    return
  }
  cloudImporting.value = true
  try {
    const res = await cloudProductApi.cloudImport({
      cloudIds: cloudSelectedIds.value,
      categoryId: cloudCategoryId.value,
    })
    message.success(`导入 ${res?.imported ?? 0} 条，跳过 ${res?.skipped ?? 0} 条（同名已存在）`)
    cloudModalVisible.value = false
    listRef.value?.refresh?.()
  } catch (e: any) {
    message.error(e?.message || '云导入失败')
  } finally {
    cloudImporting.value = false
  }
}

// ── 导入商品（Excel/CSV）──
const importModalVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importResult = ref<{ count: number; skipped: number; errors: string[] } | null>(null)

function openImportModal() {
  importFileList.value = []
  importResult.value = null
  importModalVisible.value = true
}

async function handleImportOk() {
  const file = importFileList.value[0]?.originFileObj || importFileList.value[0]
  if (!file) {
    message.warning('请先选择文件')
    return
  }
  importLoading.value = true
  try {
    importResult.value = await productApi.importFile(file)
    listRef.value?.refresh?.()
  } catch (e: any) {
    message.error(e?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// ── 导出 / 打印 ──
async function handleExport() {
  try {
    const params = extraParams()
    const blob: any = await request.get('/erp/product/export', { params, responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `商品_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.warning('导出失败')
  }
}

const printDialogRef = ref<any>(null)

/** 打印数据：优先勾选行，未勾选时打印当前页 */
function printableRows(): any[] {
  const rows = selectedRows.value
  if (rows.length) return rows
  return listRef.value?.getTableData?.() || []
}

const printData = ref<Record<string, any>>({ title: '商品列表', columns: [], rows: [] })

function buildPrintData() {
  const rows = printableRows()
  printData.value = {
    title: { all: '商品列表', package: '套餐列表', shelf: '商品上架', auth: '商品授权' }[activeTab.value] || '商品列表',
    columns: activeColumns.value
      .filter(c => !['imageUrl', 'rowNo', 'checkbox', 'action'].includes(c.key))
      .map(c => ({ key: c.key, title: c.title })),
    rows: rows.map(r => {
      const flat: Record<string, any> = { ...r }
      if (activeTab.value === 'all') {
        flat.productType = PRODUCT_TYPE_MAP[r.productType] || r.productType
      }
      return flat
    }),
  }
}

function handlePrint() {
  if (!printableRows().length) {
    message.warning('暂无可打印的数据')
    return
  }
  buildPrintData()
  printDialogRef.value?.open?.()
}

/** 套餐：条码打印（走条码打印模板） */
const barcodePrintDialogRef = ref<any>(null)
const barcodePrintData = ref<Record<string, any>>({ title: '套餐条码', columns: [], rows: [] })

function handleBarcodePrint() {
  if (!printableRows().length) {
    message.warning('暂无可打印的数据')
    return
  }
  barcodePrintData.value = {
    title: '套餐条码',
    columns: [
      { key: 'kitCode', title: '套餐编号' },
      { key: 'kitName', title: '套餐名称' },
      { key: 'barcode', title: '条码' },
    ],
    rows: printableRows().map((r: any) => ({
      kitCode: r.kitCode,
      kitName: r.kitName,
      barcode: r.barcode || r.kitCode,
    })),
  }
  barcodePrintDialogRef.value?.open?.()
}

// ── 错误边界 & 快捷键 ──
function handleError(err: any) {
  console.warn('[商品列表] ErrorBoundary 捕获异常:', err)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    refreshAll()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ── 初始化 ──
async function fetchGrades() {
  try {
    const data = await productGradeApi.list()
    grades.value = (Array.isArray(data) ? data : [])
      .filter(g => g.status === 1)
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || (a.gradeLevel ?? 0) - (b.gradeLevel ?? 0))
  } catch (e) {
    console.error('[商品列表] 加载价格等级失败', e)
  }
}

/** 加载商品标签字典（标准槽位 + 用户昵称）：商品上架子标签的标签列按它动态展开 */
async function fetchMallTags() {
  try {
    const data = await mallTagApi.list()
    mallTags.value = (Array.isArray(data) ? data : [])
      .filter(t => t.status === 1)
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch (e) {
    console.error('[商品列表] 加载商品标签失败', e)
  }
  // 标签字典与列表数据是两条异步链路：字典后到时，已渲染的行上没有 tag_TAG_N 打标字段，
  // 标签列会整列空白 → 字典就绪后补一次数据刷新（仅商品上架 Tab 需要）
  if (activeTab.value === 'shelf') {
    nextTick(() => refreshAll())
  }
}

async function fetchBrandOptions() {
  try {
    brandOptions.value = await productApi.getBrands()
  } catch {
    brandOptions.value = []
  }
}

async function fetchIndustryCategoryOptions() {
  try {
    const data = await productApi.getIndustryCategories()
    industryCategoryOptions.value = Array.isArray(data) ? data : []
  } catch {
    industryCategoryOptions.value = []
  }
}

onMounted(() => {
  fetchGrades()
  fetchMallTags()
  fetchBrandOptions()
  fetchIndustryCategoryOptions()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
.no-image {
  color: #ccc;
}
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
.ellipsis-cell {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}
.import-errors {
  margin-top: 8px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  color: #d4380d;
  background: #fff2e8;
  border-radius: 4px;
  padding: 8px 12px;
}
/* ── 查询区布局（与资料模块一致） ── */
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row.second-row {
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
</style>
