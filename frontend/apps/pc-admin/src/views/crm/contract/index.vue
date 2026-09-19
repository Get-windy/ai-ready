<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="合同管理"
      full-height
    >
      <!--
        合同台账列表页（CRM → 合同管理 → 合同，菜单 80230；标签「添加」跳 form.vue）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM（Odoo/SAP/金蝶/用友）建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/合同开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 列配置齿轮在表头 rowNo 列（个人 + 全局）；页面配置弹窗控制查询条件显隐 + 功能按钮启用
        · 本轮改造：
          ① 外壳升级路线 A（原为 PageContainer 内嵌 ErrorBoundary + BillTableList 平铺）
          ② 修 P0「新建合同必 400」：contractType 由字符串 sales/purchase/service/lease
             改为后端 ContractType 数字枚举 1..7（DTO 是 Integer，发字符串 Jackson 必 400）
          ③ 修 4 个查询条件静默失效（原只发 keyword + status）：改为全量透传后端真实支持的
             keyword/customerId/contractType/status
          ④ 修列渲染三处缺陷：formatter 对象解构（编号列抛 TypeError、金额列恒 ¥0.00）
             → 全部改插槽；状态列补 type:'slot'；操作列补 slotName:'actionCell'
          ⑤ 列配置键值补全（storage-key = global-config-key = crm-contract-table-columns）
          ⑥ 详情抽屉去掉读不存在字段（contractTypeLabel/paymentMethodLabel/signPerson/terms）
             → 改本地字典映射 + 真实列（paymentTerms 等）
          ⑦ 审批流程 steps 由「纯静态四级」改为后端真实状态驱动（Contract.status + 真实时间列）
          ⑧ 清桩：去掉「下载合同/续签申请/开票申请/批量审批」4 处 message.info 桩；
             「删除」实为 terminate 的误导入口一并去掉（后端无 DELETE /{id}）
          ⑨ 客户下拉改 crmCustomerApi.dropdown()（原读 r.name，而实体字段是 customerName → 文本恒空；
             过滤函数取 vnode 属性 → 恒不过滤）
          ⑩ 去掉前端演示单号（CT+日期+3 位随机，违反「严禁前端演示号」红线，且会被后端号段覆盖）
      -->
      <!-- ═══ 页头附加区：数据状态 / 自动刷新倒计时 / 快捷键提示 ═══ -->
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
            <span class="shortcut-hint"><kbd>F8</kbd> 打印</span>
          </span>
        </a-space>
      </template>

      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新建合同 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            v-permission="'crm:contract:create'"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新建合同
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="refreshLoading"
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
                  placeholder="合同编号/名称/客户"
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
                  :filter-option="filterCustomerOption"
                  :options="customerOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('contractType')"
                class="search-item"
              >
                <span class="search-label">合同类型</span>
                <a-select
                  v-model:value="searchForm.contractType"
                  placeholder="全部类型"
                  size="small"
                  allow-clear
                  :options="CONTRACT_TYPE_OPTIONS"
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
                  :options="CONTRACT_STATUS_OPTIONS"
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
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（统计卡置于表格面板内，flex 纵向堆叠） ═══ -->
        <template #table>
          <div class="table-area">
            <div class="stat-strip">
              <div class="stat-card">
                <div class="stat-title">
                  待审批
                </div>
                <div class="stat-value">
                  {{ statusCounts.pending }}
                </div>
                <div class="stat-desc">
                  本页待审批份数
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  待签署
                </div>
                <div class="stat-value">
                  {{ statusCounts.signing }}
                </div>
                <div class="stat-desc">
                  本页已审批/待签署
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  生效中
                </div>
                <div class="stat-value">
                  {{ statusCounts.effective }}
                </div>
                <div class="stat-desc">
                  本页生效/执行/已完成
                </div>
              </div>
              <div class="stat-card">
                <div class="stat-title">
                  合同总额
                </div>
                <div class="stat-value">
                  ¥{{ formatAmount(totalAmount) }}
                </div>
                <div class="stat-desc">
                  本页合计
                </div>
              </div>
            </div>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="crm-contract-table-columns"
              global-config-key="crm-contract-table-columns"
            >
              <template #contractTypeCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ contractTypeLabel(record) }}</span>
              </template>

              <template #amountCell="{ record }">
                <span v-if="record.__ghost" />
                <span
                  v-else
                  class="amount-cell"
                >¥{{ formatAmount(record.contractAmount) }}</span>
              </template>

              <template #paidAmountCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>¥{{ formatAmount(record.paidAmount) }}</span>
              </template>

              <template #pendingAmountCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>¥{{ formatAmount(record.pendingAmount) }}</span>
              </template>

              <template #progressCell="{ record }">
                <span v-if="record.__ghost" />
                <span v-else>{{ record.executionProgress ?? 0 }}%</span>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="getStatusColor(record.status)"
                >
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>

              <!-- 操作列：插槽名必须与组件兜底名 actionCell 一致（原页面提供 #action → 按钮不渲染） -->
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
                      v-permission="'crm:contract:view'"
                      type="link"
                      size="small"
                      @click="handleView(record)"
                    >
                      <EyeOutlined />
                    </a-button>
                  </a-tooltip>
                  <PrintButton
                    :record="record"
                    :business-id="record.id"
                    business-type="contract"
                    button-type="link"
                    button-size="small"
                    tooltip="打印"
                  />
                  <a-tooltip
                    v-if="Number(record.status) === 0"
                    title="提交审批"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:contract:edit'"
                      type="link"
                      size="small"
                      @click="handleSubmitApproval(record)"
                    >
                      <SendOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    v-if="Number(record.status) === 0"
                    title="编辑"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:contract:edit'"
                      type="link"
                      size="small"
                      @click="handleEdit(record)"
                    >
                      <EditOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    v-if="Number(record.status) === 1"
                    title="审批"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:contract:approve'"
                      type="link"
                      size="small"
                      @click="handleApprove(record)"
                    >
                      <CheckCircleOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    v-if="Number(record.status) === 2 || Number(record.status) === 3"
                    title="签订"
                    placement="bottom"
                  >
                    <a-button
                      v-permission="'crm:contract:sign'"
                      type="link"
                      size="small"
                      @click="handleSign(record)"
                    >
                      <FileDoneOutlined />
                    </a-button>
                  </a-tooltip>
                  <a-tooltip
                    v-if="Number(record.status) >= 5"
                    title="终止合同"
                    placement="bottom"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="handleTerminate(record)"
                    >
                      <StopOutlined />
                    </a-button>
                  </a-tooltip>
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
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 新建 / 编辑合同弹窗 ═══ -->
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
                label="合同编号"
                name="contractNo"
              >
                <a-input
                  v-model:value="formData.contractNo"
                  placeholder="保存后由后端号段生成"
                  disabled
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="合同名称"
                name="contractName"
              >
                <a-input
                  v-model:value="formData.contractName"
                  placeholder="请输入合同名称"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="合同类型"
                name="contractType"
              >
                <!-- 值域 = 后端 ContractType 枚举（1..7），必须发数字，发字符串 Jackson 必 400 -->
                <a-select
                  v-model:value="formData.contractType"
                  placeholder="请选择合同类型"
                  size="small"
                  :options="CONTRACT_TYPE_OPTIONS"
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
                  :filter-option="filterCustomerOption"
                  :options="customerOptions"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="开始日期"
                name="startDate"
              >
                <a-date-picker
                  v-model:value="formData.startDate"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="结束日期"
                name="endDate"
              >
                <a-date-picker
                  v-model:value="formData.endDate"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="合同金额"
                name="contractAmount"
              >
                <a-input-number
                  v-model:value="formData.contractAmount"
                  :min="0"
                  :precision="2"
                  style="width: 100%"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="币种"
                name="currency"
              >
                <a-select
                  v-model:value="formData.currency"
                  size="small"
                  :options="CURRENCY_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="付款方式"
                name="paymentMethod"
              >
                <a-select
                  v-model:value="formData.paymentMethod"
                  placeholder="请选择付款方式"
                  size="small"
                  allow-clear
                  :options="PAYMENT_METHOD_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="签订日期"
                name="signDate"
              >
                <a-date-picker
                  v-model:value="formData.signDate"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="负责人"
                name="salesPersonName"
              >
                <a-input
                  v-model:value="formData.salesPersonName"
                  placeholder="请输入负责人姓名"
                  size="small"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item
                label="付款条款"
                name="paymentTerms"
                :label-col="{ span: 3 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="formData.paymentTerms"
                  placeholder="请输入付款条款"
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

      <!-- ═══ 详情抽屉 ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="合同详情"
        placement="right"
        width="80vw"
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
                v-permission="'crm:contract:detailrefresh'"
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
              <a-descriptions-item label="合同编号">
                <span class="contract-no">{{ detailData.contractNo }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="合同名称">
                {{ detailData.contractName }}
              </a-descriptions-item>
              <a-descriptions-item label="合同类型">
                {{ contractTypeLabel(detailData) }}
              </a-descriptions-item>
              <a-descriptions-item label="客户名称">
                {{ detailData.customerName || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="开始日期">
                {{ detailData.startDate || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="结束日期">
                {{ detailData.endDate || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="合同金额">
                <span class="amount-cell">¥{{ formatAmount(detailData.contractAmount) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="付款方式">
                {{ paymentMethodLabel(detailData.paymentMethod) }}
              </a-descriptions-item>
              <a-descriptions-item label="已收金额">
                ¥{{ formatAmount(detailData.paidAmount) }}
              </a-descriptions-item>
              <a-descriptions-item label="待收金额">
                ¥{{ formatAmount(detailData.pendingAmount) }}
              </a-descriptions-item>
              <a-descriptions-item label="签订日期">
                {{ detailData.signDate || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="签订时间">
                {{ detailData.signedTime || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="签署方式">
                {{ signMethodLabel(detailData.signMethod) }}
              </a-descriptions-item>
              <a-descriptions-item label="签订地点">
                {{ detailData.signedLocation || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="执行进度">
                {{ detailData.executionProgress ?? 0 }}%
              </a-descriptions-item>
              <a-descriptions-item label="负责人">
                {{ detailData.salesPersonName || '无' }}
              </a-descriptions-item>
              <a-descriptions-item label="合同状态">
                <a-tag :color="getStatusColor(detailData.status)">
                  {{ getStatusText(detailData.status) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="创建时间">
                {{ detailData.createTime || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="付款条款"
                :span="2"
              >
                {{ detailData.paymentTerms || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="交付条款"
                :span="2"
              >
                {{ detailData.deliveryTerms || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="质保条款"
                :span="2"
              >
                {{ detailData.warrantyTerms || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="服务条款"
                :span="2"
              >
                {{ detailData.serviceTerms || '无' }}
              </a-descriptions-item>
              <a-descriptions-item
                label="备注"
                :span="2"
              >
                {{ detailData.remark || '无' }}
              </a-descriptions-item>
            </a-descriptions>

            <a-divider>审批与签署进度</a-divider>
            <!--
              进度条按后端真实状态机驱动（单级审批：0 草稿 → 1 待审批 → 2 已审批 → 4 已签署 → 5 生效中），
              描述取 crm_contract 的真实时间列。原页面的「四级审批」是纯静态 UI
              （currentStep/creator/departmentApprover/financeApprover/generalApprover 后端全都不存在，
              且后端实为单级审批），本页不再伪造流程数据。
            -->
            <a-steps
              :current="detailStepCurrent"
              status="process"
              size="small"
            >
              <a-step
                title="提交申请"
                :description="detailData.createTime || '—'"
              />
              <a-step
                title="审批"
                :description="detailData.approvedTime || (Number(detailData.status) === 1 ? '审批中' : '—')"
              />
              <a-step
                title="签署"
                :description="detailData.signedTime || '—'"
              />
              <a-step
                title="生效"
                :description="detailData.effectiveTime || '—'"
              />
            </a-steps>
            <p
              v-if="detailData.approvedNote"
              class="detail-note"
            >
              审批意见：{{ detailData.approvedNote }}
            </p>
            <p
              v-if="detailData.terminatedReason"
              class="detail-note"
            >
              终止原因：{{ detailData.terminatedReason }}
            </p>
          </template>
        </a-spin>
      </a-drawer>

      <!-- ═══ 签订弹窗（只收集后端真实落库的字段：签署方式 + 签订地点） ═══ -->
      <a-modal
        v-model:open="signModalVisible"
        title="合同签订"
        width="500px"
        :confirm-loading="signLoading"
        @ok="handleSignSubmit"
        @cancel="signModalVisible = false"
      >
        <a-form
          :model="signForm"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="合同名称">
            <a-input
              :value="signForm.contractName"
              disabled
              size="small"
            />
          </a-form-item>
          <a-form-item
            label="签署方式"
            required
          >
            <a-select
              v-model:value="signForm.signMethod"
              placeholder="请选择签署方式"
              size="small"
              :options="SIGN_METHOD_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="签订地点">
            <a-input
              v-model:value="signForm.location"
              placeholder="请输入签订地点"
              size="small"
            />
          </a-form-item>
          <p class="sign-tip">
            说明：签订日期与签订人由系统按当前登录人与时间自动落库（后端 sign 端点只接受签署方式与地点）。
          </p>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  EyeOutlined,
  EditOutlined,
  CheckCircleOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  DownloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  SendOutlined,
  StopOutlined,
  WarningOutlined,
  SyncOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { contractApi, crmCustomerApi } from '@/api/crm'
import { exportCsv } from '@/utils/exportCsv'

defineOptions({ name: 'CrmContractIndex' })

// ═══ 字典：合同类型（唯一真源 = 后端 ContractType 枚举，Integer 1..7） ═══
const CONTRACT_TYPE_OPTIONS = [
  { label: '销售合同', value: 1 },
  { label: '采购合同', value: 2 },
  { label: '服务合同', value: 3 },
  { label: '项目合同', value: 4 },
  { label: '框架合同', value: 5 },
  { label: '合作伙伴合同', value: 6 },
  { label: '其他合同', value: 7 },
]
const CONTRACT_TYPE_TEXT: Record<number, string> = {
  1: '销售合同', 2: '采购合同', 3: '服务合同', 4: '项目合同',
  5: '框架合同', 6: '合作伙伴合同', 7: '其他合同',
}
// 合同状态（唯一真源 = 后端 ContractStatus 枚举，11 态）
const CONTRACT_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已审批', value: 2 },
  { label: '待签署', value: 3 },
  { label: '已签署', value: 4 },
  { label: '生效中', value: 5 },
  { label: '执行中', value: 6 },
  { label: '已完成', value: 7 },
  { label: '已终止', value: 8 },
  { label: '已过期', value: 9 },
  { label: '已取消', value: 10 },
]
const statusColorMap: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'blue', 3: 'geekblue', 4: 'purple',
  5: 'green', 6: 'green', 7: 'green', 8: 'red', 9: 'red', 10: 'red',
}
const statusTextMap: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署',
  5: '生效中', 6: '执行中', 7: '已完成', 8: '已终止', 9: '已过期', 10: '已取消',
}
// 付款方式：后端 payment_method 为 Integer 但无枚举类、无字典表（见合同开发文档 §5.4），
// 这里沿用本模块表单页（菜单「添加」入口）既有的 1/2/3 词表，不新造值域。
const PAYMENT_METHOD_OPTIONS = [
  { label: '一次性付款', value: 1 },
  { label: '分期付款', value: 2 },
  { label: '货到付款', value: 3 },
]
const PAYMENT_METHOD_TEXT: Record<number, string> = {
  1: '一次性付款', 2: '分期付款', 3: '货到付款',
}
const CURRENCY_OPTIONS = [
  { label: '人民币', value: 'CNY' },
  { label: '美元', value: 'USD' },
]
// 签署方式（唯一真源 = 后端 SignMethod 枚举，Integer 1..4）
const SIGN_METHOD_OPTIONS = [
  { label: '在线签署', value: 1 },
  { label: '线下签署', value: 2 },
  { label: '电子签章', value: 3 },
  { label: '混合签署', value: 4 },
]
const SIGN_METHOD_TEXT: Record<number, string> = {
  1: '在线签署', 2: '线下签署', 3: '电子签章', 4: '混合签署',
}

function contractTypeLabel(record: any): string {
  if (record?.contractTypeDesc) return record.contractTypeDesc
  return CONTRACT_TYPE_TEXT[Number(record?.contractType)] || '-'
}
function paymentMethodLabel(v: any): string {
  return PAYMENT_METHOD_TEXT[Number(v)] || '无'
}
function signMethodLabel(v: any): string {
  return SIGN_METHOD_TEXT[Number(v)] || '无'
}
function getStatusColor(status: any): string {
  return statusColorMap[Number(status)] || 'default'
}
function getStatusText(status: any): string {
  return statusTextMap[Number(status)] || '未知'
}
function formatAmount(amount: any): string {
  if (amount === null || amount === undefined || isNaN(Number(amount))) return '0.00'
  return Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function handleError(error: Error) {
  console.error('[CRM合同] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '关键词', visible: true },
  { key: 'customerId', label: '客户', visible: true },
  { key: 'contractType', label: '合同类型', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新建合同', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-contract-page-config'

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

// ═══ 客户下拉（crm_contract.customer_id 指向 CRM 客户 crm_customer） ═══
const customerOptions = ref<{ label: string; value: any }[]>([])

function filterCustomerOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function fetchCustomerOptions() {
  try {
    // 原实现读 r.name，而 CRM 客户实体字段是 customerName → 下拉文本恒空；dropdown 端点直接返回 {id,name}
    const list = await crmCustomerApi.dropdown()
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({
      label: c.name,
      value: c.id,
    }))
  } catch (e) {
    console.warn('[CRM合同] 客户下拉获取失败', e)
    customerOptions.value = []
  }
}

// ═══ 查询条件（只挂后端 page 端点真实支持的参数，避免「看起来有筛选、实际不发送」） ═══
const searchForm = reactive({
  keyword: '',
  customerId: undefined as any,
  contractType: undefined as number | undefined,
  status: undefined as number | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const refreshLoading = ref(false)
const hasError = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref<string>('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ═══ 表格列（rowNo 承载列配置齿轮；插槽列必须 type:'slot' + slotName） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'contractNo', title: '合同编号', type: 'input', width: 160 },
  { key: 'contractName', title: '合同名称', type: 'input', width: 180 },
  { key: 'customerName', title: '客户名称', type: 'input', width: 160 },
  { key: 'contractType', title: '合同类型', type: 'slot', slotName: 'contractTypeCell', width: 110 },
  { key: 'contractAmount', title: '合同金额', type: 'slot', slotName: 'amountCell', width: 130, align: 'right' },
  { key: 'paidAmount', title: '已收金额', type: 'slot', slotName: 'paidAmountCell', width: 120, align: 'right', defaultHidden: true },
  { key: 'pendingAmount', title: '待收金额', type: 'slot', slotName: 'pendingAmountCell', width: 120, align: 'right', defaultHidden: true },
  { key: 'currency', title: '币种', type: 'input', width: 80, defaultHidden: true },
  { key: 'signDate', title: '签订日期', type: 'input', width: 110 },
  { key: 'startDate', title: '开始日期', type: 'input', width: 110 },
  { key: 'endDate', title: '结束日期', type: 'input', width: 110 },
  { key: 'executionProgress', title: '执行进度', type: 'slot', slotName: 'progressCell', width: 100, defaultHidden: true },
  { key: 'opportunityName', title: '关联商机', type: 'input', width: 150, defaultHidden: true },
  { key: 'quotationNo', title: '关联报价单', type: 'input', width: 150, defaultHidden: true },
  { key: 'contactName', title: '联系人', type: 'input', width: 100, defaultHidden: true },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'departmentName', title: '部门', type: 'input', width: 120, defaultHidden: true },
  { key: 'renewalCount', title: '续签次数', type: 'input', width: 90, defaultHidden: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100, align: 'center' },
  { key: 'createTime', title: '创建时间', type: 'input', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 160, defaultHidden: true },
]

// ═══ 统计卡（本页口径，与原实现一致） ═══
const statusCounts = computed(() => {
  const rows = tableData.value.filter(r => !r.__ghost)
  return {
    pending: rows.filter(r => Number(r.status) === 1).length,
    signing: rows.filter(r => [2, 3].includes(Number(r.status))).length,
    effective: rows.filter(r => Number(r.status) >= 5 && Number(r.status) < 8).length,
  }
})
const totalAmount = computed(() =>
  tableData.value.filter(r => !r.__ghost).reduce((s, r) => s + (Number(r.contractAmount) || 0), 0)
)

// ═══ 数据加载 ═══
async function fetchData(silent = false) {
  if (!silent) loading.value = true
  hasError.value = false
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      customerId: searchForm.customerId,
      contractType: searchForm.contractType,
      status: searchForm.status,
    }
    const result: any = await contractApi.page(params)
    const records = result?.records || result?.data?.records || []
    tableData.value = records
    pagination.total = Number(result?.total ?? result?.data?.total ?? 0)
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    if (!silent) {
      hasError.value = true
      message.error('获取合同数据失败')
    }
    console.warn('[CRM合同] 获取合同数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally {
    if (!silent) loading.value = false
    refreshLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.customerId = undefined
  searchForm.contractType = undefined
  searchForm.status = undefined
  pagination.current = 1
  fetchData()
}
function handleRefresh() {
  refreshLoading.value = true
  lastUpdateTime.value = ''
  fetchData()
}
function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

// ═══ 自动刷新（30s；详情抽屉打开时暂停，避免无意义轮询） ═══
function startAutoRefresh() {
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (!loading.value && !modalVisible.value && !detailVisible.value && !signModalVisible.value) {
      fetchData(true)
    }
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}
function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

// ═══ 详情抽屉 ═══
const detailVisible = ref(false)
const detailData = ref<any>({})
const detailLoading = ref(false)
const detailError = ref(false)

async function fetchDetail(id: any) {
  detailLoading.value = true
  detailError.value = false
  try {
    const res = await contractApi.getById(id)
    detailData.value = (res as any) || {}
  } catch (err) {
    detailError.value = true
    console.warn('[CRM合同] 获取合同详情失败', err)
    message.error('获取合同详情失败')
  } finally {
    detailLoading.value = false
  }
}
function handleView(record: any) {
  detailData.value = {}
  fetchDetail(record.id)
  detailVisible.value = true
}
function handleDetailClose() {
  detailVisible.value = false
  detailData.value = {}
  detailError.value = false
}
function handleDetailRefresh() {
  if (detailData.value.id) fetchDetail(detailData.value.id)
}

/** 审批进度游标：由真实 status 驱动（单级审批） */
const detailStepCurrent = computed(() => {
  const s = Number(detailData.value?.status)
  if (!detailData.value?.id || Number.isNaN(s)) return 0
  if (s <= 0) return 0
  if (s === 1) return 1
  if (s === 2 || s === 3) return 2
  if (s === 4) return 3
  return 4
})

// ═══ 新建 / 编辑弹窗（字段只保留 ContractCreateDTO 真实可落库的列） ═══
const modalVisible = ref(false)
const modalTitle = ref('新建合同')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive<any>({
  id: undefined,
  contractNo: '',
  contractName: '',
  contractType: undefined,
  customerId: undefined,
  startDate: undefined,
  endDate: undefined,
  contractAmount: undefined,
  currency: 'CNY',
  paymentMethod: undefined,
  signDate: undefined,
  salesPersonName: '',
  paymentTerms: '',
  remark: '',
})

const formRules = {
  contractName: [{ required: true, message: '请输入合同名称' }],
  contractType: [{ required: true, message: '请选择合同类型' }],
  customerId: [{ required: true, message: '请选择客户' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }],
  contractAmount: [{ required: true, message: '请输入合同金额' }],
}

function resetForm() {
  Object.assign(formData, {
    id: undefined,
    contractNo: '',
    contractName: '',
    contractType: undefined,
    customerId: undefined,
    startDate: undefined,
    endDate: undefined,
    contractAmount: undefined,
    currency: 'CNY',
    paymentMethod: undefined,
    signDate: undefined,
    salesPersonName: '',
    paymentTerms: '',
    remark: '',
  })
}

const initialFormSnapshot = ref('')
const formDirty = computed(() => {
  if (!modalVisible.value) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formData })
}

function handleAdd() {
  modalTitle.value = '新建合同'
  isEdit.value = false
  resetForm()
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

function handleEdit(record: any) {
  modalTitle.value = '编辑合同'
  isEdit.value = true
  resetForm()
  // 只回填本页表单真实持有的字段（其余字段由后端维护，避免把只读列回写）
  Object.assign(formData, {
    id: record.id,
    contractNo: record.contractNo || '',
    contractName: record.contractName || '',
    contractType: record.contractType,
    customerId: record.customerId,
    startDate: record.startDate,
    endDate: record.endDate,
    contractAmount: record.contractAmount,
    currency: record.currency || 'CNY',
    paymentMethod: record.paymentMethod,
    signDate: record.signDate,
    salesPersonName: record.salesPersonName || '',
    paymentTerms: record.paymentTerms || '',
    remark: record.remark || '',
  })
  modalVisible.value = true
  nextTick(() => saveFormSnapshot())
}

/** 组装提交体：只发 DTO 真实存在的字段（原实现把 contractNo/signPerson/terms 一起发，
 *  后端 FAIL_ON_UNKNOWN_PROPERTIES=false 会静默丢弃，属于「填了也存不下来」的假字段） */
function buildPayload() {
  const customer = customerOptions.value.find(o => String(o.value) === String(formData.customerId))
  return {
    contractName: formData.contractName,
    contractType: formData.contractType, // 数字 1..7
    customerId: formData.customerId,
    customerName: customer?.label || undefined,
    startDate: formData.startDate || undefined,
    endDate: formData.endDate || undefined,
    contractAmount: formData.contractAmount,
    currency: formData.currency || 'CNY',
    paymentMethod: formData.paymentMethod,
    signDate: formData.signDate || undefined,
    salesPersonName: formData.salesPersonName || undefined,
    paymentTerms: formData.paymentTerms || undefined,
    remark: formData.remark || undefined,
  }
}

async function handleSubmit(saveAndNew = false) {
  try {
    await formRef.value?.validate()
  } catch (err) {
    console.warn('[CRM合同] 表单验证失败', err)
    return
  }
  submitLoading.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && formData.id) {
      await contractApi.update(formData.id as any, payload)
    } else {
      await contractApi.create(payload as any)
    }
    message.success('保存成功')
    if (saveAndNew) {
      isEdit.value = false
      resetForm()
      nextTick(() => saveFormSnapshot())
    } else {
      modalVisible.value = false
      fetchData()
    }
  } catch (err: any) {
    console.warn('[CRM合同] 保存失败', err)
    message.error(err?.response?.data?.message || err?.message || '保存失败')
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
      onOk: () => { modalVisible.value = false },
    })
    return
  }
  modalVisible.value = false
}

// ═══ 行操作 ═══
function handleSubmitApproval(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定将合同「${record.contractName}」提交审批吗？`,
    okText: '确定',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await contractApi.submitForApproval(record.id)
        message.success('已提交审批')
        fetchData()
      } catch (err: any) {
        console.warn('[CRM合同] 提交审批失败', err)
        message.error(err?.response?.data?.message || err?.message || '提交审批失败')
      }
    },
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '确认审批',
    content: `确定要审批合同「${record.contractName}」吗？`,
    okText: '确认审批',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await contractApi.approve(record.id)
        message.success('审批成功')
        fetchData()
      } catch (err: any) {
        console.warn('[CRM合同] 审批失败', err)
        message.error(err?.response?.data?.message || err?.message || '审批失败')
      }
    },
  })
}

function handleTerminate(record: any) {
  Modal.confirm({
    title: '确认终止',
    content: `确定要终止合同「${record.contractName}」吗？`,
    okText: '确认终止',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk: async () => {
      try {
        await contractApi.terminate(record.id, '手动终止')
        message.success('合同已终止')
        fetchData()
      } catch (err: any) {
        console.warn('[CRM合同] 终止失败', err)
        message.error(err?.response?.data?.message || err?.message || '终止失败')
      }
    },
  })
}

// ═══ 签订 ═══
const signModalVisible = ref(false)
const signLoading = ref(false)
const signForm = reactive<any>({ contractId: undefined, contractName: '', signMethod: 2, location: '' })

function handleSign(record: any) {
  signForm.contractId = record.id
  signForm.contractName = record.contractName
  signForm.signMethod = 2
  signForm.location = ''
  signModalVisible.value = true
}

async function handleSignSubmit() {
  if (!signForm.signMethod) {
    message.warning('请选择签署方式')
    return
  }
  signLoading.value = true
  try {
    // signMethod 后端是 String 入参（且当前 Service 未落库），这里传枚举数字的字符串形式，值域 = SignMethod 1..4
    await contractApi.sign(signForm.contractId, String(signForm.signMethod), signForm.location || undefined)
    message.success(`合同「${signForm.contractName}」签订成功`)
    signModalVisible.value = false
    fetchData()
  } catch (err: any) {
    console.warn('[CRM合同] 签订失败', err)
    message.error(err?.response?.data?.message || err?.message || '签订失败')
  } finally {
    signLoading.value = false
  }
}

// ═══ 导出 / 打印(F8) ═══
function handleExport() {
  const rows = tableData.value.filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['合同编号', '合同名称', '客户名称', '合同类型', '合同金额', '已收金额', '待收金额',
    '签订日期', '开始日期', '结束日期', '执行进度', '负责人', '状态', '创建时间']
  const lines = rows.map((r: any) => [
    r.contractNo || '', r.contractName || '', r.customerName || '', contractTypeLabel(r),
    formatAmount(r.contractAmount), formatAmount(r.paidAmount), formatAmount(r.pendingAmount),
    r.signDate || '', r.startDate || '', r.endDate || '', `${r.executionProgress ?? 0}%`,
    r.salesPersonName || '', getStatusText(r.status), r.createTime || '',
  ])
  exportCsv(headers, lines, '合同')
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = tableData.value.filter(r => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.contractNo)}</td>
      <td>${escapeHtml(r.contractName || '')}</td>
      <td>${escapeHtml(r.customerName || '')}</td>
      <td>${escapeHtml(contractTypeLabel(r))}</td>
      <td style="text-align:right">${escapeHtml(formatAmount(r.contractAmount))}</td>
      <td>${escapeHtml(r.startDate || '')}</td>
      <td>${escapeHtml(r.endDate || '')}</td>
      <td>${escapeHtml(getStatusText(r.status))}</td>
      <td>${escapeHtml(r.salesPersonName || '')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>合同台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>合同台账</h2>
    <div class="meta">
      <span>打印时间：${new Date().toLocaleString('zh-CN')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>合同编号</th><th>合同名称</th><th>客户名称</th><th>合同类型</th>
      <th>合同金额</th><th>开始日期</th><th>结束日期</th><th>状态</th><th>负责人</th></tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

// ═══ 快捷键（F5 刷新 / Ctrl+N 新增 / F8 打印，均带 300ms 防抖） ═══
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', handleRefresh); return }
  if (e.ctrlKey && (e.key === 'n' || e.key === 'N')) { e.preventDefault(); debounceClick('add', handleAdd); return }
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    debounceClick('print', handlePrint)
  }
}

function handleParentCreate() { handleAdd() }
function handleParentRefresh() { fetchData() }

onMounted(() => {
  fetchCustomerOptions()
  fetchData()
  startAutoRefresh()
  window.addEventListener('crm:create' as any, handleParentCreate as any)
  window.addEventListener('crm:refresh' as any, handleParentRefresh as any)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('crm:create' as any, handleParentCreate as any)
  window.removeEventListener('crm:refresh' as any, handleParentRefresh as any)
  document.removeEventListener('keydown', handleKeydown)
})

onBeforeRouteLeave((to, from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单内容尚未保存，确定要离开吗？',
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
.data-status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #666; }
.update-time { color: #999; }

.search-area { padding: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 140px; }
.search-item :deep(.ant-input-affix-wrapper) { width: 200px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* 统计卡：置于表格面板内（CategoryListLayout 的 #table 插槽，flex 纵向堆叠） */
.stat-strip {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 10px 12px 8px;
}
.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  background: #fafafa;
}
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 20px; font-weight: 600; color: #303133; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; }
.stat-desc { font-size: 12px; color: #999; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.contract-no { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-weight: 500; }
.amount-cell { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; color: #f5222d; font-weight: 500; }

.table-empty { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.table-empty-icon { font-size: 48px; color: #d9d9d9; }
.table-empty-text { color: #999; margin-top: 12px; }

.detail-note { margin-top: 12px; font-size: 13px; color: #666; }
.sign-tip { margin: 0; font-size: 12px; color: #999; line-height: 1.6; }

/* 橙色新增按钮（CRM 模块统一） */
.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; user-select: none; }
.shortcut-hint { display: inline-flex; align-items: center; gap: 2px; padding: 1px 4px; border-radius: 3px; background: #f5f7fa; }
.shortcut-hint kbd {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 18px; height: 18px; padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px; color: #606266; background: #fff;
  border: 1px solid #d0d5dd; border-radius: 3px; box-shadow: 0 1px 0 #d0d5dd; line-height: 18px;
}

/* ── 自动刷新倒计时徽章 ──────────────────── */
.auto-refresh-badge {
  display: inline-flex; align-items: center; gap: 4px; font-size: 12px;
  color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
</style>
