<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="仓库规划"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="仓库规划"
      row-key="id"
    >
      <template #header-extra>
        <a-button
          type="primary"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>新增仓库
        </a-button>
      </template>
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'warehouseType'">
          <a-tag :color="WAREHOUSE_TYPE_MAP[text]?.color">
            {{ WAREHOUSE_TYPE_MAP[text]?.label || text || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'isWmsEnabled'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '已启用' : '已停用' }}
          </a-tag>
        </template>
        <template v-else-if="['totalCapacity', 'usedCapacity'].includes(column.dataIndex as string)">
          {{ formatNumber(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openEdit(record)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="toggleWms(record)"
            >
              {{ record.isWmsEnabled === 1 ? '停用' : '启用' }}
            </a-button>
            <a-popconfirm
              title="确认删除该仓库？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(record)"
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

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑仓库' : '新增仓库'"
      :confirm-loading="saving"
      width="560px"
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
          label="仓库编码"
          name="warehouseCode"
        >
          <a-input
            v-model:value="form.warehouseCode"
            placeholder="请输入仓库编码"
            :disabled="!!editingId"
          />
        </a-form-item>
        <a-form-item
          label="仓库名称"
          name="warehouseName"
        >
          <a-input
            v-model:value="form.warehouseName"
            placeholder="请输入仓库名称"
          />
        </a-form-item>
        <a-form-item
          label="仓库类型"
          name="warehouseType"
        >
          <a-select
            v-model:value="form.warehouseType"
            placeholder="请选择仓库类型"
            :options="warehouseTypeOptions"
          />
        </a-form-item>
        <a-form-item
          label="总容量(m³)"
          name="totalCapacity"
        >
          <a-input-number
            v-model:value="form.totalCapacity"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入总容量"
          />
        </a-form-item>
        <a-form-item
          label="启用WMS"
          name="isWmsEnabled"
        >
          <a-switch
            :checked="form.isWmsEnabled === 1"
            @change="(v: any) => (form.isWmsEnabled = v ? 1 : 0)"
          />
        </a-form-item>
        <a-form-item
          label="备注"
          name="remark"
        >
          <a-textarea
            v-model:value="form.remark"
            :rows="2"
            placeholder="请输入备注"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { warehouseApi, type WmsWarehouse } from '@/api/wms/warehouse'

// ═══ 仓库类型（与后端 WmsWarehouse.warehouseType 注释一致） ═══
const WAREHOUSE_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通仓', color: 'blue' },
  2: { label: '冷库', color: 'cyan' },
  3: { label: '危险品仓', color: 'red' },
  4: { label: '保税仓', color: 'purple' }
}

const warehouseTypeOptions = Object.entries(WAREHOUSE_TYPE_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

const queryFields: ReportQueryField[] = [
  { key: 'warehouseName', type: 'input', label: '仓库名称', placeholder: '仓库名称', width: 180 },
  { key: 'warehouseCode', type: 'input', label: '仓库编码', placeholder: '仓库编码', width: 180 },
  {
    key: 'warehouseType',
    type: 'select',
    label: '仓库类型',
    placeholder: '全部类型',
    options: warehouseTypeOptions
  }
]

const columns: any[] = [
  { title: '仓库编码', dataIndex: 'warehouseCode', key: 'warehouseCode', width: 120 },
  { title: '仓库名称', dataIndex: 'warehouseName', key: 'warehouseName', width: 160, ellipsis: true },
  { title: '类型', dataIndex: 'warehouseType', key: 'warehouseType', width: 100 },
  { title: '库区数', dataIndex: 'zoneCount', key: 'zoneCount', width: 80, align: 'right' },
  { title: '货位数', dataIndex: 'locationCount', key: 'locationCount', width: 80, align: 'right' },
  { title: '总容量(m³)', dataIndex: 'totalCapacity', key: 'totalCapacity', width: 110, align: 'right' },
  { title: '已用容量', dataIndex: 'usedCapacity', key: 'usedCapacity', width: 100, align: 'right' },
  { title: 'WMS状态', dataIndex: 'isWmsEnabled', key: 'isWmsEnabled', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', dataIndex: 'action', key: 'action', width: 170, fixed: 'right' }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求（后端 Page 绑定 current/size，此处做参数映射） ═══
function fetcher(params: Record<string, any>) {
  const { page, ...rest } = params
  return warehouseApi.page({ ...rest, current: page })
}

// ═══ 新增/编辑弹窗 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const formRef = ref()
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = () => ({
  warehouseCode: '',
  warehouseName: '',
  warehouseType: 1 as number,
  totalCapacity: undefined as number | undefined,
  isWmsEnabled: 1 as number,
  remark: ''
})
const form = reactive(emptyForm())

const rules: Record<string, any> = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }]
}

function resetForm(data?: Partial<WmsWarehouse>) {
  Object.assign(form, emptyForm(), data || {})
}

function openCreate() {
  editingId.value = null
  resetForm()
  modalOpen.value = true
}

function openEdit(record: any) {
  editingId.value = record.id
  resetForm({
    warehouseCode: record.warehouseCode,
    warehouseName: record.warehouseName,
    warehouseType: record.warehouseType ?? 1,
    totalCapacity: record.totalCapacity,
    isWmsEnabled: record.isWmsEnabled ?? 1,
    remark: record.remark
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
      await warehouseApi.update({ id: editingId.value, ...form })
      message.success('仓库更新成功')
    } else {
      await warehouseApi.save({ ...form })
      message.success('仓库创建成功')
    }
    modalOpen.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[仓库规划] 保存失败', e)
  } finally {
    saving.value = false
  }
}

async function toggleWms(record: any) {
  const target = record.isWmsEnabled === 1 ? 0 : 1
  try {
    await warehouseApi.update({ ...record, isWmsEnabled: target })
    message.success(target === 1 ? '已启用WMS管理' : '已停用WMS管理')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[仓库规划] 状态切换失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await warehouseApi.remove(record.id)
    message.success('删除成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[仓库规划] 删除失败', e)
  }
}
</script>
