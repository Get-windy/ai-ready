<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        职位管理（人力资源 → 组织管理 → 职位管理，菜单 90006 / hr:position）
        · 左：部门分类树（数据源 sys_department，HR 只读引用，不在此维护部门主数据）
        · 列配置齿轮在数据表表头 rowNo 列（个人配置 / 全局配置）
        · 工具栏：新增岗位 ｜ 页面配置 / 刷新 / 打印(F8) / 导出
        · 查询区（受页面配置控制）：岗位编码 · 岗位名称 · 岗位级别 · 状态
        · 行内：修改 / 删除（有在岗员工时后端拒绝并返回明确 message）
        · 表单：抽屉 + 分区卡片 + 两列栅格（岗位属主数据，非单据，不用 BillFormPage）
        · 编制/在岗两列为后端回填的派生口径，表单不提供「在岗人数」输入入口
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="true"
        category-title="部门"
        :category-editable="false"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :selected-category-id="selectedDeptId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentDeptPath"
        :show-table-footer="true"
        @category-select="handleDeptSelect"
        @category-expand="handleDeptExpand"
      >
        <!-- ═══ 工具栏左侧 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增岗位
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（字段显隐由页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div
                v-if="isQueryVisible('positionCode')"
                class="search-item"
              >
                <span class="search-label">岗位编码</span>
                <a-input
                  v-model:value="searchForm.positionCode"
                  placeholder="请输入岗位编码"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('positionName')"
                class="search-item"
              >
                <span class="search-label">岗位名称</span>
                <a-input
                  v-model:value="searchForm.positionName"
                  placeholder="请输入岗位名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('positionLevel')"
                class="search-item"
              >
                <span class="search-label">岗位级别</span>
                <a-select
                  v-model:value="searchForm.positionLevel"
                  placeholder="全部级别"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="levelOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isQueryVisible('status')"
                class="search-item"
              >
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 120px"
                  allow-clear
                  :options="statusOptions"
                  @change="handleSearch"
                />
              </div>
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
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="hr-position-table-columns"
              global-config-key="hr-position-table-columns"
            >
              <template #nameCell="{ record, column }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record[column?.key ?? 'positionName'] }}</a>
              </template>

              <template #levelCell="{ record, column }">
                <span v-if="!record.__ghost">{{ textOf(POSITION_LEVEL_MAP, record[column?.key ?? 'positionLevel']) }}</span>
              </template>

              <template #vacancyCell="{ record }">
                <span v-if="!record.__ghost">{{ vacancyOf(record) }}</span>
              </template>

              <template #overQuotaCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.overQuota ? 'error' : 'success'"
                >
                  {{ record.overQuota ? '超编' : '正常' }}
                </a-tag>
              </template>

              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="record.status === 1 ? 'success' : 'error'"
                >
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
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
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 岗位档案抽屉（主数据：分区卡片 + 两列栅格） ═══ -->
      <a-drawer
        v-model:open="formOpen"
        :title="formState.id ? '修改岗位' : '新增岗位'"
        :width="680"
        :mask-closable="false"
        :destroy-on-close="true"
        placement="right"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 16 }"
          size="small"
        >
          <FormSection
            title="基本信息"
            tip="岗位编码留空时由号段自动生成；岗位名称必填"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="岗位编码">
                  <a-input
                    v-model:value="formState.positionCode"
                    placeholder="留空自动生成"
                    :disabled="!!formState.id"
                    :maxlength="50"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="岗位名称"
                  name="positionName"
                >
                  <a-input
                    v-model:value="formState.positionName"
                    placeholder="请输入岗位名称"
                    :maxlength="100"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="所属部门">
                  <a-tree-select
                    v-model:value="formState.deptId"
                    :tree-data="deptTreeSelectData"
                    placeholder="请选择所属部门"
                    allow-clear
                    tree-default-expand-all
                    :field-names="{ label: 'categoryName', value: 'id', children: 'children' }"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="岗位级别">
                  <a-select
                    v-model:value="formState.positionLevel"
                    placeholder="请选择岗位级别"
                    :options="levelOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="状态">
                  <a-select
                    v-model:value="formState.status"
                    :options="statusOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="排序">
                  <a-input-number
                    v-model:value="formState.sort"
                    :min="0"
                    :precision="0"
                    style="width: 100%"
                    placeholder="数字越小越靠前"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection
            title="编制管理"
            tip="在岗人数由员工在职情况派生，此处只读"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="编制人数">
                  <a-input-number
                    v-model:value="formState.quotaCount"
                    :min="0"
                    :precision="0"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
              <a-col
                v-if="formState.id"
                :span="12"
              >
                <a-form-item label="在岗人数">
                  <span class="readonly-text">{{ formState.currentCount ?? 0 }}</span>
                </a-form-item>
              </a-col>
              <a-col
                v-if="formState.id"
                :span="12"
              >
                <a-form-item label="编制缺口">
                  <span class="readonly-text">{{ Math.max(0, (formState.quotaCount || 0) - (formState.currentCount || 0)) }}</span>
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>

          <FormSection title="职责说明">
            <a-row :gutter="24">
              <a-col :span="24">
                <a-form-item label="岗位职责">
                  <a-textarea
                    v-model:value="formState.responsibility"
                    placeholder="请输入岗位职责"
                    :rows="3"
                    :maxlength="2000"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="备注">
                  <a-textarea
                    v-model:value="formState.remark"
                    placeholder="请输入备注"
                    :rows="2"
                    :maxlength="500"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </FormSection>
        </a-form>

        <template #footer>
          <a-space>
            <a-button @click="formOpen = false">
              取消
            </a-button>
            <a-button
              type="primary"
              :loading="saving"
              @click="handleSave"
            >
              保存
            </a-button>
          </a-space>
        </template>
      </a-drawer>

      <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        storage-key="hr-position-page-config"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import FormSection from '@/components/FormSection/index.vue'
import { departmentApi } from '@/api/department'
import { hrPositionApi, POSITION_LEVEL_MAP, type HrPosition } from '@/api/hr'

defineOptions({ name: 'HrPositionList' })

// ═══ 状态 ═══
const loading = ref(false)
const exporting = ref(false)
const saving = ref(false)
const tableData = ref<HrPosition[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const levelOptions = Object.entries(POSITION_LEVEL_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

// ═══ 数据表列（默认显示 11 列 + 序号；3 列默认隐藏） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'positionCode', title: '岗位编码', type: 'input', width: 140 },
  { key: 'positionName', title: '岗位名称', type: 'slot', slotName: 'nameCell', width: 160 },
  { key: 'deptName', title: '部门', type: 'input', width: 140 },
  { key: 'positionLevel', title: '岗位级别', type: 'slot', slotName: 'levelCell', width: 100 },
  { key: 'quotaCount', title: '编制人数', type: 'input', width: 100, align: 'right' },
  { key: 'currentCount', title: '在岗人数', type: 'input', width: 100, align: 'right' },
  { key: 'vacancy', title: '空缺', type: 'slot', slotName: 'vacancyCell', width: 90, align: 'right' },
  { key: 'overQuota', title: '是否超编', type: 'slot', slotName: 'overQuotaCell', width: 100 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'right' },
  // ── 默认隐藏 ──
  { key: 'sort', title: '排序', type: 'input', width: 80, defaultHidden: true },
  { key: 'responsibility', title: '岗位职责', type: 'input', width: 240, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
]

// ═══ 查询条件 ═══
const searchForm = reactive({
  positionCode: '',
  positionName: '',
  positionLevel: undefined as number | undefined,
  status: undefined as number | undefined,
})

// ═══ 部门分类树（数据源 sys_department，HR 只读） ═══
const ROOT_DEPT_ID = '0'
const selectedDeptId = ref<string | number>(ROOT_DEPT_ID)
const categoryTreeData = ref<any[]>([])
const deptNodes = ref<any[]>([])
const categoryLoading = ref(false)
const expandedKeys = ref<(string | number)[]>([])
const deptMap = ref<Record<string, string>>({})

const currentDeptPath = computed(() => {
  if (String(selectedDeptId.value) === ROOT_DEPT_ID) return '全部部门'
  return deptMap.value[String(selectedDeptId.value)] || '全部部门'
})
// 表单里的「所属部门」不提供「全部部门」伪根节点
const deptTreeSelectData = computed(() => deptNodes.value)

function flattenDept(nodes: any[], map: Record<string, string>) {
  for (const n of nodes || []) {
    map[String(n.id)] = n.categoryName
    if (n.children?.length) flattenDept(n.children, map)
  }
}

async function loadDeptTree() {
  categoryLoading.value = true
  try {
    const res: any = await departmentApi.getTree()
    const list: any[] = Array.isArray(res) ? res : res?.data || []
    const mapped = list.map((d: any) => ({
      id: d.id,
      categoryName: d.departmentName,
      children: d.children,
    }))
    deptNodes.value = mapped
    categoryTreeData.value = [{ id: ROOT_DEPT_ID, categoryName: '全部部门', children: mapped }]
    const map: Record<string, string> = {}
    flattenDept(mapped, map)
    deptMap.value = map
    expandedKeys.value = [ROOT_DEPT_ID]
  } catch (error) {
    console.warn('[职位管理] 部门树加载失败', error)
    deptNodes.value = []
    categoryTreeData.value = [{ id: ROOT_DEPT_ID, categoryName: '全部部门' }]
  } finally {
    categoryLoading.value = false
  }
}

function handleDeptSelect(keys: any[]) {
  const key = keys?.[0]
  selectedDeptId.value = key === undefined || key === null ? ROOT_DEPT_ID : key
  handleSearch()
}

function handleDeptExpand(keys: any[]) {
  expandedKeys.value = keys || []
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (String(selectedDeptId.value) !== ROOT_DEPT_ID) params.deptId = selectedDeptId.value
  if (searchForm.positionCode) params.positionCode = searchForm.positionCode.trim()
  if (searchForm.positionName) params.positionName = searchForm.positionName.trim()
  if (searchForm.positionLevel !== undefined) params.positionLevel = searchForm.positionLevel
  if (searchForm.status !== undefined) params.status = searchForm.status
  return params
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await hrPositionApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[职位管理] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.positionCode = ''
  searchForm.positionName = ''
  searchForm.positionLevel = undefined
  searchForm.status = undefined
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 展示辅助 ═══
function textOf(map: Record<any, string>, value: any): string {
  if (value === null || value === undefined) return '-'
  return map[value] || '-'
}

/** 空缺 = 编制人数 - 在岗人数（负数按 0 显示） */
function vacancyOf(record: any): number {
  return Math.max(0, (record?.quotaCount || 0) - (record?.currentCount || 0))
}

// ═══ 岗位表单 ═══
const formOpen = ref(false)
const formRef = ref()
const emptyForm = () => ({
  id: null as number | string | null,
  positionCode: '',
  positionName: '',
  deptId: undefined as number | undefined,
  positionLevel: 3 as number,
  quotaCount: 1 as number,
  currentCount: 0 as number,
  sort: 0 as number,
  status: 1 as number,
  responsibility: '',
  remark: '',
})
const formState = reactive(emptyForm())

const rules = {
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
}

function resetForm() {
  Object.assign(formState, emptyForm())
}

async function handleAdd() {
  resetForm()
  // 列表左树已选部门时作为默认所属部门
  if (String(selectedDeptId.value) !== ROOT_DEPT_ID) formState.deptId = Number(selectedDeptId.value)
  formOpen.value = true
  // 编码留空由后端生成，这里预取号段做提示（失败不阻塞）
  try {
    const code: any = await hrPositionApi.nextCode()
    if (!formState.positionCode && code) formState.positionCode = String(code)
  } catch (error) {
    console.warn('[职位管理] 号段获取失败，保存时由后端自动生成', error)
  }
}

async function handleEdit(record: HrPosition) {
  resetForm()
  formOpen.value = true
  try {
    const detail: any = await hrPositionApi.getById(record.id)
    const row: any = detail || record
    formState.id = row.id
    formState.positionCode = row.positionCode || ''
    formState.positionName = row.positionName || ''
    formState.deptId = row.deptId ?? undefined
    formState.positionLevel = row.positionLevel ?? 3
    formState.quotaCount = row.quotaCount ?? 0
    formState.currentCount = row.currentCount ?? 0
    formState.sort = row.sort ?? 0
    formState.status = row.status ?? 1
    formState.responsibility = row.responsibility || ''
    formState.remark = row.remark || ''
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载岗位详情失败')
  }
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload = {
    positionCode: formState.positionCode?.trim() || null,
    positionName: formState.positionName.trim(),
    deptId: formState.deptId ?? null,
    positionLevel: formState.positionLevel ?? 3,
    quotaCount: formState.quotaCount ?? 0,
    sort: formState.sort ?? 0,
    status: formState.status ?? 1,
    responsibility: formState.responsibility || null,
    remark: formState.remark || null,
  }
  saving.value = true
  try {
    if (formState.id) {
      await hrPositionApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await hrPositionApi.create(payload)
      message.success('新增成功')
    }
    formOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除（后端在有在岗员工时拒绝并返回明确 message） ═══
function handleDelete(record: HrPosition) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除岗位「${record.positionName}」吗？删除后不再出现在台账中。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await hrPositionApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 打印(F8)：岗位台账 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const body = rows.map((r: any, i: number) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.positionCode)}</td>
      <td>${escapeHtml(r.positionName)}</td>
      <td>${escapeHtml(r.deptName || '')}</td>
      <td>${escapeHtml(POSITION_LEVEL_MAP[r.positionLevel] || '')}</td>
      <td>${escapeHtml(r.quotaCount ?? 0)}</td>
      <td>${escapeHtml(r.currentCount ?? 0)}</td>
      <td>${escapeHtml(vacancyOf(r))}</td>
      <td>${escapeHtml(r.overQuota ? '超编' : '正常')}</td>
      <td>${escapeHtml(r.status === 1 ? '启用' : '停用')}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>岗位台账</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>岗位台账</h2>
    <div class="meta">
      <span>部门：${escapeHtml(currentDeptPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${rows.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>岗位编码</th><th>岗位名称</th><th>部门</th><th>岗位级别</th>
        <th>编制人数</th><th>在岗人数</th><th>空缺</th><th>是否超编</th><th>状态</th>
      </tr></thead>
      <tbody>${body}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1100,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV，全量按当前筛选） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params = buildParams()
    delete params.pageNum
    delete params.pageSize
    const res: any = await hrPositionApi.page({ ...params, pageNum: 1, pageSize: 10000 })
    const rows: HrPosition[] = res?.records || []
    if (!rows.length) {
      message.warning('没有可导出的数据')
      return
    }
    const header = ['岗位编码', '岗位名称', '部门', '岗位级别', '编制人数', '在岗人数', '空缺', '是否超编', '排序', '状态', '岗位职责', '备注']
    const lines = rows.map(r => [
      r.positionCode, r.positionName, r.deptName,
      POSITION_LEVEL_MAP[r.positionLevel] || '', r.quotaCount ?? 0, r.currentCount ?? 0,
      vacancyOf(r), r.overQuota ? '超编' : '正常', r.sort ?? 0, r.status === 1 ? '启用' : '停用',
      r.responsibility, r.remark,
    ].map(cell => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(','))
    const csv = `\uFEFF${header.join(',')}\n${lines.join('\n')}`
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `岗位台账_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success(`导出成功，共 ${rows.length} 条`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 页面配置 ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'positionCode', label: '岗位编码', visible: true },
  { key: 'positionName', label: '岗位名称', visible: true },
  { key: 'positionLevel', label: '岗位级别', visible: true },
  { key: 'status', label: '状态', visible: true },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增岗位', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}

function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}

function handlePageConfigChange(config: { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }) {
  if (config?.queryFields) queryFields.value = config.queryFields.map(f => ({ ...f }))
  if (config?.functionButtons) functionButtons.value = config.functionButtons.map(b => ({ ...b }))
}

function handleError(error: Error) {
  console.error('[职位管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadDeptTree()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
/* 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.readonly-text { color: #333; }

.btn-add { background: #ff6b35 !important; border-color: #ff6b35 !important; }
.btn-add:hover { background: #e55a2b !important; border-color: #e55a2b !important; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
