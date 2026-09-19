<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        财务期初（设置 → 数据录入 → 财务期初，菜单 70551 / set:initial-finance，单入口）

        对标 ql361「设置 → 期初录入 → 财务期初」实测形态 = **5 个 Tab 的台账**：
          银行现金期初   3 列：科目编号 · 科目名称 · 期初金额
          应付期初       4 列：供应商编号 · 供应商名称 · 应付金额 · 预付金额
          应收期初       5 列：客户编号 · 客户名称 · 默认经手人 · 应收金额 · 预收金额
          固定资产期初   3 列：科目编号 · 科目名称 · 期初金额
          资产负债期初   4 列：科目编号 · 科目名称 · 借贷方向 · 期初金额

        数据源（开发文档 §7.3 路线 B 的两张表，本页为二者唯一维护入口）：
          · 按科目：erp_initial_finance_subject（银行现金 / 固定资产 / 资产负债）
          · 按往来：erp_initial_finance_partner（应付 / 应收）

        ⚠️ 历史 P0（本轮修复）：
          ① 后端零控制器、库零表 → 旧页面 5 个请求全部 404（「整页空壳」）；
          ② 科目下拉请求路径写成 /finance/subject/list（缺 erp/）→ 恒 404、下拉恒空 → 不可提交。
             现统一为 /erp/finance/subject/list（本文件不再出现裸 /finance/subject）。

        ⚠️ 会计期间联动（本轮补齐，开发文档 §5.3/§5.4）：
          · 默认年度取**当前会计年**（/erp/finance/initial/current-year），不再是 new Date().getFullYear()；
          · 目标年度**已全部关账**（会计期间 status 全为 0）时置灰「录入期初 / 保存期初」并给出原因，
            后端在新增/修改/删除时按同一口径硬校验（400 + 同一文案）；
          · 试算平衡弹窗增加「存货对平检查」（存货类科目 14xx 期初 vs 库存期初 Σ 数量×单价），
            只报数不改数、不阻断保存。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：录入期初（按当前 Tab 打开对应字段的弹窗） ═══ -->
        <template #toolbar-left>
          <!-- 关账保护：目标年度已全部关账时置灰「录入期初」，原因见查询区提示条与 tooltip -->
          <a-tooltip
            v-if="btnEnabled('add')"
            :title="yearEditable ? null : yearBlockReason"
            placement="bottom"
          >
            <span>
              <a-button
                type="primary"
                size="small"
                class="btn-add"
                :disabled="!yearEditable"
                @click="openCreate"
              >
                <PlusOutlined /> 录入期初
              </a-button>
            </span>
          </a-tooltip>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 / 保存期初 / 试算平衡（对标实测 5 个） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('print')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-tooltip
              v-if="btnEnabled('save')"
              :title="yearEditable ? null : yearBlockReason"
              placement="bottom"
            >
              <span>
                <a-button
                  type="primary"
                  size="small"
                  :loading="saving"
                  :disabled="!yearEditable"
                  @click="handleSaveSheet"
                >
                  保存期初
                </a-button>
              </span>
            </a-tooltip>
            <a-button
              v-if="btnEnabled('trialBalance')"
              size="small"
              :loading="tbLoading"
              @click="handleTrialBalance"
            >
              试算平衡
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区：横向自适应网格（禁止纵向单列），查询字段按 Tab 维度切换 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- 会计期间联动提示条：当前会计年取值 + 回退提示 + 关账禁止原因 -->
            <div class="period-bar">
              <span class="period-current">当前会计年：{{ accountingYear ?? '-' }}</span>
              <a-alert
                v-if="!yearEditable || yearHint"
                class="period-alert"
                :type="!yearEditable || yearSource === 'SYSTEM_YEAR' ? 'warning' : 'info'"
                show-icon
                :message="!yearEditable ? yearBlockReason : yearHint"
              />
              <!-- 存货对平检查结果（跑过一次「试算平衡」后常驻提示；只提示，不改数、不阻断保存） -->
              <a-alert
                v-if="tbResult?.inventoryCheck && !tbResult.inventoryCheck.matched"
                class="period-alert"
                type="warning"
                show-icon
                :message="tbResult.inventoryCheck.note"
              />
            </div>
            <div
              ref="gridRef"
              class="search-grid"
            >
              <!-- 按科目 Tab：科目编号 / 科目名称 -->
              <div
                v-if="fieldVisible('subjectCode')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.subjectCode"
                  placeholder="科目编号"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('subjectName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.subjectName"
                  placeholder="科目名称"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <!-- 按往来单位 Tab：供应商编号/名称 或 客户编号/名称 -->
              <div
                v-if="fieldVisible('partnerCode')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.partnerCode"
                  :placeholder="activeTabConfig.partnerLabel + '编号'"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('partnerName')"
                class="search-field-item"
              >
                <a-input
                  v-model:value="searchForm.partnerName"
                  :placeholder="activeTabConfig.partnerLabel + '名称'"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="fieldVisible('periodYear')"
                class="search-field-item"
              >
                <a-input-number
                  v-model:value="searchForm.periodYear"
                  placeholder="期初年度"
                  size="small"
                  style="width: 100%"
                  :min="2000"
                  :max="2099"
                />
              </div>
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列；每个 Tab 一套独立 storage-key） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="0"
              :summary-columns="summaryColumns"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
              empty-text="还没有内容哦 请点击「录入期初」开始录入吧！"
            >
              <!-- 科目编号（只读展示；空值显示「-」） -->
              <template #subjectCodeCell="{ record }">
                <span>{{ record.subjectCode || '-' }}</span>
              </template>

              <!-- 科目名称 -->
              <template #subjectNameCell="{ record }">
                <span>{{ record.subjectName || '-' }}</span>
              </template>

              <!-- 往来单位编号 -->
              <template #partnerCodeCell="{ record }">
                <span>{{ record.partnerCode || '-' }}</span>
              </template>

              <!-- 往来单位名称 -->
              <template #partnerNameCell="{ record }">
                <span>{{ record.partnerName || '-' }}</span>
              </template>

              <!-- 默认经手人（仅应收期初；对标为纯文本列，无下拉数据源） -->
              <template #defaultHandlerCell="{ record }">
                <span>{{ record.defaultHandler || '-' }}</span>
              </template>

              <!-- 借贷方向（仅资产负债期初）：可改，配合「保存期初」整体保存 -->
              <template #directionCell="{ record }">
                <a-select
                  v-model:value="record.direction"
                  size="small"
                  style="width: 100%"
                  :options="DIRECTION_OPTIONS"
                />
              </template>

              <!--
                金额列：6 类金额共用一套插槽（每个 Tab 只用其中一个），行内可改，
                改完点工具栏「保存期初」整体保存（对标「保存期初」= 整表保存语义）。
                允许负数（对标实测「银行存款期初 -390,048.78」），故不设 :min。
              -->
              <template #amountCell="{ record, column }">
                <a-input-number
                  v-model:value="record[column.key]"
                  size="small"
                  style="width: 100%"
                  :precision="2"
                  :controls="false"
                  placeholder="0.00"
                />
              </template>

              <!-- 行操作：编辑（弹窗，走 PUT /update）/ 删除（逻辑删除） -->
              <template #actionCell="{ record }">
                <a-space :size="0">
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
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
            </BillDetailTable>
          </div>
        </template>

        <!--
          ═══ 底部：经典分页栏 ═══
          对标实测（应付期初 Tab）是**真实分页**：`第 (1/1) 页`、`共 8 条记录`、`每页显示 N 行`
          → 5 个 Tab 一律用标准分页，不存在「12 行固定矩阵」形态。
        -->
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

      <!--
        ═══ 录入 / 编辑期初弹窗（对标为页内弹窗，非表单页；字段按 Tab 维度不同） ═══
        ⚠️ 说明：对标 5 个 Tab 均**无「页面配置」弹窗**（财务期初.json 的 pageConfig.found = false × 5，
        见开发文档 §8.1 第 10 条）→ 本页刻意**不引入 PageConfigPanel**，避免「发明」对标没有的能力。
      -->
      <a-modal
        v-model:open="modalOpen"
        :title="editingId ? '编辑期初' : '录入期初'"
        :width="620"
        :confirm-loading="saving"
        :ok-button-props="{ disabled: formYearBlocked }"
        ok-text="确定"
        @ok="handleSave"
      >
        <!-- 关账保护：弹窗目标年度已全部关账 → 禁用「确定」并说明原因（后端同样硬校验） -->
        <a-alert
          v-if="formYearBlocked"
          class="tb-alert"
          type="warning"
          show-icon
          :message="formYearBlockReason"
        />
        <a-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top: 16px"
        >
          <!-- ── 按科目 Tab：科目 / 科目名称 / 年度 / [借贷方向] / 期初金额 ── -->
          <template v-if="activeTabConfig.kind === 'subject'">
            <a-form-item
              label="科目"
              name="subjectId"
            >
              <a-select
                v-model:value="form.subjectId"
                placeholder="输入科目编号/名称搜索"
                show-search
                allow-clear
                option-filter-prop="label"
                :loading="subjectLoading"
                :options="subjectOptions"
                :disabled="!!editingId"
                @change="handleSubjectChange"
              />
            </a-form-item>
            <a-form-item label="科目名称">
              <a-input
                :value="form.subjectName"
                disabled
              />
            </a-form-item>
            <a-form-item
              label="期初年度"
              name="periodYear"
            >
              <a-input-number
                v-model:value="form.periodYear"
                :min="2000"
                :max="2099"
                style="width: 100%"
              />
            </a-form-item>
            <!-- 借贷方向仅资产负债期初有（银行现金/固定资产两个 Tab 对标无此列） -->
            <a-form-item
              v-if="activeTabConfig.key === 'balanceSheet'"
              label="借贷方向"
              name="direction"
            >
              <a-select
                v-model:value="form.direction"
                :options="DIRECTION_OPTIONS"
              />
            </a-form-item>
            <a-form-item
              label="期初金额"
              name="openingAmount"
            >
              <a-input-number
                v-model:value="form.openingAmount"
                :precision="2"
                placeholder="保留 2 位小数，允许负数"
                style="width: 100%"
              />
            </a-form-item>
          </template>

          <!-- ── 按往来单位 Tab：往来单位 / 编号名称 / 年度 / [默认经手人] / 金额 ── -->
          <template v-else>
            <a-form-item
              :label="activeTabConfig.partnerLabel"
              name="partnerId"
            >
              <a-select
                v-model:value="form.partnerId"
                :placeholder="'输入' + activeTabConfig.partnerLabel + '编号/名称搜索'"
                show-search
                allow-clear
                option-filter-prop="label"
                :loading="partnerLoading"
                :options="partnerOptions"
                :disabled="!!editingId"
                @change="handlePartnerChange"
              />
            </a-form-item>
            <a-form-item :label="activeTabConfig.partnerLabel + '编号'">
              <a-input
                :value="form.partnerCode"
                disabled
              />
            </a-form-item>
            <a-form-item
              label="期初年度"
              name="periodYear"
            >
              <a-input-number
                v-model:value="form.periodYear"
                :min="2000"
                :max="2099"
                style="width: 100%"
              />
            </a-form-item>
            <!-- 默认经手人仅应收期初有（对标应付 Tab 无此列） -->
            <a-form-item
              v-if="activeTabConfig.key === 'receivable'"
              label="默认经手人"
            >
              <a-input
                v-model:value="form.defaultHandler"
                placeholder="请输入"
                :maxlength="50"
              />
            </a-form-item>
            <template v-if="activeTabConfig.key === 'payable'">
              <a-form-item
                label="应付金额"
                name="payableAmount"
              >
                <a-input-number
                  v-model:value="form.payableAmount"
                  :precision="2"
                  placeholder="保留 2 位小数，允许负数"
                  style="width: 100%"
                />
              </a-form-item>
              <a-form-item label="预付金额">
                <a-input-number
                  v-model:value="form.prepayAmount"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </template>
            <template v-else>
              <a-form-item
                label="应收金额"
                name="receivableAmount"
              >
                <a-input-number
                  v-model:value="form.receivableAmount"
                  :precision="2"
                  placeholder="保留 2 位小数，允许负数"
                  style="width: 100%"
                />
              </a-form-item>
              <a-form-item label="预收金额">
                <a-input-number
                  v-model:value="form.advanceAmount"
                  :precision="2"
                  style="width: 100%"
                />
              </a-form-item>
            </template>
          </template>
        </a-form>
      </a-modal>

      <!-- ═══ 试算平衡结果弹窗（对标工具栏「试算平衡」） ═══ -->
      <a-modal
        v-model:open="tbVisible"
        title="期初试算平衡"
        :footer="null"
        :width="560"
      >
        <a-descriptions
          :column="1"
          bordered
          size="small"
        >
          <a-descriptions-item label="统计年度">
            {{ tbResult?.periodYear ?? '全部年度' }}
          </a-descriptions-item>
          <a-descriptions-item label="借方合计">
            {{ formatAmount(tbResult?.debitTotal) }}
          </a-descriptions-item>
          <a-descriptions-item label="贷方合计">
            {{ formatAmount(tbResult?.creditTotal) }}
          </a-descriptions-item>
          <a-descriptions-item label="借贷差额">
            {{ formatAmount(tbResult?.difference) }}
          </a-descriptions-item>
          <a-descriptions-item label="参与行数">
            {{ tbResult?.rowCount ?? 0 }}
          </a-descriptions-item>
        </a-descriptions>
        <a-alert
          class="tb-alert"
          :type="tbResult?.balanced ? 'success' : 'warning'"
          show-icon
          :message="tbResult?.balanced ? '借贷平衡' : '借贷不平衡，请检查期初金额与借贷方向'"
          description="口径：仅统计银行现金 / 固定资产 / 资产负债三类「按科目」期初；银现与固资按借方计。借贷不平衡不阻断保存（是否强制平衡需产品裁定）。"
        />

        <!-- 存货对平检查：财务侧存货类科目期初 vs 库存期初 Σ(数量×单价)，只报数不改数 -->
        <template v-if="tbResult?.inventoryCheck">
          <a-descriptions
            class="tb-alert"
            title="存货对平检查"
            :column="1"
            bordered
            size="small"
          >
            <a-descriptions-item :label="`财务侧（存货类科目 ${tbResult.inventoryCheck.subjectCodePrefix}xx 期初）`">
              {{ formatAmount(tbResult.inventoryCheck.financeAmount) }}
              （{{ tbResult.inventoryCheck.financeRowCount }} 行）
            </a-descriptions-item>
            <a-descriptions-item label="库存侧（Σ 数量 × 成本单价）">
              {{ formatAmount(tbResult.inventoryCheck.stockAmount) }}
              （{{ tbResult.inventoryCheck.stockRowCount }} 行）
            </a-descriptions-item>
            <a-descriptions-item label="差额（财务 − 库存）">
              {{ formatAmount(tbResult.inventoryCheck.difference) }}
            </a-descriptions-item>
          </a-descriptions>
          <a-alert
            class="tb-alert"
            :type="tbResult.inventoryCheck.matched ? 'success' : 'warning'"
            show-icon
            :message="tbResult.inventoryCheck.matched ? '存货已对平' : '存货未对平'"
            :description="tbResult.inventoryCheck.note"
          />
        </template>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import * as XLSX from 'xlsx'
import {
  PlusOutlined,
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
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import {
  initialFinanceApi,
  type InitialFinanceQuery,
  type InitialFinanceType,
  type PeriodStatusResult,
  type TrialBalanceResult,
} from '@/api/set/initial'
import { accountSubjectApi } from '@/api/finance'
import request from '@/utils/request'

defineOptions({ name: 'SetInitialFinance' })

// ═══════════════════════════════════════ 常量 ═══════════════════════════════════════

/** 借贷方向（逐字取自对标/旧实现：DEBIT 借方 / CREDIT 贷方） */
const DIRECTION_OPTIONS = [
  { value: 'DEBIT', label: '借方' },
  { value: 'CREDIT', label: '贷方' },
]

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

/** 单个 Tab 的完整配置（列 + 查询条件 + 功能按钮，三套独立） */
interface TabConfig {
  /** Tab key（同时用于 storage-key 后缀） */
  key: string
  /** Tab 标签（逐字取自对标） */
  label: string
  /** 后端期初类型 */
  initialType: InitialFinanceType
  /** 维度：subject=按科目 / partner=按往来单位（决定用哪组端点与列） */
  kind: 'subject' | 'partner'
  /** 往来单位称谓（partner 维度用） */
  partnerLabel: string
  /** 列定义（含 rowNo 齿轮列与操作列） */
  columns: DetailColumnConfig[]
  /** 与本 Tab 匹配的查询条件 */
  queryFields: QueryFieldSetting[]
  /** 本 Tab 的功能按钮 */
  buttons: FunctionButtonSetting[]
}

/**
 * 工具栏按钮（对标实测：录入期初 + 刷新 · 打印(F8) · 导出 · 保存期初 · 试算平衡）。
 * 按 Tab 各自声明一份（不用共享数组，避免后续按 Tab 差异化时互相污染）。
 */
function sheetButtons(): FunctionButtonSetting[] {
  return [
    { key: 'add', label: '录入期初', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'print', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'save', label: '保存期初', enabled: true },
    { key: 'trialBalance', label: '试算平衡', enabled: true },
  ]
}

/** 科目维度共用的列（科目编号 / 科目名称 / [借贷方向] / 期初金额） */
function subjectColumns(withDirection: boolean): DetailColumnConfig[] {
  const cols: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
    { key: 'subjectCode', title: '科目编号', type: 'slot', slotName: 'subjectCodeCell', width: 140 },
    { key: 'subjectName', title: '科目名称', type: 'slot', slotName: 'subjectNameCell', width: 240 },
  ]
  if (withDirection) {
    cols.push({ key: 'direction', title: '借贷方向', type: 'slot', slotName: 'directionCell', width: 110, align: 'center' })
  }
  cols.push({ key: 'openingAmount', title: '期初金额', type: 'slot', slotName: 'amountCell', width: 160, align: 'right' })
  return cols
}

/** 往来单位维度共用的列（编号 / 名称 / [默认经手人] / 金额列） */
function partnerColumns(kind: 'payable' | 'receivable', partnerLabel: string): DetailColumnConfig[] {
  const cols: DetailColumnConfig[] = [
    { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
    { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
    { key: 'partnerCode', title: partnerLabel + '编号', type: 'slot', slotName: 'partnerCodeCell', width: 140 },
    { key: 'partnerName', title: partnerLabel + '名称', type: 'slot', slotName: 'partnerNameCell', width: 220 },
  ]
  if (kind === 'receivable') {
    cols.push({ key: 'defaultHandler', title: '默认经手人', type: 'slot', slotName: 'defaultHandlerCell', width: 120 })
    cols.push({ key: 'receivableAmount', title: '应收金额', type: 'slot', slotName: 'amountCell', width: 150, align: 'right' })
    cols.push({ key: 'advanceAmount', title: '预收金额', type: 'slot', slotName: 'amountCell', width: 150, align: 'right' })
  } else {
    cols.push({ key: 'payableAmount', title: '应付金额', type: 'slot', slotName: 'amountCell', width: 150, align: 'right' })
    cols.push({ key: 'prepayAmount', title: '预付金额', type: 'slot', slotName: 'amountCell', width: 150, align: 'right' })
  }
  return cols
}

/** 5 个 Tab（顺序、标签逐字取自对标实测；序号即对标 Tab 排列） */
const TABS: TabConfig[] = [
  {
    key: 'bankCash',
    label: '银行现金期初',
    initialType: 'BANK_CASH',
    kind: 'subject',
    partnerLabel: '',
    columns: subjectColumns(false),
    queryFields: [
      { key: 'subjectCode', label: '科目编号', visible: true },
      { key: 'subjectName', label: '科目名称', visible: true },
      { key: 'periodYear', label: '期初年度', visible: true },
    ],
    buttons: sheetButtons(),
  },
  {
    key: 'payable',
    label: '应付期初',
    initialType: 'PAYABLE',
    kind: 'partner',
    partnerLabel: '供应商',
    columns: partnerColumns('payable', '供应商'),
    queryFields: [
      { key: 'partnerCode', label: '供应商编号', visible: true },
      { key: 'partnerName', label: '供应商名称', visible: true },
      { key: 'periodYear', label: '期初年度', visible: true },
    ],
    buttons: sheetButtons(),
  },
  {
    key: 'receivable',
    label: '应收期初',
    initialType: 'RECEIVABLE',
    kind: 'partner',
    partnerLabel: '客户',
    columns: partnerColumns('receivable', '客户'),
    queryFields: [
      { key: 'partnerCode', label: '客户编号', visible: true },
      { key: 'partnerName', label: '客户名称', visible: true },
      { key: 'periodYear', label: '期初年度', visible: true },
    ],
    buttons: sheetButtons(),
  },
  {
    key: 'fixedAsset',
    label: '固定资产期初',
    initialType: 'FIXED_ASSET',
    kind: 'subject',
    partnerLabel: '',
    columns: subjectColumns(false),
    queryFields: [
      { key: 'subjectCode', label: '科目编号', visible: true },
      { key: 'subjectName', label: '科目名称', visible: true },
      { key: 'periodYear', label: '期初年度', visible: true },
    ],
    buttons: sheetButtons(),
  },
  {
    key: 'balanceSheet',
    label: '资产负债期初',
    initialType: 'BALANCE_SHEET',
    kind: 'subject',
    partnerLabel: '',
    columns: subjectColumns(true),
    queryFields: [
      { key: 'subjectCode', label: '科目编号', visible: true },
      { key: 'subjectName', label: '科目名称', visible: true },
      { key: 'periodYear', label: '期初年度', visible: true },
    ],
    buttons: sheetButtons(),
  },
]

// ═══════════════════════════════════════ 状态 ═══════════════════════════════════════

const activeTab = ref<string>(TABS[0].key)
const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

/** 查询条件（年度不设默认：默认展示该 Tab 全部年度，避免新录数据「查不到」） */
const searchForm = reactive<Record<string, any>>({
  subjectCode: '',
  subjectName: '',
  partnerCode: '',
  partnerName: '',
  periodYear: undefined,
})

// ═══════════════════════════ 会计期间联动（当前会计年 + 关账保护） ═══════════════════════════

/**
 * 当前会计年（来自后端 /current-year，取代原先写死的 new Date().getFullYear()）。
 * 后端回退链：开启中且今天落在期间内的年度 → 该租户最大会计年度 → 服务器系统年。
 */
const accountingYear = ref<number | null>(null)
/** 回退提示（后端返回 hint 时展示；如「回退为系统年」） */
const yearHint = ref('')
/** 取值来源（SYSTEM_YEAR 时用 warning 提示，其余为 info） */
const yearSource = ref<string>('')

/** 关账状态按「当前生效年度」判定：查询条件有年度用它，否则用当前会计年 */
const effectiveYear = computed<number | undefined>(
  () => searchForm.periodYear || accountingYear.value || undefined,
)
/** 生效年度的关账状态（置灰工具栏「录入期初 / 保存期初」用） */
const yearStatus = ref<PeriodStatusResult | null>(null)
/** 允许录入/修改（状态未取到时按允许处理，最终以后端硬校验为准） */
const yearEditable = computed(() => yearStatus.value?.editable !== false)
/** 禁止原因（按钮 tooltip + 提示条共用同一文案） */
const yearBlockReason = computed(() => yearStatus.value?.reason || '')

/** 拉取当前会计年 */
async function loadAccountingYear() {
  try {
    const res: any = await initialFinanceApi.currentYear()
    const data = res || {}
    accountingYear.value = data.periodYear != null ? Number(data.periodYear) : null
    yearSource.value = data.source || ''
    yearHint.value = data.hint || ''
  } catch (error) {
    console.warn('[财务期初] 当前会计年加载失败，回退系统年', error)
    accountingYear.value = new Date().getFullYear()
    yearSource.value = 'SYSTEM_YEAR'
    yearHint.value = '当前会计年接口不可用，年度已回退为浏览器系统年'
  }
}

/** 按年度刷新关账状态（后端为唯一权威，前端只用于置灰与提示） */
async function refreshYearStatus(year?: number) {
  const target = year || effectiveYear.value
  if (!target) {
    yearStatus.value = null
    return
  }
  try {
    const res: any = await initialFinanceApi.periodStatus(target)
    yearStatus.value = res || null
  } catch (error) {
    console.warn('[财务期初] 关账状态加载失败', error)
    yearStatus.value = null
  }
}

// 生效年度变化（查询条件改年度 / 当前会计年加载完成）→ 重算关账状态
watch(effectiveYear, (year) => {
  refreshYearStatus(year)
})

/** 当前 Tab 配置 */
const activeTabConfig = computed<TabConfig>(() => {
  return TABS.find(t => t.key === activeTab.value) || TABS[0]
})

/**
 * 每个 Tab 独立一套列配置存储（个人 + 全局）。
 * BillDetailTable 内部对 storage-key 有 watch → 切 Tab 会自动按新 key 重载列显隐，
 * 不会出现「切 Tab 后沿用上一 Tab 列设置」的多视图陷阱。
 */
const tableStorageKey = computed(() => `set-initial-finance-columns-${activeTab.value}`)

const activeColumns = computed<DetailColumnConfig[]>(() => activeTabConfig.value.columns)

/**
 * 合计行：对标只在应付/应收两个 Tab 有（开发文档 §8.1 第 12 条：
 * 应付「合计 664,900.27 110,727」、应收「合计 738,820.1 25,301」）。
 * 口径 = **当前页合计**（对标实测数据量为单页全量，故页内合计即全量合计）。
 */
const summaryColumns = computed(() => {
  const tab = activeTabConfig.value
  if (tab.kind !== 'partner' || !tableData.value.length) return []
  // 金额按「分」累加再除回，避免浮点误差（如 0.1 + 0.2）
  const sum = (key: string) => {
    let cents = 0
    for (const row of tableData.value) {
      const value = Number(row?.[key])
      if (Number.isFinite(value)) cents += Math.round(value * 100)
    }
    return cents / 100
  }
  return tab.key === 'payable'
    ? [{ key: 'payableAmount', value: sum('payableAmount') }, { key: 'prepayAmount', value: sum('prepayAmount') }]
    : [{ key: 'receivableAmount', value: sum('receivableAmount') }, { key: 'advanceAmount', value: sum('advanceAmount') }]
})

// ═══════════════════════════════════════ 下拉数据源 ═══════════════════════════════════════

const subjectOptions = ref<{ label: string; value: string }[]>([])
const subjectLoading = ref(false)
const partnerOptions = ref<{ label: string; value: string; raw: any }[]>([])
const partnerLoading = ref(false)

/**
 * 加载会计科目下拉。
 * ⚠️ 历史 P0：旧实现请求 `/finance/subject/list`（缺 `erp/` 段）→ 404 → 下拉恒空 →
 * 必填项永远无法满足 → 页面不可提交。正确路径 = `/erp/finance/subject/list`
 * （AccountSubjectController.java:36,50-53）。
 */
async function loadSubjectOptions() {
  subjectLoading.value = true
  try {
    const res: any = await accountSubjectApi.getList()
    const list: any[] = Array.isArray(res) ? res : (res?.data ?? [])
    subjectOptions.value = list.map((s: any) => ({
      label: s.subjectCode ? `${s.subjectCode} - ${s.subjectName}` : (s.subjectName || ''),
      value: String(s.id),
    }))
  } catch (error) {
    console.warn('[财务期初] 会计科目下拉加载失败', error)
    subjectOptions.value = []
  } finally {
    subjectLoading.value = false
  }
}

/**
 * 加载往来单位下拉（供应商 / 客户）。
 *
 * ⚠️ **数据源真相（2026-09-18 已查证）**：实际落在现役往来单位表 **`biz_party`**
 * （devdb 实测 152 行），由 `/erp/md/customer/list` 提供（`MdCustomerController` 的注释即
 * 「使用 biz_party 表，接管原 PartnerController(erp_partner) 全部功能」）。
 * 旧文档里写的 `erp_partner` 是**历史空表**（devdb 实测 0 行，`SystemRebuildService` 亦登记
 * 「客户/供应商主数据现落在 biz_party（erp_partner 为历史表，已无写入）」）→ 本页只走前者。
 *
 * 按 partnerType 过滤并只取启用中的（optionsApi 的通用 helpers 不带类型过滤，
 * 直接取会把供应商混进客户下拉，故此处显式传参）。
 */
async function loadPartnerOptions() {
  partnerLoading.value = true
  try {
    const partnerType = activeTabConfig.value.key === 'payable' ? 'supplier' : 'customer'
    const res: any = await request.get('/erp/md/customer/list', {
      partnerType,
      status: 'ENABLED',
      pageSize: 500,
    })
    const list: any[] = Array.isArray(res) ? res : (res?.data ?? [])
    partnerOptions.value = list.map((p: any) => ({
      label: p.partnerCode ? `${p.partnerCode} - ${p.partnerName || ''}` : (p.partnerName || ''),
      value: String(p.id),
      raw: p,
    }))
  } catch (error) {
    console.warn('[财务期初] 往来单位下拉加载失败', error)
    partnerOptions.value = []
  } finally {
    partnerLoading.value = false
  }
}

/**
 * 编辑回填时保证「已选科目」一定在下拉选项里。
 * 科目列表接口默认只返回启用中的科目（AccountSubjectQuery），若该科目后来被停用，
 * 下拉缺选项会让已选值显示为裸 id。
 */
function ensureSubjectOption(record: any) {
  if (!record.subjectId) return
  const id = String(record.subjectId)
  if (subjectOptions.value.some(o => String(o.value) === id)) return
  subjectOptions.value = [
    {
      label: record.subjectCode
        ? `${record.subjectCode} - ${record.subjectName || ''}`
        : (record.subjectName || id),
      value: id,
    },
    ...subjectOptions.value,
  ]
}

/** 选中科目后回填编号/名称快照 */
function handleSubjectChange(value: any) {
  const option = subjectOptions.value.find(o => String(o.value) === String(value))
  if (!option) {
    form.subjectId = undefined
    form.subjectCode = ''
    form.subjectName = ''
    return
  }
  form.subjectId = String(value)
  // 下拉 label 形如「1001 - 库存现金」，回填时按首个 " - " 拆出编号与名称
  const idx = option.label.indexOf(' - ')
  form.subjectCode = idx > -1 ? option.label.slice(0, idx) : ''
  form.subjectName = idx > -1 ? option.label.slice(idx + 3) : option.label
}

/** 编辑回填时保证「已选往来单位」一定在下拉选项里（同 ensureSubjectOption：停用单位不会被接口返回） */
function ensurePartnerOption(record: any) {
  if (!record.partnerId) return
  const id = String(record.partnerId)
  if (partnerOptions.value.some(o => String(o.value) === id)) return
  partnerOptions.value = [
    {
      label: record.partnerCode
        ? `${record.partnerCode} - ${record.partnerName || ''}`
        : (record.partnerName || id),
      value: id,
      raw: { id, partnerCode: record.partnerCode, partnerName: record.partnerName },
    },
    ...partnerOptions.value,
  ]
}

/** 选中往来单位后回填编号/名称快照 */
function handlePartnerChange(value: any) {
  const option = partnerOptions.value.find(o => String(o.value) === String(value))
  if (!option) {
    form.partnerId = undefined
    form.partnerCode = ''
    form.partnerName = ''
    return
  }
  form.partnerId = String(value)
  form.partnerCode = option.raw?.partnerCode || ''
  form.partnerName = option.raw?.partnerName || ''
}

// ═══════════════════════════════════════ 列表查询 ═══════════════════════════════════════

function buildQueryParams(): InitialFinanceQuery {
  const tab = activeTabConfig.value
  const params: InitialFinanceQuery = {
    initialType: tab.initialType,
    periodYear: searchForm.periodYear || undefined,
  }
  if (tab.kind === 'subject') {
    params.subjectCode = searchForm.subjectCode || undefined
    params.subjectName = searchForm.subjectName || undefined
  } else {
    params.partnerCode = searchForm.partnerCode || undefined
    params.partnerName = searchForm.partnerName || undefined
  }
  return params
}

/** 加载当前 Tab 的期初列表（保存后也必须回读本方法） */
async function load() {
  loading.value = true
  try {
    const tab = activeTabConfig.value
    const params = {
      ...buildQueryParams(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    const res: any = tab.kind === 'subject'
      ? await initialFinanceApi.subjectPage(params)
      : await initialFinanceApi.partnerPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[财务期初] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  load()
}

function handleReset() {
  searchForm.subjectCode = ''
  searchForm.subjectName = ''
  searchForm.partnerCode = ''
  searchForm.partnerName = ''
  searchForm.periodYear = undefined
  handleSearch()
}

function handleRefresh() {
  load()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  load()
}

/**
 * 切换 Tab：换 storage-key（列配置按 Tab 重载）**并且必须重新加载数据**，
 * 否则会出现「切了 Tab 还是上一 Tab 的数据」。
 */
function handleTabChange(key: string) {
  if (key === activeTab.value) return
  activeTab.value = key
  pagination.current = 1
  pagination.total = 0
  tableData.value = []
  // 查询条件按 Tab 独立：切换时清空，避免把科目条件带到往来 Tab
  searchForm.subjectCode = ''
  searchForm.subjectName = ''
  searchForm.partnerCode = ''
  searchForm.partnerName = ''
  searchForm.periodYear = undefined
  load()
}

// ═══════════════════════════════════════ 功能按钮开关 ═══════════════════════════════════════

function fieldVisible(key: string): boolean {
  return activeTabConfig.value.queryFields.find(f => f.key === key)?.visible !== false
}

function btnEnabled(key: string): boolean {
  return activeTabConfig.value.buttons.find(b => b.key === key)?.enabled !== false
}

// ═══════════════════════════════════════ 录入 / 编辑弹窗 ═══════════════════════════════════════

const formRef = ref()
const modalOpen = ref(false)
/** 编辑中的期初行 ID（雪花 ID 一律按字符串处理，禁止 Number(id)） */
const editingId = ref<string | null>(null)

/** 弹窗年度的关账状态（目标年度已全部关账时禁用「确定」并给出原因） */
const formYearStatus = ref<PeriodStatusResult | null>(null)
const formYearBlocked = computed(() => formYearStatus.value?.editable === false)
const formYearBlockReason = computed(() => formYearStatus.value?.reason || '')

/** 按弹窗当前年度刷新关账状态 */
async function refreshFormYearStatus() {
  const year = form.periodYear ? Number(form.periodYear) : undefined
  if (!year) {
    formYearStatus.value = null
    return
  }
  try {
    const res: any = await initialFinanceApi.periodStatus(year)
    formYearStatus.value = res || null
  } catch (error) {
    console.warn('[财务期初] 关账状态加载失败', error)
    formYearStatus.value = null
  }
}

/** 默认年度 = 当前会计年（会计年接口未取到时才回退浏览器系统年） */
const emptyForm = () => ({
  periodYear: (accountingYear.value ?? new Date().getFullYear()) as number | undefined,
  subjectId: undefined as string | undefined,
  subjectCode: '',
  subjectName: '',
  direction: 'DEBIT' as InitialFinanceDirection,
  openingAmount: undefined as number | undefined,
  partnerId: undefined as string | undefined,
  partnerCode: '',
  partnerName: '',
  defaultHandler: '',
  payableAmount: undefined as number | undefined,
  prepayAmount: undefined as number | undefined,
  receivableAmount: undefined as number | undefined,
  advanceAmount: undefined as number | undefined,
})
const form = reactive(emptyForm())

// 弹窗里改年度 = 改目标期间 → 立即重算关账状态（决定「确定」是否可用）
watch(() => form.periodYear, () => {
  refreshFormYearStatus()
})

/** 校验规则按 Tab 维度不同（往来单位弹窗没有「科目」，反之亦然） */
const rules = computed<Record<string, any>>(() => {
  const tab = activeTabConfig.value
  const base: Record<string, any> = {
    periodYear: [{ required: true, message: '请选择期初年度', trigger: 'change' }],
  }
  if (tab.kind === 'subject') {
    base.subjectId = [{ required: true, message: '请选择科目', trigger: 'change' }]
    base.openingAmount = [{ required: true, message: '请输入期初金额', trigger: 'blur' }]
    if (tab.key === 'balanceSheet') {
      base.direction = [{ required: true, message: '请选择借贷方向', trigger: 'change' }]
    }
    return base
  }
  base.partnerId = [{ required: true, message: `请选择${tab.partnerLabel}`, trigger: 'change' }]
  if (tab.key === 'payable') {
    base.payableAmount = [{ required: true, message: '请输入应付金额', trigger: 'blur' }]
  } else {
    base.receivableAmount = [{ required: true, message: '请输入应收金额', trigger: 'blur' }]
  }
  return base
})

function resetForm(data?: Partial<ReturnType<typeof emptyForm>>) {
  Object.assign(form, emptyForm(), data || {})
}

async function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
  // 打开弹窗即保证下拉有真实数据（历史 P0：下拉恒空 → 必填项无法满足 → 不可提交）
  if (activeTabConfig.value.kind === 'subject') {
    await loadSubjectOptions()
  } else {
    await loadPartnerOptions()
  }
  // 关账保护：目标年度已全部关账则禁用「确定」并显示原因
  await refreshFormYearStatus()
}

async function openEdit(record: any) {
  editingId.value = String(record.id)
  resetForm({
    periodYear: record.periodYear ? Number(record.periodYear) : undefined,
    subjectId: record.subjectId ? String(record.subjectId) : undefined,
    subjectCode: record.subjectCode || '',
    subjectName: record.subjectName || '',
    direction: record.direction || 'DEBIT',
    openingAmount: toNumberOrUndefined(record.openingAmount),
    partnerId: record.partnerId ? String(record.partnerId) : undefined,
    partnerCode: record.partnerCode || '',
    partnerName: record.partnerName || '',
    defaultHandler: record.defaultHandler || '',
    payableAmount: toNumberOrUndefined(record.payableAmount),
    prepayAmount: toNumberOrUndefined(record.prepayAmount),
    receivableAmount: toNumberOrUndefined(record.receivableAmount),
    advanceAmount: toNumberOrUndefined(record.advanceAmount),
  })
  modalOpen.value = true
  if (activeTabConfig.value.kind === 'subject') {
    await loadSubjectOptions()
    ensureSubjectOption(record)
  } else {
    await loadPartnerOptions()
    ensurePartnerOption(record)
  }
  // 关账保护：编辑的行若落在已关账年度，禁用「确定」并显示原因（后端同样硬校验）
  await refreshFormYearStatus()
}

function toNumberOrUndefined(value: any): number | undefined {
  if (value === null || value === undefined || value === '') return undefined
  const num = Number(value)
  return Number.isFinite(num) ? num : undefined
}

/** 新增：POST /save（后端收数组，单条也包成数组）；编辑：PUT /update（科目/往来单位不可改） */
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const tab = activeTabConfig.value
  saving.value = true
  try {
    if (tab.kind === 'subject') {
      const payload = {
        initialType: tab.initialType,
        periodYear: form.periodYear,
        subjectId: form.subjectId,
        subjectCode: form.subjectCode,
        subjectName: form.subjectName,
        // 借贷方向只在资产负债 Tab 生效，其余类型后端会强制置空
        direction: tab.key === 'balanceSheet' ? form.direction : undefined,
        openingAmount: form.openingAmount,
      }
      if (editingId.value) {
        await initialFinanceApi.subjectUpdate({ ...payload, id: editingId.value })
        message.success('更新成功')
      } else {
        await initialFinanceApi.subjectSave([payload])
        message.success('录入成功')
      }
    } else {
      const payload = {
        initialType: tab.initialType,
        periodYear: form.periodYear,
        partnerId: form.partnerId,
        partnerCode: form.partnerCode,
        partnerName: form.partnerName,
        defaultHandler: tab.key === 'receivable' ? form.defaultHandler : undefined,
        payableAmount: tab.key === 'payable' ? form.payableAmount : undefined,
        prepayAmount: tab.key === 'payable' ? form.prepayAmount : undefined,
        receivableAmount: tab.key === 'receivable' ? form.receivableAmount : undefined,
        advanceAmount: tab.key === 'receivable' ? form.advanceAmount : undefined,
      }
      if (editingId.value) {
        await initialFinanceApi.partnerUpdate({ ...payload, id: editingId.value })
        message.success('更新成功')
      } else {
        await initialFinanceApi.partnerSave([payload])
        message.success('录入成功')
      }
    }
    modalOpen.value = false
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/**
 * 保存期初（对标工具栏按钮）：把当前 Tab 表格中的**全部行**整体保存。
 * 行内修改过的金额/借贷方向在这里落库；未修改的行按原值重写（幂等）。
 * 后端 save 按 id 判定新增/更新，故与「录入期初」共用同一端点。
 */
async function handleSaveSheet() {
  const rows = (tableData.value || []).filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('当前没有可保存的期初数据')
    return
  }
  const tab = activeTabConfig.value
  saving.value = true
  try {
    if (tab.kind === 'subject') {
      await initialFinanceApi.subjectSave(rows.map(r => ({
        id: String(r.id),
        initialType: tab.initialType,
        periodYear: r.periodYear ? Number(r.periodYear) : undefined,
        subjectId: r.subjectId ? String(r.subjectId) : undefined,
        subjectCode: r.subjectCode,
        subjectName: r.subjectName,
        direction: tab.key === 'balanceSheet' ? r.direction : undefined,
        openingAmount: toNumberOrUndefined(r.openingAmount),
      })))
    } else {
      await initialFinanceApi.partnerSave(rows.map(r => ({
        id: String(r.id),
        initialType: tab.initialType,
        periodYear: r.periodYear ? Number(r.periodYear) : undefined,
        partnerId: r.partnerId ? String(r.partnerId) : undefined,
        partnerCode: r.partnerCode,
        partnerName: r.partnerName,
        defaultHandler: r.defaultHandler,
        payableAmount: toNumberOrUndefined(r.payableAmount),
        prepayAmount: toNumberOrUndefined(r.prepayAmount),
        receivableAmount: toNumberOrUndefined(r.receivableAmount),
        advanceAmount: toNumberOrUndefined(r.advanceAmount),
      })))
    }
    message.success(`已保存 ${rows.length} 条期初`)
    // 保存后回读（以库中值为准）
    await load()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══════════════════════════════════════ 删除 ═══════════════════════════════════════

function handleDelete(record: any) {
  const tab = activeTabConfig.value
  const who = tab.kind === 'subject'
    ? `科目「${record.subjectName || record.subjectCode || ''}」`
    : `${tab.partnerLabel}「${record.partnerName || record.partnerCode || ''}」`
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除${who} ${record.periodYear || ''} 年度的期初数据吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        if (tab.kind === 'subject') {
          await initialFinanceApi.subjectRemove(String(record.id))
        } else {
          await initialFinanceApi.partnerRemove(String(record.id))
        }
        message.success('删除成功')
        await load()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '删除失败')
      }
    },
  })
}

// ═══════════════════════════════════════ 试算平衡 ═══════════════════════════════════════

const tbVisible = ref(false)
const tbLoading = ref(false)
const tbResult = ref<TrialBalanceResult | null>(null)

async function handleTrialBalance() {
  tbLoading.value = true
  try {
    const res: any = await initialFinanceApi.trialBalance(searchForm.periodYear || undefined)
    tbResult.value = res || null
    tbVisible.value = true
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '试算平衡失败')
  } finally {
    tbLoading.value = false
  }
}

// ═══════════════════════════════════════ 导出（真实 xlsx） ═══════════════════════════════════════

async function handleExport() {
  const tab = activeTabConfig.value
  exporting.value = true
  try {
    const res: any = await initialFinanceApi.export(buildQueryParams())
    const rows: any[] = Array.isArray(res) ? res : (res?.data ?? [])
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const sheetRows = rows.map(row => tab.kind === 'subject'
      ? {
          科目编号: row.subjectCode || '',
          科目名称: row.subjectName || '',
          ...(tab.key === 'balanceSheet' ? { 借贷方向: row.direction === 'CREDIT' ? '贷方' : '借方' } : {}),
          期初金额: formatAmount(row.openingAmount),
        }
      : {
          [tab.partnerLabel + '编号']: row.partnerCode || '',
          [tab.partnerLabel + '名称']: row.partnerName || '',
          ...(tab.key === 'receivable'
            ? { 默认经手人: row.defaultHandler || '', 应收金额: formatAmount(row.receivableAmount), 预收金额: formatAmount(row.advanceAmount) }
            : { 应付金额: formatAmount(row.payableAmount), 预付金额: formatAmount(row.prepayAmount) }),
        })
    const ws = XLSX.utils.json_to_sheet(sheetRows)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, tab.label)
    XLSX.writeFile(wb, `${tab.label}_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
    message.success(`已导出 ${rows.length} 条`)
  } catch (error: any) {
    console.error('[财务期初] 导出失败', error)
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══════════════════════════════════════ 打印(F8) ═══════════════════════════════════════

function formatAmount(value: any): string {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  return Number.isFinite(num) ? num.toFixed(2) : String(value)
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

/** 打印当前 Tab 的当前页（与屏幕同口径；F8 亦触发本方法） */
function handlePrint() {
  const tab = activeTabConfig.value
  const rows = (tableData.value || []).filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const head = tab.kind === 'subject'
    ? `<th>#</th><th>科目编号</th><th>科目名称</th>${tab.key === 'balanceSheet' ? '<th>借贷方向</th>' : ''}<th>期初金额</th>`
    : `<th>#</th><th>${escapeHtml(tab.partnerLabel)}编号</th><th>${escapeHtml(tab.partnerLabel)}名称</th>`
      + (tab.key === 'receivable'
        ? '<th>默认经手人</th><th>应收金额</th><th>预收金额</th>'
        : '<th>应付金额</th><th>预付金额</th>')
  const body = rows.map((r: any, i: number) => {
    const cells = tab.kind === 'subject'
      ? `<td>${escapeHtml(r.subjectCode)}</td><td>${escapeHtml(r.subjectName)}</td>`
        + (tab.key === 'balanceSheet' ? `<td>${r.direction === 'CREDIT' ? '贷方' : '借方'}</td>` : '')
        + `<td style="text-align:right">${escapeHtml(formatAmount(r.openingAmount))}</td>`
      : `<td>${escapeHtml(r.partnerCode)}</td><td>${escapeHtml(r.partnerName)}</td>`
        + (tab.key === 'receivable'
          ? `<td>${escapeHtml(r.defaultHandler)}</td>`
            + `<td style="text-align:right">${escapeHtml(formatAmount(r.receivableAmount))}</td>`
            + `<td style="text-align:right">${escapeHtml(formatAmount(r.advanceAmount))}</td>`
          : `<td style="text-align:right">${escapeHtml(formatAmount(r.payableAmount))}</td>`
            + `<td style="text-align:right">${escapeHtml(formatAmount(r.prepayAmount))}</td>`)
    return `<tr><td>${i + 1}</td>${cells}</tr>`
  }).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>${escapeHtml(tab.label)}</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>${escapeHtml(tab.label)}</h2>
    <div class="meta">
      <span>期初年度：${escapeHtml(searchForm.periodYear ?? '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>
    </body></html>`
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
}

function handleError(error: Error) {
  console.error('[财务期初] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══════════════════════════════════════ 初始化 ═══════════════════════════════════════

onMounted(async () => {
  await nextTick()
  // 先取「当前会计年」（弹窗默认年度依赖它；置灰判定也依赖它），失败不阻断列表加载
  await loadAccountingYear()
  window.addEventListener('keydown', handleF8Key)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
/* 查询区横向自适应网格（禁止纵向单列） */
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
/* 会计期间联动提示条（当前会计年 + 回退/关账提示） */
.period-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.period-current { font-size: 12px; color: #595959; white-space: nowrap; }
.period-alert { flex: 1; min-width: 260px; padding: 2px 8px; }
.period-alert :deep(.ant-alert-message) { font-size: 12px; margin: 0; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.tb-alert { margin-top: 12px; }

/* 橙色新增按钮（设置模块统一） */
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
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
