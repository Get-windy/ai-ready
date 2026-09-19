<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="role-page-header">
          <div class="role-page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>角色管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="role-page-header-title">
              角色管理
            </h2>
          </div>
          <div class="role-page-header-right">
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <span
              v-if="autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
              <span class="shortcut-hint">双击行查看详情</span>
            </span>
            <a-button
              size="small"
              :loading="refreshLoading"
              @click="debounceClick('refresh', fetchData)"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <div
        ref="tableWrap"
        class="role-management"
      >
        <!-- 统计卡片 -->
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ pagination.total }}
              </div>
              <div class="stat-card-label">
                角色总数
              </div>
            </div>
            <SafetyOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-active">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ activeCount }}
              </div>
              <div class="stat-card-label">
                启用角色
              </div>
            </div>
            <CheckCircleOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-disabled">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ disabledCount }}
              </div>
              <div class="stat-card-label">
                停用角色
              </div>
            </div>
            <StopOutlined class="stat-card-icon" />
          </div>
        </div>

        <a-skeleton
          v-if="loading && tableData.length === 0"
          active
          :paragraph="{ rows: 8 }"
          style="padding: 24px;"
        />

        <BillTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableDataSource"
          :loading="loading"
          :pagination="pagination"
          :row-key="'id'"
          :min-empty-rows="12"
          :filter-fields="filterFields"
          :show-search="false"
          :selectable="false"
          add-text="新增角色"
          add-permission="system:role:create"
          @add="handleAdd"
          @edit="handleEdit"
          @delete="handleDeleteConfirm"
          @refresh="debounceClick('refresh', fetchData)"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
        >
          <template #empty>
            <a-empty
              v-if="!hasError"
              description="暂无数据"
            />
            <a-result
              v-else
              status="error"
              title="数据加载失败"
            >
              <template #extra>
                <a-button
                  type="primary"
                  @click="debounceClick('refresh', fetchData)"
                >
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重新加载
                </a-button>
              </template>
            </a-result>
          </template>

          <template #roleNameCell="{ record }">
            <a-space>
              <a-tag :color="getRoleTypeColor(record.roleType)">
                {{ getRoleTypeName(record.roleType) }}
              </a-tag>
              <span>{{ record.roleName }}</span>
            </a-space>
          </template>
          <template #scopeCell="{ record }">
            <a-tag :color="record.scope === 'PLATFORM' ? 'purple' : 'blue'">
              {{ record.scope === 'PLATFORM' ? '平台级' : '租户级' }}
            </a-tag>
          </template>
          <template #statusCell="{ record }">
            <a-switch
              :checked="record.status === 0"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: string | boolean) => { if (typeof checked === 'boolean') handleStatusChange(record, checked) }"
            />
          </template>

          <template #action="{ record }">
            <a-space>
              <a-button
                v-permission="'system:role:update'"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-permission="'system:permission:assign'"
                type="link"
                size="small"
                @click="handlePermission(record)"
              >
                权限
              </a-button>
              <a-button
                v-permission="'system:role:update'"
                type="link"
                size="small"
                @click="handleMenu(record)"
              >
                菜单
              </a-button>
              <a-button
                v-permission="'system:role:update'"
                type="link"
                size="small"
                @click="handleBillType(record)"
              >
                单据权限
              </a-button>
              <a-button
                v-permission="'system:role:delete'"
                type="link"
                size="small"
                danger
                @click="handleDeleteConfirm(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </BillTableList>

        <!-- 角色表单弹窗 -->
        <FullScreenDetail
          :visible="modalVisible"
          :title="modalTitle"
          :dirty="formDirty"
          :save-loading="submittingLoading"
          :show-save-and-new="!isEdit"
          @save="handleModalOk"
          @close="handleFormClose"
          @save-and-new="handleFormSaveAndNew"
        >
          <a-form
            ref="formRef"
            :model="formState"
            :rules="formRules"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item
              label="角色名称"
              name="roleName"
            >
              <a-input
                v-model:value="formState.roleName"
                placeholder="请输入角色名称"
              />
            </a-form-item>
            <a-form-item
              label="角色编码"
              name="roleCode"
            >
              <a-input
                v-model:value="formState.roleCode"
                placeholder="请输入角色编码"
                :disabled="isEdit"
              />
            </a-form-item>
            <a-form-item
              label="角色类型"
              name="roleType"
            >
              <a-select
                v-model:value="formState.roleType"
                size="small"
                placeholder="请选择角色类型"
              >
                <a-select-option
                  v-for="opt in roleTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item
              label="作用域"
              name="scope"
            >
              <a-select
                v-model:value="formState.scope"
                size="small"
                placeholder="请选择作用域"
              >
                <a-select-option value="TENANT">
                  租户级
                </a-select-option>
                <a-select-option value="PLATFORM">
                  平台级
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item
              label="排序"
              name="sort"
            >
              <a-input-number
                v-model:value="formState.sort"
                :min="0"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item
              label="状态"
              name="status"
            >
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="0">
                  正常
                </a-radio>
                <a-radio :value="1">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item
              label="备注"
              name="remark"
            >
              <a-textarea
                v-model:value="formState.remark"
                placeholder="请输入备注"
                :rows="3"
              />
            </a-form-item>
          </a-form>
        </FullScreenDetail>

        <!-- ═══ 角色详情（只读）═══ -->
        <!-- 双击行打开（见下方 useRowDblclick）；:show-footer="false" → 无保存/保存并新增按钮，纯只读查看，不提交任何写操作 -->
        <FullScreenDetail
          :visible="viewVisible"
          :title="viewTitle"
          :show-footer="false"
          @close="handleViewClose"
        >
          <a-skeleton
            v-if="viewLoading"
            active
            :paragraph="{ rows: 8 }"
            style="padding: 8px;"
          />

          <a-result
            v-else-if="viewError"
            status="error"
            title="角色详情加载失败"
            :sub-title="viewErrorMessage"
          >
            <template #extra>
              <a-button
                type="primary"
                @click="debounceClick('viewReload', reloadView)"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
                重新加载
              </a-button>
            </template>
          </a-result>

          <template v-else-if="viewDetail">
            <!-- 基本信息：字段口径对齐后端 SysRole 实体（GET /api/role/{id} 直接返回 sys_role 行） -->
            <a-descriptions
              :column="2"
              size="small"
              bordered
            >
              <a-descriptions-item label="角色名称">
                {{ viewDetail.roleName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="角色编码">
                {{ viewDetail.roleCode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="角色类型">
                <a-tag
                  v-if="viewRoleType"
                  :color="viewRoleType.color"
                >
                  {{ viewRoleType.label }}
                </a-tag>
                <span v-else>-</span>
              </a-descriptions-item>
              <a-descriptions-item label="作用域">
                <a-tag :color="viewDetail.scope === 'PLATFORM' ? 'purple' : 'blue'">
                  {{ viewDetail.scope === 'PLATFORM' ? '平台级' : (viewDetail.scope === 'TENANT' ? '租户级' : '-') }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="数据范围">
                {{ viewDataScopeLabel }}
              </a-descriptions-item>
              <a-descriptions-item label="排序">
                {{ viewDetail.sort ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="viewDetail.status === 0 ? 'success' : 'default'">
                  {{ viewStatusText }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="角色ID">
                {{ viewDetail.id }}
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ viewDetail.createTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="更新时间">
                {{ viewDetail.updateTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ viewDetail.remark || '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <!-- 已分配权限：GET /api/role/{id}/permissions（返回 sys_permission.id 清单，下方按模块分组还原权限码） -->
            <div class="detail-section-header">
              <span class="detail-section-title">已分配权限</span>
              <span class="detail-section-count">共 {{ viewPermissionIds.length }} 项</span>
              <span
                v-if="viewPermOptionsLoading"
                class="detail-section-hint"
              >权限名称解析中…</span>
            </div>

            <a-alert
              v-if="viewPermOptionsError && viewPermissionIds.length > 0"
              type="warning"
              show-icon
              message="权限名称解析失败"
              description="未能解析的项仅显示权限 ID，请检查网络或权限后重试"
              style="margin-bottom: 8px"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="debounceClick('viewPermReload', loadPermissionOptions)"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <a-empty
              v-if="viewPermissionIds.length === 0"
              description="该角色暂无已分配权限"
            />
            <div
              v-else
              class="detail-perm-groups"
            >
              <div
                v-for="group in viewPermissionGroups"
                :key="group.module"
                class="detail-perm-group"
              >
                <div class="detail-perm-module">
                  模块：{{ group.module }}（{{ group.items.length }}）
                </div>
                <div class="detail-perm-tags">
                  <a-tooltip
                    v-for="item in group.items"
                    :key="item.id"
                    :title="item.name || `权限ID ${item.id}`"
                    placement="bottom"
                  >
                    <a-tag>{{ item.code || `#${item.id}` }}</a-tag>
                  </a-tooltip>
                </div>
              </div>
            </div>

            <!-- 已分配菜单：GET /api/role/{id}/menus（返回 sys_menu.id 清单，下方用菜单树还原层级与名称） -->
            <div class="detail-section-header">
              <span class="detail-section-title">已分配菜单</span>
              <span class="detail-section-count">共 {{ viewMenuIds.length }} 项</span>
              <span
                v-if="viewMenuTreeLoading"
                class="detail-section-hint"
              >菜单名称解析中…</span>
            </div>

            <a-alert
              v-if="viewMenuTreeError && viewMenuIds.length > 0"
              type="warning"
              show-icon
              message="菜单名称解析失败"
              description="下面仅显示菜单 ID，请检查网络后重试"
              style="margin-bottom: 8px"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="debounceClick('viewMenuReload', loadMenuTreeForView)"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <a-empty
              v-if="viewMenuIds.length === 0"
              description="该角色暂无已分配菜单"
            />
            <a-tree
              v-else-if="viewMenuTree.length > 0"
              :tree-data="viewMenuTree"
              :checkable="false"
              :selectable="false"
              :default-expand-all="true"
            />
            <div
              v-else
              class="detail-id-list"
            >
              <a-tag
                v-for="id in viewMenuIds"
                :key="id"
              >
                #{{ id }}
              </a-tag>
            </div>
          </template>
        </FullScreenDetail>

        <!-- 权限配置弹窗 -->
        <a-modal
          v-model:open="permissionModalVisible"
          title="配置权限"
          width="500px"
          :confirm-loading="permissionLoading"
          @ok="handlePermissionOk"
        >
          <a-alert
            message="勾选需要分配给该角色的权限"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-tree
            v-model:checked-keys="checkedPermissionKeys"
            :tree-data="permissionTree"
            checkable
            :default-expand-all="true"
            :selectable="false"
          />
        </a-modal>

        <!-- 菜单配置弹窗 -->
        <a-modal
          v-model:open="menuModalVisible"
          title="配置菜单"
          width="500px"
          :confirm-loading="menuLoading"
          @ok="handleMenuOk"
        >
          <a-alert
            message="勾选需要分配给该角色的菜单"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-tree
            v-model:checked-keys="checkedMenuKeys"
            :tree-data="menuTree"
            checkable
            :default-expand-all="true"
            :selectable="false"
          />
        </a-modal>

        <!-- 单据类型权限配置弹窗 -->
        <a-modal
          v-model:open="billTypeModalVisible"
          title="配置单据类型权限"
          width="550px"
          :confirm-loading="billTypeLoading"
          @ok="handleBillTypeOk"
        >
          <a-alert
            message="设置角色对每种单据类型的操作权限级别"
            type="info"
            show-icon
            style="margin-bottom: 16px"
          />
          <a-table
            :data-source="billTypeData"
            :columns="billTypeColumns"
            :pagination="false as any"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-select
                  v-model:value="record.permissionLevel"
                  style="width: 120px"
                  size="small"
                >
                  <a-select-option :value="0">
                    无权限
                  </a-select-option>
                  <a-select-option :value="1">
                    查看
                  </a-select-option>
                  <a-select-option :value="2">
                    编辑
                  </a-select-option>
                  <a-select-option :value="3">
                    审核
                  </a-select-option>
                </a-select>
              </template>
            </template>
          </a-table>
        </a-modal>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRowDblclick } from '@/composables/useRowDblclick'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { SafetyOutlined, CheckCircleOutlined, StopOutlined, ReloadOutlined, SyncOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { roleApi, type RoleInfo } from '@/api/role'
import menuApi from '@/api/menu'
import { roleBillTypeApi, type BillTypeDetail } from '@/api/roleBillType'
import { permissionApi } from '@/api/permission'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'
import BillTableList, { type FilterField } from '@/components/BillTableList/BillTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const searchForm = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined })

const tableData = ref<RoleInfo[]>([])
const loading = ref(false)

const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'roleName', title: '角色信息', width: 200, slotName: 'roleNameCell' },
  { field: 'roleCode', title: '角色编码', width: 150 },
  { field: 'scope', title: '作用域', width: 100, slotName: 'scopeCell' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'remark', title: '备注', showOverflow: 'tooltip' },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

const filterFields: FilterField[] = [
  { key: 'roleName', label: '角色名称', type: 'input', placeholder: '请输入角色名称' },
  { key: 'roleCode', label: '角色编码', type: 'input', placeholder: '请输入角色编码' },
  { key: 'scope', label: '作用域', type: 'select', options: [{ label: '平台级', value: 'PLATFORM' }, { label: '租户级', value: 'TENANT' }] },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
]

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// ── 角色类型选项（从API加载） ──────────────────────────
const roleTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadRoleTypeOptions() {
  try {
    const res = await dictItemApi.getByDictCode('ROLE_TYPE')
    if (res.data) {
      roleTypeOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[角色管理] 加载角色类型失败', err)
  }
}

const formState = reactive({ id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', sort: 0, status: 0, remark: '' })
const formRules: any = {
  roleName: { required: true, message: '请输入角色名称', trigger: 'blur' },
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }, { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }]
}

const permissionModalVisible = ref(false)
const permissionLoading = ref(false)
const checkedPermissionKeys = ref<number[]>([])
const permissionTree = ref<any[]>([])
const currentRoleId = ref(0)

const menuModalVisible = ref(false)
const menuLoading = ref(false)
const checkedMenuKeys = ref<number[]>([])
const menuTree = ref<any[]>([])

// ── 单据类型权限弹窗 ────────────────────────────────────────
const billTypeModalVisible = ref(false)
const billTypeLoading = ref(false)
const billTypeData = ref<BillTypeDetail[]>([])
const billTypeColumns = [
  { title: '单据类型', dataIndex: 'billTypeName', key: 'billTypeName', width: 150 },
  { title: '类型编码', dataIndex: 'billType', key: 'billType', width: 100 },
  { title: '权限级别', key: 'level', width: 180 },
]

// ── debounceClick ──────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: () => void) {
  if (clickLocks.get(key)) return
  clickLocks.set(key, true)
  try { fn() } finally { setTimeout(() => clickLocks.set(key, false), 300) }
}

// ── Keyboard shortcuts ─────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) { e.preventDefault(); debounceClick('refresh', fetchData) }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

// ── Form dirty tracking ────────────────────────────────────
const initialFormSnapshot = ref('')
const watchReady = ref(false)
const formDirty = computed(() => {
  if (!watchReady.value) return false
  return initialFormSnapshot.value !== JSON.stringify(formState)
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formState)
}

onBeforeRouteLeave((to, from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开', content: '当前表单未保存，确定要离开吗？', okText: '确定', cancelText: '取消',
      onOk() { next() }, onCancel() { next(false) }
    })
  } else { next() }
})

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await roleApi.getPage({ tenantId: userStore.tenantId, ...searchForm, current: pagination.current, size: pagination.pageSize } as any)
    if (res) { tableData.value = res.records || []; pagination.total = res.total || 0 }
  } catch (err) {
    hasError.value = true
    console.warn('[系统管理] 加载角色数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(searchForm, { roleName: '', roleCode: '', status: undefined })
  else Object.assign(searchForm, filters)
  pagination.current = 1; fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', sort: 0, status: 0, remark: '' })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleEdit = (record: RoleInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, roleName: record.roleName, roleCode: record.roleCode, roleType: record.roleType, scope: record.scope || 'TENANT', sort: record.sort, status: record.status, remark: record.remark })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      if (isEdit.value) { await roleApi.update(formState.id, formState); message.success('更新成功') }
      else { await roleApi.create(formState); message.success('创建成功') }
      modalVisible.value = false; fetchData()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭', content: '当前表单未保存，确定要关闭吗？', okText: '确定', cancelText: '取消',
      onOk() { modalVisible.value = false; watchReady.value = false; formRef.value?.resetFields() }
    })
  } else {
    modalVisible.value = false
    watchReady.value = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      await roleApi.create(formState)
      message.success('创建成功')
      fetchData()
      handleAdd()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleDeleteConfirm = (record: RoleInfo) => {
  Modal.confirm({
    title: '确认删除', content: `确定要删除角色 "${record.roleName}" 吗？此操作不可撤销。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await roleApi.delete(record.id); message.success('删除成功'); fetchData() } catch (error: any) { message.error(error.message || '删除失败') }
    }
  })
}

const handleStatusChange = async (record: RoleInfo, checked: boolean) => {
  const newStatus = checked ? 0 : 1
  try { await roleApi.updateStatus(record.id, newStatus); message.success('状态更新成功'); fetchData() } catch (err) { console.warn('[系统管理] 更新角色状态失败', err); message.error('状态更新失败') }
}

const handlePermission = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const menuRes = await menuApi.getTree({})
    permissionTree.value = menuRes ? buildPermissionTree(menuRes) : []
  } catch (err) {
    permissionTree.value = []
    console.warn('[系统管理] 加载权限树失败', err)
    message.error('加载权限树失败')
  }
  try { const res = await roleApi.getPermissions(record.id); checkedPermissionKeys.value = res.data || [] } catch (err) { console.warn('[系统管理] 获取权限列表失败', err); checkedPermissionKeys.value = [] }
  permissionModalVisible.value = true
}

const buildPermissionTree = (menus: any[]): any[] => {
  return menus.map((m: any) => ({
    title: m.menuName,
    key: m.id,
    children: m.children?.length ? buildPermissionTree(m.children) : undefined
  }))
}

const handlePermissionOk = async () => {
  permissionLoading.value = true
  try { await roleApi.assignPermissions(currentRoleId.value, checkedPermissionKeys.value as number[]); message.success('权限配置成功'); permissionModalVisible.value = false } finally { permissionLoading.value = false }
}

const handleMenu = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const menuRes = await menuApi.getTree({})
    menuTree.value = menuRes ? buildPermissionTree(menuRes) : []
  } catch (err) {
    menuTree.value = []
    console.warn('[系统管理] 加载菜单树失败', err)
    message.error('加载菜单树失败')
  }
  try { const res = await roleApi.getMenus(record.id); checkedMenuKeys.value = res.data || [] } catch (err) { console.warn('[系统管理] 获取菜单列表失败', err); checkedMenuKeys.value = [] }
  menuModalVisible.value = true
}

const handleMenuOk = async () => {
  menuLoading.value = true
  try { await roleApi.assignMenus(currentRoleId.value, checkedMenuKeys.value as number[]); message.success('菜单配置成功'); menuModalVisible.value = false } finally { menuLoading.value = false }
}

const handleBillType = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const res = await roleBillTypeApi.getRoleBillTypes(record.id)
    billTypeData.value = res.data || []
  } catch (err) {
    console.warn('[系统管理] 加载单据类型权限失败', err)
    message.error('加载单据类型权限失败')
    billTypeData.value = []
  }
  billTypeModalVisible.value = true
}

const handleBillTypeOk = async () => {
  billTypeLoading.value = true
  try {
    const assignments = billTypeData.value
      .filter(d => d.permissionLevel > 0)
      .map(d => ({ billType: d.billType, permissionLevel: d.permissionLevel }))
    await roleBillTypeApi.assignBillTypes(currentRoleId.value, assignments)
    message.success('单据类型权限配置成功')
    billTypeModalVisible.value = false
  } catch (err) {
    console.warn('[系统管理] 分配单据类型权限失败', err)
    message.error('分配失败')
  } finally {
    billTypeLoading.value = false
  }
}

const roleTypeColorMap: Record<number, string> = { 0: 'blue', 1: 'green' }
const getRoleTypeColor = (type: number) => roleTypeColorMap[type] || 'default'
const getRoleTypeName = (type: number) => {
  const found = roleTypeOptions.value.find(o => o.value === type)
  return found ? found.label : '未知'
}

onMounted(() => {
  fetchData()
  loadRoleTypeOptions()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }

// ==================== 角色详情（只读） ====================
// 容器复用本页既有的 FullScreenDetail（:show-footer="false" → 无保存按钮，纯只读，不提交/不删除）
// 三块数据全部取自真实接口，且不复用列表行快照：
//   1. 基本信息   GET /api/role/{id}               （后端直接返回 sys_role 行）
//   2. 已分配权限 GET /api/role/{id}/permissions    （返回 sys_permission.id 清单）
//   3. 已分配菜单 GET /api/role/{id}/menus          （返回 sys_menu.id 清单）
// 名称还原：权限码来自 GET /api/permission/page（sys_permission 属系统级共享表，接口按 tenantId 显式过滤，
// 角色可能同时挂着 tenant_id=0 的全局权限与本租户权限，故两段都要取）；菜单名来自 GET /api/menu/tree。
type RoleDetail = RoleInfo & { updateTime?: string }

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewError = ref(false)
const viewErrorMessage = ref('')
const viewDetail = ref<RoleDetail | null>(null)
/** 当前查看的角色 id：雪花 ID 按原值（字符串）透传，不做 Number() 转换 */
const viewRoleId = ref<string | number | null>(null)

const viewTitle = computed(() => viewDetail.value?.roleName
  ? `角色详情 - ${viewDetail.value.roleName}`
  : '角色详情')

// 数据范围口径对齐后端 SysRole.dataScope 注释（0-全部 1-本部门 2-本部门及以下 3-仅本人 4-自定义）
const DATA_SCOPE_LABELS: Record<number, string> = { 0: '全部数据', 1: '本部门', 2: '本部门及以下', 3: '仅本人', 4: '自定义' }
// 角色类型兜底口径对齐后端 SysRole.roleType 注释（0-系统角色 1-自定义角色）
// 说明：当前库里没有 ROLE_TYPE 字典（loadRoleTypeOptions 取不到值），详情里不再回落成「未知」而误报
const ROLE_TYPE_LABELS: Record<number, string> = { 0: '系统角色', 1: '自定义角色' }

const viewDataScopeLabel = computed(() => {
  const v = viewDetail.value?.dataScope
  if (v === null || v === undefined) return '-'
  return DATA_SCOPE_LABELS[v] || String(v)
})

const viewStatusText = computed(() => {
  const v = viewDetail.value?.status
  return v === 0 ? '正常' : (v === 1 ? '停用' : '-')
})

const viewRoleType = computed(() => {
  const v = viewDetail.value?.roleType
  if (v === null || v === undefined || v === '') return null
  const num = Number(v)
  const dictLabel = roleTypeOptions.value.find(o => o.value === num)?.label
  return { label: dictLabel || ROLE_TYPE_LABELS[num] || '未知', color: getRoleTypeColor(num) }
})

/**
 * 拆包：响应拦截器在成功时已把 Result 拆成 data 本体（见 utils/request.ts），
 * 因此运行时拿到的通常就是数据本身；这里同时兼容尚未拆包的 { data: ... } 形态。
 */
function unwrap<T>(res: unknown): T | null {
  if (res === null || res === undefined) return null
  const wrapped = res as { data?: T }
  return wrapped.data !== undefined ? wrapped.data : (res as T)
}

/** 归一化为字符串数组（雪花 ID 一律按字符串处理，禁止 Number()） */
function normalizeIdList(raw: unknown): string[] {
  const list = Array.isArray(raw) ? raw : []
  return list.map((v: unknown) => String(v))
}

/** 传给 roleApi 的 id：始终原样透传（雪花 ID 不做 Number()），仅补一处类型断言 */
function asApiId(id: string | number): number {
  return id as unknown as number
}

// ── 已分配权限 ──────────────────────────────────────────
const viewPermissionIds = ref<string[]>([])
const viewPermOptionsLoading = ref(false)
const viewPermOptionsError = ref(false)
/** sys_permission.id → { name, code }，用于把分配到角色的权限 ID 还原成权限码 */
const viewPermOptionMap = ref<Map<string, { name: string; code: string }>>(new Map())

/** 权限码形如 system:role:list，第一段即模块标识，据此分组（sys_permission 无可用父子层级） */
const viewPermissionGroups = computed(() => {
  const groups = new Map<string, { id: string; name: string; code: string }[]>()
  for (const id of viewPermissionIds.value) {
    const hit = viewPermOptionMap.value.get(id)
    const code = hit?.code || ''
    const module = code ? code.split(':')[0] : '未解析权限'
    const bucket = groups.get(module)
    if (bucket) bucket.push({ id, name: hit?.name || '', code })
    else groups.set(module, [{ id, name: hit?.name || '', code }])
  }
  return Array.from(groups.entries())
    .map(([module, items]) => ({ module, items }))
    .sort((a, b) => a.module.localeCompare(b.module))
})

/** 权限名称解析：GET /api/permission/page（分别取全局 tenant_id=0 与角色所属租户） */
async function loadPermissionOptions() {
  viewPermOptionsLoading.value = true
  viewPermOptionsError.value = false
  try {
    const tenantIds: number[] = [0]
    const roleTenantId = viewDetail.value?.tenantId
    if (roleTenantId !== null && roleTenantId !== undefined && String(roleTenantId) !== '0') {
      tenantIds.push(roleTenantId)
    }
    const results = await Promise.allSettled(tenantIds.map(tenantId =>
      permissionApi.getPage({ tenantId, current: 1, size: 1000 })
    ))
    const map = new Map<string, { name: string; code: string }>()
    let failed = false
    for (const result of results) {
      if (result.status === 'rejected') {
        // 单段失败不影响另一段已解析出的名称，但要显式进入错误态给重试，不能静默当成"该角色没有权限"
        failed = true
        console.warn('[角色管理] 解析权限名称失败', result.reason)
        continue
      }
      const page = result.value as unknown as {
        records?: Array<{ id?: string | number; permissionName?: string; permissionCode?: string }>
      }
      const records = Array.isArray(page?.records) ? page.records : []
      for (const p of records) {
        if (p?.id === undefined || p?.id === null) continue
        map.set(String(p.id), { name: p.permissionName || '', code: p.permissionCode || '' })
      }
    }
    viewPermOptionMap.value = map
    viewPermOptionsError.value = failed
  } finally {
    viewPermOptionsLoading.value = false
  }
}

// ── 已分配菜单 ──────────────────────────────────────────
const viewMenuIds = ref<string[]>([])
const viewMenuTreeLoading = ref(false)
const viewMenuTreeError = ref(false)
const viewMenuTree = ref<ReadonlyTreeNode[]>([])

/** 菜单树节点（GET /api/menu/tree 返回体；雪花 ID 运行时可能是字符串） */
interface MenuTreeNode {
  id?: string | number
  menuName?: string
  menuType?: number
  children?: MenuTreeNode[]
}

/** 只读菜单树节点（a-tree 的 tree-data） */
interface ReadonlyTreeNode {
  key: string
  title: string
  children?: ReadonlyTreeNode[]
}

/** 菜单类型口径对齐前端 MenuInfo.menuType 注释（0-目录 1-菜单 2-按钮） */
const MENU_TYPE_LABELS: Record<number, string> = { 0: '目录', 1: '菜单', 2: '按钮' }

/**
 * 用「已分配菜单 ID」重建菜单树：只保留被分配的节点及其祖先目录；
 * 祖先仅用于体现层级，标题里显式标注「上级目录」以免被误认为已分配。
 */
function buildAssignedMenuTree(nodes: MenuTreeNode[], assigned: Set<string>): ReadonlyTreeNode[] {
  const result: ReadonlyTreeNode[] = []
  for (const node of nodes || []) {
    if (!node || node.id === undefined || node.id === null) continue
    const children = buildAssignedMenuTree(node.children || [], assigned)
    const selfAssigned = assigned.has(String(node.id))
    if (!selfAssigned && children.length === 0) continue
    const typeLabel = node.menuType !== undefined ? (MENU_TYPE_LABELS[node.menuType] || '') : ''
    result.push({
      key: String(node.id),
      title: `${node.menuName || String(node.id)}${typeLabel ? `（${typeLabel}）` : ''}${selfAssigned ? '' : '（上级目录，未分配）'}`,
      children: children.length > 0 ? children : undefined,
    })
  }
  return result
}

/** 菜单名称解析：复用本页「菜单配置」弹窗同款接口 GET /api/menu/tree */
async function loadMenuTreeForView() {
  viewMenuTreeLoading.value = true
  viewMenuTreeError.value = false
  try {
    const menuRes = await menuApi.getTree({})
    const nodes = unwrap<MenuTreeNode[]>(menuRes) || []
    viewMenuTree.value = buildAssignedMenuTree(nodes, new Set(viewMenuIds.value))
  } catch (err) {
    viewMenuTree.value = []
    viewMenuTreeError.value = true
    console.warn('[角色管理] 解析菜单名称失败', err)
  } finally {
    viewMenuTreeLoading.value = false
  }
}

/** 基本信息 + 已分配权限 ID + 已分配菜单 ID（任一失败整体进入错误态，并提供重试） */
const fetchViewDetail = async (roleId: string | number) => {
  viewLoading.value = true
  viewError.value = false
  viewErrorMessage.value = ''
  viewDetail.value = null
  viewPermissionIds.value = []
  viewMenuIds.value = []
  viewMenuTree.value = []
  viewPermOptionMap.value = new Map()
  viewPermOptionsError.value = false
  viewMenuTreeError.value = false
  try {
    const [roleRes, permRes, menuRes] = await Promise.all([
      roleApi.getById(asApiId(roleId)),
      roleApi.getPermissions(asApiId(roleId)),
      roleApi.getMenus(asApiId(roleId)),
    ])
    const role = unwrap<RoleDetail>(roleRes)
    if (!role || role.id === undefined || role.id === null) {
      viewError.value = true
      viewErrorMessage.value = '未找到该角色，可能已被删除'
      return
    }
    viewDetail.value = role
    viewPermissionIds.value = normalizeIdList(unwrap<string[] | number[]>(permRes))
    viewMenuIds.value = normalizeIdList(unwrap<string[] | number[]>(menuRes))
  } catch (err) {
    viewError.value = true
    viewErrorMessage.value = '角色详情加载失败'
    console.warn('[角色管理] 加载角色详情失败', err)
    return
  } finally {
    viewLoading.value = false
  }
  // 附属数据（名称解析）失败不阻塞主体，各自在对应分区给错误态与重试
  loadPermissionOptions()
  loadMenuTreeForView()
}

// 重试：基本信息 + 已分配权限/菜单一起重载
const reloadView = () => {
  if (viewRoleId.value === null) return
  fetchViewDetail(viewRoleId.value)
}

const handleViewClose = () => {
  viewVisible.value = false
}

// 查看详情（只读，双击行触发）
const handleView = (record: RoleInfo) => {
  const roleId = record?.id
  if (roleId === undefined || roleId === null) return
  viewRoleId.value = roleId
  viewVisible.value = true
  fetchViewDetail(roleId)
}

// 双击行查看角色详情 —— 页面侧自行实现（不依赖共享表格组件派发事件）
// 行标识由表格行上的 data-row-key（= row-key 指定的 id）反查得到；占位空行不带该属性
const tableWrap = ref<HTMLElement | null>(null)
useRowDblclick(tableWrap, () => tableDataSource.value, handleView, 'id')
</script>

<style scoped>
.role-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.role-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.role-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.role-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.role-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.role-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}





/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.role-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.role-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 角色详情（只读）分区 ──────────────────────────── */
.detail-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1890ff;
}

.detail-section-count {
  font-size: 12px;
  color: #666;
}

.detail-section-hint {
  font-size: 12px;
  color: #999;
}

.detail-perm-group {
  margin-bottom: 10px;
}

.detail-perm-module {
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}

.detail-perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.detail-perm-tags :deep(.ant-tag) {
  margin-inline-end: 0;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
}

.detail-id-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* ── FullScreenDetail form compact overrides ── */
.fsd-body .ant-form-item {
  margin-bottom: 12px !important;
}
.fsd-body .ant-form-item:last-child {
  margin-bottom: 0 !important;
}
.fsd-body .ant-input,
.fsd-body .ant-input-password,
.fsd-body .ant-input-number,
.fsd-body .ant-select,
.fsd-body .ant-picker,
.fsd-body .ant-tree-select,
.fsd-body .ant-cascader-picker {
  min-height: 28px !important;
  font-size: 13px !important;
}
.fsd-body .ant-form-item-label > label {
  font-size: 13px !important;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.role-management :deep(.ant-input-sm),
.role-management :deep(.ant-input-number-sm),
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.role-management :deep(.ant-picker-small),
.role-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.role-management :deep(.ant-input-number-sm input) { height: 26px; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
