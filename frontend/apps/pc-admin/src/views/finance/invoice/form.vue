<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        CRM 发票 · 单据表单页（菜单 70350 的主入口 path=crm/invoice/form）
        · CRM 为本系统独有模块（ql361 无 CRM 域）；后端不在 crm 模块，而是 ERP 财务 InvoiceController
          （@RequestMapping("/api/erp/invoice")，纯 Spring Data JPA，实体 85 列）
        · 注意：菜单点开**直接进本页**（与客户/线索/商机/合同相反），标签「历史」才到列表页
        · 路线 A 外壳：ErrorBoundary > PageContainer(full-height) > CategoryListLayout
        · 本轮修复：
          ① P0 字段契约：invoiceNo→invoiceNumber、remark→notes、taxId→customerTaxNumber、
             issuer→issuedByName、amount→subtotalAmount（原 amount 后端不存在，金额恒 0 默认值）
          ② P0 发票类型改后端 InvoiceType 12 个枚举 name（原 special/normal/electronic → Jackson 反序列化必 400）
          ③ P0 编辑走 PUT /erp/invoice/{id}（后端本轮已补，部分更新语义；原端点不存在 → 必 404）
          ④ P1 补齐实体已有但 UI 无入口的字段：dueDate / currencyCode / exchangeRate / taxType /
             discountAmount / shippingAmount / otherAmount / 往来方税号地址电话银行 / 单据号 / 付款条件
          ⑤ P1 客户下拉改真实接口（原 index 弹窗硬编码 3 条）；新增供应商下拉（supplierApi）
          ⑥ P1 Ctrl+S / F8 改真实监听（原只是提示文字）；删掉「提交」（后端无发票提交端点，名不副实）
          ⑦ P1 明细列名对齐 invoice_item（item_name / specification / unit / quantity / unit_price / tax_rate / tax_amount / amount）
        · 后端缺口（前端无法修，如实登记）：
          - 「新建」须由「发票申请单」生成（POST /create-from-application 要求 applicationId NOT NULL +
            必填 issuedBy/issuedByName），而发票申请**无任何页面** → 新建链路在架构上堵死
          - createInvoiceFromApplication 不写 InvoiceItem，InvoiceItemMapper 零调用方 → 明细永不落库
          - 发票 JPA 路径无 tenant_id / is_deleted 过滤
        · 因此本页：保存走真实端点（编辑可用）；明细只参与本页金额汇总，**不随 payload 提交**（避免
          Jackson 合并把 items 当关联写入而产生脏数据）
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：返回 / 单号 / 状态 / 保存 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              size="small"
              @click="handleBack"
            >
              返回列表
            </a-button>
            <span class="doc-no mono">{{ form.invoiceNumber || '（发票号码由后端号段生成）' }}</span>
            <a-tag :color="getStatusColor(form.invoiceStatus)">
              {{ getStatusText(form.invoiceStatus) }}
            </a-tag>
            <a-button
              v-if="isButtonEnabled('save')"
              size="small"
              type="primary"
              :loading="saving"
              :disabled="!editable"
              @click="handleSave"
            >
              保存<span class="shortcut-hint">Ctrl+S</span>
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：状态操作 / 刷新 / 打印 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-dropdown v-if="isButtonEnabled('flow') && form.id">
              <a-button
                size="small"
                :disabled="!form.id"
              >
                状态操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleFlowAction(key as string)">
                  <a-menu-item
                    v-if="canIssue"
                    key="issue"
                  >
                    开具（置为已生成）
                  </a-menu-item>
                  <a-menu-item
                    v-if="canSend"
                    key="send"
                  >
                    发送
                  </a-menu-item>
                  <a-menu-item
                    v-if="canVoid"
                    key="void"
                    danger
                  >
                    作废
                  </a-menu-item>
                  <a-menu-item
                    v-if="!canIssue && !canSend && !canVoid"
                    key="none"
                    disabled
                  >
                    当前状态无可执行操作
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
              <!-- 新建链路缺口（后端 create-from-application 需要发票申请单）如实提示，不造假数据 -->
              <a-alert
                v-if="!form.id"
                class="form-alert"
                type="info"
                show-icon
                message="新建发票须由「发票申请单」生成：后端 POST /erp/invoice/create-from-application 要求 applicationId（NOT NULL）、orderId 及 issuedBy/issuedByName，而系统内暂无发票申请页面。直接保存将返回后端错误，属已登记的后端缺口。"
              />
              <a-alert
                v-if="form.id && !editable"
                class="form-alert"
                type="warning"
                show-icon
                :message="`当前状态「${getStatusText(form.invoiceStatus)}」为终态（已取消/已冲红/已作废），不可修改。`"
              />

              <!-- 基本信息 -->
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
                      <a-form-item
                        label="发票号码"
                        name="invoiceNumber"
                      >
                        <a-input
                          v-model:value="form.invoiceNumber"
                          placeholder="请输入发票号码"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="发票类型"
                        name="invoiceType"
                      >
                        <a-select
                          v-model:value="form.invoiceType"
                          placeholder="请选择发票类型"
                          :options="invoiceTypeOptions"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="发票方向">
                        <!-- 后端 invoice 表**无 direction 列**（方向由 invoice_type 反推），故此单选仅用于
                             界面分组：筛选发票类型候选 + 决定必填「客户」还是「供应商」，不随 payload 提交 -->
                        <a-radio-group
                          v-model:value="form.direction"
                          size="small"
                          :disabled="!editable"
                          @change="onDirectionChange"
                        >
                          <a-radio value="sales">
                            销售发票
                          </a-radio>
                          <a-radio value="purchase">
                            采购发票
                          </a-radio>
                        </a-radio-group>
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item
                        label="开票日期"
                        name="invoiceDate"
                      >
                        <a-date-picker
                          v-model:value="form.invoiceDate"
                          value-format="YYYY-MM-DD"
                          style="width: 100%"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item
                        label="应收到期日"
                        name="dueDate"
                      >
                        <a-date-picker
                          v-model:value="form.dueDate"
                          value-format="YYYY-MM-DD"
                          style="width: 100%"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="开票人">
                        <a-input
                          v-model:value="form.issuedByName"
                          placeholder="默认取当前登录用户"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="币种">
                        <a-select
                          v-model:value="form.currencyCode"
                          :options="CURRENCY_OPTIONS"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="汇率">
                        <a-input-number
                          v-model:value="form.exchangeRate"
                          :min="0"
                          :precision="6"
                          :disabled="!editable"
                          style="width: 100%"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="税种/税类型">
                        <a-input
                          v-model:value="form.taxType"
                          placeholder="如：增值税"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <!-- 往来方（销售发票取客户；采购发票取供应商；字段与实体 customer_* / supplier_* 六件套一致） -->
              <a-card
                title="往来方"
                size="small"
                class="form-section"
              >
                <a-form
                  :model="form"
                  :rules="formRules"
                  :label-col="{ span: 6 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <template v-if="form.direction === 'purchase'">
                    <a-row :gutter="24">
                      <a-col :span="8">
                        <a-form-item
                          label="供应商"
                          name="supplierId"
                        >
                          <a-select
                            v-model:value="form.supplierId"
                            placeholder="请选择供应商"
                            show-search
                            allow-clear
                            :filter-option="filterOption"
                            :options="supplierOptions"
                            :loading="supplierLoading"
                            :disabled="!editable"
                            size="small"
                            @search="handleSupplierSearch"
                            @change="onSupplierChange"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="供应商税号">
                          <a-input
                            v-model:value="form.supplierTaxNumber"
                            placeholder="纳税人识别号"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="供应商电话">
                          <a-input
                            v-model:value="form.supplierPhone"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                    <a-row :gutter="24">
                      <a-col :span="8">
                        <a-form-item label="供应商地址">
                          <a-input
                            v-model:value="form.supplierAddress"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="供应商银行账号">
                          <a-input
                            v-model:value="form.supplierBankAccount"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </template>
                  <template v-else>
                    <a-row :gutter="24">
                      <a-col :span="8">
                        <a-form-item
                          label="客户"
                          name="customerId"
                        >
                          <a-select
                            v-model:value="form.customerId"
                            placeholder="请选择客户"
                            show-search
                            allow-clear
                            :filter-option="filterOption"
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
                        <a-form-item label="客户税号">
                          <a-input
                            v-model:value="form.customerTaxNumber"
                            placeholder="纳税人识别号"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="客户电话">
                          <a-input
                            v-model:value="form.customerPhone"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                    <a-row :gutter="24">
                      <a-col :span="8">
                        <a-form-item label="客户地址">
                          <a-input
                            v-model:value="form.customerAddress"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                      <a-col :span="8">
                        <a-form-item label="客户银行账号">
                          <a-input
                            v-model:value="form.customerBankAccount"
                            :disabled="!editable"
                            size="small"
                          />
                        </a-form-item>
                      </a-col>
                    </a-row>
                  </template>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="发票抬头">
                        <!-- 实体无独立抬头列，抬头即往来方名称快照（customer_name / supplier_name） -->
                        <a-input
                          :value="form.direction === 'purchase' ? form.supplierName : form.customerName"
                          placeholder="选择往来方后自动带出"
                          disabled
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <!-- 凭据与条款（全部为实体已有列） -->
              <a-card
                title="凭据与条款"
                size="small"
                class="form-section"
              >
                <a-form
                  :model="form"
                  :label-col="{ span: 6 }"
                  :wrapper-col="{ span: 16 }"
                >
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="订单编号">
                        <a-input
                          v-model:value="form.orderNumber"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="合同编号">
                        <a-input
                          v-model:value="form.contractNumber"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="发货单号">
                        <a-input
                          v-model:value="form.deliveryNumber"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="8">
                      <a-form-item label="项目编号">
                        <a-input
                          v-model:value="form.projectCode"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="业务区域">
                        <a-input
                          v-model:value="form.businessRegion"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                    <a-col :span="8">
                      <a-form-item label="业务部门">
                        <a-input
                          v-model:value="form.businessDepartment"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-row :gutter="24">
                    <a-col :span="12">
                      <a-form-item label="付款条件">
                        <a-textarea
                          v-model:value="form.paymentTerms"
                          placeholder="如：款到发货 / 月结 30 天"
                          :rows="2"
                          :disabled="!editable"
                          size="small"
                        />
                      </a-form-item>
                    </a-col>
                  </a-row>
                </a-form>
              </a-card>

              <!-- 发票明细（列名与 invoice_item 逐字对齐） -->
              <a-card
                title="发票明细"
                size="small"
                class="form-section"
              >
                <!-- 后端 createInvoiceFromApplication 不写 InvoiceItem、InvoiceItemMapper 零调用方 → 明细不落库。
                     故明细仅参与本页金额汇总，且**不随保存 payload 提交**（避免 Jackson 合并误写关联）。 -->
                <a-alert
                  class="form-alert"
                  type="info"
                  show-icon
                  message="后端开票链路未实现发票明细写入（invoice_item 无写入路径）：明细仅参与本页金额汇总，不会随保存落库。"
                />
                <div class="items-wrap">
                  <BillDetailTable
                    :data-source="pagedItems"
                    :columns="itemColumns"
                    :loading="pageLoading"
                    :min-rows="0"
                    :row-key="'__key'"
                    :summary-columns="itemSummaryColumns"
                    storage-key="crm-invoice-form-item-columns"
                    global-config-key="crm-invoice-form-item-columns"
                  >
                    <template #itemNameCell="{ record }">
                      <a-select
                        v-if="editable"
                        :value="record.productId"
                        placeholder="选择商品（可搜索）"
                        show-search
                        allow-clear
                        :filter-option="filterOption"
                        :options="productOptions"
                        :loading="productLoading"
                        size="small"
                        style="width: 100%"
                        @search="handleProductSearch"
                        @change="(val: any) => onProductSelect(record, val)"
                      />
                      <span v-else>{{ record.itemName || '-' }}</span>
                    </template>
                    <template #itemCodeCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.itemCode"
                        placeholder="商品编码"
                        size="small"
                      />
                      <span v-else>{{ record.itemCode || '-' }}</span>
                    </template>
                    <template #specificationCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.specification"
                        placeholder="规格型号"
                        size="small"
                      />
                      <span v-else>{{ record.specification || '-' }}</span>
                    </template>
                    <template #unitCell="{ record }">
                      <a-input
                        v-if="editable"
                        v-model:value="record.unit"
                        placeholder="单位"
                        size="small"
                        style="width: 70px"
                      />
                      <span v-else>{{ record.unit || '-' }}</span>
                    </template>
                    <template #quantityCell="{ record }">
                      <a-input-number
                        v-if="editable"
                        v-model:value="record.quantity"
                        :min="0"
                        :precision="3"
                        size="small"
                        style="width: 90px"
                        @change="() => recalcItem(record)"
                      />
                      <span v-else>{{ record.quantity }}</span>
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
                    <template #itemAmountCell="{ record }">
                      <span class="mono">{{ formatAmount(record.amount) }}</span>
                    </template>
                    <template #itemTaxAmountCell="{ record }">
                      <span class="mono amount-red">{{ formatAmount(record.taxAmount) }}</span>
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

              <!-- 金额汇总（口径与后端 calculateNetAmount / calculateTotalAmount 一致：
                   net = subtotal − discount + shipping + other；total = net + tax） -->
              <a-card
                title="金额汇总"
                size="small"
                class="form-section"
              >
                <a-row :gutter="24">
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">不含税金额</span>
                      <a-input-number
                        v-if="editable && !hasItems"
                        v-model:value="form.subtotalAmount"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 140px"
                      />
                      <span
                        v-else
                        class="summary-value"
                      >¥{{ formatAmount(effectiveSubtotal) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">税额</span>
                      <a-input-number
                        v-if="editable && !hasItems"
                        v-model:value="form.taxAmount"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 140px"
                      />
                      <span
                        v-else
                        class="summary-value"
                      >¥{{ formatAmount(effectiveTax) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">折扣金额</span>
                      <a-input-number
                        v-model:value="form.discountAmount"
                        :min="0"
                        :precision="2"
                        :disabled="!editable"
                        size="small"
                        style="width: 140px"
                      />
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">运费</span>
                      <a-input-number
                        v-model:value="form.shippingAmount"
                        :min="0"
                        :precision="2"
                        :disabled="!editable"
                        size="small"
                        style="width: 140px"
                      />
                    </div>
                  </a-col>
                </a-row>
                <a-row
                  :gutter="24"
                  style="margin-top: 12px"
                >
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">其他费用</span>
                      <a-input-number
                        v-model:value="form.otherAmount"
                        :min="0"
                        :precision="2"
                        :disabled="!editable"
                        size="small"
                        style="width: 140px"
                      />
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item total">
                      <span class="summary-label">价税合计</span>
                      <span class="summary-value amount-red">¥{{ formatAmount(effectiveTotal) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">已付金额</span>
                      <span class="summary-value">¥{{ formatAmount(form.paidAmount) }}</span>
                    </div>
                  </a-col>
                  <a-col :span="6">
                    <div class="summary-item">
                      <span class="summary-label">未付金额</span>
                      <span class="summary-value">¥{{ formatAmount(form.unpaidAmount) }}</span>
                    </div>
                  </a-col>
                </a-row>
              </a-card>

              <!-- 备注 -->
              <a-card
                title="备注"
                size="small"
                class="form-section"
              >
                <a-textarea
                  v-model:value="form.notes"
                  placeholder="请输入备注"
                  :rows="3"
                  :disabled="!editable"
                  size="small"
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
        storage-key="crm-invoice-form-page-config"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 发送方式（后端 POST /{id}/send 的 sendMethod + sentBy 均必填） ═══ -->
      <a-modal
        v-model:open="sendVisible"
        title="发送发票"
        :confirm-loading="flowLoading"
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
              :options="SEND_METHOD_OPTIONS"
              placeholder="请选择发送方式"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 作废原因（后端 POST /{id}/void 的 reason + voidedBy 均必填） ═══ -->
      <a-modal
        v-model:open="voidVisible"
        title="发票作废"
        :confirm-loading="flowLoading"
        ok-text="确认作废"
        ok-type="danger"
        cancel-text="取消"
        :width="460"
        @ok="handleVoidConfirm"
      >
        <a-textarea
          v-model:value="voidReason"
          :rows="3"
          placeholder="请填写作废原因（后端必填）"
        />
      </a-modal>
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
import { invoiceApi, crmCustomerApi } from '@/api/crm'
import { productApi } from '@/api/erp/product'
import { supplierApi } from '@/api/supplier'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  SettingOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'

defineOptions({ name: 'CrmInvoiceForm' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ═══ 常量 / 字典（与后端 InvoiceType、InvoiceStatus、PaymentStatus 枚举逐字对齐） ═══
const SALES_TYPES = [
  'SALES_INVOICE', 'TAX_INVOICE', 'ELECTRONIC_INVOICE', 'REGULAR_INVOICE',
  'SPECIAL_INVOICE', 'VEHICLE_INVOICE', 'USED_VEHICLE_INVOICE', 'EXPORT_INVOICE',
]
const PURCHASE_TYPES = ['PURCHASE_INVOICE', 'IMPORT_INVOICE']
const INVOICE_TYPE_TEXT: Record<string, string> = {
  SALES_INVOICE: '销售发票',
  PURCHASE_INVOICE: '采购发票',
  PROFORMA_INVOICE: '形式发票',
  TAX_INVOICE: '税务发票',
  CREDIT_NOTE: '红字发票',
  ELECTRONIC_INVOICE: '电子发票',
  REGULAR_INVOICE: '普通发票',
  SPECIAL_INVOICE: '专用发票',
  VEHICLE_INVOICE: '机动车发票',
  USED_VEHICLE_INVOICE: '二手车发票',
  EXPORT_INVOICE: '出口发票',
  IMPORT_INVOICE: '进口发票',
}
const STATUS_TEXT: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  IN_APPROVAL: '审批中',
  APPROVED: '已批准',
  REJECTED: '已拒绝',
  GENERATED: '已生成',
  SENT: '已发送',
  PARTIALLY_PAID: '部分支付',
  PAID: '已支付',
  OVERDUE: '逾期',
  CANCELLED: '已取消',
  CREDITED: '已冲红',
  VOIDED: '已作废',
}
const STATUS_COLOR: Record<string, string> = {
  DRAFT: 'default',
  SUBMITTED: 'orange',
  IN_APPROVAL: 'processing',
  APPROVED: 'cyan',
  REJECTED: 'red',
  GENERATED: 'blue',
  SENT: 'cyan',
  PARTIALLY_PAID: 'geekblue',
  PAID: 'green',
  OVERDUE: 'red',
  CANCELLED: 'default',
  CREDITED: 'magenta',
  VOIDED: 'default',
}
const CURRENCY_OPTIONS = [
  { label: '人民币(CNY)', value: 'CNY' },
  { label: '美元(USD)', value: 'USD' },
  { label: '欧元(EUR)', value: 'EUR' },
]
/** 发送方式：后端 send_method 为 varchar，取值域 EMAIL/SMS/POSTAL/PORTAL */
const SEND_METHOD_OPTIONS = [
  { label: '邮件', value: 'EMAIL' },
  { label: '短信', value: 'SMS' },
  { label: '邮寄', value: 'POSTAL' },
  { label: '门户', value: 'PORTAL' },
]
const TERMINAL_STATUSES = ['CANCELLED', 'CREDITED', 'VOIDED']

function getStatusText(status: any): string {
  if (status === null || status === undefined || status === '') return '草稿'
  return STATUS_TEXT[String(status)] || String(status)
}
function getStatusColor(status: any): string {
  return STATUS_COLOR[String(status)] || 'default'
}
function formatAmount(v: any): string {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function round2(n: number): number {
  return Math.round((Number(n) || 0) * 100) / 100
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'itemKeyword', label: '明细筛选', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存', enabled: true },
  { key: 'flow', label: '状态操作', enabled: true },
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

// ═══ 表单数据（key 与 Invoice 实体字段逐字对齐；不提交 items/direction） ═══
const formRef = ref<FormInstance>()
const saving = ref(false)
const pageLoading = ref(false)
const flowLoading = ref(false)

const form = reactive<Record<string, any>>({
  id: undefined,
  invoiceNumber: '',
  invoiceType: undefined as string | undefined,
  direction: 'sales',
  invoiceDate: dayjs().format('YYYY-MM-DD'),
  dueDate: dayjs().add(30, 'day').format('YYYY-MM-DD'),
  issuedByName: '',
  currencyCode: 'CNY',
  exchangeRate: 1,
  taxType: '',
  customerId: undefined,
  customerName: '',
  customerTaxNumber: '',
  customerAddress: '',
  customerPhone: '',
  customerBankAccount: '',
  supplierId: undefined,
  supplierName: '',
  supplierTaxNumber: '',
  supplierAddress: '',
  supplierPhone: '',
  supplierBankAccount: '',
  orderNumber: '',
  contractNumber: '',
  deliveryNumber: '',
  projectCode: '',
  businessRegion: '',
  businessDepartment: '',
  paymentTerms: '',
  subtotalAmount: 0,
  taxAmount: 0,
  discountAmount: 0,
  shippingAmount: 0,
  otherAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0,
  invoiceStatus: 'GENERATED',
  // 明细仅本页使用，不提交
  items: [] as any[],
})

const formRules = computed(() => {
  const rules: Record<string, any> = {
    invoiceNumber: [{ required: true, message: '请输入发票号码' }],
    invoiceType: [{ required: true, message: '请选择发票类型' }],
    invoiceDate: [{ required: true, message: '请选择开票日期' }],
    dueDate: [{ required: true, message: '请选择应收到期日' }],
  }
  // 销售发票必须有客户、采购发票必须有供应商（与 Invoice.validate() 一致）
  if (form.direction === 'purchase') {
    rules.supplierId = [{ required: true, message: '请选择供应商' }]
  } else {
    rules.customerId = [{ required: true, message: '请选择客户' }]
  }
  return rules
})

const invoiceTypeOptions = computed(() => {
  const keys = form.direction === 'purchase' ? PURCHASE_TYPES : SALES_TYPES
  const list = form.invoiceType && !keys.includes(form.invoiceType) ? [...keys, form.invoiceType] : keys
  return list.map(k => ({ label: INVOICE_TYPE_TEXT[k] || k, value: k }))
})

/** 终态不可改（后端 PUT /{id} 为部分更新且无状态守卫，前端只屏蔽已取消/已冲红/已作废） */
const editable = computed(() => !form.id || !TERMINAL_STATUSES.includes(String(form.invoiceStatus)))

// ═══ 金额口径（与后端 calculateNetAmount / calculateTotalAmount 一致） ═══
const hasItems = computed(() => form.items.length > 0)
const itemsSubtotal = computed(() => round2(form.items.reduce((s: number, i: any) => s + (Number(i.amount) || 0), 0)))
const itemsTax = computed(() => round2(form.items.reduce((s: number, i: any) => s + (Number(i.taxAmount) || 0), 0)))
const effectiveSubtotal = computed(() => (hasItems.value ? itemsSubtotal.value : Number(form.subtotalAmount) || 0))
const effectiveTax = computed(() => (hasItems.value ? itemsTax.value : Number(form.taxAmount) || 0))
const effectiveTotal = computed(() => round2(
  effectiveSubtotal.value
  - (Number(form.discountAmount) || 0)
  + (Number(form.shippingAmount) || 0)
  + (Number(form.otherAmount) || 0)
  + effectiveTax.value,
))

// ═══ 下拉数据源（全部真实接口，无硬编码假数据） ═══
const customerOptions = ref<any[]>([])
const customerLoading = ref(false)
const supplierOptions = ref<any[]>([])
const supplierLoading = ref(false)
const productOptions = ref<any[]>([])
const productLoading = ref(false)

function filterOption(input: string, option: any): boolean {
  return String(option?.label ?? '').toLowerCase().includes(String(input).toLowerCase())
}

async function loadCustomerOptions() {
  try {
    const res: any = await crmCustomerApi.page({ pageNum: 1, pageSize: 200 })
    customerOptions.value = (res?.records || []).map((c: any) => ({ label: c.customerName || c.name, value: c.id }))
  } catch (e) {
    console.warn('[发票表单] 客户下拉加载失败', e)
    customerOptions.value = []
  }
}
async function handleCustomerSearch(keyword: string) {
  customerLoading.value = true
  try {
    const list: any = await crmCustomerApi.dropdown(keyword || undefined)
    const searched = (Array.isArray(list) ? list : []).map((c: any) => ({ label: c.name, value: c.id }))
    // 保留当前已选项，避免远端结果里没有它时下拉退化成显示 id
    const kept = customerOptions.value.filter(o => String(o.value) === String(form.customerId))
    const merged = new Map<string, any>()
    for (const o of [...kept, ...searched]) merged.set(String(o.value), o)
    customerOptions.value = [...merged.values()]
  } catch (e) {
    console.warn('[发票表单] 客户下拉搜索失败', e)
  } finally {
    customerLoading.value = false
  }
}

async function loadSupplierOptions() {
  try {
    const res: any = await supplierApi.page({ pageNum: 1, pageSize: 200 })
    supplierOptions.value = (res?.records || []).map((s: any) => ({ label: s.supplierName, value: s.id }))
  } catch (e) {
    console.warn('[发票表单] 供应商下拉加载失败', e)
    supplierOptions.value = []
  }
}
async function handleSupplierSearch(keyword: string) {
  supplierLoading.value = true
  try {
    const res: any = await supplierApi.page({ pageNum: 1, pageSize: 50, keyword: keyword || undefined })
    const searched = (res?.records || []).map((s: any) => ({ label: s.supplierName, value: s.id }))
    const kept = supplierOptions.value.filter(o => String(o.value) === String(form.supplierId))
    const merged = new Map<string, any>()
    for (const o of [...kept, ...searched]) merged.set(String(o.value), o)
    supplierOptions.value = [...merged.values()]
  } catch (e) {
    console.warn('[发票表单] 供应商下拉搜索失败', e)
  } finally {
    supplierLoading.value = false
  }
}

function mapProducts(records: any[]) {
  return records.map((p: any) => ({
    label: `${p.productName}${p.spec ? ` (${p.spec})` : ''}`,
    value: p.id,
    itemCode: p.productCode,
    itemName: p.productName,
    specification: p.spec,
    unit: p.unit,
    // 商品档案无 salePrice 字段（原实现读 product.salePrice → 单价恒 0）；取标准售价/零售价
    unitPrice: p.standardPrice ?? p.retailPrice ?? 0,
  }))
}
async function loadProductOptions() {
  try {
    const res: any = await productApi.page({ pageNum: 1, pageSize: 50 })
    productOptions.value = mapProducts(res?.records || [])
  } catch (e) {
    console.warn('[发票表单] 商品下拉加载失败', e)
    productOptions.value = []
  }
}
async function handleProductSearch(keyword: string) {
  productLoading.value = true
  try {
    const res: any = await productApi.page({ pageNum: 1, pageSize: 50, keyword: keyword || undefined })
    const selectedIds = new Set(
      form.items.map((i: any) => i.productId).filter((v: any) => v != null).map((v: any) => String(v)),
    )
    const kept = productOptions.value.filter(o => selectedIds.has(String(o.value)))
    const merged = new Map<string, any>()
    for (const o of [...kept, ...mapProducts(res?.records || [])]) merged.set(String(o.value), o)
    productOptions.value = [...merged.values()]
  } catch (e) {
    console.warn('[发票表单] 商品下拉搜索失败', e)
  } finally {
    productLoading.value = false
  }
}

function onDirectionChange() {
  // 方向切换后发票类型候选变化：若当前类型不属于新方向则清空，避免落库类型与方向矛盾
  const keys = form.direction === 'purchase' ? PURCHASE_TYPES : SALES_TYPES
  if (form.invoiceType && !keys.includes(form.invoiceType)) form.invoiceType = undefined
}
function onCustomerChange(customerId: any) {
  if (customerId === undefined || customerId === null || customerId === '') {
    form.customerName = ''
    return
  }
  const found = customerOptions.value.find(o => String(o.value) === String(customerId))
  form.customerName = found?.label || ''
}
function onSupplierChange(supplierId: any) {
  if (supplierId === undefined || supplierId === null || supplierId === '') {
    form.supplierName = ''
    return
  }
  const found = supplierOptions.value.find(o => String(o.value) === String(supplierId))
  form.supplierName = found?.label || ''
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
    itemCode: '',
    itemName: '',
    specification: '',
    unit: '',
    quantity: 1,
    unitPrice: 0,
    taxRate: 13,
    amount: 0,
    taxAmount: 0,
  }
}
/** 与后端 invoice_item 口径一致：amount = quantity × unitPrice；taxAmount = amount × taxRate/100 */
function recalcItem(item: any) {
  const amount = round2((Number(item.quantity) || 0) * (Number(item.unitPrice) || 0))
  item.amount = amount
  item.taxAmount = round2(amount * (Number(item.taxRate) || 0) / 100)
}
function addItem() {
  form.items.push(createEmptyItem())
  itemPagination.current = Math.max(1, Math.ceil(form.items.length / itemPagination.pageSize))
}
function removeItem(record: any) {
  const idx = form.items.findIndex((i: any) => i.__key === record.__key)
  if (idx > -1) form.items.splice(idx, 1)
  if (!form.items.length && editable.value) form.items.push(createEmptyItem())
}
function onProductSelect(record: any, productId: any) {
  const found = productOptions.value.find(o => String(o.value) === String(productId))
  if (!found) {
    record.productId = undefined
    return
  }
  record.productId = found.value
  record.itemCode = found.itemCode || ''
  record.itemName = found.itemName || ''
  record.specification = found.specification || ''
  record.unit = found.unit || ''
  record.unitPrice = Number(found.unitPrice) || 0
  recalcItem(record)
}

// 明细分页（客户端切片；组件内置分页不做切片）
const itemPagination = reactive({ current: 1, pageSize: 20 })
const itemKeyword = ref('')
const filteredItems = computed(() => {
  const kw = itemKeyword.value.trim().toLowerCase()
  if (!kw) return form.items
  return form.items.filter((i: any) =>
    String(i.itemName || '').toLowerCase().includes(kw)
    || String(i.itemCode || '').toLowerCase().includes(kw)
    || String(i.specification || '').toLowerCase().includes(kw),
  )
})
const pagedItems = computed(() => {
  const start = (itemPagination.current - 1) * itemPagination.pageSize
  return filteredItems.value.slice(start, start + itemPagination.pageSize)
})
const totalQuantity = computed(() => form.items.reduce((s: number, i: any) => s + (Number(i.quantity) || 0), 0))
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

// ═══ 明细列（key 与 invoice_item 字段逐字对齐；操作列 key 固定 action 才不进列配置面板） ═══
const itemColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'itemActionCell', width: 70, fixed: 'left' },
  { key: 'itemName', title: '商品名称', type: 'slot', slotName: 'itemNameCell', width: 230 },
  { key: 'itemCode', title: '商品编码', type: 'slot', slotName: 'itemCodeCell', width: 140 },
  { key: 'specification', title: '规格型号', type: 'slot', slotName: 'specificationCell', width: 140 },
  { key: 'unit', title: '单位', type: 'slot', slotName: 'unitCell', width: 90, align: 'center' },
  { key: 'quantity', title: '数量', type: 'slot', slotName: 'quantityCell', width: 100, align: 'right' },
  { key: 'unitPrice', title: '单价', type: 'slot', slotName: 'unitPriceCell', width: 110, align: 'right' },
  { key: 'taxRate', title: '税率%', type: 'slot', slotName: 'taxRateCell', width: 90, align: 'right' },
  { key: 'amount', title: '金额', type: 'slot', slotName: 'itemAmountCell', width: 110, align: 'right' },
  { key: 'taxAmount', title: '税额', type: 'slot', slotName: 'itemTaxAmountCell', width: 110, align: 'right' },
]
const itemSummaryColumns = computed(() => {
  const rows = form.items
  return [
    { key: 'quantity', value: rows.reduce((s: number, i: any) => s + (Number(i.quantity) || 0), 0) },
    { key: 'amount', value: round2(rows.reduce((s: number, i: any) => s + (Number(i.amount) || 0), 0)) },
    { key: 'taxAmount', value: round2(rows.reduce((s: number, i: any) => s + (Number(i.taxAmount) || 0), 0)), highlight: true },
  ]
})

// ═══ 详情加载 / 保存 ═══
function readRouteId(): string | undefined {
  const raw: any = route.params?.id ?? route.query?.id
  // ID 一律按字符串处理，禁止 Number()（自增/雪花都可能超出安全整数）
  return raw === undefined || raw === null || raw === '' ? undefined : String(raw)
}

async function loadDetail(id: string) {
  pageLoading.value = true
  try {
    const data: any = await invoiceApi.getById(id as any) || {}
    form.id = data.id
    form.invoiceNumber = data.invoiceNumber || ''
    form.invoiceType = data.invoiceType || undefined
    form.invoiceDate = data.invoiceDate || ''
    form.dueDate = data.dueDate || ''
    form.issuedByName = data.issuedByName || ''
    form.currencyCode = data.currencyCode || 'CNY'
    form.exchangeRate = data.exchangeRate ?? 1
    form.taxType = data.taxType || ''
    form.customerId = data.customerId ?? undefined
    form.customerName = data.customerName || ''
    form.customerTaxNumber = data.customerTaxNumber || ''
    form.customerAddress = data.customerAddress || ''
    form.customerPhone = data.customerPhone || ''
    form.customerBankAccount = data.customerBankAccount || ''
    form.supplierId = data.supplierId ?? undefined
    form.supplierName = data.supplierName || ''
    form.supplierTaxNumber = data.supplierTaxNumber || ''
    form.supplierAddress = data.supplierAddress || ''
    form.supplierPhone = data.supplierPhone || ''
    form.supplierBankAccount = data.supplierBankAccount || ''
    form.orderNumber = data.orderNumber || ''
    form.contractNumber = data.contractNumber || ''
    form.deliveryNumber = data.deliveryNumber || ''
    form.projectCode = data.projectCode || ''
    form.businessRegion = data.businessRegion || ''
    form.businessDepartment = data.businessDepartment || ''
    form.paymentTerms = data.paymentTerms || ''
    form.subtotalAmount = Number(data.subtotalAmount) || 0
    form.taxAmount = Number(data.taxAmount) || 0
    form.discountAmount = Number(data.discountAmount) || 0
    form.shippingAmount = Number(data.shippingAmount) || 0
    form.otherAmount = Number(data.otherAmount) || 0
    form.paidAmount = Number(data.paidAmount) || 0
    form.unpaidAmount = Number(data.unpaidAmount) || 0
    form.invoiceStatus = data.invoiceStatus || 'DRAFT'
    form.notes = data.notes || ''
    // 方向无落库列，由 invoiceType 反推（后端 InvoiceType.isPurchaseType()）
    form.direction = PURCHASE_TYPES.includes(String(data.invoiceType)) ? 'purchase' : 'sales'
    // invoice_item 无写入路径 → 明细恒空；不改造假数据，保留空数组并保证可录入
    form.items = (Array.isArray(data.items) ? data.items : []).map((i: any) => ({
      __key: `item-${i.id}`,
      id: i.id,
      productId: undefined,
      itemCode: i.itemCode || '',
      itemName: i.itemName || '',
      specification: i.specification || '',
      unit: i.unit || '',
      quantity: Number(i.quantity) || 0,
      unitPrice: Number(i.unitPrice) || 0,
      taxRate: Number(i.taxRate) || 0,
      amount: Number(i.amount) || 0,
      taxAmount: Number(i.taxAmount) || 0,
    }))
    // 编辑回填：若往来方不在下拉前 200 条内，补一条选项，避免下拉退化成显示主键
    if (form.customerId && !customerOptions.value.some((o: any) => String(o.value) === String(form.customerId))) {
      customerOptions.value.push({ label: form.customerName || String(form.customerId), value: form.customerId })
    }
    if (form.supplierId && !supplierOptions.value.some((o: any) => String(o.value) === String(form.supplierId))) {
      supplierOptions.value.push({ label: form.supplierName || String(form.supplierId), value: form.supplierId })
    }
    if (!form.items.length && editable.value) form.items.push(createEmptyItem())
    itemPagination.current = 1
  } catch (e) {
    console.error('[发票表单] 加载详情失败', e)
    message.error('加载发票信息失败')
  } finally {
    pageLoading.value = false
    saveSnapshot()
  }
}

async function fetchData() {
  const id = readRouteId()
  if (id) {
    await loadDetail(id)
    return
  }
  form.id = undefined
  form.invoiceNumber = ''
  form.invoiceStatus = 'GENERATED'
  if (!form.items.length) form.items.push(createEmptyItem())
}
const invoiceOps = {
  /** 开具/状态变更：newStatus 取 InvoiceStatus 枚举 name（后端无 ISSUED） */
  updateStatus(id: any, newStatus: string, notes?: string) {
    return request.put(`/erp/invoice/${id}/status`, null, { params: { newStatus, notes } })
  },
  send(id: any, sendMethod: string, sentBy: any) {
    return request.post(`/erp/invoice/${id}/send`, null, { params: { sendMethod, sentBy } })
  },
  voidInvoice(id: any, reason: string, voidedBy: any) {
    return request.post(`/erp/invoice/${id}/void`, null, { params: { reason, voidedBy } })
  },
}

/**
 * 保存 payload：只提交 Invoice 实体真实存在的字段。
 * ⚠️ 不带 items（后端 createInvoiceFromApplication 不写明细，而 PUT /{id} 走 Jackson 合并，
 *    带 items 会把关联当补丁写入而产生脏数据）；不带 direction（实体无该列）。
 */
function buildPayload() {
  return {
    invoiceNumber: form.invoiceNumber || undefined,
    invoiceType: form.invoiceType || undefined,
    invoiceDate: form.invoiceDate || undefined,
    dueDate: form.dueDate || undefined,
    currencyCode: form.currencyCode || undefined,
    exchangeRate: form.exchangeRate ?? undefined,
    taxType: form.taxType || undefined,
    customerId: form.customerId ?? undefined,
    customerName: form.customerName || undefined,
    customerTaxNumber: form.customerTaxNumber || undefined,
    customerAddress: form.customerAddress || undefined,
    customerPhone: form.customerPhone || undefined,
    customerBankAccount: form.customerBankAccount || undefined,
    supplierId: form.supplierId ?? undefined,
    supplierName: form.supplierName || undefined,
    supplierTaxNumber: form.supplierTaxNumber || undefined,
    supplierAddress: form.supplierAddress || undefined,
    supplierPhone: form.supplierPhone || undefined,
    supplierBankAccount: form.supplierBankAccount || undefined,
    orderNumber: form.orderNumber || undefined,
    contractNumber: form.contractNumber || undefined,
    deliveryNumber: form.deliveryNumber || undefined,
    projectCode: form.projectCode || undefined,
    businessRegion: form.businessRegion || undefined,
    businessDepartment: form.businessDepartment || undefined,
    paymentTerms: form.paymentTerms || undefined,
    subtotalAmount: effectiveSubtotal.value,
    taxAmount: effectiveTax.value,
    discountAmount: Number(form.discountAmount) || 0,
    shippingAmount: Number(form.shippingAmount) || 0,
    otherAmount: Number(form.otherAmount) || 0,
    totalAmount: effectiveTotal.value,
    issuedByName: form.issuedByName || undefined,
    notes: form.notes || undefined,
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请完善必填项（含发票号码 / 类型 / 日期 / 到期日）')
    return
  }
  // 往来方在独立卡片（非 formRef 所在的 a-form），故此处显式校验（与 Invoice.validate() 一致）
  if (form.direction === 'purchase' && !form.supplierId) {
    message.warning('请选择供应商')
    return
  }
  if (form.direction !== 'purchase' && !form.customerId) {
    message.warning('请选择客户')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      // 编辑：PUT /erp/invoice/{id}（后端本轮已补，部分更新语义：仅覆盖非 null 字段）
      await invoiceApi.update(form.id, payload as any)
      message.success('保存成功')
      await loadDetail(String(form.id))
      return
    }
    // 新建：后端仅提供「由发票申请单生成」的端点，issuedBy/issuedByName 为必填 query（原先不传 → 必 400）
    const issuedBy = userStore.userId
    const issuedByName = userStore.nickname || userStore.username || ''
    if (!issuedBy) {
      message.error('未获取到当前登录用户，无法保存')
      return
    }
    const res: any = await request.post('/erp/invoice/create-from-application', payload, {
      params: { issuedBy, issuedByName },
    })
    message.success('保存成功')
    if (res?.id) {
      await loadDetail(String(res.id))
      await router.replace({ path: '/crm/invoice/form', query: { id: String(res.id) } })
    }
  } catch (e: any) {
    console.warn('[发票表单] 保存失败', e)
    if (e?.errorFields) message.warning('请完善必填项')
  } finally {
    saving.value = false
  }
}

// ═══ 状态操作（开具 / 发送 / 作废，参数与后端必填项对齐） ═══
const canIssue = computed(() => ['DRAFT', 'SUBMITTED', 'IN_APPROVAL', 'APPROVED', 'REJECTED'].includes(String(form.invoiceStatus)))
const canSend = computed(() => String(form.invoiceStatus) === 'GENERATED')
const canVoid = computed(() => String(form.invoiceStatus) === 'GENERATED')

const sendVisible = ref(false)
const sendMethod = ref('EMAIL')
const voidVisible = ref(false)
const voidReason = ref('')

function handleFlowAction(key: string) {
  if (key === 'issue') {
    Modal.confirm({
      title: '确认开具',
      content: '确定将本发票置为「已生成」吗？',
      okText: '确认开具',
      cancelText: '取消',
      centered: true,
      onOk: async () => {
        try {
          await invoiceOps.updateStatus(form.id, 'GENERATED', '前端开具')
          message.success('发票已开具（状态：已生成）')
          await loadDetail(String(form.id))
        } catch (e) {
          console.warn('[发票表单] 开具失败', e)
        }
      },
    })
    return
  }
  if (key === 'send') {
    sendMethod.value = 'EMAIL'
    sendVisible.value = true
    return
  }
  if (key === 'void') {
    voidReason.value = ''
    voidVisible.value = true
  }
}

async function handleSendConfirm() {
  if (!sendMethod.value) {
    message.warning('请选择发送方式')
    return
  }
  const sentBy = userStore.userId
  if (!sentBy) {
    message.error('未获取到当前登录用户，无法发送')
    return
  }
  flowLoading.value = true
  try {
    await invoiceOps.send(form.id, sendMethod.value, sentBy)
    message.success('发票已发送')
    sendVisible.value = false
    await loadDetail(String(form.id))
  } catch (e) {
    console.warn('[发票表单] 发送失败', e)
  } finally {
    flowLoading.value = false
  }
}

async function handleVoidConfirm() {
  const reason = voidReason.value.trim()
  if (!reason) {
    message.warning('请填写作废原因（后端必填）')
    return
  }
  const voidedBy = userStore.userId
  if (!voidedBy) {
    message.error('未获取到当前登录用户，无法作废')
    return
  }
  flowLoading.value = true
  try {
    await invoiceOps.voidInvoice(form.id, reason, voidedBy)
    message.success('发票已作废')
    voidVisible.value = false
    await loadDetail(String(form.id))
  } catch (e) {
    console.warn('[发票表单] 作废失败', e)
  } finally {
    flowLoading.value = false
  }
}

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
      <td>${escapeHtml(i.itemCode || '')}</td>
      <td>${escapeHtml(i.itemName || '')}</td>
      <td>${escapeHtml(i.specification || '')}</td>
      <td>${escapeHtml(i.unit || '')}</td>
      <td style="text-align:right">${i.quantity ?? 0}</td>
      <td style="text-align:right">${formatAmount(i.unitPrice)}</td>
      <td style="text-align:right">${i.taxRate || 0}%</td>
      <td style="text-align:right">${formatAmount(i.amount)}</td>
      <td style="text-align:right">${formatAmount(i.taxAmount)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>发票</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
      .sum{margin-top:8px;text-align:right;font-size:13px}
    </style></head><body>
    <h2>发票</h2>
    <div class="meta">
      <span>发票号码：${escapeHtml(form.invoiceNumber || '')}</span>
      <span>发票类型：${escapeHtml(INVOICE_TYPE_TEXT[String(form.invoiceType)] || '')}</span>
      <span>客户/供应商：${escapeHtml(form.customerName || form.supplierName || '')}</span>
      <span>开票日期：${escapeHtml(form.invoiceDate || '')}</span>
      <span>到期日：${escapeHtml(form.dueDate || '')}</span>
      <span>状态：${escapeHtml(getStatusText(form.invoiceStatus))}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
    </div>
    <table>
      <thead><tr><th>#</th><th>商品编码</th><th>商品名称</th><th>规格型号</th><th>单位</th>
      <th>数量</th><th>单价</th><th>税率%</th><th>金额</th><th>税额</th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="sum">
      不含税金额：¥${formatAmount(effectiveSubtotal.value)}
      折扣：¥${formatAmount(form.discountAmount)}
      运费：¥${formatAmount(form.shippingAmount)}
      其他费用：¥${formatAmount(form.otherAmount)}
      税额：¥${formatAmount(effectiveTax.value)}
      <strong>价税合计：¥${formatAmount(effectiveTotal.value)}</strong>
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

// ═══ 快捷键（Ctrl+S 保存；原先只是提示文字）；后端无发票提交端点，故不设「提交」 ═══
function handleKeydown(e: KeyboardEvent) {
  if (!e.ctrlKey && !e.metaKey) return
  if (e.key.toLowerCase() === 's') {
    e.preventDefault()
    if (editable.value && !saving.value) handleSave()
  }
}

// ═══ 导航 / 脏检查 ═══
const initialSnapshot = ref('')
function snapshot(): string {
  return JSON.stringify({ ...form, items: form.items.map((i: any) => ({ ...i, __key: undefined })) })
}
const dirty = computed(() => !!initialSnapshot.value && editable.value && snapshot() !== initialSnapshot.value)
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
      onOk: () => router.push('/crm/invoice'),
    })
    return
  }
  router.push('/crm/invoice')
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
  console.error('[发票表单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(async () => {
  pageLoading.value = true
  try {
    // 开票人默认当前登录用户（后端 issued_by/issued_by_name 为必填快照）
    form.issuedByName = userStore.nickname || userStore.username || ''
    await Promise.all([loadCustomerOptions(), loadSupplierOptions(), loadProductOptions()])
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
.items-wrap { height: 360px; display: flex; flex-direction: column; }

.doc-no { font-size: 13px; color: #303133; }
.mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}
.amount-red { color: #f5222d; font-weight: 500; }

.summary-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.summary-label { font-size: 13px; color: #666; }
.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}
.summary-item.total .summary-value { color: #f5222d; }

.shortcut-hint {
  display: inline-flex;
  align-items: center;
  margin-left: 4px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
  font-size: 11px;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
