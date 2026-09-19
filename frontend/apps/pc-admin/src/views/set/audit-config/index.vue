<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        审核设置（设置 → 系统配置 → 审核设置，菜单 80622 / set:audit-config）
        · 对标 ql361「审核设置」实测形态（docs/Yh-Spec/手动整理对标开发文档/设置模块/审核设置开发文档.md §8.1）：
          顶部一条说明条 + 三列规则矩阵「单据 | 审核设置 | 摘要」，共 16 类单据，每行一个「设置」按钮
        · 16 类单据与 7 类审核条件均逐字取自 ql361 实测，后端 AuditRuleCatalog 为唯一真源（本页不硬编码单据清单）
        · 「摘要」由后端按规则拼装（条件描述 + 时提交给[审批人]审核;），本页只做展示 —— 避免两处拼字符串各说各话
        · 规则落库 sys_audit_rule（租户 × 单据类型），保存后**回读**（摘要必须以服务端口径为准）
        ⚠️ 不加 PageConfigPanel：ql361 本页实测 pageConfig.found = false（无「页面配置」入口），
           按「对标有才加」的口径不加；列配置齿轮仍保留（本系统金标准项，走表头 rowNo 列）
      -->
      <div class="audit-config-page">
        <!-- 多审批人语义说明条：文案逐字取自 ql361 页面顶部提示条（原「接口尚未开放」的错误文案已删除） -->
        <a-alert
          type="warning"
          show-icon
          class="rule-tip"
          message="当同一个审核条件设置多个审核人时，其中任何一个审核人审核之后不再需要其他职员审核"
          description="配置「审核条件 → 审批人」后，命中条件的单据将提交给对应审批人审核；「摘要」列为已配置规则的可读文本。"
        />

        <CategoryListLayout
          :tabs="[]"
          :show-category-panel="false"
          :show-table-footer="true"
          @search="handleSearch"
        >
          <!-- ═══ 工具栏右侧：刷新（对标本页无工具栏/无打印，仅保留金标准的刷新） ═══ -->
          <template #toolbar-right>
            <a-space :size="8">
              <a-button
                size="small"
                :loading="loading"
                @click="fetchList"
              >
                <ReloadOutlined /> 刷新
              </a-button>
            </a-space>
          </template>

          <!-- ═══ 查询区：横向自适应网格（禁止纵向单列） ═══ -->
          <template #search-fields>
            <div class="search-area">
              <div
                ref="gridRef"
                class="search-grid"
              >
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchForm.docName"
                    placeholder="单据名称"
                    size="small"
                    allow-clear
                    @press-enter="handleSearch"
                  />
                </div>
                <div class="search-field-item">
                  <a-select
                    v-model:value="searchForm.configured"
                    placeholder="配置状态"
                    size="small"
                    allow-clear
                    :options="CONFIGURED_OPTIONS"
                    @change="handleSearch"
                  />
                </div>
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

          <!-- ═══ 数据表：三列规则矩阵（列配置齿轮在表头 rowNo 列） ═══ -->
          <template #table>
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="0"
                row-key="docType"
                storage-key="set-audit-config-table-columns"
                global-config-key="set-audit-config-table-columns"
              >
                <!-- 单据（名称逐字取自 ql361 实测） -->
                <template #docNameCell="{ record }">
                  <span>{{ record.docName }}</span>
                </template>

                <!-- 审核设置：行内「设置」按钮（对标：每行一个设置按钮打开配置弹窗） -->
                <template #auditSettingCell="{ record }">
                  <a-button
                    v-if="!record.__ghost"
                    type="link"
                    size="small"
                    @click="openSetting(record)"
                  >
                    设置
                  </a-button>
                </template>

                <!-- 摘要：后端拼装的可读文本；未配置为空（与 ql361 实测一致：16 行中仅 3 行有摘要） -->
                <template #summaryCell="{ record }">
                  <!-- 摘要可能很长（一个单据最多 7 条规则），单元格内省略、悬停看全文 -->
                  <a-tooltip
                    v-if="record.summary"
                    placement="bottom"
                    :title="record.summary"
                  >
                    <span class="summary-text">{{ record.summary }}</span>
                  </a-tooltip>
                  <span
                    v-else-if="!record.__ghost"
                    class="cell-empty"
                  >-</span>
                </template>
              </BillDetailTable>
            </div>
          </template>

          <!-- ═══ 底部：经典分页栏 ═══ -->
          <template #table-footer>
            <StandardPagination
              variant="classic"
              :current="pagination.current"
              :page-size="pagination.pageSize"
              :total="filteredCount"
              :page-size-options="[20, 50, 100]"
              @change="handlePageChange"
            />
          </template>
        </CategoryListLayout>
      </div>
    </PageContainer>

    <!-- ═══ 审核设置弹窗：某类单据的「审核条件 → 审批人集合」（子组件，保存后回调回读） ═══ -->
    <AuditRuleForm
      v-model:open="modalOpen"
      :doc="currentDoc"
      :conditions="conditionCatalog"
      @saved="handleSaved"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import AuditRuleForm from './form.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { auditConfigApi, type AuditDocRule } from '@/api/workflow'

defineOptions({ name: 'SetAuditConfig' })

// ═══ 状态 ═══
const loading = ref(false)
/** 后端返回的 16 行全量数据（行序与单据清单都由后端 AuditRuleCatalog 决定，前端不再硬编码） */
const allRows = ref<AuditDocRule[]>([])
/** 审核条件目录（弹窗编辑用；同样来自后端，避免前后端两套枚举） */
const conditionCatalog = ref<{ value: string; label: string }[]>([])
/** 提交给 BillDetailTable 的当前页数据（组件用 v-model:data-source，故需可写 ref） */
const tableData = ref<AuditDocRule[]>([])

// ═══ 查询条件（横向自适应网格） ═══
const searchForm = reactive<{ docName: string; configured: string | undefined }>({
  docName: '',
  configured: undefined,
})
const CONFIGURED_OPTIONS = [
  { label: '已配置', value: 'yes' },
  { label: '未配置', value: 'no' },
]

const pagination = reactive({ current: 1, pageSize: 20 })

const gridRef = ref<HTMLElement | null>(null)
const actionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(actionRef, gridRef)

// ═══ 列定义：逐字对标 ql361 三列表头（单据 / 审核设置 / 摘要） ═══
// rowNo 列承载表头「列配置」齿轮（本系统金标准项，非对标列）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'docName', title: '单据', type: 'slot', slotName: 'docNameCell', width: 220 },
  { key: 'auditSetting', title: '审核设置', type: 'slot', slotName: 'auditSettingCell', width: 120 },
  { key: 'summary', title: '摘要', type: 'slot', slotName: 'summaryCell' },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res = await auditConfigApi.list()
    allRows.value = res?.list || []
    conditionCatalog.value = res?.conditions || []
  } catch (error: any) {
    console.error('[审核设置] 加载失败', error)
    message.error(error?.response?.data?.message || '审核设置加载失败')
    allRows.value = []
  } finally {
    loading.value = false
  }
}

// ═══ 查询 / 分页（16 类单据为固定清单，前端筛选 + 前端分页） ═══
const filteredRows = computed(() => {
  const keyword = (searchForm.docName || '').trim()
  return allRows.value.filter((row) => {
    if (keyword && !String(row.docName || '').includes(keyword)) return false
    if (searchForm.configured === 'yes' && !row.configured) return false
    if (searchForm.configured === 'no' && row.configured) return false
    return true
  })
})

const filteredCount = computed(() => filteredRows.value.length)

const pagedRows = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return filteredRows.value.slice(start, start + pagination.pageSize)
})

watch(pagedRows, (rows) => {
  tableData.value = [...rows]
}, { immediate: true })

function handleSearch() {
  pagination.current = 1
}

function handleReset() {
  searchForm.docName = ''
  searchForm.configured = undefined
  pagination.current = 1
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
}

// ═══ 设置弹窗（规则编辑与保存都在子组件 form.vue 内，父页只负责打开与回读） ═══
const modalOpen = ref(false)
const currentDoc = ref<AuditDocRule | null>(null)

function openSetting(record: AuditDocRule) {
  currentDoc.value = record
  modalOpen.value = true
}

/** 子组件保存成功后回读：摘要是后端按落库规则拼装的，界面必须以回读结果为准 */
async function handleSaved() {
  await fetchList()
}

function handleError(error: Error) {
  console.error('[审核设置] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchList)
</script>

<style scoped>
/* 外层必须是 flex 纵向容器：说明条固定高度、CategoryListLayout 占满剩余高度 */
.audit-config-page { display: flex; flex-direction: column; flex: 1; min-height: 0; }
/* CategoryListLayout 自带 height:100%，此处改成 flex 占位，避免「说明条 + 100% 高」把页面撑出滚动条 */
.audit-config-page :deep(.category-list-layout) { flex: 1; min-height: 0; height: auto; }
.rule-tip { margin-bottom: 8px; flex-shrink: 0; }

/* 查询区：横向自适应网格（禁止纵向单列） */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 8px 12px; }
.search-field-item { min-width: 150px; }
.search-action-group { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-width: 150px; }

/* BillDetailTable 根元素 flex:1，父级非 flex 纵向容器时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }

.summary-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
.cell-empty { color: #bbb; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-multiple.ant-select-sm .ant-select-selector) { min-height: 28px; }
</style>
