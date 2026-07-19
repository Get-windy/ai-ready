<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <span class="page-header__breadcrumb">产品管理 / 价格等级管理</span>
            <h2 class="page-header__title">
              价格等级管理
            </h2>
          </div>
          <div class="page-header__right">
            <a-space :size="12">
              <a-button
                size="small"
                :loading="loading"
                @click="loadGrades"
              >
                <ReloadOutlined /> 刷新
              </a-button>
              <a-button
                type="primary"
                size="small"
                @click="handleAdd"
              >
                <PlusOutlined /> 新增等级
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <!-- 说明卡片 -->
      <a-card
        size="small"
        class="info-card"
        style="margin-bottom: 16px;"
      >
        <a-alert
          type="info"
          show-icon
        >
          <template #message>
            <span>价格等级用于为不同客户类型设置不同的销售价格。系统默认提供8个等级，您可以自定义等级名称（昵称）。等级名称将在产品多单位价格配置和客户等级管理中显示。</span>
          </template>
        </a-alert>
      </a-card>

      <!-- 等级列表 -->
      <a-card
        title="等级列表"
        size="small"
      >
        <a-spin :spinning="loading">
          <a-table
            :data-source="grades"
            :columns="columns"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'sortOrder'">
                <span class="sort-num">{{ record.sortOrder }}</span>
              </template>
              <template v-if="column.key === 'gradeName'">
                <div class="grade-name-cell">
                  <template v-if="editingId === record.id">
                    <a-input
                      v-model:value="editingName"
                      size="small"
                      style="width: 200px"
                      :maxlength="20"
                      @press-enter="handleSaveEdit(record)"
                      @keyup.escape="cancelEdit"
                    />
                    <a-space>
                      <a-button
                        type="link"
                        size="small"
                        @click="handleSaveEdit(record)"
                      >
                        <CheckOutlined />
                      </a-button>
                      <a-button
                        type="link"
                        size="small"
                        danger
                        @click="cancelEdit"
                      >
                        <CloseOutlined />
                      </a-button>
                    </a-space>
                  </template>
                  <template v-else>
                    <span class="grade-name">{{ record.gradeName }}</span>
                    <a-button
                      type="link"
                      size="small"
                      class="edit-btn"
                      @click="startEdit(record)"
                    >
                      <EditOutlined />
                    </a-button>
                  </template>
                </div>
              </template>
              <template v-if="column.key === 'status'">
                <a-switch
                  :checked="record.status === 1"
                  checked-children="启用"
                  un-checked-children="停用"
                  :loading="record._saving"
                  @change="(val: boolean) => handleToggleStatus(record, val)"
                />
              </template>
              <template v-if="column.key === 'actions'">
                <a-space>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="index === 0"
                    @click="handleMoveUp(record, index)"
                  >
                    上移
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="index === grades.length - 1"
                    @click="handleMoveDown(record, index)"
                  >
                    下移
                  </a-button>
                  <a-popconfirm
                    title="确定删除该等级吗？"
                    ok-text="确定"
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
          </a-table>
        </a-spin>
      </a-card>

      <!-- 新增等级弹窗 -->
      <a-modal
        v-model:open="addModalVisible"
        title="新增价格等级"
        :confirm-loading="addLoading"
        @ok="handleAddConfirm"
        @cancel="addModalVisible = false"
      >
        <a-form layout="vertical">
          <a-form-item
            label="等级名称"
            required
          >
            <a-input
              v-model:value="newGrade.gradeName"
              placeholder="例如：VIP客户"
              :maxlength="20"
            />
          </a-form-item>
          <a-form-item label="等级编码">
            <a-input
              v-model:value="newGrade.gradeCode"
              placeholder="可选，留空自动生成"
              :maxlength="30"
            />
          </a-form-item>
          <a-form-item label="排序号">
            <a-input-number
              v-model:value="newGrade.sortOrder"
              :min="1"
              :max="99"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="newGrade.remark"
              :rows="2"
              :maxlength="100"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, PlusOutlined, EditOutlined, CheckOutlined, CloseOutlined
} from '@ant-design/icons-vue'
import { productGradeApi, type ProductGrade } from '@/api/erp/product'

function handleError(err: any) { console.warn('[等级管理] ErrorBoundary:', err) }

const loading = ref(false)
const grades = ref<(ProductGrade & { _saving?: boolean })[]>([])

const columns = [
  { title: '序号', key: 'sortOrder', width: 70, align: 'center' as const },
  { title: '等级编码', dataIndex: 'gradeCode', key: 'gradeCode', width: 150 },
  { title: '等级名称（昵称）', key: 'gradeName', width: 260 },
  { title: '等级级别', dataIndex: 'gradeLevel', key: 'gradeLevel', width: 100, align: 'center' as const },
  { title: '状态', key: 'status', width: 110, align: 'center' as const },
  { title: '操作', key: 'actions', width: 180 }
]

async function loadGrades() {
  loading.value = true
  try {
    const list = await productGradeApi.list()
    grades.value = list.sort((a, b) => a.sortOrder - b.sortOrder)
  } catch {
    message.error('加载等级列表失败')
  } finally {
    loading.value = false
  }
}

// ── 编辑等级名称 ──
const editingId = ref<number | null>(null)
const editingName = ref('')

function startEdit(record: ProductGrade) {
  editingId.value = record.id
  editingName.value = record.gradeName
}

function cancelEdit() {
  editingId.value = null
  editingName.value = ''
}

async function handleSaveEdit(record: ProductGrade & { _saving?: boolean }) {
  const newName = editingName.value.trim()
  if (!newName) {
    message.warning('等级名称不能为空')
    return
  }
  if (newName === record.gradeName) {
    cancelEdit()
    return
  }
  record._saving = true
  try {
    await productGradeApi.update(record.id, { gradeName: newName })
    record.gradeName = newName
    message.success('等级名称已更新')
    cancelEdit()
  } catch {
    message.error('更新失败')
  } finally {
    record._saving = false
  }
}

// ── 新增等级 ──
const addModalVisible = ref(false)
const addLoading = ref(false)
const newGrade = ref({ gradeName: '', gradeCode: '', sortOrder: 1, remark: '' })

function handleAdd() {
  newGrade.value = {
    gradeName: '',
    gradeCode: '',
    sortOrder: grades.value.length + 1,
    remark: ''
  }
  addModalVisible.value = true
}

async function handleAddConfirm() {
  if (!newGrade.value.gradeName.trim()) {
    message.warning('请输入等级名称')
    return
  }
  addLoading.value = true
  try {
    await productGradeApi.create({
      gradeName: newGrade.value.gradeName.trim(),
      gradeCode: newGrade.value.gradeCode.trim() || `GRADE_${Date.now().toString(36).toUpperCase()}`,
      gradeLevel: grades.value.length + 1,
      sortOrder: newGrade.value.sortOrder,
      status: 1,
      remark: newGrade.value.remark
    })
    message.success('新增成功')
    addModalVisible.value = false
    await loadGrades()
  } catch {
    message.error('新增失败')
  } finally {
    addLoading.value = false
  }
}

// ── 启用/停用 ──
async function handleToggleStatus(record: ProductGrade & { _saving?: boolean }, enabled: boolean) {
  record._saving = true
  try {
    await productGradeApi.update(record.id, { status: enabled ? 1 : 0 })
    record.status = enabled ? 1 : 0
    message.success(enabled ? '已启用' : '已停用')
  } catch {
    message.error('操作失败')
  } finally {
    record._saving = false
  }
}

// ── 排序 ──
async function handleMoveUp(record: ProductGrade, index: number) {
  if (index <= 0) return
  const prev = grades.value[index - 1]
  await swapOrder(record, prev)
}

async function handleMoveDown(record: ProductGrade, index: number) {
  if (index >= grades.value.length - 1) return
  const next = grades.value[index + 1]
  await swapOrder(record, next)
}

async function swapOrder(a: ProductGrade, b: ProductGrade) {
  try {
    const tmp = a.sortOrder
    await productGradeApi.update(a.id, { sortOrder: b.sortOrder })
    await productGradeApi.update(b.id, { sortOrder: tmp })
    await loadGrades()
  } catch {
    message.error('排序失败')
  }
}

// ── 删除 ──
async function handleDelete(record: ProductGrade) {
  try {
    await productGradeApi.delete(record.id)
    message.success('删除成功')
    await loadGrades()
  } catch {
    message.error('删除失败，可能该等级正在被使用')
  }
}

onMounted(() => {
  loadGrades()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header__breadcrumb {
  color: rgba(0,0,0,.45);
  font-size: 12px;
}
.page-header__title {
  margin: 4px 0 0;
  font-size: 20px;
  font-weight: 600;
}
.info-card :deep(.ant-card-body) {
  padding: 8px 12px;
}
.sort-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f5ff;
  color: #1890ff;
  font-weight: 600;
  font-size: 13px;
}
.grade-name-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}
.grade-name {
  font-weight: 500;
}
.edit-btn {
  opacity: 0.4;
  transition: opacity 0.2s;
}
.grade-name-cell:hover .edit-btn {
  opacity: 1;
}
</style>
