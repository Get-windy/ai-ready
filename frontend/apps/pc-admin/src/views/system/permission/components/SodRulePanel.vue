<template>
  <div class="sod-panel">
    <div class="sod-toolbar">
      <div class="sod-toolbar__left">
        <h3>职责分离规则（SoD）</h3>
        <span class="sod-toolbar__hint">
          同一用户不能同时拥有同一条规则里列出的任意两个角色；给用户分配角色时系统会强制校验
        </span>
      </div>
      <a-space :size="8">
        <a-button
          size="small"
          :loading="loading"
          @click="load"
        >
          刷新
        </a-button>
        <a-button
          v-permission="'tenant-admin:sod-rule:create'"
          type="primary"
          size="small"
          @click="openCreate"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          新增规则
        </a-button>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="rows"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
      :loading="loading"
      size="small"
      row-key="id"
      bordered
      :scroll="{ x: 880 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'ruleName'">
          {{ record.ruleName }}
        </template>
        <template v-else-if="column.key === 'conflictRoles'">
          <a-space
            :size="4"
            wrap
          >
            <a-tag
              v-for="rid in parseRoleIds(record.conflictRoleIds)"
              :key="rid"
              color="red"
            >
              {{ roleName(rid) }}
            </a-tag>
            <span v-if="!parseRoleIds(record.conflictRoleIds).length">-</span>
          </a-space>
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'default'">
            {{ record.status === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <a-button
              v-permission="'tenant-admin:sod-rule:update'"
              type="link"
              size="small"
              @click="openEdit(record)"
            >
              编辑
            </a-button>
            <a-button
              v-permission="'tenant-admin:sod-rule:delete'"
              type="link"
              size="small"
              danger
              @click="handleRemove(record)"
            >
              删除
            </a-button>
          </a-space>
        </template>
      </template>
      <template #emptyText>
        <a-empty description="尚未配置职责分离规则" />
      </template>
    </a-table>

    <!-- 新增/编辑 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑 SoD 规则' : '新增 SoD 规则'"
      width="560px"
      :confirm-loading="saving"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleSubmit"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item
          label="规则名称"
          name="ruleName"
        >
          <a-input
            v-model:value="form.ruleName"
            placeholder="如：采购与付款审批不得同人"
          />
        </a-form-item>
        <a-form-item
          label="不能同时授予"
          name="conflictRoleIds"
        >
          <a-select
            v-model:value="form.roleIds"
            mode="multiple"
            placeholder="选择任意两个及以上不能同时授予的角色"
            :options="roleOptions"
            option-filter-prop="label"
          />
          <div class="form-hint">
            同一个用户若被同时授予这里勾选的任意两个角色，保存时会被系统拒绝
          </div>
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">
              启用
            </a-radio>
            <a-radio :value="0">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="form.description"
            :rows="2"
            placeholder="这条规则对应的风险（便于审计追溯）"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
/**
 * 职责分离（SoD）规则面板
 *
 * 挂在「权限配置」页：SoD 是**全局规则**（约束角色组合），不属于单个角色，故不放进角色弹窗。
 * 此前该表与后端接口齐全但**零前端入口、零数据**（2026-09-20 补）。
 */
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { sodRuleApi, type SodRule } from '@/api/sodRule'
import { roleApi } from '@/api/role'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const rows = ref<SodRule[]>([])
const loading = ref(false)
const saving = ref(false)
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()

const roleOptions = ref<{ label: string; value: number }[]>([])
const roleNameMap = ref<Record<number, string>>({})

const form = reactive({
  ruleName: '',
  roleIds: [] as number[],
  status: 1,
  description: '',
})

const formRules = {
  ruleName: { required: true, message: '请输入规则名称', trigger: 'blur' },
  conflictRoleIds: {
    validator: () => (form.roleIds.length >= 2 ? Promise.resolve() : Promise.reject('互斥角色至少选择两个')),
    trigger: 'change',
  },
}

const columns = [
  { title: '规则名称', key: 'ruleName', width: 200 },
  { title: '不能同时授予的角色', key: 'conflictRoles', width: 320 },
  { title: '状态', key: 'status', width: 90 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 130, fixed: 'right' as const },
]

function parseRoleIds(raw?: string): number[] {
  if (!raw) return []
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed.map((v: any) => Number(v)) : []
  } catch {
    return []
  }
}

function roleName(roleId: number): string {
  return roleNameMap.value[roleId] || `#${roleId}`
}

async function loadRoles() {
  if (roleOptions.value.length) return
  try {
    const res: any = await roleApi.getPage({ tenantId: userStore.tenantId, current: 1, size: 200 })
    const records = res?.records || []
    roleOptions.value = records.map((r: any) => ({ label: r.roleName, value: Number(r.id) }))
    roleNameMap.value = records.reduce((acc: Record<number, string>, r: any) => {
      acc[Number(r.id)] = r.roleName
      return acc
    }, {})
  } catch (err) {
    console.warn('[权限配置] 加载角色列表失败', err)
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await sodRuleApi.page({ pageNum: 1, pageSize: 100 })
    rows.value = res?.records || []
  } catch (err) {
    console.warn('[权限配置] 加载 SoD 规则失败', err)
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.ruleName = ''
  form.roleIds = []
  form.status = 1
  form.description = ''
  modalVisible.value = true
}

function openEdit(record: SodRule) {
  editingId.value = record.id ?? null
  form.ruleName = record.ruleName
  form.roleIds = parseRoleIds(record.conflictRoleIds)
  form.status = record.status === 0 ? 0 : 1
  form.description = record.description || ''
  modalVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      ruleName: form.ruleName.trim(),
      conflictRoleIds: JSON.stringify(form.roleIds),
      status: form.status,
      description: form.description,
    }
    if (editingId.value) {
      await sodRuleApi.update(editingId.value, payload)
      message.success('规则已更新')
    } else {
      await sodRuleApi.create(payload)
      message.success('规则已创建')
    }
    modalVisible.value = false
    await load()
  } catch (err: any) {
    message.error(err?.message || '保存规则失败')
  } finally {
    saving.value = false
  }
}

function handleRemove(record: SodRule) {
  if (!record.id) return
  Modal.confirm({
    title: '删除 SoD 规则',
    content: `确定删除规则「${record.ruleName}」吗？删除后该互斥约束不再生效。`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      await sodRuleApi.remove(record.id as number)
      message.success('已删除')
      await load()
    },
  })
}

onMounted(() => {
  void loadRoles()
  void load()
})
</script>

<style scoped>
.sod-panel {
  margin-top: 16px;
}

.sod-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sod-toolbar__left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.sod-toolbar__left h3 {
  margin: 0;
}

.sod-toolbar__hint {
  font-size: 12px;
  color: #909399;
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
</style>
