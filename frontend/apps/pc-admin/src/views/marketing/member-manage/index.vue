<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        会员管理（营销 → 会员中心 → 会员管理，菜单 80300）
        对标 ql361「营销 → 会员中心 → 会员管理」：左「客户分类」树 + 查询区 7 项 + 数据表 16 列（默认 10）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/会员管理开发文档.md
        口径：会员 = 客户（ERP 往来单位 biz_party）的扩展档案；列全部取自 biz_party.member_* 扩展列
        工具栏：新增 ｜ 发短信 / 发优惠券 ｜ 会员设置 / 刷新 / 打印(F8) / 更多
        行级：修改 / 积分明细 / 更多
        金标准：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable + PageConfigPanel
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="客户分类"
        :show-table-footer="true"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        @category-select="handleCategorySelect"
        @category-retry="loadCategoryTree"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增
          </a-button>
          <a-button
            v-if="isButtonEnabled('sendSms')"
            size="small"
            :disabled="!selectedRowKeys.length"
            @click="openSmsModal"
          >
            发短信
          </a-button>
          <a-button
            v-if="isButtonEnabled('sendCoupon')"
            size="small"
            :disabled="!selectedRowKeys.length"
            @click="openCouponModal"
          >
            发优惠券
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('memberSetting')"
              size="small"
              @click="goMemberSetting"
            >
              会员设置
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
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
            <a-dropdown v-if="isButtonEnabled('more')">
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleMoreAction">
                  <a-menu-item key="batchEnable">
                    批量启用会员卡
                  </a-menu-item>
                  <a-menu-item key="batchStop">
                    批量停用会员卡
                  </a-menu-item>
                  <a-menu-item key="config">
                    页面配置
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标 7 项 + 查询） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryVisible('party.keyword')">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.partyKeyword"
                  placeholder="请输入客户编号/名称"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryVisible('member.name')">
                <span class="search-label">会员名称</span>
                <a-input
                  v-model:value="searchForm.memberKeyword"
                  placeholder="请输入会员名称"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryVisible('member.phone')">
                <span class="search-label">联系电话</span>
                <a-input
                  v-model:value="searchForm.phone"
                  placeholder="请输入联系电话"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryVisible('member.customer')">
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="customerName"
                  placeholder="请选择客户"
                  size="small"
                  style="width: 160px"
                  read-only
                  allow-clear
                  @click="customerPickerOpen = true"
                  @clear="clearCustomer"
                />
                <a-button
                  size="small"
                  title="选择客户"
                  @click="customerPickerOpen = true"
                >
                  <SearchOutlined />
                </a-button>
              </template>
              <template v-if="isQueryVisible('member.handler')">
                <span class="search-label">客户经手人</span>
                <a-select
                  v-model:value="searchForm.handler"
                  placeholder="请选择经手人"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="handlerOptions"
                  @change="handleSearch"
                />
              </template>
            </div>
            <div class="search-row second-row">
              <template v-if="isQueryVisible('member.lastTradeStart')">
                <span class="search-label">最近交易</span>
                <a-range-picker
                  v-model:value="lastTradeRange"
                  size="small"
                  style="width: 240px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleResetSearch"
              >
                重置
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
              row-key="id"
              storage-key="marketing-member-manage-table-columns"
              global-config-key="marketing-member-manage-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <!-- 会员名称 -->
              <template #memberNameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.memberName }}</a>
              </template>

              <!-- 会员卡状态 -->
              <template #memberCardStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="CARD_STATUS_MAP[record.memberCardStatus]?.color || 'default'"
                >
                  {{ record.memberCardStatusDesc || CARD_STATUS_MAP[record.memberCardStatus]?.text || '-' }}
                </a-tag>
              </template>

              <!-- 有效时间 -->
              <template #validRangeCell="{ record }">
                <span v-if="!record.__ghost">{{ formatValidRange(record) }}</span>
              </template>

              <!-- 最近交易时间 -->
              <template #lastTradeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDate(record.lastTradeTime) }}</span>
              </template>

              <!-- 发卡时间 -->
              <template #issueTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDate(record.memberIssueTime) }}</span>
              </template>

              <!-- 操作列（对标：行内 修改 / 积分明细 / 更多） -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openPointsDetail(record)"
                  >
                    积分明细
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e: any) => handleRowMore(e, record)">
                        <a-menu-item key="toggleCard">
                          {{ record.memberCardStatus === 'STOPPED' ? '启用会员卡' : '停用会员卡' }}
                        </a-menu-item>
                        <a-menu-item key="coupon">发优惠券</a-menu-item>
                        <a-menu-item key="sms">发短信</a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部分页 ═══ -->
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

      <!-- ═══ 页面配置（对标有「页面配置」弹窗） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="marketing-member-manage-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新增/修改会员档案 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改会员档案' : '新增会员'"
        :confirm-loading="saving"
        width="720px"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="客户"
                name="partyId"
              >
                <a-input
                  :value="form.partyName"
                  placeholder="请选择客户"
                  read-only
                  @click="formCustomerPickerOpen = true"
                >
                  <template #suffix>
                    <SearchOutlined class="cell-link" @click="formCustomerPickerOpen = true" />
                  </template>
                </a-input>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="会员名称"
                name="memberName"
              >
                <a-input
                  v-model:value="form.memberName"
                  placeholder="请输入会员名称"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="会员卡号"
                name="memberCardNo"
              >
                <a-input
                  v-model:value="form.memberCardNo"
                  placeholder="请输入会员卡号"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="会员级别">
                <a-input
                  v-model:value="form.memberLevel"
                  placeholder="请输入会员级别"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="会员卡状态">
                <a-select
                  v-model:value="form.memberCardStatus"
                  :options="CARD_STATUS_OPTIONS"
                  placeholder="请选择"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="有效期">
                <a-range-picker
                  v-model:value="validRange"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="会员生日">
                <a-date-picker
                  v-model:value="form.birthday"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="初始积分">
                <a-input-number
                  v-model:value="form.memberInitialPoints"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="当前积分">
                <a-input-number
                  v-model:value="form.points"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="默认经手人">
                <a-select
                  v-model:value="form.defaultHandlerId"
                  :options="handlerOptions"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  placeholder="请选择"
                  @change="handleHandlerChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="form.remark"
                  :rows="2"
                  placeholder="请输入备注"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <!-- ═══ 积分明细（行级入口） ═══ -->
      <a-modal
        v-model:open="pointsOpen"
        :title="`积分明细 · ${pointsMember?.memberName || ''}`"
        width="1000px"
        :footer="null"
      >
        <a-table
          :columns="POINTS_COLUMNS"
          :data-source="pointsRows"
          :loading="pointsLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ y: 360 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'createTime'">
              {{ fmtTime(record.createTime) }}
            </template>
          </template>
        </a-table>
        <div class="modal-footer-tip">
          共 {{ pointsRows.length }} 条流水（数据源：销售订单积分流水，与销售开单「会员信息」Tab 同源）
        </div>
      </a-modal>

      <!-- ═══ 发短信（对标：选择客户 → 短信内容/模板 → 签名 → 类型 → 协议 → 确认发送） ═══ -->
      <a-modal
        v-model:open="smsOpen"
        title="发短信"
        :confirm-loading="smsSending"
        width="640px"
        ok-text="确认发送"
        @ok="handleSendSms"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="选择客户">
            已选择 <b>{{ targetMembers.length }}</b> 人
          </a-form-item>
          <a-form-item label="短信内容">
            <a-textarea
              v-model:value="smsForm.content"
              :rows="4"
              :maxlength="320"
              placeholder="请填写短信内容~"
            />
            <div class="modal-footer-tip">
              提示：每超出 67 字将按照新一条短信发送；当前 {{ smsForm.content.length }}/67
            </div>
          </a-form-item>
          <a-form-item label="选择短信模板">
            <a-select
              v-model:value="smsForm.templateId"
              placeholder="选择短信模板"
              allow-clear
              :options="smsTemplateOptions"
              @change="handleSmsTemplateChange"
            />
          </a-form-item>
          <a-form-item label="公司签名">
            <a-input v-model:value="smsForm.signName" />
          </a-form-item>
          <a-form-item label="短信类型">
            <a-radio-group v-model:value="smsForm.smsType">
              <a-radio value="NOTICE">通知短信</a-radio>
              <a-radio value="MARKETING">营销短信</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="短信协议">
            <a-checkbox v-model:checked="smsForm.agreed">
              是否同意短信协议
            </a-checkbox>
          </a-form-item>
          <a-form-item label="手机预览">
            <div class="sms-preview">
              {{ smsPreview }}
            </div>
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 发优惠券 ═══ -->
      <a-modal
        v-model:open="couponOpen"
        title="发优惠券"
        :confirm-loading="couponIssuing"
        width="560px"
        ok-text="确认发放"
        @ok="handleIssueCoupon"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="已选客户">
            已选择 <b>{{ targetMembers.length }}</b> 人
          </a-form-item>
          <a-form-item label="优惠券">
            <a-select
              v-model:value="couponForm.templateId"
              placeholder="请选择优惠券"
              :options="couponOptions"
              @change="handleCouponChange"
            />
          </a-form-item>
          <a-form-item label="每人发放张数">
            <a-input-number
              v-model:value="couponForm.quantityPerPartner"
              :min="1"
              :max="100"
              style="width: 140px"
            />
          </a-form-item>
          <a-form-item label="可发放数量">
            {{ couponRemain }}
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 客户选择器（复用通用组件） -->
      <PartnerSelectModal
        v-model:open="customerPickerOpen"
        default-tab="customer"
        @select="handleCustomerPicked"
      />
      <PartnerSelectModal
        v-model:open="formCustomerPickerOpen"
        default-tab="customer"
        @select="handleFormCustomerPicked"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SearchOutlined,
  SettingOutlined, DownOutlined, DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { memberManageApi, couponTemplateApi, smsMarketingApi, type CouponTemplate, type SmsTemplate } from '@/api/marketing'
import { partnerApi, partnerCategoryApi } from '@/api/erp/partner'
import { optionsApi } from '@/api/options'

defineOptions({ name: 'MarketingMemberManage' })

const router = useRouter()

// ═══ 字典 ═══
const CARD_STATUS_MAP: Record<string, { text: string; color: string }> = {
  NORMAL: { text: '启用', color: 'green' },
  STOPPED: { text: '停用', color: 'default' },
  EXPIRED: { text: '已过期', color: 'orange' },
}
const CARD_STATUS_OPTIONS = Object.entries(CARD_STATUS_MAP).map(([value, v]) => ({ value, label: v.text }))

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const selectedRowKeys = ref<any[]>([])

// ═══ 查询条件（对标 7 项） ═══
const searchForm = reactive({
  partyKeyword: '',
  memberKeyword: '',
  phone: '',
  customerId: undefined as any,
  handler: undefined as string | undefined,
})
const customerName = ref('')
const lastTradeRange = ref<any>(undefined)
const customerPickerOpen = ref(false)

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 分类树（客户分类） ═══
const categoryTreeData = ref<any[]>([])
const categoryLoading = ref(false)
const categoryError = ref(false)
const selectedCategoryId = ref<any>('0')
const currentPath = ref('全部客户')

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const res: any = await partnerCategoryApi.getTree('CUSTOMER')
    const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
    categoryTreeData.value = [{
      id: '0',
      categoryName: '全部客户',
      children: list,
    }]
  } catch (error: any) {
    console.error('[会员管理] 加载客户分类失败', error)
    categoryError.value = true
    categoryTreeData.value = [{ id: '0', categoryName: '全部客户', children: [] }]
  } finally {
    categoryLoading.value = false
  }
}

function findCategoryName(nodes: any[], id: any, parents: string[] = []): string | null {
  for (const n of nodes) {
    const path = [...parents, n.categoryName]
    if (String(n.id) === String(id)) return path.join('/')
    if (n.children?.length) {
      const hit = findCategoryName(n.children, id, path)
      if (hit) return hit
    }
  }
  return null
}

function handleCategorySelect(keys: any[]) {
  const key = keys?.[0] ?? '0'
  selectedCategoryId.value = key
  currentPath.value = findCategoryName(categoryTreeData.value, key) || '全部客户'
  handleSearch()
}

// ═══ 列定义（对标 16 列，默认 10） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'memberName', title: '会员名称', type: 'slot', slotName: 'memberNameCell', width: 180 },
  { key: 'memberCardNo', title: '会员卡号', type: 'input', width: 160 },
  { key: 'partyName', title: '客户名称', type: 'input', width: 220 },
  { key: 'partyCode', title: '客户编号', type: 'input', width: 130, defaultHidden: true },
  { key: 'phone', title: '联系电话', type: 'input', width: 130, defaultHidden: true },
  { key: 'memberLevel', title: '会员级别', type: 'input', width: 120 },
  { key: 'memberCardStatus', title: '会员卡状态', type: 'slot', slotName: 'memberCardStatusCell', width: 110 },
  { key: 'validRange', title: '有效时间', type: 'slot', slotName: 'validRangeCell', width: 200 },
  { key: 'birthday', title: '会员生日', type: 'input', width: 120 },
  { key: 'points', title: '当前积分', type: 'input', width: 100 },
  { key: 'memberTotalConsume', title: '累计消费额', type: 'input', width: 120, defaultHidden: true },
  { key: 'memberIssueTime', title: '发卡时间', type: 'slot', slotName: 'issueTimeCell', width: 130, defaultHidden: true },
  { key: 'lastTradeTime', title: '最近交易时间', type: 'slot', slotName: 'lastTradeCell', width: 140 },
  { key: 'memberInitialPoints', title: '初始积分', type: 'input', width: 100, defaultHidden: true },
  { key: 'defaultHandlerName', title: '默认经手人', type: 'input', width: 120, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160 },
]

// ═══ 页面配置（查询条件 7 + 功能按钮 6） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'party.keyword', label: '筛选条件（客户编号/名称）', visible: true },
  { key: 'member.name', label: '会员名称', visible: true },
  { key: 'member.phone', label: '联系电话', visible: true },
  { key: 'member.customer', label: '客户', visible: true },
  { key: 'member.handler', label: '客户经手人', visible: true },
  { key: 'member.lastTradeStart', label: '最近交易（起）', visible: true },
  { key: 'member.lastTradeEnd', label: '最近交易（止）', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'sendSms', label: '发短信', enabled: true },
  { key: 'sendCoupon', label: '发优惠券', enabled: true },
  { key: 'memberSetting', label: '会员设置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'more', label: '更多', enabled: true },
]

const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

function handlePageConfigChange(cfg: any) {
  if (cfg?.queryFields) queryFieldsConfig.value = cfg.queryFields
  if (cfg?.functionButtons) functionButtonConfig.value = cfg.functionButtons
}
function isQueryVisible(key: string): boolean {
  const f = queryFieldsConfig.value.find(x => x.key === key)
  return f ? f.visible : true
}
function isButtonEnabled(key: string): boolean {
  const b = functionButtonConfig.value.find(x => x.key === key)
  return b ? b.enabled : true
}

// ═══ 经手人下拉 ═══
const handlerOptions = ref<any[]>([])
async function loadHandlerOptions() {
  try {
    const rows: any = await optionsApi.getUsers()
    handlerOptions.value = (Array.isArray(rows) ? rows : []).map((u: any) => ({
      value: u.name || u.realName || u.nickname || u.username,
      label: u.name || u.realName || u.nickname || u.username,
      id: u.id,
    })).filter((o: any) => o.value)
  } catch (e) {
    console.error('[会员管理] 加载经手人失败', e)
    handlerOptions.value = []
  }
}
function handleHandlerChange(v: any) {
  const hit = handlerOptions.value.find(o => o.value === v)
  form.defaultHandlerId = hit?.id
  form.defaultHandlerName = hit?.value
}

// ═══ 工具 ═══
function fmtDate(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD') : '-'
}
function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function formatValidRange(record: any): string {
  const s = record.memberValidStart ? dayjs(record.memberValidStart).format('YYYY-MM-DD') : ''
  const e = record.memberValidEnd ? dayjs(record.memberValidEnd).format('YYYY-MM-DD') : ''
  if (!s && !e) return '-'
  return `${s} ~ ${e}`
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await memberManageApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partyKeyword: searchForm.partyKeyword || undefined,
      memberKeyword: searchForm.memberKeyword || undefined,
      phone: searchForm.phone || undefined,
      customerId: searchForm.customerId || undefined,
      handler: searchForm.handler || undefined,
      lastTradeStart: lastTradeRange.value?.[0] || undefined,
      lastTradeEnd: lastTradeRange.value?.[1] || undefined,
      categoryId: selectedCategoryId.value && String(selectedCategoryId.value) !== '0'
        ? selectedCategoryId.value : undefined,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[会员管理] 加载列表失败', error)
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
function handleResetSearch() {
  searchForm.partyKeyword = ''
  searchForm.memberKeyword = ''
  searchForm.phone = ''
  searchForm.customerId = undefined
  searchForm.handler = undefined
  customerName.value = ''
  lastTradeRange.value = undefined
  handleSearch()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}
/** 勾选维护：以**记录 id**（而非 rowIndex）为准，避免换查询/翻页后勾选漂移 */
function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  const keys = selectedRowKeys.value.filter(k => String(k) !== String(record.id))
  if (checked) keys.push(record.id)
  selectedRowKeys.value = keys
}
function handleCheckboxAll(checked: boolean, records: any[]) {
  selectedRowKeys.value = checked ? (records || []).map(r => r.id) : []
}
function clearCustomer() {
  searchForm.customerId = undefined
  customerName.value = ''
}
function handleCustomerPicked(record: any) {
  searchForm.customerId = record?.id
  customerName.value = record?.partyName || record?.partnerName || ''
  customerPickerOpen.value = false
  handleSearch()
}
function goMemberSetting() {
  router.push('/marketing/member-config')
}

/** 已选会员行（批量触达用） */
const targetMembers = computed(() => {
  const keys = selectedRowKeys.value.map(String)
  return tableData.value.filter((r: any) => keys.includes(String(r.id)) && !r.__ghost)
})

// ═══ 新增 / 修改 ═══
const formRef = ref()
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const formCustomerPickerOpen = ref(false)
const validRange = ref<any>(undefined)

const emptyForm = () => ({
  partyId: undefined as any,
  partyName: '',
  memberName: '',
  memberCardNo: '',
  memberLevel: '',
  memberCardStatus: 'NORMAL',
  birthday: undefined as any,
  memberInitialPoints: 0 as number | undefined,
  points: 0 as number | undefined,
  defaultHandlerId: undefined as any,
  defaultHandlerName: '',
  remark: '',
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  partyId: [{ required: true, message: '请选择客户' }],
  memberName: [{ required: true, message: '请输入会员名称' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  validRange.value = undefined
  formOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    partyId: record.id,
    partyName: record.partyName || '',
    memberName: record.memberName || '',
    memberCardNo: record.memberCardNo || '',
    memberLevel: record.memberLevel || '',
    memberCardStatus: record.memberCardStatus || 'NORMAL',
    birthday: record.birthday ? dayjs(record.birthday).format('YYYY-MM-DD') : undefined,
    memberInitialPoints: record.memberInitialPoints ?? 0,
    points: record.points ?? 0,
    defaultHandlerId: record.defaultHandlerId,
    defaultHandlerName: record.defaultHandlerName || '',
    remark: record.remark || '',
  })
  validRange.value = record.memberValidStart && record.memberValidEnd
    ? [dayjs(record.memberValidStart).format('YYYY-MM-DD'), dayjs(record.memberValidEnd).format('YYYY-MM-DD')]
    : undefined
  formOpen.value = true
}

function handleFormCustomerPicked(record: any) {
  form.partyId = record?.id
  form.partyName = record?.partyName || record?.partnerName || ''
  if (!form.memberName && form.partyName) form.memberName = form.partyName
  formCustomerPickerOpen.value = false
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: any = {
      partnerType: 'customer',
      memberName: form.memberName,
      memberCardNo: form.memberCardNo,
      memberLevel: form.memberLevel,
      memberCardStatus: form.memberCardStatus,
      memberValidStart: validRange.value?.[0],
      memberValidEnd: validRange.value?.[1],
      birthday: form.birthday,
      memberInitialPoints: form.memberInitialPoints,
      points: form.points,
      defaultHandlerId: form.defaultHandlerId,
      defaultHandlerName: form.defaultHandlerName,
      remark: form.remark,
    }
    if (editingId.value) {
      await partnerApi.update(editingId.value, payload)
      message.success('会员档案已更新')
    } else {
      payload.partnerCode = undefined
      const created: any = await partnerApi.create(payload)
      if (!created?.id && !created) {
        message.error('新增失败：请检查客户编号是否重复')
      } else {
        message.success('会员已新增')
      }
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 积分明细 ═══
const pointsOpen = ref(false)
const pointsLoading = ref(false)
const pointsRows = ref<any[]>([])
const pointsMember = ref<any>(null)

const POINTS_COLUMNS = [
  { title: '单据编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '发生时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '此前积分', dataIndex: 'prevPoints', key: 'prevPoints', width: 100 },
  { title: '销售积分', dataIndex: 'salePoints', key: 'salePoints', width: 100 },
  { title: '退货积分', dataIndex: 'returnPoints', key: 'returnPoints', width: 100 },
  { title: '兑换积分', dataIndex: 'exchangePoints', key: 'exchangePoints', width: 100 },
  { title: '使用积分', dataIndex: 'usedPoints', key: 'usedPoints', width: 100 },
  { title: '当前积分', dataIndex: 'currentPoints', key: 'currentPoints', width: 100 },
]

async function openPointsDetail(record: any) {
  pointsMember.value = record
  pointsOpen.value = true
  pointsLoading.value = true
  try {
    const res: any = await memberManageApi.pointsPage({
      memberCardNo: record.memberCardNo || undefined,
      memberName: record.memberCardNo ? undefined : record.memberName || undefined,
      pageNum: 1,
      pageSize: 200,
    })
    pointsRows.value = res?.records || []
  } catch (error: any) {
    console.error('[会员管理] 加载积分明细失败', error)
    message.error(error?.response?.data?.message || '加载积分明细失败')
    pointsRows.value = []
  } finally {
    pointsLoading.value = false
  }
}

// ═══ 行级更多 / 工具栏更多 ═══
async function handleRowMore(e: any, record: any) {
  if (e.key === 'toggleCard') {
    const next = record.memberCardStatus === 'STOPPED' ? 'NORMAL' : 'STOPPED'
    try {
      await partnerApi.update(record.id, { memberCardStatus: next })
      message.success(next === 'NORMAL' ? '会员卡已启用' : '会员卡已停用')
      fetchList()
    } catch (error: any) {
      message.error(error?.response?.data?.message || '操作失败')
    }
  } else if (e.key === 'coupon') {
    selectedRowKeys.value = [record.id]
    openCouponModal()
  } else if (e.key === 'sms') {
    selectedRowKeys.value = [record.id]
    openSmsModal()
  }
}

async function handleMoreAction(e: any) {
  if (e.key === 'config') {
    showPageConfig.value = true
    return
  }
  const next = e.key === 'batchEnable' ? 'NORMAL' : 'STOPPED'
  if (!targetMembers.value.length) {
    message.warning('请先勾选会员')
    return
  }
  try {
    for (const m of targetMembers.value) {
      await partnerApi.update(m.id, { memberCardStatus: next })
    }
    message.success(next === 'NORMAL' ? '已批量启用' : '已批量停用')
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量操作失败')
  }
}

// ═══ 发短信 ═══
const smsOpen = ref(false)
const smsSending = ref(false)
const smsTemplates = ref<SmsTemplate[]>([])
const smsForm = reactive({
  content: '',
  templateId: undefined as any,
  signName: '',
  smsType: 'NOTICE',
  agreed: false,
})
const smsTemplateOptions = computed(() => smsTemplates.value.map(t => ({ value: t.id, label: t.templateTitle })))
const smsPreview = computed(() => {
  const sign = smsForm.signName ? `【${smsForm.signName}】` : ''
  const suffix = smsForm.smsType === 'MARKETING' ? '拒收请回复R' : ''
  return `${sign}${smsForm.content}${suffix}` || '（短信预览）'
})

async function openSmsModal() {
  if (!targetMembers.value.length) {
    message.warning('请先勾选会员')
    return
  }
  smsOpen.value = true
  try {
    const [setting, tpls] = await Promise.all([
      smsMarketingApi.getSetting(),
      smsMarketingApi.templateList(),
    ])
    smsForm.signName = setting?.signName || ''
    smsTemplates.value = Array.isArray(tpls) ? tpls : []
  } catch (e) {
    console.error('[会员管理] 加载短信设置失败', e)
  }
}
function handleSmsTemplateChange(id: any) {
  const tpl = smsTemplates.value.find(t => t.id === id)
  if (tpl) {
    smsForm.content = tpl.templateContent || ''
    smsForm.smsType = tpl.smsType || 'NOTICE'
  }
}
async function handleSendSms() {
  if (!smsForm.content.trim()) {
    message.warning('请填写短信内容')
    return
  }
  if (!smsForm.agreed) {
    message.warning('请先勾选同意短信协议')
    return
  }
  smsSending.value = true
  try {
    const res = await smsMarketingApi.send({
      partnerIds: targetMembers.value.map((m: any) => m.id),
      content: smsForm.content,
      signName: smsForm.signName,
      smsType: smsForm.smsType,
      agreed: smsForm.agreed,
    })
    message.success(`已提交发送 ${res?.sentCount ?? 0} 条（批次 ${res?.batchNo || ''}）`)
    smsOpen.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发送失败')
  } finally {
    smsSending.value = false
  }
}

// ═══ 发优惠券 ═══
const couponOpen = ref(false)
const couponIssuing = ref(false)
const couponTemplates = ref<CouponTemplate[]>([])
const couponForm = reactive({ templateId: undefined as any, quantityPerPartner: 1 })
const couponOptions = computed(() => couponTemplates.value.map(c => ({
  value: c.id,
  label: `${c.couponName}（未领取 ${c.remainingCount ?? 0}）`,
})))
const couponRemain = computed(() => {
  const hit = couponTemplates.value.find(c => c.id === couponForm.templateId)
  return hit ? (hit.remainingCount ?? 0) : '-'
})

async function openCouponModal() {
  if (!targetMembers.value.length) {
    message.warning('请先勾选会员')
    return
  }
  couponOpen.value = true
  try {
    const res: any = await couponTemplateApi.page({ pageNum: 1, pageSize: 200, status: 'NORMAL' })
    couponTemplates.value = res?.records || []
  } catch (e) {
    console.error('[会员管理] 加载优惠券失败', e)
  }
}
function handleCouponChange() {
  couponForm.quantityPerPartner = 1
}
async function handleIssueCoupon() {
  if (!couponForm.templateId) {
    message.warning('请选择优惠券')
    return
  }
  couponIssuing.value = true
  try {
    const n = await couponTemplateApi.issue(couponForm.templateId, {
      partnerIds: targetMembers.value.map((m: any) => m.id),
      quantityPerPartner: couponForm.quantityPerPartner,
      sourceBillNo: '会员管理-后台发放',
    })
    message.success(`已发放 ${n} 张`)
    couponOpen.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发放失败')
  } finally {
    couponIssuing.value = false
  }
}

// ═══ 打印(F8) / 导出 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

const PRINT_COLUMNS = [
  { key: 'memberName', title: '会员名称' },
  { key: 'memberCardNo', title: '会员卡号' },
  { key: 'partyName', title: '客户名称' },
  { key: 'partyCode', title: '客户编号' },
  { key: 'phone', title: '联系电话' },
  { key: 'memberLevel', title: '会员级别' },
  { key: 'memberCardStatus', title: '会员卡状态' },
  { key: 'validRange', title: '有效时间' },
  { key: 'birthday', title: '会员生日' },
  { key: 'points', title: '当前积分' },
  { key: 'memberTotalConsume', title: '累计消费额' },
  { key: 'lastTradeTime', title: '最近交易时间' },
  { key: 'remark', title: '备注' },
]

function printCell(row: any, key: string): string {
  if (key === 'validRange') return formatValidRange(row)
  if (key === 'memberCardStatus') return CARD_STATUS_MAP[row.memberCardStatus]?.text || ''
  if (key === 'lastTradeTime') return fmtDate(row.lastTradeTime)
  return row[key] == null ? '' : String(row[key])
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    PRINT_COLUMNS.map(c => `<td>${escapeHtml(printCell(r, c.key))}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>会员名册</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>会员名册</h2>
    <div class="meta">
      <span>客户分类：${escapeHtml(currentPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table><thead><tr><th>#</th>${PRINT_COLUMNS.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
    <tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
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
}

function handleExport() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exporting.value = true
  try {
    const header = PRINT_COLUMNS.map(c => c.title)
    const body = rows.map((r: any) => PRINT_COLUMNS.map(c => printCell(r, c.key)))
    const csv = '\uFEFF' + [header, ...body]
      .map(line => line.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `会员名册_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[会员管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadCategoryTree()
  loadHandlerOptions()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-row.second-row { margin-top: 8px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
.modal-footer-tip { margin-top: 8px; font-size: 12px; color: #999; }
.sms-preview {
  min-height: 48px;
  padding: 8px 10px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fafafa;
  font-size: 13px;
  color: #333;
  word-break: break-all;
}
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
