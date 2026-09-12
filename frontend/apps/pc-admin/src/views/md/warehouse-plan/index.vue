<template>
  <ErrorBoundary>
    <PageContainer full-height>
      <!--
        资料模块布局（对标 ql361 资料 → 仓库管理 → 仓库规划）：
        Tab 条（1.仓库 / 2.货位）→ 工具栏 → 查询区 → 左侧仓库分类树 + 右侧数据表。
        骨架统一复用 CategoryListLayout，避免版式走样。
      -->
      <CategoryListLayout
        :show-category-panel="activeTab === 'warehouse'"
        category-title="仓库分类"
        :category-editable="activeTab === 'warehouse'"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :tabs="tabs"
        :active-tab="activeTab"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="switchTab"
        @category-select="onCategorySelect"
        @category-expand="onCategoryExpand"
        @category-add="openCategoryCreate"
        @search="handleSearch"
      >
        <!-- ═══ 分类树头部：修改 / 删除（对标：✎ ✕ + 收起） ═══ -->
        <template #category-header-actions>
          <a-tooltip
            title="修改"
            placement="bottom"
          >
            <a-button
              type="link"
              size="small"
              :disabled="selectedCategoryId === '0'"
              @click="openCategoryEdit"
            >
              <EditOutlined />
            </a-button>
          </a-tooltip>
          <a-popconfirm
            title="确认删除该分类？"
            ok-text="删除"
            cancel-text="取消"
            :disabled="selectedCategoryId === '0'"
            @confirm="handleCategoryDelete"
          >
            <a-tooltip
              title="删除"
              placement="bottom"
            >
              <a-button
                type="link"
                size="small"
                :disabled="selectedCategoryId === '0'"
              >
                <CloseOutlined />
              </a-button>
            </a-tooltip>
          </a-popconfirm>
        </template>

        <!-- ═══ 工具栏 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'warehouse'"
            type="primary"
            size="small"
            class="btn-orange"
            @click="openWarehouseCreate"
          >
            <PlusOutlined /> 新增
          </a-button>
          <a-button
            v-else
            type="primary"
            size="small"
            class="btn-orange"
            @click="openLocationCreate"
          >
            <PlusOutlined /> 新增货位
          </a-button>
        </template>

        <template #toolbar-right>
          <a-tooltip
            title="刷新"
            placement="bottom"
          >
            <a-button
              size="small"
              :loading="activeTab === 'warehouse' ? warehouseLoading : locationLoading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-tooltip>
          <a-tooltip
            title="打印(F8)"
            placement="bottom"
          >
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-tooltip>
          <!-- Tab1：导出按钮；Tab2：导出收在「更多」里（对标实测） -->
          <a-tooltip
            v-if="activeTab === 'warehouse'"
            title="导出"
            placement="bottom"
          >
            <a-button
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-tooltip>
          <template v-else>
            <a-tooltip
              title="批量删除"
              placement="bottom"
            >
              <a-button
                size="small"
                :disabled="!selectedLocationIds.length"
                @click="handleBatchDelete"
              >
                <DeleteOutlined /> 批量删除
              </a-button>
            </a-tooltip>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="onMoreMenuClick">
                  <a-menu-item key="export">
                    <DownloadOutlined /> 导出
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
        </template>

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="search-row">
            <template v-if="activeTab === 'warehouse'">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="warehouseQuery.keyword"
                  placeholder="编号/名称/联系人/电话/地址"
                  size="small"
                  style="width: 220px"
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
              <a-checkbox
                v-model:checked="warehouseQuery.showDisabled"
                @change="handleSearch"
              >
                显示停用
              </a-checkbox>
              <a-checkbox
                v-model:checked="warehouseQuery.showHierarchy"
                @change="handleSearch"
              >
                显示层次结构
              </a-checkbox>
            </template>
            <template v-else>
              <div class="search-item">
                <span class="search-label">仓库</span>
                <a-input
                  v-model:value="locationQuery.warehouseKeyword"
                  placeholder="仓库名称/编号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">货位编号</span>
                <a-input
                  v-model:value="locationQuery.locationCode"
                  placeholder="货位编号"
                  size="small"
                  style="width: 180px"
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
              <a-checkbox
                v-model:checked="locationQuery.showDisabled"
                @change="handleSearch"
              >
                显示停用
              </a-checkbox>
            </template>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-section">
            <BillDetailTable
              v-if="activeTab === 'warehouse'"
              ref="warehouseTableRef"
              v-model:data-source="warehouseRows"
              :columns="warehouseColumns"
              :loading="warehouseLoading"
              :view-mode="true"
              :fill-mode="true"
              storage-key="md-warehouse-plan-columns"
              global-config-key="md-warehouse-plan-columns-global"
              @sort-change="onWarehouseSort"
            >
              <template #actionCell="{ record }">
                <div
                  v-if="!record.__ghost"
                  class="row-actions"
                >
                  <a
                    class="grid-link"
                    @click="openWarehouseEdit(record)"
                  >修改</a>
                  <a-popconfirm
                    title="确认删除该仓库？"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleWarehouseDelete(record)"
                  >
                    <a class="grid-link">删除</a>
                  </a-popconfirm>
                  <a
                    class="grid-link"
                    @click="handleWarehouseToggle(record)"
                  >{{ record.status === 0 ? '启用' : '停用' }}</a>
                </div>
              </template>
            </BillDetailTable>

            <BillDetailTable
              v-else
              ref="locationTableRef"
              v-model:data-source="locationRows"
              :columns="locationColumns"
              :loading="locationLoading"
              :view-mode="true"
              :fill-mode="true"
              storage-key="md-warehouse-location-columns"
              global-config-key="md-warehouse-location-columns-global"
              @checkbox-change="onLocationCheck"
              @checkbox-all="onLocationCheckAll"
              @sort-change="onLocationSort"
            >
              <template #actionCell="{ record }">
                <div
                  v-if="!record.__ghost"
                  class="row-actions"
                >
                  <a
                    class="grid-link"
                    @click="openLocationEdit(record)"
                  >修改</a>
                  <a
                    v-if="record.isBuiltin !== 1"
                    class="grid-link"
                    @click="handleLocationDelete(record)"
                  >删除</a>
                  <a
                    class="grid-link"
                    @click="handleLocationToggle(record)"
                  >{{ record.isEnabled === 0 ? '启用' : '停用' }}</a>
                </div>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 分页（对标：经典分页条） ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="activeTab === 'warehouse' ? warehousePage.current : locationPage.current"
            :page-size="activeTab === 'warehouse' ? warehousePage.pageSize : locationPage.pageSize"
            :total="activeTab === 'warehouse' ? warehousePage.total : locationPage.total"
            :page-size-options="[20, 50, 100]"
            @change="onPageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 仓库信息弹窗（对标实测 9 字段） ═══ -->
      <a-modal
        v-model:open="warehouseModal.open"
        :title="warehouseModal.editingId ? '仓库信息--编辑' : '仓库信息'"
        width="620px"
        :confirm-loading="warehouseModal.saving"
        :mask-closable="false"
        @ok="handleWarehouseSave"
      >
        <a-form
          ref="warehouseFormRef"
          :model="warehouseForm"
          :rules="warehouseRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          class="warehouse-form"
        >
          <a-form-item
            label="仓库名称"
            name="warehouseName"
          >
            <a-input
              v-model:value="warehouseForm.warehouseName"
              placeholder="请输入仓库名称"
              :maxlength="200"
            />
          </a-form-item>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="仓库编号"
                name="warehouseCode"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <a-input
                  v-model:value="warehouseForm.warehouseCode"
                  placeholder="自动生成"
                  :maxlength="50"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="所属分类"
                name="categoryId"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <a-tree-select
                  v-model:value="warehouseForm.categoryId"
                  :tree-data="categorySelectTree"
                  :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                  placeholder="请选择所属分类"
                  allow-clear
                  tree-default-expand-all
                  :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <!-- 上级仓库：对标业务规范「仓库本身支持父子层级」，配合列表「显示层次结构」 -->
          <a-form-item
            label="上级仓库"
            name="parentId"
          >
            <a-select
              v-model:value="warehouseForm.parentId"
              placeholder="顶级仓库（不选即为顶级）"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="parentWarehouseOptions"
              :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
            />
          </a-form-item>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="助记码"
                name="easyCode"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <a-input
                  v-model:value="warehouseForm.easyCode"
                  :maxlength="50"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="联系人"
                name="contactPerson"
                :label-col="{ span: 10 }"
                :wrapper-col="{ span: 14 }"
              >
                <a-input
                  v-model:value="warehouseForm.contactPerson"
                  :maxlength="100"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            label="电话"
            name="contactPhone"
          >
            <a-input
              v-model:value="warehouseForm.contactPhone"
              :maxlength="20"
            />
          </a-form-item>
          <a-form-item
            label="地址"
            name="address"
          >
            <a-input
              v-model:value="warehouseForm.address"
              :maxlength="500"
            />
          </a-form-item>
          <a-form-item
            label="邮编"
            name="zipCode"
          >
            <a-input
              v-model:value="warehouseForm.zipCode"
              :maxlength="20"
            />
          </a-form-item>
          <a-form-item
            label="备注"
            name="remark"
          >
            <a-textarea
              v-model:value="warehouseForm.remark"
              :rows="3"
              :maxlength="500"
            />
          </a-form-item>
        </a-form>
        <template #footer>
          <a-button
            type="primary"
            class="btn-orange"
            :loading="warehouseModal.saving"
            @click="handleWarehouseSave"
          >
            保存(Enter)
          </a-button>
          <a-button @click="warehouseModal.open = false">
            取消(Esc)
          </a-button>
        </template>
      </a-modal>

      <!-- ═══ 分类信息弹窗（对标实测：分类名称*/所属分类/层级编号*） ═══ -->
      <a-modal
        v-model:open="categoryModal.open"
        :title="categoryModal.editingId ? '分类信息--编辑' : '分类信息--新增'"
        width="520px"
        :confirm-loading="categoryModal.saving"
        :mask-closable="false"
        @ok="handleCategorySave"
      >
        <a-form
          ref="categoryFormRef"
          :model="categoryForm"
          :rules="categoryRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="分类名称"
            name="categoryName"
          >
            <a-input
              v-model:value="categoryForm.categoryName"
              placeholder="请输入分类名称"
              :maxlength="100"
            />
          </a-form-item>
          <a-form-item
            label="所属分类"
            name="parentId"
          >
            <a-tree-select
              v-model:value="categoryForm.parentId"
              :tree-data="categorySelectTree"
              :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
              placeholder="顶级分类"
              allow-clear
              tree-default-expand-all
            />
          </a-form-item>
          <a-form-item
            label="层级编号"
            name="categoryCode"
          >
            <a-input
              v-model:value="categoryForm.categoryCode"
              placeholder="留空自动生成"
              :maxlength="50"
            />
          </a-form-item>
        </a-form>
        <template #footer>
          <a-button
            type="primary"
            class="btn-orange"
            :loading="categoryModal.saving"
            @click="handleCategorySave"
          >
            保存(Enter)
          </a-button>
          <a-button @click="categoryModal.open = false">
            关闭(Esc)
          </a-button>
        </template>
      </a-modal>

      <!-- ═══ 货位弹窗（对标实测：批量生成货位编号） ═══ -->
      <a-modal
        v-model:open="locationModal.open"
        :title="locationModal.editingId ? '货位--编辑' : '货位'"
        width="560px"
        :confirm-loading="locationModal.saving"
        :mask-closable="false"
        @ok="handleLocationSave"
      >
        <a-form
          v-if="!locationModal.editingId"
          ref="locationFormRef"
          :model="locationForm"
          :rules="locationRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
        >
          <a-form-item
            label="仓库"
            name="warehouseId"
          >
            <a-select
              v-model:value="locationForm.warehouseId"
              placeholder="请选择仓库"
              show-search
              option-filter-prop="label"
              :options="warehouseOptions"
              :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
            />
          </a-form-item>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="通道号"
                name="channelNo"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input
                  v-model:value="locationForm.channelNo"
                  :maxlength="10"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="生成数"
                name="channelCount"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input-number
                  v-model:value="locationForm.channelCount"
                  :min="1"
                  :max="99"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="货架号"
                name="shelfNo"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input
                  v-model:value="locationForm.shelfNo"
                  :maxlength="10"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="生成数"
                name="shelfCount"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input-number
                  v-model:value="locationForm.shelfCount"
                  :min="1"
                  :max="99"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            label="货架层数"
            name="layerCount"
          >
            <a-input-number
              v-model:value="locationForm.layerCount"
              :min="1"
              :max="99"
              placeholder="一位数字，如：1"
              style="width: 100%"
            />
          </a-form-item>
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="货位列号"
                name="columnNo"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input
                  v-model:value="locationForm.columnNo"
                  :maxlength="10"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="生成数"
                name="columnCount"
                :label-col="{ span: 9 }"
                :wrapper-col="{ span: 15 }"
              >
                <a-input-number
                  v-model:value="locationForm.columnCount"
                  :min="1"
                  :max="99"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            label="备注"
            name="remark"
          >
            <a-input
              v-model:value="locationForm.remark"
              :maxlength="500"
            />
          </a-form-item>
        </a-form>
        <div
          v-if="!locationModal.editingId"
          class="location-tip"
        >
          <div class="tip-title">
            说明：
          </div>
          <div class="tip-line">
            系统生成货位编号的格式如下：
          </div>
          <div class="tip-format">
            <span class="fmt-col">
              <span class="fmt-label">通道及货架</span>
              <span class="fmt-arrow">↓</span>
              <span class="fmt-value">{{ locationPreview.channelShelf }}</span>
            </span>
            <span class="fmt-dash">-</span>
            <span class="fmt-col">
              <span class="fmt-label">货架层与列</span>
              <span class="fmt-arrow">↓</span>
              <span class="fmt-value">{{ locationPreview.layerColumn }}</span>
            </span>
          </div>
          <div class="tip-line">
            生成数量=通道生成数 * 货架生成数 *货架层数 * 货位列号生成数 = <b>{{ locationPreview.total }}</b>
          </div>
          <div class="tip-line">
            建议货架号为奇数的货位全部放置在通道左侧，货架号为偶数的货位全部放置在通道右侧
          </div>
        </div>

        <!-- 编辑态：编号与仓库不可改，仅维护备注 -->
        <a-form
          v-else
          :model="locationEditForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
        >
          <a-form-item label="仓库">
            <a-input
              :value="locationEditForm.warehouseName"
              disabled
            />
          </a-form-item>
          <a-form-item label="货位编号">
            <a-input
              :value="locationEditForm.locationCode"
              disabled
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="locationEditForm.remark"
              :maxlength="500"
            />
          </a-form-item>
        </a-form>
        <template #footer>
          <a-button
            type="primary"
            class="btn-orange"
            :loading="locationModal.saving"
            @click="handleLocationSave"
          >
            保存(Enter)
          </a-button>
          <a-button @click="locationModal.open = false">
            取消(Esc)
          </a-button>
        </template>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  CloseOutlined,
  DeleteOutlined,
  DownOutlined,
  DownloadOutlined,
  EditOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { warehousePlanApi, warehouseCategoryApi, warehouseLocationApi, type ErpWarehouse, type WarehouseCategory } from '@/api/erp/warehousePlan'

defineOptions({ name: 'MdWarehousePlan' })

// ═══ Tab（对标：1.仓库 / 2.货位） ═══
type TabKey = 'warehouse' | 'location'
const tabs = [
  { key: 'warehouse', label: '1.仓库' },
  { key: 'location', label: '2.货位' },
]
const activeTab = ref<TabKey>('warehouse')

function switchTab(tab: string) {
  const next: TabKey = tab === 'location' ? 'location' : 'warehouse'
  if (next === activeTab.value) return
  activeTab.value = next
  if (next === 'warehouse') {
    if (!warehouseRows.value.length) fetchWarehouses()
  } else if (!locationRows.value.length) {
    fetchLocations()
  }
}

// ═══ 左侧仓库分类树 ═══
const ROOT_ID = '0'
const categoryLoading = ref(false)
const rawCategories = ref<WarehouseCategory[]>([])
const categoryFlatMap = ref<Record<string, WarehouseCategory>>({})
const selectedCategoryId = ref<string>(ROOT_ID)
const expandedKeys = ref<string[]>([ROOT_ID])
const categoryTreeData = ref<any[]>([{ id: ROOT_ID, categoryName: '全部' }])

const categorySelectTree = computed(() => [{ id: ROOT_ID, categoryName: '全部', children: toSelectNodes(rawCategories.value) }])

function toSelectNodes(nodes: WarehouseCategory[]): any[] {
  return (nodes || []).map(n => ({
    id: String(n.id),
    categoryName: n.categoryName,
    children: n.children?.length ? toSelectNodes(n.children) : undefined,
  }))
}

function normalizeCategories(nodes: WarehouseCategory[]): any[] {
  return (nodes || []).map(n => {
    const node: any = {
      id: String(n.id),
      categoryName: n.categoryName,
      categoryCode: n.categoryCode,
      parentId: n.parentId != null ? String(n.parentId) : ROOT_ID,
      warehouseCount: n.warehouseCount,
    }
    if (n.children?.length) node.children = normalizeCategories(n.children)
    return node
  })
}

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const data: any = await warehouseCategoryApi.tree()
    const list = Array.isArray(data) ? data : data?.records || []
    rawCategories.value = list
    const tree = normalizeCategories(list)
    categoryTreeData.value = [{ id: ROOT_ID, categoryName: '全部', children: tree.length ? tree : undefined }]
    const flat: Record<string, WarehouseCategory> = {}
    const walk = (nodes: any[]) => nodes.forEach(n => {
      flat[String(n.id)] = n
      if (n.children?.length) walk(n.children)
    })
    walk(tree)
    categoryFlatMap.value = flat
    expandedKeys.value = [ROOT_ID, ...tree.map((n: any) => n.id)]
  } catch (e) {
    console.warn('[仓库规划] 分类树加载失败', e)
    categoryTreeData.value = [{ id: ROOT_ID, categoryName: '全部' }]
  } finally {
    categoryLoading.value = false
  }
}

const currentPath = computed(() => {
  if (selectedCategoryId.value === ROOT_ID) return '全部'
  const parts: string[] = []
  let cursor: string | undefined = selectedCategoryId.value
  let guard = 0
  while (cursor && cursor !== ROOT_ID && guard++ < 20) {
    const node = categoryFlatMap.value[cursor]
    if (!node) break
    parts.unshift(node.categoryName)
    cursor = node.parentId != null ? String(node.parentId) : ROOT_ID
  }
  return ['全部', ...parts].join(' / ')
})

function onCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys.length ? String(keys[0]) : ROOT_ID
  warehousePage.current = 1
  fetchWarehouses()
}

function onCategoryExpand(keys: any[]) {
  expandedKeys.value = keys.map(String)
}

// ═══ Tab1 仓库列表 ═══
const warehouseTableRef = ref()
const warehouseLoading = ref(false)
const warehouseRows = ref<ErpWarehouse[]>([])
const warehouseQuery = reactive({ keyword: '', showDisabled: false, showHierarchy: true })
const warehousePage = reactive({ current: 1, pageSize: 20, total: 0 })

const warehouseColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 46 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  // 对标实测：仓库编号 / 仓库名称 表头带排序箭头
  { key: 'warehouseCode', title: '仓库编号', type: 'input', width: 150, sortable: true },
  { key: 'warehouseName', title: '仓库名称', type: 'input', width: 220, sortable: true, formatter: (v, record) => (record.status === 0 ? `${v ?? ''}（停用）` : (v ?? '')) },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 120 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 150 },
  { key: 'address', title: '地址', type: 'input', width: 320 },
]

async function fetchWarehouses() {
  warehouseLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: warehousePage.current,
      pageSize: warehousePage.pageSize,
      showDisabled: warehouseQuery.showDisabled,
      showHierarchy: warehouseQuery.showHierarchy,
    }
    if (warehouseQuery.keyword) params.keyword = warehouseQuery.keyword.trim()
    if (selectedCategoryId.value !== ROOT_ID) params.categoryId = selectedCategoryId.value
    const res: any = await warehousePlanApi.page(params)
    warehouseRows.value = res?.records || []
    warehousePage.total = Number(res?.total || 0)
  } catch (e) {
    console.warn('[仓库规划] 仓库列表加载失败', e)
    warehouseRows.value = []
    warehousePage.total = 0
  } finally {
    warehouseLoading.value = false
  }
}

// ═══ Tab2 货位列表 ═══
const locationTableRef = ref()
const locationLoading = ref(false)
const locationRows = ref<any[]>([])
const locationQuery = reactive({ warehouseKeyword: '', locationCode: '', showDisabled: false })
const locationPage = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedLocationIds = ref<Array<string | number>>([])

const locationColumns: DetailColumnConfig[] = [
  { key: 'rowCheck', title: '', type: 'checkbox', width: 44 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  // 对标实测：所属仓库 / 货位编号 表头带排序箭头
  { key: 'warehouseName', title: '所属仓库', type: 'input', width: 220, sortable: true },
  { key: 'locationCode', title: '货位编号', type: 'input', width: 220, sortable: true },
  { key: 'remark', title: '备注', type: 'input', width: 360 },
]

async function fetchLocations() {
  locationLoading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: locationPage.current,
      pageSize: locationPage.pageSize,
      showDisabled: locationQuery.showDisabled,
    }
    if (locationQuery.warehouseKeyword) params.warehouseKeyword = locationQuery.warehouseKeyword.trim()
    if (locationQuery.locationCode) params.locationCode = locationQuery.locationCode.trim()
    const res: any = await warehouseLocationApi.page(params)
    locationRows.value = res?.records || []
    locationPage.total = Number(res?.total || 0)
    selectedLocationIds.value = []
  } catch (e) {
    console.warn('[仓库规划] 货位列表加载失败', e)
    locationRows.value = []
    locationPage.total = 0
  } finally {
    locationLoading.value = false
  }
}

function onLocationCheck(record: any) {
  const id = String(record.id)
  const idx = selectedLocationIds.value.findIndex(v => String(v) === id)
  if (idx >= 0) selectedLocationIds.value.splice(idx, 1)
  else selectedLocationIds.value.push(record.id)
}
function onLocationCheckAll(checked: boolean, records: any[]) {
  selectedLocationIds.value = checked ? records.filter(r => !r.__ghost).map(r => r.id) : []
}

// ═══ 表头排序（对标：仓库编号/仓库名称、所属仓库/货位编号 带排序箭头）═══
function applyLocalSort(rows: any[], key: string | null, order: string | null) {
  if (!key || !order) return [...rows]
  return [...rows].sort((a, b) => {
    const x = a[key]
    const y = b[key]
    const r = typeof x === 'number' && typeof y === 'number'
      ? x - y
      : String(x ?? '').localeCompare(String(y ?? ''), 'zh-Hans-CN')
    return order === 'asc' ? r : -r
  })
}
function onWarehouseSort(key: string | null, order: string | null) {
  warehouseRows.value = applyLocalSort(warehouseRows.value, key, order)
}
function onLocationSort(key: string | null, order: string | null) {
  locationRows.value = applyLocalSort(locationRows.value, key, order)
}

// ═══ 查询 / 刷新 / 分页 / 快捷操作 ═══
function handleSearch() {
  if (activeTab.value === 'warehouse') {
    warehousePage.current = 1
    fetchWarehouses()
  } else {
    locationPage.current = 1
    fetchLocations()
  }
}

function handleRefresh() {
  if (activeTab.value === 'warehouse') {
    fetchCategoryTree()
    fetchWarehouses()
  } else {
    fetchLocations()
  }
}

function onPageChange(current: number, pageSize: number) {
  if (activeTab.value === 'warehouse') {
    warehousePage.current = current
    warehousePage.pageSize = pageSize
    fetchWarehouses()
  } else {
    locationPage.current = current
    locationPage.pageSize = pageSize
    fetchLocations()
  }
}

// ═══ 仓库信息弹窗 ═══
const warehouseFormRef = ref()
const warehouseModal = reactive({ open: false, saving: false, editingId: '' as string })
const emptyWarehouseForm = () => ({
  warehouseName: '',
  warehouseCode: '',
  categoryId: ROOT_ID as string,
  parentId: undefined as string | undefined,
  easyCode: '',
  contactPerson: '',
  contactPhone: '',
  address: '',
  zipCode: '',
  remark: '',
})
const warehouseForm = reactive(emptyWarehouseForm())
const warehouseRules: Record<string, any> = {
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  warehouseCode: [{ required: true, message: '请输入仓库编号', trigger: 'blur' }],
}

async function openWarehouseCreate() {
  Object.assign(warehouseForm, emptyWarehouseForm())
  warehouseModal.editingId = ''
  if (selectedCategoryId.value !== ROOT_ID) warehouseForm.categoryId = selectedCategoryId.value
  fetchWarehouseOptions()
  try {
    const code: any = await warehousePlanApi.nextCode()
    warehouseForm.warehouseCode = typeof code === 'string' ? code : (code?.data || '')
  } catch {
    // 编号接口不可用时不阻塞新增（保存后端会自动补号）
  }
  warehouseModal.open = true
}

async function openWarehouseEdit(record: any) {
  warehouseModal.editingId = String(record.id)
  Object.assign(warehouseForm, emptyWarehouseForm())
  fetchWarehouseOptions()
  try {
    const detail: any = await warehousePlanApi.getById(record.id)
    const d = detail || record
    Object.assign(warehouseForm, {
      warehouseName: d.warehouseName || '',
      warehouseCode: d.warehouseCode || '',
      categoryId: d.categoryId != null && String(d.categoryId) !== '0' ? String(d.categoryId) : ROOT_ID,
      parentId: d.parentId != null && String(d.parentId) !== '0' ? String(d.parentId) : undefined,
      easyCode: d.easyCode || '',
      contactPerson: d.contactPerson || '',
      contactPhone: d.contactPhone || '',
      address: d.address || '',
      zipCode: d.zipCode || '',
      remark: d.remark || '',
    })
  } catch {
    Object.assign(warehouseForm, {
      warehouseName: record.warehouseName || '',
      warehouseCode: record.warehouseCode || '',
      categoryId: record.categoryId != null && String(record.categoryId) !== '0' ? String(record.categoryId) : ROOT_ID,
      parentId: record.parentId != null && String(record.parentId) !== '0' ? String(record.parentId) : undefined,
      easyCode: record.easyCode || '',
      contactPerson: record.contactPerson || '',
      contactPhone: record.contactPhone || '',
      address: record.address || '',
      zipCode: record.zipCode || '',
      remark: record.remark || '',
    })
  }
  warehouseModal.open = true
}

async function handleWarehouseSave() {
  try {
    await warehouseFormRef.value?.validate()
  } catch {
    return
  }
  warehouseModal.saving = true
  try {
    const payload: any = {
      warehouseName: warehouseForm.warehouseName.trim(),
      warehouseCode: warehouseForm.warehouseCode.trim(),
      easyCode: warehouseForm.easyCode || null,
      contactPerson: warehouseForm.contactPerson || null,
      contactPhone: warehouseForm.contactPhone || null,
      address: warehouseForm.address || null,
      zipCode: warehouseForm.zipCode || null,
      remark: warehouseForm.remark || null,
      categoryId: warehouseForm.categoryId && warehouseForm.categoryId !== ROOT_ID ? warehouseForm.categoryId : '0',
      parentId: warehouseForm.parentId || '0',
    }
    if (warehouseModal.editingId) {
      payload.id = warehouseModal.editingId
      await warehousePlanApi.update(payload)
      message.success('仓库已更新')
    } else {
      await warehousePlanApi.save(payload)
      message.success('仓库已新增')
    }
    warehouseModal.open = false
    fetchCategoryTree()
    fetchWarehouses()
    fetchWarehouseOptions()
  } catch (e: any) {
    console.warn('[仓库规划] 仓库保存失败', e)
  } finally {
    warehouseModal.saving = false
  }
}

async function handleWarehouseDelete(record: any) {
  try {
    await warehousePlanApi.remove(record.id)
    message.success('删除成功')
    fetchCategoryTree()
    fetchWarehouses()
  } catch (e) {
    console.warn('[仓库规划] 仓库删除失败', e)
  }
}

async function handleWarehouseToggle(record: any) {
  const target = record.status === 0 ? 1 : 0
  try {
    await warehousePlanApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    fetchWarehouses()
  } catch (e) {
    console.warn('[仓库规划] 状态切换失败', e)
  }
}

// ═══ 分类信息弹窗 ═══
const categoryFormRef = ref()
const categoryModal = reactive({ open: false, saving: false, editingId: '' as string })
const emptyCategoryForm = () => ({ categoryName: '', parentId: ROOT_ID as string, categoryCode: '' })
const categoryForm = reactive(emptyCategoryForm())
const categoryRules: Record<string, any> = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openCategoryCreate() {
  Object.assign(categoryForm, emptyCategoryForm())
  categoryForm.parentId = selectedCategoryId.value
  categoryModal.editingId = ''
  categoryModal.open = true
}

function openCategoryEdit() {
  if (selectedCategoryId.value === ROOT_ID) {
    message.warning('请先选择要修改的分类')
    return
  }
  const node: any = categoryFlatMap.value[selectedCategoryId.value]
  if (!node) return
  categoryModal.editingId = String(node.id)
  Object.assign(categoryForm, {
    categoryName: node.categoryName || '',
    parentId: node.parentId != null ? String(node.parentId) : ROOT_ID,
    categoryCode: node.categoryCode || '',
  })
  categoryModal.open = true
}

async function handleCategorySave() {
  try {
    await categoryFormRef.value?.validate()
  } catch {
    return
  }
  categoryModal.saving = true
  try {
    const payload = {
      categoryName: categoryForm.categoryName.trim(),
      parentId: categoryForm.parentId && categoryForm.parentId !== ROOT_ID ? categoryForm.parentId : '0',
      categoryCode: categoryForm.categoryCode || null,
    }
    if (categoryModal.editingId) {
      await warehouseCategoryApi.update(categoryModal.editingId, payload)
      message.success('分类已更新')
    } else {
      await warehouseCategoryApi.create(payload as any)
      message.success('分类已新增')
    }
    categoryModal.open = false
    fetchCategoryTree()
  } catch (e) {
    console.warn('[仓库规划] 分类保存失败', e)
  } finally {
    categoryModal.saving = false
  }
}

async function handleCategoryDelete() {
  if (selectedCategoryId.value === ROOT_ID) {
    message.warning('请先选择要删除的分类')
    return
  }
  try {
    await warehouseCategoryApi.remove(selectedCategoryId.value)
    message.success('分类已删除')
    selectedCategoryId.value = ROOT_ID
    fetchCategoryTree()
    fetchWarehouses()
  } catch (e) {
    console.warn('[仓库规划] 分类删除失败', e)
  }
}

// ═══ 货位弹窗（新增=批量生成 / 编辑=仅备注） ═══
const locationFormRef = ref()
const locationModal = reactive({ open: false, saving: false, editingId: '' as string })
const emptyLocationForm = () => ({
  warehouseId: undefined as string | undefined,
  channelNo: 'A',
  channelCount: 1,
  shelfNo: '01',
  shelfCount: 1,
  layerCount: 1,
  columnNo: '01',
  columnCount: 1,
  remark: '',
})
const locationForm = reactive(emptyLocationForm())
const locationEditForm = reactive({ warehouseName: '', locationCode: '', remark: '' })
const locationRules: Record<string, any> = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  channelNo: [{ required: true, message: '请输入通道号', trigger: 'blur' }],
  shelfNo: [{ required: true, message: '请输入货架号', trigger: 'blur' }],
  layerCount: [{ required: true, message: '请输入货架层数', trigger: 'blur' }],
  columnNo: [{ required: true, message: '请输入货位列号', trigger: 'blur' }],
}

/** 仓库下拉（启用中的仓库）；货位弹窗与「上级仓库」共用，打开弹窗前会重取一次 */
const warehouseOptions = ref<Array<{ label: string; value: string }>>([])
/** 上级仓库候选：排除自身（后端另有自引用/成环校验） */
const parentWarehouseOptions = computed(() =>
  warehouseOptions.value.filter(o => o.value !== String(warehouseModal.editingId || ''))
)
async function fetchWarehouseOptions() {
  try {
    const list: any = await warehousePlanApi.listAll()
    warehouseOptions.value = (Array.isArray(list) ? list : [])
      .filter((w: any) => w.status !== 0)
      .map((w: any) => ({ label: `${w.warehouseName}（${w.warehouseCode}）`, value: String(w.id) }))
  } catch {
    warehouseOptions.value = []
  }
}

const locationPreview = computed(() => {
  const channel = (locationForm.channelNo || 'A').trim()
  const shelf = (locationForm.shelfNo || '1').trim().replace(/^0+(?=\d)/, '')
  const layer = locationForm.layerCount || 1
  const column = (locationForm.columnNo || '1').trim()
  const total = (locationForm.channelCount || 1) * (locationForm.shelfCount || 1) * (locationForm.layerCount || 1) * (locationForm.columnCount || 1)
  return { channelShelf: `${channel}${shelf}`, layerColumn: `${layer}${column}`, total }
})

async function openLocationCreate() {
  Object.assign(locationForm, emptyLocationForm())
  locationModal.editingId = ''
  // 每次打开都重取一次，保证刚新增/停用的仓库状态即时反映到下拉
  await fetchWarehouseOptions()
  locationModal.open = true
}

async function openLocationEdit(record: any) {
  locationModal.editingId = String(record.id)
  locationEditForm.warehouseName = record.warehouseName || ''
  locationEditForm.locationCode = record.locationCode || ''
  locationEditForm.remark = record.remark || ''
  locationModal.open = true
}

async function handleLocationSave() {
  if (locationModal.editingId) {
    locationModal.saving = true
    try {
      await warehouseLocationApi.update({ id: locationModal.editingId, remark: locationEditForm.remark || null } as any)
      message.success('货位已更新')
      locationModal.open = false
      fetchLocations()
    } catch (e) {
      console.warn('[仓库规划] 货位更新失败', e)
    } finally {
      locationModal.saving = false
    }
    return
  }
  try {
    await locationFormRef.value?.validate()
  } catch {
    return
  }
  locationModal.saving = true
  try {
    const selected = warehouseOptions.value.find(o => o.value === String(locationForm.warehouseId))
    const created: any = await warehouseLocationApi.generate({
      warehouseId: locationForm.warehouseId as string,
      warehouseName: selected ? String(selected.label).split('（')[0] : undefined,
      channelNo: locationForm.channelNo,
      channelCount: locationForm.channelCount,
      shelfNo: locationForm.shelfNo,
      shelfCount: locationForm.shelfCount,
      layerCount: locationForm.layerCount,
      columnNo: locationForm.columnNo,
      columnCount: locationForm.columnCount,
      remark: locationForm.remark || undefined,
    })
    const count = Number(created?.data ?? created ?? 0)
    message.success(count > 0 ? `已生成 ${count} 个货位` : '所选货位已存在，未生成新记录')
    locationModal.open = false
    fetchLocations()
  } catch (e) {
    console.warn('[仓库规划] 货位生成失败', e)
  } finally {
    locationModal.saving = false
  }
}

async function handleLocationDelete(record: any) {
  if (record.isBuiltin === 1) {
    message.warning('系统内置货位不可删除')
    return
  }
  try {
    await warehouseLocationApi.remove(record.id)
    message.success('删除成功')
    fetchLocations()
  } catch (e) {
    console.warn('[仓库规划] 货位删除失败', e)
  }
}

async function handleLocationToggle(record: any) {
  const target = record.isEnabled === 0 ? 1 : 0
  try {
    await warehouseLocationApi.setEnabled(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    fetchLocations()
  } catch (e) {
    console.warn('[仓库规划] 货位状态切换失败', e)
  }
}

async function handleBatchDelete() {
  if (!selectedLocationIds.value.length) {
    message.warning('请先勾选要删除的货位')
    return
  }
  try {
    const removed: any = await warehouseLocationApi.batchDelete(selectedLocationIds.value)
    const count = Number(removed?.data ?? removed ?? 0)
    message.success(`已删除 ${count} 个货位`)
    fetchLocations()
  } catch (e) {
    console.warn('[仓库规划] 货位批量删除失败', e)
  }
}

function onMoreMenuClick({ key }: any) {
  if (key === 'export') handleExport()
}

// ═══ 导出（真实 xlsx） ═══
const exporting = ref(false)
async function handleExport() {
  exporting.value = true
  try {
    const isWarehouse = activeTab.value === 'warehouse'
    const params: Record<string, any> = isWarehouse
      ? {
        keyword: warehouseQuery.keyword || undefined,
        showDisabled: warehouseQuery.showDisabled,
        showHierarchy: warehouseQuery.showHierarchy,
        categoryId: selectedCategoryId.value !== ROOT_ID ? selectedCategoryId.value : undefined,
      }
      : {
        warehouseKeyword: locationQuery.warehouseKeyword || undefined,
        locationCode: locationQuery.locationCode || undefined,
        showDisabled: locationQuery.showDisabled,
      }
    const blob: any = isWarehouse ? await warehousePlanApi.export(params) : await warehouseLocationApi.export(params)
    if (!(blob instanceof Blob) || blob.size === 0) {
      message.warning('暂无可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${isWarehouse ? '仓库规划' : '货位'}_${dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.warn('[仓库规划] 导出失败', e)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8) ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const isWarehouse = activeTab.value === 'warehouse'
  const rows = isWarehouse ? warehouseRows.value : locationRows.value
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const title = isWarehouse ? '仓库规划' : '货位'
  const bodyRows = isWarehouse
    ? warehouseRows.value.map((r, i) => `
      <tr>
        <td>${i + 1}</td>
        <td>${escapeHtml(r.warehouseCode)}</td>
        <td>${escapeHtml(r.warehouseName)}</td>
        <td>${escapeHtml(r.contactPerson)}</td>
        <td>${escapeHtml(r.contactPhone)}</td>
        <td>${escapeHtml(r.address)}</td>
      </tr>`).join('')
    : locationRows.value.map((r, i) => `
      <tr>
        <td>${i + 1}</td>
        <td>${escapeHtml(r.warehouseName)}</td>
        <td>${escapeHtml(r.locationCode)}</td>
        <td>${escapeHtml(r.remark)}</td>
      </tr>`).join('')
  const head = isWarehouse
    ? '<th>#</th><th>仓库编号</th><th>仓库名称</th><th>联系人</th><th>联系电话</th><th>地址</th>'
    : '<th>#</th><th>所属仓库</th><th>货位编号</th><th>备注</th>'
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>${title}</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>${title}</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table><thead><tr>${head}</tr></thead><tbody>${bodyRows}</tbody></table>
    </body></html>`
  const win = window.open('', '_blank', 'width=1000,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchCategoryTree()
  fetchWarehouses()
  fetchWarehouseOptions()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ── 查询区（slot 内容带本组件 scope，须在此定义） ── */
.search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
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
  margin-left: 4px;
}

.btn-orange {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-orange:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* 表格区：BillDetailTable 依赖 flex 链撑高 */
.table-section {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 行内操作链接（对标：修改 删除 停用） */
.row-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
.grid-link {
  color: #1677ff;
  cursor: pointer;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}
.grid-link:hover {
  color: #ff6b35;
}

/* 货位弹窗说明区 */
.location-tip {
  margin-top: 4px;
  padding: 10px 12px;
  background: #fff8f3;
  border: 1px dashed #ffd8bf;
  border-radius: 4px;
  font-size: 12px;
  color: #d46b08;
  line-height: 1.9;
}
.location-tip .tip-title {
  font-weight: 600;
}
.location-tip .tip-format {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 10px;
  margin: 6px 0;
  color: #333;
}
.location-tip .fmt-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.location-tip .fmt-label {
  font-size: 12px;
  color: #8c8c8c;
}
.location-tip .fmt-arrow {
  color: #d46b08;
}
.location-tip .fmt-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}
.location-tip .fmt-dash {
  font-size: 18px;
  padding-bottom: 4px;
}

/* 仓库表单：两列布局下的标签对齐 */
.warehouse-form :deep(.ant-form-item) {
  margin-bottom: 12px;
}
</style>
