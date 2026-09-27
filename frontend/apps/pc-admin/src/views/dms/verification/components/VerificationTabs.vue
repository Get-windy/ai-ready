<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
        :tabs="tabs"
        :active-tab="activeTab"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏右侧：功能按钮（随 Tab 变化，受页面配置开关控制） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <template v-if="activeTab === 'kyc'">
              <a-button
                v-if="fnEnabled('submitKyc')"
                size="small"
                @click="openKycSubmit()"
              >
                <SafetyCertificateOutlined /> 提交认证
              </a-button>
              <a-button
                v-if="fnEnabled('scanExpiry')"
                size="small"
                @click="handleScanExpiry"
              >
                <ClockCircleOutlined /> 到期扫描
              </a-button>
            </template>
            <template v-else-if="activeTab === 'alert'">
              <a-button
                v-if="fnEnabled('scan')"
                size="small"
                @click="handleScan"
              >
                <ThunderboltOutlined /> 批量核验
              </a-button>
            </template>
            <template v-else-if="activeTab === 'binding'">
              <a-button
                v-if="fnEnabled('bind')"
                size="small"
                @click="openBindModal"
              >
                <LinkOutlined /> 出车登记
              </a-button>
              <a-button
                v-if="fnEnabled('handover')"
                size="small"
                @click="openHandover()"
              >
                <SwapRightOutlined /> 交车
              </a-button>
            </template>
            <template v-else-if="activeTab === 'inspection'">
              <a-button
                v-if="fnEnabled('spotCheck')"
                size="small"
                @click="openSpotCheck"
              >
                <PlusOutlined /> 新增抽检/定检
              </a-button>
            </template>
            <template v-else-if="activeTab === 'energy'">
              <a-button
                v-if="fnEnabled('addEnergy')"
                size="small"
                @click="openEnergyModal()"
              >
                <PlusOutlined /> 新增补能
              </a-button>
            </template>
            <template v-else-if="activeTab === 'energyCard'">
              <a-button
                v-if="fnEnabled('addCard')"
                size="small"
                @click="openCardModal()"
              >
                <PlusOutlined /> 新增补能卡
              </a-button>
            </template>
            <a-button
              v-if="fnEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="fnEnabled('printF8')"
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="fnEnabled('export')"
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

        <!-- ═══ 查询条件（随 Tab 变化 + 页面配置显隐） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <template
                v-for="field in visibleSearchFields"
                :key="field.key"
              >
                <div
                  v-if="field.type === 'select'"
                  class="search-field-item"
                >
                  <span class="search-label">{{ field.label }}</span>
                  <a-select
                    v-model:value="searchParams[field.key]"
                    size="small"
                    style="width: 150px"
                    :options="field.options"
                    placeholder="全部"
                    allow-clear
                  />
                </div>
                <div
                  v-else-if="field.type === 'dateRange'"
                  class="search-field-item"
                >
                  <span class="search-label">{{ field.label }}</span>
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 230px"
                    @change="handleDateChange"
                  />
                </div>
                <div
                  v-else-if="field.type === 'checkbox'"
                  class="search-field-item"
                >
                  <a-checkbox
                    v-model:checked="searchParams[field.key]"
                    @change="handleSearch"
                  >
                    {{ field.label }}
                  </a-checkbox>
                </div>
                <div
                  v-else
                  class="search-field-item"
                >
                  <span class="search-label">{{ field.label }}</span>
                  <a-input
                    v-model:value="searchParams[field.key]"
                    :placeholder="field.placeholder || field.label"
                    size="small"
                    style="width: 150px"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                <SearchOutlined /> 查询
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（每 Tab 独立列配置 storage-key）；能耗报表为可视化面板 ═══ -->
        <template #table>
          <!-- 能耗报表：汇总卡 + 油电对比 + 图表 + 分组表 -->
          <div
            v-if="activeTab === 'energyStats'"
            class="energy-stats"
          >
            <a-spin :spinning="statsLoading">
              <a-row :gutter="12">
                <a-col
                  v-for="card in statsCards"
                  :key="card.label"
                  :span="6"
                >
                  <div class="stat-card">
                    <div class="stat-label">
                      {{ card.label }}
                    </div>
                    <div class="stat-main">
                      <span class="stat-value">{{ card.value }}</span>
                      <span class="stat-suffix">{{ card.suffix }}</span>
                    </div>
                  </div>
                </a-col>
              </a-row>

              <a-row :gutter="12">
                <a-col :span="12">
                  <div class="panel">
                    <div class="panel-title">
                      油电对比（每公里成本）
                    </div>
                    <ARReportChart
                      :option="compareOption"
                      :height="260"
                      :loading="statsLoading"
                    />
                    <table class="mini-table">
                      <thead>
                        <tr>
                          <th>分组</th>
                          <th>记录数</th>
                          <th>金额(元)</th>
                          <th>里程(km)</th>
                          <th>每公里成本(元)</th>
                          <th>CO₂(kg)</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr
                          v-for="g in compareRows"
                          :key="g.key"
                        >
                          <td>{{ g.label }}</td>
                          <td>{{ g.logCount }}</td>
                          <td>{{ fmtAmount(g.amount) }}</td>
                          <td>{{ g.mileage || '-' }}</td>
                          <td>{{ g.unitCost == null ? '-' : fmtAmount(g.unitCost) }}</td>
                          <td>{{ fmtAmount(g.co2Kg) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </a-col>
                <a-col :span="12">
                  <div class="panel">
                    <div class="panel-title">
                      按能源类型（金额占比）
                    </div>
                    <ARReportChart
                      :option="typePieOption"
                      :height="260"
                      :loading="statsLoading"
                    />
                  </div>
                </a-col>
              </a-row>

              <div class="panel">
                <div class="panel-title">
                  按主体（车 / 人）
                </div>
                <a-table
                  :data-source="statsData?.bySubject || []"
                  :columns="statsColumns"
                  :pagination="false"
                  row-key="key"
                  size="small"
                  bordered
                />
              </div>
            </a-spin>
          </div>

          <BillTableList
            v-else
            :key="activeTab"
            :columns="currentColumns"
            :data-source="tableData"
            :loading="loading"
            :storage-key="`dms-verification-columns-${tabCode}`"
            :global-config-key="`dms-verification-columns-${tabCode}`"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="activeTab === 'alert'"
            :min-empty-rows="20"
            row-key="rowKey"
            @page-change="handlePageChange"
            @selection-change="handleSelectionChange"
          >
            <!-- 通用操作列 -->
            <template #actionCell="{ record }">
              <template v-if="record && !record.__ghost">
                <!-- 实名认证 -->
                <template v-if="activeTab === 'kyc'">
                  <a-button
                    type="link"
                    size="small"
                    @click="record.verifyStatus === 2 ? openKycDetail(record) : openKycSubmit(record)"
                  >
                    {{ record.verifyStatus === 2 ? '查看' : '编辑' }}
                  </a-button>
                  <a-button
                    v-if="record.verifyStatus === 1"
                    type="link"
                    size="small"
                    @click="openKycAudit(record)"
                  >
                    审核
                  </a-button>
                </template>
                <!-- 人车绑定 -->
                <template v-else-if="activeTab === 'binding'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openBindingDetail(record)"
                  >
                    详情
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="openHandover(record)"
                  >
                    交车
                  </a-button>
                </template>
                <!-- 预警 -->
                <template v-else-if="activeTab === 'alert'">
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.handleStatus !== 0"
                    @click="openAlertHandle(record)"
                  >
                    处理
                  </a-button>
                  <a-button
                    v-if="record.bindingId"
                    type="link"
                    size="small"
                    @click="openBindingDetailById(record.bindingId)"
                  >
                    绑定详情
                  </a-button>
                </template>
                <!-- 车辆补能 -->
                <template v-else-if="activeTab === 'energy'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openEnergyModal(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="removeEnergy(record)"
                  >
                    删除
                  </a-button>
                </template>
                <!-- 补能卡：编辑 / 启停 / 删除 -->
                <template v-else-if="activeTab === 'energyCard'">
                  <a-button
                    type="link"
                    size="small"
                    @click="openCardModal(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="toggleCardStatus(record)"
                  >
                    {{ record.status === 1 ? '停用' : '启用' }}
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="removeCard(record)"
                  >
                    删除
                  </a-button>
                </template>
                <!-- 准入核验：跳到实名认证台账 -->
                <template v-else-if="activeTab === 'onboarding'">
                  <a-button
                    type="link"
                    size="small"
                    @click="gotoKycFromOnboarding(record)"
                  >
                    查看台账
                  </a-button>
                </template>
                <!-- 证照核验：跳到该持证人的实名认证台账 -->
                <template v-else-if="activeTab === 'cert'">
                  <a-button
                    v-if="record.verificationId"
                    type="link"
                    size="small"
                    @click="openKycDetail({ id: record.verificationId })"
                  >
                    查看台账
                  </a-button>
                </template>
                <!-- 车辆巡检 -->
                <template v-else>
                  <a-button
                    type="link"
                    size="small"
                    @click="openInspectionDetail(record)"
                  >
                    详情
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openInspectionReview(record)"
                  >
                    审核
                  </a-button>
                </template>
              </template>
            </template>

            <!-- 认证状态 -->
            <template #verifyStatusCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="VERIFY_STATUS_MAP[record.verifyStatus]?.color || 'default'"
              >
                {{ record.verifyStatusText || VERIFY_STATUS_MAP[record.verifyStatus]?.text }}
              </a-tag>
            </template>

            <!-- 接单资质 -->
            <template #eligibleCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-tooltip
                  v-if="!record.eligible && record.ineligibleReason"
                  :title="record.ineligibleReason"
                  placement="bottom"
                >
                  <a-tag color="red">
                    不具备
                  </a-tag>
                </a-tooltip>
                <a-tag
                  v-else
                  :color="record.eligible ? 'green' : 'red'"
                >
                  {{ record.eligible ? '具备' : '不具备' }}
                </a-tag>
              </template>
            </template>

            <!-- 到期预警 -->
            <template #expiringCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-tag
                  v-if="Number(record.expiringCertCount) > 0"
                  color="orange"
                >
                  {{ record.expiringCertCount }} 项到期
                </a-tag>
                <span v-else>-</span>
              </template>
            </template>

            <!-- 绑定状态 -->
            <template #statusCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="BINDING_STATUS_MAP[record.status]?.color || 'default'"
              >
                {{ BINDING_STATUS_MAP[record.status]?.text || record.status }}
              </a-tag>
            </template>

            <!-- 预警类型 / 级别 / 处理状态 -->
            <template #alertTypeCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="ALERT_TYPE_MAP[record.alertType]?.color || 'default'"
              >
                {{ ALERT_TYPE_MAP[record.alertType]?.text || record.alertType }}
              </a-tag>
            </template>
            <template #alertLevelCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="ALERT_LEVEL_MAP[record.alertLevel]?.color || 'default'"
              >
                {{ ALERT_LEVEL_MAP[record.alertLevel]?.text || record.alertLevel }}
              </a-tag>
            </template>
            <template #handleStatusCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="HANDLE_STATUS_MAP[record.handleStatus]?.color || 'default'"
              >
                {{ HANDLE_STATUS_MAP[record.handleStatus]?.text || record.handleStatus }}
              </a-tag>
            </template>

            <!-- 巡检类型 / 结果 -->
            <template #inspectionTypeCell="{ record }">
              <span v-if="record && !record.__ghost">{{ INSPECTION_TYPE_MAP[record.inspectionType]?.text || record.inspectionType }}</span>
            </template>
            <template #resultCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.result === 1 ? 'green' : 'red'"
              >
                {{ record.result === 1 ? '通过' : '不通过' }}
              </a-tag>
            </template>

            <!-- 补能卡：绑定主体 -->
            <template #cardSubjectCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-tag :color="record.subjectType === 'RIDER' ? 'purple' : 'blue'">
                  {{ record.subjectType === 'RIDER' ? '骑手' : '车辆' }}
                </a-tag>
                {{ record.subjectName || '-' }}
              </template>
            </template>
            <!-- 补能卡：额度/已用 -->
            <template #cardQuotaCell="{ record }">
              <template v-if="record && !record.__ghost">
                <template v-if="record.quota != null">
                  {{ record.usedQuota ?? 0 }} / {{ record.quota }}
                  <a-tag
                    v-if="Number(record.quotaUsagePercent) > 100"
                    color="red"
                    style="margin-left:4px"
                  >
                    超限
                  </a-tag>
                </template>
                <span v-else>不限量</span>
              </template>
            </template>
            <!-- 补能卡：到期 -->
            <template #cardExpireCell="{ record }">
              <template v-if="record && !record.__ghost && record.expireDate">
                <a-tag
                  v-if="record.expired"
                  color="red"
                >
                  已过期
                </a-tag>
                <a-tag
                  v-else-if="record.daysToExpire != null && Number(record.daysToExpire) <= 30"
                  color="orange"
                >
                  剩 {{ record.daysToExpire }} 天
                </a-tag>
                <span v-else>{{ record.daysToExpire }} 天</span>
              </template>
              <span v-else>-</span>
            </template>
            <!-- 补能卡：状态 -->
            <template #cardStatusCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.status === 1 ? 'green' : 'default'"
              >
                {{ record.statusText || (record.status === 1 ? '启用' : '停用') }}
              </a-tag>
            </template>
            <!-- 准入核验：可否接单 / 最近检查 / 可否出车 -->
            <template #onboardEligibleCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.eligible ? 'green' : 'red'"
              >
                {{ record.eligible ? '可接单' : '不可接单' }}
              </a-tag>
            </template>
            <template #onboardInspectionCell="{ record }">
              <template v-if="record && !record.__ghost && record.lastInspectionResult != null">
                <a-tag :color="record.lastInspectionResult === 1 ? 'green' : 'red'">
                  {{ record.lastInspectionResult === 1 ? '通过' : '不通过' }}
                </a-tag>
                <span class="muted">{{ record.lastInspectionTime }}</span>
              </template>
              <span v-else>-</span>
            </template>
            <template #onboardCanDriveCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-tooltip
                  v-if="!record.canDrive && record.blockReasons"
                  :title="record.blockReasons"
                  placement="bottom"
                >
                  <a-tag color="red">
                    不可出车
                  </a-tag>
                </a-tooltip>
                <a-tag
                  v-else
                  :color="record.canDrive ? 'green' : 'red'"
                >
                  {{ record.canDrive ? '可出车' : '不可出车' }}
                </a-tag>
              </template>
            </template>

            <!-- 补能：主体类型 -->
            <template #subjectTypeCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.subjectType === 'RIDER' ? 'purple' : 'blue'"
              >
                {{ record.subjectType === 'RIDER' ? '骑手两轮车' : '四轮车' }}
              </a-tag>
            </template>
            <!-- 补能：异常 -->
            <template #abnormalCell="{ record }">
              <template v-if="record && !record.__ghost">
                <a-tooltip
                  v-if="Number(record.abnormalFlag) === 1"
                  :title="record.abnormalReason || '补能异常'"
                  placement="bottom"
                >
                  <a-tag color="red">
                    异常
                  </a-tag>
                </a-tooltip>
                <a-tag
                  v-else
                  color="green"
                >
                  正常
                </a-tag>
              </template>
            </template>
            <!-- 证照：剩余天数（已过期/即将到期/正常三色） -->
            <template #certExpireCell="{ record }">
              <template v-if="record && !record.__ghost && record.daysToExpire !== null && record.daysToExpire !== undefined">
                <a-tag
                  v-if="Number(record.daysToExpire) < 0"
                  color="red"
                >
                  已过期 {{ Math.abs(Number(record.daysToExpire)) }} 天
                </a-tag>
                <a-tag
                  v-else-if="record.expiring"
                  color="orange"
                >
                  剩 {{ record.daysToExpire }} 天
                </a-tag>
                <span v-else>{{ record.daysToExpire }} 天</span>
              </template>
              <span v-else>-</span>
            </template>
            <!-- 证照：状态 -->
            <template #certStatusCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="CERT_STATUS_MAP[record.verifyStatus]?.color || 'default'"
              >
                {{ record.verifyStatusText || CERT_STATUS_MAP[record.verifyStatus]?.text }}
              </a-tag>
            </template>

            <!-- 车辆检查项：0-正常 1-异常（每列独立插槽，BillDetailTable 插槽不传 column） -->
            <template #exteriorCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.exteriorStatus === 0 ? 'green' : 'red'"
              >
                {{ record.exteriorStatus === 0 ? '正常' : '异常' }}
              </a-tag>
            </template>
            <template #tireCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.tireStatus === 0 ? 'green' : 'red'"
              >
                {{ record.tireStatus === 0 ? '正常' : '异常' }}
              </a-tag>
            </template>
            <template #lightCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.lightStatus === 0 ? 'green' : 'red'"
              >
                {{ record.lightStatus === 0 ? '正常' : '异常' }}
              </a-tag>
            </template>
            <template #brakeCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.brakeStatus === 0 ? 'green' : 'red'"
              >
                {{ record.brakeStatus === 0 ? '正常' : '异常' }}
              </a-tag>
            </template>
            <template #cleanlinessCell="{ record }">
              <a-tag
                v-if="record && !record.__ghost"
                :color="record.cleanlinessStatus === 0 ? 'green' : 'red'"
              >
                {{ record.cleanlinessStatus === 0 ? '正常' : '异常' }}
              </a-tag>
            </template>

            <template #extinguisherCell="{ record }">
              <span v-if="record && !record.__ghost">{{ record.fireExtinguisher === 0 ? '正常' : '缺失/过期' }}</span>
            </template>
            <template #triangleCell="{ record }">
              <span v-if="record && !record.__ghost">{{ record.warningTriangle === 0 ? '有' : '缺失' }}</span>
            </template>
          </BillTableList>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :default-query-fields-config="defaultQueryFields"
      :default-function-buttons-config="defaultFunctionButtons"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 实名认证提交/编辑弹窗 ═══ -->
    <a-modal
      v-model:open="kycVisible"
      :title="kycForm.id ? '实名认证 - 编辑' : '实名认证 - 提交'"
      :width="880"
      :confirm-loading="kycSaving"
      :mask-closable="false"
      @ok="submitKyc"
    >
      <a-form
        ref="kycFormRef"
        :model="kycForm"
        :rules="kycRules"
        :label-col="{ span: 7 }"
        :wrapper-col="{ span: 16 }"
        size="small"
      >
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item
              label="配送员"
              name="riderId"
            >
              <a-select
                v-model:value="kycForm.riderId"
                show-search
                option-filter-prop="label"
                placeholder="请选择配送员"
                :options="riderOptions"
                :disabled="!!kycForm.id"
                @change="onRiderChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="身份类型">
              <a-input
                :value="kycRiderTypeText"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="所属渠道">
              <a-input
                :value="kycForm.channelName || '-'"
                disabled
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="证件姓名">
              <a-input
                v-model:value="kycForm.realName"
                placeholder="与身份证一致"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="身份证号">
              <a-input
                v-model:value="kycForm.idCardNo"
                placeholder="提交后仅保存脱敏值（前 3 后 4）"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="实名核验有效期">
              <a-date-picker
                v-model:value="kycForm.endorseExpireDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                placeholder="选填"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="证件人像面">
              <a-input
                v-model:value="kycForm.idCardFrontUrl"
                placeholder="图片地址或点击右侧上传"
              >
                <template #suffix>
                  <a @click.prevent="pickKycImage('front')">上传</a>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="证件国徽面">
              <a-input
                v-model:value="kycForm.idCardBackUrl"
                placeholder="图片地址或点击右侧上传"
              >
                <template #suffix>
                  <a @click.prevent="pickKycImage('back')">上传</a>
                </template>
              </a-input>
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider style="margin: 4px 0 12px">
          背景审查 / 外部平台背书
        </a-divider>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="背书/审查机构">
              <a-input
                v-model:value="kycForm.endorseOrg"
                placeholder="自有员工填审查机构；外部平台填渠道方"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="背书/审查结论">
              <a-select
                v-model:value="kycForm.endorseResult"
                allow-clear
                placeholder="请选择"
                :options="[{ value: 1, label: '通过' }, { value: 0, label: '未通过' }]"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider style="margin: 4px 0 12px">
          证照明细（驾驶证 / 行驶证 / 健康证 / 从业资格证）
        </a-divider>
        <a-table
          :data-source="kycForm.certificates"
          :columns="certColumns"
          :pagination="false"
          row-key="_uid"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'certType'">
              <a-select
                v-model:value="record.certType"
                size="small"
                style="width: 100%"
                :options="CERT_TYPE_OPTIONS"
              />
            </template>
            <template v-else-if="column.dataIndex === 'certNo'">
              <a-input
                v-model:value="record.certNo"
                size="small"
                placeholder="证照编号"
              />
            </template>
            <template v-else-if="column.dataIndex === 'issueDate'">
              <a-date-picker
                v-model:value="record.issueDate"
                size="small"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </template>
            <template v-else-if="column.dataIndex === 'expireDate'">
              <a-date-picker
                v-model:value="record.expireDate"
                size="small"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </template>
            <template v-else-if="column.dataIndex === 'certUrl'">
              <a-input
                v-model:value="record.certUrl"
                size="small"
                placeholder="影像地址"
              />
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-button
                type="link"
                size="small"
                danger
                @click="removeCertRow(index)"
              >
                删除
              </a-button>
            </template>
          </template>
        </a-table>
        <a-button
          type="dashed"
          size="small"
          block
          style="margin-top: 8px"
          @click="addCertRow"
        >
          <PlusOutlined /> 添加证照
        </a-button>

        <a-form-item
          label="备注"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 20 }"
          style="margin-top: 12px"
        >
          <a-textarea
            v-model:value="kycForm.remark"
            :rows="2"
            placeholder="备注"
          />
        </a-form-item>
        <a-form-item
          label="提交审核"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 20 }"
        >
          <a-checkbox v-model:checked="kycForm.submitAudit">
            直接提交审核（不勾选则仅存草稿）
          </a-checkbox>
        </a-form-item>
      </a-form>
      <input
        ref="kycFileInputRef"
        type="file"
        accept="image/*"
        style="display: none"
        @change="onKycImageChange"
      >
    </a-modal>

    <!-- ═══ 实名认证审核弹窗 ═══ -->
    <KycAuditModal
      v-model:open="auditVisible"
      :saving="auditSaving"
      :record="auditRecord"
      @submit="handleKycAudit"
    />
    <!-- ═══ 交车弹窗（交车登记 + 收车后检查，业界 check-in 单证） ═══ -->
    <HandoverModal
      v-model:open="handoverVisible"
      :record="handoverInitRecord"
      @saved="fetchData"
    />
    <!-- ═══ 出车登记弹窗（绑定信息 + 出车前检查，两步向导） ═══ -->
    <BindModal
      v-model:open="bindVisible"
      :rider-options="riderOptions"
      :vehicle-raw-list="vehicleRawList"
      @saved="onBindSaved"
    />
    <!-- ═══ 抽检/定检弹窗（车管员发起，不关联人车绑定） ═══ -->
    <SpotCheckModal
      v-model:open="spotCheckVisible"
      :saving="spotCheckSaving"
      :vehicle-options="vehicleOptions"
      :rider-options="riderOptions"
      :vehicle-raw-list="vehicleRawList"
      @submit="handleSpotCheckSubmit"
    />
    <!-- ═══ 补能卡 / 套餐弹窗（一卡一车一人） ═══ -->
    <CardModal
      v-model:open="cardVisible"
      :record="cardInitRecord"
      :vehicle-options="vehicleOptions"
      :rider-options="riderOptions"
      @saved="onCardSaved"
    />
    <!-- ═══ 补能录入弹窗（车/人二选一） ═══ -->
    <EnergyModal
      v-model:open="energyVisible"
      :record="energyInitRecord"
      :vehicle-options="vehicleOptions"
      :rider-options="riderOptions"
      :energy-card-options="energyCardOptions"
      :energy-card-list="energyCardList"
      @saved="onEnergySaved"
    />
    <!-- ═══ 巡检详情抽屉 ═══ -->
    <InspectionDetailDrawer
      :open="inspectionDetailVisible"
      :loading="inspectionDetailLoading"
      :data="inspectionDetailData"
      @update:open="inspectionDetailVisible = $event"
    />

    <!-- ═══ 预警处理弹窗 ═══ -->
    <AlertHandleModal
      v-model:open="alertVisible"
      :saving="alertSaving"
      :record="alertRecord"
      @submit="handleAlertSubmit"
    />

    <!-- ═══ 巡检审核弹窗 ═══ -->
    <InspectionReviewModal
      v-model:open="inspectionVisible"
      :saving="inspectionSaving"
      :record="inspectionRecord"
      @submit="handleInspectionReview"
    />
    <!-- ═══ 实名认证详情抽屉（已通过台账只读） ═══ -->
    <KycDetailDrawer
      :open="kycDetailVisible"
      :loading="kycDetailLoading"
      :data="kycDetailData"
      @update:open="kycDetailVisible = $event"
    />

    <!-- ═══ 绑定详情抽屉 ═══ -->
    <BindingDetailDrawer
      :open="detailVisible"
      :loading="detailLoading"
      :data="detailData"
      @update:open="detailVisible = $event"
    />

    <!-- ═══ 打印（F8）统一打印弹窗 ═══ -->
    <PrintDialog
      ref="printDialogRef"
      page-code="dms:verification"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs, { type Dayjs } from 'dayjs'
import {
  ReloadOutlined, SearchOutlined, SettingOutlined, PrinterOutlined, DownloadOutlined,
  PlusOutlined, SafetyCertificateOutlined, ClockCircleOutlined, ThunderboltOutlined, LinkOutlined, SwapRightOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import EnergyModal from './dialogs/EnergyModal.vue'
import CardModal from './dialogs/CardModal.vue'
import BindModal from './dialogs/BindModal.vue'
import HandoverModal from './dialogs/HandoverModal.vue'
import SpotCheckModal from './dialogs/SpotCheckModal.vue'
import KycAuditModal from './dialogs/KycAuditModal.vue'
import InspectionReviewModal from './dialogs/InspectionReviewModal.vue'
import AlertHandleModal from './dialogs/AlertHandleModal.vue'
import BindingDetailDrawer from './dialogs/BindingDetailDrawer.vue'
import KycDetailDrawer from './dialogs/KycDetailDrawer.vue'
import InspectionDetailDrawer from './dialogs/InspectionDetailDrawer.vue'
import VehicleCheckForm from './VehicleCheckForm.vue'
import type { VehicleCheckData } from './VehicleCheckForm.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import request from '@/utils/request'
import { verificationApi } from '@/api/dms/verification'
import { vehicleApi, energyLogApi, energyCardApi } from '@/api/dms/vehicle'

defineOptions({ name: 'DmsVerification' })

function handleError(err: any) {
  console.warn('[实名认证]', err)
}

function formatDateTime(value?: string): string {
  if (!value) return '-'
  const d = dayjs(value)
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

// ═══ Tab ═══
/**
 * 同一套组件按 `scope` 服务两个菜单（不复制代码）：
 *  · usage = 配送 → 人车管理 → 用车管理（一次用车的全过程：登记 → 巡检 → 补能 → 预警）
 *  · kyc   = 配送 → 人车管理 → 人员核验（人的合规：实名认证 + 证照核验）
 */
const props = withDefaults(defineProps<{ scope?: 'usage' | 'kyc' }>(), { scope: 'usage' })

type TabKey = 'binding' | 'inspection' | 'energy' | 'energyCard' | 'energyStats' | 'alert' | 'kyc' | 'cert' | 'onboarding'

const ALL_TABS: Record<TabKey, { key: TabKey; label: string }> = {
  binding: { key: 'binding', label: '用车登记' },
  inspection: { key: 'inspection', label: '车辆巡检' },
  energy: { key: 'energy', label: '车辆补能' },
  energyCard: { key: 'energyCard', label: '补能卡' },
  energyStats: { key: 'energyStats', label: '能耗报表' },
  alert: { key: 'alert', label: '核验预警' },
  kyc: { key: 'kyc', label: '实名认证' },
  cert: { key: 'cert', label: '证照核验' },
  onboarding: { key: 'onboarding', label: '准入核验' },
}

const tabs = computed(() => (props.scope === 'kyc'
  ? [ALL_TABS.kyc, ALL_TABS.cert, ALL_TABS.onboarding]
  : [ALL_TABS.binding, ALL_TABS.inspection, ALL_TABS.energy, ALL_TABS.energyCard, ALL_TABS.energyStats, ALL_TABS.alert]))

const activeTab = ref<TabKey>(props.scope === 'kyc' ? 'kyc' : 'binding')

/** 默认 Tab 随 scope 切换（两个菜单复用同一组件实例时也正确） */
watch(() => props.scope, (val) => {
  activeTab.value = val === 'kyc' ? 'kyc' : 'binding'
})

/**
 * Tab → 台账编码（导出 / 列配置持久化共用）。
 * 预警统一用 warn：平台安全切面 SecurityAspect 会拦截含 "alert" 的字符串参数（400 非法字符），
 * 而「全局列配置」会以 `col-config/dms-verification-columns-<code>` 打到后端。
 */
const TAB_CODE: Record<string, string> = {
  kyc: 'kyc', binding: 'binding', alert: 'warn', inspection: 'inspection',
}
const tabCode = computed(() => TAB_CODE[activeTab.value] || activeTab.value)


import {
  VERIFY_STATUS_MAP,
  BINDING_STATUS_MAP,
  ALERT_TYPE_MAP,
  ALERT_LEVEL_MAP,
  HANDLE_STATUS_MAP,
  INSPECTION_TYPE_MAP,
  CERT_STATUS_MAP,
  CARD_TYPE_OPTIONS,
  ENERGY_TYPE_OPTIONS,
  PAY_MODE_OPTIONS,
  CERT_TYPE_OPTIONS,
  RIDER_TYPE_MAP,
  KYC_SEARCH_FIELDS,
  BINDING_SEARCH_FIELDS,
  ALERT_SEARCH_FIELDS,
  INSPECTION_SEARCH_FIELDS,
  ENERGY_SEARCH_FIELDS,
  CERT_SEARCH_FIELDS,
  ENERGY_CARD_SEARCH_FIELDS,
  ENERGY_STATS_SEARCH_FIELDS,
  ONBOARDING_SEARCH_FIELDS,
  KYC_COLUMNS,
  BINDING_COLUMNS,
  ALERT_COLUMNS,
  INSPECTION_COLUMNS,
  ENERGY_COLUMNS,
  CERT_COLUMNS,
  ENERGY_CARD_COLUMNS,
  ONBOARDING_COLUMNS,
  INSPECTION_ITEM_LABELS,
  emptyCheck,
} from '../config/verification-config'
import type { SearchFieldDef, ColumnDef } from '../config/verification-config'
const currentSearchFields = computed<SearchFieldDef[]>(() => {
  switch (activeTab.value) {
    case 'binding': return BINDING_SEARCH_FIELDS
    case 'alert': return ALERT_SEARCH_FIELDS
    case 'inspection': return INSPECTION_SEARCH_FIELDS
    case 'energy': return ENERGY_SEARCH_FIELDS
    case 'energyCard': return ENERGY_CARD_SEARCH_FIELDS
    case 'energyStats': return ENERGY_STATS_SEARCH_FIELDS
    case 'cert': return CERT_SEARCH_FIELDS
    case 'onboarding': return ONBOARDING_SEARCH_FIELDS
    default: return KYC_SEARCH_FIELDS
  }
})

// ═══ 列定义 ═══
const currentColumns = computed<ColumnDef[]>(() => {
  switch (activeTab.value) {
    case 'binding': return BINDING_COLUMNS
    case 'alert': return ALERT_COLUMNS
    case 'inspection': return INSPECTION_COLUMNS
    case 'energy': return ENERGY_COLUMNS
    case 'energyCard': return ENERGY_CARD_COLUMNS
    case 'cert': return CERT_COLUMNS
    case 'onboarding': return ONBOARDING_COLUMNS
    // 能耗报表为可视化面板，不使用表格列
    case 'energyStats': return []
    default: return KYC_COLUMNS
  }
})

// ═══ 页面配置（查询条件 + 功能按钮，逐 Tab 独立） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const FUNCTION_BUTTONS: Record<string, FunctionButtonSetting[]> = {
  kyc: [
    { key: 'submitKyc', label: '提交认证', enabled: true },
    { key: 'scanExpiry', label: '到期扫描', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  binding: [
    { key: 'bind', label: '出车登记', enabled: true },
    { key: 'handover', label: '交车', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  alert: [
    { key: 'scan', label: '批量核验', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  inspection: [
    { key: 'spotCheck', label: '新增抽检/定检', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  energy: [
    { key: 'addEnergy', label: '新增补能', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'export', label: '导出', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  energyCard: [
    { key: 'addCard', label: '新增补能卡', enabled: true },
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  energyStats: [
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'export', label: '导出汇总', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  onboarding: [
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    { key: 'config', label: '页面配置', enabled: true },
  ],
  cert: [
    { key: 'refresh', label: '刷新', enabled: true },
    { key: 'printF8', label: '打印(F8)', enabled: true },
    // 证照核验不做独立导出（避免与实名认证台账两个导出入口口径不一致），需要时可在页面配置里开启
    { key: 'export', label: '导出', enabled: false },
    { key: 'config', label: '页面配置', enabled: true },
  ],
}

const defaultQueryFields = computed<QueryFieldSetting[]>(() =>
  currentSearchFields.value.map(f => ({ key: f.key, label: f.label, visible: true })))
const defaultFunctionButtons = computed<FunctionButtonSetting[]>(() => FUNCTION_BUTTONS[activeTab.value] || [])

const queryFieldsConfig = ref<QueryFieldSetting[]>([])
const functionButtonConfig = ref<FunctionButtonSetting[]>([])
const showPageConfig = ref(false)
const pageConfigStorageKey = computed(() => `dms-verification-page-config-${tabCode.value}`)

const visibleSearchFields = computed(() => {
  const config = queryFieldsConfig.value
  return currentSearchFields.value.filter(f => {
    const c = config.find(cf => cf.key === f.key)
    return c ? c.visible : true
  })
})

function fnEnabled(key: string): boolean {
  if (key === 'config') return true
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

function syncTabConfig() {
  queryFieldsConfig.value = defaultQueryFields.value.map(f => ({ ...f }))
  functionButtonConfig.value = defaultFunctionButtons.value.map(f => ({ ...f }))
}

function handlePageConfigChange(config: any) {
  queryFieldsConfig.value = config.queryFields || defaultQueryFields.value
  functionButtonConfig.value = config.functionButtons || defaultFunctionButtons.value
}

// ═══ 列表 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const selectedRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

const searchParams = reactive<Record<string, any>>({
  riderName: undefined, riderPhone: undefined, riderType: undefined, verifyStatus: undefined,
  eligible: undefined, expiring: false,
  plateNo: undefined, status: undefined,
  alertType: undefined, alertLevel: undefined, handleStatus: undefined,
  inspectionType: undefined, result: undefined,
  subjectType: undefined, energyType: undefined, payMode: undefined,
  keyword: undefined, abnormalOnly: undefined,
  certType: undefined,
  cardType: undefined, onlyBlocked: undefined,
  startDate: undefined, endDate: undefined,
})
const dateRange = ref<[Dayjs, Dayjs] | null>(null)

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.startDate = dates[0]?.format('YYYY-MM-DD')
    searchParams.endDate = dates[1]?.format('YYYY-MM-DD')
  } else {
    searchParams.startDate = undefined
    searchParams.endDate = undefined
  }
  handleSearch()
}

function buildParams(): Record<string, any> {
  const p: Record<string, any> = { page: pagination.current, size: pagination.pageSize }
  currentSearchFields.value
    .map(f => f.key)
    .filter(k => k !== 'date')
    .forEach(k => {
      const v = searchParams[k]
      if (v !== undefined && v !== null && v !== '') p[k] = v
    })
  if (searchParams.expiring) p.expiring = true
  if (searchParams.startDate) p.startDate = searchParams.startDate
  if (searchParams.endDate) p.endDate = searchParams.endDate
  return p
}

async function fetchData() {
  if (activeTab.value === 'energyStats') {
    await fetchStats()
    return
  }
  loading.value = true
  try {
    const params = buildParams()
    let res: any
    switch (activeTab.value) {
      case 'binding': res = await verificationApi.bindingPage(params); break
      case 'alert': res = await verificationApi.alertPage(params); break
      case 'inspection': res = await verificationApi.inspectionPage(params); break
      case 'energy': res = await energyLogApi.page(params); break
      case 'energyCard': res = await energyCardApi.page(params); break
      case 'cert': res = await verificationApi.certificatePage(params); break
      case 'onboarding': res = await verificationApi.onboardingPage(params); break
      default: res = await verificationApi.kycPage(params)
    }
    const data = res?.data ?? res
    const records: any[] = data?.records || []
    tableData.value = records.map((r: any, i: number) => ({ ...r, rowKey: `${activeTab.value}-${r.id}-${i}` }))
    pagination.total = Number(data?.total) || 0
  } catch (e: any) {
    console.warn('[实名认证] 加载失败', e)
    message.error(e?.message || '加载数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function resetSearchParams() {
  Object.keys(searchParams).forEach(k => {
    searchParams[k] = typeof searchParams[k] === 'boolean' ? false : undefined
  })
  dateRange.value = null
}

function handleTabChange(key: string) {
  activeTab.value = key as TabKey
  resetSearchParams()
  syncTabConfig()
  pagination.current = 1
  fetchData()
}

// ═══ 下拉选项 ═══
const riderOptions = ref<{ value: string; label: string; phone?: string; riderType?: number; channelId?: number }[]>([])
const vehicleOptions = ref<{ value: string; label: string }[]>([])
const vehicleRawList = ref<any[]>([])

async function fetchRiderOptions(keyword?: string) {
  try {
    const res: any = await verificationApi.riderOptions(keyword)
    const list = res?.data ?? res ?? []
    riderOptions.value = list.map((r: any) => ({
      value: String(r.value),
      label: `${r.label || ''}${r.phone ? ' / ' + r.phone : ''}`,
      phone: r.phone,
      riderType: r.riderType,
      channelId: r.channelId,
    }))
  } catch (e) {
    console.warn('[实名认证] 加载配送员选项失败', e)
  }
}

async function fetchVehicleOptions() {
  try {
    // 复用车辆模块统一选择器数据源（排除已报废），不重复实现
    const res: any = await vehicleApi.options()
    const list = res?.data ?? res ?? []
    vehicleRawList.value = Array.isArray(list) ? list : []
    vehicleOptions.value = vehicleRawList.value.map((v: any) => ({
      value: String(v.id),
      label: v.plateNo || v.vehicleCode || String(v.id),
    }))
  } catch (e) {
    console.warn('[实名认证] 加载车辆选项失败', e)
  }
}

// ═══ 实名认证提交 ═══
const kycVisible = ref(false)
const kycSaving = ref(false)
const kycFormRef = ref<FormInstance>()
const kycFileInputRef = ref<HTMLInputElement>()
const kycImageSide = ref<'front' | 'back'>('front')
const kycForm = reactive<any>({
  id: undefined, riderId: undefined, realName: '', idCardNo: '', channelName: '',
  idCardFrontUrl: '', idCardBackUrl: '', endorseOrg: '', endorseResult: undefined,
  endorseExpireDate: undefined, remark: '', submitAudit: true, certificates: [],
})
const kycRules: Record<string, any[]> = {
  riderId: [{ required: true, message: '请选择配送员', trigger: 'change' }],
}
const kycRiderTypeText = computed(() => {
  const hit = riderOptions.value.find(o => o.value === String(kycForm.riderId))
  return hit?.riderType ? RIDER_TYPE_MAP[hit.riderType] || '-' : '-'
})

const certColumns = [
  { title: '证照类型', dataIndex: 'certType', width: 130 },
  { title: '证照编号', dataIndex: 'certNo', width: 160 },
  { title: '发证日期', dataIndex: 'issueDate', width: 130 },
  { title: '有效期至', dataIndex: 'expireDate', width: 130 },
  { title: '影像地址', dataIndex: 'certUrl' },
  { title: '操作', dataIndex: 'action', width: 70 },
]

let certUid = 0

function addCertRow() {
  kycForm.certificates.push({
    _uid: `c${++certUid}`, certType: 1, certNo: '', issueDate: undefined, expireDate: undefined, certUrl: '',
  })
}

function removeCertRow(index: number) {
  kycForm.certificates.splice(index, 1)
}

function resetKycForm() {
  Object.assign(kycForm, {
    id: undefined, riderId: undefined, realName: '', idCardNo: '', channelName: '',
    idCardFrontUrl: '', idCardBackUrl: '', endorseOrg: '', endorseResult: undefined,
    endorseExpireDate: undefined, remark: '', submitAudit: true, certificates: [],
  })
  addCertRow()
}

function parseIdCardUrls(json?: string): { front: string; back: string } {
  if (!json) return { front: '', back: '' }
  try {
    const obj = JSON.parse(json)
    return { front: obj.front || '', back: obj.back || '' }
  } catch {
    return { front: '', back: '' }
  }
}

/** 打开实名认证弹窗：record 为空=新增；有值=编辑（回填台账 + 证照明细） */
async function openKycSubmit(record?: any) {
  resetKycForm()
  if (record) {
    try {
      const res: any = await verificationApi.kycDetail(record.id)
      const d = res?.data ?? res
      const urls = parseIdCardUrls(d.idCardUrls)
      Object.assign(kycForm, {
        id: d.id,
        riderId: d.riderId != null ? String(d.riderId) : undefined,
        realName: d.realName || '',
        idCardNo: d.idCardNo || '',
        channelName: d.channelName || '',
        idCardFrontUrl: urls.front,
        idCardBackUrl: urls.back,
        endorseOrg: d.endorseOrg || '',
        endorseResult: d.endorseResult,
        endorseExpireDate: d.endorseExpireDate || undefined,
        remark: d.remark || '',
        submitAudit: d.verifyStatus !== 2,
        certificates: (d.certificates || []).map((c: any) => ({ ...c, _uid: `c${++certUid}` })),
      })
      if (kycForm.certificates.length === 0) addCertRow()
    } catch (e: any) {
      message.error(e?.message || '加载实名认证详情失败')
      return
    }
  }
  kycVisible.value = true
}

function onRiderChange(value: any) {
  const hit = riderOptions.value.find(o => o.value === String(value))
  kycForm.channelName = hit?.channelId ? String(hit.channelId) : ''
}

function pickKycImage(side: 'front' | 'back') {
  kycImageSide.value = side
  kycFileInputRef.value?.click()
}

async function onKycImageChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res: any = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : (res?.url || res?.data?.url)
    if (!url) {
      message.error('图片上传失败')
      return
    }
    if (kycImageSide.value === 'front') {
      kycForm.idCardFrontUrl = url
    } else {
      kycForm.idCardBackUrl = url
    }
  } catch {
    message.error('图片上传失败')
  }
}

async function submitKyc() {
  try {
    await kycFormRef.value?.validate()
  } catch {
    return
  }
  kycSaving.value = true
  try {
    await verificationApi.kycSubmit({
      id: kycForm.id,
      riderId: kycForm.riderId,
      realName: kycForm.realName || undefined,
      idCardNo: kycForm.idCardNo || undefined,
      idCardFrontUrl: kycForm.idCardFrontUrl || undefined,
      idCardBackUrl: kycForm.idCardBackUrl || undefined,
      endorseOrg: kycForm.endorseOrg || undefined,
      endorseResult: kycForm.endorseResult,
      endorseExpireDate: kycForm.endorseExpireDate || undefined,
      remark: kycForm.remark || undefined,
      submitAudit: kycForm.submitAudit,
      certificates: kycForm.certificates.map((c: any) => ({
        certType: c.certType,
        certNo: c.certNo || undefined,
        issueDate: c.issueDate || undefined,
        expireDate: c.expireDate || undefined,
        certUrl: c.certUrl || undefined,
        remark: c.remark,
      })),
    })
    message.success(kycForm.submitAudit ? '已提交审核' : '已保存草稿')
    kycVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '提交失败')
  } finally {
    kycSaving.value = false
  }
}

// ═══ 实名认证审核 ═══
const auditVisible = ref(false)
const auditSaving = ref(false)
const auditRecord = ref<any>(null)

function openKycAudit(record: any) {
  auditRecord.value = record
  auditVisible.value = true
}

async function handleKycAudit(values: { approved: boolean; auditRemark?: string; endorseExpireDate?: string }) {
  auditSaving.value = true
  try {
    await verificationApi.kycAudit(auditRecord.value.id, {
      approved: values.approved,
      auditRemark: values.auditRemark || undefined,
      endorseExpireDate: values.endorseExpireDate || undefined,
    })
    message.success(values.approved ? '已通过' : '已驳回')
    auditVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '审核失败')
  } finally {
    auditSaving.value = false
  }
}

// ═══ 到期扫描 / 批量核验 ═══
async function handleScanExpiry() {
  try {
    const res: any = await verificationApi.scanExpiry()
    const count = res?.data ?? res ?? 0
    message.success(`到期扫描完成，新增预警 ${count} 条`)
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '到期扫描失败')
  }
}

async function handleScan() {
  try {
    const res: any = await verificationApi.scan()
    const r = res?.data ?? res ?? {}
    message.success('批量核验完成：扫描 ' + (r.scannedBindings || 0) + ' 条绑定，'
      + '超时 ' + (r.timeoutAlerts || 0) + ' / 滞留 ' + (r.stayAlerts || 0)
      + ' / 分离 ' + (r.separationAlerts || 0))
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '批量核验失败')
  }
}

// ═══ 出车登记（两步向导：绑定信息 → 出车前检查） ═══
const bindVisible = ref(false)

function openBindModal() {
  bindVisible.value = true
}

function onBindSaved() {
  activeTab.value = 'binding'
  fetchData()
}

// ═══ 交车（交车登记 + 收车后检查） ═══
const handoverVisible = ref(false)
const handoverInitRecord = ref<any>(null)

function openHandover(record?: any) {
  handoverInitRecord.value = record || null
  handoverVisible.value = true
}

// ═══ 预警处理 ═══
const alertVisible = ref(false)
const alertSaving = ref(false)
const alertRecord = ref<any>(null)

function openAlertHandle(record: any) {
  alertRecord.value = record
  alertVisible.value = true
}

async function handleAlertSubmit(values: { handleStatus: number; remark?: string }) {
  alertSaving.value = true
  try {
    await verificationApi.handleAlert(alertRecord.value.id, {
      handleStatus: values.handleStatus,
      remark: values.remark || undefined,
    })
    message.success('处理成功')
    alertVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '处理失败')
  } finally {
    alertSaving.value = false
  }
}

// ═══ 巡检审核 ═══
const inspectionVisible = ref(false)
const inspectionSaving = ref(false)
const inspectionRecord = ref<any>(null)

function openInspectionReview(record: any) {
  inspectionRecord.value = record
  inspectionVisible.value = true
}

async function handleInspectionReview(values: { result: number; remark?: string }) {
  inspectionSaving.value = true
  try {
    await verificationApi.reviewInspection(inspectionRecord.value.id, {
      result: values.result,
      remark: values.remark || undefined,
    })
    message.success('审核完成')
    inspectionVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '审核失败')
  } finally {
    inspectionSaving.value = false
  }
}

// ═══ 抽检 / 定检（车管员发起，不关联人车绑定） ═══
const spotCheckVisible = ref(false)
const spotCheckSaving = ref(false)

function openSpotCheck() {
  spotCheckVisible.value = true
}

async function handleSpotCheckSubmit(values: any) {
  if (!values.vehicleId) {
    message.warning('请选择车辆')
    return
  }
  spotCheckSaving.value = true
  try {
    await verificationApi.inspectionCreate(values)
    message.success('检查记录已保存')
    spotCheckVisible.value = false
    activeTab.value = 'inspection'
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    spotCheckSaving.value = false
  }
}

// ═══ 能耗报表（P2：汇总 + 油电对比 + 分组） ═══
const statsLoading = ref(false)
const statsData = ref<any>(null)

const fmtAmount = (v: any) => (v == null ? '-' : Number(v).toFixed(2))

const statsCards = computed(() => {
  const d = statsData.value || {}
  return [
    { label: '补能记录', value: d.logCount ?? 0, suffix: '条' },
    { label: '补能金额', value: fmtAmount(d.totalAmount), suffix: '元' },
    { label: '有效里程', value: d.totalMileage ?? 0, suffix: 'km' },
    { label: '平均每公里成本', value: d.avgUnitCost == null ? '-' : fmtAmount(d.avgUnitCost), suffix: '元/km' },
  ]
})

const compareRows = computed(() => {
  const d = statsData.value || {}
  return [d.fuelGroup, d.powerGroup].filter(Boolean)
})

const statsColumns: any[] = [
  { title: '主体', dataIndex: 'label' },
  { title: '记录数', dataIndex: 'logCount', width: 90, align: 'right' },
  { title: '金额(元)', dataIndex: 'amount', width: 110, align: 'right', customRender: ({ text }: any) => fmtAmount(text) },
  { title: '数量', dataIndex: 'quantity', width: 100, align: 'right' },
  { title: '里程(km)', dataIndex: 'mileage', width: 100, align: 'right' },
  { title: '每公里成本(元)', dataIndex: 'unitCost', width: 130, align: 'right', customRender: ({ text }: any) => (text == null ? '-' : fmtAmount(text)) },
  { title: 'CO₂(kg)', dataIndex: 'co2Kg', width: 100, align: 'right', customRender: ({ text }: any) => fmtAmount(text) },
]

const compareOption = computed(() => {
  const rows = compareRows.value
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['每公里成本(元)'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: rows.map(r => r.label) },
    yAxis: { type: 'value', name: '元/km' },
    series: [{
      name: '每公里成本(元)',
      type: 'bar',
      barWidth: 48,
      data: rows.map(r => r.unitCost ?? 0),
      itemStyle: { color: (p: any) => (p.dataIndex === 0 ? '#fa8c16' : '#52c41a') },
      label: { show: true, position: 'top', formatter: (p: any) => (p.value ? Number(p.value).toFixed(2) : '-') },
    }],
  }
})

const typePieOption = computed(() => {
  const rows = statsData.value?.byEnergyType || []
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 元 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      data: rows.map(r => ({ name: r.label, value: Number(r.amount || 0) })),
      label: { formatter: '{b}\n{d}%' },
    }],
  }
})

async function fetchStats() {
  statsLoading.value = true
  try {
    const params: Record<string, any> = {}
    if (searchParams.startDate) params.startDate = searchParams.startDate
    if (searchParams.endDate) params.endDate = searchParams.endDate
    if (searchParams.subjectType) params.subjectType = searchParams.subjectType
    const res: any = await energyLogApi.stats(params)
    statsData.value = res?.data ?? res
  } catch (e: any) {
    message.error(e?.message || '加载能耗报表失败')
    statsData.value = null
  } finally {
    statsLoading.value = false
  }
}

// ═══ 补能卡 / 套餐（一卡一车一人） ═══
const cardVisible = ref(false)
const cardInitRecord = ref<any>(null)

function openCardModal(record?: any) {
  cardInitRecord.value = record || null
  cardVisible.value = true
}

function onCardSaved() {
  fetchCardOptions()
  fetchData()
}

function removeCard(record: any) {
  Modal.confirm({
    title: '确认信息',
    content: `确定删除补能卡「${record.cardNo}」？删除后该卡的补能流水将无法稽核卡状态。`,
    async onOk() {
      try {
        await energyCardApi.remove(record.id)
        message.success('删除成功')
        fetchCardOptions()
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

async function toggleCardStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await energyCardApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    fetchCardOptions()
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

// ═══ 准入核验（人证 × 车证一屏） ═══
function gotoKycFromOnboarding(record: any) {
  // 该骑手若有实名认证台账则直达台账详情；否则提示先提交
  verificationApi.kycByRider(record.riderId).then((res: any) => {
    const kyc = res?.data ?? res
    if (kyc?.id) {
      activeTab.value = 'kyc'
      openKycDetail({ id: kyc.id })
    } else {
      message.warning('该配送员尚未提交实名认证')
    }
  }).catch(() => message.warning('该配送员尚未提交实名认证'))
}

// ═══ 补能录入（车/人二选一） ═══
const energyVisible = ref(false)
const energyInitRecord = ref<any>(null)
/** 启用补能卡选项（录入时选卡） */
const energyCardOptions = ref<{ value: string; label: string }[]>([])
/** 原始卡档案（选卡后带出主体/类型用） */
const energyCardList = ref<any[]>([])
async function fetchCardOptions() {
  try {
    const res: any = await energyCardApi.options()
    const list = res?.data ?? res ?? []
    energyCardList.value = Array.isArray(list) ? list : []
    energyCardOptions.value = energyCardList.value.map((c: any) => ({
      value: c.cardNo,
      label: `${c.cardNo}${c.cardName ? ' / ' + c.cardName : ''}（${c.subjectName || '-'}）`,
    }))
  } catch (e) {
    console.warn('[用车管理] 加载补能卡选项失败', e)
  }
}

function openEnergyModal(record?: any) {
  energyInitRecord.value = record || null
  energyVisible.value = true
}

function onEnergySaved() {
  fetchData()
}

function removeEnergy(record: any) {
  Modal.confirm({
    title: '确认信息',
    content: `确定删除该条补能记录（${formatDateTime(record.occurredAt)}）？`,
    async onOk() {
      try {
        await energyLogApi.remove(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

// ═══ 巡检详情 ═══
const inspectionDetailVisible = ref(false)
const inspectionDetailLoading = ref(false)
const inspectionDetailData = ref<any>(null)

async function openInspectionDetail(record: any) {
  inspectionDetailVisible.value = true
  inspectionDetailLoading.value = true
  inspectionDetailData.value = null
  try {
    const res: any = await verificationApi.inspectionDetail(record.id)
    inspectionDetailData.value = res?.data ?? res
  } catch (e: any) {
    message.error(e?.message || '加载巡检详情失败')
  } finally {
    inspectionDetailLoading.value = false
  }
}

// ═══ 实名认证详情（已通过台账只读） ═══
const kycDetailVisible = ref(false)
const kycDetailLoading = ref(false)
const kycDetailData = ref<any>(null)

async function openKycDetail(record: any) {
  kycDetailVisible.value = true
  kycDetailLoading.value = true
  kycDetailData.value = null
  try {
    const res: any = await verificationApi.kycDetail(record.id)
    kycDetailData.value = res?.data ?? res
  } catch (e: any) {
    message.error(e?.message || '加载实名认证详情失败')
  } finally {
    kycDetailLoading.value = false
  }
}

// ═══ 绑定详情 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<any>(null)


async function openBindingDetail(record: any) {
  await loadBindingDetail(record.id)
}

async function openBindingDetailById(bindingId: number | string) {
  await loadBindingDetail(bindingId)
}

async function loadBindingDetail(id: number | string) {
  detailVisible.value = true
  detailLoading.value = true
  detailData.value = null
  try {
    const res: any = await verificationApi.bindingDetail(id)
    detailData.value = res?.data ?? res
  } catch (e: any) {
    message.error(e?.message || '加载绑定详情失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 导出 / 打印 ═══
async function handleExport() {
  if (activeTab.value === 'cert') {
    message.warning('证照核验请使用「查看台账」后按台账导出，避免两处口径不一致')
    return
  }
  try {
    const params = buildParams()
    delete params.page
    delete params.size
    const blob = activeTab.value === 'energy'
      ? await energyLogApi.export(params)
      : await verificationApi.exportExcel(tabCode.value, params)
    const url = window.URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `实名认证-${tabLabel(activeTab.value)}_${dayjs().format('YYYY-MM-DD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.warn('[实名认证] 导出失败', e)
    message.error('导出失败')
  }
}

function tabLabel(key: string): string {
  return tabs.value.find(t => t.key === key)?.label || key
}

const printDialogRef = ref<any>(null)

/**
 * 打印用的列：当前 Tab 表格实际在用那套（逐 Tab 各有一套），剔掉序号/操作/槽位与无标题列。
 * 引擎按 `items.columnsFrom: "columns"` 动态画表头 ⇒ 列改了不用改模板。
 */
const printColumns = computed(() =>
  currentColumns.value
    .filter((c: any) => c.title
      && !['rowNo', 'checkbox', 'action', 'slot'].includes(c.type)
      && !['rowNo', 'checkbox', 'action'].includes(c.key))
    .map((c: any) => ({ key: c.field || c.key, title: c.title, width: c.width, align: c.align })),
)

const printData = computed<Record<string, any>>(() => ({
  // 结果集打印的约定形状：title / columns / rows（原有键一并保留，保持兼容）
  title: `实名认证 - ${tabLabel(activeTab.value)}`,
  columns: printColumns.value,
  totalText: `共 ${pagination.total} 条`,
  pageTitle: `实名认证 - ${tabLabel(activeTab.value)}`,
  rows: (selectedRows.value.length ? selectedRows.value : tableData.value),
  total: pagination.total,
  printTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
}))

function handlePrintF8() {
  if (tableData.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  printDialogRef.value?.open?.()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrintF8()
  }
}

// ═══ 初始化 ═══
onMounted(async () => {
  syncTabConfig()
  await Promise.all([fetchRiderOptions(), fetchVehicleOptions(), fetchCardOptions()])
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* 插槽内容样式需自备（Vue scoped 不作用到布局组件内部的插槽内容） */
/* ── 能耗报表面板 ── */
.energy-stats { padding: 12px 16px; overflow-y: auto; height: 100%; }
.stat-card { background: #fafafa; border: 1px solid #f0f0f0; border-radius: 6px; padding: 12px 14px; margin-bottom: 12px; }
.stat-label { font-size: 12px; color: #8c8c8c; margin-bottom: 6px; }
.stat-main { display: flex; align-items: baseline; gap: 4px; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; line-height: 1.2; }
.stat-suffix { font-size: 12px; color: #8c8c8c; }
.panel { background: #fff; border: 1px solid #f0f0f0; border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.panel-title { font-size: 13px; font-weight: 600; color: #303133; margin-bottom: 8px; }
.mini-table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 12px; }
.mini-table th, .mini-table td { border: 1px solid #f0f0f0; padding: 4px 8px; text-align: right; }
.mini-table th:first-child, .mini-table td:first-child { text-align: left; }
.mini-table th { background: #fafafa; color: #595959; font-weight: 500; }
.muted { color: #8c8c8c; font-size: 12px; margin-left: 6px; }
.search-area {
  padding: 2px 0;
}
.search-grid {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.search-field-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 4px;
}
.drawer-sub-title {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
