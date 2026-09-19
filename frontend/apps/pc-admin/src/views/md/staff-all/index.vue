<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        全部操作员（人力资源 → 职员管理 → 全部操作员，菜单 80532 / md:staff-all）
        · 定位：系统操作员账号台账（sys_user），非 ERP 业务单据 → 无打印模板、无左分类树、单视图
        · 骨架（路线 A）：CategoryListLayout + BillTableList（表头序号齿轮列配置：个人/全局）+ PageConfigPanel
        · 数据：GET  /user/page（分页）· POST /user（新增）· PUT /user/{id}（修改）
                DELETE /user/{id}（删除）· PUT /user/{id}/status（启用/禁用）
                PUT /user/{id}/password/reset（重置密码）· POST /user/{id}/roles（分配角色）
                POST /user/batch-assign-roles（批量分配角色）· DELETE /user/batch（批量删除，本页走逐个删除）
        · 状态口径（P0-4 裁定）：1 = 正常/启用、0 = 禁用、2 = 锁定。后端 createUser 硬写 status=0，
          新增后本页按所选状态回写一次（见 handleSave 注释）。
        · 安全红线：SysUser.password 已 @JsonIgnore，接口不下发密码；本页不渲染、不读取任何密码字段。
        · 前端就近收敛 P0-2：sys_user 在后端 IGNORE_TENANT_TABLES 中（租户过滤要调用方显式传参），
          非平台超管查询时显式带 tenantId，否则 /user/page 会返回全库所有租户的账号。
        ⚠️ 与 views/system/user/index.vue（无菜单的重复实现）功能重叠；收敛裁定见 HR 模块文档，本页不做删改。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增操作员 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增操作员
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 打印(F8) / 导出 ═══ -->
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
            <a-alert
              class="risk-alert"
              type="warning"
              show-icon
              message="本页维护的是系统登录账号（sys_user），不是员工档案：账号与 hr_employee 当前未绑定，入职开号 / 离职封号仍需人工处理。"
              description="停用、删除、重置密码会影响该账号的登录与会话；已离职人员建议停用而非删除。列表不下发、不展示任何密码字段。"
            />
            <div class="search-row">
              <div
                v-if="isQueryVisible('username')"
                class="search-item"
              >
                <span class="search-label">登录账号</span>
                <a-input
                  v-model:value="searchForm.username"
                  placeholder="请输入登录账号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <!-- 缺口：后端 UserDTO.Query 无 nickname 参数（/user/page 不支持姓名过滤），控件置灰待后端补齐 -->
              <div
                v-if="isQueryVisible('nickname')"
                class="search-item"
              >
                <span class="search-label">姓名</span>
                <a-tooltip
                  placement="bottom"
                  title="后端 /user/page 暂不支持按姓名过滤（UserDTO.Query 无 nickname 参数），补齐后启用"
                >
                  <a-input
                    placeholder="后端待支持"
                    size="small"
                    style="width: 150px"
                    disabled
                  />
                </a-tooltip>
              </div>
              <!-- 缺口：后端 UserDTO.Query 无 phone 参数，手机号过滤同样待后端补齐 -->
              <div
                v-if="isQueryVisible('phone')"
                class="search-item"
              >
                <span class="search-label">手机号</span>
                <a-tooltip
                  placement="bottom"
                  title="后端 /user/page 暂不支持按手机号过滤（UserDTO.Query 无 phone 参数），补齐后启用"
                >
                  <a-input
                    placeholder="后端待支持"
                    size="small"
                    style="width: 150px"
                    disabled
                  />
                </a-tooltip>
              </div>
              <div
                v-if="isQueryVisible('deptId')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-tree-select
                  v-model:value="searchForm.deptId"
                  placeholder="全部部门"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  tree-default-expand-all
                  :tree-data="deptTreeData"
                  :field-names="{ label: 'departmentName', value: 'id', children: 'children' }"
                  @change="handleSearch"
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
                  style="width: 130px"
                  allow-clear
                  :options="STATUS_OPTIONS"
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列右上角） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              ref="tableRef"
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              storage-key="md-staff-all-columns"
              global-config-key="md-staff-all-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
            >
              <template #usernameCell="{ record, column }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record[column.key] }}</a>
              </template>

              <template #genderCell="{ record, column }">
                <span v-if="!record.__ghost">{{ GENDER_MAP[record[column.key]] || '-' }}</span>
              </template>

              <template #statusCell="{ record, column }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record[column.key])"
                >
                  {{ statusText(record[column.key]) }}
                </a-tag>
              </template>

              <template #userTypeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ USER_TYPE_MAP[record[column.key]] || '-' }}</span>
              </template>

              <template #dataScopeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ DATA_SCOPE_MAP[record[column.key]] || '-' }}</span>
              </template>

              <template #booleanCell="{ record, column }">
                <span v-if="!record.__ghost">{{ record[column.key] ? '是' : '否' }}</span>
              </template>

              <template #passwordExpiredCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && record.passwordUpdateTime"
                  :color="record.passwordExpired ? 'red' : 'green'"
                >
                  {{ record.passwordExpired ? '已过期' : '正常' }}
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
              </template>

              <template #dateCell="{ record, column }">
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
                    @click="handleOpenReset(record)"
                  >
                    重置密码
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleToggleStatus(record)"
                  >
                    {{ toggleStatusText(record.status) }}
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>

              <!-- ═══ 勾选后的批量操作条（BillTableList 内置；勾选结果由 @selection-change 同步到 selectedRows） ═══ -->
              <template #batch-actions>
                <a-button
                  size="small"
                  :disabled="!selectedRows.length"
                  :loading="batchSaving"
                  @click="handleBatchDisable(selectedRows)"
                >
                  批量停用
                </a-button>
                <a-button
                  size="small"
                  :disabled="!selectedRows.length"
                  @click="handleOpenBatchRoles(selectedRows)"
                >
                  批量分配角色
                </a-button>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（页长 [20,50,100]） ═══ -->
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

      <!-- ═══ 新增 / 修改操作员（账号属主数据：分区卡片 + 两列栅格 + 行内校验） ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="formState.id ? '修改操作员' : '新增操作员'"
        :width="760"
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
            title="账号信息"
            tip="登录账号创建后不可更改；编辑不提供改密入口，请走行内「重置密码」"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="登录账号"
                  name="username"
                >
                  <a-input
                    v-model:value="formState.username"
                    placeholder="3-50 位，租户内唯一"
                    :maxlength="50"
                    :disabled="!!formState.id"
                  />
                </a-form-item>
              </a-col>
              <a-col
                v-if="!formState.id"
                :span="12"
              >
                <a-form-item
                  label="初始密码"
                  name="password"
                  :extra="PASSWORD_POLICY_HINT"
                >
                  <a-input-password
                    v-model:value="formState.password"
                    placeholder="8-64 位，至少 3 类字符"
                    :maxlength="64"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="姓名"
                  name="nickname"
                >
                  <a-input
                    v-model:value="formState.nickname"
                    placeholder="请输入姓名"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="状态">
                  <a-select
                    v-model:value="formState.status"
                    :options="STATUS_FORM_OPTIONS"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="联系方式">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="手机号"
                  name="phone"
                >
                  <a-input
                    v-model:value="formState.phone"
                    placeholder="11 位手机号"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="邮箱"
                  name="email"
                >
                  <a-input
                    v-model:value="formState.email"
                    placeholder="name@example.com"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="组织与角色">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="所属部门">
                  <a-tree-select
                    v-model:value="formState.deptId"
                    placeholder="请选择所属部门"
                    allow-clear
                    tree-default-expand-all
                    :tree-data="deptTreeData"
                    :field-names="{ label: 'departmentName', value: 'id', children: 'children' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="角色">
                  <a-select
                    v-model:value="formState.roleIds"
                    mode="multiple"
                    placeholder="请选择角色"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="roleOptions"
                    @change="rolesTouched = true"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-alert
                  type="info"
                  show-icon
                  message="角色为覆盖式写入（后端先删后插）"
                  description="系统未提供「查询账号已有角色」接口：此处不选择即不改动原有角色；一旦选择并保存，该账号角色会被覆盖为本次勾选的角色。"
                />
              </a-col>
            </a-row>
          </FormSection>
          <!-- 缺口：表单不含「岗位」（post_id 无名称数据源且后端无岗位-账号维护 UI）、「备注」
               （UserCreateRequest / UserUpdateRequest 均无 remark 字段），需后端补齐后接入 -->
        </a-form>
      </a-modal>

      <!-- ═══ 重置密码（编辑态改密的唯一入口） ═══ -->
      <a-modal
        v-model:open="resetVisible"
        title="重置密码"
        :width="480"
        :mask-closable="false"
        :confirm-loading="resetSaving"
        ok-text="确认重置"
        cancel-text="取消"
        @ok="handleSubmitReset"
      >
        <a-alert
          class="dialog-alert"
          type="warning"
          show-icon
          :message="`将为账号「${resetTarget?.username || ''}」设置新密码`"
          description="重置后该账号旧密码立即失效（注：后端未联动踢出已登录会话，旧 token 在过期前仍可能可用）。"
        />
        <a-form
          :model="resetPwdForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item
            label="新密码"
            required
            :extra="PASSWORD_POLICY_HINT"
          >
            <a-input-password
              v-model:value="resetPwdForm.newPassword"
              placeholder="8-64 位，至少 3 类字符"
              :maxlength="64"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 批量分配角色 ═══ -->
      <a-modal
        v-model:open="batchRoleVisible"
        title="批量分配角色"
        :width="520"
        :mask-closable="false"
        :confirm-loading="batchSaving"
        ok-text="确认分配"
        cancel-text="取消"
        @ok="handleSubmitBatchRoles"
      >
        <a-alert
          class="dialog-alert"
          type="warning"
          show-icon
          :message="`将为 ${batchRoleTargetIds.length} 个账号覆盖式分配角色`"
          description="后端先删后插：所选账号原有的全部角色会被本次勾选的角色替换，请确认后再提交。"
        />
        <a-form
          :model="batchRoleForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item
            label="角色"
            required
          >
            <a-select
              v-model:value="batchRoleForm.roleIds"
              mode="multiple"
              placeholder="请选择角色"
              allow-clear
              show-search
              option-filter-prop="label"
              :options="roleOptions"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="md-staff-all-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
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
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { userApi, type UserInfo } from '@/api/user'
import { roleApi } from '@/api/role'
import { departmentApi } from '@/api/department'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'MdStaffAll' })

// ═══ 状态映射（口径：1 启用 / 0 禁用 / 2 锁定，见 HR 文档 §9 状态语义裁定） ═══
const GENDER_MAP: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
const USER_TYPE_MAP: Record<number, string> = { 0: '系统用户', 1: '企业用户', 2: '代理用户' }
const DATA_SCOPE_MAP: Record<string, string> = {
  ALL: '全数据',
  DEPT: '本部门',
  DEPT_CHILD: '本部门及子部门',
  SELF: '仅本人',
}
const STATUS_OPTIONS = [
  { label: '启用（正常）', value: 1 },
  { label: '禁用', value: 0 },
  { label: '锁定', value: 2 },
]
const STATUS_FORM_OPTIONS = [
  { label: '启用（正常）', value: 1 },
  { label: '禁用', value: 0 },
]

function statusText(status: number | null | undefined): string {
  if (status === 1) return '启用'
  if (status === 2) return '锁定'
  return '禁用'
}

function statusColor(status: number | null | undefined): string {
  if (status === 1) return 'green'
  if (status === 2) return 'red'
  return 'default'
}

/** 行内状态按钮：锁定态的「解锁」与启用同为 status=1，但文案必须区分，避免误判为解锁操作 */
function toggleStatusText(status: number | null | undefined): string {
  if (status === 1) return '禁用'
  if (status === 2) return '解锁'
  return '启用'
}

/** 密码有效期（天）：与后端 password.policy.max-age-days 默认值一致；后端为 @Value，改配置需重启 */
const PASSWORD_MAX_AGE_DAYS = 90
const PASSWORD_POLICY_HINT = `长度 8-64 位，且至少包含大写字母、小写字母、数字、特殊符号中的 3 类；不含空白字符；有效期 ${PASSWORD_MAX_AGE_DAYS} 天`

function isPasswordExpired(passwordUpdateTime: string | null | undefined): boolean {
  if (!passwordUpdateTime) return false
  return dayjs().diff(dayjs(passwordUpdateTime), 'day') > PASSWORD_MAX_AGE_DAYS
}

function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const resetSaving = ref(false)
const batchSaving = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

const userStore = useUserStore()
/** 平台超管编码，与 stores/user.ts 的 SUPER_ADMIN_ROLES 保持一致 */
const SUPER_ADMIN_ROLE_CODES = ['SUPER_ADMIN', 'admin', 'super_admin']

// ═══ 查询条件 ═══
const searchForm = reactive({
  username: '',
  deptId: undefined as number | undefined,
  status: undefined as number | undefined,
})

// ═══ 数据表列（默认可见 9 个业务列 + 锁定列序号/勾选/操作；其余 16 列为默认隐藏的治理字段） ═══
// 缺口：后端 /user/page 不支持排序参数（SysUserMapper.selectUserPage 固定 ORDER BY create_time DESC），
//       故不开放表头排序，避免「点了排序但顺序不变」的静默失效。
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  // 勾选列：key 必须为 'checkbox'（BillDetailTable LOCKED_COLUMNS 口径），且必须紧邻序号列
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 240, fixed: 'left' },
  // ── 默认可见 ──
  { key: 'username', title: '登录账号', type: 'slot', slotName: 'usernameCell', width: 150 },
  { key: 'nickname', title: '姓名', type: 'input', width: 110 },
  { key: 'phone', title: '手机号', type: 'input', width: 130 },
  { key: 'email', title: '邮箱', type: 'input', width: 180 },
  { key: 'deptName', title: '所属部门', type: 'input', width: 140 },
  { key: 'gender', title: '性别', type: 'slot', slotName: 'genderCell', width: 70 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'lastLoginTime', title: '最近登录', type: 'slot', slotName: 'dateCell', width: 170 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'dateCell', width: 170 },
  // ── 默认隐藏（有数据源未上列的治理 / 安全字段） ──
  { key: 'realName', title: '真实姓名', type: 'input', width: 120, defaultHidden: true },
  { key: 'avatar', title: '头像地址', type: 'input', width: 180, defaultHidden: true },
  // 缺口：post_id 有列无名称数据源（且 sys_user_position 表不可用），仅以 ID 形式提供
  { key: 'postId', title: '岗位ID', type: 'input', width: 120, defaultHidden: true },
  { key: 'userType', title: '用户类型', type: 'slot', slotName: 'userTypeCell', width: 110, defaultHidden: true },
  { key: 'isSuperAdmin', title: '超管', type: 'slot', slotName: 'booleanCell', width: 90, defaultHidden: true },
  { key: 'isTenantAdmin', title: '租户管理员', type: 'slot', slotName: 'booleanCell', width: 120, defaultHidden: true },
  { key: 'dataScope', title: '数据权限范围', type: 'slot', slotName: 'dataScopeCell', width: 130, defaultHidden: true },
  { key: 'loginCount', title: '登录次数', type: 'input', width: 90, defaultHidden: true },
  { key: 'lastLoginIp', title: '最后登录IP', type: 'input', width: 140, defaultHidden: true },
  { key: 'passwordUpdateTime', title: '密码更新时间', type: 'slot', slotName: 'dateCell', width: 170, defaultHidden: true },
  { key: 'passwordExpired', title: '密码是否过期', type: 'slot', slotName: 'passwordExpiredCell', width: 130, defaultHidden: true },
  { key: 'tenantId', title: '租户ID', type: 'input', width: 90, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'dateCell', width: 170, defaultHidden: true },
  // 缺口：create_by / update_by 无自动填充器，历史数据恒为空（后端需补 MetaObjectHandler）
  { key: 'createBy', title: '创建人ID', type: 'input', width: 100, defaultHidden: true },
  { key: 'updateBy', title: '更新人ID', type: 'input', width: 100, defaultHidden: true },
  // ⚠️ 禁止上列：password（BCrypt 哈希）/ extInfo / version / deleted
]

// ═══ 部门树（用于「所属部门」查询与表单；deptId → deptName 反查） ═══
const deptTreeData = ref<any[]>([])
const deptNameMap = new Map<string, string>()

function flattenDept(nodes: any[]) {
  for (const node of nodes || []) {
    deptNameMap.set(String(node.id), node.departmentName)
    if (node.children?.length) flattenDept(node.children)
  }
}

async function loadDeptTree() {
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    deptTreeData.value = list
    deptNameMap.clear()
    flattenDept(list)
    // 缺口：P1-4 —— DepartmentServiceImpl.listAll() 硬过滤 status=1，而种子部门 status 全为 0，
    //       因此本下拉在部分环境恒为空；部门名回退展示为 '-'，非本页可修。
  } catch (error) {
    console.warn('[全部操作员] 部门树加载失败', error)
    deptTreeData.value = []
  }
}

// ═══ 角色下拉（仅写不读：后端无 GET /user/{id}/roles） ═══
const roleOptions = ref<{ label: string; value: number }[]>([])

async function loadRoles() {
  try {
    const res: any = await roleApi.listAll()
    const list: any[] = Array.isArray(res) ? res : res?.records || res?.data || []
    roleOptions.value = list.map((r: any) => ({
      label: r.roleCode ? `[${r.roleCode}] ${r.roleName}` : r.roleName,
      value: r.id,
    }))
  } catch (error) {
    console.warn('[全部操作员] 角色列表加载失败', error)
  }
}

// ═══ 查询参数拼装 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  // P0-2 就近收敛：sys_user 被后端租户拦截器忽略（IGNORE_TENANT_TABLES），
  // 非平台超管必须显式带 tenantId，否则会拿到全库所有租户的账号。
  // 缺口：平台超管的「跨租户」应做成显式租户控件（并以后端强制 tenant 为准），当前未实现。
  const isPlatformSuperAdmin = userStore.roles.some(r => SUPER_ADMIN_ROLE_CODES.includes(r))
  if (!isPlatformSuperAdmin) params.tenantId = userStore.tenantId
  if (searchForm.username) params.username = searchForm.username.trim()
  if (searchForm.deptId != null) params.deptId = searchForm.deptId
  if (searchForm.status !== undefined) params.status = searchForm.status
  return params
}

// ═══ 数据加载 ═══
/** 行数据加工：补 deptName（后端只返回 deptId）与密码过期计算列 */
function enrichRows(rows: any[]): any[] {
  return (rows || []).map((row: any) => ({
    ...row,
    deptName: row.deptId != null ? deptNameMap.get(String(row.deptId)) || '-' : '-',
    passwordExpired: isPasswordExpired(row.passwordUpdateTime),
  }))
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await userApi.getPage(buildParams())
    tableData.value = enrichRows(res?.records || [])
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[全部操作员] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    // 换页/换查询后必须显式清空勾选：BillDetailTable 只清自身勾选态，
    // BillTableList 的 selectedRecords 不会随 dataSource 变化清空（会留下「勾选不显示但批量条还在」的错位）
    tableRef.value?.clearSelection?.()
    selectedRows.value = []
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.username = ''
  searchForm.deptId = undefined
  searchForm.status = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows || []
}

// ═══ 新增 / 修改弹窗 ═══
const modalVisible = ref(false)
const formRef = ref()
const rolesTouched = ref(false)
const originStatus = ref<number | null>(null)

const emptyForm = () => ({
  id: null as number | string | null,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  gender: undefined as number | undefined,
  deptId: undefined as number | undefined,
  status: 1 as number,
  roleIds: [] as number[],
})
const formState = reactive(emptyForm())

/** 密码复杂度校验：与后端 PasswordPolicy（8-64 位、4 类字符至少 3 类、无空白）保持一致 */
function validatePassword(_rule: any, value: string): Promise<void> {
  const pwd = value || ''
  if (!pwd) return Promise.reject('请输入初始密码')
  if (pwd.length < 8 || pwd.length > 64) return Promise.reject('密码长度必须在 8-64 个字符之间')
  if (/\s/.test(pwd)) return Promise.reject('密码不能包含空白字符')
  const categories = [/[A-Z]/, /[a-z]/, /\d/, /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?`~]/]
    .filter(re => re.test(pwd)).length
  if (categories < 3) return Promise.reject('密码必须至少包含大写字母、小写字母、数字、特殊符号中的 3 类')
  return Promise.resolve()
}

const rules: Record<string, any> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, max: 50, message: '登录账号长度 3-50 字符', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
  rolesTouched.value = false
  originStatus.value = null
}

function handleAdd() {
  resetForm()
  modalVisible.value = true
}

function handleEdit(record: any) {
  resetForm()
  formState.id = record.id
  formState.username = record.username || ''
  formState.nickname = record.nickname || ''
  formState.phone = record.phone || ''
  formState.email = record.email || ''
  formState.gender = record.gender ?? undefined
  formState.deptId = record.deptId ?? undefined
  formState.status = record.status ?? 1
  originStatus.value = record.status ?? null
  modalVisible.value = true
}

/** 列表返回的字段已可直接编辑（GET /user/{id} 需额外权限且数据同源），不额外请求详情 */
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (formState.id) {
      // 编辑：后端 UserUpdateRequest 只接收 昵称/邮箱/手机/头像/性别/部门/岗位，不回传 username/password
      // 手机号/邮箱传空串以支持「清空」（后端 updateById 忽略 null，只认非 null 值，传 undefined 会变成改不掉）
      // 缺口：性别 / 部门清空无法生效（后端 updateById 忽略 null，且无显式 set(null) 分支）
      await userApi.update(formState.id as number, {
        nickname: formState.nickname.trim(),
        email: formState.email.trim(),
        phone: formState.phone.trim(),
        gender: formState.gender,
        deptId: formState.deptId,
      })
      // 状态不在 UserUpdateRequest 内，须单独走 PUT /user/{id}/status
      if (originStatus.value !== null && formState.status !== originStatus.value) {
        await userApi.updateStatus(formState.id as number, formState.status)
      }
      // 角色为覆盖式写入且系统读不到已有角色：仅在用户实际改动且选了角色时才提交，避免清空原角色
      if (rolesTouched.value && formState.roleIds.length) {
        await userApi.assignRoles(formState.id as number, formState.roleIds)
      }
      message.success('操作员更新成功')
    } else {
      const newId: any = await userApi.create({
        username: formState.username.trim(),
        password: formState.password,
        nickname: formState.nickname.trim(),
        phone: formState.phone || undefined,
        email: formState.email || undefined,
        gender: formState.gender,
        deptId: formState.deptId,
      } as Partial<UserInfo> & { password: string })
      // P0-4：后端 createUser 硬写 status=0（未修前新建账号既显示禁用也登录不了），
      // 因此新建后按所选状态回写一次；缺 system:user:update-status 权限时仅提示，不回滚账号创建。
      if (newId && formState.status !== 0) {
        try {
          await userApi.updateStatus(Number(newId), formState.status)
        } catch (error: any) {
          message.warning(error?.response?.data?.message || '账号已创建，但状态回写失败（需 system:user:update-status 权限）')
        }
      }
      if (newId && formState.roleIds.length) {
        try {
          await userApi.assignRoles(Number(newId), formState.roleIds)
        } catch (error: any) {
          message.warning(error?.response?.data?.message || '账号已创建，但角色分配失败（需 system:user:assign-role 权限）')
        }
      }
      message.success('操作员创建成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 启用 / 禁用 / 解锁 ═══
function handleToggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  const actionText = target === 1 ? (record.status === 2 ? '解锁' : '启用') : '禁用'
  Modal.confirm({
    title: '确认',
    content: target === 1
      ? `确定要${actionText}账号「${record.username}」吗？`
      : `确定要禁用账号「${record.username}」吗？禁用后该账号将无法登录（后端当前未联动踢出已登录会话）。`,
    okType: target === 0 ? 'danger' : 'primary',
    onOk: async () => {
      try {
        await userApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

/** 批量停用：后端无批量状态端点，逐个调用 PUT /user/{id}/status（超管等会被后端 400 拒绝，按失败计数） */
async function handleBatchDisable(rows: any[]) {
  const targets = (rows || []).filter((r: any) => r.status !== 0)
  if (!targets.length) {
    message.warning('所选账号均已是禁用状态')
    return
  }
  Modal.confirm({
    title: '批量停用',
    content: `确定要停用所选 ${targets.length} 个账号吗？超管 / 租户管理员会被后端拒绝。`,
    okType: 'danger',
    onOk: async () => {
      batchSaving.value = true
      let ok = 0
      const failures: string[] = []
      for (const row of targets) {
        try {
          await userApi.updateStatus(row.id, 0)
          ok += 1
        } catch (error: any) {
          failures.push(`${row.username}：${error?.response?.data?.message || '失败'}`)
        }
      }
      batchSaving.value = false
      if (failures.length) {
        message.warning(`成功 ${ok} 个，失败 ${failures.length} 个（${failures.slice(0, 3).join('；')}${failures.length > 3 ? ' …' : ''}）`)
      } else {
        message.success(`已停用 ${ok} 个账号`)
      }
      fetchList()
    },
  })
}

// ═══ 删除 ═══
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除账号「${record.username}」吗？删除后该账号不再出现在台账中（后端不会清理其角色关联，也不校验关联业务数据）。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await userApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 重置密码 ═══
const resetVisible = ref(false)
const resetTarget = ref<any>(null)
const resetPwdForm = reactive({ newPassword: '' })

function handleOpenReset(record: any) {
  resetTarget.value = record
  resetPwdForm.newPassword = ''
  resetVisible.value = true
}

async function handleSubmitReset() {
  if (!resetTarget.value) return
  try {
    await validatePassword(null, resetPwdForm.newPassword)
  } catch (msg: any) {
    message.warning(typeof msg === 'string' ? msg : '密码不符合策略')
    return
  }
  resetSaving.value = true
  try {
    await userApi.resetPassword(resetTarget.value.id, resetPwdForm.newPassword)
    message.success(`账号「${resetTarget.value.username}」密码已重置`)
    resetVisible.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '重置密码失败')
  } finally {
    resetSaving.value = false
  }
}

// ═══ 批量分配角色 ═══
const batchRoleVisible = ref(false)
const batchRoleTargetIds = ref<number[]>([])
const batchRoleForm = reactive({ roleIds: [] as number[] })

function handleOpenBatchRoles(rows: any[]) {
  batchRoleTargetIds.value = (rows || []).map((r: any) => r.id)
  if (!batchRoleTargetIds.value.length) {
    message.warning('请先勾选账号')
    return
  }
  batchRoleForm.roleIds = []
  batchRoleVisible.value = true
}

async function handleSubmitBatchRoles() {
  if (!batchRoleForm.roleIds.length) {
    message.warning('请至少选择一个角色')
    return
  }
  batchSaving.value = true
  try {
    await userApi.batchAssignRoles(batchRoleTargetIds.value, batchRoleForm.roleIds)
    message.success(`已为 ${batchRoleTargetIds.value.length} 个账号分配角色`)
    batchRoleVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '分配角色失败')
  } finally {
    batchSaving.value = false
  }
}

// ═══ 打印(F8)：当前页账号台账（账号台账非票据，走浏览器打印，无打印模板）
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
      <td>${escapeHtml(r.username)}</td>
      <td>${escapeHtml(r.nickname)}</td>
      <td>${escapeHtml(r.phone)}</td>
      <td>${escapeHtml(r.email)}</td>
      <td>${escapeHtml(r.deptName)}</td>
      <td>${escapeHtml(GENDER_MAP[r.gender] || '')}</td>
      <td>${escapeHtml(statusText(r.status))}</td>
      <td>${escapeHtml(formatTime(r.lastLoginTime))}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>操作员账号台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>操作员账号台账</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}（当前页）</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>登录账号</th><th>姓名</th><th>手机号</th><th>邮箱</th>
        <th>所属部门</th><th>性别</th><th>状态</th><th>最近登录</th>
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

// ═══ 导出（CSV，含 BOM；不含任何密码字段） ═══
async function handleExport() {
  exporting.value = true
  try {
    // 导出按当前查询条件的全量口径（pageSize 10000 为保护上限），不是仅当前页
    const res: any = await userApi.getPage({ ...buildParams(), pageNum: 1, pageSize: 10000 })
    const rows = enrichRows(res?.records || [])
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['登录账号', '姓名', '手机号', '邮箱', '所属部门', '性别', '状态', '最近登录', '创建时间']
    const lines = rows.map((r: any) => [
      r.username, r.nickname, r.phone, r.email, r.deptName,
      GENDER_MAP[r.gender] || '', statusText(r.status),
      formatTime(r.lastLoginTime), formatTime(r.createTime),
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `操作员账号_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
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
  { key: 'username', label: '登录账号', visible: true },
  { key: 'nickname', label: '姓名（后端待支持）', visible: true },
  { key: 'phone', label: '手机号（后端待支持）', visible: false },
  { key: 'deptId', label: '部门', visible: true },
  { key: 'status', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增操作员', enabled: true },
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
  console.error('[全部操作员] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  // 部门树先加载：/user/page 返回的是 deptId，需要部门名映射再渲染「所属部门」列
  await loadDeptTree()
  loadRoles()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.risk-alert { margin-bottom: 10px; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.dialog-alert { margin-bottom: 12px; }

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
