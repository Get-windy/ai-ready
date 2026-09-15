<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        智能调度（配送 → 调度管理 → 智能调度，菜单 80850 `dms:dispatch`）
        · 定位（《智能调度开发文档》§3.1）：**调度策略与执行台** —— 策略配置 / 调度预览与执行 / 效果复盘
          【任务列表与手工指派入口复用《调度任务》，本页不重复做任务列表接口】
        · 骨架：CategoryListLayout（三 Tab）+ BillTableList（序号齿轮列配置）+ PageConfigPanel
        · 原实现为空壳：/dispatch/page 不存在、/rider/list 不存在、assign 参数 body≠@RequestParam、
          handleBatchCancel/handleView 为空壳函数 —— 本次全部消灭
      -->
      <CategoryListLayout
        :tabs="dispatchTabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="activeTab === 'execute'"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-space
            v-if="activeTab === 'execute'"
            :size="8"
          >
            <a-button
              v-if="isButtonEnabled('preview')"
              size="small"
              :loading="previewing"
              @click="handlePreview"
            >
              <EyeOutlined /> 自动调度预览
            </a-button>
            <a-button
              v-if="isButtonEnabled('run')"
              type="primary"
              size="small"
              class="btn-run"
              :disabled="previewResult.assignableCount === 0"
              :loading="running"
              @click="handleRun"
            >
              <ThunderboltOutlined /> 执行自动调度（{{ previewResult.assignableCount }} 单）
            </a-button>
            <span
              v-if="previewResult.rows.length"
              class="sel-tip"
            >策略：{{ previewResult.strategyText }} · 可派 {{ previewResult.assignableCount }} / 待分配 {{ previewResult.taskCount }}</span>
          </a-space>
          <a-space
            v-else-if="activeTab === 'strategy'"
            :size="8"
          >
            <a-button
              v-if="isButtonEnabled('saveStrategy')"
              type="primary"
              size="small"
              class="btn-run"
              :loading="savingStrategy"
              @click="handleSaveStrategy"
            >
              <SaveOutlined /> 保存策略
            </a-button>
            <a-button
              size="small"
              @click="loadStrategy"
            >
              <UndoOutlined /> 放弃修改
            </a-button>
          </a-space>
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
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（仅「调度执行」Tab 使用） ═══ -->
        <template #search-fields>
          <div
            v-if="activeTab === 'execute'"
            class="search-area"
          >
            <div class="search-row">
              <div
                v-if="isQueryVisible('taskNo')"
                class="search-item"
              >
                <span class="search-label">任务编号</span>
                <a-input
                  v-model:value="searchForm.taskNo"
                  placeholder="请输入任务编号"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('maxTasks')"
                class="search-item"
              >
                <span class="search-label">扫描上限</span>
                <a-input-number
                  v-model:value="searchForm.maxTasks"
                  :min="1"
                  :max="200"
                  :precision="0"
                  size="small"
                  style="width: 110px"
                  placeholder="默认 50"
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
              <span class="tip-text">待分配任务（status=待分配）；预览只算不落库，确认执行才指派</span>
            </div>
          </div>
        </template>

        <!-- ═══ 主体 ═══ -->
        <template #table>
          <div class="table-area">
            <!-- ── Tab1：策略配置 ── -->
            <div
              v-if="activeTab === 'strategy'"
              class="pane-scroll"
            >
              <div class="section-card">
                <div class="section-head">
                  <span class="section-title">派单策略</span>
                  <span class="section-tip">落配置中心（dms_config），保存即热生效；缺失时用代码默认值</span>
                </div>
                <a-form
                  layout="horizontal"
                  :label-col="{ span: 7 }"
                  :wrapper-col="{ span: 16 }"
                  :colon="false"
                  size="small"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="派单策略">
                        <a-select
                          v-model:value="strategyForm.strategy"
                          :options="STRATEGY_OPTIONS"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="超时升级阈值(分钟)">
                        <a-input-number
                          v-model:value="strategyForm.timeoutEscalateMinutes"
                          :min="0"
                          :precision="0"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="权重·距离">
                        <a-input-number
                          v-model:value="strategyForm.weightDistance"
                          :min="0"
                          :precision="1"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="权重·负载">
                        <a-input-number
                          v-model:value="strategyForm.weightLoad"
                          :min="0"
                          :precision="1"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="权重·评分">
                        <a-input-number
                          v-model:value="strategyForm.weightScore"
                          :min="0"
                          :precision="1"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </div>

              <div class="section-card">
                <div class="section-head">
                  <span class="section-title">配送员约束</span>
                  <span class="section-tip">不满足约束的候选会在预览里给出拒绝原因（无人可派时可见原因）</span>
                </div>
                <a-form
                  layout="horizontal"
                  :label-col="{ span: 7 }"
                  :wrapper-col="{ span: 16 }"
                  :colon="false"
                  size="small"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="单人在途上限(单)">
                        <a-input-number
                          v-model:value="strategyForm.maxConcurrent"
                          :min="0"
                          :precision="0"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="仅向空闲配送员派单">
                        <a-switch
                          :checked="strategyForm.requireOnline"
                          @change="(v: any) => (strategyForm.requireOnline = !!v)"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="单任务载重上限(kg)">
                        <a-input-number
                          v-model:value="strategyForm.maxLoadKg"
                          :min="0"
                          :precision="0"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="单任务容积上限(m³)">
                        <a-input-number
                          v-model:value="strategyForm.maxVolumeM3"
                          :min="0"
                          :precision="2"
                          style="width: 100%"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="区域分包严格模式">
                        <a-switch
                          :checked="strategyForm.areaStrict"
                          @change="(v: any) => (strategyForm.areaStrict = !!v)"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <div class="section-tip">
                    0 表示不限；已按「无资质不接单」强制校验证照（实名认证），该项不可关闭。
                    区域分包严格模式仅「区域分包」策略生效：开启后任务线路未绑定该配送员即不可派，关闭则绑定者优先。
                  </div>
                </a-form>
              </div>

              <div class="section-card">
                <div class="section-head">
                  <span class="section-title">区域分包绑定</span>
                  <a-space :size="8">
                    <a-input
                      v-model:value="bindingQuery.keyword"
                      placeholder="线路/配送员关键字"
                      size="small"
                      style="width: 180px"
                      allow-clear
                      @press-enter="handleBindingSearch"
                    />
                    <a-button
                      size="small"
                      @click="handleBindingSearch"
                    >
                      查询
                    </a-button>
                    <a-button
                      v-if="isButtonEnabled('addBinding')"
                      type="primary"
                      size="small"
                      class="btn-run"
                      @click="openBinding(null)"
                    >
                      <PlusOutlined /> 新增绑定
                    </a-button>
                  </a-space>
                </div>
                <div class="section-tip binding-tip">
                  按线路档案（资料 → 配送管理 → 线路）绑定固定配送员；一个线路可绑多人（按优先级），一名配送员可服务多条线路。
                  <span
                    v-if="strategyForm.strategy !== 'AREA'"
                    class="warn-inline"
                  >当前策略不是「区域分包」，绑定仅在切换到该策略后生效。</span>
                </div>
                <BillTableList
                  :columns="bindingColumns"
                  :data-source="bindings"
                  :loading="bindingLoading"
                  :pagination="bindingPagination"
                  :show-toolbar="false"
                  :show-search="false"
                  :show-add="false"
                  :show-export="false"
                  :show-batch-delete="false"
                  :selectable="false"
                  :min-empty-rows="5"
                  storage-key="dms-dispatch-area-binding-columns"
                  global-config-key="dms-dispatch-area-binding-columns"
                  row-key="id"
                  @page-change="handleBindingPageChange"
                >
                  <template #bindingActionCell="{ record }">
                    <a-space
                      v-if="record && !record.__ghost"
                      :size="0"
                    >
                      <a-button
                        type="link"
                        size="small"
                        @click="openBinding(record)"
                      >
                        编辑
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        @click="toggleBindingStatus(record)"
                      >
                        {{ record.status === 1 ? '停用' : '启用' }}
                      </a-button>
                      <a-popconfirm
                        title="确认删除该绑定？"
                        ok-text="删除"
                        cancel-text="取消"
                        @confirm="removeBinding(record)"
                      >
                        <a-button
                          type="link"
                          size="small"
                          danger
                        >
                          删除
                        </a-button>
                      </a-popconfirm>
                    </a-space>
                  </template>
                  <template #bindingStatusCell="{ record }">
                    <a-tag
                      v-if="record && !record.__ghost"
                      :color="record.status === 1 ? 'green' : 'default'"
                    >
                      {{ record.status === 1 ? '启用' : '停用' }}
                    </a-tag>
                  </template>
                </BillTableList>
              </div>
            </div>

            <!-- ── Tab2：调度执行 ── -->
            <template v-else-if="activeTab === 'execute'">
              <BillTableList
                :columns="taskColumns"
                :data-source="taskRows"
                :loading="loading"
                :pagination="false"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                :selectable="false"
                storage-key="dms-dispatch-columns"
                global-config-key="dms-dispatch-columns"
                row-key="id"
              >
                <template #taskCell="{ record }">
                  <span v-if="!record.__ghost">{{ record.taskNo || record.id }}</span>
                </template>

                <template #statusCell="{ record }">
                  <a-tag
                    v-if="!record.__ghost"
                    color="orange"
                  >
                    {{ record.statusText || '待分配' }}
                  </a-tag>
                </template>

                <template #previewRiderCell="{ record }">
                  <span v-if="!record.__ghost">
                    <template v-if="record.__preview?.riderName">
                      {{ record.__preview.riderName }}
                      <span class="sub-text">{{ fmtMeters(record.__preview.distanceMeters) }}</span>
                    </template>
                    <span
                      v-else-if="record.__preview"
                      class="fail-text"
                    >{{ record.__preview.failReason || '无人可派' }}</span>
                    <span
                      v-else
                      class="sub-text"
                    >未预览</span>
                  </span>
                </template>

                <template #ruleCell="{ record }">
                  <span
                    v-if="!record.__ghost"
                    class="rule-text"
                  >{{ record.__preview?.ruleHit || '-' }}</span>
                </template>

                <template #actionCell="{ record }">
                  <a-space
                    v-if="!record.__ghost"
                    :size="0"
                  >
                    <a-button
                      type="link"
                      size="small"
                      @click="openAssign(record)"
                    >
                      手动指派
                    </a-button>
                  </a-space>
                </template>
              </BillTableList>
            </template>

            <!-- ── Tab3：效果复盘 ── -->
            <div
              v-else
              class="pane-scroll"
            >
              <div class="stat-bar">
                <div class="stat-item">
                  <span class="stat-label">任务总数</span>
                  <span class="stat-value">{{ stat.taskTotal }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">待分配</span>
                  <span class="stat-value stat-warn">{{ stat.pendingCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">已指派</span>
                  <span class="stat-value">{{ stat.assignedCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">自动占比</span>
                  <span class="stat-value stat-ok">{{ fmtRate(stat.autoRate) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">平均派单耗时</span>
                  <span class="stat-value">{{ fmtSeconds(stat.avgDispatchSeconds) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">在途</span>
                  <span class="stat-value">{{ stat.activeCount }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">超时率</span>
                  <span class="stat-value stat-danger">{{ fmtRate(stat.overdueRate) }}</span>
                </div>
              </div>

              <div class="section-card">
                <div class="section-head">
                  <span class="section-title">调度方式分布</span>
                  <span class="section-tip">口径：自动(1) / 手工(2) / 抢单(3) / 竞价(4)；抢单与竞价归口《订单池》，本页只做占比统计</span>
                </div>
                <div class="dist-row">
                  <div class="dist-item">
                    <span class="dist-label">自动调度</span>
                    <a-progress
                      :percent="percent(stat.autoCount)"
                      :stroke-color="'#52c41a'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.autoCount }} 单</span>
                  </div>
                  <div class="dist-item">
                    <span class="dist-label">手工指派</span>
                    <a-progress
                      :percent="percent(stat.manualCount)"
                      :stroke-color="'#1890ff'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.manualCount }} 单</span>
                  </div>
                  <div class="dist-item">
                    <span class="dist-label">抢单</span>
                    <a-progress
                      :percent="percent(stat.grabCount)"
                      :stroke-color="'#faad14'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.grabCount }} 单（归口《订单池》）</span>
                  </div>
                  <div class="dist-item">
                    <span class="dist-label">竞价</span>
                    <a-progress
                      :percent="percent(stat.bidCount)"
                      :stroke-color="'#722ed1'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.bidCount }} 单（归口《订单池》）</span>
                  </div>
                  <div class="dist-item">
                    <span class="dist-label">其他方式</span>
                    <a-progress
                      :percent="percent(stat.otherCount)"
                      :stroke-color="'#d9d9d9'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.otherCount }} 单（未记方式的历史数据）</span>
                  </div>
                  <div class="dist-item">
                    <span class="dist-label">超时在途</span>
                    <a-progress
                      :percent="percent(stat.overdueCount)"
                      :stroke-color="'#ff4d4f'"
                      size="small"
                    />
                    <span class="dist-num">{{ stat.overdueCount }} 单 / 在途 {{ stat.activeCount }} 单</span>
                  </div>
                </div>
                <div class="section-tip">
                  待分配 {{ stat.pendingCount }} 单仍无人承接 —— 可在「调度执行」预览后一键自动调度，或到《调度任务》手工指派。
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（调度执行） ═══ -->
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

      <!-- ═══ 手动指派弹窗（候选含评分/在途/约束原因） ═══ -->
      <a-modal
        v-model:open="assignVisible"
        title="手动指派配送员"
        :width="720"
        :mask-closable="false"
        :confirm-loading="assigning"
        ok-text="确认指派"
        cancel-text="取消"
        @ok="handleAssignOk"
      >
        <div class="assign-head">
          任务：{{ assignTask?.taskNo || assignTask?.id }} · 客户：{{ assignTask?.customerName || '-' }}
        </div>
        <a-table
          :data-source="candidates"
          :columns="candidateColumns"
          :loading="candidatesLoading"
          :pagination="false"
          row-key="riderId"
          size="small"
          bordered
          :row-selection="candidateSelection"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'eligible'">
              <a-tag :color="record.eligible ? 'green' : 'default'">
                {{ record.eligible ? '可指派' : '不可指派' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'reason'">
              <span class="fail-text">{{ record.reason || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'distanceMeters'">
              {{ fmtMeters(record.distanceMeters) }}
            </template>
            <template v-else-if="column.dataIndex === 'online'">
              <a-tag :color="record.online ? 'green' : 'default'">
                {{ record.online ? '在线' : '离线' }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'routeBound'">
              <a-tag
                v-if="record.routeBound"
                color="blue"
              >
                已绑定
              </a-tag>
              <span
                v-else
                class="sub-text"
              >未绑定</span>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 区域分包绑定弹窗（线路档案 × 配送员，禁止手输 ID） ═══ -->
      <a-modal
        v-model:open="bindingVisible"
        :title="bindingForm.id ? '编辑区域分包绑定' : '新增区域分包绑定'"
        :width="580"
        :mask-closable="false"
        :confirm-loading="bindingSaving"
        ok-text="保存"
        cancel-text="取消"
        @ok="handleBindingOk"
      >
        <a-form
          layout="horizontal"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          :colon="false"
          size="small"
        >
          <a-form-item
            label="配送线路"
            required
          >
            <a-select
              v-model:value="bindingForm.routeId"
              show-search
              :filter-option="false"
              :options="routeSelectOptions"
              :loading="routeLoading"
              placeholder="按编号/名称搜索线路档案"
              style="width: 100%"
              @search="loadRouteOptions"
            />
          </a-form-item>
          <a-form-item
            label="配送员"
            required
          >
            <a-select
              v-model:value="bindingForm.riderId"
              show-search
              :filter-option="false"
              :options="riderSelectOptions"
              :loading="riderLoading"
              placeholder="按姓名/手机号搜索配送员"
              style="width: 100%"
              @search="loadRiderOptions"
            />
          </a-form-item>
          <a-form-item label="优先级">
            <a-input-number
              v-model:value="bindingForm.priority"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
            <div class="section-tip">数值越小越优先（同一线路绑定多人时排序）</div>
          </a-form-item>
          <a-form-item label="状态">
            <a-switch
              :checked="bindingForm.status === 1"
              @change="(v: any) => (bindingForm.status = v ? 1 : 0)"
            />
            <span class="section-tip">停用后不参与区域分包命中</span>
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="bindingForm.remark"
              :rows="2"
              placeholder="选填"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="dms-dispatch-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined, SettingOutlined, EyeOutlined, ThunderboltOutlined,
  SaveOutlined, UndoOutlined, PlusOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import request from '@/utils/request'
import {
  dispatchApi, type DispatchCandidate, type DispatchPreviewRow, type DispatchStrategy,
  type RouteRiderBinding, type RouteRiderSaveData,
} from '@/api/dms/dispatch'

defineOptions({ name: 'DmsDispatch' })

const STRATEGY_OPTIONS = [
  { label: '最近可用（距离优先）', value: 'NEAREST' },
  { label: '负载均衡（在途少优先）', value: 'BALANCED' },
  { label: '评分优先', value: 'SCORE' },
  { label: '区域分包', value: 'AREA' },
]

const dispatchTabs = [
  { key: 'strategy', label: '策略配置' },
  { key: 'execute', label: '调度执行' },
  { key: 'review', label: '效果复盘' },
]
const activeTab = ref('strategy')

// ═══ 策略配置 ═══
const savingStrategy = ref(false)
const strategyForm = reactive<DispatchStrategy>({
  strategy: 'NEAREST',
  weightDistance: 1,
  weightLoad: 1,
  weightScore: 1,
  maxConcurrent: 5,
  requireOnline: true,
  maxLoadKg: 2000,
  maxVolumeM3: 10,
  timeoutEscalateMinutes: 30,
  areaStrict: false,
})

async function loadStrategy() {
  try {
    const res: any = await dispatchApi.strategy()
    Object.assign(strategyForm, {
      strategy: res?.strategy || 'NEAREST',
      weightDistance: Number(res?.weightDistance ?? 1),
      weightLoad: Number(res?.weightLoad ?? 1),
      weightScore: Number(res?.weightScore ?? 1),
      maxConcurrent: Number(res?.maxConcurrent ?? 5),
      requireOnline: res?.requireOnline !== false,
      maxLoadKg: Number(res?.maxLoadKg ?? 2000),
      maxVolumeM3: Number(res?.maxVolumeM3 ?? 10),
      timeoutEscalateMinutes: Number(res?.timeoutEscalateMinutes ?? 30),
      areaStrict: res?.areaStrict === true,
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载派单策略失败')
  }
}

async function handleSaveStrategy() {
  savingStrategy.value = true
  try {
    await dispatchApi.saveStrategy({ ...strategyForm })
    message.success('策略已保存（配置中心，热生效）')
    loadStrategy()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存策略失败')
  } finally {
    savingStrategy.value = false
  }
}

// ═══ 区域分包绑定（线路档案 × 配送员） ═══
const bindings = ref<RouteRiderBinding[]>([])
const bindingLoading = ref(false)
const bindingPagination = reactive({ current: 1, pageSize: 10, total: 0 })
const bindingQuery = reactive({ keyword: '' })
const bindingVisible = ref(false)
const bindingSaving = ref(false)
const bindingForm = reactive<RouteRiderSaveData & { id?: number }>({
  id: undefined, routeId: undefined, riderId: undefined, priority: 0, status: 1, remark: '',
})
const routeSelectOptions = ref<Array<{ label: string; value: number }>>([])
const riderSelectOptions = ref<Array<{ label: string; value: number }>>([])
const routeLoading = ref(false)
const riderLoading = ref(false)

const bindingColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'bindingActionCell', width: 140, fixed: 'left' },
  { title: '线路编号', key: 'routeCode', width: 130 },
  { title: '线路名称', key: 'routeName', width: 180 },
  { title: '配送员', key: 'riderName', width: 130 },
  { title: '手机号', key: 'riderPhone', width: 130 },
  { title: '优先级', key: 'priority', width: 90 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'bindingStatusCell', width: 90 },
  { title: '备注', key: 'remark', width: 200 },
  { title: '更新时间', key: 'updateTime', width: 170 },
]

async function fetchBindings() {
  bindingLoading.value = true
  try {
    const res: any = await dispatchApi.routeRiderPage({
      pageNum: bindingPagination.current,
      pageSize: bindingPagination.pageSize,
      keyword: bindingQuery.keyword.trim() || undefined,
    })
    bindings.value = res?.records || []
    bindingPagination.total = Number(res?.total) || 0
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载区域分包绑定失败')
    bindings.value = []
  } finally {
    bindingLoading.value = false
  }
}

function handleBindingSearch() {
  bindingPagination.current = 1
  fetchBindings()
}

function handleBindingPageChange(page: number, pageSize: number) {
  bindingPagination.current = page
  bindingPagination.pageSize = pageSize
  fetchBindings()
}

async function loadRouteOptions(keyword?: string) {
  routeLoading.value = true
  try {
    const res: any = await dispatchApi.routeOptions(keyword?.trim() || undefined)
    routeSelectOptions.value = (Array.isArray(res) ? res : []).map((r: any) => ({
      label: r.routeCode ? `${r.routeName}（${r.routeCode}）` : r.routeName,
      value: r.id,
    }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载线路档案失败')
  } finally {
    routeLoading.value = false
  }
}

async function loadRiderOptions(keyword?: string) {
  riderLoading.value = true
  try {
    const res: any = await dispatchApi.riderOptions(keyword?.trim() || undefined)
    riderSelectOptions.value = (Array.isArray(res) ? res : []).map((r: any) => ({
      label: r.phone ? `${r.realName}（${r.phone}）` : r.realName,
      value: r.id,
    }))
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载配送员失败')
  } finally {
    riderLoading.value = false
  }
}

/** 新增（record 为空）或编辑：选项先按当前值补齐，避免回填时下拉显示空白 */
function openBinding(record?: RouteRiderBinding | null) {
  bindingForm.id = record?.id
  bindingForm.routeId = record?.routeId
  bindingForm.riderId = record?.riderId
  bindingForm.priority = record?.priority ?? 0
  bindingForm.status = record?.status ?? 1
  bindingForm.remark = record?.remark || ''
  bindingVisible.value = true
  loadRouteOptions()
  loadRiderOptions()
  if (record) {
    if (record.routeId && !routeSelectOptions.value.some(o => o.value === record.routeId)) {
      routeSelectOptions.value = [
        { label: record.routeCode ? `${record.routeName}（${record.routeCode}）` : String(record.routeName), value: record.routeId },
        ...routeSelectOptions.value,
      ]
    }
    if (record.riderId && !riderSelectOptions.value.some(o => o.value === record.riderId)) {
      riderSelectOptions.value = [
        { label: record.riderPhone ? `${record.riderName}（${record.riderPhone}）` : String(record.riderName), value: record.riderId },
        ...riderSelectOptions.value,
      ]
    }
  }
}

async function handleBindingOk() {
  if (!bindingForm.routeId) {
    message.warning('请选择配送线路')
    return
  }
  if (!bindingForm.riderId) {
    message.warning('请选择配送员')
    return
  }
  bindingSaving.value = true
  try {
    const payload: RouteRiderSaveData = {
      routeId: bindingForm.routeId,
      riderId: bindingForm.riderId,
      priority: bindingForm.priority ?? 0,
      status: bindingForm.status ?? 1,
      remark: bindingForm.remark,
    }
    if (bindingForm.id) {
      await dispatchApi.updateRouteRider(bindingForm.id, payload)
      message.success('绑定已更新')
    } else {
      await dispatchApi.createRouteRider(payload)
      message.success('绑定已新增')
    }
    bindingVisible.value = false
    fetchBindings()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存绑定失败')
  } finally {
    bindingSaving.value = false
  }
}

async function toggleBindingStatus(record: RouteRiderBinding) {
  const next = record.status === 1 ? 0 : 1
  try {
    await dispatchApi.updateRouteRiderStatus(record.id, next)
    message.success(next === 1 ? '已启用' : '已停用')
    fetchBindings()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '状态更新失败')
  }
}

async function removeBinding(record: RouteRiderBinding) {
  try {
    await dispatchApi.deleteRouteRider(record.id)
    message.success('绑定已删除')
    fetchBindings()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '删除失败')
  }
}

// ═══ 调度执行 ═══
const loading = ref(false)
const previewing = ref(false)
const running = ref(false)
const taskRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({ taskNo: '', maxTasks: 50 as number | undefined })
const previewResult = reactive({
  strategy: '', strategyText: '', taskCount: 0, assignableCount: 0, unassignableCount: 0,
  rows: [] as DispatchPreviewRow[],
})

const taskColumns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 100, fixed: 'left' },
  { title: '任务编号', key: 'taskNo', type: 'slot', slotName: 'taskCell', width: 160 },
  { title: '客户', key: 'customerName', width: 150 },
  { title: '配送费', key: 'deliveryFee', width: 100 },
  { title: '重量(kg)', key: 'totalWeight', width: 100 },
  { title: '体积(m³)', key: 'totalVolume', width: 100 },
  { title: '状态', key: 'status', type: 'slot', slotName: 'statusCell', width: 100 },
  { title: '预览·命中配送员', key: 'previewRider', type: 'slot', slotName: 'previewRiderCell', width: 200 },
  { title: '预览·命中规则', key: 'ruleHit', type: 'slot', slotName: 'ruleCell', width: 320 },
]

async function fetchTasks() {
  loading.value = true
  try {
    const res: any = await request.get('/dms/task/page', {
      params: {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        statusList: '0',
        ...(searchForm.taskNo ? { taskNo: searchForm.taskNo.trim() } : {}),
      },
    })
    const rows: any[] = res?.records || []
    // 预览结果按 taskId 回填到行上（同一张表即可看到「选中谁 + 命中规则 + 理由」）
    const byId = new Map(previewResult.rows.map(r => [String(r.taskId), r]))
    taskRows.value = rows.map(r => ({ ...r, __preview: byId.get(String(r.id)) || null }))
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载待分配任务失败')
  } finally {
    loading.value = false
  }
}

async function handlePreview() {
  previewing.value = true
  try {
    const res: any = await dispatchApi.preview(searchForm.maxTasks)
    Object.assign(previewResult, {
      strategy: res?.strategy || '',
      strategyText: res?.strategyText || '',
      taskCount: Number(res?.taskCount) || 0,
      assignableCount: Number(res?.assignableCount) || 0,
      unassignableCount: Number(res?.unassignableCount) || 0,
      rows: res?.rows || [],
    })
    await fetchTasks()
    message.success(`预览完成：待分配 ${previewResult.taskCount} 单，可指派 ${previewResult.assignableCount} 单`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '自动调度预览失败')
  } finally {
    previewing.value = false
  }
}

async function handleRun() {
  const count = previewResult.assignableCount
  if (count === 0) {
    message.warning('没有可指派的任务（请先预览，或检查约束/配送员）')
    return
  }
  Modal.confirm({
    title: '确认执行自动调度',
    content: `将按当前策略「${previewResult.strategyText}」指派 ${count} 单，确认执行？`,
    okText: '确认执行',
    onOk: async () => {
      running.value = true
      try {
        const res: any = await dispatchApi.autoDispatch(false, searchForm.maxTasks)
        const failed = (res?.rows || []).filter((r: any) => !r.assignable)
        message.success(`自动调度完成：成功 ${res?.assignableCount || 0} 单`
          + (failed.length ? `，失败 ${failed.length} 单（原因见表格）` : ''))
        Object.assign(previewResult, {
          rows: res?.rows || [],
          assignableCount: Number(res?.assignableCount) || 0,
          taskCount: Number(res?.taskCount) || 0,
        })
        await fetchTasks()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '自动调度失败')
      } finally {
        running.value = false
      }
    },
  })
}

function handleSearch() {
  pagination.current = 1
  fetchTasks()
}

function handleReset() {
  searchForm.taskNo = ''
  searchForm.maxTasks = 50
  previewResult.rows = []
  previewResult.assignableCount = 0
  searchForm.taskNo = ''
  pagination.current = 1
  fetchTasks()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchTasks()
}

// ═══ 手动指派 ═══
const assignVisible = ref(false)
const assigning = ref(false)
const candidatesLoading = ref(false)
const assignTask = ref<any>(null)
const candidates = ref<DispatchCandidate[]>([])
const selectedRiderId = ref<string | null>(null)

const candidateColumns = [
  { title: '配送员', dataIndex: 'riderName', width: 120 },
  { title: '手机号', dataIndex: 'riderPhone', width: 130 },
  { title: '距离', dataIndex: 'distanceMeters', width: 100 },
  { title: '在途', dataIndex: 'activeTasks', width: 70 },
  { title: '评分', dataIndex: 'ratingScore', width: 70 },
  { title: '综合得分', dataIndex: 'score', width: 100 },
  { title: '在线', dataIndex: 'online', width: 80 },
  { title: '线路绑定', dataIndex: 'routeBound', width: 90 },
  { title: '可指派', dataIndex: 'eligible', width: 90 },
  { title: '约束说明', dataIndex: 'reason' },
]

const candidateSelection = computed(() => ({
  selectedRowKeys: selectedRiderId.value ? [selectedRiderId.value] : [],
  onChange: (keys: any[]) => { selectedRiderId.value = keys.length ? String(keys[0]) : null },
  getCheckboxProps: (record: DispatchCandidate) => ({ disabled: !record.eligible }),
}) as any)

async function openAssign(record: any) {
  assignTask.value = record
  selectedRiderId.value = null
  assignVisible.value = true
  candidatesLoading.value = true
  try {
    const res: any = await dispatchApi.candidates(record.id)
    candidates.value = Array.isArray(res) ? res : []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载候选配送员失败')
    candidates.value = []
  } finally {
    candidatesLoading.value = false
  }
}

async function handleAssignOk() {
  if (!selectedRiderId.value) {
    message.warning('请选择一名「可指派」的配送员')
    return
  }
  assigning.value = true
  try {
    await dispatchApi.assign(assignTask.value.id, selectedRiderId.value)
    message.success('指派成功')
    assignVisible.value = false
    await fetchTasks()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '指派失败')
  } finally {
    assigning.value = false
  }
}

// ═══ 效果复盘 ═══
const stat = reactive({
  taskTotal: 0, pendingCount: 0, assignedCount: 0, autoCount: 0, manualCount: 0, otherCount: 0,
  grabCount: 0, bidCount: 0,
  autoRate: 0, avgDispatchSeconds: 0, activeCount: 0, overdueCount: 0, overdueRate: 0, unassignedCount: 0,
})

async function fetchStat() {
  try {
    const res: any = await dispatchApi.stat()
    Object.assign(stat, {
      taskTotal: Number(res?.taskTotal) || 0,
      pendingCount: Number(res?.pendingCount) || 0,
      assignedCount: Number(res?.assignedCount) || 0,
      autoCount: Number(res?.autoCount) || 0,
      manualCount: Number(res?.manualCount) || 0,
      otherCount: Number(res?.otherCount) || 0,
      grabCount: Number(res?.grabCount) || 0,
      bidCount: Number(res?.bidCount) || 0,
      autoRate: Number(res?.autoRate) || 0,
      avgDispatchSeconds: Number(res?.avgDispatchSeconds) || 0,
      activeCount: Number(res?.activeCount) || 0,
      overdueCount: Number(res?.overdueCount) || 0,
      overdueRate: Number(res?.overdueRate) || 0,
      unassignedCount: Number(res?.unassignedCount) || 0,
    })
  } catch (error) {
    console.warn('[智能调度] 统计加载失败', error)
  }
}

function percent(count: number) {
  const base = stat.autoCount + stat.manualCount + stat.grabCount + stat.bidCount + stat.otherCount
  if (!base) return 0
  return Math.round((count * 100) / base)
}

function refreshAll() {
  fetchStat()
  if (activeTab.value === 'execute') fetchTasks()
  if (activeTab.value === 'strategy') {
    loadStrategy()
    fetchBindings()
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  if (key === 'execute') fetchTasks()
  else if (key === 'strategy') {
    loadStrategy()
    fetchBindings()
  } else fetchStat()
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'taskNo', label: '任务编号', visible: true },
  { key: 'maxTasks', label: '扫描上限', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'preview', label: '自动调度预览', enabled: true },
  { key: 'run', label: '执行自动调度', enabled: true },
  { key: 'saveStrategy', label: '保存策略', enabled: true },
  { key: 'addBinding', label: '新增区域分包绑定', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
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
function fmtMeters(val?: number | null) {
  if (val == null) return ''
  const n = Number(val)
  return Number.isNaN(n) ? '' : `${Math.round(n)} 米`
}

function fmtRate(val?: number | null) {
  if (val == null) return '-'
  return `${Number(val).toFixed(2)}%`
}

function fmtSeconds(val?: number | null) {
  if (val == null) return '-'
  const n = Number(val)
  if (n < 60) return `${n.toFixed(0)} 秒`
  if (n < 3600) return `${(n / 60).toFixed(1)} 分钟`
  return `${(n / 3600).toFixed(1)} 小时`
}

function handleError(error: Error) {
  console.error('[智能调度] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadStrategy()
  fetchStat()
  fetchBindings()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.tip-text { font-size: 12px; color: #8c8c8c; }
.sel-tip { font-size: 12px; color: #1890ff; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.pane-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 16px; background: #f5f7fa; }

.section-card {
  background: #fff;
  border-radius: 6px;
  padding: 16px 24px 8px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.section-title { font-size: 14px; font-weight: 600; color: #262626; }
.section-tip { font-size: 12px; color: #8c8c8c; }

.stat-bar {
  display: flex;
  align-items: baseline;
  gap: 22px;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  flex-wrap: wrap;
}
.stat-item { display: flex; align-items: baseline; gap: 6px; }
.stat-label { font-size: 12px; color: #8c8c8c; }
.stat-value { font-size: 16px; font-weight: 600; color: #262626; }
.stat-ok { color: #389e0d; }
.stat-warn { color: #d46b08; }
.stat-danger { color: #cf1322; }

.dist-row { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.dist-item { display: flex; align-items: center; gap: 12px; }
.dist-label { width: 90px; font-size: 13px; color: #595959; }
.dist-item :deep(.ant-progress) { flex: 1; margin-bottom: 0; }
.dist-num { width: 140px; font-size: 12px; color: #8c8c8c; text-align: right; }

.binding-tip { margin-bottom: 8px; }
.warn-inline { margin-left: 8px; color: #d46b08; }
.sub-text { font-size: 12px; color: #8c8c8c; margin-left: 4px; }
.fail-text { font-size: 12px; color: #cf1322; }
.rule-text { font-size: 12px; color: #595959; }
.assign-head { margin-bottom: 10px; font-size: 13px; color: #262626; }

.btn-run { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-run:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-form-item) { margin-bottom: 12px; }
:deep(.ant-form-item-label > label) { font-size: 13px; color: #595959; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
