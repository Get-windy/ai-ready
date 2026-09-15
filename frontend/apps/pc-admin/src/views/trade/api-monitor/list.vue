<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        API监控（配送 → API监控，菜单 90107 `trade:api-monitor`）
        · 定位：外部接口（开放接口 / 渠道）的**监控与联调自检**看板
        · 骨架：CategoryListLayout（三 Tab）+ BillTableList（表头齿轮列配置，逐 Tab 独立 storage-key）+ PageConfigPanel
        · 数据：全部后端真实聚合（api_access_log / inventory_sync_record / dms_config / external_channel_config），无桩无模拟
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="activeTab !== 'alerts'"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：自动刷新 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-switch
              v-model:checked="autoRefreshOn"
              size="small"
              @change="handleAutoRefreshChange"
            />
            <span class="toolbar-tip">
              自动刷新{{ autoRefreshOn ? `（${refreshSeconds}s）` : '' }}
            </span>
            <span
              v-if="lastRefreshAt"
              class="toolbar-tip"
            >最近刷新 {{ lastRefreshAt }}</span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新(F5) / 导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              title="页面配置"
              placement="bottom"
              :mouse-enter-delay="0.4"
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
              @click="refreshAll()"
            >
              <ReloadOutlined /> 刷新(F5)
            </a-button>
            <a-tooltip
              :title="activeTab === 'alerts' ? '异常告警为实时派生数据，无导出' : '导出当前 Tab（真实 xlsx）'"
              placement="bottom"
              :mouse-enter-delay="0.4"
            >
              <a-button
                v-if="isButtonEnabled('export')"
                size="small"
                :disabled="activeTab === 'alerts'"
                :loading="exporting"
                @click="handleExport"
              >
                <DownloadOutlined /> 导出
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 查询区（逐 Tab 独立条件，显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- Tab1 接口调用日志 -->
            <div
              v-if="activeTab === 'calls'"
              class="search-row"
            >
              <div
                v-if="isQueryVisible('calls.channelCode')"
                class="search-item"
              >
                <span class="search-label">渠道</span>
                <a-input
                  v-model:value="callQuery.channelCode"
                  placeholder="如 TAOBAO"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('calls.apiPath')"
                class="search-item"
              >
                <span class="search-label">接口</span>
                <a-select
                  v-model:value="callQuery.apiPath"
                  placeholder="全部接口"
                  size="small"
                  style="width: 210px"
                  allow-clear
                  :options="apiPathOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('calls.direction')"
                class="search-item"
              >
                <span class="search-label">方向</span>
                <a-select
                  v-model:value="callQuery.direction"
                  placeholder="全部"
                  size="small"
                  style="width: 110px"
                  allow-clear
                  :options="DIRECTION_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('calls.status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="callQuery.status"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('calls.keyword')"
                class="search-item"
              >
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="callQuery.keyword"
                  placeholder="请求号 / 错误信息"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('calls.timeRange')"
                class="search-item"
              >
                <span class="search-label">调用时间</span>
                <a-range-picker
                  v-model:value="callTimeRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
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

            <!-- Tab2 库存同步记录 -->
            <div
              v-else-if="activeTab === 'sync'"
              class="search-row"
            >
              <div
                v-if="isQueryVisible('sync.channelCode')"
                class="search-item"
              >
                <span class="search-label">渠道</span>
                <a-input
                  v-model:value="syncQuery.channelCode"
                  placeholder="如 TAOBAO"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sync.skuCode')"
                class="search-item"
              >
                <span class="search-label">SKU编码</span>
                <a-input
                  v-model:value="syncQuery.skuCode"
                  placeholder="请输入SKU编码"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sync.syncType')"
                class="search-item"
              >
                <span class="search-label">类型</span>
                <a-select
                  v-model:value="syncQuery.syncType"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="SYNC_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sync.syncStatus')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="syncQuery.syncStatus"
                  placeholder="全部"
                  size="small"
                  style="width: 100px"
                  allow-clear
                  :options="SYNC_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sync.errorCategory')"
                class="search-item"
              >
                <span class="search-label">失败分类</span>
                <a-select
                  v-model:value="syncQuery.errorCategory"
                  placeholder="全部"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="ERROR_CATEGORY_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('sync.timeRange')"
                class="search-item"
              >
                <span class="search-label">同步时间</span>
                <a-range-picker
                  v-model:value="syncTimeRange"
                  size="small"
                  show-time
                  value-format="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                  style="width: 320px"
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

            <!-- Tab3 异常告警 -->
            <div
              v-else
              class="search-row"
            >
              <span class="search-label">告警口径</span>
              <span class="toolbar-tip">
                阈值取自配置中心（配送参数 → API监控）：
                错误率 &gt; {{ fmtPlain(thresholds.errorRatePercent, '%') }}、P95 &gt; {{ fmtPlain(thresholds.p95Ms, 'ms') }}、
                失败 &gt; {{ fmtPlain(thresholds.failCount, '次') }}、同步失败 &gt; {{ fmtPlain(thresholds.syncFailCount, '条') }}；
                静默期 {{ fmtPlain(thresholds.silenceMinutes, '分钟') }}
              </span>
              <a-button
                type="primary"
                size="small"
                @click="refreshAlerts"
              >
                重新判定
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 看板主体 ═══ -->
        <template #table>
          <div class="monitor-body">
            <!-- ── 一、统计卡片（全部后端真实聚合） ── -->
            <a-row :gutter="12">
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="API状态"
                    :value="0"
                    :formatter="() => healthStatus"
                    :value-style="{ color: healthStatus === 'UP' ? '#3f8600' : '#cf1322' }"
                  >
                    <template #suffix>
                      <a-tag :color="healthStatus === 'UP' ? 'success' : 'error'">
                        {{ healthStatus === 'UP' ? '正常' : '异常' }}
                      </a-tag>
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <!--
                    ⚠️ 展示串（如「-」）不能直接传给 a-statistic 的 value：
                    其内部按 /^(-?)(\d*)(\.(\d+))?$/ 解析，"-" 会被解析成「负号 + 空整数 → 0」渲染为 -0。
                    故字符串展示一律走 formatter（原样输出），数值型才传 value。
                  -->
                  <a-statistic
                    title="今日接口调用量"
                    :value="0"
                    :formatter="() => cardText.callCount"
                  >
                    <template #suffix>
                      <a-tooltip
                        :title="stats.callLogSource || '开放接口 / 渠道调用（api_access_log）'"
                        placement="bottom"
                      >
                        <InfoCircleOutlined class="kpi-help" />
                      </a-tooltip>
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="接口成功率"
                    :value="0"
                    :formatter="() => cardText.rate"
                    :value-style="{ color: successColor }"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="平均耗时"
                    :value="0"
                    :formatter="() => cardText.avgCost"
                  />
                </a-card>
              </a-col>
            </a-row>

            <a-row
              :gutter="12"
              style="margin-top: 12px"
            >
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="P95 耗时"
                    :value="0"
                    :formatter="() => cardText.p95Cost"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="今日失败次数"
                    :value="0"
                    :formatter="() => cardText.failCount"
                    :value-style="{ color: Number(stats.todayFailCount) > 0 ? '#cf1322' : undefined }"
                  />
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="待处理订单"
                    :value="pendingCount"
                    suffix="笔"
                  />
                  <a-button
                    type="link"
                    size="small"
                    class="kpi-link"
                    @click="goToExternalOrder"
                  >
                    查看
                  </a-button>
                </a-card>
              </a-col>
              <a-col :span="6">
                <a-card
                  size="small"
                  :bordered="false"
                  class="kpi-card"
                >
                  <a-statistic
                    title="库存同步失败数"
                    :value="Number(stats.syncFailedCount) || 0"
                    :value-style="{ color: Number(stats.syncFailedCount) > 0 ? '#cf1322' : undefined }"
                  >
                    <template #suffix>
                      <span
                        v-if="stats.syncFailedCount === null || stats.syncFailedCount === undefined"
                        class="kpi-help-text"
                      >（暂无同步记录）</span>
                    </template>
                  </a-statistic>
                </a-card>
              </a-col>
            </a-row>

            <!-- ── 二、依赖健康面板（逐依赖分级，非整站 UP/DOWN） ── -->
            <a-card
              size="small"
              class="panel-card"
              :bordered="false"
            >
              <template #title>
                依赖健康
                <span class="panel-sub">{{ depsHealthyText }}</span>
              </template>
              <template #extra>
                <a-button
                  type="link"
                  size="small"
                  :loading="depsLoading"
                  @click="loadDeps"
                >
                  重新探测
                </a-button>
              </template>
              <a-spin :spinning="depsLoading">
                <div class="dep-list">
                  <a-tooltip
                    v-for="dep in deps"
                    :key="dep.key"
                    placement="bottom"
                    :title="dep.detail + (dep.lastError ? `；最近错误：${dep.lastError}` : '')"
                  >
                    <div
                      class="dep-item"
                      :class="depClass(dep.status)"
                    >
                      <span class="dep-name">{{ dep.name }}</span>
                      <a-tag
                        :color="depColor(dep.status)"
                        class="dep-tag"
                      >
                        {{ depStatusText(dep.status) }}
                      </a-tag>
                      <span
                        v-if="dep.latencyMs != null"
                        class="dep-latency"
                      >{{ dep.latencyMs }}ms</span>
                    </div>
                  </a-tooltip>
                  <span
                    v-if="deps.length === 0 && !depsLoading"
                    class="toolbar-tip"
                  >暂无依赖项</span>
                </div>
              </a-spin>
            </a-card>

            <!-- ── 三、快速联调（分组 + 参数表单 + 发送 + 结果 + 历史） ── -->
            <a-card
              size="small"
              class="panel-card"
              :bordered="false"
            >
              <template #title>
                快速联调
                <span class="panel-sub">回环调用本实例真实开放接口，结果与耗时均为实测</span>
              </template>
              <template #extra>
                <a-button
                  type="link"
                  size="small"
                  @click="sandboxCollapsed = !sandboxCollapsed"
                >
                  {{ sandboxCollapsed ? '展开' : '收起' }}
                </a-button>
              </template>
              <div v-show="!sandboxCollapsed">
                <div class="sandbox-form">
                  <div class="sandbox-field">
                    <span class="search-label">接口</span>
                    <a-select
                      v-model:value="sandboxApiKey"
                      size="small"
                      style="width: 320px"
                      show-search
                      option-filter-prop="label"
                      :options="endpointOptions"
                      @change="handleEndpointChange"
                    />
                  </div>
                  <div
                    v-for="param in activeEndpointParams"
                    :key="param.name"
                    class="sandbox-field"
                  >
                    <span class="search-label">
                      {{ param.label }}
                      <span
                        v-if="param.required"
                        class="required-mark"
                      >*</span>
                    </span>
                    <a-textarea
                      v-if="param.in === 'body'"
                      v-model:value="sandboxParams[param.name]"
                      :rows="2"
                      size="small"
                      style="width: 420px"
                      :placeholder="param.sample || param.description"
                    />
                    <a-input
                      v-else
                      v-model:value="sandboxParams[param.name]"
                      size="small"
                      style="width: 200px"
                      :placeholder="param.sample || param.description"
                    />
                    <span class="param-in">{{ param.in === 'path' ? '路径' : param.in === 'body' ? '报文' : '查询' }}</span>
                  </div>
                  <a-space :size="8">
                    <a-button
                      type="primary"
                      size="small"
                      :loading="sandboxSending"
                      @click="handleSandboxSend"
                    >
                      发送
                    </a-button>
                    <a-button
                      size="small"
                      @click="resetSandboxParams"
                    >
                      重置参数
                    </a-button>
                  </a-space>
                </div>

                <div
                  v-if="sandboxResult"
                  class="sandbox-result"
                >
                  <a-descriptions
                    size="small"
                    bordered
                    :column="4"
                  >
                    <a-descriptions-item label="接口">
                      {{ sandboxResult.apiName || sandboxResult.apiKey }}
                    </a-descriptions-item>
                    <a-descriptions-item label="方法">
                      {{ sandboxResult.method }}
                    </a-descriptions-item>
                    <a-descriptions-item label="HTTP / 业务码">
                      <a-tag :color="sandboxResult.success ? 'success' : 'error'">
                        {{ sandboxResult.httpStatus || '-' }}
                        <template v-if="sandboxResult.bizCode != null">
                          / {{ sandboxResult.bizCode }}
                        </template>
                      </a-tag>
                    </a-descriptions-item>
                    <a-descriptions-item label="耗时">
                      {{ sandboxResult.costMs }} ms
                    </a-descriptions-item>
                    <a-descriptions-item
                      label="请求号"
                      :span="2"
                    >
                      <span class="mono">{{ sandboxResult.requestId }}</span>
                    </a-descriptions-item>
                    <a-descriptions-item
                      label="请求地址"
                      :span="2"
                    >
                      <span class="mono">{{ sandboxResult.url }}</span>
                    </a-descriptions-item>
                  </a-descriptions>
                  <div
                    v-if="sandboxResult.errorMsg"
                    class="sandbox-error"
                  >
                    {{ sandboxResult.errorMsg }}
                  </div>
                  <pre class="json-view">{{ prettyBody }}</pre>
                </div>

                <div class="sandbox-history">
                  <div class="history-title">
                    联调历史（最近 5 次）
                    <a-button
                      type="link"
                      size="small"
                      @click="goToSandboxLogs"
                    >
                      查看全部
                    </a-button>
                  </div>
                  <a-table
                    size="small"
                    :columns="HISTORY_COLUMNS"
                    :data-source="sandboxHistory"
                    :pagination="false"
                    row-key="id"
                    :locale="{ emptyText: '暂无联调记录' }"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'accessTime'">
                        {{ fmtDateTime(record.accessTime) }}
                      </template>
                      <template v-if="column.key === 'status'">
                        <a-tag :color="record.status === 'SUCCESS' ? 'success' : 'error'">
                          {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
                        </a-tag>
                      </template>
                      <template v-if="column.key === 'cost'">
                        {{ record.responseTime == null ? '-' : `${record.responseTime} ms` }}
                      </template>
                    </template>
                  </a-table>
                </div>
              </div>
            </a-card>

            <!-- ── 四、明细台账（Tab 驱动，表头齿轮列配置） ── -->
            <div class="table-area">
              <BillTableList
                :columns="currentColumns"
                :data-source="activeTab === 'alerts' ? alerts : tableData"
                :loading="loading"
                :pagination="false as any"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                :selectable="false"
                :storage-key="tableStorageKey"
                :global-config-key="tableStorageKey"
                row-key="id"
              >
                <template #callTimeCell="{ record }">
                  <span>{{ fmtDateTime(record.accessTime) }}</span>
                </template>
                <template #directionCell="{ record }">
                  <a-tag :color="DIRECTION_MAP[record.direction]?.color || 'default'">
                    {{ DIRECTION_MAP[record.direction]?.label || record.direction || '-' }}
                  </a-tag>
                </template>
                <template #apiCell="{ record }">
                  <div class="cell-strong">
                    {{ record.apiName || record.apiPath || '-' }}
                  </div>
                  <div class="sub-text">
                    {{ record.apiPath }}
                  </div>
                </template>
                <template #statusCell="{ record }">
                  <a-tag :color="record.status === 'SUCCESS' ? 'success' : 'error'">
                    {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
                  </a-tag>
                </template>
                <template #costCell="{ record }">
                  <span :class="record.responseTime > slowThreshold ? 'cost-slow' : ''">
                    {{ record.responseTime == null ? '-' : `${record.responseTime} ms` }}
                  </span>
                </template>

                <template #syncStatusCell="{ record }">
                  <a-tag :color="SYNC_STATUS_MAP[record.syncStatus]?.color || 'default'">
                    {{ SYNC_STATUS_MAP[record.syncStatus]?.label || '-' }}
                  </a-tag>
                </template>
                <template #syncTypeCell="{ record }">
                  <a-tag :color="record.syncType === 'PUSH' ? 'blue' : 'green'">
                    {{ SYNC_TYPE_MAP[record.syncType] || record.syncType || '-' }}
                  </a-tag>
                </template>
                <template #categoryCell="{ record }">
                  <span v-if="record.errorCategory">{{ categoryLabel(record.errorCategory) }}</span>
                  <span v-else>-</span>
                </template>
                <template #syncTimeCell="{ record }">
                  <span>{{ fmtDateTime(record.syncTime) }}</span>
                </template>
                <template #retryTimeCell="{ record }">
                  <span>{{ record.lastRetryTime ? fmtDateTime(record.lastRetryTime) : '-' }}</span>
                </template>
                <template #syncActionCell="{ record }">
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.syncStatus === 1"
                    :loading="retryingId === record.id"
                    @click="handleRetry(record)"
                  >
                    重试
                  </a-button>
                </template>

                <template #alertLevelCell="{ record }">
                  <a-tag :color="record.level === 'CRITICAL' ? 'error' : 'warning'">
                    {{ record.level === 'CRITICAL' ? '严重' : '警告' }}
                  </a-tag>
                </template>
                <template #alertTypeCell="{ record }">
                  <span>{{ ALERT_TYPE_MAP[record.alertType] || record.alertType }}</span>
                </template>
                <template #silenceCell="{ record }">
                  <a-tag :color="record.silenced ? 'default' : 'processing'">
                    {{ record.silenced ? '静默期内' : '已外发事件' }}
                  </a-tag>
                </template>
              </BillTableList>
            </div>
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

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="trade-api-monitor-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, DownloadOutlined, SettingOutlined, InfoCircleOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import {
  apiMonitorApi, inventorySyncApi, externalOrderApi,
  type ApiCallLog, type ApiEndpoint, type ApiMonitorAlert, type DependencyHealth,
  type InventorySyncRecord, type SandboxResult
} from '@/api/trade'

defineOptions({ name: 'TradeApiMonitor' })

// ═══ 常量字典 ═══
const TABS = [
  { key: 'calls', label: '接口调用日志' },
  { key: 'sync', label: '库存同步记录' },
  { key: 'alerts', label: '异常告警' }
]

const DIRECTION_MAP: Record<string, { label: string; color: string }> = {
  IN: { label: '入站', color: 'blue' },
  OUT: { label: '出站', color: 'purple' },
  SANDBOX: { label: '联调', color: 'orange' }
}
const DIRECTION_OPTIONS = Object.entries(DIRECTION_MAP).map(([value, v]) => ({ value, label: v.label }))
const STATUS_OPTIONS = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAIL', label: '失败' }
]
const SYNC_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待同步', color: 'default' },
  1: { label: '成功', color: 'success' },
  2: { label: '失败', color: 'error' }
}
const SYNC_STATUS_OPTIONS = Object.entries(SYNC_STATUS_MAP).map(([value, v]) => ({ value: Number(value), label: v.label }))
const SYNC_TYPE_MAP: Record<string, string> = { PUSH: '推送', PULL: '拉取', QUERY: '查询' }
const SYNC_TYPE_OPTIONS = Object.entries(SYNC_TYPE_MAP).map(([value, label]) => ({ value, label }))
const ERROR_CATEGORY_MAP: Record<string, string> = {
  NETWORK: '网络异常', AUTH: '鉴权失败', PARAM: '参数错误',
  RATE_LIMIT: '渠道限流', BIZ_REJECT: '业务拒绝', UNKNOWN: '未知原因'
}
const ERROR_CATEGORY_OPTIONS = Object.entries(ERROR_CATEGORY_MAP).map(([value, label]) => ({ value, label }))
const ALERT_TYPE_MAP: Record<string, string> = {
  ERROR_RATE: '错误率', P95_LATENCY: 'P95 耗时', FAIL_COUNT: '失败次数',
  SYNC_FAILED: '库存同步失败', DEPENDENCY: '依赖异常'
}
const HISTORY_COLUMNS = [
  { title: '时间', key: 'accessTime', width: 170 },
  { title: '接口', dataIndex: 'apiName', key: 'apiName' },
  { title: '状态', key: 'status', width: 90 },
  { title: '耗时', key: 'cost', width: 100 },
  { title: '请求号', dataIndex: 'requestId', key: 'requestId', width: 230 }
]

// ═══ 状态 ═══
const router = useRouter()
const activeTab = ref('calls')
const loading = ref(false)
const exporting = ref(false)
const retryingId = ref<any>(null)
const lastRefreshAt = ref('')

const healthStatus = ref('UP')
const pendingCount = ref(0)
const stats = ref<Record<string, any>>({})
const thresholds = ref<Record<string, any>>({})

const deps = ref<DependencyHealth[]>([])
const depsLoading = ref(false)

const tableData = ref<any[]>([])
const alerts = ref<ApiMonitorAlert[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const callQuery = reactive<{ channelCode?: string; apiPath?: string; direction?: string; status?: string; keyword?: string }>({})
const syncQuery = reactive<{ channelCode?: string; skuCode?: string; syncType?: string; syncStatus?: number; errorCategory?: string }>({})
const callTimeRange = ref<[string, string] | undefined>()
const syncTimeRange = ref<[string, string] | undefined>()

// 联调面板
const sandboxCollapsed = ref(false)
const endpoints = ref<ApiEndpoint[]>([])
const sandboxApiKey = ref<string>('queryInventory')
const sandboxParams = reactive<Record<string, string>>({})
const sandboxSending = ref(false)
const sandboxResult = ref<SandboxResult | null>(null)
const sandboxHistory = ref<ApiCallLog[]>([])

// 自动刷新
const autoRefreshOn = ref(false)
const refreshSeconds = ref(30)
let autoTimer: any = null

// ═══ 计算属性 ═══
const successColor = computed(() => {
  const v = stats.value.successRate
  if (v === null || v === undefined) return undefined
  return Number(v) >= 99 ? '#3f8600' : Number(v) >= 90 ? '#d46b08' : '#cf1322'
})

/**
 * 卡片展示文本（预计算为字符串，经 a-statistic 的 formatter 原样渲染）
 *
 * ⚠️ 不能把字符串直接传给 a-statistic 的 value：其内部按 /^(-?)(\d*)(\.(\d+))?$/ 解析，
 * 「-」会被当成「负号 + 空整数 → 0」渲染成 `-0`（无数据卡片显示异常）。
 */
const cardText = computed(() => ({
  callCount: fmtCount(stats.value.todayCallCount),
  rate: fmtRate(stats.value.successRate),
  avgCost: fmtMs(stats.value.avgCostMs),
  p95Cost: fmtMs(stats.value.p95CostMs),
  failCount: fmtCount(stats.value.todayFailCount),
  syncFail: fmtCount(stats.value.syncFailedCount)
}))

/** 慢请求高亮阈值：取 P95 告警线（配置中心可改） */
const slowThreshold = computed(() => Number(thresholds.value.p95Ms) || 2000)

const endpointOptions = computed(() => {
  const groups = new Map<string, { label: string; options: { label: string; value: string }[] }>()
  for (const ep of endpoints.value) {
    if (!groups.has(ep.group)) {
      groups.set(ep.group, { label: ep.groupLabel, options: [] })
    }
    groups.get(ep.group)!.options.push({ label: `${ep.name}（${ep.method} ${ep.path}）`, value: ep.key })
  }
  return [...groups.values()]
})

const activeEndpoint = computed(() => endpoints.value.find(e => e.key === sandboxApiKey.value))
const activeEndpointParams = computed(() => activeEndpoint.value?.params || [])

const apiPathOptions = computed(() =>
  endpoints.value.map(ep => ({ label: `${ep.name}（${ep.path}）`, value: ep.path })))

const depsHealthyText = computed(() => {
  if (deps.value.length === 0) return ''
  const down = deps.value.filter(d => d.status === 'DOWN').length
  const notConfigured = deps.value.filter(d => d.status === 'NOT_CONFIGURED').length
  return `共 ${deps.value.length} 项 · 异常 ${down} · 未配置 ${notConfigured}`
})

const prettyBody = computed(() => {
  const body = sandboxResult.value?.responseBody
  if (!body) return '（无响应体）'
  try {
    return JSON.stringify(JSON.parse(body), null, 2)
  } catch (e) {
    return body
  }
})

const tableStorageKey = computed(() => {
  if (activeTab.value === 'calls') return 'trade-api-monitor-calls'
  if (activeTab.value === 'sync') return 'trade-api-monitor-sync'
  return 'trade-api-monitor-alerts'
})

// ═══ 列（★=默认显示；三 Tab 各一套） ═══
const callColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '调用时间', key: 'accessTime', type: 'slot', slotName: 'callTimeCell', width: 160 },
  { title: '方向', key: 'direction', type: 'slot', slotName: 'directionCell', width: 90 },
  { title: '渠道', key: 'channelCode', width: 110 },
  { title: '接口', key: 'apiName', type: 'slot', slotName: 'apiCell', width: 230 },
  { title: '方法', key: 'requestMethod', width: 80 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 90 },
  { title: '耗时', key: 'responseTime', type: 'slot', slotName: 'costCell', width: 100 },
  { title: '错误信息', key: 'errorMsg', width: 240 },
  { title: '响应码', key: 'responseCode', width: 90, defaultHidden: true },
  { title: '请求号', key: 'requestId', width: 220, defaultHidden: true },
  { title: '错误码', key: 'errorCode', width: 130, defaultHidden: true },
  { title: '调用方IP', key: 'ipAddress', width: 130, defaultHidden: true }
]

const syncColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'slot', slotName: 'syncActionCell', width: 80, fixed: 'left' },
  { title: '渠道', key: 'channelCode', width: 120 },
  { title: 'SKU编码', key: 'skuCode', width: 150 },
  { title: '同步数量', key: 'syncQty', width: 100 },
  { title: '类型', key: 'syncType', type: 'slot', slotName: 'syncTypeCell', width: 90 },
  { title: '状态', key: 'syncStatus', type: 'slot', slotName: 'syncStatusCell', width: 90 },
  { title: '失败分类', key: 'errorCategory', type: 'slot', slotName: 'categoryCell', width: 110 },
  { title: '错误信息', key: 'errorMsg', width: 240 },
  { title: '重试次数', key: 'retryCount', width: 90 },
  { title: '最近重试时间', key: 'lastRetryTime', type: 'slot', slotName: 'retryTimeCell', width: 160, defaultHidden: true },
  { title: '同步时间', key: 'syncTime', type: 'slot', slotName: 'syncTimeCell', width: 160 }
]

const alertColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '级别', key: 'level', type: 'slot', slotName: 'alertLevelCell', width: 90 },
  { title: '类型', key: 'alertType', type: 'slot', slotName: 'alertTypeCell', width: 130 },
  { title: '标题', key: 'title', width: 220 },
  { title: '明细', key: 'detail', width: 340 },
  { title: '当前值', key: 'currentValue', width: 100 },
  { title: '阈值', key: 'threshold', width: 100 },
  { title: '事件', key: 'silenced', type: 'slot', slotName: 'silenceCell', width: 110 },
  { title: '处置建议', key: 'suggestion', width: 300 }
]

const currentColumns = computed(() => {
  if (activeTab.value === 'calls') return callColumns
  if (activeTab.value === 'sync') return syncColumns
  return alertColumns
})

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'calls.channelCode', label: '调用日志·渠道', visible: true },
  { key: 'calls.apiPath', label: '调用日志·接口', visible: true },
  { key: 'calls.direction', label: '调用日志·方向', visible: true },
  { key: 'calls.status', label: '调用日志·状态', visible: true },
  { key: 'calls.keyword', label: '调用日志·关键字', visible: true },
  { key: 'calls.timeRange', label: '调用日志·调用时间', visible: true },
  { key: 'sync.channelCode', label: '同步记录·渠道', visible: true },
  { key: 'sync.skuCode', label: '同步记录·SKU编码', visible: true },
  { key: 'sync.syncType', label: '同步记录·类型', visible: true },
  { key: 'sync.syncStatus', label: '同步记录·状态', visible: true },
  { key: 'sync.errorCategory', label: '同步记录·失败分类', visible: true },
  { key: 'sync.timeRange', label: '同步记录·同步时间', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新(F5)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'autoRefresh', label: '自动刷新', enabled: true },
  { key: 'retrySync', label: '同步失败重试', enabled: true }
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

// ═══ 格式化 ═══
function fmtCount(v: any) {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}
function fmtRate(v: any) {
  return v === null || v === undefined || v === '' ? '-' : `${Number(v).toFixed(2)}%`
}
function fmtMs(v: any) {
  return v === null || v === undefined || v === '' ? '-' : `${v} ms`
}
function fmtPlain(v: any, unit: string) {
  return v === null || v === undefined || v === '' ? '-' : `${v}${unit}`
}
function fmtDateTime(val?: string | null) {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}
function categoryLabel(code: string) {
  return ERROR_CATEGORY_MAP[code] || code
}
function depStatusText(status: string) {
  if (status === 'UP') return '正常'
  if (status === 'DOWN') return '异常'
  return '未配置'
}
function depColor(status: string) {
  if (status === 'UP') return 'success'
  if (status === 'DOWN') return 'error'
  return 'default'
}
function depClass(status: string) {
  if (status === 'UP') return 'dep-up'
  if (status === 'DOWN') return 'dep-down'
  return 'dep-unknown'
}

// ═══ 数据加载 ═══
function extract<T>(res: any, fallback: T): T {
  const payload = res?.data?.data ?? res?.data ?? res
  return (payload === undefined || payload === null ? fallback : payload) as T
}

async function loadHealth() {
  try {
    const payload = extract<Record<string, any>>(await apiMonitorApi.health(), {})
    healthStatus.value = payload.status || 'UP'
  } catch (error) {
    healthStatus.value = 'DOWN'
    console.warn('[API监控] 健康检查失败', error)
  }
}

async function loadPendingCount() {
  try {
    const payload = extract<any>(await externalOrderApi.countPending(), 0)
    pendingCount.value = Number(payload) || 0
  } catch (error) {
    pendingCount.value = 0
    console.warn('[API监控] 待处理订单数加载失败', error)
  }
}

/** ⚠️ 统计必须在自身探针（健康/待处理）之后取，保证卡片与口径严格一致 */
async function loadStats() {
  try {
    stats.value = extract<Record<string, any>>(await apiMonitorApi.stat(), {})
  } catch (error) {
    stats.value = {}
    console.warn('[API监控] 统计加载失败', error)
  }
}

async function loadThresholds() {
  try {
    thresholds.value = extract<Record<string, any>>(await apiMonitorApi.thresholds(), {})
    const seconds = Number(thresholds.value.autoRefreshSeconds)
    refreshSeconds.value = seconds > 0 ? seconds : 30
  } catch (error) {
    console.warn('[API监控] 阈值加载失败', error)
  }
}

async function loadDeps() {
  depsLoading.value = true
  try {
    deps.value = extract<DependencyHealth[]>(await apiMonitorApi.deps(), [])
  } catch (error) {
    deps.value = []
    console.warn('[API监控] 依赖健康加载失败', error)
  } finally {
    depsLoading.value = false
  }
}

function buildCallQuery(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (callQuery.channelCode) params.channelCode = callQuery.channelCode.trim()
  if (callQuery.apiPath) params.apiPath = callQuery.apiPath
  if (callQuery.direction) params.direction = callQuery.direction
  if (callQuery.status) params.status = callQuery.status
  if (callQuery.keyword) params.keyword = callQuery.keyword.trim()
  if (callTimeRange.value?.length === 2) {
    params.startTime = callTimeRange.value[0]
    params.endTime = callTimeRange.value[1]
  }
  return params
}

function buildSyncQuery(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (syncQuery.channelCode) params.channelCode = syncQuery.channelCode.trim()
  if (syncQuery.skuCode) params.skuCode = syncQuery.skuCode.trim()
  if (syncQuery.syncType) params.syncType = syncQuery.syncType
  if (syncQuery.syncStatus != null) params.syncStatus = syncQuery.syncStatus
  if (syncQuery.errorCategory) params.errorCategory = syncQuery.errorCategory
  if (syncTimeRange.value?.length === 2) {
    params.startTime = syncTimeRange.value[0]
    params.endTime = syncTimeRange.value[1]
  }
  return params
}

async function loadTable() {
  if (activeTab.value === 'alerts') {
    await refreshAlerts()
    return
  }
  loading.value = true
  try {
    if (activeTab.value === 'calls') {
      const res: any = await apiMonitorApi.callsPage(buildCallQuery())
      const payload = extract<{ records?: ApiCallLog[]; total?: number }>(res, {})
      tableData.value = payload.records || []
      pagination.total = Number(payload.total) || 0
    } else {
      const res: any = await inventorySyncApi.page(buildSyncQuery())
      const payload = extract<{ records?: InventorySyncRecord[]; total?: number }>(res, {})
      tableData.value = payload.records || []
      pagination.total = Number(payload.total) || 0
    }
  } catch (error: any) {
    tableData.value = []
    pagination.total = 0
    console.error('[API监控] 台账加载失败', error)
    message.error(error?.response?.data?.message || '台账加载失败')
  } finally {
    loading.value = false
  }
}

async function refreshAlerts() {
  loading.value = true
  try {
    const list = extract<ApiMonitorAlert[]>(await apiMonitorApi.alerts(), [])
    // 告警是**派生数据**（无主键）：补稳定行键，避免表格 row-key 重复
    alerts.value = list.map((alert, index) => ({ ...alert, id: `${alert.alertType}-${index}` }))
  } catch (error: any) {
    alerts.value = []
    console.error('[API监控] 告警判定失败', error)
    message.error(error?.response?.data?.message || '告警判定失败')
  } finally {
    loading.value = false
  }
}

async function loadEndpoints() {
  try {
    endpoints.value = extract<ApiEndpoint[]>(await apiMonitorApi.endpoints(), [])
    if (!endpoints.value.some(e => e.key === sandboxApiKey.value)) {
      sandboxApiKey.value = endpoints.value[0]?.key || ''
    }
    resetSandboxParams()
  } catch (error) {
    endpoints.value = []
    console.warn('[API监控] 开放接口目录加载失败', error)
  }
}

/** 联调历史 = 调用日志中 direction=SANDBOX 的分页（同一张表，不单建历史表） */
async function loadSandboxHistory() {
  try {
    const payload = extract<{ records?: ApiCallLog[] }>(
      await apiMonitorApi.callsPage({ pageNum: 1, pageSize: 5, direction: 'SANDBOX' }), {})
    sandboxHistory.value = payload.records || []
  } catch (error) {
    sandboxHistory.value = []
    console.warn('[API监控] 联调历史加载失败', error)
  }
}

/** 全量刷新（统计放最后：保证「今日调用量」含页面自身探针调用） */
async function refreshAll() {
  await Promise.all([loadHealth(), loadPendingCount(), loadDeps(), loadThresholds()])
  await loadTable()
  await Promise.all([loadStats(), loadSandboxHistory()])
  lastRefreshAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  pagination.pageSize = 20
  tableData.value = []
  alerts.value = []
  loadTable()
}

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  if (activeTab.value === 'calls') {
    callQuery.channelCode = undefined
    callQuery.apiPath = undefined
    callQuery.direction = undefined
    callQuery.status = undefined
    callQuery.keyword = undefined
    callTimeRange.value = undefined
  } else if (activeTab.value === 'sync') {
    syncQuery.channelCode = undefined
    syncQuery.skuCode = undefined
    syncQuery.syncType = undefined
    syncQuery.syncStatus = undefined
    syncQuery.errorCategory = undefined
    syncTimeRange.value = undefined
  }
  pagination.current = 1
  loadTable()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadTable()
}

// ═══ 联调 ═══
function resetSandboxParams() {
  Object.keys(sandboxParams).forEach(key => delete sandboxParams[key])
  for (const param of activeEndpointParams.value) {
    sandboxParams[param.name] = param.sample || ''
  }
}

function handleEndpointChange() {
  sandboxResult.value = null
  resetSandboxParams()
}

async function handleSandboxSend() {
  const endpoint = activeEndpoint.value
  if (!endpoint) {
    message.warning('请先选择接口')
    return
  }
  const missing = endpoint.params
    .filter(p => p.required && !String(sandboxParams[p.name] || '').trim())
    .map(p => p.label)
  if (missing.length > 0) {
    message.warning(`请填写必填参数：${missing.join('、')}`)
    return
  }
  sandboxSending.value = true
  try {
    const params: Record<string, string> = {}
    for (const p of endpoint.params) {
      const value = String(sandboxParams[p.name] ?? '').trim()
      if (value) params[p.name] = value
    }
    const result = extract<SandboxResult | null>(
      await apiMonitorApi.sandboxInvoke({ apiKey: endpoint.key, params }), null)
    sandboxResult.value = result
    if (result?.success) {
      message.success(`调用成功（${result.costMs} ms）`)
    } else {
      message.warning(result?.errorMsg || '调用失败')
    }
    await Promise.all([loadSandboxHistory(), loadStats()])
  } catch (error: any) {
    message.error(error?.response?.data?.message || '联调调用失败')
  } finally {
    sandboxSending.value = false
  }
}

function goToSandboxLogs() {
  activeTab.value = 'calls'
  callQuery.direction = 'SANDBOX'
  pagination.current = 1
  loadTable()
}

// ═══ 同步失败重试 ═══
async function handleRetry(record: InventorySyncRecord) {
  if (!isButtonEnabled('retrySync')) {
    message.warning('「同步失败重试」按钮已在页面配置中禁用')
    return
  }
  retryingId.value = record.id
  try {
    const result = extract<Record<string, any> | null>(
      await apiMonitorApi.retrySync(record.id), null)
    if (result?.success) {
      message.success(result.message || '重试成功')
    } else {
      message.warning(result?.message || '重试失败')
    }
    await Promise.all([loadTable(), loadStats()])
  } catch (error: any) {
    message.error(error?.response?.data?.message || '重试失败')
  } finally {
    retryingId.value = null
  }
}

// ═══ 导出（真实 xlsx） ═══
async function handleExport() {
  if (activeTab.value === 'alerts') {
    message.info('异常告警为实时派生数据，无导出')
    return
  }
  exporting.value = true
  try {
    const blob: any = activeTab.value === 'calls'
      ? await apiMonitorApi.callsExport(buildCallQuery())
      : await inventorySyncApi.export(buildSyncQuery())
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${activeTab.value === 'calls' ? '接口调用日志' : '库存同步记录'}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 自动刷新 / F5 ═══
function handleAutoRefreshChange(checked: boolean | string | number) {
  const on = !!checked
  if (on && !isButtonEnabled('autoRefresh')) {
    message.warning('「自动刷新」已在页面配置中禁用')
    autoRefreshOn.value = false
    return
  }
  stopAutoRefresh()
  if (on) {
    autoTimer = setInterval(() => refreshAll(), Math.max(refreshSeconds.value, 5) * 1000)
    message.success(`已开启自动刷新（每 ${Math.max(refreshSeconds.value, 5)} 秒）`)
  }
}

function stopAutoRefresh() {
  if (autoTimer) {
    clearInterval(autoTimer)
    autoTimer = null
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'F5') {
    event.preventDefault()
    refreshAll()
  }
}

// ═══ 其他 ═══
function goToExternalOrder() {
  router.push('/trade/external-order')
}

function handleError(error: Error) {
  console.error('[API监控] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  refreshAll()
  loadEndpoints()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  stopAutoRefresh()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #8c8c8c; }
.required-mark { color: #cf1322; margin-left: 2px; }

.monitor-body { padding: 12px 16px 16px; }
.kpi-card { background: #fafafa; border-radius: 6px; }
.kpi-help { color: #8c8c8c; font-size: 12px; cursor: help; }
.kpi-help-text { font-size: 12px; color: #999; }
.kpi-link { position: absolute; top: 6px; right: 8px; padding: 0; }

.panel-card { margin-top: 12px; background: #fff; }
.panel-sub { font-size: 12px; color: #8c8c8c; margin-left: 8px; font-weight: 400; }

.dep-list { display: flex; flex-wrap: wrap; gap: 8px; }
.dep-item {
  display: flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 4px; border: 1px solid #f0f0f0; background: #fafafa;
}
.dep-up { border-color: #d9f7be; }
.dep-down { border-color: #ffccc7; background: #fff1f0; }
.dep-unknown { border-color: #e8e8e8; color: #8c8c8c; }
.dep-name { font-size: 13px; }
.dep-tag { margin: 0; }
.dep-latency { font-size: 12px; color: #8c8c8c; }

.sandbox-form { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.sandbox-field { display: flex; align-items: center; gap: 6px; }
.param-in { font-size: 12px; color: #bfbfbf; }
.sandbox-result { margin-top: 12px; }
.sandbox-error { margin-top: 8px; color: #cf1322; font-size: 13px; }
.json-view {
  margin-top: 8px; padding: 10px; max-height: 240px; overflow: auto;
  background: #f6f8fa; border: 1px solid #eee; border-radius: 4px;
  font-size: 12px; line-height: 1.6; white-space: pre-wrap; word-break: break-all;
}
.mono { font-family: 'Consolas', 'Monaco', monospace; font-size: 12px; }

.sandbox-history { margin-top: 12px; }
.history-title { font-size: 13px; color: #595959; margin-bottom: 6px; }

.table-area { margin-top: 12px; min-height: 380px; }
.cell-strong { font-size: 13px; }
.sub-text { font-size: 12px; color: #8c8c8c; }
.cost-slow { color: #cf1322; font-weight: 600; }

:deep(.ant-statistic-title) { font-size: 12px; }
:deep(.ant-statistic-content) { font-size: 18px; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
