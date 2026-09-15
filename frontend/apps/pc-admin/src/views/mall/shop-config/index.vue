<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
    >
      <!--
        页面定位：商城 → 商城设置 → 店铺设置（对标 ql361「商城 → 商城设置 → 店铺设置」）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/店铺设置开发文档.md
        结构说明：左侧 3 子项导航（店铺参数 / 注册设置 / 支付设置）
                 + 右侧配置表单（店铺参数 4 分组卡片；支付设置为「支付方式×场景」矩阵表）
                 + 底部统一「保存」（按子项分别提交）
        金标准：ErrorBoundary > PageContainer(full-height) > 左子项导航 + 右分组表单
        API：shopConfigApi（GET/PUT /erp/mall/admin/config，单行 ShopConfig 实体）
        保存口径：单行实体三页共用，保存时「完整配置 + 当前子项字段」合并提交
        存储口径：布尔开关库内 INTEGER 0/1；支付场景矩阵落 TEXT 列 payment_scenes（JSON），
                 在线支付渠道明细仍由 payment_methods 承载
        对标重做：注册设置子项已按 2026-09-14 ql361 实测的 5 项重做（Flyway V11.365.0）
        后端：本页列由 Flyway V11.361.7__Add_Tenant_Shop_Config_GoldStandard_Fields.sql 补齐，
              注册设置子项列由 V11.365.0__Rebuild_Mall_Shop_Register_Settings.sql 补齐
      -->
      <div class="config-layout">
        <!-- ═══ 左侧：3 子项导航 ═══ -->
        <div class="config-layout__side">
          <a-menu
            :selected-keys="[currentMenuKey]"
            mode="inline"
            class="config-menu"
            @click="onMenuClick"
          >
            <a-menu-item
              v-for="m in MENU_ITEMS"
              :key="m.key"
            >
              <span class="config-menu__text">{{ m.label }}</span>
            </a-menu-item>
          </a-menu>
        </div>

        <!-- ═══ 右侧：当前子项配置表单 ═══ -->
        <div
          class="config-layout__main"
          data-testid="shop-config-form"
        >
          <a-spin :spinning="loading">
            <a-form
              :label-col="{ span: 7 }"
              :wrapper-col="{ span: 15 }"
              size="small"
            >
              <!-- ══════ 子项一：店铺参数（4 分组） ══════ -->
              <template v-if="currentMenuKey === 'params'">
                <a-card
                  title="基础设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（shop_enabled）；注意与既有 status 语义并存，未混用 -->
                  <a-form-item label="商城开关">
                    <a-switch
                      v-model:checked="params.shopEnabled"
                      checked-children="开"
                      un-checked-children="关"
                    />
                  </a-form-item>
                  <!-- 已落库（open_time / close_time） -->
                  <a-form-item label="营业时间">
                    <a-space>
                      <a-time-picker
                        v-model:value="params.openTime"
                        format="HH:mm"
                        value-format="HH:mm"
                        placeholder="起"
                        style="width: 120px"
                      />
                      <span>至</span>
                      <a-time-picker
                        v-model:value="params.closeTime"
                        format="HH:mm"
                        value-format="HH:mm"
                        placeholder="止"
                        style="width: 120px"
                      />
                    </a-space>
                  </a-form-item>
                  <!-- 已落库（sms_signature） -->
                  <a-form-item
                    label="短信签名"
                    required
                  >
                    <a-input
                      v-model:value="params.smsSignature"
                      :maxlength="50"
                      placeholder="请输入公司名称/商城名称"
                    />
                  </a-form-item>
                  <!-- 已落库（buyer_hide_level） -->
                  <a-form-item label="启用买家不显示客户级别">
                    <a-checkbox v-model:checked="params.buyerHideLevel" />
                  </a-form-item>
                  <!-- 已落库（allow_guest / guest_show_price） -->
                  <a-form-item label="是否允许游客访问">
                    <a-select
                      v-model:value="params.allowGuest"
                      style="width: 200px"
                      :options="GUEST_ACCESS_OPTIONS"
                    />
                  </a-form-item>
                  <a-form-item label="游客显示价格">
                    <a-select
                      v-model:value="params.guestShowPrice"
                      style="width: 200px"
                      :options="GUEST_PRICE_OPTIONS"
                    />
                  </a-form-item>
                  <!-- 已落库（quantity_scale） -->
                  <a-form-item label="商城数量小数位数">
                    <a-select
                      v-model:value="params.quantityScale"
                      style="width: 200px"
                      :options="QUANTITY_SCALE_OPTIONS"
                    />
                  </a-form-item>
                  <!-- 已落库（wechat_only_login） -->
                  <a-form-item label="只允许微信登录">
                    <a-checkbox v-model:checked="params.wechatOnlyLogin" />
                  </a-form-item>
                </a-card>

                <a-card
                  title="价格设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（price_track / price_track_with_unit / enable_retail_price） -->
                  <a-form-item label="价格规则">
                    <a-space
                      direction="vertical"
                      size="small"
                    >
                      <a-checkbox v-model:checked="params.priceTrack">
                        价格跟踪
                      </a-checkbox>
                      <a-checkbox v-model:checked="params.priceTrackWithUnit">
                        跟踪价格随单位联动
                      </a-checkbox>
                      <a-checkbox v-model:checked="params.enableRetailPrice">
                        启用建议零售价
                      </a-checkbox>
                    </a-space>
                  </a-form-item>
                </a-card>

                <a-card
                  title="商品设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（product_auth_manage / enable_split_unit / show_sales /
                       stock_display / enable_mall_category / out_of_stock_display） -->
                  <a-form-item label="商品规则">
                    <a-space
                      direction="vertical"
                      size="small"
                    >
                      <a-checkbox v-model:checked="params.productAuthManage">
                        商品授权管理
                      </a-checkbox>
                      <a-checkbox v-model:checked="params.enableSplitUnit">
                        启用分单位显示
                      </a-checkbox>
                      <a-checkbox v-model:checked="params.showSales">
                        启用商城显示销量
                      </a-checkbox>
                      <a-checkbox v-model:checked="params.enableMallCategory">
                        启用商城分类
                      </a-checkbox>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="库存显示方式">
                    <a-space>
                      <a-select
                        v-model:value="params.stockDisplay"
                        style="width: 200px"
                        :options="STOCK_DISPLAY_OPTIONS"
                      />
                      <a-button
                        type="link"
                        size="small"
                        class="link-btn"
                        @click="handleStockDisplayHelp"
                      >
                        说明
                      </a-button>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="无货商品显示方式">
                    <a-select
                      v-model:value="params.outOfStockDisplay"
                      style="width: 200px"
                      :options="OUT_OF_STOCK_OPTIONS"
                    />
                  </a-form-item>
                </a-card>

                <a-card
                  title="订单设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（auto_receive_enabled / auto_receive_days） -->
                  <a-form-item label="天自动收货">
                    <a-space>
                      <a-checkbox v-model:checked="params.autoReceiveEnabled">
                        启用
                      </a-checkbox>
                      <a-input-number
                        v-model:value="params.autoReceiveDays"
                        :min="1"
                        :max="90"
                        :precision="0"
                        :disabled="!params.autoReceiveEnabled"
                        style="width: 100px"
                      />
                      <span>天自动收货</span>
                    </a-space>
                  </a-form-item>
                </a-card>
              </template>

              <!-- ══════ 子项二：注册设置（对标实测 5 项重做，Flyway V11.365.0） ══════ -->
              <template v-else-if="currentMenuKey === 'register'">
                <!--
                  对标实测（2026-09-14 ql361 活体抓取，权威）该子项**只有这 5 项，且没有「注册协议」**：
                    ① 允许注册账号（复选框，同行「设置注册信息」按钮）
                    ② 买家注册默认级别（必填 *，下拉，同行「客户级别设置」按钮）
                    ③ 买家注册默认分类（必填 *，搜索式选择器）
                    ④ 买家账号注册审核（必填 *，下拉 否(0)/是(1)）
                    ⑤ 新用户注册送优惠券（复选框，同行「设置优惠券」按钮）
                  说明见 docs/Yh-Spec/手动整理对标开发文档/交易模块/店铺设置开发文档.md「剩余缺口」#1。
                -->
                <a-card
                  title="注册设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- ① 允许注册账号（对标 name=enable_join_apply / EnableJoinApply） -->
                  <a-form-item label="允许注册账号">
                    <a-space>
                      <a-checkbox v-model:checked="register.enableJoinApply" />
                      <!-- 对标同行按钮「设置注册信息」：本系统无对应页面/抽屉/端点 → 置为禁用，不造假弹窗 -->
                      <a-button
                        type="link"
                        size="small"
                        class="link-btn"
                        disabled
                        title="本系统后端/页面未实现（对标 B2BRegisteredContacts 数据结构未接入）"
                      >
                        设置注册信息
                      </a-button>
                    </a-space>
                    <div class="form-tip">
                      对应列 <code>enable_join_apply</code>（对标 <code>EnableJoinApply</code>，实测默认已勾选）。
                      「设置注册信息」按钮置灰：<strong>本系统后端/页面未实现</strong>（对标该项数据结构为 <code>B2BRegisteredContacts</code>，未接入）。
                    </div>
                  </a-form-item>

                  <!-- ② 买家注册默认级别（必填 *，选项来自本系统客户级别字典） -->
                  <a-form-item
                    label="买家注册默认级别"
                    required
                  >
                    <a-space>
                      <a-select
                        v-model:value="register.regDefaultGradeId"
                        :options="gradeOptions"
                        :loading="gradeLoading"
                        :disabled="!gradeOptions.length"
                        allow-clear
                        show-search
                        option-filter-prop="label"
                        placeholder="请选择客户级别"
                        style="width: 200px"
                      />
                      <!-- 对标同行按钮「客户级别设置」：本系统无对应跳转页 → 置为禁用 -->
                      <a-button
                        type="link"
                        size="small"
                        class="link-btn"
                        disabled
                        title="本系统无「客户级别设置」跳转页"
                      >
                        客户级别设置
                      </a-button>
                    </a-space>
                    <div class="form-tip">
                      对应列 <code>reg_default_grade_id</code>（对标 <code>Default2bCustomerDealerTypeId</code>，实测样本「A餐饮客户」= 464415）。
                      选项取自本系统<strong>客户级别字典</strong> <code>GET /erp/partner/grades?gradeType=CUSTOMER</code>（<code>partnerGradeApi.list('CUSTOMER')</code>，
                      与「买家账号」「客户资料」等页同源）；{{ gradeOptions.length ? `当前字典 ${gradeOptions.length} 项` : '⚠️ 字典为空或接口不可用，暂时无法选择' }}。
                      「客户级别设置」按钮置灰：<strong>本系统无该跳转页</strong>。
                    </div>
                  </a-form-item>

                  <!-- ③ 买家注册默认分类（必填 *；⚠️ 对标选项字典未实测 → 文本输入承载原值，不造字典） -->
                  <a-form-item
                    label="买家注册默认分类"
                    required
                  >
                    <a-input
                      v-model:value="register.regDefaultCategory"
                      :maxlength="100"
                      placeholder="在线注册客户"
                      style="width: 240px"
                    />
                    <div class="form-tip">
                      对应列 <code>reg_default_category</code>（VARCHAR，存原值字符串）。
                      ⚠️ <strong>对标「买家注册默认分类」的选项来源未实测</strong>（实测只拿到取值「在线注册客户」，未拿到其选项字典/接口），
                      故本系统<strong>不造字典</strong>：以文本输入承载原值，不做假下拉。若后续实测到字典来源（对标为带放大镜的搜索式选择器），再改为选择器。
                    </div>
                  </a-form-item>

                  <!-- ④ 买家账号注册审核（必填 *，下拉 否(0)/是(1)） -->
                  <a-form-item
                    label="买家账号注册审核"
                    required
                  >
                    <a-select
                      v-model:value="register.regAuditRequired"
                      :options="REG_AUDIT_OPTIONS"
                      style="width: 200px"
                    />
                    <div class="form-tip">
                      对应列 <code>reg_audit_required</code>；对标实测取值 <code>是(1)</code>，选项为 <code>否(0)/是(1)</code>。
                    </div>
                  </a-form-item>

                  <!-- ⑤ 新用户注册送优惠券（复选框 + 同行「设置优惠券」按钮） -->
                  <a-form-item label="新用户注册送优惠券">
                    <a-space>
                      <a-checkbox v-model:checked="register.regGiveCoupon" />
                      <!-- 对标同行按钮「设置优惠券」：本系统无对应页面/抽屉/端点 → 置为禁用 -->
                      <a-button
                        type="link"
                        size="small"
                        class="link-btn"
                        disabled
                        title="本系统后端/页面未实现（优惠券选择器无端点）"
                      >
                        设置优惠券
                      </a-button>
                    </a-space>
                    <div class="form-tip">
                      对应列 <code>reg_give_coupon</code> + <code>reg_give_coupons</code>
                      （对标 <code>MallRegGiveCoupons</code>，实测值 <code>&#123;"opengive":false,"couponslist":[]&#125;</code>）。
                      本页保存时写入 <code>&#123;opengive: 勾选态, couponslist: []&#125;</code>；
                      「设置优惠券」按钮置灰：<strong>本系统无优惠券选择页/端点，couponslist 暂保持空数组</strong>（元素结构亦未实测）。
                    </div>
                  </a-form-item>

                  <!-- 历史自造项：已从「注册设置」子项移除，仅在此声明，不再读写库列 -->
                  <div class="form-tip">
                    「注册协议」（列 <code>register_agreement</code>）为本系统历史自造项，对标该子项<strong>无此项</strong>，
                    自 Flyway <code>V11.365.0</code> 起已从本子项移除、前端不再读写（列与既有数据仅为兼容旧前端/已发布 JAR 而保留）。
                  </div>
                </a-card>
              </template>

              <!-- ══════ 子项三：支付设置（支付方式×场景矩阵） ══════ -->
              <template v-else-if="currentMenuKey === 'payment'">
                <a-card
                  title="支付设置"
                  :bordered="false"
                  class="group-card"
                >
                  <a-table
                    :columns="paymentColumns"
                    :data-source="paymentRows"
                    :pagination="false"
                    size="small"
                    row-key="code"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'sort'">
                        <a-space :size="4">
                          <a-button
                            type="link"
                            size="small"
                            class="order-btn"
                            :disabled="record.index === 0"
                            @click="movePaymentRow(record.index, -1)"
                          >
                            ↑
                          </a-button>
                          <a-button
                            type="link"
                            size="small"
                            class="order-btn"
                            :disabled="record.index === paymentRows.length - 1"
                            @click="movePaymentRow(record.index, 1)"
                          >
                            ↓
                          </a-button>
                          <span class="sort-index">{{ record.index + 1 }}</span>
                        </a-space>
                      </template>
                      <template v-else-if="column.key === 'enabled'">
                        <a-checkbox v-model:checked="payment[record.code].enabled" />
                      </template>
                      <template v-else-if="column.key === 'rule'">
                        <div class="rule-cell">
                          <a-checkbox v-model:checked="payment[record.code].ruleEnabled">
                            {{ record.ruleLabel }}
                          </a-checkbox>
                          <template v-if="record.code === 'ONLINE'">
                            <a-input-number
                              v-model:value="payment[record.code].ruleValue"
                              :min="1"
                              :max="720"
                              :precision="0"
                              :disabled="!payment[record.code].ruleEnabled"
                              size="small"
                              style="width: 80px"
                            />
                            <span class="rule-unit">小时</span>
                          </template>
                        </div>
                      </template>
                      <template v-else-if="column.key === 'remark'">
                        <div class="remark-cell">
                          {{ record.remark }}
                        </div>
                      </template>
                    </template>
                  </a-table>
                  <div class="form-tip" style="margin-top: 12px">
                    在线支付还包含微信、支付宝、聚合支付、农业银行、建设银行、京东支付，这六种方式请直接至「支付配置」启用；
                    本系统已在库的支付方式：{{ enabledMethodText }}。
                    本页矩阵（含启停 / 显示顺序 / 统一规则值）落库列 <code>payment_scenes</code>（JSON，本实现存储口径）；
                    在线支付的渠道启用明细仍由既有列 <code>payment_methods</code> 承载。
                  </div>
                </a-card>
              </template>

              <!-- ═══ 底部：按子项保存 ═══ -->
              <div class="config-footer">
                <a-space>
                  <a-button
                    type="primary"
                    :loading="saving"
                    @click="handleSave"
                  >
                    保存
                  </a-button>
                  <a-button
                    :disabled="loading"
                    @click="loadConfig"
                  >
                    重置
                  </a-button>
                </a-space>
              </div>
            </a-form>
          </a-spin>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 店铺设置（商城 → 商城设置 → 店铺设置）
 * 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/店铺设置开发文档.md
 * 结构：左侧 3 子项导航（店铺参数/注册设置/支付设置）+ 右侧分组表单 + 支付方式矩阵表
 *
 * 存储口径（Flyway V11.361.7__Add_Tenant_Shop_Config_GoldStandard_Fields.sql 已补齐本页全部列）：
 *   · 布尔开关：库内 INTEGER 0/1，经 boolToBit / bitToBool 显式转换（不依赖隐式转换）
 *   · 字符串枚举：allow_guest / guest_show_price / stock_display / out_of_stock_display
 *   · 支付场景矩阵：JSON 数组落 TEXT 列 payment_scenes
 *       [{code:'ONLINE'|'CREDIT'|'COD', enabled, ruleEnabled, ruleValue, sort}]
 *       —— 这是**本实现的存储口径**（含场景启停 / 显示顺序 / 统一规则值）；
 *          在线支付的**渠道启用明细**仍由既有列 payment_methods（逗号分隔）承载，二者并存不冲突
 *   · 注册设置（对标实测 5 项，Flyway V11.365.0 重做）：
 *       ① 允许注册账号 → enable_join_apply（复选框；同行「设置注册信息」按钮本系统未实现 → 禁用）
 *       ② 买家注册默认级别 → reg_default_grade_id（**必填**，选项来自本系统客户级别字典
 *            partnerGradeApi.list('CUSTOMER')；同行「客户级别设置」按钮本系统无跳转页 → 禁用）
 *       ③ 买家注册默认分类 → reg_default_category（**必填**；⚠️ 对标选项字典**未实测** → 文本输入承载原值，不造字典）
 *       ④ 买家账号注册审核 → reg_audit_required（**必填**，否(0)/是(1)）
 *       ⑤ 新用户注册送优惠券 → reg_give_coupon + reg_give_coupons
 *            （复选框 + JSON {opengive,couponslist}；「设置优惠券」按钮本系统未实现 → 禁用）
 *       ⚠️ 对标「注册设置」**没有「注册协议」**：register_agreement 为我方历史自造，
 *          本子项自 V11.365.0 起已移除并不再读写（列仅为兼容保留）。
 *       ⚠️ 对标营业执照门控（EnableJoinApplyLicense / JoinApplyLicensePrompt）未抓实，不臆造实现。
 *   · 已落库复用列：shop_name / enable_register / enable_auto_audit / payment_methods
 *     （后三者仅由旧版本代码维护；本页「注册设置」子项不再读写）
 *
 * 保存安全机制：三页共用单行实体 + 同一对端点；后端 updateConfig 走 updateById，
 *   MyBatis-Plus 默认忽略 null 字段 → 本页只提交「店铺参数」字段时不会覆盖另两页字段。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  shopConfigApi,
  boolToBit,
  bitToBool,
  parseJsonField,
  stringifyJsonField,
  type ShopConfig,
} from '@/api/erp/mall'
import { partnerGradeApi } from '@/api/erp/partner'

defineOptions({ name: 'MallShopConfig' })

// ═══ 左侧 3 子项导航（对标实测） ═══
const MENU_ITEMS = [
  { key: 'params', label: '店铺参数' },
  { key: 'register', label: '注册设置' },
  { key: 'payment', label: '支付设置' },
] as const
const currentMenuKey = ref<string>('params')
const currentMenuLabel = computed(() => MENU_ITEMS.find(m => m.key === currentMenuKey.value)?.label || '')

function onMenuClick({ key }: { key: string | number }) {
  currentMenuKey.value = String(key)
}

// ═══ 子项一 · 店铺参数 ═══
const GUEST_ACCESS_OPTIONS = [
  { label: '不允许', value: 'NOT_ALLOW' },
  { label: '允许', value: 'ALLOW' },
]
const GUEST_PRICE_OPTIONS = [
  { label: '不显示价格', value: 'HIDE' },
  { label: '显示价格', value: 'SHOW' },
]
const QUANTITY_SCALE_OPTIONS = [
  { label: '0位小数', value: 0 },
  { label: '1位小数', value: 1 },
  { label: '2位小数', value: 2 },
  { label: '3位小数', value: 3 },
]
const STOCK_DISPLAY_OPTIONS = [
  { label: '显示可用库存', value: 'AVAILABLE' },
  { label: '显示库存数量', value: 'QUANTITY' },
  { label: '不显示库存', value: 'HIDE' },
]
const OUT_OF_STOCK_OPTIONS = [
  { label: '显示补货中', value: 'RESTOCKING' },
  { label: '不显示商品', value: 'HIDE' },
]

const params = reactive({
  // 基础设置
  shopEnabled: false,
  openTime: undefined as string | undefined,
  closeTime: undefined as string | undefined,
  smsSignature: '',
  buyerHideLevel: false,
  allowGuest: 'NOT_ALLOW',
  guestShowPrice: 'HIDE',
  quantityScale: 1,
  wechatOnlyLogin: false,
  // 价格设置
  priceTrack: false,
  priceTrackWithUnit: false,
  enableRetailPrice: false,
  // 商品设置
  productAuthManage: false,
  enableSplitUnit: false,
  showSales: false,
  stockDisplay: 'AVAILABLE',
  enableMallCategory: false,
  outOfStockDisplay: 'RESTOCKING',
  // 订单设置
  autoReceiveEnabled: false,
  autoReceiveDays: 7,
})

function handleStockDisplayHelp() {
  message.info('显示可用库存：可用库存 = 实际库存 - 占用库存；显示库存数量：展示实际库存；不显示库存：买家端不展示库存')
}

// ═══ 子项二 · 注册设置（对标实测 5 项，Flyway V11.365.0 重做） ═══
/**
 * 对标实测（2026-09-14 ql361 活体抓取）该子项**只有 5 项且无「注册协议」**：
 *   ① 允许注册账号      → enable_join_apply（复选框）
 *   ② 买家注册默认级别  → reg_default_grade_id（必填下拉，选项=本系统客户级别字典）
 *   ③ 买家注册默认分类  → reg_default_category（必填；⚠️ 对标选项字典未实测 → 文本输入承载原值，不造字典）
 *   ④ 买家账号注册审核  → reg_audit_required（必填下拉，否(0)/是(1)）
 *   ⑤ 新用户注册送优惠券 → reg_give_coupon + reg_give_coupons（复选框 + 优惠券设置 JSON）
 *
 * ⚠️ 营业执照门控（对标 EnableJoinApplyLicense=false / JoinApplyLicensePrompt="上传营业执照"）**未抓实，不臆造实现**。
 * ⚠️ 历史自造字段 registerAgreement（注册协议）已从本子项移除，前端**不再读写**（见 mall.ts 的 @deprecated 说明）。
 */
const register = reactive({
  enableJoinApply: false,
  regDefaultGradeId: undefined as number | undefined,
  regDefaultCategory: '',
  /** 否(0)/是(1)，对标实测默认「是(1)」 */
  regAuditRequired: 1,
  regGiveCoupon: false,
})
/** ④ 买家账号注册审核选项（对标取值 否(0)/是(1)） */
const REG_AUDIT_OPTIONS = [
  { label: '否', value: 0 },
  { label: '是', value: 1 },
]
/** ② 买家注册默认级别选项：来自本系统**客户级别字典**（与「买家账号」「客户资料」同源） */
const gradeOptions = ref<Array<{ label: string; value: number }>>([])
const gradeLoading = ref(false)

async function loadGradeOptions() {
  gradeLoading.value = true
  try {
    const list: any = await partnerGradeApi.list('CUSTOMER')
    const rows: any[] = Array.isArray(list) ? list : (list?.data ?? [])
    gradeOptions.value = (Array.isArray(rows) ? rows : [])
      .filter(g => g && g.id !== undefined && g.id !== null)
      .map(g => ({ label: String(g.gradeName ?? g.gradeCode ?? g.id), value: Number(g.id) }))
  } catch (e) {
    // 字典不可用时不阻断页面：置空 + 页面提示，保存时按必填校验拦截
    console.warn('[店铺设置] 客户级别字典获取失败（/erp/partner/grades?gradeType=CUSTOMER）', e)
    gradeOptions.value = []
  } finally {
    gradeLoading.value = false
  }
}

// ═══ 子项三 · 支付设置矩阵（对标 3 场景） ═══
interface PaymentRow {
  code: string
  name: string
  /** 已启用渠道（用于本系统 payment_methods 映射说明） */
  methodText: string
  ruleLabel: string
  remark: string
}
const PAYMENT_ROWS = reactive<PaymentRow[]>([
  {
    code: 'ONLINE',
    name: '在线支付',
    methodText: '微信 / 支付宝 / 银联 / 银行转账',
    ruleLabel: '未付款自动取消订单',
    remark: '在线支付还包含微信、支付宝、聚合支付、农业银行、建设银行、京东支付，这六种方式请直接至「支付配置」启用；同时启用允许未付款发货和多少小时未付款自动取消订单，买家提交订单始终不会被取消；启用订单审核时会被取消。',
  },
  {
    code: 'CREDIT',
    name: '欠款',
    methodText: '—',
    ruleLabel: '允许欠款下单',
    remark: '买家在商城提交订单采用欠款支付方式则提交订单可不支付，后台能操作发货，买家收货后可直接在商城支付。',
  },
  {
    code: 'COD',
    name: '货到付款',
    methodText: '—',
    ruleLabel: '',
    remark: '买家在商城提交订单采用货到付款方式则提交订单可不支付，后台能操作发货，买家收货后可直接在商城进行支付。',
  },
])
const payment = reactive<Record<string, { enabled: boolean; ruleEnabled: boolean; ruleValue: number }>>({
  ONLINE: { enabled: false, ruleEnabled: false, ruleValue: 24 },
  CREDIT: { enabled: false, ruleEnabled: false, ruleValue: 0 },
  COD: { enabled: false, ruleEnabled: false, ruleValue: 0 },
})

const paymentColumns = [
  { title: '支付方式', key: 'name', width: 110 },
  { title: '商城默认显示顺序', key: 'sort', width: 150 },
  { title: '支付方式启用', key: 'enabled', width: 120, align: 'center' as const },
  { title: '统一规则设置', key: 'rule', width: 240 },
  { title: '备注说明', key: 'remark' },
]

/** 矩阵数据源（带 index，供「默认显示顺序」上下调整使用） */
const paymentRows = computed(() => PAYMENT_ROWS.map((row, index) => ({ ...row, index })))

function movePaymentRow(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= PAYMENT_ROWS.length) return
  const [row] = PAYMENT_ROWS.splice(index, 1)
  PAYMENT_ROWS.splice(target, 0, row)
}

/** payment_methods → 支付方式映射（已落库渠道） */
const METHOD_MAP: Record<string, string> = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  UNIONPAY: '银联支付',
  BANK: '银行转账',
  CASH: '货到付款',
}
/** 已落库的支付方式渠道列表（payment_methods，逗号分隔） */
const paymentMethodList = ref<string[]>([])
const enabledMethodText = computed(() => {
  const keys = paymentMethodList.value
  return keys.length ? keys.map(k => METHOD_MAP[k] || k).join(' / ') : '无'
})

// ═══ 配置读写（单行 ShopConfig） ═══
const loading = ref(false)
const saving = ref(false)
const configId = ref<number | undefined>(undefined)
/** 完整配置缓存：保存时合并，避免覆盖其它设置页维护的字段 */
let fullConfig: ShopConfig | null = null

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    // ── 「注册设置」子项回填（V11.365.0 起，对标实测 5 项；**只读写对标列**，
    //    历史自造列 enable_register / enable_auto_audit / register_agreement 不再参与本子项）──
    // ① 允许注册账号（库内 0/1 → boolean；新列无值时默认不勾选）
    register.enableJoinApply = bitToBool(cfg?.enableJoinApply, false)
    // ② 买家注册默认级别（BIGINT → number；无值留空，由必填校验拦截）
    register.regDefaultGradeId = (cfg?.regDefaultGradeId === null || cfg?.regDefaultGradeId === undefined)
      ? undefined
      : Number(cfg.regDefaultGradeId)
    // ③ 买家注册默认分类（⚠️ 对标选项字典未实测，存原值字符串）
    register.regDefaultCategory = cfg?.regDefaultCategory || ''
    // ④ 买家账号注册审核（库内 0/1，对标实测默认 是(1)）
    register.regAuditRequired = Number(cfg?.regAuditRequired ?? 1)
    // ⑤ 新用户注册送优惠券（复选框）
    register.regGiveCoupon = bitToBool(cfg?.regGiveCoupon, false)
    paymentMethodList.value = cfg?.paymentMethods
      ? String(cfg.paymentMethods).split(',').filter(Boolean)
      : []
    // ── V11.361.7 起「店铺参数」子项全部字段真实回填 ──
    params.shopEnabled = bitToBool(cfg?.shopEnabled, false)
    params.openTime = cfg?.openTime || undefined
    params.closeTime = cfg?.closeTime || undefined
    params.smsSignature = cfg?.smsSignature || ''
    params.buyerHideLevel = bitToBool(cfg?.buyerHideLevel, false)
    params.allowGuest = cfg?.allowGuest || 'NOT_ALLOW'
    params.guestShowPrice = cfg?.guestShowPrice || 'HIDE'
    params.quantityScale = cfg?.quantityScale ?? 1
    params.wechatOnlyLogin = bitToBool(cfg?.wechatOnlyLogin, false)
    params.priceTrack = bitToBool(cfg?.priceTrack, false)
    params.priceTrackWithUnit = bitToBool(cfg?.priceTrackWithUnit, false)
    params.enableRetailPrice = bitToBool(cfg?.enableRetailPrice, false)
    params.productAuthManage = bitToBool(cfg?.productAuthManage, false)
    params.enableSplitUnit = bitToBool(cfg?.enableSplitUnit, false)
    params.showSales = bitToBool(cfg?.showSales, false)
    params.stockDisplay = cfg?.stockDisplay || 'AVAILABLE'
    params.enableMallCategory = bitToBool(cfg?.enableMallCategory, false)
    params.outOfStockDisplay = cfg?.outOfStockDisplay || 'RESTOCKING'
    params.autoReceiveEnabled = bitToBool(cfg?.autoReceiveEnabled, false)
    params.autoReceiveDays = Number(cfg?.autoReceiveDays ?? 7)
    // 先按已落库渠道回填「在线支付」启用态（兼容 payment_scenes 为空的历史行），
    // 再由 payment_scenes 覆盖（若该行已保存过场景矩阵）
    syncPaymentFromMethods()
    restorePaymentScenes(cfg?.paymentScenes)
  } catch (e) {
    console.warn('[店铺设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

/** 在线支付场景对应的已落库渠道（渠道明细在「支付配置」维护） */
const ONLINE_METHODS = ['ALIPAY', 'WECHAT', 'UNIONPAY', 'BANK']
/** 记住最近一次已落库的渠道串，避免「停用→再启用」丢失渠道 */
let lastMethodStr = ''

/** 已有 payment_methods → 在线支付场景启用状态 */
function syncPaymentFromMethods() {
  const keys = paymentMethodList.value.filter(k => ONLINE_METHODS.includes(k))
  payment.ONLINE.enabled = keys.length > 0
  if (keys.length > 0) lastMethodStr = keys.join(',')
}

/** 支付场景矩阵默认值（对标 3 场景）——表单初值即默认值（ONLINE 规则默认 24 小时） */


/**
 * 回填支付场景矩阵（TEXT 列 payment_scenes，JSON 数组）。
 * 兼容空值 / 脏数据：解析失败或数组为空时**保留调用方已回填的值**（即 payment_methods 推导结果），
 * 不清零；同时按 sort 恢复行的显示顺序。
 */
function restorePaymentScenes(raw: unknown) {
  const list = parseJsonField<Array<Record<string, any>>>(raw, [])
  if (!Array.isArray(list) || !list.length) return
  const byCode = new Map<string, Record<string, any>>()
  list.forEach((s) => { if (s && typeof s === 'object' && s.code) byCode.set(String(s.code), s) })
  PAYMENT_ROWS.forEach((r) => {
    const s = byCode.get(r.code)
    if (!s) return
    payment[r.code].enabled = s.enabled === true
    payment[r.code].ruleEnabled = s.ruleEnabled === true
    payment[r.code].ruleValue = Number(s.ruleValue) || 0
  })
  // 按 sort 恢复显示顺序（缺 sort 的行保持原位置）
  const ordered = [...PAYMENT_ROWS].sort((a, b) => {
    const sa = Number(byCode.get(a.code)?.sort)
    const sb = Number(byCode.get(b.code)?.sort)
    const va = Number.isFinite(sa) ? sa : Number.MAX_SAFE_INTEGER
    const vb = Number.isFinite(sb) ? sb : Number.MAX_SAFE_INTEGER
    return va - vb
  })
  PAYMENT_ROWS.splice(0, PAYMENT_ROWS.length, ...ordered)
}

/** 支付场景矩阵 → JSON 数组（含 sort=当前显示顺序） */
function buildPaymentScenesJson(): string | undefined {
  return stringifyJsonField(PAYMENT_ROWS.map((r, index) => ({
    code: r.code,
    enabled: payment[r.code].enabled === true,
    ruleEnabled: payment[r.code].ruleEnabled === true,
    ruleValue: payment[r.code].ruleValue ?? 0,
    sort: index,
  })))
}

function handleError(err: any) {
  console.warn('[店铺设置] ErrorBoundary:', err)
}

/** 按子项校验 */
function validateCurrentItem(): boolean {
  if (currentMenuKey.value === 'params') {
    if (!params.smsSignature.trim()) {
      message.warning('请输入短信签名')
      return false
    }
  }
  if (currentMenuKey.value === 'register') {
    // 对标该子项 3 个必填项（②③④）—— 与实测 `*` 标记一致
    if (!register.regDefaultGradeId) {
      message.warning('请选择买家注册默认级别')
      return false
    }
    if (!register.regDefaultCategory.trim()) {
      message.warning('请填写买家注册默认分类')
      return false
    }
    if (register.regAuditRequired !== 0 && register.regAuditRequired !== 1) {
      message.warning('请选择买家账号注册审核')
      return false
    }
  }
  if (currentMenuKey.value === 'payment') {
    if (!PAYMENT_ROWS.some(r => payment[r.code].enabled)) {
      message.warning('请至少启用一种支付方式')
      return false
    }
  }
  return true
}

/** 按子项分别保存：仅提交当前子项维护的字段 + 完整配置合并
 *  （后端 updateById 忽略 null 字段，故未提交字段不会被清空；三页共用一行因此安全） */
async function handleSave() {
  if (!validateCurrentItem()) return
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
    }
    if (currentMenuKey.value === 'register') {
      // ① 允许注册账号
      payload.enableJoinApply = boolToBit(register.enableJoinApply)
      // ② 买家注册默认级别（必填）
      payload.regDefaultGradeId = register.regDefaultGradeId
      // ③ 买家注册默认分类（必填；⚠️ 对标选项字典未实测，存原值字符串）
      payload.regDefaultCategory = register.regDefaultCategory.trim()
      // ④ 买家账号注册审核（必填，0/1）
      payload.regAuditRequired = Number(register.regAuditRequired) === 1 ? 1 : 0
      // ⑤ 新用户注册送优惠券（复选框 + 对标 MallRegGiveCoupons 的 JSON 形状）
      payload.regGiveCoupon = boolToBit(register.regGiveCoupon)
      payload.regGiveCoupons = stringifyJsonField({
        opengive: register.regGiveCoupon === true,
        // ⚠️ 优惠券选择页/端点本系统未实现，couponslist 暂保持空数组（元素结构亦未实测）
        couponslist: [] as Array<Record<string, unknown>>,
      })
      // ⚠️ 不再提交 registerAgreement / enableRegister / enableAutoAudit：
      //    该三项为历史自造口径（对标「注册设置」无此三项），本子项自 V11.365.0 起不再读写，
      //    不提交即由后端 updateById 的 null-忽略语义保持库内既有值不变。
    }
    if (currentMenuKey.value === 'params') {
      // 布尔 → 库内 0/1（显式转换）
      payload.shopEnabled = boolToBit(params.shopEnabled)
      payload.buyerHideLevel = boolToBit(params.buyerHideLevel)
      payload.wechatOnlyLogin = boolToBit(params.wechatOnlyLogin)
      payload.priceTrack = boolToBit(params.priceTrack)
      payload.priceTrackWithUnit = boolToBit(params.priceTrackWithUnit)
      payload.enableRetailPrice = boolToBit(params.enableRetailPrice)
      payload.productAuthManage = boolToBit(params.productAuthManage)
      payload.enableSplitUnit = boolToBit(params.enableSplitUnit)
      payload.showSales = boolToBit(params.showSales)
      payload.enableMallCategory = boolToBit(params.enableMallCategory)
      payload.autoReceiveEnabled = boolToBit(params.autoReceiveEnabled)
      // 文本 / 枚举
      payload.openTime = params.openTime
      payload.closeTime = params.closeTime
      payload.smsSignature = params.smsSignature
      payload.allowGuest = params.allowGuest
      payload.guestShowPrice = params.guestShowPrice
      payload.quantityScale = Number(params.quantityScale) || 0
      payload.stockDisplay = params.stockDisplay
      payload.outOfStockDisplay = params.outOfStockDisplay
      payload.autoReceiveDays = Number(params.autoReceiveDays) || 1
    }
    if (currentMenuKey.value === 'payment') {
      // 已落库口径：payment_methods 承载「在线支付」渠道启用（渠道明细在「支付配置」维护）
      payload.paymentMethods = payment.ONLINE.enabled
        ? (paymentMethodList.value.filter(k => ONLINE_METHODS.includes(k)).join(',') || lastMethodStr || 'ALIPAY,WECHAT')
        : ''
      // 支付场景矩阵（启停 / 显示顺序 / 统一规则值）落 payment_scenes JSON
      payload.paymentScenes = buildPaymentScenesJson()
    }
    await shopConfigApi.update(payload)
    message.success(`${currentMenuLabel.value}已保存`)
    await loadConfig()
  } catch (e) {
    console.warn('[店铺设置] 保存失败', e)
    message.error(`${currentMenuLabel.value}保存失败`)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
  // ② 买家注册默认级别的选项来自本系统客户级别字典（与买家账号/客户资料同源）
  loadGradeOptions()
})
</script>

<style scoped>
/* 左子项导航 + 右配置表单（全高布局，右区独立滚动） */
.config-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 16px;
  overflow: hidden;
  padding: 12px 16px;
  box-sizing: border-box;
}
.config-layout__side {
  width: 180px;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
  padding-right: 8px;
  overflow: auto;
}
.config-menu {
  border-inline-end: none !important;
}
.config-menu__text {
  font-size: 14px;
}
.config-layout__main {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding-right: 8px;
}
.group-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 16px;
}
.group-card :deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 16px;
  border-bottom: 1px solid #f5f5f5;
}
.group-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  padding: 10px 0;
}
.group-card :deep(.ant-card-body) {
  padding: 16px 16px 4px;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-top: 2px;
}
.link-btn {
  padding: 0;
}
.order-btn {
  padding: 0 2px;
  font-size: 13px;
}
.sort-index {
  color: #8c8c8c;
  font-size: 12px;
}
.rule-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.rule-unit {
  font-size: 12px;
  color: #8c8c8c;
}
.remark-cell {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  white-space: normal;
}
.config-footer {
  position: sticky;
  bottom: 0;
  padding: 12px 0 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
