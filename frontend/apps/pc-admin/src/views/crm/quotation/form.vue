<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        CRM 报价单 · 单据表单页（菜单 70330 的主入口 path=crm/quotation/form）
        · 本系统独有模块（ql361 无 CRM 域），规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/报价单开发文档.md
        · 注意：菜单点开**直接进本页**（与客户/线索/商机/合同相反），标签「历史」才到列表页
        · 路线 A 外壳：ErrorBoundary > PageContainer(full-height) > CategoryListLayout
        · 本轮修复：
          ① P0 字段映射对齐后端 QuotationCreateDTO / QuotationItemDTO（title / valid_from+valid_to /
             contact_name / payment_terms；明细 productSpec / productUnit / unitPrice / discountRate …），
             原先 quotationName / contactPerson / contactPhone / validDays / terms / spec / unit / price / discount
             全部被后端静默丢弃，DB 里 title / valid_to / contact_name 恒 NULL
          ② P0 编辑回填：GET /{id} 不返回 items，改为另调 GET /{id}/items（原编辑时明细恒空）
          ③ P0 删除前端演示单号：单号由后端号段生成（QT+yyyyMMdd+4 位）
          ④ P1 「提交」改接 POST /{id}/submit（原先与「保存」同为一个 create/update，名不副实）
          ⑤ P1 Ctrl+S / Ctrl+Enter / F8 改真实快捷键监听（原先只是提示文字）
          ⑥ P1 客户下拉改真实接口（原 index 弹窗是硬编码假数据；本页原 pageSize 200 无远端搜索 → 加远端搜索）
          ⑦ 补齐后端已就绪的状态机入口：审批通过/拒绝、发送（含必填 method）、标记已接受、客户拒绝、转订单、取消、新建版本
        · 缺口（后端未支持，前端不造假）：crm_quotation 无 contact_phone 列；QuotationCreateDTO
          无 competitorQuote/competitorPrice；单据级 discount_rate/tax_rate 不参与后端金额计算，故不设入口
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：返回 / 单号 / 状态 / 保存 / 提交审批 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              size="small"
              @click="handleBack"
            >
              返回列表
            </a-button>
            <span class="doc-no mono">{{ form.quotationNo || '（保存后由后端号段生成）' }}</span>
            <a-tag :color="getStatusColor(form.status)">
              {{ getStatusText(form.status) }}
            </a-tag>
            <a-button
              v-if="isButtonEnabled('save')"
              size="small"
              :loading="saving"
              :disabled="!editable"
              @click="handleSave"
            >
              保存
            </a-button>
            <a-button
              v-if="isButtonEnabled('submit') && editable"
              type="primary"
              size="small"
              :loading="saving"
              @click="handleSubmitApproval"
            >
              提交审批
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：状态操作 / 刷新 / 打印 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-dropdown v-if="isButtonEnabled('more')">
              <a-button size="small">
                状态操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleFlowAction(key as string)">
                  <a-menu-item
                    v-if="form.status === 1"
                    key="approve"
                  >
                    审批通过
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status === 1"
                    key="reject"
                  >
                    审批拒绝
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status === 2"
                    key="send"
                  >
                    发送报价
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status === 3"
                    key="accept"
                  >
                    标记已接受
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status === 3"
                    key="rejectByCustomer"
                  >
                    客户拒绝
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status === 4"
                    key="convert"
                  >
                    转为销售订单
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.status !== 7 && form.status !== 8"
                    key="cancel"
                  >
                    取消报价
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    v-if="form.id"
                    key="newVersion"
                  >
                    新建版本
                  </a-menu-item>
                  <a-menu-item
                    v-if="form.id"
                    key="versions"
                  >
                    版本历史
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="pageLoading"
              @click="fetchData"
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

        <!-- ═══ 查询区：明细筛选（横向网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isFieldVisible('itemKeyword')"
                class="search-item"
              >
                <span class="search-label">明细筛选</span>
                <a-input
                  v-model:value="itemKeyword"
                  placeholder="商品名称/编码/规格"
                  size="small"
                  allow-clear
                  style="width: 220px"
                  @press-enter="handleItemFilter"
                  @change="handleItemFilter"
                />
              </div>
              <div class="search-item search-actions">
                <span class="search-label">
                  明细 {{ filteredItems.length }} / {{ form.items.length }} 行，总数量 {{ totalQuantity }}
                </span>
                <a-button
                  size="small"
                  :disabled="!itemKeyword"
                  @click="resetItemFilter"
                >
                  重置筛选
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表体：单据卡片区 + 明细表 ═══ -->
        <template #table>
          <div class="table-area">
            <div class="form-scroll">
              <a-alert
                v-if="form.id && !editable"
                class="form-alert"
                type="info"
                show-icon
                :message="`当前状态「${getStatusText(form.status)}」不可修改（后端仅允许草稿编辑）。如需变更请使用右上「状态操作 → 新建版本」。`"
              />

              <!-- 基本信息（字段与 QuotationCreateDTO 逐字对齐） -->
              <a-card
                title="基本信息"
                size="small"
                class="form-section"
              >
                <a-form
                  ref="formRef"
                  :model="form"
                  :rules="formRules"
                  :label-col="{ span: 6 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="报价单号">
                        <a-input
                          :value="form.quotationNo"
                          placeholder="保存后由后端号段生成"
                          disabled
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="报价名称"
                        name="title"
                      >
                        <a-input
                          v-model:value="form.title"
                          placeholder="请输入报价名称"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="报价日期"
                        name="quotationDate"
                      >
                        <a-date-picker
                          v-model:value="form.quotationDate"
                          value-format="YYYY-MM-DD"
                          style="width: 100%"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="有效期(天)">
                        <a-input-number
                          v-model:value="form.validDays"
                          :min="0"
                          :disabled="!editable"
                          style="width: 100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="有效期至">
                        <a-input
                          :value="computedValidTo"
                          placeholder="按报价日期 + 有效天数自动计算"
                          disabled
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="报价类型">
                        <a-select
                          v-model:value="form.quotationType"
                          placeholder="请选择报价类型"
                          :options="QUOTATION_TYPE_OPTIONS"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item
                        label="客户名称"
                        name="customerId"
                      >
                        <a-select
                          v-model:value="form.customerId"
                          placeholder="请选择客户"
                          show-search
                          allow-clear
                          :filter-option="filterCustomerOption"
                          :options="customerOptions"
                          :loading="customerLoading"
                          :disabled="!editable"
                          size="small"
                          @search="handleCustomerSearch"
                          @change="onCustomerChange"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="联系人">
                        <a-input
                          v-model:value="form.contactName"
                          placeholder="请输入联系人"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="关联商机">
                        <a-select
                          v-model:value="form.opportunityId"
                          placeholder="请选择商机"
                          allow-clear
                          show-search
                          :filter-option="filterCustomerOption"
                          :options="opportunityOptions"
                          :disabled="!editable"
                          size="small"
                          @change="onOpportunityChange"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="币种">
                        <a-select
                          v-model:value="form.currency"
                          placeholder="请选择币种"
                          :options="CURRENCY_OPTIONS"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="销售员">
                        <a-input
                          :value="form.salesPersonName"
                          placeholder="取当前登录用户"
                          disabled
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="赢率(%)">
                        <a-input-number
                          v-model:value="form.winProbability"
                          :min="0"
                          :max="100"
                          :disabled="!editable"
                          style="width: 100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <!-- 条款与交付（全部为后端已有列） -->
              <a-card
                title="条款与交付"
                size="small"
                class="form-section"
              >
                <a-form
                  :model="form"
                  :label-col="{ span: 5 }"
                  :wrapper-col="{ span: 17 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="付款条款">
                        <a-textarea
                          v-model:value="form.paymentTerms"
                          placeholder="如：款到发货 / 月结 30 天"
                          :rows="2"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="交货条款">
                        <a-input
                          v-model:value="form.deliveryTerms"
                          placeholder="如：送货上门 / 自提"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="付款天数">
                        <a-input-number
                          v-model:value="form.paymentDays"
                          :min="0"
                          :disabled="!editable"
                          style="width: 100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="交货天数">
                        <a-input-number
                          v-model:value="form.deliveryDays"
                          :min="0"
                          :disabled="!editable"
                          style="width: 100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="交货地址">
                        <a-input
                          v-model:value="form.deliveryAddress"
                          placeholder="请输入交货地址"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="收货人">
                        <a-input
                          v-model:value="form.receiverName"
                          placeholder="请输入收货人"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="收货电话">
                        <a-input
                          v-model:value="form.receiverPhone"
                          placeholder="请输入收货电话"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="内部备注">
                        <a-input
                          v-model:value="form.internalNote"
                          placeholder="仅内部可见"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="12">
                      <a-form-item label="备注">
                        <a-input
                          v-model:value="form.remark"
                          placeholder="对客备注"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <!-- 报价明细（BillDetailTable 可编辑；字段与 QuotationItemDTO 逐字对齐） -->
              <a-card
                title="报价明细"
                size="small"
                class="form-section"
              >
                <div class="items-wrap">
                  <BillDetailTable
                    :data-source="pagedItems"
                    :columns="itemColumns"
                    :loading="pageLoading"
                    :min-rows="0"
                    :row-key="'__key'"
                    :summary-columns="itemSummaryColumns"
                    storage-key="crm-quotation-form-item-columns"
                    global-config-key="crm-quotation-form-item-columns"
                  >
                    <template #productCell="{ record }">
                      <a-select
                        v-if="editable"
                        :value="record.productId"
                        placeholder="选择商品（可搜索）"
                        show-search
                        allow-clear
                        :filter-option="filterProductOption"
                        :options="productOptions"
                        :loading="productLoading"
                        size="small"
                        style="width: 100%"
                        @search="handleProductSearch"
                        @change="(val: any) => onProductSelect(record, val)"
                      />
                      <span v-else>{{ record.productName || '-' }}</span>
                    </template>
                    <template #productSpecCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.productSpec"
                        placeholder="规格型号"
                        size="small"
                      />
                      <span v-else>{{ record.productSpec || '-' }}</span>
                    </template>
                    <template #quantityCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.quantity"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 90px"
                        @change="() => recalcItem(record)"
                      />
                      <span v-else>{{ record.quantity }}</span>
                    </template>
                    <template #productUnitCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.productUnit"
                        placeholder="单位"
                        size="small"
                        style="width: 70px"
                      />
                      <span v-else>{{ record.productUnit || '-' }}</span>
                    </template>
                    <template #unitPriceCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.unitPrice"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 100px"
                        @change="() => recalcItem(record)"
                      />
                      <span v-else>{{ formatAmount(record.unitPrice) }}</span>
                    </template>
                    <template #discountRateCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.discountRate"
                        :min="0"
                        :max="100"
                        :precision="2"
                        size="small"
                        style="width: 80px"
                        @change="() => recalcItem(record)"
                      />
                      <span v-else>{{ record.discountRate || 0 }}</span>
                    </template>
                    <template #taxRateCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.taxRate"
                        :min="0"
                        :max="100"
                        :precision="2"
                        size="small"
                        style="width: 80px"
                        @change="() => recalcItem(record)"
                      />
                      <span v-else>{{ record.taxRate || 0 }}</span>
                    </template>
                    <template #lineAmountCell="{ record }">
                      <span class="mono">{{ formatAmount(record.lineAmount) }}</span>
                    </template>
                    <template #lineTotalCell="{ record }">
                      <span class="mono amount-red">{{ formatAmount(record.lineTotal) }}</span>
                    </template>
                    <template #deliveryDaysCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.deliveryDays"
                        :min="0"
                        size="small"
                        style="width: 80px"
                      />
                      <span v-else>{{ record.deliveryDays ?? '-' }}</span>
                    </template>
                    <template #descriptionCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.description"
                        placeholder="行说明"
                        size="small"
                      />
                      <span v-else>{{ record.description || '-' }}</span>
                    </template>
                    <template #itemActionCell="{ record }">
                      <a-button
                        v-if="editable"
                        type="link"
                        danger
                        size="small"
                        @click="removeItem(record)"
                      >
                        删除
                      </a-button>
                      <span v-else>-</span>
                    </template>
                  </BillDetailTable>
                </div>
                <a-button
                  v-if="editable"
                  type="dashed"
                  block
                  style="margin-top: 12px"
                  @click="addItem"
                >
                  <template #icon>
                    <PlusOutlined />
                  </template>
                  添加明细行
                </a-button>
              </a-card>

              <!-- 费用汇总（口径与后端 calculateLineAmount / calculateAmounts 一致） -->
              <a-card
                title="费用汇总"
                size="small"
                class="form-section"
              >
                <a-row :gutter="24">
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">产品金额（未扣折扣）</span>
                      <span class="summary-value">{{ formatAmount(calcTotalAmount()) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">折扣金额</span>
                      <span class="summary-value discount">{{ formatAmount(calcDiscountAmount()) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">税额</span>
                      <span class="summary-value">{{ formatAmount(calcTaxAmount()) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item total">
                      <span class="summary-label">报价总额（final_amount）</span>
                      <span class="summary-value amount-red">{{ formatAmount(calcFinalAmount()) }}</span>
                    </div>
                  </a-col>
                </a-row>
              </a-card>

              <!-- 审批与流转（数据源为 QuotationVO 的真实时间戳字段；VO 无 approvalRecords） -->
              <a-card
                title="审批与流转"
                size="small"
                class="form-section"
              >
                <a-timeline v-if="flowSteps.length">
                  <a-timeline-item
                    v-for="(step, idx) in flowSteps"
                    :key="idx"
                    :color="step.color"
                  >
                    <div class="flow-step">
                      <div class="flow-step-title">{{ step.title }}</div>
                      <div
                        v-if="step.desc"
                        class="flow-step-desc"
                      >
                        {{ step.desc }}
                      </div>
                      <div class="flow-step-time">{{ step.time }}</div>
                    </div>
                  </a-timeline-item>
                </a-timeline>
                <a-empty
                  v-else
                  description="尚未产生流转记录"
                />
              </a-card>
            </div>
          </div>
        </template>

        <!-- ═══ 经典分页栏（明细分页） ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="itemPagination.current"
            :page-size="itemPagination.pageSize"
            :total="filteredItems.length"
            :page-size-options="[20, 50, 100]"
            @change="handleItemPageChange"
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
        storage-key="crm-quotation-form-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 发送方式（后端 POST /{id}/send 的 method 为必填） ═══ -->
      <a-modal
        v-model:open="sendVisible"
        title="发送报价"
        :confirm-loading="sendLoading"
        ok-text="确认发送"
        cancel-text="取消"
        :width="420"
        @ok="handleSendConfirm"
      >
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

      <!-- ═══ 原因输入（审批拒绝 / 客户拒绝 / 取消） ═══ -->
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

      <!-- ═══ 版本历史 ═══ -->
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
                  @click="handleLoadVersion(record)"
                >
                  载入
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
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import { quotationApi } from '@/api/erp'
import { crmCustomerApi, opportunityApi } from '@/api/crm'
import { productApi } from '@/api/erp/product'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmQuotationForm' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 后端已就绪但 api/erp.ts 的 quotationApi 未封装（本任务禁止改 api/ 目录，缺口已在报告登记）。
 * 全部直连 QuotationController，无造假数据。
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
  versions(id: any) { return request.get(`/crm/quotation/${id}/versions`) },
  newVersion(id: any) { return request.post(`/crm/quotation/${id}/new-version`) },
}

// ═══ 字典（与后端 QuotationStatus / QuotationType / SentMethod 对齐） ═══
const STATUS_TEXT: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '已发送', 4: '已接受',
  5: '已拒绝', 6: '已过期', 7: '已转订单', 8: '已取消',
}
const STATUS_COLOR: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'cyan', 3: 'blue', 4: 'green',
  5: 'red', 6: 'orange', 7: 'purple', 8: 'default',
}
const QUOTATION_TYPE_OPTIONS = [
  { label: '标准报价', value: 1 },
  { label: '项目报价', value: 2 },
  { label: '招标报价', value: 3 },
  { label: '重复报价', value: 4 },
  { label: '特殊报价', value: 5 },
]
const CURRENCY_OPTIONS = [
  { label: '人民币(CNY)', value: 'CNY' },
  { label: '美元(USD)', value: 'USD' },
  { label: '欧元(EUR)', value: 'EUR' },
]
/**
 * 发送方式：DB 列 sent_method 为 varchar（存量值 'email'），SentMethod 枚举却是 Integer 1-6。
 * 写入口径统一为字符串小写（与列类型/存量数据一致），读取侧兼容小写/中文/数字。
 */
const SENT_METHOD_OPTIONS = [
  { label: '邮件', value: 'email' },
  { label: '微信', value: 'wechat' },
  { label: '短信', value: 'sms' },
  { label: '传真', value: 'fax' },
  { label: '人工送达', value: 'hand_delivery' },
  { label: '在线查看', value: 'online' },
]

function getStatusText(status: any): string {
  const n = Number(status)
  return STATUS_TEXT[n] ?? (status == null || status === '' ? '草稿' : String(status))
}
function getStatusColor(status: any): string {
  return STATUS_COLOR[Number(status)] || 'default'
}
function getSentMethodText(method: any): string {
  if (method == null || method === '') return ''
  const raw = String(method)
  const matched = SENT_METHOD_OPTIONS.find(o => o.value === raw.toLowerCase() || o.label === raw)
  return matched ? matched.label : raw
}
function formatAmount(v: any): string {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'itemKeyword', label: '明细筛选', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存', enabled: true },
  { key: 'submit', label: '提交审批', enabled: true },
  { key: 'more', label: '状态操作', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
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

// ═══ 表单数据（key 与 QuotationCreateDTO 逐字对齐） ═══
const formRef = ref<FormInstance>()
const saving = ref(false)
const pageLoading = ref(false)
const form = reactive<Record<string, any>>({
  id: undefined,
  quotationNo: '',
  title: '',
  customerId: undefined,
  customerName: '',
  opportunityId: undefined,
  opportunityName: '',
  contactName: '',
  quotationDate: dayjs().format('YYYY-MM-DD'),
  validDays: 30,
  quotationType: 1,
  currency: 'CNY',
  salesPersonId: userStore.userId || undefined,
  salesPersonName: userStore.nickname || userStore.username || '',
  paymentTerms: '',
  paymentDays: undefined,
  deliveryTerms: '',
  deliveryDays: undefined,
  deliveryAddress: '',
  receiverName: '',
  receiverPhone: '',
  internalNote: '',
  remark: '',
  winProbability: undefined,
  status: 0,
  createTime: '',
  updateTime: '',
  approvedTime: '',
  approvedNote: '',
  rejectedTime: '',
  rejectedReason: '',
  sentTime: '',
  sentMethod: '',
  acceptedTime: '',
  acceptedNote: '',
  convertedTime: '',
  orderNo: '',
  items: [] as any[],
})

const formRules: Record<string, any> = {
  title: [{ required: true, message: '请输入报价名称', trigger: 'blur' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  quotationDate: [{ required: true, message: '请选择报价日期', trigger: 'change' }],
}

/** 只有草稿可编辑（后端 update/addItem/... 一律要求 status == 0） */
const editable = computed(() => !form.id || Number(form.status) === 0)

/** 有效期至 = 报价日期 + 有效天数（后端列 valid_from / valid_to，无 valid_days 列） */
const computedValidTo = computed(() => {
  if (!form.quotationDate || form.validDays == null || form.validDays === '') return ''
  return dayjs(form.quotationDate).add(Number(form.validDays), 'day').format('YYYY-MM-DD')
})

// ═══ 下拉数据源（全部真实接口） ═══
const customerOptions = ref<any[]>([])
const customerLoading = ref(false)
const opportunityOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const productLoading = ref(false)

function filterCustomerOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}
function filterProductOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function handleCustomerSearch(keyword: string) {
  customerLoading.value = true
  try {
    const list: any = await crmCustomerApi.dropdown(keyword || undefined)
    customerOptions.value = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
  } catch (e) {
    console.warn('[报价表单] 客户下拉搜索失败', e)
  } finally {
    customerLoading.value = false
  }
}

async function handleProductSearch(keyword: string) {
  productLoading.value = true
  try {
    const res: any = await productApi.page({ pageNum: 1, pageSize: 50, keyword: keyword || undefined })
    // 保留明细中已选中的商品，避免远端搜索结果里没有它时下拉退化成显示 id
    const selectedIds = new Set(
      form.items.map((i: any) => i.productId).filter((v: any) => v != null).map((v: any) => String(v)),
    )
    const kept = productOptions.value.filter(o => selectedIds.has(String(o.value)))
    const merged = new Map<string, any>()
    for (const o of [...kept, ...mapProducts(res?.records || [])]) merged.set(String(o.value), o)
    productOptions.value = [...merged.values()]
  } catch (e) {
    console.warn('[报价表单] 商品下拉搜索失败', e)
  } finally {
    productLoading.value = false
  }
}

/** 编辑回填时按明细自带的产品信息补出下拉项，保证显示商品名而不是 id */
function seedProductOptionsFromItems() {
  const merged = new Map<string, any>()
  for (const o of productOptions.value) merged.set(String(o.value), o)
  for (const i of form.items) {
    if (i.productId == null) continue
    const key = String(i.productId)
    if (merged.has(key)) continue
    merged.set(key, {
      label: `${i.productName || '商品'}${i.productSpec ? ` (${i.productSpec})` : ''}`,
      value: i.productId,
      productCode: i.productCode,
      productName: i.productName,
      productSpec: i.productSpec,
      productUnit: i.productUnit,
      unitPrice: i.unitPrice,
      costPrice: i.costPrice,
      taxRate: i.taxRate,
    })
  }
  productOptions.value = [...merged.values()]
}

function mapProducts(records: any[]) {
  return records.map((p: any) => ({
    label: `${p.productName}${p.spec ? ` (${p.spec})` : ''}`,
    value: p.id,
    // 选中后回填用（a-select 只读 label/value，多余字段无副作用）
    productCode: p.productCode,
    productName: p.productName,
    productSpec: p.spec,
    productUnit: p.unit,
    // 商品档案无 salePrice 字段（原实现读 product.salePrice → 单价恒 0）；标准售价优先
    unitPrice: p.standardPrice ?? p.retailPrice ?? 0,
    costPrice: p.costPrice,
    taxRate: p.taxRate,
  }))
}

async function loadCustomerOptions() {
  try {
    const res: any = await crmCustomerApi.page({ pageNum: 1, pageSize: 200 })
    customerOptions.value = (res?.records || []).map((c: any) => ({ label: c.customerName, value: c.id }))
  } catch (e) {
    console.warn('[报价表单] 客户下拉加载失败', e)
  }
}

async function loadOpportunityOptions() {
  try {
    const res: any = await opportunityApi.page({ pageNum: 1, pageSize: 200 })
    opportunityOptions.value = (res?.records || []).map((o: any) => ({ label: o.name, value: o.id }))
  } catch (e) {
    console.warn('[报价表单] 商机下拉加载失败', e)
  }
}

async function loadProductOptions() {
  try {
    const res: any = await productApi.page({ pageNum: 1, pageSize: 50 })
    productOptions.value = mapProducts(res?.records || [])
  } catch (e) {
    console.warn('[报价表单] 商品下拉加载失败', e)
  }
}

function onCustomerChange(customerId: any) {
  const found = customerOptions.value.find(o => String(o.value) === String(customerId))
  const record = (found || {}) as any
  form.customerName = record.label || ''
  // 联系人回填：只有初始 200 条分页带业务联系人字段（下拉搜索接口仅返回 id/name）
  if (!form.contactName && record.businessContact) form.contactName = record.businessContact
}

function onOpportunityChange(opportunityId: any) {
  const found = opportunityOptions.value.find(o => String(o.value) === String(opportunityId))
  form.opportunityName = found?.label || ''
}

// ═══ 明细 ═══
let itemKeySeed = 1
function nextItemKey(): string {
  itemKeySeed += 1
  return `item-${Date.now()}-${itemKeySeed}`
}

function createEmptyItem() {
  return {
    __key: nextItemKey(),
    id: undefined,
    productId: undefined,
    productCode: '',
    productName: '',
    productSpec: '',
    productUnit: '',
    quantity: 1,
    unitPrice: 0,
    costPrice: 0,
    discountRate: 0,
    taxRate: 0,
    lineAmount: 0,
    discountAmount: 0,
    taxAmount: 0,
    lineTotal: 0,
    deliveryDays: undefined,
    description: '',
  }
}

/** 与后端 calculateLineAmount 完全一致（HALF_UP 2 位） */
function round2(n: number): number {
  return Math.round((Number(n) || 0) * 100) / 100
}
function recalcItem(item: any) {
  const quantity = Number(item.quantity) || 0
  const unitPrice = Number(item.unitPrice) || 0
  const lineAmount = round2(quantity * unitPrice)
  const discountAmount = round2(lineAmount * (Number(item.discountRate) || 0) / 100)
  const taxAmount = round2((lineAmount - discountAmount) * (Number(item.taxRate) || 0) / 100)
  item.lineAmount = lineAmount
  item.discountAmount = discountAmount
  item.taxAmount = taxAmount
  item.lineTotal = round2(lineAmount - discountAmount + taxAmount)
}

function onProductSelect(record: any, productId: any) {
  const found = productOptions.value.find(o => String(o.value) === String(productId))
  if (!found) {
    record.productId = undefined
    return
  }
  record.productId = found.value
  record.productCode = found.productCode || ''
  record.productName = found.productName || ''
  record.productSpec = found.productSpec || ''
  record.productUnit = found.productUnit || ''
  record.unitPrice = Number(found.unitPrice) || 0
  record.costPrice = Number(found.costPrice) || 0
  if (found.taxRate != null && record.taxRate == null) record.taxRate = Number(found.taxRate) || 0
  recalcItem(record)
}

function addItem() {
  form.items.push(createEmptyItem())
  // 跳到最后一页，保证新增行可见
  itemPagination.current = Math.max(1, Math.ceil(form.items.length / itemPagination.pageSize))
}

function removeItem(record: any) {
  const idx = form.items.findIndex((i: any) => i.__key === record.__key)
  if (idx > -1) {
    form.items.splice(idx, 1)
  }
  if (!form.items.length && editable.value) form.items.push(createEmptyItem())
}

// 明细分页（客户端切片；组件内置分页不做切片，须由页面提供）
const itemPagination = reactive({ current: 1, pageSize: 20 })
const itemKeyword = ref('')

const filteredItems = computed(() => {
  const kw = itemKeyword.value.trim().toLowerCase()
  if (!kw) return form.items
  return form.items.filter((i: any) =>
    String(i.productName || '').toLowerCase().includes(kw)
    || String(i.productCode || '').toLowerCase().includes(kw)
    || String(i.productSpec || '').toLowerCase().includes(kw),
  )
})

const pagedItems = computed(() => {
  const start = (itemPagination.current - 1) * itemPagination.pageSize
  return filteredItems.value.slice(start, start + itemPagination.pageSize)
})

const totalQuantity = computed(() =>
  form.items.reduce((s: number, i: any) => s + (Number(i.quantity) || 0), 0),
)

function handleItemFilter() {
  itemPagination.current = 1
}
function resetItemFilter() {
  itemKeyword.value = ''
  itemPagination.current = 1
}
function handleItemPageChange(page: number, pageSize: number) {
  itemPagination.current = page
  itemPagination.pageSize = pageSize
}

const itemColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  // key 固定为 action：组件的 LOCKED_COLUMNS 只认 rowNo/checkbox/action，否则操作列会误进列配置面板
  { key: 'action', title: '操作', type: 'action', slotName: 'itemActionCell', width: 70, fixed: 'left' },
  { key: 'productName', title: '商品', type: 'slot', slotName: 'productCell', width: 230 },
  { key: 'productCode', title: '商品编码', type: 'input', width: 130, readonly: true },
  { key: 'productSpec', title: '规格型号', type: 'slot', slotName: 'productSpecCell', width: 130 },
  { key: 'quantity', title: '数量', type: 'slot', slotName: 'quantityCell', width: 100, align: 'right' },
  { key: 'productUnit', title: '单位', type: 'slot', slotName: 'productUnitCell', width: 90, align: 'center' },
  { key: 'unitPrice', title: '单价', type: 'slot', slotName: 'unitPriceCell', width: 110, align: 'right' },
  { key: 'discountRate', title: '折扣%', type: 'slot', slotName: 'discountRateCell', width: 90, align: 'right' },
  { key: 'taxRate', title: '税率%', type: 'slot', slotName: 'taxRateCell', width: 90, align: 'right' },
  { key: 'lineAmount', title: '行金额', type: 'slot', slotName: 'lineAmountCell', width: 110, align: 'right' },
  { key: 'lineTotal', title: '行小计', type: 'slot', slotName: 'lineTotalCell', width: 110, align: 'right' },
  { key: 'deliveryDays', title: '交期(天)', type: 'slot', slotName: 'deliveryDaysCell', width: 100, align: 'right' },
  { key: 'description', title: '行说明', type: 'slot', slotName: 'descriptionCell', width: 160 },
]

const itemSummaryColumns = computed(() => {
  const rows = form.items
  return [
    { key: 'quantity', value: rows.reduce((s: number, i: any) => s + (Number(i.quantity) || 0), 0) },
    { key: 'lineAmount', value: rows.reduce((s: number, i: any) => s + (Number(i.lineAmount) || 0), 0) },
    { key: 'lineTotal', value: rows.reduce((s: number, i: any) => s + (Number(i.lineTotal) || 0), 0), highlight: true },
  ]
})

// 费用汇总（口径与后端 total_amount / discount_amount / tax_amount / final_amount 一致）
function calcTotalAmount(): number {
  return round2(form.items.reduce((s: number, i: any) => s + (Number(i.lineAmount) || 0), 0))
}
function calcDiscountAmount(): number {
  return round2(form.items.reduce((s: number, i: any) => s + (Number(i.discountAmount) || 0), 0))
}
function calcTaxAmount(): number {
  return round2(form.items.reduce((s: number, i: any) => s + (Number(i.taxAmount) || 0), 0))
}
function calcFinalAmount(): number {
  return round2(calcTotalAmount() - calcDiscountAmount() + calcTaxAmount())
}

// ═══ 详情加载 ═══
function readRouteId(): string | undefined {
  const raw: any = route.params?.id ?? route.query?.id
  // 雪花 ID 一律按字符串处理，禁止 Number()
  return raw === undefined || raw === null || raw === '' ? undefined : String(raw)
}

async function loadDetail(id: string) {
  pageLoading.value = true
  try {
    // GET /{id} 不返回 items（VO 只给 itemCount），必须另调 GET /{id}/items
    const [vo, items] = await Promise.all([
      quotationApi.getById(id as any),
      quotationOps.getItems(id),
    ])
    const data: any = vo || {}
    form.id = data.id
    form.quotationNo = data.quotationNo || ''
    form.title = data.title || ''
    form.customerId = data.customerId
    form.customerName = data.customerName || ''
    form.opportunityId = data.opportunityId
    form.opportunityName = data.opportunityName || ''
    form.contactName = data.contactName || ''
    form.quotationDate = data.quotationDate || ''
    form.validDays = data.validTo && data.quotationDate
      ? Math.max(dayjs(data.validTo).diff(dayjs(data.quotationDate), 'day'), 0)
      : 30
    form.quotationType = data.quotationType ?? 1
    form.currency = data.currency || 'CNY'
    form.salesPersonId = data.salesPersonId
    form.salesPersonName = data.salesPersonName || ''
    form.paymentTerms = data.paymentTerms || ''
    form.paymentDays = data.paymentDays ?? undefined
    form.deliveryTerms = data.deliveryTerms || ''
    form.deliveryDays = data.deliveryDays ?? undefined
    form.deliveryAddress = data.deliveryAddress || ''
    form.receiverName = data.receiverName || ''
    form.receiverPhone = data.receiverPhone || ''
    form.internalNote = data.internalNote || ''
    form.remark = data.remark || ''
    form.winProbability = data.winProbability ?? undefined
    form.status = data.status ?? 0
    form.createTime = data.createTime || ''
    form.updateTime = data.updateTime || ''
    form.approvedTime = data.approvedTime || ''
    form.approvedNote = data.approvedNote || ''
    form.rejectedTime = data.rejectedTime || ''
    form.rejectedReason = data.rejectedReason || ''
    form.sentTime = data.sentTime || ''
    form.sentMethod = data.sentMethod || ''
    form.acceptedTime = data.acceptedTime || ''
    form.acceptedNote = data.acceptedNote || ''
    form.convertedTime = data.convertedTime || ''
    form.orderNo = data.orderNo || ''

    form.items = (Array.isArray(items) ? items : []).map((i: any) => {
      const item: any = {
        __key: `item-${i.id}`,
        id: i.id,
        productId: i.productId,
        productCode: i.productCode || '',
        productName: i.productName || '',
        productSpec: i.productSpec || '',
        productUnit: i.productUnit || '',
        quantity: Number(i.quantity) || 0,
        unitPrice: Number(i.unitPrice) || 0,
        costPrice: Number(i.costPrice) || 0,
        discountRate: Number(i.discountRate) || 0,
        taxRate: Number(i.taxRate) || 0,
        // 金额取库中实存值（后端 calculateLineAmount 的口径），用户改动任一字段时才重算
        lineAmount: Number(i.lineAmount) || 0,
        discountAmount: Number(i.discountAmount) || 0,
        taxAmount: Number(i.taxAmount) || 0,
        lineTotal: Number(i.lineTotal) || 0,
        deliveryDays: i.deliveryDays ?? undefined,
        description: i.description || '',
      }
      return item
    })
    if (!form.items.length && editable.value) form.items.push(createEmptyItem())
    seedProductOptionsFromItems()
    itemPagination.current = 1
  } catch (e) {
    console.error('[报价表单] 加载详情失败', e)
    message.error('加载报价信息失败')
  } finally {
    pageLoading.value = false
    // 回填完成后重置脏检查基线（含状态动作后的重新加载）
    saveSnapshot()
  }
}

async function fetchData() {
  const id = readRouteId()
  if (id) {
    await loadDetail(id)
    return
  }
  // 新建：单号由后端号段生成，前端不造演示号
  form.id = undefined
  form.quotationNo = ''
  form.status = 0
  if (!form.items.length) form.items.push(createEmptyItem())
}

// ═══ 保存 / 提交（字段名与后端 DTO 逐字对齐） ═══
function buildItemPayload(item: any) {
  return {
    productId: item.productId ?? undefined,
    productCode: item.productCode || undefined,
    productName: item.productName || undefined,
    productSpec: item.productSpec || undefined,
    productUnit: item.productUnit || undefined,
    quantity: Number(item.quantity) || 0,
    unitPrice: Number(item.unitPrice) || 0,
    costPrice: item.costPrice != null ? Number(item.costPrice) : undefined,
    discountRate: Number(item.discountRate) || 0,
    taxRate: Number(item.taxRate) || 0,
    deliveryDays: item.deliveryDays ?? undefined,
    description: item.description || undefined,
  }
}

function buildPayload() {
  const quotationDate = form.quotationDate
    ? dayjs(form.quotationDate).format('YYYY-MM-DD')
    : undefined
  const validTo = computedValidTo.value || undefined
  return {
    title: form.title || undefined,
    customerId: form.customerId,
    customerName: form.customerName || undefined,
    opportunityId: form.opportunityId ?? undefined,
    opportunityName: form.opportunityName || undefined,
    contactName: form.contactName || undefined,
    quotationDate,
    validFrom: quotationDate,
    validTo,
    quotationType: form.quotationType ?? undefined,
    currency: form.currency || undefined,
    salesPersonId: form.salesPersonId ?? undefined,
    salesPersonName: form.salesPersonName || undefined,
    paymentTerms: form.paymentTerms || undefined,
    paymentDays: form.paymentDays ?? undefined,
    deliveryTerms: form.deliveryTerms || undefined,
    deliveryDays: form.deliveryDays ?? undefined,
    deliveryAddress: form.deliveryAddress || undefined,
    receiverName: form.receiverName || undefined,
    receiverPhone: form.receiverPhone || undefined,
    internalNote: form.internalNote || undefined,
    remark: form.remark || undefined,
    winProbability: form.winProbability ?? undefined,
    items: form.items.map(buildItemPayload),
  }
}

/** 保存并返回单据 id（新建时后端生成号段与 id） */
async function doSave(): Promise<any> {
  await formRef.value?.validate()
  const payload = buildPayload()
  const res: any = form.id
    ? await quotationApi.update(form.id, payload)
    : await quotationApi.create(payload)
  if (res?.id) form.id = res.id
  if (res?.quotationNo) form.quotationNo = res.quotationNo
  if (res?.status !== undefined) form.status = res.status
  return res
}

/**
 * 写操作后同步：重新回填（含后端生成的号段/金额/状态）并补上 URL 上的 id，
 * 使刷新/返回不再当成新单。必须先 loadDetail（重置脏标记）再 replace，
 * 否则 onBeforeRouteLeave 的未保存确认会拦下这次路由替换。
 */
async function syncAfterWrite() {
  if (!form.id) return
  await loadDetail(String(form.id))
  if (!readRouteId()) {
    await router.replace({ path: '/crm/quotation/form', query: { id: String(form.id) } })
  }
}

async function handleSave() {
  saving.value = true
  try {
    await doSave()
    message.success('保存成功')
    await syncAfterWrite()
  } catch (e: any) {
    console.warn('[报价表单] 保存失败', e)
    if (e?.errorFields) message.warning('请完善必填项')
  } finally {
    saving.value = false
  }
}

async function handleSubmitApproval() {
  saving.value = true
  try {
    const res = await doSave()
    const id = res?.id ?? form.id
    if (!id) throw new Error('单据尚未保存，无法提交审批')
    await quotationOps.submit(id)
    message.success('已提交审批')
    await syncAfterWrite()
  } catch (e: any) {
    console.warn('[报价表单] 提交审批失败', e)
    if (e?.errorFields) message.warning('请完善必填项')
  } finally {
    saving.value = false
  }
}

// ═══ 状态机动作 ═══
const sendVisible = ref(false)
const sendLoading = ref(false)
const sendMethod = ref('email')

async function handleFlowAction(key: string) {
  switch (key) {
    case 'approve':
      Modal.confirm({
        title: '审批通过',
        content: '确定审批通过本报价单吗？',
        okText: '审批通过',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try {
            await quotationOps.approve(form.id)
            message.success('审批已通过')
            await loadDetail(String(form.id))
          } catch (e) {
            console.warn('[报价表单] 审批失败', e)
          }
        },
      })
      break
    case 'reject':
      openReasonModal('reject')
      break
    case 'send':
      sendMethod.value = 'email'
      sendVisible.value = true
      break
    case 'accept':
      Modal.confirm({
        title: '标记为已接受',
        content: '确定客户已接受本报价单吗？',
        okText: '确认',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try {
            await quotationOps.accept(form.id)
            message.success('已标记为已接受')
            await loadDetail(String(form.id))
          } catch (e) {
            console.warn('[报价表单] 标记已接受失败', e)
          }
        },
      })
      break
    case 'rejectByCustomer':
      openReasonModal('rejectByCustomer')
      break
    case 'convert':
      Modal.confirm({
        title: '确认转订单',
        content: '确定将本报价单转为销售订单吗？（后端将真实写入 erp_sale_order）',
        okText: '确认转换',
        cancelText: '取消',
        centered: true,
        onOk: async () => {
          try {
            await quotationApi.convertToOrder(form.id)
            message.success('报价已成功转为订单')
            await loadDetail(String(form.id))
          } catch (e) {
            console.warn('[报价表单] 转订单失败', e)
          }
        },
      })
      break
    case 'cancel':
      openReasonModal('cancel')
      break
    case 'newVersion':
      handleNewVersion()
      break
    case 'versions':
      openVersionDrawer()
      break
  }
}

async function handleSendConfirm() {
  if (!sendMethod.value) {
    message.warning('请选择发送方式')
    return
  }
  sendLoading.value = true
  try {
    await quotationOps.send(form.id, sendMethod.value)
    message.success('报价已发送')
    sendVisible.value = false
    await loadDetail(String(form.id))
  } catch (e) {
    console.warn('[报价表单] 发送失败', e)
  } finally {
    sendLoading.value = false
  }
}

const reasonVisible = ref(false)
const reasonLoading = ref(false)
const reasonText = ref('')
const reasonMode = ref<'reject' | 'rejectByCustomer' | 'cancel'>('reject')
const reasonTitle = computed(() => {
  if (reasonMode.value === 'reject') return '审批拒绝'
  if (reasonMode.value === 'rejectByCustomer') return '客户拒绝'
  return '取消报价'
})

function openReasonModal(mode: 'reject' | 'rejectByCustomer' | 'cancel') {
  reasonMode.value = mode
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
    if (reasonMode.value === 'reject') {
      await quotationOps.reject(form.id, reason)
      message.success('已拒绝')
    } else if (reasonMode.value === 'rejectByCustomer') {
      await quotationOps.rejectByCustomer(form.id, reason)
      message.success('已标记为客户拒绝')
    } else {
      await quotationOps.cancel(form.id, reason)
      message.success('报价已取消')
    }
    reasonVisible.value = false
    await loadDetail(String(form.id))
  } catch (e) {
    console.warn('[报价表单] 操作失败', e)
  } finally {
    reasonLoading.value = false
  }
}

// ═══ 版本 ═══
const versionVisible = ref(false)
const versionLoading = ref(false)
const versionList = ref<any[]>([])
const versionColumns = [
  { title: '版本', key: 'version', width: 90 },
  { title: '报价单号', dataIndex: 'quotationNo', key: 'quotationNo', width: 160 },
  { title: '报价名称', dataIndex: 'title', key: 'title' },
  { title: '状态', key: 'status', width: 110 },
  { title: '报价总额', dataIndex: 'finalAmount', key: 'finalAmount', width: 120 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 80 },
]

async function openVersionDrawer() {
  versionVisible.value = true
  await loadVersions()
}

async function loadVersions() {
  if (!form.id) return
  versionLoading.value = true
  try {
    const res: any = await quotationOps.versions(form.id)
    versionList.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.warn('[报价表单] 加载版本失败', e)
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
        const res: any = await quotationOps.newVersion(form.id)
        message.success('新版本已创建')
        versionVisible.value = false
        const newId = res?.id
        if (newId) {
          // 先回填（重置脏标记）再换 URL，避免被未保存确认拦截
          await loadDetail(String(newId))
          await router.replace({ path: '/crm/quotation/form', query: { id: String(newId) } })
        } else {
          await loadVersions()
        }
      } catch (e) {
        console.warn('[报价表单] 新建版本失败', e)
      }
    },
  })
}

async function handleLoadVersion(record: any) {
  versionVisible.value = false
  await loadDetail(String(record.id))
  await router.replace({ path: '/crm/quotation/form', query: { id: String(record.id) } })
}

// ═══ 流转时间线（数据源为 QuotationVO 的真实字段；VO 无 approvalRecords） ═══
const flowSteps = computed(() => {
  const steps: any[] = []
  if (form.createTime || form.quotationDate) {
    steps.push({ title: '创建报价单', desc: `报价日期 ${form.quotationDate || '-'}`, time: form.createTime || '', color: 'blue' })
  }
  if (form.approvedTime) {
    steps.push({ title: '审批通过', desc: form.approvedNote || '', time: form.approvedTime, color: 'green' })
  }
  if (form.rejectedTime) {
    steps.push({ title: '拒绝', desc: form.rejectedReason || '', time: form.rejectedTime, color: 'red' })
  }
  if (form.sentTime) {
    steps.push({ title: `已发送（${getSentMethodText(form.sentMethod) || '未记录方式'}）`, desc: '', time: form.sentTime, color: 'blue' })
  }
  if (form.acceptedTime) {
    steps.push({ title: '客户已接受', desc: form.acceptedNote || '', time: form.acceptedTime, color: 'green' })
  }
  if (form.convertedTime) {
    steps.push({ title: `已转销售订单${form.orderNo ? `（${form.orderNo}）` : ''}`, desc: '', time: form.convertedTime, color: 'purple' })
  }
  return steps
})

// ═══ 打印(F8) ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = form.items.map((i: any, index: number) => `
    <tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(i.productCode || '')}</td>
      <td>${escapeHtml(i.productName || '')}</td>
      <td>${escapeHtml(i.productSpec || '')}</td>
      <td>${escapeHtml(i.productUnit || '')}</td>
      <td style="text-align:right">${i.quantity ?? 0}</td>
      <td style="text-align:right">${formatAmount(i.unitPrice)}</td>
      <td style="text-align:right">${i.discountRate || 0}%</td>
      <td style="text-align:right">${formatAmount(i.lineTotal)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>报价单</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
      .sum{margin-top:8px;text-align:right;font-size:13px}
    </style></head><body>
    <h2>报价单</h2>
    <div class="meta">
      <span>报价单号：${escapeHtml(form.quotationNo || '（未保存）')}</span>
      <span>报价名称：${escapeHtml(form.title || '')}</span>
      <span>客户：${escapeHtml(form.customerName || '')}</span>
      <span>报价日期：${escapeHtml(form.quotationDate || '')}</span>
      <span>有效期至：${escapeHtml(computedValidTo.value || '')}</span>
      <span>状态：${escapeHtml(getStatusText(form.status))}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>商品编码</th><th>商品名称</th><th>规格型号</th><th>单位</th>
      <th>数量</th><th>单价</th><th>折扣%</th><th>行小计</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="sum">
      产品金额：¥${formatAmount(calcTotalAmount())}
      折扣金额：¥${formatAmount(calcDiscountAmount())}
      税额：¥${formatAmount(calcTaxAmount())}
      <strong>报价总额：¥${formatAmount(calcFinalAmount())}</strong>
    </div>
    </body></html>`
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

// ═══ 快捷键（原先 Ctrl+S / Ctrl+Enter 只是提示文字） ═══
function handleKeydown(e: KeyboardEvent) {
  if (!e.ctrlKey && !e.metaKey) return
  const key = e.key.toLowerCase()
  if (key === 's') {
    e.preventDefault()
    if (editable.value) handleSave()
  } else if (key === 'enter') {
    e.preventDefault()
    if (editable.value) handleSubmitApproval()
  }
}

// ═══ 导航 / 脏检查 ═══
const initialSnapshot = ref('')

function snapshot(): string {
  return JSON.stringify({
    ...form,
    items: form.items.map((i: any) => ({ ...i, __key: undefined })),
  })
}

const dirty = computed(() => {
  if (!editable.value) return false
  return !!initialSnapshot.value && snapshot() !== initialSnapshot.value
})

function saveSnapshot() {
  initialSnapshot.value = snapshot()
}

function handleBack() {
  if (dirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前修改尚未保存，确定离开吗？',
      okText: '离开',
      cancelText: '取消',
      centered: true,
      onOk: () => router.push('/crm/quotation'),
    })
    return
  }
  router.push('/crm/quotation')
}

onBeforeRouteLeave((_to, _from, next) => {
  if (dirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前修改尚未保存，确定离开吗？',
      okText: '离开',
      cancelText: '取消',
      centered: true,
      onOk: () => next(),
      onCancel: () => next(false),
    })
    return
  }
  next()
})

function handleError(error: Error) {
  console.error('[报价表单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([loadCustomerOptions(), loadOpportunityOptions(), loadProductOptions()])
    await fetchData()
  } finally {
    pageLoading.value = false
  }
  await nextTick()
  saveSnapshot()
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('keydown', handleF8Key)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('keydown', handleF8Key)
})

/** 明细行数变化后修正分页页码（删除/筛选后避免停在空页） */
watch(() => filteredItems.value.length, (len) => {
  const maxPage = Math.max(1, Math.ceil(len / itemPagination.pageSize))
  if (itemPagination.current > maxPage) itemPagination.current = maxPage
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex/无确定高度时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.form-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 16px 24px; }
.form-section { margin-bottom: 12px; }
.form-alert { margin-bottom: 12px; }
/* 明细区给确定高度：让 BillDetailTable 的 flex:1 有可分配空间（不传 :max-height） */
.items-wrap { height: 380px; display: flex; flex-direction: column; }

.version-toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.doc-no { font-size: 13px; color: #303133; }
.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}
.amount-red { color: #f5222d; font-weight: 500; }

.summary-item { display: flex; flex-direction: column; align-items: center; }
.summary-label { font-size: 13px; color: #666; }
.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.summary-value.discount { color: #faad14; }
.summary-item.total .summary-value { color: #f5222d; }

.flow-step-title { font-size: 14px; font-weight: 500; color: #303133; }
.flow-step-desc { font-size: 12px; color: #606266; margin-top: 4px; }
.flow-step-time { font-size: 12px; color: #909399; margin-top: 4px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
