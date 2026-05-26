<template>
  <div class="user-authorization-page">
    <el-page-header @back="goBack">
      <template #content>
        <span class="header-title">用户授权管理</span>
      </template>
      <template #extra>
        <div class="header-actions">
          <el-button type="primary" @click="exportData">
            <el-icon><Download /></el-icon>
            导出数据
          </el-button>
        </div>
      </template>
    </el-page-header>

    <el-divider />

    <UserAuthorization />

    <!-- 权限预览对话框 -->
    <el-dialog
      v-model="permissionPreviewVisible"
      title="权限预览"
      width="800px"
    >
      <PermissionPreview 
        v-if="permissionPreviewVisible && selectedUserId"
        :user-id="selectedUserId"
      />
    </el-dialog>

    <!-- 授权历史对话框 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="授权历史"
      width="1000px"
    >
      <AuthorizationHistory 
        v-if="historyDialogVisible && selectedUserId"
        :user-id="selectedUserId"
      />
    </el-dialog>

    <!-- 冲突检测对话框 -->
    <el-dialog
      v-model="conflictDialogVisible"
      title="权限冲突检测"
      width="800px"
    >
      <PermissionConflictDetection 
        v-if="conflictDialogVisible"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import UserAuthorization from '../components/UserAuthorization/UserAuthorization.vue'
import PermissionPreview from '../components/UserAuthorization/PermissionPreview.vue'
import AuthorizationHistory from '../components/UserAuthorization/AuthorizationHistory.vue'
import PermissionConflictDetection from '../components/UserAuthorization/PermissionConflictDetection.vue'
import { roleManagementApi } from '../api'

const router = useRouter()
const permissionPreviewVisible = ref(false)
const historyDialogVisible = ref(false)
const conflictDialogVisible = ref(false)
const selectedUserId = ref<string>('')

function goBack() {
  router.back()
}

async function exportData() {
  try {
    const blob = await roleManagementApi.exportRoles('excel')
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `用户授权数据_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

function showPermissionPreview(userId: string) {
  selectedUserId.value = userId
  permissionPreviewVisible.value = true
}

function showAuthorizationHistory(userId: string) {
  selectedUserId.value = userId
  historyDialogVisible.value = true
}

function showConflictDetection() {
  conflictDialogVisible.value = true
}
</script>

<style scoped>
.user-authorization-page {
  padding: 24px;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

:deep(.el-page-header) {
  margin-bottom: 24px;
}

:deep(.el-divider) {
  margin: 0 0 24px 0;
}
</style>