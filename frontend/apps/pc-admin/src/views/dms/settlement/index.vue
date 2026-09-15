<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :tabs="TABS"
        :active-tab="activeTab"
        :show-table-footer="activeTab === 'settlement'"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：全局缺省费率（规则命中时以规则表为准） ═══ -->
        <template #toolbar-left>
          <a-space
            :size="8"
            class="rule-bar"
          >
            <a-tag color="blue">
              缺省费率：起步 {{ fmtMoney(rule.baseFee) }} 元 ｜ {{ fmtMoney(rule.perKmRate) }} 元/km ｜ 免费 {{ rule.freeDistanceKm ?? 0 }} km
            </a-tag>
            <a-button
              type="link"
              size="small"
              @click="switchTab('ruleTab')"
            >
              计费规则（{{ ruleTotal }}）
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <template v-if="activeTab === 'settlement'">
              <a-button
                type="primary"
                size="small"
                @click="openGenerateModal"
              >
                <PlusOutlined /> 生成结算单
              </a-button>
              <a-button
                size="small"
                :disabled="!selectedRowKeys.length"
                @click="handleBatchConfirm"
              >
                <CheckOutlined /> 批量确认
              </a-button>
            </template>
            <a-button
              v-if="activeTab === 'ruleTab'"
              type="primary"
              size="small"
              @click="openRuleModal()"
            >
              <PlusOutlined /> 新增规则
            </a-button>
            <a-button
              size="small"
              @click="refreshCurrent"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="activeTab === 'settlement'"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template v-if="activeTab === 'settlement'">
                <a-input
                  v-model:value="query.settlementNo"
                  size="small"
                  placeholder="结算单号"
                  allow-clear
                  style="width: 170px"
                />
                <a-select
                  v-model:value="query.targetType"
                  size="small"
                  placeholder="结算对象"
                  allow-clear
                  :options="TARGET_TYPE_OPTIONS"
                  style="width: 140px"
                />
                <a-select
                  v-model:value="query.status"
                  size="small"
                  placeholder="结算状态"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  style="width: 140px"
                />
                <a-range-picker
                  v-model:value="periodRange"
                  size="small"
                  style="width: 240px"
                  :placeholder="['周期起', '周期止']"
                />
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
              </template>

              <template v-else-if="activeTab === 'ruleTab'">
                <a-input
                  v-model:value="ruleQuery.ruleCode"
                  size="small"
                  placeholder="规则编码"
                  allow-clear
                  style="width: 150px"
                />
                <a-input
                  v-model:value="ruleQuery.ruleName"
                  size="small"
                  placeholder="规则名称"
                  allow-clear
                  style="width: 180px"
                />
                <a-select
                  v-model:value="ruleQuery.targetType"
                  size="small"
                  placeholder="结算对象"
                  allow-clear
                  :options="TARGET_TYPE_OPTIONS"
                  style="width: 140px"
                />
                <a-select
                  v-model:value="ruleQuery.status"
                  size="small"
                  placeholder="状态"
                  allow-clear
                  :options="ENABLE_OPTIONS"
                  style="width: 120px"
                />
                <a-button
                  type="primary"
                  size="small"
                  @click="handleRuleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleRuleReset"
                >
                  重置
                </a-button>
              </template>

              <template v-else-if="activeTab === 'reconcile'">
                <a-range-picker
                  v-model:value="reconcileRange"
                  size="small"
                  style="width: 240px"
                  :placeholder="['周期起', '周期止']"
                />
                <a-select
                  v-model:value="reconcileQuery.targetType"
                  size="small"
                  placeholder="结算对象"
                  allow-clear
                  :options="TARGET_TYPE_OPTIONS"
                  style="width: 140px"
                />
                <a-checkbox v-model:checked="reconcileQuery.onlyDiff">
                  仅看差异
                </a-checkbox>
                <a-button
                  type="primary"
                  size="small"
                  @click="fetchReconcile"
                >
                  对账
                </a-button>
              </template>

              <template v-else>
                <a-range-picker
                  v-model:value="reportRange"
                  size="small"
                  style="width: 260px"
                  :placeholder="['开始日期', '结束日期']"
                />
                <a-button
                  type="primary"
                  size="small"
                  @click="fetchReport"
                >
                  生成报表
                </a-button>
              </template>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区 ═══ -->
        <template #table>
          <!-- ① 结算单台账 -->
          <BillTableList
            v-if="activeTab === 'settlement'"
            :columns="columns"
            :data-source="tableData"
            storage-key="dms-settlement-columns"
            global-config-key="dms-settlement-columns"
            :loading="loading"
            :pagination="false"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
            @selection-change="handleSelectionChange"
          >
            <template #actionCell="{ record }">
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  明细
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  @click="handleConfirm(record)"
                >
                  确认
                </a-button>
                <a-button
                  v-if="record.status === 1"
                  type="link"
                  size="small"
                  @click="handlePush(record)"
                >
                  推送ERP
                </a-button>
                <a-button
                  v-if="record.status === 0"
                  type="link"
                  size="small"
                  danger
                  @click="handleDelete(record)"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </BillTableList>

          <!-- ② 计费规则 -->
          <BillTableList
            v-else-if="activeTab === 'ruleTab'"
            :columns="ruleColumns"
            :data-source="ruleData"
            storage-key="dms-settlement-rule-columns"
            global-config-key="dms-settlement-rule-columns"
            :loading="ruleLoading"
            :pagination="false"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            row-key="id"
          >
            <template #ruleActionCell="{ record }">
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  @click="openRuleModal(record)"
                >
                  修改
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleRuleToggle(record)"
                >
                  {{ record.status === 1 ? '停用' : '启用' }}
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  danger
                  @click="handleRuleDelete(record)"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </BillTableList>

          <!-- ③ 对账 -->
          <div
            v-else-if="activeTab === 'reconcile'"
            class="report-wrap"
          >
            <a-spin :spinning="reconcileLoading">
              <a-row :gutter="16">
                <a-col :span="4">
                  <a-statistic
                    title="结算单数"
                    :value="reconcile.summary?.settlementCount || 0"
                    suffix="张"
                  />
                </a-col>
                <a-col :span="4">
                  <a-statistic
                    title="结算金额"
                    :value="fmtMoney(reconcile.summary?.totalAmount)"
                    prefix="¥"
                  />
                </a-col>
                <a-col :span="4">
                  <a-statistic
                    title="已结金额"
                    :value="fmtMoney(reconcile.summary?.settledAmount)"
                    prefix="¥"
                  />
                </a-col>
                <a-col :span="4">
                  <a-statistic
                    title="差异合计"
                    :value="fmtMoney(reconcile.summary?.diffAmount)"
                    prefix="¥"
                    :value-style="{ color: Number(reconcile.summary?.diffAmount) ? '#cf1322' : undefined }"
                  />
                </a-col>
                <a-col :span="4">
                  <a-statistic
                    title="差异单数"
                    :value="reconcile.summary?.diffCount || 0"
                    suffix="张"
                  />
                </a-col>
                <a-col :span="4">
                  <a-statistic
                    title="未记账"
                    :value="reconcile.summary?.unaccountedCount || 0"
                    suffix="张"
                  />
                </a-col>
              </a-row>
              <a-table
                class="reconcile-table"
                :columns="reconcileColumns"
                :data-source="reconcile.rows || []"
                :pagination="false"
                size="small"
                row-key="settlementNo"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'diff'">
                    <span :class="{ 'diff-bad': Number(record.diff) !== 0 }">{{ fmtMoney(record.diff) }}</span>
                  </template>
                  <template v-else-if="column.key === 'matched'">
                    <a-tag :color="record.matched ? 'green' : 'red'">
                      {{ record.matched ? '已对平' : '有差异' }}
                    </a-tag>
                  </template>
                  <template v-else-if="column.key === 'accounted'">
                    <a-tag :color="record.accounted ? 'blue' : 'orange'">
                      {{ record.accounted ? '已记账' : '未记账' }}
                    </a-tag>
                  </template>
                </template>
              </a-table>
              <div class="modal-hint">
                对账口径：渠道（外部运力）已结金额取**应付已付**；配送员已结金额取周期内**配送费收款**（收款管理）；
                差异 = 已结金额 − 结算金额（0 为对平）。
              </div>
            </a-spin>
          </div>

          <!-- ④ 周期报表 -->
          <div
            v-else
            class="report-wrap"
          >
            <a-spin :spinning="reportLoading">
              <a-row :gutter="16">
                <a-col :span="8">
                  <a-statistic
                    title="可结算任务数"
                    :value="report.totalTasks || 0"
                    suffix="单"
                  />
                </a-col>
                <a-col :span="8">
                  <a-statistic
                    title="结算金额合计"
                    :value="fmtMoney(report.totalFee)"
                    prefix="¥"
                  />
                </a-col>
                <a-col :span="8">
                  <a-statistic
                    title="统计周期"
                    :value="`${report.startDate || '-'} ~ ${report.endDate || '-'}`"
                  />
                </a-col>
              </a-row>
              <div class="report-section">
                <div class="report-title">
                  按配送员
                </div>
                <a-table
                  :columns="reportRiderColumns"
                  :data-source="report.byRider || []"
                  :pagination="false"
                  size="small"
                  row-key="riderName"
                />
              </div>
              <div class="report-section">
                <div class="report-title">
                  按日
                </div>
                <a-table
                  :columns="reportDayColumns"
                  :data-source="report.byDay || []"
                  :pagination="false"
                  size="small"
                  row-key="date"
                />
              </div>
            </a-spin>
          </div>
        </template>

        <!-- ═══ 底部：经典分页 + 合计 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
          <div class="table-footer">
            <div class="footer-label">
              合计（本页 {{ tableData.length }} 张结算单）
            </div>
            <div class="footer-values">
              <span>金额合计: {{ fmtMoney(pageTotalAmount) }}</span>
              <span>单量合计: {{ pageTotalCount }}</span>
            </div>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 生成结算单 ═══ -->
    <a-modal
      v-model:open="showGenerateModal"
      title="生成结算单"
      :width="520"
      ok-text="生成"
      cancel-text="取消"
      :confirm-loading="generating"
      @ok="submitGenerate"
    >
      <a-form
        layout="vertical"
        size="small"
      >
        <a-form-item
          label="结算周期"
          required
        >
          <a-range-picker
            v-model:value="generateForm.period"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="结算对象类型">
          <a-select
            v-model:value="generateForm.targetType"
            :options="TARGET_TYPE_OPTIONS"
            @change="onGenerateTypeChange"
          />
        </a-form-item>
        <a-form-item
          label="结算对象"
          required
        >
          <a-select
            v-if="generateForm.targetType === 2"
            v-model:value="generateForm.targetId"
            show-search
            option-filter-prop="label"
            placeholder="选择运力渠道"
            :options="channelOptions"
            :loading="channelLoading"
            style="width: 100%"
          />
          <a-select
            v-else
            v-model:value="generateForm.targetId"
            show-search
            option-filter-prop="label"
            placeholder="选择配送员"
            :options="riderOptions"
            :loading="riderLoading"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="generateForm.remark" />
        </a-form-item>
        <div class="modal-hint">
          生成口径：聚合周期内「已签收 / 已完成」任务，按**命中的计费规则**（无命中用缺省费率）计费；
          部分签收按实际签收数量折算（起步价全收），拒收不计费。同周期同对象的草稿会被重建。
        </div>
      </a-form>
    </a-modal>

    <!-- ═══ 结算单明细（费用构成 + 签收口径） ═══ -->
    <a-drawer
      v-model:open="showDetail"
      title="结算单明细"
      :width="980"
    >
      <a-spin :spinning="detailLoading">
        <a-descriptions
          :column="2"
          size="small"
          bordered
        >
          <a-descriptions-item label="结算单号">
            {{ currentSettlement.settlementNo }}
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            {{ statusText(currentSettlement.status) }}
          </a-descriptions-item>
          <a-descriptions-item label="结算对象">
            {{ currentSettlement.targetName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="周期">
            {{ currentSettlement.periodStart }} ~ {{ currentSettlement.periodEnd }}
          </a-descriptions-item>
          <a-descriptions-item label="单量">
            {{ currentSettlement.taskCount }}
          </a-descriptions-item>
          <a-descriptions-item label="结算金额">
            ¥{{ fmtMoney(currentSettlement.totalAmount) }}
          </a-descriptions-item>
          <a-descriptions-item
            label="计费规则快照"
            :span="2"
          >
            {{ currentSettlement.ruleSnapshot || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="currentSettlement.erpVoucherNo"
            label="记账凭证号"
          >
            {{ currentSettlement.erpVoucherNo }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="currentSettlement.erpPayableNo"
            label="应付单号"
          >
            {{ currentSettlement.erpPayableNo }}
          </a-descriptions-item>
          <a-descriptions-item
            v-if="currentSettlement.pushTraceId"
            label="推送 traceId"
            :span="2"
          >
            {{ currentSettlement.pushTraceId }}
          </a-descriptions-item>
        </a-descriptions>
        <a-table
          class="detail-table"
          :columns="itemColumns"
          :data-source="detailItems"
          :pagination="false"
          size="small"
          row-key="id"
          :scroll="{ x: 1100 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'signType'">
              {{ SIGN_TYPE_TEXT[record.signType] || '-' }}
            </template>
            <template v-else-if="column.key === 'billingRatio'">
              {{ record.billingRatio == null ? '-' : Number(record.billingRatio).toFixed(2) }}
            </template>
            <template v-else-if="MONEY_ITEM_KEYS.includes(column.key as string)">
              {{ column.key === 'distanceKm' ? (record.distanceKm ?? 0) : fmtMoney(record[column.dataIndex as string]) }}
            </template>
          </template>
        </a-table>
      </a-spin>
    </a-drawer>

    <!-- ═══ 计费规则（新增/修改，档案表单：分区卡片 + 两列栅格） ═══ -->
    <a-modal
      v-model:open="showRuleModal"
      :title="ruleForm.id ? '修改计费规则' : '新增计费规则'"
      :width="880"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="ruleSaving"
      @ok="submitRule"
    >
      <a-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="ruleRules"
        layout="vertical"
        size="small"
      >
        <a-card
          title="基本信息"
          size="small"
          class="rule-card"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item
                label="规则编码"
                name="ruleCode"
              >
                <a-input
                  v-model:value="ruleForm.ruleCode"
                  placeholder="留空自动生成（JSR001）"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="规则名称"
                name="ruleName"
              >
                <a-input
                  v-model:value="ruleForm.ruleName"
                  placeholder="如：达达渠道-月结"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item
                label="结算对象"
                name="targetType"
              >
                <a-select
                  v-model:value="ruleForm.targetType"
                  :options="TARGET_TYPE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="结算周期">
                <a-select
                  v-model:value="ruleForm.settleCycle"
                  :options="SETTLE_CYCLE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="优先级（越小越优先）">
                <a-input-number
                  v-model:value="ruleForm.priority"
                  :min="1"
                  :max="999"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="状态">
                <a-select
                  v-model:value="ruleForm.status"
                  :options="ENABLE_OPTIONS"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card
          title="适用范围（留空 = 不限）"
          size="small"
          class="rule-card"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="适用渠道">
                <a-select
                  v-model:value="ruleForm.channelId"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  placeholder="全部渠道"
                  :options="channelOptions"
                  :loading="channelLoading"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="适用线路">
                <a-select
                  v-model:value="ruleForm.routeId"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  placeholder="全部线路"
                  :options="routeOptions"
                  :loading="routeLoading"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="计价方式">
                <a-select
                  v-model:value="ruleForm.billingType"
                  :options="BILLING_TYPE_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="生效开始">
                <a-date-picker
                  v-model:value="ruleForm.effectiveStart"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="生效结束">
                <a-date-picker
                  v-model:value="ruleForm.effectiveEnd"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <a-card
          title="费率与加价"
          size="small"
          class="rule-card"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="起步价（元）">
                <a-input-number
                  v-model:value="ruleForm.baseFee"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="showDistanceFields"
              :span="8"
            >
              <a-form-item label="免费里程（km）">
                <a-input-number
                  v-model:value="ruleForm.freeDistanceKm"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="showDistanceFields"
              :span="8"
            >
              <a-form-item label="每公里单价（元/km）">
                <a-input-number
                  v-model:value="ruleForm.perKmRate"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="showWeightFields"
              :span="8"
            >
              <a-form-item label="每公斤单价（元/kg）">
                <a-input-number
                  v-model:value="ruleForm.perKgRate"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="夜间附加系数（×起步价）">
                <a-input-number
                  v-model:value="ruleForm.timeSurchargeRate"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="加急附加费（元）">
                <a-input-number
                  v-model:value="ruleForm.urgentSurcharge"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="备注">
                <a-textarea
                  v-model:value="ruleForm.remark"
                  :rows="2"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>
      </a-form>
      <div class="modal-hint">
        算费口径：按单 = 起步价；按距离 = 起步价 + (里程−免费里程)×每公里单价；按重量 = 起步价 + 重量×每公斤单价；组合 = 起步价 + 里程费 + 重量费。
        命中规则时**整体**按规则计费，未命中回落全局缺省费率。
      </div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 配送结算（配送 → 结算收款 → 配送结算，菜单 80910）
 *
 * 金标准四视图：
 *   · 结算单：生成（草稿）→ 确认（锁定金额）→ 推送 ERP（**真实记账**：凭证 + 外部运力应付，幂等）
 *   · 计费规则：按 结算对象 × 渠道/线路 × 生效期 × 优先级 差异化计价，未命中回落全局缺省费率
 *   · 对账：结算单 vs 财务（凭证金额 / 应付核销 / 配送费收款），输出差异清单
 *   · 周期报表：按配送员 / 按日聚合（口径 = 已签收 / 已完成）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import * as XLSX from 'xlsx'
import {
  PlusOutlined, ReloadOutlined, ExportOutlined, CheckOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { settlementApi, type DmsSettlement, type DmsSettlementItem, type DmsSettlementRule, type SettlementRule, type SettlementReconcile } from '@/api/dms/settlement'
import { channelApi } from '@/api/dms/channel'
import { riderApi } from '@/api/dms/rider'
import { mdRouteApi } from '@/api/md'

const TABS = [
  { key: 'settlement', label: '结算单' },
  { key: 'ruleTab', label: '计费规则' },
  { key: 'reconcile', label: '对账' },
  { key: 'report', label: '周期报表' }
]
const activeTab = ref('settlement')

const TARGET_TYPE_OPTIONS = [
  { label: '配送员', value: 1 },
  { label: '渠道', value: 2 }
]
const STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '已确认', value: 1 },
  { label: '已推送', value: 2 }
]
const ENABLE_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]
const BILLING_TYPE_OPTIONS = [
  { label: '按单（固定）', value: 1 },
  { label: '按距离', value: 2 },
  { label: '按重量', value: 3 },
  { label: '组合（距离+重量）', value: 4 }
]
const SETTLE_CYCLE_OPTIONS = [
  { label: '日结', value: 1 },
  { label: '周结', value: 2 },
  { label: '月结', value: 3 }
]
const STATUS_TEXT: Record<number, string> = { 0: '草稿', 1: '已确认', 2: '已推送' }
const SIGN_TYPE_TEXT: Record<number, string> = { 1: '正常签收', 2: '部分签收', 3: '拒收' }
const MONEY_ITEM_KEYS = ['distanceKm', 'baseFee', 'mileageFee', 'weightFee', 'timeSurcharge', 'urgentSurcharge', 'totalFee']

function statusText(s?: number) {
  return s === null || s === undefined ? '-' : (STATUS_TEXT[s] || String(s))
}
function billingTypeText(v?: number) {
  return BILLING_TYPE_OPTIONS.find(o => o.value === v)?.label || '-'
}
function fmtMoney(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  return isNaN(n) ? String(v) : n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function unwrap(res: any) {
  return res?.data?.data ?? res?.data ?? res ?? {}
}

// ═══ 全局缺省费率 + 规则数 ═══
const rule = ref<SettlementRule>({})
const ruleTotal = ref(0)
async function loadRule() {
  try {
    rule.value = unwrap(await settlementApi.rule())
  } catch (e) {
    console.warn('[配送结算] 缺省费率加载失败', e)
  }
}

// ═══ 下拉数据（渠道 / 配送员 / 线路） ═══
const channelOptions = ref<any[]>([])
const channelLoading = ref(false)
const riderOptions = ref<any[]>([])
const riderLoading = ref(false)
const routeOptions = ref<any[]>([])
const routeLoading = ref(false)

async function loadChannels() {
  if (channelOptions.value.length) return
  channelLoading.value = true
  try {
    const res: any = await channelApi.options()
    const list = Array.isArray(res) ? res : (res?.data?.data ?? res?.data ?? [])
    channelOptions.value = (list || []).map((c: any) => ({
      label: c.channelName || c.channelCode || String(c.id),
      value: c.id
    }))
  } catch (e) {
    console.warn('[配送结算] 渠道下拉加载失败', e)
  } finally {
    channelLoading.value = false
  }
}
async function loadRiders() {
  if (riderOptions.value.length) return
  riderLoading.value = true
  try {
    const res: any = await riderApi.list({ size: 500 })
    const list = res?.data?.data ?? res?.data ?? res ?? []
    riderOptions.value = (Array.isArray(list) ? list : list.records || []).map((r: any) => ({
      label: r.riderName || r.name || String(r.id),
      value: r.id
    }))
  } catch (e) {
    console.warn('[配送结算] 配送员下拉加载失败', e)
  } finally {
    riderLoading.value = false
  }
}
async function loadRoutes() {
  if (routeOptions.value.length) return
  routeLoading.value = true
  try {
    const res: any = await mdRouteApi.options()
    const list = res?.data?.data ?? res?.data ?? res ?? []
    routeOptions.value = (Array.isArray(list) ? list : []).map((r: any) => ({
      label: r.routeName || r.routeCode || String(r.id),
      value: r.id
    }))
  } catch (e) {
    console.warn('[配送结算] 线路下拉加载失败', e)
  } finally {
    routeLoading.value = false
  }
}

// ═══ 结算单列表 ═══
const loading = ref(false)
const tableData = ref<DmsSettlement[]>([])
const selectedRowKeys = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const periodRange = ref<[Dayjs, Dayjs] | null>(null)
const query = reactive<Record<string, any>>({ settlementNo: '', targetType: undefined, status: undefined })

const pageTotalAmount = computed(() => tableData.value.reduce((a, r) => a + (Number(r.totalAmount) || 0), 0))
const pageTotalCount = computed(() => tableData.value.reduce((a, r) => a + (Number(r.taskCount) || 0), 0))

function buildParams(current: number, size: number) {
  const params: Record<string, any> = {
    current,
    size,
    settlementNo: query.settlementNo || undefined,
    targetType: query.targetType,
    status: query.status
  }
  if (periodRange.value) {
    params.startDate = periodRange.value[0].format('YYYY-MM-DD')
    params.endDate = periodRange.value[1].format('YYYY-MM-DD')
  }
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const data = unwrap(await settlementApi.page(buildParams(pagination.current, pagination.pageSize)))
    tableData.value = data.records || []
    pagination.total = Number(data.total) || 0
  } catch (e) {
    tableData.value = []
    pagination.total = 0
    message.error('结算单查询失败')
    console.warn('[配送结算] 查询失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  query.settlementNo = ''
  query.targetType = undefined
  query.status = undefined
  periodRange.value = null
  handleSearch()
}
function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}
function handleSelectionChange(_rows: any[], ids: any[]) {
  selectedRowKeys.value = ids
}
function switchTab(key: string) {
  activeTab.value = key
  handleTabChange(key)
}
function handleTabChange(key: string) {
  activeTab.value = key
  if (key === 'report') fetchReport()
  else if (key === 'reconcile') fetchReconcile()
  else if (key === 'ruleTab') fetchRules()
  else fetchData()
}
function refreshCurrent() {
  handleTabChange(activeTab.value)
}

// ═══ 列定义 ═══
const columns = [
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' as const },
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 190, fixed: 'right' as const, slotName: 'actionCell' },
  { title: '结算单号', field: 'settlementNo', key: 'settlementNo', width: 170 },
  {
    title: '结算对象类型', field: 'targetType', key: 'targetType', width: 110,
    formatter: (v: any) => (v === 1 ? '配送员' : v === 2 ? '渠道' : '-')
  },
  { title: '结算对象', field: 'targetName', key: 'targetName', width: 120 },
  { title: '周期起', field: 'periodStart', key: 'periodStart', width: 110 },
  { title: '周期止', field: 'periodEnd', key: 'periodEnd', width: 110 },
  { title: '单量', field: 'taskCount', key: 'taskCount', width: 80, align: 'right' as const },
  {
    title: '结算金额', field: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' as const,
    formatter: (v: any) => fmtMoney(v)
  },
  { title: '状态', field: 'status', key: 'status', width: 90, formatter: (v: any) => statusText(v) },
  { title: '记账凭证号', field: 'erpVoucherNo', key: 'erpVoucherNo', width: 180 },
  { title: '应付单号', field: 'erpPayableNo', key: 'erpPayableNo', width: 160, defaultHidden: true },
  { title: '推送时间', field: 'pushTime', key: 'pushTime', width: 150, defaultHidden: true },
  { title: '推送次数', field: 'pushCount', key: 'pushCount', width: 90, align: 'right' as const, defaultHidden: true },
  { title: '计费规则快照', field: 'ruleSnapshot', key: 'ruleSnapshot', width: 240, defaultHidden: true },
  { title: '备注', field: 'remark', key: 'remark', width: 140 },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150 }
]

// ═══ 生成结算单 ═══
const showGenerateModal = ref(false)
const generating = ref(false)
const generateForm = reactive<{ period: [Dayjs, Dayjs] | null; targetType: number; targetId: any; remark: string }>({
  period: null, targetType: 1, targetId: undefined, remark: ''
})

function openGenerateModal() {
  generateForm.period = [dayjs().startOf('month'), dayjs()]
  generateForm.targetType = 1
  generateForm.targetId = undefined
  generateForm.remark = ''
  loadRiders()
  loadChannels()
  showGenerateModal.value = true
}
function onGenerateTypeChange() {
  generateForm.targetId = undefined
  if (generateForm.targetType === 2) loadChannels()
  else loadRiders()
}

async function submitGenerate() {
  if (!generateForm.period) {
    message.warning('请选择结算周期')
    return
  }
  if (!generateForm.targetId) {
    message.warning('请选择结算对象')
    return
  }
  generating.value = true
  try {
    await settlementApi.generate({
      periodStart: generateForm.period[0].format('YYYY-MM-DD'),
      periodEnd: generateForm.period[1].format('YYYY-MM-DD'),
      targetType: generateForm.targetType,
      targetId: generateForm.targetId,
      remark: generateForm.remark || undefined
    })
    message.success('结算单已生成（草稿）')
    showGenerateModal.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '生成失败：该周期该对象可能没有可结算任务')
    console.warn('[配送结算] 生成失败', e)
  } finally {
    generating.value = false
  }
}

// ═══ 行操作 ═══
async function handleConfirm(record: DmsSettlement) {
  try {
    await settlementApi.confirm(record.id as number)
    message.success('已确认，金额已锁定')
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '确认失败')
  }
}

async function handlePush(record: DmsSettlement) {
  try {
    const data = unwrap(await settlementApi.pushErp(record.id as number))
    if (data.idempotent) {
      message.success('该结算单已推送过（幂等，返回原结果）')
    } else if (data.accounted) {
      message.success(`已推送 ERP：凭证 ${data.voucherNo || '-'}${data.payableNo ? '，应付 ' + data.payableNo : ''}`)
    } else {
      message.warning(`已推送事件，但记账未完成：${data.accountMessage || '-'}`)
    }
    fetchData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '推送失败')
  }
}

function handleDelete(record: DmsSettlement) {
  Modal.confirm({
    title: '删除结算单',
    content: `确认删除草稿结算单「${record.settlementNo}」？已确认/已推送的需保留审计留痕，不可删除。`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await settlementApi.remove(record.id as number)
        message.success('已删除')
        fetchData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    }
  })
}

async function handleBatchConfirm() {
  const drafts = tableData.value.filter(r => selectedRowKeys.value.includes(r.id) && r.status === 0)
  if (!drafts.length) {
    message.warning('选中项中没有「草稿」状态的结算单')
    return
  }
  for (const row of drafts) {
    try {
      await settlementApi.confirm(row.id as number)
    } catch (e) {
      console.warn('[配送结算] 批量确认跳过', row.settlementNo, e)
    }
  }
  message.success(`已确认 ${drafts.length} 张结算单`)
  fetchData()
}

// ═══ 明细抽屉 ═══
const showDetail = ref(false)
const detailLoading = ref(false)
const detailItems = ref<DmsSettlementItem[]>([])
const currentSettlement = ref<DmsSettlement>({})

const itemColumns = [
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 160 },
  { title: '签收时间', dataIndex: 'signTime', key: 'signTime', width: 150 },
  { title: '签收类型', key: 'signType', width: 100 },
  { title: '应签收', dataIndex: 'plannedQuantity', key: 'plannedQuantity', width: 90, align: 'right' as const },
  { title: '实签收', dataIndex: 'actualQuantity', key: 'actualQuantity', width: 90, align: 'right' as const },
  { title: '计费系数', key: 'billingRatio', width: 90, align: 'right' as const },
  { title: '里程(km)', dataIndex: 'distanceKm', key: 'distanceKm', width: 90, align: 'right' as const },
  { title: '起步价', dataIndex: 'baseFee', key: 'baseFee', width: 90, align: 'right' as const },
  { title: '里程费', dataIndex: 'mileageFee', key: 'mileageFee', width: 90, align: 'right' as const },
  { title: '重量费', dataIndex: 'weightFee', key: 'weightFee', width: 90, align: 'right' as const },
  { title: '夜间附加', dataIndex: 'timeSurcharge', key: 'timeSurcharge', width: 90, align: 'right' as const },
  { title: '加急附加', dataIndex: 'urgentSurcharge', key: 'urgentSurcharge', width: 90, align: 'right' as const },
  { title: '小计', dataIndex: 'totalFee', key: 'totalFee', width: 100, align: 'right' as const }
]

async function openDetail(record: DmsSettlement) {
  currentSettlement.value = { ...record }
  showDetail.value = true
  detailLoading.value = true
  detailItems.value = []
  try {
    detailItems.value = unwrap(await settlementApi.items(record.id as number))
  } catch (e) {
    message.error('明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 计费规则 ═══
const ruleLoading = ref(false)
const ruleSaving = ref(false)
const ruleData = ref<DmsSettlementRule[]>([])
const ruleQuery = reactive<Record<string, any>>({ ruleCode: '', ruleName: '', targetType: undefined, status: undefined })
const showRuleModal = ref(false)
const ruleFormRef = ref()
const ruleForm = reactive<Record<string, any>>({
  id: undefined, ruleCode: '', ruleName: '', targetType: 1, channelId: undefined, routeId: undefined,
  billingType: 2, baseFee: 5, freeDistanceKm: 0, perKmRate: 2, perKgRate: 0,
  timeSurchargeRate: 1.5, urgentSurcharge: 10, settleCycle: 1,
  effectiveStart: undefined, effectiveEnd: undefined, priority: 100, status: 1, remark: ''
})
const ruleRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  targetType: [{ required: true, message: '请选择结算对象', trigger: 'change' }]
}
const showDistanceFields = computed(() => [2, 4].includes(ruleForm.billingType))
const showWeightFields = computed(() => [3, 4].includes(ruleForm.billingType))

const ruleColumns = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' as const },
  { key: 'action', title: '操作', type: 'action', width: 160, fixed: 'right' as const, slotName: 'ruleActionCell' },
  { title: '规则编码', field: 'ruleCode', key: 'ruleCode', width: 110 },
  { title: '规则名称', field: 'ruleName', key: 'ruleName', width: 160 },
  {
    title: '结算对象', field: 'targetType', key: 'targetType', width: 100,
    formatter: (v: any) => (v === 2 ? '渠道' : '配送员')
  },
  {
    title: '适用渠道', field: 'channelId', key: 'channelId', width: 140,
    formatter: (v: any) => channelLabel(v)
  },
  {
    title: '适用线路', field: 'routeId', key: 'routeId', width: 140,
    formatter: (v: any) => routeLabel(v)
  },
  { title: '计价方式', field: 'billingType', key: 'billingType', width: 130, formatter: (v: any) => billingTypeText(v) },
  { title: '起步价', field: 'baseFee', key: 'baseFee', width: 90, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '免费里程', field: 'freeDistanceKm', key: 'freeDistanceKm', width: 90, align: 'right' as const },
  { title: '每公里', field: 'perKmRate', key: 'perKmRate', width: 90, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '每公斤', field: 'perKgRate', key: 'perKgRate', width: 90, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  { title: '夜间系数', field: 'timeSurchargeRate', key: 'timeSurchargeRate', width: 90, align: 'right' as const },
  { title: '加急附加', field: 'urgentSurcharge', key: 'urgentSurcharge', width: 90, align: 'right' as const, formatter: (v: any) => fmtMoney(v) },
  {
    title: '结算周期', field: 'settleCycle', key: 'settleCycle', width: 90,
    formatter: (v: any) => SETTLE_CYCLE_OPTIONS.find(o => o.value === v)?.label || '-'
  },
  { title: '生效开始', field: 'effectiveStart', key: 'effectiveStart', width: 110 },
  { title: '生效结束', field: 'effectiveEnd', key: 'effectiveEnd', width: 110 },
  { title: '优先级', field: 'priority', key: 'priority', width: 80, align: 'right' as const },
  {
    title: '状态', field: 'status', key: 'status', width: 80,
    formatter: (v: any) => (v === 1 ? '启用' : '停用')
  },
  { title: '备注', field: 'remark', key: 'remark', width: 160, defaultHidden: true }
]

function channelLabel(id?: number | null) {
  if (!id) return '不限'
  return channelOptions.value.find(o => o.value === id)?.label || String(id)
}
function routeLabel(id?: number | null) {
  if (!id) return '不限'
  return routeOptions.value.find(o => o.value === id)?.label || String(id)
}

async function fetchRules() {
  ruleLoading.value = true
  try {
    const data = unwrap(await settlementApi.rulePage({
      current: 1, size: 200,
      ruleCode: ruleQuery.ruleCode || undefined,
      ruleName: ruleQuery.ruleName || undefined,
      targetType: ruleQuery.targetType,
      status: ruleQuery.status
    }))
    ruleData.value = data.records || []
    ruleTotal.value = Number(data.total) || ruleData.value.length
  } catch (e) {
    ruleData.value = []
    message.error('计费规则加载失败')
  } finally {
    ruleLoading.value = false
  }
}
function handleRuleSearch() {
  fetchRules()
}
function handleRuleReset() {
  ruleQuery.ruleCode = ''
  ruleQuery.ruleName = ''
  ruleQuery.targetType = undefined
  ruleQuery.status = undefined
  fetchRules()
}

async function openRuleModal(record?: DmsSettlementRule) {
  loadChannels()
  loadRoutes()
  if (record) {
    Object.assign(ruleForm, record)
  } else {
    Object.assign(ruleForm, {
      id: undefined, ruleCode: '', ruleName: '', targetType: 1, channelId: undefined, routeId: undefined,
      billingType: 2, baseFee: 5, freeDistanceKm: 0, perKmRate: 2, perKgRate: 0,
      timeSurchargeRate: 1.5, urgentSurcharge: 10, settleCycle: 1,
      effectiveStart: undefined, effectiveEnd: undefined, priority: 100, status: 1, remark: ''
    })
  }
  showRuleModal.value = true
}

async function submitRule() {
  try {
    await ruleFormRef.value?.validate()
  } catch (e) {
    return
  }
  ruleSaving.value = true
  try {
    const payload: Partial<DmsSettlementRule> = { ...ruleForm }
    if (ruleForm.id) {
      await settlementApi.ruleUpdate(ruleForm.id, payload)
      message.success('规则已更新（新生成的结算单按新规则计费，历史单保留快照）')
    } else {
      await settlementApi.ruleCreate(payload)
      message.success('规则已新增')
    }
    showRuleModal.value = false
    fetchRules()
    loadRule()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    ruleSaving.value = false
  }
}

async function handleRuleToggle(record: DmsSettlementRule) {
  try {
    await settlementApi.ruleStatus(record.id as number)
    message.success(record.status === 1 ? '规则已停用' : '规则已启用')
    fetchRules()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  }
}

function handleRuleDelete(record: DmsSettlementRule) {
  Modal.confirm({
    title: '删除计费规则',
    content: `确认删除规则「${record.ruleName}」？删除后该规则不再参与算费（历史结算单已存规则快照，不受影响）。`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await settlementApi.ruleRemove(record.id as number)
        message.success('已删除')
        fetchRules()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    }
  })
}

// ═══ 对账 ═══
const reconcileLoading = ref(false)
const reconcile = ref<SettlementReconcile>({})
const reconcileRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])
const reconcileQuery = reactive<{ targetType?: number; onlyDiff: boolean }>({ targetType: undefined, onlyDiff: false })

const reconcileColumns = [
  { title: '结算单号', dataIndex: 'settlementNo', key: 'settlementNo', width: 170 },
  {
    title: '结算对象类型', dataIndex: 'targetType', key: 'targetType', width: 110,
    customRender: ({ text }: any) => (text === 2 ? '渠道' : '配送员')
  },
  { title: '结算对象', dataIndex: 'targetName', key: 'targetName', width: 120 },
  { title: '周期起', dataIndex: 'periodStart', key: 'periodStart', width: 110 },
  { title: '周期止', dataIndex: 'periodEnd', key: 'periodEnd', width: 110 },
  { title: '单量', dataIndex: 'taskCount', key: 'taskCount', width: 70, align: 'right' as const },
  {
    title: '结算金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' as const,
    customRender: ({ text }: any) => fmtMoney(text)
  },
  { title: '记账凭证号', dataIndex: 'erpVoucherNo', key: 'erpVoucherNo', width: 170 },
  {
    title: '凭证金额', dataIndex: 'voucherAmount', key: 'voucherAmount', width: 110, align: 'right' as const,
    customRender: ({ text }: any) => fmtMoney(text)
  },
  { title: '记账', key: 'accounted', width: 90 },
  { title: '应付/收款', dataIndex: 'settledAmount', key: 'settledAmount', width: 110, align: 'right' as const,
    customRender: ({ text }: any) => fmtMoney(text) },
  { title: '差异', key: 'diff', width: 110, align: 'right' as const },
  { title: '对平', key: 'matched', width: 90 }
]

async function fetchReconcile() {
  reconcileLoading.value = true
  try {
    const params: Record<string, any> = { onlyDiff: reconcileQuery.onlyDiff, targetType: reconcileQuery.targetType }
    if (reconcileRange.value) {
      params.startDate = reconcileRange.value[0].format('YYYY-MM-DD')
      params.endDate = reconcileRange.value[1].format('YYYY-MM-DD')
    }
    reconcile.value = unwrap(await settlementApi.reconcile(params))
  } catch (e) {
    reconcile.value = {}
    message.error('对账失败')
  } finally {
    reconcileLoading.value = false
  }
}

// ═══ 周期报表 ═══
const reportLoading = ref(false)
const report = ref<Record<string, any>>({})
const reportRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('month'), dayjs()])
const reportRiderColumns = [
  { title: '配送员', dataIndex: 'riderName', key: 'riderName' },
  { title: '单量', dataIndex: 'taskCount', key: 'taskCount', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 160, align: 'right' as const,
    customRender: ({ text }: any) => `¥${fmtMoney(text)}` }
]
const reportDayColumns = [
  { title: '日期', dataIndex: 'date', key: 'date' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 160, align: 'right' as const,
    customRender: ({ text }: any) => `¥${fmtMoney(text)}` }
]

async function fetchReport() {
  reportLoading.value = true
  try {
    const params: Record<string, any> = {}
    if (reportRange.value) {
      params.startDate = reportRange.value[0].format('YYYY-MM-DD')
      params.endDate = reportRange.value[1].format('YYYY-MM-DD')
    }
    report.value = unwrap(await settlementApi.report(params))
  } catch (e) {
    report.value = {}
    message.error('报表生成失败')
  } finally {
    reportLoading.value = false
  }
}

// ═══ 导出（真实 xlsx，按当前查询条件） ═══
async function handleExport() {
  loading.value = true
  try {
    const data = unwrap(await settlementApi.page(buildParams(1, 5000)))
    const list: DmsSettlement[] = data.records || []
    if (!list.length) {
      message.warning('没有可导出的数据')
      return
    }
    const cols = columns.filter(c => c.type !== 'checkbox' && c.type !== 'rowNo' && c.type !== 'action')
    const rows = list.map(row => {
      const item: Record<string, any> = {}
      cols.forEach((col: any) => {
        const raw = (row as any)[col.field]
        item[col.title] = col.formatter ? col.formatter(raw, row) : (raw ?? '')
      })
      return item
    })
    const ws = XLSX.utils.json_to_sheet(rows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '配送结算单')
    XLSX.writeFile(wb, `配送结算单_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${list.length} 张结算单`)
  } catch (e) {
    message.error('导出失败')
  } finally {
    loading.value = false
  }
}

function handleError(e: Error) {
  console.error('[配送结算] 页面错误', e)
}

onMounted(() => {
  loadRule()
  fetchData()
})
</script>

<style scoped>
.rule-bar { font-size: 12px; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-grid :deep(.ant-input),
.search-grid :deep(.ant-select) { font-size: 13px; }
.table-footer {
  display: flex; align-items: center; padding: 6px 12px; background: #fafafa;
  border: 1px solid #f0f0f0; border-top: none; font-size: 12px; color: #333;
}
.footer-label { font-weight: 600; min-width: 190px; }
.footer-values { flex: 1; display: flex; gap: 16px; }
.report-wrap { padding: 12px 16px; }
.report-section { margin-top: 18px; }
.report-title { font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.reconcile-table { margin-top: 16px; }
.diff-bad { color: #cf1322; font-weight: 600; }
.detail-table { margin-top: 12px; }
.rule-card { margin-bottom: 12px; background: #fafafa; }
.modal-hint { font-size: 12px; color: #8c8c8c; margin-top: 8px; }
</style>
