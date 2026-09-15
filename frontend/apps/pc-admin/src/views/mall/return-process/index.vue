<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        退货申请处理（商城 → 订单处理 → 退货申请处理，交易模块菜单 60015）
        · 对标：ql361「商城 → 订单处理 → 退货申请处理」；本页为商城退货申请（售后）受理工作台（列表复合页）
        · 骨架（金标准路线 A）：ErrorBoundary > PageContainer(full-height) > CategoryListLayout + BillDetailTable + StandardPagination
        · 2 个视图 Tab（按单据 / 按明细），逐 Tab 独立一套：列定义 + 查询条件 + 功能按钮 + storage-key
        · 列口径严格取自 docs/Yh-Spec/手动整理对标开发文档/交易模块/退货申请处理开发文档.md
          - 按单据：文档列名清单 28 项 → 对标默认显示 20、隐藏 8（审核时间 + 表头自定义字段1~5）
          - 按明细：文档列名清单 44 项 → 对标默认显示 32、隐藏 12（审核时间 + 表头自定义1~5 + 单据自定义1~5）
          - 「收货」为父分组（订货数量/已收数量/未收数量）
          - ⚠️ 文档「全部配置数据列」的**编号计数**（26 / 40）小于其**列名清单**实际条目（28 / 44）；
            本实现以「列名清单 + 对标默认显示列名 + 文档明示的隐藏列」为准逐条落列，保证默认显示口径与对标一致。
        · API（保留本页既有接入）：saleReturnApi —— /erp/sale/return/page、/page-detail、/{id}、/{id}/items、
          /{id}/approve、/{id}/reject、/{id}/complete、/export；查询条件逐项按后端 /page、/page-detail 的同名参数传参
          （「结算单位」无独立列，后端按客户快照 customer_name 过滤，与 finance/receipt-doc 同口径）
        · ⚠️ 数据源说明：本页复用**销售退货单申请** API（/erp/sale/return/*）。
          api/erp/mall.ts 另有 /erp/mall/admin/return/*（approve/reject/batch-approve/batch-reject）未接入——
          是否切换为独立商城退货数据源待业务确认（开发文档「待完善 5. API 对齐」）。
        · 后端字段口径（本轮已对齐，见开发文档「后端改动（本轮）」）：
          - 按单据 /page 返回 SaleReturn 实体：收货三列（orderedQuantity/receivedQuantity/unreceivedQuantity）、
            returnQuantityTotal、settledAmount、billAmount、extNum1/extNum2/extText3~5、auditTime 均为真实列；
          - 按明细 /page-detail 返回强类型 SaleReturnItemPageDTO（原 Page<Map>）：
            表头自定义字段 → headerExtNum1/headerExtNum2/headerExtText3~5；
            单据自定义字段（明细行）→ extNum1~3 / extText1~2（沿用 sales/return-apply 同端点口径）；
            审核备注 → approvedNote（真实列 approved_note：审批意见/拒绝原因）。
          - 仍无列可承载的展示项：`audit_remark`（库中无该列）→ 以 approvedNote 承载，见开发文档说明。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏右侧：页面配置 / 刷新 / 打印(F8) / 导出 ═══ -->
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
              @click="handleRefresh"
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

        <!-- ═══ 查询区：逐 Tab 独立条件，显隐由页面配置控制 ═══ -->
        <template #search-fields>
          <div class="search-area">
            <!-- ── 按单据：16 个查询条件字段（含隐藏） ── -->
            <div
              v-if="activeTab === 'doc'"
              class="search-row"
            >
              <div
                v-if="isQueryVisible('doc.dateRange')"
                class="search-item"
              >
                <span class="search-label">单据时间</span>
                <a-range-picker
                  v-model:value="docDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 220px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.returnNo')"
                class="search-item"
              >
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="docQuery.returnNo"
                  placeholder="退货单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.customerName')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="docQuery.customerName"
                  placeholder="客户名称"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.settleUnit')"
                class="search-item"
              >
                <span class="search-label">结算单位</span>
                <a-input
                  v-model:value="docQuery.settleUnit"
                  placeholder="结算单位"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.status')"
                class="search-item"
              >
                <span class="search-label">单据状态</span>
                <a-select
                  v-model:value="docQuery.status"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.sourceOrder')"
                class="search-item"
              >
                <span class="search-label">来源订单</span>
                <a-input
                  v-model:value="docQuery.sourceOrder"
                  placeholder="来源订单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.returnType')"
                class="search-item"
              >
                <span class="search-label">退货类型</span>
                <a-input
                  v-model:value="docQuery.returnType"
                  placeholder="退货类型"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.handlerName')"
                class="search-item"
              >
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="docQuery.handlerName"
                  placeholder="经手人"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.warehouseName')"
                class="search-item"
              >
                <span class="search-label">仓库</span>
                <a-input
                  v-model:value="docQuery.warehouseName"
                  placeholder="仓库"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.remark')"
                class="search-item"
              >
                <span class="search-label">单据备注</span>
                <a-input
                  v-model:value="docQuery.remark"
                  placeholder="单据备注"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.auditTime')"
                class="search-item"
              >
                <span class="search-label">审核时间</span>
                <a-date-picker
                  v-model:value="docAuditTime"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 140px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.extNum1')"
                class="search-item"
              >
                <span class="search-label">自定义1</span>
                <a-input
                  v-model:value="docQuery.extNum1"
                  placeholder="表头自定义字段1"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.extNum2')"
                class="search-item"
              >
                <span class="search-label">自定义2</span>
                <a-input
                  v-model:value="docQuery.extNum2"
                  placeholder="表头自定义字段2"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.extText3')"
                class="search-item"
              >
                <span class="search-label">自定义3</span>
                <a-input
                  v-model:value="docQuery.extText3"
                  placeholder="表头自定义字段3"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.extText4')"
                class="search-item"
              >
                <span class="search-label">自定义4</span>
                <a-input
                  v-model:value="docQuery.extText4"
                  placeholder="表头自定义字段4"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.extText5')"
                class="search-item"
              >
                <span class="search-label">自定义5</span>
                <a-input
                  v-model:value="docQuery.extText5"
                  placeholder="表头自定义字段5"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
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

            <!-- ── 按明细：17 个查询条件字段（含隐藏） ── -->
            <div
              v-else
              class="search-row"
            >
              <div
                v-if="isQueryVisible('detail.dateRange')"
                class="search-item"
              >
                <span class="search-label">单据时间</span>
                <a-range-picker
                  v-model:value="detailDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 220px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.returnNo')"
                class="search-item"
              >
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="detailQuery.returnNo"
                  placeholder="退货单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.productName')"
                class="search-item"
              >
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="detailQuery.productName"
                  placeholder="商品名称/货号/条码"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.settleUnit')"
                class="search-item"
              >
                <span class="search-label">结算单位</span>
                <a-input
                  v-model:value="detailQuery.settleUnit"
                  placeholder="结算单位"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.customerName')"
                class="search-item"
              >
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="detailQuery.customerName"
                  placeholder="客户名称"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.handlerName')"
                class="search-item"
              >
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="detailQuery.handlerName"
                  placeholder="经手人"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.status')"
                class="search-item"
              >
                <span class="search-label">单据状态</span>
                <a-select
                  v-model:value="detailQuery.status"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.sourceOrder')"
                class="search-item"
              >
                <span class="search-label">来源订单</span>
                <a-input
                  v-model:value="detailQuery.sourceOrder"
                  placeholder="来源订单号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.warehouseName')"
                class="search-item"
              >
                <span class="search-label">仓库</span>
                <a-input
                  v-model:value="detailQuery.warehouseName"
                  placeholder="仓库"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.extNum1')"
                class="search-item"
              >
                <span class="search-label">自定义1</span>
                <a-input
                  v-model:value="detailQuery.extNum1"
                  placeholder="表头自定义字段1"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.extNum2')"
                class="search-item"
              >
                <span class="search-label">自定义2</span>
                <a-input
                  v-model:value="detailQuery.extNum2"
                  placeholder="表头自定义字段2"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.extText3')"
                class="search-item"
              >
                <span class="search-label">自定义3</span>
                <a-input
                  v-model:value="detailQuery.extText3"
                  placeholder="表头自定义字段3"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.extText4')"
                class="search-item"
              >
                <span class="search-label">自定义4</span>
                <a-input
                  v-model:value="detailQuery.extText4"
                  placeholder="表头自定义字段4"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.extText5')"
                class="search-item"
              >
                <span class="search-label">自定义5</span>
                <a-input
                  v-model:value="detailQuery.extText5"
                  placeholder="表头自定义字段5"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.itemRemark')"
                class="search-item"
              >
                <span class="search-label">备注</span>
                <a-input
                  v-model:value="detailQuery.itemRemark"
                  placeholder="明细备注"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.remark')"
                class="search-item"
              >
                <span class="search-label">单据备注</span>
                <a-input
                  v-model:value="detailQuery.remark"
                  placeholder="单据备注"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.auditTime')"
                class="search-item"
              >
                <span class="search-label">审核时间</span>
                <a-date-picker
                  v-model:value="detailAuditTime"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 140px"
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

        <!-- ═══ 数据表（列配置齿轮在表头序号列；逐 Tab 独立 storage-key） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="currentColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
            >
              <template #returnNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record)"
                >{{ record.returnNo || '-' }}</a>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="STATUS_MAP[record.status ?? -1]?.color || 'default'">
                  {{ STATUS_MAP[record.status ?? -1]?.label || '-' }}
                </a-tag>
              </template>
              <template #settleStatusCell="{ record }">
                <a-tag :color="SETTLE_STATUS_MAP[record.settleStatus]?.color || 'default'">
                  {{ SETTLE_STATUS_MAP[record.settleStatus]?.label || record.settleStatus || '-' }}
                </a-tag>
              </template>
              <template #returnTypeCell="{ record }">
                {{ RETURN_TYPE_MAP[record.returnType] || record.returnTypeDesc || record.returnType || '-' }}
              </template>
              <template #totalAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #settledAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #billAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #unitPriceCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #returnAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #qtyCell="{ record, column }">
                {{ fmtQty(record[column.key]) }}
              </template>
              <!-- 「收货」分组子列：订货数量 / 已收数量 / 未收数量（真实列，按单据取主表汇总、按明细取明细行） -->
              <template #orderedQtyCell="{ record }">
                {{ fmtQty(record.orderedQuantity) }}
              </template>
              <template #receivedQtyCell="{ record }">
                {{ fmtQty(record.receivedQuantity) }}
              </template>
              <template #unreceivedQtyCell="{ record }">
                {{ fmtQty(record.unreceivedQuantity) }}
              </template>
              <!-- 只读文本列 -->
              <template #textCell="{ record, column }">
                <span>{{ fmtText(record[column.key]) }}</span>
              </template>
              <!-- 日期列（YYYY-MM-DD HH:mm:ss） -->
              <template #timeCell="{ record, column }">
                <span>{{ fmtDateTime(record[column.key]) }}</span>
              </template>

              <!-- ═══ 行级操作：详情 / 通过 / 拒绝 / 完成（开发文档「行级操作」） ═══ -->
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
                    详情
                  </a-button>
                  <a-popconfirm
                    v-if="record.status === 1"
                    title="确认审批通过该退货申请？"
                    ok-text="通过"
                    cancel-text="取消"
                    @confirm="handleApprove(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      style="color: #52c41a"
                    >
                      通过
                    </a-button>
                  </a-popconfirm>
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    danger
                    @click="openReject(record)"
                  >
                    拒绝
                  </a-button>
                  <a-popconfirm
                    v-if="record.status === 2"
                    title="确认完成该退货申请单？"
                    ok-text="完成"
                    cancel-text="取消"
                    @confirm="handleComplete(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      style="color: #52c41a"
                    >
                      完成
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用），storage-key 逐 Tab 切换 ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 退货详情弹窗（保留既有实现） ═══ -->
      <a-modal
        v-model:open="detailVisible"
        :title="'退货申请详情 - ' + (detailData?.returnNo || '')"
        :footer="null"
        width="900px"
      >
        <a-descriptions
          v-if="detailData"
          bordered
          :column="2"
          size="small"
        >
          <a-descriptions-item label="退货单号">
            {{ detailData.returnNo }}
          </a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <a-tag :color="STATUS_MAP[detailData.status ?? -1]?.color || 'default'">
              {{ STATUS_MAP[detailData.status ?? -1]?.label || '-' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="客户名称">
            {{ detailData.customerName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="仓库">
            {{ detailData.warehouseName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="退货数量">
            {{ fmtQty(detailData.totalQuantity) }}
          </a-descriptions-item>
          <a-descriptions-item label="本单金额">
            {{ fmtMoney(detailData.totalAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="制单人">
            {{ detailData.creatorName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="审核人">
            {{ detailData.auditorName || detailData.auditor || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="提交时间">
            {{ fmtDateTime(detailData.submitTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="审核时间">
            {{ fmtDateTime(detailData.auditTime || detailData.approvedTime) }}
          </a-descriptions-item>
          <a-descriptions-item
            label="退货原因"
            :span="2"
          >
            {{ detailData.reason || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="单据备注"
            :span="2"
          >
            {{ detailData.remark || '-' }}
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>退货明细</a-divider>
        <a-table
          :data-source="detailData?.items || []"
          :columns="itemColumns"
          :pagination="false"
          size="small"
          row-key="id"
          :locale="{ emptyText: '暂无明细' }"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="['unitPrice', 'lineAmount'].includes(column.dataIndex as string)">
              {{ fmtMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'returnQuantity'">
              {{ fmtQty(text) }}
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 拒绝原因弹窗（保留既有实现） ═══ -->
      <a-modal
        v-model:open="rejectVisible"
        title="拒绝退货申请"
        :confirm-loading="rejectLoading"
        @ok="handleRejectConfirm"
      >
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入拒绝原因（必填）"
          :rows="3"
          :maxlength="500"
        />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  ReloadOutlined, DownloadOutlined, PrinterOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { saleReturnApi } from '@/api/erp'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'MallReturnProcess' })

/**
 * 页面行数据 = 后端 SaleReturn（按单据）/ SaleReturnItemPageDTO（按明细）字段。
 * 列 key 与后端返回字段名严格同名，非同名的（表头自定义字段按明细侧）使用 headerExt* 前缀区分。
 */
interface ReturnRow {
  id: number
  rowKey: string
  /** BillDetailTable 空数据占位行行标记（由组件生成，不参与行级操作渲染） */
  __ghost?: boolean
  returnNo?: string
  customerName?: string
  customerCode?: string
  customerLevel?: string
  warehouseName?: string
  handlerName?: string
  deptName?: string
  returnType?: number
  returnTypeDesc?: string
  settleStatus?: string
  status?: number
  totalQuantity?: number
  totalAmount?: number
  settledAmount?: number
  billAmount?: number
  returnQuantityTotal?: number
  reason?: string
  remark?: string
  sourceOrder?: string
  printCount?: number
  submitTime?: string
  orderDate?: string
  auditTime?: string
  approvedTime?: string
  creatorName?: string
  auditorName?: string
  auditor?: string
  createTime?: string
  items?: any[]
  /** 收货进度三列（真实列：主表 ordered_quantity/received_quantity/unreceived_quantity；
   *  按明细行 orderedQuantity 取明细申请退货数量、receivedQuantity 取明细已收数量） */
  orderedQuantity?: number
  receivedQuantity?: number
  unreceivedQuantity?: number
  /** 审核备注（真实来源 = approved_note：审批意见/拒绝原因） */
  approvedNote?: string
  /** 表头自定义字段（按单据取自 SaleReturn；按明细取自 DTO 的 headerExt*） */
  extNum1?: number
  extNum2?: number
  extText3?: string
  extText4?: string
  extText5?: string
  // ── 按明细（SaleReturnItemPageDTO）──
  itemId?: number
  productName?: string
  productCode?: string
  barcode?: string
  specification?: string
  modelNo?: string
  originPlace?: string
  brand?: string
  /** 按明细·表头自定义字段（DTO: headerExtNum1/headerExtNum2/headerExtText3~5） */
  headerExtNum1?: number
  headerExtNum2?: number
  headerExtText3?: string
  headerExtText4?: string
  headerExtText5?: string
  /** 按明细·单据自定义字段（DTO: extNum1~3 / extText1~2，取自明细行 ext_*） */
  extNum3?: number
  extText1?: string
  extText2?: string
  unit?: string
  returnQuantity?: number
  unitPrice?: number
  /** 退货金额（DTO: lineAmount ← 明细 line_amount） */
  lineAmount?: number
  itemRemark?: string
}

// ═══ Tab（对标 2 个视图） ═══
const TABS = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' }
]
const activeTab = ref('doc')

// ═══ 字典（与后端 SaleReturnServiceImpl 状态流转一致：0草稿 1待审批 2已审批 3已完成 4已取消） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'red' }
}
const STATUS_OPTIONS = Object.entries(STATUS_MAP)
  .map(([value, v]) => ({ value: Number(value), label: v.label }))
/** 后端 SaleReturn.settleStatus 为字符串（未结算/已结算），文档口径「结算状态（未结算/已结算）」 */
const SETTLE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  UNSETTLED: { label: '未结算', color: 'orange' },
  SETTLED: { label: '已结算', color: 'green' },
  未结算: { label: '未结算', color: 'orange' },
  已结算: { label: '已结算', color: 'green' }
}
/** 后端 SaleReturn.returnType 为数字枚举，配套字段 returnTypeDesc 为文本描述（以 returnTypeDesc 优先渲染） */
const RETURN_TYPE_MAP: Record<number, string> = {}

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<ReturnRow[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const docQuery = reactive<Record<string, any>>({})
const detailQuery = reactive<Record<string, any>>({})
const docDateRange = ref<[string, string] | undefined>()
const detailDateRange = ref<[string, string] | undefined>()
const docAuditTime = ref<string | undefined>()
const detailAuditTime = ref<string | undefined>()

// ═══ 列定义（★ 严格取自退货申请处理开发文档「全部配置数据列」） ═══
/** 按单据 Tab —— 全部可配置列 26，对标默认显示 20（隐藏 审核时间/表头自定义字段1~5） */
const docColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'orderDate', title: '单据日期', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'returnNo', title: '单据编号', type: 'slot', slotName: 'returnNoCell', width: 170 },
  { key: 'submitTime', title: '提交时间', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'sourceOrder', title: '来源订单', type: 'slot', slotName: 'textCell', width: 170 },
  { key: 'status', title: '单据状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'settleStatus', title: '结算状态', type: 'slot', slotName: 'settleStatusCell', width: 100 },
  { key: 'returnType', title: '退货类型', type: 'slot', slotName: 'returnTypeCell', width: 110 },
  {
    // 「收货」父分组（对标表头两行：分组列不进列配置面板，仅暴露叶子列）
    key: 'receiveGroup',
    title: '收货',
    children: [
      { key: 'orderedQuantity', title: '订货数量', type: 'slot', slotName: 'orderedQtyCell', width: 100, align: 'right' },
      { key: 'receivedQuantity', title: '已收数量', type: 'slot', slotName: 'receivedQtyCell', width: 100, align: 'right' },
      { key: 'unreceivedQuantity', title: '未收数量', type: 'slot', slotName: 'unreceivedQtyCell', width: 100, align: 'right' }
    ]
  },
  { key: 'returnQuantityTotal', title: '退货数量', type: 'slot', slotName: 'qtyCell', width: 100, align: 'right' },
  { key: 'settledAmount', title: '已结金额', type: 'slot', slotName: 'settledAmountCell', width: 110, align: 'right' },
  { key: 'warehouseName', title: '仓库', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'billAmount', title: '本单金额', type: 'slot', slotName: 'billAmountCell', width: 110, align: 'right' },
  { key: 'handlerName', title: '经手人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'reason', title: '退货原因', type: 'slot', slotName: 'textCell', width: 160 },
  { key: 'remark', title: '单据备注', type: 'slot', slotName: 'textCell', width: 150 },
  // 审核备注：真实来源 = approved_note（审批意见 / 拒绝原因），库中无 audit_remark 列
  { key: 'approvedNote', title: '审核备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'printCount', title: '打印次数', type: 'slot', slotName: 'textCell', width: 90, align: 'right' },
  { key: 'auditTime', title: '审核时间', type: 'slot', slotName: 'timeCell', width: 160, defaultHidden: true },
  { key: 'extNum1', title: '表头自定义字段1(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extNum2', title: '表头自定义字段2(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText3', title: '表头自定义字段3(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText4', title: '表头自定义字段4(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText5', title: '表头自定义字段5(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true }
]

/** 按明细 Tab —— 全部可配置列（对标文档口径「40」，默认显示 32）：
 *  隐藏 8 列 = 审核时间 + 表头自定义字段1~5 + 单据自定义字段1~5；
 *  ⚠️ 文档「全部配置数据列（40）」其列名清单实际展开为 44 项（编号 1~40 与清单条目不一致），
 *     本实现严格按**列名清单**逐条落列，并在「审核时间/表头自定义字段/单据自定义字段」上保持默认隐藏。 */
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 200, fixed: 'left' },
  { key: 'orderDate', title: '单据日期', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'returnNo', title: '单据编号', type: 'slot', slotName: 'returnNoCell', width: 170 },
  { key: 'sourceOrder', title: '来源单据', type: 'slot', slotName: 'textCell', width: 170 },
  { key: 'status', title: '单据状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'returnType', title: '退货类型', type: 'slot', slotName: 'returnTypeCell', width: 110 },
  { key: 'settleStatus', title: '结算状态', type: 'slot', slotName: 'settleStatusCell', width: 100 },
  { key: 'warehouseName', title: '仓库', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'textCell', width: 180 },
  { key: 'productCode', title: '货号', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'barcode', title: '条码', type: 'slot', slotName: 'textCell', width: 130 },
  { key: 'specification', title: '规格', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'modelNo', title: '型号', type: 'slot', slotName: 'textCell', width: 110, defaultHidden: true },
  { key: 'originPlace', title: '产地', type: 'slot', slotName: 'textCell', width: 100, defaultHidden: true },
  { key: 'brand', title: '品牌', type: 'slot', slotName: 'textCell', width: 100, defaultHidden: true },
  // 表头自定义字段（DTO: headerExt* ← 单头 r.ext_num1 / r.ext_num2 / r.ext_text3~5）
  { key: 'headerExtNum1', title: '表头自定义字段1(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'headerExtNum2', title: '表头自定义字段2(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'headerExtText3', title: '表头自定义字段3(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'headerExtText4', title: '表头自定义字段4(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'headerExtText5', title: '表头自定义字段5(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  // 单据自定义字段（DTO: extNum1~3 / extText1~2 ← 明细 i.ext_*；与 sales/return-apply 同端点同口径）
  { key: 'extNum1', title: '单据自定义1(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extNum2', title: '单据自定义2(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extNum3', title: '单据自定义3(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText1', title: '单据自定义4(文本字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText2', title: '单据自定义5(文本字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'unit', title: '单位', type: 'slot', slotName: 'textCell', width: 70 },
  {
    // 「收货」父分组：订货数量 / 已收数量 / 未收数量（对标默认显示）
    key: 'receiveGroup',
    title: '收货',
    children: [
      { key: 'orderedQuantity', title: '订货数量', type: 'slot', slotName: 'orderedQtyCell', width: 100, align: 'right' },
      { key: 'receivedQuantity', title: '已收数量', type: 'slot', slotName: 'receivedQtyCell', width: 100, align: 'right' },
      { key: 'unreceivedQuantity', title: '未收数量', type: 'slot', slotName: 'unreceivedQtyCell', width: 100, align: 'right' }
    ]
  },
  { key: 'returnQuantity', title: '退货数量', type: 'slot', slotName: 'qtyCell', width: 100, align: 'right' },
  { key: 'unitPrice', title: '单价', type: 'slot', slotName: 'unitPriceCell', width: 100, align: 'right' },
  // 退货金额 ← DTO lineAmount（明细 line_amount，原列 key returnAmount 无对应字段，恒渲染 '-'）
  { key: 'lineAmount', title: '退货金额', type: 'slot', slotName: 'returnAmountCell', width: 110, align: 'right' },
  { key: 'handlerName', title: '经手人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'reason', title: '退货原因', type: 'slot', slotName: 'textCell', width: 160 },
  { key: 'itemRemark', title: '明细备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'remark', title: '单据备注', type: 'slot', slotName: 'textCell', width: 150 },
  // 审核备注：真实来源 = approved_note（审批意见 / 拒绝原因）
  { key: 'approvedNote', title: '审核备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'submitTime', title: '提交日期', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'createTime', title: '制单日期', type: 'slot', slotName: 'timeCell', width: 160 },
  { key: 'auditTime', title: '审核时间', type: 'slot', slotName: 'timeCell', width: 160, defaultHidden: true }
]

const currentColumns = computed<DetailColumnConfig[]>(() => (
  activeTab.value === 'doc' ? docColumns : detailColumns
))

/** 切 Tab 必须换 storage-key，否则会沿用上一 Tab 的列配置 */
const tableStorageKey = computed(() => `mall-return-process-table-columns-${activeTab.value}`)
const pageConfigStorageKey = computed(() => `mall-return-process-page-config-${activeTab.value}`)

// ═══ 页面配置（逐 Tab 独立一套：查询条件显隐 + 功能按钮启用） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

/** 按单据 Tab：查询条件 16 项（文档「查询条件配置字段（16）」） */
const DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'doc.dateRange', label: '单据时间', visible: true },
  { key: 'doc.returnNo', label: '单据编号', visible: true },
  { key: 'doc.customerName', label: '客户', visible: true },
  { key: 'doc.settleUnit', label: '结算单位', visible: false },
  { key: 'doc.status', label: '单据状态', visible: true },
  { key: 'doc.sourceOrder', label: '来源订单', visible: false },
  { key: 'doc.returnType', label: '退货类型', visible: false },
  { key: 'doc.handlerName', label: '经手人', visible: false },
  { key: 'doc.warehouseName', label: '仓库', visible: false },
  { key: 'doc.remark', label: '单据备注', visible: false },
  { key: 'doc.auditTime', label: '审核时间', visible: false },
  { key: 'doc.extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'doc.extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'doc.extText3', label: '表头自定义字段3(文本)', visible: false },
  { key: 'doc.extText4', label: '表头自定义字段4(文本)', visible: false },
  { key: 'doc.extText5', label: '表头自定义字段5(文本)', visible: false }
]

/** 按明细 Tab：查询条件 17 项（文档「查询条件配置字段（17）」） */
const DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'detail.dateRange', label: '单据时间', visible: true },
  { key: 'detail.returnNo', label: '单据编号', visible: true },
  { key: 'detail.productName', label: '商品', visible: true },
  { key: 'detail.settleUnit', label: '结算单位', visible: false },
  { key: 'detail.customerName', label: '客户', visible: true },
  { key: 'detail.handlerName', label: '经手人', visible: false },
  { key: 'detail.status', label: '单据状态', visible: false },
  { key: 'detail.sourceOrder', label: '来源订单', visible: false },
  { key: 'detail.warehouseName', label: '仓库', visible: false },
  { key: 'detail.extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'detail.extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'detail.extText3', label: '表头自定义字段3(文本)', visible: false },
  { key: 'detail.extText4', label: '表头自定义字段4(文本)', visible: false },
  { key: 'detail.extText5', label: '表头自定义字段5(文本)', visible: false },
  { key: 'detail.itemRemark', label: '备注', visible: false },
  { key: 'detail.remark', label: '单据备注', visible: false },
  { key: 'detail.auditTime', label: '审核时间', visible: false }
]

/** 功能按钮：文档未检出「功能按钮」Tab；按对标按钮栏实测（配置/刷新/打印(F8)/导出）落 3 项可配置按钮 */
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]

const DEFAULT_QUERY_FIELDS = computed<QueryFieldSetting[]>(() => (
  activeTab.value === 'doc' ? DOC_QUERY_FIELDS : DETAIL_QUERY_FIELDS
))
const queryFields = ref<QueryFieldSetting[]>(DOC_QUERY_FIELDS.map(f => ({ ...f })))
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
function fmtMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtQty(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}
function fmtText(val: any): string {
  if (val === null || val === undefined || val === '') return '-'
  return String(val)
}
function fmtDateTime(val?: string | null): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

// ═══ 数据加载 ═══
function extract<T>(res: any, fallback: T): T {
  const payload = res?.data?.data ?? res?.data ?? res
  return (payload === undefined || payload === null ? fallback : payload) as T
}

/** 行键：按明细复用主表 id，必须补明细主键避免 row-key 重复（组件规范第 2 条） */
function normalizeRows(records: any[], tab: string): ReturnRow[] {
  return (records || []).map((r: any, i: number) => ({
    ...r,
    rowKey: tab === 'detail'
      ? String(r.itemId ?? r.id ?? `row-${i}`)
      : String(r.id ?? `row-${i}`)
  }))
}

/** 数字型自定义字段：仅当输入可解析为数字时传参（后端参数为 BigDecimal，避免非法值 400） */
function numParam(v: any): number | undefined {
  if (v === undefined || v === null || v === '') return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}

async function loadTable() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (activeTab.value === 'doc') {
      // ── 按单据：页面渲染的 16 项查询条件逐项传参（未渲染项不传） ──
      if (docDateRange.value?.length === 2) {
        params.startDate = docDateRange.value[0]
        params.endDate = docDateRange.value[1]
      }
      if (docQuery.returnNo) params.returnNo = docQuery.returnNo
      if (docQuery.customerName) params.customerName = docQuery.customerName
      // 结算单位：后端无独立 settle_unit 列，以客户快照承载（同 finance/receipt-doc 口径）
      if (docQuery.settleUnit) params.settleUnit = docQuery.settleUnit
      if (docQuery.status !== undefined && docQuery.status !== null) params.status = docQuery.status
      if (docQuery.sourceOrder) params.sourceOrder = docQuery.sourceOrder
      if (docQuery.handlerName) params.handlerName = docQuery.handlerName
      if (docQuery.warehouseName) params.warehouseName = docQuery.warehouseName
      if (docQuery.remark) params.remark = docQuery.remark
      const docAudit = docAuditTime.value
      if (docAudit) params.auditTime = docAudit
      const docExtNum1 = numParam(docQuery.extNum1)
      if (docExtNum1 !== undefined) params.extNum1 = docExtNum1
      const docExtNum2 = numParam(docQuery.extNum2)
      if (docExtNum2 !== undefined) params.extNum2 = docExtNum2
      if (docQuery.extText3) params.extText3 = docQuery.extText3
      if (docQuery.extText4) params.extText4 = docQuery.extText4
      if (docQuery.extText5) params.extText5 = docQuery.extText5
      // ⚠️ 未闭环：退货类型（returnType）页面为自由文本输入，后端为 Integer 枚举且全库无字典表，
      //    直接传参会 400；待做字典化/下拉后再接线（见开发文档「剩余缺口」）。
      const res: any = await saleReturnApi.page(params)
      const payload = extract<{ records?: any[]; total?: number }>(res, {})
      tableData.value = normalizeRows(payload.records || [], 'doc')
      pagination.total = Number(payload.total) || 0
    } else {
      // ── 按明细：页面渲染的 17 项查询条件逐项传参 ──
      if (detailDateRange.value?.length === 2) {
        params.startDate = detailDateRange.value[0]
        params.endDate = detailDateRange.value[1]
      }
      if (detailQuery.returnNo) params.returnNo = detailQuery.returnNo
      if (detailQuery.productName) params.productName = detailQuery.productName
      if (detailQuery.settleUnit) params.settleUnit = detailQuery.settleUnit
      if (detailQuery.customerName) params.customerName = detailQuery.customerName
      if (detailQuery.handlerName) params.handlerName = detailQuery.handlerName
      if (detailQuery.status !== undefined && detailQuery.status !== null) params.status = detailQuery.status
      if (detailQuery.sourceOrder) params.sourceOrder = detailQuery.sourceOrder
      if (detailQuery.warehouseName) params.warehouseName = detailQuery.warehouseName
      const detailExtNum1 = numParam(detailQuery.extNum1)
      if (detailExtNum1 !== undefined) params.extNum1 = detailExtNum1
      const detailExtNum2 = numParam(detailQuery.extNum2)
      if (detailExtNum2 !== undefined) params.extNum2 = detailExtNum2
      if (detailQuery.extText3) params.extText3 = detailQuery.extText3
      if (detailQuery.extText4) params.extText4 = detailQuery.extText4
      if (detailQuery.extText5) params.extText5 = detailQuery.extText5
      if (detailQuery.itemRemark) params.itemRemark = detailQuery.itemRemark
      if (detailQuery.remark) params.remark = detailQuery.remark
      const detailAudit = detailAuditTime.value
      if (detailAudit) params.auditTime = detailAudit
      // 按明细：走后端 page-detail（明细行 + 单头字段，返回强类型 SaleReturnItemPageDTO）
      const res: any = await saleReturnApi.pageDetail(params)
      const payload = extract<{ records?: any[]; total?: number }>(res, {})
      tableData.value = normalizeRows(payload.records || [], 'detail')
      pagination.total = Number(payload.total) || 0
    }
  } catch (error: any) {
    tableData.value = []
    pagination.total = 0
    console.error('[退货申请处理] 列表加载失败', error)
    message.error(error?.response?.data?.message || '列表加载失败')
  } finally {
    loading.value = false
  }
}

// ═══ 详情 / 审批 / 完成（保留既有实现） ═══
const detailVisible = ref(false)
const detailData = ref<ReturnRow | null>(null)

const itemColumns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 110 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', ellipsis: true },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '退货数量', dataIndex: 'returnQuantity', key: 'returnQuantity', width: 90, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'lineAmount', key: 'lineAmount', width: 110, align: 'right' },
  { title: '退货原因', dataIndex: 'reason', key: 'reason', width: 120, ellipsis: true }
]

async function openDetail(record: ReturnRow) {
  try {
    const res: any = await saleReturnApi.getById(record.id)
    const data: any = res?.data ?? res
    if (!data?.items) {
      try {
        const items: any = await saleReturnApi.getItems(record.id)
        data.items = items?.data ?? items ?? []
      } catch {
        data.items = []
      }
    }
    detailData.value = data
    detailVisible.value = true
  } catch (error) {
    console.warn('[退货申请处理] 详情获取失败', error)
  }
}

async function handleApprove(record: ReturnRow) {
  try {
    await saleReturnApi.approve(record.id)
    message.success(`退货单 ${record.returnNo} 已审批通过`)
    loadTable()
  } catch (error) {
    console.warn('[退货申请处理] 审批通过失败', error)
  }
}

async function handleComplete(record: ReturnRow) {
  try {
    await saleReturnApi.complete(record.id)
    message.success(`退货单 ${record.returnNo} 已完成`)
    loadTable()
  } catch (error) {
    console.warn('[退货申请处理] 完成失败', error)
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<ReturnRow | null>(null)

function openReject(record: ReturnRow) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入拒绝原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await saleReturnApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`退货单 ${rejectTarget.value.returnNo} 已拒绝并退回草稿`)
    rejectVisible.value = false
    loadTable()
  } catch (error) {
    console.warn('[退货申请处理] 拒绝失败', error)
  } finally {
    rejectLoading.value = false
  }
}

// ═══ 查询 / 分页 / Tab ═══
function handleRefresh() {
  loadTable()
}
function handleSearch() {
  pagination.current = 1
  loadTable()
}
function handleReset() {
  if (activeTab.value === 'doc') {
    Object.keys(docQuery).forEach(k => delete docQuery[k])
    docDateRange.value = undefined
    docAuditTime.value = undefined
  } else {
    Object.keys(detailQuery).forEach(k => delete detailQuery[k])
    detailDateRange.value = undefined
    detailAuditTime.value = undefined
  }
  pagination.current = 1
  loadTable()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  loadTable()
}
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  tableData.value = []
  // 切 Tab：列配置(storage-key)与页面配置(storage-key)同步切换，查询条件/按钮换成该 Tab 的一套
  queryFields.value = DEFAULT_QUERY_FIELDS.value.map(f => ({ ...f }))
  functionButtons.value = DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f }))
  loadTable()
}

// ═══ 导出（当前 Tab 数据，CSV + BOM） ═══
async function handleExport() {
  exporting.value = true
  try {
    const rows = tableData.value
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const cols = () => currentColumns.value.filter(
      c => c.type !== 'rowNo' && c.type !== 'action' && c.type !== 'checkbox' && c.key !== '__filler__'
    )
    const headers = cols().map(c => c.title)
    const body = rows.map(r => cols().map(c => (r[c.key as keyof ReturnRow] ?? '') as string | number))
    exportCsv(headers, body, `退货申请处理-${activeTab.value === 'doc' ? '按单据' : '按明细'}`)
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8)：window.open + 内联 HTML 打印模板 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function buildPrintHtml(rows: ReturnRow[]): string {
  const isDoc = activeTab.value === 'doc'
  const head = isDoc
    ? ['#', '单据日期', '单据编号', '来源订单', '单据状态', '结算状态', '退货类型', '退货数量', '已结金额', '仓库', '客户', '本单金额', '经手人', '退货原因']
    : ['#', '单据日期', '单据编号', '来源单据', '客户', '商品名称', '货号', '规格', '单位', '退货数量', '单价', '退货金额', '明细备注']
  const body = rows.map((r, i) => {
    const cells = isDoc
      ? [
          fmtDateTime(r.orderDate), r.returnNo, r.sourceOrder,
          STATUS_MAP[r.status ?? -1]?.label || '',
          SETTLE_STATUS_MAP[r.settleStatus || '']?.label || r.settleStatus,
          RETURN_TYPE_MAP[r.returnType ?? -1] || r.returnTypeDesc,
          fmtQty(r.returnQuantityTotal), fmtMoney(r.settledAmount), r.warehouseName,
          r.customerName, fmtMoney(r.billAmount), r.handlerName, r.reason
        ]
      : [
          fmtDateTime(r.orderDate), r.returnNo, r.sourceOrder, r.customerName, r.productName,
          r.productCode, r.specification, r.unit, fmtQty(r.returnQuantity),
          fmtMoney(r.unitPrice), fmtMoney(r.lineAmount), r.itemRemark
        ]
    return `<tr><td>${i + 1}</td>${cells.map(v => `<td>${escapeHtml(v ?? '')}</td>`).join('')}</tr>`
  }).join('')

  return `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>退货申请处理</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>退货申请处理（${isDoc ? '按单据' : '按明细'}）</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>${head.map(h => `<th>${escapeHtml(h)}</th>`).join('')}</tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
}

function handlePrint() {
  const rows = tableData.value.filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const win = window.open('', '_blank', 'width=1100,height=720')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(buildPrintHtml(rows))
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[退货申请处理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadTable()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.cell-link { color: #1890ff; cursor: pointer; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
