<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        买家账号（交易 → 商城 → 基础业务 → 买家账号，对标 ql361「商城 → 基础业务 → 买家账号」）
        · 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/买家账号开发文档.md
        · 页面定位：单入口单视图列表页 = 左侧「买家归属分类」分类树 + 右侧买家账号数据表（无子 Tab）
          对标无「页面配置」弹窗（查询区/按钮为固定项）
        · 列配置：走 BillDetailTable 表头 rowNo 列齿轮（个人配置 / 全局配置），storage-key 持久化
        · 列数：可配置 15 列 = 对标 9 列（客户名称/注册时间/最近交易时间/最后登录时间/登录名/默认经手人/客户级别/所属仓库/所属部门，全默认显示）
                              + 本系统扩展 6 列（状态 默认显示；手机号码/昵称/身份/审核状态/来源 默认隐藏）
          默认显示 10 列、默认隐藏 5 列；固定列（非列配置）：序号、操作
        · 工具栏：注册验证码查询 ｜ 刷新 / 打印(F8) / 导出（对标固定按钮，无「新增」）
        · 行级操作：删除 / 编辑 / 解锁（解锁=恢复停用/锁定账号；原「禁用」逻辑保留，走同一状态接口）
        · 数据来源：shop_user（API /erp/mall/admin/user：page / {id} PUT（编辑） / {id} DELETE（删除） / {id}/status）；
          分类树来源 biz_party_category（API /erp/partner/categories/tree?categoryType=CUSTOMER）
        · 后端字段：shop_user 已补 14 个字段（含 categoryId / defaultHandlerName / customerLevel /
          warehouseName / deptName，Flyway V11.361.3），上述列均有数据源。
        · 本轮已闭环后端缺口（交易模块金标准）：
          1) /user/page 已接收 createTimeStart / createTimeEnd（注册时间，截止日含当天）、
             categoryId（归属分类，分类树选中即参与过滤）、gradeId（客户级别）、showDisabled（显示停用）
          2) PUT /erp/mall/admin/user/{id}（编辑买家账号，专用 Update DTO 部分更新，落 shop_user）
          3) DELETE /erp/mall/admin/user/{id}（删除买家账号，逻辑删除 deleted=1，非物理删除）
        · 仍缺的后端能力（保留缺口，不造假数据）：
          1) shop_user 无「最近交易时间」（lastTradeTime）→ 该列恒显示 '-'
          2) 注册验证码查询：系统**无验证码存储数据源**（无 sms_record/verify_code 类表，
             shop_user 无验证码列，商城注册流程 MallAuthServiceImpl.register 既不发送也不校验验证码，
             通知模块验证码仅存内存且非注册验证码）→ 按钮维持「待补」提示，不造表、不返回假数据
          3) 对标固定查询项「客户 / 所属仓库 / 经手人 / 所属部门」后端无对应过滤参数（查询区亦未实现这些项）
      -->
      <CategoryListLayout
        category-title="买家归属分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentPath"
        :tabs="[]"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeys = keys)"
        @category-retry="loadCategoryTree"
      >
        <!-- ═══ 工具栏左侧：注册验证码查询（对标固定按钮；系统无验证码存储数据源 → 维持待补提示） ═══ -->
        <template #toolbar-left>
          <a-button
            size="small"
            @click="handleVerifyCodeQuery"
          >
            注册验证码查询
          </a-button>
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

        <!-- ═══ 查询区（对标固定项：注册时间起止 + 筛选条件 + 客户级别 + 显示停用） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">注册时间</span>
              <a-range-picker
                v-model:value="searchForm.dateRange"
                size="small"
                style="width: 230px"
                value-format="YYYY-MM-DD"
                @change="handleSearch"
              />
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入客户名称/登录名"
                size="small"
                style="width: 190px"
                allow-clear
                @press-enter="handleSearch"
              />
              <span class="search-label">客户级别</span>
              <a-select
                v-model:value="searchForm.gradeId"
                placeholder="全部"
                size="small"
                style="width: 140px"
                allow-clear
                :options="gradeOptions"
                @change="handleSearch"
              />
              <a-checkbox
                v-model:checked="searchForm.showDisabled"
                @change="handleSearch"
              >
                显示停用
              </a-checkbox>
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
              storage-key="mall-buyer-account-table-columns"
              global-config-key="mall-buyer-account-table-columns"
            >
              <!-- 客户名称（企业客户取公司名称，个人会员取昵称） -->
              <template #customerNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.companyName || record.nickname || record.username || '-' }}</span>
              </template>

              <!-- 注册时间 -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 最近交易时间（shop_user 暂无 lastTradeTime 字段） -->
              <template #lastTradeTimeCell="{ record }">
                {{ fmtTime(record.lastTradeTime) }}
              </template>

              <!-- 最后登录时间 -->
              <template #lastLoginTimeCell="{ record }">
                {{ fmtTime(record.lastLoginTime) }}
              </template>

              <!-- 登录名 -->
              <template #usernameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.username || '-' }}</span>
              </template>

              <!-- 默认经手人（shop_user.default_handler_name） -->
              <template #defaultHandlerCell="{ record }">
                <span v-if="!record.__ghost">{{ record.defaultHandlerName || record.defaultHandler || '-' }}</span>
              </template>

              <!-- 客户级别（shop_user.customer_level） -->
              <template #customerLevelCell="{ record }">
                <span v-if="!record.__ghost">{{ record.customerLevelName || record.customerLevel || '-' }}</span>
              </template>

              <!-- 所属仓库（shop_user.warehouse_name） -->
              <template #warehouseNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.warehouseName || '-' }}</span>
              </template>

              <!-- 所属部门（shop_user.dept_name） -->
              <template #deptNameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.deptName || '-' }}</span>
              </template>

              <!-- 状态：正常 / 已禁用（对标「固定列·状态」） -->
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.status === 1 ? 'green' : 'red'"
                >
                  {{ record.status === 1 ? '正常' : '已禁用' }}
                </a-tag>
              </template>

              <!-- 本系统扩展列（默认隐藏） -->
              <template #phoneCell="{ record }">
                <span v-if="!record.__ghost">{{ record.phone || '-' }}</span>
              </template>
              <template #nicknameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.nickname || '-' }}</span>
              </template>
              <template #userTypeCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.userType === 'ENTERPRISE' ? '企业客户' : record.userType === 'MEMBER' ? '个人会员' : '-' }}
                </span>
              </template>
              <template #auditStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="AUDIT_STATUS_MAP[record.auditStatus]?.color || 'default'"
                >
                  {{ AUDIT_STATUS_MAP[record.auditStatus]?.label || '未知' }}
                </a-tag>
              </template>
              <template #sourceCell="{ record }">
                <span v-if="!record.__ghost">{{ record.source || '-' }}</span>
              </template>

              <!-- 操作列（对标：删除 / 编辑 / 解锁） -->
              <template #actionCell="{ record, column }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                  :data-col="column.key"
                >
                  <a-popconfirm
                    title="确认删除该买家账号？删除后账号从列表移除（后端逻辑删除，非物理删除）"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDelete(record as ShopUser)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      删除
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record as ShopUser)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status !== 1"
                    type="link"
                    size="small"
                    style="color: #52c41a"
                    @click="handleUnlock(record as ShopUser)"
                  >
                    解锁
                  </a-button>
                  <a-button
                    v-else
                    type="link"
                    size="small"
                    @click="handleDisable(record as ShopUser)"
                  >
                    禁用
                  </a-button>
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

      <!-- ═══ 编辑买家账号弹窗（PUT /erp/mall/admin/user/{id}，字段与后端 ShopUserUpdateRequest 对齐） ═══ -->
      <a-modal
        v-model:open="editOpen"
        title="编辑买家账号"
        :width="720"
        :confirm-loading="editSaving"
        ok-text="保存"
        cancel-text="取消"
        :mask-closable="false"
        @ok="handleEditSubmit"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="登录名">
                <a-input
                  :value="editRecord?.username"
                  disabled
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="昵称">
                <a-input
                  v-model:value="editForm.nickname"
                  placeholder="请输入昵称"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="真实姓名">
                <a-input
                  v-model:value="editForm.contactName"
                  placeholder="请输入真实姓名"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="手机号">
                <a-input
                  v-model:value="editForm.phone"
                  placeholder="请输入手机号"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="公司名称">
                <a-input
                  v-model:value="editForm.companyName"
                  placeholder="企业客户公司名称"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="邮箱">
                <a-input
                  v-model:value="editForm.email"
                  placeholder="请输入邮箱"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="客户级别">
                <a-select
                  v-model:value="editForm.customerLevel"
                  placeholder="请选择客户级别"
                  allow-clear
                  :options="gradeLevelOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="归属分类">
                <a-select
                  v-model:value="editForm.categoryId"
                  placeholder="请选择归属分类"
                  allow-clear
                  :options="categoryOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="默认经手人">
                <a-select
                  v-model:value="editForm.defaultHandlerId"
                  placeholder="请选择默认经手人"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="editHandlerOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="所属仓库">
                <a-select
                  v-model:value="editForm.warehouseId"
                  placeholder="请选择所属仓库"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="editWarehouseOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="所属部门">
                <a-select
                  v-model:value="editForm.deptId"
                  placeholder="请选择所属部门"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="editDeptOptions"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="QQ">
                <a-input
                  v-model:value="editForm.qq"
                  placeholder="请输入QQ"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="微信">
                <a-input
                  v-model:value="editForm.wechat"
                  placeholder="请输入微信"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="地址"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 21 }"
              >
                <a-input
                  v-model:value="editForm.address"
                  placeholder="请输入地址"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 21 }"
              >
                <a-textarea
                  v-model:value="editForm.remark"
                  :rows="3"
                  placeholder="请输入备注"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="mall-buyer-account"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { shopUserApi, type ShopUser, type ShopUserPageParams, type ShopUserUpdatePayload } from '@/api/erp/mall'
import { partnerCategoryApi, partnerGradeApi } from '@/api/erp/partner'
import optionsApi from '@/api/options'
import { warehousePlanApi } from '@/api/erp/warehousePlan'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MallBuyerAccount' })

// ═══ 审核状态（与后端 ShopUser.auditStatus 一致：0待审核 1通过 2驳回） ═══
const AUDIT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已通过', color: 'green' },
  2: { label: '已驳回', color: 'red' }
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<ShopUser[]>([])

// ═══ 查询条件（对标固定项：注册时间起止 / 筛选条件 / 客户级别 / 显示停用）
//     参数口径：createTimeStart/createTimeEnd（注册时间，截止日含当天）、categoryId（分类树选中）、
//     gradeId（客户级别ID，后端解析为级别名称匹配 customer_level）、showDisabled（false=只看启用）
const searchForm = reactive({
  keyword: '' as string,
  dateRange: undefined as [string, string] | undefined,
  gradeId: undefined as number | undefined,
  showDisabled: true,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 列定义（DetailColumnConfig[]）
 * 对标 9 列（全默认显示）：客户名称/注册时间/最近交易时间/最后登录时间/登录名/默认经手人/客户级别/所属仓库/所属部门
 * 本系统扩展：状态（默认显示，对标列为「固定列·状态」）；手机号码/昵称/身份/审核状态/来源（默认隐藏）
 * 固定列（不进列配置面板）：rowNo（承载列配置齿轮）、action
 */
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 180, fixed: 'left' },
  { key: 'customerName', title: '客户名称', type: 'slot', slotName: 'customerNameCell', width: 180, sortable: true },
  { key: 'createTime', title: '注册时间', type: 'slot', slotName: 'createTimeCell', width: 150 },
  { key: 'lastTradeTime', title: '最近交易时间', type: 'slot', slotName: 'lastTradeTimeCell', width: 150 },
  { key: 'lastLoginTime', title: '最后登录时间', type: 'slot', slotName: 'lastLoginTimeCell', width: 150 },
  { key: 'username', title: '登录名', type: 'slot', slotName: 'usernameCell', width: 140 },
  { key: 'defaultHandler', title: '默认经手人', type: 'slot', slotName: 'defaultHandlerCell', width: 120 },
  { key: 'customerLevel', title: '客户级别', type: 'slot', slotName: 'customerLevelCell', width: 140 },
  { key: 'warehouseName', title: '所属仓库', type: 'slot', slotName: 'warehouseNameCell', width: 130 },
  { key: 'deptName', title: '所属部门', type: 'slot', slotName: 'deptNameCell', width: 120 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  // ── 本系统扩展列（默认隐藏） ──
  { key: 'phone', title: '手机号码', type: 'slot', slotName: 'phoneCell', width: 130, defaultHidden: true },
  { key: 'nickname', title: '昵称', type: 'slot', slotName: 'nicknameCell', width: 120, defaultHidden: true },
  { key: 'userType', title: '身份', type: 'slot', slotName: 'userTypeCell', width: 100, defaultHidden: true },
  { key: 'auditStatus', title: '审核状态', type: 'slot', slotName: 'auditStatusCell', width: 100, defaultHidden: true },
  { key: 'source', title: '来源', type: 'slot', slotName: 'sourceCell', width: 90, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 分类树（买家归属分类）：来源 biz_party_category.party_type=CUSTOMER ═══
const ROOT_CATEGORY_ID = '__all__'
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<any[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => [
  { id: ROOT_CATEGORY_ID, categoryName: '全部客户', children: categoryList.value },
])

const currentPath = computed(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return '全部客户'
  const path: string[] = []
  const walk = (nodes: any[], target: string): boolean => {
    for (const n of nodes) {
      path.push(n.categoryName)
      if (String(n.id) === target) return true
      if (n.children?.length && walk(n.children, target)) return true
      path.pop()
    }
    return false
  }
  walk(categoryList.value, String(selectedCategoryId.value))
  return path.length ? path.join(' / ') : '全部客户'
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await partnerCategoryApi.getTree('CUSTOMER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[买家账号] 分类树加载失败', e)
    categoryError.value = true
    categoryList.value = []
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : ROOT_CATEGORY_ID
  handleSearch()
}

// ═══ 客户级别选项（来源 GET /erp/partner/grades → biz_customer_grade；
//     本页同时用于：查询区 gradeId 过滤（后端解析为级别名称匹配 customer_level）与编辑弹窗「客户级别」） ═══
const gradeOptions = ref<{ label: string; value: number }[]>([])

async function loadGrades() {
  try {
    const list = await partnerGradeApi.list()
    gradeOptions.value = (list || []).map((g: any) => ({ label: g.gradeName, value: g.id }))
  } catch (e) {
    console.warn('[买家账号] 客户级别加载失败', e)
    gradeOptions.value = []
  }
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const params: ShopUserPageParams = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      // 显示停用：默认勾选 → 不传（后端不过滤，含停用账号）；取消勾选 → showDisabled=false（后端强制只看启用）
      showDisabled: searchForm.showDisabled ? undefined : false,
      // 注册时间起止（后端按 create_time 过滤，截止日含当天 23:59:59）
      createTimeStart: searchForm.dateRange?.[0] || undefined,
      createTimeEnd: searchForm.dateRange?.[1] || undefined,
      // 归属分类：分类树根节点「全部客户」= 不过滤
      categoryId: String(selectedCategoryId.value) === ROOT_CATEGORY_ID ? undefined : Number(selectedCategoryId.value),
      // 客户级别：后端按 biz_customer_grade.id 解析级别名称后匹配 shop_user.customer_level
      gradeId: searchForm.gradeId,
    }
    const res: any = await shopUserApi.page(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[买家账号] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleRefresh() {
  loadCategoryTree()
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 行级操作 ═══
/** 解锁：对标口径（停用/锁定后恢复），本系统即 status 置 1（复用现有状态接口） */
async function handleUnlock(record: ShopUser) {
  try {
    await shopUserApi.toggleStatus(record.id, 1)
    message.success(`买家「${record.nickname || record.username}」已解锁`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '解锁失败')
  }
}

/** 禁用：保留原有启停逻辑（status 置 0） */
async function handleDisable(record: ShopUser) {
  try {
    await shopUserApi.toggleStatus(record.id, 0)
    message.success(`买家「${record.nickname || record.username}」已禁用`)
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '禁用失败')
  }
}

/** 列表展示名（与「客户名称」列同口径） */
function displayName(record: ShopUser): string {
  return record.companyName || record.nickname || record.username || String(record.id ?? '')
}

/** 删除：真实调用 DELETE /erp/mall/admin/user/{id}（后端逻辑删除 deleted=1） */
async function handleDelete(record: ShopUser) {
  if (record.id == null) return
  try {
    await shopUserApi.remove(record.id)
    message.success(`买家「${displayName(record)}」已删除`)
    // 删除当前页最后一条时回退一页，避免停留在空页
    if (currentRows().length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

// ═══ 编辑弹窗（PUT /erp/mall/admin/user/{id}；字段与后端 ShopUserUpdateRequest 对齐） ═══
interface NamedOption { label: string; value: number }
interface LabelOption { label: string; value: string }

const editOpen = ref(false)
const editSaving = ref(false)
const editRecord = ref<ShopUser | null>(null)

const editForm = reactive({
  nickname: '',
  contactName: '',
  phone: '',
  companyName: '',
  email: '',
  qq: '',
  wechat: '',
  address: '',
  remark: '',
  customerLevel: undefined as string | undefined,
  categoryId: undefined as number | undefined,
  defaultHandlerId: null as number | null,
  warehouseId: null as number | null,
  deptId: null as number | null,
})

/** 下拉主数据（默认经手人/所属仓库/所属部门；接口失败只降级为空选项，不阻断其余字段编辑） */
const handlerOptions = ref<NamedOption[]>([])
const warehouseOptions = ref<NamedOption[]>([])
const deptOptions = ref<NamedOption[]>([])
/** 打开弹窗时按当前记录补入当前值，避免主数据缺该值导致回显为空、保存丢名称 */
const editHandlerOptions = ref<NamedOption[]>([])
const editWarehouseOptions = ref<NamedOption[]>([])
const editDeptOptions = ref<NamedOption[]>([])

/** 客户级别下拉：shop_user.customer_level 存级别**名称**，故 value 用名称（与 gradeId 过滤口径一致） */
const gradeLevelOptions = computed<LabelOption[]>(() => {
  const out: LabelOption[] = []
  const seen = new Set<string>()
  const push = (name?: string) => {
    const v = (name || '').trim()
    if (!v || seen.has(v)) return
    seen.add(v)
    out.push({ label: v, value: v })
  }
  gradeOptions.value.forEach(g => push(g.label))
  // 记录上的当前级别若不在主数据中，追加一项保证可回显（否则保存会被静默清空）
  push(editForm.customerLevel)
  return out
})

/** 归属分类下拉（分类树扁平化，不含根节点「全部客户」；后端 categoryId 仅支持改选、不支持清空） */
const categoryOptions = computed<NamedOption[]>(() => {
  const out: NamedOption[] = []
  const walk = (nodes: any[], prefix: string) => {
    for (const n of nodes || []) {
      const id = Number(n?.id)
      if (Number.isFinite(id) && String(n?.id) !== ROOT_CATEGORY_ID) {
        const label = prefix ? `${prefix} / ${n.categoryName}` : String(n?.categoryName || id)
        out.push({ label, value: id })
        if (n?.children?.length) walk(n.children, label)
      } else if (n?.children?.length) {
        walk(n.children, prefix)
      }
    }
  }
  walk(categoryList.value, '')
  return out
})

function pickList(raw: any): any[] {
  if (Array.isArray(raw)) return raw
  if (Array.isArray(raw?.data)) return raw.data
  if (Array.isArray(raw?.records)) return raw.records
  return []
}

function toNamedOptions(raw: any, nameKeys: string[]): NamedOption[] {
  return pickList(raw)
    .map((it: any) => {
      const id = Number(it?.id ?? it?.userId ?? it?.warehouseId ?? it?.deptId)
      const name = nameKeys.map(k => it?.[k]).find((v: any) => typeof v === 'string' && v.trim())
      return { label: name ? String(name) : `#${id}`, value: id }
    })
    .filter(o => Number.isFinite(o.value) && o.value > 0)
}

/** 加载编辑弹窗下拉主数据
 *  · 默认经手人：GET /api/user/list（sys_user）
 *  · 所属仓库：  GET /erp/warehouse/list（erp_warehouse —— shop_user.warehouse_id 的口径来源）
 *  · 所属部门：  GET /department/list（sys_dept）
 *  任一接口失败/无权限（403）只降级为空选项，不阻断其余字段编辑。
 */
async function loadEditOptions() {
  const [users, warehouses, depts] = await Promise.all([
    optionsApi.getUsers().catch(() => []),
    warehousePlanApi.listAll().catch(() => []),
    optionsApi.getDepartments().catch(() => []),
  ])
  handlerOptions.value = toNamedOptions(users, ['realName', 'nickname', 'name', 'username', 'userName'])
  warehouseOptions.value = toNamedOptions(warehouses, ['warehouseName', 'name'])
  deptOptions.value = toNamedOptions(depts, ['departmentName', 'deptName', 'name'])
}

/** 下拉补齐当前值：主数据缺失该 id 时按记录上的名称补一项 */
function withCurrent(options: NamedOption[], id?: number | null, name?: string | null): NamedOption[] {
  const list = [...options]
  const numId = id != null ? Number(id) : NaN
  if (Number.isFinite(numId) && numId > 0 && !list.some(o => o.value === numId)) {
    list.unshift({ label: name || `#${numId}`, value: numId })
  }
  return list
}

/** 由下拉选项反查名称（id 为空返回 ''，后端据此同时清空 id → 取消归属） */
function labelOf(options: NamedOption[], id: number | null | undefined): string {
  if (id == null) return ''
  return options.find(o => o.value === Number(id))?.label || ''
}

function handleEdit(record: ShopUser) {
  editRecord.value = record
  Object.assign(editForm, {
    nickname: record.nickname ?? '',
    contactName: record.contactName ?? '',
    phone: record.phone ?? '',
    companyName: record.companyName ?? '',
    email: record.email ?? '',
    qq: record.qq ?? '',
    wechat: record.wechat ?? '',
    address: record.address ?? '',
    remark: record.remark ?? '',
    customerLevel: record.customerLevel || undefined,
    categoryId: record.categoryId != null ? Number(record.categoryId) : undefined,
    defaultHandlerId: record.defaultHandlerId != null ? Number(record.defaultHandlerId) : null,
    warehouseId: record.warehouseId != null ? Number(record.warehouseId) : null,
    deptId: record.deptId != null ? Number(record.deptId) : null,
  })
  editHandlerOptions.value = withCurrent(handlerOptions.value, editForm.defaultHandlerId, record.defaultHandlerName)
  editWarehouseOptions.value = withCurrent(warehouseOptions.value, editForm.warehouseId, record.warehouseName)
  editDeptOptions.value = withCurrent(deptOptions.value, editForm.deptId, record.deptName)
  editOpen.value = true
}

async function handleEditSubmit() {
  const record = editRecord.value
  if (!record || record.id == null) return
  editSaving.value = true
  try {
    const payload: ShopUserUpdatePayload = {
      nickname: editForm.nickname,
      contactName: editForm.contactName,
      phone: editForm.phone,
      companyName: editForm.companyName,
      email: editForm.email,
      qq: editForm.qq,
      wechat: editForm.wechat,
      address: editForm.address,
      remark: editForm.remark,
      customerLevel: editForm.customerLevel ?? '',
      // 归属三件套成对提交：id=null 且 name='' → 后端同时清空 id（取消归属）
      defaultHandlerId: editForm.defaultHandlerId,
      defaultHandlerName: labelOf(editHandlerOptions.value, editForm.defaultHandlerId),
      warehouseId: editForm.warehouseId,
      warehouseName: labelOf(editWarehouseOptions.value, editForm.warehouseId),
      deptId: editForm.deptId,
      deptName: labelOf(editDeptOptions.value, editForm.deptId),
    }
    // 归属分类为数值型：仅支持改选（null = 不修改，故仅在选中时提交）
    if (editForm.categoryId != null) {
      payload.categoryId = Number(editForm.categoryId)
    }
    await shopUserApi.update(record.id, payload)
    message.success('买家账号已更新')
    editOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

// 注册验证码查询：系统**无验证码存储数据源**，故维持「待补」提示（不造表、不返回假数据）：
//   1) shop_user 无验证码/验证码时间列（V6.4.0 建表及 V9.21.0 / V11.361.3 迁移均未加）
//   2) 全库无 sms_record / verify_code 类表；仅 sys_notification_record（通知记录）为实体表，
//      而通知服务的验证码/短信记录只存内存 Map，且登录取的是图形验证码（AuthController，ConcurrentHashMap）
//   3) 商城注册 MallAuthServiceImpl.register 只查用户名重复，既不发送也不校验验证码
function handleVerifyCodeQuery() {
  message.warning('注册验证码查询暂不可用：系统无验证码存储数据源（无 sms_record/verify_code 表、shop_user 无验证码列、注册流程未发送验证码），待补齐短信验证码存储后再接入')
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
const printColumns: any[] = [
  { title: '客户名称', key: 'companyName', formatter: (v: any, record: any) => v || record.nickname || '' },
  { title: '注册时间', key: 'createTime', formatter: (v: any) => fmtTime(v) },
  { title: '最近交易时间', key: 'lastTradeTime', formatter: (v: any) => fmtTime(v) },
  { title: '最后登录时间', key: 'lastLoginTime', formatter: (v: any) => fmtTime(v) },
  { title: '登录名', key: 'username' },
  { title: '状态', key: 'status', formatter: (v: any) => (v === 1 ? '正常' : '已禁用') },
]

function currentRows(): ShopUser[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'mall-buyer-account',
  title: '买家账号',
  rows: () => currentRows(),
  columns: () => printColumns,
  // 原打印抬头的元信息行（当前路径 / 筛选条件 / 记录数；打印时间由模板 pageHeader 负责）
  totalText: () => `当前路径：${currentPath.value}，筛选条件：${searchForm.keyword || '全部'}，记录数：${currentRows().length}`,
  emptyTip: '没有可打印的数据',
})

// ═══ 导出（前端 CSV，\uFEFF BOM 保证 Excel 中文不乱码） ═══
function handleExport() {
  const rows = currentRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const header = ['客户名称', '注册时间', '最近交易时间', '最后登录时间', '登录名', '默认经手人', '客户级别', '所属仓库', '所属部门', '状态']
  const lines = rows.map((r: any) => [
    r.companyName || r.nickname || '',
    fmtTime(r.createTime),
    fmtTime(r.lastTradeTime),
    fmtTime(r.lastLoginTime),
    r.username ?? '',
    r.defaultHandlerName || r.defaultHandler || '',
    r.customerLevelName || r.customerLevel || '',
    r.warehouseName ?? '',
    r.deptName ?? '',
    r.status === 1 ? '正常' : '已禁用',
  ])
  const csv = [header, ...lines]
    .map(cols => cols.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `买家账号_${dayjs().format('YYYYMMDD')}.csv`
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
  console.error('[买家账号] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadCategoryTree()
  loadGrades()
  loadEditOptions()
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

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
