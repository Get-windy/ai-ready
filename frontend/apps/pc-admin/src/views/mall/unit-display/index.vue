<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="单位显示"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="商品单位"
      row-key="id"
      empty-text="暂无商品单位"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增单位
        </a-button>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'conversionRate'">
          {{ formatQty(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openEdit(record as ProductUnitDict)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              :style="record.status === 1 ? 'color: #fa8c16' : 'color: #52c41a'"
              @click="toggleStatus(record as ProductUnitDict)"
            >
              {{ record.status === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确认删除该单位？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record as ProductUnitDict)"
            >
              <a-button
                type="link"
                size="small"
                danger
              >
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- 新增/编辑单位弹窗 -->
    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑单位' : '新增单位'"
      :confirm-loading="saving"
      width="520px"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="单位名称"
          name="unitName"
        >
          <a-input
            v-model:value="form.unitName"
            placeholder="如：件、箱、公斤"
          />
        </a-form-item>
        <a-form-item
          label="助记码"
          name="mnemonicCode"
        >
          <a-input
            v-model:value="form.mnemonicCode"
            placeholder="拼音首字母，如：J、X、KG"
          />
        </a-form-item>
        <a-form-item
          label="单位类型"
          name="unitType"
        >
          <a-select
            v-model:value="form.unitType"
            allow-clear
            placeholder="请选择单位类型"
            :options="[
              { label: '基本单位', value: '基本单位' },
              { label: '辅助单位', value: '辅助单位' }
            ]"
          />
        </a-form-item>
        <a-form-item
          label="换算率"
          name="conversionRate"
        >
          <a-input-number
            v-model:value="form.conversionRate"
            :min="0"
            :precision="4"
            style="width: 100%"
            placeholder="相对基本单位的换算率"
          />
        </a-form-item>
        <a-form-item
          label="排序"
          name="sortOrder"
        >
          <a-input-number
            v-model:value="form.sortOrder"
            :min="0"
            :precision="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item
          label="状态"
          name="status"
        >
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">
              启用
            </a-radio>
            <a-radio :value="0">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          label="备注"
          name="remark"
        >
          <a-textarea
            v-model:value="form.remark"
            :rows="2"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { productUnitDictApi, type ProductUnitDict } from '@/api/erp/product'

defineOptions({ name: 'MallUnitDisplay' })

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '单位名称/助记码' }
]

const columns: any[] = [
  { title: '单位名称', dataIndex: 'unitName', key: 'unitName', width: 140 },
  { title: '助记码', dataIndex: 'mnemonicCode', key: 'mnemonicCode', width: 110 },
  { title: '单位类型', dataIndex: 'unitType', key: 'unitType', width: 110 },
  { title: '换算率', dataIndex: 'conversionRate', key: 'conversionRate', width: 110, align: 'right' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' }
]

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求（erp-stock /erp/product-unit-dict/page） ═══
function fetcher(params: Record<string, any>) {
  return productUnitDictApi.page(params)
}

// ═══ 新增/编辑/启停/删除 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)

const emptyForm = () => ({
  unitName: '',
  mnemonicCode: '',
  unitType: undefined as string | undefined,
  conversionRate: 1 as number,
  sortOrder: 0 as number,
  status: 1 as number,
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, Rule[]> = {
  unitName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  modalOpen.value = true
}

function openEdit(record: ProductUnitDict) {
  editingId.value = record.id ?? null
  Object.assign(form, emptyForm(), {
    unitName: record.unitName,
    mnemonicCode: record.mnemonicCode || '',
    unitType: record.unitType,
    conversionRate: Number(record.conversionRate) || 1,
    sortOrder: record.sortOrder ?? 0,
    status: record.status ?? 1,
    remark: record.remark || ''
  })
  modalOpen.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await productUnitDictApi.update(editingId.value, { ...form })
      message.success('单位已更新')
    } else {
      await productUnitDictApi.create({ ...form })
      message.success('单位已创建')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[单位显示] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleStatus(record: ProductUnitDict) {
  const target = record.status === 1 ? 0 : 1
  try {
    await productUnitDictApi.update(record.id!, { status: target })
    message.success(target === 1 ? `单位「${record.unitName}」已启用` : `单位「${record.unitName}」已停用`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[单位显示] 状态切换失败', e)
  }
}

async function handleDelete(record: ProductUnitDict) {
  try {
    await productUnitDictApi.delete(record.id!)
    message.success('单位已删除')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[单位显示] 删除失败', e)
  }
}
</script>
