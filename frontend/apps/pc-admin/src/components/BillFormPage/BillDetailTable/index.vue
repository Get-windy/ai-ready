<template>
  <div
    class="bill-detail-table"
    :class="{ 'table-expanded': expanded }"
  >
    <!-- Loading 遮罩 -->
    <div
      v-if="loading"
      class="table-loading-mask"
    >
      <span class="loading-spinner" />
      <span>加载中...</span>
    </div>
    <!-- 表格容器 -->
    <div
      ref="tableContainerRef"
      class="spreadsheet-table"
      :style="spreadsheetTableStyle"
    >
      <!-- 空数据提示（没有 minRows 时才显示；文案可由 empty-text 覆盖，默认「暂无数据」不影响既有页面） -->
      <div
        v-if="!loading && dataSourceModel.length === 0 && !minRows"
        class="table-empty-text"
      >
        {{ emptyText }}
      </div>
      <table
        v-else
        class="ss-grid"
        :style="gridTableStyle"
      >
        <thead :class="{ 'ss-grouped': headerGrouped }">
          <tr
            v-for="(row, ri) in headerRows"
            :key="'hr-' + ri"
          >
            <th
              v-for="cell in row"
              :key="cell.key"
              :colspan="cell.colspan > 1 ? cell.colspan : undefined"
              :rowspan="cell.rowspan > 1 ? cell.rowspan : undefined"
              :style="cell.col ? getColStyle(cell.col) : undefined"
              :class="cell.col ? getColClass(cell.col) : 'ss-header-group'"
              :data-filler="cell.col && cell.col.key === '__filler__' ? true : undefined"
              :data-col-key="cell.col ? cell.col.key : undefined"
            >
              <!-- ═══ rowNo 列：内嵌齿轮设置图标 ═══ -->
              <template v-if="cell.col && cell.col.type === 'rowNo'">
                <span
                  class="th-settings-btn"
                  title="配置"
                  @click.stop="showColPanel = true"
                >
                  <SettingOutlined />
                </span>
              </template>
              <!-- ═══ checkbox 列：全选复选框 ═══ -->
              <template v-else-if="cell.col && cell.col.type === 'checkbox'">
                <input
                  type="checkbox"
                  class="ss-checkbox ss-checkbox-header"
                  :checked="checkedRows.size === realDataCount && realDataCount > 0"
                  @change="(e) => checkAll((e.target as HTMLInputElement).checked)"
                >
              </template>
              <!-- ═══ 商品名称列：内嵌扫描枪开关 ═══ -->
              <template v-else-if="cell.col && cell.col.showScanToggle">
                <span class="th-title">{{ cell.title }}</span>
                <a-tooltip :title="scanEnabled ? '已开启扫描枪录入' : '扫描枪录入'">
                  <span class="th-scan-label">扫描枪录入</span>
                </a-tooltip>
                <a-switch
                  v-model:checked="scanEnabled"
                  size="small"
                  class="th-scan-switch"
                />
                <span
                  v-if="cell.col.sortable"
                  class="th-sort-icon"
                  :class="getSortIconClass(cell.col)"
                  :title="cell.col.tooltip || '点击排序'"
                  @click="toggleSort(cell.col)"
                >
                  <CaretUpOutlined v-if="sortState.key === cell.col.key && sortState.order === 'asc'" />
                  <CaretDownOutlined v-else-if="sortState.key === cell.col.key && sortState.order === 'desc'" />
                  <span
                    v-else
                    class="sort-neutral"
                  ><CaretUpOutlined /><CaretDownOutlined /></span>
                </span>
              </template>
              <!-- ═══ 普通列标题（含分组标题，分组标题无 col） ═══ -->
              <template v-else>
                <span class="th-title">{{ cell.title }}</span>
                <a-tooltip
                  v-if="cell.col && cell.col.headerTip"
                  :title="cell.col.headerTip"
                >
                  <QuestionCircleOutlined class="th-help-icon" />
                </a-tooltip>
                <span
                  v-if="cell.col && cell.col.sortable"
                  class="th-sort-icon"
                  :class="getSortIconClass(cell.col)"
                  :title="cell.col.tooltip || '点击排序'"
                  @click="toggleSort(cell.col)"
                >
                  <CaretUpOutlined v-if="sortState.key === cell.col.key && sortState.order === 'asc'" />
                  <CaretDownOutlined v-else-if="sortState.key === cell.col.key && sortState.order === 'desc'" />
                  <span
                    v-else
                    class="sort-neutral"
                  ><CaretUpOutlined /><CaretDownOutlined /></span>
                </span>
              </template>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(record, rowIndex) in displayRows"
            :key="record[rowKey] ?? record.id ?? rowIndex"
            class="ss-row"
            :class="{ 'ss-empty-row': record._isEmptyRow }"
            :data-row-key="record._isEmptyRow ? undefined : (record[rowKey] ?? record.id)"
          >
            <td
              v-for="col in visibleColumns"
              :key="col.key"
              :class="getCellClass(col)"
              :style="getColStyle(col)"
              :data-col-key="col.key"
            >
              <!-- 只读列表的 __ghost 空行：除行号外一律空白（避免无 slot 列渲染 '-' 等占位值；
                   可编辑明细的 ghost 行仍需渲染输入控件，故仅限 isViewMode） -->
              <template v-if="isViewMode && record.__ghost && col.type !== 'rowNo' && col.key !== '__filler__'">
                <span class="ss-empty-cell" />
              </template>
              <!-- 填充列：空白 -->
              <template v-else-if="col.key === '__filler__'">
                <span class="ss-empty-cell" />
              </template>
              <!-- 空行（与填充列互斥） -->
              <template v-else-if="record._isEmptyRow">
                <span class="ss-empty-cell" />
              </template>
              <!-- 行号 -->
              <template v-else-if="col.type === 'rowNo'">
                <span class="ss-row-no">{{ rowIndex + 1 }}</span>
              </template>
              <!-- 复选框列 -->
              <template v-else-if="col.type === 'checkbox'">
                <input
                  type="checkbox"
                  class="ss-checkbox"
                  :checked="isChecked(record, rowIndex)"
                  @change="(e) => handleCheckboxChange(record, rowIndex, (e.target as HTMLInputElement).checked)"
                >
              </template>
              <!-- 操作列 -->
              <template v-else-if="col.type === 'action'">
                <!-- 操作按钮（数据驱动，自动折叠）：≤4 平铺撑大；>4 显示前 3 个高频按钮 + "更多"下拉 -->
                <template v-if="col.actionButtons && col.actionButtons.length">
                  <div class="ss-button-cell">
                    <template
                      v-for="btn in (col.actionButtons.length > 4 ? col.actionButtons.slice(0, 3) : col.actionButtons)"
                      :key="btn.key || btn.label"
                    >
                      <a-button
                        :type="btn.type || 'link'"
                        :danger="btn.danger"
                        size="small"
                        @click="btn.onClick?.(record, rowIndex)"
                      >{{ btn.label }}</a-button>
                    </template>
                    <a-dropdown
                      v-if="col.actionButtons.length > 4"
                      :trigger="['click']"
                    >
                      <a-button type="link" size="small">更多</a-button>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item
                            v-for="btn in col.actionButtons.slice(3)"
                            :key="btn.key || btn.label"
                            @click="btn.onClick?.(record, rowIndex)"
                          >{{ btn.label }}</a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </div>
                </template>
                <!-- 兼容页面自定义 slot（不折叠，仅撑大列宽） -->
                <!-- ⚠️ column 必须透传：页面普遍写 `#xxxCell="{ record, column }"` 再取 record[column.key]，
                     漏传会让「金额/数量」类格子恒取到 undefined（显示 0，而合计行却正确）。 -->
                <slot
                  v-else
                  :name="resolveActionSlotName(col)"
                  :record="record"
                  :column="col"
                  :index="rowIndex"
                  :empty="false"
                />
              </template>
              <!-- 按钮列 -->
              <template v-else-if="col.type === 'button'">
                <div class="ss-button-cell">
                  <template
                    v-for="(btn, bi) in col.buttons"
                    :key="bi"
                  >
                    <a-button
                      :type="btn.type || 'link'"
                      :danger="btn.danger"
                      size="small"
                      @click="btn.onClick?.(record, rowIndex)"
                    >
                      {{ btn.label }}
                    </a-button>
                  </template>
                </div>
              </template>
              <!-- 自定义插槽列（column 透传：页面用 record[column.key] 取值的格子依赖它） -->
              <template v-else-if="col.type === 'slot'">
                <slot
                  :name="col.slotName || col.key + 'Cell'"
                  :record="record"
                  :column="col"
                  :index="rowIndex"
                  :empty="false"
                />
              </template>
              <!-- boolean 类型：查看模式 -->
              <template v-else-if="isViewMode && col.type === 'boolean'">
                <span class="ss-cell-text ss-bool-display">{{ record[col.key] ? '✓' : '-' }}</span>
              </template>
              <!-- 查看模式 -->
              <template v-else-if="isViewMode">
                <span class="ss-cell-text">{{ getCellDisplayValue(col, record) }}</span>
              </template>
              <!-- ══ 编辑模式：点击编辑（非编辑态显示文本，点击该格进入编辑框） ═══ -->
              <template v-else>
                <!-- boolean 类型：开关，直接勾选（不进入"点击编辑"） -->
                <label v-if="col.type === 'boolean'" class="ss-bool-cell">
                  <input
                    type="checkbox"
                    :checked="!!record[col.key]"
                    class="ss-bool-checkbox"
                    @change="(e: Event) => updateCell(record, col.key, (e.target as HTMLInputElement).checked)"
                  >
                </label>
                <!-- 正在编辑的单元格 → 渲染编辑器 -->
                <template v-else-if="isActiveEditing(rowIndex, col.key)">
                  <!-- ══ select 列编辑器（成功经验：点击单元格即自动展开下拉）══
                       · 用法：列配置 { type:'select', options:[{value,label}] }。点击单元格直接展开下拉、选中回填 label 文本。
                       · 本编辑器用 a-select，进入编辑态由 enterEdit 置 selectOpen=true 自动展开。
                       · ⚠️ 切勿给它加 :get-popup-container="() => document.body" —— 在非浏览器/边缘环境会抛
                         "Cannot read properties of undefined (reading 'body')"。如确需自定义 popup 容器，请用
                         trigger.ownerDocument.body 安全获取，勿直接引用全局 document。
                       · 交互：失焦/下拉关闭 = 提交保存；Esc = 取消。 -->
                  <a-select
                    v-if="col.type === 'select'"
                    :value="editingCell?.editing"
                    :options="resolveColumnOptions(col, record)"
                    :placeholder="col.placeholder || '请选择'"
                    show-search
                    :option-filter-prop="'label'"
                    size="small"
                    style="width:100%"
                    :open="selectOpen"
                    @change="(val: any) => onEditorInput(val)"
                    @blur="commitEdit(record)"
                    @keydown.esc="cancelEdit()"
                    @dropdown-visible-change="(v: boolean) => { if (!v) commitEdit(record) }"
                  />
                  <!-- date 类型 -->
                  <template v-else-if="col.type === 'date'">
                    <input
                      v-focus
                      v-if="editingCell?.editing"
                      type="date"
                      :value="editingCell.editing"
                      class="ss-native-input ss-native-date"
                      @input="(e: Event) => onEditorInput((e.target as HTMLInputElement).value)"
                      @blur="commitEdit(record)"
                      @keydown="handleEditorKeydown($event, record, rowIndex, col)"
                    >
                    <span v-else class="ss-date-empty"></span>
                  </template>
                  <!-- number 类型 -->
                  <input
                    v-focus
                    v-else-if="col.type === 'number'"
                    type="number"
                    :value="editingCell?.editing ?? 0"
                    :step="getNumberStep(col)"
                    class="ss-native-input ss-native-number"
                    @input="(e: Event) => onEditorInput(parseNumber((e.target as HTMLInputElement).value, col))"
                    @blur="commitEdit(record)"
                    @keydown="handleEditorKeydown($event, record, rowIndex, col)"
                  >
                  <!-- searchable input 类型（输入搜索 + 下拉） -->
                  <template v-else-if="col.searchable">
                    <SearchSelect
                      :model-value="editingCell?.editing"
                      :options="resolveColumnOptions(col, record)"
                      :placeholder="col.placeholder || '搜索'"
                      @update:model-value="(val: any) => onEditorInput(val)"
                      @open-select-modal="handleOpenSelectModal(record, rowIndex, col.key)"
                    />
                  </template>
                  <!-- input 类型（默认） -->
                  <input
                    v-focus
                    v-else
                    type="text"
                    :value="editingCell?.editing ?? ''"
                    :placeholder="col.placeholder || ''"
                    class="ss-native-input ss-native-text"
                    @input="(e: Event) => onEditorInput((e.target as HTMLInputElement).value)"
                    @blur="commitEdit(record)"
                    @keydown="handleEditorKeydown($event, record, rowIndex, col)"
                  >
                </template>
                <!-- 可编辑单元格：非编辑态显示文本，点击 / 回车进入编辑 -->
                <template v-else-if="isEditableText(col)">
                  <span
                    class="ss-cell-editable"
                    :class="{ 'ss-cell-placeholder': !getCellText(col, record) }"
                    tabindex="0"
                    @click="enterEdit(rowIndex, col, record)"
                    @keydown.enter.prevent="enterEdit(rowIndex, col, record)"
                  >{{ getCellText(col, record) || col.placeholder || '' }}</span>
                </template>
                <!-- 其它不可编辑列（只读展示） -->
                <template v-else>
                  <span class="ss-cell-text">{{ getCellDisplayValue(col, record) }}</span>
                </template>
              </template>
            </td>
          </tr>
        </tbody>
        <!-- ═══ 合计行整合进 tfoot ═══ -->
        <tfoot v-if="summaryValues.length">
          <tr class="ss-summary-tr">
            <td
              v-for="col in visibleColumns"
              :key="'sum-' + col.key"
              :class="getSummaryCellClass(col)"
              :style="{ width: col.width ? col.width + 'px' : 'auto' }"
            >
              <template v-if="col.key === '__filler__'">
                <span />
              </template>
              <template v-else-if="col.type === 'rowNo'">
                <span class="summary-total-label">合计</span>
              </template>
              <template v-else>
                {{ getSummaryValue(col.key) }}
              </template>
            </td>
          </tr>
        </tfoot>
      </table>

      <!-- ═══ 底部展开/收起（在滚动容器内，sticky 到底部） ═══ -->
      <div class="detail-expand">
        <a-button
          type="link"
          size="small"
          @click="toggleExpand"
        >
          <FullscreenOutlined />
          {{ expanded ? '表格收起显示' : '表格展开显示' }}
        </a-button>
      </div>
    </div>

    <!-- ═══ 列设置面板（Modal 形式，含个人/全局标签页） ═══ -->
    <a-modal
      v-model:open="showColPanel"
      title="配置"
      :footer="null"
      :mask-closable="true"
      :closable="true"
      width="680px"
      centered
    >
      <a-tabs v-model:active-key="colConfigTab" class="col-config-tabs">
        <!-- ── 个人配置 Tab ── -->
        <a-tab-pane key="personal" tab="个人配置">
          <div class="col-tab-tip">只对当前操作员有效，在全局配置内配置字段显示、排序</div>
          <div class="col-settings-panel-modal">
            <div class="col-panel-body">
              <div
                v-for="(setting, si) in personalPanelSettings"
                :key="setting.key"
                class="col-setting-row"
                :class="{ 'col-setting-ghost': !setting.visible }"
              >
                <span class="col-seq">{{ si + 1 }}</span>
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="onColSettingChange"
                />
                <span
                  class="col-setting-title"
                  draggable="true"
                  @dragstart="onDragStart(panelToRealIndex(si, 'personal'))"
                  @dragover.prevent="onDragOver(panelToRealIndex(si, 'personal'))"
                  @drop="onDrop(si)"
                >
                  <span class="drag-handle">⠿</span>
                  {{ setting.title }}
                </span>
                <span class="col-display-name">{{ setting.displayName || setting.title }}</span>
                <a-input-number
                  v-model:value="setting.width"
                  class="col-width-input"
                  :min="40"
                  :max="500"
                  size="small"
                  @change="onColSettingChange"
                />
              </div>
            </div>
            <div class="col-panel-footer">
              <a-button size="middle" @click="resetColumnSettings">恢复默认</a-button>
            </div>
          </div>
        </a-tab-pane>

        <!-- ── 全局配置 Tab ── -->
        <a-tab-pane key="global" tab="全局配置">
          <div class="col-tab-tip">系统级配置，无单据配置权限的操作员只能在已配置范围内进行操作</div>
          <div class="col-settings-panel-modal">
            <div class="col-panel-body">
              <div
                v-for="(setting, si) in globalPanelSettings"
                :key="setting.key"
                class="col-setting-row"
                :class="{ 'col-setting-ghost': !setting.visible }"
              >
                <span class="col-seq">{{ si + 1 }}</span>
                <a-checkbox
                  v-model:checked="setting.visible"
                  :disabled="isLockedColumn(setting.key)"
                  @change="onGlobalSettingChange"
                />
                <span
                  class="col-setting-title"
                  draggable="true"
                  @dragstart="onDragStart(panelToRealIndex(si, 'global'))"
                  @dragover.prevent="onDragOver(panelToRealIndex(si, 'global'))"
                  @drop="onDrop(si)"
                >
                  <span class="drag-handle">⠿</span>
                  {{ setting.title }}
                </span>
                <a-input
                  v-model:value="setting.displayName"
                  class="col-display-name-input"
                  size="small"
                  :placeholder="setting.title"
                  @change="onGlobalSettingChange"
                />
                <a-input-number
                  v-model:value="setting.width"
                  class="col-width-input"
                  :min="40"
                  :max="500"
                  size="small"
                  @change="onGlobalSettingChange"
                />
              </div>
            </div>
            <div class="col-panel-footer">
              <a-button size="middle" @click="resetColumnSettings">恢复默认</a-button>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>


    <!-- 分页器：与全站统一走经典形态（首页/上页/第(x/y)页/下页/尾页/跳转/共 N 条记录/每页显示 N 行）。
         表格展开显示时自动让位 —— 它就在表格正下方，展开时必须收起，表格才能占满到页面底部。 -->
    <StandardPagination
      v-if="showPagination && !expanded"
      variant="classic"
      v-model:current="currentPage"
      v-model:page-size="currentPageSize"
      :total="total"
      :page-size-options="pageSizeOptions"
      :hide-on-single-page="false"
      @change="onPageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive, nextTick, onMounted, onBeforeUnmount, useSlots } from 'vue'
import { SettingOutlined, FullscreenOutlined, CaretUpOutlined, CaretDownOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import { Modal, Button, Checkbox, Select, InputNumber, Input, Tabs } from 'ant-design-vue'
import type { DetailColumnConfig, ColumnSetting, DetailColumnOption } from './types'
import SearchSelect from '@/components/SearchSelect/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'

defineOptions({ name: 'BillDetailTable' })

// ══════════════════════════════════════════════════════════════════
// 【BillDetailTable 金标准使用规范】—— 业务页接入时请遵守，否则易出现折叠/空白问题
//   1. minRows 默认 20：空数据时自动填充 __ghost 占位行（可编辑空行）。
//      → 业务页【不要手动 push 空行】，否则真实空行与 ghost 占位行混排，
//        导致「只显示部分行 + 其余折叠/下方空白」。
//   2. 表格高度由 flex 链自适应：【默认不要传 :max-height】。
//      → 传了 max-height 会用固定高度限死，出现「只显示 N 行 + 下方留白」；
//        超出区域的行由底部「表格展开显示」折叠展示，符合"占满区域、超出折叠"。
//   3. 默认收起（expanded=false）：收起态 flex 占满、超出折叠；点「表格展开显示」为 70vh 滚动。
//      → 如需某业务页默认展开，可传 :default-expanded="true"（默认 false，向后兼容）。
//   4. 支持 v-model:data-source 双向回写父数组；占位行录入有值后自动提升为真实行。
// ══════════════════════════════════════════════════════════════════
const props = withDefaults(defineProps<{
  /** 列配置 */
  columns: DetailColumnConfig[]
  /** 是否查看模式 */
  viewMode?: boolean
  /**
   * 行唯一键字段名（默认 id）。
   * 多行共用主表 id 的场景（如「按明细」查询返回的是主表 id）必须传明细行主键字段（如 itemId），
   * 否则 :key 重复会触发 Vue「Duplicate keys found during update」，导致行复用错乱。
   */
  rowKey?: string
  /** 表格最大高度（0=不限制，由 flex 父容器驱动高度；>0 时用 inline style 限制） */
  maxHeight?: number
  /** 合计列定义 */
  summaryColumns?: { key: string; value: number; highlight?: boolean }[]
  /** 是否加载中 */
  loading?: boolean
  /** 最小显示行数（不足时用空行填充；默认 20，业务页确需改变才传入并在传参处注释原因） */
  minRows?: number
  /** 空数据提示文案（默认「暂无数据」；仅在未传 min-rows 时显示，供对标页还原 ql361 空态文案） */
  emptyText?: string
  /** 列配置存储键名（不同表格使用不同键，避免冲突） */
  storageKey?: string
  /** 全局列配置持久化键名（传入后「全局配置」Tab 落后端 user-config，跨浏览器生效） */
  globalConfigKey?: string
  /** 填充模式：右侧生成空白列占满剩余宽度 */
  fillMode?: boolean
  /** 是否显示分页器 */
  showPagination?: boolean
  /** 当前页码 */
  current?: number
  /** 每页大小 */
  pageSize?: number
  /** 总条数 */
  total?: number
  /** 每页显示条数选项 */
  pageSizeOptions?: number[]
  /** 回车跳转列（按回车跳转到下一行同列） */
  enterJumpColumns?: string[]
  /** 公式配置（{ 列key: 表达式 }，支持 {fieldName} 占位符） */
  formulas?: Record<string, string>
  /** 初始是否展开（默认收起：收起态受 maxHeight 限高；展开态 70vh 滚动） */
  defaultExpanded?: boolean
}>(), {
  viewMode: false,
  rowKey: 'id',
  maxHeight: 0,
  summaryColumns: () => [],
  loading: false,
  // 默认 20 行（金标准）：与「空数据时显示空提示（dataSource 为空且未传 minRows）」配合；业务页确需改变才在页面传 :min-rows 并注释原因
  minRows: 20,
  emptyText: '暂无数据',
  defaultExpanded: false,
  storageKey: 'product-unit-columns-config',
  // ⚠️ 组件级解决操作列超宽（勿改回 false）：默认启用 __filler__ 空列占满剩余宽度，
  //    使操作列(唯一 auto 列)不被剩余空间撑宽；列多的场景 __filler__ 会被压缩到 0，无副作用。
  //    页面无需再传 fill-mode。个别页面确需无填充时可显式 :fill-mode="false"。
  fillMode: true,
  showPagination: false,
  current: 1,
  pageSize: 20,
  total: 0,
  pageSizeOptions: () => [10, 20, 50, 100],
  enterJumpColumns: () => [],
  formulas: () => ({}),
})

// ═══ 可编辑数据：支持 v-model:data-source（组件管理可编辑行并双向回写父数组，免父逻辑接入） ═══
const dataSourceModel = defineModel<any[]>('dataSource', { default: () => [] })

const emit = defineEmits<{
  'cellChange': [record: any, fieldKey: string, value: any]
  /** 展开/收起状态变化 */
  'expand-change': [expanded: boolean]
  /** 复选框变化 */
  'checkbox-change': [record: any, rowIndex: number, checked: boolean]
  /** 全选/取消全选 */
  'checkbox-all': [checked: boolean, records: any[]]
  /** 排序变化 */
  'sort-change': [key: string | null, order: 'asc' | 'desc' | null]
  /** 打开选择弹窗（空关键字回车时触发） */
  'openSelectModal': [record: any, rowIndex: number, fieldKey: string]
  /** 分页变化 */
  'update:current': [number]
  'update:pageSize': [number]
  'page-change': [{ page: number; pageSize: number }]
}>()

// ═══ 状态 ═══
const expanded = ref(props.defaultExpanded)
const showColPanel = ref(false)
const scanEnabled = ref(false)
const tableContainerRef = ref<HTMLElement>()

/** 各列实际渲染宽度（key → px），由 measureColWidths 回填，供冻结列偏移使用 */
const measuredColWidths = ref<Record<string, number>>({})

/**
 * 回填各列「实际渲染宽度」，供冻结列偏移使用（见 stickyOffset 的说明）。
 * 只在数值真变化时写回，避免 ResizeObserver → 样式变化 → 再次回调的自激循环。
 */
function measureColWidths() {
  const root = tableContainerRef.value
  if (!root) return
  const ths = root.querySelectorAll<HTMLElement>('.ss-grid thead th[data-col-key]')
  if (!ths.length) return
  const next: Record<string, number> = {}
  ths.forEach((th) => {
    const key = th.dataset.colKey
    const w = th.offsetWidth
    if (key && w > 0) next[key] = w
  })
  const prev = measuredColWidths.value
  const keys = Object.keys(next)
  if (keys.length && keys.every(k => prev[k] === next[k]) && keys.length === Object.keys(prev).length) return
  measuredColWidths.value = next
}

let colResizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    measureColWidths()
    // 页面把表格配成默认展开时，同样要让宿主收起下方区域，保持初始状态自洽
    if (expanded.value) notifyHostExpand(true)
  })
  if (typeof ResizeObserver !== 'undefined' && tableContainerRef.value) {
    colResizeObserver = new ResizeObserver(() => measureColWidths())
    colResizeObserver.observe(tableContainerRef.value)
  }
})

onBeforeUnmount(() => {
  colResizeObserver?.disconnect()
  colResizeObserver = null
})

// ═══ 操作列(按钮列)宽度：采用页面定义的 col.width（配合按钮折叠），不再按内容测量，
//     这样多 tab/多实例各自独立（+/− 等窄操作列不被撑大），也不受单项宽内容影响。 ═══

// ═══ 点击编辑：单实例编辑框状态（同一时刻仅一个单元格处于编辑态） ═══
const editingCell = ref<{ rowIndex: number; fieldKey: string; original: any; editing: any } | null>(null)

/** select 编辑器：进入编辑态时自动展开下拉选项 */
const selectOpen = ref(false)

/** 自动聚焦指令：进入编辑态时聚焦当前编辑器 */
const vFocus = {
  mounted: (el: HTMLElement) => {
    const target = el as HTMLInputElement
    target.focus?.()
    target.select?.()
  },
}
const colPanelRef = ref<HTMLElement>()

// 排序状态
const sortState = reactive<{
  key: string | null
  order: 'asc' | 'desc' | null
}>({
  key: null,
  order: null,
})

// 复选框状态
const checkedRows = ref<Set<number>>(new Set())
const checkedRecords = ref<any[]>([])

// 分页相关状态
const currentPage = ref(props.current)
const currentPageSize = ref(props.pageSize)

// 同步 props 变化
watch(() => props.current, (val) => {
  currentPage.value = val
})

watch(() => props.pageSize, (val) => {
  currentPageSize.value = val
})

// 分页事件处理
function onPageChange(page: number, pageSize: number) {
  // 触发父组件更新
  emit('update:current', page)
  emit('update:pageSize', pageSize)
  emit('page-change', { page, pageSize })
}

const isViewMode = computed(() => props.viewMode)

/** 表格容器样式
 * ⚠️ maxHeight 默认不应用（maxHeight prop 默认值 = 0），
 *    高度完全由 flex 链驱动。如果外部传入 maxHeight > 0，
 *    会用 inline style 限制高度，此时 flex 和 maxHeight 取小值，
 *    可能导致 .spreadsheet-table 下方出现空白区域。
 *    推荐：在 flex 布局中不要传 maxHeight，让组件自适应。
 */
const spreadsheetTableStyle = computed(() => {
  // ⚠️ 组件级（勿改回）：完全忽略传入的 maxHeight，高度由 flex 链驱动占满容器（BillFormPage 的 flex:1）。
  //    若应用 maxHeight 会固定高度，导致「只显示部分行 + 下方空白」。
  //    收起态 flex 占满、超出经「表格展开显示」折叠；展开态由 CSS .table-expanded 的 70vh 控制。
  //    所有页面默认即占满，无需也不应传 max-height；个别页确需固定高度可显式覆盖。
  return {}
})

// ═══ 数据行（带空行填充和排序） ═══
const displayRows = computed(() => {
  const data = [...dataSourceModel.value]

  // 应用排序
  if (sortState.key && sortState.order) {
    const col = leafColumns.value.find(c => c.key === sortState.key)
    if (col?.sorter) {
      data.sort(col.sorter)
    } else {
      // 默认排序逻辑
      data.sort((a, b) => {
        const valA = a[sortState.key!]
        const valB = b[sortState.key!]
        if (valA == null && valB == null) return 0
        if (valA == null) return 1
        if (valB == null) return -1
        if (typeof valA === 'number' && typeof valB === 'number') {
          return valA - valB
        }
        return String(valA).localeCompare(String(valB))
      })
    }
    if (sortState.order === 'desc') {
      data.reverse()
    }
  }

  // 空行填充（可编辑占位行：空数据时显示 minRows 个可编辑行，序号连续；录入有值后由父组件提升为真实行）
  const minRows = props.minRows || 0
  if (data.length < minRows) {
    for (let i = data.length; i < minRows; i++) {
      data.push({ __ghost: true, id: `ghost-${i}` })
    }
  }
  return data
})

// 说明：操作列宽度采用页面定义的 col.width（配合按钮折叠），不再按内容测量；
// 这样多 tab/多实例各自独立（+/− 窄操作列不被撑大），且不被单项宽内容影响。

// 真实数据行数（排除空行）
const realDataCount = computed(() => dataSourceModel.value.length)

// ═══ 排序功能 ═══
function toggleSort(col: DetailColumnConfig) {
  if (!col.sortable) return

  if (sortState.key === col.key) {
    // 循环切换排序状态：null -> asc -> desc -> null
    if (sortState.order === 'asc') {
      sortState.order = 'desc'
    } else if (sortState.order === 'desc') {
      sortState.key = null
      sortState.order = null
    } else {
      sortState.order = 'asc'
    }
  } else {
    sortState.key = col.key
    sortState.order = 'asc'
  }

  emit('sort-change', sortState.key, sortState.order)
}

function getSortIconClass(col: DetailColumnConfig) {
  if (sortState.key !== col.key) return 'sort-none'
  return sortState.order === 'asc' ? 'sort-asc' : 'sort-desc'
}

// ═══ 复选框方法 ═══
function isChecked(record: any, rowIndex: number): boolean {
  return checkedRows.value.has(rowIndex)
}

/**
 * 清空勾选
 *
 * ⚠️ 勾选按 **rowIndex** 持有，而父组件（BillTableList）的 clearSelection 只清它自己的
 * selectedRecords；若不同步清这里，会出现「批量条消失、行上仍勾着」，且换查询条件后
 * 同一 index 指向**另一行**（勾选漂移 → 批量操作作用到用户没勾的数据）。
 */
function clearSelection() {
  checkedRows.value.clear()
  checkedRecords.value = []
}

/** 行身份键（判断 dataSource 是否换了一批数据） */
function rowIdentity(record: any, index: number): string {
  return String(record?.[props.rowKey || 'id'] ?? record?.key ?? index)
}

// 数据集合变化（换查询/翻页/刷新后行不同）→ 清空勾选，杜绝索引漂移；
// 同一批数据重渲染（父级无关状态变化）保留勾选。
// 注：`dataSource` 由 `defineModel('dataSource')` 声明（见上方 dataSourceModel），
// **不在** `defineProps` 的类型里，故不能写 `props.dataSource`（TS2339）；
// 非本地模式下 `dataSourceModel.value` 即父级传入的数组，语义完全等价。
watch(() => dataSourceModel.value, (next, prev) => {
  const nextKeys = (next || []).map(rowIdentity).join('|')
  const prevKeys = (prev || []).map(rowIdentity).join('|')
  if (nextKeys !== prevKeys) {
    clearSelection()
  }
})

function handleCheckboxChange(record: any, rowIndex: number, checked: boolean) {
  if (checked) {
    checkedRows.value.add(rowIndex)
    checkedRecords.value.push(record)
  } else {
    checkedRows.value.delete(rowIndex)
    const idx = checkedRecords.value.indexOf(record)
    if (idx > -1) {
      checkedRecords.value.splice(idx, 1)
    }
  }
  emit('checkbox-change', record, rowIndex, checked)
}

function checkAll(checked: boolean) {
  checkedRows.value.clear()
  checkedRecords.value = []
  if (checked) {
    // 只选择真实数据行，排除空行
    dataSourceModel.value.forEach((record, index) => {
      checkedRows.value.add(index)
      checkedRecords.value.push(record)
    })
  }
  emit('checkbox-all', checked, [...checkedRecords.value])
}

function getCheckedRecords(): any[] {
  return [...checkedRecords.value]
}

// 暴露方法给父组件
defineExpose({
  getCheckedRecords,
  clearSelection,
  openColumnConfig: () => { showColPanel.value = true },
})

// ═══ 分组表头：扁平化叶子列（分组节点自身不参与行渲染与列配置） ═══
function flattenColumns(cols: any[], parent?: { key: string; title: string }): any[] {
  const out: any[] = []
  for (const col of cols || []) {
    if (Array.isArray(col?.children) && col.children.length) {
      out.push(...flattenColumns(col.children, { key: col.key, title: col.title || '' }))
    } else if (parent) {
      out.push({ ...col, __groupKey: parent.key, __groupTitle: parent.title })
    } else {
      out.push({ ...col })
    }
  }
  return out
}

/** 叶子列（分组列取其 children；无分组时与 props.columns 等价） */
const leafColumns = computed<DetailColumnConfig[]>(() => flattenColumns(props.columns))
/** 是否存在分组表头（决定 thead 渲染一行还是两行） */
const headerGrouped = computed(() => leafColumns.value.some(c => (c as any).__groupKey))

/**
 * 列配置弹窗展示名：分组列用「分组名-子列名」（如 期初余额-借方），
 * 与对标列配置的列名口径一致；无分组列仍为自身标题。
 */
function configTitle(col: DetailColumnConfig): string {
  const group = (col as any).__groupTitle
  return group ? `${group}-${col.title}` : col.title
}

// ═══ 列设置（运行时） ═══
const defaultSettings = computed<ColumnSetting[]>(() =>
  leafColumns.value.map(col => ({
    key: col.key,
    title: configTitle(col),
    displayName: configTitle(col),
    visible: !col.defaultHidden,
    width: col.width || 100,
    fixed: col.fixed || '',
  }))
)

const columnSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

// 列配置面板活动标签：个人配置 / 全局配置
const colConfigTab = ref<'personal' | 'global'>('personal')

// 全局配置（独立存储，显示名可编辑）
const globalSettings = reactive<ColumnSetting[]>([...defaultSettings.value])

/**
 * 列配置弹窗展示项：剔除系统列（LOCKED_COLUMNS：序号 / 操作）。
 * 对标 ql361 的列配置弹窗只列出业务数据列（如互联账号页仅 往来单位/互联用户名/手机号 三条），
 * 序号与操作是系统列不参与配置；这里只影响面板展示，表格渲染顺序仍以 columnSettings 为准。
 */
function panelSettingsOf(target: ColumnSetting[]): ColumnSetting[] {
  return target.filter(s => !isLockedColumn(s.key))
}
const personalPanelSettings = computed(() => panelSettingsOf(columnSettings))
const globalPanelSettings = computed(() => panelSettingsOf(globalSettings))

/** 面板序号 → columnSettings/globalSettings 真实下标（拖拽排序用） */
function panelToRealIndex(panelIdx: number, tab: 'personal' | 'global'): number {
  const list = tab === 'personal' ? personalPanelSettings.value : globalPanelSettings.value
  const target = tab === 'personal' ? columnSettings : globalSettings
  if (panelIdx < 0 || panelIdx >= list.length) return panelIdx
  const real = target.findIndex(s => s.key === list[panelIdx].key)
  return real < 0 ? panelIdx : real
}

// 从本地存储加载列配置
const STORAGE_KEY = computed(() => props.storageKey || 'product-unit-columns-config')
const GLOBAL_STORAGE_KEY = computed(() => (props.storageKey || 'product-unit-columns-config') + '-global')

/**
 * 按当前视图的列定义重建列设置：
 * - 该视图有存储配置 → 用存储值（用户自定义优先）；未记录的列回落到该视图默认显隐
 * - 该视图无存储配置 → 全部用该视图默认显隐（!defaultHidden）
 *
 * 必须**无条件重建**：多视图（Tab）页面切换 storage-key 时，若不重建，
 * watch(leafColumns) 会因同名列（如按单据的「已收数量」与按明细的「已收货数量」）键相同而保留
 * 上一个视图的 visible，导致目标视图的默认可见列被上一个视图的隐藏状态覆盖。
 */
function loadStoredSettings(storageKey: string, target: ColumnSetting[]) {
  let parsed: any[] | null = null
  try {
    const stored = localStorage.getItem(storageKey)
    if (stored) parsed = JSON.parse(stored)
  } catch (error) {
    console.warn('加载列配置失败:', error)
  }
  const mergedConfig = leafColumns.value.map(col => {
    const storedCol = parsed?.find((sc: any) => sc.key === col.key)
    const title = configTitle(col)
    return storedCol
      ? { key: col.key, title, displayName: storedCol.displayName || title, visible: storedCol.visible ?? !col.defaultHidden, width: storedCol.width || col.width || 100, fixed: storedCol.fixed || col.fixed || '' }
      : { key: col.key, title, displayName: title, visible: !col.defaultHidden, width: col.width || 100, fixed: col.fixed || '' }
  })
  target.splice(0, target.length, ...mergedConfig)
}

function saveStoredSettings(storageKey: string, settings: ColumnSetting[]) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(settings))
  } catch (error) {
    console.warn('保存列配置失败:', error)
  }
}

// 加载个人配置和全局配置
loadStoredSettings(STORAGE_KEY.value, columnSettings)
loadStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
// 全局配置：页面显式传入 global-config-key 时以后端为准（跨浏览器/终端）
loadGlobalSettingsFromServer()

/**
 * 多视图（Tab）页面会按视图动态切换 storage-key（如 `xxx-table-columns-doc/-detail`），
 * 此时必须重新加载该视图的列配置，否则切 Tab 后仍沿用上一个 Tab 的设置（要刷新页面才生效）。
 * flush:'post' 确保在 leafColumns 同步（上面的 watch）之后执行，用当前视图的列定义合并存储值。
 */
watch(STORAGE_KEY, () => {
  loadStoredSettings(STORAGE_KEY.value, columnSettings)
  loadStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
  loadGlobalSettingsFromServer()
}, { flush: 'post' })

/**
 * 是否保留了用户自定义显示名：
 * 默认 displayName 恒等于 title，仅当用户改过名才会出现 displayName !== title。
 * 多视图（Tab）复用同一列 key 时，未自定义的列名必须跟随当前视图的列定义，
 * 否则切换视图会出现「表头名与列不符」（如按部门的「部门」串到按类型的「费用名称」）。
 */
function isRenamed(existing?: ColumnSetting): boolean {
  return !!existing && !!existing.displayName && existing.displayName !== existing.title
}

// 同步 columns 变化（分组列以叶子列为准）
watch(leafColumns, (newCols) => {
  // 同步个人配置
  const existingMap = new Map(columnSettings.map(s => [s.key, s]))
  const newSettings = newCols.map(col => {
    const existing = existingMap.get(col.key)
    const title = configTitle(col)
    return {
      key: col.key,
      title,
      displayName: isRenamed(existing) ? existing!.displayName! : title,
      visible: existing ? existing.visible : !col.defaultHidden,
      width: existing ? existing.width : (col.width || 100),
      fixed: existing ? existing.fixed : (col.fixed || ''),
    }
  })
  columnSettings.splice(0, columnSettings.length, ...newSettings)

  // 同步全局配置
  const globalMap = new Map(globalSettings.map(s => [s.key, s]))
  const newGlobalSettings = newCols.map(col => {
    const existing = globalMap.get(col.key)
    const title = configTitle(col)
    return {
      key: col.key,
      title,
      displayName: isRenamed(existing) ? existing!.displayName! : title,
      visible: existing ? existing.visible : !col.defaultHidden,
      width: existing ? existing.width : (col.width || 100),
      fixed: existing ? existing.fixed : (col.fixed || ''),
    }
  })
  globalSettings.splice(0, globalSettings.length, ...newGlobalSettings)
}, { deep: true })

/** 可见列（过滤隐藏 + 按设置顺序 + 应用冻结 + 可选填充列） */
const visibleColumns = computed<DetailColumnConfig[]>(() => {
  const colMap = new Map(leafColumns.value.map(c => [c.key, c]))
  const cols = columnSettings
    .filter(s => s.visible && s.key !== '__filler__')
    .map(s => {
      const col = colMap.get(s.key)!
      return {
        ...col,
        // 使用个人配置的 displayName（优先）或全局配置的 displayName
        title: s.displayName || col.title,
        width: s.width,
        fixed: s.fixed as 'left' | 'right' | undefined,
      }
    })
  // 填充模式：在尾部追加空白列占满剩余空间
  if (props.fillMode) {
    cols.push({ key: '__filler__', title: '', type: '__filler__', width: undefined } as any)
  }
  // ⚠️ 组件级（勿删）：序号列(rowNo)强制为最左侧一列。
  //    即使页面把勾选框(rowCheckbox)等配置在 rowNo 前面，也统一调整序号到最左，保证序号列始终在表格最左侧。
  const rowNoIdx = cols.findIndex(c => c.type === 'rowNo')
  if (rowNoIdx > 0) {
    const [rn] = cols.splice(rowNoIdx, 1)
    cols.unshift(rn)
  }
  return cols
})

// 列配置或行数据变化都会改变列的实际渲染宽度 → 重新实测（冻结列偏移依赖它）
watch(
  () => visibleColumns.value.map(c => `${c.key}:${c.width ?? ''}:${c.fixed ?? ''}`).join('|'),
  () => nextTick(measureColWidths)
)
watch(() => displayRows.value.length, () => nextTick(measureColWidths))

// ═══ 表头渲染行：无分组=单行；有分组=两行（组标题 + 叶子列） ═══
interface HeaderCell {
  key: string
  title: string
  colspan: number
  rowspan: number
  col?: DetailColumnConfig
}

const headerRows = computed<HeaderCell[][]>(() => {
  const cols = visibleColumns.value
  // 无分组：单行，全部叶子列
  if (!headerGrouped.value) {
    return [cols.map(col => ({ key: col.key, title: col.title, colspan: 1, rowspan: 1, col }))]
  }
  const row1: HeaderCell[] = []
  const row2: HeaderCell[] = []
  let i = 0
  while (i < cols.length) {
    const col: any = cols[i]
    const groupKey = col.__groupKey
    if (!groupKey) {
      row1.push({ key: col.key, title: col.title, colspan: 1, rowspan: 2, col })
      i++
      continue
    }
    let j = i
    while (j < cols.length && (cols[j] as any).__groupKey === groupKey) j++
    const span = j - i
    if (span === 1) {
      // 组内只剩 1 列：不渲染组标题，避免出现只有一列的分组表头
      row1.push({ key: col.key, title: col.title, colspan: 1, rowspan: 2, col })
    } else {
      row1.push({ key: `group-${groupKey}`, title: col.__groupTitle || '', colspan: span, rowspan: 1 })
      for (let k = i; k < j; k++) {
        const leaf: any = cols[k]
        row2.push({ key: leaf.key, title: leaf.title, colspan: 1, rowspan: 1, col: leaf })
      }
    }
    i = j
  }
  return [row1, row2]
})

// 计算所有可见列的总宽度（不含填充列）
const totalContentWidth = computed(() => {
  return visibleColumns.value.reduce((sum, col) => {
    if (col.key === '__filler__') return sum
    return sum + (col.width || 80)
  }, 0)
})

// 表格动态样式：宽度 100%，最小宽度为列宽总和
// — 列宽不足容器宽时，表格 100% 填充（配合填充列占满剩余空间）
// — 列宽超出容器时，表格按列宽撑开，外层 overflow:auto 产生横向滚动
const gridTableStyle = computed(() => {
  if (totalContentWidth.value === 0) return { width: '100%' }
  return { width: '100%', minWidth: totalContentWidth.value + 'px' }
})

// ═══ 合计值 ═══
const summaryValues = computed(() => props.summaryColumns)

function getSummaryValue(key: string): string {
  const found = props.summaryColumns.find(c => c.key === key)
  return found ? String(found.value) : ''
}

// ═══ 样式辅助 ═══
/**
 * 冻结列偏移的宽度口径 —— 必须用「实际渲染宽度」而不是列配置里的 width。
 * ⚠️ 组件级（踩过的坑）：操作列/按钮列在 getColStyle 里被强制 width:auto（保证按钮总放得下），
 *    于是它**实际渲染宽度 ≠ 配置里的 width**；若偏移仍按配置 width 累加，排在它后面的冻结列
 *    （如「按单据」页的勾选列）会被钉到一个错误位置 —— 表现为表格中间浮着一个错位的选中列。
 *    实测宽度在渲染后用 ResizeObserver 回填，未测到时才退回配置值。
 */
function colEffectiveWidth(c: DetailColumnConfig): number {
  return measuredColWidths.value[c.key] || c.width || 100
}

/**
 * 冻结列累计偏移：
 * 左侧冻结按可见顺序累加「排在该列之前的左冻结列」宽度，右侧冻结从末尾往前累加。
 * ⚠️ 不可直接写 left:0 —— 多列左冻结时会全部钉在同一位置而重叠。
 */
function stickyOffset(col: DetailColumnConfig): { left?: string; right?: string } {
  const cols = visibleColumns.value
  if (col.fixed === 'left') {
    let left = 0
    for (const c of cols) {
      if (c.key === col.key) return { left: left + 'px' }
      if (c.fixed === 'left') left += colEffectiveWidth(c)
    }
    return { left: '0px' }
  }
  if (col.fixed === 'right') {
    let right = 0
    for (let i = cols.length - 1; i >= 0; i--) {
      const c = cols[i]
      if (c.key === col.key) return { right: right + 'px' }
      if (c.fixed === 'right') right += colEffectiveWidth(c)
    }
    return { right: '0px' }
  }
  return {}
}

function getColStyle(col: DetailColumnConfig) {
  if (col.key === '__filler__') {
    // ⚠️ 填充列必须 width:100% 才能占满剩余宽度（table-layout:auto 下空内容吸不到剩余）。
    //    否则操作列(唯一 auto 列)会被剩余空间撑宽——列少时操作列超宽、随容器漂移。
    return { width: '100%' }
  }
  const isAction = col.type === 'action' || col.type === 'button'
  const sticky = stickyOffset(col)
  if (isAction) {
    // 操作列：table-layout:auto 下按该列最宽内容自适应（width:auto + nowrap），按钮总放得下、不溢出也不撑大；
    // min-width 80 防 +/− 等窄操作列塌缩；按钮过多(>4)由 actionButtons 折叠为"更多"下拉。
    return {
      width: 'auto',
      minWidth: 80,
      whiteSpace: 'nowrap',
      ...sticky,
    }
  }
  return {
    width: col.width ? col.width + 'px' : 'auto',
    minWidth: col.width ? col.width + 'px' : '80px',
    ...sticky,
  }
}

function getColClass(col: DetailColumnConfig) {
  if (col.key === '__filler__') {
    return { 'ss-filler-col': true }
  }
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-header-settings': col.type === 'rowNo',
  }
}

function getCellClass(col: DetailColumnConfig) {
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-cell-number': col.type === 'number',
    'ss-cell-action': col.type === 'action' || col.type === 'button',
    'ss-cell-checkbox': col.type === 'checkbox',
    'ss-cell-center': col.align === 'center',
    'ss-cell-right': col.align === 'right',
  }
}

function getSummaryCellClass(col: DetailColumnConfig) {
  const found = props.summaryColumns.find(c => c.key === col.key)
  return {
    'ss-fixed-left': col.fixed === 'left',
    'ss-fixed-right': col.fixed === 'right',
    'ss-cell-number': col.type === 'number',
    'summary-highlight': found?.highlight,
  }
}

function getNumberStep(col: DetailColumnConfig): string {
  if (col.precision === 0) return '1'
  if (col.precision === 1) return '0.1'
  return '0.01'
}

function parseNumber(val: string, col: DetailColumnConfig): number {
  const num = parseFloat(val)
  if (isNaN(num)) return 0
  const precision = col.precision ?? 2
  return Number(num.toFixed(precision))
}

// ══ 单元格更新 ═══
function updateCell(record: any, fieldKey: string, value: any) {
  record[fieldKey] = value
  // 公式计算：检查是否有公式引用了此字段
  for (const [targetKey, formula] of Object.entries(props.formulas)) {
    if (!formula) continue
    const fieldRef = `{${fieldKey}}`
    if (formula.includes(fieldRef)) {
      const result = evaluateFormula(formula, record)
      if (result !== null && result !== undefined) {
        record[targetKey] = result
      }
    }
  }
  emit('cellChange', record, fieldKey, value)
}

// ═══ 点击编辑：单实例编辑框 ═══

/** 该列是否可"点击编辑"为文本（排除 boolean/操作/按钮/复选框/slot/行号/填充列，及只读/锁定） */
function isEditableText(col: DetailColumnConfig, record?: any): boolean {
  if (isViewMode.value) return false
  if (col.type === 'boolean' || col.type === 'action' || col.type === 'button' || col.type === 'checkbox' || col.type === 'slot' || col.type === 'rowNo' || col.key === '__filler__') return false
  if (col.readonly || col.locked) return false
  if (record && record._isEmptyRow) return false
  return true
}

/** 当前是否正在编辑该单元格 */
function isActiveEditing(rowIndex: number, fieldKey: string): boolean {
  return !!editingCell.value && editingCell.value.rowIndex === rowIndex && editingCell.value.fieldKey === fieldKey
}

/** 解析列 options：支持三种形态
 *  1. 静态数组（列级）：[{value,label}]，原样
 *  2. 函数（行级）：(record)=>[{value,label}]
 *  3. optionsField 字段名：读 record[optionsField] 作为该行下拉选项
 */
function resolveColumnOptions(col: DetailColumnConfig, record: any): DetailColumnOption[] {
  if (typeof col.options === 'function') {
    return col.options(record) || []
  }
  if (col.optionsField) {
    return record?.[col.optionsField] || []
  }
  return col.options || []
}

/** 单元格非编辑态文本（select/searchable 显示所选 option 的 label） */
function getCellText(col: DetailColumnConfig, record: any): string {
  const val = record?.[col.key]
  const options = resolveColumnOptions(col, record)
  if ((col.type === 'select' || col.searchable) && options.length) {
    const opt = options.find((o: any) => String(o.value) === String(val))
    if (opt) return opt.label ?? ''
  }
  if (val === null || val === undefined) return ''
  return String(val)
}

/** 进入编辑态（点击 / 回车） */
function enterEdit(rowIndex: number, col: DetailColumnConfig, record: any) {
  if (!isEditableText(col, record)) return
  const raw = record[col.key]
  editingCell.value = {
    rowIndex,
    fieldKey: col.key,
    original: raw,
    // ⚠️ 空值一律以 '' 作为编辑初值（number 列也不要预填 0）：否则 NULL 单元格"点开即把 0 当成新值"提交，
    //    会把空值写成 0（配合下方 isEmptyValue 判定，点开不动不会再触发 cellChange）。
    editing: raw === null || raw === undefined ? '' : raw,
  }
  // select 列：进入编辑态即展开下拉选项（避免"先显示请选择再点击"）
  if (col.type === 'select') selectOpen.value = true
}

/** 编辑态输入暂存 */
function onEditorInput(val: any) {
  if (editingCell.value) editingCell.value.editing = val
}

/** 空值等价判定：null / undefined / '' 视为同一空值 */
function isEmptyValue(v: any): boolean {
  return v === null || v === undefined || v === ''
}

/** 提交（失焦 / Enter）：值有变化才写入并触发 cellChange；占位行(__ghost)提升为真实行回写父数组 */
function commitEdit(record: any) {
  const cell = editingCell.value
  if (!cell) return
  editingCell.value = null
  selectOpen.value = false
  const unchanged = cell.original === cell.editing || (isEmptyValue(cell.original) && isEmptyValue(cell.editing))
  if (!unchanged) {
    if (record && record.__ghost && !dataSourceModel.value.includes(record)) {
      delete record.__ghost
      dataSourceModel.value.push(record)
    }
    updateCell(record, cell.fieldKey, cell.editing)
  }
}

/** 取消（Esc）：丢弃暂存值，record 未变无需回滚 */
function cancelEdit() {
  editingCell.value = null
  selectOpen.value = false
}

/** 编辑器键盘：Enter=保存并跳下一行同列（连续录入），Esc=取消 */
function handleEditorKeydown(e: KeyboardEvent, record: any, rowIndex: number, col: DetailColumnConfig) {
  if (e.key === 'Escape') {
    e.preventDefault()
    cancelEdit()
    ;(e.target as HTMLElement | null)?.blur?.()
  } else if (e.key === 'Enter') {
    e.preventDefault()
    commitEdit(record)
    const next = displayRows.value[rowIndex + 1]
    if (next && !next._isEmptyRow && isEditableText(col, next)) {
      enterEdit(rowIndex + 1, col, next)
    }
  }
}

/** 简单表达式求值（支持数学运算和常用函数） */
function evaluateFormula(formula: string, row: any): any {
  try {
    const expr = formula.replace(/\{(\w+)\}/g, (_, key) => {
      const val = row[key]
      return val === null || val === undefined || val === '' ? '0' : String(val)
    })
    // 安全求值：仅允许数字、运算符、括号和安全的数学函数
    const safeFunctions = ['Math.round', 'Math.floor', 'Math.ceil', 'Math.abs', 'Math.max', 'Math.min', 'Math.pow', 'Math.sqrt']
    let safeExpr = expr
    safeFunctions.forEach(fn => {
      const shortName = fn.split('.')[1]
      safeExpr = safeExpr.replace(new RegExp(`\\b${shortName}\\b`, 'g'), fn)
    })
    if (/^[\d\s+\-*/.(),%<>=!&|?:a-zA-Z]+$/.test(safeExpr)) {
      return new Function(`return (${safeExpr})`)()
    }
    return null
  } catch {
    return null
  }
}

// 提示：单元格"回车跳转/连续录入"已由 handleEditorKeydown 统一处理

// ═══ 打开选择弹窗 ═══
function handleOpenSelectModal(record: any, rowIndex: number, fieldKey: string) {
  emit('openSelectModal', record, rowIndex, fieldKey)
}

/** 获取查看模式的显示值 */
function getCellDisplayValue(col: DetailColumnConfig, record: any): string {
  const raw = record[col.key]
  if (raw === undefined || raw === null || raw === '') {
    // 即使值为空，如果有 formatter 也调用它
    if (col.formatter) {
      return col.formatter(raw, record)
    }
    return ''
  }

  // 使用 formatter
  if (col.formatter) {
    return col.formatter(raw, record)
  }

  if (col.type === 'select' && (col.options || col.optionsField)) {
    const opt = resolveColumnOptions(col, record).find(o => String(o.value) === String(raw))
    return opt?.label || String(raw)
  }
  if (col.type === 'number') {
    return typeof raw === 'number' ? raw.toFixed(col.precision ?? 2) : String(raw)
  }
  return String(raw)
}

// ═══ 展开/收起 ═══
/**
 * 把展开状态告诉**宿主布局**（CategoryListLayout / DocCenterLayout / BillTableList / VxeTableList …），
 * 让宿主自动收起自己表格下方的区域（分页栏 / 页脚区），表格才能真正长高占满。
 * ⚠️ 组件级（踩过的坑）：此前只 emit('expand-change') 交给业务页处理，而绝大多数列表页根本没接这个事件，
 *    于是点「表格展开显示」只切了个没有视觉效果的 class —— 下面的分页整天占着位置，按钮形同摆设。
 * ⚠️ 用冒泡 DOM 事件而不是 provide/inject：插槽内容的注入链取决于「定义插槽的页面」而不是「渲染它的布局」，
 *    跨层级不可靠；DOM 事件严格按真实 DOM 树冒泡，且天然覆盖 BillTableList 这类多一层包裹的场景。
 */
function notifyHostExpand(value: boolean) {
  tableContainerRef.value?.dispatchEvent(
    new CustomEvent('table-expand-change', { detail: value, bubbles: true })
  )
}

function toggleExpand() {
  expanded.value = !expanded.value
  emit('expand-change', expanded.value)
  notifyHostExpand(expanded.value)
}

/** 锁定列（系统列，不参与列配置）：行号 / 勾选 / 操作 */
const slotBag = useSlots() as Record<string, unknown>

/**
 * 解析「操作列」要渲染的插槽名。
 *
 * 🔴 历史坑（2026-09-20 修）：本组件原先只认 `col.slotName || 'actionCell'`，且**没有** `action` 回退；
 * 而全站有 **38 个页面**写的是 `#action`（列定义里也没有 `slotName`）→
 * 这些页面的**操作列整列空白**（表现为"没有编辑/删除按钮"，会被误判成权限问题，
 * 实际是插槽名不匹配 —— 实测 role 页操作列单元格渲染为空，而权限码与 v-permission 均正常）。
 * 现按「显式 slotName → actionCell → action」优先级回退，两种写法都兼容。
 */
function resolveActionSlotName(col: { slotName?: string } | null | undefined): string {
  if (col?.slotName) return col.slotName
  if (slotBag.actionCell) return 'actionCell'
  if (slotBag.action) return 'action'
  return 'actionCell'
}

const LOCKED_COLUMNS = ['rowNo', 'checkbox', 'action']
function isLockedColumn(key: string): boolean {
  return LOCKED_COLUMNS.includes(key)
}

// ═══ 列设置 ═══
function onColSettingChange() {
  columnSettings.splice(0, 0) // force reactivity
  saveStoredSettings(STORAGE_KEY.value, columnSettings)
}

function onGlobalSettingChange() {
  globalSettings.splice(0, 0) // force reactivity
  saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
  saveGlobalSettingsToServer()
}

/** 全局列配置落后端（仅在页面显式传入 global-config-key 时启用，跨浏览器/终端生效） */
async function saveGlobalSettingsToServer() {
  if (!props.globalConfigKey || globalSettings.length === 0) return
  try {
    const { userPageConfigApi } = await import('@/api/erp')
    await userPageConfigApi.save('col-config', props.globalConfigKey, JSON.stringify([...globalSettings]))
  } catch (e) {
    console.warn('全局列配置保存失败:', e)
  }
}

/** 从后端加载全局列配置（失败降级 localStorage） */
async function loadGlobalSettingsFromServer() {
  if (!props.globalConfigKey) return
  try {
    const { userPageConfigApi } = await import('@/api/erp')
    const raw: any = await userPageConfigApi.get('col-config', props.globalConfigKey)
    if (raw && typeof raw === 'string') {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed) && parsed.length > 0) {
        const storedMap = new Map(parsed.map((s: any) => [s.key, s]))
        const merged = leafColumns.value.map(col => {
          const storedCol: any = storedMap.get(col.key)
          const title = configTitle(col)
          return storedCol
            ? { key: col.key, title, displayName: storedCol.displayName || title, visible: storedCol.visible ?? !col.defaultHidden, width: storedCol.width || col.width || 100, fixed: storedCol.fixed || col.fixed || '' }
            : { key: col.key, title, displayName: title, visible: !col.defaultHidden, width: col.width || 100, fixed: col.fixed || '' }
        })
        globalSettings.splice(0, globalSettings.length, ...merged)
      }
    }
  } catch (e) {
    console.warn('全局列配置加载失败，使用本地配置:', e)
  }
}

function resetColumnSettings() {
  if (colConfigTab.value === 'personal') {
    columnSettings.splice(0, columnSettings.length, ...defaultSettings.value)
    saveStoredSettings(STORAGE_KEY.value, columnSettings)
  } else {
    globalSettings.splice(0, globalSettings.length, ...defaultSettings.value)
    saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
    saveGlobalSettingsToServer()
  }
}

// ═══ 拖拽排序 ═══
let dragIndex = -1

function onDragStart(index: number) {
  dragIndex = index
}

function onDragOver(index: number) {
  if (dragIndex === -1 || dragIndex === index) return
  const target = colConfigTab.value === 'personal' ? columnSettings : globalSettings
  const item = target.splice(dragIndex, 1)[0]
  target.splice(index, 0, item)
  dragIndex = index
}

function onDrop(_index: number) {
  dragIndex = -1
  // 保存拖拽后的顺序
  if (colConfigTab.value === 'personal') {
    saveStoredSettings(STORAGE_KEY.value, columnSettings)
  } else {
    saveStoredSettings(GLOBAL_STORAGE_KEY.value, globalSettings)
    saveGlobalSettingsToServer()
  }
}
</script>

<style scoped>
.bill-detail-table {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  position: relative;
}

/* ═══ Loading 遮罩 ═══ */
.table-loading-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  z-index: 10;
  font-size: 13px;
  color: #666;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f3f3;
  border-top: 2px solid #1890ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ═══ 空数据提示 ═══ */
.table-empty-text {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #999;
  font-size: 13px;
}

/* ═ 表格容器 ═══
 * ⚠️ 父容器链必须满足以下条件，sticky 表头才能正常工作：
 *   1. 每层都需要 overflow: hidden（ containment ）
 *   2. 每层都需要 display: flex; flex-direction: column（让 flex: 1 生效）
 *   3. 不要给 .spreadsheet-table 设置固定 maxHeight 内联样式，
 *      否则会破坏 flex 高度分配，导致滚动极值时表头 1px 跳动。
 *      maxHeight 只在展开模式（70vh）或外部显式传入 > 0 时才应用。
 */
.spreadsheet-table {
  /* ⚠️ 必须用 height:0 + flex-grow:1，不能用 flex:1。
   * flex:1 的 flex-basis:0% 在某些浏览器中不能可靠地创建
   * 有界高度，导致 overflow:auto 无法触发滚动（展开时尤其明显）。
   * height:0 强制元素从零高度开始，flex-grow:1 增长填满可用空间，
   * 保证高度始终等于 flex 分配的空间 → overflow:auto 产生滚动。
   */
  height: 0;
  flex-grow: 1;
  overflow: auto;          /* 唯一的滚动容器，sticky th 相对它定位 */
  border: 1px solid #d9d9d9;
  border-bottom: none;
}

/* ⚠️ 必须用 separate，不能用 collapse！
 * border-collapse: collapse + position: sticky 有两个严重问题：
 *   1. 折叠边框在 thead/tbody 之间共享，吸顶时边框跟着 tbody 滚走，
 *      表头和表体之间出现"分隔线消失"的视觉 bug。
 *   2. 折叠边框居中于单元格边缘，导致 sticky top:0 的定位基准
 *      与初始位置有 0.5px 偏移，滚动时表头出现 1px 跳动。
 * separate + border-spacing: 0 完美避开这两个问题。
 */
.ss-grid {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: auto;
  font-size: 13px;
}

/* ─── 表头 ── */
.ss-grid thead {
  background: #fafafa;
}
/* ⚠️ 边框策略：只画 right + bottom，不画 top + left。
 * 原因：separate 模式下相邻单元格边框不折叠，如果四边都画会出现 2px 双线。
 * 只画 right + bottom 可保证任意两格之间恰好 1px 线条：
 *   - 左边的格子画 right，右边的格子不画 left → 合计 1px
 *   - 上面的格子画 bottom，下面的格子不画 top → 合计 1px
 * 外框由 .spreadsheet-table 容器的 border 提供（top/left/right），
 * 底部由最后一行的 border-bottom 提供。
 */
.ss-grid th {
  background: #fafafa;
  border-right: 1px solid #d9d9d9;
  border-bottom: 2px solid #b0b0b0;
  padding: 0 6px;
  text-align: center;
  font-weight: 600;
  color: #262626;
  font-size: 13px;
  height: 32px;
  box-sizing: border-box;
  position: sticky;
  top: 0;
  z-index: 10;
  white-space: nowrap;
  vertical-align: middle;
  user-select: none;
}

.ss-grid td {
  border-right: 1px solid #d9d9d9;
  border-bottom: 1px solid #d9d9d9;
  padding: 0;
  height: 28px;
  vertical-align: middle;
}

/* ─── 分组表头：第二行吸顶偏移到第一行下方（第一行 th 高 32px） ─── */
.ss-grid thead.ss-grouped tr:nth-child(2) th {
  top: 32px;
  border-bottom: 2px solid #b0b0b0;
}
/* 组标题与子表头之间用细线分隔（仅 colspan 的组标题格，rowspan 叶子格保持表头整体下边线） */
.ss-grid thead.ss-grouped tr:nth-child(1) th[colspan] {
  border-bottom: 1px solid #d9d9d9;
}

/* 分组标题单元格（无对应数据列，仅做列分组） */
.ss-grid th.ss-header-group {
  color: #262626;
  background: #f5f5f5;
}

/* 最后一列不画右边线，容器提供右边框 */
.ss-grid th:last-child:not(.ss-filler-col),
.ss-grid td:last-child:not(.ss-filler-col) {
  border-right: none;
}

/* 填充列：th 保留底边框和背景色，仅移除右边框 */
.ss-grid th.ss-filler-col {
  border-right: none !important;
  background: #fafafa;
}
/* 填充列：td 完全无边框 */
.ss-grid td.ss-filler-col {
  border: none !important;
  padding: 0 !important;
  cursor: default;
  min-width: 0;
}

/* 左对齐列：左侧 5px 间距 */
.ss-grid td:not(.ss-cell-center):not(.ss-cell-right):not(.ss-cell-number):not(.ss-cell-action):not(.ss-cell-checkbox) {
  padding-left: 5px;
}

/* 右对齐列（数字列 + 显式右对齐）：右侧 5px 间距 */
.ss-grid td.ss-cell-number,
.ss-grid td.ss-cell-right {
  padding-right: 5px;
}

/* ⚠️ 数据行默认白底：冻结列用 background:inherit 取行底色，
   若行本身透明，横向滚动时滚动列内容会从冻结列下方透出（视觉穿插）。 */
.ss-row td {
  background: #fff;
}

.ss-row:hover td {
  background: #f5f7fa;
}

/* 空行样式 */
.ss-empty-row td {
  background: #fafafa;
}

.ss-empty-cell {
  display: block;
  height: 28px;
}

.ss-row-no {
  display: block;
  text-align: center;
  color: #8c8c8c;
  font-size: 11px;
  line-height: 28px;
}

/* ─── rowNo 列：齿轮设置按钮 ─── */
.ss-header-settings {
  cursor: pointer;
  text-align: center;
  padding: 0 !important;
}

.th-settings-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ff4d4f;
  font-size: 15px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 3px;
  transition: background 0.2s;
}

.th-settings-btn:hover {
  background: rgba(255, 77, 79, 0.08);
}

/* ─── 列标题文字 ─── */
.th-title {
  display: inline;
  margin-right: 4px;
}

/* ─── 表头帮助图标（对标 ql361 表头 ⓘ） ─── */
.th-help-icon {
  font-size: 11px;
  color: #8c8c8c;
  margin-left: 2px;
  vertical-align: middle;
  cursor: help;
}

/* ─── 扫描枪标签 + 开关 ─── */
.th-scan-label {
  font-size: 11px;
  color: #8c8c8c;
  margin-right: 4px;
  font-weight: 400;
}

.th-scan-switch {
  margin-right: 2px;
}

/* ─── 排序图标 ─── */
.th-sort-icon {
  font-size: 10px;
  color: #bfbfbf;
  cursor: pointer;
  margin-left: 2px;
  position: relative;
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

.th-sort-icon:hover {
  color: #1890ff;
}



.sort-asc {
  color: #1890ff;
}

.sort-desc {
  color: #1890ff;
}

.sort-neutral {
  display: inline-flex;
  flex-direction: column;
  line-height: 0.8;
  font-size: 8px;
  color: #bfbfbf;
}
.sort-neutral .anticon {
  font-size: 9px;
}

.sort-order-badge {
  font-size: 10px;
  margin-left: 1px;
  font-weight: bold;
}

/* ═══ 点击编辑单元格（非编辑态文本，点击进入编辑框） ═══ */
.ss-cell-editable {
  display: inline-block;
  width: 100%;
  min-height: 18px;
  line-height: 20px;
  padding: 0 2px;
  cursor: pointer;
  border-radius: 2px;
}
.ss-cell-editable:hover {
  background: #e6f7ff;
  outline: 1px solid #91d5ff;
}
.ss-cell-editable:focus-visible {
  outline: 1px solid #1890ff;
}
.ss-cell-placeholder {
  color: #bfbfbf;
}

/* ═══ 原生输入框样式（无嵌套感） ═══ */
.ss-native-input {
  width: 100%;
  height: 100%;
  border: none !important;
  outline: none !important;
  background: transparent !important;
  font-size: 13px !important;
  color: #262626;
  padding: 0 6px !important;
  box-sizing: border-box;
  font-family: inherit;
  line-height: 28px;
}

.ss-native-input:focus {
  background: #e6f7ff !important;
}

.ss-native-input::placeholder {
  color: #bfbfbf;
}

.ss-native-select {
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
}

.ss-native-number {
  text-align: right;
}

.ss-native-date {
  cursor: pointer;
}

/* 无数据日期单元格：纯空白占位（无内容、无占位符、无日期图标） */
.ss-date-empty {
  display: inline-block;
  min-width: 100px;
  min-height: 22px;
}

/* ─── 数字单元格右对齐 ─── */
.ss-cell-number .ss-native-input {
  text-align: right;
}

/* ─── 固定列 ─── */
.ss-fixed-left {
  position: sticky;
  left: 0;
  z-index: 1;
  background: inherit;
}

.ss-fixed-right {
  position: sticky;
  right: 0;
  z-index: 1;
  background: inherit;
}

/* ⚠️ 冻结表头层级必须高于普通表头（普通 th = sticky top:0 / z-index:10），
   否则横向滚动时冻结表头会被普通表头盖住（表现为"表头冻结失效、单元格冻结正常"）；
   同时显式给不透明底色，避免滚动内容从表头透出。 */
.ss-grid th.ss-fixed-left,
.ss-grid th.ss-fixed-right {
  z-index: 12;
  background: #fafafa;
}

.ss-row:hover .ss-fixed-left,
.ss-row:hover .ss-fixed-right {
  background: #f5f7fa !important;
}

/* 查看模式文本 */
.ss-cell-text {
  display: block;
  padding: 0 6px;
  font-size: 13px;
  color: #262626;
  line-height: 28px;
}

/* boolean 类型：复选框单元格 */
.ss-bool-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  cursor: pointer;
}
.ss-bool-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1890ff;
}
.ss-bool-display {
  text-align: center;
  font-weight: 600;
  color: #52c41a;
}

/* ═══ 复选框样式 ═══ */
.ss-checkbox {
  width: 14px;
  height: 14px;
  cursor: pointer;
  margin: 0 auto;
  display: block;
}

.ss-checkbox-header {
  margin: 0 auto;
}

/* ═══ 按钮列样式 ═══ */
.ss-button-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 100%;
}

/* ─── 操作列内容居中 ── */
.ss-grid td.ss-cell-action {
  text-align: center;
}

/* ─── 统一 Ant Design 按钮字体为 13px ─── */
:deep(.ant-btn) {
  font-size: 13px !important;
}
:deep(.ant-btn.ant-btn-sm) {
  height: 24px;
  padding: 0 6px;
}
:deep(.ant-btn.ant-btn-link) {
  height: auto;
  line-height: 1.4;
  padding: 0 4px;
}
.ss-grid td.ss-cell-action :deep(.ant-space) {
  display: inline-flex;
  justify-content: center;
  width: 100%;
}

/* ═══ 合计行（tfoot） ═══ */
.ss-summary-tr td {
  background: #fafafa;
  border-right: 1px solid #d9d9d9;
  border-bottom: 1px solid #d9d9d9;
  padding: 0 6px;
  height: 30px;
  font-weight: 600;
  font-size: 13px;
  vertical-align: middle;
}

.summary-total-label {
  font-weight: 700;
  color: #262626;
  display: block;
  line-height: 30px;
}

.summary-highlight {
  color: #ff4d4f;
  font-weight: 700;
  text-align: right;
}

/* ═══ 展开/收起 ═══ */
.detail-expand {
  text-align: center;
  padding: 4px;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-top: none;
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  z-index: 10;
}

.detail-expand :deep(.ant-btn-link) {
  color: #1890ff;
  font-size: 12px;
}

/* ═══ 展开模式：隐藏边框，可滚动 ═══ */
.table-expanded {
  /* 不添加 overflow-y: auto，保持 .spreadsheet-table 为唯一滚动容器
     这样 sticky thead 才能正常工作 */
  min-height: auto;
}

.table-expanded .spreadsheet-table {
  border: none;
}

.table-expanded .detail-expand {
  border: none;
  background: transparent;
}

/* ═══ 列设置面板 ═══ */
.col-settings-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 60px 20px 0;
}

.col-settings-panel {
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  width: 360px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  z-index: 1001;
}

.col-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 600;
  color: #262626;
}

.col-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 6px 0;
}

.col-setting-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  font-size: 12px;
  cursor: default;
  transition: background 0.15s;
}

.col-setting-row:hover {
  background: #f5f7fa;
}

.col-setting-ghost {
  opacity: 0.45;
}

.col-setting-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: grab;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #262626;
}

.drag-handle {
  color: #bfbfbf;
  cursor: grab;
  font-size: 13px;
  user-select: none;
}

.col-freeze-select {
  width: 80px;
  height: 24px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  padding: 0 4px;
  color: #595959;
  background: #fff;
  outline: none;
  flex-shrink: 0;
}

.col-freeze-select:focus {
  border-color: #1890ff;
}

.col-width-input {
  width: 55px;
  height: 24px;
  font-size: 11px;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  padding: 0 6px;
  text-align: right;
  outline: none;
  flex-shrink: 0;
}

.col-width-input:focus {
  border-color: #1890ff;
}

.col-panel-footer {
  padding: 8px 14px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

/* 为Modal样式的列设置面板新增样式 */
.col-settings-panel-modal {
  max-height: 55vh;
  overflow-y: auto;
}

/* 列配置标签页样式 */
.col-config-tabs :deep(.ant-tabs-nav) {
  padding: 0 14px;
  margin-bottom: 0;
}
.col-config-tabs :deep(.ant-tabs-content-holder) {
  border: 1px solid #d9d9d9;
  border-top: none;
  border-radius: 0 0 4px 4px;
}
.col-tab-tip {
  font-size: 12px;
  color: #fa8c16;
  padding: 8px 14px 4px;
}
.col-seq {
  width: 24px;
  text-align: center;
  color: #8c8c8c;
  font-size: 11px;
  flex-shrink: 0;
}
.col-display-name {
  flex: 0 0 120px;
  padding: 2px 6px;
  font-size: 12px;
  color: #595959;
  background: #fafafa;
  border-radius: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.col-display-name-input {
  flex: 0 0 120px;
  height: 24px;
  font-size: 12px;
}

/* 分页器样式（继承自 ColumnConfigTable） */
.standard-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 0;
  background: #fff;
  border-top: 1px solid #d9d9d9;
  margin-top: -1px;
  z-index: 1;
  flex-shrink: 0; /* 防止在 flex 布局中被压缩 */
}

.standard-pagination :deep(.ant-pagination) {
  margin-right: 16px;
}

.standard-pagination .pagination-info {
  color: #999;
  font-size: 13px;
  margin-left: 16px;
}
</style>