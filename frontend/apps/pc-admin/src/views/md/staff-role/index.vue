<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        岗位权限（人力资源 → 职员管理 → 岗位权限，菜单 80531 `md:staff-role`）
        · 定位：系统岗位主数据（sys_position）台账 + 岗位分类树。
          ⚠️ 菜单名「岗位权限」与实际能力无关：本页是岗位档案 CRUD，不接触任何角色/菜单/权限码
          （系统真实模型是 用户 ↔ 角色 ↔ 菜单 RBAC，权限配置入口在 views/system/role，本次不动）。
        · 骨架：CategoryListLayout（左：岗位分类树）+ BillTableList（表头齿轮列配置）
          + StandardPagination(variant="classic") + PageConfigPanel
        · 工具栏：新增岗位 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：岗位编码 · 岗位名称 · 状态
        · 行内：修改 / 启用-停用 / 删除
        · 表单：弹窗内分区卡片 + 两列栅格（岗位是主数据档案，非单据，不用 BillFormPage）
        · 接口：@/api/position 的 positionApi（后端 core-api PositionController）。
          页面内一律走相对路径封装（/position/...），由请求拦截器统一拼接服务前缀，勿手写绝对路径
      -->
      <div class="page-tip">
        <a-alert
          type="info"
          show-icon
          message="本页维护系统岗位主数据（sys_position），与「用户 ↔ 角色 ↔ 菜单」权限体系无关联"
          description="菜单名「岗位权限」沿用历史命名。岗位分类、岗位下用户分配、岗位与角色/权限的挂钩暂无页面入口；本页仅提供岗位档案（编码/名称/分类/排序/状态/备注）。"
        />
      </div>

      <div class="layout-wrap">
        <CategoryListLayout
          :tabs="[]"
          :show-category-panel="true"
          category-title="岗位分类"
          :category-editable="false"
          :category-tree-data="categoryTreeData"
          :category-loading="categoryLoading"
          :selected-category-id="selectedCategoryId"
          :category-expanded-keys="expandedKeys"
          :current-path="currentCategoryPath"
          :show-table-footer="true"
          @category-select="handleCategorySelect"
          @category-expand="handleCategoryExpand"
        >
          <!-- ═══ 工具栏左侧：新增岗位 ═══ -->
          <template #toolbar-left>
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增岗位
            </a-button>
          </template>

          <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印(F8) + 导出 ═══ -->
          <template #toolbar-right>
            <a-space :size="8">
              <a-button
                size="small"
                title="页面配置"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
              <a-button
                v-if="isButtonEnabled('refresh')"
                size="small"
                :loading="loading"
                @click="fetchList"
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

          <!-- ═══ 查询区（横向自适应网格；字段显隐由页面配置控制） ═══ -->
          <template #search-fields>
            <div class="search-area">
              <div class="search-row">
                <div
                  v-if="isQueryVisible('positionCode')"
                  class="search-item"
                >
                  <span class="search-label">岗位编码</span>
                  <a-input
                    v-model:value="searchForm.positionCode"
                    placeholder="请输入岗位编码"
                    size="small"
                    style="width: 180px"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="isQueryVisible('positionName')"
                  class="search-item"
                >
                  <span class="search-label">岗位名称</span>
                  <a-input
                    v-model:value="searchForm.positionName"
                    placeholder="请输入岗位名称"
                    size="small"
                    style="width: 180px"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="isQueryVisible('status')"
                  class="search-item"
                >
                  <span class="search-label">状态</span>
                  <a-select
                    v-model:value="searchForm.status"
                    placeholder="全部状态"
                    size="small"
                    style="width: 120px"
                    allow-clear
                    :options="statusOptions"
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
            </div>
          </template>

          <!-- ═══ 数据表（BillTableList 自带工具栏/分页均关闭，改用外层工具栏 + 经典分页栏，避免两套分页） ═══ -->
          <template #table>
            <div class="table-area">
              <BillTableList
                :columns="columns"
                :data-source="tableData"
                :loading="loading"
                :pagination="false"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                storage-key="md-staff-role-table-columns"
                global-config-key="md-staff-role-table-columns"
                row-key="id"
                @sort-change="handleSortChange"
              >
                <template #nameCell="{ record, column }">
                  <a
                    v-if="!record.__ghost"
                    class="cell-link"
                    @click="handleEdit(record)"
                  >{{ record[column.key] }}</a>
                </template>

                <template #statusCell="{ record, column }">
                  <a-tag
                    v-if="!record.__ghost"
                    :color="record[column.key] === 1 ? 'green' : 'default'"
                  >
                    {{ statusText(record[column.key]) }}
                  </a-tag>
                </template>

                <template #userCountCell="{ record, column }">
                  <span v-if="!record.__ghost">{{ record[column.key] ?? 0 }}</span>
                </template>

                <template #timeCell="{ record, column }">
                  <span v-if="!record.__ghost">{{ formatTime(record[column.key]) }}</span>
                </template>

                <template #actionCell="{ record }">
                  <a-space
                    v-if="!record.__ghost"
                    :size="0"
                  >
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
                      {{ record.status === 1 ? '停用' : '启用' }}
                    </a-button>
                    <a-popconfirm
                      title="确认删除该岗位？"
                      ok-text="删除"
                      cancel-text="取消"
                      @confirm="handleDelete(record)"
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
              </BillTableList>
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
      </div>

      <!-- ═══ 新增 / 修改弹窗（主数据：分区卡片 + 两列栅格） ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="formState.id ? '修改岗位' : '新增岗位'"
        :width="720"
        :mask-closable="false"
        :confirm-loading="saving"
        ok-text="保存(Enter)"
        cancel-text="关闭(Esc)"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <FormSection
            title="岗位信息"
            tip="岗位编码在租户内唯一，编辑时不可改动；岗位分类来自分类树"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="岗位编码"
                  name="positionCode"
                >
                  <a-input
                    v-model:value="formState.positionCode"
                    placeholder="请输入岗位编码"
                    :maxlength="50"
                    :disabled="!!formState.id"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="岗位名称"
                  name="positionName"
                >
                  <a-input
                    v-model:value="formState.positionName"
                    placeholder="请输入岗位名称"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="岗位分类">
                  <a-select
                    v-model:value="formState.categoryId"
                    placeholder="请选择岗位分类"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="categoryOptions"
                    :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="排序">
                  <a-input-number
                    v-model:value="formState.sort"
                    :min="0"
                    :precision="0"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="状态与备注">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="状态">
                  <a-switch
                    :checked="formState.status === 1"
                    checked-children="启用"
                    un-checked-children="禁用"
                    @change="(v: any) => (formState.status = v ? 1 : 0)"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="formState.remark"
                    placeholder="请输入岗位备注"
                    :rows="2"
                    :maxlength="255"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="md-staff-role-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
// 缺口（后端/数据侧能力缺失，本页只做前端兜底，未改任何其它文件）：
// ① 岗位分类维护（新增/编辑/删除/启停）未做 UI：api/position.ts 已封装 getCategoryPage/getCategoryList/
//    createCategory/updateCategory/deleteCategory/updateCategoryStatus，本次仅接入分类树与表单分类下拉。
// ② 后端 GET /position/category/tree 未在 api/position.ts 封装（该文件本次禁止修改）→ 本页用
//    getCategoryList（CategoryVO 含 parentId/children）在前端自建树；该接口只返回 status=1 的分类。
// ③ 后端 GET /position/export 为无 BOM 的 CSV（Excel 中文乱码，已在规格登记）→ 本页改用 getPage 拉全量
//    在前端生成带 BOM 的 CSV；positionApi.export 仍未接线。
// ④ 后端 PositionVO.deptName 从不回填（P1-1），PositionQueryRequest 也无 level/创建时间区间参数
//    → 不设「所属部门」列，不设级别/时间类查询条件。
// ⑤ 后端 GET /position/page 无排序参数（恒 orderByAsc sort）→ 表头排序为当前页本地排序，取消排序时重新拉取。
// ⑥ 岗位下用户分配（POST /assign、DELETE /remove、GET /{id}/users）按本次页面范围未接入，
//    在岗人数列取后端 userCount 聚合值；「在岗人员」明细弹窗待后续补。
// ⑦ 规格判定本页打印 N/A（本模块无 ql361 对标页）；此处按调用方要求保留 F8 打印当前筛选结果，可由页面配置关闭。
// ⑧ 岗位-角色/权限零关联：系统真实模型是 用户↔角色↔菜单 RBAC，本页不接触任何角色/权限码。
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import { positionApi } from '@/api/position'

defineOptions({ name: 'MdStaffRole' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

// ═══ 数据表列（默认可见 7 个数据列 + 序号 + 操作；级别/描述/备注/更新时间默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'positionCode', title: '岗位编码', type: 'input', width: 150, sortable: true },
  { key: 'positionName', title: '岗位名称', type: 'slot', slotName: 'nameCell', width: 180, sortable: true },
  { key: 'categoryName', title: '岗位分类', type: 'input', width: 150 },
  { key: 'sort', title: '排序', type: 'input', width: 80, align: 'right', sortable: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'userCount', title: '在岗人数', type: 'slot', slotName: 'userCountCell', width: 100, align: 'right' },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'timeCell', width: 170, sortable: true },
  // ── 默认隐藏 ──
  { key: 'level', title: '岗位级别', type: 'input', width: 90, align: 'right', defaultHidden: true },
  { key: 'description', title: '岗位描述', type: 'input', width: 200, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'timeCell', width: 170, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  positionCode: '',
  positionName: '',
  status: undefined as number | undefined,
})

// ═══ 左侧分类树：岗位分类（数据源 sys_position_category） ═══
const ROOT_CATEGORY_ID = 0
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])
const categoryNameMap = ref<Record<string, string>>({})
const categoryOptions = ref<{ label: string; value: number }[]>([])

const currentCategoryPath = computed(() => {
  if (String(selectedCategoryId.value) === String(ROOT_CATEGORY_ID)) return '全部岗位'
  return categoryNameMap.value[String(selectedCategoryId.value)] || '全部岗位'
})

/** 平铺分类（CategoryVO 含 parentId）→ 树；后端 getCategoryList 只返回启用分类 */
function buildCategoryTree(list: any[]): any[] {
  const nodes = (list || []).map((c: any) => ({
    id: c.id,
    parentId: c.parentId ?? ROOT_CATEGORY_ID,
    categoryName: c.categoryName,
    children: [] as any[],
  }))
  const nodeMap = new Map<string, any>()
  nodes.forEach((n: any) => nodeMap.set(String(n.id), n))
  const roots: any[] = []
  nodes.forEach((n: any) => {
    const parent = nodeMap.get(String(n.parentId))
    if (String(n.parentId) !== String(ROOT_CATEGORY_ID) && parent) parent.children.push(n)
    else roots.push(n)
  })
  return roots
}

async function loadCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await positionApi.getCategoryList()
    const list: any[] = Array.isArray(res) ? res : Array.isArray(res?.data) ? res.data : []
    const map: Record<string, string> = {}
    list.forEach((c: any) => {
      map[String(c.id)] = c.categoryName
    })
    categoryNameMap.value = map
    categoryOptions.value = list.map((c: any) => ({ label: c.categoryName, value: c.id }))
    categoryTreeData.value = [
      { id: ROOT_CATEGORY_ID, categoryName: '全部岗位', children: buildCategoryTree(list) },
    ]
    expandedKeys.value = [ROOT_CATEGORY_ID]
  } catch (error) {
    console.warn('[岗位权限] 岗位分类加载失败', error)
    categoryTreeData.value = [{ id: ROOT_CATEGORY_ID, categoryName: '全部岗位' }]
    categoryNameMap.value = {}
  } finally {
    categoryLoading.value = false
  }
}

/** 选中分类节点：根节点看全部，其余节点按 categoryId 过滤 */
function handleCategorySelect(keys: any[]) {
  const key = keys?.[0]
  selectedCategoryId.value = key === undefined || key === null ? ROOT_CATEGORY_ID : key
  handleSearch()
}

function handleCategoryExpand(keys: any[]) {
  expandedKeys.value = keys || []
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (String(selectedCategoryId.value) !== String(ROOT_CATEGORY_ID)) {
    params.categoryId = selectedCategoryId.value
  }
  if (searchForm.positionCode) params.positionCode = searchForm.positionCode.trim()
  if (searchForm.positionName) params.positionName = searchForm.positionName.trim()
  if (searchForm.status !== undefined) params.status = searchForm.status
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await positionApi.getPage(buildParams())
    // 兼容两种形状：拦截器已拆包的 {records,total} 与未拆包的 {data:{records,total}}
    const records = res?.records || res?.data?.records || []
    const total = res?.total ?? res?.data?.total ?? 0
    tableData.value = records
    pagination.total = Number(total) || 0
  } catch (error: any) {
    console.error('[岗位权限] 加载列表失败', error)
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

function handleReset() {
  searchForm.positionCode = ''
  searchForm.positionName = ''
  searchForm.status = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/**
 * 表头排序：后端 PositionQueryRequest 无排序参数（恒按 sort 升序），
 * 故这里对当前页数据做本地排序；第三次点击（order=null）重新拉取恢复后端默认口径。
 */
function handleSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  if (!key || !order) {
    fetchList()
    return
  }
  const dir = order === 'asc' ? 1 : -1
  const rows = [...tableData.value]
  rows.sort((a: any, b: any) => {
    const va = a?.[key]
    const vb = b?.[key]
    if (va === null || va === undefined) return vb === null || vb === undefined ? 0 : -dir
    if (vb === null || vb === undefined) return dir
    if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
    return String(va).localeCompare(String(vb), 'zh-Hans-CN') * dir
  })
  tableData.value = rows
}

// ═══ 展示辅助 ═══
function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function statusText(status: number | undefined | null): string {
  return status === 1 ? '启用' : '禁用'
}

// ═══ 新增 / 修改弹窗 ═══
const modalVisible = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null as string | number | null,
  positionCode: '',
  positionName: '',
  categoryId: undefined as number | undefined,
  sort: 0,
  status: 1,
  remark: '',
})
const formState = reactive(emptyForm())

const rules: Record<string, any> = {
  positionCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

function handleAdd() {
  resetForm()
  modalVisible.value = true
}

/** 编辑一律先拉详情回填（不用列表行，避免未来改 UpdateDTO 策略时静默丢字段） */
async function handleEdit(record: any) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await positionApi.getById(record.id)
    const data = detail?.data || detail
    if (!data) return
    formState.id = data.id
    formState.positionCode = data.positionCode || ''
    formState.positionName = data.positionName || ''
    formState.categoryId = data.categoryId ?? undefined
    formState.sort = data.sort ?? 0
    formState.status = data.status ?? 1
    formState.remark = data.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载岗位详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload: any = {
    positionCode: formState.positionCode.trim(),
    positionName: formState.positionName.trim(),
    categoryId: formState.categoryId ?? null,
    sort: formState.sort ?? 0,
    status: formState.status,
    remark: formState.remark || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      await positionApi.update(formState.id as number, payload)
      message.success('修改成功')
    } else {
      await positionApi.create(payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启用 / 停用 ═══
function handleToggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  const actionText = target === 1 ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}岗位「${record.positionName}」吗？`,
    onOk: async () => {
      try {
        await positionApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

// ═══ 删除（后端会校验岗位下是否有关联用户，冲突时回 400 文案） ═══
async function handleDelete(record: any) {
  try {
    await positionApi.delete(record.id)
    message.success('删除成功')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 打印(F8)：岗位台账（当前筛选结果） ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.positionCode)}</td>
      <td>${escapeHtml(r.positionName)}</td>
      <td>${escapeHtml(r.categoryName || '')}</td>
      <td>${escapeHtml(r.level ?? '')}</td>
      <td>${escapeHtml(r.sort ?? '')}</td>
      <td>${escapeHtml(statusText(r.status))}</td>
      <td>${escapeHtml(r.userCount ?? 0)}</td>
      <td>${escapeHtml(r.remark || '')}</td>
      <td>${escapeHtml(formatTime(r.createTime))}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>岗位台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>岗位台账</h2>
    <div class="meta">
      <span>岗位分类：${escapeHtml(currentCategoryPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>岗位编码</th><th>岗位名称</th><th>岗位分类</th><th>级别</th>
        <th>排序</th><th>状态</th><th>在岗人数</th><th>备注</th><th>创建时间</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
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
  // 弹窗内 Enter 保存（多行文本域内不触发）
  if (e.key === 'Enter' && modalVisible.value && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const target = e.target as HTMLElement | null
    if (target && target.tagName === 'TEXTAREA') return
    e.preventDefault()
    handleSave()
  }
}

// ═══ 导出（CSV：按当前筛选与分类范围拉全量，带 BOM 防 Excel 中文乱码） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await positionApi.getPage({ ...params, pageNum: 1, pageSize: 10000 })
    const records = res?.records || res?.data?.records || []
    if (!records.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['岗位编码', '岗位名称', '岗位分类', '岗位级别', '排序', '状态', '在岗人数', '岗位描述', '备注', '创建时间', '更新时间']
    const lines = records.map((r: any) => [
      r.positionCode, r.positionName, r.categoryName, r.level, r.sort,
      statusText(r.status), r.userCount ?? 0, r.description, r.remark,
      formatTime(r.createTime), formatTime(r.updateTime),
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `岗位权限_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${records.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'positionCode', label: '岗位编码', visible: true },
  { key: 'positionName', label: '岗位名称', visible: true },
  { key: 'status', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增岗位', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
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

function handleError(error: Error) {
  console.error('[岗位权限] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadCategoryTree()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
/* 页头提示条：flex-shrink:0，把剩余高度全部让给下方布局 */
.page-tip { flex-shrink: 0; padding: 8px 16px 0; }
/* 布局外壳：必须 flex 纵向占满剩余高度，CategoryListLayout 的 height:100% 才有确定高度 */
.layout-wrap { flex: 1; min-height: 0; display: flex; flex-direction: column; }

/* 插槽内容的样式必须自备：布局组件的 scoped 样式不作用于注入的插槽内容 */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：表格根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 橙色新增按钮（资料模块统一） */
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
