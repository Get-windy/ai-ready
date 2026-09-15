<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        车辆维保台账（配送 → 人车管理 → 车辆维护，菜单 80780，无对标页面）
        · 金标准骨架：CategoryListLayout（无分类树）+ BillTableList（表头齿轮列配置，个人/全局）+ PageConfigPanel
        · 工具栏：新增维保 / 到期提醒 / 费用统计 ｜ 页面配置 / 刷新 / 导出
        · 查询：维保单号 · 车辆(选择器) · 维保类型(多选) · 维保日期范围 · 费用区间 · 厂商 · 是否含附件
        · 列表：维保单号/车辆(车牌)/维保类型/维保日期/内容/费用/厂商/维保前·后里程/下次到期/经办人/附件/备注
        · 闭环：维保后里程回写车辆 current_mileage；保养按间隔推算下次到期；年检·保险回写车辆到期日
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增维保 + 到期提醒 + 费用统计 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增维保
            </a-button>
            <a-button
              v-if="isButtonEnabled('expiring')"
              size="small"
              @click="openExpiring"
            >
              <ClockCircleOutlined /> 到期提醒
              <span
                v-if="expiringCount > 0"
                class="due-badge"
              >{{ expiringCount }}</span>
            </a-button>
            <a-button
              v-if="isButtonEnabled('stat')"
              size="small"
              @click="openStat"
            >
              <BarChartOutlined /> 费用统计
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 导出 ═══ -->
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
                v-if="isQueryVisible('maintNo')"
                class="search-item"
              >
                <span class="search-label">维保单号</span>
                <a-input
                  v-model:value="searchForm.maintNo"
                  placeholder="请输入维保单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('vehicleId')"
                class="search-item"
              >
                <span class="search-label">车辆</span>
                <a-select
                  v-model:value="searchForm.vehicleId"
                  placeholder="全部车辆"
                  size="small"
                  style="width: 190px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="vehicleOptionList"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('maintTypes')"
                class="search-item"
              >
                <span class="search-label">维保类型</span>
                <a-select
                  v-model:value="searchForm.maintTypes"
                  placeholder="全部类型"
                  size="small"
                  style="width: 200px"
                  mode="multiple"
                  :max-tag-count="2"
                  allow-clear
                  :options="MAINT_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('maintDateRange')"
                class="search-item"
              >
                <span class="search-label">维保日期</span>
                <a-range-picker
                  v-model:value="searchForm.dateRange"
                  size="small"
                  style="width: 230px"
                  value-format="YYYY-MM-DD"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('maintCost')"
                class="search-item"
              >
                <span class="search-label">费用</span>
                <a-input-number
                  v-model:value="searchForm.costMin"
                  placeholder="最小"
                  size="small"
                  :min="0"
                  style="width: 100px"
                />
                <span class="range-sep">-</span>
                <a-input-number
                  v-model:value="searchForm.costMax"
                  placeholder="最大"
                  size="small"
                  :min="0"
                  style="width: 100px"
                />
              </div>
              <div
                v-if="isQueryVisible('vendor')"
                class="search-item"
              >
                <span class="search-label">厂商</span>
                <!-- 往来单位选择器（选中按档案ID精确过滤；直接输入按名称快照模糊过滤）
                     ⚠️ antd 约定：allowClear / placeholder 必须配在自定义输入子元素上，
                     否则 AutoComplete 会告警「Customize getInputElement should customize clear and placeholder logic」 -->
                <a-auto-complete
                  v-model:value="searchForm.vendor"
                  :options="queryVendorOptions"
                  :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
                  @search="handleQueryVendorSearch"
                  @select="handleQueryVendorSelect"
                  @change="handleQueryVendorChange"
                >
                  <a-input
                    allow-clear
                    size="small"
                    style="width: 200px"
                    placeholder="选择往来单位或输入厂商名称"
                    @press-enter="handleSearch"
                  />
                  <template #option="item">
                    <span>{{ item.label }}</span>
                    <span style="color: #999; font-size: 12px; margin-left: 6px">{{ item.partyTypeText }}</span>
                  </template>
                </a-auto-complete>
              </div>
              <div
                v-if="isQueryVisible('hasAttachment')"
                class="search-item"
              >
                <span class="search-label">附件</span>
                <a-select
                  v-model:value="searchForm.hasAttachment"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  placeholder="全部"
                  :options="[
                    { label: '含附件', value: 1 },
                    { label: '不含附件', value: 0 },
                  ]"
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
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              storage-key="dms-vehicle-maintenance-columns"
              global-config-key="dms-vehicle-maintenance-columns"
              row-key="id"
            >
              <template #noCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.maintNo }}</a>
              </template>

              <template #vehicleCell="{ record }">
                <span v-if="!record.__ghost">{{ vehicleText(record) }}</span>
              </template>

              <template #typeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="MAINT_TYPE_MAP[record.maintType]?.color || 'default'"
                >
                  {{ record.maintTypeText || MAINT_TYPE_MAP[record.maintType]?.text || record.maintType }}
                </a-tag>
              </template>

              <template #contentCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  class="cell-ellipsis"
                  :title="record.maintContent || ''"
                >{{ record.maintContent || '-' }}</span>
              </template>

              <template #dateCell="{ record }">
                <span v-if="!record.__ghost">{{ formatDate(record.maintDate) }}</span>
              </template>

              <template #costCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.maintCost) }}</span>
              </template>

              <template #beforeMileageCell="{ record }">
                <span v-if="!record.__ghost">{{ record.beforeMaintMileage == null ? '-' : record.beforeMaintMileage }}</span>
              </template>

              <template #afterMileageCell="{ record }">
                <span v-if="!record.__ghost">{{ record.afterMaintMileage == null ? '-' : record.afterMaintMileage }}</span>
              </template>

              <template #vendorCell="{ record }">
                <span v-if="!record.__ghost">{{ record.maintVendor || '-' }}</span>
                <a-tag
                  v-if="!record.__ghost && record.vendorId"
                  color="blue"
                  class="vendor-linked-tag"
                >
                  档案
                </a-tag>
              </template>

              <template #nextDueCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="dueClass(record)"
                >{{ nextDueText(record) }}</span>
              </template>

              <template #attachmentCell="{ record }">
                <a-tag
                  v-if="!record.__ghost && hasAttachment(record)"
                  color="blue"
                >
                  {{ attachmentCount(record) }} 个
                </a-tag>
                <span v-else-if="!record.__ghost">-</span>
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
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
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

      <!-- ═══ 新增 / 修改维保记录 ═══ -->
      <a-modal
        v-model:open="modalVisible"
        :title="formState.id ? '修改维保记录' : '新增维保记录'"
        :width="860"
        :mask-closable="false"
        :confirm-loading="saving"
        ok-text="保存(Enter)"
        cancel-text="关闭(Esc)"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item
                label="维保单号"
              >
                <a-input
                  v-model:value="formState.maintNo"
                  disabled
                  placeholder="保存时自动生成（WBD 号段）"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="车辆"
                required
              >
                <a-select
                  v-model:value="formState.vehicleId"
                  placeholder="请选择车辆（车牌号）"
                  show-search
                  option-filter-prop="label"
                  :options="vehicleOptionList"
                  :loading="vehicleLoading"
                  @change="handleVehicleChange"
                />
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item
                label="维保类型"
                required
              >
                <a-select
                  v-model:value="formState.maintType"
                  placeholder="请选择维保类型"
                  :options="MAINT_TYPE_OPTIONS"
                  @change="handleTypeChange"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="维保日期"
                required
              >
                <a-date-picker
                  v-model:value="formState.maintDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  placeholder="请选择维保日期"
                  @change="handleDueRecalc"
                />
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item label="维保前里程(km)">
                <a-input-number
                  :value="vehicleInfo.currentMileage"
                  disabled
                  style="width: 100%"
                  placeholder="选择车辆后自动带出当前里程"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="维保后里程(km)">
                <a-input-number
                  v-model:value="formState.afterMaintMileage"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                  placeholder="维保完成时里程（回写车辆当前里程）"
                  @change="handleDueRecalc"
                />
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item label="维保费用">
                <a-input-number
                  v-model:value="formState.maintCost"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  placeholder="请输入维保费用"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="维保厂商">
                <!-- 生产级口径：厂商引用往来单位档案（供应商/其他往来单位）+ 名称快照；未建档可手工填写 -->
                <a-auto-complete
                  v-model:value="formState.maintVendor"
                  :options="vendorOptions"
                  :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
                  @search="handleVendorSearch"
                  @select="handleVendorSelect"
                  @change="handleVendorChange"
                >
                  <!-- allowClear / placeholder 配在自定义输入上（antd 约定），清空即退回「未建档手工填写」 -->
                  <a-input
                    allow-clear
                    :maxlength="100"
                    placeholder="选择往来单位，或直接填写厂商名称"
                  />
                  <template #option="item">
                    <span>{{ item.label }}</span>
                    <span style="color: #999; font-size: 12px; margin-left: 6px">{{ item.partyTypeText }}</span>
                  </template>
                </a-auto-complete>
                <div class="form-tip">
                  <template v-if="formState.vendorId">
                    已关联往来单位档案（保存后以档案名称记账）
                  </template>
                  <template v-else>
                    未建档厂商可直接填写名称（不计入厂商档案成本分析）
                  </template>
                </div>
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item label="联系人">
                <a-input
                  v-model:value="formState.maintContact"
                  :maxlength="50"
                  placeholder="厂商联系人"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="联系电话">
                <a-input
                  v-model:value="formState.maintPhone"
                  :maxlength="30"
                  placeholder="厂商联系电话"
                />
              </a-form-item>
            </a-col>

            <a-col :span="12">
              <a-form-item label="下次到期日期">
                <a-date-picker
                  v-model:value="formState.nextMaintDate"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  placeholder="按维保类型自动推算，可修改"
                  @change="nextDateTouched = true"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="下次到期里程(km)">
                <a-input-number
                  v-model:value="formState.nextMaintMileage"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                  placeholder="保养按车辆间隔推算，可修改"
                  @change="nextMileageTouched = true"
                />
              </a-form-item>
            </a-col>

            <a-col :span="24">
              <a-form-item
                label="维保内容"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="formState.maintContent"
                  :rows="2"
                  :maxlength="500"
                  placeholder="维保项目 / 更换配件 / 故障描述"
                />
              </a-form-item>
            </a-col>

            <a-col :span="24">
              <a-form-item
                label="附件"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-upload
                  v-model:file-list="fileList"
                  :custom-request="handleUpload"
                  :max-count="9"
                  accept="image/*,.pdf,.doc,.docx,.xls,.xlsx"
                >
                  <a-button size="small">
                    <UploadOutlined /> 上传发票/记录单
                  </a-button>
                </a-upload>
                <div class="form-tip">
                  支持图片与 PDF/Office 文档，单个不超过 20MB，最多 9 个
                </div>
              </a-form-item>
            </a-col>

            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="formState.remark"
                  :rows="2"
                  :maxlength="500"
                  placeholder="请输入备注"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-modal>

      <!-- ═══ 到期提醒 ═══ -->
      <a-modal
        v-model:open="expiringVisible"
        title="维保到期提醒"
        :width="920"
        :footer="null"
      >
        <div class="modal-toolbar">
          <a-space :size="8">
            <span class="search-label">日期窗口(天)</span>
            <a-input-number
              v-model:value="expiringDays"
              :min="1"
              :max="365"
              size="small"
              style="width: 90px"
            />
            <span class="search-label">里程窗口(km)</span>
            <a-input-number
              v-model:value="expiringWarnKm"
              :min="0"
              size="small"
              style="width: 110px"
            />
            <a-button
              type="primary"
              size="small"
              :loading="expiringLoading"
              @click="loadExpiring"
            >
              查询
            </a-button>
          </a-space>
          <span class="search-label">共 {{ expiringRows.length }} 条待提醒</span>
        </div>
        <a-table
          :data-source="expiringRows"
          :columns="expiringColumns"
          :loading="expiringLoading"
          size="small"
          row-key="id"
          :pagination="false"
          :scroll="{ y: 340 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'maintTypeText'">
              <a-tag :color="MAINT_TYPE_MAP[record.maintType]?.color || 'default'">
                {{ record.maintTypeText || MAINT_TYPE_MAP[record.maintType]?.text }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'dueStatus'">
              <a-tag :color="DUE_STATUS_MAP[record.dueStatus]?.color || 'default'">
                {{ DUE_STATUS_MAP[record.dueStatus]?.text || record.dueStatus }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-button
                type="link"
                size="small"
                @click="handleExpiringEdit(record)"
              >
                查看/登记
              </a-button>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 费用统计 ═══ -->
      <a-modal
        v-model:open="statVisible"
        title="维保费用统计"
        :width="860"
        :footer="null"
      >
        <a-spin :spinning="statLoading">
          <a-row
            :gutter="12"
            class="stat-cards"
          >
            <a-col :span="8">
              <div class="stat-card">
                <div class="stat-label">
                  维保记录数
                </div>
                <div class="stat-value">
                  {{ statData.recordCount }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="stat-card">
                <div class="stat-label">
                  维保总费用
                </div>
                <div class="stat-value">
                  {{ formatMoney(statData.totalCost) }}
                </div>
              </div>
            </a-col>
            <a-col :span="8">
              <div class="stat-card">
                <div class="stat-label">
                  涉及车辆
                </div>
                <div class="stat-value">
                  {{ statData.vehicleCount }}
                </div>
              </div>
            </a-col>
          </a-row>
          <a-tabs>
            <a-tab-pane
              key="type"
              tab="按维保类型"
            >
              <a-table
                :data-source="statData.byType"
                :columns="statTypeColumns"
                size="small"
                row-key="type"
                :pagination="false"
              />
            </a-tab-pane>
            <a-tab-pane
              key="month"
              tab="按月份"
            >
              <a-table
                :data-source="statData.byMonth"
                :columns="statMonthColumns"
                size="small"
                row-key="month"
                :pagination="false"
              />
            </a-tab-pane>
            <a-tab-pane
              key="vehicle"
              tab="按车辆"
            >
              <a-table
                :data-source="statData.byVehicle"
                :columns="statVehicleColumns"
                size="small"
                row-key="plateNo"
                :pagination="false"
              />
            </a-tab-pane>
            <a-tab-pane
              key="vendor"
              tab="按厂商"
            >
              <a-table
                :data-source="statData.byVendor"
                :columns="statVendorColumns"
                size="small"
                row-key="vendorKey"
                :pagination="false"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'linked'">
                    <a-tag
                      v-if="record.linked"
                      color="blue"
                    >
                      档案
                    </a-tag>
                    <span v-else>手工</span>
                  </template>
                </template>
              </a-table>
            </a-tab-pane>
          </a-tabs>
        </a-spin>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-vehicle-maintenance-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DownloadOutlined,
  SettingOutlined,
  ClockCircleOutlined,
  BarChartOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import request from '@/utils/request'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { vehicleMaintenanceApi, type MaintenanceVO } from '@/api/dms/vehicle-maintenance'

defineOptions({ name: 'DmsVehicleMaintenance' })

const route = useRoute()

// ═══ 枚举（与后端 MaintTypeEnum 一一对应：1-保养 … 6-其他） ═══
const MAINT_TYPE_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '保养', color: 'blue' },
  2: { text: '维修', color: 'orange' },
  3: { text: '年检', color: 'purple' },
  4: { text: '保险', color: 'cyan' },
  5: { text: '事故', color: 'red' },
  6: { text: '其他', color: 'default' },
}
const MAINT_TYPE_OPTIONS = Object.keys(MAINT_TYPE_MAP)
  .map(k => ({ label: MAINT_TYPE_MAP[Number(k)].text, value: Number(k) }))

const DUE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  OVERDUE: { text: '已逾期', color: 'red' },
  DUE_SOON: { text: '即将到期', color: 'orange' },
  NORMAL: { text: '正常', color: 'green' },
  NONE: { text: '无周期', color: 'default' },
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const tableData = ref<MaintenanceVO[]>([])

// ═══ 查询条件 ═══
const searchForm = reactive({
  maintNo: '',
  vehicleId: undefined as number | undefined,
  maintTypes: [] as number[],
  dateRange: [] as string[],
  costMin: undefined as number | undefined,
  costMax: undefined as number | undefined,
  vendor: '',
  vendorId: undefined as number | string | undefined,
  hasAttachment: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 车辆选择器（车辆档案 /options，禁止手输 ID） ═══
const vehicleLoading = ref(false)
const vehicleOptions = ref<any[]>([])
const vehicleOptionList = computed(() =>
  vehicleOptions.value.map(v => ({
    label: vehicleLabel(v),
    value: v.id,
  })))

function vehicleLabel(v: any): string {
  const name = v.plateNo || `车辆#${v.id}`
  const extra = [v.brand, v.model].filter(Boolean).join(' ')
  return extra ? `${name}（${extra}）` : name
}

async function loadVehicleOptions() {
  vehicleLoading.value = true
  try {
    const res: any = await vehicleMaintenanceApi.vehicleOptions()
    vehicleOptions.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error) {
    console.warn('[车辆维护] 车辆下拉加载失败', error)
    vehicleOptions.value = []
  } finally {
    vehicleLoading.value = false
  }
}

// ═══ 厂商选择器（资料 → 往来单位：供应商 / 其他往来单位；可搜索、可自由填写） ═══
/** 弹窗内厂商候选 */
const vendorOptions = ref<any[]>([])
/** 查询区厂商候选 */
const queryVendorOptions = ref<any[]>([])
/** 当前选中的往来单位名称（用于区分「选中档案」与「手工改名」） */
const selectedVendorName = ref('')
const querySelectedVendorName = ref('')
let vendorSearchTimer: any = null
let queryVendorSearchTimer: any = null

function mapVendorOptions(rows: any[]): any[] {
  return (rows || []).map(v => ({
    value: v.name,
    id: v.id,
    name: v.name,
    label: v.code ? `${v.name} [${v.code}]` : v.name,
    partyTypeText: v.partyTypeText || '',
  }))
}

async function queryVendors(keyword: string, target: 'form' | 'query') {
  try {
    const res: any = await vehicleMaintenanceApi.vendorOptions({ keyword: keyword || undefined, limit: 20 })
    const rows = Array.isArray(res) ? res : (res?.records || [])
    const options = mapVendorOptions(rows)
    if (target === 'form') vendorOptions.value = options
    else queryVendorOptions.value = options
  } catch (error) {
    console.warn('[车辆维护] 厂商候选加载失败', error)
  }
}

/** 输入即搜（300ms 防抖），避免逐字打接口 */
function handleVendorSearch(keyword: string) {
  if (vendorSearchTimer) clearTimeout(vendorSearchTimer)
  vendorSearchTimer = setTimeout(() => queryVendors(keyword, 'form'), 300)
}

function handleQueryVendorSearch(keyword: string) {
  if (queryVendorSearchTimer) clearTimeout(queryVendorSearchTimer)
  queryVendorSearchTimer = setTimeout(() => queryVendors(keyword, 'query'), 300)
}

/** 弹窗：选中往来单位 → 记 vendorId，名称以档案为准 */
function handleVendorSelect(value: any, option: any) {
  formState.vendorId = option?.id ?? undefined
  selectedVendorName.value = option?.name || String(value || '')
  formState.maintVendor = selectedVendorName.value
}

/** 弹窗：手工改写名称 → 解除档案引用（自由填写兜底） */
function handleVendorChange(value: any) {
  if (formState.vendorId != null && value !== selectedVendorName.value) {
    formState.vendorId = undefined
    selectedVendorName.value = ''
  }
}

/** 查询区：选中往来单位 → 按档案ID精确过滤 */
function handleQueryVendorSelect(value: any, option: any) {
  searchForm.vendorId = option?.id ?? undefined
  querySelectedVendorName.value = option?.name || String(value || '')
  searchForm.vendor = querySelectedVendorName.value
  handleSearch()
}

/** 查询区：手工改写 → 退回名称模糊过滤 */
function handleQueryVendorChange(value: any) {
  if (searchForm.vendorId != null && value !== querySelectedVendorName.value) {
    searchForm.vendorId = undefined
    querySelectedVendorName.value = ''
  }
}

// ═══ 数据表列 ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' },
  { title: '维保单号', key: 'maintNo', type: 'slot', slotName: 'noCell', width: 130 },
  { title: '车辆(车牌号)', key: 'plateNo', type: 'slot', slotName: 'vehicleCell', width: 170 },
  { title: '维保类型', key: 'maintType', type: 'slot', slotName: 'typeCell', width: 100 },
  { title: '维保日期', key: 'maintDate', type: 'slot', slotName: 'dateCell', width: 110 },
  { title: '维保内容', key: 'maintContent', type: 'slot', slotName: 'contentCell', width: 240 },
  { title: '费用', key: 'maintCost', type: 'slot', slotName: 'costCell', width: 110 },
  { title: '维保厂商', key: 'maintVendor', type: 'slot', slotName: 'vendorCell', width: 190 },
  { title: '维保前里程(km)', key: 'beforeMaintMileage', type: 'slot', slotName: 'beforeMileageCell', width: 120 },
  { title: '维保后里程(km)', key: 'afterMaintMileage', type: 'slot', slotName: 'afterMileageCell', width: 120 },
  { title: '下次到期', key: 'nextMaintDate', type: 'slot', slotName: 'nextDueCell', width: 190 },
  { title: '经办人', key: 'handlerName', width: 100 },
  { title: '附件', key: 'attachmentUrls', type: 'slot', slotName: 'attachmentCell', width: 90 },
  { title: '备注', key: 'remark', width: 180, defaultHidden: true },
]

// ═══ 查询参数 ═══
function buildQuery(): Record<string, any> {
  const params: Record<string, any> = {}
  if (searchForm.maintNo) params.maintNo = searchForm.maintNo.trim()
  if (searchForm.vehicleId != null) params.vehicleId = searchForm.vehicleId
  if (searchForm.maintTypes?.length) params.maintTypes = searchForm.maintTypes.join(',')
  if (searchForm.dateRange?.length === 2) {
    params.dateFrom = searchForm.dateRange[0]
    params.dateTo = searchForm.dateRange[1]
  }
  if (searchForm.costMin != null) params.costMin = searchForm.costMin
  if (searchForm.costMax != null) params.costMax = searchForm.costMax
  // 厂商：选中档案按 ID 精确过滤（改名不影响历史命中）；手工输入按名称快照模糊
  if (searchForm.vendorId != null) params.vendorId = searchForm.vendorId
  else if (searchForm.vendor) params.vendor = searchForm.vendor.trim()
  if (searchForm.hasAttachment != null) params.hasAttachment = searchForm.hasAttachment
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await vehicleMaintenanceApi.page({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[车辆维护] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
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

// ═══ 展示工具 ═══
function formatDate(val?: string | null): string {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

function formatMoney(val?: number | null): string {
  if (val == null) return '-'
  return Number(val).toFixed(2)
}

function vehicleText(record: MaintenanceVO): string {
  if (!record.plateNo) return record.vehicleId ? `车辆#${record.vehicleId}` : '-'
  const extra = [record.vehicleBrand, record.vehicleModel].filter(Boolean).join(' ')
  return extra ? `${record.plateNo}（${extra}）` : record.plateNo
}

function nextDueText(record: MaintenanceVO): string {
  const parts: string[] = []
  if (record.nextMaintDate) parts.push(formatDate(record.nextMaintDate))
  if (record.nextMaintMileage != null) parts.push(`${record.nextMaintMileage}km`)
  return parts.length ? parts.join(' / ') : '-'
}

function dueClass(record: MaintenanceVO): string {
  if (record.dueStatus === 'OVERDUE') return 'due-overdue'
  if (record.dueStatus === 'DUE_SOON') return 'due-soon'
  return ''
}

function parseAttachments(record: MaintenanceVO): string[] {
  const raw = record?.attachmentUrls
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return []
  }
}

function hasAttachment(record: MaintenanceVO): boolean {
  return parseAttachments(record).length > 0
}

function attachmentCount(record: MaintenanceVO): number {
  return parseAttachments(record).length
}

// ═══ 新增 / 修改 ═══
const modalVisible = ref(false)
const formRef = ref()
const nextDateTouched = ref(false)
const nextMileageTouched = ref(false)
const fileList = ref<any[]>([])
const vehicleInfo = reactive<{ currentMileage?: number; maintenanceIntervalKm?: number }>({})

const formState = reactive({
  id: null as number | null,
  maintNo: '',
  vehicleId: undefined as number | undefined,
  maintType: undefined as number | undefined,
  maintDate: dayjs().format('YYYY-MM-DD'),
  maintContent: '',
  maintCost: undefined as number | undefined,
  maintVendor: '',
  /** 厂商往来单位ID（选中档案才有值；手工填写厂商时为空；雪花ID按字符串透传） */
  vendorId: undefined as number | string | undefined,
  maintContact: '',
  maintPhone: '',
  afterMaintMileage: undefined as number | undefined,
  nextMaintDate: undefined as string | undefined,
  nextMaintMileage: undefined as number | undefined,
  remark: '',
})

function resetForm() {
  formState.id = null
  formState.maintNo = ''
  formState.vehicleId = undefined
  formState.maintType = undefined
  formState.maintDate = dayjs().format('YYYY-MM-DD')
  formState.maintContent = ''
  formState.maintCost = undefined
  formState.maintVendor = ''
  formState.vendorId = undefined
  selectedVendorName.value = ''
  formState.maintContact = ''
  formState.maintPhone = ''
  formState.afterMaintMileage = undefined
  formState.nextMaintDate = undefined
  formState.nextMaintMileage = undefined
  formState.remark = ''
  fileList.value = []
  nextDateTouched.value = false
  nextMileageTouched.value = false
  vehicleInfo.currentMileage = undefined
  vehicleInfo.maintenanceIntervalKm = undefined
}

async function handleAdd() {
  resetForm()
  modalVisible.value = true
  // 从《车辆管理》「维保记录」进入时带车过来，直接预选并带出当前里程
  const preset = route.query.vehicleId ? Number(route.query.vehicleId) : undefined
  if (preset) {
    formState.vehicleId = preset
    await handleVehicleChange(preset)
  }
  try {
    const no: any = await vehicleMaintenanceApi.nextNo()
    formState.maintNo = no || ''
  } catch (error) {
    console.warn('[车辆维护] 生成维保单号失败', error)
  }
}

async function handleEdit(record: MaintenanceVO) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await vehicleMaintenanceApi.detail(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.maintNo = detail.maintNo || ''
    formState.vehicleId = detail.vehicleId
    formState.maintType = detail.maintType
    formState.maintDate = detail.maintDate ? String(detail.maintDate).slice(0, 10) : undefined
    formState.maintContent = detail.maintContent || ''
    formState.maintCost = detail.maintCost != null ? Number(detail.maintCost) : undefined
    formState.maintVendor = detail.maintVendor || ''
    formState.vendorId = detail.vendorId ?? undefined
    selectedVendorName.value = detail.vendorId ? (detail.maintVendor || '') : ''
    formState.maintContact = detail.maintContact || ''
    formState.maintPhone = detail.maintPhone || ''
    formState.afterMaintMileage = detail.afterMaintMileage
    formState.nextMaintDate = detail.nextMaintDate ? String(detail.nextMaintDate).slice(0, 10) : undefined
    formState.nextMaintMileage = detail.nextMaintMileage
    formState.remark = detail.remark || ''
    fileList.value = parseAttachments(detail).map((url, i) => ({
      uid: `${i}-${url}`,
      name: url.split('/').pop() || `附件${i + 1}`,
      status: 'done',
      url,
    }))
    // 编辑时既有到期值视为已确认，不再被自动推算覆盖
    nextDateTouched.value = !!detail.nextMaintDate
    nextMileageTouched.value = detail.nextMaintMileage != null
    await loadVehicleInfo(detail.vehicleId)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载维保记录失败')
  }
}

/** 选择车辆：带出当前里程 / 保养间隔（维保前里程默认值 + 下次到期推算依据） */
async function handleVehicleChange(vehicleId: number) {
  await loadVehicleInfo(vehicleId)
  handleDueRecalc()
}

async function loadVehicleInfo(vehicleId?: number) {
  if (vehicleId == null) return
  const hit = vehicleOptions.value.find(v => v.id === vehicleId)
  if (hit) {
    vehicleInfo.currentMileage = hit.currentMileage
    vehicleInfo.maintenanceIntervalKm = hit.maintenanceIntervalKm
  }
  try {
    const detail: any = await request.get(`/dms/vehicle/${vehicleId}`)
    if (detail) {
      vehicleInfo.currentMileage = detail.currentMileage ?? vehicleInfo.currentMileage
      vehicleInfo.maintenanceIntervalKm = detail.maintenanceIntervalKm ?? vehicleInfo.maintenanceIntervalKm
    }
  } catch (error) {
    console.warn('[车辆维护] 车辆详情加载失败', error)
  }
}

function handleTypeChange() {
  // 类型变化：未手工改过的到期值按新类型重算
  nextDateTouched.value = false
  nextMileageTouched.value = false
  handleDueRecalc()
}

/** 下次到期推算（与后端 computeNextDate / computeNextMileage 同口径，未手工修改时才覆盖） */
function handleDueRecalc() {
  const type = formState.maintType
  const date = formState.maintDate
  if (!nextDateTouched.value) {
    if ((type === 1) && date) {
      formState.nextMaintDate = dayjs(date).add(90, 'day').format('YYYY-MM-DD')
    } else if ((type === 3 || type === 4) && date) {
      formState.nextMaintDate = dayjs(date).add(1, 'year').format('YYYY-MM-DD')
    } else {
      formState.nextMaintDate = undefined
    }
  }
  if (!nextMileageTouched.value) {
    if (type === 1 && formState.afterMaintMileage != null) {
      const interval = vehicleInfo.maintenanceIntervalKm && vehicleInfo.maintenanceIntervalKm > 0
        ? vehicleInfo.maintenanceIntervalKm : 5000
      formState.nextMaintMileage = Number(formState.afterMaintMileage) + interval
    } else {
      formState.nextMaintMileage = undefined
    }
  }
}

/** 附件上传（复用平台通用上传契约 POST /api/file/upload） */
async function handleUpload(options: any) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res: any = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) throw new Error('上传返回缺少 url')
    options.onSuccess({ url }, options.file)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '附件上传失败')
    options.onError(error)
  }
}

function collectAttachmentUrls(): string {
  const urls = fileList.value
    .map(f => f.url || f.response?.url)
    .filter(Boolean)
  return urls.length ? JSON.stringify(urls) : ''
}

async function handleSave() {
  if (formState.vehicleId == null) {
    message.warning('请选择车辆')
    return
  }
  if (formState.maintType == null) {
    message.warning('请选择维保类型')
    return
  }
  if (!formState.maintDate) {
    message.warning('请选择维保日期')
    return
  }
  if (formState.afterMaintMileage != null && vehicleInfo.currentMileage != null
    && Number(formState.afterMaintMileage) < Number(vehicleInfo.currentMileage)) {
    message.warning('维保后里程不能小于车辆当前里程（维保前里程）')
    return
  }
  if (formState.nextMaintMileage != null && formState.afterMaintMileage != null
    && Number(formState.nextMaintMileage) <= Number(formState.afterMaintMileage)) {
    message.warning('下次到期里程应大于维保后里程')
    return
  }

  const payload = {
    vehicleId: formState.vehicleId,
    maintType: formState.maintType,
    maintDate: formState.maintDate,
    maintContent: formState.maintContent || null,
    maintCost: formState.maintCost ?? null,
    maintVendor: formState.maintVendor || null,
    vendorId: formState.vendorId ?? null,
    maintContact: formState.maintContact || null,
    maintPhone: formState.maintPhone || null,
    afterMaintMileage: formState.afterMaintMileage ?? null,
    nextMaintDate: formState.nextMaintDate || null,
    nextMaintMileage: formState.nextMaintMileage ?? null,
    attachmentUrls: collectAttachmentUrls(),
    remark: formState.remark || null,
  }

  saving.value = true
  try {
    if (formState.id) {
      await vehicleMaintenanceApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await vehicleMaintenanceApi.create(payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchList()
    loadExpiringCount()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: MaintenanceVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除维保记录「${record.maintNo}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await vehicleMaintenanceApi.remove(record.id)
        message.success('删除成功')
        fetchList()
        loadExpiringCount()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 到期提醒 ═══
const expiringVisible = ref(false)
const expiringLoading = ref(false)
const expiringRows = ref<MaintenanceVO[]>([])
const expiringDays = ref(30)
const expiringWarnKm = ref(1000)
const expiringCount = ref(0)

const expiringColumns: any[] = [
  { title: '维保单号', dataIndex: 'maintNo', width: 120 },
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '维保类型', dataIndex: 'maintTypeText', width: 90 },
  { title: '维保日期', dataIndex: 'maintDate', width: 105 },
  { title: '维保后里程(km)', dataIndex: 'afterMaintMileage', width: 120 },
  { title: '下次到期日期', dataIndex: 'nextMaintDate', width: 120 },
  { title: '剩余天数', dataIndex: 'remainDays', width: 90 },
  { title: '下次到期里程(km)', dataIndex: 'nextMaintMileage', width: 140 },
  { title: '剩余公里', dataIndex: 'remainKm', width: 90 },
  { title: '状态', dataIndex: 'dueStatus', width: 90 },
  { title: '操作', dataIndex: 'action', width: 90, fixed: 'right' },
]

async function loadExpiring() {
  expiringLoading.value = true
  try {
    const res: any = await vehicleMaintenanceApi.expiring({
      days: expiringDays.value,
      warnKm: expiringWarnKm.value,
    })
    expiringRows.value = Array.isArray(res) ? res : (res?.records || [])
  } catch (error: any) {
    message.error(error?.response?.data?.message || '到期提醒加载失败')
  } finally {
    expiringLoading.value = false
  }
}

/** 顶部按钮角标：静默加载，不影响主流程 */
async function loadExpiringCount() {
  try {
    const res: any = await vehicleMaintenanceApi.expiring({ days: 30, warnKm: 1000 })
    expiringCount.value = Array.isArray(res) ? res.length : (res?.records?.length || 0)
  } catch {
    expiringCount.value = 0
  }
}

function openExpiring() {
  expiringVisible.value = true
  loadExpiring()
}

function handleExpiringEdit(record: MaintenanceVO) {
  expiringVisible.value = false
  handleEdit(record)
}

// ═══ 费用统计 ═══
const statVisible = ref(false)
const statLoading = ref(false)
const statData = reactive<any>({
  recordCount: 0,
  totalCost: 0,
  vehicleCount: 0,
  byType: [],
  byMonth: [],
  byVehicle: [],
  byVendor: [],
})

const statTypeColumns: any[] = [
  { title: '维保类型', dataIndex: 'typeText', width: 140 },
  { title: '记录数', dataIndex: 'count', width: 100 },
  { title: '费用合计', dataIndex: 'cost', width: 140 },
]
const statMonthColumns: any[] = [
  { title: '月份', dataIndex: 'month', width: 140 },
  { title: '记录数', dataIndex: 'count', width: 100 },
  { title: '费用合计', dataIndex: 'cost', width: 140 },
]
const statVehicleColumns: any[] = [
  { title: '车牌号', dataIndex: 'plateNo', width: 180 },
  { title: '记录数', dataIndex: 'count', width: 100 },
  { title: '费用合计', dataIndex: 'cost', width: 140 },
]
const statVendorColumns: any[] = [
  { title: '维保厂商', dataIndex: 'vendorName', width: 220 },
  { title: '来源', dataIndex: 'linked', width: 100 },
  { title: '记录数', dataIndex: 'count', width: 100 },
  { title: '费用合计', dataIndex: 'cost', width: 140 },
]

async function openStat() {
  statVisible.value = true
  await loadStat()
}

async function loadStat() {
  statLoading.value = true
  try {
    const res: any = await vehicleMaintenanceApi.stat(buildQuery())
    Object.assign(statData, {
      recordCount: res?.recordCount || 0,
      totalCost: res?.totalCost || 0,
      vehicleCount: res?.vehicleCount || 0,
      byType: res?.byType || [],
      byMonth: res?.byMonth || [],
      byVehicle: res?.byVehicle || [],
      byVendor: res?.byVendor || [],
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '费用统计加载失败')
  } finally {
    statLoading.value = false
  }
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const blob: any = await vehicleMaintenanceApi.export(buildQuery())
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `车辆维保_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'maintNo', label: '维保单号', visible: true },
  { key: 'vehicleId', label: '车辆', visible: true },
  { key: 'maintTypes', label: '维保类型', visible: true },
  { key: 'maintDateRange', label: '维保日期', visible: true },
  { key: 'maintCost', label: '费用区间', visible: true },
  { key: 'vendor', label: '维保厂商', visible: true },
  { key: 'hasAttachment', label: '是否含附件', visible: false },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增维保', enabled: true },
  { key: 'expiring', label: '到期提醒', enabled: true },
  { key: 'stat', label: '费用统计', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
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
  console.error('[车辆维护] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  await loadVehicleOptions()
  // 预热厂商候选（首屏即列出常用往来单位，未输入也可直接选）
  queryVendors('', 'form')
  queryVendors('', 'query')
  // 从《车辆管理》「维保记录」进入：按车辆过滤
  const preset = route.query.vehicleId ? Number(route.query.vehicleId) : undefined
  if (preset) {
    searchForm.vehicleId = preset
  }
  fetchList()
  loadExpiringCount()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.range-sep { color: #999; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

/* 橙色新增按钮（系统统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* 到期提醒角标 */
.due-badge {
  display: inline-block;
  min-width: 16px;
  height: 16px;
  line-height: 16px;
  padding: 0 4px;
  margin-left: 4px;
  border-radius: 8px;
  background: #ff4d4f;
  color: #fff;
  font-size: 11px;
  text-align: center;
}

/* 列表到期着色 */
.due-overdue { color: #ff4d4f; }
.due-soon { color: #fa8c16; }

.form-tip { font-size: 12px; color: #999; margin-top: 4px; }

/* 列内「档案」标记（厂商已关联往来单位） */
.vendor-linked-tag { margin-left: 4px; transform: scale(0.9); }

/* 弹窗内工具条 */
.modal-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

/* 统计卡片 */
.stat-cards { margin-bottom: 12px; }
.stat-card {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 12px;
  background: #fafafa;
}
.stat-label { font-size: 12px; color: #888; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; margin-top: 4px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
