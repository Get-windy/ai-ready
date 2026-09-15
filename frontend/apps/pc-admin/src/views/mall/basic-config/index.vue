<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
    >
      <!--
        页面定位：商城 → 商城设置 → 基础设置（对标 ql361「商城 → 商城设置 → 基础设置」）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/基础设置开发文档.md
        结构说明：左侧 6 子项导航（基础设置 / 显示设置 / 图片水印 / 访问设置 / 绑定小程序 / 消息设置）
                 + 右侧配置表单（按「分组卡片」组织字段）+ 底部统一「保存」（按子项分别提交）
        金标准：ErrorBoundary > PageContainer(full-height) > 左子项导航 + 右分组表单
        API：shopConfigApi（GET/PUT /erp/mall/admin/config，单行 ShopConfig 实体）
        保存口径：ShopConfig 为三页共用单行实体，保存时以「完整配置 + 当前子项字段」合并提交，避免互相覆盖
        存储口径：布尔开关库内为 INTEGER 0/1；JSON 结构化字段（营业资质/联系方式/显示字段/消息订阅）库内为 TEXT，
                  由前端 JSON.stringify / JSON.parse（try-catch 兜底）转换
        后端：已由 Flyway V11.361.7__Add_Tenant_Shop_Config_GoldStandard_Fields.sql 补齐本页全部列
      -->
      <div class="config-layout">
        <!-- ═══ 左侧：6 子项导航 ═══ -->
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
          data-testid="basic-config-form"
        >
          <a-spin :spinning="loading">
            <a-form
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
              size="small"
            >
              <!-- ══════ 子项一：基础设置（3 分组） ══════ -->
              <template v-if="currentMenuKey === 'basic'">
                <a-card
                  title="基础信息"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 保留下方「商城状态」为本系统已在库字段（shop_config.status），对标文档未单列该项，按原业务保留 -->
                  <a-form-item label="商城状态">
                    <a-switch
                      v-model:checked="basic.enabled"
                      checked-children="启用"
                      un-checked-children="停用"
                    />
                    <div class="form-tip">
                      停用后买家端将无法访问商城（本系统已在库字段 status，保留原业务逻辑）
                    </div>
                  </a-form-item>
                  <a-form-item
                    label="商城LOGO"
                    extra="建议上传图片尺寸为 200*60"
                  >
                    <!-- 已落库（shop_logo）；⚠️ 仍缺文件上传端点，此处为 URL 文本输入（见「剩余缺口」） -->
                    <a-input
                      v-model:value="basic.shopLogoUrl"
                      placeholder="图片URL（建议 200*60）"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="公众号二维码">
                    <a-input
                      v-model:value="basic.officialQrCode"
                      placeholder="图片URL"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item
                    label="商城名称"
                    required
                  >
                    <a-input
                      v-model:value="basic.shopName"
                      :maxlength="50"
                      placeholder="请输入商城名称"
                    />
                  </a-form-item>
                  <a-form-item
                    label="主营类目"
                    required
                  >
                    <a-select
                      v-model:value="basic.mainCategory"
                      placeholder="请选择主营类目"
                      allow-clear
                      :options="MAIN_CATEGORY_OPTIONS"
                    />
                  </a-form-item>
                  <a-form-item
                    label="营业资质"
                    required
                  >
                    <!-- 已落库（qualifications JSON：{license,foodPermit,other1..other4}）；
                         ⚠️ 仍为 URL 文本输入，缺图片上传端点 -->
                    <a-space
                      direction="vertical"
                      style="width: 100%"
                    >
                      <div
                        v-for="q in QUALIFICATION_SLOTS"
                        :key="q.key"
                        class="qualification-row"
                      >
                        <span class="qualification-row__label">{{ q.label }}</span>
                        <a-input
                          v-model:value="basic.qualifications[q.key]"
                          placeholder="图片URL"
                          allow-clear
                        />
                      </div>
                      <div class="form-tip">
                        据《电商法》第十五、十六条规定需公示营业执照与经营许可证；食品行业需公示食品经营许可证（建议上传尺寸 620*422）
                      </div>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="商城介绍">
                    <a-textarea
                      v-model:value="basic.shopDesc"
                      :rows="4"
                      :maxlength="500"
                      show-count
                      placeholder="主要用于微信分享商城链接时，接收方所看到的信息"
                    />
                  </a-form-item>
                </a-card>

                <a-card
                  title="联系方式"
                  :bordered="false"
                  class="group-card"
                >
                  <a-form-item label="QQ在线客服">
                    <a-button
                      type="link"
                      size="small"
                      class="link-btn"
                      @click="handleHelp('如何开启QQ在线客服')"
                    >
                      如何开启QQ在线客服
                    </a-button>
                  </a-form-item>
                  <a-form-item label="联系方式">
                    <!-- 已落库（contacts JSON 数组：[{type,number}]） -->
                    <a-space
                      direction="vertical"
                      style="width: 100%"
                    >
                      <div
                        v-for="(row, idx) in basic.contacts"
                        :key="row.rowKey"
                        class="contact-row"
                      >
                        <a-select
                          v-model:value="row.type"
                          style="width: 120px"
                          :options="CONTACT_TYPE_OPTIONS"
                          placeholder="类型"
                        />
                        <a-input
                          v-model:value="row.number"
                          style="width: 220px"
                          placeholder="号码"
                        />
                        <a-button
                          type="link"
                          size="small"
                          danger
                          @click="removeContact(idx)"
                        >
                          删除
                        </a-button>
                      </div>
                      <a-button
                        size="small"
                        @click="addContact"
                      >
                        <PlusOutlined /> 新增联系方式
                      </a-button>
                    </a-space>
                  </a-form-item>
                </a-card>

                <a-card
                  title="退货地址"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（return_consignee / return_phone / return_address） -->
                  <a-form-item label="收件人">
                    <a-input
                      v-model:value="basic.returnConsignee"
                      placeholder="请输入收件人"
                      :maxlength="50"
                    />
                  </a-form-item>
                  <a-form-item label="收件电话">
                    <a-input
                      v-model:value="basic.returnPhone"
                      placeholder="请输入收件电话"
                      :maxlength="20"
                    />
                  </a-form-item>
                  <a-form-item label="收件地址">
                    <a-input
                      v-model:value="basic.returnAddress"
                      placeholder="请输入收件地址"
                      :maxlength="200"
                    />
                  </a-form-item>
                </a-card>
              </template>

              <!-- ══════ 子项二：显示设置（2 分组） ══════ -->
              <template v-else-if="currentMenuKey === 'display'">
                <a-card
                  title="商品详情页显示字段设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（display_detail_fields JSON 数组） -->
                  <a-form-item label="显示字段">
                    <a-checkbox-group
                      v-model:value="display.detailFields"
                      :options="DETAIL_FIELD_OPTIONS"
                    />
                  </a-form-item>
                </a-card>

                <a-card
                  title="商品列表页显示字段设置"
                  :bordered="false"
                  class="group-card"
                >
                  <div class="group-tip">
                    包含商城首页、商品列表、我的收藏、常购清单、搜索结果页
                  </div>
                  <!-- 已落库（display_list_fields JSON 数组） -->
                  <a-form-item label="显示字段">
                    <a-checkbox-group
                      v-model:value="display.listFields"
                      :options="LIST_FIELD_OPTIONS"
                    />
                  </a-form-item>
                </a-card>
              </template>

              <!-- ══════ 子项三：图片水印 ══════ -->
              <template v-else-if="currentMenuKey === 'watermark'">
                <a-card
                  title="图片水印"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（watermark_type / watermark_image / watermark_text） -->
                  <a-form-item label="水印方式">
                    <a-radio-group v-model:value="watermark.type">
                      <a-radio value="none">
                        不使用水印
                      </a-radio>
                      <a-radio value="image">
                        图片水印
                      </a-radio>
                      <a-radio value="text">
                        文字水印
                      </a-radio>
                    </a-radio-group>
                  </a-form-item>
                  <template v-if="watermark.type === 'image'">
                    <a-form-item label="水印图片">
                      <a-input
                        v-model:value="watermark.image"
                        placeholder="水印图片URL"
                        allow-clear
                      />
                    </a-form-item>
                  </template>
                  <template v-else-if="watermark.type === 'text'">
                    <a-form-item label="水印文字">
                      <a-input
                        v-model:value="watermark.text"
                        placeholder="请输入水印文字"
                        :maxlength="50"
                      />
                    </a-form-item>
                  </template>
                  <div class="form-tip">
                    实测对标仅确认 3 种水印类型；透明度/位置等细化参数未实测，暂不提供
                  </div>
                </a-card>
              </template>

              <!-- ══════ 子项四：访问设置 ══════ -->
              <template v-else-if="currentMenuKey === 'access'">
                <a-card
                  title="访问设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（mall_qr_code / wechat_link / auth_domain / default_warehouse_id） -->
                  <a-form-item label="微商城二维码">
                    <a-input
                      v-model:value="access.mallQrCode"
                      placeholder="图片URL"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="微信版链接地址">
                    <a-input
                      v-model:value="access.wechatLink"
                      readonly
                      placeholder="保存后由后端生成"
                    />
                  </a-form-item>
                  <a-form-item label="网页授权域名">
                    <a-radio-group v-model:value="access.authDomain">
                      <a-radio
                        v-for="d in AUTH_DOMAIN_OPTIONS"
                        :key="d.value"
                        :value="d.value"
                      >
                        {{ d.label }}
                      </a-radio>
                    </a-radio-group>
                    <div class="form-tip">
                      需在公众号/小程序开发配置中把授权域名改为所选地址
                    </div>
                  </a-form-item>
                  <a-form-item label="网店仓库">
                    <a-select
                      v-model:value="access.defaultWarehouseId"
                      placeholder="请选择默认仓库"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                      :options="warehouseOptions"
                      :loading="warehouseLoading"
                    />
                    <div class="form-tip">
                      通过微信公众号跳转到微商城时的默认仓库
                    </div>
                  </a-form-item>
                  <a-form-item label="仓库二维码">
                    <a-empty
                      v-if="!warehouseOptions.length"
                      description="暂无仓库数据"
                    />
                    <a-space
                      v-else
                      wrap
                    >
                      <a-tag
                        v-for="w in warehouseOptions"
                        :key="w.value"
                      >
                        {{ w.label }}
                      </a-tag>
                    </a-space>
                    <div class="form-tip">
                      仓库二维码需后端按仓库生成，配置端点仍缺（本列仅显示仓库名）
                    </div>
                  </a-form-item>
                </a-card>
              </template>

              <!-- ══════ 子项五：绑定小程序（2 分组） ══════ -->
              <template v-else-if="currentMenuKey === 'miniapp'">
                <a-card
                  title="绑定小程序"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（miniapp_appid/appsecret/pay_mch_id/pay_mch_key/mall_url + b2b_pay_*）；
                       ⚠️ 当前为明文存储，「凭据加密 + 脱敏返回」仍未闭环 -->
                  <a-form-item label="appid">
                    <a-input
                      v-model:value="miniapp.appid"
                      placeholder="请到小程序开发者中心获取并填写；appid 为空则其他项不需填"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="微信小程序授权">
                    <a-button
                      size="small"
                      @click="handleHelp('微信小程序授权')"
                    >
                      去授权
                    </a-button>
                  </a-form-item>
                  <a-form-item label="appsecret">
                    <a-input-password
                      v-model:value="miniapp.appsecret"
                      placeholder="请输入 appsecret"
                    />
                  </a-form-item>
                  <a-form-item label="支付商户号">
                    <a-input
                      v-model:value="miniapp.payMchId"
                      placeholder="支付商户号为空则支付商户密钥也不需填写"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="支付商户密钥">
                    <a-input-password
                      v-model:value="miniapp.payMchKey"
                      placeholder="请输入支付商户密钥"
                    />
                  </a-form-item>
                  <a-form-item label="小程序商城地址">
                    <a-input
                      v-model:value="miniapp.mallUrl"
                      readonly
                      placeholder="保存后由后端生成"
                    />
                  </a-form-item>
                  <a-form-item label="操作">
                    <a-space>
                      <a-button
                        size="small"
                        @click="handleHelp('下载小程序')"
                      >
                        下载小程序
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        class="link-btn"
                        @click="handleHelp('如何绑定小程序')"
                      >
                        如何绑定小程序
                      </a-button>
                    </a-space>
                  </a-form-item>
                </a-card>

                <a-card
                  title="门店助手B To B支付"
                  :bordered="false"
                  class="group-card"
                >
                  <a-form-item label="支付商户号">
                    <a-input
                      v-model:value="miniapp.b2bPayMchId"
                      placeholder="支付商户号为空则支付商户密钥也不需填写"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="支付商户密钥">
                    <a-input-password
                      v-model:value="miniapp.b2bPayMchKey"
                      placeholder="请输入支付商户密钥"
                    />
                  </a-form-item>
                </a-card>
              </template>

              <!-- ══════ 子项六：消息设置（16 项） ══════ -->
              <template v-else-if="currentMenuKey === 'message'">
                <a-card
                  title="系统消息"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（message_subscribe JSON：16 项开关） -->
                  <a-form-item label="消息开关">
                    <a-space
                      direction="vertical"
                      size="small"
                      style="width: 100%"
                    >
                      <div
                        v-for="m in MESSAGE_ITEMS"
                        :key="m.key"
                        class="message-row"
                      >
                        <a-switch
                          v-model:checked="messageSettings[m.key]"
                          size="small"
                        />
                        <span class="message-row__label">{{ m.label }}</span>
                      </div>
                    </a-space>
                  </a-form-item>
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
                  <a-button @click="loadConfig">
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
 * 基础设置（商城 → 商城设置 → 基础设置）
 * 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/基础设置开发文档.md
 * 结构：左侧 6 子项导航 + 右侧分组表单；按子项分别保存（合并提交单行 ShopConfig）
 *
 * 存储口径（Flyway V11.361.7__Add_Tenant_Shop_Config_GoldStandard_Fields.sql 已补齐本页全部列）：
 *   · 布尔开关：库内 INTEGER 0/1（本页无布尔开关，商城状态用既有 status）
 *   · JSON 结构化字段（TEXT 列，前端 JSON.stringify/JSON.parse，try-catch 兜底）：
 *       qualifications（营业资质 6 槽位） / contacts（联系方式多值）
 *       display_detail_fields（详情显示字段，10 选） / display_list_fields（列表显示字段）
 *       message_subscribe（16 项消息订阅开关）
 *   · 普通文本列：official_qr_code / main_category / return_consignee / return_phone /
 *       return_address / watermark_type / watermark_image / watermark_text / mall_qr_code /
 *       wechat_link / auth_domain / default_warehouse_id / miniapp_appid 等
 *   · 已落库复用列：shop_name / shop_logo / shop_desc / status
 *
 * ⚠️ 仍缺（保留缺口，不编造）：素材文件上传端点（略图为 URL 文本输入）、仓库二维码生成端点、
 *    小程序凭据加密+脱敏、对标「物流」类未实测字段（见各页文档「剩余缺口」）。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  shopConfigApi,
  parseJsonField,
  stringifyJsonField,
  type ShopConfig,
} from '@/api/erp/mall'
import optionsApi from '@/api/options'

defineOptions({ name: 'MallBasicConfig' })

// ═══ 左侧 6 子项导航（对标实测） ═══
const MENU_ITEMS = [
  { key: 'basic', label: '基础设置' },
  { key: 'display', label: '显示设置' },
  { key: 'watermark', label: '图片水印' },
  { key: 'access', label: '访问设置' },
  { key: 'miniapp', label: '绑定小程序' },
  { key: 'message', label: '消息设置' },
] as const
const currentMenuKey = ref<string>('basic')
const currentMenuLabel = computed(() => MENU_ITEMS.find(m => m.key === currentMenuKey.value)?.label || '')

function onMenuClick({ key }: { key: string | number }) {
  currentMenuKey.value = String(key)
}

// ═══ 子项一 · 基础设置 ═══
const QUALIFICATION_SLOTS = [
  { key: 'license', label: '营业执照' },
  { key: 'foodPermit', label: '食品经营许可证' },
  { key: 'other1', label: '其他证照1' },
  { key: 'other2', label: '其他证照2' },
  { key: 'other3', label: '其他证照3' },
  { key: 'other4', label: '其他证照4' },
]
const MAIN_CATEGORY_OPTIONS = [
  { label: '食品百货', value: '食品百货' },
  { label: '酒水饮料', value: '酒水饮料' },
  { label: '生鲜农贸', value: '生鲜农贸' },
  { label: '办公用品', value: '办公用品' },
  { label: '母婴用品', value: '母婴用品' },
  { label: '化妆用品', value: '化妆用品' },
  { label: '五金建材', value: '五金建材' },
  { label: '家电数码', value: '家电数码' },
  { label: '医药制品', value: '医药制品' },
  { label: '服装鞋帽', value: '服装鞋帽' },
]
const CONTACT_TYPE_OPTIONS = [
  { label: '微信', value: '微信' },
  { label: '手机', value: '手机' },
  { label: 'QQ', value: 'QQ' },
  { label: '电话', value: '电话' },
]

const basic = reactive({
  enabled: true,
  shopName: '',
  shopDesc: '',
  shopLogoUrl: '',
  officialQrCode: '',
  mainCategory: undefined as string | undefined,
  qualifications: {
    license: '', foodPermit: '', other1: '', other2: '', other3: '', other4: '',
  } as Record<string, string>,
  contacts: [{ rowKey: 'c1', type: '微信', number: '' }] as { rowKey: string; type: string; number: string }[],
  returnConsignee: '',
  returnPhone: '',
  returnAddress: '',
})

/** 新建一条联系方式行 */
function newContact(seq: number) {
  return { rowKey: `c${seq}`, type: '微信', number: '' }
}
let contactSeq = 1
function addContact() {
  contactSeq += 1
  basic.contacts.push(newContact(contactSeq))
}
function removeContact(idx: number) {
  basic.contacts.splice(idx, 1)
}

// ═══ 子项二 · 显示设置 ═══
const DETAIL_FIELD_OPTIONS = [
  { label: '规格', value: 'spec' },
  { label: '型号', value: 'model' },
  { label: '货号', value: 'productCode' },
  { label: '产地', value: 'origin' },
  { label: '品牌', value: 'brand' },
  { label: '商品条码', value: 'barcode' },
  { label: '保质期', value: 'shelfLife' },
  { label: '生产日期', value: 'produceDate' },
  { label: '重量（kg）', value: 'weight' },
  { label: '体积（m³）', value: 'volume' },
]
const LIST_FIELD_OPTIONS = [{ label: '批次号', value: 'batchNo' }]
/** 对标实测默认勾选：未勾选 货号 / 品牌 / 重量 / 体积 / 批次号 */
const DEFAULT_DETAIL_FIELDS = ['spec', 'model', 'origin', 'barcode', 'shelfLife', 'produceDate']
const display = reactive({
  detailFields: [...DEFAULT_DETAIL_FIELDS] as string[],
  listFields: [] as string[],
})

// ═══ 子项三 · 图片水印 ═══
const watermark = reactive({
  type: 'none' as 'none' | 'image' | 'text',
  image: '',
  text: '',
})

// ═══ 子项四 · 访问设置 ═══
const AUTH_DOMAIN_OPTIONS = [
  { label: 'gateway.ql361.com', value: 'gateway.ql361.com' },
  { label: 'shop200004989.ql361.shop', value: 'shop200004989.ql361.shop' },
  { label: 'smallshop200004989.ql361.shop', value: 'smallshop200004989.ql361.shop' },
]
const access = reactive({
  mallQrCode: '',
  wechatLink: '',
  authDomain: 'gateway.ql361.com',
  defaultWarehouseId: undefined as number | undefined,
})
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const warehouseLoading = ref(false)

// ═══ 子项五 · 绑定小程序 ═══
const miniapp = reactive({
  appid: '',
  appsecret: '',
  payMchId: '',
  payMchKey: '',
  mallUrl: '',
  b2bPayMchId: '',
  b2bPayMchKey: '',
})

// ═══ 子项六 · 消息设置（实测 16 项，默认全部开启） ═══
const MESSAGE_ITEMS = [
  { key: 'registerSuccess', label: '注册成功' },
  { key: 'modifyPassword', label: '修改密码' },
  { key: 'orderCancel', label: '订单取消' },
  { key: 'orderTerminate', label: '订单终止' },
  { key: 'orderAudit', label: '订单审核' },
  { key: 'returnAudit', label: '退货审核' },
  { key: 'refund', label: '退款' },
  { key: 'tradeLogistics', label: '交易物流' },
  { key: 'orderPartDelivery', label: '销售订单部分发货' },
  { key: 'orderAllDelivery', label: '销售订单全部发货' },
  { key: 'orderOnceDelivery', label: '销售订单一次全部发货' },
  { key: 'autoSign', label: '系统自动签收' },
  { key: 'other', label: '其他' },
  { key: 'couponReturn', label: '优惠券退回' },
  { key: 'couponGrant', label: '优惠券赠送' },
  { key: 'promotion', label: '促销活动' },
]
const messageSettings = reactive<Record<string, boolean>>(
  Object.fromEntries(MESSAGE_ITEMS.map(m => [m.key, true]))
)

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
    // 已落库字段回填
    basic.enabled = (cfg?.status ?? 1) === 1
    basic.shopName = cfg?.shopName || ''
    basic.shopDesc = cfg?.shopDesc || ''
    basic.shopLogoUrl = cfg?.shopLogo || ''
    // ── V11.361.7 起本页全部字段真实回填 ──
    basic.officialQrCode = cfg?.officialQrCode || ''
    basic.mainCategory = cfg?.mainCategory || undefined
    basic.qualifications = parseJsonField<Record<string, string>>(cfg?.qualifications, emptyQualifications())
    basic.contacts = parseJsonField<{ rowKey?: string; type: string; number: string }[]>(cfg?.contacts, [])
      .filter(c => c && typeof c === 'object')
      .map((c, i) => ({ rowKey: `c${i + 1}`, type: String(c.type ?? ''), number: String(c.number ?? '') }))
    if (!basic.contacts.length) basic.contacts = [newContact(1)]
    contactSeq = basic.contacts.length
    basic.returnConsignee = cfg?.returnConsignee || ''
    basic.returnPhone = cfg?.returnPhone || ''
    basic.returnAddress = cfg?.returnAddress || ''
    display.detailFields = parseJsonField<string[]>(cfg?.displayDetailFields, [...DEFAULT_DETAIL_FIELDS])
      .filter(f => typeof f === 'string')
    display.listFields = parseJsonField<string[]>(cfg?.displayListFields, []).filter(f => typeof f === 'string')
    watermark.type = (['none', 'image', 'text'].includes(String(cfg?.watermarkType))
      ? String(cfg?.watermarkType)
      : 'none') as 'none' | 'image' | 'text'
    watermark.image = cfg?.watermarkImage || ''
    watermark.text = cfg?.watermarkText || ''
    access.mallQrCode = cfg?.mallQrCode || ''
    access.wechatLink = cfg?.wechatLink || ''
    access.authDomain = cfg?.authDomain || AUTH_DOMAIN_OPTIONS[0].value
    access.defaultWarehouseId = cfg?.defaultWarehouseId ?? undefined
    miniapp.appid = cfg?.miniappAppid || ''
    miniapp.appsecret = cfg?.miniappAppsecret || ''
    miniapp.payMchId = cfg?.miniappPayMchId || ''
    miniapp.payMchKey = cfg?.miniappPayMchKey || ''
    miniapp.mallUrl = cfg?.miniappMallUrl || ''
    miniapp.b2bPayMchId = cfg?.b2bPayMchId || ''
    miniapp.b2bPayMchKey = cfg?.b2bPayMchKey || ''
    const savedMsg = parseJsonField<Record<string, boolean>>(cfg?.messageSubscribe, {})
    MESSAGE_ITEMS.forEach((m) => { messageSettings[m.key] = savedMsg[m.key] !== false })
  } catch (e) {
    console.warn('[基础设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

/** 空白营业资质槽位 */
function emptyQualifications(): Record<string, string> {
  return { license: '', foodPermit: '', other1: '', other2: '', other3: '', other4: '' }
}

function handleError(err: any) {
  console.warn('[基础设置] ErrorBoundary:', err)
}

/** 按子项校验（仅基础设置子项含必填） */
function validateCurrentItem(): boolean {
  if (currentMenuKey.value === 'basic') {
    if (!basic.shopName.trim()) {
      message.warning('请输入商城名称')
      return false
    }
    if (!basic.mainCategory) {
      message.warning('请选择主营类目')
      return false
    }
    if (!basic.qualifications.license.trim()) {
      message.warning('请上传营业资质（营业执照）')
      return false
    }
  }
  return true
}

/** 按子项分别保存：仅提交当前子项维护的字段 + 完整配置合并
 *  （后端 updateById 忽略 null 字段，故未提交字段不会被清空） */
async function handleSave() {
  if (!validateCurrentItem()) return
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      status: basic.enabled ? 1 : 0,
      shopName: basic.shopName.trim() || fullConfig?.shopName || '订货商城',
      shopDesc: basic.shopDesc,
      shopLogo: basic.shopLogoUrl,
    }
    if (currentMenuKey.value === 'basic') {
      // 文本列
      payload.officialQrCode = basic.officialQrCode
      payload.mainCategory = basic.mainCategory
      payload.returnConsignee = basic.returnConsignee
      payload.returnPhone = basic.returnPhone
      payload.returnAddress = basic.returnAddress
      // JSON 列：营业资质 / 联系方式（联系方式过滤掉完全空白的行）
      payload.qualifications = stringifyJsonField(basic.qualifications)
      payload.contacts = stringifyJsonField(
        basic.contacts
          .filter(c => String(c.type || '').trim() || String(c.number || '').trim())
          .map(c => ({ type: c.type, number: c.number })),
      )
    }
    if (currentMenuKey.value === 'display') {
      payload.displayDetailFields = stringifyJsonField([...display.detailFields])
      payload.displayListFields = stringifyJsonField([...display.listFields])
    }
    if (currentMenuKey.value === 'watermark') {
      payload.watermarkType = watermark.type
      // 按当前水印方式只保留有效的那一项，避免残留上一次的配置
      payload.watermarkImage = watermark.type === 'image' ? watermark.image : ''
      payload.watermarkText = watermark.type === 'text' ? watermark.text : ''
    }
    if (currentMenuKey.value === 'access') {
      payload.mallQrCode = access.mallQrCode
      payload.wechatLink = access.wechatLink
      payload.authDomain = access.authDomain
      payload.defaultWarehouseId = access.defaultWarehouseId
    }
    if (currentMenuKey.value === 'miniapp') {
      payload.miniappAppid = miniapp.appid
      payload.miniappAppsecret = miniapp.appsecret
      payload.miniappPayMchId = miniapp.payMchId
      payload.miniappPayMchKey = miniapp.payMchKey
      payload.miniappMallUrl = miniapp.mallUrl
      payload.b2bPayMchId = miniapp.b2bPayMchId
      payload.b2bPayMchKey = miniapp.b2bPayMchKey
    }
    if (currentMenuKey.value === 'message') {
      payload.messageSubscribe = stringifyJsonField(
        Object.fromEntries(MESSAGE_ITEMS.map(m => [m.key, messageSettings[m.key] === true])),
      )
    }
    await shopConfigApi.update(payload)
    message.success(`${currentMenuLabel.value}已保存`)
    await loadConfig()
  } catch (e) {
    console.warn('[基础设置] 保存失败', e)
    message.error(`${currentMenuLabel.value}保存失败`)
  } finally {
    saving.value = false
  }
}

/** 帮助链接/未接入能力：统一提示 */
function handleHelp(name: string) {
  message.info(`「${name}」能力待接入（后端/开放平台对接后开放）`)
}

async function loadWarehouses() {
  warehouseLoading.value = true
  try {
    const res: any = await optionsApi.getWarehouses()
    const list = Array.isArray(res) ? res : (res?.data ?? [])
    warehouseOptions.value = list.map((w: any) => ({ label: w.name || w.warehouseName, value: w.id }))
  } catch (e) {
    console.warn('[基础设置] 仓库列表获取失败', e)
    warehouseOptions.value = []
  } finally {
    warehouseLoading.value = false
  }
}

onMounted(() => {
  loadConfig()
  loadWarehouses()
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
.group-tip {
  font-size: 12px;
  color: #8c8c8c;
  margin: -4px 0 12px;
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
.qualification-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qualification-row__label {
  width: 130px;
  flex-shrink: 0;
  color: #595959;
  font-size: 13px;
}
.contact-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.message-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.message-row__label {
  font-size: 13px;
  color: #333;
}
.config-footer {
  position: sticky;
  bottom: 0;
  padding: 12px 0 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
