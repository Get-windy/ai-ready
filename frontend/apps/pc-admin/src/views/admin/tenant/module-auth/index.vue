<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        模块授权（系统 → 租户管理 → 模块授权，菜单 62004）
        · 平台控制台页面：ql361 无对标（系统模块是平台级，ql361 是租户级产品）
        · 页面形态是「先选租户 → 再勾选该租户可用菜单 → 保存」的授权工作台，**不是表格页**，
          因此下方 #table 插槽里放的是授权勾选区（菜单卡片/勾选列表），不是 BillDetailTable
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/模块授权开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏左侧：保存授权（主操作）+ 未保存提示 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('save')"
            type="primary"
            size="small"
            :disabled="!selectedTenantId"
            :loading="saving"
            @click="handleSave"
          >
            <SaveOutlined /> 保存授权
          </a-button>
          <a-tag
            v-if="dirty"
            color="orange"
          >
            有未保存的修改
          </a-tag>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading || tenantsLoading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：租户选择 + 菜单关键字搜索（关键字为实时过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('tenant')">
                <span class="search-label">租户</span>
                <a-select
                  :value="selectedTenantId"
                  placeholder="请选择租户"
                  size="small"
                  style="width: 260px"
                  show-search
                  allow-clear
                  :loading="tenantsLoading"
                  :filter-option="filterTenantOption"
                  :options="tenantOptions"
                  @change="handleTenantChange"
                />
              </template>
              <template v-if="isQueryFieldVisible('keyword')">
                <span class="search-label">菜单名称/编码</span>
                <a-input
                  v-model:value="keyword"
                  placeholder="输入菜单名称或编码过滤"
                  size="small"
                  style="width: 220px"
                  allow-clear
                />
              </template>
              <a-button
                size="small"
                :disabled="!keyword"
                @click="handleReset"
              >
                重置
              </a-button>
              <span class="search-label">已选 {{ selectedMenuIds.length }} / 共 {{ menus.length }}</span>
            </div>
          </div>
        </template>

        <!-- ═══ 授权勾选区（替代表格；滚动容器高度自适应） ═══ -->
        <template #table>
          <div class="auth-area">
            <!-- 统计 + 批量勾选（作用于「当前筛选结果」） -->
            <div class="auth-bar">
              <span class="auth-stat">
                已选 <b>{{ selectedMenuIds.length }}</b> / 共 <b>{{ menus.length }}</b> 项
                <template v-if="keyword">
                  （筛选后 {{ filteredMenus.length }} 项）
                </template>
              </span>
              <a-space :size="8">
                <a-button
                  v-if="isButtonEnabled('selectAll')"
                  size="small"
                  :disabled="!filteredMenus.length"
                  @click="toggleAllFiltered(true)"
                >
                  全选当前结果
                </a-button>
                <a-button
                  v-if="isButtonEnabled('selectAll')"
                  size="small"
                  :disabled="!filteredMenus.length"
                  @click="toggleAllFiltered(false)"
                >
                  全不选当前结果
                </a-button>
              </a-space>
            </div>

            <div class="auth-scroll">
              <a-spin :spinning="loading">
                <a-empty
                  v-if="!selectedTenantId"
                  description="请先选择一个租户"
                />
                <a-empty
                  v-else-if="!menus.length && !loading"
                  description="该租户暂无可授权的菜单"
                />
                <a-empty
                  v-else-if="!filteredMenus.length && !loading"
                  description="没有匹配的菜单"
                />
                <a-checkbox-group
                  v-else
                  v-model:value="selectedMenuIds"
                  class="menu-grid"
                >
                  <!--
                    ⚠️ 取值必须是 SysMenu 的真实字段：`menuName`（标题）/ `id`（勾选值）。
                    原实现用的 `moduleName` / `moduleCode` 在 cn.aiedge.base.entity.SysMenu
                    中**不存在** → 卡片标题恒空白、勾选值恒 undefined。
                    live 实测 GET /api/menu/list?tenantId=1 返回 381 条，键名为
                    {"id":"90006","menuName":"职位管理","menuCode":"hr:position",...}，
                    且 id 因 JacksonConfig（Long→String）是**字符串**。
                  -->
                  <a-checkbox
                    v-for="m in filteredMenus"
                    :key="m.id"
                    :value="String(m.id)"
                    class="menu-item"
                  >
                    <span class="menu-name">{{ m.menuName || '(未命名菜单)' }}</span>
                    <a-tag
                      class="menu-type"
                      :color="MENU_TYPE_COLOR[m.menuType] || 'default'"
                    >
                      {{ MENU_TYPE_MAP[m.menuType] || '其他' }}
                    </a-tag>
                    <span class="menu-code">{{ m.menuCode }}</span>
                  </a-checkbox>
                </a-checkbox-group>
              </a-spin>
            </div>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="system-tenant-module-auth-page-config"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  SaveOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { tenantApi } from '@/api/tenant'
import { menuApi, type MenuInfo } from '@/api/menu'

defineOptions({ name: 'AdminTenantModuleAuth' })

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 菜单类型（SysMenu.menuType）：0=目录 1=菜单 2=按钮 */
const MENU_TYPE_MAP: Record<number, string> = { 0: '目录', 1: '菜单', 2: '按钮' }
const MENU_TYPE_COLOR: Record<number, string> = { 0: 'default', 1: 'blue', 2: 'purple' }

/** 租户下拉的一行（字段与 sys_tenant 对齐；id 为字符串） */
interface TenantRow { id: string; tenantName: string; tenantCode: string }
/** 租户下拉选项（携带自定义字段供本地过滤） */
interface TenantOption { label: string; value: string; tenantName: string; tenantCode: string }

// ═══ 状态 ═══
const tenants = ref<TenantRow[]>([])
const tenantsLoading = ref(false)
/** 所选租户 ID：雪花 ID 一律按字符串持有，避免 Number() 精度丢失 */
const selectedTenantId = ref<string | undefined>(undefined)

const menus = ref<MenuInfo[]>([])
const loading = ref(false)
/** 勾选集合：值一律是 String(menu.id) */
const selectedMenuIds = ref<string[]>([])
/** 服务端基线（上次读到的已授权集合），用于判定 dirty */
const authorizedIds = ref<string[]>([])

const saving = ref(false)
const keyword = ref('')

// ═══ 计算 ═══
const tenantOptions = computed<TenantOption[]>(() =>
  tenants.value.map(t => ({
    label: `${t.tenantName} (${t.tenantCode})`,
    value: String(t.id),
    tenantName: t.tenantName,
    tenantCode: t.tenantCode,
  })),
)

/** 按名称/编码实时过滤（381 条候选必须能筛） */
const filteredMenus = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return menus.value
  return menus.value.filter(m =>
    String(m.menuName || '').toLowerCase().includes(kw)
    || String(m.menuCode || '').toLowerCase().includes(kw),
  )
})

/** 勾选与基线是否不一致（判断「有未保存的修改」） */
const dirty = computed(() => {
  if (!selectedTenantId.value) return false
  const a = new Set(authorizedIds.value)
  const b = new Set(selectedMenuIds.value)
  if (a.size !== b.size) return true
  for (const id of a) if (!b.has(id)) return true
  return false
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'tenant', label: '租户', visible: true },
  { key: 'keyword', label: '菜单名称/编码', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存授权', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'selectAll', label: '全选/全不选', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

/**
 * 统一提取错误文案（避免全文散落 `any`）。
 * 拦截器会把原始 axios 文案（如 "Request failed with status code 400"）作为 Error.message 抛出，
 * 这类英文/状态码文案对用户无意义 → 一律换成调用方给的中文兜底；
 * 后端返回的业务文案（如「缺少必要参数: tenantId」）则原样透出。
 */
function errMsg(error: unknown, fallback: string): string {
  if (error instanceof Error && error.message && !/^Request failed/.test(error.message)) {
    return error.message
  }
  return fallback
}

/** 租户下拉本地过滤：同时匹配租户名称与编码 */
function filterTenantOption(input: string, option: unknown) {
  const kw = String(input || '').toLowerCase()
  const o = option as TenantOption | undefined
  return String(o?.tenantName || '').toLowerCase().includes(kw)
    || String(o?.tenantCode || '').toLowerCase().includes(kw)
}

// ═══ 数据加载 ═══
/**
 * 租户下拉数据源：GET /api/tenant/page。
 * ⚠️ 拦截器已把 Result 拆包 → 拿到的是 Page 本体（{ records, total }），
 *    旧代码读 `res.data.records` 恒 undefined（res.data 是 Page 的 data 字段，不存在）。
 */
async function fetchTenants() {
  tenantsLoading.value = true
  try {
    const res = await tenantApi.getPage({ pageNum: 1, pageSize: 200 }) as unknown as
      { records?: TenantRow[]; data?: { records?: TenantRow[] } }
    const page = res?.records ? res : (res?.data || {})
    tenants.value = page.records || []
  } catch (error: unknown) {
    console.error('[模块授权] 加载租户列表失败', error)
    message.error(errMsg(error, '加载租户列表失败'))
    tenants.value = []
  } finally {
    tenantsLoading.value = false
  }
}

/** 读取某租户已授权菜单 ID（统一归一为字符串） */
async function fetchAuthorizedIds(tenantId: string): Promise<string[]> {
  const res = await menuApi.getTenantMenuIds(tenantId) as unknown
  const list: unknown[] = Array.isArray(res)
    ? res
    : (Array.isArray((res as { data?: unknown[] })?.data) ? (res as { data: unknown[] }).data : [])
  return list.map(v => String(v))
}

/**
 * 加载某租户的候选菜单 + 已授权回显。
 * 候选来自 GET /api/menu/list?tenantId=xxx（tenantId 为**必填**参数，缺参 400）。
 */
async function loadTenantAuth(tenantId: string) {
  loading.value = true
  try {
    const [candidates, authorized] = await Promise.all([
      menuApi.getList(tenantId),
      fetchAuthorizedIds(tenantId),
    ])
    menus.value = Array.isArray(candidates) ? candidates : []
    authorizedIds.value = authorized
    selectedMenuIds.value = [...authorized]
  } catch (error: unknown) {
    console.error('[模块授权] 加载授权数据失败', error)
    message.error(errMsg(error, '加载授权数据失败'))
    menus.value = []
    authorizedIds.value = []
    selectedMenuIds.value = []
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
/**
 * 切换租户：候选与勾选**全部重算**，绝不留上一次的勾选。
 * 若当前有未保存的修改，先二次确认（避免静默丢弃用户操作）。
 */
async function handleTenantChange(val: unknown) {
  const next = (val === null || val === undefined || val === '') ? undefined : String(val)
  if (next === selectedTenantId.value) return

  if (dirty.value) {
    const ok = await confirmModal(
      '放弃未保存的勾选?',
      '切换到其它租户会丢弃当前未保存的勾选内容，是否继续?',
    )
    if (!ok) return // 不改 selectedTenantId，a-select 受控回弹到原值
  }

  selectedTenantId.value = next
  keyword.value = ''
  menus.value = []
  selectedMenuIds.value = []
  authorizedIds.value = []
  if (next) await loadTenantAuth(next)
}

function handleReset() {
  keyword.value = ''
}

function handleRefresh() {
  fetchTenants()
  if (selectedTenantId.value) loadTenantAuth(selectedTenantId.value)
}

/** 全选 / 全不选**当前筛选结果**；筛选之外已勾选的项不受影响 */
function toggleAllFiltered(checked: boolean) {
  const s = new Set(selectedMenuIds.value)
  filteredMenus.value.forEach(m => {
    const id = String(m.id)
    if (checked) s.add(id)
    else s.delete(id)
  })
  selectedMenuIds.value = [...s]
}

function confirmModal(title: string, content: string): Promise<boolean> {
  return new Promise(resolve => {
    Modal.confirm({
      title,
      content,
      okText: '确定',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

// ═══ 保存授权 ═══
/**
 * 保存 = PUT /api/tenant-menu/{tenantId}（body 为菜单 ID 数组，全量覆盖）。
 *
 * ⚠️ 后端 `assignMenus` 在**空数组时直接 return**（不落库、不报错），
 *    但 Controller 仍返回 `Result.ok("授权成功")` → 无条件弹 success 就是**谎报成功**。
 *    因此这里做两件事：① 空数组先二次确认；② 提交后**回读一次**，只有回读与提交一致才报成功。
 *
 * 🔴 2026-09-18 live 实测（本页写入链路当前是坏的，后端缺陷，前端不掩盖）：
 *    · 空数组：PUT 返回 200「授权成功」，但一行都没写（早退）；
 *    · 非空数组：首次 PUT 返回 200，但行落在**会话租户**（tenant_id=1）而非路径参数租户（2）
 *      —— 即「给租户 2 授权」写到了租户 1；
 *    · 再次 PUT 同一租户 → 400「请求数据不完整或存在冲突」
 *      （`uk_tenant_menu(tenant_id, menu_id)` 唯一索引不含 `deleted`，与软删语义冲突）。
 *    回读校验因此是必需品：它把上述情况如实报成「后端未变更授权」，而不是假报成功。
 */
async function handleSave() {
  const tenantId = selectedTenantId.value
  if (!tenantId) {
    message.warning('请先选择租户')
    return
  }

  if (!selectedMenuIds.value.length) {
    const ok = await confirmModal(
      '确定提交空授权?',
      '当前未勾选任何菜单。后端收到空数组时不会做任何变更（不会清空该租户已有授权），是否仍要提交?',
    )
    if (!ok) return
  }

  // 保持字符串 ID 原样提交：Jackson 可把 ["80601"] 反序列化为 List<Long>，
  // 且避免 Number() 对雪花 ID 的精度丢失
  const payload = [...selectedMenuIds.value]
  const submitted = new Set(payload)

  saving.value = true
  try {
    await menuApi.assignTenantMenus(tenantId, payload)
  } catch (error: unknown) {
    console.error('[模块授权] 保存授权失败', error)
    message.error(errMsg(error, '授权保存失败'))
    saving.value = false
    return
  }

  // 回读校验
  try {
    const actualList = await fetchAuthorizedIds(tenantId)
    const actual = new Set(actualList)
    const same = submitted.size === actual.size && [...submitted].every(id => actual.has(id))

    if (same) {
      authorizedIds.value = actualList
      selectedMenuIds.value = [...actualList]
      if (!payload.length) {
        // 空提交且回读也是空的：后端早退，授权未发生变更 —— 如实说明，不谎报「已保存」
        message.info('已提交空授权；后端对空数组不落库，授权未发生变更')
      } else {
        message.success(`授权已保存（共 ${payload.length} 项）`)
      }
    } else {
      // 以服务端为准，消除本地假象
      authorizedIds.value = actualList
      selectedMenuIds.value = [...actualList]
      message.warning(
        `后端未变更授权（提交 ${submitted.size} 项，回读 ${actual.size} 项），已按服务端数据刷新`,
      )
    }
  } catch (error: unknown) {
    console.error('[模块授权] 保存后回读校验失败', error)
    message.error('授权已提交，但回读校验失败，请点「刷新」确认实际结果')
  } finally {
    saving.value = false
  }
}

function handleError(error: Error) {
  console.error('[模块授权] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  // 候选菜单依赖所选租户（/menu/list 的 tenantId 必填），故此处只加载租户下拉
  fetchTenants()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* 授权区：flex 纵向容器，滚动区 flex:1 + min-height:0 才能正确自适应高度 */
.auth-area { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.auth-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
}
.auth-stat { font-size: 13px; color: #666; }
.auth-stat b { color: #1890ff; }
.auth-scroll { flex: 1; min-height: 0; overflow: auto; padding: 12px 16px; }

/* 菜单勾选网格：自适应列宽 */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 8px;
  width: 100%;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  line-height: 20px;
  overflow: hidden;
}
.menu-item:hover { border-color: #91d5ff; background: #f5faff; }
:deep(.menu-item.ant-checkbox-wrapper-checked) { border-color: #1890ff; background: #e6f7ff; }
.menu-name { font-weight: 500; font-size: 13px; }
.menu-type { flex-shrink: 0; margin-inline-end: 0; font-size: 11px; line-height: 16px; }
.menu-code {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
