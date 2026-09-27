<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        储值卡（营销 → 会员中心 → 储值卡，菜单 80304）
        ⚠️ **本系统建模页**：ql361「营销」域实测仅 17 页，无对应页（见 README §1.3）。
        口径来源：金蝶云星辰「积分储值」/ 有赞「储值即会员（充300送60）」/ 微盟「权益卡」。
        ⚠️ 合规（页面必须提供退款入口与告知）：
          · 商务部《单用途商业预付卡管理办法》——发卡备案；充值档位不宜过高、赠送比例须在毛利承受范围内
          · 最高法法释〔2025〕4 号（2025-05-01 施行）——预付费纠纷责任认定，须明确退款规则
        双视图 Tab：① 卡档案 ② 储值流水；逐 Tab 独立列配置。
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
            v-if="activeTab === 'card'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openIssue"
          >
            <PlusOutlined /> 开卡
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="activeTab === 'card'"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-space>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="activeTab === 'card'">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请输入卡号/会员名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">卡类型</span>
                <a-select
                  v-model:value="searchForm.cardType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="TYPE_OPTIONS"
                  @change="handleSearch"
                />
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
              </template>
              <template v-else>
                <span class="search-label">卡号</span>
                <a-input
                  v-model:value="flowSearch.cardNo"
                  placeholder="请输入卡号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">流水类型</span>
                <a-select
                  v-model:value="flowSearch.flowType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="FLOW_TYPE_OPTIONS"
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
            <!-- ═══ Tab1 卡档案 ═══ -->
            <BillDetailTable
              v-if="activeTab === 'card'"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-stored-card-table-columns"
              global-config-key="marketing-stored-card-table-columns"
            >
              <template #cardNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record)"
                >{{ record.cardNo }}</a>
              </template>
              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ STORED_CARD_TYPE_MAP[record.cardType] || record.cardType || '-' }}</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtMoney(record[column.key]) }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="STORED_CARD_STATUS_MAP[record.status]?.color || 'default'"
                >
                  {{ STORED_CARD_STATUS_MAP[record.status]?.text || record.status || '-' }}
                </a-tag>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.status === 'FROZEN' || record.status === 'REFUNDED'"
                    @click="openRecharge(record)"
                  >
                    充值
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.status === 'FROZEN' || record.status === 'REFUNDED'"
                    @click="openConsume(record)"
                  >
                    消费
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu @click="(e: any) => handleMore(e, record)">
                        <a-menu-item key="refund">退款</a-menu-item>
                        <a-menu-item key="flows">查看流水</a-menu-item>
                        <a-menu-item key="freeze">
                          {{ record.status === 'FROZEN' ? '解冻' : '冻结' }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab2 储值流水 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="flowData"
              :columns="flowColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-stored-card-flow-table-columns"
              global-config-key="marketing-stored-card-flow-table-columns"
            >
              <template #flowTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="FLOW_TYPE_COLOR[record.flowType] || 'default'"
                >
                  {{ STORED_FLOW_TYPE_MAP[record.flowType] || record.flowType || '-' }}
                </a-tag>
              </template>
              <template #moneyCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtMoney(record[column.key]) }}</span>
              </template>
              <template #settleCell="{ record }">
                <span v-if="!record.__ghost">{{ SETTLE_ACCOUNT_MAP[record.settleAccount] || '-' }}</span>
              </template>
              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ fmtTime(record[column.key]) }}</span>
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

      <!-- ═══ 开卡 ═══ -->
      <a-modal
        v-model:open="issueOpen"
        title="开卡"
        :confirm-loading="saving"
        width="620px"
        @ok="handleIssue"
      >
        <a-alert
          type="warning"
          show-icon
          message="合规提示"
          description="单用途预付卡须遵守《单用途商业预付卡管理办法》与最高法法释〔2025〕4 号：充值档位不宜过高、赠送比例须在毛利承受范围内；本页提供「退款」入口，不得只进不出。"
        />
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-row :gutter="8">
            <a-col :span="12">
              <a-form-item
                label="卡号"
                required
              >
                <a-input
                  v-model:value="issueForm.cardNo"
                  placeholder="如 SC0001"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="卡类型">
                <a-select
                  v-model:value="issueForm.cardType"
                  :options="TYPE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="会员">
                <a-space>
                  <a-button
                    size="small"
                    @click="customerPickerOpen = true"
                  >
                    选择客户
                  </a-button>
                  <span class="selected-tip">{{ issueForm.partnerName || '未选择' }}</span>
                </a-space>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="面值"
                required
              >
                <a-input-number
                  v-model:value="issueForm.faceValue"
                  :min="0.01"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="开卡赠送">
                <a-input-number
                  v-model:value="issueForm.bonusAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="有效期至">
                <a-date-picker
                  v-model:value="issueForm.expireTime"
                  style="width: 100%"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="结算账户">
                <a-select
                  v-model:value="issueForm.settleAccount"
                  :options="SETTLE_ACCOUNT_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-input v-model:value="issueForm.remark" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <!-- ═══ 充值 / 消费 / 退款（共用弹窗，按 mode 切换） ═══ -->
      <a-modal
        v-model:open="opOpen"
        :title="opTitle"
        :confirm-loading="saving"
        width="520px"
        @ok="handleOpConfirm"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 15 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="卡号">
            {{ opCard?.cardNo }}（余额 {{ fmtMoney(opCard?.balance) }}）
          </a-form-item>
          <a-form-item
            :label="opMode === 'refund' ? '退款金额' : (opMode === 'recharge' ? '充值金额' : '消费金额')"
            required
          >
            <a-input-number
              v-model:value="opForm.amount"
              :min="0.01"
              :precision="2"
              :max="opMode === 'consume' || opMode === 'refund' ? Number(opCard?.balance || 0) : undefined"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            v-if="opMode === 'recharge' || opMode === 'refund'"
            label="结算账户"
          >
            <a-select
              v-model:value="opForm.settleAccount"
              :options="SETTLE_ACCOUNT_OPTIONS"
            />
          </a-form-item>
          <a-form-item
            v-if="opMode === 'recharge'"
            label="充值赠送"
          >
            <a-input-number
              v-model:value="opForm.bonusAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            v-if="opMode !== 'recharge'"
            label="备注"
          >
            <a-input
              v-model:value="opForm.remark"
              :placeholder="opMode === 'refund' ? '退款原因（建议填写，便于合规追溯）' : '备注'"
            />
          </a-form-item>
          <a-form-item
            v-else
            label="关联单据"
          >
            <a-input
              v-model:value="opForm.sourceBillNo"
              placeholder="选填（如收款单号）"
            />
          </a-form-item>
          <a-alert
            v-if="opMode === 'refund'"
            type="info"
            show-icon
            message="退款为预付费合规入口：按卡内余额退回，全额退回即退卡"
          />
        </a-form>
      </a-modal>

      <!-- ═══ 卡详情 + 流水 ═══ -->
      <a-modal
        v-model:open="detailOpen"
        :title="`卡详情 · ${detailCard?.cardNo || ''}`"
        width="860px"
        :footer="null"
      >
        <a-descriptions
          :column="3"
          size="small"
          bordered
        >
          <a-descriptions-item label="卡号">
            {{ detailCard?.cardNo }}
          </a-descriptions-item>
          <a-descriptions-item label="会员">
            {{ detailCard?.partnerName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="卡类型">
            {{ STORED_CARD_TYPE_MAP[detailCard?.cardType] || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="面值">
            {{ fmtMoney(detailCard?.faceValue) }}
          </a-descriptions-item>
          <a-descriptions-item label="余额">
            {{ fmtMoney(detailCard?.balance) }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ STORED_CARD_STATUS_MAP[detailCard?.status]?.text || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="累计充值">
            {{ fmtMoney(detailCard?.totalRecharge) }}
          </a-descriptions-item>
          <a-descriptions-item label="累计赠送">
            {{ fmtMoney(detailCard?.totalBonus) }}
          </a-descriptions-item>
          <a-descriptions-item label="累计消费">
            {{ fmtMoney(detailCard?.totalConsume) }}
          </a-descriptions-item>
          <a-descriptions-item label="开卡时间">
            {{ fmtTime(detailCard?.issueTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="有效期至">
            {{ detailCard?.expireTime ? fmtTime(detailCard.expireTime) : '长期有效' }}
          </a-descriptions-item>
          <a-descriptions-item label="备注">
            {{ detailCard?.remark || '-' }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table
          :columns="FLOW_DETAIL_COLUMNS"
          :data-source="detailFlows"
          :loading="detailLoading"
          :pagination="false"
          row-key="id"
          size="small"
          style="margin-top: 12px"
          :scroll="{ y: 300 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'flowType'">
              {{ STORED_FLOW_TYPE_MAP[record.flowType] || record.flowType }}
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ fmtTime(record.createTime) }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <PartnerSelectModal
        v-model:open="customerPickerOpen"
        default-tab="customer"
        @select="handleCustomerPicked"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="marketing-stored-card"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import {
  storedCardApi, STORED_CARD_TYPE_MAP, STORED_CARD_STATUS_MAP, STORED_FLOW_TYPE_MAP,
  SETTLE_ACCOUNT_OPTIONS,
  type StoredCard, type StoredCardFlow,
} from '@/api/marketing'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MarketingStoredCard' })

const TABS = [
  { key: 'card', label: '卡档案' },
  { key: 'flow', label: '储值流水' },
]
const activeTab = ref('card')

const TYPE_OPTIONS = Object.entries(STORED_CARD_TYPE_MAP).map(([value, label]) => ({ value, label }))
const STATUS_OPTIONS = Object.entries(STORED_CARD_STATUS_MAP).map(([value, v]) => ({ value, label: v.text }))
const FLOW_TYPE_OPTIONS = Object.entries(STORED_FLOW_TYPE_MAP).map(([value, label]) => ({ value, label }))
const SETTLE_ACCOUNT_MAP: Record<string, string> = {
  CASH: '库存现金', BANK: '银行存款',
}
const FLOW_TYPE_COLOR: Record<string, string> = {
  ISSUE: 'blue', RECHARGE: 'green', BONUS: 'cyan', CONSUME: 'orange', REFUND: 'purple', ADJUST: 'default',
}

const loading = ref(false)
const saving = ref(false)
const tableData = ref<StoredCard[]>([])
const flowData = ref<StoredCardFlow[]>([])
const searchForm = reactive({
  keyword: '', cardType: undefined as string | undefined, status: undefined as string | undefined,
})
const flowSearch = reactive({ cardNo: '', flowType: undefined as string | undefined })
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// Tab1 卡档案
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'cardNo', title: '卡号', type: 'slot', slotName: 'cardNoCell', width: 150 },
  { key: 'partnerName', title: '会员/客户', type: 'input', width: 200 },
  { key: 'cardType', title: '卡类型', type: 'slot', slotName: 'typeCell', width: 100 },
  { key: 'faceValue', title: '面值', type: 'slot', slotName: 'moneyCell', width: 110 },
  { key: 'balance', title: '当前余额', type: 'slot', slotName: 'moneyCell', width: 110 },
  { key: 'totalRecharge', title: '累计充值', type: 'slot', slotName: 'moneyCell', width: 110 },
  { key: 'totalBonus', title: '累计赠送', type: 'slot', slotName: 'moneyCell', width: 110 },
  { key: 'totalConsume', title: '累计消费', type: 'slot', slotName: 'moneyCell', width: 110 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'issueTime', title: '开卡时间', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'expireTime', title: '有效期至', type: 'slot', slotName: 'timeCell', width: 170 },
  { key: 'remark', title: '备注', type: 'input', width: 180 },
]

// Tab2 储值流水
const flowColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'cardNo', title: '卡号', type: 'input', width: 150 },
  { key: 'flowType', title: '流水类型', type: 'slot', slotName: 'flowTypeCell', width: 110 },
  { key: 'amount', title: '本金变动', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'bonusAmount', title: '赠送变动', type: 'slot', slotName: 'moneyCell', width: 120 },
  { key: 'balanceAfter', title: '变动后余额', type: 'slot', slotName: 'moneyCell', width: 130 },
  { key: 'sourceBillNo', title: '关联单据', type: 'input', width: 160 },
  { key: 'settleAccount', title: '结算账户', type: 'slot', slotName: 'settleCell', width: 100 },
  { key: 'voucherNo', title: '记账凭证号', type: 'input', width: 150 },
  { key: 'handlerName', title: '经手人', type: 'input', width: 120 },
  { key: 'remark', title: '备注', type: 'input', width: 220 },
  { key: 'createTime', title: '发生时间', type: 'slot', slotName: 'timeCell', width: 170 },
]

const FLOW_DETAIL_COLUMNS = [
  { title: '类型', dataIndex: 'flowType', key: 'flowType', width: 100 },
  { title: '本金变动', dataIndex: 'amount', key: 'amount', width: 110 },
  { title: '赠送变动', dataIndex: 'bonusAmount', key: 'bonusAmount', width: 110 },
  { title: '变动后余额', dataIndex: 'balanceAfter', key: 'balanceAfter', width: 120 },
  { title: '经手人', dataIndex: 'handlerName', key: 'handlerName', width: 110 },
  { title: '记账凭证号', dataIndex: 'voucherNo', key: 'voucherNo', width: 150 },
  { title: '发生时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
]

function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function fmtMoney(v: any): string {
  return v == null ? '-' : Number(v).toFixed(2)
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'card') {
      const res: any = await storedCardApi.page({
        keyword: searchForm.keyword || undefined,
        cardType: searchForm.cardType || undefined,
        status: searchForm.status || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await storedCardApi.flowPage({
        cardNo: flowSearch.cardNo || undefined,
        flowType: flowSearch.flowType || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      flowData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[储值卡] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'card') tableData.value = []
    else flowData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
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

// ═══ 开卡 ═══
const issueOpen = ref(false)
const customerPickerOpen = ref(false)
const issueForm = reactive({
  cardNo: '',
  cardType: 'STORED' as string | undefined,
  partnerId: undefined as any,
  partnerName: '',
  faceValue: 500 as number | undefined,
  bonusAmount: 0 as number | undefined,
  expireTime: undefined as any,
  remark: '',
  settleAccount: 'BANK' as string | undefined,
})

function openIssue() {
  Object.assign(issueForm, {
    cardNo: 'SC' + dayjs().format('YYYYMMDDHHmmss'),
    cardType: 'STORED', partnerId: undefined, partnerName: '',
    faceValue: 500, bonusAmount: 0, expireTime: undefined, remark: '',
    settleAccount: 'BANK',
  })
  issueOpen.value = true
}
function handleCustomerPicked(record: any) {
  issueForm.partnerId = record?.id
  issueForm.partnerName = record?.partyName || record?.partnerName || ''
  customerPickerOpen.value = false
}

async function handleIssue() {
  if (!issueForm.cardNo?.trim()) {
    message.warning('请输入卡号')
    return
  }
  if (!issueForm.partnerId) {
    message.warning('请选择会员/客户')
    return
  }
  if (!issueForm.faceValue || issueForm.faceValue <= 0) {
    message.warning('请输入大于 0 的面值')
    return
  }
  saving.value = true
  try {
    await storedCardApi.issue({
      card: {
        cardNo: issueForm.cardNo,
        cardType: issueForm.cardType,
        partnerId: issueForm.partnerId,
        partnerName: issueForm.partnerName,
        faceValue: issueForm.faceValue,
        expireTime: issueForm.expireTime || undefined,
        settleAccount: issueForm.settleAccount,
      },
      bonusAmount: issueForm.bonusAmount,
      remark: issueForm.remark,
    })
    message.success('开卡成功')
    issueOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '开卡失败')
  } finally {
    saving.value = false
  }
}

// ═══ 充值 / 消费 / 退款 ═══
const opOpen = ref(false)
const opMode = ref<'recharge' | 'consume' | 'refund'>('recharge')
const opCard = ref<any>(null)
const opForm = reactive({
  amount: 0 as number | undefined, bonusAmount: 0 as number | undefined,
  sourceBillNo: '', remark: '', settleAccount: 'BANK' as string | undefined,
})

const opTitle = computed(() => ({
  recharge: '充值', consume: '消费扣减', refund: '退款（合规入口）',
}[opMode.value]))

function openOp(record: any, mode: 'recharge' | 'consume' | 'refund') {
  opCard.value = record
  opMode.value = mode
  Object.assign(opForm, {
    amount: undefined, bonusAmount: 0, sourceBillNo: '', remark: '',
    settleAccount: record?.settleAccount || 'BANK',
  })
  opOpen.value = true
}
const openRecharge = (r: any) => openOp(r, 'recharge')
const openConsume = (r: any) => openOp(r, 'consume')
const openRefund = (r: any) => openOp(r, 'refund')

async function handleOpConfirm() {
  if (!opForm.amount || opForm.amount <= 0) {
    message.warning('请输入大于 0 的金额')
    return
  }
  saving.value = true
  try {
    if (opMode.value === 'recharge') {
      await storedCardApi.recharge(opCard.value.id, {
        amount: opForm.amount, bonusAmount: opForm.bonusAmount, sourceBillNo: opForm.sourceBillNo,
        settleAccount: opForm.settleAccount,
      })
      message.success('充值成功')
    } else if (opMode.value === 'consume') {
      await storedCardApi.consume(opCard.value.id, { amount: opForm.amount, sourceBillNo: opForm.sourceBillNo })
      message.success('消费扣减成功')
    } else {
      await storedCardApi.refund(opCard.value.id, {
        amount: opForm.amount, remark: opForm.remark, settleAccount: opForm.settleAccount,
      })
      message.success('退款成功')
    }
    opOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// ═══ 更多：退款 / 流水 / 冻结 ═══
function handleMore(e: any, record: any) {
  if (e.key === 'refund') {
    openRefund(record)
  } else if (e.key === 'flows') {
    openDetail(record)
  } else if (e.key === 'freeze') {
    const next = record.status === 'FROZEN' ? 'ACTIVE' : 'FROZEN'
    Modal.confirm({
      title: next === 'FROZEN' ? '冻结卡片' : '解冻卡片',
      content: `确定要${next === 'FROZEN' ? '冻结' : '解冻'}卡「${record.cardNo}」吗？`,
      onOk: async () => {
        try {
          await storedCardApi.changeStatus(record.id, next)
          message.success(next === 'FROZEN' ? '已冻结' : '已解冻')
          fetchList()
        } catch (error: any) {
          message.error(error?.response?.data?.message || '操作失败')
        }
      },
    })
  }
}

// ═══ 卡详情 + 流水 ═══
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailCard = ref<any>(null)
const detailFlows = ref<any[]>([])

async function openDetail(record: any) {
  detailCard.value = record
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res: any = await storedCardApi.flowPage({ cardId: record.id, pageNum: 1, pageSize: 200 })
    detailFlows.value = res?.records || []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载流水失败')
    detailFlows.value = []
  } finally {
    detailLoading.value = false
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open()，现在交给 PrintDialog：列与行由页面给，模板负责版式。
const PRINT_COLUMNS = [
  { key: 'cardNo', title: '卡号' },
  { key: 'partnerName', title: '会员/客户' },
  { key: 'cardType', title: '卡类型' },
  { key: 'faceValue', title: '面值' },
  { key: 'balance', title: '当前余额' },
  { key: 'status', title: '状态' },
  { key: 'issueTime', title: '开卡时间' },
]
function printCell(row: any, key: string): string {
  if (key === 'cardType') return STORED_CARD_TYPE_MAP[row.cardType] || ''
  if (key === 'faceValue' || key === 'balance') return fmtMoney(row[key])
  if (key === 'status') return STORED_CARD_STATUS_MAP[row.status]?.text || ''
  if (key === 'issueTime') return fmtTime(row.issueTime)
  return row[key] == null ? '' : String(row[key])
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'marketing-stored-card',
  // 列是原打印函数里写死的清单，静态生成器写不进模板 → 明确按数据列打
  useDataColumns: true,
  title: '储值卡台账',
  columns: () => PRINT_COLUMNS.map(c => ({
    key: c.key,
    title: c.title,
    formatter: (_v: any, row: any) => printCell(row, c.key),
  })),
  rows: () => (tableData.value || []).filter((r: any) => !r.__ghost),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    if (activeTab.value === 'card') handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[储值卡] 页面错误', error)
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
.selected-tip { font-size: 12px; color: #666; }
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
