<template>
  <ErrorBoundary>
    <PageContainer full-height>
      <!--
        资料模块布局（对标 ql361）：Tab 条 → 工具栏 → 查询区 → 左侧分类面板 + 右侧数据表/网格。
        骨架统一复用 CategoryListLayout（与客户/供应商等资料页同一组件，保证版式不走样）。
      -->
      <CategoryListLayout
        :show-category-panel="true"
        :category-title="activeTab === 'list' ? '商品分类' : '图片目录'"
        :category-editable="false"
        :category-tree-data="panelTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="panelSelectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :tabs="tabs"
        :active-tab="activeTab"
        :current-path="panelCurrentPath"
        :show-table-footer="true"
        @tab-change="switchTab"
        @category-select="onCategorySelect"
        @category-expand="onCategoryExpand"
        @search="handleListSearch"
      >
        <!-- ═══ 工具栏：Tab1 仅「查询方案」，Tab2 上传/自动匹配/删除/搬移 ═══ -->
        <template #toolbar-left>
          <template v-if="activeTab === 'list'">
            <a-select
              v-model:value="activeScheme"
              size="small"
              style="width: 150px"
              :placeholder="'--查询方案--'"
              @change="applyScheme"
            >
              <a-select-option :value="''">
                --查询方案--
              </a-select-option>
              <a-select-option
                v-for="s in schemeNames"
                :key="s"
                :value="s"
              >
                {{ s }}
              </a-select-option>
            </a-select>
            <a-tooltip title="保存为查询方案">
              <a-button
                size="small"
                @click="saveScheme"
              >
                <PlusOutlined />
              </a-button>
            </a-tooltip>
          </template>
          <template v-else>
            <a-button
              type="primary"
              size="small"
              class="btn-orange"
              @click="triggerSpaceUpload"
            >
              <UploadOutlined /> 上传图片
            </a-button>
            <input
              ref="spaceFileRef"
              type="file"
              accept="image/*"
              multiple
              style="display:none"
              @change="onSpaceFilesSelected"
            >
          </template>
        </template>

        <template #toolbar-right>
          <template v-if="activeTab === 'space'">
            <a-button
              type="primary"
              size="small"
              class="btn-orange"
              @click="handleAutoMatch"
            >
              <ThunderboltOutlined /> 自动匹配
            </a-button>
            <a-button
              size="small"
              :disabled="!selectedMaterialIds.length"
              @click="handleSpaceDelete"
            >
              <DeleteOutlined /> 删除
            </a-button>
            <a-button
              size="small"
              :disabled="!selectedMaterialIds.length"
              @click="openMoveModal"
            >
              <DragOutlined /> 搬移
            </a-button>
          </template>
        </template>

        <!-- ═══ 查询条件 ═══ -->
        <template #search-fields>
          <template v-if="activeTab === 'list'">
            <div class="search-row">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="listQuery.keyword"
                  placeholder="请输入品名/货号/助记码"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleListSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">图片</span>
                <a-select
                  v-model:value="listQuery.imageFilter"
                  size="small"
                  style="width: 120px"
                  placeholder="全部"
                  allow-clear
                >
                  <a-select-option value="ALL">
                    全部
                  </a-select-option>
                  <a-select-option value="HAS">
                    已上传图片
                  </a-select-option>
                  <a-select-option value="NONE">
                    未上传图片
                  </a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">规格</span>
                <a-input
                  v-model:value="listQuery.spec"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleListSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">型号</span>
                <a-input
                  v-model:value="listQuery.model"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleListSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">产地</span>
                <a-input
                  v-model:value="listQuery.origin"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleListSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">品牌</span>
                <a-input
                  v-model:value="listQuery.brand"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleListSearch"
                >
                  <template #suffix>
                    <SearchOutlined
                      style="color:#bbb;cursor:pointer"
                      @click="handleListSearch"
                    />
                  </template>
                </a-input>
              </div>
            </div>
            <div class="search-row second-row">
              <div class="search-item">
                <span class="search-label">商品状态</span>
                <a-select
                  v-model:value="listQuery.status"
                  size="small"
                  style="width: 110px"
                  placeholder="全部"
                  allow-clear
                >
                  <a-select-option value="ALL">
                    全部
                  </a-select-option>
                  <a-select-option value="ENABLED">
                    启用
                  </a-select-option>
                  <a-select-option value="DISABLED">
                    停用
                  </a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">排序</span>
                <a-select
                  v-model:value="listQuery.sortField"
                  size="small"
                  style="width: 130px"
                >
                  <a-select-option value="PRODUCT_CODE">
                    按货号
                  </a-select-option>
                  <a-select-option value="PRODUCT_NAME">
                    按名称
                  </a-select-option>
                  <a-select-option value="CREATE_TIME">
                    按录入时间
                  </a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">顺序</span>
                <a-select
                  v-model:value="listQuery.sortOrder"
                  size="small"
                  style="width: 100px"
                >
                  <a-select-option value="ASC">
                    顺序
                  </a-select-option>
                  <a-select-option value="DESC">
                    倒序
                  </a-select-option>
                </a-select>
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleListSearch"
              >
                查询
              </a-button>
            </div>
          </template>

          <template v-else>
            <div class="search-row">
              <a-input
                v-model:value="spaceQuery.keyword"
                placeholder="请输入关键字搜索"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSpaceSearch"
              />
              <div class="search-item">
                <span class="search-label">图片匹配方式</span>
                <a-select
                  v-model:value="spaceQuery.matchType"
                  size="small"
                  style="width: 130px"
                >
                  <a-select-option value="NAME">
                    按名称
                  </a-select-option>
                  <a-select-option value="CODE">
                    按商品货号
                  </a-select-option>
                </a-select>
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSpaceSearch"
              >
                查询
              </a-button>
            </div>
          </template>
        </template>

        <!-- ═══ 内容区 ═══ -->
        <template #table>
          <!-- Tab1：商品图片列表（对标 9 列） -->
          <div
            v-if="activeTab === 'list'"
            class="table-section"
          >
            <BillDetailTable
              ref="productTableRef"
              :columns="columns"
              v-model:data-source="tableData"
              :loading="listLoading"
              :view-mode="true"
              :fill-mode="true"
              storage-key="md-image-columns"
              global-config-key="md-image-columns-global"
            >
              <!-- 上传图片列：选择图片（__ghost 为表格补齐的空行，不显示操作） -->
              <template #uploadCell="{ record }">
                <a-button
                  v-if="!record.__ghost"
                  type="link"
                  size="small"
                  @click="openPickModal(record)"
                >
                  选择图片
                </a-button>
              </template>

              <!-- 图片列：缩略图 + 删除(红✕) + 主图标记（对标：⊙主图，点击可切换主图） -->
              <template #imageCell="{ record }">
                <div
                  v-if="record.images && record.images.length"
                  class="image-cell-list"
                >
                  <div
                    v-for="img in record.images"
                    :key="img.id"
                    class="image-cell-item"
                  >
                    <img
                      :src="img.imageUrl"
                      class="image-thumb"
                      alt="商品图片"
                      @error="onThumbError"
                    >
                    <div class="image-cell-actions">
                      <a-tooltip title="删除图片">
                        <CloseCircleFilled
                          class="image-del"
                          @click="handleRemoveImage(img, record)"
                        />
                      </a-tooltip>
                      <a-tooltip :title="img.isMain === 1 ? '当前主图' : '设为主图'">
                        <span
                          class="image-main-tag"
                          :class="{ 'is-main': img.isMain === 1 }"
                          @click="onMainTagClick(img, record)"
                        >
                          <span class="main-dot" /> 主图
                        </span>
                      </a-tooltip>
                    </div>
                  </div>
                </div>
                <span
                  v-else-if="!record.__ghost"
                  class="image-empty"
                >-</span>
              </template>
            </BillDetailTable>
          </div>

          <!-- Tab2：图片空间（图片库网格） -->
          <div
            v-else
            class="space-panel"
          >
            <div class="space-header">
              <span class="space-title">我的图片</span>
              <span class="space-tip">
                自动匹配规则：将图片的名称命名为匹配方式对应的名称，多张图片在图片名称后用 -1、-2 表示。如：匹配方式为按商品货号匹配，则匹配的图片的命名为sp001-1、sp001-2。
              </span>
              <div class="space-header-right">
                <a-checkbox
                  v-model:checked="selectAllMaterials"
                  @change="toggleSelectAllMaterials"
                >
                  全选
                </a-checkbox>
                <a-checkbox
                  v-model:checked="onlyImage"
                  @change="handleSpaceSearch"
                >
                  只显示图片
                </a-checkbox>
              </div>
            </div>

            <div class="image-grid-wrapper">
              <a-spin :spinning="spaceLoading">
                <div
                  v-if="materials.length"
                  class="image-grid"
                >
                  <div
                    v-for="m in materials"
                    :key="m.id"
                    class="image-grid-item"
                    :class="{ selected: selectedMaterialIds.includes(m.id) }"
                    @click="toggleMaterial(m.id)"
                  >
                    <div class="image-grid-thumb">
                      <img
                        :src="m.imageUrl"
                        alt="图片"
                        @error="onThumbError"
                      >
                      <span class="image-grid-check">
                        <CheckCircleFilled v-if="selectedMaterialIds.includes(m.id)" style="color:#ff6b35" />
                        <span
                          v-else
                          class="unchecked-circle"
                        />
                      </span>
                    </div>
                    <div
                      class="image-grid-name"
                      :title="m.imageName"
                    >
                      {{ m.imageName || '-' }}
                    </div>
                  </div>
                </div>
                <a-empty
                  v-else-if="!spaceLoading"
                  description="暂无图片"
                />
              </a-spin>
            </div>
          </div>
        </template>

        <!-- ═══ 分页 ═══ -->
        <template #table-footer>
          <!-- 对标：首页/上页/第(x/y)页/下页/尾页/跳转/共N条记录/每页显示N行 -->
          <StandardPagination
            v-if="activeTab === 'list'"
            variant="classic"
            :current="listPagination.current"
            :page-size="listPagination.pageSize"
            :total="listPagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handleListPageChange"
          />
          <StandardPagination
            v-else
            :current="spacePagination.current"
            :page-size="spacePagination.pageSize"
            :total="spacePagination.total"
            :page-size-options="[60, 120, 200]"
            @change="handleSpacePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 选择图片弹窗（商品图片列表 → 图片空间选区） ═══ -->
      <a-modal
        v-model:open="pickModalOpen"
        title="选择图片"
        width="860px"
        :confirm-loading="pickLoading"
        @ok="confirmPick"
      >
        <div class="pick-toolbar">
          <a-input
            v-model:value="pickQuery.keyword"
            placeholder="请输入关键字搜索"
            size="small"
            style="width: 200px"
            allow-clear
            @press-enter="loadPickMaterials"
          />
          <a-button
            type="primary"
            size="small"
            @click="loadPickMaterials"
          >
            查询
          </a-button>
          <a-button
            type="primary"
            size="small"
            class="btn-orange"
            @click="triggerPickUpload"
          >
            <UploadOutlined /> 上传图片
          </a-button>
          <input
            ref="pickFileRef"
            type="file"
            accept="image/*"
            multiple
            style="display:none"
            @change="onPickFilesSelected"
          >
        </div>
        <div class="pick-product-tip">
          目标商品：<b>{{ pickProduct?.productName }}</b>
          <span class="tip-muted">（选择图片后将关联到该商品，可设为主图）</span>
        </div>
        <a-spin :spinning="pickLoading">
          <div
            v-if="pickMaterials.length"
            class="image-grid pick-grid"
          >
            <div
              v-for="m in pickMaterials"
              :key="m.id"
              class="image-grid-item"
              :class="{ selected: pickSelectedId === m.id }"
              @click="pickSelectedId = m.id"
            >
              <div class="image-grid-thumb">
                <img
                  :src="m.imageUrl"
                  alt="图片"
                  @error="onThumbError"
                >
                <span class="image-grid-check">
                  <CheckCircleFilled v-if="pickSelectedId === m.id" style="color:#ff6b35" />
                  <span
                    v-else
                    class="unchecked-circle"
                  />
                </span>
              </div>
              <div
                class="image-grid-name"
                :title="m.imageName"
              >
                {{ m.imageName || '-' }}
              </div>
            </div>
          </div>
          <a-empty
            v-else-if="!pickLoading"
            description="暂无图片，可先上传"
          />
        </a-spin>
        <div class="pick-footer">
          <a-checkbox v-model:checked="pickAsMain">
            设为主图
          </a-checkbox>
        </div>
      </a-modal>

      <!-- ═══ 搬移弹窗（选择目标商品） ═══ -->
      <ProductSelectModal
        v-model:open="moveModalOpen"
        :multiple="false"
        @confirm="confirmMove"
      />

      <!-- ═══ 上传结果提示（自动匹配） ═══ -->
      <a-modal
        v-model:open="matchResultOpen"
        title="自动匹配结果"
        :footer="null"
        width="420px"
      >
        <div class="match-result">
          <p>参与匹配素材：<b>{{ matchResult.total }}</b> 张</p>
          <p>匹配成功：<b style="color:#52c41a">{{ matchResult.matched }}</b> 张</p>
          <p>匹配到多个商品跳过：<b style="color:#faad14">{{ matchResult.ambiguous }}</b> 张</p>
          <p>未匹配：<b style="color:#999">{{ matchResult.unmatched }}</b> 张</p>
        </div>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  SearchOutlined,
  UploadOutlined,
  DeleteOutlined,
  DragOutlined,
  ThunderboltOutlined,
  CloseCircleFilled,
  CheckCircleFilled,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { productImageApi, type ProductImageMaterial, type ProductImageRow } from '@/api/erp/productImage'
import { productCategoryApi } from '@/api/erp/product'

defineOptions({ name: 'MdImageManagement' })

// ── Tab（对标：商品图片列表 / 图片空间） ──
type TabKey = 'list' | 'space'
const tabs = [
  { key: 'list', label: '商品图片列表' },
  { key: 'space', label: '图片空间' },
]
const activeTab = ref<TabKey>('list')
function switchTab(tab: string) {
  activeTab.value = (tab === 'space' ? 'space' : 'list')
  if (activeTab.value === 'space' && !materials.value.length) {
    loadSpaceMaterials()
  }
}

// ── 商品分类树（Tab1）/ 图片目录（Tab2，对标仅「我的图片」单节点） ──
/** 对标：分类树根节点「全部商品」，选中表示不做分类过滤 */
const ROOT_CATEGORY_ID = '0'
const MINE_DIR_ID = 'mine'
const categoryLoading = ref(false)
const categoryTreeData = ref<any[]>([])
const categoryFlat = ref<any[]>([])
const selectedCategoryId = ref<string>(ROOT_CATEGORY_ID)
const expandedKeys = ref<string[]>([])
const currentCategoryPath = computed(() => {
  if (!selectedCategoryId.value || selectedCategoryId.value === ROOT_CATEGORY_ID) return '全部商品'
  const node = categoryFlat.value.find((c) => String(c.id) === String(selectedCategoryId.value))
  return node ? node.categoryName : '全部商品'
})

/** 左侧面板随 Tab 切换内容（Tab1=商品分类树，Tab2=图片目录） */
const panelTreeData = computed(() => (
  activeTab.value === 'list'
    ? categoryTreeData.value
    : [{ id: MINE_DIR_ID, categoryName: '我的图片' }]
))
const panelSelectedCategoryId = computed(() => (
  activeTab.value === 'list' ? selectedCategoryId.value : MINE_DIR_ID
))
const panelCurrentPath = computed(() => (
  activeTab.value === 'list' ? currentCategoryPath.value : '我的图片'
))

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const data: any = await productCategoryApi.getTree()
    const realTree = normalizeTree(Array.isArray(data) ? data : data?.records || [])
    // 对标实测：根节点为「全部商品」（tool-results/ql361/image/30-tab1-struct.json）
    categoryTreeData.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品', children: realTree }]
    const flat: any[] = []
    const walk = (nodes: any[]) => {
      nodes.forEach((n) => {
        flat.push(n)
        if (n.children?.length) walk(n.children)
      })
    }
    walk(categoryTreeData.value)
    categoryFlat.value = flat
    expandedKeys.value = [ROOT_CATEGORY_ID, ...realTree.map((n) => n.id)]
  } catch {
    categoryTreeData.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部商品' }]
    categoryFlat.value = categoryTreeData.value
  } finally {
    categoryLoading.value = false
  }
}
function normalizeTree(nodes: any[]): any[] {
  return nodes.map((n) => ({
    ...n,
    id: String(n.id),
    children: n.children?.length ? normalizeTree(n.children) : undefined,
  }))
}
function onCategorySelect(keys: any[]) {
  if (activeTab.value !== 'list') return
  // 取消选中时回落到根节点「全部商品」
  selectedCategoryId.value = keys.length ? String(keys[0]) : ROOT_CATEGORY_ID
  listPagination.current = 1
  loadProductImages()
}
function onCategoryExpand(keys: any[]) {
  expandedKeys.value = keys.map(String)
}

// ── Tab1 商品图片列表 ──
const productTableRef = ref()
const listLoading = ref(false)
const tableData = ref<ProductImageRow[]>([])
const listQuery = reactive({
  keyword: '',
  imageFilter: 'ALL',
  spec: '',
  model: '',
  origin: '',
  brand: '',
  status: 'ALL',
  sortField: 'PRODUCT_CODE',
  sortOrder: 'ASC',
})
const listPagination = reactive({ current: 1, pageSize: 20, total: 0 })

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 46 },
  { key: 'productName', title: '商品名称', type: 'input', width: 200 },
  { key: 'status', title: '商品状态', type: 'input', width: 90, formatter: formatStatus },
  { key: 'productCode', title: '货号', type: 'input', width: 140 },
  { key: 'spec', title: '规格', type: 'input', width: 140 },
  { key: 'model', title: '型号', type: 'input', width: 120 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'uploadAction', title: '上传图片', type: 'slot', slotName: 'uploadCell', width: 100 },
  { key: 'imageCell', title: '图片', type: 'slot', slotName: 'imageCell', width: 320 },
]

function formatStatus(v: any) {
  if (v === 'ENABLED') return '启用'
  if (v === 'DISABLED') return '停用'
  return v || '-'
}

/** 图片失效占位（记录残留 / 物理文件缺失时避免破图与错误上报刷屏） */
const BROKEN_IMG = 'data:image/svg+xml;utf8,'
  + '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">'
  + '<rect width="100%" height="100%" fill="%23f5f5f5"/>'
  + '<text x="50%" y="52%" font-size="12" fill="%23bbb" text-anchor="middle">图片失效</text></svg>'
function onThumbError(e: Event) {
  const el = e.target as HTMLImageElement
  if (!el || el.dataset.fallback === '1') return
  el.dataset.fallback = '1'
  el.src = BROKEN_IMG
}

async function loadProductImages() {
  listLoading.value = true
  try {
    const params: any = {
      pageNum: listPagination.current,
      pageSize: listPagination.pageSize,
      sortField: listQuery.sortField,
      sortOrder: listQuery.sortOrder,
    }
    if (selectedCategoryId.value && selectedCategoryId.value !== ROOT_CATEGORY_ID) {
      params.categoryId = selectedCategoryId.value
    }
    if (listQuery.keyword) params.keyword = listQuery.keyword
    if (listQuery.imageFilter && listQuery.imageFilter !== 'ALL') params.imageFilter = listQuery.imageFilter
    if (listQuery.spec) params.spec = listQuery.spec
    if (listQuery.model) params.model = listQuery.model
    if (listQuery.origin) params.origin = listQuery.origin
    if (listQuery.brand) params.brand = listQuery.brand
    if (listQuery.status && listQuery.status !== 'ALL') params.status = listQuery.status

    const res: any = await productImageApi.page(params)
    tableData.value = res?.records || []
    listPagination.total = Number(res?.total || 0)
  } catch {
    tableData.value = []
    listPagination.total = 0
    message.error('加载商品图片列表失败')
  } finally {
    listLoading.value = false
  }
}

function handleListSearch() {
  listPagination.current = 1
  loadProductImages()
}
function handleListPageChange(current: number, pageSize: number) {
  listPagination.current = current
  listPagination.pageSize = pageSize
  loadProductImages()
}

// ── 查询方案（本地保存当前查询条件） ──
const SCHEME_KEY = 'md-image-query-schemes'
const activeScheme = ref('')
const schemes = ref<Record<string, any>>({})
const schemeNames = computed(() => Object.keys(schemes.value))

function loadSchemes() {
  try {
    schemes.value = JSON.parse(localStorage.getItem(SCHEME_KEY) || '{}')
  } catch {
    schemes.value = {}
  }
}
function saveScheme() {
  const name = window.prompt('请输入查询方案名称')
  if (!name) return
  schemes.value[name] = { ...listQuery, categoryId: selectedCategoryId.value }
  localStorage.setItem(SCHEME_KEY, JSON.stringify(schemes.value))
  activeScheme.value = name
  message.success('查询方案已保存')
}
function applyScheme(name: string) {
  if (!name || !schemes.value[name]) return
  Object.assign(listQuery, schemes.value[name])
  selectedCategoryId.value = schemes.value[name].categoryId || ROOT_CATEGORY_ID
  handleListSearch()
}

// ── 图片操作（Tab1 行内） ──
function handleRemoveImage(img: any, record: ProductImageRow) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要从「${record.productName}」删除该图片吗？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await productImageApi.batchDelete([img.id])
      message.success('删除成功')
      loadProductImages()
    },
  })
}
async function handleSetMain(img: any, record: ProductImageRow) {
  await productImageApi.setMain(img.id)
  message.success(`已设置「${record.productName}」的主图`)
  loadProductImages()
}
/** 主图标记点击：已是主图则无操作，否则切换为当前商品主图 */
function onMainTagClick(img: any, record: ProductImageRow) {
  if (img.isMain === 1) return
  handleSetMain(img, record)
}

// ── 选择图片弹窗 ──
const pickModalOpen = ref(false)
const pickLoading = ref(false)
const pickProduct = ref<ProductImageRow | null>(null)
const pickMaterials = ref<ProductImageMaterial[]>([])
const pickSelectedId = ref<string>('')
const pickAsMain = ref(true)
const pickQuery = reactive({ keyword: '' })
const pickFileRef = ref<HTMLInputElement>()

function openPickModal(record: ProductImageRow) {
  pickProduct.value = record
  pickSelectedId.value = ''
  pickQuery.keyword = ''
  pickModalOpen.value = true
  loadPickMaterials()
}
async function loadPickMaterials() {
  pickLoading.value = true
  try {
    const res: any = await productImageApi.spacePage({
      keyword: pickQuery.keyword || undefined,
      pageNum: 1,
      pageSize: 60,
    })
    pickMaterials.value = res?.records || []
  } catch {
    pickMaterials.value = []
  } finally {
    pickLoading.value = false
  }
}
function triggerPickUpload() {
  pickFileRef.value?.click()
}
async function onPickFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  await uploadFiles(files, undefined)
  loadPickMaterials()
}
async function confirmPick() {
  if (!pickSelectedId.value || !pickProduct.value) {
    message.warning('请选择一张图片')
    return
  }
  await productImageApi.bind({
    imageId: pickSelectedId.value,
    productId: pickProduct.value.productId,
    isMain: pickAsMain.value ? 1 : 0,
  })
  message.success('图片已关联到商品')
  pickModalOpen.value = false
  loadProductImages()
}

// ── Tab2 图片空间 ──
const spaceLoading = ref(false)
const materials = ref<ProductImageMaterial[]>([])
const spaceQuery = reactive({ keyword: '', matchType: 'NAME' })
const spacePagination = reactive({ current: 1, pageSize: 60, total: 0 })
const onlyImage = ref(false)
const selectedMaterialIds = ref<string[]>([])
const selectAllMaterials = ref(false)
const spaceFileRef = ref<HTMLInputElement>()

async function loadSpaceMaterials() {
  spaceLoading.value = true
  try {
    const res: any = await productImageApi.spacePage({
      keyword: spaceQuery.keyword || undefined,
      onlyImage: onlyImage.value ? 1 : undefined,
      pageNum: spacePagination.current,
      pageSize: spacePagination.pageSize,
    })
    materials.value = res?.records || []
    spacePagination.total = Number(res?.total || 0)
    selectedMaterialIds.value = []
    selectAllMaterials.value = false
  } catch {
    materials.value = []
    spacePagination.total = 0
    message.error('加载图片空间失败')
  } finally {
    spaceLoading.value = false
  }
}
function handleSpaceSearch() {
  spacePagination.current = 1
  loadSpaceMaterials()
}
function handleSpacePageChange(current: number, pageSize: number) {
  spacePagination.current = current
  spacePagination.pageSize = pageSize
  loadSpaceMaterials()
}
function toggleMaterial(id: string) {
  const idx = selectedMaterialIds.value.indexOf(id)
  if (idx >= 0) selectedMaterialIds.value.splice(idx, 1)
  else selectedMaterialIds.value.push(id)
  selectAllMaterials.value = materials.value.length > 0 && selectedMaterialIds.value.length === materials.value.length
}
function toggleSelectAllMaterials(e: any) {
  const checked = e?.target?.checked ?? selectAllMaterials.value
  selectedMaterialIds.value = checked ? materials.value.map((m) => m.id) : []
}
function triggerSpaceUpload() {
  spaceFileRef.value?.click()
}
async function onSpaceFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  await uploadFiles(files, undefined)
  loadSpaceMaterials()
}

/** 通用上传（productId 为空即图片空间素材） */
async function uploadFiles(files: File[], productId?: string) {
  if (!files.length) return
  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      message.error(`${file.name} 不是图片文件`)
      continue
    }
    if (file.size / 1024 / 1024 > 10) {
      message.error(`${file.name} 超过 10M`)
      continue
    }
    try {
      await productImageApi.upload(file, productId)
    } catch {
      message.error(`${file.name} 上传失败`)
    }
  }
  message.success('图片上传完成')
}

function handleSpaceDelete() {
  if (!selectedMaterialIds.value.length) return
  Modal.confirm({
    title: '确认删除',
    content: `确定删除选中的 ${selectedMaterialIds.value.length} 张图片吗？已关联商品的图片将同时解除关联。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await productImageApi.batchDelete([...selectedMaterialIds.value])
      message.success('删除成功')
      loadSpaceMaterials()
      if (activeTab.value === 'list') loadProductImages()
    },
  })
}

async function handleAutoMatch() {
  try {
    const res: any = await productImageApi.autoMatch(spaceQuery.matchType)
    matchResult.value = {
      total: res?.total || 0,
      matched: res?.matched || 0,
      ambiguous: res?.ambiguous || 0,
      unmatched: res?.unmatched || 0,
    }
    matchResultOpen.value = true
    loadSpaceMaterials()
  } catch {
    message.error('自动匹配失败')
  }
}
const matchResultOpen = ref(false)
const matchResult = reactive({ total: 0, matched: 0, ambiguous: 0, unmatched: 0 })

// ── 搬移 ──
const moveModalOpen = ref(false)
function openMoveModal() {
  if (!selectedMaterialIds.value.length) {
    message.warning('请先选择要搬移的图片')
    return
  }
  moveModalOpen.value = true
}
async function confirmMove(products: any[]) {
  const product = products?.[0]
  if (!product) return
  const count = await productImageApi.move({
    imageIds: [...selectedMaterialIds.value],
    productId: String(product.id),
  })
  message.success(`已搬移 ${count} 张图片到「${product.productName || product.name}」`)
  moveModalOpen.value = false
  loadSpaceMaterials()
}

onMounted(() => {
  loadSchemes()
  fetchCategoryTree()
  loadProductImages()
})
</script>

<style scoped>
/* ── 业务专属样式（骨架版式由 CategoryListLayout 提供，勿在此重复实现） ── */
/* 查询区：slot 内容带本组件 scope，需在此定义（父组件样式不作用到 slot 内容） */
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

.btn-orange {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-orange:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* Tab1 表格区：BillDetailTable 依赖 flex 链撑高 */
.table-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ── 商品图片单元 ── */
.image-cell-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0;
}
.image-cell-item {
  position: relative;
  text-align: center;
}
.image-thumb {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  background: #fafafa;
}
.image-cell-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}
.image-del {
  color: #ff4d4f;
  cursor: pointer;
  font-size: 14px;
}
/* 对标：图片下方「⊙ 主图」标记（实心点=当前主图，空心点=点击设为主图） */
.image-main-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #1890ff;
  cursor: pointer;
  user-select: none;
}
.image-main-tag .main-dot {
  width: 10px;
  height: 10px;
  border: 1px solid #1890ff;
  border-radius: 50%;
  box-sizing: border-box;
}
.image-main-tag.is-main .main-dot {
  background: #1890ff;
}
.image-empty {
  color: #bbb;
}

/* ── 图片空间 ── */
.space-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.space-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.space-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
.space-tip {
  flex: 1;
  font-size: 12px;
  color: #d46b08;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.space-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.image-grid-wrapper {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px;
}
/* 对标实测：缩略图 138x138、每行约 6 格（tool-results/ql361/image/32-space-struct.json） */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.image-grid-item {
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 4px;
  transition: border-color 0.2s;
}
.image-grid-item:hover {
  border-color: #ffd8bf;
}
.image-grid-item.selected {
  border-color: #ff6b35;
  background: #fff7f0;
}
.image-grid-thumb {
  position: relative;
  width: 100%;
  height: 138px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 4px;
  overflow: hidden;
}
.image-grid-thumb img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
.image-grid-check {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 16px;
  background: #fff;
  border-radius: 50%;
  line-height: 1;
}
.unchecked-circle {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 1px solid #d9d9d9;
  border-radius: 50%;
  background: #fff;
}
.image-grid-name {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── 选择图片弹窗 ── */
.pick-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.pick-product-tip {
  font-size: 13px;
  color: #333;
  margin-bottom: 10px;
}
.tip-muted {
  color: #999;
  font-size: 12px;
}
.pick-grid {
  max-height: 420px;
  overflow: auto;
  /* 弹窗窄于整页，缩小单格最小宽度以保持多列可浏览 */
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
}
.pick-footer {
  margin-top: 12px;
  text-align: right;
}
.match-result p {
  margin: 6px 0;
  font-size: 13px;
}
</style>
