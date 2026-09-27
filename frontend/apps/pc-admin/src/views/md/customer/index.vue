<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="showCategoryPanel"
        :category-title="categoryTitle"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentPath"
        :tabs="tabs"
        :active-tab="activeTab"
        :show-table-footer="true"
        @tab-change="handleTabChange"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeys = keys)"
        @category-add="openCategoryModal(null)"
        @category-retry="loadCategoryTree"
      >
        <!-- 分类树标题栏操作（对标实测：标题右侧为「修改 / 删除 / 新增」，作用于当前选中节点） -->
        <template #category-header-actions>
          <a-button
            type="link"
            size="small"
            title="修改"
            @click="handleCategoryHeaderEdit"
          >
            <EditOutlined />
          </a-button>
          <a-button
            type="link"
            size="small"
            title="删除"
            @click="handleCategoryHeaderDelete"
          >
            <DeleteOutlined />
          </a-button>
        </template>

        <!-- 分类节点：纯文本（编辑/删除走标题栏按钮，与对标一致） -->
        <template #tree-title="node">
          <span class="tree-node-name">{{ node.categoryName }}</span>
        </template>

        <!-- ═══ 工具栏 ═══ -->
        <template #toolbar-left>
          <!-- 会员管理：对标工具栏最左侧为「查询方案」下拉 + 另存为（全部客户无此项） -->
          <div
            v-if="activeTab === 'member'"
            class="query-scheme-wrap"
          >
            <a-select
              v-model:value="memberQueryScheme"
              size="small"
              style="width: 140px"
              placeholder="--查询方案--"
              @change="handleSchemeChange"
            >
              <a-select-option value="">
                --查询方案--
              </a-select-option>
              <a-select-option
                v-for="s in memberSchemeList"
                :key="s.name"
                :value="s.name"
              >
                {{ s.name }}
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
              title="另存为查询方案"
              @click="handleSaveQueryScheme"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增
          </a-button>
          <template v-if="activeTab === 'all'">
            <a-button
              size="small"
              :loading="importLoading"
              @click="importModalVisible = true"
            >
              <UploadOutlined /> 导入
            </a-button>
          </template>
          <template v-if="activeTab === 'all' || activeTab === 'member'">
            <a-button
              size="small"
              @click="handleSendSms"
            >
              <MessageOutlined /> 发短信
            </a-button>
            <a-button
              size="small"
              @click="handleSendCoupon"
            >
              <GiftOutlined /> 发优惠券
            </a-button>
          </template>
          <a-button
            v-if="activeTab === 'contact'"
            size="small"
            @click="handleImportRoutes"
          >
            导入默认线路/物流
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <!-- 页面配置（仅 全部客户 / 会员管理 两个子标签提供，对标实测） -->
            <a-button
              v-if="hasPageConfig"
              size="small"
              title="配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('memberSetting')"
              size="small"
              @click="handleMemberSetting"
            >
              会员设置
            </a-button>
            <a-button
              v-if="activeTab === 'contact'"
              size="small"
              @click="handleSetDeliveryMethod"
            >
              设置配送方式
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
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export') && activeTab === 'all'"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchModify')"
              size="small"
              :disabled="selectedIds.length === 0"
              @click="handleBatchModify"
            >
              批量修改
            </a-button>
            <a-dropdown v-if="hasMoreActions">
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    v-if="isButtonEnabled('batchMove')"
                    :disabled="selectedIds.length === 0"
                    @click="openMoveModal"
                  >
                    <DragOutlined /> 批量搬移
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('batchDelete')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchDelete"
                  >
                    <DeleteOutlined /> 批量删除
                  </a-menu-item>
                  <a-menu-divider v-if="activeTab === 'all'" />
                  <a-menu-item
                    v-if="activeTab === 'all' && isButtonEnabled('disable')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('DISABLED')"
                  >
                    停用
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'all' && isButtonEnabled('enable')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('ENABLED')"
                  >
                    启用
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'all' && isButtonEnabled('priceTrackOff')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchPriceTrack(false)"
                  >
                    取消价格跟踪
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'all' && isButtonEnabled('priceTrackOn')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchPriceTrack(true)"
                  >
                    启用价格跟踪
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'all' && isButtonEnabled('mallAccount')"
                    :disabled="selectedIds.length === 0"
                    @click="handleOpenMallAccount"
                  >
                    开通商城账号
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'member' && isButtonEnabled('export')"
                    :disabled="exporting"
                    @click="handleExport"
                  >
                    <DownloadOutlined /> 导出
                  </a-menu-item>
                  <a-menu-item
                    v-if="activeTab === 'member' && isButtonEnabled('importPoints')"
                    @click="handleImportPoints"
                  >
                    批量导入积分
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区（按子标签切换，字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <!-- 全部客户 / 会员管理：对齐对标「每行 6 列」的查询区栅格，避免折成 3 行 -->
          <div :class="['search-row', { 'search-grid': activeTab === 'all' || activeTab === 'member' }]">
            <!-- 全部客户 -->
            <template v-if="activeTab === 'all'">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="allQuery.keyword"
                  placeholder="请输入客户编号/名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('address')"
                class="search-item"
              >
                <span class="search-label">联系地址</span>
                <a-input
                  v-model:value="allQuery.address"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('createTimeStart')"
                class="search-item"
              >
                <span class="search-label">新增日期（起）</span>
                <a-date-picker
                  v-model:value="allQuery.createTimeStart"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('createTimeEnd')"
                class="search-item"
              >
                <span class="search-label">新增日期（止）</span>
                <a-date-picker
                  v-model:value="allQuery.createTimeEnd"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('lastTradeStart')"
                class="search-item"
              >
                <span class="search-label">最近交易（起）</span>
                <a-date-picker
                  v-model:value="allQuery.lastTradeStart"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('lastTradeEnd')"
                class="search-item"
              >
                <span class="search-label">最近交易（止）</span>
                <a-date-picker
                  v-model:value="allQuery.lastTradeEnd"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('settleType')"
                class="search-item"
              >
                <span class="search-label">结款方式</span>
                <a-select
                  v-model:value="allQuery.settleType"
                  size="small"
                  style="width: 110px"
                  :options="settleTypeQueryOptions"
                />
              </div>
              <div
                v-if="isQueryVisible('gradeName')"
                class="search-item"
              >
                <span class="search-label">客户级别</span>
                <a-select
                  v-model:value="allQuery.gradeName"
                  size="small"
                  style="width: 140px"
                  :options="gradeOptions"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                />
              </div>
              <div
                v-if="isQueryVisible('handler')"
                class="search-item"
              >
                <span class="search-label">默认经手人</span>
                <a-input
                  v-model:value="allQuery.handler"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                >
                  <template #suffix>
                    <SearchOutlined class="search-suffix-icon" />
                  </template>
                </a-input>
              </div>
              <div
                v-if="isQueryVisible('promoter')"
                class="search-item"
              >
                <span class="search-label">推广人</span>
                <a-input
                  v-model:value="allQuery.promoter"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('warehouse')"
                class="search-item"
              >
                <span class="search-label">所属仓库</span>
                <a-input
                  v-model:value="allQuery.warehouse"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('region')"
                class="search-item"
              >
                <span class="search-label">所属区域</span>
                <a-input
                  v-model:value="allQuery.region"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                >
                  <template #suffix>
                    <SearchOutlined class="search-suffix-icon" />
                  </template>
                </a-input>
              </div>
              <div
                v-if="isQueryVisible('customerSource')"
                class="search-item"
              >
                <span class="search-label">客户来源</span>
                <a-select
                  v-model:value="allQuery.customerSource"
                  size="small"
                  style="width: 120px"
                  :options="customerSourceOptions"
                  allow-clear
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">显示状态</span>
                <a-select
                  v-model:value="allQuery.status"
                  size="small"
                  style="width: 110px"
                  :options="statusOptions"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-checkbox
                v-if="isQueryVisible('showHierarchy')"
                v-model:checked="allQuery.showHierarchy"
                @change="handleSearch"
              >
                显示层次结构
              </a-checkbox>
              <!-- 全部客户专属勾选项（对标：与「查询」按钮同排，位于其右侧） -->
              <span class="search-checkbox-group">
                <a-checkbox
                  v-if="isQueryVisible('onlyMallAccount')"
                  v-model:checked="allQuery.onlyMallAccount"
                  @change="handleSearch"
                >
                  只显示开通商城账号
                </a-checkbox>
                <a-checkbox
                  v-if="isQueryVisible('onlyNoTrade')"
                  v-model:checked="allQuery.onlyNoTrade"
                  @change="handleSearch"
                >
                  只显示无销售记录客户
                </a-checkbox>
                <a-checkbox
                  v-if="isQueryVisible('showAsCustomer')"
                  v-model:checked="allQuery.showAsCustomer"
                  @change="handleSearch"
                >
                  显示供应商中的客户
                </a-checkbox>
              </span>
            </template>

            <!-- 会员管理 -->
            <template v-else-if="activeTab === 'member'">
              <div
                v-if="isQueryVisible('keyword')"
                class="search-item"
              >
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="memberQuery.keyword"
                  placeholder="请输入会员名称/卡号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('memberKeyword')"
                class="search-item"
              >
                <span class="search-label">会员名称</span>
                <a-input
                  v-model:value="memberQuery.memberKeyword"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('phone')"
                class="search-item"
              >
                <span class="search-label">联系电话</span>
                <a-input
                  v-model:value="memberQuery.phone"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('customerId')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="memberQuery.customerKeyword"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleMemberCustomerSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('handler')"
                class="search-item"
              >
                <span class="search-label">客户经手人</span>
                <a-input
                  v-model:value="memberQuery.handler"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('lastTradeStart')"
                class="search-item"
              >
                <span class="search-label">最近交易（起）</span>
                <a-date-picker
                  v-model:value="memberQuery.lastTradeStart"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('lastTradeEnd')"
                class="search-item"
              >
                <span class="search-label">最近交易（止）</span>
                <a-date-picker
                  v-model:value="memberQuery.lastTradeEnd"
                  size="small"
                  style="width: 150px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('memberLevel')"
                class="search-item"
              >
                <span class="search-label">会员级别</span>
                <a-select
                  v-model:value="memberQuery.memberLevel"
                  size="small"
                  style="width: 130px"
                  :options="memberLevelOptions"
                  allow-clear
                />
              </div>
              <div
                v-if="isQueryVisible('memberCardStatus')"
                class="search-item"
              >
                <span class="search-label">会员卡状态</span>
                <a-select
                  v-model:value="memberQuery.memberCardStatus"
                  size="small"
                  style="width: 120px"
                  :options="memberCardStatusOptions"
                  allow-clear
                />
              </div>
              <div
                v-if="isQueryVisible('birthday')"
                class="search-item"
              >
                <span class="search-label">生日</span>
                <a-date-picker
                  v-model:value="memberQuery.birthday"
                  size="small"
                  style="width: 140px"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </div>
              <div
                v-if="isQueryVisible('age')"
                class="search-item"
              >
                <span class="search-label">年龄</span>
                <a-input-number
                  v-model:value="memberQuery.ageMin"
                  size="small"
                  :min="0"
                  style="width: 90px"
                  placeholder="起"
                />
                <span class="range-sep">~</span>
                <a-input-number
                  v-model:value="memberQuery.ageMax"
                  size="small"
                  :min="0"
                  style="width: 90px"
                  placeholder="止"
                />
              </div>
              <div
                v-if="isQueryVisible('points')"
                class="search-item"
              >
                <span class="search-label">当前积分</span>
                <a-input-number
                  v-model:value="memberQuery.pointsMin"
                  size="small"
                  style="width: 100px"
                  placeholder="起"
                />
                <span class="range-sep">~</span>
                <a-input-number
                  v-model:value="memberQuery.pointsMax"
                  size="small"
                  style="width: 100px"
                  placeholder="止"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                查询
              </a-button>
            </template>

            <!-- 全部联系人 -->
            <template v-else-if="activeTab === 'contact'">
              <div class="search-item">
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="contactQuery.customerKeyword"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleContactCustomerSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">关键字</span>
                <a-input
                  v-model:value="contactQuery.keyword"
                  placeholder="姓名/手机"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">客户经手人</span>
                <a-input
                  v-model:value="contactQuery.handler"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">联系地址</span>
                <a-input
                  v-model:value="contactQuery.address"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">配送方式</span>
                <a-select
                  v-model:value="contactQuery.deliveryMethod"
                  size="small"
                  style="width: 120px"
                  :options="deliveryMethodOptions"
                  allow-clear
                />
              </div>
              <div class="search-item">
                <span class="search-label">所属区域</span>
                <a-input
                  v-model:value="contactQuery.region"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                查询
              </a-button>
            </template>

            <!-- 区域管理 -->
            <template v-else-if="activeTab === 'region'">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="regionQuery.keyword"
                  placeholder="请输入区域编号/名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                class="btn-search"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-checkbox
                v-model:checked="regionQuery.showHierarchy"
                @change="handleSearch"
              >
                显示层次结构
              </a-checkbox>
            </template>
          </div>

        </template>

        <!-- ═══ 数据表格（列配置走表头齿轮：个人 / 全局） ═══ -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="activeColumns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :show-pagination="false"
            :storage-key="activeStorageKey"
            :global-config-key="`${activeStorageKey}-global`"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
          >
            <template #actionCell="{ record }">
              <!-- 占位空行（__ghost）不渲染行操作，保持纯空白 -->
              <a-space
                v-if="!record.__ghost"
                :size="0"
              >
                <template v-if="activeTab === 'all'">
                  <a-button
                    type="link"
                    size="small"
                    @click="handlePlaceOrder(record)"
                  >
                    代客下单
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
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
                        <a-menu-item @click="handleView(record)">
                          查看详情
                        </a-menu-item>
                        <a-menu-item @click="handleToggleStatus(record)">
                          {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                        </a-menu-item>
                        <a-menu-item
                          danger
                          @click="handleDelete(record)"
                        >
                          删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </template>
                <template v-else-if="activeTab === 'member'">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEditMember(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="handlePointsDetail(record)"
                  >
                    积分明细
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
                        <a-menu-item @click="handleView(record)">
                          查看客户档案
                        </a-menu-item>
                        <a-menu-item
                          danger
                          @click="handleDelete(record)"
                        >
                          删除会员卡
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </template>
                <template v-else>
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEditRow(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDeleteRow(record)"
                  >
                    删除
                  </a-button>
                </template>
              </a-space>
            </template>

            <template #nameCell="{ record }">
              <a
                class="cell-link"
                @click="handleView(record)"
              >{{ activeTab === 'member' ? record.memberName : record.partnerName }}</a>
            </template>

            <template #memberValidCell="{ record }">
              {{ formatDate(record.memberValidStart) }} ~ {{ formatDate(record.memberValidEnd) }}
            </template>

            <template #attachmentCell="{ record }">
              <template v-if="!record.__ghost">
                <a
                  v-if="record.attachmentCount"
                  class="cell-link"
                  @click="showAttachments(record)"
                >
                  <PaperClipOutlined /> {{ record.attachmentCount }}
                </a>
                <span v-else>-</span>
              </template>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 底部：分页 ═══ -->
        <template #table-footer>
          <StandardPagination
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="activeFunctionButtons"
      :storage-key="activePageConfigStorageKey"
      hide-print-config
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 联系人弹窗（独立联系人实体，对标「全部联系人 → 新增」） ═══ -->
    <ContactFormModal
      v-model:open="contactModalOpen"
      :record="contactModalRecord"
      @saved="onModalSaved"
    />

    <!-- ═══ 会员卡弹窗（对标「会员管理 → 新增」） ═══ -->
    <MemberCardModal
      v-model:open="memberModalOpen"
      :record="memberModalRecord"
      @saved="onModalSaved"
    />

    <!-- ═══ 分类新增/编辑 ═══ -->
    <a-modal
      v-model:open="categoryModalVisible"
      :title="editingCategory ? '编辑分类' : '新增分类'"
      :confirm-loading="categorySaving"
      width="420px"
      @ok="handleCategorySave"
    >
      <a-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="分类名称"
          name="categoryName"
        >
          <a-input
            v-model:value="categoryForm.categoryName"
            placeholder="请输入分类名称"
            size="small"
          />
        </a-form-item>
        <a-form-item label="分类编码">
          <a-input
            v-model:value="categoryForm.categoryCode"
            placeholder="留空则系统自动生成"
            size="small"
          />
        </a-form-item>
        <a-form-item label="上级分类">
          <a-tree-select
            v-model:value="categoryForm.parentId"
            :tree-data="categoryTreeData"
            :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="无（根节点）"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number
            v-model:value="categoryForm.sortOrder"
            :min="0"
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 批量搬移 ═══ -->
    <a-modal
      v-model:open="moveModalVisible"
      title="批量搬移"
      :confirm-loading="moveSaving"
      width="420px"
      @ok="handleBatchMove"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="目标分类">
          <a-tree-select
            v-model:value="moveCategoryId"
            :tree-data="categoryTreeData"
            :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="请选择目标分类"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-alert
        :message="`将把选中的 ${selectedIds.length} 个客户搬移到目标分类`"
        type="info"
        show-icon
      />
    </a-modal>

    <!-- ═══ 批量修改（客户级别 / 结款方式 / 默认经手人） ═══ -->
    <a-modal
      v-model:open="batchModifyVisible"
      title="批量修改"
      :confirm-loading="batchSaving"
      width="460px"
      @ok="handleBatchModifySave"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="客户级别">
          <a-select
            v-model:value="batchForm.gradeName"
            :options="gradeOptions"
            placeholder="不修改"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="结款方式">
          <a-select
            v-model:value="batchForm.settleType"
            :options="settleTypeOptions"
            placeholder="不修改"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="所属区域">
          <a-input
            v-model:value="batchForm.region"
            placeholder="不修改"
            size="small"
          />
        </a-form-item>
        <a-form-item label="客户来源">
          <a-select
            v-model:value="batchForm.customerSource"
            :options="customerSourceOptions"
            placeholder="不修改"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-alert
        :message="`将批量修改选中的 ${selectedIds.length} 个客户，未填写的项保持原值`"
        type="info"
        show-icon
      />
    </a-modal>

    <!-- ═══ 导入 ═══ -->
    <a-modal
      v-model:open="importModalVisible"
      title="数据导入"
      :confirm-loading="importLoading"
      @ok="handleImportUpload"
    >
      <a-alert
        message="请上传 Excel 文件（.xlsx/.xls），系统将自动解析并导入客户。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-upload-dragger
        v-model:file-list="importFileList"
        :before-upload="() => false"
        accept=".xlsx,.xls"
        :max-count="1"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">
          点击或拖拽文件到此区域上传
        </p>
        <p class="ant-upload-hint">
          仅支持 .xlsx / .xls 格式的 Excel 文件
        </p>
      </a-upload-dragger>
    </a-modal>

    <!-- ═══ 客户级别新增/编辑 ═══ -->
    <a-modal
      v-model:open="gradeModalVisible"
      :title="gradeForm.id ? '编辑客户级别' : '新增客户级别'"
      :confirm-loading="gradeSaving"
      width="440px"
      @ok="handleGradeSave"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="客户级别">
          <a-input
            v-model:value="gradeForm.gradeName"
            size="small"
          />
        </a-form-item>
        <a-form-item label="级别默认价">
          <a-input-number
            v-model:value="gradeForm.discountRate"
            :min="0"
            :max="100"
            :precision="2"
            addon-after="%"
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 区域新增/编辑 ═══ -->
    <a-modal
      v-model:open="regionModalVisible"
      :title="regionForm.id ? '编辑区域' : '新增区域'"
      :confirm-loading="regionSaving"
      width="440px"
      @ok="handleRegionSave"
    >
      <a-form
        ref="regionFormRef"
        :model="regionForm"
        :rules="regionRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="区域名称"
          name="regionName"
        >
          <a-input
            v-model:value="regionForm.regionName"
            placeholder="请输入区域名称"
            size="small"
          />
        </a-form-item>
        <a-form-item label="区域编号">
          <a-input
            v-model:value="regionForm.regionCode"
            placeholder="留空则系统自动生成"
            size="small"
          />
        </a-form-item>
        <a-form-item label="上级区域">
          <a-select
            v-model:value="regionForm.parentId"
            :options="regionParentOptions"
            placeholder="无（根节点）"
            allow-clear
            size="small"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-input
            v-model:value="regionForm.remark"
            size="small"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 附件查看 ═══ -->
    <a-modal
      v-model:open="attachModalVisible"
      :title="`附件 - ${attachPartnerName}`"
      width="560px"
      :footer="null"
    >
      <a-spin :spinning="attachLoading">
        <a-empty
          v-if="attachList.length === 0"
          description="暂无附件"
        />
        <div
          v-for="f in attachList"
          :key="f.id"
          class="attach-row"
        >
          <FileOutlined />
          <a
            :href="f.fileUrl"
            target="_blank"
            class="attach-name"
          >{{ f.fileName }}</a>
          <span class="attach-size">{{ formatSize(f.fileSize) }}</span>
        </div>
      </a-spin>
    </a-modal>

    <!-- ═══ 积分明细 ═══ -->
    <a-modal
      v-model:open="pointsModalVisible"
      :title="`积分明细 - ${pointsMemberName}`"
      width="520px"
      :footer="null"
    >
      <a-alert
        :message="`当前积分：${pointsCurrent}　累计消费额：${formatMoney(pointsConsume)}`"
        type="info"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-table
        :columns="pointsColumns"
        :data-source="pointsRows"
        :pagination="false"
        size="small"
        row-key="id"
      />
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownOutlined,
  DownloadOutlined,
  DragOutlined,
  EditOutlined,
  FileOutlined,
  GiftOutlined,
  InboxOutlined,
  MessageOutlined,
  PaperClipOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import ContactFormModal from './components/ContactFormModal.vue'
import MemberCardModal from './components/MemberCardModal.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import {
  customerRegionApi,
  partnerApi,
  partnerAttachmentApi,
  partnerCategoryApi,
  partnerGradeApi,
  type PartnerAttachment,
  type PartnerCategory,
} from '@/api/erp/partner'
import request from '@/utils/request'

defineOptions({ name: 'MdCustomer' })

const router = useRouter()
const handleError = (e: any) => console.warn('[客户] ErrorBoundary:', e)

/** 虚拟根节点：全部客户 */
const ROOT_CATEGORY_ID = '0'

// ═══ 子标签（对标实测 5 个：全部客户 / 会员管理 / 全部联系人 / 客户级别 / 区域管理） ═══
const tabs = [
  { key: 'all', label: '全部客户' },
  { key: 'member', label: '会员管理' },
  { key: 'contact', label: '全部联系人' },
  { key: 'grade', label: '客户级别' },
  { key: 'region', label: '区域管理' },
]
const activeTab = ref('all')

/** 分类树：全部客户 / 会员管理 / 全部联系人 显示「客户分类」；区域管理显示「地区分类」（对标实测均有左树，客户级别为全宽视图） */
const showCategoryPanel = computed(() => ['all', 'member', 'contact', 'region'].includes(activeTab.value))

/** 分类面板标题：区域管理为「地区分类」，其余为「客户分类」（对标实测） */
const categoryTitle = computed(() => (activeTab.value === 'region' ? '地区分类' : '客户分类'))

/** 页面配置弹窗：仅 全部客户 / 会员管理 提供（对标实测） */
const hasPageConfig = computed(() => ['all', 'member'].includes(activeTab.value))

// ═══ 下拉选项 ═══
const statusOptions = [
  { label: '全部', value: '' },
  { label: '已启用', value: 'ENABLED' },
  { label: '已停用', value: 'DISABLED' },
]
const settleTypeOptions = [
  { label: '现结', value: '现结' },
  { label: '挂账', value: '挂账' },
]
/** 查询区结款方式：对标默认显示「全部」（空值 = 不过滤） */
const settleTypeQueryOptions = [
  { label: '全部', value: '' },
  { label: '现结', value: '现结' },
  { label: '挂账', value: '挂账' },
]
const customerSourceOptions = [
  { label: '门店拜访', value: '门店拜访' },
  { label: '电话营销', value: '电话营销' },
  { label: '老客户介绍', value: '老客户介绍' },
  { label: '线上推广', value: '线上推广' },
  { label: '其他', value: '其他' },
]
const memberCardStatusOptions = [
  { label: '正常', value: 'NORMAL' },
  { label: '停用', value: 'STOPPED' },
  { label: '已过期', value: 'EXPIRED' },
]
const deliveryMethodOptions = [
  { label: '自配', value: '自配' },
  { label: '物流', value: '物流' },
  { label: '自提', value: '自提' },
]
const memberLevelOptions = ref<Array<{ label: string; value: string }>>([])
const gradeOptions = ref<Array<{ label: string; value: string }>>([])

// ═══ 查询条件（按子标签拆分） ═══
const allQuery = reactive({
  keyword: '',
  address: '',
  createTimeStart: undefined as any,
  createTimeEnd: undefined as any,
  lastTradeStart: undefined as any,
  lastTradeEnd: undefined as any,
  settleType: '' as string | undefined,
  gradeName: undefined as string | undefined,
  handler: '',
  promoter: '',
  warehouse: '',
  region: '',
  customerSource: undefined as string | undefined,
  status: 'ENABLED' as string,
  showHierarchy: false,
  onlyMallAccount: false,
  onlyNoTrade: false,
  showAsCustomer: false,
})
/** 会员管理「查询方案」（对标工具栏最左侧）：查询条件另存为本地方案，下拉可套用 */
const memberQueryScheme = ref('')
const memberSchemeList = ref<Array<{ name: string; query: Record<string, any> }>>([])
const MEMBER_SCHEME_STORAGE = 'md-customer-member-schemes'

function loadMemberSchemes() {
  try {
    const raw = localStorage.getItem(MEMBER_SCHEME_STORAGE)
    memberSchemeList.value = raw ? JSON.parse(raw) : []
  } catch {
    memberSchemeList.value = []
  }
}

function handleSaveQueryScheme() {
  const name = `方案${memberSchemeList.value.length + 1}`
  const query = JSON.parse(JSON.stringify(memberQuery))
  const idx = memberSchemeList.value.findIndex(s => s.name === name)
  if (idx >= 0) {
    memberSchemeList.value[idx] = { name, query }
  } else {
    memberSchemeList.value = [...memberSchemeList.value, { name, query }]
  }
  try {
    localStorage.setItem(MEMBER_SCHEME_STORAGE, JSON.stringify(memberSchemeList.value))
  } catch {
    // 忽略本地存储不可用
  }
  memberQueryScheme.value = name
  message.success(`已另存为「${name}」`)
}

function handleSchemeChange(name: string) {
  if (!name) return
  const hit = memberSchemeList.value.find(s => s.name === name)
  if (!hit) return
  Object.assign(memberQuery, JSON.parse(JSON.stringify(hit.query)))
  handleSearch()
}

const memberQuery = reactive({
  keyword: '',
  memberKeyword: '',
  phone: '',
  customerId: undefined as any,
  customerKeyword: '',
  handler: '',
  lastTradeStart: undefined as any,
  lastTradeEnd: undefined as any,
  memberLevel: undefined as string | undefined,
  memberCardStatus: undefined as string | undefined,
  birthday: undefined as any,
  ageMin: undefined as any,
  ageMax: undefined as any,
  pointsMin: undefined as any,
  pointsMax: undefined as any,
})
const contactQuery = reactive({
  keyword: '',
  customerId: undefined as any,
  customerKeyword: '',
  handler: '',
  address: '',
  deliveryMethod: undefined as string | undefined,
  region: '',
})
const regionQuery = reactive({
  keyword: '',
  showHierarchy: false,
})
const gradeQuery = reactive({
  keyword: '',
})

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const PAGE_CONFIG_PREFIX = 'md-customer-page-config'

/** 全部客户查询条件（对标 18 项，含隐藏项） */
const DEFAULT_ALL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'address', label: '联系地址', visible: true },
  { key: 'createTimeStart', label: '新增日期（起）', visible: true },
  { key: 'createTimeEnd', label: '新增日期（止）', visible: true },
  { key: 'lastTradeStart', label: '最近交易（起）', visible: true },
  { key: 'lastTradeEnd', label: '最近交易（止）', visible: true },
  { key: 'settleType', label: '结款方式', visible: true },
  { key: 'gradeName', label: '客户级别', visible: false },
  { key: 'handler', label: '默认经手人', visible: true },
  { key: 'promoter', label: '推广人', visible: false },
  { key: 'warehouse', label: '所属仓库', visible: false },
  { key: 'region', label: '所属区域', visible: true },
  { key: 'customerSource', label: '客户来源', visible: false },
  { key: 'status', label: '显示状态', visible: true },
  { key: 'showHierarchy', label: '显示层次结构', visible: false },
  { key: 'onlyMallAccount', label: '只显示开通商城账号', visible: true },
  { key: 'onlyNoTrade', label: '只显示无销售记录客户', visible: true },
  { key: 'showAsCustomer', label: '显示供应商中的客户', visible: true },
]

/** 全部客户功能按钮（对标 11 项） */
const DEFAULT_ALL_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'batchModify', label: '批量修改', enabled: true },
  { key: 'batchDelete', label: '批量删除', enabled: true },
  { key: 'batchMove', label: '批量搬移', enabled: true },
  { key: 'disable', label: '停用', enabled: true },
  { key: 'enable', label: '启用', enabled: true },
  { key: 'priceTrackOff', label: '取消价格跟踪', enabled: true },
  { key: 'priceTrackOn', label: '启用价格跟踪', enabled: true },
  { key: 'mallAccount', label: '开通商城账号', enabled: true },
]

/** 会员管理查询条件（对标 12 项） */
const DEFAULT_MEMBER_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'memberKeyword', label: '会员名称', visible: true },
  { key: 'phone', label: '联系电话', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'handler', label: '客户经手人', visible: true },
  { key: 'lastTradeStart', label: '最近交易（起）', visible: true },
  { key: 'lastTradeEnd', label: '最近交易（止）', visible: true },
  { key: 'memberLevel', label: '会员级别', visible: false },
  { key: 'memberCardStatus', label: '会员卡状态', visible: false },
  { key: 'birthday', label: '生日', visible: false },
  { key: 'age', label: '年龄', visible: false },
  { key: 'points', label: '当前积分', visible: false },
]

/** 会员管理功能按钮（对标 5 项） */
const DEFAULT_MEMBER_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'memberSetting', label: '会员设置', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'importPoints', label: '批量导入积分', enabled: true },
]

const allQueryFields = ref<QueryFieldSetting[]>(DEFAULT_ALL_QUERY_FIELDS.map(f => ({ ...f })))
const allFunctionButtons = ref<FunctionButtonSetting[]>(DEFAULT_ALL_FUNCTION_BUTTONS.map(f => ({ ...f })))
const memberQueryFields = ref<QueryFieldSetting[]>(DEFAULT_MEMBER_QUERY_FIELDS.map(f => ({ ...f })))
const memberFunctionButtons = ref<FunctionButtonSetting[]>(DEFAULT_MEMBER_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)
// 联系人 / 会员卡弹窗（对标：这两个子标签的「新增」各自打开独立弹窗，而不是新增客户）
const contactModalOpen = ref(false)
const contactModalRecord = ref<any>(null)
const memberModalOpen = ref(false)
const memberModalRecord = ref<any>(null)
function onModalSaved() { loadData() }

const activeQueryFields = computed(() =>
  activeTab.value === 'member' ? memberQueryFields.value : allQueryFields.value,
)
const activeFunctionButtons = computed(() =>
  activeTab.value === 'member' ? memberFunctionButtons.value : allFunctionButtons.value,
)
const activePageConfigStorageKey = computed(() => `${PAGE_CONFIG_PREFIX}-${activeTab.value}`)

function isQueryVisible(key: string): boolean {
  const fields = activeQueryFields.value
  const hit = fields.find(f => f.key === key)
  return hit ? hit.visible : false
}

/** 固定功能按钮（无页面配置弹窗的子标签，对标实测：仅 刷新/打印/导出） */
const FIXED_TAB_BUTTONS: Record<string, string[]> = {
  contact: ['refresh', 'printF8', 'export'],
  grade: ['refresh', 'printF8', 'export'],
  region: ['refresh', 'printF8', 'export'],
}

function isButtonEnabled(key: string): boolean {
  if (activeTab.value !== 'all' && activeTab.value !== 'member') {
    return (FIXED_TAB_BUTTONS[activeTab.value] || []).includes(key)
  }
  const buttons = activeFunctionButtons.value
  const hit = buttons.find(b => b.key === key)
  return hit ? hit.enabled : false
}

const hasMoreActions = computed(() => {
  if (activeTab.value === 'all') {
    return ['batchMove', 'batchDelete', 'disable', 'enable', 'priceTrackOff', 'priceTrackOn', 'mallAccount']
      .some(k => isButtonEnabled(k))
  }
  if (activeTab.value === 'member') {
    // 对标会员管理：工具栏只到「打印(F8) + 更多」，导出/批量导入积分收在「更多」里
    return isButtonEnabled('importPoints') || isButtonEnabled('export')
  }
  return false
})

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(activePageConfigStorageKey.value)
    const isMember = activeTab.value === 'member'
    const defaults = isMember ? DEFAULT_MEMBER_QUERY_FIELDS : DEFAULT_ALL_QUERY_FIELDS
    const btnDefaults = isMember ? DEFAULT_MEMBER_FUNCTION_BUTTONS : DEFAULT_ALL_FUNCTION_BUTTONS
    if (!raw) {
      // 无本地配置时恢复默认（切换子标签时保证默认一致）
      if (isMember) {
        memberQueryFields.value = defaults.map(f => ({ ...f }))
        memberFunctionButtons.value = btnDefaults.map(f => ({ ...f }))
      } else {
        allQueryFields.value = defaults.map(f => ({ ...f }))
        allFunctionButtons.value = btnDefaults.map(f => ({ ...f }))
      }
      return
    }
    const parsed = JSON.parse(raw) as PageConfigData
    const merged = defaults.map(df => {
      const saved = parsed.queryFields?.find((f: QueryFieldSetting) => f.key === df.key)
      return saved ? { ...df, ...saved } : { ...df }
    })
    const mergedBtns = btnDefaults.map(bf => {
      const saved = parsed.functionButtons?.find((f: FunctionButtonSetting) => f.key === bf.key)
      return saved ? { ...bf, ...saved } : { ...bf }
    })
    if (isMember) {
      memberQueryFields.value = merged
      memberFunctionButtons.value = mergedBtns
    } else {
      allQueryFields.value = merged
      allFunctionButtons.value = mergedBtns
    }
  } catch {
    // ignore
  }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(activePageConfigStorageKey.value, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  loadPageConfig()
}

// ═══ 表格列（对标实测：列配置走数据表表头齿轮，默认显示列见各子标签） ═══
const ACTION_WIDTH: Record<string, number> = { all: 170, member: 165, contact: 110, grade: 110, region: 110 }

const ALL_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: ACTION_WIDTH.all, fixed: 'left' },
  { key: 'partnerCode', title: '客户编号', width: 135, sortable: true, ellipsis: true },
  { key: 'partnerName', title: '客户名称', type: 'slot', slotName: 'nameCell', width: 200, sortable: true, ellipsis: true },
  { key: 'settleType', title: '结款方式', width: 90 },
  { key: 'gradeName', title: '客户级别', width: 140, sortable: true, ellipsis: true },
  { key: 'warehouseName', title: '所属仓库', width: 100 },
  { key: 'region', title: '所属区域', width: 100 },
  { key: 'defaultHandlerName', title: '默认经手人', width: 100 },
  { key: 'promoterName', title: '推广人', width: 90 },
  { key: 'contactPerson', title: '联系人', width: 90 },
  { key: 'mnemonicCode', title: '助记码', width: 120, ellipsis: true },
  { key: 'phone', title: '联系电话', width: 120 },
  { key: 'address', title: '联系地址', width: 200, ellipsis: true },
  { key: 'buyerAccount', title: '买家账号', width: 120 },
  { key: 'customerOnePass', title: '客户一票通', width: 110 },
  { key: 'creditDays', title: '动态收款期限（天）', width: 150 },
  { key: 'fixedCreditDay', title: '固定账期', width: 100 },
  { key: 'statementDay', title: '结算期', width: 90 },
  { key: 'bankName', title: '开户银行', width: 130, ellipsis: true },
  { key: 'bankAccount', title: '银行账号', width: 150, ellipsis: true },
  { key: 'taxNumber', title: '税号', width: 150, ellipsis: true },
  { key: 'customerSource', title: '客户来源', width: 100 },
  { key: 'businessLicenseExpiry', title: '营业执照有效期', width: 140, formatter: (v: any) => formatDate(v) },
  { key: 'lastTradeTime', title: '最近交易时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'createTime', title: '新增时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'attachmentCount', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 90, align: 'center' },
  { key: 'remark', title: '备注', width: 160, ellipsis: true },
]

const MEMBER_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: ACTION_WIDTH.member, fixed: 'left' },
  { key: 'memberName', title: '会员名称', type: 'slot', slotName: 'nameCell', width: 150, ellipsis: true },
  { key: 'memberCardNo', title: '会员卡号', width: 140 },
  { key: 'partnerName', title: '客户名称', width: 170, ellipsis: true },
  { key: 'partnerCode', title: '客户编号', width: 135, defaultHidden: true, ellipsis: true },
  { key: 'contactPhone', title: '联系电话', width: 120, defaultHidden: true },
  { key: 'memberLevel', title: '会员级别', width: 120 },
  { key: 'memberCardStatusDesc', title: '会员卡状态', width: 100 },
  { key: 'memberValid', title: '有效时间', type: 'slot', slotName: 'memberValidCell', width: 210 },
  { key: 'birthday', title: '会员生日', width: 110, formatter: (v: any) => formatDate(v) },
  { key: 'points', title: '当前积分', width: 100, align: 'right' },
  { key: 'memberTotalConsume', title: '累计消费额', width: 120, align: 'right', defaultHidden: true, formatter: (v: any) => formatMoney(v) },
  { key: 'memberIssueTime', title: '发卡时间', width: 150, defaultHidden: true, formatter: (v: any) => formatDateTime(v) },
  { key: 'lastTradeTime', title: '最近交易时间', width: 150, formatter: (v: any) => formatDateTime(v) },
  { key: 'memberInitialPoints', title: '初始积分', width: 100, align: 'right', defaultHidden: true },
  { key: 'defaultHandlerName', title: '默认经手人', width: 110, defaultHidden: true },
  { key: 'remark', title: '备注', width: 150, ellipsis: true },
]

const CONTACT_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: ACTION_WIDTH.contact, fixed: 'left' },
  { key: 'contactName', title: '姓名', width: 110, ellipsis: true },
  { key: 'gender', title: '性别', width: 70 },
  { key: 'partnerName', title: '对应客户', width: 200, ellipsis: true },
  { key: 'partnerCode', title: '客户编号', width: 135, defaultHidden: true, ellipsis: true },
  { key: 'partyRegion', title: '客户所属区域', width: 120, defaultHidden: true },
  { key: 'handlerName', title: '客户经手人', width: 110, defaultHidden: true },
  { key: 'position', title: '职务', width: 100 },
  { key: 'mobile', title: '手机', width: 130 },
  { key: 'detailAddress', title: '联系地址', width: 240, ellipsis: true },
  { key: 'deliveryMethod', title: '配送方式', width: 100 },
  { key: 'logisticsCompany', title: '物流公司', width: 140, ellipsis: true },
  { key: 'outletName', title: '网点', width: 130, defaultHidden: true, ellipsis: true },
]

const GRADE_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: ACTION_WIDTH.grade, fixed: 'left' },
  { key: 'gradeName', title: '客户级别', width: 260, ellipsis: true },
  { key: 'defaultPrice', title: '级别默认价', width: 220 },
]

const REGION_COLUMNS: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: ACTION_WIDTH.region, fixed: 'left' },
  { key: 'regionCode', title: '区域编号', width: 180 },
  { key: 'regionName', title: '区域名称', width: 260, ellipsis: true },
  { key: 'remark', title: '备注', width: 320, ellipsis: true },
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  switch (activeTab.value) {
    case 'member': return MEMBER_COLUMNS
    case 'contact': return CONTACT_COLUMNS
    case 'grade': return GRADE_COLUMNS
    case 'region': return REGION_COLUMNS
    default: return ALL_COLUMNS
  }
})

const activeStorageKey = computed(() => `md-customer-columns-${activeTab.value}`)

// ═══ 数据状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
/** 选中记录（BillDetailTable 的 checkbox 事件载荷是记录本身，批量操作统一取 id） */
const selectedRows = ref<any[]>([])
const selectedIds = computed<any[]>(() => selectedRows.value.map((r: any) => r.id).filter(v => v != null))
const tableRef = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

function formatDate(v: any): string {
  if (!v) return ''
  const s = String(v).replace('T', ' ')
  return s.slice(0, 10)
}
function formatDateTime(v: any): string {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}
function formatMoney(v: any): string {
  if (v === null || v === undefined || v === '') return ''
  const n = Number(v)
  return Number.isNaN(n) ? String(v) : n.toFixed(2)
}
function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}
function escapeHtml(v: any): string {
  if (v === null || v === undefined) return ''
  return String(v).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string))
}

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<PartnerCategory[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => {
  // 区域管理：左侧为「地区分类」树（数据源 erp_customer_region，按 parentId 组树）
  if (activeTab.value === 'region') {
    return [{ id: ROOT_CATEGORY_ID, categoryName: '全部', children: toRegionTreeNodes(regionTreeList.value) }]
  }
  return [{ id: ROOT_CATEGORY_ID, categoryName: '全部客户', children: categoryList.value }]
})

/** 区域平铺列表 → 分类树节点（字段名对齐 CategoryListLayout 的 categoryName） */
function toRegionTreeNodes(list: any[]): any[] {
  return (list || []).map((r: any) => ({
    ...r,
    categoryName: r.regionName,
    children: r.children?.length ? toRegionTreeNodes(r.children) : [],
  }))
}

const currentPath = computed(() => {
  const isRegion = activeTab.value === 'region'
  const rootLabel = isRegion ? '全部' : '全部客户'
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return rootLabel
  const source = isRegion ? toRegionTreeNodes(regionTreeList.value) : categoryList.value
  const path: string[] = []
  const walk = (nodes: any[], target: string): boolean => {
    for (const n of nodes) {
      path.push(n.categoryName)
      if (String(n.id) === target) return true
      if (n.children?.length && walk(n.children, target)) return true
      path.pop()
    }
    return false
  }
  walk(source, String(selectedCategoryId.value))
  return path.length ? path.join(' / ') : rootLabel
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await partnerCategoryApi.getTree('CUSTOMER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[客户] 分类树加载失败', e)
    categoryError.value = true
    categoryList.value = []
  } finally {
    categoryLoading.value = false
  }
}

/** 区域管理左侧「地区分类」树（对标实测：区域管理同样带左树） */
const regionTreeList = ref<any[]>([])
async function loadRegionTree() {
  try {
    const list = await customerRegionApi.list({ showHierarchy: true })
    regionTreeList.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[客户] 地区分类树加载失败', e)
    regionTreeList.value = []
  }
}

function handleCategorySelect(keys: (string | number)[]) {
  const key = keys[0]
  selectedCategoryId.value = key != null ? String(key) : ROOT_CATEGORY_ID
  pagination.current = 1
  loadData()
}

/** 在分类 / 区域树中按 id 定位节点 */
function findTreeNode(nodes: any[], id: string): any | null {
  for (const n of nodes) {
    if (String(n.id) === id) return n
    if (n.children?.length) {
      const hit = findTreeNode(n.children, id)
      if (hit) return hit
    }
  }
  return null
}

/** 当前子标签左树的数据源（区域管理为「地区分类」，其余为「客户分类」） */
function currentTreeSource(): any[] {
  return activeTab.value === 'region' ? toRegionTreeNodes(regionTreeList.value) : categoryList.value
}

/** 分类树标题栏「修改」：对标未选中节点时提示「请选择要修改的分类！」 */
function handleCategoryHeaderEdit() {
  const isRegion = activeTab.value === 'region'
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) {
    message.info(isRegion ? '请选择要修改的区域！' : '请选择要修改的分类！')
    return
  }
  const node = findTreeNode(currentTreeSource(), String(selectedCategoryId.value))
  if (!node) return
  if (isRegion) handleEditRow(node)
  else openCategoryModal(node)
}

/** 分类树标题栏「删除」 */
function handleCategoryHeaderDelete() {
  const isRegion = activeTab.value === 'region'
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) {
    message.info(isRegion ? '请选择要删除的区域！' : '请选择要删除的分类！')
    return
  }
  const node = findTreeNode(currentTreeSource(), String(selectedCategoryId.value))
  if (!node) return
  if (isRegion) handleDeleteRow(node)
  else handleCategoryDelete(node)
}

// 分类新增/编辑
const categoryModalVisible = ref(false)
const categorySaving = ref(false)
const editingCategory = ref<PartnerCategory | null>(null)
const categoryFormRef = ref<any>(null)
const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
})
const categoryRules = { categoryName: [{ required: true, message: '请输入分类名称' }] }

function openCategoryModal(node: any) {
  editingCategory.value = node || null
  if (node && node.id !== ROOT_CATEGORY_ID) {
    Object.assign(categoryForm, {
      categoryName: node.categoryName,
      categoryCode: node.categoryCode,
      parentId: node.parentId || undefined,
      sortOrder: node.sortOrder || 0,
    })
  } else {
    Object.assign(categoryForm, {
      categoryName: '',
      categoryCode: '',
      parentId: String(selectedCategoryId.value) !== ROOT_CATEGORY_ID ? Number(selectedCategoryId.value) : undefined,
      sortOrder: 0,
    })
  }
  categoryModalVisible.value = true
}

async function handleCategorySave() {
  try {
    await categoryFormRef.value?.validate()
    categorySaving.value = true
    const payload: Partial<PartnerCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId,
      sortOrder: categoryForm.sortOrder,
      categoryType: 'CUSTOMER',
    }
    if (editingCategory.value) {
      await partnerCategoryApi.update(editingCategory.value.id, payload)
      message.success('分类已更新')
    } else {
      await partnerCategoryApi.create(payload)
      message.success('分类已新增')
    }
    categoryModalVisible.value = false
    await loadCategoryTree()
  } catch (e: any) {
    if (e?.errorFields) return
    message.error(e?.message || '保存失败')
  } finally {
    categorySaving.value = false
  }
}

function handleCategoryDelete(node: any) {
  Modal.confirm({
    title: '删除分类',
    content: `确定删除分类「${node.categoryName}」吗？分类下存在客户时无法删除。`,
    okType: 'danger',
    onOk: async () => {
      const result = await partnerCategoryApi.delete(node.id) as any
      if (typeof result === 'string' && result) {
        message.warning(result)
        return
      }
      message.success('分类已删除')
      if (String(selectedCategoryId.value) === String(node.id)) {
        selectedCategoryId.value = ROOT_CATEGORY_ID
      }
      await loadCategoryTree()
      await loadData()
    },
  })
}

async function loadMemberLevelOptions() {
  try {
    const list: any = await request.get('/erp/member-level/list')
    memberLevelOptions.value = (list || []).map((l: any) => ({ label: l.levelName, value: l.levelName }))
  } catch {
    memberLevelOptions.value = []
  }
}

async function loadGradeOptions() {
  try {
    const list = await partnerGradeApi.list()
    gradeOptions.value = (list || []).map((g: any) => ({ label: g.gradeName, value: g.gradeName }))
  } catch (e) {
    console.warn('[客户] 客户级别加载失败', e)
  }
}

// ═══ 数据加载（按子标签分派） ═══
async function loadData() {
  loading.value = true
  try {
    if (activeTab.value === 'all') {
      const params: Record<string, any> = {
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: allQuery.keyword || undefined,
        address: allQuery.address || undefined,
        createTimeStart: allQuery.createTimeStart || undefined,
        createTimeEnd: allQuery.createTimeEnd || undefined,
        lastTradeStart: allQuery.lastTradeStart || undefined,
        lastTradeEnd: allQuery.lastTradeEnd || undefined,
        settleType: allQuery.settleType || undefined,
        gradeName: allQuery.gradeName || undefined,
        handler: allQuery.handler || undefined,
        promoter: allQuery.promoter || undefined,
        warehouse: allQuery.warehouse || undefined,
        region: allQuery.region || undefined,
        customerSource: allQuery.customerSource || undefined,
        status: allQuery.status || undefined,
        showHierarchy: allQuery.showHierarchy || undefined,
        onlyMallAccount: allQuery.onlyMallAccount || undefined,
        onlyNoTrade: allQuery.onlyNoTrade || undefined,
        showAsCustomer: allQuery.showAsCustomer || undefined,
        partnerType: 'customer',
      }
      if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
        params.categoryId = Number(selectedCategoryId.value)
      }
      const res: any = await partnerApi.page(params)
      tableData.value = res?.records || []
      pagination.total = Number(res?.total || 0)
    } else if (activeTab.value === 'member') {
      const res: any = await partnerApi.memberPage({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        memberKeyword: memberQuery.memberKeyword || memberQuery.keyword || undefined,
        phone: memberQuery.phone || undefined,
        customerId: memberQuery.customerId || undefined,
        handler: memberQuery.handler || undefined,
        lastTradeStart: memberQuery.lastTradeStart || undefined,
        lastTradeEnd: memberQuery.lastTradeEnd || undefined,
        memberLevel: memberQuery.memberLevel || undefined,
        memberCardStatus: memberQuery.memberCardStatus || undefined,
        birthday: memberQuery.birthday || undefined,
        ageMin: memberQuery.ageMin ?? undefined,
        ageMax: memberQuery.ageMax ?? undefined,
        pointsMin: memberQuery.pointsMin ?? undefined,
        pointsMax: memberQuery.pointsMax ?? undefined,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total || 0)
    } else if (activeTab.value === 'contact') {
      const res: any = await partnerApi.contactPage({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: contactQuery.keyword || undefined,
        customerId: contactQuery.customerId || undefined,
        handler: contactQuery.handler || undefined,
        address: contactQuery.address || undefined,
        deliveryMethod: contactQuery.deliveryMethod || undefined,
        region: contactQuery.region || undefined,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total || 0)
    } else if (activeTab.value === 'grade') {
      // 客户级别统一走 /erp/partner/grades（biz_customer_grade），与客户表单/筛选下拉同源
      const res: any = await partnerGradeApi.page({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: gradeQuery.keyword || undefined,
      })
      const records = (res?.records || res?.data?.records || []).map((r: any) => ({
        ...r,
        defaultPrice: r.discountRate != null ? `${r.gradeName}*${(Number(r.discountRate) / 100).toFixed(2)}` : '-',
      }))
      tableData.value = records
      pagination.total = Number(res?.total ?? res?.data?.total ?? records.length)
    } else if (activeTab.value === 'region') {
      const res: any = await customerRegionApi.page({
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
        keyword: regionQuery.keyword || undefined,
        showHierarchy: regionQuery.showHierarchy || undefined,
        // 左侧「地区分类」树选中节点 → 只显示该区域及其子区域
        regionId: String(selectedCategoryId.value) !== ROOT_CATEGORY_ID ? Number(selectedCategoryId.value) : undefined,
      })
      tableData.value = res?.records || []
      pagination.total = Number(res?.total || 0)
    }
  } catch (e: any) {
    console.warn('[客户] 列表加载失败', e)
    message.error(e?.message || '加载失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleTabChange(key: string) {
  activeTab.value = key
  selectedRows.value = []
  pagination.current = 1
  pagination.pageSize = 20
  selectedCategoryId.value = ROOT_CATEGORY_ID
  loadPageConfig()
  loadData()
  if (key === 'region') {
    loadRegionParentOptions()
    loadRegionTree()
  }
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadData()
}

/** 单行勾选：占位空行（__ghost）不参与批量操作 */
function handleRowCheck(record: any, _rowIndex?: number, checked?: boolean) {
  if (!record || record.__ghost) return
  const exists = selectedRows.value.some((r: any) => String(r.id) === String(record.id))
  if (checked === false) {
    selectedRows.value = selectedRows.value.filter((r: any) => String(r.id) !== String(record.id))
  } else if (!exists) {
    selectedRows.value = [...selectedRows.value, record]
  }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  selectedRows.value = checked ? (records || []).filter((r: any) => r && !r.__ghost) : []
}

function refreshAll() {
  loadCategoryTree()
  loadGradeOptions()
  loadMemberLevelOptions()
  if (activeTab.value === 'region') loadRegionTree()
  loadData()
}

// ═══ 行操作 ═══
function handleAdd() {
  if (activeTab.value === 'contact') {
    contactModalRecord.value = null
    contactModalOpen.value = true
    return
  }
  if (activeTab.value === 'member') {
    memberModalRecord.value = null
    memberModalOpen.value = true
    return
  }
  if (activeTab.value === 'grade') {
    Object.assign(gradeForm, { id: undefined, gradeName: '', discountRate: 100 })
    gradeModalVisible.value = true
    return
  }
  if (activeTab.value === 'region') {
    Object.assign(regionForm, { id: undefined, regionName: '', regionCode: '', parentId: undefined, remark: '' })
    regionModalVisible.value = true
    return
  }
  router.push('/md/customer/form')
}

function handleEdit(record: any) {
  router.push({ path: '/md/customer/form', query: { id: record.id } })
}

function handleView(record: any) {
  handleEdit(record)
}

function handleEditMember(record: any) {
  memberModalRecord.value = record
  memberModalOpen.value = true
}

function handleEditRow(record: any) {
  if (activeTab.value === 'grade') {
    Object.assign(gradeForm, {
      id: record.id,
      gradeName: record.gradeName,
      discountRate: record.discountRate ?? 100,
    })
    gradeModalVisible.value = true
  } else if (activeTab.value === 'region') {
    Object.assign(regionForm, {
      id: record.id,
      regionName: record.regionName,
      regionCode: record.regionCode,
      parentId: record.parentId || undefined,
      remark: record.remark || '',
    })
    regionModalVisible.value = true
  } else {
    // 全部联系人：打开联系人弹窗（独立联系人实体，可维护其服务的多个往来单位）
    contactModalRecord.value = record
    contactModalOpen.value = true
  }
}

function handleDeleteRow(record: any) {
  if (activeTab.value === 'grade') {
    Modal.confirm({
      title: '删除客户级别',
      content: `确定删除「${record.gradeName}」吗？`,
      okType: 'danger',
      onOk: async () => {
        try {
          await partnerGradeApi.delete(record.id)
          message.success('已删除')
          loadData()
        } catch (e: any) {
          message.error(e?.message || '删除失败')
        }
      },
    })
  } else if (activeTab.value === 'region') {
    Modal.confirm({
      title: '删除区域',
      content: `确定删除区域「${record.regionName}」吗？`,
      okType: 'danger',
      onOk: async () => {
        const result = await customerRegionApi.remove(record.id) as any
        if (typeof result === 'string' && result) {
          message.warning(result)
          return
        }
        message.success('区域已删除')
        await loadData()
        await loadRegionParentOptions()
        await loadRegionTree()
      },
    })
  } else {
    Modal.confirm({
      title: '删除联系人',
      content: `确定删除联系人「${record.contactName}」吗？`,
      okType: 'danger',
      onOk: async () => {
        await request.delete(`/erp/partner/contacts/${record.id}`)
        message.success('已删除')
        loadData()
      },
    })
  }
}

function handleToggleStatus(record: any) {
  const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const text = next === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: `确认${text}`,
    content: `确定要${text}「${record.partnerName}」吗？`,
    onOk: async () => {
      await partnerApi.updateStatus(record.id, next)
      message.success(`已${text}`)
      loadData()
    },
  })
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${record.partnerName || record.memberName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      await partnerApi.delete(record.id)
      message.success('删除成功')
      loadData()
    },
  })
}

/** 代客下单：直达销售订单表单并预填客户 */
function handlePlaceOrder(record: any) {
  router.push({ path: '/erp/sale/form', query: { customerId: record.id, customerName: record.partnerName } })
}

// ═══ 批量操作 ═══
const moveModalVisible = ref(false)
const moveSaving = ref(false)
const moveCategoryId = ref<any>(undefined)

function openMoveModal() {
  moveCategoryId.value = undefined
  moveModalVisible.value = true
}

async function handleBatchMove() {
  if (!moveCategoryId.value) {
    message.warning('请选择目标分类')
    return
  }
  moveSaving.value = true
  try {
    await partnerApi.batchMove(selectedIds.value, moveCategoryId.value)
    message.success('搬移成功')
    moveModalVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e?.message || '搬移失败')
  } finally {
    moveSaving.value = false
  }
}

async function handleBatchStatus(status: string) {
  const text = status === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: `批量${text}`,
    content: `确定要${text}选中的 ${selectedIds.value.length} 个客户吗？`,
    onOk: async () => {
      await partnerApi.batchStatus(selectedIds.value, status)
      message.success(`已批量${text}`)
      loadData()
    },
  })
}

async function handleBatchPriceTrack(enabled: boolean) {
  const text = enabled ? '启用' : '取消'
  Modal.confirm({
    title: `${text}价格跟踪`,
    content: `确定要${text}选中的 ${selectedIds.value.length} 个客户的价格跟踪吗？`,
    onOk: async () => {
      await partnerApi.batchPriceTrack(selectedIds.value, enabled)
      message.success(`已${text}价格跟踪`)
      loadData()
    },
  })
}

function handleBatchDelete() {
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedIds.value.length} 个客户吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      await partnerApi.batchDelete(selectedIds.value)
      message.success('批量删除成功')
      loadData()
    },
  })
}

/** 开通商城账号：以客户编号生成买家账号（已有账号则保留） */
function handleOpenMallAccount() {
  const targets = tableData.value.filter(r => selectedIds.value.some(id => String(id) === String(r.id)))
  const pending = targets.filter(r => !r.buyerAccount)
  if (pending.length === 0) {
    message.info('选中的客户均已开通商城账号')
    return
  }
  Modal.confirm({
    title: '开通商城账号',
    content: `将为 ${pending.length} 个客户开通商城账号（买家账号 = 客户编号）。`,
    onOk: async () => {
      for (const row of pending) {
        await partnerApi.update(row.id, { buyerAccount: row.partnerCode })
      }
      message.success('已开通商城账号')
      loadData()
    },
  })
}

// 批量修改
const batchModifyVisible = ref(false)
const batchSaving = ref(false)
const batchForm = reactive({
  gradeName: undefined as string | undefined,
  settleType: undefined as string | undefined,
  region: '',
  customerSource: undefined as string | undefined,
})

function handleBatchModify() {
  Object.assign(batchForm, { gradeName: undefined, settleType: undefined, region: '', customerSource: undefined })
  batchModifyVisible.value = true
}

async function handleBatchModifySave() {
  const payload: Record<string, any> = {}
  if (batchForm.gradeName) payload.gradeName = batchForm.gradeName
  if (batchForm.settleType) payload.settleType = batchForm.settleType
  if (batchForm.region) payload.region = batchForm.region
  if (batchForm.customerSource) payload.customerSource = batchForm.customerSource
  if (Object.keys(payload).length === 0) {
    message.warning('请至少填写一项要修改的内容')
    return
  }
  batchSaving.value = true
  try {
    for (const id of selectedIds.value) {
      await partnerApi.update(id, payload)
    }
    message.success('批量修改成功')
    batchModifyVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e?.message || '批量修改失败')
  } finally {
    batchSaving.value = false
  }
}

// ═══ 客户级别 / 区域 弹窗 ═══
const gradeModalVisible = ref(false)
const gradeSaving = ref(false)
const gradeForm = reactive({ id: undefined as any, gradeName: '', discountRate: 100 })

async function handleGradeSave() {
  if (!gradeForm.gradeName) {
    message.warning('请输入客户级别名称')
    return
  }
  gradeSaving.value = true
  try {
    // 字段口径与 biz_customer_grade 对齐（级别名称同时作为编码，与其他主数据一致）
    const payload = {
      gradeName: gradeForm.gradeName,
      gradeCode: gradeForm.gradeName,
      discountRate: gradeForm.discountRate,
      status: 1,
    }
    if (gradeForm.id) {
      await partnerGradeApi.update(gradeForm.id, payload)
    } else {
      await partnerGradeApi.create(payload)
    }
    message.success('保存成功')
    gradeModalVisible.value = false
    await loadGradeOptions()
    loadData()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    gradeSaving.value = false
  }
}

const regionModalVisible = ref(false)
const regionSaving = ref(false)
const regionFormRef = ref<any>(null)
const regionForm = reactive({
  id: undefined as any,
  regionName: '',
  regionCode: '',
  parentId: undefined as any,
  remark: '',
})
const regionRules = { regionName: [{ required: true, message: '请输入区域名称' }] }
const regionParentOptions = ref<Array<{ label: string; value: any }>>([])

async function loadRegionParentOptions() {
  try {
    const list = await customerRegionApi.list()
    regionParentOptions.value = (list || []).map(r => ({ label: r.regionName, value: r.id }))
  } catch (e) {
    console.warn('[客户] 区域列表加载失败', e)
    regionParentOptions.value = []
  }
}

async function handleRegionSave() {
  try {
    await regionFormRef.value?.validate()
  } catch {
    return
  }
  regionSaving.value = true
  try {
    const payload: any = {
      regionName: regionForm.regionName,
      regionCode: regionForm.regionCode || undefined,
      parentId: regionForm.parentId || 0,
      remark: regionForm.remark,
    }
    if (regionForm.id) {
      await customerRegionApi.update(regionForm.id, payload)
    } else {
      await customerRegionApi.create(payload)
    }
    message.success('保存成功')
    regionModalVisible.value = false
    await loadData()
    await loadRegionParentOptions()
    await loadRegionTree()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    regionSaving.value = false
  }
}

// ═══ 打印 / 导出 ═══
function currentPrintColumns(): Array<{ title: string; key: string; fmt?: (v: any, r: any) => string }> {
  if (activeTab.value === 'member') {
    return [
      { title: '会员名称', key: 'memberName' },
      { title: '会员卡号', key: 'memberCardNo' },
      { title: '客户名称', key: 'partnerName' },
      { title: '会员级别', key: 'memberLevel' },
      { title: '会员卡状态', key: 'memberCardStatusDesc' },
      { title: '会员生日', key: 'birthday', fmt: (v: any) => formatDate(v) },
      { title: '当前积分', key: 'points' },
      { title: '累计消费额', key: 'memberTotalConsume', fmt: (v: any) => formatMoney(v) },
      { title: '最近交易时间', key: 'lastTradeTime', fmt: (v: any) => formatDateTime(v) },
      { title: '备注', key: 'remark' },
    ]
  }
  if (activeTab.value === 'contact') {
    return [
      { title: '姓名', key: 'contactName' },
      { title: '性别', key: 'gender' },
      { title: '对应客户', key: 'partnerName' },
      { title: '客户编号', key: 'partnerCode' },
      { title: '职务', key: 'position' },
      { title: '手机', key: 'mobile' },
      { title: '联系地址', key: 'detailAddress' },
      { title: '配送方式', key: 'deliveryMethod' },
      { title: '物流公司', key: 'logisticsCompany' },
    ]
  }
  if (activeTab.value === 'grade') {
    return [
      { title: '客户级别', key: 'gradeName' },
      { title: '级别默认价', key: 'defaultPrice' },
    ]
  }
  if (activeTab.value === 'region') {
    return [
      { title: '区域编号', key: 'regionCode' },
      { title: '区域名称', key: 'regionName' },
      { title: '备注', key: 'remark' },
    ]
  }
  return [
    { title: '客户编号', key: 'partnerCode' },
    { title: '客户名称', key: 'partnerName' },
    { title: '结款方式', key: 'settleType' },
    { title: '客户级别', key: 'gradeName' },
    { title: '所属仓库', key: 'warehouseName' },
    { title: '所属区域', key: 'region' },
    { title: '默认经手人', key: 'defaultHandlerName' },
    { title: '联系人', key: 'contactPerson' },
    { title: '联系电话', key: 'phone' },
    { title: '联系地址', key: 'address' },
    { title: '开户银行', key: 'bankName' },
    { title: '银行账号', key: 'bankAccount' },
    { title: '备注', key: 'remark' },
  ]
}

const TAB_TITLE: Record<string, string> = {
  all: '客户',
  member: '会员管理',
  contact: '全部联系人',
  grade: '客户级别',
  region: '区域管理',
}

function handlePrint() {
  if (!tableData.value.length) {
    message.warning('没有可打印的数据')
    return
  }
  const cols = currentPrintColumns()
  const rows = tableData.value.map((r, i) => `<tr><td>${i + 1}</td>${cols
    .map(c => `<td>${escapeHtml(c.fmt ? c.fmt(r[c.key], r) : r[c.key])}</td>`)
    .join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>${TAB_TITLE[activeTab.value]}</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;gap:24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>${TAB_TITLE[activeTab.value]}</h2>
    <div class="meta">
      <span>当前路径：${escapeHtml(currentPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${tableData.value.length}</span>
    </div>
    <table>
      <thead><tr><th>序号</th>${cols.map(c => `<th>${escapeHtml(c.title)}</th>`).join('')}</tr></thead>
      <tbody>${rows}</tbody>
    </table>
  </body></html>`
  const w = window.open('', '_blank', 'width=1200,height=800')
  if (!w) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  setTimeout(() => w.print(), 300)
}

async function handleExport() {
  exporting.value = true
  try {
    if (activeTab.value === 'all') {
      const params: Record<string, any> = {
        partnerType: 'customer',
        title: '客户',
        keyword: allQuery.keyword || undefined,
        status: allQuery.status || undefined,
        settleType: allQuery.settleType || undefined,
        region: allQuery.region || undefined,
        handler: allQuery.handler || undefined,
        address: allQuery.address || undefined,
        gradeName: allQuery.gradeName || undefined,
        warehouse: allQuery.warehouse || undefined,
        promoter: allQuery.promoter || undefined,
        customerSource: allQuery.customerSource || undefined,
        createTimeStart: allQuery.createTimeStart || undefined,
        createTimeEnd: allQuery.createTimeEnd || undefined,
        lastTradeStart: allQuery.lastTradeStart || undefined,
        lastTradeEnd: allQuery.lastTradeEnd || undefined,
        onlyMallAccount: allQuery.onlyMallAccount || undefined,
        onlyNoTrade: allQuery.onlyNoTrade || undefined,
        showAsCustomer: allQuery.showAsCustomer || undefined,
      }
      if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
        params.categoryId = Number(selectedCategoryId.value)
      }
      const blob: any = await partnerApi.export(params)
      downloadBlob(blob, `客户_${dayjs().format('YYYYMMDD')}.xlsx`)
    } else {
      // 子标签导出：按当前列与查询条件生成 CSV（Excel 可直接打开）
      const cols = currentPrintColumns()
      const header = cols.map(c => c.title).join(',')
      const lines = tableData.value.map((r, i) => [
        i + 1,
        ...cols.map(c => {
          const v = c.fmt ? c.fmt(r[c.key], r) : r[c.key]
          const s = v === null || v === undefined ? '' : String(v)
          return `"${s.replace(/"/g, '""')}"`
        }),
      ].join(','))
      const csv = `\ufeff${header}\n${lines.join('\n')}`
      downloadBlob(new Blob([csv], { type: 'text/csv;charset=utf-8' }), `${TAB_TITLE[activeTab.value]}_${dayjs().format('YYYYMMDD')}.csv`)
    }
    message.success('导出成功')
  } catch (e: any) {
    console.warn('[客户] 导出失败', e)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function downloadBlob(blob: any, filename: string) {
  const url = window.URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}

// ═══ 导入 ═══
const importModalVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])

async function handleImportUpload() {
  if (importFileList.value.length === 0) {
    message.warning('请先选择要上传的文件')
    return
  }
  const file = importFileList.value[0]
  const formData = new FormData()
  formData.append('file', file.originFileObj || file)
  importLoading.value = true
  try {
    // ⚠️ 改调资料模块自带的**真实**导入端点：`/import/v2/excel/customer`（cn.aiedge.export）
    //    只解析校验、完全不落库，却回执「成功 N 条」（2026-09-24 系统模块审计 P0）。
    //    本端点会做编码生成 + 编号查重 + 真实写入 erp_partner。
    const res: any = await request.post('/erp/md/customer/import-excel?partnerType=customer', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const errors: string[] = Array.isArray(res?.errors) ? res.errors : []
    const okCount = Number(res?.success) || 0
    if (errors.length) {
      message.warning(
        `导入完成：成功 ${okCount} 条，失败 ${errors.length} 条。${errors.slice(0, 2).join('；')}${errors.length > 2 ? ' …' : ''}`,
        8,
      )
    } else {
      message.success(`导入成功 ${okCount} 条`)
    }
    importModalVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e?.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// ═══ 营销 / 会员联动入口 ═══
function handleSendSms() {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择客户')
    return
  }
  message.info(`已选择 ${selectedIds.value.length} 个客户：短信模板与发送通道在「营销 → 短信营销」中配置后发送`)
}

function handleSendCoupon() {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择客户')
    return
  }
  message.info(`已选择 ${selectedIds.value.length} 个客户：优惠券模板在「营销 → 优惠券」中配置后发放`)
}

function handleMemberSetting() {
  router.push('/md/customer/form')
  message.info('会员卡等级与积分规则在客户表单「会员信息」中维护')
}

function handleImportRoutes() {
  message.info('默认线路/物流导入：请在「资料 → 配送管理 → 线路」维护线路后为联系人设置配送方式')
}

function handleSetDeliveryMethod() {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择联系人')
    return
  }
  Modal.confirm({
    title: '设置配送方式',
    content: `将为选中的 ${selectedIds.value.length} 个联系人设置为「自配」。`,
    onOk: async () => {
      for (const id of selectedIds.value) {
        await request.put(`/erp/partner/contacts/${id}`, { deliveryMethod: '自配' })
      }
      message.success('已设置配送方式')
      loadData()
    },
  })
}

function handleImportPoints() {
  message.info('批量导入积分：请在「营销 → 会员管理」导入积分明细')
}

// ═══ 客户/联系人下拉检索 ═══
async function handleMemberCustomerSearch() {
  const kw = memberQuery.customerKeyword
  if (!kw) {
    memberQuery.customerId = undefined
    handleSearch()
    return
  }
  try {
    const list = await partnerApi.search(kw, 'customer')
    if (!list || list.length === 0) {
      message.warning('未找到匹配客户')
      return
    }
    memberQuery.customerId = list[0].id
    handleSearch()
  } catch (e: any) {
    message.error(e?.message || '检索失败')
  }
}

async function handleContactCustomerSearch() {
  const kw = contactQuery.customerKeyword
  if (!kw) {
    contactQuery.customerId = undefined
    handleSearch()
    return
  }
  try {
    const list = await partnerApi.search(kw, 'customer')
    if (!list || list.length === 0) {
      message.warning('未找到匹配客户')
      return
    }
    contactQuery.customerId = list[0].id
    handleSearch()
  } catch (e: any) {
    message.error(e?.message || '检索失败')
  }
}

// ═══ 附件 / 积分明细 ═══
const attachModalVisible = ref(false)
const attachLoading = ref(false)
const attachList = ref<PartnerAttachment[]>([])
const attachPartnerName = ref('')

async function showAttachments(record: any) {
  attachPartnerName.value = record.partnerName || ''
  attachModalVisible.value = true
  attachLoading.value = true
  try {
    attachList.value = await partnerAttachmentApi.getByPartner(record.id)
  } catch {
    attachList.value = []
  } finally {
    attachLoading.value = false
  }
}

const pointsModalVisible = ref(false)
const pointsMemberName = ref('')
const pointsCurrent = ref(0)
const pointsConsume = ref(0)
const pointsRows = ref<any[]>([])
const pointsColumns = [
  { title: '时间', dataIndex: 'time', key: 'time', width: 160 },
  { title: '积分变动', dataIndex: 'change', key: 'change', width: 100 },
  { title: '说明', dataIndex: 'remark', key: 'remark' },
]

function handlePointsDetail(record: any) {
  pointsMemberName.value = record.memberName || record.partnerName
  pointsCurrent.value = record.points ?? 0
  pointsConsume.value = record.memberTotalConsume ?? 0
  pointsRows.value = [
    {
      id: 'init',
      time: formatDateTime(record.memberIssueTime),
      change: record.memberInitialPoints ?? 0,
      remark: '开卡初始积分',
    },
  ]
  pointsModalVisible.value = true
}

// ═══ 键盘快捷键（F8 打印） ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  loadPageConfig()
  loadCategoryTree()
  loadGradeOptions()
  loadRegionParentOptions()
  loadMemberSchemes()
  loadData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-row.second-row {
  margin-top: 8px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
/* 全部客户 / 会员管理：对齐对标「每行 6 列、列宽约 226px」的查询区布局
   （对标实测：筛选条件/联系地址/新增日期起止/最近交易起止 = 第1行；
     结款方式/默认经手人/所属区域/显示状态/查询/勾选项 = 第2行） */
.search-grid .search-item {
  flex: 0 0 218px;
  min-width: 0;
}
.search-grid .search-item :deep(.ant-input),
.search-grid .search-item :deep(.ant-input-affix-wrapper),
.search-grid .search-item :deep(.ant-picker),
.search-grid .search-item :deep(.ant-select) {
  flex: 1;
  width: auto !important;
  min-width: 0;
}
.search-grid .search-checkbox-group {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex: 1 1 auto;
  white-space: nowrap;
}
/* 对标：默认经手人 / 所属区域 输入框右侧带放大镜图标 */
.search-suffix-icon {
  color: #bfbfbf;
  font-size: 12px;
}
.query-scheme-wrap {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-right: 8px;
}
.range-sep {
  color: #999;
  padding: 0 2px;
}
.btn-search {
  margin-left: 8px;
}
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
.tree-node-name {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attach-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px dashed #f0f0f0;
}
.attach-name {
  flex: 1;
}
.attach-size {
  color: #999;
  font-size: 12px;
}
</style>
