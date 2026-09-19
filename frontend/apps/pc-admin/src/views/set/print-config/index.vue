<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        打印设置（设置 → 打印管理 → 打印设置，新建菜单 80930，父菜单 61205 打印管理）

        路线 A′（参数/表单类页）：ErrorBoundary > PageContainer(full-height) > SettingsLayout
          （通用「左列纵向标签 + 右栏内容」组件，与「系统参数」页同一个组件，契约匹配：
           props = items / modelValue(v-model) / title / sideWidth，内容走默认插槽）
          └─ 左列 7 项（ql361 实拍：页签「打印设置」+ 6 个模板类目）
          └─ 右栏「打印设置」视图 = 3 张分组卡片（打印设置 / 助手打印 / 远程打印）+ 底部唯一「保存」
          └─ 底部保存 → PUT /set/print-config → **await load() 回读**（只有库内值才算保存成功）

        数据口径：真实读写专用表 set_print_config（迁移 V11.402.0，开发文档 §7.3 路线 B）。
          · 字段与文案逐字取自 ql361 实拍（tool-results/ql361/设置-live/打印设置.json + .png），不发明字段；
          · 取值域（打印内容候选值、小数位上下界）由后端下发（GET 的 options），前端不写死。

        ⚠️ 如实登记的边界（禁止假装已实现）：
          1. 本页是「打印的配置入口」，不是打印引擎 —— 模板/链路/客户端/任务全部复用既有 erp-printing 能力；
             配置的**消费点**在前端共享打印组件 components/PrintDialog/printBehavior.ts（读本页配置后作用于
             打印行为）：已接线 5 项（允许打印草稿 / 单据打印小数位数 / 打印内容 / 助手打印 / 远程打印）；
             未接线 2 项（属性商品汇总打印、批次效期商品汇总打印）—— 打印引擎逐行直出、无汇总行能力，不硬造。
          2. ql361「打印内容」下拉的完整选项集已实测为 3 项（2026-09-18，证据 tool-results/ql361/设置-deep/
             _summary.md §三 3.1），由后端 options 下发；小数位档位对标为「整数~8位小数」9 档，
             本系统按主数据精度 numeric(18,4) 收敛为 0~4（本系统适配口径，已登记）。
             `?` 气泡正文仍未实测 → 文案写的是**本系统行为说明**，不冒充对标原文。
          3. 6 个模板类目：本系统**无 billtype/类目维度的模板载体**（sys_print_template 只有 page_code 列，
             且 0 行），相关 5 个打印页面在 sys_menu 中无菜单项（§6.3-P0）→ 不提供跳转（不造假入口），
             只做口径说明；是否引入类目维度并补菜单项，属待裁定项。
          4. 切标签的脏检查：ql361 行为未实测 → 本页按同组「系统参数」页的既有做法做**非阻塞提示**。
      -->
      <SettingsLayout
        v-model="currentView"
        :items="navItems"
        title="打印设置"
        @change="onViewChange"
      >
        <template v-if="currentView === PRINT_VIEW">
          <a-spin :spinning="loading">
            <!-- ═══ 卡片 1：打印设置（7 个配置项，逐字对标） ═══ -->
            <div class="param-card">
              <div class="param-card__head">
                <div class="param-card__title">打印设置</div>
              </div>
              <div class="param-card__body">
                <!-- 行 1：允许打印草稿 · 属性商品汇总打印 · 批次效期商品汇总打印 · 打印内容 -->
                <div class="param-row param-row--wrap">
                  <div class="param-item">
                    <a-checkbox v-model:checked="form.allowDraftPrint">
                      允许打印草稿
                    </a-checkbox>
                    <a-tooltip
                      :title="HELP.allowDraftPrint"
                      placement="bottom"
                    >
                      <QuestionCircleOutlined class="param-help-icon" />
                    </a-tooltip>
                  </div>

                  <div class="param-item">
                    <a-checkbox v-model:checked="form.attrSummaryPrint">
                      属性商品汇总打印
                    </a-checkbox>
                    <a-tooltip
                      :title="HELP.attrSummaryPrint"
                      placement="bottom"
                    >
                      <QuestionCircleOutlined class="param-help-icon" />
                    </a-tooltip>
                  </div>

                  <div class="param-item">
                    <a-checkbox v-model:checked="form.batchSummaryPrint">
                      批次效期商品汇总打印
                    </a-checkbox>
                    <a-tooltip
                      :title="HELP.batchSummaryPrint"
                      placement="bottom"
                    >
                      <QuestionCircleOutlined class="param-help-icon" />
                    </a-tooltip>
                  </div>

                  <div class="param-item">
                    <span class="param-item__label">打印内容</span>
                    <a-select
                      v-model:value="form.printContent"
                      class="param-item__control"
                      :options="printContentOptions"
                    />
                    <a-tooltip
                      :title="HELP.printContent"
                      placement="bottom"
                    >
                      <QuestionCircleOutlined class="param-help-icon" />
                    </a-tooltip>
                  </div>
                </div>

                <!-- 行 2：单据打印小数位数（总开关）+ 数量 / 单价 -->
                <div class="param-row param-row--wrap">
                  <div class="param-item">
                    <a-checkbox v-model:checked="form.decimalEnabled">
                      单据打印小数位数
                    </a-checkbox>
                    <a-tooltip
                      :title="HELP.decimalEnabled"
                      placement="bottom"
                    >
                      <QuestionCircleOutlined class="param-help-icon" />
                    </a-tooltip>
                  </div>

                  <div class="param-item">
                    <span class="param-item__label">数量</span>
                    <a-input-number
                      v-model:value="form.qtyDecimal"
                      class="param-item__number"
                      :min="decimalMin"
                      :max="decimalMax"
                      :precision="0"
                      :disabled="!form.decimalEnabled"
                    />
                    <span class="param-item__suffix">位小数</span>
                  </div>

                  <div class="param-item">
                    <span class="param-item__label">单价</span>
                    <a-input-number
                      v-model:value="form.priceDecimal"
                      class="param-item__number"
                      :min="decimalMin"
                      :max="decimalMax"
                      :precision="0"
                      :disabled="!form.decimalEnabled"
                    />
                    <span class="param-item__suffix">位小数</span>
                  </div>
                </div>

                <!-- 生效范围说明：逐项写清「已生效 / 未生效」，不夸大也不遗漏 -->
                <div class="param-tip">
                  生效范围（打印组件 <code>components/PrintDialog</code> 实时读取本页配置）：
                  「允许打印草稿」= 草稿单据的打印门控；「单据打印小数位数」= 打印时对数量列按数量位数、
                  单价列按单价位数的定长格式化（金额列不受影响，与对标口径一致）；「打印内容」= 明细行
                  「批次效期」文本的拼接口径（批号／生产日期／到期日期 + 数量）。
                  <br>
                  尚未生效：「属性商品汇总打印」「批次效期商品汇总打印」—— 本系统打印引擎逐行直出、
                  没有「按商品/批次汇总成一行」的能力，未硬造，仅保存配置。
                  <br>
                  「打印内容」的 3 个候选值与小数位档位均已在比对站实测采集（2026-09-18）；
                  本系统小数位只提供 0~4 位，因主数据精度为 numeric(18,4)（对标为「整数~8位小数」9 档）。
                  <br>
                  「打印内容」的作用方式：打印时按所选口径为每条明细行派生 <code>batchEffectiveText</code>
                  字段（如 <code>P20240101 2024-01-01~2025-01-01 *2.00</code>），
                  模板中放一个字段组件引用 <code>batchEffectiveText</code> 即可按此输出
                  （未引用该字段的模板不受影响）。
                </div>
              </div>
            </div>

            <!-- ═══ 卡片 2：助手打印（开关 + 逐字说明 + 下载链接） ═══ -->
            <div class="param-card">
              <div class="param-card__head">
                <div class="param-card__title">助手打印</div>
                <a-switch
                  v-model:checked="form.assistantEnabled"
                  checked-children="开"
                  un-checked-children="关"
                />
              </div>
              <div class="param-card__body">
                <div class="param-help">说明及操作步骤：</div>
                <div class="param-help">1、开启助手打印，将跳过预览直接打印。</div>
                <div class="param-help">
                  2、请
                  <!-- 下载地址由后端下发（GET /set/print-config/assistant），不得硬编码；未配置时降级提示 -->
                  <a
                    v-if="assistant.enabled"
                    :href="assistant.downloadUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                  >点击此处</a>
                  <span
                    v-else
                    class="param-help__muted"
                  >点击此处</span>
                  下载并安装打印助手
                  <span
                    v-if="!assistant.enabled"
                    class="param-help__muted"
                  >（管理员尚未配置下载地址，请联系管理员获取安装包）</span>
                  <span
                    v-else-if="assistant.version"
                    class="param-help__muted"
                  >（版本 {{ assistant.version }}）</span>
                </div>
                <div class="param-tip">
                  本系统的打印助手下载地址由服务端下发（配置项 set.print.assistant-download-url），未配置时按上文降级。
                  该开关已生效：开启后，打印弹窗中直接点「打印」会先自动渲染模板再打印，不再要求先点「预览」；
                  关闭后恢复「必须先预览」的原有流程（配置读不到时也按关闭处理，不影响打印可用性）。
                </div>
              </div>
            </div>

            <!-- ═══ 卡片 3：远程打印（开关 + 4 步说明 + 下载地址） ═══ -->
            <div class="param-card">
              <div class="param-card__head">
                <div class="param-card__title">远程打印</div>
                <a-switch
                  v-model:checked="form.remoteEnabled"
                  checked-children="开"
                  un-checked-children="关"
                />
              </div>
              <div class="param-card__body">
                <div class="param-help">操作步骤及注意事项：</div>
                <div class="param-help">
                  1、在作为远程点的计算机上下载并安装打印助手。【
                  <a
                    v-if="assistant.enabled"
                    :href="assistant.downloadUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                  >下载地址</a>
                  <span
                    v-else
                    class="param-help__muted"
                  >下载地址</span>
                  】。
                </div>
                <div class="param-help">2、安装并运行打印助手，如果已运行可以双击屏幕右下方任务栏图标，弹出打印助手登录框。</div>
                <div class="param-help">3、输入的用户名及密码，选择正确的打印机作为远程打印机，同时填写远程打印点名称</div>
                <div class="param-help">4、操作员在打印的时候根据远程点位名称选择想要使用的远程点位。</div>
                <div class="param-tip">
                  远程打印的执行链路由既有「打印链路」页维护（sys_print_chain / sys_print_chain_item，多级步骤），
                  打印客户端注册走既有 /v2/print/clients；本页只提供开关，不重复实现链路编排与客户端注册。
                  该开关已生效：关闭时打印弹窗的「远程打印」模式不可选；开启且选择远程打印时，
                  打印弹窗会按本页 pageCode 取「已启用」的打印链路并提交打印任务（/v2/print/tasks/by-chain）。
                  提交前会检查是否已注册打印客户端 —— 当前库中尚无链路与客户端数据，
                  此时弹窗明确提示原因且不提交任务（不留永远不被执行的 pending 任务）。
                </div>
              </div>
            </div>

            <!-- ═══ 底部：唯一一个「保存」（对标实拍：页面底部居中） ═══ -->
            <div class="param-footer">
              <a-space
                direction="vertical"
                :size="4"
              >
                <a-button
                  v-permission="'set:print-config:update'"
                  type="primary"
                  :loading="saving"
                  data-testid="print-config-save"
                  @click="handleSave"
                >
                  保存
                </a-button>
                <span
                  v-if="dirty"
                  class="param-footer__hint"
                >
                  有未保存的改动
                </span>
              </a-space>
            </div>
          </a-spin>
        </template>

        <!-- ═══ 模板类目视图（箱号/物流/条码/套餐条码/批次条码/货位码）═══ -->
        <div
          v-else
          class="param-card"
          :data-template-view="currentView"
        >
          <div class="param-card__head">
            <div class="param-card__title">{{ currentViewLabel }}</div>
          </div>
          <div class="param-card__body">
            <div class="param-help">
              ql361 的这 6 个类目点开后是「内嵌 iframe 模板设计器」（画布 + 宽/高/辅助线/数据代码等，
              billtype 分别为 9999/9998/9997/10002/10058，箱号模板另走 toolbox/printdesign）。
            </div>
            <div class="param-tip">
              本系统没有按类目（billtype）维度的模板载体，因此不提供跳转（如实说明，不造假入口）：
              ① 模板表 sys_print_template / erp_print_template 只有 <code>page_code</code> 维度，
                 没有 billtype/类目列（已核对 information_schema）；
              ② 两张表当前均为 0 行，无任何「箱号/物流/条码/套餐条码/批次条码/货位码」模板数据；
              ③ 既有「打印模板」「打印链路」「打印客户端」「打印任务」「模板设计器」页面在
                 sys_menu 中没有菜单项（开发文档 §6.3-P0）→ 正常运行时这些路由未注册，跳过去只会 404。
              待裁定：是否为这 6 个类目引入 billtype 维度的模板表并补菜单项。
            </div>
          </div>
        </div>
      </SettingsLayout>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 打印设置（设置 → 打印管理 → 打印设置，菜单 80930）
 *
 * 后端：`PrintConfigController`（`cn.aiedge.erp.printing`，前缀 `/api/set/print-config`）
 * 存储：`set_print_config`（迁移 V11.402.0，一行一租户，tenant_id 由服务端从会话取）
 * 对标：ql361「设置 → 系统配置 → 打印设置」实测 2026-09-18（JSON + PNG）
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SettingsLayout from '@/components/SettingsLayout/SettingsLayout.vue'
import type { SettingsLayoutItem } from '@/components/SettingsLayout/SettingsLayout.vue'
import {
  printConfigApi,
  type PrintConfig,
  type PrintContentOption,
  type PrintAssistantInfo,
} from '@/api/set/print-config'
// 保存成功后作废打印组件的配置缓存，让改动**立刻**作用于打印弹窗（无需等缓存过期）
import { invalidatePrintBehaviorCache } from '@/components/PrintDialog/printBehavior'

defineOptions({ name: 'SetPrintConfig' })

/** 「打印设置」视图的 key（默认选中） */
const PRINT_VIEW = 'print'

/**
 * 左列纵向标签（7 项，逐字取自 ql361 实拍）：
 * 页签「打印设置」+ 6 个模板类目。
 * 说明：任务口径中的「左列 6 个标签」指 6 个**模板类目**；对标实拍左列共渲染 7 行
 * （「打印设置」为当前页签且默认选中），两者不矛盾，见开发文档 §4.1 / §8.1-15。
 */
const navItems: SettingsLayoutItem[] = [
  { key: PRINT_VIEW, label: '打印设置' },
  { key: 'box-template', label: '箱号模板' },
  { key: 'logistics-template', label: '物流模板' },
  { key: 'barcode-template', label: '条码模板' },
  { key: 'combo-barcode', label: '套餐条码' },
  { key: 'batch-barcode', label: '批次条码' },
  { key: 'location-code', label: '货位码' },
]

/**
 * `?` 帮助气泡文案。
 * ⚠️ ql361 的气泡正文**未实测**（开发文档 §12-P1）→ 这里写的是**本系统的行为说明**，
 * 不是对标原文（不编造对标文案）；已生效的项按真实生效范围描述。
 */
const HELP = {
  allowDraftPrint: '关闭后，草稿状态的单据在打印弹窗中不允许打印并给出提示。',
  attrSummaryPrint: '开启后，含规格属性的商品在打印明细中按商品汇总为一行。（本系统打印引擎暂无汇总行能力，仅保存配置）',
  batchSummaryPrint: '开启后，按批次/效期管理的商品在打印明细中汇总为一行。（本系统打印引擎暂无汇总行能力，仅保存配置）',
  printContent: '打印明细行「批次效期」文本的拼接口径，取值由服务端下发（已实测 3 项）。',
  decimalEnabled: '开启后，单据打印时数量列/单价列按下面设置的小数位输出（金额列不受影响）；关闭则沿用原有精度。',
} as const

const currentView = ref<string>(PRINT_VIEW)
const loading = ref(false)
const saving = ref(false)

/** 表单（本地编辑态；保存成功后由 load() 用库内值覆盖） */
const form = reactive<Omit<PrintConfig, 'id' | 'tenantId' | 'updateTime' | 'options'>>({
  allowDraftPrint: true,
  attrSummaryPrint: true,
  batchSummaryPrint: false,
  printContent: '',
  decimalEnabled: false,
  qtyDecimal: 2,
  priceDecimal: 2,
  assistantEnabled: true,
  remoteEnabled: true,
})

/** 回读基线（序列化后的表单）：用于「有未保存改动」的判定 */
const baseline = ref('')

/** 取值域（后端下发：打印内容候选值 + 小数位上下界），前端不写死 */
const printContentOptions = ref<PrintContentOption[]>([])
const decimalMin = ref(0)
const decimalMax = ref(4)

/** 打印助手下载信息（地址由服务端下发；未配置时降级提示） */
const assistant = ref<PrintAssistantInfo>({ downloadUrl: '', version: '', enabled: false })

const currentViewLabel = computed(
  () => navItems.find(item => item.key === currentView.value)?.label || '',
)

/** 是否有未保存的改动 */
const dirty = computed(() => !!baseline.value && JSON.stringify(form) !== baseline.value)

function handleError(error: Error) {
  console.error('[打印设置] 页面错误', error)
}

/** 读取配置（保存后同样调用它回读：只有库内值才算保存成功） */
async function load() {
  loading.value = true
  try {
    const res = await printConfigApi.get()
    form.allowDraftPrint = res.allowDraftPrint === true
    form.attrSummaryPrint = res.attrSummaryPrint === true
    form.batchSummaryPrint = res.batchSummaryPrint === true
    form.printContent = res.printContent ?? ''
    form.decimalEnabled = res.decimalEnabled === true
    form.qtyDecimal = res.qtyDecimal ?? 2
    form.priceDecimal = res.priceDecimal ?? 2
    form.assistantEnabled = res.assistantEnabled === true
    form.remoteEnabled = res.remoteEnabled === true

    printContentOptions.value = res.options?.printContent || []
    decimalMin.value = res.options?.decimalMin ?? 0
    decimalMax.value = res.options?.decimalMax ?? 4

    baseline.value = JSON.stringify(form)
  } catch (e) {
    // 不用 e.message：axios 的错误文案是英文（如 Request failed with status code 500）
    console.error('[打印设置] 配置加载失败', e)
    message.error('打印设置加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/** 读取打印助手下载信息（未配置/失败都按「不可用」降级，不展示空链接） */
async function loadAssistant() {
  try {
    const res = await printConfigApi.getAssistant()
    assistant.value = {
      downloadUrl: res?.downloadUrl || '',
      version: res?.version || '',
      enabled: res?.enabled === true && !!res?.downloadUrl,
    }
  } catch (e) {
    console.warn('[打印设置] 打印助手信息加载失败（降级为「联系管理员」）', e)
    assistant.value = { downloadUrl: '', version: '', enabled: false }
  }
}

/**
 * 保存：先做本地校验（错误文案可读），提交后 **回读**再提示成功。
 * 后端同样校验（非法值返回 400），前端校验只是为了让提示更具体。
 */
async function handleSave() {
  const content = (form.printContent || '').trim()
  if (!content) {
    message.error('请选择打印内容')
    return
  }
  if (!inDecimalRange(form.qtyDecimal) || !inDecimalRange(form.priceDecimal)) {
    message.error(`数量/单价小数位需在 ${decimalMin.value}~${decimalMax.value} 之间`)
    return
  }

  saving.value = true
  try {
    await printConfigApi.update({
      allowDraftPrint: form.allowDraftPrint,
      attrSummaryPrint: form.attrSummaryPrint,
      batchSummaryPrint: form.batchSummaryPrint,
      printContent: content,
      decimalEnabled: form.decimalEnabled,
      qtyDecimal: form.qtyDecimal,
      priceDecimal: form.priceDecimal,
      assistantEnabled: form.assistantEnabled,
      remoteEnabled: form.remoteEnabled,
    })
    // 让打印组件下次打开弹窗时重新读取（否则最长 5 分钟才生效）
    invalidatePrintBehaviorCache()
    // 回读：以库内值为准（避免「保存成功但没存下」的假保存）
    await load()
    message.success('保存成功')
  } catch (e) {
    // 校验类错误（400）已在提交前本地拦截；这里的失败一律给可读中文提示（不静默）
    console.error('[打印设置] 保存失败', e)
    message.error('保存失败，请检查填写内容或稍后重试')
  } finally {
    saving.value = false
  }
}

function inDecimalRange(value: number | undefined): boolean {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return false
  return Number(value) >= decimalMin.value && Number(value) <= decimalMax.value
}

/**
 * 切标签的脏检查（**非阻塞提示**）。
 * ql361 的该交互未实测（开发文档 §5.2 / §12-P1）→ 按同组「系统参数」页的既有做法提示，不拦截切换。
 */
function onViewChange(next: string) {
  if (next === PRINT_VIEW) return
  if (dirty.value) {
    message.warning('打印设置有未保存的改动，切换后仍可在保存前返回继续修改')
  }
}

onMounted(async () => {
  await Promise.all([load(), loadAssistant()])
})
</script>

<style scoped>
/* ── 右栏卡片（与「系统参数」页同一视觉口径） ── */
.param-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 12px;
}

.param-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 0 16px;
}

.param-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.param-card__body {
  padding: 4px 16px 12px;
  border-top: 1px solid #f5f5f5;
}

/* 配置行：横向排列，窄屏自动换行（本页字段少，按对标把首行 4 项并排） */
.param-row {
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 8px 0;
}

.param-row--wrap {
  flex-wrap: wrap;
  row-gap: 12px;
}

.param-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #262626;
}

.param-item__label {
  color: #595959;
}

.param-item__control {
  width: 180px;
}

.param-item__number {
  width: 90px;
}

.param-item__suffix {
  color: #8c8c8c;
}

.param-help-icon {
  color: #8c8c8c;
  cursor: help;
  font-size: 13px;
}

/* 逐字对标的多行说明文案 */
.param-help {
  padding: 2px 0;
  font-size: 13px;
  color: #595959;
  line-height: 1.9;
}

.param-help__muted {
  color: #bfbfbf;
}

/* 灰色小字：取值域缺口与生效范围 */
.param-tip {
  margin-top: 8px;
  padding: 6px 10px;
  background: #fafafa;
  border-radius: 4px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.7;
}

/* ── 底部：全页级唯一「保存」 ── */
.param-footer {
  position: sticky;
  bottom: 0;
  padding: 12px 0 16px;
  text-align: center;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.6), #fff 40%);
}

.param-footer__hint {
  font-size: 12px;
  color: #faad14;
}
</style>
