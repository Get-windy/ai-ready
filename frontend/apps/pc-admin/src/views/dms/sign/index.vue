<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        签收管理（配送 → 配送跟踪 → 签收管理，菜单 80900 `dms:sign`）
        · 骨架：CategoryListLayout（无分类树）+ BillTableList（表头序号齿轮列配置）+ PageConfigPanel
        · 审核流转（《签收管理开发文档》§3.4）：
            提交 → 待审核(0) ──通过──> 已通过(1) → 任务「已完成(6)」→ 触发结算/代收货款（事件外发）
                             └──驳回──> 已驳回(2) → 任务退回「配送中(4)」，可重新签收
        · 定位偏差：超阈值只标记待复核，不拒绝签收（§3.6.2）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：批量审核 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-dropdown v-if="isButtonEnabled('batchAudit')">
              <a-button
                size="small"
                :disabled="selectedRows.length === 0"
              >
                <AuditOutlined /> 批量审核 <DownOutlined style="font-size:10px;" />
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => openBatchAudit(key as string)">
                  <a-menu-item key="approve">
                    <CheckCircleOutlined /> 批量通过
                  </a-menu-item>
                  <a-menu-item key="reject">
                    <CloseCircleOutlined /> 批量驳回
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <span
              v-if="selectedRows.length > 0"
              class="sel-tip"
            >已选 {{ selectedRows.length }} 条</span>
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
              @click="refreshAll"
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
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="任务编号/订单号/客户/备注"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('riderId')"
                class="search-item"
              >
                <span class="search-label">配送员</span>
                <a-select
                  v-model:value="searchForm.riderId"
                  placeholder="全部"
                  size="small"
                  style="width: 150px"
                  show-search
                  option-filter-prop="label"
                  allow-clear
                  :options="riderOptions"
                  :loading="riderLoading"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('signTypes')"
                class="search-item"
              >
                <span class="search-label">签收类型</span>
                <a-select
                  v-model:value="searchForm.signTypes"
                  mode="multiple"
                  placeholder="全部"
                  size="small"
                  style="min-width: 150px"
                  max-tag-count="responsive"
                  :options="SIGN_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('auditStatusList')"
                class="search-item"
              >
                <span class="search-label">审核状态</span>
                <a-select
                  v-model:value="searchForm.auditStatusList"
                  mode="multiple"
                  placeholder="全部"
                  size="small"
                  style="min-width: 150px"
                  max-tag-count="responsive"
                  :options="AUDIT_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('signTime')"
                class="search-item"
              >
                <span class="search-label">签收时间</span>
                <a-range-picker
                  v-model:value="signTimeRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 220px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('onlyWarning')"
                class="search-item"
              >
                <a-checkbox
                  v-model:checked="searchForm.onlyWarning"
                  @change="handleSearch"
                >
                  仅看超阈值
                </a-checkbox>
              </div>
              <div
                v-if="isQueryVisible('hasSignature')"
                class="search-item"
              >
                <span class="search-label">手写签名</span>
                <a-select
                  v-model:value="searchForm.hasSignature"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="YES_NO_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('hasPhoto')"
                class="search-item"
              >
                <span class="search-label">签收照片</span>
                <a-select
                  v-model:value="searchForm.hasPhoto"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="YES_NO_OPTIONS"
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
            <!-- 统计条（签收率 / 超阈值率 / 拒收率，§3.5 /sign/stat） -->
            <div
              v-if="isButtonEnabled('statBar')"
              class="stat-bar"
            >
              <div class="stat-item">
                <span class="stat-label">签收记录</span>
                <span class="stat-value">{{ stat.total }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">待审核</span>
                <span class="stat-value stat-warn">{{ stat.pending }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">审核通过率</span>
                <span class="stat-value stat-ok">{{ fmtRate(stat.approveRate) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">超阈值率</span>
                <span class="stat-value stat-warn">{{ fmtRate(stat.warningRate) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">拒收率</span>
                <span class="stat-value stat-danger">{{ fmtRate(stat.rejectRate) }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">部分签收</span>
                <span class="stat-value">{{ stat.partialCount }}</span>
              </div>
            </div>

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
              :selectable="true"
              storage-key="dms-sign-columns"
              global-config-key="dms-sign-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
            >
              <template #taskCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record)"
                >{{ record.taskNo || record.taskId }}</a>
              </template>

              <template #riderCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.riderName || '-' }}
                  <span
                    v-if="record.riderPhone"
                    class="sub-text"
                  >{{ record.riderPhone }}</span>
                </span>
              </template>

              <template #signTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="SIGN_TYPE_MAP[record.signType]?.color"
                >
                  {{ record.signTypeText || SIGN_TYPE_MAP[record.signType]?.label || '-' }}
                </a-tag>
              </template>

              <template #deviationCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  :class="record.locationWarning === 1 ? 'dev-warn' : ''"
                >
                  {{ record.locationDeviation != null ? `${Math.round(Number(record.locationDeviation))} 米` : '-' }}
                  <span v-if="record.locationWarning === 1">· 超阈值</span>
                </span>
              </template>

              <template #proofCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  class="proof-cell"
                >
                  <a-tag
                    v-if="(record.photoCount || 0) > 0"
                    color="blue"
                  >{{ record.photoCount }} 张</a-tag>
                  <a-tag
                    v-else
                    color="default"
                  >无照片</a-tag>
                  <a-tag :color="record.hasSignature ? 'green' : 'default'">
                    {{ record.hasSignature ? '有签名' : '无签名' }}
                  </a-tag>
                </span>
              </template>

              <template #qtyCell="{ record }">
                <span v-if="!record.__ghost">
                  {{ record.signType === 2
                    ? `${fmtNum(record.actualQuantity)} / ${fmtNum(record.plannedQuantity)}`
                    : (record.signType === 3 ? '-' : fmtNum(record.plannedQuantity)) }}
                </span>
              </template>

              <template #signPosCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtCoord(record.signLat, record.signLng) }}</span>
              </template>

              <template #customerPosCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtCoord(record.customerLat, record.customerLng) }}</span>
              </template>

              <template #auditCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="AUDIT_MAP[record.auditStatus ?? 0]?.color"
                >
                  {{ record.auditStatusText || AUDIT_MAP[record.auditStatus ?? 0]?.label }}
                </a-tag>
              </template>

              <template #auditTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDateTime(record.auditTime) }}</span>
              </template>

              <template #signTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtDateTime(record.signTime) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openDetail(record)"
                  >
                    查看详情
                  </a-button>
                  <a-button
                    v-if="record.auditStatus === 0"
                    type="link"
                    size="small"
                    @click="openAudit(record, 1)"
                  >
                    审核
                  </a-button>
                  <a-button
                    v-if="record.auditStatus === 0"
                    type="link"
                    size="small"
                    danger
                    @click="openAudit(record, 2)"
                  >
                    驳回
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

      <!-- ═══ 详情抽屉：签收四要素 + 位置对比 + 审核轨迹 ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="签收详情"
        :width="720"
        destroy-on-close
      >
        <template v-if="current">
          <a-descriptions
            :column="2"
            bordered
            size="small"
          >
            <a-descriptions-item label="任务编号">{{ current.taskNo || current.taskId }}</a-descriptions-item>
            <a-descriptions-item label="任务状态">
              {{ current.taskStatusText || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="配送员">
              {{ current.riderName || '-' }} {{ current.riderPhone ? `（${current.riderPhone}）` : '' }}
            </a-descriptions-item>
            <a-descriptions-item label="配送车辆">{{ current.vehicleName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="客户">{{ current.customerName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="客户电话">{{ current.customerPhone || '-' }}</a-descriptions-item>
            <a-descriptions-item label="签收类型">
              <a-tag :color="SIGN_TYPE_MAP[current.signType]?.color">
                {{ current.signTypeText || SIGN_TYPE_MAP[current.signType]?.label }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="签收数量">
              <template v-if="current.signType === 2">
                {{ fmtNum(current.actualQuantity) }} / {{ fmtNum(current.plannedQuantity) }}（部分）
              </template>
              <template v-else>{{ fmtNum(current.plannedQuantity) }}</template>
            </a-descriptions-item>
            <a-descriptions-item label="签收时间">{{ fmtDateTime(current.signTime) }}</a-descriptions-item>
            <a-descriptions-item label="签收位置">{{ fmtCoord(current.signLat, current.signLng) }}</a-descriptions-item>
            <a-descriptions-item label="客户位置">{{ fmtCoord(current.customerLat, current.customerLng) }}</a-descriptions-item>
            <a-descriptions-item label="定位偏差">
              <a-tag :color="current.locationWarning === 1 ? 'red' : 'green'">
                {{ current.locationDeviation != null ? `${Math.round(Number(current.locationDeviation))} 米` : '-' }}
                {{ current.locationWarning === 1 ? '（超阈值，待复核）' : '' }}
              </a-tag>
              <span class="sub-text">阈值 {{ fmtNum(current.deviationThresh) }} 米</span>
            </a-descriptions-item>
            <a-descriptions-item label="审核状态">
              <a-tag :color="AUDIT_MAP[current.auditStatus ?? 0]?.color">
                {{ current.auditStatusText || AUDIT_MAP[current.auditStatus ?? 0]?.label }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="审核人">{{ current.auditByName || '-' }}</a-descriptions-item>
            <a-descriptions-item label="审核时间">{{ fmtDateTime(current.auditTime) }}</a-descriptions-item>
            <a-descriptions-item label="审核意见">{{ current.auditRemark || '-' }}</a-descriptions-item>
            <a-descriptions-item label="签收备注">{{ current.remark || '-' }}</a-descriptions-item>
            <a-descriptions-item label="代收货款">{{ fmtMoney(current.collectOnDelivery) }}</a-descriptions-item>
            <a-descriptions-item label="配送费">{{ fmtMoney(current.deliveryFee) }}</a-descriptions-item>
          </a-descriptions>

          <div class="media-row">
            <div class="media-block">
              <div class="media-title">签收照片（{{ photoList.length }}）</div>
              <div
                v-if="photoList.length"
                class="media-list"
              >
                <a-image
                  v-for="(url, i) in photoList"
                  :key="i"
                  :src="url"
                  :width="120"
                  :height="90"
                  style="object-fit: cover"
                />
              </div>
              <a-empty
                v-else
                description="无照片"
                :image="simpleImage"
              />
            </div>
            <div class="media-block">
              <div class="media-title">手写签名</div>
              <a-image
                v-if="current.signatureUrl"
                :src="current.signatureUrl"
                :width="180"
                :height="90"
                style="object-fit: contain"
              />
              <a-empty
                v-else
                description="无签名"
                :image="simpleImage"
              />
            </div>
          </div>

          <div
            v-if="current.auditStatus === 0"
            class="drawer-footer"
          >
            <a-space :size="12">
              <a-button
                type="primary"
                @click="openAudit(current, 1)"
              >
                审核通过
              </a-button>
              <a-button
                danger
                @click="openAudit(current, 2)"
              >
                驳回
              </a-button>
            </a-space>
          </div>
        </template>
      </a-drawer>

      <!-- ═══ 审核弹窗（驳回原因必填） ═══ -->
      <a-modal
        v-model:open="auditVisible"
        :title="auditForm.auditStatus === 1 ? '审核通过' : '审核驳回'"
        :width="460"
        :mask-closable="false"
        :confirm-loading="auditing"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleAuditSubmit"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item label="签收记录">
            <span>{{ auditTargets.length > 1 ? `已选 ${auditTargets.length} 条` : (auditTargets[0]?.taskNo || '-') }}</span>
          </a-form-item>
          <a-form-item label="审核结论">
            <a-tag :color="auditForm.auditStatus === 1 ? 'green' : 'red'">
              {{ auditForm.auditStatus === 1 ? '通过' : '驳回' }}
            </a-tag>
          </a-form-item>
          <a-form-item
            label="审核意见"
            :required="auditForm.auditStatus === 2"
          >
            <a-textarea
              v-model:value="auditForm.auditRemark"
              :rows="3"
              :maxlength="300"
              :placeholder="auditForm.auditStatus === 2 ? '请填写驳回原因（必填）' : '可选，填写审核备注'"
            />
          </a-form-item>
          <div class="modal-tip">
            {{ auditForm.auditStatus === 1
              ? '通过后任务置为「已完成」，并外发事件给配送结算 / 收款管理。'
              : '驳回后任务退回「配送中」，配送员可重新签收。' }}
          </div>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-sign-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Empty } from 'ant-design-vue'
import {
  ReloadOutlined, DownloadOutlined, SettingOutlined, DownOutlined,
  AuditOutlined, CheckCircleOutlined, CloseCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'
import { signApi, type DmsSignRecord } from '@/api/dms/sign'

defineOptions({ name: 'DmsSign' })

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

/** 签收类型（与后端 SignTypeEnum / 司机端同一套枚举，§3.6.8） */
const SIGN_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '正常签收', color: 'green' },
  2: { label: '部分签收', color: 'orange' },
  3: { label: '拒收', color: 'red' },
}
const SIGN_TYPE_OPTIONS = [
  { label: '正常签收', value: 1 }, { label: '部分签收', value: 2 }, { label: '拒收', value: 3 },
]

/** 审核状态（后端 SignAuditStatusEnum） */
const AUDIT_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已通过', color: 'green' },
  2: { label: '已驳回', color: 'red' },
}
const AUDIT_OPTIONS = [
  { label: '待审核', value: 0 }, { label: '已通过', value: 1 }, { label: '已驳回', value: 2 },
]
const YES_NO_OPTIONS = [{ label: '有', value: true }, { label: '无', value: false }]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<DmsSignRecord[]>([])
const selectedRows = ref<DmsSignRecord[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })

const stat = reactive({ total: 0, pending: 0, approveRate: 0, warningRate: 0, rejectRate: 0, partialCount: 0 })

const searchForm = reactive({
  keyword: '',
  riderId: undefined as number | undefined,
  signTypes: [] as number[],
  auditStatusList: [] as number[],
  onlyWarning: false,
  hasSignature: undefined as boolean | undefined,
  hasPhoto: undefined as boolean | undefined,
})
const signTimeRange = ref<[string, string] | undefined>()

// ═══ 列（★=默认显示；其余 defaultHidden） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 180, fixed: 'left' },
  { title: '任务编号', key: 'taskNo', type: 'slot', slotName: 'taskCell', width: 160, sortable: true },
  { title: '配送员', key: 'riderName', type: 'slot', slotName: 'riderCell', width: 140 },
  { title: '客户', key: 'customerName', width: 150 },
  { title: '签收类型', key: 'signType', type: 'slot', slotName: 'signTypeCell', width: 100 },
  { title: '签收数量', key: 'quantity', type: 'slot', slotName: 'qtyCell', width: 110 },
  { title: '签收时间', key: 'signTime', type: 'slot', slotName: 'signTimeCell', width: 150, sortable: true },
  { title: '定位偏差', key: 'locationDeviation', type: 'slot', slotName: 'deviationCell', width: 130, sortable: true },
  { title: '凭证', key: 'proof', type: 'slot', slotName: 'proofCell', width: 150 },
  { title: '审核状态', key: 'auditStatus', type: 'slot', slotName: 'auditCell', width: 100 },
  { title: '审核人', key: 'auditByName', width: 100, defaultHidden: true },
  { title: '审核时间', key: 'auditTime', type: 'slot', slotName: 'auditTimeCell', width: 150, defaultHidden: true, sortable: true },
  { title: '审核意见', key: 'auditRemark', width: 180, defaultHidden: true },
  { title: '签收备注', key: 'remark', width: 180, defaultHidden: true },
  { title: '签收位置', key: 'signPosition', type: 'slot', slotName: 'signPosCell', width: 170, defaultHidden: true },
  { title: '客户位置', key: 'customerPosition', type: 'slot', slotName: 'customerPosCell', width: 170, defaultHidden: true },
  { title: '偏差阈值(米)', key: 'deviationThresh', width: 110, defaultHidden: true },
  { title: '订单号', key: 'orderNo', width: 150, defaultHidden: true },
  { title: '配送车辆', key: 'vehicleName', width: 110, defaultHidden: true },
  { title: '客户电话', key: 'customerPhone', width: 130, defaultHidden: true },
]

// ═══ 配送员下拉（选择器，禁止手输 ID） ═══
const riderLoading = ref(false)
const riderOptions = ref<{ label: string; value: number }[]>([])
async function loadRiderOptions() {
  riderLoading.value = true
  try {
    const res: any = await request.get('/dms/rider/options')
    const list: any[] = Array.isArray(res) ? res : res?.records || res?.data || []
    riderOptions.value = list.map((r: any) => ({ label: r.realName || r.riderNo || `#${r.id}`, value: r.id }))
  } catch (error) {
    console.warn('[签收管理] 加载配送员下拉失败', error)
    riderOptions.value = []
  } finally {
    riderLoading.value = false
  }
}

// ═══ 查询参数 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword.trim()
  if (searchForm.riderId != null) params.riderId = searchForm.riderId
  if (searchForm.signTypes?.length) params.signTypes = searchForm.signTypes.join(',')
  if (searchForm.auditStatusList?.length) params.auditStatusList = searchForm.auditStatusList.join(',')
  if (searchForm.onlyWarning) params.onlyWarning = true
  if (searchForm.hasSignature != null) params.hasSignature = searchForm.hasSignature
  if (searchForm.hasPhoto != null) params.hasPhoto = searchForm.hasPhoto
  if (signTimeRange.value?.length === 2) {
    params.signTimeStart = signTimeRange.value[0]
    params.signTimeEnd = signTimeRange.value[1]
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
    const res: any = await signApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[签收管理] 加载台账失败', error)
    message.error(error?.response?.data?.message || '加载签收台账失败')
  } finally {
    loading.value = false
  }
}

async function fetchStat() {
  try {
    const res: any = await signApi.stat(buildParams())
    Object.assign(stat, {
      total: Number(res?.total) || 0,
      pending: Number(res?.pending) || 0,
      approveRate: Number(res?.approveRate) || 0,
      warningRate: Number(res?.warningRate) || 0,
      rejectRate: Number(res?.rejectRate) || 0,
      partialCount: Number(res?.partialCount) || 0,
    })
  } catch (error) {
    console.warn('[签收管理] 加载统计失败', error)
  }
}

function refreshAll() {
  fetchList()
  fetchStat()
}

function handleSearch() {
  pagination.current = 1
  refreshAll()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.riderId = undefined
  searchForm.signTypes = []
  searchForm.auditStatusList = []
  searchForm.onlyWarning = false
  searchForm.hasSignature = undefined
  searchForm.hasPhoto = undefined
  signTimeRange.value = undefined
  sortState.field = undefined
  sortState.order = undefined
  pagination.current = 1
  refreshAll()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  refreshAll()
}

function handleSortChange(key: string | null, order: string | null) {
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

function handleSelectionChange(rows: DmsSignRecord[]) {
  selectedRows.value = (rows || []).filter(r => !(r as any).__ghost)
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const current = ref<DmsSignRecord | null>(null)

const photoList = computed<string[]>(() => {
  const raw = current.value?.photoUrls
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : [String(raw)]
  } catch {
    return String(raw).split(',').map(s => s.trim()).filter(Boolean)
  }
})

async function openDetail(record: DmsSignRecord) {
  current.value = record
  detailVisible.value = true
  try {
    const detail: any = await signApi.detail(record.id)
    if (detail) current.value = detail
  } catch (error: any) {
    console.warn('[签收管理] 加载详情失败', error)
  }
}

// ═══ 审核 ═══
const auditVisible = ref(false)
const auditing = ref(false)
const auditTargets = ref<DmsSignRecord[]>([])
const auditForm = reactive({ auditStatus: 1, auditRemark: '' })

function openAudit(record: DmsSignRecord, status: number) {
  auditTargets.value = [record]
  auditForm.auditStatus = status
  auditForm.auditRemark = ''
  auditVisible.value = true
}

function openBatchAudit(key: string) {
  const targets = selectedRows.value.filter(r => r.auditStatus === 0)
  if (targets.length === 0) {
    message.warning('所选记录中没有「待审核」的签收')
    return
  }
  auditTargets.value = targets
  auditForm.auditStatus = key === 'approve' ? 1 : 2
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function handleAuditSubmit() {
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    message.warning('驳回必须填写驳回原因')
    return
  }
  auditing.value = true
  try {
    const ids = auditTargets.value.map(r => r.id)
    if (ids.length === 1) {
      await signApi.audit(ids[0], { auditStatus: auditForm.auditStatus, auditRemark: auditForm.auditRemark })
    } else {
      await signApi.batchAudit(ids, { auditStatus: auditForm.auditStatus, auditRemark: auditForm.auditRemark })
    }
    message.success(auditForm.auditStatus === 1 ? '审核通过' : '已驳回')
    auditVisible.value = false
    detailVisible.value = false
    selectedRows.value = []
    refreshAll()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '审核失败')
  } finally {
    auditing.value = false
  }
}

// ═══ 导出 ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const blob: any = await signApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `签收台账_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
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
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'riderId', label: '配送员', visible: true },
  { key: 'signTypes', label: '签收类型', visible: true },
  { key: 'auditStatusList', label: '审核状态', visible: true },
  { key: 'signTime', label: '签收时间', visible: true },
  { key: 'onlyWarning', label: '仅看超阈值', visible: true },
  { key: 'hasSignature', label: '手写签名', visible: true },
  { key: 'hasPhoto', label: '签收照片', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'batchAudit', label: '批量审核', enabled: true },
  { key: 'statBar', label: '统计条', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
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

// ═══ 展示辅助 ═══
function fmtDateTime(val?: string | null) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function fmtCoord(lat?: number | null, lng?: number | null) {
  if (lat == null || lng == null) return '-'
  return `${Number(lat).toFixed(6)}, ${Number(lng).toFixed(6)}`
}

function fmtNum(val?: number | null) {
  if (val == null) return '-'
  const n = Number(val)
  return Number.isNaN(n) ? '-' : String(n)
}

function fmtMoney(val?: number | null) {
  if (val == null) return '-'
  return `¥ ${Number(val).toFixed(2)}`
}

function fmtRate(val?: number | null) {
  if (val == null) return '-'
  return `${Number(val).toFixed(2)}%`
}

function handleError(error: Error) {
  console.error('[签收管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  refreshAll()
  loadRiderOptions()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.sel-tip { font-size: 12px; color: #1890ff; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.stat-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.stat-item { display: flex; align-items: baseline; gap: 6px; }
.stat-label { font-size: 12px; color: #8c8c8c; }
.stat-value { font-size: 16px; font-weight: 600; color: #262626; }
.stat-ok { color: #389e0d; }
.stat-warn { color: #d46b08; }
.stat-danger { color: #cf1322; }

.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.sub-text { font-size: 12px; color: #8c8c8c; margin-left: 4px; }
.dev-warn { color: #cf1322; font-weight: 600; }
.proof-cell { display: inline-flex; gap: 4px; }

.media-row { display: flex; gap: 48px; margin-top: 16px; }
.media-title { font-size: 13px; color: #595959; margin-bottom: 8px; }
.media-list { display: flex; flex-wrap: wrap; gap: 8px; }
.drawer-footer { margin-top: 20px; text-align: right; }
.modal-tip { font-size: 12px; color: #fa8c16; line-height: 1.6; padding-left: 4px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
