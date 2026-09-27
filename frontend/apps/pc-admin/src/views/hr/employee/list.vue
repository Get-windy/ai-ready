<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        员工列表（人力资源 → 员工管理 → 员工列表）
        · 左：部门分类树（数据源 sys_department，HR 只读引用，不在此维护部门主数据）
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增员工 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：关键字 · 状态 · 员工类型 · 岗位 · 学历 · 性别 · 入职日期区间
        · 行内：修改 / 合同 / 更多（转正 · 离职 · 异动记录 · 删除）
        · 表单：抽屉 + 分区卡片 + 两列栅格（员工档案是主数据，非单据，不用 BillFormPage）
      -->
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
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增员工
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

        <!-- ═══ 查询区（字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="工号 / 姓名 / 手机号"
                  size="small"
                  style="width: 200px"
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
              <div
                v-if="isQueryVisible('employeeType')"
                class="search-item"
              >
                <span class="search-label">员工类型</span>
                <a-select
                  v-model:value="searchForm.employeeType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="employeeTypeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('positionId')"
                class="search-item"
              >
                <span class="search-label">岗位</span>
                <a-select
                  v-model:value="searchForm.positionId"
                  placeholder="全部岗位"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="positionOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('education')"
                class="search-item"
              >
                <span class="search-label">学历</span>
                <a-select
                  v-model:value="searchForm.education"
                  placeholder="全部学历"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="educationOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('gender')"
                class="search-item"
              >
                <span class="search-label">性别</span>
                <a-select
                  v-model:value="searchForm.gender"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="genderOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('hireDate')"
                class="search-item"
              >
                <span class="search-label">入职日期</span>
                <a-range-picker
                  v-model:value="searchForm.hireDateRange"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
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

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="hr-employee-table-columns"
              global-config-key="hr-employee-table-columns"
              @sort-change="handleSortChange"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.employeeName }}</a>
              </template>

              <template #genderCell="{ record }">
                <span v-if="!record.__ghost">{{ textOf(GENDER_MAP, record.gender) }}</span>
              </template>

              <template #educationCell="{ record }">
                <span v-if="!record.__ghost">{{ textOf(EDUCATION_MAP, record.education) }}</span>
              </template>

              <template #employeeTypeCell="{ record }">
                <span v-if="!record.__ghost">{{ textOf(EMPLOYEE_TYPE_MAP, record.employeeType) }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="employeeStatusColor(record.status)"
                >
                  {{ employeeStatusText(record.status) }}
                </a-tag>
              </template>

              <template #dateCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatDate(record[column.key]) }}</span>
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
                    @click="handleOpenContracts(record)"
                  >
                    合同
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item
                          v-if="record.status === 2"
                          @click="handleRegularize(record)"
                        >
                          转正
                        </a-menu-item>
                        <a-menu-item
                          v-if="record.status === 1 || record.status === 2"
                          @click="handleResign(record)"
                        >
                          离职
                        </a-menu-item>
                        <a-menu-item @click="handleOpenChanges(record)">
                          异动记录
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item
                          danger
                          @click="handleDelete(record)"
                        >
                          删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
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

      <!-- ═══ 员工档案抽屉（主数据：分区卡片 + 两列栅格） ═══ -->
      <a-drawer
        v-model:open="formOpen"
        :title="formState.id ? '修改员工档案' : '新增员工档案'"
        :width="880"
        :mask-closable="false"
        :destroy-on-close="true"
        placement="right"
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
            title="基本信息"
            tip="工号留空时由号段自动生成；姓名必填"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="员工工号">
                  <a-input
                    v-model:value="formState.employeeNo"
                    placeholder="留空自动生成"
                    :disabled="!!formState.id"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="员工姓名"
                  name="employeeName"
                >
                  <a-input
                    v-model:value="formState.employeeName"
                    placeholder="请输入员工姓名"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="性别">
                  <a-select
                    v-model:value="formState.gender"
                    :options="genderOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="出生日期">
                  <a-date-picker
                    v-model:value="formState.birthDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="身份证号"
                  name="idCard"
                >
                  <a-input
                    v-model:value="formState.idCard"
                    placeholder="18 位身份证号"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="学历">
                  <a-select
                    v-model:value="formState.education"
                    :options="educationOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="毕业院校">
                  <a-input
                    v-model:value="formState.school"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="专业">
                  <a-input
                    v-model:value="formState.major"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="联系方式">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="手机号"
                  name="phone"
                >
                  <a-input
                    v-model:value="formState.phone"
                    placeholder="11 位手机号"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="邮箱"
                  name="email"
                >
                  <a-input
                    v-model:value="formState.email"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="户籍地址">
                  <a-input
                    v-model:value="formState.hometownAddress"
                    :maxlength="200"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="现居住地址">
                  <a-input
                    v-model:value="formState.currentAddress"
                    :maxlength="200"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="紧急联系人">
                  <a-input
                    v-model:value="formState.emergencyContact"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="紧急联系电话">
                  <a-input
                    v-model:value="formState.emergencyPhone"
                    :maxlength="20"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="任职信息">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="部门">
                  <a-tree-select
                    v-model:value="formState.deptId"
                    :tree-data="deptTreeSelectData"
                    placeholder="请选择部门"
                    allow-clear
                    tree-default-expand-all
                    :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="岗位">
                  <a-select
                    v-model:value="formState.positionId"
                    placeholder="请选择岗位"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="positionOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="员工类型">
                  <a-select
                    v-model:value="formState.employeeType"
                    :options="employeeTypeOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="入职日期"
                  name="hireDate"
                >
                  <a-date-picker
                    v-model:value="formState.hireDate"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </a-form-item>
              </a-col>
              <a-col
                v-if="formState.id"
                :span="12"
              >
                <a-form-item label="当前状态">
                  <a-tag :color="employeeStatusColor(formState.status)">
                    {{ employeeStatusText(formState.status) }}
                  </a-tag>
                  <span
                    v-if="formState.status === 0 && formState.leaveDate"
                    class="status-hint"
                  >离职日期 {{ formatDate(formState.leaveDate) }}</span>
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="formState.remark"
                    :rows="2"
                    :maxlength="500"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>

        <template #footer>
          <a-space>
            <a-button @click="formOpen = false">
              取消
            </a-button>
            <a-button
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              保存
            </a-button>
          </a-space>
        </template>
      </a-drawer>

      <!-- ═══ 合同弹窗 ═══ -->
      <a-modal
        v-model:open="contractOpen"
        :title="`劳动合同 - ${contractOwner?.employeeName || ''}`"
        :width="900"
        :footer="null"
      >
        <a-space
          style="margin-bottom: 12px"
          :size="8"
        >
          <a-button
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAddContract"
          >
            <PlusOutlined /> 新增合同
          </a-button>
          <a-button
            size="small"
            :loading="contractLoading"
            @click="loadContracts"
          >
            <ReloadOutlined /> 刷新
          </a-button>
        </a-space>
        <a-table
          :data-source="contracts"
          :columns="contractColumns"
          :loading="contractLoading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'contractType'">
              {{ CONTRACT_TYPE_MAP[record.contractType] || '-' }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="contractStatusColor(record.status)">
                {{ CONTRACT_STATUS_MAP[record.status] || '-' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="0">
                <a-button
                  type="link"
                  size="small"
                  @click="handleEditContract(record)"
                >
                  修改
                </a-button>
                <a-button
                  v-if="record.status === 1"
                  type="link"
                  size="small"
                  danger
                  @click="handleTerminateContract(record)"
                >
                  终止
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  danger
                  @click="handleDeleteContract(record)"
                >
                  删除
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 合同表单 ═══ -->
      <a-modal
        v-model:open="contractFormOpen"
        :title="contractForm.id ? '修改合同' : '新增合同'"
        :width="640"
        :mask-closable="false"
        :confirm-loading="contractSaving"
        @ok="handleSaveContract"
      >
        <a-form
          :model="contractForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item
            label="合同编号"
            required
          >
            <a-input
              v-model:value="contractForm.contractNo"
              placeholder="留空自动生成"
              :disabled="!!contractForm.id"
            />
          </a-form-item>
          <a-form-item label="合同名称">
            <a-input
              v-model:value="contractForm.contractName"
              placeholder="如 固定期限劳动合同"
            />
          </a-form-item>
          <a-form-item
            label="合同类型"
            required
          >
            <a-select
              v-model:value="contractForm.contractType"
              :options="contractTypeOptions"
            />
          </a-form-item>
          <a-form-item label="开始日期">
            <a-date-picker
              v-model:value="contractForm.startDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="结束日期">
            <a-date-picker
              v-model:value="contractForm.endDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="试用期到期日">
            <a-date-picker
              v-model:value="contractForm.trialDateEnd"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="约定薪资">
            <a-input-number
              v-model:value="contractForm.salaryAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="签订日期">
            <a-date-picker
              v-model:value="contractForm.signDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="contractForm.remark"
              :rows="2"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 转正 ═══ -->
      <a-modal
        v-model:open="regularizeOpen"
        title="员工转正"
        :width="480"
        :confirm-loading="actionSaving"
        @ok="handleSubmitRegularize"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="员工">
            <span>{{ actionTarget?.employeeName }}（{{ actionTarget?.employeeNo }}）</span>
          </a-form-item>
          <a-form-item label="转正日期">
            <a-date-picker
              v-model:value="regularizeForm.regularDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="转正说明">
            <a-textarea
              v-model:value="regularizeForm.remark"
              :rows="2"
              placeholder="试用期评价 / 转正说明"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 离职 ═══ -->
      <a-modal
        v-model:open="resignOpen"
        title="员工离职"
        :width="480"
        :confirm-loading="actionSaving"
        @ok="handleSubmitResign"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="员工">
            <span>{{ actionTarget?.employeeName }}（{{ actionTarget?.employeeNo }}）</span>
          </a-form-item>
          <a-form-item label="最后工作日">
            <a-date-picker
              v-model:value="resignForm.lastWorkDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item label="离职类型">
            <a-select
              v-model:value="resignForm.resignType"
              :options="resignTypeOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="离职原因">
            <a-textarea
              v-model:value="resignForm.resignReason"
              :rows="2"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 异动记录 ═══ -->
      <a-drawer
        v-model:open="changesOpen"
        :title="`人事异动记录 - ${actionTarget?.employeeName || ''}`"
        :width="760"
        placement="right"
      >
        <a-table
          :data-source="changes"
          :columns="changeColumns"
          :loading="changesLoading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'changeType'">
              <a-tag>{{ CHANGE_TYPE_MAP[record.changeType] || record.changeType }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="hr-employee-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="hr-employee-list"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
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
  hrPositionApi,
  hrContractApi,
  GENDER_MAP,
  EDUCATION_MAP,
  EMPLOYEE_TYPE_MAP,
  EMPLOYEE_STATUS_MAP,
  CONTRACT_TYPE_MAP,
  CONTRACT_STATUS_MAP,
  RESIGN_TYPE_OPTIONS,
  CHANGE_TYPE_MAP,
  type HrEmployee,
  type HrContract,
  type HrEmployeeChange,
} from '@/api/hr'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'HrEmployeeList' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const actionSaving = ref(false)
const tableData = ref<HrEmployee[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })

const statusOptions = [
  { label: '在职', value: 1 },
  { label: '试用', value: 2 },
  { label: '离职', value: 0 },
]
const genderOptions = Object.entries(GENDER_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const educationOptions = Object.entries(EDUCATION_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const employeeTypeOptions = Object.entries(EMPLOYEE_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const contractTypeOptions = Object.entries(CONTRACT_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const resignTypeOptions = RESIGN_TYPE_OPTIONS

// ═══ 数据表列（默认显示 10 列 + 序号 + 操作；其余默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'employeeNo', title: '员工工号', type: 'input', width: 150, sortable: true },
  { key: 'employeeName', title: '员工姓名', type: 'slot', slotName: 'nameCell', width: 110, sortable: true },
  { key: 'deptName', title: '部门', type: 'input', width: 130 },
  { key: 'positionName', title: '岗位', type: 'input', width: 130 },
  { key: 'employeeType', title: '员工类型', type: 'slot', slotName: 'employeeTypeCell', width: 100 },
  { key: 'hireDate', title: '入职日期', type: 'slot', slotName: 'dateCell', width: 120, sortable: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, sortable: true },
  { key: 'gender', title: '性别', type: 'slot', slotName: 'genderCell', width: 70 },
  { key: 'education', title: '学历', type: 'slot', slotName: 'educationCell', width: 90 },
  { key: 'phone', title: '手机号', type: 'input', width: 130 },
  // ── 默认隐藏 ──
  { key: 'leaveDate', title: '离职日期', type: 'slot', slotName: 'dateCell', width: 120, defaultHidden: true },
  { key: 'workYears', title: '工龄(年)', type: 'input', width: 90, defaultHidden: true },
  { key: 'birthDate', title: '出生日期', type: 'slot', slotName: 'dateCell', width: 120, defaultHidden: true },
  { key: 'idCard', title: '身份证号', type: 'input', width: 190, defaultHidden: true },
  { key: 'school', title: '毕业院校', type: 'input', width: 160, defaultHidden: true },
  { key: 'major', title: '专业', type: 'input', width: 140, defaultHidden: true },
  { key: 'email', title: '邮箱', type: 'input', width: 180, defaultHidden: true },
  { key: 'hometownAddress', title: '户籍地址', type: 'input', width: 200, defaultHidden: true },
  { key: 'currentAddress', title: '现居住地址', type: 'input', width: 200, defaultHidden: true },
  { key: 'emergencyContact', title: '紧急联系人', type: 'input', width: 120, defaultHidden: true },
  { key: 'emergencyPhone', title: '紧急联系电话', type: 'input', width: 140, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  keyword: '',
  status: undefined as number | undefined,
  employeeType: undefined as number | undefined,
  positionId: undefined as number | undefined,
  education: undefined as number | undefined,
  gender: undefined as number | undefined,
  hireDateRange: undefined as [string, string] | undefined,
})

// ═══ 部门分类树（数据源 sys_department，HR 只读） ═══
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
const deptTreeSelectData = computed(() => categoryTreeData.value)

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
    const list: any[] = Array.isArray(res) ? res : res?.data || []
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
    console.warn('[员工列表] 部门树加载失败', error)
    categoryTreeData.value = [{ id: ROOT_DEPT_ID, categoryName: '全部部门' }]
  } finally {
    categoryLoading.value = false
  }
}

function handleDeptSelect(keys: any[]) {
  const key = keys?.[0]
  selectedDeptId.value = key === undefined || key === null ? ROOT_DEPT_ID : key
  handleSearch()
}

function handleDeptExpand(keys: any[]) {
  expandedKeys.value = keys || []
}

// ═══ 岗位下拉 ═══
const positionOptions = ref<{ label: string; value: number }[]>([])

async function loadPositions() {
  try {
    const res: any = await hrPositionApi.list()
    const list: any[] = Array.isArray(res) ? res : res?.records || []
    positionOptions.value = list.map((p: any) => ({
      label: p.positionCode ? `[${p.positionCode}] ${p.positionName}` : p.positionName,
      value: p.id,
    }))
  } catch (error) {
    console.warn('[员工列表] 岗位列表加载失败', error)
  }
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (String(selectedDeptId.value) !== ROOT_DEPT_ID) params.deptId = selectedDeptId.value
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (searchForm.status !== undefined) params.status = searchForm.status
  if (searchForm.employeeType !== undefined) params.employeeType = searchForm.employeeType
  if (searchForm.positionId !== undefined) params.positionId = searchForm.positionId
  if (searchForm.education !== undefined) params.education = searchForm.education
  if (searchForm.gender !== undefined) params.gender = searchForm.gender
  if (searchForm.hireDateRange?.length === 2) {
    params.hireDateStart = searchForm.hireDateRange[0]
    params.hireDateEnd = searchForm.hireDateRange[1]
  }
  if (sortState.field) {
    params.sortField = sortState.field
    params.sortOrder = sortState.order
  }
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await hrEmployeeApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[员工列表] 加载列表失败', error)
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
  searchForm.keyword = ''
  searchForm.status = undefined
  searchForm.employeeType = undefined
  searchForm.positionId = undefined
  searchForm.education = undefined
  searchForm.gender = undefined
  searchForm.hireDateRange = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 表头排序（服务端排序，后端按白名单列生效，其余回落「创建时间倒序」） */
function handleSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

// ═══ 展示辅助 ═══
function textOf(map: Record<any, string>, value: any): string {
  if (value === null || value === undefined) return '-'
  return map[value] || '-'
}

function formatDate(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

function employeeStatusText(status: number): string {
  return EMPLOYEE_STATUS_MAP[status]?.text || '-'
}

function employeeStatusColor(status: number): string {
  return EMPLOYEE_STATUS_MAP[status]?.color || 'default'
}

function contractStatusColor(status: number): string {
  return ({ 0: 'default', 1: 'success', 2: 'warning', 3: 'error' } as Record<number, string>)[status] || 'default'
}

// ═══ 员工档案表单 ═══
const formOpen = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null as number | string | null,
  employeeNo: '',
  employeeName: '',
  gender: undefined as number | undefined,
  birthDate: undefined as string | undefined,
  idCard: '',
  education: undefined as number | undefined,
  school: '',
  major: '',
  phone: '',
  email: '',
  hometownAddress: '',
  currentAddress: '',
  emergencyContact: '',
  emergencyPhone: '',
  deptId: undefined as number | undefined,
  positionId: undefined as number | undefined,
  employeeType: undefined as number | undefined,
  hireDate: undefined as string | undefined,
  leaveDate: undefined as string | undefined,
  status: 2 as number,
  remark: '',
})
const formState = reactive(emptyForm())

const rules = {
  employeeName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  hireDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  idCard: [{ pattern: /^\d{17}[\dXx]$/, message: '身份证号应为 18 位', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

function handleAdd() {
  resetForm()
  formOpen.value = true
}

async function handleEdit(record: HrEmployee) {
  resetForm()
  formOpen.value = true
  try {
    const detail: any = await hrEmployeeApi.getById(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.employeeNo = detail.employeeNo || ''
    formState.employeeName = detail.employeeName || ''
    formState.gender = detail.gender
    formState.birthDate = detail.birthDate || undefined
    formState.idCard = detail.idCard || ''
    formState.education = detail.education
    formState.school = detail.school || ''
    formState.major = detail.major || ''
    formState.phone = detail.phone || ''
    formState.email = detail.email || ''
    formState.hometownAddress = detail.hometownAddress || ''
    formState.currentAddress = detail.currentAddress || ''
    formState.emergencyContact = detail.emergencyContact || ''
    formState.emergencyPhone = detail.emergencyPhone || ''
    formState.deptId = detail.deptId
    formState.positionId = detail.positionId
    formState.employeeType = detail.employeeType
    formState.hireDate = detail.hireDate || undefined
    formState.leaveDate = detail.leaveDate || undefined
    formState.status = detail.status ?? 2
    formState.remark = detail.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载员工详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload = {
    employeeNo: formState.employeeNo || null,
    employeeName: formState.employeeName.trim(),
    gender: formState.gender ?? null,
    birthDate: formState.birthDate || null,
    idCard: formState.idCard || null,
    education: formState.education ?? null,
    school: formState.school || null,
    major: formState.major || null,
    phone: formState.phone || null,
    email: formState.email || null,
    hometownAddress: formState.hometownAddress || null,
    currentAddress: formState.currentAddress || null,
    emergencyContact: formState.emergencyContact || null,
    emergencyPhone: formState.emergencyPhone || null,
    deptId: formState.deptId ?? null,
    positionId: formState.positionId ?? null,
    employeeType: formState.employeeType ?? 1,
    hireDate: formState.hireDate || null,
    remark: formState.remark || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      await hrEmployeeApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await hrEmployeeApi.create(payload)
      message.success('新增成功')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: HrEmployee) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除员工「${record.employeeName}」吗？删除后不再出现在台账中。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrEmployeeApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 转正 / 离职 ═══
const regularizeOpen = ref(false)
const resignOpen = ref(false)
const actionTarget = ref<HrEmployee | null>(null)
const regularizeForm = reactive({ regularDate: undefined as string | undefined, remark: '' })
const resignForm = reactive({
  lastWorkDate: undefined as string | undefined,
  resignType: undefined as number | undefined,
  resignReason: '',
})

function handleRegularize(record: HrEmployee) {
  actionTarget.value = record
  regularizeForm.regularDate = dayjs().format('YYYY-MM-DD')
  regularizeForm.remark = ''
  regularizeOpen.value = true
}

async function handleSubmitRegularize() {
  if (!actionTarget.value) return
  actionSaving.value = true
  try {
    await hrEmployeeApi.regularize(actionTarget.value.id, {
      regularDate: regularizeForm.regularDate,
      remark: regularizeForm.remark || undefined,
    })
    message.success('转正成功')
    regularizeOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '转正失败')
  } finally {
    actionSaving.value = false
  }
}

function handleResign(record: HrEmployee) {
  actionTarget.value = record
  resignForm.lastWorkDate = dayjs().format('YYYY-MM-DD')
  resignForm.resignType = undefined
  resignForm.resignReason = ''
  resignOpen.value = true
}

async function handleSubmitResign() {
  if (!actionTarget.value) return
  actionSaving.value = true
  try {
    await hrEmployeeApi.resign(actionTarget.value.id, {
      lastWorkDate: resignForm.lastWorkDate,
      resignType: resignForm.resignType,
      resignReason: resignForm.resignReason || undefined,
    })
    message.success('离职办理成功')
    resignOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '离职办理失败')
  } finally {
    actionSaving.value = false
  }
}

// ═══ 合同 ═══
const contractOpen = ref(false)
const contractLoading = ref(false)
const contractSaving = ref(false)
const contractFormOpen = ref(false)
const contracts = ref<HrContract[]>([])
const contractOwner = ref<HrEmployee | null>(null)
const contractForm = reactive({
  id: null as number | string | null,
  contractNo: '',
  contractName: '',
  contractType: 1 as number,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
  trialDateEnd: undefined as string | undefined,
  salaryAmount: undefined as number | undefined,
  signDate: undefined as string | undefined,
  remark: '',
})
const contractColumns = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 160 },
  { title: '合同名称', dataIndex: 'contractName', key: 'contractName', width: 150 },
  { title: '类型', dataIndex: 'contractType', key: 'contractType', width: 100 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 170 },
]

async function handleOpenContracts(record: HrEmployee) {
  contractOwner.value = record
  contractOpen.value = true
  await loadContracts()
}

async function loadContracts() {
  if (!contractOwner.value) return
  contractLoading.value = true
  try {
    const res: any = await hrContractApi.listByEmployee(contractOwner.value.id)
    contracts.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载合同失败')
    contracts.value = []
  } finally {
    contractLoading.value = false
  }
}

function resetContractForm() {
  contractForm.id = null
  contractForm.contractNo = ''
  contractForm.contractName = ''
  contractForm.contractType = 1
  contractForm.startDate = undefined
  contractForm.endDate = undefined
  contractForm.trialDateEnd = undefined
  contractForm.salaryAmount = undefined
  contractForm.signDate = dayjs().format('YYYY-MM-DD')
  contractForm.remark = ''
}

function handleAddContract() {
  resetContractForm()
  contractFormOpen.value = true
}

function handleEditContract(record: HrContract) {
  resetContractForm()
  contractForm.id = record.id
  contractForm.contractNo = record.contractNo || ''
  contractForm.contractName = record.contractName || ''
  contractForm.contractType = record.contractType ?? 1
  contractForm.startDate = record.startDate || undefined
  contractForm.endDate = record.endDate || undefined
  contractForm.trialDateEnd = record.trialDateEnd || undefined
  contractForm.salaryAmount = record.salaryAmount
  contractForm.signDate = record.signDate || undefined
  contractForm.remark = record.remark || ''
  contractFormOpen.value = true
}

async function handleSaveContract() {
  if (!contractOwner.value) return
  if (!contractForm.contractType) {
    message.warning('请选择合同类型')
    return
  }
  const payload = {
    employeeId: contractOwner.value.id,
    contractNo: contractForm.contractNo || null,
    contractName: contractForm.contractName || null,
    contractType: contractForm.contractType,
    startDate: contractForm.startDate || null,
    endDate: contractForm.endDate || null,
    trialDateEnd: contractForm.trialDateEnd || null,
    salaryAmount: contractForm.salaryAmount ?? null,
    signDate: contractForm.signDate || null,
    remark: contractForm.remark || null,
  }
  contractSaving.value = true
  try {
    if (contractForm.id) {
      await hrContractApi.update(contractForm.id, payload)
      message.success('合同修改成功')
    } else {
      await hrContractApi.create(payload)
      message.success('合同新增成功')
    }
    contractFormOpen.value = false
    loadContracts()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存合同失败')
  } finally {
    contractSaving.value = false
  }
}

function handleTerminateContract(record: HrContract) {
  Modal.confirm({
    title: '确认终止',
    content: `确定要终止合同「${record.contractNo}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await hrContractApi.updateStatus(record.id, 3)
        message.success('合同已终止')
        loadContracts()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '终止失败')
      }
    },
  })
}

function handleDeleteContract(record: HrContract) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除合同「${record.contractNo}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await hrContractApi.remove(record.id)
        message.success('删除成功')
        loadContracts()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 异动记录 ═══
const changesOpen = ref(false)
const changesLoading = ref(false)
const changes = ref<HrEmployeeChange[]>([])
const changeColumns = [
  { title: '生效日期', dataIndex: 'effectiveDate', key: 'effectiveDate', width: 110 },
  { title: '异动类型', dataIndex: 'changeType', key: 'changeType', width: 100 },
  { title: '工号', dataIndex: 'employeeNo', key: 'employeeNo', width: 150 },
  { title: '原因', dataIndex: 'reason', key: 'reason', width: 180 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 100 },
  { title: '操作时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
]

async function handleOpenChanges(record: HrEmployee) {
  actionTarget.value = record
  changesOpen.value = true
  changesLoading.value = true
  try {
    const res: any = await hrEmployeeApi.changes({ pageNum: 1, pageSize: 200, employeeId: record.id })
    changes.value = res?.records || []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载异动记录失败')
    changes.value = []
  } finally {
    changesLoading.value = false
  }
}

// ═══ 打印（结果集打印）：员工花名册 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列与原来的表格逐列对齐（# 行号、类型/状态/性别/学历中文都在 formatter 里还原）。
const printColumns: any[] = [
  { title: '#', key: '__seq', width: 40, align: 'center' },
  { title: '工号', key: 'employeeNo' },
  { title: '姓名', key: 'employeeName' },
  { title: '部门', key: 'deptName', formatter: (v: any) => v || '' },
  { title: '岗位', key: 'positionName', formatter: (v: any) => v || '' },
  { title: '类型', key: 'employeeType', formatter: (_v: any, r: any) => EMPLOYEE_TYPE_MAP[r.employeeType] || '' },
  { title: '入职日期', key: 'hireDate', formatter: (_v: any, r: any) => formatDate(r.hireDate) },
  { title: '状态', key: 'status', formatter: (_v: any, r: any) => employeeStatusText(r.status) },
  { title: '性别', key: 'gender', formatter: (_v: any, r: any) => GENDER_MAP[r.gender] || '' },
  { title: '学历', key: 'education', formatter: (_v: any, r: any) => EDUCATION_MAP[r.education] || '' },
  { title: '手机号', key: 'phone', formatter: (v: any) => v || '' },
]

/** 可打印行（去掉树形占位行）——标题里的记录数与表格行同源 */
function printableRows(): any[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost)
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'hr-employee-list',
  // 原打印抬头的「部门/记录数」元信息行并入标题；打印时间由引擎按本次打印时间给
  title: () => `员工花名册（部门：${currentDeptPath.value}，记录数：${printableRows().length}）`,
  columns: () => printColumns,
  rows: () => printableRows().map((r: any, i: number) => ({ ...r, __seq: i + 1 })),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，全量按当前筛选） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await hrEmployeeApi.page({ ...params, pageNum: 1, pageSize: 10000 })
    const rows: HrEmployee[] = res?.records || []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['工号', '姓名', '部门', '岗位', '员工类型', '入职日期', '状态', '性别', '学历', '手机号', '邮箱', '离职日期']
    const lines = rows.map(r => [
      r.employeeNo, r.employeeName, r.deptName, r.positionName,
      EMPLOYEE_TYPE_MAP[r.employeeType] || '', formatDate(r.hireDate),
      employeeStatusText(r.status), GENDER_MAP[r.gender] || '', EDUCATION_MAP[r.education] || '',
      r.phone, r.email, formatDate(r.leaveDate),
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `员工花名册_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键字', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'employeeType', label: '员工类型', visible: true },
  { key: 'positionId', label: '岗位', visible: true },
  { key: 'education', label: '学历', visible: true },
  { key: 'gender', label: '性别', visible: true },
  { key: 'hireDate', label: '入职日期', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增员工', enabled: true },
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
  console.error('[员工列表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadDeptTree()
  loadPositions()
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
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.status-hint { margin-left: 8px; color: #999; font-size: 12px; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
