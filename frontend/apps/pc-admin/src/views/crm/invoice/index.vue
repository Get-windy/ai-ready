<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        CRM 发票 · 台账列表（菜单 70350 的 list_path → 标签「历史」）
        · CRM 为本系统独有模块（ql361 无 CRM 域）；但本页后端不在 crm 模块，而是 ERP 财务的
          InvoiceController（@RequestMapping("/api/erp/invoice")，纯 Spring Data JPA）
        · 注意：菜单主 path 是表单页 crm/invoice/form，本页是「历史」标签页（与客户/线索/商机/合同相反）
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable(+StandardPagination)
        · 本轮修复：
          ① P0 分页键：/erp/invoice/query 是 Spring Data Pageable（认 page(0 基)/size），原发 pageNum/pageSize 恒第 1 页
          ② P0 字段契约：invoiceNo→invoiceNumber、status→invoiceStatus、amount→subtotalAmount、
             invoiceTypeLabel→invoiceType(枚举 name)、issuer→issuedByName、remark→notes
          ③ P0 状态词表：前端 5 个字符串 → 后端 InvoiceStatus 13 个枚举 name（原先 5 个行操作入口恒隐藏）
          ④ P1 三个 Tab：改调 /erp/invoice/query?direction=sales|purchase（原 activeTab 从不参与查询）
          ⑤ P1 汇总行：summary-columns 必须是 { key, value }（原先只有 label/value → 合计行全空）
          ⑥ P1 列配置键：storage-key / global-config-key 按 Tab 独立（原先未传，回退到共享默认键，跨页串配置）
          ⑦ P1 发送/作废：补必填 sentBy / voidedBy（取当前登录用户），发送方式走弹窗选择
          ⑧ P1 编辑：改跳 form.vue（原内嵌弹窗恒调 create → 编辑变重复新建），删除硬编码客户/订单假数据
          ⑨ 在 Tab 之间独立列定义 + 查询条件 + 功能按钮 + storage-key（多 Tab 页铁律）
        · 后端缺口（前端无法修，如实登记）：发票 JPA 路径无 tenant_id / is_deleted 过滤（跨租户可见、
          作废票仍出现在列表）；新建发票仍须先有「发票申请单」。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            v-permission="'crm:invoice:create'"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleCreate"
          >
            <PlusOutlined /> 新建发票
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-tooltip
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
          </a-space>
        </template>

        <!-- ═══ 查询区（横向网格；只暴露 /query 真正接收的参数） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isFieldVisible('invoiceNumber')"
                class="search-item"
              >
                <span class="search-label">发票号码</span>
                <a-input
                  v-model:value="searchForm.invoiceNumber"
                  placeholder="输入发票号码"
                  size="small"
                  allow-clear
                  style="width: 180px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('partnerName')"
                class="search-item"
              >
                <span class="search-label">{{ partnerLabel }}</span>
                <a-input
                  v-model:value="searchForm.partnerName"
                  :placeholder="`输入${partnerLabel}`"
                  size="small"
                  allow-clear
                  style="width: 180px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('issuedByName')"
                class="search-item"
              >
                <span class="search-label">开票人</span>
                <a-input
                  v-model:value="searchForm.issuedByName"
                  placeholder="输入开票人"
                  size="small"
                  allow-clear
                  style="width: 140px"
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('invoiceDateRange')"
                class="search-item"
              >
                <span class="search-label">开票日期</span>
                <a-range-picker
                  v-model:value="searchForm.invoiceDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
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

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <a-alert
              v-if="hasError"
              class="table-error"
              type="warning"
              show-icon
              message="发票数据加载失败"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="handleRefresh"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <!-- 统计卡：四张口径统一（全量，来自 GET /erp/invoice/statistics）；接口不可用时回落本页合计 -->
            <div class="stats-cards">
              <a-row :gutter="16">
                <a-col :span="6">
                  <div class="stat-card stat-card-blue">
                    <div
                      class="stat-icon"
                      style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"
                    >
                      <FileTextOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        发票总数
                      </div>
                      <div class="stat-value">
                        {{ stats.totalCount }}
                      </div>
                      <div class="stat-desc">
                        {{ stats.fromApi ? '全部发票（全量）' : '全部发票（按当前查询）' }}
                      </div>
                    </div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-card stat-card-purple">
                    <div
                      class="stat-icon"
                      style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"
                    >
                      <DollarOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        价税合计
                      </div>
                      <div class="stat-value">
                        ¥{{ formatAmount(stats.totalAmount) }}
                      </div>
                      <div class="stat-desc">
                        {{ stats.fromApi ? '全量合计' : '本页合计' }}
                      </div>
                    </div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-card stat-card-green">
                    <div
                      class="stat-icon"
                      style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"
                    >
                      <CheckCircleOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        已付金额
                      </div>
                      <div class="stat-value">
                        ¥{{ formatAmount(stats.totalPaid) }}
                      </div>
                      <div class="stat-desc positive">
                        {{ stats.fromApi ? '全量已付' : '本页已付' }}
                      </div>
                    </div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="stat-card stat-card-orange">
                    <div
                      class="stat-icon"
                      style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"
                    >
                      <WarningOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        未付金额
                      </div>
                      <div class="stat-value">
                        ¥{{ formatAmount(stats.totalUnpaid) }}
                      </div>
                      <div class="stat-desc">
                        逾期 {{ stats.overdueCount }} 张
                      </div>
                    </div>
                  </div>
                </a-col>
              </a-row>
            </div>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
            >
              <template #invoiceTypeCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ getInvoiceTypeText(record.invoiceType) }}</span>
              </template>

              <template #invoiceStatusCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else
                  :color="getStatusColor(record.invoiceStatus)"
                >
                  {{ getStatusText(record.invoiceStatus) }}
                </a-tag>
              </template>

              <template #paymentStatusCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else
                  :color="getPaymentStatusColor(record.paymentStatus)"
                >
                  {{ getPaymentStatusText(record.paymentStatus) }}
                </a-tag>
              </template>

              <template #creditCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else-if="record.isCreditNote"
                  color="magenta"
                >
                  红冲票
                </a-tag>
                <span v-else>-</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-permission="'crm:invoice:view'"
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="isEditable(record)"
                    v-permission="'crm:invoice:edit'"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="canIssue(record)"
                    v-permission="'crm:invoice:issue'"
                    type="link"
                    size="small"
                    @click="handleIssue(record)"
                  >
                    开具
                  </a-button>
                  <a-button
                    v-if="canSend(record)"
                    v-permission="'crm:invoice:send'"
                    type="link"
                    size="small"
                    @click="handleSend(record)"
                  >
                    发送
                  </a-button>
                  <a-button
                    v-if="canVoid(record)"
                    v-permission="'crm:invoice:cancelconfirm'"
                    type="link"
                    size="small"
                    danger
                    @click="handleCancelConfirm(record)"
                  >
                    作废
                  </a-button>
                  <!-- 打印：走打印管理模块的打印链；record 需把 invoiceNumber 适配成组件识别的 invoiceNo -->
                  <PrintButton
                    v-if="canPrint(record)"
                    template-type="invoice"
                    :business-id="String(record.id)"
                    business-type="invoice"
                    :record="printRecord(record)"
                    button-text="打印"
                    button-size="small"
                    button-type="link"
                    tooltip="打印"
                  />
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 经典分页栏 ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用；按 Tab 独立存储键） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="activeQueryFields"
        :function-buttons-config="activeFunctionButtons"
        :default-query-fields-config="defaultQueryFields"
        :default-function-buttons-config="defaultFunctionButtons"
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 发送方式弹窗（后端 POST /{id}/send 的 sendMethod + sentBy 均必填） ═══ -->
      <a-modal
        v-model:open="sendVisible"
        title="发送发票"
        :confirm-loading="sendLoading"
        ok-text="确认发送"
        cancel-text="取消"
        :width="420"
        @ok="handleSendConfirm"
      >
        <p class="modal-tip">
          发票号码：<span class="mono">{{ sendTarget.invoiceNumber }}</span>
        </p>
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="发送方式">
            <a-select
              v-model:value="sendMethod"
              :options="SEND_METHOD_OPTIONS"
              placeholder="请选择发送方式"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 作废原因弹窗（后端 POST /{id}/void 的 reason + voidedBy 均必填） ═══ -->
      <a-modal
        v-model:open="voidVisible"
        title="发票作废"
        :confirm-loading="voidLoading"
        ok-text="确认作废"
        ok-type="danger"
        cancel-text="取消"
        :width="460"
        @ok="handleVoidConfirm"
      >
        <p class="modal-tip">
          发票号码：<span class="mono">{{ voidTarget.invoiceNumber }}</span>
        </p>
        <a-textarea
          v-model:value="voidReason"
          :rows="3"
          placeholder="请填写作废原因（后端必填）"
        />
      </a-modal>

      <!-- ═══ 详情抽屉（字段名与 Invoice 实体逐字对齐） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="发票详情"
        placement="right"
        width="80vw"
        :footer="null"
      >
        <a-spin :spinning="detailLoading">
          <a-alert
            v-if="detailError"
            type="warning"
            show-icon
            message="详情数据加载失败"
          >
            <template #action>
              <a-button
                v-permission="'crm:invoice:detailrefresh'"
                size="small"
                @click="fetchDetail(detailId)"
              >
                重试
              </a-button>
            </template>
          </a-alert>
          <template v-else-if="detailData.id">
            <a-descriptions
              :column="2"
              bordered
              size="small"
            >
              <a-descriptions-item label="发票号码">
                <span class="mono">{{ detailData.invoiceNumber || '-' }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="发票类型">
                <a-tag>{{ getInvoiceTypeText(detailData.invoiceType) }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="发票状态">
                <a-tag :color="getStatusColor(detailData.invoiceStatus)">
                  {{ getStatusText(detailData.invoiceStatus) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="付款状态">
                <a-tag :color="getPaymentStatusColor(detailData.paymentStatus)">
                  {{ getPaymentStatusText(detailData.paymentStatus) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="客户名称">
                {{ detailData.customerName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="客户税号">
                {{ detailData.customerTaxNumber || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="供应商名称">
                {{ detailData.supplierName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="供应商税号">
                {{ detailData.supplierTaxNumber || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="开票日期">
                {{ detailData.invoiceDate || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="到期日">
                {{ detailData.dueDate || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="不含税金额">
                <span class="mono">¥{{ formatAmount(detailData.subtotalAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="税额">
                <span class="mono">¥{{ formatAmount(detailData.taxAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="折扣金额">
                <span class="mono">¥{{ formatAmount(detailData.discountAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="运费">
                <span class="mono">¥{{ formatAmount(detailData.shippingAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="其他费用">
                <span class="mono">¥{{ formatAmount(detailData.otherAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="价税合计">
                <span class="mono amount-red">¥{{ formatAmount(detailData.totalAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="已付金额">
                <span class="mono">¥{{ formatAmount(detailData.paidAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="未付金额">
                <span class="mono">¥{{ formatAmount(detailData.unpaidAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="币种">
                {{ detailData.currencyCode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="汇率">
                {{ detailData.exchangeRate ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="开票人">
                {{ detailData.issuedByName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="开票时间">
                {{ formatDateTime(detailData.issuedAt) || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="复核人">
                {{ detailData.reviewedByName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="复核时间">
                {{ formatDateTime(detailData.reviewedAt) || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="发送方式">
                {{ getSendMethodText(detailData.sendMethod) || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="发送时间">
                {{ formatDateTime(detailData.sentAt) || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="订单编号">
                {{ detailData.orderNumber || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="合同编号">
                {{ detailData.contractNumber || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="发货单号">
                {{ detailData.deliveryNumber || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="项目编号">
                {{ detailData.projectCode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="红冲票">
                {{ detailData.isCreditNote ? '是' : '否' }}
              </a-descriptions-item>
              <a-descriptions-item label="付款条件">
                {{ detailData.paymentTerms || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ detailData.notes || '无' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-divider>发票明细</a-divider>
            <!-- 后端 createInvoiceFromApplication 不写 InvoiceItem（invoice_item 表 0 行）→ 明细恒空，
                 属后端缺口，前端不造假数据，如实提示 -->
            <a-empty
              v-if="!detailItems.length"
              description="后端开票链路未写入发票明细（invoice_item 无写入路径），故明细恒为空"
            />
            <div
              v-else
              class="detail-items"
            >
              <BillDetailTable
                v-model:data-source="detailItems"
                :columns="detailItemColumns"
                :loading="detailLoading"
                :view-mode="true"
                :min-rows="5"
                storage-key="crm-invoice-detail-item-columns"
                global-config-key="crm-invoice-detail-item-columns"
              >
                <template #itemAmountCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span
                    v-else
                    class="mono"
                  >¥{{ formatAmount(record.amount) }}</span>
                </template>
                <template #itemTaxAmountCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span
                    v-else
                    class="mono"
                  >¥{{ formatAmount(record.taxAmount) }}</span>
                </template>
              </BillDetailTable>
            </div>
          </template>
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { invoiceApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
  FileTextOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmInvoiceList' })

const router = useRouter()
const userStore = useUserStore()

/**
 * 后端端点直连（invoiceApi 未封装的参数/端点；本任务禁止改 src/api/，缺口已在交付报告登记）：
 * · /query：方向 + 条件分页，Spring Data Pageable（page 从 0 开始 + size）
 * · /statistics：全量聚合（totalCount/totalAmount/totalPaid/totalUnpaid/overdueCount…）
 * · /{id}/send、/{id}/void：必填 sentBy / voidedBy（原先缺失 → 必 400）
 */
const invoiceOps = {
  query(params: any) {
    return request.get('/erp/invoice/query', params)
  },
  statistics(params?: any) {
    return request.get('/erp/invoice/statistics', params)
  },
  /** 状态变更：newStatus 必须取 InvoiceStatus 枚举 name（后端无 ISSUED 值） */
  updateStatus(id: any, newStatus: string, notes?: string) {
    return request.put(`/erp/invoice/${id}/status`, null, { params: { newStatus, notes } })
  },
  send(id: any, sendMethod: string, sentBy: any) {
    return request.post(`/erp/invoice/${id}/send`, null, { params: { sendMethod, sentBy } })
  },
  voidInvoice(id: any, reason: string, voidedBy: any) {
    return request.post(`/erp/invoice/${id}/void`, null, { params: { reason, voidedBy } })
  },
}

// ═══ Tab（三个 Tab 各自独立：列定义 / 查询条件 / 功能按钮 / storage-key） ═══
const TABS = [
  { key: 'all', label: '全部发票' },
  { key: 'sales', label: '销售发票' },
  { key: 'purchase', label: '采购发票' },
]
const activeTab = ref('all')
/** Tab → 后端 direction 取值（后端按 equalsIgnoreCase 匹配 sales / purchase；空=全部类型） */
const DIRECTION_BY_TAB: Record<string, string | undefined> = {
  all: undefined,
  sales: 'sales',
  purchase: 'purchase',
}

// ═══ 字典（与后端 InvoiceStatus / PaymentStatus / InvoiceType 枚举逐字对齐） ═══
const STATUS_TEXT: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  IN_APPROVAL: '审批中',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  GENERATED: '已生成',
  SENT: '已发送',
  PARTIALLY_PAID: '部分支付',
  PAID: '已支付',
  OVERDUE: '逾期',
  CANCELLED: '已取消',
  CREDITED: '已冲红',
  VOIDED: '已作废',
}
const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'default',
  SUBMITTED: 'orange',
  IN_APPROVAL: 'processing',
  APPROVED: 'cyan',
  REJECTED: 'red',
  GENERATED: 'blue',
  SENT: 'cyan',
  PARTIALLY_PAID: 'geekblue',
  PAID: 'green',
  OVERDUE: 'red',
  CANCELLED: 'default',
  CREDITED: 'magenta',
  VOIDED: 'default',
}
const PAYMENT_STATUS_TEXT: Record<string, string> = {
  PENDING: '待付款',
  PARTIALLY_PAID: '部分付款',
  PAID: '已付款',
  OVERDUE: '逾期',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  PARTIALLY_REFUNDED: '部分退款',
  FAILED: '付款失败',
  PROCESSING: '付款处理中',
  MATCHED: '已匹配',
  PARTIALLY_MATCHED: '部分匹配',
  VOIDED: '已作废',
}
const PAYMENT_STATUS_COLOR: Record<string, string> = {
  PENDING: 'default',
  PARTIALLY_PAID: 'geekblue',
  PAID: 'green',
  OVERDUE: 'red',
  CANCELLED: 'default',
  REFUNDING: 'orange',
  REFUNDED: 'purple',
  PARTIALLY_REFUNDED: 'purple',
  FAILED: 'red',
  PROCESSING: 'processing',
  MATCHED: 'cyan',
  PARTIALLY_MATCHED: 'cyan',
  VOIDED: 'default',
}
const INVOICE_TYPE_TEXT: Record<string, string> = {
  SALES_INVOICE: '销售发票',
  PURCHASE_INVOICE: '采购发票',
  PROFORMA_INVOICE: '形式发票',
  TAX_INVOICE: '税务发票',
  CREDIT_NOTE: '红字发票',
  ELECTRONIC_INVOICE: '电子发票',
  REGULAR_INVOICE: '普通发票',
  SPECIAL_INVOICE: '专用发票',
  VEHICLE_INVOICE: '机动车发票',
  USED_VEHICLE_INVOICE: '二手车发票',
  EXPORT_INVOICE: '出口发票',
  IMPORT_INVOICE: '进口发票',
}
/** 发送方式：后端 send_method 为 varchar，取值域 EMAIL/SMS/POSTAL/PORTAL */
const SEND_METHOD_OPTIONS = [
  { label: '邮件', value: 'EMAIL' },
  { label: '短信', value: 'SMS' },
  { label: '邮寄', value: 'POSTAL' },
  { label: '门户', value: 'PORTAL' },
]

function getStatusText(status: any): string {
  if (status === null || status === undefined || status === '') return '-'
  return STATUS_TEXT[String(status)] || String(status)
}
function getStatusColor(status: any): string {
  return STATUS_COLOR[String(status)] || 'default'
}
function getPaymentStatusText(status: any): string {
  if (status === null || status === undefined || status === '') return '-'
  return PAYMENT_STATUS_TEXT[String(status)] || String(status)
}
function getPaymentStatusColor(status: any): string {
  return PAYMENT_STATUS_COLOR[String(status)] || 'default'
}
function getInvoiceTypeText(type: any): string {
  if (type === null || type === undefined || type === '') return '-'
  return INVOICE_TYPE_TEXT[String(type)] || String(type)
}
function getSendMethodText(method: any): string {
  if (method === null || method === undefined || method === '') return ''
  const matched = SEND_METHOD_OPTIONS.find(o => o.value === String(method).toUpperCase() || o.label === String(method))
  return matched ? matched.label : String(method)
}
function formatAmount(v: any): string {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
/** 后端 LocalDateTime 序列化为 ISO 串（2026-09-18T10:20:30），列表/详情统一显示到秒 */
function formatDateTime(v: any): string {
  if (!v) return ''
  return dayjs(v).isValid() ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : String(v)
}
/** PrintButton 的 documentNo 读 record.invoiceNo，而本实体字段是 invoiceNumber → 做一层适配 */
function printRecord(record: any): Record<string, any> {
  return { ...record, invoiceNo: record.invoiceNumber }
}

// ═══ 页面配置（按 Tab 独立一份；storage-key 也按 Tab 独立，避免切 Tab 沿用上一 Tab 配置） ═══
const TAB_QUERY_FIELDS: Record<string, QueryFieldSetting[]> = {
  all: [
    { key: 'invoiceNumber', label: '发票号码', visible: true },
    { key: 'partnerName', label: '往来单位', visible: true },
    { key: 'issuedByName', label: '开票人', visible: true },
    { key: 'invoiceDateRange', label: '开票日期', visible: true },
  ],
  sales: [
    { key: 'invoiceNumber', label: '发票号码', visible: true },
    { key: 'partnerName', label: '客户名称', visible: true },
    { key: 'issuedByName', label: '开票人', visible: true },
    { key: 'invoiceDateRange', label: '开票日期', visible: true },
  ],
  purchase: [
    { key: 'invoiceNumber', label: '发票号码', visible: true },
    { key: 'partnerName', label: '供应商名称', visible: true },
    { key: 'issuedByName', label: '开票人', visible: true },
    { key: 'invoiceDateRange', label: '开票日期', visible: true },
  ],
}
const TAB_FUNCTION_BUTTONS: Record<string, FunctionButtonSetting[]> = {
  all: [
    { key: 'create', label: '新建发票', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
  ],
  sales: [
    { key: 'create', label: '新建发票', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
  ],
  purchase: [
    { key: 'create', label: '新建发票', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
  ],
}
const showPageConfig = ref(false)
const queryFieldsConfigByTab = reactive<Record<string, QueryFieldSetting[]>>({
  all: TAB_QUERY_FIELDS.all.map(f => ({ ...f })),
  sales: TAB_QUERY_FIELDS.sales.map(f => ({ ...f })),
  purchase: TAB_QUERY_FIELDS.purchase.map(f => ({ ...f })),
})
const functionButtonsConfigByTab = reactive<Record<string, FunctionButtonSetting[]>>({
  all: TAB_FUNCTION_BUTTONS.all.map(b => ({ ...b })),
  sales: TAB_FUNCTION_BUTTONS.sales.map(b => ({ ...b })),
  purchase: TAB_FUNCTION_BUTTONS.purchase.map(b => ({ ...b })),
})
const activeQueryFields = computed(() => queryFieldsConfigByTab[activeTab.value] || TAB_QUERY_FIELDS[activeTab.value])
const activeFunctionButtons = computed(() => functionButtonsConfigByTab[activeTab.value] || TAB_FUNCTION_BUTTONS[activeTab.value])
const defaultQueryFields = computed(() => TAB_QUERY_FIELDS[activeTab.value])
const defaultFunctionButtons = computed(() => TAB_FUNCTION_BUTTONS[activeTab.value])
const tableStorageKey = computed(() => `crm-invoice-table-columns-${activeTab.value}`)
const pageConfigStorageKey = computed(() => `crm-invoice-page-config-${activeTab.value}`)

function isFieldVisible(key: string): boolean {
  const found = activeQueryFields.value.find(f => f.key === key)
  return found ? found.visible : true
}
function isButtonEnabled(key: string): boolean {
  const found = activeFunctionButtons.value.find(b => b.key === key)
  return found ? found.enabled : true
}
/** PageConfigPanel 的 change 回调按「当前 Tab」写入对应配置，切 Tab 互不覆盖 */
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfigByTab[activeTab.value] = config.queryFields
  if (config?.functionButtons) functionButtonsConfigByTab[activeTab.value] = config.functionButtons
}

// ═══ 列表状态 ═══
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  invoiceNumber: '',
  partnerName: '',
  issuedByName: '',
  invoiceDateRange: undefined as [string, string] | undefined,
})

const partnerLabel = computed(() => {
  if (activeTab.value === 'sales') return '客户名称'
  if (activeTab.value === 'purchase') return '供应商名称'
  return '往来单位'
})

const realRows = computed(() => tableData.value.filter((r: any) => !r.__ghost))

// ═══ 统计卡（口径统一为全量；/statistics 不可用时回落本页合计） ═══
const stats = reactive({ totalCount: 0, totalAmount: 0, totalPaid: 0, totalUnpaid: 0, overdueCount: 0, fromApi: false })
async function fetchStatistics() {
  try {
    const res: any = await invoiceOps.statistics()
    stats.totalCount = Number(res?.totalCount) || 0
    stats.totalAmount = Number(res?.totalAmount) || 0
    stats.totalPaid = Number(res?.totalPaid) || 0
    stats.totalUnpaid = Number(res?.totalUnpaid) || 0
    stats.overdueCount = Number(res?.overdueCount) || 0
    stats.fromApi = true
  } catch (err) {
    console.warn('[CRM发票] 统计接口不可用，回退为当前页合计', err)
    const rows = realRows.value
    const sum = (key: string) => rows.reduce((s: number, r: any) => s + (Number(r?.[key]) || 0), 0)
    stats.totalCount = pagination.total
    stats.totalAmount = sum('totalAmount')
    stats.totalPaid = sum('paidAmount')
    stats.totalUnpaid = sum('unpaidAmount')
    stats.overdueCount = rows.filter((r: any) => r.paymentStatus === 'OVERDUE').length
    stats.fromApi = false
  }
}

// ═══ 列定义（每个 Tab 独立一套；key 与 Invoice 实体字段逐字对齐） ═══
// ⚠️ formatter 是位置参数 (raw, record)，切勿写成 ({ row }) 对象解构（本模块原代码即犯此错）
function buildColumns(tab: string): DetailColumnConfig[] {
  const cols: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 260, fixed: 'left' },
    { key: 'invoiceNumber', title: '发票号码', type: 'input', width: 170 },
    { key: 'invoiceType', title: '发票类型', type: 'slot', slotName: 'invoiceTypeCell', width: 110 },
  ]
  if (tab === 'purchase') {
    cols.push({ key: 'supplierName', title: '供应商名称', type: 'input', width: 180 })
  } else if (tab === 'sales') {
    cols.push({ key: 'customerName', title: '客户名称', type: 'input', width: 180 })
  } else {
    cols.push({ key: 'customerName', title: '客户名称', type: 'input', width: 170 })
    cols.push({ key: 'supplierName', title: '供应商名称', type: 'input', width: 170 })
  }
  cols.push(
    { key: 'invoiceDate', title: '开票日期', type: 'input', width: 110 },
    { key: 'dueDate', title: '到期日', type: 'input', width: 110 },
    { key: 'subtotalAmount', title: '不含税金额', type: 'input', width: 120, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
    { key: 'taxAmount', title: '税额', type: 'input', width: 110, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
    { key: 'totalAmount', title: '价税合计', type: 'input', width: 130, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
    { key: 'paidAmount', title: '已付金额', type: 'input', width: 120, align: 'right', defaultHidden: true, formatter: (raw: any) => `¥${formatAmount(raw)}` },
    { key: 'unpaidAmount', title: '未付金额', type: 'input', width: 120, align: 'right', defaultHidden: true, formatter: (raw: any) => `¥${formatAmount(raw)}` },
    { key: 'invoiceStatus', title: '发票状态', type: 'slot', slotName: 'invoiceStatusCell', width: 100 },
    { key: 'paymentStatus', title: '付款状态', type: 'slot', slotName: 'paymentStatusCell', width: 100 },
    { key: 'issuedByName', title: '开票人', type: 'input', width: 100 },
    { key: 'issuedAt', title: '开票时间', type: 'input', width: 170, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
    { key: 'isCreditNote', title: '红冲票', type: 'slot', slotName: 'creditCell', width: 90, defaultHidden: true },
    { key: 'currencyCode', title: '币种', type: 'input', width: 80, defaultHidden: true },
    { key: 'orderNumber', title: '订单编号', type: 'input', width: 150, defaultHidden: true },
    { key: 'contractNumber', title: '合同编号', type: 'input', width: 150, defaultHidden: true },
    { key: 'deliveryNumber', title: '发货单号', type: 'input', width: 150, defaultHidden: true },
    { key: 'projectCode', title: '项目编号', type: 'input', width: 130, defaultHidden: true },
    { key: 'overdueDays', title: '逾期天数', type: 'input', width: 90, align: 'right', defaultHidden: true },
    { key: 'notes', title: '备注', type: 'input', width: 160, defaultHidden: true },
    { key: 'createdAt', title: '创建时间', type: 'input', width: 170, defaultHidden: true, formatter: (raw: any) => formatDateTime(raw) },
  )
  return cols
}
const columns = computed(() => buildColumns(activeTab.value))

/** 明细列（invoice_item 表字段名：item_name / specification / unit / quantity / unit_price / tax_rate / tax_amount / amount） */
const detailItemColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'lineNumber', title: '行号', type: 'input', width: 70 },
  { key: 'itemCode', title: '商品编码', type: 'input', width: 130 },
  { key: 'itemName', title: '商品名称', type: 'input', width: 180 },
  { key: 'specification', title: '规格型号', type: 'input', width: 130 },
  { key: 'unit', title: '单位', type: 'input', width: 80, align: 'center' },
  { key: 'quantity', title: '数量', type: 'input', width: 90, align: 'right' },
  { key: 'unitPrice', title: '单价', type: 'input', width: 110, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'taxRate', title: '税率%', type: 'input', width: 90, align: 'right' },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'itemAmountCell', width: 120, align: 'right' },
  { key: 'taxAmount', title: '税额', type: 'slot', slotName: 'itemTaxAmountCell', width: 120, align: 'right' },
]

/** 汇总行：组件契约是 { key, value }（原先只有 label/value → 每列 find 不到 → 合计行全空） */
const summaryColumns = computed(() => {
  const rows = realRows.value
  const sum = (key: string) => rows.reduce((s: number, r: any) => s + (Number(r?.[key]) || 0), 0)
  return [
    { key: 'rowNo', value: rows.length },
    { key: 'subtotalAmount', value: sum('subtotalAmount') },
    { key: 'taxAmount', value: sum('taxAmount') },
    { key: 'totalAmount', value: sum('totalAmount'), highlight: true },
    { key: 'paidAmount', value: sum('paidAmount') },
    { key: 'unpaidAmount', value: sum('unpaidAmount') },
  ]
})

// ═══ 行操作可用性（与后端守卫对齐：PUT /{id} 部分更新无状态守卫、POST /{id}/send 走 canSend()、POST /{id}/void 走 canVoid()） ═══
const TERMINAL_STATUSES = ['CANCELLED', 'CREDITED', 'VOIDED']
function statusOf(record: any): string {
  return String(record?.invoiceStatus || '')
}
function isEditable(record: any): boolean {
  // 后端 PUT /{id} 为部分更新且无状态守卫，前端只屏蔽终态与已删除票
  return !TERMINAL_STATUSES.includes(statusOf(record)) && !record?.deleted
}
function canIssue(record: any): boolean {
  return ['DRAFT', 'SUBMITTED', 'IN_APPROVAL', 'APPROVED', 'REJECTED'].includes(statusOf(record))
}
function canSend(record: any): boolean {
  // 对应 Invoice.canSend()：GENERATED 且未付清且非红冲
  return statusOf(record) === 'GENERATED'
    && record?.paymentStatus !== 'PAID'
    && !record?.isCreditNote
}
function canVoid(record: any): boolean {
  // 对应 Invoice.canVoid()：GENERATED 且 PENDING
  return statusOf(record) === 'GENERATED' && record?.paymentStatus === 'PENDING'
}
function canPrint(record: any): boolean {
  return !!record?.invoiceNumber
}

// ═══ 列表加载 ═══
async function fetchList() {
  loading.value = true
  hasError.value = false
  try {
    // Spring Data Pageable：page 从 0 开始 + size；原发 pageNum/pageSize 被后端忽略 → 恒第 1 页 20 条
    const params: any = {
      page: Math.max(pagination.current - 1, 0),
      size: pagination.pageSize,
    }
    const direction = DIRECTION_BY_TAB[activeTab.value]
    if (direction) params.direction = direction
    if (searchForm.invoiceNumber) params.invoiceNumber = searchForm.invoiceNumber
    if (searchForm.partnerName) params.partnerName = searchForm.partnerName
    if (searchForm.issuedByName) params.issuedByName = searchForm.issuedByName
    if (searchForm.invoiceDateRange?.[0]) params.startDate = searchForm.invoiceDateRange[0]
    if (searchForm.invoiceDateRange?.[1]) params.endDate = searchForm.invoiceDateRange[1]
    const res: any = await invoiceOps.query(params)
    // Result<Page<Invoice>> 被 request 拦截器解包为 Spring Page 对象
    tableData.value = res?.content || res?.records || []
    pagination.total = Number(res?.totalElements ?? res?.total ?? 0)
  } catch (err) {
    console.error('[CRM发票] 加载列表失败', err)
    hasError.value = true
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList().then(fetchStatistics)
}
function handleReset() {
  searchForm.invoiceNumber = ''
  searchForm.partnerName = ''
  searchForm.issuedByName = ''
  searchForm.invoiceDateRange = undefined
  pagination.current = 1
  fetchList().then(fetchStatistics)
}
function handleRefresh() {
  fetchList().then(fetchStatistics)
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}
/** 切 Tab：换 direction + 换列定义 + 换 storage-key + 回到第 1 页，并清空该 Tab 的查询条件 */
function handleTabChange(key: string) {
  if (!key || key === activeTab.value) return
  activeTab.value = key
  searchForm.invoiceNumber = ''
  searchForm.partnerName = ''
  searchForm.issuedByName = ''
  searchForm.invoiceDateRange = undefined
  pagination.current = 1
  if (showPageConfig.value) showPageConfig.value = false
  fetchList().then(fetchStatistics)
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref(false)
const detailData = ref<any>({})
const detailItems = ref<any[]>([])
const detailId = ref<any>(null)

async function fetchDetail(id: any) {
  if (!id) return
  detailId.value = id
  detailLoading.value = true
  detailError.value = false
  try {
    // 雪花/自增 ID 一律按字符串透传，不做 Number()
    const res: any = await invoiceApi.getById(String(id) as any)
    detailData.value = res || {}
    detailItems.value = Array.isArray(res?.items) ? res.items : []
  } catch (err) {
    console.error('[CRM发票] 加载详情失败', err)
    detailError.value = true
    detailData.value = {}
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}
function handleView(record: any) {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ═══ 新建 / 编辑：统一跳转 form.vue（原内嵌弹窗恒调 create，编辑会变成重复新建） ═══
function handleCreate() {
  router.push({ path: '/crm/invoice/form' })
}
function handleEdit(record: any) {
  detailVisible.value = false
  router.push({ path: '/crm/invoice/form', query: { id: String(record.id) } })
}

// ═══ 开具（PUT /{id}/status，newStatus 取枚举 name；前端无 ISSUED 值） ═══
function handleIssue(record: any) {
  Modal.confirm({
    title: '确认开具',
    content: `确定将发票「${record.invoiceNumber || record.id}」置为「已生成」吗？`,
    okText: '确认开具',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await invoiceOps.updateStatus(record.id, 'GENERATED', '前端开具')
        message.success('发票已开具（状态：已生成）')
        await fetchList()
        fetchStatistics()
        if (detailVisible.value) fetchDetail(detailId.value)
      } catch (err) {
        console.error('[CRM发票] 开具失败', err)
      }
    },
  })
}

// ═══ 发送（sendMethod + sentBy 必填） ═══
const sendVisible = ref(false)
const sendLoading = ref(false)
const sendMethod = ref('EMAIL')
const sendTarget = ref<any>({})

function handleSend(record: any) {
  sendTarget.value = record
  sendMethod.value = 'EMAIL'
  sendVisible.value = true
}
async function handleSendConfirm() {
  if (!sendMethod.value) {
    message.warning('请选择发送方式')
    return
  }
  // 后端 @RequestParam Long sentBy 必填，取当前登录用户（原先不传 → 必 400）
  const sentBy = userStore.userId
  if (!sentBy) {
    message.error('未获取到当前登录用户，无法发送')
    return
  }
  sendLoading.value = true
  try {
    await invoiceOps.send(sendTarget.value.id, sendMethod.value, sentBy)
    message.success('发票已发送')
    sendVisible.value = false
    await fetchList()
    fetchStatistics()
    if (detailVisible.value) fetchDetail(detailId.value)
  } catch (err) {
    console.error('[CRM发票] 发送失败', err)
  } finally {
    sendLoading.value = false
  }
}

// ═══ 作废（reason + voidedBy 必填） ═══
const voidVisible = ref(false)
const voidLoading = ref(false)
const voidReason = ref('')
const voidTarget = ref<any>({})

function handleCancelConfirm(record: any) {
  voidTarget.value = record
  voidReason.value = ''
  voidVisible.value = true
}
async function handleVoidConfirm() {
  const reason = voidReason.value.trim()
  if (!reason) {
    message.warning('请填写作废原因（后端必填）')
    return
  }
  const voidedBy = userStore.userId
  if (!voidedBy) {
    message.error('未获取到当前登录用户，无法作废')
    return
  }
  voidLoading.value = true
  try {
    await invoiceOps.voidInvoice(voidTarget.value.id, reason, voidedBy)
    message.success('发票已作废')
    voidVisible.value = false
    await fetchList()
    fetchStatistics()
    if (detailVisible.value) fetchDetail(detailId.value)
  } catch (err) {
    console.error('[CRM发票] 作废失败', err)
  } finally {
    voidLoading.value = false
  }
}

// ═══ 打印(F8) / 导出 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = realRows.value
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.invoiceNumber || '')}</td>
      <td>${escapeHtml(getInvoiceTypeText(r.invoiceType))}</td>
      <td>${escapeHtml(r.customerName || r.supplierName || '')}</td>
      <td>${escapeHtml(r.invoiceDate || '')}</td>
      <td>${escapeHtml(getStatusText(r.invoiceStatus))}</td>
      <td>${escapeHtml(getPaymentStatusText(r.paymentStatus))}</td>
      <td style="text-align:right">¥${formatAmount(r.totalAmount)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>发票台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>发票台账（${escapeHtml(TABS.find(t => t.key === activeTab.value)?.label || '')}）</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>发票号码</th><th>发票类型</th><th>客户/供应商</th>
      <th>开票日期</th><th>发票状态</th><th>付款状态</th><th>价税合计</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=700')
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
  const rows = realRows.value
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['发票号码', '发票类型', '客户名称', '供应商名称', '开票日期', '到期日',
    '不含税金额', '税额', '价税合计', '已付金额', '未付金额', '发票状态', '付款状态',
    '开票人', '订单编号', '合同编号', '备注']
  const body = rows.map((r: any) => [
    r.invoiceNumber, getInvoiceTypeText(r.invoiceType), r.customerName, r.supplierName,
    r.invoiceDate, r.dueDate, formatAmount(r.subtotalAmount), formatAmount(r.taxAmount),
    formatAmount(r.totalAmount), formatAmount(r.paidAmount), formatAmount(r.unpaidAmount),
    getStatusText(r.invoiceStatus), getPaymentStatusText(r.paymentStatus),
    r.issuedByName, r.orderNumber, r.contractNumber, r.notes,
  ])
  exportCsv(headers, body, `发票台账_${dayjs().format('YYYYMMDD_HHmmss')}`)
}

// ═══ 快捷键 / 跨页事件 / 自动刷新（弹窗打开时暂停轮询） ═══
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function anyOverlayOpen(): boolean {
  return detailVisible.value || sendVisible.value || voidVisible.value || showPageConfig.value
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    handleRefresh()
    return
  }
  if (e.ctrlKey && e.key.toLowerCase() === 'n') {
    e.preventDefault()
    handleCreate()
  }
}
function handleParentCreate() {
  handleCreate()
}
function handleError(error: Error) {
  console.error('[CRM发票] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  fetchList().then(fetchStatistics)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (!anyOverlayOpen()) fetchList()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, handleRefresh as any)
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('keydown', handleF8Key)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, handleRefresh as any)
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('keydown', handleF8Key)
})

defineExpose({ handleQuery: fetchList })
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-error { flex-shrink: 0; margin: 8px 0; }
.detail-items { max-height: 360px; overflow: auto; }
.modal-tip { margin-bottom: 12px; color: #666; }

.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}
.amount-red { color: #f5222d; font-weight: 500; }

.stats-cards { flex-shrink: 0; margin-bottom: 12px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12); transform: translateY(-2px); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  margin-right: 12px;
}
.stat-content { flex: 1; }
.stat-title { font-size: 13px; color: #666; }
.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.stat-desc { font-size: 12px; color: #999; }
.stat-desc.positive { color: #52c41a; }

/* 橙色新增按钮（CRM 模块统一） */
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
