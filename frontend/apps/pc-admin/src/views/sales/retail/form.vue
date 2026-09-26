<template>
  <div class="retail-form-page">
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :tabs="[]"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleHoldOrder"
      @submit="handleSettle"
    >
      <!-- ═══ 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <!-- 商品搜索行 -->
        <div v-if="isVisible('productSearch')" class="product-search-row">
          <span class="search-label">商品 条码/名称/编码(F1)</span>
          <a-input
            ref="productSearchInput"
            v-model:value="productSearchText"
            placeholder="条码/名称/编码"
            size="small"
            class="product-search-input"
            @press-enter="handleProductSearch"
          />
        </div>
        <BillDetailTable
          :columns="detailColumns"
          v-model:data-source="formData.products"
          :summary-columns="tableSummaryColumns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
          @open-select-modal="handleOpenProductSelectModal"
        >
          <template #actionCell="{ index, empty }">
            <template v-if="!empty">
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  class="action-add-btn"
                  @click="handleInsertProduct(index)"
                >
                  <PlusCircleOutlined />
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  class="action-del-btn"
                  @click="handleRemoveProduct(index)"
                >
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
            <template v-else>
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  class="action-add-btn"
                  @click="handleAddProduct()"
                >
                  <PlusCircleOutlined />
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  class="action-del-btn"
                  disabled
                >
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
          </template>
        </BillDetailTable>
      </template>

      <!-- ═══ 底部面板：优惠 + 扩展信息 + 收款（字段受页面配置控制） ═══ -->
      <template #bottom-extra>
        <div class="retail-bottom-layout">
          <!-- 简易模式：仅一行金额摘要 -->
          <template v-if="simpleMode">
            <div class="retail-bottom-simple">
              <span class="simple-amount-label">应收金额：</span>
              <span class="simple-amount-value text-red">{{ payableAmount.toFixed(2) }}</span>
              <span style="margin:0 20px;color:#ccc">|</span>
              <span class="simple-amount-label">实收金额：</span>
              <span class="simple-amount-value">{{ totalPaid.toFixed(2) }}</span>
              <span style="margin:0 20px;color:#ccc">|</span>
              <span class="simple-amount-label">找零：</span>
              <span class="simple-amount-value">{{ changeAmount.toFixed(2) }}</span>
            </div>
          </template>
          <template v-else>
            <!-- 左侧：优惠 + 扩展信息 + 备注 + 单据信息 -->
            <div class="retail-bottom-left">
              <!-- 优惠行 -->
              <div v-if="showDiscountRow" class="discount-row">
                <div v-if="isVisible('directDiscount')" class="discount-field">
                  <span class="discount-label">直接优惠</span>
                  <a-input-number
                    v-model:value="directDiscount"
                    :min="0"
                    :precision="2"
                    size="small"
                    style="width: 140px"
                    placeholder="0"
                    @change="updateChangeAmount"
                  />
                </div>
                <div v-if="isVisible('couponDiscount')" class="discount-field">
                  <span class="discount-label">优惠券</span>
                  <a-input-number
                    v-model:value="formData.couponDiscount"
                    :min="0"
                    :precision="2"
                    size="small"
                    style="width: 120px"
                    placeholder="0"
                    @change="updateChangeAmount"
                  />
                </div>
                <div v-if="isVisible('promoDiscount')" class="discount-field">
                  <span class="discount-label">促销优惠</span>
                  <a-input-number
                    v-model:value="promoDiscount"
                    :min="0"
                    :precision="2"
                    size="small"
                    style="width: 110px"
                    placeholder="0"
                    @change="updateChangeAmount"
                  />
                </div>
                <div v-if="isVisible('prevPoints')" class="discount-field">
                  <span class="discount-label">此前积分</span>
                  <span class="discount-value">{{ prevPoints }}</span>
                </div>
                <div class="discount-field">
                  <span class="discount-label">本次产生积分</span>
                  <span class="discount-value">{{ memberGeneratedPoints }}</span>
                </div>
              </div>

              <!-- 扩展信息（客户快照 + 自定义字段） -->
              <div v-if="showExtInfo" class="ext-info-row">
                <div v-if="isVisible('bankName')" class="ext-field">
                  <span class="ext-label">开户行</span>
                  <a-input v-model:value="formData.bankName" size="small" />
                </div>
                <div v-if="isVisible('bankAccount')" class="ext-field">
                  <span class="ext-label">银行账号</span>
                  <a-input v-model:value="formData.bankAccount" size="small" />
                </div>
                <div v-if="isVisible('taxNo')" class="ext-field">
                  <span class="ext-label">税号</span>
                  <a-input v-model:value="formData.taxNo" size="small" />
                </div>
                <div v-if="isVisible('extNum1')" class="ext-field">
                  <span class="ext-label">自定义字段1</span>
                  <a-input-number v-model:value="formData.extNum1" size="small" :precision="2" style="width:100%" />
                </div>
                <div v-if="isVisible('extNum2')" class="ext-field">
                  <span class="ext-label">自定义字段2</span>
                  <a-input-number v-model:value="formData.extNum2" size="small" :precision="2" style="width:100%" />
                </div>
                <div v-if="isVisible('extNum3')" class="ext-field">
                  <span class="ext-label">自定义字段3</span>
                  <a-input-number v-model:value="formData.extNum3" size="small" :precision="2" style="width:100%" />
                </div>
                <div v-if="isVisible('extText1')" class="ext-field">
                  <span class="ext-label">自定义字段4</span>
                  <a-input v-model:value="formData.extText1" size="small" />
                </div>
                <div v-if="isVisible('extText2')" class="ext-field">
                  <span class="ext-label">自定义字段5</span>
                  <a-input v-model:value="formData.extText2" size="small" />
                </div>
              </div>

              <!-- 单据备注 -->
              <div v-if="isVisible('orderRemark')" class="remark-row">
                <span class="remark-label">单据备注</span>
                <a-input
                  v-model:value="formData.orderRemark"
                  size="small"
                  class="remark-input"
                />
              </div>
              <!-- 单据信息 -->
              <div class="doc-info-row">
                <span v-if="isVisible('creatorName')" class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
                <span v-if="isVisible('createTime')" class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
                <span v-if="isVisible('printCount')" class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
                <span v-if="isVisible('cashierName')" class="doc-info-item">收银员 {{ formData.cashierName || currentUserName || '系统' }}</span>
                <a-button type="link" size="small" class="doc-info-link" @click="handlePrint">
                  打印小票
                </a-button>
              </div>
            </div>

            <!-- 右侧：收款面板 -->
            <div class="retail-payment-panel">
              <div class="payment-title">
                收款
              </div>
              <!-- 收款方式（受「零售设置」启用集合控制） -->
              <div class="payment-buttons">
                <button
                  v-for="m in enabledPayMethods"
                  :key="m.value"
                  class="pay-btn"
                  :style="{ background: m.color }"
                  :class="{ active: methodAmounts[m.value] > 0 }"
                  @click="focusPayment(m.value)"
                >
                  <span class="pay-btn-icon">{{ m.icon }}</span>
                  <span>{{ m.label }}</span>
                </button>
              </div>
              <div class="payment-buttons">
                <label v-if="isVisible('combinedPayment')" class="combined-payment-label">
                  <a-checkbox v-model:checked="combinedPayment" @change="handleCombinedChange" />
                  <span>组合收款</span>
                </label>
                <a-button type="link" size="small" @click="clearPayments">清空收款</a-button>
              </div>
              <!-- 收款明细 -->
              <div class="payment-details">
                <div v-if="isVisible('payableAmount')" class="payment-detail-row">
                  <span class="payment-detail-label">应收金额</span>
                  <span class="payment-detail-value text-red">{{ payableAmount.toFixed(2) }}</span>
                </div>
                <div v-if="isVisible('payMethod1')" class="payment-detail-row">
                  <span class="payment-detail-label">收款方式1</span>
                  <a-select v-model:value="primaryMethod" size="small" style="width:120px" :options="enabledPayMethods.map(m => ({ label: m.label, value: m.value }))" />
                </div>
                <div v-if="isVisible('payAmount')" class="payment-detail-row">
                  <span class="payment-detail-label">收款金额</span>
                  <a-input-number
                    v-model:value="methodAmounts[primaryMethod]"
                    :min="0"
                    :precision="2"
                    size="small"
                    style="width: 120px"
                    @change="updateChangeAmount"
                  />
                </div>
                <div v-if="isVisible('payMethod')" class="payment-detail-row">
                  <span class="payment-detail-label">收款方式2</span>
                  <a-select v-model:value="secondMethod" size="small" style="width:120px" :options="enabledPayMethods.map(m => ({ label: m.label, value: m.value }))" />
                </div>
                <div v-if="isVisible('availablePrepaid')" class="payment-detail-row">
                  <span class="payment-detail-label">可用预收</span>
                  <span class="payment-detail-value">{{ formData.prepaidBalance || 0 }}</span>
                </div>
                <div v-if="isVisible('prepaidBalance')" class="payment-detail-row">
                  <span class="payment-detail-label">预收余额</span>
                  <span class="payment-detail-value">{{ prepaidBalanceDisplay }}</span>
                </div>
                <div v-if="isVisible('payQrCode')" class="payment-detail-row">
                  <span class="payment-detail-label">收款码</span>
                  <a-input v-model:value="paymentQrCode" size="small" style="width: 160px" placeholder="扫码/账号" />
                </div>
                <div v-if="isVisible('changeAmount')" class="payment-detail-row">
                  <span class="payment-detail-label">找零金额</span>
                  <span class="payment-detail-value">{{ changeAmount.toFixed(2) }}</span>
                </div>
                <div class="payment-detail-row">
                  <span class="payment-detail-label">实收合计</span>
                  <span class="payment-detail-value text-red">{{ totalPaid.toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </template>

    </BillFormPage>

    <!-- ═══ 配置弹窗：页面配置(36字段) / 零售设置 / 打印设置 ═══ -->
    <a-modal
      v-model:open="showFormConfig"
      title="配置"
      :width="820"
      :footer="null"
      destroy-on-close
    >
      <a-tabs v-model:active-key="configModalTab" size="small">
        <!-- Tab 1: 页面配置 -->
        <a-tab-pane key="pageConfig" tab="页面配置">
          <p class="config-hint">勾选控制字段显隐，勾选后自动保存并对所有操作员生效</p>
          <a-table
            :columns="pageConfigTableColumns"
            :data-source="pageConfigFields"
            :pagination="false"
            size="small"
            row-key="key"
            :scroll="{ y: 380 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'visible'">
                <a-checkbox
                  :checked="record.visible"
                  @change="(e: any) => handlePageFieldVisibleChange(record.key, e.target.checked)"
                />
              </template>
            </template>
          </a-table>
        </a-tab-pane>
        <!-- Tab 2: 零售设置 -->
        <a-tab-pane key="settings" tab="零售设置">
          <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-item label="默认出库仓库">
              <a-select
                v-model:value="retailSettings.defaultWarehouseId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="(optionRefs.warehouses || []).map((w: any) => ({ label: w.name || w.warehouseName, value: w.id }))"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认经手人">
              <a-select
                v-model:value="retailSettings.defaultHandlerId"
                show-search
                allow-clear
                size="small"
                style="width:100%"
                :loading="loadingOptions"
                :options="(optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))"
                @change="saveFormConfig"
              />
            </a-form-item>
            <a-form-item label="默认销售类型">
              <a-select v-model:value="retailSettings.defaultSaleType" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option value="NORMAL">正常销售</a-select-option>
                <a-select-option value="RETURN">退货</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="积分规则">
              <a-input-number v-model:value="retailSettings.pointPerYuan" :min="0" :precision="2" size="small" @change="saveFormConfig" />
              <span style="margin-left:8px;color:#8c8c8c">每消费 1 元产生的积分数</span>
            </a-form-item>
            <a-form-item label="收款方式">
              <a-checkbox-group
                :value="retailSettings.enabledMethods"
                :options="payMethodOptions"
                @change="handleEnabledMethodsChange"
              />
            </a-form-item>
            <a-form-item label="默认开启组合收款">
              <a-checkbox v-model:checked="retailSettings.enableCombinedPayment" @change="saveFormConfig">
                允许同一单多种收款方式组合
              </a-checkbox>
            </a-form-item>
            <a-form-item label="默认简易模式">
              <a-checkbox v-model:checked="retailSettings.autoSimpleMode" @change="saveFormConfig">
                打开表单时默认隐藏扩展列
              </a-checkbox>
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <!-- Tab 3: 打印设置 -->
        <a-tab-pane key="printSettings" tab="打印设置">
          <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
            <a-form-item label="打印模板">
              <a-select v-model:value="printSettings.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option value="standard">标准模板（小票）</a-select-option>
                <a-select-option value="simple">简化模板</a-select-option>
                <a-select-option value="detailed">详细模板</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="打印份数">
              <a-input-number v-model:value="printSettings.printCopies" :min="1" :precision="0" size="small" @change="saveFormConfig" />
            </a-form-item>
            <a-form-item label="纸张大小">
              <a-select v-model:value="printSettings.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                <a-select-option value="A4">A4</a-select-option>
                <a-select-option value="A5">A5</a-select-option>
                <a-select-option value="B5">B5</a-select-option>
                <a-select-option value="58mm">58mm 热敏小票</a-select-option>
                <a-select-option value="80mm">80mm 热敏小票</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="打印选项">
              <a-checkbox v-model:checked="printSettings.alwaysLastTemplate" @change="saveFormConfig">
                始终使用最后一次打印的模板，打印时不再选择
              </a-checkbox>
              <a-checkbox v-model:checked="printSettings.printAfterSubmit" @change="saveFormConfig">
                记账后立即打印
              </a-checkbox>
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 会员选择弹窗 ═══ -->
    <a-modal
      v-model:open="showMemberSearch"
      title="选择会员"
      width="520px"
      @ok="confirmMemberSelect"
    >
      <a-input-search
        v-model:value="memberSearchKeyword"
        placeholder="输入手机号 / 会员卡号 / 姓名搜索"
        enter-button="搜索"
        style="margin-bottom: 12px"
        @search="doMemberSearch"
      />
      <a-list
        :data-source="memberSearchResults"
        :loading="memberSearching"
        size="small"
      >
        <template #renderItem="{ item }">
          <a-list-item
            style="cursor:pointer"
            :class="{ 'member-selected': selectedMemberId === item.id }"
            @click="selectedMemberId = item.id; selectedMember = item"
          >
            <a-list-item-meta
              :title="item.partyName || item.name"
              :description="`手机: ${item.phone || '-'} | 卡号: ${item.memberCardNo || '-'}`"
            />
            <template #extra>
              <a-tag
                v-if="selectedMemberId === item.id"
                color="blue"
              >
                已选
              </a-tag>
            </template>
          </a-list-item>
        </template>
        <template #header>
          <div
            v-if="memberSearchResults.length === 0 && !memberSearching"
            style="color:#999;text-align:center;padding:16px 0"
          >
            未找到会员，可继续以散客下单
          </div>
        </template>
      </a-list>
      <div style="margin-top:8px;color:#999;font-size:12px">
        提示：不选择会员则按散客下账
      </div>
    </a-modal>

    <!-- ═══ POS收银模式全屏覆盖层 ═══ -->
    <Teleport to="body">
      <div
        v-if="posMode"
        class="pos-overlay"
      >
        <!-- POS 顶部栏 -->
        <div class="pos-header">
          <button
            class="pos-exit-btn"
            @click="exitPosMode"
          >
            <span class="pos-exit-icon" /> 退出POS收银模式(Shift+Esc)
          </button>
          <h1 class="pos-title">
            POS收银模式
          </h1>
          <div class="pos-header-actions">
            <button
              class="pos-shortcut-btn pos-shortcut-f10"
              @click="showShortcutPanel = true"
            >
              ⌨ 快捷键(F10)
            </button>
            <button
              class="pos-shortcut-btn pos-shortcut-f2"
              @click="handleShowOrderHistory"
            >
              🕐 零售单历史(F2)
            </button>
          </div>
        </div>

        <!-- POS 商品表格 -->
        <div class="pos-table-container">
          <table class="pos-table">
            <thead>
              <tr>
                <th class="pos-th-settings">
                  <SettingOutlined />
                </th>
                <th class="pos-th-action">
                  操作
                </th>
                <th class="pos-th-product">
                  商品名称
                </th>
                <th class="pos-th-barcode">
                  条码
                </th>
                <th class="pos-th-stock">
                  可用库存
                </th>
                <th class="pos-th-qty">
                  数量
                </th>
                <th class="pos-th-price">
                  单价
                </th>
                <th class="pos-th-amount">
                  金额
                </th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(row, idx) in posDisplayRows"
                :key="idx"
                class="pos-tr"
                :class="{
                  'pos-tr-first': idx === 0,
                  'pos-tr-selected': idx === posSelectedRow,
                  'pos-tr-empty': !row.productId && !row.productName
                }"
                @click="posSelectedRow = idx"
                @dblclick="handlePosProductSelect(idx)"
              >
                <td class="pos-td">
                  {{ idx + 1 }}
                </td>
                <td class="pos-td pos-td-action">
                  <button
                    class="pos-action-btn pos-action-add"
                    @click.stop="handleInsertProduct(idx)"
                  />
                  <button
                    class="pos-action-btn pos-action-del"
                    @click.stop="handleRemoveProduct(idx)"
                  >
                    ✕
                  </button>
                </td>
                <td class="pos-td pos-td-product">
                  <span
                    v-if="row.productId || row.productName"
                    class="pos-product-name"
                  >{{ row.productName || '未选择' }}</span>
                  <span
                    v-else
                    class="pos-empty-cell"
                    @click.stop="handlePosProductSelect(idx)"
                  >双击选择商品</span>
                </td>
                <td class="pos-td">
                  {{ row.barcode || '' }}
                </td>
                <td class="pos-td">
                  {{ row.availableStock != null ? row.availableStock : '' }}
                </td>
                <td class="pos-td pos-td-qty">
                  <input
                    v-if="row.productId"
                    type="number"
                    class="pos-input"
                    :value="row.quantity || 0"
                    min="0"
                    step="1"
                    @change="(e: any) => { row.quantity = Number(e.target.value); updateChangeAmount() }"
                  >
                  <span
                    v-else
                    class="pos-empty-cell"
                  />
                </td>
                <td class="pos-td pos-td-price">
                  {{ row.unitPrice != null ? row.unitPrice.toFixed(2) : '' }}
                </td>
                <td class="pos-td pos-td-amount">
                  {{ ((row.quantity || 0) * (row.unitPrice || 0)).toFixed(2) }}
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr class="pos-tr pos-tr-total">
                <td
                  class="pos-td pos-td-footer-label"
                  colspan="2"
                >
                  合计
                </td>
                <td class="pos-td" />
                <td class="pos-td" />
                <td class="pos-td pos-td-total-val">
                  {{ totalQuantity }}
                </td>
                <td class="pos-td pos-td-total-val">
                  {{ totalAmount.toFixed(2) }}
                </td>
                <td class="pos-td" />
                <td class="pos-td pos-td-total-val pos-td-total-amount">
                  {{ totalAmount.toFixed(2) }}
                </td>
              </tr>
            </tfoot>
          </table>
        </div>

        <!-- POS 底部栏 -->
        <div class="pos-bottom-bar">
          <div class="pos-bottom-info">
            <div class="pos-info-row">
              <span class="pos-info-label">客户名称：</span>
              <span class="pos-info-value">{{ formData.customerName || '散客' }}</span>
              <button
                class="pos-info-btn"
                title="详情"
                @click="showMemberSearch = true"
              >
                详
              </button>
              <button
                class="pos-info-btn"
                title="搜索"
                @click="showMemberSearch = true"
              />
            </div>
            <div class="pos-info-row">
              <span class="pos-info-label">会员卡号：</span>
              <span class="pos-info-placeholder">F6 输入卡号/电话号码</span>
              <span class="pos-info-points">此前积分：<span class="pos-points-value">{{ prevPoints }}</span></span>
            </div>
          </div>
          <div class="pos-bottom-actions">
            <button
              class="pos-action-btn-large pos-btn-hold"
              @click="handleHoldOrder"
            >
              挂单<br><span class="pos-shortcut-text">(F3)</span>
            </button>
            <button
              class="pos-action-btn-large pos-btn-settle"
              @click="handleSettle"
            >
              收款<br><span class="pos-shortcut-text">(空格键)</span>
            </button>
            <div class="pos-total-display">
              <span class="pos-total-symbol">¥</span>
              <span class="pos-total-amount">{{ payableAmount.toFixed(0) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ POS 快捷键面板 (F10) ═══ -->
      <a-modal
        v-model:open="showShortcutPanel"
        title="⌨ POS 收银快捷键"
        :footer="null"
        width="460px"
        :destroy-on-close="true"
        class="pos-shortcut-modal"
      >
        <div class="pos-shortcut-list">
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">F1</span>
            <span class="pos-shortcut-desc">搜索商品（条码/名称/编码）</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">F2</span>
            <span class="pos-shortcut-desc">零售单历史（今日单据）</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">F3</span>
            <span class="pos-shortcut-desc">挂单</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">F6</span>
            <span class="pos-shortcut-desc">会员卡号 / 手机号搜索</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">F10</span>
            <span class="pos-shortcut-desc">快捷键面板</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">空格</span>
            <span class="pos-shortcut-desc">收款 / 结算</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">↑↓</span>
            <span class="pos-shortcut-desc">切换选中商品行</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">双击行</span>
            <span class="pos-shortcut-desc">选择商品</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">Shift+Esc</span>
            <span class="pos-shortcut-desc">退出 POS 收银模式</span>
          </div>
          <div class="pos-shortcut-item">
            <span class="pos-shortcut-key">Ctrl+Enter</span>
            <span class="pos-shortcut-desc">记账（非POS模式）</span>
          </div>
        </div>
      </a-modal>

      <!-- ═══ POS 零售单历史 (F2) ═══ -->
      <a-modal
        v-model:open="showOrderHistory"
        title="🕐 零售单历史（今日）"
        :footer="null"
        width="700px"
        :destroy-on-close="true"
        class="pos-history-modal"
      >
        <div v-if="loadingOrderHistory" style="text-align:center;padding:24px;color:#999">
          <a-spin /> 加载中...
        </div>
        <div v-else-if="orderHistoryList.length === 0" style="text-align:center;padding:24px;color:#999">
          今日暂无零售单记录
        </div>
        <div v-else class="pos-history-list">
          <div class="pos-history-header">
            <span class="pos-history-h-col" style="width:160px">单据编号</span>
            <span class="pos-history-h-col" style="width:80px">金额</span>
            <span class="pos-history-h-col" style="width:80px">状态</span>
            <span class="pos-history-h-col" style="width:100px">客户</span>
            <span class="pos-history-h-col" style="width:100px">时间</span>
            <span class="pos-history-h-col" style="width:60px">操作</span>
          </div>
          <div
            v-for="item in orderHistoryList"
            :key="item.id"
            class="pos-history-row"
            @click="handleViewHistoryOrder(item)"
          >
            <span class="pos-history-col" style="width:160px">{{ item.retailNo || item.orderNo || '-' }}</span>
            <span class="pos-history-col" style="width:80px;color:#ff7a45">
              {{ (item.payableAmount || item.totalAmount || 0).toFixed(2) }}
            </span>
            <span class="pos-history-col" style="width:80px">
              <a-tag :color="statusColor(item.status)">{{ statusLabel(item.status) }}</a-tag>
            </span>
            <span class="pos-history-col" style="width:100px">{{ item.customerName || '散客' }}</span>
            <span class="pos-history-col" style="width:100px;color:#888">
              {{ item.orderDate || (item.createTime || '').slice(0, 10) }}
            </span>
            <span class="pos-history-col" style="width:60px">
              <a-button size="small" type="link" @click.stop="handleViewHistoryOrder(item)">查看</a-button>
            </span>
          </div>
        </div>
      </a-modal>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch, onMounted, onUnmounted } from 'vue'

defineOptions({ name: 'RetailForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
  ThunderboltOutlined,
  PrinterOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { retailOrderApi, memberApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { PRODUCT_RETAIL_DEFAULTS } from '@/utils/productDefaults'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)
const productSearchText = ref('')
const productSearchInput = ref()

// ═══ 会员搜索状态 ═══
const showMemberSearch = ref(false)
const memberSearchKeyword = ref('')
const memberSearchResults = ref<any[]>([])
const memberSearching = ref(false)
const selectedMemberId = ref<number | null>(null)
const selectedMember = ref<any>(null)

// ═══ POS 模式 ═══
const posMode = ref(false)
const simpleMode = ref(false)
const posSelectedRow = ref(0)
const showShortcutPanel = ref(false)
const showOrderHistory = ref(false)
const orderHistoryList = ref<any[]>([])
const loadingOrderHistory = ref(false)

/** 客户价格等级（用于商品单价自动匹配） */
const customerLevel = ref('')

// POS 显示行数（确保至少15行）
const posDisplayRows = computed(() => {
  const rows = formData.products || []
  const minRows = 15
  if (rows.length >= minRows) return rows
  return [...rows, ...Array.from({ length: minRows - rows.length }, () => ({
    id: `pos-empty-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    productId: undefined,
    productName: '',
    barcode: '',
    availableStock: 0,
    quantity: 0,
    unitPrice: 0,
    _isEmptyRow: true,
  }))]
})

// ═══ 收款方式定义（与后端 RetailOrderPayment.paymentMethod 一一对应）═══
const PAY_METHODS = [
  { value: 'CASH', label: '现金', icon: '💵', color: '#fa8c16' },
  { value: 'CARD', label: '银行卡', icon: '💳', color: '#faad14' },
  { value: 'PREPAID', label: '预收款', icon: '📥', color: '#1890ff' },
  { value: 'TRANSFER', label: '转账', icon: '✅', color: '#52c41a' },
  { value: 'ALIPAY', label: '支付宝', icon: '🔵', color: '#1677ff' },
  { value: 'WECHAT', label: '微信', icon: '💚', color: '#07c160' },
  { value: 'AGGREGATE', label: '聚合支付', icon: '🔗', color: '#722ed1' },
  { value: 'ABC', label: '中国农业银行', icon: '🏦', color: '#13a8a8' },
  { value: 'CCB', label: '中国建设银行', icon: '🏦', color: '#2f54eb' },
  { value: 'JD', label: '京东支付', icon: '🛒', color: '#eb2f96' },
]
const DEFAULT_ENABLED_METHODS = ['CASH', 'CARD', 'PREPAID', 'TRANSFER', 'ALIPAY', 'WECHAT']
const payMethodOptions = PAY_METHODS.map(m => ({ label: m.label, value: m.value }))

// ═══ 支付金额（按收款方式存放，组合收款自然支持）═══
const methodAmounts = reactive<Record<string, number>>(
  Object.fromEntries(PAY_METHODS.map(m => [m.value, 0]))
)
const enabledMethods = ref<string[]>([...DEFAULT_ENABLED_METHODS])
const primaryMethod = ref('CASH')
const secondMethod = ref('WECHAT')
const paymentQrCode = ref('')
const changeAmount = ref(0)
const combinedPayment = ref(false)

/** 启用中的收款方式（零售设置可配置，直接决定收款按钮与结算落库方式） */
const enabledPayMethods = computed(() => PAY_METHODS.filter(m => enabledMethods.value.includes(m.value)))

// ══ 优惠 ═══
const directDiscount = ref(0)
const promoDiscount = ref(0)
const prevPoints = ref(0)
const memberGeneratedPoints = ref(0)
const memberUsedPoints = ref(0)

/** 收款方式 → 主表汇总字段名（与 RetailOrder 实体一致） */
const AMOUNT_FIELD_BY_METHOD: Record<string, string> = {
  CASH: 'cashAmount', CARD: 'cardAmount', PREPAID: 'prepaidAmount', TRANSFER: 'transferAmount',
  ALIPAY: 'alipayAmount', WECHAT: 'wechatAmount', AGGREGATE: 'aggregateAmount',
  ABC: 'abcAmount', CCB: 'ccbAmount', JD: 'jdAmount',
}

/** 由已录入的收款方式推导支付方式（单方式原样，多方式 MIXED） */
function resolvePaymentMethod(): string {
  const used = enabledMethods.value.filter(m => Number(methodAmounts[m]) > 0)
  if (used.length === 0) return primaryMethod.value || 'CASH'
  return used.length === 1 ? used[0] : 'MIXED'
}

/** 结算用支付明细行（仅金额 > 0 的方式） */
function buildPayments() {
  return enabledMethods.value
    .filter(m => Number(methodAmounts[m]) > 0)
    .map(m => ({
      paymentMethod: m,
      paymentAmount: Number(methodAmounts[m]),
      paymentAccount: m === 'CASH' ? '' : (formData.paymentAccount1 || ''),
      remark: '',
    }))
}

// ── useBillForm ──
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  buildPayload,
  totalQuantity,
  totalAmount,
} = useBillForm({
  billPrefix: 'LSD',
  // 单据编号来自后端号段（GET /api/sales/retail/next-no），严禁前端演示自增号
  codeApiPath: '/sales/retail/next-no',
  api: {
    create: (data: any) => retailOrderApi.create({ order: data, items: data.items }),
    update: (id: any, data: any) => retailOrderApi.update(id, { order: data, items: data.items }),
    getById: (id: number) => retailOrderApi.getDetail(id).then((res: any) => res?.order || res),
  },
  redirectPath: '/sales/retail',
  optionTypes: ['warehouses', 'users', 'products'],
  productDefaults: PRODUCT_RETAIL_DEFAULTS,
  onDetailLoaded: (data: any) => {
    // 已结算单据：回填收款方式金额与支付明细
    const order = data?.order || data || {}
    PAY_METHODS.forEach(m => { methodAmounts[m.value] = Number(order[AMOUNT_FIELD_BY_METHOD[m.value]] || 0) })
    combinedPayment.value = !!order.combinedPayment
    paymentQrCode.value = order.paymentQrCode || ''
    const payments: any[] = data?.payments || []
    const paidMethods = payments.filter(p => Number(p.paymentAmount) > 0).map(p => p.paymentMethod)
    if (paidMethods.length) {
      primaryMethod.value = paidMethods[0]
      if (paidMethods[1]) secondMethod.value = paidMethods[1]
    }
    prevPoints.value = order.prevPoints || 0
    if (order.memberGeneratedPoints != null) memberGeneratedPoints.value = Number(order.memberGeneratedPoints)
    if (order.memberUsedPoints != null) memberUsedPoints.value = Number(order.memberUsedPoints)
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status: status ?? 1,
    retailNo: fd.orderNo || fd.retailNo || '',
    customerId: fd.customerId,
    customerName: fd.customerName,
    customerCode: fd.customerCode || '',
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    departmentId: fd.departmentId,
    departmentName: fd.departmentName,
    orderDate: fd.orderDate,
    saleType: fd.saleType || 'NORMAL',
    memberCardNo: fd.memberCardNo || '',
    memberName: fd.memberName || '',
    directDiscount: directDiscount.value,
    couponDiscount: fd.couponDiscount || 0,
    promoDiscount: promoDiscount.value,
    prevPoints: prevPoints.value,
    payableAmount: payableAmount.value,
    cashAmount: methodAmounts.CASH,
    cardAmount: methodAmounts.CARD,
    alipayAmount: methodAmounts.ALIPAY,
    wechatAmount: methodAmounts.WECHAT,
    aggregateAmount: methodAmounts.AGGREGATE,
    prepaidAmount: methodAmounts.PREPAID,
    transferAmount: methodAmounts.TRANSFER,
    abcAmount: methodAmounts.ABC,
    ccbAmount: methodAmounts.CCB,
    jdAmount: methodAmounts.JD,
    combinedPayment: combinedPayment.value,
    paymentMethod: resolvePaymentMethod(),
    changeAmount: changeAmount.value,
    totalReceived: totalPaid.value,
    paymentQrCode: paymentQrCode.value,
    cashierId: userStore?.userInfo?.id,
    cashierName: currentUserName.value || '系统',
    prepaidBalance: fd.prepaidBalance || 0,
    memberGeneratedPoints: memberGeneratedPoints.value,
    memberUsedPoints: memberUsedPoints.value,
    remark: fd.orderRemark,
    bankName: fd.bankName || '',
    bankAccount: fd.bankAccount || '',
    taxNo: fd.taxNo || '',
    extNum1: fd.extNum1, extNum2: fd.extNum2, extNum3: fd.extNum3, extNum4: fd.extNum4, extNum5: fd.extNum5,
    extText1: fd.extText1, extText2: fd.extText2, extText3: fd.extText3, extText4: fd.extText4, extText5: fd.extText5,
    items: fd.products.filter((p: any) => p.productId != null || p.productName).map((p: any) => ({
      productId: p.productId,
      productName: p.productName,
      productCode: p.itemCode || p.productCode || '',
      itemCode: p.itemCode,
      barcode: p.barcode,
      specification: p.specification || '',
      model: p.model || '',
      origin: p.origin || '',
      brand: p.brand || '',
      productAttribute: p.productAttribute || '',
      imageUrl: p.imageUrl || '',
      unit: p.unit,
      batchCode: p.batchCode,
      batchNo: p.batchNo || '',
      productionDate: p.productionDate,
      shelfLife: p.shelfLife,
      expiryDate: p.expiryDate,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      lineAmount: (p.quantity || 0) * (p.unitPrice || 0),
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      conversionRelation: p.conversionRelation || '',
      conversionResult: p.conversionResult || 0,
      smallUnit: p.smallUnit || '',
      smallUnitQuantity: p.smallUnitQuantity || 0,
      smallUnitPrice: p.smallUnitPrice || 0,
      discountRate: p.discountRate || 0,
      discountedPrice: p.discountedPrice || 0,
      discountedAmount: p.discountedAmount || 0,
      favorableDiscountRate: p.favorableDiscountRate || 0,
      favorableUnitPrice: p.favorableUnitPrice || 0,
      favorableAmount: p.favorableAmount || 0,
      retailPrice: p.retailPrice || 0,
      wholesalePrice: p.wholesalePrice || 0,
      minSalePrice: p.minSalePrice || 0,
      lastSalePrice: p.lastSalePrice || 0,
      lastSaleDate: p.lastSaleDate || null,
      priceRestaurant: p.priceRestaurant || 0,
      priceCanteen: p.priceCanteen || 0,
      priceVipSelf: p.priceVipSelf || 0,
      priceLargeGroup: p.priceLargeGroup || 0,
      priceSpecialCustomer: p.priceSpecialCustomer || 0,
      priceOutRestaurant: p.priceOutRestaurant || 0,
      priceVipLevel1: p.priceVipLevel1 || 0,
      priceVipLevel2: p.priceVipLevel2 || 0,
      availableStock: p.availableStock || 0,
      availableStockConverted: p.availableStockConverted || 0,
      bookStock: p.bookStock || 0,
      costPrice: p.costPrice || 0,
      costAmount: p.costAmount || 0,
      gift: p.gift || false,
      exchangePoints: p.exchangePoints || 0,
      generatedPoints: p.generatedPoints || 0,
      usedPoints: p.usedPoints || 0,
      remark: p.remark,
      extNum1: p.extNum1, extNum2: p.extNum2, extNum3: p.extNum3,
      extNum4: p.extNum4, extNum5: p.extNum5, extNum6: p.extNum6, extNum7: p.extNum7,
      extText1: p.extText1, extText2: p.extText2,
      extPartner: p.extPartner, extStaff: p.extStaff, extDept: p.extDept,
    })),
  }),
})

// ── 初始化零售单字段 ─
if (!('customerId' in formData)) {Object.assign(formData, {
  customerId: undefined,
  customerName: '散客',
  warehouseId: undefined,
  warehouseName: '',
  handlerId: undefined,
  handlerName: '',
  orderDate: new Date().toISOString().slice(0, 10),
  saleType: 'NORMAL',
  memberCardNo: '',
  memberName: '',
  orderRemark: '',
  prepaidBalance: 0,
})}

// ── 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const totalDiscount = computed(() => directDiscount.value + promoDiscount.value)
const payableAmount = computed(() => Math.max(0, totalAmount.value - totalDiscount.value))
/** 实收合计（仅统计启用中的收款方式） */
const totalPaid = computed(() => enabledMethods.value.reduce((s: number, m: string) => s + (Number(methodAmounts[m]) || 0), 0))
const prepaidBalanceDisplay = computed(() => (formData.prepaidBalance || 0).toFixed(2))

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value },
  { key: 'midPack', value: totalMidPack.value },
  { key: 'smallPack', value: totalSmallPack.value },
])

// ═══════════════════════════════════════
// 表单页配置（页面配置 36 字段 / 零售设置 / 打印设置）
// ═══════════════════════════════════════

const FORM_CONFIG_MODULE = 'retail-form'
const FORM_CONFIG_PAGE = 'form'

interface PageFieldSetting { key: string; label: string; visible: boolean }

/** 页面配置 36 字段（对标：零售单开发文档「页面配置 Tab」字段清单） */
const PAGE_FIELDS: PageFieldSetting[] = [
  { key: 'docNo', label: '编号', visible: true },
  { key: 'customerName', label: '客户', visible: true },
  { key: 'customerCode', label: '客户编号', visible: false },
  { key: 'bankName', label: '开户行', visible: false },
  { key: 'bankAccount', label: '银行账号', visible: false },
  { key: 'taxNo', label: '税号', visible: false },
  { key: 'memberCardNo', label: '会员卡号', visible: true },
  { key: 'warehouseId', label: '出库仓库', visible: true },
  { key: 'handlerId', label: '经手人', visible: true },
  { key: 'departmentId', label: '部门', visible: false },
  { key: 'orderDate', label: '单据日期', visible: true },
  { key: 'saleType', label: '销售类型', visible: true },
  { key: 'extNum1', label: '自定义字段1', visible: false },
  { key: 'extNum2', label: '自定义字段2', visible: false },
  { key: 'extNum3', label: '自定义字段3', visible: false },
  { key: 'extText1', label: '自定义字段4', visible: false },
  { key: 'extText2', label: '自定义字段5', visible: false },
  { key: 'productSearch', label: '商品', visible: true },
  { key: 'directDiscount', label: '直接优惠', visible: true },
  { key: 'couponDiscount', label: '优惠券', visible: true },
  { key: 'promoDiscount', label: '促销优惠', visible: true },
  { key: 'prevPoints', label: '此前积分', visible: true },
  { key: 'orderRemark', label: '单据备注', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'createTime', label: '制单时间', visible: true },
  { key: 'printCount', label: '打印次数', visible: true },
  { key: 'combinedPayment', label: '组合收款', visible: true },
  { key: 'payableAmount', label: '应收金额', visible: true },
  { key: 'availablePrepaid', label: '可用预收', visible: true },
  { key: 'prepaidBalance', label: '预收余额', visible: true },
  { key: 'changeAmount', label: '找零金额', visible: true },
  { key: 'payMethod1', label: '收款方式1', visible: true },
  { key: 'payAmount', label: '收款金额', visible: true },
  { key: 'payMethod', label: '收款方式', visible: true },
  { key: 'payQrCode', label: '收款码', visible: false },
  { key: 'cashierName', label: '收银员', visible: false },
]

const pageConfig = reactive<Record<string, { visible: boolean }>>(
  Object.fromEntries(PAGE_FIELDS.map(f => [f.key, { visible: f.visible }]))
)
const pageConfigFields = computed(() => PAGE_FIELDS.map((f, i) => ({
  key: f.key,
  index: i + 1,
  name: f.label,
  visible: pageConfig[f.key]?.visible !== false,
})))
const pageConfigTableColumns = [
  { title: '序号', dataIndex: 'index', key: 'index', width: 60 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '显示', dataIndex: 'visible', key: 'visible', width: 80, align: 'center' as const },
]
function isVisible(key: string): boolean {
  return pageConfig[key]?.visible !== false
}

// ── 零售设置（真实生效：默认值、收款方式、简易模式）──
const retailSettings = reactive({
  defaultWarehouseId: undefined as any,
  defaultHandlerId: undefined as any,
  defaultSaleType: 'NORMAL',
  pointPerYuan: 1,
  enableCombinedPayment: true,
  enabledMethods: [...DEFAULT_ENABLED_METHODS] as string[],
  autoSimpleMode: false,
})

// ── 打印设置 ──
const printSettings = reactive({
  printTemplate: 'standard',
  printCopies: 1,
  printPaperSize: 'A4',
  alwaysLastTemplate: false,
  printAfterSubmit: false,
})

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) pageConfig[k].visible = parsed.fields[k].visible !== false
      })
    }
    if (parsed.settings) Object.assign(retailSettings, parsed.settings)
    if (parsed.print) Object.assign(printSettings, parsed.print)
    if (Array.isArray(retailSettings.enabledMethods) && retailSettings.enabledMethods.length) {
      enabledMethods.value = [...retailSettings.enabledMethods]
    }
    combinedPayment.value = !!retailSettings.enableCombinedPayment
    simpleMode.value = !!retailSettings.autoSimpleMode
  } catch { /* 后端不可用时使用默认配置 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible }])),
    settings: { ...retailSettings },
    print: { ...printSettings },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败，保留本页内存态 */ }
}

function handlePageFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}

function handleEnabledMethodsChange(values: any) {
  retailSettings.enabledMethods = [...values]
  enabledMethods.value = [...values]
  // 关闭的方式金额清零，避免脏数据进入结算
  PAY_METHODS.forEach(m => { if (!values.includes(m.value)) methodAmounts[m.value] = 0 })
  updateChangeAmount()
  saveFormConfig()
}

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '零售单',
  orderNo: formData.orderNo || formData.retailNo,
  showAttachment: true,
  actions: [
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'print', label: '打印', icon: PrinterOutlined },
    { key: 'pos-mode', label: 'POS收银模式', icon: ThunderboltOutlined },
    { key: 'simple-mode', label: simpleMode.value ? '关闭简易' : '简易模式' },
    { key: 'keyboard', label: '键盘' },
    { key: 'more', label: '更多', children: [
      { key: 'copy-order', label: '复制零售单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const departmentOptions = ref<any[]>([])

const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  {
    key: 'customerName',
    label: '客户',
    type: 'input',
    inlineLabel: true,
    width: 220,
    placeholder: '散客',
    searchBtn: '+Q',
  },
  { key: 'customerCode', label: '客户编号', type: 'input', inlineLabel: true, width: 140 },
  { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 200, placeholder: '输入卡号/电话号码' },
  { key: 'warehouseId', label: '出库仓库', type: 'select', required: true, inlineLabel: true, width: 180 },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 160 },
  { key: 'departmentId', label: '部门', type: 'select', inlineLabel: true, width: 160 },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 160 },
  { key: 'saleType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 160, options: [{ label: '正常销售', value: 'NORMAL' }, { label: '退货', value: 'RETURN' }] },
]

/** 实际渲染的基本信息字段：受页面配置控制 + 注入真实下拉数据 */
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => isVisible(f.key))
    .map(f => ({
      ...f,
      options: f.key === 'warehouseId'
        ? (optionRefs.warehouses || []).map((w: any) => ({ label: w.name || w.warehouseName, value: w.id }))
        : f.key === 'handlerId'
          ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
          : f.key === 'departmentId'
            ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
            : (f as any).options,
      loading: ['warehouseId', 'handlerId', 'departmentId'].includes(f.key) ? loadingOptions.value : (f as any).loading,
      searchBtn: ['warehouseId', 'handlerId', 'departmentId'].includes(f.key) ? '+Q' : (f as any).searchBtn,
    }))
)

/** 优惠行是否需要渲染 */
const showDiscountRow = computed(() =>
  isVisible('directDiscount') || isVisible('couponDiscount') || isVisible('promoDiscount') || isVisible('prevPoints')
)

/** 扩展信息区是否需要渲染（开户行/银行账号/税号/自定义字段） */
const showExtInfo = computed(() =>
  ['bankName', 'bankAccount', 'taxNo', 'extNum1', 'extNum2', 'extNum3', 'extText1', 'extText2'].some(k => isVisible(k))
)

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '零售数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '应收金额', value: payableAmount.value.toFixed(2), highlight: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单应收',
  amountValue: `\u00a5${payableAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '挂单',
  draftShortcut: 'F3',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置
// ══════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => { return ([
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  // 图片列
  { key: 'imageUrl', title: '图片', type: 'slot', slotName: 'imageUrlCell', width: 60 },
  // 商品信息(8列)
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name,
      value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true,
  },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 80 },
  { key: 'origin', title: '产地', type: 'input', width: 80 },
  { key: 'productAttribute', title: '商品行属性', type: 'input', width: 90 },
  // 库存(3列)
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算', type: 'number', width: 110, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2 },
  // 批次(4列)
  { key: 'batchCode', title: '批次条码', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 70 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  // 数量/包装(6列)
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'unit', title: '计价单位', type: 'input', width: 70 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 70, precision: 0 },
  // 价格(3列)
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  // 小单位(3列)
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  // 折扣(6列)
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 70, precision: 2 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 90, precision: 2 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 100, precision: 2 },
  { key: 'favorableDiscountRate', title: '优惠折扣(%)', type: 'number', width: 90, precision: 2 },
  { key: 'favorableUnitPrice', title: '惠后单价', type: 'number', width: 90, precision: 2 },
  { key: 'favorableAmount', title: '优惠后金额', type: 'number', width: 100, precision: 2 },
  // 市场价格(5列)
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2 },
  { key: 'lastSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 120 },
  // 8价格等级
  { key: 'priceRestaurant', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceCanteen', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceVipSelf', title: '自助VIP', type: 'number', width: 80, precision: 2 },
  { key: 'priceLargeGroup', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceSpecialCustomer', title: '特价客户', type: 'number', width: 80, precision: 2 },
  { key: 'priceOutRestaurant', title: '外围餐饮店', type: 'number', width: 90, precision: 2 },
  { key: 'priceVipLevel1', title: '重点VIP01', type: 'number', width: 90, precision: 2 },
  { key: 'priceVipLevel2', title: '连锁VIP', type: 'number', width: 80, precision: 2 },
  // 成本(2列)
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2 },
  // 已收(1列)
  { key: 'receivedQuantity', title: '已收数量', type: 'number', width: 80, precision: 2 },
  // 积分(4列)
  { key: 'exchangePoints', title: '兑换礼品', type: 'input', width: 80 },
  { key: 'exchangePointsVal', title: '兑换积分', type: 'number', width: 80, precision: 2 },
  { key: 'generatedPoints', title: '产生积分', type: 'number', width: 80, precision: 2 },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 80, precision: 2 },
  // 赠品/备注(3列)
  { key: 'gift', title: '赠品', type: 'boolean', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 120 },
  // 自定义字段(12列)
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 110 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 110 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'number', width: 120 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'number', width: 100 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'number', width: 100 },
  // 简易模式下隐藏的列key列表
] satisfies DetailColumnConfig[]).filter((col) => {
  if (!simpleMode.value) return true
  const hiddenKeys = [
    'productAttribute', 'availableStockConverted', 'bookStock',
    'batchCode', 'productionDate', 'shelfLife', 'expiryDate',
    'conversionRelation', 'pieceQuantity', 'bigPack', 'midPack', 'smallPack',
    'smallUnitPrice', 'retailPrice', 'wholesalePrice', 'minSalePrice',
    'lastSalePrice', 'lastSaleDate', 'smallUnit', 'smallUnitQuantity',
    'discountRate', 'discountedPrice', 'discountedAmount',
    'favorableDiscountRate', 'favorableUnitPrice', 'favorableAmount',
    'costPrice', 'costAmount',
    'priceRestaurant', 'priceCanteen', 'priceVipSelf', 'priceLargeGroup',
    'priceSpecialCustomer', 'priceOutRestaurant', 'priceVipLevel1', 'priceVipLevel2',
    'gift', 'exchangePoints', 'usedPoints', 'generatedPoints',
    'extNum1', 'extNum2', 'extNum3', 'extText1', 'extText2',
    'extNum4', 'extNum5', 'extPartner', 'extStaff', 'extDept',
  ]
  return !hiddenKeys.includes(col.key)
}); })

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '', barcode: '', unit: '',
    batchCode: '', productionDate: '', shelfLife: '', expiryDate: '',
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, scanMode: false, quantity: 0,
    productName: '', remark: '',
  }
  formData.products.splice(index + 1, 0, newProduct)
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  updateChangeAmount()
}

function updateChangeAmount() {
  const change = totalPaid.value - payableAmount.value
  changeAmount.value = change > 0 ? Number(change.toFixed(2)) : 0
}

// 应收变化时同步找零与产生积分。
// ⚠️ 2026-09-26：原先固定按「1 元 = 1 分」硬编码，而配置面板的「每元积分数」
// （retailSettings.pointPerYuan）存了值却从不参与计算 —— 勾了不生效。现按配置乘算，
// 默认值 1 时与后端结算口径一致（历史行为不变）。
watch([payableAmount, () => retailSettings.pointPerYuan], ([v, perYuan]) => {
  memberGeneratedPoints.value = Math.floor(v * (Number(perYuan) || 0))
  updateChangeAmount()
}, { immediate: true })

/**
 * 根据客户价格等级自动匹配商品单价
 * 8个标准化价格等级映射到产品字段
 */
function getPriceByCustomerLevel(product: any, level: string): number {
  const priceLevelMap: Record<string, string> = {
    'RESTAURANT': 'priceRestaurant',
    'CANTEEN': 'priceCanteen',
    'VIP_SELF': 'priceVipSelf',
    'LARGE_GROUP': 'priceLargeGroup',
    'SPECIAL_CUSTOMER': 'priceSpecialCustomer',
    'OUT_RESTAURANT': 'priceOutRestaurant',
    'VIP_LEVEL1': 'priceVipLevel1',
    'VIP_LEVEL2': 'priceVipLevel2',
    'MEMBER': 'retailPrice',
    'ENTERPRISE': 'wholesalePrice',
    'VIP': 'retailPrice',
  }
  const field = priceLevelMap[level] || ''
  if (field && product[field] != null && product[field] > 0) return product[field]
  // 回退优先级：retailPrice > wholesalePrice > standardPrice > salePrice > price > 0
  return product.retailPrice || product.wholesalePrice || product.standardPrice || product.salePrice || product.price || 0
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) applyProductToRow(record, p)
  } else if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    record.amount = (record.quantity || 0) * (record.unitPrice || 0)
    updateChangeAmount()
  }
}

/**
 * 商品搜索（F1 / 扫码）：优先走后端零售商品快查（返回真实零售价与所选仓库可用库存），
 * 后端不可用时回退到已加载的商品选项。
 */
async function handleProductSearch() {
  const keyword = productSearchText.value.trim()
  if (!keyword) return
  let product: any = null
  try {
    const list: any = await retailOrderApi.quickSearchProducts(keyword, formData.warehouseId)
    const hit = Array.isArray(list) ? list[0] : null
    if (hit) {
      product = {
        id: hit.id,
        name: hit.productName,
        code: hit.productCode,
        barcode: hit.barcode,
        specification: hit.specification,
        model: hit.model,
        origin: hit.origin,
        brand: hit.brand,
        unit: hit.unit,
        stock: hit.availableStock,
        image: hit.imageUrl,
        retailPrice: hit.retailPrice,
        wholesalePrice: hit.wholesalePrice,
        costPrice: hit.costPrice,
      }
    }
  } catch { /* 回退到本地选项 */ }
  if (!product) {
    const kw = keyword.toLowerCase()
    product = optionRefs.products.find((p: any) =>
      (p.name || '').toLowerCase().includes(kw) ||
      (p.code || '').toLowerCase().includes(kw) ||
      (p.barcode || '').toLowerCase().includes(kw)
    )
  }
  if (!product) {
    message.warning('未找到匹配商品')
    return
  }
  const emptyIdx = formData.products.findIndex((p: any) => !p.productId && !p.productName)
  if (emptyIdx < 0) handleAddProduct()
  const row = formData.products[emptyIdx >= 0 ? emptyIdx : formData.products.length - 1]
  applyProductToRow(row, product)
  productSearchText.value = ''
  message.success(`已添加：${product.name}`)
}

/** 商品快照 + 价格等级取价 + 扫码默认数量 1，写入明细行 */
function applyProductToRow(row: any, product: any) {
  row.productId = product.id
  row.productName = product.name || product.productName || ''
  row.productCode = product.code || product.productCode || ''
  row.itemCode = product.code || product.productCode || ''
  row.barcode = product.barcode || ''
  row.specification = product.specification || product.spec || ''
  row.model = product.model || ''
  row.origin = product.origin || ''
  row.brand = product.brand || ''
  row.unit = product.unit || ''
  row.imageUrl = product.image || product.imageUrl || ''
  row.availableStock = product.stock ?? product.availableStock ?? 0
  row.costPrice = product.costPrice || 0
  row.retailPrice = product.retailPrice || 0
  row.wholesalePrice = product.wholesalePrice || 0
  row.unitPrice = getPriceByCustomerLevel(product, customerLevel.value)
  if (!row.quantity) row.quantity = 1
  row.amount = (row.quantity || 0) * (row.unitPrice || 0)
  updateChangeAmount()
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'customerName') {
    showMemberSearch.value = true
    memberSearchKeyword.value = ''
    memberSearchResults.value = []
    selectedMemberId.value = null
    selectedMember.value = null
  } else if (fieldKey === 'warehouseId') {
    message.info('请在仓库下拉列表中搜索')
  } else if (fieldKey === 'handlerId') {
    message.info('请在经手下拉列表中搜索')
  }
}

// ═══ 会员搜索 ═══
async function doMemberSearch() {
  if (!memberSearchKeyword.value.trim()) return
  memberSearching.value = true
  try {
    memberSearchResults.value = await memberApi.search(memberSearchKeyword.value)
  } catch {
    memberSearchResults.value = []
  } finally {
    memberSearching.value = false
  }
}

function confirmMemberSelect() {
  if (selectedMember.value) {
    const m = selectedMember.value
    formData.customerId = m.id
    formData.customerName = m.partyName || m.name || '会员'
    formData.memberCardNo = m.memberCardNo || ''
    formData.memberName = m.partyName || m.name || ''
    // 存储客户价格等级
    customerLevel.value = m.partyLevel || m.customerLevel || ''
    prevPoints.value = m.points || 0
    message.success(`已选择会员：${formData.customerName}`)
  } else {
    formData.customerId = undefined
    formData.customerName = '散客'
    formData.memberCardNo = ''
    formData.memberName = ''
    customerLevel.value = ''
  }
  showMemberSearch.value = false
}

// ═══ 产品选择弹窗 ═══
function handleOpenProductSelectModal(record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length
  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) handleAddProduct()
  }
  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      applyProductToRow(formData.products[rowIndex], p)
    }
  })
  showProductSelect.value = false
  updateChangeAmount()
  message.success(`已选择 ${products.length} 个商品`)
}

// ═══ 收款按钮点击 ═══
function focusPayment(method: string) {
  if (method === 'PREPAID' && Number(formData.prepaidBalance || 0) <= 0) {
    message.warning('该客户当前无可用预收款')
    return
  }
  const payable = payableAmount.value
  if (!combinedPayment.value) {
    // 非组合收款：单选一种方式，清空其它
    enabledMethods.value.forEach(m => { methodAmounts[m] = 0 })
    methodAmounts[method] = method === 'PREPAID'
      ? Math.min(payable, Number(formData.prepaidBalance || 0))
      : payable
  } else {
    // 组合收款：补足剩余应收
    const others = enabledMethods.value
      .filter(m => m !== method)
      .reduce((s, m) => s + Number(methodAmounts[m] || 0), 0)
    let remain = Math.max(0, payable - others)
    if (method === 'PREPAID') remain = Math.min(remain, Number(formData.prepaidBalance || 0))
    methodAmounts[method] = remain
  }
  updateChangeAmount()
}

/** 清空全部收款 */
function clearPayments() {
  enabledMethods.value.forEach(m => { methodAmounts[m] = 0 })
  updateChangeAmount()
}

/** 组合收款开关：关闭时只保留一种收款方式 */
function handleCombinedChange() {
  if (!combinedPayment.value) {
    const current = resolvePaymentMethod()
    const keep = current === 'MIXED' ? primaryMethod.value : current
    enabledMethods.value.forEach(m => { if (m !== keep) methodAmounts[m] = 0 })
  }
  updateChangeAmount()
}

// ═══ 挂单（F3）：先落草稿，再调后端 /hold 置为挂单状态 ═══
async function handleHoldOrder() {
  if (!ensureProducts()) return
  saving.value = true
  try {
    const id = await persistOrder()
    await retailOrderApi.hold(id)
    message.success('已挂单，可在零售单列表中「取单」继续收银')
    router.push('/sales/retail')
  } catch (e: any) {
    message.error(e?.message || '挂单失败')
  } finally {
    saving.value = false
  }
}

/** 明细校验：至少一行有效商品 */
function ensureProducts(): boolean {
  const valid = (formData.products || []).filter((p: any) => p.productId != null || p.productName)
  if (valid.length === 0) {
    message.warning('请先添加商品明细')
    return false
  }
  return true
}

/** 保存单据（新建或更新），返回订单ID —— 结算/挂单均以此为基础 */
async function persistOrder(status = 0): Promise<any> {
  const payload = buildPayload(status)
  const editId = route.query.id || route.params.id
  let order: any
  if (editId) {
    order = await retailOrderApi.update(editId, { order: payload, items: payload.items })
  } else {
    order = await retailOrderApi.create({ order: payload, items: payload.items })
  }
  const id = order?.id ?? editId
  if (!id) throw new Error('单据保存失败，未获取到单据ID')
  formData.id = id
  formData.orderNo = order?.retailNo || payload.retailNo || formData.orderNo
  formData.retailNo = formData.orderNo
  return id
}

// ═══ 记账（Ctrl+Enter）：先落单，再由后端 /settle 完成收款落库 + 库存扣减 ═══
async function handleSettle() {
  if (payableAmount.value > 0 && totalPaid.value < payableAmount.value) {
    message.warning(`收款不足：应收 ${payableAmount.value.toFixed(2)}，已收 ${totalPaid.value.toFixed(2)}`)
    return
  }
  if (!ensureProducts()) return
  if (payableAmount.value > 0 && buildPayments().length === 0) {
    message.warning('请选择收款方式并录入收款金额')
    return
  }
  saving.value = true
  try {
    const id = await persistOrder(0)
    await retailOrderApi.settle(id, buildPayments())
    message.success('结算成功，已完成收款与库存扣减')
    // 「保存后立即打印」：勾选时留在本页出小票，否则回列表。
    // ⚠️ 原先这个开关只在配置面板里存了个值、没有任何消费点（勾了不生效）。
    if (printSettings.printAfterSubmit) {
      setTimeout(() => handlePrint(), 500)
    } else {
      router.push('/sales/retail')
    }
  } catch (e: any) {
    message.error(e?.message || '结算失败')
  } finally {
    saving.value = false
  }
}

// ═══ 复制零售单 ═══
async function handleCopyOrder() {
  if (!formData.id) {
    // 未保存的单据，先保存再复制
    message.info('请先保存当前单据后再复制')
    return
  }
  try {
    const newOrder = await retailOrderApi.copy(formData.id)
    if (newOrder?.id) {
      message.success('复制成功，正在打开新单据')
      router.push(`/sales/retail/form?id=${newOrder.id}`)
    } else {
      message.success('复制成功，可新建单据查看')
      router.push('/sales/retail/form')
    }
  } catch (e: any) {
    message.error('复制失败: ' + (e.message || e))
  }
}

// ═══ POS 模式切换 ═══
function enterPosMode() {
  posMode.value = true
}

function exitPosMode() {
  posMode.value = false
}

function handlePosProductSelect(rowIndex: number) {
  currentSelectRowIndex.value = rowIndex
  showProductSelect.value = true
}

// ═══ POS 零售单历史 ═══
function statusColor(status: number): string {
  const map: Record<number, string> = { 0: 'default', 1: 'green', 2: 'orange', 3: 'red' }
  return map[status] || 'default'
}
function statusLabel(status: number): string {
  const map: Record<number, string> = { 0: '草稿', 1: '已完成', 2: '挂单', 3: '已作废' }
  return map[status] || '未知'
}
async function handleShowOrderHistory() {
  showOrderHistory.value = true
  if (orderHistoryList.value.length > 0) return
  loadingOrderHistory.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const res = await retailOrderApi.pageByDoc({
      pageNum: 1,
      pageSize: 50,
      dateStart: today,
      dateEnd: today,
    })
    orderHistoryList.value = res?.records || res?.data?.records || []
  } catch (e: any) {
    message.error('获取零售单历史失败: ' + (e.message || e))
  } finally {
    loadingOrderHistory.value = false
  }
}
function handleViewHistoryOrder(item: any) {
  showOrderHistory.value = false
  if (item.id) {
    router.push(`/sales/retail/form?id=${item.id}`)
  }
}

// ═══ 键盘快捷键 ══
function handleKeyDown(e: KeyboardEvent) {
  if (!posMode.value) return

  // Shift+Esc: 退出 POS 模式
  if (e.shiftKey && e.key === 'Escape') {
    e.preventDefault()
    exitPosMode()
    return
  }

  // F2: 零售单历史
  if (e.key === 'F2') {
    e.preventDefault()
    handleShowOrderHistory()
    return
  }

  // F3: 挂单
  if (e.key === 'F3') {
    e.preventDefault()
    handleHoldOrder()
    return
  }

  // F6: 会员卡号搜索
  if (e.key === 'F6') {
    e.preventDefault()
    showMemberSearch.value = true
    return
  }

  // F10: 快捷键面板
  if (e.key === 'F10') {
    e.preventDefault()
    showShortcutPanel.value = true
    return
  }

  // 空格键: 收款
  if (e.key === ' ' && !e.target || (e.target instanceof HTMLElement && !['INPUT', 'TEXTAREA', 'BUTTON'].includes(e.target.tagName))) {
    e.preventDefault()
    handleSettle()
    return
  }

  // ↑↓: 切换选中行
  if (e.key === 'ArrowUp') {
    e.preventDefault()
    posSelectedRow.value = Math.max(0, posSelectedRow.value - 1)
    return
  }
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    posSelectedRow.value = Math.min(posDisplayRows.value.length - 1, posSelectedRow.value + 1)
    return
  }
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    case 'pos-mode':
      enterPosMode()
      break
    case 'simple-mode':
      simpleMode.value = !simpleMode.value
      message.success(simpleMode.value ? '简易模式已开启 - 已自动隐藏扩展字段' : '简易模式已关闭 - 恢复完整显示')
      break
    case 'keyboard':
      message.info('快捷键: F1=搜索商品, F2=历史, F3=挂单, F6=会员, Space=收款, ↑↓=切换, Shift+Esc=退出POS')
      break
    case 'copy-order':
      handleCopyOrder()
      break
    case 'export':
      handleExport()
      break
  }
}

// ── 打印：渲染当前单据（含明细与小票金额）后调用浏览器打印，并累加打印次数 ──
async function handlePrint() {
  const rows = (formData.products || [])
    .filter((p: any) => p.productId != null || p.productName)
    .map((p: any, i: number) => `<tr>
        <td>${i + 1}</td><td>${escapeHtml(p.productName || '')}</td><td>${escapeHtml(p.barcode || '')}</td>
        <td class="num">${fmtNum(p.quantity)}</td><td class="num">${fmtNum(p.unitPrice)}</td><td class="num">${fmtNum((p.quantity || 0) * (p.unitPrice || 0))}</td>
      </tr>`).join('')
  const payRows = buildPayments()
    .map(p => `<tr><td>${payMethodLabel(p.paymentMethod)}</td><td class="num">${fmtNum(p.paymentAmount)}</td></tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>零售单 ${escapeHtml(formData.orderNo || '')}</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}.num{text-align:right}
      .totals{margin-top:8px;font-size:13px;text-align:right}.totals span{margin-left:16px}
    </style></head><body>
    <h2>零售单</h2>
    <div class="meta">
      <span>单据编号：${escapeHtml(formData.orderNo || '')}</span>
      <span>单据日期：${escapeHtml(formData.orderDate || '')}</span>
      <span>客户：${escapeHtml(formData.customerName || '散客')}</span>
      <span>仓库：${escapeHtml(formData.warehouseName || '')}</span>
      <span>经手人：${escapeHtml(formData.handlerName || '')}</span>
      <span>制单人：${escapeHtml(formData.creatorName || currentUserName.value || '')}</span>
    </div>
    <table><thead><tr><th>#</th><th>商品名称</th><th>条码</th><th>数量</th><th>单价</th><th>金额</th></tr></thead>
    <tbody>${rows || '<tr><td colspan="6">无明细</td></tr>'}</tbody></table>
    <div class="totals">
      <span>商品金额：${fmtNum(totalAmount.value)}</span>
      <span>优惠金额：${fmtNum(totalDiscount.value)}</span>
      <span>本单应收：${fmtNum(payableAmount.value)}</span>
      <span>收款合计：${fmtNum(totalPaid.value)}</span>
      <span>找零：${fmtNum(changeAmount.value)}</span>
    </div>
    ${payRows ? `<table style="margin-top:8px"><thead><tr><th>收款方式</th><th>金额</th></tr></thead><tbody>${payRows}</tbody></table>` : ''}
    </body></html>`
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(`${html}`)
  win.document.close()
  win.focus()
  win.print()
  if (formData.id) {
    try {
      await retailOrderApi.afterPrint(formData.id)
      formData.printCount = (formData.printCount || 0) + 1
    } catch { /* 计数失败不影响打印 */ }
  }
}

// ── 导出：当前单据明细导出 CSV（真实数据） ──
function handleExport() {
  const rows = (formData.products || []).filter((p: any) => p.productId != null || p.productName)
  if (rows.length === 0) {
    message.warning('没有可导出的明细')
    return
  }
  const headers = ['商品名称', '货号', '条码', '规格', '型号', '产地', '单位', '数量', '单价', '金额', '折扣(%)', '折后单价', '折后金额', '优惠后金额', '备注']
  const escapeCsv = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const body = rows.map((p: any) => [
    p.productName, p.itemCode, p.barcode, p.specification, p.model, p.origin, p.unit,
    p.quantity, p.unitPrice, (p.quantity || 0) * (p.unitPrice || 0),
    p.discountRate, p.discountedPrice, p.discountedAmount, p.favorableAmount, p.remark,
  ].map(escapeCsv).join(','))
  const csv = [headers.join(','), ...body].join('\r\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `零售单明细_${formData.orderNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success(`导出成功（${rows.length} 行）`)
}

function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string))
}
function fmtNum(v: any): string {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function payMethodLabel(method: string): string {
  return PAY_METHODS.find(m => m.value === method)?.label || method || ''
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(async () => {
  await loadFormConfig()
  // 部门下拉（基本信息「部门」字段）
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
  // 初始化默认散客
  try {
    const walkin = await memberApi.getWalkIn()
    if (walkin && !formData.customerId) {
      formData.customerId = walkin.id
      formData.customerName = walkin.partyName || '散客'
    }
  } catch {
    formData.customerName = '散客'
  }
  // 零售设置默认值生效（仅新建单据时）
  const editId = route.query.id || route.params.id
  if (!editId) {
    if (retailSettings.defaultWarehouseId && !formData.warehouseId) formData.warehouseId = retailSettings.defaultWarehouseId
    if (retailSettings.defaultHandlerId && !formData.handlerId) formData.handlerId = retailSettings.defaultHandlerId
    if (retailSettings.defaultSaleType) formData.saleType = retailSettings.defaultSaleType
    formData.cashierName = currentUserName.value || '系统'
  }

  if (formData.products.length === 0) {
    for (let i = 0; i < 12; i++) handleAddProduct()
  }
  // 键盘快捷键监听
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})
</script>

<style scoped>
.retail-form-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ═══ 商品搜索行 ══ */
.product-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-bottom: none;
  padding-left: 12px;
}
.search-label {
  font-size: 13px;
  color: #595959;
  white-space: nowrap;
  font-weight: 500;
}
.product-search-input {
  width: 260px;
}

/* ═══ 操作按钮 ═══ */
.action-add-btn {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
}
.action-del-btn {
  color: #ff4d4f;
  padding: 0;
  font-size: 14px;
}

/* ═══ 底部左右布局 ═══ */
.retail-bottom-layout {
  display: flex;
  gap: 16px;
  padding: 12px 0 0;
}
.retail-bottom-left {
  flex: 1;
  min-width: 0;
}
/* ═══ 扩展信息区 ═══ */
.ext-info-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 6px 12px;
  margin-bottom: 6px;
}
.ext-field { display: flex; align-items: center; gap: 6px; min-width: 0; }
.ext-label { font-size: 12px; color: #595959; white-space: nowrap; }
.ext-field :deep(.ant-input), .ext-field :deep(.ant-input-number) { flex: 1; min-width: 0; }

/* ═══ 配置弹窗 ═══ */
.config-hint { font-size: 12px; color: #8c8c8c; margin: 0 0 8px; }

.retail-payment-panel {
  width: 320px;
  flex-shrink: 0;
  background: #fafbfc;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
}

/* ═══ 优惠行 ═══ */
.discount-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.discount-field {
  display: flex;
  align-items: center;
  gap: 4px;
}
.discount-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
}
.discount-value {
  font-size: 13px;
  color: #262626;
  min-width: 40px;
}

/* ═══ 备注 ═══ */
.remark-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.remark-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  min-width: 60px;
}
.remark-input {
  flex: 1;
}

/* ═══ 单据信息 ═══ */
.doc-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
  font-size: 12px;
  color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
}
.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}

/* ═══ 收款面板 ═══ */
.payment-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 10px;
}
.payment-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}
.pay-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 52px;
  border: 2px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  transition: all 0.2s;
  padding: 4px;
}
.pay-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.pay-btn.active {
  border-color: #262626;
  box-shadow: 0 0 0 2px rgba(0,0,0,0.1);
}
.pay-btn-icon {
  font-size: 18px;
  line-height: 1;
  margin-bottom: 2px;
}
.pay-btn-cash {
  background: #fa8c16;
}
.pay-btn-card {
  background: #faad14;
}
.pay-btn-prepaid {
  background: #1890ff;
}
.pay-btn-transfer-green {
  background: #52c41a;
}
.pay-btn-alipay {
  background: #1677ff;
}
.pay-btn-wechat {
  background: #07c160;
}
.combined-payment-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #595959;
  margin-left: 4px;
  cursor: pointer;
}

/* ══ 收款明细 ═══ */
.payment-details {
  margin-top: 8px;
  border-top: 1px solid #e8e8e8;
  padding-top: 8px;
}
.payment-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}
.payment-detail-label {
  color: #8c8c8c;
  min-width: 60px;
}
.payment-detail-value {
  color: #262626;
  font-weight: 500;
  text-align: right;
  min-width: 60px;
}
.payment-input-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}
.payment-input-label {
  font-size: 11px;
  color: #8c8c8c;
}
.payment-input-value {
  font-size: 12px;
  color: #262626;
}
.payment-input-unit {
  font-size: 11px;
  color: #8c8c8c;
}

/* ═══ 辅助样式 ══ */
.text-red {
  color: #ff4d4f !important;
}
.member-selected {
  background: #e6f7ff;
}

/* ══ POS 按钮样式（header 第一个按钮）═══════ */
:deep(.bill-header .header-right .ant-btn:first-child) {
  background: #ff4d4f !important;
  color: #fff !important;
  border-color: #ff4d4f !important;
  font-weight: 600;
}

:deep(.bill-header .header-right .ant-btn:first-child:hover) {
  background: #ff7875 !important;
  border-color: #ff7875 !important;
  color: #fff !important;
}

/* ═══════════════════════════════════════
   POS 收银模式全屏覆盖层（暗色主题）
   ═══════════════════════════════════════ */
.pos-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: #1a1a1a;
  display: flex;
  flex-direction: column;
  color: #e0e0e0;
  font-size: 14px;
}

/* POS 顶部栏 */
.pos-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #2d2d2d;
  border-bottom: 1px solid #444;
  flex-shrink: 0;
}

.pos-exit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #ff4d4f;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.pos-exit-btn:hover {
  background: #ff7875;
}

.pos-exit-icon {
  font-size: 14px;
}

.pos-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  flex: 1;
  text-align: center;
}

.pos-header-actions {
  display: flex;
  gap: 8px;
}

.pos-shortcut-btn {
  border: none;
  border-radius: 4px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.pos-shortcut-btn:hover {
  opacity: 0.85;
}

.pos-shortcut-f10 {
  background: #5b6abf;
  color: #fff;
}

.pos-shortcut-f2 {
  background: #52c41a;
  color: #fff;
}

/* POS 商品表格 */
.pos-table-container {
  flex: 1;
  overflow: auto;
  padding: 0;
}

.pos-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.pos-table thead th {
  background: #2d2d2d;
  color: #ccc;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 6px;
  border-bottom: 2px solid #444;
  text-align: left;
  position: sticky;
  top: 0;
  z-index: 10;
}

.pos-th-settings {
  width: 36px;
  text-align: center;
  color: #888;
}

.pos-th-action {
  width: 70px;
}

.pos-th-product {
  width: 25%;
}

.pos-th-barcode {
  width: 12%;
}

.pos-th-stock {
  width: 8%;
}

.pos-th-qty {
  width: 8%;
}

.pos-th-price {
  width: 10%;
}

.pos-th-amount {
  width: 12%;
  color: #ff7a45;
}

.pos-tr {
  transition: background 0.15s;
}

.pos-tr-first {
  background: #e8a87c !important;
}

.pos-tr-first .pos-td {
  color: #262626;
  font-weight: 500;
}

.pos-tr-selected {
  background: #c97b4f !important;
}

.pos-tr-selected .pos-td {
  color: #fff;
}

.pos-tr-empty {
  opacity: 0.5;
}

.pos-tr-empty:hover {
  background: #3a3a3a;
  opacity: 1;
}

.pos-tr:not(.pos-tr-first):not(.pos-tr-selected):not(.pos-tr-empty):hover {
  background: #333;
}

.pos-td {
  padding: 6px 8px;
  border-bottom: 1px solid #3a3a3a;
  font-size: 13px;
  color: #ccc;
}

.pos-td-action {
  display: flex;
  gap: 4px;
  align-items: center;
}

.pos-action-btn {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s;
}

.pos-action-btn:hover {
  transform: scale(1.15);
}

.pos-action-add {
  background: #52c41a;
  color: #fff;
}

.pos-action-del {
  background: #ff4d4f;
  color: #fff;
}

.pos-td-product {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pos-product-name {
  color: #e0e0e0;
}

.pos-empty-cell {
  color: #666;
  font-style: italic;
  cursor: pointer;
}

.pos-empty-cell:hover {
  color: #aaa;
}

.pos-input {
  width: 60px;
  background: #3a3a3a;
  border: 1px solid #555;
  color: #e0e0e0;
  border-radius: 3px;
  padding: 2px 6px;
  font-size: 13px;
  text-align: right;
}

.pos-input:focus {
  border-color: #ff7a45;
  outline: none;
}

.pos-td-qty input {
  width: 50px;
}

.pos-td-price,
.pos-td-amount {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

/* POS 表格合计行 */
.pos-tr-total {
  background: #2d2d2d;
  border-top: 2px solid #555;
}

.pos-tr-total .pos-td {
  font-weight: 600;
  color: #e0e0e0;
}

.pos-td-footer-label {
  color: #e0e0e0;
  font-weight: 700;
}

.pos-td-total-val {
  text-align: right;
  color: #ff7a45;
  font-weight: 700;
}

.pos-td-total-amount {
  font-size: 15px;
  color: #ff7a45;
}

/* POS 底部栏 */
.pos-bottom-bar {
  display: flex;
  align-items: stretch;
  background: #2d2d2d;
  border-top: 1px solid #444;
  flex-shrink: 0;
  min-height: 70px;
}

.pos-bottom-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 16px;
  gap: 4px;
}

.pos-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.pos-info-label {
  color: #888;
  min-width: 80px;
}

.pos-info-value {
  color: #e0e0e0;
  font-weight: 500;
}

.pos-info-placeholder {
  color: #666;
  font-style: italic;
}

.pos-info-points {
  margin-left: auto;
  color: #888;
  font-size: 12px;
}

.pos-points-value {
  color: #ff7a45;
}

.pos-info-btn {
  background: none;
  border: none;
  color: #aaa;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 6px;
}

.pos-info-btn:hover {
  color: #fff;
}

.pos-bottom-actions {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}

.pos-action-btn-large {
  border: none;
  padding: 12px 28px;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1.3;
  transition: opacity 0.2s;
  min-width: 100px;
  height: 70px;
}

.pos-action-btn-large:hover {
  opacity: 0.9;
}

.pos-btn-hold {
  background: #faad14;
  color: #fff;
}

.pos-btn-settle {
  background: #ff7a45;
  color: #fff;
}

.pos-shortcut-text {
  font-size: 11px;
  font-weight: 400;
  opacity: 0.8;
}

.pos-total-display {
  display: flex;
  align-items: baseline;
  background: #ff7a45;
  color: #fff;
  padding: 0 24px;
  height: 70px;
  align-items: center;
  gap: 4px;
  min-width: 120px;
}

.pos-total-symbol {
  font-size: 20px;
  font-weight: 400;
}

.pos-total-amount {
  font-size: 32px;
  font-weight: 700;
}

/* ═══ 简易模式 ═══ */
.retail-bottom-simple {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  width: 100%;
}
.simple-amount-label {
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}
.simple-amount-value {
  font-size: 16px;
  font-weight: 700;
  margin-left: 4px;
}
.text-red {
  color: #ff4d4f;
}

/* ═══ POS 快捷键面板 ═══ */
.pos-shortcut-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.pos-shortcut-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 4px;
  transition: background 0.2s;
}
.pos-shortcut-item:hover {
  background: #f0f5ff;
}
.pos-shortcut-key {
  display: inline-block;
  min-width: 90px;
  padding: 2px 10px;
  background: #262626;
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  font-family: 'Courier New', monospace;
}
.pos-shortcut-desc {
  font-size: 14px;
  color: #595959;
}
:deep(.pos-shortcut-modal .ant-modal-header) {
  background: #2d2d2d;
  border-bottom: 1px solid #444;
}
:deep(.pos-shortcut-modal .ant-modal-title) {
  color: #e0e0e0;
}
:deep(.pos-shortcut-modal .ant-modal-content) {
  background: #1a1a1a;
}
:deep(.pos-shortcut-modal .ant-modal-close) {
  color: #888;
}

/* ═══ POS 零售单历史面板 ═══ */
:deep(.pos-history-modal .ant-modal-header) {
  background: #2d2d2d;
  border-bottom: 1px solid #444;
}
:deep(.pos-history-modal .ant-modal-title) {
  color: #e0e0e0;
}
:deep(.pos-history-modal .ant-modal-content) {
  background: #1a1a1a;
}
:deep(.pos-history-modal .ant-modal-close) {
  color: #888;
}
.pos-history-list {
  max-height: 400px;
  overflow-y: auto;
}
.pos-history-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #2d2d2d;
  border-bottom: 1px solid #444;
  font-size: 12px;
  color: #888;
  font-weight: 600;
  position: sticky;
  top: 0;
  z-index: 1;
}
.pos-history-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid #3a3a3a;
  font-size: 13px;
  color: #ccc;
  cursor: pointer;
  transition: background 0.15s;
}
.pos-history-row:hover {
  background: #333;
}
.pos-history-h-col,
.pos-history-col {
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
