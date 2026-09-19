<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        页面定位：设置 → 系统配置 → 系统参数（菜单 80621 / set:sys-params，单入口）
        对标页面：ql361「设置 → 系统配置 → 系统参数」（左列纵向 8 标签 + 右栏分组卡片 + 底部统一保存）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/设置模块/系统参数开发文档.md（§8.1 为实测规格）

        结构（路线 A′ · 配置页形态）：
          ErrorBoundary > PageContainer(full-height) > SettingsLayout（通用左列纵向标签组件）
            └─ 右栏：每张「配置卡片」= 配置名 + `?` 帮助气泡 + 右侧控件（开关/下拉）+ 可展开子项 + 灰色温馨提示
            └─ 底部：唯一一个「保存」（保存当前视图的改动 → 重新 GET 回读）

        数据口径：左列 8 个视图来自 GET /config/nav-groups（**不写死**）；配置项来自
          GET /config/list（真实读 sys_config）；枚举/多段的候选值来自 GET /config/value-options；
          保存走 POST /config/batch-save（真实落库）。

        2026-09-18 收口（迁移 V11.421.0）：ql361 8 个标签的配置项已全部采集并 seed 进 sys_config，
        于是：
          · 8 个视图都有真实配置项 → 页面**纯数据驱动**（nav_group 归标签、parent_key 归层级、
            value_type 决定控件、选项集决定下拉候选值），新增配置项**不需要改前端代码**；
          · 「消息提醒」是三层的（通知事件 → 通知对象 → 通知方式）→ 子项/孙项统一交给
            `ParamNodeRow` 递归渲染（层深不写死）；
          · `?` 气泡与灰色温馨提示都来自库里的 help_text / tip_text（help_text 为空时回退
            remark，不编造 ql361 原文）；
          · `locked`（被商品引用后不能改）由后端按商品档案的真实引用计数实时算出，
            锁定项渲染为禁用 + 悬浮说明「为什么不能改」。

        ⚠️ 如实登记的剩余缺口（不编造）：
          1. 报告「未取到清单」里的项没有补：勾选状态未取到的行按未勾选存放并在库里的 remark
             标注（流程启用·自提 / 拒收数量参与补单、单据设置·多仓库成本调价 / 启用销售税率 /
             启用商品行属性管理 / 启用采购税率、库存设置·账面库存允许为负、消息提醒·货位预警
             补货通知的系统通知）；「消息提醒」各行的收件人文本、以及「短信通知」那条归属不明
             的气泡正文都没有落库。
          2. ql361 的**小节标题 / 卡片折叠行为 / 「设置」二级弹层 / 「去设置」跳转**没有建模：
             本页把配置项按 sort_order 平铺成卡片（卡片体是否展示由父开关决定）。
          3. 无 `PageConfigPanel` / 无表头齿轮列配置 / 无分页 —— 本形态（卡片配置台）对标实测
             就没有这三样（文档 §8.1-12）。
      -->
      <SettingsLayout
        v-model="currentView"
        :items="navItems"
        title="系统参数"
      >
        <a-spin :spinning="loading">
          <!-- 空视图：如实提示缺口，不编造 -->
          <a-empty
            v-if="!currentCards.length"
            class="param-empty"
            :description="emptyDescription"
          />

          <div
            v-for="card in currentCards"
            :key="card.key"
            class="param-card"
            :data-param-card="card.key"
          >
            <!-- 卡片头：配置名 + ? 帮助气泡 + 锁定/停用标记 + 右侧控件（布尔型） -->
            <div class="param-card__head">
              <div class="param-card__title">
                <span class="param-card__name">{{ card.name }}</span>
                <a-tooltip
                  v-if="card.help"
                  :title="card.help"
                  placement="bottom"
                >
                  <QuestionCircleOutlined
                    class="param-card__help"
                    data-testid="param-help"
                  />
                </a-tooltip>
                <a-tag
                  v-if="card.locked"
                  color="default"
                  class="param-card__tag"
                  data-testid="param-locked"
                >
                  已锁定
                </a-tag>
                <a-tag
                  v-if="card.enabled === false"
                  color="default"
                  class="param-card__tag"
                >
                  已停用
                </a-tag>
              </div>

              <div class="param-card__control">
                <!-- 卡片自身的开关（如「批次、批号管理」「商品规格属性」「序列号管理」）。
                     `group` 型卡片是分组节点（如「消息提醒」的通知事件），没有自身控件。 -->
                <a-tooltip
                  v-if="card.valueType === 'boolean' && card.locked"
                  :title="card.lockedReason || '该配置已被商品引用，不能更改'"
                  placement="bottom"
                >
                  <a-switch
                    :checked="isOn(card.key)"
                    disabled
                    :data-param-key="card.key"
                  />
                </a-tooltip>
                <a-switch
                  v-else-if="card.valueType === 'boolean'"
                  :checked="isOn(card.key)"
                  :data-param-key="card.key"
                  @change="(v: any) => setBool(card.key, v)"
                />
              </div>
            </div>

            <!-- 卡片体：非布尔项的自身控件 + 子项 + 温馨提示 -->
            <div
              v-if="card.valueType !== 'boolean' || card.children.length || card.tip"
              class="param-card__body"
            >
              <!-- 非布尔项、非分组项的自身控件（下拉 / 输入框 / 多段下拉） -->
              <div
                v-if="card.valueType !== 'boolean' && card.valueType !== 'group'"
                class="param-row"
              >
                <span class="param-row__label" />
                <span class="param-row__control">
                  <ParamControl
                    :item-key="card.key"
                    :value-type="card.valueType"
                    :value="valueOf(card.key)"
                    :options="optionsFor(card.key)"
                    :segment-options="segmentOptionsFor(card.key)"
                    :disabled="card.locked"
                    @update="(v: string) => setValue(card.key, v)"
                  />
                </span>
              </div>

              <!-- 子项（布尔型父开关打开时才展开，ql361：批次、批号管理 → 保质期管理 / 批号管理 /
                   批号启用大小写）；「消息提醒」还有第三层（通知对象 → 通知方式），
                   由 ParamNodeRow 递归渲染。 -->
              <template v-if="card.children.length && isExpanded(card)">
                <ParamNodeRow
                  v-for="child in card.children"
                  :key="child.key"
                  :node="child"
                  :values="editValues"
                  :enum-options="enumOptionsMap"
                  :segment-options="segmentOptionsMap"
                  @update="setValue"
                />
              </template>

              <!-- 卡片自身的温馨提示（灰色小字，ql361 逐字文案） -->
              <div
                v-if="card.tip"
                class="param-tip"
              >
                温馨提示：{{ card.tip }}
              </div>
            </div>
          </div>

          <!-- 底部：当前视图的统一「保存」（对标 ql361 的页面底部唯一保存按钮） -->
          <div class="param-footer">
            <a-space
              direction="vertical"
              :size="4"
            >
              <a-button
                type="primary"
                :loading="saving"
                data-testid="param-save"
                @click="handleSave"
              >
                保存
              </a-button>
              <span
                v-if="dirtyKeys.length"
                class="param-footer__hint"
              >
                当前共 {{ dirtyKeys.length }} 项改动待保存
              </span>
            </a-space>
          </div>
        </a-spin>
      </SettingsLayout>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 系统参数（设置 → 系统配置 → 系统参数，菜单 80621）
 *
 * 本次（2026-09-18）把原来的「查询区 + 裸表格 + 弹窗」整页重写为路线 A′（配置页形态），
 * 同时后端从「JVM 内存假实现」改为真实读写 `sys_config`（见 SystemConfigServiceImpl）。
 * 页面本身不落库细节：只调 /config/* 接口。
 */
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SettingsLayout from '@/components/SettingsLayout/SettingsLayout.vue'
import type { SettingsLayoutItem } from '@/components/SettingsLayout/SettingsLayout.vue'
import ParamControl from './ParamControl.vue'
import ParamNodeRow from './ParamNodeRow.vue'
import type { ParamNode } from './ParamNodeRow.vue'
import {
  configApi,
  type ConfigNavGroup,
  type ConfigValueOption,
  type ConfigValueOptionsPayload,
  type SystemConfig,
} from '@/api/config'

defineOptions({ name: 'SetSysParams' })

const loading = ref(false)
const saving = ref(false)
/** 左列 8 个视图（来自后端，不写死） */
const navGroups = ref<ConfigNavGroup[]>([])
/** 全部配置项（真实读 sys_config） */
const allItems = ref<SystemConfig[]>([])
/** 单选型配置项的候选值（配置键 → 扁平选项集） */
const enumOptionsMap = ref<Record<string, ConfigValueOption[]>>({})
/** 多段控件的分段候选值（配置键 → 按段的选项集） */
const segmentOptionsMap = ref<Record<string, ConfigValueOption[][]>>({})
/** 当前选中的视图 */
const currentView = ref('')
/** 本地编辑态：配置键 → 值（统一按字符串处理，与 sys_config.param_value（text）一致） */
const editValues = reactive<Record<string, string>>({})
/** 回读基线：用于计算「有改动」的键 */
const baseline = ref<Record<string, string>>({})

/** 后端未定义的视图编码兜底到「其他」（保证 DB 里的行不会因为视图编码不认识而在页面上消失） */
const FALLBACK_VIEW = 'other'

/** 把一行 sys_config 折算成视图节点（卡片 = 顶层项；children 由 parent_key 递归挂载） */
function toNode(item: SystemConfig): ParamNode {
  return {
    key: item.configKey,
    name: item.configName || item.configKey,
    valueType: item.valueType || 'string',
    // `?` 气泡：优先用帮助文案；未采集到帮助文案时回退展示配置项说明（不编造 ql361 原文）
    help: item.helpText || item.description || '',
    tip: item.tipText || '',
    locked: item.locked === true,
    // 锁定原因由后端按「商品是否引用该配置」实时算出（不落库）
    lockedReason: item.lockedReason || '',
    enabled: item.enabled !== false,
    children: [],
  }
}

/**
 * 把扁平配置项组装成树（**任意层深**）。
 *
 * 层深不是写死的：`parent_key` 指到哪一层就挂到哪一层 —— 「行业设置」是两层
 * （批次、批号管理 → 保质期管理…），「消息提醒」是三层
 * （通知事件 → 通知对象 → 通知方式）。顺序沿用后端 sort_order。
 */
function buildTree(items: SystemConfig[]): ParamNode[] {
  const keys = new Set(items.map(i => i.configKey))
  const childrenOf = new Map<string, SystemConfig[]>()
  for (const item of items) {
    if (item.parentKey && keys.has(item.parentKey)) {
      if (!childrenOf.has(item.parentKey)) childrenOf.set(item.parentKey, [])
      childrenOf.get(item.parentKey)!.push(item)
    }
  }
  const build = (item: SystemConfig): ParamNode => {
    const node = toNode(item)
    node.children = (childrenOf.get(item.configKey) || []).map(build)
    return node
  }
  // 顶层项 = 无父项，或父项不在本视图内（避免子项被「吃掉」而看不见）
  return items.filter(i => !i.parentKey || !keys.has(i.parentKey)).map(build)
}

/** 按视图把扁平配置项组装成「卡片 + 子项（+ 孙项）」 */
const cardsByView = computed<Record<string, ParamNode[]>>(() => {
  const known = new Set(navGroups.value.map(g => g.code))
  const grouped: Record<string, SystemConfig[]> = {}
  for (const item of allItems.value) {
    const code = item.navGroup && known.has(item.navGroup) ? item.navGroup : FALLBACK_VIEW
    if (!grouped[code]) grouped[code] = []
    grouped[code].push(item)
  }

  const result: Record<string, ParamNode[]> = {}
  for (const [code, items] of Object.entries(grouped)) {
    result[code] = buildTree(items)
  }
  return result
})

const navItems = computed<SettingsLayoutItem[]>(() => navGroups.value.map(g => ({
  key: g.code,
  label: g.name,
  badge: (cardsByView.value[g.code] || []).length,
})))

const currentCards = computed<ParamNode[]>(() => cardsByView.value[currentView.value] || [])

/**
 * 空态文案：8 个视图现在都有真实配置项（V11.421.0 seed），因此正常不会看到空态；
 * 万一某个视图真为空（例如被人工删光了），文案如实说明「未采集到/无配置项」，不编造。
 */
const emptyDescription = computed(() => (navGroups.value.length
  ? `「${currentViewLabel.value}」暂无配置项：ql361 未采集到该视图的配置项，或本视图的配置项已被删除；缺口如实保留（不编造）`
  : '暂无系统参数数据'))

const currentViewLabel = computed(() => navGroups.value.find(g => g.code === currentView.value)?.name || '')

/** 有改动的配置键 */
const dirtyKeys = computed(() => Object.keys(editValues).filter(k => editValues[k] !== baseline.value[k]))

function valueOf(key: string): string {
  return editValues[key] ?? ''
}

function isOn(key: string): boolean {
  return valueOf(key) === 'true'
}

function setValue(key: string, value: string) {
  editValues[key] = value
}

function setBool(key: string, checked: any) {
  editValues[key] = checked ? 'true' : 'false'
}

function optionsFor(key: string): ConfigValueOption[] {
  return enumOptionsMap.value[key] || []
}

function segmentOptionsFor(key: string): ConfigValueOption[][] {
  return segmentOptionsMap.value[key] || []
}

/**
 * 卡片是否展开（布尔型父开关打开才展开子项；非布尔型父项与 `group` 分组节点直接展开，
 * 避免子项被永久隐藏）
 */
function isExpanded(card: ParamNode): boolean {
  return card.valueType !== 'boolean' || isOn(card.key)
}

function handleError(err: unknown) {
  console.warn('[系统参数] ErrorBoundary:', err)
}

/** 拉取配置项与候选值字典（保存后重新调用 = 接口回读） */
async function loadItems() {
  loading.value = true
  try {
    const res: any = await configApi.getList({})
    const rows: SystemConfig[] = Array.isArray(res) ? res : (res?.records || [])
    allItems.value = rows

    // 用库内值重建编辑态与基线（保存后回读即以此为准）
    const base: Record<string, string> = {}
    Object.keys(editValues).forEach((k) => { delete editValues[k] })
    for (const row of rows) {
      const value = row.configValue ?? ''
      editValues[row.configKey] = value
      base[row.configKey] = value
    }
    baseline.value = base

    if (!currentView.value || !navGroups.value.some(g => g.code === currentView.value)) {
      currentView.value = navGroups.value[0]?.code || ''
    }
  } catch (e) {
    console.warn('[系统参数] 配置项加载失败', e)
    message.error('系统参数加载失败')
  } finally {
    loading.value = false
  }
}

/** 拉取左列视图与候选值字典 */
async function loadMeta() {
  try {
    const groups = await configApi.getNavGroups()
    navGroups.value = Array.isArray(groups) ? groups : []
  } catch (e) {
    console.warn('[系统参数] 左列视图加载失败', e)
    navGroups.value = []
  }
  try {
    const options = await configApi.getValueOptions()
    splitOptions(options)
  } catch (e) {
    console.warn('[系统参数] 候选值字典加载失败（下拉可能为空）', e)
    splitOptions(null)
  }
}

/**
 * 候选值字典按形态拆成两张表
 *
 * 后端返回 `配置键 → 扁平选项集 | 分段选项集`（分段形态的外层数组下标 = 段序号，
 * 见 `/config/value-options` 的接口注释）；这里按「首元素是不是数组」区分，
 * 让控件拿到的永远是对应形态的那一张表。**不要**按配置键硬编码。
 */
function splitOptions(payload: ConfigValueOptionsPayload | null) {
  const single: Record<string, ConfigValueOption[]> = {}
  const segs: Record<string, ConfigValueOption[][]> = {}
  for (const [key, value] of Object.entries(payload || {})) {
    if (Array.isArray(value) && Array.isArray(value[0])) {
      segs[key] = value as ConfigValueOption[][]
    } else {
      single[key] = (value as ConfigValueOption[]) || []
    }
  }
  enumOptionsMap.value = single
  segmentOptionsMap.value = segs
}

/**
 * 收集当前视图内「可写」的配置键（含子项/孙项）
 *
 * 排除两类：`locked = true` 的不可逆配置（被商品引用后不能改）、以及 `group` 分组节点
 * （分组行没有自己的控件，值不参与表单提交）。
 */
function collectEditableKeys(cards: ParamNode[]): string[] {
  const keys: string[] = []
  const walk = (list: ParamNode[]) => {
    for (const card of list) {
      if (!card.locked && card.valueType !== 'group') keys.push(card.key)
      if (card.children.length) walk(card.children)
    }
  }
  walk(cards)
  return keys
}

/**
 * 保存当前视图（对标 ql361：底部唯一一个「保存」提交当前标签页的表单），随后重新 GET 回读。
 *
 * 提交口径：当前视图内**全部可写项**的键值（不是只提交 diff）——与 ql361 的表单提交语义一致，
 * 且重复保存是幂等的（值相同则写回相同值）。`locked` 项不提交。
 */
async function handleSave() {
  const keys = collectEditableKeys(currentCards.value)
  if (!keys.length) {
    message.warning('当前视图没有可保存的配置项')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, string> = {}
    keys.forEach((k) => { payload[k] = editValues[k] ?? '' })
    await configApi.batchSave(payload)
    // 回读：以库内值为准刷新本地编辑态与基线（后端真落库，回读即验真）
    await loadItems()
    message.success('保存成功')
  } catch (e) {
    console.warn('[系统参数] 保存失败', e)
    message.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 切换视图时，把当前视图的未保存改动提示出来（不阻塞切换） */
watch(currentView, (_next, prev) => {
  if (!prev) return
  const dirtyInPrev = dirtyKeys.value.some(k => allItems.value.find(i => i.configKey === k)?.navGroup === prev)
  if (dirtyInPrev) {
    message.warning('上一个视图有未保存的改动，切换后可在保存前返回继续修改')
  }
})

onMounted(async () => {
  await loadMeta()
  await loadItems()
})
</script>

<style scoped>
/* ── 右栏：配置卡片 ── */
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
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.param-card__help {
  color: #8c8c8c;
  cursor: help;
  font-size: 13px;
}

.param-card__tag {
  margin-inline-start: 4px;
  font-weight: 400;
}

.param-card__control {
  flex-shrink: 0;
}

.param-card__body {
  padding: 4px 16px 12px;
  border-top: 1px solid #f5f5f5;
}

/* 配置行：左侧配置名 + 右侧控件（卡片自身的那一行；子项/孙项的行样式在 ParamNodeRow 里） */
.param-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
}

.param-row__label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #595959;
}

.param-row__control {
  flex-shrink: 0;
}

.param-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.param-list__join {
  color: #8c8c8c;
}

/* 灰色「温馨提示」（ql361 逐字文案） */
.param-tip {
  padding: 2px 0 4px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
}

.param-empty {
  margin-top: 64px;
}

/* ── 底部：全页级统一保存 ── */
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
