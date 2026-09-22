<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
      title="条款字典维护"
    >
      <template #headerExtra>
        <a-space :size="8">
          <a-button
            size="small"
            @click="goBack"
          >
            <RollbackOutlined /> 返回协议列表
          </a-button>
          <a-button
            size="small"
            :loading="loading"
            @click="fetchData"
          >
            <ReloadOutlined /> 刷新
          </a-button>
        </a-space>
      </template>

      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <a-button
            v-permission="'agreement:platform:term-option:manage'"
            type="primary"
            size="small"
            @click="openCreate"
          >
            <PlusOutlined /> 新增条款选项
          </a-button>
        </template>

        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">条款类别</span>
                    <a-select
                      v-model:value="filterTermCode"
                      size="small"
                      allow-clear
                      placeholder="全部"
                      :options="termCodeOptions"
                    />
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="keyword"
                    placeholder="选项名称 / 含义说明"
                    size="small"
                    allow-clear
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="fetchData"
                    >
                      查询
                    </a-button>
                  </div>
                  <div class="search-field-item search-action-item">
                    <a-button
                      size="small"
                      @click="handleReset"
                    >
                      重置
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template #table>
          <div class="dict-hint">
            <InfoCircleOutlined />
            <span>
              平台只定义「有哪些选项、各自什么含义」，<strong>不设默认值</strong>、也不替双方决定选哪一项 ——
              给了默认值就等于平台替双方做了决定。协议里的条款只能从这里选，选的时候双方能看到「选它系统会怎么执行」。
            </span>
          </div>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'agreement-term-option-table-columns'"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :show-delete="false"
              :selectable="false"
              row-key="rowKey"
            >
              <template #termCodeCell="{ record }">
                {{ record.termName || record.termCode }}
              </template>
              <template #requiredCell="{ record }">
                <a-tag
                  v-if="record.required"
                  color="red"
                >
                  必填
                </a-tag>
                <a-tag
                  v-else
                  color="default"
                >
                  可选
                </a-tag>
              </template>
              <template #semanticsCell="{ record }">
                <span :title="record.semantics">{{ record.semantics || '（未填写说明）' }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 0 ? 'red' : 'green'">
                  {{ record.status === 0 ? '已停用' : '启用中' }}
                </a-tag>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    v-permission="'agreement:platform:term-option:manage'"
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    v-permission="'agreement:platform:term-option:manage'"
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 新增 / 编辑选项 ═══ -->
    <a-modal
      v-model:open="formVisible"
      :title="formMode === 'create' ? '新增条款选项' : '修改条款选项'"
      :width="680"
      :mask-closable="false"
      :confirm-loading="submitting"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form
        layout="vertical"
        class="term-form"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="条款类别"
              required
            >
              <a-select
                v-model:value="form.termCode"
                show-search
                placeholder="选择已有类别，或直接输入新类别名"
                :options="termCodeOptions"
                :disabled="formMode === 'edit'"
              />
              <div class="field-hint">
                同类别的选项会归到协议详情页的同一个下拉里，例如「退货政策」一项就是一个类别。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="选项名称"
              required
            >
              <a-input
                v-model:value="form.optionLabel"
                :maxlength="64"
                placeholder="双方在下拉里看到的文字，例如「退回运费由供货方承担」"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="选项编码">
              <a-input
                v-model:value="form.optionCode"
                :maxlength="32"
                placeholder="系统内部标识，字母或下划线；留空由系统生成"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="含义说明（选它系统会怎么执行）"
              required
            >
              <a-textarea
                v-model:value="form.semantics"
                :rows="3"
                :maxlength="500"
                show-count
                placeholder="例如：发生退货时，退回运费由供货方承担并从本期结算中扣除"
              />
              <div class="field-hint">
                这段文字是给双方看的条款说明书，也是将来举证时的要点，请写清「谁承担、怎么扣、什么时候扣」。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="需要填写的参数">
              <a-input
                v-model:value="form.needsParam"
                :maxlength="32"
                placeholder="例如：分账比例；没有参数就留空"
              />
              <div class="field-hint">
                填了参数名的选项，签协议时必须同时把参数值填上，协议才能生效。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="是否必填">
              <a-switch
                v-model:checked="form.required"
                checked-children="必填"
                un-checked-children="可选"
              />
              <div class="field-hint">
                责任划分一类的条款建议设为必填，逼双方在签约时说清楚。
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="排序">
              <a-input-number
                v-model:value="form.sort"
                :min="0"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用状态">
              <a-radio-group v-model:value="form.status">
                <a-radio :value="1">
                  启用
                </a-radio>
                <a-radio :value="0">
                  停用
                </a-radio>
              </a-radio-group>
              <div class="field-hint">
                停用只影响以后新签的协议，历史协议已保存的快照不受影响。
              </div>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 条款字典维护（平台侧）
 *
 * 权限：查看用 `agreement:term-option:list`，新增/修改/删除用 `agreement:platform:term-option:manage`。
 * 平台在这里**只定义选项**（有哪些选项、各自什么含义、是否必填），刻意**不提供默认值** ——
 * 给默认值就等于平台替双方决定了责任划分（见 DOMAIN-MODEL §3.4.4d / §十二 裁定④）。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, RollbackOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import {
  agreementApi,
  type AgreementTermOption,
  type AgreementTermOptionGroup
} from '@/api/agreement'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const rawOptions = ref<AgreementTermOption[]>([])
const filterTermCode = ref<string | undefined>(undefined)
const keyword = ref('')

const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 140, fixed: 'right', slotName: 'actionCell' },
  { title: '条款类别', key: 'termCode', field: 'termCode', width: 160, type: 'slot', slotName: 'termCodeCell' },
  { title: '选项名称', key: 'optionLabel', field: 'optionLabel', width: 220 },
  { title: '含义说明（系统怎么执行）', key: 'semantics', field: 'semantics', width: 420, type: 'slot', slotName: 'semanticsCell' },
  { title: '需要填参数', key: 'needsParam', field: 'needsParam', width: 120 },
  { title: '是否必填', key: 'required', field: 'required', width: 100, align: 'center', type: 'slot', slotName: 'requiredCell' },
  { title: '排序', key: 'sort', field: 'sort', width: 80, align: 'right' },
  { title: '状态', key: 'status', field: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' }
]

/** 类别下拉：从已有字典里归并出类别清单 */
const termCodeOptions = computed(() => {
  const map = new Map<string, string>()
  for (const o of rawOptions.value) {
    if (o.termCode && !map.has(o.termCode)) map.set(o.termCode, o.termName || o.termCode)
  }
  return Array.from(map.entries()).map(([code, name]) => ({ label: name, value: code }))
})

const tableData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return rawOptions.value
    .filter(o => !filterTermCode.value || o.termCode === filterTermCode.value)
    .filter(o => {
      if (!kw) return true
      return String(o.optionLabel || '').toLowerCase().includes(kw)
        || String(o.semantics || '').toLowerCase().includes(kw)
    })
    .map((o, idx) => ({ ...o, rowKey: `${o.termCode}-${o.optionCode}-${idx}` }))
})

/** 字典接口按类别分组下发，这里摊平成一行一个选项，便于平台维护 */
async function fetchData() {
  loading.value = true
  try {
    const res: any = await agreementApi.listTermOptionGroups()
    const raw = res?.data ?? res
    let groups: AgreementTermOptionGroup[] = []
    if (Array.isArray(raw)) {
      groups = raw.map((g: any) => ({ ...g, options: g.options || g.children || [] }))
    } else if (raw && typeof raw === 'object') {
      groups = Object.keys(raw).map(code => {
        const opts: any[] = raw[code] || []
        return { termCode: code, termName: opts[0]?.termName, options: opts }
      })
    }
    const flat: AgreementTermOption[] = []
    for (const g of groups) {
      for (const o of (g.options || [])) {
        flat.push({ ...o, termCode: o.termCode || g.termCode, termName: o.termName || g.termName })
      }
    }
    rawOptions.value = flat
  } catch (error: any) {
    console.warn('[条款字典] 加载失败', error)
    rawOptions.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filterTermCode.value = undefined
  keyword.value = ''
}

// ── 新增 / 编辑 ──
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | string | null>(null)

const form = reactive({
  termCode: undefined as string | undefined,
  optionCode: '',
  optionLabel: '',
  semantics: '',
  needsParam: '',
  required: false,
  sort: 0,
  status: 1
})

function openCreate() {
  formMode.value = 'create'
  editingId.value = null
  form.termCode = filterTermCode.value
  form.optionCode = ''
  form.optionLabel = ''
  form.semantics = ''
  form.needsParam = ''
  form.required = false
  form.sort = 0
  form.status = 1
  formVisible.value = true
}

function openEdit(record: AgreementTermOption) {
  formMode.value = 'edit'
  editingId.value = record.id
  form.termCode = record.termCode
  form.optionCode = record.optionCode
  form.optionLabel = record.optionLabel
  form.semantics = record.semantics || ''
  form.needsParam = record.needsParam || ''
  form.required = !!record.required
  form.sort = record.sort ?? 0
  form.status = record.status ?? 1
  formVisible.value = true
}

async function handleSubmit() {
  if (!form.termCode) {
    message.warning('请先选择或填写条款类别')
    return
  }
  if (!form.optionLabel.trim()) {
    message.warning('请填写选项名称，这是双方在下拉里看到的文字')
    return
  }
  if (!form.semantics.trim()) {
    message.warning('请填写含义说明：选了这一项，系统会怎么执行')
    return
  }
  const body: Partial<AgreementTermOption> = {
    termCode: form.termCode,
    optionCode: form.optionCode.trim() || undefined,
    optionLabel: form.optionLabel.trim(),
    semantics: form.semantics.trim(),
    needsParam: form.needsParam.trim() || null,
    required: form.required,
    sort: form.sort,
    status: form.status
  }
  submitting.value = true
  try {
    if (formMode.value === 'create') {
      await agreementApi.createTermOption(body)
      message.success('选项已新增')
    } else if (editingId.value !== null) {
      await agreementApi.updateTermOption(editingId.value, body)
      message.success('选项已保存')
    }
    formVisible.value = false
    fetchData()
  } catch (error: any) {
    console.warn('[条款字典] 保存失败', error)
  } finally {
    submitting.value = false
  }
}

function handleDelete(record: AgreementTermOption) {
  Modal.confirm({
    title: '删除条款选项',
    content: `确定删除「${record.optionLabel}」吗？删除后新协议不再能选它，历史协议已保存的快照不受影响。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await agreementApi.deleteTermOption(record.id)
        message.success('已删除')
        fetchData()
      } catch (error: any) {
        console.warn('[条款字典] 删除失败', error)
      }
    }
  })
}

function goBack() {
  router.push('/agreement')
}

const handleError = (error: Error) => {
  console.error('[条款字典] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchData)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0, 0, 0, 0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) {
  border: none !important; border-radius: 0 !important; box-shadow: none !important;
  padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center;
}
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.dict-hint {
  display: flex; align-items: flex-start; gap: 8px; flex-shrink: 0;
  font-size: 12px; line-height: 1.7; color: #595959;
  background: #e6f7ff; border-bottom: 1px solid #91d5ff; padding: 8px 16px;
}
.dict-hint :deep(.anticon) { color: #1890ff; margin-top: 3px; }
.term-form :deep(.ant-form-item) { margin-bottom: 12px; }
.field-hint { font-size: 12px; color: #999; line-height: 1.5; margin-top: 2px; }
</style>
