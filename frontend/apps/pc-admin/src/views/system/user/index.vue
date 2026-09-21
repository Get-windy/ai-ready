<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="user-page-header">
          <div class="user-page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>用户管理</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="user-page-header-title">
              用户管理
            </h2>
          </div>
          <div class="user-page-header-right">
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
            <a-button
              v-permission="'system:user:query'"
              size="small"
              :loading="refreshLoading"
              @click="debounceClick('refresh', fetchData)"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
              <span class="shortcut-hint"><kbd>双击</kbd> 查看详情</span>
            </span>
          </div>
        </div>
      </template>

      <div
        ref="tableWrap"
        class="user-management"
      >
        <!-- 统计卡片 -->
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ pagination.total }}
              </div>
              <div class="stat-card-label">
                用户总数
              </div>
            </div>
            <TeamOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-active">
            <div class="stat-card-body">
              <div class="stat-card-value">
                {{ activeCount }}
              </div>
              <div class="stat-card-label">
                正常用户
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
                停用用户
              </div>
            </div>
            <StopOutlined class="stat-card-icon" />
          </div>
        </div>

        <a-skeleton
          v-if="loading && tableDataSource.length === 0"
          active
          :paragraph="{ rows: 8 }"
          style="padding: 20px;"
        />
        <BillTableList
          v-else
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableDataSource"
          :loading="loading"
          :pagination="pagination"
          :row-key="'id'"
          :filter-fields="filterFields"
          :show-search="false"
          :selectable="true"
          :min-empty-rows="12"
          add-text="新增用户"
          add-permission="system:user:create"
          @add="handleAdd"
          @edit="handleEdit"
          @delete="handleDelete"
          @batch-delete="handleBatchDelete"
          @refresh="debounceClick('refresh', fetchData)"
          @page-change="handlePageChange"
          @filter-change="handleFilterChange"
          @selection-change="(keys: any) => { (selectedRowKeys as any) = keys }"
        >
          <template #empty>
            <div class="empty-state-wrapper">
              <a-empty
                v-if="!hasError"
                description="暂无用户数据"
              >
                <template #image>
                  <TeamOutlined style="font-size: 48px; color: #d9d9d9;" />
                </template>
                <a-button
                  v-permission="'system:user:create'"
                  type="primary"
                  size="small"
                  @click="handleAdd"
                >
                  <template #icon>
                    <PlusOutlined />
                  </template>
                  新增第一个用户
                </a-button>
              </a-empty>
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
            </div>
          </template>

          <template #usernameCell="{ record }">
            <a-space>
              <a-avatar
                :src="record.avatar"
                :size="32"
              >
                {{ record.nickname?.charAt(0) || record.username?.charAt(0) }}
              </a-avatar>
              <div>
                <div class="user-name">
                  {{ record.username }}
                </div>
                <div class="user-nickname">
                  {{ record.nickname }}
                </div>
              </div>
            </a-space>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="record.status === 0 ? 'success' : 'warning'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>
          <template #userTypeCell="{ record }">
            <a-tag :color="getUserTypeColor(record.userType)">
              {{ getUserTypeName(record.userType) }}
            </a-tag>
          </template>
          <template #tenantNameCell="{ record }">
            <span>{{ tenantMap[record.tenantId] || `租户${record.tenantId}` }}</span>
          </template>

          <!-- 7 类数据权限列：列值是可点链接，点击进入该维度的授权设置（行为与 ql361 一致） -->
          <template
            v-for="c in DATA_SCOPE_COLUMNS"
            :key="c.key"
            #[`scope_${c.key}`]="{ record }"
          >
            <a
              class="scope-link"
              @click="handleOpenScope(record, c.key)"
            >{{ scopeText(record.id, c.key) }}</a>
          </template>

          <template #action="{ record }">
            <a-space>
              <a-button
                v-permission="'system:user:update'"
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-permission="'system:role:assign'"
                type="link"
                size="small"
                @click="handleAssignRole(record)"
              >
                分配角色
              </a-button>
              <a-dropdown>
                <a-button
                  type="link"
                  size="small"
                >
                  更多<DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item
                      v-permission="'system:user:update'"
                      @click="handleResetPassword(record)"
                    >
                      <KeyOutlined /> 重置密码
                    </a-menu-item>
                    <!-- 权限预览：以该用户身份查看菜单/按钮/数据可见范围（后端按被模拟用户判定权限） -->
                    <a-menu-item
                      v-permission="'system:simulate'"
                      @click="handleSimulate(record)"
                    >
                      <EyeOutlined /> 以该用户身份预览
                    </a-menu-item>
                    <a-menu-item
                      v-permission="'system:user:update'"
                      @click="handleToggleStatus(record)"
                    >
                      <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item
                      v-permission="'system:user:delete'"
                      danger
                      @click="handleDelete(record)"
                    >
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </BillTableList>

        <!-- 用户表单弹窗 -->
        <FullScreenDetail
          :visible="modalVisible"
          :title="modalTitle"
          :save-loading="submittingLoading"
          :show-save-and-new="!isEdit"
          :dirty="formDirty"
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
              label="用户名"
              name="username"
            >
              <a-input
                v-model:value="formState.username"
                placeholder="请输入用户名"
                :disabled="isEdit"
              />
            </a-form-item>
            <a-form-item
              label="昵称"
              name="nickname"
            >
              <a-input
                v-model:value="formState.nickname"
                placeholder="请输入昵称"
              />
            </a-form-item>
            <a-form-item
              v-if="!isEdit"
              label="密码"
              name="password"
            >
              <a-input-password
                v-model:value="formState.password"
                placeholder="请输入密码"
              />
            </a-form-item>
            <a-form-item
              label="邮箱"
              name="email"
            >
              <a-input
                v-model:value="formState.email"
                placeholder="请输入邮箱"
              />
            </a-form-item>
            <a-form-item
              label="手机号"
              name="phone"
            >
              <a-input
                v-model:value="formState.phone"
                placeholder="请输入手机号"
              />
            </a-form-item>
            <a-form-item
              label="性别"
              name="gender"
            >
              <a-radio-group v-model:value="formState.gender">
                <a-radio :value="0">
                  未知
                </a-radio>
                <a-radio :value="1">
                  男
                </a-radio>
                <a-radio :value="2">
                  女
                </a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item
              label="用户类型"
              name="userType"
            >
              <a-select
                v-model:value="formState.userType"
                size="small"
                placeholder="请选择用户类型"
              >
                <a-select-option
                  v-for="opt in userTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </a-select-option>
              </a-select>
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
          </a-form>
        </FullScreenDetail>

        <!-- 分配角色弹窗 -->
        <a-modal
          v-model:open="roleModalVisible"
          title="分配角色"
          :confirm-loading="roleModalLoading"
          @ok="handleRoleModalOk"
        >
          <a-transfer
            v-model:target-keys="targetRoleKeys"
            :data-source="roleList"
            :titles="['可选角色', '已选角色']"
            :render="(item: any) => item.title"
            show-search
            :filter-option="filterRoleOption"
          />
        </a-modal>

        <!-- 数据权限设置弹窗（对标 ql361「XX数据权限设置」：左侧分类 + 名称/已授权 勾选表） -->
        <a-modal
          v-model:open="scopeModalVisible"
          :title="`${scopeModalTitle}（${scopeTargetUserName}）`"
          width="760px"
          :confirm-loading="scopeSaving"
          ok-text="保存"
          cancel-text="关闭"
          @ok="handleScopeOk"
        >
          <a-spin :spinning="scopeModalLoading">
            <div class="scope-dialog">
              <!-- 左侧分类（维度无分类数据时不显示） -->
              <div
                v-if="scopeCategories.length"
                class="scope-dialog__side"
              >
                <div class="scope-dialog__side-title">
                  分类
                </div>
                <ul class="scope-dialog__categories">
                  <li
                    :class="{ active: scopeCategoryId === '' }"
                    @click="scopeCategoryId = ''"
                  >
                    <span>全部</span>
                    <span class="cnt">{{ scopeTargets.length }}</span>
                  </li>
                  <li
                    v-for="c in scopeCategories"
                    :key="c.name"
                    :class="{ active: scopeCategoryId === c.name }"
                    @click="scopeCategoryId = c.name"
                  >
                    <span>{{ c.name }}</span>
                    <span class="cnt">{{ c.count }}</span>
                  </li>
                </ul>
              </div>

              <div class="scope-dialog__main">
                <div class="scope-dialog__toolbar">
                  <a-space :size="8">
                    <a-input
                      v-model:value="scopeKeyword"
                      placeholder="名称/编号"
                      size="small"
                      style="width: 180px"
                      allow-clear
                    />
                    <a-button
                      size="small"
                      @click="toggleAllScope(true)"
                    >
                      全选
                    </a-button>
                    <a-button
                      size="small"
                      @click="toggleAllScope(false)"
                    >
                      清空
                    </a-button>
                  </a-space>
                  <span class="scope-dialog__summary">已授权 <b>{{ scopeChecked.size }}</b> 项</span>
                </div>
                <div class="scope-dialog__table-wrap">
                  <table class="scope-dialog__table">
                    <thead>
                      <tr>
                        <th>名称</th>
                        <th class="col-auth">
                          已授权
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="t in scopeFilteredTargets"
                        :key="t.id"
                      >
                        <td>{{ t.name }}</td>
                        <td class="col-auth">
                          <a-checkbox
                            :checked="scopeChecked.has(t.id)"
                            @change="(e: any) => toggleScopeTarget(t.id, !!e.target.checked)"
                          />
                        </td>
                      </tr>
                      <tr v-if="!scopeFilteredTargets.length">
                        <td
                          colspan="2"
                          class="scope-dialog__empty"
                        >
                          无可授权的对象
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </a-spin>
        </a-modal>

        <!-- 用户详情抽屉（只读）：双击行打开，不提供任何写操作 -->
        <a-drawer
          v-model:open="detailVisible"
          title="用户详情"
          placement="right"
          width="60vw"
          :footer="null"
          destroy-on-close
        >
          <a-spin :spinning="detailLoading">
            <!-- 取数失败：给出错误态与重试入口，不静默显示空 -->
            <a-result
              v-if="detailError"
              status="error"
              title="用户详情加载失败"
              :sub-title="detailError"
            >
              <template #extra>
                <a-button
                  type="primary"
                  @click="loadDetail()"
                >
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重新加载
                </a-button>
              </template>
            </a-result>

            <template v-else-if="detailData">
              <div class="detail-user-head">
                <a-avatar
                  :src="detailData.avatar"
                  :size="40"
                >
                  {{ detailData.nickname?.charAt(0) || detailData.username?.charAt(0) }}
                </a-avatar>
                <div>
                  <div class="user-name">
                    {{ detailData.username }}
                  </div>
                  <div class="user-nickname">
                    {{ detailData.nickname || '-' }}
                  </div>
                </div>
              </div>

              <!-- 账号信息：GET /user/{id} -->
              <a-descriptions
                class="detail-block"
                title="账号信息"
                bordered
                :column="2"
                size="small"
              >
                <a-descriptions-item label="用户名">
                  {{ detailData.username || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="昵称">
                  {{ detailData.nickname || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="姓名">
                  {{ detailData.realName || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="手机号">
                  {{ detailData.phone || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="邮箱">
                  {{ detailData.email || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="性别">
                  {{ getGenderName(detailData.gender) }}
                </a-descriptions-item>
                <a-descriptions-item label="状态">
                  <a-tag :color="detailData.status === 0 ? 'success' : 'warning'">
                    {{ getStatusName(detailData.status) }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="用户类型">
                  <a-tag :color="getUserTypeColor(detailData.userType ?? -1)">
                    {{ getUserTypeName(detailData.userType ?? -1) }}
                  </a-tag>
                </a-descriptions-item>
              </a-descriptions>

              <!-- 归属信息 -->
              <a-descriptions
                class="detail-block"
                title="归属信息"
                bordered
                :column="2"
                size="small"
              >
                <a-descriptions-item label="所属租户">
                  {{ tenantLabel(detailData.tenantId) }}
                </a-descriptions-item>
                <a-descriptions-item label="所属部门">
                  {{ deptLabel }}
                </a-descriptions-item>
                <a-descriptions-item label="超级管理员">
                  {{ detailData.isSuperAdmin ? '是' : '否' }}
                </a-descriptions-item>
                <a-descriptions-item label="租户管理员">
                  {{ detailData.isTenantAdmin ? '是' : '否' }}
                </a-descriptions-item>
              </a-descriptions>

              <!-- 角色：GET /user-permission/user/{id}/role-ids + GET /role/list -->
              <a-descriptions
                class="detail-block"
                title="角色"
                bordered
                :column="1"
                size="small"
              >
                <a-descriptions-item label="已分配角色">
                  <a-spin
                    :spinning="rolesLoading"
                    size="small"
                  >
                    <template v-if="rolesError">
                      <span style="color: #f5222d;">{{ rolesError }}</span>
                      <a-button
                        type="link"
                        size="small"
                        @click="loadDetail(currentDetailId)"
                      >
                        重试
                      </a-button>
                    </template>
                    <a-space
                      v-else-if="roleTags.length"
                      wrap
                    >
                      <a-tag
                        v-for="role in roleTags"
                        :key="role.id"
                        color="blue"
                      >
                        {{ role.name }}
                      </a-tag>
                    </a-space>
                    <!-- 仍在加载时不要断言「暂未分配」，避免把加载中误报成空态 -->
                    <span
                      v-else-if="!rolesLoading"
                      style="color: #999;"
                    >该用户暂未分配任何角色</span>
                  </a-spin>
                </a-descriptions-item>
              </a-descriptions>

              <!-- 时间信息 -->
              <a-descriptions
                class="detail-block"
                title="时间信息"
                bordered
                :column="2"
                size="small"
              >
                <a-descriptions-item label="创建时间">
                  {{ formatDateTime(detailData.createTime) }}
                </a-descriptions-item>
                <a-descriptions-item label="更新时间">
                  {{ formatDateTime(detailData.updateTime) }}
                </a-descriptions-item>
                <a-descriptions-item label="最后登录时间">
                  {{ formatDateTime(detailData.lastLoginTime) }}
                </a-descriptions-item>
                <a-descriptions-item label="最后登录IP">
                  {{ detailData.lastLoginIp || '-' }}
                </a-descriptions-item>
              </a-descriptions>
            </template>

            <a-empty
              v-else-if="!detailLoading"
              description="未找到该用户的信息"
            />
          </a-spin>
        </a-drawer>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, h } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRowDblclick } from '@/composables/useRowDblclick'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { DownOutlined, KeyOutlined, StopOutlined, DeleteOutlined, PlusOutlined, TeamOutlined, CheckCircleOutlined, ReloadOutlined, SyncOutlined, WarningOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { useSimulation } from '@/composables/useSimulation'
import BillTableList, { type FilterField } from '@/components/BillTableList/BillTableList.vue'
import { userApi, type UserInfo, type TenantInfo } from '@/api/user'
import { roleApi, type RoleInfo } from '@/api/role'
import { dictItemApi } from '@/api/dict'
import { departmentApi } from '@/api/department'
import { userDataScopeApi, type DataScopeKey, type DataScopeTarget } from '@/api/userDataScope'
import { useSubmitLock, useOptimisticUpdate } from '@/composables'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const searchForm = reactive({ username: '', phone: '', status: undefined as number | undefined, tenantId: undefined as number | undefined })

const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const { isSubmitting: batchDeleteLoading } = useSubmitLock()

const { executeOptimistic, isUndoing: isUndoInProgress } = useOptimisticUpdate<UserInfo>({
  dataList: tableData, showUndo: true, undoTimeout: 5000,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

// ═══ 数据权限维度（对标 ql361「全部操作员」7 类数据权限，2026-09-19 实抓） ═══
// 列顺序与 ql361 一致：仓库 / 调拨 / 部门 / 往来单位 / 商品 / 现金银行 / 客户级别
const DATA_SCOPE_COLUMNS: Array<{ key: DataScopeKey; title: string }> = [
  { key: 'warehouse', title: '仓库权限' },
  { key: 'transfer', title: '调拨权限' },
  { key: 'department', title: '部门权限' },
  { key: 'partner', title: '往来单位权限' },
  { key: 'product', title: '商品权限' },
  { key: 'fund', title: '现金银行权限' },
  { key: 'customer_level', title: '客户级别权限' },
]

const vxeColumns = computed(() => [
  { field: 'username', title: '用户信息', width: 200, slotName: 'usernameCell' },
  { field: 'phone', title: '手机号', width: 120 },
  { field: 'email', title: '邮箱', width: 180, showOverflow: 'tooltip' },
  { field: 'userType', title: '用户类型', width: 100, slotName: 'userTypeCell' },
  // 7 类数据权限列（列值是可点链接，点击进入该维度的授权设置，行为与 ql361 一致）
  ...DATA_SCOPE_COLUMNS.map(c => ({
    field: `scope_${c.key}`,
    title: c.title,
    width: 132,
    slotName: `scope_${c.key}`,
  })),
  { field: 'tenantName', title: '所属租户', width: 120, slotName: 'tenantNameCell' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' }
])

const tenantFilterOptions = computed(() => tenantList.value.map(t => ({ label: t.tenantName, value: t.id })))
const filterFields = computed<FilterField[]>(() => {
  const fields: FilterField[] = [
    { key: 'username', label: '用户名', type: 'input', placeholder: '请输入用户名' },
    { key: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
    { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
  ]
  if (userStore.isSystemUser) {
    fields.push({ key: 'tenantId', label: '所属租户', type: 'select', options: tenantFilterOptions.value })
  }
  return fields
})

// ═══ 数据权限（对标 ql361「全部操作员」7 类数据权限设置） ═══════════════════════
// ql361 形态：列表 7 列各显示授权摘要，列值是可点链接 → 打开「XX数据权限设置」面板
// （左侧分类 + 「名称 ｜ 已授权」勾选表）。后端表 sys_user_data_scope（user_id + scope_key + target_ids）。
/** userId(字符串) → scopeKey → 已授权对象数（用于列表列上的摘要） */
const scopeSummary = ref<Record<string, Record<string, number>>>({})
const scopeModalVisible = ref(false)
const scopeModalLoading = ref(false)
const scopeSaving = ref(false)
const scopeTargetUserId = ref('')
const scopeTargetUserName = ref('')
const scopeModalKey = ref<DataScopeKey>('warehouse')
const scopeTargets = ref<DataScopeTarget[]>([])
const scopeChecked = ref<Set<string>>(new Set())
const scopeKeyword = ref('')
const scopeCategoryId = ref('')

const scopeModalTitle = computed(() => {
  const hit = DATA_SCOPE_COLUMNS.find(c => c.key === scopeModalKey.value)
  return `${hit?.title || '数据权限'}设置`
})

/** 左侧分类（由候选对象自带的 categoryName 派生，无分类的维度不显示该栏） */
const scopeCategories = computed(() => {
  const map = new Map<string, number>()
  for (const t of scopeTargets.value) {
    const key = t.categoryName || ''
    if (!key) continue
    map.set(key, (map.get(key) || 0) + 1)
  }
  return Array.from(map.entries()).map(([name, count]) => ({ name, count }))
})

const scopeFilteredTargets = computed(() => {
  const kw = scopeKeyword.value.trim().toLowerCase()
  return scopeTargets.value.filter(t => {
    if (scopeCategoryId.value && (t.categoryName || '') !== scopeCategoryId.value) return false
    if (!kw) return true
    return (t.name || '').toLowerCase().includes(kw) || (t.code || '').toLowerCase().includes(kw)
  })
})

/** 列上展示的授权摘要文案 */
function scopeText(userId: unknown, key: DataScopeKey): string {
  const n = scopeSummary.value[String(userId)]?.[key]
  if (!n) return '未设置'
  return `${n} 个`
}

/** 列表加载后拉当前页各操作员的授权摘要（并发、失败不阻断列表） */
async function loadScopeSummary(rows: UserInfo[]) {
  if (!rows?.length) {
    scopeSummary.value = {}
    return
  }
  const next: Record<string, Record<string, number>> = {}
  await Promise.allSettled(rows.map(async (r) => {
    const uid = String(r.id)
    try {
      const res = await userDataScopeApi.getUserScopes(uid) as unknown as { data?: Record<string, string[]> }
      const map = res?.data || {}
      const counts: Record<string, number> = {}
      for (const [k, v] of Object.entries(map)) counts[k] = Array.isArray(v) ? v.length : 0
      next[uid] = counts
    } catch {
      // 单个用户取不到摘要不影响列表展示，保持「未设置」
      next[uid] = {}
    }
  }))
  scopeSummary.value = next
}

/** 打开某操作员的某维度数据权限设置面板 */
async function handleOpenScope(record: UserInfo, key: DataScopeKey) {
  scopeTargetUserId.value = String(record.id)
  scopeTargetUserName.value = record.username || ''
  scopeModalKey.value = key
  scopeKeyword.value = ''
  scopeCategoryId.value = ''
  scopeTargets.value = []
  scopeChecked.value = new Set()
  scopeModalVisible.value = true
  scopeModalLoading.value = true
  try {
    const [targetsRes, scopesRes] = await Promise.all([
      userDataScopeApi.getTargets(key),
      userDataScopeApi.getUserScopes(scopeTargetUserId.value),
    ])
    scopeTargets.value = ((targetsRes as unknown as { data?: DataScopeTarget[] })?.data) || []
    const map = (scopesRes as unknown as { data?: Record<string, string[]> })?.data || {}
    scopeChecked.value = new Set((map?.[key] || []).map(id => String(id)))
  } catch (err) {
    console.warn('[用户管理] 加载数据权限失败', err)
    message.error('加载数据权限失败')
  } finally {
    scopeModalLoading.value = false
  }
}

function toggleScopeTarget(id: string, checked: boolean) {
  const next = new Set(scopeChecked.value)
  if (checked) next.add(id)
  else next.delete(id)
  scopeChecked.value = next
}

function toggleAllScope(checked: boolean) {
  const next = new Set(scopeChecked.value)
  for (const t of scopeFilteredTargets.value) {
    if (checked) next.add(t.id)
    else next.delete(t.id)
  }
  scopeChecked.value = next
}

async function handleScopeOk() {
  if (!scopeTargetUserId.value) return
  scopeSaving.value = true
  try {
    await userDataScopeApi.saveScope(scopeTargetUserId.value, scopeModalKey.value, Array.from(scopeChecked.value))
    message.success(`${scopeModalTitle.value}保存成功，共 ${scopeChecked.value.size} 项`)
    scopeModalVisible.value = false
    await loadScopeSummary(tableData.value)
  } catch (err) {
    console.warn('[用户管理] 保存数据权限失败', err)
    message.error('保存数据权限失败')
  } finally {
    scopeSaving.value = false
  }
}

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// ── 用户类型选项（从API加载） ──────────────────────────
const userTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadUserTypeOptions() {
  try {
    const res = await dictItemApi.getByDictCode('USER_TYPE')
    if (res.data) {
      userTypeOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[用户管理] 加载用户类型失败', err)
  }
}

const formState = reactive({ id: 0, username: '', nickname: '', password: '', email: '', phone: '', gender: 0, userType: 2, tenantId: userStore.tenantId, status: 0 })
const formRules: any = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  password: { required: true, message: '请输入密码', min: 6, trigger: 'blur' },
  email: [{ required: false, type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }],
  phone: [{ required: false, pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }],
}

const roleModalVisible = ref(false)
const roleModalLoading = ref(false)
const roleList = ref<{ key: string; title: string }[]>([])
const targetRoleKeys = ref<string[]>([])
const currentUserId = ref(0)

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

const tenantList = ref<TenantInfo[]>([])
const tenantMap = computed(() => {
  const map: Record<number, string> = {}
  tenantList.value.forEach((t) => { map[t.id] = t.tenantName })
  return map
})

const loadTenants = async () => {
  try { const res = await userApi.getTenants(); if (res.data) tenantList.value = res.data } catch (err) { tenantList.value = []; console.warn('[系统管理] 加载租户列表失败', err); message.error('加载租户列表失败') }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await userApi.getPage({ tenantId: userStore.tenantId, ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })
    if (res) { tableData.value = res.records || []; pagination.total = res.total || 0 }
    // 列表就绪后并行拉 7 类数据权限摘要（失败不阻断列表）
    loadScopeSummary(tableData.value)
  } catch (err) {
    hasError.value = true
    console.warn('[系统管理] 加载用户数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally { loading.value = false; lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN'); refreshLoading.value = false }
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(searchForm, { username: '', phone: '', status: undefined, tenantId: undefined })
  else Object.assign(searchForm, filters)
  pagination.current = 1; fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, username: '', nickname: '', password: '', email: '', phone: '', gender: 0, userType: 2, tenantId: userStore.tenantId, status: 0 })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleEdit = (record: UserInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, username: record.username, nickname: record.nickname, email: record.email, phone: record.phone, gender: record.gender, userType: record.userType, tenantId: record.tenantId, status: record.status })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      if (isEdit.value) { await userApi.update(formState.id, formState); message.success('更新成功') }
      else { await userApi.create(formState as any); message.success('创建成功') }
      modalVisible.value = false; fetchData()
    })
    void result
  } catch (error: any) { console.warn('[用户管理] 提交用户表单失败', error); if (error) message.error(error?.message || '操作失败') }
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
      await userApi.create(formState as any)
      message.success('创建成功')
      fetchData()
      handleAdd()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleDelete = (record: UserInfo) => {
  const savedRecord = { ...record }
  Modal.confirm({
    title: '确认删除', content: `确定要删除用户 "${record.username}" 吗？`,
    async onOk() {
      return executeOptimistic(
        (list) => list.filter((item) => item.id !== record.id),
        (originalList) => { tableData.value = originalList },
        () => userApi.delete(record.id),
        async () => { const createData = { ...savedRecord, password: '123456' } as Record<string, any>; delete createData.id; await userApi.create(createData as any); await fetchData() },
        '删除'
      )
    },
  })
}

const handleBatchDelete = (deleteKeys?: number[]) => {
  if (batchDeleteLoading.value) return
  const idsToDelete = deleteKeys || [...selectedRowKeys.value] as number[]
  const deletedItems = tableData.value.filter((item) => idsToDelete.includes(item.id))
  Modal.confirm({
    title: '确认删除', content: `确定要删除选中的 ${idsToDelete.length} 个用户吗？`,
    async onOk() {
      batchDeleteLoading.value = true
      const result = await executeOptimistic(
        (list) => list.filter((item) => !idsToDelete.includes(item.id)),
        (originalList) => { tableData.value = originalList },
        () => userApi.batchDelete(idsToDelete),
        async () => { for (const item of deletedItems) { try { const createData = { ...item, password: '123456' } as Record<string, any>; delete createData.id; await userApi.create(createData as any) } catch (err) { console.warn('[系统管理] 恢复用户失败', err) } } await fetchData() },
        '删除'
      )
      if (result) selectedRowKeys.value = []
      batchDeleteLoading.value = false
    },
  })
}

// ── 权限预览（以该用户身份查看） ──────────────────────────────
const { start: startSimulation } = useSimulation()

/**
 * 以该用户身份预览权限。
 *
 * 开启后**全局**按该用户的权限判定（后端 StpInterfaceImpl 以被模拟用户计算权限），
 * 顶栏出现「结束预览」横幅 —— 用于验证「某人到底能看哪些菜单/按钮/数据」，
 * 而不是靠管理员猜。需 system:simulate 权限。
 */
async function handleSimulate(record: UserInfo) {
  try {
    await startSimulation(String(record.id), `用户管理预览：${record.username}`)
  } catch {
    // 失败提示已在 useSimulation 内完成
  }
}

const handleResetPassword = (record: UserInfo) => {
  const genPwd = (len = 12) => {
    const uppers = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'; const lowers = 'abcdefghijklmnopqrstuvwxyz'; const digits = '0123456789'; const specials = '!@#$%'; const all = uppers + lowers + digits + specials
    let pwd = ''
    pwd += uppers[Math.floor(Math.random() * uppers.length)]; pwd += lowers[Math.floor(Math.random() * lowers.length)]; pwd += digits[Math.floor(Math.random() * digits.length)]; pwd += specials[Math.floor(Math.random() * specials.length)]
    for (let i = pwd.length; i < len; i++) pwd += all[Math.floor(Math.random() * all.length)]
    return pwd.split('').sort(() => Math.random() - 0.5).join('')
  }
  const newPassword = genPwd()
  Modal.confirm({
    title: '重置密码',
    content: h('div', [
      h('p', { style: { marginBottom: '12px' } }, `确定要重置用户 "${record.username}" 的密码吗？`),
      h('div', { style: { padding: '12px', background: '#f5f5f5', borderRadius: '4px', fontSize: '13px' } }, [
        h('div', { style: { marginBottom: '4px', color: '#999' } }, '新密码（请立即告知用户）：'),
        h('div', { style: { fontFamily: 'monospace', fontSize: '16px', fontWeight: 'bold', color: '#1890ff', letterSpacing: '2px' } }, newPassword),
        h('div', { style: { marginTop: '8px', color: '#f5222d', fontSize: '12px' } }, '此密码仅在此处显示一次，关闭后将无法再次查看')
      ])
    ]),
    okText: '确认重置', cancelText: '取消',
    async onOk() { await userApi.resetPassword(record.id, newPassword); message.success('密码已重置') }
  })
}

const handleToggleStatus = async (record: UserInfo) => {
  const newStatus = record.status === 0 ? 1 : 0
  await executeOptimistic(
    (list) => list.map((item) => item.id === record.id ? { ...item, status: newStatus } : item),
    (originalList) => { tableData.value = originalList },
    () => userApi.updateStatus(record.id, newStatus),
    undefined,
    '更新状态'
  )
}

const handleAssignRole = async (record: UserInfo) => {
  currentUserId.value = record.id
  // 根据当前用户类型过滤角色 scope：系统用户看到 PLATFORM，租户用户看到 TENANT
  const scope = userStore.isSystemUser ? 'PLATFORM' : 'TENANT'
  const res = await roleApi.getPage({ tenantId: userStore.tenantId, scope, size: 100 } as any)
  if (res?.records) roleList.value = res.records.map((r: RoleInfo) => ({ key: String(r.id), title: r.roleName }))
  try {
    const userRes = await userApi.getById(record.id)
    targetRoleKeys.value = (userRes.data as any)?.roleIds ? (userRes.data as any).roleIds.map(String) : []
  } catch (err) { console.warn('[系统管理] 获取用户角色失败', err); targetRoleKeys.value = [] }
  roleModalVisible.value = true
}

const handleRoleModalOk = async () => {
  roleModalLoading.value = true
  try { await userApi.assignRoles(currentUserId.value, targetRoleKeys.value.map(Number)); message.success('分配成功'); roleModalVisible.value = false } finally { roleModalLoading.value = false }
}

const filterRoleOption = (input: string, option: any) => option.title.toLowerCase().includes(input.toLowerCase())
const userTypeColorPalette = ['gold', 'blue', 'green']
const getUserTypeColor = (type: number) => {
  const idx = userTypeOptions.value.findIndex(o => o.value === type)
  return idx >= 0 ? userTypeColorPalette[idx % userTypeColorPalette.length] : 'default'
}
const getUserTypeName = (type: number) => {
  const found = userTypeOptions.value.find(o => o.value === type)
  return found ? found.label : '未知'
}

onMounted(() => {
  fetchData()
  loadTenants()
  loadUserTypeOptions()
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

// ── 用户详情（只读）──────────────────────────────────────
// 数据来源（全部为真实接口，无写操作）：
//   · 账号/归属/时间：GET /api/user/{id}（SysUser 实体，含 tenantId / userType / lastLoginTime）
//   · 角色：GET /api/user-permission/user/{id}/role-ids（直查 sys_user_role 关联表）
//          + GET /api/role/list（把 roleId 映射成角色名）
//   · 部门名：GET /api/department/{id}（详情只返回 deptId；取不到名时降级显示部门ID）
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailData = ref<UserInfo | null>(null)
/** 当前正在查看的用户ID（字符串口径，雪花ID不做数值转换） */
const currentDetailId = ref<any>(null)
const rolesLoading = ref(false)
const rolesError = ref('')
const roleTags = ref<{ id: string; name: string }[]>([])
const deptName = ref('')


/** 性别取值口径与编辑表单一致（0-未知 1-男 2-女） */
const GENDER_NAMES: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
function getGenderName(gender?: number) {
  if (gender === undefined || gender === null) return '-'
  return GENDER_NAMES[gender] || '未知'
}

/** 状态口径：0-正常 1-停用 2-锁定（见后端 SysUser.status 注释；列表页只出现 0/1） */
function getStatusName(status?: number) {
  if (status === 0) return '正常'
  if (status === 1) return '停用'
  if (status === 2) return '锁定'
  return '-'
}

/** 所属租户：与列表「所属租户」列同口径（tenantMap 来自 /auth/tenants） */
function tenantLabel(tenantId?: number) {
  if (tenantId === undefined || tenantId === null) return '-'
  return tenantMap.value[tenantId] || `租户${tenantId}`
}

/** 后端 LocalDateTime 形如 2026-06-12T01:22:49.946885 */
function formatDateTime(value?: string) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const deptLabel = computed(() => {
  const deptId = detailData.value?.deptId
  if (deptId === undefined || deptId === null || deptId === '') return '-'
  return deptName.value || `部门ID ${deptId}`
})

/**
 * 加载详情主数据（失败即整块错误态 + 重试）
 * @param id 不传时沿用当前正在查看的用户
 */
async function loadDetail(id?: any) {
  const userId = id ?? currentDetailId.value
  if (userId === undefined || userId === null || userId === '') return
  currentDetailId.value = userId
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailData.value = null
  deptName.value = ''
  rolesLoading.value = true
  try {
    const res: any = await userApi.getById(userId)
    if (!res) {
      detailError.value = '接口未返回该用户的数据'
      return
    }
    detailData.value = res as UserInfo
    // 角色随详情一并返回（后端 /api/v2/user/{id} 的 roles/roleIds），无需再发请求
    resolveRoles(res as UserInfo)
    // 部门名是分块数据：失败只影响本块，不拖垮整个详情
    void loadDeptName(res.deptId)
  } catch (err: any) {
    console.warn('[用户管理] 加载用户详情失败', err)
    detailError.value = err?.message || '请求失败，请稍后重试'
  } finally {
    detailLoading.value = false
  }
}

/**
 * 解析该用户已分配的角色 → 角色名标签
 *
 * 数据源：**`GET /api/v2/user/{id}` 自带的 `roles` / `roleIds`**（后端 `UserServiceImpl`
 * 已把角色随详情一并返回），不再单开一个端点。
 *
 * ⚠️ 历史弯路（勿再走回）：本页曾改调 `GET /api/user-permission/user/{id}/role-ids`，
 * 那条路径要 `system:role:view` 权限（仅授 SUPER_ADMIN）→ **租户管理员调用会 500**，
 * 且还要再发一次 `/role/list` 做 id→名映射。真正的根因在**后端**：
 * `RoleMapper.selectByUserId` 的状态过滤写成 `r.status = 1`，而 `sys_role.status`
 * 是 **0 = 启用 / 1 = 停用**（见 `PermissionInitializationConfig` 的 `setStatus(0); // 启用`）
 * → 该查询恒返回空 → 用户详情的角色恒为空。**已于 2026-09-19 修正为 `status = 0`**，
 * 故本页直接用详情自带字段即可：少一次请求，且不再受权限码限制。
 */
function resolveRoles(detail: UserInfo | null) {
  rolesError.value = ''
  roleTags.value = []
  if (!detail) return
  const roles = (detail as any).roles as RoleInfo[] | undefined
  const roleIds = (detail as any).roleIds as any[] | undefined
  if (Array.isArray(roles) && roles.length > 0) {
    roleTags.value = roles.map(r => ({ id: String(r.id), name: r.roleName || `角色ID ${r.id}` }))
    return
  }
  // 后端只回 id 时（理论上不会，留兜底）用 id 展示，不静默隐藏
  if (Array.isArray(roleIds) && roleIds.length > 0) {
    roleTags.value = roleIds.map(id => ({ id: String(id), name: `角色ID ${id}` }))
  }
}

/** 部门名（详情只返回 deptId，取名字失败时降级显示部门ID，不影响其它块） */
async function loadDeptName(deptId?: any) {
  if (deptId === undefined || deptId === null || deptId === '') return
  try {
    const res: any = await departmentApi.getById(deptId)
    deptName.value = res?.departmentName || ''
  } catch (err) {
    console.warn('[用户管理] 加载部门名称失败', err)
    deptName.value = ''
  }
}

// 查看详情：双击行时由 useRowDblclick 回调，行为只读（不修改任何数据）
const handleView = (record: any) => {
  if (!record?.id) return
  void loadDetail(record.id)
}

// 双击行查看用户详情 —— 页面侧自行实现（不依赖共享表格组件派发事件）
// 行标识由表格行上的 data-row-key（= row-key 指定的 id）反查得到；占位空行不带该属性
const tableWrap = ref<HTMLElement | null>(null)
useRowDblclick(tableWrap, () => tableDataSource.value, handleView, 'id')
</script>

<style scoped>
/* ═══ 数据权限设置面板（对标 ql361「XX数据权限设置」） ═══ */
.scope-link {
  color: #1677ff;
  cursor: pointer;
}
.scope-link:hover {
  text-decoration: underline;
}
.scope-dialog {
  display: flex;
  gap: 12px;
  min-height: 360px;
}
.scope-dialog__side {
  width: 160px;
  flex-shrink: 0;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px;
  max-height: 420px;
  overflow-y: auto;
}
.scope-dialog__side-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.scope-dialog__categories {
  list-style: none;
  margin: 0;
  padding: 0;
}
.scope-dialog__categories li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 8px;
  font-size: 13px;
  color: #303133;
  border-radius: 4px;
  cursor: pointer;
}
.scope-dialog__categories li:hover {
  background: #f5f7fa;
}
.scope-dialog__categories li.active {
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}
.scope-dialog__categories .cnt {
  font-size: 12px;
  color: #909399;
}
.scope-dialog__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.scope-dialog__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.scope-dialog__summary {
  font-size: 13px;
  color: #606266;
}
.scope-dialog__summary b {
  color: #1677ff;
  padding: 0 2px;
}
.scope-dialog__table-wrap {
  flex: 1;
  overflow: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  max-height: 380px;
}
.scope-dialog__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.scope-dialog__table th,
.scope-dialog__table td {
  border-bottom: 1px solid #f0f0f0;
  padding: 6px 10px;
  text-align: left;
  color: #303133;
}
.scope-dialog__table thead th {
  position: sticky;
  top: 0;
  background: #fafafa;
  font-weight: 600;
  z-index: 1;
}
.scope-dialog__table .col-auth {
  width: 80px;
  text-align: center;
}
.scope-dialog__empty {
  text-align: center;
  color: #909399;
  padding: 32px 0;
}

.user-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.user-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.user-page-header-right {
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

.user-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.user-management > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px 0 16px 0;
  flex-shrink: 0;
}

.empty-state-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  width: 100%;
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

.user-name { font-weight: 500; }
.user-nickname { font-size: 12px; color: #999; }

/* ── 用户详情抽屉（只读）─────────────── */
.detail-user-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.detail-block + .detail-block {
  margin-top: 16px;
}




/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.user-management :deep(.ant-input-sm),
.user-management :deep(.ant-input-number-sm),
.user-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.user-management :deep(.ant-picker-small),
.user-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.user-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.user-management :deep(.ant-input-number-sm input) { height: 26px; }

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

/* ── vxe-table 表头 2px 粗边框 ──────────────────── */
.user-management :deep(.vxe-table-list-container .vxe-header--row .vxe-header--column) {
  border-bottom: 2px solid #d0d5dd !important;
}

</style>
