<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        优惠券（营销 → 营销活动 → 优惠券，菜单 80311）
        对标 ql361「营销 → 营销活动 → 优惠券」：**双视图 Tab 复合页**（逐 Tab 独立列配置）
          · Tab1「优惠券设置」15 列（全可见）—— 制券视图；行级 发优惠券 / 作废 / 修改
          · Tab2「领用明细」13 列（全可见）—— 逐客户逐券的领取与核销流水
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/优惠券开发文档.md
        查询区：优惠券*（券名）/ 领用状态 / 单据编号；工具栏：发短信 / 刷新 / 打印(F8) / 导出
        数量守恒口径：总数 = 已领取（未使用） + 已使用 + 未领取
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'template'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增优惠券
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :disabled="!selectedRowKeys.length"
              @click="openSendSms"
            >
              发短信
            </a-button>
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
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
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标：优惠券* / 领用状态 / 单据编号 / 仅显示已勾选记录） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <span class="search-label">优惠券</span>
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="优惠券*"
                size="small"
                style="width: 180px"
                allow-clear
                @press-enter="handleSearch"
              />
              <template v-if="activeTab === 'record'">
                <span class="search-label">领用状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="RECEIVE_STATUS_OPTIONS"
                  @change="handleSearch"
                />
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="searchForm.billNo"
                  placeholder="请输入单据编号"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <a-checkbox v-model:checked="searchForm.onlyChecked">
                  仅显示已勾选记录
                </a-checkbox>
              </template>
              <template v-else>
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.templateStatus"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="TEMPLATE_STATUS_OPTIONS"
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
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <!-- ═══ Tab1 优惠券设置 ═══ -->
            <BillDetailTable
              v-if="activeTab === 'template'"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-coupon-template-table-columns"
              global-config-key="marketing-coupon-template-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.couponName }}</a>
              </template>

              <template #openReceiveCell="{ record }">
                <span v-if="!record.__ghost">{{ record.openReceive === 1 ? '是' : '否' }}</span>
              </template>

              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ COUPON_TYPE_MAP[record.couponType] || record.couponType || '-' }}</span>
              </template>

              <template #useRuleCell="{ record }">
                <span v-if="!record.__ghost">{{ useRuleText(record.useRule) }}</span>
              </template>

              <template #faceValueCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtMoney(record.faceValue) }}</span>
              </template>

              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtDate(record[column.key]) }}</span>
              </template>

              <template #scopeCell="{ record }">
                <span v-if="!record.__ghost">{{ record.customerScope === 'SPECIFIED' ? '指定客户' : '全部客户' }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="COUPON_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ COUPON_STATUS_MAP[record.status]?.text || record.status || '-' }}
                </a-tag>
              </template>

              <template #yesNoCell="{ record, column }">
                <span v-if="!record.__ghost">{{ record[column.key] === 1 ? '允许' : '禁止' }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openIssue(record)"
                  >
                    发优惠券
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.status === 'VOID'"
                    @click="handleVoid(record)"
                  >
                    作废
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    修改
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab2 领用明细 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="recordData"
              :columns="recordColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-coupon-record-table-columns"
              global-config-key="marketing-coupon-record-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <template #nameCell="{ record }">
                <span v-if="!record.__ghost">{{ record.couponName }}</span>
              </template>
              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ COUPON_TYPE_MAP[record.couponType] || record.couponType || '-' }}</span>
              </template>
              <template #useRuleCell="{ record }">
                <span v-if="!record.__ghost">{{ useRuleText(record.useRule) }}</span>
              </template>
              <template #faceValueCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtMoney(record.faceValue) }}</span>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="COUPON_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ COUPON_STATUS_MAP[record.status]?.text || record.status || '-' }}
                </a-tag>
              </template>
              <template #receiveStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="RECEIVE_STATUS_COLOR[record.receiveStatus] || 'default'"
                >
                  {{ record.receiveStatus }}
                </a-tag>
              </template>

              <!-- 券生命周期闭环：核销（后台补录）/ 作废 -->
              <template #recordActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-if="record.receiveStatus === '已领取'"
                    type="link"
                    size="small"
                    @click="handleRedeem(record)"
                  >
                    核销
                  </a-button>
                  <a-button
                    v-if="record.receiveStatus === '已领取'"
                    type="link"
                    size="small"
                    danger
                    @click="handleVoidCoupon(record)"
                  >
                    作废
                  </a-button>
                  <span v-else>-</span>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <!-- ═══ 新增/修改优惠券 ═══ -->
      <a-modal
        v-model:open="formOpen"
        :title="editingId ? '修改优惠券' : '新增优惠券'"
        :confirm-loading="saving"
        width="720px"
        @ok="handleSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="优惠券名称"
                required
              >
                <a-input
                  v-model:value="form.couponName"
                  placeholder="请输入优惠券名称"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="开放领取">
                <a-switch
                  v-model:checked="form.openReceiveBool"
                  checked-children="是"
                  un-checked-children="否"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="类型">
                <a-select
                  v-model:value="form.couponType"
                  :options="COUPON_TYPE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="使用规则">
                <a-select
                  v-model:value="form.useRule"
                  :options="USE_RULE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="面值">
                <a-input-number
                  v-model:value="form.faceValue"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="总数">
                <a-input-number
                  v-model:value="form.totalCount"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="起始时间">
                <a-date-picker
                  v-model:value="form.startTime"
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="到期时间">
                <a-date-picker
                  v-model:value="form.endTime"
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="商城使用">
                <a-switch
                  v-model:checked="form.mallEnabledBool"
                  checked-children="允许"
                  un-checked-children="禁止"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="线下使用">
                <a-switch
                  v-model:checked="form.offlineEnabledBool"
                  checked-children="允许"
                  un-checked-children="禁止"
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

      <!-- ═══ 发优惠券 ═══ -->
      <a-modal
        v-model:open="issueOpen"
        :title="`发优惠券 · ${issueTemplate?.couponName || ''}`"
        :confirm-loading="issuing"
        width="560px"
        ok-text="确认发放"
        @ok="handleIssue"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="已选客户">
            已选择 <b>{{ selectedRowKeys.length || issueCustomers.length }}</b> 人
          </a-form-item>
          <a-form-item label="每人发放张数">
            <a-input-number
              v-model:value="issueQuantity"
              :min="1"
              :max="100"
              style="width: 140px"
            />
          </a-form-item>
          <a-form-item label="可发放数量">
            {{ issueTemplate?.remainingCount ?? '-' }}
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 发短信 ═══ -->
      <a-modal
        v-model:open="smsOpen"
        title="发短信"
        :confirm-loading="smsSending"
        width="620px"
        ok-text="确认发送"
        @ok="handleSendSms"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="已选">
            {{ selectedRowKeys.length }} 条记录（按客户去重后发送）
          </a-form-item>
          <a-form-item label="短信内容">
            <a-textarea
              v-model:value="smsContent"
              :rows="4"
              :maxlength="320"
              placeholder="请填写短信内容~"
            />
          </a-form-item>
          <a-form-item label="公司签名">
            <a-input v-model:value="smsSignName" />
          </a-form-item>
          <a-form-item label="短信类型">
            <a-radio-group v-model:value="smsType">
              <a-radio value="NOTICE">通知短信</a-radio>
              <a-radio value="MARKETING">营销短信</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="短信协议">
            <a-checkbox v-model:checked="smsAgreed">
              是否同意短信协议
            </a-checkbox>
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import {
  couponTemplateApi, smsMarketingApi, COUPON_TYPE_MAP, COUPON_STATUS_MAP,
  type CouponTemplate,
} from '@/api/marketing'

defineOptions({ name: 'MarketingCoupon' })

const TABS = [
  { key: 'template', label: '优惠券设置' },
  { key: 'record', label: '领用明细' },
]
const activeTab = ref('template')

const COUPON_TYPE_OPTIONS = [
  { value: 'CASH', label: '现金券' },
  { value: 'DISCOUNT', label: '折扣券' },
  { value: 'FULL_CUT', label: '满减券' },
]
const USE_RULE_OPTIONS = [
  { value: 'UNLIMITED', label: '无限制' },
  { value: 'FULL_100', label: '满 100 元可用' },
  { value: 'FULL_200', label: '满 200 元可用' },
]
const TEMPLATE_STATUS_OPTIONS = [
  { value: 'NORMAL', label: '正常' },
  { value: 'VOID', label: '已作废' },
]
const RECEIVE_STATUS_OPTIONS = [
  { value: 'UNUSED', label: '已领取' },
  { value: 'USED', label: '已使用' },
  { value: 'EXPIRED', label: '已过期' },
  { value: 'CANCELLED', label: '已作废' },
]
const RECEIVE_STATUS_COLOR: Record<string, string> = {
  已领取: 'blue', 已使用: 'green', 已过期: 'orange', 已作废: 'red',
}

const loading = ref(false)
const exporting = ref(false)
const tableData = ref<CouponTemplate[]>([])
const recordData = ref<any[]>([])
const selectedRowKeys = ref<any[]>([])
const searchForm = reactive({
  keyword: '',
  status: undefined as string | undefined,
  billNo: '',
  onlyChecked: false,
  templateStatus: undefined as string | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ── Tab1 列（对标 15 列，全可见） ──
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'couponName', title: '优惠券名称', type: 'slot', slotName: 'nameCell', width: 200 },
  { key: 'openReceive', title: '开放领取', type: 'slot', slotName: 'openReceiveCell', width: 100 },
  { key: 'couponType', title: '类型', type: 'slot', slotName: 'typeCell', width: 100 },
  { key: 'useRule', title: '使用规则', type: 'slot', slotName: 'useRuleCell', width: 140 },
  { key: 'faceValue', title: '面值', type: 'slot', slotName: 'faceValueCell', width: 110 },
  { key: 'totalCount', title: '总数', type: 'input', width: 90 },
  { key: 'receivedCount', title: '已领取（未使用）', type: 'input', width: 140 },
  { key: 'usedCount', title: '已使用', type: 'input', width: 90 },
  { key: 'remainingCount', title: '未领取', type: 'input', width: 90 },
  { key: 'customerScope', title: '指定客户', type: 'slot', slotName: 'scopeCell', width: 110 },
  { key: 'startTime', title: '起始时间', type: 'slot', slotName: 'timeCell', width: 120 },
  { key: 'endTime', title: '到期时间', type: 'slot', slotName: 'timeCell', width: 120 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'mallEnabled', title: '商城使用', type: 'slot', slotName: 'yesNoCell', width: 100 },
  { key: 'offlineEnabled', title: '线下使用', type: 'slot', slotName: 'yesNoCell', width: 100 },
]

// ── Tab2 列（对标 13 列，全可见） ──
const recordColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'recordActionCell', width: 150, fixed: 'left' },
  { key: 'partnerName', title: '客户', type: 'input', width: 200 },
  { key: 'contactName', title: '联系人', type: 'input', width: 110 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 130 },
  { key: 'couponName', title: '优惠券名称', type: 'slot', slotName: 'nameCell', width: 180 },
  { key: 'couponType', title: '类型', type: 'slot', slotName: 'typeCell', width: 100 },
  { key: 'useRule', title: '使用规则', type: 'slot', slotName: 'useRuleCell', width: 130 },
  { key: 'faceValue', title: '面值', type: 'slot', slotName: 'faceValueCell', width: 100 },
  { key: 'receiveStatus', title: '领用状态', type: 'slot', slotName: 'receiveStatusCell', width: 110 },
  { key: 'billNo', title: '单据编号', type: 'input', width: 170 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'receiveTime', title: '领取时间', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'usedTime', title: '使用时间', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'sourceBillNo', title: '来源单据', type: 'input', width: 170 },
]

// ── 工具 ──
function fmtDate(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD') : '-'
}
function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function fmtMoney(v: any): string {
  return v == null ? '-' : `${Number(v).toFixed(2)}元`
}
function useRuleText(rule: any): string {
  if (!rule) return '-'
  if (rule === 'UNLIMITED') return '无限制'
  if (String(rule).startsWith('FULL_')) return `满 ${String(rule).slice(5)} 元可用`
  return String(rule)
}

// ── 数据 ──
async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'template') {
      const res: any = await couponTemplateApi.page({
        couponName: searchForm.keyword || undefined,
        status: searchForm.templateStatus || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await couponTemplateApi.recordPage({
        couponName: searchForm.keyword || undefined,
        status: searchForm.status || undefined,
        billNo: searchForm.billNo || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      recordData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[优惠券] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'template') tableData.value = []
    else recordData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  selectedRowKeys.value = []
  pagination.current = 1
  fetchList()
}
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}
function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  const keys = selectedRowKeys.value.filter(k => String(k) !== String(record.id))
  if (checked) keys.push(record.id)
  selectedRowKeys.value = keys
}
function handleCheckboxAll(checked: boolean, records: any[]) {
  selectedRowKeys.value = checked ? (records || []).map(r => r.id) : []
}

const selectedRows = computed(() => {
  const keys = selectedRowKeys.value.map(String)
  const src: any[] = activeTab.value === 'template' ? tableData.value : recordData.value
  return src.filter((r: any) => keys.includes(String(r.id)) && !r.__ghost)
})

// ── 新增 / 修改 ──
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<any>(null)
const emptyForm = () => ({
  couponName: '',
  openReceiveBool: false,
  couponType: 'CASH',
  useRule: 'UNLIMITED',
  faceValue: 0 as number | undefined,
  totalCount: 0 as number | undefined,
  startTime: undefined as any,
  endTime: undefined as any,
  mallEnabledBool: true,
  offlineEnabledBool: false,
  remark: '',
})
const form = reactive(emptyForm())

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formOpen.value = true
}
function openEdit(record: any) {
  editingId.value = record.id
  Object.assign(form, emptyForm(), {
    couponName: record.couponName || '',
    openReceiveBool: record.openReceive === 1,
    couponType: record.couponType || 'CASH',
    useRule: record.useRule || 'UNLIMITED',
    faceValue: record.faceValue ?? 0,
    totalCount: record.totalCount ?? 0,
    startTime: record.startTime || undefined,
    endTime: record.endTime || undefined,
    mallEnabledBool: record.mallEnabled === 1,
    offlineEnabledBool: record.offlineEnabled === 1,
    remark: record.remark || '',
  })
  formOpen.value = true
}

async function handleSave() {
  if (!form.couponName?.trim()) {
    message.warning('请输入优惠券名称')
    return
  }
  if (form.startTime && form.endTime && dayjs(form.endTime).isBefore(dayjs(form.startTime))) {
    message.warning('到期时间不得早于起始时间')
    return
  }
  saving.value = true
  try {
    const payload: any = {
      couponName: form.couponName,
      openReceive: form.openReceiveBool ? 1 : 0,
      couponType: form.couponType,
      useRule: form.useRule,
      faceValue: form.faceValue,
      totalCount: form.totalCount,
      startTime: form.startTime || undefined,
      endTime: form.endTime || undefined,
      mallEnabled: form.mallEnabledBool ? 1 : 0,
      offlineEnabled: form.offlineEnabledBool ? 1 : 0,
      customerScope: 'ALL',
      remark: form.remark,
    }
    if (editingId.value) {
      await couponTemplateApi.update(editingId.value, payload)
      message.success('优惠券已更新')
    } else {
      await couponTemplateApi.create(payload)
      message.success('优惠券已新增')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleVoid(record: any) {
  Modal.confirm({
    title: '确认作废',
    content: `确定要作废优惠券「${record.couponName}」吗？其下未使用的券将同时作废。`,
    okText: '确认作废',
    okType: 'danger',
    onOk: async () => {
      try {
        await couponTemplateApi.voidTemplate(record.id)
        message.success('已作废')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '作废失败')
      }
    },
  })
}

// ── 发优惠券 ──
const issueOpen = ref(false)
const issuing = ref(false)
const issueTemplate = ref<any>(null)
const issueQuantity = ref(1)
const issueCustomers = ref<any[]>([])

function openIssue(record: any) {
  issueTemplate.value = record
  issueQuantity.value = 1
  issueOpen.value = true
}
async function handleIssue() {
  const tpl = issueTemplate.value
  if (!tpl) return
  const partnerIds = activeTab.value === 'template'
    ? issueCustomers.value.map((c: any) => c.id)
    : selectedRows.value.map((r: any) => r.partnerId || r.id)
  if (!partnerIds.length) {
    message.warning('请先勾选要发放的客户（或在「领用明细」中勾选记录）')
    return
  }
  issuing.value = true
  try {
    const n = await couponTemplateApi.issue(tpl.id, {
      partnerIds,
      quantityPerPartner: issueQuantity.value,
      sourceBillNo: '优惠券-后台发放',
    })
    message.success(`已发放 ${n} 张`)
    issueOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发放失败')
  } finally {
    issuing.value = false
  }
}

// ── 券核销 / 作废（后台补录） ──
function handleRedeem(record: any) {
  let orderId: number | undefined
  let orderNo = ''
  Modal.confirm({
    title: '核销优惠券',
    content: () =>
      h('div', { style: 'font-size:13px;line-height:22px' }, [
        h('div', `券号：${record.code || '-'}`),
        h('div', `客户：${record.partnerName || '-'}`),
        h('div', `面值：${fmtMoney(record.faceValue)}`),
        h('div', { style: 'margin-top:8px;color:#999' }, '说明：核销后券置为「已使用」，并记录单据号（可填线下单据号）。'),
      ]),
    okText: '确认核销',
    onOk: async () => {
      try {
        await couponTemplateApi.redeem(record.id, orderId, orderNo || undefined)
        message.success('已核销')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '核销失败')
      }
    },
  })
}

function handleVoidCoupon(record: any) {
  Modal.confirm({
    title: '作废优惠券',
    content: `确定要作废券「${record.code || ''}」吗？作废后不可恢复。`,
    okText: '确认作废',
    okType: 'danger',
    onOk: async () => {
      try {
        await couponTemplateApi.voidCoupon(record.id)
        message.success('已作废')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '作废失败')
      }
    },
  })
}

// ── 发短信 ──
const smsOpen = ref(false)
const smsSending = ref(false)
const smsContent = ref('')
const smsSignName = ref('')
const smsType = ref('NOTICE')
const smsAgreed = ref(false)

async function openSendSms() {
  if (!selectedRowKeys.value.length) {
    message.warning('请先勾选记录')
    return
  }
  smsOpen.value = true
  try {
    const setting = await smsMarketingApi.getSetting()
    smsSignName.value = setting?.signName || ''
  } catch (e) {
    console.error('[优惠券] 加载短信设置失败', e)
  }
}
async function handleSendSms() {
  if (!smsContent.value.trim()) {
    message.warning('请填写短信内容')
    return
  }
  if (!smsAgreed.value) {
    message.warning('请先勾选同意短信协议')
    return
  }
  const ids = selectedRows.value
    .map((r: any) => r.partnerId || r.id)
    .filter((v: any) => v != null)
  const unique = Array.from(new Set(ids.map(String))).map(v => Number(v))
  if (!unique.length) {
    message.warning('所选记录无可发送的客户')
    return
  }
  smsSending.value = true
  try {
    const res = await smsMarketingApi.send({
      partnerIds: unique,
      content: smsContent.value,
      signName: smsSignName.value,
      smsType: smsType.value,
      agreed: smsAgreed.value,
    })
    message.success(`已提交发送 ${res?.sentCount ?? 0} 条`)
    smsOpen.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发送失败')
  } finally {
    smsSending.value = false
  }
}

// ── 打印 / 导出 ──
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
const PRINT_COLUMNS_TEMPLATE = [
  { key: 'couponName', title: '优惠券名称' },
  { key: 'openReceiveText', title: '开放领取' },
  { key: 'couponTypeText', title: '类型' },
  { key: 'useRuleText', title: '使用规则' },
  { key: 'faceValue', title: '面值' },
  { key: 'totalCount', title: '总数' },
  { key: 'receivedCount', title: '已领取（未使用）' },
  { key: 'usedCount', title: '已使用' },
  { key: 'remainingCount', title: '未领取' },
  { key: 'scopeText', title: '指定客户' },
  { key: 'startTime', title: '起始时间' },
  { key: 'endTime', title: '到期时间' },
  { key: 'statusText', title: '状态' },
  { key: 'mallText', title: '商城使用' },
  { key: 'offlineText', title: '线下使用' },
]
const PRINT_COLUMNS_RECORD = [
  { key: 'partnerName', title: '客户' },
  { key: 'contactName', title: '联系人' },
  { key: 'contactPhone', title: '联系电话' },
  { key: 'couponName', title: '优惠券名称' },
  { key: 'couponTypeText', title: '类型' },
  { key: 'useRuleText', title: '使用规则' },
  { key: 'faceValue', title: '面值' },
  { key: 'receiveStatus', title: '领用状态' },
  { key: 'billNo', title: '单据编号' },
  { key: 'statusText', title: '状态' },
  { key: 'receiveTime', title: '领取时间' },
  { key: 'usedTime', title: '使用时间' },
  { key: 'sourceBillNo', title: '来源单据' },
]

function printCell(row: any, key: string, isTemplate: boolean): string {
  switch (key) {
    case 'openReceiveText': return row.openReceive === 1 ? '是' : '否'
    case 'couponTypeText': return COUPON_TYPE_MAP[row.couponType] || row.couponType || ''
    case 'useRuleText': return useRuleText(row.useRule)
    case 'scopeText': return row.customerScope === 'SPECIFIED' ? '指定客户' : '全部客户'
    case 'statusText': return COUPON_STATUS_MAP[row.status]?.text || row.status || ''
    case 'mallText': return row.mallEnabled === 1 ? '允许' : '禁止'
    case 'offlineText': return row.offlineEnabled === 1 ? '允许' : '禁止'
    case 'startTime': case 'endTime': return fmtDate(row[key])
    case 'receiveTime': case 'usedTime': return fmtTime(row[key])
    default: return row[key] == null ? '' : String(row[key])
  }
}

function currentPrintRows() {
  const isTemplate = activeTab.value === 'template'
  const src: any[] = isTemplate ? tableData.value : recordData.value
  return { isTemplate, cols: isTemplate ? PRINT_COLUMNS_TEMPLATE : PRINT_COLUMNS_RECORD, rows: (src || []).filter((r: any) => !r.__ghost) }
}

function handlePrint() {
  const { isTemplate, cols, rows } = currentPrintRows()
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const title = isTemplate ? '优惠券设置' : '优惠券领用明细'
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    cols.map(c => `<td>${escapeHtml(printCell(r, c.key, isTemplate))}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>${title}</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>${title}</h2>
    <table><thead><tr><th>#</th>${cols.map(c => `<th>${c.title}</th>`).join('')}</tr></thead>
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
  const { isTemplate, cols, rows } = currentPrintRows()
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  exporting.value = true
  try {
    const csv = '\uFEFF' + [cols.map(c => c.title),
      ...rows.map((r: any) => cols.map(c => printCell(r, c.key, isTemplate)))]
      .map(line => line.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `优惠券_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } finally {
    exporting.value = false
  }
}

function handleError(error: Error) {
  console.error('[优惠券] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
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
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
