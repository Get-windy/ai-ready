<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        商机（CRM → 商机管理 → 商机，菜单 80220）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的 Opportunity 模型建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/商机开发文档.md
        · 路线 A′：ErrorBoundary > PageContainer(full-height) > CategoryListLayout，
          管道看板 / 列表 / 统计三视图共用同一外壳（视图切换在工具栏）
        · 列配置齿轮在表头 rowNo 列；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 本轮改造：外壳升级金标准、阶段值域统一为数字 1-5（与后端 opportunity_stage 一致）、
                  看板拖拽真实调用 PUT /crm/opportunity/{id}/stage、表单保存真实落库、
                  列表/看板/统计改用实体字段名（opportunityName/opportunityStage/salesPersonName/probability）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建商机 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            type="primary"
            size="small"
            class="btn-add"
            title="新增商机(Ctrl+N)"
            @click="handleAdd"
          >
            <PlusOutlined /> 新建商机
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：视图切换 / 刷新 / 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-radio-group
              v-model:value="activeView"
              button-style="solid"
              size="small"
            >
              <a-radio-button value="pipeline">
                <AppstoreOutlined /> 管道
              </a-radio-button>
              <a-radio-button value="list">
                <UnorderedListOutlined /> 列表
              </a-radio-button>
              <a-radio-button value="statistics">
                <BarChartOutlined /> 统计
              </a-radio-button>
            </a-radio-group>
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <span
              v-if="autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              title="刷新(F5)"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
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

        <!-- ═══ 查询区（横向网格；显隐受页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isFieldVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">关键词</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="商机名称/客户"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('customerId')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-select
                  v-model:value="searchForm.customerId"
                  placeholder="全部客户"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="customerOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('opportunityStage')"
                class="search-item"
              >
                <span class="search-label">商机阶段</span>
                <a-select
                  v-model:value="searchForm.opportunityStage"
                  placeholder="全部阶段"
                  size="small"
                  allow-clear
                  :options="STAGE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('salesPersonId')"
                class="search-item"
              >
                <span class="search-label">负责人</span>
                <a-select
                  v-model:value="searchForm.salesPersonId"
                  placeholder="全部负责人"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="userOptions"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button
                  type="primary"
                  size="small"
                  :loading="loading"
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

        <!-- ═══ 三视图（管道看板 / 列表 / 统计） ═══ -->
        <template #table>
          <div class="table-area">
            <!-- ── 视图 1：管道看板（默认视图，与改造前保持一致） ── -->
            <div
              v-if="activeView === 'pipeline'"
              class="pipeline-container"
            >
              <div
                v-for="stage in PIPELINE_STAGES"
                :key="stage.value"
                class="pipeline-stage"
              >
                <div
                  class="stage-header"
                  :style="{ borderBottomColor: stage.color }"
                >
                  <a-space>
                    <span class="stage-name">{{ stage.label }}</span>
                    <a-badge
                      :count="getStageCount(stage.value)"
                      :overflow-count="99"
                      :number-style="{ backgroundColor: stage.color }"
                    />
                  </a-space>
                  <span class="stage-amount">¥{{ formatAmount(getStageAmount(stage.value)) }}</span>
                </div>
                <div class="stage-cards">
                  <div
                    v-for="opp in getStageOpportunities(stage.value)"
                    :key="opp.id"
                    class="opportunity-card"
                    :draggable="isDraggable(opp)"
                    @dragstart="handleDragStart($event, opp, stage.value)"
                    @dragover.prevent
                    @dragenter.prevent
                    @drop="handleDrop($event, stage.value)"
                    @click="handleView(opp)"
                  >
                    <div class="card-header">
                      <span class="card-title">{{ opp.opportunityName }}</span>
                      <a-tag
                        :color="probabilityColor(opp.probability)"
                        size="small"
                      >
                        {{ Number(opp.probability) || 0 }}%
                      </a-tag>
                    </div>
                    <div class="card-body">
                      <div class="card-row">
                        <span class="label"><UserOutlined /> 客户:</span>
                        <span class="value">{{ opp.customerName }}</span>
                      </div>
                      <div class="card-row">
                        <span class="label"><DollarOutlined /> 金额:</span>
                        <span class="value amount">¥{{ formatAmount(opp.estimatedAmount) }}</span>
                      </div>
                      <div class="card-row">
                        <span class="label"><TeamOutlined /> 负责人:</span>
                        <span class="value">{{ opp.salesPersonName }}</span>
                      </div>
                    </div>
                    <div class="card-footer">
                      <span class="close-date"><CalendarOutlined /> {{ opp.expectedCloseDate || '-' }}</span>
                      <div class="card-actions">
                        <a-button
                          size="small"
                          type="link"
                          @click.stop="handleEdit(opp)"
                        >
                          编辑
                        </a-button>
                        <a-button
                          size="small"
                          type="link"
                          @click.stop="handleMove(opp)"
                        >
                          移动
                        </a-button>
                      </div>
                    </div>
                  </div>
                  <div
                    v-if="getStageOpportunities(stage.value).length === 0"
                    class="empty-stage"
                  >
                    <InboxOutlined class="empty-icon" />
                    <span>暂无商机</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- ── 视图 2：列表 ── -->
            <BillDetailTable
              v-else-if="activeView === 'list'"
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-opportunity-table-columns"
              global-config-key="crm-opportunity-table-columns"
            >
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-tooltip
                    title="查看详情"
                    placement="bottom"
                  >
                    <a-button
                      type="link"
                      size="small"
                      @click="handleView(record)"
                    >
                      <EyeOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    title="编辑"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:opportunity:edit'"
                      type="link"
                      size="small"
                      @click="handleEdit(record)"
                    >
                      <EditOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    title="移动阶段"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:opportunity:edit'"
                      type="link"
                      size="small"
                      @click="handleMove(record)"
                    >
                      <SwapRightOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    v-if="Number(record.opportunityStage) === 5"
                    title="转订单（须经报价单链路）"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:opportunity:convert'"
                      type="link"
                      size="small"
                      @click="handleConvert(record)"
                    >
                      <FileProtectOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-dropdown trigger="click">
                    <a-button
                      type="link"
                      size="small"
                      class="action-more-btn"
                    >
                      <MoreOutlined />
                    </a-button>
                    <template #overlay>
                      <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                        <a-menu-item key="follow">
                          <MessageOutlined /> 添加跟进
                        </a-menu-item>
                        <a-menu-item key="quotation">
                          <FileTextOutlined /> 创建报价
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item
                          key="close"
                          danger
                        >
                          <StopOutlined /> 关闭商机
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>

              <template #stageCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="stageColor(record.opportunityStage)"
                >
                  {{ stageText(record) }}
                </a-tag>
              </template>

              <template #probabilityCell="{ record }">
                <a-progress
                  v-if="!record.__ghost"
                  :percent="Number(record.probability) || 0"
                  size="small"
                  :stroke-color="Number(record.probability) >= 70 ? '#52c41a' : '#1890ff'"
                />
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="statusColor(record.status)"
                >
                  {{ statusText(record) }}
                </a-tag>
              </template>

              <template #opportunityTypeCell="{ record }">
                <span v-if="!record.__ghost">{{ typeText(record.opportunityType) }}</span>
              </template>

              <template #opportunitySourceCell="{ record }">
                <span v-if="!record.__ghost">{{ sourceText(record.opportunitySource) }}</span>
              </template>
            </BillDetailTable>

            <!-- ── 视图 3：统计（4 张卡片 + 阶段分布/金额趋势 + 阶段详情） ── -->
            <div
              v-else
              class="statistics-area"
            >
              <div class="stats-cards">
                <a-row :gutter="16">
                  <a-col :span="6">
                    <div class="stat-card stat-card-blue">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"
                      >
                        <FundOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          商机总数
                        </div>
                        <div class="stat-value">
                          {{ statistics.total }}
                        </div>
                        <div class="stat-desc">
                          全部商机
                        </div>
                      </div>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="stat-card stat-card-orange">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);"
                      >
                        <DollarOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          预计金额
                        </div>
                        <div class="stat-value">
                          ¥{{ formatAmount(statistics.totalAmount) }}
                        </div>
                        <div class="stat-desc">
                          预估成交总额
                        </div>
                      </div>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="stat-card stat-card-green">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);"
                      >
                        <CheckCircleOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          成交金额
                        </div>
                        <div class="stat-value">
                          ¥{{ formatAmount(statistics.wonAmount) }}
                        </div>
                        <div class="stat-desc positive">
                          已实际成交
                        </div>
                      </div>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="stat-card stat-card-purple">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);"
                      >
                        <FundOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          成交率
                        </div>
                        <div class="stat-value">
                          {{ statistics.winRate }}%
                        </div>
                        <div class="stat-desc">
                          赢单 / (赢单+输单)
                        </div>
                      </div>
                    </div>
                  </a-col>
                </a-row>
              </div>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-card
                    title="阶段分布"
                    size="small"
                    :loading="loading"
                  >
                    <div
                      ref="stageChartRef"
                      class="chart-container"
                    />
                  </a-card>
                </a-col>
                <a-col :span="12">
                  <a-card
                    title="金额趋势（近 6 个月）"
                    size="small"
                    :loading="loading"
                  >
                    <div
                      ref="trendChartRef"
                      class="chart-container"
                    />
                  </a-card>
                </a-col>
              </a-row>
              <a-card
                title="阶段详情统计"
                size="small"
                style="margin-top: 16px"
              >
                <!-- 只读汇总表：data-source 是 computed，用单向绑定（v-model 会往只读值回写）；
                     外层必须有定高 flex 容器，否则表格（height:0 + flex-grow:1）会塌陷为 0 -->
                <div class="stage-stats-wrap">
                  <BillDetailTable
                    :data-source="stageStatsData"
                    :columns="stageStatsColumns"
                    :view-mode="true"
                    :min-rows="5"
                    storage-key="crm-opportunity-stage-stats-columns"
                    global-config-key="crm-opportunity-stage-stats-columns"
                  />
                </div>
              </a-card>
            </div>
          </div>
        </template>

        <!-- ═══ 经典分页栏（仅列表视图需要） ═══ -->
        <template #table-footer>
          <StandardPagination
            v-if="activeView === 'list'"
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新建 / 编辑商机 ═══ -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        :save-loading="submitLoading"
        :show-save-and-new="!isEdit"
        :dirty="formDirty"
        @save="handleSubmit"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="商机名称"
                name="opportunityName"
              >
                <a-input
                  v-model:value="formData.opportunityName"
                  placeholder="请输入商机名称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="客户名称"
                name="customerId"
              >
                <a-select
                  v-model:value="formData.customerId"
                  placeholder="请选择客户"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="customerOptions"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="商机阶段"
                name="opportunityStage"
              >
                <a-select
                  v-model:value="formData.opportunityStage"
                  placeholder="请选择商机阶段"
                  :options="STAGE_OPTIONS"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="预计金额"
                name="estimatedAmount"
              >
                <a-input-number
                  v-model:value="formData.estimatedAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="负责人"
                name="salesPersonId"
              >
                <a-select
                  v-model:value="formData.salesPersonId"
                  placeholder="请选择负责人"
                  show-search
                  allow-clear
                  :filter-option="filterOption"
                  :options="userOptions"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="预计成交日期"
                name="expectedCloseDate"
              >
                <a-date-picker
                  v-model:value="formData.expectedCloseDate"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="赢单概率(%)"
                name="probability"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-slider
                  v-model:value="formData.probability"
                  :min="0"
                  :max="100"
                  :marks="{ 0: '0%', 25: '25%', 50: '50%', 75: '75%', 100: '100%' }"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="商机类型"
                name="opportunityType"
              >
                <a-select
                  v-model:value="formData.opportunityType"
                  placeholder="请选择商机类型"
                  :options="OPPORTUNITY_TYPE_OPTIONS"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="商机来源"
                name="opportunitySource"
              >
                <a-select
                  v-model:value="formData.opportunitySource"
                  placeholder="请选择商机来源"
                  :options="OPPORTUNITY_SOURCE_OPTIONS"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="意向产品"
                name="productInterest"
              >
                <a-input
                  v-model:value="formData.productInterest"
                  placeholder="客户意向产品"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="竞争对手"
                name="competitor"
              >
                <a-input
                  v-model:value="formData.competitor"
                  placeholder="主要竞争对手"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="需求描述"
                name="requirement"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="formData.requirement"
                  placeholder="请输入客户需求描述"
                  :rows="3"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="备注"
                name="remark"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="formData.remark"
                  placeholder="请输入备注"
                  :rows="2"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </FullScreenDetail>

      <!-- ═══ 商机详情 ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="商机详情"
        placement="right"
        width="70vw"
        :footer="null"
        @close="handleDetailClose"
      >
        <a-spin :spinning="detailLoading">
          <template v-if="detailError">
            <div class="table-empty">
              <WarningOutlined
                class="table-empty-icon"
                style="color: #faad14"
              />
              <p class="table-empty-text">
                详情数据加载失败
              </p>
              <a-button
                type="primary"
                size="small"
                @click="handleDetailRefresh"
              >
                <ReloadOutlined /> 重试
              </a-button>
            </div>
          </template>
          <template v-else-if="detailData.id">
            <a-descriptions
              :column="2"
              bordered
              size="small"
            >
              <a-descriptions-item label="商机编号">
                {{ detailData.opportunityCode || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="商机名称">
                <span class="opp-name">{{ detailData.opportunityName }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="商机阶段">
                <a-tag :color="stageColor(detailData.opportunityStage)">
                  {{ stageText(detailData) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="statusColor(detailData.status)">
                  {{ statusText(detailData) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="客户名称">
                {{ detailData.customerName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="负责人">
                {{ detailData.salesPersonName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="预计金额">
                <span class="amount-cell">¥{{ formatAmount(detailData.estimatedAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="成交金额">
                <span class="amount-cell">¥{{ formatAmount(detailData.actualAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="赢单概率">
                <a-progress
                  :percent="Number(detailData.probability) || 0"
                  size="small"
                />
              </a-descriptions-item>
              <a-descriptions-item label="商机类型">
                {{ typeText(detailData.opportunityType) }}
              </a-descriptions-item>
              <a-descriptions-item label="商机来源">
                {{ sourceText(detailData.opportunitySource) }}
              </a-descriptions-item>
              <a-descriptions-item label="部门">
                {{ detailData.departmentName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="预计成交日期">
                {{ detailData.expectedCloseDate || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="实际关闭日期">
                {{ detailData.actualCloseDate || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="意向产品">
                {{ detailData.productInterest || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="竞争对手">
                {{ detailData.competitor || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="需求描述"
                :span="2"
              >
                {{ detailData.requirement || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                v-if="detailData.winReason"
                label="赢单原因"
                :span="2"
              >
                {{ detailData.winReason }}
              </a-descriptions-item>
              <a-descriptions-item
                v-if="detailData.loseReason"
                label="输单原因"
                :span="2"
              >
                {{ detailData.loseReason }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ detailData.remark || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ detailData.createdAt || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="更新时间">
                {{ detailData.updatedAt || '-' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-divider>商机跟进记录</a-divider>
            <a-empty
              v-if="!followRecords.length"
              description="暂无跟进记录"
            />
            <a-timeline v-else>
              <a-timeline-item
                v-for="r in followRecords"
                :key="r.id"
                color="blue"
              >
                <div class="timeline-content">
                  <div class="timeline-title">
                    {{ followUpTypeText(r.followUpType) }}{{ r.followUpResultDesc ? ' · ' + r.followUpResultDesc : '' }}
                  </div>
                  <div class="timeline-desc">
                    {{ r.content }}
                  </div>
                  <div class="timeline-time">
                    {{ r.followUpDate || r.createdAt }} - {{ r.salesPersonName || '-' }}
                  </div>
                </div>
              </a-timeline-item>
            </a-timeline>
          </template>
        </a-spin>
      </a-drawer>

      <!-- ═══ 移动商机阶段 ═══ -->
      <a-modal
        v-model:open="moveVisible"
        title="移动商机阶段"
        width="480px"
        :confirm-loading="moveLoading"
        @ok="handleMoveConfirm"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="商机">
            <span>{{ moveData.opportunityName }}</span>
          </a-form-item>
          <a-form-item label="当前阶段">
            <a-tag :color="stageColor(moveData.currentStage)">
              {{ STAGE_TEXT[moveData.currentStage] || '未设置' }}
            </a-tag>
          </a-form-item>
          <a-form-item label="目标阶段">
            <a-select
              v-model:value="moveData.targetStage"
              placeholder="请选择目标阶段"
              size="small"
              :options="moveTargetOptions"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 添加跟进 ═══ -->
      <a-modal
        v-model:open="followVisible"
        title="添加跟进记录"
        width="560px"
        :confirm-loading="followSaving"
        ok-text="保存"
        cancel-text="取消"
        @ok="handleFollowSubmit"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="商机">
            <span>{{ followForm.opportunityName }}</span>
          </a-form-item>
          <a-form-item label="客户">
            <span>{{ followForm.customerName }}</span>
          </a-form-item>
          <a-form-item
            label="跟进方式"
            required
          >
            <a-select
              v-model:value="followForm.followUpType"
              :options="FOLLOW_UP_TYPE_OPTIONS"
              size="small"
            />
          </a-form-item>
          <a-form-item label="跟进日期">
            <a-date-picker
              v-model:value="followForm.followUpDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </a-form-item>
          <a-form-item
            label="跟进内容"
            required
          >
            <a-textarea
              v-model:value="followForm.content"
              placeholder="请输入跟进内容"
              :rows="3"
              size="small"
            />
          </a-form-item>
          <a-form-item label="跟进结果">
            <a-select
              v-model:value="followForm.followUpResult"
              :options="FOLLOW_UP_RESULT_OPTIONS"
              allow-clear
              size="small"
            />
          </a-form-item>
          <a-form-item label="下次跟进">
            <a-date-picker
              v-model:value="followForm.nextFollowUpDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 关闭商机（输单，须填原因） ═══ -->
      <a-modal
        v-model:open="closeVisible"
        title="关闭商机"
        width="480px"
        :confirm-loading="closeSaving"
        ok-text="确定关闭"
        ok-type="danger"
        cancel-text="取消"
        @ok="handleCloseConfirm"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="商机">
            <span>{{ closeForm.opportunityName }}</span>
          </a-form-item>
          <a-form-item
            label="输单原因"
            required
          >
            <a-textarea
              v-model:value="closeForm.loseReason"
              placeholder="请输入输单原因"
              :rows="3"
              size="small"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import * as echarts from 'echarts'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { opportunityApi, opportunityStageApi, crmCustomerApi, followUpApi } from '@/api/crm'
import { userApi } from '@/api/user'
import { exportCsv } from '@/utils/exportCsv'
import { useUserStore } from '@/stores/user'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  SwapRightOutlined,
  FileProtectOutlined,
  StopOutlined,
  ReloadOutlined,
  InboxOutlined,
  MoreOutlined,
  MessageOutlined,
  FileTextOutlined,
  DownloadOutlined,
  SettingOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  BarChartOutlined,
  FundOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  TeamOutlined,
  UserOutlined,
  CalendarOutlined,
  SyncOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmOpportunity' })

const userStore = useUserStore()

// ═══ 阶段 / 状态 字典（与后端 CustomerOpportunityServiceImpl 一致，唯一权威值域：数字 1-5） ═══
// 说明：终态「赢单/输单」不进阶段枚举，由 status 表达（1 跟进中 / 2 赢单 / 3 输单）
const STAGE_TEXT: Record<number, string> = { 1: '初步接触', 2: '需求确认', 3: '方案报价', 4: '商务谈判', 5: '成交' }
const STAGE_COLOR: Record<number, string> = { 1: 'default', 2: 'orange', 3: 'blue', 4: 'purple', 5: 'green' }
const STAGE_HEX: Record<number, string> = { 1: '#969799', 2: '#faad14', 3: '#1890ff', 4: '#722ed1', 5: '#52c41a' }
const STAGE_OPTIONS = [1, 2, 3, 4, 5].map(v => ({ label: STAGE_TEXT[v], value: v }))

const STATUS_TEXT: Record<number, string> = { 1: '跟进中', 2: '赢单', 3: '输单' }
const STATUS_COLOR: Record<number, string> = { 1: 'orange', 2: 'green', 3: 'red' }
const STATUS_OPTIONS = [1, 2, 3].map(v => ({ label: STATUS_TEXT[v], value: v }))

const OPPORTUNITY_TYPE_OPTIONS = [
  { label: '新客户', value: 1 },
  { label: '老客户增购', value: 2 },
  { label: '续约', value: 3 },
]
const OPPORTUNITY_TYPE_TEXT: Record<number, string> = { 1: '新客户', 2: '老客户增购', 3: '续约' }
const OPPORTUNITY_SOURCE_OPTIONS = [
  { label: '线索转化', value: 1 },
  { label: '客户主动', value: 2 },
  { label: '销售开发', value: 3 },
]
const OPPORTUNITY_SOURCE_TEXT: Record<number, string> = { 1: '线索转化', 2: '客户主动', 3: '销售开发' }

const FOLLOW_UP_TYPE_OPTIONS = [
  { label: '电话', value: 1 },
  { label: '拜访', value: 2 },
  { label: '邮件', value: 3 },
  { label: '微信', value: 4 },
  { label: '其他', value: 5 },
]
const FOLLOW_UP_TYPE_TEXT: Record<number, string> = { 1: '电话', 2: '拜访', 3: '邮件', 4: '微信', 5: '其他' }
const FOLLOW_UP_RESULT_OPTIONS = [
  { label: '有意向', value: 1 },
  { label: '无意向', value: 2 },
  { label: '待跟进', value: 3 },
]
const FOLLOW_UP_RESULT_TEXT: Record<number, string> = { 1: '有意向', 2: '无意向', 3: '待跟进' }

// 管道看板 5 列（阶段 1-5；「输单」商机移出管道，见 getStageOpportunities）
const PIPELINE_STAGES = [1, 2, 3, 4, 5].map(v => ({
  value: v,
  label: STAGE_TEXT[v],
  color: STAGE_HEX[v],
}))

function stageText(record: any): string {
  return record?.opportunityStageDesc || STAGE_TEXT[record?.opportunityStage] || '未设置'
}
function stageColor(v: number | undefined): string {
  return (v && STAGE_COLOR[v]) || 'default'
}
function statusText(record: any): string {
  return record?.statusDesc || STATUS_TEXT[record?.status] || '跟进中'
}
function statusColor(v: number | undefined): string {
  return STATUS_COLOR[Number(v) || 1] || STATUS_COLOR[1]
}
function typeText(v: number | undefined): string {
  return (v && OPPORTUNITY_TYPE_TEXT[v]) || '-'
}
function sourceText(v: number | undefined): string {
  return (v && OPPORTUNITY_SOURCE_TEXT[v]) || '-'
}
function followUpTypeText(v: number | undefined): string {
  return (v && FOLLOW_UP_TYPE_TEXT[v]) || '跟进'
}
function probabilityColor(v: number | undefined): string {
  const n = Number(v) || 0
  if (n >= 80) return 'green'
  if (n >= 60) return 'blue'
  if (n >= 40) return 'orange'
  return 'default'
}
/** 金额千分位。⚠️ BillDetailTable 的 formatter 是位置参数 (raw, record)，不要写成 ({ row }) => … */
function moneyFormatter(raw: any): string {
  if (raw === null || raw === undefined || raw === '') return '-'
  const n = Number(raw)
  if (Number.isNaN(n)) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatAmount(v: any): string {
  const n = Number(v)
  if (!Number.isFinite(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词（商机名称/客户）', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'opportunityStage', label: '商机阶段', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'salesPersonId', label: '负责人', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建商机', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-opportunity-page-config'

function isFieldVisible(key: string): boolean {
  const found = queryFieldsConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}
function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
}

// ═══ 视图 / 数据状态 ═══
const activeView = ref<'pipeline' | 'list' | 'statistics'>('pipeline')
const loading = ref(false)
const submitLoading = ref(false)
const moveLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const tableData = ref<any[]>([])
/** 全量商机（/export）：管道看板与统计必须基于全量，否则列头计数/金额只是当前页的失真值 */
const boardData = ref<any[]>([])

const searchForm = reactive({
  keyword: '',
  customerId: undefined as number | undefined,
  opportunityStage: undefined as number | undefined,
  status: undefined as number | undefined,
  salesPersonId: undefined as number | undefined,
})

const customerOptions = ref<{ label: string; value: number }[]>([])
const userOptions = ref<{ label: string; value: number }[]>([])

// ═══ 列表视图列定义（实体字段名；rowNo 承载列配置齿轮，action 为固定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'opportunityCode', title: '商机编号', type: 'input', width: 160 },
  { key: 'opportunityName', title: '商机名称', type: 'input', width: 180 },
  { key: 'customerName', title: '客户', type: 'input', width: 160 },
  { key: 'opportunityStage', title: '商机阶段', type: 'slot', slotName: 'stageCell', width: 100, align: 'center' },
  { key: 'probability', title: '赢单概率', type: 'slot', slotName: 'probabilityCell', width: 130 },
  { key: 'estimatedAmount', title: '预计金额', type: 'input', width: 120, align: 'right', formatter: moneyFormatter },
  { key: 'actualAmount', title: '成交金额', type: 'input', width: 120, align: 'right', formatter: moneyFormatter },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90, align: 'center' },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'expectedCloseDate', title: '预计成交日期', type: 'input', width: 120 },
  { key: 'opportunityStageDesc', title: '阶段说明', type: 'input', width: 100, defaultHidden: true },
  { key: 'statusDesc', title: '状态说明', type: 'input', width: 100, defaultHidden: true },
  { key: 'opportunityType', title: '商机类型', type: 'slot', slotName: 'opportunityTypeCell', width: 110, defaultHidden: true },
  { key: 'opportunitySource', title: '商机来源', type: 'slot', slotName: 'opportunitySourceCell', width: 110, defaultHidden: true },
  { key: 'leadId', title: '来源线索', type: 'input', width: 120, defaultHidden: true },
  { key: 'productInterest', title: '意向产品', type: 'input', width: 160, defaultHidden: true },
  { key: 'requirement', title: '需求描述', type: 'input', width: 200, defaultHidden: true },
  { key: 'competitor', title: '竞争对手', type: 'input', width: 140, defaultHidden: true },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'actualCloseDate', title: '实际关闭日期', type: 'input', width: 120, defaultHidden: true },
  { key: 'winReason', title: '赢单原因', type: 'input', width: 160, defaultHidden: true },
  { key: 'loseReason', title: '输单原因', type: 'input', width: 160, defaultHidden: true },
  { key: 'createdAt', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 统计视图「阶段详情统计」列定义（只读汇总表，min-rows 取 5 以避免占位行撑高卡片） ═══
const stageStatsColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'stageName', title: '阶段', type: 'input', width: 120 },
  { key: 'count', title: '商机数', type: 'input', width: 90, align: 'right' },
  { key: 'amount', title: '预计金额', type: 'input', width: 140, align: 'right', formatter: moneyFormatter },
  { key: 'avgAmount', title: '平均金额', type: 'input', width: 140, align: 'right', formatter: moneyFormatter },
  { key: 'percent', title: '占比', type: 'input', width: 90, align: 'right' },
]

// ═══ 查询参数组装（后端 /page 支持的 5 个过滤项，全部真实生效） ═══
function buildQuery() {
  return {
    keyword: searchForm.keyword || undefined,
    customerId: searchForm.customerId,
    opportunityStage: searchForm.opportunityStage,
    status: searchForm.status,
    salesPersonId: searchForm.salesPersonId,
  }
}

async function fetchList() {
  try {
    const res: any = await opportunityApi.page({
      ...buildQuery(),
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e: any) {
    console.error('[商机] 加载列表失败', e)
    message.error(e?.message || '获取商机列表失败')
    tableData.value = []
    pagination.total = 0
  }
}

async function fetchBoard() {
  try {
    // /export 返回裸 List：看板与统计基于全量（当前页只有 20 条，直接聚合会严重失真）
    const list: any = await opportunityStageApi.exportList(buildQuery())
    boardData.value = Array.isArray(list) ? list : []
  } catch (e: any) {
    console.warn('[商机] 加载全量商机失败', e)
    boardData.value = []
  }
}

async function fetchData() {
  loading.value = true
  try {
    const tasks: Promise<any>[] = [fetchList()]
    // 列表视图不需要看板数据，避免每次翻页都拉全量
    if (activeView.value !== 'list') tasks.push(fetchBoard())
    await Promise.allSettled(tasks)
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    if (activeView.value === 'statistics') nextTick(initCharts)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.customerId = undefined
  searchForm.opportunityStage = undefined
  searchForm.status = undefined
  searchForm.salesPersonId = undefined
  pagination.current = 1
  fetchData()
}

function handleRefresh() {
  lastUpdateTime.value = ''
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 看板聚合（基于全量 boardData；输单商机移出管道，赢单停留在「成交」列） ═══
function getStageOpportunities(stage: number) {
  return boardData.value.filter(o => Number(o.opportunityStage) === stage && Number(o.status) !== 3)
}
function getStageCount(stage: number) {
  return getStageOpportunities(stage).length
}
function getStageAmount(stage: number) {
  return getStageOpportunities(stage).reduce((s, o) => s + (Number(o.estimatedAmount) || 0), 0)
}
/** 已赢单/已输单的商机不允许再拖拽改阶段 */
function isDraggable(opp: any): boolean {
  const status = Number(opp?.status)
  return status !== 2 && status !== 3
}

// ═══ 统计卡（前端全量聚合：/statistics 返回裸 Map 且忽略查询条件，此处口径与看板/筛选一致） ═══
const statistics = computed(() => {
  const rows = boardData.value
  const won = rows.filter(r => Number(r.status) === 2)
  const closed = rows.filter(r => Number(r.status) === 2 || Number(r.status) === 3)
  const totalAmount = rows.reduce((s, r) => s + (Number(r.estimatedAmount) || 0), 0)
  const wonAmount = won.reduce((s, r) => s + (Number(r.actualAmount) || 0), 0)
  // 守卫分母为 0（全部商机都在跟进中）的场景
  const winRate = closed.length ? Math.round((won.length / closed.length) * 1000) / 10 : 0
  return { total: rows.length, totalAmount, wonAmount, winRate }
})

const stageStatsData = computed(() => {
  const total = statistics.value.totalAmount
  return PIPELINE_STAGES.map(s => {
    const count = getStageCount(s.value)
    const amount = getStageAmount(s.value)
    return {
      stageName: s.label,
      count,
      amount,
      avgAmount: count > 0 ? amount / count : 0,
      percent: total > 0 ? `${Math.round((amount / total) * 100)}%` : '0%',
    }
  })
})

// ═══ 图表 ═══
const stageChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let stageChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

function initCharts() {
  initStageChart()
  initTrendChart()
}
function disposeCharts() {
  stageChart?.dispose()
  trendChart?.dispose()
  stageChart = null
  trendChart = null
}
function initStageChart() {
  if (!stageChartRef.value) return
  stageChart?.dispose()
  stageChart = echarts.init(stageChartRef.value)
  stageChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      name: '商机阶段',
      type: 'pie',
      radius: ['40%', '70%'],
      data: PIPELINE_STAGES
        .filter(s => getStageCount(s.value) > 0)
        .map(s => ({ name: s.label, value: getStageCount(s.value), itemStyle: { color: s.color } })),
    }],
  })
}
function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)
  // 近 6 个月，按「预计成交日期」聚合预计金额、按「实际关闭日期」聚合成交金额
  // ⚠️ 用 Map 而非普通对象做键表：'constructor'/'toString' 等原型键会污染取值
  const months: string[] = []
  for (let i = 5; i >= 0; i--) months.push(dayjs().subtract(i, 'month').format('YYYY-MM'))
  const estMap = new Map<string, number>()
  const actMap = new Map<string, number>()
  months.forEach(m => { estMap.set(m, 0); actMap.set(m, 0) })
  boardData.value.forEach((r: any) => {
    const estMonth = r.expectedCloseDate ? String(r.expectedCloseDate).slice(0, 7) : ''
    if (estMap.has(estMonth)) estMap.set(estMonth, (estMap.get(estMonth) || 0) + (Number(r.estimatedAmount) || 0))
    const actMonth = r.actualCloseDate ? String(r.actualCloseDate).slice(0, 7) : ''
    if (actMap.has(actMonth)) actMap.set(actMonth, (actMap.get(actMonth) || 0) + (Number(r.actualAmount) || 0))
  })
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['预计金额', '成交金额'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', name: '金额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
    series: [
      { name: '预计金额', type: 'line', data: months.map(m => estMap.get(m) || 0), smooth: true, itemStyle: { color: '#1890ff' } },
      { name: '成交金额', type: 'line', data: months.map(m => actMap.get(m) || 0), smooth: true, itemStyle: { color: '#52c41a' } },
    ],
  })
}

// 视图切换：切到看板需拉全量；切到统计再渲染图表；离开统计销毁实例避免 DOM 泄漏
watch(activeView, async (view) => {
  if (view === 'list') {
    disposeCharts()
    return
  }
  await fetchBoard()
  if (view === 'statistics') nextTick(initCharts)
  else disposeCharts()
})

// ═══ 新建 / 编辑商机（真实落库：POST /crm/opportunity、PUT /crm/opportunity/{id}） ═══
const modalVisible = ref(false)
const modalTitle = ref('新建商机')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<any>({
  id: undefined,
  opportunityName: '',
  customerId: undefined,
  opportunityStage: 1,
  estimatedAmount: undefined,
  probability: 20,
  salesPersonId: undefined,
  expectedCloseDate: undefined,
  opportunityType: undefined,
  opportunitySource: undefined,
  productInterest: '',
  competitor: '',
  requirement: '',
  remark: '',
})
const formRules = {
  opportunityName: [{ required: true, message: '请输入商机名称' }],
  customerId: [{ required: true, message: '请选择客户' }],
  opportunityStage: [{ required: true, message: '请选择商机阶段' }],
  estimatedAmount: [{ required: true, message: '请输入预计金额' }],
  salesPersonId: [{ required: true, message: '请选择负责人' }],
}

// 脏数据追踪（关闭弹窗时二次确认）
const initialFormSnapshot = ref('')
const formDirty = computed(() => {
  if (!modalVisible.value) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formData })
}

function resetFormData() {
  Object.assign(formData, {
    id: undefined,
    opportunityName: '',
    customerId: undefined,
    opportunityStage: 1,
    estimatedAmount: undefined,
    probability: 20,
    salesPersonId: undefined,
    expectedCloseDate: undefined,
    opportunityType: undefined,
    opportunitySource: undefined,
    productInterest: '',
    competitor: '',
    requirement: '',
    remark: '',
  })
}

function handleAdd() {
  modalTitle.value = '新建商机'
  isEdit.value = false
  resetFormData()
  modalVisible.value = true
  nextTick(saveFormSnapshot)
}

function handleEdit(record: any) {
  modalTitle.value = '编辑商机'
  isEdit.value = true
  // 只回填表单实际拥有的字段，避免把带时间戳/审计字段的整实体灌进表单
  Object.assign(formData, {
    id: record.id,
    opportunityName: record.opportunityName || '',
    customerId: record.customerId,
    opportunityStage: Number(record.opportunityStage) || 1,
    estimatedAmount: record.estimatedAmount === null || record.estimatedAmount === undefined
      ? undefined
      : Number(record.estimatedAmount),
    probability: Number(record.probability) || 0,
    salesPersonId: record.salesPersonId,
    expectedCloseDate: record.expectedCloseDate || undefined,
    opportunityType: record.opportunityType ?? undefined,
    opportunitySource: record.opportunitySource ?? undefined,
    productInterest: record.productInterest || '',
    competitor: record.competitor || '',
    requirement: record.requirement || '',
    remark: record.remark || '',
  })
  modalVisible.value = true
  nextTick(saveFormSnapshot)
}

async function handleSubmit(saveAndNew = false) {
  try {
    await formRef.value?.validate()
  } catch (err) {
    console.warn('[商机] 表单校验未通过', err)
    return
  }
  const customer = customerOptions.value.find(o => o.value === formData.customerId)
  const salesPerson = userOptions.value.find(o => o.value === formData.salesPersonId)
  const payload: any = {
    opportunityName: formData.opportunityName,
    customerId: formData.customerId,
    // 客户名快照：列表/看板直接读 customer_name，不写会恒空
    customerName: customer?.label,
    opportunityStage: formData.opportunityStage,
    opportunityStageDesc: STAGE_TEXT[formData.opportunityStage],
    estimatedAmount: formData.estimatedAmount,
    probability: formData.probability,
    salesPersonId: formData.salesPersonId,
    salesPersonName: salesPerson?.label,
    expectedCloseDate: formData.expectedCloseDate,
    opportunityType: formData.opportunityType,
    opportunitySource: formData.opportunitySource,
    productInterest: formData.productInterest || undefined,
    competitor: formData.competitor || undefined,
    requirement: formData.requirement || undefined,
    remark: formData.remark || undefined,
  }
  submitLoading.value = true
  try {
    if (isEdit.value && formData.id) {
      await opportunityApi.update(formData.id, payload)
      message.success('商机已更新')
    } else {
      await opportunityApi.create(payload)
      message.success('商机已保存')
    }
    if (saveAndNew) {
      isEdit.value = false
      resetFormData()
      nextTick(saveFormSnapshot)
    } else {
      modalVisible.value = false
    }
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    submitLoading.value = false
  }
}

function handleFormSaveAndNew() {
  handleSubmit(true)
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单内容尚未保存，确定要关闭吗？',
      centered: true,
      onOk: () => { modalVisible.value = false },
    })
    return
  }
  modalVisible.value = false
}

// ═══ 详情抽屉（真实 GET /crm/opportunity/{id} + 跟进记录 GET /crm/followUp/page） ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref(false)
const detailData = ref<any>({})
const followRecords = ref<any[]>([])

async function fetchDetail(id: number | string) {
  detailLoading.value = true
  detailError.value = false
  followRecords.value = []
  try {
    detailData.value = (await opportunityApi.getById(id as any)) as any
  } catch (err) {
    detailError.value = true
    console.error('[商机] 获取商机详情失败', err)
    message.error('获取商机详情失败')
    detailLoading.value = false
    return
  }
  // 跟进记录单独兜底：跟进查询失败不应把整个详情抽屉判为加载失败
  try {
    const res: any = await followUpApi.page({ opportunityId: id as any, pageNum: 1, pageSize: 50 })
    followRecords.value = res?.records || []
  } catch (err) {
    console.warn('[商机] 获取跟进记录失败', err)
    followRecords.value = []
  }
  detailLoading.value = false
}

function handleView(record: any) {
  detailVisible.value = true
  detailData.value = {}
  fetchDetail(record.id)
}

function handleDetailClose() {
  detailVisible.value = false
  detailData.value = {}
  followRecords.value = []
  detailError.value = false
}

function handleDetailRefresh() {
  if (detailData.value.id) fetchDetail(detailData.value.id)
}

// ═══ 移动阶段（真实 PUT /crm/opportunity/{id}/stage，阶段值域数字 1-5） ═══
const moveVisible = ref(false)
const moveData = reactive<{ id: any; opportunityName: string; currentStage: number; targetStage: number | undefined }>({
  id: undefined,
  opportunityName: '',
  currentStage: 1,
  targetStage: undefined,
})
const moveTargetOptions = computed(() =>
  STAGE_OPTIONS.filter(s => s.value !== moveData.currentStage)
)

function handleMove(record: any) {
  if (!isDraggable(record)) {
    message.warning('已赢单 / 已输单的商机不能再变更阶段')
    return
  }
  moveData.id = record.id
  moveData.opportunityName = record.opportunityName || ''
  moveData.currentStage = Number(record.opportunityStage) || 0
  moveData.targetStage = undefined
  moveVisible.value = true
}

async function handleMoveConfirm() {
  if (!moveData.targetStage) {
    message.warning('请选择目标阶段')
    return
  }
  moveLoading.value = true
  try {
    await opportunityApi.updateStage(moveData.id, moveData.targetStage)
    message.success('商机阶段已更新')
    moveVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '移动失败')
  } finally {
    moveLoading.value = false
  }
}

// ═══ 看板拖拽改阶段（与弹窗共用同一端点，避免同页面两套行为） ═══
const dragData = ref<{ opp: any; sourceStage: number } | null>(null)

function handleDragStart(event: DragEvent, opp: any, sourceStage: number) {
  if (!isDraggable(opp)) return
  dragData.value = { opp, sourceStage }
  // 雪花/BIGINT 主键一律按字符串处理，禁止 Number() 转换
  event.dataTransfer?.setData('text/plain', String(opp.id))
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}

function handleDrop(event: DragEvent, targetStage: number) {
  const dragged = dragData.value
  dragData.value = null
  if (!dragged) return
  const { opp, sourceStage } = dragged
  if (sourceStage === targetStage) return
  Modal.confirm({
    title: '移动商机阶段',
    content: `将「${opp.opportunityName}」从「${STAGE_TEXT[sourceStage] || '未设置'}」移动到「${STAGE_TEXT[targetStage]}」？`,
    okText: '确认移动',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await opportunityApi.updateStage(opp.id, targetStage)
        message.success('商机阶段已更新')
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '移动失败')
      }
    },
  })
}

// ═══ 添加跟进（真实 POST /crm/followUp） ═══
const followVisible = ref(false)
const followSaving = ref(false)
const followForm = reactive({
  opportunityId: undefined as any,
  opportunityName: '',
  customerId: undefined as any,
  customerName: '',
  followUpType: 1,
  followUpDate: dayjs().format('YYYY-MM-DD'),
  content: '',
  followUpResult: undefined as number | undefined,
  nextFollowUpDate: undefined as string | undefined,
})

function openFollowModal(record: any) {
  followForm.opportunityId = record.id
  followForm.opportunityName = record.opportunityName || ''
  followForm.customerId = record.customerId
  followForm.customerName = record.customerName || ''
  followForm.followUpType = 1
  followForm.followUpDate = dayjs().format('YYYY-MM-DD')
  followForm.content = ''
  followForm.followUpResult = undefined
  followForm.nextFollowUpDate = undefined
  followVisible.value = true
}

async function handleFollowSubmit() {
  if (!followForm.content.trim()) {
    message.warning('请输入跟进内容')
    return
  }
  followSaving.value = true
  try {
    await followUpApi.create({
      opportunityId: followForm.opportunityId,
      opportunityName: followForm.opportunityName,
      customerId: followForm.customerId,
      customerName: followForm.customerName,
      followUpType: followForm.followUpType,
      followUpTypeDesc: FOLLOW_UP_TYPE_TEXT[followForm.followUpType],
      followUpDate: followForm.followUpDate,
      content: followForm.content.trim(),
      followUpResult: followForm.followUpResult,
      followUpResultDesc: followForm.followUpResult ? FOLLOW_UP_RESULT_TEXT[followForm.followUpResult] : undefined,
      nextFollowUpDate: followForm.nextFollowUpDate,
      salesPersonId: userStore.userId || undefined,
      salesPersonName: userStore.nickname || userStore.username || undefined,
    })
    message.success('跟进记录已保存')
    followVisible.value = false
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    followSaving.value = false
  }
}

// ═══ 关闭商机（复用后端 /{id}/lose，必须填输单原因） ═══
const closeVisible = ref(false)
const closeSaving = ref(false)
const closeForm = reactive<{ id: any; opportunityName: string; loseReason: string }>({
  id: undefined,
  opportunityName: '',
  loseReason: '',
})

function openCloseModal(record: any) {
  closeForm.id = record.id
  closeForm.opportunityName = record.opportunityName || ''
  closeForm.loseReason = ''
  closeVisible.value = true
}

async function handleCloseConfirm() {
  if (!closeForm.loseReason.trim()) {
    message.warning('请输入输单原因')
    return
  }
  closeSaving.value = true
  try {
    await opportunityApi.lose(closeForm.id, closeForm.loseReason.trim())
    message.success('商机已关闭')
    closeVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '关闭失败')
  } finally {
    closeSaving.value = false
  }
}

// ═══ 行内「更多」菜单 ═══
function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'follow':
      openFollowModal(record)
      break
    case 'quotation':
      // 目标链路：POST /api/crm/quotation/from-opportunity/{id} → 报价单 → 转销售订单
      // 该端点在本模块 api 封装中尚无对应方法（api/crm.ts 未暴露），暂以提示代替，不伪造成功
      message.info('请先赢单，再到「CRM → 报价单」由商机创建报价，经报价单转销售订单')
      break
    case 'close':
      openCloseModal(record)
      break
  }
}

/** 转订单：CRM 不得直写 ERP 销售订单（须经报价单链路），此处只做引导，不新增该能力 */
function handleConvert(record: any) {
  Modal.info({
    title: '转订单说明',
    content: `商机「${record.opportunityName}」转订单须经「报价单 → 销售订单」链路：CRM 客户不是 ERP 往来单位，不能直接写入 ERP 销售订单。`,
    okText: '知道了',
    centered: true,
  })
}

// ═══ 导出（全量 + 中文映射） ═══
async function handleExport() {
  try {
    const list: any = await opportunityStageApi.exportList(buildQuery())
    const rows = Array.isArray(list) ? list : []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['商机编号', '商机名称', '客户', '商机阶段', '赢单概率', '预计金额',
      '成交金额', '状态', '负责人', '预计成交日期', '商机类型', '商机来源']
    const body = rows.map((r: any) => [
      r.opportunityCode || '',
      r.opportunityName || '',
      r.customerName || '',
      stageText(r),
      `${Number(r.probability) || 0}%`,
      formatAmount(r.estimatedAmount),
      formatAmount(r.actualAmount),
      statusText(r),
      r.salesPersonName || '',
      r.expectedCloseDate || '',
      typeText(r.opportunityType),
      sourceText(r.opportunitySource),
    ])
    exportCsv(headers, body, `商机_${dayjs().format('YYYYMMDD_HHmmss')}`)
    message.success('导出成功')
  } catch (e: any) {
    message.error(e?.message || '导出失败')
  }
}

// ═══ 快捷键 / 跨页事件 / 自动刷新 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    handleRefresh()
    return
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}
function handleParentCreate() { handleAdd() }

function handleError(error: Error) {
  console.error('[商机] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

async function loadOptions() {
  try {
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
  } catch (e) {
    console.warn('[商机] 客户下拉获取失败', e)
  }
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200, status: 1 })
    const records = res?.records || []
    userOptions.value = records.map((u: any) => ({ label: u.nickname || u.username, value: u.id }))
  } catch (e) {
    console.warn('[商机] 负责人下拉获取失败', e)
  }
}

onMounted(async () => {
  await loadOptions()
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('crm:create', handleParentCreate)
  window.addEventListener('crm:refresh', handleRefresh)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  disposeCharts()
  window.removeEventListener('crm:create', handleParentCreate)
  window.removeEventListener('crm:refresh', handleRefresh)
  document.removeEventListener('keydown', handleKeydown)
})

onBeforeRouteLeave((_to, _from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单内容尚未保存，确定要离开吗？',
      centered: true,
      onOk: () => next(),
      onCancel: () => next(false),
    })
  } else {
    next()
  }
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
/* 查询区（插槽内容样式必须自备：scoped 不作用于布局组件内的插槽内容） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 180px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px;
  background: #f5f7fa; user-select: none;
}

/* 橙色新增按钮（CRM 模块统一） */
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

/* ── 管道看板 ── */
.pipeline-container { flex: 1; min-height: 0; display: flex; gap: 16px; overflow-x: auto; padding: 16px; background: #f5f5f5; }
.pipeline-stage {
  min-width: 280px; width: 280px; display: flex; flex-direction: column;
  background: #fff; border-radius: 8px; padding: 12px; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.stage-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; border-bottom: 2px solid; margin-bottom: 12px; flex-shrink: 0;
}
.stage-name { font-size: 14px; font-weight: 600; color: #303133; }
.stage-amount { font-size: 12px; color: #1890ff; font-weight: 500; }
.stage-cards { flex: 1; min-height: 0; overflow-y: auto; }
.opportunity-card {
  background: #fafafa; border-radius: 6px; padding: 12px; margin-bottom: 12px;
  cursor: pointer; border: 1px solid #e8e8e8; transition: all 0.2s;
}
.opportunity-card:hover { border-color: #1890ff; box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2); background: #fff; }
.card-header { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.card-title { font-size: 14px; font-weight: 500; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-body { margin-top: 8px; }
.card-row { display: flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 12px; }
.card-row .label { color: #909399; display: flex; align-items: center; gap: 4px; }
.card-row .value { color: #606266; }
.card-row .amount { color: #f5222d; font-weight: 500; }
.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: 12px; padding-top: 8px; border-top: 1px solid #f0f0f0;
}
.close-date { font-size: 12px; color: #999; }
.card-actions { display: flex; gap: 8px; }
.empty-stage { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 20px; color: #999; font-size: 12px; }
.empty-icon { font-size: 32px; margin-bottom: 8px; }

/* ── 统计视图 ── */
.statistics-area { flex: 1; min-height: 0; overflow-y: auto; padding: 16px; }
.stats-cards { margin-bottom: 16px; }
.stat-card {
  display: flex; align-items: center; padding: 16px; background: #fff;
  border-radius: 8px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); transition: all 0.3s;
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12); transform: translateY(-2px); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center;
  justify-content: center; color: #fff; font-size: 24px; margin-right: 16px; flex-shrink: 0;
}
.stat-content { flex: 1; min-width: 0; }
.stat-title { font-size: 14px; color: #666; margin-bottom: 4px; }
.stat-value {
  font-size: 24px; font-weight: 600; color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.stat-desc { font-size: 12px; color: #999; margin-top: 4px; }
.stat-desc.positive { color: #52c41a; }
.chart-container { height: 300px; }
/* 定高 flex 容器：卡片内嵌 BillDetailTable 时驱动其 flex 高度链 */
.stage-stats-wrap { height: 250px; display: flex; flex-direction: column; }

/* ── 详情抽屉 ── */
.opp-name { font-weight: 500; }
.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums; color: #f5222d; font-weight: 500;
}
.timeline-content .timeline-title { font-size: 14px; font-weight: 500; color: #303133; }
.timeline-content .timeline-desc { font-size: 12px; color: #606266; margin-top: 4px; }
.timeline-content .timeline-time { font-size: 12px; color: #909399; margin-top: 4px; }

.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }

.action-more-btn { padding: 0 4px; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
