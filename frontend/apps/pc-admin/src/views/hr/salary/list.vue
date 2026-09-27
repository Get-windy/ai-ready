<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        薪资管理（人力资源 → 薪资管理 → 薪资管理）
        · 两个 Tab：① 薪资发放（系统按月生成，非人工录入单据）② 薪资结构（员工主数据，档案形态表单）
        · 无左分类树（薪资按员工/部门维度查询，不需要分类树数据源）→ show-category-panel=false
        · 每个 Tab 各一套列定义 + 各一个列配置 storage-key；页面配置 storage-key 亦随 Tab 切换
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置，与 storage-key 同值）
        · 工具栏：生成月度薪资 · 全部发放 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 行内：工资条 · 发放 · 撤销 · 删除（发放）／ 修改 · 删除（结构）
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
          <template v-if="activeTab === 'payment'">
            <a-month-picker
              v-model:value="generateMonth"
              placeholder="发放月份"
              size="small"
              style="width: 130px"
              value-format="YYYY-MM"
              allow-clear
            />
            <a-button
              v-if="isButtonEnabled('generate')"
              type="primary"
              size="small"
              class="btn-add"
              :loading="generating"
              @click="handleGenerate"
            >
              <PlusOutlined /> 生成月度薪资
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchConfirm')"
              size="small"
              :disabled="!tableData.length"
              @click="handleBatchConfirm"
            >
              全部发放
            </a-button>
          </template>
          <a-button
            v-else-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAddStructure"
          >
            <PlusOutlined /> 新增薪资结构
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
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
              v-if="activeTab === 'payment' && isButtonEnabled('printF8')"
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

        <!-- ═══ 查询区（字段显隐由页面配置控制，逐 Tab 独立） ═══ -->
        <template #search-fields>
          <!-- 薪资发放 -->
          <div
            v-if="activeTab === 'payment'"
            class="search-area"
          >
            <div class="search-row">
              <div
                v-if="isQueryVisible('paymentMonth')"
                class="search-item"
              >
                <span class="search-label">发放月份</span>
                <a-month-picker
                  v-model:value="paymentQuery.paymentMonth"
                  placeholder="选择月份"
                  size="small"
                  style="width: 130px"
                  value-format="YYYY-MM"
                  allow-clear
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">员工</span>
                <a-input
                  v-model:value="paymentQuery.keyword"
                  placeholder="工号 / 姓名 / 手机号"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('deptId')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-select
                  v-model:value="paymentQuery.deptId"
                  placeholder="全部部门"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="deptOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('paymentStatus')"
                class="search-item"
              >
                <span class="search-label">发放状态</span>
                <a-select
                  v-model:value="paymentQuery.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="paymentStatusOptions"
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

          <!-- 薪资结构 -->
          <div
            v-else
            class="search-area"
          >
            <div class="search-row">
              <div
                v-if="isQueryVisible('employeeId')"
                class="search-item"
              >
                <span class="search-label">员工</span>
                <a-select
                  v-model:value="structureQuery.employeeId"
                  placeholder="全部员工"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="employeeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('structureStatus')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="structureQuery.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="structureStatusOptions"
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

        <!-- ═══ 数据表（两个 Tab 共用实例，列定义 / storage-key 随 Tab 切换） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
            >
              <!-- 金额列：统一 2 位小数（BigDecimal 序列化可能丢尾零或成字符串） -->
              <template #moneyCell="{ record, column }">
                <span
                  v-if="!record.__ghost"
                  :class="{ 'money-strong': column.key === 'actualAmount' }"
                >{{ formatMoney(record[column.key]) }}</span>
              </template>

              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDate(record[column.key]) }}</span>
              </template>

              <template #dateTimeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDateTime(record[column.key]) }}</span>
              </template>

              <!-- 发放状态：三态映射（0 待发放 / 1 已发放 / 2 已撤销） -->
              <template #paymentStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="paymentStatusColor(record.status)"
                >
                  {{ paymentStatusText(record.status) }}
                </a-tag>
              </template>

              <template #structureStatusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="structureStatusColor(record.status)"
                >
                  {{ structureStatusText(record.status) }}
                </a-tag>
              </template>

              <template #paymentActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleOpenPayslip(record)"
                  >
                    工资条
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleConfirm(record)"
                  >
                    发放
                  </a-button>
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    @click="handleRevoke(record)"
                  >
                    撤销
                  </a-button>
                  <a-button
                    v-if="record.status !== 1"
                    type="link"
                    size="small"
                    danger
                    @click="handleDeletePayment(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>

              <template #structureActionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEditStructure(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteStructure(record)"
                  >
                    删除
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

      <!-- ═══ 薪资生成结果（必须回显 message / 新增 / 跳过 / 无结构 / 失败） ═══ -->
      <a-modal
        v-model:open="generateResultOpen"
        title="薪资生成结果"
        :width="520"
        :footer="null"
      >
        <a-alert
          :message="generateResult.message || '生成完成'"
          :type="generateResult.generated > 0 ? 'success' : 'warning'"
          show-icon
        />
        <div class="gen-stats">
          <div class="gen-stat">
            <span class="gen-stat__value">{{ generateResult.generated }}</span>
            <span class="gen-stat__label">新增</span>
          </div>
          <div class="gen-stat">
            <span class="gen-stat__value">{{ generateResult.skipped }}</span>
            <span class="gen-stat__label">已存在跳过</span>
          </div>
          <div class="gen-stat">
            <span class="gen-stat__value">{{ generateResult.noStructure }}</span>
            <span class="gen-stat__label">无薪资结构</span>
          </div>
          <div class="gen-stat">
            <span class="gen-stat__value gen-stat__value--danger">{{ generateResult.failed }}</span>
            <span class="gen-stat__label">失败</span>
          </div>
        </div>
        <p class="gen-tip">
          发放月份：{{ generateResult.month || '-' }}
        </p>
      </a-modal>

      <!-- ═══ 工资条（payment + 计算依据 structure） ═══ -->
      <a-modal
        v-model:open="payslipOpen"
        :title="payslipTitle"
        :width="760"
        :footer="null"
      >
        <a-spin :spinning="payslipLoading">
          <a-descriptions
            title="员工与期间"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="员工工号">
              {{ payslip.payment?.employeeNo || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="员工姓名">
              {{ payslip.payment?.employeeName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="部门">
              {{ payslip.payment?.deptName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="发放月份">
              {{ payslip.payment?.paymentMonth || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="发放状态">
              <a-tag :color="paymentStatusColor(payslip.payment?.status)">
                {{ paymentStatusText(payslip.payment?.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="发放日期">
              {{ formatDate(payslip.payment?.paymentDate) }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            class="payslip-block"
            title="应发构成"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="基本工资">
              {{ formatMoney(payslip.payment?.baseAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="绩效工资">
              {{ formatMoney(payslip.payment?.performanceAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="津贴补贴">
              {{ formatMoney(payslip.payment?.allowanceAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="加班工资">
              {{ formatMoney(payslip.payment?.overtimeAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="应发合计">
              {{ formatMoney(payslip.payment?.grossAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="备注">
              {{ payslip.payment?.remark || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            class="payslip-block"
            title="扣除与实发"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="社保扣款">
              {{ formatMoney(payslip.payment?.socialDeduct) }}
            </a-descriptions-item>
            <a-descriptions-item label="公积金扣款">
              {{ formatMoney(payslip.payment?.fundDeduct) }}
            </a-descriptions-item>
            <a-descriptions-item label="个税扣款">
              {{ formatMoney(payslip.payment?.taxDeduct) }}
            </a-descriptions-item>
            <a-descriptions-item label="缺勤扣款">
              {{ formatMoney(payslip.payment?.deductAmount) }}
            </a-descriptions-item>
            <a-descriptions-item label="实发金额">
              <span class="money-strong">{{ formatMoney(payslip.payment?.actualAmount) }}</span>
            </a-descriptions-item>
            <a-descriptions-item label="计薪工时 / 缺勤天数">
              {{ payslip.payment?.workHours ?? '-' }} / {{ payslip.payment?.absentDays ?? '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-descriptions
            v-if="payslip.structure"
            class="payslip-block"
            title="当期薪资结构（计算依据）"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="基本工资">
              {{ formatMoney(payslip.structure.baseSalary) }}
            </a-descriptions-item>
            <a-descriptions-item label="绩效基数">
              {{ formatMoney(payslip.structure.performanceSalary) }}
            </a-descriptions-item>
            <a-descriptions-item label="岗位津贴">
              {{ formatMoney(payslip.structure.positionAllowance) }}
            </a-descriptions-item>
            <a-descriptions-item label="交通补贴">
              {{ formatMoney(payslip.structure.transportAllowance) }}
            </a-descriptions-item>
            <a-descriptions-item label="餐饮补贴">
              {{ formatMoney(payslip.structure.mealAllowance) }}
            </a-descriptions-item>
            <a-descriptions-item label="住房补贴">
              {{ formatMoney(payslip.structure.housingAllowance) }}
            </a-descriptions-item>
            <a-descriptions-item label="其他补贴">
              {{ formatMoney(payslip.structure.otherAllowance) }}
            </a-descriptions-item>
            <a-descriptions-item label="社保基数">
              {{ formatMoney(payslip.structure.socialBase) }}
            </a-descriptions-item>
            <a-descriptions-item label="公积金基数">
              {{ formatMoney(payslip.structure.fundBase) }}
            </a-descriptions-item>
            <a-descriptions-item label="生效日期">
              {{ formatDate(payslip.structure.effectiveDate) }}
            </a-descriptions-item>
          </a-descriptions>
          <a-empty
            v-else
            description="该员工当期无生效的薪资结构"
          />
        </a-spin>
      </a-modal>

      <!-- ═══ 薪资结构表单（主数据：分区卡片 + 两列栅格 + 行内校验） ═══ -->
      <a-modal
        v-model:open="structureOpen"
        :title="structureForm.id ? '修改薪资结构' : '新增薪资结构'"
        :width="780"
        :mask-closable="false"
        :confirm-loading="structureSaving"
        @ok="handleSaveStructure"
      >
        <a-form
          ref="structureFormRef"
          :model="structureForm"
          :rules="structureRules"
          :label-col="{ span: 8 }"
          :wrapper-col="{ span: 15 }"
          size="small"
        >
          <FormSection
            title="基本信息"
            tip="员工必填；修改时不可更换员工（同一员工仅保留一份生效结构）"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="员工"
                  name="employeeId"
                >
                  <a-select
                    v-model:value="structureForm.employeeId"
                    placeholder="请选择员工"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :disabled="!!structureForm.id"
                    :options="employeeOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="生效日期">
                  <a-date-picker
                    v-model:value="structureForm.effectiveDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                    placeholder="留空则为当天"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection
            title="薪资构成"
            :tip="structureTotalTip"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="基本工资"
                  name="baseSalary"
                >
                  <a-input-number
                    v-model:value="structureForm.baseSalary"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="绩效工资基数">
                  <a-input-number
                    v-model:value="structureForm.performanceSalary"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="岗位津贴">
                  <a-input-number
                    v-model:value="structureForm.positionAllowance"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="交通补贴">
                  <a-input-number
                    v-model:value="structureForm.transportAllowance"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="餐饮补贴">
                  <a-input-number
                    v-model:value="structureForm.mealAllowance"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="住房补贴">
                  <a-input-number
                    v-model:value="structureForm.housingAllowance"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="其他补贴">
                  <a-input-number
                    v-model:value="structureForm.otherAllowance"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection
            title="社保与公积金"
            tip="仅登记个人缴纳部分对应的缴费基数（比例由后端规则决定）"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="社保基数">
                  <a-input-number
                    v-model:value="structureForm.socialBase"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="公积金基数">
                  <a-input-number
                    v-model:value="structureForm.fundBase"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="备注">
            <a-row>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="structureForm.remark"
                    :rows="2"
                    :maxlength="500"
                    placeholder="调薪原因 / 约定说明"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮，storage-key 随 Tab 切换） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="activeDefaultQueryFields"
        :default-function-buttons-config="activeDefaultFunctionButtons"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="hr-salary-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 薪资管理（人力资源 → 薪资管理 → 薪资管理）
 */
// 缺口：社保 / 公积金比例与个税参数设置 `salary/setting`（GET/PUT）—— 后端无端点，本页不实现
// 缺口：工资批次（Payroll Run）`salary/payroll-run`（草稿 → 已核算 → 已发放 → 已关闭）—— 后端无端点，本页不实现
// 缺口：薪资期间控制 `salary/period-control`（月结锁定 / 释放 / 更正）—— 后端无端点，本页不实现
// 缺口：员工自助 `salary/payment/my`（依赖 sys_user ↔ hr_employee 映射）—— 后端无端点，本页不实现
// 缺口：重算 / 追溯 `salary/payment/recalculate`（改考核或考勤后重算当月）—— 后端无端点，本页不实现
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
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import { departmentApi } from '@/api/department'
import {
  hrEmployeeApi,
  hrSalaryApi,
  SALARY_PAYMENT_STATUS_MAP,
  type HrSalaryPayment,
  type HrSalaryStructure,
} from '@/api/hr'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'HrSalaryList' })

type TabKey = 'payment' | 'structure'

const TABS = [
  { key: 'payment', label: '薪资发放' },
  { key: 'structure', label: '薪资结构' },
]

// ═══ 状态 ═══
const activeTab = ref<TabKey>('payment')
const loading = ref(false)
const exporting = ref(false)
const generating = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 查询条件（逐 Tab 各一套） ═══
const paymentQuery = reactive({
  paymentMonth: undefined as string | undefined,
  keyword: '',
  deptId: undefined as number | string | undefined,
  status: undefined as number | undefined,
})

const structureQuery = reactive({
  employeeId: undefined as number | string | undefined,
  status: undefined as number | undefined,
})

const paymentStatusOptions = Object.entries(SALARY_PAYMENT_STATUS_MAP).map(([k, v]) => ({ label: v.text, value: Number(k) }))
const structureStatusOptions = [
  { label: '生效', value: 1 },
  { label: '失效', value: 0 },
]

// ═══ 列定义：薪资发放（每个 Tab 一套，storage-key 各自独立） ═══
const paymentColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'paymentActionCell', width: 200, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 130 },
  { key: 'employeeName', title: '员工姓名', type: 'input', width: 100 },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'paymentMonth', title: '发放月份', type: 'input', width: 100 },
  { key: 'baseAmount', title: '基本工资', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'performanceAmount', title: '绩效工资', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'allowanceAmount', title: '津贴补贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'overtimeAmount', title: '加班工资', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'grossAmount', title: '应发合计', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'socialDeduct', title: '社保扣款', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'fundDeduct', title: '公积金扣款', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'taxDeduct', title: '个税扣款', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'deductAmount', title: '缺勤扣款', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'actualAmount', title: '实发金额', type: 'slot', slotName: 'moneyCell', width: 120, align: 'right' },
  { key: 'status', title: '发放状态', type: 'slot', slotName: 'paymentStatusCell', width: 100 },
  { key: 'paymentDate', title: '发放日期', type: 'slot', slotName: 'dateCell', width: 120 },
  // ── 默认隐藏 ──
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'dateTimeCell', width: 150, defaultHidden: true },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'dateTimeCell', width: 150, defaultHidden: true },
]

// ═══ 列定义：薪资结构 ═══
const structureColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'structureActionCell', width: 120, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 130 },
  { key: 'employeeName', title: '员工姓名', type: 'input', width: 100 },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'baseSalary', title: '基本工资', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'performanceSalary', title: '绩效工资基数', type: 'slot', slotName: 'moneyCell', width: 130, align: 'right' },
  { key: 'positionAllowance', title: '岗位津贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'transportAllowance', title: '交通补贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'mealAllowance', title: '餐饮补贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'housingAllowance', title: '住房补贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'otherAllowance', title: '其他补贴', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'socialBase', title: '社保基数', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
  { key: 'fundBase', title: '公积金基数', type: 'slot', slotName: 'moneyCell', width: 120, align: 'right' },
  { key: 'effectiveDate', title: '生效日期', type: 'slot', slotName: 'dateCell', width: 120 },
  { key: 'expiryDate', title: '失效日期', type: 'slot', slotName: 'dateCell', width: 120 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'structureStatusCell', width: 90 },
  // ── 默认隐藏 ──
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

const activeColumns = computed<DetailColumnConfig[]>(() =>
  activeTab.value === 'payment' ? paymentColumns : structureColumns
)

/**
 * 列配置 storage-key 与 global-config-key 同值（同一份配置同时作为个人 key 与全局 key）；
 * 与 PageConfigPanel 的 storage-key 必须不同值（一个管列、一个管查询/按钮）。
 */
const tableStorageKey = computed(() => `hr-salary-${activeTab.value}-table-columns`)
const pageConfigStorageKey = computed(() => `hr-salary-page-config-${activeTab.value}`)

// ═══ 展示辅助 ═══
function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '') return '-'
  const n = Number(val)
  return Number.isFinite(n) ? n.toFixed(2) : '-'
}

function formatDate(val: any): string {
  return val ? String(val).slice(0, 10) : '-'
}

function formatDateTime(val: any): string {
  return val ? String(val).replace('T', ' ').slice(0, 16) : '-'
}

function paymentStatusText(status: any): string {
  return SALARY_PAYMENT_STATUS_MAP[status]?.text || '-'
}

function paymentStatusColor(status: any): string {
  return SALARY_PAYMENT_STATUS_MAP[status]?.color || 'default'
}

/** 薪资结构状态（api/hr 未提供该映射，页面内本地维护：0 失效 / 1 生效） */
const SALARY_STRUCTURE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '失效', color: 'default' },
  1: { text: '生效', color: 'success' },
}

function structureStatusText(status: any): string {
  return SALARY_STRUCTURE_STATUS_MAP[status]?.text || '-'
}

function structureStatusColor(status: any): string {
  return SALARY_STRUCTURE_STATUS_MAP[status]?.color || 'default'
}

// ═══ 下拉数据源 ═══
const employeeOptions = ref<{ label: string; value: number | string }[]>([])
const deptOptions = ref<{ label: string; value: number | string }[]>([])

async function loadEmployees() {
  try {
    const res: any = await hrEmployeeApi.page({ pageNum: 1, pageSize: 200 })
    const list: any[] = res?.records || []
    employeeOptions.value = list.map((e: any) => ({
      label: e.employeeNo ? `[${e.employeeNo}] ${e.employeeName}` : e.employeeName,
      value: e.id,
    }))
  } catch (error) {
    console.warn('[薪资管理] 员工下拉加载失败', error)
  }
}

async function loadDepts() {
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    const flat: { label: string; value: number | string }[] = []
    const walk = (nodes: any[], prefix: string) => {
      for (const n of nodes || []) {
        const label = prefix ? `${prefix} / ${n.departmentName}` : n.departmentName
        flat.push({ label, value: n.id })
        if (n.children?.length) walk(n.children, label)
      }
    }
    walk(list, '')
    deptOptions.value = flat
  } catch (error) {
    console.warn('[薪资管理] 部门下拉加载失败', error)
  }
}

// ═══ 数据加载 ═══
function buildPaymentParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (paymentQuery.paymentMonth) params.paymentMonth = paymentQuery.paymentMonth
  if (paymentQuery.keyword) params.keyword = paymentQuery.keyword.trim()
  if (paymentQuery.deptId !== undefined && paymentQuery.deptId !== null) params.deptId = paymentQuery.deptId
  if (paymentQuery.status !== undefined && paymentQuery.status !== null) params.status = paymentQuery.status
  return params
}

function buildStructureParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (structureQuery.employeeId !== undefined && structureQuery.employeeId !== null) params.employeeId = structureQuery.employeeId
  if (structureQuery.status !== undefined && structureQuery.status !== null) params.status = structureQuery.status
  return params
}

async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'payment') {
      const res: any = await hrSalaryApi.pagePayments(buildPaymentParams())
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else {
      const res: any = await hrSalaryApi.pageStructures(buildStructureParams())
      tableData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[薪资管理] 加载列表失败', error)
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
  if (activeTab.value === 'payment') {
    paymentQuery.paymentMonth = undefined
    paymentQuery.keyword = ''
    paymentQuery.deptId = undefined
    paymentQuery.status = undefined
  } else {
    structureQuery.employeeId = undefined
    structureQuery.status = undefined
  }
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 切 Tab：重载该 Tab 的页面配置（查询/按钮）与数据；列配置由 BillDetailTable 按 storage-key 自动重载 */
function handleTabChange(key: string) {
  if (key !== 'payment' && key !== 'structure') return
  if (key === activeTab.value) return
  activeTab.value = key
  pagination.current = 1
  loadPageConfig()
  fetchList()
}

// ═══ 生成月度薪资 ═══
const generateMonth = ref<string | undefined>(dayjs().format('YYYY-MM'))
const generateResultOpen = ref(false)
const generateResult = reactive({
  month: '',
  message: '',
  generated: 0,
  skipped: 0,
  noStructure: 0,
  failed: 0,
})

function handleGenerate() {
  const month = generateMonth.value
  if (!month) {
    message.warning('请先选择发放月份')
    return
  }
  Modal.confirm({
    title: '生成月度薪资',
    content: `确定生成 ${month} 的薪资数据吗？同一员工同月已有记录将跳过，未配置薪资结构的员工将计入「无薪资结构」。`,
    okText: '生成',
    onOk: async () => {
      generating.value = true
      try {
        const res: any = await hrSalaryApi.generateMonthlyPayment(month)
        generateResult.month = res?.paymentMonth || month
        generateResult.message = res?.message || '生成完成'
        generateResult.generated = Number(res?.generated) || 0
        generateResult.skipped = Number(res?.skipped) || 0
        generateResult.noStructure = Number(res?.noStructure) || 0
        generateResult.failed = Number(res?.failed) || 0
        generateResultOpen.value = true
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '生成失败')
      } finally {
        generating.value = false
      }
    },
  })
}

// ═══ 发放 / 撤销 / 删除（薪资发放） ═══
function handleConfirm(record: HrSalaryPayment) {
  Modal.confirm({
    title: '确认发放',
    content: `确定发放「${record.employeeName || record.employeeNo || ''}」${record.paymentMonth} 的薪资吗？发放日期将写为今天。`,
    onOk: async () => {
      try {
        await hrSalaryApi.confirmPayment(record.id)
        message.success('发放成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '发放失败')
      }
    },
  })
}

/**
 * 全部发放：BillDetailTable 的勾选状态不便取用，故按「当前页全部待发放记录」批量确认，
 * 避免基于行号持有勾选导致的漂移问题（详见记忆《列表勾选按 rowIndex 持有的漂移陷阱》）。
 */
function handleBatchConfirm() {
  const pending = tableData.value.filter(r => !r.__ghost && r.status === 0)
  if (!pending.length) {
    message.warning('当前页没有待发放的薪资记录')
    return
  }
  Modal.confirm({
    title: '全部发放',
    content: `确定将当前页 ${pending.length} 条待发放记录批量置为「已发放」吗？发放日期将写为今天。`,
    okText: '全部发放',
    onOk: async () => {
      try {
        await hrSalaryApi.batchConfirm(pending.map(r => r.id))
        message.success(`已发放 ${pending.length} 条`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量发放失败')
      }
    },
  })
}

function handleRevoke(record: HrSalaryPayment) {
  Modal.confirm({
    title: '撤销发放',
    content: `确定撤销「${record.employeeName || record.employeeNo || ''}」${record.paymentMonth} 的发放吗？撤销后状态回到「待发放」。`,
    okType: 'danger',
    onOk: async () => {
      try {
        await hrSalaryApi.revokePayment(record.id)
        message.success('已撤销')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '撤销失败')
      }
    },
  })
}

function handleDeletePayment(record: HrSalaryPayment) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除「${record.employeeName || record.employeeNo || ''}」${record.paymentMonth} 的薪资记录吗？已发放的记录需先撤销才能删除。`,
    okType: 'danger',
    onOk: async () => {
      try {
        await hrSalaryApi.removePayment(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 工资条 ═══
const payslipOpen = ref(false)
const payslipLoading = ref(false)
const payslip = ref<{ payment?: HrSalaryPayment; structure?: HrSalaryStructure }>({})

const payslipTitle = computed(() => {
  const p = payslip.value.payment
  return `工资条 - ${p?.employeeName || ''} ${p?.paymentMonth || ''}`
})

async function handleOpenPayslip(record: any) {
  payslip.value = { payment: record }
  payslipOpen.value = true
  payslipLoading.value = true
  try {
    const res: any = await hrSalaryApi.getPayment(record.id)
    payslip.value = { payment: res?.payment || record, structure: res?.structure || undefined }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载工资条失败')
  } finally {
    payslipLoading.value = false
  }
}

// ═══ 薪资结构表单 ═══
const structureOpen = ref(false)
const structureSaving = ref(false)
const structureFormRef = ref()

const emptyStructureForm = () => ({
  id: null as number | string | null,
  employeeId: undefined as number | string | undefined,
  baseSalary: undefined as number | undefined,
  performanceSalary: undefined as number | undefined,
  positionAllowance: undefined as number | undefined,
  transportAllowance: undefined as number | undefined,
  mealAllowance: undefined as number | undefined,
  housingAllowance: undefined as number | undefined,
  otherAllowance: undefined as number | undefined,
  socialBase: undefined as number | undefined,
  fundBase: undefined as number | undefined,
  effectiveDate: undefined as string | undefined,
  remark: '',
})

const structureForm = reactive(emptyStructureForm())

const structureRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  baseSalary: [{ required: true, message: '请输入基本工资', trigger: 'blur' }],
}

/** 薪资合计 = 基本工资 + 绩效基数 + 5 类津贴（与月度生成的应发构成口径一致，不含加班） */
const structureTotal = computed(() => {
  const n = (v: any) => (v === undefined || v === null || v === '' ? 0 : Number(v) || 0)
  return n(structureForm.baseSalary)
    + n(structureForm.performanceSalary)
    + n(structureForm.positionAllowance)
    + n(structureForm.transportAllowance)
    + n(structureForm.mealAllowance)
    + n(structureForm.housingAllowance)
    + n(structureForm.otherAllowance)
})

const structureTotalTip = computed(() => `薪资合计 ${formatMoney(structureTotal.value)}（基本工资 + 绩效基数 + 5 类津贴）`)

function handleAddStructure() {
  Object.assign(structureForm, emptyStructureForm())
  structureOpen.value = true
}

function handleEditStructure(record: HrSalaryStructure) {
  Object.assign(structureForm, emptyStructureForm())
  structureForm.id = record.id
  structureForm.employeeId = record.employeeId
  structureForm.baseSalary = record.baseSalary
  structureForm.performanceSalary = record.performanceSalary
  structureForm.positionAllowance = record.positionAllowance
  structureForm.transportAllowance = record.transportAllowance
  structureForm.mealAllowance = record.mealAllowance
  structureForm.housingAllowance = record.housingAllowance
  structureForm.otherAllowance = record.otherAllowance
  structureForm.socialBase = record.socialBase
  structureForm.fundBase = record.fundBase
  structureForm.effectiveDate = record.effectiveDate || undefined
  structureForm.remark = record.remark || ''
  structureOpen.value = true
}

async function handleSaveStructure() {
  try {
    await structureFormRef.value?.validate()
  } catch {
    return
  }
  const payload: Partial<HrSalaryStructure> = {
    baseSalary: structureForm.baseSalary ?? 0,
    performanceSalary: structureForm.performanceSalary ?? 0,
    positionAllowance: structureForm.positionAllowance ?? 0,
    transportAllowance: structureForm.transportAllowance ?? 0,
    mealAllowance: structureForm.mealAllowance ?? 0,
    housingAllowance: structureForm.housingAllowance ?? 0,
    otherAllowance: structureForm.otherAllowance ?? 0,
    socialBase: structureForm.socialBase ?? 0,
    fundBase: structureForm.fundBase ?? 0,
    effectiveDate: structureForm.effectiveDate || undefined,
    remark: structureForm.remark || undefined,
  }
  structureSaving.value = true
  try {
    if (structureForm.id) {
      // 修改：后端以员工为维度维护唯一生效结构，employeeId 不入参（后端会置空）
      await hrSalaryApi.updateStructure(structureForm.id, payload)
      message.success('修改成功')
    } else {
      await hrSalaryApi.createStructure({ ...payload, employeeId: structureForm.employeeId as any })
      message.success('新增成功')
    }
    structureOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    structureSaving.value = false
  }
}

function handleDeleteStructure(record: HrSalaryStructure) {
  Modal.confirm({
    title: '确认删除',
    content: `确定删除「${record.employeeName || record.employeeNo || ''}」的薪资结构吗？删除后该员工月度薪资将无法生成。`,
    okType: 'danger',
    onOk: async () => {
      try {
        await hrSalaryApi.removeStructure(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 打印(F8) / 导出：共用「所见即所打」列口径 ═══
const MONEY_KEYS = new Set([
  'baseAmount', 'performanceAmount', 'allowanceAmount', 'overtimeAmount', 'grossAmount',
  'socialDeduct', 'fundDeduct', 'taxDeduct', 'deductAmount', 'actualAmount',
  'baseSalary', 'performanceSalary', 'positionAllowance', 'transportAllowance',
  'mealAllowance', 'housingAllowance', 'otherAllowance', 'socialBase', 'fundBase',
])
const DATE_KEYS = new Set(['paymentDate', 'effectiveDate', 'expiryDate'])
const DATETIME_KEYS = new Set(['createTime', 'updateTime'])

/** 打印/导出列：剔除系统列，仅取默认可见列（即「所见即所打」） */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.filter(c =>
    c.type !== 'rowNo' && c.type !== 'action' && c.key !== '__filler__' && !c.defaultHidden
  )
)

function cellText(col: DetailColumnConfig, record: any): string {
  const value = record[col.key]
  if (col.key === 'status') {
    return activeTab.value === 'payment' ? paymentStatusText(value) : structureStatusText(value)
  }
  if (MONEY_KEYS.has(col.key)) return formatMoney(value)
  if (DATETIME_KEYS.has(col.key)) return formatDateTime(value)
  if (DATE_KEYS.has(col.key)) return formatDate(value)
  return value === null || value === undefined || value === '' ? '' : String(value)
}

function tabLabel(): string {
  return TABS.find(t => t.key === activeTab.value)?.label || '薪资管理'
}

function buildFilterSummary(): string {
  if (activeTab.value === 'payment') {
    const parts: string[] = []
    if (paymentQuery.paymentMonth) parts.push(`发放月份：${paymentQuery.paymentMonth}`)
    if (paymentQuery.keyword) parts.push(`员工：${paymentQuery.keyword}`)
    if (paymentQuery.status !== undefined && paymentQuery.status !== null) parts.push(`发放状态：${paymentStatusText(paymentQuery.status)}`)
    return parts.join('　')
  }
  const parts: string[] = []
  const emp = employeeOptions.value.find(e => e.value === structureQuery.employeeId)
  if (emp) parts.push(`员工：${emp.label}`)
  if (structureQuery.status !== undefined && structureQuery.status !== null) parts.push(`状态：${structureStatusText(structureQuery.status)}`)
  return parts.join('　')
}

/** 可打印行（去掉树形占位行）——标题里的记录数与表格行同源 */
function printableRows(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost)
}

// ═══ 打印（结果集打印）：工资表 / 薪资结构 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列是 computed（随 Tab 与列配置变），静态生成器写不进模板 → 明确按数据列打；
// 单元格文本仍走 cellText（与导出同一口径），逐列还原原来的中文/日期/金额格式。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'hr-salary-list',
  useDataColumns: true,
  // 原打印抬头的筛选摘要/记录数元信息行并入标题；打印时间由引擎按本次打印时间给
  title: () => `${tabLabel()}${activeTab.value === 'payment' ? '（工资表）' : ''}（${buildFilterSummary()}，记录数：${printableRows().length}）`,
  columns: () => [
    { key: '__seq', title: '#', align: 'center' },
    ...printableColumns.value.map(c => ({ key: c.key, title: c.title, align: c.align })),
  ],
  rows: () => printableRows().map((r: any, i: number) => {
    const out: Record<string, any> = { __seq: i + 1 }
    printableColumns.value.forEach(c => { out[c.key] = cellText(c, r) })
    return out
  }),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if (activeTab.value !== 'payment') return
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

/** 导出（CSV，按当前筛选全量） */
async function handleExport() {
  exporting.value = true
  try {
    const cols = printableColumns.value
    let rows: any[] = []
    if (activeTab.value === 'payment') {
      const res: any = await hrSalaryApi.pagePayments({ ...buildPaymentParams(), pageNum: 1, pageSize: 10000 })
      rows = res?.records || []
    } else {
      const res: any = await hrSalaryApi.pageStructures({ ...buildStructureParams(), pageNum: 1, pageSize: 10000 })
      rows = res?.records || []
    }
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['序号', ...cols.map(c => c.title)]
    const lines = rows.map((r, i) => [i + 1, ...cols.map(c => cellText(c, r))]
      .map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${tabLabel()}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置（逐 Tab 独立 storage-key 与默认项） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS_PAYMENT: QueryFieldSetting[] = [
  { key: 'paymentMonth', label: '发放月份', visible: true },
  { key: 'keyword', label: '员工（工号/姓名/手机号）', visible: true },
  { key: 'deptId', label: '部门', visible: true },
  { key: 'paymentStatus', label: '发放状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS_PAYMENT: FunctionButtonSetting[] = [
  { key: 'generate', label: '生成月度薪资', enabled: true },
  { key: 'batchConfirm', label: '全部发放', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const DEFAULT_QUERY_FIELDS_STRUCTURE: QueryFieldSetting[] = [
  { key: 'employeeId', label: '员工', visible: true },
  { key: 'structureStatus', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS_STRUCTURE: FunctionButtonSetting[] = [
  { key: 'add', label: '新增薪资结构', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>([])
const functionButtons = ref<FunctionButtonSetting[]>([])
const showPageConfig = ref(false)

const activeDefaultQueryFields = computed(() =>
  activeTab.value === 'payment' ? DEFAULT_QUERY_FIELDS_PAYMENT : DEFAULT_QUERY_FIELDS_STRUCTURE
)
const activeDefaultFunctionButtons = computed(() =>
  activeTab.value === 'payment' ? DEFAULT_FUNCTION_BUTTONS_PAYMENT : DEFAULT_FUNCTION_BUTTONS_STRUCTURE
)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

/** 按已保存配置的顺序还原，未记录的项追加到默认顺序末尾（保证拖拽排序可持久） */
function mergeSavedOrder<T extends { key: string }>(
  defaults: T[],
  saved: T[] | undefined,
  merge: (def: T, item: T) => T
): T[] {
  if (!Array.isArray(saved) || saved.length === 0) return defaults.map(d => ({ ...d }))
  const result: T[] = []
  saved.forEach((s) => {
    const def = defaults.find(d => d.key === s.key)
    if (def) result.push(merge(def, s))
  })
  defaults.forEach((d) => {
    if (!saved.some(s => s.key === d.key)) result.push({ ...d })
  })
  return result
}

/** 载入当前 Tab 的页面配置（localStorage 优先，无则用默认）；切 Tab 必须重载 */
function loadPageConfig() {
  const qDefaults = activeDefaultQueryFields.value
  const bDefaults = activeDefaultFunctionButtons.value
  try {
    const raw = localStorage.getItem(pageConfigStorageKey.value)
    if (raw) {
      const parsed = JSON.parse(raw)
      queryFields.value = mergeSavedOrder(qDefaults, parsed?.queryFields, (df, saved) => ({ ...df, ...saved }))
      functionButtons.value = mergeSavedOrder(bDefaults, parsed?.functionButtons, (bf, saved) => ({ ...bf, ...saved }))
      return
    }
  } catch (error) {
    console.warn('[薪资管理] 页面配置读取失败', error)
  }
  queryFields.value = qDefaults.map(f => ({ ...f }))
  functionButtons.value = bDefaults.map(b => ({ ...b }))
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[薪资管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  loadEmployees()
  loadDepts()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.money-strong { color: #52c41a; font-weight: 600; }

.gen-stats { display: flex; gap: 12px; margin-top: 16px; }
.gen-stat { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 10px 4px; background: #fafafa; border: 1px solid #f0f0f0; border-radius: 4px; }
.gen-stat__value { font-size: 20px; font-weight: 600; color: #1890ff; }
.gen-stat__value--danger { color: #ff4d4f; }
.gen-stat__label { font-size: 12px; color: #888; margin-top: 2px; }
.gen-tip { margin: 12px 0 0; font-size: 12px; color: #888; }

.payslip-block { margin-top: 16px; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
