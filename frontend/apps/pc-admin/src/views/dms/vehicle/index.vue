<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        车辆管理（配送 → 人车管理 → 车辆管理，菜单 80770 `dms:vehicle`）
        · 双入口：本页=列表；`dms/vehicle/form`=新增/编辑表单页（菜单「添加」标签）
        · 骨架：CategoryListLayout（无分类树）+ BillTableList（表头序号齿轮列配置）+ PageConfigPanel
        · 工具栏：新增 / 到期提醒 ｜ 页面配置 · 刷新 · 打印(F8) · 导出
        · 行级：编辑 / 状态变更 / 更多（绑定配送员 · 解绑 · 维保记录）
        · 状态机（《车辆管理开发文档》§3.2）：0-空闲 1-使用中 2-维修中 3-已报废(终态) 4-已出勤
        · 人车一对一（§3.3）：绑定/解绑写 `dms_rider_vehicle_binding` 流水
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 + 到期提醒 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('add')"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="isButtonEnabled('expiring')"
              size="small"
              @click="openExpiring"
            >
              <BellOutlined /> 到期提醒
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印 + 导出 ═══ -->
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
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="车牌号/车辆编码/品牌/型号"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('vehicleType')"
                class="search-item"
              >
                <span class="search-label">车辆类型</span>
                <a-select
                  v-model:value="searchForm.vehicleType"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="VEHICLE_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('ownershipType')"
                class="search-item"
              >
                <span class="search-label">归属类型</span>
                <a-select
                  v-model:value="searchForm.ownershipType"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="OWNERSHIP_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">车辆状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('expiringDays')"
                class="search-item"
              >
                <span class="search-label">证件到期</span>
                <a-select
                  v-model:value="searchForm.expiringDays"
                  size="small"
                  style="width: 130px"
                  :options="EXPIRING_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('currentRiderName')"
                class="search-item"
              >
                <span class="search-label">当前配送员</span>
                <a-input
                  v-model:value="searchForm.currentRiderName"
                  placeholder="配送员姓名"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('department')"
                class="search-item"
              >
                <span class="search-label">所属部门</span>
                <a-input
                  v-model:value="searchForm.department"
                  placeholder="部门名称"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('registerDate')"
                class="search-item"
              >
                <span class="search-label">注册日期</span>
                <a-range-picker
                  v-model:value="registerDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 220px"
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列；勾选后出批量操作条） ═══ -->
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
              :show-batch-delete="true"
              :selectable="true"
              storage-key="dms-vehicle-columns"
              global-config-key="dms-vehicle-columns"
              row-key="id"
              @batch-delete="handleBatchDelete"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
            >
              <template #batch-actions>
                <a-button
                  v-if="isButtonEnabled('batchStatus')"
                  size="small"
                  @click="openBatchStatus(selectedRows)"
                >
                  批量启停
                </a-button>
                <a-button
                  v-if="isButtonEnabled('batchUnbind')"
                  size="small"
                  @click="handleBatchUnbind(selectedRows)"
                >
                  批量解绑
                </a-button>
              </template>

              <template #plateCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.plateNo }}</a>
              </template>

              <template #brandModelCell="{ record }">
                <span v-if="!record.__ghost">{{ [record.brand, record.model].filter(Boolean).join(' / ') || '-' }}</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusMeta(record.status).color"
                >
                  {{ record.statusText || statusMeta(record.status).text }}
                </a-tag>
              </template>

              <template #certCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="['cert-text', certClass(record)]"
                >{{ record.certWarnText || '-' }}</span>
              </template>

              <template #riderCell="{ record }">
                <span v-if="!record.__ghost">{{ record.currentRiderName || '-' }}</span>
              </template>

              <template #maintenanceCell="{ record }">
                <span v-if="!record.__ghost">{{ record.lastMaintenanceDate || '-' }}</span>
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
                    编辑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openStatusModal(record)"
                  >
                    状态变更
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多 <DownOutlined style="font-size:10px;" />
                    </a-button>
                    <template #overlay>
                      <a-menu @click="({ key }) => handleMoreAction(key as string, record)">
                        <a-menu-item
                          key="bind"
                          :disabled="!!record.currentRiderId || record.status === 3"
                        >
                          <TeamOutlined /> 绑定配送员
                        </a-menu-item>
                        <a-menu-item
                          key="unbind"
                          :disabled="!record.currentRiderId"
                        >
                          <DisconnectOutlined /> 解绑
                        </a-menu-item>
                        <a-menu-item key="history">
                          <HistoryOutlined /> 绑定流水
                        </a-menu-item>
                        <a-menu-item key="maintenance">
                          <ToolOutlined /> 维保记录
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item
                          key="delete"
                          danger
                        >
                          <DeleteOutlined /> 删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
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

      <!-- ═══ 状态变更弹窗（状态机由后端校验） ═══ -->
      <a-modal
        v-model:open="statusModalVisible"
        title="状态变更"
        :width="420"
        :mask-closable="false"
        :confirm-loading="statusSaving"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleStatusSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="车牌号">
            <span>{{ statusRecord?.plateNo }}</span>
          </a-form-item>
          <a-form-item label="当前状态">
            <a-tag :color="statusMeta(statusRecord?.status).color">
              {{ statusRecord?.statusText || statusMeta(statusRecord?.status).text }}
            </a-tag>
          </a-form-item>
          <a-form-item label="变更至">
            <a-select
              v-model:value="statusTarget"
              placeholder="请选择目标状态"
              :options="statusTargetOptions"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 批量启停弹窗 ═══ -->
      <a-modal
        v-model:open="batchStatusModalVisible"
        title="批量状态变更"
        :width="440"
        :mask-closable="false"
        :confirm-loading="batchStatusSaving"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleBatchStatusSave"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="已选车辆">
            <span>{{ batchStatusRecords.length }} 辆</span>
          </a-form-item>
          <a-form-item label="变更至">
            <a-select
              v-model:value="batchStatusTarget"
              placeholder="请选择目标状态"
              :options="batchStatusOptions"
              style="width: 100%"
            />
          </a-form-item>
          <div class="modal-tip">
            已报废车辆为终态，不可再变更；存在在途任务的车辆不可改为「维修中 / 已报废」。
          </div>
        </a-form>
      </a-modal>

      <!-- ═══ 绑定配送员弹窗（人车一对一） ═══ -->
      <a-modal
        v-model:open="bindModalVisible"
        title="绑定配送员"
        :width="480"
        :mask-closable="false"
        :confirm-loading="bindSaving"
        ok-text="绑定"
        cancel-text="取消"
        @ok="handleBindSave"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <a-form-item label="车牌号">
            <span>{{ bindRecord?.plateNo }}</span>
          </a-form-item>
          <a-form-item label="配送员">
            <a-select
              v-model:value="bindForm.riderId"
              placeholder="请选择配送员（仅可指派）"
              show-search
              option-filter-prop="label"
              :options="riderOptions"
              :loading="riderLoading"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="出车里程(km)">
            <a-input-number
              v-model:value="bindForm.mileage"
              :min="0"
              :precision="0"
              placeholder="交车时回填里程"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="绑定原因">
            <a-input
              v-model:value="bindForm.remark"
              placeholder="如：早班出车 / 临时替班"
              :maxlength="200"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 绑定流水弹窗 ═══ -->
      <a-modal
        v-model:open="historyModalVisible"
        title="绑定 / 解绑流水"
        :width="820"
        :footer="null"
      >
        <a-table
          :data-source="historyRows"
          :columns="historyColumns"
          :loading="historyLoading"
          :pagination="false"
          row-key="id"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="record.status === 0 ? 'blue' : 'default'">
                {{ record.status === 0 ? '绑定中' : record.status === 1 ? '已交车' : '异常解绑' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'bindTime'">
              {{ formatTime(record.bindTime) }}
            </template>
            <template v-else-if="column.dataIndex === 'handoverTime'">
              {{ formatTime(record.handoverTime) || '-' }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 证件到期提醒（保险 / 年检 / 营运证） ═══ -->
      <a-modal
        v-model:open="expiringModalVisible"
        title="证件到期提醒"
        :width="900"
        :footer="null"
      >
        <div class="expiring-toolbar">
          <span>到期窗口</span>
          <a-select
            v-model:value="expiringDays"
            size="small"
            style="width: 120px"
            :options="EXPIRING_OPTIONS.filter(o => o.value)"
            @change="fetchExpiring"
          />
          <a-tag color="red">
            已过期 {{ expiringRows.filter(r => r.warnLevel === 'EXPIRED').length }}
          </a-tag>
          <a-tag color="orange">
            7 天内 {{ expiringRows.filter(r => r.warnLevel === 'WARNING').length }}
          </a-tag>
        </div>
        <a-table
          :data-source="expiringRows"
          :columns="expiringColumns"
          :loading="expiringLoading"
          :pagination="false"
          row-key="rowKey"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'daysLeft'">
              <span :class="['cert-text', record.warnLevel === 'EXPIRED' ? 'cert-expired' : record.warnLevel === 'WARNING' ? 'cert-warning' : 'cert-normal']">
                {{ record.daysLeft < 0 ? `已过期 ${-record.daysLeft} 天` : `${record.daysLeft} 天` }}
              </span>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-vehicle-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="dms-vehicle"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  BellOutlined, DownOutlined, TeamOutlined, DisconnectOutlined, HistoryOutlined, ToolOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'
import {
  vehicleApi,
  type DmsVehicle,
  type DmsVehicleCertExpiry,
  type DmsBindingHistory,
} from '@/api/dms/vehicle'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'DmsVehicle' })

const router = useRouter()

// ═══ 枚举（与后端 VehicleStatusEnum / vehicle_type / ownership_type 一致） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '空闲', color: 'green' },
  1: { text: '使用中', color: 'blue' },
  2: { text: '维修中', color: 'orange' },
  3: { text: '已报废', color: 'default' },
  4: { text: '已出勤', color: 'cyan' },
}

const VEHICLE_TYPE_OPTIONS = [
  { label: '电动车', value: 1 }, { label: '小货车', value: 2 }, { label: '面包车', value: 3 },
  { label: '厢式货车', value: 4 }, { label: '冷藏车', value: 5 }, { label: '三轮车', value: 6 },
]

const OWNERSHIP_OPTIONS = [
  { label: '公司自有', value: 1 }, { label: '个人自带', value: 2 }, { label: '租赁', value: 3 },
]

const STATUS_OPTIONS = Object.entries(STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))

const EXPIRING_OPTIONS = [
  { label: '全部', value: undefined },
  { label: '7 天内到期', value: 7 },
  { label: '30 天内到期', value: 30 },
  { label: '60 天内到期', value: 60 },
  { label: '90 天内到期', value: 90 },
]

function statusMeta(status?: number) {
  return STATUS_MAP[Number(status)] || { text: '-', color: 'default' }
}

function formatTime(val?: string | null) {
  return val ? String(val).replace('T', ' ').slice(0, 16) : ''
}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<DmsVehicle[]>([])

const searchForm = reactive({
  keyword: '',
  vehicleType: undefined as number | undefined,
  ownershipType: undefined as number | undefined,
  status: undefined as number | undefined,
  expiringDays: undefined as number | undefined,
  currentRiderName: '',
  department: '',
})

const registerDateRange = ref<[string, string] | undefined>()

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })
/** 勾选行（批量启停 / 批量解绑使用；比插槽属性名更稳，直接吃 selection-change 事件） */
const selectedRows = ref<DmsVehicle[]>([])

function handleSelectionChange(rows: DmsVehicle[]) {
  selectedRows.value = (rows || []).filter(r => !(r as any).__ghost)
}

// ═══ 数据表列（对齐《车辆管理开发文档》§3.1） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  // BillTableList 无 rowSelection 配置，勾选列需自行声明（批量启停 / 批量解绑 / 批量删除依赖它）
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { title: '车牌号', key: 'plateNo', type: 'slot', slotName: 'plateCell', width: 130, sortable: true, fixed: 'left' },
  { title: '车辆编码', key: 'vehicleCode', width: 130, sortable: true },
  { title: '车辆类型', key: 'vehicleTypeText', width: 110 },
  { title: '品牌/型号', key: 'brandModel', type: 'slot', slotName: 'brandModelCell', width: 200 },
  { title: '归属类型', key: 'ownershipTypeText', width: 100 },
  { title: '核定载重(kg)', key: 'ratedLoad', width: 120 },
  { title: '货厢容积(m³)', key: 'cargoVolume', width: 120 },
  { title: '当前配送员', key: 'currentRiderName', type: 'slot', slotName: 'riderCell', width: 110 },
  { title: '车队长', key: 'vehicleManagerName', width: 100 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 90, sortable: true },
  { title: '证件到期', key: 'certWarnText', type: 'slot', slotName: 'certCell', width: 150 },
  { title: '最近维保', key: 'lastMaintenanceDate', type: 'slot', slotName: 'maintenanceCell', width: 110 },
  { title: '当前里程(km)', key: 'currentMileage', width: 120, sortable: true },
  { title: '所属部门', key: 'department', width: 120 },
  { title: '车主', key: 'ownerName', width: 100, defaultHidden: true },
  { title: '保险到期', key: 'insuranceExpireDate', width: 115, defaultHidden: true, sortable: true },
  { title: '年检到期', key: 'inspectionExpireDate', width: 115, defaultHidden: true },
  { title: '营运证到期', key: 'operatingPermitExpireDate', width: 120, defaultHidden: true },
  { title: '注册日期', key: 'registerDate', width: 115, defaultHidden: true, sortable: true },
  { title: '备注', key: 'remark', width: 180, defaultHidden: true },
]

const historyColumns = [
  { title: '配送员', dataIndex: 'riderName', width: 110 },
  { title: '手机号', dataIndex: 'riderPhone', width: 130 },
  { title: '绑定时间', dataIndex: 'bindTime', width: 150 },
  { title: '绑定里程(km)', dataIndex: 'bindMileage', width: 120 },
  { title: '交车时间', dataIndex: 'handoverTime', width: 150 },
  { title: '交车里程(km)', dataIndex: 'handoverMileage', width: 120 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '绑定原因', dataIndex: 'bindReason', width: 160 },
]

const expiringColumns = [
  { title: '车牌号', dataIndex: 'plateNo', width: 120 },
  { title: '车辆编码', dataIndex: 'vehicleCode', width: 110 },
  { title: '证件', dataIndex: 'certTypeText', width: 80 },
  { title: '到期日', dataIndex: 'certDate', width: 110 },
  { title: '剩余', dataIndex: 'daysLeft', width: 110 },
  { title: '车辆状态', dataIndex: 'statusText', width: 90 },
  { title: '当前配送员', dataIndex: 'currentRiderName', width: 110 },
  { title: '归属类型', dataIndex: 'ownershipTypeText', width: 100 },
  { title: '所属部门', dataIndex: 'department', width: 120 },
]

// ═══ 查询参数 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (searchForm.vehicleType != null) params.vehicleType = searchForm.vehicleType
  if (searchForm.ownershipType != null) params.ownershipType = searchForm.ownershipType
  if (searchForm.status != null) params.status = searchForm.status
  if (searchForm.expiringDays != null) params.expiringDays = searchForm.expiringDays
  if (searchForm.currentRiderName) params.currentRiderName = searchForm.currentRiderName.trim()
  if (searchForm.department) params.department = searchForm.department.trim()
  if (registerDateRange.value?.length === 2) {
    params.registerDateStart = registerDateRange.value[0]
    params.registerDateEnd = registerDateRange.value[1]
  }
  if (sortState.field) {
    params.sortField = sortState.field
    params.sortOrder = sortState.order
  }
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await vehicleApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[车辆管理] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
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
  searchForm.vehicleType = undefined
  searchForm.ownershipType = undefined
  searchForm.status = undefined
  searchForm.expiringDays = undefined
  searchForm.currentRiderName = ''
  searchForm.department = ''
  registerDateRange.value = undefined
  sortState.field = undefined
  sortState.order = undefined
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

/** 表头排序（服务端排序，仅白名单列在后端生效） */
function handleSortChange(key: string | null, order: string | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

// ═══ 新增 / 编辑（双入口 → 表单页） ═══
function handleAdd() {
  router.push('/dms/vehicle/form')
}

function handleEdit(record: DmsVehicle) {
  router.push({ path: '/dms/vehicle/form', query: { id: record.id } })
}

// ═══ 状态变更 ═══
const statusModalVisible = ref(false)
const statusSaving = ref(false)
const statusRecord = ref<DmsVehicle | null>(null)
const statusTarget = ref<number | undefined>(undefined)

const statusTargetOptions = computed(() =>
  STATUS_OPTIONS.filter(o => o.value !== statusRecord.value?.status))

function openStatusModal(record: DmsVehicle) {
  statusRecord.value = record
  statusTarget.value = undefined
  statusModalVisible.value = true
}

async function handleStatusSave() {
  if (!statusRecord.value || statusTarget.value == null) {
    message.warning('请选择目标状态')
    return
  }
  statusSaving.value = true
  try {
    await vehicleApi.updateStatus(statusRecord.value.id, statusTarget.value)
    message.success('状态更新成功')
    statusModalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '状态更新失败')
  } finally {
    statusSaving.value = false
  }
}

// ═══ 批量启停 / 批量报废 ═══
const batchStatusModalVisible = ref(false)
const batchStatusSaving = ref(false)
const batchStatusRecords = ref<DmsVehicle[]>([])
const batchStatusTarget = ref<number | undefined>(undefined)

const batchStatusOptions = computed(() => {
  // 仅保留选中车辆都能流转到的目标状态（已报废为终态，不可再变更）
  return STATUS_OPTIONS.filter(o => !batchStatusRecords.value.some(r => r.status === 3 || r.status === o.value))
})

function openBatchStatus(rows: DmsVehicle[]) {
  const targets = rows.filter(r => !(r as any).__ghost)
  if (targets.length === 0) {
    message.warning('请先勾选车辆')
    return
  }
  batchStatusRecords.value = targets
  batchStatusTarget.value = undefined
  batchStatusModalVisible.value = true
}

async function handleBatchStatusSave() {
  if (batchStatusTarget.value == null) {
    message.warning('请选择目标状态')
    return
  }
  batchStatusSaving.value = true
  try {
    await vehicleApi.batchStatus(batchStatusRecords.value.map(r => r.id), batchStatusTarget.value)
    message.success('批量状态变更成功')
    batchStatusModalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量状态变更失败')
  } finally {
    batchStatusSaving.value = false
  }
}

// ═══ 删除（单条走「更多」菜单，批量走勾选操作条） ═══
function handleDelete(record: DmsVehicle) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除车辆「${record.plateNo}」吗？绑定中或存在在途任务的车辆会被拒绝。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await vehicleApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

async function handleBatchDelete(ids: (number | string)[]) {
  if (!ids || ids.length === 0) {
    message.warning('请先勾选车辆')
    return
  }
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${ids.length} 辆车吗？绑定中或存在在途任务的车辆会被拒绝。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await vehicleApi.batchDelete(ids)
        message.success('批量删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量删除失败')
        fetchList()
      }
    },
  })
}

// ═══ 绑定 / 解绑（人车一对一） ═══
const bindModalVisible = ref(false)
const bindSaving = ref(false)
const bindRecord = ref<DmsVehicle | null>(null)
const riderLoading = ref(false)
const riderOptions = ref<{ label: string; value: number }[]>([])
const bindForm = reactive({ riderId: undefined as number | undefined, mileage: undefined as number | undefined, remark: '' })

async function loadRiderOptions() {
  riderLoading.value = true
  try {
    const res: any = await request.get('/dms/rider/options', { params: { assignable: true } })
    const list: any[] = Array.isArray(res) ? res : res?.records || res?.data || []
    riderOptions.value = list.map((r: any) => ({
      label: `${r.realName || '-'}${r.phone ? '（' + r.phone + '）' : ''}`,
      value: r.id,
    }))
  } catch (error) {
    console.warn('[车辆管理] 加载配送员下拉失败', error)
    riderOptions.value = []
  } finally {
    riderLoading.value = false
  }
}

function openBindModal(record: DmsVehicle) {
  bindRecord.value = record
  bindForm.riderId = undefined
  bindForm.mileage = record.currentMileage ?? undefined
  bindForm.remark = ''
  bindModalVisible.value = true
  loadRiderOptions()
}

async function handleBindSave() {
  if (!bindRecord.value) return
  if (bindForm.riderId == null) {
    message.warning('请选择配送员')
    return
  }
  bindSaving.value = true
  try {
    await vehicleApi.bindRider(bindRecord.value.id, {
      riderId: bindForm.riderId,
      mileage: bindForm.mileage,
      remark: bindForm.remark || undefined,
    })
    message.success('绑定成功')
    bindModalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '绑定失败')
  } finally {
    bindSaving.value = false
  }
}

function handleUnbind(record: DmsVehicle) {
  Modal.confirm({
    title: '确认解绑',
    content: `确定要解绑车辆「${record.plateNo}」的配送员「${record.currentRiderName}」吗？`,
    okText: '确认解绑',
    onOk: async () => {
      try {
        await vehicleApi.unbindRider(record.id, record.currentMileage ?? undefined)
        message.success('解绑成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '解绑失败')
      }
    },
  })
}

async function handleBatchUnbind(rows: DmsVehicle[]) {
  const targets = rows.filter(r => !(r as any).__ghost && r.currentRiderId)
  if (targets.length === 0) {
    message.warning('所选车辆中无绑定中的车辆')
    return
  }
  Modal.confirm({
    title: '确认批量解绑',
    content: `确定要解绑选中的 ${targets.length} 辆车的配送员吗？`,
    okText: '确认解绑',
    onOk: async () => {
      let ok = 0
      for (const row of targets) {
        try {
          await vehicleApi.unbindRider(row.id, row.currentMileage ?? undefined)
          ok++
        } catch (error: any) {
          message.error(`${row.plateNo}：${error?.response?.data?.message || '解绑失败'}`)
        }
      }
      if (ok > 0) message.success(`已解绑 ${ok} 辆车`)
      fetchList()
    },
  })
}

// ═══ 绑定流水 ═══
const historyModalVisible = ref(false)
const historyLoading = ref(false)
const historyRows = ref<DmsBindingHistory[]>([])

async function openHistory(record: DmsVehicle) {
  historyModalVisible.value = true
  historyLoading.value = true
  historyRows.value = []
  try {
    const res: any = await vehicleApi.bindingHistory(record.id)
    historyRows.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载绑定流水失败')
  } finally {
    historyLoading.value = false
  }
}

function handleMoreAction(key: string, record: DmsVehicle) {
  if (key === 'bind') openBindModal(record)
  else if (key === 'unbind') handleUnbind(record)
  else if (key === 'history') openHistory(record)
  else if (key === 'delete') handleDelete(record)
  else if (key === 'maintenance') {
    router.push({ path: '/dms/vehicle/maintenance', query: { vehicleId: record.id, plateNo: record.plateNo } })
  }
}

// ═══ 证件到期提醒 ═══
const expiringModalVisible = ref(false)
const expiringLoading = ref(false)
const expiringDays = ref<number>(30)
const expiringRows = ref<(DmsVehicleCertExpiry & { rowKey: string })[]>([])

async function fetchExpiring() {
  expiringLoading.value = true
  try {
    const res: any = await vehicleApi.expiring({ days: expiringDays.value })
    const list: DmsVehicleCertExpiry[] = Array.isArray(res) ? res : []
    expiringRows.value = list.map((r, i) => ({ ...r, rowKey: `${r.vehicleId}-${r.certType}-${i}` }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载证件到期清单失败')
  } finally {
    expiringLoading.value = false
  }
}

function openExpiring() {
  expiringModalVisible.value = true
  fetchExpiring()
}

function certClass(record: DmsVehicle): string {
  // 后端 Long 统一序列化为字符串（雪花ID 精度保护），此处显式转数值再比较
  const days = record.certDaysLeft == null ? null : Number(record.certDaysLeft)
  if (days == null || Number.isNaN(days)) return ''
  if (days < 0) return 'cert-expired'
  if (days <= 30) return 'cert-warning'
  return 'cert-normal'
}

// ═══ 打印(F8) / 导出 ═══
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'dms-vehicle',
  title: '页面配置',
  columns: () => columns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const blob: any = await vehicleApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `车辆_${new Date().toISOString().slice(0, 10)}.xlsx`
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
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'vehicleType', label: '车辆类型', visible: true },
  { key: 'ownershipType', label: '归属类型', visible: true },
  { key: 'status', label: '车辆状态', visible: true },
  { key: 'expiringDays', label: '证件到期', visible: true },
  { key: 'currentRiderName', label: '当前配送员', visible: true },
  { key: 'department', label: '所属部门', visible: true },
  { key: 'registerDate', label: '注册日期', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'expiring', label: '到期提醒', enabled: true },
  { key: 'batchStatus', label: '批量启停', enabled: true },
  { key: 'batchUnbind', label: '批量解绑', enabled: true },
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
  console.error('[车辆管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
// 双入口表单页保存后 router.push 回本页：若菜单开启 keepAlive，需要 onActivated 重新拉取才能看到新数据
const firstActivate = ref(true)

onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onActivated(() => {
  if (firstActivate.value) {
    firstActivate.value = false
    return
  }
  fetchList()
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 证件到期告警色 */
.cert-text { font-size: 12px; }
.cert-expired { color: #cf1322; font-weight: 600; }
.cert-warning { color: #d46b08; }
.cert-normal { color: #389e0d; }

.expiring-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; font-size: 13px; color: #666; }
.modal-tip { font-size: 12px; color: #fa8c16; line-height: 1.6; padding-left: 4px; }

/* 橙色新增按钮（资料模块统一） */
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

@media print {
  .search-area { display: none !important; }
}
</style>
