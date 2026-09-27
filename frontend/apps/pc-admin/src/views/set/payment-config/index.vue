<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        支付配置（设置 → 系统配置 → 支付配置，菜单 80623 / set:payment-config，单入口）

        · 对标 ql361「设置 → 系统配置 → 支付配置」的 4 个配置 Tab（逐字）：
          微信公众号配置 / 支付方式 / 场景配置 / 在线退款
        · 骨架：CategoryListLayout（4 Tab）+ BillDetailTable（表头齿轮列配置，**逐 Tab 独立 storage-key**）
          + PageConfigPanel（查询条件 + 功能按钮，放在布局外面）+ StandardPagination(classic)
        · 数据：全部后端真实读写 sys_project_config（core-payment 的 /payment/config/* 端点）。
          原先「借 /api/config 写缓存不落库 → 保存后重新打开抽屉永远为空」的假保存（P0）已修掉。
        · 切 Tab 必须**换 storage-key 且重新加载数据**（原先两 Tab 共用一个 onMounted 拉取、切 Tab 不发请求）
        · 字段口径：《设置模块/支付配置开发文档.md》§2（ql361 实测清单），不发明字段
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：各 Tab 的「保存」 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <!-- Tab① 微信公众号配置：保存全部配置项 -->
            <a-button
              v-if="activeTab === 'wechat' && isButtonEnabled('save')"
              v-permission="'payment:config:update'"
              type="primary"
              size="small"
              :loading="saving"
              @click="saveWechatItems"
            >
              保存
            </a-button>
            <!-- Tab③ 场景配置：保存全部场景行 -->
            <a-button
              v-if="activeTab === 'scenes' && isButtonEnabled('save')"
              v-permission="'payment:config:update'"
              type="primary"
              size="small"
              :loading="saving"
              @click="saveSceneRows"
            >
              保存
            </a-button>
            <!-- Tab④ 在线退款：保存开关 -->
            <a-button
              v-if="activeTab === 'refund' && isButtonEnabled('save')"
              v-permission="'payment:config:update'"
              type="primary"
              size="small"
              :loading="saving"
              @click="saveRefundItems"
            >
              保存
            </a-button>
            <!-- Tab② 支付方式：渠道参数走行内「参数配置」抽屉，工具栏只放提示 -->
            <span
              v-if="activeTab === 'methods'"
              class="toolbar-tip"
            >
              点行内「参数配置」维护该渠道的商户号 / 密钥 / 通知地址
            </span>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：页面配置 / 刷新 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              v-if="isButtonEnabled('pageConfig')"
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
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="loadActiveTab"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（逐 Tab 独立条件；横向自适应网格，禁止纵向单列） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div
              ref="gridRef"
              class="search-grid"
            >
              <!-- Tab① 微信公众号配置 -->
              <template v-if="activeTab === 'wechat'">
                <div
                  v-if="isQueryVisible('wechat.keyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchForm.wechatKeyword"
                    placeholder="配置项名称/键"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>

              <!-- Tab② 支付方式 -->
              <template v-else-if="activeTab === 'methods'">
                <div
                  v-if="isQueryVisible('methods.keyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchForm.methodsKeyword"
                    placeholder="渠道编码/名称"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  v-if="isQueryVisible('methods.enabled')"
                  class="search-field-item"
                >
                  <a-select
                    v-model:value="searchForm.methodsEnabled"
                    placeholder="是否启用"
                    size="small"
                    allow-clear
                    :options="ENABLED_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
                <div
                  v-if="isQueryVisible('methods.amount')"
                  class="search-field-item"
                >
                  <a-input-number
                    v-model:value="searchForm.methodsAmount"
                    placeholder="单笔金额（元）"
                    size="small"
                    style="width: 100%"
                    :min="0.01"
                    :precision="2"
                    @press-enter="handleSearch"
                  />
                </div>
              </template>

              <!-- Tab③ 场景配置 -->
              <template v-else-if="activeTab === 'scenes'">
                <div
                  v-if="isQueryVisible('scenes.keyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchForm.scenesKeyword"
                    placeholder="场景编码/名称"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>

              <!-- Tab④ 在线退款 -->
              <template v-else>
                <div
                  v-if="isQueryVisible('refund.keyword')"
                  class="search-field-item"
                >
                  <a-input
                    v-model:value="searchForm.refundKeyword"
                    placeholder="配置项名称/键"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
              </template>

              <!-- 动作组：占位宽度按内容自适应（useAutoGridSpan） -->
              <div
                ref="actionRef"
                class="search-action-group"
                :style="{ gridColumn: 'span ' + actionSpan }"
              >
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

        <!-- ═══ 数据表（逐 Tab 一套列定义 + 独立 storage-key；切 Tab 重新挂载并重新加载） ═══ -->
        <template #table>
          <div class="table-area">
            <!--
              ⚠️ 4 张表都显式传了 :min-rows（默认 20）：本页每个 Tab 的行数是**白名单驱动的固定小集合**
                 （wechat 4 项 / 在线退款 1 项 / 场景 3 行 / 渠道 5 行），用默认 20 会出现大段空行；
                 统一压到 8（在线退款 1 行数据 → 4）既铺满可见区又不留大片空白。行数变化时需同步复核。
            -->
            <!-- Tab① 微信公众号配置 -->
            <BillDetailTable
              v-if="activeTab === 'wechat'"
              :columns="wechatColumns"
              :data-source="pagedWechatRows"
              :loading="loading"
              :view-mode="true"
              :min-rows="8"
              storage-key="set-payment-config-columns-wechat"
              global-config-key="set-payment-config-columns-wechat"
              row-key="itemKey"
            >
              <template #wechatValueCell="{ record }">
                <a-switch
                  v-if="record.valueType === 'boolean'"
                  :checked="wechatEdit[record.itemKey] === 'true'"
                  checked-children="开"
                  un-checked-children="关"
                  size="small"
                  @change="(checked: any) => setWechatEdit(record.itemKey, checked ? 'true' : 'false')"
                />
                <a-input-password
                  v-else-if="record.valueType === 'password'"
                  :value="wechatEdit[record.itemKey]"
                  size="small"
                  placeholder="未配置"
                  @update:value="(v: string) => setWechatEdit(record.itemKey, v)"
                />
                <a-input
                  v-else
                  :value="wechatEdit[record.itemKey]"
                  size="small"
                  placeholder="未配置"
                  allow-clear
                  @update:value="(v: string) => setWechatEdit(record.itemKey, v)"
                />
              </template>

              <template #updateTimeCell="{ record }">
                {{ record.updateTime ? formatDateTime(record.updateTime) : '-' }}
              </template>
            </BillDetailTable>

            <!-- Tab② 支付方式 -->
            <BillDetailTable
              v-else-if="activeTab === 'methods'"
              :columns="methodColumns"
              :data-source="pagedMethodRows"
              :loading="loading"
              :view-mode="true"
              :min-rows="8"
              storage-key="set-payment-config-columns-methods"
              global-config-key="set-payment-config-columns-methods"
              row-key="channelCode"
            >
              <template #methodChannelCell="{ record }">
                <a-tag :color="channelTag(record.channelCode).color">
                  {{ channelTag(record.channelCode).name }}
                </a-tag>
              </template>

              <template #methodLimitCell="{ record }">
                {{ formatMoney(record.minAmount) }} ~ {{ formatMoney(record.maxAmount) }}
              </template>

              <!-- 渠道状态：区分「渠道本身不支持」与「渠道支持但你没配凭据」——
                   后者是可修的，必须给出明确指引，否则管理员只会看到「不可用」不知道怎么办 -->
              <template #methodAvailableCell="{ record }">
                <a-tooltip v-if="!record.available && record.credentialReady === false"
                           title="该渠道需要回调验签凭据（公钥 / APIv3 密钥 / 平台证书表），未配置或配置无效时不生效">
                  <a-tag color="warning">未配凭据</a-tag>
                </a-tooltip>
                <a-tag v-else :color="record.available ? 'success' : 'default'">
                  {{ record.available ? '可用' : '不可用' }}
                </a-tag>
              </template>

              <template #methodEnabledCell="{ record }">
                <a-tag :color="record.enabled ? 'success' : 'default'">
                  {{ record.enabled ? '启用' : '停用' }}
                </a-tag>
              </template>

              <template #methodSecretCell="{ record }">
                <span v-if="record.secretConfigured">已配置</span>
                <span
                  v-else
                  class="cell-muted"
                >未配置</span>
              </template>

              <template #methodTextCell="{ record, column }">
                <span v-if="record[column.key]">{{ record[column.key] }}</span>
                <span
                  v-else
                  class="cell-muted"
                >未配置</span>
              </template>

              <template #updateTimeCell="{ record }">
                {{ record.updateTime ? formatDateTime(record.updateTime) : '-' }}
              </template>

              <template #methodActionCell="{ record }">
                <a-button
                  v-permission="'payment:config:list'"
                  type="link"
                  size="small"
                  @click="openParamDrawer(record)"
                >
                  参数配置
                </a-button>
              </template>
            </BillDetailTable>

            <!-- Tab③ 场景配置：支付场景 × 支付渠道矩阵 -->
            <BillDetailTable
              v-else-if="activeTab === 'scenes'"
              :columns="sceneColumns"
              :data-source="pagedSceneRows"
              :loading="loading"
              :view-mode="true"
              :min-rows="8"
              storage-key="set-payment-config-columns-scenes"
              global-config-key="set-payment-config-columns-scenes"
              row-key="sceneCode"
            >
              <!-- 5 个渠道列共用同一个插槽，靠 column.key 区分是哪个渠道 -->
              <template #sceneChannelCell="{ record, column }">
                <a-checkbox
                  :checked="isSceneChannelChecked(record.sceneCode, column.key)"
                  @change="(e: any) => setSceneChannel(record.sceneCode, column.key, e.target.checked)"
                />
              </template>

              <template #sceneChannelTextCell="{ record }">
                <span v-if="(sceneEdit[record.sceneCode] || []).length">
                  {{ sceneChannelNames(record.sceneCode) }}
                </span>
                <span
                  v-else
                  class="cell-muted"
                >未启用</span>
              </template>

              <template #updateTimeCell="{ record }">
                {{ record.updateTime ? formatDateTime(record.updateTime) : '-' }}
              </template>
            </BillDetailTable>

            <!-- Tab④ 在线退款 -->
            <template v-else>
              <BillDetailTable
                :columns="refundColumns"
                :data-source="pagedRefundRows"
                :loading="loading"
                :view-mode="true"
                :min-rows="4"
                storage-key="set-payment-config-columns-refund"
                global-config-key="set-payment-config-columns-refund"
                row-key="itemKey"
              >
                <template #refundValueCell="{ record }">
                  <a-switch
                    :checked="refundEdit[record.itemKey] === 'true'"
                    checked-children="开"
                    un-checked-children="关"
                    size="small"
                    @change="(checked: any) => setRefundEdit(record.itemKey, checked ? 'true' : 'false')"
                  />
                </template>

                <template #updateTimeCell="{ record }">
                  {{ record.updateTime ? formatDateTime(record.updateTime) : '-' }}
                </template>
              </BillDetailTable>

              <!-- 两段说明文案由后端下发（逐字对标 ql361，避免前端硬编码后漂移） -->
              <div class="refund-notes">
                <div
                  v-for="(note, i) in refundNotes"
                  :key="i"
                  class="refund-note"
                >
                  {{ note }}
                </div>
              </div>
            </template>
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
    </PageContainer>

    <!-- ═══ 页面配置（查询条件 + 功能按钮），按金标准放在布局外面 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :hide-print-config="true"
      storage-key="set-payment-config-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 渠道参数配置抽屉（Tab② 行内「参数配置」） ═══ -->
    <a-drawer
      v-model:open="paramVisible"
      :title="`渠道参数配置 - ${currentChannel?.channelName || currentChannel?.channelCode || ''}`"
      width="480px"
    >
      <a-spin :spinning="paramLoading">
        <a-form
          ref="paramFormRef"
          :model="paramForm"
          :rules="paramRules"
          layout="vertical"
        >
          <a-form-item label="APP ID">
            <a-input
              v-model:value="paramForm.appId"
              placeholder="渠道分配的 APP ID"
            />
          </a-form-item>
          <a-form-item
            label="商户号"
            name="merchantNo"
          >
            <a-input
              v-model:value="paramForm.merchantNo"
              placeholder="商户号 / MCH ID"
            />
          </a-form-item>
          <a-form-item
            label="API 密钥"
            name="appSecret"
          >
            <a-input-password
              v-model:value="paramForm.appSecret"
              placeholder="API 密钥 / API Secret"
            />
          </a-form-item>
          <a-form-item
            label="异步通知 URL"
            name="notifyUrl"
          >
            <a-input
              v-model:value="paramForm.notifyUrl"
              placeholder="https://..."
            />
          </a-form-item>
          <!-- ── 回调验签凭据（2026-09-21）──────────────────────────────────
               上面 5 项够「发起支付」，但不够**验签回调**。不配这些，
               回调端点会 fail-closed 拒绝（而不是「不验签当成功」）。
               按渠道条件展示：支付宝只要一个公钥；微信要 APIv3 密钥 + 平台证书表。 -->
          <a-form-item
            v-if="isAlipayChannel"
            label="应用私钥（下单/退款签名用）"
            help="PKCS8、Base64，可带 -----BEGIN PRIVATE KEY----- 头尾；没有它无法下单"
          >
            <a-textarea
              v-model:value="paramForm.alipayPrivateKey"
              :rows="5"
              placeholder="支付宝应用私钥（注意：不是支付宝公钥）"
            />
          </a-form-item>
          <a-form-item
            v-if="isAlipayChannel"
            label="支付宝公钥（回调验签用）"
          >
            <a-textarea
              v-model:value="paramForm.alipayPublicKey"
              :rows="5"
              placeholder="支付宝公钥 Base64，可带 -----BEGIN PUBLIC KEY----- 头尾"
            />
          </a-form-item>
          <a-form-item
            v-if="isWechatChannel"
            label="APIv3 密钥（解密回调 resource）"
          >
            <a-input-password
              v-model:value="paramForm.wechatApiV3Key"
              placeholder="32 位 APIv3 密钥，长度不对后端会拒绝"
            />
          </a-form-item>
          <a-form-item
            v-if="isWechatChannel"
            label="平台证书表（回调验签用）"
            :validate-status="certsError ? 'error' : undefined"
            :help="certsError || '键=证书序列号，值=PEM 公钥；轮换期新旧证书都放进来'"
          >
            <a-textarea
              v-model:value="paramForm.wechatPlatformCerts"
              :rows="6"
              :placeholder="CERTS_PLACEHOLDER"
            />
          </a-form-item>
          <a-form-item
            v-if="isUnionPayChannel"
            label="商户私钥（下单/退款签名用）"
            help="PKCS8、Base64，可带 -----BEGIN PRIVATE KEY----- 头尾；没有它无法下单"
          >
            <a-textarea
              v-model:value="paramForm.unionPayMerchantPrivateKey"
              :rows="5"
              placeholder="银联商户私钥"
            />
          </a-form-item>
          <a-form-item
            v-if="isUnionPayChannel"
            label="证书 ID（certId）"
            help="银联签名报文中随 signature 一起上送"
          >
            <a-input
              v-model:value="paramForm.unionPayCertId"
              placeholder="如 1234ABCD5678"
            />
          </a-form-item>
          <a-form-item
            v-if="isUnionPayChannel"
            label="平台证书表（回调验签用）"
            :validate-status="certsError ? 'error' : undefined"
            :help="certsError || '键=证书 ID（certId），值=PEM 公钥'"
          >
            <a-textarea
              v-model:value="paramForm.unionPayCerts"
              :rows="6"
              :placeholder="CERTS_PLACEHOLDER"
            />
          </a-form-item>
          <a-form-item label="是否启用">
            <a-switch v-model:checked="paramForm.enabled" />
          </a-form-item>
          <div class="drawer-tip">
            在线渠道（支付宝 / 微信 / 银联）必须填商户号与 API 密钥；线下渠道（银行转账 / 现金）无需填写。
            <br />
            若要接收**支付结果回调**，还需按渠道补上「回调验签凭据」—— 支付宝要公钥，
            微信要 APIv3 密钥与平台证书表。缺这些时回调会被安全拒绝（不会不验签直接当成功）。
          </div>
          <a-button
            v-permission="'payment:config:update'"
            type="primary"
            block
            :loading="paramSaving"
            @click="saveParam"
          >
            保存参数
          </a-button>
        </a-form>
      </a-spin>
    </a-drawer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import {
  paymentConfigApi,
  PAYMENT_CHANNEL_MAP,
  type PaymentChannelConfigVO,
  type PaymentConfigItemVO,
  type PaymentChannelParam,
  type PaymentSceneVO,
} from '@/api/payment'

defineOptions({ name: 'SetPaymentConfig' })

// ═══ 4 个 Tab（文案逐字取自《支付配置开发文档》§8.1-1） ═══
const TABS = [
  { key: 'wechat', label: '微信公众号配置' },
  { key: 'methods', label: '支付方式' },
  { key: 'scenes', label: '场景配置' },
  { key: 'refund', label: '在线退款' },
]
const activeTab = ref('wechat')

/** 矩阵列 = 后端渠道 Bean 的 5 个渠道码（非 ql361 的 8 个渠道，§8.3 明确不借鉴其渠道清单） */
const CHANNEL_CODES = ['ALIPAY', 'WECHAT', 'UNIONPAY', 'BANK', 'CASH'] as const

const ENABLED_OPTIONS = [
  { label: '启用', value: true },
  { label: '停用', value: false },
]

// ═══ 格式化 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || Number.isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatDateTime(val: string | null | undefined): string {
  if (!val) return '-'
  return String(val).replace('T', ' ').slice(0, 19)
}

function channelTag(code: string | null | undefined): { name: string; color: string } {
  return PAYMENT_CHANNEL_MAP[String(code ?? '').toUpperCase()] || { name: code || '-', color: 'default' }
}

// ═══ 横向自适应查询网格 ═══
const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 页面配置（查询条件 + 功能按钮；字段键按 Tab 命名空间，单页面配置键） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const PAGE_CONFIG_KEY = 'set-payment-config-page-config'

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'wechat.keyword', label: '配置项名称/键', visible: true },
  { key: 'methods.keyword', label: '渠道编码/名称', visible: true },
  { key: 'methods.enabled', label: '是否启用', visible: true },
  { key: 'methods.amount', label: '单笔金额（元）', visible: true },
  { key: 'scenes.keyword', label: '场景编码/名称', visible: true },
  { key: 'refund.keyword', label: '配置项名称/键', visible: true },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'save', label: '保存', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]
const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))
/** 「恢复默认」的出厂基准（页面把「当前配置」传给 queryFieldsConfig，必须另给出厂值否则恢复默认无效） */
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = QUERY_FIELDS.map(f => ({ ...f }))
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = FUNCTION_BUTTONS.map(f => ({ ...f }))
const showPageConfig = ref(false)

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed.queryFields)) {
      queryFieldsConfig.value = QUERY_FIELDS.map(def => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === def.key)
        return saved ? { ...def, visible: saved.visible !== false } : { ...def }
      })
    }
    if (Array.isArray(parsed.functionButtons)) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(def => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === def.key)
        return saved ? { ...def, enabled: saved.enabled !== false } : { ...def }
      })
    }
  } catch { /* 配置损坏时回落到出厂值 */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  if (Array.isArray(config.queryFields)) queryFieldsConfig.value = config.queryFields
  if (Array.isArray(config.functionButtons)) functionButtonsConfig.value = config.functionButtons
}

const isQueryVisible = (key: string) => queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
const isButtonEnabled = (key: string) => functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false

// ═══ 查询条件（逐 Tab 独立） ═══
const searchForm = reactive({
  wechatKeyword: '' as string,
  methodsKeyword: '' as string,
  methodsEnabled: undefined as boolean | undefined,
  methodsAmount: undefined as number | undefined,
  scenesKeyword: '' as string,
  refundKeyword: '' as string,
})

// ═══ 数据状态 ═══
const loading = ref(false)
const saving = ref(false)
const wechatRows = ref<PaymentConfigItemVO[]>([])
const methodRows = ref<PaymentChannelConfigVO[]>([])
const sceneRows = ref<PaymentSceneVO[]>([])
const refundRows = ref<PaymentConfigItemVO[]>([])
const refundNotes = ref<string[]>([])

/** 行内编辑态：itemKey -> 当前输入值（不回写 dataSource，保存成功后靠重新 GET 回读覆盖） */
const wechatEdit = ref<Record<string, string>>({})
const refundEdit = ref<Record<string, string>>({})
/** 场景矩阵编辑态：sceneCode -> 已勾选渠道码 */
const sceneEdit = ref<Record<string, string[]>>({})

// ═══ 列定义（逐 Tab 一套；齿轮列配置走表头，storage-key 逐 Tab 独立） ═══

// Tab① 微信公众号配置（4 个配置项，逐字取自开发文档 §2「微信公众号配置」Tab）
const wechatColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'itemName', title: '配置项', type: 'input', width: 180 },
  { key: 'itemValue', title: '配置值', type: 'slot', slotName: 'wechatValueCell', width: 280 },
  { key: 'description', title: '说明', type: 'input', width: 400 },
  { key: 'updateTime', title: '配置时间', type: 'slot', slotName: 'updateTimeCell', width: 160, defaultHidden: true },
]

// Tab② 支付方式（5 个渠道 + 已落库参数；「支付商户号/支付商户密钥」标题逐字取自 ql361 实测）
const methodColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', width: 100, fixed: 'right', slotName: 'methodActionCell' },
  { key: 'channelCode', title: '渠道编码', type: 'input', width: 120 },
  { key: 'channelName', title: '支付方式', type: 'slot', slotName: 'methodChannelCell', width: 130 },
  { key: 'merchantNo', title: '支付商户号', type: 'slot', slotName: 'methodTextCell', width: 170 },
  { key: 'secretConfigured', title: '支付商户密钥', type: 'slot', slotName: 'methodSecretCell', width: 140 },
  { key: 'notifyUrl', title: '异步通知 URL', type: 'slot', slotName: 'methodTextCell', width: 260, className: 'cell-ellipsis' },
  { key: 'enabled', title: '是否启用', type: 'slot', slotName: 'methodEnabledCell', width: 100 },
  { key: 'limit', title: '单笔限额（元）', type: 'slot', slotName: 'methodLimitCell', width: 180 },
  { key: 'available', title: '渠道状态', type: 'slot', slotName: 'methodAvailableCell', width: 110 },
  { key: 'updateTime', title: '配置时间', type: 'slot', slotName: 'updateTimeCell', width: 160, defaultHidden: true },
]

// Tab③ 场景配置（矩阵：行 = 支付场景，列 = 5 个支付渠道 + 已启用汇总）
const sceneColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'sceneName', title: '支付场景', type: 'input', width: 140 },
  { key: 'sceneCode', title: '场景编码', type: 'input', width: 140 },
  ...CHANNEL_CODES.map(code => ({
    key: code,
    title: PAYMENT_CHANNEL_MAP[code]?.name || code,
    type: 'slot' as const,
    slotName: 'sceneChannelCell',
    width: 110,
    align: 'center' as const,
  })),
  { key: 'channelSummary', title: '已启用支付方式', type: 'slot', slotName: 'sceneChannelTextCell', width: 240 },
  { key: 'updateTime', title: '配置时间', type: 'slot', slotName: 'updateTimeCell', width: 160, defaultHidden: true },
]

// Tab④ 在线退款（1 个开关 + 说明）
const refundColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'itemName', title: '配置项', type: 'input', width: 200 },
  { key: 'itemValue', title: '配置值', type: 'slot', slotName: 'refundValueCell', width: 160 },
  { key: 'description', title: '说明', type: 'input', width: 400 },
  { key: 'updateTime', title: '配置时间', type: 'slot', slotName: 'updateTimeCell', width: 160, defaultHidden: true },
]

// ═══ 前端过滤 + 分页 ═══
// 说明：4 个 Tab 的数据源都是**后端白名单驱动的固定小集合**（wechat 4 项 / methods 5 渠道 /
// scenes 3 场景 / refund 1 项），后端一次性返回全量，分页与「单笔金额」筛选在页面本地完成。
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const filteredMethodRows = computed(() => {
  const amount = searchForm.methodsAmount
  if (amount === null || amount === undefined) return methodRows.value
  // 保留「单笔限额包含该金额」的渠道（与旧「查询可用渠道」同一口径：不满足的渠道会消失，不是置灰）
  return methodRows.value.filter(r => Number(r.minAmount) <= amount && amount <= Number(r.maxAmount))
})

function pageOf<T>(rows: T[]): T[] {
  const start = (pagination.current - 1) * pagination.pageSize
  return rows.slice(start, start + pagination.pageSize)
}

const pagedWechatRows = computed(() => pageOf(wechatRows.value))
const pagedMethodRows = computed(() => pageOf(filteredMethodRows.value))
const pagedSceneRows = computed(() => pageOf(sceneRows.value))
const pagedRefundRows = computed(() => pageOf(refundRows.value))

function activeRowCount(): number {
  if (activeTab.value === 'wechat') return wechatRows.value.length
  if (activeTab.value === 'methods') return filteredMethodRows.value.length
  if (activeTab.value === 'scenes') return sceneRows.value.length
  return refundRows.value.length
}

function syncTotal() {
  pagination.total = activeRowCount()
  const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
}

// ═══ 数据加载（每个 load 都是真实 GET；保存后必须调用它回读） ═══
async function loadWechat() {
  loading.value = true
  try {
    const res: any = await paymentConfigApi.listItems('wechat', searchForm.wechatKeyword || undefined)
    wechatRows.value = res || []
    wechatEdit.value = Object.fromEntries(
      (res || []).map((r: PaymentConfigItemVO) => [r.itemKey, r.itemValue ?? ''])
    )
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载微信公众号配置失败')
    wechatRows.value = []
  } finally {
    loading.value = false
    syncTotal()
  }
}

async function loadMethods() {
  loading.value = true
  try {
    const res: any = await paymentConfigApi.listChannels({
      keyword: searchForm.methodsKeyword || undefined,
      enabled: searchForm.methodsEnabled,
    })
    methodRows.value = res || []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载支付方式失败')
    methodRows.value = []
  } finally {
    loading.value = false
    syncTotal()
  }
}

async function loadScenes() {
  loading.value = true
  try {
    const res: any = await paymentConfigApi.listScenes(searchForm.scenesKeyword || undefined)
    sceneRows.value = res || []
    sceneEdit.value = Object.fromEntries(
      (res || []).map((r: PaymentSceneVO) => [r.sceneCode, [...(r.channels || [])]])
    )
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载场景配置失败')
    sceneRows.value = []
  } finally {
    loading.value = false
    syncTotal()
  }
}

async function loadRefund() {
  loading.value = true
  try {
    const [items, notes] = await Promise.all([
      paymentConfigApi.listItems('refund', searchForm.refundKeyword || undefined),
      paymentConfigApi.refundNotes(),
    ])
    refundRows.value = (items as any) || []
    refundEdit.value = Object.fromEntries(
      (refundRows.value).map(r => [r.itemKey, r.itemValue ?? ''])
    )
    refundNotes.value = (notes as any) || []
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载在线退款配置失败')
    refundRows.value = []
    refundNotes.value = []
  } finally {
    loading.value = false
    syncTotal()
  }
}

/** 按当前 Tab 加载（切 Tab 与「刷新」都走这里） */
function loadActiveTab() {
  if (activeTab.value === 'wechat') return loadWechat()
  if (activeTab.value === 'methods') return loadMethods()
  if (activeTab.value === 'scenes') return loadScenes()
  return loadRefund()
}

/**
 * 切 Tab：换 storage-key 并**重新加载数据**。
 * · storage-key：模板里每个 Tab 是一张独立的 BillDetailTable（v-if），各自带
 *   `set-payment-config-columns-${tab}`，故切 Tab 即换 key、且表组件重新挂载重新读列配置；
 * · 数据：原实现两 Tab 在 onMounted 一次性拉取、切 Tab 不发任何请求（P1），这里改为每次都重新 GET。
 */
function handleTabChange(key: string) {
  if (key === activeTab.value) return
  activeTab.value = key
  pagination.current = 1
  loadActiveTab()
}

function handleSearch() {
  pagination.current = 1
  loadActiveTab()
}

function handleReset() {
  Object.assign(searchForm, {
    wechatKeyword: '',
    methodsKeyword: '',
    methodsEnabled: undefined,
    methodsAmount: undefined,
    scenesKeyword: '',
    refundKeyword: '',
  })
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  syncTotal()
}

// ═══ Tab① 微信公众号配置：行内编辑 + 保存后回读 ═══
function setWechatEdit(itemKey: string, value: string) {
  wechatEdit.value = { ...wechatEdit.value, [itemKey]: value }
}

async function saveWechatItems() {
  if (!wechatRows.value.length) {
    message.warning('没有可保存的配置项')
    return
  }
  saving.value = true
  try {
    for (const row of wechatRows.value) {
      await paymentConfigApi.saveItem(row.itemKey, wechatEdit.value[row.itemKey] ?? '')
    }
    message.success('保存成功')
    // 必须重新 GET 回读：只有服务端返回的值才算保存成功（避免「假保存」再次发生）
    await loadWechat()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ Tab③ 场景配置：矩阵勾选 + 保存后回读 ═══
function isSceneChannelChecked(sceneCode: string, channelCode: string): boolean {
  return (sceneEdit.value[sceneCode] || []).includes(channelCode)
}

function setSceneChannel(sceneCode: string, channelCode: string, checked: boolean) {
  const current = new Set(sceneEdit.value[sceneCode] || [])
  if (checked) current.add(channelCode)
  else current.delete(channelCode)
  sceneEdit.value = { ...sceneEdit.value, [sceneCode]: [...current] }
}

/** 已启用支付方式的中文汇总（列头由 column.key 提供渠道码） */
function sceneChannelNames(sceneCode: string): string {
  const codes = sceneEdit.value[sceneCode] || []
  return codes.map(c => PAYMENT_CHANNEL_MAP[c]?.name || c).join('、')
}

async function saveSceneRows() {
  if (!sceneRows.value.length) {
    message.warning('没有可保存的场景')
    return
  }
  saving.value = true
  try {
    for (const row of sceneRows.value) {
      await paymentConfigApi.saveScene(row.sceneCode, sceneEdit.value[row.sceneCode] || [])
    }
    message.success('保存成功')
    await loadScenes()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ Tab④ 在线退款：开关 + 保存后回读 ═══
function setRefundEdit(itemKey: string, value: string) {
  refundEdit.value = { ...refundEdit.value, [itemKey]: value }
}

async function saveRefundItems() {
  if (!refundRows.value.length) {
    message.warning('没有可保存的配置项')
    return
  }
  saving.value = true
  try {
    for (const row of refundRows.value) {
      await paymentConfigApi.saveItem(row.itemKey, refundEdit.value[row.itemKey] ?? '')
    }
    message.success('保存成功')
    await loadRefund()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ Tab② 支付方式：渠道参数抽屉 ═══
const paramVisible = ref(false)
const paramLoading = ref(false)
const paramSaving = ref(false)
const paramFormRef = ref()
const currentChannel = ref<PaymentChannelConfigVO | null>(null)

const paramForm = reactive<Required<PaymentChannelParam>>({
  appId: '',
  merchantNo: '',
  appSecret: '',
  notifyUrl: '',
  enabled: true,
  // 签名凭据（2026-09-26）：只有公钥能验、没有私钥签不了
  alipayPrivateKey: '',
  alipayGateway: '',
  // 回调验签凭据（2026-09-21）
  alipayPublicKey: '',
  wechatApiV3Key: '',
  wechatPlatformCerts: '',
  unionPayCerts: '',
  // 签名凭据（2026-09-26）
  unionPayMerchantPrivateKey: '',
  unionPayCertId: '',
  unionPayGateway: '',
})

/** 在线渠道必须填商户号与密钥；线下渠道（BANK/CASH）无需填（与后端 confirmOfflinePayment 的口径一致） */
const ONLINE_CHANNELS = ['ALIPAY', 'WECHAT', 'UNIONPAY']

/** 当前抽屉里的渠道码（大写） */
const paramChannelCode = computed(() =>
  String(currentChannel.value?.channelCode || '').toUpperCase(),
)
const isAlipayChannel = computed(() => paramChannelCode.value === 'ALIPAY')
const isWechatChannel = computed(() => paramChannelCode.value === 'WECHAT')
const isUnionPayChannel = computed(() => paramChannelCode.value === 'UNIONPAY')

const CERTS_PLACEHOLDER =
  '{\n  "证书序列号": "-----BEGIN PUBLIC KEY-----\\n...\\n-----END PUBLIC KEY-----"\n}'

/**
 * 平台证书表的即时校验。
 *
 * <p>⚠️ 为什么要在**保存前**校验而不是交给后端：后端 `parseCerts` 遇到非法 JSON 会
 * 静默返回空表（fail-closed），表现为「保存成功、但回调永远被拒」——
 * 管理员会以为配好了。这里提前拦住，避免把「JSON 写错」变成「回调莫名其妙不工作」。</p>
 */
const certsError = computed(() => {
  // 微信与银联都是「证书表」形状，共用同一套校验；当前渠道不适用则跳过
  const raw = (isWechatChannel.value
    ? paramForm.wechatPlatformCerts
    : isUnionPayChannel.value
      ? paramForm.unionPayCerts
      : ''
  )?.trim() || ''
  if (!isWechatChannel.value && !isUnionPayChannel.value) return ''
  if (!raw) return '' // 允许留空（此时回调会被拒，但配置本身可保存）
  try {
    const parsed = JSON.parse(raw)
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return '必须是 JSON 对象：{"证书序列号":"PEM 公钥"}'
    }
    const bad = Object.entries(parsed).find(
      ([k, v]) => !k || typeof v !== 'string' || !String(v).includes('BEGIN PUBLIC KEY'),
    )
    if (bad) return `条目「${bad[0]}」的值不像 PEM 公钥（应含 BEGIN PUBLIC KEY）`
    return ''
  } catch {
    return '不是合法 JSON，请检查引号与换行转义'
  }
})

const paramRules = computed(() => {
  const code = String(currentChannel.value?.channelCode || '').toUpperCase()
  const requiredOnline = ONLINE_CHANNELS.includes(code)
  return {
    merchantNo: requiredOnline
      ? [{ required: true, message: '在线渠道必须填写商户号', trigger: 'blur' }]
      : [],
    appSecret: requiredOnline
      ? [{ required: true, message: '在线渠道必须填写 API 密钥', trigger: 'blur' }]
      : [],
    notifyUrl: [
      {
        validator: (_rule: any, value: string) => {
          if (!value || /^https?:\/\/.+/i.test(value)) return Promise.resolve()
          return Promise.reject(new Error('异步通知 URL 必须以 http:// 或 https:// 开头'))
        },
        trigger: 'blur',
      },
    ],
  }
})

async function openParamDrawer(record: PaymentChannelConfigVO) {
  currentChannel.value = record
  // 先清空再打开，避免残留上一渠道的值
  paramForm.appId = ''
  paramForm.merchantNo = ''
  paramForm.appSecret = ''
  paramForm.notifyUrl = ''
  paramForm.enabled = true
  paramForm.alipayPrivateKey = ''
  paramForm.alipayGateway = ''
  paramForm.alipayPublicKey = ''
  paramForm.wechatApiV3Key = ''
  paramForm.wechatPlatformCerts = ''
  paramForm.unionPayCerts = ''
  paramForm.unionPayMerchantPrivateKey = ''
  paramForm.unionPayCertId = ''
  paramForm.unionPayGateway = ''
  paramVisible.value = true
  paramLoading.value = true
  try {
    const saved: any = await paymentConfigApi.getChannelParam(record.channelCode)
    if (saved) {
      paramForm.appId = saved.appId || ''
      paramForm.merchantNo = saved.merchantNo || ''
      paramForm.appSecret = saved.appSecret || ''
      paramForm.notifyUrl = saved.notifyUrl || ''
      paramForm.enabled = saved.enabled !== false
      // 签名/验签凭据：后端按 String 存（证书表本身是一段 JSON 文本），原样回显
      paramForm.alipayPrivateKey = saved.alipayPrivateKey || ''
      paramForm.alipayGateway = saved.alipayGateway || ''
      paramForm.alipayPublicKey = saved.alipayPublicKey || ''
      paramForm.wechatApiV3Key = saved.wechatApiV3Key || ''
      paramForm.wechatPlatformCerts = saved.wechatPlatformCerts || ''
      paramForm.unionPayCerts = saved.unionPayCerts || ''
      paramForm.unionPayMerchantPrivateKey = saved.unionPayMerchantPrivateKey || ''
      paramForm.unionPayCertId = saved.unionPayCertId || ''
      paramForm.unionPayGateway = saved.unionPayGateway || ''
    }
    await nextTick()
    paramFormRef.value?.clearValidate?.()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '渠道参数读取失败')
  } finally {
    paramLoading.value = false
  }
}

async function saveParam() {
  if (!currentChannel.value) return
  try {
    await paramFormRef.value?.validate()
  } catch {
    return
  }
  // 平台证书表格式不对就不让保存：后端遇到非法 JSON 会静默当成「没配」，
  // 表现是「保存成功但回调永远被拒」，比当场报错难查得多
  if (certsError.value) {
    message.error(`平台证书表格式有误：${certsError.value}`)
    return
  }
  paramSaving.value = true
  try {
    await paymentConfigApi.saveChannelParam(currentChannel.value.channelCode, { ...paramForm })
    message.success('保存成功')
    paramVisible.value = false
    // 重新 GET 回读，列表里的商户号/是否已配置/启用状态随之更新
    await loadMethods()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    paramSaving.value = false
  }
}

// ═══ 初始化 ═══
function handleError(error: Error) {
  console.error('[支付配置] 页面错误', error)
}

onMounted(async () => {
  loadPageConfig()
  await nextTick()
  loadActiveTab()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.toolbar-tip { color: #999; font-size: 12px; }
.cell-muted { color: #bbb; }
/* 异步通知 URL 过长时省略（DetailColumnConfig 无 ellipsis 字段，用 className 实现） */
:deep(.cell-ellipsis) { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.drawer-tip { color: #fa8c16; font-size: 12px; line-height: 1.6; margin-bottom: 12px; }

/* 在线退款的两段说明文案（逐字对标 ql361，后端下发） */
.refund-notes { flex-shrink: 0; padding: 10px 12px; border-top: 1px solid #f0f0f0; background: #fff; }
.refund-note { color: #666; font-size: 12px; line-height: 1.8; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
