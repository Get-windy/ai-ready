<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        CRM 报价单 · 台账列表（菜单 70330 的 list_path → 标签「历史」）
        · 本系统独有模块（ql361 无 CRM 域），规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/报价单开发文档.md
        · 注意：菜单主 path 是表单页 crm/quotation/form，本页是「历史」列表页（与客户/线索/商机/合同相反）
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable(+StandardPagination)
        · 本轮修复：
          ① P0 字段映射对齐后端实体（title / valid_from+valid_to / contact_name / payment_terms，明细 productSpec…）
          ② P0 状态值域改 Integer 0-8（原字符串 draft/sent… 致筛选 400、状态列空白、统计卡恒 0）
          ③ P0 客户下拉改真实接口 crmCustomerApi（原 5 条硬编码假数据）
          ④ P1 send 补必填 method，并按后端状态机调整按钮显隐
          ⑤ P1 桩按钮接线：版本历史 /{id}/versions + 新建版本 /{id}/new-version、复制 /{id}/copy、批量发送逐个 send
          ⑥ P1 formatter 改位置参数 (raw, record)；列配置键值统一 crm-quotation-table-columns
        · 表单收敛（文档 §9.2 P1）：新建/编辑不再用页面内嵌弹窗，统一跳转 form.vue，本页只保留详情抽屉
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('create')"
              v-permission="'crm:quotation:create'"
              type="primary"
              size="small"
              class="btn-add"
              @click="handleCreate"
            >
              <PlusOutlined /> 新建报价
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchSend')"
              v-permission="'crm:quotation:batchsend'"
              size="small"
              :disabled="selectedIds.size === 0"
              @click="handleBatchSend"
            >
              <SendOutlined /> 批量发送{{ selectedIds.size ? `(${selectedIds.size})` : '' }}
            </a-button>
            <a-button
              v-if="isButtonEnabled('batchDelete')"
              size="small"
              danger
              :disabled="selectedIds.size === 0"
              @click="handleBatchDelete"
            >
              <DeleteOutlined /> 批量删除{{ selectedIds.size ? `(${selectedIds.size})` : '' }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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

        <!-- ═══ 查询区（横向网格；只暴露后端 page() 真正接收的参数，不放死控件） ═══ -->
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
                  placeholder="报价单号/报价名称/客户"
                  size="small"
                  allow-clear
                  style="width: 200px"
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
                  :filter-option="filterCustomerOption"
                  :options="customerOptions"
                  :loading="customerLoading"
                  style="width: 200px"
                  @search="handleCustomerSearch"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('opportunityId')"
                class="search-item"
              >
                <span class="search-label">关联商机</span>
                <a-select
                  v-model:value="searchForm.opportunityId"
                  placeholder="全部商机"
                  size="small"
                  show-search
                  allow-clear
                  :filter-option="filterCustomerOption"
                  :options="opportunityOptions"
                  style="width: 200px"
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
                  style="width: 140px"
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
                  @click="handleReset"
                >
                  重置
                </a-button>
                <a-button
                  size="small"
                  @click="handlePendingApproval"
                >
                  待审批
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 加载失败提示（表格组件无 #empty 插槽，故置于表格上方） -->
            <a-alert
              v-if="hasError"
              class="table-error"
              type="warning"
              show-icon
              message="报价数据加载失败"
            >
              <template #action>
                <a-button
                  size="small"
                  @click="handleRefresh"
                >
                  重试
                </a-button>
              </template>
            </a-alert>

            <!-- 统计卡（口径：前 3 张取后端 /statistics 全量聚合，第 4 张为本页合计） -->
            <div class="stats-cards">
              <a-row :gutter="16">
                <a-col :span="6">
                  <div class="stat-card stat-card-blue">
                    <div
                      class="stat-icon"
                      style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);"
                    >
                      <FileTextOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        报价总数
                      </div>
                      <div class="stat-value">
                        {{ statsCounts.total }}
                      </div>
                      <div class="stat-desc">
                        全部报价（全量）
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
                      <SendOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        已发送
                      </div>
                      <div class="stat-value">
                        {{ statsCounts.sent }}
                      </div>
                      <div class="stat-desc">
                        等待回复（全量）
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
                        已接受
                      </div>
                      <div class="stat-value">
                        {{ statsCounts.accepted }}
                      </div>
                      <div class="stat-desc positive">
                        可转订单（全量）
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
                      <DollarOutlined />
                    </div>
                    <div class="stat-content">
                      <div class="stat-title">
                        报价总额
                      </div>
                      <div class="stat-value">
                        ¥{{ formatAmount(pageFinalAmount) }}
                      </div>
                      <div class="stat-desc">
                        本页合计（final_amount）
                      </div>
                    </div>
                  </div>
                </a-col>
              </a-row>
            </div>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              storage-key="crm-quotation-table-columns"
              global-config-key="crm-quotation-table-columns"
              @checkbox-change="handleCheckboxChange"
              @checkbox-all="handleCheckboxAll"
            >
              <template #statusCell="{ record }">
                <span v-if="record.__ghost" />
                <a-tag
                  v-else
                  :color="getStatusColor(record.status)"
                >
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>

              <template #quotationTypeCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ getTypeText(record.quotationType) }}</span>
              </template>

              <template #validCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ formatValidPeriod(record) }}</span>
              </template>

              <template #sentMethodCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ getSentMethodText(record.sentMethod) }}</span>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    v-permission="'crm:quotation:view'"
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    v-permission="'crm:quotation:edit'"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-if="record.status === 0"
                    type="link"
                    size="small"
                    @click="handleSubmitApproval(record)"
                  >
                    提交审批
                  </a-button>
                  <a-button
                    v-if="record.status === 1"
                    type="link"
                    size="small"
                    @click="handleApprove(record)"
                  >
                    审批通过
                  </a-button>
                  <a-button
                    v-if="record.status === 2"
                    v-permission="'crm:quotation:send'"
                    type="link"
                    size="small"
                    @click="openSendModal(record)"
                  >
                    发送
                  </a-button>
                  <a-button
                    v-if="record.status === 3"
                    type="link"
                    size="small"
                    @click="handleAccept(record)"
                  >
                    标记已接受
                  </a-button>
                  <a-button
                    v-if="record.status === 4"
                    v-permission="'crm:quotation:convert'"
                    type="link"
                    size="small"
                    @click="handleConvert(record)"
                  >
                    转订单
                  </a-button>
                  <a-dropdown trigger="click">
                    <a-button
                      type="link"
                      size="small"
                      class="action-more-btn"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                        <a-menu-item
                          v-if="record.status === 1"
                          key="reject"
                        >
                          <CloseOutlined /> 审批拒绝
                        </a-menu-item>
                        <a-menu-item
                          v-if="record.status === 3"
                          key="rejectByCustomer"
                        >
                          <CloseOutlined /> 客户拒绝
                        </a-menu-item>
                        <a-menu-item key="copy">
                          <CopyOutlined /> 复制报价
                        </a-menu-item>
                        <a-menu-item key="versions">
                          <HistoryOutlined /> 版本历史
                        </a-menu-item>
                        <a-menu-item
                          v-if="record.status !== 7 && record.status !== 8"
                          key="cancel"
                        >
                          <CloseOutlined /> 取消报价
                        </a-menu-item>
                        <a-menu-item key="download">
                          <DownloadOutlined /> 下载 PDF
                        </a-menu-item>
                        <a-menu-divider />
                        <a-menu-item
                          v-if="record.status === 0"
                          key="delete"
                          danger
                        >
                          <DeleteOutlined /> 删除
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                  <!-- 打印：走打印管理模块的打印链（组件内置，模板未配置时给出明确提示） -->
                  <PrintButton
                    template-type="quotation"
                    :business-id="String(record.id)"
                    business-type="quotation"
                    button-text="打印"
                    button-size="small"
                    button-type="link"
                    tooltip="打印"
                  />
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 经典分页栏 ═══ -->
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
        storage-key="crm-quotation-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 详情抽屉（只读；含明细） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="报价详情"
        placement="right"
        width="80vw"
        :footer="null"
      >
        <a-spin :spinning="detailLoading">
          <a-alert
            v-if="detailError"
            type="warning"
            show-icon
            message="详情数据加载失败"
          >
            <template #action>
              <a-button
                v-permission="'crm:quotation:detailrefresh'"
                size="small"
                @click="fetchDetail(detailId)"
              >
                重试
              </a-button>
            </template>
          </a-alert>
          <template v-else-if="detailData.id">
            <a-descriptions
              :column="2"
              bordered
              size="small"
            >
              <a-descriptions-item label="报价单号">
                <span class="mono">{{ detailData.quotationNo }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="版本">
                V{{ detailData.version || 1 }}
              </a-descriptions-item>
              <a-descriptions-item label="报价名称">
                {{ detailData.title || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="getStatusColor(detailData.status)">
                  {{ getStatusText(detailData.status) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="客户名称">
                {{ detailData.customerName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="关联商机">
                {{ detailData.opportunityName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="联系人">
                {{ detailData.contactName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="报价类型">
                {{ getTypeText(detailData.quotationType) }}
              </a-descriptions-item>
              <a-descriptions-item label="报价日期">
                {{ detailData.quotationDate || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="有效期">
                {{ formatValidPeriod(detailData) }}
              </a-descriptions-item>
              <a-descriptions-item label="币种">
                {{ detailData.currency || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="销售员">
                {{ detailData.salesPersonName || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="产品金额">
                <span class="mono">¥{{ formatAmount(detailData.totalAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="折扣金额">
                <span class="mono">¥{{ formatAmount(detailData.discountAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="税额">
                <span class="mono">¥{{ formatAmount(detailData.taxAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="报价总额">
                <span class="mono amount-red">¥{{ formatAmount(detailData.finalAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="付款条款">
                {{ detailData.paymentTerms || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="发送方式">
                {{ getSentMethodText(detailData.sentMethod) }}
              </a-descriptions-item>
              <a-descriptions-item label="发送时间">
                {{ detailData.sentTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="审批时间">
                {{ detailData.approvedTime || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="转单号">
                {{ detailData.orderNo || '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="拒绝/取消原因">
                {{ detailData.rejectedReason || '-' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ detailData.remark || '无' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-divider>报价明细</a-divider>
            <div class="detail-items">
              <BillDetailTable
                v-model:data-source="detailItems"
                :columns="detailItemColumns"
                :loading="detailLoading"
                :view-mode="true"
                :min-rows="5"
                storage-key="crm-quotation-detail-item-columns"
                global-config-key="crm-quotation-detail-item-columns"
              >
                <template #lineAmountCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span
                    v-else
                    class="mono"
                  >¥{{ formatAmount(record.lineAmount) }}</span>
                </template>
                <template #lineTotalCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span
                    v-else
                    class="mono amount-red"
                  >¥{{ formatAmount(record.lineTotal) }}</span>
                </template>
              </BillDetailTable>
            </div>

            <div class="detail-footer">
              <a-space>
                <a-button
                  v-if="detailData.status === 0"
                  v-permission="'crm:quotation:edit'"
                  type="primary"
                  @click="handleEdit(detailData)"
                >
                  编辑报价
                </a-button>
                <a-button
                  v-if="detailData.status === 1"
                  type="primary"
                  @click="handleApprove(detailData)"
                >
                  审批通过
                </a-button>
                <a-button
                  v-if="detailData.status === 2"
                  v-permission="'crm:quotation:sendfromdetail'"
                  type="primary"
                  @click="openSendModal(detailData)"
                >
                  <SendOutlined /> 发送报价
                </a-button>
                <a-button
                  v-if="detailData.status === 3"
                  type="primary"
                  @click="handleAccept(detailData)"
                >
                  标记已接受
                </a-button>
                <a-button
                  v-if="detailData.status === 4"
                  v-permission="'crm:quotation:convertfromdetail'"
                  type="primary"
                  @click="handleConvert(detailData)"
                >
                  转为订单
                </a-button>
                <a-button
                  v-permission="'crm:quotation:downloadpdf'"
                  @click="handleDownloadPdf"
                >
                  <DownloadOutlined /> 下载 PDF
                </a-button>
                <a-button @click="openVersionDrawer(detailData)">
                  <HistoryOutlined /> 版本历史
                </a-button>
              </a-space>
            </div>
          </template>
        </a-spin>
      </a-drawer>

      <!-- ═══ 发送方式选择（后端 POST /{id}/send 的 method 为必填） ═══ -->
      <a-modal
        v-model:open="sendVisible"
        title="发送报价"
        :confirm-loading="sendLoading"
        ok-text="确认发送"
        cancel-text="取消"
        :width="420"
        @ok="handleSendConfirm"
      >
        <p class="modal-tip">
          报价单：<span class="mono">{{ sendTarget.quotationNo }}</span>
        </p>
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="发送方式">
            <a-select
              v-model:value="sendMethod"
              :options="SENT_METHOD_OPTIONS"
              placeholder="请选择发送方式"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 原因输入（审批拒绝 / 客户拒绝 / 取消报价） ═══ -->
      <a-modal
        v-model:open="reasonVisible"
        :title="reasonTitle"
        :confirm-loading="reasonLoading"
        ok-text="确认"
        cancel-text="取消"
        :width="460"
        @ok="handleReasonConfirm"
      >
        <a-textarea
          v-model:value="reasonText"
          :rows="3"
          placeholder="请填写原因（后端必填）"
        />
      </a-modal>

      <!-- ═══ 版本历史抽屉（GET /{id}/versions、POST /{id}/new-version） ═══ -->
      <a-drawer
        v-model:open="versionVisible"
        title="版本历史"
        placement="right"
        width="60vw"
        :footer="null"
      >
        <a-spin :spinning="versionLoading">
          <div class="version-toolbar">
            <a-button
              size="small"
              @click="handleNewVersion"
            >
              <PlusOutlined /> 新建版本
            </a-button>
          </div>
          <a-table
            :columns="versionColumns"
            :data-source="versionList"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'version'">
                V{{ record.version || 1 }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  查看
                </a-button>
              </template>
            </template>
          </a-table>
          <a-empty
            v-if="!versionLoading && versionList.length === 0"
            description="暂无可查版本（首版 parent_id 未回写，接口对首版返回空）"
          />
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { quotationApi } from '@/api/erp'
import { crmCustomerApi, opportunityApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'
import request from '@/utils/request'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
  SendOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  DollarOutlined,
  HistoryOutlined,
  DeleteOutlined,
  CloseOutlined,
  CopyOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmQuotationList' })

/**
 * 后端已就绪但 api/erp.ts 的 quotationApi 未封装（本任务禁止改 api/ 目录，
 * 缺口已在交付报告登记）。以下端点全部直连后端 QuotationController，无任何造假数据。
 */
const quotationOps = {
  getItems(id: any) { return request.get(`/crm/quotation/${id}/items`) },
  submit(id: any) { return request.post(`/crm/quotation/${id}/submit`) },
  approve(id: any, note?: string) { return request.post(`/crm/quotation/${id}/approve`, null, { params: { note } }) },
  reject(id: any, reason: string) { return request.post(`/crm/quotation/${id}/reject`, null, { params: { reason } }) },
  send(id: any, method: string) { return request.post(`/crm/quotation/${id}/send`, null, { params: { method } }) },
  accept(id: any, note?: string) { return request.post(`/crm/quotation/${id}/accept`, null, { params: { note } }) },
  rejectByCustomer(id: any, reason: string) { return request.post(`/crm/quotation/${id}/reject-by-customer`, null, { params: { reason } }) },
  cancel(id: any, reason: string) { return request.post(`/crm/quotation/${id}/cancel`, null, { params: { reason } }) },
  copy(id: any) { return request.post(`/crm/quotation/${id}/copy`) },
  versions(id: any) { return request.get(`/crm/quotation/${id}/versions`) },
  newVersion(id: any) { return request.post(`/crm/quotation/${id}/new-version`) },
  batchDelete(ids: any[]) { return request.delete('/crm/quotation/batch', { data: ids }) },
  statistics() { return request.get('/crm/quotation/statistics') },
}

// ═══ 字典（与后端 QuotationStatus / QuotationType / SentMethod 枚举逐字对齐） ═══
const STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已审批', value: 2 },
  { label: '已发送', value: 3 },
  { label: '已接受', value: 4 },
  { label: '已拒绝', value: 5 },
  { label: '已过期', value: 6 },
  { label: '已转订单', value: 7 },
  { label: '已取消', value: 8 },
]
const STATUS_TEXT: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '已发送', 4: '已接受',
  5: '已拒绝', 6: '已过期', 7: '已转订单', 8: '已取消',
}
const STATUS_COLOR: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'cyan', 3: 'blue', 4: 'green',
  5: 'red', 6: 'orange', 7: 'purple', 8: 'default',
}
const QUOTATION_TYPE_TEXT: Record<number, string> = {
  1: '标准报价', 2: '项目报价', 3: '招标报价', 4: '重复报价', 5: '特殊报价',
}

/**
 * 发送方式：DB 列 sent_method 是 varchar（存量数据为 'email'），
 * 而 SentMethod 枚举是 Integer 1-6 —— 三套词表并存（文档 §5.5）。
 * 写入口径统一为**字符串小写**（与列类型、存量数据一致）；读取侧兼容小写/大写/中文/数字。
 */
const SENT_METHOD_OPTIONS = [
  { label: '邮件', value: 'email' },
  { label: '微信', value: 'wechat' },
  { label: '短信', value: 'sms' },
  { label: '传真', value: 'fax' },
  { label: '人工送达', value: 'hand_delivery' },
  { label: '在线查看', value: 'online' },
]
const SENT_METHOD_BY_NUM: Record<number, string> = {
  1: '邮件', 2: '微信', 3: '短信', 4: '传真', 5: '人工送达', 6: '在线查看',
}

function getStatusText(status: any): string {
  // 先判空：Number(null) === 0 会被误判成「草稿」
  if (status === null || status === undefined || status === '') return '-'
  return STATUS_TEXT[Number(status)] ?? String(status)
}
function getStatusColor(status: any): string {
  return STATUS_COLOR[Number(status)] || 'default'
}
function getTypeText(type: any): string {
  if (type == null || type === '') return '-'
  return QUOTATION_TYPE_TEXT[Number(type)] || String(type)
}
function getSentMethodText(method: any): string {
  if (method == null || method === '') return '-'
  const raw = String(method)
  const lower = raw.toLowerCase()
  const matched = SENT_METHOD_OPTIONS.find(o => o.value === lower || o.label === raw)
  if (matched) return matched.label
  const byNum = SENT_METHOD_BY_NUM[Number(raw)]
  return byNum || raw
}

function formatAmount(v: any): string {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatValidPeriod(record: any): string {
  if (!record) return '-'
  if (record.validFrom || record.validTo) {
    return `${record.validFrom || '不限'} ~ ${record.validTo || '不限'}`
  }
  return '-'
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'opportunityId', label: '关联商机', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建报价', enabled: true },
  { key: 'batchSend', label: '批量发送', enabled: true },
  { key: 'batchDelete', label: '批量删除', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))

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

// ═══ 状态与数据 ═══
const router = useRouter()
const loading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedIds = ref<Set<string>>(new Set())
const statsCounts = reactive({ total: 0, sent: 0, accepted: 0 })
const pageFinalAmount = computed(() =>
  tableData.value.reduce((sum, r) => sum + (Number(r?.finalAmount) || 0), 0)
)

const searchForm = reactive({
  keyword: '',
  customerId: undefined as any,
  opportunityId: undefined as any,
  status: undefined as number | undefined,
})

const customerOptions = ref<{ label: string; value: any }[]>([])
const customerLoading = ref(false)
const opportunityOptions = ref<{ label: string; value: any }[]>([])

function filterCustomerOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

// ═══ 列定义（key 与后端 QuotationVO 字段逐字对齐；金额口径见文档 §7.3） ═══
// ⚠️ formatter 是位置参数 (raw, record)，切勿写成 ({ row }) 对象解构
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'checkbox', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 250, fixed: 'left' },
  { key: 'quotationNo', title: '报价单号', type: 'input', width: 150 },
  { key: 'title', title: '报价名称', type: 'input', width: 200 },
  { key: 'customerName', title: '客户名称', type: 'input', width: 160 },
  { key: 'opportunityName', title: '关联商机', type: 'input', width: 160, defaultHidden: true },
  { key: 'contactName', title: '联系人', type: 'input', width: 100 },
  { key: 'quotationDate', title: '报价日期', type: 'input', width: 110 },
  { key: 'validTo', title: '有效期', type: 'slot', slotName: 'validCell', width: 190 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'totalAmount', title: '产品金额', type: 'input', width: 120, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'discountAmount', title: '折扣金额', type: 'input', width: 110, align: 'right', defaultHidden: true, formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'taxAmount', title: '税额', type: 'input', width: 100, align: 'right', defaultHidden: true, formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'finalAmount', title: '报价总额', type: 'input', width: 130, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'currency', title: '币种', type: 'input', width: 80, defaultHidden: true },
  { key: 'quotationType', title: '报价类型', type: 'slot', slotName: 'quotationTypeCell', width: 110, defaultHidden: true },
  { key: 'version', title: '版本', type: 'input', width: 70, defaultHidden: true },
  { key: 'salesPersonName', title: '销售员', type: 'input', width: 100 },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'paymentTerms', title: '付款条款', type: 'input', width: 160, defaultHidden: true },
  { key: 'deliveryTerms', title: '交货条款', type: 'input', width: 160, defaultHidden: true },
  { key: 'sentMethod', title: '发送方式', type: 'slot', slotName: 'sentMethodCell', width: 100, defaultHidden: true },
  { key: 'sentTime', title: '发送时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'approvedTime', title: '审批时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'orderNo', title: '转单号', type: 'input', width: 150 },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
  { key: 'createTime', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
]

const summaryColumns = computed(() => {
  const rows = tableData.value.filter((r: any) => !r.__ghost)
  const totalAmount = rows.reduce((s: number, r: any) => s + (Number(r.totalAmount) || 0), 0)
  const discountAmount = rows.reduce((s: number, r: any) => s + (Number(r.discountAmount) || 0), 0)
  const finalAmount = rows.reduce((s: number, r: any) => s + (Number(r.finalAmount) || 0), 0)
  // 合计值按组件契约传数字（列本身已有 ¥ 格式化 formatter）
  return [
    { key: 'totalAmount', value: totalAmount },
    { key: 'discountAmount', value: discountAmount },
    { key: 'finalAmount', value: finalAmount, highlight: true },
  ]
})

const detailItemColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'productCode', title: '商品编码', type: 'input', width: 130 },
  { key: 'productName', title: '商品名称', type: 'input', width: 180 },
  { key: 'productSpec', title: '规格型号', type: 'input', width: 120 },
  { key: 'productUnit', title: '单位', type: 'input', width: 70 },
  { key: 'quantity', title: '数量', type: 'input', width: 80, align: 'right' },
  { key: 'unitPrice', title: '单价', type: 'input', width: 100, align: 'right', formatter: (raw: any) => `¥${formatAmount(raw)}` },
  { key: 'discountRate', title: '折扣%', type: 'input', width: 80, align: 'right' },
  { key: 'lineAmount', title: '行金额', type: 'slot', slotName: 'lineAmountCell', width: 110, align: 'right' },
  { key: 'lineTotal', title: '行小计', type: 'slot', slotName: 'lineTotalCell', width: 110, align: 'right' },
  { key: 'taxRate', title: '税率%', type: 'input', width: 80, align: 'right', defaultHidden: true },
  { key: 'lineNo', title: '行号', type: 'input', width: 70, defaultHidden: true },
  { key: 'deliveryDays', title: '交期(天)', type: 'input', width: 90, defaultHidden: true },
  { key: 'description', title: '说明', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 列表加载 ═══
async function fetchList() {
  loading.value = true
  hasError.value = false
  try {
    const res: any = await quotationApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      customerId: searchForm.customerId,
      opportunityId: searchForm.opportunityId,
      status: searchForm.status,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedIds.value = new Set()
  } catch (err) {
    console.error('[CRM报价] 加载列表失败', err)
    hasError.value = true
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

/** 后端 /statistics 返回 { 中文状态: count }，用于修正原「已发送/已接受恒 0」与总数口径 */
async function fetchStatistics() {
  try {
    const res: any = await quotationOps.statistics()
    const get = (desc: string) => Number(res?.[desc]) || 0
    statsCounts.sent = get('已发送')
    statsCounts.accepted = get('已接受')
    statsCounts.total = Object.values(res || {}).reduce((s: number, v: any) => s + (Number(v) || 0), 0)
  } catch (err) {
    console.warn('[CRM报价] 统计接口不可用，回退为当前页统计', err)
    const rows = tableData.value.filter((r: any) => !r.__ghost)
    statsCounts.sent = rows.filter((r: any) => Number(r.status) === 3).length
    statsCounts.accepted = rows.filter((r: any) => Number(r.status) === 4).length
    statsCounts.total = pagination.total
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList().then(fetchStatistics)
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.customerId = undefined
  searchForm.opportunityId = undefined
  searchForm.status = undefined
  pagination.current = 1
  fetchList().then(fetchStatistics)
}

/** 快捷筛选：待审批队列（status=1，后端状态机中唯一可从草稿一键推进的待办态） */
function handlePendingApproval() {
  searchForm.status = 1
  handleSearch()
}

function handleRefresh() {
  fetchList().then(fetchStatistics)
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function handleCheckboxChange(record: any, _rowIndex: number, checked: boolean) {
  const set = new Set(selectedIds.value)
  const key = String(record?.id ?? '')
  if (!key) return
  if (checked) set.add(key)
  else set.delete(key)
  selectedIds.value = set
}

function handleCheckboxAll(checked: boolean, records: any[]) {
  selectedIds.value = checked
    ? new Set((records || []).filter((r: any) => !r.__ghost).map((r: any) => String(r.id)))
    : new Set()
}

function getSelectedRecords(): any[] {
  return tableData.value.filter((r: any) => !r.__ghost && selectedIds.value.has(String(r.id)))
}

async function handleCustomerSearch(keyword: string) {
  customerLoading.value = true
  try {
    const list: any = await crmCustomerApi.dropdown(keyword || undefined)
    const searched = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
    // 保留当前已选客户，避免远端搜索结果里没有它时下拉退化成显示 id
    const selected = customerOptions.value.filter(o => String(o.value) === String(searchForm.customerId))
    const merged = new Map<string, any>()
    for (const o of [...selected, ...searched]) merged.set(String(o.value), o)
    customerOptions.value = [...merged.values()]
  } catch (err) {
    console.warn('[CRM报价] 客户下拉搜索失败', err)
  } finally {
    customerLoading.value = false
  }
}

async function loadCustomerOptions() {
  try {
    const res: any = await crmCustomerApi.page({ pageNum: 1, pageSize: 200 })
    customerOptions.value = (res?.records || []).map((c: any) => ({ label: c.customerName, value: c.id }))
  } catch (err) {
    console.warn('[CRM报价] 客户下拉加载失败', err)
    customerOptions.value = []
  }
}

async function loadOpportunityOptions() {
  try {
    const res: any = await opportunityApi.page({ pageNum: 1, pageSize: 200 })
    opportunityOptions.value = (res?.records || []).map((o: any) => ({ label: o.name, value: o.id }))
  } catch (err) {
    console.warn('[CRM报价] 商机下拉加载失败', err)
    opportunityOptions.value = []
  }
}

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref(false)
const detailData = ref<any>({})
const detailItems = ref<any[]>([])
const detailId = ref<any>(null)

/** GET /{id} 不返回 items（后端只给 itemCount），必须另调 GET /{id}/items */
async function fetchDetail(id: any) {
  if (!id) return
  detailId.value = id
  detailLoading.value = true
  detailError.value = false
  try {
    const [vo, items] = await Promise.all([
      quotationApi.getById(id),
      quotationOps.getItems(id),
    ])
    detailData.value = (vo as any) || {}
    detailItems.value = Array.isArray(items) ? items : []
  } catch (err) {
    console.error('[CRM报价] 加载详情失败', err)
    detailError.value = true
    detailData.value = {}
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}

function handleView(record: any) {
  detailVisible.value = true
  fetchDetail(record.id)
}

// ═══ 表单收敛：新建 / 编辑统一跳转 form.vue ═══
function handleCreate() {
  router.push({ path: '/crm/quotation/form' })
}

function handleEdit(record: any) {
  detailVisible.value = false
  router.push({ path: '/crm/quotation/form', query: { id: String(record.id) } })
}

// ═══ 状态机动作（与后端 QuotationServiceImpl 前置校验一致） ═══
const ACTION_BUSY = ref(false)

async function runAction(fn: () => Promise<any>, okText: string) {
  if (ACTION_BUSY.value) return
  ACTION_BUSY.value = true
  try {
    await fn()
    message.success(okText)
    await fetchList()
    fetchStatistics()
    if (detailVisible.value) fetchDetail(detailId.value)
  } catch (err: any) {
    console.error('[CRM报价] 操作失败', err)
    // 错误提示由 request 拦截器统一处理，此处只兜底
  } finally {
    ACTION_BUSY.value = false
  }
}

function handleSubmitApproval(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定将报价「${record.title || record.quotationNo}」提交审批吗？`,
    okText: '确认提交',
    cancelText: '取消',
    centered: true,
    onOk: () => runAction(() => quotationOps.submit(record.id), '已提交审批'),
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批通过',
    content: `确定审批通过报价「${record.title || record.quotationNo}」吗？`,
    okText: '审批通过',
    cancelText: '取消',
    centered: true,
    onOk: () => runAction(() => quotationOps.approve(record.id), '审批已通过'),
  })
}

function handleAccept(record: any) {
  Modal.confirm({
    title: '标记为已接受',
    content: `确定客户已接受报价「${record.title || record.quotationNo}」吗？`,
    okText: '确认',
    cancelText: '取消',
    centered: true,
    onOk: () => runAction(() => quotationOps.accept(record.id), '已标记为已接受'),
  })
}

function handleConvert(record: any) {
  Modal.confirm({
    title: '确认转订单',
    content: `确定将报价「${record.title || record.quotationNo}」转为销售订单吗？`,
    okText: '确认转换',
    cancelText: '取消',
    centered: true,
    onOk: () => runAction(() => quotationApi.convertToOrder(record.id), '报价已成功转为订单'),
  })
}

// ═══ 发送方式弹窗（后端 method 为必填 @RequestParam，不传必 400） ═══
const sendVisible = ref(false)
const sendLoading = ref(false)
const sendMethod = ref<string>('email')
const sendTarget = ref<any>({})

function openSendModal(record: any) {
  if (Number(record.status) !== 2) {
    message.warning('只有「已审批」状态的报价单可以发送')
    return
  }
  sendTarget.value = record
  sendMethod.value = 'email'
  sendVisible.value = true
}

async function handleSendConfirm() {
  if (!sendMethod.value) {
    message.warning('请选择发送方式')
    return
  }
  sendLoading.value = true
  try {
    await quotationOps.send(sendTarget.value.id, sendMethod.value)
    message.success('报价已发送')
    sendVisible.value = false
    await fetchList()
    fetchStatistics()
    if (detailVisible.value) fetchDetail(detailId.value)
  } catch (err) {
    console.error('[CRM报价] 发送报价失败', err)
  } finally {
    sendLoading.value = false
  }
}

/** 批量发送：后端无批量端点，逐个调用 send（仅「已审批」可发送，其余跳过并汇总结果） */
function handleBatchSend() {
  const rows = getSelectedRecords()
  if (!rows.length) {
    message.warning('请先勾选要发送的报价单')
    return
  }
  const eligible = rows.filter((r: any) => Number(r.status) === 2)
  const skipped = rows.length - eligible.length
  if (!eligible.length) {
    message.warning('勾选的报价单中没有「已审批」状态，无法发送（后端仅允许已审批→发送）')
    return
  }
  Modal.confirm({
    title: '批量发送',
    content: `可发送 ${eligible.length} 张报价单${skipped ? `（跳过 ${skipped} 张非「已审批」）` : ''}，发送方式为「邮件」。确定继续吗？`,
    okText: '确认发送',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      const failed: string[] = []
      for (const r of eligible) {
        try {
          await quotationOps.send(r.id, 'email')
        } catch (err) {
          console.warn('[CRM报价] 批量发送失败', r.quotationNo, err)
          failed.push(r.quotationNo || String(r.id))
        }
      }
      const okCount = eligible.length - failed.length
      if (failed.length) {
        message.warning(`发送完成：成功 ${okCount} 张，失败 ${failed.length} 张（${failed.join('、')}）`)
      } else {
        message.success(`批量发送完成：成功 ${okCount} 张`)
      }
      await fetchList()
      fetchStatistics()
    },
  })
}

// ═══ 原因输入弹窗（审批拒绝 / 客户拒绝 / 取消） ═══
const reasonVisible = ref(false)
const reasonLoading = ref(false)
const reasonText = ref('')
const reasonMode = ref<'reject' | 'rejectByCustomer' | 'cancel'>('reject')
const reasonTarget = ref<any>({})
const reasonTitle = computed(() => {
  if (reasonMode.value === 'reject') return '审批拒绝'
  if (reasonMode.value === 'rejectByCustomer') return '客户拒绝'
  return '取消报价'
})

function openReasonModal(mode: 'reject' | 'rejectByCustomer' | 'cancel', record: any) {
  reasonMode.value = mode
  reasonTarget.value = record
  reasonText.value = ''
  reasonVisible.value = true
}

async function handleReasonConfirm() {
  const reason = reasonText.value.trim()
  if (!reason) {
    message.warning('请填写原因（后端必填）')
    return
  }
  reasonLoading.value = true
  try {
    const id = reasonTarget.value.id
    if (reasonMode.value === 'reject') {
      await quotationOps.reject(id, reason)
      message.success('已拒绝')
    } else if (reasonMode.value === 'rejectByCustomer') {
      await quotationOps.rejectByCustomer(id, reason)
      message.success('已标记为客户拒绝')
    } else {
      await quotationOps.cancel(id, reason)
      message.success('报价已取消')
    }
    reasonVisible.value = false
    await fetchList()
    fetchStatistics()
    if (detailVisible.value) fetchDetail(detailId.value)
  } catch (err) {
    console.error('[CRM报价] 操作失败', err)
  } finally {
    reasonLoading.value = false
  }
}

// ═══ 更多下拉 ═══
function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'reject':
      openReasonModal('reject', record)
      break
    case 'rejectByCustomer':
      openReasonModal('rejectByCustomer', record)
      break
    case 'cancel':
      openReasonModal('cancel', record)
      break
    case 'copy':
      Modal.confirm({
        title: '复制报价',
        content: `确定复制报价「${record.title || record.quotationNo}」吗？（服务端复制，标题追加「(副本)」）`,
        okText: '确认复制',
        cancelText: '取消',
        centered: true,
        onOk: () => runAction(() => quotationOps.copy(record.id), '复制成功'),
      })
      break
    case 'versions':
      openVersionDrawer(record)
      break
    case 'download':
      // 后端无报价 PDF 生成端点（打印链走行内「打印」按钮）；不造假数据，给明确提示
      message.info('报价单 PDF 后端暂无生成端点，请使用行内「打印」按钮走打印管理模块的打印链')
      break
    case 'delete':
      Modal.confirm({
        title: '确认删除',
        content: `确定要删除报价「${record.title || record.quotationNo}」吗？`,
        okText: '确认删除',
        okType: 'danger',
        cancelText: '取消',
        centered: true,
        onOk: () => runAction(() => quotationApi.delete(record.id), '删除成功'),
      })
      break
  }
}

function handleDownloadPdf() {
  message.info('报价单 PDF 后端暂无生成端点，请使用行内「打印」按钮走打印管理模块的打印链')
}

function handleBatchDelete() {
  const rows = getSelectedRecords()
  if (!rows.length) {
    message.warning('请先勾选要删除的报价单')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除勾选的 ${rows.length} 张报价单吗？`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: () =>
      runAction(
        () => quotationOps.batchDelete(rows.map((r: any) => String(r.id))),
        `已删除 ${rows.length} 张报价单`,
      ),
  })
}

// ═══ 版本历史（GET /{id}/versions、POST /{id}/new-version） ═══
const versionVisible = ref(false)
const versionLoading = ref(false)
const versionList = ref<any[]>([])
const versionQuotationId = ref<any>(null)
const versionColumns = [
  { title: '版本', key: 'version', width: 90 },
  { title: '报价单号', dataIndex: 'quotationNo', key: 'quotationNo', width: 160 },
  { title: '报价名称', dataIndex: 'title', key: 'title' },
  { title: '状态', key: 'status', width: 110 },
  { title: '报价总额', dataIndex: 'finalAmount', key: 'finalAmount', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

async function openVersionDrawer(record: any) {
  versionQuotationId.value = record.id
  versionList.value = []
  versionVisible.value = true
  await loadVersions()
}

async function loadVersions() {
  if (!versionQuotationId.value) return
  versionLoading.value = true
  try {
    const res: any = await quotationOps.versions(versionQuotationId.value)
    versionList.value = Array.isArray(res) ? res : []
  } catch (err) {
    console.error('[CRM报价] 加载版本历史失败', err)
    versionList.value = []
  } finally {
    versionLoading.value = false
  }
}

function handleNewVersion() {
  Modal.confirm({
    title: '新建版本',
    content: '将基于当前报价单创建新版本（版本号 +1，标题追加「 Vn」），确定继续吗？',
    okText: '创建',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await quotationOps.newVersion(versionQuotationId.value)
        message.success('新版本已创建')
        await loadVersions()
        await fetchList()
        fetchStatistics()
      } catch (err) {
        console.error('[CRM报价] 新建版本失败', err)
      }
    },
  })
}

// ═══ 打印(F8) / 导出 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = tableData.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.quotationNo)}</td>
      <td>${escapeHtml(r.title || '')}</td>
      <td>${escapeHtml(r.customerName || '')}</td>
      <td>${escapeHtml(r.quotationDate || '')}</td>
      <td>${escapeHtml(formatValidPeriod(r))}</td>
      <td>${escapeHtml(getStatusText(r.status))}</td>
      <td style="text-align:right">¥${formatAmount(r.finalAmount)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>报价单台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>报价单台账</h2>
    <div class="meta">
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>报价单号</th><th>报价名称</th><th>客户名称</th>
      <th>报价日期</th><th>有效期</th><th>状态</th><th>报价总额</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
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

function handleExport() {
  const rows = tableData.value.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['报价单号', '报价名称', '客户名称', '联系人', '报价日期', '有效期至', '状态',
    '产品金额', '折扣金额', '税额', '报价总额', '币种', '销售员', '转单号']
  const body = rows.map((r: any) => [
    r.quotationNo, r.title, r.customerName, r.contactName, r.quotationDate, r.validTo,
    getStatusText(r.status), formatAmount(r.totalAmount), formatAmount(r.discountAmount),
    formatAmount(r.taxAmount), formatAmount(r.finalAmount), r.currency, r.salesPersonName, r.orderNo,
  ])
  exportCsv(headers, body, `报价单台账_${dayjs().format('YYYYMMDD_HHmmss')}`)
}

// ═══ 快捷键与自动刷新 ═══
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function anyOverlayOpen(): boolean {
  return detailVisible.value || sendVisible.value || reasonVisible.value || versionVisible.value || showPageConfig.value
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    handleRefresh()
    return
  }
  if (e.ctrlKey && e.key.toLowerCase() === 'n') {
    e.preventDefault()
    handleCreate()
  }
}

function handleParentCreate() {
  handleCreate()
}

function handleError(error: Error) {
  console.error('[CRM报价] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  await Promise.all([loadCustomerOptions(), loadOpportunityOptions()])
  fetchList().then(fetchStatistics)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    // 弹窗/抽屉打开时暂停轮询，避免打断录入与抽屉内容
    if (!anyOverlayOpen()) fetchList()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, handleRefresh as any)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, handleRefresh as any)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchList })
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.table-error { flex-shrink: 0; margin: 8px 0; }
.detail-items { max-height: 360px; overflow: auto; }
.version-toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.modal-tip { margin-bottom: 12px; color: #666; }

.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}
.amount-red { color: #f5222d; font-weight: 500; }

.stats-cards { flex-shrink: 0; margin-bottom: 12px; }
.stat-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}
.stat-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12); transform: translateY(-2px); }
.stat-card.stat-card-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); border: 1px solid #91d5ff; }
.stat-card.stat-card-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); border: 1px solid #b7eb8f; }
.stat-card.stat-card-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); border: 1px solid #ffd591; }
.stat-card.stat-card-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); border: 1px solid #d3adf7; }
.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  margin-right: 12px;
}
.stat-content { flex: 1; }
.stat-title { font-size: 13px; color: #666; }
.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.stat-desc { font-size: 12px; color: #999; }
.stat-desc.positive { color: #52c41a; }

.action-more-btn { padding: 0 4px; }

.detail-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

/* 橙色新增按钮（CRM 模块统一） */
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
