<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        菜单配置（设置 → 系统配置 → 菜单配置，菜单 80620 / set:menu-config，单入口）
        · 定位（租户级）：**我这个租户**改**自己**菜单的显隐。对标 ql361「设置 → 系统配置 → 菜单配置」的
          「左分组 + 右侧每个页面一行开/关」形态。
        · 边界：全局菜单定义（名称/路由/组件/权限码）属系统模块的平台级「菜单管理」，本页只读不改；
          写操作只落在**本租户**自己的配置行上（sys_project_config.tenant_id = 当前会话租户），
          租户 ID 取自登录会话、不接受前端传入，因此不可能影响其它租户。
        · 分组来源：左分类面板 = 一级域（sys_menu 中 parent_id = 0 且 client_type = 'tenant-admin' 的目录）；
          右栏数据表 = 该域下的**页面级菜单**（menu_type = 1），一行一个「显示 / 隐藏」开关。
        · 「权限标识」列取 sys_menu.menu_code —— 该表**没有** perms 列（旧实现绑 perms，导致整列恒空、
          弹窗里填的权限码被静默丢弃）。
        · 分页：菜单定义是全局资产（百级数据），一次取回后在本页做筛选 + 经典分页，切页不再打接口，
          开关状态也不会因翻页而抖动。
        · 即时保存：对标 ql361 是「底部统一保存」，本系统沿用仓库口径（逐项即时写库 + 幂等判定），
          避免「改了没保存」的静默丢失；写入失败会把开关回滚到原状态。
        · 自锁保护：「菜单配置」页自身不可关闭（关掉后本页即从导航消失，就再也进不来恢复）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="菜单分组"
        :category-editable="false"
        :category-tree-data="domainNodes"
        :category-loading="loading"
        :category-error="loadError"
        :selected-category-id="selectedDomainId"
        :current-path="currentPath"
        :show-table-footer="true"
        @category-select="handleDomainSelect"
        @category-retry="fetchList"
      >
        <!-- ═══ 工具栏左侧：恢复（把本组已隐藏的菜单重新显示出来） ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('showAll')"
              size="small"
              :loading="bulkLoading"
              @click="handleShowAllInDomain"
            >
              <CheckCircleOutlined /> 本组全部显示
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="btnEnabled('pageConfig')"
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <div
                v-if="fieldVisible('menuName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.menuName"
                  placeholder="菜单名称"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('menuCode')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.menuCode"
                  placeholder="权限标识（如 set:menu-config）"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('path')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.path"
                  placeholder="路由路径"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('visibleState')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.visibleState"
                  placeholder="显示状态"
                  size="small"
                  allow-clear
                  :options="visibleStateOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('status')"
                class="search-field-item"
              >
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="菜单状态"
                  size="small"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
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
          </div>
        </template>

        <!-- ═══ 数据表：每个页面一行「显示 / 隐藏」开关（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="set-menu-config-table-columns"
              global-config-key="set-menu-config-table-columns"
            >
              <!-- 菜单名称 -->
              <template #menuNameCell="{ record }">
                <span class="cell-menu-name">{{ record.menuName }}</span>
              </template>

              <!-- 权限标识（sys_menu.menu_code，本表没有 perms 列） -->
              <template #menuCodeCell="{ record }">
                <span
                  v-if="record.menuCode"
                  class="cell-code"
                >{{ record.menuCode }}</span>
                <span
                  v-else
                  class="cell-empty"
                >-</span>
              </template>

              <!-- 菜单状态：sys_menu.status（1 = 启用，0 = 停用；语义与后端 getUserMegaMenus 一致） -->
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'success' : 'error'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>

              <!-- 显示 / 隐藏开关（本租户级） -->
              <template #visibleCell="{ record }">
                <a-tooltip :title="visibleTip(record)">
                  <span class="cell-switch">
                    <a-switch
                      :checked="record.visible"
                      :disabled="record.locked || record.status !== 1"
                      :loading="togglingId === String(record.id)"
                      checked-children="显示"
                      un-checked-children="隐藏"
                      @change="(checked: any) => handleToggle(record, checked)"
                    />
                  </span>
                </a-tooltip>
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

      <!-- ═══ 页面配置（查询条件 + 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonsConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :hide-print-config="true"
        storage-key="set-menu-config-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { setMenuConfigApi, type SetMenuConfigItem } from '@/api/set/menu-config'

defineOptions({ name: 'SetMenuConfig' })

// ═══ 状态 ═══
const loading = ref(false)
const loadError = ref(false)
const bulkLoading = ref(false)
const togglingId = ref<string | null>(null)
/** 全量行（页面级菜单，含已隐藏项）；表格数据是它的分页切片，切片的元素与它共享同一对象引用 */
const allRows = ref<SetMenuConfigItem[]>([])
const tableData = ref<SetMenuConfigItem[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 查询条件 ═══
const searchForm = reactive<Record<string, any>>({
  menuName: '',
  menuCode: '',
  path: '',
  visibleState: undefined,
  status: undefined
})

const visibleStateOptions = [
  { label: '显示', value: 'show' },
  { label: '隐藏', value: 'hide' }
]
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PAGE_CONFIG_KEY = 'set-menu-config-page-config'

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'menuName', label: '菜单名称', visible: true },
  { key: 'menuCode', label: '权限标识', visible: true },
  { key: 'path', label: '路由路径', visible: true },
  { key: 'visibleState', label: '显示状态', visible: true },
  { key: 'status', label: '菜单状态', visible: true }
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'showAll', label: '本组全部显示', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true }
]
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面把「当前配置」传给 queryFieldsConfig，必须另给出厂值否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))
const showPageConfig = ref(false)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldsConfig.value = QUERY_FIELDS.map(def => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === def.key)
        return saved ? { ...def, visible: saved.visible !== false } : { ...def }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(def => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === def.key)
        return saved ? { ...def, enabled: saved.enabled !== false } : { ...def }
      })
    }
  } catch { /* 配置损坏时按出厂值渲染 */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || []
  }))
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 数据表列（金标准：首列 rowNo 承载表头齿轮列配置） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'menuName', title: '菜单名称', type: 'slot', slotName: 'menuNameCell', width: 200 },
  { key: 'menuCode', title: '权限标识', type: 'slot', slotName: 'menuCodeCell', width: 200 },
  { key: 'path', title: '路由路径', width: 200, ellipsis: true },
  { key: 'groupName', title: '所属分组', width: 140 },
  { key: 'sort', title: '排序', width: 70, align: 'right' },
  { key: 'status', title: '菜单状态', type: 'slot', slotName: 'statusCell', width: 100 },
  // 列 key 用 tenantVisible（而非 visible）：避免与列配置项自身的 visible 字段同名造成误读
  { key: 'tenantVisible', title: '显示/隐藏（本租户）', type: 'slot', slotName: 'visibleCell', width: 160 },
  { key: 'component', title: '组件路径', width: 260, ellipsis: true, defaultHidden: true },
  { key: 'icon', title: '图标', width: 160, defaultHidden: true }
]

// ═══ 左侧分组（一级域） ═══
const selectedDomainId = ref<string>('')

const domainNodes = computed(() => {
  // 用 Map 收集（而非普通对象）：菜单 id 作键时避免命中原型链上的键
  const map = new Map<string, { id: string; categoryName: string; sort: number }>()
  for (const row of allRows.value) {
    if (row.domainId == null) continue
    const id = String(row.domainId)
    if (map.has(id)) continue
    map.set(id, { id, categoryName: row.domainName || '未分组', sort: row.domainSort ?? 0 })
  }
  // 按一级域自身的 sort 排序（页面自身的 sort 只在组内有序，跨组会交错）
  return [...map.values()]
    .sort((a, b) => a.sort - b.sort)
    .map(({ id, categoryName }) => ({ id, categoryName }))
})

const selectedDomainName = computed(
  () => domainNodes.value.find(n => n.id === selectedDomainId.value)?.categoryName || '全部分组'
)

const currentPath = computed(() => `${selectedDomainName.value} / 全部页面`)

function handleDomainSelect(keys: (string | number)[]) {
  if (!keys.length) return
  selectedDomainId.value = String(keys[0])
  pagination.current = 1
  applyFilters()
}

// ═══ 筛选 + 经典分页（本地分页：切页不打接口，开关状态不抖动） ═══
function applyFilters() {
  const kwMenuName = String(searchForm.menuName || '').trim().toLowerCase()
  const kwMenuCode = String(searchForm.menuCode || '').trim().toLowerCase()
  const kwPath = String(searchForm.path || '').trim().toLowerCase()

  const rows = allRows.value.filter(row => {
    if (selectedDomainId.value && String(row.domainId) !== selectedDomainId.value) return false
    if (kwMenuName && !String(row.menuName || '').toLowerCase().includes(kwMenuName)) return false
    if (kwMenuCode && !String(row.menuCode || '').toLowerCase().includes(kwMenuCode)) return false
    if (kwPath && !String(row.path || '').toLowerCase().includes(kwPath)) return false
    if (searchForm.visibleState === 'show' && !row.visible) return false
    if (searchForm.visibleState === 'hide' && row.visible) return false
    if (searchForm.status !== undefined && searchForm.status !== null && row.status !== searchForm.status) return false
    return true
  })

  pagination.total = rows.length
  const maxPage = Math.max(1, Math.ceil(rows.length / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
  const start = (pagination.current - 1) * pagination.pageSize
  // slice 保留元素引用：表格行与 allRows 是同一批对象，开关状态可原地更新
  tableData.value = rows.slice(start, start + pagination.pageSize)
}

// ═══ 加载 ═══
async function fetchList() {
  loading.value = true
  loadError.value = false
  try {
    const res: any = await setMenuConfigApi.list()
    allRows.value = Array.isArray(res) ? res : []
    // 默认选中第一个一级域；当前选中域已不存在时同样回落到第一个
    const nodes = domainNodes.value
    if (!nodes.length) {
      selectedDomainId.value = ''
    } else if (!nodes.some(n => n.id === selectedDomainId.value)) {
      selectedDomainId.value = nodes[0].id
    }
    applyFilters()
  } catch (error: any) {
    console.error('[菜单配置] 加载菜单清单失败', error)
    loadError.value = true
    allRows.value = []
    tableData.value = []
    pagination.total = 0
    message.error(error?.response?.data?.message || error?.message || '加载菜单清单失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  applyFilters()
}

function handleReset() {
  searchForm.menuName = ''
  searchForm.menuCode = ''
  searchForm.path = ''
  searchForm.visibleState = undefined
  searchForm.status = undefined
  pagination.current = 1
  applyFilters()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyFilters()
}

/** 开关禁用原因（自锁保护 / 菜单已在平台侧停用） */
function visibleTip(record: SetMenuConfigItem): string {
  if (record.locked) return '「菜单配置」页自身不可隐藏（否则无法再进入本页恢复）'
  if (record.status !== 1) return '菜单已在平台侧停用，如需使用请先在菜单管理中启用'
  return record.visible ? '点击隐藏该菜单（仅对本租户生效）' : '点击显示该菜单（仅对本租户生效）'
}

/** 单个菜单显隐（即时写库；失败回滚开关状态） */
async function handleToggle(record: SetMenuConfigItem, checked: boolean | string | number) {
  const target = checked === true
  const previous = record.visible
  if (target === previous) return
  togglingId.value = String(record.id)
  // 先落 UI：记录已被 a-switch 改成新值，这里把数据源同步成同一状态
  record.visible = target
  try {
    await setMenuConfigApi.updateVisible(String(record.id), target)
    message.success(target ? `已显示「${record.menuName}」` : `已隐藏「${record.menuName}」`)
  } catch (error: any) {
    record.visible = previous
    message.error(error?.response?.data?.message || error?.message || '设置失败')
  } finally {
    togglingId.value = null
  }
}

/** 本组全部显示：把当前分组里已隐藏且可操作的菜单逐个恢复显示 */
function handleShowAllInDomain() {
  const rows = allRows.value.filter(
    row => String(row.domainId) === selectedDomainId.value && !row.locked && row.status === 1 && !row.visible
  )
  if (!rows.length) {
    message.info('当前分组没有已隐藏的菜单')
    return
  }
  Modal.confirm({
    title: '本组全部显示',
    content: `确定要显示「${selectedDomainName.value}」下已隐藏的 ${rows.length} 个菜单吗？`,
    okText: '确定显示',
    onOk: async () => {
      bulkLoading.value = true
      let success = 0
      let failure = 0
      for (const row of rows) {
        try {
          await setMenuConfigApi.updateVisible(String(row.id), true)
          row.visible = true
          success++
        } catch {
          failure++
        }
      }
      bulkLoading.value = false
      if (failure) {
        message.warning(`处理完成：成功 ${success} 个，失败 ${failure} 个`)
      } else {
        message.success(`已显示 ${success} 个菜单`)
      }
    }
  })
}

function handleError(error: Error) {
  console.error('[菜单配置] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  fetchList()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
/* 查询区必须横向自适应网格（禁止纵向单列） */
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-menu-name { font-weight: 500; }
.cell-code { font-family: Consolas, Monaco, monospace; font-size: 12px; color: #555; }
.cell-empty { color: #bbb; }
.cell-switch { display: inline-flex; align-items: center; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
