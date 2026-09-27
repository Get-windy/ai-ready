<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        订单处理（商城 → 订单处理 → 订单处理，交易模块菜单 60015）
        · 对标：ql361「商城 → 订单处理 → 订单处理」；本页为商城订单**统一处理工作台**（列表复合页，无独立表单）
        · 骨架（金标准路线 A）：ErrorBoundary > PageContainer(full-height) > CategoryListLayout + BillDetailTable + StandardPagination
        · 2 个视图 Tab（按单据 / 按明细），逐 Tab 独立一套：列定义 + 查询条件 + 功能按钮 + storage-key
        · 列口径严格取自 docs/Yh-Spec/手动整理对标开发文档/交易模块/订单处理开发文档.md
          - 按单据：全部可配置列 39，对标默认显示 32；「发货」为父分组（已发数量/未发数量/商品数量）
          - 按明细：全部可配置列 47，对标默认显示 35；「发货」为父分组（已发数量/未发数量）
          - ⚠️ 文档自相矛盾处（已按“列名清单”为准并注释）：按单据列名清单展开后为 35 个叶子列
            （39 计数 = 「发货」父分组按 1 列计 + 3 个子列另计），本页按“清单/默认显示列名”逐条落列。
        · API（已全部接入，无占位）：
          - 按单据 Tab：mallAdminOrderApi.page（GET /erp/mall/admin/order/page）
          - 按明细 Tab：mallAdminOrderApi.pageDetail（GET /erp/mall/admin/order/page-detail，
            数据源 erp_sale_order_item JOIN erp_sale_order，返回 { records, total }，行键取明细主键 itemId）
          - 行级：mallAdminOrderApi.detail/approve/reject、mallOrderApi.receive（POST /{id}/receive 收款）、
            mallOrderApi.terminate（POST /{id}/terminate 强制终止）
          - 批量：mallOrderApi.batchApprove / batchShip（PUT /order/batch-approve、/order/batch-ship，body 为裸数组 ids）
        · 查询条件透传（本轮已闭环）：orderNo / startDate·endDate（单据日期范围，截止日含当天）/
          consignee / paymentMethod / orderSource / productName（按 erp_sale_order_item 反查单据）/
          status（erp_sale_order.status 数字，多选逗号分隔）；「客户」走 keyword。
          后端 /order/page 与 /order/page-detail 均按同一套可选参数接收，为空即不过滤。
        · 状态口径（本轮统一）：以 erp_sale_order.status 权威口径为准 ——
          0草稿 / 1待审批 / 2已审批 / 3部分出库 / 4完成 / 5交易完成 / 6已取消；
          列表展示优先读 extInfo.originalMallStatus（商城侧细分状态），缺失时按 status 兜底。
        · 展示列中后端 ErpSaleOrderMall 未返回的字段（见 OrderRow 内注释）保持 '-'（不做假数据）
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：交易汇总口径（/erp/mall/admin/trade-analysis） ═══ -->
        <template #toolbar-left>
          <span class="toolbar-tip">
            商城订单合计 {{ tradeSummary.totalOrderCount ?? 0 }} 单 · GMV {{ fmtMoney(tradeSummary.totalGmv) }}
          </span>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 批量审核 / 批量发货 / 刷新 / 批量打印 / 打印(F8) / 导出 ═══ -->
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
              size="small"
              :disabled="checkedRecords.length === 0"
              :loading="batchApproving"
              @click="handleBatchApprove"
            >
              批量审核
            </a-button>
            <a-button
              size="small"
              :disabled="checkedRecords.length === 0"
              :loading="batchShipping"
              @click="handleBatchShip"
            >
              批量发货
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchPrint')"
              size="small"
              :disabled="checkedRecords.length === 0"
              @click="handleBatchPrint"
            >
              <PrinterOutlined /> 批量打印
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
            <!-- ── 按单据：24 个查询条件字段（含隐藏） ── -->
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
                v-if="isQueryVisible('doc.orderNo')"
                class="search-item"
              >
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="docQuery.orderNo"
                  placeholder="XSDD-"
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
                v-if="isQueryVisible('doc.promoterName')"
                class="search-item"
              >
                <span class="search-label">推广人</span>
                <a-input
                  v-model:value="docQuery.promoterName"
                  placeholder="推广人"
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
                v-if="isQueryVisible('doc.productName')"
                class="search-item"
              >
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="docQuery.productName"
                  placeholder="商品名称/货号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.orderSource')"
                class="search-item"
              >
                <span class="search-label">订单来源</span>
                <a-select
                  v-model:value="docQuery.orderSource"
                  placeholder="全部"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="ORDER_SOURCE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.status')"
                class="search-item"
              >
                <span class="search-label">单据状态</span>
                <a-select
                  v-model:value="docQuery.status"
                  mode="multiple"
                  placeholder="全部状态"
                  size="small"
                  style="min-width: 180px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  :max-tag-count="2"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.consignee')"
                class="search-item"
              >
                <span class="search-label">收货人</span>
                <a-input
                  v-model:value="docQuery.consignee"
                  placeholder="收货人"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.paymentMethod')"
                class="search-item"
              >
                <span class="search-label">支付方式</span>
                <a-select
                  v-model:value="docQuery.paymentMethod"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PAYMENT_METHOD_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.paymentStatus')"
                class="search-item"
              >
                <span class="search-label">支付状态</span>
                <a-select
                  v-model:value="docQuery.paymentStatus"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PAYMENT_STATUS_OPTIONS"
                  @change="handleSearch"
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
                v-if="isQueryVisible('doc.deliveryMethod')"
                class="search-item"
              >
                <span class="search-label">配送方式</span>
                <a-input
                  v-model:value="docQuery.deliveryMethod"
                  placeholder="配送方式"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.shipDateStart')"
                class="search-item"
              >
                <span class="search-label">发货日期(起)</span>
                <a-date-picker
                  v-model:value="docShipDateStart"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 140px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.shipDateEnd')"
                class="search-item"
              >
                <span class="search-label">发货日期(止)</span>
                <a-date-picker
                  v-model:value="docShipDateEnd"
                  size="small"
                  value-format="YYYY-MM-DD"
                  style="width: 140px"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.sellerRemark')"
                class="search-item"
              >
                <span class="search-label">卖家备注</span>
                <a-input
                  v-model:value="docQuery.sellerRemark"
                  placeholder="卖家备注"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.pickupAddress')"
                class="search-item"
              >
                <span class="search-label">提货地址</span>
                <a-input
                  v-model:value="docQuery.pickupAddress"
                  placeholder="提货地址"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('doc.promotionOrder')"
                class="search-item"
              >
                <span class="search-label">优惠订单</span>
                <a-select
                  v-model:value="docQuery.promotionOrder"
                  placeholder="全部订单"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PROMOTION_ORDER_OPTIONS"
                  @change="handleSearch"
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

            <!-- ── 按明细：23 个查询条件字段（含隐藏） ── -->
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
                v-if="isQueryVisible('detail.orderNo')"
                class="search-item"
              >
                <span class="search-label">单据编号</span>
                <a-input
                  v-model:value="detailQuery.orderNo"
                  placeholder="XSDD-"
                  size="small"
                  style="width: 150px"
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
                v-if="isQueryVisible('detail.deptName')"
                class="search-item"
              >
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="detailQuery.deptName"
                  placeholder="部门"
                  size="small"
                  style="width: 120px"
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
                v-if="isQueryVisible('detail.productName')"
                class="search-item"
              >
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="detailQuery.productName"
                  placeholder="商品名称/货号/条码"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.consignee')"
                class="search-item"
              >
                <span class="search-label">收货人</span>
                <a-input
                  v-model:value="detailQuery.consignee"
                  placeholder="收货人"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.paymentMethod')"
                class="search-item"
              >
                <span class="search-label">支付方式</span>
                <a-select
                  v-model:value="detailQuery.paymentMethod"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PAYMENT_METHOD_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.paymentStatus')"
                class="search-item"
              >
                <span class="search-label">支付状态</span>
                <a-select
                  v-model:value="detailQuery.paymentStatus"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="PAYMENT_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.status')"
                class="search-item"
              >
                <span class="search-label">单据状态</span>
                <a-select
                  v-model:value="detailQuery.status"
                  mode="multiple"
                  placeholder="全部状态"
                  size="small"
                  style="min-width: 180px"
                  allow-clear
                  :options="STATUS_OPTIONS"
                  :max-tag-count="2"
                  @change="handleSearch"
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
                v-if="isQueryVisible('detail.customerRemark')"
                class="search-item"
              >
                <span class="search-label">客户备注</span>
                <a-input
                  v-model:value="detailQuery.customerRemark"
                  placeholder="客户（买家）备注"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.sellerRemark')"
                class="search-item"
              >
                <span class="search-label">卖家备注</span>
                <a-input
                  v-model:value="detailQuery.sellerRemark"
                  placeholder="卖家备注"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.deliveryMethod')"
                class="search-item"
              >
                <span class="search-label">配送方式</span>
                <a-input
                  v-model:value="detailQuery.deliveryMethod"
                  placeholder="配送方式"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.activityProduct')"
                class="search-item"
              >
                <span class="search-label">活动商品</span>
                <a-select
                  v-model:value="detailQuery.activityProduct"
                  placeholder="全部"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="ACTIVITY_PRODUCT_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('detail.pickupAddress')"
                class="search-item"
              >
                <span class="search-label">提货地址</span>
                <a-input
                  v-model:value="detailQuery.pickupAddress"
                  placeholder="提货地址"
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
              ref="tableRef"
              v-model:data-source="tableData"
              :columns="currentColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="tableStorageKey"
              :global-config-key="tableStorageKey"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <template #orderNoCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openDetail(record)"
                >{{ record.orderNo || '-' }}</a>
              </template>
              <template #totalAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #otherFeeCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #freightCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #codAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #settledAmountCell="{ record, column }">
                {{ fmtMoney(record[column.key]) }}
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="MALL_STATUS_MAP[mallStatusOf(record)]?.color || 'default'">
                  {{ MALL_STATUS_MAP[mallStatusOf(record)]?.label || mallStatusOf(record) || '-' }}
                </a-tag>
              </template>
              <template #paymentStatusCell="{ record }">
                <a-tag
                  v-if="record.paymentStatus !== null && record.paymentStatus !== undefined"
                  :color="PAYMENT_STATUS_MAP[record.paymentStatus]?.color || 'default'"
                >
                  {{ PAYMENT_STATUS_MAP[record.paymentStatus]?.label || '-' }}
                </a-tag>
                <span v-else>-</span>
              </template>
              <template #paymentMethodCell="{ record }">
                {{ PAYMENT_METHOD_MAP[record.paymentMethod] || record.paymentMethod || '-' }}
              </template>
              <template #orderSourceCell="{ record }">
                {{ ORDER_SOURCE_MAP[record.orderSource] || '-' }}
              </template>
              <template #couponUsedCell="{ record }">
                {{ record.couponUsed ? '是' : '否' }}
              </template>
              <template #forceStopCell="{ record }">
                {{ record.forceStop ? '是' : '否' }}
              </template>
              <!-- 「发货」分组子列：已发数量 / 未发数量 / 商品数量 -->
              <template #shippedQtyCell="{ record }">
                {{ fmtQty(record.shippedQuantity) }}
              </template>
              <template #unshippedQtyCell="{ record }">
                {{ fmtQty(record.unshippedQuantity) }}
              </template>
              <template #productQtyCell="{ record }">
                {{ fmtQty(record.productQuantity) }}
              </template>
              <!-- 只读文本列：统一走 slot，避免列 key 与后端字段不一致时静默渲染空白 -->
              <template #textCell="{ record, column }">
                <span>{{ fmtText(record[column.key]) }}</span>
              </template>

              <!-- ═══ 行级操作：审核 / 收款 / 终止 / 更多（@ 开发文档「固定操作列」） ═══ -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    :disabled="!canApprove(record)"
                    @click="handleApprove(record)"
                  >
                    审核
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="!canReceive(record)"
                    :loading="receivingId === record.id"
                    @click="handleReceive(record)"
                  >
                    收款
                  </a-button>
                  <a-popconfirm
                    title="确认强制终止该订单？终止后订单状态置为已取消。"
                    ok-text="终止"
                    cancel-text="取消"
                    @confirm="handleTerminate(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                      :disabled="!canTerminate(record)"
                      :loading="terminatingId === record.id"
                    >
                      终止
                    </a-button>
                  </a-popconfirm>
                  <a-dropdown :trigger="['click']">
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item
                          key="detail"
                          @click="openDetail(record)"
                        >
                          详情
                        </a-menu-item>
                        <a-menu-item
                          key="reject"
                          :disabled="!canReject(record)"
                          @click="openReject(record)"
                        >
                          审核驳回
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
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

      <!-- ═══ 订单详情弹窗 ═══ -->
      <a-modal
        v-model:open="detailVisible"
        :title="'订单详情 - ' + (detailData?.orderNo || '')"
        :footer="null"
        width="760px"
      >
        <a-descriptions
          v-if="detailData"
          bordered
          :column="2"
          size="small"
        >
          <a-descriptions-item
            label="订单编号"
            :span="2"
          >
            {{ detailData.orderNo }}
          </a-descriptions-item>
          <a-descriptions-item label="客户名称">
            {{ detailData.customerName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="订单来源">
            {{ ORDER_SOURCE_MAP[detailData.orderSource ?? -1] || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="单据状态">
            <a-tag :color="MALL_STATUS_MAP[mallStatusOf(detailData)]?.color || 'default'">
              {{ MALL_STATUS_MAP[mallStatusOf(detailData)]?.label || '-' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="支付状态">
            {{ PAYMENT_STATUS_MAP[detailData.paymentStatus ?? -1]?.label || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="订单金额">
            {{ fmtMoney(detailData.totalAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已结金额">
            {{ fmtMoney(detailData.receivedAmount) }}
          </a-descriptions-item>
          <a-descriptions-item label="支付方式">
            {{ PAYMENT_METHOD_MAP[detailData.paymentMethod || ''] || detailData.paymentMethod || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="提交时间">
            {{ fmtDateTime(detailData.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="收货人">
            {{ detailData.consignee || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="联系电话">
            {{ detailData.consigneePhone || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="收货地址"
            :span="2"
          >
            {{ detailData.consigneeAddress || detailData.shippingAddress || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="客户备注"
            :span="2"
          >
            {{ detailData.buyerRemark || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="卖家备注"
            :span="2"
          >
            {{ detailData.orderRemark || detailData.remark || '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-modal>

      <!-- ═══ 审核驳回原因弹窗 ═══ -->
      <a-modal
        v-model:open="rejectVisible"
        title="审核驳回"
        :confirm-loading="rejectLoading"
        @ok="handleRejectConfirm"
      >
        <a-textarea
          v-model:value="rejectReason"
          placeholder="请输入驳回原因（必填）"
          :rows="3"
          :maxlength="500"
        />
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="mall-order-process"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
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
import { mallAdminOrderApi, mallOrderApi, mallTradeApi, type MallOrderAdmin } from '@/api/erp/mall'
import { exportCsv } from '@/utils/exportCsv'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MallOrderProcess' })

/**
 * 页面行数据 = 后端 ErpSaleOrderMall 字段 + 对标列所需扩展字段。
 * 后端 ErpSaleOrderMall 未返回的字段对应列渲染为 '-'（不做假数据）；此处仅标注字段来源，与接口是否已补齐无关。
 */
interface OrderRow {
  id: number
  rowKey: string
  /** BillDetailTable 空数据占位行标记（由组件生成，不参与行级操作渲染） */
  __ghost?: boolean
  orderNo?: string
  customerId?: number
  customerName?: string
  orderDate?: string
  status?: number
  orderSource?: number
  totalAmount?: number
  receivedAmount?: number
  paymentMethod?: string
  paymentStatus?: number
  deliveryStatus?: number
  consignee?: string
  consigneePhone?: string
  consigneeAddress?: string
  shippingAddress?: string
  orderRemark?: string
  buyerRemark?: string
  remark?: string
  extInfo?: string
  createTime?: string
  updateTime?: string
  // ── 对标列所需扩展字段（来源：ErpSaleOrderMall 单头分页 / MallOrderItemPageDTO 按明细分页） ──
  otherFee?: number                 // otherFee（其他费用）
  deliveryMethod?: string           // deliveryMethod（配送方式）
  logisticsCompany?: string         // logisticsCompany（物流公司）
  trackingNo?: string               // trackingNo（运单号）
  freight?: number                  // freight（运费）
  freightPayer?: string             // freightPayer（运费承担方）
  codAmount?: number                // codAmount（代收金额）
  expectedShipTime?: string         // expectedShipTime（预计发货）
  shippedQuantity?: number          // shippedQuantity（发货\已发数量）
  unshippedQuantity?: number        // unshippedQuantity（发货\未发数量）
  productQuantity?: number          // productQuantity（发货\商品数量）
  warehouseName?: string            // warehouseName（仓库）
  forceStop?: boolean               // forceStop（强制终止）
  promoterName?: string             // promoterName（推广人）
  handlerName?: string              // handlerName（经手人）
  printCount?: number               // printCount（打印次数）
  bookkeepingStatus?: string        // bookkeepingStatus（记账状态）
  settledAmount?: number            // settledAmount（已结金额）
  couponUsed?: boolean              // couponUsed（是否使用优惠券）
  auditTime?: string                // auditTime（审核时间）
  extNum1?: number
  extNum2?: number
  extText3?: string
  extText4?: string
  extText5?: string
  // ── 按明细列所需（数据源 GET /erp/mall/admin/order/page-detail → MallOrderItemPageDTO） ──
  itemId?: number
  /**
   * 部门名称（列 key = departmentName，对齐后端 MallOrderItemPageDTO.departmentName；
   * 来源列 erp_sale_order.dept_name，V9.49.0 迁移已建，非新造列）。
   */
  departmentName?: string
  productName?: string
  productCode?: string
  barcode?: string
  smallUnitBarcode?: string
  specification?: string
  modelNo?: string
  originPlace?: string
  brand?: string
  docExtNum1?: number
  docExtNum2?: number
  docExtNum3?: number
  docExtText4?: string
  docExtText5?: string
  batchBarcode?: string
  productionDate?: string
  expiryDate?: string
  unit?: string
  saleQuantity?: number
  smallUnitQuantity?: number
  unitPrice?: number
  smallUnitPrice?: number
  amount?: number
  shippedItemQty?: number
  unshippedItemQty?: number
  creatorName?: string
  itemRemark?: string
  customerRemark?: string
  sellerRemark?: string
  pickupAddress?: string
  activityProduct?: string
}

// ═══ Tab（对标 2 个视图） ═══
const TABS = [
  { key: 'doc', label: '按单据' },
  { key: 'detail', label: '按明细' }
]
const activeTab = ref('doc')

// ═══ 字典（以后端 erp_sale_order.status 权威口径为唯一基准） ═══
/**
 * erp_sale_order.status 权威口径（以 `ErpSaleOrderMall.status` / `SaleOrder.status` 注释为准）：
 * 0草稿（未付款）/ 1待审批（已付款待审）/ 2已审批（待发货）/ 3部分出库（已发货）/
 * 4完成 / 5交易完成 / 6已取消。
 *
 * 「单据状态」下拉直接提交 erp 数字状态（多选逗号分隔），与商城订单页口径一致；
 * 列表展示优先读 extInfo.originalMallStatus（商城侧细分状态），缺失时按下方 status 兜底。
 */
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'orange' },
  1: { label: '待审批', color: 'gold' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '部分出库', color: 'cyan' },
  4: { label: '完成', color: 'green' },
  5: { label: '交易完成', color: 'green' },
  6: { label: '已取消', color: 'default' }
}
/**
 * 单据状态多选选项（对标 ql361「审核中/待付款/待发货/发货完成/交易完成/审核驳回」6 项）。
 *
 * ql361 的「审核中」与「待付款」在 erp_sale_order.status 里都落在 **1（待审批）**：
 * 商城侧用 extInfo.originalMallStatus 区分 PENDING_AUDIT / PAID，erp 单列状态无法区分，
 * 故此处按 erp 权威口径只保留 1 项「待审批（待付款/审核中）」；「审核驳回」与「强制终止」
 * 在 erp 侧同为 **6（已取消）**，同样合并为一项。
 */
const STATUS_OPTIONS = [0, 1, 2, 3, 5, 6, 4].map(value => ({
  value,
  label: value === 1 ? '待审批（待付款/审核中）'
    : value === 2 ? '已审批（待发货）'
      : value === 3 ? '部分出库（已发货）'
        : value === 6 ? '已取消（审核驳回/强制终止）'
          : STATUS_MAP[value].label
}))
/** 商城侧细分状态（extInfo.originalMallStatus）→ 展示字典；erp status 无法区分的细分状态靠此还原 */
const MALL_STATUS_MAP: Record<string, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: '待付款', color: 'gold' },
  PENDING_AUDIT: { label: '审核中', color: 'orange' },
  PAID: { label: '待审批', color: 'blue' },
  APPROVED: { label: '待发货', color: 'cyan' },
  SHIPPED: { label: '发货完成', color: 'geekblue' },
  COMPLETED: { label: '交易完成', color: 'green' },
  REJECTED: { label: '审核驳回', color: 'red' },
  CANCELLED: { label: '已取消', color: 'default' }
}
const PAYMENT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待支付', color: 'orange' },
  1: { label: '支付中', color: 'blue' },
  2: { label: '已支付', color: 'green' },
  3: { label: '部分支付', color: 'gold' },
  4: { label: '已退款', color: 'red' }
}
const PAYMENT_STATUS_OPTIONS = Object.entries(PAYMENT_STATUS_MAP)
  .map(([value, v]) => ({ value: Number(value), label: v.label }))
/** 后端 MallOrder.paymentMethod: ALIPAY / WECHAT / UNIONPAY / BANK / CASH */
const PAYMENT_METHOD_MAP: Record<string, string> = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  UNIONPAY: '银联',
  BANK: '银行转账',
  CASH: '现金'
}
const PAYMENT_METHOD_OPTIONS = Object.entries(PAYMENT_METHOD_MAP)
  .map(([value, label]) => ({ value, label }))
/** 后端 orderSource: 2=企业客户商城 3=个人会员商城 */
const ORDER_SOURCE_MAP: Record<number, string> = {
  2: '企业客户商城',
  3: '个人会员商城'
}
const ORDER_SOURCE_OPTIONS = Object.entries(ORDER_SOURCE_MAP)
  .map(([value, label]) => ({ value: Number(value), label }))
const PROMOTION_ORDER_OPTIONS = [
  { value: 'all', label: '全部订单' },
  { value: 'yes', label: '仅优惠订单' },
  { value: 'no', label: '非优惠订单' }
]
const ACTIVITY_PRODUCT_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'yes', label: '仅活动商品' },
  { value: 'no', label: '非活动商品' }
]

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<OrderRow[]>([])
const checkedRecords = ref<OrderRow[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const docQuery = reactive<Record<string, any>>({})
const detailQuery = reactive<Record<string, any>>({})
const docDateRange = ref<[string, string] | undefined>()
const detailDateRange = ref<[string, string] | undefined>()
const docShipDateStart = ref<string | undefined>()
const docShipDateEnd = ref<string | undefined>()
const docAuditTime = ref<string | undefined>()
const detailAuditTime = ref<string | undefined>()

function handleCheckboxChange(_record: OrderRow, _index: number, _checked: boolean) {
  // BillDetailTable 勾选态由组件内部维护，这里同步「已选行」列表供批量打印使用
  checkedRecords.value = tableRef.value?.getCheckedRecords?.() || []
}
function handleCheckboxAll(_checked: boolean, records: OrderRow[]) {
  checkedRecords.value = records || []
}

// ═══ 列定义（★ 严格取自订单处理开发文档「全部配置数据列」；defaultHidden=true 即文档的隐藏列） ═══
/**
 * 按单据 Tab —— 全部可配置列 39（文档口径），对标默认显示 32。
 * ⚠️ 文档口径说明：文档「39」把「发货」父分组按 1 列计、其 3 个子列另计；
 *    本实现按文档的**列名清单**逐条落列（35 个叶子列），默认显示 32（隐藏 是否采购/审核时间/表头自定义1~5 共 7 列）。
 */
const docColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  // 勾选列（key 必须为 checkbox —— BillDetailTable LOCKED_COLUMNS 锁定列口径）
  // ⚠️ 必须紧挨序号列：操作列在 BillDetailTable 内被强制 width:auto（实际宽度≠配置宽度），
  //    勾选列若排在操作列之后，会被钉到错误的 left 偏移上（表现为表格中间浮着一个错位的选中列）。
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'orderNo', title: '单据编号', type: 'slot', slotName: 'orderNoCell', width: 170 },
  { key: 'totalAmount', title: '订单金额', type: 'slot', slotName: 'totalAmountCell', width: 110, align: 'right' },
  { key: 'otherFee', title: '其他费用', type: 'slot', slotName: 'otherFeeCell', width: 100, align: 'right' },
  { key: 'deliveryMethod', title: '配送方式', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'logisticsCompany', title: '物流公司', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'trackingNo', title: '运单号', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'freight', title: '运费', type: 'slot', slotName: 'freightCell', width: 90, align: 'right' },
  { key: 'freightPayer', title: '运费承担方', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'codAmount', title: '代收金额', type: 'slot', slotName: 'codAmountCell', width: 100, align: 'right' },
  { key: 'consignee', title: '收货人', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'consigneePhone', title: '联系电话', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'consigneeAddress', title: '收货地址', type: 'slot', slotName: 'textCell', width: 220 },
  { key: 'buyerRemark', title: '客户备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'orderRemark', title: '卖家备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'status', title: '单据状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'expectedShipTime', title: '预计发货', type: 'slot', slotName: 'textCell', width: 150 },
  {
    // 「发货」父分组（对标表头两行：分组列不进列配置面板，仅暴露叶子列）
    key: 'shipGroup',
    title: '发货',
    children: [
      { key: 'shippedQuantity', title: '已发数量', type: 'slot', slotName: 'shippedQtyCell', width: 100, align: 'right' },
      { key: 'unshippedQuantity', title: '未发数量', type: 'slot', slotName: 'unshippedQtyCell', width: 100, align: 'right' },
      { key: 'productQuantity', title: '商品数量', type: 'slot', slotName: 'productQtyCell', width: 100, align: 'right' }
    ]
  },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'warehouseName', title: '仓库', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'createTime', title: '提交时间', type: 'slot', slotName: 'textCell', width: 160 },
  { key: 'orderSource', title: '订单来源', type: 'slot', slotName: 'orderSourceCell', width: 120 },
  { key: 'forceStop', title: '强制终止', type: 'slot', slotName: 'forceStopCell', width: 90, align: 'center' },
  { key: 'promoterName', title: '推广人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'handlerName', title: '经手人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'orderDate', title: '单据日期', type: 'slot', slotName: 'textCell', width: 160 },
  { key: 'printCount', title: '打印次数', type: 'slot', slotName: 'textCell', width: 90, align: 'right' },
  { key: 'paymentMethod', title: '支付方式', type: 'slot', slotName: 'paymentMethodCell', width: 110 },
  { key: 'bookkeepingStatus', title: '记账状态', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'settledAmount', title: '已结金额', type: 'slot', slotName: 'settledAmountCell', width: 110, align: 'right' },
  { key: 'couponUsed', title: '是否使用优惠券', type: 'slot', slotName: 'couponUsedCell', width: 130, align: 'center' },
  { key: 'isPurchase', title: '是否采购', type: 'slot', slotName: 'textCell', width: 100, align: 'center', defaultHidden: true },
  { key: 'auditTime', title: '审核时间', type: 'slot', slotName: 'textCell', width: 160, defaultHidden: true },
  { key: 'extNum1', title: '表头自定义字段1(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extNum2', title: '表头自定义字段2(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText3', title: '表头自定义字段3(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText4', title: '表头自定义字段4(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText5', title: '表头自定义字段5(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true }
]

/**
 * 按明细 Tab —— 全部可配置列 47（文档口径），对标默认显示 35。
 * ⚠️ 按明细 Tab 数据源 = GET /erp/mall/admin/order/page-detail（详情见 loadTable 的 detail 分支）。
 */
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'orderDate', title: '单据日期', type: 'slot', slotName: 'textCell', width: 160 },
  { key: 'orderNo', title: '单据编号', type: 'slot', slotName: 'orderNoCell', width: 170 },
  { key: 'status', title: '单据状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'warehouseName', title: '仓库', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'productName', title: '商品名称', type: 'slot', slotName: 'textCell', width: 180 },
  { key: 'productCode', title: '商品货号', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'barcode', title: '条码', type: 'slot', slotName: 'textCell', width: 130 },
  { key: 'smallUnitBarcode', title: '小单位条码', type: 'slot', slotName: 'textCell', width: 130, defaultHidden: true },
  { key: 'specification', title: '规格', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'modelNo', title: '型号', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'originPlace', title: '产地', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'brand', title: '品牌', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'extNum1', title: '表头自定义字段1(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extNum2', title: '表头自定义字段2(数字)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText3', title: '表头自定义字段3(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText4', title: '表头自定义字段4(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'extText5', title: '表头自定义字段5(文本)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'docExtNum1', title: '单据自定义1(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'docExtNum2', title: '单据自定义2(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'docExtNum3', title: '单据自定义3(数字字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'docExtText4', title: '单据自定义4(文本字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'docExtText5', title: '单据自定义5(文本字段)', type: 'slot', slotName: 'textCell', width: 170, defaultHidden: true },
  { key: 'batchBarcode', title: '批次条码', type: 'slot', slotName: 'textCell', width: 130 },
  { key: 'productionDate', title: '生产日期', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'expiryDate', title: '到期日期', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'unit', title: '单位', type: 'slot', slotName: 'textCell', width: 70 },
  { key: 'saleQuantity', title: '销售数量', type: 'slot', slotName: 'textCell', width: 100, align: 'right' },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'slot', slotName: 'textCell', width: 110, align: 'right' },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, align: 'right', precision: 2 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 110, align: 'right', precision: 2 },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'totalAmountCell', width: 110, align: 'right' },
  {
    // 「发货」父分组：已发数量 / 未发数量（对标默认显示；按明细的「发货」分组无「商品数量」子列）
    key: 'shipGroup',
    title: '发货',
    children: [
      { key: 'shippedItemQty', title: '已发数量', type: 'slot', slotName: 'shippedQtyCell', width: 100, align: 'right' },
      { key: 'unshippedItemQty', title: '未发数量', type: 'slot', slotName: 'unshippedQtyCell', width: 100, align: 'right' }
    ]
  },
  { key: 'deliveryMethod', title: '配送方式', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'logisticsCompany', title: '物流公司', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'trackingNo', title: '运单号', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'consignee', title: '收货人', type: 'slot', slotName: 'textCell', width: 100 },
  { key: 'consigneePhone', title: '联系电话', type: 'slot', slotName: 'textCell', width: 120 },
  { key: 'consigneeAddress', title: '收货地址', type: 'slot', slotName: 'textCell', width: 220 },
  { key: 'handlerName', title: '经手人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'departmentName', title: '部门', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'creatorName', title: '制单人', type: 'slot', slotName: 'textCell', width: 110 },
  { key: 'itemRemark', title: '备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'customerRemark', title: '客户备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'sellerRemark', title: '卖家备注', type: 'slot', slotName: 'textCell', width: 150 },
  { key: 'auditTime', title: '审核时间', type: 'slot', slotName: 'textCell', width: 160, defaultHidden: true }
]

const currentColumns = computed<DetailColumnConfig[]>(() => (
  activeTab.value === 'doc' ? docColumns : detailColumns
))

/** 切 Tab 必须换 storage-key，否则会沿用上一 Tab 的列配置 */
const tableStorageKey = computed(() => `mall-order-process-table-columns-${activeTab.value}`)
const pageConfigStorageKey = computed(() => `mall-order-process-page-config-${activeTab.value}`)

// ═══ 页面配置（逐 Tab 独立一套：查询条件显隐 + 功能按钮启用） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

/** 按单据 Tab：查询条件 24 项（文档「查询条件配置字段（24）」） */
const DOC_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'doc.dateRange', label: '单据时间', visible: true },
  { key: 'doc.orderNo', label: '单据编号', visible: true },
  { key: 'doc.customerName', label: '客户', visible: true },
  { key: 'doc.promoterName', label: '推广人', visible: true },
  { key: 'doc.handlerName', label: '经手人', visible: true },
  { key: 'doc.productName', label: '商品', visible: true },
  { key: 'doc.orderSource', label: '订单来源', visible: true },
  { key: 'doc.status', label: '单据状态', visible: true },
  { key: 'doc.consignee', label: '收货人', visible: false },
  { key: 'doc.paymentMethod', label: '支付方式', visible: false },
  { key: 'doc.paymentStatus', label: '支付状态', visible: false },
  { key: 'doc.warehouseName', label: '仓库', visible: false },
  { key: 'doc.deliveryMethod', label: '配送方式', visible: false },
  { key: 'doc.shipDateStart', label: '发货日期(起)', visible: false },
  { key: 'doc.shipDateEnd', label: '发货日期(止)', visible: false },
  { key: 'doc.sellerRemark', label: '卖家备注', visible: false },
  { key: 'doc.pickupAddress', label: '提货地址', visible: false },
  { key: 'doc.promotionOrder', label: '优惠订单', visible: true },
  { key: 'doc.auditTime', label: '审核时间', visible: false },
  { key: 'doc.extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'doc.extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'doc.extText3', label: '表头自定义字段3(文本)', visible: false },
  { key: 'doc.extText4', label: '表头自定义字段4(文本)', visible: false },
  { key: 'doc.extText5', label: '表头自定义字段5(文本)', visible: false }
]

/** 按明细 Tab：查询条件 23 项（文档「查询条件配置字段（23）」） */
const DETAIL_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'detail.dateRange', label: '单据时间', visible: true },
  { key: 'detail.orderNo', label: '单据编号', visible: true },
  { key: 'detail.customerName', label: '客户', visible: true },
  { key: 'detail.handlerName', label: '经手人', visible: true },
  { key: 'detail.deptName', label: '部门', visible: false },
  { key: 'detail.warehouseName', label: '仓库', visible: false },
  { key: 'detail.productName', label: '商品', visible: false },
  { key: 'detail.consignee', label: '收货人', visible: false },
  { key: 'detail.paymentMethod', label: '支付方式', visible: false },
  { key: 'detail.paymentStatus', label: '支付状态', visible: false },
  { key: 'detail.status', label: '单据状态', visible: false },
  { key: 'detail.extNum1', label: '表头自定义字段1(数字)', visible: false },
  { key: 'detail.extNum2', label: '表头自定义字段2(数字)', visible: false },
  { key: 'detail.extText3', label: '表头自定义字段3(文本)', visible: false },
  { key: 'detail.extText4', label: '表头自定义字段4(文本)', visible: false },
  { key: 'detail.extText5', label: '表头自定义字段5(文本)', visible: false },
  { key: 'detail.itemRemark', label: '备注', visible: false },
  { key: 'detail.customerRemark', label: '客户备注', visible: false },
  { key: 'detail.sellerRemark', label: '卖家备注', visible: false },
  { key: 'detail.deliveryMethod', label: '配送方式', visible: false },
  { key: 'detail.activityProduct', label: '活动商品', visible: true },
  { key: 'detail.pickupAddress', label: '提货地址', visible: false },
  { key: 'detail.auditTime', label: '审核时间', visible: false }
]

/** 功能按钮（按单据 4 项；按明细文档未检出「功能按钮」Tab，沿用同口径按钮集） */
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batchPrint', label: '批量打印', enabled: true },
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

/**
 * 还原商城侧细分状态：优先 extInfo.originalMallStatus，缺失时按 erp_sale_order.status 兜底。
 *
 * 兜底字典与后端 `erpStatusToMallStatus` 逐值一致（互逆映射）：
 * 0→PENDING_PAYMENT / 1→PAID / 2→APPROVED / 3→SHIPPED / 4=5→COMPLETED / 6→CANCELLED。
 * 说明：erp 规范里 4（完成）与 5（交易完成）是两个阶段，商城口径都归「交易完成」；
 * 6（已取消）同时覆盖商城侧「审核驳回」与「强制终止」，无 extInfo 时无法区分，故按「已取消」展示。
 */
function mallStatusOf(record: Partial<OrderRow>): string {
  const ext = record.extInfo
  if (ext && ext.includes('originalMallStatus')) {
    const m = ext.match(/"originalMallStatus"\s*:\s*"([^"]+)"/)
    if (m) return m[1]
  }
  const fallback: Record<number, string> = {
    0: 'PENDING_PAYMENT', 1: 'PAID', 2: 'APPROVED', 3: 'SHIPPED', 4: 'COMPLETED', 5: 'COMPLETED', 6: 'CANCELLED'
  }
  return fallback[record.status ?? 0] || 'PENDING_PAYMENT'
}

// ═══ 行级操作可用性（沿用既有业务判断，不新增后端语义） ═══
function canApprove(record: OrderRow): boolean {
  return ['PAID', 'PENDING_AUDIT'].includes(mallStatusOf(record))
}
function canReject(record: OrderRow): boolean {
  return ['PAID', 'PENDING_PAYMENT', 'PENDING_AUDIT'].includes(mallStatusOf(record))
}
function canReceive(record: OrderRow): boolean {
  // 收款：尚未结清（已结金额 < 订单金额）时可用
  if (record.receivedAmount === undefined || record.totalAmount === undefined) return false
  return Number(record.receivedAmount) < Number(record.totalAmount)
}
function canTerminate(record: OrderRow): boolean {
  return !['COMPLETED', 'CANCELLED'].includes(mallStatusOf(record))
}

// ═══ 数据加载 ═══
function extract<T>(res: any, fallback: T): T {
  const payload = res?.data?.data ?? res?.data ?? res
  return (payload === undefined || payload === null ? fallback : payload) as T
}

/** 行键：按单据用主表 id；按明细必须用明细主键 itemId（后端已按明细分页，主表 id 会在多行间复用） */
function normalizeRows(records: any[], tab: string): OrderRow[] {
  return (records || []).map((r: any, i: number) => ({
    ...r,
    rowKey: tab === 'detail'
      ? String(r.itemId ?? `row-${i}`)
      : String(r.id ?? `row-${i}`)
  }))
}

/**
 * 构建查询参数（两个 Tab 共用同一套「已渲染即透传」口径）。
 *
 * 后端 `GET /order/page` 与 `GET /order/page-detail` 已扩参接收：
 * orderNo(模糊) / startDate·endDate(单据日期范围，截止日含当天) / consignee(模糊) /
 * paymentMethod(等值) / orderSource(等值) / productName(商品名称·货号，按明细反查) /
 * status(erp_sale_order.status 数字，可多值逗号分隔)。
 *
 * `keyword` 仅承接「客户」查询项（后端 keyword 覆盖 单据编号/客户名称/收货人 三列，
 * 单据编号与收货人已各有专用参数，避免重复命中）。
 */
function buildQuery(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  const isDoc = activeTab.value === 'doc'
  const query = isDoc ? docQuery : detailQuery
  const range = isDoc ? docDateRange.value : detailDateRange.value

  if (range?.length === 2) {
    params.startDate = range[0]
    params.endDate = range[1]
  }
  if (query.orderNo) params.orderNo = query.orderNo
  if (query.customerName) params.keyword = query.customerName
  if (query.consignee) params.consignee = query.consignee
  if (query.paymentMethod) params.paymentMethod = query.paymentMethod
  if (query.orderSource !== undefined && query.orderSource !== null && query.orderSource !== '') {
    params.orderSource = query.orderSource
  }
  if (query.productName) params.productName = query.productName
  // 单据状态：多选直接提交 erp 数字状态（逗号分隔），后端按 orderStatus/status 双口径兼容
  if (Array.isArray(query.status) && query.status.length) params.status = query.status.join(',')
  return params
}

async function loadTable() {
  loading.value = true
  try {
    const params = buildQuery()
    const res: any = activeTab.value === 'doc'
      ? await mallAdminOrderApi.page(params)
      : await mallAdminOrderApi.pageDetail(params)
    const payload = extract<{ records?: any[]; total?: number }>(res, {})
    tableData.value = normalizeRows(payload.records || [], activeTab.value)
    pagination.total = Number(payload.total) || 0
    checkedRecords.value = []
  } catch (error: any) {
    tableData.value = []
    pagination.total = 0
    console.error('[订单处理] 列表加载失败', error)
    message.error(error?.response?.data?.message || '列表加载失败')
  } finally {
    loading.value = false
  }
}

// ═══ 详情 / 审核 / 收款 / 终止 ═══
const tableRef = ref<InstanceType<typeof BillDetailTable> | null>(null)
const detailVisible = ref(false)
const detailData = ref<OrderRow | null>(null)
/** 行级动作进行中的订单 id（收款/强制终止按钮 loading，防重复提交） */
const receivingId = ref<number | null>(null)
const terminatingId = ref<number | null>(null)

async function openDetail(record: OrderRow) {
  try {
    detailData.value = (await mallAdminOrderApi.detail(record.id)) as OrderRow
    detailVisible.value = true
  } catch (error) {
    console.warn('[订单处理] 订单详情获取失败', error)
  }
}

async function handleApprove(record: OrderRow) {
  try {
    await mallAdminOrderApi.approve(record.id)
    message.success(`订单 ${record.orderNo} 已审核通过`)
    loadTable()
  } catch (error) {
    console.warn('[订单处理] 审核通过失败', error)
  }
}

/**
 * 收款：POST /erp/mall/admin/order/{id}/receive（后端记收款金额并把支付状态置为已支付，与 /pay 同实现）
 * 成功后刷新列表；失败走 message.error 提示后端原因。
 */
async function handleReceive(record: OrderRow) {
  receivingId.value = record.id
  try {
    await mallOrderApi.receive(record.id)
    message.success(`订单 ${record.orderNo} 收款成功`)
    loadTable()
  } catch (error: any) {
    console.warn('[订单处理] 收款失败', error)
    message.error(error?.response?.data?.message || '收款失败')
  } finally {
    receivingId.value = null
  }
}

/**
 * 强制终止：POST /erp/mall/admin/order/{id}/terminate（后端把订单状态置为已取消）
 * 危险动作：二次确认后再提交，成功后刷新列表。
 */
async function handleTerminate(record: OrderRow) {
  terminatingId.value = record.id
  try {
    await mallOrderApi.terminate(record.id, '管理端强制终止')
    message.success(`订单 ${record.orderNo} 已强制终止`)
    loadTable()
  } catch (error: any) {
    console.warn('[订单处理] 强制终止失败', error)
    message.error(error?.response?.data?.message || '强制终止失败')
  } finally {
    terminatingId.value = null
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<OrderRow | null>(null)

function openReject(record: OrderRow) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await mallAdminOrderApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`订单 ${rejectTarget.value.orderNo} 已驳回`)
    rejectVisible.value = false
    loadTable()
  } catch (error) {
    console.warn('[订单处理] 驳回失败', error)
  } finally {
    rejectLoading.value = false
  }
}

// ═══ 批量动作（PUT /order/batch-approve、/order/batch-ship，body 为裸数组 ids） ═══
const batchApproving = ref(false)
const batchShipping = ref(false)

async function handleBatchApprove() {
  const ids = checkedRecords.value.map(r => Number(r.id)).filter(id => !isNaN(id))
  if (!ids.length) {
    message.warning('请先勾选要审核的订单')
    return
  }
  batchApproving.value = true
  try {
    const res: any = await mallOrderApi.batchApprove(ids)
    const count = Number(res?.data?.data ?? res?.data ?? res) || 0
    message.success(`批量审核完成，成功 ${count} 条`)
    loadTable()
  } catch (error: any) {
    console.warn('[订单处理] 批量审核失败', error)
    message.error(error?.response?.data?.message || '批量审核失败')
  } finally {
    batchApproving.value = false
  }
}

async function handleBatchShip() {
  const ids = checkedRecords.value.map(r => Number(r.id)).filter(id => !isNaN(id))
  if (!ids.length) {
    message.warning('请先勾选要发货的订单')
    return
  }
  batchShipping.value = true
  try {
    const res: any = await mallOrderApi.batchShip(ids)
    const count = Number(res?.data?.data ?? res?.data ?? res) || 0
    message.success(`批量发货完成，成功 ${count} 条`)
    loadTable()
  } catch (error: any) {
    console.warn('[订单处理] 批量发货失败', error)
    message.error(error?.response?.data?.message || '批量发货失败')
  } finally {
    batchShipping.value = false
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
    docShipDateStart.value = undefined
    docShipDateEnd.value = undefined
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
  checkedRecords.value = []
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
    const headers = currentColumns.value
      .filter(c => c.type !== 'rowNo' && c.type !== 'action' && c.type !== 'checkbox' && c.key !== '__filler__')
      .map(c => c.title)
    const body = rows.map(r => currentColumns.value
      .filter(c => c.type !== 'rowNo' && c.type !== 'action' && c.type !== 'checkbox' && c.key !== '__filler__')
      .map(c => r[c.key as keyof OrderRow] ?? ''))
    exportCsv(headers, body as any, `订单处理-${activeTab.value === 'doc' ? '按单据' : '按明细'}`)
  } finally {
    exporting.value = false
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「#」行号列由模板/引擎处理，不再由页面拼）。
// 两套表头随 Tab 切换，故用 printColumns() + useDataColumns 覆盖模板列。
function printColumns(): any[] {
  if (activeTab.value === 'doc') {
    return [
      { title: '单据编号', key: 'orderNo' },
      { title: '单据日期', key: 'orderDate', formatter: (v: any) => fmtDateTime(v) },
      { title: '客户', key: 'customerName' },
      { title: '订单金额', key: 'totalAmount', align: 'right' },
      { title: '已结金额', key: 'settledAmount', align: 'right' },
      { title: '单据状态', key: 'status', formatter: (v: any, record: any) => MALL_STATUS_MAP[mallStatusOf(record)]?.label || mallStatusOf(record) },
      { title: '支付方式', key: 'paymentMethod', formatter: (v: any) => PAYMENT_METHOD_MAP[v || ''] || v || '' },
      { title: '收货人', key: 'consignee' },
      { title: '联系电话', key: 'consigneePhone' },
      { title: '收货地址', key: 'consigneeAddress', formatter: (v: any, record: any) => v || record.shippingAddress || '' },
      { title: '卖家备注', key: 'orderRemark', formatter: (v: any, record: any) => v || record.remark || '' },
    ]
  }
  return [
    { title: '单据日期', key: 'orderDate', formatter: (v: any) => fmtDateTime(v) },
    { title: '单据编号', key: 'orderNo' },
    { title: '客户', key: 'customerName' },
    { title: '商品名称', key: 'productName' },
    { title: '商品货号', key: 'productCode' },
    { title: '规格', key: 'specification' },
    { title: '单位', key: 'unit' },
    { title: '销售数量', key: 'saleQuantity', align: 'right' },
    { title: '单价', key: 'unitPrice', align: 'right' },
    { title: '金额', key: 'amount', align: 'right' },
    { title: '备注', key: 'itemRemark' },
  ]
}

/** 打印行：已结金额缺失时回落「已收金额」（复刻原打印的 settledAmount ?? receivedAmount 口径） */
function toPrintRows(rows: OrderRow[]): OrderRow[] {
  return (rows || []).map(r => ({ ...r, settledAmount: r.settledAmount ?? r.receivedAmount }))
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'mall-order-process',
  title: '订单处理',
  rows: () => toPrintRows(tableData.value),
  selectedRows: () => toPrintRows(checkedRecords.value),
  columns: () => printColumns(),
  useDataColumns: true,
  // 原打印抬头的 记录数 元信息行（打印时间由模板 pageHeader 自动带）
  totalText: () => `${activeTab.value === 'doc' ? '按单据' : '按明细'}，记录数：${(checkedRecords.value.length ? checkedRecords.value : tableData.value).length}`,
  emptyTip: '没有可打印的数据',
})

/** 批量打印：勾选行优先（与打印(F8) 同一入口，useListPrint 已按勾选行取数） */
function handleBatchPrint() {
  if (!checkedRecords.value.length) {
    message.warning('请先勾选要打印的单据')
    return
  }
  handlePrint()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[订单处理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 交易汇总（保留既有 /trade-analysis 调用，用于顶部提示口径） ═══
const tradeSummary = ref<{ totalOrderCount?: number; totalGmv?: number }>({})

onMounted(async () => {
  loadTable()
  try {
    const res = await mallTradeApi.analysis()
    tradeSummary.value = (res as any)?.summary || {}
  } catch (error) {
    console.warn('[订单处理] 交易汇总获取失败', error)
  }
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #666; }
.cell-link { color: #1890ff; cursor: pointer; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
