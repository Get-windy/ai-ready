<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        职员部门（人力资源 → 职员管理 → 职员部门，菜单 80530）
        · 单视图主数据台账：左分类树 = 部门层级（选中节点即「只看该部门的下级部门」，选根「全部部门」看全部）
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增部门 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：部门编码 · 部门名称 · 状态
        · 数据来源 sys_department（表有 tenant_id，多租户拦截器正常注入）
        · 表单：弹窗内用分区卡片 + 两列栅格（部门是主数据档案，非单据，不用 BillFormPage）
      -->
      <a-tabs
        v-model:activeKey="activeTab"
        class="staff-dept-tabs"
        @change="handleTabChange"
      >
        <!-- ═══ 职员信息（对标 ql361 同名子标签；本表只读，增删改停用在「人力资源 → 员工列表」） ═══ -->
        <a-tab-pane
          key="staff"
          tab="职员信息"
        >
          <div class="staff-tab">
            <div class="search-area">
              <div class="search-row">
                <div class="search-item">
                  <span class="search-label">职员编号/姓名</span>
                  <a-input
                    v-model:value="staffSearch.keyword"
                    placeholder="请输入职员编号/姓名"
                    size="small"
                    style="width: 200px"
                    allow-clear
                    @press-enter="handleStaffSearch"
                  />
                </div>
                <a-button
                  type="primary"
                  size="small"
                  @click="handleStaffSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleStaffReset"
                >
                  重置
                </a-button>
                <a-button
                  size="small"
                  @click="handleGoEmployee"
                >
                  前往员工管理
                </a-button>
              </div>
            </div>
            <div class="table-area">
              <BillTableList
                :columns="staffColumns"
                :data-source="staffData"
                :loading="staffLoading"
                :pagination="false"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                storage-key="md-staff-dept-staff-columns"
                global-config-key="md-staff-dept-staff-columns"
                row-key="id"
              />
            </div>
            <StandardPagination
              variant="classic"
              :current="staffPagination.current"
              :page-size="staffPagination.pageSize"
              :total="staffPagination.total"
              :page-size-options="[20, 50, 100]"
              @change="handleStaffPageChange"
            />
          </div>
        </a-tab-pane>

        <!-- ═══ 部门信息（原有部门主数据 CRUD，行为不变） ═══ -->
        <a-tab-pane
          key="dept"
          tab="部门信息"
        >
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="部门"
        :category-editable="false"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="selectedDeptId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentDeptPath"
        :show-table-footer="true"
        @category-select="handleDeptSelect"
        @category-expand="handleDeptExpand"
      >
        <!-- ═══ 工具栏左侧：新增部门 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增部门
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
                v-if="isQueryVisible('departmentCode')"
                class="search-item"
              >
                <span class="search-label">部门编码</span>
                <a-input
                  v-model:value="searchForm.departmentCode"
                  placeholder="请输入部门编码"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('departmentName')"
                class="search-item"
              >
                <span class="search-label">部门名称</span>
                <a-input
                  v-model:value="searchForm.departmentName"
                  placeholder="请输入部门名称"
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
              storage-key="md-staff-dept-table-columns"
              global-config-key="md-staff-dept-table-columns"
              row-key="id"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.departmentName }}</a>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.status === 1 ? 'green' : 'default'"
                >
                  {{ record.status === 1 ? '启用' : '禁用' }}
                </a-tag>
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
                    title="确认删除该部门？"
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
        </a-tab-pane>
      </a-tabs>

      <!-- ═══ 新增 / 修改弹窗（主数据：分区卡片 + 两列栅格） ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="formState.id ? '修改部门' : '新增部门'"
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
            title="部门信息"
            tip="部门编码在租户内唯一，编辑时不可改动；不选上级即为顶级部门"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="部门编码"
                  name="departmentCode"
                >
                  <a-input
                    v-model:value="formState.departmentCode"
                    placeholder="请输入部门编码"
                    :maxlength="100"
                    :disabled="!!formState.id"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="部门名称"
                  name="departmentName"
                >
                  <a-input
                    v-model:value="formState.departmentName"
                    placeholder="请输入部门名称"
                    :maxlength="200"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="上级部门">
                  <a-tree-select
                    v-model:value="formState.parentId"
                    :tree-data="parentTreeSelectData"
                    :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                    placeholder="不选则为顶级部门"
                    allow-clear
                    tree-default-expand-all
                    :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="负责人">
                  <a-input
                    v-model:value="formState.leaderName"
                    placeholder="请输入负责人姓名"
                    :maxlength="100"
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
            </a-row>
          </FormSection>

          <FormSection title="联系方式">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="联系电话">
                  <a-input
                    v-model:value="formState.phone"
                    placeholder="请输入联系电话"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="邮箱">
                  <a-input
                    v-model:value="formState.email"
                    placeholder="请输入邮箱"
                    :maxlength="200"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="备注">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="formState.description"
                    placeholder="请输入部门备注"
                    :rows="2"
                    :maxlength="500"
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
        storage-key="md-staff-dept-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
// 缺口（后端能力缺失，本页只做前端兜底，未改任何后端文件）：
// ① 负责人 leaderName / leaderId 无选项来源：员工 hr_employee 与系统用户 sys_user 口径未裁定 → 本页仅提供文本输入，实际写入 leader_name。
// ② 上级部门下拉与左树共用 GET /department/list|tree，后端硬过滤 status=1，而存量部门 status 全为 0 → 下拉可能为空（P0，需后端补不过滤状态的 /department/options）。
// ③ 后端删除不校验 sys_user.dept_id / sys_position.dept_id / hr_employee.dept_id 引用 → 可产生孤儿引用（P0，须后端补引用校验，本页无法拦）。
// ④ 后端 update 改 parentId 不重算 ancestors → 合法与非法层级并存，由后端修复；本页仅做「排除自己及整棵后代子树」防自环。
// ⑤ 后端 GET /department/page 无排序参数 → 本页列不开放服务端排序。
// ⑥ 后端 GET /department/export 为无 BOM 的 CSV 全量端点（Excel 中文乱码）→ 本页改用 /page 拉全量在前端生成带 BOM 的 CSV，departmentApi.export 仍未接线。
// ⑦ 规格判定本页打印 N/A（本模块无 ql361 对标页）；此处按调用方要求保留 F8 打印当前筛选结果，可由页面配置关闭。
// ⑧ 负责人口径下的「部门负责人（现任员工）」、成本中心、编制数、部门合并等业界能力本系统未实现，无对应接口。
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
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
import { departmentApi } from '@/api/department'
import { hrEmployeeApi } from '@/api/hr'

defineOptions({ name: 'MdStaffDept' })

const router = useRouter()

// ═══ 双 Tab（对标 ql361「资料 → 职员权限 → 职员部门」页的两个子标签：职员信息 | 部门信息） ═══
// 默认停在「部门信息」以保持本页既有行为不变（本页菜单语义主体是部门主数据）。
const activeTab = ref<'staff' | 'dept'>('dept')
const staffLoaded = ref(false)

// ── 职员信息 Tab：只读职员清单 ───────────────────────────────────────
// 数据源 hr_employee（本系统职员档案的权威来源），**只读**：增删改停用统一在
// 「人力资源 → 员工管理 → 员工列表」(hr/employee) 维护，避免同一主数据两处可写。
// 列对齐 ql361 实抓的 10 列，其中「直属上级」hr_employee 无对应字段，故本表不上该列（已在开发文档登记为缺口）。
const staffLoading = ref(false)
const staffData = ref<any[]>([])
const staffPagination = reactive({ current: 1, pageSize: 20, total: 0 })
const staffSearch = reactive({ keyword: '' })

const staffColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'employeeNo', title: '职员编号', type: 'input', width: 110 },
  { key: 'employeeName', title: '职员姓名', type: 'input', width: 120 },
  { key: 'genderText', title: '性别', type: 'input', width: 70 },
  { key: 'deptName', title: '部门', type: 'input', width: 120 },
  { key: 'userPositionName', title: '职务', type: 'input', width: 130 },
  { key: 'phone', title: '联系电话', type: 'input', width: 130 },
  { key: 'isOperatorText', title: '是操作员', type: 'input', width: 90 },
  { key: 'userLoginName', title: '操作员账号', type: 'input', width: 150 },
]

async function fetchStaff() {
  staffLoading.value = true
  try {
    const res: any = await hrEmployeeApi.page({
      pageNum: staffPagination.current,
      pageSize: staffPagination.pageSize,
      keyword: staffSearch.keyword.trim() || undefined,
    })
    const records = res?.records || res?.data?.records || []
    staffData.value = records.map((r: any) => ({
      ...r,
      genderText: r.gender === 1 ? '男' : r.gender === 2 ? '女' : '-',
      // 「是操作员」以是否绑定系统账号（user_id）判定，与「全部操作员」页同源
      isOperatorText: r.userId ? '√' : '',
    }))
    staffPagination.total = Number(res?.total ?? res?.data?.total ?? 0) || 0
  } catch (error: any) {
    console.error('[职员部门] 加载职员清单失败', error)
    message.error(error?.response?.data?.message || '加载职员清单失败')
    staffData.value = []
    staffPagination.total = 0
  } finally {
    staffLoading.value = false
  }
}

function handleStaffSearch() {
  staffPagination.current = 1
  fetchStaff()
}

function handleStaffReset() {
  staffSearch.keyword = ''
  handleStaffSearch()
}

/** 跳到员工列表维护职员档案（本 Tab 刻意只读，避免同一主数据两处可写） */
function handleGoEmployee() {
  router.push('/hr/employee')
}

function handleStaffPageChange(page: number, pageSize: number) {
  staffPagination.current = page
  staffPagination.pageSize = pageSize
  fetchStaff()
}

/** 切到「职员信息」时懒加载一次；切回不重复请求 */
function handleTabChange(key: string | number) {
  if (key === 'staff' && !staffLoaded.value) {
    staffLoaded.value = true
    fetchStaff()
  }
}

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

// ═══ 数据表列（默认可见 8 个数据列 + 序号 + 操作；邮箱/备注默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 180, fixed: 'left' },
  { key: 'departmentCode', title: '部门编码', type: 'input', width: 130 },
  { key: 'departmentName', title: '部门名称', type: 'slot', slotName: 'nameCell', width: 180 },
  { key: 'parentName', title: '上级部门', type: 'input', width: 140 },
  { key: 'leaderName', title: '负责人', type: 'input', width: 110 },
  { key: 'phone', title: '联系电话', type: 'input', width: 130 },
  { key: 'sort', title: '排序', type: 'input', width: 80, align: 'right' },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'timeCell', width: 170 },
  // ── 默认隐藏 ──
  { key: 'email', title: '邮箱', type: 'input', width: 200, defaultHidden: true },
  { key: 'description', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  departmentCode: '',
  departmentName: '',
  status: undefined as number | undefined,
})

// ═══ 左侧部门分类树（数据源 GET /department/tree） ═══
const ROOT_DEPT_ID = '0'
const selectedDeptId = ref<string | number>(ROOT_DEPT_ID)
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const expandedKeys = ref<(string | number)[]>([])
const deptMap = ref<Record<string, string>>({})

const currentDeptPath = computed(() => {
  if (String(selectedDeptId.value) === ROOT_DEPT_ID) return '全部部门'
  return deptMap.value[String(selectedDeptId.value)] || '全部部门'
})

function flattenDept(nodes: any[], map: Record<string, string>) {
  for (const n of nodes || []) {
    map[String(n.id)] = n.categoryName
    if (n.children?.length) flattenDept(n.children, map)
  }
}

async function loadDeptTree() {
  categoryLoading.value = true
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : Array.isArray(res?.data) ? res.data : []
    const mapped = list.map((d: any) => ({
      id: d.id,
      categoryName: d.departmentName,
      children: d.children,
    }))
    categoryTreeData.value = [{ id: ROOT_DEPT_ID, categoryName: '全部部门', children: mapped }]
    const map: Record<string, string> = {}
    flattenDept(mapped, map)
    deptMap.value = map
    expandedKeys.value = [ROOT_DEPT_ID]
  } catch (error) {
    console.warn('[职员部门] 部门树加载失败', error)
    categoryTreeData.value = [{ id: ROOT_DEPT_ID, categoryName: '全部部门' }]
    deptMap.value = {}
  } finally {
    categoryLoading.value = false
  }
}

/** 选中分类节点：根节点看全部，其余节点看该部门的下级部门（后端按 parentId 精确过滤） */
function handleDeptSelect(keys: any[]) {
  const key = keys?.[0]
  selectedDeptId.value = key === undefined || key === null ? ROOT_DEPT_ID : key
  handleSearch()
}

function handleDeptExpand(keys: any[]) {
  expandedKeys.value = keys || []
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (String(selectedDeptId.value) !== ROOT_DEPT_ID) params.parentId = selectedDeptId.value
  if (searchForm.departmentCode) params.departmentCode = searchForm.departmentCode.trim()
  if (searchForm.departmentName) params.departmentName = searchForm.departmentName.trim()
  if (searchForm.status !== undefined) params.status = searchForm.status
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await departmentApi.getPage(buildParams())
    // 兼容两种形状：拦截器已拆包的 {records,total} 与未拆包的 {data:{records,total}}
    const records = res?.records || res?.data?.records || []
    const total = res?.total ?? res?.data?.total ?? 0
    tableData.value = records
    pagination.total = Number(total) || 0
  } catch (error: any) {
    console.error('[职员部门] 加载列表失败', error)
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
  searchForm.departmentCode = ''
  searchForm.departmentName = ''
  searchForm.status = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 展示辅助 ═══
function formatTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function statusText(status: number | undefined): string {
  return status === 1 ? '启用' : '禁用'
}

// ═══ 新增 / 修改弹窗 ═══
const modalVisible = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null as string | number | null,
  departmentCode: '',
  departmentName: '',
  parentId: undefined as any,
  leaderName: '',
  phone: '',
  email: '',
  sort: 0,
  status: 1,
  description: '',
})
const formState = reactive(emptyForm())

const rules: Record<string, any> = {
  departmentCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }],
  departmentName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
}

/** 上级部门下拉数据：真实部门树（不含「全部部门」根），并排除自己及整棵后代子树以防组织树成环 */
function excludeSubtree(nodes: any[], id: string | number | null | undefined): any[] {
  if (id === null || id === undefined) return nodes || []
  return (nodes || [])
    .filter((n: any) => String(n.id) !== String(id))
    .map((n: any) => ({
      id: n.id,
      categoryName: n.categoryName,
      children: n.children ? excludeSubtree(n.children, id) : undefined,
    }))
}

const parentTreeSelectData = computed(() =>
  excludeSubtree(categoryTreeData.value[0]?.children || [], formState.id)
)

function resetForm() {
  Object.assign(formState, emptyForm())
}

function handleAdd() {
  resetForm()
  modalVisible.value = true
}

/** 编辑一律先拉详情回填（不用列表行，避免缺字段被清空） */
async function handleEdit(record: any) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await departmentApi.getById(record.id)
    const data = detail?.data || detail
    if (!data) return
    formState.id = data.id
    formState.departmentCode = data.departmentCode || ''
    formState.departmentName = data.departmentName || ''
    // parentId=0 / null 均表示顶级部门，统一回填为空，避免下拉显示裸 id
    const pid = data.parentId
    formState.parentId = pid === null || pid === undefined || String(pid) === '0' ? undefined : pid
    formState.leaderName = data.leaderName || ''
    formState.phone = data.phone || ''
    formState.email = data.email || ''
    formState.sort = data.sort ?? 0
    formState.status = data.status ?? 1
    formState.description = data.description || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载部门详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload: any = {
    departmentCode: formState.departmentCode.trim(),
    departmentName: formState.departmentName.trim(),
    parentId: formState.parentId ?? null,
    leaderName: formState.leaderName || null,
    phone: formState.phone || null,
    email: formState.email || null,
    sort: formState.sort ?? 0,
    status: formState.status,
    description: formState.description || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      await departmentApi.update(formState.id as number, payload)
      message.success('修改成功')
      modalVisible.value = false
      fetchList()
    } else {
      await departmentApi.create(payload)
      message.success('新增成功')
      modalVisible.value = false
      // 新建会改变部门层级 → 左树与列表一并刷新
      loadDeptTree()
      fetchList()
    }
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
    content: `确定要${actionText}部门「${record.departmentName}」吗？`,
    onOk: async () => {
      try {
        await departmentApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

// ═══ 删除 ═══
async function handleDelete(record: any) {
  try {
    await departmentApi.delete(record.id)
    message.success('删除成功')
    loadDeptTree()
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 打印(F8)：部门台账（当前筛选结果） ═══
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
      <td>${escapeHtml(r.departmentCode)}</td>
      <td>${escapeHtml(r.departmentName)}</td>
      <td>${escapeHtml(r.parentName || '')}</td>
      <td>${escapeHtml(r.leaderName || '')}</td>
      <td>${escapeHtml(r.phone || '')}</td>
      <td>${escapeHtml(r.email || '')}</td>
      <td>${escapeHtml(r.sort ?? '')}</td>
      <td>${escapeHtml(statusText(r.status))}</td>
      <td>${escapeHtml(formatTime(r.createTime))}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>部门台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>部门台账</h2>
    <div class="meta">
      <span>部门范围：${escapeHtml(currentDeptPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>部门编码</th><th>部门名称</th><th>上级部门</th><th>负责人</th>
        <th>联系电话</th><th>邮箱</th><th>排序</th><th>状态</th><th>创建时间</th>
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

// ═══ 导出（CSV：按当前筛选与部门范围拉全量，带 BOM 防 Excel 中文乱码） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await departmentApi.getPage({ ...params, pageNum: 1, pageSize: 10000 })
    const records = res?.records || res?.data?.records || []
    if (!records.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['部门编码', '部门名称', '上级部门', '负责人', '联系电话', '邮箱', '排序', '状态', '备注', '创建时间']
    const lines = records.map((r: any) => [
      r.departmentCode, r.departmentName, r.parentName, r.leaderName, r.phone, r.email,
      r.sort, statusText(r.status), r.description, formatTime(r.createTime),
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `职员部门_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
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
  { key: 'departmentCode', label: '部门编码', visible: true },
  { key: 'departmentName', label: '部门名称', visible: true },
  { key: 'status', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增部门', enabled: true },
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
  console.error('[职员部门] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadDeptTree()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
/* 插槽内容的样式必须自备：布局组件的 scoped 样式不作用于注入的插槽内容 */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：表格根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* ═══ 双 Tab 高度链 ═══
   PageContainer(full-height) → a-tabs → tab-pane → 内容 必须逐层撑满，
   否则表格根元素（flex:1）高度会塌陷为 0（本项目踩过的坑）。 */
.staff-dept-tabs { height: 100%; display: flex; flex-direction: column; }
.staff-dept-tabs :deep(.ant-tabs-nav) { margin: 0; flex-shrink: 0; }
.staff-dept-tabs :deep(.ant-tabs-content-holder) { flex: 1; min-height: 0; }
.staff-dept-tabs :deep(.ant-tabs-content) { height: 100%; }
.staff-dept-tabs :deep(.ant-tabs-tabpane) { height: 100%; }
/* 职员信息 Tab：查询区 + 表格 + 经典分页 的弹性纵向布局 */
.staff-tab { height: 100%; display: flex; flex-direction: column; }
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
