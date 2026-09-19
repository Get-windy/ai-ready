<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        系统重建（设置 → 账套操作 → 系统重建，菜单 70560 / set:rebuild）
        ⚠️ 本页是「危险操作页」（清数据），不是只读台账 —— 按对标 ql361 实测形态重做：
            红色不可恢复警告 + 2 列（选项/描述）固定 12 项复选矩阵 + 必输登录密码 + 「确定」。
        对标证据：docs/Yh-Spec/手动整理对标开发文档/设置模块/系统重建开发文档.md §2 / §4 / §8.1

        历史 P0（本次修复）：原实现给 BillTableList 传了组件不存在的 prop `apiUrl`/`params`，
        并调用了未暴露的 `reload()` → 表格恒空、查询/重置静默失效、后端零控制器 → 整页假死。
        本次改为 BillDetailTable（无分页、无查询区、无行操作），并接上真实后端。

        为什么「12 行固定矩阵，不走分页」：
          对标页的 12 项是**固定清单**（不是数据集），ql361 实测本页也没有分页控件；
          翻页对固定清单没有意义，反而会让「全选」语义含糊。故 :show-pagination="false"
          + :min-rows="12"（正好 12 行，不补空行）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 顶部：不可恢复的红色警告 + 提示条（对标逐字文案） ═══ -->
        <template #search-fields>
          <div class="rebuild-head">
            <a-alert
              type="error"
              show-icon
              class="rebuild-alert"
              :message="warningText"
            />
            <div class="rebuild-tip">
              {{ tipText }}
            </div>
          </div>
        </template>

        <!-- ═══ 数据表：12 项固定复选矩阵（2 列：选项 / 描述） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :columns="columns"
              :data-source="optionRows"
              :loading="loading"
              :view-mode="true"
              :min-rows="12"
              :show-pagination="false"
              row-key="key"
              storage-key="set-rebuild-options-columns"
            >
              <!-- 选项列：复选框 + 选项名（复选框放在本列内，保证表头恰为「选项 / 描述」2 列） -->
              <template #optionCell="{ record }">
                <a-checkbox
                  :checked="isChecked(record.key)"
                  :disabled="isDisabled(record.key)"
                  @change="(e) => onRowCheck(record.key, e)"
                >
                  <span class="option-name">{{ record.name }}</span>
                </a-checkbox>
                <a-tooltip
                  v-if="isDisabled(record.key)"
                  placement="bottom"
                  :title="disabledTip(record.key)"
                >
                  <span class="option-flag">已包含</span>
                </a-tooltip>
              </template>

              <!-- 描述列：对标逐字描述 + 影响行数预估（dry-run）+ 缺口说明 -->
              <template #descriptionCell="{ record }">
                <span>{{ record.description }}</span>
                <span
                  v-if="record.estimatedRows >= 0"
                  class="estimate"
                >（预计影响 {{ record.estimatedRows }} 行）</span>
                <span
                  v-if="record.note"
                  class="row-note"
                  :title="record.note"
                >说明</span>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：全选 + 已选计数 + 确定（对标唯一动作按钮） ═══ -->
        <template #table-footer>
          <div class="rebuild-footer">
            <a-checkbox
              :checked="allChecked"
              :indeterminate="indeterminate"
              @change="onCheckAll"
            >
              全选
            </a-checkbox>
            <span class="selected-count">已选 {{ selectedKeys.length }} / {{ optionRows.length }} 项</span>
            <span class="footer-gap" />
            <a-button
              v-permission.disabled="'set:rebuild:execute'"
              type="primary"
              danger
              :loading="executing"
              @click="openPasswordModal"
            >
              确定
            </a-button>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 密码验证弹窗（对标：为确保安全，请输入登录密码 *） ═══ -->
    <a-modal
      v-model:open="passwordModalOpen"
      title="系统重建 - 安全验证"
      :width="560"
      :confirm-loading="executing"
      ok-text="确定"
      cancel-text="取消"
      @ok="handleConfirm"
    >
      <a-alert
        type="error"
        show-icon
        class="modal-alert"
        :message="warningText"
      />
      <div class="modal-block">
        <div class="modal-label">
          将清除以下范围（共 {{ selectedKeys.length }} 项，预计 {{ selectedTotalRows }} 行）：
        </div>
        <ul class="scope-list">
          <li
            v-for="row in selectedRows"
            :key="row.key"
          >
            {{ row.name }}
            <span class="scope-desc">{{ row.description }}</span>
          </li>
        </ul>
      </div>
      <a-form
        layout="vertical"
        :colon="false"
      >
        <a-form-item
          label="为确保安全，请输入登录密码"
          required
        >
          <a-input-password
            v-model:value="password"
            placeholder="请输入当前登录账号的登录密码"
            autocomplete="new-password"
            @press-enter="handleConfirm"
          />
        </a-form-item>
      </a-form>
      <div class="modal-note">
        密码仅用于本次服务端二次校验，不会被保存、不会写入日志。
      </div>
    </a-modal>

    <!-- ═══ 执行结果弹窗（每个选项清了什么、清了 N 行） ═══ -->
    <a-modal
      v-model:open="resultVisible"
      title="系统重建结果"
      :width="640"
      :footer="null"
    >
      <a-alert
        type="success"
        show-icon
        class="modal-alert"
        :message="`系统重建已完成，合计清理 / 归零 ${resultTotal} 行`"
      />
      <!-- 结果清单用自定义列表渲染（不用 a-table 的 bodyCell：空分支会让非插槽列渲染为空） -->
      <div class="result-list">
        <div
          v-for="row in resultRows"
          :key="row.key"
          class="result-row"
        >
          <span class="result-name">{{ row.name }}</span>
          <span
            class="result-rows"
            :class="{ 'result-zero': !row.clearedRows }"
          >{{ row.clearedRows }} 行</span>
          <span class="result-detail">{{ formatTables(row.tables) }}</span>
        </div>
        <div
          v-if="resultRows.length === 0"
          class="result-detail"
        >
          本次没有清理任何行。
        </div>
      </div>
      <div class="modal-tip">
        {{ resultTip }}
      </div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { rebuildApi, type RebuildOption, type RebuildResultRow } from '@/api/set/rebuild'

defineOptions({ name: 'SetRebuild' })

/**
 * 对标页面卡片文案（后端 /options 也会下发同值；接口失败时用这里的兜底，保证警告永不消失）
 * 逐字取自 ql361 实测「设置 → 账套操作 → 系统重建」。
 */
const DEFAULT_WARNING = '系统重建将清除 所有单据、订单 和以下选项数据且 不能恢复！,请谨慎!'
const DEFAULT_TIP = '注意： 若需单独备份数据，请联系客服！系统重建后请点击右上角的"刷新"或者重新登录后进行系统开账！'

// ═══ 状态 ═══
const loading = ref(false)
const executing = ref(false)
const optionRows = ref<RebuildOption[]>([])
const warningText = ref(DEFAULT_WARNING)
const tipText = ref(DEFAULT_TIP)
/** 选项间包含关系：勾 key（父）则自动勾选并禁用 value（子）—— 由后端注册表下发 */
const implies = ref<Record<string, string>>({})
/** 已勾选的范围键（唯一提交来源） */
const selectedKeys = ref<string[]>([])

const passwordModalOpen = ref(false)
const password = ref('')
const resultVisible = ref(false)
const resultRows = ref<RebuildResultRow[]>([])
const resultTotal = ref(0)
const resultTip = ref('')

// ═══ 表格列：严格 2 列（选项 / 描述）—— 与对标列头一致 ═══
// 复选框放在「选项」列内（slot），因此表头不出现第 3 列；
// 「全选」按任务要求放在表格底部工具条（含半选态）。
const columns: DetailColumnConfig[] = [
  { key: 'option', title: '选项', type: 'slot', slotName: 'optionCell', width: 240, fixed: 'left' },
  { key: 'description', title: '描述', type: 'slot', slotName: 'descriptionCell', width: 480 },
]

// 清理结果只在结果弹窗里展示（用自定义列表），不污染主表格的 2 列口径

// ═══ 勾选 ═══
function isChecked(key: string): boolean {
  return selectedKeys.value.includes(key)
}

/** 被父项包含的子项：父项勾选后自动勾上，且不可单独取消（对标未说明、本系统显式处理的隐含依赖） */
function isDisabled(key: string): boolean {
  for (const child of Object.values(implies.value)) {
    if (child === key) {
      const parent = Object.keys(implies.value).find(k => implies.value[k] === key)
      if (parent && isChecked(parent)) return true
    }
  }
  return false
}

function disabledTip(key: string): string {
  const parent = Object.keys(implies.value).find(k => implies.value[k] === key)
  const parentName = optionRows.value.find(o => o.key === parent)?.name || parent || ''
  return `已由「${parentName}」包含，无需单独勾选`
}

function onRowCheck(key: string, e: any) {
  if (isDisabled(key)) return
  const checked = !!e?.target?.checked
  const next = new Set(selectedKeys.value)
  if (checked) {
    next.add(key)
    // 父项勾选 → 连带勾选被包含项（后端口径同样包含，此处只是让用户看得见）
    const child = implies.value[key]
    if (child) next.add(child)
  } else {
    next.delete(key)
    const child = implies.value[key]
    if (child) next.delete(child)
  }
  selectedKeys.value = [...next]
}

function onCheckAll(e: any) {
  selectedKeys.value = e?.target?.checked ? optionRows.value.map(o => o.key) : []
}

const allChecked = computed(() => optionRows.value.length > 0 && selectedKeys.value.length === optionRows.value.length)
const indeterminate = computed(() => selectedKeys.value.length > 0 && !allChecked.value)

const selectedRows = computed(() => optionRows.value.filter(o => selectedKeys.value.includes(o.key)))
const selectedTotalRows = computed(() =>
  selectedRows.value.reduce((sum, o) => sum + (o.estimatedRows > 0 ? o.estimatedRows : 0), 0),
)

// ═══ 加载范围清单（含影响行数预估） ═══
async function loadOptions() {
  loading.value = true
  try {
    const res = await rebuildApi.options()
    optionRows.value = res?.options || []
    if (res?.warning) warningText.value = res.warning
    if (res?.tip) tipText.value = res.tip
    implies.value = res?.implies || {}
    // 清理已失效的勾选（范围内不再存在的键，避免提交出「未知范围」）
    const valid = new Set(optionRows.value.map(o => o.key))
    selectedKeys.value = selectedKeys.value.filter(k => valid.has(k))
  } catch (error: any) {
    console.error('[系统重建] 清除范围加载失败', error)
    message.error(error?.message || '清除范围加载失败')
    optionRows.value = []
  } finally {
    loading.value = false
  }
}

// ═══ 提交链路：确定 → 密码弹窗 → 二次确认 → 执行 ═══
function openPasswordModal() {
  // 前置校验 1：至少勾 1 项（对标「确定」的前置校验；此步不发任何请求）
  if (selectedKeys.value.length === 0) {
    message.warning('请至少选择一项要清除的数据范围')
    return
  }
  password.value = ''
  passwordModalOpen.value = true
}

function handleConfirm() {
  // 前置校验 2：密码非空（此步不发任何请求）
  if (!password.value) {
    message.warning('为确保安全，请输入登录密码')
    return
  }
  // 二次确认：逐字复述不可恢复警告 + 列出将清除的范围（危险操作的显式确认）
  Modal.confirm({
    title: '确认执行系统重建？',
    width: 520,
    okText: '确认执行',
    okType: 'danger',
    cancelText: '取消',
    content: h('div', { style: 'line-height:1.7' }, [
      h('div', { style: 'color:#cf1322;font-weight:600;margin-bottom:8px' }, warningText.value),
      h('div', `将清除 ${selectedRows.value.length} 项：${selectedRows.value.map(r => r.name).join('、')}`),
      h('div', { style: 'color:#8c8c8c;margin-top:6px' }, '此操作不可恢复，且需要重新登录后进行系统开账。'),
    ]),
    onOk: doExecute,
  })
}

async function doExecute() {
  executing.value = true
  try {
    const res = await rebuildApi.execute({ options: selectedKeys.value, password: password.value })
    // 提交后立刻丢弃明文密码（不留在内存/表单里）
    password.value = ''
    passwordModalOpen.value = false
    resultRows.value = res?.results || []
    resultTotal.value = Number(res?.totalClearedRows) || 0
    resultTip.value = res?.tip || DEFAULT_TIP
    resultVisible.value = true
    message.success(`系统重建完成，合计清理 ${resultTotal.value} 行`)
    // 数据已变 → 重新预估（未勾选范围的行数也会随之变化）
    selectedKeys.value = []
    await loadOptions()
  } catch (error: any) {
    // 后端文案优先（如「登录密码错误，请重新输入」「请至少选择一项…」）
    message.error(error?.message || '系统重建执行失败')
  } finally {
    executing.value = false
  }
}

function formatTables(tables?: { table: string; mode: string; rows: number }[]): string {
  if (!tables || tables.length === 0) return '-'
  return tables
    .filter(t => Number(t.rows) > 0)
    .map(t => `${t.table}:${t.rows}`)
    .join('，') || '无变动'
}

function handleError(error: Error) {
  console.error('[系统重建] 页面错误', error)
}

onMounted(loadOptions)
</script>

<style scoped>
/* ⚠️ 插槽内容的样式必须自备（布局组件的 .search-row 等类名不作用于页面注入的内容） */
.rebuild-head { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.rebuild-alert { margin-bottom: 8px; }
.rebuild-tip { color: #fa8c16; font-size: 12px; line-height: 1.6; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.option-name { font-weight: 500; }
.option-flag { margin-left: 6px; font-size: 12px; color: #bfbfbf; }
.estimate { margin-left: 6px; color: #fa8c16; font-size: 12px; }
.row-note {
  margin-left: 6px; font-size: 12px; color: #1890ff;
  border-bottom: 1px dashed #1890ff; cursor: help;
}

.rebuild-footer {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 16px; background: #fff; border-top: 1px solid #e8e8e8;
}
.selected-count { font-size: 12px; color: #8c8c8c; }
.footer-gap { flex: 1; }

.modal-alert { margin-bottom: 12px; }
.modal-block { margin-bottom: 12px; }
.modal-label { font-size: 13px; color: #595959; margin-bottom: 4px; }
.scope-list { max-height: 180px; overflow: auto; margin: 0; padding-left: 18px; font-size: 13px; }
.scope-list li { line-height: 1.8; }
.scope-desc { color: #8c8c8c; font-size: 12px; }
.modal-note { color: #8c8c8c; font-size: 12px; }
.modal-tip { margin-top: 12px; color: #fa8c16; font-size: 12px; line-height: 1.6; }
.result-list { max-height: 300px; overflow: auto; }
.result-row {
  display: flex; align-items: baseline; gap: 8px;
  padding: 6px 0; border-bottom: 1px dashed #f0f0f0;
}
.result-name { width: 110px; flex-shrink: 0; font-weight: 500; }
.result-rows { width: 80px; flex-shrink: 0; color: #cf1322; text-align: right; }
.result-zero { color: #8c8c8c; }
.result-detail { font-size: 12px; color: #595959; word-break: break-all; }
</style>
