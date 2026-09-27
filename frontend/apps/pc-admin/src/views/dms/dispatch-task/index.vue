<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        调度任务（配送 → 调度管理 → 调度任务，菜单 80730 `dms:dispatch-task`）
        · 数据源与《配送单》《配送查询》同源（dms_task），本页为**调度视角**（配送员维度）
        · 骨架：CategoryListLayout（无分类树）+ BillTableList（表头序号齿轮列配置）+ PageConfigPanel
        · 能力：指派 / 改派（含负载上限校验与拒绝原因）/ 取消 / 异常 / 轨迹 /
          批量指派 / 批量取消 / 批量打印 / 自动调度（逐单结果报告）/ 超时升级 / 地图派单 / 调度审计
        · 口径依据：《调度任务开发文档》§2（现状）§3（金标准目标）§5（状态机）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：自动调度 / 超时升级 / 地图派单 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('autoDispatch')"
              type="primary"
              size="small"
              class="btn-add"
              :loading="autoDispatching"
              @click="handleAutoDispatch"
            >
              <ThunderboltOutlined /> 自动调度
            </a-button>
            <a-button
              v-if="isButtonEnabled('mapDispatch')"
              size="small"
              @click="openMapDispatch"
            >
              <EnvironmentOutlined /> 地图派单
            </a-button>
            <a-button
              v-if="isButtonEnabled('escalate')"
              size="small"
              :loading="escalating"
              @click="handleEscalate"
            >
              <ClockCircleOutlined /> 超时升级
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 + 刷新 + 打印 + 导出 + 调度策略配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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
            <a-button
              v-if="isButtonEnabled('strategyConfig')"
              size="small"
              @click="goStrategyConfig"
            >
              <ControlOutlined /> 调度策略配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向自适应网格；字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('taskNo')"
                class="search-item"
              >
                <span class="search-label">任务编号</span>
                <a-input
                  v-model:value="searchForm.taskNo"
                  placeholder="任务编号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('orderNo')"
                class="search-item"
              >
                <span class="search-label">订单号</span>
                <a-input
                  v-model:value="searchForm.orderNo"
                  placeholder="关联订单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('customerName')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-input
                  :value="searchForm.customerName"
                  placeholder="选择客户"
                  size="small"
                  style="width: 150px"
                  readonly
                  allow-clear
                  @click="customerPickerVisible = true"
                  @clear="clearCustomer"
                />
              </div>
              <div
                v-if="isQueryVisible('riderId')"
                class="search-item"
              >
                <span class="search-label">配送员</span>
                <a-select
                  v-model:value="searchForm.riderId"
                  placeholder="全部配送员"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="riderOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('vehicleId')"
                class="search-item"
              >
                <span class="search-label">配送车辆</span>
                <a-select
                  v-model:value="searchForm.vehicleId"
                  placeholder="全部车辆"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="vehicleOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('statusList')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.statusList"
                  mode="multiple"
                  placeholder="全部状态"
                  size="small"
                  style="min-width: 190px"
                  allow-clear
                  :max-tag-count="2"
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('priority')"
                class="search-item"
              >
                <span class="search-label">优先级</span>
                <a-select
                  v-model:value="searchForm.priority"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="PRIORITY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('orderType')"
                class="search-item"
              >
                <span class="search-label">类型</span>
                <a-select
                  v-model:value="searchForm.orderType"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="ORDER_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('deliveryDate')"
                class="search-item"
              >
                <span class="search-label">配送日期</span>
                <a-range-picker
                  v-model:value="deliveryDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 220px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('routeId')"
                class="search-item"
              >
                <span class="search-label">配送线路</span>
                <a-select
                  v-model:value="searchForm.routeId"
                  placeholder="全部线路"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="routeOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('routeArea')"
                class="search-item"
              >
                <span class="search-label">配送区域</span>
                <a-input
                  v-model:value="searchForm.routeArea"
                  placeholder="配送区域"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('unassigned')"
                class="search-item"
              >
                <span class="search-label">有无配送员</span>
                <a-select
                  v-model:value="searchForm.unassigned"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="ASSIGN_STATE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('abnormal')"
                class="search-item"
              >
                <span class="search-label">是否异常</span>
                <a-select
                  v-model:value="searchForm.abnormal"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="ABNORMAL_OPTIONS"
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
              ref="tableRef"
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
              storage-key="dms-dispatch-task-columns"
              global-config-key="dms-dispatch-task-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
              @sort-change="handleSortChange"
            >
              <template #batch-actions>
                <a-button
                  v-if="isButtonEnabled('batchAssign')"
                  size="small"
                  @click="openBatchAssign(selectedRows)"
                >
                  批量指派
                </a-button>
                <a-button
                  v-if="isButtonEnabled('batchCancel')"
                  size="small"
                  danger
                  @click="openBatchCancel(selectedRows)"
                >
                  批量取消
                </a-button>
                <a-button
                  v-if="isButtonEnabled('batchPrint')"
                  size="small"
                  @click="openBatchPrint(selectedRows)"
                >
                  批量打印
                </a-button>
              </template>

              <!-- 任务编号：点击查看调度审计时间线 -->
              <template #taskNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openLogs(record)"
                >{{ record.taskNo }}</a>
              </template>

              <template #orderTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="ORDER_TYPE_MAP[record.orderType]?.color || 'default'"
                >
                  {{ ORDER_TYPE_MAP[record.orderType]?.label || '-' }}
                </a-tag>
              </template>

              <template #riderCell="{ record }">
                <span v-if="!record.__ghost">{{ record.riderName || '-' }}</span>
              </template>

              <template #priorityCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="PRIORITY_MAP[record.priority]?.color || 'default'"
                >
                  {{ PRIORITY_MAP[record.priority]?.label || '普通' }}
                </a-tag>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusMeta(record.status).color"
                >
                  {{ record.statusText || statusMeta(record.status).label }}
                </a-tag>
              </template>

              <template #overdueCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.overdue ? 'red' : 'default'"
                >
                  {{ record.overdue ? '已超时' : '正常' }}
                </a-tag>
              </template>

              <template #totalQuantityCell="{ record }">
                <span v-if="!record.__ghost">{{ formatQty(record.totalQuantity) }}</span>
              </template>

              <template #goodsAmountCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.goodsAmount) }}</span>
              </template>

              <template #deliveryFeeCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.deliveryFee) }}</span>
              </template>

              <template #collectOnDeliveryCell="{ record }">
                <span v-if="!record.__ghost">{{ formatMoney(record.collectOnDelivery) }}</span>
              </template>

              <template #distanceCell="{ record }">
                <span v-if="!record.__ghost">{{ record.distanceKm == null ? '-' : record.distanceKm + ' km' }}</span>
              </template>

              <template #timeCell="{ record, column }">
                <span v-if="!record.__ghost">{{ formatTime(record[column.key]) || '-' }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="openAssignModal(record, 'assign')"
                  >
                    指派
                  </a-button>
                  <a-button
                    v-if="record.status === 1 || record.status === 2"
                    type="link"
                    size="small"
                    @click="openAssignModal(record, 'reassign')"
                  >
                    改派
                  </a-button>
                  <a-button
                    v-if="record.status === 0 || record.status === 1"
                    type="link"
                    size="small"
                    danger
                    @click="openCancelModal(record)"
                  >
                    取消
                  </a-button>
                  <a-button
                    v-if="record.riderId && [2, 3, 4].includes(record.status)"
                    type="link"
                    size="small"
                    @click="goTracking(record)"
                  >
                    轨迹
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
                          key="exception"
                          :disabled="![2, 3, 4, 5].includes(record.status)"
                        >
                          <ExclamationCircleOutlined /> 标记异常
                        </a-menu-item>
                        <a-menu-item key="logs">
                          <HistoryOutlined /> 调度审计
                        </a-menu-item>
                        <a-menu-item key="detail">
                          <ProfileOutlined /> 单据明细
                        </a-menu-item>
                        <a-menu-item key="print">
                          <PrinterOutlined /> 打印配送单
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

      <!-- ═══ 客户选择（往来单位，通用组件） ═══ -->
      <PartnerSelectModal
        v-model:open="customerPickerVisible"
        title="客户选择"
        default-tab="customer"
        @select="handleCustomerSelected"
      />

      <!-- ═══ 指派 / 改派配送员 ═══ -->
      <a-modal
        v-model:open="assignModalVisible"
        :title="assignMode === 'assign' ? '指派配送员' : '改派配送员'"
        :width="900"
        :mask-closable="false"
        :confirm-loading="assignSubmitting"
        :ok-button-props="{ disabled: !selectedRiderId }"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleAssignConfirm"
      >
        <div class="assign-task-info">
          任务单号：{{ currentTask?.taskNo }}（订单号：{{ currentTask?.orderNo }}）
          <template v-if="assignMode === 'reassign'">
            ，当前配送员：{{ currentTask?.riderName || currentTask?.riderId || '-' }}
          </template>
        </div>
        <div class="assign-toolbar">
          <a-space :size="8">
            <a-select
              v-model:value="candidateFilter"
              size="small"
              style="width: 130px"
              :options="CANDIDATE_FILTER_OPTIONS"
            />
            <a-select
              v-model:value="candidateSort"
              size="small"
              style="width: 130px"
              :options="CANDIDATE_SORT_OPTIONS"
            />
            <a-input
              v-model:value="candidateKeyword"
              placeholder="姓名 / 电话 / 车牌"
              size="small"
              style="width: 170px"
              allow-clear
            />
            <a-button
              size="small"
              @click="openMapDispatch(currentTask || undefined)"
            >
              <EnvironmentOutlined /> 地图选人
            </a-button>
          </a-space>
        </div>
        <a-table
          :columns="candidateColumns"
          :data-source="filteredCandidates"
          :loading="candidatesLoading"
          :row-selection="{ type: 'radio', selectedRowKeys: selectedRiderKeys, onChange: handleCandidateSelect, getCheckboxProps: candidateCheckboxProps }"
          :pagination="false"
          row-key="riderId"
          size="small"
          bordered
          :locale="{ emptyText: '暂无候选配送员' }"
          :scroll="{ y: 300 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'eligible'">
              <a-tag :color="record.eligible ? 'green' : 'default'">
                {{ record.eligible ? '可派' : '不可派' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              {{ RIDER_STATUS_TEXT[record.status] || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'distanceMeters'">
              {{ record.distanceMeters == null || Number(record.distanceMeters) >= 999999 ? '-' : Math.round(Number(record.distanceMeters)) + ' m' }}
            </template>
            <template v-else-if="column.dataIndex === 'reason'">
              <span class="reason-text">{{ record.reason || '-' }}</span>
            </template>
          </template>
        </a-table>
        <div class="assign-footer-tip">
          指派前校验配送员负载上限：在途并接数 / 载重 / 容积任一超限或配送员休息中即拒绝；距离与围栏属自动派单的优化约束，
          人工指派不拦截（资质校验以《配送参数》的准入核验开关为准）。
        </div>
        <div class="assign-reason">
          <span class="search-label">调度原因</span>
          <a-input
            v-model:value="assignReason"
            placeholder="选填，留痕到调度审计"
            size="small"
            :maxlength="200"
            style="width: 420px"
          />
        </div>
      </a-modal>

      <!-- ═══ 地图派单 ═══ -->
      <a-modal
        v-model:open="mapModalVisible"
        title="地图派单"
        :width="1080"
        :footer="null"
        :mask-closable="false"
      >
        <div class="map-toolbar-line">
          <a-space :size="8">
            <span class="search-label">待分配任务</span>
            <a-select
              v-model:value="mapTaskId"
              placeholder="选择待分配任务"
              size="small"
              style="width: 300px"
              show-search
              option-filter-prop="label"
              :options="mapTaskOptions"
              @change="handleMapTaskChange"
            />
            <a-button
              size="small"
              :loading="mapLoading"
              @click="loadMapData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-tag color="blue">
              任务 {{ mapTasks.length }}
            </a-tag>
            <a-tag color="orange">
              配送员 {{ mapCandidates.length }}
            </a-tag>
          </a-space>
        </div>
        <RouteMapCanvas
          title="派单地图（点击任务点选单，点击配送员点指派）"
          height="380px"
          :markers="mapMarkers"
          empty-text="暂无可落图的任务/配送员坐标"
          @marker-click="handleMapMarkerClick"
        />
        <div class="map-candidate-list">
          <div class="map-candidate-title">
            候选配送员
            <span v-if="mapSelectedTask">（任务 {{ mapSelectedTask.taskNo }}）</span>
          </div>
          <a-table
            :columns="mapCandidateColumns"
            :data-source="mapCandidates"
            :loading="candidatesLoading"
            :pagination="false"
            row-key="riderId"
            size="small"
            bordered
            :locale="{ emptyText: '请先在图上点选一个待分配任务' }"
            :scroll="{ y: 200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'eligible'">
                <a-tag :color="record.eligible ? 'green' : 'default'">
                  {{ record.eligible ? '可派' : '不可派' }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                {{ RIDER_STATUS_TEXT[record.status] || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'distanceMeters'">
                {{ record.distanceMeters == null || Number(record.distanceMeters) >= 999999 ? '-' : Math.round(Number(record.distanceMeters)) + ' m' }}
              </template>
              <template v-else-if="column.dataIndex === 'reason'">
                <span class="reason-text">{{ record.reason || '-' }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'op'">
                <a-button
                  type="link"
                  size="small"
                  :disabled="!record.eligible"
                  @click="assignFromMap(record)"
                >
                  指派
                </a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-modal>

      <!-- ═══ 批量指派 ═══ -->
      <a-modal
        v-model:open="batchAssignVisible"
        title="批量指派"
        :width="520"
        :mask-closable="false"
        :confirm-loading="batchSubmitting"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleBatchAssign"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item label="已选任务">
            <span>{{ batchRows.length }} 条</span>
          </a-form-item>
          <a-form-item label="指派配送员">
            <a-select
              v-model:value="batchRiderId"
              placeholder="请选择配送员"
              show-search
              option-filter-prop="label"
              :options="riderOptions"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="调度原因">
            <a-input
              v-model:value="batchReason"
              placeholder="选填，留痕到调度审计"
              :maxlength="200"
            />
          </a-form-item>
          <div class="modal-tip">
            仅「待分配」任务可批量指派；并接上限 / 载重 / 容积 / 资质不满足的任务会被跳过并在结果中列出原因。
          </div>
        </a-form>
      </a-modal>

      <!-- ═══ 批量取消 / 单条取消 ═══ -->
      <a-modal
        v-model:open="cancelModalVisible"
        title="取消任务"
        :width="480"
        :mask-closable="false"
        :confirm-loading="batchSubmitting"
        ok-text="确定取消"
        ok-type="danger"
        cancel-text="返回"
        @ok="handleCancelConfirm"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 17 }"
          size="small"
        >
          <a-form-item label="任务">
            <span>{{ cancelTargets.length === 1 ? cancelTargets[0]?.taskNo : `已选 ${cancelTargets.length} 条` }}</span>
          </a-form-item>
          <a-form-item label="取消原因">
            <a-textarea
              v-model:value="cancelReason"
              :rows="2"
              :maxlength="200"
              placeholder="选填，留痕到调度审计"
            />
          </a-form-item>
          <div class="modal-tip">
            仅「待分配 / 已分配」任务可取消；已分配任务取消后会自动释放配送员占用。
          </div>
        </a-form>
      </a-modal>

      <!-- ═══ 调度结果报告（自动调度 / 批量操作 / 超时升级） ═══ -->
      <a-modal
        v-model:open="reportVisible"
        :title="reportTitle"
        :width="760"
        :footer="null"
      >
        <div class="report-summary">
          <a-tag color="blue">
            提交 {{ report.total }}
          </a-tag>
          <a-tag color="green">
            成功 {{ report.success }}
          </a-tag>
          <a-tag :color="report.failed ? 'red' : 'default'">
            失败/跳过 {{ report.failed }}
          </a-tag>
        </div>
        <a-table
          :columns="reportColumns"
          :data-source="report.rows"
          :pagination="false"
          row-key="rowKey"
          size="small"
          bordered
          :locale="{ emptyText: '全部执行成功' }"
        />
      </a-modal>

      <!-- ═══ 调度审计时间线 ═══ -->
      <a-modal
        v-model:open="logsVisible"
        :title="`调度审计 — ${logsTaskNo}`"
        :width="860"
        :footer="null"
      >
        <a-table
          :columns="logColumns"
          :data-source="logRows"
          :loading="logsLoading"
          :pagination="false"
          row-key="rowKey"
          size="small"
          bordered
          :locale="{ emptyText: '暂无调度记录' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'actionText'">
              <a-tag :color="ACTION_COLOR[record.action] || 'default'">
                {{ record.actionText || record.action }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'riderChange'">
              <span>{{ riderChangeText(record) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 单据明细 ═══ -->
      <a-modal
        v-model:open="detailVisible"
        :title="`单据明细 — ${detailTaskNo}`"
        :width="900"
        :footer="null"
      >
        <a-table
          :columns="detailColumns"
          :data-source="detailRows"
          :loading="detailLoading"
          :pagination="false"
          row-key="lineKey"
          size="small"
          bordered
        />
      </a-modal>

      <!-- ═══ 批量打印：打印模板选择（对标 ql361 BatchPsSelectPrintTemplate） ═══ -->
      <a-modal
        v-model:open="batchPrintVisible"
        title="打印模板选择"
        :width="520"
        ok-text="打印"
        cancel-text="取消"
        @ok="confirmBatchPrint"
      >
        <div class="batch-print-body">
          <div class="bp-row">
            <span class="bp-label">打印内容</span>
            <a-checkbox-group v-model:value="printScope">
              <a-checkbox value="bill">
                配送单
              </a-checkbox>
            </a-checkbox-group>
          </div>
          <div class="bp-row">
            <span class="bp-label">过滤条件</span>
            <a-checkbox v-model:checked="onlyUnprinted">
              仅打印未打印过的配送单
            </a-checkbox>
          </div>
          <div class="bp-row bp-muted">
            已选 {{ selectedRows.length }} 条，将按顺序逐单打印。
          </div>
        </div>
      </a-modal>

      <!-- ═══ 打印弹窗（打印模板渲染） ═══ -->
      <PrintDialog
        ref="printDialogRef"
        page-code="dispatch-task"
        :document-id="printData.id"
        :print-data="printData"
        @print-success="handlePrintSuccess"
      />

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-dispatch-task-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onActivated, onBeforeUnmount, nextTick, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ThunderboltOutlined, ReloadOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  EnvironmentOutlined, ClockCircleOutlined, ControlOutlined, DownOutlined, HistoryOutlined,
  ProfileOutlined, ExclamationCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import RouteMapCanvas from '@/components/business/RouteMapCanvas/index.vue'
import { taskApi, type DmsTask, type BatchResultVO } from '@/api/dms/task'
import { dispatchApi, type DispatchCandidate } from '@/api/dms/dispatch'
import { mdRouteApi } from '@/api/md'

defineOptions({ name: 'DmsDispatchTask' })

const router = useRouter()
const route = useRoute()

// ═══ 枚举（与后端 TaskStatusEnum / DispatchTypeEnum 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待分配', color: 'orange' },
  1: { label: '已分配', color: 'blue' },
  2: { label: '已接单', color: 'cyan' },
  3: { label: '取货中', color: 'processing' },
  4: { label: '配送中', color: 'processing' },
  5: { label: '已签收', color: 'geekblue' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' },
  8: { label: '异常', color: 'magenta' },
}

const ORDER_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '销售配送', color: 'blue' },
  2: { label: '调拨', color: 'purple' },
  3: { label: '退货', color: 'orange' },
}

const PRIORITY_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通', color: 'default' },
  2: { label: '紧急', color: 'orange' },
  3: { label: '加急', color: 'red' },
}

const RIDER_STATUS_TEXT: Record<number, string> = {
  0: '离线', 1: '空闲', 2: '忙碌', 3: '休息',
}

const ACTION_COLOR: Record<string, string> = {
  ASSIGN: 'blue',
  REASSIGN: 'geekblue',
  UNASSIGN: 'default',
  CANCEL: 'red',
  EXCEPTION: 'magenta',
  AUTO_ASSIGN: 'green',
  ESCALATE: 'volcano',
  BATCH_ASSIGN: 'blue',
  BATCH_CANCEL: 'red',
}

const STATUS_OPTIONS = Object.entries(STATUS_MAP).map(([v, m]) => ({ label: m.label, value: Number(v) }))
const ORDER_TYPE_OPTIONS = Object.entries(ORDER_TYPE_MAP).map(([v, m]) => ({ label: m.label, value: Number(v) }))
const PRIORITY_OPTIONS = Object.entries(PRIORITY_MAP).map(([v, m]) => ({ label: m.label, value: Number(v) }))
const ASSIGN_STATE_OPTIONS = [
  { label: '仅未分配', value: true },
  { label: '仅已分配', value: false },
]
const ABNORMAL_OPTIONS = [
  { label: '仅异常', value: true },
  { label: '非异常', value: false },
]

function statusMeta(status?: number) {
  return STATUS_MAP[Number(status)] || { label: `状态${status}`, color: 'default' }
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || Number.isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || Number.isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatTime(val?: string | null): string {
  return val ? String(val).replace('T', ' ').slice(0, 19) : ''
}

// ═══ 下钻入口：《配送仪表盘》卡片 → ?status=0..8 / ?riderId=xxx ═══
const initialQuery = computed<{ status?: number; riderId?: string }>(() => {
  const q: { status?: number; riderId?: string } = {}
  const status = route.query.status
  if (status !== undefined && status !== null && status !== '') q.status = Number(status)
  const riderId = route.query.riderId
  if (riderId !== undefined && riderId !== null && riderId !== '') q.riderId = String(riderId)
  return q
})

// ═══ 查询区状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<DmsTask[]>([])
const selectedRows = ref<DmsTask[]>([])
const tableRef = ref<InstanceType<typeof BillTableList> | null>(null)

/**
 * 清空表格勾选
 *
 * <p>必须做：勾选状态由表格组件内部按行 ID 持有，**换查询条件后旧勾选仍残留**，
 * 批量指派/取消会把「已经不在列表里的单据」也一起提交（采购收货/配送查询都踩过同一坑）。</p>
 */
function clearSelection() {
  tableRef.value?.clearSelection?.()
  selectedRows.value = []
}

const searchForm = reactive({
  taskNo: '',
  orderNo: '',
  // 雪花 ID 一律以字符串保存（Number() 会丢精度，见 JS 大整数精度修复）
  customerId: undefined as string | undefined,
  customerName: '',
  riderId: undefined as string | undefined,
  vehicleId: undefined as string | undefined,
  statusList: [] as number[],
  priority: undefined as number | undefined,
  orderType: undefined as number | undefined,
  routeId: undefined as string | undefined,
  routeArea: '',
  unassigned: undefined as boolean | undefined,
  abnormal: undefined as boolean | undefined,
})

const deliveryDateRange = ref<[string, string] | undefined>()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const sortState = reactive<{ field?: string; order?: string }>({ field: undefined, order: undefined })

const riderOptions = ref<{ label: string; value: string }[]>([])
const vehicleOptions = ref<{ label: string; value: string }[]>([])
const routeOptions = ref<{ label: string; value: string }[]>([])

/** 配送资源候选统一口径：与《配送查询》《配送单》同源 /api/dms/task/filter-options */
async function loadResourceOptions() {
  try {
    const res: any = await taskApi.filterOptions()
    const data = res?.data?.data ?? res?.data ?? res ?? {}
    riderOptions.value = (data.drivers || []).map((r: any) => ({
      label: r.extra ? `${r.name}（${r.extra}）` : r.name,
      value: String(r.id),
    }))
    vehicleOptions.value = (data.vehicles || []).map((v: any) => ({
      label: v.extra ? `${v.name}（${v.extra}）` : v.name,
      value: String(v.id),
    }))
  } catch (e) {
    console.warn('[调度任务] 配送资源下拉加载失败', e)
  }
}

/** 配送线路候选（线路主数据，仅启用） */
async function loadRouteOptions() {
  try {
    const res: any = await mdRouteApi.options()
    const list: any[] = res?.data ?? res ?? []
    routeOptions.value = (Array.isArray(list) ? list : []).map((r: any) => ({
      label: r.routeName || r.routeCode,
      value: String(r.id),
    }))
  } catch (e) {
    console.warn('[调度任务] 配送线路下拉加载失败', e)
  }
}

// ═══ 数据表列（《调度任务开发文档》§3.2；★ = 默认显示） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  // BillTableList 无 rowSelection 配置，勾选列需自行声明（批量指派/取消/打印依赖它）
  { title: '', key: 'rowCheckbox', type: 'checkbox', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { title: '任务编号', key: 'taskNo', type: 'slot', slotName: 'taskNoCell', width: 170, fixed: 'left' },
  { title: '订单号', key: 'orderNo', width: 150 },
  { title: '类型', key: 'orderType', type: 'slot', slotName: 'orderTypeCell', width: 100 },
  { title: '客户', key: 'customerName', width: 150, ellipsis: true },
  { title: '收货地址', key: 'customerAddress', width: 220, ellipsis: true },
  { title: '配送员', key: 'riderName', type: 'slot', slotName: 'riderCell', width: 110 },
  { title: '配送员电话', key: 'riderPhone', width: 130 },
  { title: '车辆/车牌', key: 'vehicleName', width: 120 },
  { title: '数量', key: 'totalQuantity', type: 'slot', slotName: 'totalQuantityCell', width: 100, align: 'right', sortable: true },
  { title: '货品金额', key: 'goodsAmount', type: 'slot', slotName: 'goodsAmountCell', width: 120, align: 'right', sortable: true },
  { title: '优先级', key: 'priority', type: 'slot', slotName: 'priorityCell', width: 90, sortable: true },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 100, sortable: true },
  { title: '期望送达', key: 'deadlineTime', type: 'slot', slotName: 'timeCell', width: 160, sortable: true },
  { title: '下单时间', key: 'createTime', type: 'slot', slotName: 'timeCell', width: 160, sortable: true },
  // ── 以下为默认隐藏列（可在表头齿轮中开启） ──
  { title: '距目的地', key: 'distanceKm', type: 'slot', slotName: 'distanceCell', width: 110, align: 'right', defaultHidden: true },
  { title: '负载(在途)', key: 'activeTaskCount', width: 100, align: 'right', defaultHidden: true },
  { title: '超时', key: 'overdue', type: 'slot', slotName: 'overdueCell', width: 90, defaultHidden: true },
  { title: '配送费', key: 'deliveryFee', type: 'slot', slotName: 'deliveryFeeCell', width: 100, align: 'right', sortable: true, defaultHidden: true },
  { title: '代收货款', key: 'collectOnDelivery', type: 'slot', slotName: 'collectOnDeliveryCell', width: 110, align: 'right', defaultHidden: true },
  { title: '配送区域', key: 'routeArea', width: 140, defaultHidden: true },
  { title: '来源单据', key: 'sourceBillNo', width: 150, defaultHidden: true },
  { title: '客户电话', key: 'customerPhone', width: 130, defaultHidden: true },
  { title: '指派时间', key: 'dispatchTime', type: 'slot', slotName: 'timeCell', width: 160, sortable: true, defaultHidden: true },
  { title: '取货时间', key: 'pickupTime', type: 'slot', slotName: 'timeCell', width: 160, defaultHidden: true },
  { title: '完成时间', key: 'completedTime', type: 'slot', slotName: 'timeCell', width: 160, sortable: true, defaultHidden: true },
  { title: '打印次数', key: 'printCount', width: 90, align: 'right', defaultHidden: true },
  { title: '制单人', key: 'creatorName', width: 100, defaultHidden: true },
  { title: '备注', key: 'remark', width: 180, defaultHidden: true },
]

// ═══ 候选配送员列（与后端 DispatchCandidateVO 一一对应） ═══
const candidateColumns: any[] = [
  { title: '配送员', dataIndex: 'riderName', width: 100 },
  { title: '电话', dataIndex: 'riderPhone', width: 120 },
  { title: '车牌', dataIndex: 'vehicleNo', width: 100 },
  { title: '状态', dataIndex: 'status', width: 70 },
  { title: '评分', dataIndex: 'ratingScore', width: 70, align: 'right' },
  { title: '累计单量', dataIndex: 'totalOrders', width: 90, align: 'right' },
  { title: '负载', dataIndex: 'activeTasks', width: 70, align: 'right' },
  { title: '距离', dataIndex: 'distanceMeters', width: 90, align: 'right' },
  { title: '综合得分', dataIndex: 'score', width: 90, align: 'right' },
  { title: '可派', dataIndex: 'eligible', width: 80 },
  { title: '说明', dataIndex: 'reason', width: 240 },
]

/** 地图派单候选列表：在通用候选列后追加「指派」操作列 */
const mapCandidateColumns: any[] = [
  ...candidateColumns,
  { title: '操作', dataIndex: 'op', width: 80, fixed: 'right' },
]

const reportColumns: any[] = [
  { title: '任务编号', dataIndex: 'taskNo', width: 180 },
  { title: '原因', dataIndex: 'reason', width: 420 },
]

const logColumns: any[] = [
  { title: '动作', dataIndex: 'actionText', width: 110 },
  { title: '变更', dataIndex: 'riderChange', width: 200 },
  { title: '原因', dataIndex: 'reason', width: 260 },
  { title: '操作人', dataIndex: 'operatorName', width: 100 },
  { title: '时间', dataIndex: 'createTime', width: 160 },
]

const detailColumns: any[] = [
  { title: '行号', dataIndex: 'lineNo', width: 60 },
  { title: '商品编码', dataIndex: 'productCode', width: 130 },
  { title: '商品名称', dataIndex: 'productName', width: 180 },
  { title: '规格', dataIndex: 'spec', width: 120 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '数量', dataIndex: 'quantity', width: 90, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', width: 90, align: 'right' },
  { title: '金额', dataIndex: 'amount', width: 100, align: 'right' },
  { title: '备注', dataIndex: 'remark', width: 140 },
]

// ═══ 查询参数 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    current: pagination.current,
    size: pagination.pageSize,
  }
  if (searchForm.taskNo) params.taskNo = searchForm.taskNo.trim()
  if (searchForm.orderNo) params.orderNo = searchForm.orderNo.trim()
  if (searchForm.customerId != null) params.customerId = searchForm.customerId
  if (searchForm.riderId != null) params.riderId = searchForm.riderId
  if (searchForm.vehicleId != null) params.vehicleId = searchForm.vehicleId
  if (searchForm.statusList?.length) params.statusList = searchForm.statusList.join(',')
  if (searchForm.priority != null) params.priority = searchForm.priority
  if (searchForm.orderType != null) params.orderType = searchForm.orderType
  if (searchForm.routeId != null) params.routeId = searchForm.routeId
  if (searchForm.routeArea) params.routeArea = searchForm.routeArea.trim()
  if (searchForm.unassigned != null) params.unassigned = searchForm.unassigned
  if (searchForm.abnormal != null) params.abnormal = searchForm.abnormal
  if (deliveryDateRange.value?.length === 2) {
    params.deliveryDateStart = deliveryDateRange.value[0]
    params.deliveryDateEnd = deliveryDateRange.value[1]
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
    const res: any = await taskApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[调度任务] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  clearSelection()
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.taskNo = ''
  searchForm.orderNo = ''
  searchForm.customerId = undefined
  searchForm.customerName = ''
  searchForm.riderId = undefined
  searchForm.vehicleId = undefined
  searchForm.statusList = []
  searchForm.priority = undefined
  searchForm.orderType = undefined
  searchForm.routeId = undefined
  searchForm.routeArea = ''
  searchForm.unassigned = undefined
  searchForm.abnormal = undefined
  deliveryDateRange.value = undefined
  sortState.field = undefined
  sortState.order = undefined
  pagination.current = 1
  clearSelection()
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  clearSelection()
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleSortChange(key: string | null, order: string | null) {
  clearSelection()
  sortState.field = key || undefined
  sortState.order = order || undefined
  pagination.current = 1
  fetchList()
}

function handleSelectionChange(rows: DmsTask[]) {
  selectedRows.value = (rows || []).filter(r => !(r as any).__ghost)
}

// ═══ 客户选择 ═══
const customerPickerVisible = ref(false)

function handleCustomerSelected(record: any) {
  searchForm.customerId = record?.id == null ? undefined : String(record.id)
  searchForm.customerName = record?.name || record?.partnerName || record?.customerName || ''
  customerPickerVisible.value = false
  handleSearch()
}

function clearCustomer() {
  searchForm.customerId = undefined
  searchForm.customerName = ''
  handleSearch()
}

// ═══ 调度结果报告（自动调度 / 批量操作 / 超时升级统一出口） ═══
const reportVisible = ref(false)
const reportTitle = ref('调度结果')
const report = reactive<{ total: number; success: number; failed: number; rows: any[] }>({
  total: 0, success: 0, failed: 0, rows: [],
})

function showReport(title: string, result?: BatchResultVO | null) {
  reportTitle.value = title
  report.total = Number(result?.total) || 0
  report.success = Number(result?.success) || 0
  report.failed = Number(result?.failed) || 0
  report.rows = (result?.items || []).map((item, index) => ({
    rowKey: `${item.taskId || index}-${index}`,
    taskNo: item.taskNo || item.taskId || '-',
    reason: item.reason || '-',
  }))
  reportVisible.value = true
}

// ═══ 自动调度（逐单结果报告） ═══
const autoDispatching = ref(false)

function handleAutoDispatch() {
  Modal.confirm({
    title: '自动调度',
    content: '对所有待分配任务执行自动调度？将按「智能调度」中配置的策略与约束派单。',
    okText: '执行',
    onOk: async () => {
      autoDispatching.value = true
      try {
        const res: any = await dispatchApi.autoDispatch(false)
        const data = res?.data ?? res ?? {}
        showReport('自动调度结果', {
          total: data.taskCount || 0,
          success: data.assignableCount || 0,
          failed: data.unassignableCount || 0,
          items: (data.rows || [])
            .filter((r: any) => !r.assignable)
            .map((r: any) => ({ taskId: r.taskId, taskNo: r.taskNo, reason: r.failReason || '未指派' })),
        })
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '自动调度失败')
      } finally {
        autoDispatching.value = false
      }
    },
  })
}

// ═══ 超时升级扫描 ═══
const escalating = ref(false)

function handleEscalate() {
  Modal.confirm({
    title: '超时升级',
    content: '扫描超时任务：超时未接单的自动重派，已超时的在途任务记录告警待人工介入？',
    okText: '执行',
    onOk: async () => {
      escalating.value = true
      try {
        const res: any = await dispatchApi.escalateOverdue()
        showReport('超时升级结果', res?.data ?? res)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '超时升级扫描失败')
      } finally {
        escalating.value = false
      }
    },
  })
}

// ═══ 指派 / 改派 ═══
const assignModalVisible = ref(false)
const assignMode = ref<'assign' | 'reassign'>('assign')
const currentTask = ref<DmsTask | null>(null)
const candidates = ref<DispatchCandidate[]>([])
const candidatesLoading = ref(false)
const assignSubmitting = ref(false)
const selectedRiderId = ref<string | null>(null)
const assignReason = ref('')
const candidateFilter = ref<'assignable' | 'all'>('assignable')
const candidateSort = ref<'score' | 'distance' | 'load' | 'rating'>('score')
const candidateKeyword = ref('')

const CANDIDATE_FILTER_OPTIONS = [
  { label: '仅可派', value: 'assignable' },
  { label: '全部配送员', value: 'all' },
]
const CANDIDATE_SORT_OPTIONS = [
  { label: '综合得分优先', value: 'score' },
  { label: '距离最近优先', value: 'distance' },
  { label: '负载最少优先', value: 'load' },
  { label: '评分最高优先', value: 'rating' },
]

const selectedRiderKeys = computed(() => (selectedRiderId.value ? [selectedRiderId.value] : []))

/** 不可派的候选禁用勾选，避免提交后才被后端拒绝 */
function candidateCheckboxProps(record: DispatchCandidate) {
  return { disabled: !record.eligible }
}

const filteredCandidates = computed(() => {
  let list = [...candidates.value]
  if (candidateFilter.value === 'assignable') {
    list = list.filter(c => c.eligible)
  }
  const kw = candidateKeyword.value.trim()
  if (kw) {
    list = list.filter(c =>
      (c.riderName || '').includes(kw) || (c.riderPhone || '').includes(kw) || (c.vehicleNo || '').includes(kw))
  }
  const num = (v: any) => (v == null || Number.isNaN(Number(v)) ? Number.MAX_SAFE_INTEGER : Number(v))
  const desc = (v: any) => (v == null || Number.isNaN(Number(v)) ? -1 : Number(v))
  switch (candidateSort.value) {
    case 'distance':
      list.sort((a, b) => num(a.distanceMeters) - num(b.distanceMeters))
      break
    case 'load':
      list.sort((a, b) => num(a.activeTasks) - num(b.activeTasks))
      break
    case 'rating':
      list.sort((a, b) => desc(b.ratingScore) - desc(a.ratingScore))
      break
    default:
      list.sort((a, b) => desc(b.score) - desc(a.score))
  }
  return list
})

async function openAssignModal(record: DmsTask, mode: 'assign' | 'reassign') {
  assignMode.value = mode
  currentTask.value = record
  selectedRiderId.value = null
  assignReason.value = ''
  candidateKeyword.value = ''
  candidateFilter.value = 'assignable'
  candidateSort.value = 'score'
  candidates.value = []
  assignModalVisible.value = true
  await loadCandidates(record.id as string)
}

async function loadCandidates(taskId: number | string) {
  candidatesLoading.value = true
  try {
    const res: any = await dispatchApi.candidates(taskId)
    const list: DispatchCandidate[] = Array.isArray(res) ? res : (res?.data ?? [])
    candidates.value = Array.isArray(list) ? list : []
    // 「无人可派」时不要停在空列表：自动展开全部配送员，让「为什么没人可派」的拒绝原因可见
    if (candidates.value.length > 0 && !candidates.value.some(c => c.eligible)) {
      candidateFilter.value = 'all'
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || '候选配送员加载失败')
  } finally {
    candidatesLoading.value = false
  }
}

function handleCandidateSelect(keys: (string | number)[]) {
  // 不可 Number()：雪花 ID 超出 MAX_SAFE_INTEGER 会被改写
  selectedRiderId.value = keys.length ? String(keys[0]) : null
}

async function handleAssignConfirm() {
  if (!currentTask.value || !selectedRiderId.value) {
    message.warning('请选择配送员')
    return
  }
  assignSubmitting.value = true
  try {
    if (assignMode.value === 'assign') {
      await taskApi.assign(currentTask.value.id as string, selectedRiderId.value, assignReason.value || undefined)
      message.success(`任务 ${currentTask.value.taskNo} 已指派`)
    } else {
      await taskApi.reassign(
        currentTask.value.id as string,
        selectedRiderId.value,
        currentTask.value.riderId,
        assignReason.value || undefined,
      )
      message.success(`任务 ${currentTask.value.taskNo} 已改派`)
    }
    assignModalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '指派失败')
  } finally {
    assignSubmitting.value = false
  }
}

// ═══ 取消（单条 / 批量） ═══
const cancelModalVisible = ref(false)
const cancelTargets = ref<DmsTask[]>([])
const cancelReason = ref('')
const batchSubmitting = ref(false)

function openCancelModal(record: DmsTask) {
  cancelTargets.value = [record]
  cancelReason.value = ''
  cancelModalVisible.value = true
}

function openBatchCancel(rows: DmsTask[]) {
  const targets = rows.filter(r => !(r as any).__ghost)
  if (!targets.length) {
    message.warning('请先勾选任务')
    return
  }
  cancelTargets.value = targets
  cancelReason.value = ''
  cancelModalVisible.value = true
}

async function handleCancelConfirm() {
  if (!cancelTargets.value.length) return
  batchSubmitting.value = true
  try {
    if (cancelTargets.value.length === 1) {
      await taskApi.cancel(cancelTargets.value[0].id as string, cancelReason.value || undefined)
      message.success(`任务 ${cancelTargets.value[0].taskNo} 已取消`)
      cancelModalVisible.value = false
      clearSelection()
    } else {
      const res: any = await taskApi.batchCancel(
        cancelTargets.value.map(r => r.id).filter(Boolean) as Array<number | string>,
        cancelReason.value || undefined,
      )
      cancelModalVisible.value = false
      clearSelection()
      showReport('批量取消结果', res?.data ?? res)
    }
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '取消失败')
  } finally {
    batchSubmitting.value = false
  }
}

// ═══ 批量指派 ═══
const batchAssignVisible = ref(false)
const batchRows = ref<DmsTask[]>([])
const batchRiderId = ref<string | undefined>(undefined)
const batchReason = ref('')

function openBatchAssign(rows: DmsTask[]) {
  const targets = rows.filter(r => !(r as any).__ghost)
  if (!targets.length) {
    message.warning('请先勾选任务')
    return
  }
  batchRows.value = targets
  batchRiderId.value = undefined
  batchReason.value = ''
  batchAssignVisible.value = true
}

async function handleBatchAssign() {
  if (!batchRiderId.value) {
    message.warning('请选择配送员')
    return
  }
  batchSubmitting.value = true
  try {
    const res: any = await taskApi.batchAssign(
      batchRows.value.map(r => r.id).filter(Boolean) as Array<number | string>,
      batchRiderId.value,
      batchReason.value || undefined,
    )
    batchAssignVisible.value = false
    showReport('批量指派结果', res?.data ?? res)
    clearSelection()
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '批量指派失败')
  } finally {
    batchSubmitting.value = false
  }
}

// ═══ 标记异常 ═══
function handleMarkException(record: DmsTask) {
  let reason = ''
  Modal.confirm({
    title: '标记异常',
    content: () =>
      h('div', [
        h('p', `确定将任务「${record.taskNo}」标记为异常？标记后需人工介入（重新指派 / 取消）。`),
        h('input', {
          class: 'ant-input ant-input-sm',
          placeholder: '异常原因（选填）',
          style: 'margin-top:8px',
          onInput: (e: any) => { reason = e.target.value },
        }),
      ]),
    okText: '标记异常',
    okType: 'danger',
    onOk: async () => {
      try {
        await taskApi.markException(record.id as string, reason || undefined)
        message.success(`任务 ${record.taskNo} 已标记异常`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '标记异常失败')
      }
    },
  })
}

// ═══ 行级「更多」 ═══
function handleMoreAction(key: string, record: DmsTask) {
  if (key === 'exception') handleMarkException(record)
  else if (key === 'logs') openLogs(record)
  else if (key === 'detail') openDetail(record)
  else if (key === 'print') openPrint(record)
}

function goTracking(record: DmsTask) {
  router.push({ path: '/dms/realtime-tracking', query: { riderId: record.riderId as any } })
}

// ═══ 调度审计时间线 ═══
const logsVisible = ref(false)
const logsLoading = ref(false)
const logsTaskNo = ref('')
const logRows = ref<any[]>([])

async function openLogs(record: DmsTask) {
  logsTaskNo.value = record.taskNo
  logsVisible.value = true
  logsLoading.value = true
  logRows.value = []
  try {
    const res: any = await taskApi.logs(record.id as string)
    const list: any[] = Array.isArray(res) ? res : (res?.data ?? [])
    logRows.value = (Array.isArray(list) ? list : []).map((item, index) => ({
      ...item,
      rowKey: `${item.id || index}-${index}`,
    }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '调度审计加载失败')
  } finally {
    logsLoading.value = false
  }
}

function riderChangeText(record: any): string {
  const from = record.fromRiderName || (record.fromRiderId ? `#${record.fromRiderId}` : '')
  const to = record.toRiderName || (record.toRiderId ? `#${record.toRiderId}` : '')
  if (!from && !to) return '-'
  if (from && to) return `${from} → ${to}`
  return from ? `${from} → 空` : `空 → ${to}`
}

// ═══ 单据明细 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailTaskNo = ref('')
const detailRows = ref<any[]>([])

async function openDetail(record: DmsTask) {
  detailTaskNo.value = record.taskNo
  detailVisible.value = true
  detailLoading.value = true
  detailRows.value = []
  try {
    const res: any = await taskApi.detail(record.id as string)
    const data = res?.data ?? res ?? {}
    detailRows.value = (data.items || []).map((item: any, index: number) => ({
      ...item,
      lineKey: `${item.id || index}-${index}`,
    }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '单据明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 地图派单（复用 RouteMapCanvas，不另造地图） ═══
const mapModalVisible = ref(false)
const mapLoading = ref(false)
const mapTasks = ref<DmsTask[]>([])
const mapCandidates = ref<DispatchCandidate[]>([])
const mapTaskId = ref<string | undefined>(undefined)
const mapSelectedTask = ref<DmsTask | null>(null)

const mapTaskOptions = computed(() => mapTasks.value.filter(t => t.id != null).map(t => ({
  label: `${t.taskNo}${t.customerName ? '（' + t.customerName + '）' : ''}`,
  value: String(t.id),
})))

const mapMarkers = computed(() => {
  const markers: Array<{ key: string; lat: number; lng: number; label: string; type: string }> = []
  for (const t of mapTasks.value) {
    if (t.customerLat == null || t.customerLng == null) continue
    markers.push({
      key: `task-${t.id}`,
      lat: Number(t.customerLat),
      lng: Number(t.customerLng),
      label: `任务 ${t.taskNo}`,
      type: 'destination',
    })
  }
  for (const c of mapCandidates.value) {
    if (c.currentLat == null || c.currentLng == null) continue
    markers.push({
      key: `rider-${c.riderId}`,
      lat: Number(c.currentLat),
      lng: Number(c.currentLng),
      label: `配送员 ${c.riderName || c.riderId}`,
      type: 'rider',
    })
  }
  return markers
})

async function openMapDispatch(task?: DmsTask) {
  mapModalVisible.value = true
  mapSelectedTask.value = task || null
  mapTaskId.value = task?.id == null ? undefined : String(task.id)
  mapCandidates.value = []
  await loadMapData()
  if (task?.id != null) await loadCandidates(task.id)
}

async function loadMapData() {
  mapLoading.value = true
  try {
    const res: any = await taskApi.page({ current: 1, size: 100, statusList: [0] })
    mapTasks.value = res?.records || []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '待分配任务加载失败')
  } finally {
    mapLoading.value = false
  }
}

async function handleMapTaskChange(taskId: string) {
  const task = mapTasks.value.find(t => String(t.id) === String(taskId)) || null
  mapSelectedTask.value = task
  mapCandidates.value = []
  if (task?.id != null) {
    await loadCandidates(task.id)
    mapCandidates.value = candidates.value
  }
}

function handleMapMarkerClick(marker: { key?: string }) {
  const key = marker?.key || ''
  if (key.startsWith('task-')) {
    const taskId = key.substring(5)
    mapTaskId.value = taskId
    handleMapTaskChange(taskId)
  } else if (key.startsWith('rider-') && mapSelectedTask.value) {
    const riderId = key.substring(6)
    const candidate = mapCandidates.value.find(c => String(c.riderId) === riderId)
    if (candidate) assignFromMap(candidate)
  }
}

async function assignFromMap(candidate: DispatchCandidate) {
  if (!mapSelectedTask.value) {
    message.warning('请先在图上点选待分配任务')
    return
  }
  const task = mapSelectedTask.value
  try {
    await taskApi.assign(task.id as string, candidate.riderId, '地图派单')
    message.success(`任务 ${task.taskNo} 已指派给 ${candidate.riderName || candidate.riderId}`)
    mapModalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '地图派单失败')
  }
}

// ═══ 打印（单张 / 批量） ═══
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})
const printQueue = ref<DmsTask[]>([])
const batchPrintVisible = ref(false)
const printScope = ref<string[]>(['bill'])
const onlyUnprinted = ref(false)

async function openPrint(record: DmsTask) {
  printData.value = { ...record, statusText: statusMeta(record.status).label }
  await nextTick()
  printDialogRef.value?.open()
}

function handlePrint() {
  if (!selectedRows.value.length) {
    message.warning('请先勾选要打印的配送单')
    return
  }
  printQueue.value = []
  openPrint(selectedRows.value[0])
}

function openBatchPrint(rows: DmsTask[]) {
  const targets = rows.filter(r => !(r as any).__ghost)
  if (!targets.length) {
    message.warning('请先勾选要打印的配送单')
    return
  }
  batchPrintVisible.value = true
}

async function confirmBatchPrint() {
  batchPrintVisible.value = false
  const list = onlyUnprinted.value ? selectedRows.value.filter(r => !Number(r.printCount)) : selectedRows.value
  if (!list.length) {
    message.warning('没有符合打印条件的配送单（均已打印过）')
    return
  }
  const [first, ...rest] = list
  printQueue.value = rest
  await openPrint(first)
}

async function handlePrintSuccess() {
  const id = printData.value?.id
  if (id) {
    try {
      await taskApi.print(String(id))
    } catch (e) {
      console.warn('[调度任务] 打印次数回写失败', e)
    }
  }
  const next = printQueue.value.shift()
  if (next) {
    await openPrint(next)
  } else {
    fetchList()
  }
}

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
    delete params.current
    delete params.size
    const blob: any = await taskApi.exportExcel(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `调度任务_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 调度策略配置（复用《智能调度》策略页，不重复造配置台） ═══
function goStrategyConfig() {
  router.push('/dms/dispatch')
}

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskNo', label: '任务编号', visible: true },
  { key: 'orderNo', label: '订单号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'riderId', label: '配送员', visible: true },
  { key: 'vehicleId', label: '配送车辆', visible: true },
  { key: 'statusList', label: '状态', visible: true },
  { key: 'priority', label: '优先级', visible: true },
  { key: 'orderType', label: '类型', visible: true },
  { key: 'deliveryDate', label: '配送日期', visible: true },
  { key: 'routeId', label: '配送线路', visible: true },
  { key: 'routeArea', label: '配送区域', visible: true },
  { key: 'unassigned', label: '有无配送员', visible: true },
  { key: 'abnormal', label: '是否异常', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'autoDispatch', label: '自动调度', enabled: true },
  { key: 'batchAssign', label: '批量指派', enabled: true },
  { key: 'batchCancel', label: '批量取消', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
  { key: 'mapDispatch', label: '地图派单', enabled: true },
  { key: 'escalate', label: '超时升级', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'strategyConfig', label: '调度策略配置', enabled: true },
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
  console.error('[调度任务] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
const firstActivate = ref(true)

onMounted(() => {
  if (initialQuery.value.status != null) searchForm.statusList = [initialQuery.value.status]
  if (initialQuery.value.riderId != null) searchForm.riderId = initialQuery.value.riderId
  loadResourceOptions()
  loadRouteOptions()
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

.assign-task-info { margin-bottom: 10px; color: #595959; font-size: 13px; }
.assign-toolbar { margin-bottom: 10px; }
.assign-footer-tip { margin-top: 10px; font-size: 12px; color: #fa8c16; line-height: 1.6; }
.assign-reason { margin-top: 10px; display: flex; align-items: center; gap: 8px; }
.reason-text { font-size: 12px; color: #d46b08; }

.modal-tip { font-size: 12px; color: #fa8c16; line-height: 1.6; padding-left: 4px; }
.report-summary { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }

.map-toolbar-line { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.map-candidate-list { margin-top: 10px; }
.map-candidate-title { font-size: 13px; color: #666; margin-bottom: 6px; }

.batch-print-body { display: flex; flex-direction: column; gap: 10px; }
.bp-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.bp-label { color: #666; width: 70px; text-align: right; }
.bp-muted { color: #999; font-size: 12px; }

/* 橙色主按钮（配送模块统一新增/主操作色） */
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
</style>
