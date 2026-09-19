<template>
  <ErrorBoundary
    @error="handleError"
    @reset="fetchData"
  >
    <PageContainer
      title="客户管理"
      full-height
    >
      <!--
        客户管理（CRM → 客户管理 → 客户，菜单 80200）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/CRM模块/客户开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillTableList
        · 左分类树 = 客户等级（全部 / 未分级 / VIP / 重要 / 普通 / 潜在）
        · 列配置齿轮在表头 rowNo 列；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 状态口径：1 = 正常 / 0 = 停用（与 crm_customer 列默认值及全模块一致）
      -->
      <template #headerExtra>
        <a-space :size="12">
          <span class="data-status">
            <a-badge :status="loading ? 'processing' : hasError ? 'error' : 'success'" />
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >
              数据更新: {{ lastUpdateTime }}
            </span>
          </span>
          <span
            v-if="autoRefreshCountdown > 0"
            class="auto-refresh-badge"
          >
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </a-space>
      </template>

      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="客户等级"
        :category-tree-data="levelTree"
        :selected-category-id="selectedLevelKey"
        :show-table-footer="currentView === 'list'"
        @category-select="handleLevelSelect"
      >
        <!-- ═══ 工具栏左侧：新增 / 导入 / 批量动作（有勾选时显示） ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            v-permission="'crm:customer:create'"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增客户
          </a-button>
          <a-button
            v-if="isButtonEnabled('import')"
            v-permission="'crm:customer:import'"
            size="small"
            @click="handleImport"
          >
            <ImportOutlined /> 导入
          </a-button>
          <a-button
            v-if="selectedRows.length > 0"
            v-permission="'crm:customer:batchassign'"
            size="small"
            type="primary"
            ghost
            @click="handleBatchAssign"
          >
            <TeamOutlined /> 批量分配（{{ selectedRows.length }}）
          </a-button>
          <a-button
            v-if="selectedRows.length > 0"
            size="small"
            danger
            @click="handleBatchDelete"
          >
            <DeleteOutlined /> 批量删除
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：列表/看板切换 / 刷新 / 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-radio-group
              v-model:value="currentView"
              button-style="solid"
              size="small"
            >
              <a-radio-button value="list">
                <UnorderedListOutlined /> 列表
              </a-radio-button>
              <a-radio-button value="kanban">
                <AppstoreOutlined /> 看板
              </a-radio-button>
            </a-radio-group>
            <a-button
              v-if="isButtonEnabled('refresh')"
              v-permission="'crm:customer:refresh'"
              size="small"
              :loading="refreshLoading"
              @click="debounceClick('refresh', handleRefresh)"
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
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="客户名称/编码"
                  size="small"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('customerLevel')"
                class="search-item"
              >
                <span class="search-label">客户等级</span>
                <a-select
                  v-model:value="searchForm.customerLevel"
                  placeholder="全部等级"
                  size="small"
                  allow-clear
                  :options="LEVEL_OPTIONS"
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
                v-if="isFieldVisible('customerType')"
                class="search-item"
              >
                <span class="search-label">客户类型</span>
                <a-select
                  v-model:value="searchForm.customerType"
                  placeholder="全部类型"
                  size="small"
                  allow-clear
                  :options="CUSTOMER_TYPE_OPTIONS"
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
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleResetFilters"
                >
                  重置
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据区：列表（统计卡 + 表格）/ 看板 ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 骨架加载（首屏） -->
            <div
              v-if="loading && dataSource.length === 0"
              class="skeleton-loading"
            >
              <a-skeleton
                :paragraph="{ rows: 3 }"
                active
              />
              <div style="height: 16px" />
              <a-skeleton
                :paragraph="{ rows: 8 }"
                active
              />
            </div>

            <!-- 列表视图 -->
            <template v-else-if="currentView === 'list'">
              <!-- 统计卡片（当前页数据聚合） -->
              <div class="stats-cards">
                <a-row :gutter="16">
                  <a-col :span="6">
                    <div class="stat-card stat-card-purple">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #ff4d4f 0%, #f5222d 100%);"
                      >
                        <CrownOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          VIP客户
                        </div>
                        <div class="stat-value">
                          {{ levelCounts.vip }}
                        </div>
                        <div class="stat-desc">
                          核心客户群
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
                        <StarOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          重要客户
                        </div>
                        <div class="stat-value">
                          {{ levelCounts.important }}
                        </div>
                        <div class="stat-desc">
                          重点跟进
                        </div>
                      </div>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="stat-card stat-card-blue">
                      <div
                        class="stat-icon"
                        style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"
                      >
                        <UserOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          普通客户
                        </div>
                        <div class="stat-value">
                          {{ levelCounts.normal }}
                        </div>
                        <div class="stat-desc">
                          稳定合作
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
                        <UsergroupAddOutlined />
                      </div>
                      <div class="stat-content">
                        <div class="stat-title">
                          潜在客户
                        </div>
                        <div class="stat-value">
                          {{ levelCounts.potential }}
                        </div>
                        <div class="stat-desc">
                          规划中
                        </div>
                      </div>
                    </div>
                  </a-col>
                </a-row>
              </div>

              <!-- 数据表：工具栏/查询区由 CategoryListLayout 提供，分页由底部经典分页栏提供 -->
              <BillTableList
                ref="tableRef"
                :columns="columns"
                :data-source="tableDataSource"
                :loading="loading"
                :pagination="false"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                :selectable="true"
                row-key="id"
                storage-key="crm-customer-table-columns"
                global-config-key="crm-customer-table-columns"
                @selection-change="handleSelectionChange"
              >
                <template #customerNameCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span
                    v-else
                    class="customer-name cell-link"
                    @click="handleView(record)"
                  >{{ record.customerName || '-' }}</span>
                </template>

                <template #customerLevelCell="{ record }">
                  <a-tag
                    v-if="!record.__ghost && record.customerLevel"
                    :color="getLevelColor(record.customerLevel)"
                  >
                    {{ getLevelName(record.customerLevel) }}
                  </a-tag>
                  <span v-else-if="record.__ghost" />
                  <span v-else>未分级</span>
                </template>

                <template #industryCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span v-else>{{ getIndustryName(record.industryType) }}</span>
                </template>

                <template #customerTypeCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span v-else>{{ getCustomerTypeName(record.customerType) }}</span>
                </template>

                <template #customerSourceCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span v-else>{{ getCustomerSourceName(record.customerSource) }}</span>
                </template>

                <!-- 状态列：1 = 正常 / 0 = 停用（此前遗漏该插槽，导致直出数字） -->
                <template #statusCell="{ record }">
                  <a-tag
                    v-if="!record.__ghost"
                    :color="record.status === 1 ? 'success' : 'default'"
                  >
                    {{ record.status === 1 ? '正常' : '停用' }}
                  </a-tag>
                  <span v-else />
                </template>

                <template #actionCell="{ record }">
                  <a-space
                    v-if="!record.__ghost"
                    :size="4"
                  >
                    <a-tooltip
                      title="查看详情"
                      placement="bottom"
                    >
                      <a-button
                        v-permission="'crm:customer:view'"
                        type="link"
                        size="small"
                        @click="handleView(record)"
                      >
                        <template #icon>
                          <EyeOutlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip
                      title="编辑"
                      placement="bottom"
                    >
                      <a-button
                        v-permission="'crm:customer:edit'"
                        type="link"
                        size="small"
                        @click="handleEdit(record)"
                      >
                        <template #icon>
                          <EditOutlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip
                      title="跟进"
                      placement="bottom"
                    >
                      <a-button
                        v-permission="'crm:customer:follow'"
                        type="link"
                        size="small"
                        @click="handleFollow(record)"
                      >
                        <template #icon>
                          <MessageOutlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-dropdown trigger="click">
                      <a-button
                        type="link"
                        size="small"
                        class="action-more-btn"
                      >
                        <template #icon>
                          <MoreOutlined />
                        </template>
                      </a-button>
                      <template #overlay>
                        <a-menu @click="({ key }) => handleActionMenuClick(String(key), record)">
                          <a-menu-item key="follows">
                            <HistoryOutlined /> 跟进记录
                          </a-menu-item>
                          <a-menu-item key="orders">
                            <FileTextOutlined /> 订单记录
                          </a-menu-item>
                          <a-menu-item key="contracts">
                            <SolutionOutlined /> 合同记录
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
            </template>

            <!-- 看板视图（按客户等级分 4 列） -->
            <div
              v-else
              class="kanban-container"
            >
              <div
                v-for="level in LEVEL_GROUPS"
                :key="level.value"
                class="kanban-column"
              >
                <div class="kanban-column-header">
                  <span class="kanban-column-title">
                    <a-tag
                      :color="getLevelColor(level.value)"
                      size="small"
                    >{{ level.label }}</a-tag>
                  </span>
                  <span class="kanban-column-count">{{ getCustomersByLevel(level.value).length }} 个</span>
                  <a-button
                    v-permission="'crm:customer:addtolevel'"
                    type="link"
                    size="small"
                    title="新增该等级客户"
                    @click="handleAddToLevel(level.value)"
                  >
                    <template #icon>
                      <PlusOutlined />
                    </template>
                  </a-button>
                </div>
                <div class="kanban-column-body">
                  <div
                    v-for="customer in getCustomersByLevel(level.value)"
                    :key="customer.id"
                    class="kanban-card"
                    @click="handleView(customer)"
                  >
                    <div class="kanban-card-header">
                      <a-space>
                        <a-avatar
                          :style="{ backgroundColor: getLevelColor(customer.customerLevel) }"
                          size="small"
                        >
                          {{ customer.customerName?.charAt(0) }}
                        </a-avatar>
                        <span class="kanban-card-name">{{ customer.customerName }}</span>
                      </a-space>
                      <a-tag
                        :color="customer.status === 1 ? 'success' : 'default'"
                        size="small"
                      >
                        {{ customer.status === 1 ? '正常' : '停用' }}
                      </a-tag>
                    </div>
                    <div class="kanban-card-body">
                      <div class="kanban-card-row">
                        <span class="kanban-card-label"><UserOutlined /> 联系人:</span>
                        <span class="kanban-card-value">{{ customer.businessContact || '-' }}</span>
                      </div>
                      <div class="kanban-card-row">
                        <span class="kanban-card-label"><PhoneOutlined /> 电话:</span>
                        <span class="kanban-card-value">{{ customer.phone || '-' }}</span>
                      </div>
                      <div class="kanban-card-row">
                        <span class="kanban-card-label"><HomeOutlined /> 行业:</span>
                        <span class="kanban-card-value">{{ getIndustryName(customer.industryType) }}</span>
                      </div>
                    </div>
                    <div class="kanban-card-footer">
                      <a-button
                        type="link"
                        size="small"
                        @click.stop="handleFollow(customer)"
                      >
                        <template #icon>
                          <MessageOutlined />
                        </template>
                        跟进
                      </a-button>
                    </div>
                  </div>
                  <div
                    v-if="getCustomersByLevel(level.value).length === 0"
                    class="kanban-empty"
                  >
                    暂无客户
                  </div>
                </div>
              </div>
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

      <!-- ═══ 批量分配（真实写 salesPersonId / salesPersonName） ═══ -->
      <a-modal
        v-model:open="assignVisible"
        title="批量分配"
        :confirm-loading="assignLoading"
        ok-text="确定分配"
        cancel-text="取消"
        :width="480"
        @ok="handleBatchAssignOk"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="客户数">
            <span>{{ selectedRows.length }} 个</span>
          </a-form-item>
          <a-form-item
            label="负责人"
            required
          >
            <a-select
              v-model:value="assignUserId"
              placeholder="请选择负责人"
              show-search
              allow-clear
              :filter-option="filterOption"
              :options="userOptions"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 跟进记录（真实接口 GET /api/crm/followUp/customer/{id}） ═══ -->
      <a-modal
        v-model:open="followRecordsVisible"
        :title="`跟进记录 - ${followRecordsCustomerName}`"
        :footer="null"
        :width="760"
      >
        <a-table
          :columns="followRecordColumns"
          :data-source="followRecords"
          :loading="followRecordsLoading"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'followUpType'">
              {{ followUpTypeText(record.followUpType) }}
            </template>
            <template v-else-if="column.key === 'followUpResult'">
              {{ followUpResultText(record.followUpResult) }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 全屏详情抽屉（新建/编辑客户） ═══ -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="modalTitle"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        :dirty="formDirty"
        @close="handleFormClose"
        @save="handleModalOk"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="formRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="客户名称"
                name="customerName"
              >
                <a-input
                  v-model:value="formState.customerName"
                  placeholder="请输入客户名称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="客户编码"
                name="customerCode"
              >
                <a-input
                  v-model:value="formState.customerCode"
                  placeholder="请输入客户编码"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="联系人"
                name="businessContact"
              >
                <a-input
                  v-model:value="formState.businessContact"
                  placeholder="请输入联系人"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="联系电话"
                name="phone"
              >
                <a-input
                  v-model:value="formState.phone"
                  placeholder="请输入联系电话"
                  size="small"
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
                  placeholder="请输入邮箱"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="客户等级"
                name="customerLevel"
              >
                <a-select
                  v-model:value="formState.customerLevel"
                  placeholder="请选择等级"
                  size="small"
                  :options="LEVEL_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="行业"
                name="industryType"
              >
                <a-select
                  v-model:value="formState.industryType"
                  placeholder="请选择行业"
                  size="small"
                  :options="INDUSTRY_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="状态"
                name="status"
              >
                <a-radio-group v-model:value="formState.status">
                  <a-radio :value="1">
                    正常
                  </a-radio>
                  <a-radio :value="0">
                    停用
                  </a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="地址"
                name="address"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-input
                  v-model:value="formState.address"
                  placeholder="请输入地址"
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
                  v-model:value="formState.remark"
                  placeholder="请输入备注"
                  :rows="3"
                  size="small"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </FullScreenDetail>

      <!-- ═══ 添加跟进记录弹窗（真实接口 POST /api/customer/{id}/follow） ═══ -->
      <a-modal
        v-model:open="followModalVisible"
        title="添加跟进记录"
        :confirm-loading="followModalLoading"
        width="600px"
        @ok="handleFollowModalOk"
      >
        <a-form
          :model="followForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="跟进类型"
            required
          >
            <a-select
              v-model:value="followForm.followType"
              placeholder="请选择跟进类型"
              size="small"
            >
              <a-select-option :value="1">
                电话
              </a-select-option>
              <a-select-option :value="2">
                拜访
              </a-select-option>
              <a-select-option :value="3">
                邮件
              </a-select-option>
              <a-select-option :value="4">
                微信
              </a-select-option>
              <a-select-option :value="5">
                其他
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            label="跟进内容"
            required
          >
            <a-textarea
              v-model:value="followForm.content"
              placeholder="请输入跟进内容"
              :rows="4"
              size="small"
            />
          </a-form-item>
          <a-form-item label="跟进结果">
            <a-select
              v-model:value="followForm.result"
              placeholder="请选择跟进结果"
              size="small"
            >
              <a-select-option :value="1">
                有意向
              </a-select-option>
              <a-select-option :value="2">
                无意向
              </a-select-option>
              <a-select-option :value="3">
                待跟进
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="下次跟进时间">
            <a-date-picker
              v-model:value="followForm.nextFollowTime"
              style="width: 100%"
              size="small"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ CSV 导入弹窗（真实落库 POST /api/customer/import） ═══ -->
      <a-modal
        v-model:open="importVisible"
        title="导入客户"
        width="700px"
        :confirm-loading="importLoading"
        @ok="handleImportConfirm"
        @cancel="importVisible = false"
      >
        <a-steps
          :current="importFileList.length > 0 ? 1 : 0"
          size="small"
          style="margin-bottom: 24px"
        >
          <a-step title="上传文件" />
          <a-step title="字段映射" />
        </a-steps>
        <a-upload
          :file-list="importFileList"
          :before-upload="() => false"
          accept=".csv"
          :max-count="1"
          @change="handleImportFileChange"
        >
          <a-button>
            <template #icon>
              <PlusOutlined />
            </template>
            选择CSV文件
          </a-button>
        </a-upload>
        <a-divider>字段映射</a-divider>
        <BillTableList
          :columns="importMappingColumns"
          :data-source="importFieldMapping"
          :pagination="false as any"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #csvFieldCell="{ record }">
            <a-input
              v-model:value="record.csvField"
              placeholder="CSV列名"
              size="small"
            />
          </template>
          <template #requiredCell="{ record }">
            <a-tag :color="record.required ? 'red' : 'default'">
              {{ record.required ? '是' : '否' }}
            </a-tag>
          </template>
        </BillTableList>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  ImportOutlined,
  MoreOutlined,
  MessageOutlined,
  UnorderedListOutlined,
  AppstoreOutlined,
  ReloadOutlined,
  DownloadOutlined,
  CrownOutlined,
  StarOutlined,
  UserOutlined,
  UsergroupAddOutlined,
  TeamOutlined,
  HistoryOutlined,
  FileTextOutlined,
  SolutionOutlined,
  PhoneOutlined,
  HomeOutlined,
  SettingOutlined,
  SyncOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { customerApi } from '@/api/customer'
import { followUpApi, crmCustomerApi, type FollowUpRecord } from '@/api/crm'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'CrmCustomer' })

const userStore = useUserStore()
const router = useRouter()
const tableRef = ref()

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleError(err: any) { console.warn('[CRM客户]', err) }

// ═══ 字典（值域与 crm_customer 列口径一致） ═══
const LEVEL_OPTIONS = [
  { label: 'VIP客户', value: 1 },
  { label: '重要客户', value: 2 },
  { label: '普通客户', value: 3 },
  { label: '潜在客户', value: 4 }
]
const LEVEL_GROUPS = LEVEL_OPTIONS
// 状态：全模块统一 1 = 正常 / 0 = 停用（与 crm_customer.status 列默认值一致）
const STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '停用', value: 0 }
]
const CUSTOMER_TYPE_OPTIONS = [
  { label: '经销商', value: 1 },
  { label: '零售商', value: 2 },
  { label: '餐饮店', value: 3 },
  { label: '其他', value: 4 }
]
const INDUSTRY_OPTIONS = [
  { label: '食品加工', value: 1 },
  { label: '餐饮服务', value: 2 },
  { label: '批发零售', value: 3 },
  { label: '其他', value: 4 }
]
const LEVEL_COLOR_MAP: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
const LEVEL_TEXT_MAP: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
const INDUSTRY_TEXT_MAP: Record<number, string> = { 1: '食品加工', 2: '餐饮服务', 3: '批发零售', 4: '其他' }
const CUSTOMER_TYPE_TEXT_MAP: Record<number, string> = { 1: '经销商', 2: '零售商', 3: '餐饮店', 4: '其他' }
const CUSTOMER_SOURCE_TEXT_MAP: Record<number, string> = { 1: '自主开发', 2: '转介绍', 3: '网络推广', 4: '展会', 5: '其他' }
// 跟进字典（与客户跟进页逐字一致）
const FOLLOW_UP_TYPE_TEXT: Record<number, string> = { 1: '电话', 2: '拜访', 3: '邮件', 4: '微信', 5: '其他' }
const FOLLOW_UP_RESULT_TEXT: Record<number, string> = { 1: '有意向', 2: '无意向', 3: '待跟进' }

function getLevelColor(level: number): string { return LEVEL_COLOR_MAP[level] || '#999' }
function getLevelName(level: number): string { return LEVEL_TEXT_MAP[level] || '未知' }
function getIndustryName(v: number | string | undefined): string {
  if (v === undefined || v === null || v === '') return '-'
  if (typeof v === 'number') return INDUSTRY_TEXT_MAP[v] || '未知'
  return String(v)
}
function getCustomerTypeName(v: number | undefined): string {
  return (v !== undefined && v !== null && CUSTOMER_TYPE_TEXT_MAP[v]) || '-'
}
function getCustomerSourceName(v: number | undefined): string {
  return (v !== undefined && v !== null && CUSTOMER_SOURCE_TEXT_MAP[v]) || '-'
}
function followUpTypeText(v: number | undefined): string {
  return (v !== undefined && v !== null && FOLLOW_UP_TYPE_TEXT[v]) || '-'
}
function followUpResultText(v: number | undefined): string {
  return (v !== undefined && v !== null && FOLLOW_UP_RESULT_TEXT[v]) || '-'
}
function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input || '').toLowerCase())
}

// ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键字（客户名称/编码）', visible: true },
  { key: 'customerLevel', label: '客户等级', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'customerType', label: '客户类型', visible: true },
  { key: 'salesPersonId', label: '负责人', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新增客户', enabled: true },
  { key: 'import', label: '导入', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-customer-page-config'

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

// ═══ 左分类树：客户等级（全部 / 未分级 / 4 个等级） ═══
const selectedLevelKey = ref<string | number>('all')
const levelTree = computed(() => [
  { id: 'all', categoryName: '全部客户' },
  { id: 'none', categoryName: '未分级' },
  ...LEVEL_OPTIONS.map(o => ({ id: String(o.value), categoryName: o.label }))
])

function handleLevelSelect(keys: (string | number)[]) {
  const key = keys && keys.length ? keys[0] : 'all'
  selectedLevelKey.value = key
  if (key === 'all') {
    searchForm.customerLevel = undefined
    searchForm.unleveled = undefined
  } else if (key === 'none') {
    searchForm.customerLevel = undefined
    searchForm.unleveled = true
  } else {
    searchForm.customerLevel = Number(key)
    searchForm.unleveled = undefined
  }
  pagination.current = 1
  fetchData()
}

// ═══ 查询条件（后端 GET /api/customer/page 支持的参数） ═══
const searchForm = reactive({
  keyword: '',
  customerLevel: undefined as number | undefined,
  status: undefined as number | undefined,
  customerType: undefined as number | undefined,
  salesPersonId: undefined as any,
  /** 「未分级」节点：后端无该参数，在本页返回集上二次过滤 */
  unleveled: undefined as boolean | undefined,
})
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const hasError = ref(false)
const dataSource = ref<any[]>([])
const tableDataSource = dataSource
const currentView = ref('list')
const kanbanData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const selectedRows = ref<any[]>([])
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let kanbanLoading = false
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ═══ 表格列（rowNo 承载列配置齿轮；checkbox / action 为锁定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'customerCode', title: '客户编码', type: 'input', width: 130 },
  { key: 'customerName', title: '客户信息', type: 'slot', slotName: 'customerNameCell', width: 200 },
  { key: 'businessContact', title: '联系人', type: 'input', width: 100 },
  { key: 'phone', title: '联系电话', type: 'input', width: 120 },
  { key: 'customerLevel', title: '客户等级', type: 'slot', slotName: 'customerLevelCell', width: 100, align: 'center' },
  { key: 'industryType', title: '行业', type: 'slot', slotName: 'industryCell', width: 100 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 80, align: 'center' },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  // 以下为默认隐藏列（表头齿轮可开启）
  { key: 'customerType', title: '客户类型', type: 'slot', slotName: 'customerTypeCell', width: 100, defaultHidden: true },
  { key: 'customerSource', title: '客户来源', type: 'slot', slotName: 'customerSourceCell', width: 100, defaultHidden: true },
  { key: 'shortName', title: '客户简称', type: 'input', width: 120, defaultHidden: true },
  { key: 'email', title: '邮箱', type: 'input', width: 160, defaultHidden: true },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
  // 注意：Customer 实体映射的是 createdAt（createTime 列在实体上不存在，取值恒空）
  { key: 'createdAt', title: '创建时间', type: 'input', width: 160 },
  { key: 'lastTradeDate', title: '最近交易', type: 'input', width: 120, defaultHidden: true },
]

// ═══ 负责人下拉（查询筛选 + 批量分配共用） ═══
const userOptions = ref<{ label: string; value: any }[]>([])

async function loadUserOptions() {
  try {
    const res: any = await userApi.getPage({ pageNum: 1, pageSize: 200, status: 1 })
    const records = res?.records || res?.data?.records || []
    userOptions.value = records.map((u: any) => ({ label: u.nickname || u.username, value: u.id }))
  } catch (e) {
    console.warn('[CRM客户] 负责人下拉获取失败', e)
  }
  // 兜底：用户列表不可用时至少保证「当前登录用户」可选，批量分配仍可用
  if (!userOptions.value.length && userStore.userId) {
    userOptions.value = [{ label: `${userStore.nickname || userStore.username || '当前用户'}（当前登录）`, value: userStore.userId }]
  }
}

// ═══ 状态统计（当前页数据聚合） ═══
const levelCounts = computed(() => {
  const vip = dataSource.value.filter(c => c.customerLevel === 1).length
  const important = dataSource.value.filter(c => c.customerLevel === 2).length
  const normal = dataSource.value.filter(c => c.customerLevel === 3).length
  const potential = dataSource.value.filter(c => c.customerLevel === 4).length
  return { vip, important, normal, potential }
})

// ═══ 看板分组（按实体字段 customerLevel） ═══
const getCustomersByLevel = (level: number) => kanbanData.value.filter(c => Number(c.customerLevel) === Number(level))

// 切到看板视图时加载全部客户数据用于看板展示
const fetchKanbanData = async () => {
  if (kanbanData.value.length > 0 || kanbanLoading) return
  kanbanLoading = true
  try {
    const res: any = await customerApi.getPage({ pageNum: 1, pageSize: 9999 })
    const pageData = res?.data ?? res
    kanbanData.value = pageData?.records || []
  } catch (err) {
    console.warn('[CRM客户] 加载看板数据失败', err)
    kanbanData.value = []
  } finally {
    kanbanLoading = false
  }
}

// ═══ 自动刷新（30 秒） ═══
const startAutoRefresh = () => {
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (!loading.value && !modalVisible.value && !followModalVisible.value) {
      fetchData(true)
    }
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

const stopAutoRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

// ═══ 数据加载 ═══
async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    // 后端 /customer/page 仅支持 keyword / customerType / customerLevel / status / salesPersonId
    const params: Record<string, any> = {
      keyword: searchForm.keyword || undefined,
      customerLevel: searchForm.customerLevel,
      status: searchForm.status,
      customerType: searchForm.customerType,
      salesPersonId: searchForm.salesPersonId,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    const res: any = await customerApi.getPage(params)
    const pageData = res?.data ?? res
    let records = pageData?.records || []
    // 「未分级」是左树节点，后端无该参数 —— 在本页返回集上二次过滤（与客户分级页口径一致）
    if (searchForm.unleveled) {
      records = records.filter((r: any) => !r.customerLevel)
    }
    dataSource.value = records
    pagination.total = Number(pageData?.total) || 0
    // 用户发起的查询/翻页/刷新后清空勾选：换一页数据后旧勾选会指向别的行（勾选漂移）
    if (!silent) clearTableSelection()
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('加载客户列表失败')
    }
    console.warn('[CRM客户] 加载客户列表失败', err)
    dataSource.value = []
    pagination.total = 0
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

// ═══ 刷新 / 查询 / 分页 ═══
const handleRefresh = async () => {
  lastUpdateTime.value = ''
  kanbanData.value = []
  refreshLoading.value = true
  await fetchData()
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleResetFilters() {
  searchForm.keyword = ''
  searchForm.customerLevel = undefined
  searchForm.status = undefined
  searchForm.customerType = undefined
  searchForm.salesPersonId = undefined
  searchForm.unleveled = undefined
  selectedLevelKey.value = 'all'
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows || []
}

function clearTableSelection() {
  tableRef.value?.clearSelection?.()
  selectedRows.value = []
}

// ═══ 行操作 ═══
function handleView(record: any) { router.push(`/crm/customer/${record.id}`) }

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'follows':
      openFollowRecords(record)
      break
    case 'orders':
      // 后端 GET /api/customer/{id}/orders 是固定返回 List.of() 的桩 —— 不造假数据，明确提示
      message.info(`「${record.customerName}」暂无订单记录：后端 /customer/{id}/orders 为固定空返回，订单能力未开放`)
      break
    case 'contracts':
      // 合同未接线：后端 GET /api/crm/contract/customer/{id} 存在，但前端 api 层无对应方法
      message.info(`合同记录未接入本页：请在「合同」页按客户筛选查看「${record.customerName}」的合同`)
      break
    case 'delete':
      handleDelete(record)
      break
  }
}

async function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户"${record.customerName}"吗？`,
    centered: true,
    async onOk() {
      try {
        await customerApi.delete(record.id)
        message.success('删除成功')
        kanbanData.value = []
        fetchData()
      } catch (err) {
        console.warn('[CRM客户] 删除客户失败', err)
        message.error('删除失败')
      }
    }
  })
}

// ═══ 批量删除（此前按钮无监听，点击无反应） ═══
async function handleBatchDelete() {
  if (selectedRows.value.length === 0) { message.warning('请选择要删除的客户'); return }
  const ids = selectedRows.value.map(r => r.id)
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${ids.length} 个客户吗？`,
    centered: true,
    async onOk() {
      try {
        await customerApi.batchDelete(ids)
        message.success(`已删除 ${ids.length} 个客户`)
        clearTableSelection()
        kanbanData.value = []
        fetchData()
      } catch (err) {
        console.warn('[CRM客户] 批量删除客户失败', err)
        message.error('批量删除失败')
      }
    }
  })
}

// ═══ 批量分配（真实写 salesPersonId / salesPersonName） ═══
const assignVisible = ref(false)
const assignLoading = ref(false)
const assignUserId = ref<any>(undefined)

function handleBatchAssign() {
  if (selectedRows.value.length === 0) { message.warning('请选择要分配的客户'); return }
  assignUserId.value = undefined
  assignVisible.value = true
}

async function handleBatchAssignOk() {
  if (!assignUserId.value) { message.warning('请选择负责人'); return }
  const user = userOptions.value.find(o => String(o.value) === String(assignUserId.value))
  const salesPersonName = user?.label
  const targets = [...selectedRows.value]
  assignLoading.value = true
  let okCount = 0
  let failCount = 0
  for (const row of targets) {
    try {
      await crmCustomerApi.update(row.id, {
        salesPersonId: assignUserId.value,
        salesPersonName
      })
      okCount++
    } catch (e) {
      console.warn('[CRM客户] 批量分配失败', row?.id, e)
      failCount++
    }
  }
  assignLoading.value = false
  assignVisible.value = false
  if (okCount > 0) message.success(`已分配 ${okCount} 个客户${failCount > 0 ? `，${failCount} 个失败` : ''}`)
  else message.error('批量分配失败')
  clearTableSelection()
  fetchData()
}

// ═══ 跟进记录查看（真实接口 GET /api/crm/followUp/customer/{id}） ═══
const followRecordsVisible = ref(false)
const followRecordsLoading = ref(false)
const followRecords = ref<FollowUpRecord[]>([])
const followRecordsCustomerName = ref('')
const followRecordColumns = [
  { title: '跟进日期', dataIndex: 'followUpDate', key: 'followUpDate', width: 110 },
  { title: '类型', dataIndex: 'followUpType', key: 'followUpType', width: 80 },
  { title: '跟进内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '结果', dataIndex: 'followUpResult', key: 'followUpResult', width: 90 },
  { title: '下次跟进', dataIndex: 'nextFollowUpDate', key: 'nextFollowUpDate', width: 110 },
  { title: '跟进人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 }
]

async function openFollowRecords(record: any) {
  followRecordsCustomerName.value = record.customerName || ''
  followRecords.value = []
  followRecordsVisible.value = true
  followRecordsLoading.value = true
  try {
    const list = await followUpApi.listByCustomer(record.id)
    followRecords.value = Array.isArray(list) ? list : []
  } catch (e: any) {
    console.warn('[CRM客户] 加载跟进记录失败', e)
    message.error(e?.message || '加载跟进记录失败')
  } finally {
    followRecordsLoading.value = false
  }
}

// ═══ 新增 / 编辑客户弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑客户' : '新增客户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const formState = reactive({
  id: 0,
  customerName: '',
  customerCode: '',
  businessContact: '',
  phone: '',
  email: '',
  address: '',
  customerLevel: 3,
  industryType: undefined as number | undefined,
  status: 1,
  remark: ''
})
const formRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur', type: 'string' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur', type: 'string' }]
} as any

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formState })
}
const formDirty = computed(() => {
  if (!modalVisible.value) return false
  return JSON.stringify({ ...formState }) !== initialFormSnapshot.value
})

// ── 路由离开守卫 ─────────────────────────────────────────
onBeforeRouteLeave((_to, _from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的内容，确定离开吗？',
      onOk: () => { next() },
      onCancel: () => { next(false) },
    })
  } else {
    next()
  }
})

function resetFormState(patch?: Partial<typeof formState>) {
  Object.assign(formState, {
    id: 0,
    customerName: '',
    customerCode: '',
    businessContact: '',
    phone: '',
    email: '',
    address: '',
    customerLevel: 3,
    industryType: undefined,
    status: 1,
    remark: '',
    ...(patch || {})
  })
}

function handleAdd() {
  isEdit.value = false
  resetFormState()
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleAddToLevel(level: number) {
  isEdit.value = false
  resetFormState({ customerLevel: level })
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleEdit(record: any) {
  isEdit.value = true
  // 实体字段回填（此前用 name/code/level 等旧字段名，编辑时表单显示为空）
  resetFormState({
    id: record.id,
    customerName: record.customerName || '',
    customerCode: record.customerCode || '',
    businessContact: record.businessContact || '',
    phone: record.phone || '',
    email: record.email || '',
    address: record.address || '',
    customerLevel: record.customerLevel ?? 3,
    industryType: record.industryType ?? undefined,
    status: record.status ?? 1,
    remark: record.remark || ''
  })
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleFormClose() {
  if (formRef.value && formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的内容，确定关闭吗？',
      onOk: () => { modalVisible.value = false }
    })
  } else {
    modalVisible.value = false
  }
}

async function handleFormSaveAndNew() {
  await handleModalOk()
  if (!modalLoading.value) {
    isEdit.value = false
    resetFormState()
    modalVisible.value = true
    nextTick(() => saveFormSnapshot())
  }
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch (err) { console.warn('[CRM客户] 表单验证失败', err); return }
  modalLoading.value = true
  try {
    if (isEdit.value) {
      await customerApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await customerApi.create(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    kanbanData.value = []
    fetchData()
  } catch (err) {
    console.warn('[CRM客户] 保存客户失败', err)
    message.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    modalLoading.value = false
  }
}

// ═══ 添加跟进记录（真实接口） ═══
const followModalVisible = ref(false)
const followModalLoading = ref(false)
const currentCustomerId = ref<any>(0)
const followForm = reactive({ followType: 1, content: '', result: 3, nextFollowTime: null as any })

function handleFollow(record: any) {
  currentCustomerId.value = record.id
  Object.assign(followForm, { followType: 1, content: '', result: 3, nextFollowTime: null })
  followModalVisible.value = true
}

async function handleFollowModalOk() {
  if (!followForm.content) { message.error('请输入跟进内容'); return }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(currentCustomerId.value, {
      customerId: currentCustomerId.value,
      followType: followForm.followType,
      content: followForm.content,
      result: followForm.result,
      nextFollowTime: followForm.nextFollowTime
    })
    message.success('跟进记录添加成功')
    followModalVisible.value = false
  } catch (err) {
    console.warn('[CRM客户] 添加跟进记录失败', err)
    message.error('添加失败')
  } finally {
    followModalLoading.value = false
  }
}

// ═══ CSV 导入（真实落库） ═══
const importVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importMappingColumns: DetailColumnConfig[] = [
  { key: 'label', title: '系统字段', type: 'input', width: 120 },
  { key: 'csvField', title: 'CSV列名', type: 'slot', slotName: 'csvFieldCell', width: 160 },
  { key: 'required', title: '必填', type: 'slot', slotName: 'requiredCell', width: 60, align: 'center' }
]
const importFieldMapping = reactive([
  { csvField: '', systemField: 'customerName', required: true, label: '客户名称' },
  { csvField: '', systemField: 'customerCode', required: true, label: '客户编码' },
  { csvField: '', systemField: 'businessContact', required: false, label: '联系人' },
  { csvField: '', systemField: 'phone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'email', required: false, label: '邮箱' },
  { csvField: '', systemField: 'industryType', required: false, label: '行业' },
  { csvField: '', systemField: 'address', required: false, label: '地址' }
])

function handleImport() {
  importFileList.value = []
  importFieldMapping.forEach(m => { m.csvField = '' })
  importVisible.value = true
}
function handleImportFileChange(info: any) { importFileList.value = info.fileList.slice(-1) }

async function handleImportConfirm() {
  if (importFileList.value.length === 0) { message.warning('请先上传CSV文件'); return }
  const unmappedRequired = importFieldMapping.filter(f => f.required && !f.csvField)
  if (unmappedRequired.length > 0) {
    message.warning(`请为必填字段配置CSV映射：${unmappedRequired.map(f => f.label).join('、')}`)
    return
  }
  importLoading.value = true
  try {
    await customerApi.importCustomers({
      file: importFileList.value[0],
      mapping: importFieldMapping.reduce((acc, m) => {
        if (m.csvField) acc[m.systemField] = m.csvField
        return acc
      }, {} as Record<string, string>)
    })
    message.success('导入成功')
    importVisible.value = false
    kanbanData.value = []
    fetchData()
  } catch (err) {
    console.warn('[CRM客户] 导入客户失败', err)
    message.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

// ═══ 导出（带中文映射） ═══
function handleExport() {
  const rows = (dataSource.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['客户名称', '客户编码', '联系人', '联系电话', '邮箱', '行业', '客户等级', '状态', '创建时间']
  const lines = rows.map((r: any) => [
    r.customerName, r.customerCode, r.businessContact, r.phone, r.email,
    getIndustryName(r.industryType),
    r.customerLevel ? getLevelName(r.customerLevel) : '未分级',
    r.status === 1 ? '正常' : '停用',
    r.createdAt
  ].map(v => String(v ?? '')))
  exportCsv(headers, lines, '客户数据')
}

// ═══ 视图切换 ═══
watch(currentView, (val) => {
  if (val === 'kanban') {
    fetchKanbanData()
  }
})

function handleParentCreate() { handleAdd() }

// ── 快捷键 ──────────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
    return
  }
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    debounceClick('add', handleAdd)
    return
  }
}

onMounted(() => {
  loadUserOptions()
  fetchData()
  startAutoRefresh()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, fetchData as any)
})

onUnmounted(() => {
  stopAutoRefresh()
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, fetchData as any)
})
defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

/* ── 查询区（插槽内容样式必须自备，scoped 不作用到布局组件内部） ── */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 180px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillTableList / BillDetailTable 根元素为 flex:1，
   父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

/* 统计卡与表格同列时，表格按「剩余空间」分配高度（BillTableList 根元素默认 height:100%，
   不参与 flex 分配会与统计卡抢高度并被 overflow 裁掉底部） */
.table-area :deep(.bill-table-list-container) { flex: 1; min-height: 0; }

.stats-cards {
  flex-shrink: 0;
  margin: 12px 16px 8px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card.stat-card-blue {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border: 1px solid #91d5ff;
}

.stat-card.stat-card-green {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border: 1px solid #b7eb8f;
}

.stat-card.stat-card-orange {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.stat-card.stat-card-purple {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.customer-name {
  font-weight: 500;
  line-height: 1.2;
}

.cell-link {
  color: #1890ff;
  cursor: pointer;
}

.cell-link:hover {
  text-decoration: underline;
}

.action-more-btn {
  padding: 0 4px;
}

/* ── 看板样式 ── */
.kanban-container {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 16px;
  overflow: auto;
  padding: 12px 16px;
}

.kanban-column {
  flex: 1;
  min-width: 280px;
  background-color: #f5f5f5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.kanban-column-header {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  border-radius: 8px 8px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.kanban-column-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-column-count {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.kanban-column-body {
  flex: 1;
  padding: 8px;
  overflow: auto;
}

.kanban-empty {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  padding: 24px 0;
}

.kanban-card {
  background-color: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e8e8e8;
}

.kanban-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  border-color: #1890ff;
}

.kanban-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kanban-card-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.kanban-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.kanban-card-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.kanban-card-label {
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
}

.kanban-card-value {
  color: #606266;
}

.kanban-card-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

/* ── 橙色新增按钮（CRM 模块统一） ──────────── */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 快捷键提示 ──────────────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 自动刷新倒计时徽章 ──────────────────────── */
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* ── 骨架加载 ────────────────────────────────── */
.skeleton-loading {
  padding: 24px;
  margin: 12px 16px;
  background: #fff;
  border-radius: 8px;
}
</style>
