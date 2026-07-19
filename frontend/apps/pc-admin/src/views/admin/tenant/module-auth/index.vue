<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>模块授权</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            模块授权
          </h2>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <a-form
        layout="inline"
        class="search-form"
      >
        <a-form-item label="租户">
          <a-select
            v-model:value="selectedTenantId"
            placeholder="选择租户"
            style="width:240px"
            show-search
            :filter-option="(input:any, option:any) => option.label.toLowerCase().includes(input.toLowerCase())"
            @change="onTenantChange"
          >
            <a-select-option
              v-for="t in tenants"
              :key="t.id"
              :value="t.id"
              :label="t.tenantName"
            >
              {{ t.tenantName }} ({{ t.tenantCode }})
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>

      <a-divider />

      <div v-if="selectedTenantId">
        <a-checkbox-group v-model:value="selectedModules">
          <a-row :gutter="[16, 16]">
            <a-col
              v-for="mod in availableModules"
              :key="mod.moduleCode"
              :span="8"
            >
              <a-card
                size="small"
                :class="{ 'module-card-selected': selectedModules.includes(mod.moduleCode) }"
              >
                <a-checkbox :value="mod.moduleCode">
                  <strong>{{ mod.moduleName }}</strong>
                </a-checkbox>
                <div class="module-desc">
                  {{ mod.moduleCode }}
                </div>
              </a-card>
            </a-col>
          </a-row>
        </a-checkbox-group>

        <div style="margin-top: 24px; text-align: center">
          <a-button
            type="primary"
            size="large"
            :loading="saving"
            @click="handleSave"
          >
            保存授权
          </a-button>
        </div>
      </div>

      <a-empty
        v-else
        description="请先选择一个租户"
      />
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { tenantApi } from '@/api/tenant'

const tenants = ref<any[]>([])
const availableModules = ref<any[]>([])
const selectedTenantId = ref<number | undefined>(undefined)
const selectedModules = ref<string[]>([])
const saving = ref(false)

async function fetchTenants() {
  const res = await tenantApi.getPage({ pageNum: 1, pageSize: 200 })
  tenants.value = res.data.records || []
}

import { menuApi } from '@/api/menu'
import request from '@/utils/request'

async function fetchModules() {
  try {
    const res = await request.get('/menu/list', { params: { pageNum: 1, pageSize: 200 } })
    availableModules.value = res?.records || res?.data || []
  } catch {
    availableModules.value = []
  }
}

async function onTenantChange(tenantId: number) {
  try {
    const res = await menuApi.getTenantMenuIds(tenantId)
    selectedModules.value = Array.isArray(res.data) ? res.data.map(String) : []
  } catch {
    selectedModules.value = []
  }
}

async function handleSave() {
  saving.value = true
  try {
    await menuApi.assignTenantMenus(selectedTenantId.value!, selectedModules.value.map(Number))
    message.success('授权保存成功')
  } catch {
    message.error('授权保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchTenants()
  fetchModules()
})
</script>

<style scoped>
.module-card-selected {
  border-color: #1890ff;
  background: #e6f7ff;
}
.module-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
