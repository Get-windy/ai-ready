<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :tabs="TABS"
        :active-tab="activeTab"
        :show-table-footer="activeTab === 'ledger' || activeTab === 'unpaid'"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：收款码服务状态 + 资金口径 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-tag :color="dict.qrcodeEnabled ? 'green' : 'orange'">
              {{ dict.qrcodeEnabled ? '收款码服务已开通' : '收款码服务未开通（仅支持现金/POS 等线下收款）' }}
            </a-tag>
            <span class="rule-hint">
              代收货款（负债，需上交）与配送费（收入）分科目记账
              <template v-if="Number(dict.handoverDeadlineHours) > 0">
                ｜交款时限 {{ dict.handoverDeadlineHours }} 小时
              </template>
              <template v-if="Number(dict.cashLimitPerOrder) > 0">
                ｜单笔现金限额 ¥{{ fmtMoney(dict.cashLimitPerOrder) }}
              </template>
            </span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <template v-if="activeTab === 'ledger'">
              <a-button
                v-if="isButtonEnabled('batchConfirm')"
                size="small"
                :disabled="!selectedRows.length"
                @click="openBatchConfirm"
              >
                <CheckCircleOutlined /> 批量确认（{{ selectedRows.length }}）
              </a-button>
              <a-button
                v-if="isButtonEnabled('pushFinance')"
                size="small"
                :loading="pushing"
                :disabled="!selectedRows.length"
                @click="handlePushFinance"
              >
                <SendOutlined /> 推送财务（{{ selectedRows.length }}）
              </a-button>
              <a-button
                v-if="isButtonEnabled('export')"
                size="small"
                @click="handleExport"
              >
                <ExportOutlined /> 导出
              </a-button>
            </template>
            <template v-else-if="activeTab === 'flows'">
              <a-button
                v-if="isButtonEnabled('flowImport')"
                size="small"
                @click="showFlowImport = true"
              >
                <UploadOutlined /> 导入流水
              </a-button>
              <a-button
                v-if="isButtonEnabled('reconcile')"
                size="small"
                type="primary"
                ghost
                @click="showReconcile = true"
              >
                <SwapOutlined /> 对账
              </a-button>
            </template>
            <a-button
              size="small"
              :loading="loading"
              @click="refreshCurrent"
            >
              <ReloadOutlined /> 刷新
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

        <!-- ═══ 查询区（横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <!-- 收款台账 -->
              <template v-if="activeTab === 'ledger'">
                <a-input
                  v-if="isQueryVisible('keyword')"
                  v-model:value="ledgerQuery.keyword"
                  size="small"
                  placeholder="任务编号/客户/配送员/交易号"
                  allow-clear
                  style="width: 200px"
                  @press-enter="handleLedgerSearch"
                />
                <a-select
                  v-if="isQueryVisible('paymentType')"
                  v-model:value="ledgerQuery.paymentType"
                  size="small"
                  placeholder="收款类型"
                  allow-clear
                  :options="paymentTypeOptions"
                  style="width: 120px"
                />
                <a-select
                  v-if="isQueryVisible('payChannel')"
                  v-model:value="ledgerQuery.payChannel"
                  size="small"
                  placeholder="支付方式"
                  allow-clear
                  :options="payChannelOptions"
                  style="width: 120px"
                />
                <a-select
                  v-if="isQueryVisible('status') && !ledgerQuery.overdueOnly"
                  v-model:value="ledgerQuery.status"
                  size="small"
                  placeholder="支付状态"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  style="width: 120px"
                />
                <a-select
                  v-if="isQueryVisible('handoverStatus') && !ledgerQuery.overdueOnly"
                  v-model:value="ledgerQuery.handoverStatus"
                  size="small"
                  placeholder="交款状态"
                  allow-clear
                  :options="HANDOVER_OPTIONS"
                  style="width: 120px"
                />
                <a-range-picker
                  v-if="isQueryVisible('dateRange')"
                  v-model:value="ledgerRange"
                  size="small"
                  style="width: 230px"
                  :placeholder="['日期起', '日期止']"
                />
                <a-checkbox
                  v-if="isQueryVisible('overdueOnly')"
                  v-model:checked="ledgerQuery.overdueOnly"
                  @change="handleOverdueToggle"
                >
                  仅看交款超时
                </a-checkbox>
                <a-button
                  type="primary"
                  size="small"
                  @click="handleLedgerSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleLedgerReset"
                >
                  重置
                </a-button>
              </template>

              <!-- 未付管理 -->
              <template v-else-if="activeTab === 'unpaid'">
                <a-input
                  v-model:value="unpaidQuery.taskNo"
                  size="small"
                  placeholder="任务编号"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleUnpaidSearch"
                />
                <a-input
                  v-model:value="unpaidQuery.customerName"
                  size="small"
                  placeholder="客户"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleUnpaidSearch"
                />
                <a-range-picker
                  v-model:value="unpaidRange"
                  size="small"
                  style="width: 230px"
                  :placeholder="['日期起', '日期止']"
                />
                <a-button
                  type="primary"
                  size="small"
                  @click="handleUnpaidSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleUnpaidReset"
                >
                  重置
                </a-button>
              </template>

              <!-- 交款稽核 -->
              <template v-else-if="activeTab === 'handover'">
                <a-range-picker
                  v-model:value="handoverRange"
                  size="small"
                  style="width: 250px"
                  :placeholder="['日期起', '日期止']"
                />
                <a-button
                  type="primary"
                  size="small"
                  @click="fetchHandover"
                >
                  查询稽核
                </a-button>
              </template>

              <!-- 支付流水 -->
              <template v-else>
                <a-input
                  v-model:value="flowQuery.channelCode"
                  size="small"
                  placeholder="渠道编码（WECHAT/ALIPAY/POS）"
                  allow-clear
                  style="width: 200px"
                  @press-enter="handleFlowSearch"
                />
                <a-input
                  v-model:value="flowQuery.tradeNo"
                  size="small"
                  placeholder="交易号/商户单号"
                  allow-clear
                  style="width: 180px"
                  @press-enter="handleFlowSearch"
                />
                <a-select
                  v-model:value="flowQuery.matchStatus"
                  size="small"
                  placeholder="匹配状态"
                  allow-clear
                  :options="MATCH_OPTIONS"
                  style="width: 130px"
                />
                <a-range-picker
                  v-model:value="flowRange"
                  size="small"
                  style="width: 230px"
                  :placeholder="['日期起', '日期止']"
                />
                <a-button
                  type="primary"
                  size="small"
                  @click="handleFlowSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleFlowReset"
                >
                  重置
                </a-button>
              </template>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区 ═══ -->
        <template #table>
          <!-- 1. 收款台账 -->
          <BillTableList
            v-if="activeTab === 'ledger'"
            :columns="columns"
            :data-source="tableData"
            storage-key="dms-payment-columns"
            global-config-key="dms-payment-columns"
            :loading="loading"
            :pagination="false"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="true"
            row-key="id"
            @selection-change="handleSelectionChange"
          >
            <template #assetCell="{ record }">
              <span v-if="!record.__ghost">
                <a-tag
                  :color="record.paymentType === 1 ? 'orange' : 'blue'"
                  :bordered="false"
                >
                  {{ record.paymentType === 1 ? '代收货款' : '配送费' }}
                </a-tag>
              </span>
            </template>
            <template #overdueCell="{ record }">
              <span v-if="!record.__ghost">
                <a-tag
                  v-if="record.overdue"
                  color="red"
                >
                  超 {{ record.overdueHours }}h 未交
                </a-tag>
                <span v-else>-</span>
              </span>
            </template>
            <template #actionCell="{ record }">
              <a-space
                v-if="!record.__ghost"
                :size="2"
              >
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  @click="openQrcode(record)"
                >
                  收款码
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  @click="openConfirm(record)"
                >
                  确认收款
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  @click="openUnpaid(record)"
                >
                  标记未付
                </a-button>
                <a-button
                  v-if="record.status === 1 && record.handoverStatus !== 2"
                  type="link"
                  size="small"
                  @click="openHandover(record)"
                >
                  交款
                </a-button>
                <a-button
                  v-if="record.status === 3"
                  type="link"
                  size="small"
                  @click="openWriteOff(record)"
                >
                  核销
                </a-button>
                <a-button
                  v-if="record.status === 1 && record.financePushStatus !== 1"
                  type="link"
                  size="small"
                  @click="handlePushFinanceOne(record)"
                >
                  推财务
                </a-button>
              </a-space>
            </template>
          </BillTableList>

          <!-- 2. 未付管理（挂账 → 催收 → 核销） -->
          <BillTableList
            v-else-if="activeTab === 'unpaid'"
            :columns="unpaidColumns"
            :data-source="unpaidData"
            storage-key="dms-payment-unpaid-columns"
            global-config-key="dms-payment-unpaid-columns"
            :loading="loading"
            :pagination="false"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
          >
            <template #promiseCell="{ record }">
              <span
                v-if="!record.__ghost"
                :class="isPromiseOverdue(record) ? 'danger-text' : ''"
              >
                {{ record.promiseDate || '-' }}
                <span v-if="isPromiseOverdue(record)">（已逾期）</span>
              </span>
            </template>
            <template #actionCell="{ record }">
              <a-space
                v-if="!record.__ghost"
                :size="2"
              >
                <a-button
                  type="link"
                  size="small"
                  @click="openUrge(record)"
                >
                  催收
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="openWriteOff(record)"
                >
                  核销
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="openCollections(record)"
                >
                  催收记录
                </a-button>
              </a-space>
            </template>
          </BillTableList>

          <!-- 3. 交款稽核 -->
          <div
            v-else-if="activeTab === 'handover'"
            class="report-wrap"
          >
            <a-spin :spinning="loading">
              <a-row :gutter="16">
                <a-col :span="6">
                  <a-statistic
                    title="应上交合计"
                    :value="fmtMoney(handover.dueTotal)"
                    prefix="¥"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="已上交合计"
                    :value="fmtMoney(handover.handoverTotal)"
                    prefix="¥"
                    :value-style="{ color: '#3f8600' }"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="未上交合计"
                    :value="fmtMoney(handover.unhandoverTotal)"
                    prefix="¥"
                    :value-style="{ color: Number(handover.unhandoverTotal) > 0 ? '#cf1322' : undefined }"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="超时限未交笔数"
                    :value="handover.overdueCount || 0"
                    :value-style="{ color: Number(handover.overdueCount) > 0 ? '#cf1322' : undefined }"
                    :suffix="`笔（时限 ${handover.deadlineHours || 0}h）`"
                  />
                </a-col>
              </a-row>

              <div class="report-section">
                <div class="report-title">
                  按配送员稽核（资金安全核心）
                </div>
                <a-table
                  :columns="handoverColumns"
                  :data-source="handover.riders || []"
                  :pagination="false"
                  size="small"
                  row-key="riderId"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="['dueAmount', 'handoverAmount', 'diffAmount'].includes(String(column.key))">
                      <span :style="{ color: column.key === 'diffAmount' && Number(record.diffAmount) > 0 ? '#cf1322' : undefined }">
                        ¥{{ fmtMoney(record[column.dataIndex as string]) }}
                      </span>
                    </template>
                    <template v-else-if="column.key === 'overdueCount'">
                      <a-tag
                        v-if="Number(record.overdueCount) > 0"
                        color="red"
                      >
                        {{ record.overdueCount }}
                      </a-tag>
                      <span v-else>0</span>
                    </template>
                  </template>
                </a-table>
              </div>

              <div
                v-if="(handover.overdueList || []).length"
                class="report-section"
              >
                <div class="report-title danger-text">
                  超时限未交明细（应追责/预警）
                </div>
                <a-table
                  :columns="overdueColumns"
                  :data-source="handover.overdueList"
                  :pagination="false"
                  size="small"
                  row-key="paymentId"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'amount'">
                      ¥{{ fmtMoney(record.amount) }}
                    </template>
                    <template v-else-if="column.key === 'overdueHours'">
                      <span class="danger-text">{{ record.overdueHours }} 小时</span>
                    </template>
                  </template>
                </a-table>
              </div>
            </a-spin>
          </div>

          <!-- 4. 支付流水 / 对账 -->
          <div
            v-else
            class="report-wrap"
          >
            <a-spin :spinning="loading">
              <a-row :gutter="16">
                <a-col :span="6">
                  <a-statistic
                    title="流水笔数"
                    :value="flowStatData.total || 0"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="已匹配"
                    :value="flowStatData.matched || 0"
                    :value-style="{ color: '#3f8600' }"
                    :suffix="`笔 / ¥${fmtMoney(flowStatData.matchedAmount)}`"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="未匹配"
                    :value="flowStatData.unmatched || 0"
                    :value-style="{ color: Number(flowStatData.unmatched) > 0 ? '#fa8c16' : undefined }"
                  />
                </a-col>
                <a-col :span="6">
                  <a-statistic
                    title="差异"
                    :value="flowStatData.diff || 0"
                    :value-style="{ color: Number(flowStatData.diff) > 0 ? '#cf1322' : undefined }"
                    :suffix="`笔 / ¥${fmtMoney(flowStatData.diffAmount)}`"
                  />
                </a-col>
              </a-row>

              <a-table
                class="flow-table"
                :columns="flowColumns"
                :data-source="flowData"
                :pagination="false"
                size="small"
                row-key="id"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'matchStatus'">
                    <a-tag :color="MATCH_MAP[record.matchStatus]?.color">
                      {{ MATCH_MAP[record.matchStatus]?.label || '-' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'amount'">
                    ¥{{ fmtMoney(record.amount) }}
                  </template>
                  <template v-else-if="column.key === 'tradeTime'">
                    {{ fmtDateTime(record.tradeTime) }}
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-space :size="2">
                      <a-button
                        v-if="record.matchStatus !== 1"
                        type="link"
                        size="small"
                        @click="openFlowMatch(record)"
                      >
                        人工匹配
                      </a-button>
                      <a-button
                        v-if="record.matchStatus !== 3"
                        type="link"
                        size="small"
                        @click="handleFlowIgnore(record)"
                      >
                        忽略
                      </a-button>
                    </a-space>
                  </template>
                </template>
              </a-table>
              <StandardPagination
                variant="classic"
                :current="flowPage.current"
                :page-size="flowPage.pageSize"
                :total="flowPage.total"
                :page-size-options="[20, 50, 100]"
                @change="handleFlowPageChange"
              />
            </a-spin>
          </div>
        </template>

        <!-- ═══ 底部：汇总 + 经典分页 ═══ -->
        <template #table-footer>
          <StandardPagination
            v-if="activeTab === 'ledger'"
            variant="classic"
            :current="ledgerPage.current"
            :page-size="ledgerPage.pageSize"
            :total="ledgerPage.total"
            :page-size-options="[20, 50, 100]"
            @change="handleLedgerPageChange"
          />
          <StandardPagination
            v-else
            variant="classic"
            :current="unpaidPage.current"
            :page-size="unpaidPage.pageSize"
            :total="unpaidPage.total"
            :page-size-options="[20, 50, 100]"
            @change="handleUnpaidPageChange"
          />
          <div class="table-footer">
            <template v-if="activeTab === 'ledger'">
              <div class="footer-label">
                汇总（本页 {{ tableData.length }} 条 ｜ 已收 {{ ledgerStat.paidCount || 0 }} 笔）
              </div>
              <div class="footer-values">
                <span>已收金额: ¥{{ fmtMoney(ledgerStat.paidAmount) }}</span>
                <span>代收货款: ¥{{ fmtMoney(ledgerStat.codAmount) }}</span>
                <span>配送费: ¥{{ fmtMoney(ledgerStat.deliveryFeeAmount) }}</span>
                <span>未付(挂账): ¥{{ fmtMoney(ledgerStat.unpaidAmount) }}（{{ ledgerStat.unpaidCount || 0 }} 笔）</span>
                <span :style="{ color: Number(ledgerStat.unhandoverAmount) > 0 ? '#cf1322' : undefined }">
                  待上交: ¥{{ fmtMoney(ledgerStat.unhandoverAmount) }}
                </span>
                <span :style="{ color: Number(ledgerStat.overdueCount) > 0 ? '#cf1322' : undefined }">
                  超时未交: {{ ledgerStat.overdueCount || 0 }} 笔 / ¥{{ fmtMoney(ledgerStat.overdueAmount) }}
                </span>
              </div>
            </template>
            <template v-else>
              <div class="footer-label">
                挂账汇总（{{ unpaidStatData.unpaidCount || 0 }} 笔未付）
              </div>
              <div class="footer-values">
                <span>未付金额: ¥{{ fmtMoney(unpaidStatData.unpaidAmount) }}</span>
                <span>已催收: {{ unpaidStatData.urgedCount || 0 }} 笔</span>
                <span>未催收: {{ unpaidStatData.notUrgedCount || 0 }} 笔</span>
                <span>承诺付款: {{ unpaidStatData.promisedCount || 0 }} 笔</span>
                <span>已核销: ¥{{ fmtMoney(unpaidStatData.writeOffAmount) }}</span>
              </div>
            </template>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 收款码 ═══ -->
    <a-modal
      v-model:open="showQrcode"
      title="收款码"
      :width="460"
      :footer="null"
    >
      <div class="qrcode-box">
        <template v-if="qrcodeResult.qrcodeUrl">
          <img
            :src="qrcodeResult.qrcodeUrl"
            alt="收款码"
            class="qrcode-img"
          >
          <div class="qrcode-link">
            {{ qrcodeResult.qrcodeUrl }}
          </div>
        </template>
        <a-alert
          v-else
          type="warning"
          show-icon
          message="未开通扫码收款"
          description="未配置收款码服务地址（配送参数 dms.payment.qrcode.base-url），请使用现金 / POS 等线下方式收款后点「确认收款」。系统不会生成不可支付的示例二维码。"
        />
      </div>
    </a-modal>

    <!-- ═══ 确认收款（线下） ═══ -->
    <a-modal
      v-model:open="showConfirm"
      title="确认收款"
      :width="480"
      ok-text="确认"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitConfirm"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-form-item label="收款类型">
          <a-select
            v-model:value="confirmForm.paymentType"
            :options="paymentTypeOptions"
          />
        </a-form-item>
        <a-form-item label="支付方式">
          <a-select
            v-model:value="confirmForm.payChannel"
            :options="payChannelOptions"
          />
        </a-form-item>
        <a-form-item
          label="实收金额"
          required
        >
          <a-input-number
            v-model:value="confirmForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="外部单号（POS 流水等）">
          <a-input v-model:value="confirmForm.externalOrderNo" />
        </a-form-item>
        <a-alert
          v-if="Number(dict.cashLimitPerOrder) > 0"
          type="info"
          show-icon
          :message="`单笔现金限额 ¥${fmtMoney(dict.cashLimitPerOrder)}（超限请改用扫码/POS）`"
        />
      </a-form>
    </a-modal>

    <!-- ═══ 批量确认（线下） ═══ -->
    <a-modal
      v-model:open="showBatchConfirm"
      title="批量确认收款（线下）"
      :width="620"
      ok-text="批量确认"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitBatchConfirm"
    >
      <a-alert
        type="info"
        show-icon
        message="按各单应收金额确认收款；如需改金额请逐单「确认收款」。"
        style="margin-bottom: 10px"
      />
      <a-form
        layout="inline"
        size="small"
        style="margin-bottom: 10px"
      >
        <a-form-item label="统一支付方式">
          <a-select
            v-model:value="batchConfirmForm.payChannel"
            :options="payChannelOptions"
            style="width: 150px"
          />
        </a-form-item>
      </a-form>
      <a-table
        :columns="batchConfirmColumns"
        :data-source="selectedRows"
        :pagination="false"
        size="small"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            ¥{{ fmtMoney(record.amount) }}
          </template>
          <template v-else-if="column.key === 'paymentType'">
            {{ record.paymentType === 1 ? '代收货款' : '配送费' }}
          </template>
        </template>
      </a-table>
      <div
        v-if="batchConfirmResult"
        class="result-tip"
      >
        <a-alert
          :type="batchConfirmResult.failedCount ? 'warning' : 'success'"
          show-icon
          :message="`成功 ${batchConfirmResult.success} 条，失败 ${batchConfirmResult.failedCount} 条`"
          :description="(batchConfirmResult.failed || []).map((f: any) => `#${f.taskId}: ${f.reason}`).join('；')"
        />
      </div>
    </a-modal>

    <!-- ═══ 标记未付 ═══ -->
    <a-modal
      v-model:open="showUnpaid"
      title="标记未付（挂账）"
      :width="460"
      ok-text="确定"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitUnpaid"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-form-item
          label="未付原因"
          required
        >
          <a-textarea
            v-model:value="unpaidRemark"
            :rows="3"
            placeholder="必填：客户拒付 / 协商延期 / 其他"
          />
        </a-form-item>
        <div class="modal-tip">
          挂账后进入「未付管理」Tab，可催收、登记承诺付款日并核销收回。
        </div>
      </a-form>
    </a-modal>

    <!-- ═══ 交款登记 ═══ -->
    <a-modal
      v-model:open="showHandover"
      title="交款登记"
      :width="460"
      ok-text="登记"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitHandover"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-descriptions
          :column="1"
          size="small"
        >
          <a-descriptions-item label="任务编号">
            {{ currentRecord.taskNo || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="应收金额">
            ¥{{ fmtMoney(currentRecord.amount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已上交">
            ¥{{ fmtMoney(currentRecord.handoverAmount) }}
          </a-descriptions-item>
        </a-descriptions>
        <a-form-item
          label="本次交款金额"
          required
          style="margin-top: 12px"
        >
          <a-input-number
            v-model:value="handoverForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="handoverForm.remark" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 催收登记 ═══ -->
    <a-modal
      v-model:open="showUrge"
      title="催收登记"
      :width="480"
      ok-text="登记"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitUrge"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-descriptions
          :column="1"
          size="small"
        >
          <a-descriptions-item label="任务编号">
            {{ currentRecord.taskNo || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="未付金额">
            <span class="danger-text">¥{{ fmtMoney(currentRecord.amount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="累计催收">
            {{ currentRecord.urgeCount || 0 }} 次
          </a-descriptions-item>
        </a-descriptions>
        <a-form-item
          label="催收说明"
          required
          style="margin-top: 12px"
        >
          <a-textarea
            v-model:value="urgeForm.content"
            :rows="2"
            :maxlength="200"
            placeholder="必填：电话催收 / 上门催收 / 客户答复…"
          />
        </a-form-item>
        <a-form-item label="客户承诺付款日（可选）">
          <a-date-picker
            v-model:value="urgeForm.promiseDate"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 核销（挂账收回） ═══ -->
    <a-modal
      v-model:open="showWriteOff"
      title="核销（挂账收回）"
      :width="480"
      ok-text="确认核销"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitWriteOff"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-descriptions
          :column="1"
          size="small"
        >
          <a-descriptions-item label="任务编号">
            {{ currentRecord.taskNo || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="应收金额">
            ¥{{ fmtMoney(currentRecord.amount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已核销">
            ¥{{ fmtMoney(currentRecord.writeOffAmount) }}
          </a-descriptions-item>
        </a-descriptions>
        <a-form-item
          label="本次收回金额"
          required
          style="margin-top: 12px"
        >
          <a-input-number
            v-model:value="writeOffForm.amount"
            :min="0"
            :precision="2"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="收款方式">
          <a-select
            v-model:value="writeOffForm.payChannel"
            :options="payChannelOptions"
          />
        </a-form-item>
        <a-form-item label="说明">
          <a-input
            v-model:value="writeOffForm.content"
            placeholder="如：客户微信转账到公司账户"
          />
        </a-form-item>
        <div class="modal-tip">
          累计收回达到应收后，单据自动置为「已支付」并进入交款稽核。
        </div>
      </a-form>
    </a-modal>

    <!-- ═══ 催收/核销记录 ═══ -->
    <a-modal
      v-model:open="showCollections"
      title="挂账动作记录"
      :width="640"
      :footer="null"
    >
      <a-table
        :columns="collectionColumns"
        :data-source="collectionList"
        :pagination="false"
        size="small"
        row-key="id"
        :loading="collectionsLoading"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'actionType'">
            <a-tag :color="record.actionType === 1 ? 'orange' : record.actionType === 2 ? 'green' : 'blue'">
              {{ ACTION_TEXT[record.actionType] || '-' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ record.amount != null ? `¥${fmtMoney(record.amount)}` : '-' }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ fmtDateTime(record.createTime) }}
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- ═══ 流水导入 ═══ -->
    <a-modal
      v-model:open="showFlowImport"
      title="导入支付平台流水"
      :width="700"
      ok-text="导入"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitFlowImport"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-form-item label="默认渠道编码（行内未给时使用）">
          <a-input
            v-model:value="flowImportForm.defaultChannel"
            placeholder="WECHAT / ALIPAY / POS / BANK"
            style="width: 240px"
          />
        </a-form-item>
        <a-form-item label="流水数据（CSV，每行：渠道,平台交易号,商户单号,金额,成交时间,付款人）">
          <a-textarea
            v-model:value="flowImportForm.text"
            :rows="8"
            placeholder="WECHAT,4200001234567890,PSD-20260913-001,200.00,2026-09-13 10:20:00,张三"
          />
        </a-form-item>
        <div class="modal-tip">
          同一渠道 + 平台交易号的流水重复导入会自动跳过（幂等）；导入后点「对账」逐笔核对。
        </div>
        <a-alert
          v-if="flowImportResult"
          style="margin-top: 8px"
          :type="flowImportResult.failedCount ? 'warning' : 'success'"
          show-icon
          :message="`共 ${flowImportResult.total} 行：入库 ${flowImportResult.inserted}，跳过 ${flowImportResult.skipped}，失败 ${flowImportResult.failedCount}`"
          :description="(flowImportResult.failed || []).map((f: any) => `第${f.row}行: ${f.reason}`).join('；')"
        />
      </a-form>
    </a-modal>

    <!-- ═══ 对账 ═══ -->
    <a-modal
      v-model:open="showReconcile"
      title="与支付平台流水对账"
      :width="820"
      ok-text="开始对账"
      cancel-text="关闭"
      :confirm-loading="saving"
      @ok="submitReconcile"
    >
      <a-form
        layout="inline"
        size="small"
        style="margin-bottom: 12px"
      >
        <a-form-item label="日期范围">
          <a-range-picker
            v-model:value="reconcileForm.range"
            style="width: 240px"
          />
        </a-form-item>
        <a-form-item label="渠道">
          <a-input
            v-model:value="reconcileForm.channelCode"
            placeholder="全部"
            allow-clear
            style="width: 150px"
          />
        </a-form-item>
      </a-form>
      <template v-if="reconcileResult">
        <a-row :gutter="12">
          <a-col :span="4">
            <a-statistic
              title="流水"
              :value="reconcileResult.flowTotal || 0"
            />
          </a-col>
          <a-col :span="4">
            <a-statistic
              title="已匹配"
              :value="reconcileResult.matched || 0"
              :value-style="{ color: '#3f8600' }"
            />
          </a-col>
          <a-col :span="4">
            <a-statistic
              title="系统无此笔"
              :value="reconcileResult.flowOnly || 0"
              :value-style="{ color: Number(reconcileResult.flowOnly) > 0 ? '#cf1322' : undefined }"
            />
          </a-col>
          <a-col :span="4">
            <a-statistic
              title="金额不符"
              :value="reconcileResult.amountMismatch || 0"
              :value-style="{ color: Number(reconcileResult.amountMismatch) > 0 ? '#cf1322' : undefined }"
            />
          </a-col>
          <a-col :span="4">
            <a-statistic
              title="掉单"
              :value="reconcileResult.systemOnly || 0"
              :value-style="{ color: Number(reconcileResult.systemOnly) > 0 ? '#cf1322' : undefined }"
            />
          </a-col>
          <a-col :span="4">
            <a-statistic
              title="差异合计"
              :value="reconcileResult.diffTotal || 0"
            />
          </a-col>
        </a-row>
        <a-table
          class="flow-table"
          :columns="reconcileColumns"
          :data-source="reconcileResult.details || []"
          :pagination="{ pageSize: 10 }"
          size="small"
          row-key="flowId"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <a-tag :color="DIFF_MAP[record.type]?.color">
                {{ DIFF_MAP[record.type]?.label || record.type }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </template>
      <a-empty
        v-else
        description="选择日期范围后点「开始对账」"
      />
    </a-modal>

    <!-- ═══ 流水人工匹配 ═══ -->
    <a-modal
      v-model:open="showFlowMatch"
      title="流水人工匹配"
      :width="480"
      ok-text="匹配"
      cancel-text="取消"
      :confirm-loading="saving"
      @ok="submitFlowMatch"
    >
      <a-descriptions
        :column="1"
        size="small"
      >
        <a-descriptions-item label="平台交易号">
          {{ currentFlow.tradeNo || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="商户单号">
          {{ currentFlow.outTradeNo || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="流水金额">
          ¥{{ fmtMoney(currentFlow.amount) }}
        </a-descriptions-item>
      </a-descriptions>
      <a-form
        layout="vertical"
        size="small"
        style="margin-top: 12px"
      >
        <a-form-item
          label="匹配收款记录ID"
          required
        >
          <a-input-number
            v-model:value="flowMatchForm.paymentId"
            :min="1"
            style="width: 100%"
            placeholder="dms_payment.id"
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
      storage-key="dms-payment-page-config"
      hide-print-config
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 收款管理（配送 → 结算收款 → 收款管理，菜单 80920）
 *
 * 金标准口径（《收款管理开发文档》§3）：
 *   · 四 Tab：**收款台账 / 未付管理 / 交款稽核 / 支付流水**；
 *   · 收款类型（代收货款=负债 / 配送费=收入）与支付方式分离，**分科目记账**；
 *   · 收款码**配置化**（未开通不生成二维码，不返回示例 URL）；
 *   · 支付回调**幂等**（tradeNo）；资金上交/稽核（应上交 vs 已上交 + **交款时限超时预警**）；
 *   · **未付闭环**：挂账 → 催收 → 承诺付款 → 核销；
 *   · **支付流水对账**：导入 → 逐笔匹配（系统无此笔 / 金额不符 / 掉单三类差异）；
 *   · **推送财务**幂等（生成收款单 / 核销应收）；现金**单笔/单日限额**守资金安全。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import * as XLSX from 'xlsx'
import {
  ReloadOutlined, ExportOutlined, SettingOutlined, UploadOutlined,
  CheckCircleOutlined, SendOutlined, SwapOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  paymentApi,
  type DmsPayment, type PaymentDict, type PaymentFlow, type PaymentCollectionRecord,
} from '@/api/dms/payment'

defineOptions({ name: 'DmsPayment' })

const TABS = [
  { key: 'ledger', label: '收款台账' },
  { key: 'unpaid', label: '未付管理' },
  { key: 'handover', label: '交款稽核' },
  { key: 'flows', label: '支付流水' },
]
const activeTab = ref('ledger')

const STATUS_OPTIONS = [
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已退款', value: 2 },
  { label: '未付(挂账)', value: 3 },
]
const HANDOVER_OPTIONS = [
  { label: '未交', value: 0 },
  { label: '部分交', value: 1 },
  { label: '已交', value: 2 },
]
const MATCH_OPTIONS = [
  { label: '未匹配', value: 0 },
  { label: '已匹配', value: 1 },
  { label: '差异', value: 2 },
  { label: '已忽略', value: 3 },
]
const MATCH_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '未匹配', color: 'default' },
  1: { label: '已匹配', color: 'green' },
  2: { label: '差异', color: 'red' },
  3: { label: '已忽略', color: 'default' },
}
const DIFF_MAP: Record<string, { label: string; color: string }> = {
  MATCHED: { label: '已匹配', color: 'green' },
  FLOW_ONLY: { label: '系统无此笔', color: 'red' },
  AMOUNT_MISMATCH: { label: '金额不符', color: 'orange' },
  DUPLICATE: { label: '重复流水', color: 'volcano' },
  SYSTEM_ONLY: { label: '掉单', color: 'magenta' },
}
const ACTION_TEXT: Record<number, string> = { 1: '催收', 2: '核销', 3: '承诺付款' }

function fmtMoney(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  return isNaN(n) ? String(v) : n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtDateTime(val?: string | null) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 字典（支付方式 / 收款类型 / 收款码 / 资金安全规则） ═══
const dict = ref<PaymentDict>({})
const payChannelOptions = computed(() =>
  Object.entries(dict.value.payChannels || {}).map(([value, label]) => ({ label, value: Number(value) })))
const paymentTypeOptions = computed(() =>
  Object.entries(dict.value.paymentTypes || {}).map(([value, label]) => ({ label, value: Number(value) })))

async function loadDict() {
  try {
    const res: any = await paymentApi.dict()
    dict.value = res?.data?.data ?? res?.data ?? res ?? {}
  } catch (e) {
    console.warn('[收款管理] 字典加载失败', e)
  }
}

const loading = ref(false)
const saving = ref(false)
const pushing = ref(false)

// ═══ 收款台账 ═══
const tableData = ref<DmsPayment[]>([])
const ledgerStat = ref<Record<string, any>>({})
const ledgerPage = reactive({ current: 1, pageSize: 20, total: 0 })
const ledgerRange = ref<[Dayjs, Dayjs] | null>(null)
const selectedRows = ref<DmsPayment[]>([])
const ledgerQuery = reactive<Record<string, any>>({
  keyword: '', paymentType: undefined, payChannel: undefined,
  status: undefined, handoverStatus: undefined, overdueOnly: false,
})

function ledgerParams(current: number, size: number) {
  const params: Record<string, any> = {
    current, size,
    keyword: ledgerQuery.keyword || undefined,
    paymentType: ledgerQuery.paymentType,
    payChannel: ledgerQuery.payChannel,
    status: ledgerQuery.status,
    handoverStatus: ledgerQuery.handoverStatus,
    overdueOnly: ledgerQuery.overdueOnly || undefined,
  }
  if (ledgerRange.value) {
    params.startDate = ledgerRange.value[0].format('YYYY-MM-DD')
    params.endDate = ledgerRange.value[1].format('YYYY-MM-DD')
  }
  return params
}

async function fetchLedger() {
  loading.value = true
  try {
    const [pageRes, statRes]: any[] = await Promise.all([
      paymentApi.page(ledgerParams(ledgerPage.current, ledgerPage.pageSize)),
      paymentApi.stat(ledgerParams(1, 1)),
    ])
    const data = pageRes?.data?.data ?? pageRes?.data ?? pageRes ?? {}
    tableData.value = data.records || []
    ledgerPage.total = Number(data.total) || 0
    ledgerStat.value = statRes?.data?.data ?? statRes?.data ?? statRes ?? {}
  } catch (e) {
    tableData.value = []
    ledgerPage.total = 0
    message.error('收款台账查询失败')
    console.warn('[收款管理] 查询失败', e)
  } finally {
    loading.value = false
  }
}
function handleLedgerSearch() {
  ledgerPage.current = 1
  fetchLedger()
}
function handleLedgerReset() {
  Object.assign(ledgerQuery, {
    keyword: '', paymentType: undefined, payChannel: undefined,
    status: undefined, handoverStatus: undefined, overdueOnly: false,
  })
  ledgerRange.value = null
  handleLedgerSearch()
}
function handleLedgerPageChange(page: number, size: number) {
  ledgerPage.current = page
  ledgerPage.pageSize = size
  fetchLedger()
}
function handleSelectionChange(rows: DmsPayment[]) {
  selectedRows.value = rows || []
}
/** 「仅看交款超时」与支付/交款状态互斥（超时本身隐含「已支付且未交清」），勾选时清空后者避免查出空集 */
function handleOverdueToggle() {
  if (ledgerQuery.overdueOnly) {
    ledgerQuery.status = undefined
    ledgerQuery.handoverStatus = undefined
  }
}

// ═══ 未付管理 ═══
const unpaidData = ref<DmsPayment[]>([])
const unpaidStatData = ref<Record<string, any>>({})
const unpaidPage = reactive({ current: 1, pageSize: 20, total: 0 })
const unpaidRange = ref<[Dayjs, Dayjs] | null>(null)
const unpaidQuery = reactive<Record<string, any>>({ taskNo: '', customerName: '' })

function unpaidParams(current: number, size: number) {
  const params: Record<string, any> = {
    current, size,
    taskNo: unpaidQuery.taskNo || undefined,
    customerName: unpaidQuery.customerName || undefined,
  }
  if (unpaidRange.value) {
    params.startDate = unpaidRange.value[0].format('YYYY-MM-DD')
    params.endDate = unpaidRange.value[1].format('YYYY-MM-DD')
  }
  return params
}

async function fetchUnpaid() {
  loading.value = true
  try {
    const [pageRes, statRes]: any[] = await Promise.all([
      paymentApi.unpaidPage(unpaidParams(unpaidPage.current, unpaidPage.pageSize)),
      paymentApi.unpaidStat(unpaidParams(1, 1)),
    ])
    const data = pageRes?.data?.data ?? pageRes?.data ?? pageRes ?? {}
    unpaidData.value = data.records || []
    unpaidPage.total = Number(data.total) || 0
    unpaidStatData.value = statRes?.data?.data ?? statRes?.data ?? statRes ?? {}
  } catch (e) {
    unpaidData.value = []
    unpaidPage.total = 0
    message.error('未付台账查询失败')
  } finally {
    loading.value = false
  }
}
function handleUnpaidSearch() {
  unpaidPage.current = 1
  fetchUnpaid()
}
function handleUnpaidReset() {
  Object.assign(unpaidQuery, { taskNo: '', customerName: '' })
  unpaidRange.value = null
  handleUnpaidSearch()
}
function handleUnpaidPageChange(page: number, size: number) {
  unpaidPage.current = page
  unpaidPage.pageSize = size
  fetchUnpaid()
}
function isPromiseOverdue(record: DmsPayment) {
  return !!record.promiseDate && dayjs(String(record.promiseDate)).isBefore(dayjs(), 'day')
}

// ═══ 交款稽核 ═══
const handover = ref<Record<string, any>>({})
const handoverRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])

async function fetchHandover() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (handoverRange.value) {
      params.startDate = handoverRange.value[0].format('YYYY-MM-DD')
      params.endDate = handoverRange.value[1].format('YYYY-MM-DD')
    }
    const res: any = await paymentApi.handoverSummary(params)
    handover.value = res?.data?.data ?? res?.data ?? res ?? {}
  } catch (e) {
    handover.value = {}
    message.error('稽核汇总加载失败')
  } finally {
    loading.value = false
  }
}

// ═══ 支付流水 ═══
const flowData = ref<PaymentFlow[]>([])
const flowStatData = ref<Record<string, any>>({})
const flowPage = reactive({ current: 1, pageSize: 20, total: 0 })
const flowRange = ref<[Dayjs, Dayjs] | null>(null)
const flowQuery = reactive<Record<string, any>>({ channelCode: '', tradeNo: '', matchStatus: undefined })

function flowParams(current: number, size: number) {
  const params: Record<string, any> = {
    current, size,
    channelCode: flowQuery.channelCode || undefined,
    tradeNo: flowQuery.tradeNo || undefined,
    matchStatus: flowQuery.matchStatus,
  }
  if (flowRange.value) {
    params.startDate = flowRange.value[0].format('YYYY-MM-DD')
    params.endDate = flowRange.value[1].format('YYYY-MM-DD')
  }
  return params
}

async function fetchFlows() {
  loading.value = true
  try {
    const statParams: Record<string, any> = { channelCode: flowQuery.channelCode || undefined }
    if (flowRange.value) {
      statParams.startDate = flowRange.value[0].format('YYYY-MM-DD')
      statParams.endDate = flowRange.value[1].format('YYYY-MM-DD')
    }
    const [pageRes, statRes]: any[] = await Promise.all([
      paymentApi.flowPage(flowParams(flowPage.current, flowPage.pageSize)),
      paymentApi.flowStat(statParams),
    ])
    const data = pageRes?.data?.data ?? pageRes?.data ?? pageRes ?? {}
    flowData.value = data.records || []
    flowPage.total = Number(data.total) || 0
    flowStatData.value = statRes?.data?.data ?? statRes?.data ?? statRes ?? {}
  } catch (e) {
    flowData.value = []
    flowPage.total = 0
    message.error('支付流水查询失败')
  } finally {
    loading.value = false
  }
}
function handleFlowSearch() {
  flowPage.current = 1
  fetchFlows()
}
function handleFlowReset() {
  Object.assign(flowQuery, { channelCode: '', tradeNo: '', matchStatus: undefined })
  flowRange.value = null
  handleFlowSearch()
}
function handleFlowPageChange(page: number, size: number) {
  flowPage.current = page
  flowPage.pageSize = size
  fetchFlows()
}

// ═══ Tab 切换 / 刷新 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  selectedRows.value = []
  if (key === 'ledger') fetchLedger()
  else if (key === 'unpaid') fetchUnpaid()
  else if (key === 'handover') fetchHandover()
  else fetchFlows()
}
function refreshCurrent() {
  if (activeTab.value === 'ledger') fetchLedger()
  else if (activeTab.value === 'unpaid') fetchUnpaid()
  else if (activeTab.value === 'handover') fetchHandover()
  else fetchFlows()
}

// ═══ 列定义 ═══
const STATUS_MAP: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已退款', 3: '未付(挂账)' }
const HANDOVER_MAP: Record<number, string> = { 0: '未交', 1: '部分交', 2: '已交' }

const columns = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 250, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '任务编号', field: 'taskNo', key: 'taskNo', width: 160 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 130 },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 100 },
  { title: '收款类型', field: 'paymentType', key: 'paymentType', width: 110, type: 'slot', slotName: 'assetCell' },
  { title: '应收金额', field: 'amount', key: 'amount', width: 110, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '支付方式', field: 'payChannelName', key: 'payChannelName', width: 100 },
  { title: '支付状态', field: 'status', key: 'status', width: 110, formatter: (v: any) => (STATUS_MAP[v] || '-') },
  { title: '交款状态', field: 'handoverStatus', key: 'handoverStatus', width: 100, formatter: (v: any) => (HANDOVER_MAP[v] || '未交') },
  { title: '已上交', field: 'handoverAmount', key: 'handoverAmount', width: 110, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '交款超时', field: 'overdue', key: 'overdue', width: 130, type: 'slot', slotName: 'overdueCell' },
  { title: '支付时间', field: 'payTime', key: 'payTime', width: 150, formatter: (v: any) => fmtDateTime(v) },
  { title: '财务推送', field: 'financePushStatus', key: 'financePushStatus', width: 100, formatter: (v: any) => (v === 1 ? '已推送' : '未推送') },
  { title: '平台交易号', field: 'tradeNo', key: 'tradeNo', width: 160, defaultHidden: true },
  { title: '外部单号', field: 'externalOrderNo', key: 'externalOrderNo', width: 140, defaultHidden: true },
  { title: '未付原因', field: 'unpaidRemark', key: 'unpaidRemark', width: 140, defaultHidden: true },
  { title: '催收次数', field: 'urgeCount', key: 'urgeCount', width: 90, defaultHidden: true },
  { title: '交款人', field: 'handoverByName', key: 'handoverByName', width: 100, defaultHidden: true },
  { title: '备注', field: 'handoverRemark', key: 'handoverRemark', width: 140, defaultHidden: true },
  { title: '创建时间', field: 'createTime', key: 'createTime', width: 150, formatter: (v: any) => fmtDateTime(v) },
]

const unpaidColumns = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 200, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '任务编号', field: 'taskNo', key: 'taskNo', width: 160 },
  { title: '客户', field: 'customerName', key: 'customerName', width: 140 },
  { title: '配送员', field: 'riderName', key: 'riderName', width: 100 },
  { title: '收款类型', field: 'paymentType', key: 'paymentType', width: 110, formatter: (v: any) => (v === 1 ? '代收货款' : '配送费') },
  { title: '未付金额', field: 'amount', key: 'amount', width: 110, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '未付原因', field: 'unpaidRemark', key: 'unpaidRemark', width: 180 },
  { title: '催收次数', field: 'urgeCount', key: 'urgeCount', width: 90, formatter: (v: any) => (v == null ? 0 : v) },
  { title: '最近催收', field: 'lastUrgeTime', key: 'lastUrgeTime', width: 150, formatter: (v: any) => fmtDateTime(v) },
  { title: '承诺付款日', field: 'promiseDate', key: 'promiseDate', width: 150, type: 'slot', slotName: 'promiseCell' },
  { title: '已核销', field: 'writeOffAmount', key: 'writeOffAmount', width: 110, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '挂账时间', field: 'createTime', key: 'createTime', width: 150, formatter: (v: any) => fmtDateTime(v) },
]

const handoverColumns = [
  { title: '配送员', dataIndex: 'riderName', key: 'riderName' },
  { title: '收款笔数', dataIndex: 'recordCount', key: 'recordCount', width: 100 },
  { title: '应上交', dataIndex: 'dueAmount', key: 'dueAmount', width: 130, align: 'right' as const },
  { title: '已上交', dataIndex: 'handoverAmount', key: 'handoverAmount', width: 130, align: 'right' as const },
  { title: '未上交', dataIndex: 'diffAmount', key: 'diffAmount', width: 130, align: 'right' as const },
  { title: '未交清笔数', dataIndex: 'pendingCount', key: 'pendingCount', width: 110, align: 'right' as const },
  { title: '超时限笔数', dataIndex: 'overdueCount', key: 'overdueCount', width: 110, align: 'right' as const },
]

const overdueColumns = [
  { title: '收款记录ID', dataIndex: 'paymentId', key: 'paymentId', width: 110 },
  { title: '任务ID', dataIndex: 'taskId', key: 'taskId', width: 100 },
  { title: '配送员ID', dataIndex: 'riderId', key: 'riderId', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' as const },
  { title: '收款时间', dataIndex: 'payTime', key: 'payTime', width: 160 },
  { title: '超时', dataIndex: 'overdueHours', key: 'overdueHours', width: 110 },
]

const flowColumns = [
  { title: '渠道', dataIndex: 'channelCode', key: 'channelCode', width: 100 },
  { title: '平台交易号', dataIndex: 'tradeNo', key: 'tradeNo', width: 200 },
  { title: '商户单号', dataIndex: 'outTradeNo', key: 'outTradeNo', width: 170 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' as const },
  { title: '成交时间', dataIndex: 'tradeTime', key: 'tradeTime', width: 160 },
  { title: '付款人', dataIndex: 'payer', key: 'payer', width: 100 },
  { title: '匹配状态', dataIndex: 'matchStatus', key: 'matchStatus', width: 100 },
  { title: '匹配收款ID', dataIndex: 'paymentId', key: 'paymentId', width: 110 },
  { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 220 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' as const },
]

const reconcileColumns = [
  { title: '差异类型', dataIndex: 'type', key: 'type', width: 110 },
  { title: '平台交易号', dataIndex: 'tradeNo', key: 'tradeNo', width: 190 },
  { title: '流水金额', dataIndex: 'flowAmount', key: 'flowAmount', width: 110 },
  { title: '系统金额', dataIndex: 'systemAmount', key: 'systemAmount', width: 110 },
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 160 },
  { title: '说明', dataIndex: 'remark', key: 'remark' },
]

const collectionColumns = [
  { title: '动作', dataIndex: 'actionType', key: 'actionType', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110 },
  { title: '承诺付款日', dataIndex: 'promiseDate', key: 'promiseDate', width: 120 },
  { title: '说明', dataIndex: 'content', key: 'content' },
  { title: '经办人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
]

const batchConfirmColumns = [
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo' },
  { title: '客户', dataIndex: 'customerName', key: 'customerName' },
  { title: '收款类型', dataIndex: 'paymentType', key: 'paymentType', width: 110 },
  { title: '应收金额', dataIndex: 'amount', key: 'amount', width: 120 },
]

// ═══ 收款码 ═══
const showQrcode = ref(false)
const qrcodeResult = ref<Record<string, any>>({})
async function openQrcode(record: DmsPayment) {
  try {
    const res: any = await paymentApi.qrcode(record.taskId as number, Number(record.amount) || 0)
    qrcodeResult.value = res?.data?.data ?? res?.data ?? res ?? {}
    showQrcode.value = true
    if (!qrcodeResult.value.qrcodeUrl) {
      message.warning('未开通扫码收款，请使用现金/POS 收款后「确认收款」')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '收款码生成失败')
  }
}

// ═══ 确认收款（单/批） ═══
const showConfirm = ref(false)
const currentRecord = ref<DmsPayment>({})
const confirmForm = reactive<{ paymentType?: number; payChannel?: number; amount: number; externalOrderNo: string }>({
  paymentType: undefined, payChannel: undefined, amount: 0, externalOrderNo: '',
})
function openConfirm(record: DmsPayment) {
  currentRecord.value = record
  confirmForm.paymentType = record.paymentType
  confirmForm.payChannel = record.payChannel || 3
  confirmForm.amount = Number(record.amount) || 0
  confirmForm.externalOrderNo = ''
  showConfirm.value = true
}
async function submitConfirm() {
  saving.value = true
  try {
    await paymentApi.confirm({
      taskId: currentRecord.value.taskId as number,
      payChannel: confirmForm.payChannel,
      amount: confirmForm.amount,
      externalOrderNo: confirmForm.externalOrderNo || undefined,
      paymentType: confirmForm.paymentType,
    })
    message.success('收款已确认')
    showConfirm.value = false
    fetchLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '确认失败')
  } finally {
    saving.value = false
  }
}

const showBatchConfirm = ref(false)
const batchConfirmForm = reactive<{ payChannel: number }>({ payChannel: 3 })
const batchConfirmResult = ref<Record<string, any> | null>(null)
function openBatchConfirm() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选收款记录')
    return
  }
  batchConfirmForm.payChannel = 3
  batchConfirmResult.value = null
  showBatchConfirm.value = true
}
async function submitBatchConfirm() {
  saving.value = true
  try {
    const rows = selectedRows.value
      .filter(r => r.status === 0)
      .map(r => ({
        taskId: r.taskId,
        payChannel: batchConfirmForm.payChannel,
        amount: Number(r.amount) || 0,
        paymentType: r.paymentType,
      }))
    if (!rows.length) {
      message.warning('所选记录中没有「待支付」的单据')
      return
    }
    const res: any = await paymentApi.confirmBatch(rows)
    batchConfirmResult.value = res?.data?.data ?? res?.data ?? res
    const r = batchConfirmResult.value || {}
    if (Number(r.failedCount) > 0) {
      message.warning(`批量确认完成：成功 ${r.success} 条，失败 ${r.failedCount} 条`)
    } else {
      message.success(`批量确认完成：成功 ${r.success} 条`)
      showBatchConfirm.value = false
    }
    fetchLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '批量确认失败')
  } finally {
    saving.value = false
  }
}

// ═══ 标记未付 ═══
const showUnpaid = ref(false)
const unpaidRemark = ref('')
function openUnpaid(record: DmsPayment) {
  currentRecord.value = record
  unpaidRemark.value = ''
  showUnpaid.value = true
}
async function submitUnpaid() {
  if (!unpaidRemark.value.trim()) {
    message.warning('未付原因必填')
    return
  }
  saving.value = true
  try {
    await paymentApi.markUnpaid(currentRecord.value.taskId as number, unpaidRemark.value.trim())
    message.success('已标记未付（挂账）')
    showUnpaid.value = false
    fetchLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '标记失败')
  } finally {
    saving.value = false
  }
}

// ═══ 交款登记 ═══
const showHandover = ref(false)
const handoverForm = reactive<{ amount: number; remark: string }>({ amount: 0, remark: '' })
function openHandover(record: DmsPayment) {
  currentRecord.value = record
  handoverForm.amount = Math.max(0, Number(record.amount || 0) - Number(record.handoverAmount || 0))
  handoverForm.remark = ''
  showHandover.value = true
}
async function submitHandover() {
  if (!handoverForm.amount || handoverForm.amount <= 0) {
    message.warning('交款金额必须大于 0')
    return
  }
  saving.value = true
  try {
    await paymentApi.handover(currentRecord.value.id as number, {
      amount: handoverForm.amount,
      operatorName: '管理员',
      remark: handoverForm.remark || undefined,
    })
    message.success('交款已登记')
    showHandover.value = false
    refreshCurrent()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '登记失败')
  } finally {
    saving.value = false
  }
}

// ═══ 催收 ═══
const showUrge = ref(false)
const urgeForm = reactive<{ content: string; promiseDate?: string }>({ content: '', promiseDate: undefined })
function openUrge(record: DmsPayment) {
  currentRecord.value = record
  urgeForm.content = ''
  urgeForm.promiseDate = undefined
  showUrge.value = true
}
async function submitUrge() {
  if (!urgeForm.content.trim()) {
    message.warning('催收说明必填')
    return
  }
  saving.value = true
  try {
    await paymentApi.urge(currentRecord.value.id as number, {
      content: urgeForm.content.trim(),
      promiseDate: urgeForm.promiseDate || undefined,
      operatorName: '管理员',
    })
    message.success('催收已登记')
    showUrge.value = false
    fetchUnpaid()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '催收登记失败')
  } finally {
    saving.value = false
  }
}

// ═══ 核销 ═══
const showWriteOff = ref(false)
const writeOffForm = reactive<{ amount: number; payChannel?: number; content: string }>({
  amount: 0, payChannel: 3, content: '',
})
function openWriteOff(record: DmsPayment) {
  currentRecord.value = record
  const rest = Number(record.amount || 0) - Number(record.writeOffAmount || 0)
  writeOffForm.amount = rest > 0 ? Number(rest.toFixed(2)) : Number(record.amount || 0)
  writeOffForm.payChannel = 3
  writeOffForm.content = ''
  showWriteOff.value = true
}
async function submitWriteOff() {
  if (!writeOffForm.amount || writeOffForm.amount <= 0) {
    message.warning('核销金额必须大于 0')
    return
  }
  saving.value = true
  try {
    const res: any = await paymentApi.writeOff(currentRecord.value.id as number, {
      amount: writeOffForm.amount,
      payChannel: writeOffForm.payChannel,
      content: writeOffForm.content || undefined,
      operatorName: '管理员',
    })
    const data = res?.data?.data ?? res?.data ?? res ?? {}
    message.success(data.cleared ? '挂账已收清（置为已支付）' : '已登记部分核销')
    showWriteOff.value = false
    refreshCurrent()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '核销失败')
  } finally {
    saving.value = false
  }
}

// ═══ 催收/核销记录 ═══
const showCollections = ref(false)
const collectionList = ref<PaymentCollectionRecord[]>([])
const collectionsLoading = ref(false)
async function openCollections(record: DmsPayment) {
  currentRecord.value = record
  showCollections.value = true
  collectionsLoading.value = true
  try {
    const res: any = await paymentApi.collections(record.id as number)
    collectionList.value = res?.data?.data ?? res?.data ?? []
  } catch (e) {
    collectionList.value = []
    message.error('催收记录加载失败')
  } finally {
    collectionsLoading.value = false
  }
}

// ═══ 推送财务 ═══
async function handlePushFinanceOne(record: DmsPayment) {
  pushing.value = true
  try {
    const res: any = await paymentApi.pushFinance(record.id as number)
    const data = res?.data?.data ?? res?.data ?? {}
    message.success(data.idempotent ? '该记录已推送过（幂等返回）' : '已推送财务')
    fetchLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '推送财务失败')
  } finally {
    pushing.value = false
  }
}
async function handlePushFinance() {
  const ids = selectedRows.value.filter(r => r.status === 1).map(r => Number(r.id))
  if (!ids.length) {
    message.warning('请勾选「已支付」的收款记录')
    return
  }
  pushing.value = true
  try {
    const res: any = await paymentApi.pushFinanceBatch(ids)
    const data = res?.data?.data ?? res?.data ?? res
    if (Number(data.failedCount) > 0) {
      message.warning(`推送完成：成功 ${data.success} 条，失败 ${data.failedCount} 条`)
    } else {
      message.success(`推送完成：成功 ${data.success} 条（幂等命中 ${data.idempotentCount || 0}）`)
    }
    fetchLedger()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '批量推送失败')
  } finally {
    pushing.value = false
  }
}

// ═══ 流水导入 ═══
const showFlowImport = ref(false)
const flowImportForm = reactive<{ defaultChannel: string; text: string }>({ defaultChannel: 'WECHAT', text: '' })
const flowImportResult = ref<Record<string, any> | null>(null)

function parseFlowCsv(text: string) {
  const rows: Array<Record<string, any>> = []
  const failed: Array<Record<string, any>> = []
  text.split(/\r?\n/).forEach((line, idx) => {
    const l = line.trim()
    if (!l) return
    const cells = l.split(',').map(c => c.trim())
    if (cells.length < 2) {
      failed.push({ row: idx + 1, reason: '列数不足（至少：渠道,平台交易号）' })
      return
    }
    rows.push({
      channelCode: cells[0] || undefined,
      tradeNo: cells[1],
      outTradeNo: cells[2] || undefined,
      amount: cells[3] || undefined,
      tradeTime: cells[4] || undefined,
      payer: cells[5] || undefined,
    })
  })
  return { rows, failed }
}

async function submitFlowImport() {
  const { rows, failed } = parseFlowCsv(flowImportForm.text)
  if (!rows.length) {
    message.warning(failed.length ? '没有可导入的有效行' : '请粘贴流水数据')
    return
  }
  saving.value = true
  try {
    const res: any = await paymentApi.flowImport(rows, flowImportForm.defaultChannel || undefined)
    flowImportResult.value = res?.data?.data ?? res?.data ?? res
    const r = flowImportResult.value || {}
    if (Number(r.failedCount) > 0) {
      message.warning(`导入完成：入库 ${r.inserted}，跳过 ${r.skipped}，失败 ${r.failedCount}`)
    } else {
      message.success(`导入完成：入库 ${r.inserted}，跳过 ${r.skipped}`)
      showFlowImport.value = false
      fetchFlows()
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '流水导入失败')
  } finally {
    saving.value = false
  }
}

// ═══ 对账 ═══
const showReconcile = ref(false)
const reconcileForm = reactive<{ range: [Dayjs, Dayjs] | null; channelCode: string }>({
  range: [dayjs().subtract(7, 'day'), dayjs()],
  channelCode: '',
})
const reconcileResult = ref<Record<string, any> | null>(null)
async function submitReconcile() {
  saving.value = true
  try {
    const params: Record<string, any> = { channelCode: reconcileForm.channelCode || undefined }
    if (reconcileForm.range) {
      params.startDate = reconcileForm.range[0].format('YYYY-MM-DD')
      params.endDate = reconcileForm.range[1].format('YYYY-MM-DD')
    }
    const res: any = await paymentApi.reconcile(params)
    reconcileResult.value = res?.data?.data ?? res?.data ?? res
    const r = reconcileResult.value || {}
    message.success(`对账完成：匹配 ${r.matched} 笔，差异 ${r.diffTotal} 笔`)
    fetchFlows()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '对账失败')
  } finally {
    saving.value = false
  }
}

// ═══ 流水人工匹配 / 忽略 ═══
const showFlowMatch = ref(false)
const currentFlow = ref<PaymentFlow>({})
const flowMatchForm = reactive<{ paymentId?: number }>({ paymentId: undefined })
function openFlowMatch(flow: PaymentFlow) {
  currentFlow.value = flow
  flowMatchForm.paymentId = undefined
  showFlowMatch.value = true
}
async function submitFlowMatch() {
  if (!flowMatchForm.paymentId) {
    message.warning('请填写要匹配的收款记录ID')
    return
  }
  saving.value = true
  try {
    await paymentApi.flowMatch(currentFlow.value.id as number, flowMatchForm.paymentId)
    message.success('已人工匹配')
    showFlowMatch.value = false
    fetchFlows()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '匹配失败')
  } finally {
    saving.value = false
  }
}
async function handleFlowIgnore(flow: PaymentFlow) {
  try {
    await paymentApi.flowIgnore(flow.id as number, '人工忽略差异')
    message.success('已忽略该流水差异')
    fetchFlows()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '忽略失败')
  }
}

// ═══ 导出（真实 xlsx，后端留审计） ═══
async function handleExport() {
  loading.value = true
  try {
    const res: any = await paymentApi.exportList(ledgerParams(1, 5000))
    const list: DmsPayment[] = res?.data?.data ?? res?.data ?? []
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    const exportCols = columns.filter((c: any) => c.type !== 'rowNo' && c.type !== 'action')
    const rows = list.map(row => {
      const item: Record<string, any> = {}
      exportCols.forEach((col: any) => {
        const raw = (row as any)[col.field]
        let val: any = raw ?? ''
        if (col.key === 'paymentType') val = raw === 1 ? '代收货款' : '配送费'
        else if (col.key === 'status') val = STATUS_MAP[raw] || '-'
        else if (col.key === 'handoverStatus') val = HANDOVER_MAP[raw] || '未交'
        else if (col.key === 'overdue') val = row.overdue ? `超 ${row.overdueHours}h 未交` : ''
        else if (col.key === 'financePushStatus') val = raw === 1 ? '已推送' : '未推送'
        else if (['payTime', 'createTime'].includes(String(col.key))) val = fmtDateTime(raw)
        item[col.title] = val
      })
      return item
    })
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '收款台账')
    XLSX.writeFile(wb, `收款台账_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${list.length} 条`)
  } catch (e) {
    message.error('导出失败')
  } finally {
    loading.value = false
  }
}

// ═══ 页面配置（查询条件 / 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'paymentType', label: '收款类型', visible: true },
  { key: 'payChannel', label: '支付方式', visible: true },
  { key: 'status', label: '支付状态', visible: true },
  { key: 'handoverStatus', label: '交款状态', visible: true },
  { key: 'dateRange', label: '收款日期', visible: true },
  { key: 'overdueOnly', label: '仅看交款超时', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'batchConfirm', label: '批量确认', enabled: true },
  { key: 'pushFinance', label: '推送财务', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'flowImport', label: '导入流水', enabled: true },
  { key: 'reconcile', label: '对账', enabled: true },
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

function handleError(e: Error) {
  console.error('[收款管理] 页面错误', e)
}

onMounted(() => {
  loadDict()
  fetchLedger()
})
</script>

<style scoped>
.rule-hint { font-size: 12px; color: #8c8c8c; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-grid :deep(.ant-input),
.search-grid :deep(.ant-select) { font-size: 13px; }
.table-footer {
  display: flex; align-items: center; padding: 6px 12px; background: #fafafa;
  border: 1px solid #f0f0f0; border-top: none; font-size: 12px; color: #333;
}
.footer-label { font-weight: 600; min-width: 210px; }
.footer-values { flex: 1; display: flex; gap: 16px; flex-wrap: wrap; }
.report-wrap { padding: 12px 16px; }
.report-section { margin-top: 18px; }
.report-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.danger-text { color: #cf1322; }
.qrcode-box { text-align: center; }
.qrcode-img { max-width: 240px; }
.qrcode-link { font-size: 12px; color: #8c8c8c; word-break: break-all; margin-top: 8px; }
.modal-tip { font-size: 12px; color: #8c8c8c; margin-top: 8px; }
.result-tip { margin-top: 10px; }
.flow-table { margin-top: 16px; }
</style>
