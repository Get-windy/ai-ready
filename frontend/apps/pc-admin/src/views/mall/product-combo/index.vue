<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商品组合（交易 → 商城 → 基础业务 → 商品组合，对标 ql361「商城 → 基础业务 → 商品组合」）
        · 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/商品组合开发文档.md
        · 页面定位：单入口单视图列表页（商城组合商品/套餐管理，无子 Tab）；对标无「页面配置」弹窗（查询区/按钮固定）
        · 列配置：走 BillDetailTable 表头 rowNo 列齿轮（个人配置 / 全局配置），storage-key 持久化
        · 列数：可配置 9 列 = 对标 3 列（组合名称 / 编号 / 商品明细，全默认显示）
                              + 本系统扩展 6 列（状态 默认显示；类型 / 组合价 / 组合成本 / 利润率 / 创建时间 默认隐藏）
          默认显示 4 列、默认隐藏 5 列；固定列（非列配置）：序号、多选勾选框、操作
        · 工具栏：新增组合 / 启用 / 停用 / 批量删除 ｜ 刷新 / 打印(F8) / 导出
        · 行级操作：详情 / 编辑 / 复制 / 启用停用 / 删除
        · 数据来源：product_kit + product_kit_item（API /erp/product-kit：page/items/create/update/copy/activate/deactivate/delete）
        · 删除端点（后端 ProductKitController 已补齐）：
          1) `DELETE /erp/product-kit/{id}` — 级联逻辑删除组件行（行级「删除」）
          2) `DELETE /erp/product-kit/batch` — body `{"ids":[…]}`（工具栏「批量删除」）
        · 批量启停端点（后端 ProductKitController 已补齐，一次请求）：
          1) `POST /erp/product-kit/batch-activate` — body `{"ids":[…]}`（工具栏「启用」）
          2) `POST /erp/product-kit/batch-deactivate` — body `{"ids":[…]}`（工具栏「停用」）
          均返回实际更新条数（listByIds 受租户与逻辑删除约束，口径同 batch 删除）。
        · 剩余缺口：对标「商品明细」列为组成商品子表入口；本系统经「详情抽屉」展示子表（对标子表结构未实测）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 + 启用 / 停用 / 批量删除 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              type="primary"
              size="small"
              class="btn-add"
              @click="openCreate"
            >
              <PlusOutlined /> 新增组合
            </a-button>
            <a-button
              size="small"
              :loading="batchLoading"
              :disabled="!selectedIds.length"
              @click="handleBatchActive(true)"
            >
              启用
            </a-button>
            <a-button
              size="small"
              :loading="batchLoading"
              :disabled="!selectedIds.length"
              @click="handleBatchActive(false)"
            >
              停用
            </a-button>
            <a-button
              size="small"
              danger
              :loading="batchLoading"
              :disabled="!selectedIds.length"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 批量删除
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="handleRefresh"
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
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项：组合名称、显示状态；组合类型为本系统扩展） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">组合名称</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="组合编码/名称"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
              <a-select
                v-model:value="searchForm.kitType"
                placeholder="全部类型"
                size="small"
                style="width: 140px"
                allow-clear
                :options="kitTypeOptions"
                @change="handleSearch"
              />
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="searchForm.status"
                placeholder="全部"
                size="small"
                style="width: 130px"
                allow-clear
                :options="statusOptions"
                @change="handleSearch"
              />
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="mall-product-combo-table-columns"
              global-config-key="mall-product-combo-table-columns"
              @checkbox-change="handleRowCheck"
              @checkbox-all="handleRowCheckAll"
            >
              <!-- 组合名称：点击查看详情 -->
              <template #kitNameCell="{ record, column }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record as ProductKit)"
                >{{ record[column.key] || '-' }}</a>
              </template>

              <!-- 编号 -->
              <template #kitCodeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.kitCode || '-' }}</span>
              </template>

              <!-- 商品明细：组成商品子表入口（经详情抽屉展示，见文件头缺口说明） -->
              <template #itemsCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record as ProductKit)"
                >查看明细</a>
              </template>

              <!-- 状态：已启用 / 已停用（active 由 activate/deactivate 切换） -->
              <template #activeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.active ? 'green' : 'default'"
                >
                  {{ record.active ? '已启用' : '已停用' }}
                </a-tag>
              </template>

              <!-- 类型 -->
              <template #kitTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  color="blue"
                >
                  {{ KIT_TYPE_MAP[record.kitType ?? -1]?.label || '-' }}
                </a-tag>
              </template>

              <!-- 组合价 / 组合成本 -->
              <template #kitPriceCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.kitPrice) }}</span>
              </template>
              <template #kitCostCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.kitCost) }}</span>
              </template>

              <!-- 利润率 -->
              <template #profitRateCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.profitRate === null || record.profitRate === undefined ? '-' : Number(record.profitRate).toFixed(2) + '%' }}
                </span>
              </template>

              <!-- 创建时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列（对标：行级 编辑/查看；本系统另有 复制 / 启停用） -->
              <template #actionCell="{ record, column }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                  :data-col="column.key"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openDetail(record as ProductKit)"
                  >
                    详情
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record as ProductKit)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    :title="`确认复制组合「${record.kitName}」？`"
                    ok-text="复制"
                    cancel-text="取消"
                    @confirm="handleCopy(record as ProductKit)"
                  >
                    <a-button
                      type="link"
                      size="small"
                    >
                      复制
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    type="link"
                    size="small"
                    :style="record.active ? 'color: #fa8c16' : 'color: #52c41a'"
                    @click="toggleActive(record as ProductKit)"
                  >
                    {{ record.active ? '停用' : '启用' }}
                  </a-button>
                  <a-popconfirm
                    :title="`确认删除组合「${record.kitName}」？删除后不可恢复`"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDelete(record as ProductKit)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
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

      <!-- ═══ 组合详情抽屉（组合组件子表：对标「商品明细」列入口） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        :title="'组合详情 - ' + (detailData?.kitName || '')"
        width="720"
      >
        <a-descriptions
          v-if="detailData"
          bordered
          :column="2"
          size="small"
        >
          <a-descriptions-item label="组合编码">
            {{ detailData.kitCode }}
          </a-descriptions-item>
          <a-descriptions-item label="类型">
            {{ KIT_TYPE_MAP[detailData.kitType ?? -1]?.label || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="组合名称">
            {{ detailData.kitName }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="detailData.active ? 'green' : 'default'">
              {{ detailData.active ? '已启用' : '已停用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="组合价">
            ¥{{ formatMoney(detailData.kitPrice) }}
          </a-descriptions-item>
          <a-descriptions-item label="组合成本">
            ¥{{ formatMoney(detailData.kitCost) }}
          </a-descriptions-item>
          <a-descriptions-item label="成品编码">
            {{ detailData.productCode || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="成品名称">
            {{ detailData.productName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="组合说明"
            :span="2"
          >
            {{ detailData.description || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>组合组件</a-divider>
        <a-table
          :data-source="detailItems"
          :columns="detailItemColumns"
          :pagination="false"
          size="small"
          row-key="id"
          :loading="detailLoading"
          :locale="{ emptyText: '暂无组件' }"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="['quantity'].includes(column.dataIndex as string)">
              {{ formatQty(text) }}
            </template>
            <template v-else-if="['unitCost', 'lineCost'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
          </template>
        </a-table>
      </a-drawer>

      <!-- ═══ 新增/编辑组合弹窗（含组合组件子表） ═══ -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑商品组合' : '新增商品组合'"
        :confirm-loading="saving"
        width="860px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="组合名称"
                name="kitName"
                :label-col="{ span: 8 }"
              >
                <a-input
                  v-model:value="form.kitName"
                  placeholder="如：办公套装A"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="组合类型"
                name="kitType"
                :label-col="{ span: 8 }"
              >
                <a-select
                  v-model:value="form.kitType"
                  :options="kitTypeOptions"
                  placeholder="请选择类型"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="成品编码"
                name="productCode"
                :label-col="{ span: 8 }"
              >
                <a-input
                  v-model:value="form.productCode"
                  placeholder="组合对应的成品商品编码"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="成品名称"
                name="productName"
                :label-col="{ span: 8 }"
              >
                <a-input
                  v-model:value="form.productName"
                  placeholder="组合对应的成品商品名称"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="组合价"
                name="kitPrice"
                :label-col="{ span: 8 }"
              >
                <a-input-number
                  v-model:value="form.kitPrice"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="单位"
                name="productUnit"
                :label-col="{ span: 8 }"
              >
                <a-input
                  v-model:value="form.productUnit"
                  placeholder="如：套"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="允许拆分"
                name="allowSplit"
                :label-col="{ span: 8 }"
              >
                <a-switch v-model:checked="form.allowSplit" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="允许部分发货"
                name="allowPartial"
                :label-col="{ span: 8 }"
              >
                <a-switch v-model:checked="form.allowPartial" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="组合说明"
                name="description"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 19 }"
              >
                <a-textarea
                  v-model:value="form.description"
                  :rows="2"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>

        <a-divider orientation="left">
          组合组件
        </a-divider>
        <a-table
          :data-source="form.items"
          :columns="editItemColumns"
          :pagination="false"
          size="small"
          :row-key="(_: any, i: number) => String(i)"
          :locale="{ emptyText: '请添加组件商品' }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'componentProductCode'">
              <a-input
                v-model:value="record.componentProductCode"
                size="small"
                placeholder="商品编码"
              />
            </template>
            <template v-else-if="column.dataIndex === 'componentProductName'">
              <a-input
                v-model:value="record.componentProductName"
                size="small"
                placeholder="商品名称"
              />
            </template>
            <template v-else-if="column.dataIndex === 'componentProductUnit'">
              <a-input
                v-model:value="record.componentProductUnit"
                size="small"
                placeholder="单位"
              />
            </template>
            <template v-else-if="column.dataIndex === 'quantity'">
              <a-input-number
                v-model:value="record.quantity"
                size="small"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </template>
            <template v-else-if="column.dataIndex === 'unitCost'">
              <a-input-number
                v-model:value="record.unitCost"
                size="small"
                :min="0"
                :precision="2"
                style="width: 100%"
              />
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-button
                type="link"
                size="small"
                danger
                @click="removeItem(index)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>
        <a-button
          block
          type="dashed"
          style="margin-top: 8px"
          @click="addItem"
        >
          <template #icon>
            <PlusOutlined />
          </template>添加组件
        </a-button>
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="mall-product-combo"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { productKitApi, type ProductKit, type ProductKitItem } from '@/api/erp/mall'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MallProductCombo' })

// ═══ 套装类型（与后端 KitType 枚举一致：1组合套装 2捆绑套装 3礼品套装 4组装产品 5拆分产品） ═══
const KIT_TYPE_MAP: Record<number, { label: string }> = {
  1: { label: '组合套装' },
  2: { label: '捆绑套装' },
  3: { label: '礼品套装' },
  4: { label: '组装产品' },
  5: { label: '拆分产品' }
}

const kitTypeOptions = Object.entries(KIT_TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))

// 显示状态（对标「显示状态=已启用」；查询走 product_kit.status，启停用走 activate/deactivate 的 active）
const statusOptions = [
  { label: '已启用', value: 1 },
  { label: '已停用', value: 0 }
]

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<ProductKit[]>([])

// ═══ 查询条件 ═══
const searchForm = reactive({
  keyword: '',
  kitType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 列定义（DetailColumnConfig[]）
 * 对标 3 列（默认显示）：组合名称 / 编号 / 商品明细
 * 本系统扩展：状态（默认显示，启停用操作依据）；类型、组合价、组合成本、利润率、创建时间（默认隐藏）
 * 固定列（不进列配置面板）：rowNo（承载列配置齿轮）、checkbox（批量操作）、action
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 260, fixed: 'left' },
  { key: 'kitName', title: '组合名称', type: 'slot', slotName: 'kitNameCell', width: 200, sortable: true },
  { key: 'kitCode', title: '编号', type: 'slot', slotName: 'kitCodeCell', width: 160 },
  { key: 'items', title: '商品明细', type: 'slot', slotName: 'itemsCell', width: 110 },
  { key: 'active', title: '状态', type: 'slot', slotName: 'activeCell', width: 100 },
  { key: 'kitType', title: '类型', type: 'slot', slotName: 'kitTypeCell', width: 110, defaultHidden: true },
  { key: 'kitPrice', title: '组合价', type: 'slot', slotName: 'kitPriceCell', width: 110, align: 'right', defaultHidden: true },
  { key: 'kitCost', title: '组合成本', type: 'slot', slotName: 'kitCostCell', width: 110, align: 'right', defaultHidden: true },
  { key: 'profitRate', title: '利润率', type: 'slot', slotName: 'profitRateCell', width: 100, align: 'right', defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true },
]

const detailItemColumns: any[] = [
  { title: '商品编码', dataIndex: 'componentProductCode', key: 'componentProductCode', width: 110 },
  { title: '商品名称', dataIndex: 'componentProductName', key: 'componentProductName', ellipsis: true },
  { title: '单位', dataIndex: 'componentProductUnit', key: 'componentProductUnit', width: 70 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 90, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 100, align: 'right' },
  { title: '行成本', dataIndex: 'lineCost', key: 'lineCost', width: 110, align: 'right' }
]

const editItemColumns: any[] = [
  { title: '商品编码', dataIndex: 'componentProductCode', key: 'componentProductCode', width: 130 },
  { title: '商品名称', dataIndex: 'componentProductName', key: 'componentProductName' },
  { title: '单位', dataIndex: 'componentProductUnit', key: 'componentProductUnit', width: 80 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 110 },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 110 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 60 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载（erp-sales /erp/product-kit/page） ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await productKitApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      kitType: searchForm.kitType,
      status: searchForm.status,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedRows.value = []
  } catch (error: any) {
    console.error('[商品组合] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 行选择（批量启用/停用/删除） ═══
const selectedRows = ref<ProductKit[]>([])
const selectedIds = computed(() =>
  selectedRows.value.filter((r: any) => !r.__ghost).map((r: any) => r.id).filter((id: any) => id !== undefined && id !== null)
)

function handleRowCheck(record: any, _index: number, checked: boolean) {
  if (!record || record.__ghost) return
  if (checked) {
    if (!selectedRows.value.some((r: any) => r.id === record.id)) {
      selectedRows.value = [...selectedRows.value, record]
    }
  } else {
    selectedRows.value = selectedRows.value.filter((r: any) => r.id !== record.id)
  }
}

function handleRowCheckAll(checked: boolean, records: any[]) {
  const rows = (records || []).filter((r: any) => r && !r.__ghost)
  selectedRows.value = checked ? rows : []
}

// ═══ 详情抽屉（组合组件子表） ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<ProductKit | null>(null)
const detailItems = ref<ProductKitItem[]>([])

async function openDetail(record: ProductKit) {
  detailData.value = record
  detailItems.value = []
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailItems.value = await productKitApi.items(record.id!) || []
  } catch (e: any) {
    console.error('[商品组合] 组件获取失败', e)
    message.error(e?.response?.data?.message || e?.message || '组合组件加载失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 新增/编辑 ═══
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

interface KitItemForm {
  componentProductCode: string
  componentProductName: string
  componentProductUnit: string
  quantity: number
  unitCost: number
}

const emptyForm = () => ({
  kitName: '',
  kitType: 1 as number,
  productCode: '',
  productName: '',
  productUnit: '',
  kitPrice: 0 as number,
  allowSplit: false,
  allowPartial: false,
  description: '',
  items: [] as KitItemForm[]
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  kitName: [{ required: true, message: '请输入组合名称', trigger: 'blur' }],
  kitType: [{ required: true, message: '请选择组合类型', trigger: 'change' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

async function openEdit(record: ProductKit) {
  editingId.value = record.id ?? null
  let items: KitItemForm[] = []
  try {
    const list = await productKitApi.items(record.id!) || []
    items = list.map((it: ProductKitItem) => ({
      componentProductCode: it.componentProductCode || '',
      componentProductName: it.componentProductName || '',
      componentProductUnit: it.componentProductUnit || '',
      quantity: Number(it.quantity) || 0,
      unitCost: Number(it.unitCost) || 0
    }))
  } catch (e: any) {
    console.error('[商品组合] 组件获取失败', e)
    message.error(e?.response?.data?.message || e?.message || '组合组件加载失败')
  }
  Object.assign(form, emptyForm(), {
    kitName: record.kitName,
    kitType: record.kitType ?? 1,
    productCode: record.productCode || '',
    productName: record.productName || '',
    productUnit: record.productUnit || '',
    kitPrice: Number(record.kitPrice) || 0,
    allowSplit: !!record.allowSplit,
    allowPartial: !!record.allowPartial,
    description: record.description || '',
    items
  })
  modalOpen.value = true
}

function addItem() {
  form.items.push({ componentProductCode: '', componentProductName: '', componentProductUnit: '', quantity: 1, unitCost: 0 })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!form.items.length) {
    message.warning('请至少添加一个组件商品')
    return
  }
  if (form.items.some(it => !it.componentProductName.trim())) {
    message.warning('组件商品名称不能为空')
    return
  }
  saving.value = true
  try {
    const payload = {
      kitName: form.kitName,
      kitType: form.kitType,
      productCode: form.productCode,
      productName: form.productName,
      productUnit: form.productUnit,
      kitPrice: form.kitPrice,
      allowSplit: form.allowSplit,
      allowPartial: form.allowPartial,
      description: form.description,
      items: form.items.map(it => ({ ...it }))
    }
    if (editingId.value) {
      await productKitApi.update(editingId.value, payload)
      message.success('组合已更新')
    } else {
      await productKitApi.create(payload)
      message.success('组合已创建')
    }
    modalOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCopy(record: ProductKit) {
  try {
    await productKitApi.copy(record.id!)
    message.success(`已复制组合「${record.kitName}」`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '复制失败')
  }
}

async function toggleActive(record: ProductKit) {
  try {
    if (record.active) {
      await productKitApi.deactivate(record.id!)
      message.success(`组合「${record.kitName}」已停用`)
    } else {
      await productKitApi.activate(record.id!)
      message.success(`组合「${record.kitName}」已启用`)
    }
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '状态切换失败')
  }
}

// ═══ 批量启用 / 停用（后端 batch-activate / batch-deactivate，一次请求） ═══
const batchLoading = ref(false)

async function handleBatchActive(active: boolean) {
  const ids = selectedIds.value
  if (!ids.length) {
    message.warning('请先勾选要操作的组合')
    return
  }
  batchLoading.value = true
  try {
    const res: any = active
      ? await productKitApi.batchActivate(ids as number[])
      : await productKitApi.batchDeactivate(ids as number[])
    // 后端返回实际更新条数（解包后可能是数字或 {data:number}）
    const updated = typeof res === 'number' ? res : (res?.data ?? ids.length)
    message.success(`已${active ? '启用' : '停用'} ${updated} 个组合`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量操作失败')
  } finally {
    batchLoading.value = false
  }
}

// ═══ 单条删除（后端 DELETE /erp/product-kit/{id}：级联逻辑删除组件行） ═══
async function handleDelete(record: ProductKit) {
  if (record.id == null) return
  try {
    await productKitApi.remove(record.id)
    message.success(`组合「${record.kitName}」已删除`)
    // 删除当前页最后一条时回退一页，避免停留在空页
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

// ═══ 批量删除（后端 DELETE /erp/product-kit/batch：body {"ids":[…]}） ═══
function handleBatchDelete() {
  const ids = selectedIds.value
  if (!ids.length) {
    message.warning('请先勾选要删除的组合')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${ids.length} 个商品组合？删除后不可恢复。`,
    okType: 'danger',
    onOk: async () => {
      batchLoading.value = true
      try {
        await productKitApi.batchRemove(ids as number[])
        message.success(`已删除 ${ids.length} 个组合`)
        // 删除后当前页可能已空，回退一页
        if (ids.length >= tableData.value.length && pagination.current > 1) {
          pagination.current -= 1
        }
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '批量删除失败')
      } finally {
        batchLoading.value = false
      }
    }
  })
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
// 组合价是数值语义列（key 含 price），原样传数值交给模板 digits 显示，不做前端格式化。
const printColumns: any[] = [
  { title: '组合名称', key: 'kitName' },
  { title: '编号', key: 'kitCode', formatter: (v: any) => v || '' },
  { title: '类型', key: 'kitType', formatter: (v: any) => KIT_TYPE_MAP[v ?? -1]?.label || '' },
  { title: '状态', key: 'active', formatter: (v: any) => (v ? '已启用' : '已停用') },
  { title: '组合价', key: 'kitPrice' },
]

function currentRows(): ProductKit[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'mall-product-combo',
  title: '商品组合',
  rows: () => currentRows(),
  columns: () => printColumns,
  // 原打印抬头的筛选/记录数元信息行（打印时间由模板 pageHeader 负责）
  totalText: () => `筛选条件：${searchForm.keyword || '全部'}，记录数：${currentRows().length}`,
  emptyTip: '没有可打印的数据',
})

// ═══ 导出（前端 CSV，\uFEFF BOM 保证 Excel 中文不乱码） ═══
function handleExport() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const header = ['组合名称', '编号', '类型', '状态', '组合价', '组合成本', '利润率(%)', '创建时间']
  const lines = rows.map((r: any) => [
    r.kitName ?? '',
    r.kitCode ?? '',
    KIT_TYPE_MAP[r.kitType ?? -1]?.label || '',
    r.active ? '已启用' : '已停用',
    r.kitPrice ?? '',
    r.kitCost ?? '',
    r.profitRate ?? '',
    fmtTime(r.createTime),
  ])
  const csv = [header, ...lines]
    .map(cols => cols.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `商品组合_${dayjs().format('YYYYMMDD')}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[商品组合] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 橙色新增按钮（交易模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
